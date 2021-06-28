/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.game.entities;

import java.nio.ByteBuffer;
import launch.game.GeoCoord;

/**
 *
 * @author tobster
 */
public class Interceptor extends LaunchEntity
{
    private static final int DATA_SIZE = 10;
    
    private byte cType;             //The type of interceptor this is.
    private int lOwnerID;           //The player that launched the interceptor missile.
    private int lTargetID;          //The missile the interceptor is chasing.
    private boolean bPlayerLaunched;//Interceptors launched manually by the player gain an accuracy bonus.
    
    public Interceptor(int lID, GeoCoord geoPosition, int lOwnerID, int lTargetID, byte cType, boolean bPlayerLaunched)
    {
        super(lID, geoPosition);
        this.cType = cType;
        this.lOwnerID = lOwnerID;
        this.lTargetID = lTargetID;
        this.bPlayerLaunched = bPlayerLaunched;
    }
    
    public Interceptor(ByteBuffer bb)
    {
        super(bb);
        this.cType = bb.get();
        this.lOwnerID = bb.getInt();
        this.lTargetID = bb.getInt();
        bPlayerLaunched = (bb.get() != 0x00);
    }

    @Override
    public void Tick(int lMS)
    {
        //Nothing to do here.
    }

    @Override
    public byte[] GetData(int lAskingID)
    {
        byte[] cBaseData = super.GetData(lAskingID);
        
        ByteBuffer bb = ByteBuffer.allocate(DATA_SIZE + cBaseData.length);
        
        bb.put(cBaseData);
        bb.put(cType);
        bb.putInt(lOwnerID);
        bb.putInt(lTargetID);
        bb.put((byte)(bPlayerLaunched? 0xFF : 0x00));
        
        return bb.array();
    }
    
    public byte GetType() { return cType; }
    
    public int GetOwnerID() { return lOwnerID; }
    
    public int GetTargetID() { return lTargetID; }
    
    public boolean GetPlayerLaunched() { return bPlayerLaunched; }

    @Override
    public boolean GetOwnedBy(int lID)
    {
        return lID == lOwnerID;
    }

    @Override
    public boolean ApparentlyEquals(LaunchEntity entity)
    {
        if(entity instanceof Interceptor)
            return entity.GetID() == lID;
        return false;
    }
}
