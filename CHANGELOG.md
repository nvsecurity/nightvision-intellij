# NightVision IntelliJ Plugin Changelog

## [Unreleased]

### Fixed

- Scans no longer stop after 30 seconds: the CLI is given a deadline to start a scan, not to finish one, so a scan runs for as long as it takes
- A scan that fails to start now reports the reason from the CLI instead of "Some error happened when creating your scan"
- The create-scan page stays put until the scan has started, so a failure is visible rather than reported on a screen you have already left
- The create-scan page says "Starting scan, please wait..." while the CLI starts the scan, which can take up to two minutes
- Project, target and authentication creation likewise report the CLI's reason
- The CLI update prompt explains itself on the Overview screen, naming the version installed, the version needed and the binary the plugin resolved
- The minimum CLI version is 0.15.0, the oldest able to scan a target that is not reachable from the internet
- Scans without an authentication no longer pass the CLI an empty `--auth` value
- Error messages can be selected and copied, so a CLI failure can be pasted into a support ticket instead of retyped from a screenshot
- Scan list vulnerability counts are color-coded by severity, and name the severity on hover

## [2.2.1]

### Fixed

- The plugin is now published as a signed build
- Detect the `nightvision` CLI using the login-shell PATH, so an installed CLI is found when the IDE is launched from the Dock or Finder

## [2.2.0]

### Added

- PHP and Go language support for API Discovery
- "All languages" option that runs extraction without specifying a language
- Pre-fill API Discovery path with the current project directory
- Meaningful tab names for generated OpenAPI specs
- Resolve API URL from CLI config, matching the CLI's configured environment
- Paginated scan list with next/previous navigation

### Fixed

- Plugin no longer crashes when no project is selected
- NV logo renders cleanly at all sizes
- Redesigned API Discovery form layout
- Improved error messages when no API routes are found
- Minimum supported IDE version raised to IntelliJ 2023.3
- Restyled the detail screens and applied consistent padding and spacing across the plugin
- Updated the NightVision website and support links to nightviz.ai
