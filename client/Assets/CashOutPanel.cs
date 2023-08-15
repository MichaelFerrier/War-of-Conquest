using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using I2.Loc;

public class CashOutPanel : MonoBehaviour, RequestorListener
{
    public static CashOutPanel instance;

    public Dropdown memberDropdown;
    public InputField amountInputField;
    public Text messageText;

    public List<int> memberIDs = new List<int>();
    public List<string> memberUsernames = new List<string>();

    private enum RequestorTask
    {
        SendPrize
    };

    public CashOutPanel()
    {
        instance = this;
    }

    void OnEnable()
    {
        ConstrainAmount(true);
    }

    public void UpdateForUpdateEvent()
    {
        ConstrainAmount(false);

        // If the amount of prizeMoney has declined below minWinningsToCashOut, close the cashOutPanel.
        if (gameObject.activeInHierarchy && (GameData.instance.prizeMoney < GameData.instance.minWinningsToCashOut)) {
            GameGUI.instance.CloseAllPanels();
        }
    }

    public void MemberListReceived(List<MemberEntryData> _memberList)
    {
        // Sort the given member list by rank.
        _memberList.Sort(new MemberEntryDataRankComparer());

        // Clear the lists of member IDs and usernames.
        memberIDs.Clear();
        memberUsernames.Clear();

        foreach (MemberEntryData curData in _memberList)
        {
            // Record this member's ID and username.
            memberIDs.Add(curData.userID);
            memberUsernames.Add(curData.username);
        }

        // Remake the dropdown menu of member usernames.
        memberDropdown.ClearOptions();
        memberDropdown.AddOptions(memberUsernames);
    }

    public void OnEndEdit_Amount()
    {
        ConstrainAmount(false);
    }

    public void ConstrainAmount(bool _reset)
    {
        if (GameData.instance == null) {
            return;
        }

        string message = "";
        float maxAmount = (float)(GameData.instance.prizeMoney / 100.0);
        float minAmount = (float)(GameData.instance.minWinningsToCashOut / 100.0);

        if ((amountInputField.text == "") || _reset)
        {
            // Do not allow blank amount.
            amountInputField.text = maxAmount.ToString("N2");
        }
        else
        {
            float amount = float.Parse(amountInputField.text); // Culture specific

            if (amount < minAmount) {
                amountInputField.text = minAmount.ToString("N2");
                // "The minimum that can be cashed out is {[MIN_WINNINGS_TO_CASH_OUT]}.";
                message = LocalizationManager.GetTranslation("Nation Panel/min_winnings_to_cash_out_message").Replace("{[MIN_WINNINGS_TO_CASH_OUT]}", "$" + minAmount.ToString("N2"));
            } else if (amount > maxAmount) {
                amountInputField.text = maxAmount.ToString("N2");
            } else {
                amountInputField.text = amount.ToString("N2");
            }
        }

        messageText.text = message;
    }

    public void OnClick_Send()
    {
        // "Send ${[WINNINGS_AMOUNT]} of winnings to {[USERNAME]}?"
        Requestor.Activate((int)RequestorTask.SendPrize, 0, this, 
            LocalizationManager.GetTranslation("Nation Panel/confirm_send")
                .Replace("{[USERNAME]}", memberUsernames[memberDropdown.value])
                .Replace("{[WINNINGS_AMOUNT]}", amountInputField.text),
            LocalizationManager.GetTranslation("Generic Text/yes_word"), LocalizationManager.GetTranslation("Generic Text/no_word"));
    }

    public void RequestorResponse(int _task, int _data, Requestor.RequestorButton _result)
    {
        if (((RequestorTask)_task == RequestorTask.SendPrize) && (_result == Requestor.RequestorButton.LeftButton))
        {
            // Send cash out message to server.
            Network.instance.SendCommand("action=cash_out|target_user_id=" + memberIDs[memberDropdown.value] + "|amount=" + (int)(Math.Round(float.Parse(amountInputField.text) * 100)));
        }
    }
}
