package com.apps.fast.launch.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.components.TextUtilities;
import com.apps.fast.launch.components.Utilities;

/**
 * Created by tobster on 11/10/16.
 */
public class PurchaseButton extends FrameLayout
{
    public enum Colour
    {
        GREEN,
        RED,
        YELLOW
    }

    private ImageView imgContext;
    private ImageView imgContent;
    private TextView txtCost;
    private LinearLayout lytMore;

    public PurchaseButton(Context context)
    {
        super(context);
        Setup(context);
    }

    public PurchaseButton(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        Setup(context);
    }

    private void Setup(Context context)
    {
        inflate(context, R.layout.button_purchase, this);

        setClickable(true);

        imgContext = (ImageView)findViewById(R.id.imgContext);
        imgContent = (ImageView)findViewById(R.id.imgContent);
        txtCost = (TextView)findViewById(R.id.txtCost);
        lytMore = (LinearLayout)findViewById(R.id.lytMore);
    }

    public void SetImages(int lResIDContext, int lResIDContent)
    {
        imgContext.setImageResource(lResIDContext);
        imgContent.setImageResource(lResIDContent);
    }

    public void SetCost(int lCost, Colour colour)
    {
        txtCost.setText(TextUtilities.GetCurrencyString(lCost));

        switch(colour)
        {
            case RED:
            {
                txtCost.setTextColor(Utilities.ColourFromAttr(getContext(), R.attr.BadColour));
            }
            break;

            case YELLOW:
            {
                txtCost.setTextColor(Utilities.ColourFromAttr(getContext(), R.attr.WarningColour));
            }
            break;
        }
    }

    public void AddToMore(View view)
    {
        lytMore.addView(view);
    }
}
