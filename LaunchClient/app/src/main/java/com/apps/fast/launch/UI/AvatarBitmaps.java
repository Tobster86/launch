package com.apps.fast.launch.UI;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.apps.fast.launch.R;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.ClientDefs;

import java.io.File;
import java.util.HashMap;

import launch.game.Alliance;
import launch.game.Defs;
import launch.game.LaunchClientGame;
import launch.game.LaunchGame.Allegiance;
import launch.game.entities.Player;

public class AvatarBitmaps
{
    private static HashMap<Integer, Bitmap[]> PlayerAvatars = new HashMap<>();
    private static HashMap<Integer, Bitmap[]> AllianceAvatars = new HashMap<>();

    private static final Bitmap[] DefaultPlayerAvatars = new Bitmap[Allegiance.values().length];
    private static final Bitmap[] DefaultAllianceAvatars = new Bitmap[Allegiance.values().length];

    private static void ApplyAllegianceRing(Bitmap bitmap, int lAllegiance)
    {
        int lColour = LaunchUICommon.AllegianceColours[lAllegiance];

        int lAvatarCentre = Defs.AVATAR_SIZE / 2;
        double dblAvatarExtent = Defs.AVATAR_IMAGE / 2.0;

        for(int x = 0; x < Defs.AVATAR_SIZE; x++)
        {
            for (int y = 0; y < Defs.AVATAR_SIZE; y++)
            {
                double dblDistFromCentre = Math.sqrt(((x-lAvatarCentre)*(x-lAvatarCentre)) + ((y-lAvatarCentre)*(y-lAvatarCentre)));

                if(dblDistFromCentre > dblAvatarExtent && dblDistFromCentre < lAvatarCentre)
                {
                    bitmap.setPixel(x, y, lColour);
                }
            }
        }
    }

    private static void ApplyAllegianceSquare(Bitmap bitmap, int lAllegiance)
    {
        int lColour = LaunchUICommon.AllegianceColours[lAllegiance];

        int lNearEdge = (Defs.AVATAR_SIZE - Defs.AVATAR_IMAGE) / 2;
        int lFarEdge = Defs.AVATAR_SIZE - lNearEdge;

        for (int x = 0; x < Defs.AVATAR_SIZE; x++)
        {
            for (int y = 0; y < Defs.AVATAR_SIZE; y++)
            {
                if(x < lNearEdge || x > lFarEdge || y < lNearEdge || y > lFarEdge)
                {
                    bitmap.setPixel(x, y, lColour);
                }
            }
        }
    }

    private static Bitmap GetDefaultAvatar(Context context, int lAllegianceOrdinal, LaunchUICommon.AvatarPurpose purpose)
    {
        switch(purpose)
        {
            case PLAYER:
            {
                if(DefaultPlayerAvatars[lAllegianceOrdinal] == null)
                {
                    DefaultPlayerAvatars[lAllegianceOrdinal] = BitmapFactory.decodeResource(context.getResources(), R.drawable.marker_player).copy(Bitmap.Config.ARGB_8888 , true);
                    DefaultPlayerAvatars[lAllegianceOrdinal] = LaunchUICommon.TintBitmap(DefaultPlayerAvatars[lAllegianceOrdinal], LaunchUICommon.AllegianceColours[lAllegianceOrdinal]);
                    ApplyAllegianceRing(DefaultPlayerAvatars[lAllegianceOrdinal], lAllegianceOrdinal);
                }

                return DefaultPlayerAvatars[lAllegianceOrdinal];
            }

            case ALLIANCE:
            {
                if(DefaultAllianceAvatars[lAllegianceOrdinal] == null)
                {
                    DefaultAllianceAvatars[lAllegianceOrdinal] = BitmapFactory.decodeResource(context.getResources(), R.drawable.default_alliance).copy(Bitmap.Config.ARGB_8888 , true);
                    DefaultAllianceAvatars[lAllegianceOrdinal] = LaunchUICommon.TintBitmap(DefaultAllianceAvatars[lAllegianceOrdinal], LaunchUICommon.AllegianceColours[lAllegianceOrdinal]);
                    ApplyAllegianceSquare(DefaultAllianceAvatars[lAllegianceOrdinal], lAllegianceOrdinal);
                }

                return DefaultAllianceAvatars[lAllegianceOrdinal];
            }
        }

        return null;
    }

