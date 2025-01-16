package net.nightvision.plugin.intellij;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import net.nightvision.plugin.intellij.login.LoginScreen;
import net.nightvision.plugin.intellij.login.LoginService;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class ScansScreen {
    private JTable scansTable;
    private JPanel scansPanel;
    private JLabel tableName;
    private JButton backButton;

    private final MainWindowFactory mainWindow;
    private final Project project;

    public JPanel getScansPanel() {
        return scansPanel;
    }

    ScansScreen(Project project) {
        this.mainWindow = project.getService(MainWindowService.class).getWindowFactory();
        this.project = project;

        scansTable.setModel(new ScansTableModel());

        ListSelectionModel selectionModel = scansTable.getSelectionModel();
        selectionModel.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    int selectedRow = scansTable.getSelectedRow();
                    if (selectedRow != -1) {
                        Scan selectedScan = ((ScansTableModel)scansTable.getModel()).getScanAt(selectedRow);
                        mainWindow.openScansDetailsPage(selectedScan);
                    }
                }
            }
        });

        List<Scan> scans = LoginService.INSTANCE.getScans();
        ((ScansTableModel)scansTable.getModel()).setScans(scans);

        backButton.addActionListener(e -> {
            mainWindow.openLoginPage();
        });
    }


    static class ScansTableModel extends AbstractTableModel {
        private final List<String> columns = List.of("TARGET NAME", "LOCATION", "AUTH NAME", "CREATED AT");
        private List<Scan> scans = List.of();

        public void setScans(List<Scan> scans) {
            this.scans = scans;
            fireTableDataChanged();
        }

        public Scan getScanAt(int row) {
            return scans.get(row);
        }

        @Override
        public int getRowCount() {
            return scans.size();
        }

        @Override
        public int getColumnCount() {
            return columns.size();
        }

        @Override
        public String getColumnName(int column) {
            return columns.get(column);
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Scan scan = scans.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> scan.getTarget_name();
                case 1 -> scan.getLocation();
                case 2 -> scan.getCredentials() != null ? scan.getCredentials().getName() : "";
                case 3 -> ZonedDateTime.parse(scan.getCreated_at())
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                default -> "";
            };
        }
    }
}
