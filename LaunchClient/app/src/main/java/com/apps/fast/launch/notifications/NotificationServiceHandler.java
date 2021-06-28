package com.apps.fast.launch.notifications;

import android.content.Context;
import android.content.SharedPreferences;

import com.apps.fast.launch.components.ClientDefs;
import com.apps.fast.launch.components.Utilities;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import launch.comm.LaunchSession;
import launch.utilities.LaunchLog;
import launch.utilities.ShortDelay;
import tobcomm.TobComm;
import tobcomm.TobCommInterface;

import static launch.utilities.LaunchLog.LogType.SERVICES;

public class NotificationServiceHandler implements TobCommInterface
{
    //While the alert service is based on LaunchClientComms, these values should be different to be less burdensome to the device's comms stack.
    private static final int TICK_RATE_ALERT_SERVICE = 100;
    private static final int TIMEOUT = 5000;
    private static final int MESSAGE_BUFFER_SIZE = 10240;

    private Context context;

    private enum State
    {
        CONNECT,
        CONNECTING,
        PROCESS
    }

    private Socket socket;
    private InputStream in;
    private OutputStream out;

    private ShortDelay dlyTimeout = new ShortDelay();
    private long oIdleTime;

    private TobComm tobComm;

    private State state = State.CONNECT;

    private static int lDebugInstance = 0;

    private ScheduledExecutorService seCommsService;

    public NotificationServiceHandler(Context context)
    {
        this.context = context;
    }

    public void Start()
    {
        final byte cEncryptedDeviceID[] = Utilities.GetEncryptedDeviceID(context);
        final NotificationServiceHandler self = this;
        final int lInstance = lDebugInstance++;

            //Establish a new socket connection to the server.
            dlyTimeout.Set(TIMEOUT);

            seCommsService = Executors.newScheduledThreadPool(1);

            seCommsService.scheduleAtFixedRate(new Runnable()
            {
                @Override
                public void run()
                {
                    switch(state)
                    {
                        case CONNECT:
                        {
                            try
                            {
                                SharedPreferences sharedPreferences = context.getSharedPreferences(ClientDefs.SETTINGS, Context.MODE_PRIVATE);
                                String strURL = sharedPreferences.getString(ClientDefs.SETTINGS_SERVER_URL, ClientDefs.SETTINGS_SERVER_URL_DEFAULT);
                                int lPort = sharedPreferences.getInt(ClientDefs.SETTINGS_SERVER_PORT, ClientDefs.GetDefaultServerPort());

                                LaunchLog.Log(SERVICES, "AlertService", String.format("(%d) Opening socket...", lInstance));
                                socket = new Socket(InetAddress.getByName(strURL), lPort);
                                state = State.CONNECTING;
                            }
                            catch (Exception ex)
                            {
                                //Didn't work. Forget it for now.
                                LaunchLog.Log(SERVICES, "AlertService", String.format("(%d) Could not open socket.", lInstance));
                                Finish();
                            }
                        }
                        break;

                        case CONNECTING:
                        {
                            dlyTimeout.Tick(TICK_RATE_ALERT_SERVICE);

                            if(dlyTimeout.Expired())
                            {
                                //Timeout. Shut down.
                                LaunchLog.Log(SERVICES, "AlertService", String.format("(%d) Socket did not open in time. Aborting", lInstance));
                                Finish();
                            }
                            else if(socket != null)
                            {
                                if(socket.isConnected())
                                {
                                    //Get streams, create tobComm instance, and start processing.
                                    try
                                    {
                                        in = socket.getInputStream();
                                        out = socket.getOutputStream();
                                        tobComm = new TobComm(self);
                                        LaunchLog.Log(SERVICES, "AlertService", String.format("(%d) Got the socket and streams. Requesting...", lInstance));

                                        //Send our single alert status request.
                                        tobComm.SendObject(LaunchSession.AlertStatus, cEncryptedDeviceID);

                                        state = State.PROCESS;
                                    }
                                    catch(Exception ex)
                                    {
                                        //Something went wrong. Abort.
                                        LaunchLog.Log(SERVICES, "AlertService", String.format("(%d) Streams and that failed.", lInstance));
                                        Finish();
                                    }
                                }
                            }
                        }
                        break;

                        case PROCESS:
                        {
                            try
                            {
                                if(in.available() > 0)
                                {
                                    oIdleTime = 0;
                                    byte[] cMessage = new byte[MESSAGE_BUFFER_SIZE];
                                    int lRead = in.read(cMessage);

                                    if(lRead > 0)
                                    {
                                        tobComm.ProcessBytes(Arrays.copyOfRange(cMessage, 0, lRead));
                                    }
                                }

                                if(oIdleTime > TIMEOUT)
                                {
                                    //Session timed out.
                                    LaunchLog.Log(SERVICES, "AlertService", String.format("(%d) Finishing due to timeout.", lInstance));
                                    Finish();
                                }
                            }
                            catch(IOException ex)
                            {
                                LaunchLog.Log(SERVICES, "AlertService", String.format("(%d) Finishing due to IO error.", lInstance));
                                //Session errored.
                                Finish();
                            }
                        }
                        break;
                    }
                }
            }, 0, TICK_RATE_ALERT_SERVICE, TimeUnit.MILLISECONDS);
    }

