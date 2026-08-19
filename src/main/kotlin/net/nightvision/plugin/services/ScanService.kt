package net.nightvision.plugin.services

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.nightvision.plugin.Constants
import net.nightvision.plugin.Constants.Companion.NIGHTVISION
import net.nightvision.plugin.ScanInfo
import net.nightvision.plugin.PaginatedResult
import java.net.http.HttpClient
import java.time.Duration
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.TimeUnit

object ScanService {
    /**
     * The CLI logs this record as soon as the backend has accepted the scan, so
     * its presence is what distinguishes a started scan from a failed attempt.
     */
    val SCAN_STARTED = Regex("INFO Scan Details")

    val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build()
    val gson = GsonBuilder().create()

    fun getScans(page: Int = 1): PaginatedResult<ScanInfo> {
        val token = LoginService.token
        val params = mutableMapOf<String, String>("project" to ProjectService.getCurrentProjectId())
        if (page > 1) params["page"] = page.toString()
        val request = HttpRequest.newBuilder()
            .uri(Constants.getApiUrlFor("scans", params))
            .header("Authorization", "Token $token")
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            // Without this the error body parses to a PaginatedResult of nulls, or to
            // null outright, and the failure only surfaces as an NPE in the caller.
            throw RuntimeException("Could not load scans: HTTP ${response.statusCode()}")
        }
        val type = object : TypeToken<PaginatedResult<ScanInfo>>() {}.type
        return gson.fromJson(response.body(), type)
    }

    fun startScan(targetName: String, authenticationName: String?) {
        if (targetName.isBlank()) {
            throw IllegalArgumentException("Target name can't be empty.");
        }

        var cmd = ArrayList<String>(listOf(NIGHTVISION, "scan", targetName))
        if (authenticationName != null) {
            cmd.add("--auth")
            cmd.add(authenticationName)
        }

        val response = CommandRunnerService.runCommandSync(*cmd.toTypedArray())
        if (!SCAN_STARTED.containsMatchIn(response.output)) {
            throw RuntimeException(noScanStartedMessage(response.output, response.error))
        }
    }

    /**
     * Message for a CLI run that ended without ever logging its scan details.
     * The CLI's own output carries the reason (target not in the project, relay
     * setup failure, a rejected start-scan request), so it is reported rather
     * than replaced with a generic failure (NV-4827).
     */
    fun noScanStartedMessage(stdout: String, stderr: String): String {
        val detail = CommandRunnerService.failureDetail(stdout, stderr)
        if (detail.isEmpty()) {
            return "The NightVision CLI exited without starting a scan and without reporting a reason."
        }
        return "The NightVision CLI exited without starting a scan:\n${detail}"
    }
}