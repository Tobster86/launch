/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.utilities;

import launch.game.Defs;
import launch.game.entities.Player;

/**
 *
 * @author tobster
 */
public class LocationSpoofCheck
{
    public enum Type
    {
        FIRST,  //First location.
        PREV,   //Same as previous location.
        OK,     //Appears legitimate.
        SPOOF   //Appears to be a spoof.
    }
    
    private Type type;
    
    private LaunchClientLocation oldLocation;
    private LaunchClientLocation newLocation;
    
    private float fltDistance;
    private long oTime;
    private float fltSpeed;
    
    public LocationSpoofCheck(LaunchClientLocation oldLocation, LaunchClientLocation newLocation)
    {
        this.oldLocation = oldLocation;
        this.newLocation = newLocation;
        
        if(oldLocation != null) //Don't check if we don't have an old location to compare.
        {
            fltDistance = newLocation.GetGeoCoord().DistanceTo(oldLocation.GetGeoCoord());
            oTime = newLocation.GetTime() - oldLocation.GetTime();
            fltSpeed = fltDistance / ((float)(oTime) / Defs.MS_PER_HOUR_FLT);
            
            if(oTime == 0)
            {
                type = Type.PREV;
            }
            else if(fltSpeed > Defs.LOCATION_SPOOF_SUSPECT_SPEED && fltDistance > Defs.LOCATION_SPOOF_SUSPECT_DISTANCE)
            {
                //Location spoof was registered.
                type = Type.SPOOF;
            }
            else
            {
                type = Type.OK;
            }
        }
        else
        {
            type = Type.FIRST;
        }
    }
    
    public Type GetType() { return type; }
    
    public float GetDistance() { return fltDistance; }
    public long GetTime() { return oTime; }
    public float GetSpeed() { return fltSpeed; }
}
