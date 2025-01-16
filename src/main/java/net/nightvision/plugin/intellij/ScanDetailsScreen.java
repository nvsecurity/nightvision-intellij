package net.nightvision.plugin.intellij;

import com.intellij.openapi.project.Project;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;

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

        HashMap<String, String> targetDetailsDictionary = new HashMap<String, String>();
        targetDetailsDictionary.put("Project:", scan.getProject().getName());
        targetDetailsDictionary.put("Target Name:", scan.getTargetName());
        targetDetailsDictionary.put("Location:", scan.getLocation());

        for (String key : targetDetailsDictionary.keySet()) {
            JPanel propertyPanel = new JPanel();
            BoxLayout layout = new BoxLayout(propertyPanel, BoxLayout.X_AXIS);
            propertyPanel.setLayout(layout);

            JLabel label = new JLabel(key);
            label.setFont(label.getFont().deriveFont(Font.BOLD));
            label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
            propertyPanel.add(label);

            propertyPanel.add(new JLabel(targetDetailsDictionary.get(key)));
            detailsPanel.add(propertyPanel);
        }

        for (Component component : detailsPanel.getComponents()) {
            if (component instanceof JComponent jComponent) {
                jComponent.setAlignmentX(Component.LEFT_ALIGNMENT);
            }
        }
    }
}
