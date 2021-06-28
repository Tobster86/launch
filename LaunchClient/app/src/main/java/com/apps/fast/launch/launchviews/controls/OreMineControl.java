package com.apps.fast.launch.launchviews.controls;

import android.widget.TextView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.TextUtilities;
import com.apps.fast.launch.components.Utilities;
import com.apps.fast.launch.launchviews.LaunchView;

import java.util.List;

import launch.game.LaunchClientGame;
import launch.game.entities.OreMine;

public class OreMineControl extends LaunchView
{
    private int lID;
    private TextView txtGenerationRemaining;
    private TextView txtContest;

    public OreMineControl(LaunchClientGame game, MainActivity activity, int lOreMineID)
    {
        super(game, activity, true);
        lID = lOreMineID;
        Setup();
    }

    @Override
    protected void Setup()
    {
        inflate(context, R.layout.control_ore_mine, this);

        txtGenerationRemaining = findViewById(R.id.txtGenerationRemaining);
        txtContest = findViewById(R.id.txtContest);

        Update();
    }

    @Override
    public void Update()
    {
        OreMine oreMine = game.GetOreMine(lID);

        txtGenerationRemaining.setText(TextUtilities.GetTimeAmount(oreMine.GetGenerateTimeRemaining()));

        List<OreMine> CompetingOreMines = game.GetNearbyCompetingOreMines(oreMine);

        int lCompeting = 0;

        for(OreMine competingOreMine : CompetingOreMines)
        {
            if(competingOreMine.GetOnline())
                lCompeting++;
        }

        if(CompetingOreMines.size() == 0)
        {
            //There are no competing ore mines in the vicinity. Good colour.
            txtContest.setTextColor(Utilities.ColourFromAttr(context, R.attr.GoodColour));
        }
        else if(lCompeting == 0)
        {
            //Competing ore mines are in the vicinity, but disabled. Warning colour.
            txtContest.setTextColor(Utilities.ColourFromAttr(context, R.attr.WarningColour));
        }
        else
        {
            //Other ore mines are competing for resource. Bad colour.
            txtContest.setTextColor(Utilities.ColourFromAttr(context, R.attr.BadColour));
        }

        txtContest.setText(TextUtilities.GetOreMineCompetitionString(CompetingOreMines.size(), lCompeting, game.GetMaxPotentialOreMineReturn(oreMine)));
    }
}
