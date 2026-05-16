package ro.church_office.teamleaf.desktop;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class DesktopVersionChecker {
    static final String UPDATE_URL_PROPERTY = "ministryadmin.desktop.update-url";
    static final String UPDATE_URL_ENV = "CHURCH_ADMINISTRATION_PLATFORM_DESKTOP_UPDATE_URL";
    static final String LEGACY_UPDATE_URL_ENV = "MINISTRYADMIN_DESKTOP_UPDATE_URL";
    static final String CURRENT_VERSION_PROPERTY = "ministryadmin.desktop.current-version";

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(4);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);
    private static final Pattern TAG_NAME = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern NAME = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern VERSION = Pattern.compile("\"version\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern WEB_URL = Pattern.compile("\"(?:web_url|html_url)\"\\s*:\\s*\"([^\"]+)\"");

    private final HttpClient httpClient;
    private final String currentVersion;

    DesktopVersionChecker() {
        this(HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build(), resolveCurrentVersion());
    }

    DesktopVersionChecker(HttpClient httpClient, String currentVersion) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.currentVersion = normalizeVersion(Objects.requireNonNullElse(currentVersion, "0.0.0"));
    }

    Optional<URI> resolveUpdateUri() {
        String configured = firstNonBlank(
                System.getProperty(UPDATE_URL_PROPERTY),
                System.getenv(UPDATE_URL_ENV),
                System.getenv(LEGACY_UPDATE_URL_ENV));
        if (configured == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(new URI(configured));
        } catch (URISyntaxException ex) {
            return Optional.empty();
        }
    }

    VersionCheckResult check(URI updateUri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(updateUri)
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json,text/plain;q=0.9,*/*;q=0.8")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Serverul de update a raspuns cu HTTP " + response.statusCode() + ".");
        }
        VersionPayload payload = parsePayload(response.body(), updateUri);
        boolean newer = isNewerVersion(payload.version(), currentVersion);
        return new VersionCheckResult(currentVersion, payload.version(), payload.releaseUri(), newer);
    }

    static VersionPayload parsePayload(String body, URI fallbackUri) throws IOException {
        String text = body == null ? "" : body.trim();
        String version = firstRegexMatch(text, TAG_NAME);
        if (version == null) {
            version = firstRegexMatch(text, VERSION);
        }
        if (version == null) {
            version = firstRegexMatch(text, NAME);
        }
        if (version == null && !text.startsWith("{") && !text.startsWith("[") && !text.contains("\n")) {
            version = text;
        }
        if (version == null || version.isBlank()) {
            throw new IOException("Raspunsul de update nu contine o versiune.");
        }
        String releaseUrl = firstRegexMatch(text, WEB_URL);
        URI releaseUri = fallbackUri;
        if (releaseUrl != null) {
            try {
                releaseUri = new URI(releaseUrl.replace("\\/", "/"));
            } catch (URISyntaxException ignored) {
                releaseUri = fallbackUri;
            }
        }
        return new VersionPayload(normalizeVersion(version), releaseUri);
    }

    static boolean isNewerVersion(String candidate, String current) {
        ParsedVersion candidateVersion = ParsedVersion.parse(candidate);
        ParsedVersion currentVersion = ParsedVersion.parse(current);
        int numericCompare = candidateVersion.compareNumbers(currentVersion);
        if (numericCompare != 0) {
            return numericCompare > 0;
        }
        return !candidateVersion.preRelease() && currentVersion.preRelease();
    }

    static String resolveCurrentVersion() {
        String explicit = System.getProperty(CURRENT_VERSION_PROPERTY);
        if (explicit != null && !explicit.isBlank()) {
            return normalizeVersion(explicit);
        }
        Package pkg = DesktopVersionChecker.class.getPackage();
        if (pkg != null && pkg.getImplementationVersion() != null && !pkg.getImplementationVersion().isBlank()) {
            return normalizeVersion(pkg.getImplementationVersion());
        }
        return "0.1.0";
    }

    static String normalizeVersion(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.startsWith("v") || normalized.startsWith("V")) {
            normalized = normalized.substring(1);
        }
        return normalized.isBlank() ? "0.0.0" : normalized;
    }

    private static String firstRegexMatch(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    record VersionPayload(String version, URI releaseUri) {
    }

    record VersionCheckResult(String currentVersion, String latestVersion, URI releaseUri, boolean updateAvailable) {
    }

    private record ParsedVersion(int major, int minor, int patch, boolean preRelease) {
        static ParsedVersion parse(String value) {
            String normalized = normalizeVersion(value);
            boolean preRelease = normalized.contains("-");
            String numeric = normalized.split("-", 2)[0].split("\\+", 2)[0];
            String[] parts = numeric.split("\\.");
            return new ParsedVersion(part(parts, 0), part(parts, 1), part(parts, 2), preRelease);
        }

        int compareNumbers(ParsedVersion other) {
            int majorCompare = Integer.compare(major, other.major);
            if (majorCompare != 0) {
                return majorCompare;
            }
            int minorCompare = Integer.compare(minor, other.minor);
            if (minorCompare != 0) {
                return minorCompare;
            }
            return Integer.compare(patch, other.patch);
        }

        private static int part(String[] parts, int index) {
            if (index >= parts.length) {
                return 0;
            }
            String digits = parts[index].replaceAll("[^0-9].*$", "");
            if (digits.isBlank()) {
                return 0;
            }
            try {
                return Integer.parseInt(digits);
            } catch (NumberFormatException ex) {
                return 0;
            }
        }
    }
}
