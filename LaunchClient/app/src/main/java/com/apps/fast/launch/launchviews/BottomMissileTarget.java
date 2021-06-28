package com.apps.fast.launch.launchviews;

import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.ClientDefs;
import com.apps.fast.launch.components.TextUtilities;
import com.apps.fast.launch.components.Utilities;
import com.apps.fast.launch.views.LaunchDialog;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;

import java.util.ArrayList;
import java.util.List;

import launch.game.Defs;
import launch.game.GeoCoord;
import launch.game.LaunchClientGame;
import launch.game.entities.Player;
import launch.game.types.MissileType;

/**
 * Created by tobster on 14/07/16.
 */
public class BottomMissileTarget extends LaunchView
{
    private TextView txtInstructions;
    private TextView txtOutOfRange;
    private TextView txtMissileName;
    private TextView txtFlightTime;
    private TextView txtFriendlyFire;
    private LinearLayout btnFire;
    private LinearLayout btnCancel;
    private ImageView imgTracking;

    private boolean bFromPlayer;
    private int lSiteID;
    private byte cSlotNo;
    private MissileType missileType;

    private GeoCoord geoTarget = null;
    private boolean bTrackPlayer;
    private int lTrackPlayerID;

    private GoogleMap map;
    private Polyline targetTrajectory;

    //To not repeat elite/noob attack warnings.
    private static int lLastWarnedID = -1;

    //From marker_player.
    public BottomMissileTarget(LaunchClientGame game, MainActivity activity, byte cSlotNo)
    {
        super(game, activity, true);
        bFromPlayer = true;
        this.cSlotNo = cSlotNo;
        missileType = game.GetConfig().GetMissileType(game.GetOurPlayer().GetMissileSystem().GetSlotMissileType(cSlotNo));
        Setup();
    }

    //From missile site.
    public BottomMissileTarget(LaunchClientGame game, MainActivity activity, int lSiteID, byte cSlotNo)
    {
        super(game, activity, true);
        bFromPlayer = false;
        this.lSiteID = lSiteID;
        this.cSlotNo = cSlotNo;
        missileType = game.GetConfig().GetMissileType(game.GetMissileSite(lSiteID).GetMissileSystem().GetSlotMissileType(cSlotNo));
        Setup();
    }

