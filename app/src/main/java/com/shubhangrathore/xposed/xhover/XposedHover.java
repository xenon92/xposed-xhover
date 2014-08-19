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
import android.service.notification.StatusBarNotification;
import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
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
    private static final String PACKAGE_XHOVER = XposedHover.class.getPackage().getName();
    private static final String PACKAGE_SYSTEM_UI = "com.android.systemui";

    private static String sCallingMethod = "";
    private static boolean sExpanded;
    private static String sCurrentNotificationPackageName;

    private int mMicroFadeOutDelay;            // Evade notification time delay
    private int mShortFadeOutDelay;            // Notification waiting time delay
    private int mLongFadeOutDelay;             // Natural timeout delay
    private int mLockscreenBehavior;
    private boolean mHideNonClearable;
    private boolean mHideLowPriority;

    private KeyguardManager mKeyguardManager;
    private Context mContext;
    private StatusBarNotification mStatusBarNotification;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
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
                        .getString(MainActivity.PREF_LONG_FADE_OUT_DELAY, "2500"));

        mLockscreenBehavior =
                Integer.parseInt(mXSharedPreferences
                        .getString(MainActivity.PREF_LOCKSCREEN_BEHAVIOR, "1"));

        mHideNonClearable = mXSharedPreferences.getBoolean(MainActivity.PREF_HIDE_NON_CLEARABLE, false);
        mHideLowPriority = mXSharedPreferences.getBoolean(MainActivity.PREF_HIDE_LOW_PRIORITY, false);

        final Class<?> mHoverClass = XposedHelpers.findClass(CLASS_HOVER, loadPackageParam.classLoader);
        final Class<?> mNotificationDataEntryClass = XposedHelpers.findClass(CLASS_NOTIFICATION_DATA_ENTRY, loadPackageParam.classLoader);
        final Class<?> mNotificationHelperClass = XposedHelpers.findClass(CLASS_NOTIFICATION_HELPER, loadPackageParam.classLoader);


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
                int priority = mStatusBarNotification.getNotification().priority;
                if (mHideNonClearable && !mStatusBarNotification.isClearable()) {
                    param.setResult(sCurrentNotificationPackageName);
                } else if (mHideLowPriority && (priority < Notification.PRIORITY_LOW)) {
                    param.setResult(sCurrentNotificationPackageName);
                }
            }
        });
    }
}
