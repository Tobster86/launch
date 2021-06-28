package com.apps.fast.launch.launchviews;

import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.ClientDefs;
import com.google.android.gms.maps.model.Circle;

import launch.game.Defs;
import launch.game.GeoCoord;
import launch.game.LaunchClientGame;
import launch.utilities.LaunchClientLocation;
import launch.utilities.PrivacyZone;

/**
 * Created by tobster on 09/11/15.
 */
public class PrivacyZonesView extends LaunchView
{
    private TextView txtPrivacyBlurb;
    private TextView txtRadius;
    private SeekBar skbRadius;
    private TextView txtPrivacyMoveOut;
    private TextView btnFinish;
    private TextView btnClearAll;
    private TextView btnSave;
    private TextView btnDelete;

    private PrivacyZone privacyZone;
    private Circle circle;

    public PrivacyZonesView(LaunchClientGame game, MainActivity activity)
    {
        super(game, activity);
    }

    @Override
    protected void Setup()
    {
        inflate(context, R.layout.view_privacy_zones, this);

        txtPrivacyBlurb = (TextView)findViewById(R.id.txtPrivacyBlurb);
        txtRadius = (TextView)findViewById(R.id.txtRadius);
        skbRadius = (SeekBar)findViewById(R.id.skbRadius);
        txtPrivacyMoveOut = (TextView)findViewById(R.id.txtPrivacyMoveOut);
        btnFinish = (TextView)findViewById(R.id.btnFinish);
        btnClearAll = (TextView)findViewById(R.id.btnClearAll);
        btnSave = (TextView)findViewById(R.id.btnSave);
        btnDelete = (TextView)findViewById(R.id.btnDelete);

        skbRadius.setMax(ClientDefs.PRIVACY_ZONE_MAX_RADIUS);

        skbRadius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
                float fltValue = (float)seekBar.getProgress();

                if(privacyZone != null)
                {
                    privacyZone.SetRadius(fltValue);
                }

                if(circle != null)
                {
                    circle.setRadius(fltValue);
                }
            }
        });

        btnFinish.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                activity.SavePrivacyZones();
                activity.ExitPrivacyZones();
            }
        });

        btnClearAll.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                game.ClearPrivacyZones();
                activity.SetSelectedPrivacyZone(null);
                SetPrivacyZone(null);
                activity.RebuildMap();
                activity.GameTicked(0);
            }
        });

        btnSave.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                activity.SavePrivacyZones();
                activity.SetSelectedPrivacyZone(null);
                SetPrivacyZone(null);
                activity.RebuildMap();
                activity.GameTicked(0);
            }
        });

        btnDelete.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                game.RemovePrivacyZone(privacyZone);
                activity.SetSelectedPrivacyZone(null);
                SetPrivacyZone(null);
                activity.RebuildMap();
                activity.GameTicked(0);
            }
        });
    }

    @Override
    public void Update()
    {
        if(privacyZone != null)
        {
            activity.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    LaunchClientLocation location = activity.GetLocatifier().GetLocation();
                    if(location != null)
                    {
                        GeoCoord geoLocation = new GeoCoord(location.GetLatitude(), location.GetLongitude(), true);
                        float fltDistanceToPlayer = geoLocation.DistanceTo(privacyZone.GetPosition()) * Defs.METRES_PER_KM;
                        txtPrivacyMoveOut.setVisibility(fltDistanceToPlayer < privacyZone.GetRadius() ? VISIBLE : GONE);
                    }
                }
            });
        }
    }

    public void SetPrivacyZone(PrivacyZone privacyZone)
    {
        this.privacyZone = privacyZone;
        txtPrivacyMoveOut.setVisibility(GONE);

        if(this.privacyZone == null)
        {
            //Set overview.
            txtPrivacyBlurb.setVisibility(VISIBLE);
            txtRadius.setVisibility(GONE);
            skbRadius.setVisibility(GONE);
            btnFinish.setVisibility(VISIBLE);
            btnClearAll.setVisibility(VISIBLE);
            btnSave.setVisibility(GONE);
            btnDelete.setVisibility(GONE);
        }
        else
        {
            //Set selection.
            txtPrivacyBlurb.setVisibility(GONE);
            txtRadius.setVisibility(VISIBLE);
            skbRadius.setVisibility(VISIBLE);
            btnFinish.setVisibility(GONE);
            btnClearAll.setVisibility(GONE);
            btnSave.setVisibility(VISIBLE);
            btnDelete.setVisibility(VISIBLE);

            skbRadius.setProgress((int)privacyZone.GetRadius());
        }
    }

    public void SetCircle(Circle circle)
    {
        this.circle = circle;
    }
}
