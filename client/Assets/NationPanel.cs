using UnityEngine;
using UnityEngine.UI;
using System;
using System.Collections;
using System.Collections.Generic;
using System.Globalization;
using I2.Loc;

public class NationPanel : MonoBehaviour, RequestorListener
{
    private const int REQUEST_STATS_INTERVAL = 60;

    public static NationPanel instance;

    public Sprite inactivePanelTabSprite, activePanelTabSprite;
    public GameObject membersPanelTab, alliesPanelTab, statsPanelTab, orbsPanelTab, resourcesPanelTab, settingsPanelTab;
    public GameObject membersContentArea, alliesContentArea, statsContentArea, orbsContentArea, resourcesContentArea, settingsContentArea;

    public Text areaTotalText, areaSupportableText, areaInteriorText, areaBorderText, geoEfficiencyText, levelText, xpText, techText, bioText, psiText, xpMultiplierText, manpowerMaxText, manpowerGenRateText, manpowerPerAttackText, criticalHitChanceText, splashDamageText, maxSimultaneousAttacksText, energyMaxText, energyGenRateText, energyBurnRateText, hpPerSquareText, hpRestoreRateText, salvageValueText, wallDiscountText, structureDiscountText, invisibilityText, insurgencyText, totalDefenseText, maxNumAlliancesText, numStorageStructuresText, manpowerStoredText, energyStoredText, manpowerAvailableText, energyAvailableText, rebirthLevelBonusText, storageXPRateText;

    public TMPro.TextMeshProUGUI nationNameText, migrateButtonText, resetAdvancesButtonText, rebirthNoticeBarText, rebirthCountdownBarText;
    public Image rebirthNoticeBarImage, rebirthCountdownBarImage;
    public TMPro.TextMeshProUGUI rebirthNoticeLabelText;
    public GameObject passwordRow, customizeButton, rebirthNotice, rebirthCountdown, rebirthButton, incognitoToggleLine;
    public InputField passwordInputField;
    public Toggle blockInvitationsToggle, incognitoToggle;
    public Sprite lineDark, lineLight;

    public GameObject memberListContentObject;
    public GameObject allyListContentObject, incomingAllyInvitationListContentObject, outgoingAllyInvitationListContentObject, incomingUniteInvitationListContentObject, outgoingUniteInvitationListContentObject, orbsListContent, resourcesListContent;
    public GameObject alliesArea, allyInvitationsReceivedArea, allyInvitationsSentArea, uniteInvitationsReceivedArea, uniteInvitationsSentArea, instructionsArea;

    private bool processing_info_event = false;

    // Orbs subpanel
    public TMPro.TextMeshProUGUI currentWinningsText, tradeInText;
    public GameObject titleLinePrefab, winningsLinePrefab, tradeInButton, cashOutButton, prizePolicyGoogleText, winningsPanel;
    float prev_update_orbs_time = -10000f;
    private const float CHECK_ORBS_INTERVAL = 1800f;
    private const float UPDATE_ORBS_INTERVAL = 300f;

    // Resources subpanel
    public GameObject penaltyLine;
    public bool resourcesListModified = false;
    public TMPro.TextMeshProUGUI totalTechText, totalBioText, totalPsiText, totalEnergyText, totalManpowerText, totalXPText;
    public TMPro.TextMeshProUGUI penaltyTechText, penaltyBioText, penaltyPsiText, penaltyEnergyText, penaltyManpowerText, penaltyXPText;
    public int expandedObjectID = -1;

    Color tabTextColor_Inctive = new Color(0.65f, 0.65f, 0.65f);
    Color tabTextColor_Active = new Color(1.0f, 1.0f, 1.0f);
    Color tabTextColor_Alert = new Color(0, 0, 0);

    GameObject selectedPanelTab = null;

    List<GameObject> memberEntryList = new List<GameObject>();

    List<GameObject> allyEntryList = new List<GameObject>();
    List<GameObject> incomingAllyRequestEntryList = new List<GameObject>();
    List<GameObject> outgoingAllyRequestEntryList = new List<GameObject>();
    List<GameObject> incomingUniteRequestEntryList = new List<GameObject>();
    List<GameObject> outgoingUniteRequestEntryList = new List<GameObject>();

    int removeMemberUserID;
    string removeMemberUsername;
    float prev_request_stats_time = 0;

    private enum RequestorTask
    {
        RemoveMemberfromNation,
        ReturnToNationOrJoinAnother,
        AcceptAllianceInvitation,
        DeclineAllianceInvitation,
        WithdrawAllianceInvitation,
        SendUniteInvitation,
        AcceptUniteInvitation,
        BreakAlliance,
        Migrate,
        MigrateEntireNation,
        ResetAdvances,
        Rebirth,
        IncognitoOn,
        IncognitoOff,
        TradeIn
    };

    public NationPanel()
    {
        instance = this;
    }

    public void Start()
    {
        // Turn off the alert image of each panel tab
        membersPanelTab.transform.GetChild(0).gameObject.SetActive(false);
        alliesPanelTab.transform.GetChild(0).gameObject.SetActive(false);
        statsPanelTab.transform.GetChild(0).gameObject.SetActive(false);
        orbsPanelTab.transform.GetChild(0).gameObject.SetActive(false);
        resourcesPanelTab.transform.GetChild(0).gameObject.SetActive(false);
        settingsPanelTab.transform.GetChild(0).gameObject.SetActive(false);

        // Set text of each tab initially to inactive color.
        membersPanelTab.transform.GetChild(1).GetComponent<Text>().color = tabTextColor_Inctive;
        alliesPanelTab.transform.GetChild(1).GetComponent<Text>().color = tabTextColor_Inctive;
        statsPanelTab.transform.GetChild(1).GetComponent<Text>().color = tabTextColor_Inctive;
        orbsPanelTab.transform.GetChild(1).GetComponent<Text>().color = tabTextColor_Inctive;
        resourcesPanelTab.transform.GetChild(1).GetComponent<Text>().color = tabTextColor_Inctive;
        settingsPanelTab.transform.GetChild(1).GetComponent<Text>().color = tabTextColor_Inctive;

        // Begin with the members tab active.
        TabPressed(membersPanelTab);
    }

    public void OnEnable()
    {
        // If the stats subpanel is displayed and the stats haven't been fetched in a long time, request them.
        if (statsContentArea.activeSelf && ((Time.unscaledTime - prev_request_stats_time) >= REQUEST_STATS_INTERVAL)) {
            Network.instance.SendCommand("action=request_stats");
            prev_request_stats_time = Time.unscaledTime;
        }

        // If the nation orbs winnings haven't been fetched in a long time, and the orbs tab is hidden, fetch it.
        if ((orbsPanelTab.activeSelf == false) && ((Time.unscaledTime - prev_update_orbs_time) >= CHECK_ORBS_INTERVAL)) {
            Network.instance.SendCommand("action=get_nation_orbs");
        }

        // If we're showing the resources tab and the resources list has been modified since it was last shown, update the list.
        if (resourcesContentArea.activeSelf && resourcesListModified) {
            RefreshResourcesList();
        }

        // Update the rebirth UI
        UpdateRebirthUI();

        StartCoroutine(UpdateMigrateButton());

		//// TEST
        //GameGUI.instance.LogToChat("Safe area: " + Screen.safeArea.xMin + "," + Screen.safeArea.yMin + " to " + Screen.safeArea.xMax + "," + Screen.safeArea.yMax + ", screen size: " + Screen.width + "," + Screen.height + ", Scale: " + MapView.instance.canvas.scaleFactor);
    }

