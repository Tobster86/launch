/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.comm;

import java.net.InetAddress;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import static launch.comm.LaunchComms.LOG_NAME;
import launch.comm.clienttasks.*;
import launch.comm.clienttasks.Task.StructureType;
import launch.game.Defs;
import launch.game.GeoCoord;
import launch.game.LaunchClientGameInterface;
import launch.game.treaties.Treaty;
import launch.utilities.LaunchLog;
import static launch.utilities.LaunchLog.LogType.COMMS;
import launch.utilities.ShortDelay;
import tobcomm.TobComm;

/**
 *
 * @author tobster
 */
public class LaunchClientComms extends LaunchComms
{
    private static final int CONNECT_DWELL_TIME = 500;
    private static final int RETRY_DWELL_TIME = 2000;
    
    private enum State
    {
        OFFLINE,
        CONNECT,
        CONNECTING,
        PROCESS,
        REINIT
    }
    
    private State state = State.OFFLINE;
            
    private LaunchClientGameInterface gameInterface;
    private String strURL;
    private int lPort;
    
    private LaunchClientSession session;
    
    private byte[] cDeviceID;
    private String strDeviceName;
    private String strProcessName;
    
    private Task currentTask;
    
    private ShortDelay dlyReinit = new ShortDelay();
    
    private Queue<Integer> AvatarDownloadQueue = new LinkedList<>();
    private Queue<Integer> ImageDownloadQueue = new LinkedList<>();
    
    private Socket socket;
    
    public LaunchClientComms(LaunchClientGameInterface gameInterface, String strURL, int lPort)
    {
        this.gameInterface = gameInterface;
        this.strURL = strURL;
        this.lPort = lPort;
    }
    
    public void SetDeviceID(byte[] cDeviceID, String strDeviceName, String strProcessName)
    {
        this.cDeviceID = cDeviceID;
        this.strDeviceName = strDeviceName;
        this.strProcessName = strProcessName;
    }
    
    @Override
    public void Tick(int lMS)
    {
        switch(state)
        {
            case OFFLINE:
            {
                //Do nothing.
            }
            break;
            
            case CONNECT:
            {
                new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        //Establish a new connection to the server.
                        try
                        {
                            socket = new Socket(InetAddress.getByName(strURL), lPort);
                        }
                        catch (Exception ex)
                        {
                            //Didn't work. Reinitialise.
                            Reinitialise();
                        }
                    }
                }).start();
                
