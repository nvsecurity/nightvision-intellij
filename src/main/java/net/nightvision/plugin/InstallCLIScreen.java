package net.nightvision.plugin;

import javax.swing.*;
import com.intellij.openapi.project.Project;
import net.nightvision.plugin.exceptions.CommandNotFoundException;
import net.nightvision.plugin.services.LoginService;
import net.nightvision.plugin.services.ProjectService;

import java.awt.*;

public class InstallCLIScreen extends Screen {
    private JButton installCLIButton;
    private JPanel installCLIPanel;

    public JPanel getLoginPanel() {
        return installCLIPanel;
    }

    public InstallCLIScreen(Project project) {
        super(project);

        installCLIButton.addActionListener(e -> {
            // TODO: Implement CLI installation
            mainWindowFactory.openLoginPage();
        });

        installCLIButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}
