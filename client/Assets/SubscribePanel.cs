using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.EventSystems;
using I2.Loc;

public class SubscribePanel : MonoBehaviour, RequestorListener, IPointerDownHandler
{
    public static SubscribePanel instance;

    public GameObject subscribedContent, notSubscribedContent, cantSubscribeContent, justSubscribedContent;
    public GameObject tierList;
    public TMPro.TextMeshProUGUI titleText, cantSubscribeText, tierText, bonusCreditsStatus, bonusRebirthStatus, bonusXPStatus, bonusManpowerStatus, subscribedText;
    public GameObject switchBonusCreditsButton, switchBonusRebirthButton, switchBonusXPButton, switchBonusManpowerButton;
    public TMPro.TextMeshProUGUI switchBonusCreditsButtonText, switchBonusRebirthButtonText, switchBonusXPButtonText, switchBonusManpowerButtonText;

    private TierEntry tier0Entry = null, tier1Entry = null;
    private float subscribePressedTime = 0f;

    public enum RequestorTasks
    {
        UNSUBSCRIBE           = 0,
        SWITCH_BONUS_CREDITS  = 1,
        SWITCH_BONUS_REBIRTH  = 2,
        SWITCH_BONUS_XP       = 3,
        SWITCH_BONUS_MANPOWER = 4,
        CANCEL_NOT_PLATFORM_COMPATIBLE = 5,
    };

    public enum SubscriptionBonus
    {
        CREDITS  = 0,
	    REBIRTH  = 1,
	    XP       = 2,
	    MANPOWER = 3,
    }

    public SubscribePanel()
    {
        instance = this;
    }

    public void Awake()
    {
        // Remove any children of the tierList object.
        while (tierList.transform.childCount > 0)
        {
            GameObject cur_child = tierList.transform.GetChild(0).gameObject;
            cur_child.transform.SetParent(null);
            Destroy(cur_child);
        }
    }

    public void OnEnable()
    {
        Layout();
    }

    public void Init()
    {
        // Add tier entries

        if (tier0Entry == null)
        {
            tier0Entry = AddTierEntry(0, LocalizationManager.GetTranslation("Subscribe Panel/commander_tier"), String.Format("{0:0.00}", GameData.instance.subscriptionCostUSD[0]), GameData.instance.bonusCreditsPerDay[0] * 30, GameData.instance.bonusRebirthPerDay[0] * 30, GameData.instance.bonusXPPercentage[0], GameData.instance.bonusManpowerPercentage[0]);
        }

        if (tier1Entry == null)
        {
            tier1Entry = AddTierEntry(1, LocalizationManager.GetTranslation("Subscribe Panel/sovereign_tier"), String.Format("{0:0.00}", GameData.instance.subscriptionCostUSD[1]), GameData.instance.bonusCreditsPerDay[1] * 30, GameData.instance.bonusRebirthPerDay[1] * 30, GameData.instance.bonusXPPercentage[1], GameData.instance.bonusManpowerPercentage[1]);
        }
    }

    public TierEntry AddTierEntry(int _index, string _tierName, string _price, int _creditsPerMonth, int _rebirthPerMonth, int _xpBonusPercentage, int _manpwerBonusPercentage)
    {
        // Get a new tier entry
        GameObject entryObject = MemManager.instance.GetTierEntryObject();

        // Add the new entry to the list.
        entryObject.transform.SetParent(tierList.transform);
        entryObject.transform.SetAsLastSibling();
        entryObject.transform.localScale = new Vector3(1, 1, 1); // Needs to be done each time it's activated, in case it was changed last time used.
        entryObject.SetActive(true);

        // Get pointer to TierEntry component.
        TierEntry curEntry = entryObject.GetComponent<TierEntry>();
        curEntry.tierText.text = _tierName;
        curEntry.priceText.text = "$" + _price + " / " + LocalizationManager.GetTranslation("Subscribe Panel/word_month");
        curEntry.bonusCreditsText.text = LocalizationManager.GetTranslation("Subscribe Panel/bonus_credits").Replace("{credits_per_month}", _creditsPerMonth.ToString());
        curEntry.bonusRebirthText.text = LocalizationManager.GetTranslation("Subscribe Panel/bonus_rebirth").Replace("{rebirth_per_month}", _rebirthPerMonth.ToString());
        curEntry.bonusXPText.text = LocalizationManager.GetTranslation("Subscribe Panel/bonus_xp").Replace("{xp_bonus}", _xpBonusPercentage.ToString());
        curEntry.bonusManpowerText.text = LocalizationManager.GetTranslation("Subscribe Panel/bonus_manpower").Replace("{manpower_bonus}", _manpwerBonusPercentage.ToString());
        curEntry.tierIndex = _index;

        return curEntry;
    }

