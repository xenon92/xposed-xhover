/*
 * xHover
 *
 * Xposed module to customize Paranoid Android's Hover notification experience
 *
 * Copyright (c) 2014 Shubhang Rathore
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shubhangrathore.xposed.xhover;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.readystatesoftware.systembartint.SystemBarTintManager;

import java.io.DataOutputStream;
import java.io.IOException;


public class MainActivity extends PreferenceActivity {

    private static final String TAG = "xHover";

    public static final String PREF_MICRO_FADE_OUT_DELAY = "micro_fade_out_delay";
    public static final String PREF_LONG_FADE_OUT_DELAY = "long_fade_out_delay";
    public static final String PREF_SHORT_FADE_OUT_DELAY = "short_fade_out_delay";
    public static final String PREF_LOCKSCREEN_BEHAVIOR = "lockscreen_behavior";
    public static final String PREF_HIDE_NON_CLEARABLE = "hide_non_clearable";
    public static final String PREF_HIDE_LOW_PRIORITY = "hide_low_priority";
    private static final String PREF_ABOUT = "about_preference";
    private static final String PREF_APPLY = "apply_preference";
    private static final String PREF_CHANGELOG = "changelog_preference";
    private static final String PREF_DEVELOPER = "developer_preference";
    private static final String PREF_RESET_ALL = "reset_all";
    private static final String PREF_SOURCE_CODE = "app_source_preference";
    private static final String PREF_TEST_NOTIFICATION = "test_notification";
    private static final String PREF_VERSION = "app_version_name";

    private static final String ABOUT_XHOVER_BLOG_LINK = "http://blog.shubhangrathore.com/xhover/";
    private static final String CHANGELOG_LINK = "https://github.com/xenon92/xposed-xhover/blob/master/CHANGELOG.md";
    private static final String DEVELOPER_WEBSITE_LINK = "http://shubhangrathore.com";
    private static final String SOURCE_CODE_LINK = "https://www.github.com/xenon92/xposed-xhover";

    public static String sVersionName;

    private ListPreference mLongFadeOutDelay;             // Natural timeout preference
    private ListPreference mShortFadeOutDelay;            // Notification waiting preference
    private ListPreference mMicroFadeOutDelay;            // Evade notification preference
    private ListPreference mLockscreenBehavior;
    private CheckBoxPreference mHideNonClearable;
    private CheckBoxPreference mHideLowPriority;
    private Preference mAbout;
    private Preference mApply;
    private Preference mChangelog;
    private Preference mDeveloper;
    private Preference mResetAll;
    private Preference mSourceCode;
    private Preference mTestNotification;
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
        if (mActionBar != null) {
            mActionBar.setBackgroundDrawable(new ColorDrawable(getResources()
                    .getColor(R.color.material_indigo_dark)));
        }


        // ########################################################## //


        mHideNonClearable = (CheckBoxPreference) findPreference(PREF_HIDE_NON_CLEARABLE);
        mHideLowPriority = (CheckBoxPreference) findPreference(PREF_HIDE_LOW_PRIORITY);
        mMicroFadeOutDelay = (ListPreference) findPreference(PREF_MICRO_FADE_OUT_DELAY);
        mShortFadeOutDelay = (ListPreference) findPreference(PREF_SHORT_FADE_OUT_DELAY);
        mLongFadeOutDelay = (ListPreference) findPreference(PREF_LONG_FADE_OUT_DELAY);
        mLockscreenBehavior = (ListPreference) findPreference(PREF_LOCKSCREEN_BEHAVIOR);

        mVersion = findPreference(PREF_VERSION);
        setVersionNameInGui();

        mAbout = findPreference(PREF_ABOUT);
        mApply = findPreference(PREF_APPLY);
        mChangelog = findPreference(PREF_CHANGELOG);
        mDeveloper = findPreference(PREF_DEVELOPER);
        mResetAll = findPreference(PREF_RESET_ALL);
        mSourceCode = findPreference(PREF_SOURCE_CODE);
        mTestNotification = findPreference(PREF_TEST_NOTIFICATION);
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
        } else if (preference == mApply) {
            restartSystemUi();
        } else if (preference == mTestNotification) {
            makeStatusBarNotification();
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
        mLockscreenBehavior.setValue("1");
        mHideNonClearable.setChecked(false);
        mHideLowPriority.setChecked(false);
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

    /**
     * Restarts SystemUI. Requires SuperUser privilege.
     * @return boolean true if successful, else false.
     */
    private boolean restartSystemUi() {
        boolean mSuccessful;
        try {
            Process mProcess = Runtime.getRuntime().exec("su");
            DataOutputStream mDataOutputStream = new DataOutputStream(mProcess.getOutputStream());
            mDataOutputStream.writeBytes("pkill com.android.systemui \n");
            mDataOutputStream.writeBytes("exit\n");
            mDataOutputStream.flush();
            // We wait for the command to be completed
            // before moving forward. This ensures that the method
            // returns only after all the commands' execution is complete.
            mProcess.waitFor();

            if (mProcess.exitValue() == 1) {
                // If control is here, that means the sub process has returned
                // an unsuccessful exit code.
                // Most probably, SuperUser permission was denied
                Log.e(TAG, "Utilities: SuperUser permission denied. Unable to restart SystemUI.");
                mSuccessful = false;
            } else {
                // SuperUser permission granted
                Log.i(TAG, "Utilities: SuperUser permission granted. SystemUI restarted.");
                mSuccessful = true;
            }
        } catch (IOException e) {
            Log.e(TAG, "restartSystemUI: I/O exception");
            mSuccessful = false;
        } catch (InterruptedException e) {
            Log.e(TAG, "restartSystemUI: InterruptedException exception");
            mSuccessful = false;
        }
        return mSuccessful;
    }

    /**
     * Posts a Test notification to status bar to be displayed through Hover.
     */
    private void makeStatusBarNotification(){
        final NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_hover_pressed_notification)
                        .setAutoCancel(true)
                        .setContentTitle(getString(R.string.notification_xhover))
                        .setContentText(getString(R.string.notification_text))
                        .setTicker(getString(R.string.notification_text));

        final NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        final int mNotificationDelay = 3000;          // 3 seconds
        Handler mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            public void run() {
                mNotificationManager.notify(0, mBuilder.build());
            }}, mNotificationDelay);
    }
}
