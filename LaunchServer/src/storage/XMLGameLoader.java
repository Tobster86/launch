/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package storage;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import launch.game.Alliance;
import launch.game.Config;
import launch.game.Defs;
import launch.game.GeoCoord;
import launch.game.LaunchServerGame;
import launch.game.User;
import launch.game.entities.Interceptor;
import launch.game.entities.Loot;
import launch.game.entities.Missile;
import launch.game.entities.MissileSite;
import launch.game.entities.OreMine;
import launch.game.entities.Player;
import launch.game.entities.Radiation;
import launch.game.entities.SAMSite;
import launch.game.entities.SentryGun;
import launch.game.treaties.Treaty.Type;
import launch.game.systems.MissileSystem;
import launch.game.treaties.Affiliation;
import launch.game.treaties.AffiliationRequest;
import launch.game.treaties.War;
import launch.game.types.*;
import launch.utilities.LaunchBannedApp;
import launch.utilities.LaunchLog;
import static launch.utilities.LaunchLog.LogType.APPLICATION;
import launch.utilities.LaunchReport;
import launch.utilities.ShortDelay;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author tobster
 */
public class XMLGameLoader
{
    private static final String LOG_NAME = "Loader";
    
    private static GameLoadSaveListener currentListener;
    
    private static String strLastHandled = "";
    
