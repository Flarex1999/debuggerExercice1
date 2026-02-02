# README 2 - Comment ca marche techniquement

Ce document explique comment l'interface graphique est construite et comment elle communique avec le moteur JDI.

---

## PARTIE 1 : Comment Swing cree l'interface

### 1.1 La fenetre principale (DebuggerGUI.java)

```java
public class DebuggerGUI extends JFrame {  // JFrame = une fenetre

    public DebuggerGUI() {
        super("Debugger Graphique - JDI");  // Titre de la fenetre
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  // Fermer = quitter
        setSize(1200, 800);  // Taille en pixels
        setLocationRelativeTo(null);  // Centre sur l'ecran

        initComponents();  // Cree les panneaux
        layoutComponents();  // Les dispose dans la fenetre
    }
}
```

### 1.2 Les 5 panneaux

La fenetre contient 5 panneaux. Chaque panneau est une classe separee :

```java
private void initComponents() {
    sourcePanel = new SourceCodePanel(this);    // Affiche le code
    callStackPanel = new CallStackPanel(this);  // Affiche la pile d'appels
    inspectorPanel = new InspectorPanel();      // Affiche les variables
    outputPanel = new OutputPanel();            // Affiche la console
    commandPanel = new CommandPanel(this);      // Les boutons
}
```

### 1.3 Le Layout (disposition des panneaux)

On utilise `BorderLayout` pour placer les panneaux :

```
+--------------------------------------------------+
|                    NORTH                         |  <- commandPanel (boutons)
+--------------------------------------------------+
|                                                  |
|                                                  |
|        CENTER (avec split horizontal)            |
|   [sourcePanel]         |    [rightPanel]        |
|                         | callStackPanel         |
|                         | inspectorPanel         |
|                                                  |
+--------------------------------------------------+
|                    SOUTH                         |  <- outputPanel (console)
+--------------------------------------------------+
```

```java
private void layoutComponents() {
    setLayout(new BorderLayout(5, 5));  // Layout principal

    // Boutons en haut
    add(commandPanel, BorderLayout.NORTH);

    // Panneau droit (stack + inspector)
    JPanel rightPanel = new JPanel(new BorderLayout());
    rightPanel.add(callStackPanel, BorderLayout.NORTH);
    rightPanel.add(inspectorPanel, BorderLayout.CENTER);

    // Split entre code source et panneau droit
    JSplitPane mainSplit = new JSplitPane(
        JSplitPane.HORIZONTAL_SPLIT,  // Division horizontale
        sourcePanel,                   // A gauche
        rightPanel                     // A droite
    );
    mainSplit.setResizeWeight(0.7);  // 70% pour le code source

    add(mainSplit, BorderLayout.CENTER);

    // Console en bas
    add(outputPanel, BorderLayout.SOUTH);
}
```

---

## PARTIE 2 : Chaque composant en detail

### 2.1 CommandPanel.java - Les boutons

```java
public class CommandPanel extends JPanel {

    private JButton stepBtn;
    private JButton stepOverBtn;
    private JButton continueBtn;
    private JButton stopBtn;
    private DebuggerGUI debuggerGUI;

    public CommandPanel(DebuggerGUI gui) {
        this.debuggerGUI = gui;

        // Cree les boutons
        stepBtn = new JButton("Step In");
        stepOverBtn = new JButton("Step Over");
        continueBtn = new JButton("Continue");
        stopBtn = new JButton("Stop");

        // Ajoute les actions (quand on clique)
        stepBtn.addActionListener(e -> {
            debuggerGUI.executeCommand("step");  // Envoie "step" au debugger
        });

        stepOverBtn.addActionListener(e -> {
            debuggerGUI.executeCommand("step-over");
        });

        continueBtn.addActionListener(e -> {
            debuggerGUI.executeCommand("continue");
        });

        stopBtn.addActionListener(e -> {
            debuggerGUI.stopDebugger();
        });

        // Layout horizontal
        setLayout(new FlowLayout(FlowLayout.LEFT));
        add(stepBtn);
        add(stepOverBtn);
        add(continueBtn);
        add(stopBtn);
    }

    // Active/desactive les boutons
    public void setButtonsEnabled(boolean enabled) {
        stepBtn.setEnabled(enabled);
        stepOverBtn.setEnabled(enabled);
        continueBtn.setEnabled(enabled);
    }
}
```

