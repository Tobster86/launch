package com.apps.fast.launch.views;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.apps.fast.launch.R;
import com.apps.fast.launch.activities.MainActivity;

/**
 * Created by tobster on 09/11/15.
 */
public class EntityControls extends LinearLayout
{
    private ImageView btnGoTo;
    private ImageView btnZoom;
    private ImageView btnExpand;
    private ImageView btnContract;
    private ImageView btnClose;

    private MainActivity activity;

    public EntityControls(Context context)
    {
        super(context);
        Setup(context);
    }

    public EntityControls(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        Setup(context);
    }

    public EntityControls(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        Setup(context);
    }

    public void Setup(Context context)
    {
        inflate(context, R.layout.view_entitycontrols, this);

        btnGoTo = (ImageView)findViewById(R.id.btnGoTo);
        btnZoom = (ImageView)findViewById(R.id.btnZoom);
        btnExpand = (ImageView)findViewById(R.id.btnExpand);
        btnContract = (ImageView)findViewById(R.id.btnContract);
        btnClose = (ImageView)findViewById(R.id.btnClose);

        btnGoTo.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                activity.GoToSelectedEntity(false);
            }
        });

        btnZoom.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                activity.GoToSelectedEntity(true);
            }
        });

        btnExpand.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                activity.ExpandView();
                btnExpand.setVisibility(GONE);
                btnContract.setVisibility(VISIBLE);
            }
        });

        btnContract.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                activity.ContractView();
                btnExpand.setVisibility(VISIBLE);
                btnContract.setVisibility(GONE);
            }
        });

        btnClose.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                activity.ClearSelectedEntity();
                activity.ReturnToMainView();
            }
        });
    }

    public void SetActivity(MainActivity activity)
    {
        this.activity = activity;
    }
}
