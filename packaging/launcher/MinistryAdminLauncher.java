package ro.church_office.launcher;

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
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public final class MinistryAdminLauncher {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_LOG_CHARS = 200_000;
    private static final String DEFAULT_APP_BASE_URL = "http://localhost:8080";
    private static final String SERVER_MODE_ARG = "--server-mode";
    private static final String LAUNCHER_HOME_ARG_PREFIX = "--launcher-home=";

    private final JFrame frame = new JFrame("Church Administration Platform Launcher");
    private final JTextArea logs = new JTextArea(24, 110);
    private final JLabel status = new JLabel("Status: oprit");
    private final JLabel dbNameLabel = new JLabel("Bază de date: -");
    private final JLabel dbVersionLabel = new JLabel("Versiune DB: -");
    private final JLabel dbUserLabel = new JLabel("User DB: -");
    private final JLabel dbPasswordLabel = new JLabel("Parolă DB: -");
    private final JButton startButton = new JButton("Start");
    private final JButton stopButton = new JButton("Stop");
    private final JButton browserButton = new JButton("Deschide browser");
    private final JButton adminButton = new JButton("Admin");
    private volatile Process childProcess;

    public static void main(String[] args) {
        if (isServerMode(args)) {
            runServerMode(args);
            return;
        }
        SwingUtilities.invokeLater(() -> new MinistryAdminLauncher().show());
    }

    private static boolean isServerMode(String[] args) {
        if (args == null) {
            return false;
        }
        for (String arg : args) {
            if (SERVER_MODE_ARG.equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private static void runServerMode(String[] args) {
        String launcherHome = null;
        List<String> passThrough = new ArrayList<>();
        if (args != null) {
            for (String arg : args) {
                if (!SERVER_MODE_ARG.equals(arg)) {
                    if (arg != null && arg.startsWith(LAUNCHER_HOME_ARG_PREFIX)) {
                        launcherHome = arg.substring(LAUNCHER_HOME_ARG_PREFIX.length());
                    } else {
                        passThrough.add(arg);
                    }
                }
            }
        }
        if (launcherHome != null && !launcherHome.isBlank()) {
            try {
                Path homePath = Paths.get(launcherHome).toAbsolutePath().normalize();
                Files.createDirectories(homePath);
                Files.createDirectories(homePath.resolve("data"));
                System.setProperty("user.dir", homePath.toString());
            } catch (Exception ignored) {
                // Continue; app will log the real failure if it still cannot create folders.
            }
        }
        try {
            Class<?> jarLauncher = Class.forName("org.springframework.boot.loader.launch.JarLauncher");
            jarLauncher.getMethod("main", String[].class).invoke(null, (Object) passThrough.toArray(new String[0]));
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private MinistryAdminLauncher() {
        buildUi();
        Timer t = new Timer(800, e -> refreshButtons());
        t.start();
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

        JPanel dbInfo = new JPanel(new GridLayout(2, 2, 8, 2));
        dbInfo.add(dbNameLabel);
        dbInfo.add(dbVersionLabel);
        dbInfo.add(dbUserLabel);
        dbInfo.add(dbPasswordLabel);
        top.add(dbInfo, BorderLayout.NORTH);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.add(startButton);
        actions.add(stopButton);
        actions.add(browserButton);
        actions.add(adminButton);
        top.add(actions, BorderLayout.SOUTH);

        frame.add(top, BorderLayout.NORTH);
        frame.add(new JScrollPane(logs), BorderLayout.CENTER);

        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());
        browserButton.addActionListener(e -> openBrowser());
        adminButton.addActionListener(e -> showBuildDmgCommand());

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
            String appPath = System.getProperty("jpackage.app-path");
            if (appPath == null || appPath.isBlank()) {
                throw new IllegalStateException("Lipsește jpackage.app-path.");
            }
            Path appExecutable = Paths.get(appPath).toAbsolutePath().normalize();
            Path contentsDir = appExecutable.getParent() == null ? null : appExecutable.getParent().getParent();
            if (contentsDir == null) {
                throw new IllegalStateException("Nu pot determina Contents dir pentru app.");
            }
            Path javaBin = contentsDir.resolve("runtime/Contents/Home/bin/java");
            Path appDir = contentsDir.resolve("app");
            Path bootJar = resolveBootJar(appDir);
            if (!Files.isRegularFile(javaBin)) {
                throw new IllegalStateException("Nu găsesc java runtime: " + javaBin);
            }
            if (!Files.isRegularFile(bootJar)) {
                throw new IllegalStateException("Nu găsesc jar aplicație: " + bootJar);
            }

            String dbPath = System.getenv().getOrDefault(
                    "SQLITE_DB_PATH",
                    System.getProperty("user.home") + "/ChurchAdministrationPlatform/data/ministryadmin.sqlite.db");
            String dbUser = System.getenv().getOrDefault("DB_USER", "N/A (SQLite)");
            String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "N/A (SQLite)");
            String homeDir = System.getenv().getOrDefault(
                    "CHURCH_ADMINISTRATION_PLATFORM_HOME",
                    System.getenv().getOrDefault(
                            "MINISTRYADMIN_HOME",
                            System.getProperty("user.home") + "/ChurchAdministrationPlatform"));
            Path homePath = Paths.get(homeDir).toAbsolutePath().normalize();
            Files.createDirectories(homePath);
            Files.createDirectories(homePath.resolve("data"));
            Files.createDirectories(homePath.resolve("uploads"));
            Files.createDirectories(homePath.resolve("logs"));
            Files.createDirectories(homePath.resolve("backups"));

            List<String> cmd = new ArrayList<>();
            cmd.add(javaBin.toString());
            cmd.add("-Dfile.encoding=UTF-8");
            cmd.add("-jar");
            cmd.add(bootJar.toString());
            cmd.add("--spring.profiles.active=desktop,sqlite");
            cmd.add("--spring.datasource.url=jdbc:sqlite:" + dbPath);
            cmd.add("--ministryadmin.desktop.home-dir=" + homePath);
            cmd.add("--ministryadmin.desktop.data-dir=" + homePath.resolve("data"));
            cmd.add("--ministryadmin.desktop.uploads-dir=" + homePath.resolve("uploads"));
            cmd.add("--ministryadmin.desktop.logs-dir=" + homePath.resolve("logs"));
            cmd.add("--ministryadmin.desktop.backups-dir=" + homePath.resolve("backups"));

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            pb.directory(new File(homePath.toString()));
            childProcess = pb.start();

            appendLog("Pornire server...");
            appendLog("SQLite DB: " + dbPath);
            appendLog("Home dir: " + homePath);
            updateDbInfo(dbPath, dbUser, dbPassword);
            setStatus("rulează");

            Thread streamReader = new Thread(() -> pumpLogs(childProcess), "minadmin-log-reader");
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
            }, "minadmin-exit-watcher");
            waiter.setDaemon(true);
            waiter.start();
        } catch (Exception ex) {
            appendLog("Eroare la pornire: " + ex.getMessage());
            setStatus("eroare");
        }
        refreshButtons();
    }

    private Path resolveBootJar(Path appDir) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(appDir, "ministryadmin-web-*.jar")) {
            Path found = null;
            for (Path candidate : stream) {
                String name = candidate.getFileName().toString();
                if (name.endsWith(".jar.original")) {
                    continue;
                }
                found = candidate;
                break;
            }
            if (found != null) {
                return found;
            }
        }
        throw new IllegalStateException("Nu găsesc jar aplicație în: " + appDir);
    }

    private synchronized void stopServer() {
        if (!isRunning()) {
            appendLog("Serverul nu rulează.");
            return;
        }
        appendLog("Oprire server...");
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

    private void appendLog(String msg) {
        String line = "[" + LocalDateTime.now().format(TS) + "] " + msg + "\n";
        SwingUtilities.invokeLater(() -> {
            logs.append(line);
            String txt = logs.getText();
            if (txt.length() > MAX_LOG_CHARS) {
                logs.setText(txt.substring(txt.length() - MAX_LOG_CHARS));
            }
            logs.setCaretPosition(logs.getDocument().getLength());
        });
    }

    private void updateDbInfo(String dbPath, String dbUser, String dbPassword) {
        String dbVersion = detectSqliteVersion(dbPath);
        SwingUtilities.invokeLater(() -> {
            dbNameLabel.setText("Bază de date: " + dbPath);
            dbVersionLabel.setText("Versiune DB: " + dbVersion);
            dbUserLabel.setText("User DB: " + dbUser);
            dbPasswordLabel.setText("Parolă DB: " + dbPassword);
        });
    }

    private String detectSqliteVersion(String dbPath) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("select sqlite_version()")) {
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (Exception ignored) {
            // keep fallback
        }
        return "necunoscută";
    }

    private void showBuildDmgCommand() {
        String cmd = "cd /Users/daniel_cerna/administrare_bisericeasca && ./packaging/build-macos-dmg-sqlite-launcher.sh";
        JTextArea area = new JTextArea(cmd, 4, 80);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setCaretPosition(0);
        JScrollPane pane = new JScrollPane(area);
        javax.swing.JOptionPane.showMessageDialog(
                frame,
                pane,
                "Comandă generare DMG",
                javax.swing.JOptionPane.INFORMATION_MESSAGE
        );
    }
}
