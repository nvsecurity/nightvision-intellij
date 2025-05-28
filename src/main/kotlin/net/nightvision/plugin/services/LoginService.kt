package net.nightvision.plugin.services

import com.intellij.execution.process.ProcessNotCreatedException
import com.intellij.openapi.project.Project
import net.nightvision.plugin.Constants.Companion.NIGHTVISION
import net.nightvision.plugin.exceptions.CommandNotFoundException
import net.nightvision.plugin.exceptions.NotLoggedException
import net.nightvision.plugin.exceptions.PermissionDeniedException
import net.nightvision.plugin.services.CommandRunnerService.runCommandSync
import java.util.concurrent.TimeUnit
import kotlin.jvm.Throws


object LoginService {
    var token = ""
        private set

    @Throws(CommandNotFoundException::class, PermissionDeniedException::class, NotLoggedException::class)
    fun bypassLoginStepIfAuthenticatedAlready(project: Project): Boolean {
        try {
            val tokenService = TokenService.getInstance(project)
            token = tokenService.createToken(project)
            return token != ""
        } catch (e: Exception) {
            token = ""
            return when (e) {
                is CommandNotFoundException -> throw e
                is PermissionDeniedException -> throw e
                is NotLoggedException -> throw e
                is ProcessNotCreatedException -> throw e
                else                        -> false
            }
        }
    }

    @Throws(ProcessNotCreatedException::class)
    fun login(project: Project): Boolean {
        val tokenService = TokenService.getInstance(project)
        try {
            if (tokenService.token != "") {
                token = tokenService.token
                return true
            }
        } catch (e: NotLoggedException) {
            runCommandSync(NIGHTVISION, "login")
        }

        token = tokenService.createToken(project)
        return token != ""
    }

    fun logout(project: Project) {
        val tokenService = TokenService.getInstance(project)
        val response = runCommandSync(NIGHTVISION, "logout")
        println(response.output)
        tokenService.token = ""
        token = ""
    }
}