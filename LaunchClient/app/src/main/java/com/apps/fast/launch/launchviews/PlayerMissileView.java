package com.apps.fast.launch.launchviews;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.TextUtilities;
import com.apps.fast.launch.launchviews.controls.MissileSystemControl;
import com.apps.fast.launch.views.LaunchDialog;

import launch.game.LaunchClientGame;

/**
 * Created by tobster on 11/10/16.
 */
public class PlayerMissileView extends LaunchView
{
    private LinearLayout lytMissiles;
    private TextView txtCMSNotFitted;
    private LinearLayout btnBuyMissileLauncher;
    private TextView txtCostMissiles;

    private MissileSystemControl missileSystemControl;

    private boolean bPlayerHasCMSSystem;

    public PlayerMissileView(LaunchClientGame game, MainActivity activity)
    {
        super(game, activity);
    }

    @Override
    protected void Setup()
    {
        inflate(context, R.layout.view_player_missiles, this);

        lytMissiles = (LinearLayout) findViewById(R.id.lytMissiles);
        txtCMSNotFitted = (TextView) findViewById(R.id.txtCMSNotFitted);
        btnBuyMissileLauncher = (LinearLayout)findViewById(R.id.btnBuyMissileLauncher);
        txtCostMissiles = (TextView)findViewById(R.id.txtCostMissiles);

        if(game.GetOurPlayer().GetHasCruiseMissileSystem())
        {
            bPlayerHasCMSSystem = true;
            PopulateMissileSystemWithSystem();
        }
        else
        {
            bPlayerHasCMSSystem = false;
            PopulateMissileSystemWithPurchaseOptions();
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
                if(game.GetOurPlayer().GetHasCruiseMissileSystem())
                {
                    if(!bPlayerHasCMSSystem)
                    {
                        bPlayerHasCMSSystem = true;
                        PopulateMissileSystemWithSystem();
                    }

                    missileSystemControl.Update();
                }
                else
                {
                    if (bPlayerHasCMSSystem)
                    {
                        bPlayerHasCMSSystem = false;
                        PopulateMissileSystemWithPurchaseOptions();
                    }
                }
            }
        });
    }

    private void PopulateMissileSystemWithPurchaseOptions()
    {
        lytMissiles.removeAllViews();

        txtCMSNotFitted.setVisibility(VISIBLE);
        btnBuyMissileLauncher.setVisibility(VISIBLE);

        txtCostMissiles.setText(TextUtilities.GetCurrencyString(game.GetConfig().GetCMSSystemCost()));
        btnBuyMissileLauncher.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                int lCost = game.GetConfig().GetCMSSystemCost();

                if(game.GetOurPlayer().GetWealth() >= lCost)
                {
                    final LaunchDialog launchDialog = new LaunchDialog();
                    launchDialog.SetHeaderPurchase();
                    launchDialog.SetMessage(context.getString(R.string.purchase_missile_system, TextUtilities.GetCurrencyString(lCost)));
                    launchDialog.SetOnClickYes(new OnClickListener()
                    {
                        @Override
                        public void onClick(View view)
                        {
                            launchDialog.dismiss();
                            game.PurchaseMissileSystem();
                        }
                    });
                    launchDialog.SetOnClickNo(new OnClickListener()
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
    }

    private void PopulateMissileSystemWithSystem()
    {
        txtCMSNotFitted.setVisibility(GONE);
        btnBuyMissileLauncher.setVisibility(GONE);

        lytMissiles.removeAllViews();
        missileSystemControl = new MissileSystemControl(game, activity, game.GetOurPlayerID(), true, true);
        lytMissiles.addView(missileSystemControl);
    }
}
