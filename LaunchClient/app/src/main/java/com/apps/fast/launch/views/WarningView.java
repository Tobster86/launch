package com.apps.fast.launch.views;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.UI.AvatarBitmaps;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.TextUtilities;

import launch.game.LaunchClientGame;
import launch.game.entities.LaunchEntity;
import launch.game.entities.Missile;
import launch.game.entities.Player;
import launch.game.types.MissileType;

/**
 * Created by tobster on 09/11/15.
 */
public class WarningView extends FrameLayout
{
    private MainActivity activity;
    private LaunchClientGame game;

    private LaunchEntity threat;

    private long oTimeAtCreation = System.currentTimeMillis();
    private int lTimeToTargetAtCreation;

    //A missile.
    public WarningView(final MainActivity activity, LaunchClientGame game, final Missile missile)
    {
        super(activity);
        this.activity = activity;
        this.game = game;
        this.threat = missile;

        MissileType type = game.GetConfig().GetMissileType(missile.GetType());
        Player owner = game.GetPlayer(missile.GetOwnerID());

        lTimeToTargetAtCreation = game.GetTimeToTarget(missile);

        inflate(activity, R.layout.view_warning, this);

        ((ImageView)findViewById(R.id.imgDoer)).setImageBitmap(AvatarBitmaps.GetPlayerAvatar(activity, game, owner));
        ((TextView)findViewById(R.id.txtWarning)).setText(activity.getString(R.string.threat_missile, type.GetName(), owner.GetName()));
        ((ImageView)findViewById(R.id.imgType)).setImageResource(R.drawable.marker_missile);

        setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                activity.SelectEntity(missile);
            }
        });

        Update();
    }

    public void Update()
    {
        //Update time remaining text as an "impact in...".
        ((TextView)findViewById(R.id.txtThreatTime)).setText(activity.getString(R.string.threat_time, TextUtilities.GetTimeAmount(GetTimeToTarget())));
    }

    public long GetTimeToTarget()
    {
        return lTimeToTargetAtCreation - (System.currentTimeMillis() - oTimeAtCreation);
    }
}
