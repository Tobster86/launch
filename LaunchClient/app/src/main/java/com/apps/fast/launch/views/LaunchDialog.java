package com.apps.fast.launch.views;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.apps.fast.launch.R;

/**
 * Created by tobster on 03/09/16.
 */
public class LaunchDialog extends DialogFragment
{
    private LinearLayout lytContent;

    private TextView txtMessage = null;

    private ProgressBar progressBar;

    private TextView btnYes;
    private TextView btnOk;
    private TextView btnCancel;
    private TextView btnNo;

    private String strMessage = "";

    private boolean bProgressBarEnabled = false;

    private View.OnClickListener onClickYes = null;
    private View.OnClickListener onClickOk = null;
    private View.OnClickListener onClickCancel = null;
    private View.OnClickListener onClickNo = null;

    private boolean bHeaderLaunch = false;
    private boolean bHeaderComms = false;
    private boolean bHeaderConstruct = false;
    private boolean bHeaderPurchase = false;
    private boolean bHeaderDiplomacy = false;
    private boolean bHeaderHealth = false;
    private boolean bHeaderSAMControl = false;
    private boolean bHeaderOnOff = false;

    public void SetMessage(String strMessage)
    {
        this.strMessage = strMessage;

        if(txtMessage != null)
        {
            txtMessage.setText(strMessage);
        }
    }

    public void SetOnClickYes(View.OnClickListener onClickListener)
    {
        onClickYes = onClickListener;

        if(btnYes != null)
        {
            btnYes.setOnClickListener(onClickYes);
            btnYes.setVisibility(View.VISIBLE);
        }
    }

    public void SetOnClickOk(View.OnClickListener onClickListener)
    {
        onClickOk = onClickListener;

        if(btnOk != null)
        {
            btnOk.setOnClickListener(onClickOk);
            btnOk.setVisibility(View.VISIBLE);
        }
    }

    public void SetOnClickCancel(View.OnClickListener onClickListener)
    {
        onClickCancel = onClickListener;

        if(btnCancel != null)
        {
            btnCancel.setOnClickListener(onClickCancel);
            btnCancel.setVisibility(View.VISIBLE);
        }
    }

    public void SetOnClickNo(View.OnClickListener onClickListener)
    {
        onClickNo = onClickListener;

        if(btnNo != null)
        {
            btnNo.setOnClickListener(onClickNo);
            btnNo.setVisibility(View.VISIBLE);
        }
    }

    public void SetEnableProgressSpinner()
    {
        bProgressBarEnabled = true;

        if(progressBar != null)
        {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        View us = getActivity().getLayoutInflater().inflate(R.layout.launch_dialog, null);

        lytContent = (LinearLayout)us.findViewById(R.id.lytContent);

        txtMessage = (TextView)us.findViewById(R.id.txtMessage);

        progressBar = (ProgressBar)us.findViewById(R.id.progressBar);

        btnYes = (TextView)us.findViewById(R.id.btnYes);
        btnOk = (TextView)us.findViewById(R.id.btnOk);
        btnCancel = (TextView)us.findViewById(R.id.btnCancel);
        btnNo = (TextView)us.findViewById(R.id.btnNo);

        txtMessage.setText(strMessage);

        if(bProgressBarEnabled)
        {
            progressBar.setVisibility(View.VISIBLE);
        }

        if(onClickYes != null)
        {
            btnYes.setOnClickListener(onClickYes);
            btnYes.setVisibility(View.VISIBLE);
        }

        if(onClickOk != null)
        {
            btnOk.setOnClickListener(onClickOk);
            btnOk.setVisibility(View.VISIBLE);
        }

        if(onClickCancel != null)
        {
            btnCancel.setOnClickListener(onClickCancel);
            btnCancel.setVisibility(View.VISIBLE);
        }

        if(onClickNo != null)
        {
            btnNo.setOnClickListener(onClickNo);
            btnNo.setVisibility(View.VISIBLE);
        }

        us.findViewById(R.id.imgLaunch).setVisibility(bHeaderLaunch ? View.VISIBLE : View.GONE);
        us.findViewById(R.id.imgComms).setVisibility(bHeaderComms ? View.VISIBLE : View.GONE);
        us.findViewById(R.id.imgConstruct).setVisibility(bHeaderConstruct ? View.VISIBLE : View.GONE);
        us.findViewById(R.id.imgPurchase).setVisibility(bHeaderPurchase ? View.VISIBLE : View.GONE);
        us.findViewById(R.id.imgDiplomacy).setVisibility(bHeaderDiplomacy ? View.VISIBLE : View.GONE);
        us.findViewById(R.id.imgHealth).setVisibility(bHeaderHealth ? View.VISIBLE : View.GONE);
        us.findViewById(R.id.imgSAMControl).setVisibility(bHeaderSAMControl ? View.VISIBLE : View.GONE);
        us.findViewById(R.id.imgOnOff).setVisibility(bHeaderOnOff ? View.VISIBLE : View.GONE);

        builder.setView(us);

        return builder.create();
    }

    public void SetHeaderLaunch()
    {
        bHeaderLaunch = true;
    }

    public void SetHeaderComms()
    {
        bHeaderComms = true;
    }

    public void SetHeaderConstruct()
    {
        bHeaderConstruct = true;
    }

    public void SetHeaderPurchase()
    {
        bHeaderPurchase = true;
    }

    public void SetHeaderDiplomacy()
    {
        bHeaderDiplomacy = true;
    }

    public void SetHeaderHealth()
    {
        bHeaderHealth = true;
    }

    public void SetHeaderSAMControl()
    {
        bHeaderSAMControl = true;
    }

    public void SetHeaderOnOff()
    {
        bHeaderOnOff = true;
    }

    public void AddContent(View view)
    {
        lytContent.addView(view);
    }
}
