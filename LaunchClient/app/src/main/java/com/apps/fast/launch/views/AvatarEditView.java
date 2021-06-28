package com.apps.fast.launch.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.UI.LaunchUICommon.AvatarPurpose;
import com.apps.fast.launch.launchviews.UploadAvatarView;

import launch.game.Defs;

/**
 * Created by tobster on 27/01/16.
 */
public class AvatarEditView extends ImageView
{
    private int lMaxWidth;
    private int lMaxHeight;
    private Bitmap bitmap = null;

    private int lSelLeft = 0;
    private int lSelTop = 0;
    private int lSelRight = 0;
    private int lSelBottom = 0;
    private int lSelWidth = 0;
    private int lSelHeight = 0;

    private boolean bZooming = false;

    private UploadAvatarView listener;

    private AvatarPurpose purpose;

    public AvatarEditView(Context context)
    {
        super(context);
        Setup();
    }

    public AvatarEditView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        Setup();
    }

    public AvatarEditView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        Setup();
    }

    private void Setup()
    {
        lMaxWidth = getResources().getDisplayMetrics().widthPixels - (int)(2.0 * getResources().getDimension(R.dimen.MainViewSideMargin));
        lMaxHeight = lMaxWidth;
        setAdjustViewBounds(true);
    }

    public void SetAvatar(Bitmap bitmap, UploadAvatarView listener, AvatarPurpose purpose)
    {
        this.listener = listener;
        this.purpose = purpose;

        this.bitmap = bitmap;
        setImageBitmap(bitmap);

        setMaxWidth((bitmap.getWidth() > lMaxWidth) ? lMaxWidth : bitmap.getWidth());
        setMaxHeight((bitmap.getHeight() > lMaxHeight) ? lMaxHeight : bitmap.getHeight());
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        if(bitmap != null)
        {
            Paint paint = new Paint();
            paint.setColor(Color.YELLOW);
            paint.setStyle(Paint.Style.STROKE);

            switch(purpose)
            {
                case PLAYER:
                {
                    canvas.drawCircle((float)(lSelRight + lSelLeft) / 2.0f, (float)(lSelTop + lSelBottom) / 2.0f, (float)(lSelRight - lSelLeft) / 2.0f, paint);
                }
                break;

                case ALLIANCE:
                {
                    canvas.drawRect(lSelLeft, lSelTop, lSelRight, lSelBottom, paint);
                }
                break;
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        switch(MotionEventCompat.getActionMasked(event))
        {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_POINTER_DOWN:
            {
                if (event.getPointerCount() > 1)
                {
                    bZooming = true;

                    //Resize.
                    float fltX1 = Math.min(Math.max(MotionEventCompat.getX(event, 0), 0.0f), getWidth() - 1);
                    float fltY1 = Math.min(Math.max(MotionEventCompat.getY(event, 0), 0.0f), getHeight() - 1);
                    float fltX2 = Math.min(Math.max(MotionEventCompat.getX(event, 1), 0.0f), getWidth() - 1);
                    float fltY2 = Math.min(Math.max(MotionEventCompat.getY(event, 1), 0.0f), getHeight() - 1);

                    float fltX = (fltX1 + fltX2) / 2.0f;
                    float fltY = (fltY1 + fltY2) / 2.0f;

                    float fltEdge = Math.min(Math.abs(fltX1 - fltX2), Math.abs(fltY1 - fltY2));
                    float fltCentreOffset = fltEdge / 2.0f;

                    lSelLeft = (int)((fltX - fltCentreOffset) + 0.5f);
                    lSelRight = (int)((fltX + fltCentreOffset) + 0.5f);
                    lSelTop = (int)((fltY - fltCentreOffset) + 0.5f);
                    lSelBottom = (int)((fltY + fltCentreOffset) + 0.5f);
                }
                else
                {
                    //Move.
                    if(!bZooming)
                    {
                        int lX = (int) (MotionEventCompat.getX(event, 0) + 0.5f);
                        int lY = (int) (MotionEventCompat.getY(event, 0) + 0.5f);

                        lSelLeft = lX - (int) (((float) lSelWidth / 2) + 0.5f);
                        lSelRight = lX + (int) (((float) lSelWidth / 2) + 0.5f);
                        lSelTop = lY - (int) (((float) lSelHeight / 2) + 0.5f);
                        lSelBottom = lY + (int) (((float) lSelHeight / 2) + 0.5f);
                    }
                }

                ProcessSelection();

                postInvalidate();

                getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            }

            case MotionEvent.ACTION_UP:
            {
                if (event.getPointerCount() == 1)
                {
                    bZooming = false;
                    return true;
                }
            }
            break;
        }

        return false;
    }

    private void ProcessSelection()
    {
        //Cap.
        if(lSelLeft < 0)
        {
            lSelRight += (-lSelLeft);
            lSelLeft += (-lSelLeft);
        }

        if(lSelTop < 0)
        {
            lSelBottom += (-lSelTop);
            lSelTop += (-lSelTop);
        }

        if(lSelRight > getWidth())
        {
            lSelLeft -= ((lSelRight - getWidth()) + 1);
            lSelRight -= ((lSelRight - getWidth()) + 1);
        }

        if(lSelBottom > getHeight())
        {
            lSelTop -= ((lSelBottom - getHeight()) + 1);
            lSelBottom -= ((lSelBottom - getHeight()) + 1);
        }

        lSelWidth = (lSelRight - lSelLeft) - 1;
        lSelHeight = (lSelBottom - lSelTop) - 1;

        //Squareify, always by shrinking.
        if(lSelWidth > lSelHeight)
        {
            lSelRight = lSelLeft + lSelHeight;
            lSelWidth = lSelHeight;
        }
        else if(lSelHeight > lSelWidth)
        {
            lSelBottom = lSelTop + lSelWidth;
            lSelHeight = lSelWidth;
        }

        if(lSelWidth > 0 && lSelHeight > 0)
        {
            listener.AvatarPreviewReady();
        }
    }

    public Bitmap GetBitmap()
    {
        float fltXScale = (float)bitmap.getWidth() / (float)getWidth();
        float fltYScale = (float)bitmap.getHeight() / (float)getHeight();

        int lX = (int)(((float)lSelLeft * fltXScale) + 0.5f);
        int lY = (int)(((float)lSelTop * fltYScale) + 0.5f);
        int lWidth = (int)(((float)lSelWidth * fltXScale) + 0.5f);
        int lHeight = (int)(((float)lSelHeight * fltYScale) + 0.5f);

        while(lX + lWidth >= bitmap.getWidth())
            lWidth--;

        while(lY + lHeight >= bitmap.getHeight())
            lHeight--;

        Bitmap bmpCropped = Bitmap.createBitmap(bitmap, lX, lY, lWidth, lHeight);
        return Bitmap.createScaledBitmap(bmpCropped, Defs.AVATAR_SIZE, Defs.AVATAR_SIZE, true);
    }
}
