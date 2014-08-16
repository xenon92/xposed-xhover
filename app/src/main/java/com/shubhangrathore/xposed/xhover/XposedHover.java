package com.shubhangrathore.xposed.xhover;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by Shubhang on 14/8/2014.
 */
public class XposedHover implements IXposedHookLoadPackage {

    private static final String CLASS_HOVER = "com.android.systemui.statusbar.notification.Hover";
    private static final String PACKAGE_XHOVER = XposedHover.class.getPackage().getName();
    private static final String PACKAGE_SYSTEM_UI = "com.android.systemui";

    private static String sCallingMethod = "";
    private static boolean sExpanded;

    private int mMicroFadeOutDelay;            // Evade notification time delay
    private int mShortFadeOutDelay;            // Notification waiting time delay
    private int mLongFadeOutDelay;             // Natural timeout delay

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

        final Class<?> mHoverClass = XposedHelpers.findClass(CLASS_HOVER, loadPackageParam.classLoader);

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
                XposedBridge.log("Calling method = startMicroHideCountdown()");
                sCallingMethod = "startMicroHideCountdown";
            }

            @Override
            protected void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {

                // Resetting the flag after the countdown methods have executed
                // completely so as to prevent overriding wrong sCallingMethod detection
                sCallingMethod = "";
            }
        });


        /*
        //
        // startShortHideCountdown method is an unused method in Hover.java
        //
        // Hooking method startShortHideCountdown() that calls method startHideCountdown(int)
        // providing the later method with the time delay for which Hover notification
        // will stay on screen if another notification is waiting to be shown
        // This is the 'Notification waiting' time delay.
        XposedHelpers.findAndHookMethod(mHoverClass, "startShortHideCountdown", new XC_MethodHook() {

            @Override
            protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {

                XposedBridge.log("Calling method = startShortHideCountdown()");
                sCallingMethod = "startShortHideCountdown";
            }

            @Override
            protected void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
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

                XposedBridge.log("Calling method = startLongHideCountdown()");
                sCallingMethod = "startLongHideCountdown";
            }

            @Override
            protected void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
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
                    XposedBridge.log("Setting startHideCountdown delay to: "
                            + String.valueOf(mMicroFadeOutDelay));

                    methodHookParam.args[0] = mMicroFadeOutDelay;

                /*

                // startShortHideCountdown method is an unused method in Hover.java

                } else if (sCallingMethod.equals("startShortHideCountdown")) {

                    // Detected that startShortHideCountdown() method called
                    // startHideCountdown(int) method.
                    // Overriding 'Notification waiting' values here.
                    XposedBridge.log("Setting startHideCountdown delay to: "
                            + String.valueOf(mShortFadeOutDelay));

                    methodHookParam.args[0] = mShortFadeOutDelay;
                */

                } else if (sCallingMethod.equals("startLongHideCountdown")) {

                    // Detected that startLongHideCountdown() method called
                    // startHideCountdown(int) method.
                    // Overriding 'Natural timeout' values here.
                    XposedBridge.log("Setting startHideCountdown delay to: "
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
        XposedHelpers.findAndHookMethod(mHoverClass, "processOverridingQueue", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {

                XposedBridge.log("Calling method = processOverridingQueue()");
                sCallingMethod = "processOverridingQueue";

                // sExpanded stores the value of the boolean argument "expanded" which is passed
                // to processOverridingQueue(boolean). This denoted if the current notification
                // view being processed is expanded notification or not.
                sExpanded = Boolean.parseBoolean(String.valueOf(methodHookParam.args[0]));
                XposedBridge.log("Notification is expanded before " +
                        "processOverridingQueue() call = " + sExpanded);
            }

            @Override
            protected void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {

                // Resetting flags after processOverridingQueue method has executed
                // completely so as to prevent overriding wrong sCallingMethod detection
                // and wrong notification expanded state
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
                        XposedBridge.log("startOverrideCountdown delay override " +
                                "- EXPANDED = " + mLongFadeOutDelay);
                        methodHookParam.args[0] = mLongFadeOutDelay;
                    } else {

                        //If here, it means that there are more than one notifications
                        // in queue and the notification currently being shown is not expanded.
                        // Therefore, using mShortFadeOutDelay for current notification in Hover.
                        XposedBridge.log("startOverrideCountdown delay override " +
                                "- NOT EXPANDED = " + mShortFadeOutDelay);
                        methodHookParam.args[0] = mShortFadeOutDelay;
                    }
                }
            }
        });
    }
}
