package org.example;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.File;

/**
 * ChatWindow *
 * UI for chatting with the local model.
 * - Dark theme
 * - Colored names
 * - Typing indicator
 * - Smarter memory (Conversation)
 * - Rounded input + rounded Send button
 * - Local images via /img and auto filename detection
 * - Chat bubbles (Mochi left, You right, names next to bubbles)
 * - Replies trimmed & sanitized so Mochi doesn't call herself Claude
 * - Settings dialog (gear icon in top-right, with Memory tab)
 */
public class ChatWindow {

    // === BOT NAME ===
    private static final String BOT_NAME = "Mochi";

    // === THEME SUPPORT ===
    private enum Theme {
        DARK,
        AMOLED,
        SOFT_PINK
    }

    private Theme currentTheme = Theme.DARK;

    // These colors are configured by applyTheme(...)
    private Color BG_DARK;
    private Color BG_PANEL;
    private Color TEXT_NORMAL;

    private Color USER_COLOR;
    private Color BOT_COLOR;

    private Color INPUT_BG;
    private Color INPUT_TEXT;

    private Color BUTTON_BG;
    private final Color BUTTON_TEXT = Color.BLACK;

    // bubble colors (derived from theme)
    private Color BOT_BUBBLE_BG;
    private Color USER_BUBBLE_BG;


    // === IMAGE FOLDER ===
    private static final String IMAGE_BASE_DIR =
            "C:\\\\Users\\\\Owen\\\\Pictures\\\\MochiImages";

    // === CORE OBJECTS ===
    private final Conversation conversation;
    private final LlamaClient llamaClient;

    // === UI COMPONENTS ===
    private JPanel chatPanel;
    private JScrollPane scrollPane;
    private JTextField inputField;
    private JButton sendButton;
    private JLabel statusLabel;
    private JButton settingsButton;   // gear icon top-right
    private int chatFontSize = 13;   // default size

    // typing animation
    private Timer typingTimer;
    private int typingDots = 0;

    public ChatWindow(Conversation conversation, LlamaClient llamaClient) {
        this.conversation = conversation;
        this.llamaClient = llamaClient;
    }

    // Call this before building the UI and whenever the user changes theme.
    private void applyTheme(Theme theme) {
        this.currentTheme = theme;

        switch (theme) {
            case AMOLED -> {
                BG_DARK      = new Color(0, 0, 0);
                BG_PANEL     = new Color(18, 18, 24);
                TEXT_NORMAL  = new Color(235, 235, 245);

                USER_COLOR   = new Color(120, 180, 255);
                BOT_COLOR    = new Color(255, 130, 190);

                INPUT_BG     = new Color(15, 15, 22);
                INPUT_TEXT   = new Color(240, 240, 250);

                BUTTON_BG    = new Color(255, 130, 190);

                BOT_BUBBLE_BG  = new Color(32, 32, 48);
                USER_BUBBLE_BG = new Color(255, 130, 190);
            }
            case SOFT_PINK -> {
                BG_DARK      = new Color(30, 24, 32);
                BG_PANEL     = new Color(46, 34, 54);
                TEXT_NORMAL  = new Color(255, 240, 250);

                USER_COLOR   = new Color(180, 210, 255);
                BOT_COLOR    = new Color(255, 170, 210);

                INPUT_BG     = new Color(54, 40, 62);
                INPUT_TEXT   = new Color(255, 245, 250);

                BUTTON_BG    = new Color(255, 150, 210);

                BOT_BUBBLE_BG  = new Color(70, 50, 84);
                USER_BUBBLE_BG = new Color(255, 150, 210);
            }
            case DARK -> {
                BG_DARK      = new Color(24, 24, 32);
                BG_PANEL     = new Color(32, 32, 44);
                TEXT_NORMAL  = new Color(230, 230, 240);

                USER_COLOR   = new Color(120, 180, 255);
                BOT_COLOR    = new Color(255, 130, 190);

                INPUT_BG     = new Color(40, 40, 56);
                INPUT_TEXT   = new Color(240, 240, 250);

                BUTTON_BG    = new Color(255, 130, 190);

                BOT_BUBBLE_BG  = new Color(46, 46, 66);
                USER_BUBBLE_BG = new Color(255, 130, 190);
            }
        }

        // Refresh existing components if UI is already built
        refreshComponentColors();
    }

