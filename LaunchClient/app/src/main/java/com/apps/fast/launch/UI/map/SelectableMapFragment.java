package com.apps.fast.launch.UI.map;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.android.gms.maps.SupportMapFragment;

public class SelectableMapFragment extends SupportMapFragment
{
    public interface SelectableMapListener
    {
        void SetSelectionRectangle(float xFrom, float yFrom, float xTo, float yTo);
        void SelectEntities(float xFrom, float yFrom, float xTo, float yTo);
    }

    private SelectableMapListener mapListener;

    public View mapView;
    public TouchableWrapper touchView;
    public boolean bZoom = true;

    private float fltX1;
    private float fltY1;
    private float fltX2;
    private float fltY2;
    private boolean bSelecting = false;

    public static SelectableMapFragment newInstance() {
        return new SelectableMapFragment();
    }

    public void SetListener(SelectableMapListener listener)
    {
        mapListener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState)
    {
        mapView = super.onCreateView(inflater, parent, savedInstanceState);
        // overlay a touch view on map view to intercept the event
        touchView = new TouchableWrapper(getActivity());
        touchView.addView(mapView);
        return touchView;
    }

    @Override
    public View getView()
    {
        return mapView;
    }

    public void SetZoom(boolean bZoom)
    {
        this.bZoom = bZoom;
    }

    public class TouchableWrapper extends FrameLayout
    {
        public TouchableWrapper(Context context) {
            super(context);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event)
        {
            if(bZoom)
            {
                //Don't consume the event, let it zoom as normal.
                return super.dispatchTouchEvent(event);
            }
            else
            {
                switch (event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_MOVE:
                    {
                        if(event.getPointerCount() == 2)
                        {
                            bSelecting = true;
                            fltX1 = event.getX(0);
                            fltY1 = event.getY(0);
                            fltX2 = event.getX(1);
                            fltY2 = event.getY(1);

                            mapListener.SetSelectionRectangle(Math.min(fltX1, fltX2), Math.min(fltY1, fltY2), Math.max(fltX1, fltX2), Math.max(fltY1, fltY2));
                        }
                        else
                        {
                            if(bSelecting)
                            {
                                bSelecting = false;
                                mapListener.SelectEntities(Math.min(fltX1, fltX2), Math.min(fltY1, fltY2), Math.max(fltX1, fltX2), Math.max(fltY1, fltY2));
                            }
                        }
                    }
                    break;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP:
                    case MotionEvent.ACTION_HOVER_EXIT:
                    {
                        if(bSelecting)
                        {
                            bSelecting = false;
                            mapListener.SelectEntities(Math.min(fltX1, fltX2), Math.min(fltY1, fltY2), Math.max(fltX1, fltX2), Math.max(fltY1, fltY2));
                        }
                    }
                    break;
                }

                //Consume event to prevent map from receiving the event.
                return true;
            }
        }
    }
}