    public void InfoEventReceived()
    {
        // Record that we're processing the info event, so the below changes will not result in messages to the server.
        processing_info_event = true;

        // Display the nation's name
        nationNameText.text = GameData.instance.nationName;

        // Display the Incognito toggle only if the nation has developed invisibility.
        incognitoToggleLine.SetActive(GameData.instance.invisibility);

        // Display the customize button only if the nation is not incognito.
        customizeButton.SetActive(!GameData.instance.GetNationFlag(GameData.NationFlags.INCOGNITO));

        passwordInputField.text = GameData.instance.nationPassword;
        blockInvitationsToggle.isOn = GameData.instance.GetNationFlag(GameData.NationFlags.BLOCK_NATION_CHAT_INVITATIONS);
        incognitoToggle.isOn = GameData.instance.GetNationFlag(GameData.NationFlags.INCOGNITO);

        // Make changes necessary for having received this user's rank.
        RankReceived();

        // Update the level and XP displays.
        UpdateForLevel();
        UpdateForXP();

        // Update the Winnings UI
        UpdateWinningsUI();

        // Done processing info event.
        processing_info_event = false;
    }

    public void NationDataReceived(int _nationID)
    {
        if (_nationID == GameData.instance.nationID)
        {
            // Display the customize button only if the nation is not incognito.
            customizeButton.SetActive(!GameData.instance.GetNationFlag(GameData.NationFlags.INCOGNITO));

            // Modify the toggles according to the nation flag states.
            processing_info_event = true;
            blockInvitationsToggle.isOn = GameData.instance.GetNationFlag(GameData.NationFlags.BLOCK_NATION_CHAT_INVITATIONS);
            incognitoToggle.isOn = GameData.instance.GetNationFlag(GameData.NationFlags.INCOGNITO);
            processing_info_event = false;
        }
    }

    public void AccountInfoEventReceived()
    {
        // Display the nation's name
        nationNameText.text = GameData.instance.nationName;
    }

    public void RankReceived()
    {
        // Only allow password to be viewed if rank is commander or better.
        passwordRow.SetActive(GameData.instance.userRank <= GameData.RANK_COMMANDER);

        // Only allow password to be edited if rank is general or better.
        passwordInputField.interactable = (GameData.instance.userRank <= GameData.RANK_GENERAL);
    }

    public void MemberListReceived(List<MemberEntryData> _memberList)
    {
        GameObject curObject;
        MemberEntry curEntry;

        // Sort the given member list.
        _memberList.Sort(new MemberEntryDataComparer());

        int index = 0;
        foreach (MemberEntryData curData in _memberList)
        {
            if (index >= memberEntryList.Count)
            {
                // Get a new member entry
                curObject = MemManager.instance.GetMemberEntryObject();
                memberEntryList.Add(curObject);

                // Add the new entry to the list.
                curObject.transform.SetParent(memberListContentObject.transform);
                curObject.transform.SetAsLastSibling();
                curObject.transform.localScale = new Vector3(1, 1, 1); // Needs to be done each time it's activated, in case it was changed last time used.
            }
            else
            {
                // Get existing member entry from list
                curObject = memberEntryList[index];
            }

            // Get pointer to MemberEntry component.
            curEntry = curObject.GetComponent<MemberEntry>();

            // Initialize the new entry
            curEntry.Init(curData);

            index++;
        }

        // Remove and release any excess member entries currently in the list.
        while (memberEntryList.Count > index)
        {
            curObject = memberEntryList[index];
            memberEntryList.RemoveAt(index);
            curObject.transform.GetChild(1).GetChild(0).GetComponent<Dropdown>().Hide();
            curObject.transform.SetParent(null);
            MemManager.instance.ReleaseMemberEntryObject(curObject);
        }
    }

    public void AlliancesReceived()
    {
        // Show only those lists that are not empty. If all are empty, show instructions.
        alliesArea.SetActive(GameData.instance.alliesList.Count > 0);
        allyInvitationsReceivedArea.SetActive(GameData.instance.incomingAllyRequestsList.Count > 0);
        allyInvitationsSentArea.SetActive(GameData.instance.outgoingAllyRequestsList.Count > 0);
        uniteInvitationsReceivedArea.SetActive(GameData.instance.incomingUniteRequestsList.Count > 0);
        uniteInvitationsSentArea.SetActive(GameData.instance.outgoingUniteRequestsList.Count > 0);
        instructionsArea.SetActive((GameData.instance.alliesList.Count == 0) && (GameData.instance.incomingAllyRequestsList.Count == 0) && (GameData.instance.outgoingAllyRequestsList.Count == 0));

        UpdateAlliesPanelList(GameData.instance.alliesList, allyEntryList, allyListContentObject, GameData.AllianceListType.CURRENT_ALLIES);
        UpdateAlliesPanelList(GameData.instance.incomingAllyRequestsList, incomingAllyRequestEntryList, incomingAllyInvitationListContentObject, GameData.AllianceListType.INCOMING_ALLIANCE_INVITATIONS);
        UpdateAlliesPanelList(GameData.instance.outgoingAllyRequestsList, outgoingAllyRequestEntryList, outgoingAllyInvitationListContentObject, GameData.AllianceListType.OUTGOING_ALLIANCE_INVITATIONS);
        UpdateAlliesPanelList(GameData.instance.incomingUniteRequestsList, incomingUniteRequestEntryList, incomingUniteInvitationListContentObject, GameData.AllianceListType.INCOMING_UNITE_INVITATIONS);
        UpdateAlliesPanelList(GameData.instance.outgoingUniteRequestsList, outgoingUniteRequestEntryList, outgoingUniteInvitationListContentObject, GameData.AllianceListType.OUTGOING_UNITE_INVITATIONS);
    }

    public void NationOrbsReceived()
    {
        // Refresh the list of nation orbs
        RefreshNationOrbsList();

        // Record that the nation orbs have been updated.
        prev_update_orbs_time = Time.unscaledTime;
    }

    public void RefreshNationOrbsList()
    {
        GameObject cur_child, titleLineObject, winningsLineObject;

        // Remove any children of the orbs list content object.
        while (orbsListContent.transform.childCount > 0)
        {
            cur_child = orbsListContent.transform.GetChild(0).gameObject;
            cur_child.transform.SetParent(null);
            Destroy(cur_child);
        }

        if (GameData.instance.nationOrbsMonthlyList.Count > 0)
        {
            // Add a line for the monthly list to the orbs content area.
            titleLineObject = (GameObject) Instantiate(titleLinePrefab,  new Vector3(0f, 0f, 0f), Quaternion.identity);
            titleLineObject.transform.SetParent(orbsListContent.transform);
            titleLineObject.transform.localScale = new Vector3(1, 1, 1);
            titleLineObject.transform.GetChild(0).GetComponent<TMPro.TextMeshProUGUI>().text = CultureInfo.CurrentCulture.DateTimeFormat.GetMonthName(DateTime.Now.Month) + " " + LocalizationManager.GetTranslation("Connect Panel/winnings"); // "<Month> Winnings"    
            titleLineObject.transform.GetChild(1).GetComponent<TMPro.TextMeshProUGUI>().text = "$" + (GameData.instance.orb_winnings_history_monthly / 100.0).ToString("N2")/*string.Format("{0:n0}", GameData.instance.orb_winnings_history_monthly) + " <sprite=2>"*/;

            for (int i = 0; i < GameData.instance.nationOrbsMonthlyList.Count; i++)
            {
                // Add a line for this individual rank to the ranks list content area.
                winningsLineObject = (GameObject) Instantiate(winningsLinePrefab,  new Vector3(0f, 0f, 0f), Quaternion.identity);
                winningsLineObject.transform.SetParent(orbsListContent.transform);
                winningsLineObject.transform.localScale = new Vector3(1, 1, 1);
                winningsLineObject.GetComponent<WinningsLine>().Init(GameData.instance.nationOrbsMonthlyList[i], i);
            }
        }

        if (GameData.instance.nationOrbsList.Count > 0)
        {
            // Add a line for the all-time list to the orbs content area.
            titleLineObject = (GameObject) Instantiate(titleLinePrefab,  new Vector3(0f, 0f, 0f), Quaternion.identity);
            titleLineObject.transform.SetParent(orbsListContent.transform);
            titleLineObject.transform.localScale = new Vector3(1, 1, 1);
            titleLineObject.transform.GetChild(0).GetComponent<TMPro.TextMeshProUGUI>().text = LocalizationManager.GetTranslation("Connect Panel/all_time") + " " + LocalizationManager.GetTranslation("Connect Panel/winnings"); // "All-Time Winnings"    
            titleLineObject.transform.GetChild(1).GetComponent<TMPro.TextMeshProUGUI>().text = "$" + (GameData.instance.orb_winnings_history / 100.0).ToString("N2")/*string.Format("{0:n0}", GameData.instance.orb_winnings_history) + " <sprite=2>"*/;

            for (int i = 0; i < GameData.instance.nationOrbsList.Count; i++)
            {
                // Add a line for this individual rank to the ranks list content area.
                winningsLineObject = (GameObject) Instantiate(winningsLinePrefab,  new Vector3(0f, 0f, 0f), Quaternion.identity);
                winningsLineObject.transform.SetParent(orbsListContent.transform);
                winningsLineObject.transform.localScale = new Vector3(1, 1, 1);
                winningsLineObject.GetComponent<WinningsLine>().Init(GameData.instance.nationOrbsList[i], i);
            }
        }
    }