    public static Config LoadConfig(GameLoadSaveListener listener, String strConfigFile)
    {
        currentListener = listener;
        Config config = null;
        
        try
        {
            File file = new File(strConfigFile);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            
            LaunchLog.Log(APPLICATION, LOG_NAME, "Loading config xml...");
            
            config = new Config(GetStringRootElement(doc, XMLDefs.SERVER_EMAIL),
                    GetByteRootElement(doc, XMLDefs.VARIANT),
                    GetByteRootElement(doc, XMLDefs.DEBUG_FLAGS),
                    GetIntRootElement(doc, XMLDefs.STARTING_WEALTH),
                    GetIntRootElement(doc, XMLDefs.RESPAWN_WEALTH),
                    GetIntRootElement(doc, XMLDefs.RESPAWN_TIME),
                    GetIntRootElement(doc, XMLDefs.RESPAWN_PROTECTION_TIME),
                    GetIntRootElement(doc, XMLDefs.HOURLY_WEALTH),
                    GetIntRootElement(doc, XMLDefs.CMS_SYSTEM_COST),
                    GetIntRootElement(doc, XMLDefs.SAM_SYSTEM_COST),
                    GetIntRootElement(doc, XMLDefs.CMS_STRUCTURE_COST),
                    GetIntRootElement(doc, XMLDefs.NCMS_STRUCTURE_COST),
                    GetIntRootElement(doc, XMLDefs.SAM_STRUCTURE_COST),
                    GetIntRootElement(doc, XMLDefs.SENTRY_GUN_STRUCTURE_COST),
                    GetIntRootElement(doc, XMLDefs.ORE_MINE_STRUCTURE_COST),
                    GetFloatRootElement(doc, XMLDefs.INTERCEPTOR_BASE_HIT_CHANCE),
                    GetFloatRootElement(doc, XMLDefs.RUBBLE_MIN_VALUE),
                    GetFloatRootElement(doc, XMLDefs.RUBBLE_MAX_VALUE),
                    GetIntRootElement(doc, XMLDefs.RUBBLE_MIN_TIME),
                    GetIntRootElement(doc, XMLDefs.RUBBLE_MAX_TIME),
                    GetFloatRootElement(doc, XMLDefs.STRUCTURE_SEPARATION),
                    GetShortRootElement(doc, XMLDefs.PLAYER_BASE_HP),
                    GetShortRootElement(doc, XMLDefs.STRUCTURE_BASE_HP),
                    GetIntRootElement(doc, XMLDefs.STRUCTURE_BOOT_TIME),
                    GetByteRootElement(doc, XMLDefs.INITIAL_CSM_SLOTS),
                    GetByteRootElement(doc, XMLDefs.INITIAL_SAM_SLOTS),
                    GetFloatRootElement(doc, XMLDefs.REQUIRED_ACCURACY),
                    GetIntRootElement(doc, XMLDefs.MIN_RADIATION_TIME),
                    GetIntRootElement(doc, XMLDefs.MAX_RADIATION_TIME),
                    GetIntRootElement(doc, XMLDefs.MISSILE_SLOT_UPGRADE_BASE_COST),
                    GetByteRootElement(doc, XMLDefs.MISSILE_SLOT_UPGRADE_COUNT),
                    GetFloatRootElement(doc, XMLDefs.RESALE_VALUE),
                    GetIntRootElement(doc, XMLDefs.DECOMMISSION_TIME),
                    GetIntRootElement(doc, XMLDefs.RELOAD_TIME_BASE),
                    GetIntRootElement(doc, XMLDefs.RELOAD_TIME_STAGE1),
                    GetIntRootElement(doc, XMLDefs.RELOAD_TIME_STAGE2),
                    GetIntRootElement(doc, XMLDefs.RELOAD_TIME_STAGE3),
                    GetIntRootElement(doc, XMLDefs.RELOAD_STAGE1_COST),
                    GetIntRootElement(doc, XMLDefs.RELOAD_STAGE2_COST),
                    GetIntRootElement(doc, XMLDefs.RELOAD_STAGE3_COST),
                    GetFloatRootElement(doc, XMLDefs.REPAIR_SALVAGE_DISTANCE),
                    GetIntRootElement(doc, XMLDefs.MISSILE_SITE_MAINTENANCE_COST),
                    GetIntRootElement(doc, XMLDefs.SAM_SITE_MAINTENANCE_COST),
                    GetIntRootElement(doc, XMLDefs.SENTRY_GUN_MAINTENANCE_COST),
                    GetIntRootElement(doc, XMLDefs.ORE_MINE_MAINTENANCE_COST),
                    GetIntRootElement(doc, XMLDefs.HEALTH_INTERVAL),
                    GetIntRootElement(doc, XMLDefs.RADIATION_INTERVAL),
                    GetIntRootElement(doc, XMLDefs.PLAYER_REPAIR_COST),
                    GetIntRootElement(doc, XMLDefs.STRUCTURE_REPAIR_COST),
                    GetLongRootElement(doc, XMLDefs.AWOL_TIME),
                    GetLongRootElement(doc, XMLDefs.REMOVE_TIME),
                    GetIntRootElement(doc, XMLDefs.NUKE_UPGRADE_COST),
                    GetIntRootElement(doc, XMLDefs.ALLIANCE_COOLOFF_TIME),
                    GetIntRootElement(doc, XMLDefs.MISSILE_NUCLEAR_COST),
                    GetIntRootElement(doc, XMLDefs.MISSILE_TRACKING_COST),
                    GetIntRootElement(doc, XMLDefs.MISSILE_ECM_COST),
                    GetFloatRootElement(doc, XMLDefs.EMP_CHANCE),
                    GetFloatRootElement(doc, XMLDefs.EMP_RADIUS_MULTIPLIER),
                    GetFloatRootElement(doc, XMLDefs.ECM_INTERCEPTOR_CHANCE_REDUCTION),
                    GetFloatRootElement(doc, XMLDefs.MANUAL_INTERCEPTOR_CHANCE_INCREASE),
                    GetIntRootElement(doc, XMLDefs.SENTRY_GUN_RELOAD_TIME),
                    GetFloatRootElement(doc, XMLDefs.SENTRY_GUN_RANGE),
                    GetFloatRootElement(doc, XMLDefs.SENTRY_GUN_HIT_CHANCE),
                    GetFloatRootElement(doc, XMLDefs.ORE_MINE_RADIUS),
                    GetIntRootElement(doc, XMLDefs.MAX_ORE_VALUE),
                    GetIntRootElement(doc, XMLDefs.ORE_MINE_GENERATE_TIME),
                    GetIntRootElement(doc, XMLDefs.ORE_MIN_EXPIRY),
                    GetIntRootElement(doc, XMLDefs.ORE_MAX_EXPIRY),
                    GetIntRootElement(doc, XMLDefs.MISSILE_SPEED_INDEX_COST),
                    GetFloatRootElement(doc, XMLDefs.MISSILE_SPEED_INDEX_COST_POW),
                    GetIntRootElement(doc, XMLDefs.MISSILE_RANGE_INDEX_COST),
                    GetFloatRootElement(doc, XMLDefs.MISSILE_RANGE_INDEX_COST_POW),
                    GetIntRootElement(doc, XMLDefs.MISSILE_BLAST_RADIUS_INDEX_COST),
                    GetFloatRootElement(doc, XMLDefs.MISSILE_BLAST_RADIUS_INDEX_COST_POW),
                    GetIntRootElement(doc, XMLDefs.NUKE_BLAST_RADIUS_INDEX_COST),
                    GetFloatRootElement(doc, XMLDefs.NUKE_BLAST_RADIUS_INDEX_COST_POW),
                    GetIntRootElement(doc, XMLDefs.MISSILE_MAX_DAMAGE_INDEX_COST),
                    GetFloatRootElement(doc, XMLDefs.MISSILE_MAX_DAMAGE_INDEX_COST_POW),
                    GetIntRootElement(doc, XMLDefs.INTERCEPTOR_SPEED_INDEX_COST),
                    GetFloatRootElement(doc, XMLDefs.INTERCEPTOR_SPEED_INDEX_COST_POW),
                    GetIntRootElement(doc, XMLDefs.INTERCEPTOR_RANGE_INDEX_COST),
                    GetFloatRootElement(doc, XMLDefs.INTERCEPTOR_RANGE_INDEX_COST_POW),
                    GetFloatRootElement(doc, XMLDefs.MISSILE_PREP_TIME_PER_MAGNITUDE),
                    GetFloatRootElement(doc, XMLDefs.INTERCEPTOR_PREP_TIME_PER_MAGNITUDE),
                    GetIntRootElement(doc, XMLDefs.HOURLY_BONUS_DIPLOMATIC_PRESENCE),
                    GetIntRootElement(doc, XMLDefs.HOURLY_BONUS_POLITICAL_ENGAGEMENT),
                    GetIntRootElement(doc, XMLDefs.HOURLY_BONUS_DEFENDER_OF_THE_NATION),
                    GetIntRootElement(doc, XMLDefs.HOURLY_BONUS_NUCLEAR_SUPERPOWER),
                    GetIntRootElement(doc, XMLDefs.HOURLY_BONUS_WEEKLY_KILLS_BATCH),
                    GetIntRootElement(doc, XMLDefs.HOURLY_BONUS_SURVIVOR),
                    GetIntRootElement(doc, XMLDefs.HOURLY_BONUS_HIPPY),
                    GetIntRootElement(doc, XMLDefs.HOURLY_BONUS_PEACE_MAKER),
                    GetIntRootElement(doc, XMLDefs.HOURLY_BONUS_WAR_MONGER),
                    GetIntRootElement(doc, XMLDefs.HOURLY_BONUS_LONE_WOLF),
                    GetFloatRootElement(doc, XMLDefs.LONE_WOLF_DISTANCE));
            
            config.SetPort(GetIntRootElement(doc, XMLDefs.PORT));
      
            //Load weapon property tables.
            config.SetMissileSpeeds(GetFloatHashMap(doc, XMLDefs.MISSILE_SPEED, XMLDefs.SPEED, listener));
            config.SetMissileRanges(GetFloatHashMap(doc, XMLDefs.MISSILE_RANGE, XMLDefs.RANGE, listener));
            config.SetMissileBlastRadii(GetFloatHashMap(doc, XMLDefs.MISSILE_BLAST_RADIUS, XMLDefs.BLAST_RADIUS, listener));
            config.SetNukeBlastRadii(GetFloatHashMap(doc, XMLDefs.NUKE_BLAST_RADIUS, XMLDefs.BLAST_RADIUS, listener));
            config.SetMissileMaxDamages(GetShortHashMap(doc, XMLDefs.MISSILE_MAX_DAMAGE, XMLDefs.MAX_DAMAGE, listener));
            config.SetInterceptorSpeeds(GetFloatHashMap(doc, XMLDefs.INTERCEPTOR_SPEED, XMLDefs.SPEED, listener));
            config.SetInterceptorRanges(GetFloatHashMap(doc, XMLDefs.INTERCEPTOR_RANGE, XMLDefs.RANGE, listener));
            
            //Load missile types.
            NodeList ndeMissileTypes = doc.getElementsByTagName(XMLDefs.MISSILE_TYPE);
            
            for(int i = 0; i < ndeMissileTypes.getLength(); i++)
            {
                try
                {
                    Element ndeMissileType = (Element)ndeMissileTypes.item(i);
                    byte cID = GetByteAttribute(ndeMissileType, XMLDefs.ID);
                    String strName = GetStringAttribute(ndeMissileType, XMLDefs.NAME);
                    int lAssetID = GetIntElement(ndeMissileType, XMLDefs.ASSET_ID);
                    boolean bPurchasable = GetBooleanElement(ndeMissileType, XMLDefs.PURCHASABLE);
                    byte cSpeedIndex = GetByteElement(ndeMissileType, XMLDefs.SPEED_INDEX);
                    byte cBlastRadiusIndex = GetByteElement(ndeMissileType, XMLDefs.BLAST_RADIUS_INDEX);
                    byte cRangeIndex = GetByteElement(ndeMissileType, XMLDefs.RANGE_INDEX);
                    byte cMaxDamageIndex = GetByteElement(ndeMissileType, XMLDefs.MAX_DAMAGE_INDEX);
                    boolean bNuclear = GetBooleanElement(ndeMissileType, XMLDefs.NUCLEAR);
                    boolean bTracking = GetBooleanElement(ndeMissileType, XMLDefs.TRACKING);
                    boolean bECM = GetBooleanElement(ndeMissileType, XMLDefs.ECM);
                    
                    config.AddMissileType(cID, new MissileType(cID, bPurchasable, strName, lAssetID, bNuclear, bTracking, bECM, cSpeedIndex, cRangeIndex, cBlastRadiusIndex, cMaxDamageIndex));
                }
                catch(Exception ex)
                {
                    listener.LoadError(String.format("Error loading missile type at index %d: %s.", i, ex.getMessage()));
                }
            }
            
            //Load interceptor types.
            NodeList ndeInterceptorTypes = doc.getElementsByTagName(XMLDefs.INTERCEPTOR_TYPE);
            
            for(int i = 0; i < ndeInterceptorTypes.getLength(); i++)
            {
                try
                {
                    Element ndeInterceptorType = (Element)ndeInterceptorTypes.item(i);
                    byte cID = GetByteAttribute(ndeInterceptorType, XMLDefs.ID);
                    String strName = GetStringAttribute(ndeInterceptorType, XMLDefs.NAME);
                    int lAssetID = GetIntElement(ndeInterceptorType, XMLDefs.ASSET_ID);
                    boolean bPurchasable = GetBooleanElement(ndeInterceptorType, XMLDefs.PURCHASABLE);
                    byte cSpeedIndex = GetByteElement(ndeInterceptorType, XMLDefs.SPEED_INDEX);
                    byte cRangeIndex = GetByteElement(ndeInterceptorType, XMLDefs.RANGE_INDEX);
                    
                    config.AddInterceptorType(cID, new InterceptorType(cID, bPurchasable, strName, lAssetID, cSpeedIndex, cRangeIndex));
                }
                catch(Exception ex)
                {
                    listener.LoadError(String.format("Error loading interceptor type at index %d: %s.", i, ex.getMessage()));
                }
            }
            
            //Load minor banned apps.
            NodeList ndeMinorCheatingApps = doc.getElementsByTagName(XMLDefs.MINOR_BANNED_APP);
            
            for(int i = 0; i < ndeMinorCheatingApps.getLength(); i++)
            {
                try
                {
                    Element ndeMinorCheatingApp = (Element)ndeMinorCheatingApps.item(i);
                    String strName = GetStringAttribute(ndeMinorCheatingApp, XMLDefs.NAME);
                    String strSignature = GetStringElement(ndeMinorCheatingApp, XMLDefs.SIGNATURE);
                    String strDescription = GetStringElement(ndeMinorCheatingApp, XMLDefs.DESCRIPTION);
                    
                    config.AddMinorCheatingApp(new LaunchBannedApp(strName, strSignature, strDescription));
                }
                catch(Exception ex)
                {
                    listener.LoadError(String.format("Error loading minor cheating app at index %d: %s.", i, ex.getMessage()));
                }
            }
            
            //Load major banned apps.
            NodeList ndeMajorCheatingApps = doc.getElementsByTagName(XMLDefs.MAJOR_BANNED_APP);
            
            for(int i = 0; i < ndeMajorCheatingApps.getLength(); i++)
            {
                try
                {
                    Element ndeMajorCheatingApp = (Element)ndeMajorCheatingApps.item(i);
                    String strName = GetStringAttribute(ndeMajorCheatingApp, XMLDefs.NAME);
                    String strSignature = GetStringElement(ndeMajorCheatingApp, XMLDefs.SIGNATURE);
                    String strDescription = GetStringElement(ndeMajorCheatingApp, XMLDefs.DESCRIPTION);
                    
                    config.AddMajorCheatingApp(new LaunchBannedApp(strName, strSignature, strDescription));
                }
                catch(Exception ex)
                {
                    listener.LoadError(String.format("Error loading major cheating app at index %d: %s.", i, ex.getMessage()));
                }
            }
            
            config.Finalise();
        }
        catch (ParserConfigurationException ex)
        {
            listener.LoadError(String.format("XML parser configuration error when loading config from %s. Last handled: %s", strConfigFile, strLastHandled));
        }
        catch (SAXException ex)
        {
            listener.LoadError(String.format("SAX exception when loading config from %s. Last handled: %s", strConfigFile, strLastHandled));
        }
        catch (IOException ex)
        {
            listener.LoadError(String.format("IO error when loading config from %s. Last handled: %s", strConfigFile, strLastHandled));
        }
        catch(Exception ex)
        {
            listener.LoadError(String.format("Other error when loading config from %s. Last handled: %s", strConfigFile, strLastHandled));
        }
        
        return config;
    }
    
