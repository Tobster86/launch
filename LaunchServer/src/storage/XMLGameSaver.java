/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package storage;

import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import launch.game.Alliance;
import launch.game.Defs;
import launch.game.GeoCoord;
import launch.game.LaunchServerGame;
import launch.game.entities.*;
import launch.game.systems.MissileSystem;
import launch.game.User;
import launch.game.treaties.Treaty;
import launch.game.treaties.Treaty.Type;
import launch.game.treaties.War;
import launch.utilities.LaunchReport;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author tobster
 */
public class XMLGameSaver
{
    private static Document doc;
    
    public static void SaveGameToXMLFile(GameLoadSaveListener listener, LaunchServerGame game, String strGameFile)
    {
        try
        {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            //Create game node.
            doc = docBuilder.newDocument();
            Element eleGame = doc.createElement(XMLDefs.GAME);
            doc.appendChild(eleGame);
            
            //Save users.
            Element elements = AddNode(eleGame, XMLDefs.USERS);

            for(User user : game.GetUsers())
            {
                Player player = game.GetPlayer(user.GetPlayerID());
                String strPlayerName = player == null? "[DEAD ACCOUNT]" : player.GetName();
                
                Element eleUser = AddNode(elements, XMLDefs.USER, XMLDefs.NAME, strPlayerName);
                AddNode(eleUser, XMLDefs.IMEI, user.GetIMEI());
                AddNode(eleUser, XMLDefs.PLAYERID, user.GetPlayerID());
                AddNode(eleUser, XMLDefs.BAN_STATE, user.GetBanState().ordinal());
                AddNode(eleUser, XMLDefs.NEXT_BAN_TIME, user.GetNextBanTime());
                AddNode(eleUser, XMLDefs.BAN_DURATION_REMAINING, user.GetBanDurationRemaining());
                AddNode(eleUser, XMLDefs.BAN_REASON, user.GetBanReason());
                AddNode(eleUser, XMLDefs.LAST_IP, user.GetLastIP());
                AddNode(eleUser, XMLDefs.LAST_CONNECTION_MOBILE, user.GetLastTypeMobile());
                AddNode(eleUser, XMLDefs.LAST_CHECKED, user.GetDeviceCheckedDate());
                AddNode(eleUser, XMLDefs.LAST_CHECK_FAILED, user.GetLastDeviceCheckFailed());
                AddNode(eleUser, XMLDefs.CHECK_API_FAILED, user.GetDeviceChecksAPIFailed());
                AddNode(eleUser, XMLDefs.PROSCRIBED, user.GetProscribed());
                AddNode(eleUser, XMLDefs.CHECK_FAIL_CODE, user.GetDeviceChecksFailCode());
                AddNode(eleUser, XMLDefs.PROFILE_MATCH, user.GetProfileMatch());
                AddNode(eleUser, XMLDefs.BASIC_INTEGRITY, user.GetBasicIntegrity());
                AddNode(eleUser, XMLDefs.APPROVED, user.GetApproved());
                AddNode(eleUser, XMLDefs.EXPIRED, user.GetExpiredOn());
                AddNode(eleUser, XMLDefs.DEVICE_HASH, user.GetDeviceShortHash());
                AddNode(eleUser, XMLDefs.APP_LIST_HASH, user.GetAppListShortHash());
                Element eleReports = doc.createElement(XMLDefs.REPORTS);
                
                for(LaunchReport report : user.GetReports())
                {
                    Element eleReport = doc.createElement(XMLDefs.REPORT);
                    AddNode(eleReport, XMLDefs.TIME_START, report.GetStartTime());
                    AddNode(eleReport, XMLDefs.TIME_END, report.GetEndTime());
                    AddNode(eleReport, XMLDefs.MESSAGE, report.GetMessage());
                    AddNode(eleReport, XMLDefs.IS_MAJOR, report.GetMajor());
                    AddNode(eleReport, XMLDefs.LEFT_ID, report.GetLeftID());
                    AddNode(eleReport, XMLDefs.RIGHT_ID, report.GetRightID());
                    AddNode(eleReport, XMLDefs.TIMES, report.GetTimes());
                    AddNode(eleReport, XMLDefs.FLAGS, report.GetFlags());
                    eleReports.appendChild(eleReport);
                }
                
                eleUser.appendChild(eleReports);
            }
            
            //Save alliances.
            elements = AddNode(eleGame, XMLDefs.ALLIANCES);

            for(Alliance alliance : game.GetAlliances())
            {
                Element eleAlliance = AddNode(elements, XMLDefs.ALLIANCE, XMLDefs.ID, Integer.toString(alliance.GetID()), XMLDefs.NAME, alliance.GetName());
                AddNode(eleAlliance, XMLDefs.DESCRIPTION, alliance.GetDescription());
                AddNode(eleAlliance, XMLDefs.AVATAR, alliance.GetAvatarID());
            }
            
            //Save treaties.
            elements = AddNode(eleGame, XMLDefs.TREATIES);
            
            for(Treaty treaty : game.GetTreaties())
            {
                Element eleTreaty = AddNode(elements, XMLDefs.TREATY, XMLDefs.ID, Integer.toString(treaty.GetID()));
                AddNode(eleTreaty, XMLDefs.ALLIANCE1, treaty.GetAllianceID1());
                AddNode(eleTreaty, XMLDefs.ALLIANCE2, treaty.GetAllianceID2());
                Type type = treaty.GetType();
                AddNode(eleTreaty, XMLDefs.TYPE, type.ordinal());
                
                if(type == Type.WAR)
                {
                    War war = (War)treaty;
                    
                    AddNode(eleTreaty, XMLDefs.KILLS1, war.GetKills1());
                    AddNode(eleTreaty, XMLDefs.DEATHS1, war.GetDeaths1());
                    AddNode(eleTreaty, XMLDefs.OFFENCE_SPENDING1, war.GetOffenceSpending1());
                    AddNode(eleTreaty, XMLDefs.DEFENCE_SPENDING1, war.GetDefenceSpending1());
                    AddNode(eleTreaty, XMLDefs.DAMAGE_INFLICTED1, war.GetDamageInflicted1());
                    AddNode(eleTreaty, XMLDefs.DAMAGE_RECEIVED1, war.GetDamageReceived1());
                    AddNode(eleTreaty, XMLDefs.INCOME1, war.GetIncome1());
                    AddNode(eleTreaty, XMLDefs.KILLS2, war.GetKills2());
                    AddNode(eleTreaty, XMLDefs.DEATHS2, war.GetDeaths2());
                    AddNode(eleTreaty, XMLDefs.OFFENCE_SPENDING2, war.GetOffenceSpending2());
                    AddNode(eleTreaty, XMLDefs.DEFENCE_SPENDING2, war.GetDefenceSpending2());
                    AddNode(eleTreaty, XMLDefs.DAMAGE_INFLICTED2, war.GetDamageInflicted2());
                    AddNode(eleTreaty, XMLDefs.DAMAGE_RECEIVED2, war.GetDamageReceived2());
                    AddNode(eleTreaty, XMLDefs.INCOME2, war.GetIncome2());
                }
            }
            
            //Save players.
            elements = AddNode(eleGame, XMLDefs.PLAYERS);

            for(Player player : game.GetPlayers())
            {
                Element elePlayer = AddNode(elements, XMLDefs.PLAYER, XMLDefs.ID, Integer.toString(player.GetID()), XMLDefs.NAME, player.GetName());
                AddPositionNode(elePlayer, XMLDefs.POSITION, player.GetPosition());
                AddNode(elePlayer, XMLDefs.HP, player.GetHP());
                AddNode(elePlayer, XMLDefs.MAX_HP, player.GetMaxHP());
                AddNode(elePlayer, XMLDefs.AVATAR, player.GetAvatarID());
                AddNode(elePlayer, XMLDefs.WEALTH, player.GetWealth());
                AddNode(elePlayer, XMLDefs.LAST_SEEN, player.GetLastSeen());
                AddNode(elePlayer, XMLDefs.STATE_CHANGE, player.GetStateTimeRemaining());
                AddNode(elePlayer, XMLDefs.ALLIANCE_ID, player.GetAllianceIDForDataStorage());
                AddNode(elePlayer, XMLDefs.FLAGS1, player.GetFlags1());
                AddNode(elePlayer, XMLDefs.FLAGS2, player.GetFlags2());
                AddNode(elePlayer, XMLDefs.ALLIANCE_COOLOFF_TIME, player.GetAllianceCooloffRemaining());
                AddNode(elePlayer, XMLDefs.KILLS, player.GetKills());
                AddNode(elePlayer, XMLDefs.DEATHS, player.GetDeaths());
                AddNode(elePlayer, XMLDefs.OFFENCE_SPENDING, player.GetOffenceSpending());
                AddNode(elePlayer, XMLDefs.DEFENCE_SPENDING, player.GetDefenceSpending());
                AddNode(elePlayer, XMLDefs.DAMAGE_INFLICTED, player.GetDamageInflicted());
                AddNode(elePlayer, XMLDefs.DAMAGE_RECEIVED, player.GetDamageReceived());
                
                if(player.GetHasAirDefenceSystem())
                {
                    AddMissileSystem(elePlayer, player.GetInterceptorSystem(), XMLDefs.INTERCEPTOR_SYSTEM);
                }
                
                if(player.GetHasCruiseMissileSystem())
                {
                    AddMissileSystem(elePlayer, player.GetMissileSystem(), XMLDefs.MISSILE_SYSTEM);
                }
            }
            
            elements = AddNode(eleGame, XMLDefs.MISSILES);
            
            for(Missile missile : game.GetMissiles())
            {
                Element eleMissile = AddNode(elements, XMLDefs.MISSILE, XMLDefs.ID, missile.GetID());
                AddPositionNode(eleMissile, XMLDefs.POSITION, missile.GetPosition());
                AddNode(eleMissile, XMLDefs.TYPE, missile.GetType());
                AddNode(eleMissile, XMLDefs.OWNER_ID, missile.GetOwnerID());
                AddNode(eleMissile, XMLDefs.TRACKING, missile.GetTracking());
                AddPositionNode(eleMissile, XMLDefs.TARGET, missile.GetTarget());
                AddNode(eleMissile, XMLDefs.TARGET_ID, missile.GetTargetID());
            }
            
            elements = AddNode(eleGame, XMLDefs.INTERCEPTORS);
            
            for(Interceptor interceptor : game.GetInterceptors())
            {
                Element eleInterceptor = AddNode(elements, XMLDefs.INTERCEPTOR, XMLDefs.ID, interceptor.GetID());
                AddPositionNode(eleInterceptor, XMLDefs.POSITION, interceptor.GetPosition());
                AddNode(eleInterceptor, XMLDefs.OWNER_ID, interceptor.GetOwnerID());
                AddNode(eleInterceptor, XMLDefs.TARGET_ID, interceptor.GetTargetID());
                AddNode(eleInterceptor, XMLDefs.TYPE, interceptor.GetType());
                AddNode(eleInterceptor, XMLDefs.PLAYER_LAUNCHED, interceptor.GetPlayerLaunched());
            }
            
            elements = AddNode(eleGame, XMLDefs.MISSILE_SITES);
            
            for(MissileSite missileSite : game.GetMissileSites())
            {
                Element eleMissileSite = AddNode(elements, XMLDefs.MISSILE_SITE, XMLDefs.ID, missileSite.GetID(), XMLDefs.NAME, missileSite.GetName());
                AddPositionNode(eleMissileSite, XMLDefs.POSITION, missileSite.GetPosition());
                AddNode(eleMissileSite, XMLDefs.HP, missileSite.GetHP());
                AddNode(eleMissileSite, XMLDefs.MAX_HP, missileSite.GetMaxHP());
                AddNode(eleMissileSite, XMLDefs.OWNER_ID, missileSite.GetOwnerID());
                AddNode(eleMissileSite, XMLDefs.FLAGS, missileSite.GetFlags());
                AddNode(eleMissileSite, XMLDefs.STATE_TIME, missileSite.GetStateTimeRemaining());
                AddNode(eleMissileSite, XMLDefs.NUCLEAR, missileSite.CanTakeNukes());
                AddMissileSystem(eleMissileSite, missileSite.GetMissileSystem(), XMLDefs.MISSILE_SYSTEM);
                AddNode(eleMissileSite, XMLDefs.CHARGE_OWNER_TIME, missileSite.GetChargeOwnerTimeRemaining());
            }
            
            elements = AddNode(eleGame, XMLDefs.SAM_SITES);
            
            for(SAMSite samSite : game.GetSAMSites())
            {
                Element eleSAMSite = AddNode(elements, XMLDefs.SAM_SITE, XMLDefs.ID, samSite.GetID(), XMLDefs.NAME, samSite.GetName());
                AddPositionNode(eleSAMSite, XMLDefs.POSITION, samSite.GetPosition());
                AddNode(eleSAMSite, XMLDefs.HP, samSite.GetHP());
                AddNode(eleSAMSite, XMLDefs.MAX_HP, samSite.GetMaxHP());
                AddNode(eleSAMSite, XMLDefs.OWNER_ID, samSite.GetOwnerID());
                AddNode(eleSAMSite, XMLDefs.FLAGS, samSite.GetFlags());
                AddNode(eleSAMSite, XMLDefs.STATE_TIME, samSite.GetStateTimeRemaining());
                AddNode(eleSAMSite, XMLDefs.MODE, samSite.GetMode());
                AddMissileSystem(eleSAMSite, samSite.GetInterceptorSystem(), XMLDefs.INTERCEPTOR_SYSTEM);
                AddNode(eleSAMSite, XMLDefs.CHARGE_OWNER_TIME, samSite.GetChargeOwnerTimeRemaining());
            }
            
            elements = AddNode(eleGame, XMLDefs.SENTRY_GUNS);
            
            for(SentryGun sentryGun : game.GetSentryGuns())
            {
                Element eleSAMSite = AddNode(elements, XMLDefs.SENTRY_GUN, XMLDefs.ID, sentryGun.GetID(), XMLDefs.NAME, sentryGun.GetName());
                AddPositionNode(eleSAMSite, XMLDefs.POSITION, sentryGun.GetPosition());
                AddNode(eleSAMSite, XMLDefs.HP, sentryGun.GetHP());
                AddNode(eleSAMSite, XMLDefs.MAX_HP, sentryGun.GetMaxHP());
                AddNode(eleSAMSite, XMLDefs.OWNER_ID, sentryGun.GetOwnerID());
                AddNode(eleSAMSite, XMLDefs.FLAGS, sentryGun.GetFlags());
                AddNode(eleSAMSite, XMLDefs.STATE_TIME, sentryGun.GetStateTimeRemaining());
                AddNode(eleSAMSite, XMLDefs.CHARGE_OWNER_TIME, sentryGun.GetChargeOwnerTimeRemaining());
                AddNode(eleSAMSite, XMLDefs.RELOAD_REMAINING, sentryGun.GetReloadTimeRemaining());
            }
            
            elements = AddNode(eleGame, XMLDefs.ORE_MINES);
            
            for(OreMine oreMine : game.GetOreMines())
            {
                Element eleSAMSite = AddNode(elements, XMLDefs.ORE_MINE, XMLDefs.ID, oreMine.GetID(), XMLDefs.NAME, oreMine.GetName());
                AddPositionNode(eleSAMSite, XMLDefs.POSITION, oreMine.GetPosition());
                AddNode(eleSAMSite, XMLDefs.HP, oreMine.GetHP());
                AddNode(eleSAMSite, XMLDefs.MAX_HP, oreMine.GetMaxHP());
                AddNode(eleSAMSite, XMLDefs.OWNER_ID, oreMine.GetOwnerID());
                AddNode(eleSAMSite, XMLDefs.FLAGS, oreMine.GetFlags());
                AddNode(eleSAMSite, XMLDefs.STATE_TIME, oreMine.GetStateTimeRemaining());
                AddNode(eleSAMSite, XMLDefs.CHARGE_OWNER_TIME, oreMine.GetChargeOwnerTimeRemaining());
                AddNode(eleSAMSite, XMLDefs.GENERATE_TIME, oreMine.GetGenerateTimeRemaining());
            }
            
            elements = AddNode(eleGame, XMLDefs.LOOTS);
            
            for(Loot loot : game.GetLoots())
            {
                Element eleLoot = AddNode(elements, XMLDefs.LOOT, XMLDefs.ID, loot.GetID());
                AddPositionNode(eleLoot, XMLDefs.POSITION, loot.GetPosition());
                AddNode(eleLoot, XMLDefs.VALUE, loot.GetValue());
                AddNode(eleLoot, XMLDefs.EXPIRY, loot.GetExpiryRemaining());
                AddNode(eleLoot, XMLDefs.DESCRIPTION, loot.GetDescription());
            }
            
            elements = AddNode(eleGame, XMLDefs.RADIATIONS);
            
            for(Radiation radiation : game.GetRadiations())
            {
                Element eleRadiation = AddNode(elements, XMLDefs.RADIATION, XMLDefs.ID, radiation.GetID());
                AddPositionNode(eleRadiation, XMLDefs.POSITION, radiation.GetPosition());
                AddNode(eleRadiation, XMLDefs.RADIUS, radiation.GetRadius());
                AddNode(eleRadiation, XMLDefs.EXPIRY, radiation.GetExpiryTime());
            }
            
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.transform(new DOMSource(doc), new StreamResult(new File(strGameFile)));
        }
        catch(ParserConfigurationException ex)
        {
            listener.SaveError(String.format("XML parser configuration error when saving game from %s.", strGameFile));
        }
        catch(TransformerException ex)
        {
            listener.SaveError(String.format("Transformer error when saving game from %s.", strGameFile));
        }
    }
    
