package com.github.tvbox.osc.config;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class RemoteConfigManagerTest {
    @Test
    public void manualRefreshReloadsWhenConfigWasUpdatedOrAlreadyCurrent() {
        assertTrue(RemoteConfigManager.CheckResult.UPDATED.requiresLiveReload());
        assertTrue(RemoteConfigManager.CheckResult.NO_UPDATE.requiresLiveReload());
    }

    @Test
    public void manualRefreshDoesNotReloadAfterFailureOrConcurrentRequest() {
        assertFalse(RemoteConfigManager.CheckResult.FAILED.requiresLiveReload());
        assertFalse(RemoteConfigManager.CheckResult.BUSY.requiresLiveReload());
    }
}
