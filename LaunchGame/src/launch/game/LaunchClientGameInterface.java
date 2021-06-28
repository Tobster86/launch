/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.game;

import launch.comm.clienttasks.Task.TaskMessage;
import launch.game.entities.*;
import launch.game.treaties.*;
import launch.utilities.LaunchEvent;
import launch.utilities.LaunchClientLocation;
import launch.utilities.LaunchReport;

/**
 *
 * @author tobster
 */
public interface LaunchClientGameInterface
{
    void ReceivePlayer(Player player);
    void ReceiveMissile(Missile missile);
    void ReceiveInterceptor(Interceptor interceptor);
    void ReceiveMissileSite(MissileSite missileSite);
    void ReceiveSAMSite(SAMSite samSite);
    void ReceiveOreMine(OreMine oreMine);
    void ReceiveSentryGun(SentryGun sentryGun);
    void ReceiveLoot(Loot loot);
    void ReceiveRadiation(Radiation radiation);
    void ReceiveAlliance(Alliance alliance, boolean bMajor);
    void ReceiveTreaty(Treaty treaty);
    void ReceiveUser(User user);
    
    void RemovePlayer(int lID);
    void RemoveMissile(int lID);
    void RemoveInterceptor(int lID);
    void RemoveMissileSite(int lID);
    void RemoveSAMSite(int lID);
    void RemoveOreMine(int lID);
    void RemoveSentryGun(int lID);
    void RemoveLoot(int lID);
    void RemoveRadiation(int lID);
    
    void RemoveAlliance(int lID);
    void RemoveWar(int lID);
    
    boolean PlayerLocationAvailable();
    LaunchClientLocation GetPlayerLocation();
    int GetGameConfigChecksum();
    void SetConfig(Config config);
    void AvatarReceived(int lAvatarID, byte[] cData);
    void AvatarUploaded(int lAvatarID);
    void ImageReceived(int lImageID, byte[] cData);
    
    void Authenticated();
    void AccountUnregistered();
    void AccountNameTaken();
    void SetOurPlayerID(int lPlayerID);
    boolean GetReadyToUpdatePlayer();
    
    boolean VerifyVersion(short nMajorVersion, short nMinorVersion);
    void MajorVersionInvalid();
    
    void SetLatency(int lLatency);
    
    void SnapshotBegin();
    void SnapshotFinish();
    
    void ShowTaskMessage(TaskMessage message);
    void DismissTaskMessage();
    
    void EventReceived(LaunchEvent event);
    void ReportReceived(LaunchReport report);
    
    void AccountClosed();
    boolean ClosingAccount();
    
    void AllianceCreated();
    
    String GetProcessNames();
    
    boolean GetConnectionMobile();
    
    void DeviceCheckRequested();
    
    void DisplayGeneralError();
    
    void TempBanned(String strReason, long oDuration);
    void PermBanned(String strReason);
}
