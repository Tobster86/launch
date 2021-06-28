package com.apps.fast.launch.launchviews;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.ClientDefs;
import com.apps.fast.launch.components.TextUtilities;
import com.apps.fast.launch.launchviews.controls.AllianceControl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import launch.game.Alliance;
import launch.game.LaunchClientGame;

/**
 * Created by tobster on 16/10/15.
 */
public class DiplomacyView extends LaunchView
{
    private static final int NUMBER_TO_SHOW = 5;

    private enum SortOrder
    {
        Members,
        Worth,
        Offences,
        Defences
    }

    private TextView txtYourAlliance;
    private FrameLayout lytYourAlliance;
    private View viewYourAllianceSeperator;
    private LinearLayout btnCreateAlliance;
    private AllianceView ourAllianceView;

    private List<Alliance> AlliancesFiltered;

    private SortOrder order;

    private EditText txtFilter;
    private TextView btnSortBy;

    private LinearLayout lytAlliances;
    private LinearLayout btnPrevTop;
    private TextView txtFromToTop;
    private LinearLayout btnNextTop;
    private LinearLayout btnPrevBottom;
    private TextView txtFromToBottom;
    private LinearLayout btnNextBottom;

    private Comparator<Alliance> pointsComparator;
    private Comparator<Alliance> rankComparator;
    private Comparator<Alliance> membersComparator;
    private Comparator<Alliance> worthComparator;
    private Comparator<Alliance> offencesComparator;
    private Comparator<Alliance> defencesComparator;

    private List<AllianceView> AllianceViews;

    private int lFrom = 0;

    public DiplomacyView(LaunchClientGame game, MainActivity activity)
    {
        super(game, activity);
    }

