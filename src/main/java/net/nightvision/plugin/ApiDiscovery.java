package net.nightvision.plugin;

import javax.swing.*;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import net.nightvision.plugin.exceptions.CommandNotFoundException;
import net.nightvision.plugin.exceptions.NotLoggedException;
import net.nightvision.plugin.exceptions.PermissionDeniedException;
import com.intellij.icons.AllIcons;
import net.nightvision.plugin.services.ApiDiscoveryService;

import java.awt.*;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static net.nightvision.plugin.Constants.CONTACT_EMAIL;

public class ApiDiscovery extends Screen {

    private static final String[] LANGUAGES = {
        "All languages", "C#", "Go", "Java", "JavaScript/TypeScript", "PHP", "Python", "Ruby",
    };

    private static final Map<String, String> LANGUAGE_CLI_IDS = Map.of(
        "All languages", "all",
        "C#", "dotnet",
        "Go", "go",
        "Java", "java",
        "JavaScript/TypeScript", "js",
        "PHP", "php",
        "Python", "python",
        "Ruby", "ruby"
    );

    private final JPanel rootPanel;
    private final TextFieldWithBrowseButton pathField;
    private final ComboBox<String> languageCombo;
    private final JButton submitButton;
    private final JPanel resultsPanel;
    private final JPanel loadingPanel;

    public JPanel getApiDiscoveryPanel() {
        return rootPanel;
    }

    public ApiDiscovery(Project project) {
        super(project);

        // --- Back button ---
        JButton backButton = new JButton("Back", AllIcons.Actions.Back);
        backButton.setBorderPainted(false);
        backButton.setContentAreaFilled(false);
        backButton.setMargin(JBUI.emptyInsets());
        backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backButton.addActionListener(e -> mainWindowFactory.openOverviewPage());

        // --- Path field ---
        pathField = new TextFieldWithBrowseButton();
        pathField.addBrowseFolderListener(
            "Select Repository", "Choose the root directory of your API",
            project, FileChooserDescriptorFactory.createSingleFolderDescriptor()
        );
        if (project.getBasePath() != null) {
            pathField.setText(project.getBasePath());
        }

        // --- Language combo ---
        languageCombo = new ComboBox<>(LANGUAGES);

        // --- Submit button ---
        submitButton = new JButton("Generate OpenAPI Spec");
        submitButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        submitButton.addActionListener(e -> onSubmit());

        // Enable submit only when path is non-empty
        pathField.getTextField().getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void update() { submitButton.setEnabled(!pathField.getText().trim().isEmpty()); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
        });

        // --- Loading ---
        loadingPanel = new Loading().getLoadingPanel();
        // Loading declares a MAX_VALUE preferred size, which the grids and border
        // layouts it is normally dropped into clamp away. GridBag has no such
        // policy and would hand the row that height, so fall back to the size the
        // panel's own layout computes.
        loadingPanel.setPreferredSize(null);
        loadingPanel.setVisible(false);

