using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using I2.Loc;

public class Tutorial : MonoBehaviour
{
    public enum Lessons
    {
        none,
        nation_on_map,
        moving_map,
        past_boundary,
        goal_orbs,
        goal_go_east,
        importance_of_attacking,
        occupy_empty_1,
        occupy_empty_2,
        attack_1,
        attack_fails,
        attack_succeeds,
        attack_2,
        attack_3,
        attack_4,
        attack_5,
        attack_6,
        attack_7,
        attack_8,
        attack_9,
        defense_1,
        defense_2,
        defense_3,
        defense_4,
        defense_5,
        defense_6,
        defense_7,
        defense_8,
        defense_9,
        defense_10,
        defense_11,
        defense_12,
        defense_13,
        defense_14,
        defense_15,
        defense_16,
        defense_17,
        defense_18,
        quests_and_credits,
        summary_1,
        summary_2,
        geo_eff_1_1,
        geo_eff_1_2,
        geo_eff_2_1,
        geo_eff_2_2,
        geo_eff_2_3,
        geo_eff_3,
        geo_eff_4,
        occupy_area,
        evacuate_area,
        occupy_empty,
        mountain_paths,
        resources,
        orbs,
        next_level_boundary,
        upgrade_defense,
        level_up_1,
        level_up_2,
        level_up_3,
        level_up_4,
        level_up_5,
        map_panel_1,
        map_panel_2,
        create_password,
        recruit,
        surround_count,
        energy_reserves_decreasing,
        energy_reserves_empty,
        excess_resources,
        home_island,
        homeland_1,
        homeland_2,
        homeland_3,
        homeland_4,
        homeland_5,
        homeland_6,
        homeland_7,
        homeland_8,
        homeland_9,
        homeland_10,
        homeland_11,
        raid_1,
        raid_2,
        raid_3,
        raid_4,
        raid_5,
        raid_6,
        raid_7,
        raid_8,
        raid_9,
        raid_10,
        raid_11,
        raid_12,
        raid_13,
        raid_14,
        raid_log_1,
        raid_log_2,
        raid_log_3,
        raid_log_4
    }

    public const int SECONDS_PER_DAY = 3600 * 24;

    public static Tutorial instance;

    public RectTransform tutorialPanel;
    public GUITransition tutorialPanelTransition;
    public GameObject buttonArea;
    public TMPro.TextMeshProUGUI buttonText, lessonText;
    public TutorialArrow tutorialArrow;

    public RectTransform xpBar, manpowerBar, energyBar, advancePointBar, geoBar;
    public RectTransform nationPanelButtonLeft, questsPanelButtonLeft, advancesPanelButtonLeft, optionsPanelButtonLeft, connectPanelButtonLeft, raidLogPanelButtonLeft;
    public RectTransform nationPanelButtonBottom, questsPanelButtonBottom, advancesPanelButtonBottom, optionsPanelButtonBottom, connectPanelButtonBottom, raidLogPanelButtonBottom;
    public RectTransform nationStatsTab, connectRecruitTab, raidLeagueTab, researchButton, compassRose, createPasswordButton, nationPanelCloseButton;
    public RectTransform switchMapButton, raidIntroHeader, raidScoreHeader, raidButton, medalsText;

    public Lessons curLesson = Lessons.none;
    public Lessons prevLessonShown = Lessons.none;
    public Dictionary<Lessons, int> presentedLessons = new Dictionary<Lessons, int>();
    public int prevLessonPresentedLoginTime = -1;
    int battleEnemyID, xpAddedAmount;
    float prevPushedFurtherEastTime = -1;
    float prevMaxX1, occupyRunningAvg = 0f, evacRunningAvg = 0f, prevOccupyTime = 0f, prevEvacTime = 0f;
    bool muted = false;
    bool stateChanged = false;

    // Map contingencies
    bool playerNationInView, nationWestOfGreenLine;
    public int adjEnemySquareX, adjEnemySquareZ, adjEnemyID;
    public int adjEmptySquareX, adjEmptySquareZ;
    public int adjEmptyCentralSquareX, adjEmptyCentralSquareZ;
    public int emptyNationSquareX, emptyNationSquareZ;
    public int inertDefenseSquareX, inertDefenseSquareZ;
    int resourceX, resourceZ;
    int orbX, orbZ;
    int greenLineX, greenLineZ;
    int upgradableX, upgradableZ, upgradableObjectID, upgradableUpgradeID;
    int surroundCountX, surroundCountZ;

	public Tutorial()
    {
        instance = this;	
	}

    void Start()
    {
        // Start with the tutorial panel hidden.
        tutorialPanel.gameObject.SetActive(false);
    }

    public void SetMuted(bool _muted)
    {
        muted = _muted;

        if (muted) {
            HideLesson();
        } else if (GameData.instance.userID != -1) {
            InfoEventReceived();
        }
    }

    public void LoadState(string _state_string)
    {
        // Reset the tutorial state
        presentedLessons.Clear();

        // TESTING
        //return;

        // Parse the given _state_string into the presentedLessons dictionary.
        int end_entry_pos = -1, cur_pos = -1, divider_pos, key_int, val_int;
        string key_string, val_string;
        while ((end_entry_pos = _state_string.IndexOf(",", cur_pos + 1)) != -1)
        {
            if ((divider_pos = _state_string.IndexOf(":", cur_pos + 1)) != -1)
            {
                key_string = _state_string.Substring(cur_pos + 1, divider_pos - cur_pos - 1);
                val_string = _state_string.Substring(divider_pos + 1, end_entry_pos - divider_pos - 1);
                //Debug.Log("Tutorial state key: " + key_string + ", val" + val_string);

                if (int.TryParse(key_string, out key_int) && int.TryParse(val_string, out val_int)) {
                    presentedLessons[(Lessons)key_int] = val_int;
                } else {
                    Debug.Log("Unable to parse tutorial key '" + key_string + "' or val '" + val_string + "'.");
                }
            }

            // Advance to end of the current entry.
            cur_pos = end_entry_pos;
        }
    }

    public void SaveState()
    {
        if (stateChanged)
        {
            // Serialize the presentedLessons dictionary.
            string result = "";
            foreach (KeyValuePair<Lessons,int> entry in presentedLessons) {
                result += ((int)(entry.Key)) + ":" + entry.Value + ",";
            }

            // Send the serialized tutorial state to the server.
            Network.instance.SendCommand("action=tutorial_state|val=" + result);

            stateChanged = false;
        }
    }

    public void Restart()
    {
        // Clear and save the list of presented lessons.
        presentedLessons.Clear();
        SaveState();

        prevLessonPresentedLoginTime = -1;
        prevPushedFurtherEastTime = -1;

        // Show the first lesson.
        ShowLesson(Lessons.nation_on_map);
    }
	
