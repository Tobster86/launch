/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package storage;

/**
 *
 * @author tobster
 */
public class XMLDefs
{
    public static final String GAME = "Game";
    
    public static final String PORT = "Port";
    public static final String SERVER_EMAIL = "ServerEmail";
    public static final String VARIANT = "Variant";
    public static final String DEBUG_FLAGS = "DebugFlags";
    public static final String STARTING_WEALTH = "StartingWealth";
    public static final String RESPAWN_WEALTH = "RespawnWealth";
    public static final String RESPAWN_TIME = "RespawnTime";
    public static final String RESPAWN_PROTECTION_TIME = "RespawnProtectionTime";
    public static final String NOOB_PROTECTION_TIME = "NoobProtectionTime";
    public static final String HOURLY_WEALTH = "HourlyWealth";
    public static final String CMS_SYSTEM_COST = "CMSSystemCost";
    public static final String SAM_SYSTEM_COST = "SAMSystemCost";
    public static final String CMS_STRUCTURE_COST = "CMSStructureCost";
    public static final String NCMS_STRUCTURE_COST = "NCMSStructureCost";
    public static final String SAM_STRUCTURE_COST = "SAMStructureCost";
    public static final String SENTRY_GUN_STRUCTURE_COST = "SentryGunStructureCost";
    public static final String ORE_MINE_STRUCTURE_COST = "OreMineStructureCost";
    public static final String INTERCEPTOR_BASE_HIT_CHANCE = "InterceptorBaseHitChance";
    public static final String RUBBLE_MIN_VALUE = "RubbleMinValue";
    public static final String RUBBLE_MAX_VALUE = "RubbleMaxValue";
    public static final String RUBBLE_MIN_TIME = "RubbleMinTime";
    public static final String RUBBLE_MAX_TIME = "RubbleMaxTime";
    public static final String STRUCTURE_SEPARATION = "StructureSeparation";
    public static final String PLAYER_BASE_HP = "PlayerBaseHP";
    public static final String STRUCTURE_BASE_HP = "StructureBaseHP";
    public static final String STRUCTURE_BOOT_TIME = "StructureBootTime";
    public static final String INITIAL_CSM_SLOTS = "InitialCMSSlots";
    public static final String INITIAL_SAM_SLOTS = "InitialSAMSlots";
    public static final String REQUIRED_ACCURACY = "RequiredAccuracy";
    public static final String MIN_RADIATION_TIME = "MinRadiationTime";
    public static final String MAX_RADIATION_TIME = "MaxRadiationTime";
    public static final String MISSILE_SLOT_UPGRADE_BASE_COST = "MissileSlotUpgradeBaseCost";
    public static final String MISSILE_SLOT_UPGRADE_COUNT = "MissileSlotUpgradeCount";
    public static final String RESALE_VALUE = "ResaleValue";
    public static final String DECOMMISSION_TIME = "DecommissionTime";
    public static final String RELOAD_TIME_BASE = "ReloadTimeBase";
    public static final String RELOAD_TIME_STAGE1 = "ReloadTimeStage1";
    public static final String RELOAD_TIME_STAGE2 = "ReloadTimeStage2";
    public static final String RELOAD_TIME_STAGE3 = "ReloadTimeStage3";
    public static final String RELOAD_STAGE1_COST = "ReloadStage1Cost";
    public static final String RELOAD_STAGE2_COST = "ReloadStage2Cost";
    public static final String RELOAD_STAGE3_COST = "ReloadStage3Cost";
    public static final String REPAIR_SALVAGE_DISTANCE = "RepairSalvageDistance";
    public static final String MISSILE_SITE_MAINTENANCE_COST = "MissileSiteMaintenanceCost";
    public static final String SAM_SITE_MAINTENANCE_COST = "SAMSiteMaintenanceCost";
    public static final String SENTRY_GUN_MAINTENANCE_COST = "SentryGunMaintenanceCost";
    public static final String ORE_MINE_MAINTENANCE_COST = "OreMineMaintenanceCost";
    public static final String HEALTH_INTERVAL = "HealthInterval";
    public static final String RADIATION_INTERVAL = "RadiationInterval";
    public static final String PLAYER_REPAIR_COST = "PlayerRepairCost";
    public static final String STRUCTURE_REPAIR_COST = "StructureRepairCost";
    public static final String AWOL_TIME = "AWOLTime";
    public static final String REMOVE_TIME = "RemoveTime";
    public static final String NUKE_UPGRADE_COST = "NukeUpgradeCost";
    public static final String ALLIANCE_COOLOFF_TIME = "AllianceCooloffTime";
    public static final String MISSILE_SPEED_COST = "MissileSpeedCost";
    public static final String MISSILE_RANGE_COST = "MissileRangeCost";
    public static final String MISSILE_BLAST_RADIUS_COST = "MissileBlastRadiusCost";
    public static final String MISSILE_BLAST_RADIUS_NUKES_COST = "MissileBlastRadiusNukesCost";
    public static final String MISSILE_MAX_DAMAGE_COST = "MissileMaxDamageCost";
    public static final String MISSILE_NUCLEAR_COST = "MissileNuclearCost";
    public static final String MISSILE_TRACKING_COST = "MissileTrackingCost";
    public static final String MISSILE_ECM_COST = "MissileECMCost";
    public static final String INTERCEPTOR_SPEED_COST_ABOVE_THRESHOLD = "InterceptorSpeedCostAboveThreshold";
    public static final String INTERCEPTOR_SPEED_COST_BELOW_THRESHOLD = "InterceptorSpeedCostBelowThreshold";
    public static final String INTERCEPTOR_SPEED_COST_THRESHOLD = "InterceptorSpeedCostThreshold";
    public static final String INTERCEPTOR_RANGE_COST_ABOVE_THRESHOLD = "InterceptorRangeCostAboveThreshold";
    public static final String INTERCEPTOR_RANGE_COST_BELOW_THRESHOLD = "InterceptorRangeCostBelowThreshold";
    public static final String INTERCEPTOR_RANGE_COST_THRESHOLD = "InterceptorRangeCostThreshold";
    public static final String EMP_CHANCE = "EMPChance";
    public static final String EMP_RADIUS_MULTIPLIER = "EMPRadiusMultiplier";
    public static final String ECM_INTERCEPTOR_CHANCE_REDUCTION = "ECMInterceptorChanceReduction";
    public static final String MANUAL_INTERCEPTOR_CHANCE_INCREASE = "ManualInterceptorChanceIncrease";
    public static final String SENTRY_GUN_RELOAD_TIME = "SentryGunReloadTime";
    public static final String SENTRY_GUN_RANGE = "SentryGunRange";
    public static final String SENTRY_GUN_HIT_CHANCE = "SentryGunHitChance";
    public static final String ORE_MINE_RADIUS = "OreMineRadius";
    public static final String MAX_ORE_VALUE = "MaxOreValue";
    public static final String ORE_MINE_GENERATE_TIME = "OreMineGenerateTime";
    public static final String ORE_MIN_EXPIRY = "OreMinExpiry";
    public static final String ORE_MAX_EXPIRY = "OreMaxExpiry";
    public static final String MISSILE_SPEED_INDEX_COST = "MissileSpeedIndexCost";
    public static final String MISSILE_SPEED_INDEX_COST_POW = "MissileSpeedIndexCostPow";
    public static final String MISSILE_RANGE_INDEX_COST = "MissileRangeIndexCost";
    public static final String MISSILE_RANGE_INDEX_COST_POW = "MissileRangeIndexCostPow";
    public static final String MISSILE_BLAST_RADIUS_INDEX_COST = "MissileBlastRadiusIndexCost";
    public static final String MISSILE_BLAST_RADIUS_INDEX_COST_POW = "MissileBlastRadiusIndexCostPow";
    public static final String NUKE_BLAST_RADIUS_INDEX_COST = "NukeBlastRadiusIndexCost";
    public static final String NUKE_BLAST_RADIUS_INDEX_COST_POW = "NukeBlastRadiusIndexCostPow";
    public static final String MISSILE_MAX_DAMAGE_INDEX_COST = "MissileMaxDamageIndexCost";
    public static final String MISSILE_MAX_DAMAGE_INDEX_COST_POW = "MissileMaxDamageIndexCostPow";
    public static final String INTERCEPTOR_SPEED_INDEX_COST = "InterceptorSpeedIndexCost";
    public static final String INTERCEPTOR_SPEED_INDEX_COST_POW = "InterceptorSpeedIndexCostPow";
    public static final String INTERCEPTOR_RANGE_INDEX_COST = "InterceptorRangeIndexCost";
    public static final String INTERCEPTOR_RANGE_INDEX_COST_POW = "InterceptorRangeIndexCostPow";
    public static final String MISSILE_PREP_TIME_PER_MAGNITUDE = "MissilePrepTimePerMagnitude";
    public static final String INTERCEPTOR_PREP_TIME_PER_MAGNITUDE = "InterceptorPrepTimePerMagnitude";
    public static final String HOURLY_BONUS_DIPLOMATIC_PRESENCE = "HourlyBonusDiplomaticPresence";
    public static final String HOURLY_BONUS_POLITICAL_ENGAGEMENT = "HourlyBonusPoliticalEngagement";
    public static final String HOURLY_BONUS_DEFENDER_OF_THE_NATION = "HourlyBonusDefenderOfTheNation";
    public static final String HOURLY_BONUS_NUCLEAR_SUPERPOWER = "HourlyBonusNuclearSuperpower";
    public static final String HOURLY_BONUS_WEEKLY_KILLS_BATCH = "HourlyBonusWeeklyKillsBatch";
    public static final String HOURLY_BONUS_SURVIVOR = "HourlyBonusSurvivor";
    public static final String HOURLY_BONUS_HIPPY = "HourlyBonusHippy";
    public static final String HOURLY_BONUS_PEACE_MAKER = "HourlyBonusPeaceMaker";
    public static final String HOURLY_BONUS_WAR_MONGER = "HourlyBonusWarMonger";
    public static final String HOURLY_BONUS_LONE_WOLF = "HourlyBonusLoneWolf";
    public static final String LONE_WOLF_DISTANCE = "LoneWolfDistance";
    
