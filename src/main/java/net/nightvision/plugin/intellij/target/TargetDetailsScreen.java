package net.nightvision.plugin.intellij.target;

import com.intellij.openapi.project.Project;
import net.nightvision.plugin.intellij.Screen;
import net.nightvision.plugin.intellij.utils.IconUtils;
import net.nightvision.plugin.intellij.models.TargetInfo;
import net.nightvision.plugin.intellij.services.TargetService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

public class TargetDetailsScreen extends Screen {
    private JButton backButton;
    private JPanel detailsPanel;
    private JPanel targetDetailsPanel;

    public JPanel getTargetDetailsPanel() {
        return targetDetailsPanel;
    }

    public TargetDetailsScreen(Project project, TargetInfo targetInfo) {
        super(project);

        backButton.addActionListener(e -> {
            mainWindowFactory.openTargetsPage();
        });
        backButton.setIcon(IconUtils.getIcon("/icons/back.svg", 1f));
        backButton.setBorder(null);
        backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        BoxLayout boxLayout = new BoxLayout(detailsPanel, BoxLayout.Y_AXIS);
        detailsPanel.setLayout(boxLayout);

        String[] keysList = { "Target Name:", "Project:", "Target Type:", "Target Accessibility:", "Target ID:",
            "Date Created:", "Last Scanned:", "Base URL:", "API Spec:", "API Spec Status:", "Target is ready to scan:",
            "Check in Browser:"
            /*Excluded URL patterns*/ };

        HashMap<Integer, String> targetDetailsDictionary = getTargetDetailsHashMap(targetInfo);

        for (Integer i : targetDetailsDictionary.keySet()) {
            String key = keysList[i];
            if (!targetDetailsDictionary.containsKey(i)) {
                continue;
            }
            var content = targetDetailsDictionary.get(i);
            JPanel propertyPanel = new JPanel();
            BoxLayout layout = new BoxLayout(propertyPanel, BoxLayout.Y_AXIS);
            propertyPanel.setLayout(layout);

            JLabel label = new JLabel(key);
            label.setFont(label.getFont().deriveFont(Font.BOLD));
            propertyPanel.add(label);

            JLabel value = new JLabel(content);
            value.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));

            if ("API Spec:".equals(key)) {
                value.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                value.setToolTipText("Click to download");
                value.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        try {
                            var specUrl = TargetService.INSTANCE.getSpecURL(targetInfo.getId());
                            URI uri = new URI(specUrl);
                            Desktop.getDesktop().browse(uri);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            }

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
                            URI uri = new URI("https://app.nightvision.net/targets/" + targetInfo.getId());
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
    private HashMap<Integer, String> getTargetDetailsHashMap(TargetInfo rawTargetInfo) {
        var targetInfo = TargetService.INSTANCE.getTargetSpecificInfo(rawTargetInfo.getType(), rawTargetInfo.getId());
        if (targetInfo == null) {
            targetInfo = rawTargetInfo;
        }
        HashMap<Integer, String> detailsDictionary = new HashMap<>();
        detailsDictionary.put(0, targetInfo.getName());
        detailsDictionary.put(1, targetInfo.getProjectName());
        var t = targetInfo.getType();
        detailsDictionary.put(2, switch(t) {
            case "OPENAPI" -> "Open API";
            case "URL" -> "Web";
            case "SCRIPT" -> "Playwright";
            default -> t;
        });
        detailsDictionary.put(3, targetInfo.getInternetAccessible() ? "Public" : "Private");
        detailsDictionary.put(4, targetInfo.getId());
        detailsDictionary.put(5, ZonedDateTime.parse(targetInfo.getCreatedAt()).format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm:ss a")));
        if (targetInfo.getLastScannedAt() != null) {
            detailsDictionary.put(6, ZonedDateTime.parse(targetInfo.getLastScannedAt()).format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm:ss a")));
        } else {
            detailsDictionary.put(6, "N/A");
        }
        detailsDictionary.put(7, targetInfo.getLocation());
        if (targetInfo.getHasSpecUploaded()) {
            detailsDictionary.put(8, "➡\uFE0F " + targetInfo.getSwaggerFileName());
            var status = targetInfo.getSpecStatus();
            detailsDictionary.put(9, switch(status) {
                case "NO_SPEC" -> "Spec/collection is not specified";
                case "DOWNLOADING" -> "Downloading spec/collection";
                case "DOWNLOAD_ERROR" -> "Spec/collection download failed";
                case "WAITING_FOR_UPLOAD" -> "Waiting for spec/collection upload to complete";
                case "VALIDATING" -> "Validating spec/collection";
                case "INVALID" -> "Invalid spec/collection definition";
                case "VALID" -> "Spec/collection is valid";
                case "WARNING" -> "Spec/collection has validation warnings";
                default -> status;
            });
        }
        detailsDictionary.put(10, targetInfo.isReadyToScan() ? "Yes" : "No");
        detailsDictionary.put(11, "➡\uFE0F View Target");
        var config = targetInfo.getConfiguration();
        if (config != null) {
            var excludedUrlPatterns = config.getExcludedUrlPatterns();
            if (excludedUrlPatterns != null && !excludedUrlPatterns.isEmpty()) {
                // TODO
            }

            var excludedXPaths = config.getExcludedXPaths();
            if (excludedXPaths != null && !excludedXPaths.isEmpty()) {
                // TODO
            }
        }


        return detailsDictionary;
    }
}
