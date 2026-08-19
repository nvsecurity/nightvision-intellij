package net.nightvision.plugin.services

import com.intellij.execution.configurations.GeneralCommandLine
import org.junit.Assert.*
import org.junit.Test

/**
 * Exercises the startup deadline against real processes. The question is
 * whether the deadline fires against a live child and leaves a started one
 * alone, which a stubbed process handler cannot answer.
 */
class CommandRunnerStartupTest {

    private val marker = Regex("INFO Scan Details")
    private val isWindows = System.getProperty("os.name").startsWith("Windows")

    /** A shell command, named per platform rather than skipped on Windows. */
    private fun shell(unix: String, windows: String): GeneralCommandLine =
        if (isWindows) GeneralCommandLine("cmd", "/c", windows)
        else GeneralCommandLine("sh", "-c", unix)

    private fun run(cmd: GeneralCommandLine, timeoutMs: Long) =
        CommandRunnerService.awaitStartup(cmd, marker, timeoutMs, listOf("test"))

    @Test
    fun `returns as soon as the marker appears and leaves the process running`() {
        val cmd = shell(
            "echo 'INFO Scan Details'; sleep 30",
            "echo INFO Scan Details& timeout /t 30 /nobreak > NUL"
        )
        val began = System.currentTimeMillis()
        val outcome = run(cmd, 30_000)
        val elapsed = System.currentTimeMillis() - began

        assertTrue("expected started", outcome.started)
        assertFalse(outcome.timedOut)
        // It must not have waited for the process to finish; the point of the
        // change is that a scan outlives the startup wait.
        assertTrue("returned after ${elapsed}ms, so it waited for exit", elapsed < 20_000)
    }

    @Test
    fun `reports a process that exits before the marker`() {
        val outcome = run(shell("echo boom; exit 3", "echo boom& exit 3"), 10_000)

        assertFalse(outcome.started)
        assertTrue("expected exited", outcome.exited)
        assertFalse(outcome.timedOut)
        assertTrue("output was '${outcome.output}'", outcome.output.contains("boom"))
    }

    @Test
    fun `captures output the process wrote to stderr`() {
        val outcome = run(shell("echo boom 1>&2; exit 1", "echo boom 1>&2& exit 1"), 10_000)

        assertFalse(outcome.started)
        assertTrue("output was '${outcome.output}'", outcome.output.contains("boom"))
    }

    @Test
    fun `stops a process that never reports a scan`() {
        val began = System.currentTimeMillis()
        val outcome = run(shell("sleep 30", "timeout /t 30 /nobreak > NUL"), 1_000)
        val elapsed = System.currentTimeMillis() - began

        assertFalse(outcome.started)
        assertTrue("expected timedOut", outcome.timedOut)
        assertFalse(outcome.exited)
        assertTrue("waited ${elapsed}ms for a 1s deadline", elapsed < 15_000)
    }

    @Test
    fun `a marker on a later line still counts as started`() {
        val outcome = run(
            shell(
                "echo warming up; echo 'INFO Scan Details'; sleep 30",
                "echo warming up& echo INFO Scan Details& timeout /t 30 /nobreak > NUL"
            ),
            30_000
        )
        assertTrue(outcome.started)
    }
}
