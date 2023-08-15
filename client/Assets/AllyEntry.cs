using UnityEngine;
using UnityEngine.UI;
using I2.Loc;
using System.Collections;

public class AllyEntry : MonoBehaviour
{
    public GameObject acceptButtonArea, declineButtonArea, withdrawButtonArea, breakButtonArea, inviteUniteButtonArea, withdrawUniteButtonArea, acceptUniteButtonArea, declineUniteButtonArea;
    public TMPro.TextMeshProUGUI nameText;
    public Button acceptButton, declineButton, withdrawButton, breakButton, inviteUniteButton, withdrawUniteButton, acceptUniteButton, declineUniteButton;

    public int nationID;
    public string nationName;

    public void Init(AllyData _data, GameData.AllianceListType _list_type)
    {
        // Record the nationID and nationName of this entry.
        nationID = _data.ID;
        nationName = _data.name;

        // Set the nation name text (show offer amount, if non-zero).
        nameText.text = nationName + ((_data.paymentOffer == 0) ? "" : ("\n(" + LocalizationManager.GetTranslation("Generic Text/offer_word") + ": " + string.Format("{0:n0}", _data.paymentOffer) + "<sprite=2>)"));

        bool uniteRequestReceived = GameData.instance.NationIsInAllianceList(GameData.instance.incomingUniteRequestsList, nationID);
        bool uniteRequestSent = GameData.instance.NationIsInAllianceList(GameData.instance.outgoingUniteRequestsList, nationID);

        // Display only the appropriate buttons.
        acceptButtonArea.SetActive(_list_type == GameData.AllianceListType.INCOMING_ALLIANCE_INVITATIONS);
        declineButtonArea.SetActive(_list_type == GameData.AllianceListType.INCOMING_ALLIANCE_INVITATIONS);
        withdrawButtonArea.SetActive(_list_type == GameData.AllianceListType.OUTGOING_ALLIANCE_INVITATIONS);
        breakButtonArea.SetActive(_list_type == GameData.AllianceListType.CURRENT_ALLIES);
        inviteUniteButtonArea.SetActive((_list_type == GameData.AllianceListType.CURRENT_ALLIES) && (!uniteRequestReceived) && (!uniteRequestSent) && (GameData.instance.userRank <= GameData.RANK_COSOVEREIGN));
        acceptUniteButtonArea.SetActive((_list_type == GameData.AllianceListType.INCOMING_UNITE_INVITATIONS) && (GameData.instance.userRank <= GameData.RANK_COSOVEREIGN));
        declineUniteButtonArea.SetActive((_list_type == GameData.AllianceListType.INCOMING_UNITE_INVITATIONS) && (GameData.instance.userRank <= GameData.RANK_COSOVEREIGN));
        withdrawUniteButtonArea.SetActive((_list_type == GameData.AllianceListType.OUTGOING_UNITE_INVITATIONS) && (GameData.instance.userRank <= GameData.RANK_COSOVEREIGN));

        //Debug.Log("Ally " + nationName + ", uniteRequestReceived: " + uniteRequestReceived + ", uniteRequestSent: " + uniteRequestSent + ", rank: " + GameData.instance.userRank + ", inviteUniteButtonArea active: " + inviteUniteButtonArea.activeInHierarchy);

        // Set up the buttons' listeners.
        acceptButton.onClick.RemoveAllListeners();
        acceptButton.onClick.AddListener(() => AcceptButtonPressed());
        declineButton.onClick.RemoveAllListeners();
        declineButton.onClick.AddListener(() => DeclineButtonPressed());
        withdrawButton.onClick.RemoveAllListeners();
        withdrawButton.onClick.AddListener(() => WithdrawButtonPressed());
        breakButton.onClick.RemoveAllListeners();
        breakButton.onClick.AddListener(() => BreakButtonPressed());
        inviteUniteButton.onClick.RemoveAllListeners();
        inviteUniteButton.onClick.AddListener(() => InviteUniteButtonPressed());
        withdrawUniteButton.onClick.RemoveAllListeners();
        withdrawUniteButton.onClick.AddListener(() => WithdrawUniteButtonPressed());
        acceptUniteButton.onClick.RemoveAllListeners();
        acceptUniteButton.onClick.AddListener(() => AcceptUniteButtonPressed());
        declineUniteButton.onClick.RemoveAllListeners();
        declineUniteButton.onClick.AddListener(() => DeclineUniteButtonPressed());
    }

    public void AcceptButtonPressed()
    {
        NationPanel.instance.OnClick_AcceptAlliance(nationID, nationName);
    }

    public void DeclineButtonPressed()
    {
        NationPanel.instance.OnClick_DeclineAlliance(nationID, nationName);
    }

    public void WithdrawButtonPressed()
    {
        NationPanel.instance.OnClick_WithdrawAlliance(nationID, nationName);
    }

    public void BreakButtonPressed()
    {
        NationPanel.instance.OnClick_BreakAlliance(nationID, nationName);
    }

    public void InviteUniteButtonPressed()
    {
        NationPanel.instance.OnClick_InviteUnite(nationID, nationName);
    }

    public void WithdrawUniteButtonPressed()
    {
        NationPanel.instance.OnClick_WithdrawUnite(nationID, nationName);
    }

    public void AcceptUniteButtonPressed()
    {
        NationPanel.instance.OnClick_AcceptUnite(nationID, nationName);
    }

    public void DeclineUniteButtonPressed()
    {
        NationPanel.instance.OnClick_DeclineUnite(nationID, nationName);
    }
}
