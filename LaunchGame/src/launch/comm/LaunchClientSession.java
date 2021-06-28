/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.comm;

import java.net.Socket;
import java.nio.ByteBuffer;
import launch.comm.clienttasks.Task;
import launch.comm.clienttasks.UploadAvatarTask;
import launch.game.Alliance;
import launch.game.Config;
import launch.game.Defs;
import launch.game.LaunchClientGameInterface;
import launch.game.LaunchGame;
import launch.game.User;
import launch.game.treaties.*;
import launch.game.entities.*;
import launch.game.treaties.Affiliation;
import launch.game.treaties.War;
import launch.utilities.LaunchEvent;
import launch.utilities.LaunchClientLocation;
import launch.utilities.LaunchLog;
import launch.utilities.LaunchReport;
import launch.utilities.LaunchUtilities;
import launch.utilities.ShortDelay;
import tobcomm.TobComm;

/**
 *
 * @author tobster
 */
public class LaunchClientSession extends LaunchSession
{
    private static final int UPDATE_INTERVAL = 5000;
    private static final int CLIENT_TIMEOUT = 15000;
    private static final short IMAGE_DOWNLOAD_INTERVAL = 500; //To space out image downloads so network/UI performance isn't impacted.
    
    private enum State
    {
        LOCATION_WAIT,
        AUTHORISE,
        READY,
        PROCESSING_TASK,
        CLOSED,
    }
    
    private LaunchClientGameInterface gameInterface;
    private State state;
    
    private byte[] cDeviceID;
    private String strDeviceName;
    private String strProcessName;
    private boolean bMobile;
    
    private Task currentTask;
    private ShortDelay dlyUpdate = new ShortDelay();
    private int lLatency;
    
    private boolean bDownloadingAvatar = false;
    private boolean bDownloadingImage = false;
    
    private boolean bDownloadingMissingConfig = false;
    
    private boolean bReceivingSnapshot = false;
    
    private int lOurPlayerID;
    
    private ShortDelay dlyImageDownload = new ShortDelay();
        
    public LaunchClientSession(Socket socket, LaunchClientGameInterface gameInterface, byte[] cDeviceID, String strDeviceName, String strProcessName, boolean bMobile)
    {
        super(socket);
        
        this.gameInterface = gameInterface;
        this.cDeviceID = cDeviceID;
        this.strDeviceName = strDeviceName;
        this.strProcessName = strProcessName;
        this.bMobile = bMobile;
        this.state = State.LOCATION_WAIT;
        
        Start();
    }

    @Override
    protected void Process()
    {
        if(IsAlive())
        {
            switch(state)
            {
                case LOCATION_WAIT:
                {
                    //Waiting for location from game. We cannot progress if it's not available.
                    if(gameInterface.PlayerLocationAvailable())
                    {
                        Authenticate();
                    }
                    else if(gameInterface.GetGameConfigChecksum() == LaunchEntity.ID_NONE)
                    {
                        //Otherwise, try to download the config if it's missing.
                        if(!bDownloadingMissingConfig)
                        {
                            bDownloadingMissingConfig = true;
                            tobComm.RequestObject(Config, 0, 0, 0);
                        }
                    }
                }
                break;
                
                case AUTHORISE:
                {
                    //Waiting for authorisation.
                }
                break;
                
                case READY:
                {
                    //Idle. We can process tasks.
                }
                break;
                
                case PROCESSING_TASK:
                {
                    //Busy. We cannot accept any tasks. Revert to ready when the current task finishes.
                    if(currentTask.Complete())
                    {
                        state = State.READY;
                    }
                }
                break;
                
                case CLOSED:
                {
                    //We are formally dead and should do nothing.
                }
                break;
            }
            
            if(dlyUpdate.Expired() &&
               !gameInterface.ClosingAccount()) //Suppress when closing account, so it doesn't inadvertantly fire a location update and therefore bring the player back out of AWOL.
            {
                //Reset latency measurement.
                lLatency = 0;
                
                if (gameInterface.GetReadyToUpdatePlayer()) //Suppress before snapshot obtained, to prevent privacy zone spoofing.
                {
                    if(state == State.READY || state == State.PROCESSING_TASK)
                    {
                        //Authed. Send player's location.
                        LaunchClientLocation location = gameInterface.GetPlayerLocation();
                        tobComm.SendObject(LocationUpdate, location.GetData());
                    }
                    else
                    {
                        //Not authed. Send a keepalive.
                        tobComm.SendCommand(KeepAlive);
                    }
                }
                else
                {
                    //Send keepalive to keep connection open while downloading snapshot.
                    tobComm.SendCommand(KeepAlive);
                }
                
                dlyUpdate.Set(UPDATE_INTERVAL);
            }
        }
    }

