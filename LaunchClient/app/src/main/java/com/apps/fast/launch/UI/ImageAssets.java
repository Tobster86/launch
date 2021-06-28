package com.apps.fast.launch.UI;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.apps.fast.launch.components.ClientDefs;

import java.io.File;
import java.util.HashMap;

import launch.game.LaunchClientGame;
import launch.game.types.LaunchType;

public class ImageAssets
{
    private static HashMap<Integer, Bitmap> Assets = new HashMap<>();

    public static Bitmap GetImageAsset(Context context, LaunchClientGame game, int lAssetID)
    {
        if (lAssetID != LaunchType.ASSET_ID_DEFAULT)
        {
            if(Assets.containsKey(lAssetID))
            {
                return Assets.get(lAssetID);
            }
            else
            {
                File fileDir = context.getDir(ClientDefs.IMGASSETS_FOLDER, Context.MODE_PRIVATE);
                File file = new File(fileDir, lAssetID + ClientDefs.IMAGE_FORMAT);

                if (file.exists())
                {
                    Bitmap result = BitmapFactory.decodeFile(file.getAbsolutePath());

                    if (result != null)
                    {
                        Assets.put(lAssetID, result);
                        return result;
                    }
                }

                game.DownloadImage(lAssetID);
            }
        }

        return null;
    }
}
