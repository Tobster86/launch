/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.comm.clienttasks;

import java.nio.ByteBuffer;
import launch.comm.LaunchSession;
import tobcomm.TobComm;
import launch.game.GeoCoord;
import launch.game.LaunchClientGameInterface;


/**
 *
 * @author tobster
 */
public class LaunchSomethingTask extends Task
{
    private enum Context
    {
        PLAYER_MISSILE,
        MISSILE_SITE_MISSILE,
        PLAYER_INTERCEPTOR,
        SAM_SITE_INTERCEPTOR,
    }
    
    private Context context;
    
    public LaunchSomethingTask(LaunchClientGameInterface gameInterface, byte cSlotNo, boolean bTracking, GeoCoord geoTarget, int lTargetID)
    {
        super(gameInterface);
        
        context = Context.PLAYER_MISSILE;
        
        gameInterface.ShowTaskMessage(TaskMessage.LAUNCHING_MISSILE);
        
        ByteBuffer bb = ByteBuffer.allocate(14);
        bb.put(cSlotNo);
        bb.put((byte)(bTracking ? 0xFF : 0x00));
        bb.putFloat(geoTarget.GetLatitude());
        bb.putFloat(geoTarget.GetLongitude());
        bb.putInt(lTargetID);
        cData = bb.array();
    }
    
    public LaunchSomethingTask(LaunchClientGameInterface gameInterface, int lSiteID, byte cSlotNo, boolean bTracking, GeoCoord geoTarget, int lTargetID)
    {
        super(gameInterface);
        
        context = Context.MISSILE_SITE_MISSILE;
        
        gameInterface.ShowTaskMessage(TaskMessage.LAUNCHING_MISSILE);
        
        ByteBuffer bb = ByteBuffer.allocate(18);
        bb.putInt(lSiteID);
        bb.put(cSlotNo);
        bb.put((byte)(bTracking ? 0xFF : 0x00));
        bb.putFloat(geoTarget.GetLatitude());
        bb.putFloat(geoTarget.GetLongitude());
        bb.putInt(lTargetID);
        cData = bb.array();
    }
    
    public LaunchSomethingTask(LaunchClientGameInterface gameInterface, byte cSlotNo, int lTargetID)
    {
        super(gameInterface);
        
        context = Context.PLAYER_INTERCEPTOR;
        
        gameInterface.ShowTaskMessage(TaskMessage.LAUNCHING_INTERCEPTOR);
        
        ByteBuffer bb = ByteBuffer.allocate(5);
        bb.put(cSlotNo);
        bb.putInt(lTargetID);
        cData = bb.array();
    }
    
    public LaunchSomethingTask(LaunchClientGameInterface gameInterface, int lSiteID, byte cSlotNo, int lTargetID)
    {
        super(gameInterface);
        
        context = Context.SAM_SITE_INTERCEPTOR;
        
        gameInterface.ShowTaskMessage(TaskMessage.LAUNCHING_INTERCEPTOR);
        
        ByteBuffer bb = ByteBuffer.allocate(9);
        bb.putInt(lSiteID);
        bb.put(cSlotNo);
        bb.putInt(lTargetID);
        cData = bb.array();
    }
    
    @Override
    public void Start(TobComm comm)
    {
        switch(context)
        {
            case PLAYER_MISSILE: comm.SendObject(LaunchSession.LaunchPlayerMissile, cData); break;
            case MISSILE_SITE_MISSILE: comm.SendObject(LaunchSession.LaunchMissile, cData); break;
            case PLAYER_INTERCEPTOR: comm.SendObject(LaunchSession.LaunchPlayerInterceptor, cData); break;
            case SAM_SITE_INTERCEPTOR: comm.SendObject(LaunchSession.LaunchInterceptor, cData); break;
        }
    }
}
