package com.apps.fast.launch.launchviews;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;

import com.apps.fast.launch.activities.MainActivity;

import launch.game.LaunchClientGame;
import launch.game.entities.LaunchEntity;
import launch.game.treaties.Treaty;
import launch.utilities.LaunchEvent;

/**
 * Created by tobster on 17/07/16.
 */
public abstract class LaunchView extends FrameLayout
{
    protected LaunchClientGame game;
    protected Context context;
    protected MainActivity activity;

    //To be used for most views.
    public LaunchView(LaunchClientGame game, MainActivity activity)
    {
        super(activity);
        this.context = activity;
        this.game = game;
        this.activity = activity;
        Setup();
        Update();
    }

    //To be used with entity-based views, that call setup themselves after assigning other parameters which must be accessed from setup.
    public LaunchView(LaunchClientGame game, MainActivity activity, boolean bDontCallSetup)
    {
        super(activity);
        this.context = activity;
        this.game = game;
        this.activity = activity;

        if (!bDontCallSetup)
        {
            Setup();
            Update();
        }
    }

    //Exists so the Android Studio xml view preview can work. DO NOT CALL.
    private LaunchView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        Log.e(this.getClass().getName(), "Wrong constructor called!");
    }

    //Exists so the Android Studio xml view preview can work. DO NOT CALL.
    private LaunchView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        Log.e(this.getClass().getName(), "Wrong constructor called!");
    }

    /**
     * Shut this view down and return to the main view.
     * @param bClearSelectedEntity If true, any selected entities will be deselected.
     */
    protected void Finish(boolean bClearSelectedEntity)
    {
        if (bClearSelectedEntity)
            activity.ClearSelectedEntity();

        activity.ReturnToMainView();
    }

    /**
     * Allows views to respond whenever an entity update is received from the server.
     * @param entity The entity that updated.
     */
    public void EntityUpdated(LaunchEntity entity)
    {
        //Do nothing unless overridden.
    }

    /**
     * Allows views to respond when an entity is removed from the game.
     * @param entity A shadow of the entity that was removed from the game.
     */
    public void EntityRemoved(LaunchEntity entity)
    {
        //Do nothing unless overridden.
    }

    protected abstract void Setup();

    /**
     * Allows views to update when the game ticks.
     */
    public void Update()
    {
        //Do nothing unless overridden.
    }

    /**
     * Allows views to respond when an avatar is received and changed.
     * @param lAvatarID ID of the received avatar.
     */
    public void AvatarSaved(int lAvatarID)
    {
        //Do nothing unless overridden.
    }

    public void TreatyUpdated(Treaty treaty)
    {
        //Do nothing unless overridden.
    }
}
