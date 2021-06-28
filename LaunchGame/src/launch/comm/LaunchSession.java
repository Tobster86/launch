/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.comm;

import java.net.Socket;
import tobcomm.TobComm;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import tobcomm.protocol.ConnectionProvider;
import launch.utilities.LaunchLog;
import static launch.utilities.LaunchLog.LogType.SESSION;
import launch.utilities.ShortDelay;
import tobcomm.TobCommInterface;
import tobcomm.protocol.ConnectionProvider.ConnectionLogger;
import tobcomm.protocol.TCPProvider;

/**
 *
 * @author tobster
 */
public abstract class LaunchSession implements TobCommInterface, ConnectionLogger
{
    //Objects.
    public static final int Authorise = 0;                   //Request to authorise, using encrypted device ID.
    public static final int UserData = 1;                    //User data. Admin's eyes only.
    public static final int PermBanData = 2;                 //Player is permanently banned, with reason.
    public static final int BanData = 3;                     //Player is banned, with duration and reason.
    public static final int Registration = 4;                //Account registration request details.
    public static final int GameSnapshot = 5;                //A new snapshot of the entire game, for every new comms session.
    public static final int LocationUpdate = 6;              //Regular location data from players.
    public static final int Player = 7;                      //A player.
    public static final int Missile = 8;                     //A missile.
    public static final int Interceptor = 9;                 //An interceptor missile.
    public static final int MissileSite = 10;                //A missile launch site.
    public static final int SamSite = 11;                    //A SAM site.
    public static final int OreMine = 12;                    //An ore mine.
    public static final int SentryGun = 13;                  //A sentry gun.
    public static final int Loot = 14;                       //A loot cache.
    public static final int Radiation = 15;                  //A radioactive area.
    public static final int AllianceMinor = 16;              //An alliance minor change (i.e. points change).
    public static final int AllianceMajor = 17;              //An alliance major change that should trigger a UI refresh (i.e. players joining/leaving, etc).
    public static final int Treaty = 18;                     //A treaty.
    public static final int Avatar = 19;                     //An avatar.
    //20 is reserved.
    public static final int Config = 21;                     //The game configuration.
    //22 is reserved.    
    public static final int Event = 23;                      //An event message.
    public static final int Report = 24;                     //A report message.
    public static final int BuildMissileSite = 25;           //A request to build a missile site.
    public static final int SellMissile = 26;                //A request to sell a missile.
    public static final int SellInterceptor = 27;            //A request to sell an interceptor.
    public static final int Ban = 28;                        //Ban a player (as an administrator).
    public static final int FullPlayerStats = 29;            //A player's stats.
    public static final int LaunchMissile = 30;              //A request to launch a missile.
    public static final int LaunchPlayerMissile = 31;        //A request to launch a missile.
    public static final int LaunchInterceptor = 32;          //A request to launch an interceptor.
    public static final int LaunchPlayerInterceptor = 33;    //A request to launch an interceptor.
    public static final int AlertStatus = 34;                //A request for alert status.
    public static final int SAMSiteModeChange = 35;          //An instruction to change a SAM site mode.
    public static final int SAMSiteNameChange = 36;          //An instruction to change a SAM site name.
    public static final int MissileSiteNameChange = 37;      //An instruction to change a missile site name.
    public static final int SentryGunNameChange = 38;        //An instruction to change a sentry gun name.
    public static final int OreMineNameChange = 39;          //An instruction to change an ore mine name.
    public static final int CreateAlliance = 40;             //Alliance creation details.
    public static final int PurchaseMissiles = 41;           //A request to purchase missiles.
    public static final int PurchaseInterceptors = 42;       //A request to purchase interceptors.
    public static final int ProcessNames = 43;               //A list of process names, when location spoofing has been suspected.
    public static final int DeviceCheck = 44;                //Device check information.
    public static final int RenamePlayer = 45;               //A request for a player to change their name.
    public static final int RenameAlliance = 46;             //A request to change the name of an alliance.
    public static final int RedescribeAlliance = 47;         //A request to change an alliance description.
    public static final int MissileSitesOnOff = 48;          //An instruction to bring multiple missile sites online or take them offline.
    public static final int SAMSitesOnOff = 49;              //An instruction to bring multiple SAM sites online or take them offline.
    public static final int SentryGunsOnOff = 50;            //An instruction to bring multiple sentry guns online or take them offline.
    public static final int OreMinesOnOff = 51;              //An instruction to bring multiple ore mines online or take them offline.
    public static final int ImgAsset = 52;                   //An image.
      
