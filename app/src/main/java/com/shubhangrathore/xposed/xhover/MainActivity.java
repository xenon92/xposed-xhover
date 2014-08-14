package com.shubhangrathore.xposed.xhover;

import android.app.ActionBar;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuItem;

import com.readystatesoftware.systembartint.SystemBarTintManager;


public class MainActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener {

    private static final String PREF_LONG_FADE_OUT_DELAY = "long_fade_out_delay";
    private static final String PREF_SHORT_FADE_OUT_DELAY = "short_fade_out_delay";
    private static final String PREF_MICRO_FADE_OUT_DELAY = "micro_fade_out_delay";

    private Preference mLongFadeOutDelay;
    private Preference mShortFadeOutDelay;
    private Preference mMicroFadeOutDelay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        // Set status bar tinted color
        SystemBarTintManager tintManager = new SystemBarTintManager(this);
        tintManager.setStatusBarTintEnabled(true);
        tintManager.setNavigationBarTintEnabled(true);
        tintManager.setStatusBarTintColor(getResources().getColor(R.color.material_indigo_dark));

        // Set action bar color
        ActionBar mActionBar = getActionBar();
        mActionBar.setBackgroundDrawable(new ColorDrawable(getResources()
                .getColor(R.color.material_indigo_dark)));


        // ########################################################## //


        mMicroFadeOutDelay = findPreference(PREF_MICRO_FADE_OUT_DELAY);
        mMicroFadeOutDelay.setOnPreferenceChangeListener(this);

        mShortFadeOutDelay = findPreference(PREF_SHORT_FADE_OUT_DELAY);
        mShortFadeOutDelay.setOnPreferenceChangeListener(this);

        mLongFadeOutDelay = findPreference(PREF_LONG_FADE_OUT_DELAY);
        mLongFadeOutDelay.setOnPreferenceChangeListener(this);
    }


    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mLongFadeOutDelay
                || preference == mShortFadeOutDelay
                || preference == mMicroFadeOutDelay) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onPreferenceTreeClick (PreferenceScreen preferenceScreen, Preference preference) {
        return false;
    }

    /**
     * Confirms the action from user before resetting all the
     * values to stock values as defined by ParanoidAndroid.
     *
     * @return boolean true if user confirms reset, else false
     */
    private boolean resetConfirmation() {
        return false;
    }
}
