package com.apps.fast.launch.launchviews;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.activities.MainActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import launch.game.LaunchClientGame;
import launch.game.entities.LaunchEntity;
import launch.game.entities.Player;

/**
 * Created by tobster on 16/10/15.
 */
public class PlayersView extends LaunchView
{
    private static final int NUMBER_TO_SHOW = 10;

    private enum SortOrder
    {
        Kills,
        Worth,
        Wealth,
        Offences,
        Defences,
        Deaths,
        DamageInflicted,
        DamageReceived
    }

    private String[] SortOrderNames;

    private void InitialiseSortTitles()
    {
        SortOrderNames = new String[]
        {
                context.getString(R.string.kills),
                context.getString(R.string.sort_name_worth),
                context.getString(R.string.sort_name_wealth),
                context.getString(R.string.sort_name_offences),
                context.getString(R.string.sort_name_defences),
                context.getString(R.string.deaths),
                context.getString(R.string.damage_inflicted),
                context.getString(R.string.damage_received),
        };
    }

    private List<Player> PlayersFiltered;

    private SortOrder order;

    private TextView txtGettingStats;
    private LinearLayout lytYourLot;
    private EditText txtFilter;
    private TextView btnSortBy;
    private LinearLayout lytPlayers;
    private LinearLayout btnPrevTop;
    private TextView txtFromToTop;
    private LinearLayout btnNextTop;
    private LinearLayout btnPrevBottom;
    private TextView txtFromToBottom;
    private LinearLayout btnNextBottom;

    private Comparator<Player> worthComparator;
    private Comparator<Player> wealthComparator;
    private Comparator<Player> offencesComparator;
    private Comparator<Player> defencesComparator;
    private Comparator<Player> killsComparator;
    private Comparator<Player> deathsComparator;
    private Comparator<Player> damageInflictedComparator;
    private Comparator<Player> damageReceivedComparator;

    private Map<Integer, PlayerRankView> PlayerRankViews;

    private int lFrom = 0;
    private int lGotStats = 0;

    public PlayersView(LaunchClientGame game, MainActivity activity)
    {
        super(game, activity);
    }

    @Override
    protected void Setup()
    {
        InitialiseSortTitles();
        PlayerRankViews = new HashMap();
        order = SortOrder.Kills;
        inflate(context, R.layout.view_players, this);

        txtGettingStats = findViewById(R.id.txtGettingStats);
        lytYourLot = findViewById(R.id.lytYourLot);
        txtFilter = findViewById(R.id.txtFilter);
        btnSortBy = findViewById(R.id.btnSortBy);
        lytPlayers = findViewById(R.id.lytPlayers);
        btnPrevTop = findViewById(R.id.btnPrevTop);
        txtFromToTop = findViewById(R.id.txtFromToTop);
        btnNextTop = findViewById(R.id.btnNextTop);
        btnPrevBottom = findViewById(R.id.btnPrevBottom);
        txtFromToBottom = findViewById(R.id.txtFromToBottom);
        btnNextBottom = findViewById(R.id.btnNextBottom);

        txtFilter.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2)
            {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2)
            {
                DrawList();
            }

