package net.nightvision.plugin.intellij.auth;

import com.intellij.openapi.project.Project;
import net.nightvision.plugin.intellij.Screen;
import net.nightvision.plugin.intellij.Utils;
import net.nightvision.plugin.intellij.services.AuthenticationService;

import javax.swing.*;

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
        backButton.setIcon(Utils.getIcon("/icons/back.svg", 1f));
        backButton.setBorder(null);

        cancelButton.addActionListener(e -> {
            mainWindowFactory.openAuthenticationsPage();
        });

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

            try {
                AuthenticationService.INSTANCE.createPlaywrightAuth(authName, authUrl, description);
                mainWindowFactory.openAuthenticationsPage();
                createButton.setEnabled(true);
            } catch(Exception exception) {
                errorMessage.setText(exception.getMessage());
                errorMessage.setVisible(true);
            }

        });
    }
}
