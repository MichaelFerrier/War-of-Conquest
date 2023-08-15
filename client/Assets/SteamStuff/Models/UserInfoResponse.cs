using System.Collections;
using System.Collections.Generic;
using UnityEngine;

namespace WoCClient.SteamStuff.Models {
    [System.Serializable]
    public class UserInfoResponseObject
    {
        public UserInfoResponse response;
    }

    [System.Serializable]
    public class UserInfoResponse
    {
        public string result;
        public UserInfoParams @params;
        public UserInfoError error;
    }

    [System.Serializable]
    public class UserInfoParams
    {
        public string state;
        public string country;
        public string currency;
        public string status;
    }

    [System.Serializable]
    public class UserInfoError
    {
        public string errorcode;
        public string errordesc;
    }
}

