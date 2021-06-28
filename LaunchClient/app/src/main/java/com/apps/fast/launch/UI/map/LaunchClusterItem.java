package com.apps.fast.launch.UI.map;

import com.apps.fast.launch.components.Utilities;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

import launch.game.entities.LaunchEntity;

public class LaunchClusterItem implements ClusterItem
{
    private LatLng latLng;
    private int lID;
    private LaunchEntity entity;

    public LaunchClusterItem(LaunchEntity entity)
    {
        lID = entity.GetID();
        latLng = Utilities.GetLatLng(entity.GetPosition());
        this.entity = entity;
    }

    public int GetID() { return lID; }

    public LaunchEntity GetEntity() { return entity; }

    @Override
    public LatLng getPosition()
    {
        return latLng;
    }

    @Override
    public String getTitle()
    {
        return null;
    }

    @Override
    public String getSnippet()
    {
        return null;
    }
}
