package net.nightvision.plugin.intellij;

import com.intellij.openapi.project.Project;

import javax.swing.*;
import java.awt.*;

public class ApiAndWebTestingScreen extends Screen {
    private JPanel apiAndWebTestingPanel;

    public JPanel getApiAndWebTestingPanel() {
        return apiAndWebTestingPanel;
    }

    public ApiAndWebTestingScreen(Project project) {
        super(project);
    }

    private void createUIComponents() {
        JLabel titleLabel = new JLabel("API And Web Security Testing");
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        apiAndWebTestingPanel = new JPanel();
        apiAndWebTestingPanel.setLayout(new BoxLayout(apiAndWebTestingPanel, BoxLayout.Y_AXIS));

        JButton button1 = new JButton("Scans");
        JButton button2 = new JButton("Targets");
        button1.addActionListener(e ->  mainWindow.openScansPage());
//        button1.addActionListener(e ->  mainWindow.openScansPage());
        JPanel firstRowPanel = createPanelWithHorizontalButtons(new JButton[]{ button1, button2 });

        JButton button3 = new JButton("Authentications");
        JButton button4 = new JButton("Projects");
//        button3.addActionListener(e ->  mainWindow.openApiAndWebTestingPage());
//        button4.addActionListener(e ->  mainWindow.openScansPage());
        JPanel secondRowPanel = createPanelWithHorizontalButtons(new JButton[]{ button3, button4 });

        apiAndWebTestingPanel.add(titleLabel);
        apiAndWebTestingPanel.add(firstRowPanel);
        apiAndWebTestingPanel.add(secondRowPanel);
    }

    private JPanel createPanelWithHorizontalButtons(JButton[] buttons) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        for (JButton button : buttons) {
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, button.getPreferredSize().height));
            panel.add(button);
        }

        return panel;
    }
}
