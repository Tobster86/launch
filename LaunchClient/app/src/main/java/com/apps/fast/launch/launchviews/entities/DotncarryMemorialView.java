package com.apps.fast.launch.launchviews.entities;

import android.view.View;
import android.widget.ImageView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.launchviews.LaunchView;

import launch.game.LaunchClientGame;

/**
 * Created by tobster on 09/11/15.
 */
public class
DotncarryMemorialView extends LaunchView
{
    private ImageView btnClose;

    public DotncarryMemorialView(LaunchClientGame game, MainActivity activity)
    {
        super(game, activity);
    }

    @Override
    protected void Setup()
    {
        inflate(context, R.layout.view_dotncarry_memorial, this);
        btnClose = (ImageView)findViewById(R.id.btnClose);

        btnClose.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                activity.ClearSelectedEntity();
                activity.ReturnToMainView();
            }
        });
    }

    @Override
    public void Update()
    {
    }
}
