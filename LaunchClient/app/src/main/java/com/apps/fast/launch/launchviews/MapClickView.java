package com.apps.fast.launch.launchviews;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.ClientDefs;
import com.apps.fast.launch.components.TextUtilities;
import com.apps.fast.launch.views.DistancedEntityView;
import com.apps.fast.launch.views.EntityControls;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;

import launch.game.GeoCoord;
import launch.game.LaunchClientGame;
import launch.game.entities.LaunchEntity;
import launch.game.entities.Radiation;

/**
 * Created by tobster on 09/11/15.
 */
public class MapClickView extends LaunchView
{
    private GeoCoord geoClick;
    private TextView txtRadioactive;
    private TextView txtCalculating;
    private LinearLayout lytEntities;

    private LinearLayout btnAttack;

    public MapClickView(LaunchClientGame game, MainActivity activity, LatLng latLng)
    {
        super(game, activity, true);
        geoClick = new GeoCoord(latLng.latitude, latLng.longitude, true);
        Setup();
    }

    @Override
    protected void Setup()
    {
        inflate(context, R.layout.view_map_click, this);
        ((EntityControls)findViewById(R.id.entityControls)).SetActivity(activity);

        txtRadioactive = findViewById(R.id.txtRadioactive);
        txtCalculating = findViewById(R.id.txtCalculating);
        lytEntities = findViewById(R.id.lytEntities);
        btnAttack =  findViewById(R.id.btnAttack);

        btnAttack.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if(!game.GetInteractionReady())
                {
                    activity.ShowBasicOKDialog(context.getString(R.string.waiting_for_data));
                }
                else
                {
                    activity.MissileSelectForTarget(geoClick, TextUtilities.GetLatLongString(geoClick.GetLatitude(), geoClick.GetLongitude()));
                }
            }
        });

        ((TextView)findViewById(R.id.txtPlayerJoins)).setText(TextUtilities.GetLatLongString(geoClick.GetLatitude(), geoClick.GetLongitude()));

        txtRadioactive.setVisibility(GONE);

        //Spark up the comparisons on another thread as they're a bit intensive.
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                int lRadioactivityRemaining = 0;

                for(Radiation radiation : game.GetRadiations())
                {
                    if(geoClick.DistanceTo(radiation.GetPosition()) <= radiation.GetRadius())
                    {
                        lRadioactivityRemaining = Math.max(lRadioactivityRemaining, radiation.GetExpiryTime());
                    }
                }

                final int lRadioactivityFinal = lRadioactivityRemaining;
                final List<LaunchEntity> Nearest = game.GetNearestEntities(geoClick, ClientDefs.NEAREST_ENTITY_COUNT);

                //Populate the layout.
                activity.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if(lRadioactivityFinal > 0)
                        {
                            txtRadioactive.setVisibility(VISIBLE);
                            txtRadioactive.setText(context.getString(R.string.radioactive, TextUtilities.GetFutureTime(lRadioactivityFinal)));
                        }

                        txtCalculating.setVisibility(GONE);

                        for(final LaunchEntity entity : Nearest)
                        {
                            DistancedEntityView nev = new DistancedEntityView(context, activity, entity, geoClick, game);

                            nev.setOnClickListener(new OnClickListener()
                            {
                                @Override
                                public void onClick(View view)
                                {
                                    activity.SelectEntity(entity);
                                }
                            });

                            lytEntities.addView(nev);
                        }
                    }
                });
            }
        }).start();
    }

    @Override
    public void Update()
    {
    }
}
