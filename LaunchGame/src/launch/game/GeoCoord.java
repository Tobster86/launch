/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.game;

import java.util.Random;

/**
 *
 * @author tobster
 */
public class GeoCoord
{
    private static final double UNSET_THRESHOLD = -100000.0f;
    private static final double UNSET = UNSET_THRESHOLD * 10.0f;
    
    private static final double DEGREES_IN_RADIANS_1 = ((Math.PI * 2.0) / 360.0);
    private static final double DEGREES_IN_RADIANS_5 = ((Math.PI * 2.0) / 72.0);
    private static final double DEGREES_IN_RADIANS_60 = ((Math.PI * 2.0) / 6.0);
    private static final double DEGREES_IN_RADIANS_359 = DEGREES_IN_RADIANS_1 * 359.0;
    private static final double DEGREES_IN_RADIANS_355 = DEGREES_IN_RADIANS_1 * 355.0;
    
    private static final Random random = new Random();
    
    private double dblLatitude;
    private double dblLongitude;
    
    private float fltLastBearing;
    
    private static final int INTERCEPT_RECALC_TICKS = 60;
    private int lInterceptRecalcTicks;
    private GeoCoord geoIntercept;
    
    public GeoCoord()
    {
        dblLatitude = UNSET;
        dblLongitude = UNSET;
    }
    
    public GeoCoord(float fltLatitudeDeg, float fltLongitudeDeg)
    {
        dblLatitude = Math.toRadians(fltLatitudeDeg);
        dblLongitude = Math.toRadians(fltLongitudeDeg);
    }
    
    public GeoCoord(GeoCoord geoCopy)
    {
        dblLatitude = geoCopy.dblLatitude;
        dblLongitude = geoCopy.dblLongitude;
    }
    
    public GeoCoord(GeoCoord geoFrom, float fltRandomDeviance)
    {
        dblLatitude = geoFrom.dblLatitude;
        dblLongitude = geoFrom.dblLongitude;
        Move(random.nextDouble() * (2.0 * Math.PI), random.nextDouble() * fltRandomDeviance);
    }

    public GeoCoord(double dblLatitude, double dblLongitude, boolean bDegrees)
    {
        if(bDegrees)
        {
            this.dblLatitude = Math.toRadians(dblLatitude);
            this.dblLongitude = Math.toRadians(dblLongitude);
        }
        else
        {
            this.dblLatitude = dblLatitude;
            this.dblLongitude = dblLongitude;
        }
    }
    
    private GeoCoord(double dblLatitudeRad, double dblLongitudeRad)
    {
        dblLatitude = dblLatitudeRad;
        dblLongitude = dblLongitudeRad;
    }
    
    public GeoCoord GetCopy()
    {
        return new GeoCoord(this);
    }
    
    //Physically move toward a location with a given speed. Return a boolean indicating if the destination was reached.
    public boolean MoveToward(GeoCoord geoDestination, double dblSpeedKPH, long oTimeMS)
    {
        double dblDistance = (dblSpeedKPH / Defs.MS_PER_HOUR_DBL) * (double)oTimeMS;
        return MoveToward(geoDestination, dblDistance);
    }
    
    /**
     * Physically move toward a location by an amount.
     * @param geoDestination Other GeoLocation to move towards.
     * @param dblDistance Distance to move, in km.
     * @return Boolean indicating if the destination was reached.
     */
    public boolean MoveToward(GeoCoord geoDestination, double dblDistance)
    {
        double dblDistanceTo = DistanceTo(geoDestination);
        
        if(dblDistance < dblDistanceTo)
        {
            double dblBearing = BearingTo(geoDestination);
            Move(dblBearing, dblDistance);
            return false;
        }
        
        dblLatitude = geoDestination.dblLatitude;
        dblLongitude = geoDestination.dblLongitude;
        return true;
    }
    