    public void ResourcesListModified()
    {
        if (statsContentArea.activeInHierarchy) {
            RefreshResourcesList();
        } else {
            resourcesListModified = true;
        }
    }

    public void RefreshResourcesList()
    {
        GameObject cur_child;
        
        // Remove and free any children of the resources list content object.
        while (resourcesListContent.transform.childCount > 0)
        {
            cur_child = resourcesListContent.transform.GetChild(0).gameObject;
            cur_child.transform.SetParent(null);

            if (cur_child.GetComponent<ResourceLine>() != null) {
                MemManager.instance.ReleaseResourceLineObject(cur_child);
            } else if (cur_child.GetComponent<ResourceLocLine>() != null) {
                MemManager.instance.ReleaseResourceLocLineObject(cur_child);
            }
        }
        
        // Sort the list of ObjectRecords
        GameData.instance.objects.Sort(new ObjectRecordComparer());

        // Initialize bonus sums.
        int sumTech = 0, sumBio = 0, sumPsi = 0, sumEnergy = 0, sumManpower = 0, sumXP = 0;

        int curObjectID = -1;
        ResourceLine resourceLine = null;
        foreach (ObjectRecord cur_record in GameData.instance.objects)
        {
            // Only deal with resource objects.
            if ((cur_record.objectID < ObjectData.RESOURCE_OBJECT_BASE_ID) || (cur_record.objectID >= ObjectData.ORB_BASE_ID)) {
                continue;
            }

            if (cur_record.objectID != curObjectID)
            {
                // Finish the previous Resource Line.
                if (resourceLine != null) 
                {
                    resourceLine.Finish();

                    if (resourceLine.requirementsMet)
                    {
                        sumTech += resourceLine.valTech;
                        sumBio += resourceLine.valBio;
                        sumPsi += resourceLine.valPsi;
                        sumEnergy += resourceLine.valEnergy;
                        sumManpower += resourceLine.valManpower;
                        sumXP += resourceLine.valXP;
                    }

                    resourceLine = null;
                }

                // Record the new current objectID.
                curObjectID = cur_record.objectID;
                
                // Add a new line to the resources list
                resourceLine = MemManager.instance.GetResourceLineObject().GetComponent<ResourceLine>();
                resourceLine.gameObject.transform.SetParent(resourcesListContent.transform);
                resourceLine.gameObject.transform.localScale = new Vector3(1, 1, 1);
                resourceLine.Init(curObjectID, curObjectID == expandedObjectID);
            }

            // Add the current object to the current Resource Line.
            resourceLine.AddObject(cur_record.blockX);

            // If this resource is expanded, add a line representing this specific resource location.
            if (curObjectID == expandedObjectID)
            {
                // Add a new resource location line to the resources list
                ResourceLocLine resourceLocLine = MemManager.instance.GetResourceLocLineObject().GetComponent<ResourceLocLine>();
                resourceLocLine.gameObject.transform.SetParent(resourcesListContent.transform);
                resourceLocLine.gameObject.transform.localScale = new Vector3(1, 1, 1);
                resourceLocLine.Init(curObjectID, cur_record.blockX, cur_record.blockZ, resourceLine.requirementsMet);
            }
        }

        // Finish the final Resource Line.
        if (resourceLine != null) 
        {
            resourceLine.Finish();

            if (resourceLine.requirementsMet)
            {
                sumTech += resourceLine.valTech;
                sumBio += resourceLine.valBio;
                sumPsi += resourceLine.valPsi;
                sumEnergy += resourceLine.valEnergy;
                sumManpower += resourceLine.valManpower;
                sumXP += resourceLine.valXP;
            }
        }

        // Display each of the bonus totals.
        ResourceLine.DisplayBonus(sumTech, totalTechText, false);
        ResourceLine.DisplayBonus(sumBio, totalBioText, false);
        ResourceLine.DisplayBonus(sumPsi, totalPsiText, false);
        ResourceLine.DisplayBonus(sumEnergy, totalEnergyText, false);
        ResourceLine.DisplayBonus(sumManpower, totalManpowerText, false);
        ResourceLine.DisplayBonus(sumXP, totalXPText, true);

        bool showPenalty = false;
        DisplayManpowerPenalty(GameData.instance.statTechFromPerms, GameData.instance.statTechFromResources, penaltyTechText, ref showPenalty);
        DisplayManpowerPenalty(GameData.instance.statBioFromPerms, GameData.instance.statBioFromResources, penaltyBioText, ref showPenalty);
        DisplayManpowerPenalty(GameData.instance.statPsiFromPerms, GameData.instance.statPsiFromResources, penaltyPsiText, ref showPenalty);
        DisplayManpowerPenalty(GameData.instance.energyRateFromPerms, GameData.instance.energyRateFromResources, penaltyEnergyText, ref showPenalty);
        DisplayManpowerPenalty(GameData.instance.manpowerRateFromPerms, GameData.instance.manpowerRateFromResources, penaltyManpowerText, ref showPenalty);
        DisplayManpowerPenalty(GameData.instance.xpMultiplierFromPerms, GameData.instance.xpMultiplierFromResources, penaltyXPText, ref showPenalty);

        // Display the manpower penalty line if there are any penalties.
        penaltyLine.SetActive(showPenalty);

        // The list has been refreshed; turn off the resourcesListModified flag.
        resourcesListModified = false;
    }

    public void DisplayManpowerPenalty(float _valueFromPerms, float _valueFromResources, TMPro.TextMeshProUGUI _penaltyText, ref bool _penaltyApplied)
    {
        int manpower_burn = (int)(GameData.instance.DetermineStatManpowerPenalty(_valueFromPerms, _valueFromResources) + 0.5f);
        if (manpower_burn > 0)
        {
            _penaltyText.text = "-" + string.Format("{0:n0}", manpower_burn) + " <sprite=3>";
            _penaltyApplied = true;
        }
        else
        {
            _penaltyText.text = "";
        }
    }

    public void SetExpandedObjectID(int _expandedObjectID)
    {
        if (expandedObjectID == _expandedObjectID) {
            return;
        }

        // Record new expandedObjectID.
        expandedObjectID = _expandedObjectID;

        // Refresh the list of resources.
        RefreshResourcesList();
    }

    IEnumerator UpdateMigrateButton()
    {
        // GB-Localization
        string button_text;

        // Wait until after the first frame to begin executing this, to avoid conflict.
        yield return null;

        for (;;)
        {
            string migrate_button_migrate_nation = I2.Loc.LocalizationManager.GetTranslation("migrate_button_migrate_nation"); // "Migrate Nation";

            if (Time.unscaledTime >= GameData.instance.nextFreeMigrationTime) {
                button_text = string.Format("{0}  (0<sprite=2>)", migrate_button_migrate_nation);
            } else {
                button_text = string.Format("{0}  ({1}<sprite=2>)\n<font=\"Proxima Nova Regular SDF\"><size=11><color=\"orange\">{2} {3}</color></size></font>",
                    migrate_button_migrate_nation,
                    GameData.instance.migrationCost,
                    GameData.instance.GetDurationClockText((int)(GameData.instance.nextFreeMigrationTime - Time.unscaledTime)),
                    I2.Loc.LocalizationManager.GetTranslation("migrate_button_phrase1")); // "until next free migration"
            }

            migrateButtonText.text = button_text;

            yield return new WaitForSeconds(1);
        }
    }

