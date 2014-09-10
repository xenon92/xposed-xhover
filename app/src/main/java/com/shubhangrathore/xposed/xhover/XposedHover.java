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

import android.app.KeyguardManager;
import android.app.Notification;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by Shubhang on 14/8/2014.
 */
public class XposedHover implements IXposedHookLoadPackage {

    private static final String TAG = "XposedHover";

    private static final String CLASS_HOVER = "com.android.systemui.statusbar.notification.Hover";
    private static final String CLASS_NOTIFICATION_DATA_ENTRY = "com.android.systemui.statusbar.NotificationData$Entry";
    private static final String CLASS_NOTIFICATION_HELPER = "com.android.systemui.statusbar.notification.NotificationHelper";
    private static final String CLASS_SIZE_ADAPTIVE_LAYOUT = "com.android.internal.widget.SizeAdaptiveLayout";
    private static final String PACKAGE_XHOVER = XposedHover.class.getPackage().getName();
    private static final String PACKAGE_SYSTEM_UI = "com.android.systemui";

    private static final String FONT_FAMILY_DEFAULT = "sans-serif";
    private static final String FONT_FAMILY_LIGHT = "sans-serif-light";
    private static final String FONT_FAMILY_CONDENSED = "sans-serif-condensed";

    private static String sCallingMethod = "";
    private static boolean sExpanded;
    private static String sCurrentNotificationPackageName;

    private int mMicroFadeOutDelay;            // Evade notification time delay
    private int mShortFadeOutDelay;            // Notification waiting time delay
    private int mLongFadeOutDelay;             // Natural timeout delay
    private int mLockscreenBehavior;
    private boolean mHideNonClearable;
    private boolean mHideLowPriority;
    private String mBackgroundColor;
    private String mTitleColor;
    private String mTextColor;
    private String mImageColor;
    private int mImageBackgroundTransparency;

