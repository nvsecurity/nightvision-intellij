package net.nightvision.plugin.project;

import com.intellij.openapi.project.Project;
import net.nightvision.plugin.Screen;
import net.nightvision.plugin.utils.IconUtils;
import net.nightvision.plugin.services.ProjectService;

import javax.swing.*;
import java.awt.*;

public class ProjectsCreateScreen extends Screen {
    private JButton backButton;
    private JTextField projectNameTextField;
    private JButton cancelButton;
    private JButton createButton;
    private JPanel projectsCreatePanel;
    private JLabel errorMessage;

    public JPanel getProjectsCreatePanel() {
        return projectsCreatePanel;
    }

    public ProjectsCreateScreen(Project project) {
        super(project);

        errorMessage.setVisible(false);
        backButton.addActionListener(e -> {
            mainWindowFactory.openProjectsPage();
        });
        backButton.setIcon(IconUtils.getIcon("/icons/back.svg", 1f));
        backButton.setBorder(null);
        backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        cancelButton.addActionListener(e -> {
            mainWindowFactory.openProjectsPage();
        });
        cancelButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        createButton.addActionListener(e -> {
            errorMessage.setVisible(false);
            errorMessage.setText("");
            createButton.setEnabled(false);
            String projectName = projectNameTextField.getText();

            try {
                ProjectService.INSTANCE.createProject(projectName);
                mainWindowFactory.openProjectsPage();
            } catch(Exception exception) {
                errorMessage.setText(exception.getMessage());
                errorMessage.setVisible(true);
                createButton.setEnabled(true);
            }
        });
        createButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}
