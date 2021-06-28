package com.apps.fast.launch.launchviews;

import android.Manifest;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.TextView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.activities.MainActivity;

import java.util.ArrayList;
import java.util.List;

import launch.game.LaunchClientGame;

import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;

/**
 * Created by tobster on 15/07/16.
 */
public class PermissionsView extends LaunchView
{
    public PermissionsView(LaunchClientGame game, MainActivity activity)
    {
        super(game, activity);
    }

    @Override
    protected void Setup()
    {
        inflate(context, R.layout.main_permissions, this);

        findViewById(R.id.btnQuit).setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                activity.finish();
            }
        });

        findViewById(R.id.btnEnablePermissions).setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                List<String> Permissions = new ArrayList<>();

                if(ContextCompat.checkSelfPermission(context, Manifest.permission.INTERNET) != PERMISSION_GRANTED)
                    Permissions.add(Manifest.permission.INTERNET);

                if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_NETWORK_STATE) != PERMISSION_GRANTED)
                    Permissions.add(Manifest.permission.ACCESS_NETWORK_STATE);

                if(ContextCompat.checkSelfPermission(context, Manifest.permission.VIBRATE) != PERMISSION_GRANTED)
                    Permissions.add(Manifest.permission.VIBRATE);

                if(ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED)
                    Permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

                if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PERMISSION_GRANTED)
                    Permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);

                if(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PERMISSION_GRANTED)
                    Permissions.add(Manifest.permission.READ_PHONE_STATE);

                if(ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_BOOT_COMPLETED) != PERMISSION_GRANTED)
                    Permissions.add(Manifest.permission.RECEIVE_BOOT_COMPLETED);

                ActivityCompat.requestPermissions(activity, Permissions.toArray(new String[Permissions.size()]), 1);
            }
        });



        Refresh();
    }

    @Override
    public void Update()
    {
    }

    public void PermissionsUpdated()
    {
        Refresh();
    }

    public void Refresh()
    {
        TextView txtInternetGranted = findViewById(R.id.txt_internet_granted);
        TextView txtInternetNotGranted = findViewById(R.id.txt_internet_not_granted);
        TextView txtNetworkGranted = findViewById(R.id.txt_network_granted);
        TextView txtNetworkNotGranted = findViewById(R.id.txt_network_not_granted);
        TextView txtVibrateGranted = findViewById(R.id.txt_vibrate_granted);
        TextView txtVibrateNotGranted = findViewById(R.id.txt_vibrate_not_granted);
        TextView txtStorageGranted = findViewById(R.id.txt_storage_granted);
        TextView txtStorageNotGranted = findViewById(R.id.txt_storage_not_granted);
        TextView txtLocationGranted = findViewById(R.id.txt_location_granted);
        TextView txtLocationNotGranted = findViewById(R.id.txt_location_not_granted);
        TextView txtPhoneGranted = findViewById(R.id.txt_phone_granted);
        TextView txtPhoneNotGranted = findViewById(R.id.txt_phone_not_granted);
        TextView txtRebootGranted = findViewById(R.id.txt_reboot_granted);
        TextView txtRebootNotGranted = findViewById(R.id.txt_reboot_not_granted);

        boolean bInternetGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.INTERNET) == PERMISSION_GRANTED;
        boolean bNetworkGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_NETWORK_STATE) == PERMISSION_GRANTED;
        boolean bVibrateGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.VIBRATE) == PERMISSION_GRANTED;
        boolean bStorageGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED;
        boolean bLocationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PERMISSION_GRANTED;
        boolean bPhoneGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PERMISSION_GRANTED;
        boolean bRebootGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_BOOT_COMPLETED) == PERMISSION_GRANTED;

        txtInternetGranted.setVisibility(bInternetGranted ? VISIBLE : GONE);
        txtInternetNotGranted.setVisibility(!bInternetGranted ? VISIBLE : GONE);
        txtNetworkGranted.setVisibility(bNetworkGranted ? VISIBLE : GONE);
        txtNetworkNotGranted.setVisibility(!bNetworkGranted ? VISIBLE : GONE);
        txtVibrateGranted.setVisibility(bVibrateGranted ? VISIBLE : GONE);
        txtVibrateNotGranted.setVisibility(!bVibrateGranted ? VISIBLE : GONE);
        txtStorageGranted.setVisibility(bStorageGranted ? VISIBLE : GONE);
        txtStorageNotGranted.setVisibility(!bStorageGranted ? VISIBLE : GONE);
        txtLocationGranted.setVisibility(bLocationGranted ? VISIBLE : GONE);
        txtLocationNotGranted.setVisibility(!bLocationGranted ? VISIBLE : GONE);
        txtPhoneGranted.setVisibility(bPhoneGranted ? VISIBLE : GONE);
        txtPhoneNotGranted.setVisibility(!bPhoneGranted ? VISIBLE : GONE);
        txtRebootGranted.setVisibility(bRebootGranted ? VISIBLE : GONE);
        txtRebootNotGranted.setVisibility(!bRebootGranted ? VISIBLE : GONE);
    }
}
