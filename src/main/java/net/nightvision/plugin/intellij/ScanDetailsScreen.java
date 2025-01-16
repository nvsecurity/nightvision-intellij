package net.nightvision.plugin.intellij;

import com.intellij.openapi.project.Project;

import javax.swing.*;

public class ScanDetailsScreen {
    private JPanel scanDetailsPanel;
    private JPanel detailsPanel;
    private JButton backButton;

    private final MainWindowFactory mainWindow;
    private final Project project;

    public JPanel getScanDetailsPanel() {
        return scanDetailsPanel;
    }

    public ScanDetailsScreen (Project project, Scan scan) {
        this.mainWindow = project.getService(MainWindowService.class).getWindowFactory();
        this.project = project;

        backButton.addActionListener(e -> {
            mainWindow.openScansPage();
        });

        BoxLayout layout0 = new BoxLayout(detailsPanel, BoxLayout.Y_AXIS);
        detailsPanel.setLayout(layout0);

        JPanel targetNamePanel = new JPanel();
        BoxLayout layout = new BoxLayout(targetNamePanel, BoxLayout.X_AXIS);
        targetNamePanel.setLayout(layout);
        targetNamePanel.add(new JLabel("Target Name:"));
        targetNamePanel.add(new JLabel(scan.getTarget_name()));
        detailsPanel.add(targetNamePanel);

        JPanel targetLocationPanel = new JPanel();
        BoxLayout layout2 = new BoxLayout(targetLocationPanel, BoxLayout.X_AXIS);
        targetLocationPanel.setLayout(layout2);
        targetLocationPanel.add(new JLabel("Location:"));
        targetLocationPanel.add(new JLabel(scan.getLocation()));
        detailsPanel.add(targetLocationPanel);
    }
}