    public void UpdateAlliesPanelList(List<AllyData> _list, List<GameObject> _entry_list, GameObject _listContentObject, GameData.AllianceListType _list_type)
    {
        GameObject curObject;
        AllyEntry curEntry;

        int index = 0;
        foreach (AllyData curData in _list)
        {
            if (index >= _entry_list.Count)
            {
                // Get a new member entry
                curObject = MemManager.instance.GetAllyEntryObject();
                _entry_list.Add(curObject);

                // Add the new entry to the list.
                curObject.transform.SetParent(_listContentObject.transform);
                curObject.transform.SetAsLastSibling();
                curObject.transform.localScale = new Vector3(1, 1, 1); // Needs to be done each time it's activated, in case it was changed last time used.
            }
            else
            {
                // Get existing member entry from list
                curObject = _entry_list[index];
            }

            // Get pointer to AllyEntry component.
            curEntry = curObject.GetComponent<AllyEntry>();

            // Initialize the new entry
            curEntry.Init(curData, _list_type);

            index++;
        }

        // Remove and release any excess ally entries currently in the list.
        while (_entry_list.Count > index)
        {
            curObject = _entry_list[index];
            _entry_list.RemoveAt(index);
            curObject.transform.SetParent(null);
            MemManager.instance.ReleaseAllyEntryObject(curObject);
        }
    }

    public void UpdateForStatsEvent()
    {
        areaTotalText.text = GameData.instance.GetStatValueString(GameData.Stat.TOTAL_AREA);
        areaSupportableText.text = GameData.instance.GetStatValueString(GameData.Stat.SUPPORTABLE_AREA);
        areaInteriorText.text = GameData.instance.GetStatValueString(GameData.Stat.INTERIOR_AREA);
        areaBorderText.text = GameData.instance.GetStatValueString(GameData.Stat.PERIMETER);
        geoEfficiencyText.text = GameData.instance.GetStatValueString(GameData.Stat.GEOGRAPHIC_EFFICIENCY);
        techText.text = GameData.instance.GetStatValueString(GameData.Stat.TECH);
        bioText.text = GameData.instance.GetStatValueString(GameData.Stat.BIO);
        psiText.text = GameData.instance.GetStatValueString(GameData.Stat.PSI);
        xpMultiplierText.text = GameData.instance.GetStatValueString(GameData.Stat.XP_MULTIPLIER);
        manpowerMaxText.text = GameData.instance.GetStatValueString(GameData.Stat.MANPOWER_MAX);
        manpowerGenRateText.text = GameData.instance.GetStatValueString(GameData.Stat.MANPOWER_RATE_MINUS_BURN);
        manpowerPerAttackText.text = GameData.instance.GetStatValueString(GameData.Stat.ATTACK_MANPOWER);
        criticalHitChanceText.text = GameData.instance.GetStatValueString(GameData.Stat.CRIT_CHANCE);
        splashDamageText.text = GameData.instance.GetStatValueString(GameData.Stat.SPLASH_DAMAGE);
        maxSimultaneousAttacksText.text = GameData.instance.GetStatValueString(GameData.Stat.SIMULTANEOUS_ACTIONS);
        energyMaxText.text = GameData.instance.GetStatValueString(GameData.Stat.ENERGY);
        energyGenRateText.text = GameData.instance.GetStatValueString(GameData.Stat.ENERGY_RATE);
        energyBurnRateText.text = GameData.instance.GetStatValueString(GameData.Stat.FINAL_ENERGY_BURN_RATE);
        hpPerSquareText.text = GameData.instance.GetStatValueString(GameData.Stat.HP_PER_SQUARE);
        hpRestoreRateText.text = GameData.instance.GetStatValueString(GameData.Stat.HP_RESTORE);
        salvageValueText.text = GameData.instance.GetStatValueString(GameData.Stat.SALVAGE_VALUE);
        wallDiscountText.text = GameData.instance.GetStatValueString(GameData.Stat.WALL_DISCOUNT);
        structureDiscountText.text = GameData.instance.GetStatValueString(GameData.Stat.STRUCTURE_DISCOUNT);
        invisibilityText.text = GameData.instance.GetStatValueString(GameData.Stat.INVISIBILITY);
        insurgencyText.text = GameData.instance.GetStatValueString(GameData.Stat.INSURGENCY);
        totalDefenseText.text = GameData.instance.GetStatValueString(GameData.Stat.TOTAL_DEFENSE);
        maxNumAlliancesText.text = GameData.instance.GetStatValueString(GameData.Stat.MAX_ALLIANCES);
        numStorageStructuresText.text = GameData.instance.GetStatValueString(GameData.Stat.NUM_STORAGE_STRUCTURES) + " / " + GameData.instance.maxNumStorageStructures;
        manpowerStoredText.text = GameData.instance.GetStatValueString(GameData.Stat.MANPOWER_STORED);
        energyStoredText.text = GameData.instance.GetStatValueString(GameData.Stat.ENERGY_STORED);
        manpowerAvailableText.text = GameData.instance.GetStatValueString(GameData.Stat.MANPOWER_AVAILABLE);
        energyAvailableText.text = GameData.instance.GetStatValueString(GameData.Stat.ENERGY_AVAILABLE);        
        rebirthLevelBonusText.text = GameData.instance.GetStatValueString(GameData.Stat.REBIRTH_LEVEL_BONUS);
        storageXPRateText.text = GameData.instance.GetStatValueString(GameData.Stat.STORAGE_XP_RATE);

        // Display price for resetting advances.
        resetAdvancesButtonText.text = string.Format("{0}  ({1}<sprite=2>)", I2.Loc.LocalizationManager.GetTranslation("Nation Panel/reset_advances"), GameData.instance.resetAdvancesPrice);

        // Display the Incognito toggle only if the nation has developed invisibility.
        incognitoToggleLine.SetActive(GameData.instance.invisibility);
    }

    public void UpdateRebirthUI()
    {
        if (GameData.instance == null) {
            return;
        }

        if ((GameData.instance.level < GameData.instance.rebirth_available_level) || (GameData.instance.rebirth_available_time > Time.unscaledTime))
        {
            rebirthCountdown.SetActive(false);
            rebirthButton.SetActive(false);
            rebirthNotice.SetActive(true);

            if (GameData.instance.level < GameData.instance.rebirth_available_level)
            {
                // Notice meter will show how close to min rebirth level we're at.
                rebirthNoticeLabelText.text = GameGUI.instance.GetLocalizedString("Nation Panel/rebirth_allowed").Replace("{level}", "" + GameData.instance.rebirth_available_level);
                rebirthNoticeBarText.text = "" + Math.Min(GameData.instance.level, GameData.instance.rebirth_available_level) + " / " + GameData.instance.rebirth_available_level;
                rebirthNoticeBarImage.fillAmount = Mathf.Min(1f, (float)GameData.instance.level / (float)GameData.instance.rebirth_available_level);
            }
            else
            {
                // Notice meter will show rebirth countdown. Text will say how long until rebirth is possible.
                rebirthNoticeLabelText.text = GameGUI.instance.GetLocalizedString("Nation Panel/rebirth_allowed_time").Replace("{time_remaining}", "" + GameData.instance.GetDurationText((int)(GameData.instance.rebirth_available_time - Time.unscaledTime)));
                UpdateRebirthCountdown();
            }
        }
        else
        {
            // Show rebirth button and countdown meter.
            rebirthButton.SetActive(true);
            rebirthCountdown.SetActive(true);
            rebirthNotice.SetActive(false);
            UpdateRebirthCountdown();
        }
    }

