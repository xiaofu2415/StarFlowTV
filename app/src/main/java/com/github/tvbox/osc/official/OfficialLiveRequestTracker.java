package com.github.tvbox.osc.official;

/**
 * Tracks only the active main-document URL. The controller must reject callbacks
 * from other WebView instances before using this tracker; URLs are not load identities.
 */
public final class OfficialLiveRequestTracker {
    private volatile String currentUrl;

    public void begin(String url) {
        currentUrl = url;
    }

    public boolean stop() {
        boolean wasActive = isActive();
        currentUrl = null;
        return wasActive;
    }

    public boolean isActive() {
        return currentUrl != null;
    }

    public boolean transition(String targetUrl) {
        if (!isActive() || !OfficialLiveUrlPolicy.isAllowedNavigationUrl(targetUrl)) return false;
        currentUrl = targetUrl;
        return true;
    }

    public boolean matches(String url) {
        String activeUrl = currentUrl;
        return activeUrl != null && activeUrl.equals(url);
    }
}
