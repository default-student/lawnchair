# Private Space shortcuts on the home screen

This build keeps pinned Private Space apps on Lawnchair's home screen while their profile is locked, when **Allow on home screen** is enabled. Their names and icons remain visible. Locked icons use Lawnchair's disabled appearance; tapping one requests the normal Android Private Space unlock. The app drawer and search continue to hide locked-profile apps.

Enable in Lawnchair: **Settings > Home screen > Private space > Allow on home screen**. Pin an app from the unlocked Private Space, then lock the profile. Verify that the shortcut remains visible and dimmed. Tap it and confirm Android asks to unlock Private Space before the app opens. Also verify that the app does not appear in drawer/search while locked, then unlock and confirm the shortcut launches normally.

APK: `artifacts/lawnchair-private-space-home-visible-debug.apk`  
Package: `app.lawnchair.play.debug`  
Version: `15.Dev.(d6574ff)`  
SHA-256: `6eb241ca8c87804066806626633112ee6e52e3809a8f49b3bd569e71a15e8b83`  
Signing certificate SHA-256: `29e00e98c2637cabe800a4e2eb6b5acd33449ff302f276cf6c24855af92bf1dd`

No Pixel install or runtime interaction was performed for this change. The debug build is signed with the same certificate as the previous Lawnchair debug package, so it can update that package if installed manually.
