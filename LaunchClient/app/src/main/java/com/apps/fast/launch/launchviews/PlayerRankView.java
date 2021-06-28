package com.apps.fast.launch.launchviews;

import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.UI.AvatarBitmaps;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.TextUtilities;
import com.apps.fast.launch.components.Utilities;

import launch.game.Alliance;
import launch.game.Defs;
import launch.game.LaunchClientGame;
import launch.game.entities.Player;

/**
 * Created by tobster on 09/11/15.
 */
public class PlayerRankView extends LaunchView
{
    private Player playerShadow;

    private LinearLayout lytBackground;

    private ImageView imgPlayer;

    private TextView txtName;
    private TextView txtAlliance;
    private TextView txtAWOL;
    private TextView txtMultiplier;

    private TextView txtWealth;
    private TextView txtWealthTitle;
    private TextView txtWorth;
    private TextView txtWorthTitle;
    private TextView txtOffences;
    private TextView txtOffencesTitle;
    private TextView txtDefences;
    private TextView txtDefencesTitle;

    private ImageView imgLeader;
    private ImageView imgAlly;
    private ImageView imgWar;
    private ImageView imgAffiliate;

    private TextView txtKillsTotal;
    private TextView txtDeathsTotal;
    private TextView txtDamageInflictedTotal;
    private TextView txtOffenceSpendingCost;
    private TextView txtDamageReceivedTotal;
    private TextView txtDefenceSpendingCost;

    public PlayerRankView(LaunchClientGame game, MainActivity activity, Player player)
    {
        super(game, activity, true);
        this.playerShadow = player;
        Setup();
        Update();
    }

    @Override
    protected void Setup()
    {
        inflate(context, R.layout.view_player_rank, this);

        lytBackground = findViewById(R.id.lytBackground);
        imgPlayer = findViewById(R.id.imgPlayer);
        txtName = findViewById(R.id.txtName);
        txtAWOL = findViewById(R.id.txtAWOL);
        txtAlliance = findViewById(R.id.txtAlliance);
        txtMultiplier = findViewById(R.id.txtMultiplier);
        txtWealth = findViewById(R.id.txtWealth);
        txtWealthTitle = findViewById(R.id.txtWealthTitle);
        txtWorth = findViewById(R.id.txtWorth);
        txtWorthTitle = findViewById(R.id.txtWorthTitle);
        txtOffences = findViewById(R.id.txtOffences);
        txtOffencesTitle = findViewById(R.id.txtOffencesTitle);
        txtDefences = findViewById(R.id.txtDefences);
        txtDefencesTitle = findViewById(R.id.txtDefencesTitle);
        imgLeader = findViewById(R.id.imgLeader);
        imgAlly = findViewById(R.id.imgAlly);
        imgWar = findViewById(R.id.imgWar);
        imgAffiliate = findViewById(R.id.imgAffiliate);

        txtKillsTotal = findViewById(R.id.txtKillsTotal);
        txtDeathsTotal = findViewById(R.id.txtDeathsTotal);
        txtDamageInflictedTotal = findViewById(R.id.txtDamageInflictedTotal);
        txtOffenceSpendingCost = findViewById(R.id.txtOffenceSpendingCost);
        txtDamageReceivedTotal = findViewById(R.id.txtDamageReceivedTotal);
        txtDefenceSpendingCost = findViewById(R.id.txtDefenceSpendingCost);

        RefreshUI();
    }

    public void RefreshUI()
    {
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                Player player = game.GetPlayer(playerShadow.GetID());

                if (game.GetPlayerOnline(player))
                {
                    lytBackground.setBackground(Utilities.DrawableFromAttr(context, R.attr.DetailButtonDrawableHighlighted));
                }
                else
                {
                    lytBackground.setBackground(Utilities.DrawableFromAttr(context, R.attr.DetailButtonDrawableDisabled));
                }

                imgPlayer.setImageBitmap(AvatarBitmaps.GetPlayerAvatar(activity, game, player));
                txtName.setText(player.GetName());

                if (player.GetAllianceMemberID() != Alliance.ALLIANCE_ID_UNAFFILIATED)
                {
                    txtAlliance.setText(game.GetAlliance(player.GetAllianceMemberID()).GetName());
                    txtAlliance.setTextColor(Utilities.ColourFromAttr(context, R.attr.WarningColour));
                }
                else
                {
                    txtAlliance.setText(context.getString(R.string.unaffiliated));
                    txtAlliance.setTextColor(Utilities.ColourFromAttr(context, R.attr.GoodColour));
                }

                if (player.GetAWOL())
                {
                    txtAWOL.setVisibility(VISIBLE);
                }

                float fltMultiplier = game.GetNetWorthMultiplier(game.GetOurPlayer(), player);
                txtMultiplier.setText(TextUtilities.GetMultiplierString(fltMultiplier));

                if (fltMultiplier < Defs.NOOB_WARNING)
                    txtMultiplier.setTextColor(Utilities.ColourFromAttr(context, R.attr.BadColour));
                else if (fltMultiplier > Defs.ELITE_WARNING)
                    txtMultiplier.setTextColor(Utilities.ColourFromAttr(context, R.attr.WarningColour));

                txtWealth.setText(TextUtilities.GetCurrencyString(player.GetWealth()));
                txtWorth.setText(TextUtilities.GetCurrencyString(game.GetPlayerTotalValue(player)));
                txtOffences.setText(TextUtilities.GetCurrencyString(game.GetPlayerOffenseValue(player)));
                txtDefences.setText(TextUtilities.GetCurrencyString(game.GetPlayerDefenseValue(player)));

                imgLeader.setVisibility(player.GetIsAnMP() ? VISIBLE : GONE);

                switch (game.GetAllegiance(player))
                {
                    case ALLY:
                    {
                        imgAlly.setVisibility(VISIBLE);
                        imgWar.setVisibility(GONE);
                        imgAffiliate.setVisibility(GONE);
                    }
                    break;

                    case AFFILIATE:
                    {
                        imgAlly.setVisibility(GONE);
                        imgWar.setVisibility(GONE);
                        imgAffiliate.setVisibility(VISIBLE);
                    }
                    break;

                    case ENEMY:
                    {
                        imgAlly.setVisibility(GONE);
                        imgWar.setVisibility(VISIBLE);
                        imgAffiliate.setVisibility(GONE);
                    }
                    break;

                    default:
                    {
                        imgAlly.setVisibility(GONE);
                        imgWar.setVisibility(GONE);
                        imgAffiliate.setVisibility(GONE);
                    }
                }

                txtKillsTotal.setText(Integer.toString(player.GetKills()));
                txtDeathsTotal.setText(Integer.toString(player.GetDeaths()));
                txtDamageInflictedTotal.setText(TextUtilities.GetDamageString(player.GetDamageInflicted()));
                txtOffenceSpendingCost.setText(TextUtilities.GetCurrencyString(player.GetOffenceSpending()));
                txtDamageReceivedTotal.setText(TextUtilities.GetDamageString(player.GetDamageReceived()));
                txtDefenceSpendingCost.setText(TextUtilities.GetCurrencyString(player.GetDefenceSpending()));
            }
        });
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        super.onLayout(changed, left, top, right, bottom);

        //UNNECESSARY. EXAMPLE BELOW IF REQUIRED IN FUTURE.
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
}
