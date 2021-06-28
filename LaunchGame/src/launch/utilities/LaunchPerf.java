/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.utilities;

/**
 *
 * @author tobster
 */
public class LaunchPerf
{
    public enum Metric
    {
        PlayerTick,
        MissileTick,
        InterceptorTick,
        MissileSiteTick,
        SAMSiteTick,
        SentryGunTick,
        OtherTickAndCleanup,
        MoneyHealthAndMajorEvents,
        PlayerDefences,
        PlayerStates,
        DispatchAndBackup
    }
    
    private static final int NumberOfMetrics = Metric.values().length;
    
    private static boolean bSampling = false;
    private static long oSampleStart = 0;
    private static long[] CurrentSamples = new long[NumberOfMetrics];
    private static long[] LatestSamples = new long[NumberOfMetrics];
    
    private static boolean bPrintOnConsolidate = false;
    
    public static final void BeginSample()
    {
        oSampleStart = System.currentTimeMillis();
        bSampling = true;
    }
    
    public static final void Consolidate()
    {
        System.arraycopy(CurrentSamples, 0, LatestSamples, 0, NumberOfMetrics);
        
        if(bPrintOnConsolidate)
        {
            PrintLatestSamples();
        }
        
        bSampling = false;
    }
    
    public static final void Measure(Metric metric)
    {
        if(bSampling)
        {
            CurrentSamples[metric.ordinal()] = System.currentTimeMillis() - oSampleStart;
            oSampleStart = System.currentTimeMillis();
        }
    }
    
    public static final void PrintLatestSamples()
    {
        LaunchLog.Log(LaunchLog.LogType.PERFORMANCE, "TickLog", "---=== Latest Performance ===---");
        
        for(Metric metric : Metric.values())
        {
            String strResult = "                                        ";
            String strName = metric.name();
            String strTime = Long.toString(LatestSamples[metric.ordinal()]) + "ms";
            LaunchLog.Log(LaunchLog.LogType.PERFORMANCE, "TickLog", strName + strResult.substring(strName.length() + strTime.length()) + strTime);
            
        }
        
        LaunchLog.Log(LaunchLog.LogType.PERFORMANCE, "TickLog", "--------------------------------");
    }
}
