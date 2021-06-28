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
public class RepairTask extends Task
{
    private int lSiteID;
    private StructureType structureType;
    
    public RepairTask(LaunchClientGameInterface gameInterface, int lSiteID, StructureType structureType)
    {
        super(gameInterface);
        this.structureType = structureType;
        this.lSiteID = lSiteID;
        gameInterface.ShowTaskMessage(TaskMessage.REPAIRING);
    }
    
    @Override
    public void Start(TobComm comm)
    {
        switch(structureType)
        {
            case MISSILE_SITE: comm.SendCommand(LaunchSession.RepairMissileSite, lSiteID); break;
            case SAM_SITE: comm.SendCommand(LaunchSession.RepairSAMSite, lSiteID); break;
            case SENTRY_GUN: comm.SendCommand(LaunchSession.RepairSentryGun, lSiteID); break;
            case ORE_MINE: comm.SendCommand(LaunchSession.RepairOreMine, lSiteID); break;
        }
    }
}
