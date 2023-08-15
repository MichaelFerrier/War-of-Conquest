using UnityEngine;
using UnityEngine.UI;
using System.Collections;

public class ActivationCodePanel : MonoBehaviour
{
    public static ActivationCodePanel instance;

    public InputField entry_inputfield;
    public Text message_text;

    public ActivationCodePanel()
    {
        instance = this;
    }

    public void Reset()
    {
        // Clear the username and message fields.
        entry_inputfield.text = "";
        message_text.text = "";
    }

    public void Init(string _message)
    {
        Reset();
        message_text.text = _message;
    }

    public void OnValueChanged()
    {
        // If there are two dashes in a row (which could happen if a code is pasted in, and this function auto inserts dashes), remove a dash.
        if ((entry_inputfield.text.Length >= 2) && (entry_inputfield.text.Substring(entry_inputfield.text.Length - 1, 1).Equals("-")) && (entry_inputfield.text.Substring(entry_inputfield.text.Length - 2, 1).Equals("-")))
        {
            entry_inputfield.text = entry_inputfield.text.Substring(0, entry_inputfield.text.Length - 1); 
            entry_inputfield.MoveTextEnd(false);
        }

        // Insert dashes automatically at the right positions, as typing.
        if (((entry_inputfield.text.Length == 3) || (entry_inputfield.text.Length == 7)) && (entry_inputfield.caretPosition == entry_inputfield.text.Length) && (Input.GetKeyDown(KeyCode.Backspace) == false))
         {
            entry_inputfield.text += "-";
            entry_inputfield.MoveTextEnd(false);
        }
    }

    public void OnEndEdit()
    {
        if ((entry_inputfield.text.Length >= 9) && (entry_inputfield.text.Length < 11))
        {
            if (entry_inputfield.text.Substring(3, 1) != "-") {
                entry_inputfield.text = entry_inputfield.text.Insert(3, "-");
            }
        }

        if ((entry_inputfield.text.Length >= 9) && (entry_inputfield.text.Length < 11))
        {
            if (entry_inputfield.text.Substring(7, 1) != "-") {
                entry_inputfield.text = entry_inputfield.text.Insert(7, "-");
            }
        }

        // If enter was pressed, handle as if submit button were pressed.
        if (Input.GetKeyDown(KeyCode.Return) || Input.GetKeyDown(KeyCode.KeypadEnter)) {
            OnClick_Activate();
        }
    }

    public void OnClick_Activate()
    {
        // Record the given activation code in PlayerPrefs
        PlayerPrefs.SetString("activation_code", entry_inputfield.text);

        // Clear fields
        Reset();

        // Close the panel
        GameGUI.instance.CloseAllPanels();

        // Attempt to validate this activation code
        Network.instance.AttemptFetchInfo();
    }

    public void OnClick_Exit()
    {
        GameGUI.instance.ExitGame();
    }
}