    public void UpdateWinningsUI()
    {
        if (GameData.instance == null) {
            return;
        }

		bool play_store = false;
		#if PLAY_STORE
			play_store = true;
		#endif

        winningsPanel.SetActive((GameData.instance.prizeMoney > 0) || (GameData.instance.prizeMoneyHistory > 0));
        tradeInButton.SetActive(GameData.instance.prizeMoney > 0);
        cashOutButton.SetActive(!play_store && GameData.instance.cashOutPrizesAllowed && (GameData.instance.prizeMoney >= GameData.instance.minWinningsToCashOut));
		prizePolicyGoogleText.SetActive(play_store && GameData.instance.cashOutPrizesAllowed && (GameData.instance.prizeMoney >= GameData.instance.minWinningsToCashOut));

        currentWinningsText.text = GameGUI.instance.GetLocalizedString("Nation Panel/current_winnings") + ": $" + (GameData.instance.prizeMoney / 100.0).ToString("N2") + " <size=17><sprite=3></size>";
        tradeInText.text = GameGUI.instance.GetLocalizedString("Nation Panel/trade_in") + " " + (GameData.instance.prizeMoney * GameData.instance.creditsPerCentTradedIn) + "<sprite=2>";

        // Turn on the Orbs subpanel tab only if there are orb records to display, or prize winnings have been received.
        orbsPanelTab.SetActive((GameData.instance.nationOrbsList.Count > 0) || (GameData.instance.nationOrbsMonthlyList.Count > 0) || (GameData.instance.prizeMoney > 0) || (GameData.instance.prizeMoneyHistory > 0));
    }

    public void UpdateRebirthCountdown()
    {
        if ((GameData.instance.rebirth_available_time > Time.unscaledTime) && (GameData.instance.level >= GameData.instance.rebirth_available_level))
        {
            rebirthNoticeBarText.text = "" + GameData.instance.rebirth_countdown;
            rebirthNoticeBarImage.fillAmount = Mathf.Min(1f, (float)GameData.instance.rebirth_countdown / (float)GameData.instance.rebirth_countdown_start);
        }
        else
        {
            rebirthCountdownBarText.text = "" + GameData.instance.rebirth_countdown;
            rebirthCountdownBarImage.fillAmount = Mathf.Min(1f, (float)GameData.instance.rebirth_countdown / (float)GameData.instance.rebirth_countdown_start);
        }
    }

    public void UpdateForUpdateEvent()
    {
        energyMaxText.text = GameData.instance.GetStatValueString(GameData.Stat.ENERGY);
        energyBurnRateText.text = GameData.instance.GetStatValueString(GameData.Stat.FINAL_ENERGY_BURN_RATE);

        if (GameData.instance.level >= GameData.instance.rebirth_available_level) {
            UpdateRebirthCountdown();
        }

        // Update the Winnings UI
        UpdateWinningsUI();
    }

    public void UpdateForLevel()
    {
        // Update level text.
        levelText.text = GameData.instance.GetStatValueString(GameData.Stat.LEVEL);

        // Update supportable area, which is based on level.
        areaSupportableText.text = GameData.instance.GetStatValueString(GameData.Stat.SUPPORTABLE_AREA);

        // Update the rebirth UI
        UpdateRebirthUI();
    }

    public void UpdateForXP()
    {
        xpText.text = GameData.instance.GetStatValueString(GameData.Stat.XP);
    }

    public void UpdateForArea()
    {
        areaTotalText.text = GameData.instance.GetStatValueString(GameData.Stat.TOTAL_AREA);
        areaInteriorText.text = GameData.instance.GetStatValueString(GameData.Stat.INTERIOR_AREA);
        areaBorderText.text = GameData.instance.GetStatValueString(GameData.Stat.PERIMETER);
        geoEfficiencyText.text = GameData.instance.GetStatValueString(GameData.Stat.GEOGRAPHIC_EFFICIENCY);
        //energyGenRateText.text = GameData.instance.GetStatValueString(GameData.Stat.ENERGY_RATE);
    }

    public void OnClick_RanksInfoButton()
    {
        Requestor.Activate(0, 0, null, LocalizationManager.GetTranslation("Nation Panel/ranks_info"), LocalizationManager.GetTranslation("Generic Text/okay"), "", Requestor.FLAG_ALIGN_LEFT);
    }

    public void OnClick_WinningsInfoButton()
    {
        String info_text = ((GameData.instance.prizeMoney < GameData.instance.minWinningsToCashOut) ? LocalizationManager.GetTranslation("Nation Panel/winnings_info_under_min") : LocalizationManager.GetTranslation("Nation Panel/winnings_info_over_min"))
                .Replace("{[current_winnings]}", "$" + (GameData.instance.prizeMoney / 100.0).ToString("N2"))
                .Replace("{[min_cash_out_amount]}", "$" + (GameData.instance.minWinningsToCashOut / 100.0).ToString("N2"))
                .Replace("{[winnings_credits]}", (GameData.instance.prizeMoney * GameData.instance.creditsPerCentTradedIn).ToString());

        Requestor.Activate(0, 0, null, info_text, LocalizationManager.GetTranslation("Generic Text/okay"), "", Requestor.FLAG_ALIGN_LEFT);
    }

    public void OnClick_RemoveMember(int _memberID, string _username)
    {
        removeMemberUserID = _memberID;
        removeMemberUsername = _username;

        if (_memberID == GameData.instance.userID)
        {
            // "Are you sure you want to leave {[NATION_NAME]}?" // 
            Requestor.Activate((int)RequestorTask.RemoveMemberfromNation, _memberID, this
                , LocalizationManager.GetTranslation("Nation Panel/confirm_leave_nation").Replace("{[NATION_NAME]}", GameData.instance.nationName)
                , LocalizationManager.GetTranslation("Generic Text/yes_word"), LocalizationManager.GetTranslation("Generic Text/no_word"));
        }
        else
        {
            // "Remove {[USER_NAME]} from {[NATION_NAME]}?" // 
            Requestor.Activate((int)RequestorTask.RemoveMemberfromNation, _memberID, this
                , LocalizationManager.GetTranslation("Nation Panel/confirm_remove_user_from_nation")
                    .Replace("{[USER_NAME]}", _username)
                    .Replace("{[NATION_NAME]}", GameData.instance.nationName)
                , LocalizationManager.GetTranslation("Generic Text/yes_word"), LocalizationManager.GetTranslation("Generic Text/no_word"));
        }
    }

    public void OnChange_MemberRank(int _memberID, int _new_rank)
    {
        // Send the rank change message to the server.
        Network.instance.SendCommand("action=change_rank|memberID=" + _memberID + "|rank=" + _new_rank);
    }

    public void OnClick_AcceptAlliance(int _nationID, string _nationName)
    {
        // GB-Localization
        // "Accept the invitation to form an alliance between " + GameData.instance.nationName + " and " + _nationName + "?"
        // "Accept the invitation to form an alliance between {[NATION_NAME1]} and {[NATION_NAME2]}?"
        Requestor.Activate((int)RequestorTask.AcceptAllianceInvitation, _nationID, this, 
            LocalizationManager.GetTranslation("accept_alliance_between_two_nations_prompt")
                .Replace("{[NATION_NAME1]}", GameData.instance.nationName)
                .Replace("{[NATION_NAME2]}", _nationName), 
            LocalizationManager.GetTranslation("Generic Text/yes_word"), LocalizationManager.GetTranslation("Generic Text/no_word"));
    }

    public void OnClick_DeclineAlliance(int _nationID, string _nationName)
    {
        // GB-Localization
        // "Decline the invitation to form an alliance between " + GameData.instance.nationName + " and " + _nationName + "?"
        // "Decline the invitation to form an alliance between {[NATION_NAME1]} and {[NATION_NAME2]}?"
        Requestor.Activate((int)RequestorTask.DeclineAllianceInvitation, _nationID, this,
            LocalizationManager.GetTranslation("decline_alliance_between_two_nations_prompt")
                .Replace("{[NATION_NAME1]}", GameData.instance.nationName)
                .Replace("{[NATION_NAME2]}", _nationName),
            LocalizationManager.GetTranslation("Generic Text/yes_word"), LocalizationManager.GetTranslation("Generic Text/no_word"));
    }

