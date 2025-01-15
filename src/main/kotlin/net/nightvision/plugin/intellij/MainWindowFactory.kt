package net.nightvision.plugin.intellij

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import net.nightvision.plugin.intellij.login.LoginScreen

class MainWindowFactory : ToolWindowFactory {
    private var toolWindow: ToolWindow? = null

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        this.toolWindow = toolWindow;
        openLoginPage();
    }

    fun openLoginPage() {
        toolWindow?.let { window ->
            window.component.removeAll()
            window.component.add(LoginScreen(this).loginPanel)
            window.component.revalidate()
        }
    }

    fun openScansPage() {
        toolWindow?.let { window ->
            window.component.removeAll()
            window.component.add(ScansScreen(this).scansPanel)
            window.component.revalidate()
        }
    }

    fun openScansDetailsPage(scan: Scan) {
        toolWindow?.let { window ->
            window.component.removeAll()
            window.component.add(ScanDetailsScreen(this, scan).scanDetailsPanel)
            window.component.revalidate()
        }
    }
}
