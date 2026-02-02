package dbg;

import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StackFrame;

import java.util.List;

// Affiche l'objet qui a appele la methode courante (le sender)
public class SenderCommand implements Command {

    private ScriptableDebugger debugger;

    public SenderCommand(ScriptableDebugger debugger) {
        this.debugger = debugger;
    }

    @Override
    public Object execute() {
        try {
            List<StackFrame> frames = debugger.getStackFrames();

            if (frames == null || frames.size() < 2) {
                return "No sender (top of stack or no caller)";
            }

            // La frame 0 est la courante, la frame 1 est l'appelant
            StackFrame senderFrame = frames.get(1);
            ObjectReference sender = senderFrame.thisObject();

            if (sender == null) {
                return "Sender: null (called from static method)";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Sender:\n");
            sb.append("  Type: ").append(sender.referenceType().name()).append("\n");
            sb.append("  ID: ").append(sender.uniqueID()).append("\n");
            sb.append("  Method: ").append(senderFrame.location().method().name());

            return sb.toString();

        } catch (IncompatibleThreadStateException e) {
            return "Error: Thread not suspended";
        }
    }
}
