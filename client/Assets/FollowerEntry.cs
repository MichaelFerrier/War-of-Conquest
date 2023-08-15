using System;
using UnityEngine;

public class FollowerEntry : MonoBehaviour
{
    public TMPro.TextMeshProUGUI usernameText, bonusXPText, bonusCreditsText;
    public int userID;
    public String username;

    public void Init(int _userID, String _username, int _bonusXP, int _bonusCredits)
    {
        userID = _userID;

        usernameText.text = username = _username;
        bonusXPText.text = string.Format("{0:n0}", _bonusXP);
        bonusCreditsText.text = string.Format("{0:n0}", _bonusCredits);
    }

    public void OnClick_RemoveFollower()
    {
        // Let the ConnectPanel know about this event.
        ConnectPanel.instance.OnClick_RemoveFollower(userID, username);
    }
}
