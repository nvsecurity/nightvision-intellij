package net.nightvision.plugin;

import com.intellij.openapi.project.Project;
import net.nightvision.plugin.exceptions.CommandNotFoundException;
import net.nightvision.plugin.exceptions.NotLoggedException;
import net.nightvision.plugin.services.CommandRunnerService;
import net.nightvision.plugin.services.LoginService;
import net.nightvision.plugin.services.ProjectService;

import javax.swing.*;
import java.awt.*;

public class LoginScreen extends Screen {
    private JButton loginButton;
    private JPanel loginPanel;

    public JPanel getLoginPanel() {
        return loginPanel;
    }

    public LoginScreen(Project project) {
        super(project);

        loginButton.addActionListener(e -> {
            try {
                boolean success = LoginService.INSTANCE.login(project);
                if (success) {
                    ProjectService.INSTANCE.fetchCurrentProjectName();
                    mainWindowFactory.openOverviewPage();
                }
            } catch (CommandNotFoundException ex) {
                mainWindowFactory.openInstallCLIPage();
            }

        });
        loginButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}
