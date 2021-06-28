package com.apps.fast.launch.launchviews.entities;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.TextUtilities;
import com.apps.fast.launch.launchviews.LaunchView;
import com.apps.fast.launch.views.DistancedEntityView;
import com.apps.fast.launch.views.EntityControls;

import java.util.List;

import launch.game.LaunchClientGame;
import launch.game.entities.*;
import launch.game.types.MissileType;

/**
 * Created by tobster on 09/11/15.
 */
public class MissileView extends LaunchView
{
    private int lMissileID;

    private TextView txtToTarget;
    private TextView txtNoTargets;
    private LinearLayout btnLaunchInterceptor;
    private TextView txtSpeed;
    private TextView txtRange;
    private TextView txtBlastRadius;
    private TextView txtMaxDamage;

    private ImageView imgMissile;

    public MissileView(LaunchClientGame game, MainActivity activity, int lMissileID)
    {
        super(game, activity, true);
        this.lMissileID = lMissileID;
        Setup();
    }

    @Override
    protected void Setup()
    {
        final Missile missile = game.GetMissile(lMissileID);

        if(missile != null)
        {
            MissileType missileType = game.GetConfig().GetMissileType(missile.GetType());

            if(missileType != null)
            {
                inflate(context, R.layout.view_missile, this);
                ((EntityControls)findViewById(R.id.entityControls)).SetActivity(activity);

                ((TextView) findViewById(R.id.txtMissileTitle)).setText(TextUtilities.GetOwnedEntityName(missile, game));

                txtToTarget = (TextView) findViewById(R.id.txtToTarget);
                txtNoTargets = (TextView) findViewById(R.id.txtNoTargets);
                btnLaunchInterceptor = (LinearLayout) findViewById(R.id.btnLaunchInterceptor);
                txtSpeed = (TextView) findViewById(R.id.txtSpeed);
                txtRange = (TextView) findViewById(R.id.txtRange);
                txtBlastRadius = (TextView) findViewById(R.id.txtBlastRadius);
                txtMaxDamage = (TextView) findViewById(R.id.txtMaxDamage);

                btnLaunchInterceptor.setOnClickListener(new OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        activity.InterceptorSelectForTarget(lMissileID, TextUtilities.GetOwnedEntityName(missile, game));
                    }
                });

                imgMissile = (ImageView) findViewById(R.id.imgMissile);
                imgMissile.setImageResource(missileType.GetNuclear() ? R.drawable.marker_missilenuke : R.drawable.marker_missile);

                txtSpeed.setText(TextUtilities.GetSpeedFromKph(game.GetConfig().GetMissileSpeed(missileType)));
                txtRange.setText(TextUtilities.GetDistanceStringFromKM(game.GetConfig().GetMissileRange(missileType)));
                txtBlastRadius.setText(TextUtilities.GetDistanceStringFromKM(game.GetConfig().GetBlastRadius(missileType)));
                txtMaxDamage.setText(TextUtilities.GetDamageString(game.GetConfig().GetMissileMaxDamage(missileType)));

                Update();
            }
            else
            {
                Finish(true);
            }
        }
        else
        {
            Finish(true);
        }
    }

    @Override
    public void Update()
    {
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                Missile missile = game.GetMissile(lMissileID);

                if(missile != null)
                {
                    MissileType missileType = game.GetConfig().GetMissileType(missile.GetType());

                    if(missileType != null)
                    {
                        txtToTarget.setText(context.getString(R.string.missile_to_target, TextUtilities.GetTimeAmount(game.GetTimeToTarget(missile))));

                        //Create list of potential targets.
                        List<Damagable> Targets = game.GetNearbyDamagables(game.GetMissileTarget(missile), game.GetConfig().GetBlastRadius(missileType));

                        if (Targets.size() > 0)
                        {
                            txtNoTargets.setVisibility(GONE);
                            LinearLayout lytTargets = (LinearLayout) findViewById(R.id.lytTargets);

                            lytTargets.removeAllViews();

                            for (final Damagable entity : Targets)
                            {
                                DistancedEntityView nev = new DistancedEntityView(context, activity, entity, missile.GetPosition(), game);

                                nev.setOnClickListener(new View.OnClickListener()
                                {
                                    @Override
                                    public void onClick(View view)
                                    {
                                        activity.SelectEntity(entity);
                                    }
                                });

                                lytTargets.addView(nev);
                            }
                        }
                        else
                        {
                            txtNoTargets.setVisibility(VISIBLE);
                        }

                        btnLaunchInterceptor.setVisibility(game.GetOurPlayer().Functioning() ? VISIBLE : GONE);
                    }
                    else
                    {
                        Finish(true);
                    }
                }
                else
                {
                    Finish(true);
                }
            }
        });
    }
}
