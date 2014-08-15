package com.shubhangrathore.xposed.xhover;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.widget.Toast;

import com.readystatesoftware.systembartint.SystemBarTintManager;


public class MainActivity extends PreferenceActivity {

    public static final String PREF_MICRO_FADE_OUT_DELAY = "micro_fade_out_delay";
    public static final String PREF_LONG_FADE_OUT_DELAY = "long_fade_out_delay";
    public static final String PREF_SHORT_FADE_OUT_DELAY = "short_fade_out_delay";
    private static final String PREF_RESET_ALL = "reset_all";
    private static final String PREF_VERSION = "app_version_name";

    public static String sVersionName;

    private ListPreference mLongFadeOutDelay;             // Natural timeout preference
    private ListPreference mShortFadeOutDelay;            // Notification waiting preference
    private ListPreference mMicroFadeOutDelay;            // Evade notification preference
    private Preference mResetAll;
    private Preference mVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
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


        mMicroFadeOutDelay = (ListPreference) findPreference(PREF_MICRO_FADE_OUT_DELAY);

        mShortFadeOutDelay = (ListPreference) findPreference(PREF_SHORT_FADE_OUT_DELAY);

        mLongFadeOutDelay = (ListPreference) findPreference(PREF_LONG_FADE_OUT_DELAY);

        mVersion = findPreference(PREF_VERSION);
        setVersionNameInGui();

        mResetAll = findPreference(PREF_RESET_ALL);
    }


    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        if (preference == mResetAll) {
            resetConfirmation();
        }
        return false;
    }

    /**
     * Confirms the action from user before resetting all the
     * values to stock values as defined by Paranoid Android.
     */
    private void resetConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.reset_all))
                .setMessage(getString(R.string.reset_all_confirmation_message))
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        resetAllPreferences();
                        Toast.makeText(getApplicationContext(), getString(R.string.stock_values_restored), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Intentionally left blank
                    }
                })
                .show();
    }

    private void resetAllPreferences() {
        mLongFadeOutDelay.setValue("5000");
        mShortFadeOutDelay.setValue("2500");
        mMicroFadeOutDelay.setValue("1250");
    }

    private void setVersionNameInGui() {
        try {
            sVersionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            mVersion.setSummary(sVersionName);
        } catch (PackageManager.NameNotFoundException e) {
            // TODO: add logging
        }
    }
}
