# Debugger JDI - Guide Complet

## PARTIE 1 : C'est quoi un Debugger ?

### Le probleme
Tu as ecrit un programme, mais il fait n'importe quoi. Comment savoir ce qui se passe ?

**Solution 1 (mauvaise)** : Tu mets des `System.out.println()` partout
```java
int x = 10;
System.out.println("x = " + x);  // debug
int y = calculer(x);
System.out.println("y = " + y);  // debug
```
Probleme : c'est long, tu dois recompiler a chaque fois, et tu oublies de les enlever.

**Solution 2 (bonne)** : Tu utilises un DEBUGGER !

### C'est quoi un Debugger ?
Un debugger, c'est un programme qui **controle** un autre programme pendant qu'il s'execute.

Il peut :
1. **STOPPER** le programme a un endroit precis (breakpoint)
2. **INSPECTER** les variables (voir leur valeur)
3. **AVANCER** pas a pas dans le code

```
TON PROGRAMME          DEBUGGER              TOI
     |                    |                   |
     |---> execute ------>|                   |
     |                    |<--- "STOP!" ------| (tu poses un breakpoint)
     |     (pause)        |                   |
     |                    |---> te montre le code, les variables
     |                    |<--- "next line" --| (tu avances d'une ligne)
     |---> continue ----->|                   |
```

---

## PARTIE 2 : Les Concepts Cles

### 1. Le Breakpoint (Point d'arret)
Un breakpoint = "arrete-toi ici !"

```java
public static void main(String[] args) {
    int x = 10;        // ligne 1
    int y = 20;        // ligne 2  <-- BREAKPOINT ICI
    int z = x + y;     // ligne 3
}
```

Quand tu mets un breakpoint ligne 2, le programme s'arrete AVANT d'executer la ligne 2.
A ce moment, `x = 10` mais `y` n'existe pas encore.

### 2. La Frame (Contexte d'execution)
Une frame = tout ce qui existe quand une methode s'execute.

```java
void maMethode(int a) {    // a = argument
    int b = 5;             // b = variable locale
    // La "frame" contient : a, b, et this (l'objet)
}
```

Chaque fois que tu appelles une methode, une nouvelle frame est creee.

### 3. La Stack (Pile d'appels)
La stack = l'empilement des frames.

```java
void main() {
    foo();        // appelle foo
}

void foo() {
    bar();        // appelle bar
}

void bar() {
    // ON EST ICI
}
```

La stack ressemble a ca :
```
[0] bar()   <-- top (ou on est maintenant)
[1] foo()   <-- a appele bar()
[2] main()  <-- a appele foo()
```

### 4. Step vs Step-Over (TRES IMPORTANT)

```java
void main() {
    int x = 5;
    foo(x);      // <-- on est ici
    int y = 10;
}

void foo(int n) {
    System.out.println(n);  // ligne A
    System.out.println(n);  // ligne B
}
```

