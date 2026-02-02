package dbg;

// Place un breakpoint au debut d'une methode
public class BreakBeforeMethodCallCommand implements Command {

    private ScriptableDebugger debugger;
    private String methodName;

    public BreakBeforeMethodCallCommand(ScriptableDebugger debugger, String methodName) {
        this.debugger = debugger;
        this.methodName = methodName;
    }

    @Override
    public Object execute() {
        boolean success = debugger.addMethodBreakpoint(methodName);
        if (success) {
            return "Method breakpoint set on: " + methodName;
        } else {
            return "Failed to set method breakpoint on: " + methodName;
        }
    }
}
