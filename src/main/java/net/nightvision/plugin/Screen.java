package net.nightvision.plugin;

import com.intellij.openapi.project.Project;

public abstract class Screen {
    protected final MainWindowFactory mainWindowFactory;
    protected final Project project;

    public Screen (Project project) {
        this.project = project;
        this.mainWindowFactory = project.getService(MainWindowService.class).getWindowFactory();
    }
}
