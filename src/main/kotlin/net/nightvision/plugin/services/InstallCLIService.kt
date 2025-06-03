package net.nightvision.plugin.services

import net.nightvision.plugin.Constants
import net.nightvision.plugin.services.CommandRunnerService.getDestinationDirForPlatform
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.attribute.PosixFilePermission
import java.util.*
import kotlin.io.path.exists

object InstallCLIService {

    fun installCLI(isUpdateCLI: Boolean) {
        val platform = normalizePlatform()
        val arch = normalizeArch()

        val destDir = Path.of(getDestinationDirForPlatform())
        val cliExe = if (platform == "win32") {
            destDir.resolve("nightvision.exe")
        } else {
            destDir.resolve("nightvision")
        }

        // Already installed and no update requested? Just return
        if (cliExe.exists() && !isUpdateCLI) {
            return
        }

        val downloadUrl = getCLIDownloadUrl(platform, arch)

        // 1) ensure install dir exists
        Files.createDirectories(destDir)

        // 2) download to temp file
        val temp = Files.createTempFile("nightvision_", ".tar.gz")
        downloadFile(downloadUrl, temp)

        // 3) unpack (relies on `tar` being on the PATH)
        val extract = ProcessBuilder("tar", "-xzf", temp.toString(), "-C", destDir.toString())
            .inheritIO()
            .start()
        if (extract.waitFor() != 0) {
            throw IOException("tar exited with ${extract.exitValue()}")
        }

        // 4) cleanup
        Files.deleteIfExists(temp)

        // 5) make executable on Unix
        if (platform != "win32") {
            val perms = EnumSet.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.GROUP_EXECUTE,
                PosixFilePermission.OTHERS_EXECUTE
            )
            Files.setPosixFilePermissions(cliExe, perms)
        }
    }

    /**
     * Compare two semantic‐version strings (“a.b.c” vs. “x.y.z”).
     *
     * Returns:
     *   - negative if verA < verB
     *   - zero     if verA == verB
     *   - positive if verA > verB
     */
    private fun compareCLIVersions(verA: String, verB: String): Int {
        // Split on dots, then parse each as Int. If one version is shorter, pad with zeroes.
        val partsA = verA.split('.').mapNotNull { it.toIntOrNull() }
        val partsB = verB.split('.').mapNotNull { it.toIntOrNull() }
        val length = maxOf(partsA.size, partsB.size)

        for (i in 0 until length) {
            val a = partsA.getOrNull(i) ?: 0
            val b = partsB.getOrNull(i) ?: 0
            if (a != b) {
                return a - b
            }
        }
        return 0
    }

    fun shouldUpdateCLI(cliVersion: String): Boolean {
        if (cliVersion.isBlank()) {
            return false
        }
        if (compareCLIVersions(cliVersion, Constants.CLI_VERSION) < 0) {
            return true
        }
        return false
    }

    fun getCLIDownloadUrl(platform: String, arch: String): String {
        // TODO: Consider throwing specific exception if platform and arch are not valid
        val platformStr = if (platform == "win32") "windows" else platform
        val archStr = if (arch == "x64") "amd64" else "arm64"
        val downloadUrl = "https://downloads.nightvision.net/binaries/latest/nightvision_latest_${platformStr}_${archStr}.tar.gz"
        return downloadUrl
    }

    private fun downloadFile(url: String, dest: Path) {
        val client = HttpClient.newHttpClient()
        val req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build()

        val res = client.send(req, HttpResponse.BodyHandlers.ofFile(dest))
        if (res.statusCode() != 200) {
            Files.deleteIfExists(dest)
            throw IOException("Download failed: HTTP ${res.statusCode()}")
        }
    }

    /**
     * Map JVM os.name → one of "windows", "darwin", "linux".
     * Throws on anything else.
     */
    fun normalizePlatform(osName: String = System.getProperty("os.name")): String {
        val lower = osName.lowercase()
        return when {
            lower.startsWith("windows")       -> "windows"
            lower.startsWith("mac")           // covers "Mac OS X", "Mac OS", etc.
                    || lower.startsWith("darwin")   // just in case
                -> "darwin"
            lower.startsWith("linux")         -> "linux"
            else -> throw IllegalStateException("Unsupported platform: $osName")
        }
    }

    /**
     * Map JVM os.arch → one of "amd64", "arm64".
     * Throws on anything else.
     */
    fun normalizeArch(osArch: String = System.getProperty("os.arch")): String {
        val arch = osArch.lowercase()
        return when {
            arch == "x86_64"  || arch == "amd64"     -> "amd64"
            arch.startsWith("aarch64")
                    || arch.startsWith("arm64")
                -> "arm64"
            else -> throw IllegalStateException("Unsupported architecture: $osArch")
        }
    }

}