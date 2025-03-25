package net.nightvision.plugin.intellij.utils;

import com.intellij.ui.JBColor;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import static net.nightvision.plugin.intellij.utils.TableUtils.addHoverEffects;

public class TableUtils {

    // We’ll track, per table, a single “hover info” plus a shared map of decorators.
    private static final Map<JTable, int[]> hoveredRowMap = new WeakHashMap<>();
    private static final Map<JTable, Map<Integer, List<ColumnDecorator>>> decoratorMap = new WeakHashMap<>();

    /**
     * Enables row-hover on the given table with a specified hover color.
     * Wraps each column's renderer in a RendererAggregator that also checks
     * for column decorators. Must be called once before adding decorators.
     */
    public static void addHoverSupport(JTable table, Color hoverColor) {
        // If we’ve already installed for this table, skip
        if (hoveredRowMap.containsKey(table)) return;

        final int[] hoveredRow = { -1 };
        hoveredRowMap.put(table, hoveredRow);

        // Also initialize an empty decorator map for this table, if not present
        if (!decoratorMap.containsKey(table)) {
            decoratorMap.put(table, new HashMap<>());
        }

        // Mouse listeners to track hovered row
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
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                hoveredRow[0] = -1;
                table.setCursor(Cursor.getDefaultCursor());
                table.repaint();
            }
        });

        // Wrap each column’s existing renderer
        installAggregators(table, hoverColor);
    }

    /**
     * Adds a decorator function for a specific column. If the aggregator is already installed,
     * it re-installs the aggregator so that the new decorator is used.
     */
    public static void addColumnDecorator(JTable table, int columnIndex, ColumnDecorator decorator) {
        decoratorMap.computeIfAbsent(table, t -> new HashMap<>());
        Map<Integer, List<ColumnDecorator>> colDecorators = decoratorMap.get(table);
        colDecorators.computeIfAbsent(columnIndex, c -> new ArrayList<>()).add(decorator);

        // If we already installed aggregator for hover, re-install to update
        if (hoveredRowMap.containsKey(table)) {
            Color hoverColor = getCurrentHoverColor(table); // see helper below
            installAggregators(table, hoverColor);
        }
    }

    /**
     * Actually wraps each column's renderer in a RendererAggregator, using the stored decorators.
     */
    private static void installAggregators(JTable table, Color hoverColor) {
        int[] hoveredRow = hoveredRowMap.get(table);
        Map<Integer, List<ColumnDecorator>> colDecorators = decoratorMap.get(table);

        TableColumnModel colModel = table.getColumnModel();
        for (int i = 0; i < colModel.getColumnCount(); i++) {
            TableColumn col = colModel.getColumn(i);
            TableCellRenderer baseRenderer = col.getCellRenderer();
            if (baseRenderer == null) {
                baseRenderer = table.getDefaultRenderer(table.getColumnClass(i));
            }
            RendererAggregator aggregator = new RendererAggregator(
                    baseRenderer, hoveredRow, hoverColor, colDecorators
            );
            col.setCellRenderer(aggregator);
        }
    }

    /**
     * If needed, you can store the hover color in another map. For simplicity, let's just re-derive from the aggregator:
     * Or we can store it in a separate table->color map. For now, let's assume we only call addHoverSupport once with one color.
     */
    private static Color getCurrentHoverColor(JTable table) {
        // For a real design, store the color in a separate map.
        // Or if you only call addHoverSupport once, you can store it in a field.
        return new Color(220, 220, 255); // Default fallback
    }

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
