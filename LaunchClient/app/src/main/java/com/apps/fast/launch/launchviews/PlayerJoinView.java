package com.apps.fast.launch.launchviews;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.UI.AvatarBitmaps;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.TextUtilities;

import launch.game.LaunchClientGame;
import launch.game.entities.Player;

/**
 * Created by tobster on 09/11/15.
 */
public class PlayerJoinView extends LaunchView
{
    private Player player;

    private ImageView imgPlayer;

    private TextView txtName;
    private TextView txtWealth;
    private TextView txtWealthTitle;

    private LinearLayout btnAccept;
    private LinearLayout btnReject;

    private boolean bRedundant = false;

    public PlayerJoinView(LaunchClientGame game, MainActivity activity, Player player)
    {
        super(game, activity, true);
        this.player = player;
        Setup();
        Update();
    }

    @Override
    protected void Setup()
    {
        inflate(context, R.layout.view_player_join, this);

        imgPlayer = findViewById(R.id.imgPlayer);
        txtName = findViewById(R.id.txtName);
        txtWealth = findViewById(R.id.txtWealth);
        txtWealthTitle = findViewById(R.id.txtWealthTitle);

        btnAccept = findViewById(R.id.btnAccept);
        btnReject = findViewById(R.id.btnReject);

        imgPlayer.setImageBitmap(AvatarBitmaps.GetPlayerAvatar(activity, game, player));
        txtName.setText(player.GetName());

        txtWealth.setText(TextUtilities.GetCurrencyString(player.GetWealth()));

        btnAccept.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if(game.InBattle(player))
                {
                    activity.ShowBasicOKDialog(context.getString(R.string.alliance_accept_engaged, player.GetName()));
                }
                else
                {
                    game.AcceptJoin(player.GetID());
                }
            }
        });

        btnReject.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                game.RejectJoin(player.GetID());
            }
        });
    }

    @Override
    public void Update()
    {
        Player updatedPlayer = game.GetPlayer(player.GetID());

        if(!updatedPlayer.GetRequestingToJoinAlliance())
        {
            bRedundant = true;
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        super.onLayout(changed, left, top, right, bottom);

        //NO NECESSARY UNLESS MORE ITEMS ADDED (EXAMPLE BELOW).
        int lTitleLargest = txtWealthTitle.getWidth();
        txtWealthTitle.setWidth(lTitleLargest);

        int lValueLargest = txtWealth.getWidth();
        txtWealth.setWidth(lValueLargest);

        //Set title items to same width, to justify text of the values.
        /*int lTitleLargest = Math.max(txtScoreTitle.getWidth(), txtWealthTitle.getWidth());
        txtScoreTitle.setWidth(lTitleLargest);
        txtWealthTitle.setWidth(lTitleLargest);

        int lValueLargest = Math.max(txtScore.getWidth(), txtWealth.getWidth());
        txtScore.setWidth(lValueLargest);
        txtWealth.setWidth(lValueLargest);*/
    }

    public boolean GetRedundant()
    {
        return bRedundant;
    }
}
