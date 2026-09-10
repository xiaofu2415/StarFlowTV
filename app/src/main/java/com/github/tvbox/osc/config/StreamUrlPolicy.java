package com.github.tvbox.osc.config;

import java.net.URI;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates stream URLs that are safe to persist in a remote live configuration.
 *
 * <p>The upstream M3U contains a small number of public playback parameters.
 * They are not credentials and must remain part of the URL. Unknown and
 * authentication-like parameters are rejected instead of being copied into the
 * signed client configuration.</p>
 */
public final class StreamUrlPolicy {
    private static final Set<String> PROTOCOLS = set("http", "https", "rtsp", "rtp", "udp");
    private static final Pattern DIGITS = Pattern.compile("^[0-9]+$");
    private static final Pattern PUBLIC_ID = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");

    private StreamUrlPolicy() {
    }

    public static boolean isSafeStreamUrl(String value, String protocol) {
        if (value == null || protocol == null) return false;
        try {
            URI uri = new URI(value);
            String scheme = lower(uri.getScheme());
            String expectedProtocol = lower(protocol.trim());
            return PROTOCOLS.contains(scheme)
                    && scheme.equals(expectedProtocol)
                    && uri.getHost() != null
                    && uri.getUserInfo() == null
                    && isSafeQuery(uri.getRawQuery())
                    && uri.getRawFragment() == null;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean isSafeQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isEmpty()) return true;
        String[] parameters = rawQuery.split("&", -1);
        for (String parameter : parameters) {
            if (parameter.isEmpty()) return false;
            String[] pair = parameter.split("=", 2);
            if (pair.length != 2 || pair[0].isEmpty()) return false;
            try {
                String key = URLDecoder.decode(pair[0], "UTF-8").toLowerCase(Locale.US);
                String value = URLDecoder.decode(pair[1], "UTF-8");
                if (!isAllowedPublicParameter(key, value)) return false;
            } catch (Exception ignored) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAllowedPublicParameter(String key, String value) {
        if ("key".equals(key)) return "txiptv".equalsIgnoreCase(value);
        if ("playlive".equals(key)) return "0".equals(value) || "1".equals(value);
        if ("authid".equals(key)) return DIGITS.matcher(value).matches();
        if ("id".equals(key)) return PUBLIC_ID.matcher(value).matches();
        return false;
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.US);
    }

    private static Set<String> set(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }
}
