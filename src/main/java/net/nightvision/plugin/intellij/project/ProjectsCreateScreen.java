package net.nightvision.plugin.intellij.project;

import com.intellij.openapi.project.Project;
import net.nightvision.plugin.intellij.Screen;
import net.nightvision.plugin.intellij.Utils;
import net.nightvision.plugin.intellij.services.ProjectService;

import javax.swing.*;

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
        backButton.setIcon(Utils.getIcon("/icons/back.svg", 1f));
        backButton.setBorder(null);

        cancelButton.addActionListener(e -> {
            mainWindowFactory.openProjectsPage();
        });

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
    }
}
