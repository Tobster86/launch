/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.game;

import launch.game.treaties.Treaty;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import launch.comm.LaunchClientComms;
import launch.comm.clienttasks.Task;
import launch.game.entities.*;
import launch.game.treaties.AffiliationRequest;
import launch.game.treaties.War;
import launch.utilities.LaunchEvent;
import launch.utilities.LaunchClientLocation;
import launch.utilities.LaunchLog;
import launch.utilities.LaunchReport;
import launch.utilities.LaunchUtilities;
import launch.utilities.LongDelay;
import launch.utilities.PrivacyZone;

/**
 *
 * @author tobster
 */
public class LaunchClientGame extends LaunchGame implements LaunchClientGameInterface
{
    private final LaunchClientAppInterface application;
    private LaunchClientComms comms = null;
    
    private List<PrivacyZone> PrivacyZones = new ArrayList<>();
    private boolean bInPrivacyZone = false;
    
    private int lOurPlayerID = LaunchEntity.ID_NONE;
    
    private int lLatency = Defs.LATENCY_DISCONNECTED;
    
    private boolean bAuthenticated = false;
    
    private LinkedList<LaunchEvent> Events = new LinkedList();
    private final LinkedHashMap<String, LaunchReport> NewReports = new LinkedHashMap<>();
    private final LinkedHashMap<String, LaunchReport> OldReports = new LinkedHashMap<>();
    
    private Map<Integer, Player> NewPlayers;
    private Map<Integer, Missile> NewMissiles;
    private Map<Integer, Interceptor> NewInterceptors;
    private Map<Integer, MissileSite> NewMissileSites;
    private Map<Integer, SAMSite> NewSAMSites;
    private Map<Integer, SentryGun> NewSentryGuns;
    private Map<Integer, OreMine> NewOreMines;
    private Map<Integer, Loot> NewLoots;
    private Map<Integer, Radiation> NewRadiations;
    private Map<Integer, Alliance> NewAlliances;
    private Map<Integer, Treaty> NewTreaties;
    private List<Structure> NewAllStructures;
    private boolean bReceivingSnapshot = false;
    
    private LongDelay dlyUntilCommsAttempts = new LongDelay();
    
    private boolean bClosingAccount = false; //Used to suppress comms activities when the player is trying to close their account.

    //When downloading a snapshot the game is effectively stalled, so this accumulates the time for one big "catchup tick".
    private int lExtraTickTime = 0;
    
    public LaunchClientGame(Config config, LaunchClientAppInterface application, List<PrivacyZone> PrivacyZones, String strURL, int lPort)
    {
        super(config);
        this.application = application;
        this.PrivacyZones = PrivacyZones;
        comms = new LaunchClientComms(this, strURL, lPort);
        
        StartServices();
    }
    
    public void SetDeviceID(byte[] cDeviceID, String strDeviceName, String strProcessName)
    {
        comms.SetDeviceID(cDeviceID, strDeviceName, strProcessName);
    }

    @Override
    protected void CommsTick(int lMS)
    {
        lCommTickStarts++;
        comms.Tick(lMS);
        lCommTickEnds++;
    }

    @Override
    protected void GameTick(int lMS)
    {
        //For unban reconnection.
        dlyUntilCommsAttempts.Tick(lMS);
        
        //We may not have a config yet. Don't do anything until we do, and it's been verified (bAuthenticated won't be true until the config is known to be good).
        if(config != null && bAuthenticated)
        {
            //Don't tick while receiving a snapshot, just accumulate the time for one big catch-up tick when it arrives.
            if(bReceivingSnapshot)
            {
                lExtraTickTime += lMS;
            }
            else
            {
                super.GameTick(lMS + lExtraTickTime);

                lExtraTickTime = 0;
            }
        }
        
        application.GameTicked(lMS);
        
        lGameTickEnds++;
    }
    
    public void Suspend()
    {
        comms.Suspend();
        
        //Current task will have been killed to prevent confusing player. Dismiss any remaining task messages.
        application.DismissTaskMessage();
    }
    
    public void Resume()
    {
        if(dlyUntilCommsAttempts.Expired())
        {
            comms.Resume();
        }
    }
    
    public int GetCommsReinitRemaining()
    {
        return comms.GetReinitRemaining();
    }

    public boolean GetCommsDoingAnything()
    {
        return comms.GetDoingAnything();
    }
    
    public int GetCommsDownloadRate()
    {
        return comms.GetDownloadRate();
    }
    