    public boolean MoveToIntercept(double dblSpeedKPH, GeoCoord geoMissile, double dblMissileSpeedKPH, GeoCoord geoTarget, long oTimeMS)
    {
        lInterceptRecalcTicks--;
        
        if(lInterceptRecalcTicks <= 0)
        {
            geoIntercept = geoMissile.GetCopy();
            
            double dblMissileTime = 0;
            double dblInterceptorTime = Float.MAX_VALUE;
            
            while(dblMissileTime < dblInterceptorTime)
            {
                if(geoIntercept.MoveToward(geoTarget, dblMissileSpeedKPH, oTimeMS))
                {
                    //Prevent infinite loop if it hits the target.
                    break;
                }

                dblMissileTime = geoMissile.DistanceTo(geoIntercept) / dblMissileSpeedKPH;
                dblInterceptorTime = DistanceTo(geoIntercept) / dblSpeedKPH;
            }
            
            lInterceptRecalcTicks = INTERCEPT_RECALC_TICKS;
        }
        
        return MoveToward(geoIntercept, dblSpeedKPH, oTimeMS);
    }
    
    public GeoCoord InterceptPoint(GeoCoord geoDestination, float fltSpeed, GeoCoord geoInterceptor, float fltInterceptorSpeed)
    {
        //TO DO: We're back to heap of wank maths again until something better is figured out.
        float fltTime = 0;
        float fltInterceptorTime = Float.MAX_VALUE;
        GeoCoord geoInterceptPoint = GetCopy();
        
        while(fltTime < fltInterceptorTime)
        {
            if(geoInterceptPoint.MoveToward(geoDestination, fltSpeed, 1000))
            {
                //Prevent infinite loop.
                break;
            }
            
            fltTime = DistanceTo(geoInterceptPoint) / fltSpeed;
            fltInterceptorTime = geoInterceptor.DistanceTo(geoInterceptPoint) / fltInterceptorSpeed;
        }
        
        return geoInterceptPoint;
        
        /*float fltTimeToTarget = (DistanceTo(geoDestination) / fltSpeed);
        float fltInterceptorTimeToTarget = (geoInterceptor.DistanceTo(geoDestination) / fltInterceptorSpeed);
        float fltDistanceAlong = (fltInterceptorTimeToTarget / fltTimeToTarget) * (DistanceTo(geoDestination));
        
        GeoCoord geoIntercept = GetCopy();
        geoIntercept.MoveToward(geoDestination, fltDistanceAlong);*/
        
        //TO DO: Optimisation. We've already worked out the cross track once, and currently calculate it again to work out the along track.
        
        /*float fltCrossTrack = geoInterceptor.CrossTrackDistanceTo(this, geoDestination);
        float fltAlongTrack = geoInterceptor.AlongTrackDistance(this, geoDestination);*/
        
        /*float fltBearing = (float)Math.asin((fltSpeed * Math.sin(fltLastBearing)) / fltInterceptorSpeed);
        
        float fltTime = (float)(DistanceTo(geoInterceptor) / ((fltInterceptorSpeed * Math.cos(fltBearing)) + (fltSpeed * Math.cos(fltLastBearing))));
        
        float fltDistance = fltTime * fltSpeed;
        
        GeoCoord geoIntercept = GetCopy();
        geoIntercept.MoveToward(geoDestination, fltDistance);*/
        
        /*float fltMissileTimeToIntercept = DistanceTo(geoIntercept) / fltSpeed;
        float fltInterceptorTimeToIntercept = geoInterceptor.DistanceTo(geoIntercept) / fltInterceptorSpeed;
        
        return geoIntercept;*/
    }
    
