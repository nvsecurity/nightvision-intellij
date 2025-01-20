package net.nightvision.plugin.intellij;

import com.intellij.openapi.project.Project;
import net.nightvision.plugin.intellij.services.LoginService;
import net.nightvision.plugin.intellij.services.ScanService;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import com.intellij.openapi.util.*;
import com.intellij.util.IconUtil;

public class ScansScreen extends Screen {
    private JTable scansTable;
    private JPanel scansPanel;
    private JLabel tableName;
    private JButton logout;

    public JPanel getScansPanel() {
        return scansPanel;
    }

    ScansScreen(Project project) {
        super(project);

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

        List<Scan> scans = ScanService.INSTANCE.getScans();
        ((ScansTableModel)scansTable.getModel()).setScans(scans);

        logout.addActionListener(e -> {
            LoginService.INSTANCE.logout();
            mainWindow.openLoginPage();
        });

        scansTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {

                if (value instanceof JLabel label) {
                    label.setOpaque(true);

                    if (isSelected || hasFocus) {
                        label.setBackground(table.getSelectionBackground());
                        label.setForeground(table.getSelectionForeground());
                    } else {
                        label.setBackground(table.getBackground());
                        label.setForeground(table.getForeground());
                    }
                    return label;
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
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
            boolean isOpenapi = scan.getTargetType().equals("OpenAPI");

            Icon icon = IconLoader.getIcon(isOpenapi ? "/icons/openapi-scan.svg" : "/icons/web-scan.svg", ScansTableModel.class);
            Icon scaledIcon = IconUtil.scale(icon, null, 0.7f);
            return switch (columnIndex) {
                case 0 -> new JLabel(scan.getTargetName(), scaledIcon, JLabel.LEFT);
                case 1 -> scan.getLocation();
                case 2 -> scan.getProject().getName();
                case 3 -> String.format("%d %d %d %d %d", scanStat.getCritical(), scanStat.getHigh(), scanStat.getMedium(), scanStat.getLow(), scanStat.getInformational());
                default -> "";
            };
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 0 -> JLabel.class;
                case 1 -> String.class;
                case 2 -> String.class;
                case 3 -> String.class;
                default -> Object.class;
            };
        }
    }
}
