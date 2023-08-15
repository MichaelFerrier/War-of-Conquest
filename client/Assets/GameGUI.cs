using UnityEngine;
using UnityEngine.UI;
using UnityEngine.Advertisements;
using System;
using System.Collections;
using System.Collections.Generic;
using System.Text;
using System.Xml;
using I2.Loc;
#if UNITY_IOS || UNITY_ANDROID
using EasyMobile;
#endif

public class GameGUI : MonoBehaviour, RequestorListener, IUnityAdsListener
{
    public enum GamePanel
    {
        GAME_PANEL_NONE,
        GAME_PANEL_NATION,
        GAME_PANEL_QUESTS,
        GAME_PANEL_ADVANCES,
        GAME_PANEL_MESSAGES,
        GAME_PANEL_CONNECT,
        GAME_PANEL_OPTIONS,
        GAME_PANEL_MAP,
        GAME_PANEL_TOURNAMENT,
        GAME_PANEL_RAID,
        GAME_PANEL_ADMIN,
        GAME_PANEL_MODERATOR
    };

    public enum RequestorTasks
    {
        UPDATE_CLIENT        = 0,
        UPDATE_CLIENT_STEAM  = 1,
        BUY_CREDITS          = 2,
        RATING_ENJOYING      = 3,
        RATING_FEEDBACK      = 4,
        RATING_RATE          = 5
    };

    public enum CameraType
    {
        SCREENSHOT,
        VIDEO
    };

    public enum Languages
    {
        GERMAN = 0,
        ENGLISH = 1,
        SPANISH = 2,
        FRENCH = 3,
        ITALIAN = 4,
        PORTUGESE = 5,
        RUSSIAN = 6,
        CHINESE = 7,
        JAPANESE = 8,
        KOREAN = 9
    };

    public enum GraphicsQuality
    {
        FASTEST = 0,
        FAST = 1,
        GOOD = 2,
        GREAT = 3,
        BEST = 4
    };

    const int TARGET_FRAMERATE_DESKTOP = 60;
    const int TARGET_FRAMERATE_MOBILE = 30;
    const int TARGET_FRAMERATE_OUT_OF_GAME = 15;
    const int TARGET_FRAMERATE_DESKTOP_OUT_OF_FOCUS = 15;
    const int TARGET_FRAMERATE_MOBILE_OUT_OF_FOCUS = 4;

    const float KEEP_ALIVE_PERIOD = 300f; // 5 minutes between sending pings

    public static string[] languageNames = { "Deutsch", "English", "Español", "Français", "Italiano", "Português do Brasil", "Ру́сский язы́к", "中文", "日本語", "한국어" }; // "Chinese (Simplified)","English","French","German","Italian","Japanese","Korean","Portugese (Brazil)","Russian","Spanish"
    public static string[] languageEnglishNames = { "German", "English", "Spanish", "French", "Italian", "Portugese (Brazil)", "Russian", "Chinese (Simplified)", "Japanese", "Korean" };

    public static GameGUI instance;

    public bool smallScreenDevice = false;
    private bool updatingUIElement = false;
    private bool adFinished = false;
    public float prevUserInputTime = 0f;
    public bool hasFocus = true;

    public List<int> language_list = new List<int>();
    public List<string> language_name_list = new List<string>();
    public List<string> language_english_name_list = new List<string>();
    public Languages curLanguage;

    public List<int> availableServerIDs = new List<int>();

    public GraphicsQuality graphicsQuality;
    public bool userChoseGraphicsQuality = false;

    public GameObject panelBase, activationCodePanel, captchaPanel, mapFlagPanel, chooseNamePanel, logInPanel, joinNationPanel, inviteUnitePanel, postMessagePanel, statDetailsPanel, advanceDetailsPanel, objectInfoPanel, nationInfoPanel, buyPanel, subscribePanel, announcementPanel, reportPanel, welcomePanel, newNationPanel, newPlayerPanel, createPasswordPanel, customizePanel, contestRulesPanel, raidEndPanel, sharePanel, cashOutPanel, suspendScreen;
    public CreditsScreen creditsScreen;
    public Chat chatSystem;

    public GameObject mainUILeftObject, mainUIBottomObject;
    public GameObject gamePanelBaseObject, nationPanelObject, questsPanelObject, advancesPanelObject, messagesPanelObject, connectPanelObject, optionsPanelObject, mapPanelObject, tournamentPanelObject, raidPanelObject, adminPanelObject, moderatorPanelObject;
    public GameObject cameraMenu, cameraButton;
    public Toggle cameraMenuScreenshotToggle, cameraMenuVideoToggle;
    public GameObject mapLocationTextObject, progressDisplay;
    public GameObject energyDecreaseIndicator, energyInertIndicator, manpowerBurnIndicator, lowGeoIndicator;
    public Sprite indicatorYellowSprite, indicatorRedSprite;
    public RectTransform mainUILeftRectTransform, mainUIBottomRectTransform;
    public RectTransform gamePanelBaseRectTransform, alertBaseRectTransform, tournamentButtonRectTransform, adBonusButtonRectTransform, switchMapButtonRectTransform, raidButtonRectTransform, replayControlsRectTransform;
    public RectTransform canvasRectTransform;
    public RectTransform statusBarsLeftRectTransform, statusBarsRightRectTransform, apBarRectTransform, compassRoseRectTransform, medalsTextRectTransform, mapLocationTextRectTransform, tempsListRectTransform, messageTextRectTransform;
    public PanelButton nationButtonLeft, questsButtonLeft, raidLogButtonLeft, advancesButtonLeft, messagesButtonLeft, connectButtonLeft, optionsButtonLeft;
    public PanelButton nationButtonBottom, questsButtonBottom, raidLogButtonBottom, advancesButtonBottom, messagesButtonBottom, connectButtonBottom, optionsButtonBottom;
    public StatusBar xpBar, apBar, geoBar, manpowerBar, energyBar, creditsBar;
    public Image energyReserveBar;
    public AnimTextSource levelBulb;
    public Text levelText;
    public TMPro.TextMeshProUGUI mapLocationText, medalsText, suspendScreenText;
    public Button suspendScreenButton, suspendScreenCloseButton;
    public Image suspendScreenImage, suspendScreenLogo;
    public GameObject suspendScreenProgressBar;
    public Image suspendScreenProgressBarFill;
    public MessageText messageText;
    public AdBonusButton adBonusButton;
    public MapView mapView;
    public GameObject flashObject;
    public Image flashImage;
    public TempsList tempsList;
    public Light light;
    public Image leftNationButtonColor, bottomNationButtonColor;

    public Image cameraButtonImage;
    public Sprite cameraButtonScreenshotIcon, cameraButtonVideoIcon;
    CameraType cameraType = CameraType.SCREENSHOT;
    public float cameraButtonPressTime = -1;
    public bool cameraButtonPressed = false;
#if UNITY_IOS || UNITY_ANDROID
  public Recorder gifRecorder;
#endif
  public bool everyplayReady = false;
    const float CAMERA_MENU_OPEN_TIME = 0.75f;
    const float CAMERA_MENU_CLOSE_TIME = 6f;

    float compassRosePressTime = -1;
    const float COMPASS_ROSE_PRESS_TIME = 0.75f;

	//Text left_NameText, bottom_NameText;
	int screenWidth = -1, screenHeight = -1, initialScreenWidth = -1, initialScreenHeight = -1;
    float mainUILeftWidth = 0, mainUIBottomHeight = 0;

    // Link information
    public LinkManager linkManager = new LinkManager();

    bool suspended_due_to_inactivity = false;
    float prev_attempt_connect_time = 0f;
    const float ATTEMPT_CONNECT_MIN_INTERVAL = 15f;

    bool progress_displayed = false;
    int progress_max_num_tasks = 0, progress_cur_num_tasks = 0;

    float prevSaveTutorialTime = -1;
    const float TUTORIAL_SAVE_PERIOD = 5; // Made shorter so I can get better data about how far players make it in the tutorial.

    float prevUpdateTempIconsTime = -1;
    const float UPDATE_TEMP_ICONS_PERIOD = 5f;

    float prevCaptchaTime = 0f;
    const float MIN_CAPTCHA_INTERVAL = 3600f; // One hour

    float prevUpdateEventTime = 0f;

    public const float MAP_PANEL_TRANSITION_DURATION = 1f;

    public bool cur_update_inert = false,  prev_update_inert = false;

    public GamePanel active_game_panel = GamePanel.GAME_PANEL_NONE;

    public const int MIN_USERNAME_LENGTH = 1;
    public const int MAX_USERNAME_LENGTH = 20;

    public const int MIN_NATION_NAME_LENGTH = 1;
    public const int MAX_NATION_NAME_LENGTH = 20;

    public const int MIN_PASSWORD_LENGTH = 8;
    public const int MAX_PASSWORD_LENGTH = 20;

    public const int MAX_MAP_FLAG_DESC_LENGTH = 50;

    public List<string> securityQuestions;

	public String[] nationColors = new String[] {
		"Red",
		"Orange",
		"Yellow",
		"Green",
		"Blue",
		"Violet",
		"White",
		"Black",
		"Brown",
		"Pink",
		"Cyan",
		"Olive"
	};

	public Color[] nationColorValues = new Color[] {
		new Color(1.0f,0.0f,0.0f),
		new Color(1.0f,0.5f,0.0f),
		new Color(1.0f,1.0f,0.0f),
		new Color(0.0f,1.0f,0.0f),
		new Color(0.0f,0.0f,1.0f),
		new Color(1.0f,0.0f,1.0f),
		new Color(1.0f,1.0f,1.0f),
		new Color(0.1f,0.1f,0.1f),
		new Color(0.3f,0.25f,0.0f),
		new Color(1.0f,0.5f,0.5f),
		new Color(0.0f,1.0f,1.0f),
		new Color(0.2f,0.5f,0.1f)
	};

    // Purchasing credits
    public string[] buyCreditsPackageName;

    // Purchasing subscriptions
    public string[] subscriptionTierName;

	void Awake()
    {
		instance = this;

        language_list.Clear();
        language_name_list.Clear();
        language_english_name_list.Clear();

        language_list.Add((int)Languages.GERMAN); language_name_list.Add(languageNames[(int)Languages.GERMAN]); language_english_name_list.Add(languageEnglishNames[(int)Languages.GERMAN]);
        language_list.Add((int)Languages.ENGLISH); language_name_list.Add(languageNames[(int)Languages.ENGLISH]); language_english_name_list.Add(languageEnglishNames[(int)Languages.ENGLISH]);
        language_list.Add((int)Languages.SPANISH); language_name_list.Add(languageNames[(int)Languages.SPANISH]); language_english_name_list.Add(languageEnglishNames[(int)Languages.SPANISH]);
        language_list.Add((int)Languages.FRENCH); language_name_list.Add(languageNames[(int)Languages.FRENCH]); language_english_name_list.Add(languageEnglishNames[(int)Languages.FRENCH]);
        language_list.Add((int)Languages.ITALIAN); language_name_list.Add(languageNames[(int)Languages.ITALIAN]); language_english_name_list.Add(languageEnglishNames[(int)Languages.ITALIAN]);
        language_list.Add((int)Languages.PORTUGESE); language_name_list.Add(languageNames[(int)Languages.PORTUGESE]); language_english_name_list.Add(languageEnglishNames[(int)Languages.PORTUGESE]);
        language_list.Add((int)Languages.RUSSIAN); language_name_list.Add(languageNames[(int)Languages.RUSSIAN]); language_english_name_list.Add(languageEnglishNames[(int)Languages.RUSSIAN]);
        language_list.Add((int)Languages.CHINESE); language_name_list.Add(languageNames[(int)Languages.CHINESE]); language_english_name_list.Add(languageEnglishNames[(int)Languages.CHINESE]);
        language_list.Add((int)Languages.JAPANESE); language_name_list.Add(languageNames[(int)Languages.JAPANESE]); language_english_name_list.Add(languageEnglishNames[(int)Languages.JAPANESE]);
        language_list.Add((int)Languages.KOREAN); language_name_list.Add(languageNames[(int)Languages.KOREAN]); language_english_name_list.Add(languageEnglishNames[(int)Languages.KOREAN]);
    }

    // Use this for initialization
    void Start ()
    {
        // Determine whether this is a small screen device (if so, UI panels will be scaled up).
        if (Screen.dpi != 0)
        {
            float width = Screen.width / Screen.dpi;
            float height = Screen.height / Screen.dpi;
            float diagonal = Mathf.Sqrt((width * width) + (height * height));
            smallScreenDevice = (diagonal <= 6.5f);
        }

        // Set scale of status bar area, depending on whether this is a small screen device.
        statusBarsLeftRectTransform.localScale = new Vector3(smallScreenDevice ? 1.3f : 1f, smallScreenDevice ? 1.3f : 1f, 1f);
        statusBarsRightRectTransform.localScale = new Vector3(smallScreenDevice ? 1.3f : 1f, smallScreenDevice ? 1.3f : 1f, 1f);

        // Determine the initial graphics quality level
        InitGraphicsQuality();

        // GB-Localization, Security Questions
        // These need to be set here; LocalizationManager.GetTranslation() can't be called from GameGUI's constructor.
        securityQuestions = new List<string> {
		    LocalizationManager.GetTranslation("security_q_monthers_maiden_name"), // "Your mother's maiden name?"
		    LocalizationManager.GetTranslation("security_q_first_pet_name"), // "The name of your first pet?"
		    LocalizationManager.GetTranslation("security_q_favorite_teacher_last_name"), // "Your favorite teacher's last name?"
		    LocalizationManager.GetTranslation("security_q_street_grew_up_on"), // "Name of the street where you grew up?"
		    LocalizationManager.GetTranslation("security_q_town_where_you_were_born"), // "Name of the town where you were born?"
		    LocalizationManager.GetTranslation("security_q_first_tv_show"), // "First TV show you remember?"
		    LocalizationManager.GetTranslation("security_q_first_movie"), // "First movie you remember?"
		    LocalizationManager.GetTranslation("security_q_oldest_sibling_middle_name"), // "Oldest sibling's middle name?"
		    LocalizationManager.GetTranslation("security_q_youngest_sibling_middle_name"), // "Youngest sibling's middle name?"
		    LocalizationManager.GetTranslation("security_q_first_crush"), // "Name of your first crush?"
		    LocalizationManager.GetTranslation("security_q_favorite_song"), // "Your favorite song?"
		    LocalizationManager.GetTranslation("security_q_favorite_writer") // "Your favorite writer?"
	    };

		// Record initial screen dimensions
		initialScreenWidth = Screen.width;
		initialScreenHeight = Screen.height;

        // Initially force landscape orientation.
        ForceLandscape(true);

		//left_NameText = ((Text)GameObject.Find ("Main UI, Left/name text").GetComponent ("Text"));
		//bottom_NameText = ((Text)GameObject.Find ("Main UI, Bottom/name text").GetComponent ("Text"));

		// Initialize active states of UI elements.

        DeactivateAllPanels();

        // Start with no game panel active.
        DeactivateGamePanel();

        // Start with the suspend screen open, until the game has been entered, or welcome screen is displayed.
        // "Connecting to server..."
        OpenSuspendScreen(false, LocalizationManager.GetTranslation("connecting_to_server"), false);

        // Initialize the chat system
        chatSystem.SetDisplayMode(Chat.ChatDisplayMode.MEDIUM);

        // Initialize Unity Ads
#if UNITY_ANDROID
        string gameId = "insert game id";
        bool testMode = false; // Test mode is overridden through the Unity Dashboard
        Advertisement.AddListener(this);
        Advertisement.Initialize(gameId, testMode);
#elif UNITY_IOS
        string gameId = "insert game id";
        bool testMode = false; // Test mode is overridden through the Unity Dashboard
        Advertisement.AddListener(this);
        Advertisement.Initialize(gameId, testMode);
#endif

        //// Register for the Everyplay ReadyForRecording event
        //Everyplay.ReadyForRecording += OnEveryplayReadyForRecording;
        //Everyplay.WasClosed += OnEverplayWasClosed;

        //// Set Everyplay max recording time
        //Everyplay.SetMaxRecordingMinutesLength(30);
    }

