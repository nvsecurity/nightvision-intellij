package net.nightvision.plugin.services

import net.nightvision.plugin.exceptions.NotLoggedException
import org.junit.Assert.*
import org.junit.Test

class ScanServiceTest {

    @Test
    fun `scan started marker matches the CLI details record`() {
        val output = """
            INFO Scan Details
            │ Scan ID: 4f1c2b0e-0000-4000-8000-000000000000
        """.trimIndent()
        assertTrue(ScanService.SCAN_STARTED.containsMatchIn(output))
    }

    @Test
    fun `timeout message carries what the CLI printed before it hung`() {
        val message = ScanService.startupTimedOutMessage(
            120_000L, "ERROR token has expired. Please try to log in again\n")
        assertTrue("got: $message", message.contains("token has expired"))
        // The caller routes on the message text, so the words have to survive.
        assertEquals(
            NotLoggedException::class.java,
            CommandRunnerService.getSpecificRuntimeException(
                listOf("nightvision", "scan"), RuntimeException(message)).javaClass)
    }

    @Test
    fun `timeout message stands alone when the CLI printed nothing`() {
        val message = ScanService.startupTimedOutMessage(120_000L, "   ")
        assertTrue("got: $message", message.contains("did not start a scan within 120 seconds"))
        assertFalse("got: $message", message.trimEnd().endsWith("\n"))
    }

    @Test
    fun `scan started marker does not match ordinary progress output`() {
        assertFalse(ScanService.SCAN_STARTED.containsMatchIn("INFO Connecting to relay"))
    }

    @Test
    fun `message reports the CLI stderr when the scan never starts`() {
        val message = ScanService.noScanStartedMessage("", "target not found in project")
        assertTrue(message.contains("target not found in project"))
    }

    @Test
    fun `message reports CLI output printed to stdout`() {
        // The CLI prints several fatal errors to stdout rather than logging
        // them, so a message built from stderr alone would be empty here.
        val message = ScanService.noScanStartedMessage("could not set up the relay tunnel", "")
        assertTrue(message.contains("could not set up the relay tunnel"))
    }

    @Test
    fun `message stands alone when the CLI reported nothing at all`() {
        val message = ScanService.noScanStartedMessage("", "")
        assertTrue(message.contains("without reporting a reason"))
    }

    @Test
    fun `stderr wins over stdout when both carry text`() {
        val message = ScanService.noScanStartedMessage("progress chatter", "the real reason")
        assertTrue(message.contains("the real reason"))
        assertFalse(message.contains("progress chatter"))
    }

    @Test
    fun `scan command passes the target as its own argument`() {
        val cmd = ScanService.buildScanCommand("my target with spaces", null)
        assertEquals(listOf("nightvision", "scan", "my target with spaces"), cmd)
    }

    @Test
    fun `scan command carries the authentication when one is chosen`() {
        val cmd = ScanService.buildScanCommand("t", "my-auth")
        assertEquals(listOf("nightvision", "scan", "t", "--auth", "my-auth"), cmd)
    }

    @Test
    fun `scan command omits the auth flag when no authentication is chosen`() {
        assertEquals(listOf("nightvision", "scan", "t"), ScanService.buildScanCommand("t", null))
    }

    @Test
    fun `scan command omits the auth flag for the combo box empty entry`() {
        // The create-scan combo carries an empty entry when the project has no
        // authentications; that must not become an --auth with an empty value.
        assertEquals(listOf("nightvision", "scan", "t"), ScanService.buildScanCommand("t", ""))
        assertEquals(listOf("nightvision", "scan", "t"), ScanService.buildScanCommand("t", "   "))
    }
}
