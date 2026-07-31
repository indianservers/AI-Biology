# Smart TV Support

The mobile and television experiences ship from the same APK. Runtime device detection
uses Android television UI mode or the Leanback hardware feature.

## Television Behavior

- The app is discoverable through `LEANBACK_LAUNCHER`.
- Touchscreen hardware is optional.
- Television launches are locked to landscape and keep the display awake.
- The 3D Explorer uses a wide two-pane layout with anatomy parts on the left and the
  model stage on the right.
- When a model has no authored parts, the stage expands instead of leaving an empty
  anatomy column.
- D-pad focus has a high-contrast outline and scale response.
- Pressing OK on the model stage enters full screen. In full screen, D-pad left/right
  rotates, up/down zooms, and OK exits.
- In normal mode, D-pad directions navigate between controls without being trapped by
  the WebView.
- Model and infographic libraries use five-column grids on television.
- The model library opens fully expanded because a draggable bottom sheet is not a
  remote-control interaction.

## Debug Television Preview

Debug builds can render the exact TV branch on a phone or tablet emulator:

```powershell
adb shell wm size 1920x1080
adb shell wm density 240
adb shell am start -n com.indianservers.AIbiology/.MainActivity `
  --ez force_tv_layout true
```

Restore the emulator afterward:

```powershell
adb shell wm size reset
adb shell wm density reset
adb shell settings put system accelerometer_rotation 1
```

The `force_tv_layout` extra is ignored by release builds.

## Release Checklist

- Test on Google TV and Fire TV hardware in addition to an emulator.
- Verify five-way navigation from every focused control.
- Verify Back exits dialogs, full-screen media, and the app in that order.
- Confirm downloaded model memory usage on a low-RAM television.
- Validate launcher banner rendering at normal and accessibility display scales.
- Run Android TV quality checks before enabling TV distribution in Play Console.