	// Update is called once per frame
	void Update ()
    {
		if ((Screen.width != screenWidth) || (Screen.height != screenHeight)) {
			screenWidth = Screen.width;
			screenHeight = Screen.height;

			ResolutionChanged();
		}

        // If the game is suspended (not due to inactivity) and no attempt has been made to connect in at least 5 minutes, attempt to connect.
        if (suspendScreen.activeSelf && (panelBase.activeSelf == false) && (suspended_due_to_inactivity == false) && ((Time.unscaledTime - prev_attempt_connect_time) >= 300f))
        {
            // Must fetch info, even if it's already been fetched, in case a new client or new data is required.
            AttemptFetchInfo();
        }

        // Every so often, display captcha panel to make sure the player is not a bot.
        if (IsInGame() && ((Time.unscaledTime - Mathf.Max(prevCaptchaTime, GameData.instance.timeAtLogin)) > MIN_CAPTCHA_INTERVAL)) {
            OpenCaptchaPanel();
        }

        // Keep track of when the latest user input was received (except if the captcha panel is open).
        if (((Input.touchCount > 0) || Input.anyKey) && (!captchaPanel.activeInHierarchy)) {
            prevUserInputTime = Time.unscaledTime;
        }

        // If a command hasn't been sent to the server in a while, but the user has provided input recently, send a keep alive ping message.
        if (((Time.unscaledTime - Network.instance.prevCommandSentTime) > KEEP_ALIVE_PERIOD) && ((Time.unscaledTime - prevUserInputTime) < 120f))
        {
            Network.instance.SendCommand("ping");
        }

        if ((Input.GetKey(KeyCode.LeftControl) || Input.GetKey(KeyCode.RightControl)) && (Input.GetKey(KeyCode.LeftShift) || Input.GetKey(KeyCode.RightShift)))
        {
            if (Input.GetKeyDown(KeyCode.L))
            {
                // Bug Logging
                Debug.Log("Logging debug info. NationID: " + GameData.instance.nationID + ", name: " + GameData.instance.nationName);
                for (int z = MapView.instance.GetViewBlockZ() - 3; z <= MapView.instance.GetViewBlockZ() + 3; z++)
                {
                    for (int x = MapView.instance.GetViewBlockX() - 3; x <= MapView.instance.GetViewBlockX() + 3; x++)
                    {
                        BlockData block_data = MapView.instance.GetBlockData(x,z);
                        if (block_data == null) continue;
                        Debug.Log(x + "," + z + ": terrain: " + MapView.instance.GetBlockTerrain(x, z) + ", nationID: " + block_data.nationID + ", owner_NationID: " + block_data.owner_nationID + ", objectID: " + block_data.objectID + ", in_view_area: " + block_data.in_view_area + ", surround_count: " + block_data.surround_count);
                        if (block_data.surround_count != null) Debug.Log("    Surround count: active: " + block_data.surround_count.activeSelf + ", text: " + block_data.surround_count.transform.GetChild(0).gameObject.GetComponent<TextMesh>().text + ", color: " + block_data.surround_count.transform.GetChild(0).gameObject.GetComponent<TextMesh>().color);
                    }
                }
                LogToChat("Debug info written to log!");
            }

            if (Input.GetKeyDown(KeyCode.M))
            {
                // Log memory usage stats
                GameData.instance.LogMemoryStats();

                LogToChat("Memory stats written to log!");
            }

            if (Input.GetKeyDown(KeyCode.R))
            {
                PlayerPrefs.DeleteKey("activation_code");
                PlayerPrefs.DeleteKey("serverID");

                // GB-Localization
                DisplayMessage(I2.Loc.LocalizationManager.GetTranslation("activation_code_cleared"));
            }

            if (Input.GetKeyDown(KeyCode.H))
            {
                Network.instance.SendCommand("action=switch_map");
            }

            if (Input.GetKeyDown(KeyCode.X))
            {
                OnClick_RaidLog();
                /*
                // TESTING
                GameData.instance.raidFlags = 7; // 7->127
                GameData.instance.raidAttackerNationNumMedals = 1000;
                GameData.instance.raidDefenderNationNumMedals = 1000;
                GameData.instance.raidPercentageDefeated = 100;
                GameData.instance.raidRewardMedals = 28;
                GameData.instance.raidRewardCredits = 12;
                GameData.instance.raidRewardXP = 2400;
                OpenRaidEndPanel();
                */
                //Network.instance.SendCommand("action=raid");
            }
        }

        if (GameData.instance.userIsAdmin)
        {
            if (Input.GetKeyDown(KeyCode.T) && (Input.GetKey(KeyCode.LeftControl) || Input.GetKey(KeyCode.RightControl)))
            {
                // Toggle wide formatting of the advances panel for taking screenshots.
                if (AdvancesPanel.instance != null) {
                    AdvancesPanel.instance.SetScreenshotFormat(!AdvancesPanel.instance.screenshotFormat);
                }
            }

            if (Input.GetKeyDown(KeyCode.D) && (Input.GetKey(KeyCode.LeftControl) || Input.GetKey(KeyCode.RightControl)))
            {
                // Disconnect from server.
                Network.instance.DisconnectAbruptly();
            }
        }

        if (IsInGame())
        {
            /*
            // Testing
            if (Input.GetKey(KeyCode.Delete))
            {
                // Testing
                if (mapView.IsCameraPaused()) {
                    mapView.ResumeCamera();
                } else {
                    mapView.PauseCamera(false);
                }
            }
            */

            // Save the state of the tutorial if appropriate.
            if ((Time.unscaledTime - prevSaveTutorialTime) >= TUTORIAL_SAVE_PERIOD)
            {
                Tutorial.instance.SaveState();
                prevSaveTutorialTime = Time.unscaledTime;
            }

            if ((Time.unscaledTime - prevUpdateTempIconsTime) >= UPDATE_TEMP_ICONS_PERIOD)
            {
                tempsList.UpdateFillLevels();
                prevUpdateTempIconsTime = Time.unscaledTime;
            }
        }

        // If ad watch has finished successfully, handle rewarding user. Do this here rather than from listener function, because doing it from the thread that calls the listener function causes problems on Android.
        if (adFinished)
        {
            // Reset adFinished to false.
            adFinished = false;

            // Reward the user for watching the ad to completion.
            Network.instance.SendCommand("action=ad_watched");

			// Reset the ad bonus button's amount to 0.
            GameGUI.instance.adBonusButton.ModifyAmount(0, 0, AdBonusButton.AdBonusType.NONE, -1, -1);
        }
	}

    void OnApplicationFocus(bool _hasFocus)
    {
        hasFocus = _hasFocus;
        UpdateTargetFrameRate();
    }

    void OnApplicationPause(bool _pauseStatus)
    {
        hasFocus = !_pauseStatus;
        UpdateTargetFrameRate();
    }
    public void DataLoaded()
    {
        // Perform one-time-only initializations that require that the data has first been loaded.
        AdvancesPanel.instance.Initialize();
    }

    public void NationColorChanged()
    {
        leftNationButtonColor.color = GameData.instance.nationColor;
        bottomNationButtonColor.color = GameData.instance.nationColor;
    }

	void ResolutionChanged()
    {
        mainUILeftWidth = 0;
        mainUIBottomHeight = 0;

		// Activate the left or bottom main UI, as appropriate, and record size of main UI, to determine map view offset.
		if (screenWidth >= screenHeight) {
			mainUILeftObject.SetActive(true);
			mainUIBottomObject.SetActive(false);
		} else {
			mainUILeftObject.SetActive(false);
			mainUIBottomObject.SetActive(true);
		}

        mainUILeftWidth = mainUILeftObject.activeSelf ? mainUILeftRectTransform.rect.width : 0;
        mainUIBottomHeight = mainUIBottomObject.activeSelf ? mainUIBottomRectTransform.rect.height : 0;

        // Have the chat UI resize and reposition itself.
        chatSystem.Layout();

        // Have the MapView resize and reposition the 3D viewport.
        mapView.DeterminePosition();

        mapPanelObject.GetComponent<MapPanel>().ResolutionChanged();
        advancesPanelObject.GetComponent<AdvancesPanel>().ResolutionChanged();
        subscribePanel.GetComponent<SubscribePanel>().ResolutionChanged();

        // Layout other GUI components appropriately.

        // Layout those GUI elements that depend on map mode.
        LayoutGUIElementsForMapMode();

		// Lower the top section of UI to position them within the sfe area (below device notch).
		float safeTop = -(Screen.safeArea.position.y / MapView.instance.canvas.scaleFactor);
		if (Screen.safeArea.position.y <= 38) safeTop = 0; // Ignore small "status bar" band of unsafe area at top.

        statusBarsLeftRectTransform.anchoredPosition = new Vector2(mainUILeftWidth, safeTop);
        statusBarsRightRectTransform.anchoredPosition = new Vector2(0, safeTop);

		// Determine whether the areas for the left and right status bars overlap.
		Vector3[] corners = new Vector3[4];
		statusBarsLeftRectTransform.GetWorldCorners(corners);
		Vector3 barsLeftTopRight = corners[2];
		statusBarsRightRectTransform.GetWorldCorners(corners);
		Vector3 barsRightTopLeft = corners[1];
		bool barAreasOverlap = barsLeftTopRight.x >= barsRightTopLeft.x;

		// Position the AP Bar in the second row if it would overlap with the right status bars.
		if (barAreasOverlap) {
			apBarRectTransform.anchoredPosition = new Vector2(108, -37);
		} else {
			apBarRectTransform.anchoredPosition = new Vector2(182, -1);
		}

        PositionElementsAboveChat();

        // Position the game panel base
        gamePanelBaseRectTransform.SetInsetAndSizeFromParentEdge(RectTransform.Edge.Left, mainUILeftWidth, canvasRectTransform.rect.width - mainUILeftWidth);
        gamePanelBaseRectTransform.SetInsetAndSizeFromParentEdge(RectTransform.Edge.Top, 0, canvasRectTransform.rect.height - mainUIBottomHeight);

        // Position the alert base
        alertBaseRectTransform.SetInsetAndSizeFromParentEdge(RectTransform.Edge.Left, mainUILeftWidth, canvasRectTransform.rect.width - mainUILeftWidth);
        alertBaseRectTransform.SetInsetAndSizeFromParentEdge(RectTransform.Edge.Top, 0, canvasRectTransform.rect.height - mainUIBottomHeight);

        // Update the tutorial system
        Tutorial.instance.ResolutionChanged();

        //// Update position of Everyplay face cam
        //PositionEveryplayFaceCam();

        // Update scaling of panels
        gamePanelBaseObject.GetComponent<ScalePanel>().UpdateScale();
        panelBase.GetComponent<ScalePanel>().UpdateScale();
        Requestor.instance.gameObject.GetComponent<ScalePanel>().UpdateScale();
        ReportDialog.instance.gameObject.GetComponent<ScalePanel>().UpdateScale();
        announcementPanel.GetComponent<ScalePanel>().UpdateScale();

		// Have TextFitters update font sizes for the current resolution.
		TextFitter.FitAll();
    }

	public void LayoutGUIElementsForMapMode()
    {
        compassRoseRectTransform.gameObject.SetActive(GameData.instance.mapMode == GameData.MapMode.MAINLAND);
        medalsTextRectTransform.gameObject.SetActive(GameData.instance.mapMode != GameData.MapMode.MAINLAND);
        mapLocationTextRectTransform.anchoredPosition = new Vector2(51f, (GameData.instance.mapMode == GameData.MapMode.MAINLAND) ? -129f : -114f);
        tempsListRectTransform.anchoredPosition = new Vector2(13f, (GameData.instance.mapMode == GameData.MapMode.MAINLAND) ? -141f : -126f);

        // Update layout of the GUI elements positoned above chat.
        PositionElementsAboveChat();
    }

    public void InitGraphicsQuality()
    {
        GraphicsQuality graphics_quality;

        // Default graphics quality depends on platform; lower default quality for mobile.
#if UNITY_ANDROID || UNITY_IOS
        graphics_quality = GraphicsQuality.GOOD;
#else
        graphics_quality = GraphicsQuality.GREAT;
#endif

        //Debug.Log("Default init graphics quality: " + graphics_quality);

        if (PlayerPrefs.HasKey("graphics_quality"))
        {
            // Use the user-chosen graphics quality setting, if there is one.
            graphics_quality = (GraphicsQuality)(PlayerPrefs.GetInt("graphics_quality"));
            userChoseGraphicsQuality = true;
        }
        else if (PlayerPrefs.HasKey("auto_graphics_quality"))
        {
            // Otherwise, use the automatically determined graphics quality setting, if there is one.
            graphics_quality = (GraphicsQuality)(PlayerPrefs.GetInt("auto_graphics_quality"));
        }

        Debug.Log("Final init graphics quality: " + graphics_quality);

        // Set the graphics quality to the determined starting value.
        SetGraphicsQuality(graphics_quality, true);
    }

    public void SetGraphicsQuality(GraphicsQuality _graphics_quality, bool _update_dropdown)
    {
        // Avoid infinite loop
        if (updatingUIElement) {
            return;
        }

        // Record the new graphics quality
        graphicsQuality = _graphics_quality;

        // Set the options panel's graphics quality dropdown's value.
        updatingUIElement = true;
        OptionsPanel.instance.graphicsQualityDropdown.value = (int)_graphics_quality;
        updatingUIElement = false;

        // Set Unity's graphics quality
        QualitySettings.SetQualityLevel((int)graphicsQuality, true);

        // Have the map make any necessary changes for this change in graphics quality level
        MapView.instance.SetGraphicsQuality(_graphics_quality);
    }

    public void UpdateTargetFrameRate()
    {
        int targetFrameRate = 60;

        if (hasFocus)
        {
            if (IsInGame())
            {
#if UNITY_ANDROID || UNITY_IOS
                targetFrameRate = TARGET_FRAMERATE_MOBILE;
#else
                targetFrameRate = TARGET_FRAMERATE_DESKTOP;
#endif
            }
            else
            {
                targetFrameRate = TARGET_FRAMERATE_OUT_OF_GAME;
            }
        }
        else
        {
#if UNITY_ANDROID || UNITY_IOS
                targetFrameRate = TARGET_FRAMERATE_MOBILE_OUT_OF_FOCUS;
#else
                targetFrameRate = TARGET_FRAMERATE_DESKTOP_OUT_OF_FOCUS;
#endif
        }

        if (QualitySettings.vSyncCount > 0)
        {
            // Framerate is synched with vSync. Set vSyncCount to approximate target framerate.
            QualitySettings.vSyncCount = (targetFrameRate > 30) ? 1 : 2;
            //Debug.Log("UpdateTargetFrameRate() (hasFocus: " + hasFocus + ", IsInGame(): " + IsInGame() + ") set vSyncCount to " + QualitySettings.vSyncCount);
        }
        else
        {
            // Set the new target framerate
            Application.targetFrameRate = targetFrameRate;
            //Debug.Log("UpdateTargetFrameRate() (hasFocus: " + hasFocus + ", IsInGame(): " + IsInGame() + ") set targetFrameRate to " + Application.targetFrameRate);
        }
    }

    public bool IsUpdatingUIElement()
    {
        return updatingUIElement;
    }

    // Returns true if a UI text input field is receiving keyboard events. Used to disable other keyboard responses such as map navigation.
    public bool InputFieldIsFocused()
    {
        return Chat.instance.chatInputField.isFocused;
    }

    public void PositionElementsAboveChat()
    {
        // Position chat icon
        Chat.instance.chatIconRectTransform.SetInsetAndSizeFromParentEdge(RectTransform.Edge.Left, mainUILeftWidth + 9, Chat.instance.chatIconRectTransform.rect.width);
        Chat.instance.chatIconRectTransform.SetInsetAndSizeFromParentEdge(RectTransform.Edge.Top, canvasRectTransform.rect.height - mainUIBottomHeight - Chat.instance.chatIconRectTransform.rect.height - 6, Chat.instance.chatIconRectTransform.rect.height);

        // Position the message text
        int x_offset = Chat.instance.GetChatDisplayMode() == Chat.ChatDisplayMode.ICON ? 70 : 10;
        messageTextRectTransform.SetInsetAndSizeFromParentEdge(RectTransform.Edge.Left, mainUILeftWidth + x_offset, canvasRectTransform.rect.width - mainUILeftWidth - 70);
        messageTextRectTransform.SetInsetAndSizeFromParentEdge(RectTransform.Edge.Bottom, Chat.instance.GetChatHeight() + mainUIBottomHeight, 20);

        // Position ad bonus button
        bool show_chat_icon = (chatSystem.GetChatDisplayMode() == Chat.ChatDisplayMode.ICON);
        adBonusButtonRectTransform.SetInsetAndSizeFromParentEdge(RectTransform.Edge.Left, mainUILeftWidth + 8, adBonusButtonRectTransform.rect.width);
        adBonusButtonRectTransform.SetInsetAndSizeFromParentEdge(RectTransform.Edge.Bottom, (show_chat_icon ? 40 : 25) + Chat.instance.GetChatHeight() + mainUIBottomHeight + 6, adBonusButtonRectTransform.rect.height);

        // Position switch map button
        bool show_switch_map_button = (GameData.instance.mapMode != GameData.MapMode.REPLAY);
        switchMapButtonRectTransform.SetInsetAndSizeFromParentEdge(RectTransform.Edge.Right, 8, switchMapButtonRectTransform.rect.width);
        switchMapButtonRectTransform.SetInsetAndSizeFromParentEdge(RectTransform.Edge.Bottom, Chat.instance.GetChatHeight() + mainUIBottomHeight + 6, switchMapButtonRectTransform.rect.height);

        // Position tournament button
        tournamentButtonRectTransform.SetInsetAndSizeFromParentEdge(RectTransform.Edge.Right, 5, tournamentButtonRectTransform.rect.width);
        tournamentButtonRectTransform.SetInsetAndSizeFromParentEdge(RectTransform.Edge.Bottom, Chat.instance.GetChatHeight() + mainUIBottomHeight + 2 + (show_switch_map_button ? switchMapButtonRectTransform.rect.height + 6 : 0), tournamentButtonRectTransform.rect.height);

        // Position raid button
        raidButtonRectTransform.SetInsetAndSizeFromParentEdge(RectTransform.Edge.Right, 8, raidButtonRectTransform.rect.width);
        raidButtonRectTransform.SetInsetAndSizeFromParentEdge(RectTransform.Edge.Bottom, Chat.instance.GetChatHeight() + mainUIBottomHeight + 6 + (show_switch_map_button ? switchMapButtonRectTransform.rect.height + 6 : 0), raidButtonRectTransform.rect.height);

        // Position replay controls
        replayControlsRectTransform.SetInsetAndSizeFromParentEdge(RectTransform.Edge.Right, 8 + raidButtonRectTransform.rect.width, replayControlsRectTransform.rect.width);
        replayControlsRectTransform.SetInsetAndSizeFromParentEdge(RectTransform.Edge.Bottom, Chat.instance.GetChatHeight() + mainUIBottomHeight + 6, replayControlsRectTransform.rect.height);
    }

