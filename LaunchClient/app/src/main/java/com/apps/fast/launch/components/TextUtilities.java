package com.apps.fast.launch.components;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.TextView;

import com.apps.fast.launch.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import launch.game.LaunchGame;
import launch.game.entities.*;

/**
 * Created by tobster on 05/11/15.
 */
public class TextUtilities
{
    private static Context context;

    public enum ShortUnits
    {
        METERS,
        YARDS,
        FEET
    }

    public enum LongUnits
    {
        KILOMETERS,
        STATUTE_MILES,
        NAUTICAL_MILES
    }

    public enum Speeds
    {
        KILOMETERS_PER_HOUR,
        MILES_PER_HOUR,
        KNOTS
    }

    public static final int UNITS_METRIC = 0;
    public static final int UNITS_IMPERIAL = 1;
    public static final int UNITS_MARINE = 2;

    public static final long A_DAY = 86400000;
    public static final long AN_HOUR = 3600000;
    public static final long A_MINUTE = 60000;
    public static final long A_SEC = 1000;

    private static final SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm:ss");
    private static final SimpleDateFormat sdfDayAndTime = new SimpleDateFormat("dd/MM HH:mm");
    private static final SimpleDateFormat sdfDayAndFullTime = new SimpleDateFormat("dd/MM HH:mm:ss");
    private static final SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM/yy");

    private static final double COMPASS_DIVISIONS = (Math.PI * 2.0) / 8.0;
    private static final double COMPASS_DIV_OFFSET = COMPASS_DIVISIONS / 2.0;

    private static final float FRACTION_TO_PERCENT = 100.0f;

    private static final float KM_PER_METER = 0.001f;
    private static final float KM_PER_YARD = 0.0009144f;
    private static final float KM_PER_FOOT = 0.0003048f;
    private static final float KM_PER_MILE = 1.60934f;
    private static final float KM_PER_NM = 1.852f;
    private static final float KPH_PER_MPH = 1.60934f;
    private static final float KPH_PER_KNOT = 1.852f;

    private static final String FORMAT_STRING_METERS = "%dm";
    private static final String FORMAT_STRING_YARDS = "%dyd";
    private static final String FORMAT_STRING_FEET = "%dft";
    private static final String FORMAT_STRING_KILOMETERS = "%.1fkm";
    private static final String FORMAT_STRING_STATUTE_MILES = "%.1fmi";
    private static final String FORMAT_STRING_NAUTICAL_MILES = "%.1fnm";
    private static final String FORMAT_STRING_KPH = "%dkm/h";
    private static final String FORMAT_STRING_MPH = "%dmph";
    private static final String FORMAT_STRING_KNOTS = "%dkts";

    private static final String FORMAT_STRING_LATENCY = "%dms";

    private static final int BINARY_THOUSAND = 1024;

    private static ShortUnits shortUnits;
    private static LongUnits longUnits;
    private static Speeds speeds;
    private static String strCurrencySymbol;

    public static void Initialise(Context ctx)
    {
        context = ctx;

        SharedPreferences sharedPreferences = ctx.getSharedPreferences(ClientDefs.SETTINGS, Context.MODE_PRIVATE);
        shortUnits = ShortUnits.values()[sharedPreferences.getInt(ClientDefs.SETTINGS_SHORT_UNITS, ClientDefs.SETTINGS_UNITS_DEFAULT)];
        longUnits = LongUnits.values()[sharedPreferences.getInt(ClientDefs.SETTINGS_LONG_UNITS, ClientDefs.SETTINGS_UNITS_DEFAULT)];
        speeds = Speeds.values()[sharedPreferences.getInt(ClientDefs.SETTINGS_SPEEDS, ClientDefs.SETTINGS_UNITS_DEFAULT)];
        strCurrencySymbol = sharedPreferences.getString(ClientDefs.SETTINGS_CURRENCY, ClientDefs.SETTINGS_CURRENCY_DEFAULT);
    }