**Comment ca marche :**
1. `JButton` = un bouton cliquable
2. `addActionListener` = "quand on clique, fais ca"
3. `e -> { ... }` = une lambda (fonction anonyme)
4. `debuggerGUI.executeCommand("step")` = envoie la commande au debugger

---

### 2.2 SourceCodePanel.java - Affichage du code

```java
public class SourceCodePanel extends JPanel {

    private JTextPane codeArea;      // Zone de texte avec styles
    private JTextArea lineNumbers;   // Numeros de ligne
    private int currentLine = -1;    // Ligne actuelle surlignee
    private Set<Integer> breakpointLines;  // Lignes avec breakpoint

    public SourceCodePanel(DebuggerGUI gui) {
        // Zone des numeros de ligne
        lineNumbers = new JTextArea();
        lineNumbers.setEditable(false);
        lineNumbers.setFont(new Font("Monospaced", Font.PLAIN, 14));

        // Zone du code
        codeArea = new JTextPane();
        codeArea.setEditable(false);
        codeArea.setFont(new Font("Monospaced", Font.PLAIN, 14));

        // Clic sur numero de ligne = toggle breakpoint
        lineNumbers.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int line = getLineFromY(e.getY());  // Calcule la ligne cliquee
                toggleBreakpoint(line);             // Active/desactive breakpoint
            }
        });

        // Layout avec scroll
        JScrollPane scrollPane = new JScrollPane(codeArea);
        scrollPane.setRowHeaderView(lineNumbers);  // Numeros a gauche
        add(scrollPane, BorderLayout.CENTER);
    }

    // Charge un fichier source
    public void loadSourceFile(String filePath) {
        File file = new File(filePath);
        StringBuilder code = new StringBuilder();

        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        while ((line = reader.readLine()) != null) {
            code.append(line).append("\n");
        }
        reader.close();

        codeArea.setText(code.toString());
        updateLineNumbers();
    }

    // Surligne la ligne courante en jaune
    public void highlightLine(int line) {
        StyledDocument doc = codeArea.getStyledDocument();

        // Reset tout en blanc
        Style defaultStyle = codeArea.addStyle("default", null);
        StyleConstants.setBackground(defaultStyle, Color.WHITE);
        doc.setCharacterAttributes(0, doc.getLength(), defaultStyle, true);

        // Trouve la position de la ligne
        String text = codeArea.getText();
        String[] lines = text.split("\n");
        int startPos = 0;
        for (int i = 0; i < line - 1; i++) {
            startPos += lines[i].length() + 1;
        }
        int endPos = startPos + lines[line - 1].length();

        // Applique le jaune
        Style highlightStyle = codeArea.addStyle("highlight", null);
        StyleConstants.setBackground(highlightStyle, new Color(255, 255, 150));
        doc.setCharacterAttributes(startPos, endPos - startPos, highlightStyle, true);
    }
}
```

**Comment ca marche :**
1. `JTextPane` = zone de texte avec styles (couleurs, fond, etc.)
2. `StyledDocument` = permet de modifier le style de parties du texte
3. `StyleConstants.setBackground()` = change la couleur de fond
4. `MouseAdapter` = detecte les clics de souris

---

### 2.3 CallStackPanel.java - La pile d'appels

```java
public class CallStackPanel extends JPanel {

    private JList<String> stackList;  // Liste affichable
    private DefaultListModel<String> listModel;  // Donnees de la liste
    private List<StackFrame> frames;  // Les vraies frames JDI

    public CallStackPanel(DebuggerGUI gui) {
        listModel = new DefaultListModel<>();
        stackList = new JList<>(listModel);

        // Quand on clique sur une frame
        stackList.addListSelectionListener(e -> {
            int index = stackList.getSelectedIndex();
            if (index >= 0 && frames != null) {
                gui.onFrameSelected(frames.get(index));  // Notifie la GUI
            }
        });

        add(new JScrollPane(stackList), BorderLayout.CENTER);
    }

    // Met a jour la liste avec les nouvelles frames
    public void updateStack(List<StackFrame> newFrames) {
        this.frames = newFrames;
        listModel.clear();

        for (int i = 0; i < newFrames.size(); i++) {
            StackFrame frame = newFrames.get(i);
            // Formate: "[0] NomClasse.methode:ligne"
            String text = String.format("[%d] %s.%s:%d",
                i,
                frame.location().declaringType().name(),
                frame.location().method().name(),
                frame.location().lineNumber()
            );
            listModel.addElement(text);
        }
    }
}
```

