package com.github.tvbox.osc.official;

/** UI-thread gate: only the latest pending playback operation survives a pause. */
public final class LiveForegroundGate {
    private boolean foreground;
    private boolean destroyed;
    private Runnable pending;

    public void runWhenForeground(Runnable action) {
        if (destroyed) return;
        if (foreground) action.run();
        else pending = action;
    }

    public void onResume() {
        if (destroyed) return;
        foreground = true;
        Runnable action = pending;
        pending = null;
        if (action != null) action.run();
    }

    public void onPause() { foreground = false; }

    public void clearPending() { pending = null; }

    public void onDestroy() {
        destroyed = true;
        foreground = false;
        pending = null;
    }
}
