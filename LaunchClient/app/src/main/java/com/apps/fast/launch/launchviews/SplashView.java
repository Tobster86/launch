package com.apps.fast.launch.launchviews;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.TextUtilities;
import com.apps.fast.launch.components.Utilities;

import launch.game.Config;
import launch.game.Defs;
import launch.game.LaunchClientGame;
import launch.utilities.LaunchClientLocation;

/**
 * Created by tobster on 15/07/16.
 */
public class SplashView extends LaunchView
{
    private static final long GPS_HELP_DELAY = 5000;

    private TextView txtCommunications;
    private TextView txtCommsStatus;
    private TextView txtGPSStatus;
    private TextView txtGPSHelp;

    private TextView txtHorribleFailures;

    private long oDisplayedTime = System.currentTimeMillis();

    public SplashView(LaunchClientGame game, MainActivity activity)
    {
        super(game, activity);
    }

    @Override
    protected void Setup()
    {
        inflate(context, R.layout.main_splash, this);

        txtCommunications = findViewById(R.id.txtCommunications);
        txtCommsStatus = findViewById(R.id.txtCommsStatus);
        txtGPSStatus = findViewById(R.id.txtGPSStatus);
        txtGPSHelp = findViewById(R.id.txtGPSHelp);

        txtHorribleFailures = findViewById(R.id.txtHorribleFailures);

        txtHorribleFailures.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

                StringBuilder exceptions = new StringBuilder();

                for(String string : Utilities.HackilyLoggedExceptions)
                {
                    exceptions.append(string);
                    exceptions.append("\n");
                }

                ClipData clip = ClipData.newPlainText("Launch Exceptions", exceptions.toString());
                clipboard.setPrimaryClip(clip);

                activity.ShowBasicOKDialog("Copied to clipboard");
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
                long oLatency = game.GetLatency();
                LaunchClientLocation location = activity.GetLocatifier().GetLocation();

                if(location != null)
                {
                    Config config = game.GetConfig();
                    float fltAccuracy = location.GetAccuracy();

                    if (config != null)
                    {
                        if (fltAccuracy < config.GetRequiredAccuracy() * Defs.METRES_PER_KM)
                        {
                            txtGPSStatus.setText(context.getString(R.string.main_location_found, TextUtilities.GetDistanceStringFromM(location.GetAccuracy())));
                            txtGPSStatus.setTextColor(Utilities.ColourFromAttr(context, R.attr.GoodColour));

                            txtCommunications.setVisibility(VISIBLE);
                            txtCommsStatus.setVisibility(VISIBLE);

                            if (oLatency != Defs.LATENCY_DISCONNECTED)
                            {
                                txtCommsStatus.setText(context.getString(R.string.main_comms_established, TextUtilities.GetLatencyString(oLatency)));
                                txtCommsStatus.setTextColor(Utilities.ColourFromAttr(context, R.attr.GoodColour));
                            }

                            txtGPSHelp.setVisibility(GONE);
                        }
                        else
                        {
                            txtGPSStatus.setText(context.getString(R.string.main_location_found_not_good_enough, TextUtilities.GetDistanceStringFromM(location.GetAccuracy()), TextUtilities.GetDistanceStringFromKM(config.GetRequiredAccuracy())));
                            txtGPSStatus.setTextColor(Utilities.ColourFromAttr(context, R.attr.WarningColour));

                            txtGPSHelp.setVisibility(System.currentTimeMillis() > oDisplayedTime + GPS_HELP_DELAY ? VISIBLE : GONE);
                        }
                    }
                    else
                    {
                        txtGPSStatus.setText(context.getString(R.string.main_location_found, TextUtilities.GetDistanceStringFromM(location.GetAccuracy())));
                        txtGPSStatus.setTextColor(Utilities.ColourFromAttr(context, R.attr.GoodColour));

                        txtCommunications.setVisibility(VISIBLE);
                        txtCommsStatus.setVisibility(VISIBLE);

                        txtCommsStatus.setText("CONFIG NOT FOUND");
                        txtCommsStatus.setTextColor(Utilities.ColourFromAttr(context, R.attr.BadColour));

                        txtGPSHelp.setVisibility(System.currentTimeMillis() > oDisplayedTime + GPS_HELP_DELAY ? VISIBLE : GONE);
                    }
                }
                else
                {
                    txtGPSHelp.setVisibility(System.currentTimeMillis() > oDisplayedTime + GPS_HELP_DELAY ? VISIBLE : GONE);
                }

                /*StringBuilder exceptions = new StringBuilder();

                for(String string : Utilities.HackilyLoggedExceptions)
                {
                    exceptions.append(string);
                    exceptions.append("\n");
                }

                txtHorribleFailures.setText(exceptions.toString());*/

                /*StringBuilder hackyStatus = new StringBuilder();
                hackyStatus.append("CommsReinit: " + game.GetCommsReinitRemaining() + "\n");
                hackyStatus.append("Comms: " + game.GetComms().GetState() + "\n");
                hackyStatus.append("Session: " + game.GetComms().GetSessionState() + "\n");
                hackyStatus.append("GTS: " + game.GetGameTickStarts() + "\n");
                hackyStatus.append("GTE: " + game.GetGameTickEnds() + "\n");
                hackyStatus.append("CTS: " + game.GetCommTickStarts() + "\n");
                hackyStatus.append("CTE: " + game.GetCommTickEnds());
                txtHorribleFailures.setText(hackyStatus.toString());*/
            }
        });
    }
}
