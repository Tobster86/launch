/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.game.entities;

import launch.utilities.LaunchUtilities;
import launch.game.GeoCoord;
import java.nio.ByteBuffer;
import launch.game.Alliance;
import launch.game.User;
import launch.game.systems.*;
import launch.utilities.LaunchLog;
import launch.utilities.ShortDelay;

/**
 *
 * @author tobster
 */
public final class Player extends Damagable implements LaunchSystemListener
{
    private static final int DATA_SIZE = 30;
    private static final int STATS_DATA_SIZE = 20;
   
    private static final int FLAG1_BANNED = 0x01;        //Banned. NOTE: NOT SAVED (well, it is, but the server ignores it), but determined at run time when getting player data.
    private static final int FLAG1_RES2 = 0x02;          //Unused.
    private static final int FLAG1_RES3 = 0x04;          //Unused.
    private static final int FLAG1_HAS_CMS = 0x08;       //Player possesses a cruise missile system.
    private static final int FLAG1_HAS_SAM = 0x10;       //Player possesses a missile defence system.
    private static final int FLAG1_MP = 0x20;            //The player is a leader in an alliance.
    private static final int FLAG1_AWOL = 0x40;          //The player is AWOL. NOTE: AWOL can only happen to dead players, to stop them being considered for scoring.
    private static final int FLAG1_RSPAWN_PROT = 0x80;   //The player is subject to respawn protection.
    
    private static final int FLAG2_ALLIANCE_REQ_JOIN = 0x01;    //Player is only requesting to join an alliance, and isn't actually in one yet.
    private static final int FLAG2_ADMIN = 0x02;                //Player is an administrator.
    private static final int FLAG2_RES2 = 0x04;                 //Unused.
    private static final int FLAG2_RES3 = 0x08;                 //Unused.
    private static final int FLAG2_RES4 = 0x10;                 //Unused.
    private static final int FLAG2_RES5 = 0x20;                 //Unused.
    private static final int FLAG2_RES6 = 0x40;                 //Unused.
    private static final int FLAG2_RES7 = 0x80;                 //Unused.
    
    //Normal data.
    private String strName;                             //Player's name.
    private int lAvatarID;                              //Avatar ID.
    private int lWealth;                                //How much money they have.
    private long oLastSeen;                             //When they were last seen (UNIX epoch).
    private ShortDelay dlyStateChange;                  //Time before the player may respawn if dead, or respawn protection will cease.
    private int lAllianceID;                            //Alliance ID.
    private ShortDelay dlyAllianceCooloff;              //Time before the player may join another alliance after leaving one.

    //Normal data, condensed into flags.
    private boolean bBanned = false;                    //NOTE: NOT SAVED (well, it is, but the server ignores it), but determined at run time when getting player data.
    private boolean bHasCMS;
    private boolean bHasSAM;
    private boolean bLeader;
    private boolean bAWOL;
    private boolean bRespawnProtected;
    private boolean bRequestingAllianceJoin;
    private boolean bAdmin;
    
    //Stats data.
    private short nKills;
    private short nDeaths;
    private int lOffenceSpending;
    private int lDefenceSpending;
    private int lDamageInflicted;
    private int lDamageReceived;
    private boolean bHasFullStats;
    
    //Systems.
    private MissileSystem missiles = null;
    private MissileSystem interceptors = null;
    
    //Server only.
    private User user = null;
    
    //New player.
    public Player(int lID, short nStartingHP, String strName, int lAvatarID, int lWealth)
    {
        super(lID, new GeoCoord(), (short)0, nStartingHP);
        this.strName = strName;
        this.lAvatarID = lAvatarID;
        this.lWealth = lWealth;
        this.dlyStateChange = new ShortDelay();
        this.lAllianceID = Alliance.ALLIANCE_ID_UNAFFILIATED;
        this.dlyAllianceCooloff = new ShortDelay();
        bHasCMS = false;
        bHasSAM = false;
        bLeader = false;
        bAWOL = false;
        bRespawnProtected = true;
        bRequestingAllianceJoin = false;
        bAdmin = false;
        
        this.nKills = 0;
        this.nDeaths = 0;
        this.lOffenceSpending = 0;
        this.lDefenceSpending = 0;
        this.lDamageInflicted = 0;
        this.lDamageReceived = 0;
        
        SetLastSeen();
    }
    
