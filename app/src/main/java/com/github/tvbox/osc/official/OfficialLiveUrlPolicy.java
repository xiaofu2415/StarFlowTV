package com.github.tvbox.osc.official;

import java.net.URI;
import java.util.Locale;

public final class OfficialLiveUrlPolicy {
    private static final String[] ALLOWED_RESOURCE_DOMAINS = {
            "cctv.com",
            "cctvpic.com",
            "cntv.cn",
            "myalicdn.com",
            "bdydns.com",
            "wscdns.com",
            "myhwcdn.cn",
            "kcdnvip.com",
            "myqcloud.com"
    };

    private OfficialLiveUrlPolicy() {
    }

    public static boolean isAllowedPageUrl(String value) {
        if (value == null || value.trim().isEmpty()) return false;
        try {
            URI uri = new URI(value.trim());
            String rawPath = uri.getRawPath();
            return "https".equalsIgnoreCase(uri.getScheme())
                    && "tv.cctv.com".equalsIgnoreCase(uri.getHost())
                    && uri.getPort() == -1
                    && uri.getUserInfo() == null
                    && uri.getRawQuery() == null
                    && uri.getRawFragment() == null
                    && rawPath != null
                    && rawPath.startsWith("/live/")
                    && rawPath.indexOf('%') == -1
                    && rawPath.indexOf('\\') == -1
                    && rawPath.equals(uri.normalize().getRawPath());
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean isAllowedNavigationUrl(String value) {
        return isAllowedPageUrl(value);
    }

    public static boolean isAllowedResourceHost(String value) {
        if (value == null || value.trim().isEmpty()) return false;
        String host = value.trim().toLowerCase(Locale.US);
        for (String allowedDomain : ALLOWED_RESOURCE_DOMAINS) {
            if (isHostOrSubdomain(host, allowedDomain)) return true;
        }
        return false;
    }

    public static boolean isAllowedResourceUrl(String value) {
        if (value == null || value.trim().isEmpty()) return false;
        try {
            URI uri = new URI(value.trim());
            // The official CCTV player still publishes some legacy http:// HLS,
            // API, and worker URLs. Main-frame navigation remains HTTPS-only;
            // mixed resources are limited to the allowlisted official/CDN hosts.
            return ("https".equalsIgnoreCase(uri.getScheme())
                    || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getPort() == -1
                    && uri.getUserInfo() == null
                    && isAllowedResourceHost(uri.getHost());
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean isHostOrSubdomain(String host, String allowedDomain) {
        return host.equals(allowedDomain) || host.endsWith("." + allowedDomain);
    }
}
