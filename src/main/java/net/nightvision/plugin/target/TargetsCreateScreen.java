package net.nightvision.plugin.target;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import net.nightvision.plugin.Screen;
import net.nightvision.plugin.exceptions.CommandNotFoundException;
import net.nightvision.plugin.utils.IconUtils;
import net.nightvision.plugin.services.TargetService;

import javax.swing.*;
import java.awt.*;

public class TargetsCreateScreen extends Screen {
    private JPanel targetsCreatePanel;
    private JButton backButton;
    private JTabbedPane tabbedPane1;
    private JTextField apiTargetNameTextField;
    private JTextField apiTargetUrlTextField;
    private JButton cancelApiTargetButton;
    private JButton createApiTargetButton;
    private JLabel errorMessageApiTarget;
    private JTabbedPane apiTargetSpecTypeTabbedPane;
    private JTextField apiTargetSpecUrlTextField;
    private JButton uploadButton;
    private JLabel specFileName;
    private JLabel errorMessageWebTarget;
    private JButton createWebTargetButton;
    private JButton cancelWebTargetButton;
    private JTextField webTargetNameTextField;
    private JTextField webTargetUrlTextField;
    private JPanel specURLPane;
    private JPanel specFilePane;
    private String swaggerFilePath;

    // TODO: Consider Exclusions

    public JPanel getTargetsCreatePanel() {
        return targetsCreatePanel;
    }

    public TargetsCreateScreen(Project project) {
        super(project);

        errorMessageApiTarget.setVisible(false);
        errorMessageWebTarget.setVisible(false);
        specFileName.setVisible(false);
        backButton.addActionListener(e -> {
            mainWindowFactory.openTargetsPage();
        });
        backButton.setIcon(IconUtils.getIcon("/icons/back.svg", 1f));
        backButton.setBorder(null);
        backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        uploadButton.setIcon(IconUtils.getIcon("/icons/custom-file-select.svg", 1f));
        uploadButton.setBorder(null);
        uploadButton.addActionListener(e -> openFileDialog());
        uploadButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        cancelApiTargetButton.addActionListener(e -> {
            mainWindowFactory.openTargetsPage();
        });
        cancelApiTargetButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        cancelWebTargetButton.addActionListener(e -> {
            mainWindowFactory.openTargetsPage();
        });
        cancelWebTargetButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        apiTargetSpecUrlTextField.setToolTipText("Enter URL here...");

        createWebTargetButton.addActionListener(e -> {
            errorMessageWebTarget.setVisible(false);
            errorMessageWebTarget.setText("");
            createWebTargetButton.setEnabled(false);
            String targetName = webTargetNameTextField.getText();
            String targetURL = webTargetUrlTextField.getText();

            try {
                TargetService.INSTANCE.createWebTarget(targetName, targetURL);
                mainWindowFactory.openTargetsPage();
            } catch (CommandNotFoundException ex) {
                mainWindowFactory.openInstallCLIPage();
            } catch(Exception exception) {
                errorMessageWebTarget.setText(exception.getMessage());
                errorMessageWebTarget.setVisible(true);
            }

            createWebTargetButton.setEnabled(true);

        });
        createWebTargetButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        createApiTargetButton.addActionListener(e -> {
            errorMessageApiTarget.setVisible(false);
            errorMessageApiTarget.setText("");
            createApiTargetButton.setEnabled(false);
            String targetName = apiTargetNameTextField.getText();
            String targetURL = apiTargetUrlTextField.getText();

            var isSwaggerURL = apiTargetSpecTypeTabbedPane.getSelectedComponent().equals(specURLPane);
            String swaggerPath = isSwaggerURL ? apiTargetSpecUrlTextField.getText() : swaggerFilePath;

            try {
                TargetService.INSTANCE.createApiTarget(targetName, targetURL, swaggerPath, isSwaggerURL);
                mainWindowFactory.openTargetsPage();
            } catch(Exception exception) {
                errorMessageApiTarget.setText(exception.getMessage());
                errorMessageApiTarget.setVisible(true);
                createApiTargetButton.setEnabled(true);
            }

        });
        createApiTargetButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

    }

    private void openFileDialog() {
        FileChooserDescriptor descriptor = new FileChooserDescriptor(true,false,false,false,false,false)
                .withTitle("Select File")
                .withDescription("Select a Swagger File or Postman Collection (.YML, .YAML, .JSON)");
        // TODO: It seems has issues if using withFileFilter: https://youtrack.jetbrains.com/issue/IJPL-179805

        VirtualFile selectedFile = FileChooser.chooseFile(descriptor, project, null);
        if (selectedFile != null) {
            swaggerFilePath = selectedFile.getPath();
            specFileName.setToolTipText(swaggerFilePath);
            specFileName.setText(selectedFile.getName());
            specFileName.setVisible(true);
        }
    }
}
