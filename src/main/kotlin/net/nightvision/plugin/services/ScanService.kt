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

    /**
     * How long the CLI is given to report a scan before the attempt is judged
     * to have hung. Only startup is bounded; once the scan is under way the CLI
     * runs for as long as the scan takes.
     */
    const val SCAN_START_TIMEOUT_MS = 120_000L

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

        val outcome = CommandRunnerService.runCommandUntilStarted(
            *cmd.toTypedArray(),
            marker = SCAN_STARTED,
            startupTimeoutMs = SCAN_START_TIMEOUT_MS
        )
        if (outcome.started) {
            return
        }
        val message = if (outcome.timedOut) {
            startupTimedOutMessage(SCAN_START_TIMEOUT_MS, outcome.output)
        } else {
            noScanStartedMessage(outcome.output, "")
        }
        // Routed through the same mapping runCommandSync uses, so an expired
        // login still sends the user to the login page rather than surfacing
        // as a bare failure.
        throw CommandRunnerService.getSpecificRuntimeException(
            cmd, RuntimeException(message)
        )
    }

    /**
     * Message for a CLI that never reported a scan and had to be stopped. The
     * relay is named because it is the usual cause: a target that is not
     * reachable from the internet is scanned through it, and an older CLI can
     * obtain a relay id and then never bring the tunnel up (NV-4827).
     *
     * What the CLI printed before it hung is appended rather than dropped. It
     * carries the real reason whenever the relay is not the cause, and the
     * caller routes an expired login to the login page by matching the message
     * text, which cannot match words that were never included. It goes through
     * significantOutput first, so the progress narration a hung scan produces
     * most of does not push that reason off the screen.
     */
    fun startupTimedOutMessage(timeoutMs: Long, output: String): String {
        val seconds = Math.round(timeoutMs / 1000.0)
        val message = "The NightVision CLI did not start a scan within $seconds seconds and was " +
            "stopped. The most common cause is a failed connection to the NightVision " +
            "relay, which is required for targets that are not reachable from the internet."
        val detail = CommandRunnerService.significantOutput(output)
        if (detail.isEmpty()) {
            return message
        }
        return "$message\n$detail"
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