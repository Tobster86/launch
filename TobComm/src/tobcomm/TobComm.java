/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tobcomm;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author tobster
 */
public class TobComm
{
    private class SyncObject
    {
        private int lObjectType;
        private int lInstanceNumber;
        private int lStart;
        private byte[] cData;
        
        public SyncObject(int lObjectType, int lInstanceNumber, int lStart, byte[] cData)
        {
            this.lObjectType = lObjectType;
            this.lInstanceNumber = lInstanceNumber;
            this.lStart = lStart;
            this.cData = cData;
        }
        
        public int GetObjectType() { return lObjectType; }
        public int GetInstanceNumber() { return lInstanceNumber; }
        public int GetStart() { return lStart; }
        public byte[] GetData() { return cData; }
    }
    
    private static final String LOG_NAME = "TobComm";

    private static final byte MESSAGE_TYPE_REQUEST = 1;
    private static final byte MESSAGE_TYPE_SEND = 2;
    private static final byte MESSAGE_TYPE_COMMAND = 3;
    private static final byte MESSAGE_TYPE_REQUEST_ZIP = 4;
    private static final byte MESSAGE_TYPE_SEND_ZIP = 5;
    private static final byte MESSAGE_TYPE_COMMAND_ZIP = 6;
    
    private static final int OBJECT_HEADER_SIZE = 17; //Type (1) + ObjectNo (4) + InstanceNo (4) + Start (4) + Length (4).
    private static final int COMMAND_HEADER_SIZE = 9; //Type (1) + CommandType (4) + InstanceNo (4).
    
    private static final int SIZEOF_INT = 4;
    
    private List<SyncObject> SyncObjects;
    
    private enum State
    {
        Idle,
        ObjectNo,
        InstanceNo,
        Start,
        Length,
        Data
    }
    
    private static final int ERROR_INVALID_TYPE = 0;
    private static final int ERROR_FATAL_ERROR = 1;
    
    private static final String[] strErrors =
    {
        "Invalid type.",
        "Fatal error."
    };
    
    private TobCommInterface owner;
    private State state;
    
    //Variables for current incoming message.
    private byte cMessageType;
    private int lObjectType;
    private int lInstanceNumber;
    private int lStart;
    private int lLength;
    private ByteBuffer byteBufferData; //To receive object data.
    private ByteBuffer byteBufferInt; //To receive integer values.

    private boolean bSupressTypeErrors; //Suppresses invalid message type errors to prevent spamming the owner with error messages when our knickers get in a twist.
    
    private boolean bObjectSync = false;
    
    public TobComm(TobCommInterface owner)
    {
        this.owner = owner;
        state = State.Idle;
    }
    
    public void ProcessBytes(byte cData[])
    {
        try
        {
            for (byte cByte : cData)
            {
                switch (state)
                {
                    case Idle:
                    {
                        cMessageType = cByte;

                        //Only continue on valid message type.
                        if((cMessageType == MESSAGE_TYPE_REQUEST) ||
                                (cMessageType == MESSAGE_TYPE_SEND) ||
                                (cMessageType == MESSAGE_TYPE_COMMAND)/* ||
                                (cMessageType == MESSAGE_TYPE_REQUEST_ZIP) ||
                                (cMessageType == MESSAGE_TYPE_SEND_ZIP) ||
                                (cMessageType == MESSAGE_TYPE_COMMAND_ZIP)*/)
                        {
                            //Change state and create integer buffer.
                            state = State.ObjectNo;
                            byteBufferInt = ByteBuffer.allocate(SIZEOF_INT);
                            bSupressTypeErrors = false;
                        }
                        else
                        {
                            if(!bSupressTypeErrors)
                            {
                                owner.Error(strErrors[ERROR_INVALID_TYPE]);
                                bSupressTypeErrors = true;
                            }
                        }
                    }
                    break;

                    case ObjectNo:
                    {
                        //Store the object number.
                        byteBufferInt.put(cByte);

                        if (!byteBufferInt.hasRemaining())
                        {
                            //We have the full number. Record it and begin to store the instance number.
                            lObjectType = byteBufferInt.getInt(0);
                            byteBufferInt = ByteBuffer.allocate(SIZEOF_INT);
                            state = State.InstanceNo;
                        }
                    }
                    break;

                    case InstanceNo:
                    {
                        //Store the instance number.
                        byteBufferInt.put(cByte);

                        if (!byteBufferInt.hasRemaining())
                        {
                            //We have the instance number. Store it and decide what to do next depending on the message type.
                            lInstanceNumber = byteBufferInt.getInt(0);
                            byteBufferInt = ByteBuffer.allocate(SIZEOF_INT);

                            if (cMessageType == MESSAGE_TYPE_COMMAND)
                            {
                                //No data payload, message finished.
                                Discharge();
                            }
                            else
                            {
                                //Carry on as normal to receive the start position and size.
                                state = State.Start;
                            }
                        }
                    }
                    break;

                    case Start:
                    {
                        //Store the start position.
                        byteBufferInt.put(cByte);

                        if (!byteBufferInt.hasRemaining())
                        {
                            //We have the full number. Record it and begin to store the length.
                            lStart = byteBufferInt.getInt(0);
                            byteBufferInt = ByteBuffer.allocate(SIZEOF_INT);
                            state = State.Length;
                        }
                    }
                    break;

                    case Length:
                    {
                        //Store the length.
                        byteBufferInt.put(cByte);

                        if (!byteBufferInt.hasRemaining())
                        {
                            //We have the full length. Record it and begin to store the data, or skip to checksum if this is just a request.
                            lLength = byteBufferInt.getInt(0);
                            byteBufferInt = ByteBuffer.allocate(SIZEOF_INT);

                            if (cMessageType == MESSAGE_TYPE_REQUEST)
                            {
                                Discharge();
                            }
                            else
                            {
                                byteBufferData = ByteBuffer.allocate(lLength);
                                state = State.Data;
                            }
                        }
                    }
                    break;

                    case Data:
                    {
                        byteBufferData.put(cByte);

                        //Once we've got the chunk of data, finish.
                        if (!byteBufferData.hasRemaining())
                        {
                            Discharge();
                        }
                    }
                    break;
                }
            }
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            
            owner.Error(strErrors[ERROR_FATAL_ERROR]);
        }
    }
    
