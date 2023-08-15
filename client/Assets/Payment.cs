using UnityEngine;
using System.Collections;
using System.Collections.Generic;
using Paymentwall;
using I2.Loc;
using System;

public class Payment : MonoBehaviour, RequestorListener
{
    public enum PaymentMethod
    {
        PAYMENTWALL_BRICK = 0,
        PAYMENTWALL_WIDGET = 1,
        BRAINTREE = 2,
        IN_APP_PURCHASE = 3,
        STEAM_MTP = 4, // Steam Microtransaction
        ALT_COINS = 5 // coinpayments.net
    };

    public static string[] paymentMethodNames;

    public static Payment instance;
    public SteamPayment steamPmt;

    public List<int> payment_method_list = new List<int>();
    public List<string> payment_method_name_list = new List<string>();

    private PaymentMethod paymentMethod = PaymentMethod.PAYMENTWALL_BRICK;

    void Awake()
    {
        instance = this;

        paymentMethodNames = new string[] {
            LocalizationManager.GetTranslation("Generic Text/credit_card"), // "Credit Card"
            LocalizationManager.GetTranslation("Generic Text/bank_transfer_prepaid"), // "Bank Transfer, Prepaid"
            LocalizationManager.GetTranslation("Generic Text/credit_card") + ", Paypal", // "Credit Card, PayPal"
            LocalizationManager.GetTranslation("Generic Text/in_app_purchase"), // "In-App Purchase"
            LocalizationManager.GetTranslation("Generic Text/Steam"), // "Steam"
            LocalizationManager.GetTranslation("Generic Text/altcoins") // "Altcoins"
        };

#if UNITY_EDITOR || UNITY_STANDALONE || UNITY_WEBPLAYER || UNITY_WEBGL
#if DISABLESTEAMWORKS
        payment_method_list.Add((int)PaymentMethod.BRAINTREE); payment_method_name_list.Add(paymentMethodNames[(int)PaymentMethod.BRAINTREE]);
        payment_method_list.Add((int)PaymentMethod.PAYMENTWALL_WIDGET); payment_method_name_list.Add(paymentMethodNames[(int)PaymentMethod.PAYMENTWALL_WIDGET]);
        //payment_method_list.Add((int)PaymentMethod.ALT_COINS); payment_method_name_list.Add(paymentMethodNames[(int)PaymentMethod.ALT_COINS]);
        paymentMethod = PaymentMethod.BRAINTREE;
#else
        if (SteamManager.Initialized)
        {
            payment_method_list.Add((int)PaymentMethod.STEAM_MTP); payment_method_name_list.Add(paymentMethodNames[(int)PaymentMethod.STEAM_MTP]);
            paymentMethod = PaymentMethod.STEAM_MTP;
        }
#endif // DISABLESTEAMWORKS
#elif UNITY_ANDROID || UNITY_IOS
        payment_method_list.Add((int)PaymentMethod.BRAINTREE); payment_method_name_list.Add(paymentMethodNames[(int)PaymentMethod.BRAINTREE]);
        payment_method_list.Add((int)PaymentMethod.PAYMENTWALL_WIDGET); payment_method_name_list.Add(paymentMethodNames[(int)PaymentMethod.PAYMENTWALL_WIDGET]);
        payment_method_list.Add((int)PaymentMethod.ALT_COINS); payment_method_name_list.Add(paymentMethodNames[(int)PaymentMethod.ALT_COINS]);
        paymentMethod = PaymentMethod.BRAINTREE;

        //payment_method_list.Add((int)PaymentMethod.IN_APP_PURCHASE); payment_method_name_list.Add(paymentMethodNames[(int)PaymentMethod.IN_APP_PURCHASE]);
        //paymentMethod = PaymentMethod.IN_APP_PURCHASE;
#endif

        // Fetch the stored payment method, if it exists.
        if (PlayerPrefs.HasKey("payment_method"))
        {
            PaymentMethod storedPaymentMethod = (PaymentMethod)PlayerPrefs.GetInt("payment_method");

            // Use the stored payment method if it is currently available. Otherwise use the default.
            if (payment_method_list.Contains((int)storedPaymentMethod)) {
                paymentMethod = storedPaymentMethod;
            }
        }
    }

