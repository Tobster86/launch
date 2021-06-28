package com.apps.fast.launch.UI.map;

import android.content.Context;

import com.apps.fast.launch.R;
import com.apps.fast.launch.UI.StructureIconBitmaps;
import com.apps.fast.launch.components.ClientDefs;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

import launch.game.LaunchClientGame;
import launch.game.entities.OreMine;

public class OreMineRenderer extends DefaultClusterRenderer<LaunchClusterItem>
{
    private LaunchClientGame game;
    private Context context;

    public OreMineRenderer(Context context, LaunchClientGame game, GoogleMap map, ClusterManager clusterManager)
    {
        super(context, map, clusterManager);
        this.context = context;
        this.game = game;
    }

    @Override
    protected void onBeforeClusterItemRendered(LaunchClusterItem item, MarkerOptions markerOptions)
    {
        OreMine oreMine = game.GetOreMine(item.GetID());

        if(oreMine != null && game != null)
        {
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(StructureIconBitmaps.GetStructureBitmap(context, game, oreMine)));
            markerOptions.anchor(0.5f, 0.5f);
            markerOptions.alpha(oreMine.GetRespawnProtected() ? 0.5f : 1.0f);
        }
    }

    @Override
    protected void onBeforeClusterRendered(Cluster<LaunchClusterItem> cluster, MarkerOptions markerOptions)
    {
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.cluster_oremine));
    }

    @Override
    protected boolean shouldRenderAsCluster(Cluster<LaunchClusterItem> cluster)
    {
        if(ClientDefs.CLUSTERING_SIZE <= 0)
            return false;
        return cluster.getSize() >= ClientDefs.CLUSTERING_SIZE;
    }
}
