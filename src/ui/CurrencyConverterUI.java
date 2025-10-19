package ui;

import java.awt.*;
import javax.swing.*;
import java.net.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import io.github.cdimascio.dotenv.Dotenv;

public class CurrencyConverterUI {
    private String API_KEY;
    private String DB_FILE;

    private JFrame frame;
    private JComboBox<String> fromBox;
    private JComboBox<String> toBox;
    private JTextField amountField;
    private JLabel resultLabel;
    private JButton convertBtn, refreshBtn, closeBtn, historyBtn;

    private final Map<String, Map<String, Double>> ratesCache = new HashMap<>();

    private Connection connection;

    public void showUI() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}

        Dotenv dotenv = Dotenv.load();
        API_KEY = dotenv.get("EXCHANGE_RATE_API_KEY");
        DB_FILE = dotenv.get("DB_FILE", "conversions.db");

        connectToDatabase();
        createHistoryTable();

        frame = new JFrame("Currency Converter (ExchangeRate-API)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(845, 400); // bigger frame
        frame.setLayout(null);

        // Common font for all labels, buttons, dropdowns, textfields
        Font commonFont = new Font("Roboto Mono", Font.BOLD, 18);

        JLabel amtLabel = new JLabel("Amount:");
        amtLabel.setBounds(30, 30, 100, 30);
        amtLabel.setFont(commonFont);
        frame.add(amtLabel);

        amountField = new JTextField("1");
        amountField.setBounds(140, 30, 150, 35);
        amountField.setFont(commonFont);
        frame.add(amountField);

        JLabel fromLabel = new JLabel("From:");
        fromLabel.setBounds(30, 80, 100, 30);
        fromLabel.setFont(commonFont);
        frame.add(fromLabel);

        fromBox = new JComboBox<>();
        fromBox.setBounds(140, 80, 150, 35);
        fromBox.setFont(commonFont);
        frame.add(fromBox);

        JLabel toLabel = new JLabel("To:");
        toLabel.setBounds(320, 80, 50, 30);
        toLabel.setFont(commonFont);
        frame.add(toLabel);

        toBox = new JComboBox<>();
        toBox.setBounds(380, 80, 150, 35);
        toBox.setFont(commonFont);
        frame.add(toBox);

        convertBtn = new JButton("Convert");
        convertBtn.setBounds(140, 140, 140, 40);
        convertBtn.setFont(commonFont);
        frame.add(convertBtn);

        refreshBtn = new JButton("Refresh");
        refreshBtn.setBounds(300, 140, 140, 40);
        refreshBtn.setFont(commonFont);
        frame.add(refreshBtn);

        historyBtn = new JButton("Show History");
        historyBtn.setBounds(460, 140, 180, 40);
        historyBtn.setFont(commonFont);
        frame.add(historyBtn);

        resultLabel = new JLabel("Result: ");
        resultLabel.setBounds(30, 200, 640, 40);
        resultLabel.setFont(commonFont);
        frame.add(resultLabel);

        closeBtn = new JButton("Close");
        closeBtn.setBounds(580, 30, 100, 35);
        closeBtn.setFont(commonFont);
        frame.add(closeBtn);

        convertBtn.addActionListener(e -> onConvert());
        refreshBtn.addActionListener(e -> loadCurrenciesWithFeedback());
        historyBtn.addActionListener(e -> showHistory());
        closeBtn.addActionListener(e -> {
            System.out.println("[INFO] Closing application...");
            closeDatabase();
            frame.dispose();
        });

        loadCurrenciesWithFeedback();

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void connectToDatabase() {
        try {
            System.out.println("[INFO] Connecting to SQLite database...");
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);
            System.out.println("[INFO] Connection established: " + DB_FILE);
        } catch (SQLException e) {
            System.err.println("[ERROR] Database connection failed: " + e.getMessage());
        }
    }

    private void createHistoryTable() {
        String sql = "CREATE TABLE IF NOT EXISTS conversion_history (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "amount REAL, from_currency TEXT, to_currency TEXT," +
                "result REAL, timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            System.out.println("[INFO] conversion_history table verified/created.");
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to create table: " + e.getMessage());
        }
    }

    private void saveConversion(double amount, String from, String to, double result) {
        String sql = "INSERT INTO conversion_history (amount, from_currency, to_currency, result) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setDouble(1, amount);
            pstmt.setString(2, from);
            pstmt.setString(3, to);
            pstmt.setDouble(4, result);
            pstmt.executeUpdate();
            System.out.println("[INFO] Conversion saved: " + amount + " " + from + " -> " + result + " " + to);
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to save conversion: " + e.getMessage());
        }
    }

    private void showHistory() {
        System.out.println("[INFO] Fetching conversion history...");

        // Main panel for history
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(10, 10));

        // HTML label with heading and rows
        StringBuilder sb = new StringBuilder("<html><body style='font-family: \"Roboto Mono\";'>");
        sb.append("<h1 style='font-size:32pt; font-weight:bold; margin-bottom:20px;'>Conversion History</h1>");
        sb.append("<div style='font-size:20pt;'>");
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM conversion_history ORDER BY id DESC LIMIT 10")) {
            while (rs.next()) {
                sb.append(rs.getString("timestamp"))
                        .append(": ")
                        .append(rs.getDouble("amount")).append(" ")
                        .append(rs.getString("from_currency")).append(" → ")
                        .append(rs.getDouble("result")).append(" ")
                        .append(rs.getString("to_currency"))
                        .append("<br>");
            }
        } catch (SQLException e) {
            sb.append("<p>Error fetching history.</p>");
        }
        sb.append("</div></body></html>");

        JLabel label = new JLabel(sb.toString());
        JScrollPane scrollPane = new JScrollPane(label);
        scrollPane.setPreferredSize(new Dimension(600, 150));
        panel.add(scrollPane, BorderLayout.CENTER);

        // Clear History button
        JButton clearBtn = new JButton("Clear History");
        clearBtn.setFont(new Font("Roboto Mono", Font.BOLD, 16));
        clearBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(frame, "Are you sure you want to clear all history?",
                    "Confirm Clear", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try (Statement stmt = connection.createStatement()) {
                    stmt.executeUpdate("DELETE FROM conversion_history");
                    System.out.println("[INFO] Conversion history cleared.");
                    label.setText("<html><body style='font-family: \"Roboto Mono\";'>"
                            + "<h1 style='font-size:32pt; font-weight:bold; margin-bottom:20px;'>Conversion History</h1>"
                            + "<div style='font-size:20pt;'>No history available.</div></body></html>");
                } catch (SQLException ex) {
                    System.err.println("[ERROR] Failed to clear history: " + ex.getMessage());
                }
            }
        });
        panel.add(clearBtn, BorderLayout.SOUTH);

        JOptionPane.showMessageDialog(frame, panel, "Conversion History", JOptionPane.INFORMATION_MESSAGE);
    }

    private void closeDatabase() {
        try {
            if (connection != null) {
                connection.close();
                System.out.println("[INFO] Database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to close database: " + e.getMessage());
        }
    }

    // === API + Conversion logic ===
    private void onConvert() {
        String from = Objects.requireNonNull(fromBox.getSelectedItem()).toString();
        String to = Objects.requireNonNull(toBox.getSelectedItem()).toString();

        double amount;
        try {
            amount = Double.parseDouble(amountField.getText());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, "Invalid amount.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            double rate = fetchRate(from, to);
            double converted = rate * amount;
            resultLabel.setText("Result: " + String.format("%.4f", converted) + " " + to);
            saveConversion(amount, from, to, converted);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Error: " + e.getMessage(), "Conversion Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("[ERROR] Conversion failed: " + e.getMessage());
        }
    }

    private void loadCurrenciesWithFeedback() {
        SwingUtilities.invokeLater(() -> {
            try {
                resultLabel.setText("Loading currencies...");
                Set<String> currencies = fetchCurrencies();
                fromBox.removeAllItems();
                toBox.removeAllItems();
                for (String c : currencies) {
                    fromBox.addItem(c);
                    toBox.addItem(c);
                }
                fromBox.setSelectedItem("USD");
                toBox.setSelectedItem("INR");
                resultLabel.setText("Currencies loaded successfully.");
                System.out.println("[INFO] Currencies loaded: " + currencies.size());
            } catch (Exception e) {
                resultLabel.setText("Failed to load currencies.");
                JOptionPane.showMessageDialog(frame, "Failed to load currencies: " + e.getMessage());
                System.err.println("[ERROR] Loading currencies failed: " + e.getMessage());
            }
        });
    }

    private Set<String> fetchCurrencies() throws IOException {
        URL url = new URL("https://v6.exchangerate-api.com/v6/" + API_KEY + "/latest/USD");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        System.out.println("[INFO] Fetching currency list...");
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();

        String json = sb.toString();
        if (!json.contains("conversion_rates"))
            throw new IOException("API response invalid or limit exceeded.");

        Set<String> currencies = new TreeSet<>();
        int start = json.indexOf("conversion_rates") + 18;
        int end = json.indexOf("}", start);
        String[] pairs = json.substring(start, end).split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":");
            currencies.add(kv[0].replaceAll("\"", "").trim());
        }
        return currencies;
    }

    private double fetchRate(String from, String to) throws IOException {
        if (ratesCache.containsKey(from) && ratesCache.get(from).containsKey(to)) {
            return ratesCache.get(from).get(to);
        }

        URL url = new URL("https://v6.exchangerate-api.com/v6/" + API_KEY + "/latest/" + from);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        System.out.println("[INFO] Requesting rates for " + from + "...");
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();

        String json = sb.toString();
        if (!json.contains("conversion_rates"))
            throw new IOException("Invalid response or API limit exceeded.");

        Map<String, Double> map = new HashMap<>();
        int start = json.indexOf("conversion_rates") + 18;
        int end = json.indexOf("}", start);
        String[] pairs = json.substring(start, end).split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":");
            String currency = kv[0].replaceAll("\"", "").trim();
            double rate = Double.parseDouble(kv[1]);
            map.put(currency, rate);
        }

        ratesCache.put(from, map);
        return map.getOrDefault(to, 0.0);
    }
}