    @Override
    protected void Setup()
    {
        inflate(context, R.layout.bottom_missile_target, this);

        txtInstructions = findViewById(R.id.txtInstructions);
        txtOutOfRange = findViewById(R.id.txtOutOfRange);
        txtMissileName = findViewById(R.id.txtMissileName);
        txtFlightTime = findViewById(R.id.txtFlightTime);
        txtFriendlyFire = findViewById(R.id.txtFriendlyFire);
        btnFire = findViewById(R.id.btnFire);
        btnCancel = findViewById(R.id.btnCancel);
        imgTracking = findViewById(R.id.imgTracking);

        txtMissileName.setText(missileType.GetName());

        btnFire.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if(geoTarget == null)
                {
                    //Target not selected.
                    activity.ShowBasicOKDialog(context.getString(R.string.must_specify_target_map));
                }
                else if((bFromPlayer && game.GetOurPlayer().GetPosition().DistanceTo(geoTarget) > game.GetConfig().GetMissileRange(missileType.GetRangeIndex())) ||
                        (!bFromPlayer && game.GetMissileSite(lSiteID).GetPosition().DistanceTo(geoTarget) > game.GetConfig().GetMissileRange(missileType.GetRangeIndex())))
                {
                    //Target out of range.
                    activity.ShowBasicOKDialog(context.getString(R.string.target_out_of_range));
                }
                else if(game.ThreatensFriendlies(game.GetOurPlayerID(), geoTarget, missileType, false, false))
                {
                    activity.ShowBasicOKDialog(context.getString(R.string.deliberate_friendly_fire));
                }
                else if(game.GetOurPlayer().GetRespawnProtected())
                {
                    final LaunchDialog launchDialog = new LaunchDialog();
                    launchDialog.SetHeaderLaunch();
                    launchDialog.SetMessage(context.getString(R.string.attacking_respawn_protected_you, TextUtilities.GetTimeAmount(game.GetOurPlayer().GetStateTimeRemaining())));
                    launchDialog.SetOnClickYes(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View view)
                        {
                            launchDialog.dismiss();
                            SecondStateFiringChecks();
                        }
                    });
                    launchDialog.SetOnClickNo(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View view)
                        {
                            launchDialog.dismiss();
                        }
                    });
                    launchDialog.show(activity.getFragmentManager(), "");
                }
                else
                {
                    SecondStateFiringChecks();
                }
            }
        });

        btnCancel.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                activity.InformationMode();
            }
        });
        Update();
    }

    private void SecondStateFiringChecks()
    {
        //Splitting like this allows chaining of warning messages (breaking own respawn invulnerability, followed by everything else).
        List<Player> AffectedPlayers = game.GetAffectedPlayers(geoTarget, game.GetConfig().GetBlastRadius(missileType));

        if(AffectedPlayers.size() == 1)
        {
            final Player affectedPlayer = AffectedPlayers.get(0);

            if(lLastWarnedID != affectedPlayer.GetID() && affectedPlayer.GetRespawnProtected())
            {
                final LaunchDialog launchDialog = new LaunchDialog();
                launchDialog.SetHeaderLaunch();
                launchDialog.SetMessage(context.getString(R.string.attacking_respawn_protected, affectedPlayer.GetName(), TextUtilities.GetTimeAmount(affectedPlayer.GetStateTimeRemaining())));
                launchDialog.SetOnClickYes(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        launchDialog.dismiss();
                        lLastWarnedID = affectedPlayer.GetID();
                        Launch();
                    }
                });
                launchDialog.SetOnClickNo(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        launchDialog.dismiss();
                    }
                });
                launchDialog.show(activity.getFragmentManager(), "");
            }
            else if(lLastWarnedID != affectedPlayer.GetID() && game.GetNetWorthMultiplier(game.GetOurPlayer(), affectedPlayer) < Defs.NOOB_WARNING)
            {
                final LaunchDialog launchDialog = new LaunchDialog();
                launchDialog.SetHeaderLaunch();
                launchDialog.SetMessage(context.getString(R.string.attacking_noob, affectedPlayer.GetName()));
                launchDialog.SetOnClickYes(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        launchDialog.dismiss();
                        lLastWarnedID = affectedPlayer.GetID();
                        Launch();
                    }
                });
                launchDialog.SetOnClickNo(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        launchDialog.dismiss();
                    }
                });
                launchDialog.show(activity.getFragmentManager(), "");
            }
            else if(lLastWarnedID != affectedPlayer.GetID() && game.GetNetWorthMultiplier(game.GetOurPlayer(), affectedPlayer) > Defs.ELITE_WARNING)
            {
                final LaunchDialog launchDialog = new LaunchDialog();
                launchDialog.SetHeaderLaunch();
                launchDialog.SetMessage(context.getString(R.string.attacking_elite, affectedPlayer.GetName()));
                launchDialog.SetOnClickYes(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        launchDialog.dismiss();
                        lLastWarnedID = affectedPlayer.GetID();
                        Launch();
                    }
                });
                launchDialog.SetOnClickNo(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        launchDialog.dismiss();
                    }
                });
                launchDialog.show(activity.getFragmentManager(), "");
            }
            else
            {
                Launch();
            }
        }
        else
        {
            Launch();
        }
    }

    private void Launch()
    {
        //LAUNCH!
        activity.InformationMode();
        if (bFromPlayer) {
            game.LaunchMissilePlayer(cSlotNo, bTrackPlayer, geoTarget, lTrackPlayerID);
        } else {
            game.LaunchMissile(lSiteID, cSlotNo, bTrackPlayer, geoTarget, lTrackPlayerID);
        }
    }

    public void LocationSelected(GeoCoord geoLocation, Polyline targetTrajectory, GoogleMap map)
    {
        geoTarget = geoLocation;
        this.targetTrajectory = targetTrajectory;
        this.map = map;
        bTrackPlayer = false;

        if(missileType.GetTracking())
        {
            //Target selected?
            for (Player player : game.GetPlayers())
            {
                if(geoLocation.DistanceTo(player.GetPosition()) < ClientDefs.TRACK_THRESHOLD)
                {
                    bTrackPlayer = true;
                    lTrackPlayerID = player.GetID();
                    targetTrajectory.setColor(Color.argb(255, 255, 128, 0));
                }
            }
        }

        //Spawn a thread to check for friendly fire, so as not to hang the UI thread.
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                final boolean bThreatensFriendlies = game.ThreatensFriendlies(game.GetOurPlayerID(), geoTarget, missileType, false, false);

                activity.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        txtFriendlyFire.setVisibility(bThreatensFriendlies ? VISIBLE : GONE);
                    }
                });
            }
        }).start();

        Update();
    }

    @Override
    public void Update()
    {
        //TO DO: Kill the UI if anything happens?

        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if(geoTarget == null)
                {
                    txtInstructions.setVisibility(VISIBLE);
                    txtFlightTime.setVisibility(GONE);
                }
                else
                {
                    GeoCoord geoFrom = bFromPlayer ? game.GetOurPlayer().GetPosition() : game.GetMissileSite(lSiteID).GetPosition();
                    float fltDistance = geoFrom.DistanceTo(geoTarget);

                    txtFlightTime.setText(context.getString(R.string.flight_time_target, TextUtilities.GetTimeAmount(game.GetTimeToTarget(geoFrom, geoTarget, game.GetConfig().GetMissileSpeed(missileType.GetSpeedIndex())))));
                    txtInstructions.setVisibility(GONE);
                    txtFlightTime.setVisibility(VISIBLE);
                    txtOutOfRange.setVisibility(fltDistance > game.GetConfig().GetMissileRange(missileType.GetRangeIndex()) ? VISIBLE : GONE);
                    imgTracking.setVisibility(bTrackPlayer ? VISIBLE : GONE);

                    //Update trajectory.
                    List<LatLng> points = new ArrayList<LatLng>();
                    points.add(Utilities.GetLatLng(bFromPlayer ? game.GetOurPlayer().GetPosition() : game.GetMissileSite(lSiteID).GetPosition()));

                    if(bTrackPlayer)
                    {
                        geoTarget = game.GetPlayer(lTrackPlayerID).GetPosition();
                    }

                    points.add(Utilities.GetLatLng(geoTarget));

                    targetTrajectory.setPoints(points);
                }
            }
        });
    }
}
