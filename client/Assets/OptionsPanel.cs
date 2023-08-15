using UnityEngine;
using UnityEngine.UI;
using System.Collections.Generic;
using I2.Loc;

public class OptionsPanel : MonoBehaviour
{
    public static OptionsPanel instance;

    public GameObject createPasswordButton, logOutButton, loginReportButton, unmuteAllButton, resolutionArea;
    public Dropdown languageDropdown, graphicsQualityDropdown, resolutionDropdown;
    public Toggle blockWhispersToggle, disableChatFilterToggle, disableFlashEffectsToggle, fullscreenToggle, mapLocationToggle, gridToggle, soundEffectsToggle, musicToggle, hideTutorialToggle/*, everyplayToggle, showFaceToggle*/;
    private bool processing_info_event = false, populating_language_dropdown = false;
    
    private List<Resolution> resolution_options = new List<Resolution>();

    public OptionsPanel()
    {
        instance = this;
    }

    public void Start()
    {
    }
    
    public void InfoEventReceived()
    {
        // Populate languages menu
        GameGUI.instance.PopulateLanguageDropdown(languageDropdown);

        // Populate graphics quality dropdown
        List<string> graphics_quality_list = new List<string>();
        graphics_quality_list.Add(I2.Loc.LocalizationManager.GetTranslation("Options Screen/quality_fastest"));
        graphics_quality_list.Add(I2.Loc.LocalizationManager.GetTranslation("Options Screen/quality_fast"));
        graphics_quality_list.Add(I2.Loc.LocalizationManager.GetTranslation("Options Screen/quality_good"));
        graphics_quality_list.Add(I2.Loc.LocalizationManager.GetTranslation("Options Screen/quality_great"));
        graphics_quality_list.Add(I2.Loc.LocalizationManager.GetTranslation("Options Screen/quality_best"));
        graphicsQualityDropdown.ClearOptions();
        graphicsQualityDropdown.AddOptions(graphics_quality_list);
        graphicsQualityDropdown.value = (int)GameGUI.instance.graphicsQuality;

#if UNITY_ANDROID || UNITY_IOS
        // Don't show resoution dropdown on mobile devices
        resolutionArea.SetActive(false);
#else
        resolutionArea.SetActive(true);

        // Populate resolution dropdown
        List<string> resolution_list = new List<string>();
        Resolution[] resolutions = Screen.resolutions;
        int prev_w = -1, prev_h = -1, sel_index = -1;
        foreach (Resolution res in resolutions) 
        {
            // Skip duplicate resolutions
            if ((res.width == prev_w) && (res.height == prev_h)) continue;

            // Skip resolutions that are too low
            if ((res.width < 1024) || (res.height < 768)) continue;

            resolution_list.Add(res.width + " x " + res.height);
            prev_w = res.width;
            prev_h = res.height;

            // Record the resolution index to select
            if ((res.width == Screen.width) && (res.height == Screen.height)) {
                sel_index = resolution_options.Count;
            }

            // Record this resolution option
            resolution_options.Add(res);
        }
        resolutionDropdown.ClearOptions();
        resolutionDropdown.AddOptions(resolution_list);
        resolutionDropdown.value = sel_index;
        Debug.Log("Screen.width: " + Screen.width + ", Screen.height: " + Screen.height + ", sel_index: " + sel_index);
#endif

        Layout();

        // Record that we're processing the info event, so the below changes will not result in messages to the server.
        processing_info_event = true;

        // Set default toggle states
        mapLocationToggle.isOn = false;
        gridToggle.isOn = false;
        soundEffectsToggle.isOn = true;
        musicToggle.isOn = true;
        hideTutorialToggle.isOn = false;
        //everyplayToggle.isOn = false;
        //showFaceToggle.isOn = false;

        // Show or hide appropriate setting toggles, depending on the platform.
#if UNITY_EDITOR || UNITY_ANDROID || UNITY_IOS
        //everyplayToggle.gameObject.SetActive(false); // Everyplay has been discontinued.
        //showFaceToggle.gameObject.SetActive(false); // Everyplay has been discontinued.
        fullscreenToggle.gameObject.SetActive(false);
#else
        //everyplayToggle.gameObject.SetActive(false);
        //showFaceToggle.gameObject.SetActive(false);
        fullscreenToggle.gameObject.SetActive(true);
#endif

        // Set user flag toggles
        blockWhispersToggle.isOn = GameData.instance.GetUserFlag(GameData.UserFlags.BLOCK_WHISPERS);
        disableChatFilterToggle.isOn = GameData.instance.GetUserFlag(GameData.UserFlags.DISABLE_CHAT_FILTER);
        disableFlashEffectsToggle.isOn = GameData.instance.GetUserFlag(GameData.UserFlags.DISABLE_FLASH_EFFECTS);
        fullscreenToggle.isOn = GameData.instance.GetUserFlag(GameData.UserFlags.FULLSCREEN);
        mapLocationToggle.isOn = GameData.instance.GetUserFlag(GameData.UserFlags.SHOW_MAP_LOCATION);
        gridToggle.isOn = GameData.instance.GetUserFlag(GameData.UserFlags.SHOW_GRID);
        soundEffectsToggle.isOn = GameData.instance.GetUserFlag(GameData.UserFlags.SOUND_EFFECTS);
        musicToggle.isOn = GameData.instance.GetUserFlag(GameData.UserFlags.MUSIC);
        hideTutorialToggle.isOn = GameData.instance.GetUserFlag(GameData.UserFlags.HIDE_TUTORIAL);
        //everyplayToggle.isOn = GameData.instance.GetUserFlag(GameData.UserFlags.EVERYPLAY);
        //showFaceToggle.isOn = GameData.instance.GetUserFlag(GameData.UserFlags.SHOW_FACE);

        // Only display the login report button if a report is available.
        loginReportButton.SetActive(ReportPanel.instance.LoginReportAvailable());

        // Only display the unmute all button if any users or devices are muted.
        unmuteAllButton.SetActive((Chat.instance.muted_users.Count > 0) || (Chat.instance.muted_devices.Count > 0));

        // Done processing info event.
        processing_info_event = false;
    }

