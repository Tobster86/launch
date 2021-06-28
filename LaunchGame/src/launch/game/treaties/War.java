/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.game.treaties;

import java.nio.ByteBuffer;

/**
 *
 * @author tobster
 */
public class War extends Treaty
{
    private static final int DATA_SIZE = 60;
    
    private short nKills1;
    private short nDeaths1;
    private int lOffenceSpending1;
    private int lDefenceSpending1;
    private int lDamageInflicted1;
    private int lDamageReceived1;
    private int lIncome1;
    
    private short nKills2;
    private short nDeaths2;
    private int lOffenceSpending2;
    private int lDefenceSpending2;
    private int lDamageInflicted2;
    private int lDamageReceived2;
    private int lIncome2;
    
    //Only stored temporarily while wars are being concluded when an alliance disbands.
    private boolean bForfeited1 = false;
    private boolean bForfeited2 = false;

    /**
     * Create a new war.
     * @param lID ID of the war.
     * @param lAllianceID1 ID of one beligerant.
     * @param lAllianceID2 ID of t'other.
     */
    public War(int lID, int lAllianceID1, int lAllianceID2)
    {
        super(lID, lAllianceID1, lAllianceID2);
        this.nKills1 = 0;
        this.nDeaths1 = 0;
        this.lOffenceSpending1 = 0;
        this.lDefenceSpending1 = 0;
        this.lDamageInflicted1 = 0;
        this.lDamageReceived1 = 0;
        this.lIncome1 = 0;

        this.nKills2 = 0;
        this.nDeaths2 = 0;
        this.lOffenceSpending2 = 0;
        this.lDefenceSpending2 = 0;
        this.lDamageInflicted2 = 0;
        this.lDamageReceived2 = 0;
        this.lIncome2 = 0;
    }
    
    /**
     * Create a war from a saved game state.
     * @param lID
     * @param lAllianceID1
     * @param lAllianceID2
     * @param nKills1
     * @param nDeaths1
     * @param lOffenceSpending1
     * @param lDefenceSpending1
     * @param lDamageInflicted1
     * @param lDamageReceived1
     * @param lIncome1
     * @param nKills2
     * @param nDeaths2
     * @param lOffenceSpending2
     * @param lDefenceSpending2
     * @param lDamageInflicted2
     * @param lDamageReceived2
     * @param lIncome2 
     */
    public War(int lID, int lAllianceID1, int lAllianceID2, short nKills1, short nDeaths1, int lOffenceSpending1, int lDefenceSpending1, int lDamageInflicted1, int lDamageReceived1, int lIncome1, short nKills2, short nDeaths2, int lOffenceSpending2, int lDefenceSpending2, int lDamageInflicted2, int lDamageReceived2, int lIncome2)
    {
        super(lID, lAllianceID1, lAllianceID2);
        this.nKills1 = nKills1;
        this.nDeaths1 = nDeaths1;
        this.lOffenceSpending1 = lOffenceSpending1;
        this.lDefenceSpending1 = lDefenceSpending1;
        this.lDamageInflicted1 = lDamageInflicted1;
        this.lDamageReceived1 = lDamageReceived1;
        this.lIncome1 = lIncome1;

        this.nKills2 = nKills2;
        this.nDeaths2 = nDeaths2;
        this.lOffenceSpending2 = lOffenceSpending2;
        this.lDefenceSpending2 = lDefenceSpending2;
        this.lDamageInflicted2 = lDamageInflicted2;
        this.lDamageReceived2 = lDamageReceived2;
        this.lIncome2 = lIncome2;
    }
    
    public War(ByteBuffer bb)
    {
        super(bb);
        nKills1 = bb.getShort();
        nDeaths1 = bb.getShort();
        lOffenceSpending1 = bb.getInt();
        lDefenceSpending1 = bb.getInt();
        lDamageInflicted1 = bb.getInt();
        lDamageReceived1 = bb.getInt();
        lIncome1 = bb.getInt();

        nKills2 = bb.getShort();
        nDeaths2 = bb.getShort();
        lOffenceSpending2 = bb.getInt();
        lDefenceSpending2 = bb.getInt();
        lDamageInflicted2 = bb.getInt();
        lDamageReceived2 = bb.getInt();
        lIncome2 = bb.getInt();
    }
    
    @Override
    public Type GetType()
    {
        return Type.WAR;
    }

    @Override
    public byte[] GetData()
    {
        byte[] cBaseData = super.GetData();
        
        ByteBuffer bb = ByteBuffer.allocate(cBaseData.length + DATA_SIZE);
        bb.put(cBaseData);
        bb.putShort(nKills1);
        bb.putShort(nDeaths1);
        bb.putInt(lOffenceSpending1);
        bb.putInt(lDefenceSpending1);
        bb.putInt(lDamageInflicted1);
        bb.putInt(lDamageReceived1);
        bb.putInt(lIncome1);

        bb.putShort(nKills2);
        bb.putShort(nDeaths2);
        bb.putInt(lOffenceSpending2);
        bb.putInt(lDefenceSpending2);
        bb.putInt(lDamageInflicted2);
        bb.putInt(lDamageReceived2);
        bb.putInt(lIncome2);
        
        return bb.array();
    }
    
    public void AddKill(int lAllianceID)
    {
        if(lAllianceID == lAllianceID1)
            nKills1++;
        
        if(lAllianceID == lAllianceID2)
            nKills2++;
    }
    
    public void SubtractKills(int lAllianceID, short nCount)
    {
        if(lAllianceID == lAllianceID1)
            nKills1 = (short)Math.max(0, nKills1 - nCount);
        
        if(lAllianceID == lAllianceID2)
            nKills2 = (short)Math.max(0, nKills2 - nCount);
    }
    
