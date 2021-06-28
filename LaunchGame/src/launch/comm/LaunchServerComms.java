/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.comm;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import static launch.comm.LaunchComms.LOG_NAME;
import tobcomm.protocol.TCPProvider;
import launch.game.Alliance;
import launch.game.LaunchServerGameInterface;
import launch.game.User;
import launch.game.treaties.Treaty;
import launch.game.entities.LaunchEntity;
import launch.game.entities.Player;
import launch.utilities.LaunchBannedApp;
import launch.utilities.LaunchEvent;
import launch.utilities.LaunchLog;
import static launch.utilities.LaunchLog.LogType.COMMS;

/**
 *
 * @author tobster
 */
public class LaunchServerComms extends LaunchComms
{
    private static final int SERVER_SOCKET_TIMEOUT = 1000;
    
    private boolean bRunning = true;
    private boolean bBufferingUpdates = false;
    private boolean bShutDown = false;
    
    private AtomicInteger lSessionID = new AtomicInteger();
    final private Map<Integer, LaunchServerSession> Sessions = new ConcurrentHashMap();
    private int lTotalSessionsOpened = 0;
    private int lTotalSessionsClosed = 0;
    private int lMostActiveSessions = 0;
    
    private final LaunchServerGameInterface gameInterface;
    private final int lPort;
    
    private final List<LaunchEntity> DispatchList = new ArrayList<>();
    
    //Connection statistics.
    int lTotalDownloadRate = 0;
    int lTotalUploadRate = 0;
    
    public LaunchServerComms(final LaunchServerGameInterface gameInterface, int lPort)
    {
        this.gameInterface = gameInterface;
        this.lPort = lPort;
    }
    
    public void Begin()
    {
        new Thread()
        {
            @Override
            public void run()
            {
                ServerSocket serverSocket;
                
                try
                {
                    serverSocket = new ServerSocket(lPort);
                    serverSocket.setSoTimeout(SERVER_SOCKET_TIMEOUT);
                    
                    LaunchLog.Log(COMMS, LOG_NAME, "Server socket established.");

                    while(bRunning)
                    {
                        try
                        {
                            Socket socket = serverSocket.accept();

                            LaunchLog.Log(COMMS, LOG_NAME, String.format("Client connected, hash %08X. Creating session.", socket.hashCode()));
                            lTotalSessionsOpened++;
                            lMostActiveSessions = Math.max(lMostActiveSessions, Sessions.size());
                            LaunchServerSession session = new LaunchServerSession(lSessionID.getAndIncrement(), socket, gameInterface);
                            
                            Sessions.put(session.GetID(), session);
                        }
                        catch(IOException ex)
                        {
                            //Expected socket timeout. Don't care.
                        }
                        catch (Exception ex)
                        {
                            LaunchLog.Log(COMMS, LOG_NAME, "Socket error: " + ex.getMessage());
                        }
                    }
                    
                    serverSocket.close();
                }
                catch (Exception ex)
                {
                    LaunchLog.Log(COMMS, LOG_NAME, "Couldn't create socket: " + ex.getMessage());
                    bRunning = false;
                }
                
                LaunchLog.Log(COMMS, LOG_NAME, "Server comms finished.");
                bShutDown = true;
            }
        }.start();
    }

    @Override
    public void InterruptAll()
    {
        for(LaunchServerSession session : Sessions.values())
        {
            session.Close();
        }
    }
    
    public void ShutDown()
    {
        //Terminate all sessions and shut down.
        bRunning = false;
        
        LaunchLog.Log(COMMS, LOG_NAME, "Shut down instruction received...");

        for(LaunchServerSession session : Sessions.values())
        {
            LaunchLog.Log(COMMS, LOG_NAME, "Closing session...");
            session.Close();
        }
        
        LaunchLog.Log(COMMS, LOG_NAME, "...All sessions are closed.");
    }
    
    public boolean GetShutDown()
    {
        return bShutDown;
    }
    
