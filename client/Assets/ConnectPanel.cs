using UnityEngine;
using UnityEngine.UI;
using UnityEngine.EventSystems;
using UnityEngine.Networking;
using System.Collections;
using System.Collections.Generic;
using System.Xml;
using System;
using System.Globalization;
using I2.Loc;

public class ConnectPanel : MonoBehaviour, RequestorListener, IPointerDownHandler
{
    public static ConnectPanel instance;

    public Sprite inactivePanelTabSprite, activePanelTabSprite;
    public GameObject recruitPanelTab, patronPanelTab, ranksPanelTab, newsPanelTab;
    public GameObject recruitContentArea, patronContentArea, ranksContentArea, newsContentArea;
    public GameObject ranksListContent, followerListContent, patronOfferListContent;

    // Recruit subpanel
    public GameObject recruitFriendsArea, followersList;
    public TMPro.TextMeshProUGUI patronCodeTitle, followersTitle, patronCodeDesc;
    public GameObject followerEntryPrefab;

    // Patron subpanel
    public GameObject patronInfoArea, choosePatronArea, noPatronMessage, noInvitesMessage;
    public TMPro.TextMeshProUGUI patronInfoTitle, choosePatronTitle, patronLastMonthBonusXP, patronLastMonthBonusCredits, patronTotalBonusXP, patronTotalBonusCredits;
    public GameObject patronOfferEntryPrefab;

    // Ranks subpanel
    public GameObject titleLinePrefab, rankLinePrefab, moreLinePrefab, prizeDisclaimerPrefab;
    public GameObject backButtonArea;
    public GameObject progressRing;

    // News subpanel
    public GameObject newsTextPrefab, newsListItemPrefab, newsImagePrefab, newsHRulePrefab;
    public GameObject newsContent;
    public ScrollRect newsScrollRect;
    public Scrollbar newsScrollbar;
    List<string> links = new List<string>();
    List<TMPro.TextMeshProUGUI> text_blocks = new List<TMPro.TextMeshProUGUI>();
    float prevNewsFetchTime = -1000000;
    String news_html = "";
    int html_pos = 0;
    const float MIN_NEWS_FETCH_PERIOD = 300; // Minimum of 5 minutes between news fetches
    const int NEWS_PAGE_LENGTH = 5000; // Roughly the size of a chunk of HTML displayed as a page.
    const string NEWS_URL_DIR = "https://warofconquest.com/woc2/news/";

    Color tabTextColor_Inctive = new Color(0.65f, 0.65f, 0.65f);
    Color tabTextColor_Active = new Color(1.0f, 1.0f, 1.0f);
    Color tabTextColor_Alert = new Color(0, 0, 0);

    GameObject selectedPanelTab = null;

    GameData.RanksListType ranks_list = GameData.RanksListType.UNDEF, pending_ranks_list = GameData.RanksListType.COMBINED;
    int orb_x = 0, orb_z = 0;
    float prev_update_ranks_time = -10000f;
    XmlDocument ranksXmlDoc = null;
    bool ranks_file_received = false, ranks_data_received = false;

    private const float UPDATE_RANKS_INTERVAL = 300f;

    enum PlayerRankType
    {
        None,
        Nation,
        User
    }

    private enum RequestorTask
    {
        PatronOfferAccept,
        PatronOfferDecline,
        RemoveFollower
    };

    public ConnectPanel()
    {
        instance = this;
    }

    public void Awake()
    {
        // Turn off the alert image of each panel tab
        recruitPanelTab.transform.GetChild(0).gameObject.SetActive(false);
        patronPanelTab.transform.GetChild(0).gameObject.SetActive(false);
        ranksPanelTab.transform.GetChild(0).gameObject.SetActive(false);
        newsPanelTab.transform.GetChild(0).gameObject.SetActive(false);

        // Set text of each tab initially to inactive color.
        recruitPanelTab.transform.GetChild(1).GetComponent<Text>().color = tabTextColor_Inctive;
        patronPanelTab.transform.GetChild(1).GetComponent<Text>().color = tabTextColor_Inctive;
        ranksPanelTab.transform.GetChild(1).GetComponent<Text>().color = tabTextColor_Inctive;
        newsPanelTab.transform.GetChild(1).GetComponent<Text>().color = tabTextColor_Inctive;

        // Set up scroll rect listeners for each of the message lists.
        newsScrollRect.onValueChanged.AddListener(NewsScroll_OnValueChanged);

        if (selectedPanelTab == null)
        {
            // Begin with the recruit tab active.
            TabPressed(recruitPanelTab);
        }
    }

    public void OnEnable()
    {
        // Hide the progress ring.
        progressRing.SetActive(false);

        // If this panel is activated and the ranks tab is selected, display the combined ranks list.
        if (selectedPanelTab == ranksPanelTab) {
            DisplayRanks(pending_ranks_list, orb_x, orb_z);
        }

        pending_ranks_list = GameData.RanksListType.COMBINED;
    }

    public void InfoEventReceived()
    {
        // Reset the ranks list for this login.
        ranks_list = GameData.RanksListType.UNDEF;

        // Clear the follower and patron offer list.
        ClearFollowerListDisplay();
        ClearPatronOfferListDisplay();
    }

