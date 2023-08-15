using UnityEngine;
using UnityEngine.UI;
using System;
using System.Collections;
using System.Collections.Generic;
using I2.Loc;

public class ModeratorPanel : MonoBehaviour, RequestorListener
{
    enum Action
    {
        NO_ACTION = 0,
        WARN = 1,
        CHAT_BAN = 2,
        GAME_BAN = 3,
        WARN_FILER = 4,
        CHAT_BAN_FILER = 5
    }

    public static ModeratorPanel instance;

    public Sprite inactivePanelTabSprite, activePanelTabSprite;
    public GameObject complaintsPanelTab, infoPanelTab;
    public GameObject complaintsContentArea, infoContentArea;

    public Text filedByText, filedAgainstText, complaintText, indexText;
    public Dropdown actionDropdown;
    public InputField banDaysInputField, messageInputField, logInputField;
    List<string> actions_list;
    ComplaintData complaintData = null;
    Action action;
    int ban_days;

    Color tabTextColor_Inctive = new Color(0.65f, 0.65f, 0.65f);
    Color tabTextColor_Active = new Color(1.0f, 1.0f, 1.0f);
    Color tabTextColor_Alert = new Color(0, 0, 0);

    GameObject selectedPanelTab = null;

    public ModeratorPanel()
    {
        instance = this;
    }

    public void Start()
    {
        // Turn off the alert image of each panel tab
        complaintsPanelTab.transform.GetChild(0).gameObject.SetActive(false);
        infoPanelTab.transform.GetChild(0).gameObject.SetActive(false);
        
        // Set text of each tab initially to inactive color.
        complaintsPanelTab.transform.GetChild(1).GetComponent<Text>().color = tabTextColor_Inctive;
        infoPanelTab.transform.GetChild(1).GetComponent<Text>().color = tabTextColor_Inctive;

        // Begin with the complaints tab active.
        TabPressed(complaintsPanelTab);

        // Set up actions dropdown
        actions_list = new List<string>();
        actions_list.Add(LocalizationManager.GetTranslation("Moderator Panel/action_no_action"));
        actions_list.Add(LocalizationManager.GetTranslation("Moderator Panel/action_warning"));
        actions_list.Add(LocalizationManager.GetTranslation("Moderator Panel/action_chat_ban"));
        actions_list.Add(LocalizationManager.GetTranslation("Moderator Panel/action_game_ban"));
        actions_list.Add(LocalizationManager.GetTranslation("Moderator Panel/action_warn_filer"));
        actions_list.Add(LocalizationManager.GetTranslation("Moderator Panel/action_chat_ban_filer"));

        actionDropdown.ClearOptions();
        actionDropdown.AddOptions(actions_list);
        actionDropdown.value = 0;
    }

