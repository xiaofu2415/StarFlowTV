package com.github.tvbox.osc.official;

/** Quality preference used only by the built-in official WebView player. */
public final class OfficialLiveQualityPreference {
    public static final String DEFAULT = "auto";

    private OfficialLiveQualityPreference() {
    }

    public static String normalize(String preference) {
        if ("highest".equals(preference) || "auto".equals(preference)
                || "1080p".equals(preference) || "720p".equals(preference)
                || "smooth".equals(preference)) {
            return preference;
        }
        return DEFAULT;
    }
}
