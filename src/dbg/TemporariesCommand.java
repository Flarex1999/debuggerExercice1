package dbg;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.StackFrame;
import com.sun.jdi.Value;

import java.util.List;
import java.util.Map;

public class TemporariesCommand implements Command {

    private ScriptableDebugger debugger;

    public TemporariesCommand(ScriptableDebugger debugger) {
        this.debugger = debugger;
    }

    @Override
    public Object execute() {
        try {
            StackFrame frame = debugger.getCurrentFrame();

            if (frame == null) {
                return "No frame available";
            }

            List<LocalVariable> localVars = frame.visibleVariables();

            if (localVars.isEmpty()) {
                return "No local variables";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Local Variables:\n");

            for (LocalVariable var : localVars) {
                Value value = frame.getValue(var);
                sb.append(String.format("  %s = %s\n",
                        var.name(),
                        value != null ? value.toString() : "null"
                ));
            }

            return sb.toString().trim();

        } catch (IncompatibleThreadStateException e) {
            return "Error: Thread not suspended";
        } catch (AbsentInformationException e) {
            return "Error: No debug information available (compile with -g flag)";
        }
    }
}