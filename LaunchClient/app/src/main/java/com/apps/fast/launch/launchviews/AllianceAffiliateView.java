package com.apps.fast.launch.launchviews;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.UI.AvatarBitmaps;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.TextUtilities;
import com.apps.fast.launch.views.LaunchDialog;

import launch.game.Alliance;
import launch.game.LaunchClientGame;

/**
 * Created by tobster on 09/11/15.
 */
public class AllianceAffiliateView extends LaunchView
{
    private Alliance alliance;

    private ImageView imgAlliance;

    private TextView txtName;
    private TextView txtMembers;
    private TextView txtMembersTitle;

    private LinearLayout btnAccept;
    private LinearLayout btnReject;
    private LinearLayout btnWar;

    private boolean bRedundant = false;

    public AllianceAffiliateView(LaunchClientGame game, MainActivity activity, Alliance alliance)
    {
        super(game, activity, true);
        this.alliance = alliance;
        Setup();
        Update();
    }

    @Override
    protected void Setup()
    {
        inflate(context, R.layout.view_alliance_affiliate, this);

        imgAlliance = findViewById(R.id.imgAlliance);
        txtName = findViewById(R.id.txtName);
        txtMembers = findViewById(R.id.txtMembers);
        txtMembersTitle = findViewById(R.id.txtMembersTitle);

        btnAccept = findViewById(R.id.btnAccept);
        btnReject = findViewById(R.id.btnReject);
        btnWar = findViewById(R.id.btnWar);

        imgAlliance.setImageBitmap(AvatarBitmaps.GetAllianceAvatar(activity, game, alliance));
        txtName.setText(alliance.GetName());

        txtMembers.setText(Integer.toString(game.GetAllianceMemberCount(alliance)));

        btnAccept.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                final LaunchDialog launchDialog = new LaunchDialog();
                launchDialog.SetHeaderDiplomacy();
                launchDialog.SetMessage(context.getString(R.string.confirm_accept_affiliation, alliance.GetName(), TextUtilities.GetDateAndTime(game.GetEndOfWeekTime())));
                launchDialog.SetOnClickYes(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        launchDialog.dismiss();
                        game.AcceptAffiliation(alliance.GetID());
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

        btnReject.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                final LaunchDialog launchDialog = new LaunchDialog();
                launchDialog.SetHeaderDiplomacy();
                launchDialog.SetMessage(context.getString(R.string.confirm_reject_affiliation, alliance.GetName()));
                launchDialog.SetOnClickYes(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        launchDialog.dismiss();
                        game.RejectAffiliation(alliance.GetID());
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

        btnWar.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                final LaunchDialog launchDialog = new LaunchDialog();
                launchDialog.SetHeaderDiplomacy();
                launchDialog.SetMessage(context.getString(R.string.confirm_declare_war_reject, alliance.GetName(), TextUtilities.GetDateAndTime(game.GetEndOfWeekTime())));
                launchDialog.SetOnClickYes(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        launchDialog.dismiss();
                        game.DeclareWar(alliance.GetID());
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

    @Override
    public void Update()
    {
        Alliance ourAlliance = game.GetAlliance(game.GetOurPlayer().GetAllianceMemberID());

        if(!game.AffiliationOffered(alliance.GetID(), ourAlliance.GetID()))
        {
            bRedundant = true;
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        super.onLayout(changed, left, top, right, bottom);

        //Set title items to same width, to justify text of the values. NOT REQ UNLESS NEW ONES IN FUTURE.
        /*int lTitleLargest = Math.max(txtScoreTitle.getWidth(), txtMembersTitle.getWidth());
        txtScoreTitle.setWidth(lTitleLargest);
        txtMembersTitle.setWidth(lTitleLargest);

        int lValueLargest = Math.max(txtScore.getWidth(), txtMembers.getWidth());
        txtScore.setWidth(lValueLargest);
        txtMembers.setWidth(lValueLargest);*/
        int lTitleLargest = txtMembersTitle.getWidth();
        txtMembersTitle.setWidth(lTitleLargest);

        int lValueLargest = txtMembers.getWidth();
        txtMembers.setWidth(lValueLargest);
    }

    public boolean GetRedundant()
    {
        return bRedundant;
    }
}
