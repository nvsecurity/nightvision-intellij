# Testing the NightVision IntelliJ Plugin

## Prerequisites

- JDK 17+
- NightVision CLI 0.26.0 or newer installed and on your PATH (`nightvision version` should work)
- IntelliJ IDEA (any edition) for manual testing

## Building

```bash
./gradlew build
```

This compiles the plugin and runs the plugin verifier.

## Running locally

```bash
./gradlew runIde
```

This downloads IntelliJ Community 2024.3.5 (cached after first run) and
launches it with the NightVision plugin installed. Look for the NightVision
panel on the right sidebar.

## Plugin verification

```bash
./gradlew verifyPlugin
```

Runs the IntelliJ plugin verifier against IDE versions 2023.3.8 and
2024.3.5 to check for compatibility issues.

## Packaging for distribution

```bash
./gradlew buildPlugin
```

Produces a `.zip` file in `build/distributions/`. To install manually:

1. Open IntelliJ IDEA
2. Go to Settings > Plugins
3. Click the gear icon > Install Plugin from Disk
4. Select the `.zip` file

## Manual testing checklist

After launching with `./gradlew runIde`:

1. **Login** — Click the NightVision panel, verify the login flow works
2. **API Discovery** — Select a language (or "All supported"), pick a
   directory with source code, click Submit. Verify results appear.
3. **Projects** — Create a project, verify it appears in the list and
   in the project dropdown on other pages
4. **Targets** — Create a Web and an API target
5. **Scans** — Start a scan against a target
6. **Authentications** — Create a Playwright authentication

### Project recovery and complete resource lists

- With a valid CLI login and a saved project that was deleted or is no longer shared with you, open the tool window. Verify that project recovery offers accessible projects, project creation, and continuing without a project. Choose an accessible project and verify normal navigation resumes without logging in again.
- Repeat with no accessible projects. Verify project creation remains available and a failed project selection shows its reason while allowing another selection.
- Delay a project selection on the recovery screen, open Create project, and enter a name before selection finishes. Verify the late selection result leaves the creation form and input intact.
- Use more than one API page of projects, targets, and authentications. Verify later-page entries appear in their list screens and selectors, including scan setup, and optional authentication still defaults to none.
- Fail a later-page request or cancel loading. Verify the UI shows an error or cancellation with Retry, never offers a partial result as complete, and Retry can load successfully. Navigate away or change project during loading and verify old results do not overwrite the new screen's data.
- While a scan is starting, switch projects and let the new resource list finish loading. Verify Start Scan remains disabled until the first scan worker finishes. If the scan fails before a resource load finishes or after that load fails, verify retry remains disabled until targets are available.

## NightVision CLI login

The plugin requires a valid NightVision CLI login. Run:

```bash
# Production
nightvision login

# Test
nightvision login --api-url https://api.test.nightviz.ai/api/v1/
```

## Other commands

| Command | Description |
|---|---|
| `make build` | Build the plugin |
| `make run` | Launch sandbox IDE with plugin |
| `make dev` | Launch sandbox IDE with plugin (dev loop) |
| `make verify` | Run plugin verifier |
| `make package` | Package plugin as .zip |
| `make clean` | Remove build artifacts |
