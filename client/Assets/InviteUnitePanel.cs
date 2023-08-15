using UnityEngine;
using UnityEngine.UI;
using System;
using System.Collections;
using I2.Loc;

public class InviteUnitePanel : MonoBehaviour
{
    public static InviteUnitePanel instance;

    public InputField payment_field;
    public TMPro.TextMeshProUGUI text;

    private int nationID;
    private string nationName;

    public InviteUnitePanel()
    {
        instance = this;
    }

    public void Init(int _nationID, string _nationName)
    {
        nationID = _nationID;
        nationName = _nationName;

        // Set text
        text.text = LocalizationManager.GetTranslation("Nation Panel/confirm_invite_unite").Replace("{[NATION_NAME]}", nationName).Replace("{[COST]}", GameData.instance.uniteCost.ToString());

        // Reset payment entry
        payment_field.text = "0";
    }

    public void OnClick_Yes()
    {
        int offer_amount = 0;

        // Verify format of payment offer amount and fetch value.
        if ((!Int32.TryParse(payment_field.text, out offer_amount)) || (offer_amount < 0))
        {
            offer_amount = 0;
            payment_field.text = "0";
        }

        if (offer_amount > GameData.instance.credits_transferable)
        {
            Requestor.Activate(0, 0, null, LocalizationManager.GetTranslation("invite_unite_payment_too_high").Replace("{transferable_credits}", string.Format("{0:n0}", GameData.instance.credits_transferable)).Replace("{payment_offer}", string.Format("{0:n0}", offer_amount)), LocalizationManager.GetTranslation("Generic Text/okay"), "");
            return;
        }

        // Send event_request_unite event to the server.
        Network.instance.SendCommand("action=event_request_unite|targetNationID=" + nationID + "|payment_offer=" + offer_amount);

        // Close this panel.
        GameGUI.instance.CloseAllPanels();
    }

    public void OnClick_No()
    {
        // Close this panel.
        GameGUI.instance.CloseAllPanels();
    }
}
