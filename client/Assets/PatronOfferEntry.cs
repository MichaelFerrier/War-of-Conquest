using UnityEngine;
using UnityEngine.UI;
using System;
using System.Collections;

public class PatronOfferEntry : MonoBehaviour
{
    public TMPro.TextMeshProUGUI usernameText, bonusXPText, bonusCreditsText, numFollowersText;
    public int userID;
    public String username;

    public void Init(int _userID, String _username, int _bonusXP, int _bonusCredits, int _numFollowers)
    {
        userID = _userID;

        usernameText.text = username = _username;
        bonusXPText.text = string.Format("{0:n0}", _bonusXP);
        bonusCreditsText.text = string.Format("{0:n0}", _bonusCredits);
        numFollowersText.text = string.Format("{0:n0}", _numFollowers);
    }

    public void OnClick_AcceptPatronOffer()
    {
        ConnectPanel.instance.OnClick_AcceptPatronOffer(userID, username);
    }

    public void OnClick_DeclinePatronOffer()
    {
        ConnectPanel.instance.OnClick_DeclinePatronOffer(userID, username);
    }
}