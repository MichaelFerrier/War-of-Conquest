using System;
using System.Collections;
using System.Collections.Generic;
using System.Text;
using UnityEngine;

namespace WoCClient.SteamStuff
{
    public class WocOrderRequestUrl
    {
        public bool isTest = false;
        private string steamId;
        private string appId;
        private string lang;
        private string currency;
        private int package;
        private float price;
        private string itemDesc;
        private string email;
        private int server_id;
        private int user_id;

        public string baseWocUrl = "https://warofconquest.com/payment/steam/";

        public WocOrderRequestUrl(string steamId, string appId, string lang, string currency, int package, float price, string itemDesc)
        {
            this.steamId = steamId;
            this.appId = appId;
            this.lang = lang;
            this.currency = currency;
            this.package = package;
            this.price = price;
            this.itemDesc = itemDesc;

            this.email = GameData.instance.email;
            this.server_id = GameData.instance.serverID;
            this.user_id = GameData.instance.userID;
        }

        public Uri InitiatePaymentUri()
        {
            var sburl = new StringBuilder();

            sburl.AppendFormat("?key={0}", SteamPayment.WocAPIKey);
            sburl.AppendFormat("&server_id={0}", server_id);
            sburl.AppendFormat("&user_id={0}", user_id);
            sburl.AppendFormat("&stapp_id={0}", appId);
            sburl.AppendFormat("&stuser_id={0}", steamId);
            sburl.AppendFormat("&email={0}", WWW.EscapeURL(email));
            sburl.AppendFormat("&currency={0}", currency);
            sburl.AppendFormat("&price={0}", price);
            sburl.AppendFormat("&package={0}", package);
            sburl.AppendFormat("&desc={0}", WWW.EscapeURL(itemDesc));
            sburl.AppendFormat("&lang={0}", lang);

            return new Uri(baseWocUrl + "initpmt.php" + sburl.ToString());
        }
    }
}
