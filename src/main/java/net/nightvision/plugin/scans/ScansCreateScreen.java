package net.nightvision.plugin.scans;

import com.intellij.openapi.project.Project;
import net.nightvision.plugin.Screen;
import net.nightvision.plugin.exceptions.CommandNotFoundException;
import net.nightvision.plugin.exceptions.NotLoggedException;
import net.nightvision.plugin.exceptions.PermissionDeniedException;
import net.nightvision.plugin.utils.IconUtils;
import net.nightvision.plugin.models.AuthInfo;
import net.nightvision.plugin.models.TargetInfo;
import net.nightvision.plugin.project.ProjectSelectionPanel;
import net.nightvision.plugin.services.AuthenticationService;
import net.nightvision.plugin.services.ScanService;
import net.nightvision.plugin.services.TargetService;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static net.nightvision.plugin.project.ProjectSelectionPanel.getCommonRendererForCombobox;

public class ScansCreateScreen extends Screen {
    private JButton backButton;
    private JPanel currentProjectWrapperPanel;
    private JComboBox targetComboBox;
    private JComboBox authenticationComboBox;
    private JButton startScanButton;
    private JLabel targetLabel;
    private JBLabel scanStatusLabel;
    private JPanel scansCreatePanel;

    private String targetType;

    public JPanel getScansCreatePanel() {
        return scansCreatePanel;
    }

    public ScansCreateScreen(Project project, String targetType) {
        super(project);
        this.targetType = targetType;

        scansCreatePanel.setBorder(JBUI.Borders.empty(8));

        makeSelectable(scanStatusLabel);
        scanStatusLabel.setVisible(false);

        targetLabel.setText("Target (" + (targetType.equalsIgnoreCase("URL") ? "WEB" : "API") + ")");

        backButton.addActionListener(e -> {
            mainWindowFactory.openScansPage();
        });
        backButton.setIcon(IconUtils.getIcon("/icons/back.svg", 1f));
        backButton.setBorder(null);
        backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        currentProjectWrapperPanel.add(new ProjectSelectionPanel(mainWindowFactory, selectedProject -> {
            loadTargetComboBox();
            loadAuthenticationComboBox();
        }));

        addButtonPadding(startScanButton, 6);
        startScanButton.addActionListener(e -> {
            showStatus("Starting scan, please wait...");
            startScanButton.setEnabled(false);
            var targetName = (String) targetComboBox.getSelectedItem();
            var authName = (String) authenticationComboBox.getSelectedItem();
            new StartScanWorker(targetName, authName).execute();
        });
        startScanButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        targetComboBox.setRenderer(getCommonRendererForCombobox());
        authenticationComboBox.setRenderer(getCommonRendererForCombobox());

        loadTargetComboBox();
        loadAuthenticationComboBox();
    }

    /**
     * Says the scan is being started. The screen stays put for as long as the
     * startup deadline allows, which is up to two minutes, and a greyed-out
     * button is the only other sign that anything is happening. Navigating to
     * the scan list on submit used to be the acknowledgement that the click
     * had registered; holding the page takes it away. The wording is the VS
     * Code extension's, which prints the same sentence under its own Start
     * Scan button.
     */
    private void showStatus(String message) {
        scanStatusLabel.setForeground(UIUtil.getLabelForeground());
        scanStatusLabel.setText(message);
        scanStatusLabel.setVisible(true);
    }

    private class StartScanWorker extends SwingWorker<Void, Void> {
        private String targetName;
        private String authName;

        StartScanWorker(String targetName, String authName) {
            this.targetName = targetName;
            this.authName = authName;
        }

        @Override
        protected Void doInBackground() throws Exception {
            ScanService.INSTANCE.startScan(targetName, authName);
            return null;
        }

        @Override
        protected void done() {
            // startScan waits on the CLI for as long as the startup deadline
            // allows, which is long enough for the user to leave this screen.
            // Acting on a detached instance would either replace whatever they
            // navigated to or write the CLI's reason into a label that is no
            // longer displayed, which is the stranded-message defect this
            // routing exists to avoid (NV-4885). Mounted, not showing: a user
            // who parks on the Terminal tool window for the two minutes this
            // can take still wants the result when they come back.
            if (!isMounted(scansCreatePanel)) {
                return;
            }
            try {
                get();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                showError(ex);
                return;
            } catch (ExecutionException ex) {
                // SwingWorker wraps whatever doInBackground threw, so the cause
                // is what the routing below has to look at. Matching on the
                // wrapper meant the CLI-not-found branch never ran and every
                // failure surfaced as an ExecutionException toString.
                Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                if (cause instanceof CommandNotFoundException) {
                    mainWindowFactory.openInstallCLIPage();
                    return;
                }
                if (cause instanceof NotLoggedException) {
                    mainWindowFactory.openLoginPage();
                    return;
                }
                showError(cause);
                return;
            }
            // Only leave the page once the scan is known to have started.
            // Navigating on submit stranded every failure message on a screen
            // the user had already been taken off (NV-4827).
            mainWindowFactory.openScansPage();
        }

        private void showError(Throwable t) {
            scanStatusLabel.setForeground(JBColor.RED);
            scanStatusLabel.setText(asWrappedError(t));
            scanStatusLabel.setVisible(true);
            startScanButton.setEnabled(true);
        }
    }

    private void loadTargetComboBox() {
        targetComboBox.removeAllItems();
        List<TargetInfo> targetInfos = TargetService.INSTANCE.getTargetInfos(this.targetType);
        List<String> targetNames = new ArrayList<>();
        for (TargetInfo info : targetInfos) {
            targetNames.add(info.getName());
        }
        if (targetNames.isEmpty()) {
            targetNames.add("");
        }
        targetNames.forEach(name -> targetComboBox.addItem(name));
        targetComboBox.setSelectedIndex(0);
    }

    private void loadAuthenticationComboBox() {
        authenticationComboBox.removeAllItems();
        List<AuthInfo> authInfos = AuthenticationService.INSTANCE.getAuthInfos();
        List<String> authNames = new ArrayList<>();
        // First and selected by default, whatever the project holds. The field
        // is optional, so "none" has to be reachable; offering it only when
        // there was nothing else to choose meant every scan in a project with
        // any authentication silently carried the first one in the list. The VS
        // Code extension renders this same "-" entry on any optional dropdown.
        authNames.add("");
        for (AuthInfo info : authInfos) {
            authNames.add(info.getName());
        }
        authNames.forEach(name -> authenticationComboBox.addItem(name));
        authenticationComboBox.setSelectedIndex(0);
    }
}
