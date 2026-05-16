package ro.church_office.teamleaf.desktop;

import java.awt.Desktop;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("desktop")
public class DesktopBrowserLauncher {

    private final boolean openBrowserOnStart;
    private final String baseUrl;
    private final int serverPort;
    private final String startupPath;

    public DesktopBrowserLauncher(
            @Value("${ministryadmin.desktop.open-browser-on-start:true}") boolean openBrowserOnStart,
            @Value("${ministryadmin.desktop.base-url:http://localhost}") String baseUrl,
            @Value("${server.port:8080}") int serverPort,
            @Value("${ministryadmin.desktop.startup-path:/dashboard}") String startupPath) {
        this.openBrowserOnStart = openBrowserOnStart;
        this.baseUrl = baseUrl;
        this.serverPort = serverPort;
        this.startupPath = startupPath;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (!openBrowserOnStart) {
            return;
        }
        String url = startupUrl();

        boolean opened = tryMacOpenCommand(url);
        if (!opened) {
            opened = tryDesktopBrowse(url);
        }
        if (opened) {
            return;
        }
    }

    private String startupUrl() {
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = startupPath == null || startupPath.isBlank()
                ? "/dashboard"
                : (startupPath.startsWith("/") ? startupPath : "/" + startupPath);
        if (hasExplicitPort(normalizedBase)) {
            return normalizedBase + normalizedPath;
        }
        if (isLocalHostBase(normalizedBase)) {
            return normalizedBase + ":" + serverPort + normalizedPath;
        }
        return normalizedBase + normalizedPath;
    }

    private boolean hasExplicitPort(String urlBase) {
        try {
            URI uri = new URI(urlBase);
            return uri.getPort() != -1;
        } catch (URISyntaxException ex) {
            return false;
        }
    }

    private boolean isLocalHostBase(String urlBase) {
        try {
            URI uri = new URI(urlBase);
            String host = uri.getHost();
            if (host == null) {
                return false;
            }
            String normalized = host.toLowerCase(Locale.ROOT);
            return "localhost".equals(normalized) || "127.0.0.1".equals(normalized);
        } catch (URISyntaxException ex) {
            return false;
        }
    }

    private boolean tryDesktopBrowse(String url) {
        if (!Desktop.isDesktopSupported()) {
            return false;
        }
        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.BROWSE)) {
            return false;
        }
        try {
            desktop.browse(URI.create(url));
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean tryMacOpenCommand(String url) {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (!osName.contains("mac")) {
            return false;
        }
        try {
            Process process = new ProcessBuilder("/usr/bin/open", url).start();
            boolean finished = process.waitFor(2, TimeUnit.SECONDS);
            if (!finished) {
                return true;
            }
            return process.exitValue() == 0;
        } catch (Exception ignored) {
            return false;
        }
    }
}
