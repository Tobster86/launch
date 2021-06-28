package com.apps.fast.launch.launchviews;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.UI.AvatarBitmaps;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.TextUtilities;
import com.apps.fast.launch.components.Utilities;

import launch.game.Alliance;
import launch.game.LaunchClientGame;
import launch.game.treaties.Treaty;
import launch.game.treaties.War;

/**
 * Created by tobster on 09/11/15.
 */
public class WarView extends LaunchView
{
    private War warShadow;

    private ImageView imgAvatar1;
    private ImageView imgAvatar2;

    private TextView txtName1;
    private TextView txtName2;
    private TextView txtGettingStats;
    private LinearLayout btnRefresh;

    private LinearLayout lytStats;
    private TextView txtWinning1;
    private TextView txtWinning2;
    private TextView txtDraw;
    private TextView txtKills1;
    private TextView txtKills2;
    private TextView txtDeaths1;
    private TextView txtDeaths2;
    private TextView txtIncome1;
    private TextView txtIncome2;

    private TextView txtDamageInflicted1;
    private TextView txtDamageInflicted2;
    private TextView txtOffenceSpending1;
    private TextView txtOffenceSpending2;
    private TextView txtOffenceEfficiency1;
    private TextView txtOffenceEfficiency2;

    private TextView txtDamageReceived1;
    private TextView txtDamageReceived2;
    private TextView txtDefenceSpending1;
    private TextView txtDefenceSpending2;
    private TextView txtDefenceEfficiency1;
    private TextView txtDefenceEfficiency2;

    public WarView(LaunchClientGame game, MainActivity activity, War war)
    {
        super(game, activity, true);
        warShadow = war;
        Setup();
        Update();
    }

