package gui;

import javax.swing.*;
import java.awt.*;

// Panneau avec les boutons de commande (step, continue, etc.)
public class CommandPanel extends JPanel {

    private JButton stepInBtn;
    private JButton stepOverBtn;
    private JButton continueBtn;
    private JButton stopBtn;
    private DebuggerGUI debuggerGUI;

    public CommandPanel(DebuggerGUI gui) {
        this.debuggerGUI = gui;
        setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));
        setBorder(BorderFactory.createTitledBorder("COMMANDS"));

        // Bouton Step Into (entre dans les methodes)
        stepInBtn = new JButton("Step In");
        stepInBtn.setToolTipText("Execute la prochaine instruction (entre dans les methodes)");
        stepInBtn.addActionListener(e -> debuggerGUI.executeCommand("step"));

        // Bouton Step Over (saute les methodes)
        stepOverBtn = new JButton("Step Over");
        stepOverBtn.setToolTipText("Execute la ligne courante (sans entrer dans les methodes)");
        stepOverBtn.addActionListener(e -> debuggerGUI.executeCommand("step-over"));

        // Bouton Continue (jusqu'au prochain breakpoint)
        continueBtn = new JButton("Continue");
        continueBtn.setToolTipText("Continue jusqu'au prochain breakpoint");
        continueBtn.addActionListener(e -> debuggerGUI.executeCommand("continue"));

        // Bouton Stop
        stopBtn = new JButton("Stop");
        stopBtn.setToolTipText("Arrete le programme");
        stopBtn.setBackground(new Color(255, 100, 100));
        stopBtn.addActionListener(e -> debuggerGUI.stopDebugger());

        add(stepInBtn);
        add(stepOverBtn);
        add(continueBtn);
        add(stopBtn);
    }

    // Active/desactive les boutons
    public void setButtonsEnabled(boolean enabled) {
        stepInBtn.setEnabled(enabled);
        stepOverBtn.setEnabled(enabled);
        continueBtn.setEnabled(enabled);
        stopBtn.setEnabled(enabled);
    }
}
