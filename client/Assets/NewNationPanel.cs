using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using I2.Loc;

public class NewNationPanel : MonoBehaviour
{
    public static NewNationPanel instance;

    public InputField nationNameField;
    public Text nationNameMessage, messageText;

    public const int NUM_COLOR_OPTIONS = 15;
    public Toggle[] color_toggles = new Toggle[NUM_COLOR_OPTIONS];

    string nation_name;
    int nationColorR, nationColorG, nationColorB;

    public NewNationPanel()
    {
        instance = this;
    }

    public void Init()
    {
        // Set entry field character limit
        nationNameField.characterLimit = GameGUI.MAX_NATION_NAME_LENGTH;

        // Choose a random nation color
        int default_color_index = UnityEngine.Random.Range(0, NUM_COLOR_OPTIONS);
        for (int i = 0; i < NUM_COLOR_OPTIONS; i++) {
            color_toggles[i].isOn = (i == default_color_index);
        }

        // Reset fields
        nationNameField.text = "";
        ResetMessageFields();
    }

    public void ResetMessageFields()
    {
        nationNameMessage.text = "";
        messageText.text = LocalizationManager.GetTranslation("new_nation_intro");
    }

    public void OnClick_EnterGame()
    {
        ResetMessageFields();

        // Check all inputs

        // Nation name
        nation_name = nationNameField.text;

        // Remove any control characters from the string
        nation_name = GameGUI.RemoveControlCharacters(nation_name);

        if ((nation_name.Length < GameGUI.MIN_NATION_NAME_LENGTH) || (nation_name.Length > GameGUI.MAX_NATION_NAME_LENGTH))
        {
            // "Nation name must be between " + GameGUI.MIN_NATION_NAME_LENGTH + " and " + GameGUI.MAX_NATION_NAME_LENGTH + " characters long."
            // "Nation name must be between {[MIN_NATION_NAME_LENGTH]} and {[MAX_NATION_NAME_LENGTH]} characters long."
            nationNameMessage.text = LocalizationManager.GetTranslation("nation_name_length_validation_notice")
                .Replace("{[MIN_NATION_NAME_LENGTH]}", GameGUI.MIN_NATION_NAME_LENGTH.ToString())
                .Replace("{[MAX_NATION_NAME_LENGTH]}", GameGUI.MAX_NATION_NAME_LENGTH.ToString());

            return;
        }

        if (nation_name.IndexOf('|') != -1)
        {
            // "Names and passwords cannot contain "|"."
            nationNameMessage.text = LocalizationManager.GetTranslation("names_and_passwords_cannot_contain")
                .Replace("{character}", "|");
            return;
        }

        // Nation color
        for (int i = 0; i < NUM_COLOR_OPTIONS; i++)
        {
            if (color_toggles[i].isOn)
            {
                nationColorR = (int)(color_toggles[i].gameObject.transform.GetChild(0).gameObject.GetComponent<Image>().color.r * 255f + 0.5f);
                nationColorG = (int)(color_toggles[i].gameObject.transform.GetChild(0).gameObject.GetComponent<Image>().color.g * 255f + 0.5f);
                nationColorB = (int)(color_toggles[i].gameObject.transform.GetChild(0).gameObject.GetComponent<Image>().color.b * 255f + 0.5f);
                break;
            }
        }

        Debug.Log("About to customize nation with nation_name: " + nation_name + ", color: " + nationColorR + "," + nationColorG + "," + nationColorB);

        // Send new nation message to the game server, once the maps have finished loading.
        StartCoroutine(SubmitOnceMapsLoaded(nation_name, nationColorR, nationColorG, nationColorB));
    }

    public IEnumerator SubmitOnceMapsLoaded(string _nation_name, int _nationColorR, int _nationColorG, int _nationColorB)
    {
        messageText.text = LocalizationManager.GetTranslation("connect_logging_in");

        // TESTING
        if (Network.instance.initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false)) {
            Network.instance.LogEvent(Time.time + ": Client ID " + Network.instance.GetClientID(true) + " NewNationPanel SubmitOnceMapsLoaded().");
        }

        // Wait until the maps have been fully loaded, before sending customize_nation message, which may log the player in.
        while (Network.instance.maps_loaded == false) {
            Debug.Log("Waiting to send customize_nation message...");
            yield return new WaitForSeconds(0.2f);
        }

        // TESTING
        if (Network.instance.initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false)) {
            Network.instance.LogEvent(Time.time + ": Client ID " + Network.instance.GetClientID(true) + " NewNationPanel SubmitOnceMapsLoaded() about to send customize_nation event for ntion name " + _nation_name+ ".");
        }

        // Send login message to the game server.
        Network.instance.SendCommand("action=customize_nation|nation_name=" + _nation_name + "|color_r=" + _nationColorR + "|color_g=" + _nationColorG + "|color_b=" + _nationColorB);
    }

    public void CustomizeNationResult(bool _success, string _message)
    {
        if (_success) {
            GameGUI.instance.CloseAllPanels();
        } else {
            messageText.text = _message;
        }
    }
}
