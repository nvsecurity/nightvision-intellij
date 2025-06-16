package net.nightvision.plugin;

import com.intellij.execution.process.ProcessNotCreatedException;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import net.nightvision.plugin.exceptions.CommandNotFoundException;
import net.nightvision.plugin.exceptions.NotLoggedException;
import net.nightvision.plugin.services.CommandRunnerService;
import net.nightvision.plugin.services.InstallCLIService;
import net.nightvision.plugin.services.LoginService;
import net.nightvision.plugin.services.ProjectService;
import org.jetbrains.annotations.NotNull;

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

    private void checkShouldUpdateCli() {
        new Task.Backgroundable(project, "Checking CLI Version", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    String cliVersion = CommandRunnerService.INSTANCE.getCLIVersion();
                    boolean shouldUpdateCLI = InstallCLIService.INSTANCE.shouldUpdateCLI(cliVersion);

                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (shouldUpdateCLI) {
                            setupUpdateButton();
                        } else {
                            updateCLIButton.setVisible(false);
                        }
                    });
                } catch (ProcessNotCreatedException ex) {
                    // Ignore as it should be handled by checking authenticated part.
                }
            }
        }.queue();
    }

    public LoginScreen(Project project) {
        super(project);

        loginButton.setEnabled(false);
        errorMessageLabel.setVisible(false);
        updateCLIButton.setVisible(false);

        new Task.Backgroundable(project, "Checking if it is authenticated", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    boolean isBypass = LoginService.INSTANCE.bypassLoginStepIfAuthenticatedAlready(project);
                    if (isBypass) {
                        ProjectService.INSTANCE.fetchCurrentProjectName();
                        ApplicationManager.getApplication().invokeLater(() -> {
                            mainWindowFactory.openOverviewPage();
                        });
                    }
                    checkShouldUpdateCli();
                } catch (CommandNotFoundException | ProcessNotCreatedException ex) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        mainWindowFactory.openInstallCLIPage();
                    });
                    return;
                } catch (NotLoggedException ex) {
                    loginButton.setEnabled(true);
                    checkShouldUpdateCli();
                }
            }
        }.queue();

        loginButton.addActionListener(e -> {
            new Task.Backgroundable(project, "Fetching current project name", false) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    try {
                        boolean success = LoginService.INSTANCE.login(project);
                        if (success) {
                            ProjectService.INSTANCE.fetchCurrentProjectName();
                            ApplicationManager.getApplication().invokeLater(() -> {
                                mainWindowFactory.openOverviewPage();
                            });
                        }
                    } catch (ProcessNotCreatedException | CommandNotFoundException ex) {
                        mainWindowFactory.openInstallCLIPage();
                    }
                }
            }.queue();

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

    private void setupUpdateButton() {
        updateCLIButton.setVisible(true);
        updateCLIButton.addActionListener(e -> {
            errorMessageLabel.setVisible(false);
            errorMessageLabel.setText("");
            updateCLIButton.setText("Updating...");
            updateCLIButton.setEnabled(false);
            new UpdateCLIWorker().execute();
        });
        updateCLIButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}
