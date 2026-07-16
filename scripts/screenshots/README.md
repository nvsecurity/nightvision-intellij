# Marketplace screenshot procedure

How to regenerate the JetBrains Marketplace screenshots for the NightVision
IntelliJ plugin. Unlike the VS Code extension's Playwright harness, this
plugin is a native Swing tool window with no DOM, so capture is a macOS
screen-automation procedure (assisted, not a one-command script) followed by
a deterministic crop step (`crop.py`).

The canonical PNGs are committed under `docs-assets/screenshots/` in this
(public) repo and serve two consumers: they are uploaded by hand into the
Marketplace vendor console (plugin edit page, "Plugin Screenshots", which
JetBrains re-hosts), and they are referenced by the NightVision IntelliJ
documentation page through this repo's raw GitHub URLs. Because a docs page
may hotlink those raw URLs, treat committed filenames as stable once
referenced: refresh in place, avoid renaming or deleting. Nothing under
`docs-assets/` or `scripts/` is bundled into the plugin `.zip` (`buildPlugin`
ships only the jar).

## When to regenerate

After any change to the tool-window UI (Overview, API Discovery, the detail
screens, the scan/target/authentication/project lists). Screenshots drift
silently; the listing keeps showing the old UI until someone re-shoots.

## Shot inventory

Eight panel-only crops, one uniform crop box so they share an aspect ratio
(JetBrains recommends against mixed aspect ratios in a single listing).
Committed under `docs-assets/screenshots/` with feature-based names; carousel
ordering is set at upload time in the console, so no numeric prefixes:

- `overview.png`: the two top-level actions
- `api-discovery.png`: discovery form with the project path pre-filled
- `api-discovery-languages.png`: API Language dropdown open (all languages)
- `security-testing-menu.png`: Scans / Targets / Authentications / Projects
- `scans-list.png`: scan list with vulnerability-count badges
- `scan-details.png`: restyled stacked detail layout
- `targets-list.png`: target list (OpenAPI + Web)
- `target-details.png`: the richest detail screen (base URL, spec, status)

The two API Discovery shots expose the discovery path field, so capture them
with a neutral path (e.g. `/tmp/javaspringvulny`) -- never commit a shot
showing a real home directory. They are held out of the committed set until
re-captured that way.

## Prerequisites

1. `nightvision` CLI on PATH, logged in as the demo account `nv-demo`
   (nv-demo@nightvision.net). The plugin reads the CLI session, so whoever
   is logged into the CLI is who the data screens show. The account's
   fixtures (project `nv-demo`, targets `javaspringvulny` and
   `public-firing-range`, at least two completed scans) are what screens
   5-8 display.
2. A local clone of a demo project to open in the sandbox IDE and to
   pre-fill the API Discovery path, e.g.
   https://github.com/vulnerable-apps/javaspringvulny. The path is visible
   in the discovery shot, so keep it neutral.
3. `cliclick` for synthetic input: `brew install cliclick`.
4. macOS **Screen Recording** and **Accessibility** granted to the process
   that runs `cliclick`/`screencapture`: the terminal or its host
   (in a tmux session it is the tmux binary, e.g. `/opt/homebrew/bin/tmux`,
   NOT the terminal app). macOS silently denies synthetic input to a
   CLI-launched process instead of prompting, so grant it manually in
   System Settings > Privacy & Security.
5. Python 3 with Pillow for `crop.py` (a venv is fine:
   `python3 -m venv venv && venv/bin/pip install Pillow`).

## Capture

1. Launch the sandbox IDE with the plugin installed: `make dev`
   (`./gradlew runIde`). The plugin is auto-installed; there is no separate
   install or in-IDE login step; the login screen detects the CLI session
   and lands on Overview.
2. Open the demo project (Welcome > Open, or Cmd+Shift+G to type the path).
   Dismiss the Trust dialog (Trust Project) and cancel any Download JDK
   prompt; a JDK is not needed for the tool window.
3. Position the IDE window at a known origin and size so the capture region
   and crop box below are valid. This procedure was calibrated with the
   window at logical `(56, 54)` size `1400x908` on a 2x (retina) display:

   ```sh
   osascript -e 'tell application "System Events" to tell process "java"
     set frontmost to true
     set position of window 1 to {56, 54}
     set size of window 1 to {1400, 908}
   end tell'
   ```

4. Open the NightVision tool window (the "N" icon in the right tool stripe)
   and clean up the frame: close the AI Assistant editor tab, hide the Build
   tool window, and Option-click a notification balloon's close button to
   dismiss all notifications.
5. Navigate each screen in the inventory and capture the full IDE window to
   a native (2x) PNG. The window must stay frontmost and unobstructed:
   `screencapture -R` grabs whatever pixels occupy the rectangle, so a
   window on top bleeds in.

   ```sh
   # Full window at the calibrated geometry -> 2800x1816 native PNG.
   screencapture -x -R "56,54,1400,908" overview.png
   ```

   Clicks use `cliclick c:X,Y` in logical screen coordinates. To convert a
   point seen in a native 2x capture back to a logical click: the window is
   at `(56, 54)`, so `logical = 56 + native_x/2` (x) and `54 + native_y/2`
   (y). Read each capture, aim the next click, verify, repeat. The panel
   back-arrow sits at roughly logical `(980, 165)`; the top-level buttons
   are centered near logical `x=1183`.

## Crop

`crop.py` crops each full-window capture to the tool-window panel with one
fixed box, producing uniform ~`931x1319` panel-only PNGs.

```sh
venv/bin/python scripts/screenshots/crop.py <src-dir> <out-dir>
```

The canonical `<out-dir>` is `docs-assets/screenshots/`; review the crops,
commit them there, and upload the same files to the Marketplace carousel.

The default box `(1799, 171, 2730, 1490)` is calibrated to the window
geometry above. If you capture at a different window position, size, or
display scale, recompute it: crop `target-details.png` (the tallest
screen) with a candidate box, eyeball that the left edge starts at the panel
divider, the right edge stops before the tool stripe, the top clears the
editor tab bar, and the bottom clears "Check in Browser". Pass a custom box
with `--box left,top,right,bottom`.

## Resolution note

The panel is ~465 logical px wide, so a native 2x crop is ~931 px wide:
crisp, but under JetBrains' *recommended* (not enforced) 1200x760. Cropping
never dilutes; only upscaling would, so do not upscale. If you want >=1200 px
of real pixels, drag the tool-window splitter wider before capturing (more
native pixels) rather than stretching the crop, and recompute the crop box.

## Gotchas

- The IDE window may open on whichever display/space it last used; the
  `osascript` reposition brings it to a known spot and to the front.
- Coordinates are pinned to window position, window size, and display DPI.
  Change any of those and every click coordinate and the crop box shift.
  This is why the procedure is assisted (read a frame, aim the next click)
  rather than a fixed-coordinate script.
- The full automated analog is JetBrains Remote Robot (`remote-robot` +
  `remote-fixtures` test libraries plus a `robot-server` in a
  `runIdeForUiTests` task), which drives the Swing component tree by
  semantic locators. It is the real equivalent of the VS Code Playwright
  harness but a larger investment; adopt it if these get regenerated often.
