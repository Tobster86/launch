package com.apps.fast.launch.launchviews;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.UI.AvatarBitmaps;
import com.apps.fast.launch.UI.LaunchUICommon;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.ClientDefs;
import com.apps.fast.launch.components.Utilities;

import launch.game.LaunchClientGame;

/**
 * Created by tobster on 16/10/15.
 */
public class RegisterView extends LaunchView
{
    private static String strPlayerName = "";

    private TextView txtPlayerName;
    private LinearLayout btnAvatar;
    private LinearLayout btnPrivacyZones;
    private LinearLayout btnRegister;
    private ImageView imgAvatar;
    private int lAvatarID;

    public RegisterView(LaunchClientGame game, MainActivity activity, int lAvatarID)
    {
        super(game, activity, true);
        this.lAvatarID = lAvatarID;
        Setup();
    }

    @Override
    protected void Setup()
    {
        inflate(context, R.layout.main_register, this);

        txtPlayerName = (TextView)findViewById(R.id.txtPlayerNameRegister);
        btnAvatar = (LinearLayout) findViewById(R.id.btnAvatar);
        btnPrivacyZones = (LinearLayout) findViewById(R.id.btnPrivacyZones);
        btnRegister = (LinearLayout) findViewById(R.id.btnRegister);
        imgAvatar = (ImageView)findViewById(R.id.imgAvatar);

        txtPlayerName.setText(strPlayerName);

        btnAvatar.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Utilities.DismissKeyboard(activity, activity.getCurrentFocus());
                strPlayerName = txtPlayerName.getText().toString();
                activity.SetView(new UploadAvatarView(game, activity, LaunchUICommon.AvatarPurpose.PLAYER));
            }
        });

        btnPrivacyZones.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Utilities.DismissKeyboard(activity, activity.getCurrentFocus());
                activity.PrivacyZoneMode();
            }
        });

        btnRegister.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Utilities.DismissKeyboard(activity, activity.getCurrentFocus());

                //Clear any existing errors.
                txtPlayerName.setError(null);

                //Get the values.
                String strPlayer = txtPlayerName.getText().toString();

                if(Utilities.ValidateName(strPlayer))
                {
                    game.Register(strPlayer, lAvatarID);
                }
                else
                {
                    txtPlayerName.setError(context.getString(R.string.specify_username));
                }
            }
        });

        if(lAvatarID != ClientDefs.DEFAULT_AVATAR_ID)
        {
            imgAvatar.setImageBitmap(AvatarBitmaps.GetNeutralPlayerAvatar(activity, game, lAvatarID));
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
                txtPlayerName.setError(context.getString(R.string.username_taken));
            }
        });
    }
}
