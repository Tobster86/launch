package com.apps.fast.launch.notifications;

import android.content.Context;
import android.support.annotation.NonNull;

import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.Utilities;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class LaunchAlertWorker extends Worker
{
    private Context context;

    public LaunchAlertWorker(@NonNull Context context, @NonNull WorkerParameters workerParams)
    {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork()
    {
        if(!MainActivity.GetRunning())
        {
            Utilities.DebugLog(context, "AlertService", "Main activity not running. Firing notification service handler.");
            NotificationServiceHandler handler = new NotificationServiceHandler(context);
            handler.Start();
        }

        return Result.SUCCESS;
    }
}
