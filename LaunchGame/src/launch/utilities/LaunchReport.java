/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.utilities;

import java.nio.ByteBuffer;

/**
 *
 * @author tobster
 */
public class LaunchReport
{
    private final static int DATA_SIZE = 27;
    
    private static final int FLAG_LEFT_IS_ALLIANCE = 0x01;      //LeftID refers to an alliance, not a player.
    private static final int FLAG_RIGHT_IS_ALLIANCE = 0x02;     //RightID refers to an alliance, not a player.
    private static final int FLAG_RES3 = 0x04;
    private static final int FLAG_RES4 = 0x08;
    private static final int FLAG_RES5 = 0x10;
    private static final int FLAG_RES6 = 0x20;
    private static final int FLAG_RES7 = 0x40;
    private static final int FLAG_RES8 = 0x80;
    
    public final static int ID_NOBODY = -1;
    
    private long oTimeStart = System.currentTimeMillis();
    private long oTimeEnd = System.currentTimeMillis();
    private String strMessage;
    private boolean bIsMajor;           //Should flash the UI red, otherwise yellow.
    private int lLeftID;                //The ID of the player that "did this", or the alliance it's most relevant to.
    private int lRightID;               //The ID of the player that this was "done to", or the other alliance that was involved.
    private byte cTimes;                //Number of times it happened.
    private byte cFlags;                //Flags.
    
    //From save.
    public LaunchReport(long oTimeStart, long oTimeEnd, String strMessage, boolean bIsMajor, int lLeftID, int lRightID, byte cTimes, byte cFlags)
    {
        this.oTimeStart = oTimeStart;
        this.oTimeEnd = oTimeEnd;
        this.strMessage = strMessage;
        this.bIsMajor = bIsMajor;
        this.lLeftID = lLeftID;
        this.lRightID = lRightID;
        this.cTimes = cTimes;
        this.cFlags = cFlags;
    }
    
    public LaunchReport(String strMessage, boolean bIsMajor)
    {
        //A really basic report.
        this.strMessage = strMessage;
        this.bIsMajor = bIsMajor;
        this.lLeftID = ID_NOBODY;
        this.lRightID = ID_NOBODY;
        cTimes = 1;
        cFlags = 0;
    }
    
    public LaunchReport(String strMessage, boolean bIsMajor, int lLeftID)
    {
        //A report where a player did something.
        this.strMessage = strMessage;
        this.bIsMajor = bIsMajor;
        this.lLeftID = lLeftID;
        this.lRightID = ID_NOBODY;
        cTimes = 1;
        cFlags = 0;
    }
    
    public LaunchReport(String strMessage, boolean bIsMajor, int lLeftID, int lRightID)
    {
        //A report where a player did something to another player.
        this.strMessage = strMessage;
        this.bIsMajor = bIsMajor;
        this.lLeftID = lLeftID;
        this.lRightID = lRightID;
        cTimes = 0;
        cFlags = 0;
    }
    
    public LaunchReport(String strMessage, boolean bIsMajor, int lLeftID, int lRightID, boolean bLeftIDAlliance, boolean bRightIDAlliance)
    {
        //A report where a player did something to another player.
        this.strMessage = strMessage;
        this.bIsMajor = bIsMajor;
        this.lLeftID = lLeftID;
        this.lRightID = lRightID;
        cTimes = 0;
        cFlags = 0;
        
        if(bLeftIDAlliance)
            SetLeftIDAlliance(bLeftIDAlliance);
        
        if(bRightIDAlliance)
            SetRightIDAlliance(bRightIDAlliance);
    }
    
    public LaunchReport(ByteBuffer bb)
    {
        oTimeStart = bb.getLong();
        oTimeEnd = bb.getLong();
        strMessage = LaunchUtilities.StringFromData(bb);
        bIsMajor = (bb.get() != 0x00);
        lLeftID = bb.getInt();
        lRightID = bb.getInt();
        cTimes = bb.get();
        cFlags = bb.get();
    }
    
    public byte[] GetData()
    {
        ByteBuffer bb = ByteBuffer.allocate(DATA_SIZE + LaunchUtilities.GetStringDataSize(strMessage));
        bb.putLong(oTimeStart);
        bb.putLong(oTimeEnd);
        bb.put(LaunchUtilities.GetStringData(strMessage));
        bb.put((byte)(bIsMajor ? 0xFF : 0x00));
        bb.putInt(lLeftID);
        bb.putInt(lRightID);
        bb.put(cTimes);
        bb.put(cFlags);
        return bb.array();
    }
    
    public void Update(LaunchReport report)
    {
        //A report with the same hash was received by the client. Merge the timestamp data with this one.
        oTimeStart = Math.min(oTimeStart, report.GetStartTime());
        oTimeEnd = Math.max(oTimeEnd, report.GetEndTime());
        cTimes++;
    }
    
    public void HappenedAgain()
    {
        //The same event happened again. Update the end time and increment the counts.
        oTimeEnd = System.currentTimeMillis();
        cTimes++;
    }
    
    private void SetLeftIDAlliance(boolean bAlliance)
    {
        if(bAlliance)
            cFlags |= FLAG_LEFT_IS_ALLIANCE;
        else
            cFlags &= ~FLAG_LEFT_IS_ALLIANCE;
    }
    
    private void SetRightIDAlliance(boolean bAlliance)
    {
        if(bAlliance)
            cFlags |= FLAG_RIGHT_IS_ALLIANCE;
        else
            cFlags &= ~FLAG_RIGHT_IS_ALLIANCE;
    }
    
    public boolean GetLeftIDAlliance()
    {
        return ( cFlags & FLAG_LEFT_IS_ALLIANCE ) != 0x00;
    }
    
    public boolean GetRightIDAlliance()
    {
        return ( cFlags & FLAG_RIGHT_IS_ALLIANCE ) != 0x00;
    }
    
    public long GetStartTime() { return oTimeStart; }
    public long GetEndTime() { return oTimeEnd; }
    public boolean HasTimeRange() { return oTimeStart != oTimeEnd; }
    public String GetMessage() { return strMessage; }
    public boolean GetMajor() { return bIsMajor; }
    public int GetLeftID() { return lLeftID; }
    public int GetRightID() { return lRightID; }
    public byte GetTimes() { return cTimes; }
    public byte GetFlags() { return cFlags; }
}
