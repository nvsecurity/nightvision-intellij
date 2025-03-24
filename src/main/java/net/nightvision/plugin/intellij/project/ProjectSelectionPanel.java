package net.nightvision.plugin.intellij.project;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBPanel;
import net.nightvision.plugin.intellij.models.ProjectInfo;
import net.nightvision.plugin.intellij.services.ProjectService;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ProjectSelectionPanel extends JBPanel<ProjectSelectionPanel> {
    private final JLabel titleLabel;
    private final JComboBox<String> projectComboBox;
    // Callback (extra behavior) to execute when a project is selected.
    private final Consumer<String> onProjectSelected;

    public ProjectSelectionPanel(Consumer<String> onProjectSelected) {
        this.onProjectSelected = onProjectSelected;
        setLayout(new BorderLayout());

        titleLabel = new JLabel("Current Project:");
        add(titleLabel, BorderLayout.NORTH);

        List<ProjectInfo> projectInfos = ProjectService.INSTANCE.getProjectInfos();
        List<String> projectNames = new ArrayList<>();
        for (ProjectInfo info : projectInfos) {
            projectNames.add(info.getName());
        }

        String currentProjectName = ProjectService.INSTANCE.getCurrentProjectName();
        boolean currentIsEmpty = (currentProjectName == null || currentProjectName.isEmpty());

        if (currentIsEmpty) {
            // If current project is empty, add an invalid option ("") that will be rendered as a hyphen.
            projectNames.add(0, "");
        }

        // Create the combo box with the list of project names.
        projectComboBox = new ComboBox<>(projectNames.toArray(new String[0]));

        // Pre-select the current item.
        if (currentIsEmpty) {
            projectComboBox.setSelectedItem("");
        } else {
            projectComboBox.setSelectedItem(currentProjectName);
        }

        // Set a custom renderer so that an empty string is shown as "-" visually.
        projectComboBox.setRenderer(getCommonRendererForCombobox());

        projectComboBox.addActionListener(e -> {
            String selected = (String) projectComboBox.getSelectedItem();
            if (selected != null && !selected.isEmpty()) {
                // If a valid project is selected, remove the empty option if present.
                DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) projectComboBox.getModel();
                if (model.getSize() > 0 && model.getElementAt(0).isEmpty()) {
                    model.removeElementAt(0);
                }
                try {
                    // Set the selected project as current.
                    ProjectService.INSTANCE.setCurrentProjectName(selected);
                    // Invoke the extra behavior callback if provided.
                    if (onProjectSelected != null) {
                        onProjectSelected.accept(selected);
                    }
                } catch(Exception exception) {
                    // TODO: handle better this exception, e.g. show message...
                    // Exception here will happen if the project name is invalid or if some other error happened...
                }
            }
        });

        add(projectComboBox, BorderLayout.CENTER);
    }

    public static DefaultListCellRenderer getCommonRendererForCombobox() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list,
                                                          Object value,
                                                          int index,
                                                          boolean isSelected,
                                                          boolean cellHasFocus) {
                Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof String && ((String) value).isEmpty()) {
                    setText("-");
                }
                return comp;
            }
        };
    }
}
