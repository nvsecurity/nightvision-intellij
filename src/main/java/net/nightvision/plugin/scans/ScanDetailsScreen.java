package net.nightvision.plugin.scans;

import com.intellij.openapi.project.Project;
import net.nightvision.plugin.Constants;
import net.nightvision.plugin.ScanInfo;
import net.nightvision.plugin.Screen;
import net.nightvision.plugin.utils.IconUtils;

import com.intellij.util.ui.JBUI;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class ScanDetailsScreen extends Screen {
    private JPanel scanDetailsPanel;
    private JPanel detailsPanel;
    private JButton backButton;

    public JPanel getScanDetailsPanel() {
        return scanDetailsPanel;
    }

    public ScanDetailsScreen (Project project, ScanInfo scanInfo) {
        super(project);

        scanDetailsPanel.setBorder(JBUI.Borders.empty(8));

        backButton.addActionListener(e -> {
            mainWindowFactory.openScansPage();
        });
        backButton.setIcon(IconUtils.getIcon("/icons/back.svg", 1f));
        backButton.setBorder(null);
        backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));

        addDetailRow(detailsPanel, "Project Name:", new JLabel(scanInfo.getProject().getName()));
        addDetailRow(detailsPanel, "Target Name:", new JLabel(scanInfo.getTargetName()));
        addDetailRow(detailsPanel, "Target Type:", new JLabel(scanInfo.getTargetType()));
        addDetailRow(detailsPanel, "Date Created:", new JLabel(ZonedDateTime.parse(scanInfo.getCreatedAt())
                .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm:ss a"))));
        if (scanInfo.getCredentials() != null) {
            addDetailRow(detailsPanel, "Authentication Name:", new JLabel(scanInfo.getCredentials().getName()));
        }

        JLabel viewScan = new JLabel("View Scan");
        viewScan.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        viewScan.setToolTipText("Click to open in browser");
        viewScan.addMouseListener(new MouseAdapter() {
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
        addDetailRow(detailsPanel, "Check in Browser:", viewScan);

        detailsPanel.add(Box.createVerticalGlue());
    }
}
