/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package storage;

import java.util.List;
import launch.game.Config;
import launch.game.GeoCoord;
import launch.game.treaties.Treaty.Type;
import launch.game.systems.MissileSystem;
import launch.utilities.LaunchBannedApp;

/**
 *
 * @author tobster
 */
public interface GameLoadSaveListener
{
    void LoadError(String strDescription);
    void LoadWarning(String strDescription);
    void SaveError(String strDescription);
}