**Comment ca marche :**
1. `JList` = liste d'elements cliquables
2. `DefaultListModel` = stocke les donnees de la liste
3. `ListSelectionListener` = detecte quand on selectionne un element

---

### 2.4 InspectorPanel.java - Les variables (arbre)

L'INSPECTOR affiche les variables sous forme d'ARBRE :
- Un **objet** = noeud racine affichant le **TYPE** de l'objet
- Ses **variables d'instance** = feuilles du noeud
- Si une variable d'instance est un objet, elle a aussi des feuilles (recursif)

```
Variables
├── this : JDISimpleDebuggee
│   └── (no instance variables)
├── description : String = "Simple power printer"
├── x : int = 40
└── power : int = 2
```

**Code complet :**

```java
package gui;

import com.sun.jdi.*;
import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

// Panneau INSPECTOR qui affiche les variables sous forme d'arbre
public class InspectorPanel extends JPanel {

    private JTree variableTree;
    private DefaultMutableTreeNode rootNode;
    private DefaultTreeModel treeModel;
    private static final int MAX_DEPTH = 3;  // Limite de profondeur

    public InspectorPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("INSPECTOR"));

        // Cree l'arbre Swing
        rootNode = new DefaultMutableTreeNode("Variables");
        treeModel = new DefaultTreeModel(rootNode);
        variableTree = new JTree(treeModel);
        variableTree.setFont(new Font("Monospaced", Font.PLAIN, 12));

        add(new JScrollPane(variableTree), BorderLayout.CENTER);
    }

    // Met a jour l'arbre avec les variables de la frame
    public void updateVariables(StackFrame frame) {
        rootNode.removeAllChildren();

        try {
            // 1. Ajoute "this" (si methode d'instance)
            ObjectReference thisObj = frame.thisObject();
            if (thisObj != null) {
                DefaultMutableTreeNode thisNode = createObjectNode("this", thisObj, 0);
                rootNode.add(thisNode);
            }

            // 2. Ajoute les variables locales
            List<LocalVariable> locals = frame.visibleVariables();
            for (LocalVariable var : locals) {
                Value value = frame.getValue(var);
                DefaultMutableTreeNode varNode = createValueNode(
                    var.name(), var.typeName(), value, 0
                );
                rootNode.add(varNode);
            }

        } catch (Exception e) {
            rootNode.add(new DefaultMutableTreeNode("Error: " + e.getMessage()));
        }

        treeModel.reload();
        expandAllNodes();
    }

    // Cree un noeud pour un OBJET
    // Format: "nom : Type" avec les variables d'instance comme enfants
    private DefaultMutableTreeNode createObjectNode(String name, ObjectReference obj, int depth) {
        String typeName = simplifyTypeName(obj.referenceType().name());
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(name + " : " + typeName);

        if (depth >= MAX_DEPTH) {
            node.add(new DefaultMutableTreeNode("(max depth)"));
            return node;
        }

        // Ajoute chaque variable d'instance comme enfant
        ReferenceType type = obj.referenceType();
        List<Field> fields = type.allFields();
        Map<Field, Value> values = obj.getValues(fields);

        for (Field field : fields) {
            if (field.isStatic()) continue;  // Ignore les champs statiques

            Value value = values.get(field);
            DefaultMutableTreeNode child = createValueNode(
                field.name(), field.typeName(), value, depth + 1
            );
            node.add(child);
        }

        return node;
    }

    // Cree un noeud pour une VALEUR (primitive ou objet)
    private DefaultMutableTreeNode createValueNode(String name, String typeName, Value value, int depth) {
        // Cas null
        if (value == null) {
            return new DefaultMutableTreeNode(name + " : " + simplifyTypeName(typeName) + " = null");
        }

        // Cas String (affiche la valeur)
        if (value instanceof StringReference) {
            String str = ((StringReference) value).value();
            if (str.length() > 50) str = str.substring(0, 50) + "...";
            return new DefaultMutableTreeNode(name + " : String = \"" + str + "\"");
        }

        // Cas objet -> noeud avec enfants (recursif)
        if (value instanceof ObjectReference) {
            return createObjectNode(name, (ObjectReference) value, depth);
        }

        // Cas primitive (int, boolean, etc.)
        return new DefaultMutableTreeNode(
            name + " : " + simplifyTypeName(typeName) + " = " + value.toString()
        );
    }

    // Simplifie "java.lang.String" -> "String"
    private String simplifyTypeName(String fullName) {
        int dot = fullName.lastIndexOf('.');
        return (dot >= 0) ? fullName.substring(dot + 1) : fullName;
    }

    // Deplie tous les noeuds
    private void expandAllNodes() {
        for (int i = 0; i < variableTree.getRowCount(); i++) {
            variableTree.expandRow(i);
        }
    }

    public void clear() {
        rootNode.removeAllChildren();
        treeModel.reload();
    }
}
```

