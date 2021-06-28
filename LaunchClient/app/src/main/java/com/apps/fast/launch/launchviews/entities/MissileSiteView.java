package com.apps.fast.launch.launchviews.entities;

import android.view.View;

import com.apps.fast.launch.R;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.Utilities;
import com.apps.fast.launch.launchviews.LaunchView;
import com.apps.fast.launch.launchviews.controls.MissileSystemControl;

import java.util.List;

import launch.game.LaunchClientGame;
import launch.game.entities.LaunchEntity;
import launch.game.entities.Structure;

public class MissileSiteView extends StructureView
{
    protected LaunchView systemView;

    public MissileSiteView(LaunchClientGame game, MainActivity activity, LaunchEntity structure)
    {
        super(game, activity, structure);
    }

    @Override
    protected void Setup()
    {
        systemView = new MissileSystemControl(game, activity, structureShadow.GetID(), true, false);

        super.Setup();

        imgLogo.setImageResource(R.drawable.icon_missile);

        if(structureShadow.GetOwnerID() == game.GetOurPlayerID())
        {
            btnApplyName.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    game.SetMissileSiteName(structureShadow.GetID(), txtNameEdit.getText().toString());

                    txtNameButton.setVisibility(VISIBLE);
                    lytNameEdit.setVisibility(GONE);
                    Utilities.DismissKeyboard(activity, txtNameEdit);
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
        return game.GetMissileSite(structureShadow.GetID());
    }

    @Override
    public List<Structure> GetCurrentStructures()
    {
        return null;
    }

    @Override
    public void SetOnOff(boolean bOnline)
    {
        game.SetMissileSiteOnOff(structureShadow.GetID(), bOnline);
    }

    @Override
    protected void Sell()
    {
        game.SellMissileSite(structureShadow.GetID());
    }

    @Override
    protected void Repair()
    {
        game.RepairMissileSite(structureShadow.GetID());
    }
}
