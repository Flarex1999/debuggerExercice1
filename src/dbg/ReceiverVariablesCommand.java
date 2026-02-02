package dbg;

import com.sun.jdi.*;

import java.util.Map;

// Affiche les variables d'instance (attributs) de l'objet this
public class ReceiverVariablesCommand implements Command {

    private ScriptableDebugger debugger;

    public ReceiverVariablesCommand(ScriptableDebugger debugger) {
        this.debugger = debugger;
    }

    @Override
    public Object execute() {
        try {
            StackFrame frame = debugger.getCurrentFrame();

            if (frame == null) {
                return "No frame available";
            }

            ObjectReference thisObject = frame.thisObject();

            if (thisObject == null) {
                return "No receiver (static method)";
            }

            // Recupere tous les champs de l'objet
            ReferenceType type = thisObject.referenceType();
            Map<Field, Value> fieldValues = thisObject.getValues(type.allFields());

            if (fieldValues.isEmpty()) {
                return "No instance variables";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Instance variables of ").append(type.name()).append(":\n");

            for (Map.Entry<Field, Value> entry : fieldValues.entrySet()) {
                Field field = entry.getKey();
                Value value = entry.getValue();

                // Ignore les champs statiques
                if (field.isStatic()) {
                    continue;
                }

                sb.append(String.format("  %s -> %s\n",
                        field.name(),
                        value != null ? value.toString() : "null"
                ));
            }

            return sb.toString().trim();

        } catch (IncompatibleThreadStateException e) {
            return "Error: Thread not suspended";
        }
    }
}