**Comment ca marche :**

| Element | Role |
|---------|------|
| `JTree` | Composant Swing qui affiche un arbre |
| `DefaultMutableTreeNode` | Un noeud de l'arbre (peut avoir des enfants) |
| `DefaultTreeModel` | Modele de donnees pour l'arbre |
| `frame.thisObject()` | Recupere l'objet "this" depuis JDI |
| `frame.visibleVariables()` | Recupere les variables locales depuis JDI |
| `obj.referenceType().allFields()` | Recupere les variables d'instance d'un objet |
| `createObjectNode()` | Cree un noeud pour un objet (avec enfants) |
| `createValueNode()` | Cree un noeud pour une valeur (primitive ou objet) |

**Structure de l'arbre :**
```
Variables (racine)
├── this : MaClasse           <- Noeud objet (affiche le TYPE)
│   ├── attribut1 : int = 5   <- Variable d'instance (feuille)
│   └── attribut2 : String = "hello"
├── varLocale1 : int = 10     <- Variable locale primitive
└── varLocale2 : AutreClasse  <- Variable locale objet
    └── sousAttribut : int = 3  <- Sous-feuille (recursif)
```

---

### 2.5 OutputPanel.java - La console

Le panneau OUTPUT affiche :
- Les messages du debugger (Breakpoint hit!, Stopped at...)
- Les sorties du programme debugge (System.out.println)
- Les commandes executees (>>> step, >>> continue)

**Code complet :**

```java
package gui;

import javax.swing.*;
import java.awt.*;

// Panneau OUTPUT qui affiche la console du debugger
public class OutputPanel extends JPanel {

    private JTextArea outputArea;

    public OutputPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("OUTPUT"));

        // Zone de texte non editable
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        // Style console (fond noir, texte gris clair)
        outputArea.setBackground(new Color(30, 30, 30));
        outputArea.setForeground(new Color(200, 200, 200));

        // Ajoute avec scrollbar
        add(new JScrollPane(outputArea), BorderLayout.CENTER);
        setPreferredSize(new Dimension(400, 150));
    }

    // Ajoute du texte a la console
    public void appendOutput(String text) {
        outputArea.append(text);
        // Scroll automatique vers le bas
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }

    // Efface la console
    public void clear() {
        outputArea.setText("");
    }
}
```

**Comment ca marche :**

| Element | Role |
|---------|------|
| `JTextArea` | Zone de texte multiligne |
| `setEditable(false)` | L'utilisateur ne peut pas modifier le texte |
| `setBackground(Color)` | Couleur de fond (noir pour style console) |
| `setForeground(Color)` | Couleur du texte (gris clair) |
| `append(text)` | Ajoute du texte a la fin |
| `setCaretPosition()` | Scroll vers le bas automatiquement |

**Ce qui s'affiche dans OUTPUT :**
```
Starting debugger on: dbg.JDISimpleDebuggee
Launching VM for: dbg.JDISimpleDebuggee
VM connected successfully
Waiting for class to load...
Event: ClassPrepareEvent
Breakpoint set at line 6
Event: BreakpointEvent
Breakpoint hit!
>>> Stopped at: dbg.JDISimpleDebuggee.main:6
>>> step
>>> Stopped at: dbg.JDISimpleDebuggee.main:7
[Program] Simple power printer -- starting
>>> continue
=== Program ended ===
```

**D'ou viennent ces messages ?**

