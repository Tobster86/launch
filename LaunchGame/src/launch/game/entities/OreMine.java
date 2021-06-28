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
public class OreMine extends Structure
{
    private static final int DATA_SIZE_OWNER = 4;
    
    private ShortDelay dlyGenerate;
    
    /** New. */
    public OreMine(int lID, GeoCoord geoPosition, short nHP, short nMaxHP, int lOwnerID, boolean bRespawnProtected, int lBootTime, int lChargeTime, int lGenerateTime)
    {
        super(lID, geoPosition, nHP, nMaxHP, lOwnerID, bRespawnProtected, lBootTime, lChargeTime);
        dlyGenerate = new ShortDelay(lGenerateTime);
    }
    
    /** From save. */
    public OreMine(int lID, GeoCoord geoPosition, short nHP, short nMaxHP, String strName, int lOwnerID, byte cFlags, int lStateTime, int lChargeTime, int lGenerateTime)
    {
        super(lID, geoPosition, nHP, nMaxHP, strName, lOwnerID, cFlags, lStateTime, lChargeTime);
        dlyGenerate = new ShortDelay(lGenerateTime);
    }
    
    /** From comms. */
    public OreMine(ByteBuffer bb, int lReceivingID)
    {
        super(bb, lReceivingID);
        
        if(lReceivingID == lOwnerID)
            dlyGenerate = new ShortDelay(bb);
        else
            dlyGenerate = new ShortDelay(0);
    }

    @Override
    public void Tick(int lMS)
    {
        super.Tick(lMS);
        
        if(GetOnline())
        {
            dlyGenerate.Tick(lMS);
        }
    }
    
    @Override
    public byte[] GetData(int lAskingID)
    {
        byte[] cBaseData = super.GetData(lAskingID);
        
        ByteBuffer bb = ByteBuffer.allocate((lAskingID == lOwnerID? DATA_SIZE_OWNER : 0) + cBaseData.length);
        bb.put(cBaseData);
        
        if(lAskingID == lOwnerID)
            dlyGenerate.GetData(bb);
        
        return bb.array();
    }
    
    public void SetGenerateTime(int lTime)
    {
        dlyGenerate.Set(lTime);
        Changed(true);
    }
    
    public boolean GetGenerateOre()
    {
        return dlyGenerate.Expired();
    }
    
    public int GetGenerateTimeRemaining()
    {
        return dlyGenerate.GetRemaining();
    }

    @Override
    public String GetTypeName()
    {
        return "ore mine";
    }

    @Override
    public boolean ApparentlyEquals(LaunchEntity entity)
    {
        if(entity instanceof OreMine)
            return entity.GetID() == lID;
        return false;
    }
}