    public void Move(double dblBearing, double dblDistance)
    {
        fltLastBearing = (float)dblBearing;
        
        double dblAngularDistance = dblDistance / Defs.EARTH_RADIUS_KM;
        
        double dblNewLatitude = Math.asin(Math.sin(dblLatitude) * Math.cos(dblAngularDistance) + Math.cos(dblLatitude) * Math.sin(dblAngularDistance) * Math.cos(dblBearing));
        double dblNewLongitude = dblLongitude + Math.atan2(Math.sin(dblBearing) * Math.sin(dblAngularDistance) * Math.cos(dblLatitude), Math.cos(dblAngularDistance) - Math.sin(dblLatitude) * Math.sin(dblNewLatitude));
        
        dblLatitude = dblNewLatitude;
        dblLongitude = dblNewLongitude;
    }
    
    //Return a bearing from this position to a destination.
    public double BearingTo(GeoCoord geoDestination)
    {
        double dblY = Math.sin(geoDestination.dblLongitude - dblLongitude) * Math.cos(geoDestination.dblLatitude);
        double dblX = (Math.cos(dblLatitude) * Math.sin(geoDestination.dblLatitude)) - (Math.sin(dblLatitude) * Math.cos(geoDestination.dblLatitude) * Math.cos(geoDestination.dblLongitude - dblLongitude));
        
        return (float)Math.atan2(dblY, dblX);
    }
    
    /**
     * Return the shortest distance to a destination, in km.
     * @param geoDestination Distance to this destination.
     * @return Distance in km.
     */
    public float DistanceTo(GeoCoord geoDestination)
    {
        double dblDeltaLatitude = geoDestination.dblLatitude - dblLatitude;
        double dblDeltaLongitude = geoDestination.dblLongitude - dblLongitude;
        
        double dblA = Math.sin(dblDeltaLatitude / 2) * Math.sin(dblDeltaLatitude / 2) + Math.sin(dblDeltaLongitude / 2) * Math.sin(dblDeltaLongitude / 2) * Math.cos(dblLatitude) * Math.cos(geoDestination.dblLatitude);
        double dblC = 2 * Math.asin(Math.sqrt(dblA));
        
        return (float)(Defs.EARTH_RADIUS_KM * dblC);
    }
    
    //Return the shortest distance to a destination, in degrees.
    public double AngularDistanceTo(GeoCoord geoDestination)
    {
        double dblDeltaLatitude = geoDestination.dblLatitude - dblLatitude;
        double dblDeltaLongitude = geoDestination.dblLongitude - dblLongitude;
        
        double dblA = Math.sin(dblDeltaLatitude / 2) * Math.sin(dblDeltaLatitude / 2) + Math.sin(dblDeltaLongitude / 2) * Math.sin(dblDeltaLongitude / 2) * Math.cos(dblLatitude) * Math.cos(geoDestination.dblLatitude);
        return 2 * Math.asin(Math.sqrt(dblA));
    }
    
    public float CrossTrackDistanceTo(GeoCoord geoObject, GeoCoord geoObjectDestination)
    {
        //Nearest distance from this point to the given track, in km.
        double dbl13 = geoObject.AngularDistanceTo(this);
        double dblTheta13 = geoObject.BearingTo(this);
        double dblTheta12 = geoObject.BearingTo(geoObjectDestination);
        
        return (float)(Math.asin(Math.sin(dbl13)*Math.sin(dblTheta13 - dblTheta12)) * Defs.EARTH_RADIUS_KM);
    }
    
    public float AlongTrackDistance(GeoCoord geoObject, GeoCoord geoObjectDestination)
    {
        //Distance of the nearest point on the given track, along the given track.
        double dbl13 = geoObject.AngularDistanceTo(this);
        return (float)(Math.acos(Math.cos(dbl13) / Math.cos(CrossTrackDistanceTo(geoObject, geoObjectDestination) / Defs.EARTH_RADIUS_KM)) * Defs.EARTH_RADIUS_KM);
    }
    
    public float GetLatitude()
    {
        return (float)ToDegrees(dblLatitude);
    }
    
    public float GetLongitude()
    {
        return (float)ToDegrees(dblLongitude);
    }
    
