package ro.church_office.teamleaf.desktop;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;

/**
 * Custom title bar with macOS-style close and minimize buttons.
 * Provides a modern, frameless window appearance with custom window controls.
 */
public class CustomTitleBar extends JPanel {
    private static final Color TITLE_BAR_BG = new Color(248, 250, 252);
    private static final Color CLOSE_BUTTON_COLOR = new Color(255, 95, 86);
    private static final Color CLOSE_BUTTON_HOVER = new Color(255, 70, 60);
    private static final Color MINIMIZE_BUTTON_COLOR = new Color(160, 160, 160);
    private static final Color MINIMIZE_BUTTON_HOVER = new Color(120, 120, 120);
    private static final Color BUTTON_BORDER = new Color(0, 0, 0, 20);
    private static final int BUTTON_SIZE = 12;
    private static final int BUTTON_SPACING = 8;
    
    private final JFrame parentFrame;
    private final JLabel titleLabel;
    private Point initialClick;
    
    public CustomTitleBar(JFrame parentFrame, String title) {
        this.parentFrame = parentFrame;
        setLayout(new BorderLayout());
        setBackground(TITLE_BAR_BG);
        setPreferredSize(new Dimension(0, 28));
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)));
        
        // Right side - window control buttons
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, BUTTON_SPACING, 0));
        controlsPanel.setOpaque(false);
        controlsPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 12));
        
        // Minimize button (first on the right)
        WindowButton minimizeButton = new WindowButton(MINIMIZE_BUTTON_COLOR, MINIMIZE_BUTTON_HOVER);
        minimizeButton.addActionListener(e -> parentFrame.setState(Frame.ICONIFIED));
        minimizeButton.setToolTipText("Minimizează");
        
        // Close button (rightmost)
        WindowButton closeButton = new WindowButton(CLOSE_BUTTON_COLOR, CLOSE_BUTTON_HOVER);
        closeButton.addActionListener(e -> parentFrame.dispatchEvent(
            new java.awt.event.WindowEvent(parentFrame, java.awt.event.WindowEvent.WINDOW_CLOSING)));
        closeButton.setToolTipText("Închide");
        
        controlsPanel.add(minimizeButton);
        controlsPanel.add(closeButton);
        
        // No title label - just empty space for dragging
        titleLabel = new JLabel("");
        
        // Make title bar draggable
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                initialClick = e.getPoint();
            }
        });
        
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (initialClick != null) {
                    // Get location of window
                    int thisX = parentFrame.getLocation().x;
                    int thisY = parentFrame.getLocation().y;
                    
                    // Determine how much the mouse moved since the initial click
                    int xMoved = e.getX() - initialClick.x;
                    int yMoved = e.getY() - initialClick.y;
                    
                    // Move window to this position
                    int x = thisX + xMoved;
                    int y = thisY + yMoved;
                    parentFrame.setLocation(x, y);
                }
            }
        });
        
        add(controlsPanel, BorderLayout.EAST);
        add(titleLabel, BorderLayout.CENTER);
    }
    
    /**
     * Updates the title text displayed in the title bar.
     */
    public void setTitle(String title) {
        titleLabel.setText(title);
    }
    
    /**
     * Custom button component that renders as a circular macOS-style window control.
     */
    private static class WindowButton extends JButton {
        private final Color normalColor;
        private final Color hoverColor;
        private boolean isHovered = false;
        
        public WindowButton(Color normalColor, Color hoverColor) {
            this.normalColor = normalColor;
            this.hoverColor = hoverColor;
            
            setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
            setMinimumSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
            setMaximumSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    repaint();
                }
                
                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }
            });
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw circular button
            Color buttonColor = isHovered ? hoverColor : normalColor;
            g2.setColor(buttonColor);
            g2.fill(new Ellipse2D.Float(0, 0, BUTTON_SIZE, BUTTON_SIZE));
            
            // Draw subtle border
            g2.setColor(BUTTON_BORDER);
            g2.draw(new Ellipse2D.Float(0, 0, BUTTON_SIZE - 1, BUTTON_SIZE - 1));
            
            // Draw symbol when hovered
            if (isHovered) {
                g2.setColor(new Color(0, 0, 0, 100));
                g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                
                int center = BUTTON_SIZE / 2;
                int symbolSize = 4;
                
                // Determine which symbol to draw based on color
                if (normalColor.equals(CLOSE_BUTTON_COLOR)) {
                    // Draw X for close button
                    g2.drawLine(center - symbolSize/2, center - symbolSize/2, 
                               center + symbolSize/2, center + symbolSize/2);
                    g2.drawLine(center + symbolSize/2, center - symbolSize/2, 
                               center - symbolSize/2, center + symbolSize/2);
                } else if (normalColor.equals(MINIMIZE_BUTTON_COLOR)) {
                    // Draw minus for minimize button
                    g2.drawLine(center - symbolSize/2, center, 
                               center + symbolSize/2, center);
                }
            }
            
            g2.dispose();
        }
    }
}