    private static String GetShortDistanceStringFromKM(float fltDistanceKM)
    {
        switch(shortUnits)
        {
            case METERS: return String.format(FORMAT_STRING_METERS, (int)((fltDistanceKM / KM_PER_METER) + 0.5f));
            case YARDS: return String.format(FORMAT_STRING_YARDS, (int)((fltDistanceKM / KM_PER_YARD) + 0.5f));
            case FEET: return String.format(FORMAT_STRING_FEET, (int)((fltDistanceKM / KM_PER_FOOT) + 0.5f));
        }

        return "Short unit error!";
    }

    public static String GetDistanceStringFromKM(float fltDistanceKM)
    {
        switch(longUnits)
        {
            case KILOMETERS:
                if(fltDistanceKM < 1.0f)
                    return GetShortDistanceStringFromKM(fltDistanceKM);
                return String.format(FORMAT_STRING_KILOMETERS, fltDistanceKM);

            case STATUTE_MILES:
                if(fltDistanceKM < KM_PER_MILE)
                    return GetShortDistanceStringFromKM(fltDistanceKM);
                return String.format(FORMAT_STRING_STATUTE_MILES, fltDistanceKM / KM_PER_MILE);

            case NAUTICAL_MILES:
                if(fltDistanceKM < KM_PER_NM)
                    return GetShortDistanceStringFromKM(fltDistanceKM);
                return String.format(FORMAT_STRING_NAUTICAL_MILES, fltDistanceKM / KM_PER_NM);
        }

        return "Long unit error!";
    }

    public static String GetDistanceStringFromM(float fltDistanceM)
    {
        return GetDistanceStringFromKM(fltDistanceM * KM_PER_METER);
    }

    public static String GetSpeedFromKph(float fltSpeed)
    {
        switch(speeds)
        {
            case KILOMETERS_PER_HOUR: return String.format(FORMAT_STRING_KPH, (int)(fltSpeed + 0.5f));
            case MILES_PER_HOUR: return String.format(FORMAT_STRING_MPH, (int)((fltSpeed / KPH_PER_MPH) + 0.5f));
            case KNOTS: return String.format(FORMAT_STRING_KNOTS, (int)((fltSpeed / KPH_PER_KNOT) + 0.5f));
        }

        return "Speed unit error!";
    }

    public static String GetCurrencyString(int lMoney)
    {
        return strCurrencySymbol + lMoney;
    }

    public static String GetTimeAmount(long oTimespan)
    {
        long oDays = oTimespan / A_DAY;
        long oHours = (oTimespan % A_DAY) / AN_HOUR;
        long oMinutes = ((oTimespan % A_DAY) % AN_HOUR) / A_MINUTE;
        long oSeconds = (((oTimespan % A_DAY) % AN_HOUR) % A_MINUTE) / A_SEC;

        if(oDays > 0)
        {
            return oDays + "days, " + oHours + ":" + String.format("%02d", oMinutes) + ":" + String.format("%02d", oSeconds);
        }
        else if(oHours > 0)
        {
            return oHours + ":" + String.format("%02d", oMinutes) + ":" + String.format("%02d", oSeconds);
        }
        else if(oMinutes > 0)
        {
            return String.format("%02d", oMinutes) + ":" + String.format("%02d", oSeconds);
        }
        else
        {
            return oSeconds + "s";
        }
    }

    public static String GetLatencyString(long oLatency)
    {
        return String.format(FORMAT_STRING_LATENCY, oLatency);
    }

    public static String GetFutureTime(long oInFuture)
    {
        long oTime = System.currentTimeMillis() + oInFuture;

        Calendar cal = GregorianCalendar.getInstance();
        cal.setTimeInMillis(oTime);

        if(oInFuture > A_DAY)
        {
            return sdfDayAndTime.format(cal.getTime());
        }

        return sdfTime.format(cal.getTime());
    }

    public static String GetDate(Calendar cal)
    {
        return sdfDate.format(cal.getTime());
    }

    public static String GetTime(Calendar cal)
    {
        return sdfTime.format(cal.getTime());
    }

