package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class CurrencyConverterUI extends JFrame {
    private final JTextField amountField;
    private final JComboBox<String> fromCurrency;
    private final JComboBox<String> toCurrency;
    private final JLabel resultLabel;

    public CurrencyConverterUI() {
        setTitle("Currency Converter");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);

        // Use GridBagLayout for clean alignment
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel amountLabel = new JLabel("Amount:");
        amountField = new JTextField();

        JLabel fromLabel = new JLabel("From:");
        fromCurrency = new JComboBox<>(new String[]{"USD", "EUR", "INR", "JPY"});

        JLabel toLabel = new JLabel("To:");
        toCurrency = new JComboBox<>(new String[]{"USD", "EUR", "INR", "JPY"});

        JLabel resultText = new JLabel("Result:");
        resultLabel = new JLabel("[ 0.00 ]");
        resultLabel.setForeground(Color.BLUE);

        JButton clearBtn = new JButton("Clear");
        JButton convertBtn = new JButton("Convert");

        // Add components row by row
        gbc.gridx = 0; gbc.gridy = 0;
        add(amountLabel, gbc);
        gbc.gridx = 1;
        add(amountField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        add(fromLabel, gbc);
        gbc.gridx = 1;
        add(fromCurrency, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        add(toLabel, gbc);
        gbc.gridx = 1;
        add(toCurrency, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        add(resultText, gbc);
        gbc.gridx = 1;
        add(resultLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        add(clearBtn, gbc);
        gbc.gridx = 1;
        add(convertBtn, gbc);

        // Button actions
        convertBtn.addActionListener(e -> convertCurrency());
        clearBtn.addActionListener(e -> {
            amountField.setText("");
            resultLabel.setText("[ 0.00 ]");
        });
    }

    private void convertCurrency() {
        try {
            double amount = Double.parseDouble(amountField.getText());
            String from = (String) fromCurrency.getSelectedItem();
            String to = (String) toCurrency.getSelectedItem();

            // Dummy conversion rates (for demo)
            double rate = 0;
            if (from != null) {
                rate = getRate(from, to);
            }
            double result = amount * rate;

            resultLabel.setText(String.format("[ %.2f %s ]", result, to));
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number.");
        }
    }

    private double getRate(String from, String to) {
        // You can later integrate a real API here
        if (from.equals(to)) return 1.0;
        if (from.equals("USD") && to.equals("INR")) return 83.0;
        if (from.equals("INR") && to.equals("USD")) return 0.012;
        if (from.equals("EUR") && to.equals("INR")) return 89.0;
        if (from.equals("INR") && to.equals("EUR")) return 0.011;
        return 1.0; // fallback
    }
}
