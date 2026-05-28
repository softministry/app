package ro.church_office.teamleaf.desktop;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import ro.church_office.teamleaf.TeamleafApplication;

public final class DesktopControlWindow {
    private static final String DEFAULT_LOCAL_URL = "http://localhost:8080/";
    private static final String DEV_LOCAL_URL = "http://localhost:85/";
    private static final Color BRAND = new Color(29, 78, 216);
    private static final Color BRAND_DARK = new Color(15, 23, 42);
    private static final Color SURFACE = new Color(248, 250, 252);
    private static final Color BACKGROUND_DARK = new Color(241, 245, 249);
    private static final Color BORDER = new Color(203, 213, 225);
    private static final Color TEXT_PRIMARY = new Color(15, 23, 42);
    private static final Color TEXT_SECONDARY = new Color(100, 116, 139);
    private static final Color MUTED = new Color(71, 85, 105);
    private static final Color SUCCESS = new Color(22, 163, 74);
    private static final Color WARNING = new Color(217, 119, 6);
    private static final Color DANGER = new Color(220, 38, 38);
    private final String[] args;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    private final AtomicBoolean updateCheckRunning = new AtomicBoolean(false);
    private final DesktopVersionChecker versionChecker = new DesktopVersionChecker();
    private volatile String applicationUrl;
    private ConfigurableApplicationContext context;
    private JFrame frame;
    private JLabel statusDetail;
    private JLabel updateDetail;
    private JProgressBar progressBar;
    private Timer startupProgressTimer;
    private Timer progressCompletionTimer;
    private int startupProgress;
    private JButton primaryActionButton;
    private LogViewerWindow logViewerWindow;

    private DesktopControlWindow(String[] args) {
        this.args = args == null ? new String[0] : args.clone();
        this.applicationUrl = resolveApplicationUrl(this.args);
    }

    public static void show(String[] args) {
        SwingUtilities.invokeLater(() -> new DesktopControlWindow(args).createAndShow());
    }

