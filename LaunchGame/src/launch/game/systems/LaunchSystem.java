/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.game.systems;

import java.nio.ByteBuffer;

/**
 *
 * @author tobster
 */
public abstract class LaunchSystem
{
    private LaunchSystemListener listener = null;
    
    public LaunchSystem()
    {
        //For saved entities. System listener must be assigned later.
    }
    
    public LaunchSystem(LaunchSystemListener listener)
    {
        this.listener = listener;
    }
    
    public void SetSystemListener(LaunchSystemListener listener)
    {
        this.listener = listener;
    }
    
    public abstract void Tick(int lMS);
    
    public abstract byte[] GetData();
    
    protected final void Changed()
    {
        listener.SystemChanged(this);
    }
}
