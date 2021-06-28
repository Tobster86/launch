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
public class StructureRenameTask extends Task
{
    private StructureType type;
    
    public StructureRenameTask(LaunchClientGameInterface gameInterface, int lSiteID, String strName, StructureType type)
    {
        super(gameInterface);
        
        gameInterface.ShowTaskMessage(TaskMessage.CONFIGURING);

        this.type = type;
        
        ByteBuffer bb = ByteBuffer.allocate(4 + LaunchUtilities.GetStringDataSize(strName));
        bb.putInt(lSiteID);
        bb.put(LaunchUtilities.GetStringData(strName));
        cData = bb.array();
    }
    
    @Override
    public void Start(TobComm comm)
    {
        switch(type)
        {
            case MISSILE_SITE: comm.SendObject(LaunchSession.MissileSiteNameChange, cData); break;
            case SAM_SITE: comm.SendObject(LaunchSession.SAMSiteNameChange, cData); break;
            case SENTRY_GUN: comm.SendObject(LaunchSession.SentryGunNameChange, cData); break;
            case ORE_MINE: comm.SendObject(LaunchSession.OreMineNameChange, cData); break;
        }
    }
}
