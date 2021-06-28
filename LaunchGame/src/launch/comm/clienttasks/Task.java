/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.comm.clienttasks;

import launch.comm.LaunchSession;
import tobcomm.TobComm;
import launch.game.LaunchClientGameInterface;
import launch.utilities.LaunchLog;
import static launch.utilities.LaunchLog.LogType.TASKS;

/**
 *
 * @author tobster
 */
public abstract class Task
{
    public enum TaskMessage
    {
        REGISTERING,
        UPLOADING_AVATAR,
        RESPAWNING,
        CONSTRUCTING,
        PURCHASING,
        DECOMISSIONING,
        SELLING,
        LAUNCHING_MISSILE,
        LAUNCHING_INTERCEPTOR,
        CLOSING_ACCOUNT,
        REPAIRING,
        HEALING,
        CONFIGURING,
        UPGRADING,
        ALLIANCE_CREATE,
        ALLIANCE_JOIN,
        ALLIANCE_LEAVE,
        DECLARE_WAR,
        PROMOTING,
        ACCEPTING,
        REJECTING,
        KICKING,
        DIPLOMACY,
    }
    
    public enum StructureType
    {
        MISSILE_SITE,
        NUCLEAR_MISSILE_SITE,
        SAM_SITE,
        SENTRY_GUN,
        ORE_MINE
    }
    
    protected String strLogName = this.getClass().getName();
    
    private boolean bComplete = false;
    
    protected byte[] cData;
    
    protected LaunchClientGameInterface gameInterface;
    
    public Task(LaunchClientGameInterface gameInterface)
    {
        this.gameInterface = gameInterface;
    }
    
    public abstract void Start(TobComm comm);
    
    public boolean Complete() { return bComplete; }
    
    protected void Finish()
    {
        bComplete = true;
        LaunchLog.Log(TASKS, getClass().getName(), "Finished.");
        gameInterface.DismissTaskMessage();
    }
    
    public void HandleObject(int lObject, int lInstanceNumber, int lOffset, byte[] cData)
    {
        FailObject(lObject);
    }
    
    public void HandleCommand(int lCommand, int lInstanceNumber)
    {
        switch(lCommand)
        {
            case LaunchSession.ActionSuccess:
            {
                //Nothing to do.
            }
            break;
            
            case LaunchSession.ActionFailed:
            {
                //TO DO: Decide if there's anything to do here.
            }
            break;
            
            case LaunchSession.DisplayGeneralError:
            {
                gameInterface.DisplayGeneralError();
            }
            break;
            
            default:
            {
                FailCommand(lCommand);
            }
        }
        
        Finish();
    }
    
    protected void FailObject(int lObject)
    {
        LaunchLog.Log(TASKS, getClass().getName(), String.format("Unexpectedly received object %s!", lObject));
        throw new RuntimeException(String.format("Task %s unexpectedly received object %s.", getClass().getName(), lObject));
    }
    
    protected void FailCommand(int lObject)
    {
        LaunchLog.Log(TASKS, getClass().getName(), String.format("Unexpectedly received object %s!", lObject));
        throw new RuntimeException(String.format("Task %s unexpectedly received object %s.", getClass().getName(), lObject));
    }
}
