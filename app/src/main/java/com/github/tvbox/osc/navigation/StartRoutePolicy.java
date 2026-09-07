package com.github.tvbox.osc.navigation;

public final class StartRoutePolicy {
    public StartDestination resolve(boolean hasLiveConfig, boolean recoveryMode) {
        if (recoveryMode) {
            return StartDestination.HOME;
        }
        return hasLiveConfig ? StartDestination.LIVE : StartDestination.SETTINGS;
    }
}
