package com.apps.fast.launch.launchviews;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.apps.fast.launch.R;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.ClientDefs;

import java.util.Timer;
import java.util.TimerTask;

import launch.game.LaunchClientGame;

/**
 * Created by tobster on 15/07/16.
 */
public class DisclaimerView extends LaunchView
{
    private static final long TWO_SECONDS = 2000;

    private CheckBox chkAgree;
    private boolean bAgreed = false;

    public DisclaimerView(LaunchClientGame game, MainActivity activity)
    {
        super(game, activity);
    }

    @Override
    protected void Setup()
    {
        inflate(context, R.layout.main_disclaimer, this);

        chkAgree = (CheckBox)findViewById(R.id.chkAgree);

        chkAgree.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b)
            {
                //We will dismiss the page two seconds after the user checks agree. This boolean check is so the timed event only fires once.
                if(!bAgreed)
                {
                    bAgreed = true;

                    new Timer().schedule(new TimerTask()
                    {
                        @Override
                        public void run()
                        {
                            SharedPreferences.Editor editor = context.getSharedPreferences(ClientDefs.SETTINGS, Context.MODE_PRIVATE).edit();
                            editor.putBoolean(ClientDefs.SETTINGS_DISCLAIMER_ACCEPTED, true);
                            editor.commit();

                            activity.DisclaimerAgreed();
                        }
                    }, TWO_SECONDS);
                }
            }
        });
    }

    @Override
    public void Update()
    {
    }
}
