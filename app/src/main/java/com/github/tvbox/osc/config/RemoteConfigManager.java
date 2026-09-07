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
import java.util.concurrent.atomic.AtomicBoolean;

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
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);

    private RemoteConfigManager() {
    }

    public static void initialize(Context context) {
        File active = VersionedConfigStore.activeLiveTxt(root(context));
        if (active != null && active.isFile()) {
            Hawk.put(HawkConfig.LIVE_API_URL, "file://" + active.getAbsolutePath());
        }
        check(context.getApplicationContext());
    }

    public static void check(Context context) {
        String manifestUrl = BuildConfig.STARFLOW_CONFIG_MANIFEST_URL;
        if (!DistributionEndpoints.isSafeHttps(manifestUrl) || !RUNNING.compareAndSet(false, true)) return;
        fetch(manifestUrl, MAX_MANIFEST, manifestBytes -> {
            if (manifestBytes == null) { finish(); return; }
            fetch(sibling(manifestUrl, "manifest.sig"), MAX_SIGNATURE, signatureBytes -> {
                String signature = signatureBytes == null ? "" :
                        new String(signatureBytes, StandardCharsets.US_ASCII).trim();
                if (!Ed25519Verifier.verify(manifestBytes, signature,
                        BuildConfig.STARFLOW_SIGNING_PUBLIC_KEY_B64)) { finish(); return; }
                try {
                    ConfigManifest manifest = ConfigManifest.parse(
                            new String(manifestBytes, StandardCharsets.UTF_8));
                    ConfigDecision decision = ConfigPolicy.evaluate(
                            VersionedConfigStore.activeVersion(root(context)), manifestUrl,
                            BuildConfig.STARFLOW_SIGNING_KEY_ID, manifest);
                    if (decision != ConfigDecision.AVAILABLE) { finish(); return; }
                    downloadPayloads(context, manifest);
                } catch (Exception error) {
                    finish();
                }
            });
        });
    }

    private static void downloadPayloads(Context context, ConfigManifest manifest) {
        ConfigManifest.Entry json = manifest.file("live.json");
        ConfigManifest.Entry txt = manifest.file("live.txt");
        fetch(json.url, bounded(json, MAX_LIVE_JSON), jsonBytes -> {
            if (!matches(json, jsonBytes)) { finish(); return; }
            fetch(txt.url, bounded(txt, MAX_LIVE_TXT), txtBytes -> {
                if (!matches(txt, txtBytes)) { finish(); return; }
                if (VersionedConfigStore.activate(root(context), manifest.configVersion, jsonBytes, txtBytes)) {
                    File active = VersionedConfigStore.activeLiveTxt(root(context));
                    if (active != null) Hawk.put(HawkConfig.LIVE_API_URL, "file://" + active.getAbsolutePath());
                }
                finish();
            });
        });
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
                        int read;
                        while ((read = input.read(buffer)) >= 0) {
                            if (output.size() + read > maximum) { callback.complete(null); return; }
                            output.write(buffer, 0, read);
                        }
                        callback.complete(output.toByteArray());
                    }
                } catch (Exception error) {
                    callback.complete(null);
                }
            }
        });
    }

    private static void finish() { RUNNING.set(false); }

    private interface BytesCallback { void complete(byte[] bytes); }
}
