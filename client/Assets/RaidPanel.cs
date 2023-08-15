using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using I2.Loc;

public class RaidPanel : MonoBehaviour
{
    public static RaidPanel instance;

    public Sprite inactivePanelTabSprite, activePanelTabSprite;
    public GameObject attackPanelTab, defensePanelTab, leaguePanelTab;
    public GameObject attackContentArea, defenseContentArea, leagueContentArea;
    public GameObject attackListContentObject, defenseListContentObject;

    public GameObject cur_league_panel, next_league_panel;
    public TMPro.TextMeshProUGUI cur_league_header, cur_league_raid_reward_credits, cur_league_raid_reward_xp, cur_league_raid_reward_rebirth, cur_league_defense_reward_credits, cur_league_defense_reward_xp, cur_league_defense_reward_rebirth;
    public TMPro.TextMeshProUGUI next_league_header, next_league_raid_reward_credits, next_league_raid_reward_xp, next_league_raid_reward_rebirth, next_league_defense_reward_credits, next_league_defense_reward_xp, next_league_defense_reward_rebirth;

    GameObject selectedPanelTab = null;

    Color tabTextColor_Inctive = new Color(0.65f, 0.65f, 0.65f);
    Color tabTextColor_Active = new Color(1.0f, 1.0f, 1.0f);
    Color tabTextColor_Alert = new Color(0, 0, 0);

    public RaidPanel()
    {
        instance = this;
    }

	void Start ()
    {
		// Turn off the alert image of each panel tab
        attackPanelTab.transform.GetChild(0).gameObject.SetActive(false);
        defensePanelTab.transform.GetChild(0).gameObject.SetActive(false);
        leaguePanelTab.transform.GetChild(0).gameObject.SetActive(false);

        // Set text of each tab initially to inactive color.
        attackPanelTab.transform.GetChild(1).GetComponent<Text>().color = tabTextColor_Inctive;
        defensePanelTab.transform.GetChild(1).GetComponent<Text>().color = tabTextColor_Inctive;
        leaguePanelTab.transform.GetChild(1).GetComponent<Text>().color = tabTextColor_Inctive;
	}

    public void OnEnable()
    {
        // Update the time-ago text for each raid entry in both lists.
        foreach (Transform child in attackListContentObject.transform) child.GetComponent<RaidEntry>().UpdateTimeAgoText();
        foreach (Transform child in defenseListContentObject.transform) child.GetComponent<RaidEntry>().UpdateTimeAgoText();

        // If no panel tab is yet selected, select the defense panel tab by default.
        if (selectedPanelTab == null) {
            TabPressed(defensePanelTab);
        }

        // If the defense log is open but there are no defenses in the list, open the league tab instead.
        if ((selectedPanelTab == defensePanelTab) && (defenseListContentObject.transform.childCount == 0)) {
            TabPressed(attackPanelTab);
        }

        // If the attack log is open but there are no attacks in the list, open the defense tab instead.
        if ((selectedPanelTab == attackPanelTab) && (attackListContentObject.transform.childCount == 0)) {
            TabPressed(leaguePanelTab);
        }

        // If the League subpanel is active, update the league info.
        if (selectedPanelTab == leaguePanelTab) {
            UpdateLeagueSubpanel();
        }
    }

    public void OnEventRaidLogs()
    {
        // Clear both raid log lists.
        ClearRaidEntryList(attackListContentObject);
        ClearRaidEntryList(defenseListContentObject);

        // Fill both raid log lists.
        FillRaidEntryList(attackListContentObject, GameData.instance.raidAttackLog, true);
        FillRaidEntryList(defenseListContentObject, GameData.instance.raidDefenseLog, false);
    }

    public void ClearRaidEntryList(GameObject _content)
    {
        // Remove any raid entries from the given list content object.
        GameObject cur_entry_object;
        while (_content.transform.childCount > 0)
        {
            cur_entry_object = _content.transform.GetChild(0).gameObject;
            cur_entry_object.transform.SetParent(null);
            MemManager.instance.ReleaseRaidEntryObject(cur_entry_object);
        }
    }

    public void FillRaidEntryList(GameObject _content, List<RaidLogRecord> _record_list, bool _attack)
    {
        //Debug.Log("FillRaidEntryList() count: " + _record_list.Count + ", _attack: " + _attack);
        // Add raid entries to list.
        foreach (RaidLogRecord record in _record_list) {
            AddToRaidEntryList(_content, record, _attack);
        }
    }

    public void AddToRaidEntryList(RaidLogRecord _record, bool _attack)
    {
        AddToRaidEntryList(_attack ? attackListContentObject : defenseListContentObject, _record, _attack);
    }

    public void AddToRaidEntryList(GameObject _content, RaidLogRecord _record, bool _attack)
    {
        //Debug.Log("  AddToRaidEntryList()");

        // Get a new raid entry
        GameObject entryObject = MemManager.instance.GetRaidEntryObject();

        // Add the new entry to the list.
        entryObject.transform.SetParent(_content.transform);
        entryObject.transform.SetAsFirstSibling();
        entryObject.transform.localScale = new Vector3(1, 1, 1); // Needs to be done each time it's activated, in case it was changed last time used.

        // Get pointer to RaidEntry component.
        RaidEntry curEntry = entryObject.GetComponent<RaidEntry>();
            
        // Initialize the new entry
        curEntry.Init(_record, _attack);
    }
	