| Message | Source |
|---------|--------|
| `Starting debugger on...` | `DebuggerGUI.startDebugging()` |
| `Launching VM...` | `GUIScriptableDebugger.connectAndLaunchVM()` |
| `Breakpoint hit!` | `GUIScriptableDebugger.handleBreakpoint()` |
| `>>> Stopped at...` | `DebuggerGUI.onDebuggerStopped()` |
| `>>> step` | `DebuggerGUI.executeCommand()` |
| `[Program] ...` | Sortie du programme debugge |
| `=== Program ended ===` | `DebuggerGUI.onProgramEnded()` |

---

## PARTIE 3 : Comment on appelle JDI

### 3.1 Le pattern Listener (Observer)

L'interface `DebuggerListener` permet a la GUI d'etre notifiee des evenements :

```java
// DebuggerListener.java
public interface DebuggerListener {
    void onDebuggerStopped(StackFrame frame, List<StackFrame> callStack);
    void onOutput(String text);
    void onProgramEnded();
}
```

La GUI implemente cette interface :

```java
// DebuggerGUI.java
public class DebuggerGUI extends JFrame implements DebuggerListener {

    @Override
    public void onDebuggerStopped(StackFrame frame, List<StackFrame> callStack) {
        // Quand le debugger s'arrete, on met a jour l'affichage
        SwingUtilities.invokeLater(() -> {
            callStackPanel.updateStack(callStack);
            updateSourceCode(frame);
            inspectorPanel.updateVariables(frame);
            commandPanel.setButtonsEnabled(true);
        });
    }

    @Override
    public void onOutput(String text) {
        SwingUtilities.invokeLater(() -> {
            outputPanel.appendOutput(text);
        });
    }

    @Override
    public void onProgramEnded() {
        SwingUtilities.invokeLater(() -> {
            outputPanel.appendOutput("=== Program ended ===\n");
            commandPanel.setButtonsEnabled(false);
        });
    }
}
```

**Pourquoi `SwingUtilities.invokeLater()` ?**
Swing n'est pas thread-safe. Seul le thread Swing peut modifier l'interface.
Le debugger tourne dans un autre thread, donc on doit "poster" les mises a jour.

---

### 3.2 Demarrage du debugger

```java
// DebuggerGUI.java
public void startDebugging(Class<?> targetClass, String sourcePath) {
    this.sourceBasePath = sourcePath;

    // Cree le debugger avec nous comme listener
    debugger = new GUIScriptableDebugger(this);

    // Lance dans un thread separe (sinon la GUI freeze)
    Thread debugThread = new Thread(() -> {
        debugger.attachTo(targetClass);
    });
    debugThread.start();
}
```

### 3.3 Envoi d'une commande

```java
// DebuggerGUI.java
public void executeCommand(String command) {
    commandPanel.setButtonsEnabled(false);  // Desactive les boutons
    outputPanel.appendOutput(">>> " + command + "\n");

    // Envoie la commande au debugger
    Thread cmdThread = new Thread(() -> {
        debugger.executeCommand(command);
    });
    cmdThread.start();
}
```

---

## PARTIE 4 : Comment le debugger JDI fonctionne

### 4.1 Connexion a la VM

```java
// GUIScriptableDebugger.java
public VirtualMachine connectAndLaunchVM() throws Exception {
    // Recupere le connecteur par defaut
    LaunchingConnector connector = Bootstrap.virtualMachineManager().defaultConnector();

    // Configure les arguments
    Map<String, Connector.Argument> args = connector.defaultArguments();
    args.get("main").setValue(debugClass.getName());  // Classe a executer
    args.get("options").setValue("-cp " + classpath);  // Classpath

    // Lance la VM et retourne une reference
    return connector.launch(args);
}
```

### 4.2 La boucle d'evenements

```java
// GUIScriptableDebugger.java
private void eventLoop() {
    EventQueue queue = vm.eventQueue();  // File d'evenements JDI

    while (running) {
        EventSet events = queue.remove();  // Attend le prochain evenement

        for (Event event : events) {

            if (event instanceof ClassPrepareEvent) {
                // La classe est chargee -> pose le breakpoint initial
                handleClassPrepare((ClassPrepareEvent) event);
            }
            else if (event instanceof BreakpointEvent) {
                // Breakpoint atteint -> notifie la GUI et attend
                handleBreakpoint((BreakpointEvent) event);
                shouldResume = false;
            }
            else if (event instanceof StepEvent) {
                // Step termine -> notifie la GUI et attend
                handleStep((StepEvent) event);
                shouldResume = false;
            }
            else if (event instanceof VMDeathEvent) {
                // Programme termine
                listener.onProgramEnded();
                return;
            }
        }

        if (shouldResume) {
            events.resume();  // Continue l'execution
        }
    }
}
```