- **STEP (Step Into)** : Tu ENTRES dans foo() et tu t'arretes a la ligne A
- **STEP-OVER** : Tu SAUTES foo() (elle s'execute en entier) et tu t'arretes a `int y = 10`

En gros :
- Step = je veux voir ce qui se passe DANS la methode
- Step-Over = je me fiche de cette methode, passe a la ligne suivante

---

## PARTIE 3 : TP1 - Le Debugger Console

### Qu'est-ce qu'on a construit ?
Un debugger en ligne de commande. Tu tapes des commandes comme `step`, `frame`, etc.

### Les fichiers du TP1 (dans `src/dbg/`)

| Fichier | Role |
|---------|------|
| `ScriptableDebugger.java` | Le CERVEAU du debugger - gere la VM, les events, les breakpoints |
| `Command.java` | Interface pour toutes les commandes |
| `CommandRegistry.java` | Registre qui stocke toutes les commandes disponibles |
| `JDISimpleDebuggee.java` | Le programme de TEST qu'on debugge |

### Les Commandes de Navigation

| Commande | Fichier | Ce que ca fait |
|----------|---------|----------------|
| `step` | `StepCommand.java` | Avance d'1 instruction. Entre dans les methodes. |
| `step-over` | `StepOverCommand.java` | Avance d'1 ligne. Saute les methodes. |
| `continue` | `ContinueCommand.java` | Continue jusqu'au prochain breakpoint. |

**Dans le code :**
```java
// StepCommand.java
public Object execute() {
    debugger.step();  // appelle la methode step() du debugger
    return ">>> Stepping...";
}

// ScriptableDebugger.java - la vraie logique
public void step() {
    stepWithType(StepRequest.STEP_INTO);  // STEP_INTO = entre dans les methodes
    shouldResume = true;
}

public void stepOver() {
    stepWithType(StepRequest.STEP_OVER);  // STEP_OVER = saute les methodes
    shouldResume = true;
}
```

### Les Commandes d'Inspection

| Commande | Fichier | Ce que ca fait |
|----------|---------|----------------|
| `frame` | `FrameCommand.java` | Montre ou on est (classe.methode:ligne) |
| `stack` | `StackCommand.java` | Montre toute la pile d'appels |
| `method` | `MethodCommand.java` | Montre la signature de la methode actuelle |
| `temporaries` | `TemporariesCommand.java` | Montre les variables locales |
| `arguments` | `ArgumentsCommand.java` | Montre les arguments de la methode |
| `receiver` | `ReceiverCommand.java` | Montre l'objet `this` |
| `sender` | `SenderCommand.java` | Montre qui a appele cette methode |
| `receiver-variables` | `ReceiverVariablesCommand.java` | Montre les attributs de `this` |
| `print-var x` | `PrintVarCommand.java` | Montre la valeur de la variable `x` |

**Exemple de code (TemporariesCommand.java) :**
```java
public Object execute() {
    StackFrame frame = debugger.getCurrentFrame();  // recupere la frame actuelle

    // Recupere toutes les variables visibles
    List<LocalVariable> variables = frame.visibleVariables();

    // Affiche chaque variable avec sa valeur
    for (LocalVariable var : variables) {
        Value value = frame.getValue(var);  // recupere la valeur
        System.out.println(var.name() + " = " + value);
    }
}
```

### Les Commandes de Breakpoint

| Commande | Fichier | Ce que ca fait |
|----------|---------|----------------|
| `break Classe 10` | `BreakCommand.java` | S'arrete a la ligne 10 de Classe |
| `breakpoints` | `BreakpointsCommand.java` | Liste tous les breakpoints |
| `break-once Classe 10` | `BreakOnceCommand.java` | S'arrete 1 fois puis se supprime |
| `break-on-count Classe 10 5` | `BreakOnCountCommand.java` | S'arrete apres 5 passages |
| `break-before-method-call foo` | `BreakBeforeMethodCallCommand.java` | S'arrete quand foo() est appelee |

---

## PARTIE 4 : TP2 - Le Debugger Graphique (GUI)

### Qu'est-ce qu'on a construit ?
La meme chose que TP1, mais avec une interface graphique (boutons, fenetres, etc.)

```
+--------------------------------------------------+
|  [Step In] [Step Over] [Continue] [Stop]         | <- BOUTONS
+--------------------------------------------------+
| CALL STACK      |     SOURCE CODE                |
| [0] main:6      |  1  public class Test {        |
| [1] foo:12      |  2    void main() {            |
|                 | *3      int x = 5;  <--        | <- ligne actuelle (jaune)
|                 |  4    }                        |
+-----------------+--------------------------------+
| INSPECTOR       |                                |
| > this          |                                |
|   > x = 5       |                                |
+-----------------+--------------------------------+
|              OUTPUT                              |
| >>> Stopped at Test.main:3                       |
+--------------------------------------------------+
```

### Les fichiers du TP2 (dans `src/gui/`)

| Fichier | Role |
|---------|------|
| `MainGUI.java` | Point d'entree - lance l'interface |
| `DebuggerGUI.java` | Fenetre principale - assemble tous les panneaux |
| `GUIScriptableDebugger.java` | Version adaptee du debugger pour la GUI |
| `SourceCodePanel.java` | Affiche le code source avec surlignage |
| `CallStackPanel.java` | Affiche la pile d'appels (cliquable) |
| `InspectorPanel.java` | Affiche les variables en arbre |
| `OutputPanel.java` | Affiche la console |
| `CommandPanel.java` | Les boutons (Step In, Step Over, etc.) |
| `DebuggerListener.java` | Interface pour les events du debugger |

### Lien entre les boutons et le code

Quand tu cliques sur un bouton, voici ce qui se passe :

```
[Bouton Step In]
    -> CommandPanel.java : actionPerformed("step")
    -> DebuggerGUI.java : executeCommand("step")
    -> GUIScriptableDebugger.java : executeCommand("step")
    -> processCommand("step")
    -> doStep(StepRequest.STEP_INTO)
```

**Dans CommandPanel.java :**
```java
stepBtn.addActionListener(e -> {
    debuggerGUI.executeCommand("step");  // envoie la commande "step"
});

stepOverBtn.addActionListener(e -> {
    debuggerGUI.executeCommand("step-over");  // envoie "step-over"
});
```

**Dans GUIScriptableDebugger.java :**
```java
private void processCommand(String command) {
    switch (command) {
        case "step":
            doStep(StepRequest.STEP_INTO);   // entre dans les methodes
            break;
        case "step-over":
            doStep(StepRequest.STEP_OVER);   // saute les methodes
            break;
        case "continue":
            doContinue();                     // continue jusqu'au prochain breakpoint
            break;
    }
}
```

---

## PARTIE 5 : Comment Tester ?

### Tester le TP1 (Console)

1. **Compile tout :**
```bash
cd DebuggerJDI
javac -g -d out src/dbg/*.java
```

2. **Lance le debugger console :**
```bash
java -cp out dbg.Main
```

3. **Teste les commandes :**
```
>>> Enter command:
frame          <- tape ca, tu vois ou tu es
>>> Enter command:
temporaries    <- tape ca, tu vois les variables locales
>>> Enter command:
step           <- tape ca, tu avances d'une ligne
>>> Enter command:
step-over      <- tape ca, tu sautes la methode
>>> Enter command:
continue       <- tape ca, le programme continue
```

### Tester le TP2 (GUI)

1. **Compile tout :**
```bash
cd DebuggerJDI
javac -g -d out src/dbg/*.java src/gui/*.java
```

2. **Lance l'interface graphique :**
```bash
java -cp out gui.MainGUI
```

3. **Ce que tu devrais voir :**
   - La fenetre s'ouvre
   - Dans OUTPUT : "Breakpoint hit!" (le programme s'est arrete)
   - Le code source s'affiche avec la ligne 6 en JAUNE
   - Les variables apparaissent dans l'inspector

4. **Teste les boutons :**
   - Clique sur **[Step In]** : tu avances d'une ligne
   - Clique sur **[Step Over]** : tu sautes la methode appelee
   - Clique sur **[Continue]** : le programme continue jusqu'a la fin

---

## PARTIE 6 : Demo pour le Prof

### Comment montrer STEP IN

1. Lance le debugger GUI
2. Quand il s'arrete sur la ligne `printPower(x, power);` (ligne 10)
3. Clique sur **[Step In]**
4. **Resultat** : Tu entres DANS la methode `printPower()` et tu t'arretes a sa premiere ligne

**Montre dans le code :**
```java
// GUIScriptableDebugger.java ligne 248-263
private void doStep(int depth) {
    StepRequest sr = vm.eventRequestManager().createStepRequest(
        currentThread,
        StepRequest.STEP_LINE,
        depth  // depth = STEP_INTO pour "step in"
    );
    sr.enable();
    waitingForCommand = false;
}
```

### Comment montrer STEP OVER

1. Lance le debugger GUI
2. Quand il s'arrete sur la ligne `printPower(x, power);` (ligne 10)
3. Clique sur **[Step Over]**
4. **Resultat** : La methode `printPower()` s'execute EN ENTIER, et tu t'arretes a la ligne suivante (fin du main)

**La difference dans le code :**
```java
// Step In:  doStep(StepRequest.STEP_INTO)  -> entre dans la methode
// Step Over: doStep(StepRequest.STEP_OVER) -> saute la methode
```

### Comment montrer CONTINUE

1. Lance le debugger GUI
2. Le programme est arrete
3. Clique sur **[Continue]**
4. **Resultat** : Le programme continue jusqu'au prochain breakpoint, ou jusqu'a la fin

**Dans le code :**
```java
// GUIScriptableDebugger.java ligne 265-268
private void doContinue() {
    waitingForCommand = false;  // on arrete d'attendre une commande
    // La boucle eventLoop() reprend et fait vm.resume()
}
```

---

## PARTIE 7 : Architecture Globale

```
                    +------------------+
                    |    MainGUI.java  |  <- Point d'entree
                    +--------+---------+
                             |
                             v
                    +------------------+
                    | DebuggerGUI.java |  <- Fenetre principale
                    +--------+---------+
                             |
         +-------------------+-------------------+
         |                   |                   |
         v                   v                   v
+----------------+  +----------------+  +----------------+
| SourceCodePanel|  | CallStackPanel |  | InspectorPanel |
| (affiche code) |  | (pile appels)  |  | (variables)    |
+----------------+  +----------------+  +----------------+
         |                   |                   |
         +-------------------+-------------------+
                             |
                             v
               +-------------------------+
               | GUIScriptableDebugger   |  <- Controle la VM
               +------------+------------+
                            |
                            v
               +-------------------------+
               |    VirtualMachine (JDI) |  <- La vraie VM Java
               +------------+------------+
                            |
                            v
               +-------------------------+
               | JDISimpleDebuggee.java  |  <- Programme debugge
               +-------------------------+
```

---

## PARTIE 8 : JDI c'est quoi ?

**JDI = Java Debug Interface**

C'est une API fournie par Java pour controler une JVM.

Classes importantes :
- `VirtualMachine` : represente la JVM qu'on debugge
- `ThreadReference` : un thread dans la JVM
- `StackFrame` : une frame dans la pile
- `BreakpointRequest` : une demande de breakpoint
- `StepRequest` : une demande de step
- `Event` : un evenement (breakpoint atteint, step termine, etc.)

C'est EXACTEMENT ce que IntelliJ et Eclipse utilisent pour leur debugger !

---

## Resume en 1 minute

1. **Debugger** = outil pour controler un programme pendant son execution
2. **Breakpoint** = "arrete-toi ici"
3. **Frame** = contexte d'une methode (variables locales, arguments)
4. **Stack** = pile des frames (qui a appele qui)
5. **Step** = avance d'une ligne, entre dans les methodes
6. **Step-Over** = avance d'une ligne, saute les methodes
7. **Continue** = reprend jusqu'au prochain breakpoint
8. **JDI** = l'API Java qui permet tout ca

Le TP1 fait ca en console (tu tapes des commandes).
Le TP2 fait ca avec une interface graphique (tu cliques sur des boutons).
