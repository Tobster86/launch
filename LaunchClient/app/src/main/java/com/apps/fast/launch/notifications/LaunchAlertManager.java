package com.apps.fast.launch.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import com.apps.fast.launch.R;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.ClientDefs;
import com.apps.fast.launch.components.Utilities;

import java.util.concurrent.TimeUnit;

import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import launch.utilities.LaunchLog;

import static launch.utilities.LaunchLog.LogType.SERVICES;

public class LaunchAlertManager
{
    private static final String TAG = "LAUNCH_ATTACK_ALERTS";
    private static final String CHANNEL_ID = "default";

    private static final long[] VIBRATE = new long[]{ 0, 2000, 400, 400, 400, 400, 400, 400 };

    private static boolean IsSDK26OrHigher()
    {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    private static boolean IsSDK23OrHigher()
    {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    /**
     * Check the integrity of alerts, restarting them if necessary. It should be called whenever Launch is authenticated.
     */
    public static void SystemCheck(Context context)
    {
        Utilities.DebugLog(context,"AlertService", "LaunchAlertWorker system check");
        RestartServices(context);
    }

    /**
     * Inform the alert manager that the check interval has changed.
     */
    public static void CheckIntervalChanged(Context context)
    {
        Utilities.DebugLog(context, "AlertService","LaunchAlertWorker interval change");
        RestartServices(context);

        /*AlertService.SetAlertNotificationInterval(context, lNotificationInterval);*/
    }

    /**
     * Inform the alert manager that the device has rebooted.
     */
    public static void DeviceRebooted(Context context)
    {
        Utilities.DebugLog(context, "AlertService","LaunchAlertWorker rebooted");
        RestartServices(context);
    }

    /**
     * Restarts the services. The defacto thing to call when anything changes or needs to be checked.
     */
    private static void RestartServices(Context context)
    {
        Utilities.DebugLog(context, "AlertService","LaunchAlertWorker restarting");
        StopServices(context);
        StartServices(context);
    }

    /**
     * Checks if any associated services are running, and stops them.
     */
    private static void StopServices(Context context)
    {
        WorkManager.getInstance().cancelAllWorkByTag(TAG);
        Utilities.DebugLog(context, "AlertService","LaunchAlertWorker stopped");
    }

    /**
     * Starts
     */
    private static void StartServices(Context context)
    {
        SharedPreferences sharedPreferences = context.getSharedPreferences(ClientDefs.SETTINGS, Context.MODE_PRIVATE);

        //Disable if disabled!
        if(sharedPreferences.getInt(ClientDefs.SETTINGS_NOTIFICATION_MINUTES, ClientDefs.SETTINGS_NOTIFICATION_MINUTES_DEFAULT) == 0)
            return;

        //Register notification channel if not registered (API 26+ only).
        if (IsSDK26OrHigher())
        {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager.getNotificationChannel(CHANNEL_ID) != null)
            {
                notificationManager.deleteNotificationChannel(CHANNEL_ID);
            }

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, context.getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(context.getString(R.string.notification_channel_description));
            channel.enableLights(true);
            channel.enableVibration(true);

            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            channel.setSound(Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.airraid), attributes);
            channel.setVibrationPattern(VIBRATE);

            notificationManager.createNotificationChannel(channel);
        }

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(LaunchAlertWorker.class, sharedPreferences.getInt(ClientDefs.SETTINGS_NOTIFICATION_MINUTES, ClientDefs.SETTINGS_NOTIFICATION_MINUTES_DEFAULT), TimeUnit.MINUTES)
                .addTag(TAG)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance().enqueue(workRequest);

        Utilities.DebugLog(context, "AlertService", "LaunchAlertWorker started");
    }

    public static void FireAlert(Context context, boolean bNukeEscalation, boolean bAlly)
    {
        LaunchLog.Log(SERVICES, "AlertService", "Raising notification.");
        SharedPreferences sharedPreferences = context.getSharedPreferences(ClientDefs.SETTINGS, Context.MODE_PRIVATE);

        if(bNukeEscalation && !sharedPreferences.getBoolean(ClientDefs.SETTINGS_NOTIFICATION_NUKEESC, ClientDefs.SETTINGS_NOTIFICATION_NUKEESC_DEFAULT))
        {
            //Ally under attack notifications turned off.
            return;
        }

        if(bAlly && !sharedPreferences.getBoolean(ClientDefs.SETTINGS_NOTIFICATION_ALLIES, ClientDefs.SETTINGS_NOTIFICATION_ALLIES_DEFAULT))
        {
            //Ally under attack notifications turned off.
            return;
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID);
        notificationBuilder.setSmallIcon(R.drawable.notification);

        if(!IsSDK26OrHigher())
        {
            notificationBuilder.setPriority(Notification.PRIORITY_HIGH);
            notificationBuilder.setSound(Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.airraid));
            notificationBuilder.setVibrate(VIBRATE);
        }

        notificationBuilder.setContentTitle(context.getString(R.string.app_name));

        if(bNukeEscalation)
            notificationBuilder.setContentText(context.getString(R.string.notification_text_nuke_escalation));
        else if(bAlly)
            notificationBuilder.setContentText(context.getString(R.string.notification_text_ally));
        else
            notificationBuilder.setContentText(context.getString(R.string.notification_text));

        notificationBuilder.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), 0));
        notificationBuilder.setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, notificationBuilder.build());
    }
}