    @Override
    public void Tick(int lMS)
    {
        super.Tick(lMS);
        
        dlyImageDownload.Tick(lMS);
        
        if(IsAlive())
        {
            lLatency += lMS;
            
            //Periodically send the player's location (or a keepalive). This keeps the player's location up to date, and keeps the session alive.
            dlyUpdate.Tick(lMS);
        }
    }

    @Override
    public void Close()
    {
        super.Close();
        state = State.CLOSED;
        gameInterface.SetLatency(Defs.LATENCY_DISCONNECTED);
    }
    
    private void Authenticate()
    {
        state = State.AUTHORISE;

        byte cAuthFlags = 0x00;

        if(bMobile)
            cAuthFlags |= Defs.AUTH_FLAG_MOBILE;

        ByteBuffer bb = ByteBuffer.allocate(cDeviceID.length + 3 + LaunchUtilities.GetStringDataSize(strDeviceName) + LaunchUtilities.GetStringDataSize(strProcessName));
        bb.put(cDeviceID);
        bb.putShort(Defs.MAJOR_VERSION);
        bb.put(LaunchUtilities.GetStringData(strDeviceName));
        bb.put(LaunchUtilities.GetStringData(strProcessName));
        bb.put(cAuthFlags);

        tobComm.SendObject(Authorise, bb.array());
    }
    
    private void Authenticated()
    {
        state = State.READY;
        gameInterface.Authenticated();
        
        //Enable object sync, to process the incoming snapshot on one thread (instead of one thread for every freakin' object).
        tobComm.ObjectSyncEnable();
        tobComm.RequestObject(GameSnapshot);
                
        //Reset update interval for quick player refresh.
        dlyUpdate.ForceExpiry();
    }
    
    public boolean CanAcceptTask()
    {
        //Can we accept a task?
        if(state == State.READY)
            return true;

        //Can we accept a register or upload avatar task, which is a special case because we're still in Authorise state?
        if(state == State.AUTHORISE)
        {
            if(currentTask != null)
            {
                return currentTask.Complete();
            }
            
            /* TO DO: Remove as dead.
            if(currentTask instanceof RegisterTask || currentTask instanceof UploadAvatarTask)
            {
                //If we're on a register task but it is complete, it's a failed remnant of a previous registration attempt, so we can attempt another.
                return currentTask.Complete();
            }*/
            
            //We have no task, and can accept one.
            return true;
        }
        
        //We cannot accept a task.
        return false;
    }
    
    public boolean CanDownloadAnImage()
    {
        return state == State.READY && !bDownloadingAvatar && !bDownloadingImage && dlyImageDownload.Expired();
    }
    
    public void DownloadAvatar(int lAvatarID)
    {
        dlyImageDownload.Set(IMAGE_DOWNLOAD_INTERVAL);
        bDownloadingAvatar = true;
        tobComm.RequestObject(Avatar, lAvatarID);
    }
    
    public void DownloadImage(int lImageID)
    {
        dlyImageDownload.Set(IMAGE_DOWNLOAD_INTERVAL);
        bDownloadingImage = true;
        tobComm.RequestObject(ImgAsset, lImageID);
    }
    
    public void StartTask(Task task)
    {
        if(state != State.AUTHORISE)
        {
            //Only progress to processing task when authorised, so the registration task doesn't send us doolally.
            state = State.PROCESSING_TASK;
        }
        
        currentTask = task;
        currentTask.Start(tobComm);
    }
    
    private boolean UploadingAvatar()
    {
        if(currentTask != null)
        {
            if(!currentTask.Complete() && currentTask instanceof UploadAvatarTask)
            {
                return true;
            }
        }
        
        return false;
    }