    public List<LaunchEntity> GetNearestEntities(GeoCoord geoPosition, int lMaxEntities)
    {
        List<LaunchEntity> Result = new ArrayList<>();
        
        //Return up to a number of the nearest physically interactable entities from the specified position.
        //Create a list of all relevant entities.
        List<LaunchEntity> AllEntities = new ArrayList();
        AllEntities.addAll(Players.values());
        AllEntities.addAll(Missiles.values());
        AllEntities.addAll(Interceptors.values());
        AllEntities.addAll(GetAllStructures());
        AllEntities.addAll(Loots.values());

        //Find the nearest entities and store them in a list.
        int lExcluded = 0;
        while((Result.size() < lMaxEntities) && ((Result.size() + lExcluded) < AllEntities.size())) //Second operand of '&&' caps against there not being enough valid entities.
        {
            LaunchEntity nextNearest = null;
            float fltDistanceNearest = Float.MAX_VALUE;

            for (LaunchEntity entity : AllEntities)
            {
                boolean bCanShow = true;

                //Don't include unshowable entities.
                if(!LaunchUtilities.GetEntityVisibility(this, entity) || !entity.GetPosition().GetValid())
                {
                    lExcluded++;
                    bCanShow = false;
                }
                else if(entity instanceof Player)
                {
                    //Don't include dead or AWOL players.
                    if(!((Player)entity).Functioning())
                    {
                        lExcluded++;
                        bCanShow = false;
                    }
                }

                if(bCanShow)
                {
                    float fltDistanceTo = entity.GetPosition().DistanceTo(geoPosition);

                    if ((fltDistanceTo <= fltDistanceNearest) && (!Result.contains(entity)))
                    {
                        nextNearest = entity;
                        fltDistanceNearest = fltDistanceTo;
                    }
                }
            }

            if(nextNearest != null)
            {
                Result.add(nextNearest);
            }
        }
        
        return Result;
    }
    
    public boolean GetPlayerHasNoAirCover(Player player)
    {
        if(player.GetHasAirDefenceSystem())
        {
            return false;
        }

        for(SAMSite samSite : SAMSites.values())
        {
            if(samSite.GetOwnerID() == player.GetID())
            {
                return false;
            }
        }

        for(SentryGun sentryGun : SentryGuns.values())
        {
            if(sentryGun.GetOwnerID() == player.GetID())
            {
                return false;
            }
        }
        
        return true;
    }
    
    public boolean GetPlayerHasNoAirOffenseCapability(Player player)
    {
        if(player.GetHasCruiseMissileSystem())
        {
            return false;
        }
        
        for(MissileSite missileSite : MissileSites.values())
        {
            if(missileSite.GetOwnerID() == player.GetID())
            {
                return false;
            }
        }
        
        return true;
    }
    
    public int GetPlayerTotalHourlyIncome(Player player)
    {
        int lIncome = GetHourlyIncome(player);
        
        for(Structure structure : GetAllStructures())
        {
            if(structure.GetOwnerID() == player.GetID())
            {
                if(structure.GetOnline() || structure.GetBooting())
                {
                    lIncome -= config.GetMaintenanceCost(structure);
                }
            }
        }
        
        return lIncome;
    }
    
    private Map<Byte, Integer> GetPricesFromData(byte[] cData)
    {
        Map<Byte, Integer> Result = new HashMap<>();
        
        ByteBuffer bb = ByteBuffer.wrap(cData);
        
        while(bb.hasRemaining())
        {
            Result.put(bb.get(), bb.getInt());
        }
        
        return Result;
    }
    
    public boolean GetAnyAlerts(Player player)
    {
        if(player != null)
        {
            if(GetPlayerHasNoAirCover(player))
                return true;

            if(GetPlayerHasNoAirOffenseCapability(player))
                return true;

            if(GetPlayerTotalHourlyIncome(player) <= 0)
                return true;

            if(player.Destroyed())
                return true;

            if(GetRadioactive(player, true))
                return true;
        }
        
        return false;
    }
    
    public int GetTimeToPlayerFullHealth(Player player)
    {
        return player.GetHPDeficit() * config.GetHealthInterval();
    }

    public int GetTimeToPlayerRadiationDeath(Player player)
    {
        return player.GetHP() * config.GetRadiationInterval();
    }
    
    public Player GetOurPlayer()
    {
        if(lOurPlayerID == LaunchEntity.ID_NONE || !GetInteractionReady())
        {
            return null;
        }
        
        return GetPlayer(lOurPlayerID);
    }
    
    public List<Structure> GetOurStructures()
    {
        List<Structure> OurStructures = new ArrayList<>();

        for(Structure structure : GetAllStructures())
        {
            if(structure.GetOwnerID() == lOurPlayerID)
            {
                OurStructures.add(structure);
            }
        }
        
        return OurStructures;
    }
    
    public boolean IAmTheOnlyLeader()
    {
        Player ourPlayer = GetOurPlayer();
        
        if(ourPlayer.GetAllianceMemberID() != Alliance.ALLIANCE_ID_UNAFFILIATED && ourPlayer.GetIsAnMP())
        {
            Alliance alliance = GetAlliance(ourPlayer.GetAllianceMemberID());
            
            for(Player player : Players.values())
            {
                if(player.GetAllianceMemberID() == ourPlayer.GetAllianceMemberID())
                {
                    if(player.GetIsAnMP() && player.GetID() != ourPlayer.GetID())
                    {
                        return false;
                    }
                }
            }
            
            return true;
        }
        
        return false;
    }
    
