/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.game.systems;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import launch.utilities.ShortDelay;

//The missile system is a generic missile container platform for both cruise missiles and interceptor missiles, managing weapon slots, reloading, and preperation times.
public class MissileSystem extends LaunchSystem
{
    public static final byte MISSILE_SLOT_EMPTY_TYPE = -1;
    private static final int MISSILE_SLOT_EMPTY_TIME = -1;
    
    private static final int DATA_SIZE = 9;
    private static final int DATA_SIZE_MISSILE_SLOT = 5;
    
    private ShortDelay dlyReload;
    private int lReloadTime;
    private byte cMissileSlotCount;
    
    private Map<Byte, Byte> MissileSlotTypes = new ConcurrentHashMap();
    private Map<Byte, ShortDelay> MissileSlotPrepTimes = new ConcurrentHashMap();
    
    /** New. */
    public MissileSystem(LaunchSystemListener listener, int lReloadTime, byte cMissileSlotCount)
    {
        super(listener);
        dlyReload = new ShortDelay();
        this.lReloadTime = lReloadTime;
        this.cMissileSlotCount = cMissileSlotCount;
    }
    
    /** From save. */
    public MissileSystem(int lReloadRemaining, int lReloadTime, byte cMissileSlotCount, Map<Byte, Byte> SlotTypes, Map<Byte, ShortDelay> PrepTimes)
    {
        super();
        dlyReload = new ShortDelay(lReloadRemaining);
        this.lReloadTime = lReloadTime;
        this.cMissileSlotCount = cMissileSlotCount;
        MissileSlotTypes = SlotTypes;
        MissileSlotPrepTimes = PrepTimes;
    }
    
    /** From comms. */
    public MissileSystem(LaunchSystemListener listener, ByteBuffer bb)
    {
        super(listener);
        dlyReload = new ShortDelay(bb);
        lReloadTime = bb.getInt();
        cMissileSlotCount = bb.get();
        
        for(byte i = 0; i < cMissileSlotCount; i++)
        {
            byte cType = bb.get();
            
            if(cType == MISSILE_SLOT_EMPTY_TYPE)
            {
                //Empty slot. Just prune the empty time value off the byte buffer.
                bb.getInt();
            }
            else
            {
                //Assign to missile slot.
                MissileSlotTypes.put(i, cType);
                MissileSlotPrepTimes.put(i, new ShortDelay(bb));
            }
        }
    }
    
    /** Dummy from comms (other players). */
    public MissileSystem()
    {
        dlyReload = new ShortDelay();
    }
    
    @Override
    public void Tick(int lMS)
    {
        dlyReload.Tick(lMS);
        
        for(ShortDelay dlyPrepTime : MissileSlotPrepTimes.values())
        {
            dlyPrepTime.Tick(lMS);
        }
    }

    @Override
    public byte[] GetData()
    {
        ByteBuffer bb = ByteBuffer.allocate(DATA_SIZE + ((int)cMissileSlotCount * DATA_SIZE_MISSILE_SLOT));
        
        dlyReload.GetData(bb);
        bb.putInt(lReloadTime);
        bb.put(cMissileSlotCount);
        
        for(byte i = 0; i < cMissileSlotCount; i++)
        {
            if(MissileSlotTypes.containsKey(i))
            {
                bb.put(MissileSlotTypes.get(i));
                MissileSlotPrepTimes.get(i).GetData(bb);
            }
            else
            {
                bb.put(MISSILE_SLOT_EMPTY_TYPE);
                bb.putInt(MISSILE_SLOT_EMPTY_TIME);
            }
        }
        
        return bb.array();
    }
    
    public void AddMissileToSlot(byte cSlotNo, byte cType, int lPrepTimeRemaining)
    {
        MissileSlotTypes.put(cSlotNo, cType);
        MissileSlotPrepTimes.put(cSlotNo, new ShortDelay(lPrepTimeRemaining));
        Changed();
    }
    
    public void AddMissileToNextSlot(byte cSlotNo, byte cType, int lPrepTimeRemaining)
    {
        if(GetEmptySlotCount() > 0)
        {
            while(GetSlotHasMissile(cSlotNo))
            {
                cSlotNo++;
                if(cSlotNo >= cMissileSlotCount)
                    cSlotNo = 0;
            }
            
            MissileSlotTypes.put(cSlotNo, cType);
            MissileSlotPrepTimes.put(cSlotNo, new ShortDelay(lPrepTimeRemaining));
        }
    }
    
    public void CompleteMultiPurchase()
    {
        Changed();
    }

    public byte GetSlotCount() { return cMissileSlotCount; }
    
    public boolean GetSlotHasMissile(byte cSlotNumber)
    {
        return MissileSlotTypes.containsKey(cSlotNumber);
    }
    
    public byte GetSlotMissileType(byte cSlotNumber)
    {
        return MissileSlotTypes.get(cSlotNumber);
    }
    
    public int GetSlotPrepTimeRemaining(byte cSlotNumber)
    {
        return MissileSlotPrepTimes.get(cSlotNumber).GetRemaining();
    }
    
    public boolean ReadyToFire() { return dlyReload.Expired(); }
    
    public boolean GetSlotReadyToFire(byte cSlotNumber)
    {
        if(dlyReload.Expired())
        {
            if(MissileSlotTypes.containsKey(cSlotNumber))
            {
                return MissileSlotPrepTimes.get(cSlotNumber).Expired();
            }
        }
        
        return false;
    }
    
    public int GetReloadTimeRemaining() { return dlyReload.GetRemaining(); }
    
    public int GetReloadTime() { return lReloadTime; }
    
    public void SetReloadTime(int lReloadTime)
    {
        this.lReloadTime = lReloadTime;
        
        if(dlyReload.GetRemaining() > lReloadTime)
        {
            dlyReload.Set(lReloadTime);
        }
        
        Changed();
    }
    
    public void Fire(byte cSlot)
    {
        MissileSlotTypes.remove(cSlot);
        MissileSlotPrepTimes.remove(cSlot);
        dlyReload.Set(lReloadTime);
        Changed();
    }
    
    public void UnloadSlot(byte cSlot)
    {
        MissileSlotTypes.remove(cSlot);
        MissileSlotPrepTimes.remove(cSlot);
        Changed();
    }
    
    public void IncreaseSlotCount(byte cHowMany)
    {
        cMissileSlotCount += cHowMany;
        Changed();
    }

    public int GetEmptySlotCount()
    {
        return cMissileSlotCount - MissileSlotTypes.size();
    }

    public int GetOccupiedSlotCount()
    {
        return MissileSlotTypes.size();
    }
    
    public Map<Byte, Integer> GetTypeCounts()
    {
        Map<Byte, Integer> Result = new HashMap<>();
        
        for(Entry<Byte, Byte> MissileSlotType : MissileSlotTypes.entrySet())
        {
            Byte cKey = MissileSlotType.getValue();
            
            if(cKey != MISSILE_SLOT_EMPTY_TYPE)
            {
                if(Result.containsKey(cKey))
                {
                    int lNewValue = Result.get(cKey) + 1;
                    Result.put(cKey, lNewValue);
                }
                else
                {
                    Result.put(cKey, 1);
                }
            }
        }
        
        return Result;
    }
}
