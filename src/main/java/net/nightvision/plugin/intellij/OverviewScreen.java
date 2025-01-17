package net.nightvision.plugin.intellij;

import javax.swing.*;
import java.awt.*;
import com.intellij.openapi.project.Project;

public class OverviewScreen extends Screen {
    private JPanel overviewPanel;

    public JPanel getOverviewPanel() {
        return overviewPanel;
    }

    public OverviewScreen(Project project) {
        super(project);
    }

    private void createUIComponents() {
        JPanel panel1 = new JPanel();
        panel1.setLayout(new BoxLayout(panel1, BoxLayout.X_AXIS));
        panel1.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton button1 = new JButton("API Discovery");
        JButton button2 = new JButton("API and Web Security Testing");
        button1.addActionListener(e ->  mainWindow.openApiDiscoveryPage());
        button2.addActionListener(e ->  mainWindow.openApiAndWebTestingPage());
        button1.setMaximumSize(new Dimension(Integer.MAX_VALUE, button1.getPreferredSize().height));
        button2.setMaximumSize(new Dimension(Integer.MAX_VALUE, button2.getPreferredSize().height));

        panel1.add(button1);
        panel1.add(button2);

        JLabel titleLabel = new JLabel("Overview");
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        overviewPanel = new JPanel();
        overviewPanel.setLayout(new BoxLayout(overviewPanel, BoxLayout.Y_AXIS));
        overviewPanel.add(titleLabel);
        overviewPanel.add(panel1);
    }
}
