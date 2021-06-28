package com.apps.fast.launch.launchviews;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.UI.AvatarBitmaps;
import com.apps.fast.launch.UI.LaunchUICommon;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.ClientDefs;
import com.apps.fast.launch.components.Sounds;
import com.apps.fast.launch.components.TextUtilities;
import com.apps.fast.launch.components.Utilities;
import com.apps.fast.launch.notifications.LaunchAlertManager;
import com.apps.fast.launch.views.LaunchDialog;

import launch.game.LaunchClientGame;
import launch.game.entities.LaunchEntity;

/**
 * Created by tobster on 20/10/15.
 */
public class SettingsView extends LaunchView
{
    private CheckBox chkNotifications;
    private LinearLayout lytNotificationSettings;
    private CheckBox chkNukeEscalationNotifications;
    private CheckBox chkAllyNotifications;
    private CheckBox chkDebugNotifications;
    private TextView txtNotifications;
    private TextView txtInterval;
    private TextView btnTest;

    private CheckBox chkMeters;
    private CheckBox chkYards;
    private CheckBox chkFeet;
    private CheckBox chkKilometers;
    private CheckBox chkStatMiles;
    private CheckBox chkNautMiles;
    private CheckBox chkKph;
    private CheckBox chkMph;
    private CheckBox chkKts;

    private TextView txtCurrency;

    private LinearLayout btnChangeAvatar;
    private TextView btnCloseAccount;

    private CheckBox chkDisableAudio;

    private TextView txtClustering;
    private TextView txtDefaultZoom;

    private TextView btnTheme;

    private TextView txtRenamePlayer;
    private EditText txtNameEdit;
    private LinearLayout btnApplyName;

    private EditText txtURL;
    private EditText txtPort;

    private LinearLayout lytDebug;

    private int lNotificationInterval;
    private int lClusteringIcons;
    private float fltDefaultZoom;

    private String strURL;
    private int lPort;

    private int lInitialAvatar = ClientDefs.DEFAULT_AVATAR_ID;

    public SettingsView(LaunchClientGame game, MainActivity activity)
    {
        super(game, activity);
    }

