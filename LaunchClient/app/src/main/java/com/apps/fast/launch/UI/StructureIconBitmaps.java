package com.apps.fast.launch.UI;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;

import com.apps.fast.launch.R;

import launch.game.LaunchClientGame;
import launch.game.LaunchGame;
import launch.game.entities.MissileSite;
import launch.game.entities.OreMine;
import launch.game.entities.SAMSite;
import launch.game.entities.SentryGun;
import launch.game.entities.Structure;

import launch.game.LaunchGame.Allegiance;

/**
 * A utility class that shades structure bitmaps by allegiance, combines the power status, and caches the bitmaps in RAM.
 */
public class StructureIconBitmaps
{
    private enum StructureIndexRunStatus
    {
        ONLINE,
        OFFLINE,
        BOOTING,
        SELLING
    }

    private static final Bitmap[] MissileSiteBitmaps = new Bitmap[Allegiance.values().length * StructureIndexRunStatus.values().length];
    private static final Bitmap[] NuclearMissileSiteBitmaps = new Bitmap[Allegiance.values().length * StructureIndexRunStatus.values().length];
    private static final Bitmap[] SAMSiteBitmaps = new Bitmap[Allegiance.values().length * StructureIndexRunStatus.values().length];
    private static final Bitmap[] SentryGunBitmaps = new Bitmap[Allegiance.values().length * StructureIndexRunStatus.values().length];
    private static final Bitmap[] OreMineBitmaps = new Bitmap[Allegiance.values().length * StructureIndexRunStatus.values().length];

    private static void GenerateStructureBitmap(Context context, Bitmap[] Container, int lIndex, Allegiance allegiance, StructureIndexRunStatus runStatus, int lRes)
    {
        Bitmap icon = LaunchUICommon.TintBitmap(BitmapFactory.decodeResource(context.getResources(), lRes), LaunchUICommon.AllegianceColours[allegiance.ordinal()]);
        Bitmap overlay = null;

        switch(runStatus)
        {
            case SELLING: overlay = BitmapFactory.decodeResource(context.getResources(), R.drawable.overlay_selling); break;
            case OFFLINE: overlay = BitmapFactory.decodeResource(context.getResources(), R.drawable.overlay_offline); break;
            case BOOTING: overlay = BitmapFactory.decodeResource(context.getResources(), R.drawable.overlay_booting); break;
            case ONLINE: overlay = BitmapFactory.decodeResource(context.getResources(), R.drawable.overlay_online); break;
        }

        Bitmap bitmap = Bitmap.createBitmap(icon.getWidth(), icon.getHeight(), icon.getConfig());
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(icon, new Matrix(), null);
        canvas.drawBitmap(overlay, 0, 0, null);

        Container[lIndex] = bitmap;
    }

    public static Bitmap GetStructureBitmap(Context context, LaunchClientGame game, Structure structure)
    {
        Allegiance allegiance = game.GetAllegiance(game.GetOurPlayer(), structure);
        StructureIndexRunStatus runStatus;

        if(structure.GetSelling())
            runStatus = StructureIndexRunStatus.SELLING;
        else if(structure.GetOffline())
            runStatus = StructureIndexRunStatus.OFFLINE;
        else if(structure.GetBooting())
            runStatus = StructureIndexRunStatus.BOOTING;
        else
            runStatus = StructureIndexRunStatus.ONLINE;

        int lIndex = (allegiance.ordinal() * StructureIndexRunStatus.values().length) + runStatus.ordinal();

        if(structure instanceof MissileSite)
        {
            if(((MissileSite)structure).CanTakeNukes())
            {
                if (NuclearMissileSiteBitmaps[lIndex] == null)
                {
                    GenerateStructureBitmap(context, NuclearMissileSiteBitmaps, lIndex, allegiance, runStatus, R.drawable.marker_missilesitenuke);
                }

                return NuclearMissileSiteBitmaps[lIndex];
            }
            else
            {
                if (MissileSiteBitmaps[lIndex] == null)
                {
                    GenerateStructureBitmap(context, MissileSiteBitmaps, lIndex, allegiance, runStatus, R.drawable.marker_missilesite);
                }

                return MissileSiteBitmaps[lIndex];
            }
        }

        if(structure instanceof SAMSite)
        {
            if(SAMSiteBitmaps[lIndex] == null)
            {
                GenerateStructureBitmap(context, SAMSiteBitmaps, lIndex, allegiance, runStatus, R.drawable.marker_samsite);
            }

            return SAMSiteBitmaps[lIndex];
        }

        if(structure instanceof SentryGun)
        {
            if(SentryGunBitmaps[lIndex] == null)
            {
                GenerateStructureBitmap(context, SentryGunBitmaps, lIndex, allegiance, runStatus, R.drawable.marker_sentry);
            }

            return SentryGunBitmaps[lIndex];
        }

        if(structure instanceof OreMine)
        {
            if(OreMineBitmaps[lIndex] == null)
            {
                GenerateStructureBitmap(context, OreMineBitmaps, lIndex, allegiance, runStatus, R.drawable.marker_oremine);
            }

            return OreMineBitmaps[lIndex];
        }

        return null;
    }
}
