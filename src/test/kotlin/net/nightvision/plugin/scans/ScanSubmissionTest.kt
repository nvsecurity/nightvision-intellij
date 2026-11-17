package net.nightvision.plugin.scans

import org.junit.Assert.*
import org.junit.Test
import javax.swing.JButton
import javax.swing.SwingUtilities

class ScanSubmissionTest {
    @Test
    fun `resources must be available before a scan can start`() {
        SwingUtilities.invokeAndWait {
            val button = JButton()
            val submission = ScanSubmission(button)
            assertFalse(button.isEnabled)
            assertFalse(submission.tryStart())

            submission.setTargetsAvailable(true)
            assertTrue(button.isEnabled)
            assertTrue(submission.tryStart())
            assertFalse(button.isEnabled)
        }
    }

    @Test
    fun `a project resource refresh cannot allow a second in-flight submission`() {
        SwingUtilities.invokeAndWait {
            val button = JButton()
            val submission = ScanSubmission(button)
            submission.setTargetsAvailable(true)
            assertTrue(submission.tryStart())

            submission.setTargetsAvailable(false)
            submission.setTargetsAvailable(true)
            assertFalse(button.isEnabled)
            assertFalse(submission.tryStart())

            submission.finish()
            assertTrue(button.isEnabled)
            assertTrue(submission.tryStart())
        }
    }

    @Test
    fun `a failed scan does not allow retry while resource refresh is incomplete`() {
        SwingUtilities.invokeAndWait {
            val button = JButton()
            val submission = ScanSubmission(button)
            submission.setTargetsAvailable(true)
            assertTrue(submission.tryStart())

            // Refresh begins, then fails or is cancelled without providing targets.
            submission.setTargetsAvailable(false)
            submission.finish()
            assertFalse(button.isEnabled)
            assertFalse(submission.tryStart())

            // A successful retry supplies targets after the scan worker has ended.
            submission.setTargetsAvailable(true)
            assertTrue(button.isEnabled)
            assertTrue(submission.tryStart())
        }
    }
}
