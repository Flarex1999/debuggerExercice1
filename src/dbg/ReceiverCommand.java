package dbg;

import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StackFrame;

// Affiche l'objet receveur (this) de la methode courante
public class ReceiverCommand implements Command {

    private ScriptableDebugger debugger;

    public ReceiverCommand(ScriptableDebugger debugger) {
        this.debugger = debugger;
    }

    @Override
    public Object execute() {
        try {
            StackFrame frame = debugger.getCurrentFrame();

            if (frame == null) {
                return "No frame available";
            }

            // thisObject() retourne null pour les methodes statiques
            ObjectReference thisObject = frame.thisObject();

            if (thisObject == null) {
                return "Receiver: null (static method)";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Receiver (this):\n");
            sb.append("  Type: ").append(thisObject.referenceType().name()).append("\n");
            sb.append("  ID: ").append(thisObject.uniqueID());

            return sb.toString();

        } catch (IncompatibleThreadStateException e) {
            return "Error: Thread not suspended";
        }
    }
}
