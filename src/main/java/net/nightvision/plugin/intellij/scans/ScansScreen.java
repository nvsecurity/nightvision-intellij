package net.nightvision.plugin.intellij.scans;

import com.intellij.openapi.project.Project;
import net.nightvision.plugin.intellij.*;
import net.nightvision.plugin.intellij.services.ScanService;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

public class ScansScreen extends Screen {
    private JTable scansTable;
    private JPanel scansPanel;
    private JButton backButton;
    private JPanel loadingPanel;

    public JPanel getScansPanel() {
        return scansPanel;
    }

    public ScansScreen(Project project) {
        super(project);

        scansPanel.setLayout(new BoxLayout(scansPanel, BoxLayout.Y_AXIS));

        scansTable.setModel(new ScansTableModel());
        scansTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scansTable.setRowHeight(40);

        ListSelectionModel selectionModel = scansTable.getSelectionModel();
        selectionModel.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = scansTable.getSelectedRow();
                if (selectedRow != -1) {
                    Scan selectedScan = ((ScansTableModel)scansTable.getModel()).getScanAt(selectedRow);
                    mainWindowFactory.openScansDetailsPage(selectedScan);
                }
            }
        });


        new LoadScansWorker().execute();

        backButton.addActionListener(e -> {
            mainWindowFactory.openOverviewPage();
        });
        backButton.setIcon(Utils.getIcon("/icons/back.svg", 1f));
        backButton.setBorder(null);

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

        scansTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {

                if (value instanceof int[] val) {
                    JPanel panel = new JPanel();
                    panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
                    for (int vulnerability : val) {
                        Icon i = Utils.getIcon("/icons/dot.svg", .8f);
                        JLabel label = new JLabel(String.valueOf(vulnerability), i, JLabel.LEFT);
                        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
                        panel.add(label);
                    }

                    if (isSelected || hasFocus) {
                        panel.setBackground(table.getSelectionBackground());
                        panel.setForeground(table.getSelectionForeground());
                    } else {
                        panel.setBackground(table.getBackground());
                        panel.setForeground(table.getForeground());
                    }
                    return panel;
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        });

        loadingPanel = new Loading().getLoadingPanel();
        scansPanel.add(loadingPanel);
    }

    private class LoadScansWorker extends SwingWorker<List<Scan>, Void> {
        @Override
        protected List<Scan> doInBackground() throws Exception {
            return ScanService.INSTANCE.getScans();
        }

        @Override
        protected void done() {
            try {
                List<Scan> scans = get();
                ((ScansTableModel) scansTable.getModel()).setScans(scans);

                scansPanel.remove(loadingPanel);
                scansPanel.revalidate();
            } catch (Exception ignore) {
                // TODO: Show error message + stop loading panel
            }
        }
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

            Icon icon = Utils.getIcon(isOpenapi ? "/icons/openapi-scan.svg" : "/icons/web-scan.svg", 0.7f);
            return switch (columnIndex) {
                case 0 -> new JLabel(scan.getTargetName(), icon, JLabel.LEFT);
                case 1 -> scan.getLocation();
                case 2 -> scan.getProject().getName();
                case 3 -> new int[] {scanStat.getCritical(), scanStat.getHigh(), scanStat.getMedium(), scanStat.getLow(), scanStat.getInformational()};
                default -> "";
            };
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 0 -> JLabel.class;
                case 1 -> String.class;
                case 2 -> String.class;
                case 3 -> JPanel.class;
                default -> Object.class;
            };
        }
    }
}
