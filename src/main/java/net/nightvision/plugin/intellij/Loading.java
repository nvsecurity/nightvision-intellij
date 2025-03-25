package net.nightvision.plugin.intellij;

import net.nightvision.plugin.intellij.utils.IconUtils;

import javax.swing.*;
import java.awt.*;

public class Loading {
    private final JPanel loadingPanel;

    public JPanel getLoadingPanel() {
        return loadingPanel;
    }

    public Loading() {
        loadingPanel = new JPanel();
        loadingPanel.setLayout(new BoxLayout(loadingPanel, BoxLayout.Y_AXIS));
        loadingPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        Icon icon = IconUtils.getIcon("/icons/nightvision.svg", 5f);
        JLabel loadingLabel = new JLabel("Loading...", icon, JLabel.LEFT);
        loadingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        loadingPanel.setPreferredSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        loadingPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        loadingPanel.add(loadingLabel);
    }
}
