---
name: release-process
description: Release and publish process for the NightVision IntelliJ plugin
---

# IntelliJ Plugin Release Process

## Overview

The release pipeline is: open PR -> PR Test runs build + verify -> merge PR ->
Release Drafter updates a draft GitHub Release -> bump the version and
change-notes -> publish the GitHub Release -> plugin is built, signed, and
published to the JetBrains Marketplace.

Unlike the VS Code plugin, the version is **not** auto-bumped after a release;
you bump it by hand before publishing (see Version numbering).

## Pre-release checklist

1. Tests pass and the plugin builds + verifies: `make test`
   (`./gradlew build verifyPlugin`)
2. Plugin verifier clean against the target IDEs: `make verify`
   (`./gradlew verifyPlugin`, run against `IC-2023.3.8` and `IC-2024.3.5`)
3. Production package builds: `make package` (`./gradlew buildPlugin`, produces
   `build/distributions/intellij-idea-nightvision-<version>.zip`)
4. Manual smoke test of the `.zip`: IntelliJ > Settings > Plugins > gear icon >
   Install Plugin from Disk...
5. `version` in `build.gradle.kts` bumped to the release version
6. `plugin.xml` `<change-notes>` updated with a section for the new version
7. All of the above merged to `main`

## Automated workflows

### PR Test (`.github/workflows/pr-test.yml`)

- Triggers on every `pull_request`
- Runs `./gradlew build verifyPlugin` on JDK 17 (Temurin)
- Acts as the merge gate

### Release Drafter (`.github/workflows/release-drafter.yml`)

- Triggers on every push to `main`
- Drafts/updates a GitHub Release from merged PR titles (release-drafter v5)
- Config: `.github/release-drafter.yml`
- PR labels control categorization:
  - `feature`, `enhancement` -> Features
  - `fix`, `bugfix`, `bug`, `quick-fix` -> Bug Fixes
  - `chore`, `maintenance`, `maintain`, `cleanup` -> Maintenance
  - `documentation`, `docs` -> Documentation
- PR labels also control version resolution:
  - `major` -> major bump
  - `minor` -> minor bump
  - `patch` (or no label, default) -> patch bump
- The resolved version drives the release name/tag (`$RESOLVED_VERSION`), but the
  version that actually ships is read from `build.gradle.kts` - keep them in sync.

### Publish - "Release to JetBrains Marketplace" (`.github/workflows/publish.yml`)

- Triggers when a GitHub Release is published, or via manual `workflow_dispatch`
- Runs in the `jetbrains-marketplace` environment on JDK 17 (Temurin)
- Three sequential Gradle steps:
  1. `./gradlew buildPlugin` - builds the distribution zip
  2. `./gradlew signPlugin` - signs it with `CERTIFICATE_CHAIN`, `PRIVATE_KEY`,
     `PRIVATE_KEY_PASSWORD`
  3. `./gradlew publishPlugin` - uploads it to the JetBrains Marketplace using
     `PUBLISH_TOKEN`
- There is no automatic version-bump job (the VS Code plugin has one; this repo
  does not).

## Secrets required

Stored in the `jetbrains-marketplace` GitHub environment:

| Secret | Purpose |
|--------|---------|
| `CERTIFICATE_CHAIN` | Plugin signing certificate chain |
| `PRIVATE_KEY` | Plugin signing private key |
| `PRIVATE_KEY_PASSWORD` | Password for the signing private key |
| `PUBLISH_TOKEN` | JetBrains Marketplace upload token (Hub permanent token) |
| `GITHUB_TOKEN` | (automatic) Release Drafter |

## Package output

`./gradlew buildPlugin` produces `build/distributions/intellij-idea-nightvision-<version>.zip`
(the artifact name comes from `rootProject.name` in `settings.gradle.kts` plus the
`version` in `build.gradle.kts`). The zip contains the plugin jar and its bundled
runtime dependencies under `lib/`, with the version and `sinceBuild` injected into
`plugin.xml` by the `patchPluginXml` task. `signPlugin` rewrites this zip as a
signed artifact, and that signed zip is what `publishPlugin` uploads.

The plugin wraps the NightVision CLI and does not bundle it; the CLI is installed
or located at runtime.

## Manual release steps

1. Confirm `version` in `build.gradle.kts` and the `plugin.xml` `<change-notes>`
   are updated for the new version and merged to `main`.
