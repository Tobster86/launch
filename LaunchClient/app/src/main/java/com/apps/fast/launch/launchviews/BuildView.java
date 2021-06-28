package com.apps.fast.launch.launchviews;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.TextUtilities;
import com.apps.fast.launch.components.Utilities;
import com.apps.fast.launch.views.DistancedEntityView;
import com.apps.fast.launch.views.LaunchDialog;

import java.util.List;

import launch.game.Config;
import launch.game.LaunchClientGame;
import launch.game.entities.LaunchEntity;
import launch.game.entities.OreMine;
import launch.game.entities.Player;
import launch.game.entities.Structure;

/**
 * Created by tobster on 16/10/15.
 */
public class BuildView extends LaunchView
{
    private TextView txtCannotBuild;
    private LinearLayout lytBuild;
    private LinearLayout btnBuildMissileLauncher;
    private LinearLayout btnBuildNukeLauncher;
    private LinearLayout btnBuildSAM;
    private LinearLayout btnBuildSentryGun;
    private LinearLayout btnBuildOreMine;
    private TextView txtCostMissile;
    private TextView txtCostNuke;
    private TextView txtCostSAM;
    private TextView txtCostSentryGun;
    private TextView txtCostOreMine;
    private TextView txtDescOreMine;
    private TextView txtOreMineWarning;
    private LinearLayout lytNearbyStructures;

    public BuildView(LaunchClientGame game, MainActivity activity)
    {
        super(game, activity);
    }

