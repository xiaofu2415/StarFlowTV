package com.github.tvbox.osc.config;

import android.content.Context;

import com.github.catvod.net.OkHttp;
import com.github.tvbox.osc.BuildConfig;
import com.github.tvbox.osc.security.Ed25519Verifier;
import com.github.tvbox.osc.security.Sha256;
import com.github.tvbox.osc.update.DistributionEndpoints;
import com.github.tvbox.osc.util.HawkConfig;
import com.orhanobut.hawk.Hawk;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class RemoteConfigManager {
    private static final int MAX_MANIFEST = 256 * 1024;
    private static final int MAX_SIGNATURE = 2048;
    private static final int MAX_LIVE_JSON = 4 * 1024 * 1024;
    private static final int MAX_LIVE_TXT = 8 * 1024 * 1024;
    private static final RefreshRequestLock RUNNING = new RefreshRequestLock();

    private RemoteConfigManager() {
    }

    public enum CheckResult {
        UPDATED(true),
        NO_UPDATE(true),
        FAILED(false),
        BUSY(false);

        private final boolean reloadLiveConfig;

        CheckResult(boolean reloadLiveConfig) {
            this.reloadLiveConfig = reloadLiveConfig;
        }

        public boolean requiresLiveReload() {
            return reloadLiveConfig;
        }
    }

    public interface UpdateListener {
        void onProgress(int percent, String message);

        void onComplete(CheckResult result);
    }

    public static void initialize(Context context) {
        File active = VersionedConfigStore.activeLiveTxt(root(context));
        if (active != null && active.isFile()) {
            Hawk.put(HawkConfig.LIVE_API_URL, "file://" + active.getAbsolutePath());
        }
        check(context.getApplicationContext(), null);
    }

    public static void check(Context context) {
        check(context, null);
    }

    public static void check(Context context, UpdateListener listener) {
        String manifestUrl = BuildConfig.STARFLOW_CONFIG_MANIFEST_URL;
        if (!DistributionEndpoints.isSafeHttps(manifestUrl)) {
            finish(listener, CheckResult.FAILED);
            return;
        }
        RefreshRequestLock.Lease lease = RUNNING.tryAcquire();
        if (lease == null) {
            finish(listener, CheckResult.BUSY);
            return;
        }
        UpdateListener ownerListener = new UpdateListener() {
            @Override public void onProgress(int percent, String message) {
                notifyProgress(listener, percent, message);
            }

            @Override public void onComplete(CheckResult result) {
                if (lease.close()) finish(listener, result);
            }
        };
        try {
            checkOwned(context, manifestUrl, ownerListener);
        } catch (Exception error) {
            finish(ownerListener, CheckResult.FAILED);
        }
    }

    private static void checkOwned(Context context, String manifestUrl, UpdateListener listener) {
        notifyProgress(listener, 5, "正在校验更新清单");
        fetch(manifestUrl, MAX_MANIFEST, manifestBytes -> {
            if (manifestBytes == null) { finish(listener, CheckResult.FAILED); return; }
            notifyProgress(listener, 15, "正在校验清单签名");
            fetch(sibling(manifestUrl, "manifest.sig"), MAX_SIGNATURE, signatureBytes -> {
                String signature = signatureBytes == null ? "" :
                        new String(signatureBytes, StandardCharsets.US_ASCII).trim();
                if (!Ed25519Verifier.verify(manifestBytes, signature,
                        BuildConfig.STARFLOW_SIGNING_PUBLIC_KEY_B64)) {
                    finish(listener, CheckResult.FAILED);
                    return;
                }
                notifyProgress(listener, 25, "清单签名校验通过");
                try {
                    ConfigManifest manifest = ConfigManifest.parse(
                            new String(manifestBytes, StandardCharsets.UTF_8));
                    ConfigDecision decision = ConfigPolicy.evaluate(
                            VersionedConfigStore.activeVersion(root(context)), manifestUrl,
                            BuildConfig.STARFLOW_SIGNING_KEY_ID, manifest);
                    if (decision == ConfigDecision.NO_UPDATE) {
                        notifyProgress(listener, 100, "当前已经是最新直播源");
                        finish(listener, CheckResult.NO_UPDATE);
                        return;
                    }
                    if (decision != ConfigDecision.AVAILABLE) {
                        finish(listener, CheckResult.FAILED);
                        return;
                    }
                    downloadPayloads(context, manifest, listener);
                } catch (Exception error) {
                    finish(listener, CheckResult.FAILED);
                }
            });
        }, (read, total) -> reportDownload(listener, 5, 15, "正在校验更新清单", read, total));
    }

    private static void downloadPayloads(Context context, ConfigManifest manifest, UpdateListener listener) {
        ConfigManifest.Entry json = manifest.file("live.json");
        ConfigManifest.Entry txt = manifest.file("live.txt");
        notifyProgress(listener, 30, "正在下载 live.json");
        fetch(json.url, bounded(json, MAX_LIVE_JSON), jsonBytes -> {
            if (!matches(json, jsonBytes)) { finish(listener, CheckResult.FAILED); return; }
            notifyProgress(listener, 55, "live.json 校验通过");
            notifyProgress(listener, 60, "正在下载 live.txt");
            fetch(txt.url, bounded(txt, MAX_LIVE_TXT), txtBytes -> {
                if (!matches(txt, txtBytes)) { finish(listener, CheckResult.FAILED); return; }
                notifyProgress(listener, 85, "直播源文件校验通过");
                if (!VersionedConfigStore.activate(root(context), manifest.configVersion, jsonBytes, txtBytes)) {
                    finish(listener, CheckResult.FAILED);
                    return;
                }
                File active = VersionedConfigStore.activeLiveTxt(root(context));
                if (active == null) {
                    finish(listener, CheckResult.FAILED);
                    return;
                }
                Hawk.put(HawkConfig.LIVE_API_URL, "file://" + active.getAbsolutePath());
                notifyProgress(listener, 95, "正在切换到最新直播源");
                notifyProgress(listener, 100, "直播源更新完成");
                finish(listener, CheckResult.UPDATED);
            }, (read, total) -> reportDownload(listener, 60, 85, "正在下载 live.txt", read, total));
        }, (read, total) -> reportDownload(listener, 30, 55, "正在下载 live.json", read, total));
    }

    private static int bounded(ConfigManifest.Entry entry, int maximum) {
        return entry == null || entry.size <= 0 || entry.size > maximum ? 0 : (int) entry.size;
    }

    private static boolean matches(ConfigManifest.Entry entry, byte[] bytes) {
        return bytes != null && bytes.length == entry.size
                && Sha256.digest(bytes).equalsIgnoreCase(entry.sha256);
    }

    private static File root(Context context) {
        return new File(context.getFilesDir(), "starflow-config");
    }

    private static String sibling(String url, String name) {
        return url.substring(0, url.lastIndexOf('/') + 1) + name;
    }

    private static void fetch(String url, int maximum, BytesCallback callback) {
        fetch(url, maximum, callback, null);
    }

    private static void fetch(String url, int maximum, BytesCallback callback, FetchProgress progress) {
        if (maximum <= 0) { callback.complete(null); return; }
        OkHttp.client().newCall(new Request.Builder().url(url).get().build()).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException error) { callback.complete(null); }

            @Override public void onResponse(Call call, Response response) {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful() || body == null || body.contentLength() > maximum) {
                        callback.complete(null); return;
                    }
                    try (InputStream input = body.byteStream();
                         ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                        byte[] buffer = new byte[16 * 1024];
                        long downloaded = 0;
                        long total = body.contentLength();
                        int read;
                        while ((read = input.read(buffer)) != -1) {
                            if (read == 0) continue;
                            if (output.size() + read > maximum) { callback.complete(null); return; }
                            output.write(buffer, 0, read);
                            downloaded += read;
                            if (progress != null) progress.onBytes(downloaded, total);
                        }
                        callback.complete(output.toByteArray());
                    }
                } catch (Exception error) {
                    callback.complete(null);
                }
            }
        });
    }

    private static void finish(UpdateListener listener, CheckResult result) {
        if (listener != null) listener.onComplete(result);
    }

    private static void notifyProgress(UpdateListener listener, int percent, String message) {
        if (listener == null) return;
        listener.onProgress(Math.max(0, Math.min(100, percent)), message);
    }

    private static void reportDownload(UpdateListener listener, int start, int end,
                                       String message, long downloaded, long total) {
        if (listener == null) return;
        int percent = start;
        if (total > 0) {
            long bounded = Math.min(downloaded, total);
            percent = start + (int) ((end - start) * bounded / total);
        }
        notifyProgress(listener, percent, message);
    }

    private interface BytesCallback { void complete(byte[] bytes); }

    private interface FetchProgress { void onBytes(long downloaded, long total); }
}
