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
public class CloseAccountTask extends Task
{
    public CloseAccountTask(LaunchClientGameInterface gameInterface)
    {
        super(gameInterface);
        gameInterface.ShowTaskMessage(TaskMessage.CLOSING_ACCOUNT);
    }
    
    @Override
    public void Start(TobComm comm)
    {
        comm.SendCommand(LaunchSession.CloseAccount);
    }

    @Override
    public void HandleCommand(int lCommand, int lInstanceNumber)
    {
        if(lCommand == LaunchSession.ActionSuccess)    
        {
            gameInterface.AccountClosed();
            Finish();
        }
        else
        {
            super.HandleCommand(lCommand, lInstanceNumber);
        }
    }
}
