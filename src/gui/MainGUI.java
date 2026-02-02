package gui;

import dbg.JDISimpleDebuggee;
// import dbg.TestDebuggee;  // Alternative pour tester l'Inspector avec des objets

import javax.swing.*;

// Point d'entree pour le debugger graphique
public class  MainGUI {

    public static void main(String[] args) {
        // Utilise le look and feel du systeme
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Pas grave, on utilise le defaut
        }

        // Lance l'interface sur le thread Swing
        SwingUtilities.invokeLater(() -> {
            DebuggerGUI gui = new DebuggerGUI();
            gui.setVisible(true);

            // Chemin vers les sources
            String userDir = System.getProperty("user.dir");
            String sourcePath = userDir + java.io.File.separator + "src";

            System.out.println("Working dir: " + userDir);
            System.out.println("Source path: " + sourcePath);

            // Demarre le debugger sur la classe de test
            gui.startDebugging(JDISimpleDebuggee.class, sourcePath);
        });
    }
}
