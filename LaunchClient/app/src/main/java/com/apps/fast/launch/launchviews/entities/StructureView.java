package com.apps.fast.launch.launchviews.entities;

import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.UI.LaunchUICommon;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.TextUtilities;
import com.apps.fast.launch.components.Utilities;
import com.apps.fast.launch.launchviews.LaunchView;
import com.apps.fast.launch.views.EntityControls;
import com.apps.fast.launch.views.LaunchDialog;

import launch.game.LaunchClientGame;
import launch.game.entities.LaunchEntity;
import launch.game.entities.Structure;

/**
 * Created by tobster on 09/11/15.
 */
public abstract class StructureView extends LaunchView implements LaunchUICommon.StructureOnOffInfoProvider
{
    protected ImageView imgLogo;
    private TextView txtHP;

    private TextView txtName;
    protected TextView txtNameButton;
    protected LinearLayout lytNameEdit;
    protected EditText txtNameEdit;
    protected LinearLayout btnApplyName;

    private TextView txtOffline;
    private TextView txtBooting;
    private TextView txtOnline;
    private TextView txtDecommissioning;
    private LinearLayout btnPower;
    private ImageView imgPower;
    private LinearLayout btnSell;
    private LinearLayout btnAttack;
    private LinearLayout btnRepair;

    protected FrameLayout lytConfig;

    protected LaunchView systemView;

    //Structure reference. Note the shadow naming.
    //References the structure object passed as a constructor which may no longer be in the game, but is okay for initialisation and type identification.
    //In updates, a new copy must always be obtained from the game using this shadow's ID.
    protected Structure structureShadow;

    public StructureView(LaunchClientGame game, MainActivity activity, LaunchEntity structure)
    {
        super(game, activity, true);
        structureShadow = (Structure)structure;
        Setup();
    }

