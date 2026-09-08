package com.github.tvbox.osc.official;

/** Defers one official-page failure until the owning Activity is playable again. */
public final class OfficialLiveErrorGate {
    private boolean foreground;
    private boolean destroyed;
    private Object pendingChannel;
    private int pendingSourceIndex = -1;

    public boolean shouldHandleError(Object channel, int sourceIndex) {
        if (destroyed) return false;
        if (foreground) return true;
        if (pendingChannel == null) {
            pendingChannel = channel;
            pendingSourceIndex = sourceIndex;
        }
        return false;
    }

    public boolean onResume(Object channel, int sourceIndex) {
        if (destroyed) return false;
        foreground = true;
        boolean matches = pendingChannel != null
                && pendingChannel == channel
                && pendingSourceIndex == sourceIndex;
        clearPending();
        return matches;
    }

    public void onPause() {
        foreground = false;
    }

    public void clearPending() {
        pendingChannel = null;
        pendingSourceIndex = -1;
    }

    public void onDestroy() {
        destroyed = true;
        foreground = false;
        clearPending();
    }
}
