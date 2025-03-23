package net.nightvision.plugin.intellij.project;

import com.intellij.openapi.project.Project;
import net.nightvision.plugin.intellij.Loading;
import net.nightvision.plugin.intellij.Screen;
import net.nightvision.plugin.intellij.Utils;
import net.nightvision.plugin.intellij.auth.AuthenticationsScreen;
import net.nightvision.plugin.intellij.models.ProjectInfo;
import net.nightvision.plugin.intellij.services.ProjectService;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.List;

public class ProjectsScreen extends Screen {
    private JPanel projectsPanel;
    private JButton backButton;
    private JPanel loadingPanel;
    private JTable projectsTable;
    private JPanel loadingPanelParent;
    private JButton createProjectButton;

    public JPanel getProjectsPanel() {
        return projectsPanel;
    }

    public ProjectsScreen(Project project) {
        super(project);

        backButton.addActionListener(e -> {
            mainWindowFactory.openOverviewPage();
        });
        backButton.setIcon(Utils.getIcon("/icons/back.svg", 1f));
        backButton.setBorder(null);

        projectsTable.setModel(new ProjectsTableModel());
        projectsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        projectsTable.setRowHeight(40);

        createProjectButton.addActionListener(e -> {
            mainWindowFactory.openProjectCreatePage();
        });

        ListSelectionModel selectionModel = projectsTable.getSelectionModel();
        selectionModel.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = projectsTable.getSelectedRow();
                if (selectedRow != -1) {
                    ProjectInfo selectedAuthInfo = ((ProjectsTableModel) projectsTable.getModel()).getProjectInfoAt(selectedRow);
//                    mainWindowFactory.openAuthInfoDetailsPage(selectedAuthInfo);
                    // TODO
                }
            }
        });

        new LoadProjectsWorker().execute();

        loadingPanel = new Loading().getLoadingPanel();
        loadingPanelParent.add(loadingPanel);
        projectsTable.setVisible(false);
    }

    private class LoadProjectsWorker extends SwingWorker<List<ProjectInfo>, Void> {
        @Override
        protected List<ProjectInfo> doInBackground() throws Exception {
            return ProjectService.INSTANCE.getProjectInfos();
        }

        @Override
        protected void done() {
            try {
                List<ProjectInfo> projectInfos = get();
                ((ProjectsTableModel) projectsTable.getModel()).setProjectInfos(projectInfos);

                loadingPanelParent.remove(loadingPanel);
                loadingPanelParent.revalidate();
                projectsTable.setVisible(true);
            } catch (Exception ignore) {
                System.out.println(ignore);
                // TODO: Show error message + stop loading panel
            }
        }
    }

    static class ProjectsTableModel extends AbstractTableModel {
        private final List<String> columns = List.of("PROJECT");
        private List<ProjectInfo> projectInfos = List.of();

        public void setProjectInfos(List<ProjectInfo> projectInfos) {
            this.projectInfos = projectInfos;
            fireTableDataChanged();
        }

        public ProjectInfo getProjectInfoAt(int row) {
            return projectInfos.get(row);
        }

        @Override
        public int getRowCount() {
            return projectInfos.size();
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
            ProjectInfo projectInfo = projectInfos.get(rowIndex);

            return switch (columnIndex) {
                case 0 -> projectInfo.getName();
                default -> "";
            };
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 0 -> String.class;
                default -> Object.class;
            };
        }
    }
}
