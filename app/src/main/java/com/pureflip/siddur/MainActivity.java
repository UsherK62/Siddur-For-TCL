package com.pureflip.siddur;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/** Launcher trampoline: shows the splash colour, then opens Setup on first run or Home after. */
public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Prefs prefs = new Prefs(this);
        startActivity(new Intent(this, prefs.setupDone() ? HomeActivity.class : SetupActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