    public static void LoadGame(GameLoadSaveListener listener, String strGameFile, LaunchServerGame game)
    {
        currentListener = listener;
        
        LaunchLog.Log(APPLICATION, LOG_NAME, "Loading game xml...");
        
        try
        {
            File file = new File(strGameFile);
            
            if(!file.exists())
            {
                //No game file. Start without one.
                return;
            }
            
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            
            //Load users.
            NodeList ndeUsers = doc.getElementsByTagName(XMLDefs.USER);

            for(int i = 0; i < ndeUsers.getLength(); i++)
            {
                try
                {
                    Element eleUser = (Element)ndeUsers.item(i);
                    String strIMEI = GetStringElement(eleUser, XMLDefs.IMEI);
                    int lPlayerID = GetIntElement(eleUser, XMLDefs.PLAYERID);
                    byte cBanState = GetByteElement(eleUser,XMLDefs.BAN_STATE);
                    long oNextBanTime = GetLongElement(eleUser, XMLDefs.NEXT_BAN_TIME);
                    long oBanDurationRemaining = GetLongElement(eleUser, XMLDefs.BAN_DURATION_REMAINING);
                    String strBanReason = GetStringElement(eleUser, XMLDefs.BAN_REASON);
                    String strLastIP = GetStringElement(eleUser, XMLDefs.LAST_IP);
                    boolean bLastTypeMobile = GetBooleanElement(eleUser, XMLDefs.LAST_CONNECTION_MOBILE);
                    long oCheckedDate = GetLongElement(eleUser, XMLDefs.LAST_CHECKED);
                    boolean bLastCheckFailed = GetBooleanElement(eleUser, XMLDefs.LAST_CHECK_FAILED);
                    boolean bCheckAPIFailed = GetBooleanElement(eleUser, XMLDefs.CHECK_API_FAILED);
                    boolean bProscribed = GetBooleanElement(eleUser, XMLDefs.PROSCRIBED);
                    int lCheckFailCode = GetIntElement(eleUser, XMLDefs.CHECK_FAIL_CODE);
                    boolean bProfileMatch = GetBooleanElement(eleUser, XMLDefs.PROFILE_MATCH);
                    boolean bBasicIntegrity = GetBooleanElement(eleUser, XMLDefs.BASIC_INTEGRITY);
                    boolean bApproved = GetBooleanElement(eleUser, XMLDefs.APPROVED);
                    String strDeviceHash = GetStringElement(eleUser, XMLDefs.DEVICE_HASH);
                    String strAppListHash = GetStringElement(eleUser, XMLDefs.APP_LIST_HASH);
                    long oExpired = GetLongElement(eleUser, XMLDefs.EXPIRED);
                    
                    User user = new User(strIMEI, lPlayerID, cBanState, oNextBanTime, oBanDurationRemaining, strBanReason, strLastIP, bLastTypeMobile, oCheckedDate, bLastCheckFailed, bCheckAPIFailed, bProscribed, lCheckFailCode, bProfileMatch, bBasicIntegrity, bApproved, oExpired, strDeviceHash, strAppListHash);
                    
                    NodeList ndeReports = GetNodes(eleUser, XMLDefs.REPORT);
                    
                    for(int j = 0; j < ndeReports.getLength(); j++)
                    {
                        Element eleReport = (Element)ndeReports.item(j);
                        long oTimeStart = GetLongElement(eleReport, XMLDefs.TIME_START);
                        long oTimeEnd = GetLongElement(eleReport, XMLDefs.TIME_END);
                        String strMessage = GetStringElement(eleReport, XMLDefs.MESSAGE);
                        boolean bIsMajor = GetBooleanElement(eleReport, XMLDefs.IS_MAJOR);
                        int lLeftID = GetIntElement(eleReport, XMLDefs.LEFT_ID);
                        int lRightID = GetIntElement(eleReport, XMLDefs.RIGHT_ID);
                        byte cTimes = GetByteElement(eleReport, XMLDefs.TIMES);
                        byte cFlags = GetByteElement(eleReport, XMLDefs.FLAGS);
                        
                        user.AddReport(new LaunchReport(oTimeStart, oTimeEnd, strMessage, bIsMajor, lLeftID, lRightID, cTimes, cFlags));
                    }
                    
                    game.AddUser(user);
                }
                catch(Exception ex)
                {
                    listener.LoadError(String.format("Error loading user at index %d: %s. Last element: %s", i, ex.getMessage(), strLastHandled));
                }
            }
            
            //Load alliances.
            NodeList nodeAlliances = doc.getElementsByTagName(XMLDefs.ALLIANCE);
            
            for(int i = 0; i < nodeAlliances.getLength(); i++)
            {
                try
                {
                    Element eleAlliance = (Element)nodeAlliances.item(i);
                    int lID = GetIntAttribute(eleAlliance, XMLDefs.ID);
                    String strName = GetStringAttribute(eleAlliance, XMLDefs.NAME);
                    String strDescription = GetStringElement(eleAlliance, XMLDefs.DESCRIPTION);
                    int lAvatarID = GetIntElement(eleAlliance, XMLDefs.AVATAR);
                    
                    game.AddAlliance(new Alliance(lID, strName, strDescription, lAvatarID), false);
                }
                catch(Exception ex)
                {
                    listener.LoadError(String.format("Error loading alliance at index %d: %s.", i, ex.getMessage()));
                }
            }
            
            //Load treaties.
            NodeList nodeTreaties = doc.getElementsByTagName(XMLDefs.TREATY);
            
            for(int i = 0; i < nodeTreaties.getLength(); i++)
            {
                try
                {
                    Element eleTreaty = (Element)nodeTreaties.item(i);
                    int lID = GetIntAttribute(eleTreaty, XMLDefs.ID);
                    int lAlliance1 = GetIntElement(eleTreaty, XMLDefs.ALLIANCE1);
                    int lAlliance2 = GetIntElement(eleTreaty, XMLDefs.ALLIANCE2);
                    Type type = Type.values()[GetIntElement(eleTreaty, XMLDefs.TYPE)];
                    
                    if(type == Type.WAR)
                    {
                        short nKills1 = GetShortElement(eleTreaty, XMLDefs.KILLS1);
                        short nDeaths1 = GetShortElement(eleTreaty, XMLDefs.DEATHS1);
                        int lOffenceSpending1 = GetIntElement(eleTreaty, XMLDefs.OFFENCE_SPENDING1);
                        int lDefenceSpending1 = GetIntElement(eleTreaty, XMLDefs.DEFENCE_SPENDING1);
                        int lDamageInflicted1 = GetIntElement(eleTreaty, XMLDefs.DAMAGE_INFLICTED1);
                        int lDamageReceived1 = GetIntElement(eleTreaty, XMLDefs.DAMAGE_RECEIVED1);
                        int lIncome1 = GetIntElement(eleTreaty, XMLDefs.INCOME1);
                        short nKills2 = GetShortElement(eleTreaty, XMLDefs.KILLS2);
                        short nDeaths2 = GetShortElement(eleTreaty, XMLDefs.DEATHS2);
                        int lOffenceSpending2 = GetIntElement(eleTreaty, XMLDefs.OFFENCE_SPENDING2);
                        int lDefenceSpending2 = GetIntElement(eleTreaty, XMLDefs.DEFENCE_SPENDING2);
                        int lDamageInflicted2 = GetIntElement(eleTreaty, XMLDefs.DAMAGE_INFLICTED2);
                        int lDamageReceived2 = GetIntElement(eleTreaty, XMLDefs.DAMAGE_RECEIVED2);
                        int lIncome2 = GetIntElement(eleTreaty, XMLDefs.INCOME2);

                        game.AddTreaty(new War(lID, lAlliance1, lAlliance2, nKills1, nDeaths1, lOffenceSpending1, lDefenceSpending1, lDamageInflicted1, lDamageReceived1, lIncome1, nKills2, nDeaths2, lOffenceSpending2, lDefenceSpending2, lDamageInflicted2, lDamageReceived2, lIncome2));
                    }
                    else
                    {
                        switch(type)
                        {
                            case AFFILIATION: game.AddTreaty(new Affiliation(lID, lAlliance1, lAlliance2)); break;
                            case AFFILIATION_REQUEST: game.AddTreaty(new AffiliationRequest(lID, lAlliance1, lAlliance2)); break;
                        }
                    }
                }
                catch(Exception ex)
                {
                    listener.LoadError(String.format("Error loading treaty at index %d: %s.", i, ex.getMessage()));
                }
            }
            
            //Load players.
            NodeList nodes = doc.getElementsByTagName(XMLDefs.PLAYER);

            for(int i = 0; i < nodes.getLength(); i++)
            {
                try
                {
                    Element elePlayer = (Element)nodes.item(i);
                    int lID = GetIntAttribute(elePlayer, XMLDefs.ID);
                    GeoCoord geoPosition = GetPositionElement(elePlayer, XMLDefs.POSITION);
                    short nHP = GetShortElement(elePlayer, XMLDefs.HP);
                    short nMaxHP = GetShortElement(elePlayer, XMLDefs.MAX_HP);
                    String strName = GetStringAttribute(elePlayer, XMLDefs.NAME);
                    int lAvatarID = GetIntElement(elePlayer, XMLDefs.AVATAR);
                    int lWealth = GetIntElement(elePlayer, XMLDefs.WEALTH);
                    long oLastSeen = GetLongElement(elePlayer, XMLDefs.LAST_SEEN);
                    int lStateChange = GetIntElement(elePlayer, XMLDefs.STATE_CHANGE);
                    int lAllianceID = GetIntElement(elePlayer, XMLDefs.ALLIANCE_ID);
                    byte cFlags1 = GetByteElement(elePlayer, XMLDefs.FLAGS1);
                    byte cFlags2 = GetByteElement(elePlayer, XMLDefs.FLAGS2);
                    int lAllianceCooloffTime = GetIntElement(elePlayer, XMLDefs.ALLIANCE_COOLOFF_TIME);
        
                    short nKills = GetShortElement(elePlayer, XMLDefs.KILLS);
                    short nDeaths = GetShortElement(elePlayer, XMLDefs.DEATHS);
                    int lOffenceSpending = GetIntElement(elePlayer, XMLDefs.OFFENCE_SPENDING);
                    int lDefenceSpending = GetIntElement(elePlayer, XMLDefs.DEFENCE_SPENDING);
                    int lDamageInflicted = GetIntElement(elePlayer, XMLDefs.DAMAGE_INFLICTED);
                    int lDamageReceived = GetIntElement(elePlayer, XMLDefs.DAMAGE_RECEIVED);
                    
                    game.AddPlayer(new Player(lID, geoPosition, nHP, nMaxHP, strName, lAvatarID, lWealth, oLastSeen, lStateChange, lAllianceID, cFlags1, cFlags2, lAllianceCooloffTime, nKills, nDeaths, lOffenceSpending, lDefenceSpending, lDamageInflicted, lDamageReceived));

                    if(GetHasNode(elePlayer, XMLDefs.INTERCEPTOR_SYSTEM))
                    {
                        MissileSystem missileSystem = GetMissileSystem(elePlayer, XMLDefs.INTERCEPTOR_SYSTEM);
                        game.AddPlayerInterceptorSystem(lID, missileSystem);
                    }
                    
                    if(GetHasNode(elePlayer, XMLDefs.MISSILE_SYSTEM))
                    {
                        MissileSystem missileSystem = GetMissileSystem(elePlayer, XMLDefs.MISSILE_SYSTEM);
                        game.AddPlayerMissileSystem(lID, missileSystem);
                    }
                }
                catch(Exception ex)
                {
                    listener.LoadError(String.format("Error loading player at index %d: %s.", i, ex.getMessage()));
                }
            }
            
            LaunchLog.Log(APPLICATION, LOG_NAME, "Loading missiles...");
            
            //Load missiles.
            nodes = doc.getElementsByTagName(XMLDefs.MISSILE);

            for(int i = 0; i < nodes.getLength(); i++)
            {
                try
                {
                    Element eleMissile = (Element)nodes.item(i);
                    int lID = GetIntAttribute(eleMissile, XMLDefs.ID);
                    GeoCoord geoPosition = GetPositionElement(eleMissile, XMLDefs.POSITION);
                    byte cType = GetByteElement(eleMissile, XMLDefs.TYPE);
                    int lOwnerID = GetIntElement(eleMissile, XMLDefs.OWNER_ID);
                    boolean bTracking = GetBooleanElement(eleMissile, XMLDefs.TRACKING);
                    GeoCoord geoTarget = GetPositionElement(eleMissile, XMLDefs.TARGET);
                    int lTargetID = GetIntElement(eleMissile, XMLDefs.TARGET_ID);
                    
                    game.AddMissile(new Missile(lID, geoPosition, cType, lOwnerID, bTracking, geoTarget, lTargetID));
                }
                catch(Exception ex)
                {
                    listener.LoadError(String.format("Error loading missile at index %d: %s.", i, ex.getMessage()));
                }
            }
            
            LaunchLog.Log(APPLICATION, LOG_NAME, "Loading interceptors...");
            
            //Load interceptors.
            nodes = doc.getElementsByTagName(XMLDefs.INTERCEPTOR);

            for(int i = 0; i < nodes.getLength(); i++)
            {
                try
                {
                    Element eleInterceptor = (Element)nodes.item(i);
                    int lID = GetIntAttribute(eleInterceptor, XMLDefs.ID);
                    GeoCoord geoPosition = GetPositionElement(eleInterceptor, XMLDefs.POSITION);
                    int lOwnerID = GetIntElement(eleInterceptor, XMLDefs.OWNER_ID);
                    int lTargetID = GetIntElement(eleInterceptor, XMLDefs.TARGET_ID);
                    byte cType = GetByteElement(eleInterceptor, XMLDefs.TYPE);
                    boolean bPlayerLaunched = GetBooleanElement(eleInterceptor, XMLDefs.PLAYER_LAUNCHED);
                    
                    game.AddInterceptor(new Interceptor(lID, geoPosition, lOwnerID, lTargetID, cType, bPlayerLaunched));
                }
                catch(Exception ex)
                {
                    listener.LoadError(String.format("Error loading interceptor at index %d: %s.", i, ex.getMessage()));
                }
            }
            
            LaunchLog.Log(APPLICATION, LOG_NAME, "Loading missile sites...");
            
            //Load missile sites.
            nodes = doc.getElementsByTagName(XMLDefs.MISSILE_SITE);

            for(int i = 0; i < nodes.getLength(); i++)
            {
                try
                {
                    Element eleMissileSite = (Element)nodes.item(i);
                    int lID = GetIntAttribute(eleMissileSite, XMLDefs.ID);
                    String strName = GetStringAttribute(eleMissileSite, XMLDefs.NAME);
                    GeoCoord geoPosition = GetPositionElement(eleMissileSite, XMLDefs.POSITION);
                    short nHP = GetShortElement(eleMissileSite, XMLDefs.HP);
                    short nMaxHP = GetShortElement(eleMissileSite, XMLDefs.MAX_HP);
                    int lOwnerID = GetIntElement(eleMissileSite, XMLDefs.OWNER_ID);
                    byte cFlags = GetByteElement(eleMissileSite, XMLDefs.FLAGS);
                    int lStateTime = GetIntElement(eleMissileSite, XMLDefs.STATE_TIME);
                    boolean bCanTakeNukes = GetBooleanElement(eleMissileSite, XMLDefs.NUCLEAR);
                    MissileSystem missileSystem = GetMissileSystem(eleMissileSite, XMLDefs.MISSILE_SYSTEM);
                    int lChargeTime = GetIntElement(eleMissileSite, XMLDefs.CHARGE_OWNER_TIME);
                    
                    game.AddMissileSite(new MissileSite(lID, geoPosition, nHP, nMaxHP, strName, lOwnerID, cFlags, lStateTime, bCanTakeNukes, missileSystem, lChargeTime));
                }
                catch(Exception ex)
                {
                    listener.LoadError(String.format("Error loading missile site at index %d: %s.", i, ex.getMessage()));
                }
            }
            
            LaunchLog.Log(APPLICATION, LOG_NAME, "Loading SAM sites...");
            
            //Load SAM sites.
            nodes = doc.getElementsByTagName(XMLDefs.SAM_SITE);

            for(int i = 0; i < nodes.getLength(); i++)
            {
                try
                {
                    Element eleSAMSite = (Element)nodes.item(i);
                    int lID = GetIntAttribute(eleSAMSite, XMLDefs.ID);
                    String strName = GetStringAttribute(eleSAMSite, XMLDefs.NAME);
                    GeoCoord geoPosition = GetPositionElement(eleSAMSite, XMLDefs.POSITION);
                    short nHP = GetShortElement(eleSAMSite, XMLDefs.HP);
                    short nMaxHP = GetShortElement(eleSAMSite, XMLDefs.MAX_HP);
                    int lOwnerID = GetIntElement(eleSAMSite, XMLDefs.OWNER_ID);
                    byte cFlags = GetByteElement(eleSAMSite, XMLDefs.FLAGS);
                    int lStateTime = GetIntElement(eleSAMSite, XMLDefs.STATE_TIME);
                    byte cMode = GetByteElement(eleSAMSite, XMLDefs.MODE);
                    MissileSystem missileSystem = GetMissileSystem(eleSAMSite, XMLDefs.INTERCEPTOR_SYSTEM);
                    int lChargeTime = GetIntElement(eleSAMSite, XMLDefs.CHARGE_OWNER_TIME);
                    
                    game.AddSAMSite(new SAMSite(lID, geoPosition, nHP, nMaxHP, strName, lOwnerID, cFlags, lStateTime, cMode, missileSystem, lChargeTime));
                }
                catch(Exception ex)
                {
                    listener.LoadError(String.format("Error loading SAM site at index %d: %s.", i, ex.getMessage()));
                }
            }
            
            LaunchLog.Log(APPLICATION, LOG_NAME, "Loading sentry guns...");
            
            //Load sentry guns.
            nodes = doc.getElementsByTagName(XMLDefs.SENTRY_GUN);

            for(int i = 0; i < nodes.getLength(); i++)
            {
                try
                {
                    Element eleSentryGun = (Element)nodes.item(i);
                    int lID = GetIntAttribute(eleSentryGun, XMLDefs.ID);
                    String strName = GetStringAttribute(eleSentryGun, XMLDefs.NAME);
                    GeoCoord geoPosition = GetPositionElement(eleSentryGun, XMLDefs.POSITION);
                    short nHP = GetShortElement(eleSentryGun, XMLDefs.HP);
                    short nMaxHP = GetShortElement(eleSentryGun, XMLDefs.MAX_HP);
                    int lOwnerID = GetIntElement(eleSentryGun, XMLDefs.OWNER_ID);
                    byte cFlags = GetByteElement(eleSentryGun, XMLDefs.FLAGS);
                    int lStateTime = GetIntElement(eleSentryGun, XMLDefs.STATE_TIME);
                    int lChargeTime = GetIntElement(eleSentryGun, XMLDefs.CHARGE_OWNER_TIME);
                    int lReloadRemaining = GetIntElement(eleSentryGun, XMLDefs.RELOAD_REMAINING);
                    
                    game.AddSentryGun(new SentryGun(lID, geoPosition, nHP, nMaxHP, strName, lOwnerID, cFlags, lStateTime, lChargeTime, lReloadRemaining));
                }
                catch(Exception ex)
                {
                    listener.LoadError(String.format("Error loading sentry gun at index %d: %s.", i, ex.getMessage()));
                }
            }
            
            LaunchLog.Log(APPLICATION, LOG_NAME, "Loading ore mines...");
            
            //Load ore mines.
            nodes = doc.getElementsByTagName(XMLDefs.ORE_MINE);

            for(int i = 0; i < nodes.getLength(); i++)
            {
                try
                {
                    Element eleOreMine = (Element)nodes.item(i);
                    int lID = GetIntAttribute(eleOreMine, XMLDefs.ID);
                    String strName = GetStringAttribute(eleOreMine, XMLDefs.NAME);
                    GeoCoord geoPosition = GetPositionElement(eleOreMine, XMLDefs.POSITION);
                    short nHP = GetShortElement(eleOreMine, XMLDefs.HP);
                    short nMaxHP = GetShortElement(eleOreMine, XMLDefs.MAX_HP);
                    int lOwnerID = GetIntElement(eleOreMine, XMLDefs.OWNER_ID);
                    byte cFlags = GetByteElement(eleOreMine, XMLDefs.FLAGS);
                    int lStateTime = GetIntElement(eleOreMine, XMLDefs.STATE_TIME);
                    int lChargeTime = GetIntElement(eleOreMine, XMLDefs.CHARGE_OWNER_TIME);
                    int lGenerateTime = GetIntElement(eleOreMine, XMLDefs.GENERATE_TIME);
                    
                    game.AddOreMine(new OreMine(lID, geoPosition, nHP, nMaxHP, strName, lOwnerID, cFlags, lStateTime, lChargeTime, lGenerateTime));
                }
                catch(Exception ex)
                {
                    listener.LoadError(String.format("Error loading ore mine at index %d: %s.", i, ex.getMessage()));
                }
            }
            
            LaunchLog.Log(APPLICATION, LOG_NAME, "Loading loots...");
            
            //Load loots.
            nodes = doc.getElementsByTagName(XMLDefs.LOOT);

            for(int i = 0; i < nodes.getLength(); i++)
            {
                try
                {
                    Element eleLoot = (Element)nodes.item(i);
                    int lID = GetIntAttribute(eleLoot, XMLDefs.ID);
                    GeoCoord geoPosition = GetPositionElement(eleLoot, XMLDefs.POSITION);
                    int lValue = GetIntElement(eleLoot, XMLDefs.VALUE);
                    int lExpiry = GetIntElement(eleLoot, XMLDefs.EXPIRY);
                    String strDescription = GetStringElement(eleLoot, XMLDefs.DESCRIPTION);
                    
                    game.AddLoot(new Loot(lID, geoPosition, lValue, lExpiry, strDescription));
                }
                catch(Exception ex)
                {
                    listener.LoadError(String.format("Error loading loot at index %d: %s.", i, ex.getMessage()));
                }
            }
            
            LaunchLog.Log(APPLICATION, LOG_NAME, "Loading radiations...");
            
            //Load radiations.
            nodes = doc.getElementsByTagName(XMLDefs.RADIATION);

            for(int i = 0; i < nodes.getLength(); i++)
            {
                try
                {
                    Element eleRadiation = (Element)nodes.item(i);
                    int lID = GetIntAttribute(eleRadiation, XMLDefs.ID);
                    GeoCoord geoPosition = GetPositionElement(eleRadiation, XMLDefs.POSITION);
                    float fltRadius = GetFloatElement(eleRadiation, XMLDefs.RADIUS);
                    int lExpiry = GetIntElement(eleRadiation, XMLDefs.EXPIRY);
                    
                    game.AddRadiation(new Radiation(lID, geoPosition, fltRadius, lExpiry));
                }
                catch(Exception ex)
                {
                    listener.LoadError(String.format("Error loading radiation at index %d: %s.", i, ex.getMessage()));
                }
            }
        }
        catch (ParserConfigurationException ex)
        {
            listener.LoadError(String.format("XML parser configuration error when loading game from %s.", strGameFile));
        }
        catch (SAXException ex)
        {
            listener.LoadError(String.format("SAX exception when loading game from %s.", strGameFile));
        }
        catch (IOException ex)
        {
            listener.LoadError(String.format("IO error when loading game from %s.", strGameFile));
        }
    }
    
