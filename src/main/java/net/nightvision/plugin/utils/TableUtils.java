package net.nightvision.plugin.utils;

import com.intellij.ui.JBColor;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;

public class TableUtils {

    /**
     * Adds hover effects to the given table.
     * When hovering a row, its background changes to the specified hoverColor,
     * and the cursor changes to a hand cursor.
     *
     * @param table the JTable to add hover effects to
     * @param hoverColor the background color to use for hovered rows
     */
    public static void addHoverEffects(JTable table, JBColor hoverColor) {
        // Use an array as a mutable container for the hovered row index.
        final int[] hoveredRow = { -1 };

        // Add a mouse motion listener to update hoveredRow and the cursor.
        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row != hoveredRow[0]) {
                    hoveredRow[0] = row;
                    table.repaint();
                }
                table.setCursor(row >= 0
                        ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        : Cursor.getDefaultCursor());
            }
        });

        // When the mouse exits the table, reset hoveredRow.
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                hoveredRow[0] = -1;
                table.setCursor(Cursor.getDefaultCursor());
                table.repaint();
            }
        });

        // For each column, wrap the current cell renderer with our custom renderer.
        TableColumnModel columnModel = table.getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            TableColumn col = columnModel.getColumn(i);
            TableCellRenderer originalRenderer = col.getCellRenderer();
            // If no renderer is set, get the default renderer for the column's class.
            if (originalRenderer == null) {
                originalRenderer = table.getDefaultRenderer(table.getColumnClass(i));
            }

            // Make sure it's final (or effectively final) so the inner class can reference it.
            final TableCellRenderer finalOriginalRenderer = originalRenderer;

            col.setCellRenderer((tbl, value, isSelected, hasFocus, row, column) -> {
                Component comp = finalOriginalRenderer.getTableCellRendererComponent(
                        tbl, value, isSelected, hasFocus, row, column
                );
                if (!isSelected && row == hoveredRow[0]) {
                    // TODO: have to fix hoverColor for some cells, or not consider at all..
//                    comp.setBackground(hoverColor);
                } else {
                    comp.setBackground(isSelected
                            ? tbl.getSelectionBackground()
                            : tbl.getBackground());
                }
                return comp;
            });
        }
    }

}