    public void ResolutionChanged()
    {
        Layout();
    }

    public void Layout()
    {
        if (notSubscribedContent.activeInHierarchy)
        {
            if (Screen.width > Screen.height)
            {
                // Two columns for wide screen
                GetComponent<RectTransform>().SetSizeWithCurrentAnchors(RectTransform.Axis.Horizontal, 730);
            }
            else
            {
                // One column for tall screen
                GetComponent<RectTransform>().SetSizeWithCurrentAnchors(RectTransform.Axis.Horizontal, 400);
            }
        }
        else
        {
            GetComponent<RectTransform>().SetSizeWithCurrentAnchors(RectTransform.Axis.Horizontal, 400);
        }
    }

    public void UpdateSubscriptionState()
    {
        subscribedContent.SetActive(false);
        notSubscribedContent.SetActive(false);
        cantSubscribeContent.SetActive(false);
        justSubscribedContent.SetActive(false);

        if (GameData.instance.subscribed)
        {
            Debug.Log("UpdateSubscriptionState() turning on subscribedContent");

            subscribedContent.SetActive(true);

            titleText.text = LocalizationManager.GetTranslation("Subscribe Panel/word_subscription");

            switch (GameData.instance.subscriptionTier)
            {
                case 0:
                    tierText.text = (GameData.instance.subscriptionTier == 0) ? LocalizationManager.GetTranslation("Subscribe Panel/commander_tier") : LocalizationManager.GetTranslation("Subscribe Panel/sovereign_tier");
                    break;
                case 1:
                default:
                    tierText.text = (GameData.instance.subscriptionTier == 0) ? LocalizationManager.GetTranslation("Subscribe Panel/commander_tier") : LocalizationManager.GetTranslation("Subscribe Panel/sovereign_tier");
                    break;
            }

            bonusCreditsStatus.text = LocalizationManager.GetTranslation("Subscribe Panel/receiving_credits").Replace("{credits_per_day}", GameData.instance.bonusCreditsPerDay[GameData.instance.subscriptionTier].ToString()).Replace("{nation}", GameData.instance.subscriptionBonusCreditsTarget);
            bonusRebirthStatus.text = LocalizationManager.GetTranslation("Subscribe Panel/receiving_rebirth").Replace("{rebirth_per_day}", GameData.instance.bonusRebirthPerDay[GameData.instance.subscriptionTier].ToString()).Replace("{nation}", GameData.instance.subscriptionBonusRebirthTarget);
            bonusXPStatus.text = LocalizationManager.GetTranslation("Subscribe Panel/receiving_xp").Replace("{xp_bonus}", GameData.instance.bonusXPPercentage[GameData.instance.subscriptionTier].ToString()).Replace("{nation}", GameData.instance.subscriptionBonusXPTarget);
            bonusManpowerStatus.text = LocalizationManager.GetTranslation("Subscribe Panel/receiving_manpower").Replace("{manpower_bonus}", GameData.instance.bonusManpowerPercentage[GameData.instance.subscriptionTier].ToString()).Replace("{nation}", GameData.instance.subscriptionBonusManpowerTarget);

            switchBonusCreditsButtonText.text = LocalizationManager.GetTranslation("Subscribe Panel/switch_credits").Replace("{nation}", GameData.instance.nationName);
            switchBonusRebirthButtonText.text = LocalizationManager.GetTranslation("Subscribe Panel/switch_rebirth").Replace("{nation}", GameData.instance.nationName);
            switchBonusXPButtonText.text = LocalizationManager.GetTranslation("Subscribe Panel/switch_xp").Replace("{nation}", GameData.instance.nationName);
            switchBonusManpowerButtonText.text = LocalizationManager.GetTranslation("Subscribe Panel/switch_manpower").Replace("{nation}", GameData.instance.nationName);

            switchBonusCreditsButton.SetActive(GameData.instance.subscriptionBonusCreditsTarget != GameData.instance.nationName);
            switchBonusRebirthButton.SetActive(GameData.instance.subscriptionBonusRebirthTarget != GameData.instance.nationName);
            switchBonusXPButton.SetActive(GameData.instance.subscriptionBonusXPTarget != GameData.instance.nationName);
            switchBonusManpowerButton.SetActive(GameData.instance.subscriptionBonusManpowerTarget != GameData.instance.nationName);

            subscribedText.text = LocalizationManager.GetTranslation("Subscribe Panel/subscribed_text")
                .Replace("{status}", (GameData.instance.subscriptionStatus == "Canceled") ? LocalizationManager.GetTranslation("Subscribe Panel/status_canceled") : LocalizationManager.GetTranslation("Subscribe Panel/status_active"))
                .Replace("{paid_through_date}", GameData.instance.subscriptionPaidThrough.ToString("M/d/yyyy"))
                .Replace("{cancel_link}", (GameData.instance.subscriptionStatus == "Active") ? LocalizationManager.GetTranslation("Subscribe Panel/cancel_link") : "");
        }
        else if (GameData.instance.associatedSubscribedUsername != "")
        {
            Debug.Log("UpdateSubscriptionState() turning on cantSubscribeContent");

            cantSubscribeContent.SetActive(true);

            titleText.text = LocalizationManager.GetTranslation("Subscribe Panel/word_subscription");
            cantSubscribeText.text = LocalizationManager.GetTranslation("Subscribe Panel/cant_subscribe_text").Replace("{username}", GameData.instance.associatedSubscribedUsername);
        }
        else
        {
            Debug.Log("UpdateSubscriptionState() turning on notSubscribedContent");

            notSubscribedContent.SetActive(true);

            titleText.text = LocalizationManager.GetTranslation("Subscribe Panel/word_subscribe");
        }

        Layout();
    }

