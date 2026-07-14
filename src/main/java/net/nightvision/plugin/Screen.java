package net.nightvision.plugin;

import com.intellij.openapi.project.Project;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;

public abstract class Screen {
    protected final MainWindowFactory mainWindowFactory;
    protected final Project project;

    // Left indent applied to a detail value so it sits under its field name.
    protected static final int DETAIL_INDENT = 20;

    public Screen (Project project) {
        this.project = project;
        this.mainWindowFactory = project.getService(MainWindowService.class).getWindowFactory();
    }

    // pad is scaled, so the padding tracks the IDE's display scaling the way the
    // button's own text and icon do.
    protected static void addButtonPadding(JButton button, int pad) {
        button.setBorder(BorderFactory.createCompoundBorder(
                button.getBorder(),
                JBUI.Borders.empty(pad)
        ));
    }

    /**
     * Appends a detail row to a vertically stacked (BoxLayout Y_AXIS) container,
     * styled to match the VS Code plugin: the field name in bold on its own line,
     * with the value component indented on the line below, followed by a gap before
     * the next row.
     */
    protected static void addDetailRow(JPanel container, String name, JComponent value) {
        addDetailRow(container, name, value, true);
    }

    /**
     * As {@link #addDetailRow(JPanel, String, JComponent)}, but lets the caller
     * suppress the value's left indent. Pass false for a value that supplies its
     * own border or that should span the full width, such as a text area.
     */
    protected static void addDetailRow(JPanel container, String name, JComponent value, boolean indent) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(name);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(label);
        row.add(Box.createVerticalStrut(JBUI.scale(3)));

        value.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (indent) {
            value.setBorder(JBUI.Borders.emptyLeft(DETAIL_INDENT));
        }
        row.add(value);

        container.add(row);
        container.add(Box.createVerticalStrut(JBUI.scale(12)));
    }
}
