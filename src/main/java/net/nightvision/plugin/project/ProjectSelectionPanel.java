package net.nightvision.plugin.project;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBPanel;
import net.nightvision.plugin.MainWindowFactory;
import net.nightvision.plugin.MainWindowService;
import net.nightvision.plugin.ResourceLoader;
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
    private final JPanel statusPanel = new JPanel(new BorderLayout());
    private boolean updating;

    public ProjectSelectionPanel(MainWindowFactory mainWindowFactory, Consumer<String> onProjectSelected) {
        this(mainWindowFactory, onProjectSelected, null);
    }

    public ProjectSelectionPanel(MainWindowFactory mainWindowFactory, Consumer<String> onProjectSelected, JComponent owner) {
        this.onProjectSelected = onProjectSelected;
        setLayout(new BorderLayout());

        titleLabel = new JLabel("Current Project:");
        titleLabel.setBorder(JBUI.Borders.emptyBottom(4));
        add(titleLabel, BorderLayout.NORTH);

        projectComboBox = new ComboBox<>();

        // Set a custom renderer so that an empty string is shown as "-" visually.
        projectComboBox.setRenderer(getCommonRendererForCombobox());

        projectComboBox.addActionListener(e -> {
            if (updating) return;
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
        add(statusPanel, BorderLayout.SOUTH);
        new ResourceLoader<List<ProjectInfo>>(owner == null ? this : owner, statusPanel,
                loading -> projectComboBox.setEnabled(!loading),
                () -> ProjectService.INSTANCE.getProjectInfos(), projectInfos -> {
                    updating = true;
                    try {
                        projectComboBox.removeAllItems();
                        String current = ProjectService.INSTANCE.getCurrentProjectName();
                        boolean currentAvailable = projectInfos.stream().anyMatch(info -> info.getName().equals(current));
                        if (!currentAvailable) projectComboBox.addItem("");
                        for (ProjectInfo info : projectInfos) projectComboBox.addItem(info.getName());
                        projectComboBox.setSelectedItem(currentAvailable ? current : "");
                    } finally {
                        updating = false;
                    }
                }).load();
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
        statusPanel.removeAll();
        statusPanel.revalidate();
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
        updating = true;
        try {
            String current = ProjectService.INSTANCE.getCurrentProjectName();
            if (current.isBlank()) projectComboBox.insertItemAt("", 0);
            projectComboBox.setSelectedItem(current);
        } finally {
            updating = false;
        }
        statusPanel.removeAll();
        LOG.warn("Could not switch to project '" + selected + "'", failure);
        JTextArea error = new JTextArea(failure.getMessage() == null ? "Could not switch project." : failure.getMessage());
        error.setEditable(false);
        error.setLineWrap(true);
        error.setWrapStyleWord(true);
        error.setOpaque(false);
        statusPanel.add(error);
        statusPanel.revalidate();
        statusPanel.repaint();
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
