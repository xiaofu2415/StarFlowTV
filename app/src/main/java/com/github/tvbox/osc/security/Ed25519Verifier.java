package com.github.tvbox.osc.security;

import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;

import java.security.MessageDigest;

public final class Ed25519Verifier {
    private Ed25519Verifier() {
    }

    public static boolean verify(byte[] payload, String signatureBase64, String publicKeyBase64) {
        try {
            byte[] signature = decodeBase64(signatureBase64);
            byte[] publicKey = decodeBase64(publicKeyBase64);
            if (signature.length != 64 || publicKey.length != 32) return false;
            EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName("Ed25519");
            EdDSAEngine verifier = new EdDSAEngine(MessageDigest.getInstance(spec.getHashAlgorithm()));
            verifier.initVerify(new EdDSAPublicKey(new EdDSAPublicKeySpec(publicKey, spec)));
            verifier.update(payload);
            return verifier.verify(signature);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static byte[] decodeBase64(String input) {
        if (input == null) return new byte[0];
        String value = input.trim();
        if ((value.length() & 3) != 0) return new byte[0];
        int padding = value.endsWith("==") ? 2 : value.endsWith("=") ? 1 : 0;
        byte[] output = new byte[value.length() / 4 * 3 - padding];
        int out = 0;
        for (int i = 0; i < value.length(); i += 4) {
            int a = digit(value.charAt(i)), b = digit(value.charAt(i + 1));
            int c = value.charAt(i + 2) == '=' ? 0 : digit(value.charAt(i + 2));
            int d = value.charAt(i + 3) == '=' ? 0 : digit(value.charAt(i + 3));
            if (a < 0 || b < 0 || c < 0 || d < 0) return new byte[0];
            int block = (a << 18) | (b << 12) | (c << 6) | d;
            if (out < output.length) output[out++] = (byte) (block >> 16);
            if (out < output.length) output[out++] = (byte) (block >> 8);
            if (out < output.length) output[out++] = (byte) block;
        }
        return output;
    }

    private static int digit(char value) {
        if (value >= 'A' && value <= 'Z') return value - 'A';
        if (value >= 'a' && value <= 'z') return value - 'a' + 26;
        if (value >= '0' && value <= '9') return value - '0' + 52;
        if (value == '+') return 62;
        if (value == '/') return 63;
        return -1;
    }
}
