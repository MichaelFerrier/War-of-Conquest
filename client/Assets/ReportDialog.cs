using UnityEngine;
using UnityEngine.UI;
using System;
using System.Collections;
using System.Collections.Generic;
using I2.Loc;

public class ReportDialog : MonoBehaviour
{
    public static ReportDialog instance;

    public Button button1, button2;
    public GameObject ReportDialogBaseObject;
    public Text text;

    public Dropdown issueDropdown;

    string username, reported_text;
    int userID;

    // GB-Localization
    List<string> issues_list;

    public ReportDialog()
    {
        instance = this;
    }
    
    // Use this for initialization
    void Start ()
    {
        // GB-Localization
        issues_list = new List<string>();
        issues_list.Add(LocalizationManager.GetTranslation("Report Issues Dialog/select"));
        issues_list.Add(LocalizationManager.GetTranslation("Report Issues Dialog/bad_language")); // "Swearing or bad language"
        issues_list.Add(LocalizationManager.GetTranslation("Report Issues Dialog/discriminatory_language")); // "Racist or discriminatory language"
        issues_list.Add(LocalizationManager.GetTranslation("Report Issues Dialog/harassment")); // "Personal attacks / harassment"
        issues_list.Add(LocalizationManager.GetTranslation("Report Issues Dialog/bypassing_chat_filter")); // "Bypassing the chat filter"
        issues_list.Add(LocalizationManager.GetTranslation("Report Issues Dialog/spamming_chat")); // "Spamming chat"
        issues_list.Add(LocalizationManager.GetTranslation("Report Issues Dialog/inappropriate_name")); // "Inappropriate username or nation name"
        issues_list.Add(LocalizationManager.GetTranslation("Report Issues Dialog/talk_about_hacking")); // "Dicussion of hacking"
        issues_list.Add(LocalizationManager.GetTranslation("Report Issues Dialog/threats")); // "Threatening violence or illegal acts"
        issues_list.Add(LocalizationManager.GetTranslation("Report Issues Dialog/posting_commercial_link")); // "Posting a commercial link"

        issueDropdown.ClearOptions();
        issueDropdown.AddOptions(issues_list);
        issueDropdown.value = 0;
    }

    public void Activate(int _userID, string _username, string _reported_text)
    {
        // GB-Localization
        username = _username;
        userID = _userID;
        reported_text = _reported_text;

        // Show the ReportDialogBaseObject.
        ReportDialogBaseObject.SetActive(true);
        ReportDialogBaseObject.GetComponent<GUITransition>().StartTransition(0, 1, 1, 1, false);

        // Set the dialog text.
        // "If you would like to report {[USER_NAME]} for violating the player guidelines, select the option that best describes the nature of the violation:";
        text.text = LocalizationManager.GetTranslation("Report Issues Dialog/report_user_for_violating_player_guidelines").Replace("{[USER_NAME]}", _username);
     
        // Activate the initial blank option in the issue menu.
        issueDropdown.value = 0;

        // Enable the buttons
        button1.enabled = true;
        button2.enabled = true;
    }

    public void OnClick_Submit()
    {
        // GB-Localization
        if (issueDropdown.value == 0)
        {
            // Display requestor stating that an issue needs to be selected.
            // "Please select the option that best describes how {[USER_NAME]} violated the chat guidelines."
            Requestor.Activate(0, 0, null, LocalizationManager.GetTranslation("Report Issues Dialog/select_how_user_violated_guidelines").Replace("{[USER_NAME]}", username), LocalizationManager.GetTranslation("Generic Text/okay"), "");
        }
        else
        {
            // Send file_report event to the server.
            Network.instance.SendCommand("action=file_report|userID=" + userID + "|username=" + username + "|issue=" + issues_list[issueDropdown.value] + "|text=" + reported_text);

            // Hide the ReportDialogBaseObject.
            ReportDialogBaseObject.GetComponent<GUITransition>().StartTransition(1, 0, 1, 1, true);

            // Disable the buttons
            button1.enabled = false;
            button2.enabled = false;
        }
    }

    public void OnClick_Cancel()
    {
        // Hide the ReportDialogBaseObject.
        ReportDialogBaseObject.GetComponent<GUITransition>().StartTransition(1, 0, 1, 1, true);

        // Disable the buttons
        button1.enabled = false;
        button2.enabled = false;
    }
}
