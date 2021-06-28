/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.game;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.LinkedHashMap;
import static launch.game.User.BanState.*;
import launch.utilities.LaunchClientLocation;
import launch.utilities.LaunchReport;
import launch.utilities.LaunchUtilities;
import launch.utilities.LongDelay;

/**
 *
 * @author tobster
 */
public class User
{
    public enum BanState
    {
        NOT,
        TIME_BANNED_ACK,
        TIME_BANNED,
        PERMABANNED
    }
    
    public static final int INFO_HASH_LENGTH = 8;
    
    public static final long BAN_DURATION_INITIAL = 3600000;
    public static final long BAN_MULTIPLIER = 4;
    public static final long NEXT_BAN_HOURLY_REDUCTION = 10000;
    
    private String strIMEI;                 //The IMEI is now the unique identifier of users.
    private int lPlayerID;                  //The player ID associated with this user.
    private BanState banState;              //The player's ban state.
    private long oNextBanTime;              //The amount of ban time the player will suffer at the next infringement.
    private LongDelay dlyBanDuration;       //The amount of ban time remaining.
    private String strBanReason;            //The reason for their ban.
    private String strLastIP;               //The last IP address the account was logged into with.
    private boolean bLastTypeMobile;        //The last connection type was via a mobile network.
    private long oDeviceCheckedDate;        //When the user's device was checked (0 if never).
    private boolean bLastDeviceCheckFailed; //The last device check attempt failed.
    private boolean bDeviceChecksAPIFailed; //The last device check request got through, but the API response failed.
    private boolean bProscribed;            //The player started with a proscribed IP address or close to a proscribed location.
    private int lDeviceChecksFailCode;      //The status code of the last device check API failure.
    private boolean bProfileMatch;          //The user's device (if checked) passes the Android compatibility tests.
    private boolean bBasicIntegrity;        //The user's device (if checked) passes the basic integrity tests.
    private boolean bApproved;              //An admin has explicitly approved the user for launching weapons.
    private long oExpired;                  //Indicates that the associated player has gone if non-zero, and when since (for clearing up old accounts, but keeping them long enough for bans to be effective).
    private String strDeviceShortHash;      //Short hash identifier for the device, to assist admins in anti-multiaccounting enforcement.
    private String strAppListShortHash;     //Short hash identifier for the app list, to assist admins in anti-multiaccounting enforcement.
    
    private final LinkedHashMap<String, LaunchReport> Reports = new LinkedHashMap<>();                //The reports pending delivery to the user.
    
    //Under attack alarms.
    private boolean bAttackAlarm = false;
    private boolean bNuclearEscalationAlarm = false;
    private boolean bAllyAttackAlarm = false;
    
    //Location spoofing detection.
    private LaunchClientLocation locationPrevious;
    
    //Multiaccounting detection. Always arm on boot.
    private boolean bDoMultiAccountDetection = true;
    
    public User(String strIMEI, int lPlayerID)
    {
        this.strIMEI = strIMEI;
        this.lPlayerID = lPlayerID;
        banState = NOT;
        oNextBanTime = BAN_DURATION_INITIAL;
        dlyBanDuration = new LongDelay();
        strBanReason = "";
        strLastIP = "";
        oDeviceCheckedDate = 0;
        bLastDeviceCheckFailed = false;
        bDeviceChecksAPIFailed = false;
        bProscribed = false;
        lDeviceChecksFailCode = 0;
        bProfileMatch = false;
        bBasicIntegrity = false;
        bApproved = false;
        oExpired = 0;
        strDeviceShortHash = "";
        strAppListShortHash = "";
    }
    
