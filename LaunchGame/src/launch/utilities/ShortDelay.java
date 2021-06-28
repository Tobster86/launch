/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.utilities;

import java.nio.ByteBuffer;

/**
 * For time delays under 24 days in milliseconds (sint32).
 */
public class ShortDelay extends TimeDelay
{
    private int lTimeRemaining = 0;
    
    public ShortDelay()
    {
        lTimeRemaining = 0;
    }
    
    public ShortDelay(int lTime)
    {
        lTimeRemaining = lTime;
    }
    
    public ShortDelay(ByteBuffer bb)
    {
        lTimeRemaining = bb.getInt();
    }
    
    public void Set(int lTimeMS)
    {
        lTimeRemaining = lTimeMS;
    }
    
    public int GetRemaining()
    {
        return lTimeRemaining;
    }

    @Override
    public boolean Expired()
    {
        return lTimeRemaining <= 0;
    }

    @Override
    public void ForceExpiry()
    {
        lTimeRemaining = 0;
    }

    @Override
    public void GetData(ByteBuffer bb)
    {
        bb.putInt(lTimeRemaining);
    }

    @Override
    protected void TickImpl(int lTime)
    {
        lTimeRemaining -= lTime;
        
        if(lTimeRemaining < 0)
            lTimeRemaining = 0;
    }
}
