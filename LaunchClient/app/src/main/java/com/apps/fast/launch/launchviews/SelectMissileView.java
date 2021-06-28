package com.apps.fast.launch.launchviews;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.Utilities;
import com.apps.fast.launch.views.LaunchableSelectionView;

import java.util.ArrayList;
import java.util.List;

import launch.game.GeoCoord;
import launch.game.LaunchClientGame;
import launch.game.entities.MissileSite;
import launch.game.entities.Player;
import launch.game.systems.MissileSystem;
import launch.game.types.MissileType;

/**
 * Created by tobster on 16/10/15.
 */
public class SelectMissileView extends LaunchView
{
    private GeoCoord geoTarget;
    private LinearLayout lytExistingSites;
    private String strTargetName;

    private TextView txtNoMissiles;

    public SelectMissileView(LaunchClientGame game, MainActivity activity, GeoCoord geoTarget, String strTargetName)
    {
        super(game, activity, true);

        this.geoTarget = geoTarget;
        this.strTargetName = strTargetName;

        Setup();
        //Update();
        RebuildList();
    }

    @Override
    protected void Setup()
    {
        inflate(context, R.layout.view_select_missile, this);

        ((TextView)findViewById(R.id.txtAttacking)).setText(context.getString(R.string.attacking, strTargetName));

        txtNoMissiles = (TextView)findViewById(R.id.txtNoMissiles);

        //Existing missile sites.
        lytExistingSites = (LinearLayout)findViewById(R.id.lytAvailableMissiles);
    }

    @Override
    public void Update()
    {
        //Removed due to performance issues. TO DO: Respond only to changes that affect the entries.
        //RebuildList()
    }

    private void RebuildList()
    {
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                lytExistingSites.removeAllViews();

                boolean bMissilesAvailable = false;

                Player ourPlayer = game.GetOurPlayer();

                if(ourPlayer.GetHasCruiseMissileSystem())
                {
                    if(AddUIForSystem(ourPlayer.GetPosition(), ourPlayer.GetMissileSystem(), true, 0))
                    {
                        bMissilesAvailable = true;
                    }
                }

                for(final MissileSite missileSite : game.GetMissileSites())
                {
                    if((missileSite.GetOwnerID() == ourPlayer.GetID()) && missileSite.GetOnline())
                    {
                        if(AddUIForSystem(missileSite.GetPosition(), missileSite.GetMissileSystem(), false, missileSite.GetID()))
                        {
                            bMissilesAvailable = true;
                        }
                    }
                }

                txtNoMissiles.setVisibility(bMissilesAvailable ? GONE : VISIBLE);
            }
        });
    }

    private boolean AddUIForSystem(GeoCoord geoFrom, MissileSystem system, final boolean bIsPlayer, final int lMissileSiteID)
    {
        TextView textSiteName = new TextView(context);
        textSiteName.setText(bIsPlayer ? game.GetOurPlayer().GetName() : Utilities.GetStructureName(context, game.GetMissileSite(lMissileSiteID)));
        lytExistingSites.addView(textSiteName);

        List<Byte> SuitableTypes = new ArrayList<>();

        boolean bMissilesAvailable = false;

        for(byte c = 0; c < system.GetSlotCount(); c++)
        {
            if(system.GetSlotReadyToFire(c))
            {
                byte cType = system.GetSlotMissileType(c);

                if(!SuitableTypes.contains(cType))
                {
                    SuitableTypes.add(cType);

                    MissileType type = game.GetConfig().GetMissileType(system.GetSlotMissileType(c));

                    //Check it's in range.
                    if(geoFrom.DistanceTo(geoTarget) <= game.GetConfig().GetMissileRange(type.GetRangeIndex()))
                    {
                        bMissilesAvailable = true;
                        LaunchableSelectionView launchableSelectionView = new LaunchableSelectionView(activity, game, type, geoTarget, geoFrom);

                        //Count number of this type that are ready.
                        int lNumber = 0;
                        for(byte cSlot = 0; cSlot < system.GetSlotCount(); cSlot++)
                        {
                            if (system.GetSlotReadyToFire(cSlot))
                                if(system.GetSlotMissileType(cSlot) == cType)
                                    lNumber++;
                        }
                        launchableSelectionView.SetNumber(lNumber);

                        final byte cSlotNo = c;

                        launchableSelectionView.setOnClickListener(new OnClickListener()
                        {
                            @Override
                            public void onClick(View view)
                            {
                                if(bIsPlayer)
                                    activity.DesignateMissileTargetPlayer(cSlotNo, geoTarget);
                                else
                                    activity.DesignateMissileTarget(lMissileSiteID, cSlotNo, geoTarget);
                            }
                        });

                        lytExistingSites.addView(launchableSelectionView);
                    }
                }
            }
        }

        if(!bMissilesAvailable)
        {
            TextView textNone = new TextView(context);
            textNone.setText(context.getString(R.string.none));
            textNone.setTextColor(Utilities.ColourFromAttr(context, R.attr.BadColour));
            lytExistingSites.addView(textNone);
        }

        return bMissilesAvailable;
    }
}
