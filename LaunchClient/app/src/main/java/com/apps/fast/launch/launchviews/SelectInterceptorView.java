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
import launch.game.entities.Missile;
import launch.game.entities.Player;
import launch.game.entities.SAMSite;
import launch.game.systems.MissileSystem;
import launch.game.types.InterceptorType;
import launch.game.types.MissileType;

/**
 * Created by tobster on 16/10/15.
 */
public class SelectInterceptorView extends LaunchView
{
    private int lTargetMissile;
    private LinearLayout lytExistingSites;
    private String strTargetName;

    private TextView txtNoMissiles;

    public SelectInterceptorView(LaunchClientGame game, MainActivity activity, int lTargetMissile, String strTargetName)
    {
        super(game, activity, true);

        this.lTargetMissile = lTargetMissile;
        this.strTargetName = strTargetName;

        Setup();
        //Update();
        RebuildList();
    }

    @Override
    protected void Setup()
    {
        inflate(context, R.layout.view_select_interceptor, this);

        ((TextView)findViewById(R.id.txtAttacking)).setText(context.getString(R.string.engaging, strTargetName));

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

                boolean bInterceptorsAvailable = false;
                final Missile missile = game.GetMissile(lTargetMissile);
                MissileType missileType = game.GetConfig().GetMissileType(missile.GetType());
                GeoCoord geoMissileTarget = game.GetMissileTarget(missile);

                Player ourPlayer = game.GetOurPlayer();

                if(ourPlayer.GetHasAirDefenceSystem())
                {
                    MissileSystem missileSystem = ourPlayer.GetInterceptorSystem();

                    if(AddUIForSystem(ourPlayer.GetPosition(), missile, missileType, geoMissileTarget, missileSystem, true, 0))
                    {
                        bInterceptorsAvailable = true;
                    }
                }

                for(final SAMSite samSite : game.GetSAMSites())
                {
                    if((samSite.GetOwnerID() == ourPlayer.GetID()) && samSite.GetOnline())
                    {
                        MissileSystem missileSystem = samSite.GetInterceptorSystem();

                        if(AddUIForSystem(samSite.GetPosition(), missile, missileType, geoMissileTarget, missileSystem, false, samSite.GetID()))
                        {
                            bInterceptorsAvailable = true;
                        }
                    }
                }

                txtNoMissiles.setVisibility(bInterceptorsAvailable ? GONE : VISIBLE);
            }
        });
    }

    private boolean AddUIForSystem(GeoCoord geoFrom, final Missile missile, MissileType missileType, GeoCoord geoMissileTarget, MissileSystem system, final boolean bIsPlayer, final int lSAMSiteID)
    {
        TextView textSiteName = new TextView(context);
        textSiteName.setText(bIsPlayer ? game.GetOurPlayer().GetName() : Utilities.GetStructureName(context, game.GetSAMSite(lSAMSiteID)));
        lytExistingSites.addView(textSiteName);

        List<Byte> SuitableTypes = new ArrayList<>();

        boolean bInterceptorsAvailable = false;

        for(byte c = 0; c < system.GetSlotCount(); c++)
        {
            if(system.GetSlotReadyToFire(c))
            {
                byte cType = system.GetSlotMissileType(c);

                if(!SuitableTypes.contains(cType))
                {
                    SuitableTypes.add(cType);
                    InterceptorType type = game.GetConfig().GetInterceptorType(cType);

                    //Check it's in range.
                    if (geoFrom.DistanceTo(missile.GetPosition()) <= game.GetConfig().GetInterceptorRange(type.GetRangeIndex()))
                    {
                        GeoCoord geoIntercept = missile.GetPosition().InterceptPoint(geoMissileTarget, game.GetConfig().GetMissileSpeed(missileType.GetSpeedIndex()), geoFrom, game.GetConfig().GetInterceptorSpeed(type.GetSpeedIndex()));
                        long oTimeToIntercept = game.GetTimeToTarget(geoFrom, geoIntercept, game.GetConfig().GetInterceptorSpeed(type.GetSpeedIndex()));

                        //Check it can get to it in time and is fast enough.
                        if (oTimeToIntercept < game.GetTimeToTarget(missile) && !game.GetInterceptorTooSlow(type.GetID(), missileType.GetID()))
                        {
                            bInterceptorsAvailable = true;

                            final LaunchableSelectionView interceptorSelectionView = new LaunchableSelectionView(activity, game, type, oTimeToIntercept);

                            //Count number of this type that are ready.
                            int lNumber = 0;
                            for(byte cSlot = 0; cSlot < system.GetSlotCount(); cSlot++)
                            {
                                if (system.GetSlotReadyToFire(cSlot))
                                    if(system.GetSlotMissileType(cSlot) == cType)
                                        lNumber++;
                            }
                            interceptorSelectionView.SetNumber(lNumber);

                            final byte cSlotNo = c;

                            interceptorSelectionView.setOnClickListener(new OnClickListener()
                            {
                                @Override
                                public void onClick(View view)
                                {
                                    if(bIsPlayer)
                                        activity.DesignateInterceptorTargetPlayer(cSlotNo, missile);
                                    else
                                        activity.DesignateInterceptorTarget(lSAMSiteID, cSlotNo, missile);
                                }
                            });

                            lytExistingSites.addView(interceptorSelectionView);
                        }
                    }
                }
            }
        }

        if(!bInterceptorsAvailable)
        {
            TextView textNone = new TextView(context);
            textNone.setText(context.getString(R.string.none));
            textNone.setTextColor(Utilities.ColourFromAttr(context, R.attr.BadColour));
            lytExistingSites.addView(textNone);
        }

        return bInterceptorsAvailable;
    }
}
