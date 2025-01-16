package net.nightvision.plugin.intellij.services

import java.util.concurrent.TimeUnit


object LoginService {
     var token = ""
        private set

    fun createToken(): String {
        val process = ProcessBuilder("nightvision", "token", "create")
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        process.waitFor(30, TimeUnit.SECONDS)
        val t = process.inputStream.bufferedReader().readText()
        val token = t.trim().takeIf { it.matches(Regex("^\\S{64}$")) } ?: ""
        println(token)
        return token
    }

    fun login(): Boolean {
        if (token != "") {
            return true
        }
        val process = ProcessBuilder("nightvision", "login")
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        process.waitFor(30, TimeUnit.SECONDS)
        println(process.inputStream.bufferedReader().readText())

        token = createToken()
        return token != ""
    }

    fun logout() {
        val process = ProcessBuilder("nightvision", "logout")
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        process.waitFor(30, TimeUnit.SECONDS)
        println(process.inputStream.bufferedReader().readText())
        token = ""
    }
}