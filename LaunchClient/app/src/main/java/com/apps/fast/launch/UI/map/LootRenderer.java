package com.apps.fast.launch.UI.map;

import android.content.Context;

import com.apps.fast.launch.R;
import com.apps.fast.launch.components.ClientDefs;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

public class LootRenderer extends DefaultClusterRenderer<LaunchClusterItem>
{
    public LootRenderer(Context context, GoogleMap map, ClusterManager clusterManager)
    {
        super(context, map, clusterManager);
    }

    @Override
    protected void onBeforeClusterItemRendered(LaunchClusterItem item, MarkerOptions markerOptions)
    {
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_loot));
        markerOptions.anchor(0.5f, 0.5f);
    }

    @Override
    protected void onBeforeClusterRendered(Cluster<LaunchClusterItem> cluster, MarkerOptions markerOptions)
    {
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.cluster_loot));
    }

    @Override
    protected boolean shouldRenderAsCluster(Cluster<LaunchClusterItem> cluster)
    {
        if(ClientDefs.CLUSTERING_SIZE <= 0)
            return false;
        return cluster.getSize() >= ClientDefs.CLUSTERING_SIZE;
    }
}
