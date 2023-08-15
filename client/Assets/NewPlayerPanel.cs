using System;

using System.Collections;

using System.Collections.Generic;

using UnityEngine;

using UnityEngine.Networking;
using UnityEngine.UI;

using I2.Loc;



public class NewPlayerPanel : MonoBehaviour, RequestorListener
{
    public static NewPlayerPanel instance;

    public InputField userNameField, patronCodeField;
    public Text userNameMessage, patronCodeMessage, messageText;
    public Toggle patronCodeYesToggle;
    public Dropdown serverDropdown;
    public GameObject patronCodePanel, chooseServerPanel;

    string username, patron_code;
    int serverID;

    public NewPlayerPanel()
    {
        instance = this;
    }

    public void Start()
    {
        // Set entry field character limit
        userNameField.characterLimit = GameGUI.MAX_USERNAME_LENGTH;
        patronCodeField.characterLimit = 40;
    }

    public void Init()
    {
        // Populate the server dropdown, showing hidden servers if a control key is held down.
        GameGUI.instance.PopulateServerDropdown(serverDropdown, Input.GetKey(KeyCode.LeftControl) || Input.GetKey(KeyCode.RightControl));

        // Reset fields
        userNameField.text = "";
        patronCodeField.text = "";
        ResetMessageFields();
    }

    public void ResetMessageFields()
    {
        userNameMessage.text = "";
        patronCodeMessage.text = "";
        messageText.text = LocalizationManager.GetTranslation("new_player_intro");
    }

    public void OnClick_PatronCodeYes()
    {
        patronCodePanel.SetActive(true);
        chooseServerPanel.SetActive(false);
    }

    public void OnClick_PatronCodeNo()
    {
        patronCodePanel.SetActive(false);
        chooseServerPanel.SetActive(true);
    }

    public void OnClick_PatronCodeInfo()
    {
        String text = LocalizationManager.GetTranslation("new_player_patron_code_info"); //  "You can get a patron code from any War of Conquest player. Signing up with a patron code will start you off with an extra 50 credits, and you will join the same server as the player who gave you the code.\n\nThat player will also become your patron - you will each get as a bonus 2% of the XP that the other earns, and 10% of any credits that they buy!"
        Requestor.Activate(0, 0, null, text, LocalizationManager.GetTranslation("Generic Text/okay"), "");
    }

    public void OnClick_ServerInfo()
    {
        String text = LocalizationManager.GetTranslation("Game Servers/" + Network.instance.GetServerInfo(GameGUI.instance.availableServerIDs[serverDropdown.value]).description);
        Requestor.Activate(0, 0, null, text, LocalizationManager.GetTranslation("Generic Text/okay"), "");
    }

    public void OnClick_Next()
    {
        ResetMessageFields();

        // Check all inputs

        // Username
        username = userNameField.text;

        // Remove any control characters from the string
        username = GameGUI.RemoveControlCharacters(username);

        if ((username.Length < GameGUI.MIN_USERNAME_LENGTH) || (username.Length > GameGUI.MAX_USERNAME_LENGTH))
        {
            // "Username must be between " + GameGUI.MIN_USERNAME_LENGTH + " and " + GameGUI.MAX_USERNAME_LENGTH + " characters long.";
            userNameMessage.text = LocalizationManager.GetTranslation("username_length_validation_notice")
                .Replace("{[MIN_USERNAME_LENGTH]}", GameGUI.MIN_USERNAME_LENGTH.ToString())
                .Replace("{[MAX_USERNAME_LENGTH]}", GameGUI.MAX_USERNAME_LENGTH.ToString());

            return;
        }

        if (username.IndexOf('|') != -1)
        {
            // "Names and passwords cannot contain "|"."
            userNameMessage.text = LocalizationManager.GetTranslation("names_and_passwords_cannot_contain")
                .Replace("{character}", "|");
            return;
        }

        // Patron code
        patron_code = patronCodeYesToggle.isOn ? patronCodeField.text : "";

        if (patronCodeYesToggle.isOn && (patron_code.Length == 0))
        {
            // "If you have a patron code, enter it here."
            patronCodeMessage.text = LocalizationManager.GetTranslation("patron_code_enter_here");
            return;
        }

        if (patronCodeYesToggle.isOn)
        {
            // Determine the server ID from the patron code, and then attempt to create the new player account.
            StartCoroutine(PatronCodeToServerID());
        }
        else
        {
            // Determine the ID of the selected server.
            serverID = GameGUI.instance.availableServerIDs[serverDropdown.value];

            // We already know the server ID, so go straight to attempting to create the new player account.
            AttemptCreatePlayerAccount();
        }
    }

