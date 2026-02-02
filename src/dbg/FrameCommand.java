package dbg;

import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.StackFrame;
import com.sun.jdi.event.LocatableEvent;

public class FrameCommand implements Command {

    private ScriptableDebugger debugger;

    public FrameCommand(ScriptableDebugger debugger) {
        this.debugger = debugger;
    }

    @Override
    public Object execute() {
        try {
            StackFrame frame = debugger.getCurrentFrame();
            if (frame != null) {
                String frameInfo = String.format("Frame: %s.%s (line %d)",
                        frame.location().declaringType().name(),
                        frame.location().method().name(),
                        frame.location().lineNumber()
                );
                return frameInfo;
            } else {
                return "No frame available";
            }
        } catch (IncompatibleThreadStateException e) {
            return "Error: Cannot get frame (thread not suspended)";
        }
    }
}