    public User(String strIMEI,
                int lPlayerID,
                byte cBanState,
                long oNextBanTime,
                long oBanDurationRemaining,
                String strBanReason,
                String strLastIP,
                boolean bLastTypeMobile,
                long oDeviceCheckedDate,
                boolean bLastDeviceCheckFailed,
                boolean bDeviceChecksAPIFailed,
                boolean bProscribed,
                int lDeviceChecksFailCode,
                boolean bProfileMatch,
                boolean bBasicIntegrity,
                boolean bApproved,
                long oExpired,
                String strDeviceShortHash,
                String strAppListShortHash)
    {
        this.strIMEI = strIMEI;
        this.lPlayerID = lPlayerID;
        banState = BanState.values()[cBanState];
        this.oNextBanTime = oNextBanTime;
        dlyBanDuration = new LongDelay(oBanDurationRemaining);
        this.strBanReason = strBanReason;
        this.strLastIP = strLastIP;
        this.bLastTypeMobile = bLastTypeMobile;
        this.oDeviceCheckedDate = oDeviceCheckedDate;
        this.bLastDeviceCheckFailed = bLastDeviceCheckFailed;
        this.bDeviceChecksAPIFailed = bDeviceChecksAPIFailed;
        this.bProscribed = bProscribed;
        this.lDeviceChecksFailCode = lDeviceChecksFailCode;
        this.bProfileMatch = bProfileMatch;
        this.bBasicIntegrity = bBasicIntegrity;
        this.bApproved = bApproved;
        this.oExpired = oExpired;
        this.strDeviceShortHash = strDeviceShortHash;
        this.strAppListShortHash = strAppListShortHash;
    }
    
    public User(ByteBuffer bb)
    {
        strIMEI = LaunchUtilities.StringFromData(bb);
        lPlayerID = bb.getInt();
        banState = BanState.values()[bb.get()];
        oNextBanTime = bb.getLong();
        dlyBanDuration = new LongDelay(bb);
        strBanReason = LaunchUtilities.StringFromData(bb);
        strLastIP = LaunchUtilities.StringFromData(bb);
        bLastTypeMobile = bb.get() != 0x00;
        oDeviceCheckedDate = bb.getLong();
        bLastDeviceCheckFailed = bb.get() != 0x00;
        bDeviceChecksAPIFailed = bb.get() != 0x00;
        bProscribed = bb.get() != 0x00;
        lDeviceChecksFailCode = bb.getInt();
        bProfileMatch = bb.get() != 0x00;
        bBasicIntegrity = bb.get() != 0x00;
        bApproved = bb.get() != 0x00;
        oExpired = bb.getLong();
        strDeviceShortHash = LaunchUtilities.StringFromData(bb);
        strAppListShortHash = LaunchUtilities.StringFromData(bb);
        bAttackAlarm = bb.get() != 0x00;
        bNuclearEscalationAlarm = bb.get() != 0x00;
        bAllyAttackAlarm = bb.get() != 0x00;
    }
    
    public void Tick(int lMS)
    {
        if(banState == TIME_BANNED)
        {
            dlyBanDuration.Tick(lMS);
            
            if(dlyBanDuration.Expired())
            {
                banState = NOT;
                strBanReason = "";
            }
        }
    }
    
    public void HourlyTick()
    {
        oNextBanTime -= NEXT_BAN_HOURLY_REDUCTION;
        oNextBanTime = Math.max(oNextBanTime, BAN_DURATION_INITIAL);
    }
    
    public String GetIMEI() { return strIMEI; }
    
    public void SetIMEI(String strIMEI) { this.strIMEI = strIMEI; }
    
    public int GetPlayerID() { return lPlayerID; }
    
    public BanState GetBanState() { return banState; }
    public long GetNextBanTime() { return oNextBanTime; }
    public long GetBanDurationRemaining() { return dlyBanDuration.GetRemaining(); }
    public String GetBanReason() { return strBanReason; }
    
    public String GetLastIP() { return strLastIP; }
    public boolean GetLastTypeMobile() { return bLastTypeMobile; }
    public long GetDeviceCheckedDate() { return oDeviceCheckedDate; }
    public boolean GetLastDeviceCheckFailed() { return bLastDeviceCheckFailed; }
    public boolean GetDeviceChecksAPIFailed() { return bDeviceChecksAPIFailed; }
    public boolean GetProscribed() { return bProscribed; }
    public int GetDeviceChecksFailCode() { return lDeviceChecksFailCode; }
    public boolean GetProfileMatch() { return bProfileMatch; }
    public boolean GetBasicIntegrity() { return bBasicIntegrity; }
    public boolean GetApproved() { return bApproved; }
    
