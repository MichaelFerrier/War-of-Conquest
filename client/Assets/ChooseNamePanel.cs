using UnityEngine;
using UnityEngine.UI;
using System.Collections;
using I2.Loc;

public class ChooseNamePanel : MonoBehaviour
{
    public static ChooseNamePanel instance;

    public InputField entry_inputfield;
    public Text entry_text, message_text;

    public ChooseNamePanel()
    {
        instance = this;
    }

    public void Reset()
    {
        // Set entry field character limit
        entry_inputfield.characterLimit = GameGUI.MAX_USERNAME_LENGTH;

        // Clear the username and message fields.
        entry_text.text = "";
        message_text.text = "";
    }

    public void OnClick_Done()
    {
        // Clear fields
        Reset();

        // Check all input

        // Username
        string username = entry_text.text;

        if (username.Length == 0)
        {
            // No input given; just close the panel.
            GameGUI.instance.CloseAllPanels();
            return;
        }

        // GB-Localization
        if ((username.Length < GameGUI.MIN_USERNAME_LENGTH) || (username.Length > GameGUI.MAX_USERNAME_LENGTH))
        {
            // "Username must be between " + GameGUI.MIN_USERNAME_LENGTH + " and " + GameGUI.MAX_USERNAME_LENGTH + " characters long."
            // "Username must be between {[MIN_USERNAME_LENGTH]} and {[MAX_USERNAME_LENGTH]} characters long."
            message_text.text = LocalizationManager.GetTranslation("username_length_validation_notice")
                .Replace("{[MIN_USERNAME_LENGTH]}", GameGUI.MIN_USERNAME_LENGTH.ToString())
                .Replace("{[MAX_USERNAME_LENGTH]}", GameGUI.MAX_USERNAME_LENGTH.ToString());
            return;
        }

        if (username.IndexOf(' ') != -1)
        {
            // "A username cannot contain spaces."
            message_text.text = LocalizationManager.GetTranslation("username_cannot_contain_spaces");
            return;
        }

        // Send message to change username.
        Network.instance.SendCommand("action=set_username|name=" + username);
    }

    public void OnClick_Close()
    {
        // Clear fields
        Reset();

        GameGUI.instance.CloseAllPanels();
    }

    public void OnEdit_Username()
    {
        string username = entry_text.text;

        if (username.Length == 0) {
            return;
        }

        // GB-Localization
        if ((username.Length < GameGUI.MIN_USERNAME_LENGTH) || (username.Length > GameGUI.MAX_USERNAME_LENGTH))
        {
            // "Username must be between " + GameGUI.MIN_USERNAME_LENGTH + " and " + GameGUI.MAX_USERNAME_LENGTH + " characters long.";
            message_text.text = LocalizationManager.GetTranslation("username_length_validation_notice")
                .Replace("{[MIN_USERNAME_LENGTH]}", GameGUI.MIN_USERNAME_LENGTH.ToString())
                .Replace("{[MAX_USERNAME_LENGTH]}", GameGUI.MAX_USERNAME_LENGTH.ToString());
            return;
        }

        if (username.IndexOf(' ') != -1)
        {
            // "A username cannot contain spaces."
            message_text.text = LocalizationManager.GetTranslation("username_cannot_contain_spaces");
            return;
        }

        // Send message to server asking it whether this username is already taken.
        Network.instance.SendCommand("action=set_username|name=" + username + "|check=1");
    }

    public void UsernameAvailable(bool _isAvailable, bool _isSet)
    {
        if (_isSet)
        {
            // Clear fields
            Reset();

            // The username has been set; close this panel.
            GameGUI.instance.CloseAllPanels();

            // Record the fact that the username has been customized, and update the GUI.
            GameData.instance.SetUserFlag(GameData.UserFlags.CUSTOM_USERNAME, true);
            OptionsPanel.instance.Layout();
        }
        else
        {
            // GB-Localization
            if (_isAvailable)
            {
                // "Available"
                message_text.text = LocalizationManager.GetTranslation("Generic Text/available_word"); 
            }
            else
            {
                // "This name is not available"
                message_text.text = LocalizationManager.GetTranslation("this_name_is_not_available");
            }
        }
    }
}
