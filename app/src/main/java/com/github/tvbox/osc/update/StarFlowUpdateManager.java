package com.github.tvbox.osc.update;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.core.content.FileProvider;

import com.github.catvod.net.OkHttp;
import com.github.tvbox.osc.BuildConfig;
import com.github.tvbox.osc.ui.dialog.TipDialog;
import com.github.tvbox.osc.security.Ed25519Verifier;
import com.github.tvbox.osc.security.Sha256;
import com.github.tvbox.osc.util.DefaultConfig;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.LOG;
import com.orhanobut.hawk.Hawk;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class StarFlowUpdateManager {
    private static final AtomicBoolean CHECK_STARTED = new AtomicBoolean(false);
    private static final long MAX_APK_BYTES = 200L * 1024L * 1024L;
    private static final int MAX_MANIFEST_BYTES = 256 * 1024;

    private StarFlowUpdateManager() {
    }

    public static void check(Activity activity) {
        final String manifestUrl = BuildConfig.STARFLOW_UPDATE_URL;
        if (!DistributionEndpoints.isSafeHttps(manifestUrl)
                || !CHECK_STARTED.compareAndSet(false, true)) return;
        fetch(manifestUrl, MAX_MANIFEST_BYTES, manifestBytes -> {
            if (manifestBytes == null) { CHECK_STARTED.set(false); return; }
            fetch(sibling(manifestUrl, "latest.sig"), 2048, signatureBytes -> {
                String signature = signatureBytes == null ? "" :
                        new String(signatureBytes, StandardCharsets.US_ASCII).trim();
                if (!Ed25519Verifier.verify(manifestBytes, signature,
                        BuildConfig.STARFLOW_SIGNING_PUBLIC_KEY_B64)) {
                    CHECK_STARTED.set(false); return;
                }
                try {
                    UpdateManifest manifest = UpdateManifest.parse(
                            new String(manifestBytes, StandardCharsets.UTF_8));
                    String channel = Hawk.get(HawkConfig.STARFLOW_UPDATE_CHANNEL,
                            BuildConfig.STARFLOW_UPDATE_CHANNEL);
                    UpdateDecision decision = UpdatePolicy.evaluate(
                            DefaultConfig.getAppVersionCode(activity), Build.VERSION.SDK_INT,
                            channel, BuildConfig.STARFLOW_SIGNING_KEY_ID, manifestUrl, manifest);
                    UpdateManifest.Apk apk = AbiSelector.select(manifest, Build.SUPPORTED_ABIS);
                    if (decision == UpdateDecision.AVAILABLE && apk != null) {
                        activity.runOnUiThread(() -> prompt(activity, manifest, apk));
                    }
                } catch (Exception error) {
                    LOG.e("Invalid StarFlow update manifest: " + error.getClass().getSimpleName());
                } finally {
                    CHECK_STARTED.set(false);
                }
            });
        });
    }

    private static void prompt(Activity activity, UpdateManifest manifest, UpdateManifest.Apk apk) {
        String notes = manifest.releaseNotes == null ? "" : "\n" + manifest.releaseNotes;
        final TipDialog[] holder = new TipDialog[1];
        holder[0] = new TipDialog(activity,
                "发现新版本 " + manifest.versionName + notes,
                "稍后", "下载更新", new TipDialog.OnListener() {
            @Override public void left() { if (!manifest.forceUpdate) holder[0].dismiss(); }
            @Override public void right() {
                holder[0].dismiss();
                download(activity, manifest, apk);
            }
            @Override public void cancel() { if (!manifest.forceUpdate) holder[0].dismiss(); }
        });
        holder[0].setCancelable(!manifest.forceUpdate);
        holder[0].show();
    }

    private static void download(Activity activity, UpdateManifest manifest, UpdateManifest.Apk apk) {
        Request request = new Request.Builder().url(apk.downloadUrl).get().build();
        OkHttp.client().newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, java.io.IOException error) {
                LOG.e("StarFlow APK download failed: " + error.getClass().getSimpleName());
            }

            @Override public void onResponse(Call call, Response response) {
                File cacheRoot = activity.getExternalCacheDir();
                if (cacheRoot == null) cacheRoot = activity.getCacheDir();
                File target = new File(cacheRoot, "StarFlowTV-update.apk");
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful() || body == null
                            || body.contentLength() <= 0 || body.contentLength() > MAX_APK_BYTES
                            || body.contentLength() != apk.size) return;
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    long copied = 0;
                    try (InputStream input = body.byteStream();
                         FileOutputStream output = new FileOutputStream(target)) {
                        byte[] buffer = new byte[64 * 1024];
                        int read;
                        while ((read = input.read(buffer)) != -1) {
                            copied += read;
                            if (copied > MAX_APK_BYTES) throw new java.io.IOException("APK too large");
                            digest.update(buffer, 0, read);
                            output.write(buffer, 0, read);
                        }
                    }
                    if (copied != apk.size || !hex(digest.digest()).equalsIgnoreCase(apk.sha256)
                            || !verifyArchive(activity, target, apk.certificateSha256)) {
                        target.delete();
                        return;
                    }
                    activity.runOnUiThread(() -> install(activity, target));
                } catch (Exception error) {
                    target.delete();
                    LOG.e("StarFlow APK verification failed: " + error.getClass().getSimpleName());
                }
            }
        });
    }

    private static boolean verifyArchive(Activity activity, File apk, String expectedCertificate) {
        try {
            PackageInfo info = activity.getPackageManager().getPackageArchiveInfo(
                    apk.getAbsolutePath(), PackageManager.GET_SIGNATURES);
            if (info == null || !BuildConfig.APPLICATION_ID.equals(info.packageName)
                    || info.signatures == null || info.signatures.length != 1) return false;
            return Sha256.digest(info.signatures[0].toByteArray()).equalsIgnoreCase(expectedCertificate);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String sibling(String url, String name) {
        return url.substring(0, url.lastIndexOf('/') + 1) + name;
    }

    private static void fetch(String url, int maximum, BytesCallback callback) {
        OkHttp.client().newCall(new Request.Builder().url(url).get().build()).enqueue(new Callback() {
            @Override public void onFailure(Call call, java.io.IOException error) {
                callback.complete(null);
            }

            @Override public void onResponse(Call call, Response response) {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful() || body == null || body.contentLength() > maximum) {
                        callback.complete(null); return;
                    }
                    try (InputStream input = body.byteStream();
                         java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
                        byte[] buffer = new byte[8192];
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

    private interface BytesCallback { void complete(byte[] value); }

    private static String hex(byte[] bytes) {
        StringBuilder value = new StringBuilder(bytes.length * 2);
        for (byte item : bytes) value.append(String.format(Locale.US, "%02x", item & 0xff));
        return value.toString();
    }

    private static void install(Activity activity, File apk) {
        Uri uri = FileProvider.getUriForFile(
                activity, BuildConfig.APPLICATION_ID + ".fileprovider", apk);
        Intent intent = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
    }
}