    //From save.
    public Player(int lID, GeoCoord geoPosition, short nHP, short nMaxHP, String strName, int lAvatarID, int lWealth, long oLastSeen, int lStateChange, int lAllianceID, byte cFlags1, byte cFlags2, int lAllianceCooloff, short nKills, short nDeaths, int lOffenceSpending, int lDefenceSpending, int lDamageInflicted, int lDamageReceived)
    {
        super(lID, geoPosition, nHP, nMaxHP);
        this.strName = strName;
        this.lAvatarID = lAvatarID;
        this.lWealth = lWealth;
        this.oLastSeen = oLastSeen;
        this.dlyStateChange = new ShortDelay(lStateChange);
        this.lAllianceID = lAllianceID;
        this.dlyAllianceCooloff = new ShortDelay(lAllianceCooloff);
        bHasCMS = (cFlags1 & FLAG1_HAS_CMS) != 0x00;
        bHasSAM = (cFlags1 & FLAG1_HAS_SAM) != 0x00;
        bLeader = (cFlags1 & FLAG1_MP) != 0x00;
        bAWOL = (cFlags1 & FLAG1_AWOL) != 0x00;
        bRespawnProtected = (cFlags1 & FLAG1_RSPAWN_PROT) != 0x00;
        bRequestingAllianceJoin = (cFlags2 & FLAG2_ALLIANCE_REQ_JOIN) != 0x00;
        bAdmin = (cFlags2 & FLAG2_ADMIN) != 0x00;

        this.nKills = nKills;
        this.nDeaths = nDeaths;
        this.lOffenceSpending = lOffenceSpending;
        this.lDefenceSpending = lDefenceSpending;
        this.lDamageInflicted = lDamageInflicted;
        this.lDamageReceived = lDamageReceived;
        
        //For old config transfer only.
        if(lAllianceID == Alliance.ALLIANCE_ID_UNAFFILIATED)
        {
            SetIsAnMP(false);
        }
    }
    
    public Player(ByteBuffer bb, int lReceivingID)
    {
        super(bb);
        strName = LaunchUtilities.StringFromData(bb);
        lAvatarID = bb.getInt();
        lWealth = bb.getInt();
        oLastSeen = bb.getLong();
        dlyStateChange = new ShortDelay(bb);
        lAllianceID = bb.getInt();
        byte cFlags1 = bb.get();
        byte cFlags2 = bb.get();
        dlyAllianceCooloff = new ShortDelay(bb);

        bBanned = (cFlags1 & FLAG1_BANNED) != 0x00;
        bHasCMS = (cFlags1 & FLAG1_HAS_CMS) != 0x00;
        bHasSAM = (cFlags1 & FLAG1_HAS_SAM) != 0x00;
        bLeader = (cFlags1 & FLAG1_MP) != 0x00;
        bAWOL = (cFlags1 & FLAG1_AWOL) != 0x00;
        bRespawnProtected = (cFlags1 & FLAG1_RSPAWN_PROT) != 0x00;
        bRequestingAllianceJoin = (cFlags2 & FLAG2_ALLIANCE_REQ_JOIN) != 0x00;
        bAdmin = (cFlags2 & FLAG2_ADMIN) != 0x00;
        
        if(lReceivingID == lID)
        {
            if(GetHasCruiseMissileSystem())
            {
                missiles = new MissileSystem(this, bb);
            }

            if(GetHasAirDefenceSystem())
            {
                interceptors = new MissileSystem(this, bb);
            }
        }
        else
        {
            //Dummy ones if this isn't the client's player.
            if(GetHasCruiseMissileSystem())
            {
                missiles = new MissileSystem();
            }

            if(GetHasAirDefenceSystem())
            {
                interceptors = new MissileSystem();
            }
        }
        
        //If there are bytes remaining, this is a "full" image complete with stats.
        if(bb.hasRemaining())
        {
            nKills = bb.getShort();
            nDeaths = bb.getShort();
            lOffenceSpending = bb.getInt();
            lDefenceSpending = bb.getInt();
            lDamageInflicted = bb.getInt();
            lDamageReceived = bb.getInt();
            bHasFullStats = true;
        }
        else
            bHasFullStats = false;
    }
    
