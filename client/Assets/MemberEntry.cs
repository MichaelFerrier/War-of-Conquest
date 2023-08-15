using UnityEngine;
using UnityEngine.UI;
using System;
using System.Collections;
using System.Collections.Generic;
using I2.Loc;

public class MemberEntry : MonoBehaviour
{
    public static Color loggedInColor = new Color(1, 1, 1);
    public static Color loggedOutColor = new Color(0.7f, 0.7f, 0.7f);

    List<int> rank_list = new List<int>();
    List<string> rank_string_list = new List<string>();

    public TMPro.TextMeshProUGUI buttonText;
    public Text usernameText, xpText, rankText;
    public GameObject buttonObject, rankTextObject, rankDropdownObject, infoButtonObject;
    public Dropdown rankDropdown;
    public int userID;
    public string username;

    private bool initializing = false;
     
    public void Init(MemberEntryData _data)
    {
        // Record that this entry is being initialized
        initializing = true;

        //buttonObject = gameObject.transform.GetChild(3).GetChild(0).gameObject;
        //rankTextObject = gameObject.transform.GetChild(2).GetChild(1).gameObject;
        //rankDropdownObject = gameObject.transform.GetChild(2).GetChild(0).gameObject;

        //usernameText = gameObject.transform.GetChild(0).GetChild(0).GetComponent<Text>();
        //xpText = gameObject.transform.GetChild(0).GetChild(1).GetComponent<Text>();
        //rankText = rankTextObject.GetComponent<Text>();

        //buttonText = buttonObject.transform.GetChild(0).GetComponent<TMPro.TextMeshProUGUI>();
        //rankDropdown = rankDropdownObject.GetComponent<Dropdown>();

        // Record the userID and username of this entry's member.
        userID = _data.userID;
        username = _data.username;

        // Set the username text, and set its color to indicate if the member is logged in.
        usernameText.text = _data.username;
        usernameText.color = _data.logged_in ? loggedInColor : loggedOutColor;

        // Set the XP text.
        xpText.text = String.Format("{0:n0}", _data.points) + " " + LocalizationManager.GetTranslation("Generic Text/xp_word");

        // Set up the Rank display/dropdown.
        if ((_data.rank > GameData.instance.userRank) && (GameData.instance.userRank <= GameData.RANK_CAPTAIN))
        {
            //infoButtonObject.SetActive(true);
            rankDropdownObject.SetActive(true);
            rankTextObject.SetActive(false);

            rank_list.Clear();
            rank_string_list.Clear();

            if (GameData.RANK_CIVILIAN > GameData.instance.userRank) { rank_list.Add(GameData.RANK_CIVILIAN); rank_string_list.Add(GameData.instance.GetRankString(GameData.RANK_CIVILIAN)); }
            if (GameData.RANK_WARRIOR > GameData.instance.userRank) { rank_list.Add(GameData.RANK_WARRIOR); rank_string_list.Add(GameData.instance.GetRankString(GameData.RANK_WARRIOR)); }
            if (GameData.RANK_COMMANDER > GameData.instance.userRank) { rank_list.Add(GameData.RANK_COMMANDER); rank_string_list.Add(GameData.instance.GetRankString(GameData.RANK_COMMANDER)); }
            if (GameData.RANK_CAPTAIN > GameData.instance.userRank) { rank_list.Add(GameData.RANK_CAPTAIN); rank_string_list.Add(GameData.instance.GetRankString(GameData.RANK_CAPTAIN)); }
            if (GameData.RANK_GENERAL > GameData.instance.userRank) { rank_list.Add(GameData.RANK_GENERAL); rank_string_list.Add(GameData.instance.GetRankString(GameData.RANK_GENERAL)); }
            if (GameData.RANK_COSOVEREIGN > GameData.instance.userRank) { rank_list.Add(GameData.RANK_COSOVEREIGN); rank_string_list.Add(GameData.instance.GetRankString(GameData.RANK_COSOVEREIGN)); }

            rankDropdown.ClearOptions();
            rankDropdown.AddOptions(rank_string_list);
            rankDropdown.value = rank_list.IndexOf(_data.rank);
        }
        else
        {
            //infoButtonObject.SetActive(false);
            rankDropdownObject.SetActive(false);
            rankTextObject.SetActive(true);
            rankText.text = GameData.instance.GetRankString(_data.rank);
        }

        // GB-Localization
        // Set up the Leave/Remove button.
        if (((_data.rank > GameData.instance.userRank) && (GameData.instance.userRank <= GameData.RANK_GENERAL)) || (_data.userID == GameData.instance.userID))
        {
            if (_data.userID == GameData.instance.userID) {
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/leave_word"); // "Leave"
            } else {
                buttonText.text = LocalizationManager.GetTranslation("Generic Text/remove_word"); // "Remove"
            }

            buttonObject.SetActive(true);
        }
        else
        {
            buttonObject.SetActive(false);
        }

        // Set up the button's listener.
        Button removeButton = buttonObject.GetComponent<Button>();
        removeButton.onClick.RemoveAllListeners();
        removeButton.onClick.AddListener(() => ButtonPressed());

        // Set up the dropdown's listener.
        rankDropdown.onValueChanged.RemoveAllListeners();
        rankDropdown.onValueChanged.AddListener(delegate { RankValueChanged(rankDropdown); });

        // Record that this entry is no longer being initialized
        initializing = false;
    }

    public void ButtonPressed()
    {
        NationPanel.instance.OnClick_RemoveMember(userID, username);
    }

    public void RankValueChanged(Dropdown _target)
    {
        if (!initializing) {
            NationPanel.instance.OnChange_MemberRank(userID, rank_list[rankDropdown.value]);
        }
    }
}
