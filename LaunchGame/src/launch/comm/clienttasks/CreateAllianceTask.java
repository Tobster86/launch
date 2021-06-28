/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.comm.clienttasks;

import java.nio.ByteBuffer;
import launch.comm.LaunchSession;
import tobcomm.TobComm;
import launch.game.LaunchClientGameInterface;
import launch.utilities.LaunchUtilities;


/**
 *
 * @author tobster
 */
public class CreateAllianceTask extends Task
{
    public CreateAllianceTask(LaunchClientGameInterface gameInterface, String strName, String strDescription, int lAvatarID)
    {
        super(gameInterface);
        gameInterface.ShowTaskMessage(TaskMessage.ALLIANCE_CREATE);
        
        ByteBuffer bb = ByteBuffer.allocate(LaunchUtilities.GetStringDataSize(strName) + LaunchUtilities.GetStringDataSize(strDescription) + 4);
        bb.put(LaunchUtilities.GetStringData(strName));
        bb.put(LaunchUtilities.GetStringData(strDescription));
        bb.putInt(lAvatarID);
        cData = bb.array();
    }
    
    @Override
    public void Start(TobComm comm)
    {
        comm.SendObject(LaunchSession.CreateAlliance, 0, 0, cData);
    }

    @Override
    public void HandleCommand(int lCommand, int lInstanceNumber)
    {
        if(lCommand == LaunchSession.ActionSuccess)
        {
            gameInterface.AllianceCreated();
            Finish();
        }
        else
        {
            super.HandleCommand(lCommand, lInstanceNumber);
        }
    }
}
