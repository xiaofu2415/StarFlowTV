package com.github.tvbox.osc.update;

import java.net.URI;

public final class DistributionEndpoints {
    private DistributionEndpoints() {
    }

    public static String resolveLiveUrl(String current, String builtIn) {
        if (current != null && !current.trim().isEmpty()) {
            return current.trim();
        }
        return isSafeHttps(builtIn) ? builtIn.trim() : "";
    }

    public static boolean isSafeHttps(String value) {
        if (value == null || value.trim().isEmpty()) return false;
        try {
            URI uri = new URI(value.trim());
            return "https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null
                    && uri.getUserInfo() == null
                    && uri.getRawQuery() == null
                    && uri.getRawFragment() == null;
        } catch (Exception ignored) {
            return false;
        }
    }
}