    public void AckBan()
    {
        if(banState == TIME_BANNED_ACK)
            banState = TIME_BANNED;
    }
    
    public void TempBan(String strReason)
    {
        RequireNewChecks();
        strBanReason = strReason;
        banState = TIME_BANNED_ACK;
        dlyBanDuration.Set(oNextBanTime);
        oNextBanTime *= BAN_MULTIPLIER;
    }
    
    public void Permaban(String strReason)
    {
        RequireNewChecks();
        strBanReason = strReason;
        banState = PERMABANNED;
    }
    
    public void Unban()
    {
        strBanReason = "";
        banState = NOT;
    }
    
    public void SetLastNetwork(String strIPAddress, boolean bMobile)
    {
        strLastIP = strIPAddress;
        bLastTypeMobile = bMobile;
    }
    
    public void AddReport(LaunchReport report)
    {
        String strMessage = report.GetMessage();

        if(Reports.containsKey(strMessage))
        {
            Reports.get(strMessage).HappenedAgain();
        }
        else
        {
            Reports.put(strMessage, report);

            //Remove first report if we've breached the limit.
            if(Reports.size() > Defs.MAX_REPORTS)
            {
                Reports.remove(Reports.entrySet().iterator().next().getKey());
            }
        }
    }
    
    public void AcknowledgeReport()
    {
        if(Reports.size() > 0)
        {
            Reports.remove(Reports.entrySet().iterator().next().getKey());
        }
    }
    
    public void AcknowledgeReports(int lCount)
    {
        while(Reports.size() > 0 && lCount > 0)
        {
            Reports.remove(Reports.entrySet().iterator().next().getKey());
            lCount--;
        }
    }
    
    public boolean HasReports() { return Reports.size() > 0; }
    
    public int GetUnreadReports() { return Reports.size(); }
    
    public LaunchReport GetNextReport() 
    {
        return Reports.entrySet().iterator().next().getValue();
    }
    
    public Collection<LaunchReport> GetReports()
    {
        return Reports.values();
    }
    
    public void SetUnderAttack() { bAttackAlarm = true; }
    public void SetNuclearEscalation() { bNuclearEscalationAlarm = true;}
    public void SetAllyUnderAttack() { bAllyAttackAlarm = true;}
    public boolean GetUnderAttack() { return bAttackAlarm; }
    public boolean GetNuclearEscalation() { return bNuclearEscalationAlarm; }
    public boolean GetAllyUnderAttack() { return bAllyAttackAlarm; }
    public void ClearAlarms()
    {
        bAttackAlarm = false;
        bNuclearEscalationAlarm = false;
        bAllyAttackAlarm = false;
    }
    
    public void SetPreviousLocation(LaunchClientLocation location) { locationPrevious = location; }
    public LaunchClientLocation GetPreviousLocation() { return locationPrevious; }
    
    public boolean AccountRestricted()
    {
        //An admin has approved the account, no matter what.
        if(bApproved)
            return false;
        
        //They have been checked and everything looks okay.
        if(!bLastDeviceCheckFailed && !bDeviceChecksAPIFailed && !bProscribed)
        {
            if(bProfileMatch || bBasicIntegrity)
            {
                return false;
            }
        }
        
        return true;
    }
    
    public void ApproveAccount()
    {
        bApproved = true;
    }
    
    public void RequireNewChecks()
    {
        bApproved = false;
        oDeviceCheckedDate = 0;
        bLastDeviceCheckFailed = false;
        bDeviceChecksAPIFailed = false;
        bProscribed = false;
        lDeviceChecksFailCode = 0;
        bProfileMatch = false;
        bBasicIntegrity = false;
    }
    
    public boolean DeviceCheckRequired()
    {
        if(!bApproved)
        {
            return oDeviceCheckedDate == 0 || bLastDeviceCheckFailed || bDeviceChecksAPIFailed;
        }
        
        return false;
    }
    
