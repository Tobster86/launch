/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launchai;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import launch.comm.clienttasks.Task;
import launch.game.Config;
import launch.game.GeoCoord;
import launch.game.LaunchClientAppInterface;
import launch.game.LaunchClientGame;
import launch.game.User;
import launch.game.entities.LaunchEntity;
import launch.game.entities.Player;
import launch.game.entities.Structure;
import launch.game.systems.MissileSystem;
import launch.game.treaties.Treaty;
import launch.game.types.MissileType;
import launch.utilities.LaunchClientLocation;
import launch.utilities.LaunchEvent;
import launch.utilities.LaunchLog;
import static launch.utilities.LaunchLog.LogType.*;
import launch.utilities.LaunchReport;
import launch.utilities.PrivacyZone;
import launch.utilities.Security;

/**
 *
 * @author tobster
 */
public class LaunchAI implements LaunchClientAppInterface
{
    private static final String SERVER_URL = "77.68.17.204";
    private static final int SERVER_PORT = 30069;
    
    private static final String DESIRED_PLAYER_NAME = "George Boole [AI]";
    private static final String LOG_NAME = "Bot";
    private static final String CONFIG_FILENAME = "config.xml";
    
    private LaunchClientGame game;
    
    private GeoCoord geoLocation = new GeoCoord(27, -45, true);
    
    private boolean bLoggedIn = false;
    
    private enum BotState
    {
        IDLE,
        MOVING,
        SHOOTING
    }
    
    private BotState state;
    
    
    public LaunchAI()
    {
        //Initialise.
        LaunchLog.SetLoggingEnabled(APPLICATION, true, true); //Keep an application log file, but everything else can just go to the console.
        LaunchLog.SetConsoleLoggingEnabled(SESSION, true);
        LaunchLog.SetConsoleLoggingEnabled(COMMS, true);
        LaunchLog.SetConsoleLoggingEnabled(GAME, true);
        LaunchLog.SetConsoleLoggingEnabled(TASKS, true);
        LaunchLog.SetConsoleLoggingEnabled(SERVICES, true);
        LaunchLog.SetConsoleLoggingEnabled(LOCATIONS, true);

        //Load game config file if we have one.
        File file = new File(CONFIG_FILENAME);

        Config config = null;

        if (file.exists())
        {
            RandomAccessFile rafConfig = null;

            try
            {
                rafConfig = new RandomAccessFile(file, "r");
                byte[] cConfig = new byte[(int) rafConfig.length()];
                rafConfig.read(cConfig);

                config = new Config(cConfig);
                
                LaunchLog.Log(APPLICATION, LOG_NAME, "Loaded local game config file successfully.");
            }
            catch (Exception ex)
            { /* Don't care; if the stored one is broken or outdated we'll simply download another automatically. */ }
            finally
            {
                if (rafConfig != null)
                {
                    try
                    {
                        rafConfig.close();
                    }
                    catch (Exception ex)
                    { /* Don't care. */ }
                }
                else
                {
                    LaunchLog.Log(APPLICATION, LOG_NAME, "I don't have a local game config file, but I'll download one...");
                }
            }
        }
        
        //Create an empty list of privacy zones. We need one to fire up the game.
        List<PrivacyZone> PrivacyZones = new ArrayList<>();
        
        //Fire up the game.
        game = new LaunchClientGame(config, this, PrivacyZones, SERVER_URL, SERVER_PORT);
        
        //Set our identity.
        String strPlainTextDeviceID = "I am a robot"; //This will identify the AI's Launch account. TO DO: Get from a configuration file?
        byte[] cDeviceID = null;
        
        try
        {
            cDeviceID = Security.GetSHA256(strPlainTextDeviceID.getBytes());
        }
        catch(Exception ex)
        {
            //Couldn't encrypt the credentials. Just quit for now.
            LaunchLog.Log(APPLICATION, LOG_NAME, "Couldn't encrypt my credentials! Fuck this shit...");
            System.exit(0);
        }
        
        game.SetDeviceID(cDeviceID, strPlainTextDeviceID, "I don't have processes");
        
        //Start the comms.
        game.Resume();
        
        LaunchLog.Log(APPLICATION, LOG_NAME, "I'm fired up, just waiting on the server now.");
    }