    public void SetUser(User user)
    {
        this.user = user;
    }
    
    public User GetUser() { return user; }
    
    //Brand new.
    public void AddMissileSystem(int lReloadTime, byte cMissileSlotCount)
    {
        missiles = new MissileSystem(this, lReloadTime, cMissileSlotCount);
        SetHasCruiseMissileSystem(true);
    }
    
    //From save (alt).
    public void AddMissileSystem(MissileSystem missileSystem)
    {
        missiles = missileSystem;
        SetHasCruiseMissileSystem(true);
    }
    
    //Brand new.
    public void AddInterceptorSystem(int lReloadTime, byte cMissileSlotCount)
    {
        interceptors = new MissileSystem(this, lReloadTime, cMissileSlotCount);
        SetHasAirDefenceSystem(true);
    }
    
    //From save (alt).
    public void AddInterceptorSystem(MissileSystem interceptorSystem)
    {
        interceptors = interceptorSystem;
        SetHasAirDefenceSystem(true);
    }
    
    public void RemoveMissileSystem(int lSaleValue)
    {
        lWealth += lSaleValue;
        SetHasCruiseMissileSystem(false);
        missiles = null;
    }
    
    public void RemoveAirDefenceSystem(int lSaleValue)
    {
        lWealth += lSaleValue;
        SetHasAirDefenceSystem(false);
        interceptors = null;
    }

    @Override
    public void Tick(int lMS)
    {
        dlyStateChange.Tick(lMS);
        dlyAllianceCooloff.Tick(lMS);
        
        if(GetHasCruiseMissileSystem())
        {
            missiles.Tick(lMS);
        }
        
        if(GetHasAirDefenceSystem())
        {
            interceptors.Tick(lMS);
        }
    }

    @Override
    public byte[] GetData(int lAskingID)
    {
        byte[] cBaseData = super.GetData(lAskingID);
        byte[] cMissileSystemData = (lAskingID == lID && GetHasCruiseMissileSystem()) ? missiles.GetData() : new byte[0];
        byte[] cInterceptorSystemData = (lAskingID == lID && GetHasAirDefenceSystem()) ? interceptors.GetData() : new byte[0];
        
        ByteBuffer bb = ByteBuffer.allocate(cBaseData.length + DATA_SIZE + LaunchUtilities.GetStringDataSize(strName) + cMissileSystemData.length + cInterceptorSystemData.length);
        
        bb.put(cBaseData);
        bb.put(LaunchUtilities.GetStringData(strName));
        bb.putInt(lAvatarID);
        bb.putInt(lWealth);
        bb.putLong(oLastSeen);
        dlyStateChange.GetData(bb);
        bb.putInt(lAllianceID);
        bb.put(GetFlags1());
        bb.put(GetFlags2());
        dlyAllianceCooloff.GetData(bb);
        bb.put(cMissileSystemData);
        bb.put(cInterceptorSystemData);
        
        return bb.array();
    }
    
    public byte[] GetFullStatsData(int lAskingID)
    {
        byte[] cStandardData = GetData(lAskingID);
        
        ByteBuffer bb = ByteBuffer.allocate(cStandardData.length + STATS_DATA_SIZE);
        bb.put(cStandardData);
        bb.putShort(nKills);
        bb.putShort(nDeaths);
        bb.putInt(lOffenceSpending);
        bb.putInt(lDefenceSpending);
        bb.putInt(lDamageInflicted);
        bb.putInt(lDamageReceived);
        
        return bb.array();
    }
    
    public String GetName() { return strName; }
    
