package com.apps.fast.launch.launchviews;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.views.DistancedEntityView;
import com.apps.fast.launch.views.EntityControls;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import launch.game.GeoCoord;
import launch.game.LaunchClientGame;
import launch.game.entities.LaunchEntity;
import launch.game.entities.SentryGun;
import launch.game.entities.Structure;

/**
 * Created by tobster on 20/04/20.
 */
public class MapSelectView extends LaunchView
{
    private TextView txtCalculating;
    private LinearLayout lytGroups;
    private LinearLayout lytEntities;

    private GeoCoord geoFrom;
    private GeoCoord geoTo;

    //Thread interruption, to prevent crashes if the user gets bored and dismisses the view.
    private boolean bCanInterruptSetupThread = false;
    private Thread setupThread = null;

    public MapSelectView(LaunchClientGame game, MainActivity activity, LatLng from, LatLng to)
    {
        super(game, activity, true);
        geoFrom = new GeoCoord(from.latitude, from.longitude, true);
        geoTo = new GeoCoord(to.latitude, to.longitude, true);;
        Setup();
    }

    @Override
    protected void Setup()
    {
        inflate(context, R.layout.view_map_select, this);
        ((EntityControls)findViewById(R.id.entityControls)).SetActivity(activity);

        txtCalculating = findViewById(R.id.txtCalculating);
        lytGroups = findViewById(R.id.lytGroups);
        lytEntities = findViewById(R.id.lytEntities);

        //Spark up the comparisons on another thread as they're a bit intensive.
        setupThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                final List<SentryGun> OurSentries = new ArrayList<>();
                final List<SentryGun> OurSAMs = new ArrayList<>();
                final List<SentryGun> OurMissileSites = new ArrayList<>();
                final List<SentryGun> OurOreMines = new ArrayList<>();
                final List<LaunchEntity> EverythingElse = new ArrayList<>();

                FillPlayerOrEverythingElseContainer(game.GetSentryGuns(), OurSentries, EverythingElse);
                FillPlayerOrEverythingElseContainer(game.GetSAMSites(), OurSAMs, EverythingElse);
                FillPlayerOrEverythingElseContainer(game.GetMissileSites(), OurMissileSites, EverythingElse);
                FillPlayerOrEverythingElseContainer(game.GetOreMines(), OurOreMines, EverythingElse);
                FillEverythingElseContainer(game.GetPlayers(), EverythingElse);
                FillEverythingElseContainer(game.GetMissiles(), EverythingElse);
                FillEverythingElseContainer(game.GetInterceptors(), EverythingElse);
                FillEverythingElseContainer(game.GetLoots(), EverythingElse);

                //Containers complete, onto UI, where we now cannot interrupt this thread.
                bCanInterruptSetupThread = false;

                activity.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        txtCalculating.setVisibility(GONE);

                        if(OurSentries.size() > 0)
                            lytGroups.addView(new StructureMaintenanceView(game, activity, OurSentries));
                        if(OurSAMs.size() > 0)
                            lytGroups.addView(new StructureMaintenanceView(game, activity, OurSAMs));
                        if(OurMissileSites.size() > 0)
                            lytGroups.addView(new StructureMaintenanceView(game, activity, OurMissileSites));
                        if(OurOreMines.size() > 0)
                            lytGroups.addView(new StructureMaintenanceView(game, activity, OurOreMines));

                        for(final LaunchEntity entity : EverythingElse)
                        {
                            DistancedEntityView nev = new DistancedEntityView(context, activity, entity, game);

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
        });

        bCanInterruptSetupThread = true;
        setupThread.start();
    }

    public void FillPlayerOrEverythingElseContainer(Collection Structures, List OurContainer, List EverythingElse)
    {
        for(Object object : Structures)
        {
            Structure structure = (Structure)object;

            if(structure.GetPosition().IsInsideGeoRect(geoFrom, geoTo))
            {
                if(structure.GetOwnedBy(game.GetOurPlayerID()))
                    OurContainer.add(structure);
                else
                    EverythingElse.add(structure);
            }
        }
    }

    public void FillEverythingElseContainer(Collection Structures, List EverythingElse)
    {
        for(Object object : Structures)
        {
            LaunchEntity entity = (LaunchEntity)object;

            if(entity.GetPosition().IsInsideGeoRect(geoFrom, geoTo))
            {
                EverythingElse.add(entity);
            }
        }
    }

    @Override
    public void Update()
    {
    }

    @Override
    protected void Finish(boolean bClearSelectedEntity)
    {
        super.Finish(bClearSelectedEntity);

        if(bCanInterruptSetupThread)
        {
            if(setupThread.isAlive())
                setupThread.stop();
        }
    }

    @Override
    public void EntityUpdated(LaunchEntity entity)
    {
        for(int i = 0; i < lytGroups.getChildCount(); i++)
        {
            View view = lytGroups.getChildAt(i);

            if(view instanceof StructureMaintenanceView)
            {
                ((StructureMaintenanceView) view).EntityUpdated(entity);
            }
        }
    }
}