    @Override
    public void ObjectReceived(int lObject, int lInstanceNumber, int lOffset, byte[] cData)
    {
        ByteBuffer bb = ByteBuffer.wrap(cData);
        
        switch(lObject)
        {
            case Authorise:
            {
                short nMajorVersion = bb.getShort();
                short nMinorVersion = bb.getShort();
                int lConfigChecksum = bb.getInt();
                int lPlayerID = bb.getInt();
                
                //Store the player ID and notify the game interface of the player ID.
                lOurPlayerID = lPlayerID;
                gameInterface.SetOurPlayerID(lPlayerID);
                
                if(gameInterface.VerifyVersion(nMajorVersion, nMinorVersion))
                {
                    if(lConfigChecksum != gameInterface.GetGameConfigChecksum())
                    {
                        tobComm.RequestObject(Config);
                    }
                    else
                    {
                        Authenticated();
                    }
                }
            }
            break;

            case UserData:
            {
                User user = new User(bb);
                gameInterface.ReceiveUser(user);
            }
            break;
            
            case BanData:
            {
                long oDuration = bb.getLong();
                String strBanReason = LaunchUtilities.StringFromData(bb);
                
                gameInterface.TempBanned(strBanReason, oDuration);
            }
            break;

            case PermBanData:
            {
                String strBanReason = LaunchUtilities.StringFromData(bb);
                
                gameInterface.PermBanned(strBanReason);
            }
            break;
            
            case Event:
            {
                gameInterface.EventReceived(new LaunchEvent(ByteBuffer.wrap(cData)));
            }
            break;
            
            case Report:
            {
                gameInterface.ReportReceived(new LaunchReport(ByteBuffer.wrap(cData)));

                //Ack the report if outside of snapshot.
                if(!bReceivingSnapshot)
                {
                    tobComm.SendCommand(ReportAck);
                }
            }
            break;
            
            case Config:
            {
                gameInterface.SetConfig(new Config(cData));

                if(!bDownloadingMissingConfig)
                    Authenticated();
                bDownloadingMissingConfig = false;
            }
            break;
            
            case Avatar:
            {
                if(UploadingAvatar())
                {
                    currentTask.HandleObject(lObject, lInstanceNumber, lOffset, cData);
                }
                else
                {
                    gameInterface.AvatarReceived(lInstanceNumber, cData);
                    bDownloadingAvatar = false;
                }
            }
            break;
            
            case Player:
            {
                gameInterface.ReceivePlayer(new Player(bb, lOurPlayerID));
            }
            break;
            
            case Missile:
            {
                gameInterface.ReceiveMissile(new Missile(bb));
            }
            break;
            
            case Interceptor:
            {
                gameInterface.ReceiveInterceptor(new Interceptor(bb));
            }
            break;
            
            case MissileSite:
            {
                gameInterface.ReceiveMissileSite(new MissileSite(bb, lOurPlayerID));
            }
            break;
            
            case SamSite:
            {
                gameInterface.ReceiveSAMSite(new SAMSite(bb, lOurPlayerID));
            }
            break;
            
            case OreMine:
            {
                gameInterface.ReceiveOreMine(new OreMine(bb, lOurPlayerID));
            }
            break;
            
            case SentryGun:
            {
                gameInterface.ReceiveSentryGun(new SentryGun(bb, lOurPlayerID));
            }
            break;
            
            case Loot:
            {
                gameInterface.ReceiveLoot(new Loot(bb));
            }
            break;
            
            case Radiation:
            {
                gameInterface.ReceiveRadiation(new Radiation(bb));
            }
            break;
            
            case AllianceMinor:
            {
                gameInterface.ReceiveAlliance(new Alliance(bb), false);
            }
            break;
            
            case AllianceMajor:
            {
                gameInterface.ReceiveAlliance(new Alliance(bb), true);
            }
            break;
            
            case Treaty:
            {
                byte cType = bb.get();
                
                Treaty treaty = null;
                
                switch(launch.game.treaties.Treaty.Type.values()[cType])
                {
                    case WAR: treaty = new War(bb); break;
                    case AFFILIATION: treaty = new Affiliation(bb); break;
                    case AFFILIATION_REQUEST: treaty = new AffiliationRequest(bb); break;
                }

                gameInterface.ReceiveTreaty(treaty);
            }
            break;
            
            case FullPlayerStats:
            {
                gameInterface.ReceivePlayer(new Player(bb, lOurPlayerID));
            }
            break;
            
            case ImgAsset:
            {
                gameInterface.ImageReceived(lInstanceNumber, cData);
                bDownloadingImage = false;
            }
            break;
            
            default:
            {
                //The current task must handle this object.
                if(currentTask != null)
                    currentTask.HandleObject(lObject, lInstanceNumber, lOffset, cData);
            }
        }
    }

