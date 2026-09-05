package com.github.tvbox.osc.update;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.core.content.FileProvider;

import com.github.catvod.net.OkHttp;
import com.github.tvbox.osc.BuildConfig;
import com.github.tvbox.osc.ui.dialog.TipDialog;
import com.github.tvbox.osc.util.DefaultConfig;
import com.github.tvbox.osc.util.LOG;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
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

    private StarFlowUpdateManager() {
    }

    public static void check(Activity activity) {
        final String manifestUrl = BuildConfig.STARFLOW_UPDATE_URL;
        if (!DistributionEndpoints.isSafeHttps(manifestUrl)
                || !CHECK_STARTED.compareAndSet(false, true)) return;
        Request request = new Request.Builder().url(manifestUrl).get().build();
        OkHttp.client().newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, java.io.IOException error) {
                LOG.e("StarFlow update check failed: " + error.getClass().getSimpleName());
            }

            @Override public void onResponse(Call call, Response response) {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful() || body == null) return;
                    UpdateManifest manifest = UpdateManifest.parse(body.string());
                    UpdateDecision decision = UpdatePolicy.evaluate(
                            DefaultConfig.getAppVersionCode(activity), Build.VERSION.SDK_INT,
                            manifestUrl, manifest);
                    if (decision == UpdateDecision.AVAILABLE) {
                        activity.runOnUiThread(() -> prompt(activity, manifest));
                    }
                } catch (Exception error) {
                    LOG.e("Invalid StarFlow update manifest: " + error.getClass().getSimpleName());
                }
            }
        });
    }

    private static void prompt(Activity activity, UpdateManifest manifest) {
        String notes = manifest.releaseNotes == null ? "" : "\n" + manifest.releaseNotes;
        final TipDialog[] holder = new TipDialog[1];
        holder[0] = new TipDialog(activity,
                "发现新版本 " + manifest.versionName + notes,
                "稍后", "下载更新", new TipDialog.OnListener() {
            @Override public void left() { holder[0].dismiss(); }
            @Override public void right() {
                holder[0].dismiss();
                download(activity, manifest);
            }
            @Override public void cancel() { holder[0].dismiss(); }
        });
        holder[0].show();
    }

    private static void download(Activity activity, UpdateManifest manifest) {
        Request request = new Request.Builder().url(manifest.apk.url).get().build();
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
                            || body.contentLength() != manifest.apk.size) return;
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
                    if (copied != manifest.apk.size
                            || !hex(digest.digest()).equalsIgnoreCase(manifest.apk.sha256)) {
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
