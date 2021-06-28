package com.apps.fast.launch.launchviews;

import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.UI.LaunchUICommon;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.TextUtilities;
import com.apps.fast.launch.components.Utilities;

import java.util.ArrayList;
import java.util.List;

import launch.game.Config;
import launch.game.Defs;
import launch.game.LaunchClientGame;
import launch.game.entities.LaunchEntity;
import launch.game.entities.MissileSite;
import launch.game.entities.OreMine;
import launch.game.entities.Player;
import launch.game.entities.SAMSite;
import launch.game.entities.SentryGun;
import launch.game.entities.Structure;

/**
 * Created by tobster on 16/10/15.
 */
public class WealthRulesView extends LaunchView
{
    private static final int HOURS_PER_24 = 24;

    private LinearLayout lytHourlyCosts;
    private List<StructureMaintenanceView> EntityViews;

    private ImageView imgShowOnline;
    private ImageView imgShowOffline;
    private ImageView imgShowMissileSites;
    private ImageView imgShowSAMSites;
    private ImageView imgShowSentryGuns;
    private ImageView imgShowOreMines;
    private TextView txtWealthStatement;
    private TextView txtCostStatement;

    private TextView txtGettingStats;
    private LinearLayout lytBonusBasicIncome;
    private LinearLayout lytBonusDiplomaticPresence;
    private LinearLayout lytBonusPoliticalEngagement;
    private LinearLayout lytBonusDefenderOfTheNation;
    private LinearLayout lytBonusNuclearSuperpower;
    private LinearLayout lytBonusWeeklyKills;
    private LinearLayout lytBonusSurvivor;
    private LinearLayout lytBonusHippy;
    private LinearLayout lytBonusPeaceMaker;
    private LinearLayout lytBonusWarMonger;
    private LinearLayout lytBonusLoneWolf;
    private TextView txtBasicIncomeVal;
    private TextView txtDiplomaticPresenceVal;
    private TextView txtPoliticalEngagementVal;
    private TextView txtDefenderOfTheNationVal;
    private TextView txtNuclearSuperpowerVal;
    private TextView txtWeeklyKillsVal;
    private TextView txtSurvivorVal;
    private TextView txtHippyVal;
    private TextView txtPeaceMakerVal;
    private TextView txtWarMongerVal;
    private TextView txtLoneWolfVal;

    boolean bShowOnline;
    boolean bShowOffline;
    boolean bShowMissileSites;
    boolean bShowSAMSites;
    boolean bShowSentryGuns;
    boolean bShowOreMines;

    boolean bHasBonuses = false;

    public WealthRulesView(LaunchClientGame game, MainActivity activity)
    {
        super(game, activity);
    }

