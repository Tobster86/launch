package com.apps.fast.launch.launchviews;

import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.UI.LaunchUICommon;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.TextUtilities;
import com.apps.fast.launch.components.Utilities;
import com.apps.fast.launch.views.ButtonFlasher;
import com.apps.fast.launch.views.LaunchDialog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import launch.game.LaunchClientGame;
import launch.game.entities.LaunchEntity;
import launch.game.entities.MissileSite;
import launch.game.entities.OreMine;
import launch.game.entities.SAMSite;
import launch.game.entities.SentryGun;
import launch.game.entities.Structure;

/**
 * Created by tobster on 09/11/15.
 */
public class StructureMaintenanceView extends LaunchView implements LaunchUICommon.StructureOnOffInfoProvider
{
    private Structure structureShadow = null;
    private Collection StructureList = null;

    private TextView txtState;
    private TextView txtHealth;
    private TextView txtEmptySlots;
    private TextView txtCost;
    private TextView txtTime;
    private ImageView imgPower;

    private ImageButton btnAuto;
    private ImageButton btnSemi;
    private ImageButton btnManual;
    private ButtonFlasher flasherAuto;
    private ButtonFlasher flasherSemi;
    private ButtonFlasher flasherManual;

    /**
     * Initialise for a single structure.
     * @param game Reference to the game.
     * @param activity Reference to the main activity.
     * @param structure The structure.
     */
    public StructureMaintenanceView(LaunchClientGame game, MainActivity activity, Structure structure)
    {
        super(game, activity, true);
        this.structureShadow = structure;
        Setup();
    }

    /**
     * Initialise for a list of structures which MUST ALL BE THE SAME TYPE.
     * @param game Reference to the game.
     * @param activity Reference to the main activity.
     * @param structures List of structures WHICH MUST ALL BE THE SAME TYPE.
     */
    public StructureMaintenanceView(LaunchClientGame game, MainActivity activity, Collection structures)
    {
        super(game, activity, true);
        this.StructureList = structures;
        Setup();
    }

