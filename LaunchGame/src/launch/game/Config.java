/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.game;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.CRC32;
import launch.game.entities.MissileSite;
import launch.game.entities.OreMine;
import launch.game.entities.Player;
import launch.game.entities.SAMSite;
import launch.game.entities.SentryGun;
import launch.game.entities.Structure;
import launch.game.types.*;
import launch.utilities.LaunchBannedApp;
import launch.utilities.LaunchUtilities;

/**
 * @author tobster
 */
public class Config
{
    protected static final int RULES_DATA_SIZE = 365 + (9 * 4);  //Rules, plus a 32-bit int count for each list.

    //Server-only, not communicated.
    protected int lPort;

    //Server email address.
    protected String strServerEmailAddress;

    //Rules.
    protected byte cVariant;                      //Variant doesn't do anything but provide the server with a means of forcing a config change.
    protected byte cDebugFlags;
    protected int lStartingWealth;
    protected int lRespawnWealth;
    protected int lRespawnTime;
    protected int lRespawnProtectionTime;
    protected int lHourlyWealth;
    protected int lCMSSystemCost;
    protected int lSAMSystemCost;
    protected int lCMSStructureCost;
    protected int lNukeCMSStructureCost;
    protected int lSAMStructureCost;
    protected int lSentryGunStructureCost;
    protected int lOreMineStructureCost;
    protected float fltInterceptorBaseHitChance;
    protected float fltRubbleMinValue;
    protected float fltRubbleMaxValue;
    protected int lRubbleMinTime;
    protected int lRubbleMaxTime;
    protected float fltStructureSeparation;
    protected short nPlayerBaseHP;
    protected short nStructureBaseHP;
    protected int lStructureBootTime;
    protected byte cInitialMissileSlots;
    protected byte cInitialInterceptorSlots;
    protected float fltRequiredAccuracy;
    protected int lMinRadiationTime;
    protected int lMaxRadiationTime;
    protected int lMissileUpgradeBaseCost;
    protected byte cMissileUpgradeCount;
    protected float fltResaleValue;
    protected int lDecommissionTime;
    protected int lReloadTimeBase;
    protected int lReloadTimeStage1;
    protected int lReloadTimeStage2;
    protected int lReloadTimeStage3;
    protected int lReloadStage1Cost;
    protected int lReloadStage2Cost;
    protected int lReloadStage3Cost;
    protected float fltRepairSalvageDistance;
    protected int lMissileSiteMaintenanceCost;
    protected int lSAMSiteMaintenanceCost;
    protected int lSentryGunMaintenanceCost;
    protected int lOreMineMaintenanceCost;
    protected int lHealthInterval;
    protected int lRadiationInterval;
    protected int lPlayerRepairCost;
    protected int lStructureRepairCost;
    protected long oAWOLTime;
    protected long oRemoveTime;
    protected int lNukeUpgradeCost;
    protected int lAllianceCooloffTime;
    protected int lMissileNuclearCost;
    protected int lMissileTrackingCost;
    protected int lMissileECMCost;
    protected float fltEMPChance;
    protected float fltEMPRadiusMultiplier;
    protected float fltECMInterceptorChanceReduction;
    protected float fltManualInterceptorChanceIncrease;
    protected int lSentryGunReloadTime;
    protected float fltSentryGunRange;
    protected float fltSentryGunHitChance;
    protected float fltOreMineRadius;
    protected int lMaxOreValue;
    protected int lOreMineGenerateTime;
    protected int lOreMinExpiry;
    protected int lOreMaxExpiry;
    protected int lMissileSpeedIndexCost;
    protected float fltMissileSpeedIndexCostPow;
    protected int lMissileRangeIndexCost;
    protected float fltMissileRangeIndexCostPow;
    protected int lMissileBlastRadiusIndexCost;
    protected float fltMissileBlastRadiusIndexCostPow;
    protected int lNukeBlastRadiusIndexCost;
    protected float fltNukeBlastRadiusIndexCostPow;
    protected int lMissileMaxDamageIndexCost;
    protected float fltMissileMaxDamageIndexCostPow;
    protected int lInterceptorSpeedIndexCost;
    protected float fltInterceptorSpeedIndexCostPow;
    protected int lInterceptorRangeIndexCost;
    protected float fltInterceptorRangeIndexCostPow;
    protected float fltMissilePrepTimePerMagnitude;
    protected float fltInterceptorPrepTimePerMagnitude;
    protected int lHourlyBonusDiplomaticPresence;     //Hourly bonus for having an avatar.
    protected int lHourlyBonusPoliticalEngagement;    //Hourly bonus for being in an alliance that has an avatar.
    protected int lHourlyBonusDefenderOfTheNation;    //Hourly bonus for having an active defense system.
    protected int lHourlyBonusNuclearSuperpower;      //Hourly bonus for possessing nuclear weapons.
    protected int lHourlyBonusWeeklyKillsBatch;       //Stackable hourly bonus for killing every n^2 players this week.
    protected int lHourlyBonusSurvivor;               //Hourly bonus for not having died this week.
    protected int lHourlyBonusHippy;                  //Hourly bonus for having made no attacks this week.
    protected int lHourlyBonusPeaceMaker;             //Hourly bonus for being allied or affiliated with 25% of players in the game.
    protected int lHourlyBonusWarMonger;              //Hourly bonus for being at war with 25% of players in the game.
    protected int lHourlyBonusLoneWolf;               //Hourly bonus for being isolated from other players by distance.
    protected float fltLoneWolfDistance;              //Distance from the nearest player to be eligible for the Lone Wolf bonus.

    //Derived from other configs.
    protected float fltOreMineDiameter;
    protected float fltNuclearEscalationRadius;

    //Server-only.
    protected List<LaunchBannedApp> MinorCheatingApps = new ArrayList();
    protected List<LaunchBannedApp> MajorCheatingApps = new ArrayList();

