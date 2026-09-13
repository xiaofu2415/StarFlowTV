package com.github.tvbox.osc.security;

import com.github.tvbox.osc.BuildConfig;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class SigningConfigurationTest {
    @Test public void embedsRotatedProductionSigningConfiguration() {
        assertEquals("/HYjh73wFcp2EYVi6VekEtQ5mWASjCCRSx3rWo/WkyU=",
                BuildConfig.STARFLOW_SIGNING_PUBLIC_KEY_B64);
        assertEquals("starflow-production-2026-09-r2",
                BuildConfig.STARFLOW_SIGNING_KEY_ID);
    }
}