    @Override
    protected void Setup()
    {
        inflate(context, R.layout.view_structure_maintenance, this);

        TextView txtCount = findViewById(R.id.txtCount);
        ImageView imgType = findViewById(R.id.imgType);
        TextView txtType = findViewById(R.id.txtType);
        TextView txtName = findViewById(R.id.txtName);
        LinearLayout btnPower = findViewById(R.id.btnPower);
        LinearLayout lytSAMControls = findViewById(R.id.lytSAMControls);
        btnAuto = findViewById(R.id.btnModeAuto);
        btnSemi = findViewById(R.id.btnModeSemi);
        btnManual = findViewById(R.id.btnModeManual);

        txtState = findViewById(R.id.txtState);
        txtHealth = findViewById(R.id.txtHealth);
        txtEmptySlots = findViewById(R.id.txtEmptySlots);
        txtCost = findViewById(R.id.txtCost);
        txtTime = findViewById(R.id.txtTime);
        imgPower = findViewById(R.id.imgPower);

        LaunchUICommon.SetPowerButtonOnClickListener(activity, btnPower, this, game);

        Structure iconControlStructure = structureShadow == null ? (Structure)StructureList.iterator().next() : structureShadow;
        txtType.setText(TextUtilities.GetEntityTypeAndName(iconControlStructure, game));

        if(structureShadow != null)
        {
            //Individual structure. Show name if it has one, and bin the count label.
            if (structureShadow.GetName().length() > 0)
                txtName.setText(structureShadow.GetName());
            else
                txtName.setVisibility(GONE);

            txtCount.setVisibility(GONE);
        }

        if(StructureList != null)
        {
            //Group of structures. Bin the name and state labels and set the count.
            txtName.setVisibility(GONE);
            txtState.setVisibility(GONE);
            txtTime.setVisibility(GONE);
            txtCount.setText(Integer.toString(StructureList.size()));
        }

        if(iconControlStructure instanceof MissileSite)
        {
            imgType.setImageResource(R.drawable.icon_missile);
        }
        else if(iconControlStructure instanceof SAMSite)
        {
            lytSAMControls.setVisibility(VISIBLE);
            imgType.setImageResource(R.drawable.icon_sam);

            flasherAuto = new ButtonFlasher(btnAuto);
            flasherSemi = new ButtonFlasher(btnSemi);
            flasherManual = new ButtonFlasher(btnManual);

            if(structureShadow != null)
            {
                btnAuto.setOnClickListener(new OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        if (!game.GetSAMSite(structureShadow.GetID()).GetAuto())
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
                        if (!game.GetSAMSite(structureShadow.GetID()).GetSemiAuto())
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
                        if (!game.GetSAMSite(structureShadow.GetID()).GetManual())
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
            else if(StructureList != null)
            {
                final List<Integer> IDs = new ArrayList<>();

                for(Object object : StructureList)
                {
                    Structure structure = (Structure)object;
                    IDs.add(structure.GetID());
                }

                btnAuto.setOnClickListener(new OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        game.SetSAMSiteModes(IDs, SAMSite.MODE_AUTO);
                    }
                });

                btnSemi.setOnClickListener(new OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        game.SetSAMSiteModes(IDs, SAMSite.MODE_SEMI_AUTO);
                    }
                });

                btnManual.setOnClickListener(new OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        game.SetSAMSiteModes(IDs, SAMSite.MODE_MANUAL);
                    }
                });
            }
        }
        else if(iconControlStructure instanceof SentryGun)
        {
            imgType.setImageResource(R.drawable.icon_sentrygun);
        }
        else if(iconControlStructure instanceof OreMine)
        {
            imgType.setImageResource(R.drawable.icon_oremine);
        }

        Update();
    }

    @Override
    public void Update()
    {
        if(structureShadow != null)
        {
            Structure structure = GetCurrentStructure();

            if (structure != null)
            {
                TextUtilities.SetStructureState(txtState, structure);
                TextUtilities.AssignHealthStringAndAppearance(txtHealth, structure);

                int lOccupiedSlots = 0;
                int lSlotCount = 0;

                if (structure instanceof MissileSite)
                {
                    lOccupiedSlots = ((MissileSite) structure).GetMissileSystem().GetOccupiedSlotCount();
                    lSlotCount = ((MissileSite) structure).GetMissileSystem().GetSlotCount();
                }
                else if (structure instanceof SAMSite)
                {
                    lOccupiedSlots = ((SAMSite) structure).GetInterceptorSystem().GetOccupiedSlotCount();
                    lSlotCount = ((SAMSite) structure).GetInterceptorSystem().GetSlotCount();

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
                }
                else
                {
                    txtEmptySlots.setVisibility(GONE);
                }

                txtEmptySlots.setText(context.getString(R.string.empty_slot_count, lOccupiedSlots, lSlotCount));

                if (lOccupiedSlots == lSlotCount)
                    txtEmptySlots.setTextColor(Utilities.ColourFromAttr(context, R.attr.GoodColour));
                else if (lOccupiedSlots == 0)
                    txtEmptySlots.setTextColor(Utilities.ColourFromAttr(context, R.attr.BadColour));
                else
                    txtEmptySlots.setTextColor(Utilities.ColourFromAttr(context, R.attr.WarningColour));

                if ((structure.GetOffline()) || (structure.GetSelling()))
                {
                    txtCost.setText(TextUtilities.GetCurrencyString(0));
                    txtTime.setVisibility(GONE);
                }
                else
                {
                    txtCost.setText(TextUtilities.GetCurrencyString(game.GetConfig().GetMaintenanceCost(structure)));
                    txtTime.setText(TextUtilities.GetTimeAmount(structure.GetChargeOwnerTimeRemaining()));
                    txtTime.setVisibility(VISIBLE);
                }

                imgPower.setImageResource(structure.GetRunning() ? R.drawable.button_online : R.drawable.button_offline);
            }
        }
        else
        {
            List<Structure> CurrentStructures = GetCurrentStructures();
            Structure controlStructure = CurrentStructures.get(0);

            TextUtilities.AssignHealthStringAndAppearance(txtHealth, CurrentStructures);

            int lOccupiedSlots = 0;
            int lSlotCount = 0;

            if (controlStructure instanceof MissileSite)
            {
                for(Structure structure : CurrentStructures)
                {
                    lOccupiedSlots += ((MissileSite) structure).GetMissileSystem().GetOccupiedSlotCount();
                    lSlotCount += ((MissileSite) structure).GetMissileSystem().GetSlotCount();
                }
            }
            else if (controlStructure instanceof SAMSite)
            {
                boolean bAutos = false;
                boolean bSemis = false;
                boolean bManuals = false;

                for(Structure structure : CurrentStructures)
                {
                    lOccupiedSlots += ((SAMSite) structure).GetInterceptorSystem().GetOccupiedSlotCount();
                    lSlotCount += ((SAMSite) structure).GetInterceptorSystem().GetSlotCount();

                    SAMSite samSite = (SAMSite)structure;

                    if(samSite.GetAuto())
                        bAutos = true;

                    if(samSite.GetSemiAuto())
                        bSemis = true;

                    if(samSite.GetManual())
                        bManuals = true;
                }

                if(bAutos)
                {
                    flasherAuto.TurnGreen(context);
                }
                else
                {
                    flasherAuto.TurnOff(context);
                }

                if(bSemis)
                {
                    flasherSemi.TurnGreen(context);
                }
                else
                {
                    flasherSemi.TurnOff(context);
                }

                if(bManuals)
                {
                    flasherManual.TurnGreen(context);
                }
                else
                {
                    flasherManual.TurnOff(context);
                }
            }
            else
            {
                txtEmptySlots.setVisibility(GONE);
            }

            txtEmptySlots.setText(context.getString(R.string.empty_slot_count, lOccupiedSlots, lSlotCount));

            if (lOccupiedSlots == lSlotCount)
                txtEmptySlots.setTextColor(Utilities.ColourFromAttr(context, R.attr.GoodColour));
            else if (lOccupiedSlots == 0)
                txtEmptySlots.setTextColor(Utilities.ColourFromAttr(context, R.attr.BadColour));
            else
                txtEmptySlots.setTextColor(Utilities.ColourFromAttr(context, R.attr.WarningColour));

            int lNumberRunning = 0;
            int lNumberOffline = 0;

            for(Structure structure : CurrentStructures)
            {
                if(structure.GetRunning())
                    lNumberRunning++;
                else if(structure.GetOffline())
                    lNumberOffline++;
            }

            if(lNumberRunning > 0 && lNumberOffline == 0)
                imgPower.setImageResource(R.drawable.button_online);
            else if(lNumberRunning > 0 && lNumberOffline > 0)
                imgPower.setImageResource(R.drawable.button_onoffmix);
            else
                imgPower.setImageResource(R.drawable.button_offline);

            txtCost.setText(TextUtilities.GetCurrencyString(game.GetConfig().GetMaintenanceCost(controlStructure) * lNumberRunning));
        }
    }

    @Override
    public boolean IsSingleStructure()
    {
        return structureShadow != null;
    }

    @Override
    public Structure GetCurrentStructure()
    {
        if(structureShadow instanceof MissileSite)
        {
            return game.GetMissileSite(structureShadow.GetID());
        }
        else if(structureShadow instanceof SAMSite)
        {
            return game.GetSAMSite(structureShadow.GetID());
        }
        else if(structureShadow instanceof SentryGun)
        {
            return game.GetSentryGun(structureShadow.GetID());
        }
        else if(structureShadow instanceof OreMine)
        {
            return game.GetOreMine(structureShadow.GetID());
        }

        return null;
    }

    @Override
    public List<Structure> GetCurrentStructures()
    {
        Structure controlStructure = (Structure)StructureList.iterator().next();
        List<Structure> CurrentStructures = new ArrayList<>();

        for(Object object : StructureList)
        {
            Structure structure = (Structure)object;

            if(controlStructure instanceof MissileSite)
            {
                CurrentStructures.add(game.GetMissileSite(structure.GetID()));
            }
            else if(controlStructure instanceof SAMSite)
            {
                CurrentStructures.add(game.GetSAMSite(structure.GetID()));
            }
            else if(controlStructure instanceof SentryGun)
            {
                CurrentStructures.add(game.GetSentryGun(structure.GetID()));
            }
            else if(controlStructure instanceof OreMine)
            {
                CurrentStructures.add(game.GetOreMine(structure.GetID()));
            }
        }

        return CurrentStructures;
    }

    @Override
    public void SetOnOff(boolean bOnline)
    {
        if(structureShadow != null)
        {
            if (structureShadow instanceof MissileSite)
            {
                game.SetMissileSiteOnOff(structureShadow.GetID(), bOnline);
            }
            else if (structureShadow instanceof SAMSite)
            {
                game.SetSAMSiteOnOff(structureShadow.GetID(), bOnline);
            }
            else if (structureShadow instanceof SentryGun)
            {
                game.SetSentryGunOnOff(structureShadow.GetID(), bOnline);
            }
            else if (structureShadow instanceof OreMine)
            {
                game.SetOreMineOnOff(structureShadow.GetID(), bOnline);
            }
        }
        else
        {
            List<Integer> IDs = new ArrayList<>();

            for(Object object : StructureList)
            {
                Structure structure = (Structure)object;
                IDs.add(structure.GetID());
            }

            Structure controlStructure = (Structure)StructureList.iterator().next();

            if(controlStructure instanceof MissileSite)
            {
                game.SetMissileSitesOnOff(IDs, bOnline);
            }
            else if(controlStructure instanceof SAMSite)
            {
                game.SetSAMSitesOnOff(IDs, bOnline);
            }
            else if(controlStructure instanceof SentryGun)
            {
                game.SetSentryGunsOnOff(IDs, bOnline);
            }
            else if(controlStructure instanceof OreMine)
            {
                game.SetOreMinesOnOff(IDs, bOnline);
            }
        }
    }

    @Override
    public void EntityUpdated(LaunchEntity entity)
    {
        boolean bUpdate = false;

        if(structureShadow != null)
        {
            if(entity.ApparentlyEquals(structureShadow))
                bUpdate = true;
        }

        if(StructureList != null)
        {
            for(Object object : StructureList)
            {
                Structure structure = (Structure)object;

                if(entity.ApparentlyEquals(structure))
                {
                    bUpdate = true;
                    break;
                }
            }
        }

        if(bUpdate)
        {
            activity.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    Update();
                }
            });
        }
    }
}
