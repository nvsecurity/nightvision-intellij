package net.nightvision.plugin;

import com.intellij.openapi.project.Project;
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

        loginPanel.setLayout(new BoxLayout(loginPanel, BoxLayout.Y_AXIS));
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, loginButton.getPreferredSize().height));
        loginButton.addActionListener(e -> {
            boolean success = LoginService.INSTANCE.login();
            if (success) {
                ProjectService.INSTANCE.fetchCurrentProjectName();
                mainWindowFactory.openOverviewPage();
            }
        });
        loginButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}