### 4.3 Gestion d'un breakpoint

```java
private void handleBreakpoint(BreakpointEvent event) {
    listener.onOutput("Breakpoint hit!\n");

    currentThread = event.thread();  // Sauvegarde le thread
    notifyStop();      // Notifie la GUI
    waitForCommand();  // Attend une commande (step, continue, etc.)

    vm.resume();  // Reprend l'execution
}

private void notifyStop() {
    StackFrame frame = currentThread.frame(0);  // Frame actuelle
    List<StackFrame> stack = currentThread.frames();  // Pile complete

    listener.onDebuggerStopped(frame, stack);  // Notifie la GUI
}

private void waitForCommand() {
    waitingForCommand = true;

    while (waitingForCommand && running) {
        Thread.sleep(50);  // Attend

        if (pendingCommand != null) {
            processCommand(pendingCommand);  // Execute la commande
            pendingCommand = null;
        }
    }
}
```

### 4.4 Execution d'un step

```java
private void processCommand(String command) {
    switch (command) {
        case "step":
            doStep(StepRequest.STEP_INTO);
            break;
        case "step-over":
            doStep(StepRequest.STEP_OVER);
            break;
        case "continue":
            doContinue();
            break;
    }
}

private void doStep(int depth) {
    // Supprime les anciens step requests
    for (StepRequest sr : vm.eventRequestManager().stepRequests()) {
        sr.disable();
        vm.eventRequestManager().deleteEventRequest(sr);
    }

    // Cree un nouveau step request
    StepRequest sr = vm.eventRequestManager().createStepRequest(
        currentThread,          // Sur quel thread
        StepRequest.STEP_LINE,  // Granularite: par ligne
        depth                   // STEP_INTO ou STEP_OVER
    );
    sr.enable();

    waitingForCommand = false;  // Sort de la boucle d'attente
}
```

---

## PARTIE 5 : Comment les breakpoints fonctionnent

### 5.1 Poser un breakpoint

Quand tu cliques sur un numero de ligne :

```java
// SourceCodePanel.java
private void toggleBreakpoint(int line) {
    if (breakpointLines.contains(line)) {
        breakpointLines.remove(line);  // Enleve le breakpoint
    } else {
        breakpointLines.add(line);  // Ajoute visuellement
        debuggerGUI.addBreakpoint(currentSourcePath, line);  // Pose vraiment
    }
    updateLineNumbers();  // Rafraichit l'affichage (* pour breakpoint)
}
```

```java
// DebuggerGUI.java
public void addBreakpoint(String sourcePath, int line) {
    // Extrait le nom de classe du chemin complet
    String className = extractClassName(sourcePath);
    debugger.addBreakpoint(className, line);
}

// Extrait "dbg.MaClasse" depuis "C:\...\src\dbg\MaClasse.java"
private String extractClassName(String filePath) {
    // Normalise les separateurs
    String path = filePath.replace("\\", "/");

    // Cherche "/src/" dans le chemin
    int srcIndex = path.indexOf("/src/");
    String relativePath;

    if (srcIndex >= 0) {
        // Prend tout apres "/src/"
        relativePath = path.substring(srcIndex + 5);  // +5 pour "/src/"
    } else {
        // Sinon prend juste le nom du fichier
        int lastSlash = path.lastIndexOf("/");
        relativePath = (lastSlash >= 0) ? path.substring(lastSlash + 1) : path;
    }

    // Enleve .java et remplace / par .
    if (relativePath.endsWith(".java")) {
        relativePath = relativePath.substring(0, relativePath.length() - 5);
    }
    return relativePath.replace("/", ".");

    // Exemple:
    // Input:  "C:/Users/.../src/dbg/JDISimpleDebuggee.java"
    // Output: "dbg.JDISimpleDebuggee"
}
```

