package com.apps.fast.launch.components;

import android.content.Context;
import android.content.SharedPreferences;

import com.apps.fast.launch.activities.MainActivity;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import launch.game.Defs;

/**
 * Created by tobster on 20/10/15.
 */
public class ClientDefs
{
    public static final String SETTINGS = "LaunchSettings";     //The sharedpreferences file name for settings and settings folder name.

    //Debugging.
    public static final int NOTIFICATION_DEBUG_LOG_SIZE = 500;
    public static final String DEBUG_INDEX = "dbg_index";
    public static final String DEBUG_PREFIX = "dbg_";

    //Major settings.
    public static final String SETTINGS_SERVER_URL = "ServerAddress";
    public static final String SETTINGS_SERVER_PORT = "ServerPort";
    public static final String SETTINGS_NOTIFICATION_MINUTES = "NotificationMinutes";
    public static final String SETTINGS_NOTIFICATION_NUKEESC = "NotificationNukeEscalation";
    public static final String SETTINGS_NOTIFICATION_ALLIES = "NotificationAllies";
    public static final String SETTINGS_NOTIFICATION_DEBUG = "NotifDebug";
    public static final String SETTINGS_SHORT_UNITS = "ShortUnits";
    public static final String SETTINGS_LONG_UNITS = "LongUnits";
    public static final String SETTINGS_SPEEDS = "Speeds";
    public static final String SETTINGS_CURRENCY = "CurrencyUnit";
    public static final String SETTINGS_NOTIFICATION_SOUND = "NotificationSound";
    public static final String SETTINGS_NOTIFICATION_VIBRATE = "NotificationVibrate";
    public static final String SETTINGS_DISCLAIMER_ACCEPTED = "DisclaimerAccepted00001"; /* Increment trailing digits as required to force a new acceptance. */
    public static final String SETTINGS_SERVER_MESSAGE_CHECKSUM ="ServerMessageChecksum";
    public static final String SETTINGS_IDENTITY_STORED = "StoredIdentity0"; //To prevent IMEI spoofing.
    public static final String SETTINGS_IDENTITY_GENERATED = "IdentityGenerated"; //Device ID has been generated.
    public static final String SETTINGS_DISABLE_AUDIO = "DisableAudio";
    public static final String SETTINGS_CLUSTERING = "Clustering";
    public static final String SETTINGS_THEME = "Theme";
    public static final String SETTINGS_MAP_SATELLITE = "Satellite";
    public static final String SETTINGS_ZOOM_LEVEL = "ZoomLevel";

    //Purchase preferences.
    private static final String SETTINGS_PREFERRED_MISSILE_1 = "PreferredMissile1";
    private static final String SETTINGS_PREFERRED_MISSILE_2 = "PreferredMissile2";
    private static final String SETTINGS_PREFERRED_MISSILE_3 = "PreferredMissile3";
    private static final String SETTINGS_PREFERRED_INTERCEPTOR_1 = "PreferredInterceptor1";
    private static final String SETTINGS_PREFERRED_INTERCEPTOR_2 = "PreferredInterceptor2";
    private static final String SETTINGS_PREFERRED_INTERCEPTOR_3 = "PreferredInterceptor3";

    //Visibility overrides.
    public static final String SETTINGS_VISIBILITY_OVERRIDES = "VisibilityOverrides";

    //Defaults.

    /* Live */
    public static final String SETTINGS_SERVER_URL_DEFAULT = "77.68.17.204";
    public static int GetDefaultServerPort() { return 30069; }

    /* Debug */
    /*public static final String SETTINGS_SERVER_URL_DEFAULT = "192.168.0.54";
    public static int GetDefaultServerPort() { return 30069; }*/

