package net.nightvision.plugin.project;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import net.nightvision.plugin.Screen;
import net.nightvision.plugin.exceptions.CommandNotFoundException;
import net.nightvision.plugin.exceptions.NotLoggedException;
import net.nightvision.plugin.utils.IconUtils;
import net.nightvision.plugin.services.ProjectService;
import org.jetbrains.annotations.NotNull;

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

            new Task.Backgroundable(project, "Create Project", false) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    try {
                        ProjectService.INSTANCE.createProject(projectName);

                        ApplicationManager.getApplication().invokeLater(() -> {
                            mainWindowFactory.openProjectsPage();
                        });
                    } catch (CommandNotFoundException ex) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            mainWindowFactory.openInstallCLIPage();
                        });
                    } catch (NotLoggedException ex) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            mainWindowFactory.openLoginPage();
                        });
                    } catch(Exception exception) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            errorMessage.setText(exception.getMessage());
                            errorMessage.setVisible(true);
                        });
                    }
                }
            }.queue();


            createButton.setEnabled(true);
        });
        createButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}