    //Value tables.
    protected Map<Byte, Float> MissileSpeeds = new LinkedHashMap<>();
    protected Map<Byte, Float> MissileRanges = new LinkedHashMap<>();
    protected Map<Byte, Float> MissileBlastRadii = new LinkedHashMap<>();
    protected Map<Byte, Float> NukeBlastRadii = new LinkedHashMap<>();
    protected Map<Byte, Short> MissileMaxDamages = new LinkedHashMap<>();
    protected Map<Byte, Float> InterceptorSpeeds = new LinkedHashMap<>();
    protected Map<Byte, Float> InterceptorRanges = new LinkedHashMap<>();

    //Types.
    protected Map<Byte, MissileType> MissileTypes = new LinkedHashMap<>();
    protected Map<Byte, InterceptorType> InterceptorTypes = new LinkedHashMap<>();

    //Communicable data.
    protected int lSize;
    protected byte[] cData;
    protected int lChecksum;
    
    //For testing.
    public Config()
    {}

    //From config data.
    public Config(String strServerEmailAddress,
            byte cVariant,
            byte cDebugFlags,
            int lStartingWealth,
            int lRespawnWealth,
            int lRespawnTime,
            int lRespawnProtectionTime,
            int lHourlyWealth,
            int lCMSSystemCost,
            int lSAMSystemCost,
            int lCMSStructureCost,
            int lNukeCMSStructureCost,
            int lSAMStructureCost,
            int lSentryGunStructureCost,
            int lOreMineStructureCost,
            float fltInterceptorBaseHitChance,
            float fltRubbleMinValue,
            float fltRubbleMaxValue,
            int lRubbleMinTime,
            int lRubbleMaxTime,
            float fltStructureSeparation,
            short nPlayerBaseHP,
            short nStructureBaseHP,
            int lStructureBootTime,
            byte cInitialMissileSlots,
            byte cInitialInterceptorSlots,
            float fltRequiredAccuracy,
            int lMinRadiationTime,
            int lMaxRadiationTime,
            int lMissileUpgradeBaseCost,
            byte cMissileUpgradeCount,
            float fltResaleValue,
            int lDecommissionTime,
            int lReloadTimeBase,
            int lReloadTimeStage1,
            int lReloadTimeStage2,
            int lReloadTimeStage3,
            int lReloadStage1Cost,
            int lReloadStage2Cost,
            int lReloadStage3Cost,
            float fltRepairSalvageDistance,
            int lMissileSiteMaintenanceCost,
            int lSAMSiteMaintenanceCost,
            int lSentryGunMaintenanceCost,
            int lOreMineMaintenanceCost,
            int lHealthInterval,
            int lRadiationInterval,
            int lPlayerRepairCost,
            int lStructureRepairCost,
            long oAWOLTime,
            long oRemoveTime,
            int lNukeUpgradeCost,
            int lAllianceCooloffTime,
            int lMissileNuclearCost,
            int lMissileTrackingCost,
            int lMissileECMCost,
            float fltEMPChance,
            float fltEMPRadiusMultiplier,
            float fltECMInterceptorChanceReduction,
            float fltManualInterceptorChanceIncrease,
            int lSentryGunReloadTime,
            float fltSentryGunRange,
            float fltSentryGunHitChance,
            float fltOreMineRadius,
            int lMaxOreValue,
            int lOreMineGenerateTime,
            int lOreMinExpiry,
            int lOreMaxExpiry,
            int lMissileSpeedIndexCost,
            float fltMissileSpeedIndexCostPow,
            int lMissileRangeIndexCost,
            float fltMissileRangeIndexCostPow,
            int lMissileBlastRadiusIndexCost,
            float fltMissileBlastRadiusIndexCostPow,
            int lNukeBlastRadiusIndexCost,
            float fltNukeBlastRadiusIndexCostPow,
            int lMissileMaxDamageIndexCost,
            float fltMissileMaxDamageIndexCostPow,
            int lInterceptorSpeedIndexCost,
            float fltInterceptorSpeedIndexCostPow,
            int lInterceptorRangeIndexCost,
            float fltInterceptorRangeIndexCostPow,
            float fltMissilePrepTimePerMagnitude,
            float fltInterceptorPrepTimePerMagnitude,
            int lHourlyBonusDiplomaticPresence,
            int lHourlyBonusPoliticalEngagement,
            int lHourlyBonusDefenderOfTheNation,
            int lHourlyBonusNuclearSuperpower,
            int lHourlyBonusWeeklyKillsBatch,
            int lHourlyBonusSurvivor,
            int lHourlyBonusHippy,
            int lHourlyBonusPeaceMaker,
            int lHourlyBonusWarMonger,
            int lHourlyBonusLoneWolf,
            float fltLoneWolfDistance)
    {
        this.strServerEmailAddress = strServerEmailAddress;

        this.cVariant = cVariant;
        this.cDebugFlags = cDebugFlags;
        this.lStartingWealth = lStartingWealth;
        this.lRespawnWealth = lRespawnWealth;
        this.lRespawnTime = lRespawnTime;
        this.lRespawnProtectionTime = lRespawnProtectionTime;
        this.lHourlyWealth = lHourlyWealth;
        this.lCMSSystemCost = lCMSSystemCost;
        this.lSAMSystemCost = lSAMSystemCost;
        this.lCMSStructureCost = lCMSStructureCost;
        this.lNukeCMSStructureCost = lNukeCMSStructureCost;
        this.lSAMStructureCost = lSAMStructureCost;
        this.lSentryGunStructureCost = lSentryGunStructureCost;
        this.lOreMineStructureCost = lOreMineStructureCost;
        this.fltInterceptorBaseHitChance = fltInterceptorBaseHitChance;
        this.fltRubbleMinValue = fltRubbleMinValue;
        this.fltRubbleMaxValue = fltRubbleMaxValue;
        this.lRubbleMinTime = lRubbleMinTime;
        this.lRubbleMaxTime = lRubbleMaxTime;
        this.fltStructureSeparation = fltStructureSeparation;
        this.nPlayerBaseHP = nPlayerBaseHP;
        this.nStructureBaseHP = nStructureBaseHP;
        this.lStructureBootTime = lStructureBootTime;
        this.cInitialMissileSlots = cInitialMissileSlots;
        this.cInitialInterceptorSlots = cInitialInterceptorSlots;
        this.fltRequiredAccuracy = fltRequiredAccuracy;
        this.lMinRadiationTime = lMinRadiationTime;
        this.lMaxRadiationTime = lMaxRadiationTime;
        this.lMissileUpgradeBaseCost = lMissileUpgradeBaseCost;
        this.cMissileUpgradeCount = cMissileUpgradeCount;
        this.fltResaleValue = fltResaleValue;
        this.lDecommissionTime = lDecommissionTime;
        this.lReloadTimeBase = lReloadTimeBase;
        this.lReloadTimeStage1 = lReloadTimeStage1;
        this.lReloadTimeStage2 = lReloadTimeStage2;
        this.lReloadTimeStage3 = lReloadTimeStage3;
        this.lReloadStage1Cost = lReloadStage1Cost;
        this.lReloadStage2Cost = lReloadStage2Cost;
        this.lReloadStage3Cost = lReloadStage3Cost;
        this.fltRepairSalvageDistance = fltRepairSalvageDistance;
        this.lMissileSiteMaintenanceCost = lMissileSiteMaintenanceCost;
        this.lSAMSiteMaintenanceCost = lSAMSiteMaintenanceCost;
        this.lSentryGunMaintenanceCost = lSentryGunMaintenanceCost;
        this.lOreMineMaintenanceCost = lOreMineMaintenanceCost;
        this.lHealthInterval = lHealthInterval;
        this.lRadiationInterval = lRadiationInterval;
        this.lPlayerRepairCost = lPlayerRepairCost;
        this.lStructureRepairCost = lStructureRepairCost;
        this.oAWOLTime = oAWOLTime;
        this.oRemoveTime = oRemoveTime;
        this.lNukeUpgradeCost = lNukeUpgradeCost;
        this.lAllianceCooloffTime = lAllianceCooloffTime;
        this.lMissileNuclearCost = lMissileNuclearCost;
        this.lMissileTrackingCost = lMissileTrackingCost;
        this.lMissileECMCost = lMissileECMCost;
        this.fltEMPChance = fltEMPChance;
        this.fltEMPRadiusMultiplier = fltEMPRadiusMultiplier;
        this.fltECMInterceptorChanceReduction = fltECMInterceptorChanceReduction;
        this.fltManualInterceptorChanceIncrease = fltManualInterceptorChanceIncrease;
        this.lSentryGunReloadTime = lSentryGunReloadTime;
        this.fltSentryGunRange = fltSentryGunRange;
        this.fltSentryGunHitChance = fltSentryGunHitChance;
        this.fltOreMineRadius = fltOreMineRadius;
        this.lMaxOreValue = lMaxOreValue;
        this.lOreMineGenerateTime = lOreMineGenerateTime;
        this.lOreMinExpiry = lOreMinExpiry;
        this.lOreMaxExpiry = lOreMaxExpiry;
        this.lMissileSpeedIndexCost = lMissileSpeedIndexCost;
        this.fltMissileSpeedIndexCostPow = fltMissileSpeedIndexCostPow;
        this.lMissileRangeIndexCost = lMissileRangeIndexCost;
        this.fltMissileRangeIndexCostPow = fltMissileRangeIndexCostPow;
        this.lMissileBlastRadiusIndexCost = lMissileBlastRadiusIndexCost;
        this.fltMissileBlastRadiusIndexCostPow = fltMissileBlastRadiusIndexCostPow;
        this.lNukeBlastRadiusIndexCost = lNukeBlastRadiusIndexCost;
        this.fltNukeBlastRadiusIndexCostPow = fltNukeBlastRadiusIndexCostPow;
        this.lMissileMaxDamageIndexCost = lMissileMaxDamageIndexCost;
        this.fltMissileMaxDamageIndexCostPow = fltMissileMaxDamageIndexCostPow;
        this.lInterceptorSpeedIndexCost = lInterceptorSpeedIndexCost;
        this.fltInterceptorSpeedIndexCostPow = fltInterceptorSpeedIndexCostPow;
        this.lInterceptorRangeIndexCost = lInterceptorRangeIndexCost;
        this.fltInterceptorRangeIndexCostPow = fltInterceptorRangeIndexCostPow;
        this.fltMissilePrepTimePerMagnitude = fltMissilePrepTimePerMagnitude;
        this.fltInterceptorPrepTimePerMagnitude = fltInterceptorPrepTimePerMagnitude;
        this.lHourlyBonusDiplomaticPresence = lHourlyBonusDiplomaticPresence;
        this.lHourlyBonusPoliticalEngagement = lHourlyBonusPoliticalEngagement;
        this.lHourlyBonusDefenderOfTheNation = lHourlyBonusDefenderOfTheNation;
        this.lHourlyBonusNuclearSuperpower = lHourlyBonusNuclearSuperpower;
        this.lHourlyBonusWeeklyKillsBatch = lHourlyBonusWeeklyKillsBatch;
        this.lHourlyBonusSurvivor = lHourlyBonusSurvivor;
        this.lHourlyBonusHippy = lHourlyBonusHippy;
        this.lHourlyBonusPeaceMaker = lHourlyBonusPeaceMaker;
        this.lHourlyBonusWarMonger = lHourlyBonusWarMonger;
        this.lHourlyBonusLoneWolf = lHourlyBonusLoneWolf;
        this.fltLoneWolfDistance = fltLoneWolfDistance;

        this.fltOreMineDiameter = fltOreMineRadius * 2.0f;
    }

