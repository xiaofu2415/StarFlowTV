package com.github.tvbox.osc.security;

import org.junit.Test;

import java.util.Base64;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class Ed25519VerifierTest {
    @Test public void verifiesRfc8032VectorAndRejectsTampering() {
        String publicKey = Base64.getEncoder().encodeToString(hex(
                "d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a"));
        String signature = Base64.getEncoder().encodeToString(hex(
                "e5564300c360ac729086e2cc806e828a84877f1eb8e5d974d873e06522490155" +
                "5fb8821590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b"));
        assertTrue(Ed25519Verifier.verify(new byte[0], signature, publicKey));
        assertFalse(Ed25519Verifier.verify(new byte[]{1}, signature, publicKey));
        assertFalse(Ed25519Verifier.verify(new byte[0], "bad", publicKey));
    }

    private static byte[] hex(String value) {
        byte[] result = new byte[value.length() / 2];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) Integer.parseInt(value.substring(i * 2, i * 2 + 2), 16);
        }
        return result;
    }
}