    private static LinkedHashMap GetFloatHashMap(Document doc, String strOfNamedType, String strAttrName, GameLoadSaveListener listener)
    {
        LinkedHashMap<Byte, Float> Result = new LinkedHashMap();
        
        NodeList ndeMissileSpeeds = doc.getElementsByTagName(strOfNamedType);
        for(int i = 0; i < ndeMissileSpeeds.getLength(); i++)
        {
            try
            {
                Element ndeMissileSpeed = (Element)ndeMissileSpeeds.item(i);
                byte cID = GetByteAttribute(ndeMissileSpeed, XMLDefs.ID);
                float fltAttribute = GetFloatAttribute(ndeMissileSpeed, strAttrName);
                Result.put(cID, fltAttribute);
            }
            catch(Exception ex)
            {
                listener.LoadError(String.format("Error loading %s at index %d: %s.", strOfNamedType, i, ex.getMessage()));
            }
        }
        
        return Result;
    }
    
    private static LinkedHashMap GetShortHashMap(Document doc, String strOfNamedType, String strAttrName, GameLoadSaveListener listener)
    {
        LinkedHashMap<Byte, Short> Result = new LinkedHashMap();
        
        NodeList ndeMissileSpeeds = doc.getElementsByTagName(strOfNamedType);
        for(int i = 0; i < ndeMissileSpeeds.getLength(); i++)
        {
            try
            {
                Element ndeMissileSpeed = (Element)ndeMissileSpeeds.item(i);
                byte cID = GetByteAttribute(ndeMissileSpeed, XMLDefs.ID);
                short nAttribute = GetShortAttribute(ndeMissileSpeed, strAttrName);
                Result.put(cID, nAttribute);
            }
            catch(Exception ex)
            {
                listener.LoadError(String.format("Error loading %s at index %d: %s.", strOfNamedType, i, ex.getMessage()));
            }
        }
        
        return Result;
    }
    
