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

    private int mMicroFadeOutDelay;            // Evade notification time delay

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {

        if (!loadPackageParam.packageName.equals(PACKAGE_SYSTEM_UI)) {
            return;
        }

        XSharedPreferences mXSharedPreferences = new XSharedPreferences(PACKAGE_XHOVER);

        mMicroFadeOutDelay = Integer.parseInt(mXSharedPreferences.getString(MainActivity.PREF_MICRO_FADE_OUT_DELAY, "1250"));

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

        XposedHelpers.findAndHookMethod(mHoverClass, "startHideCountdown", int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {

                if (sCallingMethod.equals("startMicroHideCountdown")) {

                    // Detected that startMicroHideCountdown() method called
                    // startHideCountdown(int) method.
                    // Overriding 'Evade notification' values here.
                    XposedBridge.log("Setting startHideCountdown delay to: " + String.valueOf(mMicroFadeOutDelay));
                    methodHookParam.args[0] = mMicroFadeOutDelay;
                }
            }
        });
    }
}
