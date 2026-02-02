package dbg;

// Place un breakpoint qui se supprime apres 1 passage
public class BreakOnceCommand implements Command {

    private ScriptableDebugger debugger;
    private String filename;
    private int lineNumber;

    public BreakOnceCommand(ScriptableDebugger debugger, String filename, int lineNumber) {
        this.debugger = debugger;
        this.filename = filename;
        this.lineNumber = lineNumber;
    }

    @Override
    public Object execute() {
        boolean success = debugger.addBreakpointOnce(filename, lineNumber);
        if (success) {
            return "One-time breakpoint set at " + filename + ":" + lineNumber;
        } else {
            return "Failed to set breakpoint at " + filename + ":" + lineNumber;
        }
    }
}