    //Commands.
    public static final int AccountUnregistered = 0;             //The account must be registered (present user with form).
    public static final int MajorVersionInvalid = 1;             //Notify the client that a major update is available.
    public static final int NameTaken = 2;                       //The player or alliance name already exists.
    public static final int AccountCreateSuccess = 3;            //The account was created successfully.
    //4 & 5 are reserved.
    public static final int SnapshotBegin = 6;                   //Indicates the start of a requested game snapshot.
    public static final int SnapshotComplete = 7;                //Indicates the end of a requested game snapshot.
    public static final int SnapshotAck = 8;                     //Acknowledges receipt of the end of the snapshot.
    public static final int ImageError = 9;                      //Error reading image data.
    public static final int ActionSuccess = 10;                  //The last action was completed.
    public static final int ActionFailed = 11;                   //The last action failed for an unspecified reason.
    public static final int PurchaseMissileSystem = 12;          //A request to purchase a missile system for a player.
    public static final int PurchaseSAMSystem = 13;              //A request to purchase an air defence system for a player.
    public static final int BuildSamSite = 14;                   //A request to build a SAM site.
    public static final int BuildSentryGun = 15;                 //A request to build a sentry gun.
    public static final int BuildOreMine = 16;                   //A request to build an ore mine.
    public static final int ReportAck = 17;                      //A client acking a report so it may be deleted.
    public static final int KeepAlive = 18;                      //A keepalive for when location information isn't available.
    public static final int RemovePlayer = 19;                   //A player has left the game and must be removed.
    public static final int RemoveMissile = 20;                  //A missile has been removed from the game.
    public static final int RemoveInterceptor = 21;              //An interceptor has been removed from the game.
    public static final int RemoveMissileSite = 22;              //A missile site has been removed from the game.
    public static final int RemoveSAMSite = 23;                  //A SAM site has been removed from the game.
    public static final int RemoveOreMine = 24;                  //An ore mine has been removed from the game.
    public static final int RemoveSentryGun = 25;                //A sentry gun has been removed from the game.
    public static final int RemoveLoot = 26;                     //A loot has been removed from the game.
    public static final int RemoveRadiation = 27;                //A radioactive area has been removed from the game.
    public static final int RemoveAlliance = 28;                 //An alliance has been removed from the game.
    public static final int RemoveTreaty = 29;                   //A treaty has been removed from the game.
    public static final int Respawn = 30;                        //A request to respawn.
    public static final int PlayerMissileSlotUpgrade = 31;       //A request to upgrade missile slots on player's CMS system.
    public static final int PlayerInterceptorSlotUpgrade = 32;   //A request to upgrade interceptor slots on player's SAM system.
    public static final int MissileSlotUpgrade = 33;             //A request to upgrade missile slots on a missile site (instance no).
    public static final int InterceptorSlotUpgrade = 34;         //A request to upgrade interceptor slots on a SAM site (instance no).
    public static final int PlayerMissileReloadUpgrade = 35;     //A request to upgrade reloading on player's CMS system.
    public static final int PlayerInterceptorReloadUpgrade = 36; //A request to upgrade reloading on player's SAM system.
    public static final int MissileReloadUpgrade = 37;           //A request to upgrade reloading on a missile site (instance no).
    public static final int InterceptorReloadUpgrade = 38;       //A request to upgrade reloading on a SAM site (instance no).
    public static final int SellMissileSite = 39;                //A request to sell a missile site.
    public static final int SellSAMSite = 40;                    //A request to sell a SAM site.
    public static final int SellSentryGun = 41;                  //A request to sell a sentry gun.
    public static final int SellOreMine = 42;                    //A request to sell an ore mine.
    public static final int SellMissileSystem = 43;              //A request to sell a missile system.
    public static final int SellSAMSystem = 44;                  //A request to sell a SAM system.
    public static final int RepairMissileSite = 45;              //A request to remotely repair the missile site with instance number.
    public static final int RepairSAMSite = 46;                  //A request to remotely repair the SAM site with instance number.
    public static final int RepairSentryGun = 47;                //A request to remotely repair the sentry gun with instance number.
    public static final int RepairOreMine = 48;                  //A request to remotely repair the ore mine with instance number.
    public static final int Heal = 49;                           //A request to fully heal the player.
    public static final int SetAvatar = 50;                      //A request to set an avatar ID.
    public static final int CloseAccount = 51;                   //A request to close the player's account.
    public static final int AlertAllClear = 52;                  //Alert indication that a player is not under attack.
    public static final int AlertUnderAttack = 53;               //Alert indication that a player is under attack.
    public static final int AlertNukeEscalation = 54;            //Alert indication that a player's ally is under attack.
    public static final int AlertAllyUnderAttack = 55;           //Alert indication that a player's ally is under attack.
    //56 & 57 are reserved.
    public static final int UpgradeMissileSiteNuclear = 58;      //A request to upgrade a missile site to nuclear capabilities.
    public static final int JoinAlliance = 59;                   //A request to join the specified alliance.
    public static final int LeaveAlliance = 60;                  //A request to leave any alliance the player is a member of.
    public static final int DeclareWar = 61;                     //A request for the player's alliance to declare war on the specified alliance.
    public static final int SetAllianceAvatar = 62;              //A request to set an alliance avatar ID.
    public static final int Promote = 63;                        //A request to promote an alliance member to a leader.
    public static final int AcceptJoin = 64;                     //Accept a player into the alliance you lead.
    public static final int RejectJoin = 65;                     //Reject a player's request to join the alliance you lead.
    public static final int Kick = 66;                           //Kick a player from the alliance you lead.
    public static final int ResetName = 67;                      //Reset a player's name (as an administrator).
    public static final int ResetAvatar = 68;                    //Reset a player's avatar (as an administrator).
    public static final int ProposeAffiliation = 69;             //An offer of a peace treaty to another alliance.
    public static final int AcceptAffiliation = 70;              //An acceptance of a peace treaty from another alliance.
    public static final int RejectAffiliation = 71;              //An acceptance of a peace treaty from another alliance.
    public static final int DisplayGeneralError = 72;            //A command to display a generic error on the client, for limited accounts suspected of cheating.
    