            @Override
            public void afterTextChanged(Editable editable)
            {

            }
        });

        worthComparator = new Comparator<Player>()
        {
            @Override
            public int compare(Player playerOne, Player playerTOther)
            {
                //AWOL players always come last.
                if(!playerOne.GetAWOL() && playerTOther.GetAWOL())
                {
                    return -1;
                }

                if(playerOne.GetAWOL() && !playerTOther.GetAWOL())
                {
                    return 1;
                }

                return game.GetPlayerTotalValue(playerTOther) - game.GetPlayerTotalValue(playerOne);
            }
        };

        wealthComparator = new Comparator<Player>()
        {
            @Override
            public int compare(Player playerOne, Player playerTOther)
            {
                //AWOL players always come last.
                if(!playerOne.GetAWOL() && playerTOther.GetAWOL())
                {
                    return -1;
                }

                if(playerOne.GetAWOL() && !playerTOther.GetAWOL())
                {
                    return 1;
                }

                return playerTOther.GetWealth() - playerOne.GetWealth();
            }
        };

        offencesComparator = new Comparator<Player>()
        {
            @Override
            public int compare(Player playerOne, Player playerTOther)
            {
                //AWOL players always come last.
                if(!playerOne.GetAWOL() && playerTOther.GetAWOL())
                {
                    return -1;
                }

                if(playerOne.GetAWOL() && !playerTOther.GetAWOL())
                {
                    return 1;
                }

                return game.GetPlayerOffenseValue(playerTOther) - game.GetPlayerOffenseValue(playerOne);
            }
        };

        defencesComparator = new Comparator<Player>()
        {
            @Override
            public int compare(Player playerOne, Player playerTOther)
            {
                //AWOL players always come last.
                if(!playerOne.GetAWOL() && playerTOther.GetAWOL())
                {
                    return -1;
                }

                if(playerOne.GetAWOL() && !playerTOther.GetAWOL())
                {
                    return 1;
                }

                return game.GetPlayerDefenseValue(playerTOther) - game.GetPlayerDefenseValue(playerOne);
            }
        };

        killsComparator = new Comparator<Player>()
        {
            @Override
            public int compare(Player playerOne, Player playerTOther)
            {
                return playerTOther.GetKills() - playerOne.GetKills();
            }
        };

        deathsComparator = new Comparator<Player>()
        {
            @Override
            public int compare(Player playerOne, Player playerTOther)
            {
                return playerTOther.GetDeaths() - playerOne.GetDeaths();
            }
        };

        damageInflictedComparator = new Comparator<Player>()
        {
            @Override
            public int compare(Player playerOne, Player playerTOther)
            {
                return playerTOther.GetDamageInflicted() - playerOne.GetDamageInflicted();
            }
        };

        damageReceivedComparator = new Comparator<Player>()
        {
            @Override
            public int compare(Player playerOne, Player playerTOther)
            {
                return playerTOther.GetDamageReceived() - playerOne.GetDamageReceived();
            }
        };

        btnSortBy.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                //Display dialog with sort by options.
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(context.getString(R.string.sort_by));

                builder.setSingleChoiceItems(SortOrderNames, order.ordinal(), new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        order = SortOrder.values()[i];
                        dialogInterface.dismiss();
                        DrawList();
                    }
                });

                builder.show();
            }
        });

        btnPrevTop.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                PrevClicked();
            }
        });

        btnNextTop.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                NextClicked();
            }
        });

        btnPrevBottom.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                PrevClicked();
            }
        });

        btnNextBottom.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                NextClicked();
            }
        });

        GetStats();
    }

    private void PrevClicked()
    {
        lFrom -= NUMBER_TO_SHOW;
        lFrom = Math.max(lFrom, 0);
        DrawList();
    }

    private void NextClicked()
    {
        if(lFrom + NUMBER_TO_SHOW < PlayersFiltered.size())
        {
            lFrom += NUMBER_TO_SHOW;
            DrawList();
        }
    }

    private void DrawList()
    {
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                lytPlayers.removeAllViews();
                PlayerRankViews.clear();

                //Sort the players into a filtered list.
                String strFilter = txtFilter.getText().toString().toLowerCase();
                PlayersFiltered = new ArrayList();
                for(Player player : game.GetPlayers())
                {
                    if(player.GetName().toLowerCase().contains(strFilter))
                    {
                        PlayersFiltered.add(player);
                    }
                }

                switch (order)
                {
                    case Worth:
                    {
                        Collections.sort(PlayersFiltered, worthComparator);
                        btnSortBy.setText(context.getString(R.string.sort_worth));
                    }
                    break;

                    case Wealth:
                    {
                        Collections.sort(PlayersFiltered, wealthComparator);
                        btnSortBy.setText(context.getString(R.string.sort_wealth));
                    }
                    break;

                    case Offences:
                    {
                        Collections.sort(PlayersFiltered, offencesComparator);
                        btnSortBy.setText(context.getString(R.string.sort_offences));
                    }
                    break;

                    case Defences:
                    {
                        Collections.sort(PlayersFiltered, defencesComparator);
                        btnSortBy.setText(context.getString(R.string.sort_defences));
                    }
                    break;

                    case Kills:
                    {
                        Collections.sort(PlayersFiltered, killsComparator);
                        btnSortBy.setText(context.getString(R.string.kills));
                    }
                    break;

                    case Deaths:
                    {
                        Collections.sort(PlayersFiltered, deathsComparator);
                        btnSortBy.setText(context.getString(R.string.deaths));
                    }
                    break;

                    case DamageInflicted:
                    {
                        Collections.sort(PlayersFiltered, damageInflictedComparator);
                        btnSortBy.setText(context.getString(R.string.damage_inflicted));
                    }
                    break;

                    case DamageReceived:
                    {
                        Collections.sort(PlayersFiltered, damageReceivedComparator);
                        btnSortBy.setText(context.getString(R.string.damage_received));
                    }
                    break;
                }

                String strTextFromTo = context.getString(R.string.number_range, lFrom + 1, Math.min(lFrom + NUMBER_TO_SHOW, PlayersFiltered.size()));

                txtFromToTop.setText(strTextFromTo);
                txtFromToBottom.setText(strTextFromTo);

                //Create listing.
                for(int i = lFrom; i < Math.min(lFrom + NUMBER_TO_SHOW, PlayersFiltered.size()); i++)
                {
                    final Player player = PlayersFiltered.get(i);

                    PlayerRankView playerView = new PlayerRankView(game, activity, player);

                    if (!player.GetAWOL() || game.GetOurPlayer().GetIsAnAdmin())
                    {
                        playerView.setOnClickListener(new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View view)
                            {
                                activity.SelectEntity(player);
                            }
                        });
                    }

                    lytPlayers.addView(playerView);
                    PlayerRankViews.put(player.GetID(), playerView);
                }
            }
        });
    }

    private void GetStats()
    {
        //TO DO: Modify this garbage to get a single, full, and ideally compressed block of player stats and respond to it.
        lGotStats = 0;

        //A bit hacky. Request all player stats and wait a few seconds. If we've got "most" of them, draw the list, otherwise request it again.
        for(Player player : game.GetPlayers())
        {
            game.GetPlayerStats(player);
        }

        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {}

                if(lGotStats > (int)((float) game.GetPlayers().size() * 0.75f))
                {
                    lGotStats = game.GetPlayers().size();

                    activity.runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            txtGettingStats.setVisibility(GONE);
                            lytYourLot.setVisibility(VISIBLE);
                        }
                    });

                    DrawList();
                }
                else
                {
                    //Try again.
                    GetStats();
                }
            }
        }).start();
    }

    @Override
    public void EntityUpdated(LaunchEntity entity)
    {
        //Here, we accumulate requested full player stats of all players on first opening the view. We don't care about other player updates.
        //Then, once we've got all of them we draw the list and enable the full UI. We refresh the full UI if we receive any other full stats.
        if(entity instanceof Player)
        {
            if (((Player) entity).GetHasFullStats())
            {
                if(lGotStats < game.GetPlayers().size())
                {
                    lGotStats++;
                }
                else
                {
                    if (PlayerRankViews.containsKey(entity.GetID()))
                    {
                        PlayerRankViews.get(entity.GetID()).RefreshUI();
                    }
                }
            }
        }
    }
}
