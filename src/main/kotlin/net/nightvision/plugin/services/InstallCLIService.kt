package net.nightvision.plugin.services

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

    fun getCLIDownloadUrl(platform: String, arch: String): String {
        // TODO: Consider throwing specific exception if platform and arch are not valid
        val platformStr = if (platform == "win32") "windows" else platform
        val archStr = if (arch == "x64") "amd64" else "arm64"
        val downloadUrl = "https://downloads.nightvision.net/binaries/latest/nightvision_latest_${platformStr}_${archStr}.tar.gz"
        return downloadUrl
    }

    fun installCLI(isUpdateCLI: Boolean): Boolean {
        val platform = System.getProperty("os.name").let {
            if (it.startsWith("Windows", ignoreCase = true)) "win32" else "unix"
        }
        val arch = System.getProperty("os.arch")

        val destDir = Path.of(getDestinationDirForPlatform())
        val cliExe = if (platform == "win32") {
            destDir.resolve("myApp.exe")
        } else {
            destDir.resolve("myApp")
        }

        // Already installed and no update requested? Just return
        if (cliExe.exists() && !isUpdateCLI) {
            return true
        }

        val downloadUrl = getCLIDownloadUrl(platform, arch)

        try {
            // 1) ensure install dir exists
            Files.createDirectories(destDir)

            // 2) download to temp file
            val temp = Files.createTempFile("myApp_", ".tar.gz")
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

            return true
        } catch (e: Exception) {
            // TODO: Improve exception handling here...
            val msg = e.message ?: e.javaClass.simpleName
            return false
        }
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

}