    public void OnClick_WithdrawAlliance(int _nationID, string _nationName)
    {
        // GB-Localization
        // "Withdraw the alliance invitation that was sent to " + _nationName + "?"
        // "Withdraw the alliance invitation that was sent to {[NATION_NAME1]}?"
        Requestor.Activate((int)RequestorTask.WithdrawAllianceInvitation, _nationID, this, 
            LocalizationManager.GetTranslation("withdraw_alliance_invitation")
                .Replace("{[NATION_NAME1]}", _nationName), 
            LocalizationManager.GetTranslation("Generic Text/yes_word"), LocalizationManager.GetTranslation("Generic Text/no_word"));
    }

    public void OnClick_BreakAlliance(int _nationID, string _nationName)
    {
        // GB-Localization
        // "Break " + GameData.instance.nationName + "'s alliance with " + _nationName + "?"
        // "Break {[NATION_NAME1]}'s alliance with {[NATION_NAME2]}?"
        Requestor.Activate((int)RequestorTask.BreakAlliance, _nationID, this, 
            LocalizationManager.GetTranslation("break_alliance_between_two_nations_prompt")
                .Replace("{[NATION_NAME1]}", GameData.instance.nationName)
                .Replace("{[NATION_NAME2]}", _nationName),
            LocalizationManager.GetTranslation("Generic Text/yes_word"), LocalizationManager.GetTranslation("Generic Text/no_word"));
    }

    public void OnClick_InviteUnite(int _nationID, string _nationName)
    {
        // Set up the invite unite panel.
        InviteUnitePanel.instance.Init(_nationID, _nationName);

        // Show the invite unite panel.
        GameGUI.instance.OpenInviteUniteDialog();
/*
       Requestor.Activate((int)RequestorTask.SendUniteInvitation, _nationID, this, 
            LocalizationManager.GetTranslation("Nation Panel/confirm_invite_unite").Replace("{[NATION_NAME]}", _nationName).Replace("{[COST]}", GameData.instance.uniteCost.ToString()), 
            LocalizationManager.GetTranslation("Generic Text/yes_word"), LocalizationManager.GetTranslation("Generic Text/no_word"));
            */
    }

    public void OnClick_WithdrawUnite(int _nationID, string _nationName)
    {
        // Send event_withdraw_unite event to the server.
        Network.instance.SendCommand("action=event_withdraw_unite|targetNationID=" + _nationID);
    }

    public void OnClick_AcceptUnite(int _nationID, string _nationName)
    {
       Requestor.Activate((int)RequestorTask.AcceptUniteInvitation, _nationID, this, 
            LocalizationManager.GetTranslation("Nation Panel/confirm_unite").Replace("{[NATION_NAME]}", _nationName).Replace("{[COST]}", GameData.instance.uniteCost.ToString()), 
            LocalizationManager.GetTranslation("Generic Text/yes_word"), LocalizationManager.GetTranslation("Generic Text/no_word"));
    }

    public void OnClick_DeclineUnite(int _nationID, string _nationName)
    {
        // Send event_decline_unite event to the server.
        Network.instance.SendCommand("action=event_decline_unite|targetNationID=" + _nationID);
    }

    public void OnClick_Migrate()
    {
        // GB-Localization
        // "Only a sovereign or cosovereign can order a nation to migrate."
        if (GameData.instance.userRank > GameData.RANK_COSOVEREIGN) {
            Requestor.Activate(-1, 0, null, LocalizationManager.GetTranslation("only_sovereign_or_co_can_migrate_nation"), LocalizationManager.GetTranslation("Generic Text/okay"), "");
        } else {
            int cost = (Time.unscaledTime >= GameData.instance.nextFreeMigrationTime) ? 0 : GameData.instance.migrationCost;
            if (GameData.instance.credits >= cost) {
                Requestor.Activate((int)RequestorTask.Migrate, 0, this, 
                    LocalizationManager.GetTranslation("Nation Panel/migrate_type_question"),
                    LocalizationManager.GetTranslation("Nation Panel/migrate_type_entire_nation"), LocalizationManager.GetTranslation("Nation Panel/migrate_type_colony"), Requestor.FLAG_CLOSE_BUTTON);
            } else {
                GameGUI.instance.RequestBuyCredits();
            }
        }
    }

    public void OnClick_ResetAdvances()
    {
        // TESTING
        //ReportDialog.instance.Activate(GameData.instance.userID, GameData.instance.username, "Test text");

        if (GameData.instance.userRank > GameData.RANK_COSOVEREIGN) {
            // Only a nation's sovereign or co-sovereign can do this.
            Requestor.Activate(-1, 0, null, LocalizationManager.GetTranslation("only_sov_or_co_can"), LocalizationManager.GetTranslation("Generic Text/okay"), "");
        } else {
            if (GameData.instance.credits >= GameData.instance.resetAdvancesPrice) {
                // "Do you want to free up all of {[NATION_NAME1]}'s Advance Points so that they can be assigned to different advances?"
                Requestor.Activate((int)RequestorTask.ResetAdvances, 0, this, 
                    LocalizationManager.GetTranslation("Nation Panel/are_you_sure_you_want_to_reset_advances")
                        .Replace("{[NATION_NAME1]}", GameData.instance.nationName),
                    LocalizationManager.GetTranslation("Generic Text/yes_word"), LocalizationManager.GetTranslation("Generic Text/no_word"));
            } else {
                GameGUI.instance.RequestBuyCredits();
            }
        }
    }

    public void OnClick_RebirthInfo()
    {
        string text = LocalizationManager.GetTranslation("Connect Panel/rebirth_info") // "Each nation, once sufficiently advanced, undergoes revolution and rebirth. After rebirth a nation reverts to a more primitive stage and must rebuild its advances. But each rebirth also gives the nation a free bonus level, allowing it to grow more powerful with each rebirth cycle. At level {countdown_start_level}, {nation_name}'s rebirth timer will start at {rebirth_countdown_start} and begin counting down. As {nation_name}'s level increases the timer will count down more quickly. Once the timer has started, we can choose to wait until the end of the countdown or we can rebirth early at any time. We can also delay rebirth by purchasing credits -- each 25 credits purchased will add 1 to the countdown. (Up to a maximum of +{max_per_cycle} per rebirth cycle. We can still add {remaining_this_cycle} this cycle.) After our next rebirth, {nation_name}'s level bonus will increase to +{next_rebirth_level_bonus}."
            .Replace("{nation_name}", "" + GameData.instance.nationName)
            .Replace("{rebirth_countdown_start}", "" + GameData.instance.rebirth_countdown_start)
            .Replace("{countdown_start_level}", "" + GameData.instance.rebirth_available_level)
            .Replace("{max_per_cycle}", "" + GameData.instance.maxRebirthCountdownPurchased)
            .Replace("{remaining_this_cycle}", "" + ((int)Mathf.Max(0, GameData.instance.maxRebirthCountdownPurchased - GameData.instance.rebirth_countdown_purchased)))
            .Replace("{next_rebirth_level_bonus}", "" + Mathf.Min(GameData.instance.rebirth_level_bonus + GameData.instance.levelBonusPerRebirth, GameData.instance.maxRebirthLevelBonus));

        Requestor.Activate(0, 0, null, text, LocalizationManager.GetTranslation("Generic Text/okay"), "", Requestor.FLAG_ALIGN_LEFT);
    }

