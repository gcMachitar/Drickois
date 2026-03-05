// InventoryButtonPanel.java
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.TitledBorder;

public class InventoryButtonPanel extends JPanel {

    private static final Color SECONDARY_COLOR = new Color(220, 240, 210);
    private static final Color BORDER_COLOR = new Color(101, 67, 33);
    private static final Color TEXT_COLOR = new Color(50, 80, 45);
    private static final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 14);

    private static final Color ADD_ITEM_COLOR = new Color(144, 238, 144);
    private static final Color UPDATE_ITEM_COLOR = new Color(255, 255, 153);
    private static final Color DELETE_ITEM_COLOR = new Color(255, 102, 102);
    private static final Color CLEAR_FIELDS_COLOR = new Color(200, 200, 200);
    private static final Color GENERATE_REPORT_COLOR = new Color(173, 216, 230);


    private final Map<String, JButton> buttons;

    public InventoryButtonPanel(ActionListener addListener, ActionListener updateListener,
                                ActionListener deleteListener, ActionListener clearListener,
                                ActionListener generateReportListener) {
        setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
        setBackground(SECONDARY_COLOR);
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 2),
                "Actions",
                TitledBorder.CENTER,
                TitledBorder.TOP,
                HEADER_FONT,
                TEXT_COLOR
        ));

        buttons = new HashMap<>();

        JButton addButton = createStyledButton("Add Item", ADD_ITEM_COLOR, addListener, "/resources/additemIcon.png");
        JButton updateButton = createStyledButton("Update Item", UPDATE_ITEM_COLOR, updateListener, "/resources/updateitemIcon.png");
        JButton deleteButton = createStyledButton("Delete Item", DELETE_ITEM_COLOR, deleteListener, "/resources/deleteitemIcon.png");
        JButton clearButton = createStyledButton("Clear Fields", CLEAR_FIELDS_COLOR, clearListener, "/resources/clearIcon.png");
        JButton reportButton = createStyledButton("Generate Report", GENERATE_REPORT_COLOR, generateReportListener, "/resources/generatereportIcon.png");

        add(addButton);
        add(updateButton);
        add(deleteButton);
        add(clearButton);
        add(reportButton);

        buttons.put("Add Item", addButton);
        buttons.put("Update Item", updateButton);
        buttons.put("Delete Item", deleteButton);
        buttons.put("Clear Fields", clearButton);
        buttons.put("Generate Report", reportButton);
    }

    public JButton getButton(String text) {
        return buttons.get(text);
    }

    private JButton createStyledButton(String text, Color bgColor, ActionListener listener, String iconPath) {
        JButton button = new JButton(text);
        button.setFont(HEADER_FONT);
        button.setBackground(bgColor);
        button.setForeground(TEXT_COLOR);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR.darker(), 1),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        button.addActionListener(listener);

        setButtonIcon(button, iconPath);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(bgColor.brighter());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });
        return button;
    }

    private void setButtonIcon(JButton button, String iconPath) {
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource(iconPath));
            Image image = icon.getImage();
            Image scaledImage = image.getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            button.setIcon(new ImageIcon(scaledImage));
            button.setHorizontalTextPosition(SwingConstants.RIGHT);
            button.setVerticalTextPosition(SwingConstants.CENTER);
            button.setIconTextGap(5);
        } catch (Exception e) {
            System.err.println("Could not load icon: " + iconPath + " - " + e.getMessage());
            button.setText(button.getText());
        }
    }
}