    public void TabPressed(GameObject _panelTab)
    {
        // If there was a formerly active panel tab, display it as inactive.
        if (selectedPanelTab != null)
        {
            selectedPanelTab.GetComponent<Image>().sprite = inactivePanelTabSprite;
            selectedPanelTab.transform.GetChild(1).GetComponent<Text>().color = tabTextColor_Inctive;
        }

        // Record the new active panel tab.
        selectedPanelTab = _panelTab;
        //Debug.Log("TabPressed() " + _panelTab);

        // Display the newly activated tab as being active.
        selectedPanelTab.GetComponent<Image>().sprite = activePanelTabSprite;

        // Set selected tab's text color.
        selectedPanelTab.transform.GetChild(1).GetComponent<Text>().color = tabTextColor_Active;

        // Turn off the newly activated tab's alert image.
        selectedPanelTab.transform.GetChild(0).gameObject.SetActive(false);

        // Activate the appropriate content area.
        recruitContentArea.SetActive(selectedPanelTab == recruitPanelTab);
        patronContentArea.SetActive(selectedPanelTab == patronPanelTab);
        ranksContentArea.SetActive(selectedPanelTab == ranksPanelTab);
        newsContentArea.SetActive(selectedPanelTab == newsPanelTab);

        // If the news hasn't been fetched within the MIN_NEWS_FETCH_PERIOD, fetch and display it now.
        if ((selectedPanelTab == newsPanelTab) && ((Time.unscaledTime - prevNewsFetchTime) >= MIN_NEWS_FETCH_PERIOD))
        {
            FetchNews();
            prevNewsFetchTime = Time.unscaledTime;
        }

        // Hide the progress ring.
        progressRing.SetActive(false);

        // If this panel is active and the ranks tab is selected, display the combined ranks list.
        if ((this.gameObject.activeInHierarchy) && (selectedPanelTab == ranksPanelTab)) {
            DisplayRanks(GameData.RanksListType.COMBINED);
        }

        // Tell the tutorial system about this tab being activated.
        Tutorial.instance.PanelTabActivated();
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

    public void OnClick_RecruitTab()
    {
        TabPressed(recruitPanelTab);
    }

    public void OnClick_PatronTab()
    {
        TabPressed(patronPanelTab);
    }

    public void OnClick_RanksTab()
    {
        TabPressed(ranksPanelTab);
    }

    public void OnClick_NewsTab()
    {
        TabPressed(newsPanelTab);
    }

    public void SetPendingRanksList(GameData.RanksListType _ranks_list, int _orb_x = 0, int _orb_z = 0)
    {
        // Record new pending ranks list and orb location.
        pending_ranks_list = _ranks_list;
        orb_x = _orb_x;
        orb_z = _orb_z;

        // Activate the ranks panel.
        TabPressed(ranksPanelTab);
    }

    public void DisplayRanks(GameData.RanksListType _ranks_list, int _orb_x = 0, int _orb_z = 0)
    {
        // If displaying a different ranks list from what was last displayed, or displaying orb winnings, or enough time has passed since last update, fetch data.
        if ((_ranks_list != ranks_list) || (_ranks_list == GameData.RanksListType.ORB_WINNINGS) || ((Time.unscaledTime - prev_update_ranks_time) >= UPDATE_RANKS_INTERVAL))
        {
            // Clear the current ranks list display
            ClearRanksListDisplay();

            // Record new current ranks list, and update time.
            ranks_list = _ranks_list;
            orb_x = _orb_x;
            orb_z = _orb_z;
            prev_update_ranks_time = Time.unscaledTime;

            // Fetch the ranks list data
            StartCoroutine(FetchRanksList());
        }
    }

    public IEnumerator FetchRanksList()
    {
        float start_time;

        // Determine filename of ranks list file.
        string filename = "";
        switch (ranks_list)
        {
            case GameData.RanksListType.COMBINED: filename = "ranks_combined.xml"; break;
            case GameData.RanksListType.NATION_XP: filename = "ranks_nation_xp.xml"; break;
            case GameData.RanksListType.NATION_XP_MONTHLY: filename = "ranks_nation_xp_monthly.xml"; break;
            case GameData.RanksListType.USER_XP: filename = "ranks_user_xp.xml"; break;
            case GameData.RanksListType.USER_XP_MONTHLY: filename = "ranks_user_xp_monthly.xml"; break;
            case GameData.RanksListType.USER_FOLLOWERS: filename = "ranks_user_followers.xml"; break;
            case GameData.RanksListType.USER_FOLLOWERS_MONTHLY: filename = "ranks_user_followers_monthly.xml"; break;
            case GameData.RanksListType.NATION_WINNINGS: filename = "ranks_nation_winnings.xml"; break;
            case GameData.RanksListType.NATION_WINNINGS_MONTHLY: filename = "ranks_nation_winnings_monthly.xml"; break;
            case GameData.RanksListType.NATION_RAID_EARNINGS: filename = "ranks_nation_raid_earnings.xml"; break;
            case GameData.RanksListType.NATION_RAID_EARNINGS_MONTHLY: filename = "ranks_nation_raid_earnings_monthly.xml"; break;
            case GameData.RanksListType.NATION_ORB_SHARD_EARNINGS: filename = "ranks_nation_orb_shard_earnings.xml"; break;
            case GameData.RanksListType.NATION_ORB_SHARD_EARNINGS_MONTHLY: filename = "ranks_nation_orb_shard_earnings_monthly.xml"; break;
            case GameData.RanksListType.NATION_MEDALS: filename = "ranks_nation_medals.xml"; break;
            case GameData.RanksListType.NATION_MEDALS_MONTHLY: filename = "ranks_nation_medals_monthly.xml"; break;
            case GameData.RanksListType.NATION_LATEST_TOURNAMENT: filename = "ranks_nation_latest_tournament.xml"; break;
            case GameData.RanksListType.NATION_TOURNAMENT_TROPHIES: filename = "ranks_nation_tournament_trophies.xml"; break;
            case GameData.RanksListType.NATION_TOURNAMENT_TROPHIES_MONTHLY: filename = "ranks_nation_tournament_trophies_monthly.xml"; break;
            case GameData.RanksListType.NATION_LEVEL: filename = "ranks_nation_level.xml"; break;
            case GameData.RanksListType.NATION_REBIRTHS: filename = "ranks_nation_rebirths.xml"; break;
            case GameData.RanksListType.NATION_QUESTS: filename = "ranks_nation_quests.xml"; break;
            case GameData.RanksListType.NATION_QUESTS_MONTHLY: filename = "ranks_nation_quests_monthly.xml"; break;
            case GameData.RanksListType.NATION_ENERGY_DONATED: filename = "ranks_nation_energy_donated.xml"; break;
            case GameData.RanksListType.NATION_ENERGY_DONATED_MONTHLY: filename = "ranks_nation_energy_donated_monthly.xml"; break;
            case GameData.RanksListType.NATION_MANPOWER_DONATED: filename = "ranks_nation_manpower_donated.xml"; break;
            case GameData.RanksListType.NATION_MANPOWER_DONATED_MONTHLY: filename = "ranks_nation_manpower_donated_monthly.xml"; break;
            case GameData.RanksListType.NATION_AREA: filename = "ranks_nation_area.xml"; break;
            case GameData.RanksListType.NATION_AREA_MONTHLY: filename = "ranks_nation_area_monthly.xml"; break;
            case GameData.RanksListType.NATION_CAPTURES: filename = "ranks_nation_captures.xml"; break;
            case GameData.RanksListType.NATION_CAPTURES_MONTHLY: filename = "ranks_nation_captures_monthly.xml"; break;
            case GameData.RanksListType.ORB_WINNINGS: filename = "ranks_" + orb_x + "_" + orb_z + ".xml"; break;
        }

        // Reset the records of whether the ranks file and data have been received.
        ranks_file_received = false;
        ranks_data_received = false;

        // Request this user and nation's relevant ranks data from the game server.
        if (ranks_list == GameData.RanksListType.ORB_WINNINGS) {
            Network.instance.SendCommand("action=get_orb_winnings|x=" + orb_x + "|y=" + orb_z);
        } else {
            Network.instance.SendCommand("action=get_ranks_data");
        }

        // Access the game server's ranks xml file; yield until it is retrieved.
        String url = "http://" + Network.instance.GetConnectedServerAddress() + "/generated/ranks/" + filename;
        //Debug.Log("ranks list: " + ranks_list + ", ranks url: " + url);

        UnityWebRequest www = UnityWebRequest.Get(url);
		yield return Network.instance.RequestFromWeb(www);

		if (www.isNetworkError || www.isHttpError) {
			Debug.Log("Error accessing ranks xml at '" + url + "': " + www.error);
        } else {
    	    // Parse the xml file
		    ranksXmlDoc= new XmlDocument();
		    ranksXmlDoc.LoadXml(www.downloadHandler.text);

            // Record that the ranks file has been received.
            ranks_file_received = true;

            // If the ranks data has already been received, we have all the info we need. Compose the ranks list display now.
            if (ranks_data_received) {
                ComposeRanksListDisplay();
            }
        }
    }

    public void ClearRanksListDisplay()
    {
        GameObject cur_child;

        // Hide the back button
        backButtonArea.SetActive(false);

        // Show the progress ring
        progressRing.SetActive(true);

        // Remove any children of the ranks list content object.
        while (ranksListContent.transform.childCount > 0)
        {
            cur_child = ranksListContent.transform.GetChild(0).gameObject;
            cur_child.transform.SetParent(null);
            Destroy(cur_child);
        }
    }

    public void ComposeRanksListDisplay()
    {
        // Get the list of nodes representing the individual ranks lists.
        XmlNodeList nodes = ranksXmlDoc.DocumentElement.SelectNodes("/lists/list");

        // Display each ranks list.
		foreach (XmlNode node in nodes) {
            ComposeSingleRanksList(node, ranksListContent.transform);
		}
/*
#if UNITY_IOS
		if ((ranks_list == GameData.RanksListType.COMBINED) || (ranks_list == GameData.RanksListType.NATION_WINNINGS) || (ranks_list == GameData.RanksListType.NATION_WINNINGS_MONTHLY) || (ranks_list == GameData.RanksListType.ORB_WINNINGS))
		{
			// Add the prize disclaimer to the ranks list content area.
            GameObject prizeDisclaimerLineObject = (GameObject) Instantiate(prizeDisclaimerPrefab,  new Vector3(0f, 0f, 0f), Quaternion.identity);
            prizeDisclaimerLineObject.transform.SetParent(ranksListContent.transform);
            prizeDisclaimerLineObject.transform.localScale = new Vector3(1, 1, 1);
		}
#endif
*/
        // Show the back button if appropriate.
        backButtonArea.SetActive((ranks_list != GameData.RanksListType.COMBINED) && (ranks_list != GameData.RanksListType.ORB_WINNINGS));

        // Hide the progress ring
        progressRing.SetActive(false);

        // Reset the records of whether the ranks file and data have been received.
        ranks_file_received = false;
        ranks_data_received = false;
    }

    public void ComposeSingleRanksList(XmlNode _list_node, Transform _listContentTransform)
    {
        XmlNode cur_node;
        GameObject titleLineObject, rankLineObject, moreButtonLineObject;

        // Get the list of nodes representing the individual rank lines.
        XmlNodeList nodes = _list_node.SelectNodes("ranks/rank");

        // If this list is empty, do not display it at all.
        if (nodes.Count == 0) {
            return;
        }

        // Get the list's title node
        cur_node = _list_node.SelectSingleNode("ID");

        // Determine the title text and the player's rank information based on the title node's contents.
        string title = "";
        int playerAmount = 0;
        bool orb_winnings_monthly = false;
        PlayerRankType playerRankType = PlayerRankType.None;
        GameData.RanksListType list_type = GameData.RanksListType.UNDEF;
        RankLine.RankLineFormat format = RankLine.RankLineFormat.INTEGER;
        if (cur_node.InnerText.Equals("orb_history")) {
            list_type = GameData.RanksListType.ORB_WINNINGS;
            orb_winnings_monthly = false;
            title = LocalizationManager.GetTranslation("Connect Panel/orb_history") + " " + LocalizationManager.GetTranslation("Connect Panel/all_time"); // "Orb Top Winners: All-Time";
            playerRankType = PlayerRankType.Nation;
            format = RankLine.RankLineFormat.MONEY;
            playerAmount = GameData.instance.nation_orb_winnings;
        }  else if (cur_node.InnerText.Equals("orb_history_monthly")) {
            list_type = GameData.RanksListType.ORB_WINNINGS;
            orb_winnings_monthly = true;
            title = LocalizationManager.GetTranslation("Connect Panel/orb_history") + " " + CultureInfo.CurrentCulture.DateTimeFormat.GetMonthName(DateTime.Now.Month);
            playerRankType = PlayerRankType.Nation;
            format = RankLine.RankLineFormat.MONEY;
            playerAmount = GameData.instance.nation_orb_winnings_monthly;
        }  else if (cur_node.InnerText.Equals("ranks_nation_xp")) {
            list_type = GameData.RanksListType.NATION_XP;
            title = LocalizationManager.GetTranslation("Connect Panel/ranks_nation_xp") + " " + LocalizationManager.GetTranslation("Connect Panel/all_time"); // "Top XP Earning Nations: All-Time";
            playerRankType = PlayerRankType.Nation;
            playerAmount = GameData.instance.nationRanks.nation_xp_history;
        }  else if (cur_node.InnerText.Equals("ranks_nation_xp_monthly")) {
            list_type = GameData.RanksListType.NATION_XP_MONTHLY;
            title = LocalizationManager.GetTranslation("Connect Panel/ranks_nation_xp") + " " + CultureInfo.CurrentCulture.DateTimeFormat.GetMonthName(DateTime.Now.Month);
            playerRankType = PlayerRankType.Nation;
            playerAmount = GameData.instance.nationRanks.nation_xp_monthly;
        }  else if (cur_node.InnerText.Equals("ranks_user_xp")) {
            list_type = GameData.RanksListType.USER_XP;
            title = LocalizationManager.GetTranslation("Connect Panel/ranks_user_xp") + " " + LocalizationManager.GetTranslation("Connect Panel/all_time"); // "Top XP Earning Players: All-Time";
            playerRankType = PlayerRankType.User;
            playerAmount = GameData.instance.userRanks.user_xp;
        }  else if (cur_node.InnerText.Equals("ranks_user_xp_monthly")) {
            list_type = GameData.RanksListType.USER_XP_MONTHLY;
            title = LocalizationManager.GetTranslation("Connect Panel/ranks_user_xp") + " " + CultureInfo.CurrentCulture.DateTimeFormat.GetMonthName(DateTime.Now.Month);
            playerRankType = PlayerRankType.User;
            playerAmount = GameData.instance.userRanks.user_xp_monthly;
        }  else if (cur_node.InnerText.Equals("ranks_user_followers")) {
            list_type = GameData.RanksListType.USER_FOLLOWERS;
            title = LocalizationManager.GetTranslation("Connect Panel/ranks_user_followers") + " " + LocalizationManager.GetTranslation("Connect Panel/all_time"); // "Top XP Earning Players: All-Time";
            playerRankType = PlayerRankType.User;
            playerAmount = GameData.instance.userRanks.user_followers;
        }  else if (cur_node.InnerText.Equals("ranks_user_followers_monthly")) {
            list_type = GameData.RanksListType.USER_FOLLOWERS_MONTHLY;
            title = LocalizationManager.GetTranslation("Connect Panel/ranks_user_followers") + " " + CultureInfo.CurrentCulture.DateTimeFormat.GetMonthName(DateTime.Now.Month);
            playerRankType = PlayerRankType.User;
            playerAmount = GameData.instance.userRanks.user_followers_monthly;
        }  else if (cur_node.InnerText.Equals("ranks_nation_winnings")) {
            list_type = GameData.RanksListType.NATION_WINNINGS;
            title = LocalizationManager.GetTranslation("Connect Panel/ranks_nation_winnings") + " " + LocalizationManager.GetTranslation("Connect Panel/all_time"); // "Top Nations in Orb Winnings: All-Time";
            playerRankType = PlayerRankType.Nation;
            format = RankLine.RankLineFormat.MONEY;
            playerAmount = GameData.instance.nationRanks.prize_money_history;
        }  else if (cur_node.InnerText.Equals("ranks_nation_winnings_monthly")) {
            list_type = GameData.RanksListType.NATION_WINNINGS_MONTHLY;
            title = LocalizationManager.GetTranslation("Connect Panel/ranks_nation_winnings") + " " + CultureInfo.CurrentCulture.DateTimeFormat.GetMonthName(DateTime.Now.Month);
            playerRankType = PlayerRankType.Nation;
            format = RankLine.RankLineFormat.MONEY;
            playerAmount = GameData.instance.nationRanks.prize_money_history_monthly;
        }  else if (cur_node.InnerText.Equals("ranks_nation_medals")) {
            list_type = GameData.RanksListType.NATION_MEDALS;
            title = LocalizationManager.GetTranslation("Connect Panel/ranks_nation_medals") + " " + LocalizationManager.GetTranslation("Connect Panel/all_time"); // "Most Medals: All-Time";
            playerRankType = PlayerRankType.Nation;
            playerAmount = GameData.instance.nationRanks.medals_history;
        }  else if (cur_node.InnerText.Equals("ranks_nation_medals_monthly")) {
            list_type = GameData.RanksListType.NATION_MEDALS_MONTHLY;
            title = LocalizationManager.GetTranslation("Connect Panel/ranks_nation_medals") + " " + CultureInfo.CurrentCulture.DateTimeFormat.GetMonthName(DateTime.Now.Month);
            playerRankType = PlayerRankType.Nation;
            playerAmount = GameData.instance.nationRanks.medals_history_monthly;
        }  else if (cur_node.InnerText.Equals("ranks_nation_raid_earnings")) {
            list_type = GameData.RanksListType.NATION_RAID_EARNINGS;
            title = LocalizationManager.GetTranslation("Connect Panel/ranks_nation_raid_earnings") + " " + LocalizationManager.GetTranslation("Connect Panel/all_time"); // "Top Nations in Raid Earnings: All-Time";
            playerRankType = PlayerRankType.Nation;
            playerAmount = GameData.instance.nationRanks.raid_earnings_history;
        }  else if (cur_node.InnerText.Equals("ranks_nation_raid_earnings_monthly")) {
            list_type = GameData.RanksListType.NATION_RAID_EARNINGS_MONTHLY;
            title = LocalizationManager.GetTranslation("Connect Panel/ranks_nation_raid_earnings") + " " + CultureInfo.CurrentCulture.DateTimeFormat.GetMonthName(DateTime.Now.Month);
            playerRankType = PlayerRankType.Nation;
            playerAmount = GameData.instance.nationRanks.raid_earnings_history_monthly;
        }  else if (cur_node.InnerText.Equals("ranks_nation_orb_shard_earnings")) {
            list_type = GameData.RanksListType.NATION_ORB_SHARD_EARNINGS;
            title = LocalizationManager.GetTranslation("Connect Panel/ranks_nation_orb_shard_earnings") + " " + LocalizationManager.GetTranslation("Connect Panel/all_time"); // "Top Nations in Orb Shard Earnings: All-Time";
            playerRankType = PlayerRankType.Nation;
            playerAmount = GameData.instance.nationRanks.orb_shard_earnings_history;
        }  else if (cur_node.InnerText.Equals("ranks_nation_orb_shard_earnings_monthly")) {
            list_type = GameData.RanksListType.NATION_ORB_SHARD_EARNINGS;
            title = LocalizationManager.GetTranslation("Connect Panel/ranks_nation_orb_shard_earnings") + " " + CultureInfo.CurrentCulture.DateTimeFormat.GetMonthName(DateTime.Now.Month);
            playerRankType = PlayerRankType.Nation;
            playerAmount = GameData.instance.nationRanks.orb_shard_earnings_history_monthly;
        }  else if (cur_node.InnerText.Equals("ranks_nation_latest_tournament")) {
            list_type = GameData.RanksListType.NATION_LATEST_TOURNAMENT;
            title = LocalizationManager.GetTranslation("Connect Panel/ranks_nation_latest_tournament"); // "Top Nations in the Latest Tournament";
            playerRankType = PlayerRankType.Nation;
            playerAmount = 0;
        }  else if (cur_node.InnerText.Equals("ranks_nation_tournament_trophies")) {
            list_type = GameData.RanksListType.NATION_TOURNAMENT_TROPHIES;
            title = LocalizationManager.GetTranslation("Connect Panel/ranks_nation_tournament_trophies") + " " + LocalizationManager.GetTranslation("Connect Panel/all_time"); // "Top Nations in Tournament Trophies: All-Time";
            playerRankType = PlayerRankType.Nation;
            playerAmount = GameData.instance.nationRanks.tournament_trophies_history;
        }  else if (cur_node.InnerText.Equals("ranks_nation_tournament_trophies_monthly")) {
            list_type = GameData.RanksListType.NATION_TOURNAMENT_TROPHIES_MONTHLY;
            title = LocalizationManager.GetTranslation("Connect Panel/ranks_nation_tournament_trophies") + " " + CultureInfo.CurrentCulture.DateTimeFormat.GetMonthName(DateTime.Now.Month);
            playerRankType = PlayerRankType.Nation;
            playerAmount = GameData.instance.nationRanks.tournament_trophies_history_monthly;
        }  else if (cur_node.InnerText.Equals("ranks_nation_level")) {
            list_type = GameData.RanksListType.NATION_LEVEL;
            title = LocalizationManager.GetTranslation("Connect Panel/ranks_nation_level") + " " + LocalizationManager.GetTranslation("Connect Panel/all_time"); // "Highest Level Nations: All-Time";
            playerRankType = PlayerRankType.Nation;
            playerAmount = GameData.instance.nationRanks.level_history;
        }  else if (cur_node.InnerText.Equals("ranks_nation_rebirths")) {
            list_type = GameData.RanksListType.NATION_REBIRTHS;
            title = LocalizationManager.GetTranslation("Connect Panel/ranks_nation_rebirths") + " " + LocalizationManager.GetTranslation("Connect Panel/all_time"); // "Top Nations in Number of Rebirths: All-Time";
            playerRankType = PlayerRankType.Nation;
            playerAmount = GameData.instance.nationRanks.rebirth_count;
        }  else if (cur_node.InnerText.Equals("ranks_nation_quests")) {
            list_type = GameData.RanksListType.NATION_QUESTS;
            title = LocalizationManager.GetTranslation("Connect Panel/ranks_nation_quests") + " " + LocalizationManager.GetTranslation("Connect Panel/all_time"); // "Top Nations in Number of Completed Quests: All-Time";
            playerRankType = PlayerRankType.Nation;
            playerAmount = GameData.instance.nationRanks.quests_completed;
        }  else if (cur_node.InnerText.Equals("ranks_nation_quests_monthly")) {
            list_type = GameData.RanksListType.NATION_QUESTS_MONTHLY;
            title = LocalizationManager.GetTranslation("Connect Panel/ranks_nation_quests") + " " + CultureInfo.CurrentCulture.DateTimeFormat.GetMonthName(DateTime.Now.Month);
            playerRankType = PlayerRankType.Nation;
            playerAmount = GameData.instance.nationRanks.quests_completed_monthly;
        }  else if (cur_node.InnerText.Equals("ranks_nation_energy_donated")) {
            list_type = GameData.RanksListType.NATION_ENERGY_DONATED;
            title = LocalizationManager.GetTranslation("Connect Panel/ranks_nation_energy_donated") + " " + LocalizationManager.GetTranslation("Connect Panel/all_time"); // "Top Nations Donating Energy to Allies: All-Time";
            playerRankType = PlayerRankType.Nation;
            playerAmount = GameData.instance.nationRanks.donated_energy_history;
        }  else if (cur_node.InnerText.Equals("ranks_nation_energy_donated_monthly")) {
            list_type = GameData.RanksListType.NATION_ENERGY_DONATED_MONTHLY;
            title = LocalizationManager.GetTranslation("Connect Panel/ranks_nation_energy_donated") + " " + CultureInfo.CurrentCulture.DateTimeFormat.GetMonthName(DateTime.Now.Month);
            playerRankType = PlayerRankType.Nation;
            playerAmount = GameData.instance.nationRanks.donated_energy_history_monthly;
        }  else if (cur_node.InnerText.Equals("ranks_nation_manpower_donated")) {
            list_type = GameData.RanksListType.NATION_MANPOWER_DONATED;
            title = LocalizationManager.GetTranslation("Connect Panel/ranks_nation_manpower_donated") + " " + LocalizationManager.GetTranslation("Connect Panel/all_time"); // "Top Nations Donating Manpower to Allies: All-Time";
            playerRankType = PlayerRankType.Nation;
            playerAmount = GameData.instance.nationRanks.donated_manpower_history;
        }  else if (cur_node.InnerText.Equals("ranks_nation_manpower_donated_monthly")) {
            list_type = GameData.RanksListType.NATION_MANPOWER_DONATED_MONTHLY;
            title = LocalizationManager.GetTranslation("Connect Panel/ranks_nation_manpower_donated") + " " + CultureInfo.CurrentCulture.DateTimeFormat.GetMonthName(DateTime.Now.Month);
            playerRankType = PlayerRankType.Nation;
            playerAmount = GameData.instance.nationRanks.donated_manpower_history_monthly;
        }  else if (cur_node.InnerText.Equals("ranks_nation_area")) {
            list_type = GameData.RanksListType.NATION_AREA;
            title = LocalizationManager.GetTranslation("Connect Panel/ranks_nation_area") + " " + LocalizationManager.GetTranslation("Connect Panel/all_time"); // "Largest Nations: All-Time";
            playerRankType = PlayerRankType.Nation;
            playerAmount = GameData.instance.nationRanks.max_area;
        }  else if (cur_node.InnerText.Equals("ranks_nation_area_monthly")) {
            list_type = GameData.RanksListType.NATION_AREA_MONTHLY;
            title = LocalizationManager.GetTranslation("Connect Panel/ranks_nation_area") + " " + CultureInfo.CurrentCulture.DateTimeFormat.GetMonthName(DateTime.Now.Month);
            playerRankType = PlayerRankType.Nation;
            playerAmount = GameData.instance.nationRanks.max_area_monthly;
        }  else if (cur_node.InnerText.Equals("ranks_nation_captures")) {
            list_type = GameData.RanksListType.NATION_CAPTURES;
            title = LocalizationManager.GetTranslation("Connect Panel/ranks_nation_captures") + " " + LocalizationManager.GetTranslation("Connect Panel/all_time"); // "Nations that have Captured the Most Land: All-Time";
            playerRankType = PlayerRankType.Nation;
            playerAmount = GameData.instance.nationRanks.captures_history;
        }  else if (cur_node.InnerText.Equals("ranks_nation_captures_monthly")) {
            list_type = GameData.RanksListType.NATION_CAPTURES_MONTHLY;
            title = LocalizationManager.GetTranslation("Connect Panel/ranks_nation_captures") + " " + CultureInfo.CurrentCulture.DateTimeFormat.GetMonthName(DateTime.Now.Month);
            playerRankType = PlayerRankType.Nation;
            playerAmount = GameData.instance.nationRanks.captures_history_monthly;
        } else if (cur_node.InnerText.Equals("ranks_nation_tournament_current")) {
            list_type = GameData.RanksListType.NATION_TOURNAMENT_CURRENT;
            title = LocalizationManager.GetTranslation("Tournament/ranks_nation_tournament_current");
            playerRankType = PlayerRankType.Nation;
            playerAmount = GameData.instance.nationRanks.tournament_current;
        }

        // Determine the ideal number of generic and specific (player and contacts) ranks for this list.
        int ideal_num_ranks, ideal_num_generic_ranks, ideal_num_specific_ranks, num_generic_ranks = 0;
        if (list_type == GameData.RanksListType.ORB_WINNINGS)
        {
            ideal_num_ranks = 15;
            ideal_num_generic_ranks = 10;
            ideal_num_specific_ranks = 5;
        }
        else if (ranks_list == GameData.RanksListType.COMBINED)
        {
            ideal_num_ranks = 10;
            ideal_num_generic_ranks = 4;
            ideal_num_specific_ranks = 6;
        }
        else if (list_type == GameData.RanksListType.NATION_TOURNAMENT_CURRENT)
        {
            ideal_num_ranks = 1000;
            ideal_num_generic_ranks = 950;
            ideal_num_specific_ranks = 50;
        }
        else 
        {
            ideal_num_ranks = 100;
            ideal_num_generic_ranks = 50;
            ideal_num_specific_ranks = 50;
        }

        // Fill the specific_ranks map with the ranks for this list that belong to the player and their contacts.

        Dictionary<int, RankEntryRecord> specific_ranks = new Dictionary<int, RankEntryRecord>();

        if (list_type == GameData.RanksListType.ORB_WINNINGS)
        {
            // Add an entry to the specific_ranks table for each contact ranking for this orb.
            foreach (OrbRanksRecord cur_orb_rank in GameData.instance.contactOrbRanks) {
                specific_ranks.Add(cur_orb_rank.nationID, new RankEntryRecord(cur_orb_rank.nationID, cur_orb_rank.nation_name, orb_winnings_monthly ? cur_orb_rank.orb_winnings_monthly : cur_orb_rank.orb_winnings, true, RankEntryRecord.RankEntryType.CONTACT));
            }

            // Add an entry to the specific_ranks table for the player's rank for this orb, if it is > 0.
            if (playerAmount > 0) {
                specific_ranks.Add(GameData.instance.nationID, new RankEntryRecord(GameData.instance.nationID, GameData.instance.nationName, playerAmount, true, RankEntryRecord.RankEntryType.PLAYER));
            }
        }
        else if (playerRankType == PlayerRankType.Nation)
        {
            // Add an entry to the specific_ranks table for each contact nation ranking in this list.
            foreach (NationRanksRecord cur_nation_rank in GameData.instance.contactNationRanks) 
            {
                int cur_rank_val = cur_nation_rank.GetValue(list_type);
                if (cur_rank_val > 0) {
                    specific_ranks.Add(cur_nation_rank.nationID, new RankEntryRecord(cur_nation_rank.nationID, cur_nation_rank.nation_name, cur_rank_val, true, RankEntryRecord.RankEntryType.CONTACT));
                }
            }

            // Add an entry to the specific_ranks table for the player's rank for this list, if it is > 0.
            if (playerAmount > 0) {
                specific_ranks.Add(GameData.instance.nationID, new RankEntryRecord(GameData.instance.nationID, GameData.instance.nationName, playerAmount, true, RankEntryRecord.RankEntryType.PLAYER));
            }
        }
        else if (playerRankType == PlayerRankType.User)
        {
            // Add an entry to the specific_ranks table for each contact user ranking in this list.
            foreach (UserRanksRecord cur_user_rank in GameData.instance.contactUserRanks) 
            {
                int cur_rank_val = cur_user_rank.GetValue(list_type);
                if (cur_rank_val > 0) {
                    specific_ranks.Add(cur_user_rank.userID, new RankEntryRecord(cur_user_rank.userID, cur_user_rank.username, cur_rank_val, true, RankEntryRecord.RankEntryType.CONTACT));
                }
            }

            // Add an entry to the specific_ranks table for the player's rank for this list, if it is > 0.
            if (playerAmount > 0) {
                specific_ranks.Add(GameData.instance.userID, new RankEntryRecord(GameData.instance.userID, GameData.instance.username, playerAmount, true, RankEntryRecord.RankEntryType.PLAYER));
            }
        }

        // Add the appropriate generic ranks (and specific ranks that are also generic ranks) to the RankEntryList.

        int ID, amount;
        int num_sequential_ranks = 0;
        string name;
        bool active;
        List<RankEntryRecord> rankEntryList = new List<RankEntryRecord>();

        //Debug.Log("playerAmount: " + playerAmount + ", specific_ranks.Count: " + specific_ranks.Count);

		foreach (XmlNode node in nodes) 
        {
            ID = Int32.Parse (node.Attributes["ID"].Value);
            amount = Int32.Parse (node.Attributes["amount"].Value);
			name = node.Attributes["name"].Value;
            active = (node.Attributes["active"] == null) ? true : node.Attributes["active"].Value.Equals("true");
           
            if (specific_ranks.ContainsKey(ID))
            {
                // The current generic rank is also a specific rank.
                // Move the current specific rank out of the specific_ranks table and into the RankEntryList.
                //Debug.Log("Moving specific rank for " + specific_ranks[ID].name + " to rankEntryList");
                specific_ranks[ID].active = active;
                rankEntryList.Add(specific_ranks[ID]);
                specific_ranks.Remove(ID);
            }
            else
            {
                // Add a RankEntryRecord representing the current generic rank to the RankEntryList, and increment the count of the number of generic ranks.
                rankEntryList.Add(new RankEntryRecord(ID, name, amount, active, RankEntryRecord.RankEntryType.GENERIC));
                //Debug.Log("Added generic rank for " + name + " to rankEntryList");
                num_generic_ranks++;
            }

            // Keep track of the number of ranks in sequence in this list, so their index number will be shown. Those after the sequence will not show an index number (because it would be incorrect).
            num_sequential_ranks++;

            // Do not add anymore generic ranks if either:
            // 1) We've reached the ideal num_generic_ranks and num_generic_ranks + specific_ranks.size() >= ideal_num_ranks, or
            // 2) We've exceeded the ideal num_generic_ranks and num_generic_ranks + specific_ranks.size() == the ideal list length.
            if (((num_generic_ranks == ideal_num_generic_ranks) && ((num_generic_ranks + specific_ranks.Count) >= ideal_num_ranks)) ||
                ((num_generic_ranks > ideal_num_generic_ranks) && ((num_generic_ranks + specific_ranks.Count) == ideal_num_ranks)))
            {
                break;
            }
   		}

        // Add the appropriate specific ranks to the rankEntryList

        int space_remaining_in_list = (ideal_num_ranks - rankEntryList.Count);
        if (specific_ranks.Count > space_remaining_in_list)
        {
            // There are too many ranks in the specific_ranks table to move them all to the rankEntryList. Move only the allowed number, that are closest in score value to the player.

            List<RankEntryRecord> specificRankEntryList = new List<RankEntryRecord>();

            // Move all rank entries from the specific_ranks table to the specificRankEntryList.
            foreach(RankEntryRecord rank_entry in specific_ranks.Values) {
                specificRankEntryList.Add(rank_entry);
            }

            specific_ranks.Clear();

            // Sort the list in order of least to greatest difference from player's score value.
            specificRankEntryList.Sort((x, y) => Math.Abs(x.value - playerAmount) - Math.Abs(y.value - playerAmount));

            // Copy to the rankEntryList only those first n specific ranks, closest in score value to the player, so as to reach the ideal number of ranks. 
            for (int i = 0; i < space_remaining_in_list; i++) {
                rankEntryList.Add(specificRankEntryList[i]);
            }

            specificRankEntryList.Clear();
        }
        else
        {
            // Move all rank entries from the specific_ranks table to the rankEntryList.
            foreach(RankEntryRecord rank_entry in specific_ranks.Values) {
                rankEntryList.Add(rank_entry);
            }

            specific_ranks.Clear();
        }

        if (list_type != GameData.RanksListType.NATION_TOURNAMENT_CURRENT) // Don't sort the tournament lists; they are provided in the correct order.
        {
            // Sort the rankEntryList from highest to lowest score value.
            rankEntryList.Sort((x, y) => y.value - x.value);
        }
        
        // Add a line for this list's title to the ranks list content area.
        titleLineObject = (GameObject) Instantiate(titleLinePrefab,  new Vector3(0f, 0f, 0f), Quaternion.identity);
        titleLineObject.transform.SetParent(_listContentTransform);
        titleLineObject.transform.localScale = new Vector3(1, 1, 1);
        titleLineObject.transform.GetChild(0).GetComponent<TMPro.TextMeshProUGUI>().text = title;    

        // Add a rank line for each entry in the rankEntryList
        int index = 0;
        foreach (RankEntryRecord cur_record in rankEntryList)
        {
            // Add a line for this individual rank to the ranks list content area.
            rankLineObject = (GameObject) Instantiate(rankLinePrefab,  new Vector3(0f, 0f, 0f), Quaternion.identity);
            rankLineObject.transform.SetParent(_listContentTransform);
            rankLineObject.transform.localScale = new Vector3(1, 1, 1);
            rankLineObject.GetComponent<RankLine>().Init(cur_record, index, index < num_sequential_ranks, format);

            // Increment index
            index++;
        }

        if ((ranks_list == GameData.RanksListType.COMBINED) && (index >= 10))
        {
            // Add a "more" button line to the ranks list content area.
            moreButtonLineObject = (GameObject) Instantiate(moreLinePrefab,  new Vector3(0f, 0f, 0f), Quaternion.identity);
            moreButtonLineObject.transform.SetParent(_listContentTransform);
            moreButtonLineObject.transform.localScale = new Vector3(1, 1, 1);

            // Set up the moreButton's listener.
            Button moreButton = moreButtonLineObject.transform.GetChild(0).GetComponent<Button>();
            moreButton.onClick.RemoveAllListeners();
            moreButton.onClick.AddListener(() => MoreButtonPressed(list_type));
        }
    }

    public void ClearPatronOfferListDisplay()
    {
        // Remove any children of the patron offer list content object (after the first one, which is the header).
        while (patronOfferListContent.transform.childCount > 1)
        {
            GameObject cur_child = patronOfferListContent.transform.GetChild(1).gameObject;
            cur_child.transform.SetParent(null);
            Destroy(cur_child);
        }
    }

    public void AddPatronOffer(int _userID, String _username, int _bonusXP, int _bonusCredits, int _numFollowers)
    {
        // Add a line for this patron offer to the patron offers list content area.
        GameObject offerLineObject = (GameObject) Instantiate(patronOfferEntryPrefab,  new Vector3(0f, 0f, 0f), Quaternion.identity);
        offerLineObject.transform.SetParent(patronOfferListContent.transform);
        offerLineObject.transform.localScale = new Vector3(1, 1, 1);
        offerLineObject.GetComponent<PatronOfferEntry>().Init(_userID, _username, _bonusXP, _bonusCredits, _numFollowers);

        LayoutPatronPanel();
    }

    public void RemovePatronOffer(int _userID)
    {
        PatronOfferEntry entry;
        foreach (Transform child in patronOfferListContent.transform)
        {
            if ((entry = child.gameObject.GetComponent<PatronOfferEntry>()) != null)
            {
                if (entry.userID == _userID)
                {
                    // Remove and delete the current PatronOfferEntry object.
                    child.SetParent(null);
                    GameObject.Destroy(child.gameObject);
                    break;
                }
            }
        }

        LayoutPatronPanel();
    }

    public void MoreButtonPressed(GameData.RanksListType _list_type)
    {
        DisplayRanks(_list_type);
    }

    public void BackButtonPressed()
    {
        DisplayRanks(GameData.RanksListType.COMBINED);
    }

    public void OrbWinningsEventReceived()
    {
        if ((ranks_list == GameData.RanksListType.ORB_WINNINGS) && (ranks_data_received == false))
        {
            ranks_data_received = true;

            // If the ranks file has already been received, we have all the info we need. Compose the ranks list display now.
            if (ranks_file_received) {
                ComposeRanksListDisplay();
            }
        }
    }

    public void RanksDataEventReceived()
    {
        if ((ranks_list != GameData.RanksListType.ORB_WINNINGS) && (ranks_data_received == false))
        {
            ranks_data_received = true;

            // If the ranks file has already been received, we have all the info we need. Compose the ranks list display now.
            if (ranks_file_received) {
                ComposeRanksListDisplay();
            }
        }
    }

    #region Recruit Panel

    public void OnClick_PatronCodeInfo()
    {
        String text = LocalizationManager.GetTranslation("Connect Panel/patron_code_info"); // "When someone starts a new game and enters your patron code, they'll receive a gift of 50 credits and will join you on the same server. You will also become their patron, which means that you each will receive as a bonus up to 5% of the XP that the other earns!";
        Debug.Log("text: " + text);
        Requestor.Activate(0, 0, null, text, LocalizationManager.GetTranslation("Generic Text/okay"), "");
    }

    public void OnClick_FollowerListInfo()
    {
        String text = LocalizationManager.GetTranslation("Connect Panel/followers_info"); // "Your followers are all the players who have you as their patron. You can offer to be another player's patron by clicking on their name in chat, and selecting 'Offer to be Patron' from the chat menu. The more followers you have, the better. Make sure to help them out and keep them happy so they'll remain loyal!";
        Requestor.Activate(0, 0, null, text, LocalizationManager.GetTranslation("Generic Text/okay"), "");
    }

    public void ClearFollowerListDisplay()
    {
        // Remove any children of the follower list content object (after the first one, which is the header).
        while (followerListContent.transform.childCount > 1)
        {
            GameObject cur_child = followerListContent.transform.GetChild(1).gameObject;
            cur_child.transform.SetParent(null);
            Destroy(cur_child);
        }

        // Update the display of the number of folowers
        UpdateFollowersTitle();

        // Hide the empty follower list.
        followersList.SetActive(false);
    }

    public void AddFollower(int _userID, String _username, int _bonusXP, int _bonusCredits)
    {
        // Add a line for this follower to the follower list content area.
        GameObject followerLineObject = (GameObject) Instantiate(followerEntryPrefab,  new Vector3(0f, 0f, 0f), Quaternion.identity);
        followerLineObject.transform.SetParent(followerListContent.transform);
        followerLineObject.transform.localScale = new Vector3(1, 1, 1);
        followerLineObject.GetComponent<FollowerEntry>().Init(_userID, _username, _bonusXP, _bonusCredits);

        // Update the display of the number of folowers
        UpdateFollowersTitle();

        LayoutRecruitPanel();
    }

    public void RemoveFollower(int _userID)
    {
        FollowerEntry entry;
        foreach (Transform child in followerListContent.transform)
        {
            if ((entry = child.gameObject.GetComponent<FollowerEntry>()) != null)
            {
                if (entry.userID == _userID)
                {
                    // Remove and delete the current FollowerEntry object.
                    child.SetParent(null);
                    GameObject.Destroy(child.gameObject);
                    break;
                }
            }
        }

        // Update the display of the number of folowers
        UpdateFollowersTitle();

        LayoutRecruitPanel();
    }

    public void UpdateFollowersTitle()
    {
        // Update the display of the number of folowers
        followersTitle.text = LocalizationManager.GetTranslation("Connect Panel/followers_title") + " (" + (followerListContent.transform.childCount - 1) + ")";
    }

    public void LayoutRecruitPanel()
    {
        // Show or hide the recruitFriendsArea, depending on the platform.
#if UNITY_EDITOR || UNITY_ANDROID || UNITY_IOS
        recruitFriendsArea.SetActive(true);
#else
        recruitFriendsArea.SetActive(false);
#endif

        // Only show the Followers area if the user has follower(s).
        followersList.SetActive(followerListContent.transform.childCount > 1);
    }
    /*
    public void OnClick_FacebookPostScreenshot()
    {

    }
    */
    #endregion

    #region Patron Panel

    public void OnClick_AcceptPatronOffer(int _userID, String _username)
    {
        Requestor.Activate((int)RequestorTask.PatronOfferAccept, _userID, this, LocalizationManager.GetTranslation("Connect Panel/patron_offer_accept").Replace("{username}", _username), //"Do you choose to accept " + _username + " as your new patron?",
            LocalizationManager.GetTranslation("Generic Text/yes_word"), LocalizationManager.GetTranslation("Generic Text/no_word"));
    }

    public void OnClick_DeclinePatronOffer(int _userID, String _username)
    {
        Requestor.Activate((int)RequestorTask.PatronOfferDecline, _userID, this, LocalizationManager.GetTranslation("Connect Panel/patron_offer_decline").Replace("{username}", _username), //"Do you decline " + _username + "'s offer to be your patron?",
            LocalizationManager.GetTranslation("Generic Text/yes_word"), LocalizationManager.GetTranslation("Generic Text/no_word"));
    }

    public void OnClick_PatronInfo()
    {
        String text = LocalizationManager.GetTranslation("Connect Panel/patron_info"); //  "You receive as a bonus 2% of the XP your patron earns, and 10% of any credits they buy! It benefits you both - your patron receives the same bonus for each of their followers.";
        Requestor.Activate(0, 0, null, text, LocalizationManager.GetTranslation("Generic Text/okay"), "");
    }

    public void OnClick_ChoosePatronInfo()
    {
        String text = LocalizationManager.GetTranslation("Connect Panel/choose_patron_info"); //  "Having a patron benefits you both - you each receive a bonus of 2% of the XP the other earns, and 10% of any credits they buy. When choosing a patron, look at the different ways that you can help each other out.";
        Requestor.Activate(0, 0, null, text, LocalizationManager.GetTranslation("Generic Text/okay"), "");
    }

    public void PatronInfoReceived()
    {
        if (GameData.instance.patronID != -1)
        {
            patronInfoTitle.text = LocalizationManager.GetTranslation("Connect Panel/patron_title").Replace("{username}", GameData.instance.username) + " " + GameData.instance.patronUsername;
            patronLastMonthBonusXP.text = string.Format("{0:n0}", (int)GameData.instance.patron_prev_month_patron_bonus_XP);
            patronLastMonthBonusCredits.text = string.Format("{0:n0}", (int)GameData.instance.patron_prev_month_patron_bonus_credits);
            patronTotalBonusXP.text = string.Format("{0:n0}", (int)GameData.instance.total_patron_xp_received);
            patronTotalBonusCredits.text = string.Format("{0:n0}", (int)GameData.instance.total_patron_credits_received);
            choosePatronTitle.text = LocalizationManager.GetTranslation("Connect Panel/choose_different_patron");
        }
        else
        {
            choosePatronTitle.text = LocalizationManager.GetTranslation("Connect Panel/choose_patron");
        }

        // Display the user's patron code.
        patronCodeTitle.text = LocalizationManager.GetTranslation("Connect Panel/patron_code_title").Replace("{username}", GameData.instance.username) + " " + GameData.instance.patronCode;

        // Display the patron code description text.
        patronCodeDesc.text = LocalizationManager.GetTranslation("Connect Panel/patron_code_desc");

        if ((GameData.instance.prev_month_patron_bonus_credits > 0) || (GameData.instance.prev_month_patron_bonus_XP > 0)) {
            patronCodeDesc.text += LocalizationManager.GetTranslation("Connect Panel/last_month_rewards").Replace("{xp}", string.Format("{0:n0}", (int)GameData.instance.prev_month_patron_bonus_XP)).Replace("{credits}", string.Format("{0:n0}", (int)GameData.instance.prev_month_patron_bonus_credits)); //  "Last month, each of your followers would have received " + string.Format("{0:n0}", (int)GameData.instance.prev_month_patron_bonus_XP) + " XP and " + string.Format("{0:n0}", (int)GameData.instance.prev_month_patron_bonus_credits) + " credits.";
        }

        LayoutPatronPanel();
    }

    public void LayoutPatronPanel()
    {
        // Determine which areas of the Patron panel to display, based on whether the user has a patron, and whether that have any patron offers.
        noPatronMessage.SetActive(GameData.instance.patronID == -1);
        patronInfoArea.SetActive(GameData.instance.patronID != -1);
        noInvitesMessage.SetActive((GameData.instance.patronID != -1) && (patronOfferListContent.transform.childCount <= 1));
        choosePatronArea.SetActive(patronOfferListContent.transform.childCount > 1);
    }

    #endregion

    #region News Panel

    public void FetchNews()
    {
        //Debug.Log("FetchNews()");
        StartCoroutine(LoadNews_Coroutine());
    }

    public IEnumerator LoadNews_Coroutine()
    {
        //Debug.Log("LoadNews_Coroutine()");

        string filename = "";

        // Reset to start of html.
        html_pos = 0;

        // Clear the contents of the news display.
        ClearNews();

        // Wait one frame so that the client's language will have been determined.
        yield return null;

        // NOTE: Html files using non-latin character set should be encoded in UTF-8 format.

        switch (GameGUI.instance.curLanguage)
        {
            case GameGUI.Languages.CHINESE: filename = "news_Chinese.htm"; break;
            case GameGUI.Languages.ENGLISH: filename = "news_English.htm"; break;
            case GameGUI.Languages.FRENCH: filename = "news_French.htm"; break;
            case GameGUI.Languages.GERMAN: filename = "news_German.htm"; break;
            case GameGUI.Languages.ITALIAN: filename = "news_Italian.htm"; break;
            case GameGUI.Languages.JAPANESE: filename = "news_Japanese.htm"; break;
            case GameGUI.Languages.KOREAN: filename = "news_Korean.htm"; break;
            case GameGUI.Languages.PORTUGESE: filename = "news_Portugese.htm"; break;
            case GameGUI.Languages.RUSSIAN: filename = "news_Russian.htm"; break;
            case GameGUI.Languages.SPANISH: filename = "news_Spanish.htm"; break;
        }

        // Attempt to load the news contents in the client's language.
        string news_url = NEWS_URL_DIR + filename;
        UnityWebRequest www = UnityWebRequest.Get(news_url);
		yield return Network.instance.RequestFromWeb(www);

        if (!www.isNetworkError && !www.isHttpError)
        {
            // Store the news HTML string.
            news_html = www.downloadHandler.text;

            // The news content has been loaded; display it and exit the coroutine.
            html_pos = ParseNews(html_pos, html_pos + NEWS_PAGE_LENGTH);

            yield break;
        }

        // Could not load the news content in the client's language; try loading it in English instead.
        Debug.Log("Failed to load news in client language: " + news_url + " Error: " + www.error);
        filename = "news_English.htm";

        // Attempt to load the news contents in English.
        news_url = NEWS_URL_DIR + filename;
        www = UnityWebRequest.Get(news_url);
		yield return Network.instance.RequestFromWeb(www);

        if (!www.isNetworkError && !www.isHttpError)
        {
            // Store the news HTML string.
            news_html = www.downloadHandler.text;

            // The news content has been loaded; display it and exit the coroutine.
            html_pos = ParseNews(html_pos, html_pos + NEWS_PAGE_LENGTH);
            yield break;
        }

        Debug.Log("Failed to load news in English: " + news_url + " Error: " + www.error);
    }

    public void ClearNews()
    {
        //Debug.Log("ClearNews()");

        // Clear the lists of links and text blocks.
        links.Clear();
        text_blocks.Clear();

         // Clear the current contents of the news display. (It's important to use foreach to do this.)
        foreach (Transform child in newsContent.transform)
        {
            Destroy(child.gameObject);
        }
    }

    public int ParseNews(int _start_pos, int _end_pos)
    {
        int block_end_pos = 0, cur_pos = _start_pos;

        //Debug.Log("ParseNews() news_html: " + news_html);

        while ((block_end_pos < news_html.Length) && (block_end_pos < _end_pos))
        {
            block_end_pos = DetermineBlockEndPos(news_html, cur_pos);
            //Debug.Log("ParseNews() block " + cur_pos + " to " + block_end_pos);

            // Exit loop if we've reached the end of the file.
            if (block_end_pos == cur_pos) {
                break;
            }

            if (news_html.IndexOf("<img", cur_pos) == cur_pos)
            {
                //Debug.Log("Image block: " + news_html.Substring(cur_pos, block_end_pos - cur_pos));

                // Add this image block
                GameObject block = (GameObject)Instantiate(newsImagePrefab, new Vector3(0f, 0f, 0f), Quaternion.identity);
                block.transform.SetParent(newsContent.transform);
                block.transform.localScale = new Vector3(1, 1, 1);

                // Isolate the image url
                int url_start_pos = news_html.IndexOf("src=\"", cur_pos) + 5;
                int url_end_pos = news_html.IndexOf("\"", url_start_pos);
                string image_url = news_html.Substring(url_start_pos, url_end_pos - url_start_pos);

                // Load the image
                StartCoroutine(LoadImage(image_url, block, newsContent.GetComponent<RectTransform>().rect.width / 2));
            }
            else if (news_html.IndexOf("<li>", cur_pos) == cur_pos)
            {
                //Debug.Log("List item block: " + news_html.Substring(cur_pos, block_end_pos - cur_pos));

                // Add this list item block
                GameObject block = (GameObject)Instantiate(newsListItemPrefab, new Vector3(0f, 0f, 0f), Quaternion.identity);
                block.transform.SetParent(newsContent.transform);
                block.transform.localScale = new Vector3(1, 1, 1);
                block.transform.GetChild(1).GetComponent<TMPro.TextMeshProUGUI>().text = FormatNewsText(news_html.Substring(cur_pos, block_end_pos - cur_pos)).Trim();
                text_blocks.Add(block.transform.GetChild(1).GetComponent<TMPro.TextMeshProUGUI>());
            }
            else if (news_html.IndexOf("<hr>", cur_pos) == cur_pos)
            {
                //Debug.Log("Horizontal rule block: " + news_html.Substring(cur_pos, block_end_pos - cur_pos));

                // Add this horizontal rule block
                GameObject block = (GameObject)Instantiate(newsHRulePrefab, new Vector3(0f, 0f, 0f), Quaternion.identity);
                block.transform.SetParent(newsContent.transform);
                block.transform.localScale = new Vector3(1, 1, 1);
            }
            else
            {
                //Debug.Log("Normal block: " + news_html.Substring(cur_pos, block_end_pos - cur_pos));

                // Add this normal block
                GameObject block = (GameObject)Instantiate(newsTextPrefab, new Vector3(0f, 0f, 0f), Quaternion.identity);
                block.transform.SetParent(newsContent.transform);
                block.transform.localScale = new Vector3(1, 1, 1);
                block.transform.GetComponent<TMPro.TextMeshProUGUI>().text = FormatNewsText(news_html.Substring(cur_pos, block_end_pos - cur_pos));
                text_blocks.Add(block.transform.GetComponent<TMPro.TextMeshProUGUI>());
            }

            // Advance to start of next block.
            cur_pos = block_end_pos;
        }

        return cur_pos;
    }

    public int DetermineBlockEndPos(string _html, int _start_pos)
    {
        int index;
        int result = _html.Length;

        index = _html.IndexOf("<img", _start_pos);
        if (index != -1)
        {
            if (index == _start_pos) {
                // Img block
                return _html.IndexOf(">", index) + 1;
            } else {
                // Normal block
                result = Math.Min(result, index);
            }
        }

        index = _html.IndexOf("<li>", _start_pos);
        if (index != -1) {
            if (index == _start_pos) {
                // List item block
                return _html.IndexOf("\n", index) + 1;
            } else {
                // Normal block
                result = Math.Min(result, index);
            }
            result = Math.Min(result, index);
        }

        index = _html.IndexOf("<hr", _start_pos);
        if (index != -1) {
            if (index == _start_pos) {
                // Horizontal rule block
                return _html.IndexOf(">", index) + 1;
            } else {
                // Normal block
                result = Math.Min(result, index);
            }
            result = Math.Min(result, index);
        }

        return result;
    }

    public string FormatNewsText(string _html)
    {
        //Debug.Log("FormatNewsText()");

        int cur_pos = 0;

        while ((cur_pos = _html.IndexOf("<a", cur_pos)) != -1)
        {
            int tag_end_pos = _html.IndexOf(">", cur_pos) + 1;
            int url_start_pos = _html.IndexOf("href=\"", cur_pos) + 6;
            int url_end_pos = _html.IndexOf("\"", url_start_pos);

            // Record the link url
            string url = _html.Substring(url_start_pos, url_end_pos - url_start_pos);
            links.Add(url);
            //Debug.Log("News url " + (links.Count - 1) + ": " + url);

            // Replace the link open tag.
            _html = _html.Substring(0, cur_pos) + "<link=\"" + (links.Count - 1) + "\"><u><color=#ECFF36FF>" + _html.Substring(tag_end_pos);

            // Replace the link close tag.
            int close_tag_pos = _html.IndexOf("</a>", cur_pos);
            _html = _html.Substring(0, close_tag_pos) + "</color></u></link>" + _html.Substring(close_tag_pos + 4);
        }

        return _html.Replace("<h1>", "<font=\"trajanpro-bold SDF\"><size=25>")
                    .Replace("</h1>", "</font></size>")
                    .Replace("<h2>", "<font=\"trajanpro-bold SDF\"><size=18>")
                    .Replace("</h2>", "</font></size>")
                    .Replace("<ul>", "")
                    .Replace("</ul>", "")
                    .Replace("<li>", "")
                    .Replace("</br>", "");
    }

    public IEnumerator LoadImage(string _url, GameObject _block, float _max_width)
    {
        //Debug.Log("LoadImage() url: " + _url);

	    UnityWebRequest www = UnityWebRequestTexture.GetTexture(_url);
		yield return Network.instance.RequestFromWeb(www);

		if (!www.isNetworkError && !www.isHttpError)
        {
			Texture2D texture = ((DownloadHandlerTexture)www.downloadHandler).texture;
            int final_width = (int)Math.Min(texture.width, _max_width);
            int final_height = final_width * texture.height / texture.width;
            _block.transform.GetChild(0).gameObject.GetComponent<Image>().sprite = Sprite.Create(texture, new Rect(0, 0, texture.width, texture.height), new Vector2(0, 0));
            _block.transform.GetChild(0).gameObject.GetComponent<LayoutElement>().preferredWidth = final_width;
            _block.transform.GetChild(0).gameObject.GetComponent<LayoutElement>().preferredHeight = final_height;
        }
        else
        {
            Debug.Log("LoadImage() www error: " + www.error);
        }
    }

    public void OnPointerDown(PointerEventData _eventData)
    {
        if (newsContentArea.activeSelf == false) {
            return;
        }

        foreach (TMPro.TextMeshProUGUI text_block in text_blocks)
        {
            // Determine whether link text has been clicked.
            int link_index = TMPro.TMP_TextUtilities.FindIntersectingLink(text_block, Input.mousePosition, null);

            if (link_index != -1)
            {
                int link_id = System.Convert.ToInt32(text_block.textInfo.linkInfo[link_index].GetLinkID());
                //Debug.Log("OnPointerDown() link_index: " + link_index + ", link_id: " + link_id);
                Application.OpenURL(links[link_id]);
            }
        }
    }

    public void NewsScroll_OnValueChanged(Vector2 value)
    {
        if ((value.y < 0.001f) && (newsScrollbar.gameObject.activeSelf) && (html_pos < news_html.Length))
        {
            // Display another page of the news html.
            html_pos = ParseNews(html_pos, html_pos + NEWS_PAGE_LENGTH);
        }
    }

    public void OnClick_WebSiteButton()
    {
        Application.OpenURL("https://warofconquest.com");
    }

    public void OnClick_ForumButton()
    {
        Application.OpenURL("https://warofconquest.com/forum/");
    }

    public void OnClick_FacebookButton()
    {
        Application.OpenURL("https://www.facebook.com/warofconquestgame/");
    }

    public void OnClick_TwitterButton()
    {
        Application.OpenURL("https://twitter.com/ironzog");
    }

    public void OnClick_RemoveFollower(int _followerID, string _follower_username)
    {
        Requestor.Activate((int)RequestorTask.RemoveFollower, _followerID, this, LocalizationManager.GetTranslation("Connect Panel/remove_follower_confirm").Replace("{username}", _follower_username), // "Do you want to remove {username} from your list of followers?",
            LocalizationManager.GetTranslation("Generic Text/yes_word"), LocalizationManager.GetTranslation("Generic Text/no_word"));
    }

    #endregion

    public void RequestorResponse(int _task, int _data, Requestor.RequestorButton _result)
    {
        if (_result == Requestor.RequestorButton.LeftButton)
        {
            if ((RequestorTask)_task == RequestorTask.PatronOfferAccept)
            {
                // Send patron_offer_accept event to the server.
                Network.instance.SendCommand("action=patron_offer_accept|targetUserID=" + _data);
            }
            else if ((RequestorTask)_task == RequestorTask.PatronOfferDecline)
            {
                // Send patron_offer_decline event to the server.
                Network.instance.SendCommand("action=patron_offer_decline|targetUserID=" + _data);
            }
            else if ((RequestorTask)_task == RequestorTask.RemoveFollower)
            {
                // Send remove_follower event to the server.
                Network.instance.SendCommand("action=remove_follower|targetUserID=" + _data);
            }
        }
    }
}

public class RankEntryRecord
{
    public enum RankEntryType
    {
        GENERIC,
        CONTACT,
        PLAYER
    }

    public RankEntryRecord(int _ID, string _name, int _value, bool _active, RankEntryType _type)
    {
        ID = _ID;
        name = _name;
        value = _value;
        active = _active;
        type = _type;
    }

    public int ID, value;
    public string name;
    public RankEntryType type;
    public bool active;
}
