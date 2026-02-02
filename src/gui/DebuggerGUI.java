package gui;

import com.sun.jdi.StackFrame;
import com.sun.jdi.AbsentInformationException;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;

// Fenetre principale du debugger graphique
public class DebuggerGUI extends JFrame implements DebuggerListener {

    private SourceCodePanel sourcePanel;
    private CallStackPanel callStackPanel;
    private InspectorPanel inspectorPanel;
    private OutputPanel outputPanel;
    private CommandPanel commandPanel;

    private GUIScriptableDebugger debugger;
    private StackFrame currentFrame;
    private String sourceBasePath;

    public DebuggerGUI() {
        super("Debugger Graphique - JDI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        initComponents();
        layoutComponents();
    }

    private void initComponents() {
        sourcePanel = new SourceCodePanel(this);
        callStackPanel = new CallStackPanel(this);
        inspectorPanel = new InspectorPanel();
        outputPanel = new OutputPanel();
        commandPanel = new CommandPanel(this);
    }

    private void layoutComponents() {
        // Layout principal
        setLayout(new BorderLayout(5, 5));

        // Panneau des commandes en haut
        add(commandPanel, BorderLayout.NORTH);

        // Panneau central avec code source
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(sourcePanel, BorderLayout.CENTER);

        // Panneau droit avec call stack et inspector
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(callStackPanel, BorderLayout.NORTH);
        rightPanel.add(inspectorPanel, BorderLayout.CENTER);
        rightPanel.setPreferredSize(new Dimension(300, 400));

        // Split entre centre et droite
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, centerPanel, rightPanel);
        mainSplit.setResizeWeight(0.7);

        add(mainSplit, BorderLayout.CENTER);

        // Output en bas
        add(outputPanel, BorderLayout.SOUTH);
    }

    // Demarre le debugger sur une classe
    public void startDebugging(Class<?> targetClass, String sourcePath) {
        this.sourceBasePath = sourcePath;

        outputPanel.appendOutput("Starting debugger on: " + targetClass.getName() + "\n");

        // Cree et demarre le debugger dans un thread separe
        debugger = new GUIScriptableDebugger(this);

        Thread debugThread = new Thread(() -> {
            debugger.attachTo(targetClass);
        });
        debugThread.start();
    }

    // Appele quand le debugger s'arrete
    @Override
    public void onDebuggerStopped(StackFrame frame, List<StackFrame> callStack) {
        this.currentFrame = frame;

        SwingUtilities.invokeLater(() -> {
            // Met a jour la pile d'appels
            callStackPanel.updateStack(callStack);

            // Met a jour le code source
            updateSourceCode(frame);

            // Met a jour l'inspector
            inspectorPanel.updateVariables(frame);

            // Active les boutons
            commandPanel.setButtonsEnabled(true);

            outputPanel.appendOutput(">>> Stopped at: " +
                    frame.location().declaringType().name() + "." +
                    frame.location().method().name() + ":" +
                    frame.location().lineNumber() + "\n");
        });
    }

    // Met a jour l'affichage du code source
    private void updateSourceCode(StackFrame frame) {
        try {
            String sourceName = frame.location().sourceName();
            String className = frame.location().declaringType().name();

            // Cherche le fichier source
            String sourcePath = findSourceFile(className, sourceName);
            if (sourcePath != null) {
                sourcePanel.loadSourceFile(sourcePath);
            }

            // Surligne la ligne courante
            sourcePanel.highlightLine(frame.location().lineNumber());

        } catch (AbsentInformationException e) {
            outputPanel.appendOutput("Warning: No source info available\n");
        }
    }

    // Trouve le fichier source a partir du nom de classe
    private String findSourceFile(String className, String sourceName) {
        String sep = File.separator;

        // Essaye le chemin de base + package (ex: src/dbg/JDISimpleDebuggee.java)
        String packagePath = className.replace('.', sep.charAt(0));
        int lastSep = packagePath.lastIndexOf(sep.charAt(0));
        if (lastSep > 0) {
            packagePath = packagePath.substring(0, lastSep + 1);
        } else {
            packagePath = "";
        }

        String fullPath = sourceBasePath + sep + packagePath + sourceName;
        System.out.println("Trying: " + fullPath);
        File file = new File(fullPath);
        if (file.exists()) {
            return fullPath;
        }

        // Essaye directement dans le dossier source
        fullPath = sourceBasePath + sep + sourceName;
        System.out.println("Trying: " + fullPath);
        file = new File(fullPath);
        if (file.exists()) {
            return fullPath;
        }

        // Essaye dans un sous-dossier dbg
        fullPath = sourceBasePath + sep + "dbg" + sep + sourceName;
        System.out.println("Trying: " + fullPath);
        file = new File(fullPath);
        if (file.exists()) {
            return fullPath;
        }

        outputPanel.appendOutput("Source not found for: " + className + "\n");
        return null;
    }

    // Appele quand le programme affiche quelque chose
    @Override
    public void onOutput(String text) {
        SwingUtilities.invokeLater(() -> {
            outputPanel.appendOutput(text);
        });
    }

    // Appele quand le programme se termine
    @Override
    public void onProgramEnded() {
        SwingUtilities.invokeLater(() -> {
            outputPanel.appendOutput("\n=== Program ended ===\n");
            commandPanel.setButtonsEnabled(false);
            callStackPanel.clear();
            inspectorPanel.clear();
        });
    }

    // Appele quand on clique sur une frame dans la pile
    public void onFrameSelected(StackFrame frame) {
        this.currentFrame = frame;
        updateSourceCode(frame);
        inspectorPanel.updateVariables(frame);
    }

    // Execute une commande (step, continue, etc.)
    public void executeCommand(String command) {
        commandPanel.setButtonsEnabled(false);
        outputPanel.appendOutput(">>> " + command + "\n");

        Thread cmdThread = new Thread(() -> {
            debugger.executeCommand(command);
        });
        cmdThread.start();
    }

    // Ajoute un breakpoint
    public void addBreakpoint(String sourcePath, int line) {
        if (debugger != null) {
            // Extrait le nom de classe du chemin complet
            // Ex: C:\...\src\dbg\JDISimpleDebuggee.java -> dbg.JDISimpleDebuggee
            String className = extractClassName(sourcePath);
            outputPanel.appendOutput("Adding breakpoint: " + className + ":" + line + "\n");
            debugger.addBreakpoint(className, line);
        }
    }

    // Extrait le nom de classe depuis un chemin de fichier
    // Ex: C:\Users\...\src\dbg\MaClasse.java -> dbg.MaClasse
    private String extractClassName(String filePath) {
        // Cherche "src" dans le chemin
        String normalizedPath = filePath.replace("\\", "/");
        int srcIndex = normalizedPath.indexOf("/src/");

        String relativePath;
        if (srcIndex >= 0) {
            // Prend tout apres "/src/"
            relativePath = normalizedPath.substring(srcIndex + 5);
        } else {
            // Sinon prend juste le nom du fichier
            int lastSlash = normalizedPath.lastIndexOf("/");
            relativePath = (lastSlash >= 0) ? normalizedPath.substring(lastSlash + 1) : normalizedPath;
        }

        // Enleve .java
        if (relativePath.endsWith(".java")) {
            relativePath = relativePath.substring(0, relativePath.length() - 5);
        }

        // Remplace / par . pour avoir le nom de classe complet
        return relativePath.replace("/", ".");
    }

    // Arrete le debugger
    public void stopDebugger() {
        if (debugger != null) {
            debugger.stop();
        }
        outputPanel.appendOutput("Debugger stopped.\n");
        commandPanel.setButtonsEnabled(false);
    }
}