    @Override
    public void CommandReceived(int lCommand, int lInstanceNumber)
    {
        switch(lCommand)
        {
            case AccountUnregistered:
            {
                //Notify game client unregistered.
                gameInterface.AccountUnregistered();
            }
            break;
            
            case MajorVersionInvalid:
            {
                //Notify game client version wrong.
                gameInterface.MajorVersionInvalid();
            }
            break;
            
            case AccountCreateSuccess:
            {
                //Immediately authorise.
                Authenticate();
                
                //Notify task so it can die.
                currentTask.HandleCommand(lCommand, lInstanceNumber);
            }
            break;
            
            case KeepAlive:
            {
                //Notify game of latency. Only when fully authed, as this will override the snapshot download speed display.
                if((state == State.READY || state == State.PROCESSING_TASK) && !bReceivingSnapshot)
                {
                    gameInterface.SetLatency(lLatency);
                }
            }
            break;
            
            case SnapshotBegin:
            {
                bReceivingSnapshot = true;
                gameInterface.SnapshotBegin();
                gameInterface.SetLatency(Defs.LATENCY_DISCONNECTED);
            }
            break;
            
            case SnapshotComplete:
            {
                //Just flush the sync buffer and process the objects. We will ack once the thread has processed them all.
                tobComm.ObjectSyncFlush();
            }
            break;
            
            case RemovePlayer:
            {
                gameInterface.RemovePlayer(lInstanceNumber);
            }
            break;
            
            case RemoveMissile:
            {
                gameInterface.RemoveMissile(lInstanceNumber);
            }
            break;
            
            case RemoveInterceptor:
            {
                gameInterface.RemoveInterceptor(lInstanceNumber);
            }
            break;
            
            case RemoveMissileSite:
            {
                gameInterface.RemoveMissileSite(lInstanceNumber);
            }
            break;
            
            case RemoveSAMSite:
            {
                gameInterface.RemoveSAMSite(lInstanceNumber);
            }
            break;
            
            case RemoveSentryGun:
            {
                gameInterface.RemoveSentryGun(lInstanceNumber);
            }
            break;
            
            case RemoveOreMine:
            {
                gameInterface.RemoveOreMine(lInstanceNumber);
            }
            break;
            
            case RemoveLoot:
            {
                gameInterface.RemoveLoot(lInstanceNumber);
            }
            break;
            
            case RemoveRadiation:
            {
                gameInterface.RemoveRadiation(lInstanceNumber);
            }
            break;
            
            case RemoveAlliance:
            {
                gameInterface.RemoveAlliance(lInstanceNumber);
            }
            break;
            
            case RemoveTreaty:
            {
                gameInterface.RemoveWar(lInstanceNumber);
            }
            break;
            
            case ImageError:
            {
                bDownloadingAvatar = false;
                bDownloadingImage = false;
            }
            break;
            
            default:
            {
                //The current task must handle this command.
                if(currentTask != null)
                    currentTask.HandleCommand(lCommand, lInstanceNumber);
            }
        }
    }

    @Override
    public void ObjectRequested(int lObject, int lInstanceNumber, int lOffset, int lLength)
    {
        switch(lObject)
        {
            case ProcessNames:
            {
                tobComm.SendObject(ProcessNames, LaunchUtilities.GetStringData(gameInterface.GetProcessNames()));
            }
            break;
            
            case DeviceCheck:
            {
                tobComm.SendObject(ProcessNames, LaunchUtilities.GetStringData(gameInterface.GetProcessNames()));
                gameInterface.DeviceCheckRequested();
            }
            break;
            
            default:
            {
                //Shouldn't happen.
            }
        }
    }

    @Override
    public void SyncObjectsProcessed()
    {
        //The sync thread has processed all snapshot objects. Now we can ack.
        bReceivingSnapshot = false;
        tobComm.SendCommand(SnapshotAck, 0);
        gameInterface.SnapshotFinish();
    }
    
    public String GetState()
    {
        return state.name();
    }
    
    /**
     * For experimental direct communications.
     * @return The TobComm instance.
     */
    public TobComm GetTobComm()
    {
        return tobComm;
    }

    @Override
    protected int GetTimeout()
    {
        return CLIENT_TIMEOUT;
    }
}
