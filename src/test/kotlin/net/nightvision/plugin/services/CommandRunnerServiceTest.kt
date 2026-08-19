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
}
