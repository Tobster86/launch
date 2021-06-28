package com.apps.fast.launch.views;

import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.UI.AvatarBitmaps;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.TextUtilities;
import com.apps.fast.launch.components.Utilities;
import com.apps.fast.launch.launchviews.LaunchView;

import launch.game.Alliance;
import launch.game.LaunchClientGame;
import launch.game.entities.Player;
import launch.utilities.LaunchReport;

/**
 * Created by tobster on 09/11/15.
 */
public class ReportView extends LaunchView
{
    private LaunchReport report;
    private boolean bNew;

    public ReportView(LaunchClientGame game, MainActivity activity, LaunchReport report, boolean bNew)
    {
        super(game, activity, true);
        this.report = report;
        this.bNew = bNew;
        Setup();
    }

    @Override
    protected void Setup()
    {
        inflate(context, R.layout.view_report, this);

        setBackground(Utilities.DrawableFromAttr(activity, bNew ? R.attr.DetailButtonDrawableNormal : R.attr.DetailButtonDrawableDisabled));

        ImageView imgDoer = findViewById(R.id.imgDoer);
        ImageView imgDoee = findViewById(R.id.imgDoee);
        LinearLayout lytText = findViewById(R.id.lytText);

        if(report.GetLeftID() != LaunchReport.ID_NOBODY)
        {
            if(report.GetLeftIDAlliance())
            {
                Alliance alliance = game.GetAlliance(report.GetLeftID());

                if (alliance != null)
                {
                    imgDoer.setImageBitmap(AvatarBitmaps.GetAllianceAvatar(activity, game, alliance));
                }
            }
            else
            {
                Player doer = game.GetPlayer(report.GetLeftID());

                if (doer != null)
                {
                    imgDoer.setImageBitmap(AvatarBitmaps.GetPlayerAvatar(activity, game, doer));
                }
            }
        }

        if(report.GetRightID() != LaunchReport.ID_NOBODY)
        {
            if(report.GetRightIDAlliance())
            {
                Alliance alliance = game.GetAlliance(report.GetRightID());

                if (alliance != null)
                {
                    imgDoee.setImageBitmap(AvatarBitmaps.GetAllianceAvatar(activity, game, alliance));
                }
            }
            else
            {
                Player doee = game.GetPlayer(report.GetRightID());

                if(doee != null)
                {
                    imgDoee.setImageBitmap(AvatarBitmaps.GetPlayerAvatar(activity, game, doee));
                }
            }
        }

        if(report.HasTimeRange())
        {
            lytText.addView(TextViewFromText(TextUtilities.GetDateAndTimeRange(report.GetStartTime(), report.GetEndTime())));
        }
        else
        {
            lytText.addView(TextViewFromText(TextUtilities.GetDateAndTime(report.GetStartTime())));
        }

        lytText.addView(TextViewFromText(report.GetMessage()));
    }

    private TextView TextViewFromText(String strText)
    {
        TextView textView = new TextView(context);
        textView.setText(strText);
        return textView;
    }

    @Override
    public void Update()
    {

    }
}
