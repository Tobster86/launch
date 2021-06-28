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
public class BanTask extends Task
{
    private int lPlayerID;
    
    public BanTask(LaunchClientGameInterface gameInterface, String strReason, int lPlayerID, boolean bPermanent)
    {
        super(gameInterface);
        gameInterface.ShowTaskMessage(TaskMessage.KICKING);
        this.lPlayerID = lPlayerID;
        byte[] cReasonData = LaunchUtilities.GetStringData(strReason);
        ByteBuffer bb = ByteBuffer.allocate(cReasonData.length + 1);
        bb.put(cReasonData);
        bb.put((byte)(bPermanent ? 0xFF : 0x00));
        cData = bb.array();
    }
    
    @Override
    public void Start(TobComm comm)
    {
        comm.SendObject(LaunchSession.Ban, lPlayerID, cData);
    }
}
