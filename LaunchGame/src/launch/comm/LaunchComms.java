/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.comm;

/**
 *
 * @author tobster
 */
public abstract class LaunchComms
{
    //***The server address and port have moved into config and app settings!***
    
    protected static final String LOG_NAME = "Comms";
    
    public abstract void Tick(int lMS);
    
    public abstract void InterruptAll();
}
