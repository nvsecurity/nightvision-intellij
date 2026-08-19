package net.nightvision.plugin.services

import net.nightvision.plugin.exceptions.CommandNotFoundException
import net.nightvision.plugin.exceptions.NotLoggedException
import net.nightvision.plugin.exceptions.PermissionDeniedException
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class CommandRunnerServiceTest {

    private val testCommand = listOf("nightvision", "test")

    private val cliDir = "/home/user/.local/nightvision/bin"

    @Test
    fun `prependCliDir prepends install dir when base path lacks it`() {
        val base = "/usr/bin${File.pathSeparator}/bin"
        val result = CommandRunnerService.prependCliDir(cliDir, base)
        assertEquals(cliDir + File.pathSeparator + base, result)
    }

    @Test
    fun `prependCliDir leaves base path unchanged when install dir is already first`() {
        val base = "$cliDir${File.pathSeparator}/usr/bin"
        val result = CommandRunnerService.prependCliDir(cliDir, base)
        assertEquals(base, result)
    }

    @Test
    fun `prependCliDir yields install dir when base path is empty`() {
        val result = CommandRunnerService.prependCliDir(cliDir, "")
        assertEquals(cliDir + File.pathSeparator, result)
    }

    @Test
    fun `file not found error maps to CommandNotFoundException`() {
        val ex = RuntimeException("Cannot run program: error=2, No such file or directory")
        val result = CommandRunnerService.getSpecificException(testCommand, ex)
        assertTrue(result is CommandNotFoundException)
    }

    @Test
    fun `permission denied error maps to PermissionDeniedException`() {
        val ex = RuntimeException("Cannot run program: error=13, Permission denied")
        val result = CommandRunnerService.getSpecificException(testCommand, ex)
        assertTrue(result is PermissionDeniedException)
    }

    @Test
    fun `expired token error maps to NotLoggedException`() {
        val ex = RuntimeException("token has expired. Please try to log in again")
        val result = CommandRunnerService.getSpecificException(testCommand, ex)
        assertTrue(result is NotLoggedException)
    }

    @Test
    fun `unrecognized error passes through unchanged`() {
        val ex = RuntimeException("something unexpected happened")
        val result = CommandRunnerService.getSpecificException(testCommand, ex)
        assertSame(ex, result)
    }

    @Test
    fun `null message passes through unchanged`() {
        val ex = RuntimeException()
        val result = CommandRunnerService.getSpecificException(testCommand, ex)
        assertSame(ex, result)
    }

    @Test
    fun `No such file or directory text maps to CommandNotFoundException`() {
        val ex = RuntimeException("No such file or directory")
        val result = CommandRunnerService.getSpecificException(testCommand, ex)
        assertTrue(result is CommandNotFoundException)
    }

    @Test
    fun `Permission denied text maps to PermissionDeniedException`() {
        val ex = RuntimeException("Permission denied")
        val result = CommandRunnerService.getSpecificException(testCommand, ex)
        assertTrue(result is PermissionDeniedException)
    }

    @Test
    fun `failureDetail prefers stderr`() {
        assertEquals("boom", CommandRunnerService.failureDetail("chatter", "boom"))
    }

    @Test
    fun `failureDetail falls back to stdout when stderr is empty`() {
        // Several CLI errors are printed to stdout rather than logged, so a
        // detail taken from stderr alone is empty for exactly those failures.
        assertEquals("boom", CommandRunnerService.failureDetail("boom", ""))
    }

    @Test
    fun `failureDetail is empty when the process said nothing`() {
        assertEquals("", CommandRunnerService.failureDetail("  ", "\n"))
    }

    @Test
    fun `significantOutput drops the CLI progress narration`() {
        // A failed scan narrates DNS, TCP and TLS before it says what went
        // wrong, and no screen in the plugin scrolls, so the narration pushed
        // the reason out of the tool window.
        val output = """
            [2026-08-21 08:18:17] INFO Launching NightVision CLI
            [2026-08-21 08:18:18] INFO Target connectivity test: starting DNS query
            [2026-08-21 08:18:18] INFO Target connectivity test: TLS handshake done
            target public-javaspringvulny does not exist
        """.trimIndent()
        assertEquals(
            "target public-javaspringvulny does not exist",
            CommandRunnerService.significantOutput(output)
        )
    }

    @Test
    fun `significantOutput keeps an expired login even when logged at INFO`() {
        // getSpecificRuntimeException reclassifies on this phrase, so dropping
        // the line carrying it would strand the user on a generic error
        // instead of the login page.
        val output = "[2026-08-21 08:18:17] INFO Launching NightVision CLI\n" +
            "[2026-08-21 08:18:18] INFO It seems your login authentication " +
            "token has expired. Please try to log in again."
        val detail = CommandRunnerService.significantOutput(output)
        assertTrue(CommandRunnerService.EXPIRED_TOKEN_PHRASE in detail)
    }

    @Test
    fun `significantOutput keeps the tail when everything is narration`() {
        val output = (1..30).joinToString("\n") { "[2026-08-21 08:18:17] INFO step $it" }
        val detail = CommandRunnerService.significantOutput(output)
        assertTrue(detail.endsWith("INFO step 30"))
        assertEquals(12, detail.lines().size)
    }

    @Test
    fun `significantOutput bounds a reason that runs long`() {
        val output = (1..40).joinToString("\n") { "reason line $it" }
        val detail = CommandRunnerService.significantOutput(output)
        assertEquals(12, detail.lines().size)
        assertTrue(detail.endsWith("reason line 40"))
    }

    @Test
    fun `resolveCliPathIn returns the first match on the search path`() {
        val sep = File.pathSeparator
        val path = "/first${sep}/second"
        val found = CommandRunnerService.resolveCliPathIn(
            path, windows = false, isExecutable = { it.path == "/second/nightvision" })
        assertEquals("/second/nightvision", found)
    }

    @Test
    fun `resolveCliPathIn prefers the earlier directory`() {
        val sep = File.pathSeparator
        // The plugin prepends its own install dir, which is why a copy it
        // installed once shadows a newer CLI installed later (NV-4873).
        val path = "$cliDir${sep}/usr/local/bin"
        val found = CommandRunnerService.resolveCliPathIn(
            path, windows = false, isExecutable = { true })
        assertEquals("$cliDir/nightvision", found)
    }

    @Test
    fun `resolveCliPathIn looks for the exe name on Windows`() {
        val found = CommandRunnerService.resolveCliPathIn(
            "C:\\tools", windows = true,
            isExecutable = { it.name == "nightvision.exe" })
        assertTrue("got $found", found!!.endsWith("nightvision.exe"))
    }

    @Test
    fun `resolveCliPathIn falls back to cmd and bat on Windows`() {
        val found = CommandRunnerService.resolveCliPathIn(
            "C:\\tools", windows = true,
            isExecutable = { it.name == "nightvision.cmd" })
        assertTrue("got $found", found!!.endsWith("nightvision.cmd"))
    }

    @Test
    fun `resolveCliPathIn prefers bat over cmd, as PATHEXT does`() {
        val found = CommandRunnerService.resolveCliPathIn(
            "C:\\tools", windows = true,
            isExecutable = { it.name == "nightvision.cmd" || it.name == "nightvision.bat" })
        assertTrue("got $found", found!!.endsWith("nightvision.bat"))
    }

    @Test
    fun `resolveCliPathIn returns null when nothing is on the path`() {
        assertNull(CommandRunnerService.resolveCliPathIn(
            "/nowhere", windows = false, isExecutable = { false }))
    }

    @Test
    fun `resolveCliPathIn skips empty path entries`() {
        val sep = File.pathSeparator
        val found = CommandRunnerService.resolveCliPathIn(
            "${sep}${sep}/only", windows = false, isExecutable = { it.path == "/only/nightvision" })
        assertEquals("/only/nightvision", found)
    }
}