    private static Element AddMissileSystem(Element parent, MissileSystem missileSystem, String strTagName)
    {
        Element eleMissileSystem = doc.createElement(strTagName);
        AddNode(eleMissileSystem, XMLDefs.RELOAD_REMAINING, missileSystem.GetReloadTimeRemaining());
        AddNode(eleMissileSystem, XMLDefs.RELOAD_TIME, missileSystem.GetReloadTime());
        AddNode(eleMissileSystem, XMLDefs.SLOT_COUNT, missileSystem.GetSlotCount());
        
        Element eleSlots = AddNode(eleMissileSystem, XMLDefs.SLOTS);
        
        for(byte c = 0; c < missileSystem.GetSlotCount(); c++)
        {
            if(missileSystem.GetSlotHasMissile(c))
            {
                AddNode(eleSlots, XMLDefs.SLOT, XMLDefs.NUMBER, c, XMLDefs.TYPE, missileSystem.GetSlotMissileType(c), XMLDefs.PREP_TIME, missileSystem.GetSlotPrepTimeRemaining(c));
            }
        }
        
        parent.appendChild(eleMissileSystem);
        return eleMissileSystem;
    }
    
    private static Element AddNode(Element parent, String strTagName)
    {
        Element element = doc.createElement(strTagName);
        parent.appendChild(element);
        return element;
    }
    
