package net.nightvision.plugin;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import net.nightvision.plugin.exceptions.CommandNotFoundException;
import net.nightvision.plugin.exceptions.NotLoggedException;
import net.nightvision.plugin.exceptions.PermissionDeniedException;
import net.nightvision.plugin.services.ApiDiscoveryService;
import net.nightvision.plugin.utils.IconUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.concurrent.ExecutionException;

import static net.nightvision.plugin.Constants.CONTACT_EMAIL;

public class ApiDiscovery extends Screen {
    private JPanel apiDiscoveryPanel;
    private JTextField pathToDirectory;
    private JButton uploadButton;
    private JPanel pathPanel;
    private JComboBox<String> apiLangCombobox;
    private JPanel pathInputPanel;
    private JPanel pathUploadPanel;
    private JButton submitButton;
    private JPanel languagePanel;
    private JPanel submitPanel;
    private JPanel resultsPanel;
    private JButton backButton;
    private JPanel backButtonPanel;
    private JPanel loadingPanel;

    private final String[] LANGUAGES = new String[] {
        "Java",
        "C#",
        "Python",
        "JavaScript",
        "Ruby",
    };

    public JPanel getApiDiscoveryPanel() {
        return apiDiscoveryPanel;
    }

    public ApiDiscovery(Project project) {
        super(project);
        apiDiscoveryPanel.setLayout(new BoxLayout(apiDiscoveryPanel, BoxLayout.Y_AXIS));
        apiDiscoveryPanel.removeAll();
        resultsPanel.setLayout(new FlowLayout(FlowLayout. LEFT));

        backButton.setIcon(IconUtils.getIcon("/icons/back.svg", 1f));
        backButton.setBorder(null);
        backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backButton.addActionListener(e -> mainWindowFactory.openOverviewPage());
        uploadButton.setIcon(IconUtils.getIcon("/icons/custom-file-select.svg", 1f));
        uploadButton.setBorder(null);
        uploadButton.addActionListener(e -> openFileDialog());
        uploadButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        submitButton.addActionListener(e -> {
            String lang = apiLangCombobox.getSelectedItem().toString();
            String dirPath = pathToDirectory.getText();

            loadingPanel.setVisible(true);
            apiDiscoveryPanel.add(loadingPanel);

            resultsPanel.setVisible(false);
            resultsPanel.removeAll();

            enableEditing(false);

            apiDiscoveryPanel.revalidate();

            new ExtractWorker(dirPath, lang).execute();
        });
        submitButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        pathToDirectory.getDocument().addDocumentListener(new DocumentListener() {
            private void updateButtonState() {
                submitButton.setEnabled(!pathToDirectory.getText().trim().isEmpty());
                submitPanel.revalidate();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                updateButtonState();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateButtonState();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateButtonState();
            }
        });

        initCombobox();

        pathPanel.add(pathInputPanel, BorderLayout.CENTER);
        pathUploadPanel.setPreferredSize(null);
        pathPanel.add(pathUploadPanel, BorderLayout.EAST);

        pathPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, pathPanel.getPreferredSize().height));
        languagePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, languagePanel.getPreferredSize().height));
        submitPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, submitPanel.getPreferredSize().height));

        backButtonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, backButtonPanel.getPreferredSize().height));
        loadingPanel = new Loading().getLoadingPanel();
        loadingPanel.setVisible(false);

        apiDiscoveryPanel.add(backButtonPanel);
        apiDiscoveryPanel.add(pathPanel);
        apiDiscoveryPanel.add(languagePanel);
        apiDiscoveryPanel.add(submitPanel);
        apiDiscoveryPanel.add(loadingPanel);
        apiDiscoveryPanel.add(resultsPanel);
    }

    private void openFileDialog() {
        FileChooserDescriptor descriptor = new FileChooserDescriptor(false,true,false,false,false,false)
            .withTitle("Select Repository")
            .withDescription("Choose a repository to open");

        VirtualFile selectedDir = FileChooser.chooseFile(descriptor, project, null);
        if (selectedDir != null) {
            pathToDirectory.setText(selectedDir.getPath());
        }
    }
    
    private void initCombobox() {
        for (String lang : LANGUAGES) {
            apiLangCombobox.addItem(lang);
        }
    }

    private void enableEditing(Boolean enabled) {
        uploadButton.setEnabled(enabled);
        submitButton.setEnabled(enabled);
        pathToDirectory.setEnabled(enabled);
        apiLangCombobox.setEnabled(enabled);
    }

    private class ExtractWorker extends SwingWorker<ApiDiscoveryService.ApiDiscoveryResults, Void> {
        private final String dirPath;
        private final String lang;

        public ExtractWorker(String dirPath, String lang) {
            this.dirPath = dirPath;
            this.lang = lang;
        }

        @Override
        protected ApiDiscoveryService.ApiDiscoveryResults doInBackground() throws Exception {
            return ApiDiscoveryService.INSTANCE.extract(dirPath, lang, project);
        }

        @Override
        protected void done() {
            resultsPanel.setVisible(true);
            try {
                ApiDiscoveryService.ApiDiscoveryResults result = get();

                System.out.println(result);
                JPanel panel = new JPanel();
                panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

                JLabel pathResults = new JLabel("Number of discovered paths: " + result.getPath());
                JLabel classResults = new JLabel("Number of discovered classes: " + result.getClasses());

                panel.add(pathResults);
                panel.add(classResults);

                resultsPanel.add(panel);

            } catch (ExecutionException ex) {
                var cause = ex.getCause();
                if (cause instanceof CommandNotFoundException) {
                    mainWindowFactory.openInstallCLIPage();
                    return;
                } else if (cause instanceof PermissionDeniedException) {
                    JPanel errorPanel = getErrorPanel(ex.getMessage());
                    resultsPanel.add(errorPanel);
                } else if (cause instanceof NotLoggedException) {
                    mainWindowFactory.openLoginPage();
                    return;
                } else {
                    JPanel errorPanel = getErrorPanel("<html>Error extracting API info. Please recheck the entered Path to the Root <br>Directory and selected Language, then try again.<br>Details: " + cause.getClass().getName() + " - " + cause.getMessage() + "</html>");
                    resultsPanel.add(errorPanel);
                }
            } catch (Exception ex) {
                JPanel errorPanel = getErrorPanel("<html>Error extracting API info. Please recheck the entered Path to the Root <br>Directory and selected Language, then try again.<br>Details: " + ex.getClass().getName() + " - " + ex.getMessage() + "</html>");
                resultsPanel.add(errorPanel);
            }

            enableEditing(true);

            apiDiscoveryPanel.remove(loadingPanel);
            apiDiscoveryPanel.revalidate();
        }

        @NotNull
        private static JPanel getErrorPanel(String errorMessage) {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel emailLabel = getEmailPanel();
            JPanel errorLabel = getErrorLabelPanel(errorMessage);
            emailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            panel.add(errorLabel);
            panel.add(emailLabel);

            return panel;
        }

        @NotNull
        private static JPanel getEmailPanel() {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

            JLabel label = new JLabel("If the problem persists, contact us at ");
            label.setForeground(JBColor.red);

            JLabel emailLabel = new JLabel(String.format("<html><a href=''>%s</a></html>", CONTACT_EMAIL));
            emailLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            emailLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    try {
                        Desktop desktop = Desktop.getDesktop();
                        if (desktop.isSupported(Desktop.Action.MAIL)) {
                            URI mailto = new URI("mailto:support@nightvision.net?subject=Subject&body=Write your email...");
                            desktop.mail(mailto);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });

            panel.add(label);
            panel.add(emailLabel);
            return panel;
        }

        @NotNull
        private static JPanel getErrorLabelPanel(String errorMessage) {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
            panel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel label = new JLabel(errorMessage);
            label.setForeground(JBColor.red);

            panel.add(label);
            return panel;
        }
    }
}
