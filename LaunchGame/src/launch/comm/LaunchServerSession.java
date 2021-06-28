/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.comm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;
import tobcomm.TobComm;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import launch.game.Alliance;
import launch.game.Defs;
import launch.game.LaunchGame;
import launch.game.LaunchServerGameInterface;
import launch.game.User;
import launch.game.treaties.Treaty;
import launch.game.entities.*;
import launch.utilities.LaunchBannedApp;
import launch.utilities.LaunchClientLocation;
import launch.utilities.LaunchEvent;
import launch.utilities.LaunchLog;
import static launch.utilities.LaunchLog.LogType.CHEATING;
import static launch.utilities.LaunchLog.LogType.NOTIFICATIONS;
import static launch.utilities.LaunchLog.LogType.LOCATIONS;
import launch.utilities.LaunchReport;
import launch.utilities.LaunchUtilities;
import launch.utilities.LocationSpoofCheck;
import launch.utilities.Security;
import static launch.utilities.LaunchLog.LogType.DEVICE_CHECKS;

/**
 *
 * @author tobster
 */
public class LaunchServerSession extends LaunchSession
{
    private static final int SERVER_TIMEOUT = 60000;
    
    private static Object AvatarMutex = new Object(); //To synchronise I/O on new avatars, preventing multiple use of the same filename with concurrent avatar uploads.
    private static int lNewAvatarID = Defs.THE_GREAT_BIG_NOTHING;
    
    private int lID;
    
    private LaunchServerGameInterface gameInterface;
    
    private boolean bRegistered = false;
    private User AuthenticatedUser = null;
    private boolean bSendingReport = false;
    private boolean bCanReceiveUpdates = false;
    
    private String strIPAddress;
    
    private int lSnapshotReports = 0;
    
    public static final Random random = new Random();
    
    public LaunchServerSession(int lID, Socket socket, LaunchServerGameInterface gameInterface)
    {
        super(socket);
        this.lID = lID;
        strIPAddress = connection.GetAddress();
        this.gameInterface = gameInterface;
        Start();
    }
    
    @Override
    protected void Process()
    {
        if(bRegistered && bCanReceiveUpdates)
        {
            //Send events when we can and when applicable.
            if(!bSendingReport)
            {
                if(AuthenticatedUser.HasReports())
                {
                    bSendingReport = true;
                    LaunchReport report = AuthenticatedUser.GetNextReport();
                    tobComm.SendObject(Report, 0, 0, report.GetData());
                }
            }
        }
    }

