import java.awt.*;
import java.io.IOException;
import javax.swing.*;

public class RegistrationFrame extends JFrame {
    private final JTextField usernameField;
    private final JTextField nameField;
    private final JTextField addressField;
    private final JTextField emailField;
    private final JTextField phoneField;
    private final JPasswordField passwordField;
    private final JTextField ageField;

    private static final Color TEXT_COLOR   = new Color(0, 0, 0);
    private static final Color TITLE_COLOR  = new Color(50, 80, 45);
    private static final Color BORDER_COLOR = new Color(101, 67, 33);
    private static final Color BUTTON_COLOR = new Color(180, 150, 110);

    private static final Font BASE_FONT  = new Font("Georgia", Font.PLAIN, 14);
    private static final Font LABEL_FONT = new Font("Georgia", Font.BOLD, 14);
    private static final Font TITLE_FONT = new Font("Georgia", Font.BOLD, 28);

    private final LoginFrame loginFrame;
    private final SupabaseClient supabaseClient;

    public RegistrationFrame(LoginFrame loginFrame, SupabaseClient supabaseClient) {
        this.loginFrame      = loginFrame;
        this.supabaseClient = supabaseClient;

        setTitle("Register - Bambu Vibe");
        setIconImage(new ImageIcon("resources/myicon.png").getImage());
        setSize(400, 450);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        setContentPane(new JLabel(new ImageIcon("resources/background.jpg")));
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setOpaque(false);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Title
        JLabel titleLabel = new JLabel("Register");
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(TITLE_COLOR);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(titleLabel, gbc);

        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridwidth = 1;
        gbc.gridy++;

        usernameField = createTextField();
        addField(mainPanel, gbc, "Username:", usernameField);

        passwordField = createPasswordField();
        addField(mainPanel, gbc, "Password:", passwordField);

        nameField = createTextField();
        addField(mainPanel, gbc, "Name:", nameField);

        ageField = createTextField();
        addField(mainPanel, gbc, "Age:", ageField);

        addressField = createTextField();
        addField(mainPanel, gbc, "Address:", addressField);

        emailField = createTextField();
        addField(mainPanel, gbc, "Email:", emailField);

        phoneField = createTextField();
        addField(mainPanel, gbc, "Phone:", phoneField);

        // Buttons
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

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(buttonPanel, gbc);

        add(mainPanel, BorderLayout.CENTER);
    }

    private void addField(JPanel panel, GridBagConstraints gbc, String labelText, JComponent field) {
        JLabel label = new JLabel(labelText);
        label.setFont(LABEL_FONT);
        label.setForeground(TEXT_COLOR);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(label, gbc);

        gbc.gridx = 1;
        panel.add(field, gbc);
    }

    private JTextField createTextField() {
        JTextField textField = new JTextField(15);
        textField.setFont(BASE_FONT);
        textField.setForeground(TEXT_COLOR);
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        return textField;
    }

    private JPasswordField createPasswordField() {
        JPasswordField pwdField = new JPasswordField(15);
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
