package net.nightvision.plugin.intellij;

import javax.swing.*;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import net.nightvision.plugin.intellij.services.ApiDiscoveryService;

import java.awt.*;

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
    private JLabel resultLabel;
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
        backButton.addActionListener(e -> mainWindow.openOverviewPage());
        uploadButton.addActionListener(e -> openFileDialog());
        submitButton.addActionListener(e -> {
            String lang = apiLangCombobox.getSelectedItem().toString();
            String dirPath = pathToDirectory.getText();
            new ExtractWorker(dirPath, lang).execute();
        });

        initCombobox();

        pathPanel.add(pathInputPanel, BorderLayout.CENTER);
        pathUploadPanel.setPreferredSize(null);
        pathPanel.add(pathUploadPanel, BorderLayout.EAST);

        pathPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, pathPanel.getPreferredSize().height));
        languagePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, languagePanel.getPreferredSize().height));
        submitPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, submitPanel.getPreferredSize().height));

        backButtonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, pathPanel.getPreferredSize().height));
        loadingPanel = new Loading().getLoadingPanel();

        apiDiscoveryPanel.add(backButtonPanel);
        apiDiscoveryPanel.add(pathPanel);
        apiDiscoveryPanel.add(languagePanel);
        apiDiscoveryPanel.add(submitPanel);
        apiDiscoveryPanel.add(loadingPanel);
    }

    private void openFileDialog() {
        FileChooserDescriptor descriptor = new FileChooserDescriptor(false,true,false,false,false,false)
            .withTitle("Select Repository")
            .withDescription("Choose a repository to open");

        VirtualFile selectedDir = FileChooser.chooseFile(descriptor, project, null);
        if (selectedDir != null) {
            System.out.println(selectedDir.getPath());
            System.out.println(pathToDirectory.getText());
            System.out.println(apiLangCombobox.getSelectedItem());

            pathToDirectory.setText(selectedDir.getPath());
        }
    }
    
    private void initCombobox() {
        for (String lang : LANGUAGES) {
            apiLangCombobox.addItem(lang);
        }
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
            return ApiDiscoveryService.INSTANCE.extract(dirPath, lang);
        }

        @Override
        protected void done() {
            try {
                ApiDiscoveryService.ApiDiscoveryResults result = get();

                System.out.println(result);
                resultLabel.setText("Paths: " + result.getPath() + "Classes: " + result.getClasses());
                resultsPanel.setVisible(true);
                apiDiscoveryPanel.remove(loadingPanel);
                apiDiscoveryPanel.add(resultsPanel);
                apiDiscoveryPanel.revalidate();

            } catch (Exception ignore) {
            }
        }
    }
}
