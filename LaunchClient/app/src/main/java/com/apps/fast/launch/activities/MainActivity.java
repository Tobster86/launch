package com.apps.fast.launch.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.util.Base64;
import android.view.View;
import android.widget.FrameLayout;

import com.apps.fast.launch.R;
import com.apps.fast.launch.UI.AvatarBitmaps;
import com.apps.fast.launch.UI.EntityIconBitmaps;
import com.apps.fast.launch.UI.map.LaunchClusterItem;
import com.apps.fast.launch.UI.map.LootRenderer;
import com.apps.fast.launch.UI.map.MissileSiteRenderer;
import com.apps.fast.launch.UI.map.OreMineRenderer;
import com.apps.fast.launch.UI.map.SAMSiteRenderer;
import com.apps.fast.launch.UI.map.SelectableMapFragment;
import com.apps.fast.launch.UI.map.SentryGunRenderer;
import com.apps.fast.launch.components.ClientDefs;
import com.apps.fast.launch.components.Locatifier;
import com.apps.fast.launch.components.Sounds;
import com.apps.fast.launch.components.TextUtilities;
import com.apps.fast.launch.components.TutorialController;
import com.apps.fast.launch.components.Utilities;
import com.apps.fast.launch.launchviews.*;
import com.apps.fast.launch.launchviews.controls.AllianceControl;
import com.apps.fast.launch.launchviews.entities.*;
import com.apps.fast.launch.notifications.LaunchAlertManager;
import com.apps.fast.launch.views.LaunchDialog;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.safetynet.SafetyNet;
import com.google.android.gms.safetynet.SafetyNetApi;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import launch.game.Alliance;
import launch.game.Config;
import launch.game.Defs;
import launch.game.GeoCoord;
import launch.game.LaunchClientAppInterface;
import launch.game.LaunchClientGame;
import launch.game.User;
import launch.game.entities.*;
import launch.game.systems.MissileSystem;
import launch.game.treaties.Treaty;
import launch.game.types.InterceptorType;
import launch.game.types.MissileType;
import launch.utilities.LaunchClientLocation;
import launch.utilities.LaunchEvent;
import launch.utilities.LaunchLog;
import launch.utilities.LaunchReport;
import launch.utilities.LaunchUtilities;
import launch.utilities.PrivacyZone;

import launch.comm.clienttasks.Task.TaskMessage;
import launch.utilities.Security;

import static launch.utilities.LaunchLog.LogType.APPLICATION;

/**
 * Created by tobster on 22/09/16.
 */
public class MainActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener, LaunchClientAppInterface, ClusterManager.OnClusterClickListener, ClusterManager.OnClusterItemClickListener, SelectableMapFragment.SelectableMapListener
{
    private static final String LOG_NAME = "MainActivity";

    private static final float TRAJECTORY_LINE_WIDTH = 1.5f;
    private static final int BLAST_RADII_STAGES = 10;
    private static final int PLAYER_MAP_ICON_SIZE = 64;

    private static boolean bRunning = false;    //For notification service.

    public static boolean GetRunning()
    {
        return bRunning;
    }

    public enum InteractionMode
    {
        MAP_UPDATE,             //Google Play Services update required screen.
        PERMISSIONS,            //Permissions screen. Checking and waiting for approval.
        DISCLAIMER,             //Disclaimer screen. No comms yet.
        IDENTITY_WARNING,       //Invalid device identity. No comms yet either.
        SPLASH,                 //Splash screen, communicating with server.
        STANDARD,               //Standard game display.
        DIPLOMACY,              //Diplomacy screens.
        PRIVACY_ZONES,          //Editing privacy zones.
        TARGET_MISSILE,         //Assigning a missile target.
        TARGET_INTERCEPTOR,     //Specifying a missile to shoot down.
        REGISTRATION,           //Player needs to register a username to start.
        CANT_LOG_IN,            //Version is invalid or user is banned.
    }

    public enum ReportsStatus
    {
        NONE,   //No reports have occurred in the marker_player's absense.
        MINOR,  //Reports have occurred, but none that reference the marker_player.
        MAJOR   //Reports have occurred, including one(s) that refence(s) the marker_player.
    }

    private LaunchClientGame game;
    private Locatifier locatifier = null;

    private FrameLayout lytMain;
    private LaunchView CurrentView = null;

    private LaunchDialog commsDialog;

    private LaunchEntity selectedEntity = null;
    private LatLng selectedLocation = null;
    private PrivacyZone selectedPrivacyZone = null;

    private boolean bMapIsSatellite = false;
    private boolean bMapModeZoom = true;

    private boolean bShowFirstLocation = true;
    private boolean bRebuildMap = false;
    private ReportsStatus reportStatus = ReportsStatus.NONE;

    private Map<Marker, PrivacyZone> PrivacyZoneMarkers = new ConcurrentHashMap<>();
    private Map<Integer, Marker> PlayerMarkers = new ConcurrentHashMap<>();
    private Map<Integer, Marker> MissileMarkers = new ConcurrentHashMap<>();
    private Map<Integer, Marker> ECMMarkers = new ConcurrentHashMap<>();
    private Map<Integer, Circle> MissileAttackMarkers = new ConcurrentHashMap<>();
    private Map<Integer, Circle> EMPMarkers = new ConcurrentHashMap<>();
    private Map<Integer, Polyline> MissileTrajectories = new ConcurrentHashMap<>();
    private Map<Integer, Marker> InterceptorMarkers = new ConcurrentHashMap<>();
    private Map<Integer, Polyline> InterceptorTrajectories = new ConcurrentHashMap<>();
    private Map<Integer, LaunchClusterItem> MissileSiteMarkers = new ConcurrentHashMap<>();
    private Map<Integer, LaunchClusterItem> SAMSiteMarkers = new ConcurrentHashMap<>();
    private Map<Integer, LaunchClusterItem> SentryGunMarkers = new ConcurrentHashMap<>();
    private Map<Integer, LaunchClusterItem> OreMineMarkers = new ConcurrentHashMap<>();
    private Map<Integer, LaunchClusterItem> LootMarkers = new ConcurrentHashMap<>();
    private Map<Integer, Circle> RadiationMarkers = new ConcurrentHashMap<>();

    private ClusterManager ClusterManagerLoot;
    private ClusterManager ClusterManagerSAMSites;
    private ClusterManager ClusterManagerSentryGuns;
    private ClusterManager ClusterManagerOreMines;
    private ClusterManager ClusterManagerMissileSites;

    private Marker DotncarryMemorial;

    private InteractionMode interactionMode = InteractionMode.MAP_UPDATE;

    //Entity selection parameters.
    private Polyline targetTrajectory;
    private List<Circle> targetBlastRadii = new ArrayList<>();
    private List<Circle> CircleOverlays = new ArrayList<>();
    private Circle targetRange;
    private boolean bPlayerTargetting;      //The targetting system is attached to the marker_player (as opposed to a structure).
    private int lTargettingSiteID;          //The site ID of whatever is targeting, if not attached to the marker_player.
    private byte cTargettingSlotNo;         //The slot number of the selected missile or marker_interceptor that is being used for targetting.
    private Polyline selectionRect;

    //Self reference for views started in threads.
    private final MainActivity me = this;

    //Map reference.
    private GoogleMap map;

    /** The level to zoom to when zooming in on stuff via a button. */
    private float fltZoomLevel;

    //Tutorial.
    private TutorialController tutorial = new TutorialController();

    //If the game is experiencing a slow-down, this will pace the UI activity by preventing stacking UI activity.
    private boolean bRendering = false;

    //Maximum distance to render ores, for optimisation.
    private float fltLootMaxDistance = 100.0f;

    //Player stuff visibility. To control the amount of Google maps-drawn stuff.
    private Map<Integer, Boolean> PlayerVisibilities = new ConcurrentHashMap();
    private Map<Integer, Boolean> CustomVisibilities = new ConcurrentHashMap();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        //Utilities.StartHackilyLoggingExceptions();

        SharedPreferences sharedPreferences = getSharedPreferences(ClientDefs.SETTINGS, Context.MODE_PRIVATE);
        switch (sharedPreferences.getInt(ClientDefs.SETTINGS_THEME, ClientDefs.SETTINGS_THEME_DEFAULT))
        {
            case ClientDefs.THEME_LAUNCH:
            {
                setTheme(R.style.LaunchTheme);
            }
            break;
            case ClientDefs.THEME_BORING:
            {
                setTheme(R.style.AppTheme);
            }
            break;
        }

        fltZoomLevel = sharedPreferences.getFloat(ClientDefs.SETTINGS_ZOOM_LEVEL, ClientDefs.SETTINGS_ZOOM_LEVEL_DEFAULT);

        ClientDefs.CLUSTERING_SIZE = sharedPreferences.getInt(ClientDefs.SETTINGS_CLUSTERING, ClientDefs.SETTINGS_CLUSTERING_DEFAULT);

        super.onCreate(savedInstanceState);

        LaunchLog.SetConsoleLoggingEnabled(LaunchLog.LogType.SESSION, true);
        LaunchLog.SetConsoleLoggingEnabled(LaunchLog.LogType.COMMS, true);
        LaunchLog.SetConsoleLoggingEnabled(LaunchLog.LogType.APPLICATION, true);
        LaunchLog.SetConsoleLoggingEnabled(LaunchLog.LogType.GAME, true);
        LaunchLog.SetConsoleLoggingEnabled(LaunchLog.LogType.TASKS, true);
        LaunchLog.SetConsoleLoggingEnabled(LaunchLog.LogType.SERVICES, true);
        LaunchLog.SetConsoleLoggingEnabled(LaunchLog.LogType.LOCATIONS, true);

        TextUtilities.Initialise(this);
        Sounds.Init(this);

        bRunning = true;

        //Get and set main UI elements.
        setContentView(R.layout.activity_main);
        lytMain = findViewById(R.id.lytMain);

        //Load config.
        File fileDir = getDir(ClientDefs.SETTINGS, Context.MODE_PRIVATE);
        File file = new File(fileDir, ClientDefs.CONFIG_FILENAME);

        Config config = null;

        if (file.exists())
        {
            RandomAccessFile rafConfig = null;

            try
            {
                rafConfig = new RandomAccessFile(file, "r");
                byte[] cConfig = new byte[(int) rafConfig.length()];
                rafConfig.read(cConfig);

                config = new Config(cConfig);
            }
            catch (Exception ex)
            { /* Don't care; if the stored one is broken or outdated we'll simply download another automatically. */ }
            finally
            {
                if (rafConfig != null)
                {
                    try
                    {
                        rafConfig.close();
                    }
                    catch (Exception ex)
                    { /* Don't care. */ }
                }
            }
        }

        //Load privacy zones.
        List<PrivacyZone> PrivacyZones = new ArrayList<>();

        file = new File(fileDir, ClientDefs.PRIVACY_FILENAME);

        if (file.exists())
        {
            try
            {
                RandomAccessFile rafConfig = new RandomAccessFile(file, "r");
                byte[] cConfig = new byte[(int) rafConfig.length()];
                rafConfig.read(cConfig);

                ByteBuffer bb = ByteBuffer.wrap(cConfig);

                while (bb.hasRemaining())
                {
                    PrivacyZones.add(new PrivacyZone(bb));
                }

                rafConfig.close();
            }
            catch (Exception ex)
            {
            } //Already checked.
        }

        //Load visibility overrides.
        for(String strOverride : sharedPreferences.getStringSet(ClientDefs.SETTINGS_VISIBILITY_OVERRIDES, new HashSet<String>()))
        {
            PlayerVisibilities.put(Integer.parseInt(strOverride), true);
            CustomVisibilities.put(Integer.parseInt(strOverride), true);
        }

        //Fire up the workings.
        String strURL = sharedPreferences.getString(ClientDefs.SETTINGS_SERVER_URL, ClientDefs.SETTINGS_SERVER_URL_DEFAULT);
        int lPort = sharedPreferences.getInt(ClientDefs.SETTINGS_SERVER_PORT, ClientDefs.GetDefaultServerPort());

        //Save the default port now that we've read it (it's random first time).
        SharedPreferences.Editor editor = getSharedPreferences(ClientDefs.SETTINGS, Context.MODE_PRIVATE).edit();
        editor.putInt(ClientDefs.SETTINGS_SERVER_PORT, lPort);
        editor.commit();