    private static MissileSystem GetMissileSystem(Element eleParent, String strTagName)
    {
        strLastHandled = "Missile system " + strTagName;
        
        Element eleMissileSystem = GetNode(eleParent, strTagName);
        int lReloadRemaining = GetIntElement(eleMissileSystem, XMLDefs.RELOAD_REMAINING);
        int lReloadTime = GetIntElement(eleMissileSystem, XMLDefs.RELOAD_TIME);
        byte cSlotCount = GetByteElement(eleMissileSystem, XMLDefs.SLOT_COUNT);

        Map<Byte, Byte> SlotTypes = new HashMap();
        Map<Byte, ShortDelay> PrepTimes = new HashMap();
        NodeList ndeSlots = GetNodes(eleMissileSystem, XMLDefs.SLOT);

        for(int j = 0; j < ndeSlots.getLength(); j++)
        {
            Element eleSlot = (Element)ndeSlots.item(j);
            byte cNumber = GetByteAttribute(eleSlot, XMLDefs.NUMBER);
            SlotTypes.put(cNumber, GetByteAttribute(eleSlot, XMLDefs.TYPE));
            PrepTimes.put(cNumber, new ShortDelay(GetIntAttribute(eleSlot, XMLDefs.PREP_TIME)));
        }
        
        return new MissileSystem(lReloadRemaining, lReloadTime, cSlotCount, SlotTypes, PrepTimes);
    }
    
