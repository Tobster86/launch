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
public class MissileType extends LaunchType
{
    private static final int FEATURE_MAGNITUDE_NUKE = 5;
    private static final int FEATURE_MAGNITUDE_TRACKING = 2;
    private static final int FEATURE_MAGNITUDE_ECM = 2;
    
    private static final int DATA_SIZE = 7;
    
    private boolean bNuclear;
    private boolean bTracking;
    private boolean bECM;
    private byte cSpeedIndex;
    private byte cRangeIndex;
    private byte cBlastRadiusIndex;
    private byte cMaxDamageIndex;
    
    public MissileType(byte cID, boolean bPurchasable, String strName, int lAssetID, boolean bNuclear, boolean bTracking, boolean bECM, byte cSpeedIndex, byte cRangeIndex, byte cBlastRadiusIndex, byte cMaxDamageIndex)
    {
        super(cID, bPurchasable, strName, lAssetID);
        this.bNuclear = bNuclear;
        this.bTracking = bTracking;
        this.bECM = bECM;
        this.cSpeedIndex = cSpeedIndex;
        this.cRangeIndex = cRangeIndex;
        this.cBlastRadiusIndex = cBlastRadiusIndex;
        this.cMaxDamageIndex = cMaxDamageIndex;
    }
    
    public MissileType(ByteBuffer bb)
    {
        super(bb);
        bNuclear = (bb.get() != 0x00);
        bTracking = (bb.get() != 0x00);
        bECM = (bb.get() != 0x00);
        cSpeedIndex = bb.get();
        cRangeIndex = bb.get();
        cBlastRadiusIndex = bb.get();
        cMaxDamageIndex = bb.get();
    }

    @Override
    public byte[] GetData()
    {
        byte[] cBaseData = super.GetData();
        
        ByteBuffer bb = ByteBuffer.allocate(DATA_SIZE + cBaseData.length);
        bb.put(cBaseData);
        bb.put((byte)(bNuclear? 0xFF : 0x00));
        bb.put((byte)(bTracking? 0xFF : 0x00));
        bb.put((byte)(bECM? 0xFF : 0x00));
        bb.put(cSpeedIndex);
        bb.put(cRangeIndex);
        bb.put(cBlastRadiusIndex);
        bb.put(cMaxDamageIndex);
        
        return bb.array();
    }
    
    public boolean GetNuclear() { return bNuclear; }
    
    public boolean GetTracking() { return bTracking; }
    
    public boolean GetECM() { return bECM; }
    
    public byte GetSpeedIndex() { return cSpeedIndex; }
    
    public byte GetRangeIndex() { return cRangeIndex; }
    
    public byte GetBlastRadiusIndex() { return cBlastRadiusIndex; }
    
    public byte GetMaxDamageIndex() { return cMaxDamageIndex; }

    @Override
    public int GetFeatureMagnitude()
    {
        int lResult = 1 + cSpeedIndex + cRangeIndex + cBlastRadiusIndex + cMaxDamageIndex;
        lResult += bNuclear? FEATURE_MAGNITUDE_NUKE : 0;
        lResult += bTracking? FEATURE_MAGNITUDE_TRACKING : 0;
        lResult += bECM? FEATURE_MAGNITUDE_ECM : 0;
        return lResult;
    }
}
