package com.apps.fast.launch.launchviews.controls;

import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.UI.AvatarBitmaps;
import com.apps.fast.launch.UI.LaunchUICommon;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.TextUtilities;
import com.apps.fast.launch.components.Utilities;
import com.apps.fast.launch.launchviews.DiplomacyView;
import com.apps.fast.launch.launchviews.LaunchView;
import com.apps.fast.launch.launchviews.PlayerRankView;
import com.apps.fast.launch.launchviews.RelationshipView;
import com.apps.fast.launch.launchviews.UploadAvatarView;
import com.apps.fast.launch.launchviews.WarView;
import com.apps.fast.launch.views.LaunchDialog;

import java.util.List;

import launch.game.Alliance;
import launch.game.LaunchClientGame;
import launch.game.entities.Player;

/**
 * Created by tobster on 09/11/15.
 */
public class AllianceControl extends LaunchView
{
    private Alliance allianceShadow;

    private ImageView imgAvatar;

    private TextView txtName;
    private TextView txtNameButton;
    private LinearLayout lytNameEdit;
    private EditText txtNameEdit;
    private LinearLayout btnApplyName;

    private TextView txtDescription;
    private TextView txtDescriptionButton;
    private LinearLayout lytDescriptionEdit;
    private EditText txtDescriptionEdit;
    private LinearLayout btnApplyDescription;

    private TextView txtJoining;
    private TextView txtAtWar;
    private TextView txtAffiliated;
    private TextView txtAffiliationOffered;

    private TextView txtMembers;
    private TextView txtMembersTitle;

    private LinearLayout btnJoinAlliance;
    private LinearLayout btnLeaveAlliance;
    private LinearLayout btnDeclareWar;
    private LinearLayout btnOfferAffiliation;

    private LinearLayout lytAtWarWith;
    private LinearLayout lytAffiliatedWith;
    private LinearLayout lytMembers;

    public AllianceControl(LaunchClientGame game, MainActivity activity, Alliance alliance)
    {
        super(game, activity, true);
        allianceShadow = alliance;
        Setup();
        Update();
    }

