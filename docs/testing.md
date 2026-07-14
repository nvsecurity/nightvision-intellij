# Testing the NightVision IntelliJ Plugin

## Prerequisites

- JDK 17+
- NightVision CLI installed and on your PATH (`nightvision version` should work)
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

## NightVision CLI login

The plugin requires a valid NightVision CLI login. Run:

```bash
# Production
nightvision login

# Staging
nightvision login --api-url https://api.test.nightvision.net/api/v1/
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
