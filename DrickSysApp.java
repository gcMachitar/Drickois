import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

public class DrickSysApp extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(DrickSysApp.class.getName());

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
    private JLabel cloudStatusLabel;
    private JButton addButtonReference;

    private final LoginFrame loginFrame;
    private final SupabaseClient supabaseClient;
    private final SupabaseSession session;

    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final String INVENTORY_FILE = "inventory.csv";
    private static final String INVENTORY_TEXT_BACKUP_FILE = "inventory_backup.txt";
    private static final String RECEIPTS_DIR = "receipts";
    private static final String DEFAULT_ITEM_NAME_PLACEHOLDER = "e.g., Cafe Latte";
    private static final String DEFAULT_QUANTITY_PLACEHOLDER = "e.g., 25";
    private static final String DEFAULT_PRICE_PLACEHOLDER = "e.g., 125.50";

    private static final String ADD_ITEM_TEXT = "Add Item";
    private static final String ORDER_ITEM_TEXT = "Order Item";
    private static final String ACTIVITY_LOGS_TEXT = "Activity Logs";
    private static final String INVENTORY_HUB_TEXT = "Inventory";
    private static final String SUPPLIER_TEXT = "Suppliers";
    private static final String EXPIRATION_TEXT = "Expirations";
    private static final String INGREDIENTS_TEXT = "Ingredients";
    private static final String STOCK_OUT_TEXT = "Stock Out";

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
    private boolean cloudConnected;
    private boolean cloudDisconnectDialogShown;


    public DrickSysApp() {
        this(null, null, null);
    }

    public DrickSysApp(LoginFrame loginFrame, SupabaseClient supabaseClient, SupabaseSession session) {
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

        cloudConnected = isCloudConfigured();
        cloudDisconnectDialogShown = false;

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

        itemNameField = createStyledTextField(DEFAULT_ITEM_NAME_PLACEHOLDER);
        String[] initialCategories = {"Coffee", "Drinks", "Sweet Delights", "Snacks", "Pastries", "Other"};
        itemCategoryField = createStyledComboBox(initialCategories);
        itemQuantityField = createStyledTextField(DEFAULT_QUANTITY_PLACEHOLDER);
        itemPriceField = createStyledTextField(DEFAULT_PRICE_PLACEHOLDER);

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

        cloudStatusLabel = new JLabel();
        cloudStatusLabel.setFont(MAIN_FONT.deriveFont(Font.BOLD));
        cloudStatusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(cloudStatusLabel, BorderLayout.CENTER);

        statusBarLabel = new JLabel("Ready.");
        statusBarLabel.setFont(MAIN_FONT.deriveFont(Font.ITALIC));
        statusBarLabel.setForeground(TEXT_COLOR.darker());
        panel.add(statusBarLabel, BorderLayout.EAST);
        updateCloudStatusIndicator();

        return panel;
    }

    private void updateStatusBar(String message, Color color) {
        statusBarLabel.setText(message);
        statusBarLabel.setForeground(color);
    }

    private void updateCloudStatusIndicator() {
        if (cloudStatusLabel == null) {
            return;
        }
        if (!isCloudConfigured()) {
            cloudStatusLabel.setText("Cloud: OFF");
            cloudStatusLabel.setForeground(Color.GRAY.darker());
            return;
        }
        if (cloudConnected) {
            cloudStatusLabel.setText("Cloud: Connected");
            cloudStatusLabel.setForeground(new Color(0, 128, 0));
        } else {
            cloudStatusLabel.setText("Cloud: Offline");
            cloudStatusLabel.setForeground(new Color(180, 90, 0));
        }
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

        JButton logsButton = createSidebarButton("/resources/generatereportIcon.png", ACTIVITY_LOGS_TEXT);
        logsButton.addActionListener(this::handleShowActivityLogsAction);
        sidebar.add(logsButton);
        sidebar.add(Box.createVerticalStrut(10));

        JButton inventoryButton = createSidebarButton("/resources/additemIcon.png", INVENTORY_HUB_TEXT);
        inventoryButton.addActionListener(this::handleShowInventoryHubDialogAction);
        sidebar.add(inventoryButton);
        sidebar.add(Box.createVerticalStrut(10));

        JButton exportTxtButton = createSidebarButton("/resources/updateitemIcon.png", "Export TXT Backup");
        exportTxtButton.addActionListener(this::handleExportTextBackupAction);
        sidebar.add(exportTxtButton);
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

    private JButton createDialogActionButton(String text) {
        JButton button = new JButton(text);
        button.setFont(MAIN_FONT.deriveFont(Font.BOLD));
        button.setForeground(TEXT_COLOR);
        button.setBackground(BUTTON_COLOR);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    @FunctionalInterface
    private interface CloudOperation {
        void run() throws IOException, InterruptedException;
    }

    private boolean runCloudOperation(String actionLabel, CloudOperation operation) {
        if (!isCloudConfigured()) {
            return false;
        }
        try {
            operation.run();
            if (!cloudConnected) {
                cloudDisconnectDialogShown = false;
                updateStatusBar("Cloud reconnected. Live sync resumed.", new Color(0, 128, 0));
            }
            cloudConnected = true;
            updateCloudStatusIndicator();
            return true;
        } catch (IOException | InterruptedException e) {
            cloudConnected = false;
            updateCloudStatusIndicator();
            updateStatusBar("Cloud unavailable, saved locally only.", Color.ORANGE.darker());
            if (!cloudDisconnectDialogShown) {
                JOptionPane.showMessageDialog(
                        this,
                        "Cloud sync failed during " + actionLabel + ".\nThe app will continue in offline mode and save local backup files.",
                        "Cloud Connection Warning",
                        JOptionPane.WARNING_MESSAGE
                );
                cloudDisconnectDialogShown = true;
            }
            LOGGER.log(Level.WARNING, "Cloud operation failed: " + actionLabel, e);
            return false;
        }
    }

    private String csvEscape(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        if (text.contains("\"")) {
            text = text.replace("\"", "\"\"");
        }
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text + "\"";
        }
        return text;
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        if (line == null) {
            return values;
        }
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        values.add(current.toString());
        return values;
    }

    private void addItem() {
        try {
            String itemName = itemNameField.getText().trim();
            String itemCategory = (String) itemCategoryField.getSelectedItem();

            if (itemName.isEmpty() || itemName.equals(DEFAULT_ITEM_NAME_PLACEHOLDER)) {
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
            boolean cloudSynced = !isCloudConfigured() || runCloudOperation("add item", () ->
                    supabaseClient.insertInventoryItem(session, itemName, itemCategory, quantity, price, dateAdded, "")
            );
            tableModel.addRow(new Object[]{itemName, itemCategory, quantity, price, dateAdded, ""});
            clearFields();
            updateTotalQuantity();
            saveInventory();
            logActionSafe("add_item", "Added: " + itemName + ", qty=" + quantity + ", price=" + price);
            if (cloudSynced) {
                updateStatusBar("Item '" + itemName + "' added and synced.", PRIMARY_COLOR.darker());
            } else {
                updateStatusBar("Item '" + itemName + "' added locally (offline mode).", Color.ORANGE.darker());
            }
        } catch (RuntimeException e) {
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

        if (itemName.isEmpty() || itemName.equals(DEFAULT_ITEM_NAME_PLACEHOLDER)) {
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
        boolean cloudSynced = !isCloudConfigured() || runCloudOperation("update item", () ->
                supabaseClient.updateInventoryItemByName(
                        session,
                        originalItemName,
                        itemName,
                        itemCategory,
                        quantity,
                        price,
                        updatedDate
                )
        );

        tableModel.setValueAt(itemName, modelRow, 0);
        tableModel.setValueAt(itemCategory, modelRow, 1);
        tableModel.setValueAt(quantity, modelRow, 2);
        tableModel.setValueAt(price, modelRow, 3);
        tableModel.setValueAt(updatedDate, modelRow, 5);
        clearFields();
        updateTotalQuantity();
        saveInventory();
        logActionSafe("update_item", "Updated: " + originalItemName + " -> " + itemName + ", qty=" + quantity + ", price=" + price);
        if (cloudSynced) {
            updateStatusBar("Item '" + itemName + "' updated and synced.", PRIMARY_COLOR.darker());
        } else {
            updateStatusBar("Item '" + itemName + "' updated locally (offline mode).", Color.ORANGE.darker());
        }
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
            boolean cloudSynced = !isCloudConfigured() || runCloudOperation("delete item", () ->
                    supabaseClient.deleteInventoryItemByName(session, itemName)
            );
            tableModel.removeRow(modelRow);
            clearFields();
            updateTotalQuantity();
            saveInventory();
            logActionSafe("delete_item", "Deleted: " + itemName);
            if (cloudSynced) {
                updateStatusBar("Item '" + itemName + "' deleted and synced.", PRIMARY_COLOR.darker());
            } else {
                updateStatusBar("Item '" + itemName + "' deleted locally (offline mode).", Color.ORANGE.darker());
            }
        } else {
            updateStatusBar("Deletion cancelled.", Color.GRAY);
        }
    }

    private void clearFields() {
        itemNameField.setText(DEFAULT_ITEM_NAME_PLACEHOLDER);
        itemNameField.setForeground(Color.GRAY);
        itemCategoryField.setSelectedIndex(0);
        itemQuantityField.setText(DEFAULT_QUANTITY_PLACEHOLDER);
        itemQuantityField.setForeground(Color.GRAY);
        itemPriceField.setText(DEFAULT_PRICE_PLACEHOLDER);
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
                    writer.write(csvEscape(tableModel.getColumnName(i)));
                    if (i < tableModel.getColumnCount() - 1) {
                        writer.write(",");
                    }
                }
                writer.newLine();

                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    for (int j = 0; j < tableModel.getColumnCount(); j++) {
                        writer.write(csvEscape(tableModel.getValueAt(i, j)));
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

    private void exportCurrentDataAsText() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Inventory Text Backup");
        fileChooser.setSelectedFile(new File("Inventory_Backup_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".txt"));
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection != JFileChooser.APPROVE_OPTION) {
            updateStatusBar("Text export cancelled.", Color.GRAY);
            return;
        }

        File fileToSave = fileChooser.getSelectedFile();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileToSave))) {
            writer.write("Bambu Vibe Inventory Backup");
            writer.newLine();
            writer.write("Generated: " + dateFormatter.format(new Date()));
            writer.newLine();
            writer.write("------------------------------------------------------------");
            writer.newLine();
            for (int i = 0; i < tableModel.getColumnCount(); i++) {
                writer.write(tableModel.getColumnName(i));
                if (i < tableModel.getColumnCount() - 1) {
                    writer.write(" | ");
                }
            }
            writer.newLine();
            writer.write("------------------------------------------------------------");
            writer.newLine();
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                for (int j = 0; j < tableModel.getColumnCount(); j++) {
                    writer.write(String.valueOf(tableModel.getValueAt(i, j)));
                    if (j < tableModel.getColumnCount() - 1) {
                        writer.write(" | ");
                    }
                }
                writer.newLine();
            }
            updateStatusBar("TXT backup exported: " + fileToSave.getName(), PRIMARY_COLOR.darker());
            JOptionPane.showMessageDialog(this, "Text backup exported successfully.", "Export Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            updateStatusBar("Error exporting text backup.", Color.RED);
            JOptionPane.showMessageDialog(this, "Failed to export text backup: " + e.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.log(Level.SEVERE, "Error exporting text backup", e);
        }
    }

    private void loadInventory() {
        tableModel.setRowCount(0);
        if (isCloudConfigured()) {
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
                cloudConnected = true;
                cloudDisconnectDialogShown = false;
                updateCloudStatusIndicator();
                saveInventory();
                updateStatusBar("Inventory loaded from cloud.", PRIMARY_COLOR.darker());
                return;
            } catch (IOException | InterruptedException e) {
                cloudConnected = false;
                updateCloudStatusIndicator();
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
                List<String> parts = parseCsvLine(line);
                if (parts.size() >= 6) {
                    try {
                        String name = parts.get(0);
                        String category = parts.get(1);
                        int quantity = Integer.parseInt(parts.get(2));
                        double price = Double.parseDouble(parts.get(3));
                        String dateAdded = parts.get(4);
                        String dateUpdated = parts.get(5);

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
        try (FileWriter fw = new FileWriter(INVENTORY_FILE);
             BufferedWriter bw = new BufferedWriter(fw)) {
            for (int i = 0; i < tableModel.getColumnCount(); i++) {
                bw.write(csvEscape(tableModel.getColumnName(i)));
                if (i < tableModel.getColumnCount() - 1) {
                    bw.write(",");
                }
            }
            bw.newLine();

            for (int i = 0; i < tableModel.getRowCount(); i++) {
                for (int j = 0; j < tableModel.getColumnCount(); j++) {
                    bw.write(csvEscape(tableModel.getValueAt(i, j)));
                    if (j < tableModel.getColumnCount() - 1) {
                        bw.write(",");
                    }
                }
                bw.newLine();
            }
            saveTextBackupSnapshot();
            System.out.println("Inventory saved successfully to " + INVENTORY_FILE);
            if (isCloudConfigured() && cloudConnected) {
                updateStatusBar("Inventory saved locally and cloud is connected.", TEXT_COLOR);
            } else if (isCloudConfigured()) {
                updateStatusBar("Inventory saved locally (offline mode).", Color.ORANGE.darker());
            } else {
                updateStatusBar("Inventory saved locally.", TEXT_COLOR);
            }
        } catch (IOException e) {
            System.err.println("Error saving inventory: " + e.getMessage());
            updateStatusBar("Error saving inventory.", Color.RED);
            LOGGER.log(Level.SEVERE, "Error saving inventory", e);
        }
    }

    private void saveTextBackupSnapshot() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(INVENTORY_TEXT_BACKUP_FILE))) {
            writer.write("Inventory Text Backup");
            writer.newLine();
            writer.write("Saved at: " + dateFormatter.format(new Date()));
            writer.newLine();
            writer.write("------------------------------------------------------------");
            writer.newLine();
            for (int i = 0; i < tableModel.getColumnCount(); i++) {
                writer.write(tableModel.getColumnName(i));
                if (i < tableModel.getColumnCount() - 1) {
                    writer.write(" | ");
                }
            }
            writer.newLine();
            writer.write("------------------------------------------------------------");
            writer.newLine();
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                for (int j = 0; j < tableModel.getColumnCount(); j++) {
                    writer.write(String.valueOf(tableModel.getValueAt(i, j)));
                    if (j < tableModel.getColumnCount() - 1) {
                        writer.write(" | ");
                    }
                }
                writer.newLine();
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed writing text backup snapshot", e);
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
        String category = String.valueOf(tableModel.getValueAt(modelRow, 1));

        boolean cloudSynced = !isCloudConfigured() || runCloudOperation("place order", () ->
                supabaseClient.placeSaleForInventoryItem(
                        session,
                        itemName,
                        category,
                        quantityOrdered,
                        itemPrice,
                        newQuantity
                )
        );

        tableModel.setValueAt(newQuantity, modelRow, 2);
        tableModel.setValueAt(updatedAt, modelRow, 5);

        updateTotalQuantity();
        saveInventory();
        logActionSafe("order_item", "Ordered: " + itemName + ", qty=" + quantityOrdered + ", remaining=" + newQuantity);

        if (cloudSynced) {
            updateStatusBar("Ordered " + quantityOrdered + " of '" + itemName + "'. Synced to cloud.", PRIMARY_COLOR.darker());
        } else {
            updateStatusBar("Ordered " + quantityOrdered + " of '" + itemName + "' locally (offline mode).", Color.ORANGE.darker());
        }
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
            JOptionPane.showMessageDialog(this, "Cloud is currently unavailable. Reconnect to view activity logs.", "Logs Unavailable", JOptionPane.WARNING_MESSAGE);
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

    private void showInventoryHubDialog() {
        if (!isCloudReady()) {
            JOptionPane.showMessageDialog(this, "Cloud is currently unavailable. Reconnect to use Inventory Hub.", "Inventory Unavailable", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (useTabbedInventoryWorkspace()) {
            showTabbedInventoryWorkspaceDialog();
            return;
        }

        String[] columns = {"Module", "ID", "Name", "Related", "Quantity", "Unit/Status", "Value", "Date", "Notes"};
        DefaultTableModel unifiedModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable unifiedTable = new JTable(unifiedModel);
        unifiedTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        unifiedTable.setRowHeight(24);
        unifiedTable.setFont(MAIN_FONT);
        unifiedTable.setForeground(TEXT_COLOR);
        unifiedTable.setBackground(Color.WHITE);
        unifiedTable.setGridColor(BORDER_COLOR.brighter());
        unifiedTable.getTableHeader().setFont(HEADER_FONT);
        unifiedTable.getTableHeader().setForeground(TEXT_COLOR);
        unifiedTable.getTableHeader().setBackground(BUTTON_COLOR);

        Runnable refreshData = () -> {
            unifiedModel.setRowCount(0);
            try {
                java.util.List<SupabaseClient.SupplierRecord> suppliers = supabaseClient.fetchSuppliers(session);
                for (SupabaseClient.SupplierRecord supplier : suppliers) {
                    unifiedModel.addRow(new Object[]{
                            SUPPLIER_TEXT,
                            supplier.getSupplierId(),
                            supplier.getSupplierName(),
                            supplier.getContactPerson(),
                            "",
                            supplier.getStatus(),
                            supplier.getPhone(),
                            "",
                            supplier.getEmail() + " | " + supplier.getAddress()
                    });
                }

                java.util.List<SupabaseClient.ExpirationRecord> expirations = supabaseClient.fetchExpirations(session);
                for (SupabaseClient.ExpirationRecord expiration : expirations) {
                    unifiedModel.addRow(new Object[]{
                            EXPIRATION_TEXT,
                            expiration.getExpirationId(),
                            expiration.getItemName(),
                            "",
                            expiration.getUnitQuantity(),
                            expiration.getUnitType(),
                            "",
                            expiration.getExpirationDate(),
                            ""
                    });
                }

                java.util.List<SupabaseClient.IngredientRecord> ingredients = supabaseClient.fetchIngredients(session);
                for (SupabaseClient.IngredientRecord ingredient : ingredients) {
                    unifiedModel.addRow(new Object[]{
                            INGREDIENTS_TEXT,
                            "",
                            ingredient.getProductName(),
                            ingredient.getItemName(),
                            ingredient.getQuantityNeeded(),
                            ingredient.getUnitType(),
                            "",
                            "",
                            ""
                    });
                }

                java.util.List<SupabaseClient.StockOutItemRecord> stockOuts = supabaseClient.fetchStockOutItems(session);
                for (SupabaseClient.StockOutItemRecord stockOut : stockOuts) {
                    unifiedModel.addRow(new Object[]{
                            STOCK_OUT_TEXT,
                            stockOut.getStockoutItemId(),
                            stockOut.getItemName(),
                            "",
                            stockOut.getQuantity(),
                            "",
                            stockOut.getCost(),
                            stockOut.getStockoutDate(),
                            stockOut.getReason()
                    });
                }
            } catch (IOException | InterruptedException ex) {
                JOptionPane.showMessageDialog(this, "Failed to load inventory data: " + ex.getMessage(), "Inventory Error", JOptionPane.ERROR_MESSAGE);
            }
        };
        refreshData.run();

        JDialog dialog = new JDialog(this, INVENTORY_HUB_TEXT, true);
        dialog.setLayout(new BorderLayout(8, 8));
        dialog.getContentPane().setBackground(SECONDARY_COLOR);
        JScrollPane scrollPane = new JScrollPane(unifiedTable);
        scrollPane.getViewport().setBackground(SECONDARY_COLOR);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        dialog.add(scrollPane, BorderLayout.CENTER);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionsPanel.setBackground(SECONDARY_COLOR);
        actionsPanel.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        JButton addButton = createDialogActionButton("Add");
        JButton editButton = createDialogActionButton("Edit");
        JButton deleteButton = createDialogActionButton("Delete");
        JButton refreshButton = createDialogActionButton("Refresh");
        JButton closeButton = createDialogActionButton("Close");

        addButton.addActionListener(event -> {
            event.getSource();
            String[] modules = {SUPPLIER_TEXT, EXPIRATION_TEXT, INGREDIENTS_TEXT, STOCK_OUT_TEXT};
            String module = (String) JOptionPane.showInputDialog(dialog, "Module:", "Add Record", JOptionPane.QUESTION_MESSAGE, null, modules, modules[0]);
            if (module == null) {
                return;
            }

            try {
                switch (module) {
                    case SUPPLIER_TEXT -> {
                    JTextField nameField = new JTextField(18);
                    JTextField contactField = new JTextField(18);
                    JTextField phoneField = new JTextField(18);
                    JTextField emailField = new JTextField(18);
                    JTextField addressField = new JTextField(18);
                    JTextField statusField = new JTextField("active", 18);
                    JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
                    panel.setBackground(SECONDARY_COLOR);
                    panel.add(new JLabel("Supplier Name:")); panel.add(nameField);
                    panel.add(new JLabel("Contact Person:")); panel.add(contactField);
                    panel.add(new JLabel("Phone:")); panel.add(phoneField);
                    panel.add(new JLabel("Email:")); panel.add(emailField);
                    panel.add(new JLabel("Address:")); panel.add(addressField);
                    panel.add(new JLabel("Status:")); panel.add(statusField);
                    if (JOptionPane.showConfirmDialog(dialog, panel, "Add Supplier", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
                        return;
                    }
                    if (nameField.getText().trim().isEmpty()) {
                        JOptionPane.showMessageDialog(dialog, "Supplier name is required.", "Validation", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    supabaseClient.addSupplier(session, nameField.getText().trim(), contactField.getText().trim(), phoneField.getText().trim(), emailField.getText().trim(), addressField.getText().trim(), statusField.getText().trim().isEmpty() ? "active" : statusField.getText().trim());
                    }
                    case EXPIRATION_TEXT -> {
                    JTextField itemField = new JTextField(18);
                    JTextField unitTypeField = new JTextField(18);
                    JTextField quantityField = new JTextField(18);
                    JTextField dateField = new JTextField("YYYY-MM-DD", 18);
                    JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
                    panel.setBackground(SECONDARY_COLOR);
                    panel.add(new JLabel("Item Name:")); panel.add(itemField);
                    panel.add(new JLabel("Unit Type:")); panel.add(unitTypeField);
                    panel.add(new JLabel("Unit Quantity:")); panel.add(quantityField);
                    panel.add(new JLabel("Expiration Date:")); panel.add(dateField);
                    if (JOptionPane.showConfirmDialog(dialog, panel, "Add Expiration", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
                        return;
                    }
                    if (!quantityField.getText().trim().matches("\\d+") || !dateField.getText().trim().matches("\\d{4}-\\d{2}-\\d{2}")) {
                        JOptionPane.showMessageDialog(dialog, "Quantity must be numeric and date must be YYYY-MM-DD.", "Validation", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    supabaseClient.addExpiration(session, itemField.getText().trim(), unitTypeField.getText().trim(), Integer.parseInt(quantityField.getText().trim()), dateField.getText().trim());
                    }
                    case INGREDIENTS_TEXT -> {
                    JTextField productField = new JTextField(18);
                    JTextField itemField = new JTextField(18);
                    JTextField unitTypeField = new JTextField(18);
                    JTextField quantityField = new JTextField(18);
                    JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
                    panel.setBackground(SECONDARY_COLOR);
                    panel.add(new JLabel("Product Name:")); panel.add(productField);
                    panel.add(new JLabel("Item Name:")); panel.add(itemField);
                    panel.add(new JLabel("Unit Type:")); panel.add(unitTypeField);
                    panel.add(new JLabel("Quantity Needed:")); panel.add(quantityField);
                    if (JOptionPane.showConfirmDialog(dialog, panel, "Add Ingredient", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
                        return;
                    }
                    if (!quantityField.getText().trim().matches("\\d+")) {
                        JOptionPane.showMessageDialog(dialog, "Quantity must be numeric.", "Validation", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    supabaseClient.addOrUpdateIngredient(session, productField.getText().trim(), itemField.getText().trim(), unitTypeField.getText().trim(), Integer.parseInt(quantityField.getText().trim()));
                    }
                    case STOCK_OUT_TEXT -> {
                    JTextField itemField = new JTextField(18);
                    JTextField quantityField = new JTextField(18);
                    JTextField costField = new JTextField(18);
                    JTextField reasonField = new JTextField(18);
                    JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
                    panel.setBackground(SECONDARY_COLOR);
                    panel.add(new JLabel("Item Name:")); panel.add(itemField);
                    panel.add(new JLabel("Quantity:")); panel.add(quantityField);
                    panel.add(new JLabel("Cost:")); panel.add(costField);
                    panel.add(new JLabel("Reason:")); panel.add(reasonField);
                    if (JOptionPane.showConfirmDialog(dialog, panel, "Record Stock Out", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
                        return;
                    }
                    if (!quantityField.getText().trim().matches("\\d+")) {
                        JOptionPane.showMessageDialog(dialog, "Quantity must be numeric.", "Validation", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    double costValue;
                    try {
                        costValue = costField.getText().trim().isEmpty() ? 0.0 : Double.parseDouble(costField.getText().trim());
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(dialog, "Cost must be numeric.", "Validation", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    supabaseClient.recordItemStockOut(session, itemField.getText().trim(), Integer.parseInt(quantityField.getText().trim()), costValue, reasonField.getText().trim());
                    loadInventory();
                    updateTotalQuantity();
                    }
                    default -> {
                        return;
                    }
                }
                refreshData.run();
            } catch (IOException | InterruptedException ex) {
                JOptionPane.showMessageDialog(dialog, "Add failed: " + ex.getMessage(), "Inventory Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        editButton.addActionListener(event -> {
            event.getSource();
            int selectedRow = unifiedTable.getSelectedRow();
            if (selectedRow < 0) {
                JOptionPane.showMessageDialog(dialog, "Select a row to edit.", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String module = String.valueOf(unifiedModel.getValueAt(selectedRow, 0));
            try {
                switch (module) {
                    case SUPPLIER_TEXT -> {
                    long supplierId = Long.parseLong(String.valueOf(unifiedModel.getValueAt(selectedRow, 1)));
                    JTextField nameField = new JTextField(String.valueOf(unifiedModel.getValueAt(selectedRow, 2)), 18);
                    JTextField contactField = new JTextField(String.valueOf(unifiedModel.getValueAt(selectedRow, 3)), 18);
                    JTextField phoneField = new JTextField(String.valueOf(unifiedModel.getValueAt(selectedRow, 6)), 18);
                    String notes = String.valueOf(unifiedModel.getValueAt(selectedRow, 8));
                    String[] parts = notes.split("\\|", 2);
                    JTextField emailField = new JTextField(parts.length > 0 ? parts[0].trim() : "", 18);
                    JTextField addressField = new JTextField(parts.length > 1 ? parts[1].trim() : "", 18);
                    JTextField statusField = new JTextField(String.valueOf(unifiedModel.getValueAt(selectedRow, 5)), 18);
                    JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
                    panel.setBackground(SECONDARY_COLOR);
                    panel.add(new JLabel("Supplier Name:")); panel.add(nameField);
                    panel.add(new JLabel("Contact Person:")); panel.add(contactField);
                    panel.add(new JLabel("Phone:")); panel.add(phoneField);
                    panel.add(new JLabel("Email:")); panel.add(emailField);
                    panel.add(new JLabel("Address:")); panel.add(addressField);
                    panel.add(new JLabel("Status:")); panel.add(statusField);
                    if (JOptionPane.showConfirmDialog(dialog, panel, "Edit Supplier", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
                        return;
                    }
                    supabaseClient.updateSupplierById(session, supplierId, nameField.getText().trim(), contactField.getText().trim(), phoneField.getText().trim(), emailField.getText().trim(), addressField.getText().trim(), statusField.getText().trim());
                    }
                    case EXPIRATION_TEXT -> {
                    long expirationId = Long.parseLong(String.valueOf(unifiedModel.getValueAt(selectedRow, 1)));
                    JTextField itemField = new JTextField(String.valueOf(unifiedModel.getValueAt(selectedRow, 2)), 18);
                    JTextField quantityField = new JTextField(String.valueOf(unifiedModel.getValueAt(selectedRow, 4)), 18);
                    JTextField unitTypeField = new JTextField(String.valueOf(unifiedModel.getValueAt(selectedRow, 5)), 18);
                    JTextField dateField = new JTextField(String.valueOf(unifiedModel.getValueAt(selectedRow, 7)), 18);
                    JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
                    panel.setBackground(SECONDARY_COLOR);
                    panel.add(new JLabel("Item Name:")); panel.add(itemField);
                    panel.add(new JLabel("Unit Type:")); panel.add(unitTypeField);
                    panel.add(new JLabel("Unit Quantity:")); panel.add(quantityField);
                    panel.add(new JLabel("Expiration Date:")); panel.add(dateField);
                    if (JOptionPane.showConfirmDialog(dialog, panel, "Edit Expiration", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
                        return;
                    }
                    if (!quantityField.getText().trim().matches("\\d+") || !dateField.getText().trim().matches("\\d{4}-\\d{2}-\\d{2}")) {
                        JOptionPane.showMessageDialog(dialog, "Quantity must be numeric and date must be YYYY-MM-DD.", "Validation", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    supabaseClient.updateExpirationById(session, expirationId, itemField.getText().trim(), unitTypeField.getText().trim(), Integer.parseInt(quantityField.getText().trim()), dateField.getText().trim());
                    }
                    case INGREDIENTS_TEXT -> {
                    String oldProduct = String.valueOf(unifiedModel.getValueAt(selectedRow, 2));
                    String oldItem = String.valueOf(unifiedModel.getValueAt(selectedRow, 3));
                    String oldUnitType = String.valueOf(unifiedModel.getValueAt(selectedRow, 5));

                    JTextField productField = new JTextField(oldProduct, 18);
                    JTextField itemField = new JTextField(oldItem, 18);
                    JTextField unitTypeField = new JTextField(oldUnitType, 18);
                    JTextField quantityField = new JTextField(String.valueOf(unifiedModel.getValueAt(selectedRow, 4)), 18);
                    JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
                    panel.setBackground(SECONDARY_COLOR);
                    panel.add(new JLabel("Product Name:")); panel.add(productField);
                    panel.add(new JLabel("Item Name:")); panel.add(itemField);
                    panel.add(new JLabel("Unit Type:")); panel.add(unitTypeField);
                    panel.add(new JLabel("Quantity Needed:")); panel.add(quantityField);
                    if (JOptionPane.showConfirmDialog(dialog, panel, "Edit Ingredient", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
                        return;
                    }
                    if (!quantityField.getText().trim().matches("\\d+")) {
                        JOptionPane.showMessageDialog(dialog, "Quantity must be numeric.", "Validation", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    supabaseClient.deleteIngredient(session, oldProduct, oldItem, oldUnitType);
                    supabaseClient.addOrUpdateIngredient(session, productField.getText().trim(), itemField.getText().trim(), unitTypeField.getText().trim(), Integer.parseInt(quantityField.getText().trim()));
                    }
                    default -> {
                        JOptionPane.showMessageDialog(dialog, "Stock Out rows are transaction history and are not editable.", "Edit Not Allowed", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                }
                refreshData.run();
            } catch (IOException | InterruptedException ex) {
                JOptionPane.showMessageDialog(dialog, "Edit failed: " + ex.getMessage(), "Inventory Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        deleteButton.addActionListener(event -> {
            event.getSource();
            int selectedRow = unifiedTable.getSelectedRow();
            if (selectedRow < 0) {
                JOptionPane.showMessageDialog(dialog, "Select a row to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String module = String.valueOf(unifiedModel.getValueAt(selectedRow, 0));
            int confirm = JOptionPane.showConfirmDialog(dialog, "Delete selected " + module + " row?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }

            try {
                switch (module) {
                    case SUPPLIER_TEXT -> {
                    long supplierId = Long.parseLong(String.valueOf(unifiedModel.getValueAt(selectedRow, 1)));
                    supabaseClient.deleteSupplierById(session, supplierId);
                    }
                    case EXPIRATION_TEXT -> {
                    long expirationId = Long.parseLong(String.valueOf(unifiedModel.getValueAt(selectedRow, 1)));
                    supabaseClient.deleteExpirationById(session, expirationId);
                    }
                    case INGREDIENTS_TEXT -> {
                    String productName = String.valueOf(unifiedModel.getValueAt(selectedRow, 2));
                    String itemName = String.valueOf(unifiedModel.getValueAt(selectedRow, 3));
                    String unitType = String.valueOf(unifiedModel.getValueAt(selectedRow, 5));
                    supabaseClient.deleteIngredient(session, productName, itemName, unitType);
                    }
                    default -> {
                        JOptionPane.showMessageDialog(dialog, "Stock Out rows are transaction history and cannot be deleted from here.", "Delete Not Allowed", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                }
                refreshData.run();
            } catch (IOException | InterruptedException ex) {
                JOptionPane.showMessageDialog(dialog, "Delete failed: " + ex.getMessage(), "Inventory Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        refreshButton.addActionListener(event -> {
            event.getSource();
            refreshData.run();
        });
        closeButton.addActionListener(event -> {
            event.getSource();
            dialog.dispose();
        });

        actionsPanel.add(addButton);
        actionsPanel.add(editButton);
        actionsPanel.add(deleteButton);
        actionsPanel.add(refreshButton);
        actionsPanel.add(closeButton);
        dialog.add(actionsPanel, BorderLayout.SOUTH);
        dialog.setSize(1100, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private boolean useTabbedInventoryWorkspace() {
        return true;
    }

    private JTable createWorkspaceTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        table.setFont(MAIN_FONT);
        table.setForeground(TEXT_COLOR);
        table.setBackground(Color.WHITE);
        table.setGridColor(BORDER_COLOR.brighter());
        table.getTableHeader().setFont(HEADER_FONT);
        table.getTableHeader().setForeground(TEXT_COLOR);
        table.getTableHeader().setBackground(BUTTON_COLOR);
        return table;
    }

    private void showTabbedInventoryWorkspaceDialog() {
        JDialog dialog = new JDialog(this, INVENTORY_HUB_TEXT, true);
        dialog.setLayout(new BorderLayout(8, 8));
        dialog.getContentPane().setBackground(SECONDARY_COLOR);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(MAIN_FONT.deriveFont(Font.BOLD));
        tabs.addTab("Items", createItemsWorkspaceTab(dialog));
        tabs.addTab("Suppliers", createSuppliersWorkspaceTab(dialog));
        tabs.addTab("Expirations", createExpirationsWorkspaceTab(dialog));
        tabs.addTab("Ingredients", createIngredientsWorkspaceTab(dialog));
        tabs.addTab("Stock Out", createStockOutWorkspaceTab(dialog));

        dialog.add(tabs, BorderLayout.CENTER);
        dialog.setSize(1100, 520);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private JPanel createItemsWorkspaceTab(JDialog dialog) {
        String[] columns = {"Item Name", "Category", "Quantity", "Price"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = createWorkspaceTable(model);

        Runnable refresh = () -> {
            model.setRowCount(0);
            try {
                for (SupabaseClient.InventoryRecord record : supabaseClient.fetchInventory(session)) {
                    model.addRow(new Object[]{record.getItemName(), record.getCategory(), record.getQuantity(), record.getPrice()});
                }
            } catch (IOException | InterruptedException ex) {
                JOptionPane.showMessageDialog(dialog, "Failed to load items: " + ex.getMessage(), "Inventory Error", JOptionPane.ERROR_MESSAGE);
            }
        };
        refresh.run();

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(SECONDARY_COLOR);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setBackground(SECONDARY_COLOR);
        actions.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        JButton add = createDialogActionButton("Add");
        JButton edit = createDialogActionButton("Edit");
        JButton delete = createDialogActionButton("Delete");
        JButton refreshBtn = createDialogActionButton("Refresh");

        add.addActionListener(event -> {
            event.getSource();
            JTextField name = new JTextField(18);
            JTextField category = new JTextField(18);
            JTextField qty = new JTextField(18);
            JTextField price = new JTextField(18);
            JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
            form.setBackground(SECONDARY_COLOR);
            form.add(new JLabel("Item Name:")); form.add(name);
            form.add(new JLabel("Category:")); form.add(category);
            form.add(new JLabel("Quantity:")); form.add(qty);
            form.add(new JLabel("Price:")); form.add(price);
            if (JOptionPane.showConfirmDialog(dialog, form, "Add Item", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
                return;
            }
            if (!qty.getText().trim().matches("\\d+")) {
                JOptionPane.showMessageDialog(dialog, "Quantity must be numeric.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                double parsedPrice = Double.parseDouble(price.getText().trim());
                supabaseClient.insertInventoryItem(session, name.getText().trim(), category.getText().trim(), Integer.parseInt(qty.getText().trim()), parsedPrice, "", "");
                refresh.run();
                loadInventory();
                updateTotalQuantity();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Price must be numeric.", "Validation", JOptionPane.WARNING_MESSAGE);
            } catch (IOException | InterruptedException ex) {
                JOptionPane.showMessageDialog(dialog, "Add item failed: " + ex.getMessage(), "Inventory Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        edit.addActionListener(event -> {
            event.getSource();
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(dialog, "Select an item to edit.", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String oldName = String.valueOf(model.getValueAt(row, 0));
            JTextField name = new JTextField(oldName, 18);
            JTextField category = new JTextField(String.valueOf(model.getValueAt(row, 1)), 18);
            JTextField qty = new JTextField(String.valueOf(model.getValueAt(row, 2)), 18);
            JTextField price = new JTextField(String.valueOf(model.getValueAt(row, 3)), 18);
            JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
            form.setBackground(SECONDARY_COLOR);
            form.add(new JLabel("Item Name:")); form.add(name);
            form.add(new JLabel("Category:")); form.add(category);
            form.add(new JLabel("Quantity:")); form.add(qty);
            form.add(new JLabel("Price:")); form.add(price);
            if (JOptionPane.showConfirmDialog(dialog, form, "Edit Item", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
                return;
            }
            if (!qty.getText().trim().matches("\\d+")) {
                JOptionPane.showMessageDialog(dialog, "Quantity must be numeric.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                double parsedPrice = Double.parseDouble(price.getText().trim());
                supabaseClient.updateInventoryItemByName(session, oldName, name.getText().trim(), category.getText().trim(), Integer.parseInt(qty.getText().trim()), parsedPrice, "");
                refresh.run();
                loadInventory();
                updateTotalQuantity();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Price must be numeric.", "Validation", JOptionPane.WARNING_MESSAGE);
            } catch (IOException | InterruptedException ex) {
                JOptionPane.showMessageDialog(dialog, "Edit item failed: " + ex.getMessage(), "Inventory Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        delete.addActionListener(event -> {
            event.getSource();
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(dialog, "Select an item to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String itemName = String.valueOf(model.getValueAt(row, 0));
            if (JOptionPane.showConfirmDialog(dialog, "Delete item '" + itemName + "'?", "Confirm Delete", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                return;
            }
            try {
                supabaseClient.deleteInventoryItemByName(session, itemName);
                refresh.run();
                loadInventory();
                updateTotalQuantity();
            } catch (IOException | InterruptedException ex) {
                JOptionPane.showMessageDialog(dialog, "Delete item failed: " + ex.getMessage(), "Inventory Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        refreshBtn.addActionListener(event -> {
            event.getSource();
            refresh.run();
        });

        actions.add(add);
        actions.add(edit);
        actions.add(delete);
        actions.add(refreshBtn);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createSuppliersWorkspaceTab(JDialog dialog) {
        String[] columns = {"ID", "Name", "Contact", "Phone", "Email", "Address", "Status"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = createWorkspaceTable(model);
        Runnable refresh = () -> {
            model.setRowCount(0);
            try {
                for (SupabaseClient.SupplierRecord supplier : supabaseClient.fetchSuppliers(session)) {
                    model.addRow(new Object[]{supplier.getSupplierId(), supplier.getSupplierName(), supplier.getContactPerson(), supplier.getPhone(), supplier.getEmail(), supplier.getAddress(), supplier.getStatus()});
                }
            } catch (IOException | InterruptedException ex) {
                JOptionPane.showMessageDialog(dialog, "Failed to load suppliers: " + ex.getMessage(), "Inventory Error", JOptionPane.ERROR_MESSAGE);
            }
        };
        refresh.run();

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(SECONDARY_COLOR);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setBackground(SECONDARY_COLOR);
        actions.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        JButton add = createDialogActionButton("Add");
        JButton edit = createDialogActionButton("Edit");
        JButton delete = createDialogActionButton("Delete");
        JButton refreshBtn = createDialogActionButton("Refresh");

        add.addActionListener(event -> {
            event.getSource();
            JTextField name = new JTextField(18);
            JTextField contact = new JTextField(18);
            JTextField phone = new JTextField(18);
            JTextField email = new JTextField(18);
            JTextField address = new JTextField(18);
            JTextField status = new JTextField("active", 18);
            JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
            form.setBackground(SECONDARY_COLOR);
            form.add(new JLabel("Supplier Name:")); form.add(name);
            form.add(new JLabel("Contact Person:")); form.add(contact);
            form.add(new JLabel("Phone:")); form.add(phone);
            form.add(new JLabel("Email:")); form.add(email);
            form.add(new JLabel("Address:")); form.add(address);
            form.add(new JLabel("Status:")); form.add(status);
            if (JOptionPane.showConfirmDialog(dialog, form, "Add Supplier", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
                return;
            }
            try {
                supabaseClient.addSupplier(session, name.getText().trim(), contact.getText().trim(), phone.getText().trim(), email.getText().trim(), address.getText().trim(), status.getText().trim().isEmpty() ? "active" : status.getText().trim());
                refresh.run();
            } catch (IOException | InterruptedException ex) {
                JOptionPane.showMessageDialog(dialog, "Add supplier failed: " + ex.getMessage(), "Inventory Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        edit.addActionListener(event -> {
            event.getSource();
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(dialog, "Select a supplier to edit.", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            long id = Long.parseLong(String.valueOf(model.getValueAt(row, 0)));
            JTextField name = new JTextField(String.valueOf(model.getValueAt(row, 1)), 18);
            JTextField contact = new JTextField(String.valueOf(model.getValueAt(row, 2)), 18);
            JTextField phone = new JTextField(String.valueOf(model.getValueAt(row, 3)), 18);
            JTextField email = new JTextField(String.valueOf(model.getValueAt(row, 4)), 18);
            JTextField address = new JTextField(String.valueOf(model.getValueAt(row, 5)), 18);
            JTextField status = new JTextField(String.valueOf(model.getValueAt(row, 6)), 18);
            JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
            form.setBackground(SECONDARY_COLOR);
            form.add(new JLabel("Supplier Name:")); form.add(name);
            form.add(new JLabel("Contact Person:")); form.add(contact);
            form.add(new JLabel("Phone:")); form.add(phone);
            form.add(new JLabel("Email:")); form.add(email);
            form.add(new JLabel("Address:")); form.add(address);
            form.add(new JLabel("Status:")); form.add(status);
            if (JOptionPane.showConfirmDialog(dialog, form, "Edit Supplier", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
                return;
            }
            try {
                supabaseClient.updateSupplierById(session, id, name.getText().trim(), contact.getText().trim(), phone.getText().trim(), email.getText().trim(), address.getText().trim(), status.getText().trim());
                refresh.run();
            } catch (IOException | InterruptedException ex) {
                JOptionPane.showMessageDialog(dialog, "Edit supplier failed: " + ex.getMessage(), "Inventory Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        delete.addActionListener(event -> {
            event.getSource();
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(dialog, "Select a supplier to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            long id = Long.parseLong(String.valueOf(model.getValueAt(row, 0)));
            if (JOptionPane.showConfirmDialog(dialog, "Delete selected supplier?", "Confirm Delete", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                return;
            }
            try {
                supabaseClient.deleteSupplierById(session, id);
                refresh.run();
            } catch (IOException | InterruptedException ex) {
                JOptionPane.showMessageDialog(dialog, "Delete supplier failed: " + ex.getMessage(), "Inventory Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        refreshBtn.addActionListener(event -> {
            event.getSource();
            refresh.run();
        });

        actions.add(add);
        actions.add(edit);
        actions.add(delete);
        actions.add(refreshBtn);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createExpirationsWorkspaceTab(JDialog dialog) {
        String[] columns = {"ID", "Item", "Unit Type", "Quantity", "Expiration Date"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = createWorkspaceTable(model);
        Runnable refresh = () -> {
            model.setRowCount(0);
            try {
                for (SupabaseClient.ExpirationRecord expiration : supabaseClient.fetchExpirations(session)) {
                    model.addRow(new Object[]{expiration.getExpirationId(), expiration.getItemName(), expiration.getUnitType(), expiration.getUnitQuantity(), expiration.getExpirationDate()});
                }
            } catch (IOException | InterruptedException ex) {
                JOptionPane.showMessageDialog(dialog, "Failed to load expirations: " + ex.getMessage(), "Inventory Error", JOptionPane.ERROR_MESSAGE);
            }
        };
        refresh.run();

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(SECONDARY_COLOR);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setBackground(SECONDARY_COLOR);
        actions.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        JButton add = createDialogActionButton("Add");
        JButton edit = createDialogActionButton("Edit");
        JButton delete = createDialogActionButton("Delete");
        JButton refreshBtn = createDialogActionButton("Refresh");

        add.addActionListener(event -> {
            event.getSource();
            JTextField item = new JTextField(18);
            JTextField unit = new JTextField(18);
            JTextField qty = new JTextField(18);
            JTextField date = new JTextField("YYYY-MM-DD", 18);
            JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
            form.setBackground(SECONDARY_COLOR);
            form.add(new JLabel("Item Name:")); form.add(item);
            form.add(new JLabel("Unit Type:")); form.add(unit);
            form.add(new JLabel("Unit Quantity:")); form.add(qty);
            form.add(new JLabel("Expiration Date:")); form.add(date);
            if (JOptionPane.showConfirmDialog(dialog, form, "Add Expiration", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
                return;
            }
            if (!qty.getText().trim().matches("\\d+") || !date.getText().trim().matches("\\d{4}-\\d{2}-\\d{2}")) {
                JOptionPane.showMessageDialog(dialog, "Quantity must be numeric and date must be YYYY-MM-DD.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                supabaseClient.addExpiration(session, item.getText().trim(), unit.getText().trim(), Integer.parseInt(qty.getText().trim()), date.getText().trim());
                refresh.run();
            } catch (IOException | InterruptedException ex) {
                JOptionPane.showMessageDialog(dialog, "Add expiration failed: " + ex.getMessage(), "Inventory Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        edit.addActionListener(event -> {
            event.getSource();
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(dialog, "Select an expiration row to edit.", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            long id = Long.parseLong(String.valueOf(model.getValueAt(row, 0)));
            JTextField item = new JTextField(String.valueOf(model.getValueAt(row, 1)), 18);
            JTextField unit = new JTextField(String.valueOf(model.getValueAt(row, 2)), 18);
            JTextField qty = new JTextField(String.valueOf(model.getValueAt(row, 3)), 18);
            JTextField date = new JTextField(String.valueOf(model.getValueAt(row, 4)), 18);
            JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
            form.setBackground(SECONDARY_COLOR);
            form.add(new JLabel("Item Name:")); form.add(item);
            form.add(new JLabel("Unit Type:")); form.add(unit);
            form.add(new JLabel("Unit Quantity:")); form.add(qty);
            form.add(new JLabel("Expiration Date:")); form.add(date);
            if (JOptionPane.showConfirmDialog(dialog, form, "Edit Expiration", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
                return;
            }
            if (!qty.getText().trim().matches("\\d+") || !date.getText().trim().matches("\\d{4}-\\d{2}-\\d{2}")) {
                JOptionPane.showMessageDialog(dialog, "Quantity must be numeric and date must be YYYY-MM-DD.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                supabaseClient.updateExpirationById(session, id, item.getText().trim(), unit.getText().trim(), Integer.parseInt(qty.getText().trim()), date.getText().trim());
                refresh.run();
            } catch (IOException | InterruptedException ex) {
                JOptionPane.showMessageDialog(dialog, "Edit expiration failed: " + ex.getMessage(), "Inventory Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        delete.addActionListener(event -> {
            event.getSource();
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(dialog, "Select an expiration row to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            long id = Long.parseLong(String.valueOf(model.getValueAt(row, 0)));
            if (JOptionPane.showConfirmDialog(dialog, "Delete selected expiration?", "Confirm Delete", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                return;
            }
            try {
                supabaseClient.deleteExpirationById(session, id);
                refresh.run();
            } catch (IOException | InterruptedException ex) {
                JOptionPane.showMessageDialog(dialog, "Delete expiration failed: " + ex.getMessage(), "Inventory Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        refreshBtn.addActionListener(event -> {
            event.getSource();
            refresh.run();
        });

        actions.add(add);
        actions.add(edit);
        actions.add(delete);
        actions.add(refreshBtn);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createIngredientsWorkspaceTab(JDialog dialog) {
        String[] columns = {"Product", "Item", "Unit Type", "Quantity Needed"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = createWorkspaceTable(model);
        Runnable refresh = () -> {
            model.setRowCount(0);
            try {
                for (SupabaseClient.IngredientRecord ingredient : supabaseClient.fetchIngredients(session)) {
                    model.addRow(new Object[]{ingredient.getProductName(), ingredient.getItemName(), ingredient.getUnitType(), ingredient.getQuantityNeeded()});
                }
            } catch (IOException | InterruptedException ex) {
                JOptionPane.showMessageDialog(dialog, "Failed to load ingredients: " + ex.getMessage(), "Inventory Error", JOptionPane.ERROR_MESSAGE);
            }
        };
        refresh.run();

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(SECONDARY_COLOR);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setBackground(SECONDARY_COLOR);
        actions.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        JButton add = createDialogActionButton("Add");
        JButton edit = createDialogActionButton("Edit");
        JButton delete = createDialogActionButton("Delete");
        JButton refreshBtn = createDialogActionButton("Refresh");

        add.addActionListener(event -> {
            event.getSource();
            JTextField product = new JTextField(18);
            JTextField item = new JTextField(18);
            JTextField unit = new JTextField(18);
            JTextField qty = new JTextField(18);
            JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
            form.setBackground(SECONDARY_COLOR);
            form.add(new JLabel("Product Name:")); form.add(product);
            form.add(new JLabel("Item Name:")); form.add(item);
            form.add(new JLabel("Unit Type:")); form.add(unit);
            form.add(new JLabel("Quantity Needed:")); form.add(qty);
            if (JOptionPane.showConfirmDialog(dialog, form, "Add Ingredient", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
                return;
            }
            if (!qty.getText().trim().matches("\\d+")) {
                JOptionPane.showMessageDialog(dialog, "Quantity must be numeric.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                supabaseClient.addOrUpdateIngredient(session, product.getText().trim(), item.getText().trim(), unit.getText().trim(), Integer.parseInt(qty.getText().trim()));
                refresh.run();
            } catch (IOException | InterruptedException ex) {
                JOptionPane.showMessageDialog(dialog, "Add ingredient failed: " + ex.getMessage(), "Inventory Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        edit.addActionListener(event -> {
            event.getSource();
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(dialog, "Select an ingredient row to edit.", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String oldProduct = String.valueOf(model.getValueAt(row, 0));
            String oldItem = String.valueOf(model.getValueAt(row, 1));
            String oldUnit = String.valueOf(model.getValueAt(row, 2));
            JTextField product = new JTextField(oldProduct, 18);
            JTextField item = new JTextField(oldItem, 18);
            JTextField unit = new JTextField(oldUnit, 18);
            JTextField qty = new JTextField(String.valueOf(model.getValueAt(row, 3)), 18);
            JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
            form.setBackground(SECONDARY_COLOR);
            form.add(new JLabel("Product Name:")); form.add(product);
            form.add(new JLabel("Item Name:")); form.add(item);
            form.add(new JLabel("Unit Type:")); form.add(unit);
            form.add(new JLabel("Quantity Needed:")); form.add(qty);
            if (JOptionPane.showConfirmDialog(dialog, form, "Edit Ingredient", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
                return;
            }
            if (!qty.getText().trim().matches("\\d+")) {
                JOptionPane.showMessageDialog(dialog, "Quantity must be numeric.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                supabaseClient.deleteIngredient(session, oldProduct, oldItem, oldUnit);
                supabaseClient.addOrUpdateIngredient(session, product.getText().trim(), item.getText().trim(), unit.getText().trim(), Integer.parseInt(qty.getText().trim()));
                refresh.run();
            } catch (IOException | InterruptedException ex) {
                JOptionPane.showMessageDialog(dialog, "Edit ingredient failed: " + ex.getMessage(), "Inventory Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        delete.addActionListener(event -> {
            event.getSource();
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(dialog, "Select an ingredient row to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (JOptionPane.showConfirmDialog(dialog, "Delete selected ingredient?", "Confirm Delete", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                return;
            }
            try {
                supabaseClient.deleteIngredient(session, String.valueOf(model.getValueAt(row, 0)), String.valueOf(model.getValueAt(row, 1)), String.valueOf(model.getValueAt(row, 2)));
                refresh.run();
            } catch (IOException | InterruptedException ex) {
                JOptionPane.showMessageDialog(dialog, "Delete ingredient failed: " + ex.getMessage(), "Inventory Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        refreshBtn.addActionListener(event -> {
            event.getSource();
            refresh.run();
        });

        actions.add(add);
        actions.add(edit);
        actions.add(delete);
        actions.add(refreshBtn);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createStockOutWorkspaceTab(JDialog dialog) {
        String[] columns = {"ID", "Date", "Reason", "Item", "Quantity", "Cost"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = createWorkspaceTable(model);
        Runnable refresh = () -> {
            model.setRowCount(0);
            try {
                for (SupabaseClient.StockOutItemRecord record : supabaseClient.fetchStockOutItems(session)) {
                    model.addRow(new Object[]{record.getStockoutItemId(), record.getStockoutDate(), record.getReason(), record.getItemName(), record.getQuantity(), record.getCost()});
                }
            } catch (IOException | InterruptedException ex) {
                JOptionPane.showMessageDialog(dialog, "Failed to load stock out records: " + ex.getMessage(), "Inventory Error", JOptionPane.ERROR_MESSAGE);
            }
        };
        refresh.run();

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(SECONDARY_COLOR);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setBackground(SECONDARY_COLOR);
        actions.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        JButton record = createDialogActionButton("Record");
        JButton refreshBtn = createDialogActionButton("Refresh");

        record.addActionListener(event -> {
            event.getSource();
            JTextField item = new JTextField(18);
            JTextField qty = new JTextField(18);
            JTextField cost = new JTextField(18);
            JTextField reason = new JTextField(18);
            JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
            form.setBackground(SECONDARY_COLOR);
            form.add(new JLabel("Item Name:")); form.add(item);
            form.add(new JLabel("Quantity:")); form.add(qty);
            form.add(new JLabel("Cost:")); form.add(cost);
            form.add(new JLabel("Reason:")); form.add(reason);
            if (JOptionPane.showConfirmDialog(dialog, form, "Record Stock Out", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
                return;
            }
            if (!qty.getText().trim().matches("\\d+")) {
                JOptionPane.showMessageDialog(dialog, "Quantity must be numeric.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            double parsedCost;
            try {
                parsedCost = cost.getText().trim().isEmpty() ? 0.0 : Double.parseDouble(cost.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Cost must be numeric.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                supabaseClient.recordItemStockOut(session, item.getText().trim(), Integer.parseInt(qty.getText().trim()), parsedCost, reason.getText().trim());
                refresh.run();
                loadInventory();
                updateTotalQuantity();
            } catch (IOException | InterruptedException ex) {
                JOptionPane.showMessageDialog(dialog, "Record stock out failed: " + ex.getMessage(), "Inventory Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        refreshBtn.addActionListener(event -> {
            event.getSource();
            refresh.run();
        });

        actions.add(record);
        actions.add(refreshBtn);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
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

    private boolean isCloudConfigured() {
        return supabaseClient != null && session != null && session.getAccessToken() != null && !session.getAccessToken().isBlank();
    }

    private boolean isCloudReady() {
        return isCloudConfigured() && cloudConnected;
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

    private void handleShowInventoryHubDialogAction(ActionEvent event) {
        event.getSource();
        showInventoryHubDialog();
    }

    private void handleExportTextBackupAction(ActionEvent event) {
        event.getSource();
        exportCurrentDataAsText();
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