    @Override
    protected void Setup()
    {
        inflate(context, R.layout.view_build, this);

        txtCannotBuild = (TextView)findViewById(R.id.txtCannotBuild);
        lytBuild = (LinearLayout)findViewById(R.id.lytBuild);
        btnBuildMissileLauncher = (LinearLayout)findViewById(R.id.btnBuildMissileLauncher);
        btnBuildSAM = (LinearLayout)findViewById(R.id.btnBuildSAM);
        btnBuildNukeLauncher = (LinearLayout)findViewById(R.id.btnBuildNukeLauncher);
        btnBuildSentryGun = (LinearLayout)findViewById(R.id.btnBuildSentryGun);
        btnBuildOreMine = (LinearLayout)findViewById(R.id.btnBuildOreMine);
        txtCostMissile = (TextView)findViewById(R.id.txtCostMissile);
        txtCostNuke = (TextView)findViewById(R.id.txtCostNuke);
        txtCostSAM = (TextView)findViewById(R.id.txtCostSAM);
        txtCostSentryGun = (TextView)findViewById(R.id.txtCostSentryGun);
        txtCostOreMine = (TextView)findViewById(R.id.txtCostOreMine);
        txtDescOreMine = (TextView)findViewById(R.id.txtDescOreMine);
        txtOreMineWarning = (TextView)findViewById(R.id.txtDescOreMineWarning);
        lytNearbyStructures = (LinearLayout)findViewById(R.id.lytNearbyStructures);

        final Config config = game.GetConfig();

        txtCostMissile.setText(TextUtilities.GetCurrencyString(config.GetCMSStructureCost()));
        txtCostNuke.setText(TextUtilities.GetCurrencyString(config.GetNukeCMSStructureCost()));
        txtCostSAM.setText(TextUtilities.GetCurrencyString(config.GetSAMStructureCost()));
        txtCostSentryGun.setText(TextUtilities.GetCurrencyString(config.GetSentryGunStructureCost()));
        txtCostOreMine.setText(TextUtilities.GetCurrencyString(config.GetOreMineStructureCost()));

        txtDescOreMine.setText(context.getString(R.string.desc_ore_mine, TextUtilities.GetDistanceStringFromKM(config.GetOreMineRadius())));

        btnBuildMissileLauncher.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if(game.GetOurPlayer().GetWealth() >= config.GetCMSStructureCost())
                {
                    final LaunchDialog launchDialog = new LaunchDialog();
                    launchDialog.SetHeaderConstruct();
                    launchDialog.SetMessage(context.getString(R.string.construct_confirm, context.getString(R.string.construct_name_cms), TextUtilities.GetCurrencyString(config.GetCMSStructureCost())));
                    launchDialog.SetOnClickYes(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View view)
                        {
                            launchDialog.dismiss();
                            game.ConstructMissileSite(false);
                            activity.ReturnToMainView();
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
                    activity.ShowBasicOKDialog(context.getString(R.string.insufficient_funds));
                }
            }
        });

        btnBuildNukeLauncher.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if(game.GetOurPlayer().GetWealth() >= config.GetNukeCMSStructureCost())
                {
                    final LaunchDialog launchDialog = new LaunchDialog();
                    launchDialog.SetHeaderConstruct();
                    launchDialog.SetMessage(context.getString(R.string.construct_confirm, context.getString(R.string.construct_name_ncms), TextUtilities.GetCurrencyString(config.GetNukeCMSStructureCost())));
                    launchDialog.SetOnClickYes(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View view)
                        {
                            launchDialog.dismiss();

                            if(game.GetOurPlayer().GetRespawnProtected())
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
                                        game.ConstructMissileSite(true);
                                        activity.ReturnToMainView();
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
                                game.ConstructMissileSite(true);
                                activity.ReturnToMainView();
                            }
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
                    activity.ShowBasicOKDialog(context.getString(R.string.insufficient_funds));
                }
            }
        });

        btnBuildSAM.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if(game.GetOurPlayer().GetWealth() >= config.GetSAMStructureCost())
                {
                    final LaunchDialog launchDialog = new LaunchDialog();
                    launchDialog.SetHeaderConstruct();
                    launchDialog.SetMessage(context.getString(R.string.construct_confirm, context.getString(R.string.construct_name_sam), TextUtilities.GetCurrencyString(config.GetSAMStructureCost())));
                    launchDialog.SetOnClickYes(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View view)
                        {
                            launchDialog.dismiss();
                            game.ConstructSAMSite();
                            activity.ReturnToMainView();
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
                    activity.ShowBasicOKDialog(context.getString(R.string.insufficient_funds));
                }
            }
        });

        btnBuildSentryGun.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if(game.GetOurPlayer().GetWealth() >= config.GetSentryGunStructureCost())
                {
                    final LaunchDialog launchDialog = new LaunchDialog();
                    launchDialog.SetHeaderConstruct();
                    launchDialog.SetMessage(context.getString(R.string.construct_confirm, context.getString(R.string.construct_name_sentry_gun), TextUtilities.GetCurrencyString(config.GetSentryGunStructureCost())));
                    launchDialog.SetOnClickYes(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View view)
                        {
                            launchDialog.dismiss();
                            game.ConstructSentryGun();
                            activity.ReturnToMainView();
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
                    activity.ShowBasicOKDialog(context.getString(R.string.insufficient_funds));
                }
            }
        });

        btnBuildOreMine.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if(game.GetOurPlayer().GetWealth() >= config.GetOreMineStructureCost())
                {
                    final LaunchDialog launchDialog = new LaunchDialog();
                    launchDialog.SetHeaderConstruct();
                    launchDialog.SetMessage(context.getString(R.string.construct_confirm_vowel, context.getString(R.string.construct_name_ore_mine), TextUtilities.GetCurrencyString(config.GetOreMineStructureCost())));
                    launchDialog.SetOnClickYes(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View view)
                        {
                            launchDialog.dismiss();
                            game.ConstructOreMine();
                            activity.ReturnToMainView();
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
                    activity.ShowBasicOKDialog(context.getString(R.string.insufficient_funds));
                }
            }
        });

        List<Structure> NearbyStructures = game.GetNearbyStructures(game.GetOurPlayer());

        //Building.
        if(NearbyStructures.size() > 0)
        {
            lytBuild.setVisibility(GONE);
            txtCannotBuild.setText(context.getString(R.string.cannot_build, TextUtilities.GetDistanceStringFromKM(game.GetConfig().GetStructureSeparation())));
            txtCannotBuild.setVisibility(View.VISIBLE);

            PopulateEntityView(lytNearbyStructures, NearbyStructures);
        }
        else
        {
            lytBuild.setVisibility(VISIBLE);
            txtCannotBuild.setVisibility(View.GONE);
        }
    }

    @Override
    public void Update()
    {
        final Player ourPlayer = game.GetOurPlayer();
        final Config config = game.GetConfig();

        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                txtCostMissile.setTextColor(Utilities.ColourFromAttr(context, ourPlayer.GetWealth() >= config.GetCMSStructureCost() ? R.attr.GoodColour : R.attr.BadColour));
                txtCostNuke.setTextColor(Utilities.ColourFromAttr(context, ourPlayer.GetWealth() >= config.GetNukeCMSStructureCost() ? R.attr.GoodColour : R.attr.BadColour));
                txtCostSAM.setTextColor(Utilities.ColourFromAttr(context, ourPlayer.GetWealth() >= config.GetSAMStructureCost() ? R.attr.GoodColour : R.attr.BadColour));
                txtCostSentryGun.setTextColor(Utilities.ColourFromAttr(context, ourPlayer.GetWealth() >= config.GetSentryGunStructureCost() ? R.attr.GoodColour : R.attr.BadColour));
                txtCostOreMine.setTextColor(Utilities.ColourFromAttr(context, ourPlayer.GetWealth() >= config.GetOreMineStructureCost() ? R.attr.GoodColour : R.attr.BadColour));

                List<OreMine> NearbyOreMines = game.GetNearbyCompetingOreMines(ourPlayer.GetPosition());
                txtOreMineWarning.setVisibility(NearbyOreMines.size() > 0? VISIBLE : GONE);
            }
        });
    }

    private void PopulateEntityView(LinearLayout view, List<Structure> Structures)
    {
        view.removeAllViews();

        for(final LaunchEntity entity : Structures)
        {
            DistancedEntityView nev = new DistancedEntityView(context, activity, entity, game.GetOurPlayer().GetPosition(), game);

            nev.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    activity.SelectEntity(entity);
                }
            });

            view.addView(nev);
        }
    }
}
