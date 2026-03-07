import java.awt.*;
import java.io.IOException;
import javax.swing.*;

public class RegistrationFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private final JTextField usernameField;
    private final JTextField nameField;
    private final JTextField addressField;
    private final JTextField emailField;
    private final JTextField phoneField;
    private final JPasswordField passwordField;
    private final JTextField ageField;

    private static final Color TEXT_COLOR   = new Color(0, 0, 0);
    private static final Color BORDER_COLOR = new Color(101, 67, 33);
    private static final Color BUTTON_COLOR = new Color(180, 150, 110);

    private static final Font BASE_FONT  = new Font("Georgia", Font.PLAIN, 14);
    private static final Font LABEL_FONT = new Font("Georgia", Font.BOLD, 14);
    private static final Font TITLE_FONT = new Font("Georgia", Font.BOLD, 28);

    private final LoginFrame loginFrame;
    private final transient SupabaseClient supabaseClient;

    @SuppressWarnings("this-escape")
    public RegistrationFrame(LoginFrame loginFrame, SupabaseClient supabaseClient) {
        this.loginFrame      = loginFrame;
        this.supabaseClient = supabaseClient;

        setTitle("Register - Drickoi's");
        ImageIcon appIcon = loadResourceIcon("/resources/myicon.png");
        if (appIcon != null) {
            setIconImage(appIcon.getImage());
        }
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);

        ImageIcon backgroundIcon = loadResourceIcon("/resources/Background.png");
        if (backgroundIcon != null) {
            setContentPane(new JLabel(backgroundIcon));
        }
        setLayout(new BorderLayout());

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setOpaque(false);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        mainPanel.setPreferredSize(new Dimension(420, 560));

        JLabel titleLabel = new JLabel("Register");
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(new Color(230, 220, 190));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setOpaque(false);
        formPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 2),
                BorderFactory.createEmptyBorder(18, 20, 18, 20)
        ));

        usernameField = createTextField();
        formPanel.add(createLabeledPanel("Username:", usernameField));
        formPanel.add(Box.createVerticalStrut(10));

        passwordField = createPasswordField();
        formPanel.add(createLabeledPanel("Password:", passwordField));
        formPanel.add(Box.createVerticalStrut(10));

        nameField = createTextField();
        formPanel.add(createLabeledPanel("Name:", nameField));
        formPanel.add(Box.createVerticalStrut(10));

        ageField = createTextField();
        formPanel.add(createLabeledPanel("Age:", ageField));
        formPanel.add(Box.createVerticalStrut(10));

        addressField = createTextField();
        formPanel.add(createLabeledPanel("Address:", addressField));
        formPanel.add(Box.createVerticalStrut(10));

        emailField = createTextField();
        formPanel.add(createLabeledPanel("Email:", emailField));
        formPanel.add(Box.createVerticalStrut(10));

        phoneField = createTextField();
        formPanel.add(createLabeledPanel("Phone:", phoneField));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setOpaque(false);

        JButton saveButton = createStyledButton("Save");
        saveButton.addActionListener(_ -> registerUser());

        JButton cancelButton = createStyledButton("Cancel");
        cancelButton.addActionListener(_ -> {
            dispose();
            loginFrame.showLoginFrame();
        });

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        JPanel contentPanel = new JPanel(new BorderLayout(0, 10));
        contentPanel.setOpaque(false);
        contentPanel.add(formPanel, BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        mainPanel.add(contentPanel, BorderLayout.CENTER);

        centerWrapper.add(mainPanel);
        add(centerWrapper, BorderLayout.CENTER);

        pack();
        setMinimumSize(new Dimension(500, 700));
        setSize(getMinimumSize());
        setLocationRelativeTo(null);
    }

    private JPanel createLabeledPanel(String labelText, JComponent field) {
        JLabel label = new JLabel(labelText);
        label.setFont(LABEL_FONT);
        label.setForeground(new Color(240, 232, 210));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        field.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(label);
        panel.add(Box.createVerticalStrut(6));
        panel.add(field);
        return panel;
    }

    private ImageIcon loadResourceIcon(String path) {
        java.net.URL resource = getClass().getResource(path);
        return resource != null ? new ImageIcon(resource) : null;
    }

    private JTextField createTextField() {
        JTextField textField = new JTextField(20);
        textField.setFont(BASE_FONT);
        textField.setForeground(TEXT_COLOR);
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        return textField;
    }

    private JPasswordField createPasswordField() {
        JPasswordField pwdField = new JPasswordField(20);
        pwdField.setFont(BASE_FONT);
        pwdField.setForeground(TEXT_COLOR);
        pwdField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        return pwdField;
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(BASE_FONT.deriveFont(Font.BOLD));
        button.setBackground(BUTTON_COLOR);
        button.setForeground(TEXT_COLOR);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void registerUser() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String name     = nameField.getText().trim();
        String ageStr   = ageField.getText().trim();
        String address  = addressField.getText().trim();
        String email    = emailField.getText().trim();
        String phone    = phoneField.getText().trim();

        if (username.isEmpty() || password.isEmpty() || name.isEmpty() || ageStr.isEmpty()
                || address.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields.", "Incomplete", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!ageStr.matches("\\d+")) {
            JOptionPane.showMessageDialog(this, "Age must be a number.", "Invalid Age", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int age = Integer.parseInt(ageStr);

        UserData user = new UserData(username, password, name, age, address, email, phone);
        try {
            SupabaseSession session = supabaseClient.signUp(email, password);
            if (session.getAccessToken().isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Registration created. Check your email for verification, then log in.",
                        "Verification Required",
                        JOptionPane.INFORMATION_MESSAGE
                );
            } else {
                supabaseClient.upsertProfile(session, user);
                new SupabaseSessionStore().save(session);
                try {
                    supabaseClient.logAction(session, "register", "Account registered: " + email);
                } catch (IOException ignored) {
                }
                JOptionPane.showMessageDialog(this, "Registration successful!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
            dispose();
            loginFrame.showLoginFrame();
        } catch (IOException | InterruptedException ex) {
            JOptionPane.showMessageDialog(this, "Registration failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
