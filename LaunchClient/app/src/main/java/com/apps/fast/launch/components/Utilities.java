package com.apps.fast.launch.components;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.apps.fast.launch.R;
import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

import launch.game.GeoCoord;
import launch.game.entities.Structure;
import launch.utilities.LaunchLog;
import launch.utilities.Security;

import static android.content.Context.INPUT_METHOD_SERVICE;
import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;
import static launch.utilities.LaunchLog.LogType.SERVICES;

/**
 * Created by tobster on 21/01/16.
 */
public class Utilities
{
    private static final DateFormat dateFormatTime = new SimpleDateFormat("MM-dd HH:mm:ss");
    private static final int IMEI_MEID_MIN_LENGTH = 14;

    private static Random random = new Random();

    public static List<String> HackilyLoggedExceptions = new ArrayList<>();

    public static LatLng GetLatLng(GeoCoord geoCoord)
    {
        return new LatLng(geoCoord.GetLatitude(), geoCoord.GetLongitude());
    }

    public static int GetPixelsFromDPDimension(Context context, int lDimensionID)
    {
        return context.getResources().getDimensionPixelSize(lDimensionID);
    }

    public static <K, V> K GetMapKeyByValue(Map<K, V> map, V value)
    {
        for (Map.Entry<K, V> entry : map.entrySet())
        {
            if (value.equals(entry.getValue()))
            {
                return entry.getKey();
            }
        }
        return null;
    }

    public static long GetServerTime()
    {
        //TO DO: Return the approximate time on the server, as adjusted by information it provided us with.
        return System.currentTimeMillis();
    }

