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
public class Radiation extends LaunchEntity
{
    private static final int DATA_SIZE = 8;
    
    private float fltRadius;        //Radius of this radioactive region.
    private ShortDelay dlyExpire;    //Time until the radiation subsides.

    public Radiation(int lID, GeoCoord geoPosition, float fltRadius, int lExpiry)
    {
        super(lID, geoPosition);
        this.fltRadius = fltRadius;
        dlyExpire = new ShortDelay(lExpiry);
    }
    
    public Radiation(ByteBuffer bb)
    {
        super(bb);
        this.fltRadius = bb.getFloat();
        dlyExpire = new ShortDelay(bb);
    }

    @Override
    public void Tick(int lMS)
    {
        dlyExpire.Tick(lMS);
    }
    
    public boolean GetExpired() { return dlyExpire.Expired(); }

    @Override
    public byte[] GetData(int lAskingID)
    {
        byte cBaseData[] = super.GetData(lAskingID);
        
        ByteBuffer bb = ByteBuffer.allocate(DATA_SIZE + cBaseData.length);
        bb.put(cBaseData);
        bb.putFloat(fltRadius);
        dlyExpire.GetData(bb);
        return bb.array();
    }
    
    public float GetRadius() { return fltRadius; }
    
    public int GetExpiryTime() { return dlyExpire.GetRemaining(); }

    @Override
    public boolean GetOwnedBy(int lID)
    {
        //No concept of "ownership".
        return true;
    }

    @Override
    public boolean ApparentlyEquals(LaunchEntity entity)
    {
        if(entity instanceof Radiation)
            return entity.GetID() == lID;
        return false;
    }
}
