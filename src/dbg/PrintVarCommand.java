package dbg;

import com.sun.jdi.*;

// Affiche la valeur d'une variable specifique
public class PrintVarCommand implements Command {

    private ScriptableDebugger debugger;
    private String varName;

    public PrintVarCommand(ScriptableDebugger debugger, String varName) {
        this.debugger = debugger;
        this.varName = varName;
    }

    @Override
    public Object execute() {
        try {
            StackFrame frame = debugger.getCurrentFrame();

            if (frame == null) {
                return "No frame available";
            }

            // Cherche d'abord dans les variables locales
            try {
                LocalVariable localVar = frame.visibleVariableByName(varName);
                if (localVar != null) {
                    Value value = frame.getValue(localVar);
                    return varName + " -> " + (value != null ? value.toString() : "null");
                }
            } catch (AbsentInformationException e) {
                // Pas d'info de debug, on continue
            }

            // Cherche dans les champs de l'objet this
            ObjectReference thisObject = frame.thisObject();
            if (thisObject != null) {
                ReferenceType type = thisObject.referenceType();
                Field field = type.fieldByName(varName);
                if (field != null) {
                    Value value = thisObject.getValue(field);
                    return varName + " -> " + (value != null ? value.toString() : "null");
                }
            }

            return "Variable not found: " + varName;

        } catch (IncompatibleThreadStateException e) {
            return "Error: Thread not suspended";
        }
    }
}