    @Override
    protected void Setup()
    {
        inflate(context, R.layout.control_alliance, this);

        imgAvatar = findViewById(R.id.imgAvatar);

        txtName = findViewById(R.id.txtName);
        txtNameButton = findViewById(R.id.txtNameButton);
        lytNameEdit = findViewById(R.id.lytNameEdit);
        txtNameEdit = findViewById(R.id.txtNameEdit);
        btnApplyName = findViewById(R.id.btnApplyName);

        txtDescription = findViewById(R.id.txtDescription);
        txtDescriptionButton = findViewById(R.id.txtDescriptionButton);
        lytDescriptionEdit = findViewById(R.id.lytDescriptionEdit);
        txtDescriptionEdit = findViewById(R.id.txtDescriptionEdit);
        btnApplyDescription = findViewById(R.id.btnApplyDescription);

        txtJoining = findViewById(R.id.txtJoining);
        txtAtWar = findViewById(R.id.txtAtWar);
        txtAffiliated = findViewById(R.id.txtAffiliated);
        txtAffiliationOffered = findViewById(R.id.txtAffiliationOffered);
        txtMembers = findViewById(R.id.txtMembers);
        txtMembersTitle = findViewById(R.id.txtMembersTitle);

        btnJoinAlliance = findViewById(R.id.btnJoinAlliance);
        btnLeaveAlliance = findViewById(R.id.btnLeaveAlliance);
        btnDeclareWar = findViewById(R.id.btnDeclareWar);
        btnOfferAffiliation = findViewById(R.id.btnOfferAffiliation);

        lytAtWarWith = findViewById(R.id.lytAtWarWith);
        lytAffiliatedWith = findViewById(R.id.lytAffiliatedWith);
        lytMembers = findViewById(R.id.lytMembers);

        if(game.GetOurPlayer().GetIsAnMP() && game.GetOurPlayer().GetAllianceMemberID() == allianceShadow.GetID())
        {
            txtName.setVisibility(GONE);

            txtNameButton.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    txtNameButton.setVisibility(GONE);
                    lytNameEdit.setVisibility(VISIBLE);
                }
            });

            txtNameEdit.setText(allianceShadow.GetName());

            btnApplyName.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    game.SetAllianceName(txtNameEdit.getText().toString());

                    txtNameButton.setVisibility(VISIBLE);
                    lytNameEdit.setVisibility(GONE);
                    Utilities.DismissKeyboard(activity, txtNameEdit);
                }
            });

            txtDescription.setVisibility(GONE);

            txtDescriptionButton.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    txtDescriptionButton.setVisibility(GONE);
                    lytDescriptionEdit.setVisibility(VISIBLE);
                }
            });

            txtDescriptionEdit.setText(allianceShadow.GetDescription());

            btnApplyDescription.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    game.SetAllianceDescription(txtDescriptionEdit.getText().toString());

                    txtDescriptionButton.setVisibility(VISIBLE);
                    lytDescriptionEdit.setVisibility(GONE);
                    Utilities.DismissKeyboard(activity, txtDescriptionEdit);
                }
            });

            imgAvatar.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    {
                        activity.SetView(new UploadAvatarView(game, activity, LaunchUICommon.AvatarPurpose.ALLIANCE));
                    }
                }
            });
        }
        else
        {
            txtNameButton.setVisibility(GONE);
            txtDescriptionButton.setVisibility(GONE);
        }

        btnJoinAlliance.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if(game.InBattle(game.GetOurPlayer()))
                {
                    activity.ShowBasicOKDialog(context.getString(R.string.alliance_join_engaged));
                }
                else if(!game.GetOurPlayer().GetAllianceCooloffExpired())
                {
                    activity.ShowBasicOKDialog(context.getString(R.string.cannot_ally, TextUtilities.GetTimeAmount(game.GetOurPlayer().GetAllianceCooloffRemaining())));
                }
                else
                {
                    final LaunchDialog launchDialog = new LaunchDialog();
                    launchDialog.SetHeaderDiplomacy();

                    if(game.GetOurPlayer().GetRequestingToJoinAlliance() && game.GetAlliance(game.GetOurPlayer().GetAllianceJoiningID()) != null)
                    {
                        Alliance allianceOther = game.GetAlliance(game.GetOurPlayer().GetAllianceJoiningID());
                        launchDialog.SetMessage(context.getString(R.string.confirm_join_alliance_cancel_other, allianceShadow.GetName(), allianceOther.GetName()));
                    }
                    else
                    {
                        launchDialog.SetMessage(context.getString(R.string.confirm_join_alliance, allianceShadow.GetName()));
                    }

                    launchDialog.SetOnClickYes(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View view)
                        {
                            launchDialog.dismiss();
                            game.JoinAlliance(allianceShadow.GetID());
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

        btnLeaveAlliance.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if(game.InBattle(game.GetOurPlayer()))
                {
                    activity.ShowBasicOKDialog(context.getString(R.string.alliance_leave_engaged));
                }
                else
                {
                    final LaunchDialog launchDialog = new LaunchDialog();
                    launchDialog.SetHeaderDiplomacy();
                    launchDialog.SetMessage(context.getString(game.IAmTheOnlyLeader() ? R.string.confirm_disband_alliance : R.string.confirm_leave_alliance,
                            allianceShadow.GetName(),
                            TextUtilities.GetTimeAmount(game.GetConfig().GetAllianceCooloffTime())));
                    launchDialog.SetOnClickYes(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View view)
                        {
                            launchDialog.dismiss();
                            game.LeaveAlliance();
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

        btnDeclareWar.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                final LaunchDialog launchDialog = new LaunchDialog();
                launchDialog.SetHeaderDiplomacy();
                launchDialog.SetMessage(context.getString(R.string.confirm_declare_war, allianceShadow.GetName(), TextUtilities.GetDateAndTime(game.GetEndOfWeekTime())));
                launchDialog.SetOnClickYes(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        launchDialog.dismiss();
                        game.DeclareWar(allianceShadow.GetID());
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

        btnOfferAffiliation.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                final LaunchDialog launchDialog = new LaunchDialog();
                launchDialog.SetHeaderDiplomacy();
                launchDialog.SetMessage(context.getString(R.string.confirm_offer_affiliation, allianceShadow.GetName(), TextUtilities.GetDateAndTime(game.GetEndOfWeekTime())));
                launchDialog.SetOnClickYes(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        launchDialog.dismiss();
                        game.OfferAffiliation(allianceShadow.GetID());
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

        final List<Alliance> Enemies = game.GetEnemies(allianceShadow);
        final List<Alliance> Friends = game.GetAffiliates(allianceShadow);
        final List<Player> Members = game.GetMembers(allianceShadow);

        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if(Enemies.size() > 0)
                {
                    lytAtWarWith.setVisibility(VISIBLE);
                }

                for(final Alliance alliance : Enemies)
                {
                    RelationshipView view = new RelationshipView(game, activity, alliance);

                    view.setOnClickListener(new OnClickListener()
                    {
                        @Override
                        public void onClick(View view)
                        {
                            activity.SetView(new WarView(game, activity, game.GetWar(allianceShadow.GetID(), alliance.GetID())));
                        }
                    });

                    lytAtWarWith.addView(view);
                }

                if(Friends.size() > 0)
                {
                    lytAffiliatedWith.setVisibility(VISIBLE);
                }

                for(final Alliance alliance : Friends)
                {
                    RelationshipView view = new RelationshipView(game, activity, alliance);

                    view.setOnClickListener(new OnClickListener()
                    {
                        @Override
                        public void onClick(View view)
                        {
                            activity.SetView(new AllianceControl(game, activity, alliance));
                        }
                    });

                    lytAffiliatedWith.addView(view);
                }

                for(final Player player : Members)
                {
                    PlayerRankView view = new PlayerRankView(game, activity, player);

                    view.setOnClickListener(new OnClickListener()
                    {
                        @Override
                        public void onClick(View view)
                        {
                            activity.ReturnToMainView();
                            activity.SelectEntity(player);
                        }
                    });

                    lytMembers.addView(view);
                }
            }
        });
    }

    @Override
    public void Update()
    {
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                Player ourPlayer = game.GetOurPlayer();
                Alliance alliance = game.GetAlliance(allianceShadow.GetID());

                if(alliance != null)
                {
                    imgAvatar.setImageBitmap(AvatarBitmaps.GetAllianceAvatar(activity, game, alliance));

                    if (ourPlayer.GetIsAnMP() && ourPlayer.GetAllianceMemberID() == alliance.GetID())
                    {
                        imgAvatar.setBackground(getResources().getDrawable(R.drawable.text_button_normal));
                    }

                    txtName.setText(alliance.GetName());
                    txtNameButton.setText(alliance.GetName());
                    txtDescription.setText(alliance.GetDescription());
                    txtDescriptionButton.setText(alliance.GetDescription());

                    txtMembers.setText(Integer.toString(game.GetAllianceMemberCount(alliance)));

                    btnJoinAlliance.setVisibility(ourPlayer.GetAllianceMemberID() == Alliance.ALLIANCE_ID_UNAFFILIATED ? VISIBLE : GONE); //TO DO: And player is not in cool-off period from leaving an alliance.
                    btnLeaveAlliance.setVisibility(ourPlayer.GetAllianceMemberID() == alliance.GetID() ? VISIBLE : GONE);

                    boolean bShowDeclareWar = false;
                    boolean bShowOfferAffiliation = false;

                    if (ourPlayer.GetIsAnMP() && ourPlayer.GetAllianceMemberID() != alliance.GetID())
                    {
                        if(game.CanDeclareWar(ourPlayer.GetAllianceMemberID(), alliance.GetID()))
                            bShowDeclareWar = true;

                        if(game.CanProposeAffiliation(ourPlayer.GetAllianceMemberID(), alliance.GetID()))
                            bShowOfferAffiliation = true;
                    }

                    btnDeclareWar.setVisibility(bShowDeclareWar ? VISIBLE : GONE);
                    btnOfferAffiliation.setVisibility(bShowOfferAffiliation ? VISIBLE : GONE);

                    txtJoining.setVisibility(game.GetOurPlayer().GetAllianceJoiningID() == allianceShadow.GetID() ? VISIBLE : GONE);

                    switch(game.GetAllegiance(alliance))
                    {
                        case ENEMY:
                        {
                            txtAtWar.setVisibility(VISIBLE);
                            txtAffiliated.setVisibility(GONE);
                            txtAffiliationOffered.setVisibility(GONE);
                        }
                        break;

                        case PENDING_TREATY:
                        {
                            txtAtWar.setVisibility(GONE);
                            txtAffiliated.setVisibility(GONE);
                            txtAffiliationOffered.setVisibility(VISIBLE);
                        }
                        break;

                        case AFFILIATE:
                        {
                            txtAtWar.setVisibility(GONE);
                            txtAffiliated.setVisibility(VISIBLE);
                            txtAffiliationOffered.setVisibility(GONE);
                        }
                        break;

                        default:
                        {
                            txtAtWar.setVisibility(GONE);
                            txtAffiliated.setVisibility(GONE);
                            txtAffiliationOffered.setVisibility(GONE);
                        }
                    }
                }
                else
                {
                    activity.SetView(new DiplomacyView(game, activity));
                }
            }
        });
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        super.onLayout(changed, left, top, right, bottom);

        //NOT NECESSARY UNLESS MORE ITEMS APPEAR IN FUTURE. EXAMPLE BELOW.
        int lTitleLargest = txtMembersTitle.getWidth();
        txtMembersTitle.setWidth(lTitleLargest);

        int lValueLargest = txtMembers.getWidth();
        txtMembers.setWidth(lValueLargest);

        //Set title items to same width, to justify text of the values.
        /*int lTitleLargest = Math.max(Math.max(txtRankTitle.getWidth(), txtScoreTitle.getWidth()), txtMembersTitle.getWidth());
        txtRankTitle.setWidth(lTitleLargest);
        txtScoreTitle.setWidth(lTitleLargest);
        txtMembersTitle.setWidth(lTitleLargest);

        int lValueLargest = Math.max(Math.max(txtRank.getWidth(), txtScore.getWidth()), txtMembers.getWidth());
        txtRank.setWidth(lValueLargest);
        txtScore.setWidth(lValueLargest);
        txtMembers.setWidth(lValueLargest);*/
    }

    @Override
    public void AvatarSaved(int lAvatarID)
    {
        for(int i = 0; i < lytAtWarWith.getChildCount(); i++)
        {
            View view = lytAtWarWith.getChildAt(i);

            if(view instanceof RelationshipView)
            {
                ((RelationshipView)view).AvatarSaved(lAvatarID);
            }
        }

        for(int i = 0; i < lytAffiliatedWith.getChildCount(); i++)
        {
            View view = lytAffiliatedWith.getChildAt(i);

            if(view instanceof RelationshipView)
            {
                ((RelationshipView)view).AvatarSaved(lAvatarID);
            }
        }
    }
}
