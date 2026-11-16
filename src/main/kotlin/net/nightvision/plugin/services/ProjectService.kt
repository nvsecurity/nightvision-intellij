package net.nightvision.plugin.services

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.nightvision.plugin.Constants
import net.nightvision.plugin.Constants.Companion.NIGHTVISION
import net.nightvision.plugin.PaginatedResult
import net.nightvision.plugin.models.ProjectInfo
import java.net.http.HttpClient
import java.time.Duration
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.TimeUnit

object ProjectService {
    val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build()
    val gson = GsonBuilder().create()
    private var currentProjectName = ""
    private var currentProjectId = ""

    fun getProjectInfos(): List<ProjectInfo> {
        return PaginatedApiService.getAll(Constants.getApiUrlFor("projects"), LoginService.token,
            ProjectInfo::class.java, httpClient, gson) { it.id }
    }

    fun createProject(projectName: String) {
        if (projectName.isBlank()) {
            throw IllegalArgumentException("Project name can't be empty.");
        }

        var cmd = ArrayList<String>(listOf(NIGHTVISION, "project", "create", projectName))
        val response = CommandRunnerService.runCommandSync(*cmd.toTypedArray())
        val t = response.output
        val id = t.trim().takeIf { Regex("Id:").containsMatchIn(it) } ?: ""
        if (id.isBlank()) {
            throw RuntimeException(
                CommandRunnerService.missingRecordMessage("create the project", response.output, response.error)
            )
        }
    }

    fun getCurrentProjectName(): String {
        return currentProjectName
    }

    fun getCurrentProjectId(): String {
        return currentProjectId
    }

    fun fetchCurrentProjectName() : String {
        val cmd = ArrayList<String>(listOf(NIGHTVISION, "project", "show"))

        val response = CommandRunnerService.runCommandSync(*cmd.toTypedArray())
        updateCurrentProject(response.output)
        return currentProjectName
    }

    internal fun updateCurrentProject(t: String) {
        val regexProjName = Regex("""(?m)^Name:\s*(.+)$""")
        val matchProjName = regexProjName.find(t)
        currentProjectName = matchProjName?.groupValues?.get(1)?.trim() ?: ""

        val regexProjId = Regex("""(?m)^Id:\s*(.+)$""")
        val matchProjId = regexProjId.find(t)
        currentProjectId = matchProjId?.groupValues?.get(1)?.trim() ?: ""

    }

    // NV-6990: a valid login must not depend on the saved project's accessibility.
    @JvmOverloads
    fun restoreCurrentProject(fetch: () -> Unit = { fetchCurrentProjectName() }): String? {
        try {
            fetch()
            if (currentProjectId.isNotBlank() && currentProjectName.isNotBlank()) return null
        } catch (ex: Exception) {
            clearCurrentProject()
            when (ex) {
                is net.nightvision.plugin.exceptions.CommandNotFoundException,
                is net.nightvision.plugin.exceptions.NotLoggedException,
                is com.intellij.execution.process.ProcessNotCreatedException,
                is com.intellij.openapi.progress.ProcessCanceledException,
                is java.util.concurrent.CancellationException,
                is InterruptedException -> throw ex
            }
            return "Your saved NightVision project could not be opened. Choose an accessible project or create one.\n" +
                (ex.message ?: "Please retry.")
        }
        clearCurrentProject()
        return "No NightVision project is selected. Choose an accessible project or create one."
    }

    private fun clearCurrentProject() {
        currentProjectName = ""
        currentProjectId = ""
    }

    fun setCurrentProjectName(projectName: String) {
        if (projectName.isBlank()) {
            return
        }
        var cmd = ArrayList<String>(listOf(NIGHTVISION, "project", "set", projectName))

        CommandRunnerService.runCommandSync(*cmd.toTypedArray())

        // Confirmed by reading the project back, not by matching what the CLI
        // said. It answers "Current project changed to X." when it switches but
        // "X is already the current project" when X was already current, and
        // matching only the first reported selecting the current project as a
        // failure. runCommandSync has already thrown if the CLI rejected the
        // name, so what is left to check is the outcome rather than the wording.
        val actual = fetchCurrentProjectName()
        if (actual != projectName) {
            throw RuntimeException("Unable to set project to '${projectName}'")
        }
    }

}
