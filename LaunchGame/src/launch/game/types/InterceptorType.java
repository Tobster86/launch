/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.game.types;

import java.nio.ByteBuffer;

/**
 *
 * @author tobster
 */
public class InterceptorType extends LaunchType
{
    private static final int DATA_SIZE = 2;
    
    private byte cSpeedIndex;
    private byte cRangeIndex;
    
    public InterceptorType(byte cID, boolean bPurchasable, String strName, int lAssetID, byte cSpeedIndex, byte cRangeIndex)
    {
        super(cID, bPurchasable, strName, lAssetID);
        this.cSpeedIndex = cSpeedIndex;
        this.cRangeIndex = cRangeIndex;
    }
    
    public InterceptorType(ByteBuffer bb)
    {
        super(bb);
        cSpeedIndex = bb.get();
        cRangeIndex = bb.get();
    }

    @Override
    public byte[] GetData()
    {
        byte[] cBaseData = super.GetData();
        
        ByteBuffer bb = ByteBuffer.allocate(DATA_SIZE + cBaseData.length);
        
        bb.put(cBaseData);
        bb.put(cSpeedIndex);
        bb.put(cRangeIndex);
        
        return bb.array();
    }
    
    public byte GetSpeedIndex() { return cSpeedIndex; }
    
    public byte GetRangeIndex() { return cRangeIndex; }

    @Override
    public int GetFeatureMagnitude()
    {
        return 1 + cSpeedIndex + cRangeIndex;
    }
}