    public void TabPressed(GameObject _panelTab)
    {
        // If there was a formerly active tab, display it as inactive.
        if (selectedPanelTab != null)
        {
            selectedPanelTab.GetComponent<Image>().sprite = inactivePanelTabSprite;
            selectedPanelTab.transform.GetChild(1).GetComponent<Text>().color = tabTextColor_Inctive;
        }

        // Record the new active chat tab.
        selectedPanelTab = _panelTab;

        // Display the newly activated tab as being active.
        selectedPanelTab.GetComponent<Image>().sprite = activePanelTabSprite;

        // Set selected tab's text color.
        selectedPanelTab.transform.GetChild(1).GetComponent<Text>().color = tabTextColor_Active;

        // Turn off the newly activated tab's alert image.
        selectedPanelTab.transform.GetChild(0).gameObject.SetActive(false);

        // Activate the appropriate content area.
        attackContentArea.SetActive(selectedPanelTab == attackPanelTab);
        defenseContentArea.SetActive(selectedPanelTab == defensePanelTab);
        leagueContentArea.SetActive(selectedPanelTab == leaguePanelTab);

        // If the League subpanel is active, update the league info.
        if (selectedPanelTab == leaguePanelTab) {
            UpdateLeagueSubpanel();
        }
    }

    public void ActivateTabAlert(GameObject _panelTab)
    {
        // Do nothing if the given tab is already selected.
        if (_panelTab == selectedPanelTab) {
            return;
        }

        // Turn on the alert image.
        _panelTab.transform.GetChild(0).gameObject.SetActive(true);

        // Change text color.
        //_panelTab.transform.GetChild(1).GetComponent<Text>().color = tabTextColor_Alert;
    }

    public void UpdateLeagueSubpanel()
    {
        int cur_league = (GameData.instance.raidNumMedals / GameData.instance.raidMedalsPerLeague);

        // Update the info panels for both leagues.
        UpdateLeagueInfo(cur_league, cur_league_panel, cur_league_header, cur_league_raid_reward_credits, cur_league_raid_reward_xp, cur_league_raid_reward_rebirth, cur_league_defense_reward_credits, cur_league_defense_reward_xp, cur_league_defense_reward_rebirth);
        UpdateLeagueInfo(cur_league + 1, next_league_panel, next_league_header, next_league_raid_reward_credits, next_league_raid_reward_xp, next_league_raid_reward_rebirth, next_league_defense_reward_credits, next_league_defense_reward_xp, next_league_defense_reward_rebirth);
    }

    public void UpdateLeagueInfo(int _league_index, GameObject _league_panel, TMPro.TextMeshProUGUI _league_header, TMPro.TextMeshProUGUI _league_raid_reward_credits, TMPro.TextMeshProUGUI _league_raid_reward_xp, TMPro.TextMeshProUGUI _league_raid_reward_rebirth, TMPro.TextMeshProUGUI _league_defense_reward_credits, TMPro.TextMeshProUGUI _league_defense_reward_xp, TMPro.TextMeshProUGUI _league_defense_reward_rebirth)
    {
        LeagueData league_data = LeagueData.GetLeagueData(_league_index);

        // If there is no info for this league, don't display it.
        _league_panel.SetActive(league_data != null);

        if (league_data == null) {
            return;
        }

        _league_header.text = ((_league_panel == next_league_panel) ? (LocalizationManager.GetTranslation("Generic Text/next") + ": ") : "") +LocalizationManager.GetTranslation("Generic Text/league_word") + " " + (_league_index + 1) + " (" + (_league_index * GameData.instance.raidMedalsPerLeague) + "-" + ((_league_index + 1) * GameData.instance.raidMedalsPerLeague - 1) + "<sprite=30>)";

        _league_raid_reward_credits.text = "<color=green>+" + string.Format("{0:n0}", league_data.raid_reward_credits) + "</color><sprite=2>";
        _league_raid_reward_xp.text = "<color=green>+" + string.Format("{0:n0}", league_data.raid_reward_xp) + "</color> " + LocalizationManager.GetTranslation("Generic Text/xp_word");
        _league_raid_reward_rebirth.text = "<color=green>+" + string.Format("{0:n0}", league_data.raid_reward_rebirth) + "</color> " + LocalizationManager.GetTranslation("Raid/raid_to_rebirth");

        _league_defense_reward_credits.text = "<color=green>+" + string.Format("{0:n0}", league_data.defense_daily_credits) + "</color><sprite=2> " + LocalizationManager.GetTranslation("Raid/raid_per_day");
        _league_defense_reward_xp.text = "<color=green>+" + string.Format("{0:n0}", league_data.defense_daily_xp) + "</color> " + LocalizationManager.GetTranslation("Generic Text/xp_word") + " " + LocalizationManager.GetTranslation("Raid/raid_per_day");
        _league_defense_reward_rebirth.text = "<color=green>+" + string.Format("{0:n0}", league_data.defense_daily_rebirth) + "</color> " + LocalizationManager.GetTranslation("Raid/raid_to_rebirth") + " " + LocalizationManager.GetTranslation("Raid/raid_per_day");

        // Only show the rebirth reward lines if there is any rebirth reward.
        _league_raid_reward_rebirth.gameObject.SetActive(league_data.raid_reward_rebirth > 0);
        _league_defense_reward_rebirth.gameObject.SetActive(league_data.defense_daily_rebirth > 0);
    }

    public void OnClick_DefenseTab()
    {
        TabPressed(defensePanelTab);
    }

    public void OnClick_AttackTab()
    {
        TabPressed(attackPanelTab);
    }

    public void OnClick_LeagueTab()
    {
        TabPressed(leaguePanelTab);
    }
}
