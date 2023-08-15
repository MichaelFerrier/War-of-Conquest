using System;
using UnityEngine;
using UnityEngine.UI;
using System.Collections;
using I2.Loc;

public class LogInPanel : MonoBehaviour
{
    public static LogInPanel instance;

    public InputField username_field, password_field;
    public Text message_text;

    public Dropdown serverDropdown;

    // Use this for initialization
    public LogInPanel()
    {
        instance = this;
    }

    public void Reset()
    {
        message_text.text = "";
        password_field.text = "";
    }

    public void Init()
    {
        // Populate the server dropdown, showing hidden servers if a control key is held down.
        GameGUI.instance.PopulateServerDropdown(serverDropdown, Input.GetKey(KeyCode.LeftControl) || Input.GetKey(KeyCode.RightControl));
    }

    public void LogInResult(bool _success, string _message)
    {
        if (_success)
        {
            // This shouldn't be necessary -- entering the game will close all panels. And, don't want to close the report panel after entering game.
            //// Close the log in panel.
            //GameGUI.instance.CloseAllPanels();

            //// Close the menu screen
            //GameGUI.instance.CloseMenuScreen();
        }
        else
        {
            // Display the given message.
            message_text.text = _message;
        }
    }

    public void OnEndEdit_Password()
    {
        // If enter was pressed, handle as if submit button were pressed.
        if (Input.GetKeyDown(KeyCode.Return) || Input.GetKeyDown(KeyCode.KeypadEnter)) {
            OnClick_Done();
        }
    }

    public void OnClick_Done()
    {
        string username = username_field.text;
        string password = password_field.text;

        // Remove any control characters from the strings
        username = GameGUI.RemoveControlCharacters(username);
        username = username.Replace("|", "");
        password = GameGUI.RemoveControlCharacters(password);
        password = password.Replace("|", "");

        // Display modified entries
        username_field.text = username;
        password_field.text = password;

        if (username.Length == 0)
        {
            message_text.text = LocalizationManager.GetTranslation("enter_your_username"); // "Please enter your username"
            return;
        }

        if (password.Length == 0)
        {
            message_text.text = LocalizationManager.GetTranslation("enter_your_password"); // "Please enter your password";
            return;
        }

        // Determine the ID of the selected server.
        int serverID = GameGUI.instance.availableServerIDs[Mathf.Max(0, Mathf.Min(GameGUI.instance.availableServerIDs.Count - 1, serverDropdown.value))];

        // If the client is not already connected to the selected game server...
        if (Network.instance.GetConnectedServerID() != serverID)
        {
            // Attempt to connect to the selected server.
            Network.SetupSocketResult result = Network.instance.SetupSocket(serverID);

            if (result == Network.SetupSocketResult.Failure)
            {
                message_text.text = LocalizationManager.GetTranslation("connect_temporarily_offline");
                return;
            }
            else if (result == Network.SetupSocketResult.Exception)
            {
                message_text.text = LocalizationManager.GetTranslation("connect_socket_exception").Replace("{PORT}", string.Format("{0:n0}", Network.instance.GetServerPort(serverID)));
                return;
            }

            // Send connection message to the game server.
            Network.instance.ConnectWithGameServer(false);
        }

        // Once maps have been fully loaded for this server, send login message.
        StartCoroutine(SubmitOnceMapsLoaded(username, password));
    }

    public void OnClick_Close()
    {
        if (GameGUI.instance.IsInGame() && (GameGUI.instance.active_game_panel == GameGUI.GamePanel.GAME_PANEL_OPTIONS)) {
            GameGUI.instance.CloseAllPanels();
        } else {
            GameGUI.instance.OpenWelcomePanel();
        }
    }

    public void OnClick_ForgotPassword()
    {
        Application.OpenURL("https://warofconquest.com/reset-password/");
    }

    public void OnClick_ServerInfo()
    {
        string text = LocalizationManager.GetTranslation("Game Servers/" + Network.instance.GetServerInfo(GameGUI.instance.availableServerIDs[serverDropdown.value]).description);
        Requestor.Activate(0, 0, null, text, LocalizationManager.GetTranslation("Generic Text/okay"), "");
    }

    public IEnumerator SubmitOnceMapsLoaded(string _username, string _password)
    {
        // TESTING
        if (Network.instance.initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false)) {
            Network.instance.LogEvent(Time.time + ": Client ID " + Network.instance.GetClientID(true) + " LogInPanel SubmitOnceMapsLoaded().");
        }

        message_text.text = LocalizationManager.GetTranslation("connect_logging_in");

        // Wait until the maps have been fully loaded, before sending login message.
        while (Network.instance.maps_loaded == false) {
            Debug.Log("Waiting to send login message...");
            yield return new WaitForSeconds(0.2f);
        }

        // TESTING
        if (Network.instance.initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false)) {
            Network.instance.LogEvent(Time.time + ": Client ID " + Network.instance.GetClientID(true) + " LogInPanel SubmitOnceMapsLoaded(): maps loaded. About to send login_in for username " + _username + ".");
        }

        // Send login message to the game server.
        Debug.Log("Sending login message...");
        Network.instance.SendCommand("action=log_in|username=" + _username + "|password=" + _password);
    }
}