    public boolean PendingDiplomacyItems()
    {
        Player ourPlayer = GetOurPlayer();

        if(ourPlayer != null)
        {
            //Are we an alliance leader?
            if (ourPlayer.GetIsAnMP())
            {
                //Return true if any players in the game are requesting to join our alliance.
                for (Player player : Players.values())
                {
                    if (player.GetAllianceJoiningID() == ourPlayer.GetAllianceMemberID())
                        return true;
                }
                
                //Return true if any other alliances wish to affiliate with ours.
                for(Treaty treaty : Treaties.values())
                {
                    if(treaty instanceof AffiliationRequest)
                    {
                        if(treaty.GetAllianceID2() == ourPlayer.GetAllianceMemberID())
                            return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    public List<Player> GetAffectedPlayers(GeoCoord geoLocation, float fltRange)
    {
        List<Player> Affected = new ArrayList();
        
        for(Player player : Players.values())
        {
            if(geoLocation.DistanceTo(player.GetPosition()) <= fltRange)
            {
                Affected.add(player);
            }
        }

        for(Structure structure : GetAllStructures())
        {
            if(geoLocation.DistanceTo(structure.GetPosition()) <= fltRange)
            {
                Player owner = Players.get(structure.GetOwnerID());

                if(!Affected.contains(owner))
                    Affected.add(owner);
            }
        }
        
        return Affected;
    }
    
    public Allegiance GetAllegiance(Player player)
    {
        return GetAllegiance(GetOurPlayer(), player);
    }
    
    public Allegiance GetAllegiance(Alliance alliance)
    {
        return GetAllegiance(GetOurPlayer(), alliance);
    }
    
    public int GetOurPlayerID() { return lOurPlayerID; }
    
    public boolean GetInPrivacyZone() { return bInPrivacyZone; }
    public List<PrivacyZone> GetPrivacyZones() { return PrivacyZones; }
    
    public void AddPrivacyZone(PrivacyZone privacyZone)
    {
        PrivacyZones.add(privacyZone);
    }
    
    public void ClearPrivacyZones()
    {
        PrivacyZones.clear();
    }
    
    public void RemovePrivacyZone(PrivacyZone zone)
    {
        PrivacyZones.remove(zone);
    }
    
    public int GetLatency()
    {
        return lLatency;
    }
    
    public boolean GetReceivingSnapshot()
    {
        return bReceivingSnapshot;
    }
    
    public boolean GetAuthenticated()
    {
        return bAuthenticated;
    }
    
    public boolean GetInteractionReady()
    {
        return bAuthenticated && Players.size() > 0;
    }
    
    public void Register(String strPlayer, int lAvatarID)
    {
        comms.Register(strPlayer, lAvatarID);
    }
    
    public void UploadAvatar(byte[] cData)
    {
        comms.UploadAvatar(cData);
    }
    
    public void DownloadAvatar(int lAvatarID)
    {
        comms.DownloadAvatar(lAvatarID);
    }
    
    public void DownloadImage(int lAssetID)
    {
        comms.DownloadImage(lAssetID);
    }
    
    public void Respawn()
    {
        comms.Respawn();
    }
    
    public void LaunchMissilePlayer(byte cSlotNo, boolean bTracking, GeoCoord geoTarget, int lTrackingID)
    {
        comms.LaunchMissilePlayer(cSlotNo, bTracking, geoTarget, lTrackingID);
    }
    
    public void LaunchMissile(int lSiteID, byte cSlotNo, boolean bTracking, GeoCoord geoTarget, int lTrackingID)
    {
        comms.LaunchMissile(lSiteID, cSlotNo, bTracking, geoTarget, lTrackingID);
    }
    
    public void LaunchInterceptorPlayer(byte cSlotNo, int lTargetID)
    {
        comms.LaunchInterceptorPlayer(cSlotNo, lTargetID);
    }
    
    public void LaunchInterceptor(int lSiteID, byte cSlotNo, int lTargetID)
    {
        comms.LaunchInterceptor(lSiteID, cSlotNo, lTargetID);
    }
    
    public void PurchaseMissilesPlayer(byte cSlotNumber, byte[] cTypes)
    {
        comms.PurchaseMissilesPlayer(cSlotNumber, cTypes);
    }
    
    public void PurchaseMissiles(int lSiteID, byte cSlotNumber, byte[] cTypes)
    {
        comms.PurchaseMissiles(lSiteID, cSlotNumber, cTypes);
    }
    
    public void PurchaseInterceptorsPlayer(byte cSlotNumber, byte[] cTypes)
    {
        comms.PurchaseInterceptorsPlayer(cSlotNumber, cTypes);
    }
    
    public void PurchaseInterceptors(int lSiteID, byte cSlotNumber, byte[] cTypes)
    {
        comms.PurchaseInterceptors(lSiteID, cSlotNumber, cTypes);
    }
    
    public void SellMissilePlayer(byte cSlotNumber)
    {
        comms.SellMissilePlayer(cSlotNumber);
    }
    
    public void SellMissile(int lSiteID, byte cSlotNumber)
    {
        comms.SellMissile(lSiteID, cSlotNumber);
    }
    
    public void SellInterceptorPlayer(byte cSlotNumber)
    {
        comms.SellInterceptorPlayer(cSlotNumber);
    }
    
    public void SellInterceptor(int lSiteID, byte cSlotNumber)
    {
        comms.SellInterceptor(lSiteID, cSlotNumber);
    }
    
    public void PurchaseMissileSystem()
    {
        comms.PurchaseMissileSystem();
    }

    public void PurchaseMissileSlotUpgradePlayer()
    {
        comms.PurchaseMissileSlotUpgradePlayer();
    }
            
    public void PurchaseMissileSlotUpgrade(int lSiteID)
    {
        comms.PurchaseMissileSlotUpgrade(lSiteID);
    }

    public void PurchaseMissileReloadUpgradePlayer()
    {
        comms.PurchaseMissileReloadUpgradePlayer();
    }
            
    public void PurchaseMissileReloadUpgrade(int lSiteID)
    {
        comms.PurchaseMissileReloadUpgrade(lSiteID);
    }
    
    public void PurchaseSAMSystem()
    {
        comms.PurchaseSAMSystem();
    }

    public void PurchaseSAMSlotUpgradePlayer()
    {
        comms.PurchaseSAMSlotUpgradePlayer();
    }
            
    public void PurchaseSAMSlotUpgrade(int lSiteID)
    {
        comms.PurchaseSAMSlotUpgrade(lSiteID);
    }

    public void PurchaseSAMReloadUpgradePlayer()
    {
        comms.PurchaseSAMReloadUpgradePlayer();
    }
            
    public void PurchaseSAMReloadUpgrade(int lSiteID)
    {
        comms.PurchaseSAMReloadUpgrade(lSiteID);
    }
    
    public void ConstructMissileSite(boolean bNuclear)
    {
        comms.ConstructMissileSite(bNuclear);
    }
    
    public void ConstructSAMSite()
    {
        comms.ConstructSAMSite();
    }
    
    public void ConstructSentryGun()
    {
        comms.ConstructSentryGun();
    }
    
    public void ConstructOreMine()
    {
        comms.ConstructOreMine();
    }
    
    public void SellMissileSite(int lSiteID)
    {
        comms.SellMissileSite(lSiteID);
    }
    
    public void SellSAMSite(int lSiteID)
    {
        comms.SellSAMSite(lSiteID);
    }
    
    public void SellSentryGun(int lSiteID)
    {
        comms.SellSentryGun(lSiteID);
    }
    
    public void SellOreMine(int lSiteID)
    {
        comms.SellOreMine(lSiteID);
    }
    
    public void SellMissileSystem()
    {
        comms.SellMissileSystem();
    }
    
    public void SellSAMSystem()
    {
        comms.SellSAMSystem();
    }
    
    public void SetMissileSiteOnOff(int lSiteID, boolean bOnline)
    {
        comms.SetOnlineOffline(lSiteID, Task.StructureType.MISSILE_SITE, bOnline);
    }
    
    public void SetSAMSiteOnOff(int lSiteID, boolean bOnline)
    {
        comms.SetOnlineOffline(lSiteID, Task.StructureType.SAM_SITE, bOnline);
    }
    
    public void SetSentryGunOnOff(int lSiteID, boolean bOnline)
    {
        comms.SetOnlineOffline(lSiteID, Task.StructureType.SENTRY_GUN, bOnline);
    }
    
    public void SetOreMineOnOff(int lSiteID, boolean bOnline)
    {
        comms.SetOnlineOffline(lSiteID, Task.StructureType.ORE_MINE, bOnline);
    }
    
    public void SetMissileSitesOnOff(List<Integer> SiteIDs, boolean bOnline)
    {
        comms.SetMultipleOnlineOffline(SiteIDs, Task.StructureType.MISSILE_SITE, bOnline);
    }
    
    public void SetSAMSitesOnOff(List<Integer> SiteIDs, boolean bOnline)
    {
        comms.SetMultipleOnlineOffline(SiteIDs, Task.StructureType.SAM_SITE, bOnline);
    }
    
    public void SetSentryGunsOnOff(List<Integer> SiteIDs, boolean bOnline)
    {
        comms.SetMultipleOnlineOffline(SiteIDs, Task.StructureType.SENTRY_GUN, bOnline);
    }
    
    public void SetOreMinesOnOff(List<Integer> SiteIDs, boolean bOnline)
    {
        comms.SetMultipleOnlineOffline(SiteIDs, Task.StructureType.ORE_MINE, bOnline);
    }
    
    public void RepairMissileSite(int lSiteID)
    {
        comms.RepairStructure(lSiteID, Task.StructureType.MISSILE_SITE);
    }
    
    public void RepairSAMSite(int lSiteID)
    {
        comms.RepairStructure(lSiteID, Task.StructureType.SAM_SITE);
    }
    
    public void RepairSentryGun(int lSiteID)
    {
        comms.RepairStructure(lSiteID, Task.StructureType.SENTRY_GUN);
    }
    
    public void RepairOreMine(int lSiteID)
    {
        comms.RepairStructure(lSiteID, Task.StructureType.ORE_MINE);
    }
    
    public void HealPlayer()
    {
        comms.HealPlayer();
    }
    
    public void SetSAMSiteMode(int lSiteID, byte cMode)
    {
        comms.SetSAMSiteMode(lSiteID, cMode);
    }
    
    public void SetSAMSiteModes(List<Integer> SiteIDs, byte cMode)
    {
        comms.SetMultipleSAMSiteModes(SiteIDs, cMode);
    }
    
    public void SetSAMSiteName(int lSiteID, String strName)
    {
        comms.SetStructureName(lSiteID, strName, Task.StructureType.SAM_SITE);
    }
    
    public void SetMissileSiteName(int lSiteID, String strName)
    {
        comms.SetStructureName(lSiteID, strName, Task.StructureType.MISSILE_SITE);
    }
    
    public void SetSentryGunName(int lSiteID, String strName)
    {
        comms.SetStructureName(lSiteID, strName, Task.StructureType.SENTRY_GUN);
    }
    
    public void SetOreMineName(int lSiteID, String strName)
    {
        comms.SetStructureName(lSiteID, strName, Task.StructureType.ORE_MINE);
    }
    
    public void SetPlayerName(String strName)
    {
        comms.SetPlayerName(strName);
    }
    
    public void SetAllianceName(String strName)
    {
        comms.SetAllianceName(strName);
    }
    
    public void SetAllianceDescription(String strName)
    {
        comms.SetAllianceDescription(strName);
    }
    
    public void CloseAccount()
    {
        bClosingAccount = true;
        comms.CloseAccount();
    }
    
    public void SetAvatar(int lAvatarID, boolean bIsAlliance)
    {
        comms.SetAvatar(lAvatarID, bIsAlliance);
    }
    
    public void UpgradeToNuclear(int lMissileSiteID)
    {
        comms.UpgradeToNuclear(lMissileSiteID);
    }
    
    public void CreateAlliance(String strName, String strDescription, int lAvatarID)
    {
        comms.CreateAlliance(strName, strDescription, lAvatarID);
    }
    
    public void JoinAlliance(int lAllianceID)
    {
        comms.JoinAlliance(lAllianceID);
    }
    
    public void LeaveAlliance()
    {
        comms.LeaveAlliance();
    }
    
    public void DeclareWar(int lAllianceID)
    {
        comms.DeclareWar(lAllianceID);
    }
    
    public void OfferAffiliation(int lAllianceID)
    {
        comms.OfferAffiliation(lAllianceID);
    }
    
    public void AcceptAffiliation(int lAllianceID)
    {
        comms.AcceptAffiliation(lAllianceID);
    }
    
    public void RejectAffiliation(int lAllianceID)
    {
        comms.RejectAffiliation(lAllianceID);
    }
    
    public void Promote(int lPromotee)
    {
        comms.Promote(lPromotee);
    }
    
    public void AcceptJoin(int lPromotee)
    {
        comms.AcceptJoin(lPromotee);
    }
    
    public void RejectJoin(int lPromotee)
    {
        comms.RejectJoin(lPromotee);
    }
    
    public void Kick(int lPromotee)
    {
        comms.Kick(lPromotee);
    }
    
    public void Ban(String strReason, int lPlayerID, boolean bPermanent)
    {
        comms.Ban(strReason, lPlayerID, bPermanent);
    }
    
    public void ResetAvatar(int lPlayerID)
    {
        comms.ResetAvatar(lPlayerID);
    }
    
    public void ResetName(int lPlayerID)
    {
        comms.ResetName(lPlayerID);
    }
    
    public void DeviceCheck(boolean bCompleteFailure, boolean bAPIFailure, int lFailureCode, boolean bProfileMatch, boolean bBasicIntegrity)
    {
        comms.DeviceCheck(bCompleteFailure, bAPIFailure, lFailureCode, bProfileMatch, bBasicIntegrity);
    }
    
    /**
     * Attempt to get the stats for a war, via experimental direct-access comms.
     * @param war The war to get stats for.
     * @return True if we think it might work. False if the comms report as broken. The stats come out of a @ref TreatyUpdated callback later.
     */
    public boolean GetWarStats(War war)
    {
        return comms.GetWarStats(war.GetID());
    }

    /**
     * Attempt to get a player's stats, via experimental direct-access comms.
     * @param player The player to get stats for.
     * @return True if we think it might work. False if the comms report as broken. The stats come out of a @ref PlayerStatsUpdated callback later.
     */
    public boolean GetPlayerStats(Player player)
    {
        return comms.GetPlayerStats(player.GetID());
    }

    /**
     * Attempt to get user data, via experimental direct-access comms.
     * @param player The player to get user data for.
     * @return True if we think it might work. False if the comms report as broken. The stats come out of a @ref ReceiveUser callback later.
     */
    public boolean GetUserData(Player player)
    {
        return comms.GetUserData(player.GetID());
    }
    
    //---------------------------------------------------------------------------------------------------------------------------------
    // LaunchGame inherited abstract methods.
    //---------------------------------------------------------------------------------------------------------------------------------
    
    @Override
    protected void MissileExploded(Missile missile)
    {
        //Do nothing. Dealt with by events.
    }

    @Override
    protected void InterceptorLostTarget(Interceptor interceptor)
    {
        //Do nothing. Dealt with by events.
    }

    @Override
    protected void InterceptorReachedTarget(Interceptor interceptor)
    {
        //Do nothing. Dealt with by events.
    }

    @Override
    protected void EntityUpdated(LaunchEntity entity, boolean bOwner)
    {
        //The client isn't "aware" of onwership at this level; that goes on via the server and other layers.
        application.EntityUpdated(entity);
    }

    @Override
    protected void EntityRemoved(LaunchEntity entity, boolean bDontCommunicate)
    {
        application.EntityRemoved(entity);
    }

    @Override
    protected void AllianceUpdated(Alliance alliance, boolean bMajor)
    {
        if(bMajor)
        {
            application.MajorChanges();
        }
        
        //TO DO: Respond to minor changes such as point updates?
    }

    @Override
    protected void AllianceRemoved(Alliance alliance)
    {
        application.MajorChanges();
    }

    @Override
    protected void TreatyUpdated(Treaty treaty)
    {
        application.TreatyUpdated(treaty);
        application.MajorChanges();
    }

    @Override
    protected void TreatyRemoved(Treaty treaty)
    {
        application.MajorChanges();
    }
    
    public List<LaunchEvent> GetEvents()
    {
        //Synchronised and cloned as events are coming in on the comms all the time, which modifies the list.
        synchronized (Events)
        {
            return new ArrayList<>(Events);
        }
    }

    public List<LaunchReport> GetNewReports() { return new ArrayList<>(NewReports.values()); }
    public List<LaunchReport> GetOldReports() { return new ArrayList<>(OldReports.values()); }
    
    public void TransferNewReportsToOld()
    {
        for(LaunchReport report : NewReports.values())
        {
            String strMessage = report.GetMessage();

            if(OldReports.containsKey(strMessage))
            {
                OldReports.get(strMessage).Update(report);
            }
            else
            {
                OldReports.put(strMessage, report);

                //Remove first report if we've breached the limit.
                if(OldReports.size() > Defs.MAX_REPORTS)
                {
                    OldReports.remove(OldReports.entrySet().iterator().next().getKey());
                }
            }
        }

        NewReports.clear();
    }
    
    //---------------------------------------------------------------------------------------------------------------------------------
    // LaunchClientGameInterface methods.
    //---------------------------------------------------------------------------------------------------------------------------------

    @Override
    public void ReceivePlayer(Player player)
    {
        Player statsCopy = Players.get(player.GetID());
        
        if(statsCopy != null && !player.GetHasFullStats())
        {
            player.StatsCopy(statsCopy);
        }
        
        if(bReceivingSnapshot)
        {
            player.SetListener(this);
            NewPlayers.put(player.GetID(), player);
        }
        else
        {
            AddPlayer(player);
        }
    }
    
    @Override
    public void ReceiveMissile(Missile missile)
    {
        if(bReceivingSnapshot)
        {
            missile.SetListener(this);
            NewMissiles.put(missile.GetID(), missile);
        }
        else
        {
            AddMissile(missile);
            EstablishStructureThreats(missile);
        }
    }
    
    @Override
    public void ReceiveInterceptor(Interceptor interceptor)
    {
        if(bReceivingSnapshot)
        {
            interceptor.SetListener(this);
            NewInterceptors.put(interceptor.GetID(), interceptor);
        }
        else
        {
            AddInterceptor(interceptor);
        }
    }
    
    @Override
    public void ReceiveMissileSite(MissileSite missileSite)
    {
        if(bReceivingSnapshot)
        {
            missileSite.SetListener(this);
            NewMissileSites.put(missileSite.GetID(), missileSite);
            NewAllStructures.add(missileSite);
        }
        else
        {
            AddMissileSite(missileSite);
            EstablishStructureThreats(missileSite);
        }
    }
    
    @Override
    public void ReceiveSAMSite(SAMSite samSite)
    {
        if(bReceivingSnapshot)
        {
            samSite.SetListener(this);
            NewSAMSites.put(samSite.GetID(), samSite);
            NewAllStructures.add(samSite);
        }
        else
        {
            AddSAMSite(samSite);
            EstablishStructureThreats(samSite);
        }
    }
    
    @Override
    public void ReceiveOreMine(OreMine oreMine)
    {
        if(bReceivingSnapshot)
        {
            oreMine.SetListener(this);
            NewOreMines.put(oreMine.GetID(), oreMine);
            NewAllStructures.add(oreMine);
        }
        else
        {
            AddOreMine(oreMine);
            EstablishStructureThreats(oreMine);
        }
    }
    
    @Override
    public void ReceiveSentryGun(SentryGun sentryGun)
    {
        if(bReceivingSnapshot)
        {
            sentryGun.SetListener(this);
            NewSentryGuns.put(sentryGun.GetID(), sentryGun);
            NewAllStructures.add(sentryGun);
        }
        else
        {
            AddSentryGun(sentryGun);
            EstablishStructureThreats(sentryGun);
        }
    }
    
    @Override
    public void ReceiveLoot(Loot loot)
    {
        if(bReceivingSnapshot)
        {
            loot.SetListener(this);
            NewLoots.put(loot.GetID(), loot);
        }
        else
        {
            AddLoot(loot);
        }
    }
    
    @Override
    public void ReceiveRadiation(Radiation radiation)
    {
        if(bReceivingSnapshot)
        {
            radiation.SetListener(this);
            NewRadiations.put(radiation.GetID(), radiation);
        }
        else
        {
            AddRadiation(radiation);
        }
    }

    @Override
    public void ReceiveAlliance(Alliance alliance, boolean bMajor)
    {
        if(bReceivingSnapshot)
        {
            alliance.SetListener(this);
            NewAlliances.put(alliance.GetID(), alliance);
        }
        else
        {
            AddAlliance(alliance, bMajor);
        }
    }

    @Override
    public void ReceiveTreaty(Treaty treaty)
    {
        if(bReceivingSnapshot)
        {
            NewTreaties.put(treaty.GetID(), treaty);
        }
        else
        {
            AddTreaty(treaty);
        }
    }

    @Override
    public void ReceiveUser(User user)
    {
        application.ReceiveUser(user);
    }
    
    @Override
    public void RemovePlayer(int lID)
    {
        Player player = Players.get(lID);
        Players.remove(lID);
        application.EntityRemoved(player);
    }
    
    @Override
    public void RemoveMissile(int lID)
    {
        Missile missile = Missiles.get(lID);
        Missiles.remove(lID);
        application.EntityRemoved(missile);
    }
    
    @Override
    public void RemoveInterceptor(int lID)
    {
        Interceptor interceptor = Interceptors.get(lID);
        Interceptors.remove(lID);
        application.EntityRemoved(interceptor);
    }
    
    @Override
    public void RemoveMissileSite(int lID)
    {
        MissileSite missileSite = MissileSites.get(lID);
        MissileSites.remove(lID);
        application.EntityRemoved(missileSite);

        //TO DO: Very expensive operation that should be optimised.
        EstablishAllStructureThreats();
    }
    
    @Override
    public void RemoveSAMSite(int lID)
    {
        SAMSite samSite = SAMSites.get(lID);
        SAMSites.remove(lID);
        application.EntityRemoved(samSite);

        //TO DO: Very expensive operation that should be optimised.
        EstablishAllStructureThreats();
    }
    
    @Override
    public void RemoveSentryGun(int lID)
    {
        SentryGun sentryGun = SentryGuns.get(lID);
        SentryGuns.remove(lID);
        application.EntityRemoved(sentryGun);

        //TO DO: Very expensive operation that should be optimised.
        EstablishAllStructureThreats();
    }
    
    @Override
    public void RemoveOreMine(int lID)
    {
        OreMine oreMine = OreMines.get(lID);
        OreMines.remove(lID);
        application.EntityRemoved(oreMine);

        //TO DO: Very expensive operation that should be optimised.
        EstablishAllStructureThreats();
    }
    
    @Override
    public void RemoveLoot(int lID)
    {
        Loot loot = Loots.get(lID);
        Loots.remove(lID);
        application.EntityRemoved(loot);
    }
    
    @Override
    public void RemoveRadiation(int lID)
    {
        Radiation radiation = Radiations.get(lID);
        Radiations.remove(lID);
        application.EntityRemoved(radiation);
    }

    @Override
    public void RemoveAlliance(int lID)
    {
        Alliance alliance = Alliances.get(lID);
        Alliances.remove(lID);
        
        application.MajorChanges();
    }

    @Override
    public void RemoveWar(int lID)
    {
        Treaty wars = Treaties.get(lID);
        Treaties.remove(lID);
        
        application.MajorChanges();
    }

    @Override
    public boolean PlayerLocationAvailable()
    {
        return application.PlayerLocationAvailable();
    }

    @Override
    public LaunchClientLocation GetPlayerLocation()
    {
        LaunchClientLocation playerLocation = application.GetPlayerLocation();
        GeoCoord geoPlayerLocation = playerLocation.GetGeoCoord();
        
        //Check against privacy zones.
        bInPrivacyZone = false;

        for (PrivacyZone privacyZone : PrivacyZones)
        {
            float fltDistance = geoPlayerLocation.DistanceTo(privacyZone.GetPosition()) * Defs.METRES_PER_KM;
            float fltRadius = privacyZone.GetRadius();

            if (fltDistance < fltRadius)
            {
                bInPrivacyZone = true;
                
                Player ourPlayer = GetOurPlayer();
                GeoCoord playerGamePosition = null;
                boolean bNoPreviousPosition = true;
                
                if(ourPlayer != null)
                {
                    //We have our player. Does it have a valid location?
                    playerGamePosition = ourPlayer.GetPosition();
                    
                    if(playerGamePosition.GetValid())
                    {
                        //Yes.
                        bNoPreviousPosition = false;
                    }
                }
                
                if(bNoPreviousPosition)
                {
                    //Move "a negative amount towards" the privacy zone. The amount is the negative of the distance to the edge of the zone.
                    geoPlayerLocation.Move(geoPlayerLocation.BearingTo(privacyZone.GetPosition()), -(fltRadius - fltDistance) / Defs.METRES_PER_KM);
                    playerLocation.SetLocationBecauseOfPrivacyZone(geoPlayerLocation);
                }
                else
                {
                    //Send our player's last location.
                    playerLocation.SetLocationBecauseOfPrivacyZone(playerGamePosition);
                }

                break;
            }
        }
        
        return playerLocation;
    }

    @Override
    public int GetGameConfigChecksum()
    {
        if(config == null)
        {
            //Return a duff checksum if there's no config.
            return LaunchEntity.ID_NONE;
        }
        
        return config.GetChecksum();
    }

    @Override
    public void SetConfig(Config config)
    {
        this.config = config;
        application.SaveConfig(config);
    }

    @Override
    public void AvatarReceived(int lAvatarID, byte[] cData)
    {
        application.SaveAvatar(lAvatarID, cData);
    }

    @Override
    public void AvatarUploaded(int lAvatarID)
    {
        application.AvatarUploaded(lAvatarID);
    }

    @Override
    public void ImageReceived(int lImageID, byte[] cData)
    {
        application.SaveImage(lImageID, cData);
    }
    
    @Override
    public void Authenticated()
    {
        bAuthenticated = true;
        application.Authenticated();
    }

    @Override
    public void AccountUnregistered()
    {
        application.AccountUnregistered();
    }

    @Override
    public void AccountNameTaken()
    {
        application.AccountNameTaken();
    }

    @Override
    public void SetOurPlayerID(int lPlayerID)
    {
        lOurPlayerID = lPlayerID;
    }

    @Override
    public boolean GetReadyToUpdatePlayer()
    {
        return GetInteractionReady();
    }

    @Override
    public boolean VerifyVersion(short nMajorVersion, short nMinorVersion)
    {
        if(nMajorVersion != Defs.MAJOR_VERSION)
        {
            application.MajorVersionMismatch();
            return false;
        }
        
        if(nMinorVersion > Defs.MINOR_VERSION)
        {
            application.MinorVersionMismatch();
        }
        
        return true;
    }

    @Override
    public void MajorVersionInvalid()
    {
        application.MajorVersionMismatch();
    }

    @Override
    public void SetLatency(int lLatency)
    {
        this.lLatency = lLatency;
    }

    @Override
    public void SnapshotBegin()
    {
        //Create new entity containers.
        NewPlayers = new ConcurrentHashMap<>();
        NewMissiles = new ConcurrentHashMap<>();
        NewInterceptors = new ConcurrentHashMap<>();
        NewMissileSites = new ConcurrentHashMap<>();
        NewSAMSites = new ConcurrentHashMap<>();
        NewOreMines = new ConcurrentHashMap<>();
        NewSentryGuns = new ConcurrentHashMap<>();
        NewLoots = new ConcurrentHashMap<>();
        NewRadiations = new ConcurrentHashMap<>();
        NewAlliances = new ConcurrentHashMap<>();
        NewTreaties = new ConcurrentHashMap<>();
        NewAllStructures = new ArrayList();
        
        bReceivingSnapshot = true;
    }

    @Override
    public void SnapshotFinish()
    {
        //Commit newly populated containers.
        Players = NewPlayers;
        Missiles = NewMissiles;
        Interceptors = NewInterceptors;
        MissileSites = NewMissileSites;
        SAMSites = NewSAMSites;
        SentryGuns = NewSentryGuns;
        OreMines = NewOreMines;
        Loots = NewLoots;
        Radiations = NewRadiations;
        Alliances = NewAlliances;
        Treaties = NewTreaties;
        
        bReceivingSnapshot = false;
        
        EstablishAllStructureThreats();
        
        application.MajorChanges();
    }

    @Override
    public void ShowTaskMessage(Task.TaskMessage message)
    {
        application.ShowTaskMessage(message);
    }

    @Override
    public void DismissTaskMessage()
    {
        application.DismissTaskMessage();
    }

    @Override
    public void EventReceived(LaunchEvent event)
    {
        synchronized (Events)
        {
            Events.addFirst(event);

            //Purge old events.
            while (Events.size() > Defs.MAX_EVENTS)
            {
                Events.removeLast();
            }
        }

        application.NewEvent(event);
    }

    @Override
    public void ReportReceived(LaunchReport report)
    {
        String strMessage = report.GetMessage();
        
        LaunchLog.Log(LaunchLog.LogType.GAME, "Report debug", "Received report: " + strMessage);

        if(NewReports.containsKey(strMessage))
        {
            NewReports.get(strMessage).Update(report);
        }
        else
        {
            NewReports.put(strMessage, report);

            //Remove first report if we've breached the limit.
            if(NewReports.size() > Defs.MAX_REPORTS)
            {
                NewReports.remove(NewReports.entrySet().iterator().next().getKey());
            }
        }

        application.NewReport(report);
    }

    @Override
    public void AccountClosed()
    {
        Suspend();
        seService.shutdown();
        application.Quit();
    }

    @Override
    public boolean ClosingAccount()
    {
        return bClosingAccount;
    }

    @Override
    public void AllianceCreated()
    {
        application.AllianceCreated();
    }

    @Override
    public String GetProcessNames()
    {
        return application.GetProcessNames();
    }

    @Override
    public boolean GetConnectionMobile()
    {
        return application.GetConnectionMobile();
    }

    @Override
    public void DeviceCheckRequested()
    {
        application.DeviceChecksRequested();
    }

    @Override
    public void DisplayGeneralError()
    {
        application.DisplayGeneralError();
    }

    @Override
    public void TempBanned(String strReason, long oDuration)
    {
        dlyUntilCommsAttempts.Set(oDuration);
        comms.Suspend();
        application.TempBanned(strReason, oDuration);
    }

    @Override
    public void PermBanned(String strReason)
    {
        dlyUntilCommsAttempts.Set(Long.MAX_VALUE);
        comms.Suspend();
        application.PermBanned(strReason);
    }
    
    public LaunchClientComms GetComms() { return comms; }
}
