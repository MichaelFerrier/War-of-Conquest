using UnityEngine;
using UnityEngine.Networking;
using System;
using System.Collections;
using System.IO;
using System.Net.Sockets;
using System.Xml;
using System.Text;
using System.Collections;
using System.Collections.Generic;
using I2.Loc;
#if !DISABLESTEAMWORKS
using Steamworks;
#endif

public class Network : MonoBehaviour {
	public static Network instance;

	bool socketReady = false;
	
	TcpClient mySocket;
	NetworkStream theStream;
	StreamWriter theWriter;
	StreamReader theReader;

    const int CLIENT_VERSION = 50;

    const float RESPONSE_EXPECTATION_PERIOD = 30f;
    const float TITLE_SEQUENCE_DELAY = 6.5f;
    public const float DOWNLOAD_TIMEOUT_PERIOD = 180f;

    public enum  SetupSocketResult
    {
        Success    = 0,
        Exception  = 1,
        Failure    = 2
    }

    public const int INPUT_BUFFER_CAPACITY = 32768;//262144;//65536;
	public int inputBufferLength = 0;
    public int inputBufferCapacity = INPUT_BUFFER_CAPACITY;
   	public byte[] inputBuffer = new byte[INPUT_BUFFER_CAPACITY]; // Oddly, to change the size of the array, it's not enough to change the constant INPUT_BUFFER_CAPACITY -- the array itself must be renamed, as if its length was cached somewhere.

	public String fileUrl = "https://warofconquest.com/woc2/";
    public String infoFilename = "info.xml";

    public XmlDocument infoXmlDoc = null;

	public ServerInfo[] servers;
	public int numServers = -1;

    public float expect_response_deadline = 0f;
    public float prevCommandSentTime = 0f;

    public bool load_data_error = false;
    public String load_data_error_message = "";
    public bool info_fetched = false;
    public bool maps_loaded = false;

    bool enter_game_once_connected = false;

    // ID and addressof game server we are currently connected to.
    int connectedServerID = -1;
    string connectedServerAddress = "";

    public bool initialRun = false;

    public Network()
    {
		instance = this;
	}

