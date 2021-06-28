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
public class SellLaunchableTask extends Task
{
    private boolean bIsMissiles;
    
    /**
     * Task to sell a launchable.
     * @param gameInterface
     * @param bIsMissile
     * @param lSiteID
     * @param cSlotNo
     */
    public SellLaunchableTask(LaunchClientGameInterface gameInterface, boolean bIsMissiles, int lSiteID, byte cSlotNo)
    {
        super(gameInterface);
        
        this.bIsMissiles = bIsMissiles;
        
        gameInterface.ShowTaskMessage(TaskMessage.SELLING);
        
        ByteBuffer bb = ByteBuffer.allocate(5);
        bb.putInt(lSiteID);
        bb.put(cSlotNo);
        cData = bb.array();
    }
    
    @Override
    public void Start(TobComm comm)
    {
        comm.SendObject(bIsMissiles? LaunchSession.SellMissile : LaunchSession.SellInterceptor, cData);
    }
}
