package net.nightvision.plugin.project;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import net.nightvision.plugin.Screen;

import javax.swing.*;
import java.awt.*;

/** NV-6990: keep a valid login usable when its saved project cannot be opened. */
public final class ProjectRecoveryScreen extends Screen {
    private final JPanel panel = new JPanel(new BorderLayout());

    public ProjectRecoveryScreen(Project project, String reason) {
        super(project);
        panel.setBorder(JBUI.Borders.empty(8));
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        panel.add(content, BorderLayout.NORTH);

        JBLabel message = new JBLabel(asWrappedHtml(reason));
        makeSelectable(message);
        message.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(message);
        content.add(Box.createVerticalStrut(JBUI.scale(12)));

        ProjectSelectionPanel selection = new ProjectSelectionPanel(mainWindowFactory,
                selected -> {
                    if (isMounted(panel)) mainWindowFactory.openOverviewPage();
                }, panel);
        selection.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(selection);
        content.add(Box.createVerticalStrut(JBUI.scale(12)));

        JButton create = new JButton("Create project");
        addButtonPadding(create, 6);
        create.addActionListener(event -> mainWindowFactory.openProjectCreatePage());
        content.add(create);
        JButton overview = new JButton("Continue without a project");
        addButtonPadding(overview, 6);
        overview.addActionListener(event -> mainWindowFactory.openOverviewPage());
        content.add(overview);
    }

    public JPanel getPanel() {
        return panel;
    }
}
