/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tobcomm.protocol;

/**
 *
 * @author tobster
 */
public abstract class ConnectionProvider
{
    public interface ConnectionLogger
    {
        public void ConnectionLog(String strLog);
    }
    
    protected ConnectionLogger logger;
    
    protected ConnectionProvider(ConnectionLogger logger)
    {
        this.logger = logger;
    }
    
    /** Initialises the connection.
     * @return Initialisation was successful. */
    public abstract boolean Initialise();
    
    /** Indicates that data is available to read.
     * @return Data is available to read. */
    public abstract boolean DataAvailable();
    
    /** Reads data into the provided buffer.
     * @param cData Buffer to read data into.
     * @return The number of bytes written into the provided buffer. */
    public abstract int Read(byte cData[]);
    
    /** Writes data to the connection.
     * @param cData Buffer containing data to write. */
    public abstract void Write(byte cData[]);
    
    /** Closes the connection. */
    public abstract void Close();
    
    /** Indicates that the connection is dead, and will no longer work.
     * @return The connection is dead. */
    public abstract boolean Died();
    
    /** Return a string that represents the destination address, e.g. an IP address.
     * @return A string representing the address. */
    public abstract String GetAddress();
}
