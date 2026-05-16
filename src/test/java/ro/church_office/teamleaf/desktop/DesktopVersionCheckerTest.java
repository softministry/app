package ro.church_office.teamleaf.desktop;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DesktopVersionCheckerTest {

    @Test
    void parsesGitLabLatestReleasePayload() throws IOException {
        DesktopVersionChecker.VersionPayload payload = DesktopVersionChecker.parsePayload("""
                {
                  "tag_name": "v1.2.3",
                  "name": "Release 1.2.3",
                  "web_url": "https://gitlab.example.com/group/project/-/releases/v1.2.3"
                }
                """, URI.create("https://gitlab.example.com/api/v4/projects/1/releases/permalink/latest"));

        assertEquals("1.2.3", payload.version());
        assertEquals(URI.create("https://gitlab.example.com/group/project/-/releases/v1.2.3"), payload.releaseUri());
    }

    @Test
    void parsesPlainTextVersionPayload() throws IOException {
        DesktopVersionChecker.VersionPayload payload = DesktopVersionChecker.parsePayload(
                "v2.0.0",
                URI.create("https://gitlab.example.com/version.txt"));

        assertEquals("2.0.0", payload.version());
    }

    @Test
    void comparesSemanticVersions() {
        assertTrue(DesktopVersionChecker.isNewerVersion("1.0.1", "1.0.0"));
        assertTrue(DesktopVersionChecker.isNewerVersion("1.1.0", "1.0.9"));
        assertTrue(DesktopVersionChecker.isNewerVersion("1.0.0", "1.0.0-SNAPSHOT"));
        assertFalse(DesktopVersionChecker.isNewerVersion("1.0.0", "1.0.0"));
        assertFalse(DesktopVersionChecker.isNewerVersion("0.9.9", "1.0.0"));
    }
}
