using UnityEngine;
using UnityEngine.UI;
using System.Collections;
using I2.Loc;

public class JoinNationPanel : MonoBehaviour
{
    public static JoinNationPanel instance;

    public InputField nation_name_field, password_field;
    public Text message_text;

    public JoinNationPanel()
    {
        instance = this;
    }

    public void Reset()
    {
        message_text.text = "";
        password_field.text = "";
    }

    public void JoinNationResult(bool _success, string _message)
    {
        if (_success)
        {
            // Close the log in panel.
            GameGUI.instance.CloseAllPanels();

            //// Close the menu screen
            //GameGUI.instance.CloseMenuScreen();
        }
        else
        {
            // Display the given message.
            message_text.text = _message;
        }
    }

    public void OnClick_Done()
    {
        string nation_name = nation_name_field.text;
        string password = password_field.text;

        // Remove any control characters from the strings
        nation_name = GameGUI.RemoveControlCharacters(nation_name);
        nation_name = nation_name.Replace("|", "");
        password = GameGUI.RemoveControlCharacters(password);
        password = password.Replace("|", "");

        // Display modified entries
        nation_name_field.text = nation_name;
        password_field.text = password;

        // GB-Localization
        if (nation_name.Length == 0)
        {
            // "Please enter name of the nation to join"
            message_text.text = LocalizationManager.GetTranslation("name_of_nation_to_join");
            return;
        }

        if (password.Length == 0)
        {
            // "Please enter the nation's password to join"
            message_text.text = LocalizationManager.GetTranslation("password_of_nation_to_join");
            return;
        }

        // Send join nation message to server.
        Network.instance.SendCommand("action=join_nation|nation=" + nation_name + "|password=" + password);
    }

    public void OnClick_Close()
    {
        GameGUI.instance.CloseAllPanels();
    }
}
