import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class VoyagePlanApp {

    // --- DATABASE CONNECTION CONFIGURATION ---

    private static final String DB_URL = "jdbc:mysql://localhost:3306/travel_db";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "asbin123";

    public static Connection getConnection() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> new LoginPage().setVisible(true));
    }

    // --- PAGE 1: LOGIN & REGISTRATION ---
    static class LoginPage extends JFrame {
        JTextField txtUser = new JTextField(15);
        JPasswordField txtPass = new JPasswordField(15);

        public LoginPage() {
            setTitle("VoyagePlan Login");
            setSize(350, 250);
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setLocationRelativeTo(null);
            setLayout(new GridBagLayout());

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);

            add(new JLabel("Username:"), gbc);
            gbc.gridx = 1; add(txtUser, gbc);

            gbc.gridx = 0; gbc.gridy = 1; add(new JLabel("Password:"), gbc);
            gbc.gridx = 1; add(txtPass, gbc);

            JPanel btnPnl = new JPanel();
            JButton btnLogin = new JButton("Login");
            JButton btnReg = new JButton("Register");
            btnPnl.add(btnLogin); btnPnl.add(btnReg);

            gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
            add(btnPnl, gbc);

            // Button Logic
            btnLogin.addActionListener(e -> {
                try (Connection conn = getConnection()) {
                    String sql = "SELECT * FROM users WHERE username=? AND password=?";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setString(1, txtUser.getText());
                    ps.setString(2, new String(txtPass.getPassword()));
                    if (ps.executeQuery().next()) {
                        new Dashboard().setVisible(true);
                        this.dispose();
                    } else {
                        JOptionPane.showMessageDialog(this, "Invalid Username/Password");
                    }
                } catch (Exception ex) { JOptionPane.showMessageDialog(this, "DB Error: " + ex.getMessage()); }
            });

            btnReg.addActionListener(e -> {
                try (Connection conn = getConnection()) {
                    String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setString(1, txtUser.getText());
                    ps.setString(2, new String(txtPass.getPassword()));
                    ps.executeUpdate();
                    JOptionPane.showMessageDialog(this, "User Registered Successfully!");
                } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Registration Failed (User might exist)"); }
            });
        }
    }

    // --- PAGE 2: MAIN DASHBOARD (CRUD OPERATIONS) ---
    static class Dashboard extends JFrame {
        DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "City", "Days", "Budget", "Checklist"}, 0);
        JTable table = new JTable(model);
        JTextField tCity = new JTextField(10), tDays = new JTextField(5), tBudget = new JTextField(7), tCheck = new JTextField(15);

        public Dashboard() {
            setTitle("VoyagePlan Dashboard");
            setSize(850, 500);
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setLocationRelativeTo(null);


            JPanel inputPnl = new JPanel();
            inputPnl.add(new JLabel("City:")); inputPnl.add(tCity);
            inputPnl.add(new JLabel("Days:")); inputPnl.add(tDays);
            inputPnl.add(new JLabel("Budget:")); inputPnl.add(tBudget);
            inputPnl.add(new JLabel("Pack:")); inputPnl.add(tCheck);
            JButton btnAdd = new JButton("Add Trip");
            inputPnl.add(btnAdd);


            JPanel actionPnl = new JPanel();
            JButton btnDel = new JButton("Delete Selected");
            JButton btnEdit = new JButton("Edit Budget");
            actionPnl.add(btnEdit); actionPnl.add(btnDel);

            add(inputPnl, BorderLayout.NORTH);
            add(new JScrollPane(table), BorderLayout.CENTER);
            add(actionPnl, BorderLayout.SOUTH);


            btnAdd.addActionListener(e -> {
                try (Connection conn = getConnection()) {
                    String sql = "INSERT INTO itinerary (city, days, budget, checklist) VALUES (?,?,?,?)";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setString(1, tCity.getText());
                    ps.setInt(2, Integer.parseInt(tDays.getText()));
                    ps.setDouble(3, Double.parseDouble(tBudget.getText()));
                    ps.setString(4, tCheck.getText());
                    ps.executeUpdate();
                    loadData();
                } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Error Saving Data"); }
            });

            btnDel.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row != -1) {
                    try (Connection conn = getConnection()) {
                        PreparedStatement ps = conn.prepareStatement("DELETE FROM itinerary WHERE id=?");
                        ps.setInt(1, (int) model.getValueAt(row, 0));
                        ps.executeUpdate();
                        loadData();
                    } catch (Exception ex) { ex.printStackTrace(); }
                }
            });

            btnEdit.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row != -1) {
                    String res = JOptionPane.showInputDialog("Enter New Budget:");
                    if (res != null) {
                        try (Connection conn = getConnection()) {
                            PreparedStatement ps = conn.prepareStatement("UPDATE itinerary SET budget=? WHERE id=?");
                            ps.setDouble(1, Double.parseDouble(res));
                            ps.setInt(2, (int) model.getValueAt(row, 0));
                            ps.executeUpdate();
                            loadData();
                        } catch (Exception ex) { ex.printStackTrace(); }
                    }
                }
            });

            loadData();
        }

        private void loadData() {
            model.setRowCount(0);
            try (Connection conn = getConnection()) {
                ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM itinerary");
                while (rs.next()) {
                    model.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getDouble(4), rs.getString(5)});
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        }
    }
}