    public static final int SETTINGS_NOTIFICATION_MINUTES_DEFAULT = 15;
    public static final boolean SETTINGS_NOTIFICATION_NUKEESC_DEFAULT = true;
    public static final boolean SETTINGS_NOTIFICATION_ALLIES_DEFAULT = true;
    public static final boolean SETTINGS_NOTIFICATION_DEBUG_DEFAULT = false;
    public static final int SETTINGS_UNITS_DEFAULT = 0;
    public static final String SETTINGS_CURRENCY_DEFAULT = "£";
    public static final boolean SETTINGS_DISCLAIMER_ACCEPTED_DEFAULT = false;
    public static final boolean SETTINGS_DISABLE_AUDIO_DEFAULT = false;
    public static final int SETTINGS_CLUSTERING_DEFAULT = 8;
    public static final int SETTINGS_THEME_DEFAULT = 0;
    public static final boolean SETTINGS_MAP_SATELLITE_DEFAULT = false;
    public static final float SETTINGS_ZOOM_LEVEL_DEFAULT = 15.0f;

    //Filenames etc.
    public static final String CONFIG_FILENAME = "config";
    public static final String PRIVACY_FILENAME = "privacy";

    public static final String AVATAR_FOLDER = "avatars";
    public static final String IMGASSETS_FOLDER = "imgassets";
    public static final String IMAGE_FORMAT = ".png";

    //Themes.
    public static final int THEME_LAUNCH = 0;
    public static final int THEME_BORING = 1;

    public static final String[] Themes = new String[]
    {
        "Launch",
        "Boring"
    };

    //External applications and links
    public static final String DISCORD_URL = "https://discord.gg/9cPZjwm";
    public static final String WIKI_URL = "http://77.68.17.204/mediawiki/index.php/Main_Page";
    public static final String PLAY_STORE_URL = "market://details?id=com.apps.fast.launch";

    //The rest.
    public static final int NEAREST_ENTITY_COUNT = 3;

    public static final float PRIVACY_ZONE_DEFAULT_RADIUS = 100.0f;
    public static final int PRIVACY_ZONE_MAX_RADIUS = 1000;

    public static final float TRACK_THRESHOLD = 0.01f;  //Auto track players with player-tracking missiles if selection distance is less than this.

    public static final long EVENT_MAIN_SCREEN_PERSISTENCE = 30000; //Show latest events for up to 30 seconds.

    public static final int ACTIVITY_REQUEST_CODE_AVATAR_IMAGE = 0;
    public static final int ACTIVITY_REQUEST_CODE_AVATAR_CAMERA = 1;

    public static final int DEFAULT_AVATAR_ID = Defs.THE_GREAT_BIG_NOTHING;

    private static final int PREFERENCE_LIST_SIZE = 3;
    private static LinkedList<Byte> MissilePreferences = null;
    private static LinkedList<Byte> InterceptorPreferences = null;

    public static int CLUSTERING_SIZE = 0; //Read/write.

    /**
     * The maximum number of types to be considered for SAM/Missile site map range ring thicknesses.
     */
    public static final int MAX_RANGE_RING_THICKNESS = 10;

    public static void StoreMoreVolatileSettings(MainActivity activity)
    {
        SharedPreferences.Editor editor = activity.getSharedPreferences(ClientDefs.SETTINGS, Context.MODE_PRIVATE).edit();

        //Store missile list order preferences, etc.
        if(MissilePreferences != null)
        {
            if(MissilePreferences.size() >= 1)
                editor.putInt(SETTINGS_PREFERRED_MISSILE_1, MissilePreferences.get(0));
            if(MissilePreferences.size() >= 2)
                editor.putInt(SETTINGS_PREFERRED_MISSILE_2, MissilePreferences.get(1));
            if(MissilePreferences.size() >= 3)
                editor.putInt(SETTINGS_PREFERRED_MISSILE_3, MissilePreferences.get(2));
        }

        if(InterceptorPreferences != null)
        {
            if(InterceptorPreferences.size() >= 1)
                editor.putInt(SETTINGS_PREFERRED_INTERCEPTOR_1, InterceptorPreferences.get(0));
            if(InterceptorPreferences.size() >= 2)
                editor.putInt(SETTINGS_PREFERRED_INTERCEPTOR_2, InterceptorPreferences.get(1));
            if(InterceptorPreferences.size() >= 3)
                editor.putInt(SETTINGS_PREFERRED_INTERCEPTOR_3, InterceptorPreferences.get(2));
        }

        editor.putBoolean(SETTINGS_MAP_SATELLITE, activity.GetMapSatellite());

        editor.commit();
    }