    public int GetAvatarID() { return lAvatarID; }
    
    public void SetAvatarID(int lAvatarID)
    {
        this.lAvatarID = lAvatarID;
        Changed(false);
    }
    
    public int GetWealth() { return lWealth; }
    
    public long GetLastSeen() { return oLastSeen; }
    
    /**
     * Get the player's RAW alliance ID.
     * WARNING: For saving the game only. This doesn't imply whether they're a full or speculative member!
     * If you need the alliance the player is a full member of, use GetAllianceMemberID().
     * @return The player's alliance ID property to be written to a game save file.
     */
    public int GetAllianceIDForDataStorage()
    {
        return lAllianceID;
    }
    
    /**
     * Get the alliance the player is a full member of, or return ALLIANCE_ID_UNAFFILIATED if they aren't a full member of one.
     * @return The player's alliance, or ALLIANCE_ID_UNAFFILIATED if they aren't a full member of an alliance.
     */
    public int GetAllianceMemberID()
    {
        if(GetRequestingToJoinAlliance())
        {
            return Alliance.ALLIANCE_ID_UNAFFILIATED;
        }
        
        return lAllianceID;
    }
    
    /**
     * Get the alliance the player is attempting to join, or return ALLIANCE_ID_UNAFFILIATED if they aren't attempting to join one.
     * @return The alliance the player is attempting to join, or ALLIANCE_ID_UNAFFILIATED if they aren't attempting to join one.
     */
    public int GetAllianceJoiningID()
    {
        //Return the ID of the alliance the player is requesting to join. If they're already in one, the answer is "no alliance".
        if(!GetRequestingToJoinAlliance())
        {
            return Alliance.ALLIANCE_ID_UNAFFILIATED;
        }
        
        return lAllianceID;
    }
    
    public void SetAllianceID(int lAllianceID)
    {
        this.lAllianceID = lAllianceID;
        SetRequestingToJoinAlliance(false);
        Changed(false);
    }
    
    public void SetAllianceRequestToJoin(int lAllianceID)
    {
        this.lAllianceID = lAllianceID;
        SetRequestingToJoinAlliance(true);
        Changed(false);
    }
    
    public void RejectAllianceRequestToJoin()
    {
        lAllianceID = Alliance.ALLIANCE_ID_UNAFFILIATED;
        SetRequestingToJoinAlliance(false);
        Changed(false);
    }
    
    public void SetAllianceCooloffTime(int lAllianceCooloff)
    {
        dlyAllianceCooloff.Set(lAllianceCooloff);
    }
    
    public boolean GetAllianceCooloffExpired() { return dlyAllianceCooloff.Expired(); }
    
    public int GetAllianceCooloffRemaining() { return dlyAllianceCooloff.GetRemaining(); }
    
    public short GetKills() { return nKills; }
    
    public short GetDeaths() { return nDeaths; }
    
    public int GetOffenceSpending() { return lOffenceSpending; }
    
    public int GetDefenceSpending() { return lDefenceSpending; }
    
    public int GetDamageInflicted() { return lDamageInflicted; }
    
    public int GetDamageReceived() { return lDamageReceived; }
    
    public int GetStateTimeRemaining() { return dlyStateChange.GetRemaining(); }
    
    public boolean GetStateTimeExpired() { return dlyStateChange.Expired(); }

    public byte GetFlags1()
    {
        bBanned = GetBanned_Server();
        
        byte cFlags1 = 0x00;
        cFlags1 |= bBanned ? FLAG1_BANNED : 0x00;
        cFlags1 |= bHasCMS ? FLAG1_HAS_CMS : 0x00;
        cFlags1 |= bHasSAM ? FLAG1_HAS_SAM : 0x00;
        cFlags1 |= bLeader ? FLAG1_MP : 0x00;
        cFlags1 |= bAWOL ? FLAG1_AWOL : 0x00;
        cFlags1 |= bRespawnProtected ? FLAG1_RSPAWN_PROT : 0x00;
        return cFlags1;

    }
    public byte GetFlags2()
    {
        byte cFlags2 = 0x00;
        cFlags2 |= bRequestingAllianceJoin ? FLAG2_ALLIANCE_REQ_JOIN : 0x00;
        cFlags2 |= bAdmin ? FLAG2_ADMIN : 0x00;
        return cFlags2;
    }
    
