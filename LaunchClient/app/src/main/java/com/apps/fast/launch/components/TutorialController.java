package com.apps.fast.launch.components;

import android.app.Activity;
import android.view.View;

import com.apps.fast.launch.R;
import com.apps.fast.launch.views.LaunchDialog;

public class TutorialController
{
    private enum State
    {
        INITIAL,
        BUILD_SAM_SITE
    }

    private State state = State.INITIAL;

    public TutorialController()
    {

    }

    public void NotifyEntry(Activity activity)
    {
        final LaunchDialog launchDialog = new LaunchDialog();
        launchDialog.SetHeaderLaunch();
        launchDialog.SetMessage(activity.getString(R.string.quit));
        launchDialog.SetOnClickYes(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
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
}
