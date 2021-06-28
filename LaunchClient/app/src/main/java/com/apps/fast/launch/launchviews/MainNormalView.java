package com.apps.fast.launch.launchviews;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.UI.LaunchUICommon;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.ClientDefs;
import com.apps.fast.launch.components.Locatifier;
import com.apps.fast.launch.components.TextUtilities;
import com.apps.fast.launch.components.Utilities;
import com.apps.fast.launch.views.LaunchDialog;

import java.util.List;

import launch.game.Defs;
import launch.game.LaunchClientGame;
import launch.game.entities.LaunchEntity;
import launch.game.entities.Player;
import launch.game.treaties.Treaty;
import launch.utilities.LaunchClientLocation;
import launch.utilities.LaunchEvent;
import launch.utilities.LaunchUtilities;

/**
 * Created by tobster on 15/07/16.
 */
public class MainNormalView extends LaunchView
{
    private LinearLayout lytEmpty;
    private FrameLayout lytBottom;

    private TextView txtMoney;
    private TextView txtHealth;
    private TextView txtHealthChange;
    private TextView txtGPS;
    private TextView txtSignal;
    private ImageView imgGPS;
    private ImageView imgSignal;

    private TextView txtPrivacyZone;
    private TextView txtMain;
    private TextView txtEvent1;
    private TextView txtEvent2;
    private TextView txtEvent3;

    private TextView txtDebug;

    private ImageView btnRespawn;

    private ImageView imgRadiation;

    private LaunchView bottomView;

    private ImageView imgSelect;
    private ImageView imgZoom;

    private ImageView imgShowNeutral;
    private ImageView imgShowFriendly;
    private ImageView imgShowEnemy;

    private ImageView imgShowPlayers;
    private ImageView imgShowMissileSites;
    private ImageView imgShowSAMSites;
    private ImageView imgShowMissiles;
    private ImageView imgShowInterceptors;
    private ImageView imgShowSentryGuns;
    private ImageView imgShowOreMines;
    private ImageView imgShowLoots;

    private ImageView imgShowMap;
    private ImageView imgShowSatellite;

    private ImageView imgMapToolsShow;
    private ImageView imgMapToolsHide;
    private ScrollView lytMapTools;

    private LinearLayout lytMoneyPoints;
    private LinearLayout lytHealth;
    private LinearLayout lytGPSSig;

    private boolean bRendering = false; //To synchronise the render thread when lots of stuff is going on.

    public MainNormalView(LaunchClientGame game, MainActivity activity)
    {
        super(game, activity);
    }

