/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.game.treaties;

import java.nio.ByteBuffer;
import launch.game.Alliance;

/**
 *
 * @author tobster
 */
public abstract class Treaty
{
    private static final int DATA_SIZE = 13;
    
    public enum Type
    {
        WAR,
        AFFILIATION,
        AFFILIATION_REQUEST,
        AFFILIATION_REJECT      //Not a type. Exists to simplify diplomacy task code.
    }
    
    private int lID;
    protected int lAllianceID1;
    protected int lAllianceID2;
    
    public Treaty(int lID, int lAllianceID1, int lAllianceID2)
    {
        this.lID = lID;
        this.lAllianceID1 = lAllianceID1;
        this.lAllianceID2 = lAllianceID2;
    }
    
    public Treaty(ByteBuffer bb)
    {
        lID = bb.getInt();
        lAllianceID1 = bb.getInt();
        lAllianceID2 = bb.getInt();
    }
    
    public byte[] GetData()
    {
        ByteBuffer bb = ByteBuffer.allocate(DATA_SIZE);
        bb.put((byte)GetType().ordinal());
        bb.putInt(lID);
        bb.putInt(lAllianceID1);
        bb.putInt(lAllianceID2);
        return bb.array();
    }
    
    public int GetID() { return lID; }
    public int GetAllianceID1() { return lAllianceID1; }
    public int GetAllianceID2() { return lAllianceID2; }
    public abstract Type GetType();
    
    /**
     * Return true if the alliance of the given ID is a party to this treaty.
     * @param lID The alliance to check.
     * @return Whether the alliance is a party to this treaty.
     */
    public boolean IsAParty(int lID)
    {
        if(lAllianceID1 == lID)
            return true;
        
        return lAllianceID2 == lID;
    }
    
    /**
     * Return true if the alliances of the given IDs are parties to this treaty.
     * @param lID1 One alliance.
     * @param lID2 T'other alliance.
     * @return True if both alliances are party to this treaty.
     */
    public boolean AreParties(int lID1, int lID2)
    {
        if(lAllianceID1 == lID1)
            return lAllianceID2 == lID2;
        
        if(lAllianceID2 == lID1)
            return lAllianceID1 == lID2;
        
        return false;
    }
    
    /**
     * Return the ID of the other party in this alliance.
     * @param lID The alliance being queried.
     * @return The other alliance in the treaty, or ALLIANCE_ID_UNAFFILIATED if lID is not a party to this alliance.
     */
    public int OtherParty(int lID)
    {
        if(lID == lAllianceID1)
            return lAllianceID2;
        
        if(lID == lAllianceID2)
            return lAllianceID1;
        
        return Alliance.ALLIANCE_ID_UNAFFILIATED;
    }
}
