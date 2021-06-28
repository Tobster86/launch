package com.apps.fast.launch.launchviews.controls;

import android.widget.LinearLayout;
import android.widget.TextView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.TextUtilities;
import com.apps.fast.launch.launchviews.LaunchView;

import launch.game.LaunchClientGame;
import launch.game.entities.SentryGun;

public class SentryGunControl extends LaunchView
{
    private int lID;
    private LinearLayout lytReload;
    private TextView txtReloading;

    public SentryGunControl(LaunchClientGame game, MainActivity activity, int lSentryGunID)
    {
        super(game, activity, true);
        lID = lSentryGunID;
        Setup();
    }

    @Override
    protected void Setup()
    {
        inflate(context, R.layout.control_sentry_gun, this);

        lytReload = (LinearLayout) findViewById(R.id.lytReload);
        txtReloading = (TextView) findViewById(R.id.txtReloading);

        Update();
    }

    @Override
    public void Update()
    {
        SentryGun sentryGun = game.GetSentryGun(lID);

        //Reload.
        long oReloadTimeRemaining = sentryGun.GetReloadTimeRemaining();

        if (oReloadTimeRemaining > 0)
        {
            lytReload.setVisibility(VISIBLE);
            txtReloading.setText(TextUtilities.GetTimeAmount(oReloadTimeRemaining));
        }
        else
        {
            lytReload.setVisibility(GONE);
        }
    }
}
