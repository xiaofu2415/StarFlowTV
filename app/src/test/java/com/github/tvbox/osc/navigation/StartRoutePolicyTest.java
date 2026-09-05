package com.github.tvbox.osc.navigation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class StartRoutePolicyTest {
    private final StartRoutePolicy policy = new StartRoutePolicy();

    @Test
    public void liveConfigStartsLive() {
        assertEquals(StartDestination.LIVE, policy.resolve(true, false));
    }

    @Test
    public void missingLiveConfigOpensSettings() {
        assertEquals(StartDestination.SETTINGS, policy.resolve(false, false));
    }

    @Test
    public void recoveryModeKeepsHomeAvailable() {
        assertEquals(StartDestination.HOME, policy.resolve(true, true));
    }
}