    //Communicated.
    public Config(byte[] cConfigData)
    {
        cData = cConfigData;
        lSize = cConfigData.length;
        ByteBuffer bb = ByteBuffer.wrap(cData);

        strServerEmailAddress = LaunchUtilities.StringFromData(bb);

        //Assign rules.
        cVariant = bb.get();
        cDebugFlags = bb.get();
        lStartingWealth = bb.getInt();
        lRespawnWealth = bb.getInt();
        lRespawnTime = bb.getInt();
        lRespawnProtectionTime = bb.getInt();
        lHourlyWealth = bb.getInt();
        lCMSSystemCost = bb.getInt();
        lSAMSystemCost = bb.getInt();
        lCMSStructureCost = bb.getInt();
        lNukeCMSStructureCost = bb.getInt();
        lSAMStructureCost = bb.getInt();
        lSentryGunStructureCost = bb.getInt();
        lOreMineStructureCost = bb.getInt();
        fltInterceptorBaseHitChance = bb.getFloat();
        fltRubbleMinValue = bb.getFloat();
        fltRubbleMaxValue = bb.getFloat();
        lRubbleMinTime = bb.getInt();
        lRubbleMaxTime = bb.getInt();
        fltStructureSeparation = bb.getFloat();
        nPlayerBaseHP = bb.getShort();
        nStructureBaseHP = bb.getShort();
        lStructureBootTime = bb.getInt();
        cInitialMissileSlots = bb.get();
        cInitialInterceptorSlots = bb.get();
        fltRequiredAccuracy = bb.getFloat();
        lMinRadiationTime = bb.getInt();
        lMaxRadiationTime = bb.getInt();
        lMissileUpgradeBaseCost = bb.getInt();
        cMissileUpgradeCount = bb.get();
        fltResaleValue = bb.getFloat();
        lDecommissionTime = bb.getInt();
        lReloadTimeBase = bb.getInt();
        lReloadTimeStage1 = bb.getInt();
        lReloadTimeStage2 = bb.getInt();
        lReloadTimeStage3 = bb.getInt();
        lReloadStage1Cost = bb.getInt();
        lReloadStage2Cost = bb.getInt();
        lReloadStage3Cost = bb.getInt();
        fltRepairSalvageDistance = bb.getFloat();
        lMissileSiteMaintenanceCost = bb.getInt();
        lSAMSiteMaintenanceCost = bb.getInt();
        lSentryGunMaintenanceCost = bb.getInt();
        lOreMineMaintenanceCost = bb.getInt();
        lHealthInterval = bb.getInt();
        lRadiationInterval = bb.getInt();
        lPlayerRepairCost = bb.getInt();
        lStructureRepairCost = bb.getInt();
        oAWOLTime = bb.getLong();
        oRemoveTime = bb.getLong();
        lNukeUpgradeCost = bb.getInt();
        lAllianceCooloffTime = bb.getInt();
        lMissileNuclearCost = bb.getInt();
        lMissileTrackingCost = bb.getInt();
        lMissileECMCost = bb.getInt();
        fltEMPChance = bb.getFloat();
        fltEMPRadiusMultiplier = bb.getFloat();
        fltECMInterceptorChanceReduction = bb.getFloat();
        fltManualInterceptorChanceIncrease = bb.getFloat();
        lSentryGunReloadTime = bb.getInt();
        fltSentryGunRange = bb.getFloat();
        fltSentryGunHitChance = bb.getFloat();
        fltOreMineRadius = bb.getFloat();
        lMaxOreValue = bb.getInt();
        lOreMineGenerateTime = bb.getInt();
        lOreMinExpiry = bb.getInt();
        lOreMaxExpiry = bb.getInt();
        lMissileSpeedIndexCost = bb.getInt();
        fltMissileSpeedIndexCostPow = bb.getFloat();
        lMissileRangeIndexCost = bb.getInt();
        fltMissileRangeIndexCostPow = bb.getFloat();
        lMissileBlastRadiusIndexCost = bb.getInt();
        fltMissileBlastRadiusIndexCostPow = bb.getFloat();
        lNukeBlastRadiusIndexCost = bb.getInt();
        fltNukeBlastRadiusIndexCostPow = bb.getFloat();
        lMissileMaxDamageIndexCost = bb.getInt();
        fltMissileMaxDamageIndexCostPow = bb.getFloat();
        lInterceptorSpeedIndexCost = bb.getInt();
        fltInterceptorSpeedIndexCostPow = bb.getFloat();
        lInterceptorRangeIndexCost = bb.getInt();
        fltInterceptorRangeIndexCostPow = bb.getFloat();
        fltMissilePrepTimePerMagnitude = bb.getFloat();
        fltInterceptorPrepTimePerMagnitude = bb.getFloat();
        lHourlyBonusDiplomaticPresence = bb.getInt();
        lHourlyBonusPoliticalEngagement = bb.getInt();
        lHourlyBonusDefenderOfTheNation = bb.getInt();
        lHourlyBonusNuclearSuperpower = bb.getInt();
        lHourlyBonusWeeklyKillsBatch = bb.getInt();
        lHourlyBonusSurvivor = bb.getInt();
        lHourlyBonusHippy = bb.getInt();
        lHourlyBonusPeaceMaker = bb.getInt();
        lHourlyBonusWarMonger = bb.getInt();
        lHourlyBonusLoneWolf = bb.getInt();
        fltLoneWolfDistance = bb.getFloat();

        //Assign property tables.
        AssignFloatPropertyTable(bb, MissileSpeeds);
        AssignFloatPropertyTable(bb, MissileRanges);
        AssignFloatPropertyTable(bb, MissileBlastRadii);
        AssignFloatPropertyTable(bb, NukeBlastRadii);
        AssignShortPropertyTable(bb, MissileMaxDamages);
        AssignFloatPropertyTable(bb, InterceptorSpeeds);
        AssignFloatPropertyTable(bb, InterceptorRanges);

        //Assign types.
        int lMissileTypes = bb.getInt();

        for(int i = 0; i < lMissileTypes; i++)
        {
            MissileType missileType = new MissileType(bb);
            MissileTypes.put(missileType.GetID(), missileType);
        }

        int lInterceptorTypes = bb.getInt();

        for(int i = 0; i < lInterceptorTypes; i++)
        {
            InterceptorType interceptorType = new InterceptorType(bb);
            InterceptorTypes.put(interceptorType.GetID(), interceptorType);
        }

        
        //Compute checksum.
        CRC32 crc32 = new CRC32();
        crc32.update(cData, 0, cData.length);
        lChecksum = (int)crc32.getValue();

        
        fltOreMineDiameter = fltOreMineRadius * 2.0f;
    }

