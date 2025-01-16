package net.nightvision.plugin.intellij;

import com.intellij.openapi.project.Project;
import net.nightvision.plugin.intellij.login.LoginService;

import javax.swing.*;
import java.util.List;
import javax.swing.table.AbstractTableModel;

public class ScansScreen {
    private JTable scansTable;
    private JPanel scansPanel;
    private JLabel tableName;
    private JButton logout;

    private final MainWindowFactory mainWindow;
    private final Project project;

    public JPanel getScansPanel() {
        return scansPanel;
    }

    ScansScreen(Project project) {
        this.mainWindow = project.getService(MainWindowService.class).getWindowFactory();
        this.project = project;

        scansTable.setModel(new ScansTableModel());
        scansTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scansTable.setRowHeight(40);

        ListSelectionModel selectionModel = scansTable.getSelectionModel();
        selectionModel.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = scansTable.getSelectedRow();
                if (selectedRow != -1) {
                    Scan selectedScan = ((ScansTableModel)scansTable.getModel()).getScanAt(selectedRow);
                    mainWindow.openScansDetailsPage(selectedScan);
                }
            }
        });

        List<Scan> scans = LoginService.INSTANCE.getScans();
        ((ScansTableModel)scansTable.getModel()).setScans(scans);

        logout.addActionListener(e -> {
            LoginService.INSTANCE.logout();
            mainWindow.openLoginPage();
        });
    }


    static class ScansTableModel extends AbstractTableModel {
        private final List<String> columns = List.of("TARGET", "LOCATION", "PROJECT", "VULNERABILITIES");
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
            VulnerablePathStatistics scanStat = scan.getVulnPathStatistics();
            return switch (columnIndex) {
                case 0 -> scan.getTargetName();
                case 1 -> scan.getLocation();
                case 2 -> scan.getProject().getName();
                case 3 -> String.format("%d %d %d %d %d", scanStat.getCritical(), scanStat.getHigh(), scanStat.getMedium(), scanStat.getLow(), scanStat.getInformational());
                default -> "";
            };
        }
    }
}
