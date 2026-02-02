package dbg;

// Place un breakpoint qui s'active apres N passages
public class BreakOnCountCommand implements Command {

    private ScriptableDebugger debugger;
    private String filename;
    private int lineNumber;
    private int count;

    public BreakOnCountCommand(ScriptableDebugger debugger, String filename, int lineNumber, int count) {
        this.debugger = debugger;
        this.filename = filename;
        this.lineNumber = lineNumber;
        this.count = count;
    }

    @Override
    public Object execute() {
        boolean success = debugger.addBreakpointOnCount(filename, lineNumber, count);
        if (success) {
            return "Count breakpoint set at " + filename + ":" + lineNumber + " (activates after " + count + " hits)";
        } else {
            return "Failed to set breakpoint at " + filename + ":" + lineNumber;
        }
    }
}