    public void OnPointerDown(PointerEventData _eventData)
    {
        // Determine whether link text has been clicked.
        int link_index = TMPro.TMP_TextUtilities.FindIntersectingLink(subscribedText, Input.mousePosition, null);

        if (link_index != -1)
        {
            bool platformCompatibleWithGateway = false;

            if (GameData.instance.subscriptionGateway == "PayPal")
            {
                Requestor.Activate((int)RequestorTasks.UNSUBSCRIBE, 0, this, 
                    LocalizationManager.GetTranslation("Subscribe Panel/cancel_subscription_question")
                    .Replace("{tier}", (GameData.instance.subscriptionTier == 0) ? LocalizationManager.GetTranslation("Subscribe Panel/commander_tier") : LocalizationManager.GetTranslation("Subscribe Panel/sovereign_tier")),
                    LocalizationManager.GetTranslation("Generic Text/yes_word"), LocalizationManager.GetTranslation("Generic Text/no_word"));
                platformCompatibleWithGateway = true;
            }

            if (!platformCompatibleWithGateway)
            {
                Requestor.Activate((int)RequestorTasks.CANCEL_NOT_PLATFORM_COMPATIBLE, 0, this, 
                    LocalizationManager.GetTranslation("Subscribe Panel/cancel_not_platform_compatible")
                    .Replace("{subscription_gateway}", GameData.instance.subscriptionGateway),
                    LocalizationManager.GetTranslation("Generic Text/ok_word"), "");
            }
        }
    }

    public void OnClick_Subscribe(int _tierIndex)
    {
        Payment.instance.PurchaseSubscription(_tierIndex);
        StartCoroutine(SubscribePressed());
    }

    public void OnClick_SwitchBonusCredits()
    {
        Requestor.Activate((int)RequestorTasks.SWITCH_BONUS_CREDITS, 0, this, 
            LocalizationManager.GetTranslation("Subscribe Panel/switch_bonus_question")
            .Replace("{bonus_type}", GetBonusTypeString(SubscriptionBonus.CREDITS))
            .Replace("{old_nation}", GameData.instance.subscriptionBonusCreditsTarget)
            .Replace("{new_nation}", GameData.instance.nationName),
            LocalizationManager.GetTranslation("Generic Text/yes_word"), LocalizationManager.GetTranslation("Generic Text/no_word"));
    }

    public void OnClick_SwitchBonusRebirth()
    {
        Requestor.Activate((int)RequestorTasks.SWITCH_BONUS_REBIRTH, 0, this, 
            LocalizationManager.GetTranslation("Subscribe Panel/switch_bonus_question")
            .Replace("{bonus_type}", GetBonusTypeString(SubscriptionBonus.REBIRTH))
            .Replace("{old_nation}", GameData.instance.subscriptionBonusRebirthTarget)
            .Replace("{new_nation}", GameData.instance.nationName),
            LocalizationManager.GetTranslation("Generic Text/yes_word"), LocalizationManager.GetTranslation("Generic Text/no_word"));
    }

