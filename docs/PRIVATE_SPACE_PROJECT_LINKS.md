# ChatGPT project links on the home screen

This feature connects ChatGPT's Android share action to a home-screen shortcut
inside Private Space. It consists of a small companion app and a Lawnchair
change. It does not alter Android, GrapheneOS, or ChatGPT.

## What the companion app does

The companion app receives a shared ChatGPT project URL, asks for a display
name, and stores both in app-private preferences for the Android profile where
the app is installed. It publishes a dynamic shortcut with a dedicated
activity alias. When the alias is launched, the helper reads its URL in that
profile and opens it with an `ACTION_VIEW` intent explicitly scoped to
`com.openai.chatgpt`. It does not fall back to a browser or another profile.

The URL is not included in the shortcut label, shortcut ID, or Lawnchair
database. Lawnchair receives only an opaque shortcut ID, a title, and marker
metadata. Treat the URL as a private link: anyone who obtains it may be able to
open the shared project according to ChatGPT's sharing behavior. Keep it out
of source files, issue reports, screenshots, and build logs.

## What changed in Lawnchair

- Added a private-space setting at **Home screen > Private space > Allow on
  home screen**. Private shortcuts can remain on the home screen while the
  profile is locked; Android controls the unlock flow when one is tapped.
- Added handling for the helper's marked project shortcuts. A LauncherApps
  callback is treated only as a signal; Lawnchair re-queries the full shortcut
  record and validates the marker, profile, and enabled activity alias.
- Reused Lawnchair's native add-item confirmation. The user can enter or edit
  the project name before adding it. Confirming creates a normal app item for
  the exact private activity alias through the launcher model, using the
  selected target app's shortcut icon. Existing home items keep their current
  icon until removed and added again.
- Added drag-and-drop support for the same confirmation flow. The marker and
  alias are checked again at drop time; invalid drops are rejected.
- Deduplicated repeated callbacks using bounded launcher-private preferences.
  Lawnchair does not read or persist the project URL. Android's existing
  `PinItemRequest` flow remains unchanged.

This keeps URL storage and title extraction in the helper's profile-private
app data. The launcher only uses the shortcut metadata needed to show and add
the item.

## Build and test

Build the helper with Android SDK tools installed and credentials supplied
through environment variables. Do not commit signing keys or passwords:

```sh
ANDROID_HOME=/path/to/android-sdk \
ANDROID_DEBUG_KEYSTORE=/path/to/debug.keystore \
ANDROID_DEBUG_KEYSTORE_PASSWORD=... \
ANDROID_DEBUG_KEY_PASSWORD=... \
./build-local.sh
```

The helper is a sideload test app, not a release build. Build and install a
Lawnchair variant that includes the companion shortcut integration. In a
dedicated test profile, share a project link to the helper, add the proposed
item in Lawnchair, and verify it opens the intended project in the same
Private Space profile. Repeat while Private Space is locked and confirm the
normal Android unlock prompt appears. Runtime behavior depends on Android,
GrapheneOS, Lawnchair, and ChatGPT versions; verify it on the target device.

## Public repository contents

Keep device backups, launcher databases, exported settings, APKs signed with
local keys, real project URLs, and device identifiers out of this repository.
The source code and this generic integration guide are sufficient to explain
and reproduce the feature. Store signing credentials only in the local
environment or the CI secret store, never in source or workflow text.