    private static void GenerateAvatar(MainActivity activity, LaunchClientGame game, Bitmap[] Container, int lAvatarID, int lAllegianceOrdinal, LaunchUICommon.AvatarPurpose purpose)
    {
        Bitmap bitmap = null;

        if (lAvatarID != ClientDefs.DEFAULT_AVATAR_ID)
        {
            File fileDir = activity.getDir(ClientDefs.AVATAR_FOLDER, Context.MODE_PRIVATE);
            File file = new File(fileDir, lAvatarID + ClientDefs.IMAGE_FORMAT);

            if (file.exists())
            {
                bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());

                if (bitmap != null)
                {
                    bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true); //Copy necessary to make mutable

                    if(bitmap.getWidth() == Defs.AVATAR_SIZE && bitmap.getHeight() == Defs.AVATAR_SIZE)
                    {
                        switch(purpose)
                        {
                            case PLAYER: ApplyAllegianceRing(bitmap, lAllegianceOrdinal); break;
                            case ALLIANCE: ApplyAllegianceSquare(bitmap, lAllegianceOrdinal); break;
                        }
                    }
                    else
                    {
                        //This one should be downloaded again as it's a legacy one.
                        game.DownloadAvatar(lAvatarID);
                        bitmap = GetDefaultAvatar(activity, lAllegianceOrdinal, purpose);
                    }
                }
                else
                {
                    //Fail safe in the unlikely event the file was unreadable.
                    bitmap = GetDefaultAvatar(activity, lAllegianceOrdinal, purpose);
                }
            }
            else
            {
                game.DownloadAvatar(lAvatarID);
                bitmap = GetDefaultAvatar(activity, lAllegianceOrdinal, purpose);
            }
        }
        else
        {
            bitmap = GetDefaultAvatar(activity, lAllegianceOrdinal, purpose);
        }

        Container[lAllegianceOrdinal] = bitmap;
    }

    private static Bitmap GetAvatar(MainActivity activity, LaunchClientGame game, int lAvatarID, int lAllegianceOrdinal, LaunchUICommon.AvatarPurpose purpose)
    {
        Bitmap[] AvatarSet = null;
        HashMap<Integer, Bitmap[]> Avatars = null;

        switch(purpose)
        {
            case PLAYER: Avatars = PlayerAvatars; break;
            case ALLIANCE: Avatars = AllianceAvatars; break;
        }

        //Put a container there if we don't have one yet.
        if(Avatars.containsKey(lAvatarID))
        {
            AvatarSet = Avatars.get(lAvatarID);
        }
        else
        {
            AvatarSet = new Bitmap[Allegiance.values().length];
            Avatars.put(lAvatarID, AvatarSet);
        }

        //Put an avatar into the allegiance slot of the set if there isn't one there already yet.
        if(AvatarSet[lAllegianceOrdinal] == null)
        {
            GenerateAvatar(activity, game, AvatarSet, lAvatarID, lAllegianceOrdinal, purpose);
        }

        return Avatars.get(lAvatarID)[lAllegianceOrdinal];
    }

    public static Bitmap GetPlayerAvatar(MainActivity activity, LaunchClientGame game, Player player)
    {
        int lAllegianceOrdinal = game.GetAllegiance(game.GetOurPlayer(), player).ordinal();
        return GetAvatar(activity, game, player.GetAvatarID(), lAllegianceOrdinal, LaunchUICommon.AvatarPurpose.PLAYER);
    }

    public static Bitmap GetAllianceAvatar(MainActivity activity, LaunchClientGame game, Alliance alliance)
    {
        int lAllegianceOrdinal = game.GetAllegiance(game.GetOurPlayer(), alliance).ordinal();
        return GetAvatar(activity, game, alliance.GetAvatarID(), lAllegianceOrdinal, LaunchUICommon.AvatarPurpose.ALLIANCE);
    }

    public static Bitmap GetNeutralAllianceAvatar(MainActivity activity, LaunchClientGame game, int lAvatarID)
    {
        return GetAvatar(activity, game, lAvatarID, Allegiance.NEUTRAL.ordinal(), LaunchUICommon.AvatarPurpose.ALLIANCE);
    }

    public static Bitmap GetNeutralPlayerAvatar(MainActivity activity, LaunchClientGame game, int lAvatarID)
    {
        return GetAvatar(activity, game, lAvatarID, Allegiance.NEUTRAL.ordinal(), LaunchUICommon.AvatarPurpose.PLAYER);
    }

    /**
     * Invalidate the avatar of this ID if cached, because a new version has been downloaded to replace the default.
     * @param lID The default avatar ID to burn.
     */
    public static void InvalidateAvatar(int lID)
    {
        if(PlayerAvatars.containsKey(lID))
        {
            PlayerAvatars.remove(lID);
        }

        if(AllianceAvatars.containsKey(lID))
        {
            AllianceAvatars.remove(lID);
        }
    }
}