    private void createAndShow() {
        frame = new JFrame("Church Administration Platform");
        frame.setUndecorated(true);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.setMinimumSize(new Dimension(520, 112));
        frame.setSize(560, 112);
        frame.setLocationRelativeTo(null);
        frame.setIconImages(loadIconImages());
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                requestShutdown();
            }
        });

        // Create main container with custom title bar
        JPanel mainContainer = new JPanel(new BorderLayout());
        CustomTitleBar titleBar = new CustomTitleBar(frame, "Church Administration Platform");
        mainContainer.add(titleBar, BorderLayout.NORTH);
        mainContainer.add(buildContent(), BorderLayout.CENTER);
        
        frame.setContentPane(mainContainer);
        setStopped();
        frame.setVisible(true);
        startApplication();
    }

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu toolsMenu = new JMenu("Meniu");
        JMenuItem logsItem = new JMenuItem("Loguri");
        logsItem.addActionListener(event -> showLogsWindow());
        JMenuItem updateItem = new JMenuItem("Cauta update");
        updateItem.addActionListener(event -> checkForUpdatesAsync(true));
        toolsMenu.add(logsItem);
        toolsMenu.add(updateItem);
        menuBar.add(toolsMenu);
        return menuBar;
    }

    private JPanel buildContent() {
        JPanel root = new BackgroundPanel();
        root.setLayout(new BorderLayout());

        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(186, 204, 255)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 8, 0);

        JLabel title = new JLabel("Pornire aplicatie");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        title.setForeground(BRAND_DARK);
        card.add(title, c);

        c.gridy++;
        c.insets = new Insets(0, 0, 6, 0);
        statusDetail = infoLabel("");
        card.add(statusDetail, c);

        c.gridy++;
        c.insets = new Insets(0, 0, 0, 0);
        progressBar = new SmoothProgressBar();
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        progressBar.setValue(0);
        progressBar.setStringPainted(false);
        progressBar.setVisible(false);
        progressBar.setBorderPainted(false);
        progressBar.setForeground(new Color(29, 78, 216));
        progressBar.setBackground(new Color(219, 234, 254));
        progressBar.setPreferredSize(new Dimension(10, 11));
        card.add(progressBar, c);

        GridBagConstraints bodyConstraints = new GridBagConstraints();
        bodyConstraints.gridx = 0;
        bodyConstraints.gridy = 0;
        bodyConstraints.weightx = 1;
        bodyConstraints.weighty = 1;
        bodyConstraints.fill = GridBagConstraints.HORIZONTAL;
        body.add(card, bodyConstraints);

        root.add(body, BorderLayout.CENTER);
        return root;
    }

    private void onPrimaryAction() {
        if (running.get()) {
            stopApplication();
        } else {
            startApplication();
        }
    }

    private void showLogsWindow() {
        if (logViewerWindow == null || !logViewerWindow.isDisplayable()) {
            logViewerWindow = new LogViewerWindow(resolveLogFilePath(args));
        }
        logViewerWindow.showWindow(frame);
    }

    private JButton button(String text, Color background, Color foreground) {
        RoundedButton button = new RoundedButton(text, 8);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        button.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        button.setPreferredSize(new Dimension(118, 30));
        button.setMinimumSize(new Dimension(118, 30));
        button.setMaximumSize(new Dimension(118, 30));
        return button;
    }

    private JLabel infoLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.LEFT);
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        label.setForeground(BRAND_DARK);
        return label;
    }

    private void startApplication() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        setStarting();
        Thread thread = new Thread(() -> {
            try {
                context = SpringApplication.run(TeamleafApplication.class, args);
                applicationUrl = resolveApplicationUrl(context);
                SwingUtilities.invokeLater(this::setRunning);
            } catch (Exception ex) {
                running.set(false);
                SwingUtilities.invokeLater(() -> setFailed(ex));
            }
        }, "ministryadmin-spring-runner");
        thread.setDaemon(false);
        thread.start();
    }

    private void stopApplication() {
        if (!running.get()) {
            return;
        }
        setStopping();
        Thread thread = new Thread(() -> {
            try {
                if (context != null) {
                    SpringApplication.exit(context, () -> 0);
                    context.close();
                }
            } finally {
                context = null;
                running.set(false);
                SwingUtilities.invokeLater(this::setStopped);
            }
        }, "ministryadmin-spring-stopper");
        thread.setDaemon(false);
        thread.start();
    }

    private void requestShutdown() {
        if (!confirmShutdown()) {
            return;
        }
        if (!shuttingDown.compareAndSet(false, true)) {
            return;
        }
        if (!running.get()) {
            disposeAndExit();
            return;
        }
        setStopping();
        Thread thread = new Thread(() -> {
            try {
                if (context != null) {
                    SpringApplication.exit(context, () -> 0);
                    context.close();
                }
            } finally {
                context = null;
                running.set(false);
                SwingUtilities.invokeLater(this::disposeAndExit);
            }
        }, "ministryadmin-spring-shutdown");
        thread.setDaemon(false);
        thread.start();
    }

    private boolean confirmShutdown() {
        if (frame == null) {
            return true;
        }
        int choice = JOptionPane.showConfirmDialog(
                frame,
                "Sigur vrei sa inchizi aplicatia?",
                "Confirmare inchidere",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        return choice == JOptionPane.YES_OPTION;
    }

    private void disposeAndExit() {
        if (frame != null) {
            frame.dispose();
        }
        System.exit(0);
    }

    private void openBrowser() {
        if (!Desktop.isDesktopSupported()) {
            return;
        }
        try {
            Desktop.getDesktop().browse(URI.create(dashboardUrl()));
        } catch (Exception ignored) {
            // Browser integration is optional.
        }
    }

    private void setStarting() {
        setStatus("PORNIRE", WARNING, startupProgressMessage(0));
        progressBar.setVisible(true);
        progressBar.setIndeterminate(false);
        progressBar.setValue(0);
        beginStartupProgress();
        setPrimaryButtonState("Loading...", new Color(180, 83, 9), false);
    }

    private void setRunning() {
        completeStartupProgressSmooth(this::finishStartupAndCloseSplash);
    }

    private void finishStartupAndCloseSplash() {
        setStatus("RULEAZA", SUCCESS, "Aplicatia ruleaza. Dashboard-ul este disponibil la " + dashboardUrl() + ".");
        progressBar.setVisible(false);
        setPrimaryButtonState("Opreste aplicatia", new Color(153, 27, 27), true);
        openBrowser();
        // Minimize window after opening browser
        SwingUtilities.invokeLater(() -> frame.setState(JFrame.ICONIFIED));
    }

    private void setStopping() {
        stopStartupProgress();
        setStatus("OPRIRE", WARNING, "Aplicatia se opreste.");
        progressBar.setVisible(false);
        setPrimaryButtonState("Loading...", new Color(180, 83, 9), false);
    }

    private void setStopped() {
        stopStartupProgress();
        setStatus("OPRIT", MUTED, "Aplicatia este oprita. Apasa Start pentru a porni serverul local.");
        progressBar.setVisible(false);
        setPrimaryButtonState("Porneste aplicatie", BRAND, true);
    }

    private void setFailed(Exception ex) {
        stopStartupProgress();
        
        // Check if it's a port-in-use error FIRST (before translation)
        if (isPortInUseError(ex)) {
            String message = translateErrorMessage(ex);
            handlePortInUseError(message);
        } else {
            String message = translateErrorMessage(ex);
            setStatus("EROARE", DANGER, message);
            progressBar.setVisible(false);
            setPrimaryButtonState("Porneste aplicatie", BRAND, true);
            
            // Show detailed error dialog
            showErrorDialog("Eroare la pornirea aplicației", message, ex);
        }
    }
    
    /**
     * Checks if the exception is caused by port already in use.
     * Recursively checks the entire exception chain.
     */
    private boolean isPortInUseError(Exception ex) {
        if (ex == null) return false;
        
        // Check current exception
        if (isPortMessage(ex.getMessage()) || isPortExceptionClass(ex)) {
            return true;
        }
        
        // Check cause recursively
        Throwable cause = ex.getCause();
        int depth = 0;
        while (cause != null && depth < 10) {
            if (isPortMessage(cause.getMessage()) || isPortExceptionClass(cause)) {
                return true;
            }
            cause = cause.getCause();
            depth++;
        }
        
        return false;
    }
    
    private boolean isPortMessage(String msg) {
        if (msg == null) return false;
        boolean hasPort = msg.contains("Port") || msg.contains("port");
        boolean hasInUse = msg.contains("was already in use") || msg.contains("already in use") || msg.contains("in use");
        return hasPort && hasInUse;
    }
    
    private boolean isPortExceptionClass(Throwable t) {
        if (t == null) return false;
        String className = t.getClass().getName();
        String simpleName = t.getClass().getSimpleName();
        return className.contains("PortInUseException") || 
               className.contains("BindException") ||
               simpleName.equals("BindException") ||
               (className.contains("WebServerException") && t.getMessage() != null && t.getMessage().toLowerCase().contains("port"));
    }
    
    /**
     * Handles port-in-use error with user-friendly dialog.
     */
    private void handlePortInUseError(String message) {
        setStatus("EROARE", WARNING, message);
        progressBar.setVisible(false);
        setPrimaryButtonState("Încearcă din nou", BRAND, true);
        
        // Show friendly dialog with options
        SwingUtilities.invokeLater(() -> {
            String[] options = {"Deschide aplicația existentă", "Închide"};
            
            int choice = JOptionPane.showOptionDialog(
                frame,
                message + "\n\nAplicația rulează deja. Doriți să deschideți aplicația existentă în browser?",
                "Aplicația rulează deja",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]
            );
            
            if (choice == 0) {
                // Open existing application in browser
                openBrowser();
            }
        });
    }

    private void resizePrimaryActionButton() {
        if (primaryActionButton == null) {
            return;
        }
        String text = primaryActionButton.getText();
        int width = 28 + primaryActionButton.getFontMetrics(primaryActionButton.getFont()).stringWidth(text);
        primaryActionButton.setPreferredSize(new Dimension(width, 30));
        primaryActionButton.setMinimumSize(new Dimension(width, 30));
        primaryActionButton.setMaximumSize(new Dimension(width, 30));
        if (primaryActionButton.getParent() != null) {
            primaryActionButton.getParent().revalidate();
            primaryActionButton.getParent().repaint();
        }
    }

    private void setPrimaryButtonState(String text, Color background, boolean enabled) {
        if (primaryActionButton == null) {
            return;
        }
        primaryActionButton.setText(text);
        primaryActionButton.setBackground(background);
        primaryActionButton.setEnabled(enabled);
        resizePrimaryActionButton();
    }


    /**
     * Translates common Spring Boot error messages to Romanian.
     */
    private String translateErrorMessage(Exception ex) {
        if (ex == null) {
            return "Pornirea aplicației a eșuat din motive necunoscute.";
        }
        
        String originalMessage = ex.getMessage();
        if (originalMessage == null) {
            return "Pornirea aplicației a eșuat.";
        }
        
        // Translate common error patterns
        if (originalMessage.contains("Port") && originalMessage.contains("was already in use")) {
            String port = extractPort(originalMessage);
            return "Portul " + port + " este deja ocupat de o altă aplicație. Închideți aplicația care folosește acest port sau configurați un alt port.";
        }
        
        if (originalMessage.contains("Failed to configure a DataSource")) {
            return "Eroare la configurarea bazei de date. Verificați setările de conexiune.";
        }
        
        if (originalMessage.contains("Unable to create initial connections")) {
            return "Nu s-a putut stabili conexiunea la baza de date. Verificați dacă baza de date este accesibilă.";
        }
        
        if (originalMessage.contains("Access is denied") || originalMessage.contains("Permission denied")) {
            return "Acces refuzat. Aplicația nu are permisiunile necesare pentru a accesa resursele solicitate.";
        }
        
        if (originalMessage.contains("OutOfMemoryError")) {
            return "Memorie insuficientă. Închideți alte aplicații sau măriți memoria alocată.";
        }
        
        if (originalMessage.contains("BindException") || originalMessage.contains("Address already in use")) {
            return "Adresa de rețea este deja în uz. O altă aplicație folosește același port.";
        }
        
        // Return original message if no translation found
        return "Eroare la pornirea aplicației: " + originalMessage;
    }
    
    /**
     * Extracts port number from error message.
     */
    private String extractPort(String message) {
        try {
            // Try to find "Port XXXX" pattern
            int portIndex = message.indexOf("Port ");
            if (portIndex >= 0) {
                String afterPort = message.substring(portIndex + 5);
                StringBuilder port = new StringBuilder();
                for (char c : afterPort.toCharArray()) {
                    if (Character.isDigit(c)) {
                        port.append(c);
                    } else {
                        break;
                    }
                }
                if (port.length() > 0) {
                    return port.toString();
                }
            }
        } catch (Exception ignored) {
        }
        return "necunoscut";
    }
    
    /**
     * Shows a professional error dialog with details.
     */
    private void showErrorDialog(String title, String message, Exception ex) {
        SwingUtilities.invokeLater(() -> {
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            // Error icon and message
            JPanel messagePanel = new JPanel(new BorderLayout(10, 10));
            JLabel iconLabel = new JLabel("⚠️");
            iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
            messagePanel.add(iconLabel, BorderLayout.WEST);
            
            JTextArea messageArea = new JTextArea(message);
            messageArea.setEditable(false);
            messageArea.setOpaque(false);
            messageArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
            messageArea.setLineWrap(true);
            messageArea.setWrapStyleWord(true);
            messageArea.setRows(3);
            messagePanel.add(messageArea, BorderLayout.CENTER);
            
            panel.add(messagePanel, BorderLayout.NORTH);
            
            // Technical details (collapsible)
            if (ex != null) {
                JPanel detailsPanel = new JPanel(new BorderLayout(5, 5));
                detailsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
                
                JLabel detailsLabel = new JLabel("Detalii tehnice:");
                detailsLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
                detailsLabel.setForeground(TEXT_SECONDARY);
                detailsPanel.add(detailsLabel, BorderLayout.NORTH);
                
                JTextArea detailsArea = new JTextArea(getStackTracePreview(ex));
                detailsArea.setEditable(false);
                detailsArea.setFont(new Font("Consolas", Font.PLAIN, 11));
                detailsArea.setBackground(BACKGROUND_DARK);
                detailsArea.setForeground(TEXT_PRIMARY);
                detailsArea.setRows(5);
                
                JScrollPane scrollPane = new JScrollPane(detailsArea);
                scrollPane.setPreferredSize(new Dimension(500, 120));
                detailsPanel.add(scrollPane, BorderLayout.CENTER);
                
                panel.add(detailsPanel, BorderLayout.CENTER);
            }
            
            JOptionPane.showMessageDialog(
                frame,
                panel,
                title,
                JOptionPane.ERROR_MESSAGE
            );
        });
    }
    
    /**
     * Gets a preview of the stack trace (first few lines).
     */
    private String getStackTracePreview(Exception ex) {
        if (ex == null) {
            return "Nu sunt disponibile detalii tehnice.";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(ex.getClass().getSimpleName()).append(": ").append(ex.getMessage()).append("\n");
        
        StackTraceElement[] trace = ex.getStackTrace();
        int limit = Math.min(5, trace.length);
        for (int i = 0; i < limit; i++) {
            sb.append("  la ").append(trace[i].toString()).append("\n");
        }
        
        if (trace.length > limit) {
            sb.append("  ... și încă ").append(trace.length - limit).append(" linii");
        }
        
        return sb.toString();
    }


    private void setStatus(String label, Color color, String detail) {
        statusDetail.setText(detail);
    }

    private void checkForUpdatesAsync(boolean userRequested) {
        if (!updateCheckRunning.compareAndSet(false, true)) {
            return;
        }
        setUpdateDetail("Update: verific versiunea. Versiune curenta: "
                + DesktopVersionChecker.resolveCurrentVersion() + ".", MUTED);
        Thread thread = new Thread(() -> {
            try {
                Optional<URI> updateUri = versionChecker.resolveUpdateUri();
                if (updateUri.isEmpty()) {
                    SwingUtilities.invokeLater(() -> setUpdateDetail(
                            "Update: URL neconfigurat. Seteaza " + DesktopVersionChecker.UPDATE_URL_PROPERTY + ".",
                            WARNING));
                    return;
                }
                DesktopVersionChecker.VersionCheckResult result = versionChecker.check(updateUri.get());
                SwingUtilities.invokeLater(() -> showVersionCheckResult(result, userRequested));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                SwingUtilities.invokeLater(() -> setUpdateDetail("Update: verificare intrerupta.", WARNING));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> setUpdateDetail("Update: verificarea a esuat: " + ex.getMessage(), DANGER));
            } finally {
                updateCheckRunning.set(false);
            }
        }, "ministryadmin-version-checker");
        thread.setDaemon(true);
        thread.start();
    }

    private void showVersionCheckResult(DesktopVersionChecker.VersionCheckResult result, boolean userRequested) {
        if (result.updateAvailable()) {
            setUpdateDetail("Update disponibil: " + result.latestVersion()
                    + ". Versiune curenta: " + result.currentVersion() + ".", WARNING);
            int choice = JOptionPane.showConfirmDialog(
                    frame,
                    "A aparut o versiune noua: " + result.latestVersion()
                            + "\nVersiunea curenta: " + result.currentVersion()
                            + "\nVrei sa deschizi pagina de download?",
                    "Update disponibil",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                openUri(result.releaseUri());
            }
        } else {
            setUpdateDetail("Nu exista versiune noua. Curenta: " + result.currentVersion()
                    + "; ultima disponibila: " + result.latestVersion() + ".", SUCCESS);
            if (userRequested) {
                JOptionPane.showMessageDialog(
                        frame,
                        "Ai ultima versiune instalata: " + result.currentVersion() + ".",
                        "Nu exista update",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void setUpdateDetail(String text, Color color) {
        if (updateDetail != null) {
            updateDetail.setText(text);
            updateDetail.setForeground(color);
            updateDetail.setVisible(true);
        }
    }

    private void beginStartupProgress() {
        stopStartupProgress();
        startupProgress = 0;
        startupProgressTimer = new Timer(45, event -> {
            if (startupProgress < 68) {
                startupProgress += 1;
            } else if (startupProgress < 86) {
                startupProgress += 1;
            } else if (startupProgress < 93) {
                startupProgress += 1;
            }
            updateStartupProgress(startupProgress);
        });
        startupProgressTimer.start();
    }

    private void completeStartupProgress() {
        stopStartupProgress();
        updateStartupProgress(100);
    }

    private void completeStartupProgressSmooth(Runnable onDone) {
        stopStartupProgress();
        if (progressCompletionTimer != null) {
            progressCompletionTimer.stop();
            progressCompletionTimer = null;
        }
        progressCompletionTimer = new Timer(20, event -> {
            if (startupProgress >= 100) {
                progressCompletionTimer.stop();
                progressCompletionTimer = null;
                if (onDone != null) {
                    onDone.run();
                }
                return;
            }
            int step = startupProgress < 92 ? 2 : 1;
            updateStartupProgress(Math.min(100, startupProgress + step));
        });
        progressCompletionTimer.start();
    }

    private void stopStartupProgress() {
        if (startupProgressTimer != null) {
            startupProgressTimer.stop();
            startupProgressTimer = null;
        }
        if (progressCompletionTimer != null) {
            progressCompletionTimer.stop();
            progressCompletionTimer = null;
        }
    }

    private void updateStartupProgress(int percent) {
        int normalized = Math.max(0, Math.min(100, percent));
        startupProgress = normalized;
        if (progressBar != null) {
            progressBar.setValue(normalized);
            progressBar.setString(startupProgressMessage(normalized));
        }
        if (statusDetail != null && normalized < 100) {
            statusDetail.setText(startupProgressMessage(normalized));
        }
    }

    static String startupProgressMessage(int percent) {
        int normalized = Math.max(0, Math.min(100, percent));
        return "Pornesc aplicatia... " + normalized + "%";
    }

    private void openUri(URI uri) {
        if (uri == null || !Desktop.isDesktopSupported()) {
            return;
        }
        try {
            Desktop.getDesktop().browse(uri);
        } catch (Exception ignored) {
            // Opening release pages is optional.
        }
    }

    static String resolveApplicationUrl(String[] args) {
        String explicitUrl = firstNonBlank(
                System.getProperty("ministryadmin.desktop.base-url"),
                System.getenv("CHURCH_ADMINISTRATION_PLATFORM_DESKTOP_BASE_URL"),
                System.getenv("MINISTRYADMIN_DESKTOP_BASE_URL"));
        if (explicitUrl != null) {
            return normalizeRootUrl(explicitUrl);
        }

        String explicitPort = firstNonBlank(
                System.getProperty("server.port"),
                System.getenv("SERVER_PORT"),
                extractArgumentValue(args, "--server.port="));
        if (explicitPort != null) {
            return "http://localhost:" + explicitPort + "/";
        }

        String profiles = firstNonBlank(
                System.getProperty("spring.profiles.active"),
                System.getenv("SPRING_PROFILES_ACTIVE"),
                extractArgumentValue(args, "--spring.profiles.active="));
        if (containsProfile(profiles, "dev")) {
            return DEV_LOCAL_URL;
        }

        return DEFAULT_LOCAL_URL;
    }

    static Path resolveLogFilePath(String[] args) {
        String explicitLogFile = firstNonBlank(
                System.getProperty("logging.file.name"),
                System.getenv("CHURCH_ADMINISTRATION_PLATFORM_LOG_FILE"),
                System.getenv("MINISTRYADMIN_LOG_FILE"),
                extractArgumentValue(args, "--logging.file.name="));
        if (explicitLogFile != null) {
            return Paths.get(explicitLogFile).toAbsolutePath().normalize();
        }

        String logsDir = firstNonBlank(
                System.getProperty("ministryadmin.desktop.logs-dir"),
                extractArgumentValue(args, "--ministryadmin.desktop.logs-dir="));
        if (logsDir == null) {
            logsDir = Paths.get(System.getProperty("user.home"), "ChurchAdministrationPlatform", "logs").toString();
        }
        return Paths.get(logsDir, "app.log").toAbsolutePath().normalize();
    }

    private String applicationUrl() {
        return applicationUrl;
    }

    private String dashboardUrl() {
        return appendPath(applicationUrl(), "dashboard");
    }

    private static String resolveApplicationUrl(ConfigurableApplicationContext context) {
        if (context != null) {
            String port = firstNonBlank(
                    context.getEnvironment().getProperty("local.server.port"),
                    context.getEnvironment().getProperty("server.port"));
            if (port != null) {
                return "http://localhost:" + port + "/";
            }
        }
        return DEFAULT_LOCAL_URL;
    }

    private static String normalizeRootUrl(String url) {
        String normalized = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        return normalized + "/";
    }

    static String appendPath(String baseUrl, String path) {
        String normalizedBase = baseUrl == null || baseUrl.isBlank() ? DEFAULT_LOCAL_URL : baseUrl;
        normalizedBase = normalizedBase.endsWith("/") ? normalizedBase.substring(0, normalizedBase.length() - 1) : normalizedBase;
        String normalizedPath = path == null ? "" : path.trim();
        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        return normalizedBase + "/" + normalizedPath;
    }

    static String resolvePortLabel(String applicationUrl) {
        try {
            URI uri = new URI(applicationUrl);
            int port = uri.getPort();
            if (port > 0) {
                return Integer.toString(port);
            }
            if ("https".equalsIgnoreCase(uri.getScheme())) {
                return "443";
            }
            if ("http".equalsIgnoreCase(uri.getScheme())) {
                return "80";
            }
        } catch (URISyntaxException ignored) {
            // Fall back to the raw URL below.
        }
        String url = applicationUrl;
        int colon = url.lastIndexOf(':');
        int slash = url.lastIndexOf('/');
        if (colon >= 0 && slash > colon) {
            return url.substring(colon + 1, slash);
        }
        return "necunoscut";
    }

    private static String extractArgumentValue(String[] args, String prefix) {
        if (args == null) {
            return null;
        }
        for (String arg : args) {
            if (arg != null && arg.startsWith(prefix)) {
                return arg.substring(prefix.length());
            }
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static boolean containsProfile(String profiles, String expected) {
        if (profiles == null || profiles.isBlank()) {
            return false;
        }
        return Arrays.stream(profiles.split(","))
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .anyMatch(expected::equals);
    }

    private List<Image> loadIconImages() {
        List<Image> images = new ArrayList<>();
        try {
            Image image = ImageIO.read(DesktopControlWindow.class.getResource("/static/img/ministryadmin-icon.png"));
            if (image != null) {
                images.add(image);
            }
        } catch (Exception ignored) {
            // The default Java icon is acceptable if the resource is unavailable.
        }
        return images;
    }

    private static final class BackgroundPanel extends JPanel {
        private BackgroundPanel() {
            super(new BorderLayout());
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, new Color(239, 246, 255), getWidth(), getHeight(), new Color(224, 242, 254)));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static final class SmoothProgressBar extends JProgressBar {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int arc = Math.max(8, h);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, w, h, arc, arc);

            int fill = (int) Math.round(w * (getPercentComplete() <= 0 ? 0 : getPercentComplete()));
            if (fill > 0) {
                GradientPaint gp = new GradientPaint(0, 0, new Color(56, 189, 248), fill, 0, new Color(37, 99, 235));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, fill, h, arc, arc);
            }
            g2.dispose();
        }
    }

    private static final class HeaderPanel extends JPanel {
        private HeaderPanel() {
            setPreferredSize(new Dimension(10, 104));
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

            JLabel name = new JLabel("Church Administration Platform");
            name.setForeground(Color.WHITE);
            name.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
            JLabel subtitle = new JLabel("Server local pentru administrarea bisericii");
            subtitle.setForeground(new Color(219, 234, 254));
            subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));

            JPanel text = new JPanel(new BorderLayout(0, 4));
            text.setOpaque(false);
            text.add(name, BorderLayout.NORTH);
            text.add(subtitle, BorderLayout.CENTER);
            add(text, BorderLayout.WEST);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, BRAND_DARK, getWidth(), getHeight(), BRAND));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    private static final class LogViewerWindow extends JFrame {
        private static final int MAX_VISIBLE_LINES = 2_500;
        private static final int INITIAL_READ_BYTES = 160_000;
        private static final Font LOG_FONT = new Font("Consolas", Font.PLAIN, 14);
        private final Path logFile;
        private final DefaultListModel<String> logModel = new DefaultListModel<>();
        private final JList<String> logList = new JList<>(logModel);
        private final JLabel status = new JLabel();
        private final JButton pauseButton = new JButton("Pauza");
        private final AtomicBoolean running = new AtomicBoolean(false);
        private final AtomicBoolean paused = new AtomicBoolean(false);
        private volatile long position = 0L;

        private LogViewerWindow(Path logFile) {
            super("Loguri aplicatie");
            this.logFile = logFile;
            buildUi();
        }

        private void buildUi() {
            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            setMinimumSize(new Dimension(720, 420));
            setSize(920, 560);
            setLayout(new BorderLayout(8, 8));

            JPanel toolbar = new JPanel(new BorderLayout(10, 0));
            toolbar.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
            toolbar.setBackground(SURFACE);

            status.setText("Fisier log: " + logFile);
            status.setForeground(MUTED);
            status.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            toolbar.add(status, BorderLayout.CENTER);

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            actions.setOpaque(false);
            JButton backButton = compactButton("Inapoi", MUTED, Color.WHITE);
            JButton refreshButton = compactButton("Refresh", BRAND, Color.WHITE);
            JButton clearButton = compactButton("Curata", Color.WHITE, BRAND_DARK);
            pauseButton.setFocusPainted(false);
            pauseButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            pauseButton.setBackground(Color.WHITE);
            pauseButton.setForeground(BRAND_DARK);

            backButton.addActionListener(event -> dispose());
            refreshButton.addActionListener(event -> loadInitialContent());
            clearButton.addActionListener(event -> logModel.clear());
            pauseButton.addActionListener(event -> togglePause());

            actions.add(backButton);
            actions.add(refreshButton);
            actions.add(pauseButton);
            actions.add(clearButton);
            toolbar.add(actions, BorderLayout.EAST);
            add(toolbar, BorderLayout.NORTH);

            logList.setCellRenderer(new LogLineRenderer());
            logList.setFixedCellHeight(25);
            logList.setFont(LOG_FONT);
            logList.setBackground(new Color(15, 23, 42));
            logList.setForeground(new Color(226, 232, 240));
            logList.setSelectionBackground(new Color(30, 64, 175));
            logList.setSelectionForeground(Color.WHITE);
            JScrollPane scrollPane = new JScrollPane(logList);
            scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            add(scrollPane, BorderLayout.CENTER);

            addWindowListener(new WindowAdapter() {
                @Override
                public void windowOpened(WindowEvent e) {
                    startTailer();
                }

                @Override
                public void windowClosed(WindowEvent e) {
                    running.set(false);
                }
            });
        }

        private void showWindow(JFrame owner) {
            if (!isVisible()) {
                setLocationRelativeTo(owner);
            }
            setVisible(true);
            toFront();
            startTailer();
        }

        private void startTailer() {
            if (!running.compareAndSet(false, true)) {
                return;
            }
            Thread thread = new Thread(this::tailLoop, "ministryadmin-log-viewer");
            thread.setDaemon(true);
            thread.start();
        }

        private void tailLoop() {
            loadInitialContent();
            while (running.get() && isDisplayable()) {
                if (!paused.get()) {
                    readNewContent();
                }
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    running.set(false);
                }
            }
        }

        private void loadInitialContent() {
            if (!Files.isRegularFile(logFile)) {
                SwingUtilities.invokeLater(() -> {
                    status.setText("Fisier log inexistent inca: " + logFile);
                    logModel.clear();
                });
                position = 0L;
                return;
            }

            try (RandomAccessFile file = new RandomAccessFile(logFile.toFile(), "r")) {
                long length = file.length();
                long start = Math.max(0L, length - INITIAL_READ_BYTES);
                file.seek(start);
                byte[] bytes = new byte[(int) (length - start)];
                file.readFully(bytes);
                position = length;
                List<String> lines = toNewestFirstLines(new String(bytes, StandardCharsets.UTF_8));
                SwingUtilities.invokeLater(() -> {
                    status.setText("Fisier log: " + logFile);
                    replaceLogLines(lines);
                });
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> status.setText("Nu pot citi logul: " + ex.getMessage()));
            }
        }

        private void readNewContent() {
            if (!Files.isRegularFile(logFile)) {
                return;
            }

            try (RandomAccessFile file = new RandomAccessFile(logFile.toFile(), "r")) {
                long length = file.length();
                if (length < position) {
                    position = 0L;
                }
                if (length == position) {
                    return;
                }

                file.seek(position);
                byte[] bytes = new byte[(int) (length - position)];
                file.readFully(bytes);
                position = length;

                List<String> newLines = toNewestFirstLines(new String(bytes, StandardCharsets.UTF_8));
                if (!newLines.isEmpty()) {
                    SwingUtilities.invokeLater(() -> prependLogLines(newLines));
                }
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> status.setText("Nu pot actualiza logul: " + ex.getMessage()));
            }
        }

        private void togglePause() {
            boolean nowPaused = !paused.get();
            paused.set(nowPaused);
            pauseButton.setText(nowPaused ? "Continua" : "Pauza");
            status.setText((nowPaused ? "Live oprit temporar: " : "Live activ: ") + logFile);
        }

        private void replaceLogLines(List<String> lines) {
            logModel.clear();
            for (String line : lines) {
                logModel.addElement(line);
                if (logModel.size() >= MAX_VISIBLE_LINES) {
                    break;
                }
            }
            if (!logModel.isEmpty()) {
                logList.ensureIndexIsVisible(0);
            }
        }

        private void prependLogLines(List<String> lines) {
            int insertAt = 0;
            for (String line : lines) {
                logModel.add(insertAt++, line);
            }
            while (logModel.size() > MAX_VISIBLE_LINES) {
                logModel.remove(logModel.size() - 1);
            }
            logList.ensureIndexIsVisible(0);
        }

        private static List<String> toNewestFirstLines(String content) {
            if (content == null || content.isBlank()) {
                return List.of();
            }
            String normalized = content.replace("\r\n", "\n").replace('\r', '\n');
            List<String> lines = new ArrayList<>(Arrays.asList(normalized.split("\n")));
            lines.removeIf(String::isBlank);
            Collections.reverse(lines);
            return lines;
        }

        private static JButton compactButton(String text, Color background, Color foreground) {
            JButton button = new JButton(text);
            button.setFocusPainted(false);
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            button.setBackground(background);
            button.setForeground(foreground);
            button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            button.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            return button;
        }

        private static final class LogLineRenderer extends DefaultListCellRenderer {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                String text = value == null ? "" : value.toString();
                label.setText(text);
                label.setOpaque(true);
                label.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
                label.setFont(LOG_FONT);

                if (!isSelected) {
                    label.setBackground(index % 2 == 0 ? new Color(15, 23, 42) : new Color(17, 24, 39));
                    label.setForeground(resolveForeground(text));
                }
                return label;
            }

            private static Color resolveForeground(String line) {
                String normalized = line.toUpperCase(Locale.ROOT);
                if (normalized.contains(" ERROR ")
                        || normalized.contains("[ERROR]")
                        || normalized.contains("EXCEPTION")
                        || normalized.contains("FAILED")
                        || normalized.contains("ESUAT")) {
                    return new Color(248, 113, 113);
                }
                if (normalized.contains(" WARN ")
                        || normalized.contains("[WARN]")
                        || normalized.contains("WARNING")) {
                    return new Color(251, 191, 36);
                }
                if (normalized.contains(" INFO ")
                        || normalized.contains("[INFO]")) {
                    return new Color(191, 219, 254);
                }
                return new Color(226, 232, 240);
            }
        }
    }

    /**
     * Custom JButton with rounded corners for a modern, professional appearance.
     */
    private static class RoundedButton extends JButton {
        private final int cornerRadius;
        private Color hoverBackground;

        public RoundedButton(String text, int cornerRadius) {
            super(text);
            this.cornerRadius = cornerRadius;
            setContentAreaFilled(false);
            setOpaque(false);
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (isEnabled()) {
                        hoverBackground = getBackground().darker();
                        repaint();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hoverBackground = null;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Determine background color
            Color bgColor = hoverBackground != null && getModel().isRollover() ? hoverBackground : getBackground();
            
            // Apply transparency if disabled
            if (!isEnabled()) {
                bgColor = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 100);
            }
            
            // Draw rounded background
            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
            
            // Draw subtle border for better definition
            g2.setColor(new Color(0, 0, 0, 20));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);
            
            g2.dispose();
            super.paintComponent(g);
        }
    }


}
