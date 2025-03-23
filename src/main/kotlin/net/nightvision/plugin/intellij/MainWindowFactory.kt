package net.nightvision.plugin.intellij

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import net.nightvision.plugin.intellij.auth.AuthenticationDetailsScreen
import net.nightvision.plugin.intellij.auth.AuthenticationsCreateScreen
import net.nightvision.plugin.intellij.auth.AuthenticationsScreen
import net.nightvision.plugin.intellij.models.AuthInfo
import net.nightvision.plugin.intellij.project.ProjectsCreateScreen
import net.nightvision.plugin.intellij.project.ProjectsScreen
import net.nightvision.plugin.intellij.scans.ScanDetailsScreen
import net.nightvision.plugin.intellij.scans.ScansScreen
import net.nightvision.plugin.intellij.services.LoginService

class MainWindowFactory : ToolWindowFactory {
    private var toolWindow: ToolWindow? = null
    private var project: Project? = null

    override fun init(toolWindow: ToolWindow) {
        this.toolWindow = toolWindow
        this.project = toolWindow.project

        val service = toolWindow.project.getService(MainWindowService::class.java)
        service.windowFactory = this

        super.init(toolWindow)
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        if (LoginService.bypassLoginStepIfAuthenticatedAlready()) {
            openOverviewPage()
            return
        }
        openLoginPage()
    }

    fun openLoginPage() {
        toolWindow?.let { window ->
            window.component.removeAll()
            window.component.add(LoginScreen(project).loginPanel)
            window.component.revalidate()
        }
    }

    fun openOverviewPage() {
        toolWindow?.let { window ->
            window.component.removeAll()
            window.component.add(OverviewScreen(project).overviewPanel)
            window.component.revalidate()
        }
    }

    fun openApiDiscoveryPage() {
        toolWindow?.let { window ->
            window.component.removeAll()
            window.component.add(ApiDiscovery(project).apiDiscoveryPanel)
            window.component.revalidate()
        }
    }

    fun openScansPage() {
        toolWindow?.let { window ->
            window.component.removeAll()
            window.component.add(ScansScreen(project).scansPanel)
            window.component.revalidate()
        }
    }

    fun openScansDetailsPage(scan: Scan) {
        toolWindow?.let { window ->
            window.component.removeAll()
            window.component.add(
                ScanDetailsScreen(
                    project,
                    scan
                ).scanDetailsPanel)
            window.component.revalidate()
        }
    }

    fun openAuthenticationsPage() {
        toolWindow?.let { window ->
            window.component.removeAll()
            window.component.add(AuthenticationsScreen(project).authenticationsPanel)
            window.component.revalidate()
        }
    }

    fun openAuthInfoDetailsPage(authInfo: AuthInfo) {
        toolWindow?.let { window ->
            window.component.removeAll()
            window.component.add(
                AuthenticationDetailsScreen(
                    project,
                    authInfo
                ).authenticationDetailsPanel)
            window.component.revalidate()
        }
    }

    fun openAuthCreatePage() {
        toolWindow?.let { window ->
            window.component.removeAll()
            window.component.add(
                AuthenticationsCreateScreen(
                    project
                ).authenticationsCreatePanel)
            window.component.revalidate()
        }
    }

    fun openProjectsPage() {
        toolWindow?.let { window ->
            window.component.removeAll()
            window.component.add(ProjectsScreen(project).projectsPanel)
            window.component.revalidate()
        }
    }

    fun openProjectCreatePage() {
        toolWindow?.let { window ->
            window.component.removeAll()
            window.component.add(ProjectsCreateScreen(project).projectsCreatePanel)
            window.component.revalidate()
        }
    }
}