    public void Finish()
    {
        LaunchLog.Log(SERVICES, "AlertService", "Finished.");
        if(seCommsService != null)
        {
            LaunchLog.Log(SERVICES, "AlertService", "Shutting down the comms service.");
            seCommsService.shutdown();
        }

        if(socket != null)
        {
            try
            {
                socket.close();
            }
            catch(Exception ex) { /* Don't care. */ }

            socket = null;
        }
    }

    @Override
    public void Error(String strErrorText)
    {
        LaunchLog.Log(SERVICES, "AlertService", "TobComm error!");
        Finish();
    }

    @Override
    public void BytesToSend(byte[] cData)
    {
        try
        {
            out.write(cData);
        }
        catch (IOException ex)
        {
            //Output stream errored.
            LaunchLog.Log(SERVICES, "AlertService", "Bytes to send IO error.");
            Finish();
        }
    }

    @Override
    public void ObjectReceived(int lObject, int lInstanceNumber, int lOffset, byte[] cData)
    {
        //We should not receive any objects.
        LaunchLog.Log(SERVICES, "AlertService", "Unexpectedly received an object.");
        Finish();
    }

    @Override
    public void CommandReceived(int lCommand, int lInstanceNumber)
    {
        LaunchLog.Log(SERVICES, "AlertService", "Got the result.");

        //We only actually need to do anything if we get an alert. In all other cases (or once the alert is processed), we're done.
        if(lCommand == LaunchSession.AlertUnderAttack)
        {
            Utilities.DebugLog(context, "AlertService", "Under attack!");
            LaunchLog.Log(SERVICES, "AlertService", "WE'RE UNDER ATTAAAAACK!");
            LaunchAlertManager.FireAlert(context, false, false);
        }
        else if(lCommand == LaunchSession.AlertNukeEscalation)
        {
            Utilities.DebugLog(context, "AlertService","Nuclear escalation!");
            LaunchLog.Log(SERVICES, "AlertService", "NYOOOOOOKS!");
            LaunchAlertManager.FireAlert(context, true, false);
        }
        else if(lCommand == LaunchSession.AlertAllyUnderAttack)
        {
            Utilities.DebugLog(context, "AlertService","Ally under attack!");
            LaunchLog.Log(SERVICES, "AlertService", "OUR ALLY IS UNDER ATTAAAAACK!");
            LaunchAlertManager.FireAlert(context, false, true);
        }
        else if(lCommand == LaunchSession.AlertAllClear)
        {
            Utilities.DebugLog(context, "AlertService","All clear");
        }
        else
        {
            Utilities.DebugLog(context, "AlertService","Something else happened. Bit wierd. Tell Tobster!");
        }

        Finish();
    }

    @Override
    public void ObjectRequested(int lObject, int lInstanceNumber, int lOffset, int lLength)
    {
        //Should not happen.
        LaunchLog.Log(SERVICES, "AlertService", "Object unexpectedly requested.");
        Finish();
    }

    @Override
    public void SyncObjectsProcessed()
    {
        //The notification service doesn't use this feature.
    }
}
