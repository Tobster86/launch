/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.game;

import launch.game.treaties.Treaty;
import launch.game.treaties.War;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import launch.game.entities.*;
import launch.game.systems.MissileSystem;
import launch.game.treaties.Affiliation;
import launch.game.treaties.AffiliationRequest;
import launch.game.types.InterceptorType;
import launch.game.types.MissileType;
import launch.utilities.LaunchLog;
import launch.utilities.LaunchPerf;

/**
 *
 * @author tobster
 */
public abstract class LaunchGame implements LaunchEntityListener
{
    public enum Allegiance
    {
        YOU,
        ALLY,
        AFFILIATE,
        ENEMY,
        NEUTRAL,
        PENDING_TREATY
    }
    
    protected static final int TICK_RATE_COMMS = 20;
    protected static final int TICK_RATE_GAME = 1000;
    
    protected static final String LOG_NAME = "Game";
    
    public static final Random random = new Random();
    
    protected ScheduledExecutorService seService;
    
    protected Config config;

    //Entities.
    protected Map<Integer, Alliance> Alliances = new ConcurrentHashMap<>();
    protected Map<Integer, Treaty> Treaties = new ConcurrentHashMap<>();
    protected Map<Integer, Player> Players = new ConcurrentHashMap<>();
    protected Map<Integer, Missile> Missiles = new ConcurrentHashMap<>();
    protected Map<Integer, Interceptor> Interceptors = new ConcurrentHashMap<>();
    protected Map<Integer, MissileSite> MissileSites = new ConcurrentHashMap<>();
    protected Map<Integer, SAMSite> SAMSites = new ConcurrentHashMap<>();
    protected Map<Integer, SentryGun> SentryGuns = new ConcurrentHashMap<>();
    protected Map<Integer, OreMine> OreMines = new ConcurrentHashMap<>();
    protected Map<Integer, Loot> Loots = new ConcurrentHashMap<>();
    protected Map<Integer, Radiation> Radiations = new ConcurrentHashMap<>();
    
    //Statistics.
    protected int lGameTickStarts = 0;
    protected int lGameTickEnds = 0;
    protected int lCommTickStarts = 0;
    protected int lCommTickEnds = 0;
    
    protected LaunchGame(Config config)
    {
        this.config = config;
    }
    
    public void StartServices()
    {
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, "Starting services...");
        
        EstablishAllStructureThreats();
        
        //Services are created here, called at the end of the child constructors, rather than in this constructor; so the full stack of Launch objects is initialised & ready to be ticked..
        seService = Executors.newScheduledThreadPool(2);
        
