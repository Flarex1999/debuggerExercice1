package gui;

import com.sun.jdi.StackFrame;

import javax.swing.*;
import java.awt.*;
import java.util.List;

// Panneau qui affiche la pile d'appels (call stack)
public class CallStackPanel extends JPanel {

    private JList<String> stackList;
    private DefaultListModel<String> listModel;
    private List<StackFrame> frames;
    private DebuggerGUI debuggerGUI;

    public CallStackPanel(DebuggerGUI gui) {
        this.debuggerGUI = gui;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("CALL STACK"));

        listModel = new DefaultListModel<>();
        stackList = new JList<>(listModel);
        stackList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        stackList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Clic sur une frame = change la frame selectionnee
        stackList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && frames != null) {
                int index = stackList.getSelectedIndex();
                if (index >= 0 && index < frames.size()) {
                    debuggerGUI.onFrameSelected(frames.get(index));
                }
            }
        });

        add(new JScrollPane(stackList), BorderLayout.CENTER);
        setPreferredSize(new Dimension(250, 150));
    }

    // Met a jour l'affichage avec la nouvelle pile
    public void updateStack(List<StackFrame> stackFrames) {
        this.frames = stackFrames;
        listModel.clear();

        if (stackFrames == null || stackFrames.isEmpty()) {
            listModel.addElement("(empty)");
            return;
        }

        for (int i = 0; i < stackFrames.size(); i++) {
            StackFrame frame = stackFrames.get(i);
            String entry = String.format("[%d] %s.%s:%d",
                    i,
                    frame.location().declaringType().name(),
                    frame.location().method().name(),
                    frame.location().lineNumber()
            );
            listModel.addElement(entry);
        }

        // Selectionne la premiere frame par defaut
        if (!listModel.isEmpty()) {
            stackList.setSelectedIndex(0);
        }
    }

    public void clear() {
        listModel.clear();
        frames = null;
    }
}