    private void AssignFloatPropertyTable(ByteBuffer bb, Map<Byte, Float> Table)
    {
        //Assign property tables.
        int lCount = bb.getInt();

        for(int i = 0; i < lCount; i++)
        {
            Table.put(bb.get(), bb.getFloat());
        }
    }

    private void AssignShortPropertyTable(ByteBuffer bb, Map<Byte, Short> Table)
    {
        //Assign property tables.
        int lCount = bb.getInt();

        for(int i = 0; i < lCount; i++)
        {
            Table.put(bb.get(), bb.getShort());
        }
    }

    private void PutFloatPropertyTable(ByteBuffer bb, Map<Byte, Float> Table)
    {
        bb.putInt(Table.size());
        for(Entry<Byte, Float> property : Table.entrySet())
        {
            bb.put(property.getKey());
            bb.putFloat(property.getValue());
        }
    }

    private void PutShortPropertyTable(ByteBuffer bb, Map<Byte, Short> Table)
    {
        bb.putInt(Table.size());
        for(Entry<Byte, Short> property : Table.entrySet())
        {
            bb.put(property.getKey());
            bb.putShort(property.getValue());
        }
    }

    public void AddMissileType(byte cID, MissileType missileType)
    {
        MissileTypes.put(cID, missileType);
    }

