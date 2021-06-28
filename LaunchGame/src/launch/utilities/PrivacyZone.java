/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.utilities;

import java.nio.ByteBuffer;
import launch.game.GeoCoord;

/**
 *
 * @author tobster
 */
public class PrivacyZone
{
    private static final int DATA_SIZE = 12;

    private GeoCoord geoPosition;
    private float fltRadius;

    public PrivacyZone(GeoCoord geoPosition, float fltRadius)
    {
        this.geoPosition = geoPosition;
        this.fltRadius = fltRadius;
    }

    public PrivacyZone(ByteBuffer bb)
    {
        geoPosition = new GeoCoord(bb.getFloat(), bb.getFloat());
        fltRadius = bb.getFloat();
    }

    public byte[] GetBytes()
    {
        ByteBuffer bb = ByteBuffer.allocate(DATA_SIZE);
        bb.putFloat(geoPosition.GetLatitude());
        bb.putFloat(geoPosition.GetLongitude());
        bb.putFloat(fltRadius);
        return bb.array();
    }

    public GeoCoord GetPosition()
    {
        return geoPosition;
    }

    public float GetRadius()
    {
        return fltRadius;
    }

    public void SetRadius(float fltRadius)
    {
        this.fltRadius = fltRadius;
    }
}