    private void refreshComponentColors() {
        if (frame != null) {
            frame.getContentPane().setBackground(BG_DARK);
        }
        if (topBar != null) {
            topBar.setBackground(BG_DARK);
        }
        if (chatPanel != null) {
            chatPanel.setBackground(BG_DARK);
        }
        if (scrollPane != null && scrollPane.getViewport() != null) {
            scrollPane.getViewport().setBackground(BG_DARK);
        }
        if (bottomPanel != null) {
            bottomPanel.setBackground(BG_PANEL);
        }
        if (statusBar != null) {
            statusBar.setBackground(BG_PANEL);
        }
        if (inputPanel != null) {
            inputPanel.setBackground(BG_PANEL);
        }

        if (inputField != null) {
            inputField.setBackground(INPUT_BG);
            inputField.setForeground(INPUT_TEXT);
            inputField.setCaretColor(INPUT_TEXT);
        }
        if (sendButton != null) {
            sendButton.setBackground(BUTTON_BG);
            sendButton.setForeground(BUTTON_TEXT);
        }
        if (statusLabel != null) {
            statusLabel.setForeground(BOT_COLOR);
        }
        if (settingsButton != null) {
            settingsButton.setForeground(BOT_COLOR);
        }

        // repaint chat area
        if (chatPanel != null) {
            chatPanel.revalidate();
            chatPanel.repaint();
        }
    }

    private JFrame frame;
    private JPanel topBar;
    private JPanel bottomPanel;
    private JPanel statusBar;
    private JPanel inputPanel;

    // -------------------------------------------------
    // create + show the window
    // -------------------------------------------------
    public void show() {
        applyTheme(currentTheme); // make sure colors are initialized
        frame = new JFrame(BOT_NAME + " - Local AI Chat");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 500);
        frame.getContentPane().setBackground(BG_DARK);

        // --- TOP BAR with gear icon (right) ---
        settingsButton = new JButton("\u2699"); // ⚙
        settingsButton.setFocusPainted(false);
        settingsButton.setBorderPainted(false);
        settingsButton.setContentAreaFilled(false);
        settingsButton.setOpaque(false);
        settingsButton.setForeground(BOT_COLOR);
        settingsButton.setFont(settingsButton.getFont().deriveFont(16f));
        settingsButton.setToolTipText("Settings");
        settingsButton.addActionListener(e -> openSettingsDialog());

        topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topBar.setBackground(BG_DARK);
        topBar.add(settingsButton);

