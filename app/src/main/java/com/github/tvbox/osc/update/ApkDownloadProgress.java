package com.github.tvbox.osc.update;

/** Converts downloaded APK byte counts into a bounded percentage for the OTA UI. */
public final class ApkDownloadProgress {
    private ApkDownloadProgress() {
    }

    public static int percent(long downloadedBytes, long totalBytes) {
        if (downloadedBytes <= 0 || totalBytes <= 0) return 0;
        if (downloadedBytes >= totalBytes) return 100;
        return (int) ((double) downloadedBytes * 100.0d / (double) totalBytes);
    }
}
