/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.game;

import launch.game.treaties.*;
import java.io.File;
import launch.comm.LaunchServerComms;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import launch.game.User.BanState;
import launch.game.entities.*;
import launch.game.systems.MissileSystem;
import launch.game.types.InterceptorType;
import launch.game.types.MissileType;
import launch.utilities.LaunchBannedApp;
import launch.utilities.LaunchClientLocation;
import launch.utilities.LaunchEvent;
import launch.utilities.ShortDelay;
import launch.utilities.LongDelay;
import launch.utilities.LaunchEvent.SoundEffect;
import launch.utilities.LaunchLog;
import static launch.utilities.LaunchLog.LogType.LOCATIONS;
import launch.utilities.LaunchPerf;
import launch.utilities.LaunchReport;
import launch.utilities.LaunchUtilities;
import launch.utilities.LocationSpoofCheck;

/**
 *
 * @author tobster
 */
public class LaunchServerGame extends LaunchGame implements LaunchServerGameInterface
{
    private static final int PROSCRIBED_ARTICLE_EXPIRY = 604800000;   //7 days for proscribed IPs and locations to stay proscribed.
    private static final float PROSCRIBED_LOCATION_COLLISION = 10.0f; //10km from banned player locations are proscribed.
    
    private class ProscribedArticle
    {
        private int lID;
        public LongDelay dlyExpiry;
        
        public ProscribedArticle(int lID)
        {
            this.lID = lID;
            dlyExpiry = new LongDelay(PROSCRIBED_ARTICLE_EXPIRY);
        }
        
        public final void Tick(int lMS)
        {
            dlyExpiry.Tick(lMS);
        }
        
        public int GetID() { return lID; }
        public final boolean Expired() { return dlyExpiry.Expired(); }
    }
    
    private class ProscribedIP extends ProscribedArticle
    {
        public String strIPAddress;
        
        public ProscribedIP(int lID, String strIPAddress)
        {
            super(lID);
            this.strIPAddress = strIPAddress;
        }
    }
    
    private class ProscribedLocation extends ProscribedArticle
    {
        public GeoCoord geoLocation;
        
        public ProscribedLocation(int lID, GeoCoord geoLocation)
        {
            super(lID);
            this.geoLocation = geoLocation;
        }
    }
    
    private final LaunchServerAppInterface application;
    private final LaunchServerComms comms;
    
    private static final short HP_PER_INTERVAL = 1;
    private static final int CHARGE_INTERVAL = 3600000;    //Hourly charge for structures.
    private static final int BACKUP_INTERVAL = 7200000;     //Back the game up every two hours.
    
    //Accounts.
    private final Map<String, User> Users = new ConcurrentHashMap();
    
    //Indices for new entities.
    private AtomicInteger lAllianceIndex = new AtomicInteger();
    private AtomicInteger lTreatyIndex = new AtomicInteger();
    private AtomicInteger lPlayerIndex = new AtomicInteger();
    private AtomicInteger lLootIndex = new AtomicInteger();
    private AtomicInteger lMissileSiteIndex = new AtomicInteger();
    private AtomicInteger lSAMSiteIndex = new AtomicInteger();
    private AtomicInteger lSentryGunIndex = new AtomicInteger();
    private AtomicInteger lOreMineIndex = new AtomicInteger();
    private AtomicInteger lRadiationIndex = new AtomicInteger();
    private AtomicInteger lMissileIndex = new AtomicInteger();
    private AtomicInteger lInterceptorIndex = new AtomicInteger();
    private AtomicInteger lProscribedIPIndex = new AtomicInteger();
    private AtomicInteger lProscribedLocationIndex = new AtomicInteger();
    
    //Health generation and radiation damage.
    private ShortDelay dlyHealthGeneration = new ShortDelay();
    private ShortDelay dlyRadiationDamage = new ShortDelay();
    
    //Time transition detection (i.e. end of week, day and hour).
    private int lCurrentDay = Calendar.getInstance(TimeZone.getTimeZone("GMT")).get(Calendar.DAY_OF_WEEK);
    private int lCurrentHour = Calendar.getInstance(TimeZone.getTimeZone("GMT")).get(Calendar.HOUR_OF_DAY);
    
    //Backup interval.
    private ShortDelay dlyBackup = new ShortDelay(BACKUP_INTERVAL);
    
    //Banned locations/IPs.
    private Map<Integer, ProscribedIP> ProscribedIPs = new ConcurrentHashMap();
    private Map<Integer, ProscribedLocation> ProscribedLocations = new ConcurrentHashMap();
    
    public LaunchServerGame(Config config, LaunchServerAppInterface application, int lPort)
    {
        super(config);
        this.application = application;
        comms = new LaunchServerComms(this, lPort);
    }
    
    /**
     * Thread-safely gets a unique ID for new stuff. 
     * @param atomicInteger The atomic counter to use.
     * @param container The corresponding container for which a unique key is required.
     * @return A unique ID for a new thing.
     */
    private int GetAtomicID(AtomicInteger atomicInteger, Map map)
    {
        int lID = atomicInteger.getAndIncrement();
        
        while(map.containsKey(lID) || lID == LaunchEntity.ID_NONE)
        {
            lID = atomicInteger.getAndIncrement();
        }
        
        return lID;
    }

    @Override
    public void StartServices()
    {
        //Server only: Perform post-load consolidation.
        for(User user : Users.values())
        {
            Player player = Players.get(user.GetPlayerID());
            
            if(player != null)
            {
                player.SetUser(user);
            }
        }
        
        comms.Begin();
        super.StartServices();
    }

    @Override
    protected void CommsTick(int lMS)
    {
        lCommTickStarts++;
        comms.Tick(lMS);
        lCommTickEnds++;
    }
    