    public static List<Byte> GetMissilePreferredOrder(Context context)
    {
        //Get the preferred missile order by purchase history.
        if(MissilePreferences != null)
            return MissilePreferences;

        SharedPreferences sharedPreferences = context.getSharedPreferences(SETTINGS, context.MODE_PRIVATE);

        if(sharedPreferences.contains(SETTINGS_PREFERRED_MISSILE_1))
        {
            MissilePreferences = new LinkedList<>();

            MissilePreferences.addLast((byte)sharedPreferences.getInt(SETTINGS_PREFERRED_MISSILE_1, 0));

            if(sharedPreferences.contains(SETTINGS_PREFERRED_MISSILE_2))
                MissilePreferences.addLast((byte)sharedPreferences.getInt(SETTINGS_PREFERRED_MISSILE_2, 0));

            if(sharedPreferences.contains(SETTINGS_PREFERRED_MISSILE_3))
                MissilePreferences.addLast((byte)sharedPreferences.getInt(SETTINGS_PREFERRED_MISSILE_3, 0));

            return MissilePreferences;
        }

        return new LinkedList<>();
    }

    public static void SetMissilePreferred(byte cMissile)
    {
        //Set a purchased missile to go to the top of the preferred list.
        if(MissilePreferences == null)
            MissilePreferences = new LinkedList<>();

        if(MissilePreferences.contains(cMissile))
            MissilePreferences.remove((Byte)cMissile);

        MissilePreferences.push(cMissile);

        if(MissilePreferences.size() > PREFERENCE_LIST_SIZE)
            MissilePreferences.removeLast();
    }

    public static List<Byte> GetInterceptorPreferredOrder(Context context)
    {
        //Get the preferred interceptor order by purchase history.
        if(InterceptorPreferences != null)
            return InterceptorPreferences;

        SharedPreferences sharedPreferences = context.getSharedPreferences(SETTINGS, context.MODE_PRIVATE);

        if(sharedPreferences.contains(SETTINGS_PREFERRED_INTERCEPTOR_1))
        {
            InterceptorPreferences = new LinkedList<>();

            InterceptorPreferences.addLast((byte)sharedPreferences.getInt(SETTINGS_PREFERRED_INTERCEPTOR_1, 0));

            if(sharedPreferences.contains(SETTINGS_PREFERRED_INTERCEPTOR_2))
                InterceptorPreferences.addLast((byte)sharedPreferences.getInt(SETTINGS_PREFERRED_INTERCEPTOR_2, 0));

            if(sharedPreferences.contains(SETTINGS_PREFERRED_INTERCEPTOR_3))
                InterceptorPreferences.addLast((byte)sharedPreferences.getInt(SETTINGS_PREFERRED_INTERCEPTOR_3, 0));

            return InterceptorPreferences;
        }

        return new LinkedList<>();
    }

    public static void SetInterceptorPreferred(byte cInterceptor)
    {
        //Set a purchased interceptor to go to the top of the preferred list.
        if(InterceptorPreferences == null)
            InterceptorPreferences = new LinkedList<>();

        if(InterceptorPreferences.contains(cInterceptor))
            InterceptorPreferences.remove((Byte)cInterceptor);

        InterceptorPreferences.push(cInterceptor);

        if(InterceptorPreferences.size() > PREFERENCE_LIST_SIZE)
            InterceptorPreferences.removeLast();
    }
}
