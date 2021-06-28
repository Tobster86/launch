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
public class AllianceJoinTask extends Task
{
    private int lPlayerID;
    private boolean bReject; //Reject (as opposed to accept).
    
    public AllianceJoinTask(LaunchClientGameInterface gameInterface, int lPlayerID, boolean bReject)
    {
        super(gameInterface);
        
        if(bReject)
            gameInterface.ShowTaskMessage(TaskMessage.REJECTING);
        else
            gameInterface.ShowTaskMessage(TaskMessage.ACCEPTING);

        this.lPlayerID = lPlayerID;
        this.bReject = bReject;
    }
    
    @Override
    public void Start(TobComm comm)
    {
        comm.SendCommand(bReject ? LaunchSession.RejectJoin : LaunchSession.AcceptJoin, lPlayerID);
    }
}