    public void AddInterceptorType(byte cID, InterceptorType interceptorType)
    {
        InterceptorTypes.put(cID, interceptorType);
    }

    public void AddMissileSpeed(byte cID, float fltSpeed)
    {
        MissileSpeeds.put(cID, fltSpeed);
    }

    public void AddMissileRange(byte cID, float fltRange)
    {
        MissileRanges.put(cID, fltRange);
    }

    public void AddMissileBlastRadius(byte cID, float fltBlastRadius)
    {
        MissileBlastRadii.put(cID, fltBlastRadius);
    }

    public void AddNukeBlastRadius(byte cID, float fltBlastRadius)
    {
        NukeBlastRadii.put(cID, fltBlastRadius);
    }

    public void AddMissileMaxDamage(byte cID, short nMaxDamage)
    {
        MissileMaxDamages.put(cID, nMaxDamage);
    }

    public void AddInterceptorSpeed(byte cID, float fltSpeed)
    {
        InterceptorSpeeds.put(cID, fltSpeed);
    }

    public void AddInterceptorRange(byte cID, float fltSpeed)
    {
        InterceptorRanges.put(cID, fltSpeed);
    }

    public void SetMissileSpeeds(Map map)
    {
        MissileSpeeds = map;
    }

    public void SetMissileRanges(Map map)
    {
        MissileRanges = map;
    }

    public void SetMissileBlastRadii(Map map)
    {
        MissileBlastRadii = map;
    }

    public void SetNukeBlastRadii(Map map)
    {
        NukeBlastRadii = map;
    }

    public void SetMissileMaxDamages(Map map)
    {
        MissileMaxDamages = map;
    }

    public void SetInterceptorSpeeds(Map map)
    {
        InterceptorSpeeds = map;
    }

    public void SetInterceptorRanges(Map map)
    {
        InterceptorRanges = map;
    }

    public void AddMinorCheatingApp(LaunchBannedApp app)
    {
        MinorCheatingApps.add(app);
    }

    public void AddMajorCheatingApp(LaunchBannedApp app)
    {
        MajorCheatingApps.add(app);
    }

