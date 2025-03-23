package net.nightvision.plugin.intellij;

import javax.swing.*;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.uiDesigner.core.GridConstraints;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static com.intellij.uiDesigner.core.GridConstraints.*;
import static javax.swing.SwingConstants.CENTER;
import static javax.swing.SwingConstants.HORIZONTAL;

public class OverviewScreen extends Screen {
    private static boolean isExtraOptionsVisible = false;
    private JPanel overviewPanel;
//    private JPanel extraButtonsPanel;
    private JButton apiDiscoveryButton;
    private JButton apiAndWebSecurityButton;
    private JPanel extraOptionsPanel;
    private JButton scansButton;
    private JButton targetsButton;
    private JButton authenticationsButton;
    private JButton projectsButton;

    public JPanel getOverviewPanel() {
        return overviewPanel;
    }

    private void setExtraOptionsActivatedTheme() {
        if (extraOptionsPanel.isVisible()) {
            apiAndWebSecurityButton.setBackground(JBColor.CYAN);
        } else {
            apiAndWebSecurityButton.setBackground(JBColor.WHITE);
        }

    }

    public OverviewScreen(Project project) {
        super(project);

        extraOptionsPanel.setVisible(isExtraOptionsVisible);
        setExtraOptionsActivatedTheme();

        apiDiscoveryButton.setIcon(Utils.getIcon("/icons/api-discovery.svg", 1f));
        apiDiscoveryButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        apiDiscoveryButton.setHorizontalTextPosition(CENTER);
        apiDiscoveryButton.addActionListener(e ->  mainWindow.openApiDiscoveryPage());

        apiAndWebSecurityButton.setIcon(Utils.getIcon("/icons/dast.svg", 1f));
        apiAndWebSecurityButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        apiAndWebSecurityButton.setHorizontalTextPosition(CENTER);

        apiAndWebSecurityButton.addActionListener(e -> {
            isExtraOptionsVisible = !isExtraOptionsVisible;
            extraOptionsPanel.setVisible(isExtraOptionsVisible);
            setExtraOptionsActivatedTheme();
        });

        scansButton.setIcon(Utils.getIcon("/icons/scans.svg", 1f));
        scansButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        scansButton.setHorizontalTextPosition(CENTER);
        scansButton.addActionListener(e ->  mainWindow.openScansPage());

        targetsButton.setIcon(Utils.getIcon("/icons/targets.svg", 1f));
        targetsButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        targetsButton.setHorizontalTextPosition(CENTER);

        authenticationsButton.setIcon(Utils.getIcon("/icons/authentications.svg", 1f));
        authenticationsButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        authenticationsButton.setHorizontalTextPosition(CENTER);
        authenticationsButton.addActionListener(e -> mainWindow.openAuthenticationsPage());

        projectsButton.setIcon(Utils.getIcon("/icons/projects.svg", 1f));
        projectsButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        projectsButton.setHorizontalTextPosition(CENTER);
    }

}
