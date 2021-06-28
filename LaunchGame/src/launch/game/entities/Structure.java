/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.game.entities;

import launch.game.GeoCoord;
import java.nio.ByteBuffer;
import launch.utilities.LaunchUtilities;
import launch.utilities.ShortDelay;

/**
 *
 * @author tobster
 */
public abstract class Structure extends Damagable
{
    private static final int DATA_SIZE = 9;
    private static final int DATA_SIZE_OWNER = 4;
    
    private static final byte STATE_OFFLINE = 0;
    private static final byte STATE_BOOTING = 0x40;
    private static final byte STATE_ONLINE = (byte)0x80;
    private static final byte STATE_DECOMMISSIONING = (byte)0xC0;
    
    public static final byte STATE_MASK = 0x1F;
    
    private static final int FLAG_STATE_LB = 0x01;
    private static final int FLAG_STATE_UB = 0x02;
    private static final int RESPAWN_PROTECTED = 0x04;
    private static final int FLAG_UNUSED1 = 0x08;
    private static final int FLAG_UNUSED2 = 0x10;
    private static final int FLAG_UNUSED3 = 0x20;
    private static final int FLAG_UNUSED4 = 0x40;
    private static final int FLAG_UNUSED5 = 0x80;
    
    private String strName;
    protected int lOwnerID;
    private byte cFlags;
    private ShortDelay dlyStateTime;
    private ShortDelay dlyChargeOwner;
    
    /** New. */
    public Structure(int lID, GeoCoord geoPosition, short nHP, short nMaxHP, int lOwnerID, boolean bRespawnProtected, int lBootTime, int lChargeOwnerTime)
    {
        super(lID, geoPosition, nHP, nMaxHP);
        this.strName = "";
        this.lOwnerID = lOwnerID;
        SetRespawnProtected(bRespawnProtected);
        dlyStateTime = new ShortDelay(lBootTime);
        dlyChargeOwner = new ShortDelay(lChargeOwnerTime);
        SetBooting();
    }
    
    /** From save. */
    public Structure(int lID, GeoCoord geoPosition, short nHP, short nMaxHP, String strName, int lOwnerID, byte cFlags, int lStateTime, int lChargeOwnerTime)
    {
        super(lID, geoPosition, nHP, nMaxHP);
        this.strName = strName;
        this.lOwnerID = lOwnerID;
        this.cFlags = cFlags;
        dlyStateTime = new ShortDelay(lStateTime);
        dlyChargeOwner = new ShortDelay(lChargeOwnerTime);
    }
    
    /**
     * A backup feature that allows a structure from a seperate game snapshot to be re-ID'd for incorporation into a different game.
     * @param lNewID The new ID to assign. Should use an atomic method to determine the ID.
     * @return The structure instance with a new ID.
     */
    public Structure ReIDAndReturnSelf(int lNewID)
    {
        this.lID = lNewID;
        return this;
    }
    
    /** From comms.
     * @param bb The byte buffer of received data.
     * @param lReceivingID The player ID of the client for unpacking potentially player-only data. */
    public Structure(ByteBuffer bb, int lReceivingID)
    {
        super(bb);
        strName = LaunchUtilities.StringFromData(bb);
        lOwnerID = bb.getInt();
        cFlags = bb.get();
        dlyStateTime = new ShortDelay(bb);
        
        if(lOwnerID == lReceivingID)
            dlyChargeOwner = new ShortDelay(bb);
        else
            dlyChargeOwner = new ShortDelay(0);
    }

    @Override
    public void Tick(int lMS)
    {
        dlyStateTime.Tick(lMS);
        
        if(GetBooting())
        {
            dlyChargeOwner.Tick(lMS);
            
            if(dlyStateTime.Expired())
            {
                SetOnline();
                Changed(false);
            }
        }
        else if(GetOnline())
        {
            dlyChargeOwner.Tick(lMS);
        }
    }

