package com.apps.fast.launch.launchviews;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.UI.AvatarBitmaps;
import com.apps.fast.launch.UI.LaunchUICommon.AvatarPurpose;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.ClientDefs;
import com.apps.fast.launch.components.Utilities;

import launch.game.Alliance;
import launch.game.LaunchClientGame;

/**
 * Created by tobster on 16/10/15.
 */
public class CreateAllianceView extends LaunchView
{
    private static String strName = "";
    private static String strDescription = "";

    private TextView txtName;
    private TextView txtDescription;
    private LinearLayout btnAvatar;
    private LinearLayout btnCreate;
    private ImageView imgAvatar;
    private int lAvatarID;

    public CreateAllianceView(LaunchClientGame game, MainActivity activity, int lAvatarID)
    {
        super(game, activity, true);
        this.lAvatarID = lAvatarID;
        Setup();
    }

    @Override
    protected void Setup()
    {
        inflate(context, R.layout.view_create_alliance, this);

        txtName = (TextView)findViewById(R.id.txtName);
        txtDescription = (TextView)findViewById(R.id.txtDescription);
        btnAvatar = (LinearLayout) findViewById(R.id.btnAvatar);
        btnCreate = (LinearLayout) findViewById(R.id.btnCreate);
        imgAvatar = (ImageView)findViewById(R.id.imgAvatar);

        txtName.setText(strName);
        txtDescription.setText(strDescription);

        btnAvatar.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Utilities.DismissKeyboard(activity, activity.getCurrentFocus());
                strName = txtName.getText().toString();
                strDescription = txtDescription.getText().toString();
                activity.SetView(new UploadAvatarView(game, activity, AvatarPurpose.ALLIANCE));
            }
        });

        btnCreate.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Utilities.DismissKeyboard(activity, activity.getCurrentFocus());

                //Clear any existing errors.
                txtName.setError(null);

                //Get the values.
                String strName = txtName.getText().toString();
                String strDescription = txtDescription.getText().toString();

                if(strName.length() > 0)
                {
                    boolean bNameTaken = false;

                    for(Alliance alliance : game.GetAlliances())
                    {
                        if(strName.equals(alliance.GetName()))
                        {
                            bNameTaken = true;
                            break;
                        }
                    }

                    if(!bNameTaken)
                    {
                        game.CreateAlliance(strName, strDescription, lAvatarID);
                    }
                    else
                    {
                        txtName.setError(context.getString(R.string.alliance_name_taken));
                    }
                }
                else
                {
                    txtName.setError(context.getString(R.string.specify_alliance_name));
                }
            }
        });

        if(lAvatarID != ClientDefs.DEFAULT_AVATAR_ID)
        {
            imgAvatar.setImageBitmap(AvatarBitmaps.GetNeutralAllianceAvatar(activity, game, lAvatarID));
        }
    }

    @Override
    public void Update()
    {

    }

    public void UsernameTaken()
    {
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                txtName.setError(context.getString(R.string.alliance_name_taken));
            }
        });
    }
}
