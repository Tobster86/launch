/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.game.entities;

import java.nio.ByteBuffer;
import launch.game.GeoCoord;
import launch.game.systems.LaunchSystem;
import launch.game.systems.LaunchSystemListener;
import launch.game.systems.MissileSystem;

/**
 *
 * @author tobster
 */
public class SAMSite extends Structure implements LaunchSystemListener
{
    private static final int DATA_SIZE_OWNER = 1;
    
    public static final byte MODE_AUTO = 0;        //Will automatically engage threats.
    public static final byte MODE_SEMI_AUTO = 1;   //Will automatically engage threats if the player is offline.
    public static final byte MODE_MANUAL = 2;      //Will not automatically engage threats.
    
    private byte cMode;
    
    private MissileSystem interceptors = null;
    
    /** New. */
    public SAMSite(int lID, GeoCoord geoPosition, short nHP, short nMaxHP, int lOwnerID, boolean bRespawnProtected, int lBootTime, int lReloadTime, byte cInterceptorSlots, int lChargeTime)
    {
        super(lID, geoPosition, nHP, nMaxHP, lOwnerID, bRespawnProtected, lBootTime, lChargeTime);
        this.cMode = MODE_AUTO;
        interceptors = new MissileSystem(this, lReloadTime, cInterceptorSlots);
    }
    
    /** From save. */
    public SAMSite(int lID, GeoCoord geoPosition, short nHP, short nMaxHP, String strName, int lOwnerID, byte cState, int lStateTime, byte cMode, MissileSystem interceptorSystem, int lChargeTime)
    {
        super(lID, geoPosition, nHP, nMaxHP, strName, lOwnerID, cState, lStateTime, lChargeTime);
        this.cMode = cMode;
        interceptors = interceptorSystem;
        interceptors.SetSystemListener(this);
    }
    
    /** From comms. */
    public SAMSite(ByteBuffer bb, int lReceivingID)
    {
        super(bb, lReceivingID);
        
        if(lReceivingID == lOwnerID)
            cMode = bb.get();
        
        interceptors = new MissileSystem(this, bb);
    }

    @Override
    public void Tick(int lMS)
    {
        super.Tick(lMS);
        interceptors.Tick(lMS);
    }
    
    @Override
    public byte[] GetData(int lAskingID)
    {
        byte[] cBaseData = super.GetData(lAskingID);
        byte[] cInterceptorSystemData = interceptors.GetData();
        
        ByteBuffer bb = ByteBuffer.allocate((lAskingID == lOwnerID? DATA_SIZE_OWNER : 0) + cBaseData.length + cInterceptorSystemData.length);
        
        bb.put(cBaseData);
        
        if(lAskingID == lOwnerID)
            bb.put(cMode);
        
        bb.put(cInterceptorSystemData);
        
        return bb.array();
    }
    
    public MissileSystem GetInterceptorSystem() { return interceptors; }
    
    public boolean GetAuto() { return cMode == MODE_AUTO; }
    
    public boolean GetSemiAuto() { return cMode == MODE_SEMI_AUTO; }
    
    public boolean GetManual() { return cMode == MODE_MANUAL; }
    
    public byte GetMode() { return cMode; }
    
    public void SetMode(byte cMode)
    {
        this.cMode = cMode;
        Changed(true);
    }

    @Override
    public void SystemChanged(LaunchSystem system)
    {
        //One of our systems changed, therefore we changed.
        Changed(false);
    }

    @Override
    public String GetTypeName()
    {
        return "SAM site";
    }

    @Override
    public boolean ApparentlyEquals(LaunchEntity entity)
    {
        if(entity instanceof SAMSite)
            return entity.GetID() == lID;
        return false;
    }
}