    @Override
    protected void Setup()
    {
        AllianceViews = new ArrayList<>();
        order = SortOrder.Members;
        inflate(context, R.layout.view_diplomacy, this);

        txtYourAlliance = (TextView)findViewById(R.id.txtYourAlliance);
        lytYourAlliance = (FrameLayout)findViewById(R.id.lytYourAlliance);
        viewYourAllianceSeperator = (View)findViewById(R.id.viewYourAllianceSeperator);

        btnCreateAlliance = (LinearLayout) findViewById(R.id.btnCreateAlliance);

        if(game.GetOurPlayer().GetAllianceMemberID() == Alliance.ALLIANCE_ID_UNAFFILIATED)
        {
            btnCreateAlliance.setVisibility(VISIBLE);

            btnCreateAlliance.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    if(!game.GetOurPlayer().GetAllianceCooloffExpired())
                    {
                        activity.ShowBasicOKDialog(context.getString(R.string.cannot_ally, TextUtilities.GetTimeAmount(game.GetOurPlayer().GetAllianceCooloffRemaining())));
                    }
                    else if(game.GetOurPlayer().GetRespawnProtected())
                    {
                        activity.ShowBasicOKDialog(context.getString(R.string.alliance_create_rspawnprot, TextUtilities.GetTimeAmount(game.GetOurPlayer().GetStateTimeRemaining())));
                    }
                    else
                    {
                        activity.SetView(new CreateAllianceView(game, activity, ClientDefs.DEFAULT_AVATAR_ID));
                    }
                }
            });

            txtYourAlliance.setVisibility(GONE);
            lytYourAlliance.setVisibility(GONE);
            viewYourAllianceSeperator.setVisibility(GONE);
        }
        else
        {
            btnCreateAlliance.setVisibility(GONE);

            txtYourAlliance.setVisibility(VISIBLE);
            lytYourAlliance.setVisibility(VISIBLE);
            viewYourAllianceSeperator.setVisibility(VISIBLE);

            final Alliance ourAlliance = game.GetAlliance(game.GetOurPlayer().GetAllianceMemberID());

            ourAllianceView = new AllianceView(game, activity, ourAlliance);

            ourAllianceView.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    activity.SetView(new AllianceControl(game, activity, ourAlliance));
                }
            });

            lytYourAlliance.addView(ourAllianceView);
        }

        txtFilter = (EditText)findViewById(R.id.txtFilter);
        btnSortBy = (TextView)findViewById(R.id.btnSortBy);
        lytAlliances = (LinearLayout)findViewById(R.id.lytAlliances);
        btnPrevTop = (LinearLayout)findViewById(R.id.btnPrevTop);
        txtFromToTop = (TextView)findViewById(R.id.txtFromToTop);
        btnNextTop = (LinearLayout)findViewById(R.id.btnNextTop);
        btnPrevBottom = (LinearLayout)findViewById(R.id.btnPrevBottom);
        txtFromToBottom = (TextView)findViewById(R.id.txtFromToBottom);
        btnNextBottom = (LinearLayout)findViewById(R.id.btnNextBottom);

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

        //Create comparators.
        membersComparator = new Comparator<Alliance>()
        {
            @Override
            public int compare(Alliance allianceOne, Alliance allianceTOther)
            {
                int lMembers1 = game.GetAllianceMemberCount(allianceOne);
                int lMembers2 = game.GetAllianceMemberCount(allianceTOther);

                return lMembers2 - lMembers1;
            }
        };

        worthComparator = new Comparator<Alliance>()
        {
            @Override
            public int compare(Alliance allianceOne, Alliance allianceTOther)
            {
                return game.GetAllianceTotalValue(allianceTOther) - game.GetAllianceTotalValue(allianceOne);
            }
        };

        offencesComparator = new Comparator<Alliance>()
        {
            @Override
            public int compare(Alliance allianceOne, Alliance allianceTOther)
            {
                return game.GetAllianceOffenseValue(allianceTOther) - game.GetAllianceOffenseValue(allianceOne);
            }
        };

        defencesComparator = new Comparator<Alliance>()
        {
            @Override
            public int compare(Alliance allianceOne, Alliance allianceTOther)
            {
                return game.GetAllianceDefenseValue(allianceTOther) - game.GetAllianceDefenseValue(allianceOne);
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

                String[] SortOrderNames = new String[]
                        {
                                context.getString(R.string.sort_name_members),
                                context.getString(R.string.sort_name_worth),
                                context.getString(R.string.sort_name_offences),
                                context.getString(R.string.sort_name_defences)
                        };

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

        DrawList();
    }

    @Override
    public void Update()
    {
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                for(AllianceView view : AllianceViews)
                {
                    view.Update();
                }
            }
        });
    }

    private void PrevClicked()
    {
        lFrom -= NUMBER_TO_SHOW;
        lFrom = Math.max(lFrom, 0);
        DrawList();
    }

    private void NextClicked()
    {
        if(lFrom + NUMBER_TO_SHOW < AlliancesFiltered.size())
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
                lytAlliances.removeAllViews();
                AllianceViews.clear();

                //Sort the alliances into a filtered list.
                String strFilter = txtFilter.getText().toString().toLowerCase();
                AlliancesFiltered = new ArrayList();
                for(Alliance alliance : game.GetAlliances())
                {
                    if(alliance.GetName().toLowerCase().contains(strFilter))
                    {
                        AlliancesFiltered.add(alliance);
                    }
                }

                switch (order)
                {
                    case Members:
                    {
                        Collections.sort(AlliancesFiltered, membersComparator);
                        btnSortBy.setText(context.getString(R.string.sort_members));
                    }
                    break;

                    case Worth:
                    {
                        Collections.sort(AlliancesFiltered, worthComparator);
                        btnSortBy.setText(context.getString(R.string.sort_worth));
                    }
                    break;

                    case Offences:
                    {
                        Collections.sort(AlliancesFiltered, offencesComparator);
                        btnSortBy.setText(context.getString(R.string.sort_offences));
                    }
                    break;

                    case Defences:
                    {
                        Collections.sort(AlliancesFiltered, defencesComparator);
                        btnSortBy.setText(context.getString(R.string.sort_defences));
                    }
                    break;
                }

                String strTextFromTo = context.getString(R.string.number_range, lFrom + 1, Math.min(lFrom + NUMBER_TO_SHOW, AlliancesFiltered.size()));

                txtFromToTop.setText(strTextFromTo);
                txtFromToBottom.setText(strTextFromTo);

                //Create listing.
                for(int i = lFrom; i < Math.min(lFrom + NUMBER_TO_SHOW, AlliancesFiltered.size()); i++)
                {
                    final Alliance alliance = AlliancesFiltered.get(i);

                    AllianceView allianceView = new AllianceView(game, activity, alliance);

                    allianceView.setOnClickListener(new OnClickListener()
                    {
                        @Override
                        public void onClick(View view)
                        {
                            activity.SetView(new AllianceControl(game, activity, alliance));
                        }
                    });

                    lytAlliances.addView(allianceView);

                    AllianceViews.add(allianceView);
                }
            }
        });
    }

    @Override
    public void AvatarSaved(int lAvatarID)
    {
        if(game.GetOurPlayer().GetAllianceMemberID() != Alliance.ALLIANCE_ID_UNAFFILIATED)
        {
            ourAllianceView.AvatarSaved(lAvatarID);
        }

        for(int i = 0; i < lytAlliances.getChildCount(); i++)
        {
            View view = lytAlliances.getChildAt(i);

            if(view instanceof AllianceView)
            {
                ((AllianceView)view).AvatarSaved(lAvatarID);
            }
        }
    }
}
