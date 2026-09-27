package com.github.tvbox.osc.official;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class OfficialLiveQualityPreferenceTest {
    @Test public void unsetQualityDefaultsToAdaptiveMode() {
        assertEquals("auto", OfficialLiveQualityPreference.DEFAULT);
        assertEquals("auto", OfficialLiveQualityPreference.normalize(null));
        assertEquals("auto", OfficialLiveQualityPreference.normalize(""));
    }

    @Test public void keepsExplicitUserQualityChoices() {
        assertEquals("highest", OfficialLiveQualityPreference.normalize("highest"));
        assertEquals("1080p", OfficialLiveQualityPreference.normalize("1080p"));
        assertEquals("720p", OfficialLiveQualityPreference.normalize("720p"));
        assertEquals("smooth", OfficialLiveQualityPreference.normalize("smooth"));
    }

    @Test public void unsupportedPreferenceFallsBackToAdaptiveMode() {
        assertEquals("auto", OfficialLiveQualityPreference.normalize("invalid"));
    }
}
