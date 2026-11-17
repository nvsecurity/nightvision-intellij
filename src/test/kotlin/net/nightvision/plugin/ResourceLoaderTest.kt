package net.nightvision.plugin

import org.junit.Assert.*
import org.junit.Test
import java.awt.event.ContainerAdapter
import java.awt.event.ContainerEvent
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SwingUtilities

class ResourceLoaderTest {
    @Test
    fun `fetch runs off the EDT and only the latest complete load reaches the UI`() {
        val firstStarted = CountDownLatch(1)
        val releaseFirst = CountDownLatch(1)
        val accepted = CountDownLatch(1)
        val requests = AtomicInteger()
        val values = mutableListOf<Int>()
        lateinit var loader: ResourceLoader<Int>
        SwingUtilities.invokeAndWait {
            val owner = JPanel()
            JPanel().add(owner)
            loader = ResourceLoader(owner, JPanel(), {}, {
                assertFalse(SwingUtilities.isEventDispatchThread())
                val request = requests.incrementAndGet()
                if (request == 1) {
                    firstStarted.countDown()
                    // Simulate a transport that takes time to honor cancellation.
                    try { releaseFirst.await(5, TimeUnit.SECONDS) } catch (_: InterruptedException) { }
                }
                request
            }, {
                assertTrue(SwingUtilities.isEventDispatchThread())
                values.add(it)
                accepted.countDown()
            })
            loader.load()
        }
        try {
            assertTrue(firstStarted.await(5, TimeUnit.SECONDS))
            SwingUtilities.invokeAndWait { loader.load() }
            assertTrue(accepted.await(5, TimeUnit.SECONDS))
            SwingUtilities.invokeAndWait { assertEquals(listOf(2), values) }
        } finally {
            releaseFirst.countDown()
        }
    }

    @Test
    fun `failed or cancelled loads replace the spinner with a retry that can succeed`() {
        for (cancel in listOf(false, true)) {
            val started = CountDownLatch(1)
            val retryShown = CountDownLatch(1)
            val accepted = CountDownLatch(1)
            val requests = AtomicInteger()
            lateinit var loader: ResourceLoader<Int>
            lateinit var status: JPanel
            SwingUtilities.invokeAndWait {
                val owner = JPanel()
                JPanel().add(owner)
                status = JPanel()
                status.addContainerListener(object : ContainerAdapter() {
                    override fun componentAdded(event: ContainerEvent) {
                        if ((event.child as? JButton)?.text == "Retry") retryShown.countDown()
                    }
                })
                loader = ResourceLoader(owner, status, {}, {
                    if (requests.incrementAndGet() == 1) {
                        started.countDown()
                        if (cancel) CountDownLatch(1).await(5, TimeUnit.SECONDS)
                        throw IllegalStateException("HTTP 503")
                    }
                    42
                }, { assertEquals(42, it); accepted.countDown() })
                loader.load()
            }
            assertTrue(started.await(5, TimeUnit.SECONDS))
            if (cancel) SwingUtilities.invokeAndWait { loader.cancel() }
            assertTrue(retryShown.await(5, TimeUnit.SECONDS))
            SwingUtilities.invokeAndWait {
                status.components.filterIsInstance<JButton>().single { it.text == "Retry" }.doClick()
            }
            assertTrue(accepted.await(5, TimeUnit.SECONDS))
            SwingUtilities.invokeAndWait { assertEquals(0, status.componentCount) }
        }
    }

    @Test
    fun `leaving a screen cancels its request without applying results`() {
        val started = CountDownLatch(1)
        val interrupted = CountDownLatch(1)
        lateinit var owner: JPanel
        lateinit var root: JPanel
        SwingUtilities.invokeAndWait {
            owner = JPanel()
            root = JPanel()
            root.add(owner)
            ResourceLoader(owner, JPanel(), {}, {
                started.countDown()
                try {
                    CountDownLatch(1).await(5, TimeUnit.SECONDS)
                } catch (ex: InterruptedException) {
                    interrupted.countDown()
                    throw ex
                }
                1
            }, { fail("Detached screen accepted a result") }).load()
        }
        assertTrue(started.await(5, TimeUnit.SECONDS))
        SwingUtilities.invokeAndWait { root.remove(owner) }
        assertTrue(interrupted.await(5, TimeUnit.SECONDS))
    }
}
