/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launchserver;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import launch.game.Alliance;
import launch.game.Config;
import launch.game.GeoCoord;
import launch.game.LaunchServerAppInterface;
import launch.game.LaunchServerGame;
import launch.game.User;
import launch.game.entities.Interceptor;
import launch.game.entities.Loot;
import launch.game.entities.Missile;
import launch.game.entities.MissileSite;
import launch.game.entities.OreMine;
import launch.game.entities.Player;
import launch.game.entities.Radiation;
import launch.game.entities.SAMSite;
import launch.game.entities.SentryGun;
import launch.game.systems.MissileSystem;
import launch.game.treaties.Affiliation;
import launch.game.treaties.AffiliationRequest;
import launch.game.treaties.War;
import launch.utilities.LaunchBannedApp;
import launch.utilities.LaunchLog;
import static launch.utilities.LaunchLog.LogType.*;
import storage.GameLoadSaveListener;
import storage.XMLGameLoader;
import storage.XMLGameSaver;

/**
 *
 * @author tobster
 */
public class LaunchServer implements LaunchServerAppInterface, GameLoadSaveListener
{
    private static final String DIRECTORY_AVATARS = "avatars/";
    private static final String LOG_NAME = "Server";
    
    private static final String CONFIG_EXTENSION = ".xml";
    private static final String GAME_FILENAME = "game" + CONFIG_EXTENSION;
    private static final String CONFIG_FILENAME = "config" + CONFIG_EXTENSION;
    private static final String BACKUP_FOLDER = "backups/%s/";
    private static final String BACKUP_FILE = "%s%s" + CONFIG_EXTENSION;
    
    private static final DateFormat dateFormatDay = new SimpleDateFormat("yyyyMMdd");
    private static final DateFormat dateFormatTime = new SimpleDateFormat("HHmmss");
    
    private LaunchServerGame game = null;
    
    private boolean bLoadErrors = false;
        
    private enum ConsoleState
    {
        IDLE,
        EVENT_TEXT,
        EVENT_CONFIRM
    }
    
    private ConsoleState consoleState = ConsoleState.IDLE;
    private String strEventMessageText;
    
    public LaunchServer()
    {
        //Set up initial logging states.
        LaunchLog.SetLoggingEnabled(SESSION, true, true);
        LaunchLog.SetLoggingEnabled(COMMS, true, true);
        LaunchLog.SetLoggingEnabled(APPLICATION, true, true);
        LaunchLog.SetLoggingEnabled(GAME, false, true);
        LaunchLog.SetLoggingEnabled(TASKS, false, true);
        LaunchLog.SetLoggingEnabled(SERVICES, true, true);
        LaunchLog.SetLoggingEnabled(LOCATIONS, false, true);
        LaunchLog.SetLoggingEnabled(POISON, true, true);
        LaunchLog.SetLoggingEnabled(PERFORMANCE, true, true);
        LaunchLog.SetLoggingEnabled(CHEATING, true, true);
        LaunchLog.SetLoggingEnabled(NOTIFICATIONS, true, true);
        LaunchLog.SetLoggingEnabled(DEVICE_CHECKS, false, true);
        LaunchLog.SetLoggingEnabled(SAM_SITE_AI, false, true);
        LaunchLog.SetLoggingEnabled(DEBUG, true, true);
        
        LaunchLog.Log(APPLICATION, LOG_NAME, "Firing up Launch server...");
        
        //Create directories if they don't exist.
        new File(DIRECTORY_AVATARS + "tmp").getParentFile().mkdir();
        
        Config config = XMLGameLoader.LoadConfig(this, CONFIG_FILENAME);
        
        if(config != null)
        {
            game = new LaunchServerGame(config, this, config.GetPort());
            
            LaunchLog.Log(APPLICATION, LOG_NAME, "Loading game...");
            XMLGameLoader.LoadGame(this, GAME_FILENAME, game);
            LaunchLog.Log(APPLICATION, LOG_NAME, "...game loaded.");
        }
        
        if(bLoadErrors)
        {
            LaunchLog.Log(APPLICATION, LOG_NAME, "There were errors when loading the game. Quitting...");
            game.ShutDown();
        }
        else
        {
            game.StartServices();
            
            LaunchLog.Log(APPLICATION, LOG_NAME, "...We're running.");
            
            LaunchConsole console = new LaunchConsole(this, game);
        
            while(!console.Quat() && game.GetRunning())
            {
                console.Tick();
            }
            
            LaunchLog.Log(APPLICATION, LOG_NAME, "Quit. Waiting for game to stop running...");
            
            //Wait for game to shut down when we quit.
            while(game.GetRunning())
            {
                try
                {
                    Thread.sleep(100);
                }
                catch(Exception ex) { /* Don't care */ }
            }
        }
        
        LaunchLog.Log(APPLICATION, LOG_NAME, "Goodbye.");
    }
    
    
    //---------------------------------------------------------------------------------------------------------------------------------
    // LaunchServerAppInterface methods.
    //---------------------------------------------------------------------------------------------------------------------------------

    @Override
    public void SaveTheGame()
    {
        try
        {
            if(!bLoadErrors)
            {
                //Create files and folders if they don't exist.
                Date now = Calendar.getInstance().getTime();
                String strBackupFolder = String.format(BACKUP_FOLDER, dateFormatDay.format(now));
                String strBackupFile = String.format(BACKUP_FILE, strBackupFolder, dateFormatTime.format(now));

                new File(GAME_FILENAME).createNewFile();
                new File(strBackupFolder).mkdirs();
                new File(strBackupFile).createNewFile();

                LaunchLog.Log(APPLICATION, LOG_NAME, "Saving and backing up the game.");
                XMLGameSaver.SaveGameToXMLFile(this, game, GAME_FILENAME);

                //Take a backup.
                XMLGameSaver.SaveGameToXMLFile(this, game, strBackupFile);
            }
            else
            {
                LaunchLog.Log(APPLICATION, LOG_NAME, "Did not save/backup the game as it didn't load correctly.");
            }
        }
        catch(IOException ex)
        {
            LaunchLog.Log(APPLICATION, LOG_NAME, "Did not save/backup the game due to IO error.");
        }
    }
            
    
    //---------------------------------------------------------------------------------------------------------------------------------
    // GameLoadSaveListener methods.
    //---------------------------------------------------------------------------------------------------------------------------------

    @Override
    public void LoadError(String strDescription)
    {
        LaunchLog.Log(APPLICATION, LOG_NAME, "Load error: " + strDescription);
        bLoadErrors = true;
    }

    @Override
    public void LoadWarning(String strDescription)
    {
        LaunchLog.Log(APPLICATION, LOG_NAME, "Load warning: " + strDescription);
    }

    @Override
    public void SaveError(String strDescription)
    {
        LaunchLog.Log(APPLICATION, LOG_NAME, "Save error: " + strDescription);
    }
}