    @Override
    protected void Setup()
    {
        inflate(context, R.layout.view_settings, this);

        chkNotifications = findViewById(R.id.chkNotifications);
        lytNotificationSettings = findViewById(R.id.lytNotificationSettings);
        chkNukeEscalationNotifications = findViewById(R.id.chkNukeEscalationNotifications);
        chkAllyNotifications = findViewById(R.id.chkAllyNotifications);
        chkDebugNotifications = findViewById(R.id.chkDebugNotifications);
        txtNotifications = findViewById(R.id.txtNotifications);
        txtInterval = findViewById(R.id.txtInterval);
        btnTest = findViewById(R.id.btnTest);

        chkMeters = findViewById(R.id.chkMeters);
        chkYards = findViewById(R.id.chkYards);
        chkFeet = findViewById(R.id.chkFeet);
        chkKilometers = findViewById(R.id.chkKilometers);
        chkStatMiles = findViewById(R.id.chkStatMiles);
        chkNautMiles = findViewById(R.id.chkNautMiles);
        chkKph = findViewById(R.id.chkKph);
        chkMph = findViewById(R.id.chkMph);
        chkKts = findViewById(R.id.chkKts);

        txtCurrency = findViewById(R.id.txtCurrency);

        btnChangeAvatar = findViewById(R.id.btnChangeAvatar);
        btnCloseAccount = findViewById(R.id.btnCloseAccount);

        chkDisableAudio = findViewById(R.id.chkDisableAudio);

        txtClustering = findViewById(R.id.txtClustering);
        txtDefaultZoom = findViewById(R.id.txtDefaultZoom);

        btnTheme = findViewById(R.id.btnTheme);

        txtRenamePlayer = findViewById(R.id.txtRenamePlayer);
        txtNameEdit = findViewById(R.id.txtNameEdit);
        btnApplyName = findViewById(R.id.btnApplyName);

        txtURL = findViewById(R.id.txtURL);
        txtPort = findViewById(R.id.txtPort);

        lytDebug = findViewById(R.id.lytDebug);

        SharedPreferences sharedPreferences = context.getSharedPreferences(ClientDefs.SETTINGS, Context.MODE_PRIVATE);

        strURL = sharedPreferences.getString(ClientDefs.SETTINGS_SERVER_URL, ClientDefs.SETTINGS_SERVER_URL_DEFAULT);
        lPort = sharedPreferences.getInt(ClientDefs.SETTINGS_SERVER_PORT, ClientDefs.GetDefaultServerPort());

        lNotificationInterval = sharedPreferences.getInt(ClientDefs.SETTINGS_NOTIFICATION_MINUTES, ClientDefs.SETTINGS_NOTIFICATION_MINUTES_DEFAULT);

        chkMeters.setChecked(sharedPreferences.getInt(ClientDefs.SETTINGS_SHORT_UNITS, ClientDefs.SETTINGS_UNITS_DEFAULT) == TextUtilities.ShortUnits.METERS.ordinal());
        chkYards.setChecked(sharedPreferences.getInt(ClientDefs.SETTINGS_SHORT_UNITS, ClientDefs.SETTINGS_UNITS_DEFAULT) == TextUtilities.ShortUnits.YARDS.ordinal());
        chkFeet.setChecked(sharedPreferences.getInt(ClientDefs.SETTINGS_SHORT_UNITS, ClientDefs.SETTINGS_UNITS_DEFAULT) == TextUtilities.ShortUnits.FEET.ordinal());

        chkKilometers.setChecked(sharedPreferences.getInt(ClientDefs.SETTINGS_LONG_UNITS, ClientDefs.SETTINGS_UNITS_DEFAULT) == TextUtilities.LongUnits.KILOMETERS.ordinal());
        chkStatMiles.setChecked(sharedPreferences.getInt(ClientDefs.SETTINGS_LONG_UNITS, ClientDefs.SETTINGS_UNITS_DEFAULT) == TextUtilities.LongUnits.STATUTE_MILES.ordinal());
        chkNautMiles.setChecked(sharedPreferences.getInt(ClientDefs.SETTINGS_LONG_UNITS, ClientDefs.SETTINGS_UNITS_DEFAULT) == TextUtilities.LongUnits.NAUTICAL_MILES.ordinal());

        chkKph.setChecked(sharedPreferences.getInt(ClientDefs.SETTINGS_SPEEDS, ClientDefs.SETTINGS_UNITS_DEFAULT) == TextUtilities.Speeds.KILOMETERS_PER_HOUR.ordinal());
        chkMph.setChecked(sharedPreferences.getInt(ClientDefs.SETTINGS_SPEEDS, ClientDefs.SETTINGS_UNITS_DEFAULT) == TextUtilities.Speeds.MILES_PER_HOUR.ordinal());
        chkKts.setChecked(sharedPreferences.getInt(ClientDefs.SETTINGS_SPEEDS, ClientDefs.SETTINGS_UNITS_DEFAULT) == TextUtilities.Speeds.KNOTS.ordinal());

        txtCurrency.setText(sharedPreferences.getString(ClientDefs.SETTINGS_CURRENCY, ClientDefs.SETTINGS_CURRENCY_DEFAULT));

        if(lNotificationInterval > 0)
        {
            txtNotifications.setVisibility(View.VISIBLE);
            txtInterval.setVisibility(View.VISIBLE);
            txtNotifications.setText(Integer.toString(lNotificationInterval));
        }
        else
        {
            txtNotifications.setVisibility(View.INVISIBLE);
            txtInterval.setVisibility(View.INVISIBLE);
            txtNotifications.setText("");
        }

        txtNotifications.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable)
            {
                try
                {
                    lNotificationInterval = Integer.parseInt(txtNotifications.getText().toString());
                }
                catch(Exception ex)
                {
                    lNotificationInterval = ClientDefs.SETTINGS_NOTIFICATION_MINUTES_DEFAULT;
                }
            }
        });

        chkNotifications.setChecked(lNotificationInterval > 0);
        lytNotificationSettings.setVisibility(lNotificationInterval > 0 ? VISIBLE : GONE);
        chkNotifications.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b)
            {
                if(b)
                {
                    lNotificationInterval = ClientDefs.SETTINGS_NOTIFICATION_MINUTES_DEFAULT;
                    txtNotifications.setVisibility(VISIBLE);
                    txtNotifications.setText(Integer.toString(lNotificationInterval));
                    txtInterval.setVisibility(VISIBLE);
                    lytNotificationSettings.setVisibility(VISIBLE);
                    chkNukeEscalationNotifications.setVisibility(VISIBLE);
                    chkAllyNotifications.setVisibility(VISIBLE);
                }
                else
                {
                    lNotificationInterval = 0;
                    txtNotifications.setText("");
                    txtNotifications.setVisibility(GONE);
                    txtInterval.setVisibility(GONE);
                    lytNotificationSettings.setVisibility(GONE);
                    chkNukeEscalationNotifications.setVisibility(GONE);
                    chkAllyNotifications.setVisibility(GONE);
                }
            }
        });

        chkNukeEscalationNotifications.setChecked(sharedPreferences.getBoolean(ClientDefs.SETTINGS_NOTIFICATION_NUKEESC, ClientDefs.SETTINGS_NOTIFICATION_NUKEESC_DEFAULT));
        chkNukeEscalationNotifications.setVisibility(lNotificationInterval > 0 ? VISIBLE : GONE);

        chkAllyNotifications.setChecked(sharedPreferences.getBoolean(ClientDefs.SETTINGS_NOTIFICATION_ALLIES, ClientDefs.SETTINGS_NOTIFICATION_ALLIES_DEFAULT));
        chkAllyNotifications.setVisibility(lNotificationInterval > 0 ? VISIBLE : GONE);

        if(game.GetInteractionReady())
        {
            if (game.GetOurPlayer().GetIsAnAdmin())
            {
                chkDebugNotifications.setVisibility(VISIBLE);
                chkDebugNotifications.setChecked(sharedPreferences.getBoolean(ClientDefs.SETTINGS_NOTIFICATION_DEBUG, ClientDefs.SETTINGS_NOTIFICATION_DEBUG_DEFAULT));
            }
        }

        final CompoundButton.OnCheckedChangeListener shortUnitChangedListener = new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b)
            {
                chkMeters.setOnCheckedChangeListener(null);
                chkYards.setOnCheckedChangeListener(null);
                chkFeet.setOnCheckedChangeListener(null);
                chkMeters.setChecked(compoundButton == chkMeters);
                chkYards.setChecked(compoundButton == chkYards);
                chkFeet.setChecked(compoundButton == chkFeet);
                chkMeters.setOnCheckedChangeListener(this);
                chkYards.setOnCheckedChangeListener(this);
                chkFeet.setOnCheckedChangeListener(this);
            }
        };

        CompoundButton.OnCheckedChangeListener longUnitChangedListener = new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b)
            {
                chkKilometers.setOnCheckedChangeListener(null);
                chkStatMiles.setOnCheckedChangeListener(null);
                chkNautMiles.setOnCheckedChangeListener(null);
                chkKilometers.setChecked(compoundButton == chkKilometers);
                chkStatMiles.setChecked(compoundButton == chkStatMiles);
                chkNautMiles.setChecked(compoundButton == chkNautMiles);
                chkKilometers.setOnCheckedChangeListener(this);
                chkStatMiles.setOnCheckedChangeListener(this);
                chkNautMiles.setOnCheckedChangeListener(this);
            }
        };

        CompoundButton.OnCheckedChangeListener speedUnitChangedListener = new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b)
            {
                chkKph.setOnCheckedChangeListener(null);
                chkMph.setOnCheckedChangeListener(null);
                chkKts.setOnCheckedChangeListener(null);
                chkKph.setChecked(compoundButton == chkKph);
                chkMph.setChecked(compoundButton == chkMph);
                chkKts.setChecked(compoundButton == chkKts);
                chkKph.setOnCheckedChangeListener(this);
                chkMph.setOnCheckedChangeListener(this);
                chkKts.setOnCheckedChangeListener(this);
            }
        };

        chkMeters.setOnCheckedChangeListener(shortUnitChangedListener);
        chkYards.setOnCheckedChangeListener(shortUnitChangedListener);
        chkFeet.setOnCheckedChangeListener(shortUnitChangedListener);
        chkKilometers.setOnCheckedChangeListener(longUnitChangedListener);
        chkStatMiles.setOnCheckedChangeListener(longUnitChangedListener);
        chkNautMiles.setOnCheckedChangeListener(longUnitChangedListener);
        chkKph.setOnCheckedChangeListener(speedUnitChangedListener);
        chkMph.setOnCheckedChangeListener(speedUnitChangedListener);
        chkKts.setOnCheckedChangeListener(speedUnitChangedListener);

        btnTest.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                SaveSettings();
                LaunchAlertManager.FireAlert(context, false, false);
            }
        });

        btnChangeAvatar.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                activity.SetView(new UploadAvatarView(game, activity, LaunchUICommon.AvatarPurpose.PLAYER));
            }
        });

        btnCloseAccount.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                final LaunchDialog launchDialog = new LaunchDialog();
                launchDialog.SetHeaderLaunch();
                launchDialog.SetMessage(context.getString(R.string.close_account_confirm, TextUtilities.GetTimeAmount(game.GetConfig().GetRemoveTime())));
                launchDialog.SetOnClickYes(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        game.CloseAccount();
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

        chkDisableAudio.setChecked(sharedPreferences.getBoolean(ClientDefs.SETTINGS_DISABLE_AUDIO, ClientDefs.SETTINGS_DISABLE_AUDIO_DEFAULT));

        chkDisableAudio.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b)
            {
                SharedPreferences.Editor editor = context.getSharedPreferences(ClientDefs.SETTINGS, Context.MODE_PRIVATE).edit();
                editor.putBoolean(ClientDefs.SETTINGS_DISABLE_AUDIO, b);
                editor.commit();

                Sounds.SetDisabled(b);
            }
        });

        lClusteringIcons = sharedPreferences.getInt(ClientDefs.SETTINGS_CLUSTERING, ClientDefs.SETTINGS_CLUSTERING_DEFAULT);

        txtClustering.setText(Integer.toString(lClusteringIcons));

        txtClustering.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable)
            {
                try
                {
                    lClusteringIcons = Integer.parseInt(txtClustering.getText().toString());
                }
                catch(Exception ex)
                {
                    lClusteringIcons = 0;
                }
            }
        });

        fltDefaultZoom = sharedPreferences.getFloat(ClientDefs.SETTINGS_ZOOM_LEVEL, ClientDefs.SETTINGS_ZOOM_LEVEL_DEFAULT);

        txtDefaultZoom.setText(Float.toString(fltDefaultZoom));

        txtDefaultZoom.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable)
            {
                try
                {
                    fltDefaultZoom = Float.parseFloat(txtDefaultZoom.getText().toString());
                }
                catch(Exception ex)
                {
                    fltDefaultZoom = 0.0f;
                }
            }
        });

        btnTheme.setText(context.getString(R.string.theme, ClientDefs.Themes[sharedPreferences.getInt(ClientDefs.SETTINGS_THEME, ClientDefs.SETTINGS_THEME_DEFAULT)]));

        btnTheme.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                //Display dialog with sort by options.
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(context.getString(R.string.select_theme));

                SharedPreferences sharedPreferences = context.getSharedPreferences(ClientDefs.SETTINGS, Context.MODE_PRIVATE);

                builder.setSingleChoiceItems(ClientDefs.Themes, sharedPreferences.getInt(ClientDefs.SETTINGS_THEME, ClientDefs.SETTINGS_THEME_DEFAULT), new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        //Exceptionally, this is saved and applied on change.
                        SharedPreferences.Editor editor = context.getSharedPreferences(ClientDefs.SETTINGS, Context.MODE_PRIVATE).edit();
                        editor.putInt(ClientDefs.SETTINGS_THEME, i);
                        editor.commit();
                        dialogInterface.dismiss();
                        btnTheme.setText(context.getString(R.string.theme, ClientDefs.Themes[i]));
                        SaveSettings();

                        activity.recreate();
                    }
                });

                builder.show();
            }
        });

        if(game.GetInteractionReady())
        {
            txtNameEdit.setText(game.GetOurPlayer().GetName());
        }
        else
        {
            txtRenamePlayer.setVisibility(GONE);
            txtNameEdit.setVisibility(GONE);
            btnApplyName.setVisibility(GONE);
        }

        btnApplyName.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Utilities.DismissKeyboard(activity, activity.getCurrentFocus());

                //Clear any existing errors.
                txtNameEdit.setError(null);

                //Get the values.
                String strPlayer = txtNameEdit.getText().toString();

                if(Utilities.ValidateName(strPlayer))
                {
                    game.SetPlayerName(txtNameEdit.getText().toString());
                }
                else
                {
                    txtNameEdit.setError(context.getString(R.string.specify_username));
                }

                Utilities.DismissKeyboard(activity, txtNameEdit);
            }
        });

        txtURL.setText(strURL);
        txtPort.setText(Integer.toString(lPort));

        findViewById(R.id.btnPrivacyZones).setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                activity.PrivacyZoneMode();
            }
        });

        if(game.GetInteractionReady())
        {
            if (game.GetOurPlayer().GetAvatarID() != ClientDefs.DEFAULT_AVATAR_ID)
            {
                lInitialAvatar = game.GetOurPlayer().GetAvatarID();
                ((ImageView) findViewById(R.id.imgAvatar)).setImageBitmap(AvatarBitmaps.GetPlayerAvatar(activity, game, game.GetOurPlayer()));
            }
        }
        else
        {
            findViewById(R.id.imgAvatar).setVisibility(GONE);

            btnCloseAccount.setVisibility(GONE);
        }

        RedrawDebug();
    }

    @Override
    public void Update()
    {
    }

    public void SaveSettings()
    {
        if(lNotificationInterval < ClientDefs.SETTINGS_NOTIFICATION_MINUTES_DEFAULT)
        {
            lNotificationInterval = ClientDefs.SETTINGS_NOTIFICATION_MINUTES_DEFAULT;
            txtNotifications.setText(Integer.toString(lNotificationInterval));
        }

        //Save the settings.
        SharedPreferences.Editor editor = context.getSharedPreferences(ClientDefs.SETTINGS, Context.MODE_PRIVATE).edit();
        editor.putInt(ClientDefs.SETTINGS_NOTIFICATION_MINUTES, lNotificationInterval);
        editor.putInt(ClientDefs.SETTINGS_CLUSTERING, lClusteringIcons);
        editor.putFloat(ClientDefs.SETTINGS_ZOOM_LEVEL, fltDefaultZoom);

        ClientDefs.CLUSTERING_SIZE = lClusteringIcons;

        if(chkNukeEscalationNotifications.isChecked())
            editor.putBoolean(ClientDefs.SETTINGS_NOTIFICATION_NUKEESC, true);
        else
            editor.putBoolean(ClientDefs.SETTINGS_NOTIFICATION_NUKEESC, false);

        if(chkAllyNotifications.isChecked())
            editor.putBoolean(ClientDefs.SETTINGS_NOTIFICATION_ALLIES, true);
        else
            editor.putBoolean(ClientDefs.SETTINGS_NOTIFICATION_ALLIES, false);

        if(chkDebugNotifications.isChecked())
            editor.putBoolean(ClientDefs.SETTINGS_NOTIFICATION_DEBUG, true);
        else
            editor.putBoolean(ClientDefs.SETTINGS_NOTIFICATION_DEBUG, false);

        if(chkMeters.isChecked())
            editor.putInt(ClientDefs.SETTINGS_SHORT_UNITS, TextUtilities.ShortUnits.METERS.ordinal());
        else if(chkYards.isChecked())
            editor.putInt(ClientDefs.SETTINGS_SHORT_UNITS, TextUtilities.ShortUnits.YARDS.ordinal());
        else
            editor.putInt(ClientDefs.SETTINGS_SHORT_UNITS, TextUtilities.ShortUnits.FEET.ordinal());

        if(chkKilometers.isChecked())
            editor.putInt(ClientDefs.SETTINGS_LONG_UNITS, TextUtilities.LongUnits.KILOMETERS.ordinal());
        else if(chkStatMiles.isChecked())
            editor.putInt(ClientDefs.SETTINGS_LONG_UNITS, TextUtilities.LongUnits.STATUTE_MILES.ordinal());
        else
            editor.putInt(ClientDefs.SETTINGS_LONG_UNITS, TextUtilities.LongUnits.NAUTICAL_MILES.ordinal());

        if(chkKph.isChecked())
            editor.putInt(ClientDefs.SETTINGS_SPEEDS, TextUtilities.Speeds.KILOMETERS_PER_HOUR.ordinal());
        else if(chkMph.isChecked())
            editor.putInt(ClientDefs.SETTINGS_SPEEDS, TextUtilities.Speeds.MILES_PER_HOUR.ordinal());
        else
            editor.putInt(ClientDefs.SETTINGS_SPEEDS, TextUtilities.Speeds.KNOTS.ordinal());

        editor.putString(ClientDefs.SETTINGS_CURRENCY, txtCurrency.getText().toString());

        String strNewURL = txtURL.getText().toString();
        int lNewPort = 0;
        boolean bConnectionChangeFailed = false;
        boolean bConnectionDidChange = false;

        try
        {
            lNewPort = Integer.parseInt(txtPort.getText().toString());
        }
        catch(Exception ex)
        {
            bConnectionChangeFailed = true;
        }

        if(lNewPort < 1000 || lNewPort > 65535)
        {
            bConnectionChangeFailed = true;
        }

        if(lNewPort != lPort || !strNewURL.equals(strURL))
        {
            //New connection settings. Check them.
            if(bConnectionChangeFailed)
            {
                activity.ShowBasicOKDialog(context.getString(R.string.invalid_connection));
            }
            else
            {
                editor.putString(ClientDefs.SETTINGS_SERVER_URL, strNewURL);
                editor.putInt(ClientDefs.SETTINGS_SERVER_PORT, lNewPort);
                bConnectionDidChange = true;
            }
        }

        editor.commit();

        //Set alert notification interval.
        LaunchAlertManager.CheckIntervalChanged(context);

        TextUtilities.Initialise(context);

        activity.SettingsChanged();

        RedrawDebug();

        if(bConnectionDidChange)
        {
            //Purge all client stuff (config, avatars and imageassets) as they will differ on the new server.
            activity.PurgeClient();

            activity.recreate();
        }
    }

    private void RedrawDebug()
    {
        lytDebug.removeAllViews();

        SharedPreferences sharedPreferences = context.getSharedPreferences(ClientDefs.SETTINGS, Context.MODE_PRIVATE);

        if(sharedPreferences.getBoolean(ClientDefs.SETTINGS_NOTIFICATION_DEBUG, ClientDefs.SETTINGS_NOTIFICATION_DEBUG_DEFAULT))
        {
            sharedPreferences = context.getSharedPreferences(ClientDefs.SETTINGS, Context.MODE_PRIVATE);

            int lDebugIndex = sharedPreferences.getInt(ClientDefs.DEBUG_INDEX, 0);

            int lEntriesDone = 0;

            while(lEntriesDone < ClientDefs.NOTIFICATION_DEBUG_LOG_SIZE)
            {
                String strDebug = sharedPreferences.getString(ClientDefs.DEBUG_PREFIX + lDebugIndex++, "");

                if(lDebugIndex >= ClientDefs.NOTIFICATION_DEBUG_LOG_SIZE)
                    lDebugIndex = 0;

                if(!strDebug.equals(""))
                {
                    TextView txtView = new TextView(context);
                    txtView.setText(strDebug);
                    lytDebug.addView(txtView, 0);
                }

                lEntriesDone++;
            }
        }
    }

    @Override
    public void AvatarSaved(int lAvatarID)
    {
        if(game.GetOurPlayer().GetAvatarID() == lAvatarID)
        {
            lInitialAvatar = lAvatarID;
            ((ImageView) findViewById(R.id.imgAvatar)).setImageBitmap(AvatarBitmaps.GetPlayerAvatar(activity, game, game.GetOurPlayer()));
        }
    }

    @Override
    public void EntityUpdated(LaunchEntity entity)
    {
        if(game.GetOurPlayer().ApparentlyEquals(entity))
        {
            if(game.GetOurPlayer().GetAvatarID() != lInitialAvatar)
            {
                lInitialAvatar = game.GetOurPlayer().GetAvatarID();
                ((ImageView) findViewById(R.id.imgAvatar)).setImageBitmap(AvatarBitmaps.GetPlayerAvatar(activity, game, game.GetOurPlayer()));
            }
        }
    }
}