```java
// GUIScriptableDebugger.java
public void addBreakpoint(String className, int line) {
    // Cherche la classe dans la VM
    for (ReferenceType type : vm.allClasses()) {
        if (type.name().equals(className)) {
            // Trouve la location de la ligne
            List<Location> locs = type.locationsOfLine(line);
            if (!locs.isEmpty()) {
                // Cree et active le breakpoint
                BreakpointRequest bp = vm.eventRequestManager()
                    .createBreakpointRequest(locs.get(0));
                bp.enable();

                breakpoints.add(bp);  // Sauvegarde
                listener.onOutput("Breakpoint added at " + className + ":" + line);
            }
        }
    }
}
```

### 5.2 Le breakpoint initial (ligne 6)

Au demarrage, on pose automatiquement un breakpoint :

```java
// GUIScriptableDebugger.java
private void handleClassPrepare(ClassPrepareEvent event) {
    ReferenceType refType = event.referenceType();

    // Pose un breakpoint sur la ligne 6 (debut du main)
    List<Location> locations = refType.locationsOfLine(6);
    if (!locations.isEmpty()) {
        BreakpointRequest bp = vm.eventRequestManager()
            .createBreakpointRequest(locations.get(0));
        bp.enable();
    }
}
```

---

## PARTIE 6 : Schema de la connexion complete

```
+------------------+
|   MainGUI.java   |  Point d'entree
+--------+---------+
         |
         | cree
         v
+------------------+          +------------------------+
|  DebuggerGUI     |  cree    | GUIScriptableDebugger  |
|  (JFrame)        |--------->| (controle JDI)         |
|                  |          +------------+-----------+
| implements       |                       |
| DebuggerListener |                       | lance
+--------+---------+                       v
         ^                    +------------------------+
         |                    |    VirtualMachine      |
         |   notifie          |    (JDI - la vraie VM) |
         |   (onStopped,      +------------+-----------+
         |    onOutput,                    |
         |    onEnded)                     | execute
         |                                 v
         |                    +------------------------+
         +--------------------| JDISimpleDebuggee.java |
              met a jour      | (programme debugge)    |
              l'affichage     +------------------------+
```

---

## PARTIE 7 : Flux d'execution complet

### Quand tu cliques sur [Step In] :

```
1. CommandPanel detecte le clic
   -> stepBtn.actionPerformed()

2. CommandPanel appelle DebuggerGUI
   -> debuggerGUI.executeCommand("step")

3. DebuggerGUI desactive les boutons et envoie au debugger
   -> debugger.executeCommand("step")

4. GUIScriptableDebugger recoit la commande
   -> pendingCommand = "step"

5. La boucle waitForCommand() voit pendingCommand
   -> processCommand("step")

6. processCommand() cree un StepRequest JDI
   -> doStep(StepRequest.STEP_INTO)
   -> vm.eventRequestManager().createStepRequest(...)

7. waitingForCommand = false, la boucle reprend
   -> vm.resume()

8. JDI execute une ligne et genere un StepEvent

9. eventLoop() recoit le StepEvent
   -> handleStep(event)

10. handleStep() notifie la GUI
    -> listener.onDebuggerStopped(frame, stack)

11. DebuggerGUI recoit la notification
    -> onDebuggerStopped() est appele

12. DebuggerGUI met a jour tous les panneaux
    -> callStackPanel.updateStack(stack)
    -> sourcePanel.highlightLine(lineNumber)
    -> inspectorPanel.updateVariables(frame)
    -> commandPanel.setButtonsEnabled(true)

13. L'utilisateur voit la nouvelle ligne surlignee
```

---

## Resume

| Concept | Ce que c'est |
|---------|--------------|
| **JFrame** | Fenetre Swing |
| **JPanel** | Conteneur pour grouper des composants |
| **JButton** | Bouton cliquable |
| **JTextPane** | Zone de texte avec styles (couleurs) |
| **JList** | Liste cliquable |
| **JTree** | Arbre de donnees |
| **ActionListener** | Detecte les clics sur boutons |
| **MouseListener** | Detecte les clics souris |
| **BorderLayout** | Dispose les composants (NORTH, CENTER, SOUTH...) |
| **SwingUtilities.invokeLater()** | Execute du code sur le thread Swing |
| **VirtualMachine** | La VM JDI qu'on controle |
| **EventQueue** | File des evenements JDI |
| **BreakpointRequest** | Demande de breakpoint |
| **StepRequest** | Demande de step |
| **StackFrame** | Contexte d'execution d'une methode |
