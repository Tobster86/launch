package com.apps.fast.launch.launchviews;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import com.apps.fast.launch.R;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.ClientDefs;
import com.apps.fast.launch.components.Utilities;

import java.util.Timer;
import java.util.TimerTask;

import launch.game.LaunchClientGame;

/**
 * Created by tobster on 15/07/16.
 */
public class NoIMEIView extends LaunchView
{
    public NoIMEIView(LaunchClientGame game, MainActivity activity)
    {
        super(game, activity);
    }

    @Override
    protected void Setup()
    {
        inflate(context, R.layout.main_nonimei, this);

        findViewById(R.id.btnQuit).setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                activity.finish();
            }
        });

        findViewById(R.id.btnGenerateIMEI).setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Utilities.CreateRandomDeviceID(context);
                activity.GoToGame();
            }
        });
    }

    @Override
    public void Update()
    {
    }
}
