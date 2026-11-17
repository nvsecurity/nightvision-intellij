package net.nightvision.plugin.scans;

import javax.swing.JButton;

/** EDT-owned submission state, independent of resource refreshes. */
final class ScanSubmission {
    private final JButton button;
    private boolean targetsAvailable;
    private boolean starting;

    ScanSubmission(JButton button) {
        this.button = button;
        updateButton();
    }

    void setTargetsAvailable(boolean available) {
        targetsAvailable = available;
        updateButton();
    }

    boolean tryStart() {
        if (starting || !targetsAvailable) return false;
        starting = true;
        updateButton();
        return true;
    }

    void finish() {
        starting = false;
        updateButton();
    }

    private void updateButton() {
        button.setEnabled(targetsAvailable && !starting);
    }
}
