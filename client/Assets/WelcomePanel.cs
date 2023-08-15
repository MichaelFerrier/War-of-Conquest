using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class WelcomePanel : MonoBehaviour
{
    public Dropdown languageDropdown;
    public GameObject closeButton;
    private bool populating_language_dropdown = false;

    public static WelcomePanel instance;

    public WelcomePanel()
    {
        instance = this;
    }

    public void Init()
    {
        // Populate languages menu
        GameGUI.instance.PopulateLanguageDropdown(languageDropdown);

        // Do not show the close button on mobile platforms
#if UNITY_ANDROID || UNITY_IOS
        closeButton.SetActive(false);
#else
        closeButton.SetActive(true);
#endif
    }

    public void PopulateLanguageDropdown()
    {
        if (populating_language_dropdown) {
            return;
        }

        populating_language_dropdown = true;
        GameGUI.instance.PopulateLanguageDropdown(languageDropdown);
        populating_language_dropdown = false;
    }

    public void OnChange_Language()
    {
        // Change the GUI language
        GameGUI.instance.OnChange_Language(languageDropdown);
    }
}