    @Override
    protected void Setup()
    {
        inflate(context, R.layout.view_structure, this);
        ((EntityControls)findViewById(R.id.entityControls)).SetActivity(activity);

        ((TextView) findViewById(R.id.txtPlayerJoins)).setText(TextUtilities.GetOwnedEntityName(structureShadow, game));

        imgLogo = findViewById(R.id.imgLogo);
        txtHP = findViewById(R.id.txtHP);

        txtName = findViewById(R.id.txtName);
        txtNameButton = findViewById(R.id.txtNameButton);
        lytNameEdit = findViewById(R.id.lytNameEdit);
        txtNameEdit = findViewById(R.id.txtNameEdit);
        btnApplyName = findViewById(R.id.btnApplyName);

        txtOffline = findViewById(R.id.txtOffline);
        txtBooting = findViewById(R.id.txtBooting);
        txtOnline = findViewById(R.id.txtOnline);
        txtDecommissioning = findViewById(R.id.txtDecommissioning);
        btnPower = findViewById(R.id.btnPower);
        imgPower = findViewById(R.id.imgPower);
        btnSell = findViewById(R.id.btnSell);
        btnAttack = findViewById(R.id.btnAttack);
        btnRepair = findViewById(R.id.btnRepair);
        lytConfig = findViewById(R.id.lytConfig);

        if(structureShadow.GetOwnerID() == game.GetOurPlayerID())
        {
            txtName.setVisibility(GONE);

            txtNameButton.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    activity.ExpandView();
                    txtNameButton.setVisibility(GONE);
                    lytNameEdit.setVisibility(VISIBLE);
                }
            });

            txtNameEdit.setText(structureShadow.GetName());

            LaunchUICommon.SetPowerButtonOnClickListener(activity, btnPower, this, game);

            btnSell.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    Structure structure = GetCurrentStructure();

                    final LaunchDialog launchDialog = new LaunchDialog();
                    launchDialog.SetHeaderPurchase();
                    launchDialog.SetMessage(context.getString(R.string.decommission_confirm, structure.GetTypeName(), TextUtilities.GetCurrencyString(game.GetSaleValue(structure)), TextUtilities.GetTimeAmount(game.GetConfig().GetDecommissionTime())));
                    launchDialog.SetOnClickYes(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View view)
                        {
                            launchDialog.dismiss();
                            Sell();
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

            btnRepair.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    Structure structure = GetCurrentStructure();

                    final int lRepairCost = game.GetRepairCost(structure);

                    final LaunchDialog launchDialog = new LaunchDialog();
                    launchDialog.SetHeaderHealth();
                    launchDialog.SetMessage(context.getString(R.string.repair_confirm, structure.GetTypeName(), TextUtilities.GetCurrencyString(lRepairCost)));
                    launchDialog.SetOnClickYes(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View view)
                        {
                            launchDialog.dismiss();

                            if(game.GetOurPlayer().GetWealth() > lRepairCost)
                            {
                                Repair();
                            }
                            else
                            {
                                activity.ShowBasicOKDialog(context.getString(R.string.insufficient_funds));
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
            });
        }
        else
        {
            txtNameButton.setVisibility(GONE);
            btnPower.setVisibility(GONE);
        }

        btnAttack.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                activity.MissileSelectForTarget(structureShadow.GetPosition(), TextUtilities.GetOwnedEntityName(structureShadow, game));
            }
        });

        lytConfig.setBackgroundColor(Utilities.ColourFromAttr(context, (structureShadow.GetOwnerID() == game.GetOurPlayerID()) ? R.attr.SystemBackgroundColour : R.attr.EnemyBackgroundColour));
    }

    @Override
    public void Update()
    {
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                Structure structure = GetCurrentStructure();

                btnAttack.setVisibility(game.GetOurPlayer().Functioning() ? VISIBLE : GONE);

                if(structure != null)
                {
                    String strName = Utilities.GetStructureName(context, structure);

                    txtName.setText(strName);
                    txtNameButton.setText(strName);

                    TextUtilities.AssignHealthStringAndAppearance(txtHP, structure);

                    txtOffline.setVisibility(structure.GetOffline() ? VISIBLE : GONE);
                    txtBooting.setVisibility(structure.GetBooting() ? VISIBLE : GONE);
                    txtOnline.setVisibility(structure.GetOnline() ? VISIBLE : GONE);
                    txtDecommissioning.setVisibility(structure.GetSelling() ? VISIBLE : GONE);

                    if(structure.GetOwnerID() == game.GetOurPlayerID())
                    {
                        if(structure.GetSelling())
                        {
                            btnPower.setVisibility(GONE);
                        }
                        else
                        {
                            btnPower.setVisibility(VISIBLE);

                            imgPower.setImageResource(structure.GetRunning() ? R.drawable.button_online : R.drawable.button_offline);
                        }

                        btnRepair.setVisibility(structure.AtFullHealth() ? GONE : VISIBLE);
                    }
                    else
                    {
                        btnPower.setVisibility(GONE);
                        btnRepair.setVisibility(GONE);
                    }

                    if(!structure.GetSelling())
                    {
                        btnSell.setVisibility((structure.GetOwnerID() == game.GetOurPlayerID()) && game.GetOurPlayer().Functioning() ? VISIBLE : GONE);
                        lytConfig.setVisibility(VISIBLE);
                    }
                    else
                    {
                        btnSell.setVisibility(GONE);
                        lytConfig.setVisibility(GONE);
                    }

                    if (structure.GetBooting())
                    {
                        txtBooting.setText(context.getString(R.string.state_booting, TextUtilities.GetTimeAmount(structure.GetStateTimeRemaining())));
                    }

                    if (structure.GetSelling())
                    {
                        txtDecommissioning.setText(context.getString(R.string.state_decommissioning, TextUtilities.GetTimeAmount(structure.GetStateTimeRemaining())));
                    }
                }
                else
                {
                    Finish(true);
                }
            }
        });
    }

    protected abstract void Sell();

    protected abstract void Repair();
}