    @Override
    public void ObjectReceived(int lObject, int lInstanceNumber, int lOffset, byte[] cData)
    {
        ByteBuffer bb = ByteBuffer.wrap(cData);
        
        switch(lObject)
        {
            case Authorise:
            {
                byte[] cDeviceID = new byte[Security.SHA256_SIZE];
                bb.get(cDeviceID);
                short nMajorVersion = bb.getShort();
                String strIdentity = Security.BytesToHexString(cDeviceID);
                String strDeviceName = LaunchUtilities.StringFromData(bb);
                String strDataDirectory = LaunchUtilities.StringFromData(bb);
                byte cFlags = bb.get();
                
                if(nMajorVersion != Defs.MAJOR_VERSION)
                {
                    //Wrong version. Send major update warning.
                    tobComm.SendCommand(MajorVersionInvalid);
                }
                else
                {
                    User user = gameInterface.VerifyID(strIdentity);

                    if(user != null)
                    {
                        user.SetLastNetwork(strIPAddress, Defs.IsMobile(cFlags));
                        
                        switch(user.GetBanState())
                        {
                            case NOT:
                            {
                                Player player = gameInterface.GetGame().GetPlayer(user.GetPlayerID());
                        
                                //Identity validated. Return it.
                                strLogName = LaunchLog.GetTimeFormattedLogName(player.GetName());
                                LaunchLog.Log(CHEATING, player.GetName(), String.format("Device Name:%s\nData Dir:%s\nIP:%s", strDeviceName, strDataDirectory, strIPAddress));
                                
                                if(strDataDirectory.length() > Defs.EXPECTED_DATA_DIR_LENGTH || !strDataDirectory.contains(Defs.DATA_DIR_SHOULD_MATCH))
                                    gameInterface.AdminReport(new LaunchReport(String.format("Player %s has funky data directory %s. Probable emulator.", player.GetName(), strDataDirectory), true, player.GetID()));
                                
                                try
                                {
                                    String strDeviceHash = Security.BytesToHexString(Security.GetSHA256(strDeviceName.getBytes())).substring(0, User.INFO_HASH_LENGTH);
                                    
                                    if(user.GetDeviceShortHash().isEmpty())
                                        user.SetDeviceShortHash(strDeviceHash);
                                    else
                                    {
                                        if(!user.GetDeviceShortHash().equals(strDeviceHash))
                                        {
                                            gameInterface.AdminReport(new LaunchReport(String.format("Player %s device hash changed, which is strange. Device is now a %s.", player.GetName(), strDeviceName), true, player.GetID()));
                                            user.SetDeviceShortHash(strDeviceHash);
                                        }
                                    }
                                }
                                catch (NoSuchAlgorithmException ex) { /* All devices that can run the launch server should support SHA256 or nothing will work! */ }
                                
                                bRegistered = true;
                                AuthenticatedUser = user;

                                ByteBuffer bbReply = ByteBuffer.allocate(12);
                                bbReply.putShort(Defs.MAJOR_VERSION);
                                bbReply.putShort(Defs.MINOR_VERSION);
                                bbReply.putInt(gameInterface.GetGameConfigChecksum());
                                bbReply.putInt(user.GetPlayerID());

                                tobComm.SendObject(Authorise, 0, 0, bbReply.array());

                                //Request device check if not up to date.
                                if(AuthenticatedUser.DeviceCheckRequired())
                                {
                                    LaunchLog.Log(DEVICE_CHECKS, player.GetName(), "Device checks required. Requesting...");
                                    tobComm.RequestObject(DeviceCheck);
                                }
                            }
                            break;
                            
                            case TIME_BANNED_ACK:
                            case TIME_BANNED:
                            {
                                user.AckBan();
                                
                                //Banned. Send reason and duration.
                                String strBanReason = user.GetBanReason();
                                int lBanReasonLength = LaunchUtilities.GetStringDataSize(strBanReason);
                                byte[] cBanReasonBytes = LaunchUtilities.GetStringData(strBanReason);
                                
                                ByteBuffer bbReply = ByteBuffer.allocate(8 + lBanReasonLength);
                                bbReply.putLong(user.GetBanDurationRemaining());
                                bbReply.put(cBanReasonBytes);
                                
                                tobComm.SendObject(BanData, bbReply.array());
                            }
                            break;
                            
                            case PERMABANNED:
                            {
                                //Permabanned. Send reason.
                                String strBanReason = user.GetBanReason();
                                int lBanReasonLength = LaunchUtilities.GetStringDataSize(strBanReason);
                                byte[] cBanReasonBytes = LaunchUtilities.GetStringData(strBanReason);
                                
                                ByteBuffer bbReply = ByteBuffer.allocate(lBanReasonLength);
                                bbReply.put(cBanReasonBytes);
                                
                                tobComm.SendObject(PermBanData, bbReply.array());
                            }
                            break;
                        }
                    }
                    else
                    {
                        //Doesn't exist.
                        tobComm.SendCommand(AccountUnregistered);
                    }
                }
            }
            break;
            
            case Registration:
            {
                byte[] cDeviceID = new byte[Security.SHA256_SIZE];
                bb.get(cDeviceID);
                String strIdentity = Security.BytesToHexString(cDeviceID);
                String strPlayerName = LaunchUtilities.StringFromData(bb);
                int lAvatarID = bb.getInt();
             
                if(strPlayerName.length() <= Defs.MAX_PLAYER_NAME_LENGTH &&
                   strPlayerName.length() > 0 &&
                   gameInterface.CheckPlayerNameAvailable(strPlayerName) &&
                   (gameInterface.VerifyID(strIdentity) == null)) //Sanity check account doesn't already exist.
                {
                    //Create account.
                    User newUser = gameInterface.CreateAccount(strIdentity, strPlayerName, lAvatarID);
                    tobComm.SendCommand(AccountCreateSuccess);
                    
                    if(gameInterface.GetIpAddressProscribed(strIPAddress))
                    {
                        newUser.Proscribe();
                        gameInterface.NotifyIPProscribed(newUser);
                    }
                }
                else
                {
                    //Player name taken.
                    tobComm.SendCommand(NameTaken);
                }
            }
            break;
            
            case Avatar:
            {
                synchronized(AvatarMutex)
                {
                    File file = new File(String.format(Defs.IMAGE_FILE_FORMAT, Defs.LOCATION_AVATARS, lNewAvatarID));
                    
                    while(file.exists() || lNewAvatarID == Defs.THE_GREAT_BIG_NOTHING)
                    {
                        lNewAvatarID++;
                        file = new File(String.format(Defs.IMAGE_FILE_FORMAT, Defs.LOCATION_AVATARS, lNewAvatarID));
                    }

                    try
                    {
                        FileOutputStream out = new FileOutputStream(file);
                        out.write(cData);
                        out.close();
                        
                        RandomAccessFile rafImage = new RandomAccessFile(String.format(Defs.IMAGE_FILE_FORMAT, Defs.LOCATION_AVATARS, lNewAvatarID), "r");
                        byte[] cResult = new byte[(int)rafImage.length()];
                        rafImage.readFully(cResult);
                        
                        tobComm.SendObject(Avatar, lNewAvatarID, cResult);
                    }
                    catch (IOException ex)
                    {
                        tobComm.SendCommand(ActionFailed, 0);
                    }
                }
            }
            break;
            
            case AlertStatus:
            {
                byte[] cDeviceID = new byte[Security.SHA256_SIZE];
                bb.get(cDeviceID);
                String strIdentity = Security.BytesToHexString(cDeviceID);
                User user = gameInterface.VerifyID(strIdentity);
                Player player = null;
                
                if(user != null)
                    player = gameInterface.GetGame().GetPlayer(user.GetPlayerID());
                
                if(user != null && player != null)
                {
                    if(user.GetUnderAttack())
                    {
                        LaunchLog.Log(NOTIFICATIONS, player.GetName(), "Alert status requested. Under attack!");
                        tobComm.SendCommand(AlertUnderAttack);
                    }
                    else if(user.GetNuclearEscalation())
                    {
                        LaunchLog.Log(NOTIFICATIONS, player.GetName(), "Alert status requested. Nuclear Escalation!");
                        tobComm.SendCommand(AlertNukeEscalation);
                    }
                    else if(user.GetAllyUnderAttack())
                    {
                        LaunchLog.Log(NOTIFICATIONS, player.GetName(), "Alert status requested. Ally under attack!");
                        tobComm.SendCommand(AlertAllyUnderAttack);
                    }
                    else
                    {
                        LaunchLog.Log(NOTIFICATIONS, player.GetName(), "Alert status requested. All clear.");
                        tobComm.SendCommand(AlertAllClear);
                    }
                }
                else
                {
                    LaunchLog.Log(NOTIFICATIONS, strLogName, "Alert status requested from unknown user and/or player.");
                    tobComm.SendCommand(ActionFailed);
                }
            }
            break;
            
            default:
            {
                if(bRegistered)
                {
                    switch(lObject)
                    {
                        case LocationUpdate:
                        {
                            LaunchClientLocation location = new LaunchClientLocation(bb);
                            Player player = gameInterface.GetGame().GetPlayer(AuthenticatedUser.GetPlayerID());
                            
                            //Check for proscribed locations if this is the player's first location.
                            if(!player.GetPosition().GetValid())
                            {
                                if(gameInterface.GetLocationProscribed(location.GetGeoCoord()))
                                {
                                    AuthenticatedUser.Proscribe();
                                    gameInterface.NotifyLocationProscribed(AuthenticatedUser);
                                }
                            }
                            
                            gameInterface.UpdatePlayerLocation(AuthenticatedUser.GetPlayerID(), location);
                            
                            //Reply with KeepAlive, to provide the client with a latency measurement.
                            tobComm.SendCommand(KeepAlive);
                            
                            //Check for location spoofing, and send a warning if it's hit.
                            LocationSpoofCheck spoofCheck = new LocationSpoofCheck(AuthenticatedUser.GetPreviousLocation(), location);

                            if(spoofCheck.GetType() == LocationSpoofCheck.Type.SPOOF)
                            {
                                //Register possible spoof, but do nothing yet (it gets confirmed with process names).
                                tobComm.RequestObject(ProcessNames);
                                
                                LaunchLog.Log(CHEATING, player.GetName(), String.format("Possible GPS spoof. Distance %skm, Speed %skph.", spoofCheck.GetDistance(), spoofCheck.GetSpeed()));
                                
                                gameInterface.SpoofWarnings(AuthenticatedUser.GetPlayerID(), spoofCheck);
                            }
                            else if(random.nextFloat() < Defs.PROCESS_CHECK_RANDOM_CHANCE) //Randomly request at low frequency as well as first time.
                            {
                                //Demand list of running apps for the first location for spoof-on-entry detection and spot checks.
                                tobComm.RequestObject(ProcessNames);
                            }
                            
                            //Check for multiaccounting if applicable.
                            if(AuthenticatedUser.DoMultiAccountDetection())
                            {
                                gameInterface.MultiAccountingCheck(AuthenticatedUser.GetPlayerID());
                            }
                            
                            LaunchLog.Log(LOCATIONS, player.GetName(), String.format("(%.6f, %.6f) @ %.1fm %s - %s %.1fm %.1fkph",
                                    location.GetLatitude(),
                                    location.GetLongitude(),
                                    location.GetAccuracy(),
                                    location.GetLocationTypeName(),
                                    spoofCheck.GetType().name(),
                                    spoofCheck.GetDistance(),
                                    spoofCheck.GetSpeed()));
                            
                            AuthenticatedUser.SetPreviousLocation(location);
                            
                            //Clear alarm statuses.
                            AuthenticatedUser.ClearAlarms();
                        }
                        break;
                        
                        case ProcessNames:
                        {
                            String strProcessNames = LaunchUtilities.StringFromData(bb);
                            
                            LaunchLog.Log(CHEATING, strLogName, String.format("Got the following list of processes:\n%s.", strProcessNames));
                            String strPlayerName = gameInterface.GetPlayerName(AuthenticatedUser.GetPlayerID());
                            
                            //Store a hash of it.
                            try
                            {
                                String strAppListHash = Security.BytesToHexString(Security.GetSHA256(strProcessNames.getBytes())).substring(0, User.INFO_HASH_LENGTH);
                                AuthenticatedUser.SetAppListShortHash(strAppListHash);
                            }
                            catch (NoSuchAlgorithmException ex) { /* All devices that can run the launch server should support SHA256 or nothing will work! */ }
                            
                            int lLines = strProcessNames.split("\n").length;
                            if(lLines <= Defs.SUSPICIOUSLY_LOW_PROCESS_NUMBER)
                            {
                                Player player = gameInterface.GetGame().GetPlayer(AuthenticatedUser.GetPlayerID());
                                gameInterface.AdminReport(new LaunchReport(String.format("Player %s has a suspiciously low number of processes (%d).", strPlayerName, lLines), true, player.GetID()));
                            }
                            
                            for(LaunchBannedApp bannedApp : gameInterface.GetGame().GetConfig().GetMajorCheatingApps())
                            {
                                if(bannedApp.Matches(strProcessNames))
                                {
                                    gameInterface.PermaBan(AuthenticatedUser.GetPlayerID(), String.format("Having %s (%s).", bannedApp.GetName(), bannedApp.GetDescription()), "[SERVER]");
                                    LaunchLog.Log(LaunchLog.LogType.CHEATING, strPlayerName, String.format("User has majorly banned app %s", bannedApp.GetName()));
                                    return;
                                }
                            }
                            
                            List<LaunchBannedApp> NaughtyApps = new ArrayList();
                            
                            for(LaunchBannedApp bannedApp : gameInterface.GetGame().GetConfig().GetMinorCheatingApps())
                            {
                                if(bannedApp.Matches(strProcessNames))
                                {
                                    LaunchLog.Log(LaunchLog.LogType.CHEATING, strPlayerName, String.format("User has majorly banned app %s", bannedApp.GetName()));
                                    NaughtyApps.add(bannedApp);
                                }
                            }
                            
                            if(NaughtyApps.size() == 1)
                            {
                                gameInterface.TempBan(AuthenticatedUser.GetPlayerID(), String.format("Having %s (%s).", NaughtyApps.get(0).GetName(), NaughtyApps.get(0).GetDescription()), "[SERVER]");
                            }
                            else if(NaughtyApps.size() > 1)
                            {
                                String strReason = "Having the following:";
                                
                                for(LaunchBannedApp app : NaughtyApps)
                                {
                                    strReason += "\n";
                                    strReason += String.format("%s (%s)", app.GetName(), app.GetDescription());
                                }
                                
                                gameInterface.TempBan(AuthenticatedUser.GetPlayerID(), strReason, "[SERVER]");
                            }
                        }
                        break;

                        case BuildMissileSite:
                        {
                            boolean bNuclear = (bb.get() != 0x00);

                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.ConstructMissileSite(AuthenticatedUser.GetPlayerID(), bNuclear),
                                    "Player purchased a missile site.",
                                    "Player couldn't build a missile site.");
                        }
                        break;
                        
                        case SellMissile:
                        {
                            int lSiteID = bb.getInt();
                            byte cSlotNo = bb.get();
                            
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.SellMissile(AuthenticatedUser.GetPlayerID(), lSiteID, cSlotNo),
                                    "Player sold a missile.",
                                    "Player couldn't sell a missile.");
                        }
                        break;
                        
                        case SellInterceptor:
                        {
                            int lSiteID = bb.getInt();
                            byte cSlotNo = bb.get();
                            
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.SellInterceptor(AuthenticatedUser.GetPlayerID(), lSiteID, cSlotNo),
                                    "Player sold an interceptor.",
                                    "Player couldn't sell an interceptor.");
                        }
                        break;

                        case PurchaseMissiles:
                        {
                            int lMissileSiteID = bb.getInt();
                            byte cSlotNo = bb.get();
                            byte[] cTypes = new byte[bb.remaining()];
                            bb.get(cTypes, 0, cTypes.length);

                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.PurchaseMissiles(AuthenticatedUser.GetPlayerID(), lMissileSiteID, cSlotNo, cTypes),
                                    "Player purchased missiles.",
                                    "Player couldn't purchase missiles.");
                        }
                        break;

                        case PurchaseInterceptors:
                        {
                            int lSAMSiteID = bb.getInt();
                            byte cSlotNo = bb.get();
                            byte[] cTypes = new byte[bb.remaining()];
                            bb.get(cTypes, 0, cTypes.length);

                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.PurchaseInterceptors(AuthenticatedUser.GetPlayerID(), lSAMSiteID, cSlotNo, cTypes),
                                    "Player purchased interceptors.",
                                    "Player couldn't purchase interceptors.");
                        }
                        break;
                        
