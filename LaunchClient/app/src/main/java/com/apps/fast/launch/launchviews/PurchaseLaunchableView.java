package com.apps.fast.launch.launchviews;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.ClientDefs;
import com.apps.fast.launch.components.TextUtilities;
import com.apps.fast.launch.views.LaunchDialog;
import com.apps.fast.launch.views.LaunchableSelectionView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import launch.game.Config;
import launch.game.LaunchClientGame;
import launch.game.entities.MissileSite;
import launch.game.entities.SAMSite;
import launch.game.systems.MissileSystem;
import launch.game.types.InterceptorType;
import launch.game.types.MissileType;

/**
 * Created by tobster on 16/10/15.
 */
public class PurchaseLaunchableView extends LaunchView
{
    private LinearLayout lytTypes;

    private boolean bMissiles;
    private boolean bFittedToPlayer;
    private int lFittedToStructureID;
    private byte cSlotNumber;

    private int lTotalCost = 0;

    private List<Byte> Types = new ArrayList<>();
    private LinearLayout btnPurchase;
    private TextView txtCount;
    private TextView txtTotalCost;

    private Map<Byte, LaunchableSelectionView> LaunchableSelectionViews = new HashMap<>();

    //For a missile site.
    public PurchaseLaunchableView(LaunchClientGame game, MainActivity activity, MissileSite site, byte cSlotNumber)
    {
        super(game, activity, true);

        lFittedToStructureID = site.GetID();
        this.cSlotNumber = cSlotNumber;

        bMissiles = true;
        bFittedToPlayer = false;
        Setup();
        SetupMissiles();

        UpdateSelections();
    }

    //For a SAM site.
    public PurchaseLaunchableView(LaunchClientGame game, MainActivity activity, SAMSite site, byte cSlotNumber)
    {
        super(game, activity, true);

        lFittedToStructureID = site.GetID();
        this.cSlotNumber = cSlotNumber;

        bMissiles = false;
        bFittedToPlayer = false;
        Setup();
        SetupInterceptors();

        UpdateSelections();
    }

    //For a player system.
    public PurchaseLaunchableView(LaunchClientGame game, MainActivity activity, boolean bMissiles, byte cSlotNumber)
    {
        super(game, activity, true);

        this.cSlotNumber = cSlotNumber;

        this.bMissiles = bMissiles;
        bFittedToPlayer = true;
        Setup();
        if(bMissiles)
            SetupMissiles();
        else
            SetupInterceptors();

        UpdateSelections();
    }

