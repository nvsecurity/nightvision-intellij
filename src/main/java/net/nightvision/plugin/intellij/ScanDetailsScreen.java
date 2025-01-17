package net.nightvision.plugin.intellij;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Objects;

public class ScanDetailsScreen extends Screen {
    private JPanel scanDetailsPanel;
    private JPanel detailsPanel;
    private JButton backButton;

    public JPanel getScanDetailsPanel() {
        return scanDetailsPanel;
    }

    public ScanDetailsScreen (Project project, Scan scan) {
        super(project.getService(MainWindowService.class).getWindowFactory(), project);

        backButton.addActionListener(e -> {
            mainWindow.openScansPage();
        });

        BoxLayout layout0 = new BoxLayout(detailsPanel, BoxLayout.Y_AXIS);
        detailsPanel.setLayout(layout0);

        HashMap<String, String> targetDetailsDictionary = getTargetDetailsHashMap(scan);

        for (String key : targetDetailsDictionary.keySet()) {
            JPanel propertyPanel = new JPanel();
            BoxLayout layout = new BoxLayout(propertyPanel, BoxLayout.Y_AXIS);
            propertyPanel.setLayout(layout);

            JLabel label = new JLabel(key);
            label.setFont(label.getFont().deriveFont(Font.BOLD));
            propertyPanel.add(label);

            JLabel value = new JLabel(targetDetailsDictionary.get(key));
            value.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
            propertyPanel.add(value);
            detailsPanel.add(propertyPanel);
        }

        for (Component component : detailsPanel.getComponents()) {
            if (component instanceof JComponent jComponent) {
                jComponent.setAlignmentX(Component.LEFT_ALIGNMENT);
            }
        }
    }

    @NotNull
    private static HashMap<String, String> getTargetDetailsHashMap(Scan scan) {
        HashMap<String, String> targetDetailsDictionary = new HashMap<>();
        targetDetailsDictionary.put("Project:", scan.getProject().getName());
        targetDetailsDictionary.put("Target Type:", scan.getTargetType());
        targetDetailsDictionary.put("Target Accessibility:", Objects.requireNonNullElse(scan.getAccessibility(), "Private"));
        targetDetailsDictionary.put("Target ID:", scan.getTargetId());
        targetDetailsDictionary.put("Target Name:", scan.getTargetName());
        targetDetailsDictionary.put("Date Created:", ZonedDateTime.parse(scan.getCreatedAt())
                .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm:ss a")));
        targetDetailsDictionary.put("Base URL:", scan.getLocation());
        return targetDetailsDictionary;
    }
}
