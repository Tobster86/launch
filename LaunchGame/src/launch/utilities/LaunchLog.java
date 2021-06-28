/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.utilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 *
 * @author tobster
 */
public class LaunchLog
{
    private static final String LOG_FORMAT = "logs/%s/%s/%s.log";
    private static final String CONSOLE_MESSAGE_FORMAT = "%s: (%s) %s";
    private static final String FILE_MESSAGE_FORMAT = "%s - %s";
    private static final String FORMAT_TIME_LOG_NAME = "%s (%s)";
    
    private static final DateFormat dateFormatDay = new SimpleDateFormat("yyyyMMdd");
    private static final DateFormat dateFormatTime = new SimpleDateFormat("HHmmss");
    
    public enum LogType
    {
        SESSION,
        COMMS,
        APPLICATION,
        GAME,
        TASKS,
        SERVICES,
        LOCATIONS,
        POISON,
        PERFORMANCE,
        CHEATING,
        NOTIFICATIONS,
        DEVICE_CHECKS,
        SAM_SITE_AI,
        DEBUG
    }
    
    private final static String[] LogFolders = new String[]
    {
        "sessions",
        "comms",
        "application",
        "game",
        "tasks",
        "services",
        "locations",
        "poison",
        "performance",
        "cheating",
        "notifications",
        "device_checks",
        "sam_site_ai",
        "debug"
    };
    
    private final static boolean[] EnabledFileLogs = new boolean[LogType.values().length];
    private final static boolean[] EnabledConsoleLogs = new boolean[LogType.values().length];
    
    public static void SetFileLoggingEnabled(LogType type, boolean bEnabled)
    {
        EnabledFileLogs[type.ordinal()] = bEnabled;
    }
    
    public static void SetConsoleLoggingEnabled(LogType type, boolean bEnabled)
    {
        EnabledConsoleLogs[type.ordinal()] = bEnabled;
    }
    
    public static void SetLoggingEnabled(LogType type, boolean bConsoleLogging, boolean bFileLogging)
    {
        EnabledFileLogs[type.ordinal()] = bFileLogging;
        EnabledConsoleLogs[type.ordinal()] = bConsoleLogging;
    }
    
    public synchronized static void Log(LogType type, String strLogName, String strMessage)
    {
        Date now = Calendar.getInstance().getTime();
        String strTime = dateFormatTime.format(now);
        
        if(EnabledFileLogs[type.ordinal()])
        {
            //Print to file Subject.log with format Timestamp - Message, creating folder/file structure if it doesn't exist (/logs/<date>/<subject>/<log name>.log).
            File file = new File(String.format(LOG_FORMAT, LogFolders[type.ordinal()], dateFormatDay.format(now), strLogName));
            file.getParentFile().mkdirs();
            
            try(PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(file, true))))
            {
                printWriter.println(String.format(FILE_MESSAGE_FORMAT, strTime, strMessage));
            }
            catch(Exception ex)
            {
                System.out.println("LOG ERROR!: " + ex.getMessage());
            }
        }
        
        if(EnabledConsoleLogs[type.ordinal()])
        {
            //Timestamped & decorated console message.
            System.out.println(String.format(CONSOLE_MESSAGE_FORMAT, strTime, strLogName, strMessage));
        }
    }
    
    public static String GetTimeFormattedLogName(String strLogName)
    {
        return String.format(FORMAT_TIME_LOG_NAME, strLogName, dateFormatTime.format(Calendar.getInstance().getTime()));
    }
    
    public static void ConsoleMessage(String strMessage)
    {
        System.out.println(strMessage);
    }
}
