package gui;

import com.sun.jdi.StackFrame;
import java.util.List;

// Interface pour recevoir les evenements du debugger
public interface DebuggerListener {

    // Appele quand le debugger s'arrete (breakpoint ou step)
    void onDebuggerStopped(StackFrame currentFrame, List<StackFrame> callStack);

    // Appele quand le programme debugge affiche quelque chose
    void onOutput(String text);

    // Appele quand le programme se termine
    void onProgramEnded();
}
