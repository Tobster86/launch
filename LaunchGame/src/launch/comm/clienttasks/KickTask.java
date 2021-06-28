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
public class KickTask extends Task
{
    private int lPlayerID;
    
    public KickTask(LaunchClientGameInterface gameInterface, int lPlayerID)
    {
        super(gameInterface);
        gameInterface.ShowTaskMessage(TaskMessage.KICKING);
        this.lPlayerID = lPlayerID;
    }
    
    @Override
    public void Start(TobComm comm)
    {
        comm.SendCommand(LaunchSession.Kick, lPlayerID);
    }
}
