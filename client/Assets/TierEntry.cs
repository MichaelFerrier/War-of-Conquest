using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class TierEntry : MonoBehaviour
{
    public TMPro.TextMeshProUGUI tierText, priceText, bonusCreditsText, bonusRebirthText, bonusXPText, bonusManpowerText;
    public int tierIndex = 0;

    public void OnClick_Subscribe()
    {
        SubscribePanel.instance.OnClick_Subscribe(tierIndex);
    }
}