    private static Element GetNode(Element element, String strNodeName)
    {
        strLastHandled = "Node " + strNodeName;
        return (Element)element.getElementsByTagName(strNodeName).item(0);
    }
    
    private static NodeList GetNodes(Element element, String strNodeName)
    {
        strLastHandled = "Nodes " + strNodeName;
        return element.getElementsByTagName(strNodeName);
    }
    
    private static boolean GetHasNode(Element element, String strTagName)
    {
        strLastHandled = "Has Node " + strTagName;
        return element.getElementsByTagName(strTagName).getLength() > 0;
    }
    
    private static Element GetElement(Element element, String strTagName)
    {
        strLastHandled = "Element " + strTagName;
        return (Element)element.getElementsByTagName(strTagName).item(0);
    }
    
    private static String GetStringAttribute(Element element, String strAttribute)
    {
        strLastHandled = "String attribute " + strAttribute;
        
        try
        {
            return element.getAttribute(strAttribute);
        }
        catch(Exception ex)
        {
            currentListener.LoadWarning(String.format("Unable to get string attribute %s of %s. Returning empty string.", strAttribute, element.getNodeName()));
            return "";
        }
    }
    
    private static String GetStringRootElement(Document doc, String strTagName)
    {
        strLastHandled = "String root element " + strTagName;
        return doc.getElementsByTagName(strTagName).item(0).getTextContent();
    }
    