    public void Finalise()
    {
        //Compute data size.
        lSize = RULES_DATA_SIZE + LaunchUtilities.GetStringDataSize(strServerEmailAddress);

        lSize += MissileSpeeds.size() * 5;
        lSize += MissileRanges.size() * 5;
        lSize += MissileBlastRadii.size() * 5;
        lSize += NukeBlastRadii.size() * 5;
        lSize += MissileMaxDamages.size() * 3;
        lSize += InterceptorSpeeds.size() * 5;
        lSize += InterceptorRanges.size() * 5;

        for(MissileType missileType : MissileTypes.values())
        {
            lSize += missileType.GetData().length;
        }

        for(InterceptorType interceptorType : InterceptorTypes.values())
        {
            lSize += interceptorType.GetData().length;
        }

        //Populate data object.
        ByteBuffer bb = ByteBuffer.allocate(lSize);

        bb.put(LaunchUtilities.GetStringData(strServerEmailAddress));

        bb.put(cVariant);
        bb.put(cDebugFlags);
        bb.putInt(lStartingWealth);
        bb.putInt(lRespawnWealth);
        bb.putInt(lRespawnTime);
        bb.putInt(lRespawnProtectionTime);
        bb.putInt(lHourlyWealth);
        bb.putInt(lCMSSystemCost);
        bb.putInt(lSAMSystemCost);
        bb.putInt(lCMSStructureCost);
        bb.putInt(lNukeCMSStructureCost);
        bb.putInt(lSAMStructureCost);
        bb.putInt(lSentryGunStructureCost);
        bb.putInt(lOreMineStructureCost);
        bb.putFloat(fltInterceptorBaseHitChance);
        bb.putFloat(fltRubbleMinValue);
        bb.putFloat(fltRubbleMaxValue);
        bb.putInt(lRubbleMinTime);
        bb.putInt(lRubbleMaxTime);
        bb.putFloat(fltStructureSeparation);
        bb.putShort(nPlayerBaseHP);
        bb.putShort(nStructureBaseHP);
        bb.putInt(lStructureBootTime);
        bb.put(cInitialMissileSlots);
        bb.put(cInitialInterceptorSlots);
        bb.putFloat(fltRequiredAccuracy);
        bb.putInt(lMinRadiationTime);
        bb.putInt(lMaxRadiationTime);
        bb.putInt(lMissileUpgradeBaseCost);
        bb.put(cMissileUpgradeCount);
        bb.putFloat(fltResaleValue);
        bb.putInt(lDecommissionTime);
        bb.putInt(lReloadTimeBase);
        bb.putInt(lReloadTimeStage1);
        bb.putInt(lReloadTimeStage2);
        bb.putInt(lReloadTimeStage3);
        bb.putInt(lReloadStage1Cost);
        bb.putInt(lReloadStage2Cost);
        bb.putInt(lReloadStage3Cost);
        bb.putFloat(fltRepairSalvageDistance);
        bb.putInt(lMissileSiteMaintenanceCost);
        bb.putInt(lSAMSiteMaintenanceCost);
        bb.putInt(lSentryGunMaintenanceCost);
        bb.putInt(lOreMineMaintenanceCost);
        bb.putInt(lHealthInterval);
        bb.putInt(lRadiationInterval);
        bb.putInt(lPlayerRepairCost);
        bb.putInt(lStructureRepairCost);
        bb.putLong(oAWOLTime);
        bb.putLong(oRemoveTime);
        bb.putInt(lNukeUpgradeCost);
        bb.putInt(lAllianceCooloffTime);
        bb.putInt(lMissileNuclearCost);
        bb.putInt(lMissileTrackingCost);
        bb.putInt(lMissileECMCost);
        bb.putFloat(fltEMPChance);
        bb.putFloat(fltEMPRadiusMultiplier);
        bb.putFloat(fltECMInterceptorChanceReduction);
        bb.putFloat(fltManualInterceptorChanceIncrease);
        bb.putInt(lSentryGunReloadTime);
        bb.putFloat(fltSentryGunRange);
        bb.putFloat(fltSentryGunHitChance);
        bb.putFloat(fltOreMineRadius);
        bb.putInt(lMaxOreValue);
        bb.putInt(lOreMineGenerateTime);
        bb.putInt(lOreMinExpiry);
        bb.putInt(lOreMaxExpiry);
        bb.putInt(lMissileSpeedIndexCost);
        bb.putFloat(fltMissileSpeedIndexCostPow);
        bb.putInt(lMissileRangeIndexCost);
        bb.putFloat(fltMissileRangeIndexCostPow);
        bb.putInt(lMissileBlastRadiusIndexCost);
        bb.putFloat(fltMissileBlastRadiusIndexCostPow);
        bb.putInt(lNukeBlastRadiusIndexCost);
        bb.putFloat(fltNukeBlastRadiusIndexCostPow);
        bb.putInt(lMissileMaxDamageIndexCost);
        bb.putFloat(fltMissileMaxDamageIndexCostPow);
        bb.putInt(lInterceptorSpeedIndexCost);
        bb.putFloat(fltInterceptorSpeedIndexCostPow);
        bb.putInt(lInterceptorRangeIndexCost);
        bb.putFloat(fltInterceptorRangeIndexCostPow);
        bb.putFloat(fltMissilePrepTimePerMagnitude);
        bb.putFloat(fltInterceptorPrepTimePerMagnitude);
        bb.putInt(lHourlyBonusDiplomaticPresence);
        bb.putInt(lHourlyBonusPoliticalEngagement);
        bb.putInt(lHourlyBonusDefenderOfTheNation);
        bb.putInt(lHourlyBonusNuclearSuperpower);
        bb.putInt(lHourlyBonusWeeklyKillsBatch);
        bb.putInt(lHourlyBonusSurvivor);
        bb.putInt(lHourlyBonusHippy);
        bb.putInt(lHourlyBonusPeaceMaker);
        bb.putInt(lHourlyBonusWarMonger);
        bb.putInt(lHourlyBonusLoneWolf);
        bb.putFloat(fltLoneWolfDistance);

        PutFloatPropertyTable(bb, MissileSpeeds);
        PutFloatPropertyTable(bb, MissileRanges);
        PutFloatPropertyTable(bb, MissileBlastRadii);
        PutFloatPropertyTable(bb, NukeBlastRadii);
        PutShortPropertyTable(bb, MissileMaxDamages);
        PutFloatPropertyTable(bb, InterceptorSpeeds);
        PutFloatPropertyTable(bb, InterceptorRanges);

        bb.putInt(MissileTypes.size());
        for(MissileType missileType : MissileTypes.values())
        {
            bb.put(missileType.GetData());
        }

        bb.putInt(InterceptorTypes.size());
        for(InterceptorType interceptorType : InterceptorTypes.values())
        {
            bb.put(interceptorType.GetData());
        }

        cData = bb.array();
        

        //Compute checksum.
        CRC32 crc32 = new CRC32();
        crc32.update(cData, 0, lSize);
        lChecksum = (int)crc32.getValue();


        //Compute nuclear escalation radius.
        float fltBiggestNukeRadius = 0.0f;

        for(MissileType type : MissileTypes.values())
        {
            if(type.GetNuclear())
            {
                fltBiggestNukeRadius = Math.max(fltBiggestNukeRadius, GetBlastRadius(type));
            }
        }

        fltNuclearEscalationRadius = GetEMPRadiusMultiplier() * fltBiggestNukeRadius * 1.5f;
    }

    public void SetPort(int lPort) { this.lPort = lPort; }
    public int GetPort() { return lPort; }

    public String GetServerEmail() { return strServerEmailAddress; }