        game = new LaunchClientGame(config, this, PrivacyZones, strURL, lPort);

        locatifier = new Locatifier(this, game, this);

        CheckMap();

        ReturnToMainView();
    }

    /**
     * Call when persistent game settings change, and the main activity should refetch them.
     */
    public void SettingsChanged()
    {
        SharedPreferences sharedPreferences = getSharedPreferences(ClientDefs.SETTINGS, Context.MODE_PRIVATE);
        fltZoomLevel = sharedPreferences.getFloat(ClientDefs.SETTINGS_ZOOM_LEVEL, ClientDefs.SETTINGS_ZOOM_LEVEL_DEFAULT);
    }

    /**
     * A recursive directory delete implementation.
     * @param file File reference of directory to delete.
     */
    private void DeleteEntireDirectoryRecursively(File file)
    {
        if (file.isDirectory())
        {
            File[] entries = file.listFiles();
            if (entries != null)
            {
                for (File entry : entries)
                {
                    DeleteEntireDirectoryRecursively(entry);
                }
            }
        }
        else
            file.delete();
    }

    /**
     * Purge all avatars from the client. Used to manage changes to the avatar feature so that any redundancies can be brutally wiped.
     */
    public void PurgeAvatars()
    {
        try
        {
            File fileDir = getDir(ClientDefs.AVATAR_FOLDER, Context.MODE_PRIVATE);
            DeleteEntireDirectoryRecursively(fileDir);
        }
        catch(Exception ex) { /* Don't care. We tried. */ }
    }

    /**
     * Purge all avatars, assets and the config file from the client. SharedPreferences are not affected.
     */
    public void PurgeClient()
    {
        try
        {
            File fileDir = getDir(ClientDefs.SETTINGS, Context.MODE_PRIVATE);
            DeleteEntireDirectoryRecursively(fileDir);
        }
        catch(Exception ex) { /* Don't care. We tried. */ }

        try
        {
            File fileDir = getDir(ClientDefs.IMGASSETS_FOLDER, Context.MODE_PRIVATE);
            DeleteEntireDirectoryRecursively(fileDir);
        }
        catch(Exception ex) { /* Don't care. We tried. */ }

        PurgeAvatars();
    }

    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        map = googleMap;

        SharedPreferences sharedPreferences = getSharedPreferences(ClientDefs.SETTINGS, Context.MODE_PRIVATE);
        switch (sharedPreferences.getInt(ClientDefs.SETTINGS_THEME, ClientDefs.SETTINGS_THEME_DEFAULT))
        {
            case ClientDefs.THEME_LAUNCH:
            {
                googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstylelaunch));
            }
            break;
            case ClientDefs.THEME_BORING:
            { /* Leave default map theme */ }
            break;
        }

        SetMapSatellite(sharedPreferences.getBoolean(ClientDefs.SETTINGS_MAP_SATELLITE, ClientDefs.SETTINGS_MAP_SATELLITE_DEFAULT));

        googleMap.setMyLocationEnabled(false);
        googleMap.setOnMarkerClickListener(this);
        googleMap.setOnMapClickListener(this);
        googleMap.getUiSettings().setRotateGesturesEnabled(false);
        googleMap.getUiSettings().setTiltGesturesEnabled(false);
        googleMap.getUiSettings().setMapToolbarEnabled(false);

        ClusterManagerMissileSites = new ClusterManager(this, map);
        ClusterManagerMissileSites.setRenderer(new MissileSiteRenderer(this, game, map, ClusterManagerMissileSites));
        ClusterManagerSAMSites = new ClusterManager(this, map);
        ClusterManagerSAMSites.setRenderer(new SAMSiteRenderer(this, game, map, ClusterManagerSAMSites));
        ClusterManagerSentryGuns = new ClusterManager(this, map);
        ClusterManagerSentryGuns.setRenderer(new SentryGunRenderer(this, game, map, ClusterManagerSentryGuns));
        ClusterManagerOreMines = new ClusterManager(this, map);
        ClusterManagerOreMines.setRenderer(new OreMineRenderer(this, game, map, ClusterManagerOreMines));
        ClusterManagerLoot = new ClusterManager(this, map);
        ClusterManagerLoot.setRenderer(new LootRenderer(this, map, ClusterManagerLoot));

        ClusterManagerMissileSites.setOnClusterClickListener(this);
        ClusterManagerMissileSites.setOnClusterItemClickListener(this);
        ClusterManagerSAMSites.setOnClusterClickListener(this);
        ClusterManagerSAMSites.setOnClusterItemClickListener(this);
        ClusterManagerSentryGuns.setOnClusterClickListener(this);
        ClusterManagerSentryGuns.setOnClusterItemClickListener(this);
        ClusterManagerOreMines.setOnClusterClickListener(this);
        ClusterManagerOreMines.setOnClusterItemClickListener(this);
        ClusterManagerLoot.setOnClusterClickListener(this);
        ClusterManagerLoot.setOnClusterItemClickListener(this);

        //Cause cluster managers to draw whenever the Google Maps camera goes idle.
        map.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener()
        {
            @Override
            public void onCameraIdle()
            {
                HighLevelUIRefresh();
            }
        });
    }

    @Override
    public boolean onClusterItemClick(ClusterItem clusterItem)
    {
        LaunchClusterItem launchClusterItem = (LaunchClusterItem) clusterItem;
        EntityClicked(launchClusterItem.GetEntity());
        return false;
    }

    @Override
    public boolean onClusterClick(Cluster cluster)
    {
        //TO DO: Show cluster information.
        return false;
    }

    @Override
    protected void onStop()
    {
        super.onStop();

        locatifier.Suspend();
        game.Suspend();
        ClientDefs.StoreMoreVolatileSettings(this);

        //Save custom player visibilities.
        HashSet<String> VisibilityOverridesSet = new HashSet<>();

        for(Map.Entry<Integer, Boolean> entry : CustomVisibilities.entrySet())
        {
            if(entry.getValue())
                VisibilityOverridesSet.add(Integer.toString(entry.getKey()));
        }

        SharedPreferences.Editor editor = getSharedPreferences(ClientDefs.SETTINGS, Context.MODE_PRIVATE).edit();
        editor.putStringSet(ClientDefs.SETTINGS_VISIBILITY_OVERRIDES, VisibilityOverridesSet);
        editor.commit();

        bRunning = false;
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        bRunning = true;

        switch (interactionMode)
        {
            case MAP_UPDATE:
            {
                CheckMap();
            }

            case PERMISSIONS:
            case DISCLAIMER:
            case IDENTITY_WARNING:
            case CANT_LOG_IN:
            {
                /* Don't resume anything in these modes. */
            }
            break;

            default:
            {
                locatifier.Resume();
                game.Resume();
            }
        }
    }

    private void CheckMap()
    {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int lApiAvailabilityResult = apiAvailability.isGooglePlayServicesAvailable(this);

        if (lApiAvailabilityResult == ConnectionResult.SUCCESS)
        {
            //Get and configure the map.
            ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);
            ((SelectableMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).SetListener(this);

            CheckPermissions();
        } else
        {
            apiAvailability.getErrorDialog(this, lApiAvailabilityResult, 0).show();
        }
    }

    private void CheckPermissions()
    {
        if (Utilities.CheckPermissions(this))
        {
            CheckDisclaimer();
        } else
        {
            interactionMode = InteractionMode.PERMISSIONS;
            ReturnToMainView();
        }
    }

    private void CheckDisclaimer()
    {
        SharedPreferences sharedPreferences = getSharedPreferences(ClientDefs.SETTINGS, MODE_PRIVATE);
        locatifier.Resume();

        if (sharedPreferences.getBoolean(ClientDefs.SETTINGS_DISCLAIMER_ACCEPTED, ClientDefs.SETTINGS_DISCLAIMER_ACCEPTED_DEFAULT))
        {
            CheckIdentity();
        } else
        {
            //Present disclaimer.
            interactionMode = InteractionMode.DISCLAIMER;
            ReturnToMainView();
        }
    }

    private void CheckIdentity()
    {
        /*SharedPreferences sharedPreferences = getSharedPreferences(ClientDefs.SETTINGS, MODE_PRIVATE);

        if(Utilities.DeviceHasValidID(this))
        {
            byte[] cDeviceIdentity = Utilities.GetEncryptedDeviceID(this);

            //Store the IMEI so we can check it for spoofing in future.
            SharedPreferences.Editor editor = getSharedPreferences(ClientDefs.SETTINGS, Context.MODE_PRIVATE).edit();
            editor.putString(ClientDefs.SETTINGS_IDENTITY_LAST_READ, Security.BytesToHexString(cDeviceIdentity));
            editor.commit();

            //Present login/register view.
            interactionMode = InteractionMode.SPLASH;
            game.SetDeviceID(Utilities.GetEncryptedDeviceID(this), Utilities.GetLastEncryptedDeviceID(this), Utilities.GetDeviceName(), Utilities.GetProcessName(this), Utilities.GetPoisonBanned(this));
            game.Resume();
        }
        else
        {
            //The device does not have a valid IMEI, nor has a random identity been generated. Display warning.
            interactionMode = InteractionMode.IDENTITY_WARNING;
        }

        ReturnToMainView();*/

        if (Utilities.DeviceHasValidID(this))
        {
            GoToGame();
        } else
        {
            //The device does not have a valid IMEI, nor has a random identity been generated. Display warning.
            interactionMode = InteractionMode.IDENTITY_WARNING;
            ReturnToMainView();
        }
    }

    public void GoToGame()
    {
        //Present login/register view.
        interactionMode = InteractionMode.SPLASH;
        game.SetDeviceID(Utilities.GetEncryptedDeviceID(this), Utilities.GetDeviceName(), Utilities.GetProcessName(this));
        game.Resume();
        ReturnToMainView();
    }

    public void SetView(final LaunchView view)
    {
        //MUST BE CALLED FROM A UI THREAD! runOnUiThread not done here, because the LaunchView itself should be created from within a UI thread before this is called.
        //Remove the current view, if applicable.
        if (CurrentView != null)
        {
            lytMain.removeView(CurrentView);
        }

        //Assign the current view and add it to the activity.
        CurrentView = view;
        lytMain.addView(CurrentView);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if (interactionMode == InteractionMode.PERMISSIONS)
        {
            ((PermissionsView) CurrentView).PermissionsUpdated();

            if (Utilities.CheckPermissions(this))
            {
                CheckDisclaimer();
            }
        }
    }

    /**
     * Signature for Android buttons, which pass a view parameter that we don't care about.
     * @param view The button that invoked this function, which we don't care about but have to match a function signature.
     */
    public void ReturnToMainView(View view)
    {
        ReturnToMainView();
    }

    /**
     * Returns to the "main view" from menus etc.
     */
    public void ReturnToMainView()
    {
        Utilities.DismissKeyboard(this, getCurrentFocus());

        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                switch (interactionMode)
                {
                    case DISCLAIMER:
                    {
                        SetView(new DisclaimerView(game, me));
                    }
                    break;

                    case PERMISSIONS:
                    {
                        SetView(new PermissionsView(game, me));
                    }
                    break;

                    case IDENTITY_WARNING:
                    {
                        SetView(new NoIMEIView(game, me));
                    }
                    break;

                    case SPLASH:
                    {
                        if (CurrentView instanceof SettingsView)
                        {
                            //Save settings when leaving settings view.
                            ((SettingsView) CurrentView).SaveSettings();
                        }

                        SetView(new SplashView(game, me));
                    }
                    break;

                    case STANDARD:
                    {
                        if (CurrentView instanceof SettingsView)
                        {
                            //Save settings when leaving settings view.
                            ((SettingsView) CurrentView).SaveSettings();
                            SetView(new MainNormalView(game, me));
                        }
                        else if (CurrentView instanceof UploadAvatarView)
                        {
                            //Return to settings view from various views.
                            SetView(new SettingsView(game, me));
                        }
                        else
                        {
                            SetView(new MainNormalView(game, me));

                            if(selectedEntity != null)
                            {
                                ApplySelectedEntityView();
                            }
                        }
                    }
                    break;

                    case DIPLOMACY:
                    {
                        if (CurrentView instanceof UploadAvatarView)
                        {
                            if (game.GetOurPlayer().GetAllianceMemberID() != Alliance.ALLIANCE_ID_UNAFFILIATED)
                            {
                                //Cancelled avatar upload from alliance control.
                                SetView(new AllianceControl(game, me, game.GetAlliance(game.GetOurPlayer().GetAllianceMemberID())));
                            }
                            else
                            {
                                //Cancelled avatar upload from new alliance view.
                                SetView(new CreateAllianceView(game, me, ClientDefs.DEFAULT_AVATAR_ID));
                            }
                        }
                        else if (CurrentView instanceof AllianceControl)
                        {
                            SetView(new DiplomacyView(game, me));
                        }
                        else
                        {
                            interactionMode = InteractionMode.STANDARD;
                            SetView(new MainNormalView(game, me));
                        }
                    }
                    break;

                    case PRIVACY_ZONES:
                    {
                        SetView(new PrivacyZonesView(game, me));
                    }
                    break;

                    case TARGET_MISSILE:
                    {
                        MainNormalView normalView = new MainNormalView(game, me);
                        SetView(normalView);
                        if (bPlayerTargetting)
                        {
                            normalView.BottomLayoutShowView(new BottomMissileTarget(game, me, cTargettingSlotNo));
                        } else
                        {
                            normalView.BottomLayoutShowView(new BottomMissileTarget(game, me, lTargettingSiteID, cTargettingSlotNo));
                        }
                    }
                    break;

                    case TARGET_INTERCEPTOR:
                    {
                        MainNormalView normalView = new MainNormalView(game, me);
                        SetView(normalView);
                        if (bPlayerTargetting)
                        {
                            normalView.BottomLayoutShowView(new BottomInterceptorTarget(game, me, cTargettingSlotNo));
                        } else
                        {
                            normalView.BottomLayoutShowView(new BottomInterceptorTarget(game, me, lTargettingSiteID, cTargettingSlotNo));
                        }
                    }
                    break;

                    case REGISTRATION:
                    {
                        SetView(new RegisterView(game, me, ClientDefs.DEFAULT_AVATAR_ID));
                    }
                    break;

                    case CANT_LOG_IN:
                    {
                        //Nothing to do.
                    }
                    break;
                }

                GameTicked(0); //Faster UI update.
            }
        });
    }

    public void ExpandView()
    {
        ((MainNormalView) CurrentView).ExpandBottomView();
    }

    public void ContractView()
    {
        ((MainNormalView) CurrentView).ContractBottomView();
    }

    @Override
    public void onBackPressed()
    {
        switch (interactionMode)
        {
            case STANDARD:
            case TARGET_MISSILE:
            case TARGET_INTERCEPTOR:
            case DIPLOMACY:
            {
                InformationMode();
            }
            break;

            default:
            {
                final LaunchDialog launchDialog = new LaunchDialog();
                launchDialog.SetHeaderLaunch();
                launchDialog.SetMessage(getString(R.string.quit));
                launchDialog.SetOnClickYes(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        finish();
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
                launchDialog.show(getFragmentManager(), "");
            }
            break;
        }
    }

    public void ExitPrivacyZones()
    {
        if (game.GetAuthenticated())
        {
            interactionMode = InteractionMode.STANDARD;
        } else
        {
            interactionMode = InteractionMode.REGISTRATION;
        }

        RebuildMap();
        ReturnToMainView();
    }

    public void ShowBasicOKDialog(final String strMessage)
    {
        final Context context = this;

        //Show an 'ok' dialog that merely displays a message.
        if (!isFinishing())
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    final LaunchDialog launchDialog = new LaunchDialog();
                    launchDialog.SetHeaderLaunch();
                    launchDialog.SetMessage(strMessage);
                    launchDialog.SetOnClickOk(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View view)
                        {
                            launchDialog.dismiss();
                        }
                    });
                    launchDialog.show(getFragmentManager(), "");
                }
            });
        }
    }

    public void DisclaimerAgreed()
    {
        CheckIdentity();
    }

    public void LocationsUpdated()
    {
        if (bShowFirstLocation)
        {
            GoTo(locatifier.GetLocation());

            bShowFirstLocation = false;
        }
    }

    /** Redraw, but don't rebuild, the map. Used when entities change to trigger reclustering, but not a rebuild. */
    public void HighLevelUIRefresh()
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                ClusterManagerMissileSites.onCameraIdle();
                ClusterManagerSAMSites.onCameraIdle();
                ClusterManagerSentryGuns.onCameraIdle();
                ClusterManagerOreMines.onCameraIdle();
                ClusterManagerLoot.onCameraIdle();

                ClusterManagerMissileSites.cluster();
                ClusterManagerSAMSites.cluster();
                ClusterManagerSentryGuns.cluster();
                ClusterManagerOreMines.cluster();
                ClusterManagerLoot.cluster();
            }
        });
    }

    public void RebuildMap()
    {
        bRebuildMap = true;
        GameTicked(0);
    }

    public void SetSelectedPrivacyZone(PrivacyZone privacyZone)
    {
        selectedPrivacyZone = privacyZone;
    }

    /**
     * Remove targetting UI elements from the map, when leaving a target control view.
     */
    public void RemoveTargettingMapUI()
    {
        if(targetRange != null)
        {
            targetRange.remove();
        }

        for(Circle targetBlastRadius : targetBlastRadii)
        {
            if (targetBlastRadius != null)
            {
                targetBlastRadius.remove();
            }
        }

        targetBlastRadii.clear();

        if(targetTrajectory != null)
        {
            targetTrajectory.remove();
        }
    }

    public void ClearSelectedEntity()
    {
        selectedEntity = null;
        selectedLocation = null;

        //Used for range rings in standard mode.
        if(targetRange != null)
        {
            targetRange.remove();
            targetRange = null;
        }

        for(Circle circle : CircleOverlays)
        {
            circle.remove();
        }
        CircleOverlays.clear();
    }

    /**
     * Select an entity, setting the selected entity to this and dealing with map/info screen UI etc.
     * @param entity The entity to select.
     */
    public void SelectEntity(LaunchEntity entity)
    {
        ClearSelectedEntity();
        ReturnToMainView();

        selectedEntity = entity;

        if(selectedEntity instanceof Loot)
        {
            targetRange = map.addCircle(new CircleOptions()
                    .center(Utilities.GetLatLng(selectedEntity.GetPosition()))
                    .radius(game.GetConfig().GetRepairSalvageDistance() * Defs.METRES_PER_KM)
                    .fillColor(Utilities.ColourFromAttr(this, R.attr.LootRadiusColour))
                    .strokeWidth(0.0f));
        }

        if(selectedEntity instanceof SentryGun)
        {
            CircleOverlays.add(map.addCircle(new CircleOptions()
                    .center(Utilities.GetLatLng(selectedEntity.GetPosition()))
                    .radius(game.GetConfig().GetSentryGunRange() * Defs.METRES_PER_KM)
                    .strokeColor(Utilities.ColourFromAttr(this, R.attr.InterceptorPathColour))
                    .strokeWidth(5.0f)));
        }

        if(selectedEntity instanceof OreMine)
        {
            CircleOverlays.add(map.addCircle(new CircleOptions()
                    .center(Utilities.GetLatLng(selectedEntity.GetPosition()))
                    .radius(game.GetConfig().GetOreMineRadius() * Defs.METRES_PER_KM)
                    .fillColor(Utilities.ColourFromAttr(this, R.attr.OreRadiusColour))
                    .strokeWidth(0.0f)));

            CircleOverlays.add(map.addCircle(new CircleOptions()
                    .center(Utilities.GetLatLng(selectedEntity.GetPosition()))
                    .radius(game.GetConfig().GetOreMineDiameter() * Defs.METRES_PER_KM)
                    .strokeColor(Utilities.ColourFromAttr(this, R.attr.BadColour))
                    .strokeWidth(5.0f)));
        }

        if(selectedEntity instanceof SAMSite)
        {
            MissileSystem missileSystem = ((SAMSite)selectedEntity).GetInterceptorSystem();

            for(Map.Entry<Byte, Integer> TypeCount : missileSystem.GetTypeCounts().entrySet())
            {
                InterceptorType type = game.GetConfig().GetInterceptorType(TypeCount.getKey());
                float fltRangeRadius = Math.min(ClientDefs.MAX_RANGE_RING_THICKNESS, TypeCount.getValue());

                CircleOverlays.add(map.addCircle(new CircleOptions()
                        .center(Utilities.GetLatLng(selectedEntity.GetPosition()))
                        .radius(game.GetConfig().GetInterceptorRange(type.GetRangeIndex()) * Defs.METRES_PER_KM)
                        .strokeColor(Utilities.ColourFromAttr(this, R.attr.InterceptorPathColour))
                        .strokeWidth(fltRangeRadius)));
            }
        }

        if(selectedEntity instanceof MissileSite)
        {
            MissileSystem missileSystem = ((MissileSite)selectedEntity).GetMissileSystem();

            for(Map.Entry<Byte, Integer> TypeCount : missileSystem.GetTypeCounts().entrySet())
            {
                MissileType type = game.GetConfig().GetMissileType(TypeCount.getKey());
                float fltRangeRadius = Math.min(ClientDefs.MAX_RANGE_RING_THICKNESS, TypeCount.getValue());

                CircleOverlays.add(map.addCircle(new CircleOptions()
                        .center(Utilities.GetLatLng(selectedEntity.GetPosition()))
                        .radius(game.GetConfig().GetMissileRange(type.GetRangeIndex()) * Defs.METRES_PER_KM)
                        .strokeColor(Utilities.ColourFromAttr(this, R.attr.MissilePathColour))
                        .strokeWidth(fltRangeRadius)));
            }
        }

        if(selectedEntity instanceof Structure)
        {
            CircleOverlays.add(map.addCircle(new CircleOptions()
                    .center(Utilities.GetLatLng(selectedEntity.GetPosition()))
                    .radius(game.GetConfig().GetStructureSeparation() * Defs.METRES_PER_KM)
                    .strokeColor(Utilities.ColourFromAttr(this, R.attr.StructureSeparationRadiusColour))
                    .strokeWidth(1.0f)));

            if(((Structure)selectedEntity).GetHPDeficit() != 0)
            {
                CircleOverlays.add(map.addCircle(new CircleOptions()
                        .center(Utilities.GetLatLng(selectedEntity.GetPosition()))
                        .radius(game.GetConfig().GetRepairSalvageDistance() * Defs.METRES_PER_KM)
                        .fillColor(Utilities.ColourFromAttr(this, R.attr.RepairRadiusColour))
                        .strokeWidth(0.0f)));
            }
        }

        ApplySelectedEntityView();
    }

    public void ApplySelectedEntityView()
    {
        if(CurrentView instanceof MainNormalView)
        {
            MainNormalView mainView = (MainNormalView)CurrentView;

            if(selectedEntity instanceof Player)
            {
                mainView.BottomLayoutShowView(new PlayerView(game, this, selectedEntity.GetID()));
            }
            else if(selectedEntity instanceof Missile)
            {
                mainView.BottomLayoutShowView(new MissileView(game, this, selectedEntity.GetID()));
            }
            else if(selectedEntity instanceof Interceptor)
            {
                mainView.BottomLayoutShowView(new InterceptorView(game, this, selectedEntity.GetID()));
            }
            else if(selectedEntity instanceof MissileSite)
            {
                mainView.BottomLayoutShowView(new MissileSiteView(game, this, selectedEntity));
            }
            else if(selectedEntity instanceof SAMSite)
            {
                mainView.BottomLayoutShowView(new SAMSiteView(game, this, selectedEntity));
            }
            else if(selectedEntity instanceof SentryGun)
            {
                mainView.BottomLayoutShowView(new SentryGunView(game, this, selectedEntity));
            }
            else if(selectedEntity instanceof OreMine)
            {
                mainView.BottomLayoutShowView(new OreMineView(game, this, selectedEntity));
            }
            else if(selectedEntity instanceof Loot)
            {
                mainView.BottomLayoutShowView(new LootView(game, this, selectedEntity.GetID()));
            }
        }
    }

    public void GoTo(final LaunchClientLocation location)
    {
        if (map != null)
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    if (location != null)
                    {
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.GetLatitude(), location.GetLongitude()), fltZoomLevel));
                    }
                }
            });
        }
    }

    public void GoToSelectedEntity(final boolean bZoomIn)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                LatLng location = selectedLocation;

                if(selectedEntity != null)
                {
                    location = Utilities.GetLatLng(selectedEntity.GetPosition());
                }

                if(location != null)
                {
                    if (bZoomIn)
                    {
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(location, fltZoomLevel));
                    }
                    else
                    {
                        map.animateCamera(CameraUpdateFactory.newLatLng(location));
                    }
                }
            }
        });
    }

    private void CreateClusterUI(LaunchEntity entity, Map<Integer, LaunchClusterItem> MarkerContainer, ClusterManager clusterManager)
    {
        if (MarkerContainer.containsKey(entity.GetID()))
        {
            clusterManager.removeItem(MarkerContainer.get(entity.GetID()));
            MarkerContainer.remove(entity.GetID());
        }

        LaunchClusterItem clusterItem = new LaunchClusterItem(entity);
        MarkerContainer.put(entity.GetID(), clusterItem);
        clusterManager.addItem(clusterItem);
    }

    /**
     * Create an entity UI, determining its visibility on the way.
     * @param entity The entity to create UI for.
     */
    private void CreateEntityUI(final LaunchEntity entity)
    {
        final Context context = this;
        final MainActivity self = this;

        if(entity instanceof Structure)
        {
            Structure structure = (Structure)entity;

            //Create the visibility profile if one doesn't exist.
            if(!PlayerVisibilities.containsKey(structure.GetOwnerID()))
            {
                switch(game.GetAllegiance(game.GetOurPlayer(), structure))
                {
                    case ALLY:
                    case YOU:
                    case ENEMY:
                    {
                        PlayerVisibilities.put(structure.GetOwnerID(), true);
                    }
                    break;

                    default:
                    {
                        PlayerVisibilities.put(structure.GetOwnerID(), false);
                    }
                }
            }

            //Back out now if it's not visible.
            if(!PlayerVisibilities.get(structure.GetOwnerID()))
            {
                return;
            }
        }

        if(entity.GetPosition().GetValid() && LaunchUtilities.GetEntityVisibility(game, entity))
        {
            if(map != null)
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        //"Non-marker" markers.
                        if (entity instanceof Radiation)
                        {
                            Radiation radiation = (Radiation) entity;

                            if (RadiationMarkers.containsKey(radiation.GetID()))
                            {
                                RadiationMarkers.get(radiation.GetID()).remove();
                                RadiationMarkers.remove(radiation.GetID());
                            }

                            RadiationMarkers.put(radiation.GetID(), map.addCircle(new CircleOptions()
                                    .center(Utilities.GetLatLng(radiation.GetPosition()))
                                    .radius(radiation.GetRadius() * Defs.METRES_PER_KM)
                                    .fillColor(Utilities.ColourFromAttr(context, R.attr.RadiationColour))
                                    .strokeWidth(0.0f)));
                        }
                        else if(entity instanceof MissileSite)
                        {
                            //Clustered missile site markers.
                            CreateClusterUI(entity, MissileSiteMarkers, ClusterManagerMissileSites);
                        }
                        else if(entity instanceof SAMSite)
                        {
                            //Clustered SAM site markers.
                            CreateClusterUI(entity, SAMSiteMarkers, ClusterManagerSAMSites);
                        }
                        else if(entity instanceof SentryGun)
                        {
                            //Clustered sentry gun markers.
                            CreateClusterUI(entity, SentryGunMarkers, ClusterManagerSentryGuns);
                        }
                        else if(entity instanceof OreMine)
                        {
                            //Clustered sentry gun markers.
                            CreateClusterUI(entity, OreMineMarkers, ClusterManagerOreMines);
                        }
                        else if(entity instanceof Loot)
                        {
                            if(fltLootMaxDistance <= 0.0f || entity.GetPosition().DistanceTo(game.GetOurPlayer().GetPosition()) <= fltLootMaxDistance)
                            {
                                //Clustered loot markers.
                                CreateClusterUI(entity, LootMarkers, ClusterManagerLoot);
                            }
                        }
                        else
                        {
                            //Map markers generally.
                            MarkerOptions options = new MarkerOptions();
                            options.position(Utilities.GetLatLng(entity.GetPosition()));
                            options.anchor(0.5f, 0.5f);

                            if (entity instanceof Player)
                            {
                                Player player = (Player) entity;
                                if (!player.GetBanned_Client() && player.GetPosition().GetValid())
                                {
                                    options.icon(GetPlayerIcon(player));
                                    options.alpha(player.GetRespawnProtected() ? 0.5f : 1.0f);

                                    if (PlayerMarkers.containsKey(player.GetID()))
                                    {
                                        PlayerMarkers.get(player.GetID()).remove();
                                        PlayerMarkers.remove(player.GetID());
                                    }

                                    PlayerMarkers.put(player.GetID(), map.addMarker(options));
                                }
                            }
                            else if (entity instanceof Missile)
                            {
                                Missile missile = (Missile) entity;
                                MissileType type = game.GetConfig().GetMissileType(missile.GetType());
                                options.icon(BitmapDescriptorFactory.fromBitmap(EntityIconBitmaps.GetMissileBitmap(self, game, missile, type.GetAssetID())));
                                options.rotation((float) GeoCoord.ToDegrees(entity.GetPosition().GetLastBearing()));

                                if (MissileMarkers.containsKey(missile.GetID()))
                                {
                                    MissileMarkers.get(missile.GetID()).remove();
                                    MissileMarkers.remove(missile.GetID());
                                }

                                MissileMarkers.put(missile.GetID(), map.addMarker(options));

                                if (type.GetECM())
                                {
                                    MarkerOptions optionsECM = new MarkerOptions();
                                    optionsECM.position(Utilities.GetLatLng(entity.GetPosition()));
                                    optionsECM.anchor(0.5f, 0.5f);
                                    optionsECM.rotation((float) GeoCoord.ToDegrees(entity.GetPosition().GetLastBearing()));
                                    optionsECM.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_ecm));

                                    if (ECMMarkers.containsKey(missile.GetID()))
                                    {
                                        ECMMarkers.get(missile.GetID()).remove();
                                        ECMMarkers.remove(missile.GetID());
                                    }

                                    ECMMarkers.put(missile.GetID(), map.addMarker(optionsECM));
                                }

                                if (MissileTrajectories.containsKey(missile.GetID()))
                                {
                                    MissileTrajectories.get(missile.GetID()).remove();
                                    MissileTrajectories.remove(missile.GetID());
                                }

                                GeoCoord geoTarget = game.GetMissileTarget(missile);

                                MissileTrajectories.put(missile.GetID(), map.addPolyline(new PolylineOptions()
                                        .add(Utilities.GetLatLng(missile.GetPosition()))
                                        .add(Utilities.GetLatLng(geoTarget))
                                        .width(TRAJECTORY_LINE_WIDTH)
                                        .geodesic(true)
                                        .color(Utilities.ColourFromAttr(context, missile.GetTracking()? R.attr.MissilePathTrackingColour : R.attr.MissilePathColour))));

                                if (MissileAttackMarkers.containsKey(missile.GetID()))
                                {
                                    MissileAttackMarkers.get(missile.GetID()).remove();
                                    MissileAttackMarkers.remove(missile.GetID());
                                }

                                if (EMPMarkers.containsKey(missile.GetID()))
                                {
                                    EMPMarkers.get(missile.GetID()).remove();
                                    EMPMarkers.remove(missile.GetID());
                                }

                                CircleOptions circleBlast = new CircleOptions()
                                        .center(Utilities.GetLatLng(game.GetMissileTarget(missile)))
                                        .radius(game.GetConfig().GetBlastRadius(type) * Defs.METRES_PER_KM)
                                        .fillColor(Utilities.ColourFromAttr(context, R.attr.BlastRadiusColour))
                                        .strokeWidth(0.0f);

                                MissileAttackMarkers.put(missile.GetID(), map.addCircle(circleBlast));

                                if (type.GetNuclear())
                                {
                                    CircleOptions circleEMP = new CircleOptions()
                                            .center(Utilities.GetLatLng(game.GetMissileTarget(missile)))
                                            .radius(game.GetConfig().GetBlastRadius(type) * game.GetConfig().GetEMPRadiusMultiplier() * Defs.METRES_PER_KM)
                                            .fillColor(Utilities.ColourFromAttr(context, R.attr.EMPColour))
                                            .strokeWidth(0.0f);

                                    EMPMarkers.put(missile.GetID(), map.addCircle(circleEMP));
                                }
                            }
                            else if (entity instanceof Interceptor)
                            {
                                Interceptor interceptor = (Interceptor) entity;
                                InterceptorType type = game.GetConfig().GetInterceptorType(interceptor.GetType());
                                options.icon(BitmapDescriptorFactory.fromBitmap(EntityIconBitmaps.GetInterceptorBitmap(self, game, interceptor, type.GetAssetID())));
                                options.rotation((float) GeoCoord.ToDegrees(entity.GetPosition().GetLastBearing()));

                                if (InterceptorMarkers.containsKey(interceptor.GetID()))
                                {
                                    InterceptorMarkers.get(interceptor.GetID()).remove();
                                    InterceptorMarkers.remove(interceptor.GetID());
                                }

                                InterceptorMarkers.put(interceptor.GetID(), map.addMarker(options));

                                if (InterceptorTrajectories.containsKey(interceptor.GetID()))
                                {
                                    InterceptorTrajectories.get(interceptor.GetID()).remove();
                                    InterceptorTrajectories.remove(interceptor.GetID());
                                }

                                Missile target = game.GetMissile(interceptor.GetTargetID());

                                if (target != null)
                                {
                                    InterceptorTrajectories.put(interceptor.GetID(), map.addPolyline(new PolylineOptions()
                                            .add(Utilities.GetLatLng(interceptor.GetPosition()))
                                            .add(Utilities.GetLatLng(target.GetPosition()))
                                            .width(TRAJECTORY_LINE_WIDTH)
                                            .geodesic(true)
                                            .color(Utilities.ColourFromAttr(context, R.attr.InterceptorPathColour))));
                                }
                            }
                        }
                    }
                });
            }
        }
    }

    public ReportsStatus GetReportsStatus() { return reportStatus; }

    public InteractionMode GetInteractionMode()
    {
        return interactionMode;
    }

    public void InformationMode()
    {
        interactionMode = InteractionMode.STANDARD;
        RemoveTargettingMapUI();
        ReturnToMainView();
    }

    public void PrivacyZoneMode()
    {
        interactionMode = InteractionMode.PRIVACY_ZONES;
        RebuildMap();
        ReturnToMainView();
    }

    public void MissileTargetModePlayer(byte cSlotNo)
    {
        if (game.GetOurPlayer().Destroyed())
        {
            ShowBasicOKDialog(getString(R.string.you_must_respawn));
        }
        else
        {
            interactionMode = InteractionMode.TARGET_MISSILE;
            bPlayerTargetting = true;
            cTargettingSlotNo = cSlotNo;

            ReturnToMainView();

            MissileType type = game.GetConfig().GetMissileType(game.GetOurPlayer().GetMissileSystem().GetSlotMissileType(cSlotNo));

            targetRange = map.addCircle(new CircleOptions()
                    .center(Utilities.GetLatLng(game.GetOurPlayer().GetPosition()))
                    .radius(game.GetConfig().GetMissileRange(type.GetRangeIndex()) * Defs.METRES_PER_KM)
                    .strokeColor(Utilities.ColourFromAttr(this, R.attr.MissilePathColour))
                    .strokeWidth(5.0f));
        }
    }

    public void MissileTargetMode(int lSiteID, byte cSlotNo)
    {
        if (game.GetOurPlayer().Destroyed())
        {
            ShowBasicOKDialog(getString(R.string.you_must_respawn));
        }
        else
        {
            interactionMode = InteractionMode.TARGET_MISSILE;
            bPlayerTargetting = false;
            lTargettingSiteID = lSiteID;
            cTargettingSlotNo = cSlotNo;

            ReturnToMainView();

            MissileSite site = game.GetMissileSite(lSiteID);
            MissileType type = game.GetConfig().GetMissileType(site.GetMissileSystem().GetSlotMissileType(cSlotNo));

            targetRange = map.addCircle(new CircleOptions()
                    .center(Utilities.GetLatLng(site.GetPosition()))
                    .radius(game.GetConfig().GetMissileRange(type.GetRangeIndex()) * Defs.METRES_PER_KM)
                    .strokeColor(Utilities.ColourFromAttr(this, R.attr.MissilePathColour))
                    .strokeWidth(5.0f));
        }
    }

    public void MissileSelectForTarget(final GeoCoord geoTarget, final String strTargetName)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                SetView(new SelectMissileView(game, me, geoTarget, strTargetName));
            }
        });
    }

    public void InterceptorSelectForTarget(final int lTargetMissile, final String strTargetName)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                SetView(new SelectInterceptorView(game, me, lTargetMissile, strTargetName));
            }
        });
    }

    public void DesignateMissileTargetPlayer(byte cSlotNo, GeoCoord geoTarget)
    {
        if (game.GetOurPlayer().Destroyed())
        {
            ShowBasicOKDialog(getString(R.string.you_must_respawn));
        }
        else
        {
            MissileTargetModePlayer(cSlotNo);
            TargetMissile(geoTarget);
        }
    }

    public void DesignateMissileTarget(int lSiteID, byte cSlotNo, GeoCoord geoTarget)
    {
        if (game.GetOurPlayer().Destroyed())
        {
            ShowBasicOKDialog(getString(R.string.you_must_respawn));
        }
        else
        {
            MissileTargetMode(lSiteID, cSlotNo);
            TargetMissile(geoTarget);
        }
    }

    public void DesignateInterceptorTargetPlayer(byte cSlotNo, Missile missile)
    {
        if (game.GetOurPlayer().Destroyed())
        {
            ShowBasicOKDialog(getString(R.string.you_must_respawn));
        }
        else
        {
            InterceptorTargetModePlayer(cSlotNo);
            TargetInterceptor(missile);
        }
    }

    public void DesignateInterceptorTarget(int lSiteID, byte cSlotNo, Missile missile)
    {
        if (game.GetOurPlayer().Destroyed())
        {
            ShowBasicOKDialog(getString(R.string.you_must_respawn));
        }
        else
        {
            InterceptorTargetMode(lSiteID, cSlotNo);
            TargetInterceptor(missile);
        }
    }

    public void InterceptorTargetModePlayer(byte cSlotNo)
    {
        if (game.GetOurPlayer().Destroyed())
        {
            ShowBasicOKDialog(getString(R.string.you_must_respawn));
        }
        else
        {
            interactionMode = InteractionMode.TARGET_INTERCEPTOR;
            bPlayerTargetting = true;
            cTargettingSlotNo = cSlotNo;

            ReturnToMainView();

            InterceptorType type = game.GetConfig().GetInterceptorType(game.GetOurPlayer().GetInterceptorSystem().GetSlotMissileType(cSlotNo));

            targetRange = map.addCircle(new CircleOptions()
                    .center(Utilities.GetLatLng(game.GetOurPlayer().GetPosition()))
                    .radius(game.GetConfig().GetInterceptorRange(type.GetRangeIndex()) * Defs.METRES_PER_KM)
                    .strokeColor(Utilities.ColourFromAttr(this, R.attr.InterceptorPathColour))
                    .strokeWidth(5.0f));
        }
    }

    public void InterceptorTargetMode(int lSiteID, byte cSlotNo)
    {
        if (game.GetOurPlayer().Destroyed())
        {
            ShowBasicOKDialog(getString(R.string.you_must_respawn));
        }
        else
        {
            interactionMode = InteractionMode.TARGET_INTERCEPTOR;
            bPlayerTargetting = false;
            lTargettingSiteID = lSiteID;
            cTargettingSlotNo = cSlotNo;

            ReturnToMainView();

            SAMSite site = game.GetSAMSite(lSiteID);
            InterceptorType type = game.GetConfig().GetInterceptorType(site.GetInterceptorSystem().GetSlotMissileType(cSlotNo));

            targetRange = map.addCircle(new CircleOptions()
                    .center(Utilities.GetLatLng(site.GetPosition()))
                    .radius(game.GetConfig().GetInterceptorRange(type.GetRangeIndex()) * Defs.METRES_PER_KM)
                    .strokeColor(Utilities.ColourFromAttr(this, R.attr.InterceptorPathColour))
                    .strokeWidth(5.0f));
        }
    }

    private void TargetMissile(GeoCoord geoLocation)
    {
        for(Circle targetBlastRadius : targetBlastRadii)
        {
            if (targetBlastRadius != null)
            {
                targetBlastRadius.remove();
            }
        }

        targetBlastRadii.clear();

        if(targetTrajectory != null)
        {
            targetTrajectory.remove();
        }

        GeoCoord geoSource = bPlayerTargetting ? game.GetOurPlayer().GetPosition() : game.GetMissileSite(lTargettingSiteID).GetPosition();

        MissileType type = game.GetConfig().GetMissileType(bPlayerTargetting ?
                game.GetOurPlayer().GetMissileSystem().GetSlotMissileType(cTargettingSlotNo) :
                game.GetMissileSite(lTargettingSiteID).GetMissileSystem().GetSlotMissileType(cTargettingSlotNo));

        targetTrajectory = map.addPolyline(new PolylineOptions()
                .add(Utilities.GetLatLng(geoSource))
                .add(Utilities.GetLatLng(geoLocation))
                .geodesic(true)
                .color(Utilities.ColourFromAttr(this, R.attr.MissilePathColour)));

        if(type.GetNuclear())
        {
            targetBlastRadii.add(map.addCircle(new CircleOptions()
                    .center(Utilities.GetLatLng(geoLocation))
                    .radius(game.GetConfig().GetBlastRadius(type) * game.GetConfig().GetEMPRadiusMultiplier() * Defs.METRES_PER_KM)
                    .fillColor(Utilities.ColourFromAttr(this, R.attr.EMPColour))
                    .strokeWidth(0.0f)));
        }

        for(int i = 1; i <= BLAST_RADII_STAGES; i++)
        {
            targetBlastRadii.add(map.addCircle(new CircleOptions()
                    .center(Utilities.GetLatLng(geoLocation))
                    .radius((game.GetConfig().GetBlastRadius(type) * ((float)i / (float)BLAST_RADII_STAGES) ) * Defs.METRES_PER_KM)
                    .fillColor(Utilities.ColourFromAttr(this, R.attr.BlastRadiusStageColour))
                    .strokeWidth(0.0f)));
        }

        ((BottomMissileTarget)((MainNormalView)CurrentView).GetBottomView()).LocationSelected(geoLocation, targetTrajectory, map);
    }

    private void TargetInterceptor(Missile target)
    {
        if(targetTrajectory != null)
        {
            targetTrajectory.remove();
        }

        GeoCoord geoSource = bPlayerTargetting ? game.GetOurPlayer().GetPosition() : game.GetSAMSite(lTargettingSiteID).GetPosition();

        targetTrajectory = map.addPolyline(new PolylineOptions()
                .add(Utilities.GetLatLng(geoSource))
                .add(Utilities.GetLatLng(target.GetPosition()))
                .geodesic(true)
                .color(Utilities.ColourFromAttr(this, R.attr.InterceptorPathColour)));

        ((BottomInterceptorTarget)((MainNormalView)CurrentView).GetBottomView()).TargetSelected(target, targetTrajectory, map);
    }

    @Override
    public void onMapClick(LatLng latLng)
    {
        switch(interactionMode)
        {
            case STANDARD:
            {
                ClearSelectedEntity();

                if (CurrentView instanceof MainNormalView)
                {
                    selectedLocation = latLng;
                    ((MainNormalView) CurrentView).BottomLayoutShowView(new MapClickView(game, this, latLng));
                }
            }
            break;

            case TARGET_MISSILE:
            {
                TargetMissile(new GeoCoord(latLng.latitude, latLng.longitude, true));
            }
            break;

            case PRIVACY_ZONES:
            {
                PrivacyZone privacyZone = new PrivacyZone(new GeoCoord(latLng.latitude, latLng.longitude, true), ClientDefs.PRIVACY_ZONE_DEFAULT_RADIUS);
                game.AddPrivacyZone(privacyZone);
                selectedPrivacyZone = privacyZone;
                RebuildMap();
                ((PrivacyZonesView)CurrentView).SetPrivacyZone(privacyZone);
            }
            break;
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker)
    {
        ClusterManagerLoot.onMarkerClick(marker);
        ClusterManagerSAMSites.onMarkerClick(marker);
        ClusterManagerMissileSites.onMarkerClick(marker);
        ClusterManagerSentryGuns.onMarkerClick(marker);
        ClusterManagerOreMines.onMarkerClick(marker);

        switch(interactionMode)
        {
            case PRIVACY_ZONES:
            {
                selectedPrivacyZone = null;

                if(PrivacyZoneMarkers.containsKey(marker))
                {
                    selectedPrivacyZone = PrivacyZoneMarkers.get(marker);
                    ((PrivacyZonesView)CurrentView).SetPrivacyZone(selectedPrivacyZone);
                }

                RebuildMap();
            }
            break;

            default:
            {
                if(marker.equals(DotncarryMemorial))
                {
                    ClearSelectedEntity();
                    ReturnToMainView();

                    if(CurrentView instanceof MainNormalView)
                    {
                        MainNormalView mainView = (MainNormalView) CurrentView;
                        mainView.BottomLayoutShowView(new DotncarryMemorialView(game, this));
                    }
                }
                else
                {
                    LaunchEntity entity = GetEntityFromMarker(marker);

                    if (entity != null)
                        EntityClicked(entity);
                }
            }
        }

        return false;
    }

    public LaunchEntity GetEntityFromMarker(Marker marker)
    {
        if(PlayerMarkers.containsValue(marker))
        {
            Integer lID = Utilities.GetMapKeyByValue(PlayerMarkers, marker);

            if (lID != null)
            {
                return game.GetPlayer(lID);
            }
        }

        if(MissileMarkers.containsValue(marker))
        {
            Integer lID = Utilities.GetMapKeyByValue(MissileMarkers, marker);

            if (lID != null)
            {
                return game.GetMissile(lID);
            }
        }

        if(ECMMarkers.containsValue(marker))
        {
            Integer lID = Utilities.GetMapKeyByValue(ECMMarkers, marker);

            if (lID != null)
            {
                return game.GetMissile(lID);
            }
        }

        if(InterceptorMarkers.containsValue(marker))
        {
            Integer lID = Utilities.GetMapKeyByValue(InterceptorMarkers, marker);

            if (lID != null)
            {
                return game.GetInterceptor(lID);
            }
        }

        return null;
    }

    public void EntityClicked(LaunchEntity entity)
    {
        switch(interactionMode)
        {
            case STANDARD:
            {
                SelectEntity(entity);
            }
            break;

            case TARGET_MISSILE:
            {
                TargetMissile(entity.GetPosition());
            }
            break;

            case TARGET_INTERCEPTOR:
            {
                if(entity instanceof Missile)
                {
                    TargetInterceptor((Missile)entity);
                }
            }
            break;
        }
    }

    public void ButtonBuild(View view)
    {
        if(!game.GetInteractionReady())
        {
            ShowBasicOKDialog(getString(R.string.waiting_for_data));
        }
        else if (game.GetOurPlayer().Destroyed())
        {
            ShowBasicOKDialog(getString(R.string.you_must_respawn));
        }
        else
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    SetView(new BuildView(game, me));
                }
            });
        }
    }

    public void ButtonMissiles(View view)
    {
        if(!game.GetInteractionReady())
        {
            ShowBasicOKDialog(getString(R.string.waiting_for_data));
        }
        else if(game.GetOurPlayer().Destroyed())
        {
            ShowBasicOKDialog(getString(R.string.you_must_respawn));
        }
        else
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    SetView(new PlayerMissileView(game, me));
                }
            });
        }
    }

    public void ButtonInterceptors(View view)
    {
        if(!game.GetInteractionReady())
        {
            ShowBasicOKDialog(getString(R.string.waiting_for_data));
        }
        else if(game.GetOurPlayer().Destroyed())
        {
            ShowBasicOKDialog(getString(R.string.you_must_respawn));
        }
        else
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    SetView(new PlayerInterceptorView(game, me));
                }
            });
        }
    }

    public void ButtonDiplomacy(View view)
    {
        if(!game.GetInteractionReady())
        {
            ShowBasicOKDialog(getString(R.string.waiting_for_data));
        }
        else
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    interactionMode = InteractionMode.DIPLOMACY;

                    if(game.PendingDiplomacyItems())
                    {
                        SetView(new DiplomacyActionView(game, me));
                    }
                    else
                    {
                        SetView(new DiplomacyView(game, me));
                    }
                }
            });
        }
    }

    public void ButtonReports(View view)
    {
        if(!game.GetInteractionReady())
        {
            ShowBasicOKDialog(getString(R.string.waiting_for_data));
        }
        else
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    SetView(new ReportsView(game, me));
                    reportStatus = ReportsStatus.NONE;
                }
            });
        }
    }

    public void ButtonWarnings(View view)
    {
        if(!game.GetInteractionReady())
        {
            ShowBasicOKDialog(getString(R.string.waiting_for_data));
        }
        else
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    SetView(new WarningsView(game, me));
                }
            });
        }
    }

    public void ButtonPlayers(View view)
    {
        if(!game.GetInteractionReady())
        {
            ShowBasicOKDialog(getString(R.string.waiting_for_data));
        }
        else
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    SetView(new PlayersView(game, me));
                }
            });
        }
    }

    public void ButtonSettings(View view)
    {
        runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    SetView(new SettingsView(game, me));
                }
            });
    }

    public void ButtonRespawn(View view)
    {
        game.Respawn();
    }

    public void ButtonPlayStore(View view)
    {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(ClientDefs.PLAY_STORE_URL));
        startActivity(intent);
    }

    public void ButtonEmailServerHelp(View view)
    {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[] { game.GetConfig().GetServerEmail() });
        intent.putExtra(Intent.EXTRA_SUBJECT, "Launch! Help & Support");

        startActivity(Intent.createChooser(intent, "Send Email"));
    }

    public void ButtonDiscord(View view)
    {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(ClientDefs.DISCORD_URL));
        startActivity(intent);
    }

    public void ButtonWiki(View view)
    {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(ClientDefs.WIKI_URL));
        startActivity(intent);
    }

    public void SetMapSatellite(boolean bSatellite)
    {
        bMapIsSatellite = bSatellite;

        map.setMapType(bSatellite ? GoogleMap.MAP_TYPE_SATELLITE : GoogleMap.MAP_TYPE_NORMAL);
    }

    public void SetMapModeZoomOrSelect(boolean bZoom)
    {
        bMapModeZoom = bZoom;
        ((SelectableMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).SetZoom(bZoom);
    }

    public boolean GetMapSatellite()
    {
        return bMapIsSatellite;
    }

    public boolean GetMapModeZoom()
    {
        return bMapModeZoom;
    }

    @Override
    public void SetSelectionRectangle(float xFrom, float yFrom, float xTo, float yTo)
    {
        LatLng pt1 = map.getProjection().fromScreenLocation(new Point((int)xFrom, (int)yFrom));
        LatLng pt2 = map.getProjection().fromScreenLocation(new Point((int)xFrom, (int)yTo));
        LatLng pt3 = map.getProjection().fromScreenLocation(new Point((int)xTo, (int)yTo));
        LatLng pt4 = map.getProjection().fromScreenLocation(new Point((int)xTo, (int)yFrom));

        if(selectionRect == null)
        {
            selectionRect = map.addPolyline(new PolylineOptions()
                    .add(pt1)
                    .add(pt2)
                    .add(pt3)
                    .add(pt4)
                    .add(pt1)
                    .geodesic(true)
                    .color(Utilities.ColourFromAttr(this, R.attr.InfoColour)));
        }
        else
        {
            List<LatLng> points = new ArrayList<>();
            points.add(pt1);
            points.add(pt2);
            points.add(pt3);
            points.add(pt4);
            points.add(pt1);

            selectionRect.setPoints(points);
        }
    }

    @Override
    public void SelectEntities(float xFrom, float yFrom, float xTo, float yTo)
    {
        if(selectionRect != null)
        {
            selectionRect.remove();
            selectionRect = null;
        }

        if(CurrentView instanceof MainNormalView)
            ((MainNormalView) CurrentView).BottomLayoutShowView(new MapSelectView(game, this, map.getProjection().fromScreenLocation(new Point((int)xFrom, (int)yTo)), map.getProjection().fromScreenLocation(new Point((int)xTo, (int)yFrom)))); //Note y reversed because its magnitude is opposite to latitudinal magnitude.


        SetMapModeZoomOrSelect(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
            case ClientDefs.ACTIVITY_REQUEST_CODE_AVATAR_IMAGE:
            {
                if (resultCode == Activity.RESULT_OK)
                {
                    ((UploadAvatarView) CurrentView).ImageActivityResult(data);
                }
            }
            break;

            case ClientDefs.ACTIVITY_REQUEST_CODE_AVATAR_CAMERA:
            {
                if (resultCode == Activity.RESULT_OK)
                {
                    ((UploadAvatarView) CurrentView).CameraActivityResult(data);
                }
            }
            break;
        }
    }

    public Locatifier GetLocatifier()
    {
        return locatifier;
    }

    public void SavePrivacyZones()
    {
        //Save privacy zones file on device.
        File fileDir = getDir(ClientDefs.SETTINGS, Context.MODE_PRIVATE);
        File file = new File(fileDir, ClientDefs.PRIVACY_FILENAME);

        try
        {
            RandomAccessFile rafConfig = new RandomAccessFile(file, "rw");
            rafConfig.setLength(0); //Clear the file.

            for(PrivacyZone privacyZone : game.GetPrivacyZones())
            {
                rafConfig.write(privacyZone.GetBytes());
            }

            rafConfig.close();
        }
        catch (Exception ex)
        {
            LaunchLog.Log(APPLICATION, LOG_NAME, "Could not save config: " + ex.getMessage());
        }
    }

    public void SetPlayersStuffVisibility(int lPlayerID, boolean bVisible)
    {
        if(bVisible)
        {
            CustomVisibilities.put(lPlayerID, true);
        }
        else
        {
            CustomVisibilities.remove(lPlayerID);
        }

        PlayerVisibilities.put(lPlayerID, bVisible);
        RebuildMap();
    }

    public boolean GetPlayersStuffVisibility(int lPlayerID)
    {
        if(!PlayerVisibilities.containsKey(lPlayerID))
            PlayerVisibilities.put(lPlayerID, false);

        return PlayerVisibilities.get(lPlayerID);
    }

    @Override
    public void GameTicked(int lMS)
    {
        final Context context = this;

        //Instruct the current view to update.
        if(CurrentView != null)
        {
            CurrentView.Update();
        }

        //Update the map.
        if(!bRendering)
        {
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    bRendering = true;

                    if (map != null)
                    {
                        //Rebuild map display if required.
                        if (bRebuildMap)
                        {
                            bRebuildMap = false;

                            //Populate map.
                            map.clear();
                            ClusterManagerMissileSites.clearItems();
                            ClusterManagerSAMSites.clearItems();
                            ClusterManagerSentryGuns.clearItems();
                            ClusterManagerOreMines.clearItems();
                            ClusterManagerLoot.clearItems();

                            PrivacyZoneMarkers = new ConcurrentHashMap<>();
                            PlayerMarkers = new ConcurrentHashMap<>();
                            MissileMarkers = new ConcurrentHashMap<>();
                            InterceptorMarkers = new ConcurrentHashMap<>();
                            MissileSiteMarkers = new ConcurrentHashMap<>();
                            SAMSiteMarkers = new ConcurrentHashMap<>();
                            SentryGunMarkers = new ConcurrentHashMap<>();
                            OreMineMarkers = new ConcurrentHashMap<>();
                            LootMarkers = new ConcurrentHashMap<>();
                            RadiationMarkers = new ConcurrentHashMap<>();

                            switch (interactionMode)
                            {
                                case PRIVACY_ZONES:
                                {
                                    for (PrivacyZone privacyZone : game.GetPrivacyZones())
                                    {
                                        MarkerOptions options = new MarkerOptions();
                                        options.position(Utilities.GetLatLng(privacyZone.GetPosition()));
                                        options.anchor(0.5f, 0.5f);
                                        options.icon(BitmapDescriptorFactory.fromResource(R.drawable.privacy));

                                        PrivacyZoneMarkers.put(map.addMarker(options), privacyZone);

                                        Circle privacyZoneCircle = map.addCircle(new CircleOptions()
                                                .center(Utilities.GetLatLng(privacyZone.GetPosition()))
                                                .radius(privacyZone.GetRadius())
                                                .fillColor(Utilities.ColourFromAttr(context, selectedPrivacyZone == privacyZone ? R.attr.PrivacyZoneSelectedColour : R.attr.PrivacyZoneColour))
                                                .strokeWidth(0.0f));

                                        //Pass circle to editor if editing this privacy zone.
                                        if (selectedPrivacyZone == privacyZone)
                                        {
                                            if (CurrentView instanceof PrivacyZonesView)
                                            {
                                                ((PrivacyZonesView) CurrentView).SetCircle(privacyZoneCircle);
                                            }
                                        }
                                    }
                                }
                                break;

                                default:
                                {
                                    if (game.GetInteractionReady())
                                    {
                                            //Dotncarry memorial.
                                            MarkerOptions opt = new MarkerOptions();
                                            opt.position(new LatLng(31.891461, -104.86071));
                                            opt.anchor(0.5f, 0.5f);
                                            opt.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_texas_obelisk));
                                            DotncarryMemorial = map.addMarker(opt);

                                            for (Player player : game.GetPlayers())
                                            {
                                                CreateEntityUI(player);
                                            }

                                            for (Missile missile : game.GetMissiles())
                                            {
                                                CreateEntityUI(missile);
                                            }

                                            for (Interceptor interceptor : game.GetInterceptors())
                                            {
                                                CreateEntityUI(interceptor);
                                            }

                                            for (MissileSite missileSite : game.GetMissileSites())
                                            {
                                                CreateEntityUI(missileSite);
                                            }

                                            for (SAMSite samSite : game.GetSAMSites())
                                            {
                                                CreateEntityUI(samSite);
                                            }

                                            for (SentryGun sentryGun : game.GetSentryGuns())
                                            {
                                                CreateEntityUI(sentryGun);
                                            }

                                            for (OreMine oreMine : game.GetOreMines())
                                            {
                                                CreateEntityUI(oreMine);
                                            }

                                            for (Loot loot : game.GetLoots())
                                            {
                                                CreateEntityUI(loot);
                                            }

                                            for (Radiation radiation : game.GetRadiations())
                                            {
                                                CreateEntityUI(radiation);
                                            }
                                    }
                                }
                            }
                        }

                        //Move entities that can move.
                        for (Map.Entry<Integer, Marker> entry : MissileMarkers.entrySet())
                        {
                            Missile missile = game.GetMissile(entry.getKey());
                            Marker marker = entry.getValue();

                            if (marker != null && missile != null)
                            {
                                marker.setPosition(Utilities.GetLatLng(missile.GetPosition()));
                                marker.setRotation((float) GeoCoord.ToDegrees(missile.GetPosition().GetLastBearing()));

                                //Update trajectory.
                                Polyline trajectory = MissileTrajectories.get(missile.GetID());
                                GeoCoord geoTarget = game.GetMissileTarget(missile);
                                trajectory.setPoints(Arrays.asList(Utilities.GetLatLng(missile.GetPosition()), Utilities.GetLatLng(geoTarget)));

                                if (missile.GetTracking())
                                {
                                    MissileAttackMarkers.get(missile.GetID()).setCenter(Utilities.GetLatLng(geoTarget));

                                    Circle EMP = EMPMarkers.get(missile.GetID());

                                    if (EMP != null)
                                        EMP.setCenter(Utilities.GetLatLng(geoTarget));
                                }
                            }
                        }

                        for (Map.Entry<Integer, Marker> entry : ECMMarkers.entrySet())
                        {
                            Missile missile = game.GetMissile(entry.getKey());
                            Marker marker = entry.getValue();

                            if (marker != null && missile != null)
                            {
                                marker.setPosition(Utilities.GetLatLng(missile.GetPosition()));
                                marker.setRotation((float) GeoCoord.ToDegrees(missile.GetPosition().GetLastBearing()));
                            }
                        }

                        for (Map.Entry<Integer, Marker> entry : InterceptorMarkers.entrySet())
                        {
                            Interceptor interceptor = game.GetInterceptor(entry.getKey());
                            Marker marker = entry.getValue();

                            if (marker != null && interceptor != null)
                            {
                                marker.setPosition(Utilities.GetLatLng(interceptor.GetPosition()));
                                marker.setRotation((float) GeoCoord.ToDegrees(interceptor.GetPosition().GetLastBearing()));

                                Missile target = game.GetMissile(interceptor.GetTargetID());

                                if (target != null)
                                {
                                    //Update trajectory.
                                    Polyline trajectory = InterceptorTrajectories.get(interceptor.GetID());
                                    trajectory.setPoints(Arrays.asList(Utilities.GetLatLng(interceptor.GetPosition()), Utilities.GetLatLng(target.GetPosition())));
                                }
                            }
                        }

                        //Rerender clustery stuff.
                        HighLevelUIRefresh();
                    }

                    bRendering = false;
                }
            });
        }
    }

    @Override
    public void SaveConfig(Config config)
    {
        //As the config has changed, purge everything to be downloaded again (as per config "Variant" functionality, among other stuff).
        PurgeClient();

        //Save config file on device.
        File fileDir = getDir(ClientDefs.SETTINGS, Context.MODE_PRIVATE);
        File file = new File(fileDir, ClientDefs.CONFIG_FILENAME);

        try
        {
            RandomAccessFile rafConfig = new RandomAccessFile(file, "rw");
            rafConfig.setLength(0); //Clear the file.
            rafConfig.write(config.GetData());
            rafConfig.close();
        }
        catch (Exception ex)
        {
            LaunchLog.Log(APPLICATION, LOG_NAME, "Could not save config: " + ex.getMessage());
        }
    }

    @Override
    public void SaveAvatar(int lAvatarID, byte[] cData)
    {
        //Avatar downloaded.
        FileOutputStream out = null;

        try
        {
            File fileDir = getDir(ClientDefs.AVATAR_FOLDER, Context.MODE_PRIVATE);
            File file = new File(fileDir, lAvatarID + ClientDefs.IMAGE_FORMAT);
            out = new FileOutputStream(file);
            out.write(cData);
            AvatarBitmaps.InvalidateAvatar(lAvatarID);

            //Update UI.
            for (final Map.Entry<Integer, Marker> entry : PlayerMarkers.entrySet())
            {
                final Player player = game.GetPlayer(entry.getKey());

                if(player.GetAvatarID() == lAvatarID)
                {
                    if (!player.GetBanned_Client() && player.GetPosition().GetValid())
                    {
                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                Marker marker = entry.getValue();
                                marker.setIcon(GetPlayerIcon(player));
                            }
                        });
                    }
                }
            }

            //Update current view.
            CurrentView.AvatarSaved(lAvatarID);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                out.close();
            }
            catch(Exception ex) { /* Don't care */ }
        }
    }

    @Override
    public void SaveImage(int lImageID, byte[] cData)
    {
        //Image downloaded.
        FileOutputStream out = null;
        boolean bSucceeded = true;

        try
        {
            File fileDir = getDir(ClientDefs.IMGASSETS_FOLDER, Context.MODE_PRIVATE);
            File file = new File(fileDir, lImageID + ClientDefs.IMAGE_FORMAT);
            out = new FileOutputStream(file);
            out.write(cData);
        }
        catch (FileNotFoundException e)
        {
            bSucceeded = false;
            e.printStackTrace();
        }
        catch (IOException e)
        {
            bSucceeded = false;
            e.printStackTrace();
        }
        finally
        {
            try
            {
                out.close();
            }
            catch(Exception ex) { /* Don't care */ }
        }

        if(bSucceeded)
        {
            //Update alliance/report screens, etc.
            /*if(CurrentView instanceof DiplomacyActionView)
            {
                //TO DO.
            }

            if(CurrentView instanceof DiplomacyView)
            {
                ((DiplomacyView)CurrentView).RefreshAvatar(lAvatarID);
            }

            if(CurrentView instanceof ReportsView)
            {
                //TO DO.
            }*/

            //Update UI.
            final MainActivity self = this;

            for (final Map.Entry<Integer, Marker> entry : MissileMarkers.entrySet())
            {
                final Missile missile = game.GetMissile(entry.getKey());

                if(missile != null)
                {
                    final MissileType type = game.GetConfig().GetMissileType(missile.GetType());

                    if (type.GetAssetID() == lImageID)
                    {
                        final Marker marker = entry.getValue();

                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                marker.setIcon(BitmapDescriptorFactory.fromBitmap(EntityIconBitmaps.GetMissileBitmap(self, game, missile, type.GetAssetID())));
                            }
                        });
                    }
                }
            }

            for (final Map.Entry<Integer, Marker> entry : InterceptorMarkers.entrySet())
            {
                final Interceptor interceptor = game.GetInterceptor(entry.getKey());

                if(interceptor != null)
                {
                    final InterceptorType type = game.GetConfig().GetInterceptorType(interceptor.GetType());

                    if (type.GetAssetID() == lImageID)
                    {
                        final Marker marker = entry.getValue();

                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                marker.setIcon(BitmapDescriptorFactory.fromBitmap(EntityIconBitmaps.GetInterceptorBitmap(self, game, interceptor, type.GetAssetID())));
                            }
                        });
                    }
                }
            }
        }
    }

    @Override
    public void AvatarUploaded(final int lAvatarID)
    {
        //Update UI.
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                switch(interactionMode)
                {
                    case REGISTRATION:
                    {
                        //Player's avatar uploaded. Go back to registration screen.
                        SetView(new RegisterView(game, me, lAvatarID));
                    }
                    break;

                    case DIPLOMACY:
                    {
                        //Player uploaded an avatar for their alliance.
                        if(game.GetOurPlayer().GetAllianceMemberID() == Alliance.ALLIANCE_ID_UNAFFILIATED)
                        {
                            SetView(new CreateAllianceView(game, me, lAvatarID));
                        }
                        else
                        {
                            game.SetAvatar(lAvatarID, true);
                            SetView(new AllianceControl(game, me, game.GetAlliance(game.GetOurPlayer().GetAllianceMemberID())));
                        }
                    }
                    break;

                    default:
                    {
                        //Player uploaded their own avatar from settings.
                        game.SetAvatar(lAvatarID, false);
                        ReturnToMainView();
                    }
                    break;
                }
            }
        });
    }

    @Override
    public boolean PlayerLocationAvailable()
    {
        return locatifier.GetCurrentLocationGood();
    }

    @Override
    public LaunchClientLocation GetPlayerLocation()
    {
        return locatifier.GetLocation();
    }

    @Override
    public void EntityUpdated(LaunchEntity entity)
    {
        if(entity instanceof Player)
        {
            final Player player = (Player)entity;

            switch (interactionMode)
            {
                case TARGET_MISSILE:
                case TARGET_INTERCEPTOR:
                {
                    //Update range circle if applicable.
                    if (player == game.GetOurPlayer() && bPlayerTargetting)
                    {
                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                if (targetRange != null)
                                {
                                    targetRange.setCenter(Utilities.GetLatLng(player.GetPosition()));
                                }
                            }
                        });
                    }
                }
                break;
            }

            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    //Update player marker.
                    for (final Map.Entry<Integer, Marker> entry : PlayerMarkers.entrySet())
                    {
                        if (entry.getKey() == player.GetID())
                        {
                            if (!player.GetBanned_Client() && player.GetPosition().GetValid())
                            {
                                Marker marker = entry.getValue();

                                marker.setPosition(Utilities.GetLatLng(player.GetPosition()));
                                marker.setIcon(GetPlayerIcon(player));
                                marker.setAlpha(player.GetRespawnProtected() ? 0.5f : 1.0f);
                            } else
                            {
                                //If a player has gone awol, just rebuild the map without them to save having to try and modify the collection during enumeration.
                                RebuildMap();
                            }

                            return;
                        }
                    }

                    //If we are this far, the player wasn't found and we need to create a new marker.
                    CreateEntityUI(player);
                }
            });
        }
        else
        {
            //Redraw everything else if their states change.
            CreateEntityUI(entity);
        }

        //Update the current view with the entity.
        if(CurrentView != null)
        {
            CurrentView.EntityUpdated(entity);
        }
    }

    @Override
    public void TreatyUpdated(Treaty treaty)
    {
        //Update the current view with the treaty.
        if(CurrentView != null)
        {
            CurrentView.TreatyUpdated(treaty);
        }
    }

    private BitmapDescriptor GetPlayerIcon(Player player)
    {
        if(player.Destroyed())
        {
            return BitmapDescriptorFactory.fromBitmap(EntityIconBitmaps.GetDeadPlayerBitmap(this, game, player));
        }

        //Scale it to be smaller for the map.
        Bitmap bitmap = AvatarBitmaps.GetPlayerAvatar(this, game, player);
        return BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmap, PLAYER_MAP_ICON_SIZE, PLAYER_MAP_ICON_SIZE, true));
    }

    @Override
    public void EntityRemoved(final LaunchEntity entity)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                //If the selected entity is removed, deselect it and back out of any associated modes.
                if(selectedEntity != null)
                {
                    if (selectedEntity.equals(entity))
                    {
                        ClearSelectedEntity();
                        InformationMode();
                    }
                }

                if(entity instanceof Player)
                {
                    Marker marker = PlayerMarkers.get(entity.GetID());

                    if (marker != null)
                    {
                        PlayerMarkers.remove(entity.GetID());
                        marker.remove();
                    }
                }
                else if(entity instanceof Missile)
                {
                    Marker marker = MissileMarkers.get(entity.GetID());
                    Marker ecm = ECMMarkers.get(entity.GetID());
                    Polyline line = MissileTrajectories.get(entity.GetID());
                    Circle circleBlast = MissileAttackMarkers.get(entity.GetID());
                    Circle circleEMP = EMPMarkers.get(entity.GetID());

                    if(marker != null)
                    {
                        MissileMarkers.remove(entity.GetID());
                        marker.remove();
                    }

                    if(ecm != null)
                    {
                        ECMMarkers.remove(entity.GetID());
                        ecm.remove();
                    }

                    if(line != null)
                    {
                        MissileTrajectories.remove(entity.GetID());
                        line.remove();
                    }

                    if(circleBlast != null)
                    {
                        MissileAttackMarkers.remove(entity.GetID());
                        circleBlast.remove();
                    }

                    if(circleEMP != null)
                    {
                        EMPMarkers.remove(entity.GetID());
                        circleEMP.remove();
                    }
                }
                else if(entity instanceof Interceptor)
                {
                    Marker marker = InterceptorMarkers.get(entity.GetID());
                    Polyline line = InterceptorTrajectories.get(entity.GetID());

                    if(marker != null)
                    {
                        InterceptorMarkers.remove(entity.GetID());
                        marker.remove();
                    }

                    if(line != null)
                    {
                        InterceptorTrajectories.remove(entity.GetID());
                        line.remove();
                    }
                }
                else if(entity instanceof MissileSite)
                {
                    LaunchClusterItem clusterItem = MissileSiteMarkers.get(entity.GetID());
                    if(clusterItem != null)
                    {
                        MissileSiteMarkers.remove(entity.GetID());
                        ClusterManagerMissileSites.removeItem(clusterItem);
                    }
                }
                else if(entity instanceof SAMSite)
                {
                    LaunchClusterItem clusterItem = SAMSiteMarkers.get(entity.GetID());
                    if(clusterItem != null)
                    {
                        SAMSiteMarkers.remove(entity.GetID());
                        ClusterManagerSAMSites.removeItem(clusterItem);
                    }
                }
                else if(entity instanceof SentryGun)
                {
                    LaunchClusterItem clusterItem = SentryGunMarkers.get(entity.GetID());
                    if(clusterItem != null)
                    {
                        SentryGunMarkers.remove(entity.GetID());
                        ClusterManagerSentryGuns.removeItem(clusterItem);
                    }
                }
                else if(entity instanceof OreMine)
                {
                    LaunchClusterItem clusterItem = OreMineMarkers.get(entity.GetID());
                    if(clusterItem != null)
                    {
                        OreMineMarkers.remove(entity.GetID());
                        ClusterManagerOreMines.removeItem(clusterItem);
                    }
                }
                else if(entity instanceof Loot)
                {
                    LaunchClusterItem clusterItem = LootMarkers.get(entity.GetID());
                    if(clusterItem != null)
                    {
                        LootMarkers.remove(entity.GetID());
                        ClusterManagerLoot.removeItem(clusterItem);
                    }
                }
                else if(entity instanceof Radiation)
                {
                    final Circle circle = RadiationMarkers.get(entity.GetID());

                    if(circle != null)
                    {
                        RadiationMarkers.remove(entity.GetID());
                        circle.remove();
                    }
                }

                HighLevelUIRefresh();
            }
        });

        //Update the current view with the entity.
        if(CurrentView != null)
        {
            CurrentView.EntityRemoved(entity);
        }
    }

    @Override
    public void MajorChanges()
    {
        //Rebuild the map.
        RebuildMap();
    }

    @Override
    public void Authenticated()
    {
        if(interactionMode == InteractionMode.REGISTRATION || interactionMode == InteractionMode.SPLASH)
        {
            interactionMode = InteractionMode.STANDARD;
            ReturnToMainView();

            //tutorial.NotifyEntry(this);

            //Display server message if applicable.
            //TO DO: Get from server, and reinstate!
            /*String strServerMessage = "Missile & interceptor purchasing has been improved; you can now purchase multiple in one go.";
            byte cServerMessageData[] = strServerMessage.getBytes();

            CRC32 crc32 = new CRC32();
            crc32.update(cServerMessageData);

            SharedPreferences sharedPreferences = getSharedPreferences(ClientDefs.SETTINGS, MODE_PRIVATE);

            if(sharedPreferences.getLong(ClientDefs.SETTINGS_SERVER_MESSAGE_CHECKSUM, 0) != crc32.getValue())
            {
                //We haven't seen this message before. Display it.
                ShowBasicOKDialog(strServerMessage);
                SharedPreferences.Editor editor = getSharedPreferences(ClientDefs.SETTINGS, Context.MODE_PRIVATE).edit();
                editor.putLong(ClientDefs.SETTINGS_SERVER_MESSAGE_CHECKSUM, crc32.getValue());
                editor.commit();
            }*/
        }

        //Update the notification service.
        LaunchAlertManager.SystemCheck(this);
    }

    @Override
    public void AccountUnregistered()
    {
        //Check we're not already in registration mode (because the comms get dropped when uploading an avatar on initial registration).
        //Also check we're not in disclaimer mode, because we don't want to change from that screen.
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if(interactionMode != InteractionMode.REGISTRATION)
                {
                    interactionMode = InteractionMode.REGISTRATION;
                    SetView(new RegisterView(game, me, ClientDefs.DEFAULT_AVATAR_ID));
                }
            }
        });
    }

    @Override
    public void AccountNameTaken()
    {
        ((RegisterView)CurrentView).UsernameTaken();
    }

    @Override
    public void MajorVersionMismatch()
    {
        interactionMode = InteractionMode.CANT_LOG_IN;

        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                setContentView(R.layout.main_majorupdate);
            }
        });
    }

    @Override
    public void MinorVersionMismatch()
    {
        //Hack: Push an event into the client game to make it appear on the text event list.
        game.EventReceived(new LaunchEvent(getString(R.string.version_mismatch_minor)));
    }

    @Override
    public void ActionSucceeded()
    {
    }

    @Override
    public void ActionFailed()
    {

    }

    @Override
    public void ShowTaskMessage(final TaskMessage message)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                String strText = "";

                switch(message)
                {
                    case REGISTERING: strText = getString(R.string.registering); break;
                    case UPLOADING_AVATAR: strText = getString(R.string.uploading_avatar); break;
                    case RESPAWNING: strText = getString(R.string.respawning); break;
                    case CONSTRUCTING: strText = getString(R.string.constructing); break;
                    case PURCHASING: strText = getString(R.string.purchasing); break;
                    case DECOMISSIONING: strText = getString(R.string.decommissioning); break;
                    case SELLING: strText = getString(R.string.selling); break;
                    case LAUNCHING_MISSILE: strText = getString(R.string.launching_missile); break;
                    case LAUNCHING_INTERCEPTOR: strText = getString(R.string.launching_interceptor); break;
                    case CLOSING_ACCOUNT: strText = getString(R.string.closing_account); break;
                    case REPAIRING: strText = getString(R.string.repairing); break;
                    case HEALING: strText = getString(R.string.healing); break;
                    case CONFIGURING: strText = getString(R.string.configuring); break;
                    case UPGRADING: strText = getString(R.string.upgrading); break;
                    case ALLIANCE_CREATE: strText = getString(R.string.creating_alliance); break;
                    case ALLIANCE_JOIN: strText = getString(R.string.joining_alliance); break;
                    case ALLIANCE_LEAVE: strText = getString(R.string.leaving_alliance); break;
                    case DECLARE_WAR: strText = getString(R.string.declaring_war); break;
                    case PROMOTING: strText = getString(R.string.promoting); break;
                    case ACCEPTING: strText = getString(R.string.accepting); break;
                    case REJECTING: strText = getString(R.string.rejecting); break;
                    case KICKING: strText = getString(R.string.kicking); break;
                    case DIPLOMACY: strText = getString(R.string.diplomacy); break;

                    default: strText = "<THE GAME IS BROKEN. PLEASE SCREENSHOT AND REPORT THIS.>"; break;
                }

                //Modify old one if possible.
                if (commsDialog != null)
                {
                    commsDialog.SetMessage(strText);
                }
                else
                {
                    //Create new one.
                    commsDialog = new LaunchDialog();
                    commsDialog.setCancelable(false);

                    commsDialog.SetHeaderComms();
                    commsDialog.SetMessage(strText);
                    commsDialog.SetEnableProgressSpinner();

                    commsDialog.show(getFragmentManager(), "");

                    //TO DO: The comms dialog should have comms status on it.
                }
            }
        });
    }

    @Override
    public void DismissTaskMessage()
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if (commsDialog != null)
                {
                    commsDialog.dismiss();
                    commsDialog = null;
                }
            }
        });
    }

    @Override
    public void NewEvent(LaunchEvent event)
    {
        switch(event.GetSoundEffect())
        {
            case EXPLOSION:
            {
                if(event.GetMessage().contains(game.GetOurPlayer().GetName()))
                    Sounds.PlayNearExplosion();
                else
                    Sounds.PlayFarExplosion();
            }
            break;

            case MONEY: Sounds.PlayMoney(); break;
            case MISSILE_LAUNCH: Sounds.PlayMissileLaunch(); break;
            case INTERCEPTOR_LAUNCH: Sounds.PlayInterceptorLaunch(); break;
            case CONSTRUCTION: Sounds.PlayConstruction(); break;
            case INTERCEPTOR_MISS: Sounds.PlayInterceptorMiss(); break;
            case INTERCEPTOR_HIT: Sounds.PlayInterceptorHit(); break;
            case SENTRY_GUN_HIT: Sounds.PlaySentryGunHit(); break;
            case SENTRY_GUN_MISS: Sounds.PlaySentryGunMiss(); break;
            case RECONFIG: Sounds.PlayReconfig(); break;
            case EQUIP: Sounds.PlayEquip(); break;
            case RESPAWN: Sounds.PlayRespawn(); break;
            case DEATH: Sounds.PlayDeath(); break;
            case REPAIR: Sounds.PlayRepair(); break;
            case HEAL: Sounds.PlayHeal(); break;
        }
    }

    @Override
    public void NewReport(LaunchReport report)
    {
        //Flash the reports button accordingly.
        if(report.GetMajor())
        {
            reportStatus = ReportsStatus.MAJOR;
        }
        else
        {
            reportStatus = ReportsStatus.MINOR;
        }
    }

    @Override
    public void Quit()
    {
        finish();
    }

    @Override
    public void AllianceCreated()
    {
        //Return to diplomacy screen after creating a new alliance.
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                SetView(new DiplomacyView(game, me));
            }
        });
    }

    @Override
    public String GetProcessNames()
    {
        String strApps = "";

        PackageManager packageManager = getPackageManager();
        List<ApplicationInfo> packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo applicationInfo : packages)
        {
            strApps += applicationInfo.processName + "\n";
        }

        return strApps.substring(0, strApps.length() - 1);
    }

    @Override
    public boolean GetConnectionMobile()
    {
        //TO DO: FIX THIS. It currently breaks on some player's phones.
        /*ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        if(connectivityManager != null)
        {
            NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());

            if(networkCapabilities != null)
            {
                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
                    return false;

                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
                    return true;
            }
        }*/

        return false;
    }

    @Override
    public void DeviceChecksRequested()
    {
        try
        {
            SafetyNet.getClient(this).attest(Security.CreateRandomHash(), "AIzaSyCzBW2Vr5AHj0RlZsmoUlWq56L_FQmC_V4")
                    .addOnSuccessListener(this,
                            new OnSuccessListener<SafetyNetApi.AttestationResponse>()
                            {
                                @Override
                                public void onSuccess(SafetyNetApi.AttestationResponse response)
                                {
                                    // Indicates communication with the service was successful.
                                    try
                                    {
                                        String strPayload = response.getJwsResult().split("\\.")[1];
                                        String strPayloadDecoded = new String(Base64.decode(strPayload, Base64.NO_WRAP));
                                        JSONObject json = new JSONObject(strPayloadDecoded);
                                        boolean bProfileMatch = json.getBoolean("ctsProfileMatch");
                                        boolean bBasicIntegrity = json.getBoolean("basicIntegrity");

                                        game.DeviceCheck(false, false, 0, bProfileMatch, bBasicIntegrity);
                                    }
                                    catch (JSONException e)
                                    {
                                        //A JSON error occurred.
                                        game.DeviceCheck(true, false, 0, false, false);
                                    }
                                    catch(Exception e)
                                    {
                                        //Anything else went wrong.
                                        game.DeviceCheck(true, false, 0, false, false);
                                    }
                                }
                            })
                    .addOnFailureListener(this, new OnFailureListener()
                    {
                        @Override
                        public void onFailure(@NonNull Exception e)
                        {
                            // An error occurred while communicating with the service.
                            if (e instanceof ApiException)
                            {
                                //An error with the Google Play services API contains some additional details.
                                ApiException apiException = (ApiException) e;
                                game.DeviceCheck(false, true, ((ApiException) e).getStatusCode(), false, false);
                            }
                            else
                            {
                                //Some other error occurred.
                                game.DeviceCheck(true, false, 0, false, false);
                            }
                        }
                    });
        }
        catch(NoSuchAlgorithmException ex)
        {
            //Some ridiculous error occurred.
            game.DeviceCheck(true, false, 0, false, false);
        }
    }

    @Override
    public void DisplayGeneralError()
    {
        ShowBasicOKDialog(getString(R.string.general_error));
    }

    @Override
    public void TempBanned(final String strReason, final long oDuration)
    {
        interactionMode = InteractionMode.CANT_LOG_IN;
        game.Suspend();

        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                SetView(new BannedView(game, me, strReason, oDuration));
            }
        });
    }

    @Override
    public void PermBanned(final String strReason)
    {
        interactionMode = InteractionMode.CANT_LOG_IN;
        game.Suspend();

        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                SetView(new BannedView(game, me, strReason));
            }
        });
    }

    @Override
    public void ReceiveUser(User user)
    {
        if(CurrentView instanceof MainNormalView)
        {
            LaunchView bottomView = ((MainNormalView)CurrentView).GetBottomView();

            if(bottomView instanceof PlayerView)
            {
                ((PlayerView)bottomView).UserReceived(user);
            }
        }
    }
}