    public void OnEnable()
    {
        if ((selectedPanelTab == complaintsPanelTab) && (complaintData == null)) 
        {
            // Fetch the first complaint from the server.
            Network.instance.SendCommand("action=mod_fetch_complaint|skip=-1");
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
        complaintsContentArea.SetActive(selectedPanelTab == complaintsPanelTab);
        infoContentArea.SetActive(selectedPanelTab == infoPanelTab);

        if (gameObject.activeInHierarchy && (selectedPanelTab == complaintsPanelTab) && (complaintData == null)) 
        {
            // Fetch the first complaint from the server.
            Network.instance.SendCommand("action=mod_fetch_complaint|skip=-1");
        }
    }

    public void ClearComplaint()
    {
        filedByText.text = "";
        filedAgainstText.text = "";
        complaintText.text = "There is no complaint to be processed.";

        // Display that no complaints exist.
        indexText.text = "0/0";

        // Reset action inputs
        actionDropdown.value = 0;
        banDaysInputField.text = "";
        messageInputField.text = "";
        logInputField.text = "";

        // Clear the ComplaintData.
        complaintData = null;
    }

    public void DisplayComplaint(int _complaintIndex, int _complaintCount, ComplaintData _complaintData)
    {
        // Display info about the user who filed the report.
        filedByText.text = _complaintData.reporter_userName + " (" + _complaintData.reporter_userID + ") of nation " + _complaintData.reporter_nationName + " (" + _complaintData.reporter_nationID + "), E-mail: " + _complaintData.reporter_email;
        filedByText.text += "\n" + LocalizationManager.GetTranslation("Moderator Panel/complainst_against") + ": " + _complaintData.reporter_num_complaints_against + ", " + LocalizationManager.GetTranslation("Moderator Panel/complaints_made") + ": " + _complaintData.reporter_num_complaints_by;
        filedByText.text += "\n" + LocalizationManager.GetTranslation("Moderator Panel/warnings") + ": " + _complaintData.reporter_num_warnings_sent + ", " + LocalizationManager.GetTranslation("Moderator Panel/chat_bans") + ": " + _complaintData.reporter_num_chat_bans + ", " + LocalizationManager.GetTranslation("Moderator Panel/game_bans") + ": " + _complaintData.reporter_num_game_bans;

        if ((_complaintData.reporter_game_ban_days > 0) || (_complaintData.reporter_chat_ban_days > 0)) {
            filedByText.text += "\nDays now banned from chat: " + _complaintData.reporter_chat_ban_days + ", from game: " + _complaintData.reporter_game_ban_days;
        }

        // Display info about the user whom the report was filed against.
        filedAgainstText.text = _complaintData.reported_userName + " (" + _complaintData.reported_userID + ") of nation " + _complaintData.reported_nationName + " (" + _complaintData.reported_nationID + "), E-mail: " + _complaintData.reported_email;
        filedAgainstText.text += "\n" + LocalizationManager.GetTranslation("Moderator Panel/complainst_against") + ": " + _complaintData.reported_num_complaints_against + ", " + LocalizationManager.GetTranslation("Moderator Panel/complaints_made") + ": " + _complaintData.reported_num_complaints_by;
        filedAgainstText.text += "\n" + LocalizationManager.GetTranslation("Moderator Panel/warnings") + ": " + _complaintData.reported_num_warnings_sent + ", " + LocalizationManager.GetTranslation("Moderator Panel/chat_bans") + ": " + _complaintData.reported_num_chat_bans + ", " + LocalizationManager.GetTranslation("Moderator Panel/game_bans") + ": " + _complaintData.reported_num_game_bans;

        if ((_complaintData.reported_game_ban_days > 0) || (_complaintData.reported_chat_ban_days > 0)) {
            filedAgainstText.text += "\nDays now banned from chat: " + _complaintData.reported_chat_ban_days + ", from game: " + _complaintData.reported_game_ban_days;
        }

        // Display info about the complaint.
        DateTime dateTime = UnixTimeStampToDateTime(_complaintData.timestamp);
        complaintText.text = LocalizationManager.GetTranslation("Moderator Panel/issue") + ": " + _complaintData.issue + "\n" +
            LocalizationManager.GetTranslation("Moderator Panel/text") + ": \"" + _complaintData.text + "\"\n" + dateTime.ToString("dddd, dd MMMM yyyy HH:mm:ss");

        // Display index of this complaint and count.
        indexText.text = (_complaintIndex + 1) + "/" + _complaintCount;

        // Reset action inputs
        actionDropdown.value = 0;
        messageInputField.text = "";
        logInputField.text = "";

        // Use heuristic to come up with default ban duration based on number of prior chat bans.
        if (_complaintData.reported_num_chat_bans <= 1) {
            banDaysInputField.text = (_complaintData.reported_chat_ban_days + 1).ToString();
        } else if (_complaintData.reported_num_chat_bans <= 5) {
            banDaysInputField.text = (_complaintData.reported_chat_ban_days + 2).ToString();
        } else if (_complaintData.reported_num_chat_bans <= 10) {
            banDaysInputField.text = (_complaintData.reported_chat_ban_days + 3).ToString();
        } else {
            banDaysInputField.text = (_complaintData.reported_chat_ban_days + 4).ToString();
        }

        // Store the ComplaintData being shown.
        complaintData = _complaintData;
    }

    public void OnClick_Resolve()
    {
        if (complaintData == null) {
            return;
        }

        action = (Action)actionDropdown.value;
        ban_days = 0;

        if ((action == Action.CHAT_BAN) || (action == Action.GAME_BAN) || (action == Action.CHAT_BAN_FILER))
        {
            if ((banDaysInputField.text == "") || (!Int32.TryParse(banDaysInputField.text, out ban_days)))
            {
                Requestor.Activate(0, 0, null, LocalizationManager.GetTranslation("Moderator Panel/msg_ban_days"), LocalizationManager.GetTranslation("Generic Text/okay"), "");
                return;
            }
        }

        String text = "Error, invalid action";
        switch (action)
        {
            case Action.NO_ACTION: text = LocalizationManager.GetTranslation("Moderator Panel/msg_confirm_no_action"); break;
            case Action.WARN: text = LocalizationManager.GetTranslation("Moderator Panel/msg_confirm_warn_reported").Replace("{username}", complaintData.reported_userName); break;
            case Action.CHAT_BAN: text = LocalizationManager.GetTranslation("Moderator Panel/msg_confirm_chat_ban_reported").Replace("{username}", complaintData.reported_userName).Replace("{days}", "" + ban_days); break;
            case Action.GAME_BAN: text = LocalizationManager.GetTranslation("Moderator Panel/msg_confirm_game_ban_reported").Replace("{username}", complaintData.reported_userName).Replace("{days}", "" + ban_days); break;
            case Action.WARN_FILER: text = LocalizationManager.GetTranslation("Moderator Panel/msg_confirm_warn_reporter").Replace("{username}", complaintData.reporter_userName); break;
            case Action.CHAT_BAN_FILER: text = LocalizationManager.GetTranslation("Moderator Panel/msg_confirm_chat_ban_reporter").Replace("{username}", complaintData.reporter_userName).Replace("{days}", "" + ban_days); break;
        }

        Requestor.Activate(0, 0, this, text, LocalizationManager.GetTranslation("Generic Text/yes_word"), LocalizationManager.GetTranslation("Generic Text/no_word"));
    }

    public void OnClick_Skip()
    {
        if (complaintData == null) {
            return;
        }

        // Fetch the first complaint from the server.
        Network.instance.SendCommand("action=mod_fetch_complaint|skip=" + ((complaintData == null) ? "-1" : "" + complaintData.ID));

        // Clear the current complaint.
        ClearComplaint();
    }

    public void RequestorResponse(int _task, int _data, Requestor.RequestorButton _result)
    {
        if (_result == Requestor.RequestorButton.LeftButton)
        {
            // Send message to server to resolve this complaint.
            Network.instance.SendCommand("action=mod_resolve_complaint|ID=" + complaintData.ID + "|act=" + (int)action + "|ban_days=" + ban_days + "|message=" + messageInputField.text + "|log=" + logInputField.text);

            // Clear the current complaint.
            ClearComplaint();
        }
    }

    public void OnClick_FiledBy()
    {
        if (complaintData != null) {
            Chat.instance.PrefillChatInputField(complaintData.reporter_userName + " of " + complaintData.reporter_nationName);
        }
    }

    public void OnClick_FiledAgainst()
    {
        if (complaintData != null) {
            Chat.instance.PrefillChatInputField(complaintData.reported_userName + " of " + complaintData.reported_nationName);
        }
    }

    public void OnClick_ComplaintsTab()
    {
        TabPressed(complaintsPanelTab);
    }

    public void OnClick_InfoTab()
    {
        TabPressed(infoPanelTab);
    }

    public static DateTime UnixTimeStampToDateTime( double unixTimeStamp )
    {
        // Unix timestamp is seconds past epoch
        System.DateTime dtDateTime = new DateTime(1970,1,1,0,0,0,0,System.DateTimeKind.Utc);
        dtDateTime = dtDateTime.AddSeconds( unixTimeStamp ).ToLocalTime();
        return dtDateTime;
    }
}
