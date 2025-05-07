package net.nightvision.plugin.services

import com.intellij.openapi.project.Project
import net.nightvision.plugin.Constants.Companion.NIGHTVISION
import net.nightvision.plugin.exceptions.CommandNotFoundException
import net.nightvision.plugin.services.CommandRunnerService.runCommandSync
import java.util.concurrent.TimeUnit
import kotlin.jvm.Throws


object LoginService {
    var token = ""
        private set

    @Throws(CommandNotFoundException::class)
    fun bypassLoginStepIfAuthenticatedAlready(project: Project): Boolean {
        try {
            val tokenService = TokenService.getInstance(project)
            token = tokenService.createToken(project)
            return token != ""
        } catch (e: Exception) {
            return when (e) {
                is CommandNotFoundException -> throw e
                else                        -> false
            }
        }
    }

    fun login(project: Project): Boolean {
        val tokenService = TokenService.getInstance(project)
        if (tokenService.token != "") {
            return true
        }

        val response = runCommandSync(NIGHTVISION, "login")
        println(response.output)

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