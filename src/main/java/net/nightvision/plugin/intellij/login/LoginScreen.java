package net.nightvision.plugin.intellij.login;

import net.nightvision.plugin.intellij.MainWindowFactory;

import javax.swing.*;

public class LoginScreen {
    private JButton loginButton;
    private JPanel loginPanel;
    private JLabel temp;
    private final MainWindowFactory factory;

    public JPanel getLoginPanel() {
        return loginPanel;
    }

    public JButton getLoginButton() {
        return loginButton;
    }

    public LoginScreen(MainWindowFactory factory) {
        this.factory = factory;
        this.loginButton.addActionListener(e -> {
            LoginService.INSTANCE.login();
            temp.setVisible(true);
            temp.setText(LoginService.INSTANCE.getToken());
            factory.openScansPage();
        });
    }
}