    public static boolean DeviceHasValidID(Context context)
    {
        SharedPreferences sharedPreferences = context.getSharedPreferences(ClientDefs.SETTINGS, context.MODE_PRIVATE);
        String strLastIdentity = sharedPreferences.getString(ClientDefs.SETTINGS_IDENTITY_STORED, "");
        if (!strLastIdentity.equals(""))
        {
            return true;
        }

        //Tablet check.
        String strPhoneNumber = ((TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number();
        if (strPhoneNumber == null)
        {
            return false;
        }

        //IMEI/MEID checks.
        try
        {
            String strDeviceID = ((TelephonyManager)context.getSystemService(context.TELEPHONY_SERVICE)).getDeviceId();

            if(DeviceIdentityCheck(strDeviceID))
            {
                return true;
            }
        }
        catch (Exception ex)
        {
            return false;
        }

        return false;
    }

    public static byte[] GetEncryptedDeviceID(Context context)
    {
        SharedPreferences sharedPreferences = context.getSharedPreferences(ClientDefs.SETTINGS, context.MODE_PRIVATE);

        String strLastIdentity = sharedPreferences.getString(ClientDefs.SETTINGS_IDENTITY_STORED, "");
        if (!strLastIdentity.equals(""))
        {
            return Security.HexStringToBytes(strLastIdentity);
        }

        //We don't yet have a stored ID. Get one and store it.
        byte[] cDeviceID = new byte[Security.SHA256_SIZE];

        try
        {
            String strDeviceID = ((TelephonyManager) context.getSystemService(context.TELEPHONY_SERVICE)).getDeviceId();
            cDeviceID = Security.GetSHA256(strDeviceID.getBytes());

            SharedPreferences.Editor editor = context.getSharedPreferences(ClientDefs.SETTINGS, Context.MODE_PRIVATE).edit();
            editor.putString(ClientDefs.SETTINGS_IDENTITY_STORED, Security.BytesToHexString(cDeviceID));
            editor.commit();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

        return cDeviceID;
    }

    public static void CreateRandomDeviceID(Context context)
    {
        try
        {
            SharedPreferences.Editor editor = context.getSharedPreferences(ClientDefs.SETTINGS, Context.MODE_PRIVATE).edit();
            editor.putBoolean(ClientDefs.SETTINGS_IDENTITY_GENERATED, true);
            editor.putString(ClientDefs.SETTINGS_IDENTITY_STORED, Security.BytesToHexString(Security.CreateRandomHash()));
            editor.commit();
        }
        catch (Exception ex)
        {
            //Shouldn't get here. We might get an app crash report if we do.
            new RuntimeException(ex);
        }
    }

    public static String GetDeviceName()
    {
        return String.format("%s|%s|%s|%s|%s|%s", Build.FINGERPRINT, Build.MODEL, Build.MANUFACTURER, Build.BRAND, Build.DEVICE, Build.PRODUCT);
    }

    public static String GetProcessName(Context context)
    {
        return context.getApplicationInfo().dataDir;
        /*int lPID = android.os.Process.myPid();
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> RunningProcesses = manager.getRunningAppProcesses();
        if (RunningProcesses != null)
        {
            for (ActivityManager.RunningAppProcessInfo processInfo : RunningProcesses)
            {
                if (processInfo.pid == lPID)
                {
                    return processInfo.processName;
                }
            }
        }
        return "UNABLE TO READ";*/
    }

    private static boolean DeviceIdentityCheck(String strIdentity)
    {
        //IMEIs and MEIDs are expected to be of at least certain length.
        if(strIdentity.length() < IMEI_MEID_MIN_LENGTH)
        {
            return false;
        }

        //All zeroes indicates Android emulator. We want to randomly generate an ID instead for uniqueness. Google Attestation checks will pick up emulators otherwise.
        if(strIdentity.contains("00000000000000"))
        {
            return false;
        }

        //Another net to catch emulators.
        if(strIdentity.equals("358240051111110"))
        {
            return false;
        }

        //Emulators are now trapped and banned through other means.
        /*if(strIdentity.replace("0", "").equals(""))
        {
            //Return false for all-zeros (which is what emulators tend to have as an IMEI).
            return false;
        }*/

        //TO DO: Luhn check; reinstate when you figure out what to do with MEIDs.
        /*int sum = 0;
        boolean alternate = false;
        for (int i = strIdentity.length() - 1; i >= 0; i--)
        {
            int n = Integer.parseInt(strIdentity.substring(i, i + 1));
            if (alternate)
            {
                n *= 2;
                if (n > 9)
                {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);*/

        return true;
    }

    public static void DismissKeyboard(Activity activity, View view)
    {
        InputMethodManager inputMethodManager = (InputMethodManager)activity.getSystemService(INPUT_METHOD_SERVICE);

        if(inputMethodManager != null)
        {
            if (inputMethodManager.isAcceptingText())
            {
                if(view != null)
                {
                    IBinder binder = view.getWindowToken();

                    if(binder != null)
                    {
                        inputMethodManager.hideSoftInputFromWindow(binder, 0);
                    }
                }
            }
        }
    }

    public static boolean CheckPermissions(Context context)
    {
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.INTERNET) != PERMISSION_GRANTED ||
           ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_NETWORK_STATE) != PERMISSION_GRANTED ||
           ContextCompat.checkSelfPermission(context, Manifest.permission.VIBRATE) != PERMISSION_GRANTED ||
           ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED ||
           ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PERMISSION_GRANTED ||
           ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PERMISSION_GRANTED ||
           ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_BOOT_COMPLETED) != PERMISSION_GRANTED)
        {
            return false;
        }

