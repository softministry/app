package ro.church_office.teamleaf.integration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceContractIT {

    @Test
    void loginTemplateContainsI18nBindings() throws IOException {
        String html = Files.readString(Path.of("src/main/resources/templates/auth/login.html"));

        assertTrue(html.contains("data-i18n=\"auth.heroTitle\""));
        assertTrue(html.contains("data-i18n=\"auth.heroSubtitle\""));
        assertTrue(html.contains("auth-login-form"));
    }

    @Test
    void languageFilesContainAuthHeroKeys() throws IOException {
        String ro = Files.readString(Path.of("src/main/resources/static/i18n/ro.json"));
        String en = Files.readString(Path.of("src/main/resources/static/i18n/en.json"));

        assertTrue(ro.contains("\"heroTitle\""));
        assertTrue(ro.contains("\"heroSubtitle\""));
        assertTrue(en.contains("\"heroTitle\""));
        assertTrue(en.contains("\"heroSubtitle\""));
    }
}