    private static final int MESSAGE_BUFFER_SIZE = 10240;
    private static final int COMMS_THREAD_SLEEP = 20;
    private static final int ONE_SECOND = 1000;
    
    protected ConnectionProvider connection;
    
    protected TobComm tobComm;
    
    private int lIdleTime = 0;
    private boolean bDead = false;
    
    protected String strLogName;
    
    private Thread processThread;
    
    //Bursting reduces number of TCP packets when sending lots of objects (i.e. during game snapshot).
    private boolean bBursting = false;
    private List<byte[]> BurstList;
    
    //Synchronised buffering of send packets removes concurrency issues with TCP socket writes.
    private Queue<byte[]> MessageSendList = new ConcurrentLinkedQueue();
    
    //Connection monitoring.
    private int lTotalDownloaded = 0;
    private int lTotalUploaded = 0;
    private int lDownloadRateCounter = 0;
    private int lDownloadRate = 0;
    private int lUploadRateCounter = 0;
    private int lUploadRate = 0;
    private ShortDelay dlyConnectionRates = new ShortDelay();
    
    public LaunchSession(Socket socket)
    {
        this.connection = new TCPProvider(socket, this);
        strLogName = LaunchLog.GetTimeFormattedLogName(connection.GetAddress());
        tobComm = new TobComm(this);
        LaunchLog.Log(SESSION, strLogName, "Session created.");
        
        processThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                LaunchLog.Log(SESSION, strLogName, "Session thread created.");
                
                if(!connection.Initialise())
                {
                    //Errored.
                    LaunchLog.Log(SESSION, strLogName, "Error initialising session.");
                    Close();
                }
                
                while(!bDead)
                {
                    if(connection.DataAvailable())
                    {
                        lIdleTime = 0;
                        byte[] cMessage = new byte[MESSAGE_BUFFER_SIZE];
                        int lRead = connection.Read(cMessage);

                        if(lRead > 0)
                        {
                            lTotalDownloaded += lRead;
                            lDownloadRateCounter += lRead;
                            tobComm.ProcessBytes(Arrays.copyOfRange(cMessage, 0, lRead));
                        }
                    }
                    
                    if(connection.Died())
                    {
                        //Session errored.
                        LaunchLog.Log(SESSION, strLogName, "Session errored while getting data.");
                        Close();
                    }
                    
                    Process();
                    
                    while(!MessageSendList.isEmpty())
                    {
                        byte[] cData = MessageSendList.poll();

                        connection.Write(cData);

                        if(connection.Died())
                        {
                            //Output stream errored.
                            LaunchLog.Log(SESSION, strLogName, String.format("Session error writing %d bytes.", cData.length));
                            Close();
                        }
                        else
                        {
                            lTotalUploaded += cData.length;
                            lUploadRateCounter += cData.length;
                        }
                    }
                    
                    try
                    {
                        Thread.sleep(COMMS_THREAD_SLEEP);
                    }
                    catch(InterruptedException ex) { /* Don't care */ }
                }
                
                LaunchLog.Log(SESSION, strLogName, "Session thread finished normally.");
            }
        });
    }
    
    protected final void Start()
    {
        processThread.start();
    }
    
    protected abstract void Process();
    
    public void Tick(int lMS)
    {
        lIdleTime += lMS;

        if(lIdleTime > GetTimeout())
        {
            //Session timed out.
            LaunchLog.Log(SESSION, strLogName, "Session timed out. Closing.");
            Close();
        }
        
        dlyConnectionRates.Tick(lMS);
        
        if(dlyConnectionRates.Expired())
        {
            lDownloadRate = lDownloadRateCounter;
            lDownloadRateCounter = 0;
            lUploadRate = lUploadRateCounter;
            lUploadRateCounter = 0;
            dlyConnectionRates.Set(ONE_SECOND);
        }
    }
    
    public int GetTimeoutRemaining()
    {
        return GetTimeout() - lIdleTime;
    }
    
    public boolean GetTimingOut()
    {
        return lIdleTime > ONE_SECOND;
    }
    
    public void Close()
    {
        if(!bDead)
        {
            bDead = true;
            
            connection.Close();
            
            try
            {
                processThread.interrupt();
            }
            catch(Exception ex) { /* Don't care */ }

            LaunchLog.Log(SESSION, strLogName, String.format("Session closed after downloading %dB & uploading %dB.", lTotalDownloaded, lTotalUploaded));
        }
        else
        {
            LaunchLog.Log(SESSION, strLogName, "The session is already closed.");
        }
    }
    
    public final boolean IsAlive() { return !bDead; }
    
    public int GetDownloadRate() { return lDownloadRate; }
    
    public int GetUploadRate() { return lUploadRate; }
    
    protected void StartBurst()
    {
        BurstList = new ArrayList();
        bBursting = true;
    }
    
    protected void EndBurst()
    {
        bBursting = false;
        
        int lSize = 0;
        
        for(byte[] array : BurstList)
        {
            lSize += array.length;
        }
        
        byte[] cBurstData = new byte[lSize];
        int lOffset = 0;
        
        for(byte[] array : BurstList)
        {
            System.arraycopy(array, 0, cBurstData, lOffset, array.length);
            lOffset += array.length;
        }
        
        MessageSendList.add(cBurstData);
        
        //Explicitly Free the memory (otherwise lots of these could hang around).
        BurstList = null;
    }

    @Override
    public void BytesToSend(byte[] cData)
    {
        if(!bDead)
        {
            if(bBursting)
            {
                BurstList.add(cData);
            }
            else
            {
                MessageSendList.add(cData);
            }
        }
        else
        {
            LaunchLog.Log(SESSION, strLogName, String.format("Not sending %d bytes. The session is dead.", cData.length));
        }
    }

    @Override
    public void Error(String strErrorText)
    {
        //LaunchComm errored. Bin the connection.
        LaunchLog.Log(SESSION, strLogName, "Connection Error: " + strErrorText);
        Close();
    }

    @Override
    public void ConnectionLog(String strLog)
    {
        LaunchLog.Log(SESSION, strLogName, "Connection Report: " + strLog);
    }
    
    protected abstract int GetTimeout();
}