    @Override
    protected void Setup()
    {
        inflate(context, R.layout.view_war, this);

        imgAvatar1 = findViewById(R.id.imgAvatar1);
        imgAvatar2 = findViewById(R.id.imgAvatar2);

        txtName1 = findViewById(R.id.txtName1);
        txtName2 = findViewById(R.id.txtName2);
        txtGettingStats = findViewById(R.id.txtGettingStats);
        btnRefresh = findViewById(R.id.btnRefresh);

        lytStats = findViewById(R.id.lytStats);
        txtWinning1 = findViewById(R.id.txtWinning1);
        txtWinning2 = findViewById(R.id.txtWinning2);
        txtDraw = findViewById(R.id.txtDraw);
        txtKills1 = findViewById(R.id.txtKills1);
        txtKills2 = findViewById(R.id.txtKills2);
        txtDeaths1 = findViewById(R.id.txtDeaths1);
        txtDeaths2 = findViewById(R.id.txtDeaths2);
        txtIncome1 = findViewById(R.id.txtIncome1);
        txtIncome2 = findViewById(R.id.txtIncome2);

        txtDamageInflicted1 = findViewById(R.id.txtDamageInflicted1);
        txtDamageInflicted2 = findViewById(R.id.txtDamageInflicted2);
        txtOffenceSpending1 = findViewById(R.id.txtOffenceSpending1);
        txtOffenceSpending2 = findViewById(R.id.txtOffenceSpending2);
        txtOffenceEfficiency1 = findViewById(R.id.txtOffenceEfficiency1);
        txtOffenceEfficiency2 = findViewById(R.id.txtOffenceEfficiency2);

        txtDamageReceived1 = findViewById(R.id.txtDamageReceived1);
        txtDamageReceived2 = findViewById(R.id.txtDamageReceived2);
        txtDefenceSpending1 = findViewById(R.id.txtDefenceSpending1);
        txtDefenceSpending2 = findViewById(R.id.txtDefenceSpending2);
        txtDefenceEfficiency1 = findViewById(R.id.txtDefenceEfficiency1);
        txtDefenceEfficiency2 = findViewById(R.id.txtDefenceEfficiency2);

        Alliance alliance1 = game.GetAlliance(warShadow.GetAllianceID1());
        Alliance alliance2 = game.GetAlliance(warShadow.GetAllianceID2());

        txtName1.setText(alliance1.GetName());
        txtName2.setText(alliance2.GetName());

        imgAvatar1.setImageBitmap(AvatarBitmaps.GetAllianceAvatar(activity, game, alliance1));
        imgAvatar2.setImageBitmap(AvatarBitmaps.GetAllianceAvatar(activity, game, alliance2));

        btnRefresh.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                //Must be off the main thread as it does network stuff.
                new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if(game.GetWarStats(warShadow))
                        {
                            activity.runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    lytStats.setVisibility(GONE);
                                    txtGettingStats.setVisibility(VISIBLE);
                                }
                            });
                        }
                    }
                }).start();
            }
        });

        BuildStats();
    }

    private void BuildStats()
    {
        War war = (War)game.GetTreaty(warShadow.GetID());

        //Kills.
        txtKills1.setText(Integer.toString(war.GetKills1()));
        txtKills2.setText(Integer.toString(war.GetKills2()));
        Utilities.ColourLargestGood(context, txtKills1, txtKills2, war.GetKills1(), war.GetKills2());

        //Deaths.
        txtDeaths1.setText(Integer.toString(war.GetDeaths1()));
        txtDeaths2.setText(Integer.toString(war.GetDeaths2()));
        Utilities.ColourLargestGood(context, txtDeaths1, txtDeaths2, war.GetDeaths2(), war.GetDeaths1());

        //Income.
        txtIncome1.setText(TextUtilities.GetCurrencyString(war.GetIncome1()));
        txtIncome2.setText(TextUtilities.GetCurrencyString(war.GetIncome2()));
        Utilities.ColourLargestGood(context, txtIncome1, txtIncome2, war.GetIncome2(), war.GetIncome1());

        //Offence efficiency.
        txtDamageInflicted1.setText(TextUtilities.GetDamageString(war.GetDamageInflicted1()));
        txtDamageInflicted2.setText(TextUtilities.GetDamageString(war.GetDamageInflicted2()));
        Utilities.ColourLargestGood(context, txtDamageInflicted1, txtDamageInflicted2, war.GetDamageInflicted1(), war.GetDamageInflicted2());

        txtOffenceSpending1.setText(TextUtilities.GetCurrencyString(war.GetOffenceSpending1()));
        txtOffenceSpending2.setText(TextUtilities.GetCurrencyString(war.GetOffenceSpending2()));
        Utilities.ColourLargestGood(context, txtOffenceSpending1, txtOffenceSpending2, war.GetOffenceSpending2(), war.GetOffenceSpending1());

        float fltOffenceEfficiency1 = Float.MAX_VALUE;
        float fltOffenceEfficiency2 = Float.MAX_VALUE;

        if(war.GetDamageInflicted1() > 0)
        {
            fltOffenceEfficiency1 = (float)war.GetOffenceSpending1() / (float)war.GetDamageInflicted1();
            txtOffenceEfficiency1.setText(TextUtilities.GetDamageEfficiency(fltOffenceEfficiency1));
        }
        else
            txtOffenceEfficiency1.setText(context.getString(R.string.rubbish));

        if(war.GetDamageInflicted2() > 0)
        {
            fltOffenceEfficiency2 = (float)war.GetOffenceSpending2() / (float)war.GetDamageInflicted2();
            txtOffenceEfficiency2.setText(TextUtilities.GetDamageEfficiency(fltOffenceEfficiency2));
        }
        else
            txtOffenceEfficiency2.setText(context.getString(R.string.rubbish));

        Utilities.ColourLargestGood(context, txtOffenceEfficiency1, txtOffenceEfficiency2, fltOffenceEfficiency2, fltOffenceEfficiency1);

        //Defence efficiency.
        txtDamageReceived1.setText(TextUtilities.GetDamageString(war.GetDamageReceived1()));
        txtDamageReceived2.setText(TextUtilities.GetDamageString(war.GetDamageReceived2()));
        Utilities.ColourLargestGood(context, txtDamageReceived1, txtDamageReceived2, war.GetDamageReceived2(), war.GetDamageReceived1());

        txtDefenceSpending1.setText(TextUtilities.GetCurrencyString(war.GetDefenceSpending1()));
        txtDefenceSpending2.setText(TextUtilities.GetCurrencyString(war.GetDefenceSpending2()));
        Utilities.ColourLargestGood(context, txtDefenceSpending1, txtDefenceSpending2, war.GetDefenceSpending2(), war.GetDefenceSpending1());

        float fltDefenceEfficiency1 = Float.MAX_VALUE;
        float fltDefenceEfficiency2 = Float.MAX_VALUE;

        if(war.GetDamageReceived1() > 0)
        {
            fltDefenceEfficiency1 = (float)war.GetDefenceSpending1() / (float)war.GetDamageReceived1();
            txtDefenceEfficiency1.setText(TextUtilities.GetDamageEfficiency(fltDefenceEfficiency1));
        }
        else
            txtDefenceEfficiency1.setText(context.getString(R.string.rubbish));

        if(war.GetDamageReceived2() > 0)
        {
            fltDefenceEfficiency2 = (float)war.GetDefenceSpending2() / (float)war.GetDamageReceived2();
            txtDefenceEfficiency2.setText(TextUtilities.GetDamageEfficiency(fltDefenceEfficiency2));
        }
        else
            txtDefenceEfficiency2.setText(context.getString(R.string.rubbish));

        Utilities.ColourLargestGood(context, txtDefenceEfficiency1, txtDefenceEfficiency2, fltDefenceEfficiency2, fltDefenceEfficiency1);

        //Determine who is winning.
        int lWinning1 = 0;
        int lWinning2 = 0;

        if(war.GetKills1() > war.GetKills2())
            lWinning1++;

        if(war.GetKills2() > war.GetKills1())
            lWinning2++;

        if(war.GetDeaths1() < war.GetDeaths2())
            lWinning1++;

        if(war.GetDeaths2() < war.GetDeaths1())
            lWinning2++;

        if(war.GetIncome1() < war.GetIncome2())
            lWinning1++;

        if(war.GetIncome2() < war.GetIncome1())
            lWinning2++;

        if(fltOffenceEfficiency1 < fltOffenceEfficiency2)
            lWinning1++;

        if(fltOffenceEfficiency2 < fltOffenceEfficiency1)
            lWinning2++;

        if(fltDefenceEfficiency1 < fltDefenceEfficiency2)
            lWinning1++;

        if(fltDefenceEfficiency2 < fltDefenceEfficiency1)
            lWinning2++;

        if(lWinning1 > lWinning2)
        {
            txtWinning1.setVisibility(VISIBLE);
            txtDraw.setVisibility(GONE);
            txtWinning2.setVisibility(GONE);
        }
        else if(lWinning2 > lWinning1)
        {
            txtWinning1.setVisibility(GONE);
            txtDraw.setVisibility(GONE);
            txtWinning2.setVisibility(VISIBLE);
        }
        else
        {
            txtWinning1.setVisibility(GONE);
            txtDraw.setVisibility(VISIBLE);
            txtWinning2.setVisibility(GONE);
        }
    }

    @Override
    public void Update()
    {
    }

    @Override
    public void AvatarSaved(int lAvatarID)
    {
        final Alliance alliance1 = game.GetAlliance(warShadow.GetAllianceID1());
        final Alliance alliance2 = game.GetAlliance(warShadow.GetAllianceID1());

        if(alliance1.GetAvatarID() == lAvatarID)
        {
            activity.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    imgAvatar1.setImageBitmap(AvatarBitmaps.GetAllianceAvatar(activity, game, alliance1));
                }
            });
        }

        if(alliance2.GetAvatarID() == lAvatarID)
        {
            activity.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    imgAvatar2.setImageBitmap(AvatarBitmaps.GetAllianceAvatar(activity, game, alliance2));
                }
            });
        }
    }

    @Override
    public void TreatyUpdated(Treaty treaty)
    {
        if(treaty.GetID() == warShadow.GetID())
        {
            activity.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    BuildStats();
                    lytStats.setVisibility(VISIBLE);
                    txtGettingStats.setVisibility(GONE);
                }
            });
        }
    }
}
