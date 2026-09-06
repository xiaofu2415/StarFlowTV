package com.github.tvbox.osc.security;

import java.security.MessageDigest;
import java.util.Locale;

public final class Sha256 {
    private Sha256() {
    }

    public static String digest(byte[] bytes) {
        try {
            byte[] result = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder value = new StringBuilder(64);
            for (byte item : result) value.append(String.format(Locale.US, "%02x", item & 0xff));
            return value.toString();
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }
}
