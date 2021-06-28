package com.apps.fast.launch.launchviews;

/**
 * Created by tobster on 16/10/15.
 */
public class CloseAccountView// extends LaunchView
{
    /*private TextView txtPassword;

    public CloseAccountView(LaunchClient launchClient, MainActivity activity)
    {
        super(launchClient, activity);
    }

    @Override
    protected void Setup()
    {
        inflate(context, R.layout.view_close_account, this);

        txtPassword = (TextView)findViewById(R.id.txtPassword);

        findViewById(R.id.btnConfirm).setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                //Clear any existing errors.
                txtPassword.setError(null);

                //Get the values.
                String strPassword = txtPassword.getText().toString();

                //Validate the general validity of the password.
                if(Security.ValidatePasswordIntegrity(strPassword))
                {
                    launchClient.CloseAccount(strPassword);
                }
                else
                {
                    //Password too short.
                    txtPassword.setError(context.getString(R.string.passwordtooshort));
                }
            }
        });
    }

    @Override
    public void Update()
    {

    }

    public void BadCredentials()
    {
        activity.runOnUiThread(new Runnable()
        {
            public void run()
            {
                txtPassword.setError(context.getString(R.string.bad_password));
                txtPassword.setText(null);
            }
        });
    }*/
}
