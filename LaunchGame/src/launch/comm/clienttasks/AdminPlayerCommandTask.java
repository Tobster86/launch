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
public class AdminPlayerCommandTask extends Task
{
    private int lPlayerID;
    private int lCommand;
    
    public AdminPlayerCommandTask(LaunchClientGameInterface gameInterface, int lPlayerID, int lCommand)
    {
        super(gameInterface);
        gameInterface.ShowTaskMessage(TaskMessage.CONFIGURING);
        this.lPlayerID = lPlayerID;
        this.lCommand = lCommand;
    }
    
    @Override
    public void Start(TobComm comm)
    {
        comm.SendCommand(lCommand, lPlayerID);
    }
}
