package net.nightvision.plugin.intellij;

import com.intellij.openapi.project.Project;

public abstract class Screen {
    protected final MainWindowFactory mainWindow;
    protected final Project project;

    public Screen (Project project) {
        this.project = project;
        this.mainWindow = project.getService(MainWindowService.class).getWindowFactory();
    }
}
