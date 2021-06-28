/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.comm.clienttasks;

import java.nio.ByteBuffer;
import java.util.List;
import launch.comm.LaunchSession;
import tobcomm.TobComm;
import launch.game.LaunchClientGameInterface;
import launch.game.entities.LaunchEntity;
import launch.utilities.LaunchUtilities;


/**
 *
 * @author tobster
 */
public class SAMSiteModeTask extends Task
{
    private int lSiteID = LaunchEntity.ID_NONE;
    
    public SAMSiteModeTask(LaunchClientGameInterface gameInterface, int lSiteID, byte cMode)
    {
        super(gameInterface);
        
        gameInterface.ShowTaskMessage(TaskMessage.CONFIGURING);
        
        this.lSiteID = lSiteID;
        
        ByteBuffer bb = ByteBuffer.allocate(Byte.BYTES);
        bb.put(cMode);
        cData = bb.array();
    }
    
    public SAMSiteModeTask(LaunchClientGameInterface gameInterface, List<Integer> SiteIDs, byte cMode)
    {
        super(gameInterface);
        
        gameInterface.ShowTaskMessage(TaskMessage.CONFIGURING);
        
        byte[] cIntListData = LaunchUtilities.GetIntListData(SiteIDs);
        ByteBuffer bb = ByteBuffer.allocate(Byte.BYTES + cIntListData.length);
        bb.put(cMode);
        bb.put(cIntListData);
        cData = bb.array();
    }
    
    @Override
    public void Start(TobComm comm)
    {
        comm.SendObject(LaunchSession.SAMSiteModeChange, lSiteID, cData);
    }
}
