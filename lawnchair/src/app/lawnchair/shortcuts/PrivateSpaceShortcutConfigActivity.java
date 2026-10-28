package app.lawnchair.shortcuts;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Bundle;

import java.util.UUID;

import app.lawnchair.LawnchairLauncher;
import com.android.launcher3.R;

/** Supplies a standard, persistent home screen shortcut for Private Space. */
public class PrivateSpaceShortcutConfigActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
        if (shortcutManager == null) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        Intent launchIntent = new Intent(PrivateSpaceShortcut.ACTION_TOGGLE)
                .setClass(this, LawnchairLauncher.class);
        ShortcutInfo shortcut = new ShortcutInfo.Builder(this, UUID.randomUUID().toString())
                .setShortLabel(getString(R.string.private_space_shortcut))
                .setIcon(Icon.createWithResource(this, R.drawable.ic_private_space_with_background))
                .setIntent(launchIntent)
                .build();
        Intent result = shortcutManager.createShortcutResultIntent(shortcut);
        setResult(result == null ? RESULT_CANCELED : RESULT_OK, result);
        finish();
    }
}
