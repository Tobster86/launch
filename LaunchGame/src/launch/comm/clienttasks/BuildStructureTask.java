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
public class BuildStructureTask extends Task
{
    private StructureType structureType;
    
    public BuildStructureTask(LaunchClientGameInterface gameInterface, StructureType structureType)
    {
        super(gameInterface);
        
        this.structureType = structureType;
        
        gameInterface.ShowTaskMessage(TaskMessage.CONSTRUCTING);
        
        if(structureType == StructureType.MISSILE_SITE)
            cData = new byte[] { (byte)(0x00) };
        
        if(structureType == StructureType.NUCLEAR_MISSILE_SITE)
            cData = new byte[] { (byte)(0xFF) };
    }
    
    @Override
    public void Start(TobComm comm)
    {
        switch(structureType)
        {
            case MISSILE_SITE:
            case NUCLEAR_MISSILE_SITE:
            {
                comm.SendObject(LaunchSession.BuildMissileSite, 0, 0, cData);
            }
            break;
            
            case SAM_SITE: comm.SendCommand(LaunchSession.BuildSamSite); break;
            case SENTRY_GUN: comm.SendCommand(LaunchSession.BuildSentryGun); break;
            case ORE_MINE: comm.SendCommand(LaunchSession.BuildOreMine); break;
        }
    }
}
