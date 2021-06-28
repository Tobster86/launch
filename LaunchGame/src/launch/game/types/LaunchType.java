/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.game.types;

import java.nio.ByteBuffer;
import launch.utilities.LaunchUtilities;

/**
 *
 * @author tobster
 */
public abstract class LaunchType
{
    private static final int DATA_SIZE = 6;
    
    public static final byte ASSET_ID_DEFAULT = -1;
    
    /**
     * Types have attributes that can either be determined from wider game settings, or be type-specific, in which case the corresponding index property is this value.
     */
    public static final byte INDEX_TYPE_OVERRIDE = -1;
    
    private byte cID;
    private boolean bPurchasable;
    private String strName;
    private int lAssetID;
    
    public LaunchType(byte cID, boolean bPurchasable, String strName, int lAssetID)
    {
        this.cID = cID;
        this.bPurchasable = bPurchasable;
        this.strName = strName;
        this.lAssetID = lAssetID;
    }
    
    public LaunchType(ByteBuffer bb)
    {
        cID = bb.get();
        bPurchasable = bb.get() != 0x00;
        strName = LaunchUtilities.StringFromData(bb);
        lAssetID = bb.getInt();
    }
    
    public byte GetID() { return cID; }
    public String GetName() { return strName; }
    public boolean GetPurchasable() { return bPurchasable; }
    public int GetAssetID() { return lAssetID; }
    
    public byte[] GetData()
    {
        ByteBuffer bb = ByteBuffer.allocate(DATA_SIZE + LaunchUtilities.GetStringDataSize(strName));
        
        bb.put(cID);
        bb.put((byte)(bPurchasable ? 0xFF : 0x00));
        bb.put(LaunchUtilities.GetStringData(strName));
        bb.putInt(lAssetID);
        
        return bb.array();
    }
    
    /**
     * The "magnitude" of type features, used to scale the prep times.
     * @return Ideally the sum of all property indices.
     */
    public abstract int GetFeatureMagnitude();
}