    public byte GetDebugFlags() { return cDebugFlags; }
    public int GetStartingWealth() { return lStartingWealth; }
    public int GetRespawnWealth() { return lRespawnWealth; }
    public int GetRespawnTime() { return lRespawnTime; }
    public int GetRespawnProtectionTime() { return lRespawnProtectionTime; }
    public int GetNoobProtectionTime() { return lRespawnProtectionTime; }
    public int GetHourlyWealth() { return lHourlyWealth; }
    public int GetCMSSystemCost() { return lCMSSystemCost; }
    public int GetSAMSystemCost() { return lSAMSystemCost; }
    public int GetCMSStructureCost() { return lCMSStructureCost; }
    public int GetNukeCMSStructureCost() { return lNukeCMSStructureCost; }
    public int GetSAMStructureCost() { return lSAMStructureCost; }
    public int GetSentryGunStructureCost() { return lSentryGunStructureCost; }
    public int GetOreMineStructureCost() { return lOreMineStructureCost; }
    public float GetInterceptorBaseHitChance() { return fltInterceptorBaseHitChance; }
    public float GetRubbleMinValue() { return fltRubbleMinValue; }
    public float GetRubbleMaxValue() { return fltRubbleMaxValue; }
    public int GetRubbleMinTime() { return lRubbleMinTime; }
    public int GetRubbleMaxTime() { return lRubbleMaxTime; }
    public float GetStructureSeparation() { return fltStructureSeparation; }
    public short GetPlayerBaseHP() { return nPlayerBaseHP; }
    public short GetStructureBaseHP() { return nStructureBaseHP; }
    public int GetStructureBootTime() { return lStructureBootTime; }
    public byte GetInitialMissileSlots() { return cInitialMissileSlots; }
    public byte GetInitialInterceptorSlots() { return cInitialInterceptorSlots; }
    public float GetRequiredAccuracy() { return fltRequiredAccuracy; }
    public int GetMinRadiationTime() { return lMinRadiationTime; }
    public int GetMaxRadiationTime() { return lMaxRadiationTime; }
    public int GetMissileUpgradeBaseCost() { return lMissileUpgradeBaseCost; }
    public byte GetMissileUpgradeCount() { return cMissileUpgradeCount; }
    public float GetResaleValue() { return fltResaleValue; }
    public int GetDecommissionTime() { return lDecommissionTime; }
    public int GetReloadTimeBase() { return lReloadTimeBase; }
    public int GetReloadTimeStage1() { return lReloadTimeStage1; }
    public int GetReloadTimeStage2() { return lReloadTimeStage2; }
    public int GetReloadTimeStage3() { return lReloadTimeStage3; }
    public int GetReloadStage1Cost() { return lReloadStage1Cost; }
    public int GetReloadStage2Cost() { return lReloadStage2Cost; }
    public int GetReloadStage3Cost() { return lReloadStage3Cost; }
    public float GetRepairSalvageDistance() { return fltRepairSalvageDistance; }
    public int GetMissileSiteMaintenanceCost() { return lMissileSiteMaintenanceCost; }
    public int GetSAMSiteMaintenanceCost() { return lSAMSiteMaintenanceCost; }
    public int GetSentryGunMaintenanceCost() { return lSentryGunMaintenanceCost; }
    public int GetOreMineMaintenanceCost() { return lOreMineMaintenanceCost; }
    public int GetHealthInterval() { return lHealthInterval; }
    public int GetRadiationInterval() { return lRadiationInterval; }
    public int GetPlayerRepairCost() { return lPlayerRepairCost; }
    public int GetStructureRepairCost() { return lStructureRepairCost; }
    public long GetAWOLTime() { return oAWOLTime; }
    public long GetRemoveTime() { return oRemoveTime; }
    public int GetNukeUpgradeCost() { return lNukeUpgradeCost; }
    public int GetAllianceCooloffTime() { return lAllianceCooloffTime; }
    public int GetMissileNuclearCost() { return lMissileNuclearCost; }
    public int GetMissileTrackingCost() { return lMissileTrackingCost; }
    public int GetMissileECMCost() { return lMissileECMCost; }
    public float GetEMPChance() { return fltEMPChance; }
    public float GetEMPRadiusMultiplier() { return fltEMPRadiusMultiplier; }
    public float GetECMInterceptorChanceReduction() { return fltECMInterceptorChanceReduction; }
    public float GetManualInterceptorChanceIncrease() { return fltManualInterceptorChanceIncrease; }
    public int GetSentryGunReloadTime() { return lSentryGunReloadTime; }
    public float GetSentryGunRange() { return fltSentryGunRange; }
    public float GetSentryGunHitChance() { return fltSentryGunHitChance; }
    public float GetOreMineRadius() { return fltOreMineRadius; }
    public int GetMaxOreValue() { return lMaxOreValue; }
    public int GetOreMineGenerateTime() { return lOreMineGenerateTime; }
    public int GetOreMinExpiry() { return lOreMinExpiry; }
    public int GetOreMaxExpiry() { return lOreMaxExpiry; }
    public int GetMissileSpeedIndexCost() { return lMissileSpeedIndexCost; }
    public float GetMissileSpeedIndexCostPow() { return fltMissileSpeedIndexCostPow; }
    public int GetMissileRangeIndexCost() { return lMissileRangeIndexCost; }
    public float GetMissileRangeIndexCostPow() { return fltMissileRangeIndexCostPow; }
    public int GetMissileBlastRadiusIndexCost() { return lMissileBlastRadiusIndexCost; }
    public float GetMissileBlastRadiusIndexCostPow() { return fltMissileBlastRadiusIndexCostPow; }
    public int GetNukeBlastRadiusIndexCost() { return lNukeBlastRadiusIndexCost; }
    public float GetNukeBlastRadiusIndexCostPow() { return fltNukeBlastRadiusIndexCostPow; }
    public int GetMissileMaxDamageIndexCost() { return lMissileMaxDamageIndexCost; }
    public float GetMissileMaxDamageIndexCostPow() { return fltMissileMaxDamageIndexCostPow; }
    public int GetInterceptorSpeedIndexCost() { return lInterceptorSpeedIndexCost; }
    public float GetInterceptorSpeedIndexCostPow() { return fltInterceptorSpeedIndexCostPow; }
    public int GetInterceptorRangeIndexCost() { return lInterceptorRangeIndexCost; }
    public float GetInterceptorRangeIndexCostPow() { return fltInterceptorRangeIndexCostPow; }
    public float GetMissilePrepTimePerMagnitude() { return fltMissilePrepTimePerMagnitude; }
    public float GetInterceptorPrepTimePerMagnitude() { return fltInterceptorPrepTimePerMagnitude; }
    public int GetHourlyBonusDiplomaticPresence() { return lHourlyBonusDiplomaticPresence; }
    public int GetHourlyBonusPoliticalEngagement() { return lHourlyBonusPoliticalEngagement; }
    public int GetHourlyBonusDefenderOfTheNation() { return lHourlyBonusDefenderOfTheNation; }
    public int GetHourlyBonusNuclearSuperpower() { return lHourlyBonusNuclearSuperpower; }
    public int GetHourlyBonusWeeklyKillsBatch() { return lHourlyBonusWeeklyKillsBatch; }
    public int GetHourlyBonusSurvivor() { return lHourlyBonusSurvivor; }
    public int GetHourlyBonusHippy() { return lHourlyBonusHippy; }
    public int GetHourlyBonusPeaceMaker() { return lHourlyBonusPeaceMaker; }
    public int GetHourlyBonusWarMonger() { return lHourlyBonusWarMonger; }
    public int GetHourlyBonusLoneWolf() { return lHourlyBonusLoneWolf; }
    public float GetLoneWolfDistance() { return fltLoneWolfDistance; }

