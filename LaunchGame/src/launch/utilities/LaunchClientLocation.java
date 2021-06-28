package launch.utilities;

import java.nio.ByteBuffer;
import launch.game.GeoCoord;

/**
 * Created by tobster on 10/09/17.
 */

public class LaunchClientLocation
{
    public static final int FLAG_GPS = 0x01;
    public static final int FLAG_NETWORK = 0x02;
    public static final int FLAG_UNKNOWN = 0x04;
    public static final int FLAG_PRIVACY_ZONE = 0x08;
    
    private GeoCoord geoLocation;
    private long oTime;
    private float fltAccuracy;
    private String strProvider;
    private byte cFlags = 0x00;

    public LaunchClientLocation(double dblLatitudeDeg, double dblLongitudeDeg, float fltAccuracy, String strProvider)
    {
        geoLocation = new GeoCoord(dblLatitudeDeg, dblLongitudeDeg, true);
        this.oTime = System.currentTimeMillis();
        this.fltAccuracy = fltAccuracy;
        this.strProvider = strProvider;
        
        switch(strProvider)
        {
            case "GPS": SetGPS(); break;
            case "NETWORK": SetNetwork(); break;
            default: SetUnknown(); break;
        }
    }

    public LaunchClientLocation(ByteBuffer bb)
    {
        geoLocation = new GeoCoord(bb.getFloat(), bb.getFloat());
        oTime = bb.getLong();
        fltAccuracy = bb.getFloat();
        cFlags = bb.get();
    }
    
    public GeoCoord GetGeoCoord() { return geoLocation; }
    
    public float GetLatitude() { return geoLocation.GetLatitude(); }
    
    public float GetLongitude() { return geoLocation.GetLongitude(); }

    public long GetTime() { return oTime; }

    public float GetAccuracy() { return fltAccuracy; }

    public String GetProvider() { return strProvider; }
    
    public byte GetFlags() { return cFlags; }
    
    private void SetGPS()
    {
        cFlags |= FLAG_GPS;
    }
    
    private void SetNetwork()
    {
        cFlags |= FLAG_NETWORK;
    }
    
    private void SetUnknown()
    {
        cFlags |= FLAG_UNKNOWN;
    }
    
    public void SetLocationBecauseOfPrivacyZone(GeoCoord geoLocation)
    {
        this.geoLocation = geoLocation;
        cFlags |= FLAG_PRIVACY_ZONE;
    }
    
    public boolean IsGPS()
    {
        return ( cFlags & FLAG_GPS ) != 0x00;
    }
    
    public boolean IsNetwork()
    {
        return ( cFlags & FLAG_NETWORK ) != 0x00;
    }
    
    public boolean IsUnknown()
    {
        return ( cFlags & FLAG_UNKNOWN ) != 0x00;
    }
    
    public boolean InPrivacyZone()
    {
        return ( cFlags & FLAG_PRIVACY_ZONE ) != 0x00;
    }
    
    public byte[] GetData()
    {
        ByteBuffer bb = ByteBuffer.allocate(21);
        bb.putFloat(GetLatitude());
        bb.putFloat(GetLongitude());
        bb.putLong(GetTime());
        bb.putFloat(GetAccuracy());
        bb.put(GetFlags());
        return bb.array();
    }
    
    public String GetLocationTypeName()
    {
        StringBuilder strbResult = new StringBuilder();

        if(IsGPS())
            strbResult.append("GPS");
        else if(IsNetwork())
            strbResult.append("Network");
        else
            strbResult.append("Unknown");

        if(InPrivacyZone())
            strbResult.append( "(P)");
        
        return strbResult.toString();
    }
}