    public void ShutDown()
    {
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, "Shutting down.");
        Save();
        comms.ShutDown();
        
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, "Stopping service tasks.");
        
        if(seService != null)
        {
            seService.shutdown();
        }
    }
    
    public void Save()
    {
        //TO DO: Pick individual debug flags when they exist (currently just used as boolean to indicate "debug mode").
        if(config.GetDebugFlags() == 0x00)
        {
            LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, "Saving game.");
            application.SaveTheGame();
        }
        else
        {
            LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, "Not saving game as we are in debug mode.");
        }
    }
    
    public boolean GetRunning()
    {
        return !comms.GetShutDown();
    }

    @Override
    protected void GameTick(int lMS)
    {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

        LaunchPerf.BeginSample();

        //Tell the comms to start buffering any updates we give it, to dispatch at the end of the tick.
        comms.BufferUpdates();
        
        //Tick proscribed articles.
        for(ProscribedIP proscribedIP : ProscribedIPs.values())
        {
            proscribedIP.Tick(lMS);
            
            if(proscribedIP.Expired())
                ProscribedIPs.remove(lMS);
        }
        
        for(ProscribedLocation proscribedLocation : ProscribedLocations.values())
        {
            proscribedLocation.Tick(lMS);
            
            if(proscribedLocation.Expired())
                ProscribedLocations.remove(lMS);
        }

        super.GameTick(lMS);

        //Process hourly events.
        int lHour = calendar.get(Calendar.HOUR_OF_DAY);

        if(lHour != lCurrentHour)
        {
            LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, "It's a new hour!");
            lCurrentHour = lHour;
            HourEnded();
        }

        //Process missile sites.
        for(MissileSite missileSite : MissileSites.values())
        {
            ProcessStructure(missileSite, config.GetMissileSiteMaintenanceCost(), true, false);
            
            if(missileSite.GetSelling() && missileSite.GetStateTimeExpired())
            {
                Player owner = Players.get(missileSite.GetOwnerID());

                if(owner != null)
                {
                    owner.AddWealth(GetSaleValue(missileSite));
                }

                MissileSites.remove(missileSite.GetID());
                EntityRemoved(missileSite, false);
            }
        }

        //Process SAM sites.
        for(SAMSite samSite : SAMSites.values())
        {
            ProcessStructure(samSite, config.GetSAMSiteMaintenanceCost(), false, true);
            
            if(samSite.GetSelling() && samSite.GetStateTimeExpired())
            {
                Player owner = Players.get(samSite.GetOwnerID());

                if(owner != null)
                {
                    owner.AddWealth(GetSaleValue(samSite));
                }

                SAMSites.remove(samSite.GetID());
                EntityRemoved(samSite, false);
            }
        }

        //Process sentry guns.
        for(SentryGun sentryGun : SentryGuns.values())
        {
            ProcessStructure(sentryGun, config.GetSentryGunMaintenanceCost(), false, true);

            if(sentryGun.GetOnline())
            {
                if(sentryGun.GetCanFire())
                {
                    Player sentryGunOwner = Players.get(sentryGun.GetOwnerID());

                    //Don't engage if the owner is AWOL or banned.
                    if(!sentryGunOwner.GetAWOL() && !sentryGunOwner.GetBanned_Server())
                    {
                        //Engage players.
                        for(Player player : Players.values())
                        {
                            //Don't engage dead or respawn protected players.
                            if(!player.Destroyed() && !player.GetRespawnProtected())
                            {
                                if(sentryGun.GetPosition().DistanceTo(player.GetPosition()) <= config.GetSentryGunRange())
                                {
                                    if(!WouldBeFriendlyFire(sentryGunOwner, player))
                                    {
                                        //Brrrrp!
                                        //Decide if the player died.
                                        if(random.nextFloat() < config.GetSentryGunHitChance())
                                        {
                                            short nDamageInflicted = player.InflictDamage(config.GetPlayerBaseHP());
                                            Scoring_DamageInflicted(sentryGunOwner, player, nDamageInflicted);
                                            Scoring_Kill(sentryGunOwner, player);
                                            Scoring_Death(player);
                                            player.SetDead(config.GetRespawnTime());

                                            CreateEvent(new LaunchEvent(String.format("%s's sentry gun blew %s to pieces.", sentryGunOwner.GetName(), player.GetName()), SoundEffect.DEATH));
                                            CreateReport(new LaunchReport(String.format("%s got too close to %s's sentry gun!", player.GetName(), sentryGunOwner.GetName()), true, sentryGunOwner.GetID(), player.GetID()));

                                            //Check if the alliance is annihilated.
                                            if(player.GetAllianceMemberID() != Alliance.ALLIANCE_ID_UNAFFILIATED)
                                            {
                                                Alliance alliance = Alliances.get(player.GetAllianceMemberID());
                                                AllianceCleanupCheck(alliance);
                                            }

                                            EstablishAllStructureThreats();
                                        }
                                        else
                                        {
                                            CreateEvent(new LaunchEvent(String.format("%s's sentry gun missed %s.", sentryGunOwner.GetName(), player.GetName()), SoundEffect.SENTRY_GUN_MISS));
                                        }

                                        sentryGun.SetReloadTime(config.GetSentryGunReloadTime());
                                    }
                                }
                            }
                        }
                        
                        //Engage missiles.
                        for(Missile missile : Missiles.values())
                        {
                            if(missile.Flying())
                            {
                                if(sentryGun.GetOwnerID() != missile.GetOwnerID())
                                {
                                    if(sentryGun.GetPosition().DistanceTo(missile.GetPosition()) <= config.GetSentryGunRange())
                                    {
                                        Player missileOwner = Players.get(missile.GetOwnerID());

                                        //Don't shoot friendly missiles going over the top; but do engage them if they're threatening the player (closes fire - affiliate - undefended loophole).
                                        if(!WouldBeFriendlyFire(sentryGunOwner, missileOwner) || ThreatensPlayerOptimised(missile, sentryGunOwner, GetMissileTarget(missile), config.GetMissileType(missile.GetType())))
                                        {
                                            //Brrrrp!
                                            //Decide if the missile got shot down.
                                            if(random.nextFloat() < config.GetSentryGunHitChance())
                                            {
                                                missile.Destroy();
                                                EntityRemoved(missile, false);

                                                CreateEvent(new LaunchEvent(String.format("%s's sentry gun shot down %s's missile.", sentryGunOwner.GetName(), missileOwner.GetName()), SoundEffect.SENTRY_GUN_HIT));
                                                CreateReport(sentryGunOwner, new LaunchReport(String.format("Your sentry gun shot down %s's missile.", missileOwner.GetName()), false, sentryGunOwner.GetID(), missileOwner.GetID()));
                                                CreateReport(missileOwner, new LaunchReport(String.format("%s's sentry gun shot down your missile.", sentryGunOwner.GetName()), false, missileOwner.GetID(), sentryGunOwner.GetID()));
                                            }
                                            else
                                            {
                                                CreateEvent(new LaunchEvent(String.format("%s's sentry gun missed %s's missile.", sentryGunOwner.GetName(), missileOwner.GetName()), SoundEffect.SENTRY_GUN_MISS));
                                                CreateReport(sentryGunOwner, new LaunchReport(String.format("Your sentry gun missed %s's missile.", missileOwner.GetName()), false, sentryGunOwner.GetID(), missileOwner.GetID()));
                                                CreateReport(missileOwner, new LaunchReport(String.format("%s's sentry gun missed your missile.", sentryGunOwner.GetName()), false, missileOwner.GetID(), sentryGunOwner.GetID()));
                                            }

                                            sentryGun.SetReloadTime(config.GetSentryGunReloadTime());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            if(sentryGun.GetSelling() && sentryGun.GetStateTimeExpired())
            {
                Player owner = Players.get(sentryGun.GetOwnerID());

                if(owner != null)
                {
                    owner.AddWealth(GetSaleValue(sentryGun));
                }

                SentryGuns.remove(sentryGun.GetID());
                EntityRemoved(sentryGun, false);
            }
        }

        //Process ore mines.
        for(OreMine oreMine : OreMines.values())
        {
            ProcessStructure(oreMine, config.GetOreMineMaintenanceCost(), false, false);

            if(oreMine.GetGenerateOre())
            {
                //Process ore generation.
                GeoCoord geoPosition = new GeoCoord(oreMine.GetPosition());
                geoPosition.Move(random.nextDouble() * (2.0 * Math.PI), random.nextDouble() * config.GetOreMineRadius());

                //Add loot with random value, offset by ore mine competition value.
                int lExpiry = config.GetOreMinExpiry() + (int)(random.nextFloat() * (float)((config.GetOreMaxExpiry()- config.GetOreMinExpiry()) + 0.5f));
                int lValue = (int)((random.nextFloat() * (float)GetMaxPotentialOreMineReturn(oreMine)) + 0.5f);
                CreateLoot(geoPosition, lValue, lExpiry, "Ore");

                oreMine.SetGenerateTime(config.GetOreMineGenerateTime());
            }
            
            if(oreMine.GetSelling() && oreMine.GetStateTimeExpired())
            {
                Player owner = Players.get(oreMine.GetOwnerID());

                if(owner != null)
                {
                    owner.AddWealth(GetSaleValue(oreMine));
                }

                OreMines.remove(oreMine.GetID());
                EntityRemoved(oreMine, false);
            }
        }

        //Process health regeneration.
        dlyHealthGeneration.Tick(lMS);

        if(dlyHealthGeneration.Expired())
        {
            LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, "Processing health regeneration...");

            for(Player player : Players.values())
            {
                //Player is alive.
                if(player.Functioning())
                {
                    //Player is not in a radioactive area.
                    if(!GetRadioactive(player, true))
                    {
                        //Player is not at full health.
                        if(player.GetHP() < player.GetMaxHP())
                        {
                            //Heal them 1hp.
                            player.AddHP(HP_PER_INTERVAL);
                            LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s healed a bit.", player.GetName()));
                        }
                    }
                }
            }

            dlyHealthGeneration.Set(config.GetHealthInterval());
        }

        //Process radiation damage.
        dlyRadiationDamage.Tick(lMS);

        if(dlyRadiationDamage.Expired())
        {
            LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, "Processing radiation damage...");

            for(Player player : Players.values())
            {
                //Player is alive.
                if(player.Functioning())
                {
                    //Player is in a radioactive area.
                    if(GetRadioactive(player, true))
                    {
                        //Damage them 1hp.
                        player.InflictDamage(HP_PER_INTERVAL);
                        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s damaged a bit.", player.GetName()));

                        //Send out event if they are dead.
                        if(player.Destroyed())
                        {
                            CreateEvent(new LaunchEvent(String.format("%s died of radiation poisoning.", player.GetName()), SoundEffect.DEATH));
                            CreateReport(player, new LaunchReport("You died of radiation poisoning.", true, player.GetID()));
                            Scoring_Death(player);
                            player.SetDead(config.GetRespawnTime());
                        }
                    }
                }
            }

            dlyRadiationDamage.Set(config.GetRadiationInterval());
        }

        //Process end of day/week events.
        int lDay = calendar.get(Calendar.DAY_OF_WEEK);

        if(lDay != lCurrentDay)
        {
            LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, "It's a new day!");

            //Midnight has passed, this is a new day.
            lCurrentDay = lDay;

            DayEnded();

            if(lCurrentDay == Calendar.MONDAY)
            {
                //And we've transitioned to Monday, and thus start a new week.
                WeekEnded();
            }
        }

        LaunchPerf.Measure(LaunchPerf.Metric.MoneyHealthAndMajorEvents);

        //Process player defence systems.
        ProcessPlayerDefences();

        LaunchPerf.Measure(LaunchPerf.Metric.PlayerDefences);

        //Process player states.
        for(Player player : Players.values())
        {
            //Process respawn protection.
            if(player.GetRespawnProtected())
            {
                if(player.GetStateTimeExpired())
                {
                    LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s's respawn protection time expired.", player.GetName()));
                    RemoveRespawnProtection(player);
                }
            }

            //Process AWOL.
            if(!player.GetAWOL())
            {
                if((player.GetLastSeen() + config.GetAWOLTime() < System.currentTimeMillis()))
                {
                    SetPlayerAWOL(player, false);

                    if(player.GetAllianceMemberID() != Alliance.ALLIANCE_ID_UNAFFILIATED)
                    {
                        Alliance alliance = Alliances.get(player.GetAllianceMemberID());

                        //Disband the alliance?
                        AllianceCleanupCheck(alliance);

                        //Set the player's alliance ID to unaffiliated so there's no crash if they come back.
                        player.SetIsAnMP(false);
                        player.SetAllianceID(Alliance.ALLIANCE_ID_UNAFFILIATED);
                    }
                }
            }
            else
            {
                //Remove AWOL players after specified period of time.
                if((player.GetLastSeen() + config.GetRemoveTime() < System.currentTimeMillis()) && !player.GetIsAnAdmin())
                {
                    LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("Removing player ID %d (%s) from the game as they've been AWOL for too long...", player.GetID(), player.GetName()));
                    CleanUpOwnedEntities(player.GetID());
                    Players.remove(player.GetID());

                    //Remove the user if they are not banned (otherwise keep it to ensure they serve their sentence).
                    User removableUser = player.GetUser();

                    if(removableUser != null)
                    {
                        if(removableUser.GetBanState() != BanState.NOT)
                            Users.remove(removableUser.GetIMEI());
                        else
                            removableUser.Expire();
                    }
                }
            }
        }

        //Process user ticks (ban durations)
        for(User user : Users.values())
            user.Tick(lMS);

        LaunchPerf.Measure(LaunchPerf.Metric.PlayerStates);

        //Tell the comms to dispatch the updates.
        comms.DispatchUpdates();

        //Backup periodically.
        dlyBackup.Tick(lMS);

        if(dlyBackup.Expired())
        {
            Save();
            dlyBackup.Set(BACKUP_INTERVAL);
        }

        LaunchPerf.Measure(LaunchPerf.Metric.DispatchAndBackup);

        LaunchPerf.Consolidate();
            
        lGameTickEnds++;
    }
    
    /**
     * Process a structure, including power, state changes and maintenance costs.
     * @param structure The structure to process.
     * @param lMaintenanceCost The hourly cost of maintaining the structure while not offline or selling.
     * @param DeadContainer The container to put the structure in for cleanup if it disappears.
     * @param bOffensiveCost If true, maintenance costs incurred affect offensive costs for scoring.
     * @param bDefensiveCost If true, maintenance costs incurred affect defensive costs for scoring.
     */
    private void ProcessStructure(Structure structure, int lMaintenanceCost, boolean bOffensiveCost, boolean bDefensiveCost)
    {
        if(structure.GetRunning())
        {
            if(structure.GetChargeOwner())
            {
                Player owner = Players.get(structure.GetOwnerID());

                if(owner != null)
                {
                    if(owner.SubtractWealth(lMaintenanceCost))
                    {
                        //Player was charged successfully.
                        structure.SetChargeOwnerTime(CHARGE_INTERVAL);
                        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s charged for online %s.", owner.GetName(), structure.GetTypeName()));
                        
                        if(bOffensiveCost)
                            Scoring_OffenceSpending(owner, lMaintenanceCost);
                        
                        if(bDefensiveCost)
                            Scoring_DefenceSpending(owner, lMaintenanceCost);
                    }
                    else
                    {
                        //Insufficient funds. Take the site offline.
                        structure.TakeOffline();
                        CreateEvent(new LaunchEvent(String.format("%s's %s went offline due to lack of funds.", owner.GetName(), structure.GetTypeName())));
                        CreateReport(owner, new LaunchReport(String.format("Your %s went offline due to lack of funds.", structure.GetTypeName()), true, owner.GetID()));
                    }
                }
                else
                {
                    //Orphaned structure. Take it offline.
                    structure.TakeOffline();
                }
            }
        }
    }
    
    private void RemoveRespawnProtection(Player player)
    {
        if(player.GetRespawnProtected())
        {
            player.SetRespawnProtected(false);
            
            //Remove respawn protection from structures.
            for(Structure structure : GetAllStructures())
            {
                if(structure.GetOwnerID() == player.GetID())
                {
                    if(structure.GetRespawnProtected())
                        structure.SetRespawnProtected(false);
                }
            }
            
            EstablishAllStructureThreats();
        }
    }
    
    /**
     * Sets a player to AWOL.
     * @param player Player to set AWOL.
     * @param bDoRageQuitCheck If the player has requested AWOL, set this to true to enable rage quit sanctioning checks.
     */
    private void SetPlayerAWOL(Player player, boolean bDoRageQuitCheck)
    {
        RemoveRespawnProtection(player);
        
        player.SetAWOL(true);
        
        if(bDoRageQuitCheck)
        {
            //Reward any players for their death.
            List<Player> Attackers = new ArrayList();

            for(Missile missile : Missiles.values())
            {
                if(missile.GetOwnerID() != player.GetID())
                {
                    Player owner = Players.get(missile.GetOwnerID());

                    if(!Attackers.contains(owner))
                    {
                        MissileType type = config.GetMissileType(missile.GetType());

                        if(GetMissileTarget(missile).DistanceTo(player.GetPosition()) <= config.GetBlastRadius(type))
                            Attackers.add(owner);
                    }
                }
            }

            for(Player attacker : Attackers)
            {
                Scoring_Kill(attacker, player);
                CreateReport(new LaunchReport(String.format("%s made %s rage quit!", attacker.GetName(), player.GetName()), true, attacker.GetID(), player.GetID()));
            }
            
            //Punish those who rage quit with a long respawn time.
            if(Attackers.size() > 0)
            {
                Scoring_Death(player);
                player.SetDead(Math.max(config.GetRespawnTime(), Defs.MS_PER_DAY));
            }
            else
                player.SetDead(config.GetRespawnTime()); 
        }
        else
        {
            //Prevent whining from people who didn't rage quit.
            player.SetDead(config.GetRespawnTime());
        }

        //Kill them for at least 24 hours to prevent abuse.
        CreateEvent(new LaunchEvent(String.format("%s has gone AWOL.", player.GetName())));
    }
    
    /**
     * To be called at the end of every hour to process hourly events.
     */
    private void HourEnded()
    {
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, "Performing wealth generation...");

        for(Player player : Players.values())
        {
            int lAmountToAdd = GetHourlyIncome(player);
            
            if(player.GetWealth() + lAmountToAdd > Defs.WEALTH_CAP)
                lAmountToAdd = Defs.WEALTH_CAP - player.GetWealth();
            
            if(lAmountToAdd > 0)
            {
                LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s gets %d.", player.GetName(), lAmountToAdd));
                player.AddWealth(lAmountToAdd);
                Scoring_StandardIncomeReceived(player, lAmountToAdd);
            }
            else
            {
                LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s gets nothing.", player.GetName()));
            }
        }

        for(User user : Users.values())
        {
            user.HourlyTick();
        }
    }
    
    /**
     * To be called at the end of every day to process end of day events.
     */
    private void DayEnded()
    {
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, "Processing end of day events...");
        
        //Remove old user accounts, and arm all of them for multiaccount detection.
        for(User user : Users.values())
        {
            if(user.GetExpiredOn() > 0)
            {
                if(user.GetBanState() == BanState.NOT ||
                   (System.currentTimeMillis() - user.GetExpiredOn() > Defs.MS_PER_QUARTER))
                    Users.remove(user.GetIMEI());
            }
            else
            {
                user.ArmMultiAccountDetection();
            }
        }
    }

    /**
     * To be called at the end of every week to process weekly events.
     */
    private void WeekEnded()
    {
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, "Processing end of week events...");
        
        //Conclude all wars.
        for(Treaty treaty : Treaties.values())
        {
            if(treaty instanceof War)
                ConcludeWar((War)treaty);
        }

        /*//Dish out awards and that, provided there's enough players.
        if(Players.size() > 1)
        {
            LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, "Sufficient players exist to perform awards.");
            int lRegularFrom = 0;

            //Create a comparator for player scores.
            Comparator<Player> scoreComparator = new Comparator<Player>()
            {
                @Override
                public int compare(Player playerOne, Player playerTOther)
                {
                    return playerTOther.GetPoints() - playerOne.GetPoints();
                }
            };

            //Sort the players into a list, by score.
            List<Player> PlayersRanked = new ArrayList(Players.values());
            Collections.sort(PlayersRanked, scoreComparator);

            //Declare the potential champion.
            Player champion = null;

            //Nominate the champion. The champion is the player with the highest, >0, non-tying score; otherwise there is no champion.
            if((PlayersRanked.get(0).GetPoints() > 0) && (PlayersRanked.get(0).GetPoints() > PlayersRanked.get(1).GetPoints()))
            {
                champion = PlayersRanked.get(0);
                champion.AddWealth(config.GetChampionPrize());
                champion.SetChampion();
                lRegularFrom = 1;
                CreateEvent(new LaunchEvent(String.format("The week has finished. Congratulations to our new champion, %s, who with %dpoints has won £%d!", champion.GetName(), champion.GetPoints(), config.GetChampionPrize()), SoundEffect.MONEY));
                CreateReport(new LaunchReport(String.format("The week has finished. Congratulations to our new champion, %s, who with %dpoints has won £%d!", champion.GetName(), champion.GetPoints(), config.GetChampionPrize()), false, champion.GetID()));
            }
            else
            {
                CreateEvent(new LaunchEvent("The week has finished. There is no champion."));
                CreateReport(new LaunchReport("The week has finished. There is no champion.", false));
            }

            if(Players.size() > 3) //There must be at least four players in order to have veterans.
            {
                LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, "Sufficient players exist to assign veterans.");

                //Nominate the veterans. Veterans are non-champion, >0 scoring players that finished ABOVE average; that is, not part of a tying-group that would cross the average threshold by player numbers.
                int lPlayersCounted = champion == null ? 0 : 1;

                //Calculate number of potential veterans, using deliberate integer division with no rounding to ensure "above" average and discount "on" average.
                int lPotentialVeterans = (Players.size() / 2) - lPlayersCounted;

                int lCurrentPrize = config.GetVeteranPrizeMax();
                int lPrizeDiminish = (config.GetVeteranPrizeMax() - config.GetVeteranPrizeMin()) / lPotentialVeterans;

                int lTieCumulator = 0; //Used to cumulate tying players.

                while(lPlayersCounted <= lPotentialVeterans)
                {
                    Player playerThis = PlayersRanked.get(lPlayersCounted);
                    Player playerNext = PlayersRanked.get(lPlayersCounted + 1);

                    if(playerThis.GetPoints() <= 0)
                    {
                        //Bust the whole thing once we're down to zero points.
                        break;
                    }

                    if(playerThis.GetPoints() > playerNext.GetPoints())
                    {
                        //This player scored higher than the next. They are a veteran, as well as any cumulated players.

                        //Compute the prize for this band (to take into account tying, etc).
                        int lPrizeMax = lCurrentPrize;
                        int lPrizeMin = lCurrentPrize - (lPrizeDiminish * (lTieCumulator));
                        int lPrizeRank = (lPrizeMax + lPrizeMin) / 2;
                        int lRank = (lPlayersCounted - lTieCumulator) + 1;
                        boolean bJoint = lTieCumulator > 0;

                        for(int i = lPlayersCounted - lTieCumulator; i <= lPlayersCounted; i++)
                        {
                            Player playerVeteran = PlayersRanked.get(i);
                            playerVeteran.AddWealth(lPrizeRank);
                            playerVeteran.SetVeteran();
                            lRegularFrom++;

                            if(bJoint)
                            {
                                CreateEvent(new LaunchEvent(String.format("Well done veteran %s, who has jointly finished in rank %d with %dpoints, and won £%d.", playerVeteran.GetName(), lRank, playerVeteran.GetPoints(), lPrizeRank)));
                                CreateReport(playerVeteran, new LaunchReport(String.format("Well done veteran, you jointly finished in rank %d with %dpoints, and won £%d!", lRank, playerVeteran.GetPoints(), lPrizeRank), false, playerVeteran.GetID()));
                            }
                            else
                            {
                                CreateEvent(new LaunchEvent(String.format("Well done veteran %s, who has finished in rank %d with %dpoints, and won £%d.", playerVeteran.GetName(), lRank, playerVeteran.GetPoints(), lPrizeRank)));
                                CreateReport(playerVeteran, new LaunchReport(String.format("Well done veteran, you finished in rank %d with %dpoints, and won £%d!", lRank, playerVeteran.GetPoints(), lPrizeRank), false, playerVeteran.GetID()));
                            }
                        }

                        lCurrentPrize -= lPrizeDiminish * (lTieCumulator + 1);
                        lTieCumulator = 0;
                    }
                    else
                    {
                        //This player shares their score with the next. Cumulate them, but don't decide if they're any good yet!
                        lTieCumulator++;
                    }

                    lPlayersCounted++;
                }
            }

            LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, "Assigning regulars and resetting scores...");

            //Everyone else is now a regular.
            for(int i = lRegularFrom; i < PlayersRanked.size(); i++)
            {
                PlayersRanked.get(i).SetRegular();
            }

            //Reset all scores.
            for(Player player : Players.values())
            {
                player.ResetPoints();
            }
        }

        //Rank alliances.
        //Create a comparator for alliance scores.
        Comparator<Alliance> allianceComparator = new Comparator<Alliance>()
        {
            @Override
            public int compare(Alliance allianceOne, Alliance allianceTOther)
            {
                //Most points.
                if(allianceTOther.GetPoints() != allianceOne.GetPoints())
                    return allianceTOther.GetPoints() - allianceOne.GetPoints();

                //Otherwise least members.
                return GetAllianceMemberCount(allianceOne) - GetAllianceMemberCount(allianceTOther);
            }
        };

        List<Alliance> AlliancesRanked = new ArrayList(Alliances.values());
        Collections.sort(AlliancesRanked, allianceComparator);
        int lRank = 1;
        for(Alliance alliance : AlliancesRanked)
        {
            alliance.SetRank(lRank++);
        }*/

        for(Player player : Players.values())
        {
            player.ResetStats();
        }

        for(Treaty treaty : Treaties.values())
        {
            Treaties.remove(treaty.GetID());
            TreatyRemoved(treaty);
        }
            
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, "Finished processing end of week events.");
    }
    
    private void ConcludeWar(War war)
    {
        Alliance alliance1 = Alliances.get(war.GetAllianceID1());
        Alliance alliance2 = Alliances.get(war.GetAllianceID2());
        
        Alliance winner = null;
        Alliance loser = null;
        
        if(war.GetWonFactors1() > war.GetWonFactors2())
        {
            winner = alliance1;
            loser = alliance2;
        }
        else if(war.GetWonFactors2() > war.GetWonFactors1())
        {
            winner = alliance2;
            loser = alliance1;
        }
        else
        {
            CreateReport(new LaunchReport(String.format("%s drew with %s. No money for any of you lot!", alliance1.GetName(), alliance2.GetName()), true, alliance1.GetID(), alliance2.GetID(), true, true));
        }
        
        if(winner != null && loser != null)
        {
            float fltPrizeFund = (float)war.GetTotalSpending() * Defs.WAR_REWARD_FACTOR;
            List<Player> Winners = GetMembers(winner);
            
            if(Winners.size() > 0)
            {
                int lPrizePerPlayer = (int)((fltPrizeFund / (float)Winners.size()) + 0.5f);
                CreateReport(new LaunchReport(String.format("%s won the war against %s! Each player wins £%d", winner.GetName(), loser.GetName(), lPrizePerPlayer), true, winner.GetID(), loser.GetID(), true, true));
                
                for(Player player : Winners)
                {
                    player.AddWealth(lPrizePerPlayer);
                }
            }
        }
    }
    
    private void ProcessPlayerDefences()
    {
        //SAM Sites.
        if(Missiles.size() > 0) //Nothing to do unless there are any missiles.
        {
            for(Missile missile : Missiles.values())
            {
                if(missile.Flying())
                {
                    GeoCoord geoTarget = GetMissileTarget(missile);
                    MissileType missileType = config.GetMissileType(missile.GetType());
                    float fltMissileSpeed = config.GetMissileSpeed(missileType.GetSpeedIndex());
                    int lMissileTimeToTarget = GetTimeToTarget(missile);

                    for(Player player : Players.values())
                    {
                        //See which players are threatened (ignoring the missile's owner).
                        if(missile.GetOwnerID() != player.GetID())
                        {
                            if(ThreatensPlayerOptimised(missile, player, geoTarget, missileType))
                            {
                                //Player threatened. Do they already have an interceptor dealing with the threat?
                                boolean bThreatUnchecked = true;

                                for(Interceptor interceptor : Interceptors.values())
                                {
                                    if(interceptor.GetOwnerID() == player.GetID())
                                    {
                                        if(interceptor.GetTargetID() == missile.GetID())
                                        {
                                            bThreatUnchecked = false;
                                            break;
                                        }
                                    }
                                }

                                if(bThreatUnchecked)
                                {
                                    //Prosecute the missile if possible using the best available (cheapest and nearest).
                                    SAMSite candidateSite = null;
                                    InterceptorType candidateType = null;
                                    int lCandidateTimeToIntercept = Integer.MAX_VALUE;

                                    for(SAMSite samSite : SAMSites.values())
                                    {
                                        if(samSite.GetOwnerID() == player.GetID())
                                        {
                                            MissileSystem samSystem = samSite.GetInterceptorSystem();

                                            if((samSite.GetAuto() || (samSite.GetSemiAuto() && (!GetPlayerOnline(player)) || player.Destroyed())) && samSite.GetOnline() && samSystem.ReadyToFire())
                                            {
                                                for(byte cType : GetAvailableInterceptors(samSystem))
                                                {
                                                    InterceptorType interceptorType = config.GetInterceptorType(cType);
                                                    float fltInterceptorSpeed = config.GetInterceptorSpeed(interceptorType.GetSpeedIndex());
                                                    
                                                    //Is the missile within this interceptor's range?
                                                    if(samSite.GetPosition().DistanceTo(missile.GetPosition()) <= config.GetInterceptorRange(interceptorType.GetRangeIndex()))
                                                    {
                                                        //Is the interceptor fast enough?
                                                        if(fltInterceptorSpeed > fltMissileSpeed)
                                                        {
                                                            if(candidateType != null)
                                                            {
                                                                //We already have a candidate. Is this cheaper or nearer?
                                                                if(config.GetInterceptorCost(interceptorType) < config.GetInterceptorCost(candidateType))
                                                                {
                                                                    //Cheaper. Accept if it can prosecute the missile.
                                                                    GeoCoord geoIntercept = missile.GetPosition().InterceptPoint(geoTarget, fltMissileSpeed, samSite.GetPosition(), fltInterceptorSpeed);
                                                                    int lTimeToIntercept = GetTimeToTarget(samSite.GetPosition(), geoIntercept, fltInterceptorSpeed);

                                                                    if(lTimeToIntercept < lMissileTimeToTarget)
                                                                    {
                                                                        candidateSite = samSite;
                                                                        candidateType = interceptorType;
                                                                        lCandidateTimeToIntercept = lTimeToIntercept;
                                                                    }
                                                                }
                                                                else if(config.GetInterceptorCost(interceptorType) == config.GetInterceptorCost(candidateType))
                                                                {
                                                                    //The same price. Accept if it can get there quicker.
                                                                    GeoCoord geoIntercept = missile.GetPosition().InterceptPoint(geoTarget, fltMissileSpeed, samSite.GetPosition(), fltInterceptorSpeed);
                                                                    int lTimeToIntercept = GetTimeToTarget(samSite.GetPosition(), geoIntercept, fltInterceptorSpeed);

                                                                    if(lCandidateTimeToIntercept < lTimeToIntercept)
                                                                    {
                                                                        candidateSite = samSite;
                                                                        candidateType = interceptorType;
                                                                        lCandidateTimeToIntercept = lTimeToIntercept;
                                                                    }
                                                                }
                                                            }
                                                            else
                                                            {
                                                                //No current candidate. This is good enough if it can reach.
                                                                GeoCoord geoIntercept = missile.GetPosition().InterceptPoint(geoTarget, fltMissileSpeed, samSite.GetPosition(), fltInterceptorSpeed);
                                                                int lTimeToIntercept = GetTimeToTarget(samSite.GetPosition(), geoIntercept, fltInterceptorSpeed);

                                                                if(lTimeToIntercept < lMissileTimeToTarget)
                                                                {
                                                                    candidateSite = samSite;
                                                                    candidateType = interceptorType;
                                                                    lCandidateTimeToIntercept = lTimeToIntercept;
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if(candidateSite != null)
                                    {
                                        //Launch the bastard.
                                        MissileSystem system = candidateSite.GetInterceptorSystem();

                                        for(byte cSlotNumber = 0; cSlotNumber < system.GetSlotCount(); cSlotNumber++)
                                        {
                                            if(system.GetSlotHasMissile(cSlotNumber) && (system.GetSlotMissileType(cSlotNumber) == candidateType.GetID()))
                                            {
                                                //Fire.
                                                Player missileOwner = Players.get(missile.GetOwnerID());
                                                system.Fire(cSlotNumber);
                                                CreateInterceptorLaunch(candidateSite.GetPosition().GetCopy(), candidateType.GetID(), player.GetID(), missile.GetID(), false);
                                                CreateEvent(new LaunchEvent(String.format("%s's SAM launched %s at %s's %s.", player.GetName(), candidateType.GetName(), missileOwner.GetName(), missileType.GetName()), SoundEffect.INTERCEPTOR_LAUNCH));
                                                CreateReport(player, new LaunchReport(String.format("Your SAM engaged %s's missile.", missileOwner.GetName()), false, player.GetID(), missileOwner.GetID()));
                                                CreateReport(missileOwner, new LaunchReport(String.format("%s's SAM engaged your missile.", player.GetName()), false, player.GetID(), missileOwner.GetID()));
                                                
                                                String strSiteName = candidateSite.GetName();
                                                if(strSiteName.equals(""))
                                                    strSiteName = "[Unnamed]";
                                                LaunchLog.Log(LaunchLog.LogType.SAM_SITE_AI, player.GetName(), String.format("%s at %s launched %s at %s's %s, at %s and headed for %s. Time to intercept %d. Missile to target %d.", strSiteName, candidateSite.GetPosition().toString(), candidateType.GetName(), missileOwner.GetName(), missileType.GetName(), missile.GetPosition().toString(), GetMissileTarget(missile).toString(), lCandidateTimeToIntercept, lMissileTimeToTarget));
                                                
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    //Process damage to entities caused by an explosion.
    private void ProcessDamage(Player inflictor, String strCause, GeoCoord geoOrigin, float fltBlastRadius, short nMaxDamage)
    {
        boolean bReestablishStructureThreats = false;
        
        //Players.
        for(Player player : Players.values())
        {
            if(player.Functioning() && !player.GetRespawnProtected() && player.GetPosition().GetValid())
            {
                float fltDistance = player.GetPosition().DistanceTo(geoOrigin);

                if(fltDistance <= fltBlastRadius)
                {
                    float fltDamageProportion = random.nextFloat() * (1.0f - (fltDistance / fltBlastRadius));
                    short nDamage = (short)(((float)nMaxDamage * fltDamageProportion) + 0.5f);
                    short nDamageInflicted = player.InflictDamage(nDamage);
                    
                    Scoring_DamageInflicted(inflictor, player, nDamageInflicted);
                    
                    if(player.Destroyed())
                    {
                        //The explosion killed the player.
                        bReestablishStructureThreats = true;
                        Scoring_Kill(inflictor, player);
                        Scoring_Death(player);
                        player.SetDead(config.GetRespawnTime());
                        CreateEvent(new LaunchEvent(String.format("%s hit %s, causing %d HP of damage. %s died.", strCause, player.GetName(), nDamageInflicted, player.GetName()), SoundEffect.DEATH));
                        
                        CreateReport(new LaunchReport(String.format("%s killed %s!", inflictor.GetName(), player.GetName()), true, inflictor.GetID(), player.GetID()));
                        
                        //Check if the alliance is annihilated.
                        if(player.GetAllianceMemberID() != Alliance.ALLIANCE_ID_UNAFFILIATED)
                        {
                            Alliance alliance = Alliances.get(player.GetAllianceMemberID());
                            AllianceCleanupCheck(alliance);
                        }
                    }
                    else
                    {
                        //The explosion damaged the player.
                        CreateEvent(new LaunchEvent(String.format("%s hit %s, causing %d HP of damage.", strCause, player.GetName(), nDamageInflicted), SoundEffect.EXPLOSION));
                        CreateReport(player, new LaunchReport(String.format("You were wounded by %s!", inflictor.GetName()), true, player.GetID(), inflictor.GetID()));
                        CreateReport(inflictor, new LaunchReport(String.format("You wounded %s!", player.GetName()), true, inflictor.GetID(), player.GetID()));
                    }
                }
            }
        }
        
        //Structures.
        for(Structure structure : GetAllStructures())
        {
            if(!structure.Destroyed() && !structure.GetRespawnProtected())
            {
                float fltDistance = structure.GetPosition().DistanceTo(geoOrigin);

                if(fltDistance <= fltBlastRadius)
                {
                    float fltDamageProportion = random.nextFloat() * (1.0f - (fltDistance / fltBlastRadius));
                    short nDamage = (short)(((float)nMaxDamage * fltDamageProportion) + 0.5f);
                    short nDamageInflicted = structure.InflictDamage(nDamage);
                    Player owner = GetPlayer(structure.GetOwnerID());
                    Scoring_DamageInflicted(inflictor, owner, nDamageInflicted);

                    if(structure.Destroyed())
                    {
                        //The explosion killed the structure.
                        bReestablishStructureThreats = true;
                        CreateEvent(new LaunchEvent(String.format("%s hit %s's %s, causing %d HP of damage and destroying it.", strCause, owner.GetName(), structure.GetTypeName(), nDamageInflicted), SoundEffect.EXPLOSION));
                        CreateReport(owner, new LaunchReport(String.format("%s destroyed your %s!", inflictor.GetName(), structure.GetTypeName()), true, owner.GetID(), inflictor.GetID()));
                        CreateReport(inflictor, new LaunchReport(String.format("You destroyed %s's %s!", owner.GetName(), structure.GetTypeName()), true, inflictor.GetID(), owner.GetID()));
                        CreateRubble(structure.GetPosition(), GetSaleValue(structure), String.format("Rubble from %s's %s.", owner.GetName(), structure.GetTypeName()));
                    }
                    else
                    {
                        //The explosion damaged the structure.
                        CreateEvent(new LaunchEvent(String.format("%s hit %s's %s, causing %d HP of damage.", strCause, owner.GetName(), structure.GetTypeName(), nDamageInflicted), SoundEffect.EXPLOSION));
                        CreateReport(owner, new LaunchReport(String.format("%s damaged your %s!", inflictor.GetName(), structure.GetTypeName()), true, owner.GetID(), inflictor.GetID()));
                        CreateReport(inflictor, new LaunchReport(String.format("You damaged %s's %s!", owner.GetName(), structure.GetTypeName()), true, inflictor.GetID(), owner.GetID()));
                    }
                }
            }
        }
        
        if(bReestablishStructureThreats)
            EstablishAllStructureThreats();
    }
    
    //Process an EMP pulse.
    private void CreateEMPPulse(GeoCoord geoLocation, float fltRadius, int lPlayerID)
    {
        Player player = Players.get(lPlayerID);
        
        for(Structure structure : GetAllStructures())
        {
            if(structure.GetBooting() || structure.GetOnline())
            {
                if(random.nextFloat() < config.GetEMPChance())
                {
                    if(geoLocation.DistanceTo(structure.GetPosition()) < fltRadius)
                    {
                        Player victim = Players.get(structure.GetOwnerID());
                        
                        structure.Reboot(config.GetStructureBootTime());
                        CreateReport(victim, new LaunchReport(String.format("%s's EMP pulse caused your %s to reboot.", player.GetName(), structure.GetTypeName()), true, player.GetID(), victim.GetID()));
                        CreateReport(player, new LaunchReport(String.format("Your EMP pulse caused %s's %s to reboot.", victim.GetName(), structure.GetTypeName()), true, player.GetID(), victim.GetID()));
                    }
                }
            }
        }
    }
    
    /**
     * Register a player death for the purpose of scoring.
     * Only wars need the stats updating; the player's own deaths count property should have already been updated by calling SetDead().
     * @param player 
     */
    private void Scoring_Death(Player player)
    {
        if(player.GetAllianceMemberID() != Alliance.ALLIANCE_ID_UNAFFILIATED)
        {
            int lAllianceID = player.GetAllianceMemberID();
            
            for(Treaty treaty : Treaties.values())
            {
                if(treaty instanceof War)
                {
                    if(treaty.IsAParty(lAllianceID))
                        ((War)treaty).AddDeath(lAllianceID);
                }
            }
        }
    }
    
    /**
     * Register that a player killed another player.
     * All instances of such should use this as it increments the killing player's kill count (unlike registering deaths).
     * @param player 
     */
    private void Scoring_Kill(Player killer, Player victim)
    {
        killer.IncrementKills();
        
        if(killer.GetAllianceMemberID() != Alliance.ALLIANCE_ID_UNAFFILIATED && victim.GetAllianceMemberID() != Alliance.ALLIANCE_ID_UNAFFILIATED)
        {
            int lAllianceIDKiller = killer.GetAllianceMemberID();
            int lAllianceIDVictim = victim.GetAllianceMemberID();
            
            for(Treaty treaty : Treaties.values())
            {
                if(treaty instanceof War)
                {
                    if(treaty.AreParties(lAllianceIDKiller, lAllianceIDVictim))
                    {
                        ((War)treaty).AddKill(lAllianceIDKiller);
                    }
                }
            }
        }
    }
    
    /**
     * Increment offence spending when a player incurs a maintenance cost of an offensive structure.
     * Costs associated with firing weapons should be dealt with in the weapon launch code.
     * @param player The player incurring the maintenance cost.
     * @param lAmount The maintenance cost.
     */
    private void Scoring_OffenceSpending(Player player, int lAmount)
    {
        player.AddOffenceSpending(lAmount);
        
        //Add the amount to any wars involving this player.
        if(player.GetAllianceMemberID() != Alliance.ALLIANCE_ID_UNAFFILIATED)
        {
            int lAllianceID = player.GetAllianceMemberID();
            
            List<War> AffectedWars = new ArrayList();
            
            for(Treaty treaty : Treaties.values())
            {
                if(treaty instanceof War)
                {
                    if(treaty.IsAParty(lAllianceID))
                        AffectedWars.add((War)treaty);
                }
            }
            
            //Let it round down. I'm not being generous.
            int lAmountPerWar = (int)((float)lAmount / (float)AffectedWars.size());
            
            if(lAmountPerWar > 0)
            {
                for(War war : AffectedWars)
                {
                    war.AddOffenceSpending(lAllianceID, lAmountPerWar);
                }
            }
        }
    }
    
    /**
     * Increment defence spending when a player fires a defensive weapon.
     * @param player The player that fired a defensive weapons.
     * @param aggressor The player who's weapon is being prosecuted.
     * @param lAmount The cost of the defensive weapon.
     */
    private void Scoring_DefenceSpending(Player player, Player aggressor, int lAmount)
    {
        player.AddDefenceSpending(lAmount);
        
        //Add the amount to any wars involving both players.
        if(player.GetAllianceMemberID() != Alliance.ALLIANCE_ID_UNAFFILIATED && aggressor.GetAllianceMemberID() != Alliance.ALLIANCE_ID_UNAFFILIATED)
        {
            for(Treaty treaty : Treaties.values())
            {
                int lAllianceIDPlayer = player.GetAllianceMemberID();
                int lAllianceIDAggressor = aggressor.GetAllianceMemberID();
            
                if(treaty instanceof War)
                {
                    if(treaty.AreParties(lAllianceIDPlayer, lAllianceIDAggressor))
                    {
                        ((War)treaty).AddDefenceSpending(lAllianceIDPlayer, lAmount);
                    }
                }
            }
        }
    }
    
    /**
     * Increment defence spending when a player incurs a maintenance cost of a defensive structure.
     * @param player The player incurring the maintenance cost.
     * @param lAmount The maintenance cost.
     */
    private void Scoring_DefenceSpending(Player player, int lAmount)
    {
        player.AddDefenceSpending(lAmount);
        
        //Add the amount to any wars involving this player.
        if(player.GetAllianceMemberID() != Alliance.ALLIANCE_ID_UNAFFILIATED)
        {
            int lAllianceID = player.GetAllianceMemberID();
            
            List<War> AffectedWars = new ArrayList();
            
            for(Treaty treaty : Treaties.values())
            {
                if(treaty instanceof War)
                {
                    if(treaty.IsAParty(lAllianceID))
                        AffectedWars.add((War)treaty);
                }
            }
            
            //Let it round down. I'm not being generous.
            int lAmountPerWar = (int)((float)lAmount / (float)AffectedWars.size());
            
            if(lAmountPerWar > 0)
            {
                for(War war : AffectedWars)
                {
                    war.AddOffenceSpending(lAllianceID, lAmountPerWar);
                }
            }
        }
    }
    
    private void Scoring_DamageInflicted(Player inflictor, Player inflictee, short nAmount)
    {
        inflictor.AddDamageInflicted(nAmount);
        inflictee.AddDamageReceived(nAmount);
        
        if(inflictor.GetAllianceMemberID() != Alliance.ALLIANCE_ID_UNAFFILIATED && inflictee.GetAllianceMemberID() != Alliance.ALLIANCE_ID_UNAFFILIATED)
        {
            int lAllianceIDInflictor = inflictor.GetAllianceMemberID();
            int lAllianceIDInflictee = inflictee.GetAllianceMemberID();
            
            for(Treaty treaty : Treaties.values())
            {
                if(treaty instanceof War)
                {
                    if(treaty.AreParties(lAllianceIDInflictor, lAllianceIDInflictee))
                    {
                        ((War)treaty).AddDamageInflicted(lAllianceIDInflictor, nAmount);
                    }
                }
            }
        }
    }
    
    /**
     * Add standard income to any wars involving a player. Standard income is hourly generated wealth, not that from bonuses or loots.
     * This exists to effectively and unhackably handicap alliances by size (total wealth accumulation during a war).
     * @param player Player earning their standard income.
     * @param lAmount The amount of it.
     */
    private void Scoring_StandardIncomeReceived(Player player, int lAmount)
    {
        //Add the amount to any wars involving this player.
        if(player.GetAllianceMemberID() != Alliance.ALLIANCE_ID_UNAFFILIATED)
        {
            int lAllianceID = player.GetAllianceMemberID();
            
            for(Treaty treaty : Treaties.values())
            {
                if(treaty instanceof War)
                {
                    if(treaty.IsAParty(lAllianceID))
                        ((War)treaty).AddIncome(lAllianceID, lAmount);
                }
            }
        }
    }
    
    private List<Byte> GetAvailableInterceptors(MissileSystem missileSystem)
    {
        List result = new ArrayList();
        
        for(byte c = 0; c < missileSystem.GetSlotCount(); c++)
        {
            if(missileSystem.GetSlotHasMissile(c))
            {
                if(missileSystem.GetSlotReadyToFire(c))
                {
                    byte cType = missileSystem.GetSlotMissileType(c);

                    if(!result.contains(cType))
                    {
                        result.add(cType);
                    }
                }
            }
        }
        
        return result;
    }
    
    private void CreateAlliance(Player creator, String strName, String strDescription, int lAvatarID)
    {
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, "Creating an alliance.");

        int lID = GetAtomicID(lAllianceIndex, Alliances);

        Alliance alliance = new Alliance(lID, strName, strDescription, lAvatarID);
        AddAlliance(alliance, false);

        creator.SetAllianceID(lID);
        creator.SetIsAnMP(true);
    }
    
    private void RemoveExistingTreaties(int lAlliance1, int lAlliance2)
    {
        for(Treaty treaty : Treaties.values())
        {
            if((treaty.GetAllianceID1() == lAlliance1 && treaty.GetAllianceID2() == lAlliance2) ||
               (treaty.GetAllianceID1() == lAlliance2 && treaty.GetAllianceID2() == lAlliance1))
            {
                Treaties.remove(treaty.GetID());
                TreatyRemoved(treaty);
            }
        }
    }
    
    private void CreateWar(int lAllianceID1, int lAllianceID2)
    {
        RemoveExistingTreaties(lAllianceID1, lAllianceID2);

        War war = new War(GetAtomicID(lTreatyIndex, Treaties), lAllianceID1, lAllianceID2);
        AddTreaty(war);
    }
    
    private void CreateAffiliation(int lAlliance1, int lAlliance2)
    {
        RemoveExistingTreaties(lAlliance1, lAlliance2);

        Affiliation affiliation = new Affiliation(GetAtomicID(lTreatyIndex, Treaties), lAlliance1, lAlliance2);
        AddTreaty(affiliation);
    }
    
    private void CreateAffiliationRequest(int lAlliance1, int lAlliance2)
    {
        RemoveExistingTreaties(lAlliance1, lAlliance2);

        AffiliationRequest affiliation = new AffiliationRequest(GetAtomicID(lTreatyIndex, Treaties), lAlliance1, lAlliance2);
        AddTreaty(affiliation);
    }
    
    private Player CreatePlayer(String strName, int lAvatarID)
    {
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, "Creating a player.");

        Player player = new Player(GetAtomicID(lPlayerIndex, Players), config.GetPlayerBaseHP(), strName, lAvatarID, config.GetStartingWealth());
        AddPlayer(player);

        return player;
    }
    
    private void CreateMissileLaunch(GeoCoord geoPosition, byte cType, int lOwnerID, boolean bTracking, GeoCoord geoTarget, int lTargetID)
    {
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, "Creating a missile launch.");

        Missile missile = new Missile(GetAtomicID(lMissileIndex, Missiles), geoPosition, cType, lOwnerID, bTracking, geoTarget, lTargetID);
        AddMissile(missile);

        EstablishStructureThreats(missile);

        //Raise appropriate reports and attack statuses for threatened players and their allies.
        Player attacker = Players.get(missile.GetOwnerID());
        int lMissileCost = config.GetMissileCost(cType);
        attacker.AddOffenceSpending(lMissileCost);

        List<Integer> AttackedAlliances = new ArrayList<>();
        
        for(Player player : Players.values())
        {
            if(player.Functioning())
            {
                if(ThreatensPlayer(player.GetID(), geoTarget, config.GetMissileType(cType), true, true))
                {
                    //Set that player's attack status.
                    User user = player.GetUser();
                    if(user != null)
                        user.SetUnderAttack();
                    
                    CreateReport(player, new LaunchReport(String.format("%s attacked you!", attacker.GetName()), true, attacker.GetID()));

                    //Do similar for their allies.
                    int lAllianceID = player.GetAllianceMemberID();

                    if(lAllianceID != Alliance.ALLIANCE_ID_UNAFFILIATED)
                    {
                        if(!AttackedAlliances.contains(lAllianceID))
                            AttackedAlliances.add(lAllianceID);

                        for(Player otherPlayer : Players.values())
                        {
                            if(lAllianceID == otherPlayer.GetAllianceMemberID())
                            {
                                if(player != otherPlayer)
                                {
                                    User otherUser = otherPlayer.GetUser();
                                    
                                    if(otherUser != null)
                                        otherUser.SetAllyUnderAttack();
                                    
                                    CreateReport(otherPlayer, new LaunchReport(String.format("%s attacked your ally %s!", attacker.GetName(), player.GetName()), true, attacker.GetID(), player.GetID()));
                                }
                            }
                        }
                    }
                }
            }
        }

        //Scoring for wars.
        int lAttackerAllianceID = attacker.GetAllianceMemberID();
        if(lAttackerAllianceID != Alliance.ALLIANCE_ID_UNAFFILIATED)
        {
            for(Integer lAllianceID : AttackedAlliances)
            {
                for(Treaty treaty : Treaties.values())
                {
                    if(treaty instanceof War)
                    {
                        if(treaty.AreParties(lAttackerAllianceID, lAllianceID))
                        {
                            ((War)treaty).AddOffenceSpending(lAttackerAllianceID, lMissileCost);
                        }
                    }
                }
            }
        }
    }
    
    private void CreateInterceptorLaunch(GeoCoord geoPosition, byte cType, int lOwnerID, int lTargetID, boolean bPlayerLaunched)
    {
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, "Creating an interceptor launch.");
        AddInterceptor(new Interceptor(GetAtomicID(lInterceptorIndex, Interceptors), geoPosition, lOwnerID, lTargetID, cType, bPlayerLaunched));

        //Scoring
        Player player = Players.get(lOwnerID);
        Player aggressor = Players.get(Missiles.get(lTargetID).GetOwnerID());
        Scoring_DefenceSpending(player, aggressor, config.GetInterceptorCost(cType));
    }
    
    private void CreateLoot(GeoCoord geoPosition, int lValue, int lExpiry, String strDescription)
    {
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, "Creating a loot.");

        AddLoot(new Loot(GetAtomicID(lLootIndex, Loots), geoPosition, lValue, lExpiry, strDescription));
    }
    
    private void CreateRubble(GeoCoord geoPosition, int lBaseValue, String strDescription)
    {
        int lExpiry = config.GetRubbleMinTime() + (int)(random.nextFloat() * (float)((config.GetRubbleMaxTime() - config.GetRubbleMinTime()) + 0.5f));
        float fltValueProportion = config.GetRubbleMinValue() + (random.nextFloat() * (config.GetRubbleMaxValue()- config.GetRubbleMinValue()));
        int lValue = (int)((fltValueProportion * (float)lBaseValue) + 0.5f);
        
        CreateLoot(geoPosition, lValue, lExpiry, strDescription);
    }
    
    private void CreateRadiation(GeoCoord geoPosition, float fltRadius)
    {
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, "Creating radiation.");
        
        AddRadiation(new Radiation(GetAtomicID(lRadiationIndex, Radiations), geoPosition, fltRadius, config.GetMinRadiationTime() + (int)(random.nextFloat() * (float)(config.GetMaxRadiationTime() - config.GetMinRadiationTime()))));
    }
    
    public void AddUser(User user)
    {
        Users.put(user.GetIMEI(), user);
    }
    
    public void SetCompassionateInvulnerability(int lPlayerID, int lTime)
    {
        Player player = Players.get(lPlayerID);
        
        if(player != null)
        {
            //Make player invulnerable.
            player.SetCompassionateInvulnerability(lTime);
            
            //Make their structures invulnerable.
            for(Structure structure : GetAllStructures())
            {
                if(structure.GetOwnerID() == lPlayerID)
                    structure.SetRespawnProtected(true);
            }
            
            CreateReport(new LaunchReport(String.format("%s was granted compassionate invulnerability.", player.GetName()), false, lPlayerID));
        }
    }
    
    /** Protect a player from going AWOL by advancing their "last seen" time by three months.
     * @param lPlayerID The ID of the player to park. */
    public void ParkPlayer(int lPlayerID)
    {
        Player player = Players.get(lPlayerID);
        
        if(player != null)
        {
            player.Park(Defs.NINETY_DAYS);
            
            CreateAdminReport(new LaunchReport(String.format("[Admin] %s's account was parked.", player.GetName()), false, lPlayerID));
        }
    }
    
    public void TestAlert(int lPlayerID)
    {
        Player player = Players.get(lPlayerID);
        
        if(player != null)
        {
            User user = player.GetUser();
        
            if(user != null)
            {
                user.SetUnderAttack();
            }
        }
    }
    
    public boolean TestGiveShitty(int lPlayerID)
    {
        Player player = Players.get(lPlayerID);
        
        if(player != null)
        {
            if(player.GetHasCruiseMissileSystem())
            {
                MissileSystem missileSystem = player.GetMissileSystem();
                
                if(missileSystem.GetEmptySlotCount() > 0)
                {
                    for(MissileType type : config.GetMissileTypes())
                    {
                        if(type.GetName().toLowerCase().contains("shitty"))
                        {
                            missileSystem.AddMissileToNextSlot((byte)0, type.GetID(), config.GetMissilePrepTime(type));
                            CreateEvent(new LaunchEvent(String.format("%s was given a shitty test missile by the God of Launch.", player.GetName()), SoundEffect.EQUIP));
                            CreateAdminReport(new LaunchReport(String.format("[Admin] Launch God gave %s a shitty test missile.", player.GetName()), true, player.GetID()));
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
    }
    
    public boolean Approve(int lPlayerID, String strApprovar)
    {
        Player player = Players.get(lPlayerID);
        
        if(player != null)
        {
            User user = player.GetUser();
            
            if(user != null)
            {
                user.ApproveAccount();
                CreateAdminReport(new LaunchReport(String.format("[Admin] %s manually approved %s's account.", strApprovar, player.GetName()), true, player.GetID()));
                return true;
            }
        }
        
        return false;
    }
    
    public boolean RequireNewChecks(int lPlayerID, String strAdmin)
    {
        Player player = Players.get(lPlayerID);
                
        if(player != null)
        {
            User user = player.GetUser();
            
            if(user != null)
            {
                user.RequireNewChecks();
                CreateAdminReport(new LaunchReport(String.format("[Admin] %s manually required new checks on %s's account.", strAdmin, player.GetName()), true, player.GetID()));
                return true;
            }
        }
        
        return false;
    }
    
    public void CleanAvatars()
    {
        File avatarFolder = new File(Defs.LOCATION_AVATARS);
        
        for(File file : avatarFolder.listFiles())
        {
            try
            {
                int lAvatarID = Integer.parseInt(file.getName().replaceAll("\\D+",""));
                boolean bMatchFound = false;
                
                for(Player player : Players.values())
                {
                    if(player.GetAvatarID() == lAvatarID)
                    {
                        LaunchLog.ConsoleMessage(String.format("Player %s uses avatar %s", player.GetName(), file.getName()));
                        bMatchFound = true;
                        break;
                    }
                }

                for(Alliance alliance : Alliances.values())
                {
                    if(alliance.GetAvatarID() == lAvatarID)
                    {
                        LaunchLog.ConsoleMessage(String.format("Alliance %s uses avatar %s", alliance.GetName(), file.getName()));
                        bMatchFound = true;
                        break;
                    }
                }
                
                if(!bMatchFound)
                {
                    if(file.delete())
                    {
                        LaunchLog.ConsoleMessage(String.format("Deleted unused avatar %s", file.getName()));
                    }
                    else
                    {
                        LaunchLog.ConsoleMessage(String.format("Could not delete unused avatar %s!", file.getName()));
                    }
                }
            }
            catch(NumberFormatException ex)
            {
                LaunchLog.ConsoleMessage(String.format("Rogue file %s found in avatars folder!", file.getName()));
            }
        }
    }
    
    public void PurgeAvatars()
    {
        comms.InterruptAll();
        
        for(Player player : Players.values())
        {
            player.SetAvatarID(Defs.THE_GREAT_BIG_NOTHING);
        }

        for(Alliance alliance : Alliances.values())
        {
            alliance.SetAvatarID(Defs.THE_GREAT_BIG_NOTHING);
        }
        
        File avatarFolder = new File(Defs.LOCATION_AVATARS);
        
        for(File file : avatarFolder.listFiles())
        {
            try
            {
                if(!file.delete())
                {
                    LaunchLog.ConsoleMessage(String.format("Could not delete avatar %s!", file.getName()));
                }
            }
            catch(NumberFormatException ex)
            {
                LaunchLog.ConsoleMessage(String.format("Rogue file %s found in avatars folder!", file.getName()));
            }
        }
    }
    
    public void Award(int lID, int lAmount, String strReason)
    {
        Player player = Players.get(lID);
        
        if(player != null)
        {
            player.AddWealth(lAmount);
            
            String strStatement = String.format("%s was awarded £%d. Reason: %s", player.GetName(), lAmount, strReason);
            
            CreateEvent(new LaunchEvent(strStatement, SoundEffect.MONEY));
            CreateReport(new LaunchReport(strStatement, true, player.GetID()));
        }
    }
    
    public Collection<User> GetUsers() { return Users.values(); }
    
    public void CreateEvent(LaunchEvent event)
    {
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("(Event) %s", event.GetMessage()));
        comms.Announce(event);
    }
    
    /**
     * Send report only to alliance members.
     * @param alliance The alliance to which all members the report should be sent.
     * @param report The report.
     */
    public void CreateReport(Alliance alliance, LaunchReport report)
    {
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("(Report -> %s members) %s", alliance.GetName(), report.GetMessage()));
        
        for(Player player : Players.values())
        {
            if(player.GetAllianceMemberID() == alliance.GetID())
            {
                User user = player.GetUser();
                
                if(user != null)
                    user.AddReport(report);
            }
        }
    }
    
    /**
     * Send report to a single player.
     * @param singlePlayer The player to send the report to.
     * @param report The report.
     */
    public void CreateReport(Player singlePlayer, LaunchReport report)
    {
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("(Report -> %s) %s", singlePlayer.GetName(), report.GetMessage()));

        User user = singlePlayer.GetUser();

        if(user != null)
            user.AddReport(report);
    }
    
    /**
     * Send report to all players.
     * @param report Report to send to all players.
     */
    public void CreateReport(LaunchReport report)
    {
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("(Report) %s", report.GetMessage()));
        
        for(User user : Users.values())
        {
            user.AddReport(report);
        }
    }
    
    /**
     * Send report to all admins.
     * @param report Report to send to all admins.
     */
    public void CreateAdminReport(LaunchReport report)
    {
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("(Report -> Admins) %s", report.GetMessage()));
        
        for(Player player : Players.values())
        {
            if(player.GetIsAnAdmin())
            {
                User user = player.GetUser();
                
                if(user != null)
                    user.AddReport(report);
            }
        }
    }
    
    public void ForceAllianceDisbandChecks()
    {
        LaunchLog.ConsoleMessage("Forcing alliance disband checks...");
        
        for(Alliance alliance : Alliances.values())
        {
            AllianceCleanupCheck(alliance);
        }
        
        LaunchLog.ConsoleMessage("...Done.");
    }
    
    public void CleanUpUnownedEntities()
    {
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, "Cleaning up orphaned entities...");
        
        //Remove all unowned entities from the game.
        for(Alliance alliance : Alliances.values())
        {
            if(GetAllianceIsLeaderless(alliance) || GetAllianceMemberCount(alliance) == 0)
            {
                Alliances.remove(alliance.GetID());
            }
        }

        for(Treaty treaty : Treaties.values())
        {
            if(!Alliances.containsKey(treaty.GetAllianceID1()) || !Alliances.containsKey(treaty.GetAllianceID2()))
            {
                Alliances.remove(treaty.GetID());
            }
        }

        for(Missile missile : Missiles.values())
        {
            if(!Players.containsKey(missile.GetOwnerID()))
            {
                LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("Removing missile %d owned by non-existent player %d.", missile.GetID(), missile.GetOwnerID()));
                Missiles.remove(missile.GetID());
            }
        }

        for(Interceptor interceptor : Interceptors.values())
        {
            if(!Players.containsKey(interceptor.GetOwnerID()))
            {
                LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("Removing interceptor %d owned by non-existent player %d.", interceptor.GetID(), interceptor.GetOwnerID()));
                Interceptors.remove(interceptor.GetID());
            }
        }

        for(Structure structure : GetAllStructures())
        {
            if(!Players.containsKey(structure.GetOwnerID()))
            {
                LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("Destroying %s %d owned by non-existent player %d.", structure.GetTypeName(), structure.GetID(), structure.GetOwnerID()));
                structure.SetHP((short)0);
            }
        }
    }
    
    public void CleanUpOwnedEntities(int lPlayerID)
    {
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("Cleaning up entities owned by player %d...", lPlayerID));

        //Remove all entities from the game owned by a particular player.
        for(Missile missile : Missiles.values())
        {
            if(missile.GetOwnerID() == lPlayerID)
            {
                LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("Removing missile %d owned by player %d.", missile.GetID(), lPlayerID));
                Missiles.remove(missile.GetID());
            }
        }

        for(Interceptor interceptor : Interceptors.values())
        {
            if(interceptor.GetOwnerID() == lPlayerID)
            {
                LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("Removing interceptor %d owned by player %d.", interceptor.GetID(), lPlayerID));
                Interceptors.remove(interceptor.GetID());
            }
        }

        for(Structure structure : GetAllStructures())
        {
            if(structure.GetOwnerID() == lPlayerID)
            {
                LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("Destroying %s %d owned by player %d.", structure.GetTypeName(), structure.GetID(), structure.GetOwnerID()));
                structure.SetHP((short)0);
            }
        }
    }
    
    public void BlessAllNames()
    {
        LaunchLog.ConsoleMessage("Blessing all names...");
                    
        for(Player player : Players.values())
        {
            String strNameInitial = player.GetName();
            player.ChangeName(LaunchUtilities.BlessName(player.GetName()));

            if(!strNameInitial.equals(player.GetName()))
                LaunchLog.ConsoleMessage(String.format("Changed %s to %s", strNameInitial, player.GetName()));
        }

        for(Alliance alliance : Alliances.values())
        {
            String strNameInitial = alliance.GetName();
            alliance.SetName(LaunchUtilities.BlessName(alliance.GetName()));

            if(!strNameInitial.equals(alliance.GetName()))
                LaunchLog.ConsoleMessage(String.format("Changed alliance %s to %s", strNameInitial, alliance.GetName()));

            String strDescriptionInitial = alliance.GetDescription();
            alliance.SetDescription(LaunchUtilities.SanitiseText(alliance.GetDescription(), false, true));

            if(!strDescriptionInitial.equals(alliance.GetDescription()))
                LaunchLog.ConsoleMessage(String.format("Changed alliance description %s to %s", strDescriptionInitial, alliance.GetDescription()));
        }

        for(Structure structure : GetAllStructures())
        {
            String strNameInitial = structure.GetName();
            structure.SetName(LaunchUtilities.SanitiseText(structure.GetName(), true, true));

            if(!strNameInitial.equals(structure.GetName()))
                LaunchLog.ConsoleMessage(String.format("Changed structure %s to %s", strNameInitial, structure.GetName()));
        }
        
        LaunchLog.ConsoleMessage("...Done.");
    }

    public void DeletePlayer(int lPlayerID)
    {
        Player player = Players.get(lPlayerID);
        
        if(player != null)
        {
            LaunchLog.ConsoleMessage(String.format("Obliterating %s...", player.GetName()));
            
            Players.remove(lPlayerID);

            CleanUpOwnedEntities(lPlayerID);
            
            comms.InterruptAll();
            
            LaunchLog.ConsoleMessage("...boom.");
        }
    }
    
    public LaunchServerComms GetServerComms()
    {
        return comms;
    }
    
    /**
     * Check if an alliance has no active players (alive and not respawn protected) to establish if it is "defeated" for the purpose of preventing war abuse.
     * @param alliance Alliance to check
     * @return True if the alliance has no "active" players and should be disbanded.
     */
    public boolean GetAllianceHasNoActivePlayers(Alliance alliance)
    {
        for(Player player : Players.values())
        {
            if(player.GetAllianceMemberID() == alliance.GetID())
            {
                if(player.Functioning() && !player.GetRespawnProtected())
                {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    public void AllianceCleanupCheck(Alliance alliance)
    {
        boolean bDisband = false;
        String strDisbandReason = "";

        //Disband the alliance if there are no members or leaders, or they are all dead or respawn protected.
        if(GetAllianceMemberCount(alliance) == 0)
        {
            bDisband = true;
            strDisbandReason = "No remaining players";
        }
        else if(GetAllianceIsLeaderless(alliance))
        {
            bDisband = true;
            strDisbandReason = "No leader";
        }
        else if(GetAllianceHasNoActivePlayers(alliance))
        {
            bDisband = true;
            strDisbandReason = "Annihilated";
        }

        if(bDisband)
        {
            for(Player player : Players.values())
            {
                if(player.GetAllianceMemberID() == alliance.GetID())
                {
                    player.SetAllianceID(Alliance.ALLIANCE_ID_UNAFFILIATED);
                    player.SetIsAnMP(false);
                    player.SetAllianceCooloffTime(config.GetAllianceCooloffTime());
                    CreateReport(player, new LaunchReport(String.format("Your alliance %s was disbanded. Reason: %s.", alliance.GetName(), strDisbandReason), true, player.GetID()));
                }
                else
                {
                    CreateReport(player, new LaunchReport(String.format("Alliance %s disbanded. Reason: %s.", alliance.GetName(), strDisbandReason), false));
                }
            }

            for(Treaty treaty : Treaties.values())
            {
                if(treaty.IsAParty(alliance.GetID()))
                {
                    //Forfeit any wars.
                    if(treaty instanceof War)
                    {
                        ((War)treaty).Forfeit(alliance.GetID());
                        ConcludeWar((War)treaty);
                    }

                    Treaties.remove(treaty.GetID());
                }
            }

            CreateEvent(new LaunchEvent(String.format("%s disbanded.", alliance.GetName()), SoundEffect.RESPAWN));

            Alliances.remove(alliance.GetID());
            AllianceRemoved(alliance);
        }
        else
        {
            AllianceUpdated(alliance, true);
        }
    }

    //---------------------------------------------------------------------------------------------------------------------------------
    // LaunchGame inherited abstract methods.
    //---------------------------------------------------------------------------------------------------------------------------------

    @Override
    protected void MissileExploded(Missile missile)
    {
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, "A missile exploded.");
        Player owner = Players.get(missile.GetOwnerID());
        MissileType type = config.GetMissileType(missile.GetType());
        float fltBlastRadius = config.GetBlastRadius(type);
        String strCause = String.format("%s's %s missile", owner.GetName(), type.GetName());
        ProcessDamage(owner, strCause, missile.GetPosition(), fltBlastRadius, config.GetMissileMaxDamage(type));
        
        if(type.GetNuclear())
        {
            CreateRadiation(missile.GetPosition().GetCopy(), fltBlastRadius);
            CreateEMPPulse(missile.GetPosition(), fltBlastRadius * config.GetEMPRadiusMultiplier(), missile.GetOwnerID());
        }
    }

    @Override
    protected void InterceptorLostTarget(Interceptor interceptor)
    {
        Player owner = Players.get(interceptor.GetOwnerID());
        CreateEvent(new LaunchEvent(String.format("%s's interceptor lost its target.", owner.GetName()), SoundEffect.INTERCEPTOR_MISS));
    }
    
    @Override
    protected void InterceptorReachedTarget(Interceptor interceptor)
    {
        Missile missile = Missiles.get(interceptor.GetTargetID());
        Player interceptorOwner = Players.get(interceptor.GetOwnerID());
        Player missileOwner = Players.get(missile.GetOwnerID());
        
        float fltHitChance = config.GetInterceptorBaseHitChance();
        
        if(interceptor.GetPlayerLaunched())
        {
            fltHitChance += config.GetManualInterceptorChanceIncrease();
        }
        
        if(config.GetMissileType(missile.GetType()).GetECM())
        {
            fltHitChance -= config.GetECMInterceptorChanceReduction();
        }
        
        //Decide if the missile got shot down.
        if(random.nextFloat() < fltHitChance)
        {
            missile.Destroy();
            EntityRemoved(missile, false);
            
            if(interceptorOwner.ApparentlyEquals(missileOwner))
            {
                CreateEvent(new LaunchEvent(String.format("%s's interceptor shot down their own missile.", interceptorOwner.GetName()), SoundEffect.INTERCEPTOR_HIT));
            }
            else
            {
                CreateEvent(new LaunchEvent(String.format("%s's interceptor shot down %s's missile.", interceptorOwner.GetName(), missileOwner.GetName()), SoundEffect.INTERCEPTOR_HIT));
                CreateReport(interceptorOwner, new LaunchReport(String.format("Your interceptor shot down %s's missile.", missileOwner.GetName()), false, interceptorOwner.GetID(), missileOwner.GetID()));
                CreateReport(missileOwner, new LaunchReport(String.format("%s's interceptor shot down your missile.", interceptorOwner.GetName()), false, missileOwner.GetID(), interceptorOwner.GetID()));
            }
        }
        else
        {
            CreateEvent(new LaunchEvent(String.format("%s's interceptor missed %s's missile.", interceptorOwner.GetName(), missileOwner.GetName()), SoundEffect.INTERCEPTOR_MISS));
            CreateReport(interceptorOwner, new LaunchReport(String.format("Your interceptor missed %s's missile.", missileOwner.GetName()), false, interceptorOwner.GetID(), missileOwner.GetID()));
            CreateReport(missileOwner, new LaunchReport(String.format("%s's interceptor missed your missile.", interceptorOwner.GetName()), false, missileOwner.GetID(), interceptorOwner.GetID()));
        }
    }

    @Override
    protected void EntityUpdated(LaunchEntity entity, boolean bOwner)
    {
        comms.EntityUpdated(entity, bOwner);
    }

    @Override
    protected void AllianceUpdated(Alliance alliance, boolean bMajor)
    {
        comms.AllianceUpdated(alliance, bMajor);
    }

    @Override
    protected void AllianceRemoved(Alliance alliance)
    {
        comms.AllianceRemoved(alliance);
    }

    @Override
    protected void EntityRemoved(LaunchEntity entity, boolean bDontCommunicate)
    {
        if(!bDontCommunicate)
        {
            comms.EntityRemoved(entity);
        }
    }

    @Override
    protected void TreatyUpdated(Treaty treaty)
    {
        comms.TreatyCreated(treaty);
    }

    @Override
    protected void TreatyRemoved(Treaty treaty)
    {
        comms.TreatyRemoved(treaty);
    }
    
    
    //---------------------------------------------------------------------------------------------------------------------------------
    // LaunchServerGameInterface methods.
    //---------------------------------------------------------------------------------------------------------------------------------
    
    @Override
    public User VerifyID(String strIMEI)
    {
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("Verifying ID %s.", strIMEI));
        return Users.get(strIMEI);
    }

    @Override
    public int GetGameConfigChecksum()
    {
        return config.GetChecksum();
    }

    @Override
    public boolean CheckPlayerNameAvailable(String strPlayerName)
    {
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("Checking availability of %s...", strPlayerName));
        
        //Check players in game.
        for(Player player : Players.values())
        {
            if(player.GetName().equals(strPlayerName))
            {
                LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, "It's taken.");
                return false;
            }
        }
        
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, "It's available.");
        return true;
    }

    @Override
    public User CreateAccount(String strIMEI, String strPlayerName, int lAvatarID)
    {
        strPlayerName = LaunchUtilities.BlessName(strPlayerName);
        Player player = CreatePlayer(strPlayerName, lAvatarID);
        User user = new User(strIMEI, player.GetID());
        player.SetUser(user);
        Users.put(strIMEI, user);
        CreateEvent(new LaunchEvent(String.format("Give a warm, explosive welcome to %s, who has joined the Game!", player.GetName()), SoundEffect.RESPAWN));
        CreateReport(new LaunchReport(String.format("Give a warm, explosive welcome to %s, who has joined the Game!", player.GetName()), false, player.GetID()));
        
        return user;
    }

    @Override
    public void UpdatePlayerLocation(int lPlayerID, LaunchClientLocation location)
    {
        Player player = Players.get(lPlayerID);
        
        if(player != null)
        {
            LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("Updating %s's location.", player.GetName()));
        
            long oPrevTime = player.GetLastSeen();
            player.SetLastSeen();
            
            if(!player.Destroyed())
            {
                GeoCoord geoPrevPosition = player.GetPosition().GetCopy();
                player.SetPosition(location.GetGeoCoord());
                UpdateTrackingMissileThreats(lPlayerID);
                
                if(player.GetLastSeen() - oPrevTime < Defs.ON_THE_MOVE_TIME_THRESHOLD)
                {
                    //Player seems to be on the move. Process the update in steps so they don't "miss" things.
                    while(geoPrevPosition.MoveToward(player.GetPosition(), Defs.ON_THE_MOVE_STEP_DISTANCE) == false)
                    {
                        ProcessPlayerLocationIteration(player, geoPrevPosition);
                    }
                }

                ProcessPlayerLocationIteration(player, player.GetPosition());
            }
            
            if(player.GetAWOL())
            {
                player.SetAWOL(false);
                CreateEvent(new LaunchEvent(String.format("%s is back!", player.GetName())));
            }
        }
    }
    
    /**
     * Process a single player location. This function allows either single or "linear" changes in player location, so they can collect and repair while on the move even if they "miss".
     */
    private void ProcessPlayerLocationIteration(Player player, GeoCoord geoPlayer)
    {
        //Process loot collection.
        for(Loot loot : Loots.values())
        {
            if(geoPlayer.BroadPhaseCollisionTest(loot.GetPosition()))
            {
                if(geoPlayer.DistanceTo(loot.GetPosition()) <= config.GetRepairSalvageDistance())
                {
                    if(!loot.Collected())
                    {
                        loot.Collect();
                        player.AddWealth(loot.GetValue());
                        comms.EntityRemoved(loot);
                        CreateEvent(new LaunchEvent(String.format("%s collected %s worth £%d.", player.GetName(), loot.GetDescription(), loot.GetValue()), SoundEffect.MONEY));

                        //Log for locations.
                        LaunchLog.Log(LOCATIONS, player.GetName(), String.format("Collected £%d at (%.6f, %.6f) - %s", loot.GetValue(), loot.GetPosition().GetLatitude(), loot.GetPosition().GetLongitude(), loot.GetDescription()));
                    }
                }
            }
        }

        //Process repairs.
        for(Structure structure : GetAllStructures())
        {
            if(!structure.AtFullHealth())
            {
                if(geoPlayer.BroadPhaseCollisionTest(structure.GetPosition()))
                {
                    Player owner = Players.get(structure.GetOwnerID());

                    if(WouldBeFriendlyFire(player, owner))
                    {
                        if(geoPlayer.DistanceTo(structure.GetPosition()) <= config.GetRepairSalvageDistance())
                        {
                            structure.FullyRepair();

                            if(player == owner)
                            {
                                CreateEvent(new LaunchEvent(String.format("%s repaired a %s.", player.GetName(), structure.GetTypeName()), SoundEffect.REPAIR));

                                //Log for locations.
                                LaunchLog.Log(LOCATIONS, player.GetName(), String.format("Repaired a %s at (%.6f, %.6f)", structure.GetTypeName(), structure.GetPosition().GetLatitude(), structure.GetPosition().GetLongitude()));
                            }
                            else
                            {
                                CreateEvent(new LaunchEvent(String.format("%s repaired %s's %s.", player.GetName(), owner.GetName(), structure.GetTypeName()), SoundEffect.REPAIR));

                                //Log for locations.
                                LaunchLog.Log(LOCATIONS, player.GetName(), String.format("Repaired %s's %s at (%.6f, %.6f)", owner.GetName(), structure.GetTypeName(), structure.GetPosition().GetLatitude(), structure.GetPosition().GetLongitude()));
                            }
                        }
                    }
                }
            }
        }
    }
    
    private boolean ValidateConstructionRequest(Player player, String structureName, int lCost)
    {
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("Validating %s's %s construction attempt...", player.GetName(), structureName));
        
        if(player.Functioning())
        {
            if(GetNearbyStructures(player).isEmpty())
            {
                if(player.SubtractWealth(lCost))
                {
                    return true;
                }
                else
                {
                    LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, "...Invalid. Unaffordable.");
                }
            }
            else
            {
                LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, "...Invalid. Nearby structures.");
            }
        }
        else
        {
            LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, "...Invalid. Player is not functioning.");
        }
        
        return false;
    }

    @Override
    public boolean ConstructMissileSite(int lPlayerID, boolean bNuclear)
    {
        Player player = Players.get(lPlayerID);
        int lCost = bNuclear? config.GetNukeCMSStructureCost() : config.GetCMSStructureCost();
        
        if(ValidateConstructionRequest(player, bNuclear? "nuclear missile site" : "missile site", lCost))
        {
            LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, "...Affordable...");

            MissileSite missileSite = new MissileSite(GetAtomicID(lMissileSiteIndex, MissileSites), player.GetPosition().GetCopy(), config.GetStructureBaseHP(), config.GetStructureBaseHP(), player.GetID(), player.GetRespawnProtected(), config.GetStructureBootTime(), config.GetReloadTimeBase(), config.GetInitialMissileSlots(), bNuclear, CHARGE_INTERVAL);
            AddMissileSite(missileSite);
            EstablishStructureThreats(missileSite);

            if(bNuclear)
            {
                CreateEvent(new LaunchEvent(String.format("%s constructed a nuclear missile site.", player.GetName()), SoundEffect.CONSTRUCTION));
                RemoveRespawnProtection(player);

                //Notify nearby users.
                for(Player otherPlayer : Players.values())
                {
                    switch(GetAllegiance(player, otherPlayer))
                    {
                        case NEUTRAL:
                        case ENEMY:
                        case PENDING_TREATY:
                        {
                            if(player.GetPosition().DistanceTo(otherPlayer.GetPosition()) < config.GetNuclearEscalationRadius())
                            {
                                User user = otherPlayer.GetUser();
                                
                                if(user != null)
                                    user.SetNuclearEscalation();
                                
                                CreateReport(otherPlayer, new LaunchReport(String.format("Unfriendly player %s constructed a nuclear missile silo within EMP attack range of you.", player.GetName()), true, player.GetID()));
                            }
                        }
                        break;
                    }
                }
            }
            else
            {
                CreateEvent(new LaunchEvent(String.format("%s constructed a missile site.", player.GetName()), SoundEffect.CONSTRUCTION));
            }

            return true;
        }
        
        return false;
    }

    @Override
    public boolean ConstructSAMSite(int lPlayerID)
    {
        Player player = Players.get(lPlayerID);
        
        if(ValidateConstructionRequest(player, "SAM site", config.GetSAMStructureCost()))
        {
            SAMSite samSite = new SAMSite(GetAtomicID(lSAMSiteIndex, SAMSites), player.GetPosition().GetCopy(), config.GetStructureBaseHP(), config.GetStructureBaseHP(), player.GetID(), player.GetRespawnProtected(), config.GetStructureBootTime(), config.GetReloadTimeBase(), config.GetInitialInterceptorSlots(), CHARGE_INTERVAL);
            AddSAMSite(samSite);
            EstablishStructureThreats(samSite);

            CreateEvent(new LaunchEvent(String.format("%s constructed a SAM site.", player.GetName()), SoundEffect.CONSTRUCTION));

            return true;
        }
        
        return false;
    }

    @Override
    public boolean ConstructSentryGun(int lPlayerID)
    {
        Player player = Players.get(lPlayerID);
        
        if(ValidateConstructionRequest(player, "sentry gun", config.GetSentryGunStructureCost()))
        {
            SentryGun sentryGun = new SentryGun(GetAtomicID(lSentryGunIndex, SentryGuns), player.GetPosition().GetCopy(), config.GetStructureBaseHP(), config.GetStructureBaseHP(), player.GetID(), player.GetRespawnProtected(), config.GetStructureBootTime(), CHARGE_INTERVAL);
            AddSentryGun(sentryGun);
            EstablishStructureThreats(sentryGun);

            CreateEvent(new LaunchEvent(String.format("%s constructed a sentry gun.", player.GetName()), SoundEffect.CONSTRUCTION));

            return true;
        }
        
        return false;
    }

    @Override
    public boolean ConstructOreMine(int lPlayerID)
    {
        Player player = Players.get(lPlayerID);
        
        if(ValidateConstructionRequest(player, "ore mine", config.GetOreMineStructureCost()))
        {
            OreMine oreMine = new OreMine(GetAtomicID(lOreMineIndex, OreMines), player.GetPosition().GetCopy(), config.GetStructureBaseHP(), config.GetStructureBaseHP(), player.GetID(), player.GetRespawnProtected(), config.GetStructureBootTime(), CHARGE_INTERVAL, config.GetOreMineGenerateTime());
            AddOreMine(oreMine);
            EstablishStructureThreats(oreMine);

            CreateEvent(new LaunchEvent(String.format("%s constructed an ore mine.", player.GetName()), SoundEffect.CONSTRUCTION));

            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean PurchaseMissiles(int lPlayerID, int lMissileSiteID, byte cSlotNo, byte[] cMissileTypes)
    {
        Player player = Players.get(lPlayerID);
        
        if(player != null)
        {
            LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s is attempting to purchase missiles...", player.GetName()));
            
            if(player.Functioning())
            {
                MissileSite missileSite;
                MissileSystem missileSystem = null;
                boolean bNukesAcceptable = false;

                if(lMissileSiteID == Defs.PLAYER_CARRIED)
                {
                    if(player.GetHasCruiseMissileSystem())
                        missileSystem = player.GetMissileSystem();
                }
                else
                {
                    missileSite = MissileSites.get(lMissileSiteID);
                    if(missileSite != null)
                    {
                        if(missileSite.GetOwnerID() == lPlayerID)
                        {
                            missileSystem = missileSite.GetMissileSystem();
                            bNukesAcceptable = missileSite.CanTakeNukes();
                        }
                    }
                }
                
                if(missileSystem != null)
                {
                    if(missileSystem.GetEmptySlotCount() >= cMissileTypes.length)
                    {
                        for(byte cType : cMissileTypes)
                        {
                            MissileType type = config.GetMissileType(cType);

                            if(type != null)
                            {
                                if((!type.GetNuclear() || bNukesAcceptable) && type.GetPurchasable())
                                {
                                    if(player.SubtractWealth(config.GetMissileCost(type)))
                                    {
                                        missileSystem.AddMissileToNextSlot(cSlotNo, cType, config.GetMissilePrepTime(type));
                                        
                                        if(type.GetNuclear())
                                        {
                                            RemoveRespawnProtection(player);
                                        }
                                    }
                                }
                            }
                        }

                        missileSystem.CompleteMultiPurchase();
                        CreateEvent(new LaunchEvent(String.format("%s acquired missiles.", player.GetName()), SoundEffect.EQUIP));
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    @Override
    public boolean PurchaseInterceptors(int lPlayerID, int lSAMSiteID, byte cSlotNo, byte[] cInterceptorTypes)
    {
        Player player = Players.get(lPlayerID);
        
        if(player != null)
        {
            LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s is attempting to purchase interceptor...", player.GetName()));
            
            if(player.Functioning())
            {
                SAMSite samSite;
                MissileSystem interceptorSystem = null;

                if(lSAMSiteID == Defs.PLAYER_CARRIED)
                {
                    if(player.GetHasAirDefenceSystem())
                        interceptorSystem = player.GetInterceptorSystem();
                }
                else
                {
                    samSite = SAMSites.get(lSAMSiteID);
                    if(samSite != null)
                    {
                        if(samSite.GetOwnerID() == lPlayerID)
                            interceptorSystem = samSite.GetInterceptorSystem();
                    }
                }
                
                if(interceptorSystem != null)
                {
                    if(interceptorSystem.GetEmptySlotCount() >= cInterceptorTypes.length)
                    {
                        for(byte cType : cInterceptorTypes)
                        {
                            InterceptorType type = config.GetInterceptorType(cType);

                            if(type != null)
                            {
                                if(type.GetPurchasable())
                                {
                                    if(player.SubtractWealth(config.GetInterceptorCost(type)))
                                    {
                                        interceptorSystem.AddMissileToNextSlot(cSlotNo, cType, config.GetInterceptorPrepTime(type));
                                    }
                                }
                            }
                        }

                        interceptorSystem.CompleteMultiPurchase();
                        CreateEvent(new LaunchEvent(String.format("%s acquired interceptors.", player.GetName()), SoundEffect.EQUIP));
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    @Override
    public boolean MissileSlotUpgrade(int lPlayerID, int lMissileSiteID)
    {
        Player player = Players.get(lPlayerID);
        MissileSite missileSite = MissileSites.get(lMissileSiteID);
        MissileSystem missileSystem = missileSite.GetMissileSystem();
        
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s is attempting to upgrade missile slots...", player.GetName()));
        
        if(player.Functioning() && ((int)missileSystem.GetSlotCount() + (int)config.GetMissileUpgradeCount()) <= Defs.MAX_MISSILE_SLOTS)
        {
            if(player.SubtractWealth(GetMissileSlotUpgradeCost(missileSystem, config.GetInitialMissileSlots())))
            {
                missileSystem.IncreaseSlotCount(config.GetMissileUpgradeCount());
                CreateEvent(new LaunchEvent(String.format("%s upgraded missile site slots.", player.GetName()), SoundEffect.EQUIP));
                return true;
            }
        }
        
        return false;
    }

    @Override
    public boolean InterceptorSlotUpgrade(int lPlayerID, int lSAMSiteID)
    {
        Player player = Players.get(lPlayerID);
        SAMSite samSite = SAMSites.get(lSAMSiteID);
        MissileSystem missileSystem = samSite.GetInterceptorSystem();
        
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s is attempting to upgrade interceptor slots...", player.GetName()));
        
        if(player.Functioning() && ((int)missileSystem.GetSlotCount() + (int)config.GetMissileUpgradeCount()) <= Defs.MAX_MISSILE_SLOTS)
        {
            if(player.SubtractWealth(GetMissileSlotUpgradeCost(missileSystem, config.GetInitialInterceptorSlots())))
            {
                missileSystem.IncreaseSlotCount(config.GetMissileUpgradeCount());
                CreateEvent(new LaunchEvent(String.format("%s upgraded SAM site slots.", player.GetName()), SoundEffect.EQUIP));
                return true;
            }
        }
        
        return false;
    }

    @Override
    public boolean PlayerMissileSlotUpgrade(int lPlayerID)
    {
        Player player = Players.get(lPlayerID);
        
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s is attempting to upgrade player missile slots...", player.GetName()));
        
        if(player.GetHasCruiseMissileSystem() && player.Functioning())
        {
            MissileSystem missileSystem = player.GetMissileSystem();
            
            if(((int)missileSystem.GetSlotCount() + (int)config.GetMissileUpgradeCount()) <= Defs.MAX_MISSILE_SLOTS)
            {
                if(player.SubtractWealth(GetMissileSlotUpgradeCost(missileSystem, config.GetInitialMissileSlots())))
                {
                    missileSystem.IncreaseSlotCount(config.GetMissileUpgradeCount());
                    CreateEvent(new LaunchEvent(String.format("%s upgraded missile slots.", player.GetName()), SoundEffect.EQUIP));
                    return true;
                }
            }
        }
        
        return false;
    }

    @Override
    public boolean PlayerInterceptorSlotUpgrade(int lPlayerID)
    {
        Player player = Players.get(lPlayerID);
        
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s is attempting to upgrade player interceptor slots...", player.GetName()));
        
        if(player.GetHasAirDefenceSystem() && player.Functioning())
        {
            MissileSystem missileSystem = player.GetInterceptorSystem();
            
            if(((int)missileSystem.GetSlotCount() + (int)config.GetMissileUpgradeCount()) <= Defs.MAX_MISSILE_SLOTS)
            {
                if(player.SubtractWealth(GetMissileSlotUpgradeCost(missileSystem, config.GetInitialInterceptorSlots())))
                {
                    missileSystem.IncreaseSlotCount(config.GetMissileUpgradeCount());
                    CreateEvent(new LaunchEvent(String.format("%s upgraded interceptor slots.", player.GetName()), SoundEffect.EQUIP));
                    return true;
                }
            }
        }
        
        return false;
    }

    @Override
    public boolean MissileReloadUpgrade(int lPlayerID, int lMissileSiteID)
    {
        Player player = Players.get(lPlayerID);
        MissileSite missileSite = MissileSites.get(lMissileSiteID);
        
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s is attempting to upgrade missile reload time...", player.GetName()));
        
        if(missileSite.GetOwnerID() == lPlayerID && player.Functioning())
        {
            MissileSystem system = missileSite.GetMissileSystem();
            int lCost = GetReloadUpgradeCost(system);
            
            if(lCost != Defs.UPGRADE_COST_MAXED)
            {
                if(player.GetWealth() >= lCost)
                {
                    player.SubtractWealth(lCost);
                    system.SetReloadTime(GetReloadUpgradeTime(system));
                    CreateEvent(new LaunchEvent(String.format("%s upgraded missile site reload time.", player.GetName()), SoundEffect.EQUIP));
                    return true;
                }
            }
        }
        
        return false;
    }

    @Override
    public boolean InterceptorReloadUpgrade(int lPlayerID, int lSAMSiteID)
    {
        Player player = Players.get(lPlayerID);
        SAMSite samSite = SAMSites.get(lSAMSiteID);
        
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s is attempting to upgrade intercetpor reload time...", player.GetName()));
        
        if(samSite.GetOwnerID() == lPlayerID && player.Functioning())
        {
            MissileSystem system = samSite.GetInterceptorSystem();
            int lCost = GetReloadUpgradeCost(system);
            
            if(lCost != Defs.UPGRADE_COST_MAXED)
            {
                if(player.GetWealth() >= lCost)
                {
                    player.SubtractWealth(lCost);
                    system.SetReloadTime(GetReloadUpgradeTime(system));
                    CreateEvent(new LaunchEvent(String.format("%s upgraded SAM site reload time.", player.GetName()), SoundEffect.EQUIP));
                    return true;
                }
            }
        }
        
        return false;
    }

    @Override
    public boolean PlayerMissileReloadUpgrade(int lPlayerID)
    {
        Player player = Players.get(lPlayerID);
        
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s is attempting to upgrade player missile reload time...", player.GetName()));
        
        if(player.GetHasCruiseMissileSystem() && player.Functioning())
        {
            MissileSystem system = player.GetMissileSystem();
            int lCost = GetReloadUpgradeCost(system);
            
            if(lCost != Defs.UPGRADE_COST_MAXED)
            {
                if(player.GetWealth() >= lCost)
                {
                    player.SubtractWealth(lCost);
                    system.SetReloadTime(GetReloadUpgradeTime(system));
                    CreateEvent(new LaunchEvent(String.format("%s upgraded missile system reload time.", player.GetName()), SoundEffect.EQUIP));
                    return true;
                }
            }
        }
        
        return false;
    }

    @Override
    public boolean PlayerInterceptorReloadUpgrade(int lPlayerID)
    {
        Player player = Players.get(lPlayerID);
        
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s is attempting to upgrade player interceptor reload time...", player.GetName()));
        
        if(player.GetHasAirDefenceSystem() && player.Functioning())
        {
            MissileSystem system = player.GetInterceptorSystem();
            int lCost = GetReloadUpgradeCost(system);
            
            if(lCost != Defs.UPGRADE_COST_MAXED)
            {
                if(player.GetWealth() >= lCost)
                {
                    player.SubtractWealth(lCost);
                    system.SetReloadTime(GetReloadUpgradeTime(system));
                    CreateEvent(new LaunchEvent(String.format("%s upgraded interceptor system reload time.", player.GetName()), SoundEffect.EQUIP));
                    return true;
                }
            }
        }
        
        return false;
    }

    @Override
    public boolean LaunchMissile(int lPlayerID, int lSiteID, byte cSlotNo, boolean bTracking, float fltTargetLatitude, float fltTargetLongitude, int lTargetID)
    {
        Player player = Players.get(lPlayerID);
        MissileSite missileSite = MissileSites.get(lSiteID);
        
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s is attempting to launch a missile...", player.GetName()));
        
        if(missileSite != null)
        {
            if(missileSite.GetOwnerID() == lPlayerID)
            {
                if(missileSite.GetOnline() && player.Functioning())
                {
                    MissileSystem missileSystem = missileSite.GetMissileSystem();

                    if(missileSystem.GetSlotReadyToFire(cSlotNo))
                    {
                        byte cTypeID = missileSystem.GetSlotMissileType(cSlotNo);
                        MissileType type = config.GetMissileType(cTypeID);
                        GeoCoord geoTarget = new GeoCoord(fltTargetLatitude, fltTargetLongitude);

                        if(!ThreatensPlayer(lPlayerID, geoTarget, type, false, false))
                        {
                            if(!ThreatensFriendlies(lPlayerID, geoTarget, type, false, false))
                            {
                                //Remove respawn protection from the player.
                                RemoveRespawnProtection(player);

                                missileSystem.Fire(cSlotNo);
                                CreateMissileLaunch(missileSite.GetPosition().GetCopy(), cTypeID, lPlayerID, bTracking, geoTarget, lTargetID);
                                CreateEvent(new LaunchEvent(String.format("%s launched %s missile.", player.GetName(), type.GetName()), SoundEffect.MISSILE_LAUNCH));
                                return true;
                            }
                        }
                    }
                }
            }
        }
        
        return false;
    }

    @Override
    public boolean LaunchPlayerMissile(int lPlayerID, byte cSlotNo, boolean bTracking, float fltTargetLatitude, float fltTargetLongitude, int lTargetID)
    {
        Player player = Players.get(lPlayerID);
        
        if(player != null)
        {
            LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s is attempting to launch a player missile...", player.GetName()));
        
            if(player.GetHasCruiseMissileSystem() && player.Functioning())
            {
                MissileSystem missileSystem = player.GetMissileSystem();
                
                if(missileSystem.GetSlotReadyToFire(cSlotNo))
                {
                    byte cTypeID = missileSystem.GetSlotMissileType(cSlotNo);
                    MissileType type = config.GetMissileType(cTypeID);
                    GeoCoord geoTarget = new GeoCoord(fltTargetLatitude, fltTargetLongitude);
                    
                    if(!ThreatensPlayer(lPlayerID, geoTarget, type, false, false))
                    {
                        if(!ThreatensFriendlies(lPlayerID, geoTarget, type, false, false))
                        {
                            //Remove respawn protection from the player.
                            RemoveRespawnProtection(player);

                            missileSystem.Fire(cSlotNo);
                            CreateMissileLaunch(player.GetPosition().GetCopy(), cTypeID, lPlayerID, bTracking, geoTarget, lTargetID);
                            CreateEvent(new LaunchEvent(String.format("%s launched %s missile.", player.GetName(), type.GetName()), SoundEffect.MISSILE_LAUNCH));
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
    }

    @Override
    public boolean LaunchInterceptor(int lPlayerID, int lSiteID, byte cSlotNo, int lTargetID)
    {
        Player player = Players.get(lPlayerID);
        SAMSite samSite = SAMSites.get(lSiteID);
        
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s is attempting to launch an interceptor...", player.GetName()));
        
        if(samSite != null)
        {
            if(samSite.GetOwnerID() == lPlayerID)
            {
                if(samSite.GetOnline() && player.Functioning())
                {
                    MissileSystem missileSystem = samSite.GetInterceptorSystem();

                    if(missileSystem.GetSlotReadyToFire(cSlotNo))
                    {
                        byte cTypeID = missileSystem.GetSlotMissileType(cSlotNo);
                        InterceptorType type = config.GetInterceptorType(cTypeID);
                        Missile targetMissile = Missiles.get(lTargetID);
                        Player targetPlayer = Players.get(targetMissile.GetOwnerID());
                        MissileType targetType = config.GetMissileType(targetMissile.GetType());

                        //Since this is a player's interceptor launch; it's deliberately stupid, and doesn't care if it'll intercept or not.
                        if(config.GetInterceptorRange(type.GetRangeIndex()) >= samSite.GetPosition().DistanceTo(targetMissile.GetPosition()))
                        {
                            //Remove respawn protection from the player.
                            RemoveRespawnProtection(player);

                            missileSystem.Fire(cSlotNo);
                            CreateInterceptorLaunch(samSite.GetPosition().GetCopy(), cTypeID, lPlayerID, lTargetID, true);
                            CreateEvent(new LaunchEvent(String.format("%s launched an interceptor at %s's %s.", player.GetName(), targetPlayer.GetName(), targetType.GetName()), SoundEffect.INTERCEPTOR_LAUNCH));
                            CreateReport(targetPlayer, new LaunchReport(String.format("%s launched an interceptor at your missile.", player.GetName()), true, player.GetID(), targetPlayer.GetID()));
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
    }

    @Override
    public boolean LaunchPlayerInterceptor(int lPlayerID, byte cSlotNo, int lTargetID)
    {
        Player player = Players.get(lPlayerID);
        
        if(player != null)
        {
            LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s is attempting to launch a player interceptor...", player.GetName()));
        
            if(player.GetHasAirDefenceSystem() && player.Functioning())
            {
                MissileSystem missileSystem = player.GetInterceptorSystem();
                
                if(missileSystem.GetSlotReadyToFire(cSlotNo))
                {
                    byte cTypeID = missileSystem.GetSlotMissileType(cSlotNo);
                    InterceptorType type = config.GetInterceptorType(cTypeID);
                    Missile targetMissile = Missiles.get(lTargetID);
                        Player targetPlayer = Players.get(targetMissile.GetOwnerID());
                        MissileType targetType = config.GetMissileType(targetMissile.GetType());
                    
                    //Since this is a player's interceptor launch; it's deliberately stupid, and doesn't care if it'll intercept or not.
                    if(config.GetInterceptorRange(type.GetRangeIndex()) >= player.GetPosition().DistanceTo(targetMissile.GetPosition()))
                    {
                        //Remove respawn protection from the player.
                        RemoveRespawnProtection(player);

                        missileSystem.Fire(cSlotNo);
                        CreateInterceptorLaunch(player.GetPosition().GetCopy(), cTypeID, lPlayerID, lTargetID, true);
                        CreateEvent(new LaunchEvent(String.format("%s launched an interceptor at %s's %s.", player.GetName(), targetPlayer.GetName(), targetType.GetName()), SoundEffect.INTERCEPTOR_LAUNCH));
                        CreateReport(targetPlayer, new LaunchReport(String.format("%s launched an interceptor at your missile.", player.GetName()), true, player.GetID(), targetPlayer.GetID()));
                        return true;
                    }
                }
            }
        }
        
        return false;
    }

    @Override
    public boolean PurchaseMissileSystem(int lPlayerID)
    {
        Player player = Players.get(lPlayerID);
        
        if(player != null)
        {
            LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s is attempting to purchase a missile system...", player.GetName()));
            
            if((!player.GetHasCruiseMissileSystem()) && player.Functioning())
            {
                if(player.SubtractWealth(config.GetCMSSystemCost()))
                {
                    player.AddMissileSystem(config.GetReloadTimeBase(), config.GetInitialMissileSlots());
                    CreateEvent(new LaunchEvent(String.format("%s acquired a missile system.", player.GetName()), SoundEffect.EQUIP));
                    return true;
                }
            }
        }
        
        return false;
    }

    @Override
    public boolean PurchaseSAMSystem(int lPlayerID)
    {
        Player player = Players.get(lPlayerID);
        
        if(player != null)
        {
            LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s is attempting to purchase a SAM system...", player.GetName()));
            
            if((!player.GetHasAirDefenceSystem()) && player.Functioning())
            {
                if(player.SubtractWealth(config.GetSAMSystemCost()))
                {
                    player.AddInterceptorSystem(config.GetReloadTimeBase(), config.GetInitialInterceptorSlots());
                    CreateEvent(new LaunchEvent(String.format("%s acquired an air defence system.", player.GetName()), SoundEffect.EQUIP));
                    return true;
                }
            }
        }
        
        return false;
    }

    @Override
    public boolean Respawn(int lPlayerID)
    {
        Player player = Players.get(lPlayerID);
        
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s is attempting to respawn...", player.GetName()));
        
        if(player.Destroyed() && player.GetCanRespawn())
        {
            player.Respawn(config.GetPlayerBaseHP(), config.GetRespawnProtectionTime());
            player.AddWealth(config.GetRespawnWealth());
            CreateEvent(new LaunchEvent(String.format("%s respawned.", player.GetName()), SoundEffect.RESPAWN));
            return true;
        }
        
        return false;
    }
    
    private boolean SellStructure(int lPlayerID, Structure structure)
    {
        Player player = Players.get(lPlayerID);
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s is attempting to sell a %s...", player.GetName(), structure.GetTypeName()));
        
        if((!structure.GetSelling()) && player.Functioning())
        {
            structure.Sell(config.GetDecommissionTime());
            CreateEvent(new LaunchEvent(String.format("%s is decommissioning a %s.", player.GetName(), structure.GetTypeName())));
            return true;
        }
        
        return false;
    }

    @Override
    public boolean SellMissileSite(int lPlayerID, int lMissileSiteID)
    {
        return SellStructure(lPlayerID, MissileSites.get(lMissileSiteID));
    }

    @Override
    public boolean SellSAMSite(int lPlayerID, int lSAMSiteID)
    {
        return SellStructure(lPlayerID, SAMSites.get(lSAMSiteID));
    }

    @Override
    public boolean SellSentryGun(int lPlayerID, int lSentryGunID)
    {
        return SellStructure(lPlayerID, SentryGuns.get(lSentryGunID));
    }

    @Override
    public boolean SellOreMine(int lPlayerID, int lOreMineID)
    {
        return SellStructure(lPlayerID, OreMines.get(lOreMineID));
    }

    @Override
    public boolean SellMissileSystem(int lPlayerID)
    {
        Player player = Players.get(lPlayerID);
        
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s is attempting to sell a missile system...", player.GetName()));
        
        if(player.GetHasCruiseMissileSystem() && player.Functioning())
        {
            int lValue = GetSaleValue(player.GetMissileSystem(), true);
            player.RemoveMissileSystem(lValue);
            CreateEvent(new LaunchEvent(String.format("%s sold their missile system.", player.GetName()), SoundEffect.MONEY));
            return true;
        }
        
        return false;
    }

    @Override
    public boolean SellSAMSystem(int lPlayerID)
    {
        Player player = Players.get(lPlayerID);
        
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s is attempting to sell a SAM system...", player.GetName()));
        
        if(player.GetHasAirDefenceSystem() && player.Functioning())
        {
            int lValue = GetSaleValue(player.GetInterceptorSystem(), false);
            player.RemoveAirDefenceSystem(lValue);
            CreateEvent(new LaunchEvent(String.format("%s sold their air defence system.", player.GetName()), SoundEffect.MONEY));
            return true;
        }
        
        return false;
    }

    @Override
    public boolean SellMissile(int lPlayerID, int lMissileSiteID, byte cSlotIndex)
    {
        Player player = Players.get(lPlayerID);
        
        if(player != null)
        {
            LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s is attempting to sell a missile...", player.GetName()));
            
            if(player.Functioning())
            {
                MissileSite missileSite;
                MissileSystem missileSystem = null;

                if(lMissileSiteID == Defs.PLAYER_CARRIED)
                {
                    if(player.GetHasCruiseMissileSystem())
                        missileSystem = player.GetMissileSystem();
                }
                else
                {
                    missileSite = MissileSites.get(lMissileSiteID);
                    if(missileSite != null)
                    {
                        if(missileSite.GetOwnerID() == lPlayerID)
                            missileSystem = missileSite.GetMissileSystem();
                    }
                }
                
                if(missileSystem != null)
                {
                    if(missileSystem.GetSlotHasMissile(cSlotIndex))
                    {
                        MissileType type = config.GetMissileType(missileSystem.GetSlotMissileType(cSlotIndex));
                        missileSystem.UnloadSlot(cSlotIndex);
                        player.AddWealth(GetSaleValue(config.GetMissileCost(type)));
                        CreateEvent(new LaunchEvent(String.format("%s sold a missile.", player.GetName()), SoundEffect.MONEY));
                        
                        return true;
                    }
                }
            }
        }
        
        return false;
    }

    @Override
    public boolean SellInterceptor(int lPlayerID, int lSAMSiteID, byte cSlotIndex)
    {
        Player player = Players.get(lPlayerID);
        
        if(player != null)
        {
            LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s is attempting to sell an interceptor...", player.GetName()));
            
            if(player.Functioning())
            {
                SAMSite samSite;
                MissileSystem interceptorSystem = null;

                if(lSAMSiteID == Defs.PLAYER_CARRIED)
                {
                    if(player.GetHasAirDefenceSystem())
                        interceptorSystem = player.GetInterceptorSystem();
                }
                else
                {
                    samSite = SAMSites.get(lSAMSiteID);
                    if(samSite != null)
                    {
                        if(samSite.GetOwnerID() == lPlayerID)
                            interceptorSystem = samSite.GetInterceptorSystem();
                    }
                }
                
                if(interceptorSystem != null)
                {
                    if(interceptorSystem.GetSlotHasMissile(cSlotIndex))
                    {
                        InterceptorType type = config.GetInterceptorType(interceptorSystem.GetSlotMissileType(cSlotIndex));
                        interceptorSystem.UnloadSlot(cSlotIndex);
                        player.AddWealth(GetSaleValue(config.GetInterceptorCost(type)));
                        CreateEvent(new LaunchEvent(String.format("%s sold an interceptor.", player.GetName()), SoundEffect.MONEY));
                        
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    
    private boolean StructureOnlineOffline(int lPlayerID, Structure structure, int lMaintenanceCost, boolean bOnline)
    {
        Player player = Players.get(lPlayerID);
        
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s is attempting to set %s %d %s...", player.GetName(), structure.GetTypeName(), structure.GetID(), bOnline? "online" : "offline"));
        
        if(structure.GetOwnerID() == player.GetID() && player.Functioning())
        {
            if(bOnline)
            {
                if(player.GetWealth() >= lMaintenanceCost)
                {
                    LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, "...Brought online.");
                    structure.BringOnline(config.GetStructureBootTime());
                    return true;
                }
            }
            else
            {
                LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, "...Taken offline.");
                structure.TakeOffline();
                return true;
            }
        }
        
        return false;
    }

    @Override
    public boolean SetMissileSitesOnOff(int lPlayerID, List<Integer> SiteIDs, boolean bOnline)
    {
        boolean bSuccess = true;
        
        for(Integer lSiteID : SiteIDs)
        {
            MissileSite missileSite = MissileSites.get(lSiteID);
            bSuccess = bSuccess && StructureOnlineOffline(lPlayerID, missileSite, config.GetMissileSiteMaintenanceCost(), bOnline);
        }
        
        return bSuccess;
    }

    @Override
    public boolean SetSAMSitesOnOff(int lPlayerID, List<Integer> SiteIDs, boolean bOnline)
    {
        boolean bSuccess = true;
        
        for(Integer lSiteID : SiteIDs)
        {
            SAMSite samSite = SAMSites.get(lSiteID);
            bSuccess = bSuccess && StructureOnlineOffline(lPlayerID, samSite, config.GetSAMSiteMaintenanceCost(), bOnline);
        }
        
        return bSuccess;
    }

    @Override
    public boolean SetSentryGunsOnOff(int lPlayerID, List<Integer> SiteIDs, boolean bOnline)
    {
        boolean bSuccess = true;
        
        for(Integer lSiteID : SiteIDs)
        {
            SentryGun sentryGun = SentryGuns.get(lSiteID);
            bSuccess = bSuccess && StructureOnlineOffline(lPlayerID, sentryGun, config.GetSentryGunMaintenanceCost(), bOnline);
        }
        
        return bSuccess;
    }

    @Override
    public boolean SetOreMinesOnOff(int lPlayerID, List<Integer> SiteIDs, boolean bOnline)
    {
        boolean bSuccess = true;
        
        for(Integer lSiteID : SiteIDs)
        {
            OreMine oreMine = OreMines.get(lSiteID);
            bSuccess = bSuccess && StructureOnlineOffline(lPlayerID, oreMine, config.GetOreMineMaintenanceCost(), bOnline);
        }
        
        return bSuccess;
    }
    
    private boolean RepairStructure(int lPlayerID, Structure structure)
    {
        Player player = GetPlayer(lPlayerID);
        
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s is attempting to remotely repair %s %d...", player.GetName(), structure.GetTypeName(), structure.GetID()));
        
        if(structure.GetOwnerID() == lPlayerID && player.Functioning())
        {
            if(player.SubtractWealth(GetRepairCost(structure)))
            {
                structure.FullyRepair();
                
                CreateEvent(new LaunchEvent(String.format("%s remotely repaired a %s.", player.GetName(), structure.GetTypeName()), SoundEffect.REPAIR));
                
                return true;
            }
        }
        
        return false;
    }

    @Override
    public boolean RepairMissileSite(int lPlayerID, int lSiteID)
    {
        MissileSite missileSite = GetMissileSite(lSiteID);
        return RepairStructure(lPlayerID, missileSite);
    }

    @Override
    public boolean RepairSAMSite(int lPlayerID, int lSiteID)
    {
        SAMSite samSite = GetSAMSite(lSiteID);
        return RepairStructure(lPlayerID, samSite);
    }

    @Override
    public boolean RepairSentryGun(int lPlayerID, int lSiteID)
    {
        SentryGun sentryGun = GetSentryGun(lSiteID);
        return RepairStructure(lPlayerID, sentryGun);
    }

    @Override
    public boolean RepairOreMine(int lPlayerID, int lSiteID)
    {
        OreMine oreMine = GetOreMine(lSiteID);
        return RepairStructure(lPlayerID, oreMine);
    }

    @Override
    public boolean HealPlayer(int lPlayerID)
    {
        Player player = GetPlayer(lPlayerID);
        
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s is attempting to heal...", player.GetName()));
        
        if(player.Functioning())
        {
            if(player.SubtractWealth(GetHealCost(player)))
            {
                player.FullyRepair();
                CreateEvent(new LaunchEvent(String.format("%s healed.", player.GetName()), SoundEffect.HEAL));
                return true;
            }
        }
        
        return false;
    }

    @Override
    public boolean SetAvatar(int lPlayerID, int lAvatarID)
    {
        Player player = Players.get(lPlayerID);
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("Setting %s's avatar.", player.GetName()));
        player.SetAvatarID(lAvatarID);
        return true;
    }

    @Override
    public boolean SetAllianceAvatar(int lPlayerID, int lAvatarID)
    {
        Player player = Players.get(lPlayerID);
        
        if(player.GetAllianceMemberID() != Alliance.ALLIANCE_ID_UNAFFILIATED)
        {
            Alliance alliance = GetAlliance(player.GetAllianceMemberID());
            LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("Setting %s's alliance avatar.", player.GetName()));
            alliance.SetAvatarID(lAvatarID);
            return true;
        }
        
        return false;
    }

    @Override
    public boolean SetSAMSiteModes(int lPlayerID, List<Integer> SiteIDs, byte cMode)
    {
        boolean bSuccess = true;
        
        for(Integer lSiteID : SiteIDs)
        {
            SAMSite samSite = SAMSites.get(lSiteID);
            
            if(lPlayerID == samSite.GetOwnerID())
            {
                samSite.SetMode(cMode);
            }
            else
                bSuccess = false;
        }
        
        return bSuccess;
    }
    
    private boolean SetStructureName(int lPlayerID, Structure structure, String strName)
    {
        Player player = Players.get(lPlayerID);
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s's is setting %s %d to name %s...", player.GetName(), structure.GetTypeName(), structure.GetID(), strName));
        strName = LaunchUtilities.SanitiseText(strName, true, true);
        
        if(player.GetID() == structure.GetOwnerID() && strName.length() <= Defs.MAX_STRUCTURE_NAME_LENGTH)
        {
            LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, "...Succeeded.");
            structure.SetName(strName);
            return true;
        }
        
        return false;
    }

    @Override
    public boolean SetSAMSiteName(int lPlayerID, int lSiteID, String strName)
    {
        return SetStructureName(lPlayerID, SAMSites.get(lSiteID), strName);
    }

    @Override
    public boolean SetMissileSiteName(int lPlayerID, int lSiteID, String strName)
    {
        return SetStructureName(lPlayerID, MissileSites.get(lSiteID), strName);
    }

    @Override
    public boolean SetSentryGunName(int lPlayerID, int lSiteID, String strName)
    {
        return SetStructureName(lPlayerID, SentryGuns.get(lSiteID), strName);
    }

    @Override
    public boolean SetOreMineName(int lPlayerID, int lSiteID, String strName)
    {
        return SetStructureName(lPlayerID, OreMines.get(lSiteID), strName);
    }

    @Override
    public boolean CloseAccount(int lPlayerID)
    {
        Player player = Players.get(lPlayerID);
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("Closing %s's account.", player.GetName()));
        SetPlayerAWOL(player, !player.Destroyed());
        
        if(player.GetAllianceMemberID() != Alliance.ALLIANCE_ID_UNAFFILIATED)
        {
            Alliance alliance = GetAlliance(player.GetAllianceMemberID());
            AllianceCleanupCheck(alliance);
        }
        
        return true;
    }

    @Override
    public boolean UpgradeToNuclear(int lPlayerID, int lMissileSiteID)
    {
        Player player = Players.get(lPlayerID);
        MissileSite missileSite = MissileSites.get(lMissileSiteID);
        
        if(lPlayerID == missileSite.GetOwnerID())
        {
            if(!missileSite.CanTakeNukes())
            {
                if(player.GetWealth() >= config.GetNukeUpgradeCost())
                {
                    player.SubtractWealth(config.GetNukeUpgradeCost());
                    missileSite.UpgradeToNuclear();
                    RemoveRespawnProtection(player);
                    CreateEvent(new LaunchEvent(String.format("%s upgraded a missile site to nuclear.", player.GetName()), SoundEffect.EQUIP));
                    
                    //Notify nearby users.
                    for(Player otherPlayer : Players.values())
                    {
                        switch(GetAllegiance(player, otherPlayer))
                        {
                            case NEUTRAL:
                            case ENEMY:
                            case PENDING_TREATY:
                            {
                                if(player.GetPosition().DistanceTo(otherPlayer.GetPosition()) < config.GetNuclearEscalationRadius())
                                {
                                    User user = otherPlayer.GetUser();

                                    if(user != null)
                                        user.SetNuclearEscalation();

                                    CreateReport(otherPlayer, new LaunchReport(String.format("Unfriendly player %s upgraded a missile site within EMP attack range of you to nuclear.", player.GetName()), true, player.GetID()));
                                }
                            }
                            break;
                        }
                    }
                    
                    return true;
                }
            }
        }
        
        return false;
    }

    @Override
    public boolean JoinAlliance(int lPlayerID, int lAllianceID)
    {
        Player player = GetPlayer(lPlayerID);
        Alliance alliance = GetAlliance(lAllianceID);
        
        if(player.GetAllianceMemberID() == Alliance.ALLIANCE_ID_UNAFFILIATED && player.GetAllianceCooloffExpired() && !InBattle(player))
        {
            player.SetAllianceRequestToJoin(lAllianceID);
            CreateEvent(new LaunchEvent(String.format("%s requested to join %s.", player.GetName(), alliance.GetName()), SoundEffect.RESPAWN));
            
            //Send report to leaders.
            for(Player leader : Players.values())
            {
                if(leader.GetAllianceMemberID() == lAllianceID && leader.GetIsAnMP())
                {
                    CreateReport(leader, new LaunchReport(String.format("%s is requesting to join %s. Go to alliances to accept/reject.", player.GetName(), alliance.GetName()), false, alliance.GetID(), player.GetID(), true, false));
                }
            }
            
            return true;
        }
        
        return false;
    }

    @Override
    public boolean LeaveAlliance(int lPlayerID)
    {
        Player player = GetPlayer(lPlayerID);
        
        if(player.GetAllianceMemberID() != Alliance.ALLIANCE_ID_UNAFFILIATED && !InBattle(player))
        {
            Alliance alliance = GetAlliance(player.GetAllianceMemberID());
            player.SetAllianceID(Alliance.ALLIANCE_ID_UNAFFILIATED);
            player.SetIsAnMP(false);
            player.SetAllianceCooloffTime(config.GetAllianceCooloffTime());
            CreateEvent(new LaunchEvent(String.format("%s left %s.", player.GetName(), alliance.GetName()), SoundEffect.EXPLOSION));
            
            for(Player ally : Players.values())
            {
                if(ally.GetAllianceMemberID() == alliance.GetID())
                {
                    CreateReport(ally, new LaunchReport(String.format("%s left %s.", player.GetName(), alliance.GetName()), false, alliance.GetID(), player.GetID(), true, false));
                }
            }
            
            //Disband the alliance?
            AllianceCleanupCheck(alliance);
            
            return true;
        }
        
        return false;
    }

    @Override
    public boolean DeclareWar(int lPlayerID, int lAllianceID)
    {
        Player player = GetPlayer(lPlayerID);
        
        if(player.GetAllianceMemberID() != Alliance.ALLIANCE_ID_UNAFFILIATED && player.GetAllianceMemberID() != lAllianceID && player.GetIsAnMP())
        {
            Alliance alliance = GetAlliance(player.GetAllianceMemberID());
            Alliance allianceOther = GetAlliance(lAllianceID);
            
            if(CanDeclareWar(alliance.GetID(), lAllianceID))
            {
                CreateWar(alliance.GetID(), lAllianceID);
                AllianceUpdated(alliance, true);
                AllianceUpdated(allianceOther, true);
                
                if(AffiliationOffered(lAllianceID, alliance.GetID()))
                {
                    CreateEvent(new LaunchEvent(String.format("%s rejected affiliation from %s and declared war (instigated by %s).", alliance.GetName(), allianceOther.GetName(), player.GetName()), SoundEffect.RESPAWN));
                    CreateReport(new LaunchReport(String.format("%s rejected affiliation from %s and declared war (instigated by %s).", alliance.GetName(), allianceOther.GetName(), player.GetName()), false, alliance.GetID(), allianceOther.GetID(), true, true));
                }
                else
                {
                    CreateEvent(new LaunchEvent(String.format("%s declared war on %s (instigated by %s).", alliance.GetName(), allianceOther.GetName(), player.GetName()), SoundEffect.RESPAWN));
                    CreateReport(new LaunchReport(String.format("%s declared war on %s (instigated by %s).", alliance.GetName(), allianceOther.GetName(), player.GetName()), false, alliance.GetID(), allianceOther.GetID(), true, true));
                }
                
                return true;
            }
        }
        
        return false;
    }

    @Override
    public boolean ProposeAffiliation(int lPlayerID, int lAllianceID)
    {
        Player player = GetPlayer(lPlayerID);
        
        if(player.GetAllianceMemberID() != Alliance.ALLIANCE_ID_UNAFFILIATED && player.GetAllianceMemberID() != lAllianceID && player.GetIsAnMP())
        {
            Alliance alliance = GetAlliance(player.GetAllianceMemberID());
            Alliance allianceOther = GetAlliance(lAllianceID);
            
            if(CanProposeAffiliation(alliance.GetID(), lAllianceID))
            {
                CreateAffiliationRequest(alliance.GetID(), lAllianceID);
                CreateEvent(new LaunchEvent(String.format("%s offered affiliation to %s (instigated by %s).", alliance.GetName(), allianceOther.GetName(), player.GetName()), SoundEffect.RESPAWN));
                CreateReport(alliance, new LaunchReport(String.format("Your alliance offered affiliation to %s (instigated by %s).", allianceOther.GetName(), player.GetName()), true, alliance.GetID(), allianceOther.GetID(), true, true));
                CreateReport(allianceOther, new LaunchReport(String.format("%s offered affiliation to your alliance (instigated by %s).", alliance.GetName(), player.GetName()), true, alliance.GetID(), allianceOther.GetID(), true, true));
                return true;
            }
        }
        
        return false;
    }

    @Override
    public boolean AcceptAffiliation(int lPlayerID, int lAllianceID)
    {
        Player player = GetPlayer(lPlayerID);
        
        if(player.GetAllianceMemberID() != Alliance.ALLIANCE_ID_UNAFFILIATED && player.GetAllianceMemberID() != lAllianceID && player.GetIsAnMP())
        {
            Alliance alliance = GetAlliance(player.GetAllianceMemberID());
            Alliance allianceOther = GetAlliance(lAllianceID);
            
            if(AffiliationOffered(lAllianceID, alliance.GetID()))
            {
                CreateAffiliation(alliance.GetID(), lAllianceID);
                AllianceUpdated(alliance, true);
                AllianceUpdated(allianceOther, true);
                CreateEvent(new LaunchEvent(String.format("%s affiliated with %s (instigated by %s).", alliance.GetName(), allianceOther.GetName(), player.GetName()), SoundEffect.RESPAWN));
                CreateReport(new LaunchReport(String.format("%s affiliated with %s (instigated by %s).", alliance.GetName(), allianceOther.GetName(), player.GetName()), false, alliance.GetID(), allianceOther.GetID(), true, true));
                return true;
            }
        }
        
        return false;
    }

    @Override
    public boolean RejectAffiliation(int lPlayerID, int lAllianceID)
    {
        Player player = GetPlayer(lPlayerID);
        
        if(player.GetAllianceMemberID() != Alliance.ALLIANCE_ID_UNAFFILIATED && player.GetAllianceMemberID() != lAllianceID && player.GetIsAnMP())
        {
            Alliance alliance = GetAlliance(player.GetAllianceMemberID());
            Alliance allianceOther = GetAlliance(lAllianceID);
            
            if(AffiliationOffered(lAllianceID, alliance.GetID()))
            {
                RemoveExistingTreaties(lAllianceID, alliance.GetID());
                CreateEvent(new LaunchEvent(String.format("%s rejected %s's affiliation offer (instigated by %s).", alliance.GetName(), allianceOther.GetName(), player.GetName()), SoundEffect.RESPAWN));
                CreateReport(new LaunchReport(String.format("%s rejected %s's affiliation offer (instigated by %s).", alliance.GetName(), allianceOther.GetName(), player.GetName()), false, alliance.GetID(), allianceOther.GetID(), true, true));
                return true;
            }
        }
        
        return false;
    }

    @Override
    public boolean Promote(int lPromotor, int lPromotee)
    {
        Player promotor = GetPlayer(lPromotor);
        Player promotee = GetPlayer(lPromotee);
        Alliance alliance = GetAlliance(promotor.GetAllianceMemberID());
        
        if(promotor.GetAllianceMemberID() != Alliance.ALLIANCE_ID_UNAFFILIATED && promotor.GetAllianceMemberID() == promotee.GetAllianceMemberID() && promotor.GetIsAnMP() && !promotee.GetIsAnMP())
        {
            promotee.SetIsAnMP(true);
            AllianceUpdated(alliance, true);
            CreateEvent(new LaunchEvent(String.format("%s promoted %s to lead %s.", promotor.GetName(), promotee.GetName(), alliance.GetName()), SoundEffect.RESPAWN));
            return true;
        }
        
        return false;
    }

    @Override
    public boolean AcceptJoin(int lLeaderID, int lMemberID)
    {
        Player leader = GetPlayer(lLeaderID);
        Alliance alliance = GetAlliance(leader.GetAllianceMemberID());
        Player joiner = GetPlayer(lMemberID);
        
        if(leader.GetIsAnMP() && joiner.GetAllianceJoiningID() == alliance.GetID() && !InBattle(joiner))
        {
            joiner.SetAllianceID(alliance.GetID());
            AllianceUpdated(alliance, true);
            CreateEvent(new LaunchEvent(String.format("%s joined %s.", joiner.GetName(), alliance.GetName()), SoundEffect.RESPAWN));
            CreateReport(joiner, new LaunchReport(String.format("You joined %s.", alliance.GetName()), false, alliance.GetID(), joiner.GetID(), true, false));
            
            for(Player player : Players.values())
            {
                if(player.GetAllianceMemberID() == alliance.GetID())
                {
                    if(player != joiner)
                    {    
                        CreateReport(player, new LaunchReport(String.format("%s joined %s and is now your ally. Approved by %s.", joiner.GetName(), alliance.GetName(), leader.GetName()), false, alliance.GetID(), joiner.GetID(), true, false));
                    }
                }
            }
            
            return true;
        }
        
        return false;
    }

    @Override
    public boolean RejectJoin(int lLeaderID, int lMemberID)
    {
        Player leader = GetPlayer(lLeaderID);
        Alliance alliance = GetAlliance(leader.GetAllianceMemberID());
        Player joiner = GetPlayer(lMemberID);
        
        if(leader.GetIsAnMP() && joiner.GetAllianceJoiningID() == alliance.GetID())
        {
            joiner.RejectAllianceRequestToJoin();
            AllianceUpdated(alliance, true);
            CreateEvent(new LaunchEvent(String.format("%s's application to join %s was declined.", joiner.GetName(), alliance.GetName()), SoundEffect.RESPAWN));
            CreateReport(joiner, new LaunchReport(String.format("Your request to join %s was rejected by %s.", alliance.GetName(), leader.GetName()), false, alliance.GetID(), leader.GetID(), true, false));
            
            for(Player player : Players.values())
            {
                if(player.GetAllianceMemberID() == alliance.GetID())
                {
                    if(player != joiner)
                    {    
                        CreateReport(player, new LaunchReport(String.format("%s's application to join %s was declined by %s.", joiner.GetName(), alliance.GetName(), leader.GetName()), false, alliance.GetID(), joiner.GetID(), true, false));
                    }
                }
            }
            
            return true;
        }
        
        return false;
    }

    @Override
    public boolean Kick(int lLeaderID, int lMemberID)
    {
        Player leader = GetPlayer(lLeaderID);
        Player kickee = GetPlayer(lMemberID);
        
        if(leader.GetAllianceMemberID() == kickee.GetAllianceMemberID() && leader.GetIsAnMP() && !kickee.GetIsAnMP() && !InBattle(kickee))
        {
            Alliance alliance = GetAlliance(leader.GetAllianceMemberID());
            kickee.SetAllianceID(Alliance.ALLIANCE_ID_UNAFFILIATED);
            kickee.SetIsAnMP(false);
            kickee.SetAllianceCooloffTime(config.GetAllianceCooloffTime());
            
            CreateEvent(new LaunchEvent(String.format("%s was kicked out of %s.", kickee.GetName(), alliance.GetName()), SoundEffect.EXPLOSION));
            CreateReport(kickee, new LaunchReport(String.format("You were kicked out of %s by %s.", alliance.GetName(), leader.GetName()), true, alliance.GetID(), leader.GetID(), true, false));
            
            for(Player ally : Players.values())
            {
                if(ally.GetAllianceMemberID() == alliance.GetID())
                {
                    CreateReport(ally, new LaunchReport(String.format("%s was kicked out of %s by %s.", kickee.GetName(), alliance.GetName(), leader.GetName()), false, alliance.GetID(), kickee.GetID(), true, false));
                }
            }
            
            AllianceUpdated(alliance, true);
            return true;
        }
        
        return false;
    }

    @Override
    public LaunchGame GetGame()
    {
        return this;
    }

    @Override
    public void BadAvatar(int lAvatarID)
    {
        //Bad avatar reported. Reset all players and alliances that use it to avatar zero.
        for(Player player : Players.values())
        {
            if(player.GetAvatarID() == lAvatarID)
            {
                LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s's avatar ID %d reported as bad. Resetting to zero.", player.GetName(), lAvatarID));
                player.SetAvatarID(0);
            }
        }
        
        for(Alliance alliance : Alliances.values())
        {
            if(alliance.GetAvatarID() == lAvatarID)
            {
                LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s's alliance avatar ID %d reported as bad. Resetting to zero.", alliance.GetName(), lAvatarID));
                alliance.SetAvatarID(0);
            }
        }
    }

    @Override
    public void BadImage(int lImageID)
    {
        //Bad image reported. Log it.
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("Image asset %d reported as bad!", lImageID));
    }

    @Override
    public boolean CreateAlliance(int lCreatorID, String strName, String strDescription, int lAvatarID)
    {
        Player creator = Players.get(lCreatorID);
        strName = LaunchUtilities.BlessName(strName);
        strDescription = LaunchUtilities.SanitiseText(strDescription, false, true);
        
        if(creator.Functioning() && !creator.GetRespawnProtected() && creator.GetAllianceMemberID() == Alliance.ALLIANCE_ID_UNAFFILIATED && creator.GetAllianceCooloffExpired() && strName.length() > 0 && strName.length() <= Defs.MAX_ALLIANCE_NAME_LENGTH)
        {
            for(Alliance alliance : Alliances.values())
            {
                //Name taken?
                if(alliance.GetName().equals(strName))
                    return false;
            }
            
            CreateAlliance(creator, strName, strDescription, lAvatarID);
            CreateEvent(new LaunchEvent(String.format("The alliance %s has been founded by %s", strName, creator.GetName()), SoundEffect.RESPAWN));
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean TempBan(int lPlayerID, String strReason, String strBanner)
    {
        Player player = Players.get(lPlayerID);
        
        if(player != null)
        {
            if(!player.GetIsAnAdmin())
            {
                //Ban the user.
                User user = player.GetUser();

                if(user != null)
                {
                    if(user.GetBanState() == BanState.NOT)
                    {
                        long oHours = user.GetNextBanTime() / Defs.MS_PER_HOUR;
                        user.TempBan(strReason);
                        comms.StopCommsTo(user);
                        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s was banned by %s. Reason: %s", player.GetName(), strBanner, strReason));
                        CreateEvent(new LaunchEvent(String.format("%s was banned.", player.GetName()), SoundEffect.DEATH));
                        CreateReport(new LaunchReport(String.format("%s was banned for %dhrs. Reason: %s", player.GetName(), oHours, strReason), false, lPlayerID));
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    @Override
    public boolean PermaBan(int lPlayerID, String strReason, String strBanner)
    {
        Player player = Players.get(lPlayerID);
        
        if(player != null)
        {
            if(!player.GetIsAnAdmin())
            {
                //Ban the user.
                User user = player.GetUser();

                if(user != null)
                {
                    //Don't reban already banned players, to prevent ban bomb log spam.
                    if(user.GetBanState() != BanState.PERMABANNED)
                    {
                        //Remove all of their missiles.
                        for(Missile missile : Missiles.values())
                        {
                            if(missile.GetOwnedBy(player.GetID()))
                            {
                                //Refund any players for their interceptors.
                                for(Interceptor interceptor : Interceptors.values())
                                {
                                    if(interceptor.GetTargetID() == missile.GetID())
                                    {
                                        int lCost = config.GetInterceptorCost(interceptor.GetType());
                                        Player interceptorOwner = Players.get(interceptor.GetOwnerID());
                                        interceptorOwner.AddWealth(lCost);
                                        CreateReport(interceptorOwner, new LaunchReport(String.format("You were reimbursed for the cost of your interceptors tracking banned player %s's missiles.", player.GetName()), true, interceptorOwner.GetID()));
                                    }
                                }

                                missile.Destroy();
                            }
                        }

                        user.Permaban(strReason);
                        comms.StopCommsTo(user);
                        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s was permabanned by %s. Reason: %s", player.GetName(), strBanner, strReason));
                        CreateEvent(new LaunchEvent(String.format("%s was permabanned.", player.GetName()), SoundEffect.DEATH));
                        CreateReport(new LaunchReport(String.format("%s was permabanned. Reason: %s", player.GetName(), strReason), false, lPlayerID));

                        //Disband their old alliance?
                        if(player.GetAllianceMemberID() != Alliance.ALLIANCE_ID_UNAFFILIATED)
                        {
                            Alliance alliance = Alliances.get(player.GetAllianceMemberID());
                            AllianceCleanupCheck(alliance);
                        }

                        //Proscribe the IP address and location.
                        ProscribedIP proscribedIP = new ProscribedIP(GetAtomicID(lProscribedIPIndex, ProscribedIPs), user.GetLastIP());
                        ProscribedIPs.put(proscribedIP.GetID(), proscribedIP);

                        ProscribedLocation proscribedLocation = new ProscribedLocation(GetAtomicID(lProscribedLocationIndex, ProscribedLocations), player.GetPosition().GetCopy());
                        ProscribedLocations.put(proscribedIP.GetID(), proscribedLocation);
                    }

                    //Offline all structures.
                    for(Structure structure : GetAllStructures())
                    {
                        if(structure.GetOwnedBy(lPlayerID))
                        {
                            structure.TakeOffline();
                        }
                    }

                    player.SetAWOL(true);

                    return true;
                }
            }
        }
        
        return false;
    }

    @Override
    public boolean Unban(int lPlayerID, String strUnbanner)
    {
        Player player = Players.get(lPlayerID);
        
        if(player != null)
        {
            //Unban the user.
            User user = player.GetUser();
            
            if(user != null)
            {
                user.Unban();
                LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s was unbanned by %s.", player.GetName(), strUnbanner));
                CreateEvent(new LaunchEvent(String.format("%s was unbanned.", player.GetName()), SoundEffect.RESPAWN));
                CreateReport(new LaunchReport(String.format("%s was unbanned.", player.GetName()), false, lPlayerID));
            }
            
            return true;
        }
        
        return false;
    }

    @Override
    public boolean AvatarReset(int lPlayerAdminID, int lPlayerToResetID)
    {
        Player admin = Players.get(lPlayerAdminID);
        Player offender = Players.get(lPlayerToResetID);
        
        if(admin != null && offender != null)
        {
            if(admin.GetIsAnAdmin())
            {
                offender.SetAvatarID(0);
                LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s reset %s's avatar", admin.GetName(), offender.GetName()));
                CreateReport(offender, new LaunchReport("Your avatar did not comply with the game rules and was removed.", true));
                return true;
            }
        }
        
        return false;
    }

    @Override
    public boolean NameReset(int lPlayerAdminID, int lPlayerToResetID)
    {
        Player admin = Players.get(lPlayerAdminID);
        Player offender = Players.get(lPlayerToResetID);
        
        if(admin != null && offender != null)
        {
            if(admin.GetIsAnAdmin())
            {
                LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s reset %s's name", admin.GetName(), offender.GetName()));
                offender.ChangeName(LaunchUtilities.GetRandomSanctifiedString() + "Mr(s) Rudechops");
                CreateReport(offender, new LaunchReport("Your name did not comply with the game rules and was changed.", true));
                return true;
            }
        }
        
        return false;
    }

    @Override
    public void SpoofWarnings(int lPlayerID, LocationSpoofCheck spoofCheck)
    {
        Player player = Players.get(lPlayerID);
        
        if(player != null)
        {
            CreateAdminReport(new LaunchReport(String.format("[Admin] %s may have GPS spoofed. Distance %skm, Speed %skph.", player.GetName(), spoofCheck.GetDistance(), spoofCheck.GetSpeed()), true, player.GetID()));
        }
    }

    @Override
    public void MultiAccountingCheck(int lPlayerID)
    {
        Player player = Players.get(lPlayerID);
        
        if(player != null)
        {
            User user = player.GetUser();
            
            if(user != null)
            {
                for(Player otherPlayer : Players.values())
                {
                    if(player.GetID() != otherPlayer.GetID())
                    {
                        if(player.GetPosition().DistanceTo(otherPlayer.GetPosition()) < Defs.MULTIACCOUNT_CONSIDERATION_DISTANCE)
                        {
                            User otherUser = otherPlayer.GetUser();
                            
                            if(otherUser != null)
                            {
                                if(user.GetDeviceShortHash().equals(otherUser.GetDeviceShortHash()))
                                {
                                    CreateAdminReport(new LaunchReport(String.format("%s and %s are within 0.5km and have the same device hash.", player.GetName(), otherPlayer.GetName()), true, lPlayerID, otherPlayer.GetID()));
                                    /*PermaBan(player.GetID(), String.format("Multiaccounting (autodetected same device as %s)", otherPlayer.GetName()), "[SERVER]");
                                    PermaBan(otherPlayer.GetID(), String.format("Multiaccounting (autodetected same device as %s)", player.GetName()), "[SERVER]");*/
                                }
                            }
                        }
                    }
                }
            }
        
            LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("Performed multiaccounting check for %s", player.GetName()));
        }
    }

    @Override
    public boolean TransferAccount(int lFromID, int lToID)
    {
        Player playerOriginal = Players.get(lFromID);
        Player playerNew = Players.get(lToID);
        
        if(playerOriginal != null && playerNew != null)
        {
            //Get both users.
            User userOriginal = playerOriginal.GetUser();
            User userNew = playerNew.GetUser();
            
            //The new user should exist otherwise something's really not right.
            if(userNew != null)
            {
                //Delete the new user.
                Users.remove(userNew.GetIMEI());
                
                //Delete the old user if it exists.
                if(userOriginal != null)
                    Users.remove(userOriginal.GetIMEI());
                else
                    LaunchLog.ConsoleMessage("NOTE: Original user was NULL so couldn't be deleted.");
                
                //Create a replacement user with the credentials from the new user.
                User userReplacement = new User(userNew.GetIMEI(), playerOriginal.GetID());
                
                //Assign the original player the replacement user, and add the replacement user to the Users list.
                playerOriginal.SetUser(userReplacement);
                Users.put(userReplacement.GetIMEI(), userReplacement);
                
                //Delete the new player and any associated artefacts (they won't be transferred). This will also interrupt all comms, which we want to happen.
                DeletePlayer(playerNew.GetID());
            }
            else
            {
                LaunchLog.ConsoleMessage("Can't transfer account: The new user is NULL.");
            }
        }
        
        LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("Could not transfer account %d to %d.", lFromID, lToID));
        
        return false;
    }

    @Override
    public boolean ChangePlayerName(int lPlayerID, String strNewName)
    {
        Player player = Players.get(lPlayerID);
        strNewName = LaunchUtilities.BlessName(strNewName);
        
        if(player != null && strNewName.length() > 0 && strNewName.length() <= Defs.MAX_PLAYER_NAME_LENGTH)
        {
            LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s is changing their name to %s.", player.GetName(), strNewName));
            player.ChangeName(strNewName);
            return true;
        }
        
        return false;
    }

    @Override
    public boolean ChangeAllianceName(int lPlayerID, String strNewName)
    {
        Player player = Players.get(lPlayerID);
        strNewName = LaunchUtilities.BlessName(strNewName);
        
        if(player != null && strNewName.length() > 0 && strNewName.length() <= Defs.MAX_ALLIANCE_NAME_LENGTH)
        {
            if(player.GetIsAnMP())
            {
                Alliance alliance = Alliances.get(player.GetAllianceMemberID());
                
                if(alliance != null)
                {
                    LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s is changing their alliance's name to %s.", player.GetName(), strNewName));
                    alliance.SetName(strNewName);
                }
            }
        }
        
        return false;
    }

    @Override
    public boolean ChangeAllianceDescription(int lPlayerID, String strNewDescription)
    {
        Player player = Players.get(lPlayerID);
        strNewDescription = LaunchUtilities.SanitiseText(strNewDescription, false, true);
        
        if(player != null && strNewDescription.length() <= Defs.MAX_ALLIANCE_DESCRIPTION_LENGTH)
        {
            if(player.GetIsAnMP())
            {
                Alliance alliance = Alliances.get(player.GetAllianceMemberID());
                
                if(alliance != null)
                {
                    LaunchLog.Log(LaunchLog.LogType.GAME, LOG_NAME, String.format("%s is changing their alliance's description to %s.", player.GetName(), strNewDescription));
                    alliance.SetDescription(strNewDescription);
                }
            }
        }
        
        return false;
    }
    
    //---------------------------------------------------------------------------------------------------------------------------------
    // Debug functions.
    //---------------------------------------------------------------------------------------------------------------------------------
    
    public void DebugAdvanceTicks(int lTicks)
    {
        comms.InterruptAll();
        
        for(int i = 0; i < lTicks; i++)
        {
            GameTick(TICK_RATE_GAME);
        }
    }
    
    public void DebugForceEndOfHour()
    {
        HourEnded();
    }
    
    public void DebugForceEndOfDay()
    {
        DayEnded();
    }
    
    public void DebugForceEndOfWeek()
    {
        WeekEnded();
    }

    @Override
    public String GetPlayerName(int lPlayerID)
    {
        Player player = Players.get(lPlayerID);
        
        if(player == null)
            return "[DOESN'T EXIST]";
        
        return player.GetName();
    }

    @Override
    public User GetUser(int lPlayerID)
    {
        Player player = Players.get(lPlayerID);
        
        if(player != null)
            return player.GetUser();
        
        return null;
    }

    @Override
    public void AdminReport(LaunchReport report)
    {
        CreateAdminReport(report);
    }

    @Override
    public void NotifyDeviceChecksCompleteFailure(String strPlayerName)
    {
        CreateAdminReport(new LaunchReport(String.format("[Admin] A device check total failure occurred for %s.", strPlayerName), true));
    }

    @Override
    public void NotifyDeviceChecksAPIFailure(String strPlayerName)
    {
        CreateAdminReport(new LaunchReport(String.format("[Admin] A device check API failure occurred for %s.", strPlayerName), true));
    }

    @Override
    public void NotifyDeviceCheckFailure(User user)
    {
        Player player = Players.get(user.GetPlayerID());
        
        if(player != null)
        {
            CreateAdminReport(new LaunchReport(String.format("[Admin] %s failed device checks. Admin checks required.", player.GetName()), true, user.GetPlayerID()));
        }
    }

    @Override
    public void NotifyIPProscribed(User user)
    {
        Player player = Players.get(user.GetPlayerID());
        
        if(player != null)
        {
            CreateAdminReport(new LaunchReport(String.format("[Admin] %s registered with a proscribed IP address. Admin checks required.", player.GetName()), true, user.GetPlayerID()));
        }
    }

    @Override
    public void NotifyLocationProscribed(User user)
    {
        Player player = Players.get(user.GetPlayerID());
        
        if(player != null)
        {
            CreateAdminReport(new LaunchReport(String.format("[Admin] %s registered close to a proscribed location. Admin checks required.", player.GetName()), true, user.GetPlayerID()));
        }
    }

    @Override
    public void NotifyAccountRestricted(User user)
    {
        Player player = Players.get(user.GetPlayerID());
        
        if(player != null)
        {
            CreateAdminReport(new LaunchReport(String.format("[Admin] %s declined weapon launch as their account is restricted. Admin checks required.", player.GetName()), true, user.GetPlayerID()));
        }        
    }

    @Override
    public boolean GetIpAddressProscribed(String strIPAddress)
    {
        for(ProscribedIP proscribedIP : ProscribedIPs.values())
        {
            if(proscribedIP.strIPAddress.equals(strIPAddress))
                return true;
        }
        
        return false;
    }

    @Override
    public boolean GetLocationProscribed(GeoCoord geoLocation)
    {
        for(ProscribedLocation proscribedLocation : ProscribedLocations.values())
        {
            if(proscribedLocation.geoLocation.DistanceTo(geoLocation) < PROSCRIBED_LOCATION_COLLISION)
                return true;
        }
        
        return false;
    }
    
    public void PerformRestoration(int lPlayerID, LaunchGame snapshot)
    {
        Player player = Players.get(lPlayerID);
        
        if(player != null)
        {
            for(Structure restoreStructure : snapshot.GetAllStructures())
            {
                if(restoreStructure.GetOwnedBy(lPlayerID))
                {
                    //Candidate for restoration.
                    if(GetNearbyStructures(restoreStructure.GetPosition(), config.GetStructureSeparation()).isEmpty())
                    {
                        //Cleared to reconstruct. Remove any loots.
                        for(Loot loot : Loots.values())
                        {
                            if(loot.GetPosition().BroadPhaseCollisionTest(restoreStructure.GetPosition()))
                            {
                                if(loot.GetPosition().DistanceTo(restoreStructure.GetPosition()) < config.GetStructureSeparation())
                                {
                                    LaunchLog.ConsoleMessage(String.format("Removing a loot while restoring %s's stuff.", player.GetName()));
                                    loot.Collect();
                                }
                            }
                        }

                        if(restoreStructure instanceof MissileSite)
                        {
                            LaunchLog.ConsoleMessage(String.format("Restoring %s's missile site.", player.GetName()));
                            AddMissileSite((MissileSite)restoreStructure.ReIDAndReturnSelf(GetAtomicID(lMissileSiteIndex, MissileSites)));
                        }

                        if(restoreStructure instanceof SAMSite)
                        {
                            LaunchLog.ConsoleMessage(String.format("Restoring %s's SAM site.", player.GetName()));
                            AddSAMSite((SAMSite)restoreStructure.ReIDAndReturnSelf(GetAtomicID(lSAMSiteIndex, SAMSites)));
                        }

                        if(restoreStructure instanceof SentryGun)
                        {
                            LaunchLog.ConsoleMessage(String.format("Restoring %s's sentry gun.", player.GetName()));
                            AddSentryGun((SentryGun)restoreStructure.ReIDAndReturnSelf(GetAtomicID(lSentryGunIndex, SentryGuns)));
                        }

                        if(restoreStructure instanceof OreMine)
                        {
                            LaunchLog.ConsoleMessage(String.format("Restoring %s's ore mine.", player.GetName()));
                            AddOreMine((OreMine)restoreStructure.ReIDAndReturnSelf(GetAtomicID(lOreMineIndex, OreMines)));
                        }
                    }
                    else
                    {
                        LaunchLog.ConsoleMessage(String.format("Skipping a structure while restoring %s's stuff as something's already there.", player.GetName()));
                    }
                }
            }
            
            LaunchLog.ConsoleMessage("Restoration complete.");
        }
        else
        {
            LaunchLog.ConsoleMessage("The player was invalid. Could not perform restoration.");
        }
    }
    
    public void BanBomb(int lPlayerID, float fltBlastRadius)
    {
        Player player = Players.get(lPlayerID);
        
        if(player != null)
        {
            CreateReport(new LaunchReport(String.format("A ban bomb was detonated at %s's location.", player.GetName()), false, lPlayerID));
            
            for(Player possibleBannee : Players.values())
            {
                if(player.GetPosition().DistanceTo(possibleBannee.GetPosition()) < fltBlastRadius)
                {
                    LaunchLog.ConsoleMessage(String.format("The ban bomb hit %s.", possibleBannee.GetName()));
                    
                    PermaBan(possibleBannee.GetID(), "Hit by an intercontinental ballistic nuclear ban hammer", "The God of Launch");
                }
            }
        }
    }
    
    public void ListProscriptions()
    {
        for(ProscribedIP ip : ProscribedIPs.values())
        {
            LaunchLog.ConsoleMessage(String.format("%s - %dms remaining", ip.strIPAddress, ip.dlyExpiry.GetRemaining()));
        }
        
        for(ProscribedLocation loc : ProscribedLocations.values())
        {
            LaunchLog.ConsoleMessage(String.format("(%.4f, %.4f) - %dms remaining", loc.geoLocation.GetLatitude(), loc.geoLocation.GetLongitude(), loc.dlyExpiry.GetRemaining()));
        }
    }
}
