package com.shubhangrathore.xposed.xhover;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

import com.readystatesoftware.systembartint.SystemBarTintManager;


public class MainActivity extends PreferenceActivity {

    private static final String TAG = "xHover";

    public static final String PREF_MICRO_FADE_OUT_DELAY = "micro_fade_out_delay";
    public static final String PREF_LONG_FADE_OUT_DELAY = "long_fade_out_delay";
    public static final String PREF_SHORT_FADE_OUT_DELAY = "short_fade_out_delay";
    private static final String PREF_ABOUT = "about_preference";
    private static final String PREF_CHANGELOG = "changelog_preference";
    private static final String PREF_DEVELOPER = "developer_preference";
    private static final String PREF_RESET_ALL = "reset_all";
    private static final String PREF_SOURCE_CODE = "app_source_preference";
    private static final String PREF_VERSION = "app_version_name";

    private static final String ABOUT_XHOVER_BLOG_LINK = "http://blog.shubhangrathore.com/xhover/";
    private static final String CHANGELOG_LINK = "https://github.com/xenon92/xposed-xhover/blob/master/CHANGELOG.md";
    private static final String DEVELOPER_WEBSITE_LINK = "http://shubhangrathore.com";
    private static final String SOURCE_CODE_LINK = "https://www.github.com/xenon92/xposed-xhover";

    public static String sVersionName;

    private ListPreference mLongFadeOutDelay;             // Natural timeout preference
    private ListPreference mShortFadeOutDelay;            // Notification waiting preference
    private ListPreference mMicroFadeOutDelay;            // Evade notification preference
    private Preference mResetAll;
    private Preference mVersion;
    private Preference mAbout;
    private Preference mChangelog;
    private Preference mDeveloper;
    private Preference mSourceCode;

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
        if (mActionBar != null) {
            mActionBar.setBackgroundDrawable(new ColorDrawable(getResources()
                    .getColor(R.color.material_indigo_dark)));
        }


        // ########################################################## //


        mMicroFadeOutDelay = (ListPreference) findPreference(PREF_MICRO_FADE_OUT_DELAY);
        mShortFadeOutDelay = (ListPreference) findPreference(PREF_SHORT_FADE_OUT_DELAY);
        mLongFadeOutDelay = (ListPreference) findPreference(PREF_LONG_FADE_OUT_DELAY);

        mVersion = findPreference(PREF_VERSION);
        setVersionNameInGui();

        mResetAll = findPreference(PREF_RESET_ALL);
        mAbout = findPreference(PREF_ABOUT);
        mChangelog = findPreference(PREF_CHANGELOG);
        mDeveloper = findPreference(PREF_DEVELOPER);
        mSourceCode = findPreference(PREF_SOURCE_CODE);
    }


    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        if (preference == mResetAll) {
            resetConfirmation();
        } else if (preference == mAbout) {
            openLink(ABOUT_XHOVER_BLOG_LINK);
        } else if (preference == mDeveloper) {
            openLink(DEVELOPER_WEBSITE_LINK);
        } else if (preference == mChangelog) {
            openLink(CHANGELOG_LINK);
        } else if (preference == mSourceCode) {
            openLink(SOURCE_CODE_LINK);
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
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Intentionally left blank
                    }
                })
                .show();
    }

    /**
     * Reset all settings to the default values as provided by Paranoid Android
     */
    private void resetAllPreferences() {
        Log.i(TAG, "Reset all preferences to stock Paranoid Android values");
        mLongFadeOutDelay.setValue("5000");
        mShortFadeOutDelay.setValue("2500");
        mMicroFadeOutDelay.setValue("1250");
        Toast.makeText(getApplicationContext(),
                getString(R.string.stock_values_restored), Toast.LENGTH_SHORT).show();
    }

    /**
     * Sets the app version name of xHover in the GUI in "Version" preference summary
     */
    private void setVersionNameInGui() {
        try {
            sVersionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            mVersion.setSummary(sVersionName);
            Log.i(TAG, "xHover version: " + sVersionName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Unable to get xHover package versionName");
        }
    }

    /**
     * Open web links by parsing the URI of the parameter link
     *
     * @param link the link to be parsed to open
     */
    private void openLink(String link) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse(link));
        startActivity(browserIntent);
        Log.i(TAG, "Opening link = " + link);
    }
}
