package dbg;

import com.sun.jdi.request.BreakpointRequest;

// Stocke les infos d'un breakpoint pour le suivi
public class BreakpointInfo {

    private String filename;
    private int lineNumber;
    private BreakpointRequest request;
    private boolean isOnce;       // Se supprime apres 1 passage
    private int targetCount;      // Nombre de passages avant activation
    private int currentCount;     // Compteur actuel

    public BreakpointInfo(String filename, int lineNumber, BreakpointRequest request) {
        this.filename = filename;
        this.lineNumber = lineNumber;
        this.request = request;
        this.isOnce = false;
        this.targetCount = 0;
        this.currentCount = 0;
    }

    public String getFilename() {
        return filename;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public BreakpointRequest getRequest() {
        return request;
    }

    public boolean isOnce() {
        return isOnce;
    }

    public void setOnce(boolean once) {
        this.isOnce = once;
    }

    public int getTargetCount() {
        return targetCount;
    }

    public void setTargetCount(int count) {
        this.targetCount = count;
    }

    public int getCurrentCount() {
        return currentCount;
    }

    public void incrementCount() {
        this.currentCount++;
    }

    // Retourne true si le breakpoint doit s'activer
    public boolean shouldActivate() {
        if (targetCount > 0) {
            return currentCount >= targetCount;
        }
        return true;
    }

    @Override
    public String toString() {
        String info = filename + ":" + lineNumber;
        if (isOnce) {
            info += " (once)";
        }
        if (targetCount > 0) {
            info += " (count: " + currentCount + "/" + targetCount + ")";
        }
        return info;
    }
}