                        case LaunchMissile:
                        {
                            int lSiteID = bb.getInt();
                            byte cSlotNo = bb.get();
                            boolean bTracking = (bb.get() != 0x00);
                            float fltTargetLatitude = bb.getFloat();
                            float fltTargetLongitude = bb.getFloat();
                            int lTargetID = bb.getInt();

                            if(!AuthenticatedUser.AccountRestricted())
                            {
                                HandleSimpleResult(tobComm, lInstanceNumber,
                                        gameInterface.LaunchMissile(AuthenticatedUser.GetPlayerID(), lSiteID, cSlotNo, bTracking, fltTargetLatitude, fltTargetLongitude, lTargetID),
                                        "Missile launched.",
                                        "Could not launch missile.");
                            }
                            else
                            {
                                tobComm.SendCommand(DisplayGeneralError);
                                gameInterface.NotifyAccountRestricted(AuthenticatedUser);
                            }
                        }
                        break;
                        
                        case LaunchPlayerMissile:
                        {
                            byte cSlotNo = bb.get();
                            boolean bTracking = (bb.get() != 0x00);
                            float fltTargetLatitude = bb.getFloat();
                            float fltTargetLongitude = bb.getFloat();
                            int lTargetID = bb.getInt();

                            if(!AuthenticatedUser.AccountRestricted())
                            {
                                HandleSimpleResult(tobComm, lInstanceNumber,
                                        gameInterface.LaunchPlayerMissile(AuthenticatedUser.GetPlayerID(), cSlotNo, bTracking, fltTargetLatitude, fltTargetLongitude, lTargetID),
                                        "Player's missile launched.",
                                        "Could not launch player's missile.");
                            }
                            else
                            {
                                tobComm.SendCommand(DisplayGeneralError);
                                gameInterface.NotifyAccountRestricted(AuthenticatedUser);
                            }
                        }
                        break;
                        
