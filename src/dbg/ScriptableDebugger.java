package dbg;

import com.sun.jdi.*;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.connect.VMStartException;
import com.sun.jdi.event.*;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.MethodEntryRequest;
import com.sun.jdi.request.StepRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ScriptableDebugger {

    private Class debugClass;
    private VirtualMachine vm;
    private Event currentEvent;
    private CommandRegistry commandRegistry;
    private boolean shouldResume;
    private List<BreakpointInfo> breakpoints;  // Liste des breakpoints poses
    private BufferedReader inputReader;  // Lecteur d'entree unique

    public VirtualMachine connectAndLaunchVM() throws IOException, IllegalConnectorArgumentsException, VMStartException {
        LaunchingConnector launchingConnector = Bootstrap.virtualMachineManager().defaultConnector();
        Map<String, Connector.Argument> arguments = launchingConnector.defaultArguments();
        arguments.get("main").setValue(debugClass.getName());

        // Configure le classpath (IMPORTANT: sinon la VM ne trouve pas la classe)
        String classpath = System.getProperty("java.class.path");
        arguments.get("options").setValue("-cp " + classpath);

        VirtualMachine vm = launchingConnector.launch(arguments);
        return vm;
    }

    public void attachTo(Class debuggeeClass) {
        this.debugClass = debuggeeClass;
        this.breakpoints = new ArrayList<>();
        this.inputReader = new BufferedReader(new InputStreamReader(System.in));
        initializeCommands();

        try {
            vm = connectAndLaunchVM();
            enableClassPrepareRequest(vm);
            startDebugger();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalConnectorArgumentsException e) {
            e.printStackTrace();
        } catch (VMStartException e) {
            e.printStackTrace();
            System.out.println(e.toString());
        } catch (VMDisconnectedException e) {
            System.out.println("Virtual Machine is disconnected: " + e.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startDebugger() throws VMDisconnectedException, InterruptedException, AbsentInformationException {
        EventSet eventSet = null;
        while ((eventSet = vm.eventQueue().remove()) != null) {
            for (Event event : eventSet) {
                currentEvent = event;
                System.out.println(event.toString());

                if (event instanceof ClassPrepareEvent) {
                    setBreakPoint(debugClass.getName(), 6);
                }

                if (event instanceof BreakpointEvent) {
                    BreakpointEvent bpEvent = (BreakpointEvent) event;

                    // Gestion des breakpoints speciaux (once, count)
                    if (!handleSpecialBreakpoint(bpEvent)) {
                        vm.resume();
                        continue;
                    }

                    shouldResume = false;
                    while (!shouldResume) {
                        readCommand();
                    }
                }

                if (event instanceof StepEvent) {
                    StepRequest stepRequest = vm.eventRequestManager().stepRequests().get(0);
                    stepRequest.disable();

                    shouldResume = false;
                    while (!shouldResume) {
                        readCommand();
                    }
                }

                if (event instanceof MethodEntryEvent) {
                    shouldResume = false;
                    while (!shouldResume) {
                        readCommand();
                    }
                }

                if (event instanceof VMDisconnectEvent) {
                    System.out.println("End of program");

                    InputStreamReader reader = new InputStreamReader(vm.process().getInputStream());
                    OutputStreamWriter writer = new OutputStreamWriter(System.out);

                    try {
                        reader.transferTo(writer);
                        writer.flush();
                    } catch (IOException e) {
                        System.out.println("Target VM input stream reading error.");
                    }

                    return;
                }

                vm.resume();
            }
        }
    }

    // Gere les breakpoints once et count, retourne true si on doit s'arreter
    private boolean handleSpecialBreakpoint(BreakpointEvent event) {
        for (Iterator<BreakpointInfo> it = breakpoints.iterator(); it.hasNext(); ) {
            BreakpointInfo bp = it.next();

            if (bp.getRequest().equals(event.request())) {
                bp.incrementCount();

                // Breakpoint avec compteur
                if (!bp.shouldActivate()) {
                    return false;  // Ne pas s'arreter
                }

                // Breakpoint once - on le supprime
                if (bp.isOnce()) {
                    bp.getRequest().disable();
                    vm.eventRequestManager().deleteEventRequest(bp.getRequest());
                    it.remove();
                }

                return true;
            }
        }

        return true;  // Breakpoint normal
    }

    public void enableClassPrepareRequest(VirtualMachine vm) {
        ClassPrepareRequest classPrepareRequest =
                vm.eventRequestManager().createClassPrepareRequest();
        classPrepareRequest.addClassFilter(debugClass.getName());
        classPrepareRequest.enable();
    }

    public void setBreakPoint(String className, int lineNumber) throws AbsentInformationException {
        for (ReferenceType targetClass : vm.allClasses()) {
            if (targetClass.name().equals(className)) {
                Location location = targetClass.locationsOfLine(lineNumber).get(0);
                BreakpointRequest bpReq =
                        vm.eventRequestManager().createBreakpointRequest(location);
                bpReq.enable();
            }
        }
    }

    public void enableStepRequest(LocatableEvent event, int stepType) {
        StepRequest stepRequest =
                vm.eventRequestManager().createStepRequest(
                        event.thread(),
                        StepRequest.STEP_LINE,  // LINE pour stepper ligne par ligne
                        stepType
                );
        stepRequest.enable();
    }

    public void step() {
        stepWithType(StepRequest.STEP_INTO);
        shouldResume = true;
    }

    public void stepOver() {
        stepWithType(StepRequest.STEP_OVER);
        shouldResume = true;
    }

    private void stepWithType(int stepType) {
        if (currentEvent != null && currentEvent instanceof LocatableEvent) {
            if (vm.eventRequestManager().stepRequests().isEmpty()) {
                enableStepRequest((LocatableEvent) currentEvent, stepType);
            } else {
                StepRequest stepRequest = vm.eventRequestManager().stepRequests().get(0);
                stepRequest.disable();
                vm.eventRequestManager().deleteEventRequest(stepRequest);
                enableStepRequest((LocatableEvent) currentEvent, stepType);
            }
        }
    }

    public void readCommand() {
        System.out.println("\n>>> Enter command: ");

        try {
            String input = inputReader.readLine();

            // Gere la fin d'entree (pipe vide)
            if (input == null) {
                System.out.println(">>> End of input, continuing...");
                continueExecution();
                return;
            }

            input = input.trim();
            if (input.isEmpty()) {
                return;
            }

            if (commandRegistry.hasCommand(input)) {
                Command command = commandRegistry.getCommand(input);
                Object result = command.execute();

                if (result != null) {
                    System.out.println(result);
                }
            } else {
                System.out.println(">>> Unknown command: " + input);
            }

        } catch (IOException e) {
            System.out.println("Error reading command.");
        }
    }

    private void initializeCommands() {
        commandRegistry = new CommandRegistry();

        // Commandes de navigation
        commandRegistry.register("step", new StepCommand(this));
        commandRegistry.register("step-over", new StepOverCommand(this));
        commandRegistry.register("continue", new ContinueCommand(this));

        // Commandes d'inspection
        commandRegistry.register("frame", new FrameCommand(this));
        commandRegistry.register("method", new MethodCommand(this));
        commandRegistry.register("stack", new StackCommand(this));
        commandRegistry.register("temporaries", new TemporariesCommand(this));
        commandRegistry.register("arguments", new ArgumentsCommand(this));
        commandRegistry.register("receiver", new ReceiverCommand(this));
        commandRegistry.register("sender", new SenderCommand(this));
        commandRegistry.register("receiver-variables", new ReceiverVariablesCommand(this));
        commandRegistry.register("breakpoints", new BreakpointsCommand(this));

        // Commandes avec parametres
        ScriptableDebugger self = this;

        // print-var <varName>
        commandRegistry.registerFactory("print-var", args -> {
            if (args.length < 1) {
                return () -> "Usage: print-var <varName>";
            }
            return new PrintVarCommand(self, args[0]);
        });

        // break <filename> <line>
        commandRegistry.registerFactory("break", args -> {
            if (args.length < 2) {
                return () -> "Usage: break <filename> <line>";
            }
            try {
                int line = Integer.parseInt(args[1]);
                return new BreakCommand(self, args[0], line);
            } catch (NumberFormatException e) {
                return () -> "Invalid line number";
            }
        });

        // break-once <filename> <line>
        commandRegistry.registerFactory("break-once", args -> {
            if (args.length < 2) {
                return () -> "Usage: break-once <filename> <line>";
            }
            try {
                int line = Integer.parseInt(args[1]);
                return new BreakOnceCommand(self, args[0], line);
            } catch (NumberFormatException e) {
                return () -> "Invalid line number";
            }
        });

        // break-on-count <filename> <line> <count>
        commandRegistry.registerFactory("break-on-count", args -> {
            if (args.length < 3) {
                return () -> "Usage: break-on-count <filename> <line> <count>";
            }
            try {
                int line = Integer.parseInt(args[1]);
                int count = Integer.parseInt(args[2]);
                return new BreakOnCountCommand(self, args[0], line, count);
            } catch (NumberFormatException e) {
                return () -> "Invalid number";
            }
        });

        // break-before-method-call <methodName>
        commandRegistry.registerFactory("break-before-method-call", args -> {
            if (args.length < 1) {
                return () -> "Usage: break-before-method-call <methodName>";
            }
            return new BreakBeforeMethodCallCommand(self, args[0]);
        });
    }

    public void continueExecution() {
        if (!vm.eventRequestManager().stepRequests().isEmpty()) {
            for (Object obj : vm.eventRequestManager().stepRequests()) {
                StepRequest stepRequest = (StepRequest) obj;
                stepRequest.disable();
            }
        }
        System.out.println(">>> Continuing...");
        shouldResume = true;
    }

    public StackFrame getCurrentFrame() throws IncompatibleThreadStateException {
        if (currentEvent != null && currentEvent instanceof LocatableEvent) {
            LocatableEvent locEvent = (LocatableEvent) currentEvent;
            if (locEvent.thread().frameCount() > 0) {
                return locEvent.thread().frame(0);
            }
        }
        return null;
    }

    public List<StackFrame> getStackFrames() throws IncompatibleThreadStateException {
        if (currentEvent != null && currentEvent instanceof LocatableEvent) {
            LocatableEvent locEvent = (LocatableEvent) currentEvent;
            return locEvent.thread().frames();
        }
        return null;
    }

    // Retourne la liste des breakpoints
    public List<BreakpointInfo> getBreakpoints() {
        return breakpoints;
    }

    // Ajoute un breakpoint simple
    public boolean addBreakpoint(String filename, int lineNumber) {
        try {
            for (ReferenceType targetClass : vm.allClasses()) {
                if (targetClass.name().equals(filename) ||
                    targetClass.name().endsWith("." + filename)) {

                    List<Location> locations = targetClass.locationsOfLine(lineNumber);
                    if (!locations.isEmpty()) {
                        Location location = locations.get(0);
                        BreakpointRequest bpReq =
                            vm.eventRequestManager().createBreakpointRequest(location);
                        bpReq.enable();

                        BreakpointInfo info = new BreakpointInfo(filename, lineNumber, bpReq);
                        breakpoints.add(info);
                        return true;
                    }
                }
            }
        } catch (AbsentInformationException e) {
            System.out.println("No debug info for " + filename);
        }
        return false;
    }

    // Ajoute un breakpoint qui se supprime apres 1 passage
    public boolean addBreakpointOnce(String filename, int lineNumber) {
        try {
            for (ReferenceType targetClass : vm.allClasses()) {
                if (targetClass.name().equals(filename) ||
                    targetClass.name().endsWith("." + filename)) {

                    List<Location> locations = targetClass.locationsOfLine(lineNumber);
                    if (!locations.isEmpty()) {
                        Location location = locations.get(0);
                        BreakpointRequest bpReq =
                            vm.eventRequestManager().createBreakpointRequest(location);
                        bpReq.enable();

                        BreakpointInfo info = new BreakpointInfo(filename, lineNumber, bpReq);
                        info.setOnce(true);
                        breakpoints.add(info);
                        return true;
                    }
                }
            }
        } catch (AbsentInformationException e) {
            System.out.println("No debug info for " + filename);
        }
        return false;
    }

    // Ajoute un breakpoint qui s'active apres N passages
    public boolean addBreakpointOnCount(String filename, int lineNumber, int count) {
        try {
            for (ReferenceType targetClass : vm.allClasses()) {
                if (targetClass.name().equals(filename) ||
                    targetClass.name().endsWith("." + filename)) {

                    List<Location> locations = targetClass.locationsOfLine(lineNumber);
                    if (!locations.isEmpty()) {
                        Location location = locations.get(0);
                        BreakpointRequest bpReq =
                            vm.eventRequestManager().createBreakpointRequest(location);
                        bpReq.enable();

                        BreakpointInfo info = new BreakpointInfo(filename, lineNumber, bpReq);
                        info.setTargetCount(count);
                        breakpoints.add(info);
                        return true;
                    }
                }
            }
        } catch (AbsentInformationException e) {
            System.out.println("No debug info for " + filename);
        }
        return false;
    }

    // Ajoute un breakpoint sur une methode
    public boolean addMethodBreakpoint(String methodName) {
        MethodEntryRequest req = vm.eventRequestManager().createMethodEntryRequest();
        req.addClassFilter(debugClass.getName());
        req.enable();

        // On stocke l'info (pas de ligne specifique)
        BreakpointInfo info = new BreakpointInfo(methodName, -1, null);
        breakpoints.add(info);

        System.out.println("Method entry breakpoint set for: " + methodName);
        return true;
    }
}
