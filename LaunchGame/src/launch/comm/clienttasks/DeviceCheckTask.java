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


/**
 *
 * @author tobster
 */
public class DeviceCheckTask extends Task
{
    public DeviceCheckTask(LaunchClientGameInterface gameInterface, boolean bCompleteFailure, boolean bAPIFailure, int lFailureCode, boolean bProfileMatch, boolean bBasicIntegrity)
    {
        super(gameInterface);
                
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.put((byte)(bCompleteFailure? 0xFF : 0x00));
        bb.put((byte)(bAPIFailure? 0xFF : 0x00));
        bb.putInt(lFailureCode);
        bb.put((byte)(bProfileMatch? 0xFF : 0x00));
        bb.put((byte)(bBasicIntegrity? 0xFF : 0x00));
        cData = bb.array();
    }
    
    @Override
    public void Start(TobComm comm)
    {
        comm.SendObject(LaunchSession.DeviceCheck, 0, 0, cData);
        
        //Fire & forget.
        Finish();
    }
}
