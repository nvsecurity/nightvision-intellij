package net.nightvision.plugin.intellij.utils;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class RendererAggregator implements TableCellRenderer {
    private final TableCellRenderer baseRenderer;
    private final int[] hoveredRow;  // pointer to shared hovered-row array
    private final Color hoverColor;
    private final Map<Integer, java.util.List<ColumnDecorator>> decoratorsByColumn;

    public RendererAggregator(TableCellRenderer baseRenderer,
                              int[] hoveredRow,
                              Color hoverColor,
                              Map<Integer, java.util.List<ColumnDecorator>> decoratorsByColumn) {
        this.baseRenderer = baseRenderer;
        this.hoveredRow = hoveredRow;
        this.hoverColor = hoverColor;
        this.decoratorsByColumn = decoratorsByColumn;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        // 1) Let the base renderer create the component
        Component comp = baseRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        // 2) Apply row-hover highlighting (unless selected)
        if (!isSelected && row == hoveredRow[0]) {
            comp.setBackground(hoverColor);
        } else {
            comp.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
        }

        // 3) Apply any column-specific decorators
        List<ColumnDecorator> decorators = decoratorsByColumn.get(column);
        if (decorators != null) {
            for (ColumnDecorator decorator : decorators) {
                decorator.decorate(table, comp, value, isSelected, hasFocus, row, column);
            }
        }
        return comp;
    }
}
