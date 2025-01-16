package net.nightvision.plugin.intellij.login;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import net.nightvision.plugin.intellij.MainWindowFactory;
import net.nightvision.plugin.intellij.MainWindowService;

import javax.swing.*;

public class LoginScreen {
    private JButton loginButton;
    private JPanel loginPanel;

    private final MainWindowFactory mainWindow;
    private final Project project;

    public JPanel getLoginPanel() {
        return loginPanel;
    }

    public LoginScreen(Project project) {
        this.mainWindow = project.getService(MainWindowService.class).getWindowFactory();
        this.project = project;

        this.loginButton.addActionListener(e -> {
            boolean success = LoginService.INSTANCE.login();
            if (success) {
                mainWindow.openScansPage();
            }
        });
    }
}