    public void OnMute()
    {
        // Show the unmute button
        unmuteAllButton.SetActive(true);
    }

    public void AccountInfoEventReceived()
    {
        Layout();
    }

    public void Layout()
    {
        // Display appropriate buttons depending on whether the user is registered.
        if (GameData.instance.userIsRegistered)
        {
            createPasswordButton.SetActive(false);
            logOutButton.SetActive(true);
        }
        else
        {
            createPasswordButton.SetActive(true);
            logOutButton.SetActive(false);
        }
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

    public void OnClick_CreatePassword()
    {
        // Open the create password panel.
        GameGUI.instance.OpenCreatePasswordPanel(CreatePasswordPanel.Context.OptionsPanel);
    }

    public void OnClick_UnmuteAll()
    {
        // Unmute all players
        Chat.instance.UnmuteAll();

        // Hide the unmute button
        unmuteAllButton.SetActive(false);
    }

    public void OnClick_Close()
    {
        // Close this panel.
        GameGUI.instance.SetActiveGamePanel(GameGUI.GamePanel.GAME_PANEL_NONE);
    }

    public void OnChange_BlockWhispers()
    {
        if (!processing_info_event)
        {
            // Record the flag value, and send flags to the server.
            GameData.instance.SetUserFlag(GameData.UserFlags.BLOCK_WHISPERS, blockWhispersToggle.isOn);
            GameData.instance.SendUserFlags();
        }
    }

    public void OnChange_DisableChatFilter()
    {
        if (!processing_info_event)
        {
            // Record the flag value, and send flags to the server.
            GameData.instance.SetUserFlag(GameData.UserFlags.DISABLE_CHAT_FILTER, disableChatFilterToggle.isOn);
            GameData.instance.SendUserFlags();
        }
    }

    public void OnChange_DisableFlashEffects()
    {
        if (!processing_info_event)
        {
            // Record the flag value, and send flags to the server.
            GameData.instance.SetUserFlag(GameData.UserFlags.DISABLE_FLASH_EFFECTS, disableFlashEffectsToggle.isOn);
            GameData.instance.SendUserFlags();
        }
    }

    public void OnChange_Fullscreen()
    {
        if (!processing_info_event)
        {
            // Record the flag value, and send flags to the server.
            GameData.instance.SetUserFlag(GameData.UserFlags.FULLSCREEN, fullscreenToggle.isOn);
            GameData.instance.SendUserFlags();
        }

        // Make the game fullscreen or windowed, as appropriate.
        GameGUI.instance.SetFullScreen(fullscreenToggle.isOn);
    }

    public void OnChange_SoundEffects()
    {
        if (!processing_info_event)
        {
            // Record the flag value, and send flags to the server.
            GameData.instance.SetUserFlag(GameData.UserFlags.SOUND_EFFECTS, soundEffectsToggle.isOn);
            GameData.instance.SendUserFlags();
        }

        // Set sound volume as appropriate.
        Sound.instance.SetSoundVolume(soundEffectsToggle.isOn ? 0 : -80);
    }

    public void OnChange_Music()
    {
        if (!processing_info_event)
        {
            // Record the flag value, and send flags to the server.
            GameData.instance.SetUserFlag(GameData.UserFlags.MUSIC, musicToggle.isOn);
            GameData.instance.SendUserFlags();
        }
    }

    public void OnChange_HideTutorial()
    {
        if (!processing_info_event)
        {
            // Record the flag value, and send flags to the server.
            GameData.instance.SetUserFlag(GameData.UserFlags.HIDE_TUTORIAL, hideTutorialToggle.isOn);
            GameData.instance.SendUserFlags();
        }

        // Turn tutorial system on or off as appropriate.
        Tutorial.instance.SetMuted(hideTutorialToggle.isOn);
    }

    public void OnChange_ShowMapLocation()
    {
        if (!processing_info_event)
        {
            // Record the flag value, and send flags to the server.
            GameData.instance.SetUserFlag(GameData.UserFlags.SHOW_MAP_LOCATION, mapLocationToggle.isOn);
            if (!processing_info_event) GameData.instance.SendUserFlags();
        }

        // Tell the GUI whether to show the map location.
        GameGUI.instance.SetShowMapLocationState(mapLocationToggle.isOn);
    }

    public void OnChange_ShowGrid()
    {
        if (!processing_info_event)
        {
            // Record the flag value, and send flags to the server.
            GameData.instance.SetUserFlag(GameData.UserFlags.SHOW_GRID, gridToggle.isOn);
            if (!processing_info_event) GameData.instance.SendUserFlags();
        }

        // Update the map with new grid setting.
        MapView.instance.SetShowGrid(gridToggle.isOn);
    }
    /*
    public void OnChange_Everyplay()
    {
        if (!processing_info_event)
        {
            // Record the flag value, and send flags to the server.
            GameData.instance.SetUserFlag(GameData.UserFlags.EVERYPLAY, everyplayToggle.isOn);
            GameData.instance.SendUserFlags();
        }

        // Reset video recording, with this new setting.
        GameGUI.instance.ResetVideoRecording();
    }

    public void OnChange_ShowFace()
    {
        if (!processing_info_event)
        {
            // Record the flag value, and send flags to the server.
            GameData.instance.SetUserFlag(GameData.UserFlags.SHOW_FACE, showFaceToggle.isOn);
            GameData.instance.SendUserFlags();
        }

        // Reset video recording, with this new setting.
        GameGUI.instance.ResetVideoRecording();
    }
    */
    public void OnChange_GraphicsQuality()
    {
        // If we're within the game itself (rather than initializing), record the given value as having been chosen by the user.
        if ((Time.unscaledTime > 10f) && !MapView.instance.IsCameraPaused() && GameGUI.instance.IsInGame() && (GameGUI.instance.IsUpdatingUIElement() == false)) 
        {
            Debug.Log("Storing new player chosen graphics quality setting: " + graphicsQualityDropdown.value);
            PlayerPrefs.SetInt("graphics_quality", graphicsQualityDropdown.value);
            GameGUI.instance.userChoseGraphicsQuality = true;
        }

        // Set the new graphics quality level
        GameGUI.instance.SetGraphicsQuality((GameGUI.GraphicsQuality)(graphicsQualityDropdown.value), false);        
    }

    public void OnChange_Resolution()
    {
#if UNITY_EDITOR || UNITY_STANDALONE || UNITY_WEBPLAYER || UNITY_WEBGL // Don't call SetResolution() on mobile platforms, it can cause incorrect resolution upon orientation changes.
        Resolution new_res = resolution_options[resolutionDropdown.value];

        // Set the new resolution
        Screen.SetResolution(new_res.width, new_res.height, fullscreenToggle.isOn);
#endif
    }
    
    public void OnChange_Language()
    {
        // Change the GUI language
        GameGUI.instance.OnChange_Language(languageDropdown);
    }
}
