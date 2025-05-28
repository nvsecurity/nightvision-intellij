package net.nightvision.plugin;

import javax.swing.*;
import com.intellij.openapi.project.Project;
import net.nightvision.plugin.exceptions.CommandNotFoundException;
import net.nightvision.plugin.services.InstallCLIService;
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
            try {
                InstallCLIService.INSTANCE.installCLI(false);
                mainWindowFactory.openLoginPage();
            } catch (Exception ex) {
                // TODO: Show error message here
                ex.printStackTrace();
            }
        });

        installCLIButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}
