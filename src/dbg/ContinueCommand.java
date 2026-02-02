package dbg;

import com.sun.jdi.request.StepRequest;

public class ContinueCommand implements Command {

    private ScriptableDebugger debugger;

    public ContinueCommand(ScriptableDebugger debugger) {
        this.debugger = debugger;
    }

    @Override
    public Object execute() {
        debugger.continueExecution();
        return null;
    }
}