package net.nightvision.plugin.intellij;

import com.intellij.openapi.util.IconLoader;
import com.intellij.util.IconUtil;

import javax.swing.*;

public final class Utils {
    public static Icon getIcon(String path, Float scale) {
        Icon icon = IconLoader.getIcon(path, Utils.class);
        return IconUtil.scale(icon, null, scale);
    }

    public static Icon getIcon(String path) {
        return IconLoader.getIcon(path, Utils.class);
    }
}
