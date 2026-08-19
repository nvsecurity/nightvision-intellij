package net.nightvision.plugin.services

import net.nightvision.plugin.Constants
import org.junit.Assert.*
import org.junit.Test

class InstallCLIServiceTest {

    @Test
    fun `the version floor is the one that can scan through the relay`() {
        assertEquals("0.15.0", Constants.CLI_VERSION)
    }

    @Test
    fun `a CLI below the floor is offered an update`() {
        // 0.9.15 is the version a customer ran under the VS Code extension
        // while the floor was 0.9.5, so it was never prompted (NV-4872).
        assertTrue(InstallCLIService.shouldUpdateCLI("0.9.15"))
        assertTrue(InstallCLIService.shouldUpdateCLI("0.14.9"))
    }

    @Test
    fun `a CLI at or above the floor is left alone`() {
        assertFalse(InstallCLIService.shouldUpdateCLI("0.15.0"))
        assertFalse(InstallCLIService.shouldUpdateCLI("0.16.2"))
        assertFalse(InstallCLIService.shouldUpdateCLI("1.0.0"))
    }

    @Test
    fun `an unknown version is not prompted`() {
        assertFalse(InstallCLIService.shouldUpdateCLI(""))
    }

    @Test
    fun `version comparison orders by component, not lexically`() {
        // "0.9.15" sorts above "0.15.0" as text; the comparison must not.
        assertTrue(InstallCLIService.shouldUpdateCLI("0.9.15"))
    }
}
