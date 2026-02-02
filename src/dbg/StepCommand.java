package dbg;

public class StepCommand implements Command {

    private ScriptableDebugger debugger;

    public StepCommand(ScriptableDebugger debugger) {
        this.debugger = debugger;
    }

    @Override
    public Object execute() {
        debugger.step();
        return null;  // <-- Retourne null car step ne retourne rien
    }
}