package org.example;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.util.BitSet;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.io.File;


public class DESGui extends JFrame {

    private JTextArea inputArea;
    private JTextArea outputArea;
    private JTextField keyField;

    public DESGui() {
        setTitle("DES Encryptor GUI");
        setSize(700, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(1, 3, 10, 10));

        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        inputArea = new JTextArea();
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        leftPanel.add(new JLabel("Wiadomość jawna:"), BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(inputArea), BorderLayout.CENTER);

        JPanel inputBtns = new JPanel(new GridLayout(2, 1));
        JButton savePlain = new JButton("Zapisz wiadomość");
        JButton loadPlain = new JButton("Wczytaj wiadomość");

        savePlain.addActionListener(e -> saveToFile(inputArea));
        loadPlain.addActionListener(e -> loadFromFile(inputArea));

        inputBtns.add(savePlain);
        inputBtns.add(loadPlain);
        leftPanel.add(inputBtns, BorderLayout.SOUTH);

        JPanel centerPanel = new JPanel(new GridLayout(6, 1, 5, 5));

        keyField = new JTextField();
        JButton loadKeyBtn = new JButton("Wczytaj klucz");
        JButton saveKeyBtn = new JButton("Zapisz klucz");
        JButton encryptBtn = new JButton(">>> Szyfruj >>>");
        JButton decryptBtn = new JButton("<<< Deszyfruj <<<");

        centerPanel.add(new JLabel("Klucz (HEX, 16 znaków):"));
        centerPanel.add(keyField);
        centerPanel.add(loadKeyBtn);
        centerPanel.add(saveKeyBtn);
        centerPanel.add(encryptBtn);
        centerPanel.add(decryptBtn);

        loadKeyBtn.addActionListener(e -> loadKeyFromFile());
        saveKeyBtn.addActionListener(e -> saveKeyToFile(keyField.getText()));

        encryptBtn.addActionListener(e -> encryptMessage());
        decryptBtn.addActionListener(e -> decryptMessage());

        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        outputArea = new JTextArea();
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        rightPanel.add(new JLabel("Zaszyfrowana wiadomość (HEX):"), BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(outputArea), BorderLayout.CENTER);

        JPanel outputBtns = new JPanel(new GridLayout(2, 1));
        JButton saveEncrypted = new JButton("Zapisz szyfrogram");
        JButton loadEncrypted = new JButton("Wczytaj szyfrogram");

        saveEncrypted.addActionListener(e -> saveToFile(outputArea));
        loadEncrypted.addActionListener(e -> loadFromFile(outputArea));

        outputBtns.add(saveEncrypted);
        outputBtns.add(loadEncrypted);
        rightPanel.add(outputBtns, BorderLayout.SOUTH);

        add(leftPanel);
        add(centerPanel);
        add(rightPanel);
    }

    private void encryptMessage() {
        String msg = inputArea.getText().trim();
        String keyHex = keyField.getText().trim().toUpperCase();

        if (msg.isEmpty() || keyHex.isEmpty()) {
            showError("Wprowadź wiadomość i klucz.");
            return;
        }

        if (keyHex.length() != 16 || !keyHex.matches("[0-9A-F]+")) {
            showError("Klucz musi mieć dokładnie 16 znaków HEX.");
            return;
        }

        try {
            BitSet key = DES.hexToBitSet(keyHex);
            BitSet[] roundKeys = DES.generateRoundKeys(key);
            List<BitSet> blocks = DES.splitToBlocks(msg);

            for (int i = 0; i < blocks.size(); i++) {
                blocks.set(i, DES.des(blocks.get(i), roundKeys));
            }

            String encryptedHex = blocks.stream()
                    .map(DES::bitSetToHex)
                    .reduce("", String::concat);

            outputArea.setText(encryptedHex);
        } catch (Exception e) {
            showError("Błąd podczas szyfrowania: " + e.getMessage());
        }
    }

    private void decryptMessage() {
        String hexInput = outputArea.getText().trim();
        String keyHex = keyField.getText().trim().toUpperCase();

        if (hexInput.isEmpty() || keyHex.isEmpty()) {
            showError("Wprowadź zaszyfrowaną wiadomość i klucz.");
            return;
        }

        if (keyHex.length() != 16 || !keyHex.matches("[0-9A-F]+")) {
            showError("Klucz musi mieć dokładnie 16 znaków HEX.");
            return;
        }

        try {
            List<BitSet> encryptedBlocks = new ArrayList<>();
            for (int i = 0; i < hexInput.length(); i += 16) {
                String blockHex = hexInput.substring(i, i + 16);
                encryptedBlocks.add(DES.hexToBitSet(blockHex));
            }

            BitSet key = DES.hexToBitSet(keyHex);
            BitSet[] roundKeys = DES.generateRoundKeys(key);
            List<BitSet> decryptedBlocks = DES.decryptBlocks(encryptedBlocks, roundKeys);

            String decryptedMessage = DES.joinBlocksToString(decryptedBlocks);

            inputArea.setText(removePadding(decryptedMessage));

        } catch (Exception e) {
            showError("Błąd podczas deszyfrowania: " + e.getMessage());
        }
    }

    private String removePadding(String message) {
        return message.replaceAll("[\\s\\x00]+$", "");
    }

    private void saveToFile(JTextArea area) {
        JFileChooser fc = new JFileChooser();
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = fc.getSelectedFile();
                String fileName = file.getName().toLowerCase();
                String content = area.getText();

                if (fileName.endsWith(".txt")) {
                    Files.writeString(file.toPath(), content);
                } else {
                    byte[] bytes = hexStringToBytes(content);
                    Files.write(file.toPath(), bytes);
                }
            } catch (IOException e) {
                showError("Błąd zapisu pliku.");
            }
        }
    }
    private void loadFromFile(JTextArea area) {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = fc.getSelectedFile();
                String fileName = file.getName().toLowerCase();

                if (fileName.endsWith(".txt")) {
                    String content = Files.readString(file.toPath());
                    area.setText(content);
                } else {
                    byte[] bytes = Files.readAllBytes(file.toPath());
                    String hex = DES.bytesToHex(bytes);
                    area.setText(hex);
                }
            } catch (IOException e) {
                showError("Błąd odczytu pliku.");
            }
        }
    }
    private void saveKeyToFile(String text) {
        JFileChooser fc = new JFileChooser();
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                Files.writeString(fc.getSelectedFile().toPath(), text);
            } catch (IOException e) {
                showError("Błąd zapisu pliku.");
            }
        }
    }
    private void loadKeyFromFile() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                String key = Files.readString(fc.getSelectedFile().toPath()).trim();
                keyField.setText(key);
            } catch (IOException e) {
                showError("Błąd odczytu pliku z kluczem.");
            }
        }
    }

    public static byte[] hexStringToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Błąd", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DESGui().setVisible(true));
    }
}