    public static final String MINOR_BANNED_APP = "MinorBannedApp";
    public static final String MAJOR_BANNED_APP = "MajorBannedApp";
    public static final String SIGNATURE = "Signature";
    public static final String DESCRIPTION = "Description";
    
    public static final String ID = "ID";
    public static final String LATITUDE = "Latitude";
    public static final String LONGITUDE = "Longitude";
    public static final String NAME = "Name";
    public static final String ASSET_ID = "AssetID";
    
    public static final String USERS = "Users";
    public static final String USER = "User";
    public static final String IMEI = "IMEI";
    public static final String PLAYERID = "PlayerID";
    public static final String BAN_STATE = "BanState";
    public static final String NEXT_BAN_TIME = "NextBanTime";
    public static final String BAN_DURATION_REMAINING = "BanDurationRemaining";
    public static final String BAN_REASON = "BanReason";
    public static final String LAST_IP = "LastIP";
    public static final String LAST_CONNECTION_MOBILE = "LastConnectionMobile";
    public static final String LAST_CHECKED = "LastChecked";
    public static final String LAST_CHECK_FAILED = "LastCheckFailed";
    public static final String CHECK_API_FAILED = "CheckAPIFailed";
    public static final String PROSCRIBED = "Proscribed";
    public static final String CHECK_FAIL_CODE = "CheckFailCode";
    public static final String PROFILE_MATCH = "ProfileMatch";
    public static final String BASIC_INTEGRITY = "BasicIntegrity";
    public static final String APPROVED = "Approved";
    public static final String EXPIRED = "Expired";
    public static final String DEVICE_HASH = "DeviceHash";
    public static final String APP_LIST_HASH = "AppListHash";
    public static final String REPORTS = "Reports";
    public static final String REPORT = "Report";
    public static final String TIME_START = "TimeStart";
    public static final String TIME_END = "TimeEnd";
    public static final String MESSAGE = "Message";
    public static final String IS_MAJOR = "IsMajor";
    public static final String LEFT_ID = "LeftID";
    public static final String RIGHT_ID = "RightID";
    public static final String TIMES = "Times";
    