    public void DisplayMessage(string _message)
    {
        messageText.DisplayMessage(_message);
    }

    public void SetFullScreen(bool _fullscreen)
    {
#if UNITY_EDITOR || UNITY_STANDALONE || UNITY_WEBPLAYER || UNITY_WEBGL // Don't call SetResolution() on mobile platforms, it can cause incorrect resolution upon orientation changes.
        if (_fullscreen)
        {
            Screen.SetResolution(Screen.currentResolution.width, Screen.currentResolution.height, true);
        }
        else
        {
            Screen.SetResolution(initialScreenWidth, initialScreenHeight, false);
        }
#endif
    }

    public bool IsPanelOpen()
    {
        return panelBase.activeSelf;
    }

    public void OpenSuspendScreen(bool _due_to_inactivity, string _message, bool _playMusic = true)
    {
        // Close any panels that are open.
        DeactivateAllPanels();

        // Deactivate any open game panel.
        DeactivateGamePanel();

        // Close the tutorial
        Tutorial.instance.Close();

        // Record whether the suspension was due to inactivity.
        suspended_due_to_inactivity = _due_to_inactivity;

        // Set the suspend screen's message text.
        suspendScreenText.text = _message;

        // If not suspending due to inactivity, wait a while before showing "Continue" button.
        if (_due_to_inactivity)
        {
            suspendScreenButton.gameObject.SetActive(true);
            suspendScreenButton.gameObject.GetComponent<GUITransition>().SetState(1, 1);
        }
        else
        {
            suspendScreenButton.gameObject.SetActive(false);
            StartCoroutine(ShowContinueButtonAfterDelay());
        }

        // Show the suspend screen.
        suspendScreen.SetActive(true);

        // Have the suspend screen's close button appear.
        suspendScreenCloseButton.gameObject.SetActive(true);
        suspendScreenCloseButton.gameObject.GetComponent<GUITransition>().StartTransition(0, 1, 1, 1, false, 0);

        // Allow a first attempt to connect to be made immediately.
        prev_attempt_connect_time = Time.unscaledTime - ATTEMPT_CONNECT_MIN_INTERVAL;

        // Pause the game camera while suspend screen is open
        MapView.instance.PauseCamera(false);

        // Update the target framerate for being out of the game.
        UpdateTargetFrameRate();

        // Play the menu music, interrupting any other music that's playing.
        Debug.Log("OpenSuspendScreen() _playMusic: " + _playMusic + ", is playing: " + Sound.instance.IsMusicPlaying() + ", " + Sound.instance.prevMusic);
        if (_playMusic) {
            PlayMenuMusic(true);
        }
    }

    public void SuspendScreenShowMessage(string _message)
    {
        // Set the suspend screen's message text.
        suspendScreenText.text = _message;
    }

    public void SuspendScreenShowProgress(float _progress)
    {
        if (_progress >= 1f)
        {
            //// Clear the message
            //GameGUI.instance.SuspendScreenShowMessage("");

            // Hide the progress bar
            suspendScreenProgressBar.SetActive(false);
        }
        else
        {
            //// Display downloading message
            //GameGUI.instance.SuspendScreenShowMessage(LocalizationManager.GetTranslation("downloading"));

            // Show the progress bar
            suspendScreenProgressBar.SetActive(true);

            // Set the progress bar's fill image for the current progress state.
            suspendScreenProgressBarFill.fillAmount = _progress;
        }
    }

    public void SuspendScreenShowImage(bool _fast)
    {
        StartCoroutine(SuspendScreenShowImage_Coroutine(_fast));
    }

    public IEnumerator SuspendScreenShowImage_Coroutine(bool _fast)
    {
        if (_fast)
        {
            // If the suspend screen image is already shown, do not fade it in again.
            if (suspendScreenImage.color.a == 1f) {
                yield break;
            }

            float fadeStartTime = Time.unscaledTime;
            float fadeDuration = 0.5f;

            while (Time.unscaledTime < (fadeStartTime + fadeDuration))
            {
                suspendScreenImage.color = suspendScreenLogo.color = new Color(1, 1, 1, (Time.unscaledTime - fadeStartTime) / fadeDuration);
                yield return null;
            }

            suspendScreenImage.color = suspendScreenLogo.color = new Color(1, 1, 1, 1);
        }
        else
        {
            yield return new WaitForSeconds(2.1f);

            float fadeStartTime = Time.unscaledTime;
            float fadeDuration = 0.5f;

            while (Time.unscaledTime < (fadeStartTime + fadeDuration))
            {
                suspendScreenImage.color = suspendScreenLogo.color = new Color(1, 1, 1, (Time.unscaledTime - fadeStartTime) / fadeDuration);
                yield return null;
            }

            suspendScreenImage.color = suspendScreenLogo.color = new Color(1, 1, 1, 1);
        }
    }

    public IEnumerator ShowContinueButtonAfterDelay()
    {
        // Wait 30 seconds before showing the continue button
        yield return new WaitForSeconds(30f);

        // If the continue button is already active, another opening of the suspend screen activated it. Exit this coroutine.
        if (suspendScreenButton.gameObject.activeSelf) {
            yield break;
        }

        // If there is no message, the suspend screen is just being used as a background. Dont show the button.
        if (suspendScreenText.text == "") {
            yield break;
        }

        // Transition in the continue button
        suspendScreenButton.gameObject.SetActive(true);
        suspendScreenButton.gameObject.GetComponent<GUITransition>().StartTransition(0, 1, 1, 1, false);
    }

    public void CloseSuspendScreen()
    {
        suspendScreen.SetActive(false);
    }

    public bool IsMapFocus()
    {
        return !(suspendScreen.activeSelf || panelBase.activeSelf || gamePanelBaseObject.activeSelf);
    }

    public void InfoEventReceived()
    {
        // Close any panels that are open.
        DeactivateAllPanels();

        // Deactivate any open game panel.
        DeactivateGamePanel();

        // Hide the suspend screen
        suspendScreen.SetActive(false);

        // Hide the indicators
        energyDecreaseIndicator.SetActive(false);
        energyInertIndicator.SetActive(false);
        manpowerBurnIndicator.SetActive(false);
        lowGeoIndicator.SetActive(false);

        // Initialize the ad bonus button.
        adBonusButton.Initialize((GameData.instance.adBonusesAllowed && (GameData.instance.adBonusAvailable > 0)) ? GameData.instance.adBonusAvailable : 0);

        // Hide the camera menu
        cameraMenu.SetActive(false);

        // Show or hide the camera button, depending on the platform.
#if UNITY_EDITOR || UNITY_ANDROID || UNITY_IOS
        cameraButton.SetActive(true);
#else
        cameraButton.SetActive(false);
#endif

        // The player's nation's color has been set.
        NationColorChanged();

        // Play the enter game music
        Sound.instance.PlayMusic(Sound.instance.musicEnterGame, false, 0f, -1, 0f, 0f);

        // Update all GUI elements for the received game info.
        UpdateGUIForGameInfo();

        // Set screenshot/video toggle according to whether the user's RECORD_VIDEO flag is set.
        //Debug.Log("RECORD_VIDEO user flag: " + GameData.instance.GetUserFlag(GameData.UserFlags.RECORD_VIDEO));
        updatingUIElement = true;
        if (GameData.instance.GetUserFlag(GameData.UserFlags.RECORD_VIDEO)) {
            OnPress_CameraVideoToggle();
        } else {
            OnPress_CameraScreenshotToggle();
        }
        updatingUIElement = false;

        // If the nation has not yet been customized, open the NewNationPanel.
        if (!GameData.instance.GetNationFlag(GameData.NationFlags.CUSTOM_NATION_NAME)) {
            GameGUI.instance.OpenNewNationPanel();
        }

        // Set the game to fullscreen if appropriate
        SetFullScreen(GameData.instance.GetUserFlag(GameData.UserFlags.FULLSCREEN));

        // Show the map location if appropriate.
        SetShowMapLocationState(GameData.instance.GetUserFlag(GameData.UserFlags.SHOW_MAP_LOCATION));

        // Make sure the flash object is turned off.
        flashObject.SetActive(false);

        // Tell the tutorial system that an info event has been received.
        Tutorial.instance.InfoEventReceived();
    }

    public void AccountInfoEventReceived()
    {
        // Have the MapView process the account info event.
        MapView.instance.AccountInfoEventReceived();

        // Have the panels process the account info event
        NationPanel.instance.AccountInfoEventReceived();
        OptionsPanel.instance.AccountInfoEventReceived();
    }

    public void UpdateGUIForGameInfo()
    {
        // Credit package names
        GameGUI.instance.buyCreditsPackageName = new string[GameData.instance.numCreditPackages];
        GameGUI.instance.buyCreditsPackageName[0] = LocalizationManager.GetTranslation("Credit Package Name/boost"); // "Boost"
        GameGUI.instance.buyCreditsPackageName[1] = LocalizationManager.GetTranslation("Credit Package Name/resupply"); // "Resupply"
        GameGUI.instance.buyCreditsPackageName[2] = LocalizationManager.GetTranslation("Credit Package Name/infusion"); // "Infusion"
        GameGUI.instance.buyCreditsPackageName[3] = LocalizationManager.GetTranslation("Credit Package Name/mother_lode"); // "Mother Lode";

        if (GameData.instance.numSubscriptionTiers > 0)
        {
            // Subscription tier names
            GameGUI.instance.subscriptionTierName = new string[GameData.instance.numSubscriptionTiers];
            GameGUI.instance.subscriptionTierName[0] = LocalizationManager.GetTranslation("Subscribe Panel/word_commander"); // "Commander"
            GameGUI.instance.subscriptionTierName[1] = LocalizationManager.GetTranslation("Subscribe Panel/sovereign_commander"); // "Sovereign"
        }

        // Update status bars.
        levelText.text = "" + GameData.instance.level;
        xpBar.SetMaxValue(GameData.instance.next_level_xp_threshold - GameData.instance.level_xp_threshold);
        xpBar.SetValue(GameData.instance.xp - GameData.instance.level_xp_threshold, false);
        apBar.SetMaxValue(1);
        apBar.gameObject.SetActive(GameData.instance.advance_points > 0);
        //apBar.SetVisibility(GameData.instance.advance_points > 0, false);
        apBar.SetValue(GameData.instance.advance_points, false);
        creditsBar.SetMaxValue(1000);
        creditsBar.SetValue(GameData.instance.credits, false);

        // Have the chat system process the info event.
        chatSystem.InfoEventReceived();

        // Have the MapView process the info event.
        MapView.instance.InfoEventReceived();

        XmlNode news_node = Network.instance.infoXmlDoc.SelectSingleNode("/info/news");
        int cur_update = Int32.Parse(news_node.Attributes["update"].Value);

        if (PlayerPrefs.HasKey("news_update") && (PlayerPrefs.GetInt("news_update") != cur_update))
        {
            // Display the news panel
            SetActiveGamePanel(GamePanel.GAME_PANEL_CONNECT);
            ConnectPanel.instance.TabPressed(ConnectPanel.instance.newsPanelTab);
        }
        else
        {
            // Only show report if the nation has already customized its nation name.
            if (GameData.instance.GetNationFlag(GameData.NationFlags.CUSTOM_NATION_NAME))
            {
                // Have the user login report panel process the info event, and display if appropriate.
                ReportPanel.instance.InfoEventReceived();
            }
        }

        // Record the current news update number.
        PlayerPrefs.SetInt("news_update", cur_update);

        // Have the panels process the info event
        NationPanel.instance.InfoEventReceived();
        OptionsPanel.instance.InfoEventReceived();
        MessagesPanel.instance.InfoEventReceived();
        QuestsPanel.instance.InfoEventReceived();
        AdvancesPanel.instance.InfoEventReceived();
        ConnectPanel.instance.InfoEventReceived();
        CustomizePanel.instance.InfoEventReceived();
        TournamentButton.instance.InfoEventReceived();

        // Update the raid score header.
        RaidScoreHeader.instance.InfoEventReceived();

        // If there are any available advance points, turn on the Advances panel alert indicator.
        GameGUI.instance.SetPanelAlertState(GameGUI.GamePanel.GAME_PANEL_ADVANCES, GameData.instance.advance_points > 0);

		// Have TextFitters update font sizes for the current language.
		TextFitter.FitAll();
    }

    public void OnSwitchedMap(GameData.MapMode _mapMode)
    {
        Debug.Log("OnSwitchedMap() called");

        // Update the panel buttons.
        if (_mapMode == GameData.MapMode.MAINLAND)
        {
            questsButtonLeft.gameObject.SetActive(true);
            questsButtonBottom.gameObject.SetActive(true);
            raidLogButtonLeft.gameObject.SetActive(false);
            raidLogButtonBottom.gameObject.SetActive(false);
        }
        else
        {
            questsButtonLeft.gameObject.SetActive(false);
            questsButtonBottom.gameObject.SetActive(false);
            raidLogButtonLeft.gameObject.SetActive(true);
            raidLogButtonBottom.gameObject.SetActive(true);
        }

        // Update the panels.
        NationPanel.instance.UpdateForStatsEvent();

        // Update the build menu.
        BuildMenu.instance.Refresh();

        // Update the tournament button.
        TournamentButton.instance.OnSwitchedMap();

        // Update the raid button.
        RaidButton.instance.OnSwitchedMap();

        // Update the switch map button.
        SwitchMapButton.instance.OnSwitchedMap();

        // Update the raid intro header.
        RaidIntroHeader.instance.OnSwitchedMap();

        // Update the raid score header.
        RaidScoreHeader.instance.OnSwitchedMap();

        // Update the replay controls.
        ReplayControls.instance.OnSwitchedMap();

        // Update the tutorial system.
        Tutorial.instance.OnSwitchedMap();

        // Re-layout those GUI elements that depend on map mode.
        LayoutGUIElementsForMapMode();

        // Update GUI as if an initial stats event had been received.
        UpdateForStatsEvent(true);

        // Update GUI as if an update event had been received.
        UpdateForUpdateEvent();
    }

    public void UpdateForUpdateEvent()
    {
        // Keep track of when previous update event was received.
        prevUpdateEventTime = Time.unscaledTime;

        float geoEfficiency = GameData.instance.GetFinalGeoEfficiency();

        // Update status bars
        energyBar.SetValue((int)Math.Max(0, GameData.instance.GetFinalEnergyRate() - GameData.instance.GetFinalEnergyBurnRate())/*GameData.instance.energy*/, true);
        energyBar.SetDisplayButton((GameData.instance.mapMode == GameData.MapMode.MAINLAND) && (GameData.instance.energy < GameData.instance.GetFinalEnergyMax()));
        energyReserveBar.fillAmount = (GameData.instance.mapMode != GameData.MapMode.MAINLAND) ? 0f : ((float)GameData.instance.energy / (float)GameData.instance.GetFinalEnergyMax());
        manpowerBar.SetValue(GameData.instance.current_footprint.manpower, true);
        creditsBar.SetValue(GameData.instance.credits, true);
        geoBar.SetValue((int)(geoEfficiency * 100f + 0.5f), true);

        // Update panels
        NationPanel.instance.UpdateForUpdateEvent();
        CashOutPanel.instance.UpdateForUpdateEvent();

        float energyRate = GameData.instance.GetFinalEnergyRate();

        bool energy_decreasing = ((GameData.instance.mapMode == GameData.MapMode.MAINLAND) && (GameData.instance.energy > 0)) && (GameData.instance.GetFinalEnergyBurnRate() > energyRate);

        // Play sound if now entering energy defecit.
        if (energy_decreasing && (energyDecreaseIndicator.activeSelf == false)) {
            Sound.instance.Play2D(Sound.instance.energy_defecit);
        }

        // Turn on indicators if appropriate.
        energyDecreaseIndicator.SetActive(energy_decreasing);
        energyInertIndicator.SetActive(((GameData.instance.mapMode != GameData.MapMode.MAINLAND) || (GameData.instance.energy == 0)) && (GameData.instance.GetFinalEnergyBurnRate() > energyRate));
        manpowerBurnIndicator.SetActive((GameData.instance.manpowerBurnRate > 0f) && (GameData.instance.mapMode != GameData.MapMode.RAID));
        if (manpowerBurnIndicator.activeSelf) manpowerBurnIndicator.GetComponent<Image>().sprite = ((GameData.instance.manpowerBurnRate < ((GameData.instance.GetMainlandManpowerRate()) * 0.25f)) ? indicatorYellowSprite : indicatorRedSprite );
        lowGeoIndicator.SetActive(GameData.instance.BenefitsFromGeoEfficiency() && (geoEfficiency < 1f));
        if (lowGeoIndicator.activeSelf) lowGeoIndicator.GetComponent<Image>().sprite = (geoEfficiency >= 0.75f) ? indicatorYellowSprite : indicatorRedSprite;

        // Update inert states of objects on map, if necessary.
        cur_update_inert = (((GameData.instance.mapMode != GameData.MapMode.MAINLAND) || (GameData.instance.energy == 0)) && (GameData.instance.GetFinalEnergyBurnRate() > energyRate)) || (geoEfficiency < 1f);
        //Debug.Log("prev_update_inert: " + prev_update_inert + ", cur_update_inert: " + cur_update_inert);

        if (cur_update_inert || prev_update_inert)
        {
            mapView.UpdateForInert();
            prev_update_inert = cur_update_inert;
        }

        // If the buy panel is active, update it.
        if (buyPanel.activeInHierarchy) {
            buyPanel.GetComponent<BuyPanel>().Setup();
        }

        // Update raid button.
        RaidButton.instance.OnUpdateEvent();
    }

