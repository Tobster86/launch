/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package game.LaunchGame;

import launch.comm.clienttasks.Task;
import launch.game.Config;
import launch.game.LaunchClientAppInterface;
import launch.game.LaunchClientGame;
import launch.game.LaunchGame;
import launch.game.User;
import launch.game.entities.LaunchEntity;
import launch.game.entities.Player;
import launch.game.treaties.Treaty;
import launch.utilities.LaunchClientLocation;
import launch.utilities.LaunchEvent;
import launch.utilities.LaunchReport;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tobster
 */
public class GetWeeklyKillsBonus {
    
    static LaunchGame game;
    static Player player;
    
    private static class TestConfig extends Config
    {
        public TestConfig()
        {
            lHourlyBonusWeeklyKillsBatch = 10;
        }
    }
    
    public GetWeeklyKillsBonus() {
    }
    
    @BeforeClass
    public static void setUpClass()
    {
        TestConfig testConfig = new TestConfig();
        
        game = new LaunchClientGame(testConfig, new LaunchClientAppInterface() {
            @Override
            public void GameTicked(int lMS) {
            }

            @Override
            public void SaveConfig(Config config) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void SaveAvatar(int lAvatarID, byte[] cData) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void AvatarUploaded(int lAvatarID) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void SaveImage(int lImageID, byte[] cData) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public boolean PlayerLocationAvailable() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public LaunchClientLocation GetPlayerLocation() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void EntityUpdated(LaunchEntity entity) {
            }

            @Override
            public void EntityRemoved(LaunchEntity entity) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void TreatyUpdated(Treaty treaty) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void MajorChanges() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void Authenticated() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void AccountUnregistered() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void AccountNameTaken() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void MajorVersionMismatch() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void MinorVersionMismatch() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void ActionSucceeded() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void ActionFailed() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void ShowTaskMessage(Task.TaskMessage message) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void DismissTaskMessage() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void NewEvent(LaunchEvent event) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void NewReport(LaunchReport report) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void Quit() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void AllianceCreated() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public String GetProcessNames() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public boolean GetConnectionMobile() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void DeviceChecksRequested() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void DisplayGeneralError() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void TempBanned(String strReason, long oDuration) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void PermBanned(String strReason) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void ReceiveUser(User user) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        }, null, "test@launch.com", 1);
        
        player = new Player(1, (short)100, "Bob", 1, 10000);
        game.AddPlayer(player);
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }
    
    @Test
    public void test()
    {
        assertEquals(0, game.GetWeeklyKillsBonus(player));
        player.IncrementKills();
        assertEquals(10, game.GetWeeklyKillsBonus(player));
        player.IncrementKills();
        assertEquals(20, game.GetWeeklyKillsBonus(player));
        player.IncrementKills();
        assertEquals(20, game.GetWeeklyKillsBonus(player));
        player.IncrementKills();
        assertEquals(30, game.GetWeeklyKillsBonus(player));
        player.IncrementKills();
        assertEquals(30, game.GetWeeklyKillsBonus(player));
        player.IncrementKills();
        assertEquals(30, game.GetWeeklyKillsBonus(player));
        player.IncrementKills();
        assertEquals(30, game.GetWeeklyKillsBonus(player));
        player.IncrementKills();
        assertEquals(40, game.GetWeeklyKillsBonus(player));
        player.IncrementKills();
        assertEquals(40, game.GetWeeklyKillsBonus(player));
        player.IncrementKills();
        assertEquals(40, game.GetWeeklyKillsBonus(player));
        player.IncrementKills();
        assertEquals(40, game.GetWeeklyKillsBonus(player));
        player.IncrementKills();
        assertEquals(40, game.GetWeeklyKillsBonus(player));
        player.IncrementKills();
        assertEquals(40, game.GetWeeklyKillsBonus(player));
        player.IncrementKills();
        assertEquals(40, game.GetWeeklyKillsBonus(player));
        player.IncrementKills();
        assertEquals(40, game.GetWeeklyKillsBonus(player));
        player.IncrementKills();
        assertEquals(50, game.GetWeeklyKillsBonus(player));
        player.IncrementKills();
        assertEquals(50, game.GetWeeklyKillsBonus(player));
        player.IncrementKills();
        assertEquals(50, game.GetWeeklyKillsBonus(player));
        player.IncrementKills();
        assertEquals(50, game.GetWeeklyKillsBonus(player));
        player.IncrementKills();
        assertEquals(50, game.GetWeeklyKillsBonus(player));
        player.IncrementKills();
        assertEquals(50, game.GetWeeklyKillsBonus(player));
        player.IncrementKills();
        assertEquals(50, game.GetWeeklyKillsBonus(player));
        player.IncrementKills();
        assertEquals(50, game.GetWeeklyKillsBonus(player));
        player.IncrementKills();
        assertEquals(50, game.GetWeeklyKillsBonus(player));
        player.IncrementKills();
        assertEquals(50, game.GetWeeklyKillsBonus(player));
        player.IncrementKills();
        assertEquals(50, game.GetWeeklyKillsBonus(player));
        player.IncrementKills();
        assertEquals(50, game.GetWeeklyKillsBonus(player));
        player.IncrementKills();
        assertEquals(50, game.GetWeeklyKillsBonus(player));
        player.IncrementKills();
        assertEquals(50, game.GetWeeklyKillsBonus(player));
        player.IncrementKills();
        assertEquals(50, game.GetWeeklyKillsBonus(player));
        player.IncrementKills();
        assertEquals(50, game.GetWeeklyKillsBonus(player));
        player.IncrementKills();
        assertEquals(60, game.GetWeeklyKillsBonus(player));
    }
}
