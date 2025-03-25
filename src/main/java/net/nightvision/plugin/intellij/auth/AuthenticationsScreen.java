package net.nightvision.plugin.intellij.auth;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import net.nightvision.plugin.intellij.Loading;
import net.nightvision.plugin.intellij.Screen;
import net.nightvision.plugin.intellij.utils.IconUtils;
import net.nightvision.plugin.intellij.models.AuthInfo;
import net.nightvision.plugin.intellij.project.ProjectSelectionPanel;
import net.nightvision.plugin.intellij.services.AuthenticationService;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.List;

import static net.nightvision.plugin.intellij.utils.TableUtils.addHoverEffects;

public class AuthenticationsScreen extends Screen {

    private JButton backButton;
    private JPanel loadingPanel;
    private JPanel loadingPanelParent;

    public JPanel getAuthenticationsPanel() {
        return authenticationsPanel;
    }

    private JPanel authenticationsPanel;
    private JButton createAuthenticationButton;
    private JTable authsTable;
    private JPanel currentProjectWrapperPanel;

    public AuthenticationsScreen(Project project) {
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

        createAuthenticationButton.addActionListener(e -> {
            mainWindowFactory.openAuthCreatePage();
        });
        createAuthenticationButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        authsTable.setModel(new AuthsTableModel());
        addHoverEffects(authsTable, new JBColor(new Color(220, 220, 255), new Color(60, 60, 80)));
        authsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        authsTable.setRowHeight(40);

        ListSelectionModel selectionModel = authsTable.getSelectionModel();
        selectionModel.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = authsTable.getSelectedRow();
                if (selectedRow != -1) {
                    AuthInfo selectedAuthInfo = ((AuthsTableModel) authsTable.getModel()).getAuthInfoAt(selectedRow);
                    mainWindowFactory.openAuthInfoDetailsPage(selectedAuthInfo);
                }
            }
        });

        loadTable();
    }

    private void loadTable() {
        new LoadAuthInfosWorker().execute();

        loadingPanel = new Loading().getLoadingPanel();
        loadingPanelParent.add(loadingPanel);
        authsTable.setVisible(false);
    }

    private class LoadAuthInfosWorker extends SwingWorker<List<AuthInfo>, Void> {
        @Override
        protected List<AuthInfo> doInBackground() throws Exception {
            return AuthenticationService.INSTANCE.getAuthInfos();
        }

        @Override
        protected void done() {
            try {
                List<AuthInfo> authInfos = get();
                ((AuthsTableModel) authsTable.getModel()).setAuthInfos(authInfos);

                loadingPanelParent.remove(loadingPanel);
                loadingPanelParent.revalidate();
                authsTable.setVisible(true);
            } catch (Exception ignore) {
                // TODO: Show error message + stop loading panel
            }
        }
    }

    static class AuthsTableModel extends AbstractTableModel {
        private final List<String> columns = List.of("AUTHENTICATION", "TYPE");
        private List<AuthInfo> authInfos = List.of();

        public void setAuthInfos(List<AuthInfo> authInfos) {
            if (authInfos != null) {
                this.authInfos = authInfos;
                fireTableDataChanged();
            }
        }

        public AuthInfo getAuthInfoAt(int row) {
            return authInfos.get(row);
        }

        @Override
        public int getRowCount() {
            return authInfos.size();
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
            AuthInfo authInfo = authInfos.get(rowIndex);

            var t = authInfo.getType();
            return switch (columnIndex) {
                case 0 -> authInfo.getName();
                case 1 -> switch(t) {
                    case "COOKIE" -> "Cookie";
                    case "HEADER" -> "Header";
                    case "SCRIPT" -> "Playwright";
                    default -> t;
                };
                default -> "";
            };
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 0 -> String.class;
                case 1 -> String.class;
                default -> Object.class;
            };
        }
    }
}
