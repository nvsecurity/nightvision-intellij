package net.nightvision.plugin.intellij;

import com.intellij.openapi.project.Project;

public abstract class Screen {
    protected final MainWindowFactory mainWindow;
    protected final Project project;

    public Screen (MainWindowFactory mainWindow, Project project) {
        this.mainWindow = mainWindow;
        this.project = project;
    }
}
