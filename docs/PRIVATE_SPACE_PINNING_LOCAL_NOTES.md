# Local Private Space build notes

## Source and scope

- Upstream PR: [Lawnchair #7115](https://github.com/LawnchairLauncher/lawnchair/pull/7115)
- PR commit: `d6574ff85a95892c0896f34fa1b90ce818e55f69`
- Parent and target base: `505dbc40e6154c05158b5d0271c45f6a885a411b` (`15-beta`)
- The original private-app pinning build used the PR commit without a local fix. The project-link confirmation feature below is a later local source change; no commit, push, backup edit, or launcher database edit was made.
- The PR adds opt-in Private Space app pinning to the launcher, with private items hidden while the profile is locked. The PR changes Lawnchair application code and launcher model handling; it does not patch Android or another app.
- The PR's `15-beta` base was kept intact instead of porting its large patch onto `16-dev`, where its structure did not apply cleanly. The local project-link feature described below builds on that compatible base. Runtime compatibility with this device's GrapheneOS build still requires device testing.

## Build and artifact

Prerequisites: Android SDK at `/home/lh/Android/Sdk`, with Gradle using a workspace-external temporary cache to avoid changing global Gradle configuration.

From this repository directory, reproduce with:

```sh
ANDROID_HOME=/home/lh/Android/Sdk \
GRADLE_USER_HOME=/tmp/lawnchair-gradle-home \
./gradlew assembleLawnWithQuickstepPlayDebug
```

Variant: `lawnWithQuickstepPlayDebug`  
Package: `app.lawnchair.play.debug`  
Version: `15.Dev.(d6574ff)` (`versionCode` 1500020300)  
Minimum SDK: 26  
Target SDK: 35  
Compile SDK: 36

Stable copied APK: `artifacts/lawnchair-private-space-debug.apk`  
Latest clean build size: 42,594,552 bytes  
Latest clean build SHA-256: `d192744cff86cabb3029a2dba4c36d15b5bdea0df45b3a2f0c661f1bda9a5925`

`apksigner verify --print-certs` passed. It is signed with the Android debug certificate, SHA-256 fingerprint:
`29e00e98c2637cabe800a4e2eb6b5acd33449ff302f276cf6c24855af92bf1dd`.

The `.debug` application ID is distinct from the regular Play package, so this APK is packaged for side-by-side testing rather than updating or replacing an existing Lawnchair installation. It is a debug-signed build and should not be treated as a release/update package.

## Feature and test status

Setting path in Lawnchair: **Home screen > Private space > Allow on home screen**. Enable it, unlock Private Space, then add an app through the launcher. The PR describes that names and icons can be visible outside Private Space while enabled; the launcher hides private items while that profile is locked. **Hide app badge** is a separate setting.

The selected build `assembleLawnWithQuickstepPlayDebug` completed successfully. A filtered invocation of `testLawnWithQuickstepPlayDebugUnitTest` also completed successfully, but Gradle reported `NO-SOURCE` for that variant, so it did not execute unit tests. Existing focused test sources include `PrivateSpaceHeaderViewTest`, `PrivateSpaceSettingsButtonTest`, and `TaplPrivateSpaceTest`; their actual execution has not been verified. No Pixel runtime or UI test has been performed.

ChatGPT can expose only shortcuts that its app publishes and Android makes available for that user/profile. The launcher code uses LauncherApps activity and shortcut queries, but there is no evidence in this build that the ChatGPT app publishes a project-specific Private Space shortcut. This does not repair a missing option inside ChatGPT.

Thermometer (`com.google.android.apps.pixel.health`) was not hardcoded or given a fallback activity. The source discovers launcher activities through Android's LauncherApps APIs and filters exact configured components. Package installation state, enabled activity aliases, and the actual resolved activity must be checked on the Pixel before diagnosing its absence.

## Device status and next verification

Read-only `adb devices -l` confirmed Pixel 9 Pro serial `54111FDAP0019Z` (`caiman`), Android 17 / API 37, with user 0 as Owner and user 10 as Private space. The installed stable Lawnchair package is `app.lawnchair`, version `15.Beta 3`, in Owner; the current HOME role remains `com.android.launcher3`.

The debug APK was installed as `app.lawnchair.play.debug` after confirming that package was absent. Android reports the package installed for users 0 and 10, version `15.Dev.(d6574ff)`, and grants `ACCESS_HIDDEN_PROFILES`. A temporary HOME role test gave the debug launcher ShortcutManager host access in owner user 0 (`true`); the earlier check against user 10 was not a valid cross-profile permission conclusion. The exact previous HOME role, `com.android.launcher3`, was restored after each test and verified. No backup or launcher database was read or changed.

Thermometer's actual Owner-profile launcher activity resolves as `com.google.android.apps.pixel.health/.thermometer.ThermometerHomeActivity`. The drawer hierarchy was reached during the temporary HOME test, but the targeted search field could not be reliably focused. Therefore the runtime test does not establish whether Thermometer appears in the Lawnchair drawer. ChatGPT is installed in Private space and exposes its launcher activity plus shortcuts with IDs `Camera`, `Voice`, and `images`; the filtered inspection showed no project-specific shortcut. Shortcut labels, text, and intents were not collected.


## Thermometer follow-up, 2026-10-23

A temporary, package-scoped diagnostic build was used to inspect Lawnchair's own app model. `LauncherApps.getActivityList(null, owner)` and the package-specific query each returned exactly one Thermometer activity: `com.google.android.apps.pixel.health/.thermometer.ThermometerHomeActivity`. The Private Space profile query returned no Thermometer activity, consistent with the package being installed in Owner only. Lawnchair's `AppFilter` allowed the Owner activity and the hidden-app preference did not hide it. The item predicate returned both true and false for separate adapters; the source binds the Owner and work/private-profile adapters to different matchers, so this alone does not prove drawer visibility or explain which screen was searched.

The temporary diagnostic statements have been removed. The clean PR build succeeds and is copied to the stable APK path above. It has not yet replaced the diagnostic package on the Pixel. The debug Lawnchair is currently the selected HOME app. The device was awake but Keyguard reported `showing=true` and `inputRestricted=true`, so no reliable Owner drawer/label/UI check or normal pin action was possible. Wait for the user to unlock the Pixel, then update the existing debug package with `adb -s 54111FDAP0019Z install -r artifacts/lawnchair-private-space-debug.apk` and perform one focused Owner drawer check. No Thermometer-specific code change is supported by the evidence so far; do not add a guessed activity fallback or database shortcut.

## Private project-link home-screen confirmation, 2026-10-23

The earlier statement that this checkout had no locally authored fix described the Thermometer investigation and original private-app pinning work. The source now also contains a focused local feature for project links shared from `com.example.privatespaceprojects` in Private Space. When the profile is unlocked and Lawnchair's private-space home-screen opt-in is enabled, LauncherApps callbacks are treated only as a signal. Lawnchair queries the complete shortcut record, checks its pending marker and exact enabled alias, and opens its existing native add-item confirmation. Confirming inserts that exact alias as a normal app item through the launcher model. The existing Android `PinItemRequest` path is unchanged. Callback IDs are opaque and deduped in bounded launcher-private preferences; URL values are not read or stored by Lawnchair.

The marker contract is version 1, `pending=true`, and ID prefix `shared-project-`. The helper keeps URL values in its own Private Space profile. `LauncherApps.pinShortcuts()` is not used because the installed launcher lacks the strict `ACCESS_SHORTCUTS` permission for the restricted profile.

The `assembleLawnWithQuickstepPlayDebug` build succeeded after correcting for key-fields-only callback records. `git diff --check` passed. The copied artifact is `artifacts/lawnchair-private-share-debug.apk`, SHA-256 `85eb3fc3073fd74dde869092b249831468ee91d91d54659248a18af3a59c06b6`. It retains package `app.lawnchair.play.debug`, version `15.Dev.(d6574ff)`, and the debug signing certificate documented above. No device install, app launch, runtime integration test, or UI test was performed for this feature.

## Project-link home-screen bug fixes, 2026-10-23

Follow-up source changes address the reported duplicate confirmation, blank home icon, and missing drag-and-drop. Callback handling now synchronously claims each opaque share ID before opening confirmation and no longer forces a separate task for repeated callbacks. The add button and drag conversion populate the exact private alias icon through IconCache. Dragging uses Lawnchair's cross-window drag listener; on drop it revalidates the marker, profile, opt-in, and alias, then returns a regular `ITEM_TYPE_APPLICATION` workspace item. Invalid drops refuse the shortcut-config fallback so a `MAIN`-only alias cannot launch during the drop.

Build and `git diff --check` passed. Updated artifact SHA-256: `4bcbcdf07d964ab9a004c9f61ce21f242d982bc89c503d63b937a662753163e8`; package, version, and debug signing certificate are unchanged. These fixes have not been installed or runtime-tested on the device.

## Project-link title confirmation follow-up, 2026-10-23

The existing native add-item confirmation now presents an editable project-name field. A share marker with `app.lawnchair.private_share.title_required=true` opens with an empty required field; otherwise the published short label pre-fills it. Blank names block both Add and drag. Confirm and successful drag/drop apply the entered label to the regular app item and save Lawnchair's existing custom-app-name preference under the exact private alias and profile, then refresh the launcher model. Cancel does not write the preference. The share helper retains ownership of title extraction and its private URL/title storage.

`assembleLawnWithQuickstepPlayDebug` succeeded and `git diff --check` passed. Current APK: `artifacts/lawnchair-private-share-debug.apk`, SHA-256 `9eebf2d4b9ffd54dc46de766b6d42a29ad93fd97ffa0f4afc7054229190ced2a`. `apksigner verify --print-certs` passed with the existing debug certificate SHA-256 `29e00e98c2637cabe800a4e2eb6b5acd33449ff302f276cf6c24855af92bf1dd`. This title follow-up has not been installed or runtime-tested on the device.