    public boolean GetCanRespawn() { return Destroyed() && dlyStateChange.Expired(); }
    
    public void SetDead(int lRespawnTime)
    {
        //Just to be sure (although this is called on us when the game determines we've died).
        SetHP((short)0);
        
        //Register the death.
        nDeaths++;
        
        //Set respawn time.
        dlyStateChange.Set(lRespawnTime);
        
        //Remove all add-ons.
        SetHasAirDefenceSystem(false);
        SetHasCruiseMissileSystem(false);
        missiles = null;
        interceptors = null;
        
        //Notify listeners.
        Changed(false);
    }
    
    public void SetLastSeen()
    {
        oLastSeen = System.currentTimeMillis();
    }
    
    public boolean GetHasCruiseMissileSystem()
    {
        return bHasCMS;
    }
    
    public boolean GetHasAirDefenceSystem()
    {
        return bHasSAM;
    }
    
    public boolean GetAWOL()
    {
        return bAWOL;
    }
    
    public void SetAWOL(boolean bAWOL)
    {
        this.bAWOL = bAWOL;
        Changed(false);
    }
    
    public boolean GetRespawnProtected()
    {
        return bRespawnProtected;
    }
    
    public boolean GetRequestingToJoinAlliance()
    {
        return bRequestingAllianceJoin;
    }
    
    public boolean GetIsAnMP()
    {
        return bLeader;
    }
    
    public void SetRespawnProtected(boolean bProtected)
    {
        this.bRespawnProtected = bProtected;
        Changed(false);
    }
    
    public void SetRequestingToJoinAlliance(boolean bRequestingToJoin)
    {
        this.bRequestingAllianceJoin = bRequestingToJoin;
        Changed(false);
    }
    
    public void Respawn(short nHP, int lRespawnProtectionTime)
    {
        SetHP(nHP);
        dlyStateChange.Set(lRespawnProtectionTime);
        SetRespawnProtected(true);
        Changed(false);
    }
    
    public void SetCompassionateInvulnerability(int lProtectionTime)
    {
        dlyStateChange.Set(lProtectionTime);
        SetRespawnProtected(true);
        Changed(false);
    }
    
    /** Artificially advance the player's "last seen" time by the specified amount.
     * @param oParkTime Time to advance the player's "last seen" time by, in ms. */
    public void Park(long oParkTime)
    {
        oLastSeen += oParkTime;
        Changed(false);
    }
    
    private void SetHasCruiseMissileSystem(boolean bHasCMS)
    {
        this.bHasCMS = bHasCMS;
        Changed(true);
    }
    
    private void SetHasAirDefenceSystem(boolean bHasSAM)
    {
        this.bHasSAM = bHasSAM;
        Changed(true);
    }
    
    public void SetIsAnMP(boolean bIsAnMP)
    {
        this.bLeader = bIsAnMP;
        Changed(false);
    }
    
    public boolean SubtractWealth(int lWealth)
    {
        if(this.lWealth >= lWealth)
        {
            this.lWealth -= lWealth;
            Changed(false);
            return true;
        }
        
        return false;
    }
    
    public void AddWealth(int lWealth)
    {
        this.lWealth += lWealth;
        Changed(false);
    }
    
    public void SetWealth(int lWealth)
    {
        this.lWealth = lWealth;
        Changed(false);
    }
    
    public boolean IsRespawnProtected()
    {
        return bRespawnProtected;
    }
    
    public MissileSystem GetMissileSystem() { return missiles; }
    public MissileSystem GetInterceptorSystem() { return interceptors; }
    
    public boolean Functioning()
    {
        //Can be hit or detected. Immediately influential on the game.
        return !Destroyed() && !GetAWOL();
    }

    @Override
    public void SystemChanged(LaunchSystem system)
    {
        //One of our systems changed, therefore we changed.
        Changed(true);
    }
    
