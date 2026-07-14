package net.nightvision.plugin;

import com.intellij.openapi.project.Project;
import com.intellij.util.ui.JBUI;

import javax.swing.*;

public abstract class Screen {
    protected final MainWindowFactory mainWindowFactory;
    protected final Project project;

    public Screen (Project project) {
        this.project = project;
        this.mainWindowFactory = project.getService(MainWindowService.class).getWindowFactory();
    }

    // pad is scaled, so the padding tracks the IDE's display scaling the way the
    // button's own text and icon do.
    protected static void addButtonPadding(JButton button, int pad) {
        button.setBorder(BorderFactory.createCompoundBorder(
                button.getBorder(),
                JBUI.Borders.empty(pad)
        ));
    }
}
