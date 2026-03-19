# NightVision IntelliJ Plugin

## Build and run

- Build: `./gradlew build` (or `make build`)
- Run in sandbox IDE: `./gradlew runIde` (or `make run`)
- Verify plugin compatibility: `./gradlew verifyPlugin` (or `make verify`)
- Package for distribution: `./gradlew buildPlugin` (produces .zip in build/distributions/)
- Clean: `./gradlew clean` (or `make clean`)

## Architecture

This is a Swing-based IntelliJ plugin that wraps the NightVision CLI.
No language parsing happens in the plugin — it delegates to the CLI via
`CommandRunnerService.runCommandSync()`. The plugin provides UI for:
API Discovery, DAST scans, target/authentication/project management.

## Key patterns

- CLI commands are built as varargs strings and executed via
  `GeneralCommandLine` in `CommandRunnerService.kt`
- API URL is resolved at runtime by `ApiUrlService.resolveApiUrl()` —
  do not hardcode API URLs
- Screen navigation is managed by `MainWindowFactory.kt`
- All HTTP API calls use the token from `LoginService.token`

## Supported languages (API Discovery)

The language list is in `ApiDiscovery.java`. Check
`nightvision swagger extract --help` for the CLI's actual `--lang` options.

## Git practices

- Always check `git status` before committing
- Put changes in the correct commit with accurate messages
- The user handles all pushes and PR creation — never push without
  explicit permission