    public boolean GetIsAnAdmin()
    {
        return bAdmin;
    }
    
    public void SetIsAnAdmin(boolean bIsAdmin)
    {
        this.bAdmin = bIsAdmin;
        Changed(false);
    }
    
    public void ChangeName(String strNewName)
    {
        strName = strNewName;
        Changed(false);
    }
    
    /**
     * Increment the number of kills this player has achieved.
     * Don't call Changed(); This forms part of statistics and isn't automatically communicated.
     */
    public void IncrementKills()
    {
        nKills++;
    }
    
    /**
     * Add some offence spending by the player.
     * Don't call Changed(); This forms part of statistics and isn't automatically communicated.
     * @param lAmount The amount spent.
     */
    public void AddOffenceSpending(int lAmount)
    {
        lOffenceSpending += lAmount;
    }
    
    /**
     * Add some defence spending by the player.
     * Don't call Changed(); This forms part of statistics and isn't automatically communicated.
     * @param lAmount The amount spent.
     */
    public void AddDefenceSpending(int lAmount)
    {
        lDefenceSpending += lAmount;
    }
    
    /**
     * Add some damage to what the player has inflicted on others.
     * Don't call Changed(); This forms part of statistics and isn't automatically communicated.
     * @param nDamage HPs of damage inflicted.
     */
    public void AddDamageInflicted(short nDamage)
    {
        lDamageInflicted += nDamage;
    }
    
    /**
     * Add some damage to what the player has received.
     * Don't call this from within this class; it includes damage to stuff owned by the player and must be driven externally to this component.
     * Don't call Changed(); This forms part of statistics and isn't automatically communicated.
     * @param nDamage HPs of damage inflicted.
     */
    public void AddDamageReceived(short nDamage)
    {
        lDamageReceived += nDamage;
    }
    
    /**
     * Reset the player's stats, i.e. at the end of the week.
     * Don't call Changed(); This forms part of statistics and isn't automatically communicated.
     */
    public void ResetStats()
    {
        nKills = 0;
        nDeaths = 0;
        lOffenceSpending = 0;
        lDefenceSpending = 0;
        lDamageInflicted = 0;
        lDamageReceived = 0;
    }
    
    /**
     * Was this a specially big stat-containing player communicated by the server?
     * @return True if this player instance contains a snapshot of the player's stats.
     */
    public boolean GetHasFullStats()
    {
        return bHasFullStats;
    }

    @Override
    public boolean GetOwnedBy(int lID)
    {
        return lID == this.lID;
    }

    @Override
    public boolean ApparentlyEquals(LaunchEntity entity)
    {
        if(entity instanceof Player)
            return entity.GetID() == lID;
        return false;
    }
    
    /**
     * SERVER ONLY. Get status of the player being banned, to decide if their stuff should be sent to other players.
     * @return True if the player is banned, and should be effectively excluded from the game.
     */
    public boolean GetBanned_Server()
    {
        if(user != null)
            return user.GetBanState() != User.BanState.NOT;
        
        //Fail deadly.
        return true;
    }
    
    /**
     * CLIENT ONLY. Get status of the player being banned, to decide if they should not be rendered.
     * @return True if the player's banned flag is enabled.
     */
    public boolean GetBanned_Client()
    {
        return bBanned;
    }
    
    /**
     * Copy stats to this player. This allows full stats to be maintained in UI elements while players are online (and thus would otherwise overwrite the stats with zeroes).
     * @param statsCopy The old player reference to copy the stats from to the new, otherwise statless player reference.
     */
    public void StatsCopy(Player statsCopy)
    {
        this.nKills = statsCopy.nKills;
        this.nDeaths = statsCopy.nDeaths;
        this.lDamageInflicted = statsCopy.lDamageInflicted;
        this.lOffenceSpending = statsCopy.lOffenceSpending;
        this.lDamageReceived = statsCopy.lDamageReceived;
        this.lDefenceSpending = statsCopy.lDefenceSpending;
    }
}