                        case LaunchInterceptor:
                        {
                            int lSiteID = bb.getInt();
                            byte cSlotNo = bb.get();
                            int lTargetID = bb.getInt();

                            if(!AuthenticatedUser.AccountRestricted())
                            {
                                HandleSimpleResult(tobComm, lInstanceNumber,
                                        gameInterface.LaunchInterceptor(AuthenticatedUser.GetPlayerID(), lSiteID, cSlotNo, lTargetID),
                                        "Interceptor launched.",
                                        "Could not launch interceptor.");
                            }
                            else
                            {
                                tobComm.SendCommand(DisplayGeneralError);
                                gameInterface.NotifyAccountRestricted(AuthenticatedUser);
                            }
                        }
                        break;
                        
                        case LaunchPlayerInterceptor:
                        {
                            byte cSlotNo = bb.get();
                            int lTargetID = bb.getInt();

                            if(!AuthenticatedUser.AccountRestricted())
                            {
                                HandleSimpleResult(tobComm, lInstanceNumber,
                                        gameInterface.LaunchPlayerInterceptor(AuthenticatedUser.GetPlayerID(), cSlotNo, lTargetID),
                                        "Player's interceptor launched.",
                                        "Could not launch player's interceptor.");
                            }
                            else
                            {
                                tobComm.SendCommand(DisplayGeneralError);
                                gameInterface.NotifyAccountRestricted(AuthenticatedUser);
                            }
                        }
                        break;
                        
