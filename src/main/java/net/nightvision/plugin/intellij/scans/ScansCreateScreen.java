package net.nightvision.plugin.intellij.scans;

import com.intellij.openapi.project.Project;
import net.nightvision.plugin.intellij.Screen;
import net.nightvision.plugin.intellij.Utils;
import net.nightvision.plugin.intellij.models.AuthInfo;
import net.nightvision.plugin.intellij.models.ProjectInfo;
import net.nightvision.plugin.intellij.models.TargetInfo;
import net.nightvision.plugin.intellij.project.ProjectSelectionPanel;
import net.nightvision.plugin.intellij.project.ProjectsScreen;
import net.nightvision.plugin.intellij.services.AuthenticationService;
import net.nightvision.plugin.intellij.services.ProjectService;
import net.nightvision.plugin.intellij.services.ScanService;
import net.nightvision.plugin.intellij.services.TargetService;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static net.nightvision.plugin.intellij.project.ProjectSelectionPanel.getCommonRendererForCombobox;

public class ScansCreateScreen extends Screen {
    private JButton backButton;
    private JPanel currentProjectWrapperPanel;
    private JComboBox targetComboBox;
    private JComboBox authenticationComboBox;
    private JButton startScanButton;
    private JLabel targetLabel;
    private JLabel errorMessageLabel;
    private JPanel scansCreatePanel;

    private String targetType;

    public JPanel getScansCreatePanel() {
        return scansCreatePanel;
    }

    public ScansCreateScreen(Project project, String targetType) {
        super(project);
        this.targetType = targetType;

        errorMessageLabel.setVisible(false);

        targetLabel.setText("Target (" + (targetType.equalsIgnoreCase("URL") ? "WEB" : "API") + ")");

        backButton.addActionListener(e -> {
            mainWindowFactory.openScansPage();
        });
        backButton.setIcon(Utils.getIcon("/icons/back.svg", 1f));
        backButton.setBorder(null);
        backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        currentProjectWrapperPanel.add(new ProjectSelectionPanel(selectedProject -> {
            loadTargetComboBox();
            loadAuthenticationComboBox();
        }));

        startScanButton.addActionListener(e -> {
            errorMessageLabel.setVisible(false);
            errorMessageLabel.setText("");
            startScanButton.setEnabled(false);
            var targetName = (String) targetComboBox.getSelectedItem();
            var authName = (String) authenticationComboBox.getSelectedItem();
            try {
                new StartScanWorker(targetName, authName).execute();
                mainWindowFactory.openScansPage();
            } catch(Exception exception) {
                errorMessageLabel.setText(exception.getMessage());
                errorMessageLabel.setVisible(true);
                startScanButton.setEnabled(true);
            }
        });
        startScanButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        targetComboBox.setRenderer(getCommonRendererForCombobox());
        authenticationComboBox.setRenderer(getCommonRendererForCombobox());

        loadTargetComboBox();
        loadAuthenticationComboBox();
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
        for (AuthInfo info : authInfos) {
            authNames.add(info.getName());
        }
        if (authNames.isEmpty()) {
            authNames.add("");
        }
        authNames.forEach(name -> authenticationComboBox.addItem(name));
        authenticationComboBox.setSelectedIndex(0);
    }
}
