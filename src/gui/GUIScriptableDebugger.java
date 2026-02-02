package gui;

import com.sun.jdi.*;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Version du debugger adaptee pour l'interface graphique
public class GUIScriptableDebugger {

    private Class<?> debugClass;
    private VirtualMachine vm;
    private ThreadReference currentThread;
    private DebuggerListener listener;
    private volatile boolean running = true;
    private volatile boolean waitingForCommand = false;
    private volatile String pendingCommand = null;
    private List<BreakpointRequest> breakpoints;

    public GUIScriptableDebugger(DebuggerListener listener) {
        this.listener = listener;
        this.breakpoints = new ArrayList<>();
    }

    // Connecte et lance la VM
    public VirtualMachine connectAndLaunchVM() throws Exception {
        LaunchingConnector connector = Bootstrap.virtualMachineManager().defaultConnector();
        Map<String, Connector.Argument> args = connector.defaultArguments();

        // Configure la classe principale
        args.get("main").setValue(debugClass.getName());

        // Configure le classpath (IMPORTANT: sinon la VM ne trouve pas la classe)
        String classpath = System.getProperty("java.class.path");
        args.get("options").setValue("-cp " + classpath);

        listener.onOutput("Launching VM for: " + debugClass.getName() + "\n");
        listener.onOutput("Classpath: " + classpath + "\n");

        return connector.launch(args);
    }

    // Demarre le debugging
    public void attachTo(Class<?> targetClass) {
        this.debugClass = targetClass;

        try {
            vm = connectAndLaunchVM();
            listener.onOutput("VM connected successfully\n");

            // Prepare la capture de la classe
            ClassPrepareRequest cpr = vm.eventRequestManager().createClassPrepareRequest();
            cpr.addClassFilter(debugClass.getName());
            cpr.enable();
            listener.onOutput("Waiting for class to load...\n");

            // Lance la boucle d'evenements
            eventLoop();

        } catch (Exception e) {
            e.printStackTrace();
            listener.onOutput("Error: " + e.getMessage() + "\n");
            if (e.getCause() != null) {
                listener.onOutput("Cause: " + e.getCause().getMessage() + "\n");
            }
        }
    }

    // Boucle principale des evenements
    private void eventLoop() {
        EventQueue queue = vm.eventQueue();

        while (running) {
            try {
                EventSet events = queue.remove();
                if (events == null) continue;

                boolean shouldResume = true;

                for (Event event : events) {
                    listener.onOutput("Event: " + event.getClass().getSimpleName() + "\n");

                    if (event instanceof ClassPrepareEvent) {
                        handleClassPrepare((ClassPrepareEvent) event);
                    }
                    else if (event instanceof BreakpointEvent) {
                        handleBreakpoint((BreakpointEvent) event);
                        shouldResume = false; // Ne pas reprendre, on attend une commande
                    }
                    else if (event instanceof StepEvent) {
                        handleStep((StepEvent) event);
                        shouldResume = false; // Ne pas reprendre, on attend une commande
                    }
                    else if (event instanceof VMDeathEvent || event instanceof VMDisconnectEvent) {
                        running = false;
                        listener.onProgramEnded();
                        return;
                    }
                }

                if (running && shouldResume) {
                    events.resume();
                }

            } catch (InterruptedException e) {
                listener.onOutput("Interrupted\n");
                break;
            } catch (VMDisconnectedException e) {
                listener.onOutput("VM Disconnected\n");
                listener.onProgramEnded();
                break;
            }
        }
    }

    // Classe prete - on pose le breakpoint initial
    private void handleClassPrepare(ClassPrepareEvent event) {
        try {
            ReferenceType refType = event.referenceType();
            listener.onOutput("Class loaded: " + refType.name() + "\n");

            List<Location> locations = refType.locationsOfLine(6);
            if (!locations.isEmpty()) {
                BreakpointRequest bp = vm.eventRequestManager().createBreakpointRequest(locations.get(0));
                bp.enable();
                listener.onOutput("Breakpoint set at line 6\n");
            } else {
                listener.onOutput("Warning: Could not find line 6\n");
                // Essaie de mettre un breakpoint sur la premiere ligne disponible
                List<Location> allLocs = refType.allLineLocations();
                if (!allLocs.isEmpty()) {
                    BreakpointRequest bp = vm.eventRequestManager().createBreakpointRequest(allLocs.get(0));
                    bp.enable();
                    listener.onOutput("Breakpoint set at line " + allLocs.get(0).lineNumber() + " instead\n");
                }
            }
        } catch (AbsentInformationException e) {
            listener.onOutput("ERROR: No debug info! Recompile with: javac -g\n");
        }
    }