    public IEnumerator PatronCodeToServerID()
    {
        // Check whether the patron code is valid.
        string url = Network.instance.fileUrl + "server_from_patron_code.php?code=" + patron_code.Replace(" ", "");
		UnityWebRequest www = UnityWebRequest.Get(url);
		yield return Network.instance.RequestFromWeb(www);

        //Debug.Log("url: " + url + ", error: " + www.error + ", text: " + www.downloadHandler.text);

		if (www.isNetworkError || www.isHttpError)
		{
	        patronCodeMessage.text = LocalizationManager.GetTranslation("patron_code_could_not_lookup");
            yield break;
		}

        if (Int32.TryParse(www.downloadHandler.text, out serverID) == false)
        {
            patronCodeMessage.text = LocalizationManager.GetTranslation("patron_code_could_not_lookup");
            yield break;
        }

        if (serverID == -1)
        {
            patronCodeMessage.text = LocalizationManager.GetTranslation("patron_code_invalid");
            yield break;
        }

        // We've determined the server ID based on the patron code, so now attempt to create the new player account.
        AttemptCreatePlayerAccount();
    }

    public void AttemptCreatePlayerAccount()
    {
        Debug.Log("About to create new player account for username: " + username + ", patron code: " + patron_code + ", serverID: " + serverID);

        // If the client is not already connected to the selected game server...
        if (Network.instance.GetConnectedServerID() != serverID)
        {
            // Attempt to connect to the selected server.
            Network.SetupSocketResult result = Network.instance.SetupSocket(serverID);

            if (result == Network.SetupSocketResult.Failure)
            {
                // TESTING
                if (Network.instance.initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false)) {
                    Network.instance.LogEvent(Time.time + ": Client ID " + Network.instance.GetClientID(true) + " AttemptCreatePlayerAccount() SetupSocket() failed for server " + serverID + ".");
                }

                messageText.text = LocalizationManager.GetTranslation("connect_temporarily_offline");
                return;
            }
            else if (result == Network.SetupSocketResult.Exception)
            {
                // TESTING
                if (Network.instance.initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false)) {
                    Network.instance.LogEvent(Time.time + ": Client ID " + Network.instance.GetClientID(true) + " AttemptCreatePlayerAccount() SetupSocket() exception for server " + serverID + ".");
                }

                messageText.text = LocalizationManager.GetTranslation("connect_socket_exception").Replace("{PORT}", string.Format("{0:n0}", Network.instance.GetServerPort(serverID)));
                return;
            }

            // TESTING
            if (Network.instance.initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false)) {
                Network.instance.LogEvent(Time.time + ": Client ID " + Network.instance.GetClientID(true) + " NewPlayerPanel AttemptCreatePlayerAccount() SetupSocket() succeeded for server " + serverID + ". About to call ConnectWithGameServer() for username " + username + ", serverID " + serverID + ".");
            }

            // Send connection message to the game server.
            Network.instance.ConnectWithGameServer(false);
        }

        // Send new player message to the game server.
        // Note we don't need to wait until the maps are loaded. That's done before submitting customize_nation event.
        Network.instance.SendCommand("action=new_player|username=" + username + "|patron_code=" + patron_code);
    }

    public void NewPlayerResult(bool _success, string _message)
    {
        if (_success)
        {
            // Associate this client with the currently connected game server, so that if it is disconnected now,
            // it will be reconnected with the server that it just created a player account on.
            Network.instance.AssociateWithConnectedServer();

            // Ask the player if they'd like to create a password for their account now.
            Requestor.Activate(0, 0, this
                , LocalizationManager.GetTranslation("new_player_create_password") // "Would you like to create a password for your account?\n\nA password protects your progress and lets you log into this account on other devices."
                , LocalizationManager.GetTranslation("Generic Text/yes_word"), LocalizationManager.GetTranslation("Generic Text/maybe_later_word"));
        }
        else
        {
            // Display the given message.
            messageText.text = _message;
        }
    }

    public void RequestorResponse(int _task, int _data, Requestor.RequestorButton _result)
    {
        if (_result == Requestor.RequestorButton.LeftButton)
        {
            // Close this panel and open the create password panel.
            GameGUI.instance.OpenCreatePasswordPanel(CreatePasswordPanel.Context.AccountCreation);
        }
        else
        {
            // Close this panel and open the new nation panel.
            GameGUI.instance.OpenNewNationPanel();
        }
    }
}
