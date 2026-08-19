package net.nightvision.plugin;

import javax.swing.*;

import com.intellij.execution.process.ProcessNotCreatedException;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import net.nightvision.plugin.services.CommandRunnerService;
import net.nightvision.plugin.services.InstallCLIService;
import net.nightvision.plugin.services.ProjectService;
import net.nightvision.plugin.utils.IconUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

import com.intellij.util.ui.JBUI;
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
    private JLabel updateMessageLabel;
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

    private void setupUpdateButton(String cliVersion, String cliPath) {
        updateCLIButton.setVisible(true);
        updateCLIButton.setToolTipText(
                cliUpdateTooltip(cliVersion, cliPath, Constants.CLI_VERSION));
        updateMessageLabel.setText(
                cliUpdateMessage(cliVersion, cliPath, Constants.CLI_VERSION));
        updateMessageLabel.setVisible(true);
        updateCLIButton.addActionListener(e -> {
            errorMessageLabel.setVisible(false);
            errorMessageLabel.setText("");
            updateCLIButton.setText("Updating...");
            updateCLIButton.setEnabled(false);
            new UpdateCLIWorker().execute();
        });
        updateCLIButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public OverviewScreen(Project project) {
        super(project);

        overviewPanel.setBorder(JBUI.Borders.empty(8));

        updateCLIButton.setVisible(false);
        updateMessageLabel.setForeground(JBColor.RED);
        updateMessageLabel.setVisible(false);
        errorMessageLabel.setVisible(false);

        new Task.Backgroundable(project, "Checking CLI Version", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    String cliVersion = CommandRunnerService.INSTANCE.getCLIVersion();
                    boolean shouldUpdateCLI = InstallCLIService.INSTANCE.shouldUpdateCLI(cliVersion);
                    // Resolved here rather than on the EDT: it walks PATH and
                    // stats each candidate.
                    String cliPath = shouldUpdateCLI ? CommandRunnerService.INSTANCE.resolveCliPath() : null;

                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (shouldUpdateCLI) {
                            setupUpdateButton(cliVersion, cliPath);
                        } else {
                            updateCLIButton.setVisible(false);
                            updateMessageLabel.setVisible(false);
                        }
                    });
                } catch (ProcessNotCreatedException ex) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        mainWindowFactory.openInstallCLIPage();
                    });
                }
            }
        }.queue();

        extraOptionsPanel.setVisible(isExtraOptionsVisible);
        setExtraOptionsActivatedTheme();

        apiDiscoveryButton.setIcon(IconUtils.getIcon("/icons/api-discovery.svg", 1f));
        apiDiscoveryButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        apiDiscoveryButton.setHorizontalTextPosition(CENTER);
        addButtonPadding(apiDiscoveryButton, 8);
        apiDiscoveryButton.addActionListener(e ->  mainWindowFactory.openApiDiscoveryPage());
        apiDiscoveryButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        apiAndWebSecurityButton.setIcon(IconUtils.getIcon("/icons/dast.svg", 1f));
        apiAndWebSecurityButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        apiAndWebSecurityButton.setHorizontalTextPosition(CENTER);
        addButtonPadding(apiAndWebSecurityButton, 8);
        apiAndWebSecurityButton.addActionListener(e -> {
            isExtraOptionsVisible = !isExtraOptionsVisible;
            extraOptionsPanel.setVisible(isExtraOptionsVisible);
            setExtraOptionsActivatedTheme();
        });
        apiAndWebSecurityButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        scansButton.setIcon(IconUtils.getIcon("/icons/scans.svg", 1f));
        scansButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        scansButton.setHorizontalTextPosition(CENTER);
        addButtonPadding(scansButton, 8);
        scansButton.addActionListener(e -> {
            if (ProjectService.INSTANCE.getCurrentProjectName().isEmpty()) {
                mainWindowFactory.openProjectsPage();
            } else {
                mainWindowFactory.openScansPage();
            }
        });
        scansButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        targetsButton.setIcon(IconUtils.getIcon("/icons/targets.svg", 1f));
        targetsButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        targetsButton.setHorizontalTextPosition(CENTER);
        addButtonPadding(targetsButton, 8);
        targetsButton.addActionListener(e -> {
            if (ProjectService.INSTANCE.getCurrentProjectName().isEmpty()) {
                mainWindowFactory.openProjectsPage();
            } else {
                mainWindowFactory.openTargetsPage();
            }
        });
        targetsButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        authenticationsButton.setIcon(IconUtils.getIcon("/icons/authentications.svg", 1f));
        authenticationsButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        authenticationsButton.setHorizontalTextPosition(CENTER);
        addButtonPadding(authenticationsButton, 8);
        authenticationsButton.addActionListener(e -> {
            if (ProjectService.INSTANCE.getCurrentProjectName().isEmpty()) {
                mainWindowFactory.openProjectsPage();
            } else {
                mainWindowFactory.openAuthenticationsPage();
            }
        });
        authenticationsButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        projectsButton.setIcon(IconUtils.getIcon("/icons/projects.svg", 1f));
        projectsButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        projectsButton.setHorizontalTextPosition(CENTER);
        addButtonPadding(projectsButton, 8);
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
                updateMessageLabel.setVisible(false);
            } catch (Exception ex) {
                errorMessageLabel.setText(ex.toString());
                errorMessageLabel.setVisible(true);
                updateCLIButton.setEnabled(true);
                updateCLIButton.setText("Update NightVision CLI");
            }
        }
    }
}
