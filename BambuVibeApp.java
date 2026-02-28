import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.*;

public class BambuVibeApp extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(BambuVibeApp.class.getName());

    private DefaultTableModel tableModel;
    private JTable inventoryTable;
    private JTextField itemNameField;
    private JComboBox<String> itemCategoryField;
    private JTextField itemQuantityField;
    private JTextField itemPriceField;
    private JTextField searchField;
    private JLabel totalQuantityLabel;
    private TableRowSorter<DefaultTableModel> sorter;
    private JLabel statusBarLabel;
    private JButton addButtonReference;

    private final LoginFrame loginFrame;
    private final SupabaseClient supabaseClient;
    private final SupabaseSession session;

    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final String INVENTORY_FILE = "inventory.csv";
    private static final String RECEIPTS_DIR = "receipts";

    private static final String ADD_ITEM_TEXT = "Add Item";
    private static final String ORDER_ITEM_TEXT = "Order Item";
    private static final String ACTIVITY_LOGS_TEXT = "Activity Logs";

    private static final Color PRIMARY_COLOR = new Color(92, 148, 86);
    private static final Color SECONDARY_COLOR = new Color(220, 240, 210);
    private static final Color BUTTON_COLOR = new Color(180, 150, 110);
    private static final Color BORDER_COLOR = new Color(101, 67, 33);
    private static final Color TEXT_COLOR = new Color(50, 80, 45);
    private static final Color LOW_STOCK_COLOR = new Color(255, 220, 100);

    private static final Font MAIN_FONT = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 14);

    private final int LOW_STOCK_THRESHOLD = 10;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();


    public BambuVibeApp() {
        this(null, null, null);
    }

    public BambuVibeApp(LoginFrame loginFrame, SupabaseClient supabaseClient, SupabaseSession session) {
        this.loginFrame = loginFrame;
        this.supabaseClient = supabaseClient;
        this.session = session;

        setTitle("Bambu Vibe");
        setIconImage(new ImageIcon("resources/myicon.png").getImage());
        setSize(400, 450);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        setSize(850, 950);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveInventory();
                dispose();
            }
        });

        initializeTable();

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(SECONDARY_COLOR);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel centerContentPanel = new JPanel();
        centerContentPanel.setLayout(new BoxLayout(centerContentPanel, BoxLayout.Y_AXIS));
        centerContentPanel.setBackground(SECONDARY_COLOR);
        centerContentPanel.add(createInputPanel());
        centerContentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        centerContentPanel.add(createSearchAndTablePanel());
        centerContentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        centerContentPanel.add(createStatusBarPanel());

        mainPanel.add(centerContentPanel, BorderLayout.CENTER);
        mainPanel.add(createSidebarPanel(), BorderLayout.WEST);

        add(mainPanel);

        setupTableSelectionListener();
        setupTablePopupMenu();
        setupSearch();

        loadInventory();
        updateTotalQuantity();
        setupTableColumns();

        SwingUtilities.invokeLater(() -> {
            if (addButtonReference != null) {
                getRootPane().setDefaultButton(addButtonReference);
            }
            pack();
        });
    }

    private void initializeTable() {
        String[] columnNames = {"Item Name", "Category", "Quantity", "Price (₱)", "Date Added", "Date Updated"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return switch (column) {
                    case 2 -> Integer.class;
                    case 3 -> Double.class;
                    case 4, 5 -> String.class;
                    default -> String.class;
                };
            }
        };

        inventoryTable = new JTable(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        inventoryTable.setRowSorter(sorter);

        inventoryTable.setRowHeight(30);
        inventoryTable.setFont(MAIN_FONT);
        inventoryTable.getTableHeader().setFont(HEADER_FONT);
        inventoryTable.getTableHeader().setForeground(TEXT_COLOR);
        inventoryTable.getTableHeader().setBackground(BUTTON_COLOR);
        inventoryTable.setSelectionBackground(PRIMARY_COLOR.darker());
        inventoryTable.setSelectionForeground(Color.WHITE);
        inventoryTable.setBackground(SECONDARY_COLOR);
        inventoryTable.setFillsViewportHeight(true);
        inventoryTable.setGridColor(BORDER_COLOR.brighter());
        inventoryTable.setShowGrid(true);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        inventoryTable.getColumnModel().getColumn(2).setCellRenderer(new LowQuantityRenderer());
        inventoryTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        inventoryTable.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        inventoryTable.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);
    }

    private class LowQuantityRenderer extends DefaultTableCellRenderer {
        public LowQuantityRenderer() {
            setHorizontalAlignment(JLabel.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component cellComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (column == 2) {
                int quantity = 0;
                if (value instanceof Number numberValue) {
                    quantity = numberValue.intValue();
                } else if (value != null) {
                    try {
                        quantity = Integer.parseInt(String.valueOf(value));
                    } catch (NumberFormatException e) {

                    }
                }

                if (quantity < LOW_STOCK_THRESHOLD) {
                    cellComponent.setBackground(LOW_STOCK_COLOR);
                    cellComponent.setForeground(Color.RED.darker());
                    cellComponent.setFont(cellComponent.getFont().deriveFont(Font.BOLD));
                } else {
                    cellComponent.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                    cellComponent.setForeground(isSelected ? table.getSelectionForeground() : TEXT_COLOR);
                    cellComponent.setFont(cellComponent.getFont().deriveFont(Font.PLAIN));
                }
            } else {
                cellComponent.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                cellComponent.setForeground(isSelected ? table.getSelectionForeground() : TEXT_COLOR);
            }
            return cellComponent;
        }
    }


    private void setupTableColumns() {
        TableColumnModel columnModel = inventoryTable.getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            TableColumn column = columnModel.getColumn(i);
            Component headerComp = inventoryTable.getTableHeader().getDefaultRenderer().getTableCellRendererComponent(
                    inventoryTable, column.getHeaderValue(), false, false, -1, i);
            int maxWidth = headerComp.getPreferredSize().width;

            for (int row = 0; row < inventoryTable.getRowCount(); row++) {
                Component cellRenderer = inventoryTable.prepareRenderer(inventoryTable.getCellRenderer(row, i), row, i);
                maxWidth = Math.max(maxWidth, cellRenderer.getPreferredSize().width);
            }

            column.setPreferredWidth(maxWidth + 15);
            column.setMaxWidth(300);
            column.setMinWidth(50);
        }
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(SECONDARY_COLOR);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 2),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        itemNameField = createStyledTextField("e.g., Essential Oil");
        String[] initialCategories = {"Wellness", "Home Goods", "Decor", "Personal Care", "Gardening", "Other"};
        itemCategoryField = createStyledComboBox(initialCategories);
        itemQuantityField = createStyledTextField("e.g., 25");
        itemPriceField = createStyledTextField("e.g., 1250.55");

        int row = 0;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new JLabel("Item Name:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(itemNameField, gbc);

        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new JLabel("Category:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(itemCategoryField, gbc);

        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new JLabel("Quantity:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(itemQuantityField, gbc);

        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new JLabel("Price (₱):"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(itemPriceField, gbc);

        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;

        InventoryButtonPanel buttonPanel = new InventoryButtonPanel(
                this::handleAddItemAction,
                this::handleUpdateItemAction,
                this::handleDeleteItemAction,
                this::handleClearFieldsAction,
                this::handleGenerateReportAction
        );
        panel.add(buttonPanel, gbc);

        this.addButtonReference = buttonPanel.getButton(ADD_ITEM_TEXT);

        return panel;
    }

    private JPanel createSearchAndTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(SECONDARY_COLOR);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 2),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
        searchPanel.setBackground(SECONDARY_COLOR);
        searchField = createStyledTextField("Search items...");
        searchField.setFont(MAIN_FONT.deriveFont(Font.ITALIC));
        searchPanel.add(searchField, BorderLayout.CENTER);

        panel.add(searchPanel, BorderLayout.NORTH);

        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(SECONDARY_COLOR);
        JScrollPane scrollPane = new JScrollPane(inventoryTable);
        scrollPane.setPreferredSize(new Dimension(700, 350));
        scrollPane.getViewport().setBackground(SECONDARY_COLOR);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        panel.add(tablePanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStatusBarPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.setBackground(SECONDARY_COLOR);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));

        totalQuantityLabel = new JLabel("Total Items: 0");
        totalQuantityLabel.setFont(HEADER_FONT.deriveFont(Font.BOLD, 16f));
        totalQuantityLabel.setForeground(TEXT_COLOR);
        panel.add(totalQuantityLabel, BorderLayout.WEST);

        statusBarLabel = new JLabel("Ready.");
        statusBarLabel.setFont(MAIN_FONT.deriveFont(Font.ITALIC));
        statusBarLabel.setForeground(TEXT_COLOR.darker());
        panel.add(statusBarLabel, BorderLayout.EAST);

        return panel;
    }

    private void updateStatusBar(String message, Color color) {
        statusBarLabel.setText(message);
        statusBarLabel.setForeground(color);
    }

    private JTextField createStyledTextField(String placeholderText) {
        JTextField textField = new JTextField(20);
        textField.setFont(MAIN_FONT);
        textField.setForeground(TEXT_COLOR);
        textField.setBackground(Color.WHITE);
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR.darker(), 1),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        textField.setText(placeholderText);
        textField.setForeground(Color.GRAY);
        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (textField.getText().equals(placeholderText)) {
                    textField.setText("");
                    textField.setForeground(TEXT_COLOR);
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (textField.getText().isEmpty()) {
                    textField.setText(placeholderText);
                    textField.setForeground(Color.GRAY);
                }
            }
        });
        return textField;
    }

    private JComboBox<String> createStyledComboBox(String[] items) {
        JComboBox<String> comboBox = new JComboBox<>(items);
        comboBox.setFont(MAIN_FONT);
        comboBox.setForeground(TEXT_COLOR);
        comboBox.setBackground(Color.WHITE);
        comboBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR.darker(), 1),
                BorderFactory.createEmptyBorder(3, 5, 3, 5)
        ));
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setForeground(TEXT_COLOR);
                return label;
            }
        });
        return comboBox;
    }

    private JPanel createSidebarPanel() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(PRIMARY_COLOR);
        sidebar.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
        sidebar.setPreferredSize(new Dimension(80, getHeight()));

        JButton orderButton = createSidebarButton("/resources/orderIcon.png", ORDER_ITEM_TEXT);
        orderButton.addActionListener(this::handleShowOrderDialogAction);
        sidebar.add(orderButton);
        sidebar.add(Box.createVerticalStrut(10));

        JButton logsButton = createSidebarButton("/resources/reportIcon.png", ACTIVITY_LOGS_TEXT);
        logsButton.addActionListener(this::handleShowActivityLogsAction);
        sidebar.add(logsButton);
        sidebar.add(Box.createVerticalStrut(10));

        sidebar.add(Box.createVerticalGlue());

        JButton logoutButton = createSidebarButton("/resources/logoutIcon.png", "Logout");
        logoutButton.addActionListener(this::handleLogoutAction);
        sidebar.add(logoutButton);


        JLabel timeLabel = new JLabel(new SimpleDateFormat("hh:mm a").format(new Date()));
        timeLabel.setFont(MAIN_FONT);
        timeLabel.setForeground(SECONDARY_COLOR);
        timeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(timeLabel);

        JLabel dateDayLabel = new JLabel(new SimpleDateFormat("EEEE").format(new Date()));
        dateDayLabel.setFont(MAIN_FONT);
        dateDayLabel.setForeground(SECONDARY_COLOR);
        dateDayLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(dateDayLabel);

        JLabel dateFullLabel = new JLabel(new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
        dateFullLabel.setFont(MAIN_FONT);
        dateFullLabel.setForeground(SECONDARY_COLOR);
        dateFullLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(dateFullLabel);

        JLabel engLabel = new JLabel("ENG");
        engLabel.setFont(MAIN_FONT.deriveFont(Font.BOLD));
        engLabel.setForeground(SECONDARY_COLOR);
        engLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(engLabel);
        sidebar.add(Box.createVerticalStrut(10));
        return sidebar;
    }

    private JButton createSidebarButton(String iconPath, String tooltip) {
        JButton button = new JButton();
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource(iconPath));
            Image image = icon.getImage();
            Image scaledImage = image.getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            button.setIcon(new ImageIcon(scaledImage));
        } catch (Exception e) {
            System.err.println("Could not load sidebar icon: " + iconPath + " - " + e.getMessage());
            button.setText("?");
        }
        button.setToolTipText(tooltip);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setPreferredSize(new Dimension(50, 50));
        button.setMaximumSize(new Dimension(50, 50));
        button.setBackground(PRIMARY_COLOR.darker());
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setBorder(new EmptyBorder(0,0,0,0));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(PRIMARY_COLOR.brighter());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(PRIMARY_COLOR.darker());
            }
        });

        button.setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                JButton b = (JButton) c;
                int size = Math.min(b.getWidth(), b.getHeight());
                if (b.isContentAreaFilled()) {
                    g2.setColor(b.getBackground());
                    g2.fillOval(0, 0, size, size);
                }
                super.paint(g2, c);
                g2.dispose();
            }

            @Override
            protected void paintFocus(Graphics g, AbstractButton b, Rectangle viewRect, Rectangle textRect, Rectangle iconRect) {
            }
        });


        return button;
    }

    private void addItem() {
        try {
            String itemName = itemNameField.getText().trim();
            String itemCategory = (String) itemCategoryField.getSelectedItem();

            if (itemName.isEmpty() || itemName.equals("e.g., Essential Oil")) {
                JOptionPane.showMessageDialog(this, "Please enter a valid item name.", "Input Error", JOptionPane.ERROR_MESSAGE);
                updateStatusBar("Error: Item name required.", Color.RED);
                return;
            }
            if (itemCategory == null || itemCategory.isEmpty() || itemCategory.equals("Select Category")) {
                JOptionPane.showMessageDialog(this, "Please select a valid item category.", "Input Error", JOptionPane.ERROR_MESSAGE);
                updateStatusBar("Error: Category required.", Color.RED);
                return;
            }

            for (int i = 0; i < tableModel.getRowCount(); i++) {
                if (tableModel.getValueAt(i, 0).toString().equalsIgnoreCase(itemName)) {
                    JOptionPane.showMessageDialog(this, "An item with this name already exists.", "Duplicate Item", JOptionPane.ERROR_MESSAGE);
                    updateStatusBar("Error: Duplicate item name.", Color.RED);
                    return;
                }
            }

            int quantity;
            try {
                quantity = Integer.parseInt(itemQuantityField.getText().trim());
                if (quantity < 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Please enter a valid positive integer for quantity.", "Input Error", JOptionPane.ERROR_MESSAGE);
                updateStatusBar("Error: Invalid quantity.", Color.RED);
                return;
            }

            double price;
            try {
                price = Double.parseDouble(itemPriceField.getText().trim());
                if (price < 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Please enter a valid positive number for price.", "Input Error", JOptionPane.ERROR_MESSAGE);
                updateStatusBar("Error: Invalid price.", Color.RED);
                return;
            }

            String dateAdded = dateFormatter.format(new Date());
            if (isCloudReady()) {
                supabaseClient.insertInventoryItem(session, itemName, itemCategory, quantity, price, dateAdded, "");
            }
            tableModel.addRow(new Object[]{itemName, itemCategory, quantity, price, dateAdded, ""});
            clearFields();
            updateTotalQuantity();
            saveInventory();
            logActionSafe("add_item", "Added: " + itemName + ", qty=" + quantity + ", price=" + price);
            updateStatusBar("Item '" + itemName + "' added successfully.", PRIMARY_COLOR.darker());
        } catch (RuntimeException | IOException | InterruptedException e) {
            JOptionPane.showMessageDialog(this, "An unexpected error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            updateStatusBar("Error: " + e.getMessage(), Color.RED);
        }
    }

    private void updateItem() {
        int selectedRow = inventoryTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to update.", "No Selection", JOptionPane.WARNING_MESSAGE);
            updateStatusBar("Warning: No item selected for update.", Color.ORANGE);
            return;
        }

        int modelRow = inventoryTable.convertRowIndexToModel(selectedRow);

        String itemName = itemNameField.getText().trim();
        String itemCategory = (String) itemCategoryField.getSelectedItem();
        int quantity;
        double price;

        if (itemName.isEmpty() || itemName.equals("e.g., Essential Oil")) {
            JOptionPane.showMessageDialog(this, "Please enter a valid item name.", "Input Error", JOptionPane.ERROR_MESSAGE);
            updateStatusBar("Error: Item name required.", Color.RED);
            return;
        }
        if (itemCategory == null || itemCategory.isEmpty() || itemCategory.equals("Select Category")) {
            JOptionPane.showMessageDialog(this, "Please select a valid item category.", "Input Error", JOptionPane.ERROR_MESSAGE);
            updateStatusBar("Error: Category required.", Color.RED);
            return;
        }

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (i != modelRow && tableModel.getValueAt(i, 0).toString().equalsIgnoreCase(itemName)) {
                JOptionPane.showMessageDialog(this, "An item with this name already exists.", "Duplicate Item", JOptionPane.ERROR_MESSAGE);
                updateStatusBar("Error: Duplicate item name.", Color.RED);
                return;
            }
        }

        try {
            quantity = Integer.parseInt(itemQuantityField.getText().trim());
            if (quantity < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid positive integer for quantity.", "Input Error", JOptionPane.ERROR_MESSAGE);
            updateStatusBar("Error: Invalid quantity.", Color.RED);
            return;
        }

        try {
            price = Double.parseDouble(itemPriceField.getText().trim());
            if (price < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid positive number for price.", "Input Error", JOptionPane.ERROR_MESSAGE);
            updateStatusBar("Error: Invalid price.", Color.RED);
            return;
        }

        String originalItemName = String.valueOf(tableModel.getValueAt(modelRow, 0));
        String updatedDate = dateFormatter.format(new Date());
        try {
            if (isCloudReady()) {
                supabaseClient.updateInventoryItemByName(
                        session,
                        originalItemName,
                        itemName,
                        itemCategory,
                        quantity,
                        price,
                        updatedDate
                );
            }
        } catch (IOException | InterruptedException e) {
            JOptionPane.showMessageDialog(this, "Failed to update cloud record: " + e.getMessage(), "Update Error", JOptionPane.ERROR_MESSAGE);
            updateStatusBar("Error updating cloud record.", Color.RED);
            return;
        }

        tableModel.setValueAt(itemName, modelRow, 0);
        tableModel.setValueAt(itemCategory, modelRow, 1);
        tableModel.setValueAt(quantity, modelRow, 2);
        tableModel.setValueAt(price, modelRow, 3);
        tableModel.setValueAt(updatedDate, modelRow, 5);
        clearFields();
        updateTotalQuantity();
        saveInventory();
        logActionSafe("update_item", "Updated: " + originalItemName + " -> " + itemName + ", qty=" + quantity + ", price=" + price);
        updateStatusBar("Item '" + itemName + "' updated successfully.", PRIMARY_COLOR.darker());
    }


    private void deleteItem() {
        int selectedRow = inventoryTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
            updateStatusBar("Warning: No item selected for deletion.", Color.ORANGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete the selected item?",
                "Confirm Deletion", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            int modelRow = inventoryTable.convertRowIndexToModel(selectedRow);
            String itemName = (String) tableModel.getValueAt(modelRow, 0);
            try {
                if (isCloudReady()) {
                    supabaseClient.deleteInventoryItemByName(session, itemName);
                }
            } catch (IOException | InterruptedException e) {
                JOptionPane.showMessageDialog(this, "Failed to delete cloud record: " + e.getMessage(), "Delete Error", JOptionPane.ERROR_MESSAGE);
                updateStatusBar("Error deleting cloud record.", Color.RED);
                return;
            }
            tableModel.removeRow(modelRow);
            clearFields();
            updateTotalQuantity();
            saveInventory();
            logActionSafe("delete_item", "Deleted: " + itemName);
            updateStatusBar("Item '" + itemName + "' deleted successfully.", PRIMARY_COLOR.darker());
        } else {
            updateStatusBar("Deletion cancelled.", Color.GRAY);
        }
    }

    private void clearFields() {
        itemNameField.setText("e.g., Essential Oil");
        itemNameField.setForeground(Color.GRAY);
        itemCategoryField.setSelectedIndex(0);
        itemQuantityField.setText("e.g., 25");
        itemQuantityField.setForeground(Color.GRAY);
        itemPriceField.setText("e.g., 1250.55");
        itemPriceField.setForeground(Color.GRAY);
        inventoryTable.clearSelection();
        updateStatusBar("Fields cleared.", TEXT_COLOR);
    }

    private void generateReport() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Report As");
        fileChooser.setSelectedFile(new File("Inventory_Report_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".csv"));

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileToSave))) {
                for (int i = 0; i < tableModel.getColumnCount(); i++) {
                    writer.write(tableModel.getColumnName(i));
                    if (i < tableModel.getColumnCount() - 1) {
                        writer.write(",");
                    }
                }
                writer.newLine();

                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    for (int j = 0; j < tableModel.getColumnCount(); j++) {
                        writer.write(String.valueOf(tableModel.getValueAt(i, j)));
                        if (j < tableModel.getColumnCount() - 1) {
                            writer.write(",");
                        }
                    }
                    writer.newLine();
                }
                JOptionPane.showMessageDialog(this, "Report generated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                logActionSafe("generate_report", "Report exported: " + fileToSave.getName());
                updateStatusBar("Report saved to: " + fileToSave.getName(), PRIMARY_COLOR.darker());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error saving report: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                updateStatusBar("Error saving report.", Color.RED);
                LOGGER.log(Level.SEVERE, "Error saving report", ex);
            }
        } else {
            updateStatusBar("Report generation cancelled.", Color.GRAY);
        }
    }

    private void loadInventory() {
        tableModel.setRowCount(0);
        if (isCloudReady()) {
            try {
                for (SupabaseClient.InventoryRecord record : supabaseClient.fetchInventory(session)) {
                    tableModel.addRow(new Object[]{
                            record.getItemName(),
                            record.getCategory(),
                            record.getQuantity(),
                            record.getPrice(),
                            record.getDateAdded(),
                            record.getDateUpdated()
                    });
                }
                updateStatusBar("Inventory loaded from cloud.", PRIMARY_COLOR.darker());
                return;
            } catch (IOException | InterruptedException e) {
                updateStatusBar("Cloud load failed, using local backup.", Color.ORANGE);
            }
        }

        File file = new File(INVENTORY_FILE);
        if (!file.exists()) {
            System.out.println("Inventory file not found. Starting with empty inventory.");
            return;
        }

        try (Scanner scanner = new Scanner(file)) {
            if (scanner.hasNextLine()) {
                scanner.nextLine();
            }
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(",", -1);
                if (parts.length >= 6) {
                    try {
                        String name = parts[0];
                        String category = parts[1];
                        int quantity = Integer.parseInt(parts[2]);
                        double price = Double.parseDouble(parts[3]);
                        String dateAdded = parts[4];
                        String dateUpdated = parts[5];

                        tableModel.addRow(new Object[]{name, category, quantity, price, dateAdded, dateUpdated});
                    } catch (NumberFormatException e) {
                        System.err.println("Skipping malformed inventory line (number format error): " + line);
                    }
                } else {
                    System.err.println("Skipping malformed inventory line (incorrect number of fields): " + line);
                }
            }
            updateStatusBar("Inventory loaded successfully from " + INVENTORY_FILE, PRIMARY_COLOR.darker());
        } catch (FileNotFoundException e) {
            System.err.println("Error loading inventory: " + e.getMessage());
            updateStatusBar("Error loading inventory.", Color.RED);
            LOGGER.log(Level.SEVERE, "Error loading inventory", e);
        }
    }

    private void saveInventory() {
        if (isCloudReady()) {
            updateStatusBar("Cloud mode active.", TEXT_COLOR);
            return;
        }

        try (FileWriter fw = new FileWriter(INVENTORY_FILE);
             BufferedWriter bw = new BufferedWriter(fw)) {
            for (int i = 0; i < tableModel.getColumnCount(); i++) {
                bw.write(tableModel.getColumnName(i));
                if (i < tableModel.getColumnCount() - 1) {
                    bw.write(",");
                }
            }
            bw.newLine();

            for (int i = 0; i < tableModel.getRowCount(); i++) {
                for (int j = 0; j < tableModel.getColumnCount(); j++) {
                    bw.write(String.valueOf(tableModel.getValueAt(i, j)));
                    if (j < tableModel.getColumnCount() - 1) {
                        bw.write(",");
                    }
                }
                bw.newLine();
            }
            System.out.println("Inventory saved successfully to " + INVENTORY_FILE);
            updateStatusBar("Inventory saved.", TEXT_COLOR);
        } catch (IOException e) {
            System.err.println("Error saving inventory: " + e.getMessage());
            updateStatusBar("Error saving inventory.", Color.RED);
            LOGGER.log(Level.SEVERE, "Error saving inventory", e);
        }
    }

    private void updateTotalQuantity() {
        int total = 0;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            total += (int) tableModel.getValueAt(i, 2);
        }
        totalQuantityLabel.setText("Total Items: " + total);
    }

    private void setupSearch() {
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterTable();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterTable();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterTable();
            }

            private void filterTable() {
                String text = searchField.getText().trim();
                if (text.length() == 0) {
                    sorter.setRowFilter(null);
                } else {
                    try {
                        sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
                    } catch (java.util.regex.PatternSyntaxException e) {
                        System.err.println("Bad regex pattern: " + text);
                    }
                }
            }
        });
    }

    private void setupTableSelectionListener() {
        inventoryTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && inventoryTable.getSelectedRow() != -1) {
                int selectedRow = inventoryTable.convertRowIndexToModel(inventoryTable.getSelectedRow());
                itemNameField.setText((String) tableModel.getValueAt(selectedRow, 0));
                itemCategoryField.setSelectedItem((String) tableModel.getValueAt(selectedRow, 1));
                itemQuantityField.setText(String.valueOf(tableModel.getValueAt(selectedRow, 2)));
                itemPriceField.setText(String.valueOf(tableModel.getValueAt(selectedRow, 3)));

                itemNameField.setForeground(TEXT_COLOR);
                itemQuantityField.setForeground(TEXT_COLOR);
                itemPriceField.setForeground(TEXT_COLOR);
            }
        });
    }

    private void setupTablePopupMenu() {
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem deleteItem = new JMenuItem("Delete Selected Item");
        deleteItem.addActionListener(this::handleDeleteItemAction);
        popupMenu.add(deleteItem);

        inventoryTable.setComponentPopupMenu(popupMenu);
    }

    private void showOrderDialog() {
        int selectedRow = inventoryTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to order.", "No Item Selected", JOptionPane.WARNING_MESSAGE);
            updateStatusBar("Warning: No item selected for order.", Color.ORANGE);
            return;
        }

        int modelRow = inventoryTable.convertRowIndexToModel(selectedRow);
        String itemName = (String) tableModel.getValueAt(modelRow, 0);
        int availableQuantity = (int) tableModel.getValueAt(modelRow, 2);
        double itemPrice = (double) tableModel.getValueAt(modelRow, 3);

        String quantityStr = JOptionPane.showInputDialog(this,
                "Enter quantity to order for '" + itemName + "' (Available: " + availableQuantity + "):",
                ORDER_ITEM_TEXT, JOptionPane.QUESTION_MESSAGE);

        if (quantityStr != null && !quantityStr.trim().isEmpty()) {
            try {
                int quantityToOrder = Integer.parseInt(quantityStr.trim());
                if (quantityToOrder <= 0) {
                    JOptionPane.showMessageDialog(this, "Quantity to order must be positive.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                    updateStatusBar("Error: Order quantity must be positive.", Color.RED);
                    return;
                }
                if (quantityToOrder > availableQuantity) {
                    JOptionPane.showMessageDialog(this, "Not enough stock. Available: " + availableQuantity, "Insufficient Stock", JOptionPane.ERROR_MESSAGE);
                    updateStatusBar("Error: Insufficient stock for '" + itemName + "'.", Color.RED);
                    return;
                }

                orderProduct(modelRow, itemName, quantityToOrder, itemPrice);

            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid quantity. Please enter a number.", "Input Error", JOptionPane.ERROR_MESSAGE);
                updateStatusBar("Error: Invalid quantity entered.", Color.RED);
            }
        } else {
            updateStatusBar("Order cancelled.", Color.GRAY);
        }
    }


    private void orderProduct(int modelRow, String itemName, int quantityOrdered, double itemPrice) {
        int currentQuantity = (int) tableModel.getValueAt(modelRow, 2);
        int newQuantity = currentQuantity - quantityOrdered;
        String updatedAt = dateFormatter.format(new Date());

        try {
            if (isCloudReady()) {
                String category = String.valueOf(tableModel.getValueAt(modelRow, 1));
                double price = (double) tableModel.getValueAt(modelRow, 3);
                supabaseClient.updateInventoryItemByName(
                        session,
                        itemName,
                        itemName,
                        category,
                        newQuantity,
                        price,
                        updatedAt
                );
            }
        } catch (IOException | InterruptedException e) {
            JOptionPane.showMessageDialog(this, "Failed to place cloud order: " + e.getMessage(), "Order Error", JOptionPane.ERROR_MESSAGE);
            updateStatusBar("Error placing cloud order.", Color.RED);
            return;
        }

        tableModel.setValueAt(newQuantity, modelRow, 2);
        tableModel.setValueAt(updatedAt, modelRow, 5);

        updateTotalQuantity();
        saveInventory();
        logActionSafe("order_item", "Ordered: " + itemName + ", qty=" + quantityOrdered + ", remaining=" + newQuantity);

        updateStatusBar("Ordered " + quantityOrdered + " of '" + itemName + "'. New quantity: " + newQuantity, PRIMARY_COLOR.darker());
        JOptionPane.showMessageDialog(this, "Order for " + quantityOrdered + " units of '" + itemName + "' placed successfully!", "Order Placed", JOptionPane.INFORMATION_MESSAGE);

        executorService.submit(() -> {
            generateReceipt(itemName, quantityOrdered, itemPrice, newQuantity);
        });
    }

    private void generateReceipt(String itemName, int quantityOrdered, double itemPrice, int remainingQuantity) {
        File receiptsDir = new File(RECEIPTS_DIR);
        if (!receiptsDir.exists()) {
            receiptsDir.mkdirs();
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String receiptFileName = RECEIPTS_DIR + "/receipt_" + timestamp + ".txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(receiptFileName))) {
            writer.write("----- Bambu Vibe Receipt -----\n");
            writer.write("Date: " + dateFormatter.format(new Date()) + "\n");
            writer.write("------------------------------\n");
            writer.write("Item Ordered: " + itemName + "\n");
            writer.write("Quantity: " + quantityOrdered + "\n");
            writer.write("Price Per Unit: ₱" + String.format("%.2f", itemPrice) + "\n");
            writer.write("Total Amount: ₱" + String.format("%.2f", (quantityOrdered * itemPrice)) + "\n");
            writer.write("Remaining Stock: " + remainingQuantity + "\n");
            writer.write("------------------------------\n");
            writer.write("Thank you for your order!\n");

            SwingUtilities.invokeLater(() -> {
                updateStatusBar("Receipt generated: " + receiptFileName, PRIMARY_COLOR.darker());
            });

        } catch (IOException e) {
            System.err.println("Error generating receipt: " + e.getMessage());
            SwingUtilities.invokeLater(() -> {
                updateStatusBar("Error generating receipt for '" + itemName + "'.", Color.RED);
            });
        }
    }

    private void showActivityLogsDialog() {
        if (!isCloudReady()) {
            JOptionPane.showMessageDialog(this, "Cloud session is not active.", "Logs Unavailable", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            java.util.List<SupabaseClient.ActionLogRecord> logs = supabaseClient.fetchActionLogs(session);
            String[] columns = {"Timestamp", "Action", "Details"};
            DefaultTableModel logsTableModel = new DefaultTableModel(columns, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            for (SupabaseClient.ActionLogRecord log : logs) {
                logsTableModel.addRow(new Object[]{log.getCreatedAt(), log.getActionType(), log.getDetails()});
            }

            JTable logsTable = new JTable(logsTableModel);
            logsTable.setRowHeight(26);
            logsTable.getTableHeader().setFont(HEADER_FONT);
            logsTable.setFont(MAIN_FONT);
            logsTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

            JDialog dialog = new JDialog(this, ACTIVITY_LOGS_TEXT, true);
            dialog.setLayout(new BorderLayout());
            dialog.add(new JScrollPane(logsTable), BorderLayout.CENTER);
            dialog.setSize(700, 400);
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        } catch (IOException | InterruptedException e) {
            JOptionPane.showMessageDialog(this, "Failed to load activity logs: " + e.getMessage(), "Logs Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void logout() {
        saveInventory();
        logActionSafe("logout", "User logged out.");
        try {
            new SupabaseSessionStore().clear();
        } catch (IOException ignored) {
        }
        dispose();
        if (loginFrame != null) {
            loginFrame.showLoginFrame();
        }
    }

    private boolean isCloudReady() {
        return supabaseClient != null && session != null && session.getAccessToken() != null && !session.getAccessToken().isBlank();
    }

    private void logActionSafe(String actionType, String details) {
        if (!isCloudReady()) {
            return;
        }
        try {
            supabaseClient.logAction(session, actionType, details);
        } catch (IOException | InterruptedException ignored) {
        }
    }

    private void handleAddItemAction(ActionEvent event) {
        event.getSource();
        addItem();
    }

    private void handleUpdateItemAction(ActionEvent event) {
        event.getSource();
        updateItem();
    }

    private void handleDeleteItemAction(ActionEvent event) {
        event.getSource();
        deleteItem();
    }

    private void handleClearFieldsAction(ActionEvent event) {
        event.getSource();
        clearFields();
    }

    private void handleGenerateReportAction(ActionEvent event) {
        event.getSource();
        generateReport();
    }

    private void handleShowOrderDialogAction(ActionEvent event) {
        event.getSource();
        showOrderDialog();
    }

    private void handleShowActivityLogsAction(ActionEvent event) {
        event.getSource();
        showActivityLogsDialog();
    }

    private void handleLogoutAction(ActionEvent event) {
        event.getSource();
        logout();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}