    @Override
    public void GameTicked(int lMS)
    {
        //The game has ticked. This is a good place for our bot to do its processing.
        if(bLoggedIn && game.GetInteractionReady())
        {
            //We're logged into the game, and we're not already in the middle of another task.
            Player ourPlayer = game.GetOurPlayer();
            
            ourPlayer.SetPo
            
            if(ourPlayer.Functioning())
            {
                //Our player is alive, and can do stuff.
                
                
                
                //Do stuff here...
                
                switch(state)
                {
                    case IDLE:
                    {
                        if(true /* I.e. some kind of decision that we're now going to move */)
                        {
                            state = BotState.MOVING;
                        }
                    }
                    break;
                    
                    case MOVING:
                    {
                        //Move towards the nasty bastard that shot us.
                        ourPlayer.GetPosition().MoveToward(theBadGuy.GetPosition(), REALLY_FAST_KPH);
                        
                        if(true /* Condition that makes us decide to stop moving and start shooting. */)
                        {
                            state = BotState.SHOOTING;
                        }
                    }
                    break;
                   
                    case SHOOTING:
                    {
                        if(bShoot) /* Some other process will time the frequency that we shoot. */
                        {
                            game.LaunchMissile(SITE_ID, SLOT_NUMBER, true /* player tracking */, theBadGuy.GetPosition(), theBadGuy.GetID());
                            
                            bShoot = false;
                        }
                        
                        //Return to idle when the player is dead.
                        if(theBadGuy.Destroyed())
                        {
                            state = BotState.IDLE;
                        }
                        
                    }
                    break;
                }
                
                
                
                
                /*
                //Example: Move us 1m to the north (a slowish walking pace).
                geoLocation.MoveToward(new GeoCoord(0.0f, 0.0f), 1.0f);
                */
                
                
                
                
                //Example: Buy a missile system if we don't have one and can afford one.
                /*
                if(!ourPlayer.GetHasCruiseMissileSystem())
                {
                    //We don't yet have a cruise missile system.
                    if(ourPlayer.GetWealth() >= game.GetConfig().GetSAMSystemCost())
                    {
                        //We can afford one. Buy one.
                        game.PurchaseMissileSystem();
                    }
                }
                */
                
                
                
                
                
                
                
                /*
                //Example: Build a SAM site.
                //Get a list of structures within the structure separation distance of the player.
                List<Structure> NearbyStructures = game.GetNearbyStructures(ourPlayer.GetPosition(), game.GetConfig().GetStructureSeparation());
                
                if(NearbyStructures.isEmpty())
                {
                    //No nearby structures. Do we have the money?
                    if(ourPlayer.GetWealth() >= game.GetConfig().GetSAMStructureCost())
                    {
                        //We have the money. Build the SAM site.
                        game.ConstructSAMSite();
                    }
                }
                */
                
                
                
                /*
                //Example: Buy a missile for the first slot of our cruise missile system.
                if(ourPlayer.GetHasCruiseMissileSystem())
                {
                    MissileSystem system = ourPlayer.GetMissileSystem();
                    
                    byte cSlotNumber = 0;
                    
                    //Check if there's not already one in the first slot.
                    if(!system.GetSlotHasMissile(cSlotNumber))
                    {
                        //Pick the first one we can afford (NOTE: very primitive/dumb behaviour).
                        for(MissileType type : game.GetConfig().GetMissileTypes())
                        {
                            if(ourPlayer.GetWealth() >= game.GetConfig().GetMissileCost(type.GetID()))
                            {
                                //Found one. Buy it.
                                game.PurchaseMissilesPlayer(cSlotNumber, new byte[] { type.GetID() });

                                //Break out of the missile types loop to stop buying any more.
                                break;
                            }
                        }
                    }
                }
                */
                
                
                
            }
            else
            {
                //Our player is dead. Respawn it if we can...
                if(ourPlayer.GetCanRespawn())
                {
                    LaunchLog.Log(APPLICATION, LOG_NAME, "I seem to be dead. Respawning.");
                    game.Respawn();
                }    
            }
        }
    }

    @Override
    public void SaveConfig(Config config)
    {
        //We downloaded a game config. Save it to a file.
        LaunchLog.Log(APPLICATION, LOG_NAME, "Config downloaded. Saving it...");
        
        File file = new File(CONFIG_FILENAME);

        try
        {
            RandomAccessFile rafConfig = new RandomAccessFile(file, "rw");
            rafConfig.setLength(0); //Clear the file.
            rafConfig.write(config.GetData());
            rafConfig.close();
            LaunchLog.Log(APPLICATION, LOG_NAME, "...Done.");
        }
        catch (Exception ex)
        {
            LaunchLog.Log(APPLICATION, LOG_NAME, "...Could not save config: " + ex.getMessage());
        }
    }

    @Override
    public void SaveAvatar(int lAvatarID, byte[] cData)
    {
        //Do nothing. Bots don't save avatars.
    }

    @Override
    public void AvatarUploaded(int lAvatarID)
    {
        //Optional: respond to the fact that we uploaded an avatar.
    }

    @Override
    public boolean PlayerLocationAvailable()
    {
        //Client must respond if its GPS is working. For a bot, that's always yes.
        return true;
    }

    @Override
    public LaunchClientLocation GetPlayerLocation()
    {
        //The bot must return it's current location.
        return new LaunchClientLocation(geoLocation.GetLatitude(), geoLocation.GetLongitude(), 0.0f, "Bot");
    }

