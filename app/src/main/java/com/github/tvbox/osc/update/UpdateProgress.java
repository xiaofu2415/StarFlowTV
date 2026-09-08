package com.github.tvbox.osc.update;

public final class UpdateProgress {
    private UpdateProgress() {
    }

    public static int percent(long downloaded, long total) {
        if (downloaded <= 0 || total <= 0) return 0;
        if (downloaded >= total) return 100;
        return (int) Math.max(0, Math.min(100, (downloaded * 100L) / total));
    }
}
