package ro.church_office.teamleaf.desktop;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class MinistryAdminLauncher {
    private static final String DEFAULT_DB_PATH = System.getProperty("user.home")
            + "/ChurchAdministrationPlatform/data/ministryadmin.sqlite.db";
    private static final String DEFAULT_APP_BASE_URL = "http://localhost:8080";
    private static final int MAX_LOG_CHARS = 200_000;
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JFrame frame = new JFrame("Church Administration Platform Launcher");
    private final JTextArea logs = new JTextArea(24, 110);
    private final JLabel status = new JLabel("Status: oprit");
    private final JButton startButton = new JButton("Start");
    private final JButton stopButton = new JButton("Stop");
    private final JButton browserButton = new JButton("Deschide browser");

    private volatile Process childProcess;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MinistryAdminLauncher().show());
    }

    private MinistryAdminLauncher() {
        buildUi();
        Timer stateTimer = new Timer(800, e -> refreshButtons());
        stateTimer.start();
    }

    private void buildUi() {
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(8, 8));

        logs.setEditable(false);
        logs.setLineWrap(true);
        logs.setWrapStyleWord(true);

        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        top.add(status, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.add(startButton);
        actions.add(stopButton);
        actions.add(browserButton);
        top.add(actions, BorderLayout.SOUTH);

        frame.add(top, BorderLayout.NORTH);
        frame.add(new JScrollPane(logs), BorderLayout.CENTER);

        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());
        browserButton.addActionListener(e -> openBrowser());

        frame.setSize(980, 560);
        frame.setLocationRelativeTo(null);
        refreshButtons();
    }

    private void show() {
        frame.setVisible(true);
        appendLog("Launcher pornit. Pornesc aplicatia automat.");
        startServer();
    }

    private synchronized void startServer() {
        if (isRunning()) {
            appendLog("Serverul este deja pornit.");
            return;
        }
        try {
            String javaBin = Paths.get(System.getProperty("java.home"), "bin", "java").toString();
            Path appJar = resolveCurrentJar();
            String dbPath = System.getenv().getOrDefault("SQLITE_DB_PATH", DEFAULT_DB_PATH);

            List<String> cmd = new ArrayList<>();
            cmd.add(javaBin);
            cmd.add("-Dspring.profiles.active=desktop,sqlite");
            cmd.add("-Dspring.datasource.url=jdbc:sqlite:" + dbPath);
            cmd.add("-Dfile.encoding=UTF-8");
            cmd.add("-jar");
            cmd.add(appJar.toString());

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            childProcess = pb.start();

            appendLog("Pornire server: " + String.join(" ", cmd));
            appendLog("SQLite DB: " + dbPath);
            setStatus("rulează");

            Thread streamReader = new Thread(() -> pumpLogs(childProcess), "ministryadmin-log-reader");
            streamReader.setDaemon(true);
            streamReader.start();

            Thread waiter = new Thread(() -> {
                try {
                    int code = childProcess.waitFor();
                    appendLog("Server oprit. Exit code=" + code);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } finally {
                    childProcess = null;
                    setStatus("oprit");
                }
            }, "ministryadmin-exit-watcher");
            waiter.setDaemon(true);
            waiter.start();
        } catch (Exception ex) {
            appendLog("Eroare la pornire: " + ex.getMessage());
            setStatus("eroare");
        }
        refreshButtons();
    }

    private synchronized void stopServer() {
        if (!isRunning()) {
            appendLog("Serverul nu rulează.");
            return;
        }
        appendLog("Trimit semnal de oprire...");
        childProcess.destroy();
        refreshButtons();
    }

    private void openBrowser() {
        try {
            if (Desktop.isDesktopSupported()) {
                String baseUrl = System.getenv().getOrDefault("APP_BASE_URL", DEFAULT_APP_BASE_URL);
                String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
                Desktop.getDesktop().browse(URI.create(normalizedBase + "/dashboard"));
            }
        } catch (Exception ex) {
            appendLog("Nu pot deschide browserul: " + ex.getMessage());
        }
    }

    private void pumpLogs(Process process) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                appendLog(line);
            }
        } catch (IOException ex) {
            appendLog("Flux log închis: " + ex.getMessage());
        }
    }

    private boolean isRunning() {
        Process p = childProcess;
        return p != null && p.isAlive();
    }

    private void refreshButtons() {
        boolean running = isRunning();
        startButton.setEnabled(!running);
        stopButton.setEnabled(running);
    }

    private void setStatus(String value) {
        SwingUtilities.invokeLater(() -> status.setText("Status: " + value));
    }

    private void appendLog(String text) {
        String line = "[" + LocalDateTime.now().format(TS) + "] " + text + "\n";
        SwingUtilities.invokeLater(() -> {
            logs.append(line);
            if (logs.getText().length() > MAX_LOG_CHARS) {
                logs.setText(logs.getText().substring(logs.getText().length() - MAX_LOG_CHARS));
            }
            logs.setCaretPosition(logs.getDocument().getLength());
        });
    }

    private Path resolveCurrentJar() throws URISyntaxException {
        URI uri = MinistryAdminLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        Path path = Paths.get(uri);
        if (path.toString().endsWith(".jar")) {
            return path;
        }
        // IDE fallback
        Path projectDir = path;
        for (int i = 0; i < 4 && projectDir != null; i++) {
            projectDir = projectDir.getParent();
        }
        if (projectDir != null) {
            Path targetJar = projectDir.resolve("target/ministryadmin-web-0.1.0.jar");
            if (targetJar.toFile().isFile()) {
                return targetJar;
            }
        }
        throw new IllegalStateException("Nu găsesc fișierul JAR al aplicației.");
    }
}
