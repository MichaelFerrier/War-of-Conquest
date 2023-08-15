using UnityEngine;
using UnityEngine.UI;
using System.Collections;
using I2.Loc;

public class PostMessagePanel : MonoBehaviour
{
    public static PostMessagePanel instance;

    public InputField recipient_nation_name_field, message_field;
    public Text message_text;

    public PostMessagePanel()
    {
        instance = this;
    }

    public void Reset()
    {
        recipient_nation_name_field.text = "";
        message_text.text = "";
        message_field.text = "";
    }

    public void SetRecipient(string _nation_name)
    {
        recipient_nation_name_field.text = _nation_name;
    }

    public bool IsClear()
    {
        return (message_field.text == "");
    }

    public void PostMessageResult(bool _success, string _message)
    {
        if (_success)
        {
            // Reset and close the post message panel.
            Reset();
            GameGUI.instance.CloseAllPanels();
        }
        else
        {
            // Display the given message.
            message_text.text = _message;
        }
    }

    public void OnClick_Send()
    {
        // GB-Localization
        string recipient_nation_name = recipient_nation_name_field.text;
        string message = message_field.text;

        // Remove any control characters from the string
        message = GameGUI.RemoveControlCharacters(message);
        message = message.Replace("|", "/");

		// TEST appending emoji to end of message
		//message = message + "\uD83D\uDC71";

        if (recipient_nation_name.Length == 0)
        {
            message_text.text = LocalizationManager.GetTranslation("enter_name_recipient_nation"); // "Please enter name of the recipient nation"
            return;
        }

        if (message.Length == 0)
        {
            message_text.text = LocalizationManager.GetTranslation("enter_message"); // "Please enter a message"
            return;
        }

        // Send join nation message to server.
        Network.instance.SendCommand("action=post_message|recipient=" + recipient_nation_name + "|text=" + message);
    }

    public void OnClick_Cancel()
    {
        Reset();
        GameGUI.instance.CloseAllPanels();
    }

    public void OnClick_Close()
    {
        GameGUI.instance.CloseAllPanels();
    }
}