    @Override
    public byte[] GetData(int lAskingID)
    {
        byte[] cBaseData = super.GetData(lAskingID);
        ByteBuffer bb = ByteBuffer.allocate(DATA_SIZE + (lAskingID == lOwnerID? DATA_SIZE_OWNER : 0) + cBaseData.length + LaunchUtilities.GetStringDataSize(strName));
        bb.put(cBaseData);
        bb.put(LaunchUtilities.GetStringData(strName));
        bb.putInt(lOwnerID);
        bb.put(cFlags);
        dlyStateTime.GetData(bb);
        
        if(lAskingID == lOwnerID)
        {
            dlyChargeOwner.GetData(bb);
        }
        
        return bb.array();
    }
    
    public void BringOnline(int lBootTime)
    {
        if(GetOffline())
        {
            SetBooting();
            dlyStateTime.Set(lBootTime);
            Changed(false);
        }
    }
    
    public void Reboot(int lBootTime)
    {
        if(GetOnline() || GetBooting())
        {
            SetBooting();
            dlyStateTime.Set(lBootTime);
            Changed(false);
        }
    }
    
    public void TakeOffline()
    {
        if(GetOnline() || GetBooting())
        {
            SetOffline();
            Changed(false);
        }
    }
    
    public void Sell(int lDecommissionTime)
    {
        if(!GetSelling())
        {
            SetSelling();
            dlyStateTime.Set(lDecommissionTime);
            Changed(false);
        }
    }
    
    public String GetName() { return strName; }
    
    public void SetName(String strName)
    {
        this.strName = strName;
        Changed(false);
    }
    
    public int GetOwnerID() { return lOwnerID; }
    
    public boolean GetOffline()
    {
        return (cFlags & ~STATE_MASK) == (STATE_OFFLINE & ~STATE_MASK);
    }
    
    public boolean GetBooting()
    {
        return (cFlags & ~STATE_MASK) == (STATE_BOOTING & ~STATE_MASK);
    }
    
    public boolean GetOnline()
    {
        return (cFlags & ~STATE_MASK) == (STATE_ONLINE & ~STATE_MASK) && !Destroyed();
    }
    
    public boolean GetSelling()
    {
        return (cFlags & ~STATE_MASK) == (STATE_DECOMMISSIONING & ~STATE_MASK) && !Destroyed();
    }
    
    protected final void SetOffline()
    {
        cFlags &= STATE_MASK;
        cFlags |= (~STATE_MASK & STATE_OFFLINE);
    }
    
    protected final void SetBooting()
    {
        cFlags &= STATE_MASK;
        cFlags |= (~STATE_MASK & STATE_BOOTING);
    }
    
    protected final void SetOnline()
    {
        cFlags &= STATE_MASK;
        cFlags |= (~STATE_MASK & STATE_ONLINE);
    }
    
    protected final void SetSelling()
    {
        cFlags &= STATE_MASK;
        cFlags |= (~STATE_MASK & STATE_DECOMMISSIONING);
    }
    
    public boolean GetRespawnProtected()
    {
        return ( cFlags & RESPAWN_PROTECTED ) != 0x00;
    }
    
    public final void SetRespawnProtected(boolean bProtected)
    {
        if(bProtected)
            cFlags |= RESPAWN_PROTECTED;
        else
            cFlags &= ~RESPAWN_PROTECTED;
        Changed(false);
    }
    
    public int GetStateTimeRemaining() { return dlyStateTime.GetRemaining(); }
    
    public boolean GetStateTimeExpired() { return dlyStateTime.Expired(); }
    
    public byte GetFlags() { return cFlags; }
    
    public boolean GetRunning()
    {
        if(GetOnline())
            return true;

        return GetBooting();
    }
    
    public int GetChargeOwnerTimeRemaining() { return dlyChargeOwner.GetRemaining(); }
    
    public boolean GetChargeOwner() { return dlyChargeOwner.Expired(); }
    
    public void SetChargeOwnerTime(int lChargeOwnerTime)
    {
        dlyChargeOwner.Set(lChargeOwnerTime);
        Changed(true);
    }
    
    public abstract String GetTypeName();

    @Override
    public boolean GetOwnedBy(int lID)
    {
        return lID == lOwnerID;
    }
}
