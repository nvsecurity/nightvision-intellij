package net.nightvision.plugin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the API Discovery language dropdown to the --lang values the CLI actually
 * accepts. Verified against `nightvision swagger extract --help`, which reports:
 *
 *   -l, --lang string  Language of the target code (csharp, go, java, js, php, python, ruby)
 *
 * If the CLI's accepted values change, update CLI_ACCEPTED_LANGS here and the map
 * in ApiDiscovery together.
 */
class ApiDiscoveryLanguagesTest {

    private val cliAcceptedLangs = setOf("csharp", "go", "java", "js", "php", "python", "ruby")

    /** Not a CLI value: ApiDiscoveryService omits --lang entirely when it sees this. */
    private val allLanguagesSentinel = "all"

    @Test
    fun `every language offered in the dropdown has a CLI id`() {
        for (display in ApiDiscovery.LANGUAGES) {
            assertTrue(
                "Language '$display' is offered in the dropdown but has no LANGUAGE_CLI_IDS entry, " +
                    "so its display name would be passed to --lang verbatim",
                ApiDiscovery.LANGUAGE_CLI_IDS.containsKey(display)
            )
        }
    }

    @Test
    fun `every mapped CLI id is one the CLI accepts`() {
        for ((display, cliId) in ApiDiscovery.LANGUAGE_CLI_IDS) {
            if (cliId == allLanguagesSentinel) continue
            assertTrue(
                "'$cliId' (mapped from '$display') is not a --lang value the CLI accepts: $cliAcceptedLangs",
                cliAcceptedLangs.contains(cliId)
            )
        }
    }

    @Test
    fun `C# maps to csharp, not dotnet`() {
        assertEquals("csharp", ApiDiscovery.LANGUAGE_CLI_IDS["C#"])
    }

    @Test
    fun `all languages maps to the sentinel rather than a CLI language`() {
        assertEquals(allLanguagesSentinel, ApiDiscovery.LANGUAGE_CLI_IDS["All languages"])
        assertTrue(
            "The all-languages sentinel must not collide with a real CLI language",
            !cliAcceptedLangs.contains(allLanguagesSentinel)
        )
    }

    @Test
    fun `dropdown and CLI id map stay in sync`() {
        assertEquals(
            "Every LANGUAGE_CLI_IDS key should be offered in the dropdown",
            ApiDiscovery.LANGUAGES.toSortedSet(),
            ApiDiscovery.LANGUAGE_CLI_IDS.keys.toSortedSet()
        )
    }
}
