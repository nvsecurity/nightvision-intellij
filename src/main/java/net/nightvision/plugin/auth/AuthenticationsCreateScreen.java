package net.nightvision.plugin.auth;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import net.nightvision.plugin.Screen;
import net.nightvision.plugin.exceptions.CommandNotFoundException;
import net.nightvision.plugin.exceptions.NotLoggedException;
import net.nightvision.plugin.exceptions.PermissionDeniedException;
import net.nightvision.plugin.utils.IconUtils;
import net.nightvision.plugin.services.AuthenticationService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class AuthenticationsCreateScreen extends Screen {
    private JButton backButton;
    private JPanel authenticationsCreatePanel;
    private JTabbedPane tabbedPane1;
    private JTextField authenticationNameTextField;
    private JTextField authenticationDescriptionTextField;
    private JTextField authenticationURLTextField;
    private JLabel helpMessageLabel;
    private JButton cancelButton;
    private JButton createButton;
    private JLabel errorMessage;

    public JPanel getAuthenticationsCreatePanel() {
        return authenticationsCreatePanel;
    }

    public AuthenticationsCreateScreen(Project project) {
        super(project);

        errorMessage.setVisible(false);
        backButton.addActionListener(e -> {
            mainWindowFactory.openAuthenticationsPage();
        });
        backButton.setIcon(IconUtils.getIcon("/icons/back.svg", 1f));
        backButton.setBorder(null);
        backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // TODO: Implement for Cookie and Header tabs!
        tabbedPane1.remove(0); // Removing Cookie
        tabbedPane1.remove(0); // Removing Header

        cancelButton.addActionListener(e -> {
            mainWindowFactory.openAuthenticationsPage();
        });
        cancelButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        helpMessageLabel.setText("""
               <html><body>
               <p style="width:350px">
               Will launch an incognito Chrome Window to record the login process. <b>Instructions</b>:<br>
               • Follow the steps to log into the application (type in the username and password)<br>
               • When you are done, exit the Chrome window. This will upload the login sequence to NightVision Cloud.
               </p>
               </body></html>
               """);

        createButton.addActionListener(e -> {
            errorMessage.setVisible(false);
            errorMessage.setText("");
            createButton.setEnabled(false);
            String authName = authenticationNameTextField.getText();
            String authUrl = authenticationURLTextField.getText();
            String description = authenticationDescriptionTextField.getText();

            new Task.Backgroundable(project, "Create Authentication Playwright", false) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    try {
                        AuthenticationService.INSTANCE.createPlaywrightAuth(authName, authUrl, description);

                        ApplicationManager.getApplication().invokeLater(() -> {
                            mainWindowFactory.openAuthenticationsPage();
                        });
                    } catch (CommandNotFoundException ex) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            mainWindowFactory.openInstallCLIPage();
                        });
                    } catch (NotLoggedException ex) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            mainWindowFactory.openLoginPage();
                        });
                        return;
                    } catch (Exception exception) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            errorMessage.setText(exception.getMessage());
                            errorMessage.setVisible(true);
                        });
                    } finally {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            createButton.setEnabled(true);
                        });
                    }
                }
            }.queue();
        });
        createButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}