    public float GetLastBearing()
    {
        return fltLastBearing;
    }
    
    public static double ToDegrees(double dblRadians)
    {
        return Math.toDegrees(dblRadians);
    }
    
    public static double ToRadians(double dblDegrees)
    {
        return Math.toRadians(dblDegrees);
    }
    
    public static float ToMiles(float fltKilometers)
    {
        return fltKilometers * Defs.MILE_TO_KM;
    }
    
    public boolean GetValid()
    {
        return (dblLatitude > UNSET_THRESHOLD) && (dblLongitude > UNSET_THRESHOLD);
    }
    
    /**
     * Perform a broad-phase (initial efficient plausibility test) collision test, to establish if more computationally intensive calculations should be performed.
     * @param geoPoint Point to collision check against.
     * @return Boolean indicating that a collision can be plausibly tested for by more computationally intensive means.
     */
    public boolean BroadPhaseCollisionTest(GeoCoord geoPoint)
    {
        //At "uninhabited" latitudes where longitudes significantly converge, just return true as there's not much up/down there anyway.
        if(Math.abs(geoPoint.dblLatitude) > DEGREES_IN_RADIANS_60)
            return true;
        
        if(Math.abs(geoPoint.dblLatitude - dblLatitude) < DEGREES_IN_RADIANS_1)
        {
            double dblLongitudeDelta = Math.abs(geoPoint.dblLongitude - dblLongitude);

            //At other latitudes, do a simple test for adjacent whole longitudes (which are at least ~35 miles apart at +/-60deg Lat, increasing towards equator).
            if(dblLongitudeDelta < DEGREES_IN_RADIANS_1)
                return true;

            //Edge of the world check.   
            return dblLongitudeDelta > DEGREES_IN_RADIANS_359;
        }
        
        return false;
    }
    
    /**
     * Special case broad-phase collision test for things like nukes, that considers 5 degrees of longitude instead of 1.
     * @param geoPoint Point to collision check against.
     * @return Boolean indicating that a collision can be plausibly tested for by more computationally intensive means.
     */
    public boolean EvenBroaderPhaseCollisionTest(GeoCoord geoPoint)
    {
        //At "uninhabited" latitudes where longitudes significantly converge, just return true as there's not much up/down there anyway.
        if(Math.abs(geoPoint.dblLatitude) > DEGREES_IN_RADIANS_60)
            return true;
        
        if(Math.abs(geoPoint.dblLatitude - dblLatitude) < DEGREES_IN_RADIANS_5)
        {
            double dblLongitudeDelta = Math.abs(geoPoint.dblLongitude - dblLongitude);

            //At other latitudes, do a simple test for adjacent whole longitudes (which are at least ~35 miles apart at +/-60deg Lat, increasing towards equator).
            if(dblLongitudeDelta < DEGREES_IN_RADIANS_5)
                return true;

            //Edge of the world check.   
            return dblLongitudeDelta > DEGREES_IN_RADIANS_355;
        }
        
        return false;
    }
    
    /**
     * Return a boolean indicating whether this position lies between the lines of latitudes and longitudes specified.
     * Does not compensate for date line; values must be normalised accordingly.
     * @param geoSouthernWest The southwesternmost point.
     * @param geoNorthernEast To northeasternmost point.
     * @return True if the position is inside the geodesic rectangle.
     */
    public boolean IsInsideGeoRect(GeoCoord geoSouthernWest, GeoCoord geoNorthernEast)
    {
        if(dblLongitude > geoSouthernWest.dblLongitude)
            if(dblLongitude < geoNorthernEast.dblLongitude)
                if(dblLatitude > geoSouthernWest.dblLatitude)
                    if(dblLatitude < geoNorthernEast.dblLatitude)
                        return true;
        
        return false;
    }

    @Override
    public String toString()
    {
        return String.format("(%.6f, %.6f)", GetLatitude(), GetLongitude());
    }
}
