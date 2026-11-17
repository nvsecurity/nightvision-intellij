package net.nightvision.plugin.services

import net.nightvision.plugin.exceptions.CommandNotFoundException
import net.nightvision.plugin.exceptions.NotLoggedException
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class ProjectRecoveryTest {
    @After
    fun clearCachedProject() {
        ProjectService.updateCurrentProject("")
    }

    @Test
    fun `inaccessible saved project gives recovery and clears stale cached identity`() {
        ProjectService.updateCurrentProject("Name: Old project\nId: old-id")
        val reason = ProjectService.restoreCurrentProject {
            throw RuntimeException("This Project does not exist or is not shared with you.")
        }
        assertTrue(reason!!.contains("Choose an accessible project"))
        assertTrue(reason.contains("not shared with you"))
        assertEquals("", ProjectService.getCurrentProjectName())
        assertEquals("", ProjectService.getCurrentProjectId())
    }

    @Test
    fun `valid project bypasses recovery after selecting a replacement`() {
        ProjectService.restoreCurrentProject { throw RuntimeException("deleted") }
        val reason = ProjectService.restoreCurrentProject {
            ProjectService.updateCurrentProject("Name: Accessible project\nId: replacement-id")
        }
        assertNull(reason)
        assertEquals("Accessible project", ProjectService.getCurrentProjectName())
        assertEquals("replacement-id", ProjectService.getCurrentProjectId())
    }

    @Test
    fun `empty project metadata offers selection rather than silently opening with stale state`() {
        ProjectService.updateCurrentProject("Name: Old project\nId: old-id")
        assertNotNull(ProjectService.restoreCurrentProject { ProjectService.updateCurrentProject("") })
        assertEquals("", ProjectService.getCurrentProjectId())
    }

    @Test
    fun `missing CLI and expired authentication retain their original recovery routes`() {
        for (failure in listOf(CommandNotFoundException(listOf("nightvision")), NotLoggedException(listOf("nightvision")))) {
            ProjectService.updateCurrentProject("Name: Old project\nId: old-id")
            val thrown = assertThrows(failure.javaClass) { ProjectService.restoreCurrentProject { throw failure } }
            assertSame(failure, thrown)
            assertEquals("", ProjectService.getCurrentProjectId())
        }
    }

    @Test
    fun `without a project the target and credential services do not query unscoped resources`() {
        assertTrue(TargetService.getTargetInfos("URL", "").isEmpty())
        assertTrue(AuthenticationService.getAuthInfos("").isEmpty())
    }
}
