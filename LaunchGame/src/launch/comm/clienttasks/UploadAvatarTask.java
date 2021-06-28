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
import launch.utilities.LaunchLog;


/**
 *
 * @author tobster
 */
public class UploadAvatarTask extends Task
{
    byte[] cData;
    
    public UploadAvatarTask(LaunchClientGameInterface gameInterface, byte[] cData)
    {
        super(gameInterface);
        gameInterface.ShowTaskMessage(TaskMessage.UPLOADING_AVATAR);
        this.cData = cData;
    }
    
    @Override
    public void Start(TobComm comm)
    {
        comm.SendObject(LaunchSession.Avatar, cData);
    }

    @Override
    public void HandleObject(int lObject, int lInstanceNumber, int lOffset, byte[] cData)
    {
        switch(lObject)
        {
            case LaunchSession.Avatar:
            {
                LaunchLog.Log(LaunchLog.LogType.TASKS, strLogName, String.format("Avatar %d downloaded.", lInstanceNumber));
                gameInterface.AvatarReceived(lInstanceNumber, cData);
                gameInterface.AvatarUploaded(lInstanceNumber);
                Finish();
            }
            break;
            
            default:
            {
                super.HandleObject(lObject, lInstanceNumber, lOffset, cData);
            }
        }
    }
}
