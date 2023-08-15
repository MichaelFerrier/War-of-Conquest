using UnityEngine;
using UnityEngine.UI;
using UnityEngine.Networking;
using UnityEngine.EventSystems;
using System.Collections;
using System.Collections.Generic;
using System.Xml;
using System;
using System.Globalization;
using I2.Loc;

public class TournamentPanel : MonoBehaviour, RequestorListener
{
    private enum RequestorTask
    {
        JoinTournament
    };

    public static TournamentPanel instance;

    public Sprite inactivePanelTabSprite, activePanelTabSprite;
    public GameObject infoPanelTab, ranksPanelTab;
    public GameObject infoContentArea, ranksContentArea;
    public GameObject ranksListContent;

    // Info subpanel
    public GameObject joinArea, statusArea;
    public TMPro.TextMeshProUGUI joinDescText, statusText;
    
    // Ranks subpanel
    public GameObject titleLinePrefab, rankLinePrefab, moreLinePrefab;
    public GameObject progressRing;
    
    Color tabTextColor_Inctive = new Color(0.65f, 0.65f, 0.65f);
    Color tabTextColor_Active = new Color(1.0f, 1.0f, 1.0f);
    Color tabTextColor_Alert = new Color(0, 0, 0);

    GameObject selectedPanelTab = null;

    float prev_update_ranks_time = -10000f;
    XmlDocument ranksXmlDoc = null;
    bool ranks_file_received = false, ranks_data_received = false;

    private const float UPDATE_RANKS_INTERVAL = 300f;

    public TournamentPanel()
    {
        instance = this;
    }

    public void Awake()
    {
        // Turn off the alert image of each panel tab
        infoPanelTab.transform.GetChild(0).gameObject.SetActive(false);
        ranksPanelTab.transform.GetChild(0).gameObject.SetActive(false);

        // Set text of each tab initially to inactive color.
        infoPanelTab.transform.GetChild(1).GetComponent<Text>().color = tabTextColor_Inctive;
        ranksPanelTab.transform.GetChild(1).GetComponent<Text>().color = tabTextColor_Inctive;

        if (selectedPanelTab == null)
        {
            // Begin with the recruit tab active.
            TabPressed(infoPanelTab);
        }
    }

