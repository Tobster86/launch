/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.comm.clienttasks;

import launch.comm.LaunchSession;
import tobcomm.TobComm;
import launch.game.LaunchClientGameInterface;


/**
 *
 * @author tobster
 */
public class SellStructureTask extends Task
{
    private StructureType structureType;
    private int lSiteID;
    
    public SellStructureTask(LaunchClientGameInterface gameInterface, StructureType structureType, int lSiteID)
    {
        super(gameInterface);
        
        this.structureType = structureType;
        this.lSiteID = lSiteID;
        
        gameInterface.ShowTaskMessage(TaskMessage.DECOMISSIONING);
    }
    
    @Override
    public void Start(TobComm comm)
    {
        switch(structureType)
        {
            case MISSILE_SITE: comm.SendCommand(LaunchSession.SellMissileSite, lSiteID); break;
            case SAM_SITE: comm.SendCommand(LaunchSession.SellSAMSite, lSiteID); break;
            case SENTRY_GUN: comm.SendCommand(LaunchSession.SellSentryGun, lSiteID); break;
            case ORE_MINE: comm.SendCommand(LaunchSession.SellOreMine, lSiteID); break;
        }
    }
}