    public void UpdateForUpdateBarsEvent(int _energy_delta, int _energy_rate_delta, int _energy_burn_rate_delta, int _manpower_delta, int _credits_delta)
    {
        int excess_energy_rate_delta = _energy_rate_delta - _energy_burn_rate_delta;

        // Determine whether the energy bar should display the button to buy energy.
        energyBar.SetDisplayButton((GameData.instance.mapMode == GameData.MapMode.MAINLAND) && (GameData.instance.energy < GameData.instance.GetFinalEnergyMax()));

        if (excess_energy_rate_delta/*_energy_delta*/ != 0)
        {
            // Update the energy bar for (just) the given delta, caused by this player.
            energyBar.SetValue((int)Math.Max(0, GameData.instance.GetFinalEnergyRate() + _energy_rate_delta - (GameData.instance.GetFinalEnergyBurnRate() + _energy_burn_rate_delta)) /*Mathf.Max(0, energyBar.GetValue() + _energy_delta)*/, true);

            // Show animated text of the amount of change.
            energyBar.DisplayAnimText(((excess_energy_rate_delta/*_energy_delta*/ >= 0) ? "+" : "-") + Mathf.Abs(excess_energy_rate_delta/*_energy_delta*/));

            // Play sound
            if (excess_energy_rate_delta/*_energy_delta*/ > 0) {
                Sound.instance.Play2D(Sound.instance.energy);
            }
        }

        if (_energy_delta != 0)
        {
            // Update the energy reserve bar for the new energy reserve value.
            energyReserveBar.fillAmount = (GameData.instance.mapMode != GameData.MapMode.MAINLAND) ? 0f : ((float)GameData.instance.energy / (float)GameData.instance.GetFinalEnergyMax());
        }

        if (_manpower_delta != 0)
        {
            // Update the manpower bar for (just) the given delta, caused by this player.
            manpowerBar.SetValue(Mathf.Max(0, manpowerBar.GetValue() + _manpower_delta), true);

            // Show animated text of the amount of change.
            manpowerBar.DisplayAnimText(((_manpower_delta >= 0) ? "+" : "-") + Mathf.Abs(_manpower_delta));

            // Play sound
            if (_manpower_delta > 0) {
                Sound.instance.Play2D(Sound.instance.manpower);
            }
        }

        if (_credits_delta != 0)
        {
			//Debug.Log("UpdateForUpdateBarsEvent() credits bar value: " + creditsBar.GetValue() + ", _credits_delta: " + _credits_delta);

            // Update the credits bar for (just) the given delta, caused by this player.
            creditsBar.SetValue(Mathf.Max(0, creditsBar.GetValue() + _credits_delta), true);

            // Show animated text of the amount of change.
            creditsBar.DisplayAnimText(((_credits_delta >= 0) ? "+" : "-") + Mathf.Abs(_credits_delta));

            // Play sound
            if (_credits_delta > 0) {
                Sound.instance.Play2D(Sound.instance.credits);
            }
        }
    }

    public void UpdateForStatsEvent(bool _initial_stats_event)
    {
        // If this is not the initial stats update event, and if the energy or manpower has changed by a significant amount, highlight the change.
        float manpower_delta = (GameData.instance.current_footprint == null) ? 0 : (GameData.instance.current_footprint.manpower - manpowerBar.GetValue());
        //float energy_delta = GameData.instance.energy - energyBar.GetValue();
        float excess_energy_rate_delta = (int)(GameData.instance.GetFinalEnergyRate() - GameData.instance.GetFinalEnergyBurnRate()) - energyBar.GetValue();
        bool highlight_manpower_delta = ((_initial_stats_event == false) && (Math.Abs(manpower_delta) >= (GameData.instance.GetFinalManpowerMax() * 0.01f)));
        bool highlight_energy_delta = ((_initial_stats_event == false) && (energyBar.GetValue() > 0) && (Math.Abs(excess_energy_rate_delta/*energy_delta*/) >= (GameData.instance.GetFinalEnergyRate()/*GameData.instance.energyMax*/ * 0.01f)));

        // Update status bars
        energyBar.SetMaxValue((int)GameData.instance.GetFinalEnergyRate()/*GameData.instance.energyMax*/);
        energyBar.SetValue((int)Math.Max(0, GameData.instance.GetFinalEnergyRate() - GameData.instance.GetFinalEnergyBurnRate())/*GameData.instance.energy*/, highlight_energy_delta);
        energyBar.SetDisplayButton((GameData.instance.mapMode == GameData.MapMode.MAINLAND) && (GameData.instance.energy < GameData.instance.GetFinalEnergyMax()));
        energyReserveBar.fillAmount = (GameData.instance.mapMode != GameData.MapMode.MAINLAND) ? 0f : ((float)GameData.instance.energy / (float)GameData.instance.GetFinalEnergyMax());
        manpowerBar.SetMaxValue(GameData.instance.GetFinalManpowerMax());
        manpowerBar.SetValue((GameData.instance.current_footprint == null) ? 0 : GameData.instance.current_footprint.manpower, highlight_manpower_delta);
        apBar.gameObject.SetActive(GameData.instance.advance_points > 0);
        //apBar.SetVisibility(GameData.instance.advance_points > 0, true);
        apBar.SetValue(GameData.instance.advance_points, false);
        geoBar.SetMaxValue(100);
        geoBar.SetValue((int)(GameData.instance.GetFinalGeoEfficiency() * 100f + 0.5f), true);

        // Update medals text
        medalsText.text = string.Format("{0:n0}", GameData.instance.raidNumMedals) + " <size=20><sprite=30></size>";

        if (highlight_energy_delta)
        {
            // Show animated text of the amount of change.
            energyBar.DisplayAnimText(((excess_energy_rate_delta/*energy_delta*/ >= 0) ? "+" : "-") + (int)Mathf.Abs(excess_energy_rate_delta/*energy_delta*/));
        }

        if (highlight_manpower_delta)
        {
            // Show animated text of the amount of change.
            manpowerBar.DisplayAnimText(((manpower_delta >= 0) ? "+" : "-") + Mathf.Abs(manpower_delta));
        }

        // Update the Nation panel
        NationPanel.instance.UpdateForStatsEvent();

        // If there are any available advance points, turn on the Advances panel alert indicator.
        GameGUI.instance.SetPanelAlertState(GameGUI.GamePanel.GAME_PANEL_ADVANCES, GameData.instance.advance_points > 0);

        // Update raid button.
        RaidButton.instance.OnStatsEvent();
    }

    public void UpdateForLocalization()
    {
        // Update the Advances Panel
        AdvancesPanel.instance.UpdateForLocalization();

        // Update the Quests Panel
        QuestsPanel.instance.UpdateForLocalization();

        // Update the Build Menu
        BuildMenu.instance.Refresh();

        GameGUI.instance.buyCreditsPackageName = new string[Math.Max(4, GameData.instance.numCreditPackages)];
        GameGUI.instance.buyCreditsPackageName[0] = LocalizationManager.GetTranslation("Credit Package Name/boost"); // "Boost"
        GameGUI.instance.buyCreditsPackageName[1] = LocalizationManager.GetTranslation("Credit Package Name/resupply"); // "Resupply"
        GameGUI.instance.buyCreditsPackageName[2] = LocalizationManager.GetTranslation("Credit Package Name/infusion"); // "Infusion"
        GameGUI.instance.buyCreditsPackageName[3] = LocalizationManager.GetTranslation("Credit Package Name/mother_lode"); // "Mother Lode";

		// Have TextFitters update font sizes for new language.
		TextFitter.FitAll();
    }

    public void AddedXP(int _xp_delta, int _xp, int _userID, int _block_x, int _block_y)
    {
        // Update the XP bar.
        if (_userID == GameData.instance.userID)
        {
            // This player is responsible for this increase in XP. Show exactly this increase.
            xpBar.SetValue(xpBar.GetValue() + _xp_delta, true);

            // Show animated text of the amount being added.
            xpBar.DisplayAnimText(((_xp_delta >= 0) ? "+" : "-") + Mathf.Abs(_xp_delta));
        }
        else
        {
            // This player is not responsible for this increase in XP. Show the change to the true new value.
            xpBar.SetValue(_xp - GameData.instance.level_xp_threshold, true);
        }

        // Update the nation panel.
        NationPanel.instance.UpdateForXP();

        // Tell the tutorial system that XP has been added.
        Tutorial.instance.AddedXP(_xp_delta);
    }

    public void ChangedLevel()
    {
        // Update the level text display.
        levelText.text = "" + GameData.instance.level;

        // Display animated text effect showing new level.
        levelBulb.DisplayAnimText("" + GameData.instance.level, 0, 0, 1f, 5f, 1f, 0f, 4);

        // Update the XP bar.
        xpBar.SetMaxValue(GameData.instance.next_level_xp_threshold - GameData.instance.level_xp_threshold);
        xpBar.SetValue(GameData.instance.xp - GameData.instance.level_xp_threshold, false);

        // Update the advancement points bar.
        apBar.gameObject.SetActive(GameData.instance.advance_points > 0);
        //apBar.SetVisibility(GameData.instance.advance_points > 0, true);
        apBar.SetValue(GameData.instance.advance_points, true);

        // Display credits reward
        if (creditsBar.GetValue() < GameData.instance.credits)
        {
            creditsBar.DisplayAnimText("+" + (GameData.instance.credits - creditsBar.GetValue()));
            creditsBar.SetValue(GameData.instance.credits, true);
        }

        // Play sound
        Sound.instance.Play2D(Sound.instance.level_up);

        // Display message
        // "We have advanced to level {[NATION_LEVEL]}!"
        DisplayMessage(LocalizationManager.GetTranslation("Client Message/level_advanced_to_x")
            .Replace("{[NATION_LEVEL]}", GameData.instance.level.ToString()));

        // Update the AdvancesPanel, in case any advances have become available by reaching the new level.
        AdvancesPanel.instance.UpdateForTechnologies();

        // Update the nation panel for the new level and XP amount.
        NationPanel.instance.UpdateForLevel();
        NationPanel.instance.UpdateForXP();

        // If there are any available advance points, turn on the Advances panel alert indicator.
        GameGUI.instance.SetPanelAlertState(GameGUI.GamePanel.GAME_PANEL_ADVANCES, GameData.instance.advance_points > 0);

        // Tell the tutorial system about this change in level, if we have an Advance Point.
        Tutorial.instance.LevelChanged();
    }

    public void ChangedManpower(int _delta, float _event_received_time)
    {
        // If no update event has been received since the event indicating this change in manpower...
        if (_event_received_time > prevUpdateEventTime)
        {
            // Update the manpower bar for (just) the given delta, caused by this player.
            manpowerBar.SetValue(Mathf.Max(0, manpowerBar.GetValue() + _delta), true);
        }

        // Show animated text of the amount of change.
        manpowerBar.DisplayAnimText(((_delta >= 0) ? "+" : "-") + Mathf.Abs(_delta));
    }

    public void ChangedEnergy(int _delta)
    {
        // Update the energy reserve bar for the new energy reserve value.
        energyReserveBar.fillAmount = (GameData.instance.mapMode != GameData.MapMode.MAINLAND) ? 0f : ((float)GameData.instance.energy / (float)GameData.instance.GetFinalEnergyMax());

        /*
        // Update the energy bar for (just) the given delta, caused by this player.
        energyBar.SetValue(Mathf.Max(0, energyBar.GetValue() + _delta), true);

        // Show animated text of the amount of change.
        energyBar.DisplayAnimText(((_delta >= 0) ? "+" : "-") + Mathf.Abs(_delta));
        */
    }

    public void ChangedGeoEfficiency(float _geo_efficiency_base)
    {
        // Record the new geo efficiency base value.
        GameData.instance.current_footprint.geo_efficiency_base = _geo_efficiency_base;

        // Display the new geo efficiency value.
        geoBar.SetMaxValue(100);
        geoBar.SetValue((int)(GameData.instance.GetFinalGeoEfficiency() * 100f + 0.5f), true);
    }

    public void SuspendEventReceived(bool _due_to_inactivity, string _message)
    {
        // Show the suspend screen, with the given message.
        OpenSuspendScreen(_due_to_inactivity, _message);

        // If suspended due to inactivity while the captcha panel is open, send message alerting the server.
        if (_due_to_inactivity && captchaPanel.activeInHierarchy) {
            Network.instance.SendCommand("action=captcha|event=suspended");
        }
    }

    public void UsernameAvailable(bool _isAvailable, bool _isSet)
    {
        if (chooseNamePanel.activeSelf)
        {
            chooseNamePanel.GetComponent<ChooseNamePanel>().UsernameAvailable(_isAvailable, _isSet);
        }
    }

    public void OpenCreditsScreen()
    {
        creditsScreen.gameObject.SetActive(true);
    }

    public void CloseAllPanels()
    {
        panelBase.GetComponent<GUITransition>().StartTransition(1, 0, 1, 1, true);

        // Disallow interactions with the panels during fade-out.
        panelBase.GetComponent<CanvasGroup>().interactable = false;
    }

    public void DeactivateAllPanels()
    {
        announcementPanel.SetActive(false);
        creditsScreen.gameObject.SetActive(false);
        activationCodePanel.SetActive(false);
        captchaPanel.SetActive(false);
        mapFlagPanel.SetActive(false);
        chooseNamePanel.SetActive(false);
        logInPanel.SetActive(false);
        joinNationPanel.SetActive(false);
        inviteUnitePanel.SetActive(false);
        postMessagePanel.SetActive(false);
        statDetailsPanel.SetActive(false);
        advanceDetailsPanel.SetActive(false);
        objectInfoPanel.SetActive(false);
        nationInfoPanel.SetActive(false);
        buyPanel.SetActive(false);
        subscribePanel.SetActive(false);
        reportPanel.SetActive(false);
        welcomePanel.SetActive(false);
        newNationPanel.SetActive(false);
        newPlayerPanel.SetActive(false);
        createPasswordPanel.SetActive(false);
        customizePanel.SetActive(false);
		contestRulesPanel.SetActive(false);
        cashOutPanel.SetActive(false);
        raidEndPanel.SetActive(false);
        sharePanel.SetActive(false);
        panelBase.SetActive(false);

        // Allow interactions with the panels
        panelBase.GetComponent<CanvasGroup>().interactable = true;
	}

    public void ShowPanelBase(float _delay=0f)
    {
        panelBase.SetActive(true);
        panelBase.GetComponent<GUITransition>().StartTransition(0, 1, 1, 1, false, _delay);
    }

    public void OpenLogInDialog()
    {
        DeactivateAllPanels();
        logInPanel.GetComponent<LogInPanel>().Init();
        logInPanel.SetActive(true);
        ShowPanelBase();

        // Clear the message field
        LogInPanel.instance.Reset();

        // Pause the game camera while log in panel is open
        MapView.instance.PauseCamera(false);

        // Update the target framerate for being out of the game.
        UpdateTargetFrameRate();

        // Play the menu music, interrupting any other music that's playing.
        PlayMenuMusic(true);
    }

    public void OpenJoinNationDialog()
    {
        DeactivateAllPanels();
        joinNationPanel.SetActive(true);
        ShowPanelBase();

        // Clear the message field
        JoinNationPanel.instance.Reset();

        // Play the menu music, interrupting any other music that's playing.
        PlayMenuMusic(true);
    }

    public void OpenInviteUniteDialog()
    {
        DeactivateAllPanels();
        inviteUnitePanel.SetActive(true);
        ShowPanelBase();
    }

    public void OpenActivationCodePanel(string _message)
    {
        DeactivateAllPanels();
        activationCodePanel.GetComponent<ActivationCodePanel>().Init(_message);
        activationCodePanel.SetActive(true);
        ShowPanelBase();

        // Pause the game camera while activation code panel is open
        MapView.instance.PauseCamera(false);

        // Update the target framerate for being out of the game.
        UpdateTargetFrameRate();
    }

    public void OpenCaptchaPanel()
    {
        DeactivateAllPanels();
        CaptchaPanel.instance.Init();
        captchaPanel.SetActive(true);
        ShowPanelBase();

        // Record when the captcha panel was last shown.
        prevCaptchaTime = Time.unscaledTime;
    }

    public void OpenMapFlagDialog(int _blockX, int _blockZ, string _text)
    {
        DeactivateAllPanels();
        mapFlagPanel.SetActive(true);
        mapFlagPanel.GetComponent<MapFlagPanel>().Init(_blockX, _blockZ, _text);
        ShowPanelBase();
    }

    public void OpenChooseNameDialog()
    {
        DeactivateAllPanels();
        chooseNamePanel.SetActive(true);
        ShowPanelBase();

        // Play the menu music, interrupting any other music that's playing.
        PlayMenuMusic(true);
    }

    public void OpenPostMessageDialog()
    {
        DeactivateAllPanels();
        postMessagePanel.SetActive(true);
        ShowPanelBase();
    }

    public void OpenStatDetailsDialog()
    {
        DeactivateAllPanels();
        statDetailsPanel.SetActive(true);
        ShowPanelBase();
    }

    public void OpenAdvanceDetailsDialog()
    {
        DeactivateAllPanels();
        advanceDetailsPanel.SetActive(true);
        ShowPanelBase();
    }

