/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.game.entities;

import java.nio.ByteBuffer;
import launch.game.GeoCoord;
import launch.utilities.ShortDelay;

/**
 *
 * @author tobster
 */
public class SentryGun extends Structure
{
    private static final int DATA_SIZE = 4;
    
    private ShortDelay dlyReload;
    
    /** New. */
    public SentryGun(int lID, GeoCoord geoPosition, short nHP, short nMaxHP, int lOwnerID, boolean bRespawnProtected, int lBootTime, int lChargeTime)
    {
        super(lID, geoPosition, nHP, nMaxHP, lOwnerID, bRespawnProtected, lBootTime, lChargeTime);
        dlyReload = new ShortDelay();
    }
    
    /** From save. */
    public SentryGun(int lID, GeoCoord geoPosition, short nHP, short nMaxHP, String strName, int lOwnerID, byte cFlags, int lStateTime, int lChargeTime, int lReloadTime)
    {
        super(lID, geoPosition, nHP, nMaxHP, strName, lOwnerID, cFlags, lStateTime, lChargeTime);
        dlyReload = new ShortDelay(lReloadTime);
    }
    
    /** From comms. */
    public SentryGun(ByteBuffer bb, int lReceivingID)
    {
        super(bb, lReceivingID);
        dlyReload = new ShortDelay(bb.getInt());
    }

    @Override
    public void Tick(int lMS)
    {
        super.Tick(lMS);
        dlyReload.Tick(lMS);
    }
    
    @Override
    public byte[] GetData(int lAskingID)
    {
        byte[] cBaseData = super.GetData(lAskingID);
        
        ByteBuffer bb = ByteBuffer.allocate(DATA_SIZE + cBaseData.length);
        
        bb.put(cBaseData);
        dlyReload.GetData(bb);
        
        return bb.array();
    }
    
    public void SetReloadTime(int lTime)
    {
        dlyReload.Set(lTime);
        Changed(false);
    }
    
    public boolean GetCanFire()
    {
        return dlyReload.Expired();
    }
    
    public int GetReloadTimeRemaining()
    {
        return dlyReload.GetRemaining();
    }

    @Override
    public String GetTypeName()
    {
        return "sentry gun";
    }

    @Override
    public boolean ApparentlyEquals(LaunchEntity entity)
    {
        if(entity instanceof SentryGun)
            return entity.GetID() == lID;
        return false;
    }
}
