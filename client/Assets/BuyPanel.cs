using UnityEngine;
using UnityEngine.UI;
using System;
using System.Collections;
using I2.Loc;

public class BuyPanel : MonoBehaviour
{
    public static BuyPanel instance;

    public BuyType buy_type;
    public float purchased_ratio, /*limit_factor,*/ absolute_limit;
    public float[] package_price = new float[4];
    public TMPro.TextMeshProUGUI titleText;
    public GameObject button0, button1, button2, button3;
    public GameObject paymentMethodPanelObject;
    public Dropdown paymentMethodDropdown;
    public GameObject textPanelObject;
    public TMPro.TextMeshProUGUI textPanelText;
    private bool initializing = false;

    public enum BuyType
    {
        Manpower,
        Energy,
        Credits
    }

	public BuyPanel()
    {
        instance = this;
	}

    void Start()
    {
        // Payment methods
        paymentMethodDropdown.ClearOptions();
        paymentMethodDropdown.AddOptions(Payment.instance.payment_method_name_list);
        paymentMethodDropdown.value = Payment.instance.payment_method_list.IndexOf((int)Payment.instance.GetPaymentMethod());
    }

    void OnEnable()
    {
        initializing = true;

        // Only display the payment method dropdown if buying credits, and if there is more than one option to choose from.
        bool display_payment_method = (Payment.instance.payment_method_list.Count > 1) && (buy_type == BuyType.Credits);
        paymentMethodPanelObject.SetActive(display_payment_method);

        StartCoroutine(UpdateTextPanel());

        initializing = false;
    }

    public bool Init(BuyPanel.BuyType _buy_type)
    {
        buy_type = _buy_type;
        return Setup();
    }