    public void OnClick_Rebirth()
    {
        if (GameData.instance.userRank > GameData.RANK_COSOVEREIGN) {
            // Only a nation's sovereign or co-sovereign can do this.
            Requestor.Activate(-1, 0, null, LocalizationManager.GetTranslation("only_sov_or_co_can"), LocalizationManager.GetTranslation("Generic Text/okay"), "");
        } else {
            // "In the grip of revolution, our nation will plunge back to the primitive era of level {[LEVEL]}. But the rebirth will also make {[NATION_NAME1]} capable of advancing more rapidly, and reaching higher levels in the future than it can now. Are you sure you want to do this?"
            Requestor.Activate((int)RequestorTask.Rebirth, 0, this, 
                LocalizationManager.GetTranslation("Nation Panel/are_you_sure_you_want_to_rebirth")
                    .Replace("{[NATION_NAME1]}", GameData.instance.nationName)
                    .Replace("{[LEVEL]}", "" + (GameData.instance.rebirth_level_bonus + GameData.instance.levelBonusPerRebirth + GameData.instance.rebirthToBaseLevel)),
                LocalizationManager.GetTranslation("Generic Text/yes_word"), LocalizationManager.GetTranslation("Generic Text/no_word"));
        }
    }

    public void OnClick_CustomizeAppearance()
    {
        GameGUI.instance.OpenCustomizePanel();
    }

    public void OnClick_TradeIn()
    {
        if (GameData.instance.userRank > GameData.RANK_COSOVEREIGN) {
            // Only a nation's sovereign or co-sovereign can do this.
            Requestor.Activate(-1, 0, null, LocalizationManager.GetTranslation("only_sov_or_co_can"), LocalizationManager.GetTranslation("Generic Text/okay"), "");
        } else {
            // "Trade in {[NATION_NAME]}'s ${[WINNINGS_AMOUNT]} winnings for ${[WINNINGS_CREDITS]} <sprite=2>?"
            Requestor.Activate((int)RequestorTask.TradeIn, 0, this, 
                LocalizationManager.GetTranslation("Nation Panel/confirm_trade_in")
                    .Replace("{[NATION_NAME]}", GameData.instance.nationName)
                    .Replace("{[WINNINGS_AMOUNT]}", (GameData.instance.prizeMoney / 100.0).ToString("N2"))
                    .Replace("{[WINNINGS_CREDITS]}", "" + (GameData.instance.prizeMoney * GameData.instance.creditsPerCentTradedIn)),
                LocalizationManager.GetTranslation("Generic Text/yes_word"), LocalizationManager.GetTranslation("Generic Text/no_word"));
        }
    }

    public void OnClick_CashOut()
    {
        if (GameData.instance.userRank > GameData.RANK_COSOVEREIGN) {
            // Only a nation's sovereign or co-sovereign can do this.
            Requestor.Activate(-1, 0, null, LocalizationManager.GetTranslation("only_sov_or_co_can"), LocalizationManager.GetTranslation("Generic Text/okay"), "");
        } else {
            GameGUI.instance.OpenCashOutPanel();
        }
    }

	public void OnClick_PrizePolicyGoogle()
    {
		Application.OpenURL("https://warofconquest.com/2019/12/02/cash-prizes-and-the-google-play-store/");
	}

    public void OnEndEdit_Password()
    {
        if (passwordInputField.text == "")
        {
            // Do not allow blank password.
            passwordInputField.text = GameData.instance.nationPassword;
        }
        else
        {
            string password = passwordInputField.text;

            // Remove any control characters from the string
            password = GameGUI.RemoveControlCharacters(password);

            // Replace any | characters
            password = password.Replace("|", "");

            // Display the modified password
            passwordInputField.text = password;

            // Send the new password to the server.
            if (!processing_info_event) Network.instance.SendCommand("action=set_nation_password|password=" + password);
        }
    }

    public void OnChange_BlockInvitations()
    {
        // Record the flag value, and send flags to the server.
        GameData.instance.SetNationFlag(GameData.NationFlags.BLOCK_NATION_CHAT_INVITATIONS, blockInvitationsToggle.isOn);
        if (!processing_info_event) GameData.instance.SendNationFlags();
    }