	// Use this for initialization
	void Start ()
    {
	}

    public PaymentMethod GetPaymentMethod()
    {
        return paymentMethod;
    }

    public void SetPaymentMethod(PaymentMethod _method)
    {
        paymentMethod = _method;
        PlayerPrefs.SetInt("payment_method", (int)paymentMethod);
    }

	public void PurchaseCredits(int _package)
    {
        // If the payment method doesn't itself allow for e-mail address input, we need an e-mail address first. Ask the player to create an account before buying credits this way.
        if (GameData.instance.userIsRegistered == false)
        {
            if (paymentMethod == PaymentMethod.PAYMENTWALL_WIDGET)
            {
               // "Please create an account before buying credits\nusing bank transfer or a prepaid card."
                Requestor.Activate(0, 0, this, LocalizationManager.GetTranslation("Client Message/create_new_account_before_buying")
                    , LocalizationManager.GetTranslation("Generic Text/okay"), "");
                return;
            }
            else if (paymentMethod == PaymentMethod.BRAINTREE)
            {
                // "Please create an account before buying credits\nusing a credit card or PayPal."
                Requestor.Activate(0, 0, this, LocalizationManager.GetTranslation("Client Message/create_new_account_before_buying_cc")
                    , LocalizationManager.GetTranslation("Generic Text/okay"), "");
                return;
            }
        }

        string item_name = GameGUI.instance.buyCreditsPackageName[_package] + ": " + GameData.instance.buyCreditsAmount[_package]
            + " " + LocalizationManager.GetTranslation("Generic Text/credits_word");

        /*
        // PayPal
        Application.OpenURL("https://www.paypal.com/xclick/business=warofconquest%40ironzog.com&item_name=" + item_name + "&amount=%24" + GameData.instance.buyCreditsCostUSD[_package] + "&custom=" + GameData.instance.serverID + "," + GameData.instance.userID + "," + _package + "&no_shipping=1&return=http%3A//warofconquest.com/purchase_return.php&cancel_return=http%3A//warofconquest.com/purchase_cancel.htm&no_note=1");
        */

        if (paymentMethod == PaymentMethod.PAYMENTWALL_BRICK)
        {
            // Paymentwall Brick
            ActivatePaymentwallBrick(GameData.instance.buyCreditsCostUSD[_package], "USD", item_name, GameData.instance.serverID + "," + GameData.instance.userID + "," + GameData.instance.buyCreditsCostUSD[_package].ToString("F") + ",USD," + _package, false/*true*/);
        }
        else if (paymentMethod == PaymentMethod.PAYMENTWALL_WIDGET)
        {
            // Paymentwall widget
            item_name = GameData.instance.buyCreditsAmount[_package] + " " + LocalizationManager.GetTranslation("Generic Text/credits_word");
            ActivatePaymentwallWidget("" + GameData.instance.userID, _package, item_name, GameData.instance.buyCreditsCostUSD[_package], "USD", GameData.instance.serverID + "," + GameData.instance.userID + "," + GameData.instance.buyCreditsCostUSD[_package].ToString("F") + ",USD," + _package + "," + GameData.instance.email, false/*true*/, false);
        }
        else if (paymentMethod == PaymentMethod.BRAINTREE)
        {
            // Braintree
            ActivateBraintreePurchase(_package, GameData.instance.serverID, GameData.instance.userID, GameData.instance.email);
        }
        else if (paymentMethod == PaymentMethod.STEAM_MTP)
        {
            ActivateSteamPurchase(_package);
        }
        else if (paymentMethod == PaymentMethod.ALT_COINS)
        {
            // Coinpayments.net
            ActivateAltCoinsPurchase(_package, item_name, GameData.instance.serverID, GameData.instance.userID, GameData.instance.buyCreditsCostUSD[_package]);
        }
	}

    public void RequestorResponse(int _task, int _data, Requestor.RequestorButton _result)
    {
        GameGUI.instance.OpenCreatePasswordPanel(CreatePasswordPanel.Context.OptionsPanel);
    }

