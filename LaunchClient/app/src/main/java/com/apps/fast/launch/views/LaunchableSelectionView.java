package com.apps.fast.launch.views;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.TextUtilities;
import com.apps.fast.launch.components.Utilities;

import launch.game.GeoCoord;
import launch.game.LaunchClientGame;
import launch.game.types.InterceptorType;
import launch.game.types.MissileType;

/**
 * Created by tobster on 09/11/15.
 */
public class LaunchableSelectionView extends FrameLayout
{
    private MainActivity activity;  //Full activity not just context, so we can display dialogs.
    private LaunchClientGame game;

    //If targeting rather than buying a missile.
    private GeoCoord geoTarget = null;
    private GeoCoord geoOrigin;

    //If targeting rather than buying an interceptor.
    private boolean bIntercepting = false;
    private long oFlightTime;

    //Purchase counts.
    private TextView txtNumber;
    private int lNumber = 0;

    private boolean bMissile;  //As opposed to an interceptor.

    //Buying a missile.
    public LaunchableSelectionView(MainActivity activity, LaunchClientGame game, MissileType type)
    {
        super(activity);
        this.activity = activity;
        this.game = game;
        bMissile = true;

        Setup(type);
    }

    //Selecting a missile to launch.
    public LaunchableSelectionView(MainActivity activity, LaunchClientGame game, MissileType type, GeoCoord geoTarget, GeoCoord geoOrigin)
    {
        super(activity);
        this.activity = activity;
        this.game = game;
        this.geoTarget = geoTarget;
        this.geoOrigin = geoOrigin;
        bMissile = true;

        Setup(type);
    }

    //Buying an interceptor.
    public LaunchableSelectionView(MainActivity activity, LaunchClientGame game, InterceptorType type)
    {
        super(activity);
        this.activity = activity;
        this.game = game;
        bMissile = false;

        Setup(type);
    }

    //Selecting an interceptor to launch.
    public LaunchableSelectionView(MainActivity activity, LaunchClientGame game, InterceptorType type, long oFlightTime)
    {
        super(activity);
        this.activity = activity;
        this.game = game;
        this.oFlightTime = oFlightTime;
        bIntercepting = true;

        Setup(type);
    }

