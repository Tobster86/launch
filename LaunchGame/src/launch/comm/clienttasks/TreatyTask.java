/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.comm.clienttasks;

import launch.comm.LaunchSession;
import tobcomm.TobComm;
import launch.game.LaunchClientGameInterface;
import launch.game.treaties.Treaty.Type;


/**
 *
 * @author tobster
 */
public class TreatyTask extends Task
{
    private int lAllianceID;
    private Type type;
    
    public TreatyTask(LaunchClientGameInterface gameInterface, int lAllianceID, Type type)
    {
        super(gameInterface);
        
        switch(type)
        {
            case WAR: gameInterface.ShowTaskMessage(TaskMessage.DECLARE_WAR); break;
            default: gameInterface.ShowTaskMessage(TaskMessage.DIPLOMACY); break;
        }
        
        this.type = type;
        this.lAllianceID = lAllianceID;
    }
    
    @Override
    public void Start(TobComm comm)
    {
        switch(type)
        {
            case WAR: comm.SendCommand(LaunchSession.DeclareWar, lAllianceID); break;
            case AFFILIATION_REQUEST: comm.SendCommand(LaunchSession.ProposeAffiliation, lAllianceID); break;
            case AFFILIATION: comm.SendCommand(LaunchSession.AcceptAffiliation, lAllianceID); break;
            case AFFILIATION_REJECT: comm.SendCommand(LaunchSession.RejectAffiliation, lAllianceID); break;
        }
    }
}