    public float GetOreMineDiameter() { return fltOreMineDiameter; }
    public float GetNuclearEscalationRadius() { return fltNuclearEscalationRadius; }

    public Collection<MissileType> GetMissileTypes() { return MissileTypes.values(); }
    public Collection<InterceptorType> GetInterceptorTypes() { return InterceptorTypes.values(); }

    public MissileType GetMissileType(byte cID) { return MissileTypes.get(cID); }
    public InterceptorType GetInterceptorType(byte cID) { return InterceptorTypes.get(cID); }

    public int GetSize() { return lSize; }
    public byte[] GetData() { return cData; }
    public int GetChecksum() { return lChecksum; }

    public int GetMissileCost(MissileType type)
    {
        float fltResult = (float)(Math.pow(type.GetSpeedIndex(), fltMissileSpeedIndexCostPow) * (float)lMissileSpeedIndexCost);
        fltResult += (float)(Math.pow(type.GetRangeIndex(), fltMissileRangeIndexCostPow) * (float)lMissileRangeIndexCost);

        if(type.GetNuclear())
        {
            fltResult += (float)(Math.pow(type.GetBlastRadiusIndex(), fltNukeBlastRadiusIndexCostPow) * (float)lNukeBlastRadiusIndexCost);
        }
        else
        {
            fltResult += (float)(Math.pow(type.GetBlastRadiusIndex(), fltMissileBlastRadiusIndexCostPow) * (float)lMissileBlastRadiusIndexCost);
        }

        fltResult += (float)(Math.pow(type.GetMaxDamageIndex(), fltMissileMaxDamageIndexCostPow) * (float)lMissileMaxDamageIndexCost);

        int lResult = (int)(fltResult + 0.5f);
        lResult += type.GetNuclear() ? lMissileNuclearCost : 0;
        lResult += type.GetTracking() ? lMissileTrackingCost : 0;
        lResult += type.GetECM() ? lMissileECMCost : 0;
        return lResult;
    }

    public int GetInterceptorCost(InterceptorType type)
    {
        float fltResult = (float)(Math.pow(type.GetSpeedIndex(), fltInterceptorSpeedIndexCostPow) * (float)lInterceptorSpeedIndexCost);
        fltResult += (float)(Math.pow(type.GetRangeIndex(), fltInterceptorRangeIndexCostPow) * (float)lInterceptorRangeIndexCost);

        int lResult = (int)(fltResult + 0.5f);
        return lResult;
    }

    public int GetMissileCost(byte cMissileType)
    {
        MissileType type = MissileTypes.get(cMissileType);
        if(type != null)
            return GetMissileCost(type);
        return Integer.MAX_VALUE;
    }

    public int GetInterceptorCost(byte cInterceptorType)
    {
        InterceptorType type = InterceptorTypes.get(cInterceptorType);
        if(type != null)
            return GetInterceptorCost(type);
        return Integer.MAX_VALUE;
    }

    public int GetMissilePrepTime(MissileType type)
    {
        return (int)(((float)type.GetFeatureMagnitude() * fltMissilePrepTimePerMagnitude * (float)Defs.MS_PER_MIN) + 0.5f);
    }

    public int GetInterceptorPrepTime(InterceptorType type)
    {
        return (int)(((float)type.GetFeatureMagnitude() * fltInterceptorPrepTimePerMagnitude * (float)Defs.MS_PER_MIN) + 0.5f);
    }

    public int GetMaintenanceCost(Structure structure)
    {
        if(structure instanceof MissileSite)
        {
            return lMissileSiteMaintenanceCost;
        }
        else if(structure instanceof SAMSite)
        {
            return lSAMSiteMaintenanceCost;
        }
        else if(structure instanceof SentryGun)
        {
            return lSentryGunMaintenanceCost;
        }
        else if(structure instanceof OreMine)
        {
            return lOreMineMaintenanceCost;
        }

        throw new RuntimeException("Queried maintenance cost of an unspecified structure.");
    }

    public float GetMissileSpeed(byte cIndex)
    {
        return MissileSpeeds.get(cIndex);
    }

    public float GetMissileSpeed(MissileType type)
    {
        return MissileSpeeds.get(type.GetSpeedIndex());
    }

    public float GetMissileRange(byte cIndex)
    {
        return MissileRanges.get(cIndex);
    }

    public float GetMissileRange(MissileType type)
    {
        return MissileRanges.get(type.GetRangeIndex());
    }

    public float GetBlastRadius(MissileType type)
    {
        if(type.GetNuclear())
            return NukeBlastRadii.get(type.GetBlastRadiusIndex());

        return MissileBlastRadii.get(type.GetBlastRadiusIndex());
    }

    public short GetMissileMaxDamage(MissileType type)
    {
        return MissileMaxDamages.get(type.GetMaxDamageIndex());
    }

    public float GetInterceptorSpeed(byte cIndex)
    {
        return InterceptorSpeeds.get(cIndex);
    }

    public float GetInterceptorSpeed(InterceptorType type)
    {
        return InterceptorSpeeds.get(type.GetSpeedIndex());
    }

    public float GetInterceptorRange(byte cIndex)
    {
        return InterceptorRanges.get(cIndex);
    }

    public float GetInterceptorRange(InterceptorType type)
    {
        return InterceptorRanges.get(type.GetRangeIndex());
    }

    public List<LaunchBannedApp> GetMinorCheatingApps() { return MinorCheatingApps; }
    public List<LaunchBannedApp> GetMajorCheatingApps() { return MajorCheatingApps; }
}