    public void OnChange_Incognito()
    {
        if (processing_info_event) 
        {
            GameData.instance.SetNationFlag(GameData.NationFlags.INCOGNITO, incognitoToggle.isOn);
            return;
        }

        if (GameData.instance.userRank > GameData.RANK_COSOVEREIGN) 
        {
            // Only a nation's sovereign or co-sovereign can do this.
            Requestor.Activate(-1, 0, null, LocalizationManager.GetTranslation("only_sov_or_co_can"), LocalizationManager.GetTranslation("Generic Text/okay"), "");
            incognitoToggle.isOn = GameData.instance.GetNationFlag(GameData.NationFlags.INCOGNITO);
        }
        else 
        {
            if (GameData.instance.GetNationFlag(GameData.NationFlags.INCOGNITO)) 
            {
                // "{[NATION_NAME]} will no longer be incognito, and its identity will be visible to all. Is this what you want to do?"
                Requestor.Activate((int)RequestorTask.IncognitoOff, 0, this, 
                    LocalizationManager.GetTranslation("Nation Panel/incognito_off_request")
                        .Replace("{[NATION_NAME]}", GameData.instance.nationName),
                    LocalizationManager.GetTranslation("Generic Text/yes_word"), LocalizationManager.GetTranslation("Generic Text/no_word"));
            }
            else 
            {
                // "{[NATION_NAME]}'s identity will be hidden from all other nations. We will be burning {[percentage]}% of our energy to remain incognito, leaving us less to maintain defenses, and we must remain incognito for at least {[min_minutes]} minutes. Is this what you want to do?"
                Requestor.Activate((int)RequestorTask.IncognitoOn, 0, this, 
                    LocalizationManager.GetTranslation("Nation Panel/incognito_on_request")
                        .Replace("{[NATION_NAME]}", GameData.instance.nationName)
                        .Replace("{[percentage]}", string.Format("{0:n0}", (int)(GameData.instance.incognitoEnergyBurn * 100)))
                        .Replace("{[min_minutes]}", string.Format("{0:n0}", (int)(GameData.instance.minIncognitoPeriod / 60))),
                    LocalizationManager.GetTranslation("Generic Text/yes_word"), LocalizationManager.GetTranslation("Generic Text/no_word"));
            }
        }
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
        membersContentArea.SetActive(selectedPanelTab == membersPanelTab);
        alliesContentArea.SetActive(selectedPanelTab == alliesPanelTab);
        statsContentArea.SetActive(selectedPanelTab == statsPanelTab);
        orbsContentArea.SetActive(selectedPanelTab == orbsPanelTab);
        resourcesContentArea.SetActive(selectedPanelTab == resourcesPanelTab);
        settingsContentArea.SetActive(selectedPanelTab == settingsPanelTab);

        // If the stats subpanel has been activated and the stats haven't been fetched in a long time, request them.
        if (statsContentArea.activeSelf && ((Time.unscaledTime - prev_request_stats_time) >= REQUEST_STATS_INTERVAL)) {
            Network.instance.SendCommand("action=request_stats");
            prev_request_stats_time = Time.unscaledTime;
        }

        // If the orb records haven't been updated within the UPDATE_ORBS_INTERVAL, request update now.
        if ((selectedPanelTab == orbsPanelTab) && ((Time.unscaledTime - prev_update_orbs_time) >= UPDATE_ORBS_INTERVAL)) {
            Network.instance.SendCommand("action=get_nation_orbs");
        }

        // If we're showing the resources tab and the resources list has been modified since it was last shown, update the list.
        if ((selectedPanelTab == resourcesPanelTab) && resourcesListModified) {
            RefreshResourcesList();
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

    public void RequestorResponse(int _task, int _data, Requestor.RequestorButton _result)
    {
        if ((RequestorTask)_task == RequestorTask.RemoveMemberfromNation)
        {
            if (_result == Requestor.RequestorButton.LeftButton)
            {
                if (removeMemberUserID == GameData.instance.userID)
                {
                    if (GameData.instance.nationID == GameData.instance.homeNationID)
                    {
                        // Have the player select the nation to join, and enter its password.
                        GameGUI.instance.OpenJoinNationDialog();
                    }
                    else
                    {
                        // GB-Localization
                        // "Do you wish to return to your own nation, " + GameData.instance.homeNationName + ", or join another nation?"
                        // "Do you wish to return to your own nation, {[NATION_NAME1]}, or join another nation?"
                        // Ask the player whether they would like to return to their own nation, or join another nation.
                        Requestor.Activate((int)RequestorTask.ReturnToNationOrJoinAnother, _data, this, 
                            LocalizationManager.GetTranslation("return_to_your_nation_prompt")
                                .Replace("{[NATION_NAME1]}", GameData.instance.homeNationName), 
                            LocalizationManager.GetTranslation("Generic Text/return_word"), LocalizationManager.GetTranslation("Generic Text/join_word"));
                    }
                }
                else
                {
                    // Send message to sever, to remove the member from this nation.
                    Network.instance.SendCommand("action=send_to_home_nation|memberID=" + removeMemberUserID);
                }
            }
        }
        else if ((RequestorTask)_task == RequestorTask.ReturnToNationOrJoinAnother)
        {
            if (_result == Requestor.RequestorButton.LeftButton)
            {
                // Send message to server, to have this player return to their home nation.
                Network.instance.SendCommand("action=send_to_home_nation|memberID=" + removeMemberUserID);
            }
            else if (_result == Requestor.RequestorButton.RightButton)
            {
                // Have the player select the nation to join, and enter its password.
                GameGUI.instance.OpenJoinNationDialog();
            }
        }

        // Alliance and unite related requests
        if (_result == Requestor.RequestorButton.LeftButton)
        {
            if ((RequestorTask)_task == RequestorTask.BreakAlliance)
            {
                // Send event_break_alliance event to the server.
                Network.instance.SendCommand("action=event_break_alliance|targetNationID=" + _data);
            }
            else if ((RequestorTask)_task == RequestorTask.AcceptAllianceInvitation)
            {
                // Send event_accept_alliance event to the server.
                Network.instance.SendCommand("action=event_accept_alliance|targetNationID=" + _data);
            }
            else if ((RequestorTask)_task == RequestorTask.DeclineAllianceInvitation)
            {
                // Send event_decline_alliance event to the server.
                Network.instance.SendCommand("action=event_decline_alliance|targetNationID=" + _data);
            }
            else if ((RequestorTask)_task == RequestorTask.WithdrawAllianceInvitation)
            {
                // Send event_withdraw_alliance event to the server.
                Network.instance.SendCommand("action=event_withdraw_alliance|targetNationID=" + _data);
            }
            else if ((RequestorTask)_task == RequestorTask.SendUniteInvitation)
            {
                // Send event_request_unite event to the server.
                Network.instance.SendCommand("action=event_request_unite|targetNationID=" + _data);
            }
            else if ((RequestorTask)_task == RequestorTask.AcceptUniteInvitation)
            {
                // Send event_accept_unite event to the server.
                Network.instance.SendCommand("action=event_accept_unite|targetNationID=" + _data);
            }
        }

        if ((RequestorTask)_task == RequestorTask.Migrate)
        {
            if (_result == Requestor.RequestorButton.LeftButton)
            {
                // GameData.instance.nationName + " will abandon all of its land and resettle in an area of the world close to where you are viewing now. Are you sure you want " + GameData.instance.nationName + " to migrate?"
                // "{[NATION_NAME1]} will abandon all of its land and resettle in an area of the world close to where you are viewing now. Are you sure you want {[NATION_NAME1]} to migrate?"
                Requestor.Activate((int)RequestorTask.MigrateEntireNation, 0, this, 
                    LocalizationManager.GetTranslation("Nation Panel/are_you_sure_you_want_to_migrate_nation")
                        .Replace("{[NATION_NAME1]}", GameData.instance.nationName),
                    LocalizationManager.GetTranslation("Generic Text/yes_word"), LocalizationManager.GetTranslation("Generic Text/no_word"));
            }
            else if (_result == Requestor.RequestorButton.RightButton)
            {
                if ((Time.unscaledTime < GameData.instance.nextFreeMigrationTime) && (GameData.instance.credits < GameData.instance.migrationCost))
                {
                    // Bring up UI for buying credits.
                    GameGUI.instance.RequestBuyCredits();
                }
                else 
                {
                    // Send migrate event to the server, for migrating a small colony.
                    Network.instance.SendCommand("action=migrate|colony=1");
                }
            }
        }

        if (((RequestorTask)_task == RequestorTask.MigrateEntireNation) && (_result == Requestor.RequestorButton.LeftButton))
        {
            if ((Time.unscaledTime < GameData.instance.nextFreeMigrationTime) && (GameData.instance.credits < GameData.instance.migrationCost))
            {
                // Bring up UI for buying credits.
                GameGUI.instance.RequestBuyCredits();
            }
            else 
            {
                // Send migrate event to the server, for migrating the entire nation.
                Network.instance.SendCommand("action=migrate|colony=0");
            }
        }

        if (((RequestorTask)_task == RequestorTask.ResetAdvances) && (_result == Requestor.RequestorButton.LeftButton))
        {
            if (GameData.instance.credits < GameData.instance.resetAdvancesPrice)
            {
                // Bring up UI for buying credits.
                GameGUI.instance.RequestBuyCredits();
            }
            else 
            {
                // Send reset advances event to the server.
                Network.instance.SendCommand("action=reset_advances");
            }
        }

        if (((RequestorTask)_task == RequestorTask.Rebirth) && (_result == Requestor.RequestorButton.LeftButton))
        {
            // Send rebirth event to the server.
            Network.instance.SendCommand("action=rebirth");
        }

        if (((RequestorTask)_task == RequestorTask.IncognitoOn) || ((RequestorTask)_task == RequestorTask.IncognitoOff))
        {
            if (_result == Requestor.RequestorButton.LeftButton)
            {
                 // Record the flag value, and send flags to the server.
                GameData.instance.SetNationFlag(GameData.NationFlags.INCOGNITO, incognitoToggle.isOn);
                GameData.instance.SendNationFlags();
            }
            else
            {
                // Pretend that we're processing the info event, so the below changes will not result in messages to the server.
                processing_info_event = true;

                // Reset the checkbox to reflect the nation's incognot state.
                incognitoToggle.isOn = GameData.instance.GetNationFlag(GameData.NationFlags.INCOGNITO);

                processing_info_event = false;
            }
        }

        if (((RequestorTask)_task == RequestorTask.TradeIn) && (_result == Requestor.RequestorButton.LeftButton))
        {
            // Send Trade In event to the server.
            Network.instance.SendCommand("action=trade_in");
        }
    }

    public void OnClick_MembersTab()
    {
        TabPressed(membersPanelTab);
    }

    public void OnClick_AlliesTab()
    {
        TabPressed(alliesPanelTab);
    }

    public void OnClick_StatsTab()
    {
        TabPressed(statsPanelTab);
    }

    public void OnClick_OrbsTab()
    {
        TabPressed(orbsPanelTab);
    }

    public void OnClick_ResourcesTab()
    {
        TabPressed(resourcesPanelTab);
    }

    public void OnClick_SettingsTab()
    {
        TabPressed(settingsPanelTab);
    }

    public void OnClick_StatTech()
    {
        StatDetailsPanel.instance.ActivateForBonus(TechData.Bonus.TECH);
    }

    public void OnClick_StatBio()
    {
        StatDetailsPanel.instance.ActivateForBonus(TechData.Bonus.BIO);
    }

    public void OnClick_StatPsi()
    {
        StatDetailsPanel.instance.ActivateForBonus(TechData.Bonus.PSI);
    }

    public void OnClick_StatEnergy()
    {
        StatDetailsPanel.instance.ActivateForBonus(TechData.Bonus.ENERGY_RATE);
    }

    public void OnClick_StatManpower()
    {
        StatDetailsPanel.instance.ActivateForBonus(TechData.Bonus.MANPOWER_RATE);
    }

    public void OnClick_StatXP()
    {
        StatDetailsPanel.instance.ActivateForBonus(TechData.Bonus.XP_MULTIPLIER);
    }
}
