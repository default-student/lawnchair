# Private Space Projects test helper

This small Android test app receives a ChatGPT project URL through Android's
share sheet, asks for a name, stores the URL in the app-private storage of the
profile where it was installed, and publishes a Lawnchair-compatible dynamic
shortcut. The shortcut points at a dedicated activity alias. Tapping that alias
in Lawnchair reads the URL in the Private Space app process and sends an
`ACTION_VIEW` intent scoped to `com.openai.chatgpt`.

The launcher marker contract is documented in
[`../docs/PRIVATE_SPACE_PINNING_LOCAL_NOTES.md`](../docs/PRIVATE_SPACE_PINNING_LOCAL_NOTES.md).
Lawnchair receives the title and opaque shortcut ID; the URL remains in this
helper's profile-private preferences. Android and GrapheneOS profile behavior,
ChatGPT's handling of the supplied project URL, and the full LauncherApps path
still require the manual Pixel test below.

## Build

The source is Java-only and the offline build uses the Android SDK directly.
Set `ANDROID_HOME`, `ANDROID_DEBUG_KEYSTORE`, `ANDROID_DEBUG_KEYSTORE_PASSWORD`,
and `ANDROID_DEBUG_KEY_PASSWORD` in the environment. The signing key is not
stored in this project. Optional SDK overrides are `ANDROID_PLATFORM` and
`ANDROID_BUILD_TOOLS`.

```sh
./build-local.sh
```

The output is `artifacts/private-space-project-helper-test.apk`. This is a
debug-signed, sideload test APK for Android 15 and later, not a release build.

## Manual test

This integration requires a Lawnchair build containing the Private Space
project-link confirmation support described in
[`../docs/PRIVATE_SPACE_PROJECT_LINKS.md`](../docs/PRIVATE_SPACE_PROJECT_LINKS.md).
Use a test profile or device, install the helper in Private Space, and make the
compatible Lawnchair build the active Home app only for the test. A debug
build has a separate application ID from the stable release and keeps its own
home-screen layout and pin data.

1. Install the helper in Private Space. If using Android's **Install
   available apps** flow, the same package may first need to be installed in
   Owner; each profile keeps separate app data. Do not share project URLs to
   the Owner copy.
2. Make the compatible Lawnchair build the active Home app, enable **Home screen >
   Private space > Allow on home screen**, and leave Private Space unlocked.
3. In ChatGPT inside Private Space, open the intended project and use its share
   action. Choose **Private Space Projects**, enter a recognizable name, and
   save. In Lawnchair, add the new private project item from its confirmation
   card. Tap it and check that ChatGPT opens the exact intended project in
   Private Space.
4. After that unlocked test, lock Private Space. Check that Lawnchair keeps
   the icon dimmed while locked, and that tapping it requests the normal
   unlock. After unlocking, check that the same project opens.

If the intent cannot be resolved, the helper reports that ChatGPT could not
open the link in this profile. The helper never falls back to a browser or an
Owner-profile app. Remove a saved item from the helper to unpublish its dynamic
shortcut and disable its activity alias. Delete the Lawnchair item first;
existing icons stop working when their alias is disabled.

The adaptive app icon and form colors use the Android system accent palette.
The foreground is an original loop/link mark for this test build, not a copy of
the ChatGPT logo.
