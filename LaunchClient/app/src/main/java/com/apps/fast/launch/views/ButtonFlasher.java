package com.apps.fast.launch.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageButton;

import com.apps.fast.launch.R;
import com.apps.fast.launch.components.Utilities;

/**
 * Created by tobster on 03/05/16.
 */
public class ButtonFlasher
{
    private static final int FLASH_OFF = 0;
    private static final int FLASH_RED = 1;
    private static final int FLASH_YELLOW = 2;
    private static final int FLASH_GREEN = 3;

    private boolean bFlash = false;
    private int lFlashState = FLASH_OFF;

    private View view;

    public ButtonFlasher(View view)
    {
        this.view = view;
    }

    public void FlashUpdate(Context context)
    {
        if(bFlash)
        {
            view.setBackground(Utilities.DrawableFromAttr(context, R.attr.ButtonDrawableNormal));
        }
        else
        {
            switch (lFlashState)
            {
                case FLASH_RED:
                {
                    view.setBackground(Utilities.DrawableFromAttr(context, R.attr.ButtonDrawableRed));
                }
                break;

                case FLASH_YELLOW:
                {
                    view.setBackground(Utilities.DrawableFromAttr(context, R.attr.ButtonDrawableYellow));
                }
                break;

                case FLASH_GREEN:
                {
                    view.setBackground(Utilities.DrawableFromAttr(context, R.attr.ButtonDrawableGreen));
                }
                break;
            }
        }

        bFlash = !bFlash;
    }

    public void FlashOff()
    {
        lFlashState = FLASH_OFF;
    }

    public void FlashRed()
    {
        lFlashState = FLASH_RED;
    }

    public void FlashYellow()
    {
        lFlashState = FLASH_YELLOW;
    }

    public void FlashGreen()
    {
        lFlashState = FLASH_GREEN;
    }

    //Use these to set solid colours, in conjunction with NOT calling FlashUpdate.
    public void TurnOff(Context context)
    {
        view.setBackground(Utilities.DrawableFromAttr(context, R.attr.ButtonDrawableNormal));
    }

    public void TurnRed(Context context)
    {
        view.setBackground(Utilities.DrawableFromAttr(context, R.attr.ButtonDrawableRed));
    }

    public void TurnYellow(Context context)
    {
        view.setBackground(Utilities.DrawableFromAttr(context, R.attr.ButtonDrawableYellow));
    }

    public void TurnGreen(Context context)
    {
        view.setBackground(Utilities.DrawableFromAttr(context, R.attr.ButtonDrawableGreen));
    }
}
