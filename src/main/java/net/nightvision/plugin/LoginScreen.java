package net.nightvision.plugin;

import com.intellij.execution.process.ProcessNotCreatedException;
import com.intellij.openapi.project.Project;
import net.nightvision.plugin.exceptions.CommandNotFoundException;
import net.nightvision.plugin.services.CommandRunnerService;
import net.nightvision.plugin.services.InstallCLIService;
import net.nightvision.plugin.services.LoginService;
import net.nightvision.plugin.services.ProjectService;

import javax.swing.*;
import java.awt.*;

public class LoginScreen extends Screen {
    private JButton loginButton;
    private JPanel loginPanel;
    private JButton updateCLIButton;
    private JLabel errorMessageLabel;

    public JPanel getLoginPanel() {
        return loginPanel;
    }

    public LoginScreen(Project project) {
        super(project);

        errorMessageLabel.setVisible(false);

        String cliVersion = CommandRunnerService.INSTANCE.getCLIVersion();
        boolean shouldUpdateCLI = InstallCLIService.INSTANCE.shouldUpdateCLI(cliVersion);
        if (shouldUpdateCLI) {
            updateCLIButton.addActionListener(e -> {
                errorMessageLabel.setVisible(false);
                errorMessageLabel.setText("");
                updateCLIButton.setText("Updating...");
                updateCLIButton.setEnabled(false);
                new UpdateCLIWorker().execute();
            });
            updateCLIButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            updateCLIButton.setVisible(false);
        }

        loginButton.addActionListener(e -> {
            try {
                boolean success = LoginService.INSTANCE.login(project);
                if (success) {
                    ProjectService.INSTANCE.fetchCurrentProjectName();
                    mainWindowFactory.openOverviewPage();
                }
            } catch (ProcessNotCreatedException | CommandNotFoundException ex) {
                mainWindowFactory.openInstallCLIPage();
            }
        });
        loginButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private class UpdateCLIWorker extends SwingWorker<Void, Void> {

        @Override
        protected Void doInBackground() throws Exception {
            InstallCLIService.INSTANCE.installCLI(true);
            return null;
        }

        @Override
        protected void done() {
            try {
                get();
                updateCLIButton.setVisible(false);
            } catch (Exception ex) {
                errorMessageLabel.setText(ex.toString());
                errorMessageLabel.setVisible(true);
                updateCLIButton.setEnabled(true);
                updateCLIButton.setText("Update CLI");
            }
        }
    }
}
