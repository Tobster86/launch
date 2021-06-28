/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.game;

import java.net.InetAddress;
import java.util.List;
import launch.game.entities.Player;
import launch.utilities.LaunchClientLocation;
import launch.utilities.LaunchReport;
import launch.utilities.LocationSpoofCheck;


/**
 *
 * @author tobster
 */
public interface LaunchServerGameInterface
{
    User VerifyID(String strIMEI);
    int GetGameConfigChecksum();
    boolean CheckPlayerNameAvailable(String strPlayerName);
    User CreateAccount(String strIMEI, String strPlayerName, int lAvatarID);
    void UpdatePlayerLocation(int lPlayerID, LaunchClientLocation location);
    boolean ConstructMissileSite(int lPlayerID, boolean bNuclear);
    boolean ConstructSAMSite(int lPlayerID);
    boolean ConstructSentryGun(int lPlayerID);
    boolean ConstructOreMine(int lPlayerID);
    boolean PurchaseMissiles(int lPlayerID, int lMissileSiteID, byte cSlotNo, byte[] cMissileTypes);
    boolean PurchaseInterceptors(int lPlayerID, int lSAMSiteID, byte cSlotNo, byte[] cInterceptorTypes);
    boolean MissileSlotUpgrade(int lPlayerID, int lMissileSiteID);
    boolean InterceptorSlotUpgrade(int lPlayerID, int lSAMSiteID);
    boolean PlayerMissileSlotUpgrade(int lPlayerID);
    boolean PlayerInterceptorSlotUpgrade(int lPlayerID);
    boolean MissileReloadUpgrade(int lPlayerID, int lMissileSiteID);
    boolean InterceptorReloadUpgrade(int lPlayerID, int lSAMSiteID);
    boolean PlayerMissileReloadUpgrade(int lPlayerID);
    boolean PlayerInterceptorReloadUpgrade(int lPlayerID);
    boolean LaunchMissile(int lPlayerID, int lSiteID, byte cSlotNo, boolean bTracking, float fltTargetLatitude, float fltTargetLongitude, int lTargetID);
    boolean LaunchPlayerMissile(int lPlayerID, byte cSlotNo, boolean bTracking, float fltTargetLatitude, float fltTargetLongitude, int lTargetID);
    boolean LaunchInterceptor(int lPlayerID, int lSiteID, byte cSlotNo, int lTargetID);
    boolean LaunchPlayerInterceptor(int lPlayerID, byte cSlotNo, int lTargetID);
    boolean PurchaseMissileSystem(int lPlayerID);
    boolean PurchaseSAMSystem(int lPlayerID);
    boolean Respawn(int lPlayerID);
    boolean SellMissileSite(int lPlayerID, int lMissileSiteID);
    boolean SellSAMSite(int lPlayerID, int lSAMSiteID);
    boolean SellSentryGun(int lPlayerID, int lSentryGunID);
    boolean SellOreMine(int lPlayerID, int lOreMineID);
    boolean SellMissileSystem(int lPlayerID);
    boolean SellSAMSystem(int lPlayerID);
    boolean SellMissile(int lPlayerID, int lMissileSiteID, byte cSlotIndex);
    boolean SellInterceptor(int lPlayerID, int lSAMSiteID, byte cSlotIndex);
    boolean SetMissileSitesOnOff(int lPlayerID, List<Integer> SiteIDs, boolean bOnline);
    boolean SetSAMSitesOnOff(int lPlayerID, List<Integer> SiteIDs, boolean bOnline);
    boolean SetSentryGunsOnOff(int lPlayerID, List<Integer> SiteIDs, boolean bOnline);
    boolean SetOreMinesOnOff(int lPlayerID, List<Integer> SiteIDs, boolean bOnline);
    boolean RepairMissileSite(int lPlayerID, int lSiteID);
    boolean RepairSAMSite(int lPlayerID, int lSiteID);
    boolean RepairSentryGun(int lPlayerID, int lSiteID);
    boolean RepairOreMine(int lPlayerID, int lSiteID);
    boolean HealPlayer(int lPlayerID);
    boolean SetAvatar(int lPlayerID, int lAvatarID);
    boolean SetAllianceAvatar(int lPlayerID, int lAvatarID);
    boolean SetSAMSiteModes(int lPlayerID, List<Integer> SiteIDs, byte cMode);
    boolean SetSAMSiteName(int lPlayerID, int lSiteID, String strName);
    boolean SetMissileSiteName(int lPlayerID, int lSiteID, String strName);
    boolean SetSentryGunName(int lPlayerID, int lSiteID, String strName);
    boolean SetOreMineName(int lPlayerID, int lSiteID, String strName);
    boolean CloseAccount(int lPlayerID);
    boolean UpgradeToNuclear(int lPlayerID, int lMissileSiteID);
    boolean JoinAlliance(int lPlayerID, int lAllianceID);
    boolean LeaveAlliance(int lPlayerID);
    boolean DeclareWar(int lPlayerID, int lAllianceID);
    boolean ProposeAffiliation(int lPlayerID, int lAllianceID);
    boolean AcceptAffiliation(int lPlayerID, int lAllianceID);
    boolean RejectAffiliation(int lPlayerID, int lAllianceID);
    boolean Promote(int lPromotor, int lPromotee);
    boolean AcceptJoin(int lLeaderID, int lMemberID);
    boolean RejectJoin(int lLeaderID, int lMemberID);
    boolean Kick(int lLeaderID, int lMemberID);
    LaunchGame GetGame(); //ONLY TO BE USED IN THIS CONTEXT FOR GETTING LISTS OF ENTITIES OR CONFIG DATA.
    void BadAvatar(int lAvatarID);
    void BadImage(int lImageID);
    boolean CreateAlliance(int lCreatorID, String strName, String strDescription, int lAvatarID);
    boolean TempBan(int lPlayerID, String strReason, String strBanner);
    boolean PermaBan(int lPlayerID, String strReason, String strBanner);
    boolean Unban(int lPlayerID, String strUnbanner);
    boolean AvatarReset(int lPlayerAdminID, int lPlayerToResetID);
    boolean NameReset(int lPlayerAdminID, int lPlayerToResetID);
    void SpoofWarnings(int lPlayerID, LocationSpoofCheck spoofCheck);
    void MultiAccountingCheck(int lPlayerID);
    boolean TransferAccount(int lOldPlayerID, int lNewPlayerID);
    boolean ChangePlayerName(int lPlayerID, String strNewName);
    boolean ChangeAllianceName(int lPlayerID, String strNewName);
    boolean ChangeAllianceDescription(int lPlayerID, String strNewDescription);
    
    String GetPlayerName(int lPlayerID);
    User GetUser(int lPlayerID);
    void AdminReport(LaunchReport report);
    void NotifyDeviceChecksCompleteFailure(String strPlayerName);
    void NotifyDeviceChecksAPIFailure(String strPlayerName);
    void NotifyDeviceCheckFailure(User user);
    void NotifyIPProscribed(User user);
    void NotifyLocationProscribed(User user);
    void NotifyAccountRestricted(User user);
    
    boolean GetIpAddressProscribed(String strIPAddress);
    boolean GetLocationProscribed(GeoCoord geoLocation);
}
