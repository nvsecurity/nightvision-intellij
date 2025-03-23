package net.nightvision.plugin.intellij;

import com.intellij.openapi.project.Project;
import net.nightvision.plugin.intellij.services.AuthenticationService;

import javax.swing.*;

public class AuthenticationsCreateScreen extends Screen {
    private JButton backButton;

    public JPanel getAuthenticationsCreatePanel() {
        return authenticationsCreatePanel;
    }

    private JPanel authenticationsCreatePanel;
    private JTabbedPane tabbedPane1;
    private JTextField authenticationNameTextField;
    private JTextField authenticationDescriptionTextField;
    private JTextField authenticationURLTextField;
    private JLabel helpMessageLabel;
    private JButton cancelButton;
    private JButton createButton;
    private JLabel errorMessage;

    public AuthenticationsCreateScreen(Project project) {
        super(project);

        errorMessage.setVisible(false);
        backButton.addActionListener(e -> {
            mainWindow.openAuthenticationsPage();
        });
        backButton.setIcon(Utils.getIcon("/icons/back.svg", 1f));
        backButton.setBorder(null);

        cancelButton.addActionListener(e -> {
            mainWindow.openAuthenticationsPage();
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
            String authName = authenticationNameTextField.getText();
            String authUrl = authenticationURLTextField.getText();
            String description = authenticationDescriptionTextField.getText();

            try {
                AuthenticationService.INSTANCE.createPlaywrightAuth(authName, authUrl, description);
                mainWindow.openAuthenticationsPage();
            } catch(Exception exception) {
                errorMessage.setText(exception.getMessage());
                errorMessage.setVisible(true);
            }

        });
    }
}