    @Override
    protected void Setup()
    {
        inflate(context, R.layout.view_wealth_rules, this);

        lytHourlyCosts = findViewById(R.id.lytHourlyCosts);
        imgShowOnline = findViewById(R.id.imgShowOnline);
        imgShowOffline = findViewById(R.id.imgShowOffline);
        imgShowMissileSites = findViewById(R.id.imgShowMissileSites);
        imgShowSAMSites = findViewById(R.id.imgShowSAMSites);
        imgShowSentryGuns = findViewById(R.id.imgShowSentryGuns);
        imgShowOreMines = findViewById(R.id.imgShowOreMines);
        txtWealthStatement = findViewById(R.id.txtWealthStatement);
        txtCostStatement = findViewById(R.id.txtCostStatement);

        txtGettingStats = findViewById(R.id.txtGettingStats);
        lytBonusBasicIncome = findViewById(R.id.lytBonusBasicIncome);
        lytBonusDiplomaticPresence = findViewById(R.id.lytBonusDiplomaticPresence);
        lytBonusPoliticalEngagement = findViewById(R.id.lytBonusPoliticalEngagement);
        lytBonusDefenderOfTheNation = findViewById(R.id.lytBonusDefenderOfTheNation);
        lytBonusNuclearSuperpower = findViewById(R.id.lytBonusNuclearSuperpower);
        lytBonusWeeklyKills = findViewById(R.id.lytBonusWeeklyKills);
        lytBonusSurvivor = findViewById(R.id.lytBonusSurvivor);
        lytBonusHippy = findViewById(R.id.lytBonusHippy);
        lytBonusPeaceMaker = findViewById(R.id.lytBonusPeaceMaker);
        lytBonusWarMonger = findViewById(R.id.lytBonusWarMonger);
        lytBonusLoneWolf = findViewById(R.id.lytBonusLoneWolf);
        txtBasicIncomeVal = findViewById(R.id.txtBasicIncomeVal);
        txtDiplomaticPresenceVal = findViewById(R.id.txtDiplomaticPresenceVal);
        txtPoliticalEngagementVal = findViewById(R.id.txtPoliticalEngagementVal);
        txtDefenderOfTheNationVal = findViewById(R.id.txtDefenderOfTheNationVal);
        txtNuclearSuperpowerVal = findViewById(R.id.txtNuclearSuperpowerVal);
        txtWeeklyKillsVal = findViewById(R.id.txtWeeklyKillsVal);
        txtSurvivorVal = findViewById(R.id.txtSurvivorVal);
        txtHippyVal = findViewById(R.id.txtHippyVal);
        txtPeaceMakerVal = findViewById(R.id.txtPeaceMakerVal);
        txtWarMongerVal = findViewById(R.id.txtWarMongerVal);
        txtLoneWolfVal = findViewById(R.id.txtLoneWolfVal);

        bShowOnline = true;
        bShowOffline = true;
        bShowMissileSites = true;
        bShowSAMSites = true;
        bShowSentryGuns = true;
        bShowOreMines = true;

        RebuildCostableEntityList();

        //Assign visibility button on click listeners.
        imgShowOnline.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                bShowOnline = !bShowOnline;

                activity.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        imgShowOnline.setColorFilter(bShowOnline? 0 : LaunchUICommon.COLOUR_TINTED);
                        RebuildCostableEntityList();
                    }
                });
            }
        });

        imgShowOffline.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                bShowOffline = !bShowOffline;

                activity.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        imgShowOffline.setColorFilter(bShowOffline? 0 : LaunchUICommon.COLOUR_TINTED);
                        RebuildCostableEntityList();
                    }
                });
            }
        });

        imgShowMissileSites.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                bShowMissileSites = !bShowMissileSites;

                activity.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        imgShowMissileSites.setColorFilter(bShowMissileSites? 0 : LaunchUICommon.COLOUR_TINTED);
                        RebuildCostableEntityList();
                    }
                });
            }
        });

        imgShowSAMSites.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                bShowSAMSites = !bShowSAMSites;

                activity.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        imgShowSAMSites.setColorFilter(bShowSAMSites? 0 : LaunchUICommon.COLOUR_TINTED);
                        RebuildCostableEntityList();
                    }
                });
            }
        });

        imgShowSentryGuns.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                bShowSentryGuns = !bShowSentryGuns;

                activity.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        imgShowSentryGuns.setColorFilter(bShowSentryGuns? 0 : LaunchUICommon.COLOUR_TINTED);
                        RebuildCostableEntityList();
                    }
                });
            }
        });

        imgShowOreMines.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                bShowOreMines = !bShowOreMines;

                activity.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        imgShowOreMines.setColorFilter(bShowOreMines? 0 : LaunchUICommon.COLOUR_TINTED);
                        RebuildCostableEntityList();
                    }
                });
            }
        });

        //Compute income & bonuses.
        Player ourPlayer = game.GetOurPlayer();
        game.GetPlayerStats(ourPlayer);
    }

    @Override
    public void Update()
    {
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if(EntityViews != null)
                {
                    for (StructureMaintenanceView structureMaintenanceView : EntityViews)
                    {
                        structureMaintenanceView.Update();
                    }
                }
            }
        });
    }

    /**
     * Rebuild the player wealth statement, and return their hourly generation.
     * @return Player's hourly wealth generation.
     */
    private int RebuildPlayerWealthStatement()
    {
        Player ourPlayer = game.GetOurPlayer();

        final int lHourlyGeneration = game.GetHourlyIncome(ourPlayer);

        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                txtWealthStatement.setText(context.getString(R.string.wealth_generating, TextUtilities.GetCurrencyString(lHourlyGeneration)));
            }
        });

        return lHourlyGeneration;
    }

    private void RebuildCostableEntityList()
    {
        int lHourlyGeneration = RebuildPlayerWealthStatement();

        //Compute hourly maintenances.
        List<Structure> OurStructures = game.GetOurStructures();

        int lHourlyCosts = 0;

        if((OurStructures.size() > 0))
        {
            findViewById(R.id.txtMaintenanceNone).setVisibility(View.GONE);
        }
        else
        {
            lytHourlyCosts.setVisibility(View.GONE);
        }

        if(EntityViews == null)
        {
            EntityViews = new ArrayList<>();
        }
        else
        {
            lytHourlyCosts.removeAllViews();
            EntityViews.clear();
        }

        for(final Structure structure : OurStructures)
        {
            if(ShouldBeVisible(structure))
            {
                StructureMaintenanceView mev = new StructureMaintenanceView(game, activity, structure);
                EntityViews.add(mev);

                mev.setOnClickListener(new OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        activity.SelectEntity(structure);
                    }
                });

                lytHourlyCosts.addView(mev);
            }

            if(structure.GetOnline() || structure.GetBooting())
            {
                lHourlyCosts += game.GetConfig().GetMaintenanceCost(structure);
            }
        }

        txtCostStatement.setText(context.getString(R.string.wealth_costs, TextUtilities.GetCurrencyString(lHourlyCosts)));

        int l24HourChange = (lHourlyGeneration - lHourlyCosts) * HOURS_PER_24;

        ((TextView)findViewById(R.id.txtTotalGeneration)).setText(context.getString(R.string.total_wealth_generation, TextUtilities.GetCurrencyString(l24HourChange)));

        if(l24HourChange > 0)
        {
            findViewById(R.id.txtNegativeEquity).setVisibility(View.GONE);
        }
        else
        {
            ((TextView)findViewById(R.id.txtTotalGeneration)).setTextColor(Utilities.ColourFromAttr(context, R.attr.BadColour));
        }
    }

    private boolean ShouldBeVisible(Structure structure)
    {
        if(!bShowOffline && structure.GetOffline())
            return false;

        if(!bShowOnline && !structure.GetOffline()) //Deliberately negative offline; all other modes should show under "online".
            return false;

        if(!bShowMissileSites && structure instanceof MissileSite)
            return false;

        if(!bShowSAMSites && structure instanceof SAMSite)
            return false;

        if(!bShowSentryGuns && structure instanceof SentryGun)
            return false;

        if(!bShowOreMines && structure instanceof OreMine)
            return false;

        return true;
    }

    @Override
    public void EntityUpdated(LaunchEntity entity)
    {
        if(entity instanceof Player)
        {
            if(entity.GetID() == game.GetOurPlayerID())
            {
                if(!bHasBonuses)
                {
                    if(((Player)entity).GetHasFullStats())
                    {
                        activity.runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                txtGettingStats.setVisibility(GONE);

                                final Config config = game.GetConfig();
                                final Player ourPlayer = game.GetOurPlayer();

                                int lActivePlayers = 0;
                                int lFriends = 0;
                                int lEnemies = 0;
                                float fltNearestPlayerDistance = Float.MAX_VALUE;
                                Player nearestPlayer = null;

                                for(Player player : game.GetPlayers())
                                {
                                    if(!player.GetAWOL())
                                    {
                                        lActivePlayers++;

                                        switch(game.GetAllegiance(player, ourPlayer))
                                        {
                                            case YOU:
                                            case ALLY:
                                            case AFFILIATE:
                                                lFriends++;
                                                break;

                                            case ENEMY:
                                                lEnemies++;
                                                break;
                                        }

                                        if(!player.ApparentlyEquals(ourPlayer))
                                        {
                                            float fltDistance = ourPlayer.GetPosition().DistanceTo(player.GetPosition());

                                            if(fltDistance < fltNearestPlayerDistance)
                                            {
                                                fltNearestPlayerDistance = fltDistance;
                                                nearestPlayer = player;
                                            }
                                        }
                                    }
                                }

                                txtBasicIncomeVal.setText(TextUtilities.GetCurrencyString(config.GetHourlyWealth()));
                                if(game.GetBasicIncomeEligible(ourPlayer))
                                {
                                    lytBonusBasicIncome.setBackground(Utilities.DrawableFromAttr(context, R.attr.DetailButtonDrawableNormal));
                                    txtBasicIncomeVal.setTextColor(Utilities.ColourFromAttr(context, R.attr.GoodColour));
                                }
                                else
                                {
                                    lytBonusBasicIncome.setBackground(Utilities.DrawableFromAttr(context, R.attr.DetailButtonDrawableDisabled));
                                    txtBasicIncomeVal.setTextColor(Utilities.ColourFromAttr(context, R.attr.BadColour));
                                }

                                lytBonusBasicIncome.setOnClickListener(new OnClickListener()
                                {
                                    @Override
                                    public void onClick(View view)
                                    {
                                        activity.ShowBasicOKDialog(context.getString(R.string.bonus_basic_income_description));
                                    }
                                });

                                txtDiplomaticPresenceVal.setText(TextUtilities.GetCurrencyString(config.GetHourlyBonusDiplomaticPresence()));
                                if(game.GetDiplomaticPresenceEligible(ourPlayer))
                                {
                                    lytBonusDiplomaticPresence.setBackground(Utilities.DrawableFromAttr(context, R.attr.DetailButtonDrawableNormal));
                                    txtDiplomaticPresenceVal.setTextColor(Utilities.ColourFromAttr(context, R.attr.GoodColour));
                                }
                                else
                                {
                                    lytBonusDiplomaticPresence.setBackground(Utilities.DrawableFromAttr(context, R.attr.DetailButtonDrawableDisabled));
                                    txtDiplomaticPresenceVal.setTextColor(Utilities.ColourFromAttr(context, R.attr.BadColour));
                                }

                                lytBonusDiplomaticPresence.setOnClickListener(new OnClickListener()
                                {
                                    @Override
                                    public void onClick(View view)
                                    {
                                        activity.ShowBasicOKDialog(context.getString(R.string.bonus_diplomatic_presence_description));
                                    }
                                });

                                txtPoliticalEngagementVal.setText(TextUtilities.GetCurrencyString(config.GetHourlyBonusPoliticalEngagement()));
                                if(game.GetPoliticalEngagementEligible(ourPlayer))
                                {
                                    lytBonusPoliticalEngagement.setBackground(Utilities.DrawableFromAttr(context, R.attr.DetailButtonDrawableNormal));
                                    txtPoliticalEngagementVal.setTextColor(Utilities.ColourFromAttr(context, R.attr.GoodColour));
                                }
                                else
                                {
                                    lytBonusPoliticalEngagement.setBackground(Utilities.DrawableFromAttr(context, R.attr.DetailButtonDrawableDisabled));
                                    txtPoliticalEngagementVal.setTextColor(Utilities.ColourFromAttr(context, R.attr.BadColour));
                                }

                                lytBonusPoliticalEngagement.setOnClickListener(new OnClickListener()
                                {
                                    @Override
                                    public void onClick(View view)
                                    {
                                        activity.ShowBasicOKDialog(context.getString(R.string.bonus_political_engagement_description));
                                    }
                                });

                                txtDefenderOfTheNationVal.setText(TextUtilities.GetCurrencyString(config.GetHourlyBonusDefenderOfTheNation()));
                                if(game.GetDefenderOfTheNationEligible(ourPlayer))
                                {
                                    lytBonusDefenderOfTheNation.setBackground(Utilities.DrawableFromAttr(context, R.attr.DetailButtonDrawableNormal));
                                    txtDefenderOfTheNationVal.setTextColor(Utilities.ColourFromAttr(context, R.attr.GoodColour));
                                }
                                else
                                {
                                    lytBonusDefenderOfTheNation.setBackground(Utilities.DrawableFromAttr(context, R.attr.DetailButtonDrawableDisabled));
                                    txtDefenderOfTheNationVal.setTextColor(Utilities.ColourFromAttr(context, R.attr.BadColour));
                                }

                                lytBonusDefenderOfTheNation.setOnClickListener(new OnClickListener()
                                {
                                    @Override
                                    public void onClick(View view)
                                    {
                                        activity.ShowBasicOKDialog(context.getString(R.string.bonus_defender_of_the_nation_description));
                                    }
                                });

                                txtNuclearSuperpowerVal.setText(TextUtilities.GetCurrencyString(config.GetHourlyBonusNuclearSuperpower()));
                                if(game.GetNuclearSuperpowerEligible(ourPlayer))
                                {
                                    lytBonusNuclearSuperpower.setBackground(Utilities.DrawableFromAttr(context, R.attr.DetailButtonDrawableNormal));
                                    txtNuclearSuperpowerVal.setTextColor(Utilities.ColourFromAttr(context, R.attr.GoodColour));
                                }
                                else
                                {
                                    lytBonusNuclearSuperpower.setBackground(Utilities.DrawableFromAttr(context, R.attr.DetailButtonDrawableDisabled));
                                    txtNuclearSuperpowerVal.setTextColor(Utilities.ColourFromAttr(context, R.attr.BadColour));
                                }

                                lytBonusNuclearSuperpower.setOnClickListener(new OnClickListener()
                                {
                                    @Override
                                    public void onClick(View view)
                                    {
                                        activity.ShowBasicOKDialog(context.getString(R.string.bonus_nuclear_superpower_description));
                                    }
                                });

                                int lWeeklyKillsBonus = game.GetWeeklyKillsBonus(ourPlayer);
                                txtWeeklyKillsVal.setText(TextUtilities.GetCurrencyString(lWeeklyKillsBonus));

                                if(lWeeklyKillsBonus > 0)
                                    txtWeeklyKillsVal.setTextColor(Utilities.ColourFromAttr(context, R.attr.WarningColour));
                                else
                                    txtWeeklyKillsVal.setTextColor(Utilities.ColourFromAttr(context, R.attr.BadColour));

                                lytBonusWeeklyKills.setOnClickListener(new OnClickListener()
                                {
                                    @Override
                                    public void onClick(View view)
                                    {
                                        activity.ShowBasicOKDialog(context.getString(R.string.bonus_weekly_kills_description, ourPlayer.GetKills(), ourPlayer.GetKills() + 1, TextUtilities.GetCurrencyString(ourPlayer.GetKills() + 1 * config.GetHourlyBonusWeeklyKillsBatch())));
                                    }
                                });

                                txtSurvivorVal.setText(TextUtilities.GetCurrencyString(config.GetHourlyBonusSurvivor()));
                                if(game.GetSurvivorEligible(ourPlayer))
                                {
                                    lytBonusSurvivor.setBackground(Utilities.DrawableFromAttr(context, R.attr.DetailButtonDrawableNormal));
                                    txtSurvivorVal.setTextColor(Utilities.ColourFromAttr(context, R.attr.GoodColour));
                                }
                                else
                                {
                                    lytBonusSurvivor.setBackground(Utilities.DrawableFromAttr(context, R.attr.DetailButtonDrawableDisabled));
                                    txtSurvivorVal.setTextColor(Utilities.ColourFromAttr(context, R.attr.BadColour));
                                }

                                lytBonusSurvivor.setOnClickListener(new OnClickListener()
                                {
                                    @Override
                                    public void onClick(View view)
                                    {
                                        activity.ShowBasicOKDialog(context.getString(R.string.bonus_survivor_description));
                                    }
                                });

                                txtHippyVal.setText(TextUtilities.GetCurrencyString(config.GetHourlyBonusHippy()));
                                if(game.GetHippyEligible(ourPlayer))
                                {
                                    lytBonusHippy.setBackground(Utilities.DrawableFromAttr(context, R.attr.DetailButtonDrawableNormal));
                                    txtHippyVal.setTextColor(Utilities.ColourFromAttr(context, R.attr.GoodColour));
                                }
                                else
                                {
                                    lytBonusHippy.setBackground(Utilities.DrawableFromAttr(context, R.attr.DetailButtonDrawableDisabled));
                                    txtHippyVal.setTextColor(Utilities.ColourFromAttr(context, R.attr.BadColour));
                                }

                                lytBonusHippy.setOnClickListener(new OnClickListener()
                                {
                                    @Override
                                    public void onClick(View view)
                                    {
                                        activity.ShowBasicOKDialog(context.getString(R.string.bonus_hippy_description));
                                    }
                                });

                                if(lActivePlayers > 0)
                                {
                                    txtPeaceMakerVal.setText(TextUtilities.GetCurrencyString(config.GetHourlyBonusPeaceMaker()));
                                    final float fltFriends = (float)lFriends / (float)lActivePlayers;

                                    if((fltFriends > Defs.RELATIONSHIP_BONUS_THRESHOLD))
                                    {
                                        lytBonusPeaceMaker.setBackground(Utilities.DrawableFromAttr(context, R.attr.DetailButtonDrawableNormal));
                                        txtPeaceMakerVal.setTextColor(Utilities.ColourFromAttr(context, R.attr.GoodColour));
                                    }
                                    else
                                    {
                                        lytBonusPeaceMaker.setBackground(Utilities.DrawableFromAttr(context, R.attr.DetailButtonDrawableDisabled));
                                        txtPeaceMakerVal.setTextColor(Utilities.ColourFromAttr(context, R.attr.BadColour));
                                    }

                                    lytBonusPeaceMaker.setOnClickListener(new OnClickListener()
                                    {
                                        @Override
                                        public void onClick(View view)
                                        {
                                            activity.ShowBasicOKDialog(context.getString(R.string.bonus_peace_maker_description, TextUtilities.GetPercentStringFromFraction(Defs.RELATIONSHIP_BONUS_THRESHOLD), TextUtilities.GetPercentStringFromFraction(fltFriends)));
                                        }
                                    });

                                    txtWarMongerVal.setText(TextUtilities.GetCurrencyString(config.GetHourlyBonusWarMonger()));
                                    final float fltEnemies = (float)lEnemies / (float)lActivePlayers;

                                    if((fltEnemies > Defs.RELATIONSHIP_BONUS_THRESHOLD))
                                    {
                                        lytBonusWarMonger.setBackground(Utilities.DrawableFromAttr(context, R.attr.DetailButtonDrawableNormal));
                                        txtWarMongerVal.setTextColor(Utilities.ColourFromAttr(context, R.attr.GoodColour));
                                    }
                                    else
                                    {
                                        lytBonusWarMonger.setBackground(Utilities.DrawableFromAttr(context, R.attr.DetailButtonDrawableDisabled));
                                        txtWarMongerVal.setTextColor(Utilities.ColourFromAttr(context, R.attr.BadColour));
                                    }

                                    lytBonusWarMonger.setOnClickListener(new OnClickListener()
                                    {
                                        @Override
                                        public void onClick(View view)
                                        {
                                            activity.ShowBasicOKDialog(context.getString(R.string.bonus_war_monger_description, TextUtilities.GetPercentStringFromFraction(Defs.RELATIONSHIP_BONUS_THRESHOLD), TextUtilities.GetPercentStringFromFraction(fltEnemies)));
                                        }
                                    });

                                    txtLoneWolfVal.setText(TextUtilities.GetCurrencyString(config.GetHourlyBonusLoneWolf()));
                                    if(nearestPlayer.GetPosition().DistanceTo(ourPlayer.GetPosition()) > config.GetLoneWolfDistance())
                                    {
                                        lytBonusLoneWolf.setBackground(Utilities.DrawableFromAttr(context, R.attr.DetailButtonDrawableNormal));
                                        txtLoneWolfVal.setTextColor(Utilities.ColourFromAttr(context, R.attr.GoodColour));
                                    }
                                    else
                                    {
                                        lytBonusLoneWolf.setBackground(Utilities.DrawableFromAttr(context, R.attr.DetailButtonDrawableDisabled));
                                        txtLoneWolfVal.setTextColor(Utilities.ColourFromAttr(context, R.attr.BadColour));
                                    }

                                    final Player finalNearestPlayer = nearestPlayer;
                                    final float finalFltNearestPlayerDistance = fltNearestPlayerDistance;
                                    lytBonusLoneWolf.setOnClickListener(new OnClickListener()
                                    {
                                        @Override
                                        public void onClick(View view)
                                        {
                                            activity.ShowBasicOKDialog(context.getString(R.string.bonus_lone_wolf_description, TextUtilities.GetDistanceStringFromKM(config.GetLoneWolfDistance()), finalNearestPlayer.GetName(), TextUtilities.GetDistanceStringFromKM(finalFltNearestPlayerDistance)));
                                        }
                                    });

                                    lytBonusPeaceMaker.setVisibility(VISIBLE);
                                    lytBonusWarMonger.setVisibility(VISIBLE);
                                    lytBonusLoneWolf.setVisibility(VISIBLE);
                                }

                                lytBonusBasicIncome.setVisibility(VISIBLE);
                                lytBonusDiplomaticPresence.setVisibility(VISIBLE);
                                lytBonusPoliticalEngagement.setVisibility(VISIBLE);
                                lytBonusDefenderOfTheNation.setVisibility(VISIBLE);
                                lytBonusNuclearSuperpower.setVisibility(VISIBLE);
                                lytBonusWeeklyKills.setVisibility(VISIBLE);
                                lytBonusSurvivor.setVisibility(VISIBLE);
                                lytBonusHippy.setVisibility(VISIBLE);
                            }
                        });

                        bHasBonuses = true;
                    }
                }

                RebuildPlayerWealthStatement();
            }
        }

        if(entity instanceof Structure)
        {
            if(((Structure)entity).GetOwnerID() == game.GetOurPlayerID())
            {
                for(final StructureMaintenanceView structureMaintenanceView : EntityViews)
                {
                    if(entity.ApparentlyEquals(structureMaintenanceView.GetCurrentStructure()))
                    {
                        if(!ShouldBeVisible((Structure)entity))
                        {
                            activity.runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    EntityViews.remove(structureMaintenanceView);
                                    lytHourlyCosts.removeView(structureMaintenanceView);
                                }
                            });
                        }
                    }
                }
            }
        }
    }

    @Override
    public void EntityRemoved(LaunchEntity entity)
    {
        if(entity instanceof Structure)
        {
            if(((Structure)entity).GetOwnerID() == game.GetOurPlayerID())
            {
                for(final StructureMaintenanceView structureMaintenanceView : EntityViews)
                {
                    if(entity.ApparentlyEquals(structureMaintenanceView.GetCurrentStructure()))
                    {
                        activity.runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                EntityViews.remove(structureMaintenanceView);
                                lytHourlyCosts.removeView(structureMaintenanceView);
                            }
                        });
                    }
                }
            }
        }
    }
}
