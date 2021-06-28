/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.game.entities;

import launch.game.GeoCoord;
import java.nio.ByteBuffer;
import launch.game.systems.LaunchSystem;
import launch.game.systems.LaunchSystemListener;
import launch.game.systems.MissileSystem;

/**
 *
 * @author tobster
 */
public class MissileSite extends Structure implements LaunchSystemListener
{
    private static final int DATA_SIZE = 1;
    
    MissileSystem missiles = null;
    
    private boolean bNuclear;
    
    /** New. */
    public MissileSite(int lID, GeoCoord geoPosition, short nHP, short nMaxHP, int lOwnerID, boolean bRespawnProtected, int lBootTime, int lReloadTime, byte cMissileSlots, boolean bNuclear, int lChargeTime)
    {
        super(lID, geoPosition, nHP, nMaxHP, lOwnerID, bRespawnProtected, lBootTime, lChargeTime);
        missiles = new MissileSystem(this, lReloadTime, cMissileSlots);
        this.bNuclear = bNuclear;
    }
    
    /** From save. */
    public MissileSite(int lID, GeoCoord geoPosition, short nHP, short nMaxHP, String strName, int lOwnerID, byte cState, int lStateTime, boolean bNuclear, MissileSystem missileSystem, int lChargeTime)
    {
        super(lID, geoPosition, nHP, nMaxHP, strName, lOwnerID, cState, lStateTime, lChargeTime);
        missiles = missileSystem;
        missiles.SetSystemListener(this);
        this.bNuclear = bNuclear;
    }
    
    /** From comms. */
    public MissileSite(ByteBuffer bb, int lReceivingID)
    {
        super(bb, lReceivingID);
        missiles = new MissileSystem(this, bb);
        bNuclear = (bb.get() != 0x00);
    }

    @Override
    public void Tick(int lMS)
    {
        super.Tick(lMS);
        missiles.Tick(lMS);
    }
    
    @Override
    public byte[] GetData(int lAskingID)
    {
        byte[] cBaseData = super.GetData(lAskingID);
        byte[] cMissileSystemData = missiles.GetData();
        
        ByteBuffer bb = ByteBuffer.allocate(DATA_SIZE + cBaseData.length + cMissileSystemData.length);
        
        bb.put(cBaseData);
        bb.put(cMissileSystemData);
        bb.put((byte)(bNuclear? 0xFF : 0x00));
        
        return bb.array();
    }
    
    public boolean CanTakeNukes()
    {
        return bNuclear;
    }
    
    public MissileSystem GetMissileSystem() { return missiles; }
    
    public void UpgradeToNuclear()
    {
        bNuclear = true;
        Changed(false);
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
        return "missile site";
    }

    @Override
    public boolean ApparentlyEquals(LaunchEntity entity)
    {
        if(entity instanceof MissileSite)
            return entity.GetID() == lID;
        return false;
    }
}