    public void ActivatePaymentwallBrick(double _amount, string _currency, string _item_name, string _custom, bool _test)
    {
        if (_test)
        {
            PWBase.SetApiMode(API_MODE.TEST);
            PWBase.SetAppKey("insert key here");
            PWBase.SetSecretKey("insert key here");
        }
        else
        {
            PWBase.SetApiMode(API_MODE.LIVE);
            PWBase.SetAppKey("insert key here");
            PWBase.SetSecretKey("insert key here");
        }

        PWBrick brick = new PWBrick(_amount, _currency, "War of Conquest", _item_name, Network.instance.GetClientID(), _custom, OnPaymentResult);
        brick.ShowPaymentForm();
    }

    public void ActivatePaymentwallWidget(string _userID, int _product_id, string _product_name, float _price, string _currency, string _custom, bool _evaluation_mode, bool _test_mode)
    {
        Dictionary<string, string> additional_params = new Dictionary<string, string>();

        // Add user info as additional parameters
        additional_params.Add("email", "" + GameData.instance.email);
        additional_params.Add("history[registration_date]", "" + GameData.instance.userCreationTime);

        if (_evaluation_mode) {
            additional_params.Add("evaluation", "1");
        }

        if (_test_mode) {
            additional_params.Add("test_mode", "1");
        }

        PWBase.SetApiType(PWBase.API_GOODS);
        PWBase.SetAppKey("insert key here"); // available in your Paymentwall merchant area
        PWBase.SetSecretKey("insert key here"); // available in your Paymentwall merchant area

        List<PWProduct> productList = new List<PWProduct>();
        PWProduct product = new PWProduct(
            _custom, // id of the product in your system
            _price, // price
            _currency, // currency code
            _product_name, // product name
            PWProduct.TYPE_FIXED // this is a time-based product; for one-time products, use Paymentwall_Product.TYPE_FIXED and omit the following 3 parameters
            );
        productList.Add(product);
        PWWidget widget = new PWWidget(
            _userID, // id of the end-user who's making the payment
            "p1_1", // widget code, e.g. p1; can be picked inside of your merchant account
            productList,
            additional_params
        );

        PWUnityWidget unity = new PWUnityWidget (widget);
        StartCoroutine (unity.callWidgetWebView (gameObject,MapView.instance.canvas)); // call this function to display widget
    }

    public void ActivateBraintreePurchase(int _package, int _serverID, int _userID, string _email)
    {
        StartCoroutine(ActivateBraintreePurchase_Coroutine(_package, _serverID, _userID, _email));
    }

    public void ActivateAltCoinsPurchase(int _package, string _item_name, int _serverID, int _userID, float _price)
    {
        // Call payment url
        String url = "https://www.warofconquest.com/payment/altcoin/initpmt.php?server_id=" + _serverID + "&user_id=" + _userID + "&package=" + _package + "&item_name=" + _item_name + "&price=" + GameData.instance.buyCreditsCostUSD[_package];
        Debug.Log("ActivateAltCoinsPurchase() about to open url: " + url);
        Application.OpenURL(url);
    }

    private void ActivateSteamPurchase(int package)
    {
#if !DISABLESTEAMWORKS
        var price = GameData.instance.buyCreditsCostUSD[package];
        string itemName = String.Format("{0}: {1} {2}",
            GameGUI.instance.buyCreditsPackageName[package],
            GameData.instance.buyCreditsAmount[package],
            LocalizationManager.GetTranslation("Generic Text/credits_word"));

        steamPmt.StartCoroutine(steamPmt.InitiateSteamPayment(package, price, itemName));
#endif // !DISABLESTEAMWORKS
    }