    public void OpenBuildInfoDialog(int _blockX, int _blockZ, int _buildID)
    {
        DeactivateAllPanels();
        ObjectInfoPanel.instance.InitForBuild(_blockX, _blockZ, _buildID);
        objectInfoPanel.SetActive(true);
        ShowPanelBase();
    }

    public void OpenObjectInfoDialog(int _blockX, int _blockZ, int _objectID)
    {
        DeactivateAllPanels();
        ObjectInfoPanel.instance.InitForObject(_blockX, _blockZ, _objectID);
        objectInfoPanel.SetActive(true);
        ShowPanelBase();
    }

    public void OpenNationInfoDialog(int _ID, string _name, int _level, int _area, int _num_trophies, float _geo_eff, int _stat_tech, int _stat_bio, int _stat_psi, int _num_alliances, int[] _ally_ids, string[] _ally_names, int _num_members, string[] _member_names, bool[] _member_logged_in, int[] _member_rank, int _attacker_stat, int _defender_stat)
    {
        DeactivateAllPanels();
        NationInfoPanel.instance.Init(_ID, _name, _level, _area, _num_trophies, _geo_eff, _stat_tech, _stat_bio, _stat_psi, _num_alliances, _ally_ids, _ally_names, _num_members, _member_names, _member_logged_in, _member_rank, _attacker_stat, _defender_stat);
        nationInfoPanel.SetActive(true);
        ShowPanelBase();
    }

    public void OpenCreditsInfoDialog(bool _not_enough)
    {
        // You can earn credits by completing quests, advancing in level, capturing and holding Orbs of Power, and also by watching short ads that become available when you capture land and resources, win raids and collect quest rewards!
        String text = _not_enough ? (LocalizationManager.GetTranslation("credits_info_not_enough") + " " + LocalizationManager.GetTranslation("credits_info")) : LocalizationManager.GetTranslation("credits_info");
        Requestor.Activate(0, 0, null, text, LocalizationManager.GetTranslation("Generic Text/okay"), "", Requestor.FLAG_ALIGN_LEFT);
     }

    public void OpenBuyDialog(BuyPanel.BuyType _buy_type)
    {
        bool success = BuyPanel.instance.Init(_buy_type);

        if (success)
        {
            DeactivateAllPanels();
            buyPanel.SetActive(true);
            ShowPanelBase();
        }
    }

    public void OpenSubscribeDialog()
    {
        SubscribePanel.instance.Init();

        DeactivateAllPanels();
        subscribePanel.SetActive(true);
        ShowPanelBase();
    }

    public void OpenReportPanel()
    {
        DeactivateAllPanels();
        reportPanel.SetActive(true);
        ShowPanelBase();
    }

    public void OpenWelcomePanel(float _delay=0f)
    {
        DeactivateAllPanels();
        welcomePanel.GetComponent<WelcomePanel>().Init();
        welcomePanel.SetActive(true);
        ShowPanelBase(_delay);

        /*
        // Make sure the suspend screen is inactive.
        CloseSuspendScreen();
        */

        // Clear the text and button from the suspend screen, to use as the background for the welcome menu.
        suspendScreenText.text = "";
        suspendScreenButton.gameObject.SetActive(false);
        suspendScreen.SetActive(true);

        // Have the suspend screen's close button disappear.
        suspendScreenCloseButton.gameObject.GetComponent<GUITransition>().StartTransition(1, 0, 1, 1, true, 0);

        // Pause the game camera while welcome panel is open
        MapView.instance.PauseCamera(false);

        // Update the target framerate for being out of the game.
        UpdateTargetFrameRate();

        // Play the menu music, interrupting any other music that's playing.
        PlayMenuMusic(true);

        // TESTING
        if (Network.instance.initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false)) {
            Network.instance.LogEvent(Time.time + ": Client ID " + Network.instance.GetClientID(true) + " opened welcome panel.");
        }

        //Debug.Log("OpenWelcomePanel() end, canvas: " + (MapView.instance.canvas.gameObject.activeInHierarchy ? "active" : "inactive") + ", suspend screen: " + (GameGUI.instance.suspendScreen.gameObject.activeInHierarchy ? "active" : "inactive") + ", panel base: " + (GameGUI.instance.panelBase.gameObject.activeInHierarchy ? "active" : "inactive") + ", welcome panel: " + (GameGUI.instance.welcomePanel.gameObject.activeInHierarchy ? "active" : "inactive"));
    }

    public void OpenNewNationPanel()
    {
        DeactivateAllPanels();
        newNationPanel.GetComponent<NewNationPanel>().Init();
        newNationPanel.SetActive(true);
        ShowPanelBase();
    }

    public void OpenNewPlayerPanel()
    {
        DeactivateAllPanels();
        newPlayerPanel.GetComponent<NewPlayerPanel>().Init();
        newPlayerPanel.SetActive(true);
        ShowPanelBase();
    }

    public void OpenCreatePasswordPanel(CreatePasswordPanel.Context _context)
    {
        DeactivateAllPanels();
        createPasswordPanel.GetComponent<CreatePasswordPanel>().Init(_context);
        createPasswordPanel.SetActive(true);
        ShowPanelBase();
    }

    public void OpenCustomizePanel()
    {
        DeactivateAllPanels();
        customizePanel.SetActive(true);
        ShowPanelBase();
    }

	public void OpenContestRulesPanel()
    {
        DeactivateAllPanels();
        contestRulesPanel.SetActive(true);
        ShowPanelBase();
    }

    public void OpenCashOutPanel()
    {
        DeactivateAllPanels();
        cashOutPanel.SetActive(true);
        ShowPanelBase();
    }

    public IEnumerator OnRaidFinished(float _delay)
    {
        yield return new WaitForSeconds(_delay + 1f);
        OpenRaidEndPanel();
    }

    public void OpenRaidEndPanel()
    {
        DeactivateAllPanels();
        raidEndPanel.SetActive(true);
        ShowPanelBase();
    }

    public void OpenSharePanel()
    {
        DeactivateAllPanels();
        sharePanel.SetActive(true);
        ShowPanelBase();
    }

    public void OpenReportPanelAfterDelay()
    {
        StartCoroutine(OpenReportPanelAfterDelay_Coroutine());
    }

    public IEnumerator OpenReportPanelAfterDelay_Coroutine()
    {
        yield return new WaitForSeconds(1);
        OpenReportPanel();
    }

    public void PlayMenuMusic(bool _replace_music)
    {
        StartCoroutine(PlayMenuMusic_Coroutine(_replace_music));
    }

    public IEnumerator PlayMenuMusic_Coroutine(bool _replace_music)
    {
        // Wait a few seconds before starting the menu music.
        yield return new WaitForSeconds(3f);

        // Don't play the menu music if we'e in the game.
        if (IsInGame()) {
            yield break;
        }

        // Don't play the menu music if some music is already playing, and we're not to replace it.
        if (Sound.instance.IsMusicPlaying() && !_replace_music) {
            yield break;
        }

        // Don't play the menu music if it is already playing.
        if (Sound.instance.IsMusicPlaying() && (Sound.instance.prevMusic == Sound.instance.musicMenu)) {
            yield break;
        }

        // Play the menu music.
        Sound.instance.PlayMusic(Sound.instance.musicMenu, true, 0f, 600f, 5f, 5f);
    }
    public bool IsInGame()
    {
        return ((suspendScreen.activeSelf == false) && (welcomePanel.activeSelf == false));
    }

    public void LogOut()
    {
        // Send message to server, logging out this player.
        Network.instance.SendCommand("action=log_out");

        // Clear the client ID and server ID, so that this client will no longer be associated by default
        // with that player account or server (unless it's logged in again).
        Network.instance.ClearAccountData();

        // Deactivate any open game panel.
        DeactivateGamePanel();

        // Close the tutorial
        Tutorial.instance.Close();

        // Display the welcome panel.
        OpenWelcomePanel();
    }

    public void ExitGame()
    {
        // TESTING
        if (Network.instance.initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false)) {
            Network.instance.LogEvent(Time.time + ": Client ID " + Network.instance.GetClientID(true) + " ExitGame().");
        }

#if UNITY_EDITOR
         // Application.Quit() does not work in the editor so
         // UnityEditor.EditorApplication.isPlaying need to be set to false to end the game
         UnityEditor.EditorApplication.isPlaying = false;
#else
         Application.Quit();