    public bool Setup()
    {
        int amount;
        float price;

        string fill_to_100 = LocalizationManager.GetTranslation("Buy Panel/fill_to_100_percent_capacity"); // "Fill to 100% capacity"
        string buy_50 = LocalizationManager.GetTranslation("Buy Panel/buy_50_percent_capacity"); // "Buy 50% of capacity"
        string buy_10 = LocalizationManager.GetTranslation("Buy Panel/buy_10_percent_capacity"); // "Buy 10% of capacity"
        string buy_n = LocalizationManager.GetTranslation("Buy Panel/buy_n_percent_capacity"); // "Buy {[n]}% of capacity"

        button0.SetActive(false);
        button1.SetActive(false);
        button2.SetActive(false);
        button3.SetActive(false);

        //// TESTING
        //GameData.instance.buyEnergyDayAmount = 10000;
        //GameData.instance.userRank = GameData.RANK_WARRIOR;

        // Update end of day time and related information.
        GameData.instance.UpdateEndOfDayTime();        

        if (buy_type == BuyType.Manpower)
        {
            // GB-Localization
            titleText.text = LocalizationManager.GetTranslation("Buy Panel/buy_manpower_title"); // "Buy Manpower"

            if (GameData.instance.current_footprint.manpower < GameData.instance.GetFinalManpowerMax())
            {
                if (GameData.instance.userRank <= GameData.RANK_CAPTAIN)
                {
                    // Determine the factor to be applied to manpower prices, based on how much manpower this nation has purchased during the current day.
                    purchased_ratio = (float)GameData.instance.current_footprint.buy_manpower_day_amount / (float)GameData.instance.GetFinalManpowerMax();
                    //float power = Mathf.Floor(purchased_ratio / GameData.instance.buyManpowerDailyLimit);
                    //limit_factor = Mathf.Pow(GameData.instance.buyManpowerLimitBase, power);
                    absolute_limit = GameData.instance.buyManpowerDailyAbsoluteLimit;
                    float fraction_allowed_to_buy = Mathf.Max(0f, absolute_limit - purchased_ratio);
                    float fraction_empty = ((float)(GameData.instance.GetFinalManpowerMax() - GameData.instance.current_footprint.manpower) / (float)GameData.instance.GetFinalManpowerMax());

                    if (purchased_ratio < absolute_limit)
                    {
                        if (fraction_empty <= fraction_allowed_to_buy)
                        {
                            // Fill to capacity
                            amount = (GameData.instance.GetFinalManpowerMax() - GameData.instance.current_footprint.manpower);
                            package_price[0] = price = AmountToPrice(amount, GameData.instance.buyManpowerBase, GameData.instance.buyManpowerMult)/* * limit_factor*/;
                            button0.transform.GetChild(0).GetComponent<TMPro.TextMeshProUGUI>().text = fill_to_100 + "\n<color=yellow>" + amount + "</color> <sprite=0>";
                            button0.transform.GetChild(1).GetComponent<TMPro.TextMeshProUGUI>().text = price + "<sprite=2>";
                            button0.SetActive(true);
                        }

                        // Buy 50% of capacity
                        if ((fraction_empty >= 0.5f) && (fraction_allowed_to_buy >= 0.5f))
                        {
                            amount = (int)(GameData.instance.GetFinalManpowerMax() * 0.5f);
                            package_price[1] = price = AmountToPrice(amount, GameData.instance.buyManpowerBase, GameData.instance.buyManpowerMult)/* * limit_factor*/;
                            button1.transform.GetChild(0).GetComponent<TMPro.TextMeshProUGUI>().text = buy_50 + "\n<color=yellow>" + amount + "</color> <sprite=0>";
                            button1.transform.GetChild(1).GetComponent<TMPro.TextMeshProUGUI>().text = price + "<sprite=2>";
                            button1.SetActive(true);
                        }

                        // Buy 10% of capacity
                        if ((fraction_empty >= 0.1f) && (fraction_allowed_to_buy >= 0.1f))
                        {
                            amount = (int)(GameData.instance.GetFinalManpowerMax() * 0.1f);
                            package_price[2] = price = AmountToPrice(amount, GameData.instance.buyManpowerBase, GameData.instance.buyManpowerMult)/* * limit_factor*/;
                            button2.transform.GetChild(0).GetComponent<TMPro.TextMeshProUGUI>().text = buy_10 + "\n<color=yellow>" + amount + "</color> <sprite=0>";
                            button2.transform.GetChild(1).GetComponent<TMPro.TextMeshProUGUI>().text = price + "<sprite=2>";
                            button2.SetActive(true);
                        }

                        // Buy less than 10% of capacity
                        if ((fraction_empty >= 0.01f) && (fraction_empty < 0.1f) && (fraction_allowed_to_buy >= 0.01f))
                        {
                            amount = (int)(GameData.instance.GetFinalManpowerMax() * Mathf.Min(fraction_empty, fraction_allowed_to_buy));
                            package_price[2] = price = AmountToPrice(amount, GameData.instance.buyManpowerBase, GameData.instance.buyManpowerMult)/* * limit_factor*/;
                            button2.transform.GetChild(0).GetComponent<TMPro.TextMeshProUGUI>().text = buy_n.Replace("{[n]}", String.Format("{0:n0}", (int)(Mathf.Min(fraction_empty, fraction_allowed_to_buy) * 100 + 0.5f))) + "\n<color=yellow>" + amount + "</color> <sprite=0>";
                            button2.transform.GetChild(1).GetComponent<TMPro.TextMeshProUGUI>().text = price + "<sprite=2>";
                            button2.SetActive(true);
                        }
                    }
                }
            }
            else 
            {
                return false;
            }
        }
        else if (buy_type == BuyType.Energy)
        {
            // GB-Localization

            string lkey_buy_energy_title = "Buy Panel/buy_energy_title"; // "Buy Energy Reserves"

            titleText.text = LocalizationManager.GetTranslation(lkey_buy_energy_title);

            if (GameData.instance.energy < GameData.instance.GetFinalEnergyMax())
            {
                if (GameData.instance.userRank <= GameData.RANK_CAPTAIN)
                {
                    // Determine the factor to be applied to energy prices, based on how much energy this nation has purchased during the current day.
                    purchased_ratio = (float)GameData.instance.buyEnergyDayAmount / (float)GameData.instance.GetFinalEnergyMax();
                    //float power = Mathf.Floor(purchased_ratio / GameData.instance.buyEnergyDailyLimit);
                    //limit_factor = Mathf.Pow(GameData.instance.buyEnergyLimitBase, power);
                    absolute_limit = GameData.instance.buyEnergyDailyAbsoluteLimit;
                    float fraction_allowed_to_buy = Mathf.Max(0f, absolute_limit - purchased_ratio);
                    float fraction_empty = ((float)(GameData.instance.GetFinalEnergyMax() - GameData.instance.energy) / (float)GameData.instance.GetFinalEnergyMax());

                    Debug.Log("GameData.instance.buyEnergyDayAmount: " + GameData.instance.buyEnergyDayAmount + ", purchased_ratio: " + purchased_ratio/* + ", power: " + power + ", limit_factor: " + limit_factor*/);

                    if (purchased_ratio < absolute_limit)
                    {
                        if (fraction_empty <= fraction_allowed_to_buy)
                        {
                            // Fill to capacity
                            amount = (GameData.instance.GetFinalEnergyMax() - GameData.instance.energy);
                            package_price[0] = price = AmountToPrice(amount, GameData.instance.buyEnergyBase, GameData.instance.buyEnergyMult)/* * limit_factor*/;
                            button0.transform.GetChild(0).GetComponent<TMPro.TextMeshProUGUI>().text = fill_to_100 + "\n<color=yellow>" + amount + "</color> <sprite=1>";
                            button0.transform.GetChild(1).GetComponent<TMPro.TextMeshProUGUI>().text = price + "<sprite=2>";
                            button0.SetActive(true);
                        }

                        // Buy 50% of capacity
                        if ((fraction_empty >= 0.5f) && (fraction_allowed_to_buy >= 0.5f))
                        {
                            amount = (int)(GameData.instance.GetFinalEnergyMax() * 0.5f);
                            package_price[1] = price = AmountToPrice(amount, GameData.instance.buyEnergyBase, GameData.instance.buyEnergyMult)/* * limit_factor*/;
                            button1.transform.GetChild(0).GetComponent<TMPro.TextMeshProUGUI>().text = buy_50 + "\n<color=yellow>" + amount + "</color> <sprite=1>";
                            button1.transform.GetChild(1).GetComponent<TMPro.TextMeshProUGUI>().text = price + "<sprite=2>";
                            button1.SetActive(true);
                        }

                        // Buy 10% of capacity
                        if ((fraction_empty >= 0.1f) && (fraction_allowed_to_buy >= 0.1f))
                        {
                            amount = (int)(GameData.instance.GetFinalEnergyMax() * 0.1f);
                            package_price[2] = price = AmountToPrice(amount, GameData.instance.buyEnergyBase, GameData.instance.buyEnergyMult)/* * limit_factor*/;
                            button2.transform.GetChild(0).GetComponent<TMPro.TextMeshProUGUI>().text = buy_10 + "\n<color=yellow>" + amount + "</color> <sprite=1>";
                            button2.transform.GetChild(1).GetComponent<TMPro.TextMeshProUGUI>().text = price + "<sprite=2>";
                            button2.SetActive(true);
                        }

                        // Buy less than 10% of capacity
                        if ((fraction_empty >= 0.01f) && (fraction_empty < 0.1f) && (fraction_allowed_to_buy >= 0.01f))
                        {
                            amount = (int)(GameData.instance.GetFinalEnergyMax() * Mathf.Min(fraction_empty, fraction_allowed_to_buy));
                            package_price[2] = price = AmountToPrice(amount, GameData.instance.buyEnergyBase, GameData.instance.buyEnergyMult)/* * limit_factor*/;
                            button2.transform.GetChild(0).GetComponent<TMPro.TextMeshProUGUI>().text = buy_n.Replace("{[n]}", String.Format("{0:n0}", (int)(Mathf.Min(fraction_empty, fraction_allowed_to_buy) * 100 + 0.5f))) + "\n<color=yellow>" + amount + "</color> <sprite=0>";
                            button2.transform.GetChild(1).GetComponent<TMPro.TextMeshProUGUI>().text = price + "<sprite=2>";
                            button2.SetActive(true);
                        }
                    }
                }
            }
            else 
            {
                return false;
            }
        }
        else if (buy_type == BuyType.Credits)
        {
            // GB-Localization
            titleText.text = LocalizationManager.GetTranslation("Buy Panel/buy_credits_title"); // "Buy Credits"

            string credit_word = LocalizationManager.GetTranslation("Generic Text/credit_word"); // "Credit"

            // TODO: GB-Localization, determine later how to represent currency. It might be based on 
            // geographic location or based on user selection
            
            // Boost
            amount = GameData.instance.buyCreditsAmount[0];
            price = GameData.instance.buyCreditsCostUSD[0];
            if ((GameData.instance.credits_allowed_to_buy == -1) || (GameData.instance.credits_allowed_to_buy >= amount))
            {
                button0.transform.GetChild(0).GetComponent<TMPro.TextMeshProUGUI>().text = credit_word + " " + GameGUI.instance.buyCreditsPackageName[0] + "\n<color=yellow>" + amount + "</color> <sprite=2>";
                button0.transform.GetChild(1).GetComponent<TMPro.TextMeshProUGUI>().text = "$" + price;
                button0.SetActive(true);
            }

            // Resupply
            amount = GameData.instance.buyCreditsAmount[1];
            price = GameData.instance.buyCreditsCostUSD[1];
            if ((GameData.instance.credits_allowed_to_buy == -1) || (GameData.instance.credits_allowed_to_buy >= amount))
            {
                button1.transform.GetChild(0).GetComponent<TMPro.TextMeshProUGUI>().text = credit_word + " " + GameGUI.instance.buyCreditsPackageName[1] + "\n<color=yellow>" + amount + "</color> <sprite=2>";
                button1.transform.GetChild(1).GetComponent<TMPro.TextMeshProUGUI>().text = "$" + price;
                button1.SetActive(true);
            }

            // Infusion
            amount = GameData.instance.buyCreditsAmount[2];
            price = GameData.instance.buyCreditsCostUSD[2];
            if ((GameData.instance.credits_allowed_to_buy == -1) || (GameData.instance.credits_allowed_to_buy >= amount))
            {
                button2.transform.GetChild(0).GetComponent<TMPro.TextMeshProUGUI>().text = credit_word + " " + GameGUI.instance.buyCreditsPackageName[2] + "\n<color=yellow>" + amount + "</color> <sprite=2>";
                button2.transform.GetChild(1).GetComponent<TMPro.TextMeshProUGUI>().text = "$" + price;
                button2.SetActive(true);
            }

            // Mother Lode
            amount = GameData.instance.buyCreditsAmount[3];
            price = GameData.instance.buyCreditsCostUSD[3];
            if ((GameData.instance.credits_allowed_to_buy == -1) || (GameData.instance.credits_allowed_to_buy >= amount))
            {
                button3.transform.GetChild(0).GetComponent<TMPro.TextMeshProUGUI>().text = credit_word + " " + GameGUI.instance.buyCreditsPackageName[3] + "\n<color=yellow>" + amount + "</color> <sprite=2>";
                button3.transform.GetChild(1).GetComponent<TMPro.TextMeshProUGUI>().text = "$" + price;
                button3.SetActive(true);
            }
        }

        return true;
    }

