package dbg;

import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.Method;
import com.sun.jdi.StackFrame;

public class MethodCommand implements Command {

    private ScriptableDebugger debugger;

    public MethodCommand(ScriptableDebugger debugger) {
        this.debugger = debugger;
    }

    @Override
    public Object execute() {
        try {
            StackFrame frame = debugger.getCurrentFrame();
            if (frame != null) {
                Method method = frame.location().method();

                StringBuilder sb = new StringBuilder();
                sb.append("Method: ").append(method.name()).append("\n");
                sb.append("  Signature: ").append(method.signature()).append("\n");
                sb.append("  Class: ").append(method.declaringType().name()).append("\n");
                sb.append("  Line: ").append(frame.location().lineNumber());

                return sb.toString();
            } else {
                return "No method available";
            }
        } catch (IncompatibleThreadStateException e) {
            return "Error: Cannot get method (thread not suspended)";
        }
    }
}