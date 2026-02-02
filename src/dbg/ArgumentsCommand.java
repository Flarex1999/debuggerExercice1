package dbg;

import com.sun.jdi.*;

import java.util.List;

// Affiche les arguments de la methode courante
public class ArgumentsCommand implements Command {

    private ScriptableDebugger debugger;

    public ArgumentsCommand(ScriptableDebugger debugger) {
        this.debugger = debugger;
    }

    @Override
    public Object execute() {
        try {
            StackFrame frame = debugger.getCurrentFrame();

            if (frame == null) {
                return "No frame available";
            }

            Method method = frame.location().method();
            List<LocalVariable> arguments = method.arguments();

            if (arguments.isEmpty()) {
                return "No arguments";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Arguments:\n");

            for (LocalVariable arg : arguments) {
                Value value = frame.getValue(arg);
                sb.append(String.format("  %s -> %s\n",
                        arg.name(),
                        value != null ? value.toString() : "null"
                ));
            }

            return sb.toString().trim();

        } catch (IncompatibleThreadStateException e) {
            return "Error: Thread not suspended";
        } catch (AbsentInformationException e) {
            return "Error: No debug info (compile with -g)";
        }
    }
}
