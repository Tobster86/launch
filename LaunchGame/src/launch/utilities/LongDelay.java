/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.utilities;

import java.nio.ByteBuffer;

/**
 * For time delays that could be greater than 24 days but less than 292 million years in milliseconds (sint64).
 */
public class LongDelay extends TimeDelay
{
    private long oTimeRemaining = 0;
    
    public LongDelay()
    {
        oTimeRemaining = 0;
    }
    
    public LongDelay(long oTime)
    {
        oTimeRemaining = oTime;
    }
    
    public LongDelay(ByteBuffer bb)
    {
        oTimeRemaining = bb.getLong();
    }
    
    public void Set(long oTimeMS)
    {
        oTimeRemaining = oTimeMS;
    }
    
    public long GetRemaining()
    {
        return oTimeRemaining;
    }

    @Override
    public boolean Expired()
    {
        return oTimeRemaining <= 0;
    }

    @Override
    public void ForceExpiry()
    {
        oTimeRemaining = 0;
    }

    @Override
    public void GetData(ByteBuffer bb)
    {
        bb.putLong(oTimeRemaining);
    }

    @Override
    protected void TickImpl(int lTime)
    {
        oTimeRemaining -= lTime;
        
        if(oTimeRemaining < 0)
            oTimeRemaining = 0;
    }
}
