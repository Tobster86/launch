package com.apps.fast.launch.launchviews;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.TextUtilities;
import com.apps.fast.launch.components.Utilities;
import com.apps.fast.launch.views.LaunchDialog;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;

import java.util.ArrayList;
import java.util.List;

import launch.game.GeoCoord;
import launch.game.LaunchClientGame;
import launch.game.entities.Missile;
import launch.game.types.InterceptorType;
import launch.game.types.MissileType;

/**
 * Created by tobster on 14/07/16.
 */
public class BottomInterceptorTarget extends LaunchView
{
    private TextView txtInstructions;
    private TextView txtOutOfRange;
    private TextView txtTooSlow;
    private TextView txtInterceptorName;
    private TextView txtFlightTime;
    private TextView txtFriendlyFire;
    private LinearLayout btnFire;
    private LinearLayout btnCancel;

    private boolean bFromPlayer;
    private int lSiteID;
    private byte cSlotNo;
    private InterceptorType interceptorType;

    private Missile target = null;
    private Polyline targetTrajectory;

    //From marker_player.
    public BottomInterceptorTarget(LaunchClientGame game, MainActivity activity, byte cSlotNo)
    {
        super(game, activity, true);
        bFromPlayer = true;
        this.cSlotNo = cSlotNo;
        interceptorType = game.GetConfig().GetInterceptorType(game.GetOurPlayer().GetInterceptorSystem().GetSlotMissileType(cSlotNo));
        Setup();
    }

    //From missile site.
    public BottomInterceptorTarget(LaunchClientGame game, MainActivity activity, int lSiteID, byte cSlotNo)
    {
        super(game, activity, true);
        bFromPlayer = false;
        this.lSiteID = lSiteID;
        this.cSlotNo = cSlotNo;
        interceptorType = game.GetConfig().GetInterceptorType(game.GetSAMSite(lSiteID).GetInterceptorSystem().GetSlotMissileType(cSlotNo));
        Setup();
    }

    @Override
    protected void Setup()
    {
        inflate(context, R.layout.bottom_interceptor_target, this);

        txtInstructions = (TextView)findViewById(R.id.txtInstructions);
        txtOutOfRange = (TextView)findViewById(R.id.txtOutOfRange);
        txtTooSlow = (TextView)findViewById(R.id.txtTooSlow);
        txtInterceptorName = (TextView)findViewById(R.id.txtInterceptorName);
        txtFlightTime = (TextView)findViewById(R.id.txtFlightTime);
        txtFriendlyFire = (TextView)findViewById(R.id.txtFriendlyFire);
        btnFire = (LinearLayout)findViewById(R.id.btnFire);
        btnCancel = (LinearLayout)findViewById(R.id.btnCancel);

        txtInterceptorName.setText(interceptorType.GetName());

        btnFire.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if(target == null)
                {
                    //Target not selected.
                    activity.ShowBasicOKDialog(context.getString(R.string.must_specify_target_missile));
                }
                else
                {
                    MissileType missileType = game.GetConfig().GetMissileType(target.GetType());

                    if (game.GetConfig().GetInterceptorSpeed(interceptorType.GetSpeedIndex()) <= game.GetConfig().GetMissileSpeed(missileType.GetSpeedIndex()))
                    {
                        //Cannot intercept as the target missile is faster.
                        activity.ShowBasicOKDialog(context.getString(R.string.cannot_intercept));
                    }
                    else
                    {
                        GeoCoord geoFrom = bFromPlayer ? game.GetOurPlayer().GetPosition() : game.GetSAMSite(lSiteID).GetPosition();
                        float fltDistance = geoFrom.DistanceTo(target.GetPosition());

                        if (fltDistance > game.GetConfig().GetInterceptorRange(interceptorType.GetRangeIndex()))
                        {
                            //Cannot intercept.
                            activity.ShowBasicOKDialog(context.getString(R.string.cannot_intercept));
                        } else if (game.GetOurPlayer().GetRespawnProtected())
                        {
                            final LaunchDialog launchDialog = new LaunchDialog();
                            launchDialog.SetHeaderLaunch();
                            launchDialog.SetMessage(context.getString(R.string.interceptor_respawn_protected_you, TextUtilities.GetTimeAmount(game.GetOurPlayer().GetStateTimeRemaining())));
                            launchDialog.SetOnClickYes(new View.OnClickListener()
                            {
                                @Override
                                public void onClick(View view)
                                {
                                    launchDialog.dismiss();
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
                        } else
                        {
                            Launch();
                        }
                    }
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

    private void Launch()
    {
        //LAUNCH!
        activity.InformationMode();
        if(bFromPlayer)
        {
            game.LaunchInterceptorPlayer(cSlotNo, target.GetID());
        }
        else
        {
            game.LaunchInterceptor(lSiteID, cSlotNo, target.GetID());
        }
    }

    public void TargetSelected(Missile target, Polyline targetTrajectory, GoogleMap map)
    {
        this.target = target;
        this.targetTrajectory = targetTrajectory;
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

                if(target == null)
                {
                    txtInstructions.setVisibility(VISIBLE);
                    txtFlightTime.setVisibility(GONE);
                }
                else
                {
                    MissileType missileType = game.GetConfig().GetMissileType(target.GetType());
                    GeoCoord geoMissileTarget = game.GetMissileTarget(target);
                    GeoCoord geoFrom = bFromPlayer ? game.GetOurPlayer().GetPosition() : game.GetSAMSite(lSiteID).GetPosition();
                    GeoCoord geoIntercept = target.GetPosition().InterceptPoint(geoMissileTarget, game.GetConfig().GetMissileSpeed(missileType.GetSpeedIndex()), geoFrom, game.GetConfig().GetInterceptorSpeed(interceptorType.GetSpeedIndex()));
                    long oInterceptTime = game.GetTimeToTarget(geoFrom, geoIntercept, game.GetConfig().GetInterceptorSpeed(interceptorType.GetSpeedIndex()));
                    float fltDistance = geoFrom.DistanceTo(target.GetPosition());

                    txtFlightTime.setText(context.getString(R.string.flight_time_target, TextUtilities.GetTimeAmount(oInterceptTime)));
                    txtInstructions.setVisibility(GONE);
                    txtFlightTime.setVisibility(VISIBLE);
                    txtOutOfRange.setVisibility(fltDistance > game.GetConfig().GetInterceptorRange(interceptorType.GetRangeIndex()) ? VISIBLE : GONE);
                    txtTooSlow.setVisibility(game.GetInterceptorTooSlow(interceptorType.GetID(), target.GetType())? VISIBLE : GONE);
                    txtFriendlyFire.setVisibility(game.GetOurPlayerID() == target.GetOwnerID() ? VISIBLE : GONE);

                    //Update trajectory.
                    List<LatLng> points = new ArrayList<LatLng>();
                    points.add(Utilities.GetLatLng(bFromPlayer ? game.GetOurPlayer().GetPosition() : game.GetSAMSite(lSiteID).GetPosition()));
                    points.add(Utilities.GetLatLng(target.GetPosition()));
                    targetTrajectory.setPoints(points);
                }
            }
        });
    }
}
