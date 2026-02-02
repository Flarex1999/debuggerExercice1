package dbg;

import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.StackFrame;
import com.sun.jdi.event.LocatableEvent;

import java.util.List;

public class StackCommand implements Command {

    private ScriptableDebugger debugger;

    public StackCommand(ScriptableDebugger debugger) {
        this.debugger = debugger;
    }

    @Override
    public Object execute() {
        try {
            List<StackFrame> frames = debugger.getStackFrames();

            if (frames == null || frames.isEmpty()) {
                return "No stack frames available";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Call Stack:\n");

            for (int i = 0; i < frames.size(); i++) {
                StackFrame frame = frames.get(i);
                sb.append(String.format("  [%d] %s.%s (line %d)\n",
                        i,
                        frame.location().declaringType().name(),
                        frame.location().method().name(),
                        frame.location().lineNumber()
                ));
            }

            return sb.toString().trim();

        } catch (IncompatibleThreadStateException e) {
            return "Error: Cannot get stack (thread not suspended)";
        }
    }
}