package net.nightvision.plugin.scans;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import net.nightvision.plugin.Loading;
import net.nightvision.plugin.ScanInfo;
import net.nightvision.plugin.Screen;
import net.nightvision.plugin.VulnerablePathStatistics;
import net.nightvision.plugin.project.ProjectSelectionPanel;
import net.nightvision.plugin.services.ScanService;
import net.nightvision.plugin.utils.IconUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import static javax.swing.SwingConstants.CENTER;
import static net.nightvision.plugin.utils.TableUtils.addHoverEffects;

public class ScansScreen extends Screen {
    private JTable scansTable;
    private JPanel scansPanel;
    private JButton backButton;
    private JPanel currentProjectWrapperPanel;
    private JPanel loadingPanelParent;
    private JButton scanWebApplicationsButton;
    private JButton scanAPIsButton;
    private JPanel loadingPanel;

    public JPanel getScansPanel() {
        return scansPanel;
    }

    public ScansScreen(Project project) {
        super(project);

        scansTable.setModel(new ScansTableModel());
        addHoverEffects(scansTable, new JBColor(new Color(220, 220, 255), new Color(60, 60, 80)));
        scansTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scansTable.setRowHeight(40);

        ListSelectionModel selectionModel = scansTable.getSelectionModel();
        selectionModel.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = scansTable.getSelectedRow();
                if (selectedRow != -1) {
                    ScanInfo selectedScanInfo = ((ScansTableModel)scansTable.getModel()).getScanAt(selectedRow);
                    mainWindowFactory.openScansDetailsPage(selectedScanInfo);
                }
            }
        });

        backButton.addActionListener(e -> {
            mainWindowFactory.openOverviewPage();
        });
        backButton.setIcon(IconUtils.getIcon("/icons/back.svg", 1f));
        backButton.setBorder(null);
        backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        currentProjectWrapperPanel.add(new ProjectSelectionPanel(selectedProject -> {
            loadTable();
        }));

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

        scansTable.getColumnModel().getColumn(scansTable.getColumnCount()-1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {

                if (value instanceof int[] val) {
                    JPanel panel = new JPanel();
                    panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
                    if (val.length == 0) {
                        JLabel label = new JLabel("Running...", JLabel.LEFT);
                        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
                        panel.add(label);
                    } else {
                        for (int vulnerability : val) {
                            Icon i = IconUtils.getIcon("/icons/dot.svg", .8f);
                            JLabel label = new JLabel(String.valueOf(vulnerability), i, JLabel.LEFT);
                            label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
                            panel.add(label);
                        }
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

        Icon openApiIcon = IconUtils.getIcon("/icons/openapi-scan.svg", 0.7f);
        Icon webScanIcon = IconUtils.getIcon("/icons/web-scan.svg", 0.7f);
        scanWebApplicationsButton.setIcon(webScanIcon);
        scanWebApplicationsButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        scanWebApplicationsButton.setHorizontalTextPosition(CENTER);
        scanWebApplicationsButton.addActionListener(e -> {
            mainWindowFactory.openScanCreatePage("URL");
        });
        scanWebApplicationsButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        scanAPIsButton.setIcon(openApiIcon);
        scanAPIsButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        scanAPIsButton.setHorizontalTextPosition(CENTER);
        scanAPIsButton.addActionListener(e -> {
            mainWindowFactory.openScanCreatePage("OPENAPI");
        });
        scanAPIsButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));


        loadTable();
    }

    private void loadTable() {
        new LoadScansWorker().execute();

        loadingPanel = new Loading().getLoadingPanel();
        loadingPanelParent.add(loadingPanel);
        scansTable.setVisible(false);
    }

    private class LoadScansWorker extends SwingWorker<List<ScanInfo>, Void> {
        @Override
        protected List<ScanInfo> doInBackground() throws Exception {
            return ScanService.INSTANCE.getScans();
        }

        @Override
        protected void done() {
            try {
                List<ScanInfo> scanInfos = get();
                ((ScansTableModel) scansTable.getModel()).setScans(scanInfos);

                loadingPanelParent.remove(loadingPanel);
                loadingPanelParent.revalidate();
                scansTable.setVisible(true);
            } catch (Exception ignore) {
                // TODO: Show error message + stop loading panel
            }
        }
    }

    static class ScansTableModel extends AbstractTableModel {
        private final List<String> columns = List.of("TARGET", "LOCATION", /*"PROJECT",*/ "VULNERABILITIES");
        private List<ScanInfo> scanInfos = List.of();

        public void setScans(List<ScanInfo> scanInfos) {
            if (scanInfos != null) {
                this.scanInfos = scanInfos;
                fireTableDataChanged();
            }
        }

        public ScanInfo getScanAt(int row) {
            return scanInfos.get(row);
        }

        @Override
        public int getRowCount() {
            return scanInfos.size();
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
            ScanInfo scanInfo = scanInfos.get(rowIndex);
            VulnerablePathStatistics scanStat = scanInfo.getVulnPathStatistics();
            boolean isOpenapi = scanInfo.getTargetType().equals("OpenAPI");

            Icon icon = IconUtils.getIcon(isOpenapi ? "/icons/openapi-scan.svg" : "/icons/web-scan.svg", 0.7f);
            return switch (columnIndex) {
                case 0 -> new JLabel(scanInfo.getTargetName(), icon, JLabel.LEFT);
                case 1 -> scanInfo.getLocation();
//                case 2 -> scan.getProject().getName();
                case 2 -> scanInfo.getStatusValue().equalsIgnoreCase("RUNNING") ? new int[]{} : new int[] {scanStat.getCritical(), scanStat.getHigh(), scanStat.getMedium(), scanStat.getLow(), scanStat.getInformational()};
                default -> "";
            };
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 0 -> JLabel.class;
                case 1 -> String.class;
//                case 2 -> String.class;
                case 2 -> JPanel.class;
                default -> Object.class;
            };
        }
    }
}
