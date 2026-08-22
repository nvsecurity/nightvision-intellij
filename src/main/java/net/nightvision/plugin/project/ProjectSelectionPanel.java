package net.nightvision.plugin.project;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBPanel;
import net.nightvision.plugin.MainWindowFactory;
import net.nightvision.plugin.MainWindowService;
import net.nightvision.plugin.exceptions.CommandNotFoundException;
import net.nightvision.plugin.exceptions.NotLoggedException;
import net.nightvision.plugin.models.ProjectInfo;
import net.nightvision.plugin.services.ProjectService;

import com.intellij.util.ui.JBUI;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ProjectSelectionPanel extends JBPanel<ProjectSelectionPanel> {
    private static final Logger LOG = Logger.getInstance(ProjectSelectionPanel.class);

    private final JLabel titleLabel;
    private final JComboBox<String> projectComboBox;
    // Callback (extra behavior) to execute when a project is selected.
    private final Consumer<String> onProjectSelected;

    public ProjectSelectionPanel(MainWindowFactory mainWindowFactory, Consumer<String> onProjectSelected) {
        this.onProjectSelected = onProjectSelected;
        setLayout(new BorderLayout());

        titleLabel = new JLabel("Current Project:");
        titleLabel.setBorder(JBUI.Borders.emptyBottom(4));
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
                selectProject(mainWindowFactory, selected);
            }
        });

        add(projectComboBox, BorderLayout.CENTER);
    }

    /**
     * Makes the selected project current, off the EDT.
     *
     * setCurrentProjectName runs "project set" and then "project show", and
     * running those from the combo box's listener held the EDT for both:
     * OSProcessHandler.waitFor asserts through checkEdtAndReadAction, so the
     * IDE logged it at SEVERE and raised an "IDE error" the user reads as a
     * crash, on top of freezing the UI (NV-4921).
     *
     * The combo is disabled for the duration. That is the only feedback the
     * panel has room for, and it doubles as the guard against a second
     * selection racing the first, which is why no request-ordering is needed
     * here.
     */
    private void selectProject(MainWindowFactory mainWindowFactory, String selected) {
        projectComboBox.setEnabled(false);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            Throwable failure = null;
            try {
                ProjectService.INSTANCE.setCurrentProjectName(selected);
            } catch (Throwable t) {
                failure = t;
            }
            Throwable outcome = failure;
            ApplicationManager.getApplication().invokeLater(
                    () -> finishSelection(mainWindowFactory, selected, outcome));
        });
    }

    /**
     * Applies the outcome on the EDT.
     *
     * Deliberately not guarded on whether the panel is still mounted. The test
     * Screen.isMounted makes would be wrong here: navigation replaces a
     * screen's whole panel, and this panel stays a child of that discarded
     * panel, so its parent is still set. Nor is a guard wanted. A callback only
     * writes to combo boxes that are no longer displayed, and the two failures
     * that navigate mean the CLI is missing or the login has expired, which
     * every screen depends on wherever the user has got to.
     */
    private void finishSelection(MainWindowFactory mainWindowFactory, String selected, Throwable failure) {
        projectComboBox.setEnabled(true);

        if (failure == null) {
            // Invoke the extra behavior callback if provided.
            if (onProjectSelected != null) {
                onProjectSelected.accept(selected);
            }
            return;
        }
        if (failure instanceof CommandNotFoundException) {
            mainWindowFactory.openInstallCLIPage();
            return;
        }
        if (failure instanceof NotLoggedException) {
            mainWindowFactory.openLoginPage();
            return;
        }
        // Anything else, such as a project name the CLI rejects, leaves the
        // combo showing a project that is not current. The panel has no error
        // label to put a reason in, and is shared by five screens, so it is
        // logged rather than swallowed outright as it was before. Reverting
        // the combo and reporting the reason is NV-4933.
        LOG.warn("Could not switch to project '" + selected + "'", failure);
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
