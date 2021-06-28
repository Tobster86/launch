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
public class SellSystemTask extends Task
{
    private boolean bIsMissilesNotSAM;
    
    public SellSystemTask(LaunchClientGameInterface gameInterface, boolean bIsMissilesNotSAM)
    {
        super(gameInterface);
        
        this.bIsMissilesNotSAM = bIsMissilesNotSAM;
        
        gameInterface.ShowTaskMessage(TaskMessage.SELLING);
    }
    
    @Override
    public void Start(TobComm comm)
    {
        comm.SendCommand(bIsMissilesNotSAM ?
                         LaunchSession.SellMissileSystem :
                         LaunchSession.SellSAMSystem);
    }
}
