package net.nightvision.plugin

import com.intellij.execution.process.ProcessNotCreatedException
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import net.nightvision.plugin.auth.AuthenticationDetailsScreen
import net.nightvision.plugin.auth.AuthenticationsCreateScreen
import net.nightvision.plugin.auth.AuthenticationsScreen
import net.nightvision.plugin.exceptions.CommandNotFoundException
import net.nightvision.plugin.exceptions.NotLoggedException
import net.nightvision.plugin.models.AuthInfo
import net.nightvision.plugin.models.ProjectInfo
import net.nightvision.plugin.models.TargetInfo
import net.nightvision.plugin.project.ProjectDetailsScreen
import net.nightvision.plugin.project.ProjectsCreateScreen
import net.nightvision.plugin.project.ProjectsScreen
import net.nightvision.plugin.scans.ScanDetailsScreen
import net.nightvision.plugin.scans.ScansCreateScreen
import net.nightvision.plugin.scans.ScansScreen
import net.nightvision.plugin.services.LoginService
import net.nightvision.plugin.services.ProjectService
import net.nightvision.plugin.target.TargetDetailsScreen
import net.nightvision.plugin.target.TargetsCreateScreen
import net.nightvision.plugin.target.TargetsScreen

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
//        try {
//            if (LoginService.bypassLoginStepIfAuthenticatedAlready(project)) {
//                ProjectService.fetchCurrentProjectName()
//                openOverviewPage()
//                return
//            }
//        } catch (ex: CommandNotFoundException) {
//            openInstallCLIPage()
//            return
//        } catch (ex: NotLoggedException) {
//            openLoginPage()
//            return
//        } catch (ex: ProcessNotCreatedException) {
//            openInstallCLIPage();
//            return
//        }

        openLoginPage()
    }

    fun openLoginPage() {
        toolWindow?.let { window ->
            window.component.removeAll()
            window.component.add(LoginScreen(project).loginPanel)
            window.component.revalidate()
        }
    }

    fun openInstallCLIPage() {
        toolWindow?.let { window ->
            window.component.removeAll()
            window.component.add(InstallCLIScreen(project).loginPanel)
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

    fun openScansDetailsPage(scanInfo: ScanInfo) {
        toolWindow?.let { window ->
            window.component.removeAll()
            window.component.add(
                ScanDetailsScreen(
                    project,
                    scanInfo
                ).scanDetailsPanel)
            window.component.revalidate()
        }
    }

    fun openScanCreatePage(scanType: String) {
        toolWindow?.let { window ->
            window.component.removeAll()
            window.component.add(
                ScansCreateScreen(
                    project,
                    scanType
                ).scansCreatePanel)
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

    fun openProjectInfoDetailsPage(projectInfo: ProjectInfo) {
        toolWindow?.let { window ->
            window.component.removeAll()
            window.component.add(
                ProjectDetailsScreen(
                    project,
                    projectInfo
                ).projectDetailsPanel)
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

    fun openTargetsPage() {
        toolWindow?.let { window ->
            window.component.removeAll()
            window.component.add(TargetsScreen(project).targetsPanel)
            window.component.revalidate()
        }
    }

    fun openTargetsCreatePage() {
        toolWindow?.let { window ->
            window.component.removeAll()
            window.component.add(TargetsCreateScreen(project).targetsCreatePanel)
            window.component.revalidate()
        }
    }

    fun openTargetInfoDetailsPage(selectedTargetInfo: TargetInfo) {
        toolWindow?.let { window ->
            window.component.removeAll()
            window.component.add(TargetDetailsScreen(project, selectedTargetInfo).targetDetailsPanel)
            window.component.revalidate()
        }
    }
}