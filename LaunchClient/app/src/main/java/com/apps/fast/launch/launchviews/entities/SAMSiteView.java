package com.apps.fast.launch.launchviews.entities;

import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.apps.fast.launch.R;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.Utilities;
import com.apps.fast.launch.launchviews.LaunchView;
import com.apps.fast.launch.launchviews.controls.MissileSystemControl;
import com.apps.fast.launch.views.ButtonFlasher;
import com.apps.fast.launch.views.LaunchDialog;

import java.util.List;

import launch.game.LaunchClientGame;
import launch.game.entities.LaunchEntity;
import launch.game.entities.SAMSite;
import launch.game.entities.Structure;

public class SAMSiteView extends StructureView
{
    private FrameLayout lytMode;
    private ImageButton btnAuto;
    private ImageButton btnSemi;
    private ImageButton btnManual;

    private ButtonFlasher flasherAuto;
    private ButtonFlasher flasherSemi;
    private ButtonFlasher flasherManual;

    public SAMSiteView(LaunchClientGame game, MainActivity activity, LaunchEntity structure)
    {
        super(game, activity, structure);
    }

    @Override
    protected void Setup()
    {
        systemView = new MissileSystemControl(game, activity, structureShadow.GetID(), false, false);

        super.Setup();

        lytMode = findViewById(R.id.lytMode);
        btnAuto = findViewById(R.id.btnModeAuto);
        btnSemi = findViewById(R.id.btnModeSemi);
        btnManual = findViewById(R.id.btnModeManual);

        flasherAuto = new ButtonFlasher(btnAuto);
        flasherSemi = new ButtonFlasher(btnSemi);
        flasherManual = new ButtonFlasher(btnManual);

        imgLogo.setImageResource(R.drawable.icon_sam);
        lytMode.setVisibility(VISIBLE);

        if(structureShadow.GetOwnerID() == game.GetOurPlayerID())
        {
            btnApplyName.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    game.SetSAMSiteName(structureShadow.GetID(), txtNameEdit.getText().toString());

                    txtNameButton.setVisibility(VISIBLE);
                    lytNameEdit.setVisibility(GONE);
                    Utilities.DismissKeyboard(activity, txtNameEdit);
                }
            });
        }

        if(structureShadow.GetOwnerID() == game.GetOurPlayerID() && !structureShadow.GetSelling())
        {
            btnAuto.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    if(!game.GetSAMSite(structureShadow.GetID()).GetAuto())
                    {
                        final LaunchDialog launchDialog = new LaunchDialog();
                        launchDialog.SetHeaderSAMControl();
                        launchDialog.SetMessage(context.getString(R.string.confirm_auto));
                        launchDialog.SetOnClickYes(new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View view)
                            {
                                launchDialog.dismiss();

                                game.SetSAMSiteMode(structureShadow.GetID(), SAMSite.MODE_AUTO);
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
                }
            });

            btnSemi.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    if(!game.GetSAMSite(structureShadow.GetID()).GetSemiAuto())
                    {
                        final LaunchDialog launchDialog = new LaunchDialog();
                        launchDialog.SetHeaderSAMControl();
                        launchDialog.SetMessage(context.getString(R.string.confirm_semi));
                        launchDialog.SetOnClickYes(new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View view)
                            {
                                launchDialog.dismiss();

                                game.SetSAMSiteMode(structureShadow.GetID(), SAMSite.MODE_SEMI_AUTO);
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
                }
            });

            btnManual.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    if(!game.GetSAMSite(structureShadow.GetID()).GetManual())
                    {
                        final LaunchDialog launchDialog = new LaunchDialog();
                        launchDialog.SetHeaderSAMControl();
                        launchDialog.SetMessage(context.getString(R.string.confirm_manual));
                        launchDialog.SetOnClickYes(new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View view)
                            {
                                launchDialog.dismiss();

                                game.SetSAMSiteMode(structureShadow.GetID(), SAMSite.MODE_MANUAL);
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
                }
            });
        }

        lytConfig.addView(systemView);
        Update();
    }

    @Override
    public void Update()
    {
        super.Update();

        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                Structure structure = GetCurrentStructure();

                Log.i("LaunchWTF", "We own it: " + Boolean.toString(structureShadow.GetOwnerID() == game.GetOurPlayerID()));
                Log.i("LaunchWTF", "Not Selling: " + Boolean.toString(!structure.GetSelling()));
                Log.i("LaunchWTF", "We're functioning: " + Boolean.toString(game.GetOurPlayer().Functioning()));

                if(structureShadow.GetOwnerID() == game.GetOurPlayerID() && (!structure.GetSelling()) && game.GetOurPlayer().Functioning())
                {
                    SAMSite samSite = (SAMSite)structure;

                    if(samSite.GetAuto())
                    {
                        flasherAuto.TurnGreen(context);
                    }
                    else
                    {
                        flasherAuto.TurnOff(context);
                    }

                    if(samSite.GetSemiAuto())
                    {
                        flasherSemi.TurnGreen(context);
                    }
                    else
                    {
                        flasherSemi.TurnOff(context);
                    }

                    if(samSite.GetManual())
                    {
                        flasherManual.TurnGreen(context);
                    }
                    else
                    {
                        flasherManual.TurnOff(context);
                    }

                    lytMode.setVisibility(VISIBLE);
                }
                else
                {
                    lytMode.setVisibility(GONE);
                }

                if(!structure.GetSelling())
                    systemView.Update();
            }
        });
    }

    @Override
    public boolean IsSingleStructure()
    {
        return true;
    }

    @Override
    public Structure GetCurrentStructure()
    {
        return game.GetSAMSite(structureShadow.GetID());
    }

    @Override
    public List<Structure> GetCurrentStructures()
    {
        return null;
    }

    @Override
    public void SetOnOff(boolean bOnline)
    {
        game.SetSAMSiteOnOff(structureShadow.GetID(), bOnline);
    }

    @Override
    protected void Sell()
    {
        game.SellSAMSite(structureShadow.GetID());
    }

    @Override
    protected void Repair()
    {
        game.RepairSAMSite(structureShadow.GetID());
    }
}