    private void Setup(MissileType type)
    {
        inflate(activity, R.layout.view_missile_selection, this);

        TextView txtName = (TextView)findViewById(R.id.txtName);
        TextView txtRange = (TextView)findViewById(R.id.txtRange);
        TextView txtBlastRadius = (TextView)findViewById(R.id.txtBlastRadius);
        TextView txtMaxDamage = (TextView)findViewById(R.id.txtMaxDamage);
        TextView txtSpeed = (TextView)findViewById(R.id.txtSpeed);
        TextView txtPrepTime = (TextView)findViewById(R.id.txtPrepTime);
        TextView txtFlightTime = (TextView)findViewById(R.id.txtFlightTime);

        TextView txtRangeTitle = (TextView)findViewById(R.id.txtRangeTitle);
        TextView txtSpeedTitle = (TextView)findViewById(R.id.txtSpeedTitle);
        TextView txtPrepTimeTitle = (TextView)findViewById(R.id.txtPrepTimeTitle);
        TextView txtFlightTimeTitle = (TextView)findViewById(R.id.txtFlightTimeTitle);

        txtNumber = (TextView)findViewById(R.id.txtNumber);

        ImageView imgNuclear = (ImageView)findViewById(R.id.imgNuclear);
        ImageView imgTracking = (ImageView)findViewById(R.id.imgTracking);
        ImageView imgECM = (ImageView)findViewById(R.id.imgECM);
        ImageView imgFast = (ImageView)findViewById(R.id.imgFast);

        TextView txtCost = (TextView)findViewById(R.id.txtCost);

        int lCost = game.GetConfig().GetMissileCost(type);

        txtRange.setText(TextUtilities.GetDistanceStringFromKM(game.GetConfig().GetMissileRange(type.GetRangeIndex())));
        txtBlastRadius.setText(TextUtilities.GetDistanceStringFromKM(game.GetConfig().GetBlastRadius(type)));
        txtMaxDamage.setText(TextUtilities.GetDamageString(game.GetConfig().GetMissileMaxDamage(type)));
        txtSpeed.setText(TextUtilities.GetSpeedFromKph(game.GetConfig().GetMissileSpeed(type.GetSpeedIndex())));
        txtCost.setText(TextUtilities.GetCurrencyString(lCost));

        imgNuclear.setVisibility(type.GetNuclear() ? VISIBLE : GONE);
        imgTracking.setVisibility(type.GetTracking() ? VISIBLE : GONE);
        imgECM.setVisibility(type.GetECM() ? VISIBLE : GONE);
        imgFast.setVisibility(game.GetMissileIsFast(type) ? VISIBLE : GONE);

        if(type.GetNuclear())
        {
            imgNuclear.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    activity.ShowBasicOKDialog(activity.getString(R.string.nuclear_information));
                }
            });
        }

        if(type.GetTracking())
        {
            imgTracking.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    activity.ShowBasicOKDialog(activity.getString(R.string.tracking_information));
                }
            });
        }

        if(type.GetECM())
        {
            imgECM.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    activity.ShowBasicOKDialog(activity.getString(R.string.ecm_information));
                }
            });
        }

        if(game.GetMissileIsFast(type))
        {
            imgFast.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    activity.ShowBasicOKDialog(activity.getString(R.string.fast_missile_information));
                }
            });
        }

        txtName.setText(type.GetName());

        if(geoTarget == null)
        {
            //Building.
            txtPrepTime.setText(TextUtilities.GetTimeAmount(game.GetConfig().GetMissilePrepTime(type)));

            if(lCost > game.GetOurPlayer().GetWealth())
            {
                txtCost.setTextColor(Utilities.ColourFromAttr(getContext(), R.attr.BadColour));
            }

            txtFlightTimeTitle.setVisibility(GONE);
            txtFlightTime.setVisibility(GONE);
        }
        else
        {
            //Launching.
            txtRange.setVisibility(GONE);
            txtRangeTitle.setVisibility(GONE);
            txtSpeed.setVisibility(GONE);
            txtSpeedTitle.setVisibility(GONE);
            txtPrepTime.setVisibility(GONE);
            txtPrepTimeTitle.setVisibility(GONE);
            txtCost.setVisibility(GONE);

            txtFlightTime.setText(TextUtilities.GetTimeAmount(game.GetTimeToTarget(geoOrigin, geoTarget, game.GetConfig().GetMissileSpeed(type.GetSpeedIndex()))));
        }
    }

    private void Setup(InterceptorType type)
    {
        inflate(activity, R.layout.view_interceptor_selection, this);

        TextView txtName = (TextView)findViewById(R.id.txtName);
        TextView txtRange = (TextView)findViewById(R.id.txtRange);
        TextView txtSpeed = (TextView)findViewById(R.id.txtSpeed);
        TextView txtPrepTime = (TextView)findViewById(R.id.txtPrepTime);
        TextView txtFlightTime = (TextView)findViewById(R.id.txtFlightTime);
        TextView txtCost = (TextView)findViewById(R.id.txtCost);

        TextView txtPrepTimeTitle = (TextView)findViewById(R.id.txtPrepTimeTitle);
        TextView txtFlightTimeTitle = (TextView)findViewById(R.id.txtFlightTimeTitle);

        txtNumber = (TextView)findViewById(R.id.txtNumber);

        ImageView imgFast = (ImageView)findViewById(R.id.imgFast);

        int lCost = game.GetConfig().GetInterceptorCost(type);

        txtName.setText(type.GetName());
        txtRange.setText(TextUtilities.GetDistanceStringFromKM(game.GetConfig().GetInterceptorRange(type.GetRangeIndex())));
        txtSpeed.setText(TextUtilities.GetSpeedFromKph(game.GetConfig().GetInterceptorSpeed(type.GetSpeedIndex())));
        txtCost.setText(TextUtilities.GetCurrencyString(lCost));

        imgFast.setVisibility(game.GetInterceptorIsFast(type) ? VISIBLE : GONE);

        if(game.GetInterceptorIsFast(type))
        {
            imgFast.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    activity.ShowBasicOKDialog(activity.getString(R.string.fast_interceptor_information));
                }
            });
        }

        if(bIntercepting)
        {
            txtFlightTime.setText(TextUtilities.GetTimeAmount(oFlightTime));

            txtCost.setVisibility(GONE);
            txtPrepTime.setVisibility(GONE);
            txtPrepTimeTitle.setVisibility(GONE);
        }
        else
        {
            txtFlightTime.setVisibility(GONE);
            txtFlightTimeTitle.setVisibility(GONE);

            if(lCost > game.GetOurPlayer().GetWealth())
            {
                //Can't afford.
                txtCost.setTextColor(Utilities.ColourFromAttr(getContext(), R.attr.BadColour));
            }

            txtPrepTime.setText(TextUtilities.GetTimeAmount(game.GetConfig().GetInterceptorPrepTime(type)));
        }
    }

    public void SetHighlighted()
    {
        findViewById(R.id.lytMain).setBackground(Utilities.DrawableFromAttr(activity, R.attr.DetailButtonDrawableHighlighted));
        //((LinearLayout)findViewById(R.id.lytMain)).setBackgroundDrawable(activity.getResources().getDrawable(R.drawable.detail_button_highlighted));
    }

    public void SetNotHighlighted()
    {
        findViewById(R.id.lytMain).setBackground(Utilities.DrawableFromAttr(activity, R.attr.DetailButtonDrawableNormal));
        //((LinearLayout)findViewById(R.id.lytMain)).setBackgroundDrawable(activity.getResources().getDrawable(R.drawable.detail_button));
    }

    public void SetDisabled()
    {
        findViewById(R.id.lytMain).setBackground(Utilities.DrawableFromAttr(activity, R.attr.DetailButtonDrawableDisabled));
        //((LinearLayout)findViewById(R.id.lytMain)).setBackgroundDrawable(activity.getResources().getDrawable(R.drawable.detail_button_disabled));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        super.onLayout(changed, left, top, right, bottom);

        if(bMissile)
        {
            TextView txtNameTitle = (TextView) findViewById(R.id.txtNameTitle);
            TextView txtRangeTitle = (TextView) findViewById(R.id.txtRangeTitle);
            TextView txtBlastRadiusTitle = (TextView) findViewById(R.id.txtBlastRadiusTitle);
            TextView txtMaxDamageTitle = (TextView) findViewById(R.id.txtMaxDamageTitle);
            TextView txtSpeedTitle = (TextView) findViewById(R.id.txtSpeedTitle);
            TextView txtPrepTimeTitle = (TextView) findViewById(R.id.txtPrepTimeTitle);
            TextView txtFlightTimeTitle = (TextView) findViewById(R.id.txtFlightTimeTitle);

            //Set title items to same width, to justify text of the values.
            int lTitleLargest = txtNameTitle.getWidth();
            lTitleLargest = Math.max(lTitleLargest, txtRangeTitle.getWidth());
            lTitleLargest = Math.max(lTitleLargest, txtBlastRadiusTitle.getWidth());
            lTitleLargest = Math.max(lTitleLargest, txtMaxDamageTitle.getWidth());
            lTitleLargest = Math.max(lTitleLargest, txtSpeedTitle.getWidth());
            lTitleLargest = Math.max(lTitleLargest, txtPrepTimeTitle.getWidth());
            lTitleLargest = Math.max(lTitleLargest, txtFlightTimeTitle.getWidth());
            txtNameTitle.setWidth(lTitleLargest);
            txtRangeTitle.setWidth(lTitleLargest);
            txtBlastRadiusTitle.setWidth(lTitleLargest);
            txtMaxDamageTitle.setWidth(lTitleLargest);
            txtSpeedTitle.setWidth(lTitleLargest);
            txtPrepTimeTitle.setWidth(lTitleLargest);
            txtFlightTimeTitle.setWidth(lTitleLargest);
        }
        else
        {
            TextView txtNameTitle = (TextView)findViewById(R.id.txtNameTitle);
            TextView txtRangeTitle = (TextView)findViewById(R.id.txtRangeTitle);
            TextView txtSpeedTitle = (TextView)findViewById(R.id.txtSpeedTitle);
            TextView txtPrepTimeTitle = (TextView)findViewById(R.id.txtPrepTimeTitle);
            TextView txtFlightTimeTitle = (TextView)findViewById(R.id.txtFlightTimeTitle);

            //Set title items to same width, to justify text of the values.
            int lTitleLargest = txtNameTitle.getWidth();
            lTitleLargest = Math.max(lTitleLargest, txtRangeTitle.getWidth());
            lTitleLargest = Math.max(lTitleLargest, txtSpeedTitle.getWidth());
            lTitleLargest = Math.max(lTitleLargest, txtPrepTimeTitle.getWidth());
            lTitleLargest = Math.max(lTitleLargest, txtFlightTimeTitle.getWidth());
            txtNameTitle.setWidth(lTitleLargest);
            txtRangeTitle.setWidth(lTitleLargest);
            txtSpeedTitle.setWidth(lTitleLargest);
            txtPrepTimeTitle.setWidth(lTitleLargest);
            txtFlightTimeTitle.setWidth(lTitleLargest);
        }
    }

    public void IncrementNumber()
    {
        txtNumber.setText(Integer.toString(++lNumber));
    }

    public void SetNumber(int lNumber)
    {
        txtNumber.setText(Integer.toString(lNumber));
    }
}
