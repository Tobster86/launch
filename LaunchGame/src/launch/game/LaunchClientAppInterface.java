/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.game;

import launch.comm.clienttasks.Task.TaskMessage;
import launch.game.entities.LaunchEntity;
import launch.game.treaties.Treaty;
import launch.utilities.LaunchClientLocation;
import launch.utilities.LaunchEvent;
import launch.utilities.LaunchReport;

/**
 *
 * @author tobster
 */
public interface LaunchClientAppInterface
{
    void GameTicked(int lMS);
    
    void SaveConfig(Config config);
    void SaveAvatar(int lAvatarID, byte[] cData);
    void AvatarUploaded(int lAvatarID);
    void SaveImage(int lImageID, byte[] cData);
    
    boolean PlayerLocationAvailable();
    LaunchClientLocation GetPlayerLocation();
    
    void EntityUpdated(LaunchEntity entity);
    void EntityRemoved(LaunchEntity entity);
    void TreatyUpdated(Treaty treaty);
    
    void MajorChanges();
    void Authenticated();
    void AccountUnregistered();
    void AccountNameTaken();
    void MajorVersionMismatch();
    void MinorVersionMismatch();
    
    void ActionSucceeded();
    void ActionFailed();
    
    void ShowTaskMessage(TaskMessage message);
    void DismissTaskMessage();
    
    void NewEvent(LaunchEvent event);
    void NewReport(LaunchReport report);
    
    void Quit();
    
    void AllianceCreated();
    
    String GetProcessNames();
    
    boolean GetConnectionMobile();
    
    void DeviceChecksRequested();
    
    void DisplayGeneralError();
    
    void TempBanned(String strReason, long oDuration);
    void PermBanned(String strReason);
    
    void ReceiveUser(User user);
}