    public void UpdateDeviceChecks(boolean bCompleteFailure, boolean bAPIFailure, int lFailureCode, boolean bProfileMatch, boolean bBasicIntegrity)
    {
        this.bLastDeviceCheckFailed = bCompleteFailure;
        this.bDeviceChecksAPIFailed = bAPIFailure;
        this.lDeviceChecksFailCode = lFailureCode;
        this.bProfileMatch = bProfileMatch;
        this.bBasicIntegrity = bBasicIntegrity;
        
        oDeviceCheckedDate = System.currentTimeMillis();
    }
    
    public void Proscribe()
    {
        this.bProscribed = true;
    }
    
    public void Expire()
    {
        Reports.clear();
        oExpired = System.currentTimeMillis();
    }
    
    public long GetExpiredOn() { return oExpired; }
    
    public void SetDeviceShortHash(String strHash) { strDeviceShortHash = strHash; }
    public void SetAppListShortHash(String strHash) { strAppListShortHash = strHash; }
    public String GetDeviceShortHash() { return strDeviceShortHash; }
    public String GetAppListShortHash() { return strAppListShortHash; }
    
    public byte[] GetData()
    {
        byte[] cIMEI = LaunchUtilities.GetStringData(strIMEI);
        byte[] cBanReason = LaunchUtilities.GetStringData(strBanReason);
        byte[] cLastIP = LaunchUtilities.GetStringData(strLastIP);
        byte[] cDeviceShortHash = LaunchUtilities.GetStringData(strDeviceShortHash);
        byte[] cAppListShortHash = LaunchUtilities.GetStringData(strAppListShortHash);
        
        ByteBuffer bb = ByteBuffer.allocate(cIMEI.length + cBanReason.length + cLastIP.length + cDeviceShortHash.length + cAppListShortHash.length + 51);
        
        bb.put(cIMEI);
        bb.putInt(lPlayerID);
        bb.put((byte)banState.ordinal());
        bb.putLong(oNextBanTime);
        bb.putLong(dlyBanDuration.GetRemaining());
        bb.put(cBanReason);
        bb.put(cLastIP);
        bb.put((byte)(bLastTypeMobile ? 0xFF : 0x00));
        bb.putLong(oDeviceCheckedDate);
        bb.put((byte)(bLastDeviceCheckFailed ? 0xFF : 0x00));
        bb.put((byte)(bDeviceChecksAPIFailed ? 0xFF : 0x00));
        bb.put((byte)(bProscribed ? 0xFF : 0x00));
        bb.putInt(lDeviceChecksFailCode);
        bb.put((byte)(bProfileMatch ? 0xFF : 0x00));
        bb.put((byte)(bBasicIntegrity ? 0xFF : 0x00));
        bb.put((byte)(bApproved ? 0xFF : 0x00));
        bb.putLong(oExpired);
        bb.put(cDeviceShortHash);
        bb.put(cAppListShortHash);
        bb.put((byte)(bAttackAlarm ? 0xFF : 0x00));
        bb.put((byte)(bNuclearEscalationAlarm ? 0xFF : 0x00));
        bb.put((byte)(bAllyAttackAlarm ? 0xFF : 0x00));
        
        return bb.array();
    }
    
    /**
     * Multi account detection should be performed on this user. It is assumed it will be performed, so the flag will be immediately disarmed.
     * @return Whether multiaccount detection should be performed on this user.
     */
    public boolean DoMultiAccountDetection()
    {
        if(bDoMultiAccountDetection)
        {
            bDoMultiAccountDetection = false;
            
            //As a hack for now, accounts can be approved in exceptional cases to stop multi account detection checks, e.g. if a user proves it's two identical but seperate devices.
            return !bApproved;
        }
        
        return false;
    }
    
    /**
     * Arm multi account detection. Should be performed periodically on all players.
     */
    public void ArmMultiAccountDetection()
    {
        bDoMultiAccountDetection = true;
    }
}