        // --- CHAT PANEL (vertical stack of lines) ---
        chatPanel = new JPanel();
        chatPanel.setBackground(BG_DARK);
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));

        scrollPane = new JScrollPane(chatPanel);
        scrollPane.getViewport().setBackground(BG_DARK);
        scrollPane.setBorder(null);

        // --- INPUT FIELD ---
        inputField = new JTextField();
        inputField.setBackground(INPUT_BG);
        inputField.setForeground(INPUT_TEXT);
        inputField.setCaretColor(INPUT_TEXT);
        inputField.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(14),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));

        // --- ROUNDED SEND BUTTON ---
        sendButton = new RoundedButton("Send", 18);
        sendButton.setBackground(BUTTON_BG);
        sendButton.setForeground(BUTTON_TEXT);
        sendButton.setFocusPainted(false);

        // --- STATUS LABEL (bottom-left text only) ---
        statusLabel = new JLabel("Ready.");
        statusLabel.setForeground(BOT_COLOR);

        inputField.addActionListener(this::handleSend);
        sendButton.addActionListener(this::handleSend);

        // --- PANELS + LAYOUT ---
        inputPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel = new JPanel(new BorderLayout(5, 5));

        inputPanel.setBackground(BG_PANEL);
        bottomPanel.setBackground(BG_PANEL);

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        // status bar only has status text now
        statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(BG_PANEL);
        statusBar.add(statusLabel, BorderLayout.WEST);

        bottomPanel.add(statusBar, BorderLayout.NORTH);
        bottomPanel.add(inputPanel, BorderLayout.SOUTH);

        frame.setLayout(new BorderLayout(5, 5));
        frame.add(topBar, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // -------------------------------------------------
    // SETTINGS DIALOG (Memory tab + future placeholders)
    // -------------------------------------------------
    private void openSettingsDialog() {
        Window owner = SwingUtilities.getWindowAncestor(chatPanel);
        JDialog dialog = new JDialog(
                owner instanceof Frame ? (Frame) owner : null,
                "Mochi Settings",
                true
        );
        dialog.getContentPane().setBackground(BG_DARK);
        dialog.setLayout(new BorderLayout(8, 8));

        JTabbedPane tabs = new JTabbedPane();

        // Memory tab
        JPanel memoryPanel = new JPanel(new BorderLayout(8, 8));
        memoryPanel.setBackground(BG_DARK);

        JTextArea memoryArea = new JTextArea();
        memoryArea.setWrapStyleWord(true);
        memoryArea.setLineWrap(true);
        memoryArea.setEditable(false);
        memoryArea.setBackground(BG_PANEL);
        memoryArea.setForeground(TEXT_NORMAL);
        memoryArea.setBorder(new EmptyBorder(8, 8, 8, 8));
        memoryArea.setFont(memoryArea.getFont().deriveFont(13f));

        String mem = conversation.getLongTermSummary();
        if (mem == null || mem.isBlank()) {
            memoryArea.setText("(No long-term memory saved yet.)");
        } else {
            memoryArea.setText(mem);
        }

        JScrollPane memScroll = new JScrollPane(memoryArea);
        memScroll.getViewport().setBackground(BG_DARK);

        JPanel memButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        memButtons.setBackground(BG_PANEL);
        JButton clearMemBtn = new JButton("Clear Memory");
        clearMemBtn.addActionListener(ev -> {
            int confirm = JOptionPane.showConfirmDialog(
                    dialog,
                    "Clear Mochi's long-term memory?\nThis cannot be undone.",
                    "Clear Memory",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION) {
                conversation.clearLongTermSummary();
                memoryArea.setText("(No long-term memory saved yet.)");
            }
        });
        memButtons.add(clearMemBtn);

        memoryPanel.add(memScroll, BorderLayout.CENTER);
        memoryPanel.add(memButtons, BorderLayout.SOUTH);

        tabs.addTab("Memory", memoryPanel);

        // General tab (future settings)
        JPanel generalPanel = new JPanel();
        generalPanel.setBackground(BG_DARK);
        generalPanel.setLayout(new BoxLayout(generalPanel, BoxLayout.Y_AXIS));
        generalPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        generalPanel.add(new JLabel("Planned settings (future):"));
        generalPanel.add(Box.createVerticalStrut(8));
        generalPanel.add(new JLabel("• Reply length (short / normal / long)"));
        generalPanel.add(new JLabel("• Model selector"));
        generalPanel.add(new JLabel("• Auto-save chat logs"));

        // Theme tab
        JPanel themePanel = new JPanel();
        themePanel.setBackground(BG_DARK);
        themePanel.setLayout(new BoxLayout(themePanel, BoxLayout.Y_AXIS));
        themePanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel themeLabel = new JLabel("Choose a theme:");
        themeLabel.setForeground(TEXT_NORMAL);

        JButton darkBtn   = new JButton("Dark");
        JButton amoledBtn = new JButton("AMOLED");
        JButton pinkBtn   = new JButton("Soft Pink");

        darkBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        amoledBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        pinkBtn.setAlignmentX(Component.LEFT_ALIGNMENT);

        darkBtn.addActionListener(ev -> applyTheme(Theme.DARK));
        amoledBtn.addActionListener(ev -> applyTheme(Theme.AMOLED));
        pinkBtn.addActionListener(ev -> applyTheme(Theme.SOFT_PINK));

        themePanel.add(themeLabel);
        themePanel.add(Box.createVerticalStrut(8));
        themePanel.add(darkBtn);
        themePanel.add(Box.createVerticalStrut(4));
        themePanel.add(amoledBtn);
        themePanel.add(Box.createVerticalStrut(4));
        themePanel.add(pinkBtn);

        tabs.addTab("Theme", themePanel);

        tabs.addTab("General", generalPanel);

        dialog.add(tabs, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setBackground(BG_PANEL);
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(ev -> dialog.dispose());
        bottom.add(closeBtn);

        dialog.add(bottom, BorderLayout.SOUTH);

        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    // -------------------------------------------------
    // sending + receiving messages
    // -------------------------------------------------
    private void handleSend(ActionEvent e) {
        String userText = inputField.getText().trim();
        if (userText.isEmpty()) {
            return;
        }

        inputField.setText("");
        appendTextBubble("You", userText, true);

        // 1) explicit /img command
        if (userText.toLowerCase().startsWith("/img ")) {
            String fileName = userText.substring(5).trim();
            handleImageCommand(fileName);
            return;
        }

        // 2) auto-detect image filename in normal text
        String detectedImage = detectImageFilename(userText);
        if (detectedImage != null) {
            handleImageCommand(detectedImage);
            return;
        }

        // 3) normal AI chat
        conversation.addUser(userText);
        String prompt = conversation.buildTrimmedPrompt();

        setInputEnabled(false);

        String userLower = userText.toLowerCase();

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                try {
                    return llamaClient.complete(prompt);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return "[error running llama-cli]";
                }
            }

            @Override
            protected void done() {
                try {
                    String reply = get();
                    if (reply == null || reply.isEmpty()) {
                        appendTextBubble(BOT_NAME, "[no response]", false);
                        conversation.addAssistant("");
                    } else {
                        // tidy + identity fix
                        reply = tidyReply(reply);
                        reply = sanitizeIdentity(reply);

                        // special case: name questions → simple, clean answer
                        if (isNameQuestion(userLower)) {
                            reply = "My name is Mochi! I'm your offline AI assistant running on your computer.";
                        }

                        appendTextBubble(BOT_NAME, reply, false);
                        conversation.addAssistant(reply);
                    }

                    maybeSummarizeIfNeeded();

                } catch (Exception ex) {
                    ex.printStackTrace();
                    appendTextBubble(BOT_NAME, "[error getting result]", false);
                } finally {
                    setInputEnabled(true);
                }
            }
        }.execute();
    }

    // Shorten long replies & cut at a sentence end
    private String tidyReply(String reply) {
        if (reply == null) return "";
        String r = reply.trim();

        int maxChars = 600; // a bit more room
        if (r.length() > maxChars) {
            r = r.substring(0, maxChars);
        }

        int lastDot  = r.lastIndexOf('.');
        int lastBang = r.lastIndexOf('!');
        int lastQ    = r.lastIndexOf('?');
        int cut = Math.max(lastDot, Math.max(lastBang, lastQ));

        if (cut > 60) {
            r = r.substring(0, cut + 1);
        }

        return r.trim();
    }

    // Remove/replace bad identity lines like "I am Claude..."
    private String sanitizeIdentity(String text) {
        if (text == null) return "";

        String cleaned = text;

        // Replace "Claude" with Mochi
        cleaned = cleaned.replace("Claude", BOT_NAME);
        cleaned = cleaned.replace("claude", BOT_NAME);

        // Remove typical "I am Claude, a large language model..." intros
        cleaned = cleaned.replaceAll("(?i)i am " + BOT_NAME + ",? a large language model[^.]*\\.", "");
        cleaned = cleaned.replaceAll("(?i)i am a large language model[^.]*\\.", "");
        cleaned = cleaned.replaceAll("(?i)i am an ai assistant[^.]*\\.", "");

        // If we accidentally removed stuff leaving a dangling "?" at start
        cleaned = cleaned.replaceAll("^\\s*\\?\\s*", "");

        return cleaned.trim();
    }

    private boolean isNameQuestion(String lower) {
        if (lower == null) return false;
        return lower.contains("what's your name")
                || lower.contains("whats your name")
                || lower.contains("what is your name")
                || lower.matches(".*who are you.*");
    }

    // -------------------------------------------------
    // image handling
    // -------------------------------------------------
    private void handleImageCommand(String fileName) {
        File file;
        if (fileName.contains(":") || fileName.startsWith("\\\\") || fileName.startsWith("/")) {
            file = new File(fileName);
        } else {
            file = new File(IMAGE_BASE_DIR, fileName);
        }

        if (!file.exists()) {
            appendTextBubble(BOT_NAME,
                    "I couldn't find an image named \"" + fileName + "\".", false);
            return;
        }

        ImageIcon icon = new ImageIcon(file.getAbsolutePath());

        int maxWidth = 260;
        if (icon.getIconWidth() > maxWidth) {
            int newHeight = icon.getIconHeight() * maxWidth / icon.getIconWidth();
            Image scaled = icon.getImage().getScaledInstance(
                    maxWidth, newHeight, Image.SCALE_SMOOTH);
            icon = new ImageIcon(scaled);
        }

        appendImageBubble(BOT_NAME, icon, false);
        conversation.addAssistant("I showed the image file \"" + fileName + "\" in the chat.");
    }

    private String detectImageFilename(String text) {
        String lower = text.toLowerCase();
        String[] words = lower.split("[\\s,]+");

        for (String w : words) {
            if (w.endsWith(".png") || w.endsWith(".jpg") ||
                    w.endsWith(".jpeg") || w.endsWith(".gif") ||
                    w.endsWith(".webp")) {

                File f = new File(IMAGE_BASE_DIR, w);
                if (f.exists()) {
                    return w;
                }
            }
        }
        return null;
    }

    // -------------------------------------------------
    // chat bubble helpers
    // -------------------------------------------------
    private void appendTextBubble(String speaker, String text, boolean isUser) {
        JPanel line = new JPanel();
        line.setLayout(new BoxLayout(line, BoxLayout.X_AXIS));
        line.setOpaque(false);

        JLabel nameLabel = new JLabel(speaker);
        nameLabel.setForeground(isUser ? USER_COLOR : BOT_COLOR);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 12f));

        JPanel bubble = createTextBubble(text,
                isUser ? USER_BUBBLE_BG : BOT_BUBBLE_BG,
                isUser ? Color.BLACK : TEXT_NORMAL);

        if (isUser) {
            line.add(Box.createHorizontalGlue());
            line.add(bubble);
            line.add(Box.createHorizontalStrut(6));
            line.add(nameLabel);
        } else {
            line.add(nameLabel);
            line.add(Box.createHorizontalStrut(6));
            line.add(bubble);
            line.add(Box.createHorizontalGlue());
        }

        addLineToChat(line);
    }

    private void appendImageBubble(String speaker, ImageIcon icon, boolean isUser) {
        JPanel line = new JPanel();
        line.setLayout(new BoxLayout(line, BoxLayout.X_AXIS));
        line.setOpaque(false);

        JLabel nameLabel = new JLabel(speaker);
        nameLabel.setForeground(isUser ? USER_COLOR : BOT_COLOR);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 12f));

        JPanel bubble = createImageBubble(icon,
                isUser ? USER_BUBBLE_BG : BOT_BUBBLE_BG);

        if (isUser) {
            line.add(Box.createHorizontalGlue());
            line.add(bubble);
            line.add(Box.createHorizontalStrut(6));
            line.add(nameLabel);
        } else {
            line.add(nameLabel);
            line.add(Box.createHorizontalStrut(6));
            line.add(bubble);
            line.add(Box.createHorizontalGlue());
        }

        addLineToChat(line);
    }

    private JPanel createTextBubble(String text, Color bg, Color fg) {
        JPanel bubble = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.dispose();
                super.paintComponent(g);
            }
        };

        bubble.setOpaque(false);
        bubble.setLayout(new BorderLayout());
        bubble.setBorder(new EmptyBorder(8, 12, 8, 12));

        JTextArea area = new JTextArea(text);
        area.setWrapStyleWord(true);
        area.setLineWrap(true);
        area.setEditable(false);
        area.setOpaque(false);
        area.setForeground(fg);
        area.setFont(area.getFont().deriveFont(13f));

        // force measurement for fixed width
        area.setSize(600, Short.MAX_VALUE);
        Dimension textSize = area.getPreferredSize();
        area.setPreferredSize(textSize);

        bubble.add(area, BorderLayout.CENTER);

        bubble.setMaximumSize(new Dimension(600, textSize.height + 20));
        bubble.setPreferredSize(new Dimension(
                Math.min(600, textSize.width + 20),
                textSize.height + 20
        ));

        bubble.revalidate();
        return bubble;
    }

    private JPanel createImageBubble(ImageIcon icon, Color bg) {
        JPanel bubble = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        bubble.setOpaque(false);
        bubble.setLayout(new BorderLayout());
        bubble.setBorder(new EmptyBorder(6, 10, 6, 10));

        JLabel imgLabel = new JLabel(icon);
        imgLabel.setOpaque(false);

        bubble.add(imgLabel, BorderLayout.CENTER);
        bubble.setMaximumSize(new Dimension(icon.getIconWidth() + 20, icon.getIconHeight() + 20));

        return bubble;
    }

    private void addLineToChat(JPanel line) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(4, 8, 4, 8));
        wrapper.add(line, BorderLayout.CENTER);

        chatPanel.add(wrapper);
        chatPanel.revalidate();

        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = scrollPane.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    // -------------------------------------------------
    // typing indicator
    // -------------------------------------------------
    private void setInputEnabled(boolean enabled) {
        inputField.setEnabled(enabled);
        sendButton.setEnabled(enabled);

        if (statusLabel == null) return;

        if (enabled) {
            stopTypingAnimation();
        } else {
            startTypingAnimation();
        }
    }

    private void startTypingAnimation() {
        if (typingTimer != null && typingTimer.isRunning()) return;

        typingDots = 0;
        typingTimer = new Timer(400, e -> {
            typingDots = (typingDots + 1) % 4;
            String dots = ".".repeat(typingDots);
            statusLabel.setText(BOT_NAME + " is thinking" + dots);
        });
        typingTimer.start();
    }

    private void stopTypingAnimation() {
        if (typingTimer != null) {
            typingTimer.stop();
        }
        typingDots = 0;
        statusLabel.setText("Ready.");
    }

    // -------------------------------------------------
    // smarter memory integration
    // -------------------------------------------------
    private void maybeSummarizeIfNeeded() {
        if (!conversation.shouldSummarize()) {
            return;
        }

        String source = conversation.buildSummarizationSource();

        String prompt =
                "You are summarizing a chat between a user and an assistant named " + BOT_NAME + ".\n" +
                        "Write 3–6 very short bullet points capturing only important, long-term facts about the user, " +
                        "their preferences, and any ongoing tasks or projects.\n" +
                        "Do not include greetings or small talk. Do not mention yourself.\n\n" +
                        "Conversation:\n" + source;

        try {
            String summary = llamaClient.complete(prompt);
            if (summary != null && !summary.isBlank()) {
                conversation.updateLongTermSummary(summary);
                conversation.pruneHistoryAfterSummary();
                System.out.println("[MEMORY] Long-term summary updated:");
                System.out.println(summary);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("[MEMORY] Failed to summarize conversation.");
        }
    }

    // -------------------------------------------------
    // helper classes: rounded border + rounded button
    // -------------------------------------------------
    private static class RoundedBorder extends AbstractBorder {
        private final int radius;

        public RoundedBorder(int radius) {
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Shape round = new RoundRectangle2D.Float(
                    x + 1, y + 1,
                    width - 3, height - 3,
                    radius, radius
            );

            g2.setColor(new Color(255, 255, 255, 80));
            g2.draw(round);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(radius / 2, radius / 2, radius / 2, radius / 2);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = insets.right = insets.top = insets.bottom = radius / 2;
            return insets;
        }
    }

    private static class RoundedButton extends JButton {
        private final int radius;

        public RoundedButton(String text, int radius) {
            super(text);
            this.radius = radius;
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color base = getBackground();
            if (getModel().isPressed()) {
                base = base.darker();
            } else if (getModel().isRollover()) {
                base = base.brighter();
            }

            g2.setColor(base);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();

            super.paintComponent(g);
        }

        @Override
        protected void paintBorder(Graphics g) {
            // no border
        }

        @Override
        public void updateUI() {
            super.updateUI();
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
        }
    }
}
