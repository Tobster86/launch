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
public class SetAvatarTask extends Task
{
    int lAvatarID;
    boolean bIsAlliance;
    
    public SetAvatarTask(LaunchClientGameInterface gameInterface, int lAvatarID, boolean bAlliance)
    {
        super(gameInterface);
        
        this.lAvatarID = lAvatarID;
        this.bIsAlliance = bAlliance;
        
        gameInterface.ShowTaskMessage(TaskMessage.UPLOADING_AVATAR);
    }
    
    @Override
    public void Start(TobComm comm)
    {
        if(bIsAlliance)
            comm.SendCommand(LaunchSession.SetAllianceAvatar, lAvatarID);
        else
            comm.SendCommand(LaunchSession.SetAvatar, lAvatarID);
    }
}