    // Breakpoint atteint
    private void handleBreakpoint(BreakpointEvent event) {
        listener.onOutput("Breakpoint hit!\n");
        currentThread = event.thread();
        notifyStop();
        waitForCommand();
        // Apres la commande, on reprend
        vm.resume();
    }

    // Step termine
    private void handleStep(StepEvent event) {
        // Desactive le step request
        StepRequest sr = (StepRequest) event.request();
        sr.disable();
        vm.eventRequestManager().deleteEventRequest(sr);

        currentThread = event.thread();
        notifyStop();
        waitForCommand();
        // Apres la commande, on reprend
        vm.resume();
    }

    // Notifie l'interface qu'on s'est arrete
    private void notifyStop() {
        try {
            if (currentThread != null && currentThread.frameCount() > 0) {
                StackFrame frame = currentThread.frame(0);
                List<StackFrame> stack = currentThread.frames();
                listener.onDebuggerStopped(frame, stack);
                readProcessOutput();
            }
        } catch (IncompatibleThreadStateException e) {
            listener.onOutput("Error getting frame: " + e.getMessage() + "\n");
        }
    }

    // Lit la sortie du programme debugge
    private void readProcessOutput() {
        try {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(vm.process().getInputStream()));

            while (reader.ready()) {
                String line = reader.readLine();
                if (line != null) {
                    listener.onOutput("[Program] " + line + "\n");
                }
            }
        } catch (IOException e) {
            // ignore
        }
    }

    // Attend une commande de l'interface
    private void waitForCommand() {
        waitingForCommand = true;
        pendingCommand = null;

        while (waitingForCommand && running) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                break;
            }

            if (pendingCommand != null) {
                String cmd = pendingCommand;
                pendingCommand = null;
                processCommand(cmd);
            }
        }
    }

    // Execute une commande depuis l'interface
    public void executeCommand(String command) {
        pendingCommand = command;
    }

    // Traite une commande
    private void processCommand(String command) {
        switch (command) {
            case "step":
                doStep(StepRequest.STEP_INTO);
                break;
            case "step-over":
                doStep(StepRequest.STEP_OVER);
                break;
            case "continue":
                doContinue();
                break;
            default:
                listener.onOutput("Unknown command: " + command + "\n");
        }
    }

    // Execute un step
    private void doStep(int depth) {
        if (currentThread == null) return;

        // Supprime les anciens step requests
        for (StepRequest sr : vm.eventRequestManager().stepRequests()) {
            sr.disable();
            vm.eventRequestManager().deleteEventRequest(sr);
        }

        // Cree un nouveau step request
        StepRequest sr = vm.eventRequestManager().createStepRequest(
            currentThread, StepRequest.STEP_LINE, depth);
        sr.enable();

        waitingForCommand = false;
    }

    // Continue l'execution
    private void doContinue() {
        waitingForCommand = false;
    }

    // Ajoute un breakpoint
    public void addBreakpoint(String className, int line) {
        try {
            for (ReferenceType type : vm.allClasses()) {
                if (type.name().equals(className) || type.name().endsWith("." + className)) {
                    List<Location> locs = type.locationsOfLine(line);
                    if (!locs.isEmpty()) {
                        BreakpointRequest bp = vm.eventRequestManager().createBreakpointRequest(locs.get(0));
                        bp.enable();
                        breakpoints.add(bp);
                        listener.onOutput("Breakpoint added at " + className + ":" + line + "\n");
                        return;
                    }
                }
            }
            listener.onOutput("Could not set breakpoint at " + className + ":" + line + "\n");
        } catch (AbsentInformationException e) {
            listener.onOutput("No debug info for " + className + "\n");
        }
    }

    // Arrete le debugger
    public void stop() {
        running = false;
        waitingForCommand = false;
        if (vm != null) {
            try {
                vm.exit(0);
            } catch (Exception e) {
                // ignore
            }
        }
    }
}
