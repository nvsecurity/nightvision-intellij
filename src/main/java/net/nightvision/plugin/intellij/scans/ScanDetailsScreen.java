package net.nightvision.plugin.intellij.scans;

import com.intellij.openapi.project.Project;
import net.nightvision.plugin.intellij.Scan;
import net.nightvision.plugin.intellij.Screen;
import net.nightvision.plugin.intellij.Utils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
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
        super(project);

        backButton.addActionListener(e -> {
            mainWindowFactory.openScansPage();
        });
        backButton.setIcon(Utils.getIcon("/icons/back.svg", 1f));
        backButton.setBorder(null);
        backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

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

            // Check if this row is the "Check in Browser:" row.
            if ("Check in Browser:".equals(key)) {
                // Set the cursor to hand so the user knows it's clickable.
                value.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                // Remove any default tooltip if needed.
                value.setToolTipText("Click to open in browser");
                // Add a mouse listener to open the browser when clicked.
                value.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        try {
                            // Create the URI for the link. Here, we construct it using the scan's id.
                            URI uri = new URI("https://app.nightvision.net/scans/" + scan.getId() + "/findings");
                            Desktop.getDesktop().browse(uri);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            }

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
        targetDetailsDictionary.put("Check in Browser:", "➡\uFE0F View Findings");
        return targetDetailsDictionary;
    }
}
