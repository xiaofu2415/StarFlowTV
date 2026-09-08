package com.github.tvbox.osc.config;

import java.io.File;
import java.net.URI;

/** Resolves the app-private live configuration without routing it through the external file server. */
public final class LocalLiveConfigFile {
    private LocalLiveConfigFile() {}

    public static boolean isFileUrl(String value) {
        return value != null && value.trim().toLowerCase().startsWith("file:");
    }

    public static File resolve(String value) {
        if (!isFileUrl(value)) return null;
        try {
            URI uri = new URI(value.trim());
            if (!"file".equalsIgnoreCase(uri.getScheme())
                    || (uri.getAuthority() != null && !uri.getAuthority().isEmpty())) return null;
            File file = new File(uri);
            return file.isFile() ? file : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
