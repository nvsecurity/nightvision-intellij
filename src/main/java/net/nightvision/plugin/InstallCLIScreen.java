package net.nightvision.plugin;

import javax.swing.*;
import com.intellij.openapi.project.Project;
import net.nightvision.plugin.scans.ScansScreen;
import net.nightvision.plugin.services.CommandRunnerService;
import net.nightvision.plugin.services.InstallCLIService;
import net.nightvision.plugin.services.ScanService;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class InstallCLIScreen extends Screen {
    private JButton installCLIButton;
    private JPanel installCLIPanel;
    private JLabel errorMessageLabel;

    public JPanel getLoginPanel() {
        return installCLIPanel;
    }

    public InstallCLIScreen(Project project) {
        super(project);

        errorMessageLabel.setVisible(false);

        installCLIButton.addActionListener(e -> {
            errorMessageLabel.setVisible(false);
            errorMessageLabel.setText("");
            installCLIButton.setText("Installing...");
            installCLIButton.setEnabled(false);
            new InstallCLIWorker().execute();
        });

        installCLIButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private class InstallCLIWorker extends SwingWorker<Void, Void> {
        @Override
        protected Void doInBackground() throws Exception {
            InstallCLIService.INSTANCE.installCLI(false);
            return null;
        }

        @Override
        protected void done() {
            try {
                get();
                String installedDir = CommandRunnerService.INSTANCE.getDestinationDirForPlatform();
                new InfoDialog("<html><body>NightVision CLI is installed under the path:<br>-> "
                        + installedDir
                        + "<br>Consider adding it to your path if you would like to execute it in your terminal.</body></html>",
                        e -> mainWindowFactory.openLoginPage())
                .setVisible(true);
            } catch (Exception ex) {
                errorMessageLabel.setText(ex.toString());
                errorMessageLabel.setVisible(true);
                installCLIButton.setEnabled(true);
                installCLIButton.setText("Install CLI");
            }
        }
    }
}
