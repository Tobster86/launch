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
public class SiteOnlineOfflineTask extends Task
{
    private int lSiteID = LaunchEntity.ID_NONE;
    private List<Integer> SiteIDs;
    
    private StructureType structureType;
    
    public SiteOnlineOfflineTask(LaunchClientGameInterface gameInterface, int lSiteID, StructureType structureType, boolean bOnline)
    {
        super(gameInterface);
        
        this.structureType = structureType;
        this.lSiteID = lSiteID;
        
        gameInterface.ShowTaskMessage(TaskMessage.CONFIGURING);
        
        cData = new byte[] { (byte)(bOnline ? 0xFF : 0x00) };
    }
    
    public SiteOnlineOfflineTask(LaunchClientGameInterface gameInterface, List<Integer> SiteIDs, StructureType structureType, boolean bOnline)
    {
        super(gameInterface);
        
        this.structureType = structureType;
        this.SiteIDs = SiteIDs;
        
        gameInterface.ShowTaskMessage(TaskMessage.CONFIGURING);
        
        byte[] cIntListData = LaunchUtilities.GetIntListData(SiteIDs);
        ByteBuffer bb = ByteBuffer.allocate(Byte.BYTES + cIntListData.length);
        bb.put((byte)(bOnline ? 0xFF : 0x00));
        bb.put(cIntListData);
        
        cData = bb.array();
    }
    
    @Override
    public void Start(TobComm comm)
    {
        switch(structureType)
        {
            case MISSILE_SITE: comm.SendObject(LaunchSession.MissileSitesOnOff, lSiteID, 0, cData); break;
            case SAM_SITE: comm.SendObject(LaunchSession.SAMSitesOnOff, lSiteID, 0, cData); break;
            case SENTRY_GUN: comm.SendObject(LaunchSession.SentryGunsOnOff, lSiteID, 0, cData); break;
            case ORE_MINE: comm.SendObject(LaunchSession.OreMinesOnOff, lSiteID, 0, cData); break;
        }
    }
}