                LaunchLog.Log(COMMS, LOG_NAME, "Socket created. Going to dwell.");
                //Half a second for things to get their shit in a sock before proceeding further.
                dlyReinit.Set(CONNECT_DWELL_TIME);
                state = State.CONNECTING;
            }
            break;
            
            case CONNECTING:
            {
                dlyReinit.Tick(lMS);
                
                if(dlyReinit.Expired())
                {
                    boolean bOkay = false;
                    
                    if(socket != null)
                    {
                        boolean bMobile = gameInterface.GetConnectionMobile();
                        session = new LaunchClientSession(socket, gameInterface, cDeviceID, strDeviceName, strProcessName, bMobile);
                        LaunchLog.Log(COMMS, LOG_NAME, "Connection established. Going to processing.");
                        state = State.PROCESS;
                        bOkay = true;
                    }
                    
                    if(!bOkay)
                    {
                        LaunchLog.Log(COMMS, LOG_NAME, "Connection didn't establish properly. Reinitialising.");
                        Reinitialise();
                    }
                }
            }
            break;
            
            case PROCESS:
            {
                if(session.IsAlive())
                {
                    session.Tick(lMS);
                    
                    //Present the session a task when it's ready and we have one.
                    if(session.CanAcceptTask() && currentTask != null)
                    {
                        //Don't restart finished tasks.
                        if(!currentTask.Complete())
                        {
                            LaunchLog.Log(COMMS, LOG_NAME, "We have a task, and the session can accept it. Starting.");
                            session.StartTask(currentTask);
                        }
                    }
                    
                    //Prune the current task once it's complete.
                    if(currentTask != null)
                    {
                        if(currentTask.Complete())
                        {
                            LaunchLog.Log(COMMS, LOG_NAME, "Pruning the current task as it has finished.");
                            currentTask = null;
                        }
                    }
                    
                    //Download avatars and custom asset images whenever we can.
                    if(session.CanDownloadAnImage())
                    {
                        if(AvatarDownloadQueue.size() > 0)
                        {
                            session.DownloadAvatar(AvatarDownloadQueue.poll());
                        }
                        else if(ImageDownloadQueue.size() > 0)
                        {
                            session.DownloadImage(ImageDownloadQueue.poll());
                        }
                    }
                }
                else
                {
                    //Session died. Go to reinit.
                    LaunchLog.Log(COMMS, LOG_NAME, "Socket timed out. Going to reinitialise.");
                    Reinitialise();
                }
            }
            break;
            
            case REINIT:
            {
                //Dwell until ready to try a new session.
                dlyReinit.Tick(lMS);

                if(dlyReinit.Expired())
                {
                    LaunchLog.Log(COMMS, LOG_NAME, "Reinit delay expired. Reconnecting.");
                    state = State.CONNECT;
                }
            }
            break;
        }
    }

    @Override
    public void InterruptAll()
    {
        session.Close();
    }
    
    private void Reinitialise()
    {
        socket = null;
        dlyReinit.Set(RETRY_DWELL_TIME);
        state = State.REINIT;
        gameInterface.SetLatency(Defs.LATENCY_DISCONNECTED);
    }
    
    public void Suspend()
    {
        LaunchLog.Log(COMMS, LOG_NAME, "Suspended.");
        state = State.OFFLINE;
        
        if(session != null)
        {
            session.Close();
        }
        
        //Kill the current task as a catch all incase the game was stuck and the player is trying to "fix" it.
        currentTask = null;
        gameInterface.SetLatency(Defs.LATENCY_DISCONNECTED);
    }
    
    public void Resume()
    {
        LaunchLog.Log(COMMS, LOG_NAME, "Resumed.");
        
        if(state == State.OFFLINE)
        {
            state = State.CONNECT;
        }
    }
    
    public int GetReinitRemaining()
    {
        if(state == State.PROCESS)
            return session.GetTimeoutRemaining();
        
        return dlyReinit.GetRemaining();
    }
    
    public boolean GetDoingAnything()
    {
        if(state == State.PROCESS)
            return !session.GetTimingOut();
        
        return false;
    }
    
    public int GetDownloadRate()
    {
        if(state == State.PROCESS)
            return session.GetDownloadRate();
        
        return 0;
    }
    
    public void Register(String strUsername, int lAvatarID)
    {
        LaunchLog.Log(COMMS, LOG_NAME, "Register called.");
        currentTask = new RegisterTask(gameInterface, cDeviceID, strUsername, lAvatarID);
    }
    
    public void UploadAvatar(byte[] cData)
    {
        LaunchLog.Log(COMMS, LOG_NAME, "Upload avatar called.");
        currentTask = new UploadAvatarTask(gameInterface, cData);
    }
    
    public void DownloadAvatar(int lAvatarID)
    {
        if(!AvatarDownloadQueue.contains(lAvatarID))
        {
            LaunchLog.Log(COMMS, LOG_NAME, String.format("Queued avatar %d for download.", lAvatarID));
            AvatarDownloadQueue.add(lAvatarID);
        }
        else
        {
            LaunchLog.Log(COMMS, LOG_NAME, String.format("Avatar %d already queued.", lAvatarID));
        }
    }
    
    public void DownloadImage(int lAssetID)
    {
        if(!ImageDownloadQueue.contains(lAssetID))
        {
            LaunchLog.Log(COMMS, LOG_NAME, String.format("Queued image %d for download.", lAssetID));
            ImageDownloadQueue.add(lAssetID);
        }
        else
        {
            LaunchLog.Log(COMMS, LOG_NAME, String.format("Image %d already queued.", lAssetID));
        }
    }
    
    public void Respawn()
    {
        LaunchLog.Log(COMMS, LOG_NAME, "Respawn called.");
        currentTask = new RespawnTask(gameInterface);
    }
    
    public void LaunchMissilePlayer(byte cSlotNo, boolean bTracking, GeoCoord geoTarget, int lTrackingID)
    {
        currentTask = new LaunchSomethingTask(gameInterface, cSlotNo, bTracking, geoTarget, lTrackingID);
    }
    
    public void LaunchMissile(int lSiteID, byte cSlotNo, boolean bTracking, GeoCoord geoTarget, int lTrackingID)
    {
        currentTask = new LaunchSomethingTask(gameInterface, lSiteID, cSlotNo, bTracking, geoTarget, lTrackingID);
    }
    
    public void LaunchInterceptorPlayer(byte cSlotNo, int lTargetID)
    {
        currentTask = new LaunchSomethingTask(gameInterface, cSlotNo, lTargetID);
    }
    
    public void LaunchInterceptor(int lSiteID, byte cSlotNo, int lTargetID)
    {
        currentTask = new LaunchSomethingTask(gameInterface, lSiteID, cSlotNo, lTargetID);
    }
    
    public void PurchaseMissilesPlayer(byte cSlotNumber, byte[] cTypes)
    {
        currentTask = new PurchaseLaunchablesTask(gameInterface, true, Defs.PLAYER_CARRIED, cSlotNumber, cTypes);
    }
    
    public void PurchaseMissiles(int lSiteID, byte cSlotNumber, byte[] cTypes)
    {
        currentTask = new PurchaseLaunchablesTask(gameInterface, true, lSiteID, cSlotNumber, cTypes);
    }
    
    public void PurchaseInterceptorsPlayer(byte cSlotNumber, byte[] cTypes)
    {
        currentTask = new PurchaseLaunchablesTask(gameInterface, false, Defs.PLAYER_CARRIED, cSlotNumber, cTypes);
    }
    
    public void PurchaseInterceptors(int lSiteID, byte cSlotNumber, byte[] cTypes)
    {
        currentTask = new PurchaseLaunchablesTask(gameInterface, false, lSiteID, cSlotNumber, cTypes);
    }
    
    public void SellMissilePlayer(byte cSlotNumber)
    {
        currentTask = new SellLaunchableTask(gameInterface, true, Defs.PLAYER_CARRIED, cSlotNumber);
    }
    
    public void SellMissile(int lSiteID, byte cSlotNumber)
    {
        currentTask = new SellLaunchableTask(gameInterface, true, lSiteID, cSlotNumber);
    }
    
    public void SellInterceptorPlayer(byte cSlotNumber)
    {
        currentTask = new SellLaunchableTask(gameInterface, false, Defs.PLAYER_CARRIED, cSlotNumber);
    }
    
    public void SellInterceptor(int lSiteID, byte cSlotNumber)
    {
        currentTask = new SellLaunchableTask(gameInterface, false, lSiteID, cSlotNumber);
    }
    
    public void PurchaseMissileSystem()
    {
        currentTask = new PurchaseMissileSystemTask(gameInterface);
    }

    public void PurchaseMissileSlotUpgradePlayer()
    {
        currentTask = new PurchaseSlotUpgradeTask(gameInterface, true);
    }
            
    public void PurchaseMissileSlotUpgrade(int lSiteID)
    {
        currentTask = new PurchaseSlotUpgradeTask(gameInterface, true, lSiteID);
    }

    public void PurchaseMissileReloadUpgradePlayer()
    {
        currentTask = new PurchaseReloadUpgradeTask(gameInterface, true);
    }
            
    public void PurchaseMissileReloadUpgrade(int lSiteID)
    {
        currentTask = new PurchaseReloadUpgradeTask(gameInterface, true, lSiteID);
    }
    
    public void PurchaseSAMSystem()
    {
        currentTask = new PurchaseSAMSystemTask(gameInterface);
    }

    public void PurchaseSAMSlotUpgradePlayer()
    {
        currentTask = new PurchaseSlotUpgradeTask(gameInterface, false);
    }
            
    public void PurchaseSAMSlotUpgrade(int lSiteID)
    {
        currentTask = new PurchaseSlotUpgradeTask(gameInterface, false, lSiteID);
    }

    public void PurchaseSAMReloadUpgradePlayer()
    {
        currentTask = new PurchaseReloadUpgradeTask(gameInterface, false);
    }
            
    public void PurchaseSAMReloadUpgrade(int lSiteID)
    {
        currentTask = new PurchaseReloadUpgradeTask(gameInterface, false, lSiteID);
    }
    
    public void ConstructMissileSite(boolean bNuclear)
    {
        currentTask = new BuildStructureTask(gameInterface, bNuclear? Task.StructureType.NUCLEAR_MISSILE_SITE : Task.StructureType.MISSILE_SITE);
    }
    
    public void ConstructSAMSite()
    {
        currentTask = new BuildStructureTask(gameInterface, Task.StructureType.SAM_SITE);
    }
    
    public void ConstructSentryGun()
    {
        currentTask = new BuildStructureTask(gameInterface, Task.StructureType.SENTRY_GUN);
    }
    
    public void ConstructOreMine()
    {
        currentTask = new BuildStructureTask(gameInterface, Task.StructureType.ORE_MINE);
    }
    
    public void SellMissileSite(int lSiteID)
    {
        currentTask = new SellStructureTask(gameInterface, Task.StructureType.MISSILE_SITE, lSiteID);
    }
    
    public void SellSAMSite(int lSiteID)
    {
        currentTask = new SellStructureTask(gameInterface, Task.StructureType.SAM_SITE, lSiteID);
    }
    
    public void SellSentryGun(int lSiteID)
    {
        currentTask = new SellStructureTask(gameInterface, Task.StructureType.SENTRY_GUN, lSiteID);
    }
    
    public void SellOreMine(int lSiteID)
    {
        currentTask = new SellStructureTask(gameInterface, Task.StructureType.ORE_MINE, lSiteID);
    }
    
    public void SellMissileSystem()
    {
        currentTask = new SellSystemTask(gameInterface, true);
    }
    
    public void SellSAMSystem()
    {
        currentTask = new SellSystemTask(gameInterface, false);
    }
    
    public void SetOnlineOffline(int lSiteID, StructureType structureType, boolean bOnline)
    {
        currentTask = new SiteOnlineOfflineTask(gameInterface, lSiteID, structureType, bOnline);
    }
    
    public void SetMultipleOnlineOffline(List<Integer> SiteIDs, StructureType structureType, boolean bOnline)
    {
        currentTask = new SiteOnlineOfflineTask(gameInterface, SiteIDs, structureType, bOnline);
    }
    
    public void RepairStructure(int lSiteID, StructureType structureType)
    {
        currentTask = new RepairTask(gameInterface, lSiteID, structureType);
    }
    
    public void HealPlayer()
    {
        currentTask = new HealTask(gameInterface);
    }
    
    public void SetSAMSiteMode(int lSiteID, byte cMode)
    {
        currentTask = new SAMSiteModeTask(gameInterface, lSiteID, cMode);
    }
    
    public void SetMultipleSAMSiteModes(List<Integer> SiteIDs, byte cMode)
    {
        currentTask = new SAMSiteModeTask(gameInterface, SiteIDs, cMode);
    }
    
    public void SetStructureName(int lSiteID, String strName, StructureType type)
    {
        currentTask = new StructureRenameTask(gameInterface, lSiteID, strName, type);
    }
    
    public void SetPlayerName(String strName)
    {
        currentTask = new RenameTask(gameInterface, strName, RenameTask.Context.Player);
    }
    
    public void SetAllianceName(String strName)
    {
        currentTask = new RenameTask(gameInterface, strName, RenameTask.Context.AllianceName);
    }
    
    public void SetAllianceDescription(String strName)
    {
        currentTask = new RenameTask(gameInterface, strName, RenameTask.Context.AllianceDescription);
    }
    
    public void CloseAccount()
    {
        currentTask = new CloseAccountTask(gameInterface);
    }
    
    public void SetAvatar(int lAvatarID, boolean bIsAlliance)
    {
        currentTask = new SetAvatarTask(gameInterface, lAvatarID, bIsAlliance);
    }
    
    public void UpgradeToNuclear(int lMissileSiteID)
    {
        currentTask = new UpgradeToNuclearTask(gameInterface, lMissileSiteID);
    }
    
    public void CreateAlliance(String strName, String strDescription, int lAvatarID)
    {
        currentTask = new CreateAllianceTask(gameInterface, strName, strDescription, lAvatarID);
    }
    
    public void JoinAlliance(int lAllianceID)
    {
        currentTask = new JoinAllianceTask(gameInterface, lAllianceID);
    }
    
    public void LeaveAlliance()
    {
        currentTask = new LeaveAllianceTask(gameInterface);
    }
    
    public void DeclareWar(int lAllianceID)
    {
        currentTask = new TreatyTask(gameInterface, lAllianceID, Treaty.Type.WAR);
    }
    
    public void OfferAffiliation(int lAllianceID)
    {
        currentTask = new TreatyTask(gameInterface, lAllianceID, Treaty.Type.AFFILIATION_REQUEST);
    }
    
    public void AcceptAffiliation(int lAllianceID)
    {
        currentTask = new TreatyTask(gameInterface, lAllianceID, Treaty.Type.AFFILIATION);
    }
    
    public void RejectAffiliation(int lAllianceID)
    {
        currentTask = new TreatyTask(gameInterface, lAllianceID, Treaty.Type.AFFILIATION_REJECT);
    }
    
    public void Promote(int lPromotee)
    {
        currentTask = new PromoteTask(gameInterface, lPromotee);
    }

    public void AcceptJoin(int lPlayer)
    {
        currentTask = new AllianceJoinTask(gameInterface, lPlayer, false);
    }

    public void RejectJoin(int lPlayer)
    {
        currentTask = new AllianceJoinTask(gameInterface, lPlayer, true);
    }

    public void Kick(int lPlayer)
    {
        currentTask = new KickTask(gameInterface, lPlayer);
    }

    public void Ban(String strReason, int lPlayer, boolean bPermanent)
    {
        currentTask = new BanTask(gameInterface, strReason, lPlayer, bPermanent);
    }

    public void ResetAvatar(int lPlayer)
    {
        currentTask = new AdminPlayerCommandTask(gameInterface, lPlayer, LaunchSession.ResetAvatar);
    }

    public void ResetName(int lPlayer)
    {
        currentTask = new AdminPlayerCommandTask(gameInterface, lPlayer, LaunchSession.ResetName);
    }
    
    public void DeviceCheck(boolean bCompleteFailure, boolean bAPIFailure, int lFailureCode, boolean bProfileMatch, boolean bBasicIntegrity)
    {
        currentTask = new DeviceCheckTask(gameInterface, bCompleteFailure, bAPIFailure, lFailureCode, bProfileMatch, bBasicIntegrity);
    }
    
    public boolean GetWarStats(int lWarID)
    {
        if(DirectCommsPossible())
        {
            TobComm tobComm = session.GetTobComm();
            tobComm.RequestObject(LaunchSession.Treaty, lWarID);
            return true;
        }
        
        return false;
    }
    
    public boolean GetPlayerStats(int lPlayerID)
    {
        if(DirectCommsPossible())
        {
            TobComm tobComm = session.GetTobComm();
            tobComm.RequestObject(LaunchSession.FullPlayerStats, lPlayerID);
            return true;
        }
        
        return false;
    }

    public boolean GetUserData(int lPlayerID)
    {
        if(DirectCommsPossible())
        {
            TobComm tobComm = session.GetTobComm();
            tobComm.RequestObject(LaunchSession.UserData, lPlayerID);
            return true;
        }

        return false;
    }
    
    /**
     * Report if experimental direct-comms (blast messages at the server without a task) are possible.
     * @return True if the connection seems established and stable. False otherwise.
     */
    public boolean DirectCommsPossible()
    {
        if(state == State.PROCESS)
        {
            if(session != null)
            {
                return session.IsAlive();
            }
        }
        
        return false;
    }
    
    public String GetState()
    {
        return state.name();
    }
    
    public String GetSessionState()
    {
        if(session == null)
            return "NULL";
        
        if(session.IsAlive())
            return session.GetState();
        
        return "DEAD";
    }
}
