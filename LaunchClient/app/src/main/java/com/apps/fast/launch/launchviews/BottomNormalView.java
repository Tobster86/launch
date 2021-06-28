package com.apps.fast.launch.launchviews;

import android.widget.ImageButton;
import android.widget.ImageView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.views.ButtonFlasher;

import launch.game.LaunchClientGame;
import launch.game.entities.LaunchEntity;
import launch.game.entities.Missile;
import launch.game.entities.Player;
import launch.game.treaties.AffiliationRequest;
import launch.game.treaties.Treaty;
import launch.game.types.MissileType;

/**
 * Created by tobster on 14/07/16.
 */
public class BottomNormalView extends LaunchView
{
    private ImageView btnReports;
    private ImageView btnWarnings;
    private ImageView btnDiplomacy;

    private ButtonFlasher btnFlasherReports;
    private ButtonFlasher btnFlasherWarnings;
    private ButtonFlasher btnFlasherDiplomacy;

    public BottomNormalView(LaunchClientGame game, MainActivity activity)
    {
        super(game, activity);
    }

    @Override
    protected void Setup()
    {
        inflate(context, R.layout.bottom_normal, this);

        btnReports = findViewById(R.id.btnReports);
        btnWarnings = findViewById(R.id.btnWarnings);
        btnDiplomacy = findViewById(R.id.btnDiplomacy);

        btnFlasherReports = new ButtonFlasher(btnReports);
        btnFlasherWarnings = new ButtonFlasher(btnWarnings);
        btnFlasherDiplomacy = new ButtonFlasher(btnDiplomacy);

        UpdateDiplomacy();

        UpdateThreats();
    }

    @Override
    public void Update()
    {
        switch(activity.GetReportsStatus())
        {
            case NONE: btnFlasherReports.FlashOff(); break;
            case MINOR: btnFlasherReports.FlashYellow(); break;
            case MAJOR: btnFlasherReports.FlashRed(); break;
        }

        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                btnFlasherWarnings.FlashUpdate(context);
                btnFlasherReports.FlashUpdate(context);
                btnFlasherDiplomacy.FlashUpdate(context);
            }
        });
    }

    private boolean PlayerThreatened()
    {
        //Assess missile threats.
        for (Missile missile : game.GetMissiles())
        {
            MissileType type = game.GetConfig().GetMissileType(missile.GetType());

            if(game.ThreatensPlayerOptimised(missile, game.GetOurPlayer(), game.GetMissileTarget(missile), type))
            {
                return true;
            }
        }

        return false;
    }

    private void UpdateThreats()
    {
        if(PlayerThreatened())
        {
            btnFlasherWarnings.FlashRed();
        }
        else if(game.GetAnyAlerts(game.GetOurPlayer()))
        {
            btnFlasherWarnings.FlashYellow();
        }
        else
        {
            btnFlasherWarnings.FlashOff();
        }
    }

    private void UpdateDiplomacy()
    {
        if(game.PendingDiplomacyItems())
        {
            btnFlasherDiplomacy.FlashRed();
        }
        else
        {
            btnFlasherDiplomacy.FlashOff();
        }
    }

    @Override
    public void EntityUpdated(LaunchEntity entity)
    {
        Player ourPlayer = game.GetOurPlayer();

        if(ourPlayer != null)
        {
            if(entity instanceof Player)
            {
                if(entity.ApparentlyEquals(ourPlayer))
                {
                    UpdateThreats();
                }
                else if(ourPlayer.GetIsAnMP())
                {
                    Player player = (Player)entity;

                    if(player.GetRequestingToJoinAlliance())
                    {
                        if(player.GetAllianceJoiningID() == ourPlayer.GetAllianceMemberID())
                        {
                            UpdateDiplomacy();
                        }
                    }
                }
            }
            else if(entity instanceof Missile)
            {
                UpdateThreats();
            }
        }
    }

    @Override
    public void TreatyUpdated(Treaty treaty)
    {
        Player ourPlayer = game.GetOurPlayer();

        if(ourPlayer != null)
        {
            if(ourPlayer.GetIsAnMP())
            {
                if(treaty instanceof AffiliationRequest)
                {
                    if(treaty.IsAParty(ourPlayer.GetAllianceMemberID()))
                    {
                        UpdateDiplomacy();
                    }
                }
            }
        }
    }
}