2. Go to the GitHub repository's Releases page.
3. Find the draft release created by Release Drafter.
4. Review and edit the release notes; confirm the tag matches the
   `build.gradle.kts` version.
5. Click "Publish release".
6. The "Release to JetBrains Marketplace" workflow runs automatically
   (build -> sign -> publish).
7. Verify the new version appears on the JetBrains Marketplace listing. Marketplace
   review/indexing can take a while, so it may not be live immediately.

## Local package testing

Build and install locally before releasing:

```bash
./gradlew buildPlugin
# Then in IntelliJ: Settings > Plugins > gear icon > Install Plugin from Disk...
# and select build/distributions/intellij-idea-nightvision-<version>.zip
```

Check IDE compatibility separately:

```bash
./gradlew verifyPlugin
```

## Publishing (manual fallback)

If the workflow fails or you need to publish locally:

```bash
export CERTIFICATE_CHAIN="..." PRIVATE_KEY="..." PRIVATE_KEY_PASSWORD="..."
export PUBLISH_TOKEN="..."
./gradlew buildPlugin
./gradlew signPlugin
./gradlew publishPlugin
```

Signing requires a certificate chain and private key - see the JetBrains
[plugin signing docs](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html).
The `PUBLISH_TOKEN` is a permanent token from your JetBrains Marketplace account
([Marketplace > My Tokens](https://plugins.jetbrains.com/author/me/tokens)).

## Marketplace details

- **Plugin ID:** `net.nightvision.plugin` (declared in `plugin.xml`)
- **Plugin name:** Nightvision
- **Distribution:** JetBrains Marketplace (find it by searching "NightVision")
- **Vendor:** NightVision (support@nightviz.ai, https://www.nightviz.ai/)
- **Minimum IDE:** `sinceBuild` 233 (IntelliJ 2023.3+), set in `patchPluginXml`

## Version numbering

- The version is the `version` field in `build.gradle.kts` (currently `2.2`).
  `patchPluginXml` injects it into the shipped `plugin.xml`.
- Bumps are **manual**. Nothing auto-increments the version after a publish, so
  bump `build.gradle.kts` before publishing a release.
- Update `plugin.xml` `<change-notes>` with a matching `<h3>` section for the new
  version - this is the user-facing changelog shown in the IDE Plugin Manager
  (there is no `CHANGELOG.md`).
- Use PR labels (`major`, `minor`, `patch`) to steer the Release Drafter's
  suggested tag, then make sure the final tag matches `build.gradle.kts`.

## Troubleshooting

- **Signing fails** ("key" / "certificate" errors): verify `CERTIFICATE_CHAIN`,
  `PRIVATE_KEY`, and `PRIVATE_KEY_PASSWORD` in the `jetbrains-marketplace`
  environment. See the JetBrains plugin signing docs linked above.
- **Publish fails with an auth error**: `PUBLISH_TOKEN` may be expired or lack
  permission for this plugin. Regenerate it at
  [Marketplace > My Tokens](https://plugins.jetbrains.com/author/me/tokens).
- **`verifyPlugin` failures**: an API incompatibility against `IC-2023.3.8` or
  `IC-2024.3.5`. Check the flagged API usage and the `sinceBuild` floor.
- **Release Drafter not updating**: ensure PRs are merged to `main` and the
  action has `GITHUB_TOKEN` permissions. Note that the Maintenance and
  Documentation categories in `.github/release-drafter.yml` use `label:`
  (singular) rather than `labels:`; PRs with those labels may not be categorized
  until that is corrected.
- **Published version doesn't match the release tag**: the shipped version comes
  from `build.gradle.kts`, not the git tag. Bump `build.gradle.kts` before
  publishing.

## Relationship to VS Code plugin

The VS Code plugin (`nvsecurity/nightvision-vscode`) has an analogous release
process built on the same Release Drafter setup and GitHub Release trigger. Key
differences:

- IntelliJ requires plugin signing (`CERTIFICATE_CHAIN` / `PRIVATE_KEY`); VS Code
  does not.
- IntelliJ version bumps are manual (`build.gradle.kts`); VS Code auto-bumps
  `package.json` after publish.
- IntelliJ change-notes live in `plugin.xml`; VS Code uses `CHANGELOG.md`.
- IntelliJ publishes to the JetBrains Marketplace via `publishPlugin`; VS Code
  publishes to the VS Code Marketplace via `@vscode/vsce`.
