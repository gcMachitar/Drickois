import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

public class LoginFrame extends JFrame {

    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JCheckBox showPasswordCheckBox;

    private final SupabaseClient supabaseClient;
    private final SupabaseSessionStore sessionStore;

    private static final Color LOGIN_TEXT_COLOR   = new Color(50, 80, 45);
    private static final Color LOGIN_BUTTON_COLOR = new Color(180, 150, 110);
    private static final Color LOGIN_BORDER_COLOR = new Color(101, 67, 33);
    private static final Font  LOGIN_FONT         = new Font("Georgia", Font.PLAIN, 14);

    public LoginFrame() {
        setTitle("Login - Bambu Vibe");
        setIconImage(new ImageIcon("resources/myicon.png").getImage());
        setResizable(false);
        setSize(400, 450);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        sessionStore = new SupabaseSessionStore();
        supabaseClient = new SupabaseClient(SupabaseConfig.getSupabaseUrl(), SupabaseConfig.getPublishableKey());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent __) {
                dispose();
            }
        });

        setContentPane(new JLabel(new ImageIcon("resources/background.png")));
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setOpaque(false);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel logoLabel = new JLabel("Bambu Vibe", SwingConstants.CENTER);
        logoLabel.setFont(new Font("Georgia", Font.BOLD, 32));
        logoLabel.setForeground(LOGIN_TEXT_COLOR);
        mainPanel.add(logoLabel, BorderLayout.NORTH);

        JPanel loginInputPanel = new JPanel();
        loginInputPanel.setLayout(new BoxLayout(loginInputPanel, BoxLayout.Y_AXIS));
        loginInputPanel.setOpaque(false);
        loginInputPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LOGIN_BORDER_COLOR, 2),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        usernameField     = createStyledTextField("Enter email");
        passwordField     = createStyledPasswordField("Enter password");
        showPasswordCheckBox = new JCheckBox("Show Password");

        showPasswordCheckBox.setOpaque(false);
        showPasswordCheckBox.setFont(LOGIN_FONT);
        showPasswordCheckBox.setForeground(LOGIN_TEXT_COLOR);
        showPasswordCheckBox.addActionListener(this::handleTogglePasswordVisibilityAction);

        loginInputPanel.add(createLabeledPanel("Email:", usernameField));
        loginInputPanel.add(Box.createVerticalStrut(10));
        loginInputPanel.add(createLabeledPanel("Password:", passwordField));
        loginInputPanel.add(showPasswordCheckBox);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setOpaque(false);

        JButton loginButton = createStyledButton("Login");
        loginButton.addActionListener(this::handleAttemptLoginAction);
        getRootPane().setDefaultButton(loginButton);

        JButton registerButton = createStyledButton("Register");
        registerButton.addActionListener(this::handleOpenRegistrationAction);

        JButton clearButton = createStyledButton("Clear");
        clearButton.addActionListener(this::handleClearFieldsAction);

        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);
        buttonPanel.add(clearButton);

        mainPanel.add(loginInputPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);

        SwingUtilities.invokeLater(this::restoreSessionIfAvailable);
    }

    private JPanel createLabeledPanel(String labelText, JComponent field) {
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Georgia", Font.BOLD, 14));
        label.setForeground(Color.BLACK);

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        panel.setOpaque(false);
        panel.add(label);
        panel.add(field);
        return panel;
    }

    private JTextField createStyledTextField(String placeholder) {
        JTextField textField = new JTextField(15);
        textField.setText(placeholder);
        textField.setFont(LOGIN_FONT);
        textField.setForeground(Color.GRAY);
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LOGIN_BORDER_COLOR.darker(), 1),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));

        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent __) {
                if (textField.getText().equals(placeholder)) {
                    textField.setText("");
                    textField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent __) {
                if (textField.getText().isEmpty()) {
                    textField.setText(placeholder);
                    textField.setForeground(Color.GRAY);
                }
            }
        });
        return textField;
    }

    private JPasswordField createStyledPasswordField(String placeholder) {
        JPasswordField pwdField = new JPasswordField(15);
        pwdField.setText(placeholder);
        pwdField.setFont(LOGIN_FONT);
        pwdField.setForeground(Color.GRAY);
        pwdField.setEchoChar((char) 0);
        pwdField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LOGIN_BORDER_COLOR.darker(), 1),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));

        pwdField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent __) {
                if (new String(pwdField.getPassword()).equals(placeholder)) {
                    pwdField.setText("");
                    pwdField.setForeground(Color.BLACK);
                    pwdField.setEchoChar('•');
                }
            }

            @Override
            public void focusLost(FocusEvent __) {
                if (new String(pwdField.getPassword()).isEmpty()) {
                    pwdField.setText(placeholder);
                    pwdField.setForeground(Color.GRAY);
                    pwdField.setEchoChar((char) 0);
                }
            }
        });
        return pwdField;
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(LOGIN_FONT.deriveFont(Font.BOLD));
        button.setBackground(LOGIN_BUTTON_COLOR);
        button.setForeground(LOGIN_TEXT_COLOR);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LOGIN_BORDER_COLOR),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void togglePasswordVisibility() {
        if (showPasswordCheckBox.isSelected()) {
            passwordField.setEchoChar((char) 0);
        } else {
            // Only mask if it's not showing placeholder
            if (!new String(passwordField.getPassword()).equals("Enter password")) {
                passwordField.setEchoChar('•');
            }
        }
    }

    private void attemptLogin() {
        if (!isSupabaseConfigured()) {
            JOptionPane.showMessageDialog(this,
                    "Set SUPABASE_PUBLISHABLE_KEY before login.",
                    "Supabase Config Missing",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        String email = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter email and password",
                    "Input Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            SupabaseSession session = supabaseClient.signIn(email, password);
            sessionStore.save(session);
            try {
                supabaseClient.logAction(session, "login", "User logged in.");
            } catch (IOException ignored) {
            }
            JOptionPane.showMessageDialog(this,
                    "Login Successful!",
                    "Welcome",
                    JOptionPane.INFORMATION_MESSAGE);
            new BambuVibeApp(this, supabaseClient, session).setVisible(true);
            dispose();
        } catch (IOException | InterruptedException ex) {
            JOptionPane.showMessageDialog(this,
                    "Invalid email or password",
                    "Login Failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void restoreSessionIfAvailable() {
        if (!isSupabaseConfigured()) {
            return;
        }
        try {
            SupabaseSession session = sessionStore.load();
            if (session == null) {
                return;
            }

            SupabaseSession refreshedSession = supabaseClient.refreshSession(session.getRefreshToken());
            sessionStore.save(refreshedSession);
            try {
                supabaseClient.logAction(refreshedSession, "session_restore", "Session restored on app start.");
            } catch (IOException ignored) {
            }
            new BambuVibeApp(this, supabaseClient, refreshedSession).setVisible(true);
            dispose();
        } catch (IOException | InterruptedException ignored) {
        }
    }

    public void showLoginFrame() {
        setVisible(true);
    }

    public void hideLoginFrame() {
        setVisible(false);
    }

    private void handleTogglePasswordVisibilityAction(ActionEvent event) {
        event.getSource();
        togglePasswordVisibility();
    }

    private void handleAttemptLoginAction(ActionEvent event) {
        event.getSource();
        attemptLogin();
    }

    private void handleOpenRegistrationAction(ActionEvent event) {
        event.getSource();
        if (!isSupabaseConfigured()) {
            JOptionPane.showMessageDialog(this,
                    "Set SUPABASE_PUBLISHABLE_KEY before registration.",
                    "Supabase Config Missing",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        setVisible(false);
        new RegistrationFrame(LoginFrame.this, supabaseClient).setVisible(true);
    }

    private void handleClearFieldsAction(ActionEvent event) {
        event.getSource();
        usernameField.setText("");
        passwordField.setText("");
    }

    private boolean isSupabaseConfigured() {
        return !SupabaseConfig.getPublishableKey().isBlank();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
