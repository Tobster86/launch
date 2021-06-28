/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tobcomm.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

/**
 *
 * @author tobster
 */
public class TCPProvider extends ConnectionProvider
{
    private Socket socket;
    private InputStream in;
    private OutputStream out;
    
    private boolean bDead = false;
    
    /** Client -> Server.
     * @param strAddress The destination URL.
     * @param lPort The port number.
     * @param logger An interface to log messages. */
    public TCPProvider(String strAddress, int lPort, ConnectionLogger logger)
    {
        super(logger);
        
        try
        {
            socket = new Socket(InetAddress.getByName(strAddress), lPort);
        }
        catch (IOException ex)
        {
            bDead = true;
        }
    }
    
    /** Server -> Client.
     * @param socket The existing socket that was accepted from a ServerSocket.
     * @param logger An interface to log messages. */
    public TCPProvider(Socket socket, ConnectionLogger logger)
    {
        super(logger);
        
        this.socket = socket;
    }
    
    @Override
    public boolean Initialise()
    {
        try
        {
            in = socket.getInputStream();
            out = socket.getOutputStream();
            logger.ConnectionLog("Session got TCP streams.");
            return true;
        }
        catch (IOException ex)
        {
            logger.ConnectionLog("Session couldn't get TCP streams: " + ex.getMessage());
        }
        
        return false;
    }

    @Override
    public boolean DataAvailable()
    {
        try
        {
            return in.available() > 0;
        }
        catch (IOException ex)
        {
            bDead = true;
            logger.ConnectionLog("TCP input stream availability error: " + ex.getMessage());
        }
        
        return false;
    }

    @Override
    public int Read(byte cData[])
    {
        try
        {
            return in.read(cData);
        }
        catch(IOException ex)
        {
            bDead = true;
            logger.ConnectionLog("TCP input stream error: " + ex.getMessage());
        }
        
        return 0;
    }

    @Override
    public void Write(byte[] cData)
    {
        try
        {
            out.write(cData);
        }
        catch (IOException ex)
        {
            bDead = true;
            logger.ConnectionLog(String.format("Output TCP stream error sending %d bytes: ", cData.length, ex.getMessage()));
        }
    }

    @Override
    public void Close()
    {
        bDead = true;
        
        try
        {
            socket.close();
        }
        catch (IOException ex)
        {
            //Don't care.
        }
    }

    @Override
    public boolean Died()
    {
        return bDead;
    }

    @Override
    public String GetAddress()
    {
        return socket.getInetAddress().toString().replace("/", "");
    }
}
