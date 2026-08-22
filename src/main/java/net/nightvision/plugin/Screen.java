package net.nightvision.plugin;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
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
        String[] lines = text.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            lines[i] = escapeHtml(lines[i]);
        }
        return wrapHtml(lines);
    }

    // Text the caller does not control, made safe to drop into a label's HTML.
    // Line breaks are not this method's business: wrapHtml gives each line its
    // own element, so callers split first and escape each line.
    private static String escapeHtml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /**
     * Assembles one label's worth of HTML, a block element per line, capped at
     * a readable width so long text wraps instead of stretching the tool
     * window.
     *
     * The lines are blocks rather than <br> separators because these labels are
     * selectable (see makeSelectable) and the two copy differently: Swing turns
     * a <br> into a space, so a three-line CLI failure pasted into a support
     * ticket would arrive as one run-on line, while a block element copies as a
     * real newline.
     */
    private static String wrapHtml(String... lineHtml) {
        StringBuilder body = new StringBuilder();
        for (String line : lineHtml) {
            // An empty div collapses to nothing, so a blank line keeps a <br>
            // to hold its height.
            body.append("<div>").append(line.isEmpty() ? "<br>" : line).append("</div>");
        }
        return "<html><body style='width: " + JBUI.scale(MESSAGE_WRAP_WIDTH) + "px'>"
                + body + "</body></html>";
    }

    /**
     * The reported text of a failure, wrapped for a JLabel. An exception with
     * no message of its own falls back to its toString, so the label never
     * renders blank where a reason was expected.
     */
    protected static String asWrappedError(Throwable t) {
        String message = t.getMessage();
        return asWrappedHtml(message == null || message.isBlank() ? t.toString() : message);
    }

    /**
     * Tooltip for the Update CLI button, naming the CLI it means. The plugin
     * prepends its own install directory to PATH, so the binary it runs is not
     * necessarily the one the user's terminal resolves; without the path here,
     * "your CLI is out of date" gives no way to tell which CLI is meant
     * (NV-4873).
     */
    protected static String cliUpdateTooltip(String version, String resolvedPath, String required) {
        StringBuilder text = new StringBuilder();
        text.append("NightVision CLI ").append(version.isBlank() ? "(unknown version)" : version);
        if (resolvedPath != null && !resolvedPath.isBlank()) {
            text.append("\n").append(resolvedPath);
        }
        text.append("\nThe plugin needs ").append(required).append(" or newer.");
        return asWrappedHtml(text.toString());
    }

    /**
     * The warning shown under the Update CLI button, wording it as the VS Code
     * plugin does under its own. The tooltip above carries the same facts, but
     * only reaches a user who already suspects something is wrong and hovers to
     * find out; an out-of-date CLI is worth saying on the screen (NV-4873).
     */
    protected static String cliUpdateMessage(String version, String resolvedPath, String required) {
        String mismatch = "<b>You have version "
                + escapeHtml(version.isBlank() ? "(unknown)" : version)
                + " of the NightVision CLI, but the plugin requires version "
                + escapeHtml(required)
                + " to be fully operational.</b>";
        if (resolvedPath == null || resolvedPath.isBlank()) {
            return wrapHtml(mismatch);
        }
        return wrapHtml(mismatch, "Using " + escapeHtml(resolvedPath));
    }

    /**
     * Lets the user select and copy a message label's text. Swing draws a
     * JLabel as an image with no caret and no selection, so a CLI failure shown
     * in one can only be screenshotted or retyped, which is the wrong thing to
     * ask of someone reporting a problem to support. setCopyable swaps in a
     * text component that renders the same HTML and allows selection.
     */
    protected static void makeSelectable(JBLabel... labels) {
        for (JBLabel label : labels) {
            // Ordered: setCopyable builds the editor pane's stylesheet once and
            // reads this flag while doing it, and the flag's setter does not
            // restyle. Left at its default the rule carries white-space:nowrap
            // on body, the element asWrappedHtml caps the width of, so a long
            // CLI line would run off the side of the tool window.
            label.setAllowAutoWrapping(true);
            label.setCopyable(true);
        }
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
