package ro.church_office.teamleaf.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HybridPasswordEncoderTest {

    private final HybridPasswordEncoder encoder = new HybridPasswordEncoder();

    @Test
    void encodeAndMatchBcrypt() {
        String hash = encoder.encode("secret-pass");

        assertNotEquals("secret-pass", hash);
        assertTrue(encoder.matches("secret-pass", hash));
        assertFalse(encoder.matches("wrong", hash));
    }

    @Test
    void matchesLegacyPlainText() {
        assertTrue(encoder.matches("legacy", "legacy"));
        assertFalse(encoder.matches("legacy", "legacy2"));
    }

    @Test
    void matchesReturnsFalseForNullEncoded() {
        assertFalse(encoder.matches("anything", null));
    }

    @Test
    void upgradeEncodingRequiredForLegacyPlainText() {
        assertTrue(encoder.upgradeEncoding("legacy"));
    }
}
