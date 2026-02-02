package gui;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.HashSet;
import java.util.Set;

// Panneau qui affiche le code source avec numeros de ligne
public class SourceCodePanel extends JPanel {

    private JTextPane codeArea;
    private JTextArea lineNumbers;
    private int currentLine = -1;
    private Set<Integer> breakpointLines;
    private DebuggerGUI debuggerGUI;
    private String currentSourcePath;

    public SourceCodePanel(DebuggerGUI gui) {
        this.debuggerGUI = gui;
        this.breakpointLines = new HashSet<>();
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("SOURCE CODE"));

        // Zone des numeros de ligne
        lineNumbers = new JTextArea();
        lineNumbers.setEditable(false);
        lineNumbers.setBackground(new Color(240, 240, 240));
        lineNumbers.setFont(new Font("Monospaced", Font.PLAIN, 14));
        lineNumbers.setForeground(Color.GRAY);

        // Zone du code
        codeArea = new JTextPane();
        codeArea.setEditable(false);
        codeArea.setFont(new Font("Monospaced", Font.PLAIN, 14));

        // Clic sur les numeros de ligne = breakpoint
        lineNumbers.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int clickedLine = getLineFromY(e.getY());
                if (clickedLine > 0) {
                    toggleBreakpoint(clickedLine);
                }
            }
        });

        // Layout
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(lineNumbers, BorderLayout.CENTER);

        JScrollPane scrollPane = new JScrollPane(codeArea);
        scrollPane.setRowHeaderView(lineNumbers);

        add(scrollPane, BorderLayout.CENTER);
    }

    // Calcule la ligne a partir de la position Y du clic
    private int getLineFromY(int y) {
        try {
            int lineHeight = lineNumbers.getFontMetrics(lineNumbers.getFont()).getHeight();
            return (y / lineHeight) + 1;
        } catch (Exception e) {
            return -1;
        }
    }

    // Active/desactive un breakpoint sur une ligne
    private void toggleBreakpoint(int line) {
        if (breakpointLines.contains(line)) {
            breakpointLines.remove(line);
        } else {
            breakpointLines.add(line);
            // Dit au debugger de poser le breakpoint
            if (debuggerGUI != null && currentSourcePath != null) {
                debuggerGUI.addBreakpoint(currentSourcePath, line);
            }
        }
        updateLineNumbers();
    }

    // Charge et affiche un fichier source
    public void loadSourceFile(String filePath) {
        this.currentSourcePath = filePath;
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                codeArea.setText("// Source file not found: " + filePath);
                return;
            }

            StringBuilder code = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                code.append(line).append("\n");
            }
            reader.close();

            codeArea.setText(code.toString());
            updateLineNumbers();

        } catch (IOException e) {
            codeArea.setText("// Error reading file: " + e.getMessage());
        }
    }

    // Met a jour les numeros de ligne (avec marqueurs de breakpoint)
    private void updateLineNumbers() {
        String text = codeArea.getText();
        int lines = text.split("\n", -1).length;

        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= lines; i++) {
            String marker = breakpointLines.contains(i) ? "*" : " ";
            sb.append(String.format("%s%3d ", marker, i));
            sb.append("\n");
        }
        lineNumbers.setText(sb.toString());
    }

    // Surligne la ligne courante d'execution
    public void highlightLine(int line) {
        this.currentLine = line;

        // Reset le style
        StyledDocument doc = codeArea.getStyledDocument();
        Style defaultStyle = codeArea.addStyle("default", null);
        StyleConstants.setBackground(defaultStyle, Color.WHITE);
        doc.setCharacterAttributes(0, doc.getLength(), defaultStyle, true);

        if (line <= 0) return;

        // Trouve la position de la ligne
        String text = codeArea.getText();
        String[] lines = text.split("\n", -1);

        if (line > lines.length) return;

        int startPos = 0;
        for (int i = 0; i < line - 1; i++) {
            startPos += lines[i].length() + 1;
        }
        int endPos = startPos + lines[line - 1].length();

        // Applique le surlignage jaune
        Style highlightStyle = codeArea.addStyle("highlight", null);
        StyleConstants.setBackground(highlightStyle, new Color(255, 255, 150));
        doc.setCharacterAttributes(startPos, endPos - startPos, highlightStyle, true);

        // Scroll vers la ligne
        try {
            codeArea.setCaretPosition(startPos);
        } catch (Exception e) {
            // ignore
        }
    }

    public Set<Integer> getBreakpointLines() {
        return breakpointLines;
    }

    public String getCurrentSourcePath() {
        return currentSourcePath;
    }
}
