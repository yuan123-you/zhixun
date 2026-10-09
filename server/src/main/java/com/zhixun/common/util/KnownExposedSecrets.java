package com.zhixun.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;

/** Fingerprints of previously public keys, retained only to reject unsafe configuration. */
final class KnownExposedSecrets {
    private KnownExposedSecrets() {}
    private static final Set<String> FINGERPRINTS = Set.of(
        "4882717c0488f4fbb99e5b74022f1ffa4d8118427b97817464177633daab7fb7",
        "7c9474816a8ca77dd777068b294110d5f96cf873b035412a80edd583e4068e08",
        "adfb5df27a48df1ebee74553b2676a43e6516471924d05249d31226a7d566a71");

    static boolean contains(String value) {
        if (value == null) return false;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return FINGERPRINTS.contains(HexFormat.of().formatHex(digest));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
