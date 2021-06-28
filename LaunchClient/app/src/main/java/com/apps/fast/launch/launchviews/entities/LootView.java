package com.apps.fast.launch.launchviews.entities;

import android.widget.TextView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.TextUtilities;
import com.apps.fast.launch.launchviews.LaunchView;
import com.apps.fast.launch.views.EntityControls;

import launch.game.LaunchClientGame;
import launch.game.entities.Loot;

/**
 * Created by tobster on 09/11/15.
 */
public class
LootView extends LaunchView
{
    private int lLootID;

    private TextView txtValue;
    private TextView txtExpiresIn;

    public LootView(LaunchClientGame game, MainActivity activity, int lLootID)
    {
        super(game, activity, true);
        this.lLootID = lLootID;
        Setup();
    }

    @Override
    protected void Setup()
    {
        Loot loot = game.GetLoot(lLootID);

        if(loot != null)
        {
            inflate(context, R.layout.view_loot, this);
            ((EntityControls)findViewById(R.id.entityControls)).SetActivity(activity);

            ((TextView) findViewById(R.id.txtLootTitle)).setText(loot.GetDescription());

            txtValue = (TextView) findViewById(R.id.txtValue);
            txtExpiresIn = (TextView) findViewById(R.id.txtExpiresIn);

            txtValue.setText(context.getString(R.string.loot_value, TextUtilities.GetCurrencyString(loot.GetValue())));

            Update();
        }
        else
        {
            Finish(true);
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
                Loot loot = game.GetLoot(lLootID);

                if(loot != null)
                {
                    txtExpiresIn.setText(context.getString(R.string.expires_in, TextUtilities.GetTimeAmount(loot.GetExpiryRemaining())));
                }
                else
                {
                    Finish(true);
                }
            }
        });
    }
}
