package net.nightvision.plugin.auth;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBTextArea;
import net.nightvision.plugin.Constants;
import net.nightvision.plugin.Screen;
import net.nightvision.plugin.utils.IconUtils;
import net.nightvision.plugin.models.AuthInfo;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Objects;

public class AuthenticationDetailsScreen extends Screen {
    private JButton backButton;
    private JPanel authenticationDetailsPanel;
    private JPanel detailsPanel;

    public JPanel getAuthenticationDetailsPanel() {
        return authenticationDetailsPanel;
    }

    public AuthenticationDetailsScreen(Project project, AuthInfo authInfo) {
        super(project);

        backButton.addActionListener(e -> {
            mainWindowFactory.openAuthenticationsPage();
        });
        backButton.setIcon(IconUtils.getIcon("/icons/back.svg", 1f));
        backButton.setBorder(null);
        backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        BoxLayout boxLayout = new BoxLayout(detailsPanel, BoxLayout.Y_AXIS);
        detailsPanel.setLayout(boxLayout);


        String[] keysList = { "Authentication Name:", "Project:", "Authentication Type:", "Authentication ID:", "Target URL:",
                "Date Created:", "Latest Updated:", "Description:", "Check in Browser:", "Authentication Script:"};

        HashMap<Integer, String> authDetailsDictionary = getAuthDetailsHashMap(authInfo);

        for (Integer i : authDetailsDictionary.keySet()) {
            String key = keysList[i];
            var content = authDetailsDictionary.get(i);
            JPanel propertyPanel = new JPanel();
            BoxLayout layout = new BoxLayout(propertyPanel, BoxLayout.Y_AXIS);
            propertyPanel.setLayout(layout);

            JLabel label = new JLabel(key);
            label.setFont(label.getFont().deriveFont(Font.BOLD));
            propertyPanel.add(label);


            if (i == 9) {
                detailsPanel.add(propertyPanel);
                propertyPanel = new JPanel();
                layout = new BoxLayout(propertyPanel, BoxLayout.Y_AXIS);
                propertyPanel.setLayout(layout);
                JBTextArea value = new JBTextArea(content);
                propertyPanel.add(value);
                detailsPanel.add(propertyPanel);
                continue;
            }

            JLabel value = new JLabel(content);
            value.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));

            if ("Check in Browser:".equals(key)) {
                value.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                value.setToolTipText("Click to open in browser");
                value.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        try {
                            URI uri = Constants.Companion.getAppUrlFor("authentications/" + authInfo.getId());
                            Desktop.getDesktop().browse(uri);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            }

            propertyPanel.add(value);
            detailsPanel.add(propertyPanel);
        }

        for (Component component : detailsPanel.getComponents()) {
            if (component instanceof JComponent jComponent) {
                jComponent.setAlignmentX(Component.LEFT_ALIGNMENT);
            }
        }
    }

    @NotNull
    private HashMap<Integer, String> getAuthDetailsHashMap(AuthInfo authInfo) {
        HashMap<Integer, String> authDetailsDictionary = new HashMap<>();
        authDetailsDictionary.put(0, authInfo.getName());
        authDetailsDictionary.put(1, authInfo.getProjectName());
        var t = authInfo.getType();
        authDetailsDictionary.put(2, switch(t) {
            case "COOKIE" -> "Cookie";
            case "HEADER" -> "Header";
            case "SCRIPT" -> "Playwright";
            default -> t;
        });
        authDetailsDictionary.put(3, authInfo.getId());
        authDetailsDictionary.put(4, authInfo.getUrl());
        authDetailsDictionary.put(5, ZonedDateTime.parse(authInfo.getCreatedAt())
                .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm:ss a")));
        if (authInfo.getLastUpdatedAt() != null) {
            authDetailsDictionary.put(6, ZonedDateTime.parse(authInfo.getLastUpdatedAt())
                    .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm:ss a")));
        } else {
            authDetailsDictionary.put(6, "N/A");
        }

        authDetailsDictionary.put(7, Objects.requireNonNullElse(authInfo.getDescription(), "N/A"));

        authDetailsDictionary.put(8, "➡\uFE0F View Authentication");

        if (authInfo.getScriptContent() != null) {
            authDetailsDictionary.put(9, authInfo.getScriptContent());
        }

        return authDetailsDictionary;
    }
}
