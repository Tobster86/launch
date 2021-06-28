package com.apps.fast.launch.views;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.UI.AvatarBitmaps;
import com.apps.fast.launch.UI.StructureIconBitmaps;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.TextUtilities;

import launch.game.GeoCoord;
import launch.game.LaunchClientGame;
import launch.game.entities.*;

/**
 * Created by tobster on 09/11/15.
 */
public class DistancedEntityView extends FrameLayout
{
    public DistancedEntityView(Context context, MainActivity activity, LaunchEntity entity, GeoCoord geoFrom, LaunchClientGame game)
    {
        super(context);

        inflate(context, R.layout.view_distanced_entity, this);

        Setup(activity, entity, game);

        TextView txtLocation = findViewById(R.id.txtLocation);

        float fltDistance = geoFrom.DistanceTo(entity.GetPosition());
        double dblDirection = geoFrom.BearingTo(entity.GetPosition());

        txtLocation.setText(TextUtilities.GetDistanceStringFromKM(fltDistance) + " " + TextUtilities.QualitativeDirectionFromBearing(dblDirection));
    }

    public DistancedEntityView(Context context, MainActivity activity, LaunchEntity entity, LaunchClientGame game)
    {
        super(context);

        inflate(context, R.layout.view_distanced_entity, this);

        Setup(activity, entity, game);

        findViewById(R.id.txtLocation).setVisibility(GONE);
    }

    void Setup(MainActivity activity, LaunchEntity entity, LaunchClientGame game)
    {
        ImageView imgType = findViewById(R.id.imgType);
        ImageView imgOwner = findViewById(R.id.imgOwner);
        TextView txtEntityName = findViewById(R.id.txtEntityName);

        Player owner = null;

        if(entity instanceof Player)
        {
            imgType.setImageBitmap(AvatarBitmaps.GetPlayerAvatar(activity, game, (Player)entity));
            txtEntityName.setText(((Player)entity).GetName());
        }
        else if(entity instanceof MissileSite)
        {
            imgType.setImageBitmap(StructureIconBitmaps.GetStructureBitmap(activity, game, (Structure)entity));
            txtEntityName.setText(TextUtilities.GetEntityTypeAndName(entity, game));
            owner = game.GetPlayer(((Structure)entity).GetOwnerID());
        }
        else if(entity instanceof SAMSite)
        {
            imgType.setImageResource(R.drawable.icon_sam);
            txtEntityName.setText(TextUtilities.GetEntityTypeAndName(entity, game));
            owner = game.GetPlayer(((Structure)entity).GetOwnerID());
        }
        else if(entity instanceof SentryGun)
        {
            imgType.setImageResource(R.drawable.icon_sentrygun);
            txtEntityName.setText(TextUtilities.GetEntityTypeAndName(entity, game));
            owner = game.GetPlayer(((Structure)entity).GetOwnerID());
        }
        else if(entity instanceof OreMine)
        {
            imgType.setImageResource(R.drawable.icon_oremine);
            txtEntityName.setText(TextUtilities.GetEntityTypeAndName(entity, game));
            owner = game.GetPlayer(((Structure)entity).GetOwnerID());
        }
        else if(entity instanceof Missile)
        {
            imgType.setImageResource(R.drawable.marker_missile);
            txtEntityName.setText(TextUtilities.GetEntityTypeAndName(entity, game));
            owner = game.GetPlayer(((Missile)entity).GetOwnerID());
        }
        else if(entity instanceof Interceptor)
        {
            imgType.setImageResource(R.drawable.marker_interceptor);
            txtEntityName.setText(TextUtilities.GetEntityTypeAndName(entity, game));
            owner = game.GetPlayer(((Interceptor)entity).GetOwnerID());
        }
        else if(entity instanceof Loot)
        {
            imgType.setImageResource(R.drawable.marker_loot);
            txtEntityName.setText(((Loot)entity).GetDescription());
        }
        else
        {
            imgType.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.todo));
            txtEntityName.setText(String.format("Support for %s not implemented!", entity.getClass().getName()));
        }

        if(owner != null)
        {
            imgOwner.setImageBitmap(AvatarBitmaps.GetPlayerAvatar(activity, game, owner));
        }
    }
}