    @Override
    public void EntityUpdated(LaunchEntity entity)
    {
        //Optional: An entity updated in the game. The bot can respond to this, if desired.
    }

    @Override
    public void EntityRemoved(LaunchEntity entity)
    {
        //Optional: An entity was removed from the game. The bot can respond to this, if desired.
    }

    @Override
    public void MajorChanges()
    {
        //Optional: Major changes happened in the game (wars, alliances, and getting a snapshot) that would normally cause clients to completely redraw their map. The bot can respond to this.
    }

    @Override
    public void Authenticated()
    {
        //The bot has been authenticated, and can start to do stuff.
        bLoggedIn = true;
        LaunchLog.Log(APPLICATION, LOG_NAME, "I'm logged in; time to rock'n'roll!");
    }

    @Override
    public void AccountUnregistered()
    {
        //The bot doesn't yet have an account, and needs to register for one.
        LaunchLog.Log(APPLICATION, LOG_NAME, "I don't have an account, registering for one...");
        bLoggedIn = false;
        game.Register(DESIRED_PLAYER_NAME, 0);
    }

    @Override
    public void AccountNameTaken()
    {
        //TO DO: The bot tried to create an account, but the name was taken, so it would need to pick another name.
        
        //Just quit for now.
        LaunchLog.Log(APPLICATION, LOG_NAME, "My name's been taken! Fuck this shit...");
        System.exit(0);
    }

    @Override
    public void MajorVersionMismatch()
    {
        //The bot should quit until it's recompiled and re-released.
        LaunchLog.Log(APPLICATION, LOG_NAME, "I'm running completely the wrong version! Fuck this shit...");
        System.exit(0);
    }

    @Override
    public void MinorVersionMismatch()
    {
        //Shouldn't affect bots.
        LaunchLog.Log(APPLICATION, LOG_NAME, "I'm slightly out of date, but it's not the end of the world.");
    }

    @Override
    public void ActionSucceeded()
    {
        //(A task was successful.)
        //Now: This is no longer a reliable way to determine that we're idle.
    }

    @Override
    public void ActionFailed()
    {
        //Optional: This is the first port of call for any actions the AI does being invalid (though LaunchClientGame should never allow this to happen). Consider responding to them for debug purposes.
        throw new RuntimeException("Actions shouldn't fail!");
    }

    @Override
    public void ShowTaskMessage(Task.TaskMessage message)
    {
        //(Although we have no message to display, this is our cue that we've started a task.)
        //Now: This is no longer a reliable way to determine that we're doing something.
    }

    @Override
    public void DismissTaskMessage()
    {
        //A task finished. TO DO: We may actually want to do something with this.
    }

    @Override
    public void NewEvent(LaunchEvent event)
    {
        //Do nothing. AIs aren't interested in events.
        LaunchLog.Log(APPLICATION, LOG_NAME, "Event received that I don't really care about: " + event.GetMessage());
    }

    @Override
    public void NewReport(LaunchReport report)
    {
        //Do nothing. AIs aren't interested in reports.
        LaunchLog.Log(APPLICATION, LOG_NAME, "Report received that I don't really care about: " + report.GetMessage());
    }

    @Override
    public void Quit()
    {
        //Our account was voluntarily closed. Stop running.
        LaunchLog.Log(APPLICATION, LOG_NAME, "My account's been closed, I may gracefully leave now...");
        System.exit(0);
    }

    @Override
    public void AllianceCreated()
    {
        //Optional: Our AI can respond to new alliances being created.
    }

    @Override
    public String GetProcessNames()
    {
        return "Bot - No process names applicable";
    }

    @Override
    public void DisplayGeneralError()
    {
        //Nothing to do: The "general error" is the one displayed to people who the server thinks is cheating. Won't happen to bots.
    }

    @Override
    public void SaveImage(int lImageID, byte[] cData)
    {
        //Bots don't care about images.
    }

    @Override
    public boolean GetConnectionMobile()
    {
        //Doesn't do anything.
        return false;
    }

    @Override
    public void DeviceChecksRequested()
    {
        //Bots don't have to do device integrity checks. Return positive everything.
        game.DeviceCheck(false, false, 0, true, true);
    }

    @Override
    public void TreatyUpdated(Treaty treaty)
    {
        //Bots don't currently care about treaties.
    }

    @Override
    public void TempBanned(String strReason, long oDuration)
    {
        //Shouldn't happen to bots, but just quit if it does.
        LaunchLog.Log(APPLICATION, LOG_NAME, "My account's locked! Fuck this shit...");
        System.exit(0);
    }

    @Override
    public void PermBanned(String strReason)
    {
        //Shouldn't happen to bots, but just quit if it does.
        LaunchLog.Log(APPLICATION, LOG_NAME, "My account's locked! Fuck this shit...");
        System.exit(0);
    }

    @Override
    public void ReceiveUser(User user)
    {
        //Bots can't administrate.
    }
}
