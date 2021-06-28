package com.apps.fast.launch.components;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.util.Log;

import com.apps.fast.launch.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import launch.utilities.ShortDelay;

/**
 * Created by tobster on 16/03/16.
 */
public class Sounds
{
    private static Context context;
    private static Random rand = new Random();

    private static final int MAX_SOUND_INTERVAL = 100;
    private static long oLastSoundPlayed = 0;

    private static List<Integer> NearExplosions;
    private static List<Integer> FarExplosions;
    private static List<Integer> Moneys;
    private static List<Integer> MissileLaunches;
    private static List<Integer> InterceptorLaunches;
    private static List<Integer> Constructions;
    private static List<Integer> InterceptorMisses;
    private static List<Integer> InterceptorHits;
    private static List<Integer> SentryHits;
    private static List<Integer> SentryMisses;
    private static List<Integer> Reconfigs;
    private static List<Integer> Equips;
    private static List<Integer> Respawns;
    private static List<Integer> Deaths;
    private static List<Integer> Repairs;
    private static List<Integer> Heals;

    private static List<MediaPlayer> MediaPlayers = new ArrayList<>();

    private static boolean bDisabled;

    public static void Init(Context ctx)
    {
        NearExplosions = new ArrayList<>();
        FarExplosions = new ArrayList<>();
        Moneys = new ArrayList<>();
        MissileLaunches = new ArrayList<>();
        InterceptorLaunches = new ArrayList<>();
        Constructions = new ArrayList<>();
        InterceptorMisses = new ArrayList<>();
        InterceptorHits = new ArrayList<>();
        SentryHits = new ArrayList<>();
        SentryMisses = new ArrayList<>();
        Reconfigs = new ArrayList<>();
        Equips = new ArrayList<>();
        Respawns = new ArrayList<>();
        Deaths = new ArrayList<>();
        Repairs = new ArrayList<>();
        Heals = new ArrayList<>();

        NearExplosions.add(R.raw.nearexplosion1);

        FarExplosions.add(R.raw.farexplosion1);
        FarExplosions.add(R.raw.farexplosion2);

        Moneys.add(R.raw.money1);

        MissileLaunches.add(R.raw.missilelaunch1);
        MissileLaunches.add(R.raw.missilelaunch2);
        MissileLaunches.add(R.raw.missilelaunch3);

        InterceptorLaunches.add(R.raw.interceptor1);
        InterceptorLaunches.add(R.raw.interceptor2);
        InterceptorLaunches.add(R.raw.interceptor3);
        InterceptorLaunches.add(R.raw.interceptor4);

        Constructions.add(R.raw.construction1);

        InterceptorMisses.add(R.raw.interceptormiss1);

        InterceptorHits.add(R.raw.interceptorhit1);

        SentryHits.add(R.raw.sentryhit1);

        SentryMisses.add(R.raw.sentrymiss1);
        SentryMisses.add(R.raw.sentrymiss2);
        SentryMisses.add(R.raw.sentrymiss3);

        Reconfigs.add(R.raw.reconfig1);

        Equips.add(R.raw.equip1);

        Respawns.add(R.raw.respawn1);

        Deaths.add(R.raw.death1);

        context = ctx;

        SharedPreferences sharedPreferences = context.getSharedPreferences(ClientDefs.SETTINGS, Context.MODE_PRIVATE);
        bDisabled = sharedPreferences.getBoolean(ClientDefs.SETTINGS_DISABLE_AUDIO, ClientDefs.SETTINGS_DISABLE_AUDIO_DEFAULT);
    }

    public static void SetDisabled(boolean bSetDisabled)
    {
        bDisabled = bSetDisabled;
    }

    private static void PlaySound(List<Integer> FromLibrary)
    {
        if(!bDisabled)
        {
            if(System.currentTimeMillis() > oLastSoundPlayed + MAX_SOUND_INTERVAL)
            {
                if (FromLibrary.size() > 0)
                {
                    Log.i("Sound", "Playing sound.");
                    int lSound = FromLibrary.get(rand.nextInt(FromLibrary.size()));

                    MediaPlayer mediaPlayer = MediaPlayer.create(context, lSound);
                    mediaPlayer.start();

                    MediaPlayers.add(mediaPlayer);

                    //Clean up finished media players.
                    for (int i = 0; i < MediaPlayers.size(); i++)
                    {
                        try
                        {
                            if (!MediaPlayers.get(i).isPlaying())
                            {
                                Log.i("Sound", "Cleaning up a dead sound.");
                                MediaPlayers.get(i).release();
                                MediaPlayers.remove(i--);
                            }
                        }
                        catch(Exception ex)
                        {
                            Log.i("Sound", "Couldn't clean up the sound, so let's just remove it.");
                            MediaPlayers.remove(i--);
                        }
                    }
                }
                else
                {
                    Log.i("Sound", "No sounds for that library.");
                }

                oLastSoundPlayed = System.currentTimeMillis();
            }
            else
            {
                Log.i("Sound", "Skipping a sound as too many are playing.");
            }
        }
    }

    public static void PlayNearExplosion()
    {
        PlaySound(NearExplosions);
    }

    public static void PlayFarExplosion()
    {
        PlaySound(FarExplosions);
    }

    public static void PlayMoney()
    {
        PlaySound(Moneys);
    }

    public static void PlayMissileLaunch()
    {
        PlaySound(MissileLaunches);
    }

    public static void PlayInterceptorLaunch()
    {
        PlaySound(InterceptorLaunches);
    }

    public static void PlayConstruction()
    {
        PlaySound(Constructions);
    }

    public static void PlayInterceptorMiss()
    {
        PlaySound(InterceptorMisses);
    }

    public static void PlayInterceptorHit()
    {
        PlaySound(InterceptorHits);
    }

    public static void PlaySentryGunHit()
    {
        PlaySound(SentryHits);
    }

    public static void PlaySentryGunMiss()
    {
        PlaySound(SentryMisses);
    }

    public static void PlayReconfig()
    {
        PlaySound(Reconfigs);
    }

    public static void PlayEquip() { PlaySound(Equips); }

    public static void PlayRespawn() { PlaySound(Respawns); }

    public static void PlayDeath() { PlaySound(Deaths); }

    public static void PlayRepair() { PlaySound(Repairs); }

    public static void PlayHeal() { PlaySound(Heals); }
}
