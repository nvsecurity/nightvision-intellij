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

    // Width a wrapped error message is capped at, so a long CLI line wraps
    // instead of stretching the tool window.
    protected static final int MESSAGE_WRAP_WIDTH = 320;

    /**
     * Whether [panel] is still mounted in the tool window.
     *
     * Navigation replaces the tool window's contents wholesale, so a screen the
     * user has left has no parent, while one whose tool window is merely hidden,
     * collapsed or unselected still does. AWT visibility is the wrong test:
     * isShowing() also goes false when the user switches to another tool window,
     * and a background result arriving then is still wanted.
     */
    protected static boolean isMounted(JComponent panel) {
        return panel.getParent() != null;
    }

    /**
     * Renders multi-line text in a JLabel. Swing labels draw plain text on one
     * line and ignore newlines, so CLI output shown to the user is escaped and
     * converted to HTML. Without this a reported failure is truncated to its
     * first line, which is rarely the line carrying the reason (NV-4827).
     */
    protected static String asWrappedHtml(String text) {
        String escaped = text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>");
        return "<html><body style='width: " + JBUI.scale(MESSAGE_WRAP_WIDTH) + "px'>"
                + escaped + "</body></html>";
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