    @Override
    protected void Setup()
    {
        inflate(context, R.layout.activity_purchase_launchable, this);

        lytTypes = (LinearLayout)findViewById(R.id.lytTypes);

        btnPurchase = (LinearLayout)findViewById(R.id.btnPurchase);
        txtCount = (TextView)findViewById(R.id.txtCount);
        txtTotalCost = (TextView)findViewById(R.id.txtTotalCost);

        btnPurchase.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                final LaunchDialog launchDialog = new LaunchDialog();
                launchDialog.SetHeaderPurchase();
                launchDialog.SetMessage(context.getString(R.string.purchase_confirm, TextUtilities.GetCurrencyString(lTotalCost)));
                launchDialog.SetOnClickYes(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        launchDialog.dismiss();

                        final byte[] cTypes = new byte[Types.size()];
                        boolean bNukeRespawnCheck = false;

                        for(int i = 0; i < Types.size(); i++)
                        {
                            cTypes[i] = Types.get(i);

                            if(bMissiles)
                            {
                                if(game.GetConfig().GetMissileType(cTypes[i]).GetNuclear())
                                    bNukeRespawnCheck = true;
                            }
                        }

                        if(bFittedToPlayer)
                        {
                            if(bMissiles)
                                game.PurchaseMissilesPlayer(cSlotNumber, cTypes);
                            else
                                game.PurchaseInterceptorsPlayer(cSlotNumber, cTypes);
                        }
                        else
                        {
                            if(bMissiles)
                            {
                                if(bNukeRespawnCheck && game.GetOurPlayer().GetRespawnProtected())
                                {
                                    final LaunchDialog launchDialogNuke = new LaunchDialog();
                                    launchDialogNuke.SetHeaderLaunch();
                                    launchDialogNuke.SetMessage(context.getString(R.string.nuke_respawn_protected_you, TextUtilities.GetTimeAmount(game.GetOurPlayer().GetStateTimeRemaining())));
                                    launchDialogNuke.SetOnClickYes(new View.OnClickListener()
                                    {
                                        @Override
                                        public void onClick(View view)
                                        {
                                            launchDialogNuke.dismiss();
                                            game.PurchaseMissiles(lFittedToStructureID, cSlotNumber, cTypes);
                                        }
                                    });
                                    launchDialogNuke.SetOnClickNo(new View.OnClickListener()
                                    {
                                        @Override
                                        public void onClick(View view)
                                        {
                                            launchDialogNuke.dismiss();
                                        }
                                    });
                                    launchDialogNuke.show(activity.getFragmentManager(), "");
                                }
                                else
                                {
                                    game.PurchaseMissiles(lFittedToStructureID, cSlotNumber, cTypes);
                                }
                            }
                            else
                                game.PurchaseInterceptors(lFittedToStructureID, cSlotNumber, cTypes);
                        }

                        //TO DO: Improve? Currently sets the first pressed type as the preferred missile.
                        if(bMissiles)
                            ClientDefs.SetMissilePreferred(cTypes[0]);
                        else
                            ClientDefs.SetInterceptorPreferred(cTypes[0]);

                        ReturnToParentView();
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
        });
    }

    private void SetupMissiles()
    {
        findViewById(R.id.imgInterceptor).setVisibility(GONE);

        //Put up to three preferred ones at the top.
        List<Byte> MissilePreferences = ClientDefs.GetMissilePreferredOrder(context);
        ArrayList<MissileType> MissileTypes = new ArrayList<>();
        for(Byte cPreferredMissile : MissilePreferences)
        {
            MissileType type = game.GetConfig().GetMissileType(cPreferredMissile);

            if(type != null)
            {
                if(type.GetPurchasable())
                {
                    MissileTypes.add(type);
                }
            }
        }

        for(MissileType type : game.GetConfig().GetMissileTypes())
        {
            if(!MissileTypes.contains(type) && type.GetPurchasable())
            {
                MissileTypes.add(type);
            }
        }

        for(final MissileType type : MissileTypes)
        {
            final LaunchableSelectionView launchableSelectionView = new LaunchableSelectionView(activity, game, type);

            launchableSelectionView.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    boolean bMissileSiteWithoutNukes = false;

                    if(!bFittedToPlayer)
                    {
                        bMissileSiteWithoutNukes = !game.GetMissileSite(lFittedToStructureID).CanTakeNukes();
                    }

                    if(type.GetNuclear() && bFittedToPlayer)
                    {
                        activity.ShowBasicOKDialog(context.getString(R.string.players_cant_carry_nukes));
                    }
                    else if(type.GetNuclear() && bMissileSiteWithoutNukes)
                    {
                        activity.ShowBasicOKDialog(context.getString(R.string.site_not_nuclear));
                    }
                    else
                    {
                        MissileSystem system = bFittedToPlayer ? game.GetOurPlayer().GetMissileSystem() : game.GetMissileSite(lFittedToStructureID).GetMissileSystem();
                        int lFreeSlots = system.GetEmptySlotCount() - Types.size();
                        int lAvailableFunds = game.GetOurPlayer().GetWealth() - lTotalCost;

                        if(lFreeSlots == 0)
                        {
                            activity.ShowBasicOKDialog(context.getString(R.string.insufficient_slots));
                        }
                        else if(lAvailableFunds < game.GetConfig().GetMissileCost(type))
                        {
                            activity.ShowBasicOKDialog(context.getString(R.string.insufficient_funds));
                        }
                        else
                        {
                            launchableSelectionView.IncrementNumber();
                            AddType(type.GetID());
                        }
                    }
                }
            });

            lytTypes.addView(launchableSelectionView);
            LaunchableSelectionViews.put(type.GetID(), launchableSelectionView);
        }
    }

    private void SetupInterceptors()
    {
        findViewById(R.id.imgMissile).setVisibility(GONE);

        //Put up to three preferred ones at the top.
        List<Byte> InterceptorPreferences = ClientDefs.GetInterceptorPreferredOrder(context);
        ArrayList<InterceptorType> InterceptorTypes = new ArrayList<>();
        for(Byte cPreferredInterceptor : InterceptorPreferences)
        {
            InterceptorType type = game.GetConfig().GetInterceptorType(cPreferredInterceptor);

            if(type != null)
            {
                if(type.GetPurchasable())
                {
                    InterceptorTypes.add(type);
                }
            }
        }

        for(InterceptorType type : game.GetConfig().GetInterceptorTypes())
        {
            if(!InterceptorTypes.contains(type) && type.GetPurchasable())
            {
                InterceptorTypes.add(type);
            }
        }

        for(final InterceptorType type : InterceptorTypes)
        {
            final LaunchableSelectionView launchableSelectionView = new LaunchableSelectionView(activity, game, type);

            launchableSelectionView.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    MissileSystem system = bFittedToPlayer ? game.GetOurPlayer().GetInterceptorSystem() : game.GetSAMSite(lFittedToStructureID).GetInterceptorSystem();
                    int lFreeSlots = system.GetEmptySlotCount() - Types.size();
                    int lAvailableFunds = game.GetOurPlayer().GetWealth() - lTotalCost;

                    if (lFreeSlots == 0)
                    {
                        activity.ShowBasicOKDialog(context.getString(R.string.insufficient_slots));
                    }
                    else if (lAvailableFunds < game.GetConfig().GetInterceptorCost(type))
                    {
                        activity.ShowBasicOKDialog(context.getString(R.string.insufficient_funds));
                    }
                    else
                    {
                        launchableSelectionView.IncrementNumber();
                        AddType(type.GetID());
                    }
                }
            });

            lytTypes.addView(launchableSelectionView);
            LaunchableSelectionViews.put(type.GetID(), launchableSelectionView);
        }
    }

    @Override
    public void Update()
    {

    }

    private void UpdateSelections()
    {
        final Config config = game.GetConfig();

        MissileSystem system;

        if(bFittedToPlayer)
            system = bMissiles ? game.GetOurPlayer().GetMissileSystem() : game.GetOurPlayer().GetInterceptorSystem();
        else
            system = bMissiles ? game.GetMissileSite(lFittedToStructureID).GetMissileSystem() : game.GetSAMSite(lFittedToStructureID).GetInterceptorSystem();

        final int lFreeSlots = system.GetEmptySlotCount() - Types.size();
        final int lAvailableFunds = game.GetOurPlayer().GetWealth() - lTotalCost;

        if(bMissiles)
        {
            activity.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    boolean bMissileSiteWithoutNukes = false;

                    if(!bFittedToPlayer)
                    {
                        bMissileSiteWithoutNukes = !game.GetMissileSite(lFittedToStructureID).CanTakeNukes();
                    }

                    for (Map.Entry<Byte, LaunchableSelectionView> entry : LaunchableSelectionViews.entrySet())
                    {
                        MissileType type = config.GetMissileType(entry.getKey());
                        LaunchableSelectionView view = entry.getValue();

                        if (lFreeSlots == 0)
                        {
                            view.SetDisabled();
                        }
                        else if (game.GetConfig().GetMissileCost(type) > lAvailableFunds)
                        {
                            view.SetDisabled();
                        }
                        else if (type.GetNuclear() && (bFittedToPlayer || bMissileSiteWithoutNukes))
                        {
                            view.SetDisabled();
                        }
                        else
                        {
                            view.SetNotHighlighted();
                        }
                    }
                }
            });
        }
        else
        {
            activity.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    for (Map.Entry<Byte, LaunchableSelectionView> entry : LaunchableSelectionViews.entrySet())
                    {
                        InterceptorType type = config.GetInterceptorType(entry.getKey());
                        LaunchableSelectionView view = entry.getValue();

                        if (lFreeSlots == 0)
                        {
                            view.SetDisabled();
                        }
                        else if (game.GetConfig().GetInterceptorCost(type) > lAvailableFunds)
                        {
                            view.SetDisabled();
                        }
                        else
                        {
                            view.SetNotHighlighted();
                        }
                    }
                }
            });
        }
    }

    private void AddType(byte cType)
    {
        Types.add(cType);

        lTotalCost = 0;

        for(Byte cLaunchableType : Types)
        {
            lTotalCost += bMissiles ? game.GetConfig().GetMissileCost(cLaunchableType) : game.GetConfig().GetInterceptorCost(cLaunchableType);
        }

        txtCount.setText(Integer.toString(Types.size()));
        txtTotalCost.setText(TextUtilities.GetCurrencyString(lTotalCost));
        btnPurchase.setVisibility(VISIBLE);

        UpdateSelections();
    }

    private void ReturnToParentView()
    {
        if(bFittedToPlayer)
        {
            activity.SetView(bMissiles ? new PlayerMissileView(game, activity) : new PlayerInterceptorView(game, activity));
        }
        else
        {
            activity.SelectEntity(bMissiles ? game.GetMissileSite(lFittedToStructureID) : game.GetSAMSite(lFittedToStructureID));
        }
    }
}
