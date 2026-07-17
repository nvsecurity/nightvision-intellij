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
}