    private void Discharge()
    {
        //Everything is good. Report back to the application on a new thread.
        final int lReceivedObjectType = lObjectType;
        final int lReceivedInstanceNumber = lInstanceNumber;
        
        switch (cMessageType)
        {
            case MESSAGE_TYPE_REQUEST:
            {
                final int lReceivedStart = lStart;
                final int lReceivedLength = lLength;

                new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        owner.ObjectRequested(lReceivedObjectType, lReceivedInstanceNumber, lReceivedStart, lReceivedLength);
                    }
                }).start();
            }
            break;
            case MESSAGE_TYPE_SEND:
            {
                final byte[] cData = Arrays.copyOf(byteBufferData.array(), byteBufferData.array().length);

                if(bObjectSync)
                {
                    SyncObjects.add(new SyncObject(lObjectType, lInstanceNumber, lStart, cData));
                }
                else
                {
                    final int lReceivedStart = lStart;
                
                    new Thread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            owner.ObjectReceived(lReceivedObjectType, lReceivedInstanceNumber, lReceivedStart, cData);
                        }
                    }).start();
                }
            }
            break;
            case MESSAGE_TYPE_COMMAND:
            {
                new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        owner.CommandReceived(lReceivedObjectType, lReceivedInstanceNumber);
                    }
                }).start();
            }
            break;
        }
        
        //And we're done.
        state = State.Idle;
    }
    
    public void SendObject(int lObject, int lInstanceNumber, int lStart, byte cData[])
    {
        owner.BytesToSend(GetSendObjectData(lObject, lInstanceNumber, lStart, cData));
    }
    
    public void SendObject(int lObject, int lInstanceNumber, byte cData[])
    {
        owner.BytesToSend(GetSendObjectData(lObject, lInstanceNumber, 0, cData));
    }
    
    public void SendObject(int lObject, byte cData[])
    {
        owner.BytesToSend(GetSendObjectData(lObject, 0, 0, cData));
    }
    
    public void SendCommand(int lCommand, int lInstanceNumber)
    {
        owner.BytesToSend(GetSendCommandData(lCommand, lInstanceNumber));
    }
    
    public void SendCommand(int lCommand)
    {
        owner.BytesToSend(GetSendCommandData(lCommand, 0));
    }
    
    public void RequestObject(int lObject, int lInstanceNumber, int lStart, int lLength)
    {
        owner.BytesToSend(GetRequestObjectData(lObject, lInstanceNumber, lStart, lLength));
    }
    
    public void RequestObject(int lObject, int lInstanceNumber)
    {
        owner.BytesToSend(GetRequestObjectData(lObject, lInstanceNumber, 0, 0));
    }
    
    public void RequestObject(int lObject)
    {
        owner.BytesToSend(GetRequestObjectData(lObject, 0, 0, 0));
    }
    
    public byte[] GetSendObjectData(int lObject, int lInstanceNumber, int lStart, byte cData[])
    {
        ByteBuffer byteBuffer = ByteBuffer.allocate(OBJECT_HEADER_SIZE + cData.length);
        byteBuffer.put(MESSAGE_TYPE_SEND);
        byteBuffer.putInt(lObject);
        byteBuffer.putInt(lInstanceNumber);
        byteBuffer.putInt(lStart);
        byteBuffer.putInt(cData.length);
        byteBuffer.put(cData);
        
        return byteBuffer.array();
    }
    
    public byte[] GetRequestObjectData(int lObject, int lInstanceNumber, int lStart, int lLength)
    {
        ByteBuffer byteBuffer = ByteBuffer.allocate(OBJECT_HEADER_SIZE);
        byteBuffer.put(MESSAGE_TYPE_REQUEST);
        byteBuffer.putInt(lObject);
        byteBuffer.putInt(lInstanceNumber);
        byteBuffer.putInt(lStart);
        byteBuffer.putInt(lLength);
        
        return byteBuffer.array();
    }
    
    public byte[] GetSendCommandData(int lCommand, int lInstanceNumber)
    {
        ByteBuffer byteBuffer = ByteBuffer.allocate(COMMAND_HEADER_SIZE);
        byteBuffer.put(MESSAGE_TYPE_COMMAND);
        byteBuffer.putInt(lCommand);
        byteBuffer.putInt(lInstanceNumber);
        
        return byteBuffer.array();
    }
    
    /**
     * Begin to acculate received objects to flush on a single thread.
     */
    public void ObjectSyncEnable()
    {
        SyncObjects = new ArrayList();
        bObjectSync = true;
    }
    
    /**
     * Flush the accumulated objects on a single thread, and return to normal async.
     */
    public void ObjectSyncFlush()
    {
        bObjectSync = false;
        
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                for(SyncObject syncObject : SyncObjects)
                {
                    owner.ObjectReceived(syncObject.GetObjectType(), syncObject.GetInstanceNumber(), syncObject.GetStart(), syncObject.GetData());
                }
                
                SyncObjects = null;
                owner.SyncObjectsProcessed();
            }
        }).start();
    }
    
    /**
     * Discard any accumulated objects and return to normal async.
     */
    public void ObjectSyncCancel()
    {
        bObjectSync = false;
        SyncObjects = null;
    }
}
