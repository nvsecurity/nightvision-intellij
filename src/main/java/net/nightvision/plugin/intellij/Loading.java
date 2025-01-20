package net.nightvision.plugin.intellij;

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

        Icon icon = Utils.getIcon("/icons/nightvision.svg", 3f);
        JLabel loadingLabel = new JLabel("Loading...", icon, JLabel.LEFT);

        loadingPanel.add(loadingLabel);
        loadingPanel.setPreferredSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }
}
