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
public class RenameTask extends Task
{
    public enum Context
    {
        Player,
        AllianceName,
        AllianceDescription,
    }
    
    private Context context;
    
    public RenameTask(LaunchClientGameInterface gameInterface, String strName, Context context)
    {
        super(gameInterface);
        
        gameInterface.ShowTaskMessage(TaskMessage.CONFIGURING);

        this.context = context;
        
        ByteBuffer bb = ByteBuffer.allocate(LaunchUtilities.GetStringDataSize(strName));
        bb.put(LaunchUtilities.GetStringData(strName));
        cData = bb.array();
    }
    
    @Override
    public void Start(TobComm comm)
    {
        switch(context)
        {
            case Player: comm.SendObject(LaunchSession.RenamePlayer, cData); break;
            case AllianceName: comm.SendObject(LaunchSession.RenameAlliance, cData); break;
            case AllianceDescription: comm.SendObject(LaunchSession.RedescribeAlliance, cData); break;
        }
    }
}
