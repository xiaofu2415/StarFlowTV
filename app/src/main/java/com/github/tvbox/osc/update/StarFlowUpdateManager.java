package com.github.tvbox.osc.update;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.widget.Toast;

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
import java.util.concurrent.atomic.AtomicInteger;

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
        check(activity, null);
    }

    public static void checkNow(Activity activity) {
        Toast.makeText(activity, "正在检查更新…", Toast.LENGTH_SHORT).show();
        check(activity, result -> {
            if (result.status == UpdateCheckResult.Status.UPDATE_AVAILABLE) return;
            Toast.makeText(activity, UpdateCheckResult.message(result.status, result.versionName),
                    Toast.LENGTH_SHORT).show();
        });
    }

    private static void check(Activity activity, CheckListener listener) {
        final String manifestUrl = BuildConfig.STARFLOW_UPDATE_URL;
        if (!DistributionEndpoints.isSafeHttps(manifestUrl)) {
            complete(activity, listener, UpdateCheckResult.Status.INVALID_MANIFEST, null);
            return;
        }
        if (!CHECK_STARTED.compareAndSet(false, true)) {
            complete(activity, listener, UpdateCheckResult.Status.BUSY, null);
            return;
        }
        fetch(manifestUrl, MAX_MANIFEST_BYTES, manifestBytes -> {
            if (manifestBytes == null) {
                CHECK_STARTED.set(false);
                complete(activity, listener, UpdateCheckResult.Status.NETWORK_ERROR, null);
                return;
            }
            fetch(sibling(manifestUrl, "latest.sig"), 2048, signatureBytes -> {
                if (signatureBytes == null) {
                    CHECK_STARTED.set(false);
                    complete(activity, listener, UpdateCheckResult.Status.NETWORK_ERROR, null);
                    return;
                }
                String signature = signatureBytes == null ? "" :
                        new String(signatureBytes, StandardCharsets.US_ASCII).trim();
                if (!Ed25519Verifier.verify(manifestBytes, signature,
                        BuildConfig.STARFLOW_SIGNING_PUBLIC_KEY_B64)) {
                    CHECK_STARTED.set(false);
                    complete(activity, listener, UpdateCheckResult.Status.SIGNATURE_INVALID, null);
                    return;
                }
                try {
                    UpdateManifest manifest = UpdateManifest.parse(
                            new String(manifestBytes, StandardCharsets.UTF_8));
                    String channel = Hawk.get(HawkConfig.STARFLOW_UPDATE_CHANNEL,
                            BuildConfig.STARFLOW_UPDATE_CHANNEL);
                    UpdateDecision decision = UpdatePolicy.evaluate(
                            DefaultConfig.getAppVersionCode(activity), Build.VERSION.SDK_INT,
                            channel, BuildConfig.STARFLOW_SIGNING_KEY_ID, manifestUrl, manifest);
                    if (decision == UpdateDecision.NO_UPDATE) {
                        complete(activity, listener, UpdateCheckResult.Status.NO_UPDATE,
                                manifest.versionName);
                    } else if (decision == UpdateDecision.UNSUPPORTED) {
                        complete(activity, listener, UpdateCheckResult.Status.UNSUPPORTED,
                                manifest.versionName);
                    } else if (decision == UpdateDecision.INVALID) {
                        complete(activity, listener, UpdateCheckResult.Status.INVALID_MANIFEST,
                                manifest.versionName);
                    } else if (decision == UpdateDecision.AVAILABLE) {
                        UpdateManifest.Apk apk = AbiSelector.select(manifest, Build.SUPPORTED_ABIS);
                        if (apk == null) {
                            complete(activity, listener, UpdateCheckResult.Status.ABI_UNAVAILABLE,
                                    manifest.versionName);
                        } else {
                            complete(activity, listener, UpdateCheckResult.Status.UPDATE_AVAILABLE,
                                    manifest.versionName);
                            activity.runOnUiThread(() -> prompt(activity, manifest, apk));
                        }
                    } else {
                        complete(activity, listener, UpdateCheckResult.Status.INVALID_MANIFEST,
                                manifest.versionName);
                    }
                } catch (Exception error) {
                    LOG.e("Invalid StarFlow update manifest: " + error.getClass().getSimpleName());
                    complete(activity, listener, UpdateCheckResult.Status.INVALID_MANIFEST, null);
                } finally {
                    CHECK_STARTED.set(false);
                }
            });
        });
    }

    private static void complete(Activity activity, CheckListener listener,
                                 UpdateCheckResult.Status status, String versionName) {
        if (listener == null) return;
        activity.runOnUiThread(() -> listener.onResult(new UpdateCheckResult(status, versionName)));
    }

    private interface CheckListener {
        void onResult(UpdateCheckResult result);
    }

    private static void prompt(Activity activity, UpdateManifest manifest, UpdateManifest.Apk apk) {
        String notes = manifest.releaseNotes == null ? "" : "\n" + manifest.releaseNotes;
        final boolean mandatory = manifest.mandatory || manifest.forceUpdate;
        final TipDialog[] holder = new TipDialog[1];
        holder[0] = new TipDialog(activity,
                "发现新版本 " + manifest.versionName + notes,
                "稍后", "下载更新", new TipDialog.OnListener() {
            @Override public void left() { if (!mandatory) holder[0].dismiss(); }
            @Override public void right() {
                holder[0].dismiss();
                download(activity, manifest, apk);
            }
            @Override public void cancel() { if (!mandatory) holder[0].dismiss(); }
        });
        holder[0].setCancelable(!mandatory);
        holder[0].show();
    }

    private static void download(Activity activity, UpdateManifest manifest, UpdateManifest.Apk apk) {
        activity.runOnUiThread(() -> downloadOnUiThread(activity, apk));
    }

    private static void downloadOnUiThread(Activity activity, UpdateManifest.Apk apk) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        ProgressDialog progress = new ProgressDialog(activity);
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setTitle("正在下载更新");
        progress.setMessage("准备下载…");
        progress.setMax(100);
        progress.setProgress(0);
        progress.setIndeterminate(false);
        progress.setCancelable(false);
        progress.show();

        AtomicInteger lastReportedPercent = new AtomicInteger(-1);
        Request request = new Request.Builder().url(apk.downloadUrl).get().build();
        OkHttp.client().newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, java.io.IOException error) {
                LOG.e("StarFlow APK download failed: " + error.getClass().getSimpleName());
                dismissDownloadProgress(activity, progress);
                showMessage(activity, "更新下载失败，请检查网络连接");
            }

            @Override public void onResponse(Call call, Response response) {
                File cacheRoot = activity.getExternalCacheDir();
                if (cacheRoot == null) cacheRoot = activity.getCacheDir();
                File target = new File(cacheRoot, "StarFlowTV-update.apk");
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful() || body == null
                            || body.contentLength() <= 0 || body.contentLength() > MAX_APK_BYTES
                            || body.contentLength() != apk.size) {
                        dismissDownloadProgress(activity, progress);
                        showMessage(activity, "更新文件大小校验失败");
                        return;
                    }
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
                            int percent = ApkDownloadProgress.percent(copied, apk.size);
                            if (lastReportedPercent.getAndSet(percent) != percent) {
                                long downloaded = copied;
                                updateDownloadProgress(activity, progress, percent, downloaded, apk.size);
                            }
                        }
                    }
                    setDownloadStatus(activity, progress, "下载完成，正在校验文件…", 100);
                    if (copied != apk.size || !hex(digest.digest()).equalsIgnoreCase(apk.sha256)
                            || !verifyArchive(activity, target, apk.certificateSha256)) {
                        target.delete();
                        dismissDownloadProgress(activity, progress);
                        showMessage(activity, "更新文件校验失败，已取消安装");
                        return;
                    }
                    activity.runOnUiThread(() -> {
                        if (activity.isFinishing() || activity.isDestroyed()) {
                            dismissDownloadProgress(activity, progress);
                            return;
                        }
                        setDownloadStatus(activity, progress, "校验通过，正在打开系统安装器…", 100);
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            dismissDownloadProgress(activity, progress);
                            install(activity, target);
                        }, 500);
                    });
                } catch (Exception error) {
                    target.delete();
                    LOG.e("StarFlow APK verification failed: " + error.getClass().getSimpleName());
                    dismissDownloadProgress(activity, progress);
                    showMessage(activity, "更新下载失败，请稍后重试");
                }
            }
        });
    }

    private static void updateDownloadProgress(Activity activity, ProgressDialog progress,
                                              int percent, long downloaded, long total) {
        activity.runOnUiThread(() -> {
            if (!progress.isShowing()) return;
            progress.setProgress(percent);
            progress.setMessage(String.format(Locale.getDefault(),
                    "已下载 %.1f / %.1f MB",
                    downloaded / (1024.0 * 1024.0), total / (1024.0 * 1024.0)));
        });
    }

    private static void setDownloadStatus(Activity activity, ProgressDialog progress,
                                          String message, int percent) {
        activity.runOnUiThread(() -> {
            if (!progress.isShowing()) return;
            progress.setProgress(percent);
            progress.setMessage(message);
        });
    }

    private static void dismissDownloadProgress(Activity activity, ProgressDialog progress) {
        activity.runOnUiThread(() -> {
            if (progress.isShowing()) progress.dismiss();
        });
    }

    private static void showMessage(Activity activity, String message) {
        activity.runOnUiThread(() -> Toast.makeText(activity, message, Toast.LENGTH_SHORT).show());
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
        try {
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
        } catch (Exception error) {
            LOG.e("StarFlow update request failed: " + error.getClass().getSimpleName());
            callback.complete(null);
        }
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
