#if !DISABLESTEAMWORKS
using Steamworks;
#endif
using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using CI.HttpClient;
using WoCClient.SteamStuff;
using I2.Loc;

public class SteamPayment : MonoBehaviour {

    private string userCountry;
    private string userCurrency;
    private string steamId;
    private string clientLang;
    private bool doneGetCountryCurrency = false;

#if !DISABLESTEAMWORKS
    protected Callback<MicroTxnAuthorizationResponse_t> m_MicroTxnAuthorizationResponse;
#endif

    private static string BaseAPIUrl = "https://api.steampowered.com/ISteamMicroTxnSandbox/GetUserInfo/v2/";
    public static string WocAPIKey = "6E267ED7A086FD06D70509140274967F";

    private void OnEnable()
    {
#if !DISABLESTEAMWORKS
        if(SteamManager.Initialized)
        {
            // Set callback.
            m_MicroTxnAuthorizationResponse = Callback<MicroTxnAuthorizationResponse_t>.Create(OnMicroTxnAuth);
        }
#endif // !DISABLESTEAMWORKS
    }

#if !DISABLESTEAMWORKS
    private void OnMicroTxnAuth(MicroTxnAuthorizationResponse_t pCallback)
    {
        StartCoroutine(OnMicroTxnAuth_Coroutine(pCallback));
    }

    private IEnumerator OnMicroTxnAuth_Coroutine(MicroTxnAuthorizationResponse_t pCallback)
    {
        if (pCallback.m_bAuthorized != 0)
        {
            // make call to WoC server, saying the order was authorized
            // pCallback.m_ulOrderID
            // pCallback.m_unAppID

            var urlComplete = "https://warofconquest.com/payment/steam/complete.php?o=" + pCallback.m_ulOrderID.ToString() + "&a=" + pCallback.m_unAppID.ToString() + "&s=EE31A7A866A099DBDF31910D3D6CC23B478A095E";

            WWW www = new WWW(urlComplete);
            float start_time = Time.unscaledTime;
            while ((www.isDone == false) && (Time.unscaledTime < (start_time + 15f)))
            {
                yield return new WaitForSeconds(1);
            }

            if ((www.error != null) || (www.isDone == false))
            {
                Debug.Log("OnMicroTxnAuth() failed with error '" + www.error + "'. URL: " + urlComplete);
                yield return null;
            }
            else 
            {
                if (www.text.StartsWith("[ERROR]"))
                {
                    // Show error message from the woc server
                    Debug.Log("OnMicroTxnAuth() error: " + www.text);
                }
                else
                {
                    // Success -- the payment has gone through!
                    // "Your credits are on their way!" 
                    Requestor.Activate(0, 0, null, LocalizationManager.GetTranslation("Client Message/credits_on_the_way"), LocalizationManager.GetTranslation("Generic Text/okay"), "");
                }
            }
        }
    }
#endif

    // Use this for initialization
    void Start () {
#if !DISABLESTEAMWORKS
        // necessary for When building for mono based platforms
        // see HttpClient Asset readme.txt
        System.Net.ServicePointManager.ServerCertificateValidationCallback += (o, certificate, chain, errors) =>
        {
            return true;
        };
#endif // !DISABLESTEAMWORKS
    }
	
#if !DISABLESTEAMWORKS
    public IEnumerator InitiateSteamPayment(int package, float price, string desc)
    {
        if (SteamManager.Initialized)
        {
            CSteamID steamIdObj = SteamUser.GetSteamID();

            this.steamId = steamIdObj.ToString();
            var appId = SteamUtils.GetAppID().ToString();

            //Debug.Log("InitiateSteamPayment() steamId: " + this.steamId + ", appId: " + appId);

            if (doneGetCountryCurrency == false)
            {
                // make steam web API calls to get country code and currency
                StartCoroutine(GetCountryandCurrency(steamIdObj));

                // wait for function to finish
                yield return new WaitUntil(() => doneGetCountryCurrency);
            }

            Debug.Log("InitiateSteamPayment() steamId: " + this.steamId + ", appId: " + appId + ", userCurrency: " + userCurrency);

            // submit the information to WoC server
            // Note that Steam requires that the price amount be given in hundredths.
            var requestUrl = new WocOrderRequestUrl(steamId, appId, "en", userCurrency, package, price, desc);
            Debug.Log("InitiateSteamPayment() payment uri: " + requestUrl.InitiatePaymentUri());

            WWW www = new WWW(requestUrl.InitiatePaymentUri().AbsoluteUri);
            float start_time = Time.unscaledTime;
            while ((www.isDone == false) && (Time.unscaledTime < (start_time + 15f)))
            {
                yield return new WaitForSeconds(1);
            }

            if ((www.error != null) || (www.isDone == false))
            {
                GameGUI.instance.LogToChat("Could not connect to warofconquest.com to initiate Steam payment.");
                Debug.Log("InitiateSteamPayment() failed with error '" + www.error + "'. URL: " + requestUrl.InitiatePaymentUri());
                yield return null;
            }
            else 
            {
                if (www.text.StartsWith("[ERROR]"))
                {
                    // Show error message from the woc server
                    GameGUI.instance.LogToChat("Error initiating Steam payment: " + www.text);
                    Debug.Log("InitiateSteamPayment() error: " + www.text);
                }
            }
        }
        else
        {
            // Show message, "Steam is not running!"
            GameGUI.instance.LogToChat("Steam is not running!");
        }
    }
#endif // !DISABLESTEAMWORKS

#if !DISABLESTEAMWORKS
    public IEnumerator GetCountryandCurrency(CSteamID userSteamId)
    {
        userCountry = "";
        userCurrency = "";
        doneGetCountryCurrency = false;

        string steamid = userSteamId.ToString();
        string userInfoUrl = String.Format(BaseAPIUrl + "?key={0}&steamid={1}", WocAPIKey, steamid);

        //Debug.Log("GetCountryandCurrency() userInfoUrl: " + userInfoUrl);

        WWW www = new WWW(userInfoUrl);
        float start_time = Time.unscaledTime;
        while ((www.isDone == false) && (Time.unscaledTime < (start_time + 15f)))
        {
            yield return new WaitForSeconds(1);
        }

        if ((www.error != null) || (www.isDone == false))
        {
            Debug.Log("GetCountryandCurrency() failed with error '" + www.error + "'. URL: " + userInfoUrl);
            doneGetCountryCurrency = true;
            yield return null;
        }
        else 
        {
            //GameGUI.instance.LogToChat("GetCountryandCurrency() result: " + www.text);

            // parse JSON to get country and currency
            var resp = JsonUtility.FromJson<WoCClient.SteamStuff.Models.UserInfoResponseObject>(www.text);

            //GameGUI.instance.LogToChat("resp.result: " + resp.response.result);
            if (resp.response.result == "OK")
            {
                Debug.Log("resp.UserInfoParams.currency: " + resp.response.@params.currency + ", resp.UserInfoParams.country: " + resp.response.@params.country);
                userCountry = resp.response.@params.country;
                userCurrency = resp.response.@params.currency;
            }

            doneGetCountryCurrency = true;
        }
    }
#endif // !DISABLESTEAMWORKS
}