    public void OnClick_SwitchBonusXP()
    {
        Requestor.Activate((int)RequestorTasks.SWITCH_BONUS_XP, 0, this, 
            LocalizationManager.GetTranslation("Subscribe Panel/switch_bonus_question")
            .Replace("{bonus_type}", GetBonusTypeString(SubscriptionBonus.XP))
            .Replace("{old_nation}", GameData.instance.subscriptionBonusXPTarget)
            .Replace("{new_nation}", GameData.instance.nationName),
            LocalizationManager.GetTranslation("Generic Text/yes_word"), LocalizationManager.GetTranslation("Generic Text/no_word"));
    }

    public void OnClick_SwitchBonusManpower()
    {
        Requestor.Activate((int)RequestorTasks.SWITCH_BONUS_MANPOWER, 0, this, 
            LocalizationManager.GetTranslation("Subscribe Panel/switch_bonus_question")
            .Replace("{bonus_type}", GetBonusTypeString(SubscriptionBonus.MANPOWER))
            .Replace("{old_nation}", GameData.instance.subscriptionBonusManpowerTarget)
            .Replace("{new_nation}", GameData.instance.nationName),
            LocalizationManager.GetTranslation("Generic Text/yes_word"), LocalizationManager.GetTranslation("Generic Text/no_word"));
    }

    public IEnumerator SubscribePressed()
    {
        float curSubscribePressedTime = subscribePressedTime = Time.unscaledTime;

        // Show message asking that they wait for subscription to process (to prevent them from creating multiple subscriptions.)
        notSubscribedContent.SetActive(false);
        justSubscribedContent.SetActive(true);
        Layout();

        // Close the panel
        GameGUI.instance.CloseAllPanels();

        // Wait five minutes for possible subscription to process.
        yield return new WaitForSeconds(300);

        if (curSubscribePressedTime == subscribePressedTime)
        {
            UpdateSubscriptionState();
        }
    }

    public void RequestorResponse(int _task, int _data, Requestor.RequestorButton _result)
    {
        if (_result == Requestor.RequestorButton.LeftButton)
        {
            if (_task == (int)RequestorTasks.UNSUBSCRIBE)
            {
                Debug.Log("Unsubscribe");
                Network.instance.SendCommand("action=unsubscribe");
            }
            else if (_task == (int)RequestorTasks.SWITCH_BONUS_CREDITS)
            {
                Debug.Log("Switch bonus credits");
                Network.instance.SendCommand("action=switch_subscription_bonus|bonus=" + ((int)SubscriptionBonus.CREDITS).ToString());
            }
            else if (_task == (int)RequestorTasks.SWITCH_BONUS_REBIRTH)
            {
                Debug.Log("Switch bonus rebirth");
                Network.instance.SendCommand("action=switch_subscription_bonus|bonus=" + ((int)SubscriptionBonus.REBIRTH).ToString());
            }
            else if (_task == (int)RequestorTasks.SWITCH_BONUS_XP)
            {
                Debug.Log("Switch bonus XP");
                Network.instance.SendCommand("action=switch_subscription_bonus|bonus=" + ((int)SubscriptionBonus.XP).ToString());
            }
            else if (_task == (int)RequestorTasks.SWITCH_BONUS_MANPOWER)
            {
                Debug.Log("Switch bonus manpower");
                Network.instance.SendCommand("action=switch_subscription_bonus|bonus=" + ((int)SubscriptionBonus.MANPOWER).ToString());
            }
        }
    }

    public string GetBonusTypeString(SubscriptionBonus _bonusType)
    {
        switch (_bonusType)
        {
            case SubscriptionBonus.CREDITS:
                return LocalizationManager.GetTranslation("Subscribe Panel/word_credits");
            case SubscriptionBonus.REBIRTH:
                return LocalizationManager.GetTranslation("Subscribe Panel/word_rebirth");
            case SubscriptionBonus.XP:
                return LocalizationManager.GetTranslation("Subscribe Panel/word_xp");
            case SubscriptionBonus.MANPOWER:
            default:
                return LocalizationManager.GetTranslation("Subscribe Panel/word_manpower_generation");
        }
    }
}
