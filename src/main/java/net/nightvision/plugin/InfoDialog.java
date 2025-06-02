package net.nightvision.plugin;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class InfoDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JLabel txtInfo;

    public InfoDialog(String text, ActionListener okAction) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        this.setSize(new Dimension(600, 200));
        this.setLocationRelativeTo(null);

        txtInfo.setText(text);

        buttonOK.addActionListener(e -> {
            okAction.actionPerformed(e);
            onOK();
        });
    }

    private void onOK() {
        // add your code here
        dispose();
    }

//    public static void main(String[] args) {
//        InfoDialog dialog = new InfoDialog();
//        dialog.pack();
//        dialog.setVisible(true);
//        System.exit(0);
//    }
}
