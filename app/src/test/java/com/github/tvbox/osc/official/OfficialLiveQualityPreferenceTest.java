package com.github.tvbox.osc.official;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class OfficialLiveQualityPreferenceTest {
    @Test public void unsetQualityDefaultsToHighestQuality() {
        assertEquals("highest", OfficialLiveQualityPreference.DEFAULT);
        assertEquals("highest", OfficialLiveQualityPreference.normalize(null));
        assertEquals("highest", OfficialLiveQualityPreference.normalize(""));
    }

    @Test public void keepsExplicitUserQualityChoices() {
        assertEquals("highest", OfficialLiveQualityPreference.normalize("highest"));
        assertEquals("auto", OfficialLiveQualityPreference.normalize("auto"));
        assertEquals("1080p", OfficialLiveQualityPreference.normalize("1080p"));
        assertEquals("720p", OfficialLiveQualityPreference.normalize("720p"));
        assertEquals("smooth", OfficialLiveQualityPreference.normalize("smooth"));
    }

    @Test public void unsupportedPreferenceFallsBackToHighestQuality() {
        assertEquals("highest", OfficialLiveQualityPreference.normalize("invalid"));
    }
}
