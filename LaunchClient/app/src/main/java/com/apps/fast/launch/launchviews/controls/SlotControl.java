package com.apps.fast.launch.launchviews.controls;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.TextUtilities;
import com.apps.fast.launch.components.Utilities;
import com.apps.fast.launch.launchviews.LaunchView;

import launch.game.LaunchClientGame;

/**
 * Created by tobster on 19/10/16.
 */
public class SlotControl extends LaunchView
{
    private TextView txtType;
    private TextView txtStatus;
    private ImageView imgAdd;
    private ImageView imgOccupied;

    private SlotListener listener;
    private byte cSlotNumber;
    private boolean bSlotIsPlayers;

    public enum ImageType
    {
        NONE,
        MISSILE,
        NUKE,
        INTERCEPTOR
    }

    public SlotControl(LaunchClientGame game, MainActivity activity, SlotListener listener, byte cSlotNumber, boolean bSlotIsPlayers)
    {
        super(game, activity, true);
        this.listener = listener;
        this.cSlotNumber = cSlotNumber;
        this.bSlotIsPlayers = bSlotIsPlayers;
        Setup();
    }

    @Override
    protected void Setup()
    {
        inflate(context, R.layout.control_missile_slot, this);

        txtType = (TextView)findViewById(R.id.txtType);
        txtStatus = (TextView)findViewById(R.id.txtStatus);
        imgAdd = (ImageView)findViewById(R.id.imgAdd);
        imgOccupied = (ImageView)findViewById(R.id.imgOccupied);

        if(bSlotIsPlayers)
        {
            setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    listener.SlotClicked(cSlotNumber);
                }
            });

            setOnLongClickListener(new OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View view)
                {
                    listener.SlotLongClicked(cSlotNumber);
                    return true;
                }
            });
        }

        Update();
    }

    @Override
    public void Update()
    {
        if(listener.GetSlotOccupied(cSlotNumber))
        {
            txtType.setText(listener.GetSlotContents(cSlotNumber));
            imgAdd.setVisibility(GONE);
            txtStatus.setVisibility(VISIBLE);

            if(listener.GetOnline())
            {
                long oPrepTime = listener.GetSlotPrepTime(cSlotNumber);

                if (oPrepTime > 0)
                {
                    txtStatus.setText(TextUtilities.GetTimeAmount(oPrepTime));
                    txtStatus.setTextColor(Utilities.ColourFromAttr(context, R.attr.BadColour));
                }
                else
                {
                    txtStatus.setText(context.getString(R.string.ready));
                    txtStatus.setTextColor(Utilities.ColourFromAttr(context, R.attr.GoodColour));
                }
            }
            else
            {
                txtStatus.setText(context.getString(R.string.offline));
                txtStatus.setTextColor(Utilities.ColourFromAttr(context, R.attr.BadColour));
            }

            txtStatus.setVisibility(VISIBLE);

            switch(listener.GetImageType(cSlotNumber))
            {
                case NONE:
                case MISSILE:
                {
                    imgOccupied.setImageDrawable(context.getResources().getDrawable(R.drawable.button_missile));
                }
                break;

                case NUKE:
                {
                    imgOccupied.setImageDrawable(context.getResources().getDrawable(R.drawable.button_nuke));
                }
                break;

                case INTERCEPTOR:
                {
                    imgOccupied.setImageDrawable(context.getResources().getDrawable(R.drawable.button_interceptor));
                }
                break;
            }

            imgOccupied.setVisibility(VISIBLE);
        }
        else
        {
            txtType.setText(context.getString(R.string.empty));
            txtStatus.setText(context.getString(R.string.buy));
            txtStatus.setTextColor(Utilities.ColourFromAttr(context, R.attr.GoodColour));
            txtStatus.setVisibility(bSlotIsPlayers ? VISIBLE : GONE);

            imgAdd.setVisibility(bSlotIsPlayers ? VISIBLE : GONE);
            imgOccupied.setVisibility(GONE);
        }
    }
}