    public IEnumerator ActivateBraintreePurchase_Coroutine(int _package, int _serverID, int _userID, string _email)
    {
        string url;

        // Fetch limited time token info from web server
        WWWForm form = new WWWForm();
        form.AddField( "package", _package );
        form.AddField( "serverID", _serverID );
        form.AddField( "userID", _userID );
        form.AddField( "email", _email );
        url = "https://warofconquest.com/payment/bt_get_token.php";
		WWW www = new WWW(url, form);

        while (www.isDone == false) {
            yield return new WaitForSeconds(0.2f);
        }

        if (www.error != null) {
            yield return null;
        }

        // Call payment url, and tack on to url what was returned by the token call.
        url = "https://warofconquest.com/paypal-payment/?pkg=" + _package + "&s=" + _serverID + "&u=" + _userID + "&e=" + _email + www.text;
        Debug.Log("ActivateBraintreePurchase() about to open url: " + url);
        Application.OpenURL(url);
    }

	public void OnPaymentResult(bool _success, string _error_message)
    {
        if (_success)
        {
            // "Your credits are on their way!"
            Requestor.Activate(0, 0, null, LocalizationManager.GetTranslation("Client Message/credits_on_the_way")
                , LocalizationManager.GetTranslation("Generic Text/okay"), "");
        }
        else
        {
            Requestor.Activate(0, 0, null, _error_message, LocalizationManager.GetTranslation("Generic Text/okay"), "");
        }

        StartCoroutine(ProcessPayment());
    }

    public IEnumerator ProcessPayment()
    {
        // After brief delay, tell the server to fetch the payment information.
        yield return new WaitForSeconds(15);
        Network.instance.SendCommand("action=get_payment");
    }

    public void PurchaseSubscription(int _tier)
    {
        // If the payment method doesn't itself allow for e-mail address input, we need an e-mail address first. Ask the player to create an account before buying credits this way.
        if (GameData.instance.userIsRegistered == false)
        {
            if (paymentMethod == PaymentMethod.BRAINTREE)
            {
                // "Please create an account before subscribing\nusing a credit card or PayPal."
                Requestor.Activate(0, 0, this, LocalizationManager.GetTranslation("Client Message/create_new_account_before_subscribing_cc")
                    , LocalizationManager.GetTranslation("Generic Text/okay"), "");
                return;
            }
        }

        string item_name = GameGUI.instance.subscriptionTierName[_tier] + " " + LocalizationManager.GetTranslation("Subscribe Panel/word_subscription");

        if (paymentMethod == PaymentMethod.BRAINTREE)
        {
            // Braintree
            ActivateBraintreeSubscribe(_tier, GameData.instance.serverID, GameData.instance.userID, GameData.instance.email);
        }
        else if (paymentMethod == PaymentMethod.STEAM_MTP)
        {
            ActivateSteamSubscribe(_tier, item_name);
        }
	}

    public void ActivateBraintreeSubscribe(int _tier, int _serverID, int _userID, string _email)
    {
        StartCoroutine(ActivateBraintreeSubscribe_Coroutine(_tier, _serverID, _userID, _email));
    }

    private void ActivateSteamSubscribe(int _tier, string _itemName)
    {
#if !DISABLESTEAMWORKS
        var price = GameData.instance.subscriptionCostUSD[_tier];
        steamPmt.StartCoroutine(steamPmt.InitiateSteamPayment(_tier, price, _itemName));
#endif // !DISABLESTEAMWORKS
    }

    public IEnumerator ActivateBraintreeSubscribe_Coroutine(int _tier, int _serverID, int _userID, string _email)
    {
        string url;

        // Fetch limited time token info from web server
        WWWForm form = new WWWForm();
        form.AddField( "package", _tier );
        form.AddField( "serverID", _serverID );
        form.AddField( "userID", _userID );
        form.AddField( "email", _email );
        url = "https://warofconquest.com/payment/bt_get_token.php";
		WWW www = new WWW(url, form);

        while (www.isDone == false) {
            yield return new WaitForSeconds(0.2f);
        }

        if (www.error != null) {
            yield return null;
        }

        // Call subscribe url, and tack on to url what was returned by the token call.
        url = "https://warofconquest.com/paypal-subscribe/?pkg=" + _tier + "&s=" + _serverID + "&u=" + _userID + "&e=" + _email + www.text;
        Debug.Log("ActivateBraintreePurchase() about to open url: " + url);
        Application.OpenURL(url);
    }
}
