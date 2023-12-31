import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Color;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpenseTracker {
    private static Connection connection;
    private JFrame frame;
    private JTable table;
    private JTextField dateField;
    private JTextField categoryField;
    private JTextField amountField;
    private JTextField nameField;
    private int currentAccountId = 0;
    private JButton btnFindExpense;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    ExpenseTracker window = new ExpenseTracker();
                    window.frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public ExpenseTracker() {
        initializeDatabase();
        initialize();
        frame.setVisible(true);
    }

    private void initializeDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            String url = "jdbc:mysql://localhost:3306/expenses";
            String user = "root";
            String password = "pratyush@10";

            connection = DriverManager.getConnection(url, user, password);

            Statement statement = connection.createStatement();
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS accounts (" +
                "id INT PRIMARY KEY," +
                "name VARCHAR(255)," +
                "password VARCHAR(255)" +
                ");");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS expenditure (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "account_id INT," +
                "date VARCHAR(255)," +
                "category TEXT," +
                "amount DOUBLE," +
                "FOREIGN KEY (account_id) REFERENCES accounts(id)" +
                ");");

            statement.close();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    private boolean isPasswordCorrect(int accountId, String password) {
        try {
            String sql = "SELECT password FROM accounts WHERE id = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, accountId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String storedPassword = rs.getString("password");
                return password.equals(storedPassword);
            }
            return false;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error checking password: " + e.getMessage());
            return false;
        }
    }    
    
    private void addAccount(String accountName, String password) {
        try {
            String getMaxIdSql = "SELECT MAX(id) FROM accounts";
            Statement getMaxIdStatement = connection.createStatement();
            ResultSet resultSet = getMaxIdStatement.executeQuery(getMaxIdSql);
            int maxId = 0;
    
            if (resultSet.next()) {
                maxId = resultSet.getInt(1);
            }
            
            int newId = maxId + 1;

            String insertSql = "INSERT INTO accounts (id, name, password) VALUES (?, ?, ?)";
            PreparedStatement stmt = connection.prepareStatement(insertSql);
            stmt.setInt(1, newId);
            stmt.setString(2, accountName);
            stmt.setString(3, password);
            stmt.executeUpdate();
            
            stmt.close();
            getMaxIdStatement.close();
            JOptionPane.showMessageDialog(frame, "Account Added Successfully");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error adding account: " + e.getMessage());
        }
    }

    public void loadData(DefaultTableModel model, int acId) throws SQLException {
        model.setRowCount(0);

        String sql = "SELECT date,category,amount FROM expenditure WHERE account_id = ? ;";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, acId);
        ResultSet rs = ps.executeQuery();
        Object[] row = new Object[3];
        while (rs.next()) {
            for (int i = 0; i < row.length; i++) {
                row[i] = rs.getObject(i + 1);
            }
            model.addRow(row);
        }
        ps.close();
    }

    private String[] getAccountDetails(int accountId) {
        double totalExpense = 0.0;
        String accountName = null;
        try {
            String accountSql = "SELECT name FROM accounts WHERE id = ?";
            PreparedStatement accountStmt = connection.prepareStatement(accountSql);
            accountStmt.setInt(1, accountId);

            ResultSet accountResult = accountStmt.executeQuery();
            if (accountResult.next()) {
                accountName = accountResult.getString("name");

                String expenseSql = "SELECT SUM(amount) AS total FROM expenditure WHERE account_id = ?";
                PreparedStatement expenseStmt = connection.prepareStatement(expenseSql);
                expenseStmt.setInt(1, accountId);

                ResultSet expenseResult = expenseStmt.executeQuery();
                if (expenseResult.next()) {
                    totalExpense = expenseResult.getDouble("total");
                }
                expenseResult.close();
                expenseStmt.close();
            }
            accountResult.close();
            accountStmt.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error retrieving account details: " + e.getMessage());
        }
        String[] detail = {accountName, totalExpense + ""};
        return detail;
    }

    private void addExpense(int accountId, String date, String category, double amount) {
        try {
            String sql = "INSERT INTO expenditure (account_id, date, category, amount) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, accountId);
            stmt.setString(2, date);
            stmt.setString(3, category);
            stmt.setDouble(4, amount);
            stmt.executeUpdate();

            stmt.close();
            JOptionPane.showMessageDialog(frame, "Expense added successfully to Account ID: " + accountId);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error adding expense: " + e.getMessage());
        }
    }

    public void updateComboBox(JComboBox<String> comboBox) throws SQLException {
        comboBox.removeAllItems();

        String sql = "SELECT * FROM accounts;";
        PreparedStatement ps = connection.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            comboBox.addItem(rs.getString("id") + "|" + rs.getString("name"));
        }
        rs.close();
        ps.close();
    }

    private void updateExpense(int accountId, String oldDate, String oldCategory, double oldAmount, String newDate, String newCategory, double newAmount) {
        try {
        String sql = "UPDATE expenditure SET date = ?, category = ?, amount = ? " +
        "WHERE account_id = ? AND date = ? AND category = ? AND amount = ?";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, newDate);
        stmt.setString(2, newCategory);
        stmt.setDouble(3, newAmount);
        stmt.setInt(4, accountId);
        stmt.setString(5, oldDate);
        stmt.setString(6, oldCategory);
        stmt.setDouble(7, oldAmount);
        stmt.executeUpdate();
        stmt.close();
        JOptionPane.showMessageDialog(frame, "Expense updated successfully.");
        } catch (SQLException e) {
        JOptionPane.showMessageDialog(frame, "Error updating expense: " + e.getMessage());
        }
    }

    private void deleteExpense(int accountId, String date, String category, double amount) {
        try {
            String sql = "DELETE FROM expenditure WHERE account_id = ? AND date = ? AND category = ? AND amount = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, accountId);
            stmt.setString(2, date);
            stmt.setString(3, category);
            stmt.setDouble(4, amount);
            stmt.executeUpdate();
            stmt.close();
            JOptionPane.showMessageDialog(frame, "Expense deleted successfully.");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error deleting expense: " + e.getMessage());
        }
    }
   
    private void findExpensesByDate(int accountId, String searchStartDate, String searchEndDate, String sql) {
        try {
            SimpleDateFormat inputDateFormat = new SimpleDateFormat("dd.MM.yyyy");
            Date startDate = inputDateFormat.parse(searchStartDate);
    
            String formattedStartDate = new SimpleDateFormat("yyyy-MM-dd").format(startDate);
    
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, accountId);
            ps.setString(2, formattedStartDate);
    
            if (!searchEndDate.isEmpty()) {
                Date endDate = inputDateFormat.parse(searchEndDate);
                String formattedEndDate = new SimpleDateFormat("yyyy-MM-dd").format(endDate);
                ps.setString(3, formattedEndDate);
            }
    
            ResultSet rs = ps.executeQuery();
    
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.setRowCount(0);
    
            while (rs.next()) {
                Object[] row = { rs.getString("date"), rs.getString("category"), rs.getDouble("amount") };
                model.addRow(row);
            }
    
            rs.close();
            ps.close();
        } catch (SQLException | ParseException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Error finding expenses: " + e.getMessage());
        }
    }
    
    private void generatePieChart(DefaultTableModel model) {
        List<String> categories = new ArrayList<>();
        List<Double> amounts = new ArrayList<>();

        for (int row = 0; row < model.getRowCount(); row++) {
            String category = (String) model.getValueAt(row, 1);
            double amount = (Double) model.getValueAt(row, 2);
            categories.add(category);
            amounts.add(amount);
        }

        Map<String, Double> categoryExpenseMap = new HashMap<>();

        for (int i = 0; i < categories.size(); i++) {
            String category = categories.get(i);
            double amount = amounts.get(i);
            categoryExpenseMap.put(category, categoryExpenseMap.getOrDefault(category, 0.0) + amount);
        }

        ProcessBuilder processBuilder = new ProcessBuilder("python", "generate_pie_chart.py");
        for (Map.Entry<String, Double> entry : categoryExpenseMap.entrySet()) {
            processBuilder.command().add(entry.getKey() + ":" + entry.getValue());
        }

        try {
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Pie chart generated successfully.");
                try {
                    Desktop.getDesktop().open(new File("pie_chart.png"));
                } catch (IOException ex) {
                    ex.printStackTrace();
                    System.err.println("Error opening the pie chart image.");
                }
            } else {
                System.err.println("Error generating the pie chart.");
            }
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
        }
    }
    
    private void initialize() {
        frame = new JFrame();
        frame.setBounds(100, 100, 600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(null);
        frame.setTitle("Expense Tracker");
        frame.getContentPane().setBackground(Color.BLACK); 

        JPanel topPanel = new JPanel();
        topPanel.setBounds(0, 0, 600, 58);
        topPanel.setBackground(Color.BLACK); 
        frame.getContentPane().add(topPanel);
        topPanel.setLayout(null);

        JLabel lblSelectAc = new JLabel("Select A/C:");
        lblSelectAc.setBounds(2, 5, 75, 15);
        lblSelectAc.setForeground(Color.CYAN);
        topPanel.add(lblSelectAc);

        JComboBox<String> accountComboBox = new JComboBox<>();
        accountComboBox.setBounds(86, 0, 130, 24);
        accountComboBox.setBackground(Color.BLACK);
        accountComboBox.setForeground(Color.RED);
        topPanel.add(accountComboBox);

        JLabel lblName = new JLabel("Name:");
        lblName.setBounds(2, 32, 70, 15);
        lblName.setForeground(Color.CYAN);
        topPanel.add(lblName);

        nameField = new JTextField();
        nameField.setColumns(10);
        nameField.setBounds(86, 27, 130, 30);
        nameField.setBackground(Color.BLACK);
        nameField.setForeground(Color.RED);
        topPanel.add(nameField);

        JButton btnAddAc = new JButton("Add A/C");
        btnAddAc.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String accountName = nameField.getText();
                String password = JOptionPane.showInputDialog(frame, "Enter Password for the new account");
        
                if (password != null) {
                    addAccount(accountName, password);
                    try {
                        updateComboBox(accountComboBox);
                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    }
                } else {
                    JOptionPane.showMessageDialog(frame, "Account creation canceled. A password is required.");
                }
            }
        });        
        btnAddAc.setBounds(223, 27, 117, 30);
        topPanel.add(btnAddAc);

        JButton btnSelect = new JButton("Select");
        btnSelect.setBounds(223, 0, 117, 25);
        topPanel.add(btnSelect);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBounds(0, 60, 590, 211);
        scrollPane.setBackground(Color.BLACK);
        frame.getContentPane().add(scrollPane);

        table = new JTable();
        table.setModel(new DefaultTableModel(
                new Object[][]{{null, null, null}},
                new String[]{"Date", "Category", "Amount"}
        ));
        table.setBackground(Color.BLACK);
        table.setForeground(Color.GREEN);
        JTableHeader tableHeader = table.getTableHeader();
        tableHeader.setBackground(Color.BLACK);
        tableHeader.setForeground(Color.CYAN);
        scrollPane.setViewportView(table);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setBounds(0, 270, 600, 90);
        bottomPanel.setBackground(Color.BLACK);
        frame.getContentPane().add(bottomPanel);
        bottomPanel.setLayout(null);

        JLabel dateLabel = new JLabel("Date:");
        dateLabel.setBounds(0, 5, 50, 15);
        dateLabel.setForeground(Color.CYAN);
        bottomPanel.add(dateLabel);

        dateField = new JTextField();
        dateField.setBounds(50, 5, 114, 30);
        dateField.setBackground(Color.BLACK);
        dateField.setForeground(Color.RED);
        bottomPanel.add(dateField);
        dateField.setColumns(10);

        JLabel descLabel = new JLabel("Category:");
        descLabel.setBounds(180, 5, 90, 15);
        descLabel.setForeground(Color.CYAN);
        bottomPanel.add(descLabel);

        categoryField = new JTextField();
        categoryField.setColumns(10);
        categoryField.setBounds(270, 5, 114, 30);
        categoryField.setBackground(Color.BLACK);
        categoryField.setForeground(Color.RED);
        bottomPanel.add(categoryField);

        JLabel amountLabel = new JLabel("Amount:");
        amountLabel.setBounds(390, 5, 70, 15);
        amountLabel.setForeground(Color.CYAN);
        bottomPanel.add(amountLabel);

        amountField = new JTextField();
        amountField.setColumns(10);
        amountField.setBounds(456, 5, 114, 30);
        amountField.setBackground(Color.BLACK);
        amountField.setForeground(Color.RED);
        bottomPanel.add(amountField);

        JButton btnAddExpense = new JButton("Add");
        btnAddExpense.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addExpense(currentAccountId, dateField.getText(), categoryField.getText(),
                        Double.valueOf(amountField.getText()));
                try {
                    loadData((DefaultTableModel) table.getModel(), currentAccountId);
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
            }
        });
        btnAddExpense.setBounds(18, 39, 100, 25);
        bottomPanel.add(btnAddExpense);

        JLabel lblTotalExpense = new JLabel("Total Expense : ");
        lblTotalExpense.setBounds(22, 75, 120, 15);
        lblTotalExpense.setForeground(Color.CYAN);
        bottomPanel.add(lblTotalExpense);

        JLabel lblTotalAmount = new JLabel("--");
        lblTotalAmount.setBounds(143, 75, 70, 15);
        lblTotalAmount.setForeground(Color.RED);
        bottomPanel.add(lblTotalAmount);

        JLabel lblCurrentAcc = new JLabel("Current Acc Name:");
        lblCurrentAcc.setBounds(270, 76, 130, 15);
        lblCurrentAcc.setForeground(Color.CYAN);
        bottomPanel.add(lblCurrentAcc);

        JLabel lblAccountName = new JLabel("Account Name");
        lblAccountName.setBounds(412, 76, 130, 15);
        lblAccountName.setForeground(Color.RED);
        bottomPanel.add(lblAccountName);

        btnSelect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String accountId = (String) accountComboBox.getSelectedItem();
                accountId = accountId.substring(0, accountId.indexOf('|'));
                int selectedAccountId = Integer.valueOf(accountId);

                JPasswordField passwordField = new JPasswordField(10); 
                JPanel panel = new JPanel();
                panel.add(new JLabel("Enter Password for Account " + selectedAccountId + ":"));
                panel.add(passwordField);

                int result = JOptionPane.showConfirmDialog(frame, panel, "Password Input", JOptionPane.OK_CANCEL_OPTION);

                if (result == JOptionPane.OK_OPTION) {
                    char[] enteredPasswordChars = passwordField.getPassword();
                    String enteredPassword = new String(enteredPasswordChars);

                    if (enteredPassword.isEmpty() || !isPasswordCorrect(selectedAccountId, enteredPassword)) {
                        JOptionPane.showMessageDialog(frame, "Incorrect password or canceled. Cannot view expenses.");
                        return;
                    }

                    try {
                        currentAccountId = selectedAccountId;
                        loadData((DefaultTableModel) table.getModel(), currentAccountId);
                        String details[] = getAccountDetails(currentAccountId);
                        lblTotalAmount.setText(details[1]);
                        lblAccountName.setText(details[0]);
                    } catch (NumberFormatException e1) {
                        e1.printStackTrace();
                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });     

        JLabel startDateLabel = new JLabel("Start Date:");
        startDateLabel.setBounds(350, 5, 70, 15);
        startDateLabel.setForeground(Color.CYAN);
        topPanel.add(startDateLabel);
    
        JTextField startDateField = new JTextField();
        startDateField.setColumns(10);
        startDateField.setBounds(420, 0, 100, 28);
        startDateField.setBackground(Color.BLACK);
        startDateField.setForeground(Color.RED);
        topPanel.add(startDateField);
    
        JLabel endDateLabel = new JLabel("End Date:");
        endDateLabel.setBounds(350, 34, 70, 15);
        endDateLabel.setForeground(Color.CYAN);
        topPanel.add(endDateLabel);
    
        JTextField endDateField = new JTextField();
        endDateField.setColumns(10);
        endDateField.setBounds(420, 29, 100, 28);
        endDateField.setBackground(Color.BLACK);
        endDateField.setForeground(Color.RED);
        topPanel.add(endDateField);

        btnFindExpense = new JButton("Find");
        btnFindExpense.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String searchStartDate = startDateField.getText();
                String searchEndDate = endDateField.getText();
                
                if (searchStartDate.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Please enter a start date.");
                    return;
                }
                
                String sql;
                if (searchEndDate.isEmpty()) {
                    sql = "SELECT date, category, amount FROM expenditure WHERE account_id = ? AND STR_TO_DATE(date, '%d.%m.%Y') = ?";
                } else {
                    sql = "SELECT date, category, amount FROM expenditure WHERE account_id = ? " +
                          "AND STR_TO_DATE(date, '%d.%m.%Y') BETWEEN ? AND ?";
                }
                
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        findExpensesByDate(currentAccountId, searchStartDate, searchEndDate, sql);
                        return null;
                    }
    
                    @Override
                    protected void done() {}
                };
                worker.execute();
            }
        });
        btnFindExpense.setBounds(523, 16, 59, 24);
        topPanel.add(btnFindExpense);
       
        JButton btnEditExpense = new JButton("Edit");
        btnEditExpense.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow >= 0) {
                    String date = (String) table.getValueAt(selectedRow, 0);
                    String category = (String) table.getValueAt(selectedRow, 1);
                    String amount = table.getValueAt(selectedRow, 2).toString();
        
                    String newDate = JOptionPane.showInputDialog(frame, "Edit Date:", date);
                    String newCategory = JOptionPane.showInputDialog(frame, "Edit Category:", category);
                    String newAmount = JOptionPane.showInputDialog(frame, "Edit Amount:", amount);
        
                    if (newDate != null && newCategory != null && newAmount != null) {
                        updateExpense(currentAccountId, date, category, Double.parseDouble(amount),
                                newDate, newCategory, Double.parseDouble(newAmount));
        
                        try {
                            loadData((DefaultTableModel) table.getModel(), currentAccountId);
                        } catch (SQLException e1) {
                            e1.printStackTrace();
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(frame, "Select an expense to edit.");
                }
            }
        });
        btnEditExpense.setBounds(160, 39, 100, 25);
        bottomPanel.add(btnEditExpense);

        JButton btnDeleteExpense = new JButton("Delete");
        btnDeleteExpense.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow >= 0) {
                    String date = (String) table.getValueAt(selectedRow, 0);
                    String category = (String) table.getValueAt(selectedRow, 1);
                    double amount = Double.parseDouble(table.getValueAt(selectedRow, 2).toString());
        
                    int confirm = JOptionPane.showConfirmDialog(frame, "Delete this expense?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        deleteExpense(currentAccountId, date, category, amount);
        
                        try {
                            loadData((DefaultTableModel) table.getModel(), currentAccountId);
                        } catch (SQLException e1) {
                            e1.printStackTrace();
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(frame, "Select an expense to delete.");
                }
            }
        });
        btnDeleteExpense.setBounds(310, 39, 100, 25);
        bottomPanel.add(btnDeleteExpense);

        JButton btnShowPieChart = new JButton("Expense Chart");
        btnShowPieChart.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                generatePieChart((DefaultTableModel) table.getModel());
            }
        });
        btnShowPieChart.setBounds(450, 39, 120, 25);
        bottomPanel.add(btnShowPieChart);
 
        try {
            updateComboBox(accountComboBox);
        } catch (SQLException e1) {
            e1.printStackTrace();
        }
    }
}
