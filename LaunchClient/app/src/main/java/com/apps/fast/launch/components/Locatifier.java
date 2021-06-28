package com.apps.fast.launch.components;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.apps.fast.launch.activities.MainActivity;

import java.util.LinkedList;
import java.util.Queue;

import launch.game.Defs;
import launch.game.LaunchClientGame;
import launch.utilities.LaunchClientLocation;

/**
 * Created by tobster on 07/06/15.
 */
public class Locatifier implements LocationListener
{
    private static final String LOG_NAME = "Locatifier";

    private static final long MIN_TIME = 2000;                  //Don't update faster than two seconds.
    private static final float MIN_DISTANCE = 1.0f;             //Or for less than one meter of movement.

    private static final long IDEAL_TIME = 10000;               //Take the best location of the past ten seconds.
    private static final long MAX_TIME = 20000;                 //Or up to twenty seconds if there isn't one good enough in ten seconds.

    private static final float IMPROVEMENT_COEFFICIENT = 1.3f;  //Older locations must be 30% better than the best newer location to be taken as valid.

    public enum Quality
    {
        GOOD,
        A_BIT_OLD,
        VERY_OLD
    };

    private boolean bGPSAvailable = false;
    private boolean bNetworkAvailable = false;

    private Queue<LaunchClientLocation> History = new LinkedList<>();
    private LaunchClientLocation currentLocation = null;

    private LaunchClientGame game;
    private MainActivity activity;
    private LocationManager locationManager;

    public Locatifier(Context context, LaunchClientGame game, MainActivity activity)
    {
        this.game = game;
        this.activity = activity;
        locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);

        bGPSAvailable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        bNetworkAvailable = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public void Resume()
    {
        if(bGPSAvailable)
        {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, this);
        }

        if(bNetworkAvailable)
        {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME, MIN_DISTANCE, this);
        }
    }

    public void Suspend()
    {
        locationManager.removeUpdates(this);
    }

    private void AddLocation(LaunchClientLocation location)
    {
        long oTheTime = System.currentTimeMillis();

        //Add to location queue.
        History.offer(location);

        //Prune old locations.
        while(History.size() > 0)
        {
            LaunchClientLocation oldLocation = History.peek();

            if(oldLocation.GetTime() < oTheTime - MAX_TIME)
                History.poll();
            else
                break;
        }

        //Manage use of the best recent location.
        if(currentLocation == null)
        {
            //No previous location available. Just take this one.
            currentLocation = location;

            //Notify the activity for first zoom on player marker if the location is good enough.
            if(game.GetConfig() == null)
            {
                activity.LocationsUpdated();
            }
            else if(location.GetAccuracy() <= game.GetConfig().GetRequiredAccuracy() * Defs.METRES_PER_KM)
            {
                activity.LocationsUpdated();
            }
        }
        else if(game.GetConfig() == null)
        {
            //Previous location available, but no game config available. Take this one if it's any better.
            if(location.GetAccuracy() <= currentLocation.GetAccuracy())
            {
                currentLocation = location;
                activity.LocationsUpdated();
            }
        }
        else if(location.GetAccuracy() <= game.GetConfig().GetRequiredAccuracy() * Defs.METRES_PER_KM)
        {
            //Accept the new location if it's either better or within 30% of the current location accuracy, or the current location is old.
            if(((location.GetAccuracy() < (currentLocation.GetAccuracy() * IMPROVEMENT_COEFFICIENT))) || (currentLocation.GetTime() < oTheTime - IDEAL_TIME))
            {
                currentLocation = location;
                activity.LocationsUpdated();
            }
        }
    }

    public LaunchClientLocation GetLocation()
    {
        return currentLocation;
    }

    public boolean GetCurrentLocationGood()
    {
        if(currentLocation == null || game.GetConfig() == null)
        {
            //We must have both a config and a good location to proceed.
            return false;
        }

        //Otherwise just assess the current location against the stored config required accuracy.
        return currentLocation.GetAccuracy() <= game.GetConfig().GetRequiredAccuracy() * Defs.METRES_PER_KM;
    }

    public Quality GetLocationQuality()
    {
        long oTheTime = System.currentTimeMillis();

        if(currentLocation != null)
        {
            if (currentLocation.GetTime() >= oTheTime - IDEAL_TIME)
                return Quality.GOOD;
            else if (currentLocation.GetTime() >= oTheTime - MAX_TIME)
                return Quality.A_BIT_OLD;
        }

        return Quality.VERY_OLD;
    }

    public Queue<LaunchClientLocation> GetHistory() { return History; }

    public boolean GetNetworkAvailable() { return bNetworkAvailable; }
    public boolean GetGPSAvailable() { return bGPSAvailable; }

    @Override
    public void onLocationChanged(Location location)
    {
        if(!location.isFromMockProvider())
        {
            AddLocation(new LaunchClientLocation(location.getLatitude(), location.getLongitude(), location.getAccuracy(), location.getProvider().toUpperCase()));
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle)
    {

    }

    @Override
    public void onProviderEnabled(String s)
    {

    }

    @Override
    public void onProviderDisabled(String s)
    {

    }
}