    private static int GetIntRootElement(Document doc, String strTagName)
    {
        strLastHandled = "Int root element " + strTagName;
        return Integer.parseInt(doc.getElementsByTagName(strTagName).item(0).getTextContent());
    }
    
    private static long GetLongRootElement(Document doc, String strTagName)
    {
        strLastHandled = "Long root element " + strTagName;
        return Long.parseLong(doc.getElementsByTagName(strTagName).item(0).getTextContent());
    }
    
    private static float GetFloatRootElement(Document doc, String strTagName)
    {
        strLastHandled = "Float root element " + strTagName;
        return Float.parseFloat(doc.getElementsByTagName(strTagName).item(0).getTextContent());
    }
    
    private static short GetShortRootElement(Document doc, String strTagName)
    {
        strLastHandled = "Short root element " + strTagName;
        return Short.parseShort(doc.getElementsByTagName(strTagName).item(0).getTextContent());
    }
    
    private static byte GetByteRootElement(Document doc, String strTagName)
    {
        strLastHandled = "Byte root element " + strTagName;
        
        try
        {
            return Byte.parseByte(doc.getElementsByTagName(strTagName).item(0).getTextContent());
        }
        catch(Exception ex)
        {
            currentListener.LoadWarning(String.format("Unable to get byte root element %s. Returning default 0.", strTagName));
            return 0;
        }
    }
    