    // Use this for initialization
    void Start()
    {
        // TESTING
        //PlayerPrefs.DeleteAll();

        AttemptFetchInfo();
        LogInstallation(false);

        // TESTING
        if (initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false)) {
            LogEvent(Time.time + ": Client ID " + GetClientID(true) + " initial installation.");
        }
    }

    public void AttemptFetchInfo()
    {
        Debug.Log("AttemptFetchInfo() called"); // testing
        // Start coroutine to connect to web server and fetch info xml file.
        StartCoroutine("AttemptFetchInfo_Coroutine");
    }

    IEnumerator AttemptFetchInfo_Coroutine()
    {
        int version;
        float start_time;
        bool success = true;
        string error_message = "";
        float title_sequence_end_time = 0f;

        // Wait until the next frame, so that all game objects have completed their Start() function before this continues, to void interference with eg. opening a panel here, that is closed by GameGUI's initialization.
        yield return null;

        /*
        // If no activation code has yet been recorded, request activation code from the player.
        if ((PlayerPrefs.HasKey("activation_code") == false) || (PlayerPrefs.GetString("activation_code").Equals(""))) 
        {
            GameGUI.instance.OpenActivationCodePanel("");
            yield break;
        }

        // Check whether the activation code is valid. Yield until response is received.
		www = new WWW(fileUrl + "validate_activation_code.php?code=" + PlayerPrefs.GetString("activation_code"));
        start_time = Time.unscaledTime;
        while ((www.isDone == false) && (Time.unscaledTime < (start_time + 15f)))
        {
            yield return new WaitForSeconds(1);
        }

        if ((www.isDone == false) || (www.error != null))
        {
            success = false;
            error_message = LocalizationManager.GetTranslation("cannot_validate_code");// "Could not validate activation code. Please try again in a few minutes.";
        }
        else
        */
        {
            /*
            if (www.text.IndexOf("true") == -1)
            {
                // If the activation code did not come back as valid, ask the player to re-enter it.
                // GB:Localization
                string activation_please_re_enter_code = 
                    LocalizationManager.GetTranslation("activation_please_re_enter_code"); // "Please re-enter your activation code."
                GameGUI.instance.OpenActivationCodePanel(activation_please_re_enter_code);
                yield break;
            }
            */

            // Show downloading message.
            GameGUI.instance.SuspendScreenShowMessage(LocalizationManager.GetTranslation("downloading"));

            if (PlayerPrefs.HasKey("serverID") == true)
            {
                // Have the suspend screen reveal its image immediately.
                GameGUI.instance.SuspendScreenShowImage(true);
            }

            // TESTING
            if (initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false)) {
                LogEvent(Time.time + ": Client ID " + GetClientID(true) + " about to load info.xml.");
            }

            // TESTING PRELOADING

	        UnityWebRequest www = UnityWebRequest.Get(fileUrl + infoFilename);
			yield return Network.instance.RequestFromWeb(www);

			if (www.isNetworkError || www.isHttpError) 
            {
                success = false;
                error_message = LocalizationManager.GetTranslation("cannot_fetch_info"); // "Could not fetch info from server. Please try again in a few minutes.";

                // TESTING
                if (initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false)) {
                    LogEvent(Time.time + ": Client ID " + GetClientID(true) + " could not load info.xml.");
                }
            }
            else 
            {
    	        // Parse the xml file
		        infoXmlDoc= new XmlDocument();
		        infoXmlDoc.LoadXml(www.downloadHandler.text);

                Debug.Log("Loaded xml info file.");

                // TESTING
                if (initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false)) {
                    LogEvent(Time.time + ": Client ID " + GetClientID(true) + " loaded info.xml.");
                }

                // Determine required client version.

                XmlNode client_node = infoXmlDoc.SelectSingleNode("/info/client");
                version = Int32.Parse (client_node.Attributes["version"].Value);
            
                if (version > CLIENT_VERSION)
                {
                    // TESTING
                    if (initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false)) {
                        LogEvent(Time.time + ": Client ID " + GetClientID(true) + " new client required.");
                    }

                    success = false;
                    error_message = "";
                    GameGUI.instance.ClientDownloadRequired();
                } 

                if (success)
                {
                    // Download data files if necessary.
                    GameGUI.instance.SuspendScreenShowProgress(0f);
                    yield return StartCoroutine(DownloadDataIfNew(FileData.DataFileType.Build, infoXmlDoc.SelectSingleNode("/info/builds"), "builds_version", "builds.tsv"));
                    GameGUI.instance.SuspendScreenShowProgress(.15f);
                    yield return StartCoroutine(DownloadDataIfNew(FileData.DataFileType.Object, infoXmlDoc.SelectSingleNode("/info/objects"), "objects_version", "objects.tsv"));
                    GameGUI.instance.SuspendScreenShowProgress(.30f);
                    yield return StartCoroutine(DownloadDataIfNew(FileData.DataFileType.Tech, infoXmlDoc.SelectSingleNode("/info/technologies"), "technologies_version", "technologies.tsv"));
                    GameGUI.instance.SuspendScreenShowProgress(.45f);
                    yield return StartCoroutine(DownloadDataIfNew(FileData.DataFileType.Quest, infoXmlDoc.SelectSingleNode("/info/quests"), "quests_version", "quests.tsv"));
                    GameGUI.instance.SuspendScreenShowProgress(.60f);
                    yield return StartCoroutine(DownloadDataIfNew(FileData.DataFileType.League, infoXmlDoc.SelectSingleNode("/info/leagues"), "leagues_version", "leagues.tsv"));
                    GameGUI.instance.SuspendScreenShowProgress(.80f);
                    yield return StartCoroutine(DownloadDataIfNew(FileData.DataFileType.Emblem, infoXmlDoc.SelectSingleNode("/info/emblems"), "emblems_version", "emblems.png"));
                    GameGUI.instance.SuspendScreenShowProgress(1f);

					Debug.Log("Loading data " + (load_data_error ? "failed." : "succeeded."));

                    if (load_data_error)
                    {
                        success = false;
                        error_message = load_data_error_message;

                        // TESTING
                        if (initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false)) {
                            LogEvent(Time.time + ": Client ID " + GetClientID(true) + " failed to load a data file.");
                        }
                    }
                }

                // TESTING
                if (initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false)) {
                    LogEvent(Time.time + ": Client ID " + GetClientID(true) + " finished loading data files. Success: " + success);
                }
 
                if (success)
                {
                    if (infoXmlDoc == null)
                    {
                        // TESTING
                        if (initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false)) {
                            LogEvent(Time.time + ": Client ID " + GetClientID(true) + " infoXmlDoc == null");
                        }

                        // Have GUI suspend game, with message.
                        GameGUI.instance.OpenSuspendScreen(false, LocalizationManager.GetTranslation("server_info_not_received"));
                        yield break;
                    }

                    // Compile the server list from the info xml
                    CompileServerList();
                }
            }
        }

        if (success) 
        {
            if (PlayerPrefs.HasKey("serverID") == false)
            {
                // Begin playing the title music
                Sound.instance.PlayMusic(Sound.instance.musicTitle, false, 0f, -1, 0f, 0f);

                // Have the suspend screen reveal its image in time to the title music.
                GameGUI.instance.SuspendScreenShowImage(false);

                title_sequence_end_time = Time.unscaledTime + TITLE_SEQUENCE_DELAY;
            }

            // Clear suspend screen message.
            GameGUI.instance.SuspendScreenShowMessage("");

            // Wait until the end of the title sequence if necessary.
            if (Time.unscaledTime < title_sequence_end_time) {
                yield return new WaitForSeconds(title_sequence_end_time - Time.unscaledTime);
            }

            //Debug.Log("Done waiting");

            // All info has been fetched. Update the GUI and attempt to enter the game.
            GameGUI.instance.DataLoaded();

            // Attempt to connect to the associated game server and enter the game.
            // If there is no associated game server, open welcome panel instead.
            AttemptEnterGame();
        }
        else
        {
            // Have GUI suspend game, with message.
            GameGUI.instance.OpenSuspendScreen(false, error_message);
        }
    }

    public void LogInstallation(bool _in_game)
    {
        if (_in_game)
        {
            if (PlayerPrefs.HasKey("loggedEnterGame") == false)
            {
                StartCoroutine(AccessURL(fileUrl + "log_installation.php?in_game=1&uid=" + WWW.EscapeURL(GetClientID()) + "&basic_uid=" + WWW.EscapeURL(GetClientID(true)) + "&client_version=" + CLIENT_VERSION + "&device_type=" + WWW.EscapeURL(GetClientDescription() + "," + SystemInfo.operatingSystem)));
                PlayerPrefs.SetInt("loggedEnterGame", 1);
            }
        }
        else
        {
            if (PlayerPrefs.HasKey("loggedInstallation") == false)
            {
                StartCoroutine(AccessURL(fileUrl + "log_installation.php?in_game=0&uid=" + WWW.EscapeURL(GetClientID()) + "&basic_uid=" + WWW.EscapeURL(GetClientID(true)) + "&client_version=" + CLIENT_VERSION + "&device_type=" + WWW.EscapeURL(GetClientDescription() + "," + SystemInfo.operatingSystem)));
                PlayerPrefs.SetInt("loggedInstallation", 1);
                initialRun = true;
            }
        }

        // TESTING
        //PlayerPrefs.DeleteKey("loggedEnterGame");
        //PlayerPrefs.DeleteKey("loggedInstallation");
    }

    public void LogEvent(String _event)
    {
        StartCoroutine(AccessURL(fileUrl + "log_event.php?event=" + WWW.EscapeURL(_event)));
    }

    IEnumerator AccessURL(String _url)
    {
        //Debug.Log("AccessURL() called for " + _url);
       
		UnityWebRequest www = UnityWebRequest.Get(_url);
		yield return Network.instance.RequestFromWeb(www);
    }

    public IEnumerator DownloadDataIfNew(FileData.DataFileType _type, XmlNode _xml_node, String _version_prefs_key, String _data_filename)
    {
        if (_xml_node == null) 
        {
            Debug.Log("No node for " + _data_filename + " version in info xml file.");
            load_data_error = true;
            load_data_error_message = "No node for " + _data_filename + " version in info xml file.";
            yield break;
        }

        int version = Int32.Parse (_xml_node.Attributes["version"].Value);
        bool download = false;
        bool load_file_error = false;
		UnityWebRequest www;

        if ((PlayerPrefs.HasKey(_version_prefs_key) == false) || (PlayerPrefs.GetInt(_version_prefs_key) != version)) {
            download = true;
        }

        // Have the appropriate data class load the data file. If it fails, re-download the data file.
        if (download == false) {
            if (FileData.LoadDataFile(_type) == false) {
                download = true;
            }
        }

        Debug.Log(_version_prefs_key + ": " + version + " download " + _data_filename + ": " + download + (download ? ((PlayerPrefs.HasKey(_version_prefs_key) == false) ? " because there is no stored version." : ((PlayerPrefs.GetInt(_version_prefs_key) == version) ? " because file could not be loaded." : (" because stored version is " + PlayerPrefs.GetInt(_version_prefs_key)) )) : ""));

        if (download)
        {
            for (int attempt = 1; attempt <= 4; attempt++)
            {
                if (attempt > 1)
                {
                    // Wait a few seconds before trying again.
                    yield return new WaitForSeconds(5f);

                    // TESTING
                    if (initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false)) {
                        LogEvent(Time.time + ": Client ID " + GetClientID(true) + " about to begin attempt " + attempt + " to download file " + _data_filename + ".");
                    }     
                }

                // TESTING
                float prev_send_event_time = Time.time;

                // Attempt to download the data file.
				www = UnityWebRequest.Get(fileUrl + _data_filename);
				var cert = new ForceAcceptAll(); // Create certificate handler to force Android devices to accept warofconquest.com's SSL certificate.
				www.certificateHandler = cert;
				www.Send();

                // Download file.
                float start_time = Time.unscaledTime;
                while ((www.isDone == false) && (Time.unscaledTime < (start_time + Network.DOWNLOAD_TIMEOUT_PERIOD)))
                {
                    Debug.Log("Downloading " + _data_filename + "... (" + (int)(www.downloadProgress * 100) + "%)");
                    yield return new WaitForSeconds(0.2f);

                    // TESTING
                    if (initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false) && (Time.time > (prev_send_event_time + 3f))) {
                        LogEvent(Time.time + ": Client ID " + GetClientID(true) + " has been downloading " + _data_filename + " for " + (Time.unscaledTime - start_time) + "s. Progress: " + www.downloadProgress + ".");
                        prev_send_event_time = Time.time;
                    }
                }

				Debug.Log("Attempt to download " + _data_filename + (www.isDone ? " complete." : " incomplete.") + " Error: " + ((www.error == null) ? " none." : www.error));

                if ((www.isDone == false) || (www.error != null))
                {
                    load_file_error = true;
                    load_data_error_message = LocalizationManager.GetTranslation("cannot_fetch_info").Replace("{file}", _data_filename) + ((www.error == null) ? "" : (" " + www.error)); // "Could not download " + _data_filename + " from server. Please try again in a few minutes.";
                }
                else 
                {
                    string filePath = Application.persistentDataPath + "/" + _data_filename; 
                    System.IO.File.WriteAllBytes(filePath, www.downloadHandler.data);

					Debug.Log("Downloaded file " + _data_filename + " written to " + filePath);

                    // Have the appropriate data class load the downloaded data file. If it fails, try again later.
                    if (FileData.LoadDataFile(_type) == false) {
                        load_file_error = true;
                    }

					Debug.Log("Downloaded file " + _data_filename + " load_file_error: " + (load_file_error ? "true" : "false"));

                    // Record the newly stored version of the data file.
                    PlayerPrefs.SetInt(_version_prefs_key, version);
                }

				// Dispose of certificate handler instance. 
				cert?.Dispose();

                // If we've succeeded in loading this data file, no need for further attempts. Exit loop.
                if (!load_file_error) {
                    break;
                }
            }
        }

        // If this file has failed to load, record that the data has failed to load.
        load_data_error = load_data_error || load_file_error;
    }

    public void CompileServerList()
    {
        // Parse the xml file
        XmlNodeList nodes = infoXmlDoc.DocumentElement.SelectNodes("/info/servers/server");

        // Record the number of servers in the list, and initialize array of ServerInfo objects.
        numServers = nodes.Count;
        servers = new ServerInfo[numServers];

        // Record information about each server.
        int server_index = 0;
        foreach (XmlNode node in nodes)
        {
            ServerInfo cur_server_info = new ServerInfo();

            cur_server_info.ID = Int32.Parse(node.Attributes["ID"].Value);
            cur_server_info.address = node.Attributes["address"].Value;
            cur_server_info.port = Int32.Parse(node.Attributes["port"].Value);
            cur_server_info.name = node.Attributes["name"].Value;
            cur_server_info.language = node.Attributes["language"].Value;
            cur_server_info.isDefault = (node.Attributes["default"].Value == "true");
            cur_server_info.hidden = (node.Attributes["hidden"].Value == "true");
            cur_server_info.redirection = Int32.Parse(node.Attributes["redirection"].Value);
            cur_server_info.description = node.Attributes["description"].Value;

            servers[server_index] = cur_server_info;

            Debug.Log("Server #" + server_index + " ID: " + cur_server_info.ID + ", address: " + cur_server_info.address + ", port: " + cur_server_info.port + ", name: " + cur_server_info.name + ", language: " + cur_server_info.language + ", isDefault: " + (cur_server_info.isDefault ? "true" : "false") + ", hidden: " + (cur_server_info.hidden ? "true" : "false") + ", redirection: " + cur_server_info.redirection + ", description: " + cur_server_info.description);

            server_index++;
        }
    }

    // Determine ID of this client's default server. 
    // Prioritize the default server for the client's language. If none, use the first server of the client's language.
    // If none, use the default English server. If none, use the first English server.
    public int DetermineDefaultServerID(String _language)
    {
        bool found_language_match = false;
        bool found_default_language_match = false;
        bool found_default_english = false;
        bool found_english = false;
        int result = -1;

        for (int server_index = 0; server_index < numServers; server_index++)
        {
            // Skip hidden and redirected servers.
            if (servers[server_index].hidden || (servers[server_index].redirection != -1)) {
                continue;
            }

            if (servers[server_index].language.CompareTo(_language) == 0)
            {
                if (servers[server_index].isDefault)
                {
                    if (found_default_language_match == false)
                    {
                        found_default_language_match = true;
                        result = servers[server_index].ID;
                    }
                }
                else if ((found_default_language_match == false) && (found_language_match == false))
                {
                    found_language_match = true;
                    result = servers[server_index].ID;
                }
            }
            else if ((servers[server_index].language.CompareTo("English") == 0) && (found_default_language_match == false) && (found_language_match == false))
            {
                if (servers[server_index].isDefault)
                {
                    if (found_default_english == false)
                    {
                        found_default_english = true;
                        result = servers[server_index].ID;
                    }
                }
                else if ((found_default_english == false) && (found_english == false))
                {
                    found_english = true;
                    result = servers[server_index].ID;
                }
            }
        }

        return result;
    }

    public int GetAssociatedServerID()
    {
        int result = PlayerPrefs.GetInt ("serverID", -1);

        if (result != -1)
        {
            bool follow_redirects = true;
            while (follow_redirects)
            {
                ServerInfo server_info = GetServerInfo(result);

                if (server_info == null) {
                    return -1;
                }

                follow_redirects = false;
                if (server_info.redirection != -1)
                {
                    // Follow this redirection, and loop again to look for any further redirections.
                    result = server_info.redirection;
                    SetAssociatedServerID(result); // Record the newly redirected associated server ID.
                    follow_redirects = true;
                }
            }
        }

        return result;
    }

    public void SetAssociatedServerID(int _serverID)
    {
        Debug.Log("SetAssociatedServerID(): " + _serverID);
        PlayerPrefs.SetInt("serverID", _serverID);
        PlayerPrefs.SetInt("prevServerID", _serverID); // Set record of previously associated server ID, which will not be cleared upon logout.
        PlayerPrefs.Save();
    }

    public void AssociateWithConnectedServer()
    {
        if (connectedServerID != -1) {
            SetAssociatedServerID(connectedServerID);
        }
    }

    public int GetConnectedServerID()
    {
        return connectedServerID;
    }

    public string GetConnectedServerAddress()
    {
        return connectedServerAddress;
    }

    public ServerInfo GetServerInfo(int _serverID)
    {
        for (int server_index = 0; server_index < numServers; server_index++)
        {
            if (servers[server_index].ID == _serverID) {
                return servers[server_index];
            }
        }

        return null;
    }

	// Update is called once per frame
	void Update()
    {
		ReadSocket();

        // If no server response has been received within the period in which it was expected, connection will be considered lost.
        if ((expect_response_deadline != 0f) && (expect_response_deadline < Time.unscaledTime))
        {
            // Reset the response expectation deadline.
            expect_response_deadline = 0f;

            // Send quit message to server, just in case it's still listening.
            OnApplicationQuit();

            // GB-Localization
            // "Lost connection with server. Please try again in a few minutes."
            // Have GUI suspend game, with message.
            GameGUI.instance.OpenSuspendScreen(false, LocalizationManager.GetTranslation("lost_connection_with_server_try_again"));

            // TESTING
            Debug.Log("Assuming disconnection because did not receive expected response.");
        }
    }
	
	void OnApplicationQuit()
    {
		SendCommand("action=quit_client");
		CloseSocket();
	}

    public void AttemptEnterGame()
    {
        // Determine the serverID associated with this client, if there is one.
        int associated_serverID = GetAssociatedServerID();
        Debug.Log("associated_serverID: " + associated_serverID);

        // TESTING
        if (initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false)) {
            LogEvent(Time.time + ": Client ID " + GetClientID(true) + " about to attempt to enter game on server " + associated_serverID + ".");
        }
        
        if (associated_serverID == -1)
        {
            // This client has no associated game server. Display the welcome panel.
            GameGUI.instance.OpenWelcomePanel(2f);
        }
        else
        {
            // Attempt to connect to this client's associated game server.
            SetupSocketResult result = SetupSocket(associated_serverID);

            if (result == SetupSocketResult.Success)
            {
                // TESTING
                if (initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false)) {
                    LogEvent(Time.time + ": Client ID " + GetClientID(true) + " AttemptEnterGame() SetupSocket() succeeded for server " + associated_serverID + ". About to call ConnectWithGameServer().");
                }

                // Enter the game world
                ConnectWithGameServer(true);
            }
            else if (result == Network.SetupSocketResult.Failure)
            {
                // Display message that the game server is temporarily offline.
                GameGUI.instance.OpenSuspendScreen(false, LocalizationManager.GetTranslation("connect_temporarily_offline"));

                // TESTING
                if (initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false)) {
                    LogEvent(Time.time + ": Client ID " + GetClientID(true) + " AttemptEnterGame() SetupSocket() failed for server " + associated_serverID + ".");
                }
            }
            else if (result == Network.SetupSocketResult.Exception)
            {
                // Display message about socket exception.
                GameGUI.instance.OpenSuspendScreen(false, LocalizationManager.GetTranslation("connect_socket_exception").Replace("{PORT}", string.Format("{0:n0}", Network.instance.GetServerPort(associated_serverID))));
            }
        }
    }

	public void ConnectWithGameServer(bool _enter_game)
    {
        // Record whether to automatically enter game once connection is established.
        enter_game_once_connected = _enter_game;

        // Record that the maps are not yet loaded.
        maps_loaded = false;

        // Get the client's activation code.
        string activation_code = PlayerPrefs.GetString("activation_code", "");

        // TESTING
        if (initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false)) {
            LogEvent(Time.time + ": Client ID " + GetClientID(true) + " about to send event_connect. enter_game_once_connected: " + enter_game_once_connected);
        }

		SendCommand("action=event_connect|uid=" + GetClientID() + "|client_version=" + CLIENT_VERSION + "|activation_code=" + activation_code + "|device_type=" + GetClientDescription() + "," + SystemInfo.operatingSystem + "|basic_uid=" + GetClientID(true), true);
	}

    public void ConnectionEstablished(int _serverID, Dictionary<int, int> _map_modified_times)
    {
        // TESTING
        if (initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false)) {
            LogEvent(Time.time + ": Client ID " + GetClientID(true) + " ConnectionEstablished() (to server + " + _serverID + ") about to call LoadMapImages_Coroutine().");
        }

        StartCoroutine(LoadMapImages_Coroutine(_serverID, _map_modified_times));
    }   

    public IEnumerator LoadMapImages_Coroutine(int _serverID, Dictionary<int, int> _map_modified_times)
    {
        int num_loaded = 0;
        bool success = true;
        String error_message = ""; 

        for (int attempt = 1; attempt <= 4; attempt++)
        {
            if (attempt > 1)
            {
                // Wait a few seconds before trying again.
                yield return new WaitForSeconds(5f);

                // TESTING
                if (initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false)) {
                    LogEvent(Time.time + ": Client ID " + GetClientID(true) + " about to begin attempt " + attempt + " to load maps.");
                }     
            }

            foreach (KeyValuePair<int, int> entry in _map_modified_times)
            {
                bool download_maps = false;
                int mapID = entry.Key;
                int map_mod_time = entry.Value;

                // TESTING
                if (initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false)) {
                    LogEvent(Time.time + ": Client ID " + GetClientID(true) + " about to try to load map " + mapID + ".");
                }

                string player_prefs_key = "map_mod_time_" + _serverID + "_" + mapID;

                if ((PlayerPrefs.HasKey(player_prefs_key) == false) || (PlayerPrefs.GetInt(player_prefs_key) != map_mod_time)) {
                    download_maps = true;
                }

                if (download_maps == false)
                {
                    if (MapView.instance.LoadMapImage(_serverID, mapID) == false) {
                        download_maps = true;
                    }

                    if ((mapID == GameData.MAINLAND_MAP_ID) && (MapPanel.instance.LoadUIMapImage(_serverID) == false)) {
                        download_maps = true;
                    }
                }

                if (download_maps)
                {
                    String url;
                    UnityWebRequest www;
                    float start_time;

                    // TESTING
                    if (initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false)) {
                        LogEvent(Time.time + ": Client ID " + GetClientID(true) + " about to download map " + mapID + ".");
                    }

                    // TESTING
                    float prev_send_event_time = Time.time;

					// Download landscape map image
					url = "http://" + Network.instance.GetConnectedServerAddress() + "/generated/clientmaps/map_" + mapID + ".png";
                    Debug.Log("Downloading landscape map ID " + mapID + " url: " + url + " enter_game_once_connected: " + enter_game_once_connected);
					// NOTE: If map doesn't download correctly, make sure that network security software isn't blocking http access to the server's IP address!
					www = UnityWebRequest.Get(url);
					www.Send();
                    start_time = Time.unscaledTime;
                    while ((www.isDone == false) && (Time.unscaledTime < (start_time + Network.DOWNLOAD_TIMEOUT_PERIOD))) 
                    {
                        if (mapID == GameData.MAINLAND_MAP_ID)
                        {
                            // Update progress bar.
                            GameGUI.instance.SuspendScreenShowProgress(www.downloadProgress);
                        }

                        // TESTING
                        if (initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false) && (Time.time > (prev_send_event_time + 3f))) {
                            LogEvent(Time.time + ": Client ID " + GetClientID(true) + " has been downloading map " + mapID + " for " + (Time.unscaledTime - start_time) + "s. Progress: " + www.downloadProgress + ".");
                            prev_send_event_time = Time.time;
                        }

                        yield return new WaitForSeconds(0.2f);
                    }

                    if (mapID == GameData.MAINLAND_MAP_ID)
                    {
                        // Update progress bar.
                        GameGUI.instance.SuspendScreenShowProgress(1f);
                    }

                    if ((www.isDone == false) || (www.error != null))
                    {
                        // TESTING
                        if (initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false)) {
                            LogEvent(Time.time + ": Client ID " + GetClientID(true) + " failed to download map " + mapID + " (url: " + url + "): " + www.error);
                        }

                        Debug.Log("Error accessing '" + url + "': " + www.error);
                        error_message = "Could not download landscape map from server " + _serverID + ".";
                        success = false;
                        break;
                    
                        //GameGUI.instance.OpenSuspendScreen(false, "Could not download landscape map from server " + _serverID + ".");
                        //yield break;
                    }
                    else
                    {
                        // TESTING
                        if (initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false)) {
                            LogEvent(Time.time + ": Client ID " + GetClientID(true) + " succeeded in downloading map " + mapID + ".");
                        }

                        string filePath = Application.persistentDataPath + "/map_" + _serverID + "_" + mapID + ".png"; 
                        System.IO.File.WriteAllBytes(filePath, www.downloadHandler.data);
                    }

                    if (mapID == GameData.MAINLAND_MAP_ID)
                    {
                        // TESTING
                        if (initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false)) {
                            LogEvent(Time.time + ": Client ID " + GetClientID(true) + " about to download UI map " + mapID + ".");
                        }

                        // TESTING
                        prev_send_event_time = Time.time;

						// Download ui map image
						url = "http://" + Network.instance.GetConnectedServerAddress() + "/generated/clientmaps/ui_map.jpg";
                        Debug.Log("ui map url: " + url);
						www = UnityWebRequest.Get(url);
						www.Send();
                        start_time = Time.unscaledTime;
                        while ((www.isDone == false) && (Time.unscaledTime < (start_time + Network.DOWNLOAD_TIMEOUT_PERIOD))) 
                        {
                            // Update progress bar.
                            GameGUI.instance.SuspendScreenShowProgress(www.downloadProgress);

                            // TESTING
                            if (initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false) && (Time.time > (prev_send_event_time + 3f))) {
                                LogEvent(Time.time + ": Client ID " + GetClientID(true) + " has been downloading UI map " + mapID + " for " + (Time.unscaledTime - start_time) + "s. Progress: " + www.downloadProgress + ".");
                                prev_send_event_time = Time.time;
                            }

                            yield return new WaitForSeconds(0.2f);
                        }

                        // Update progress bar.
                        GameGUI.instance.SuspendScreenShowProgress(1f);

                        if ((www.isDone == false) || (www.error != null))
                        {
                            // TESTING
                            if (initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false)) {
                                LogEvent(Time.time + ": Client ID " + GetClientID(true) + " failed to download UI map " + mapID + ".");
                            }

                            Debug.Log("Error accessing '" + url + "': " + www.error);
                            error_message = "Could not download UI map from server " + _serverID + ".";
                            success = false;
                            break;

                            //GameGUI.instance.OpenSuspendScreen(false, "Could not download UI map from server " + _serverID + ".");
                            //yield break;
                        }
                        else
                        {
                            // TESTING
                            if (initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false)) {
                                LogEvent(Time.time + ": Client ID " + GetClientID(true) + " succeeded in downloading UI map " + mapID + ".");
                            }

                            string filePath = Application.persistentDataPath + "/ui_map_" + _serverID + ".jpg"; 
                            System.IO.File.WriteAllBytes(filePath, www.downloadHandler.data);
                        }
                    }

                    // Record the modification time of this map for this server ID.
                    PlayerPrefs.SetInt(player_prefs_key, map_mod_time);

                    // TESTING
                    if (initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false)) {
                        LogEvent(Time.time + ": Client ID " + GetClientID(true) + " about to have mapview load map " + mapID + ".");
                    }

                    // Have the MapView load the downloaded map.
                    if (MapView.instance.LoadMapImage(_serverID, mapID) == false) 
                    {
                        // TESTING
                        if (initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false)) {
                            LogEvent(Time.time + ": Client ID " + GetClientID(true) + " mapview failed to load map " + mapID + ".");
                        }

                        Debug.Log("MapView could not load map with ID " + + mapID + ".");
                        error_message = "Failed to load map " + mapID + ".";
                        success = false;
                        break;
                    
                        //GameGUI.instance.OpenSuspendScreen(false, "Failed to load map " + mapID + ".");
                        //yield break;
                    }

                    // Keep count of number of maps that have been loaded.
                    num_loaded++;

                    if (mapID == GameData.MAINLAND_MAP_ID)
                    {
                        // Have the Map Panel load the downloaded UI map.
                        if (MapPanel.instance.LoadUIMapImage(_serverID) == false) 
                        {
                            // TESTING
                            if (initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false)) {
                                LogEvent(Time.time + ": Client ID " + GetClientID(true) + " failed to load UI map " + mapID + ".");
                            }

                            Debug.Log("Map Panel could not load UI map.");
                            error_message = "Failed to load UI map.";
                            success = false;
                            break;

                            //GameGUI.instance.OpenSuspendScreen(false, "Failed to load UI map.");
                            //yield break;
                        }
                    }
                    else
                    {
                        // Update progress bar.
                        //Debug.Log("Progress: " + ((float)num_loaded / _map_modified_times.Count) + ", num_loaded: " + num_loaded);
                        GameGUI.instance.SuspendScreenShowProgress((float)num_loaded / _map_modified_times.Count);
                    }
                }

                // TESTING
                if (initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false)) {
                    LogEvent(Time.time + ": Client ID " + GetClientID(true) + " done with map " + mapID + ".");
                }
            }

            // If all maps have loaded successfully, no more attempts needed. Exit loop.
            if (success) {
                break;
            }
        }

        if (!success)
        {
            // All attempts to load maps failed. Display error message and give up.
            GameGUI.instance.OpenSuspendScreen(false, error_message);
            yield break;
        }

        // Record that the maps have been successfully loaded.
        maps_loaded = true;

        // TESTING
        if (initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false)) {
            LogEvent(Time.time + ": Client ID " + GetClientID(true) + " all maps loaded. enter_game_once_connected: " + enter_game_once_connected + ".");
        }

        // Now that the map images have been loaded, connection is complete and entering the game is possible. 
        if (enter_game_once_connected)  {
            SendCommand("action=auto_enter_game", true);
        }
    }

    public int GetServerPort(int _serverID)
    {
        ServerInfo server_info = GetServerInfo(_serverID);
        return (server_info == null) ? -1 : server_info.port;
    }

	public SetupSocketResult SetupSocket(int _serverID)
    {
        // Close any existing socket connection to a game server.
        CloseSocket();

        socketReady = false;

        // Get the server's info
        ServerInfo server_info = GetServerInfo(_serverID);

        if (server_info == null) {
            return SetupSocketResult.Failure;
        }

		try
        {
			// Attempt to connect to the game server with the given ID.
			mySocket = new TcpClient(server_info.address, server_info.port);
			mySocket.NoDelay = true; // Do not delay message sends, waiting for buffer to fill up.
			theStream = mySocket.GetStream();
			theWriter = new StreamWriter(theStream, Encoding.UTF8);
			theReader = new StreamReader(theStream);
			socketReady = true;
            connectedServerID = _serverID;
            connectedServerAddress = server_info.address;
            Debug.Log ("socket connection established to " + server_info.address + ":" + server_info.port);
			return SetupSocketResult.Success;
		}
        catch (SocketException se)
        {
            Debug.Log("Socket error when trying to connect to " + server_info.address + ":" + server_info.port + ":" + se);
			return SetupSocketResult.Failure;
        }
		catch (Exception e)
        {
			Debug.Log("Error when trying to connect to " + server_info.address + ":" + server_info.port + ":" + e);
			return SetupSocketResult.Failure;
		}
	}

	public void SendCommand(String _command, bool _expect_response = false)
    {
		if (socketReady)
        {
			WriteSocket(_command);

            if (_expect_response)
            {
                // Expect a response within 10 seconds.
                expect_response_deadline = Time.unscaledTime + RESPONSE_EXPECTATION_PERIOD;

                // TESTING
                Debug.Log("Expecting response for command: " + _command);
            }

            // Keep track of when latest command was sent.
            prevCommandSentTime = Time.unscaledTime;
		}
	}

	public void WriteSocket(string theLine)
    {
		if (!socketReady)
			return;

        try
        {
            String tmpString = theLine + '\0';
            theWriter.Write(tmpString);
            theWriter.Flush();
        }
        catch (IOException ex)
        {
            socketReady = false;

            // Reset the response expectation deadline.
            expect_response_deadline = 0f;

            // GB-Localization
            // "Lost connection with server. Please try again in a few minutes."
            // Have GUI suspend game, with message.
            GameGUI.instance.OpenSuspendScreen(false, LocalizationManager.GetTranslation("lost_connection_with_server_try_again"));
        }
    }
	
	public void ReadSocket()
    {
        try
        {
            if ((socketReady) && (theStream.DataAvailable))
            {
                //Debug.Log("inputBufferLength: " + inputBufferLength + ", inputBufferCapacity: " + inputBufferCapacity + ", array length: " + inputBuffer.Length); 
                inputBufferLength += theStream.Read(inputBuffer, inputBufferLength, inputBufferCapacity - inputBufferLength);

                // Clear the expect_response_deadline, because data has been received from the server.
                expect_response_deadline = 0f;

                if (inputBufferLength == inputBufferCapacity) 
                {
                    // Expand the input buffer
                    inputBufferCapacity += INPUT_BUFFER_CAPACITY;
                    Array.Resize(ref inputBuffer, inputBufferCapacity);
                    Debug.Log("Expanded inputBufferCapacity to " + inputBufferCapacity);
                    //Debug.Log("ERROR: Input buffer filled to capacity. Resetting -- events are being lost.");
                    //ResetInputBuffer();
                }
            }
        }
        catch (Exception)
        {
            socketReady = false;

            // GB-Localization
            // "Lost connection with server. Please try again in a few minutes."
            // Have GUI suspend game, with message.
            GameGUI.instance.OpenSuspendScreen(false, LocalizationManager.GetTranslation("lost_connection_with_server_try_again"));
        }
    }
	
	public void CloseSocket() {
		if (!socketReady)
			return;
		theWriter.Close();
		theReader.Close();
		mySocket.Close();
		socketReady = false;
        connectedServerID = -1;
	}

    public void DisconnectAbruptly() {
        // This is used only for testing disconnection from server.
        theStream.Close();
    }
	
	public void MaintainConnection(){
		if ((!theStream.CanRead) && (connectedServerID != -1)) {
			SetupSocket(connectedServerID);
		}
	}

	public void ResetInputBuffer() {
		inputBufferLength = 0;
	}

    public string GetClientDescription()
    {
#if !DISABLESTEAMWORKS
        if (SteamManager.Initialized) {
			return "Steam '" + SteamFriends.GetPersonaName() + "': " + SystemInfo.deviceModel;
		}
#endif // !DISABLESTEAMWORKS

        return SystemInfo.deviceModel;
    }

    public string GetClientID(bool _basic = false)
    {
#if !DISABLESTEAMWORKS
        if (SteamManager.Initialized && !_basic) {
			return "SteamID:" + SteamUser.GetSteamID().GetAccountID().ToString();
		}
#endif // !DISABLESTEAMWORKS

        // NOTE: On eg. iOS, may use GameCenter ID instead.

        string clientID = SystemInfo.deviceUniqueIdentifier;

        if ((clientID.CompareTo(SystemInfo.unsupportedIdentifier) == 0) || (clientID.Length == 0))
        {
            // Unity does not provide a unique ID for this device.
            if ((PlayerPrefs.HasKey("clientID")) && (PlayerPrefs.GetString("clientID").Length == 30))
            {
                // We've previously created and stored a unique ID for this device. Return that.
                return PlayerPrefs.GetString("clientID");
            }
            else
            {
                // Create and store a random unique ID for this device, and return that.
                System.Random rand = new System.Random();
                clientID = rand.Next().ToString("D10") + rand.Next().ToString("D10") + rand.Next().ToString("D10");
                PlayerPrefs.SetString("clientID", clientID);
                PlayerPrefs.Save();
                return clientID;
            }
        }
        else
        {
            // Return the device ID as provided by Unity.
            return clientID;
        }
    }

	public void ClearAccountData()
    {
		// Clear the client data associating it with this client's server and player account.
		connectedServerID = -1;
        connectedServerAddress = "";
		PlayerPrefs.DeleteKey("serverID");
        PlayerPrefs.Save();
	}

	public IEnumerator RequestFromWeb(UnityWebRequest _www)
	{
		// This is necessary to ensure that the url's SLL certificate is accepted.
		// https://forum.unity.com/threads/unitywebrequest-report-an-error-ssl-ca-certificate-error.617521/
		var cert = new ForceAcceptAll();
		_www.certificateHandler = cert;
		 
		// Send the web request.
		yield return _www.SendWebRequest();
		 
		// Dispose of instance. 
		cert?.Dispose();
	}

	public void RequestFromWebInstant(UnityWebRequest _www)
	{
		// This is necessary to ensure that the url's SLL certificate is accepted.
		// https://forum.unity.com/threads/unitywebrequest-report-an-error-ssl-ca-certificate-error.617521/
		var cert = new ForceAcceptAll();
		_www.certificateHandler = cert;
		 
		// Send the web request.
		_www.SendWebRequest();
		 
		// Dispose of instance. 
		cert?.Dispose();
	}
} // end class Network

public class ServerInfo {
	public int ID;
	public String address;
	public int port;
	public String name;
    public String language;
	public bool isDefault;
    public bool hidden;
    public int redirection;
    public String description;
}

public class ForceAcceptAll : CertificateHandler
{
	protected override bool ValidateCertificate(byte[] certificateData)
	{
		return true;
	}
}