    public void OnClick_BuyButton(int _button_index)
    {
        if ((buy_type == BuyType.Manpower) || (buy_type == BuyType.Energy))
        {
            if (package_price[_button_index] > GameData.instance.credits)
            {
                // Bring up UI for purchasing credits.
                GameGUI.instance.RequestBuyCredits();
                return;
            }

            // Close the buy panel
            GameGUI.instance.CloseAllPanels();

            // Send the rank change message to the server.
            Network.instance.SendCommand("action=buy_" + ((buy_type == BuyType.Manpower) ? "manpower" : "energy") + "|package=" + _button_index);
        }
        else
        {
            // Close the buy panel
            GameGUI.instance.CloseAllPanels();

            // Begin the credit purchase process.
            Payment.instance.PurchaseCredits(_button_index);
        }
    }

    public void OnChange_PaymentMethod()
    {
        if (!initializing) {
            Payment.instance.SetPaymentMethod((Payment.PaymentMethod)Payment.instance.payment_method_list[paymentMethodDropdown.value]);
        }
    }

    public float AmountToPrice(int _amount, float _base, float _mult)
    {
        return Mathf.Ceil(Mathf.Pow(_base, Mathf.Log10(_amount)) * _mult);
    }

    IEnumerator UpdateTextPanel()
    {
        if (buy_type == BuyType.Credits)
        {
            if (GameData.instance.creditsAllowedBuyPerMonth == -1)
            {
                // No monthly limit on credit purchases.
                textPanelObject.SetActive(false);
            }
            else
            {
                // Display monthly limit on credit purchases.
                textPanelObject.SetActive(true);
                textPanelText.text = LocalizationManager.GetTranslation("Buy Panel/buy_credits_limit")
                    .Replace("{[credits_allowed_buy_per_month]}", String.Format("{0:n0}", GameData.instance.creditsAllowedBuyPerMonth))
                    .Replace("{[credits_allowed_buy]}", String.Format("{0:n0}", GameData.instance.credits_allowed_to_buy))
                    .Replace("{[nation_name]}", GameData.instance.nationName);
            }
        }
        else
        {
            if (GameData.instance.userRank > GameData.RANK_CAPTAIN)
            {
                // "You cannot buy manpower or energy until you've been promoted at least to captain."
                textPanelObject.SetActive(true);
                textPanelText.text = LocalizationManager.GetTranslation("Buy Panel/you_cannot_buy_manpower_until_text");
            }
            else if (!button2.active)
            {
                textPanelObject.SetActive(true);

                for (;;)
                {
                    // "Market scarcity! Supplies of {[manpower_or_energy]} have run out. It will be available again in {[time_to_eod]}."
                    textPanelText.text = LocalizationManager.GetTranslation("Buy Panel/market_scarcity_unavailable")
                        .Replace("{[manpower_or_energy]}", ((buy_type == BuyType.Manpower) ? "manpower" : "energy"))
                        .Replace("{[time_to_eod]}", GameData.instance.GetDurationClockText((int)(GameData.instance.endOfDayTime - Time.unscaledTime)));

                    yield return new WaitForSeconds(1);
                }
            }
            /*
            else if ((int)limit_factor > 1)
            {
                textPanelObject.SetActive(true);

                for (;;)
                {
                    // "Market scarcity! Prices are {[limit_factor]}x normal due to the amount of {[manpower_or_energy]} we've already bought today. Prices will return to normal in {[time_to_eod]}."
                    textPanelText.text = LocalizationManager.GetTranslation("Buy Panel/market_scarcity_prices_are_higher_text")
                        .Replace("{[limit_factor]}", ((int)limit_factor).ToString())
                        .Replace("{[manpower_or_energy]}", ((buy_type == BuyType.Manpower) ? LocalizationManager.GetTranslation("manpower_word") : LocalizationManager.GetTranslation("energy_word")))
                        .Replace("{[time_to_eod]}", GameData.instance.GetDurationClockText((int)(GameData.instance.endOfDayTime - Time.unscaledTime)));

                    yield return new WaitForSeconds(1);
                }
            }
            */
            else
            {
                textPanelObject.SetActive(false);
            }
        }
    }
}
