package net.nightvision.plugin.scans;

import com.intellij.openapi.project.Project;
import net.nightvision.plugin.Constants;
import net.nightvision.plugin.ScanInfo;
import net.nightvision.plugin.Screen;
import net.nightvision.plugin.utils.IconUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

public class ScanDetailsScreen extends Screen {
    private JPanel scanDetailsPanel;
    private JPanel detailsPanel;
    private JButton backButton;

    public JPanel getScanDetailsPanel() {
        return scanDetailsPanel;
    }

    public ScanDetailsScreen (Project project, ScanInfo scanInfo) {
        super(project);

        backButton.addActionListener(e -> {
            mainWindowFactory.openScansPage();
        });
        backButton.setIcon(IconUtils.getIcon("/icons/back.svg", 1f));
        backButton.setBorder(null);
        backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        BoxLayout layout0 = new BoxLayout(detailsPanel, BoxLayout.Y_AXIS);
        detailsPanel.setLayout(layout0);

        String[] keysList = { "Project Name:", "Target Name:", "Target Type:", "Date Created:",
                "Authentication Name:", "Check in Browser:" };

        HashMap<Integer, String> detailsDictionary = getScanDetailsHashMap(scanInfo);

        for (Integer i : detailsDictionary.keySet()) {
            String key = keysList[i];
            if (!detailsDictionary.containsKey(i)) {
                continue;
            }
            var content = detailsDictionary.get(i);
            JPanel propertyPanel = new JPanel();
            BoxLayout layout = new BoxLayout(propertyPanel, BoxLayout.Y_AXIS);
            propertyPanel.setLayout(layout);

            JLabel label = new JLabel(key);
            label.setFont(label.getFont().deriveFont(Font.BOLD));
            propertyPanel.add(label);

            JLabel value = new JLabel(content);
            value.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));

            if ("Check in Browser:".equals(key)) {
                value.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                value.setToolTipText("Click to open in browser");
                value.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        try {
                            URI uri = Constants.Companion.getAppUrlFor("scans/" + scanInfo.getId() + "/findings");
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
    private HashMap<Integer, String> getScanDetailsHashMap(ScanInfo scanInfo) {
        HashMap<Integer, String> detailsDictionary = new HashMap<>();
        detailsDictionary.put(0, scanInfo.getProject().getName());
        detailsDictionary.put(1, scanInfo.getTargetName());
        detailsDictionary.put(2, scanInfo.getTargetType());
        detailsDictionary.put(3, ZonedDateTime.parse(scanInfo.getCreatedAt()).format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm:ss a")));
        if (scanInfo.getCredentials() != null) {
            detailsDictionary.put(4, scanInfo.getCredentials().getName());
        }
        detailsDictionary.put(5, "➡\uFE0F View Scan");

        return detailsDictionary;
    }
}