    public void ShowLesson(Lessons _lesson)
    {
        //Debug.Log("ShowLesson() called for " + _lesson);

        string lesson_text;
        Lessons prevLesson = curLesson;

        if (muted) {
            return;
        }

        curLesson = _lesson;

        if (curLesson != Lessons.none) {
            prevLessonShown = curLesson;
        }

        // Hide the tutorial arrow
        tutorialArrow.Deactivate();

        switch (curLesson)
        {
            case Lessons.nation_on_map:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_nation_on_map"); // "Welcome! This is our nation, {nation_name}. It's easy to spot because it's the only nation on the map with a <color=yellow>yellow outline</color>. It doesn't look like much now, but under your leadership, I'm sure we will grow into a mighty empire!";
                lessonText.text = lesson_text.Replace("{nation_name}", GameData.instance.nationName);
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                CheckMapContingencies();
                tutorialArrow.ActivateForBlock(emptyNationSquareX, emptyNationSquareZ);
                break;
            case Lessons.moving_map:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_moving_map"); // "You can move around the map by {clicking_pressing} and dragging. You can also zoom in and out by {zoom_method}.";
                lesson_text = lesson_text.Replace("{clicking_pressing}", Input.touchSupported ? LocalizationManager.GetTranslation("Tutorials/pressing") : LocalizationManager.GetTranslation("Tutorials/clicking"));
                lesson_text = lesson_text.Replace("{zoom_method}", Input.touchSupported ? LocalizationManager.GetTranslation("Tutorials/pinching") : LocalizationManager.GetTranslation("Tutorials/desktop_zoom_method"));
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.past_boundary:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_past_boundary"); // "We can only view areas on the map that aren't far beyond where our nation has been. Before we can view further, we'll need to expand and explore in that direction!";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/okay");
                break;
            case Lessons.goal_orbs:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_goal_orbs"); // "It's up to you to lead us on our journey across these harsh lands in search of the ancient Orbs of Power: mystical artifacts that are said to hold the secret to ultimate power!";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.goal_go_east:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_goal_go_east"); // "We will need to capture resources along the way, that will boost our abilities. The further east we travel, the mightier the resources we will find and the more powerful the Orbs. But as we move east we will find more powerful enemies, as well.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                tutorialArrow.ActivateToShowEast();
                break;
            case Lessons.importance_of_attacking:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_importance_of_attacking"); // "Every inch of progress we make will be contested by other nations fighting for the same resources, and the same Orbs. That is why the most important thing to learn as our ruler is how to lead us into battle!";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.occupy_empty_1:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_occupy_empty_1"); // "Our nation is not next to any squares belonging to another nation. So, we must expand our nation before we can attack. {Click_Tap} on an empty square next to a square belonging to our nation, to occupy it.";
                lesson_text = lesson_text.Replace("{Click_Tap}", Input.touchSupported ? LocalizationManager.GetTranslation("Tutorials/Tap") : LocalizationManager.GetTranslation("Tutorials/Click"));
                lessonText.text = lesson_text;
                buttonArea.SetActive(false);
                CheckMapContingencies();
                tutorialArrow.ActivateForBlock(adjEmptyCentralSquareX, adjEmptyCentralSquareZ);
                Debug.Log("Showing tutorial arrow for block " + adjEmptyCentralSquareX + "," + adjEmptyCentralSquareZ);
                break;
            case Lessons.occupy_empty_2:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_occupy_empty_2"); // "Good! Now continue occupying empty squares, expanding toward another nation. You can quickly advance toward a destination by {pressing_clicking} and holding on an empty square and selecting 'Move To'. Once we are next to another nation, we will be able to attack!";
                lesson_text = lesson_text.Replace("{pressing_clicking}", Input.touchSupported ? LocalizationManager.GetTranslation("Tutorials/pressing") : LocalizationManager.GetTranslation("Tutorials/clicking"));
                lessonText.text = lesson_text;
                buttonArea.SetActive(false);
                break;
            case Lessons.attack_1:
                NationData adjNationData = GameData.instance.GetNationData(adjEnemyID);
                string adjacent_nation_name = (adjNationData == null) ? LocalizationManager.GetTranslation("Tutorials/this_nation") : adjNationData.GetName(adjEnemyID != GameData.instance.nationID);
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_attack_1"); // "To attack another nation, just {click_tap} on one of their squares that is next to one of our squares! Try attacking {adjacent_nation_name} now.";
                lesson_text = lesson_text.Replace("{click_tap}", Input.touchSupported ? LocalizationManager.GetTranslation("Tutorials/tap") : LocalizationManager.GetTranslation("Tutorials/click"));
                lessonText.text = lesson_text.Replace("{adjacent_nation_name}", adjacent_nation_name);
                buttonArea.SetActive(false);
                CheckMapContingencies();
                tutorialArrow.ActivateForBlock(adjEnemySquareX, adjEnemySquareZ);
                break;
            case Lessons.attack_fails:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_attack_fails"); // "Often it will take more than one attack to defeat an enemy square. If the enemy is a much more powerful nation, it may take many attacks. If an attack fails, attack again quickly before the enemy has a chance to fully restore their defenses!";
                lessonText.text = lesson_text;
                buttonArea.SetActive(false);
                break;
            case Lessons.attack_succeeds:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_attack_succeeds"); // "Excellent, our attack succeeded! We have taken a square of land from {nation_name}, and we've gained {xp_amount} XP. XP allows us to gain levels and become a more powerful nation, and winning attacks is the main way to gain XP. The more powerful the nation we defeat in an attack, the more XP we earn. Let's keep attacking!";
                lesson_text = lesson_text.Replace("{nation_name}", (GameData.instance.GetNationData(battleEnemyID) == null) ? LocalizationManager.GetTranslation("Tutorials/this_nation") : GameData.instance.GetNationData(battleEnemyID).GetName(true));
                lesson_text = lesson_text.Replace("{xp_amount}", "" + xpAddedAmount);
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                tutorialArrow.ActivateForUI(xpBar);
                break;
            case Lessons.attack_2:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_attack_2"); // "During a battle, the top bar shows how much manpower our nation has committed to this attack. If that bar reaches empty, our attack has failed!";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.attack_3:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_attack_3"); // "The bottom bar shows how many hit points the enemy square we are attacking has. If that bar reaches empty, we have won the battle!";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.attack_4:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_attack_4"); // "The icon next to the top bar shows which combat stat we are attacking with: Tech (<sprite=6>), Bio (<sprite=4>) or Psi (<sprite=5>). We automatically attack with the stat that the defending nation is weakest to defend against.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.attack_5:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_attack_5"); // "The icon next to the bottom bar shows which combat stat the defending nation is defending with. They automatically use their stat that defends best against what we are attacking with.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.attack_6:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_attack_6"); // "As we gain levels, we can develop advances that improve our combat stats. The combat stats are like rock/paper/scissors: Tech is strong against Bio, which is strong against Psi, which in turn is strong against Tech.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.attack_7:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_attack_7"); // "So, depending how we develop our combat stats, we may become very strong against some nations but not as strong against others.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.attack_8:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_attack_8"); // "Each time we attack, we lose some manpower in the battle. Our manpower reserves gradually fill back up over time.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.attack_9:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_attack_9"); // "We can attack {num_simul_attacks} squares at one time. As we gain advances at higher levels, we'll be able to attack more squares at once. Let's capture another square!";
                lesson_text = lesson_text.Replace("{num_simul_attacks}", "" + GameData.instance.maxSimultaneousProcesses);
                lessonText.text = lesson_text;
                buttonArea.SetActive(false);
                break;
            case Lessons.defense_1:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_defense_1"); // "Good work! It will also be important to learn how to build defenses. You'll build defenses around areas you want to protect, such as resources and Orbs we've captured, to make it more difficult for other nations to take them away. Let's try building a defense now.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/okay");
                break;
            case Lessons.defense_2:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_defense_2"); // "To build a defense, {click_tap} on an empty square owned by our nation, and select 'Build...' from the pop-up menu.";
                lesson_text = lesson_text.Replace("{click_tap}", Input.touchSupported ? LocalizationManager.GetTranslation("Tutorials/tap") : LocalizationManager.GetTranslation("Tutorials/click"));
                lessonText.text = lesson_text;
                buttonArea.SetActive(false);
                CheckMapContingencies();
                tutorialArrow.ActivateForBlock(emptyNationSquareX, emptyNationSquareZ);
                break;
            case Lessons.defense_3:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_defense_3"); // "The build menu shows the different types of defenses you can build. Walls add to the hit points of a square and make it more difficult for an enemy to conquer that square, while other defenses such as cannons counter-attack any enemies who attack us nearby. As our level advances, many more defenses with powerful abilities will become available. Select a wall or cannon to build it.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(false);
                break;
            case Lessons.defense_4:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_defense_4"); // "Very good! Each defense we build has a small cost in manpower (<sprite=0>) to build, as well as an ongoing cost in energy (<sprite=1>) to maintain.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.defense_5:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_defense_5"); // "Our nation is constantly generating energy, and as long as we generate energy faster than we use it to maintain our defenses, we're in good shape. This bar shows how much excess energy we generate beyond what our defenses burn. Keep it above zero to avoid running out.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.defense_6:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_defense_6"); // "To support the defenses we build, we also need to be able to move energy around our nation efficiently. This is called Geographic Efficiency, and is shown by the green bar. To keep it close to 100%, our nation needs to occupy some large areas of contiguous land rather than just small, disconnected areas.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.defense_7:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_defense_7"); // "If we build more defenses than we have the energy to support, or if our Geographic Efficiency falls below 100%, then some of our defenses will become inert and stop functioning. If a defense glows red, that means it's inert.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                CheckMapContingencies();
                tutorialArrow.ActivateForBlock(inertDefenseSquareX, inertDefenseSquareZ);
                break;
            case Lessons.defense_8:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_defense_8"); // "[Right now our Geographic Efficiency is only {geo_eff}, so {inert} of the defenses we build will be inert! This is easy enough to fix. ] We can always see how to improve our Geographic Efficiency and energy use by going to the Stats tab on the Nation Panel. Let's look at the stats tab now.";
                if (GameData.instance.GetFinalGeoEfficiency() < 1f)
                {
                    lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_defense_8a") + lesson_text;
                    lesson_text = lesson_text.Replace("{geo_eff}", GameData.instance.GetStatValueString(GameData.Stat.GEOGRAPHIC_EFFICIENCY));
                    lesson_text = lesson_text.Replace("{inert}", string.Format("{0:n0}", (int)((1f - GameData.instance.GetFinalGeoEfficiency()) * 100f + 0.5f)) + "%");
                }
                lessonText.text = lesson_text;
                buttonArea.SetActive(false);
                break;
            case Lessons.defense_9:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_defense_9"); // "The Stats tab has all of the information you'll ever need to run our nation effectively. {Click_Tap} on any stat to learn more about it. Under Research, you'll see our three combat stats (Tech, Bio and Psi) that influence how well we do in battle against other nations.";
                lesson_text = lesson_text.Replace("{Click_Tap}", Input.touchSupported ? LocalizationManager.GetTranslation("Tutorials/Tap") : LocalizationManager.GetTranslation("Tutorials/Click"));
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.defense_10:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_defense_10"); // "Further down, under Defense, you'll see Energy Generation Rate, Energy Burn Rate, and Energy Reserves. Energy Burn Rate is how much energy all of our defenses require each hour to maintain. Our Energy Generation Rate will increase as we develop higher level advances. If we burn more energy than we generate, we'll begin to use up our Energy Reserves. If we run out, some defenses will become inert.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.defense_11:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_defense_11"); // "The effectiveness of our attacks and defenses also depends on how efficiently we can move energy around our nation -- our Geographic Efficiency.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.defense_12:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_defense_12"); // "On the Stats tab under Land Area, you can see that our Geographic Efficiency is now {geo_eff}. We want to keep this at 100%, otherwise some of our defenses will become inert and useless. Geographic efficiency also determines how much benefit we gain from the resources we capture, so the higher it is, the more powerful we'll be.";
                lesson_text = lesson_text.Replace("{geo_eff}", GameData.instance.GetStatValueString(GameData.Stat.GEOGRAPHIC_EFFICIENCY));
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.defense_13:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_defense_13"); // "We can improve our Geographic Efficiency by increasing how much interior land we have (squares that are surrounded by other squares we own) and decreasing the length of our borders. So, occupying a few large areas is much better than many small areas! We should also keep our nation's area below our Supportable Area.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.defense_14:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_defense_14"); // "One more important ability to point out on the Stats tab: under Defense, you'll find the Invisibility stat. We will gain Invisibility if we develop the advance called Mass Illusion.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.defense_15:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_defense_15"); // "Once we have invisibility, each of our defenses (except for walls) will become invisible to attacking nations, until it's been triggered! This will give us a big advantage, because our enemies will be starting out blind to our defense strategy.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.defense_16:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_defense_16"); // "To close the panel, {click_tap} the X in the top corner.";
                lesson_text = lesson_text.Replace("{click_tap}", Input.touchSupported ? LocalizationManager.GetTranslation("Tutorials/tap") : LocalizationManager.GetTranslation("Tutorials/click"));
                lessonText.text = lesson_text;
                buttonArea.SetActive(false);
                break;
            case Lessons.defense_17:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_defense_17"); // "The last thing to learn about defenses is how to salvage them -- that is, to tear them down and get back some of the manpower you spent building them.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.defense_18:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_defense_18"); // "To salvage a defense, {click_tap} on it and select 'Salvage' from the pop-up menu, or 'Salvage and Evacuate' if you also want our nation to leave that square.";
                lesson_text = lesson_text.Replace("{click_tap}", Input.touchSupported ? LocalizationManager.GetTranslation("Tutorials/tap") : LocalizationManager.GetTranslation("Tutorials/click"));
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/got_it");
                break;
            case Lessons.quests_and_credits:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_quests_and_credits"); // "The Quests Panel lists many achievements you can complete to earn rewards of XP and credits! Credits (<sprite=2>) can be used to buy special temporary power-ups, customize the look of our nation, and more.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/okay");
                break;
            case Lessons.summary_1:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_summary_1"); // "You're off to a great start! I'll be back now and then with more advice, but {nation_name} is now in your hands.";
                lesson_text = lesson_text.Replace("{nation_name}", GameData.instance.nationName);
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.summary_2:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_summary_2"); // "Remember, we advance through conquest, so it's always a good idea to attack! Capture resources to improve our stats, and search for the mysterious Orbs! Finally, as we grow more powerful, let's head for the greater treasures to the east!";
                lesson_text = lesson_text.Replace("{nation_name}", GameData.instance.nationName);
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/got_it");
                break;
            case Lessons.geo_eff_1_1:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_geo_eff_1_1"); // "Our nation will have better geographic efficiency, and so generate energy faster, if it has less border area and more interior area. Better to have a few large areas, than many long, thin areas.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.geo_eff_1_2:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_geo_eff_1_2"); // "A good way to help with this is to evacuate land that we no longer need as we move from one area to another. To evacuate, {click_tap} on a square we own, and select 'Evacuate' from the pop up menu.";
                lesson_text = lesson_text.Replace("{click_tap}", Input.touchSupported ? LocalizationManager.GetTranslation("Tutorials/tap") : LocalizationManager.GetTranslation("Tutorials/click"));
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/okay");
                break;
            case Lessons.geo_eff_2_1:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_geo_eff_2_1"); // "We're not generating energy fast enough to support the defenses we've built. We'd generate energy faster if we had better geographic efficiency.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.geo_eff_2_2:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_geo_eff_2_2"); // "This can be improved by expanding so that we have more interior (non-border) area, and evacuating any stray squares so that we have less border area.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/got_it");
                break;
            case Lessons.geo_eff_2_3:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_geo_eff_2_3");
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/got_it");
                break;
            case Lessons.geo_eff_3:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_geo_eff_3"); // "We're not generating energy fast enough to support the defenses we've built, because our geographic efficiency is only {geo_eff}. A nation has better geographic efficiency if it is made up of fewer, larger areas.";
                lesson_text = lesson_text.Replace("{geo_eff}", GameData.instance.GetStatValueString(GameData.Stat.GEOGRAPHIC_EFFICIENCY));
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/okay");
                break;
            case Lessons.geo_eff_4:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_geo_eff_4");
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/got_it");
                break;
            case Lessons.occupy_area:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_occupy_area"); // "You can rapidly occupy a 5x5 area of land by {clicking_pressing} and holding for a moment on empty land, then selecting 'Occupy 5x5' from the pop-up menu.";
                lesson_text = lesson_text.Replace("{clicking_pressing}", Input.touchSupported ? LocalizationManager.GetTranslation("Tutorials/pressing") : LocalizationManager.GetTranslation("Tutorials/clicking"));
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/got_it");
                break;
            case Lessons.evacuate_area:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_evacuate_area"); // "You can rapidly evacuate all of our squares within a 5x5 area of land by {clicking_pressing} and holding for a moment on a square we own, then selecting 'Evacuate 5x5' from the pop-up menu.";
                lesson_text = lesson_text.Replace("{clicking_pressing}", Input.touchSupported ? LocalizationManager.GetTranslation("Tutorials/pressing") : LocalizationManager.GetTranslation("Tutorials/clicking"));
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/okay");
                break;
            case Lessons.occupy_empty:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_occupy_empty"); // "We can also expand our nation by occupying empty land. {Click_Tap} on an empty square next to a square belonging to our nation, to occupy it.";
                lesson_text = lesson_text.Replace("{Click_Tap}", Input.touchSupported ? LocalizationManager.GetTranslation("Tutorials/Tap") : LocalizationManager.GetTranslation("Tutorials/Click"));
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/okay");
                break;
            case Lessons.mountain_paths:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_mountain_paths"); // "Areas with scattered mountains are useful as pathways to move from one area to another. Nations avoid settling and building defenses among mountains because it would hurt their geographic efficiency, so mountainous areas are often easier to move through. You can quickly move one square by {clicking_pressing} and holding for a moment on the destination, then selecting 'Move To' from the pop-up menu.";
                lesson_text = lesson_text.Replace("{clicking_pressing}", Input.touchSupported ? LocalizationManager.GetTranslation("Tutorials/pressing") : LocalizationManager.GetTranslation("Tutorials/clicking"));
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/got_it");
                break;
            case Lessons.resources:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_resources"); // "This is one of many different kinds of resources found throughout the world. Resources are often worth capturing and defending, for the bonuses they add to our nation's stats. To find out what bonuses a resource gives, {click_tap} on it and select its name from the pop-up menu.";
                lesson_text = lesson_text.Replace("{click_tap}", Input.touchSupported ? LocalizationManager.GetTranslation("Tutorials/tap") : LocalizationManager.GetTranslation("Tutorials/click"));
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/okay");
                CheckMapContingencies();
                tutorialArrow.ActivateForBlock(resourceX, resourceZ);
                break;
            case Lessons.orbs:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_orbs"); // "This is one of the coveted Orbs of Power! They are usually well defended by the nation that occupies them, and difficult to capture, but worth it: the nation that occupies an Orb accrues free credits over time! There are several types of Orb, ranging from the relatively common Orb of Noontide to the unique and awe-inspiring Orb of Fire!";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/okay");
                CheckMapContingencies();
                tutorialArrow.ActivateForBlock(orbX, orbZ);
                break;
            case Lessons.next_level_boundary:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_next_level_boundary"); // "When {nation_name} advances to level {next_level}, the lands west of this green line will be too primitive for us to occupy. The time has come to advance further eastward!";
                lesson_text = lesson_text.Replace("{nation_name}", GameData.instance.nationName);
                lesson_text = lesson_text.Replace("{next_level}", "" + (GameData.instance.level + 1));
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/got_it");
                CheckMapContingencies();
                tutorialArrow.ActivateForBlock(greenLineX, greenLineZ);
                break;
            case Lessons.upgrade_defense:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_upgrade_defense"); // "Now that we've developed {upgrade_name}, we can upgrade your {pre_upgrade_name}! Just {click_tap} on the {pre_upgrade_name} and select 'Upgrade to {upgrade_name}' from the pop-up menu.";
                lesson_text = lesson_text.Replace("{pre_upgrade_name}", (BuildData.GetBuildData(upgradableObjectID) == null) ? LocalizationManager.GetTranslation("Tutorials/this") : BuildData.GetBuildData(upgradableObjectID).name); 
                lesson_text = lesson_text.Replace("{upgrade_name}", (BuildData.GetBuildData(upgradableUpgradeID) == null) ? LocalizationManager.GetTranslation("Tutorials/this") : BuildData.GetBuildData(upgradableUpgradeID).name); 
                lesson_text = lesson_text.Replace("{click_tap}", Input.touchSupported ? LocalizationManager.GetTranslation("Tutorials/tap") : LocalizationManager.GetTranslation("Tutorials/click"));
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/okay");
                CheckMapContingencies();
                tutorialArrow.ActivateForBlock(upgradableX, upgradableZ);
                break;
            case Lessons.level_up_1:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_level_up_1"); // "We've advanced to level {level}! We've gained an Advance Point, which can be used to increase our nation's abilities!";
                lesson_text = lesson_text.Replace("{level}", "" + GameData.instance.level);
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.level_up_2:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_level_up_2"); // "Go to the Advances Panel to see what advances are available!";
                lessonText.text = lesson_text;
                buttonArea.SetActive(false);
                break;
            case Lessons.level_up_3:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_level_up_3"); // "This tree shows all of the possible advances. The ones that are available to us right now are glowing. A green glow means you can research the advance using the Advance Point that we got by leveling up. A blue glow means a temporary power-up that we can buy with credits (<sprite=2>).";
                lesson_text = lesson_text.Replace("{Click_Tap}", Input.touchSupported ? LocalizationManager.GetTranslation("Tutorials/Tap") : LocalizationManager.GetTranslation("Tutorials/Click"));
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.level_up_4:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_level_up_4"); // "{Click_Tap} on an advance to find out what stat bonuses or special abilities it would grant us!";
                lesson_text = lesson_text.Replace("{Click_Tap}", Input.touchSupported ? LocalizationManager.GetTranslation("Tutorials/Tap") : LocalizationManager.GetTranslation("Tutorials/Click"));
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.level_up_5:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_level_up_5"); // "Once you've found the advance you want to gain, {click_tap} the Research button on the Advances Panel.";
                lesson_text = lesson_text.Replace("{click_tap}", Input.touchSupported ? LocalizationManager.GetTranslation("Tutorials/tap") : LocalizationManager.GetTranslation("Tutorials/click"));
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/got_it");
                break;
            case Lessons.map_panel_1:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_map_panel_1"); // "If you ever want to quickly go to a particular part of our nation (or you get lost and want to find your way back!) you can {click_tap} on the compass to show the Map Panel.";
                lesson_text = lesson_text.Replace("{click_tap}", Input.touchSupported ? LocalizationManager.GetTranslation("Tutorials/tap") : LocalizationManager.GetTranslation("Tutorials/click"));
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/okay");
                break;
            case Lessons.map_panel_2:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_map_panel_2"); // "The blue marker shows where you are now looking on the map, and the green markers show where different areas of our nation are located. You can {click_tap} on any marker to go to that part of the world. You can also {click_tap} on a marker's pencil, to create a map flag to remember that location.";
                lesson_text = lesson_text.Replace("{click_tap}", Input.touchSupported ? LocalizationManager.GetTranslation("Tutorials/tap") : LocalizationManager.GetTranslation("Tutorials/click"));
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/got_it");
                break;
            case Lessons.create_password:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_create_password"); // "To make sure you don't lose your progress, create a password for your account. A password will also let you log in to this account on another device!";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/got_it");
                break;
            case Lessons.recruit:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_recruit"); // "When you invite friends to join you in War of Conquest, make sure to give them your patron code! When they sign up using your code, you will become their patron, and you'll both get bonuses! For more about this, see the Recruit tab on the Connect Panel.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/got_it");
                break;
            case Lessons.surround_count:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_surround_count"); // "These numbers tell us how many of the 8 surrounding squares contain enemy defenses. This is useful for helping to figure out where hidden defenses are located. Some defenses can be defused by surrounding them before we attack!";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/got_it");
                CheckMapContingencies();
                tutorialArrow.ActivateForBlock(surroundCountX, surroundCountZ);
                break;
            case Lessons.energy_reserves_decreasing:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_energy_reserves_decreasing"); // "We're not generating energy as quickly as we're burning it to maintain our defenses. Our energy reserves are making up the difference for now, but when they run out some of our defenses will become inert and stop functioning. We need to salvage some defenses.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/got_it");
                break;
            case Lessons.energy_reserves_empty:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_energy_reserves_empty"); // "We're not generating energy as quickly as we're burning it to maintain our defenses, and our energy reserves have run out. Some of our defenses have become inert and stopped functioning. We need to salvage some defenses, or find ways to generate more energy.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/got_it");
                break;
            case Lessons.excess_resources:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_excess_resources"); // "We've captured more resources than we have the ability to exploit efficiently, and the excess is causing a drain on our manpower. See the Resources tab on the Nation Panel to decide whether we should evacuate any resources.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/got_it");
                break;
            case Lessons.home_island:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_home_island"); // "Besides fighting in the free-for-all of the main world, we can also advance our nation by raiding the home islands of other nations, and defending our own home island! {Click_Tap} the 'Home Island' button any time to get started.";
                lesson_text = lesson_text.Replace("{Click_Tap}", Input.touchSupported ? LocalizationManager.GetTranslation("Tutorials/Tap") : LocalizationManager.GetTranslation("Tutorials/Click"));
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.homeland_1:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_homeland_1"); // "This is our nation's home island. We can build defenses here and use this as a base to raid other nations' home islands.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.homeland_2:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_homeland_2"); // "Our first mission is to settle land here. You can rapidly occupy a 5x5 area of land by {pressing_clicking} and holding for a moment on empty land, then selecting 'Occupy 5x5' from the pop-up menu.";
                lesson_text = lesson_text.Replace("{pressing_clicking}", Input.touchSupported ? LocalizationManager.GetTranslation("Tutorials/pressing") : LocalizationManager.GetTranslation("Tutorials/clicking"));
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.homeland_3:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_homeland_3"); // "By settling a large patch of contiguous land that is broken up as little as possible by mountains or water, we'll be able to get our Geographic Efficiency close to 100%, so that we can support walls and defenses.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/okay");
                break;
            case Lessons.homeland_4:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_homeland_4"); // "After settling land, we should choose locations for the three Orb Shards that we've found on this island. The better we defend these shards from raids, the more credits and XP they will generate every day!";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.homeland_5:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_homeland_5"); // "To place an Orb Shard, {click_tap} on an empty square owned by our nation, and select 'Build...' from the pop-up menu.";
                lesson_text = lesson_text.Replace("{click_tap}", Input.touchSupported ? LocalizationManager.GetTranslation("Tutorials/tap") : LocalizationManager.GetTranslation("Tutorials/click"));
                lessonText.text = lesson_text;
                buttonArea.SetActive(false);
                break;
            case Lessons.homeland_6:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_homeland_6"); // "The build menu shows the three Orb Shards, as well as the different types of defenses you can build. Select the Red Shard now to place it in this location.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(false);
                break;
            case Lessons.homeland_7:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_homeland_7"); // "Excellent! Now place the green and blue shards as well. While the Red Shard will always be visible to raiding enemies, the green and blue shards will start out invisible, and only appear when the enemy attacks close to them. So you can choose to either surround them with defenses, or hide them somewhere that invading enemies might not look.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/got_it");
                break;
            case Lessons.homeland_8:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_homeland_8"); // "Note that you can change the location of an Orb Shard any time. To do so, {click_tap} on the shard and select 'Salvage' from the pop-up menu. Then {click_tap} on the new location and and select 'Build...' from the pop-up menu to place the shard there.";
                lesson_text = lesson_text.Replace("{click_tap}", Input.touchSupported ? LocalizationManager.GetTranslation("Tutorials/tap") : LocalizationManager.GetTranslation("Tutorials/click"));
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/okay");
                break;
            case Lessons.homeland_9:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_homeland_9"); // "Very good. Now that all three Orb Shards have been placed, build some walls and defenses to protect them. Each defense that we build uses up some of the energy that we generate on this island, so you can keep building walls and defenses until our excess energy reaches zero.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/okay");
                break;
            case Lessons.homeland_10:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_homeland_10"); // "Our defenses are progressing nicely! We're just about ready to venture out and raid another nation's home island. To do so, {click_tap} the 'Raid' button.";
                lesson_text = lesson_text.Replace("{click_tap}", Input.touchSupported ? LocalizationManager.GetTranslation("Tutorials/tap") : LocalizationManager.GetTranslation("Tutorials/click"));
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/okay");
                break;
            case Lessons.homeland_11:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_homeland_11"); // "You're now prepared to defend our home island and raid our enemies! To return to the main game world any time, {click_tap} the 'Main World' button.";
                lesson_text = lesson_text.Replace("{click_tap}", Input.touchSupported ? LocalizationManager.GetTranslation("Tutorials/tap") : LocalizationManager.GetTranslation("Tutorials/click"));
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/okay");
                break;
            case Lessons.raid_1:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_raid_1"); // "We've arrived at the home island of the nation {nation name}! Before a raid begins, we can get some information about the enemy nation to help us decide whether to attack. Beside the nation's name is their number of medals, which gives us an idea of how good they are at raiding and defending from raids.";
                lesson_text = lesson_text.Replace("{nation name}", GameData.instance.raidDefenderNationName);
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.raid_2:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_raid_2"); // "Below that are the number of medals, credits and XP that we stand to gain if we win a five star victory in this raid, and then how many medals we'll lose if we suffer a zero star defeat. If we manage a partial victory (one to four stars) then the medals, credits and XP we gain will be somewhere between these extremes.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.raid_3:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_raid_3"); // "This is how many medals our nation has right now. We gain medals with each victory raiding other nations' home islands, and by successfully defending our own home island. We lose medals when we're defeated in a raid, and when our home island's defenses are defeated by another nation's raid.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.raid_4:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_raid_4"); // "The more medals we have, the greater the rewards we'll gain, both for successful raids and for defending our own home island!";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.raid_5:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_raid_5"); // "If you're not sure whether you'd like to raid this island, {click_tap} the 'Next' button to see another option. Each option you skip uses up a little manpower though, giving us less to devote to the raid, so be careful not to overuse this.";
                lesson_text = lesson_text.Replace("{click_tap}", Input.touchSupported ? LocalizationManager.GetTranslation("Tutorials/tap") : LocalizationManager.GetTranslation("Tutorials/click"));
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/got_it");
                break;
            case Lessons.raid_6:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_raid_6"); // "Once you've found an island you'd like to invade, begin the raid by settling land, advancing forward from our beachhead.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/okay");
                CheckMapContingencies();
                tutorialArrow.ActivateForBlock(adjEmptySquareX, adjEmptySquareZ);
                break;
            case Lessons.raid_7:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_raid_7"); // "Our raid has begun! You can quickly advance toward the enemy nation by {pressing_clicking} and holding on an empty square and selecting 'Move To'. Once you've approached them from whatever direction you choose, begin attacking!";
                lesson_text = lesson_text.Replace("{pressing_clicking}", Input.touchSupported ? LocalizationManager.GetTranslation("Tutorials/pressing") : LocalizationManager.GetTranslation("Tutorials/clicking"));
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/okay");
                break;
            case Lessons.raid_8:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_raid_8"); // "Each attack we make uses some manpower, and once we run out of manpower the raid is finished.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.raid_9:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_raid_9"); // "A raid can also end by running out of time. Here you can see how much time we have left.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.raid_10:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_raid_10"); // "This also shows how many stars we've earned. One star is earned for capturing each of the enemy's Orb Shards. Another star is earned by capturing 50% of the enemy's land, and the final star is earned by defeating 100% of the enemy's forces.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.raid_11:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_raid_11"); // "There are many possible raid strategies to try out. You can build your own defenses, to protect your gains or aid in defeating enemy defenses. Before building defenses in a raid, we should capture enough contiguous land to give us decent geographic efficiency, otherwise our defenses will become inert and useless.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.raid_12:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_raid_12"); // "By the same token, it can be useful to carve holes in the enemy's land areas, so as to lower their geographic efficiency and cause some of their defenses to become inert.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.raid_13:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_raid_13"); // "If you ever want to end a raid early, {click_tap} the 'End Raid' button.";
                lesson_text = lesson_text.Replace("{click_tap}", Input.touchSupported ? LocalizationManager.GetTranslation("Tutorials/tap") : LocalizationManager.GetTranslation("Tutorials/click"));
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/okay");
                break;
            case Lessons.raid_14:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_raid_14"); // "After a raid, the manpower available on our home island is depleted. We'll need to wait for it to fill up again before we can start another raid.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/got_it");
                break;
            case Lessons.raid_log_1:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_raid_log_1"); // "An enemy nation has raided our home island! Let's open the Raid Log panel to see what happened.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(false);
                break;
            case Lessons.raid_log_2:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_raid_log_2"); // "Under the 'Defense' tab we'll find an entry for each time our home island has been raided by another nation. The entry shows how successful the raid was, as well as everything we gained or lost because of it. Under the 'Attack' tab you'll find an entry for each raid we've made.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.raid_log_3:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_raid_log_3"); // "You can {click_tap} 'Replay' to review any raid at high speed. This can be very helpful in figuring out how to improve our defenses against enemy attacks.";
                lesson_text = lesson_text.Replace("{click_tap}", Input.touchSupported ? LocalizationManager.GetTranslation("Tutorials/tap") : LocalizationManager.GetTranslation("Tutorials/click"));
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/next");
                break;
            case Lessons.raid_log_4:
                lesson_text = LocalizationManager.GetTranslation("Tutorial/tutorial_raid_log_4"); // "Finally, the 'League' tab will show you what league we are in, based on how many medals we have, as well as that league's rewards for a successful raid and for effective defense of our home island's Orb Shards. It also shows the rewards for the next league that we can advance to.";
                lessonText.text = lesson_text;
                buttonArea.SetActive(true);
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/okay");
                break;
        }

        // Position the tutorial arrow on the UI for the current lesson, if applicable.
        PositionArrowOnUI();

        // Show the tutorial panel
        tutorialPanel.gameObject.SetActive(true);

        // Transition in the tutorial panel
        tutorialPanelTransition.StartTransition(0f, 1f, .9f, 1f, false);

        // Record that this lesson has been presented, and the login game time.
        presentedLessons[curLesson] = GameData.instance.gameTimeAtLogin;

        // Record that the tutorial state has changed
        stateChanged = true;

        // Record the login time of this presented lesson.
        prevLessonPresentedLoginTime = GameData.instance.gameTimeAtLogin;

        // Play sound
        if (curLesson != prevLesson)
        {
            if (prevLesson == Lessons.none) {
                Sound.instance.Play2D(Sound.instance.tutorial_appears);
            } else {
                Sound.instance.Play2D(Sound.instance.tutorial_changes);
            }
        }
    }

    public void HideLesson()
    {
        // Record that no lesson is currently showing.
        curLesson = Lessons.none;

        // Transition out the tutorial panel
        tutorialPanelTransition.StartTransition(1f, 0f, 1f, .9f, true);

        // Hide the tutorial arrow
        tutorialArrow.Deactivate();
    }

    public void Close()
    {
        // Record that no lesson is currently showing.
        curLesson = Lessons.none;

        // Close the tutorial panel, without transitioning out.
        tutorialPanel.gameObject.SetActive(false);

        // Hide the tutorial arrow
        tutorialArrow.Deactivate();
    }

    public void PositionArrowOnUI()
    {
        StartCoroutine(PositionArrowOnUI_Coroutine());
    }

    public IEnumerator PositionArrowOnUI_Coroutine()
    {
        // Wait until end of frame, when UI elements will have been laid out for any changes made in this frame.
        yield return new WaitForEndOfFrame();

        switch (curLesson)
        {
            //case Lessons.nation_on_map: // TESTING
            //    tutorialArrow.ActivateForUI(nationStatsTab);
            //    break;
            case Lessons.attack_succeeds:
                tutorialArrow.ActivateForUI(xpBar);
                break;
            case Lessons.attack_8:
            case Lessons.excess_resources:
                tutorialArrow.ActivateForUI(manpowerBar);
                break;
            case Lessons.defense_5:
            case Lessons.energy_reserves_decreasing:
            case Lessons.energy_reserves_empty:
                tutorialArrow.ActivateForUI(energyBar);
                break;
            case Lessons.defense_8:
                if (GameGUI.instance.GetActiveGamePanel() == GameGUI.GamePanel.GAME_PANEL_NATION) 
                {
                    if (NationPanel.instance.statsContentArea.activeSelf) {
                        tutorialArrow.Deactivate();
                    } else {
                        tutorialArrow.ActivateForUI(nationStatsTab);
                    }
                }
                else 
                {
                    if (nationPanelButtonLeft.gameObject.activeInHierarchy) {
                        tutorialArrow.ActivateForUI(nationPanelButtonLeft);
                    } else {
                        tutorialArrow.ActivateForUI(nationPanelButtonBottom);
                    }
                }
                break;
            case Lessons.defense_16:
                if (GameGUI.instance.GetActiveGamePanel() == GameGUI.GamePanel.GAME_PANEL_NATION) {
                    tutorialArrow.ActivateForUI(nationPanelCloseButton);
                } else {
                    tutorialArrow.Deactivate();
                }
                break;
            case Lessons.defense_6:
            case Lessons.raid_11:
            case Lessons.geo_eff_1_1:
            case Lessons.geo_eff_2_1:
            case Lessons.geo_eff_3:
            case Lessons.geo_eff_4:
                tutorialArrow.ActivateForUI(geoBar);
                break;
            case Lessons.quests_and_credits:
                if (GameGUI.instance.GetActiveGamePanel() == GameGUI.GamePanel.GAME_PANEL_QUESTS) {
                    tutorialArrow.Deactivate();
                } else {
                    if (questsPanelButtonLeft.gameObject.activeInHierarchy) {
                        tutorialArrow.ActivateForUI(questsPanelButtonLeft);
                    } else {
                        tutorialArrow.ActivateForUI(questsPanelButtonBottom);
                    }
                }
                break;
            case Lessons.level_up_1:
                tutorialArrow.ActivateForUI(advancePointBar);
                break;
            case Lessons.level_up_2:
                if (GameGUI.instance.GetActiveGamePanel() == GameGUI.GamePanel.GAME_PANEL_ADVANCES) {
                    tutorialArrow.Deactivate();
                } else {
                    if (advancesPanelButtonLeft.gameObject.activeInHierarchy) {
                        tutorialArrow.ActivateForUI(advancesPanelButtonLeft);
                    } else {
                        tutorialArrow.ActivateForUI(advancesPanelButtonBottom);
                    }
                }
                break;
            case Lessons.level_up_5:
                if (GameGUI.instance.GetActiveGamePanel() != GameGUI.GamePanel.GAME_PANEL_ADVANCES) {
                    tutorialArrow.Deactivate();
                } else {
                    tutorialArrow.ActivateForUI(researchButton);
                }
                break;
            case Lessons.map_panel_1:
                tutorialArrow.ActivateForUI(compassRose);
                break;
            case Lessons.create_password:
                if (GameGUI.instance.GetActiveGamePanel() == GameGUI.GamePanel.GAME_PANEL_OPTIONS) 
                {
                    if (OptionsPanel.instance.createPasswordButton.activeSelf) {
                        tutorialArrow.ActivateForUI(createPasswordButton);
                    } else {
                        tutorialArrow.Deactivate();
                    }
                }
                else 
                {
                    if (optionsPanelButtonLeft.gameObject.activeInHierarchy) {
                        tutorialArrow.ActivateForUI(optionsPanelButtonLeft);
                    } else {
                        tutorialArrow.ActivateForUI(optionsPanelButtonBottom);
                    }
                }
                break;
            case Lessons.recruit:
                if (GameGUI.instance.GetActiveGamePanel() == GameGUI.GamePanel.GAME_PANEL_CONNECT) 
                {
                    if (ConnectPanel.instance.recruitContentArea.activeSelf) {
                        tutorialArrow.Deactivate();
                    } else {
                        tutorialArrow.ActivateForUI(connectRecruitTab);
                    }
                }
                else 
                {
                    if (connectPanelButtonLeft.gameObject.activeInHierarchy) {
                        tutorialArrow.ActivateForUI(connectPanelButtonLeft);
                    } else {
                        tutorialArrow.ActivateForUI(connectPanelButtonBottom);
                    }
                }
                break;
            case Lessons.home_island:
                tutorialArrow.ActivateForUI(switchMapButton);
                break;
            case Lessons.homeland_3:
                tutorialArrow.ActivateForUI(geoBar);
                break;
            case Lessons.homeland_9:
                tutorialArrow.ActivateForUI(energyBar);
                break;
            case Lessons.homeland_10:
                tutorialArrow.ActivateForUI(raidButton);
                break;
            case Lessons.homeland_11:
                tutorialArrow.ActivateForUI(switchMapButton);
                break;
            case Lessons.raid_1:
                tutorialArrow.ActivateForUI(raidIntroHeader);
                break;
            case Lessons.raid_2:
                tutorialArrow.ActivateForUI(raidIntroHeader);
                break;
            case Lessons.raid_3:
                tutorialArrow.ActivateForUI(medalsText);
                break;
            case Lessons.raid_4:
                tutorialArrow.ActivateForUI(medalsText);
                break;
            case Lessons.raid_5:
                tutorialArrow.ActivateForUI(raidButton);
                break;
            case Lessons.raid_8:
                tutorialArrow.ActivateForUI(manpowerBar);
                break;
            case Lessons.raid_9:
                tutorialArrow.ActivateForUI(raidScoreHeader);
                break;
            case Lessons.raid_10:
                tutorialArrow.ActivateForUI(raidScoreHeader);
                break;
            case Lessons.raid_13:
                tutorialArrow.ActivateForUI(raidButton);
                break;
            case Lessons.raid_14:
                tutorialArrow.ActivateForUI(manpowerBar);
                break;
            case Lessons.raid_log_1:
                if (GameGUI.instance.GetActiveGamePanel() == GameGUI.GamePanel.GAME_PANEL_RAID) {
                    tutorialArrow.Deactivate();
                } else {
                    if (raidLogPanelButtonLeft.gameObject.activeInHierarchy) {
                        tutorialArrow.ActivateForUI(raidLogPanelButtonLeft);
                    } else {
                        tutorialArrow.ActivateForUI(raidLogPanelButtonBottom);
                    }
                }
                break;
            case Lessons.raid_log_4:
                if (GameGUI.instance.GetActiveGamePanel() == GameGUI.GamePanel.GAME_PANEL_RAID) 
                {
                    if (RaidPanel.instance.leagueContentArea.activeSelf) {
                        tutorialArrow.Deactivate();
                    } else {
                        tutorialArrow.ActivateForUI(raidLeagueTab);
                    }
                }
                else 
                {
                    if (raidLogPanelButtonLeft.gameObject.activeInHierarchy) {
                        tutorialArrow.ActivateForUI(raidLogPanelButtonLeft);
                    } else {
                        tutorialArrow.ActivateForUI(raidLogPanelButtonBottom);
                    }
                }
                break;
        }
    }

    public void OnClick_NextButton()
    {
        switch (curLesson)
        {
            case Lessons.nation_on_map:
                ShowLesson(Lessons.moving_map);
                break;
            case Lessons.moving_map:
                ShowLesson(Lessons.goal_orbs);
                break;
            case Lessons.past_boundary:
                if (presentedLessons.ContainsKey(Lessons.moving_map) && (presentedLessons.ContainsKey(Lessons.goal_orbs) == false)) {
                    ShowLesson(Lessons.goal_orbs);
                } else {
                    HideLesson();
                }
                break;
            case Lessons.goal_orbs:
                ShowLesson(Lessons.goal_go_east);
                break;
            case Lessons.goal_go_east:
                ShowLesson(Lessons.importance_of_attacking);
                break;
            case Lessons.importance_of_attacking:
                ShowLessonIfAppropriate();
                break;
            case Lessons.attack_succeeds:
                ShowLesson(Lessons.attack_2);
                break;
            case Lessons.attack_2:
                ShowLesson(Lessons.attack_3);
                break;
            case Lessons.attack_3:
                ShowLesson(Lessons.attack_4);
                break;
            case Lessons.attack_4:
                ShowLesson(Lessons.attack_5);
                break;
            case Lessons.attack_5:
                ShowLesson(Lessons.attack_6);
                break;
            case Lessons.attack_6:
                ShowLesson(Lessons.attack_7);
                break;
            case Lessons.attack_7:
                ShowLesson(Lessons.attack_8);
                break;
            case Lessons.attack_8:
                ShowLesson(Lessons.attack_9);
                break;
            case Lessons.defense_1:
                HideLesson();
                ShowLessonIfAppropriate();
                break;
            case Lessons.defense_4:
                ShowLesson(Lessons.defense_5);
                break;
            case Lessons.defense_5:
                ShowLesson(Lessons.defense_6);
                break;
            case Lessons.defense_6:
                ShowLesson(Lessons.defense_7);
                break;
            case Lessons.defense_7:
                ShowLesson(Lessons.defense_8);
                break;
            case Lessons.defense_9:
                ShowLesson(Lessons.defense_10);
                break;
            case Lessons.defense_10:
                ShowLesson(Lessons.defense_11);
                break;
            case Lessons.defense_11:
                ShowLesson(Lessons.defense_12);
                break;
            case Lessons.defense_12:
                ShowLesson(Lessons.defense_13);
                break;
            case Lessons.defense_13:
                ShowLesson(Lessons.defense_14);
                break;
            case Lessons.defense_14:
                ShowLesson(Lessons.defense_15);
                break;
            case Lessons.defense_15:
                if (GameGUI.instance.GetActiveGamePanel() != GameGUI.GamePanel.GAME_PANEL_NONE) {
                    ShowLesson(Lessons.defense_16);
                } else {
                    ShowLesson(Lessons.defense_17);
                }
                break;
               case Lessons.defense_17:
                ShowLesson(Lessons.defense_18);
                break;
            case Lessons.defense_18:
                ShowLesson(Lessons.quests_and_credits);
                break;
            case Lessons.quests_and_credits:
                ShowLesson(Lessons.home_island);
                break;
            case Lessons.summary_1:
                ShowLesson(Lessons.summary_2);
                break;
            case Lessons.level_up_1:
                ShowLesson(Lessons.level_up_2);
                break;
            case Lessons.level_up_3:
                ShowLesson(Lessons.level_up_4);
                break;
            case Lessons.level_up_4:
                if (GameData.instance.advance_points > 0) {
                    ShowLesson(Lessons.level_up_5);
                } else {
                    HideLesson();
                }
                break;
            case Lessons.geo_eff_1_1:
                ShowLesson(Lessons.geo_eff_1_2);
                break;
            case Lessons.geo_eff_2_1:
                if (GameData.instance.current_footprint.area > GameData.instance.GetSupportableArea()) {
                    ShowLesson(Lessons.geo_eff_2_3);
                } else {
                    ShowLesson(Lessons.geo_eff_2_2);
                }
                break;
            case Lessons.home_island:
                ShowLesson(Lessons.summary_1);
                break;
            case Lessons.homeland_1:
                ShowLesson(Lessons.homeland_2);
                break;
            case Lessons.homeland_2:
                ShowLesson(Lessons.homeland_3);
                break;
            case Lessons.homeland_4:
                ShowLesson(Lessons.homeland_5);
                break;
            case Lessons.raid_1:
                ShowLesson(Lessons.raid_2);
                break;
            case Lessons.raid_2:
                ShowLesson(Lessons.raid_3);
                break;
            case Lessons.raid_3:
                ShowLesson(Lessons.raid_4);
                break;
            case Lessons.raid_4:
                ShowLesson(Lessons.raid_5);
                break;
            case Lessons.raid_5:
                ShowLesson(Lessons.raid_6);
                break;
            case Lessons.raid_8:
                ShowLesson(Lessons.raid_9);
                break;
            case Lessons.raid_9:
                ShowLesson(Lessons.raid_10);
                break;
            case Lessons.raid_10:
                ShowLesson(Lessons.raid_11);
                break;
            case Lessons.raid_11:
                ShowLesson(Lessons.raid_12);
                break;
            case Lessons.raid_12:
                ShowLesson(Lessons.raid_13);
                break;
            case Lessons.raid_log_2:
                ShowLesson(Lessons.raid_log_3);
                break;
            case Lessons.raid_log_3:
                ShowLesson(Lessons.raid_log_4);
                break;
            case Lessons.summary_2:
            case Lessons.level_up_5:
            case Lessons.geo_eff_1_2:
            case Lessons.geo_eff_2_2:
            case Lessons.geo_eff_2_3:
            case Lessons.geo_eff_3:
            case Lessons.geo_eff_4:
            case Lessons.occupy_area:
            case Lessons.evacuate_area:
            case Lessons.occupy_empty:
            case Lessons.mountain_paths:
            case Lessons.resources:
            case Lessons.orbs:
            case Lessons.next_level_boundary:
            case Lessons.upgrade_defense:
            case Lessons.map_panel_1:
            case Lessons.map_panel_2:
            case Lessons.create_password:
            case Lessons.recruit:
            case Lessons.surround_count:
            case Lessons.energy_reserves_decreasing:
            case Lessons.energy_reserves_empty:
            case Lessons.excess_resources:
            case Lessons.homeland_3:
            case Lessons.homeland_7:
            case Lessons.homeland_8:
            case Lessons.homeland_9:
            case Lessons.homeland_10:
            case Lessons.homeland_11:
            case Lessons.raid_6:
            case Lessons.raid_7:
            case Lessons.raid_13:
            case Lessons.raid_14:
            case Lessons.raid_log_4:
                HideLesson();
                break;
        }
    }

    public void ShowLessonIfAppropriate()
    {
        if (muted) {
            return;
        }

        if (GameData.instance.mapMode == GameData.MapMode.MAINLAND)
        {
            if (curLesson == Lessons.none)
            {
                if (presentedLessons.ContainsKey(Lessons.importance_of_attacking) == false)
                {
                    // Show the first lesson
                    //ShowLesson(Lessons.map_panel_1); // TESTING
                    ShowLesson(Lessons.nation_on_map);
                    return;
                }
            }
        
            // If we're within the attack series of lessons...
            if (presentedLessons.ContainsKey(Lessons.importance_of_attacking) && (presentedLessons.ContainsKey(Lessons.attack_9) == false))
            {
                // Check for map conditions that would cause a lesson to be displayed.
                CheckMapContingencies();

                if (adjEnemySquareX == -1)
                {
                    // Display the first lesson about moving toward a nation to attack.
                    if (presentedLessons.ContainsKey(Lessons.occupy_empty_1) == false) {
                        ShowLesson(Lessons.occupy_empty_1);
                    }
                }
                else if ((curLesson == Lessons.occupy_empty_2) || (curLesson == Lessons.importance_of_attacking) || (curLesson == Lessons.none))
                {
                    // Display the next lesson in the attack series.
                    if (presentedLessons.ContainsKey(Lessons.attack_succeeds))
                    {
                        if (presentedLessons.ContainsKey(Lessons.attack_8)) {
                            ShowLesson(Lessons.attack_9);
                        } if (presentedLessons.ContainsKey(Lessons.attack_7)) {
                            ShowLesson(Lessons.attack_8);
                        } else if (presentedLessons.ContainsKey(Lessons.attack_6)) {
                            ShowLesson(Lessons.attack_7);
                        } else if (presentedLessons.ContainsKey(Lessons.attack_5)) {
                            ShowLesson(Lessons.attack_6);
                        } else if (presentedLessons.ContainsKey(Lessons.attack_4)) {
                            ShowLesson(Lessons.attack_5);
                        } else if (presentedLessons.ContainsKey(Lessons.attack_3)) {
                            ShowLesson(Lessons.attack_4);
                        } else if (presentedLessons.ContainsKey(Lessons.attack_2)) {
                            ShowLesson(Lessons.attack_3);
                        } else if (presentedLessons.ContainsKey(Lessons.attack_succeeds)) {
                            ShowLesson(Lessons.attack_2);
                        }
                    }
                    else if (presentedLessons.ContainsKey(Lessons.attack_1) == false)
                    {
                        ShowLesson(Lessons.attack_1);
                    }
                }

                return;
            }

            if (curLesson == Lessons.none)
            {
                if (presentedLessons.ContainsKey(Lessons.defense_1) == false)
                {
                    // Show the first defense lesson
                    ShowLesson(Lessons.defense_1);
                    return;
                }
            }

            // If we've started the defense series of lessons, proceed when there is an empty square to build a defense in.
            if (presentedLessons.ContainsKey(Lessons.defense_1) && (presentedLessons.ContainsKey(Lessons.defense_2) == false))
            {
                // Check for map conditions that would cause a lesson to be displayed.
                CheckMapContingencies();

                // If there is an empty square in view that belongs to this nation...
                if (emptyNationSquareX != -1)
                {
                    ShowLesson(Lessons.defense_2);
                    return;
                }
            }

            if (curLesson == Lessons.none)
            {
                if (presentedLessons.ContainsKey(Lessons.defense_2))
                {
                    if (presentedLessons.ContainsKey(Lessons.defense_4) == false)
                    {
                        // Show the second defense lesson
                        ShowLesson(Lessons.defense_2);
                        return;
                    }
                    else if (presentedLessons.ContainsKey(Lessons.quests_and_credits) == false)
                    {
                        ShowLesson(Lessons.quests_and_credits);
                        return;
                    }
                    else if (presentedLessons.ContainsKey(Lessons.summary_2) == false)
                    {
                        ShowLesson(Lessons.summary_1);
                        return;
                    }
                }

                if ((presentedLessons.ContainsKey(Lessons.map_panel_1)  == false) && (presentedLessons.ContainsKey(Lessons.map_panel_2)  == false))
                {
                    // Check for map conditions that would cause a lesson to be displayed.
                    CheckMapContingencies();

                    if (playerNationInView == false) 
                    {
                        ShowLesson(Lessons.map_panel_1);
                        return;
                    }
                }

                if (GameData.instance.GetFinalEnergyBurnRate() > GameData.instance.GetFinalEnergyRate())
                {
                    if ((GameData.instance.energy > 0) && (presentedLessons.ContainsKey(Lessons.energy_reserves_decreasing) == false))
                    {
                        ShowLesson(Lessons.energy_reserves_decreasing);
                        return;
                    }

                    if ((GameData.instance.energy == 0) && (presentedLessons.ContainsKey(Lessons.energy_reserves_empty) == false))
                    {
                        ShowLesson(Lessons.energy_reserves_empty);
                        return;
                    }
                }

                if ((GameData.instance.manpowerBurnRate > 10) && (presentedLessons.ContainsKey(Lessons.excess_resources) == false))
                {
                    ShowLesson(Lessons.excess_resources);
                    return;
                }

                // If no tutorial has yet been shown during the present login...
                if (prevLessonPresentedLoginTime != GameData.instance.gameTimeAtLogin)
                {
                    if (GameData.instance.BenefitsFromGeoEfficiency())
                    {
                        if ((GameData.instance.GetFinalGeoEfficiency() <= ((GameData.instance.geographicEfficiencyMin + GameData.instance.geographicEfficiencyMax) / 2.0)))
                        {
                            if (GameData.instance.current_footprint.area > GameData.instance.GetSupportableArea()) {
                                ShowLesson(Lessons.geo_eff_4);
                            } else {
                                ShowLesson(Lessons.geo_eff_1_1);
                            }
                            return;
                        }

                        if ((GameData.instance.GetFinalEnergyBurnRate() > 0) && (GameData.instance.GetFinalGeoEfficiency() < 1.0f))
                        {
                            //Debug.Log("energy burn: " + GameData.instance.GetFinalEnergyBurnRate() + ", raid energy burn: " + GameData.instance.raidland_footprint.energy_burn_rate);
                            ShowLesson(Lessons.geo_eff_2_1);
                            return;
                        }
                    }

                    if ((presentedLessons.ContainsKey(Lessons.occupy_empty_1) == false) && (presentedLessons.ContainsKey(Lessons.occupy_empty) == false))
                    {
                        // Check for map conditions that would cause a lesson to be displayed.
                        CheckMapContingencies();

                        if (adjEmptySquareX != -1)
                        {
                            ShowLesson(Lessons.occupy_empty);
                            return;
                        }
                    }

                    if ((presentedLessons.ContainsKey(Lessons.resources) == false) || (presentedLessons.ContainsKey(Lessons.orbs) == false) ||
                        (presentedLessons.ContainsKey(Lessons.next_level_boundary) == false) ||
                        (presentedLessons.ContainsKey(Lessons.upgrade_defense) == false) ||
                        (presentedLessons.ContainsKey(Lessons.surround_count) == false))
                    {
                        // Check for map conditions that would cause a lesson to be displayed.
                        CheckMapContingencies();

                        // Show resources lesson if appropriate
                        if (presentedLessons.ContainsKey(Lessons.resources) == false)
                        {
                            if (resourceX != -1) 
                            {
                                ShowLesson(Lessons.resources);
                                return;
                            }
                        }

                        // Show orbs lesson if appropriate
                        if (presentedLessons.ContainsKey(Lessons.orbs) == false)
                        {
                            if (orbX != -1) 
                            {
                                ShowLesson(Lessons.orbs);
                                return;
                            }
                        }
                    
                        // Show next level boundary lesson if appropriate
                        if (presentedLessons.ContainsKey(Lessons.next_level_boundary) == false)
                        {
                            if (nationWestOfGreenLine && (greenLineX != -1)) 
                            {
                                ShowLesson(Lessons.next_level_boundary);
                                return;
                            }
                        }

                        // Show surround count lesson if appropriate
                        if (presentedLessons.ContainsKey(Lessons.surround_count) == false)
                        {
                            if ((surroundCountX != -1) && (MapView.instance.GetCameraZoomDistance() <= MapView.SURROUND_COUNT_DISAPPEAR_START_ZOOM_DIST))
                            {
                                ShowLesson(Lessons.surround_count);
                                return;
                            }
                        }
                    }
                }

                if (GameData.instance.BenefitsFromGeoEfficiency() && (GameData.instance.current_footprint.area > 10) && (GameData.instance.GetFinalGeoEfficiency() <= 0.5f) && (presentedLessons.ContainsKey(Lessons.geo_eff_3) == false))
                {
                    ShowLesson(Lessons.geo_eff_3);
                    return;
                }
            }
        }

        if (GameData.instance.mapMode == GameData.MapMode.HOMELAND)
        {
            if (presentedLessons.ContainsKey(Lessons.homeland_1) == false)
            {
                // Show the first homeland lesson
                ShowLesson(Lessons.homeland_1);
                return;
            }

            if (presentedLessons.ContainsKey(Lessons.raid_13) && (presentedLessons.ContainsKey(Lessons.raid_14) == false) && (GameData.instance.homeland_footprint.manpower < GameData.instance.GetFinalManpowerMax()))
            {
                ShowLesson(Lessons.raid_14);
                return;
            }

            if (presentedLessons.ContainsKey(Lessons.raid_13) && (presentedLessons.ContainsKey(Lessons.raid_log_4) == false) && (curLesson == Lessons.none) && (GameData.instance.raidDefenseLog.Count > 0))
            {
                ShowLesson(Lessons.raid_log_1);
                return;
            }

            if ((GameGUI.instance.GetActiveGamePanel() == GameGUI.GamePanel.GAME_PANEL_NONE) && presentedLessons.ContainsKey(Lessons.raid_log_4) && (presentedLessons.ContainsKey(Lessons.homeland_11) == false))
            {
                ShowLesson(Lessons.homeland_11);
                return;
            }

            // If at least 50% of generated energy is being burned by homeland defenses...
            //Debug.Log("Here1, GameData.instance.GetFinalEnergyBurnRate(): " + GameData.instance.GetFinalEnergyBurnRate() + ", (GameData.instance.GetFinalEnergyRate() * 0.5f): " + (GameData.instance.GetFinalEnergyRate() * 0.5f));
            if (((curLesson == Lessons.none) || (curLesson == Lessons.homeland_9)) && ((presentedLessons.ContainsKey(Lessons.raid_1) == false) || ((presentedLessons.ContainsKey(Lessons.raid_13) == false) && (prevLessonPresentedLoginTime != GameData.instance.gameTimeAtLogin))) && (GameData.instance.GetFinalEnergyBurnRate() >= (GameData.instance.GetFinalEnergyRate() * 0.5f)))
            {
                ShowLesson(Lessons.homeland_10);
            }

            if ((curLesson == Lessons.none) || (curLesson < Lessons.homeland_1))
            {
                if (presentedLessons.ContainsKey(Lessons.homeland_3) == false)
                {
                    // Show the first homeland lesson
                    ShowLesson(Lessons.homeland_1);
                    return;
                }

                if (presentedLessons.ContainsKey(Lessons.homeland_4) == false)
                {
                    ShowLesson(Lessons.homeland_4);
                    return;
                }

                if (presentedLessons.ContainsKey(Lessons.homeland_9) == false)
                {
                    // Determine how many of the orb shards have previously been built.
                    int num_shards = GameData.instance.DetermineNumShardsPlaced();

                    if (num_shards == 0)
                    {
                        ShowLesson(Lessons.homeland_5);
                        return;
                    }

                    if (num_shards == 1)
                    {
                        ShowLesson(Lessons.homeland_7);
                        return;
                    }

                    if (num_shards == 2)
                    {
                        ShowLesson(Lessons.homeland_8);
                        return;
                    }

                    if (num_shards == 3)
                    {
                        ShowLesson(Lessons.homeland_9);
                        return;
                    }
                }
            }
        }

        if (GameData.instance.mapMode == GameData.MapMode.RAID)
        {
            if (((presentedLessons.ContainsKey(Lessons.raid_5) == false) && (curLesson != Lessons.raid_1)) || ((presentedLessons.ContainsKey(Lessons.raid_6) == false) && (curLesson == Lessons.none)))
            {
                // Show the first raid lesson
                ShowLesson(Lessons.raid_1);
                return;
            }
        }

        // Regardless of map mode...
        if (curLesson == Lessons.none)
        {
            // If no tutorial has yet been shown during the present login...
            if (prevLessonPresentedLoginTime != GameData.instance.gameTimeAtLogin)
            {
                // Check for map conditions that would cause a lesson to be displayed.
                CheckMapContingencies();

                if ((GameData.instance.userIsRegistered == false) && 
                        ((presentedLessons.ContainsKey(Lessons.create_password) == false) && ((GameData.instance.gameTimeAtLogin - GameData.instance.userCreationTime) > (SECONDS_PER_DAY * 7)) || 
                        (presentedLessons.ContainsKey(Lessons.create_password) && ((GameData.instance.gameTimeAtLogin - presentedLessons[Lessons.create_password]) > (SECONDS_PER_DAY * 30)))))
                {
                    ShowLesson(Lessons.create_password);
                    return;
                }

                if ((presentedLessons.ContainsKey(Lessons.recruit) == false) && ((GameData.instance.gameTimeAtLogin - GameData.instance.userCreationTime) > (SECONDS_PER_DAY * 5)))
                {
                    ShowLesson(Lessons.recruit);
                    return;
                }

                // Show upgrade defense lesson if appropriate
                if (presentedLessons.ContainsKey(Lessons.upgrade_defense) == false)
                {
                    if (upgradableX != -1) 
                    {
                        ShowLesson(Lessons.upgrade_defense);
                        return;
                    }
                }
            }
        }
    }

    public void CheckMapContingencies()
    {
        int x, z, start_x, end_x, x1, z1;
        BlockData block_data, adj_block_data;

        // Reset the result values
        playerNationInView = nationWestOfGreenLine = false;
        adjEnemySquareX = adjEnemySquareZ = adjEnemyID = -1;
        adjEmptySquareX = adjEmptySquareZ = -1;
        adjEmptyCentralSquareX = adjEmptyCentralSquareZ = -1;
        emptyNationSquareX = emptyNationSquareZ = -1;
        inertDefenseSquareX = inertDefenseSquareZ = -1;
        resourceX = resourceZ = -1;
        orbX = orbZ = -1;
        greenLineX = greenLineZ = -1;
        upgradableX = upgradableZ = upgradableObjectID = upgradableUpgradeID = -1;
        surroundCountX = surroundCountZ = -1;

        // Determine how far from the center of the view is allowed, and still considered near the center.
        float nearCenterDist = Mathf.Min(MapView.instance.view_bottom_block.z - MapView.instance.view_top_block.z, MapView.instance.view_right_block.x - MapView.instance.view_left_block.x) / 6f;
        
        // Iterate through each z row within the view area...
        for (z = MapView.instance.view_top_block.z; z <= MapView.instance.view_bottom_block.z; z++) 
        {
            // Determine the leftmost block x of this z row in the view area. 
            if (z <= MapView.instance.view_left_block.z) {
                start_x = MapView.instance.InterpXGivenZ(MapView.instance.view_top_block, MapView.instance.view_left_block, z);
            } else {
                start_x = MapView.instance.InterpXGivenZ(MapView.instance.view_left_block, MapView.instance.view_bottom_block, z);
            }

            // Determine the rightmost block x of this z row in the view area.
            if (z <= MapView.instance.view_right_block.z) {
                end_x = MapView.instance.InterpXGivenZ(MapView.instance.view_top_block, MapView.instance.view_right_block, z);
            } else {
                end_x = MapView.instance.InterpXGivenZ(MapView.instance.view_right_block, MapView.instance.view_bottom_block, z);
            }

            // Iterate through each block in the current row of the view area...
            for (x = start_x; x <= end_x; x++) 
            {
                block_data = MapView.instance.GetBlockData(x, z);

                if (block_data != null)
                {
                    if (block_data.nationID == GameData.instance.nationID)
                    {
                        // Record that the player's nation is in the view.
                        playerNationInView = true;

                        // Iterate thorugh all squares adjacent to this square belonging to the player's nation...
                        for (z1 = z - 1; z1 <= z + 1; z1++)
                        {
                            for (x1 = x - 1; x1 <= x + 1; x1++)
                            {
                                // Skip the center square itself.
                                if ((z1 == z) && (x1 == x)) {
                                    continue;
                                }

                                // Get the adjacent block's data.
                                adj_block_data = MapView.instance.GetBlockData(x1, z1);

                                // Skip if this adjacent block doesn't exist.
                                if (adj_block_data == null) {
                                    continue;
                                }

                                if (adj_block_data.nationID == -1)
                                {
                                    // If the current empty adjacent square is closer to the center of the view than the prev one (or if there is no prev one), record it.
                                    if (IsCloserToCenterOfView(x1, z1, adjEmptySquareX, adjEmptySquareZ))
                                    {
                                        adjEmptySquareX = x1;
                                        adjEmptySquareZ = z1;
                                    }

                                    // If the current empty adjacent square is closer to the center of the map than the prev one (or if there is no prev one), record it.
                                    if (IsCloserToCenterOfMap(x1, z1, adjEmptyCentralSquareX, adjEmptyCentralSquareZ))
                                    {
                                        adjEmptyCentralSquareX = x1;
                                        adjEmptyCentralSquareZ = z1;
                                    }
                                }
                                else if (adj_block_data.nationID != GameData.instance.nationID)
                                {
                                    // If the current enemy adjacent square is closer to the center of the view than the prev one (or if there is no prev one), record it.
                                    if (IsCloserToCenterOfView(x1, z1, adjEnemySquareX, adjEnemySquareZ))
                                    {
                                        adjEnemySquareX = x1;
                                        adjEnemySquareZ = z1;
                                        adjEnemyID = adj_block_data.nationID;
                                    }
                                }
                            }
                        }

                        if (block_data.objectID == -1)
                        {
                            // If the current empty square belonging to the player's nation is closer to the center of the view than the prev one (or if there is no prev one), record it.
                            if (IsCloserToCenterOfView(x, z, emptyNationSquareX, emptyNationSquareZ))
                            {
                                emptyNationSquareX = x;
                                emptyNationSquareZ = z;
                            }
                        }
                        else if ((block_data.objectID < ObjectData.RESOURCE_OBJECT_BASE_ID) && (block_data.owner_nationID == GameData.instance.nationID) && (MapView.instance.BlockIsToBeInert(x, z, block_data))) 
                        {
                            // If the square containing this nation's inert defense is closer to the center of the view than the prev one (or if there is no prev one), record it.
                            if (IsCloserToCenterOfView(x, z, inertDefenseSquareX, inertDefenseSquareZ))
                            {
                                inertDefenseSquareX = x;
                                inertDefenseSquareZ = z;
                            }
                        }

                        if (block_data.build_object != null)
                        {
                            if ((block_data.objectID != -1) && ((block_data.completion_time == -1) || (block_data.completion_time <= Time.time))) 
                            {
                                int availableUpgrade = GameData.instance.GetAvailableUpgrade(block_data.objectID);

                                if (availableUpgrade != -1)
                                {
                                    // If the current square with an available upgrade is closer to the center of the view than the prev one (or if there is no prev one), record it.
                                    if (IsCloserToCenterOfView(x, z, upgradableX, upgradableZ))
                                    {
                                        upgradableX = x;
                                        upgradableZ = z;
                                        upgradableObjectID = block_data.objectID;
                                        upgradableUpgradeID = availableUpgrade;
                                    }
                                }
                            }
                        }

                        if (x < GameData.instance.map_position_limit_next_level)
                        {
                            nationWestOfGreenLine = true;
                        }
                    }
                    else
                    {
                        if (block_data.objectID >= ObjectData.ORB_BASE_ID)
                        {
                            // If the current square containing an orb is closer to the center of the view than the prev one (or if there is no prev one), record it.
                            if (IsCloserToCenterOfView(x, z, orbX, orbZ) && (DistFromCenter(x, z) <= nearCenterDist))
                            {
                                orbX = x;
                                orbZ = z;
                            }
                        }
                        else if (block_data.objectID >= ObjectData.RESOURCE_OBJECT_BASE_ID)
                        {
                            // If the current square containing a resource object is closer to the center of the view than the prev one (or if there is no prev one), record it.
                            if (IsCloserToCenterOfView(x, z, resourceX, resourceZ) && (DistFromCenter(x, z) <= nearCenterDist))
                            {
                                resourceX = x;
                                resourceZ = z;
                            }
                        }
                    }

                    if (x == GameData.instance.map_position_limit_next_level)
                    {
                        // If the current square on the nation's next level boundary line is closer to the center of the view than the prev one (or if there is no prev one), record it.
                        if (IsCloserToCenterOfView(x, z, greenLineX, greenLineZ) && (DistFromCenter(x, z) <= nearCenterDist))
                        {
                            greenLineX = x;
                            greenLineZ = z;
                        }
                    }

                    if (block_data.surround_count != null)
                    {
                        // If the current square containing a surround count is closer to the center of the view than the prev one (or if there is no prev one), record it.
                        if (IsCloserToCenterOfView(x, z, surroundCountX, surroundCountZ) && (DistFromCenter(x, z) <= nearCenterDist))
                        {
                            surroundCountX = x;
                            surroundCountZ = z;
                        }
                    }
                }
            }
        }
    }

    public float DistFromCenter(int _blockX, int _blockZ)
    {
        int xDist = _blockX - MapView.instance.GetViewBlockX();
        int zDist = _blockZ - MapView.instance.GetViewBlockZ();

        return Mathf.Sqrt(((xDist * xDist) + (zDist * zDist)));
    }

    public bool IsCloserToCenterOfView(int _newX, int _newZ, int _prevX, int _prevZ)
    {
        int xDist, zDist;

        // If there is no prev position, return true.
        if (_prevX == -1) {
            return true;
        }

        // Determine new position's distance from center of view
        xDist = _newX - MapView.instance.GetViewBlockX();
        zDist = _newZ - MapView.instance.GetViewBlockZ();
        int new_dist_sqr = (xDist * xDist) + (zDist * zDist);

        // Determine prev position's distance from center of view
        xDist = _prevX - MapView.instance.GetViewBlockX();
        zDist = _prevZ - MapView.instance.GetViewBlockZ();
        int prev_dist_sqr = (xDist * xDist) + (zDist * zDist);

        return (new_dist_sqr < prev_dist_sqr);
    }

    public bool IsCloserToCenterOfMap(int _newX, int _newZ, int _prevX, int _prevZ)
    {
        int xDist, zDist;

        // If there is no prev position, return true.
        if (_prevX == -1) {
            return true;
        }

        // Determine new position's distance from center of map
        xDist = _newX - (MapView.instance.mapDimX / 2);
        zDist = _newZ - (MapView.instance.mapDimZ / 2);
        int new_dist_sqr = (xDist * xDist) + (zDist * zDist);

        // Determine prev position's distance from center of map
        xDist = _prevX - (MapView.instance.mapDimX / 2);
        zDist = _prevZ - (MapView.instance.mapDimZ / 2);
        int prev_dist_sqr = (xDist * xDist) + (zDist * zDist);

        return (new_dist_sqr < prev_dist_sqr);
    }

    public void ResolutionChanged()
    {
        tutorialPanel.anchoredPosition = new Vector2(0, GameGUI.instance.GetMainUIBottomHeight());

        // Reposition the arrow on the UI, if appropriate.
        PositionArrowOnUI();
    }

    public void BattleBegins(float _battle_duration, bool _success, int _enemyNationID)
    {
        if (muted) {
            return;
        }

        if ((GameData.instance.mapMode != GameData.MapMode.MAINLAND) && (GameData.instance.mapMode != GameData.MapMode.RAID)) {
            return;
        }

        if ((presentedLessons.ContainsKey(Lessons.attack_succeeds) == false) || presentedLessons.ContainsKey(Lessons.attack_1))
        {
            // Display lesson once battle is complete, if appropriate.
            if ((presentedLessons.ContainsKey(Lessons.attack_9) == false) ||
                (presentedLessons.ContainsKey(Lessons.attack_fails) == false) || 
                (presentedLessons.ContainsKey(Lessons.attack_succeeds) == false) || 
                ((presentedLessons.ContainsKey(Lessons.defense_1) == false) && (presentedLessons.ContainsKey(Lessons.attack_9))) ||
                (presentedLessons.ContainsKey(Lessons.raid_13) == false))
            {
                StartCoroutine(ProcessBattleEnd(_battle_duration, _success, _enemyNationID));
            }
        }
    }

    public IEnumerator ProcessBattleEnd(float _battle_duration, bool _success, int _enemyNationID)
    {
        // Wait until the battle has finished, and a little longer
        yield return new WaitForSeconds(_battle_duration + 0.5f);

        if (GameData.instance.mapMode == GameData.MapMode.MAINLAND) 
        {
            // Record the ID of the enemy nation whose square we've taken.
            battleEnemyID = _enemyNationID;

            if (_success)
            {
                // Show attack_succeeds lesson if appropriate.
                if ((presentedLessons.ContainsKey(Lessons.attack_succeeds) == false) && 
                    ((curLesson == Lessons.none) || (curLesson == Lessons.attack_1) || (curLesson == Lessons.attack_fails) || (curLesson == Lessons.occupy_empty_1) || (curLesson == Lessons.occupy_empty_2))) 
                {
                    ShowLesson(Lessons.attack_succeeds);
                    yield break;
                }

                // Show defense_1 lesson if appropriate.
                else if ((presentedLessons.ContainsKey(Lessons.defense_1) == false) && (presentedLessons.ContainsKey(Lessons.attack_9)))
                {
                    ShowLesson(Lessons.defense_1);
                    yield break;
                }
            }
            else
            {
                // Show attack_fails lesson if appropriate. Either show it between attack_1 and atack_2, or after attack_9; don't interrupt attack series with it.
                if ((presentedLessons.ContainsKey(Lessons.attack_fails) == false) && 
                    ((curLesson == Lessons.none) || (curLesson == Lessons.attack_1)) &&
                    ((presentedLessons.ContainsKey(Lessons.attack_2) == false) || (presentedLessons.ContainsKey(Lessons.attack_9)))) 
                {
                    ShowLesson(Lessons.attack_fails);
                    yield break;
                }
            }

            if (presentedLessons.ContainsKey(Lessons.attack_1))
            {
                if (presentedLessons.ContainsKey(Lessons.attack_9) == false)
                {
                    // Display the next attack lesson if appropriate.
                    ShowLessonIfAppropriate();
                }
            }
        }

        if (GameData.instance.mapMode == GameData.MapMode.RAID)
        {
            if (presentedLessons.ContainsKey(Lessons.raid_7) && (presentedLessons.ContainsKey(Lessons.raid_13) == false) && ((curLesson == Lessons.raid_7) || (curLesson == Lessons.none)))
            {
                ShowLesson(Lessons.raid_8);
                yield return null;
            }
        }
    }

    public void BlockProcessBegins(float _delay, int _process_type)
    {
        if (muted) {
            return;
        }

        StartCoroutine(ProcessBlockProcessEnd(_delay, _process_type));
    }

    public IEnumerator ProcessBlockProcessEnd(float _delay, int _process_type)
    {
        // Wait until the block process has finished, and a little longer
        yield return new WaitForSeconds(_delay + 0.5f);

        if (GameData.instance.mapMode == GameData.MapMode.MAINLAND)
        {
            if (_process_type == BlockProcess.PROCESS_OCCUPY)
            {
                if (curLesson == Lessons.occupy_empty_1)
                {
                    ShowLesson(Lessons.occupy_empty_2);
                    yield return null;
                }

                if (curLesson == Lessons.occupy_empty_2)
                {
                    // Show the next attack lesson if appropriate.
                    ShowLessonIfAppropriate();
                    yield return null;
                }

                if (presentedLessons.ContainsKey(Lessons.defense_1) && (presentedLessons.ContainsKey(Lessons.defense_2) == false))
                {
                    // Show the next defense lesson if appropriate.
                    ShowLessonIfAppropriate();
                    yield return null;
                }

                if ((curLesson == Lessons.none) && (presentedLessons.ContainsKey(Lessons.occupy_area) == false) && (MapView.instance.GetAutoProcess() == MapView.AutoProcessType.NONE))
                {
                    evacRunningAvg = 0;
                    occupyRunningAvg = (occupyRunningAvg * Mathf.Pow(0.25f, (Time.unscaledTime - prevOccupyTime) / 60f)) + 1f;
                    prevOccupyTime = Time.unscaledTime;

                    if (occupyRunningAvg >= 7f) 
                    {
                        ShowLesson(Lessons.occupy_area);
                        yield return null;
                    }
                }
            }
            else if (_process_type == BlockProcess.PROCESS_EVACUATE)
            {
                if ((curLesson == Lessons.none) && (presentedLessons.ContainsKey(Lessons.evacuate_area) == false) && (MapView.instance.GetAutoProcess() == MapView.AutoProcessType.NONE))
                {
                    occupyRunningAvg = 0;
                    evacRunningAvg = (evacRunningAvg * Mathf.Pow(0.25f, (Time.unscaledTime - prevEvacTime) / 60f)) + 1f;
                    prevEvacTime = Time.unscaledTime;

                    if (evacRunningAvg >= 7f) 
                    {
                        ShowLesson(Lessons.evacuate_area);
                        yield return null;
                    }
                }
            }
        }

        if (GameData.instance.mapMode == GameData.MapMode.HOMELAND)
        {
            if (_process_type == BlockProcess.PROCESS_OCCUPY)
            {
                if (presentedLessons.ContainsKey(Lessons.homeland_3) && (presentedLessons.ContainsKey(Lessons.homeland_4) == false) && (GameData.instance.GetFinalGeoEfficiency() >= 0.7f))
                {
                    ShowLesson(Lessons.homeland_4);
                    yield return null;
                }
            }
        }

        if (GameData.instance.mapMode == GameData.MapMode.RAID)
        {
            if (_process_type == BlockProcess.PROCESS_OCCUPY)
            {
                if (presentedLessons.ContainsKey(Lessons.raid_6) && (presentedLessons.ContainsKey(Lessons.raid_8) == false) && (prevLessonShown != Lessons.raid_7) && ((curLesson == Lessons.raid_6) || (curLesson == Lessons.none)))
                {
                    ShowLesson(Lessons.raid_7);
                    yield return null;
                }
            }
        }
    }

    public void AddedXP(int _xp_delta)
    {
        xpAddedAmount = _xp_delta;
    }

    public void ViewReturnedToBounds()
    {
        if (GameData.instance.mapMode != GameData.MapMode.MAINLAND) {
            return;
        }

        // Show the past boundary lesson if appropriate.
        if ((presentedLessons.ContainsKey(Lessons.past_boundary) == false) &&
            ((curLesson == Lessons.moving_map) || (curLesson == Lessons.none)))
        {
            ShowLesson(Lessons.past_boundary);
        }
    }

    public void ViewPanFinished()
    {
        if ((presentedLessons.ContainsKey(Lessons.resources) == false) || (presentedLessons.ContainsKey(Lessons.orbs) == false))
        {
            ShowLessonIfAppropriate();
        }
    }

    public void OnSwitchedMap()
    {
        // If showing a lesson that isn't appropriate to the current map mode, hide the lesson.
        if ((GameData.instance.mapMode == GameData.MapMode.MAINLAND) && (curLesson >= Lessons.homeland_1)) {
            HideLesson();
        }
        else if ((GameData.instance.mapMode == GameData.MapMode.HOMELAND) && ((curLesson < Lessons.homeland_1) || ((curLesson >= Lessons.raid_1) && (curLesson <= Lessons.raid_13)))) {
            HideLesson();
        }
        else if ((GameData.instance.mapMode == GameData.MapMode.RAID) && ((curLesson < Lessons.raid_1) || (curLesson >Lessons.raid_13))) {
            HideLesson();
        }
        
        ShowLessonIfAppropriate();
    }

    public void ActiveGamePanelSet(GameGUI.GamePanel _activeGamePanel)
    {
        if (muted) {
            return;
        }

        if (GameData.instance.mapMode == GameData.MapMode.MAINLAND) 
        {
            if ((_activeGamePanel == GameGUI.GamePanel.GAME_PANEL_NATION) && (curLesson == Lessons.defense_8))
            {
                if (NationPanel.instance.statsContentArea.activeSelf)
                {
                    // The Nation Panel's Stats tab is open. Advance to defense_9.
                    ShowLesson(Lessons.defense_9);
                }
            }

            if ((_activeGamePanel == GameGUI.GamePanel.GAME_PANEL_NONE) && (curLesson == Lessons.defense_16))
            {
                // The game panel has ben closed. Advance to defense_17.
                ShowLesson(Lessons.defense_17);
            }

            if ((_activeGamePanel == GameGUI.GamePanel.GAME_PANEL_ADVANCES) && (curLesson == Lessons.level_up_2))
            {
                // The Advances Panel has opened. Advance to level_up_3.
                ShowLesson(Lessons.level_up_3);
            }

            if ((_activeGamePanel == GameGUI.GamePanel.GAME_PANEL_MAP) && ((curLesson == Lessons.none) || (curLesson == Lessons.map_panel_1)) && (presentedLessons.ContainsKey(Lessons.map_panel_2) == false))
            {
                // The map panel has been opened, show map_panel_2.
                ShowLesson(Lessons.map_panel_2);
            }
        }

        if (GameData.instance.mapMode == GameData.MapMode.HOMELAND) 
        {
            if ((_activeGamePanel == GameGUI.GamePanel.GAME_PANEL_RAID) && ((curLesson == Lessons.raid_log_1) || (curLesson == Lessons.none)) && (presentedLessons.ContainsKey(Lessons.raid_log_4) == false))
            {
                // The Raid Panel is open. Advance to raid_log_2.
                ShowLesson(Lessons.raid_log_2);
            }

            if ((_activeGamePanel == GameGUI.GamePanel.GAME_PANEL_NONE) && presentedLessons.ContainsKey(Lessons.raid_log_4) && (presentedLessons.ContainsKey(Lessons.homeland_11) == false))
            {
                ShowLesson(Lessons.homeland_11);
            }
        }

        // Reposition the arrow on the UI, if appropriate.
        PositionArrowOnUI();
    }

    public void PanelTabActivated()
    {
        if (muted) {
            return;
        }

        if ((GameData.instance != null) && (GameData.instance.mapMode == GameData.MapMode.MAINLAND))
        {
            if ((GameGUI.instance != null) && (GameGUI.instance.GetActiveGamePanel() == GameGUI.GamePanel.GAME_PANEL_NATION) && (curLesson == Lessons.defense_8))
            {
                if (NationPanel.instance.statsContentArea.activeSelf)
                {
                    // The Nation Panel's Stats tab is open. Advance to defense_9.
                    ShowLesson(Lessons.defense_9);
                }
            }
        }

        // Reposition the arrow on the UI, if appropriate.
        PositionArrowOnUI();
    }

    public void BuildMenuOpened()
    {
        if (GameData.instance.mapMode == GameData.MapMode.MAINLAND) 
        {
            if (presentedLessons.ContainsKey(Lessons.defense_2) && (presentedLessons.ContainsKey(Lessons.defense_3) == false) && ((curLesson == Lessons.defense_2) || (curLesson == Lessons.none)))
            {
                // The build menu has been opened; advance to defense lesson 3.
                ShowLesson(Lessons.defense_3);
            }
        }

        if (GameData.instance.mapMode == GameData.MapMode.HOMELAND) 
        {
            if (presentedLessons.ContainsKey(Lessons.homeland_5) && (presentedLessons.ContainsKey(Lessons.homeland_6) == false) && ((curLesson == Lessons.homeland_5) || (curLesson == Lessons.none)))
            {
                // The build menu has been opened; advance to homeland lesson 6.
                ShowLesson(Lessons.homeland_6);
            }
        }
    }

    public void BuildCommandSent(int _buildID)
    {
        if (GameData.instance.mapMode == GameData.MapMode.MAINLAND) 
        {
            //Debug.Log("Tutorial: BuildCommandSent() contains defense_2: " + presentedLessons.ContainsKey(Lessons.defense_2) + ", contains defense_4: " + presentedLessons.ContainsKey(Lessons.defense_4) + ", cur: " + curLesson);

            if (presentedLessons.ContainsKey(Lessons.defense_2) && (presentedLessons.ContainsKey(Lessons.defense_4) == false) && ((curLesson == Lessons.defense_2) || (curLesson == Lessons.defense_3) || (curLesson == Lessons.none)))
            {
                // A build command has been sent; advance to defense lesson 4.
                ShowLesson(Lessons.defense_4);
            }
        }

        if (GameData.instance.mapMode == GameData.MapMode.HOMELAND) 
        {
            // If building an orb shard...
            if ((_buildID == 200) || (_buildID == 201) || (_buildID == 202))
            {
                // Determine how many of the orb shards have previously been built.
                int num_shards = GameData.instance.DetermineNumShardsPlaced();

                if ((num_shards >= 2) && presentedLessons.ContainsKey(Lessons.homeland_8) && (presentedLessons.ContainsKey(Lessons.homeland_9) == false) && ((curLesson == Lessons.homeland_6) || (curLesson == Lessons.homeland_7)  || (curLesson == Lessons.homeland_8) || (curLesson == Lessons.none)))
                {
                    // Building last orb shard -- show homeland lesson 9.
                    ShowLesson(Lessons.homeland_9);
                }
                else if ((num_shards >= 1) && presentedLessons.ContainsKey(Lessons.homeland_7) && (presentedLessons.ContainsKey(Lessons.homeland_8) == false) && ((curLesson == Lessons.homeland_7) || (curLesson == Lessons.none)))
                {
                    // Building second orb shard -- show homeland lesson 8.
                    ShowLesson(Lessons.homeland_8);
                }
                else if ((num_shards >= 0) && presentedLessons.ContainsKey(Lessons.homeland_6) && (presentedLessons.ContainsKey(Lessons.homeland_7) == false) && ((curLesson == Lessons.homeland_6) || (curLesson == Lessons.none)))
                {
                    // Building first orb shard -- show homeland lesson 7.
                    ShowLesson(Lessons.homeland_7);
                }
            }
            else
            {
                // If at least 50% of generated energy is being burned by homeland defenses...
                if (((curLesson == Lessons.none) || (curLesson == Lessons.homeland_9)) && ((presentedLessons.ContainsKey(Lessons.raid_1) == false) || ((presentedLessons.ContainsKey(Lessons.raid_13) == false) && (prevLessonPresentedLoginTime != GameData.instance.gameTimeAtLogin))) && (GameData.instance.GetFinalEnergyBurnRate() >= (GameData.instance.GetFinalEnergyRate() * 0.5f)))
                {
                    ShowLesson(Lessons.homeland_10);
                }
            }
        }
    }

    public void LevelChanged()
    {
        if (GameData.instance.mapMode != GameData.MapMode.MAINLAND) {
            return;
        }

        if ((curLesson == Lessons.none) && (presentedLessons.ContainsKey(Lessons.level_up_4) == false) && (GameData.instance.advance_points > 0))
        {
            // Display level_up_1 lesson
            ShowLesson(Lessons.level_up_1);
        }
    }

    public void ResearchCommandSent()
    {
        if (muted) {
            return;
        }

        if (GameData.instance.mapMode != GameData.MapMode.MAINLAND) {
            return;
        }

        if (curLesson == Lessons.level_up_4)
        {
            // Hide lesson level_up_4, which instructs to research an advance.
            HideLesson();
        }
    }

    public void PushedFurtherEast()
    {
        if (muted) {
            return;
        }

        if (GameData.instance.mapMode != GameData.MapMode.MAINLAND) {
            return;
        }

        // Do nothing if we've already shown the mountain paths lesson, or if a lesson is currently being shown.
        if ((curLesson != Lessons.none) || (presentedLessons.ContainsKey(Lessons.mountain_paths))) {
            return;
        }

        if ((Time.unscaledTime - prevPushedFurtherEastTime) > 120f)
        {
            // If the nation has pushed at least 5 squares further east in the past 2 minutes, show the mountain path lesson.
            if ((MapView.instance.maxX1 - prevMaxX1) >= 5)
            {
                ShowLesson(Lessons.mountain_paths);
                return;
            }

            prevPushedFurtherEastTime = Time.unscaledTime;
            prevMaxX1 = MapView.instance.maxX1;
        }
    }

    public void InfoEventReceived()
    {
        StartCoroutine(InfoEventReceived_Coroutine());
    }

    public IEnumerator InfoEventReceived_Coroutine()
    {
        // Wait for a brief delay before opening a lesson.
        yield return new WaitForSeconds(3f);

        // Show a lesson if appropriate.
        ShowLessonIfAppropriate();
    }

    public bool IsLessonActive()
    {
        return (curLesson != Lessons.none);
    }
}
