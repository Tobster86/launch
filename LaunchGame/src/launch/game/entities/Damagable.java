/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.game.entities;

import launch.game.GeoCoord;
import java.nio.ByteBuffer;

/**
 *
 * @author tobster
 */
public abstract class Damagable extends LaunchEntity
{
    private static final int DATA_SIZE = 4;
    
    private short nHP;
    private short nMaxHP;
    
    public Damagable(int lID, GeoCoord geoPosition, short nHP, short nMaxHP)
    {
        super(lID, geoPosition);
        this.nHP = nHP;
        this.nMaxHP = nMaxHP;
    }
    
    public Damagable(ByteBuffer bb)
    {
        super(bb);
        nHP = bb.getShort();
        nMaxHP = bb.getShort();
    }

    @Override
    public byte[] GetData(int lAskingID)
    {
        byte[] cBaseData = super.GetData(lAskingID);
        
        ByteBuffer bb = ByteBuffer.allocate(DATA_SIZE + cBaseData.length);
        bb.put(cBaseData);
        bb.putShort(nHP);
        bb.putShort(nMaxHP);
        return bb.array();
    }
    
    /**
     * Inflict damage. Return the amount actually inflicted.
     * @param nDamage The damage to inflict.
     * @return The damage actually inflicted, if the damagable's HP was less than the damage to inflict.
     */
    public short InflictDamage(short nDamage)
    {
        short nDamageInflicted = (short)Math.min((int)nDamage, (int)nHP);
        
        nHP -= nDamage;
        
        if(nHP < 0)
            nHP = 0;
        
        Changed(false);
        
        return nDamageInflicted;
    }
    
    public short GetHP() { return nHP; }
    public short GetMaxHP() { return nMaxHP; }
    
    public void SetHP(short nHP)
    {
        this.nHP = nHP;
    }
    
    public void SetMaxHP(short nMaxHP)
    {
        this.nMaxHP = nMaxHP;
    }
    
    public void AddHP(short nHP)
    {
        this.nHP += nHP;
        this.nHP = (short)Math.min(this.nHP, nMaxHP);
        Changed(false);
    }
    
    public short GetHPDeficit()
    {
        //Return how many HP away from full the damagable is.
        return (short)(nMaxHP - nHP);
    }
    
    public void FullyRepair()
    {
        nHP = nMaxHP;
        Changed(false);
    }
    
    public boolean Destroyed() { return nHP <= 0; }
    
    public boolean AtFullHealth() { return nHP == nMaxHP; }
}