    public void AddDeath(int lAllianceID)
    {
        if(lAllianceID == lAllianceID1)
            nDeaths1++;
        
        if(lAllianceID == lAllianceID2)
            nDeaths2++;
    }
    
    public void AddOffenceSpending(int lAllianceID, int lAmount)
    {
        if(lAllianceID == lAllianceID1)
            lOffenceSpending1 += lAmount;
        
        if(lAllianceID == lAllianceID2)
            lOffenceSpending2 += lAmount;
    }
    
    public void AddDefenceSpending(int lAllianceID, int lAmount)
    {
        if(lAllianceID == lAllianceID1)
            lDefenceSpending1 += lAmount;
        
        if(lAllianceID == lAllianceID2)
            lDefenceSpending2 += lAmount;
    }
    
    public void AddDamageInflicted(int lAllianceIDInflictor, short nHP)
    {
        if(lAllianceIDInflictor == lAllianceID1)
        {
            lDamageInflicted1 += nHP;
            lDamageReceived2 += nHP;
        }
        
        if(lAllianceIDInflictor == lAllianceID2)
        {
            lDamageInflicted2 += nHP;
            lDamageReceived1 += nHP;
        }
    }
    
    public void SubtractDamageInflicted(int lAllianceID, int lHP)
    {
        if(lAllianceID == lAllianceID1)
        {
            lDamageInflicted1 = Math.max(0, lDamageInflicted1 - lHP);
        }
        
        if(lAllianceID == lAllianceID2)
        {
            lDamageInflicted2 = Math.max(0, lDamageInflicted2 - lHP);
        }
    }
    
    public void AddIncome(int lAllianceID, int lAmount)
    {
        if(lAllianceID == lAllianceID1)
            lIncome1 += lAmount;
        
        if(lAllianceID == lAllianceID2)
            lIncome2 += lAmount;
    }

    public short GetKills1()
    {
        return nKills1;
    }

    public short GetDeaths1()
    {
        return nDeaths1;
    }

    public int GetOffenceSpending1()
    {
        return lOffenceSpending1;
    }

    public int GetDefenceSpending1()
    {
        return lDefenceSpending1;
    }

    public int GetDamageInflicted1()
    {
        return lDamageInflicted1;
    }

    public int GetDamageReceived1()
    {
        return lDamageReceived1;
    }

    public int GetIncome1()
    {
        return lIncome1;
    }

    public short GetKills2()
    {
        return nKills2;
    }

    public short GetDeaths2()
    {
        return nDeaths2;
    }

    public int GetOffenceSpending2()
    {
        return lOffenceSpending2;
    }

    public int GetDefenceSpending2()
    {
        return lDefenceSpending2;
    }

    public int GetDamageInflicted2()
    {
        return lDamageInflicted2;
    }

    public int GetDamageReceived2()
    {
        return lDamageReceived2;
    }

    public int GetIncome2()
    {
        return lIncome2;
    }
    
    private float CalculateEfficiency(int lCost, int lHP)
    {
        if(lHP > 0)
        {
            return (float)lCost / (float)lHP;
        }
        
        return Float.MAX_VALUE;
    }
    
    public float GetOffenceEfficiency1()
    {
        return CalculateEfficiency(lOffenceSpending1, lDamageInflicted1);
    }
    
    public float GetOffenceEfficiency2()
    {
        return CalculateEfficiency(lOffenceSpending2, lDamageInflicted2);
    }
    
    public float GetDefenceEfficiency1()
    {
        return CalculateEfficiency(lDefenceSpending1, lDamageReceived1);
    }
    
    public float GetDefenceEfficiency2()
    {
        return CalculateEfficiency(lDefenceSpending2, lDamageReceived2);
    }
    
    public int GetWonFactors1()
    {
        if(bForfeited1)
            return 0;
        else if(bForfeited2)
            return Integer.MAX_VALUE;
        
        int lResult = 0;
        
        if(nKills1 > nKills2)
            lResult++;
        
        if(nDeaths1 < nDeaths2)
            lResult++;
        
        if(lIncome1 < lIncome2)
            lResult++;
        
        if(GetOffenceEfficiency1() < GetOffenceEfficiency2())
            lResult++;
        
        if(GetDefenceEfficiency1() < GetDefenceEfficiency2())
            lResult++;
        
        return lResult;
    }
    
    public int GetWonFactors2()
    {
        if(bForfeited2)
            return 0;
        else if(bForfeited1)
            return Integer.MAX_VALUE;
        
        int lResult = 0;
        
        if(nKills2 > nKills1)
            lResult++;
        
        if(nDeaths2 < nDeaths1)
            lResult++;
        
        if(lIncome2 < lIncome1)
            lResult++;
        
        if(GetOffenceEfficiency2() < GetOffenceEfficiency1())
            lResult++;
        
        if(GetDefenceEfficiency2() < GetDefenceEfficiency1())
            lResult++;
        
        return lResult;
    }
    
    public int GetTotalSpending()
    {
        return lOffenceSpending1 + lOffenceSpending2 + lDefenceSpending1 + lDefenceSpending2;
    }
    
    /**
     * Cause the specified alliance to forfeit the war.
     * @param lAllianceID The alliance that shall forfeit the war.
     */
    public void Forfeit(int lAllianceID)
    {
        if(lAllianceID == lAllianceID1)
        {
            bForfeited1 = true;
        }
        
        if(lAllianceID == lAllianceID2)
        {
            bForfeited2 = true;
        }
    }
}
