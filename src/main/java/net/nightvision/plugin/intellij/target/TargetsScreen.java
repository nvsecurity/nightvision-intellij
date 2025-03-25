package net.nightvision.plugin.intellij.target;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import net.nightvision.plugin.intellij.Loading;
import net.nightvision.plugin.intellij.Screen;
import net.nightvision.plugin.intellij.utils.IconUtils;
import net.nightvision.plugin.intellij.models.TargetInfo;
import net.nightvision.plugin.intellij.project.ProjectSelectionPanel;
import net.nightvision.plugin.intellij.services.TargetService;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.List;

import static net.nightvision.plugin.intellij.utils.TableUtils.addHoverEffects;

public class TargetsScreen extends Screen {
    private JButton backButton;
    private JPanel loadingPanel;
    private JTable targetsTable;
    private JPanel loadingPanelParent;
    private JButton createTargetButton;
    private JPanel targetsPanel;
    private JPanel currentProjectWrapperPanel;

    public JPanel getTargetsPanel() {
        return targetsPanel;
    }

    public TargetsScreen(Project project) {
        super(project);

        backButton.addActionListener(e -> {
            mainWindowFactory.openOverviewPage();
        });
        backButton.setIcon(IconUtils.getIcon("/icons/back.svg", 1f));
        backButton.setBorder(null);
        backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        currentProjectWrapperPanel.add(new ProjectSelectionPanel(selectedProject -> {
            loadTable();
        }));

        targetsTable.setModel(new TargetsTableModel());
        addHoverEffects(targetsTable, new JBColor(new Color(220, 220, 255), new Color(60, 60, 80)));
        targetsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        targetsTable.setRowHeight(40);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        targetsTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);

        createTargetButton.addActionListener(e -> {
            mainWindowFactory.openTargetsCreatePage();
        });
        createTargetButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        ListSelectionModel selectionModel = targetsTable.getSelectionModel();
        selectionModel.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = targetsTable.getSelectedRow();
                if (selectedRow != -1) {
                    TargetInfo selectedTargetInfo = ((TargetsTableModel) targetsTable.getModel()).getTargetInfoAt(selectedRow);
                    mainWindowFactory.openTargetInfoDetailsPage(selectedTargetInfo);
                }
            }
        });

        loadTable();
    }

    private void loadTable() {
        new LoadTargetsWorker().execute();

        loadingPanel = new Loading().getLoadingPanel();
        loadingPanelParent.add(loadingPanel);
        targetsTable.setVisible(false);
    }

    private class LoadTargetsWorker extends SwingWorker<List<TargetInfo>, Void> {
        @Override
        protected List<TargetInfo> doInBackground() throws Exception {
            return TargetService.INSTANCE.getTargetInfos("");
        }

        @Override
        protected void done() {
            try {
                List<TargetInfo> targetInfos = get();
                ((TargetsTableModel) targetsTable.getModel()).setTargetInfos(targetInfos);

                loadingPanelParent.remove(loadingPanel);
                loadingPanelParent.revalidate();
                targetsTable.setVisible(true);
            } catch (Exception ignore) {
                // TODO: Show error message + stop loading panel
            }
        }
    }

    static class TargetsTableModel extends AbstractTableModel {
        private final List<String> columns = List.of("TARGET", "URL", "TYPE");
        private List<TargetInfo> targetInfos = List.of();

        public void setTargetInfos(List<TargetInfo> targetInfos) {
            if (targetInfos != null) {
                this.targetInfos = targetInfos;
                fireTableDataChanged();
            }
        }

        public TargetInfo getTargetInfoAt(int row) {
            return targetInfos.get(row);
        }

        @Override
        public int getRowCount() {
            return targetInfos.size();
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
            TargetInfo targetInfo = targetInfos.get(rowIndex);

            return switch (columnIndex) {
                case 0 -> targetInfo.getName();
                case 1 -> targetInfo.getLocation();
                case 2 -> targetInfo.getType().equalsIgnoreCase("URL") ? "Web" : "Open API";
                default -> "";
            };
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 0, 1, 2 -> String.class;
                default -> Object.class;
            };
        }
    }
}