    public static String GetTime(long oTime)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(oTime);

        return sdfTime.format(cal.getTime());
    }

    public static String GetDateAndTime(long oTime)
    {
        if(oTime == 0)
            return "Never";

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(oTime);

        return sdfDayAndTime.format(cal.getTime());
    }

    public static String GetDateAndTimeRange(long oStart, long oEnd)
    {
        //TO DO: String(s) from resource, and condense the month/day if same?
        Calendar calStart = Calendar.getInstance();
        calStart.setTimeInMillis(oStart);
        Calendar calEnd = Calendar.getInstance();
        calEnd.setTimeInMillis(oEnd);

        return String.format("%s - %s", sdfDayAndTime.format(calStart.getTime()), sdfDayAndTime.format(calEnd.getTime()));
    }

    public static String GetDateAndFullTime(long oTime)
    {
        if(oTime == 0)
            return "Never";

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(oTime);

        return sdfDayAndFullTime.format(cal.getTime());
    }

    public static String QualitativeDirectionFromBearing(double dblBearing)
    {
        //Normalise.
        if(dblBearing < 0)
        {
            dblBearing += (2 * Math.PI);
        }

        if(dblBearing > COMPASS_DIV_OFFSET)
        {
            if(dblBearing < COMPASS_DIVISIONS + COMPASS_DIV_OFFSET)
            {
                return context.getString(R.string.northeast);
            }
            else if(dblBearing < (COMPASS_DIVISIONS * 2) + COMPASS_DIV_OFFSET)
            {
                return context.getString(R.string.east);
            }
            else if(dblBearing < (COMPASS_DIVISIONS * 3) + COMPASS_DIV_OFFSET)
            {
                return context.getString(R.string.southeast);
            }
            else if(dblBearing < (COMPASS_DIVISIONS * 4) + COMPASS_DIV_OFFSET)
            {
                return context.getString(R.string.south);
            }
            else if(dblBearing < (COMPASS_DIVISIONS * 5) + COMPASS_DIV_OFFSET)
            {
                return context.getString(R.string.southwest);
            }
            else if(dblBearing < (COMPASS_DIVISIONS * 6) + COMPASS_DIV_OFFSET)
            {
                return context.getString(R.string.west);
            }
            else if(dblBearing < (COMPASS_DIVISIONS * 7) + COMPASS_DIV_OFFSET)
            {
                return context.getString(R.string.northwest);
            }
        }

        return context.getString(R.string.north);
    }

    public static String GetEntityTypeAndName(LaunchEntity entity, LaunchGame game)
    {
        if(entity instanceof Player)
        {
            return ((Player)entity).GetName();
        }
        else if(entity instanceof Missile)
        {
            return GetMissileAndType((Missile)entity, game);
        }
        else if(entity instanceof Interceptor)
        {
            return GetInterceptorAndType((Interceptor)entity, game);
        }
        else if(entity instanceof MissileSite)
        {
            return context.getString(R.string.missile_site);
        }
        else if(entity instanceof SAMSite)
        {
            return context.getString(R.string.sam_site);
        }
        else if(entity instanceof SentryGun)
        {
            return context.getString(R.string.sentry_gun);
        }
        else if(entity instanceof OreMine)
        {
            return context.getString(R.string.ore_mine);
        }
        else if(entity instanceof Loot)
        {
            return (((Loot)entity).GetDescription().isEmpty() ? context.getString(R.string.loot) : context.getString(R.string.named_entity, context.getString(R.string.loot), ((Loot)entity).GetDescription()));
        }

        return "NOT IMPLEMENTED! (GetEntityTypeAndName)";
    }

    public static String GetOwnedEntityName(LaunchEntity entity, LaunchGame game)
    {
        if(entity instanceof Player)
        {
            Player player = (Player)entity;
            return player.GetName();
        }
        if(entity instanceof Missile)
        {
            Missile missile = (Missile)entity;
            return context.getString(R.string.owners_entity, game.GetPlayer(missile.GetOwnerID()).GetName(), GetMissileAndType(missile, game));
        }
        else if(entity instanceof Interceptor)
        {
            Interceptor interceptor = (Interceptor)entity;
            return context.getString(R.string.owners_entity, game.GetPlayer(interceptor.GetOwnerID()).GetName(), GetInterceptorAndType(interceptor, game));
        }
        else if(entity instanceof MissileSite)
        {
            MissileSite missileSite = (MissileSite)entity;
            return context.getString(R.string.owners_entity, game.GetPlayer(missileSite.GetOwnerID()).GetName(), context.getString(R.string.missile_site_title));
        }
        else if(entity instanceof SAMSite)
        {
            SAMSite samSite = (SAMSite)entity;
            return context.getString(R.string.owners_entity, game.GetPlayer(samSite.GetOwnerID()).GetName(), context.getString(R.string.sam_site_title));
        }
        else if(entity instanceof SentryGun)
        {
            SentryGun sentryGun = (SentryGun) entity;
            return context.getString(R.string.owners_entity, game.GetPlayer(sentryGun.GetOwnerID()).GetName(), context.getString(R.string.sentry_gun_title));
        }
        else if(entity instanceof OreMine)
        {
            OreMine oreMine = (OreMine)entity;
            return context.getString(R.string.owners_entity, game.GetPlayer(oreMine.GetOwnerID()).GetName(), context.getString(R.string.ore_mine_title));
        }

        return "NOT IMPLEMENTED! (GetOwnedEntityName)";
    }

    public static String GetMissileAndType(Missile missile, LaunchGame game)
    {
        return context.getString(R.string.missile_type, game.GetConfig().GetMissileType(missile.GetType()).GetName(), context.getString(R.string.missile));
    }

    public static String GetInterceptorAndType(Interceptor interceptor, LaunchGame game)
    {
        return context.getString(R.string.missile_type, game.GetConfig().GetInterceptorType(interceptor.GetType()).GetName(), context.getString(R.string.interceptor));
    }

    public static void SetStructureState(TextView textView, Structure structure)
    {
        if(structure.GetOnline())
        {
            textView.setText(R.string.state_online_name);
            textView.setTextColor(Utilities.ColourFromAttr(context, R.attr.GoodColour));
        }

        if(structure.GetBooting())
        {
            textView.setText(R.string.state_booting_name);
            textView.setTextColor(Utilities.ColourFromAttr(context, R.attr.WarningColour));
        }

        if(structure.GetSelling())
        {
            textView.setText(R.string.state_selling_name);
            textView.setTextColor(Utilities.ColourFromAttr(context, R.attr.WarningColour));
        }

        if(structure.GetOffline())
        {
            textView.setText(R.string.state_offline_name);
            textView.setTextColor(Utilities.ColourFromAttr(context, R.attr.BadColour));
        }
    }

    public static String GetPercentStringFromFraction(float fltFraction)
    {
        return String.format("%.0f%%", fltFraction * FRACTION_TO_PERCENT);
    }

    public static String GetLatLongString(float fltLatitude, float fltLongitude)
    {
        return context.getString(R.string.lat_long, String.format("%.6f", fltLatitude), String.format("%.6f", fltLongitude));
    }

    public static void AssignHealthStringAndAppearance (TextView txtHealth, Damagable damagable)
    {
        if(damagable.Destroyed())
        {
            txtHealth.setTextColor(Utilities.ColourFromAttr(context, R.attr.BadColour));

            if(damagable instanceof Player)
            {
                txtHealth.setText(context.getString(R.string.health_dead));
            }
            else
            {
                txtHealth.setText(context.getString(R.string.health_destroyed));
            }
        }
        else
        {
            txtHealth.setText(context.getString(R.string.hps, damagable.GetHP(), damagable.GetMaxHP()));

            float fltDamageRatio = (float) damagable.GetHP() / (float) damagable.GetMaxHP();
            if (fltDamageRatio > 0.999f)
                txtHealth.setTextColor(Utilities.ColourFromAttr(context, R.attr.GoodColour));
            else if (fltDamageRatio > 0.5f)
                txtHealth.setTextColor(Utilities.ColourFromAttr(context, R.attr.WarningColour));
            else
                txtHealth.setTextColor(Utilities.ColourFromAttr(context, R.attr.BadColour));
        }
    }

    public static void AssignHealthStringAndAppearance (TextView txtHealth, List<Structure> Structures)
    {
        short nMinHealth = Short.MAX_VALUE;
        short nMaxHealth = 0;
        short nMinMaxHealth = Short.MAX_VALUE;
        short nMaxMaxHealth = 0;

        for(Structure structure: Structures)
        {
            nMinHealth = (short)Math.min(nMinHealth, structure.GetHP());
            nMaxHealth = (short)Math.max(nMaxHealth, structure.GetHP());
            nMinMaxHealth = (short)Math.min(nMinMaxHealth, structure.GetMaxHP());
            nMaxMaxHealth = (short)Math.max(nMaxMaxHealth, structure.GetMaxHP());
        }

        String strHealthRangeString = (nMinHealth == nMaxHealth) ? Short.toString(nMinHealth) : String.format("%d-%d", nMinHealth, nMaxHealth);
        String strHealthMaxString = (nMinMaxHealth == nMaxMaxHealth) ? Short.toString(nMinMaxHealth) : String.format("%d-%d", nMinMaxHealth, nMaxMaxHealth);
        String strHealthString = String.format("%s/%s", strHealthRangeString, strHealthMaxString);

        txtHealth.setText(strHealthString);

        float fltDamageRatio = (float) nMinHealth / (float) nMinMaxHealth;
        if (fltDamageRatio > 0.999f)
            txtHealth.setTextColor(Utilities.ColourFromAttr(context, R.attr.GoodColour));
        else if (fltDamageRatio > 0.5f)
            txtHealth.setTextColor(Utilities.ColourFromAttr(context, R.attr.WarningColour));
        else
            txtHealth.setTextColor(Utilities.ColourFromAttr(context, R.attr.BadColour));
    }

    public static String GetDamageString(int lDamage)
    {
        return context.getString(R.string.hp, lDamage);
    }

    public static String GetMultiplierString(float fltMultiplier)
    {
        return context.getString(R.string.multiplier, String.format("%.2f", fltMultiplier));
    }

    public static String GetOreMineCompetitionString(int lTotal, int lCompeting, int lMaxValue)
    {
        return context.getString(R.string.ore_mine_competing, lCompeting, lTotal, GetCurrencyString(lMaxValue));
    }

    public static String GetConnectionSpeed(int lBytesPerSecond)
    {
        if(lBytesPerSecond < BINARY_THOUSAND)
        {
            //Bytes per second.
            return context.getString(R.string.value_unknown_download, lBytesPerSecond, context.getString(R.string.bytes_per_sec));
        }
        else if(lBytesPerSecond < BINARY_THOUSAND * BINARY_THOUSAND)
        {
            //KB per sec.
            return context.getString(R.string.value_unknown_download, (int)((float)(lBytesPerSecond) / (float)(BINARY_THOUSAND) + 0.5f), context.getString(R.string.kilobytes_per_sec));
        }
        else
        {
            //MB per sec.
            return context.getString(R.string.value_unknown_download, (int)((float)(lBytesPerSecond) / (float)(BINARY_THOUSAND * BINARY_THOUSAND) + 0.5f), context.getString(R.string.megabytes_per_sec));
        }
    }

    /**
     * Return a damage efficiency string, that is units of the player's currency symbol per hit point.
     * @param fltDamageEfficiency The incoming number, will be reduced to 2dp.
     * @return The damage efficiency string, e.g. "£69.69/hp".
     */
    public static String GetDamageEfficiency(float fltDamageEfficiency)
    {
        return context.getString(R.string.hp_efficiency, strCurrencySymbol, String.format("%.2f", fltDamageEfficiency));
    }
}
