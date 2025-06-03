package net.nightvision.plugin;

import javax.swing.*;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import net.nightvision.plugin.services.CommandRunnerService;
import net.nightvision.plugin.services.InstallCLIService;
import net.nightvision.plugin.utils.IconUtils;

import java.awt.*;

import static javax.swing.SwingConstants.CENTER;

public class OverviewScreen extends Screen {
    private static boolean isExtraOptionsVisible = false;
    private JPanel overviewPanel;
//    private JPanel extraButtonsPanel;
    private JButton apiDiscoveryButton;
    private JButton apiAndWebSecurityButton;
    private JPanel extraOptionsPanel;
    private JButton scansButton;
    private JButton targetsButton;
    private JButton authenticationsButton;
    private JButton projectsButton;
    private JButton updateCLIButton;
    private JLabel errorMessageLabel;

    public JPanel getOverviewPanel() {
        return overviewPanel;
    }

    private void setExtraOptionsActivatedTheme() {
        if (extraOptionsPanel.isVisible()) {
            apiAndWebSecurityButton.setBackground(JBColor.CYAN);
        } else {
            apiAndWebSecurityButton.setBackground(JBColor.WHITE);
        }

    }

    public OverviewScreen(Project project) {
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

        extraOptionsPanel.setVisible(isExtraOptionsVisible);
        setExtraOptionsActivatedTheme();

        apiDiscoveryButton.setIcon(IconUtils.getIcon("/icons/api-discovery.svg", 1f));
        apiDiscoveryButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        apiDiscoveryButton.setHorizontalTextPosition(CENTER);
        apiDiscoveryButton.addActionListener(e ->  mainWindowFactory.openApiDiscoveryPage());
        apiDiscoveryButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        apiAndWebSecurityButton.setIcon(IconUtils.getIcon("/icons/dast.svg", 1f));
        apiAndWebSecurityButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        apiAndWebSecurityButton.setHorizontalTextPosition(CENTER);
        apiAndWebSecurityButton.addActionListener(e -> {
            isExtraOptionsVisible = !isExtraOptionsVisible;
            extraOptionsPanel.setVisible(isExtraOptionsVisible);
            setExtraOptionsActivatedTheme();
        });
        apiAndWebSecurityButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        scansButton.setIcon(IconUtils.getIcon("/icons/scans.svg", 1f));
        scansButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        scansButton.setHorizontalTextPosition(CENTER);
        scansButton.addActionListener(e ->  mainWindowFactory.openScansPage());
        scansButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        targetsButton.setIcon(IconUtils.getIcon("/icons/targets.svg", 1f));
        targetsButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        targetsButton.setHorizontalTextPosition(CENTER);
        targetsButton.addActionListener(e -> mainWindowFactory.openTargetsPage());
        targetsButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        authenticationsButton.setIcon(IconUtils.getIcon("/icons/authentications.svg", 1f));
        authenticationsButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        authenticationsButton.setHorizontalTextPosition(CENTER);
        authenticationsButton.addActionListener(e -> mainWindowFactory.openAuthenticationsPage());
        authenticationsButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        projectsButton.setIcon(IconUtils.getIcon("/icons/projects.svg", 1f));
        projectsButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        projectsButton.setHorizontalTextPosition(CENTER);
        projectsButton.addActionListener(e -> mainWindowFactory.openProjectsPage());
        projectsButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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
