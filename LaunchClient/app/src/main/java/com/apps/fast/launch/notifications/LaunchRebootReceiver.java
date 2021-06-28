package com.apps.fast.launch.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class LaunchRebootReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()))
        {
            LaunchAlertManager.DeviceRebooted(context);
        }
    }
}
