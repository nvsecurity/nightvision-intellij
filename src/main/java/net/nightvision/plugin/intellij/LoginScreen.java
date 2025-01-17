package net.nightvision.plugin.intellij;

import com.intellij.openapi.project.Project;
import net.nightvision.plugin.intellij.services.LoginService;

import javax.swing.*;
import java.awt.*;

public class LoginScreen extends Screen {
    private JButton loginButton;
    private JPanel loginPanel;

    public JPanel getLoginPanel() {
        return loginPanel;
    }

    public LoginScreen(Project project) {
        super(project.getService(MainWindowService.class).getWindowFactory(), project);

        loginPanel.setLayout(new BoxLayout(loginPanel, BoxLayout.Y_AXIS));
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, loginButton.getPreferredSize().height));
        loginButton.addActionListener(e -> {
            boolean success = LoginService.INSTANCE.login();
            if (success) {
                mainWindow.openOverviewPage();
            }
        });
    }
}
