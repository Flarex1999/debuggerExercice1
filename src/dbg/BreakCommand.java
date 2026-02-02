package dbg;

// Place un breakpoint a une ligne donnee
public class BreakCommand implements Command {

    private ScriptableDebugger debugger;
    private String filename;
    private int lineNumber;

    public BreakCommand(ScriptableDebugger debugger, String filename, int lineNumber) {
        this.debugger = debugger;
        this.filename = filename;
        this.lineNumber = lineNumber;
    }

    @Override
    public Object execute() {
        boolean success = debugger.addBreakpoint(filename, lineNumber);
        if (success) {
            return "Breakpoint set at " + filename + ":" + lineNumber;
        } else {
            return "Failed to set breakpoint at " + filename + ":" + lineNumber;
        }
    }
}
