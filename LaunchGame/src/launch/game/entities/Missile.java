/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.game.entities;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import launch.game.GeoCoord;

/**
 *
 * @author tobster
 */
public class Missile extends LaunchEntity
{
    private static final int DATA_SIZE = 18;
    
    private byte cType;
    private int lOwnerID;
    private boolean bTracking;
    private GeoCoord geoTarget;
    private int lTargetID;
    
    //Flags (not transmitted).
    private boolean bFlying = true;         //Indicates the missile has not reached its target or been shot down.
    
    //"Threatens player" optimisations. Server only.
    private List<Integer> StructureThreatenedPlayers = new ArrayList<>();
    
    public Missile(int lID, GeoCoord geoPosition, byte cType, int lOwnerID, boolean bTracking, GeoCoord geoTarget, int lTargetID)
    {
        super(lID, geoPosition);
        this.cType = cType;
        this.lOwnerID = lOwnerID;
        this.bTracking = bTracking;
        this.geoTarget = geoTarget;
        this.lTargetID = lTargetID;
    }
    
    public Missile(ByteBuffer bb)
    {
        super(bb);
        cType = bb.get();
        lOwnerID = bb.getInt();
        bTracking = (bb.get() != 0x00);
        geoTarget = new GeoCoord(bb.getFloat(), bb.getFloat());
        lTargetID = bb.getInt();
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
        bb.put((byte)(bTracking? 0xFF : 0x00));
        bb.putFloat(geoTarget.GetLatitude());
        bb.putFloat(geoTarget.GetLongitude());
        bb.putInt(lTargetID);
        
        return bb.array();
    }
    
    public byte GetType() { return cType; }
    
    public int GetOwnerID() { return lOwnerID; }
    
    public boolean GetTracking() { return bTracking; }
    
    /**
     * Get the target location of this missile. WARNING: This won't actually be where the missile's heading if it's tracking a player! Callers must check this!
     * @return The missile's target location, or gibberish if it's player tracking.
     */
    public GeoCoord GetTarget() { return geoTarget; }
    
    public int GetTargetID() { return lTargetID; }
    
    public boolean Flying()
    {
        return bFlying;
    }
    
    public void Destroy()
    {
        bFlying = false;
    }
    
    public void SelfDestruct()
    {
        //To be used to self-destruct missiles to prevent an error only. Will cause an explosion. Don't give this power to players.
        bTracking = false;
        geoTarget = geoPosition;
    }
    
    public void ClearStructureThreatenedPlayers()
    {
        StructureThreatenedPlayers.clear();
    }
    
    public void AddStructureThreatenedPlayer(int lPlayerID)
    {
        StructureThreatenedPlayers.add(lPlayerID);
    }
    
    public boolean ThreatensPlayersStructures(int lPlayerID)
    {
        return StructureThreatenedPlayers.contains(lPlayerID);
    }

    @Override
    public boolean GetOwnedBy(int lID)
    {
        return lID == lOwnerID;
    }

    @Override
    public boolean ApparentlyEquals(LaunchEntity entity)
    {
        if(entity instanceof Missile)
            return entity.GetID() == lID;
        return false;
    }
}