                        case SAMSiteModeChange:
                        {
                            byte cMode = bb.get();
                            
                            List<Integer> SiteIDs = new ArrayList();
                            
                            if(lInstanceNumber != LaunchEntity.ID_NONE)
                            {
                                SiteIDs.add(lInstanceNumber);
                            }
                            else
                            {
                                SiteIDs = LaunchUtilities.IntListFromData(bb);
                            }
                            
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.SetSAMSiteModes(AuthenticatedUser.GetPlayerID(), SiteIDs, cMode),
                                    "SAM site mode changed.",
                                    "Could not change SAM site mode.");
                        }
                        break;
                        
                        case SAMSiteNameChange:
                        {
                            int lSiteID = bb.getInt();
                            String strName = LaunchUtilities.StringFromData(bb);
                            
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.SetSAMSiteName(AuthenticatedUser.GetPlayerID(), lSiteID, strName),
                                    "SAM site name changed.",
                                    "Could not change SAM site name.");
                        }
                        break;
                        
                        case MissileSiteNameChange:
                        {
                            int lSiteID = bb.getInt();
                            String strName = LaunchUtilities.StringFromData(bb);
                            
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.SetMissileSiteName(AuthenticatedUser.GetPlayerID(), lSiteID, strName),
                                    "Missile site name changed.",
                                    "Could not change missile site name.");
                        }
                        break;
                        
                        case SentryGunNameChange:
                        {
                            int lSiteID = bb.getInt();
                            String strName = LaunchUtilities.StringFromData(bb);
                            
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.SetSentryGunName(AuthenticatedUser.GetPlayerID(), lSiteID, strName),
                                    "Sentry gun name changed.",
                                    "Could not change sentry gun name.");
                        }
                        break;
                        
                        case OreMineNameChange:
                        {
                            int lSiteID = bb.getInt();
                            String strName = LaunchUtilities.StringFromData(bb);
                            
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.SetOreMineName(AuthenticatedUser.GetPlayerID(), lSiteID, strName),
                                    "Ore mine name changed.",
                                    "Could not change ore mine name.");
                        }
                        break;
                        
                        case CreateAlliance:
                        {
                            String strName = LaunchUtilities.StringFromData(bb);
                            String strDescription = LaunchUtilities.StringFromData(bb);
                            int lAvatarID = bb.getInt();
                            
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.CreateAlliance(AuthenticatedUser.GetPlayerID(), strName, strDescription, lAvatarID),
                                    "Alliance created.",
                                    "Could not create alliance.");
                        }
                        break;
                        
                        case RenamePlayer:
                        {
                            String strName = LaunchUtilities.StringFromData(bb);
                            
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.ChangePlayerName(AuthenticatedUser.GetPlayerID(), strName),
                                    "Player renamed.",
                                    "Could not rename player.");
                        }
                        break;
                        
                        case RenameAlliance:
                        {
                            String strName = LaunchUtilities.StringFromData(bb);
                            
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.ChangeAllianceName(AuthenticatedUser.GetPlayerID(), strName),
                                    "Alliance renamed.",
                                    "Could not rename alliance.");
                        }
                        break;
                        
                        case RedescribeAlliance:
                        {
                            String strDescription = LaunchUtilities.StringFromData(bb);
                            
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.ChangeAllianceDescription(AuthenticatedUser.GetPlayerID(), strDescription),
                                    "Alliance redescribed.",
                                    "Could not redescribe alliance.");
                        }
                        break;
                        
                        case MissileSitesOnOff:
                        {
                            boolean bOnline = bb.get() != 0x00;
                            
                            List<Integer> SiteIDs = new ArrayList();
                            
                            if(lInstanceNumber != LaunchEntity.ID_NONE)
                            {
                                SiteIDs.add(lInstanceNumber);
                            }
                            else
                            {
                                SiteIDs = LaunchUtilities.IntListFromData(bb);
                            }
                            
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                gameInterface.SetMissileSitesOnOff(AuthenticatedUser.GetPlayerID(), SiteIDs, bOnline),
                                "Missile site set online/offline.",
                                "Could not set missile site online/offline.");
                        }
                        break;
                        
                        case SAMSitesOnOff:
                        {
                            boolean bOnline = bb.get() != 0x00;
                            
                            List<Integer> SiteIDs = new ArrayList();
                            
                            if(lInstanceNumber != LaunchEntity.ID_NONE)
                            {
                                SiteIDs.add(lInstanceNumber);
                            }
                            else
                            {
                                SiteIDs = LaunchUtilities.IntListFromData(bb);
                            }
                            
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.SetSAMSitesOnOff(AuthenticatedUser.GetPlayerID(), SiteIDs, bOnline),
                                    "SAM site set online/offline.",
                                    "Could not set SAM site online/offline.");
                        }
                        break;
                        
                        case SentryGunsOnOff:
                        {
                            boolean bOnline = bb.get() != 0x00;
                            
                            List<Integer> SiteIDs = new ArrayList();
                            
                            if(lInstanceNumber != LaunchEntity.ID_NONE)
                            {
                                SiteIDs.add(lInstanceNumber);
                            }
                            else
                            {
                                SiteIDs = LaunchUtilities.IntListFromData(bb);
                            }
                            
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.SetSentryGunsOnOff(AuthenticatedUser.GetPlayerID(), SiteIDs, bOnline),
                                    "Sentry gun set online/offline.",
                                    "Could not set sentry gun online/offline.");
                        }
                        break;
                        
                        case OreMinesOnOff:
                        {
                            boolean bOnline = bb.get() != 0x00;
                            
                            List<Integer> SiteIDs = new ArrayList();
                            
                            if(lInstanceNumber != LaunchEntity.ID_NONE)
                            {
                                SiteIDs.add(lInstanceNumber);
                            }
                            else
                            {
                                SiteIDs = LaunchUtilities.IntListFromData(bb);
                            }
                            
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.SetOreMinesOnOff(AuthenticatedUser.GetPlayerID(), SiteIDs, bOnline),
                                    "Ore mine set online/offline.",
                                    "Could not set ore mine online/offline.");
                        }
                        break;
                        
                        case DeviceCheck:
                        {
                            Player player = gameInterface.GetGame().GetPlayer(AuthenticatedUser.GetPlayerID());
                            
                            boolean bCompleteFailure = bb.get() != 0x00;
                            boolean bAPIFailure = bb.get() != 0x00;
                            int lFailureCode = bb.getInt();
                            boolean bProfileMatch = bb.get() != 0x00;
                            boolean bBasicIntegrity = bb.get() != 0x00;
                            
                            if(bCompleteFailure)
                            {
                                LaunchLog.Log(DEVICE_CHECKS, player.GetName(), "Device checks failed completely!");
                                gameInterface.NotifyDeviceChecksCompleteFailure(player.GetName());
                            }
                            else if(bAPIFailure)
                            {
                                LaunchLog.Log(DEVICE_CHECKS, player.GetName(), String.format("Device check API failed with code %d", lFailureCode));
                                gameInterface.NotifyDeviceChecksAPIFailure(player.GetName());
                            }
                            else
                            {
                                LaunchLog.Log(DEVICE_CHECKS, player.GetName(), "Device checks complete.");
                                
                                if(!bProfileMatch && !bBasicIntegrity)
                                {
                                    LaunchLog.Log(DEVICE_CHECKS, player.GetName(), "Both device checks checks failed!");
                                    gameInterface.NotifyDeviceCheckFailure(AuthenticatedUser);
                                }
                                
                                LaunchLog.Log(DEVICE_CHECKS, player.GetName(), bProfileMatch? "Device profile okay." : "Device profile check failed.");
                                LaunchLog.Log(DEVICE_CHECKS, player.GetName(), bProfileMatch? "Basic integrity okay." : "Basic integrity check failed.");
                            }
                            
                            AuthenticatedUser.UpdateDeviceChecks(bCompleteFailure, bAPIFailure, lFailureCode, bProfileMatch, bBasicIntegrity);
                        }
                        break;
                        
                        case Ban:
                        {
                            String strReason = LaunchUtilities.StringFromData(bb);
                            boolean bPermanent = bb.get() != 0x00;
                            Player banner = gameInterface.GetGame().GetPlayer(AuthenticatedUser.GetPlayerID());
                            
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    bPermanent ? gameInterface.PermaBan(lInstanceNumber, strReason, banner.GetName()) :
                                                 gameInterface.TempBan(lInstanceNumber, strReason, banner.GetName()),
                                    "Issued a ban.",
                                    "Could not issue a ban.");
                        }
                        break;
                    }
                }
                else
                {
                    tobComm.SendCommand(AccountUnregistered);
                }
            }
        }
    }
    
    private void HandleSimpleResult(TobComm launchComm, int lInstanceNumber, boolean bResult, String strPositiveLog, String strNegativeLog)
    {
        //Handles a simple action success/action fail Session object outcome.
        if(bResult)
        {
            launchComm.SendCommand(ActionSuccess, lInstanceNumber);
        }
        else
        {
            launchComm.SendCommand(ActionFailed, lInstanceNumber);
        }
    }

    @Override
    public void CommandReceived(int lCommand, int lInstanceNumber)
    {
        switch(lCommand)
        {
            case KeepAlive:
            {
                tobComm.SendCommand(KeepAlive, 0);
            }
            break;
            
            case SetAvatar:
            {
                HandleSimpleResult(tobComm, lInstanceNumber,
                        gameInterface.SetAvatar(AuthenticatedUser.GetPlayerID(), lInstanceNumber),
                        "Avatar set.",
                        "Could not set avatar.");
            }
            break;
            
            default:
            {
                if(bRegistered)
                {
                    switch(lCommand)
                    {
                        case ReportAck:
                        {
                            AuthenticatedUser.AcknowledgeReport();
                            bSendingReport = false;
                        }
                        break;

                        case SnapshotAck:
                        {
                            bCanReceiveUpdates = true;
                            
                            AuthenticatedUser.AcknowledgeReports(lSnapshotReports);
                        }
                        break;
                        
                        case PurchaseMissileSystem:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.PurchaseMissileSystem(AuthenticatedUser.GetPlayerID()),
                                    "Missile system purchased.",
                                    "Missile system purchase failed.");
                        }
                        break;

                        case PurchaseSAMSystem:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.PurchaseSAMSystem(AuthenticatedUser.GetPlayerID()),
                                    "SAM system purchased.",
                                    "SAM system purchase failed.");
                        }
                        break;

                        case BuildSamSite:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.ConstructSAMSite(AuthenticatedUser.GetPlayerID()),
                                    "Player purchased a SAM site.",
                                    "Player couldn't purchase a SAM site.");
                        }
                        break;

                        case BuildSentryGun:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.ConstructSentryGun(AuthenticatedUser.GetPlayerID()),
                                    "Player purchased a sentry gun.",
                                    "Player couldn't purchase a sentry gun.");
                        }
                        break;

                        case BuildOreMine:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.ConstructOreMine(AuthenticatedUser.GetPlayerID()),
                                    "Player purchased an ore mine.",
                                    "Player couldn't purchase an ore mine.");
                        }
                        break;
            
                        case Respawn:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.Respawn(AuthenticatedUser.GetPlayerID()),
                                    "Respawn successful.",
                                    "Could not respawn.");
                        }
                        break;
            
                        case PlayerMissileSlotUpgrade:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.PlayerMissileSlotUpgrade(AuthenticatedUser.GetPlayerID()),
                                    "Slots upgraded.",
                                    "Could not upgrade slots.");
                        }
                        break;

                        case PlayerInterceptorSlotUpgrade:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.PlayerInterceptorSlotUpgrade(AuthenticatedUser.GetPlayerID()),
                                    "Slots upgraded.",
                                    "Could not upgrade slots.");
                        }
                        break;
            
                        case PlayerMissileReloadUpgrade:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.PlayerMissileReloadUpgrade(AuthenticatedUser.GetPlayerID()),
                                    "Reload upgraded.",
                                    "Could not upgrade reload.");
                        }
                        break;

                        case PlayerInterceptorReloadUpgrade:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.PlayerInterceptorReloadUpgrade(AuthenticatedUser.GetPlayerID()),
                                    "Reload upgraded.",
                                    "Could not upgrade reload.");
                        }
                        break;

                        case SellMissileSite:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.SellMissileSite(AuthenticatedUser.GetPlayerID(), lInstanceNumber),
                                    "Missile site sold.",
                                    "Could not sell missile site.");
                        }
                        break;
                        
                        case MissileSlotUpgrade:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.MissileSlotUpgrade(AuthenticatedUser.GetPlayerID(), lInstanceNumber),
                                    "Slots upgraded.",
                                    "Could not upgrade slots.");
                        }
                        break;

                        case InterceptorSlotUpgrade:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.InterceptorSlotUpgrade(AuthenticatedUser.GetPlayerID(), lInstanceNumber),
                                    "Slots upgraded.",
                                    "Could not upgrade slots.");
                        }
                        break;
                        
                        case MissileReloadUpgrade:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.MissileReloadUpgrade(AuthenticatedUser.GetPlayerID(), lInstanceNumber),
                                    "Reload upgraded.",
                                    "Could not upgrade reload.");
                        }
                        break;

                        case InterceptorReloadUpgrade:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.InterceptorReloadUpgrade(AuthenticatedUser.GetPlayerID(), lInstanceNumber),
                                    "Reload upgraded.",
                                    "Could not upgrade reload.");
                        }
                        break;

                        case SellSAMSite:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.SellSAMSite(AuthenticatedUser.GetPlayerID(), lInstanceNumber),
                                    "SAM site sold.",
                                    "Could not sell SAM site.");
                        }
                        break;

                        case SellSentryGun:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.SellSentryGun(AuthenticatedUser.GetPlayerID(), lInstanceNumber),
                                    "Sentry gun sold.",
                                    "Could not sell sentry gun.");
                        }
                        break;

                        case SellOreMine:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.SellOreMine(AuthenticatedUser.GetPlayerID(), lInstanceNumber),
                                    "Ore mine sold.",
                                    "Could not sell ore mine.");
                        }
                        break;
                        
                        case SellMissileSystem:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.SellMissileSystem(AuthenticatedUser.GetPlayerID()),
                                    "Missile system sold.",
                                    "Could not sell missile system.");
                        }
                        break;
                        
                        case SellSAMSystem:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.SellSAMSystem(AuthenticatedUser.GetPlayerID()),
                                    "SAM system sold.",
                                    "Could not sell SAM system.");
                        }
                        break;
                        
                        case RepairMissileSite:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.RepairMissileSite(AuthenticatedUser.GetPlayerID(), lInstanceNumber),
                                    "Missile site repaired.",
                                    "Could not repair missile site.");
                        }
                        break;
                        
                        case RepairSAMSite:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.RepairSAMSite(AuthenticatedUser.GetPlayerID(), lInstanceNumber),
                                    "SAM site repaired.",
                                    "Could not repair SAM site.");
                        }
                        break;
                        
                        case RepairSentryGun:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.RepairSentryGun(AuthenticatedUser.GetPlayerID(), lInstanceNumber),
                                    "Sentry gun repaired.",
                                    "Could not repair sentry gun.");
                        }
                        break;
                        
                        case RepairOreMine:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.RepairOreMine(AuthenticatedUser.GetPlayerID(), lInstanceNumber),
                                    "Ore mine repaired.",
                                    "Could not repair ore mine.");
                        }
                        break;
                        
                        case Heal:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.HealPlayer(AuthenticatedUser.GetPlayerID()),
                                    "Player healed.",
                                    "Could not heal player.");
                        }
                        break;
                        
                        case CloseAccount:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.CloseAccount(AuthenticatedUser.GetPlayerID()),
                                    "Account closed.",
                                    "Could not close account.");
                        }
                        break;
                        
                        case UpgradeMissileSiteNuclear:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.UpgradeToNuclear(AuthenticatedUser.GetPlayerID(), lInstanceNumber),
                                    "Missile site upgraded to nuclear.",
                                    "Could not upgrade missile site to nuclear.");
                        }
                        break;
                        
                        case JoinAlliance:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.JoinAlliance(AuthenticatedUser.GetPlayerID(), lInstanceNumber),
                                    "Join request acknowledged",
                                    "Could not acknowledge join request.");
                        }
                        break;
                        
                        case LeaveAlliance:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.LeaveAlliance(AuthenticatedUser.GetPlayerID()),
                                    "Left alliance.",
                                    "Could not leave alliance.");
                        }
                        break;
                        
                        case DeclareWar:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.DeclareWar(AuthenticatedUser.GetPlayerID(), lInstanceNumber),
                                    "Declared war.",
                                    "Could not declare war.");
                        }
                        break;
                        
                        case ProposeAffiliation:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.ProposeAffiliation(AuthenticatedUser.GetPlayerID(), lInstanceNumber),
                                    "Proposed affiliation.",
                                    "Could not propose affiliation.");
                        }
                        break;
                        
                        case AcceptAffiliation:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.AcceptAffiliation(AuthenticatedUser.GetPlayerID(), lInstanceNumber),
                                    "Accepted affiliation.",
                                    "Could not accept affiliation.");
                        }
                        break;
                        
                        case RejectAffiliation:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.RejectAffiliation(AuthenticatedUser.GetPlayerID(), lInstanceNumber),
                                    "Rejected affiliation.",
                                    "Could not reject affiliation.");
                        }
                        break;
                        
                        case Promote:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.Promote(AuthenticatedUser.GetPlayerID(), lInstanceNumber),
                                    "Promoted player.",
                                    "Could not promote player.");
                        }
                        break;
                        
                        case AcceptJoin:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.AcceptJoin(AuthenticatedUser.GetPlayerID(), lInstanceNumber),
                                    "Accepted join.",
                                    "Could not accept join.");
                        }
                        break;
                        
                        case RejectJoin:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.RejectJoin(AuthenticatedUser.GetPlayerID(), lInstanceNumber),
                                    "Rejected join.",
                                    "Could not reject join.");
                        }
                        break;
            
                        case SetAllianceAvatar:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.SetAllianceAvatar(AuthenticatedUser.GetPlayerID(), lInstanceNumber),
                                    "Alliance avatar set.",
                                    "Could not set alliance avatar.");
                        }
                        break;
                        
                        case Kick:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.Kick(AuthenticatedUser.GetPlayerID(), lInstanceNumber),
                                    "Kicked.",
                                    "Could not kick.");
                        }
                        break;
                        
                        case ResetAvatar:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.AvatarReset(AuthenticatedUser.GetPlayerID(), lInstanceNumber),
                                    "ResetAvatar command succeeded.",
                                    "ResetAvatar command did not succeed.");
                        }
                        break;
                        
                        case ResetName:
                        {
                            HandleSimpleResult(tobComm, lInstanceNumber,
                                    gameInterface.NameReset(AuthenticatedUser.GetPlayerID(), lInstanceNumber),
                                    "ResetName command succeeded.",
                                    "ResetName command did not succeed.");
                        }
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void ObjectRequested(int lObject, int lInstanceNumber, int lOffset, int lLength)
    {
        switch(lObject)
        {
            case GameSnapshot:
            {
                //If the session is authenticated, send a complete game snapshot.
                if(bRegistered)
                {
                    LaunchGame game = gameInterface.GetGame();
                    
                    //Send snapshot begin command.
                    tobComm.SendCommand(SnapshotBegin, 0);
                    
                    //Start burst mode, so all of this goes in a minimal number of TCP packets.
                    StartBurst();
                    
                    //Send the player first, to fix some null pointer crashes.
                    int lTheirPlayerID = AuthenticatedUser.GetPlayerID();
                    Player player = game.GetPlayer(lTheirPlayerID);
                    tobComm.SendObject(Player, lTheirPlayerID, 0, player.GetData(lTheirPlayerID));
                    
                    //Send all alliances.
                    for(Alliance alliance : game.GetAlliances())
                    {
                        tobComm.SendObject(AllianceMinor, alliance.GetData());
                    }
                    
                    //Send all wars.
                    for(Treaty treaty : game.GetTreaties())
                    {
                        tobComm.SendObject(Treaty, treaty.GetData());
                    }
                    
                    //Send all players.
                    for(Player entity : game.GetPlayers())
                    {
                        tobComm.SendObject(Player, entity.GetID(), 0, entity.GetData(lTheirPlayerID));
                    }
                    
                    //Send all missiles.
                    for(Missile entity : game.GetMissiles())
                    {
                        tobComm.SendObject(Missile, entity.GetID(), 0, entity.GetData(lTheirPlayerID));
                    }
                    
                    //Send all interceptors.
                    for(Interceptor entity : game.GetInterceptors())
                    {
                        tobComm.SendObject(Interceptor, entity.GetID(), 0, entity.GetData(lTheirPlayerID));
                    }
                    
                    //Send all missile sites.
                    for(MissileSite entity : game.GetMissileSites())
                    {
                        tobComm.SendObject(MissileSite, entity.GetID(), 0, entity.GetData(lTheirPlayerID));
                    }
                    
                    //Send all SAM sites.
                    for(SAMSite entity : game.GetSAMSites())
                    {
                        tobComm.SendObject(SamSite, entity.GetID(), 0, entity.GetData(lTheirPlayerID));
                    }
                    
                    //Send all sentry guns.
                    for(SentryGun entity : game.GetSentryGuns())
                    {
                        tobComm.SendObject(SentryGun, entity.GetID(), 0, entity.GetData(lTheirPlayerID));
                    }
                    
                    //Send all ore mines.
                    for(OreMine entity : game.GetOreMines())
                    {
                        tobComm.SendObject(OreMine, entity.GetID(), 0, entity.GetData(lTheirPlayerID));
                    }
                    
                    //Send all loots.
                    for(Loot entity : game.GetLoots())
                    {
                        tobComm.SendObject(Loot, entity.GetID(), 0, entity.GetData(lTheirPlayerID));
                    }
                    
                    //Send all radiations.
                    for(Radiation entity : game.GetRadiations())
                    {
                        tobComm.SendObject(Radiation, entity.GetID(), 0, entity.GetData(lTheirPlayerID));
                    }
                    
                    for(LaunchReport report : AuthenticatedUser.GetReports())
                    {
                        tobComm.SendObject(Report, 0, 0, report.GetData());
                        lSnapshotReports++;
                    }
                    
                    tobComm.SendCommand(SnapshotComplete, 0);
                    
                    //End burst mode, to go back to one TCP packet per thing.
                    EndBurst();
                }
                else
                {
                    tobComm.SendCommand(AccountUnregistered);
                }
            }
            break;
            
            case Avatar:
            {
                //Avatar requested.
                RandomAccessFile rafImage;
                
                try
                {
                    rafImage = new RandomAccessFile(String.format(Defs.IMAGE_FILE_FORMAT, Defs.LOCATION_AVATARS, lInstanceNumber), "r");
                    byte[] cResult = new byte[(int)rafImage.length()];
                    rafImage.readFully(cResult);

                    tobComm.SendObject(Avatar, lInstanceNumber, cResult);
                }
                catch (Exception ex)
                {
                    //The avatar is bad, flag this to the game so that all players using it can be reset.
                    gameInterface.BadAvatar(lInstanceNumber);
                    tobComm.SendCommand(ImageError, lInstanceNumber);
                }
            }
            break;
            
            case Config:
            {
                //Game config requested.
                LaunchGame game = gameInterface.GetGame();
                tobComm.SendObject(LaunchSession.Config, 0, lOffset, game.GetConfig().GetData());
            }
            break;
            
            case ImgAsset:
            {
                //Image requested.
                RandomAccessFile rafImage;
                
                try
                {
                    rafImage = new RandomAccessFile(String.format(Defs.IMAGE_FILE_FORMAT, Defs.LOCATION_IMGASSETS, lInstanceNumber), "r");
                    byte[] cResult = new byte[(int)rafImage.length()];
                    rafImage.readFully(cResult);

                    tobComm.SendObject(ImgAsset, lInstanceNumber, cResult);
                }
                catch (Exception ex)
                {
                    //The avatar is bad, flag this to the game so that all players using it can be reset.
                    gameInterface.BadImage(lInstanceNumber);
                    tobComm.SendCommand(ImageError, lInstanceNumber);
                }
            }
            break; 
            
            case Treaty:
            {
                Treaty treaty = gameInterface.GetGame().GetTreaty(lInstanceNumber);
                
                if(treaty != null)
                {
                    tobComm.SendObject(Treaty, treaty.GetData());
                }
            }
            break;
            
            case FullPlayerStats:
            {
                Player fullPlayer = gameInterface.GetGame().GetPlayer(lInstanceNumber);
                
                if(fullPlayer != null)
                {
                    tobComm.SendObject(FullPlayerStats, fullPlayer.GetFullStatsData(AuthenticatedUser.GetPlayerID()));
                }
            }
            break;
            
            case UserData:
            {
                boolean bSuccess = false;
                
                if(AuthenticatedUser != null)
                {
                    Player player = gameInterface.GetGame().GetPlayer(AuthenticatedUser.GetPlayerID());
                    
                    if(player != null)
                    {
                        if(player.GetIsAnAdmin())
                        {
                            User user = gameInterface.GetUser(lInstanceNumber);
                            
                            tobComm.SendObject(lObject, lInstanceNumber, user.GetData());
                        }
                    }
                }
                
                if(!bSuccess)
                {
                    tobComm.SendCommand(ActionFailed);
                }
            }
            break;
        }
    }
    
    public void SendEntity(LaunchEntity entity)
    {
        if(bRegistered)
        {
            if(entity instanceof Player)
            {
                Player player = (Player)entity;
                tobComm.SendObject(Player, entity.GetID(), 0, entity.GetData(AuthenticatedUser.GetPlayerID()));
            }
            else if(entity instanceof Missile)
            {
                Missile missile = (Missile)entity;
                tobComm.SendObject(Missile, entity.GetID(), 0, entity.GetData(AuthenticatedUser.GetPlayerID()));
            }
            else if(entity instanceof Interceptor)
            {
                Interceptor interceptor = (Interceptor)entity;
                tobComm.SendObject(Interceptor, entity.GetID(), 0, entity.GetData(AuthenticatedUser.GetPlayerID()));
            }
            else if(entity instanceof MissileSite)
            {
                Structure structure = (Structure) entity;
                tobComm.SendObject(MissileSite, entity.GetID(), 0, entity.GetData(AuthenticatedUser.GetPlayerID()));
            }
            else if(entity instanceof SAMSite)
            {
                Structure structure = (Structure) entity;
                tobComm.SendObject(SamSite, entity.GetID(), 0, entity.GetData(AuthenticatedUser.GetPlayerID()));
            }
            else if(entity instanceof OreMine)
            {
                Structure structure = (Structure) entity;
                tobComm.SendObject(OreMine, entity.GetID(), 0, entity.GetData(AuthenticatedUser.GetPlayerID()));
            }
            else if(entity instanceof SentryGun)
            {
                Structure structure = (Structure) entity;
                tobComm.SendObject(SentryGun, entity.GetID(), 0, entity.GetData(AuthenticatedUser.GetPlayerID()));
            }
            else if(entity instanceof Loot)
                tobComm.SendObject(Loot, entity.GetID(), 0, entity.GetData(AuthenticatedUser.GetPlayerID()));
            else if(entity instanceof Radiation)
                tobComm.SendObject(Radiation, entity.GetID(), 0, entity.GetData(AuthenticatedUser.GetPlayerID()));
        }
    }
    
    public void RemoveEntity(LaunchEntity entity)
    {
        if(bRegistered)
        {
            if(entity instanceof Player)
                tobComm.SendCommand(RemovePlayer, entity.GetID());
            else if(entity instanceof Missile)
                tobComm.SendCommand(RemoveMissile, entity.GetID());
            else if(entity instanceof Interceptor)
                tobComm.SendCommand(RemoveInterceptor, entity.GetID());
            else if(entity instanceof MissileSite)
                tobComm.SendCommand(RemoveMissileSite, entity.GetID());
            else if(entity instanceof SAMSite)
                tobComm.SendCommand(RemoveSAMSite, entity.GetID());
            else if(entity instanceof OreMine)
                tobComm.SendCommand(RemoveOreMine, entity.GetID());
            else if(entity instanceof SentryGun)
                tobComm.SendCommand(RemoveSentryGun, entity.GetID());
            else if(entity instanceof Loot)
                tobComm.SendCommand(RemoveLoot, entity.GetID());
            else if(entity instanceof Loot)
                tobComm.SendCommand(RemoveRadiation, entity.GetID());
        }
    }
    
    public void SendAlliance(Alliance alliance, boolean bMajor)
    {
        tobComm.SendObject(bMajor ? AllianceMajor : AllianceMinor, alliance.GetID(), alliance.GetData());
    }
    
    public void SendTreaty(Treaty treaty)
    {
        tobComm.SendObject(Treaty, treaty.GetID(), treaty.GetData());
    }
    
    public void RemoveAlliance(Alliance alliance)
    {
        tobComm.SendCommand(RemoveAlliance, alliance.GetID());
    }
    
    public void RemoveTreaty(Treaty treaty)
    {
        tobComm.SendCommand(RemoveTreaty, treaty.GetID());
    }
    
    public void SendEvent(LaunchEvent event)
    {
        tobComm.SendObject(Event, event.GetData());
    }
    
    public User GetAuthenticatedUser()
    {
        return AuthenticatedUser;
    }
    
    public boolean CanReceiveUpdates()
    {
        return bCanReceiveUpdates;
    }
    
    public int GetID() { return lID; }

    @Override
    protected int GetTimeout()
    {
        return SERVER_TIMEOUT;
    }

    @Override
    public void SyncObjectsProcessed()
    {
        throw new UnsupportedOperationException("The server shouldn't use this feature.");
    }
}
