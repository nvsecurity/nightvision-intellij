package net.nightvision.plugin.intellij;

import com.intellij.openapi.project.Project;

import javax.swing.*;
import java.awt.*;

public class ApiAndWebTestingScreen extends Screen {
    private JPanel apiAndWebTestingPanel;
    private JPanel titlePanel;
    private JButton backButton;
    private JLabel title;

    public JPanel getApiAndWebTestingPanel() {
        return apiAndWebTestingPanel;
    }

    public ApiAndWebTestingScreen(Project project) {
        super(project);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(backButton);
        panel.add(titlePanel);
        titlePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, titlePanel.getPreferredSize().height));
//        apiAndWebTestingPanel = new JPanel();
        apiAndWebTestingPanel.removeAll();
        apiAndWebTestingPanel.revalidate();
        apiAndWebTestingPanel.setLayout(new BoxLayout(apiAndWebTestingPanel, BoxLayout.Y_AXIS));

        backButton.addActionListener(e -> mainWindow.openOverviewPage());

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

        apiAndWebTestingPanel.add(panel);
        apiAndWebTestingPanel.add(firstRowPanel);
        apiAndWebTestingPanel.add(secondRowPanel);
        apiAndWebTestingPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, apiAndWebTestingPanel.getPreferredSize().height));
    }

//    private void createUIComponents() {
//
//    }

    private JPanel createPanelWithHorizontalButtons(JButton[] buttons) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        for (JButton button : buttons) {
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, button.getPreferredSize().height));
            panel.add(button);
        }
        panel.setPreferredSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));

        return panel;
    }
}