        return true;
    }

    public static int ColourFromAttr(Context context, int lAttrID)
    {
        TypedValue result = new TypedValue();
        context.getTheme().resolveAttribute(lAttrID, result, true);
        return result.data;
    }

    public static Drawable DrawableFromAttr(Context context, int lAttrID)
    {
        TypedValue result = new TypedValue();
        return context.getDrawable(context.getTheme().obtainStyledAttributes(new int[] {lAttrID}).getResourceId(0, 0));
    }

    public static String GetStructureName(Context context, Structure structure)
    {
        if(structure.GetName().length() > 0)
        {
            return structure.GetName();
        }
        else
        {
            return context.getString(R.string.unnamed);
        }
    }

    public static boolean ValidateName(String strName)
    {
        if(strName.trim().length() > 0)
        {
            //TO DO: Swearwords etc.
            return true;
        }

        return false;
    }

    public static void DebugLog(Context context, String strLogName, String strLog)
    {
        LaunchLog.Log(SERVICES, strLogName, strLog);
        SharedPreferences sharedPreferences = context.getSharedPreferences(ClientDefs.SETTINGS, Context.MODE_PRIVATE);

        if(sharedPreferences.getBoolean(ClientDefs.SETTINGS_NOTIFICATION_DEBUG, ClientDefs.SETTINGS_NOTIFICATION_DEBUG_DEFAULT))
        {
            int lDebugIndex = sharedPreferences.getInt(ClientDefs.DEBUG_INDEX, 0);

            Date now = Calendar.getInstance().getTime();
            String strTime = dateFormatTime.format(now);

            SharedPreferences.Editor editor = context.getSharedPreferences(ClientDefs.SETTINGS, Context.MODE_PRIVATE).edit();

            editor.putString(ClientDefs.DEBUG_PREFIX + lDebugIndex, String.format("%s - %s", strTime, strLog));

            lDebugIndex++;
            if(lDebugIndex >= ClientDefs.NOTIFICATION_DEBUG_LOG_SIZE)
                lDebugIndex = 0;

            editor.putInt(ClientDefs.DEBUG_INDEX, lDebugIndex);

            editor.commit();
        }
    }

    /**
     * DO NOT INCLUDE IN RELEASES.
     */
    public static void StartHackilyLoggingExceptions()
    {
        HackilyLoggedExceptions.add("Horrible failures will appear here. Touch to copy.");

        java.lang.Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler()
        {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable)
            {
                HackilyLoggedExceptions.add(throwable.getMessage());
            }
        });

        /*Thread hackyExceptionLoggingThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                HackilyLoggedExceptions.add("Horrible failures will appear here. Touch to copy.");

                boolean bHackiliyLoggingExceptions = true;

                try
                {
                    Process process = Runtime.getRuntime().exec("logcat");
                    BufferedReader bufferedReader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()));

                    while(bHackiliyLoggingExceptions)
                    {
                        String line = bufferedReader.readLine();

                        if(line.contains("Exception") || line.contains("at "))
                        {
                            HackilyLoggedExceptions.add(line);
                        }
                    }
                }
                catch(Exception ex)
                {
                    bHackiliyLoggingExceptions = false;
                    HackilyLoggedExceptions.add("The hacky exception logging thread has shat its trousers.");
                }
            }
        });

        hackyExceptionLoggingThread.start();*/
    }

    /**
     * Colour the textbox associated with the largest value "good colour" and the smallest "bad colour"; or both "warning colour" if equal.
     * BRO TIP: To colour the smallest good, reverse lVal1 & lVal2 in the function call.
     * @param context App context.
     * @param txt1 Text view 1.
     * @param txt2 Text view 2.
     * @param lVal1 Value associated with text view 1.
     * @param lVal2 Value associated with text view 2.
     */
    public static void ColourLargestGood(Context context, TextView txt1, TextView txt2, int lVal1, int lVal2)
    {
        if(lVal1 > lVal2)
        {
            txt1.setTextColor(ColourFromAttr(context, R.attr.GoodColour));
            txt2.setTextColor(ColourFromAttr(context, R.attr.BadColour));
        }
        else if(lVal1 < lVal2)
        {
            txt1.setTextColor(ColourFromAttr(context, R.attr.BadColour));
            txt2.setTextColor(ColourFromAttr(context, R.attr.GoodColour));
        }
        else
        {
            txt1.setTextColor(ColourFromAttr(context, R.attr.WarningColour));
            txt2.setTextColor(ColourFromAttr(context, R.attr.WarningColour));
        }
    }

    /**
     * Colour the textbox associated with the largest value "good colour" and the smallest "bad colour"; or both "warning colour" if equal.
     * BRO TIP: To colour the smallest good, reverse fltVal1 & fltVal2 in the function call.
     * @param context App context.
     * @param txt1 Text view 1.
     * @param txt2 Text view 2.
     * @param fltVal1 Value associated with text view 1.
     * @param fltVal2 Value associated with text view 2.
     */
    public static void ColourLargestGood(Context context, TextView txt1, TextView txt2, float fltVal1, float fltVal2)
    {
        if(fltVal1 > fltVal2)
        {
            txt1.setTextColor(ColourFromAttr(context, R.attr.GoodColour));
            txt2.setTextColor(ColourFromAttr(context, R.attr.BadColour));
        }
        else if(fltVal1 < fltVal2)
        {
            txt1.setTextColor(ColourFromAttr(context, R.attr.BadColour));
            txt2.setTextColor(ColourFromAttr(context, R.attr.GoodColour));
        }
        else
        {
            txt1.setTextColor(ColourFromAttr(context, R.attr.WarningColour));
            txt2.setTextColor(ColourFromAttr(context, R.attr.WarningColour));
        }
    }
}