    private KeyguardManager mKeyguardManager;
    private Context mContext;
    private StatusBarNotification mStatusBarNotification;

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!loadPackageParam.packageName.equals(PACKAGE_SYSTEM_UI)) {
            return;
        }

        XSharedPreferences mXSharedPreferences = new XSharedPreferences(PACKAGE_XHOVER);

        mMicroFadeOutDelay =
                Integer.parseInt(mXSharedPreferences
                        .getString(MainActivity.PREF_MICRO_FADE_OUT_DELAY, "1250"));

        mShortFadeOutDelay =
                Integer.parseInt(mXSharedPreferences
                        .getString(MainActivity.PREF_SHORT_FADE_OUT_DELAY, "2500"));

        mLongFadeOutDelay =
                Integer.parseInt(mXSharedPreferences
                        .getString(MainActivity.PREF_LONG_FADE_OUT_DELAY, "5000"));

        mLockscreenBehavior =
                Integer.parseInt(mXSharedPreferences
                        .getString(MainActivity.PREF_LOCKSCREEN_BEHAVIOR, "1"));

        mHideNonClearable = mXSharedPreferences.getBoolean(MainActivity.PREF_HIDE_NON_CLEARABLE, false);
        mHideLowPriority = mXSharedPreferences.getBoolean(MainActivity.PREF_HIDE_LOW_PRIORITY, false);

        mBackgroundColor = ColorPickerPreference.convertToARGB(mXSharedPreferences
                .getInt(MainActivity.PREF_NOTIFICATION_BACKGROUND_COLOR_PICKER, 1));

        mTitleColor = ColorPickerPreference.convertToARGB(mXSharedPreferences
                .getInt(MainActivity.PREF_NOTIFICATION_TITLE_COLOR_PICKER, 1));

        mTextColor = ColorPickerPreference.convertToARGB(mXSharedPreferences
                .getInt(MainActivity.PREF_NOTIFICATION_TEXT_COLOR_PICKER, 1));

        //TODO: Convert transparency into SeekBar sliders
        mImageBackgroundTransparency =
                Integer.parseInt(mXSharedPreferences
                        .getString(MainActivity.PREF_NOTIFICATION_IMAGE_BACKGROUND_TRANSPARENCY, "175"));

        final Class<?> mHoverClass = XposedHelpers.findClass(CLASS_HOVER, loadPackageParam.classLoader);
        final Class<?> mNotificationDataEntryClass = XposedHelpers.findClass(CLASS_NOTIFICATION_DATA_ENTRY, loadPackageParam.classLoader);
        final Class<?> mNotificationHelperClass = XposedHelpers.findClass(CLASS_NOTIFICATION_HELPER, loadPackageParam.classLoader);
        final Class<?> mSizeAdaptiveLayoutClass = XposedHelpers.findClass(CLASS_SIZE_ADAPTIVE_LAYOUT, loadPackageParam.classLoader);


        // Hooking method startMicroHideCountdown() that calls method startHideCountdown(int)
        // providing the later method with the time delay for which Hover notification
        // will stay on screen after a touch is detected outside the notification view.
        // This is the 'Evade notification' feature.
        XposedHelpers.findAndHookMethod(mHoverClass, "startMicroHideCountdown", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                // Setting sCallingMethod flag that will be used before executing
                // startHideCountdown(int) method to determine the method name
                // that called it. This helps overriding the 'int' time delay parameter
                // according to the method that called startHideCountdown(int).
                sCallingMethod = "startMicroHideCountdown";
                Log.d(TAG, "Calling method: " + sCallingMethod);
            }

            @Override
            protected void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                // Resetting the flag after the countdown methods have executed
                // completely so as to prevent overriding wrong sCallingMethod detection
                Log.d(TAG, "Calling method reset after: " + sCallingMethod);
                sCallingMethod = "";
            }
        });


        /*
        // startShortHideCountdown method is an unused method in Hover.java
        //
        // Hooking method startShortHideCountdown() that calls method startHideCountdown(int)
        // providing the later method with the time delay for which Hover notification
        // will stay on screen if another notification is waiting to be shown
        // This is the 'Notification waiting' time delay.
        XposedHelpers.findAndHookMethod(mHoverClass, "startShortHideCountdown", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                sCallingMethod = "startShortHideCountdown";
                Log.d(TAG, "Calling method: " + sCallingMethod);
            }

            @Override
            protected void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                Log.d(TAG, "Calling method reset after: " + sCallingMethod);
                sCallingMethod = "";
            }
        });
        */


        // Hooking method startLongHideCountdown() that calls method startHideCountdown(int)
        // providing the later method with the time delay for which Hover notification
        // will stay on screen if there is no interaction with the device
        // This is the 'Natural timeout' delay.
        XposedHelpers.findAndHookMethod(mHoverClass, "startLongHideCountdown", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                sCallingMethod = "startLongHideCountdown";
                Log.d(TAG, "Calling method: " + sCallingMethod);
            }

            @Override
            protected void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                Log.d(TAG, "Calling method reset after: " + sCallingMethod);
                sCallingMethod = "";
            }
        });


        // Hooking to method startHideCountdown(int) that starts
        // a countdown timer taking an int argument as the time duration
        // of the countdown.
        XposedHelpers.findAndHookMethod(mHoverClass, "startHideCountdown", int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                if (sCallingMethod.equals("startMicroHideCountdown")) {
                    // Detected that startMicroHideCountdown() method called
                    // startHideCountdown(int) method.
                    // Overriding 'Evade notification' values here.
                    Log.d(TAG, "Setting startHideCountdown delay to: "
                            + String.valueOf(mMicroFadeOutDelay));

                    methodHookParam.args[0] = mMicroFadeOutDelay;

                /*
                // startShortHideCountdown method is an unused method in Hover.java

                } else if (sCallingMethod.equals("startShortHideCountdown")) {

                    // Detected that startShortHideCountdown() method called
                    // startHideCountdown(int) method.
                    // Overriding 'Notification waiting' values here.
                    Log.d(TAG, "Setting startHideCountdown delay to: "
                            + String.valueOf(mShortFadeOutDelay));

                    methodHookParam.args[0] = mShortFadeOutDelay;
                */

                } else if (sCallingMethod.equals("startLongHideCountdown")) {
                    // Detected that startLongHideCountdown() method called
                    // startHideCountdown(int) method.
                    // Overriding 'Natural timeout' values here.
                    Log.d(TAG, "Setting startHideCountdown delay to: "
                            + String.valueOf(mLongFadeOutDelay));

                    methodHookParam.args[0] = mLongFadeOutDelay;
                }
            }
        });


        // Hooking to method processOverridingQueue(boolean) that processes notifications
        // in queue and calls startOverrideCountdown(int) based on two things:
        // 1. If there are multiple notifications to waiting in queue to be shown
        // 2. If the current notification is expanded
        //
        // (1 && 2) -> mLongFadeOutDelay
        // (1 && !2) -> mShortFadeOutDelay
        // (!1 && 2) -> mLongFadeOutDelay
        // (!1 && !2) -> mShortFadeOutDelay
        // This method hook along with startOverrideCountdown(int) method hook deals with
        // 'Notification waiting' timeout.
        XposedHelpers.findAndHookMethod(mHoverClass, "processOverridingQueue", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                sCallingMethod = "processOverridingQueue";
                Log.d(TAG, "Calling method: " + sCallingMethod);

                // sExpanded stores the value of the boolean argument "expanded" which is passed
                // to processOverridingQueue(boolean). This denoted if the current notification
                // view being processed is expanded notification or not.
                sExpanded = Boolean.parseBoolean(String.valueOf(methodHookParam.args[0]));
                Log.d(TAG, "Notification is expanded before: " + sCallingMethod +
                        " call: " + sExpanded);
            }

            @Override
            protected void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                // Resetting flags after processOverridingQueue method has executed
                // completely so as to prevent overriding wrong sCallingMethod detection
                // and wrong notification expanded state
                Log.d(TAG, "Calling method and sExpanded reset after: " + sCallingMethod);
                sCallingMethod = "";
                sExpanded = false;
            }
        });


        // This uses the time delay value sent by processOverridingQueue(boolean)
        // capturing the delay in int argument and using the delay to set
        // duration for current Hover notification.
        XposedHelpers.findAndHookMethod(mHoverClass, "startOverrideCountdown", int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                if (sCallingMethod.equals("processOverridingQueue")) {
                    if (sExpanded) {
                        // If here, it means that there are more than one notifications
                        // in queue and the notification currently being shown is expanded.
                        // Therefore, using mLongFadeOutDelay for current notification in Hover.
                        Log.d(TAG, "startOverrideCountdown delay override " +
                                "- EXPANDED: " + mLongFadeOutDelay);
                        methodHookParam.args[0] = mLongFadeOutDelay;
                    } else {
                        //If here, it means that there are more than one notifications
                        // in queue and the notification currently being shown is not expanded.
                        // Therefore, using mShortFadeOutDelay for current notification in Hover.
                        Log.d(TAG, "startOverrideCountdown delay override " +
                                "- NOT EXPANDED: " + mShortFadeOutDelay);
                        methodHookParam.args[0] = mShortFadeOutDelay;
                    }
                }
            }
        });


        // Hooking isKeyguardSecureShowing() to override Hover notifications
        // behaviour on lock screens
        XposedHelpers.findAndHookMethod(mHoverClass, "isKeyguardSecureShowing", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                mKeyguardManager = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
                // Default behaviour of Hover is to show notifications on Insecure
                // lock screen only. (i.e. mLockscreenBehavior == 1)
                if (mLockscreenBehavior == 0) {
                    // Hide Hover notifications from secure as well as insecure lock screen
                    param.setResult(mKeyguardManager.isKeyguardLocked());
                } else if (mLockscreenBehavior == 2) {
                    // Show Hover notifications on secure as well as insecure lock screen
                    param.setResult(false);
                }
            }
        });


        // HACK: to not show non-clearable and/or low-priority notifications in Hover
        //
        // I am using the boolean variable flag "foreground" that is declared
        // as a local variable in the method "setNotification(xxx,xxx)" in Hover class.
        // The boolean decides if the notification is from a TOP MOST app,
        // i.e. the app currently on screen. It uses the method "getForegroundPackageName"
        // from NotificationHelper class and does the evaluation.
        // I re-purposed this single flag to check and evaluate 3 things now:
        // 1. If the app is actually in foreground ONLY
        // 2. If the notification is clearable or not
        // 3. If the notification is of low priority (like the ones from Google Now)
        XposedHelpers.findAndHookMethod(mHoverClass, "setNotification", mNotificationDataEntryClass, boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Object notificationEntry = param.args[0];
                mStatusBarNotification = (StatusBarNotification) XposedHelpers.getObjectField(notificationEntry, "notification");
                sCurrentNotificationPackageName = mStatusBarNotification.getPackageName();
            }
        });


        // If the notification is non-clearable and/or of low priority, depending on
        // user configuration, returning the current notification's
        // package name spoofing the actual TOP MOST (foreground) app. This makes the
        // the setNotification method act as if the notification is from the TOP MOST
        // app and it sets the "foreground" flag as true, which sends the notification
        // to status bar instead of Hover.
        XposedHelpers.findAndHookMethod(mNotificationHelperClass, "getForegroundPackageName", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (mStatusBarNotification != null) {
                    int priority = mStatusBarNotification.getNotification().priority;
                    if (mHideNonClearable && !mStatusBarNotification.isClearable()) {
                        param.setResult(sCurrentNotificationPackageName);
                    } else if (mHideLowPriority && (priority < Notification.PRIORITY_LOW)) {
                        param.setResult(sCurrentNotificationPackageName);
                    }
                }
            }
        });


        // Hook method applyStyle to apply notification style to Hover notification view
        XposedHelpers.findAndHookMethod(mNotificationHelperClass, "applyStyle", mSizeAdaptiveLayoutClass, int.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                boolean forHover = ((Integer) methodHookParam.args[1] == 0);
                ViewGroup notificationView = (ViewGroup) methodHookParam.args[0];
                if (notificationView.getBackground() == null) {
                    // if the notification has no background, use custom notification background
                    Log.i(TAG, "Set background color: " + mBackgroundColor);
                    notificationView.setBackgroundColor(Color.parseColor(mBackgroundColor));
                }
                // This deals with the alpha of the background
                // of the notification text area (title + message)
                // Text will stay opaque, only the background will be affected
                // Keeping this completely opaque as the transparency can be configured
                // through background color picker preference
                XposedHelpers.callMethod(methodHookParam.thisObject, "setViewBackgroundAlpha",
                        notificationView, forHover ? 255 : 255);

                List<View> subViews = (List) XposedHelpers.callMethod(methodHookParam.thisObject,
                        "getAllViewsInLayout", notificationView);
                for (View v : subViews) {
                    if (v instanceof ViewGroup) { // apply hover alpha to the view group
                        // Gives blackish overlay on Hover notification view
                        // Blackish overlay is disabled for now
                        XposedHelpers.callMethod(methodHookParam.thisObject, "setViewBackgroundAlpha",
                                v, forHover ? 0 : 255);
                    } else if (v instanceof ImageView) { // remove image background
                        // Sets the alpha of the background of the image on
                        // the left side - the notification icon background
                        Log.i(TAG, "Set image black background transparency: " + mImageBackgroundTransparency);
                        XposedHelpers.callMethod(methodHookParam.thisObject,
                                "setViewBackgroundAlpha", v,
                                forHover ? mImageBackgroundTransparency : 255);
                    } else if (v instanceof TextView) { // set font family
                        boolean title = v.getId() == android.R.id.title;
                        TextView text = ((TextView) v);
                        text.setTypeface(Typeface.create(
                                forHover ? FONT_FAMILY_CONDENSED :
                                        (title ? FONT_FAMILY_LIGHT : FONT_FAMILY_DEFAULT),
                                title ? Typeface.BOLD : Typeface.NORMAL));
                        if (forHover) {
                            if (title) {
                                // Set title color
                                Log.i(TAG, "Set title color: " + mTitleColor);
                                text.setTextColor(Color.parseColor(mTitleColor));
                            } else {
                                // Set notification text color
                                Log.i(TAG, "Set text color: " + mTextColor);
                                text.setTextColor(Color.parseColor(mTextColor));
                            }
                        } else {
                            // Reset the text colors to stock android notification colors
                            // for notification view in notification drawer
                            Log.i(TAG, "Reset title and text colors to stock android values");
                            if (title) {
                                text.setTextColor(Color.WHITE);
                            } else {
                                text.setTextColor(Color.parseColor("#FFAAAAAA"));
                            }
                        }
                    }
                }
                return null;
            }
        });
    }
}
