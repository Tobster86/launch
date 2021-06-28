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
public abstract class TimeDelay
{
    public void Tick(int lTime)
    {
        TickImpl(lTime);
    }
    
    public abstract boolean Expired();
    public abstract void ForceExpiry();
    public abstract void GetData(ByteBuffer bb);
    protected abstract void TickImpl(int lTime);
    
    /*private long oTimeRemaining = 0;
    
    private static final Random rand = new Random();
    
    public TimeDelay()
    {
        Set(0);
    }
    
    public TimeDelay(long oTimeMS)
    {
        Set(oTimeMS);
    }
    
    public void Set(long oTimeMS)
    {
        oTimeRemaining = oTimeMS;
    }
    
    public void SetRandomBetween(long oSmallest, long oLargest)
    {
        oTimeRemaining = oSmallest + rand.nextInt((int)(oLargest - oSmallest));
    }
    
    public void Tick(long oTickMS)
    {
        if(oTimeRemaining > 0)
        {
            oTimeRemaining -= oTickMS;
        }
    }
    
    public boolean Expired()
    {
        return oTimeRemaining <= 0;
    }
    
    public long GetRemaining()
    {
        return oTimeRemaining;
    }
    
    public int GetRemainingInt()
    {
        return (int)oTimeRemaining;
    }
    
    public void ForceExpiry()
    {
        oTimeRemaining = 0;
    }*/
}