#endif
    }

    public void SuspendScreen_OnClick_Continue()
    {
        // Must fetch info, even if it's already been fetched, in case a new client or new data is required.
        AttemptFetchInfo();
    }
    /*
    public void OnClick_StartNewNation()
    {
        if (GameData.instance.accountCreationAllowedTime > Time.unscaledTime)
        {
            String text = LocalizationManager.GetTranslation("max_accounts_creation_delay")
                .Replace("{max_accounts}", "" + GameData.instance.maxAccountsPerPeriod)
                .Replace("{max_account_days}", "" + (GameData.instance.maxAccountsPeriod / 86400))
                .Replace("{time_remaining}", GameData.instance.GetDurationText((int)(GameData.instance.accountCreationAllowedTime - Time.unscaledTime)));

            // "You've logged in with the maximum of {max_accounts} different accounts during the past {max_account_days} days. You will be able to create a new account in {time_remaining}."
            Requestor.Activate(0, 0, null, text, LocalizationManager.GetTranslation("Generic Text/okay"), "");
        }
        else
        {
            OpenNewPlayerPanel();
        }
    }
    */
    public void ClientDownloadRequired()
    {
#if DISABLESTEAMWORKS
        // "War of Conquest has been updated,\nand a new game download is required to play." |
        Requestor.Activate((int)RequestorTasks.UPDATE_CLIENT, 0, this, LocalizationManager.GetTranslation("Client Message/new_client_download_required")
            , LocalizationManager.GetTranslation("Generic Text/download_word"), "");
# else
        // "War of Conquest has been updated,\nand a new game download is required to play." |
        Requestor.Activate((int)RequestorTasks.UPDATE_CLIENT_STEAM, 0, this, LocalizationManager.GetTranslation("Client Message/new_client_download_required_steam")
            , LocalizationManager.GetTranslation("Generic Text/exit_word"), "");
#endif // DISABLESTEAMWORKS
    }

    public void RequestBuyCredits()
    {
        if (GameData.instance.creditPurchasesAllowed)
        {
            // "We don't have enough credits for that.\nGet more credits now?"
            Requestor.Activate((int)RequestorTasks.BUY_CREDITS, 0, this, LocalizationManager.GetTranslation("Client Message/not_enough_credits_get_more"),
                LocalizationManager.GetTranslation("Generic Text/yes_word"), LocalizationManager.GetTranslation("Generic Text/no_word"));
        }
        else
        {
            OpenCreditsInfoDialog(true);
        }
    }

    public void AttemptFetchInfo()
    {
        // Attempt to fetch server info, if at least a minute has passed since the previous attempt.
        if ((Time.unscaledTime - prev_attempt_connect_time) >= ATTEMPT_CONNECT_MIN_INTERVAL)
        {
            Network.instance.AttemptFetchInfo();
            prev_attempt_connect_time = Time.unscaledTime;
        }
    }

    public void AttemptEnterGame()
    {
        // Attempt to enter the game, if at least the minimum interval has passed since the previous attempt.
        if ((Time.unscaledTime - prev_attempt_connect_time) >= ATTEMPT_CONNECT_MIN_INTERVAL)
        {
            Network.instance.AttemptEnterGame();
            prev_attempt_connect_time = Time.unscaledTime;
        }
    }

    public void DeactivateGamePanel()
    {
        // Hide all game panels
        nationPanelObject.SetActive(false);
        questsPanelObject.SetActive(false);
        advancesPanelObject.SetActive(false);
        messagesPanelObject.SetActive(false);
        connectPanelObject.SetActive(false);
        optionsPanelObject.SetActive(false);
        mapPanelObject.SetActive(false);
        tournamentPanelObject.SetActive(false);
        raidPanelObject.SetActive(false);
        adminPanelObject.SetActive(false);
        moderatorPanelObject.SetActive(false);
        gamePanelBaseObject.SetActive(false);

        // Record that no game panel is open.
        active_game_panel = GamePanel.GAME_PANEL_NONE;

        // Update the state of the game panel buttons.
        UpdateGamePanelButtons();
    }

    public void CloseActiveGamePanel()
    {
        // Close any active game panel.
        SetActiveGamePanel(GameGUI.GamePanel.GAME_PANEL_NONE);
    }

    public void SetActiveGamePanel(GamePanel _panel)
    {
        if (active_game_panel == GamePanel.GAME_PANEL_MESSAGES) {
            MessagesPanel.instance.ClosingPanel();
        }

        if (active_game_panel == GamePanel.GAME_PANEL_MAP) {
            MapPanel.instance.ClosingPanel();
        }

//        // Activate the game panel base, if appropriate.
//        gamePanelBaseObject.SetActive(_panel != GamePanel.GAME_PANEL_NONE);

        if (_panel == GamePanel.GAME_PANEL_NONE)
        {
            if (active_game_panel != GamePanel.GAME_PANEL_NONE) {
                gamePanelBaseObject.GetComponent<GUITransition>().StartTransition(1, 0, 1, 1, true);
            }
        }
        else
        {
            // Start by deactivating the game panel base, so that it will be activated below, for the ScalePanel script to run.
            gamePanelBaseObject.SetActive(false);

            // TESTING
            if (_panel == GamePanel.GAME_PANEL_ADVANCES) Debug.Log("Open Advances Panel place 0");

            // Show only the appropriate game panel
            nationPanelObject.SetActive(_panel == GamePanel.GAME_PANEL_NATION);
            questsPanelObject.SetActive(_panel == GamePanel.GAME_PANEL_QUESTS);
            raidPanelObject.SetActive(_panel == GamePanel.GAME_PANEL_RAID);
            advancesPanelObject.SetActive(_panel == GamePanel.GAME_PANEL_ADVANCES);
            messagesPanelObject.SetActive(_panel == GamePanel.GAME_PANEL_MESSAGES);
            connectPanelObject.SetActive(_panel == GamePanel.GAME_PANEL_CONNECT);
            optionsPanelObject.SetActive(_panel == GamePanel.GAME_PANEL_OPTIONS);
            mapPanelObject.SetActive(_panel == GamePanel.GAME_PANEL_MAP);
            tournamentPanelObject.SetActive(_panel == GamePanel.GAME_PANEL_TOURNAMENT);
            raidPanelObject.SetActive(_panel == GamePanel.GAME_PANEL_RAID);
            adminPanelObject.SetActive(_panel == GamePanel.GAME_PANEL_ADMIN);
            moderatorPanelObject.SetActive(_panel == GamePanel.GAME_PANEL_MODERATOR);

            // Show the game panel base
            gamePanelBaseObject.SetActive(true);

            if (active_game_panel == GamePanel.GAME_PANEL_NONE) {
                gamePanelBaseObject.GetComponent<GUITransition>().StartTransition(0, 1, 1, 1, false);
            }
        }

        // Record the new active game panel.
        active_game_panel = _panel;

        // Update the state of the game panel buttons
        UpdateGamePanelButtons();

        // Turn off the alert state for the newly active game panel.
        if (active_game_panel != GamePanel.GAME_PANEL_NONE) {
            SetPanelAlertState(active_game_panel, false);
        }

        // Tell the tutorial system about the new active game panel.
        Tutorial.instance.ActiveGamePanelSet(active_game_panel);
    }

    private void UpdateGamePanelButtons()
    {
        nationButtonLeft.GetComponent<PanelButton>().SetPanelIsActive(active_game_panel == GamePanel.GAME_PANEL_NATION);
        questsButtonLeft.GetComponent<PanelButton>().SetPanelIsActive(active_game_panel == GamePanel.GAME_PANEL_QUESTS);
        raidLogButtonLeft.GetComponent<PanelButton>().SetPanelIsActive(active_game_panel == GamePanel.GAME_PANEL_RAID);
        advancesButtonLeft.GetComponent<PanelButton>().SetPanelIsActive(active_game_panel == GamePanel.GAME_PANEL_ADVANCES);
        messagesButtonLeft.GetComponent<PanelButton>().SetPanelIsActive(active_game_panel == GamePanel.GAME_PANEL_MESSAGES);
        connectButtonLeft.GetComponent<PanelButton>().SetPanelIsActive(active_game_panel == GamePanel.GAME_PANEL_CONNECT);
        optionsButtonLeft.GetComponent<PanelButton>().SetPanelIsActive(active_game_panel == GamePanel.GAME_PANEL_OPTIONS);

        nationButtonBottom.GetComponent<PanelButton>().SetPanelIsActive(active_game_panel == GamePanel.GAME_PANEL_NATION);
        questsButtonBottom.GetComponent<PanelButton>().SetPanelIsActive(active_game_panel == GamePanel.GAME_PANEL_QUESTS);
        raidLogButtonBottom.GetComponent<PanelButton>().SetPanelIsActive(active_game_panel == GamePanel.GAME_PANEL_RAID);
        advancesButtonBottom.GetComponent<PanelButton>().SetPanelIsActive(active_game_panel == GamePanel.GAME_PANEL_ADVANCES);
        messagesButtonBottom.GetComponent<PanelButton>().SetPanelIsActive(active_game_panel == GamePanel.GAME_PANEL_MESSAGES);
        connectButtonBottom.GetComponent<PanelButton>().SetPanelIsActive(active_game_panel == GamePanel.GAME_PANEL_CONNECT);
        optionsButtonBottom.GetComponent<PanelButton>().SetPanelIsActive(active_game_panel == GamePanel.GAME_PANEL_OPTIONS);
    }

    public GamePanel GetActiveGamePanel()
    {
        return active_game_panel;
    }

    public void SetPanelAlertState(GamePanel _panel, bool _alert_state)
    {
        // If told to turn on alert state for the game panel that is currently active, do nothing.
        if (_alert_state && (active_game_panel == _panel)) {
            return;
        }

        switch (_panel)
        {
            case GamePanel.GAME_PANEL_NATION:
                nationButtonLeft.SetAlertState(_alert_state);
                nationButtonBottom.SetAlertState(_alert_state);
                break;
            case GamePanel.GAME_PANEL_QUESTS:
                questsButtonLeft.SetAlertState(_alert_state);
                questsButtonBottom.SetAlertState(_alert_state);
                break;
            case GamePanel.GAME_PANEL_RAID:
                raidLogButtonLeft.SetAlertState(_alert_state);
                raidLogButtonBottom.SetAlertState(_alert_state);
                break;
            case GamePanel.GAME_PANEL_ADVANCES:
                advancesButtonLeft.SetAlertState(_alert_state);
                advancesButtonBottom.SetAlertState(_alert_state);
                break;
            case GamePanel.GAME_PANEL_MESSAGES:
                messagesButtonLeft.SetAlertState(_alert_state);
                messagesButtonBottom.SetAlertState(_alert_state);
                break;
            case GamePanel.GAME_PANEL_CONNECT:
                connectButtonLeft.SetAlertState(_alert_state);
                connectButtonBottom.SetAlertState(_alert_state);
                break;
            case GamePanel.GAME_PANEL_OPTIONS:
                optionsButtonLeft.SetAlertState(_alert_state);
                optionsButtonBottom.SetAlertState(_alert_state);
                break;
        }
    }

    public void SetShowMapLocationState(bool _show)
    {
        mapLocationTextObject.SetActive(_show);

        if (_show) {
            mapView.UpdateMapLocationText();
        }
    }

    public void DisplayProgress(int _tasks_remaining)
    {
        if (_tasks_remaining > 0)
        {
            if (!progress_displayed)
            {
                progressDisplay.SetActive(true);
                progressDisplay.GetComponent<RectTransform>().anchoredPosition = new Vector2(mainUILeftWidth + ((canvasRectTransform.rect.width - mainUILeftWidth) / 2), mainUIBottomHeight + chatSystem.GetOpaqueChatHeight() + ((canvasRectTransform.rect.height - mainUIBottomHeight - chatSystem.GetOpaqueChatHeight()) / 2));
                progress_displayed = true;
            }

            if (_tasks_remaining > progress_max_num_tasks) {
                progress_max_num_tasks = _tasks_remaining;
            }
        }
        else
        {
            if (progress_displayed)
            {
                progressDisplay.SetActive(false);
                progress_displayed = false;
            }

            progress_max_num_tasks = 0;
        }

        progress_cur_num_tasks = _tasks_remaining;

        if (progress_max_num_tasks > 0) {
            progressDisplay.transform.GetChild(0).GetComponent<Image>().fillAmount = ((float)(progress_max_num_tasks - progress_cur_num_tasks + 1) / (float)progress_max_num_tasks);
        }
    }

    public void ForceLandscape(bool _active)
    {
        Debug.Log("ForceLandscape: " + _active);

        Screen.autorotateToLandscapeLeft = true;
        Screen.autorotateToLandscapeRight = true;

        if (_active)
        {
            Screen.autorotateToPortrait = false;
            Screen.autorotateToPortraitUpsideDown = false;

            if (Screen.height > Screen.width) {
                Screen.orientation = ScreenOrientation.LandscapeLeft;
            } else {
                Screen.orientation = ScreenOrientation.AutoRotation;
            }
        }
        else
        {
            Screen.autorotateToPortrait = true;
            Screen.autorotateToPortraitUpsideDown = true;
            Screen.orientation = ScreenOrientation.AutoRotation;
        }
    }

    public void LogToChat(string _log_text)
    {
        chatSystem.ChatMessageReceived(-1, -1, -1, "", "", 0, Chat.instance.GetChatChannelID(), "", _log_text, _log_text, 0);
    }

    public string GetBonusText(TechData.Bonus _bonus_type, int _bonus_val, int _bonus_val_max, float _position, bool _create_link, LinkManager _link_manager)
    {
        string open_link = _create_link ? ("<link=\"" + _link_manager.GetNumLinks() + "\"><u>") : "";
        string close_link = _create_link ? "</u></link>" : "";

        if (_create_link) {
            _link_manager.AddLink(LinkManager.LinkType.STAT, (int)_bonus_type);
        }

        const string color_tag_inc = "<color=#00ff00ff>"; // <color=lime>

        // Debug.Log("_position: " + _position + ", _bonus_val: " + _bonus_val + ", _bonus_val_max: " + _bonus_val_max);

        // Determine bonus value text. If _position is given, base value on position. Otherwise give range.
        string bonus_val_text;
        if (_position == -1)
        {
            bonus_val_text = string.Format("{0:n0}", _bonus_val);
            if (_bonus_val_max > 0) {
                bonus_val_text += "-" + _bonus_val_max;
            }
        }
        else
        {
            if (_bonus_val_max > 0) {
                _bonus_val += (int)(((_bonus_val_max - _bonus_val) * _position));
            }
            bonus_val_text = string.Format("{0:n0}", _bonus_val);
        }

        if (((_bonus_type == TechData.Bonus.STRUCTURE_DISCOUNT) || (_bonus_type == TechData.Bonus.WALL_DISCOUNT)) && (_bonus_val > 0))
        {
            bonus_val_text = "-" + bonus_val_text;
        }
        else if ((_bonus_type == TechData.Bonus.STRUCTURE_DISCOUNT) || (_bonus_type == TechData.Bonus.WALL_DISCOUNT) || (_bonus_val > 0))
        {
            bonus_val_text = "+" + bonus_val_text;
        }

        switch (_bonus_type)
        {
            case TechData.Bonus.ATTACK_MANPOWER: return color_tag_inc + bonus_val_text + "</color> " + open_link + LocalizationManager.GetTranslation("bonus_text_manpower_per_attack") + close_link;
            case TechData.Bonus.BIO: return color_tag_inc + bonus_val_text + "</color> " + open_link + LocalizationManager.GetTranslation("bonus_text_bio") + close_link;
            case TechData.Bonus.CRIT_CHANCE: return color_tag_inc + bonus_val_text + "%</color> " + open_link + LocalizationManager.GetTranslation("bonus_text_critical_hit") + close_link;
            case TechData.Bonus.ENERGY_MAX: return color_tag_inc + bonus_val_text + "</color> " + open_link + LocalizationManager.GetTranslation("bonus_text_max_energy") + close_link;
            case TechData.Bonus.ENERGY_RATE: return color_tag_inc + bonus_val_text + "</color> " + open_link + LocalizationManager.GetTranslation("bonus_text_energy_rate") + close_link;
            case TechData.Bonus.GEO_EFFICIENCY: return color_tag_inc + bonus_val_text + "%</color> " + open_link + LocalizationManager.GetTranslation("bonus_text_geographic_efficiency") + close_link;
            case TechData.Bonus.HP_PER_SQUARE: return color_tag_inc + bonus_val_text + "</color> " + open_link + LocalizationManager.GetTranslation("bonus_text_hit_points_per_square") + close_link;
            case TechData.Bonus.HP_RESTORE: return color_tag_inc + bonus_val_text + "</color> " + open_link + LocalizationManager.GetTranslation("bonus_text_restore_hit_points") + close_link;
            case TechData.Bonus.INVISIBILITY: return open_link + LocalizationManager.GetTranslation("bonus_text_structures_invisible_to_others") + close_link;
            case TechData.Bonus.MANPOWER_MAX: return color_tag_inc + bonus_val_text + "</color> " + open_link + LocalizationManager.GetTranslation("bonus_text_max_manpower") + close_link;
            case TechData.Bonus.MANPOWER_RATE: return color_tag_inc + bonus_val_text + "</color> " + open_link + LocalizationManager.GetTranslation("bonus_text_manpower_rate") + close_link;
            case TechData.Bonus.MAX_ALLIANCES: return color_tag_inc + bonus_val_text + "</color> " + open_link + LocalizationManager.GetTranslation("bonus_text_max_alliances") + close_link;
            case TechData.Bonus.PSI: return color_tag_inc + bonus_val_text + "</color> " + open_link + LocalizationManager.GetTranslation("bonus_text_psi") + close_link;
            case TechData.Bonus.SALVAGE_VALUE: return color_tag_inc + bonus_val_text + "%</color> " + open_link + LocalizationManager.GetTranslation("bonus_text_salvage_value") + close_link;
            case TechData.Bonus.SIMULTANEOUS_ACTIONS: return color_tag_inc + bonus_val_text + "</color> " + open_link + LocalizationManager.GetTranslation("bonus_text_simultaneous_actions") + close_link;
            case TechData.Bonus.SPLASH_DAMAGE: return color_tag_inc + bonus_val_text + "%</color> " + open_link + LocalizationManager.GetTranslation("bonus_text_splash_damage") + close_link;
            case TechData.Bonus.STRUCTURE_DISCOUNT: return color_tag_inc + bonus_val_text + "%</color> " + open_link + LocalizationManager.GetTranslation("bonus_text_structure_cost") + close_link;
            case TechData.Bonus.TECH: return color_tag_inc + bonus_val_text + "</color> " + open_link + LocalizationManager.GetTranslation("bonus_text_tech") + close_link;
            case TechData.Bonus.WALL_DISCOUNT: return color_tag_inc + bonus_val_text + "%</color> " + open_link + LocalizationManager.GetTranslation("bonus_text_wall_cost") + close_link;
            case TechData.Bonus.XP_MULTIPLIER: return color_tag_inc + bonus_val_text + "%</color> " + open_link + LocalizationManager.GetTranslation("Generic Text/xp_word") + close_link;
            case TechData.Bonus.TECH_MULT: return color_tag_inc + bonus_val_text + "%</color> " + open_link + LocalizationManager.GetTranslation("bonus_text_tech") + close_link;
            case TechData.Bonus.BIO_MULT: return color_tag_inc + bonus_val_text + "%</color> " + open_link + LocalizationManager.GetTranslation("bonus_text_bio") + close_link;
            case TechData.Bonus.PSI_MULT: return color_tag_inc + bonus_val_text + "%</color> " + open_link + LocalizationManager.GetTranslation("bonus_text_psi") + close_link;
            case TechData.Bonus.MANPOWER_RATE_MULT: return color_tag_inc + bonus_val_text + "%</color> " + open_link + LocalizationManager.GetTranslation("bonus_text_manpower_rate") + close_link;
            case TechData.Bonus.MANPOWER_MAX_MULT: return color_tag_inc + bonus_val_text + "%</color> " + open_link + LocalizationManager.GetTranslation("bonus_text_max_manpower") + close_link;
            case TechData.Bonus.ENERGY_RATE_MULT: return color_tag_inc + bonus_val_text + "%</color> " + open_link + LocalizationManager.GetTranslation("bonus_text_energy_rate") + close_link;
            case TechData.Bonus.ENERGY_MAX_MULT: return color_tag_inc + bonus_val_text + "%</color> " + open_link + LocalizationManager.GetTranslation("bonus_text_max_energy") + close_link;
            case TechData.Bonus.HP_PER_SQUARE_MULT: return color_tag_inc + bonus_val_text + "%</color> " + open_link + LocalizationManager.GetTranslation("bonus_text_hit_points_per_square") + close_link;
            case TechData.Bonus.HP_RESTORE_MULT: return color_tag_inc + bonus_val_text + "%</color> " + open_link + LocalizationManager.GetTranslation("bonus_text_restore_hit_points") + close_link;
            case TechData.Bonus.ATTACK_MANPOWER_MULT: return color_tag_inc + bonus_val_text + "%</color> " + open_link + LocalizationManager.GetTranslation("bonus_text_manpower_per_attack") + close_link;
            case TechData.Bonus.CREDITS: return color_tag_inc + bonus_val_text + "</color> " + LocalizationManager.GetTranslation("bonus_text_credits");
            case TechData.Bonus.INSURGENCY: return open_link + LocalizationManager.GetTranslation("bonus_text_insurgency") + close_link;
            case TechData.Bonus.TOTAL_DEFENSE: return open_link + LocalizationManager.GetTranslation("bonus_text_total_defense") + close_link;
            default: return LocalizationManager.GetTranslation("bonus_text_unknown_bonus_type") + ": " + _bonus_type;
        }
    }

    public string GetBonusIconText(TechData.Bonus _bonus_type, int _bonus_val, int _bonus_val_max, float _position, bool _create_link, LinkManager _link_manager)
    {
        string open_link = _create_link ? ("<link=\"" + _link_manager.GetNumLinks() + "\"><u>") : "";
        string close_link = _create_link ? "</u></link>" : "";

        if (_create_link) {
            _link_manager.AddLink(LinkManager.LinkType.STAT, (int)_bonus_type);
        }

        // Debug.Log("_position: " + _position + ", _bonus_val: " + _bonus_val + ", _bonus_val_max: " + _bonus_val_max);

        // Determine bonus value text. If _position is given, base value on position. Otherwise give range.
        string bonus_val_text;
        if (_position == -1)
        {
            bonus_val_text = string.Format("{0:n0}", _bonus_val);
            if (_bonus_val_max > 0) {
                bonus_val_text += "-" + _bonus_val_max;
            }
        }
        else
        {
            if (_bonus_val_max > 0) {
                _bonus_val += (int)(((_bonus_val_max - _bonus_val) * _position));
            }
            bonus_val_text = string.Format("{0:n0}", _bonus_val);
        }

        if (((_bonus_type == TechData.Bonus.STRUCTURE_DISCOUNT) || (_bonus_type == TechData.Bonus.WALL_DISCOUNT)) && (_bonus_val > 0))
        {
            bonus_val_text = "-" + bonus_val_text;
        }
        else if ((_bonus_type == TechData.Bonus.STRUCTURE_DISCOUNT) || (_bonus_type == TechData.Bonus.WALL_DISCOUNT) || (_bonus_val > 0))
        {
            bonus_val_text = "+" + bonus_val_text;
        }

        switch (_bonus_type)
        {
            case TechData.Bonus.ATTACK_MANPOWER: return bonus_val_text + " " + open_link + "<sprite=21>" + close_link;
            case TechData.Bonus.BIO: return bonus_val_text + " " + open_link + "<sprite=4>" + close_link;
            case TechData.Bonus.CRIT_CHANCE: return bonus_val_text + "% " + open_link + "<sprite=12>" + close_link;
            case TechData.Bonus.ENERGY_MAX: return bonus_val_text + " " + open_link + "<sprite=1><sprite=16>" + close_link;
            case TechData.Bonus.ENERGY_RATE: return bonus_val_text + " " + open_link + "<sprite=1><sprite=15>" + close_link;
            case TechData.Bonus.GEO_EFFICIENCY: return bonus_val_text + "% " + open_link + "<sprite=17>" + close_link;
            case TechData.Bonus.HP_PER_SQUARE: return bonus_val_text + " " + open_link + "<sprite=18><sprite=16>" + close_link;
            case TechData.Bonus.HP_RESTORE: return bonus_val_text + " " + open_link + "<sprite=18><sprite=15>" + close_link;
            case TechData.Bonus.INVISIBILITY: return open_link + "<sprite=7>" + close_link;
            case TechData.Bonus.MANPOWER_MAX: return bonus_val_text + " " + open_link + "<sprite=0><sprite=16>" + close_link;
            case TechData.Bonus.MANPOWER_RATE: return bonus_val_text + " " + open_link + "<sprite=0><sprite=15>" + close_link;
            case TechData.Bonus.MAX_ALLIANCES: return bonus_val_text + " " + open_link + "<sprite=8>" + close_link;
            case TechData.Bonus.PSI: return bonus_val_text + " " + open_link + "<sprite=5>" + close_link;
            case TechData.Bonus.SALVAGE_VALUE: return bonus_val_text + "% " + open_link + "<sprite=11>" + close_link;
            case TechData.Bonus.SIMULTANEOUS_ACTIONS: return bonus_val_text + " " + open_link + "<sprite=13>" + close_link;
            case TechData.Bonus.SPLASH_DAMAGE: return bonus_val_text + "% " + open_link + "<sprite=19>" + close_link;
            case TechData.Bonus.STRUCTURE_DISCOUNT: return bonus_val_text + "% " + open_link + "<sprite=9>" + close_link;
            case TechData.Bonus.TECH: return bonus_val_text + " " + open_link + "<sprite=6>" + close_link;
            case TechData.Bonus.WALL_DISCOUNT: return bonus_val_text + "% " + open_link + "<sprite=10>" + close_link;
            case TechData.Bonus.XP_MULTIPLIER: return bonus_val_text + "% " + open_link + "<sprite=14>" + close_link;
            case TechData.Bonus.TECH_MULT: return bonus_val_text + "% " + open_link + "<sprite=6>" + close_link;
            case TechData.Bonus.BIO_MULT: return bonus_val_text + "% " + open_link + "<sprite=4>" + close_link;
            case TechData.Bonus.PSI_MULT: return bonus_val_text + "% " + open_link + "<sprite=5>" + close_link;
            case TechData.Bonus.MANPOWER_RATE_MULT: return bonus_val_text + "% " + open_link + "<sprite=0><sprite=15>" + close_link;
            case TechData.Bonus.MANPOWER_MAX_MULT: return bonus_val_text + "% " + open_link + "<sprite=0><sprite=16>" + close_link;
            case TechData.Bonus.ENERGY_RATE_MULT: return bonus_val_text + "% " + open_link + "<sprite=1><sprite=15>" + close_link;
            case TechData.Bonus.ENERGY_MAX_MULT: return bonus_val_text + "% " + open_link + "<sprite=1><sprite=16>" + close_link;
            case TechData.Bonus.HP_PER_SQUARE_MULT: return bonus_val_text + "% " + open_link + "<sprite=18><sprite=16>" + close_link;
            case TechData.Bonus.HP_RESTORE_MULT: return bonus_val_text + "% " + open_link + "<sprite=18><sprite=15>" + close_link;
            case TechData.Bonus.ATTACK_MANPOWER_MULT: return bonus_val_text + "% " + open_link + "<sprite=21>" + close_link;
            case TechData.Bonus.CREDITS: return bonus_val_text + " " + "<sprite=2>";
            case TechData.Bonus.INSURGENCY: return open_link + "<sprite=28>" + close_link;
            case TechData.Bonus.TOTAL_DEFENSE: return open_link + "<sprite=28>" + close_link;
            default: return LocalizationManager.GetTranslation("bonus_text_unknown_bonus_type") + ": " + _bonus_type;
        }
    }

    public string GetCombatStatName(int _combat_stat)
    {
        switch (_combat_stat)
        {
            case GameData.STAT_TECH: return I2.Loc.LocalizationManager.GetTranslation("Stat Description/tech_title");
            case GameData.STAT_BIO: return I2.Loc.LocalizationManager.GetTranslation("Stat Description/bio_title");
            case GameData.STAT_PSI: return I2.Loc.LocalizationManager.GetTranslation("Stat Description/psi_title");
            default: return "undef";
        }
    }

    public void OnAdvanceResearched()
    {
        StartCoroutine(OnAdvanceResearched_Coroutine());
    }

    public IEnumerator OnAdvanceResearched_Coroutine()
    {
#if UNITY_ANDROID || UNITY_IOS
        // Wait a few seconds before possibly asking player to rate the app.
        yield return new WaitForSeconds(3f);

        // Determine max allowed rating requests per year
        int max_requests_per_year = (int)EM_Settings.RatingRequest.AnnualCap;

        // Determine the min period between making a request, by dividing the length of a year by the number of requests allowed per year.
        int period_between_requests = (60 * 60 * 24 * 14); //  (60 * 60 * 24 * 365) / max_requests_per_year;

        //LogToChat("Here1: max_requests_per_year: " + max_requests_per_year + ", period_between_requests: " + period_between_requests);

        // If no tutorial is active, and if the nation's level is at least 5, and if this is the first rating request, or enough time has elapsed since the previous rating request...
        if (StoreReview.CanRequestRating() && (Tutorial.instance.IsLessonActive() == false) && (GameData.instance.level >= 5) && ((PlayerPrefs.HasKey("prev_rating_request_time") == false) || ((GameData.instance.gameTimeAtLogin - PlayerPrefs.GetInt("prev_rating_request_time")) >= period_between_requests))) {
            RequestRating();
        }
#endif
		yield break;
    }

    public void RequestRating()
    {
        Requestor.Activate((int)RequestorTasks.RATING_ENJOYING, 0, this, LocalizationManager.GetTranslation("rating_enjoying"), LocalizationManager.GetTranslation("Generic Text/yes_word"), LocalizationManager.GetTranslation("Generic Text/no_word"));
    }

    public void PopulateServerDropdown(Dropdown _serverDropdown, bool _showHidden)
    {
        List<string> server_strings = new List<string>();
        string cur_string;
        int selectedIndex = 0;

        //Debug.Log("_showHidden: " + _showHidden);

        // Determine ID of server to select by default. Use previously associated server, or else default for the current language.
        int selectedServerID = PlayerPrefs.GetInt("prevServerID", -1);
        if (selectedServerID == -1) {
            selectedServerID = Network.instance.DetermineDefaultServerID(PlayerPrefs.GetString("language"));
        }

        availableServerIDs.Clear();

        for (int i = 0; i < Network.instance.numServers; i++)
        {
            if ((Network.instance.servers[i].hidden && (!_showHidden)) || (Network.instance.servers[i].redirection != -1)) {
                continue;
            }

            // Start with the server's name.
            cur_string = Network.instance.servers[i].name;

            // Add the language to the server name if a language is given.
            if (Network.instance.servers[i].language.Length > 0)
            {
                int lang_index = Array.IndexOf(languageEnglishNames, Network.instance.servers[i].language);
                if (lang_index > -1) {
                    cur_string = cur_string + " (" + languageEnglishNames[lang_index] + ")";
                }
            }

            // If this is the server that should be selected by default, record the index to select.
            if (Network.instance.servers[i].ID == selectedServerID) {
                selectedIndex = server_strings.Count;
            }

            // Add the current server name to the list.
            server_strings.Add(cur_string);

            // Add the current server ID to the availableServerIDs list.
            availableServerIDs.Add(Network.instance.servers[i].ID);
        }

        // Add all of the server name options to the dropdown menu.
        _serverDropdown.ClearOptions();
        _serverDropdown.AddOptions(server_strings);

        // Select the server with the given _selectedServerID.
        _serverDropdown.value = selectedIndex;
    }

    public void PopulateLanguageDropdown(Dropdown _languageDropdown)
    {
        _languageDropdown.ClearOptions();
        _languageDropdown.AddOptions(language_name_list);

        // Set active language
        if (PlayerPrefs.HasKey("language"))
        {
            _languageDropdown.value = language_english_name_list.IndexOf(PlayerPrefs.GetString("language"));
        }
        else
        {
            Languages lang = Languages.ENGLISH;

            switch (Application.systemLanguage)
            {
                case SystemLanguage.Chinese:
                case SystemLanguage.ChineseSimplified:
                case SystemLanguage.ChineseTraditional: lang = Languages.CHINESE; break;
                case SystemLanguage.English: lang = Languages.ENGLISH; break;
                case SystemLanguage.French: lang = Languages.FRENCH; break;
                case SystemLanguage.German: lang = Languages.GERMAN; break;
                case SystemLanguage.Italian: lang = Languages.ITALIAN; break;
                case SystemLanguage.Japanese: lang = Languages.JAPANESE; break;
                case SystemLanguage.Korean: lang = Languages.KOREAN; break;
                case SystemLanguage.Portuguese: lang = Languages.PORTUGESE; break;
                case SystemLanguage.Russian: lang = Languages.RUSSIAN; break;
                case SystemLanguage.Spanish: lang = Languages.SPANISH; break;
            }

            if (LocalizationManager.HasLanguage(language_english_name_list[(int)lang]))
            {
                _languageDropdown.value = language_list.IndexOf((int)lang);
            }
        }
    }

    public void OnChange_Language(Dropdown _languageDropdown)
    {
        if (LocalizationManager.HasLanguage(language_english_name_list[_languageDropdown.value]))
        {
            // Record the new localizaton langauge
            curLanguage = (Languages)_languageDropdown.value;

            // Set the game's localization language
            LocalizationManager.CurrentLanguage = language_english_name_list[_languageDropdown.value];

			Debug.Log("OnChange_Language() for " + ((_languageDropdown == WelcomePanel.instance.languageDropdown) ? "welcome panel dropdown" : ((_languageDropdown == OptionsPanel.instance.languageDropdown) ? "options panel dropdown" : "unknown" )) + " to " + language_english_name_list[_languageDropdown.value]);

            // Record the chosen language in PlayerPrefs
            PlayerPrefs.SetString("language", language_english_name_list[_languageDropdown.value]);

            // Update language of all data types
            BuildData.UpdateLocalization();
            ObjectData.UpdateLocalization();
            QuestData.UpdateLocalization();
            TechData.UpdateLocalization();

            // Update GUI for localization
            GameGUI.instance.UpdateForLocalization();

            // Repopulate other language dropdown

            if (_languageDropdown != WelcomePanel.instance.languageDropdown) {
                WelcomePanel.instance.PopulateLanguageDropdown();
            }

            if (_languageDropdown != OptionsPanel.instance.languageDropdown) {
                OptionsPanel.instance.PopulateLanguageDropdown();
            }
        }
    }

    public string GetLocalizedString(string _key)
    {
        string val = I2.Loc.LocalizationManager.GetTranslation(_key);

        if ((val == null) || (val.Length == 0))
        {
            Debug.Log("Localized value not found for key '" + _key + "'");
            return "";
        }

        return val;
    }

    public static string RemoveControlCharacters(string inString)
    {
        if (inString == null) return null;
        StringBuilder newString = new StringBuilder();
        char ch;
        for (int i = 0; i < inString.Length; i++)
        {
            ch = inString[i];
            if (!char.IsControl(ch))
            {
                newString.Append(ch);
            }
        }
        return newString.ToString();
    }

    public void OnClick_ManpowerBulb()
    {
        StatDetailsPanel.instance.Activate(GameData.Stat.MANPOWER_RATE);
    }

    public void OnClick_EnergyBulb()
    {
        StatDetailsPanel.instance.Activate(GameData.Stat.ENERGY_RATE);
    }

    public void OnClick_CreditsBulb()
    {
        OpenSubscribeDialog();

        /*
        if (GameData.instance.creditPurchasesAllowed) {
            OpenBuyDialog(BuyPanel.BuyType.Credits);
        } else {
            OpenCreditsInfoDialog(false);
        }
        */
    }

    public void OnClick_XPBulb()
    {
        StatDetailsPanel.instance.Activate(GameData.Stat.XP);
    }

    public void OnClick_APBulb()
    {
        SetActiveGamePanel(GamePanel.GAME_PANEL_ADVANCES);
    }

    public void OnClick_GeoBulb()
    {
        StatDetailsPanel.instance.Activate(GameData.Stat.GEOGRAPHIC_EFFICIENCY);
    }

    public void OnClick_AddManpower()
    {
        OpenBuyDialog(BuyPanel.BuyType.Manpower);
    }

    public void OnClick_AddEnergy()
    {
        // Only allow energy purchase if in mainland map.
        if (GameData.instance.mapMode == GameData.MapMode.MAINLAND) {
            OpenBuyDialog(BuyPanel.BuyType.Energy);
        }
    }

    public void OnClick_AddCredits()
    {
        OpenSubscribeDialog();

        /*
        if (GameData.instance.creditPurchasesAllowed) {
            OpenBuyDialog(BuyPanel.BuyType.Credits);
        } else {
//#if UNITY_ANDROID || UNITY_IOS
//            OpenCreditsInfoDialog(false);
//#else
			OpenEarnCreditsPage();
//#endif
        }
        */
    }

	public void OpenEarnCreditsPage()
	{
		Application.OpenURL("https://warofconquest.com/earn-credits/?u=" + GameData.instance.serverID + "," + GameData.instance.userID + "," + GameData.instance.email);
	}

    public void OnPress_CompassRose()
    {
        compassRosePressTime = Time.unscaledTime;
        StartCoroutine(Coroutine_CompassRose());
    }

    public void OnClick_CompassRose()
    {
        if ((compassRosePressTime != -1) && (mapView.IsCameraPaused() == false))
        {
            if (MapPanel.instance.mode == MapPanel.Mode.Map) {
                // Transition the map panel in, by first transitioning to a top-down view of the world.
                StartCoroutine(OpenMapPanel_Coroutine());
            } else {
                // Transition directly to the map panel without changing the view of the world.
                GameGUI.instance.SetActiveGamePanel(GamePanel.GAME_PANEL_MAP);
            }
        }

        compassRosePressTime = -1;
    }

    public IEnumerator Coroutine_CompassRose()
    {
        //Debug.Log("Coroutine_CompassRose()");
        //LogToChat("Coroutine_CompassRose() start compassRosePressTime: " + compassRosePressTime);
        while ((compassRosePressTime != -1) && ((Time.unscaledTime - compassRosePressTime) < COMPASS_ROSE_PRESS_TIME)) {
            yield return 0;
        }

        if (compassRosePressTime != -1)
        {
            compassRosePressTime = -1;

            if (mapView.IsCameraPaused() == false)
            {
                // Send message to server.
                Network.instance.SendCommand("action=next_area");
            }
        }
    }

    public IEnumerator OpenMapPanel_Coroutine()
    {
        // Open the map panel
        MapView.instance.EnterMapPanelView(MAP_PANEL_TRANSITION_DURATION);
        yield return new WaitForSeconds(MAP_PANEL_TRANSITION_DURATION);
        GameGUI.instance.SetActiveGamePanel(GamePanel.GAME_PANEL_MAP);
    }

    public void OnPress_CameraButton()
    {
        //LogToChat("OnPress_CameraButton() cameraMenu.activeSelf: " + cameraMenu.activeSelf);
        if (cameraMenu.activeSelf)
        {
            // Close the camera menu
            cameraMenu.GetComponent<GUITransition>().StartTransition(1,0,1,1,true);
        }
        else
        {
            cameraButtonPressTime = Time.unscaledTime;
            cameraButtonPressed = true;
            StartCoroutine(Coroutine_CameraButton());
        }
    }

    public void OnRelease_CameraButton()
    {
        //LogToChat("OnRelease_CameraButton()");
        cameraButtonPressed = false;
        cameraButtonPressTime = Time.unscaledTime;
        cameraMenu.GetComponent<CameraMenu>().CameraButtonReleased();
    }

    public void OnClick_CameraButton()
    {
        //LogToChat("OnClick_CameraButton() cameraMenu.activeSelf: " + cameraMenu.activeSelf + ", cameraButtonPressTime: " + cameraButtonPressTime);
        if ((cameraButtonPressTime != -1) && (cameraMenu.activeSelf == false))
        {
            if (cameraType == CameraType.SCREENSHOT)
            {
                Debug.Log("Screenshot button pressed");

                // Capture a screenshot an open share panel
                StartCoroutine(CaptureScreenshot());
            }
            else if (cameraType == CameraType.VIDEO)
            {
                Debug.Log("Video button pressed");

                /*if (Everyplay.IsRecording())
                {
                    // Set Everyplay recording metadata
                    Everyplay.SetMetadata("nation", GameData.instance.nationName);
                    Everyplay.SetMetadata("player", GameData.instance.username);
                    Everyplay.SetMetadata("patron_code", GameData.instance.patronCode);

                    Everyplay.StopRecording();

                    Debug.Log("About to show Everplay sharing modal");

                    Everyplay.ShowSharingModal();
                }
                else*/
#if UNITY_IOS || UNITY_ANDROID
                if (Gif.IsRecording(gifRecorder))
                {
                    // Offer to share video that has been recorded
                    AnimatedClip clip = Gif.StopRecording(gifRecorder);
                    Debug.Log("Gif clip: " + clip + ", w: " + clip.Width + " h: " + clip.Height);

                    // Open the share panel
                    OpenSharePanel();

                    // Initialize the share panel
                    SharePanel.instance.InitForGifClip(clip);

                    // Restart recording video
                    Gif.StartRecording(gifRecorder);
                }
#endif
            }

            //LogToChat("OnClick_CameraButton() resetting cameraButtonPressTime to -1");
            cameraButtonPressTime = -1;
        }
    }

    IEnumerator CaptureScreenshot()
    {
        // Wait until the end of frame
        yield return new WaitForEndOfFrame();

#if UNITY_IOS || UNITY_ANDROID
        // Create a Texture2D object of the screenshot using the CaptureScreenshot() method
        Texture2D texture = Sharing.CaptureScreenshot();

        // Initialize the share panel
        SharePanel.instance.InitForTexture2D(texture);

        // Open the share panel
        OpenSharePanel();
#endif
    }

    public void OnPress_CameraScreenshotToggle()
    {
        // Set new camera type
        cameraType = CameraType.SCREENSHOT;
        cameraButtonImage.sprite = cameraButtonScreenshotIcon;

        // Record the video flag value, and send flags to the server.
        GameData.instance.SetUserFlag(GameData.UserFlags.RECORD_VIDEO, false);
        if (!updatingUIElement) GameData.instance.SendUserFlags();

        // Close the camera menu
        CloseCameraMenu();

        // Stop video recording
        StopVideoRecording();
    }

    public void OnPress_CameraVideoToggle()
    {
        // Set new camera type
        cameraType = CameraType.VIDEO;
        cameraButtonImage.sprite = cameraButtonVideoIcon;

        // Record the video flag value, and send flags to the server.
        GameData.instance.SetUserFlag(GameData.UserFlags.RECORD_VIDEO, true);
        if (!updatingUIElement) GameData.instance.SendUserFlags();

        // Close the camera menu
        CloseCameraMenu();

        // Start recoding of video
        ResetVideoRecording();
    }

    public void StopVideoRecording()
    {
#if UNITY_IOS || UNITY_ANDROID
        // Make sure Easy Mobile Gif recording is disabled
        if (Gif.IsRecording(gifRecorder))
        {
            Gif.StopRecording(gifRecorder);
            gifRecorder.enabled = false;
        }
#endif

        //// Make sure Everyplay recording is disabled
        //if (Everyplay.FaceCamIsSessionRunning()) Everyplay.FaceCamStopSession();
        //if (Everyplay.IsRecording()) Everyplay.StopRecording();
    }

    public void ResetVideoRecording()
    {
        // Do not start video recording if not in video recording mode.
        if (cameraType != CameraType.VIDEO) {
            return;
        }

        // Stop any video recording already in progress
        StopVideoRecording();

        StartCoroutine(ResetVideoRecording_Coroutine());
    }

    public IEnumerator ResetVideoRecording_Coroutine()
    {
        /*
        Debug.Log("Everplay: IsRecordingSupported(): " + Everyplay.IsRecordingSupported() + ", everyplayReady: " + everyplayReady + ", IsReadyForRecording(): " + Everyplay.IsReadyForRecording() + ", EVERYPLAY flag: " + GameData.instance.GetUserFlag(GameData.UserFlags.EVERYPLAY));

        if (Everyplay.IsRecordingSupported() && everyplayReady && Everyplay.IsReadyForRecording() && GameData.instance.GetUserFlag(GameData.UserFlags.EVERYPLAY))
        {
            if (GameData.instance.GetUserFlag(GameData.UserFlags.SHOW_FACE))
            {
                // If we do not have recording permission, requst it.
                if (Everyplay.FaceCamIsRecordingPermissionGranted() == false)
                {
                    // Request recording permission
                    Everyplay.FaceCamRequestRecordingPermission();

                    // Wait up to 10 seconds for recording permission to be granted.
                    float start_time = Time.unscaledTime;
                    while ((Everyplay.FaceCamIsRecordingPermissionGranted() == false) && (Time.unscaledTime <= (start_time + 10f))) {
                        yield return new WaitForSeconds(0.5f);
                    }
                }

                if (Everyplay.FaceCamIsRecordingPermissionGranted())
                {
                    // Set up the Facecam preview
                    Everyplay.FaceCamSetPreviewVisible(true);
                    PositionEveryplayFaceCam();

                    // Start FaceCam recording session
                    Everyplay.FaceCamStartSession();
                }
            }
            else if (Everyplay.FaceCamIsSessionRunning())
            {
                // Stop FaceCam recording session
                Everyplay.FaceCamStopSession();
            }

            // Make sure Easy Mobile Gif recorder is disabled
            if (Gif.IsRecording(gifRecorder)) Gif.StopRecording(gifRecorder);
            gifRecorder.enabled = false;

            // Start Everyplay video recording
            Everyplay.StartRecording();
        }
        else*/
        {
            //// Make sure Everyplay recording is disabled
            //if (Everyplay.FaceCamIsSessionRunning()) Everyplay.FaceCamStopSession();
            //if (Everyplay.IsRecording()) Everyplay.StopRecording();
#if UNITY_IOS || UNITY_ANDROID
            // Start Easy Mobile video recording
            gifRecorder.enabled = true;
            Gif.StartRecording(gifRecorder);
#endif
        }

        yield return null; // Wait one frame, since this is a co-routine (becase of the obsolete Everyplay code).
    }
    /*
    public void PositionEveryplayFaceCam()
    {
        Everyplay.FaceCamSetPreviewOrigin(Everyplay.FaceCamPreviewOrigin.BottomRight);
        Everyplay.FaceCamSetPreviewSideWidth((int)(Mathf.Sqrt((Screen.height * Screen.height) + (Screen.width * Screen.width)) * 0.10f));
        Everyplay.FaceCamSetPreviewPositionX((int)(Screen.width * 0.02f));
        Everyplay.FaceCamSetPreviewPositionY((int)(Screen.height * 0.16f));
    }
    */
    public IEnumerator Coroutine_CameraButton()
    {
        //LogToChat("Coroutine_CameraButton() start cameraButtonPressTime: " + cameraButtonPressTime);
        while ((cameraButtonPressTime != -1) && ((Time.unscaledTime - cameraButtonPressTime) < CAMERA_MENU_OPEN_TIME)) {
            yield return 0;
        }

        if (cameraButtonPressTime != -1)
        {
            // Open the camera menu
            //LogToChat("Coroutine_CameraButton() cameraButtonPressTime: " + cameraButtonPressTime + ", opening menu.");
            cameraMenu.SetActive(true);
            cameraMenuScreenshotToggle.isOn = (cameraType == CameraType.SCREENSHOT);
            cameraMenuVideoToggle.isOn = (cameraType == CameraType.VIDEO);
            cameraMenu.GetComponent<GUITransition>().StartTransition(0,1,1,1,false);
        }

        while ((cameraButtonPressTime != -1) && (cameraButtonPressed || ((Time.unscaledTime - cameraButtonPressTime) < CAMERA_MENU_CLOSE_TIME))) {
            yield return 0;
        }

        //Debug.Log("cameraButtonPressTime: " + cameraButtonPressTime + ", cameraButtonPressed: " + cameraButtonPressed + ", time since press: " + (Time.unscaledTime - cameraButtonPressTime));

        if (cameraButtonPressTime != -1)
        {
            // Close the camera menu
            CloseCameraMenu();
        }
    }

    public void CloseCameraMenu()
    {
        //LogToChat("CloseCameraMenu() called");
        cameraMenu.GetComponent<GUITransition>().StartTransition(1,0,1,1,true);
        cameraButtonPressTime = -1;
    }
    /*
    public void OnEveryplayReadyForRecording(bool enabled)
    {
        // Record whether Everplay is ready.
        everyplayReady = enabled;
    }

    public void OnEverplayWasClosed()
    {
        // Restart Everyplay video recording
        Everyplay.StartRecording();
    }
    */
    public void OnClick_Nation()
    {
        // Activate the Options game panel, or close it if it's already active.
        SetActiveGamePanel((active_game_panel == GamePanel.GAME_PANEL_NATION) ? GamePanel.GAME_PANEL_NONE : GamePanel.GAME_PANEL_NATION);
    }

    public void OnClick_Quests()
    {
        // Activate the Options game panel, or close it if it's already active.
        SetActiveGamePanel((active_game_panel == GamePanel.GAME_PANEL_QUESTS) ? GamePanel.GAME_PANEL_NONE : GamePanel.GAME_PANEL_QUESTS);
    }

    public void OnClick_RaidLog()
    {
        // Activate the Raid Log game panel, or close it if it's already active.
        SetActiveGamePanel((active_game_panel == GamePanel.GAME_PANEL_RAID) ? GamePanel.GAME_PANEL_NONE : GamePanel.GAME_PANEL_RAID);
    }

    public void OnClick_Advances()
    {
        // Activate the Options game panel, or close it if it's already active.
        SetActiveGamePanel((active_game_panel == GamePanel.GAME_PANEL_ADVANCES) ? GamePanel.GAME_PANEL_NONE : GamePanel.GAME_PANEL_ADVANCES);
    }

    public void OnClick_Messages()
    {
        // Activate the Options game panel, or close it if it's already active.
        SetActiveGamePanel((active_game_panel == GamePanel.GAME_PANEL_MESSAGES) ? GamePanel.GAME_PANEL_NONE : GamePanel.GAME_PANEL_MESSAGES);
    }

    public void OnClick_Info()
    {
        // Activate the Connect game panel, or close it if it's already active.
        SetActiveGamePanel((active_game_panel == GamePanel.GAME_PANEL_CONNECT) ? GamePanel.GAME_PANEL_NONE : GamePanel.GAME_PANEL_CONNECT);
    }

    public void OnClick_Options()
    {
        // Activate the Options game panel, or close it if it's already active.
        SetActiveGamePanel((active_game_panel == GamePanel.GAME_PANEL_OPTIONS) ? GamePanel.GAME_PANEL_NONE : GamePanel.GAME_PANEL_OPTIONS);
    }

    public void OnClick_Tournament()
    {
        // Activate the Tournament game panel, or close it if it's already active.
        SetActiveGamePanel((active_game_panel == GamePanel.GAME_PANEL_TOURNAMENT) ? GamePanel.GAME_PANEL_NONE : GamePanel.GAME_PANEL_TOURNAMENT);
    }

    public void OnClick_LowGeoIndictor()
    {
        // Our geographic efficiency is only {[geo_eff]}%, so {[inverse_geo_eff]}% of any defenses we've built have become inert and useless, and we only receive {[geo_eff]}% of the benefits of any resources we hold. Our nation will have better geographic efficiency if we have less border and more interior area -- that is, if we're made up of fewer large areas, rather than many small areas. {[supportable_area_info]}
        string pms_low_geo_indicator_text = LocalizationManager.GetTranslation("pms_low_geo_indicator_text");

        // We should also keep our nation's area, which is now {[area]}, below {[supportable_area]}, the maximum we can support without our geographic efficiency being compromised.
        string pms_supportable_area_info = (GameData.instance.current_footprint.area > GameData.instance.GetSupportableArea()) ? LocalizationManager.GetTranslation("pms_supportable_area_info_text") : "";

        pms_low_geo_indicator_text = pms_low_geo_indicator_text.Replace("{[geo_eff]}", string.Format("{0:n0}", Mathf.RoundToInt(GameData.instance.GetFinalGeoEfficiency() * 100)));
        pms_low_geo_indicator_text = pms_low_geo_indicator_text.Replace("{[inverse_geo_eff]}", string.Format("{0:n0}", Mathf.RoundToInt((1f - GameData.instance.GetFinalGeoEfficiency()) * 100)));
        pms_low_geo_indicator_text = pms_low_geo_indicator_text.Replace("{[supportable_area_info]}", string.Format("{0:n0}", pms_supportable_area_info));

        Requestor.Activate(0, 0, null, pms_low_geo_indicator_text, LocalizationManager.GetTranslation("okay_button_label"), "", Requestor.FLAG_ALIGN_LEFT);
    }

    public void OnClick_ManpowerBurnIndicator()
    {
        // We're losing {[manpower_burn_rate]} manpower per hour because we're holding excess resources that raise our stats beyond the +{[resource_cap_percent]}% cap that we can support. That's {[manpower_burn_percent]}% of the manpower we generate. To stop losing that manpower we would need to evacuate some resources. See the Nation > Resources panel to decide which ones.
        string pms_manpower_burn_indicator_text = LocalizationManager.GetTranslation("pms_manpower_burn_indicator_text");

        pms_manpower_burn_indicator_text = pms_manpower_burn_indicator_text.Replace("{[manpower_burn_rate]}", string.Format("{0:n0}", GameData.instance.manpowerBurnRate));
        pms_manpower_burn_indicator_text = pms_manpower_burn_indicator_text.Replace("{[resource_cap_percent]}", string.Format("{0:n0}", GameData.instance.resourceBonusCap * 100f));
        pms_manpower_burn_indicator_text = pms_manpower_burn_indicator_text.Replace("{[manpower_burn_percent]}", string.Format("{0:n0}", Mathf.RoundToInt(GameData.instance.manpowerBurnRate / GameData.instance.GetMainlandManpowerRate() * 100f)));

        Requestor.Activate(0, 0, null, pms_manpower_burn_indicator_text, LocalizationManager.GetTranslation("okay_button_label"), "", Requestor.FLAG_ALIGN_LEFT);
    }

    public void OnClick_EnergyDecreaseIndicator()
    {
        int seconds_until_inert = (int)((float)GameData.instance.energy / ((GameData.instance.GetFinalEnergyBurnRate() - GameData.instance.GetFinalEnergyRate()) / 3600f));

        string pms_energy_decrease_indicator_text = LocalizationManager.GetTranslation("pms_energy_decrease_indicator_text"); // "We are burning more energy than we are generating..."

        pms_energy_decrease_indicator_text = pms_energy_decrease_indicator_text.Replace("{[energy_burn_rate]}", GameData.instance.GetStatValueString(GameData.Stat.FINAL_ENERGY_BURN_RATE));
        pms_energy_decrease_indicator_text = pms_energy_decrease_indicator_text.Replace("{[energy_gain_rate]}", string.Format("{0:n0}", GameData.instance.GetFinalEnergyRate()));
        pms_energy_decrease_indicator_text = pms_energy_decrease_indicator_text.Replace("{[energy_out_duration]}", GameData.instance.GetDurationText(seconds_until_inert));

        Requestor.Activate(0, 0, null, pms_energy_decrease_indicator_text, LocalizationManager.GetTranslation("okay_button_label"), "", Requestor.FLAG_ALIGN_LEFT);
    }

    public void OnClick_EnergyInertIndicator()
    {
        int percent_inert = (int)(100 * (1f - Mathf.Min(GameData.instance.GetFinalGeoEfficiency(), (float)GameData.instance.GetFinalEnergyRate() / (float)GameData.instance.GetFinalEnergyBurnRate())));

        string pms_inert_indicator_text = LocalizationManager.GetTranslation("pms_inert_indicator_text"); // "We are burning more energy than we are generating..."

        pms_inert_indicator_text = pms_inert_indicator_text.Replace("{[energy_burn_rate]}", GameData.instance.GetStatValueString(GameData.Stat.FINAL_ENERGY_BURN_RATE));
        pms_inert_indicator_text = pms_inert_indicator_text.Replace("{[energy_gain_rate]}", string.Format("{0:n0}", GameData.instance.GetFinalEnergyRate()));
        pms_inert_indicator_text = pms_inert_indicator_text.Replace("{[percent_inert]}", percent_inert.ToString());

        Requestor.Activate(0, 0, null, pms_inert_indicator_text, LocalizationManager.GetTranslation("okay_button_label"), "", Requestor.FLAG_ALIGN_LEFT);
    }

    // Implement a function for showing a rewarded video ad:
    public void ShowRewardedVideo() {
        Advertisement.Show("rewardedVideo");
    }

    // Implement IUnityAdsListener interface methods:
    public void OnUnityAdsDidFinish (string placementId, ShowResult showResult) {
        Debug.Log("OnUnityAdsDidFinish() called, with placementId " + placementId + " and showResult " + showResult);
        //LogToChat("OnUnityAdsDidFinish() called, with placementId " + placementId + " and showResult " + showResult);
        // Define conditional logic for each ad completion status:
        if (showResult == ShowResult.Finished) {
            // Set flag to handle the ad having finished (because handling it here, using the thread that calls this callback, causes problems on Android.)
            adFinished = true;
            Debug.Log("Ad watched to completion.");
        } else if (showResult == ShowResult.Skipped) {
            // Do not reward the user for skipping the ad.
            Debug.Log("Ad skipped.");
        } else if (showResult == ShowResult.Failed) {
            Debug.Log("The ad did not finish due to an error.");
        }
    }

    public void OnUnityAdsReady (string placementId) {
        /// If the ready Placement is rewarded, show the ad:
        //if (placementId == "rewardedVideo") {
        //    Advertisement.Show ("rewardedVideo");
        //}
    }

    public void OnUnityAdsDidError (string message) {
        // Log the error.
    }

    public void OnUnityAdsDidStart (string placementId) {
        // Optional actions to take when the end-users triggers an ad.
    }

    public float GetMainUILeftWidth()
    {
        return mainUILeftWidth;
    }

    public float GetMainUIBottomHeight()
    {
        return mainUIBottomHeight;
    }

    public void RequestorResponse(int _task, int _data, Requestor.RequestorButton _result)
    {
        if (_task == (int)RequestorTasks.UPDATE_CLIENT)
        {
            // Respond to new client download request.
            if (_result == Requestor.RequestorButton.LeftButton) {
                Application.OpenURL("https://warofconquest.com/download/");
            }

            // Exit the game client
            ExitGame();
        }
        else if (_task == (int)RequestorTasks.UPDATE_CLIENT_STEAM)
        {
            // Exit the game client
            ExitGame();
        }
        else if (_task == (int)RequestorTasks.BUY_CREDITS)
        {
            if (_result == Requestor.RequestorButton.LeftButton) {
                OpenSubscribeDialog();
                //OpenBuyDialog(BuyPanel.BuyType.Credits);
            }
        }
        else if (_task == (int)RequestorTasks.RATING_ENJOYING)
        {
            if (_result == Requestor.RequestorButton.LeftButton) {
                Requestor.Activate((int)RequestorTasks.RATING_RATE, 0, this, LocalizationManager.GetTranslation("rating_rate"), LocalizationManager.GetTranslation("Generic Text/yes_word"), LocalizationManager.GetTranslation("Generic Text/not_now"));
            } else if (_result == Requestor.RequestorButton.RightButton) {
                Requestor.Activate((int)RequestorTasks.RATING_FEEDBACK, 0, this, LocalizationManager.GetTranslation("rating_feedback"), LocalizationManager.GetTranslation("Generic Text/yes_word"), LocalizationManager.GetTranslation("Generic Text/no_word"));
            }
        }
        else if (_task == (int)RequestorTasks.RATING_RATE)
        {
            if (_result == Requestor.RequestorButton.LeftButton) {
                //LogToChat("CanRequestRating(): " + StoreReview.CanRequestRating());
#if UNITY_IOS || UNITY_ANDROID
                // If the annual max number of requests hasn't been met, the player hasn't turned off requests, and the player hasn't rated yet...
                if (StoreReview.CanRequestRating())
                {
                    // Request rating
                    StoreReview.RequestRating();
                    Debug.Log("Requested rating.");
                    //LogToChat("Requested rating");

                    // Record login time of session in which this previous rating request was made.
                    PlayerPrefs.SetInt("prev_rating_request_time", GameData.instance.gameTimeAtLogin);
                }
#endif
            }
        }
        else if (_task == (int)RequestorTasks.RATING_FEEDBACK)
        {
            if (_result == Requestor.RequestorButton.LeftButton) {
                Application.OpenURL("https://warofconquest.com/forum/");
            }
        }
    }

    public static Rect RectTransformToScreenSpace(RectTransform transform)
    {
         Vector2 size = Vector2.Scale(transform.rect.size, transform.lossyScale);
         Rect rect = new Rect(transform.position.x, Screen.height - transform.position.y, size.x, size.y);
         rect.x -= (transform.pivot.x * size.x);
         rect.y -= ((1.0f - transform.pivot.y) * size.y);
         return rect;
    }
}