    //Called at the start of Tick(), so that entities with multiple changes in the tick only end up being dispatched once.
    public void BufferUpdates()
    {
        //Clear the dispatch list and start buffering changed entities.
        DispatchList.clear();
        bBufferingUpdates = true;
    }
    
    //Called at the end of Tick(), when all unique entity updates should be dispatched to connected clients.
    public void DispatchUpdates()
    {
        //Dispatch the updates.
        bBufferingUpdates = false;
        
        for(LaunchEntity entity : DispatchList)
        {
            for(LaunchServerSession session : Sessions.values())
            {
                if(session.CanReceiveUpdates())
                {
                    session.SendEntity(entity);
                }
            }
        }
    }
    
    /**
     * An entity updated on the server.
     * @param entity The entity that updated.
     * @param bOwner Whether only the owner should know that it updated (for optimising player-only relevant stuff out of everyone else's comms).
     */
    public void EntityUpdated(LaunchEntity entity, boolean bOwner)
    {
        //Don't dispatch when buffering updates is true, instead add them to the list to dispatch. Unless it's only for the player that owns the entity.
        if(bBufferingUpdates && !bOwner)
        {
            if(!DispatchList.contains(entity))
            {
                DispatchList.add(entity);
            }
        }
        else
        {
            //Otherwise send to all sessions.
            for(LaunchServerSession session : Sessions.values())
            {
                if(session.CanReceiveUpdates())
                {
                    if(!bOwner || entity.GetOwnedBy(session.GetAuthenticatedUser().GetPlayerID()))
                    {
                        session.SendEntity(entity);
                    }
                }
            }
        }
    }
    
    public void AllianceUpdated(Alliance alliance, boolean bMajor)
    {
        //Send to all sessions.
        for(LaunchServerSession session : Sessions.values())
        {
            if(session.CanReceiveUpdates())
            {
                session.SendAlliance(alliance, bMajor);
            }
        }
    }
    
    public void AllianceRemoved(Alliance alliance)
    {
        //Send to all sessions.
        for(LaunchServerSession session : Sessions.values())
        {
            if(session.CanReceiveUpdates())
            {
                session.RemoveAlliance(alliance);
            }
        }
    }
    
    public void EntityRemoved(LaunchEntity entity)
    {
        for(LaunchServerSession session : Sessions.values())
        {
            if(session.CanReceiveUpdates())
            {
                session.RemoveEntity(entity);
            }
        }
    }
    
    public void TreatyCreated(Treaty war)
    {
        for(LaunchServerSession session : Sessions.values())
        {
            if(session.CanReceiveUpdates())
            {
                session.SendTreaty(war);
            }
        }
    }
    
    public void TreatyRemoved(Treaty war)
    {
        for(LaunchServerSession session : Sessions.values())
        {
            if(session.CanReceiveUpdates())
            {
                session.RemoveTreaty(war);
            }
        }
    }
    
    public void Announce(LaunchEvent event)
    {
        for(LaunchServerSession session : Sessions.values())
        {
            if(session.CanReceiveUpdates())
            {
                session.SendEvent(event);
            }
        }
    }

    @Override
    public void Tick(int lMS)
    {
        boolean bDeadSessions = false;

        lTotalDownloadRate = 0;
        lTotalUploadRate = 0;

        for(LaunchServerSession session : Sessions.values())
        {
            if(session.IsAlive())
            {
                session.Tick(lMS);
                lTotalDownloadRate += session.GetDownloadRate();
                lTotalUploadRate += session.GetUploadRate();
            }
            else
            {
                Sessions.remove(session.GetID());
            }
        }
    }
    
    public void StopCommsTo(User user)
    {
        for(LaunchServerSession session : Sessions.values())
        {
            if(session.GetAuthenticatedUser() == user)
            {
                session.Close();
            }
        }
    }
    
    public int GetActiveSessions()
    {
        return Sessions.size();
    }
    
    public int GetTotalSessionsOpened()
    {
        return lTotalSessionsOpened;
    }
    
    public int GetTotalSessionsClosed()
    {
        return lTotalSessionsClosed;
    }
    
    public int GetMostActiveSessions()
    {
        return lMostActiveSessions;
    }
}
