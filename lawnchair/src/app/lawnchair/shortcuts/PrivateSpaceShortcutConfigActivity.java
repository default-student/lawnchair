package app.lawnchair.shortcuts;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import app.lawnchair.LawnchairLauncher;
import com.android.launcher3.R;

/** Supplies a standard, persistent home screen shortcut for Private Space. */
public class PrivateSpaceShortcutConfigActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent launchIntent = new Intent(PrivateSpaceShortcut.ACTION_TOGGLE)
                .setClass(this, LawnchairLauncher.class);
        Intent result = new Intent()
                .putExtra(Intent.EXTRA_SHORTCUT_INTENT, launchIntent)
                .putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.private_space_shortcut));
        result.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(this,
                        R.drawable.ic_private_space_with_background));
        setResult(RESULT_OK, result);
        finish();
    }
}
