package dbg;

import java.util.List;

// Liste tous les breakpoints actifs
public class BreakpointsCommand implements Command {

    private ScriptableDebugger debugger;

    public BreakpointsCommand(ScriptableDebugger debugger) {
        this.debugger = debugger;
    }

    @Override
    public Object execute() {
        List<BreakpointInfo> breakpoints = debugger.getBreakpoints();

        if (breakpoints == null || breakpoints.isEmpty()) {
            return "No breakpoints";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Breakpoints:\n");

        for (int i = 0; i < breakpoints.size(); i++) {
            sb.append(String.format("  [%d] %s\n", i, breakpoints.get(i).toString()));
        }

        return sb.toString().trim();
    }
}
