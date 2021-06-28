/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.comm.clienttasks;

import launch.comm.LaunchSession;
import tobcomm.TobComm;
import launch.game.LaunchClientGameInterface;


/**
 *
 * @author tobster
 */
public class PurchaseSlotUpgradeTask extends Task
{
    private enum Context
    {
        PLAYER_MISSILE,
        MISSILE_SITE_MISSILE,
        PLAYER_INTERCEPTOR,
        SAM_SITE_INTERCEPTOR,
    }
    
    private Context context;
    
    private int lSiteID;
    
    public PurchaseSlotUpgradeTask(LaunchClientGameInterface gameInterface, boolean bIsMissile)
    {
        super(gameInterface);
        
        context = bIsMissile? Context.PLAYER_MISSILE : Context.PLAYER_INTERCEPTOR;
        
        gameInterface.ShowTaskMessage(TaskMessage.UPGRADING);
    }
    
    public PurchaseSlotUpgradeTask(LaunchClientGameInterface gameInterface, boolean bIsMissile, int lSiteID)
    {
        super(gameInterface);
        
        context = bIsMissile? Context.MISSILE_SITE_MISSILE: Context.SAM_SITE_INTERCEPTOR;
        
        gameInterface.ShowTaskMessage(TaskMessage.UPGRADING);
        
        this.lSiteID = lSiteID;
    }
    
    @Override
    public void Start(TobComm comm)
    {
        switch(context)
        {
            case PLAYER_MISSILE: comm.SendCommand(LaunchSession.PlayerMissileSlotUpgrade); break;
            case MISSILE_SITE_MISSILE: comm.SendCommand(LaunchSession.MissileSlotUpgrade, lSiteID); break;
            case PLAYER_INTERCEPTOR: comm.SendCommand(LaunchSession.PlayerInterceptorSlotUpgrade); break;
            case SAM_SITE_INTERCEPTOR: comm.SendCommand(LaunchSession.InterceptorSlotUpgrade, lSiteID); break;
        }
    }
}