    public static final String ALLIANCES = "Alliances";
    public static final String ALLIANCE = "Alliance";
    
    public static final String TREATIES = "Treaties";
    public static final String TREATY = "Treaty";
    public static final String ALLIANCE1 = "Alliance1";
    public static final String ALLIANCE2 = "Alliance2";
    
    public static final String PLAYERS = "Players";
    public static final String PLAYER = "Player";
    public static final String POSITION = "Position";
    public static final String HP = "HP";
    public static final String MAX_HP = "MaxHP";
    public static final String AVATAR = "Avatar";
    public static final String WEALTH = "Wealth";
    public static final String LAST_SEEN = "LastSeen";
    public static final String STATE_CHANGE = "StateChange";
    public static final String ALLIANCE_ID = "AllianceID";
    public static final String FLAGS = "Flags";
    public static final String FLAGS1 = "Flags1";
    public static final String FLAGS2 = "Flags2";
    public static final String KILLS = "Kills";
    public static final String DEATHS = "Deaths";
    public static final String OFFENCE_SPENDING = "OffenceSpending";
    public static final String DEFENCE_SPENDING = "DefenceSpending";
    public static final String DAMAGE_INFLICTED = "DamageInflicted";
    public static final String DAMAGE_RECEIVED = "DamageReceived";
    
    public static final String COST = "Cost";
    public static final String RANGE = "Range";
    public static final String BLAST_RADIUS = "BlastRadius";
    public static final String MAX_DAMAGE = "MaxDamage";
    public static final String RELOAD_TIME = "ReloadTime";
    
