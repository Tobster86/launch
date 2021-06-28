/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.utilities;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import launch.game.Defs;
import launch.game.GeoCoord;
import launch.game.LaunchClientGame;
import launch.game.entities.*;
import static launch.utilities.LaunchLog.LogType.LOCATIONS;

/**
 *
 * @author tobster
 */
public class LaunchUtilities
{
    private static final String SANCTIFIED_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static Random random = new Random();

    public static boolean NEUTRAL_VISIBLE = true;
    public static boolean FRIENDLY_VISIBLE = true;
    public static boolean ENEMY_VISIBLE = true;

    public static boolean PLAYERS_VISIBLE = true;
    public static boolean MISSILE_SITES_VISIBLE = true;
    public static boolean SAM_SITES_VISIBLE = true;
    public static boolean MISSILES_VISIBLE = true;
    public static boolean INTERCEPTORS_VISIBLE = true;
    public static boolean SENTRY_GUNS_VISIBLE = true;
    public static boolean ORE_MINES_VISIBLE = true;
    public static boolean LOOTS_VISIBLE = true;
    
    private static final int STRING_LENGTH_PREFIX_SIZE = 2;
    
    private static final float MS_PER_HOUR = 3600000.0f;
    
    public static int GetStringDataSize(String str)
    {
        return STRING_LENGTH_PREFIX_SIZE + str.getBytes().length;
    }
    
    public static byte[] GetStringData(String str)
    {
        ByteBuffer bb = ByteBuffer.allocate(GetStringDataSize(str));
        byte[] cString = str.getBytes();
        bb.putShort((short)cString.length);
        bb.put(cString);
        return bb.array();
    }
    
    public static String StringFromData(ByteBuffer bb)
    {
        byte[] cString = new byte[bb.getShort()];
        bb.get(cString, 0, cString.length);
        return new String(cString);
    }
    
    public static List<Integer> IntListFromData(ByteBuffer bb)
    {
        List<Integer> Result = new ArrayList();
        
        short nCount = bb.getShort();

        for(int i = 0; i < nCount; i++)
        {
            Result.add(bb.getInt());
        }
        
        return Result;
    }
    
    public static byte[] GetIntListData(List<Integer> IntList)
    {
        ByteBuffer bb = ByteBuffer.allocate(Short.BYTES + (IntList.size() * Integer.BYTES));
        bb.putShort((short)IntList.size());
        
        for(Integer integer : IntList)
        {
            bb.putInt(integer);
        }
        
        return bb.array();
    }

    public static long GetNextHour()
    {
        //Determine the ms until the next hour.
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY) + 1);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        
        return cal.getTimeInMillis() - System.currentTimeMillis();
    }

    public static boolean GetEntityVisibility(LaunchClientGame game, LaunchEntity entity)
    {
        boolean bEntityTypeVisible = false;

        if(entity instanceof Player)
            bEntityTypeVisible = PLAYERS_VISIBLE;
        else if(entity instanceof MissileSite)
            bEntityTypeVisible = MISSILE_SITES_VISIBLE;
        else if(entity instanceof SAMSite)
            bEntityTypeVisible = SAM_SITES_VISIBLE;
        else if(entity instanceof SentryGun)
            bEntityTypeVisible = SENTRY_GUNS_VISIBLE;
        else if(entity instanceof OreMine)
            bEntityTypeVisible = ORE_MINES_VISIBLE;
        else if(entity instanceof Missile)
            bEntityTypeVisible = MISSILES_VISIBLE;
        else if(entity instanceof Interceptor)
            bEntityTypeVisible = INTERCEPTORS_VISIBLE;
        else if(entity instanceof Loot)
            return LOOTS_VISIBLE; //Considered sideless.

        if(bEntityTypeVisible)
        {
            switch(game.GetAllegiance(game.GetOurPlayer(), entity))
            {
                case YOU:
                case AFFILIATE:
                case ALLY:
                case PENDING_TREATY:
                {
                    return FRIENDLY_VISIBLE;
                }

                case ENEMY: return ENEMY_VISIBLE;

                case NEUTRAL: return NEUTRAL_VISIBLE;
            }
        }
        else
        {
            return false;
        }

        //Everything else is visible.
        return true;
    }
    
    /**
     * Returns a string of the format [Three random Latin letters and numbers].
     * @return A sanctified string.
     */
    public static String GetRandomSanctifiedString()
    {
        return "[" +
        SANCTIFIED_CHARACTERS.charAt(random.nextInt(SANCTIFIED_CHARACTERS.length())) +
        SANCTIFIED_CHARACTERS.charAt(random.nextInt(SANCTIFIED_CHARACTERS.length())) +
        SANCTIFIED_CHARACTERS.charAt(random.nextInt(SANCTIFIED_CHARACTERS.length())) +
        "] ";
    }
    
    /**
     * Remove dangerous characters and wicked phrases from text.
     * @param strText Text to clean if applicable.
     * @param bDangerous Remove dangerous characters. Note: Alliance descriptions should be cut slack on this to allow URLs, unless this feature is separated out in future.
     * @param bProfane Remove profane terms.
     * @return A clean text string.
     */
    public static String SanitiseText(String strText, boolean bDangerous, boolean bProfane)
    {
        //Dangerous.
        if(bDangerous)
        {
            strText = strText.replace("\\", "|");
            strText = strText.replace("/", "|");
            strText = strText.replace("\"", "|");
        }
        
        //Profane.
        if(bProfane)
        {
            strText = strText.replaceAll("(?i)hitler", "Jesus");
            strText = strText.replaceAll("(?i)fuck", "love");
            strText = strText.replaceAll("(?i)nigga", "angel");
            strText = strText.replaceAll("(?i)nigger", "angel");
        }
        
        return strText;
    }
    
    /**
    public static boolean 
     * Ensures a name is greppable with Latin alphanumeric + numeric characters. Forces it if not. While we're here, deal with some other unwanted name artefacts.
     * @param strName The player or alliance name to bless.
     * @return A blessed, greppable name.
     */
    public static String BlessName(String strName)
    {
        strName = strName.trim();
        
        if(strName.length() > Defs.MAX_PLAYER_NAME_LENGTH)
            strName = GetRandomSanctifiedString() + "My name was too long";
        else
        {
            strName = SanitiseText(strName, true, true);

            if(!strName.matches("^.*[a-zA-Z0-9][a-zA-Z0-9][a-zA-Z0-9].*$"))
            {
                strName = GetRandomSanctifiedString() + strName;
            }
        }
        
        return strName;
    }
}
