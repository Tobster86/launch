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
public class PlayerInterceptorView extends LaunchView
{
    private LinearLayout lytSAM;
    private TextView txtSAMNotFitted;
    private LinearLayout btnBuySAM;
    private TextView txtCostSAM;

    private MissileSystemControl samSystemControl;

    private boolean bPlayerHasSAMSystem;

    public PlayerInterceptorView(LaunchClientGame game, MainActivity activity)
    {
        super(game, activity);
    }

    @Override
    protected void Setup()
    {
        inflate(context, R.layout.view_player_interceptors, this);

        lytSAM = (LinearLayout) findViewById(R.id.lytSAM);
        txtSAMNotFitted = (TextView) findViewById(R.id.txtSAMNotFitted);
        btnBuySAM = (LinearLayout)findViewById(R.id.btnBuySAM);
        txtCostSAM = (TextView)findViewById(R.id.txtCostSAM);

        if(game.GetOurPlayer().GetHasAirDefenceSystem())
        {
            bPlayerHasSAMSystem = true;
            PopulateSAMSystemWithSystem();
        }
        else
        {
            bPlayerHasSAMSystem = false;
            PopulateSAMSystemWithPurchaseOptions();
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
                if(game.GetOurPlayer().GetHasAirDefenceSystem())
                {
                    if(!bPlayerHasSAMSystem)
                    {
                        bPlayerHasSAMSystem = true;
                        PopulateSAMSystemWithSystem();
                    }

                    samSystemControl.Update();
                }
                else
                {
                    if (bPlayerHasSAMSystem)
                    {
                        bPlayerHasSAMSystem = false;
                        PopulateSAMSystemWithPurchaseOptions();
                    }
                }
            }
        });
    }

    private void PopulateSAMSystemWithPurchaseOptions()
    {
        lytSAM.removeAllViews();

        txtSAMNotFitted.setVisibility(VISIBLE);
        btnBuySAM.setVisibility(VISIBLE);

        txtCostSAM.setText(TextUtilities.GetCurrencyString(game.GetConfig().GetSAMSystemCost()));
        btnBuySAM.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                int lCost = game.GetConfig().GetSAMSystemCost();

                if(game.GetOurPlayer().GetWealth() >= lCost)
                {
                    final LaunchDialog launchDialog = new LaunchDialog();
                    launchDialog.SetHeaderPurchase();
                    launchDialog.SetMessage(context.getString(R.string.purchase_sam_system, TextUtilities.GetCurrencyString(lCost)));
                    launchDialog.SetOnClickYes(new OnClickListener()
                    {
                        @Override
                        public void onClick(View view)
                        {
                            launchDialog.dismiss();
                            game.PurchaseSAMSystem();
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

    private void PopulateSAMSystemWithSystem()
    {
        txtSAMNotFitted.setVisibility(GONE);
        btnBuySAM.setVisibility(GONE);

        lytSAM.removeAllViews();
        samSystemControl = new MissileSystemControl(game, activity, game.GetOurPlayerID(), false, true);
        lytSAM.addView(samSystemControl);
    }
}