        //Comms service.
        seService.scheduleAtFixedRate(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    CommsTick(TICK_RATE_COMMS);
                }
                catch(Exception ex)
                {
                    LaunchLog.Log(LaunchLog.LogType.GAME, "CommsServiceErrors", "Unhandled comms tick error: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        }, 0, TICK_RATE_COMMS, TimeUnit.MILLISECONDS);
        
        //Game service.
        seService.scheduleAtFixedRate(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    GameTick(TICK_RATE_GAME);
                }
                catch(Exception ex)
                {
                    LaunchLog.Log(LaunchLog.LogType.GAME, "GameServiceErrors", "Unhandled game tick error: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        }, 0, TICK_RATE_GAME, TimeUnit.MILLISECONDS);
        
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, "...Started.");
    }
    
    protected abstract void CommsTick(int lMS);
    
    protected void GameTick(int lMS)
    {
        lGameTickStarts++;
        
        //Tick all entities.
        for(Player player : Players.values())
        {
            if(!player.GetAWOL())
            {
                player.Tick(lMS);
            }
        }

        LaunchPerf.Measure(LaunchPerf.Metric.PlayerTick);

        for(Missile missile : Missiles.values())
        {
            missile.Tick(lMS);

            //Missiles are flying until they reach their target or are shot down, after which they are removed from the game.
            if(missile.Flying())
            {
                MissileType type = config.GetMissileType(missile.GetType());

                //Move the missile towards its target. MoveToward() returns true when target is reached.
                GeoCoord geoTarget = GetMissileTarget(missile);

                if(missile.GetPosition().MoveToward(geoTarget, config.GetMissileSpeed(type.GetSpeedIndex()), lMS))
                {
                    //Missile has reached target and exploded.
                    missile.SetPosition(geoTarget); //(Correct explosion location).
                    MissileExploded(missile);
                    missile.Destroy();
                    Missiles.remove(missile.GetID());
                    EntityRemoved(missile, true);
                }
            }
            else
            {
                Missiles.remove(missile.GetID());
            }
        }

        LaunchPerf.Measure(LaunchPerf.Metric.MissileTick);

        for(Interceptor interceptor : Interceptors.values())
        {
            interceptor.Tick(lMS);

            Missile targetMissile = Missiles.get(interceptor.GetTargetID());
            InterceptorType type = config.GetInterceptorType(interceptor.GetType());

            //Interceptors are flying until they reach or lose their target, after which they are removed from the game.
            if(targetMissile == null)
            {
                //Target lost.
                InterceptorLostTarget(interceptor);
                Interceptors.remove(interceptor.GetID());
                EntityRemoved(interceptor, true);
            }
            else if(!targetMissile.Flying())
            {
                //Target lost.
                InterceptorLostTarget(interceptor);
                Interceptors.remove(interceptor.GetID());
                EntityRemoved(interceptor, true);
            }
            else
            {
                MissileType missileType = config.GetMissileType(targetMissile.GetType());

                if(interceptor.GetPosition().MoveToIntercept(config.GetInterceptorSpeed(type.GetSpeedIndex()), targetMissile.GetPosition(), config.GetMissileSpeed(missileType.GetSpeedIndex()), GetMissileTarget(targetMissile), lMS))
                {
                    //Interceptor has caught up with target missile.
                    InterceptorReachedTarget(interceptor);
                    Interceptors.remove(interceptor.GetID());
                    EntityRemoved(interceptor, true);
                }
            }
        }

        LaunchPerf.Measure(LaunchPerf.Metric.InterceptorTick);
        
        for(MissileSite missileSite : MissileSites.values())
        {
            missileSite.Tick(lMS);
            
            if(missileSite.Destroyed())
            {
                MissileSites.remove(missileSite.GetID());
                EntityRemoved(missileSite, true);
            }
        }
        
        for(SAMSite samSite : SAMSites.values())
        {
            samSite.Tick(lMS);
            
            if(samSite.Destroyed())
            {
                SAMSites.remove(samSite.GetID());
                EntityRemoved(samSite, true);
            }
        }
        
        for(SentryGun sentryGun : SentryGuns.values())
        {
            sentryGun.Tick(lMS);
            
            if(sentryGun.Destroyed())
            {
                SentryGuns.remove(sentryGun.GetID());
                EntityRemoved(sentryGun, true);
            }
        }
        
        for(OreMine oreMine : OreMines.values())
        {
            oreMine.Tick(lMS);
            
            if(oreMine.Destroyed())
            {
                OreMines.remove(oreMine.GetID());
                EntityRemoved(oreMine, true);
            }
        }

        for(Loot loot : Loots.values())
        {
            loot.Tick(lMS);

            if(loot.Expired() || loot.Collected())
            {
                Loots.remove(loot.GetID());
                EntityRemoved(loot, true);
            }
        }

        for(Radiation radiation : Radiations.values())
        {
            radiation.Tick(lMS);

            if(radiation.GetExpired())
            {
                Radiations.remove(radiation.GetID());
                EntityRemoved(radiation, true);
            }
        }
    }
    
    public List<Damagable> GetNearbyDamagables(GeoCoord geoPosition, float fltDistance)
    {
        //Return all damagable entities within the specified distance of the specified position.
        List<Damagable> Result = new ArrayList<>();
        
        for(Player player : Players.values())
        {
            if(player.GetPosition().DistanceTo(geoPosition) < fltDistance)
            {
                Result.add(player);
            }
        }
        
        for(Structure structure : GetNearbyStructures(geoPosition, fltDistance))
        {
            Result.add(structure);
        }
        
        return Result;
    }
    
    /**
     * Return a list of nearby structures for the purpose of player construction, considering the attack range of enemy sentries.
     * @param player The player intending to construct something.
     * @return 
     */
    public List<Structure> GetNearbyStructures(Player player)
    {
        List<Structure> Result = new ArrayList<>();
        GeoCoord geoPosition = player.GetPosition();
        
        for(Structure structure : GetAllStructures())
        {
            if(structure.GetPosition().DistanceTo(geoPosition) < config.GetStructureSeparation())
            {
                Result.add(structure);
            }
            else if(structure instanceof SentryGun)
            {
                SentryGun sentry = (SentryGun)structure;
                
                switch(GetAllegiance(player, sentry))
                {
                    case ENEMY:
                    case NEUTRAL:
                    {
                        if(structure.GetPosition().DistanceTo(geoPosition) < config.GetSentryGunRange())
                        {
                            Result.add(structure);
                        }
                    }
                    break;
                }
            }
        }
        
        return Result;
    }
    
    /**
     * Return a list of nearby structures for general cases.
     * @param geoPosition Position
     * @param fltDistance Distance from this position to include structures within.
     * @return 
     */
    public List<Structure> GetNearbyStructures(GeoCoord geoPosition, float fltDistance)
    {
        List<Structure> Result = new ArrayList<>();
        
        for(Structure structure : GetAllStructures())
        {
            if(structure.GetPosition().DistanceTo(geoPosition) < fltDistance)
            {
                Result.add(structure);
            }
        }
        
        return Result;
    }
    
    /** Get a list of sufficient nearby ore mines to a location such that they could compete, and reduce each other's ore values.
     * @param geoLocation The proposed location.
     * @return A list of potentially competing ore mines. */
    public final List<OreMine> GetNearbyCompetingOreMines(GeoCoord geoLocation)
    {
        List<OreMine> Result = new ArrayList<>();
        
        for(OreMine oreMine : OreMines.values())
        {
            if(oreMine.GetPosition().DistanceTo(geoLocation) < config.GetOreMineDiameter())
            {
                Result.add(oreMine);
            }
        }
        
        return Result;
    }
    
    /** Get a list of sufficient nearby ore mines to another ore mine such that they could compete, and reduce each other's ore values.
     * @param oreMine The ore mine to check for competition against.
     * @return A list of potentially competing ore mines. */
    public final List<OreMine> GetNearbyCompetingOreMines(OreMine oreMine)
    {
        List<OreMine> Result = new ArrayList<>();
        
        for(OreMine otherOreMine : OreMines.values())
        {
            if(otherOreMine != oreMine)
            {
                if(oreMine.GetPosition().DistanceTo(otherOreMine.GetPosition()) < config.GetOreMineDiameter())
                {
                    Result.add(otherOreMine);
                }
            }
        }
        
        return Result;
    }
    
    /** Get maximum ore mine discovered ore value, considering nearby competing ore mines.
     * @param oreMine The ore mine to query.
     * @return The maximum value of ore that will be discovered by this ore mine. */
    public int GetMaxPotentialOreMineReturn(OreMine oreMine)
    {
        float fltCompetitionMultiplier = 1.0f;
        
        for(OreMine otherOreMine : OreMines.values())
        {
            if(oreMine != otherOreMine)
            {
                if(otherOreMine.GetOnline())
                {
                    float fltDistance = oreMine.GetPosition().DistanceTo(otherOreMine.GetPosition());

                    if(fltDistance < config.GetOreMineRadius())
                    {
                        //Reduced by a half.
                        fltCompetitionMultiplier *= 0.5f;
                    }
                    else if(fltDistance < config.GetOreMineDiameter())
                    {
                        //Reduced by a proportion of radius overlaps.
                        fltCompetitionMultiplier *= (fltDistance / config.GetOreMineDiameter());
                    }
                }
            }
        }
        
        return (int)(((float)config.GetMaxOreValue() * fltCompetitionMultiplier) + 0.5f);
    }
    
    protected final void UpdateTrackingMissileThreats(int lPlayerID)
    {
        for(Missile missile : Missiles.values())
        {
            if(missile.GetTracking() && missile.GetTargetID() == lPlayerID)
            {
                EstablishStructureThreats(missile);
            }
        }
    }
    
    protected final void EstablishStructureThreats(Missile missile)
    {
        MissileType type = config.GetMissileType(missile.GetType());
        GeoCoord geoTarget = GetMissileTarget(missile);
        missile.ClearStructureThreatenedPlayers();
        float fltThreatRadius = config.GetBlastRadius(type);
        
        if(type.GetNuclear())
            fltThreatRadius *= config.GetEMPRadiusMultiplier();
        
        for(Structure structure : GetAllStructures())
        {
            if(!missile.ThreatensPlayersStructures(structure.GetOwnerID()))
            {
                if(!structure.GetRespawnProtected() && !structure.Destroyed())
                {
                    if(structure.GetPosition().DistanceTo(geoTarget) <= fltThreatRadius)
                    {
                        missile.AddStructureThreatenedPlayer(structure.GetOwnerID());
                    }
                }
            }
        }
    }
    
    protected final void EstablishStructureThreats(Structure structure)
    {
        for(Missile missile : Missiles.values())
        {
            if(!missile.ThreatensPlayersStructures(structure.GetOwnerID()))
            {
                MissileType type = config.GetMissileType(missile.GetType());
                GeoCoord geoTarget = GetMissileTarget(missile);
                float fltThreatRadius = config.GetBlastRadius(type);

                if(type.GetNuclear())
                    fltThreatRadius *= config.GetEMPRadiusMultiplier();

                if(structure.GetPosition().DistanceTo(geoTarget) <= fltThreatRadius)
                {
                    missile.AddStructureThreatenedPlayer(structure.GetOwnerID());
                }
            }
        }
    }
    
    protected final void EstablishAllStructureThreats()
    {
        for(Missile missile : Missiles.values())
        {
            EstablishStructureThreats(missile);
        }
    }

    /**
     * Returns true if the specified missile threatens the player. Uses cached structure threats for optimisation. It's necessary to call EstablishStructureThreats() as appropriate for this to be up to date.
     * @param missile The missile to query if it is a threat.
     * @param player The player to query if they are threatened by the missile.
     * @param geoTarget The location that the missile is targetting. This must come as a separate parameter as it can vary by whether the missile is tracking a player or not.
     * @param type The missile's type. NOTE: This parameter has already been derived from config by the caller, so passing it instead of deriving it again improves performance.
     * @return A boolean value indicating whether the specified missile threatens the specified player.
     */
    public boolean ThreatensPlayerOptimised(Missile missile, Player player, GeoCoord geoTarget, MissileType type)
    {
        if(missile.ThreatensPlayersStructures(player.GetID()))
            return true;

        //Tracking the player?
        if(missile.GetTracking())
        {
            if(missile.GetTargetID() == player.GetID())
            {
                return true;
            }
        }
        
        //Player within blast radius?
        if((!player.GetRespawnProtected()) || type.GetNuclear())
        {
            if(player.GetPosition().DistanceTo(geoTarget) <= config.GetBlastRadius(type))
            {
                return true;
            }
        }
            
        return false;
    }

    /**
     * Returns true if the specified missile type threatens friendlies (the player or allies) at the specified location. NOT OPTIMISED! Use ThreatensPlayerOptimised() for per-tick status!
     * @param lPlayerID Player to query if they or their allies would be threatened.
     * @param geoTarget The target of the missile.
     * @param type The type of the missile.
     * @param bConsiderEMP Consider EMP from nukes to be a threat.
     * @param bConsiderRespawnProtection Consider respawn protected stuff not to be threatened.
     * @return Boolean value indicating whether a threat would exist.
     */
    public boolean ThreatensFriendlies(int lPlayerID, GeoCoord geoTarget, MissileType type, boolean bConsiderEMP, boolean bConsiderRespawnProtection)
    {
        Player player = GetPlayer(lPlayerID);
        
        if(player.GetAllianceMemberID() == Alliance.ALLIANCE_ID_UNAFFILIATED)
        {
            //Player is not in an alliance. Just see if it threatens them.
            return ThreatensPlayer(lPlayerID, geoTarget, type, bConsiderEMP, bConsiderRespawnProtection);
        }
        else
        {
            //Player is in an alliance. See if this threatens any of their allies or affiliates.
            for(Player otherPlayer : Players.values())
            {
                switch(GetAllegiance(player, otherPlayer))
                {
                    case AFFILIATE:
                    case ALLY:
                    case YOU:
                    case PENDING_TREATY:
                    {
                        if(ThreatensPlayer(otherPlayer.GetID(), geoTarget, type, bConsiderEMP, bConsiderRespawnProtection))
                        {
                            return true;
                        }
                    }
                    break;
                }
            }
        }
        
        return false;
    }
    
    /** Check if a missile targeted at the specified location would threaten a player or their assets.
     * @param lPlayerID The ID of the player to check.
     * @param geoTarget The target coordinates.
     * @param type The missile type ID.
     * @param bConsiderEMP A boolean indicating whether the EMP radius of nukes should be considered as "threatening" in this context.
     * @param bConsiderRespawnProtection A boolean indicating whether respawn protected players or assets should be ignored.
     * @return 
     */
    public boolean ThreatensPlayer(int lPlayerID, GeoCoord geoTarget, MissileType type, boolean bConsiderEMP, boolean bConsiderRespawnProtection)
    {
        float fltThreatRadius = config.GetBlastRadius(type);
        
        if(type.GetNuclear() && bConsiderEMP)
            fltThreatRadius *= config.GetEMPRadiusMultiplier();
        
        Player player = GetPlayer(lPlayerID);

        //Return true if whatever this is will threaten the identified player or their assets.
        if(!player.GetRespawnProtected() || !bConsiderRespawnProtection || type.GetNuclear())
        {
            if(player.GetPosition().DistanceTo(geoTarget) <= fltThreatRadius)
            {
                return true;
            }
        }
        
        for(Structure structure : GetAllStructures())
        {
            if(structure.GetOwnerID() == lPlayerID)
            {
                if(!structure.GetRespawnProtected() || !bConsiderRespawnProtection)
                {
                    if(structure.GetPosition().DistanceTo(geoTarget) <= fltThreatRadius)
                    {
                        return true;
                    }
                }
            }
        }
            
        return false;
    }
    
    /**
     * Indicates that the specified player is "in battle", for the purpose of checking if it's okay for them to join/leave an alliance.
     * @param player Player to query.
     * @return True if the player is attacking or under attack. False if they're idle.
     */
    public boolean InBattle(Player player)
    {
        for(Missile missile : Missiles.values())
        {
            //If the player owns any in-flight missiles, they are in battle.
            if(missile.GetOwnerID() == player.GetID())
                return true;

            //If any missiles threaten the player, they are in battle.
            if(ThreatensPlayer(player.GetID(), GetMissileTarget(missile), config.GetMissileType(missile.GetType()), false, false))
                return true;
        }
        
        return false;
    }
    
    public int GetMissileSlotUpgradeCost(MissileSystem missileSystem, byte cBaseSlotCount)
    {
        return (((missileSystem.GetSlotCount() - cBaseSlotCount) / config.GetMissileUpgradeCount()) + 1) * config.GetMissileUpgradeBaseCost();
    }
    
    public int GetMissileSlotSaleValue(MissileSystem missileSystem, byte cBaseSlotCount)
    {
        return ((missileSystem.GetSlotCount() - cBaseSlotCount) / config.GetMissileUpgradeCount()) * config.GetMissileUpgradeBaseCost();
    }
    
    public int GetReloadUpgradeCost(MissileSystem system)
    {
        if(system.GetReloadTime() >= config.GetReloadTimeBase())
        {
            return config.GetReloadStage1Cost();
        }
        else if(system.GetReloadTime() >= config.GetReloadTimeStage1())
        {
            return config.GetReloadStage2Cost();
        }
        else if(system.GetReloadTime() >= config.GetReloadTimeStage2())
        {
            return config.GetReloadStage3Cost();
        }
        
        return Defs.UPGRADE_COST_MAXED;
    }
    
    public int GetReloadUpgradeSaleValue(MissileSystem system)
    {
        if(system.GetReloadTime() >= config.GetReloadTimeBase())
        {
            return 0;
        }
        else if(system.GetReloadTime() >= config.GetReloadTimeStage1())
        {
            return config.GetReloadStage1Cost();
        }
        else if(system.GetReloadTime() >= config.GetReloadTimeStage2())
        {
            return config.GetReloadStage2Cost();
        }
        
        return config.GetReloadStage3Cost();
    }

    public int GetReloadUpgradeTime(MissileSystem system)
    {
        if(system.GetReloadTime() >= config.GetReloadTimeBase())
        {
            return config.GetReloadTimeStage1();
        }
        else if(system.GetReloadTime() >= config.GetReloadTimeStage1())
        {
            return config.GetReloadTimeStage2();
        }
        
        return config.GetReloadTimeStage3();
    }
    
    private int GetStructureValue(Structure structure)
    {
        int lValue = 0;
        
        MissileSystem missileSystem = null;
        
        if(structure instanceof MissileSite)
        {
            MissileSite missileSite = (MissileSite)structure;
            lValue = missileSite.CanTakeNukes() ? config.GetNukeCMSStructureCost() : config.GetCMSStructureCost();
            missileSystem = missileSite.GetMissileSystem();
            lValue += GetMissileSlotSaleValue(missileSystem, config.GetInitialMissileSlots());
            lValue += GetReloadUpgradeSaleValue(missileSystem);
        }
        else if(structure instanceof SAMSite)
        {
            lValue = config.GetSAMStructureCost();
            missileSystem = ((SAMSite)structure).GetInterceptorSystem();
            lValue += GetMissileSlotSaleValue(missileSystem, config.GetInitialInterceptorSlots());
            lValue += GetReloadUpgradeSaleValue(missileSystem);
        }
        else if(structure instanceof SentryGun)
        {
            lValue = config.GetSentryGunStructureCost();
        }
        else if(structure instanceof OreMine)
        {
            lValue = config.GetOreMineStructureCost();
        }
        else
        {
            throw new RuntimeException("Structure value queried for unknown structure.");
        }
        
        if(missileSystem != null)
        {
            for(byte c = 0; c < missileSystem.GetSlotCount(); c++)
            {
                if(missileSystem.GetSlotHasMissile(c))
                {
                    if(structure instanceof MissileSite)
                        lValue += config.GetMissileCost(config.GetMissileType(missileSystem.GetSlotMissileType(c)));
                    else if(structure instanceof SAMSite)
                        lValue += config.GetInterceptorCost(config.GetInterceptorType(missileSystem.GetSlotMissileType(c)));
                }
            }
        }
        
        return lValue;
    }
    
    private int GetSystemValue(MissileSystem system, boolean bMissiles)
    {
        int lValue = bMissiles? config.GetCMSSystemCost() : config.GetSAMSystemCost();
        lValue += GetMissileSlotSaleValue(system, bMissiles ? config.GetInitialMissileSlots() : config.GetInitialInterceptorSlots());
        lValue += GetReloadUpgradeSaleValue(system);
        
        for(byte c = 0; c < system.GetSlotCount(); c++)
        {
            if(system.GetSlotHasMissile(c))
            {
                lValue += bMissiles? config.GetMissileCost(config.GetMissileType(system.GetSlotMissileType(c))) : config.GetInterceptorCost(config.GetInterceptorType(system.GetSlotMissileType(c)));
            }
        }
        
        return lValue;
    }
    
    public int GetSaleValue(int lGenericValue)
    {
        return (int)(((float)lGenericValue * config.GetResaleValue()) + 0.5f);
    }
    
    public int GetSaleValue(Structure structure)
    {
        return GetSaleValue(GetStructureValue(structure));
    }
    
    public int GetSaleValue(MissileSystem system, boolean bIsMissiles)
    {
        return GetSaleValue(GetSystemValue(system, bIsMissiles));
    }
    
    public int GetRepairCost(Structure structure)
    {
        return config.GetStructureRepairCost() * structure.GetHPDeficit();
    }
    
    public int GetHealCost(Player player)
    {
        return config.GetPlayerRepairCost() * player.GetHPDeficit();
    }
    
    public int GetTimeToTarget(Missile missile)
    {
        MissileType type = config.GetMissileType(missile.GetType());
        GeoCoord geoTarget = GetMissileTarget(missile);
        return GetTimeToTarget(missile.GetPosition(), geoTarget, config.GetMissileSpeed(type.GetSpeedIndex()));
    }
    
    public int GetTimeToTarget(GeoCoord geoFrom, GeoCoord geoTo, float fltSpeed)
    {
        return (int) ((geoFrom.DistanceTo(geoTo) / fltSpeed) * Defs.MS_PER_HOUR_FLT);
    }
    
    public boolean GetInterceptorTooSlow(byte cInterceptorType, byte cMissileType)
    {
        return config.GetInterceptorSpeed(config.GetInterceptorType(cInterceptorType).GetSpeedIndex()) <= config.GetMissileSpeed(config.GetMissileType(cMissileType).GetSpeedIndex());
    }
    
    public boolean GetInterceptorIsFast(InterceptorType interceptorType)
    {
        float fltInterceptorSpeed = config.GetInterceptorSpeed(interceptorType.GetSpeedIndex());
        
        //"Fast" means that it can intercept any missile.
        for(MissileType missileType : config.GetMissileTypes())
        {
            if(config.GetMissileSpeed(missileType.GetSpeedIndex()) > fltInterceptorSpeed)
                return false;
        }
        
        return true;
    }
    
    public boolean GetMissileIsFast(MissileType missileType)
    {
        float fltMissileSpeed = config.GetMissileSpeed(missileType.GetSpeedIndex());
        
        //"Fast" means that it can outrun some interceptor types.
        for(InterceptorType interceptorType : config.GetInterceptorTypes())
        {
            if(config.GetInterceptorSpeed(interceptorType.GetSpeedIndex()) < fltMissileSpeed)
                return true;
        }
        
        return false;
    }
    
    public GeoCoord GetMissileTarget(Missile missile)
    {
        if(missile.GetTracking())
        {
            Player target = Players.get(missile.GetTargetID());
            if(target != null)
            {
                return target.GetPosition();
            }
            else
            {
                //Self destruct the missile if it doesn't know where it's going.
                missile.SelfDestruct();
                return missile.GetPosition();
            }
        }
        
        return missile.GetTarget();
    }
    
    public boolean GetRadioactive(LaunchEntity entity, boolean bConsiderRespawnProtection)
    {
        for(Radiation radiation : Radiations.values())
        {
            if(bConsiderRespawnProtection)
            {
                if(entity instanceof Player)
                {
                    if(((Player)entity).GetRespawnProtected())
                        return false;
                }
            }

            if(entity.GetPosition().DistanceTo(radiation.GetPosition()) <= radiation.GetRadius())
            {
                return true;
            }
        }
        
        return false;
    }

    public boolean GetPlayerOnline(Player player)
    {
        return System.currentTimeMillis() - player.GetLastSeen() <= Defs.PLAYER_ONLINE_TIME;
    }
    
    public long GetEndOfWeekTime()
    {
        Calendar date = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        
        int lDaysDifference = Calendar.MONDAY - date.get(Calendar.DAY_OF_WEEK);
        
        if (lDaysDifference <= 0)
            lDaysDifference += 7;
        
        date.add(Calendar.DAY_OF_MONTH, lDaysDifference);
        
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        
        return date.getTimeInMillis();
    }
    
    /**
     * Return The current relationship between a pair of alliances.
     * @param lAlliance1
     * @param lAlliance2
     * @return The Allegiance type corresponding to this relationship.
     */
    public Allegiance GetAllianceRelationship(int lAlliance1, int lAlliance2)
    {
        for(Treaty treaty : Treaties.values())
        {
            if(treaty.AreParties(lAlliance1, lAlliance2))
            {
                switch(treaty.GetType())
                {
                    case AFFILIATION: return Allegiance.AFFILIATE;
                    case WAR: return Allegiance.ENEMY;
                    case AFFILIATION_REQUEST: return Allegiance.PENDING_TREATY;
                }
            }
        }
        
        return Allegiance.NEUTRAL;
    }
    
    public boolean AffiliationOffered(int lAllianceBy, int lAllianceTo)
    {
        for(Treaty treaty : Treaties.values())
        {
            if(treaty instanceof AffiliationRequest)
            {
                if(treaty.GetAllianceID1() == lAllianceBy && treaty.GetAllianceID2() == lAllianceTo)
                    return true;
            }
        }
        
        return false;
    }
    
    /**
     * A war can be declared between the stated alliances.
     * @param lAllianceDeclarer
     * @param lAllianceDeclaree
     * @return Whether a war declaration is possible.
     */
    public boolean CanDeclareWar(int lAllianceDeclarer, int lAllianceDeclaree)
    {
        switch(GetAllianceRelationship(lAllianceDeclarer, lAllianceDeclaree))
        {
            case NEUTRAL: return true; //Obviously you can declare war.
            
            case PENDING_TREATY:
            {
                //You can only declare war if you're not offering the treaty.
                if(AffiliationOffered(lAllianceDeclarer, lAllianceDeclaree))
                    return false;
            }
        }
        
        //In all other cases, no.
        return false;
    }
    
    /**
     * Whether an alliance can offer affiliation to another alliance.
     * @param lAllianceDeclarer
     * @param lAllianceDeclaree
     * @return Whether affiliation can be offered.
     */
    public boolean CanProposeAffiliation(int lAllianceDeclarer, int lAllianceDeclaree)
    {
        //This is only doable if the alliances are neutral. In all other cases (including affiliation already offered), it's not.
        switch(GetAllianceRelationship(lAllianceDeclarer, lAllianceDeclaree))
        {
            case NEUTRAL: return true;
        }
        
        return false;
    }
    
    /**
     * Get the owner of an entity. Can be null.
     * @param entity
     * @return The owner, if owned, by an existent player.
     */
    public Player GetOwner(LaunchEntity entity)
    {
        Player player = null;
            
        if(entity instanceof Player)
        {
            player = (Player)entity;
        }
        else if(entity instanceof Missile)
        {
            player =  GetPlayer(((Missile)entity).GetOwnerID());
        }
        else if(entity instanceof Interceptor)
        {
            player = GetPlayer(((Interceptor)entity).GetOwnerID());
        }
        else if(entity instanceof Structure)
        {
            player = GetPlayer(((Structure)entity).GetOwnerID());
        }
        
        return player;
    }
    
    /**
     * Get the allegiance between two known players.
     * @param player1
     * @param player2
     * @return The allegiance between the players.
     */
    public Allegiance GetAllegiance(Player player1, Player player2)
    {
        if(player1.GetID() == player2.GetID())
        {
            //It's you, you wally.
            return Allegiance.YOU;
        }
        else if(player1.GetAllianceMemberID() != Alliance.ALLIANCE_ID_UNAFFILIATED)
        {
            if(player2.GetAllianceMemberID() != Alliance.ALLIANCE_ID_UNAFFILIATED)
            {
                //Are both players in the same alliance?
                if(player1.GetAllianceMemberID() == player2.GetAllianceMemberID())
                    return Allegiance.ALLY;
                
                //Otherwise, return the relationship between the alliances.
                return GetAllianceRelationship(player1.GetAllianceMemberID(), player2.GetAllianceMemberID());
            }
            else if(player2.GetAllianceJoiningID() != Alliance.ALLIANCE_ID_UNAFFILIATED)
            {
                //Check if player2 is requesting to join player1's alliance, in which case they're pending a treaty.
                if(player2.GetAllianceJoiningID() == player1.GetAllianceMemberID())
                    return Allegiance.PENDING_TREATY;
            }
        }
        else if(player1.GetAllianceJoiningID() != Alliance.ALLIANCE_ID_UNAFFILIATED)
        {
            if(player2.GetAllianceMemberID() != Alliance.ALLIANCE_ID_UNAFFILIATED)
            {
                //Player 1 is requesting to join an alliance. See if it's player2's existing alliance.
                if(player2.GetAllianceJoiningID() == player1.GetAllianceMemberID())
                    return Allegiance.PENDING_TREATY;
            }
            //If they're both waiting to join the same alliance, they are still neutral to each other.
        }
        
        //In all other cases, the players are neutral.
        return Allegiance.NEUTRAL;
    }
    
    /**
     * Get the allegiance between two entities.
     * @param entity1
     * @param entity2
     * @return The allegiance between the entities.
     */
    public Allegiance GetAllegiance(LaunchEntity entity1, LaunchEntity entity2)
    {
        Player player1 = GetOwner(entity1);
        Player player2 = GetOwner(entity2);

        if(player1 != null && player2 != null)
        {
            return GetAllegiance(player1, player2);
        }
        
        return Allegiance.NEUTRAL;
    }
    
    public Allegiance GetAllegiance(Player player, Alliance alliance)
    {
        if(player.GetAllianceMemberID() == Alliance.ALLIANCE_ID_UNAFFILIATED)
        {
            //All alliances that the player isn't joining are neutral.
            if(player.GetAllianceJoiningID() == alliance.GetID())
                return Allegiance.PENDING_TREATY;
            else
                return Allegiance.NEUTRAL;
        }
        else if(player.GetAllianceMemberID() == alliance.GetID())
        {
            //Is the player in this alliance?
            return Allegiance.YOU;
        }
        
        for(Treaty treaty : Treaties.values())
        {
            if(treaty.AreParties(player.GetAllianceMemberID(), alliance.GetID()))
            {
                if(treaty instanceof War)
                    return Allegiance.ENEMY;
                if(treaty instanceof Affiliation)
                    return Allegiance.AFFILIATE;
                if(treaty instanceof AffiliationRequest)
                    return Allegiance.PENDING_TREATY;
            }
        }
        
        //In all other cases, it's neutral.
        return Allegiance.NEUTRAL;
    }
    
    public boolean WouldBeFriendlyFire(Player player, Player otherPlayer)
    {
        switch(GetAllegiance(player, otherPlayer))
        {
            case YOU:
            case AFFILIATE:
            case ALLY:
            case PENDING_TREATY:
                return true;
        }
        
        return false;
    }
    
    public int GetAllianceMemberCount(Alliance alliance)
    {
        int lResult = 0;
        
        for(Player player : Players.values())
        {
            if(player.GetAllianceMemberID() == alliance.GetID() && !player.GetAWOL())
                lResult++;
        }
        
        return lResult;
    }
    
    public boolean GetAllianceIsLeaderless(Alliance alliance)
    {
        int lLeaders = 0;
        
        for(Player player : Players.values())
        {
            if(player.GetAllianceMemberID() == alliance.GetID() && player.GetIsAnMP() && !player.GetAWOL())
                lLeaders++;
        }
        
        return lLeaders == 0;
    }
    
    /**
     * Return all enemies of an alliance (those the alliance is engaged in wars with).
     * @param alliance The alliance to query.
     * @return A list of alliances this alliance is at war with.
     */
    public List<Alliance> GetEnemies(Alliance alliance)
    {
        List<Alliance> Result = new ArrayList();
        
        for(Treaty treaty : Treaties.values())
        {
            if(treaty.GetType() == Treaty.Type.WAR)
            {
                if(treaty.IsAParty(alliance.GetID()))
                    Result.add(Alliances.get(treaty.OtherParty(alliance.GetID())));
            }
        }
        
        return Result;
    }
    
    /**
     * Return all affiliates of an alliance.
     * @param alliance The alliance to query.
     * @return A list of alliances this alliance is affiliated with.
     */
    public List<Alliance> GetAffiliates(Alliance alliance)
    {
        List<Alliance> Result = new ArrayList();
        
        for(Treaty treaty : Treaties.values())
        {
            if(treaty.GetType() == Treaty.Type.AFFILIATION)
            {
                if(treaty.IsAParty(alliance.GetID()))
                    Result.add(Alliances.get(treaty.OtherParty(alliance.GetID())));
            }
        }
        
        return Result;
    }
    
    /**
     * Get the war involving both specified alliances. Return NULL if there isn't one.
     * @param lAllianceID1
     * @param lAllianceID2
     * @return The war the alliances are having with eachother, otherwise NULL.
     */
    public War GetWar(int lAllianceID1, int lAllianceID2)
    {
        for(Treaty treaty : Treaties.values())
        {
            if(treaty instanceof War)
            {
                War war = (War)treaty;

                if(war.AreParties(lAllianceID1, lAllianceID2))
                    return war;
            }
        }
        
        return null;
    }
    
    /**
     * Return all members of an alliance.
     * @param alliance The alliance to query.
     * @return A list of players who are members of this alliance.
     */
    public List<Player> GetMembers(Alliance alliance)
    {
        List<Player> Result = new ArrayList();
        
        for(Player player : Players.values())
        {
            if(player.GetAllianceMemberID() == alliance.GetID())
                Result.add(player);
        }
        
        return Result;
    }
    
    public int GetPlayerOffenseValue(Player player)
    {
        int lValue = 0;
        
        for(MissileSite missileSite : MissileSites.values())
        {
            if(missileSite.GetOwnerID() == player.GetID())
            {
                lValue += GetStructureValue(missileSite);
            }
        }

        for(Missile missile : Missiles.values())
        {
            if(missile.GetOwnerID() == player.GetID())
            {
                lValue += config.GetMissileCost(config.GetMissileType(missile.GetType()));
            }
        }
        
        if(player.GetHasCruiseMissileSystem())
        {
            lValue += GetSystemValue(player.GetMissileSystem(), true);
        }
        
        return lValue;
    }
    
    public int GetPlayerDefenseValue(Player player)
    {
        int lValue = 0;

        for(SAMSite samSite : SAMSites.values())
        {
            if(samSite.GetOwnerID() == player.GetID())
            {
                lValue += GetStructureValue(samSite);
            }
        }

        for(Interceptor interceptor : Interceptors.values())
        {
            if(interceptor.GetOwnerID() == player.GetID())
            {
                lValue += config.GetInterceptorCost(config.GetInterceptorType(interceptor.GetType()));
            }
        }

        for(SentryGun sentrygun : SentryGuns.values())
        {
            if(sentrygun.GetOwnerID() == player.GetID())
            {
                lValue += GetStructureValue(sentrygun);
            }
        }

        if(player.GetHasAirDefenceSystem())
        {
            lValue += GetSystemValue(player.GetInterceptorSystem(), false);
        }

        return lValue;
    }
    
    public int GetPlayerNeutralValue(Player player)
    {
        int lValue = 0;
        
        for(OreMine oreMine : OreMines.values())
        {
            if(oreMine.GetOwnerID() == player.GetID())
            {
                lValue += GetStructureValue(oreMine);
            }
        }
        
        return lValue;
    }
    
    public int GetPlayerTotalValue(Player player)
    {
        return GetPlayerOffenseValue(player) + GetPlayerDefenseValue(player) + GetPlayerNeutralValue(player) + player.GetWealth();
    }

    public float GetNetWorthMultiplier(Player inflictor, Player inflictee)
    {
        float fltInflictorWorth = GetPlayerTotalValue(inflictor);
        float fltInflicteeWorth = GetPlayerTotalValue(inflictee);

        return fltInflicteeWorth / fltInflictorWorth;
    }
    
    public int GetAllianceOffenseValue(Alliance alliance)
    {
        int lValue = 0;
        
        for(Player player : Players.values())
        {
            if(player.GetAllianceMemberID() == alliance.GetID())
                lValue += GetPlayerOffenseValue(player);
        }
        
        return lValue;
    }
    
    public int GetAllianceDefenseValue(Alliance alliance)
    {
        int lValue = 0;

        for(Player player : Players.values())
        {
            if(player.GetAllianceMemberID() == alliance.GetID())
                lValue += GetPlayerDefenseValue(player);
        }
        
        return lValue;
    }
    
    public int GetAllianceTotalValue(Alliance alliance)
    {
        int lValue = 0;
        
        for(Player player : Players.values())
        {
            if(player.GetAllianceMemberID() == alliance.GetID())
                lValue += GetPlayerTotalValue(player);
        }
        
        return lValue;
    }
    
    public int GetHourlyIncome(Player player)
    {
        if(!player.GetAWOL())
        {
            int lAmountToAdd = 0;

            if(GetBasicIncomeEligible(player))
                lAmountToAdd += config.GetHourlyWealth();

            if(GetDiplomaticPresenceEligible(player))
                lAmountToAdd += config.GetHourlyBonusDiplomaticPresence();
            
            if(GetPoliticalEngagementEligible(player))
                lAmountToAdd += config.GetHourlyBonusPoliticalEngagement();
            
            if(GetDefenderOfTheNationEligible(player))
                lAmountToAdd += config.GetHourlyBonusDefenderOfTheNation();
            
            if(GetNuclearSuperpowerEligible(player))
                lAmountToAdd += config.GetHourlyBonusNuclearSuperpower();
            
            lAmountToAdd += GetWeeklyKillsBonus(player);
            
            if(GetSurvivorEligible(player))
                lAmountToAdd += config.GetHourlyBonusSurvivor();
            
            if(GetHippyEligible(player))
                lAmountToAdd += config.GetHourlyBonusHippy();
            
            if(GetPeaceMakerEligible(player))
                lAmountToAdd += config.GetHourlyBonusPeaceMaker();
            
            if(GetWarMongerEligible(player))
                lAmountToAdd += config.GetHourlyBonusWarMonger();
            
            if(GetLoneWolfEligible(player))
                lAmountToAdd += config.GetHourlyBonusLoneWolf();
            
            return lAmountToAdd;
        }
        
        return 0;
    }
    
    public boolean GetBasicIncomeEligible(Player player)
    {
        return !player.Destroyed();
    }
    
    public boolean GetDiplomaticPresenceEligible(Player player)
    {
        return player.GetAvatarID() != Defs.THE_GREAT_BIG_NOTHING;
    }
    
    public boolean GetPoliticalEngagementEligible(Player player)
    {
        if(player.GetAllianceMemberID() != Alliance.ALLIANCE_ID_UNAFFILIATED)
        {
            Alliance alliance = Alliances.get(player.GetAllianceMemberID());
            
            return alliance.GetAvatarID() != Defs.THE_GREAT_BIG_NOTHING;
        }
        
        return false;
    }
    
    public boolean GetDefenderOfTheNationEligible(Player player)
    {
        for(SAMSite samSite : SAMSites.values())
        {
            if(samSite.GetOwnedBy(player.GetID()))
            {
                if(samSite.GetOnline())
                    return true;
            }
        }
        
        for(SentryGun sentryGun : SentryGuns.values())
        {
            if(sentryGun.GetOwnedBy(player.GetID()))
            {
                if(sentryGun.GetOnline())
                    return true;
            }
        }
        
        return false;
    }
    
    public boolean GetNuclearSuperpowerEligible(Player player)
    {
        for(MissileSite site : MissileSites.values())
        {
            if(site.CanTakeNukes())
            {
                if(site.GetOwnedBy(player.GetID()))
                    return true;
            }
        }
        
        return false;
    }
    
    public int GetWeeklyKillsBonus(Player player)
    {
        int lMultiplier = 0;

        while(player.GetKills() >= (int)(Math.pow(2, lMultiplier)))
        {
            lMultiplier++;
        }
        
        return config.GetHourlyBonusWeeklyKillsBatch() * lMultiplier;
    }
    
    public boolean GetSurvivorEligible(Player player)
    {
        return player.GetDeaths() == 0;
    }
    
    public boolean GetHippyEligible(Player player)
    {
        return player.GetOffenceSpending() == 0;
    }
    
    public boolean GetPeaceMakerEligible(Player player)
    {
        int lFriends = 0;
        int lActivePlayers = 0;
        
        for(Player otherPlayer : Players.values())
        {
            if(!otherPlayer.GetAWOL())
            {
                lActivePlayers++;
                
                switch(GetAllegiance(player, otherPlayer))
                {
                    case YOU:
                    case ALLY:
                    case AFFILIATE:
                        lFriends++;
                        break;
                }
            }
        }
        
        if(lActivePlayers > 0)
            return ((float)lFriends / (float)lActivePlayers > Defs.RELATIONSHIP_BONUS_THRESHOLD);
        
        return false;
    }
    
    public boolean GetWarMongerEligible(Player player)
    {
        int lEnemies = 0;
        int lActivePlayers = 0;
        
        for(Player otherPlayer : Players.values())
        {
            if(!otherPlayer.GetAWOL())
            {
                lActivePlayers++;
                
                switch(GetAllegiance(player, otherPlayer))
                {
                    case ENEMY:
                        lEnemies++;
                        break;
                }
            }
        }
        
        if(lActivePlayers > 0)
            return ((float)lEnemies / (float)lActivePlayers > Defs.RELATIONSHIP_BONUS_THRESHOLD);
        
        return false;
    }
    
    public boolean GetLoneWolfEligible(Player player)
    {
        for(Player otherPlayer : Players.values())
        {
            if(!otherPlayer.GetAWOL())
            {
                if(!otherPlayer.ApparentlyEquals(player))
                {
                    if(otherPlayer.GetPosition().DistanceTo(player.GetPosition()) < config.GetLoneWolfDistance())
                        return false;
                }
            }
        }
        
        return true;
    }
    
            
    //---------------------------------------------------------------------------------------------------------------------------------
    // Public entity creators.
    //---------------------------------------------------------------------------------------------------------------------------------
    
    public void AddAlliance(Alliance alliance, boolean bMajor)
    {
        alliance.SetListener(this);
        
        Alliances.put(alliance.GetID(), alliance);
        
        AllianceUpdated(alliance, bMajor);
    }
    
    public void AddTreaty(Treaty treaty)
    {
        Treaties.put(treaty.GetID(), treaty);
        
        TreatyUpdated(treaty);
    }
    
    public void AddPlayer(Player player)
    {
        player.SetListener(this);
        Players.put(player.GetID(), player);
        
        EntityUpdated(player, false);
    }
    
    public void AddMissile(Missile missile)
    {
        missile.SetListener(this);
        Missiles.put(missile.GetID(), missile);
        
        EntityUpdated(missile, false);
    }
    
    public void AddInterceptor(Interceptor interceptor)
    {
        interceptor.SetListener(this);
        Interceptors.put(interceptor.GetID(), interceptor);
        
        EntityUpdated(interceptor, false);
    }
    
    public void AddMissileSite(MissileSite missileSite)
    {
        missileSite.SetListener(this);
        MissileSites.put(missileSite.GetID(), missileSite);
        
        EntityUpdated(missileSite, false);
    }
    
    public void AddSAMSite(SAMSite samSite)
    {
        samSite.SetListener(this);
        SAMSites.put(samSite.GetID(), samSite);
        
        EntityUpdated(samSite, false);
    }
    
    public void AddOreMine(OreMine oreMine)
    {
        oreMine.SetListener(this);
        OreMines.put(oreMine.GetID(), oreMine);
        
        EntityUpdated(oreMine, false);
    }
    
    public void AddSentryGun(SentryGun sentryGun)
    {
        sentryGun.SetListener(this);
        SentryGuns.put(sentryGun.GetID(), sentryGun);
        
        EntityUpdated(sentryGun, false);
    }
    
    public void AddLoot(Loot loot)
    {
        loot.SetListener(this);
        Loots.put(loot.GetID(), loot);
        
        EntityUpdated(loot, false);
    }
    
    public void AddRadiation(Radiation radiation)
    {
        radiation.SetListener(this);
        Radiations.put(radiation.GetID(), radiation);
        
        EntityUpdated(radiation, false);
    }
    
    public void AddPlayerMissileSystem(int lPlayerID, MissileSystem missileSystem)
    {
        Player player = Players.get(lPlayerID);
        player.AddMissileSystem(missileSystem);
        missileSystem.SetSystemListener(player);
    }
    
    public void AddPlayerInterceptorSystem(int lPlayerID, MissileSystem interceptorSystem)
    {
        Player player = Players.get(lPlayerID);
        player.AddInterceptorSystem(interceptorSystem);
        interceptorSystem.SetSystemListener(player);
    }
    
    public int GetGameTickStarts() { return lGameTickStarts; }
    public int GetGameTickEnds() { return lGameTickEnds; }
    public int GetCommTickStarts() { return lCommTickStarts; }
    public int GetCommTickEnds() { return lCommTickEnds; }
    
    
    //---------------------------------------------------------------------------------------------------------------------------------
    // Config accessor.
    //---------------------------------------------------------------------------------------------------------------------------------
    
    public Config GetConfig() { return config; }
    
    
    //---------------------------------------------------------------------------------------------------------------------------------
    // Public entity accessors.
    //---------------------------------------------------------------------------------------------------------------------------------
    
    public Collection<Alliance> GetAlliances() { return Alliances.values(); }
    public Collection<Treaty> GetTreaties() { return Treaties.values(); }
    public Collection<Player> GetPlayers() { return Players.values(); }
    public Collection<Missile> GetMissiles() { return Missiles.values(); }
    public Collection<Interceptor> GetInterceptors() { return Interceptors.values(); }
    public Collection<MissileSite> GetMissileSites() { return MissileSites.values(); }
    public Collection<SAMSite> GetSAMSites() { return SAMSites.values(); }
    public Collection<SentryGun> GetSentryGuns() { return SentryGuns.values(); }
    public Collection<OreMine> GetOreMines() { return OreMines.values(); }
    public Collection<Loot> GetLoots() { return Loots.values(); }
    public Collection<Radiation> GetRadiations() { return Radiations.values(); }
    
    public Collection<Structure> GetAllStructures()
    {
        List<Structure> Result = new ArrayList();
        
        for(MissileSite missileSite : MissileSites.values())
            Result.add(missileSite);
        
        for(SAMSite samSite : SAMSites.values())
            Result.add(samSite);
        
        for(SentryGun sentryGun : SentryGuns.values())
            Result.add(sentryGun);
        
        for(OreMine oreMine : OreMines.values())
            Result.add(oreMine);
        
        return Result;
    }
    
    private byte[] GetPriceData(Map<Byte, Integer> Prices)
    {
        ByteBuffer bb = ByteBuffer.allocate(5 * Prices.size());
        
        for(Map.Entry<Byte, Integer> entry : Prices.entrySet())
        {
            bb.put(entry.getKey());
            bb.putInt(entry.getValue());
        }
        
        return bb.array();
    }
    
    public Alliance GetAlliance(int lID) { return Alliances.get(lID); }
    public Treaty GetTreaty(int lID) { return Treaties.get(lID); }
    public Player GetPlayer(int lID) { return Players.get(lID); }
    public Missile GetMissile(int lID) { return Missiles.get(lID); }
    public Interceptor GetInterceptor(int lID) { return Interceptors.get(lID); }
    public MissileSite GetMissileSite(int lID) { return MissileSites.get(lID); }
    public SAMSite GetSAMSite(int lID) { return SAMSites.get(lID); }
    public SentryGun GetSentryGun(int lID) { return SentryGuns.get(lID); }
    public OreMine GetOreMine(int lID) { return OreMines.get(lID); }
    public Loot GetLoot(int lID) { return Loots.get(lID); }
    
    
    //---------------------------------------------------------------------------------------------------------------------------------
    // Abstract methods.
    //---------------------------------------------------------------------------------------------------------------------------------
    
    protected abstract void MissileExploded(Missile missile);
    protected abstract void InterceptorLostTarget(Interceptor interceptor);
    protected abstract void InterceptorReachedTarget(Interceptor interceptor);
    
    /**
     * An entity was updated or created.
     * @param entity The entity that was updated or created.
     * @param bOwner Whether only the player that "owns" the entity should be notified of the update. False if everyone.
     */
    protected abstract void EntityUpdated(LaunchEntity entity, boolean bOwner);
    
    protected abstract void EntityRemoved(LaunchEntity entity, boolean bDontCommunicate);   //Use don't communicate flag in instances where clients should be aware (e.g. due to time expiry, etc).
    protected abstract void AllianceUpdated(Alliance alliance, boolean bMajor);
    protected abstract void AllianceRemoved(Alliance alliance);
    protected abstract void TreatyUpdated(Treaty treaty);
    protected abstract void TreatyRemoved(Treaty treaty);
    
        
    //---------------------------------------------------------------------------------------------------------------------------------
    // LaunchEntityListener methods.
    //---------------------------------------------------------------------------------------------------------------------------------

    @Override
    public final void EntityChanged(LaunchEntity entity, boolean bOwner)
    {
        EntityUpdated(entity, bOwner);
    }

    @Override
    public void EntityChanged(Alliance alliance)
    {
        AllianceUpdated(alliance, false);
    }
}