    public static final String MISSILE_SPEED = "MissileSpeed";
    public static final String MISSILE_RANGE = "MissileRange";
    public static final String MISSILE_BLAST_RADIUS = "MissileBlastRadius";
    public static final String NUKE_BLAST_RADIUS = "NukeBlastRadius";
    public static final String MISSILE_MAX_DAMAGE = "MissileMaxDamage";
    public static final String INTERCEPTOR_SPEED = "InterceptorSpeed";
    public static final String INTERCEPTOR_RANGE = "InterceptorRange";
    
    public static final String MISSILE_TYPE = "MissileType";
    public static final String SPEED = "Speed";
    public static final String PREP_TIME = "PrepTime";
    public static final String NUCLEAR = "Nuclear";
    public static final String TRACKING = "Tracking";
    public static final String ECM = "ECM";
    public static final String CHARGE_OWNER_TIME = "ChargeOwnerTime";
    public static final String PURCHASABLE = "Purchasable";
    public static final String SPEED_INDEX = "SpeedIndex";
    public static final String BLAST_RADIUS_INDEX = "BlastRadiusIndex";
    public static final String RANGE_INDEX = "RangeIndex";
    public static final String MAX_DAMAGE_INDEX = "MaxDamageIndex";
    
    public static final String INTERCEPTOR_TYPE = "InterceptorType";
    
    public static final String TYPE = "Type";
    public static final String TARGET = "Target";
    public static final String ORIGIN = "Origin";
    public static final String OWNER_ID = "OwnerID";
    public static final String TIME_TO_TARGET = "TimeToTarget";
    
    public static final String MISSILES = "Missiles";
    public static final String MISSILE = "Missile";
    
    public static final String INTERCEPTORS = "Interceptors";
    public static final String INTERCEPTOR = "Interceptor";
    public static final String TARGET_ID = "TargetID";
    public static final String PLAYER_LAUNCHED = "PlayerLaunched";
    
    public static final String MISSILE_SITES = "MissileSites";
    public static final String MISSILE_SITE = "MissileSite";
    public static final String STATE = "State";
    public static final String STATE_TIME = "StateTime";
    
    public static final String SAM_SITES = "SAMSites";
    public static final String SAM_SITE = "SAMSite";
    public static final String MODE = "Mode";
    
    public static final String ORE_MINES = "OreMines";
    public static final String ORE_MINE = "OreMine";
    public static final String GENERATE_TIME = "GenerateTime";
    
    public static final String SENTRY_GUNS = "SentryGuns";
    public static final String SENTRY_GUN = "SentryGun";
    
    public static final String LOOTS = "Loots";
    public static final String LOOT = "Loot";
    public static final String VALUE = "Value";
    public static final String EXPIRY = "Expiry";
    
    public static final String RADIATIONS = "Radiations";
    public static final String RADIATION = "Radiation";
    public static final String RADIUS = "Radius";
    
    public static final String MISSILE_SYSTEM = "MissileSystem";
    public static final String INTERCEPTOR_SYSTEM = "InterceptorSystem";
    public static final String RELOAD_REMAINING = "ReloadRemaining";
    public static final String SLOT_COUNT = "SlotCount";
    public static final String SLOTS = "Slots";
    public static final String SLOT = "Slot";
    public static final String NUMBER = "Number";
    
    public static final String EMULATORS = "Emulators";
    public static final String MULTI_ACCOUNTORS = "MultiAccountors";
    public static final String SPOOF_APPS = "SpoofApps";
    public static final String IP = "IP";
    public static final String EMULATOR = "Emulator";
    public static final String MULTI_ACCOUNTOR = "MultiAccountor";
    public static final String SPOOF_APP = "SpoofApp";
    
    public static final String KILLS1 = "Kills1";
    public static final String DEATHS1 = "Deaths1";
    public static final String OFFENCE_SPENDING1 = "OffenceSpending1";
    public static final String DEFENCE_SPENDING1 = "DefenceSpending1";
    public static final String DAMAGE_INFLICTED1 = "DamageInflicted1";
    public static final String DAMAGE_RECEIVED1 = "DamageReceived1";
    public static final String INCOME1 = "Income1";
    public static final String KILLS2 = "Kills2";
    public static final String DEATHS2 = "Deaths2";
    public static final String OFFENCE_SPENDING2 = "OffenceSpending2";
    public static final String DEFENCE_SPENDING2 = "DefenceSpending2";
    public static final String DAMAGE_INFLICTED2 = "DamageInflicted2";
    public static final String DAMAGE_RECEIVED2 = "DamageReceived2";
    public static final String INCOME2 = "Income2";
}
