package net.nightvision.plugin.project;

import com.intellij.openapi.project.Project;
import net.nightvision.plugin.Constants;
import net.nightvision.plugin.Screen;
import net.nightvision.plugin.models.ProjectInfo;
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

public class ProjectDetailsScreen extends Screen {
    private JPanel projectDetailsPanel;
    private JButton backButton;
    private JPanel detailsPanel;

    public JPanel getProjectDetailsPanel() {
        return projectDetailsPanel;
    }

    public ProjectDetailsScreen(Project project, ProjectInfo projectInfo) {
        super(project);

        backButton.addActionListener(e -> {
            mainWindowFactory.openProjectsPage();
        });
        backButton.setIcon(IconUtils.getIcon("/icons/back.svg", 1f));
        backButton.setBorder(null);
        backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        BoxLayout boxLayout = new BoxLayout(detailsPanel, BoxLayout.Y_AXIS);
        detailsPanel.setLayout(boxLayout);

        String[] keysList = { "Project Name:", "Date Created:", "Last Updated:", "Check in Browser:" };

        HashMap<Integer, String> detailsDictionary = getProjectDetailsHashMap(projectInfo);

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
                            URI uri = Constants.Companion.getAppUrlFor("projects/" + projectInfo.getId());
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
    private HashMap<Integer, String> getProjectDetailsHashMap(ProjectInfo projectInfo) {
        HashMap<Integer, String> detailsDictionary = new HashMap<>();
        detailsDictionary.put(0, projectInfo.getName());
        detailsDictionary.put(1, ZonedDateTime.parse(projectInfo.getCreatedAt()).format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm:ss a")));
        detailsDictionary.put(2, ZonedDateTime.parse(projectInfo.getLastUpdatedAt()).format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm:ss a")));
        detailsDictionary.put(3, "➡\uFE0F View Project");

        return detailsDictionary;
    }
}
