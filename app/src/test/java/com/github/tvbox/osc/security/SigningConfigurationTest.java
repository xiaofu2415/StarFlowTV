package com.github.tvbox.osc.security;

import com.github.tvbox.osc.BuildConfig;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class SigningConfigurationTest {
    @Test public void embedsRotatedProductionSigningConfiguration() {
        assertEquals("kBZZK4w6OTEWuAQVUrXPK7AVo4kgktUZEV0wBVvrDFM=",
                BuildConfig.STARFLOW_SIGNING_PUBLIC_KEY_B64);
        assertEquals("starflow-production-2026-09-r1",
                BuildConfig.STARFLOW_SIGNING_KEY_ID);
    }
}
