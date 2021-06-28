package com.apps.fast.launch.launchviews;

import android.view.View;
import android.widget.LinearLayout;

import com.apps.fast.launch.R;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.views.ReportView;

import java.util.List;

import launch.game.LaunchClientGame;
import launch.game.entities.Player;
import launch.utilities.LaunchEvent;
import launch.utilities.LaunchReport;

/**
 * Created by tobster on 16/10/15.
 */
public class ReportsView extends LaunchView
{
    private LinearLayout lytReports;

    public ReportsView(LaunchClientGame game, MainActivity activity)
    {
        super(game, activity);
    }

    @Override
    protected void Setup()
    {
        inflate(context, R.layout.view_reports, this);

        lytReports = findViewById(R.id.lytReports);
        lytReports.removeAllViews();

        PopulateWithReports(game.GetOldReports(), false);
        PopulateWithReports(game.GetNewReports(), true);
        game.TransferNewReportsToOld();

        findViewById(R.id.txtNone).setVisibility(game.GetNewReports().size() > 0 || game.GetOldReports().size() > 0 ? View.GONE : View.VISIBLE);
    }

    private void PopulateWithReports(List<LaunchReport> Reports, boolean bNew)
    {
        for(final LaunchReport report : Reports)
        {
            ReportView reportView = new ReportView(game, activity, report, bNew);

            if(!report.GetLeftIDAlliance())
            {
                reportView.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        Player doer = game.GetPlayer(report.GetLeftID());

                        if (doer != null)
                        {
                            activity.SelectEntity(doer);
                        }
                    }
                });
            }

            lytReports.addView(reportView, 0);
        }
    }

    @Override
    public void Update()
    {

    }
}
