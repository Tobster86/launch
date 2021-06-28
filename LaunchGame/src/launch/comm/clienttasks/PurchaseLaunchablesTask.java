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
public class PurchaseLaunchablesTask extends Task
{
    private boolean bIsMissiles;
    
    public PurchaseLaunchablesTask(LaunchClientGameInterface gameInterface, boolean bIsMissiles, int lSiteID, byte cSlotNo, byte[] cTypes)
    {
        super(gameInterface);
        
        this.bIsMissiles = bIsMissiles;
        
        gameInterface.ShowTaskMessage(TaskMessage.PURCHASING);
        
        ByteBuffer bb = ByteBuffer.allocate(5 + cTypes.length);
        bb.putInt(lSiteID);
        bb.put(cSlotNo);
        bb.put(cTypes);
        cData = bb.array();
    }
    
    @Override
    public void Start(TobComm comm)
    {
        comm.SendObject(bIsMissiles? LaunchSession.PurchaseMissiles : LaunchSession.PurchaseInterceptors, cData);
    }
}
