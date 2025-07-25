package net.nightvision.plugin.target;

import com.intellij.openapi.project.Project;
import net.nightvision.plugin.Constants;
import net.nightvision.plugin.Screen;
import net.nightvision.plugin.utils.IconUtils;
import net.nightvision.plugin.models.TargetInfo;
import net.nightvision.plugin.services.TargetService;
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

    public TargetDetailsScreen(Project project, TargetInfo rawTargetInfo) {
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

        var targetInfo = TargetService.INSTANCE.getTargetSpecificInfo(rawTargetInfo.getType(), rawTargetInfo.getId());
        if (targetInfo == null) {
            targetInfo = rawTargetInfo;
        }
        final var finalTargetInfo = targetInfo;
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
                var specUrl = targetInfo.getSwaggerFileURL();
                if (specUrl == null || specUrl.isBlank()) {
                    specUrl = TargetService.INSTANCE.getSpecURL(targetInfo.getId());
                }
                final String finalSpecUrl = specUrl;
                if (!specUrl.isBlank()) {
                    var swaggerFileUrlName = specUrl.substring(specUrl.lastIndexOf('/') + 1);
                    var swaggerFileName = value.getText();
                    if (swaggerFileName.isBlank()) {
                        swaggerFileName = swaggerFileUrlName;
                        if (swaggerFileName.isBlank()) {
                            swaggerFileName = "spec";
                        }
                    }
                    value.setText(swaggerFileName);
                }
                value.setText("➡\uFE0F " + value.getText());


                value.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        try {
                            URI uri = new URI(finalSpecUrl);
                            Desktop.getDesktop().browse(uri);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            }

            if ("Check in Browser:".equals(key)) {
                value.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                value.setToolTipText("Click to open in browser");
                value.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        try {
                            URI uri = Constants.Companion.getAppUrlFor("targets/" + finalTargetInfo.getId());
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
    private HashMap<Integer, String> getTargetDetailsHashMap(TargetInfo targetInfo) {
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
            var swaggerFileName = targetInfo.getSwaggerFileName();
            if (swaggerFileName == null) {
                swaggerFileName = "";
            }
            detailsDictionary.put(8, swaggerFileName);
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
