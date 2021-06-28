package com.apps.fast.launch.views;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.Random;

/**
 * Created by tobster on 02/11/15.
 */
public class StaticScreen extends SurfaceView implements SurfaceHolder.Callback
{
    private static final float PARTICLE_CHANCE = 0.01f;

    private static final int[] PARTICLE_COLOURS =
            {
                    0xff004f00,
                    0xff005f00,
                    0xff006f00,
                    0xff007f00,
                    0xff008f00,
                    0xff009f00,
                    0xff00Af00,
                    0xff00Bf00,
            };

    private static final int COLOUR_COUNT = PARTICLE_COLOURS.length;

    private static final Random random = new Random();

    private Thread renderThread;
    private boolean bRun;

    private int lParticleCount;

    public StaticScreen(Context context)
    {
        super(context);
        getHolder().addCallback(this);
    }

    public StaticScreen(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        getHolder().addCallback(this);
    }

    public StaticScreen(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        getHolder().addCallback(this);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        final SurfaceHolder surfaceHolder = holder;

        /*renderThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                bRun = true;

                while(bRun)
                {
                    Canvas canvas = surfaceHolder.lockCanvas();

                    if(canvas != null)
                    {
                        canvas.drawColor(Color.BLACK);
                        Paint paint = new Paint();

                        for(int i = 0; i < lParticleCount; i++)
                        {
                            paint.setColor(PARTICLE_COLOURS[random.nextInt(COLOUR_COUNT)]);
                            canvas.drawPoint(random.nextFloat() * getWidth(), random.nextFloat() * getHeight(), paint);
                        }

                        surfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }
            }
        });

        renderThread.start();*/
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        lParticleCount = (int)(PARTICLE_CHANCE * (float)(width * height));
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        bRun = false;
    }
}
