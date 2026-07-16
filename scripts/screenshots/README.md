# Marketplace screenshot procedure

How to regenerate the JetBrains Marketplace screenshots for the NightVision
IntelliJ plugin. Unlike the VS Code extension's Playwright harness, this
plugin is a native Swing tool window with no DOM, so capture is a macOS
screen-automation procedure (assisted, not a one-command script) followed by
a deterministic crop step (`crop.py`).

The screenshots are uploaded by hand into the Marketplace vendor console
(plugin edit page, "Plugin Screenshots"). JetBrains hosts them; they are not
hotlinked from this repo, so there is no URL-permanence rule and nothing here
gets bundled into the plugin `.zip` (`buildPlugin` ships only the jar).

## When to regenerate

After any change to the tool-window UI (Overview, API Discovery, the detail
screens, the scan/target/authentication/project lists). Screenshots drift
silently; the listing keeps showing the old UI until someone re-shoots.

## Shot inventory

Eight panel-only crops, one uniform crop box so they share an aspect ratio
(JetBrains rejects mixed aspect ratios in a single listing):

1. `01-overview` -- the two top-level actions
2. `02-api-discovery` -- discovery form with the project path pre-filled
3. `03-api-discovery-languages` -- API Language dropdown open (all languages)
4. `04-security-testing-menu` -- Scans / Targets / Authentications / Projects
5. `05-scans-list` -- scan list with vulnerability-count badges
6. `06-scan-details` -- restyled stacked detail layout
7. `07-targets-list` -- target list (OpenAPI + Web)
8. `08-target-details` -- the richest detail screen (base URL, spec, status)

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
   screencapture -x -R "56,54,1400,908" 01-overview.png
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

The default box `(1799, 171, 2730, 1490)` is calibrated to the window
geometry above. If you capture at a different window position, size, or
display scale, recompute it: crop `08-target-details.png` (the tallest
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
