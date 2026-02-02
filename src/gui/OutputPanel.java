package gui;

import javax.swing.*;
import java.awt.*;

// Panneau qui affiche la sortie console du programme debugge
public class OutputPanel extends JPanel {

    private JTextArea outputArea;

    public OutputPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("OUTPUT"));

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        outputArea.setBackground(new Color(30, 30, 30));
        outputArea.setForeground(new Color(200, 200, 200));

        add(new JScrollPane(outputArea), BorderLayout.CENTER);
        setPreferredSize(new Dimension(400, 150));
    }

    // Ajoute du texte a la console
    public void appendOutput(String text) {
        outputArea.append(text);
        // Scroll vers le bas
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }

    // Efface la console
    public void clear() {
        outputArea.setText("");
    }
}
