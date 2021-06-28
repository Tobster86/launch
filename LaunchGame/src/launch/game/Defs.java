/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.game;

/**
 *
 * @author tobster
 */
public class Defs
{
    public static boolean THE_FLAG_OF_DOOM = false;   //TOP SECRET.
    
    public static int THE_GREAT_BIG_NOTHING = -1;   //Initialiser for properties indicating uninitialised/unassigned/doesn't have one/not in one/etc.
    
    public static final int WEALTH_CAP = 40000; //TO DO: Move back to config in a major release.
    
    public static final String LOCATION_AVATARS = "avatars";
    public static final String LOCATION_IMGASSETS = "imgassets";
    public static final String IMAGE_FILE_FORMAT = "%s/%d.png";
    
    public static final short MAJOR_VERSION = 34;   //Clients must be compliant with the major version, or they won't be allowed to log in.
    public static final short MINOR_VERSION = 0;    //Clients with a lower minor version can log in, but will be advised to update.
    
    public static final int AVATAR_SIZE = 128;      //Total avatar size, including allegiance ring.
    public static final int AVATAR_IMAGE = 120;     //Avatar size within the allegiance ring.
    
    public static final double EARTH_RADIUS_KM = 6372.8;
    public static final float MILE_TO_KM = 0.621371192f;

    public static final float METRES_PER_KM = 1000.0f;
    
    public static final float MULTIACCOUNT_CONSIDERATION_DISTANCE = 0.5f; //Proximity of players to consider during multiaccounting checks, km.
    
    public static final int MS_PER_SEC = 1000;
    public static final int MS_PER_MIN = MS_PER_SEC * 60;
    public static final int MS_PER_HOUR = MS_PER_MIN * 60;
    public static final int MS_PER_DAY = MS_PER_HOUR * 24;
    public static final double MS_PER_HOUR_DBL = (double)MS_PER_HOUR;
    public static final float MS_PER_HOUR_FLT = (float)MS_PER_HOUR;
    public static final long MS_PER_QUARTER = 7776000000L;

    public static final int LATENCY_DISCONNECTED = -1;
    
    public static final int MAX_EVENTS = 3;
    public static final int MAX_REPORTS = 100;

    public static final long PLAYER_ONLINE_TIME = 20000; //Time since player last updated to consider them 'online'.
    
    public static final float NOOB_WARNING = 0.2f;
    public static final float ELITE_WARNING = 2.0f;
    
    public static final float LOCATION_SPOOF_SUSPECT_SPEED = 1000.0f;      //Player speed to record a possible location spoof, KPH.
    public static final float LOCATION_SPOOF_SUSPECT_DISTANCE = 1.0f;      //Player movement distance to record a possible location spoof, KPH.
    
    public static final int MAX_MISSILE_SLOTS = 127;                       //TO DO: Change this functionality. This is just a preliminary measure to prevent an overflow crash.
    
    public static final int MAX_PLAYER_NAME_LENGTH = 32;
    public static final int MAX_ALLIANCE_NAME_LENGTH = 32;
    public static final int MAX_ALLIANCE_DESCRIPTION_LENGTH = 140;
    public static final int MAX_STRUCTURE_NAME_LENGTH = 12;
    
    public static final long IP_POISON_TIME = 172800000;
    
    public static final long NINETY_DAYS = 7776000000L;
    
    public static final float WAR_REWARD_FACTOR = 0.33333333f;
    
    public static final float RELATIONSHIP_BONUS_THRESHOLD = 0.25f;         //The threshold that must be passed to be eligible for player relationship based bonuses as a ratio of all players.
    
    /**
     * Time since a player's last update which if less than this, will cause a linear collision check with world entities as they're "on the move".
     */
    public static final long ON_THE_MOVE_TIME_THRESHOLD = 60000;
    
    /**
     * Number of kilometres to step when walking a player's "on the move" collision detection path.
     */
    public static final float ON_THE_MOVE_STEP_DISTANCE = 0.005f;
    
    /**
     * Chance of performing a running process check.
     */
    public static final float PROCESS_CHECK_RANDOM_CHANCE = 0.01f;
    
    public static final int AUTH_FLAG_RES1 = 0x01;
    public static final int AUTH_FLAG_MOBILE = 0x02;
    public static final int AUTH_FLAG_RES2 = 0x04;
    public static final int AUTH_FLAG_RES3 = 0x08;
    public static final int AUTH_FLAG_RES4 = 0x10;
    public static final int AUTH_FLAG_RES5 = 0x20;
    public static final int AUTH_FLAG_RES6 = 0x40;
    public static final int AUTH_FLAG_RES7 = 0x80;
    
    /** Identity for special player-carried stuff such as the site ID for player-carried missiles and interceptors. */
    public static final int PLAYER_CARRIED = -1;
    
    /** Cost value that generically indicates something is no longer upgradeable. */
    public static final int UPGRADE_COST_MAXED = Integer.MAX_VALUE;
    
    /** For emulator detection. */
    public static final int SUSPICIOUSLY_LOW_PROCESS_NUMBER = 80;
    public static final int EXPECTED_DATA_DIR_LENGTH = 35;
    public static final String DATA_DIR_SHOULD_MATCH = "com.apps.fast.launch";
        
    public static final boolean IsMobile(byte cAuthFlags)
    {
        return ( cAuthFlags & AUTH_FLAG_MOBILE ) != 0x00;
    }
}
