package net.nightvision.plugin.intellij;

import com.intellij.openapi.util.IconLoader;
import com.intellij.util.IconUtil;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Map;
import java.util.WeakHashMap;

public final class Utils {
    public static Icon getIcon(String path, Float scale) {
        Icon icon = IconLoader.getIcon(path, Utils.class);
        return IconUtil.scale(icon, null, scale);
    }

    public static Icon getIcon(String path) {
        return IconLoader.getIcon(path, Utils.class);
    }
}