    @Override
    protected void Setup()
    {
        inflate(context, R.layout.main_normal, this);

        lytEmpty = findViewById(R.id.lytEmpty);
        lytBottom = findViewById(R.id.lytBottom);

        txtMoney = findViewById(R.id.txtMoney);
        txtHealth = findViewById(R.id.txtHealth);
        txtHealthChange = findViewById(R.id.txtHealthChange);
        txtGPS = findViewById(R.id.txtGPS);
        txtSignal = findViewById(R.id.txtSignal);
        imgGPS = findViewById(R.id.imgGPS);
        imgSignal = findViewById(R.id.imgSignal);

        txtPrivacyZone = findViewById(R.id.txtPrivacyZone);
        txtMain = findViewById(R.id.txtMain);
        imgRadiation = findViewById(R.id.imgRadiation);
        txtEvent1 = findViewById(R.id.txtEvent1);
        txtEvent2 = findViewById(R.id.txtEvent2);
        txtEvent3 = findViewById(R.id.txtEvent3);
        txtDebug = findViewById(R.id.txtDebug);
        btnRespawn = findViewById(R.id.btnRespawn);

        //Entity visibility button elements.
        imgSelect = findViewById(R.id.imgSelect);
        imgZoom = findViewById(R.id.imgZoom);

        imgShowNeutral = findViewById(R.id.imgShowNeutral);
        imgShowFriendly = findViewById(R.id.imgShowFriendly);
        imgShowEnemy = findViewById(R.id.imgShowEnemy);

        imgShowPlayers = findViewById(R.id.imgShowPlayers);
        imgShowMissileSites = findViewById(R.id.imgShowMissileSites);
        imgShowSAMSites = findViewById(R.id.imgShowSAMSites);
        imgShowMissiles = findViewById(R.id.imgShowMissiles);
        imgShowInterceptors = findViewById(R.id.imgShowInterceptors);
        imgShowSentryGuns = findViewById(R.id.imgShowSentryGuns);
        imgShowOreMines = findViewById(R.id.imgShowOreMines);
        imgShowLoots = findViewById(R.id.imgShowLoots);

        //Get button elements.
        imgShowMap = findViewById(R.id.imgShowMap);
        imgShowSatellite = findViewById(R.id.imgShowSatellite);

        imgMapToolsShow = findViewById(R.id.imgMapToolsShow);
        imgMapToolsHide = findViewById(R.id.imgMapToolsHide);
        lytMapTools =  findViewById(R.id.lytMapTools);

        lytMoneyPoints = findViewById(R.id.lytMoneyPoints);
        lytHealth = findViewById(R.id.lytHealth);
        lytGPSSig = findViewById(R.id.lytGPSSig);

        if(activity.GetMapSatellite())
        {
            imgShowMap.setVisibility(GONE);
            imgShowSatellite.setVisibility(VISIBLE);
        }
        else
        {
            imgShowMap.setVisibility(VISIBLE);
            imgShowSatellite.setVisibility(GONE);
        }

        imgSelect.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                activity.SetMapModeZoomOrSelect(false);
                Update();
            }
        });

        imgZoom.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                activity.SetMapModeZoomOrSelect(true);
                MapToolIconsChanged();
            }
        });

        imgShowNeutral.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                LaunchUtilities.NEUTRAL_VISIBLE = !LaunchUtilities.NEUTRAL_VISIBLE;
                MapToolIconsChanged();
                activity.RebuildMap();
            }
        });

        imgShowFriendly.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                LaunchUtilities.FRIENDLY_VISIBLE = !LaunchUtilities.FRIENDLY_VISIBLE;
                MapToolIconsChanged();
                activity.RebuildMap();
            }
        });

        imgShowEnemy.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                LaunchUtilities.ENEMY_VISIBLE = !LaunchUtilities.ENEMY_VISIBLE;
                MapToolIconsChanged();
                activity.RebuildMap();
            }
        });

        imgShowPlayers.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                LaunchUtilities.PLAYERS_VISIBLE = !LaunchUtilities.PLAYERS_VISIBLE;
                MapToolIconsChanged();
                activity.RebuildMap();
            }
        });

        imgShowMissileSites.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                LaunchUtilities.MISSILE_SITES_VISIBLE = !LaunchUtilities.MISSILE_SITES_VISIBLE;
                MapToolIconsChanged();
                activity.RebuildMap();
            }
        });

        imgShowSAMSites.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                LaunchUtilities.SAM_SITES_VISIBLE = !LaunchUtilities.SAM_SITES_VISIBLE;
                MapToolIconsChanged();
                activity.RebuildMap();
            }
        });

        imgShowMissiles.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                LaunchUtilities.MISSILES_VISIBLE = !LaunchUtilities.MISSILES_VISIBLE;
                MapToolIconsChanged();
                activity.RebuildMap();
            }
        });

        imgShowInterceptors.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                LaunchUtilities.INTERCEPTORS_VISIBLE = !LaunchUtilities.INTERCEPTORS_VISIBLE;
                MapToolIconsChanged();
                activity.RebuildMap();
            }
        });

        imgShowSentryGuns.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                LaunchUtilities.SENTRY_GUNS_VISIBLE = !LaunchUtilities.SENTRY_GUNS_VISIBLE;
                MapToolIconsChanged();
                activity.RebuildMap();
            }
        });

        imgShowOreMines.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                LaunchUtilities.ORE_MINES_VISIBLE = !LaunchUtilities.ORE_MINES_VISIBLE;
                MapToolIconsChanged();
                activity.RebuildMap();
            }
        });

        imgShowLoots.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                LaunchUtilities.LOOTS_VISIBLE = !LaunchUtilities.LOOTS_VISIBLE;
                MapToolIconsChanged();
                activity.RebuildMap();
            }
        });

        imgShowMap.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                activity.SetMapSatellite(true);
                imgShowMap.setVisibility(GONE);
                imgShowSatellite.setVisibility(VISIBLE);
            }
        });

        imgShowSatellite.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                activity.SetMapSatellite(false);
                imgShowMap.setVisibility(VISIBLE);
                imgShowSatellite.setVisibility(GONE);
            }
        });

        imgMapToolsShow.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                ShowMapTools();
            }
        });

        imgMapToolsHide.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                HideMapTools();
            }
        });

        if(activity.GetInteractionMode() == MainActivity.InteractionMode.STANDARD)
        {
            BottomLayoutShowView(new BottomNormalView(game, activity));
        }

        lytMoneyPoints.setOnClickListener(new OnClickListener()
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
                    activity.SetView(new WealthRulesView(game, activity));
                }
            }
        });

        lytHealth.setOnClickListener(new OnClickListener()
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
                    Player ourPlayer = game.GetOurPlayer();

                    final LaunchDialog launchDialog = new LaunchDialog();
                    launchDialog.SetHeaderHealth();

                    if (ourPlayer.AtFullHealth())
                    {
                        launchDialog.SetMessage(context.getString(R.string.heal_full_health));

                        launchDialog.SetOnClickOk(new OnClickListener()
                        {
                            @Override
                            public void onClick(View view)
                            {
                                launchDialog.dismiss();
                            }
                        });
                    } else if (ourPlayer.Destroyed())
                    {
                        launchDialog.SetMessage(context.getString(R.string.heal_dead));

                        launchDialog.SetOnClickOk(new OnClickListener()
                        {
                            @Override
                            public void onClick(View view)
                            {
                                launchDialog.dismiss();
                            }
                        });
                    } else
                    {
                        if(game.GetRadioactive(ourPlayer, true))
                        {
                            launchDialog.SetMessage(context.getString(R.string.heal_radiation_confirm, TextUtilities.GetTimeAmount(game.GetTimeToPlayerRadiationDeath(ourPlayer)), TextUtilities.GetCurrencyString(game.GetHealCost(ourPlayer))));
                        }
                        else
                        {
                            launchDialog.SetMessage(context.getString(R.string.heal_confirm, TextUtilities.GetTimeAmount(game.GetTimeToPlayerFullHealth(ourPlayer)), TextUtilities.GetCurrencyString(game.GetHealCost(ourPlayer))));
                        }

                        launchDialog.SetOnClickYes(new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View view)
                            {
                                launchDialog.dismiss();
                                game.HealPlayer();
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
                    }

                    launchDialog.show(activity.getFragmentManager(), "");
                }
            }
        });

        lytGPSSig.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                activity.GoTo(activity.GetLocatifier().GetLocation());
            }
        });

        lytGPSSig.setOnLongClickListener(new OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View view)
            {
                activity.SetView(new GPSView(game, activity));
                return true;
            }
        });

        txtDebug.setVisibility(game.GetConfig().GetDebugFlags() != 0x00 ? VISIBLE : GONE);
    }

    public void BottomLayoutShowView(final LaunchView launchView)
    {
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if(bottomView != null)
                {
                    lytBottom.removeView(bottomView);
                }

                bottomView = launchView;
                lytBottom.addView(bottomView);
            }
        });
    }

    @Override
    public void Update()
    {
        if(!bRendering)
        {
            bRendering = true;

            activity.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    LaunchClientLocation location = activity.GetLocatifier().GetLocation();

                    if (location == null)
                    {
                        txtGPS.setText(context.getString(R.string.value_unknown));
                        txtGPS.setTextColor(Utilities.ColourFromAttr(context, R.attr.BadColour));
                        imgGPS.setVisibility(GONE);
                    }
                    else
                    {
                        txtGPS.setText(TextUtilities.GetDistanceStringFromM(location.GetAccuracy()));
                        imgGPS.setVisibility(VISIBLE);

                        if (game.GetInPrivacyZone())
                        {
                            txtGPS.setTextColor(Utilities.ColourFromAttr(context, R.attr.InfoColour));
                            imgGPS.setColorFilter(Utilities.ColourFromAttr(context, R.attr.InfoColour));
                        }
                        else
                        {
                            if (location.GetAccuracy() > game.GetConfig().GetRequiredAccuracy() * Defs.METRES_PER_KM)
                            {
                                txtGPS.setTextColor(Utilities.ColourFromAttr(context, R.attr.BadColour));
                                imgGPS.setColorFilter(Utilities.ColourFromAttr(context, R.attr.BadColour));
                            }
                            else
                            {
                                Locatifier.Quality locationQuality = activity.GetLocatifier().GetLocationQuality();

                                if (locationQuality == Locatifier.Quality.GOOD)
                                {
                                    txtGPS.setTextColor(Utilities.ColourFromAttr(context, R.attr.GoodColour));
                                    imgGPS.setColorFilter(Utilities.ColourFromAttr(context, R.attr.GoodColour));
                                }
                                else if (locationQuality == Locatifier.Quality.A_BIT_OLD)
                                {
                                    txtGPS.setTextColor(Utilities.ColourFromAttr(context, R.attr.WarningColour));
                                    imgGPS.setColorFilter(Utilities.ColourFromAttr(context, R.attr.WarningColour));
                                }
                                else
                                {
                                    txtGPS.setTextColor(Utilities.ColourFromAttr(context, R.attr.BadColour));
                                    imgGPS.setColorFilter(Utilities.ColourFromAttr(context, R.attr.BadColour));
                                }
                            }
                        }
                    }

                    long oLatency = game.GetLatency();

                    if (oLatency == Defs.LATENCY_DISCONNECTED)
                    {
                        imgSignal.setColorFilter(Utilities.ColourFromAttr(context, R.attr.BadColour));
                        txtSignal.setTextColor(Utilities.ColourFromAttr(context, R.attr.BadColour));

                        if (game.GetCommsDoingAnything())
                            txtSignal.setText(TextUtilities.GetConnectionSpeed(game.GetCommsDownloadRate()));
                        else
                            txtSignal.setText(context.getString(R.string.value_unknown_reconnect, game.GetCommsReinitRemaining() / TextUtilities.A_SEC));
                    }
                    else
                    {
                        imgSignal.setColorFilter(Utilities.ColourFromAttr(context, R.attr.GoodColour));
                        txtSignal.setTextColor(Utilities.ColourFromAttr(context, R.attr.GoodColour));
                        txtSignal.setText(TextUtilities.GetLatencyString(oLatency));
                    }

                    txtPrivacyZone.setVisibility(game.GetInPrivacyZone() ? VISIBLE : GONE);

                    //Write event text items.
                    List<LaunchEvent> Events = game.GetEvents();

                    txtEvent1.setVisibility(GONE);
                    txtEvent2.setVisibility(GONE);
                    txtEvent3.setVisibility(GONE);

                    if (Events.size() > 0)
                    {
                        LaunchEvent event = Events.get(0);

                        if (event.GetTime() > Utilities.GetServerTime() - ClientDefs.EVENT_MAIN_SCREEN_PERSISTENCE)
                        {
                            txtEvent1.setText(event.GetMessage());
                            txtEvent1.setVisibility(VISIBLE);
                        }
                    }

                    if (Events.size() > 1)
                    {
                        LaunchEvent event = Events.get(1);

                        if (event.GetTime() > Utilities.GetServerTime() - ClientDefs.EVENT_MAIN_SCREEN_PERSISTENCE)
                        {
                            txtEvent2.setText(event.GetMessage());
                            txtEvent2.setVisibility(VISIBLE);
                        }
                    }

                    if (Events.size() > 2)
                    {
                        LaunchEvent event = Events.get(2);

                        if (event.GetTime() > Utilities.GetServerTime() - ClientDefs.EVENT_MAIN_SCREEN_PERSISTENCE)
                        {
                            txtEvent3.setText(event.GetMessage());
                            txtEvent3.setVisibility(VISIBLE);
                        }
                    }

                    bRendering = false;
                }
            });
        }

        OurPlayerUpdated();
        MapToolIconsChanged();

        if(bottomView != null)
        {
            bottomView.Update();
        }
    }

    public LaunchView GetBottomView()
    {
        return bottomView;
    }

    private void ShowMapTools()
    {
        ContractBottomView();
        imgMapToolsShow.setVisibility(GONE);
        imgMapToolsHide.setVisibility(VISIBLE);
        lytMapTools.setVisibility(VISIBLE);
    }

    private void HideMapTools()
    {
        imgMapToolsShow.setVisibility(VISIBLE);
        imgMapToolsHide.setVisibility(GONE);
        lytMapTools.setVisibility(GONE);
    }

    public void ExpandBottomView()
    {
        HideMapTools();
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.weight = context.getResources().getInteger(R.integer.MiddleViewWeightCollapsed);
        lytEmpty.setLayoutParams(params);
    }

    public void ContractBottomView()
    {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.weight = context.getResources().getInteger(R.integer.MiddleViewWeightNormal);
        lytEmpty.setLayoutParams(params);
    }

    /**
     * Call from setup or whenever our player updates to update the UI elements that pertain to our player.
     */
    private void OurPlayerUpdated()
    {
        final Player ourPlayer = game.GetOurPlayer();

        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if(ourPlayer == null)
                {
                    txtMoney.setText(activity.getString(R.string.value_unknown));
                    txtHealth.setText(activity.getString(R.string.value_unknown));
                    txtHealthChange.setText(activity.getString(R.string.value_unknown));
                }
                else
                {
                    if (ourPlayer.Destroyed())
                    {
                        if (ourPlayer.GetCanRespawn())
                        {
                            txtMain.setText(context.getString(R.string.player_spectating));
                            txtMain.setVisibility(VISIBLE);
                            btnRespawn.setVisibility(VISIBLE);
                        }
                        else
                        {
                            txtMain.setText(context.getString(R.string.player_dead, TextUtilities.GetTimeAmount(ourPlayer.GetStateTimeRemaining())));
                            txtMain.setVisibility(VISIBLE);
                            btnRespawn.setVisibility(GONE);
                        }
                    }
                    else
                    {
                        txtMain.setText("");
                        txtMain.setVisibility(GONE);
                        btnRespawn.setVisibility(GONE);
                    }

                    txtMoney.setText(TextUtilities.GetCurrencyString(ourPlayer.GetWealth()));
                    TextUtilities.AssignHealthStringAndAppearance(txtHealth, ourPlayer);

                    if(ourPlayer.AtFullHealth() || ourPlayer.Destroyed())
                    {
                        txtHealthChange.setVisibility(GONE);
                    }
                    else
                    {
                        txtHealthChange.setVisibility(VISIBLE);

                        //Spawn a thread to check for radioactivity, so as not to hang the UI thread.
                        new Thread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                if(game.GetRadioactive(ourPlayer, false))
                                {
                                    activity.runOnUiThread(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            imgRadiation.setVisibility(VISIBLE);

                                            if(game.GetOurPlayer().GetRespawnProtected())
                                            {
                                                txtHealthChange.setText(TextUtilities.GetTimeAmount(game.GetTimeToPlayerFullHealth(ourPlayer)));
                                                txtHealthChange.setTextColor(Utilities.ColourFromAttr(context, R.attr.GoodColour));
                                            }
                                            else
                                            {
                                                txtHealthChange.setText(TextUtilities.GetTimeAmount(game.GetTimeToPlayerRadiationDeath(ourPlayer)));
                                                txtHealthChange.setTextColor(Utilities.ColourFromAttr(context, R.attr.BadColour));
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
                                            imgRadiation.setVisibility(GONE);
                                            txtHealthChange.setText(TextUtilities.GetTimeAmount(game.GetTimeToPlayerFullHealth(ourPlayer)));
                                            txtHealthChange.setTextColor(Utilities.ColourFromAttr(context, R.attr.GoodColour));
                                        }
                                    });
                                }
                            }
                        }).start();
                    }
                }
            }
        });
    }

    /**
     * Call from setup or whenever map icons are used.
     */
    private void MapToolIconsChanged()
    {
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                imgSelect.setVisibility(activity.GetMapModeZoom() ? VISIBLE : GONE);
                imgZoom.setVisibility(activity.GetMapModeZoom() ? GONE : VISIBLE);

                imgShowNeutral.setColorFilter(LaunchUtilities.NEUTRAL_VISIBLE? 0 : LaunchUICommon.COLOUR_TINTED);
                imgShowFriendly.setColorFilter(LaunchUtilities.FRIENDLY_VISIBLE? 0 : LaunchUICommon.COLOUR_TINTED);
                imgShowEnemy.setColorFilter(LaunchUtilities.ENEMY_VISIBLE? 0 : LaunchUICommon.COLOUR_TINTED);
                imgShowPlayers.setColorFilter(LaunchUtilities.PLAYERS_VISIBLE? 0 : LaunchUICommon.COLOUR_TINTED);
                imgShowMissileSites.setColorFilter(LaunchUtilities.MISSILE_SITES_VISIBLE? 0 : LaunchUICommon.COLOUR_TINTED);
                imgShowSAMSites.setColorFilter(LaunchUtilities.SAM_SITES_VISIBLE? 0 : LaunchUICommon.COLOUR_TINTED);
                imgShowMissiles.setColorFilter(LaunchUtilities.MISSILES_VISIBLE? 0 : LaunchUICommon.COLOUR_TINTED);
                imgShowInterceptors.setColorFilter(LaunchUtilities.INTERCEPTORS_VISIBLE? 0 : LaunchUICommon.COLOUR_TINTED);
                imgShowSentryGuns.setColorFilter(LaunchUtilities.SENTRY_GUNS_VISIBLE? 0 : LaunchUICommon.COLOUR_TINTED);
                imgShowOreMines.setColorFilter(LaunchUtilities.ORE_MINES_VISIBLE? 0 : LaunchUICommon.COLOUR_TINTED);
                imgShowLoots.setColorFilter(LaunchUtilities.LOOTS_VISIBLE? 0 : LaunchUICommon.COLOUR_TINTED);
            }
        });
    }

    @Override
    public void EntityUpdated(LaunchEntity entity)
    {
        bottomView.EntityUpdated(entity);

        if(entity instanceof Player)
        {
            Player ourPlayer = game.GetOurPlayer();

            if(ourPlayer != null)
            {
                if(ourPlayer.ApparentlyEquals(entity))
                    OurPlayerUpdated();
            }
        }
    }

    @Override
    public void TreatyUpdated(Treaty treaty)
    {
        bottomView.TreatyUpdated(treaty);
    }
}
