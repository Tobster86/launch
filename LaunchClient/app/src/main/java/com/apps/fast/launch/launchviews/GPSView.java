package com.apps.fast.launch.launchviews;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.Locatifier;
import com.apps.fast.launch.components.TextUtilities;
import com.apps.fast.launch.components.Utilities;

import java.util.Queue;

import launch.game.Defs;
import launch.game.LaunchClientGame;
import launch.utilities.LaunchClientLocation;

/**
 * Created by tobster on 16/10/15.
 */
public class GPSView extends LaunchView
{
    private TextView txtUsingLocation;
    private TextView txtNetworkUnavail;
    private TextView txtGPSUnavail;
    private TextView txtUpdatesNone;

    public GPSView(LaunchClientGame game, MainActivity activity)
    {
        super(game, activity);
    }

    @Override
    protected void Setup()
    {
        inflate(context, R.layout.view_gps, this);

        txtUsingLocation = (TextView)findViewById(R.id.txtUsingLocation);
        txtNetworkUnavail = (TextView)findViewById(R.id.txtNetworkUnavail);
        txtGPSUnavail = (TextView)findViewById(R.id.txtGPSUnavail);
        txtUpdatesNone = (TextView)findViewById(R.id.txtUpdatesNone);
    }

    @Override
    public void Update()
    {
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                Locatifier locatifier = activity.GetLocatifier();
                LaunchClientLocation location = locatifier.GetLocation();

                if(location != null)
                {
                    Locatifier.Quality quality = locatifier.GetLocationQuality();

                    txtUsingLocation.setText(context.getString(R.string.using_location, location.GetProvider(), location.GetLatitude(), location.GetLongitude(), TextUtilities.GetDistanceStringFromM(location.GetAccuracy()), TextUtilities.GetTimeAmount(System.currentTimeMillis() - location.GetTime())));
                    txtUsingLocation.setVisibility(View.VISIBLE);

                    if (quality == Locatifier.Quality.GOOD)
                        txtUsingLocation.setTextColor(Utilities.ColourFromAttr(context, R.attr.GoodColour));
                    else if (quality == Locatifier.Quality.A_BIT_OLD)
                        txtUsingLocation.setTextColor(Utilities.ColourFromAttr(context, R.attr.WarningColour));
                    else
                        txtUsingLocation.setTextColor(Utilities.ColourFromAttr(context, R.attr.BadColour));
                }
                else
                {
                    txtUsingLocation.setVisibility(View.GONE);
                }

                Queue<LaunchClientLocation> History = locatifier.GetHistory();

                txtNetworkUnavail.setVisibility(locatifier.GetNetworkAvailable() ? View.GONE : View.VISIBLE);
                txtGPSUnavail.setVisibility(locatifier.GetGPSAvailable() ? View.GONE : View.VISIBLE);
                txtUpdatesNone.setVisibility(History.size() > 0 ? View.GONE : View.VISIBLE);

                if(location != null)
                {
                    LinearLayout lytUpdates = (LinearLayout) findViewById(R.id.lytUpdates);
                    lytUpdates.removeAllViews();

                    for (LaunchClientLocation loc : History)
                    {
                        TextView txt = new TextView(context);
                        txt.setText(context.getString(R.string.location_data, TextUtilities.GetTime(loc.GetTime()), loc.GetProvider(), loc.GetLatitude(), loc.GetLongitude(), TextUtilities.GetDistanceStringFromM(loc.GetAccuracy())));

                        if(loc.GetAccuracy() > game.GetConfig().GetRequiredAccuracy() * Defs.METRES_PER_KM)
                        {
                            //This location would be no good.
                            txt.setTextColor(Utilities.ColourFromAttr(context, R.attr.BadColour));
                        }
                        else if(loc != location)
                        {
                            //This location would be good, but we're not using it.
                            txt.setTextColor(Utilities.ColourFromAttr(context, R.attr.WarningColour));
                        }
                        else
                        {
                            //This is the location we're using.
                            txt.setTextColor(Utilities.ColourFromAttr(context, R.attr.GoodColour));
                        }

                        lytUpdates.addView(txt, 0);
                    }
                }
            }
        });
    }
}
