import dbg.JDISimpleDebuggee;
import dbg.ScriptableDebugger;

public class Main {
    public static void main(String[] args) {
        System.out.println("Starting Scriptable Debugger...");
        System.out.println("Commands: step, step-over, continue, frame, method, stack,");
        System.out.println("          temporaries, arguments, receiver, sender,");
        System.out.println("          receiver-variables, breakpoints,");
        System.out.println("          print-var <name>, break <file> <line>,");
        System.out.println("          break-once <file> <line>, break-on-count <file> <line> <n>,");
        System.out.println("          break-before-method-call <method>");
        System.out.println();

        ScriptableDebugger debugger = new ScriptableDebugger();
        debugger.attachTo(JDISimpleDebuggee.class);
    }
}
