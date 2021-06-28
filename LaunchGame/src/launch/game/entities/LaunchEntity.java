/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.game.entities;

import launch.game.GeoCoord;
import java.nio.ByteBuffer;
import launch.game.Defs;

/**
 *
 * @author tobster
 */
public abstract class LaunchEntity
{
    private static final int DATA_SIZE = 12;
    public static final int ID_NONE = Defs.THE_GREAT_BIG_NOTHING;
    
    private LaunchEntityListener listener = null;
    
    protected int lID;
    protected GeoCoord geoPosition;
    
    public LaunchEntity(int lID, GeoCoord geoPosition)
    {
        this.lID = lID;
        this.geoPosition = geoPosition;
    }
    
    public LaunchEntity(ByteBuffer bb)
    {
        this.lID = bb.getInt();
        this.geoPosition = new GeoCoord(bb.getFloat(), bb.getFloat());
    }
    
    public abstract void Tick(int lMS);
    public int GetID() { return lID; }
    public GeoCoord GetPosition() { return geoPosition; }
    
    public void SetPosition(GeoCoord geoPosition)
    {
        this.geoPosition = geoPosition;
        Changed(false);
    }
    
    /**
     * Get data to communicate, base class. Subclasses should override and super call this method.
     * @param lAskingID The ID of the player this data will be sent to.
     * @return The data to communicate.
     */
    public byte[] GetData(int lAskingID)
    {
        ByteBuffer bb = ByteBuffer.allocate(DATA_SIZE);
        bb.putInt(lID);
        bb.putFloat(geoPosition.GetLatitude());
        bb.putFloat(geoPosition.GetLongitude());
        return bb.array();
    }
    
    public void SetListener(LaunchEntityListener listener) { this.listener = listener; }
    
    /**
     * Call when something changes that should be communicated to the players.
     * @param bOwner Whatever changed is only relevant to the player that owns this entity. 
     */
    protected final void Changed(boolean bOwner)
    {
        if(listener != null)
        {
            listener.EntityChanged(this, bOwner);
        }
    }
    
    public abstract boolean GetOwnedBy(int lID);
    
    /**
     * For checking if an entity references the same conceptual entity. For inspecting entity updates etc. DO NOT USE for containers/memory management (memory-referential equality)!
     * @param entity The entity to check.
     * @return Whether it appears to reference the same entity.
     */
    public abstract boolean ApparentlyEquals(LaunchEntity entity);
}
