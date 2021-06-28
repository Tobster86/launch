/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.game;

import java.nio.ByteBuffer;
import launch.game.entities.LaunchEntity;
import launch.game.entities.LaunchEntityListener;
import launch.utilities.LaunchUtilities;

/**
 *
 * @author tobster
 */
public class Alliance
{
    private static final int DATA_SIZE = 8;
    
    public static final int ALLIANCE_ID_UNAFFILIATED = Defs.THE_GREAT_BIG_NOTHING;
    public static final int ALLIANCE_AVATAR_DEFAULT = Defs.THE_GREAT_BIG_NOTHING;
    public static final int ALLIANCE_MAX_DESCRIPTION_CHARS = 140;
    
    private LaunchEntityListener listener = null;
    
    private int lID;
    private String strName;
    private String strDescription;
    private int lAvatarID;
    
    //New.
    public Alliance(int lID, String strName, String strDescription, int lAvatarID)
    {
        this.lID = lID;
        this.strName = strName;
        this.strDescription = strDescription;
        this.lAvatarID = lAvatarID;
    }
    
    //From save.
    public Alliance(int lID, String strName, String strDescription, int lAvatarID, int lRank)
    {
        this.lID = lID;
        this.strName = strName;
        this.strDescription = strDescription;
        this.lAvatarID = lAvatarID;
    }
    
    //Communicated.
    public Alliance(ByteBuffer bb)
    {
        lID = bb.getInt();
        strName = LaunchUtilities.StringFromData(bb);
        strDescription = LaunchUtilities.StringFromData(bb);
        lAvatarID = bb.getInt();
    }
    
    public void SetAvatarID(int lAvatarID)
    {
        this.lAvatarID = lAvatarID;
        Changed();
    }
    
    public int GetID() { return lID; }
    
    public void SetName(String strName)
    {
        this.strName = strName;
        Changed();
    }
    
    public String GetName() { return strName; }
    
    public void SetDescription(String strDescription)
    {
        this.strDescription = strDescription;
        Changed();
    }
    
    public String GetDescription() { return strDescription; }
    
    public int GetAvatarID() { return lAvatarID; }
    
    public void SetListener(LaunchEntityListener listener) { this.listener = listener; }
    
    private void Changed()
    {
        if(listener != null)
        {
            listener.EntityChanged(this);
        }
    }
    
    public byte[] GetData()
    {
        ByteBuffer bb = ByteBuffer.allocate(DATA_SIZE + LaunchUtilities.GetStringDataSize(strName) + LaunchUtilities.GetStringDataSize(strDescription));
        
        bb.putInt(lID);
        bb.put(LaunchUtilities.GetStringData(strName));
        bb.put(LaunchUtilities.GetStringData(strDescription));
        bb.putInt(lAvatarID);
        
        return bb.array();
    }
}
