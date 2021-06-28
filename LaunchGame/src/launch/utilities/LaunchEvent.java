/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.utilities;

import java.nio.ByteBuffer;

/**
 *
 * @author tobster
 */
public class LaunchEvent
{
    public enum SoundEffect
    {
        NONE,
        EXPLOSION,          //Client has 'near' and 'far' explosions, and will select according to whether it's 'done to' 'its' player or not.
        MONEY,
        MISSILE_LAUNCH,
        INTERCEPTOR_LAUNCH,
        CONSTRUCTION,
        INTERCEPTOR_MISS,
        INTERCEPTOR_HIT,
        SENTRY_GUN_HIT,
        SENTRY_GUN_MISS,
        RECONFIG,
        EQUIP,
        RESPAWN,
        DEATH,
        REPAIR,
        HEAL
    }
    
    private final static int DATA_SIZE = 9;
    
    private long oTime = System.currentTimeMillis();
    private String strMessage;
    private SoundEffect soundEffect;
    
    public LaunchEvent(String strMessage)
    {
        //A really basic event.
        this.strMessage = strMessage;
        this.soundEffect = SoundEffect.NONE;
    }
    
    public LaunchEvent(String strMessage, SoundEffect soundEffect)
    {
        //A basic event with a sound effect.
        this.strMessage = strMessage;
        this.soundEffect = soundEffect;
    }
    
    public LaunchEvent(ByteBuffer bb)
    {
        oTime = bb.getLong();
        strMessage = LaunchUtilities.StringFromData(bb);
        soundEffect = SoundEffect.values()[bb.get()];
    }
    
    public byte[] GetData()
    {
        ByteBuffer bb = ByteBuffer.allocate(DATA_SIZE + LaunchUtilities.GetStringDataSize(strMessage));
        bb.putLong(oTime);
        bb.put(LaunchUtilities.GetStringData(strMessage));
        bb.put((byte)soundEffect.ordinal());
        return bb.array();
    }
    
    public long GetTime() { return oTime; }
    public String GetMessage() { return strMessage; }
    public SoundEffect GetSoundEffect() { return soundEffect; }
}