    public void OnEnable()
    {
        // Hide the progress ring.
        progressRing.SetActive(false);

        // If this panel is activated and the ranks tab is selected, display the ranks list.
        if (selectedPanelTab == ranksPanelTab) {
            DisplayRanks();
        }

        // Update the panel for the current tournament status.
        UpdateForTournamentStatus();
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
        Debug.Log("TabPressed() " + _panelTab);

        // Display the newly activated tab as being active.
        selectedPanelTab.GetComponent<Image>().sprite = activePanelTabSprite;

        // Set selected tab's text color.
        selectedPanelTab.transform.GetChild(1).GetComponent<Text>().color = tabTextColor_Active;

        // Turn off the newly activated tab's alert image.
        selectedPanelTab.transform.GetChild(0).gameObject.SetActive(false);

        // Activate the appropriate content area.
        infoContentArea.SetActive(selectedPanelTab == infoPanelTab);
        ranksContentArea.SetActive(selectedPanelTab == ranksPanelTab);

        // Hide the progress ring.
        progressRing.SetActive(false);

        // If this panel is active and the ranks tab is selected, display the combined ranks list.
        if ((this.gameObject.activeInHierarchy) && (selectedPanelTab == ranksPanelTab)) {
            DisplayRanks();
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

    public void OnClick_InfoTab()
    {
        TabPressed(infoPanelTab);
    }

    public void OnClick_RanksTab()
    {
        TabPressed(ranksPanelTab);
    }

    public void UpdateForTournamentStatus()
    {
        // Don't update if the panel isn't active.
        if (!gameObject.activeInHierarchy) {
            return;
        }

        // Show the join area if the tournament is open for enrollment, and the nation is not already a contender.
        if ((GameData.instance.tournamentEnrollmentClosesTime > Time.unscaledTime) && (GameData.instance.nationTournamentStartDay != GameData.instance.globalTournamentStartDay))
        {
            // Set the join description text.
            joinDescText.text = LocalizationManager.GetTranslation("Tournament/tournament_join_desc")
                                    .Replace("<enroll_duration>", GameData.instance.GetDurationText((int)(GameData.instance.tournamentEnrollmentClosesTime - Time.unscaledTime)))
                                    .Replace("<tournament_duration>", GameData.instance.GetDurationText((int)(GameData.instance.tournamentEndTime - Time.unscaledTime)));

            joinArea.SetActive(true);
        }
        else
        {
            joinArea.SetActive(false);
        }

        // Show the status area if the nation is a contender in this tournament.
        if (GameData.instance.nationTournamentStartDay == GameData.instance.globalTournamentStartDay)
        {
            string status_text = "";

            if (GameData.instance.nationTournamentActive)
            {
                // <nation_name> has <num_trophies><sprite=23> (<num_banked_trophies> of them are banked).
                status_text += LocalizationManager.GetTranslation("Tournament/tournament_status_trophies")
                    .Replace("<nation_name>", GameData.instance.nationName)
                    .Replace("<num_trophies>", string.Format("{0:n0}", (int)(GameData.instance.tournamentTrophiesAvailable + GameData.instance.tournamentTrophiesBanked + 0.5f)));

                if ((int)(GameData.instance.tournamentTrophiesBanked + 0.5f) > 0)
                {
                    status_text += LocalizationManager.GetTranslation("Tournament/tournament_status_banked_trophies")
                        .Replace("<num_banked_trophies>", string.Format("{0:n0}", (int)(GameData.instance.tournamentTrophiesBanked + 0.5f)));
                }
            }
            else
            {
                // <nation_name> has been eliminated from this tournament.
                status_text += LocalizationManager.GetTranslation("Tournament/tournament_status_eliminated")
                    .Replace("<nation_name>", GameData.instance.nationName);
            }

            // We are ranked #<rank>.
            status_text += "\n";
            status_text += LocalizationManager.GetTranslation("Tournament/tournament_status_rank")
                    .Replace("<rank>", string.Format("{0:n0}", GameData.instance.tournamentRank));

            if (GameData.instance.nationTournamentActive && (GameData.instance.tournamentNextEliminationTime > Time.unscaledTime) && (GameData.instance.tournamentNumActiveContenders >= 3))
            {
                // Of the <num_active_contenders> active contenders, the bottom <num_to_eliminate> will be eliminated in <time_until_elimination>.
                status_text += "\n";
                int num_to_eliminate = GameData.instance.tournamentNumActiveContenders - (int)((float)GameData.instance.tournamentNumActiveContenders * 0.67f + 0.5f);
                status_text += LocalizationManager.GetTranslation("Tournament/tournament_status_next_elimination")
                    .Replace("<num_active_contenders>", string.Format("{0:n0}", GameData.instance.tournamentNumActiveContenders))
                    .Replace("<num_to_eliminate>", string.Format("{0:n0}", num_to_eliminate))
                    .Replace("<time_until_elimination>", string.Format("{0:n0}", GameData.instance.GetDurationText((int)(GameData.instance.tournamentNextEliminationTime - Time.unscaledTime))));

                // If this nation's rank puts it in a position to be eliminated, within 12 hours...
                if ((GameData.instance.tournamentRank >= (GameData.instance.tournamentNumActiveContenders - num_to_eliminate)) && ((GameData.instance.tournamentNextEliminationTime - Time.unscaledTime) <= (12 * 3600)))
                {
                    // <nation_name> is in danger of elimination!
                    status_text += "\n";
                    status_text += LocalizationManager.GetTranslation("Tournament/tournament_status_danger_of_elimination")
                        .Replace("<nation_name>", GameData.instance.nationName);
                }
            }
            else if (GameData.instance.tournamentEndTime > Time.unscaledTime)
            {
                // This tournament ends in <tournament_duration>.
                status_text += "\n";
                status_text += LocalizationManager.GetTranslation("Tournament/tournament_status_ends")
                        .Replace("<tournament_duration>", GameData.instance.GetDurationText((int)(GameData.instance.tournamentEndTime - Time.unscaledTime)));
            }

            if (!GameData.instance.nationTournamentActive)
            {
                // Better luck next time!
                status_text += "\n";
                status_text += LocalizationManager.GetTranslation("Tournament/tournament_status_next_time");
            }

            // Set the status text.
            statusText.text = status_text;

            statusArea.SetActive(true);
        }
        else
        {
            statusArea.SetActive(false);
        }
    }

    public void DisplayRanks()
    {
        // If enough time has passed since last update, fetch data.
        if ((Time.unscaledTime - prev_update_ranks_time) >= UPDATE_RANKS_INTERVAL)
        {
            // Clear the current ranks list display
            ClearRanksListDisplay();

            // Record new current ranks update time.
            prev_update_ranks_time = Time.unscaledTime;

            // Fetch the ranks list data
            StartCoroutine(FetchRanksList());
        }
    }

    public IEnumerator FetchRanksList()
    {
        float start_time;

        // Determine filename of ranks list file.
        string filename = "ranks_nation_tournament_current.xml";

        // Reset the records of whether the ranks file and data have been received.
        ranks_file_received = false;
        ranks_data_received = false;

        // Request the ranks data.
        Network.instance.SendCommand("action=get_ranks_data");

        // Access the game server's ranks xml file; yield until it is retrieved.
        String url = "http://" + Network.instance.GetConnectedServerAddress() + "/generated/ranks/" + filename;
        Debug.Log("ranks url: " + url);

		UnityWebRequest www = UnityWebRequest.Get(url);
		yield return Network.instance.RequestFromWeb(www);

		if (www.isNetworkError || www.isHttpError) {
            Debug.Log("Error accessing ranks xml: " + www.error);
        } else {
	 	    // Parse the xml file
		    ranksXmlDoc= new XmlDocument();
		    ranksXmlDoc.LoadXml(www.downloadHandler.text);

            // Record that the ranks file has been received.
            ranks_file_received = true;

            Debug.Log("file received");
            // If the ranks data has already been received, we have all the info we need. Compose the ranks list display now.
            if (ranks_data_received) {
                ComposeRanksListDisplay();
            }
        }
    }

    public void RanksDataEventReceived()
    {
        Debug.Log("data received. ranks_data_received: " + ranks_data_received + ", ranks_file_received: " + ranks_file_received);
        if (ranks_data_received == false)
        {
            ranks_data_received = true;

            // If the ranks file has already been received, we have all the info we need. Compose the ranks list display now.
            if (ranks_file_received) {
                ComposeRanksListDisplay();
            }
        }
    }

    public void ClearRanksListDisplay()
    {
        GameObject cur_child;

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
            ConnectPanel.instance.ComposeSingleRanksList(node, ranksListContent.transform);
		}

        // Hide the progress ring
        progressRing.SetActive(false);

        // Reset the records of whether the ranks file and data have been received.
        ranks_file_received = false;
        ranks_data_received = false;
    }
    
    public void OnClick_JoinTournament()
    {
        if (!GameData.instance.FealtyPreventsAction())
        {
            Requestor.Activate((int)RequestorTask.JoinTournament, 0, this, LocalizationManager.GetTranslation("Tournament/tournament_join_confirm").Replace("{nation_name}", GameData.instance.nationName),
                LocalizationManager.GetTranslation("Generic Text/yes_word"), LocalizationManager.GetTranslation("Generic Text/no_word"));
        }
    }
        
    public void RequestorResponse(int _task, int _data, Requestor.RequestorButton _result)
    {
        if (_result == Requestor.RequestorButton.LeftButton)
        {
            if ((RequestorTask)_task == RequestorTask.JoinTournament)
            {
                // Send message to server requesting to join tournament.
                Network.instance.SendCommand("action=request_join_tournament");

                // Reset prev_update_ranks_time so that ranks list will be fetched anew next time the ranks tab is viewed.
                prev_update_ranks_time = -10000;
            }
        }
    }
}