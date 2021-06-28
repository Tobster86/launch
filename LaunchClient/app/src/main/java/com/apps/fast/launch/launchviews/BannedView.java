package com.apps.fast.launch.launchviews;

import android.widget.TextView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.TextUtilities;

import launch.game.LaunchClientGame;

/**
 * Created by tobster on 15/07/16.
 */
public class BannedView extends LaunchView
{
    private boolean bPermanent;
    private long oCreated;
    private long oDuration;
    private String strReason;

    private TextView txtReason;
    private TextView txtDurationTitle;
    private TextView txtDuration;

    public BannedView(LaunchClientGame game, MainActivity activity, String strReason, long oDuration)
    {
        super(game, activity, true);
        bPermanent = false;
        this.oDuration = oDuration;
        this.strReason = strReason;
        oCreated = System.currentTimeMillis();
        Setup();
    }

    public BannedView(LaunchClientGame game, MainActivity activity, String strReason)
    {
        super(game, activity, true);
        bPermanent = true;
        this.strReason = strReason;
        Setup();
    }

    @Override
    protected void Setup()
    {
        inflate(context, R.layout.main_banned, this);

        txtReason = findViewById(R.id.txtReason);
        txtDurationTitle = findViewById(R.id.txtDurationTitle);
        txtDuration = findViewById(R.id.txtDuration);

        txtReason.setText(strReason);

        if(bPermanent)
        {
            txtDuration.setVisibility(GONE);
            txtDurationTitle.setVisibility(GONE);
        }

        Update();
    }

    @Override
    public void Update()
    {
        if(!bPermanent)
        {
            activity.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    txtDuration.setText(TextUtilities.GetTimeAmount(oDuration - (System.currentTimeMillis() - oCreated)));
                }
            });

            if(System.currentTimeMillis() - oCreated >= oDuration)
            {
                activity.GoToGame();
            }
        }
    }
}
