package dbg;

public class StepOverCommand implements Command {

    private ScriptableDebugger debugger;

    public StepOverCommand(ScriptableDebugger debugger) {
        this.debugger = debugger;
    }

    @Override
    public Object execute() {
        debugger.stepOver();
        return null;
    }
}