    private static Element AddNode(Element parent, String strTagName, String strAtt1Name, Object objAtt1Value, String strAtt2Name, Object objAtt2Value)
    {
        Element element = doc.createElement(strTagName);
        element.setAttribute(strAtt1Name, objAtt1Value.toString());
        element.setAttribute(strAtt2Name, objAtt2Value.toString());
        parent.appendChild(element);
        return element;
    }
    
    private static Element AddNode(Element parent, String strTagName, String strAtt1Name, Object objAtt1Value, String strAtt2Name, Object objAtt2Value, String strAtt3Name, Object objAtt3Value)
    {
        Element element = doc.createElement(strTagName);
        element.setAttribute(strAtt1Name, objAtt1Value.toString());
        element.setAttribute(strAtt2Name, objAtt2Value.toString());
        element.setAttribute(strAtt3Name, objAtt3Value.toString());
        parent.appendChild(element);
        return element;
    }
    
    private static Element AddNode(Element parent, String strTagName, String strAttName, Object objValue)
    {
        Element element = doc.createElement(strTagName);
        element.setAttribute(strAttName, objValue.toString());
        parent.appendChild(element);
        return element;
    }
    
    private static Element AddNode(Element parent, String strTagName, Object objValue)
    {
        Element element = doc.createElement(strTagName);
        element.appendChild(doc.createTextNode(objValue.toString()));
        parent.appendChild(element);
        return element;
    }
    
    private static Element AddPositionNode(Element eleParent, String strTagName, GeoCoord geoPosition)
    {
        Element elePosition = AddNode(eleParent, strTagName);
        if(geoPosition == null)
        {
            AddNode(elePosition, XMLDefs.LATITUDE, Float.toString(0));
            AddNode(elePosition, XMLDefs.LONGITUDE, Float.toString(0));
        }
        else
        {
            AddNode(elePosition, XMLDefs.LATITUDE, Float.toString(geoPosition.GetLatitude()));
            AddNode(elePosition, XMLDefs.LONGITUDE, Float.toString(geoPosition.GetLongitude()));
        }
        return elePosition;
    }
}