        // --- Results ---
        resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));

        // --- Layout ---
        int pad = 4; // consistent left padding for text alignment

        JPanel form = new JPanel();
        form.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;

        // Row 0: back button
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = JBUI.insets(0, pad, 16, 0);
        form.add(backButton, gbc);

        // Row 1: path label
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(0, pad, 4, 0);
        form.add(new JLabel("Path to the root directory of your API"), gbc);

        // Row 2: path field
        gbc.gridy = 2;
        gbc.insets = JBUI.insetsBottom(12);
        form.add(pathField, gbc);

        // Row 3: language label
        gbc.gridy = 3;
        gbc.insets = JBUI.insets(0, pad, 4, 0);
        form.add(new JLabel("API Language"), gbc);

        // Row 4: language combo
        gbc.gridy = 4;
        gbc.insets = JBUI.insetsBottom(12);
        form.add(languageCombo, gbc);

        // Row 5: submit button (centered, natural width)
        gbc.gridy = 5;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = JBUI.insetsBottom(12);
        form.add(submitButton, gbc);

        // Reset for full-width left-aligned rows
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Row 6: loading
        gbc.gridy = 6;
        gbc.insets = JBUI.insets(0, pad, 0, 0);
        form.add(loadingPanel, gbc);

        // Row 7: results. This row takes the remaining vertical space, which
        // also pushes the form to the top, so no separate spacer row is needed.
        // The results panel must keep that space even when empty, so it stays
        // visible throughout: GridBag skips invisible components, and a
        // word-wrapping error message pinned to a zero-weight row is allotted
        // the height GridBag computed before the text area knew its width,
        // which clipped the last line.
        gbc.gridy = 7;
        gbc.insets = JBUI.insets(0, pad, 0, 0);
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        form.add(resultsPanel, gbc);

        rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBorder(JBUI.Borders.empty(8));
        rootPanel.add(form, BorderLayout.CENTER);
    }

    private void onSubmit() {
        String selected = (String) languageCombo.getSelectedItem();
        String lang = LANGUAGE_CLI_IDS.getOrDefault(selected, selected);
        String dirPath = pathField.getText();

        // setVisible only invalidates; without a revalidate the grid never lays the
        // spinner out, and GridBag skipped it while it was hidden, so it would keep
        // the zero bounds it has never been given and no spinner would appear.
        loadingPanel.setVisible(true);
        loadingPanel.revalidate();
        loadingPanel.repaint();
        resultsPanel.removeAll();
        resultsPanel.revalidate();
        resultsPanel.repaint();
        setEditing(false);

        new ExtractWorker(dirPath, lang).execute();
    }

    private void setEditing(boolean enabled) {
        pathField.setEnabled(enabled);
        languageCombo.setEnabled(enabled);
        submitButton.setEnabled(enabled && !pathField.getText().trim().isEmpty());
    }

    private void showResults(ApiDiscoveryService.ApiDiscoveryResults result) {
        resultsPanel.removeAll();
        if (result.getPath() == 0 && result.getClasses() == 0) {
            resultsPanel.add(makeErrorPanel(
                "No API routes found. Please recheck the path and selected language, then try again."
            ));
        } else {
            String resultText = "Discovered paths: " + result.getPath()
                + "\nDiscovered classes: " + result.getClasses();
            JTextArea resultArea = new JTextArea(resultText);
            resultArea.setFont(UIManager.getFont("Label.font"));
            resultArea.setEditable(false);
            resultArea.setOpaque(false);
            resultArea.setBackground(UIManager.getColor("Panel.background"));
            resultArea.setSelectionColor(UIManager.getColor("TextArea.selectionBackground"));
            resultArea.setSelectedTextColor(UIManager.getColor("TextArea.selectionForeground"));
            resultsPanel.add(resultArea);
        }
        resultsPanel.revalidate();
        resultsPanel.repaint();
    }

    private void showError(String message) {
        resultsPanel.removeAll();
        resultsPanel.add(makeErrorPanel(message));
        resultsPanel.revalidate();
        resultsPanel.repaint();
    }

    private static JPanel makeErrorPanel(String message) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        String fullMessage = message
            + "\n\nIf the problem persists, contact us at " + CONTACT_EMAIL;

        JTextArea text = new JTextArea(fullMessage);
        text.setFont(UIManager.getFont("Label.font"));
        text.setForeground(JBColor.RED);
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setBackground(UIManager.getColor("Panel.background"));
        text.setSelectionColor(UIManager.getColor("TextArea.selectionBackground"));
        text.setSelectedTextColor(UIManager.getColor("TextArea.selectionForeground"));
        panel.add(text);

        return panel;
    }

    private class ExtractWorker extends SwingWorker<ApiDiscoveryService.ApiDiscoveryResults, Void> {
        private final String dirPath;
        private final String lang;

        ExtractWorker(String dirPath, String lang) {
            this.dirPath = dirPath;
            this.lang = lang;
        }

        @Override
        protected ApiDiscoveryService.ApiDiscoveryResults doInBackground() throws Exception {
            return ApiDiscoveryService.INSTANCE.extract(dirPath, lang, project);
        }

        @Override
        protected void done() {
            loadingPanel.setVisible(false);
            loadingPanel.revalidate();
            loadingPanel.repaint();
            try {
                showResults(get());
            } catch (ExecutionException ex) {
                var cause = ex.getCause();
                if (cause instanceof CommandNotFoundException) {
                    mainWindowFactory.openInstallCLIPage();
                    return;
                } else if (cause instanceof PermissionDeniedException) {
                    showError("Permission denied. Check that the NightVision CLI has execute permissions.");
                } else if (cause instanceof NotLoggedException) {
                    mainWindowFactory.openLoginPage();
                    return;
                } else {
                    if (cause != null) {
                        System.err.println("API Discovery error: " + cause);
                        cause.printStackTrace();
                    }
                    showError("Error extracting API info. Please recheck the path and selected language, then try again.");
                }
            } catch (Exception ex) {
                System.err.println("API Discovery error: " + ex);
                ex.printStackTrace();
                showError("Error extracting API info. Please recheck the path and selected language, then try again.");
            }
            setEditing(true);
        }
    }
}