    private static byte GetByteElement(Element element, String strTagName)
    {
        strLastHandled = "Byte root element " + strTagName;
        
        try
        {
            return Byte.parseByte(element.getElementsByTagName(strTagName).item(0).getTextContent());
        }
        catch(Exception ex)
        {
            currentListener.LoadWarning(String.format("Unable to get byte element %s of %s.", strTagName, element.getNodeName()));
            
            if(strTagName.equals(XMLDefs.FLAGS) && (element.getNodeName().equals(XMLDefs.SAM_SITE) || element.getNodeName().equals(XMLDefs.MISSILE_SITE)))
            {
                currentListener.LoadWarning(String.format("Loading default state of %s for %s.", strTagName, element.getNodeName()));
                return (byte)0x80;
            }
            
            return 0;
        }
    }
    
    private static int GetIntElement(Element element, String strTagName)
    {
        strLastHandled = "Int element " + strTagName;
        
        try
        {
            return Integer.parseInt(element.getElementsByTagName(strTagName).item(0).getTextContent());
        }
        catch(Exception ex)
        {
            if(strTagName.equals(XMLDefs.ASSET_ID))
            {
                currentListener.LoadWarning(String.format("Unable to get asset ID for %s. Returning default.", element.getNodeName()));
                return LaunchType.ASSET_ID_DEFAULT;
            }
            
            currentListener.LoadWarning(String.format("Unable to get int element %s of %s. Returning zero.", strTagName, element.getNodeName()));
            return 0;
        }
    }
    
    private static long GetLongElement(Element element, String strTagName)
    {
        strLastHandled = "Long element " + strTagName;
        
        try
        {
            return Long.parseLong(element.getElementsByTagName(strTagName).item(0).getTextContent());
        }
        catch(Exception ex)
        {
            if(strTagName.equals(XMLDefs.NEXT_BAN_TIME))
            {
                currentListener.LoadWarning("Initialising as-yet uninitialised ban time.");
                return User.BAN_DURATION_INITIAL;
            }
            
            currentListener.LoadWarning(String.format("Unable to get long element %s of %s. Returning 0.", strTagName, element.getNodeName()));
            return 0;
        }
    }
    
    private static short GetShortElement(Element element, String strTagName)
    {
        strLastHandled = "Short element " + strTagName;
        
        try
        {
            return Short.parseShort(element.getElementsByTagName(strTagName).item(0).getTextContent());
        }
        catch(Exception ex)
        {
            currentListener.LoadWarning(String.format("Unable to get short element %s of %s. Returning 0.", strTagName, element.getNodeName()));
            return (short)0;
        }
    }
    
    private static String GetStringElement(Element element, String strTagName)
    {
        strLastHandled = "String element " + strTagName;
        
        try
        {
            return element.getElementsByTagName(strTagName).item(0).getTextContent();
        }
        catch(Exception ex)
        {
            currentListener.LoadWarning(String.format("Unable to get string element %s of %s. Returning empty string.", strTagName, element.getNodeName()));
            return "";
        }
    }
    
    private static boolean GetBooleanElement(Element element, String strTagName)
    {
        strLastHandled = "Boolean element " + strTagName;
        
        try
        {
            return Boolean.parseBoolean(element.getElementsByTagName(strTagName).item(0).getTextContent());
        }
        catch(Exception ex)
        {
            currentListener.LoadWarning(String.format("Unable to get boolean element %s of %s.", strTagName, element.getNodeName()));
            
            return false;
        }
    }
    
    private static int GetIntAttribute(Element element, String strAttribute)
    {
        strLastHandled = "Int attribute " + strAttribute;
        return Integer.parseInt(element.getAttribute(strAttribute));
    }
    
    private static byte GetByteAttribute(Element element, String strAttribute)
    {
        strLastHandled = "Byte attribute " + strAttribute;
        return Byte.parseByte(element.getAttribute(strAttribute));
    }
    
    private static long GetLongAttribute(Element element, String strAttribute)
    {
        strLastHandled = "Long attribute " + strAttribute;
        return Long.parseLong(element.getAttribute(strAttribute));
    }
    
    private static float GetFloatAttribute(Element element, String strAttribute)
    {
        strLastHandled = "Float attribute " + strAttribute;
        return Float.parseFloat(element.getAttribute(strAttribute));
    }
    
    private static short GetShortAttribute(Element element, String strAttribute)
    {
        strLastHandled = "Short attribute " + strAttribute;
        return Short.parseShort(element.getAttribute(strAttribute));
    }
    
    private static float GetFloatElement(Element element, String strTagName)
    {
        strLastHandled = "Float element " + strTagName;
        return Float.parseFloat(element.getElementsByTagName(strTagName).item(0).getTextContent());
    }
    
    private static GeoCoord GetPositionElement(Element element, String strTagName)
    {
        strLastHandled = "Position element " + strTagName;
        Element elePosition = GetElement(element, strTagName);
        float fltLatitude = GetFloatElement(elePosition, XMLDefs.LATITUDE);
        float fltLongitude = GetFloatElement(elePosition, XMLDefs.LONGITUDE);
        
        return new GeoCoord(fltLatitude, fltLongitude);
    }
}
