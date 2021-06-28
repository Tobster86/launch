/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.game.entities;

import java.nio.ByteBuffer;
import launch.game.GeoCoord;
import launch.utilities.LaunchUtilities;
import launch.utilities.ShortDelay;

/**
 *
 * @author tobster
 */
public class Loot extends LaunchEntity
{
    private static final int DATA_SIZE = 8;
    
    private int lValue;
    private ShortDelay dlyExpiry;
    private String strDescription;
    
    //Flags (not transmitted).
    private boolean bCollected = false;     //Indicates the loot has been collected and may be cleared up.
    
    public Loot(int lID, GeoCoord geoPosition, int lValue, int lExpiry, String strDescription)
    {
        super(lID, geoPosition);
        this.lValue = lValue;
        this.dlyExpiry = new ShortDelay(lExpiry);
        this.strDescription = strDescription;
    }
    
    public Loot(ByteBuffer bb)
    {
        super(bb);
        lValue = bb.getInt();
        dlyExpiry = new ShortDelay(bb);
        strDescription = LaunchUtilities.StringFromData(bb);
    }

    @Override
    public void Tick(int lMS)
    {
        dlyExpiry.Tick(lMS);
    }

    @Override
    public byte[] GetData(int lAskingID)
    {
        byte cBaseData[] = super.GetData(lAskingID);
        
        ByteBuffer bb = ByteBuffer.allocate(DATA_SIZE + cBaseData.length + LaunchUtilities.GetStringDataSize(strDescription));
        bb.put(cBaseData);
        bb.putInt(lValue);
        dlyExpiry.GetData(bb);
        bb.put(LaunchUtilities.GetStringData(strDescription));
        
        return bb.array();
    }
    
    public boolean Expired()
    {
        return dlyExpiry.Expired();
    }
    
    public int GetValue() { return lValue; }
    
    public int GetExpiryRemaining() { return dlyExpiry.GetRemaining(); }
    
    public String GetDescription() { return strDescription; }
    
    /**
     * Mark the loot as collected, such that it will be removed from the game during the next tick.
     */
    public void Collect() { bCollected = true; }
    
    public boolean Collected() { return bCollected; }

    @Override
    public boolean GetOwnedBy(int lID)
    {
        //Everyone owns money.
        return true;
    }

    @Override
    public boolean ApparentlyEquals(LaunchEntity entity)
    {
        if(entity instanceof Loot)
            return entity.GetID() == lID;
        return false;
    }
}
