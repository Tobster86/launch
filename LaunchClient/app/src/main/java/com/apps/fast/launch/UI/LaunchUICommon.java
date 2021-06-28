package com.apps.fast.launch.UI;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;

import com.apps.fast.launch.R;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.TextUtilities;
import com.apps.fast.launch.views.LaunchDialog;

import java.util.List;

import launch.game.LaunchClientGame;
import launch.game.entities.Structure;

public class LaunchUICommon
{
    public static final int COLOUR_TINTED = 0xC0000000;

    public static final int[] AllegianceColours = new int[]
    {
        Color.GREEN,    //YOU
        Color.CYAN,     //ALLY
        Color.BLUE,     //AFFILIATE
        Color.RED,      //ENEMY
        Color.WHITE,    //NEUTRAL
        Color.MAGENTA   //PENDING TREATY
    };

    public enum AvatarPurpose
    {
        PLAYER,
        ALLIANCE,
    }

    public interface StructureOnOffInfoProvider
    {
        boolean IsSingleStructure();
        Structure GetCurrentStructure();
        List<Structure> GetCurrentStructures();
        void SetOnOff(boolean bOnline);
    }

    private static boolean bPowerOnHasBeenShown = false;
    private static boolean bPowerOffHasBeenShown = false;

    /**
     * Set the common on click listener for structure power on/off buttons.
     * @param activity A reference to the activity.
     * @param btnPower A reference to the power button.
     * @param infoProvider A reference to the object holding the structure information accessors.
     * @param game A reference to the client game.
     */
    public static void SetPowerButtonOnClickListener(final MainActivity activity, View btnPower, final StructureOnOffInfoProvider infoProvider, final LaunchClientGame game)
    {
        btnPower.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if(infoProvider.IsSingleStructure())
                {
                    Structure structure = infoProvider.GetCurrentStructure();
                    int lMaintenanceCost = game.GetConfig().GetMaintenanceCost(structure);

                    if (structure.GetRunning())
                    {
                        //Take offline?
                        if (!bPowerOffHasBeenShown)
                        {
                            bPowerOffHasBeenShown = true;

                            final LaunchDialog launchDialog = new LaunchDialog();
                            launchDialog.SetHeaderOnOff();
                            launchDialog.SetMessage(activity.getString(R.string.confirm_offline, structure.GetTypeName(), TextUtilities.GetCurrencyString(lMaintenanceCost), TextUtilities.GetTimeAmount(game.GetConfig().GetStructureBootTime())));
                            launchDialog.SetOnClickYes(new View.OnClickListener()
                            {
                                @Override
                                public void onClick(View view)
                                {
                                    launchDialog.dismiss();
                                    infoProvider.SetOnOff(false);
                                }
                            });
                            launchDialog.SetOnClickNo(new View.OnClickListener()
                            {
                                @Override
                                public void onClick(View view)
                                {
                                    launchDialog.dismiss();
                                }
                            });
                            launchDialog.show(activity.getFragmentManager(), "");
                        }
                        else
                        {
                            //Already been clicked and read this session. Just do it.
                            infoProvider.SetOnOff(false);
                        }
                    }
                    else
                    {
                        if (game.GetOurPlayer().GetWealth() >= lMaintenanceCost)
                        {
                            //Bring online?
                            if (!bPowerOnHasBeenShown)
                            {
                                bPowerOnHasBeenShown = true;

                                final LaunchDialog launchDialog = new LaunchDialog();
                                launchDialog.SetHeaderOnOff();

                                launchDialog.SetMessage(activity.getString(R.string.confirm_online, structure.GetTypeName(), TextUtilities.GetCurrencyString(lMaintenanceCost), TextUtilities.GetTimeAmount(game.GetConfig().GetStructureBootTime())));
                                launchDialog.SetOnClickYes(new View.OnClickListener()
                                {
                                    @Override
                                    public void onClick(View view)
                                    {
                                        launchDialog.dismiss();
                                        infoProvider.SetOnOff(true);
                                    }
                                });
                                launchDialog.SetOnClickNo(new View.OnClickListener()
                                {
                                    @Override
                                    public void onClick(View view)
                                    {
                                        launchDialog.dismiss();
                                    }
                                });
                                launchDialog.show(activity.getFragmentManager(), "");
                            }
                            else
                            {
                                //Already been clicked and read this session. Just do it.
                                infoProvider.SetOnOff(true);
                            }
                        }
                        else
                        {
                            activity.ShowBasicOKDialog(activity.getString(R.string.insufficient_funds));
                        }
                    }
                }
                else
                {
                    List<Structure> structures = infoProvider.GetCurrentStructures();

                    boolean bSomeOrAllOffline = false;

                    for(Structure structure : structures)
                    {
                        if(structure.GetOffline())
                        {
                            bSomeOrAllOffline = true;
                            break;
                        }
                    }

                    if(bSomeOrAllOffline)
                    {
                        infoProvider.SetOnOff(true);
                    }
                    else
                    {
                        infoProvider.SetOnOff(false);
                    }
                }
            }
        });
    }

    /**
     * Tint the grey shades of the given bitmap to the given colour.
     * @param bitmap The bitmap to tint.
     * @param lColour The ARGB to tint greyscale (R==G==B) pixels to multiplicatively.
     * @return The resulting tinted bitmap.
     */
    public static Bitmap TintBitmap(Bitmap bitmap, int lColour)
    {
        //Declare arrays to store the bitmap pixels and new bitmap.
        int[] lPixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        int[] lNewPixels = new int[bitmap.getWidth() * bitmap.getHeight()];

        //Transfer the bitmap pixels into an array.
        bitmap.getPixels(lPixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int lPixelIndex = 0;

        //Determine the tint values.
        float fltRedTint = (float)Color.red(lColour) / 255.0f;
        float fltGreenTint = (float)Color.green(lColour) / 255.0f;
        float fltBlueTint = (float)Color.blue(lColour) / 255.0f;

        for(int lPixel : lPixels)
        {
            //Determine the RGBA values.
            int lRed = Color.red(lPixel);
            int lGreen = Color.green(lPixel);
            int lBlue = Color.blue(lPixel);
            int lAlpha = Color.alpha(lPixel);

            //Determine if the pixel is greyscale and should thus be tinted.
            if(lRed == lGreen && lRed == lBlue)
            {
                //For code-readability, take the intensity, which is from any of the equal colours.
                int lIntensity = lRed;

                //Tint the pixel.
                int lNewRed = (int)((((float)lIntensity / 255.0f) * fltRedTint) * 255.0f);
                int lNewGreen = (int)((((float)lIntensity / 255.0f) * fltGreenTint) * 255.0f);
                int lNewBlue = (int)((((float)lIntensity / 255.0f) * fltBlueTint) * 255.0f);

                lPixel = Color.argb(lAlpha, lNewRed, lNewGreen, lNewBlue);
            }

            //Copy this pixel into the new pixel array.
            lNewPixels[lPixelIndex++] = lPixel;
        }

        return Bitmap.createBitmap(lNewPixels, bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888).copy(Bitmap.Config.ARGB_8888, true); //Additional copy necessary to make mutable.
    }
}
