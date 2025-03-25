package net.nightvision.plugin.intellij.utils;

import javax.swing.*;
import java.awt.*;

@FunctionalInterface
public interface ColumnDecorator {
    /**
     * Apply extra behavior to the rendered component.
     * @param table the JTable
     * @param component the component returned by the base renderer so far
     * @param value the cell value
     * @param isSelected whether the cell is selected
     * @param hasFocus whether the cell has focus
     * @param row the row index
     * @param column the column index
     */
    void decorate(JTable table, Component component, Object value,
                  boolean isSelected, boolean hasFocus, int row, int column);
}