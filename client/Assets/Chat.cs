using UnityEngine;
using UnityEngine.UI;
using UnityEngine.EventSystems;
using System;
using System.Collections;
using System.Collections.Generic;
using System.Text.RegularExpressions;
using I2.Loc;
#if !DISABLESTEAMWORKS
using Steamworks;
#endif

// - If at bottom already, have new text scroll into view.
// - Minimize button in small chat mode gets added to chat area's vertical layout.

public class Chat : MonoBehaviour, RequestorListener
{
    public enum ChatDisplayMode
    {
        UNDEF,
        ICON,
        SMALL,
        MEDIUM,
        LARGE
    };

    private enum RequestorTask
    {
        RemoveThisNationFromGivenChatList,
        RemoveGivenNationFromCurrentChatList
    };

    private const float MEDIUM_HEIGHT = 0.20f;
    private const float SMALL_HEIGHT = 0.15f;
    private const int SCROLLBAR_WIDTH = 18;
    private const int RESIZE_BAR_HEIGHT = 5;

    public const int CHANNEL_ID_GENERAL = 10000000;
    public const int CHANNEL_ID_ALLIES = 10000001;
    public const int CHANNEL_ID_UNDEF = 10000010;

    private const int MAX_MESSAGES_STORED = 200;
    private const float MIN_CHAT_SYSTEM_HEIGHT = 100;
    private const float SCROLL_TO_END_DURATION = 0.35f;

    public static Chat instance;

    public Canvas canvas;
    public GameData gameData;
    public MemManager memManager;
    public RectTransform canvasRectTransform;
    public GameObject mainUILeftObject, mainUIBottomObject;
    public GameObject headerObject, banInfoObject, chatScrollViewObject, chatInputObject, sendButtonObject;
    public GameObject generalChatTabObject, nationChatTabObject, alliesChatTabObject;
    public GameObject maximizeButtonObject, minimizeButtonObject;
    public GameObject resizeBarObject, tabAreaObject, chatAreaObject, chatIconObject;
    public GameObject spacer2Object, spacer3Object, ridgesObject;
    public ChatResizeBar spacer1ResizeBar, spacer2ResizeBar, spacer3ResizeBar;
    public RectTransform mainUILeftRectTransform, mainUIBottomRectTransform;
    public RectTransform chatSystemRectTransform;
    public RectTransform chatInputRectTransform, sendButtonRectTransform, chatAreaRectTransform, tabAreaRectTransform;
    public RectTransform maximizeButtonRectTransform, minimizeButtonRectTransform;
    public RectTransform chatIconRectTransform, chatResizeBarRectTransform;
    public ScrollRect chatScrollRect;
    public GameObject chatContentObject;
    public RectTransform chatScrollViewRectTransform, chatContentRectTransform;
    public InputField chatInputField;
    public Text chatInputPlaceholderText, banTimerText;
    public Image chatAreaImage;
    public Sprite activeChatTabSprite, inactiveChatTabSprite;
    public ChatResizeBar chatResizeBar;
    public GameObject chatTabPrefab, chatMessagePrefab;

    ChatDisplayMode chatDisplayMode = ChatDisplayMode.UNDEF;

    int chatChannelID = CHANNEL_ID_UNDEF;
    GameObject selectedChatTab = null;

    bool placeholderTextCleared = false;

    float prevChatInputTime = 0f;
    List<String> commandHistory = new List<String>();
    int commandHistoryIndex = -1;


    bool ban_active = false;
    float ban_end_time = 0;
    int ban_seconds_remaining = 0;

    public List<int> muted_users = new List<int>();
    public List<int> muted_devices = new List<int>();

    float scroll_to_end__start_pos, scroll_to_end__start_time, scroll_to_end__end_time;
    bool scrolling_to_end = false;

    float chat_height = -1, adjustable_chat_height = -1;

    public Dictionary<int, ChatChannelData> channelTable = new Dictionary<int, ChatChannelData>();

    public Chat()
    {
        instance = this;
    }

    // Use this for initialization
    void Start()
    {
        // Set the ChatTab class's static reference to this script component.
        ChatTab.chatSystem = this;

        // Turn off the alert image of each chat tab's, the chat icon, and the maximize button.
        generalChatTabObject.transform.GetChild(0).gameObject.SetActive(false);
        alliesChatTabObject.transform.GetChild(0).gameObject.SetActive(false);
        nationChatTabObject.transform.GetChild(0).gameObject.SetActive(false);
        chatIconObject.transform.GetChild(0).gameObject.SetActive(false);
        maximizeButtonObject.transform.GetChild(0).gameObject.SetActive(false);
    }

    void Update()
    {
        if (ban_active)
        {
            float time_remaining = ban_end_time - Time.unscaledTime;

            if (time_remaining <= 0)
            {
                // Deactivate the ban and re-set-up the chat display.
                ban_active = false;
                SetDisplayMode(chatDisplayMode);
            }
            else
            {
                int new_seconds_remaining = (int)time_remaining + 1;

                if (new_seconds_remaining != ban_seconds_remaining)
                {
                    // Determine the new text for the ban timer
                    int hours_remaining = new_seconds_remaining / 3600;
                    int remainder = new_seconds_remaining - (hours_remaining * 3600);
                    int minutes_remaining = remainder / 60;
                    int secs_remaining = remainder - (minutes_remaining * 60);

                    // GB-Localization

                    string timer_text = "";
                    if(hours_remaining > 0)
                    {
                        timer_text = String.Format("{0} {1} ",
                            hours_remaining, 
                            (hours_remaining > 1) ? LocalizationManager.GetTranslation("time_hours") : LocalizationManager.GetTranslation("time_hour"));
                    }
                    if(minutes_remaining > 0)
                    {
                        timer_text += String.Format("{0} {1} ",
                            minutes_remaining,
                            (minutes_remaining > 1) ? LocalizationManager.GetTranslation("time_minutes") : LocalizationManager.GetTranslation("time_minute"));
                    }
                    if (secs_remaining > 0)
                    {
                        timer_text += String.Format("{0} {1} ",
                            secs_remaining,
                            (secs_remaining > 1) ? LocalizationManager.GetTranslation("time_seconds") : LocalizationManager.GetTranslation("time_second"));
                    }

                    // Set the ban timer text
                    banTimerText.text = timer_text;
                }
            }
        }

        if (scrolling_to_end)
        {
            // Use sine interpolation to determine current scroll position.
            float t = Math.Min(1.0f, (Time.unscaledTime - scroll_to_end__start_time) / (scroll_to_end__end_time - scroll_to_end__start_time));
            t = Mathf.Sin(t * Mathf.PI * 0.5f);
            chatScrollRect.normalizedPosition = new Vector2(0f, scroll_to_end__start_pos + Mathf.Lerp(0, -scroll_to_end__start_pos, t));

            // If appropriate, record that we're done scrolling to the end.
            if (t == 1) {
                scrolling_to_end = false;
            }
        }

        // If enter was pressed, and were' in the game with no panels up and chat is not in icon mode, and the chat input field is not yet active and has not been submitted during this frame, activate it.
        if ((Input.GetKeyDown(KeyCode.Return) || Input.GetKeyDown(KeyCode.KeypadEnter)) && GameGUI.instance.IsInGame() && (GameGUI.instance.panelBase.activeSelf == false) && (GameGUI.instance.GetActiveGamePanel() == GameGUI.GamePanel.GAME_PANEL_NONE) && (chatDisplayMode != ChatDisplayMode.ICON) && (!chatInputField.isFocused) && (prevChatInputTime != Time.time))  
        {
            chatInputField.ActivateInputField();
            chatInputField.Select();
        }

        // Allow use of arrow keys to iterate through chat command history.
        if ((Input.GetKeyDown(KeyCode.UpArrow) || Input.GetKeyDown(KeyCode.DownArrow)) && chatInputField.isFocused)
        {
            if (commandHistory.Count > 0)
            {
                // If the up arrow was pressed, increment the commandHistoryIndex.
                if (Input.GetKeyDown(KeyCode.UpArrow)) {
                    commandHistoryIndex++;
                }

                // If the down arrow was pressed, decrement the commandHistoryIndex.
                if (Input.GetKeyDown(KeyCode.DownArrow)) {
                    commandHistoryIndex--;
                }

                // Constrain commandHistoryIndex to the number of commands in the history.
                commandHistoryIndex = Math.Max(0, Math.Min(commandHistory.Count - 1, commandHistoryIndex));

                // Fill the chat input field with the command at the current index.
                chatInputField.text = commandHistory[commandHistoryIndex];

                // Activate and select the chatInputField to allow the player to just continue typing.
                chatInputField.ActivateInputField();
                chatInputField.Select();

                // Start a coroutine to deselect text and move caret to end. This can't be done now, must be done in the next frame.
                StartCoroutine(MoveTextEnd_NextFrame());
            }
        }
    }

    public ChatChannelData AddChatChannel(int _channelID, string _title)
    {
        GameObject chatTabObject = AddTab(_title, _channelID);
        ChatChannelData channelData = new ChatChannelData(_channelID, _title, chatTabObject, chatTabObject.GetComponent<ChatTab>());
        channelTable.Add(_channelID, channelData);

        return channelData;
    }

    public GameObject AddTab(string _title, int _channelID)
    {
        // Determine the position in the row of tabs. Nation private chats are placed before spacer2, player whispers before spacer3.
        int siblingIndex = (_channelID < 0) ? spacer3Object.transform.GetSiblingIndex() : spacer2Object.transform.GetSiblingIndex();

        // Instantiate and parent the new tab object.
        GameObject chatTabObject = memManager.GetChatTabObject();
        chatTabObject.transform.SetParent(tabAreaObject.transform);
        chatTabObject.transform.SetSiblingIndex(siblingIndex);
        chatTabObject.transform.localScale = new Vector3(1, 1, 1); // Needs to be done each time it's activated, in case it was changed last time used.

        // Record the new tab's title and channel ID.
        chatTabObject.GetComponent<ChatTab>().channelID = _channelID;
        chatTabObject.transform.GetChild(1).transform.GetChild(0).GetComponent<Text>().text = _title;

        // Set up the tab's event trigger, to notify the chat system when it is pressed.
        EventTrigger trigger = chatTabObject.GetComponent<EventTrigger>();
        EventTrigger.Entry entry = new EventTrigger.Entry();
        entry.eventID = EventTriggerType.PointerDown;
        entry.callback = new EventTrigger.TriggerEvent();
        UnityEngine.Events.UnityAction<BaseEventData> call = new UnityEngine.Events.UnityAction<BaseEventData>(OnPressChatTab);
        entry.callback.AddListener(call);
        trigger.triggers.Add(entry);

        // Set up the tab's close button's listener.
        Button closeButton = chatTabObject.transform.GetChild(2).gameObject.GetComponent<Button>();
        closeButton.onClick.RemoveAllListeners();
        closeButton.onClick.AddListener(() => ChatTabCloseButtonPressed(_channelID));

        // Turn off the new tab's alert image
        chatTabObject.transform.GetChild(0).gameObject.SetActive(false);

        return chatTabObject;
    }

    public void RemoveTab(int _channelID)
    {
        // If the given channel is not in the channel table, do nothing.

        if (channelTable.ContainsKey(_channelID) == false) {
            return;
        }

        // If the tab being closed is the active chat channel, first activate the general chat channel instead.
        if (chatChannelID == _channelID) {
            ChatTabPressed(generalChatTabObject);
        }

        // Get the data corresponding to the chat channel to be closed.
        ChatChannelData channelData = channelTable[_channelID];

        // Clear the chat tab's list of event triggers.
        channelData.chatTabObject.GetComponent<EventTrigger>().triggers.Clear();

        // Remove and release the channel's tab.
        channelData.chatTabObject.transform.SetParent(null);
        memManager.ReleaseChatTabObject(channelData.chatTabObject);

        // Remove the channel's data from the channel table.
        channelTable.Remove(_channelID);
    }

    public void InfoEventReceived()
    {
        // Set chat tab channel IDs.
        generalChatTabObject.GetComponent<ChatTab>().channelID = CHANNEL_ID_GENERAL;
        alliesChatTabObject.GetComponent<ChatTab>().channelID = CHANNEL_ID_ALLIES;
        nationChatTabObject.GetComponent<ChatTab>().channelID = GameData.instance.nationID;

        // If there was a formerly active chat tab, display it as inactive.
        if (selectedChatTab != null) {
            selectedChatTab.GetComponent<Image>().sprite = inactiveChatTabSprite;
        }

        // Record that no chat tab is selected.
        selectedChatTab = null;

        // Reset the chatChannelID.
        chatChannelID = CHANNEL_ID_UNDEF;

        // Remove any previously existing chat messages from the channel tables.
        ChatMessageData cur_message_data;
        foreach(KeyValuePair<int, ChatChannelData> entry in channelTable)
        {
            while (entry.Value.messages.Count > 0) {
                cur_message_data = entry.Value.messages.Dequeue();
            }
        }

        // Remove each chat message from the chat log.
        while (chatContentObject.transform.childCount > 0)
        {
            // Remove this ChatMessageObject from the chat log.
            GameObject removed_message = chatContentObject.transform.GetChild(0).gameObject;
            removed_message.transform.SetParent(null);

            // Return the ChatMessageObject that was displaying this message, to the free list.
            memManager.ReleaseChatMessageObject(removed_message);
        }

        // Remove all chat tabs except for the general, allies, and nation tabs.
        List<int> keys = new List<int> (channelTable.Keys);
        ChatChannelData val;
        foreach (int key in keys) 
        {
            val = channelTable[key];
            if ((val.chatTabObject != generalChatTabObject) && (val.chatTabObject != alliesChatTabObject) && (val.chatTabObject != nationChatTabObject)) {
                RemoveTab(key);
            }
        }

        // Clear the channelTable, in case it was filled in a previous login.
        channelTable.Clear();

        // Add channel data to the channel table for the general, allies and nation chat channels.
        channelTable.Add(CHANNEL_ID_GENERAL, new ChatChannelData(CHANNEL_ID_GENERAL, "General", generalChatTabObject, generalChatTabObject.GetComponent<ChatTab>()));
        channelTable.Add(CHANNEL_ID_ALLIES, new ChatChannelData(CHANNEL_ID_ALLIES, "Allies", alliesChatTabObject, alliesChatTabObject.GetComponent<ChatTab>()));
        channelTable.Add(GameData.instance.nationID, new ChatChannelData(GameData.instance.nationID, "Nation", nationChatTabObject, nationChatTabObject.GetComponent<ChatTab>()));

        // Set initial chat channel to general chat.
        ChatTabPressed(generalChatTabObject);
    }

    public void ChatListReceived(int _nationID, string _nationName, List<ChatListEntryData> _chatList)
    {
        ChatChannelData channelData;

        if (channelTable.ContainsKey(_nationID))
        {
            // Fetch the existing ChatChannelData for this chat channel.
            channelData = channelTable[_nationID];
        }
        else
        {
            // Create a new tab and ChatChannelData for the given nation's chat channel.
            Debug.Log("ChatListReceived calling AddChatChannel for channel " + _nationID);
            channelData = AddChatChannel(_nationID, _nationName);
        }

        // Sort the given chat list.
        _chatList.Sort(new ChatListEntryDataComparer());

        // Record the given chat list in the channel data.
        channelData.chatList = _chatList;
    }

    public void ChatListAddReceived(int _nationID, int _addedNationID, string _addedNationName)
    {
        ChatChannelData channelData;

        if (channelTable.ContainsKey(_nationID) == false) 
        {
            Debug.Log("ChatListAddReceived() called for nation with no chat channel data: " + _nationID);
            return;
        }

        // Fetch the existing ChatChannelData for this chat channel.
        channelData = channelTable[_nationID];

        // Add the new entry to the chat list.
        ChatListEntryData newEntry = new ChatListEntryData(_addedNationID, _addedNationName);
        channelData.chatList.Add(newEntry);

        // Sort the chat list.
        channelData.chatList.Sort(new ChatListEntryDataComparer());

        // If we're currently viewing the chat channel of the nation whose chat list has changed, re-set-up the header.
        if (chatChannelID == _nationID) {
            SetUpHeader();
        }
    }

    public void ChatListRemoveReceived(int _nationID, int _removedNationID)
    {
        ChatChannelData channelData;

        if (channelTable.ContainsKey(_nationID) == false)
        {
            Debug.Log("ChatListRemoveReceived() called for nation with no chat channel data: " + _nationID);
            return;
        }

        // Fetch the existing ChatChannelData for this chat channel.
        channelData = channelTable[_nationID];

        // Determine the index of the entry to remove, and remove it.
        for (int i = 0; i < channelData.chatList.Count; i++)
        {
            if (channelData.chatList[i].nationID == _removedNationID)
            {
                channelData.chatList.RemoveAt(i);
                break;
            }
        }

        if (_removedNationID == gameData.nationID)
        {
            // The player's nation has been removed from another nation's chat list, so remove the tab for that nation's chat.
            RemoveTab(_nationID);
        }
        else if (chatChannelID == _nationID)
        {
            // We're currently viewing the chat channel of the nation whose chat list has changed, so re-set-up the header.
            SetUpHeader();
        }
    }

    public void ChatMessageReceived(int _sourceUserID, int _sourceNationID, int _sourceDeviceID, string _sourceUsername, string _sourceNationName, int _sourceNationFlags, int _channelID, string _recipientUsername, string _text, string _filteredText, int _mod_level)
    {
        Debug.Log("Chat from " + _sourceUsername + "(" + _sourceUserID + ") of nation " + _sourceNationName + "(" + _sourceNationID + ") on channel " + _channelID + " text: '" + _text + "', filtered: '" + _filteredText + "'.");

        // If a whisper has been received and the player has blocked whispers, ignore it.
        if ((_channelID < 0) && (gameData.GetUserFlag(GameData.UserFlags.BLOCK_WHISPERS))) {
            return;
        }

        // If the source user is muted on this client, ignore this message.
        if (IsUserMuted(_sourceUserID, _sourceDeviceID)) {
            return;
        }

        // Determine whether to use the filtered version of the given chat text.
        if ((_filteredText.Length > 0) /*&& (gameData.GetUserFlag(GameData.UserFlags.DISABLE_CHAT_FILTER) == false)*/ && (_sourceUserID != gameData.userID))
        {
            _text = _filteredText;
        }

        ChatChannelData channelData;

        if (channelTable.ContainsKey(_channelID))
        {
            // Fetch the existing ChatChannelData for this chat channel.
            channelData = channelTable[_channelID];
        }
        else
        {
            // Create a new tab and ChatChannelData for this chat channel.
            string tabTitle = (_channelID < 0) ? ((_sourceUserID == gameData.userID) ? _recipientUsername : _sourceUsername) : _sourceNationName;
            Debug.Log("ChatMessageReceived calling AddChatChannel for channel " + _channelID);
            channelData = AddChatChannel(_channelID, tabTitle);
        }

        // Create a new ChatMessageData for the given message.
        ChatMessageData messageData = new ChatMessageData(_sourceUserID, _sourceNationID, _sourceDeviceID, _sourceUsername, _sourceNationName, _sourceNationFlags, _text, _channelID, _mod_level);

        // Add the new message to the given channel's queue.
        channelData.messages.Enqueue(messageData);

        // If the channel has more than the maximum number of messages stored, remove the oldest.
        ChatMessageData removedMessageData;
        while (channelData.messages.Count > MAX_MESSAGES_STORED)
        {
            // Remove the oldest message.
            removedMessageData = channelData.messages.Dequeue();

            // If this chat channel is currently being displayed...
            if (chatChannelID == _channelID)
            {
                // Remove the ChatMessageObject that is displaying this removed chat message.
                removedMessageData.GetMessageObject().transform.SetParent(null);

                // Return the ChatMessageObject that was displaying this message, to the free list.
                memManager.ReleaseChatMessageObject(removedMessageData.GetMessageObject());
            }
        }

        // If we're currently displaying the given chat channel, then add the given message to the chat log display.
        if (chatChannelID == _channelID)
        {
            // Determine whether the end of the chat log is currenty being shown.
            bool showingChatEnd = IsShowingChatEnd();

            // Get a free ChatMessageObject, add it to the chatContentObject and have it display the given message.
            GameObject chatMessageObject = memManager.GetChatMessageObject();
            chatMessageObject.transform.SetParent(chatContentObject.transform);
            chatMessageObject.transform.SetAsLastSibling();
            chatMessageObject.transform.localScale = new Vector3(1, 1, 1); // Needs to be done each time it's activated, in case it was changed last time used.
            chatMessageObject.GetComponent<ChatMessage>().SetMessage(_sourceUserID, _sourceNationID, _sourceUsername, _sourceNationName, _sourceNationFlags, _channelID, _text, _mod_level);

            // Set up the ChatMessageObject button's listener.
            Button messageButton = chatMessageObject.transform.GetChild(0).gameObject.GetComponent<Button>();
            messageButton.onClick.RemoveAllListeners();
            messageButton.onClick.AddListener(() => MessageButtonPressed(messageData));

            // Record reference to the object representing this message, in the message data.
            messageData.SetMessageObject(chatMessageObject);

            // Update the canvas with the added element, before determining whether to scroll down.
            Canvas.ForceUpdateCanvases();

            // If the end of the chat log was previously being shown, and if the contents of the chat log exceeds the size of the
            // scroll rect, then begin scrolling to the (new) end of the chat log.
            if (showingChatEnd && (chatContentRectTransform.rect.height > chatScrollViewRectTransform.rect.height))
            {
                scroll_to_end__start_pos = chatScrollRect.normalizedPosition.y;
                scroll_to_end__start_time = Time.unscaledTime;
                scroll_to_end__end_time = scroll_to_end__start_time + SCROLL_TO_END_DURATION;
                scrolling_to_end = true;
            }
        }
        else
        {
            // Turn on the alert image for this message's chat channel, since it's not the currently active tab.
            channelData.chatTabObject.transform.GetChild(0).gameObject.SetActive(true);

            // If we're in small chat display mode, turn on the alert for the maximize button, to indicate that chat has been received on another channel.
            if (chatDisplayMode == ChatDisplayMode.SMALL) {
                maximizeButtonObject.transform.GetChild(0).gameObject.SetActive(true);
            }

            // If the received message is on a channel other than general chat, play the chat received sound.
            if (_channelID != CHANNEL_ID_GENERAL) {
                Sound.instance.Play2D(Sound.instance.chat_received);
            }
        }

        if (chatDisplayMode == ChatDisplayMode.ICON)
        {
            // If we're in the ICON chat display mode, turn on the chat icon's alert image to show that a chat message was received.
            chatIconObject.transform.GetChild(0).gameObject.SetActive(true);
        }
    }

    public bool IsShowingChatEnd()
    {
        return scrolling_to_end || (chatScrollRect.normalizedPosition.y < 0.02f) || (chatContentRectTransform.rect.height < (chatScrollViewRectTransform.rect.height + 10));
    }

    public float GetScrollPosition()
    {
        return scrolling_to_end ? 0f : chatScrollRect.normalizedPosition.y;
    }

    public void SetScrollPosition(float _position)
    {
        chatScrollRect.normalizedPosition = new Vector2(0, _position);
    }

	public ChatDisplayMode GetDisplayMode()
	{
		return chatDisplayMode;
	}

    public void SetDisplayMode(ChatDisplayMode _newDisplayMode)
    {
        chatDisplayMode = _newDisplayMode;

        headerObject.SetActive(DisplayHeaderForCurrentChannel() && DisplayHeaderForDisplayMode());

        if (chatDisplayMode == ChatDisplayMode.LARGE)
        {
            minimizeButtonObject.SetActive(true);
            maximizeButtonObject.SetActive(false);
            resizeBarObject.SetActive(false);
            spacer1ResizeBar.enabled = true;
            spacer2ResizeBar.enabled = true;
            spacer3ResizeBar.enabled = true;
            ridgesObject.SetActive(false);
            tabAreaObject.SetActive(true);
            chatAreaObject.SetActive(true);
            chatInputObject.SetActive(true);
            sendButtonObject.SetActive(true);
            chatIconObject.SetActive(false);
            banInfoObject.SetActive(ban_active && (chatChannelID == CHANNEL_ID_GENERAL));
            chatScrollViewObject.SetActive((ban_active && (chatChannelID == CHANNEL_ID_GENERAL)) == false);

            // Turn off the chat maximize button's alert image.
            maximizeButtonObject.transform.GetChild(0).gameObject.SetActive(false);

            // Set chat area to be opaque
            chatAreaImage.color = new Color(1, 1, 1, 1);
        }
        else if (chatDisplayMode == ChatDisplayMode.MEDIUM)
        {
            minimizeButtonObject.SetActive(true);
            maximizeButtonObject.SetActive(true);
            resizeBarObject.SetActive(true);
            spacer1ResizeBar.enabled = true;
            spacer2ResizeBar.enabled = true;
            spacer3ResizeBar.enabled = true;
            ridgesObject.SetActive(true);
            tabAreaObject.SetActive(true);
            chatAreaObject.SetActive(true);
            chatInputObject.SetActive(true);
            sendButtonObject.SetActive(true);
            chatIconObject.SetActive(false);
            banInfoObject.SetActive(ban_active && (chatChannelID == CHANNEL_ID_GENERAL));
            chatScrollViewObject.SetActive((ban_active && (chatChannelID == CHANNEL_ID_GENERAL)) == false);

            // Turn off the chat maximize button's alert image.
            maximizeButtonObject.transform.GetChild(0).gameObject.SetActive(false);

            // Set chat area to be opaque
            chatAreaImage.color = new Color(1, 1, 1, 1);
        }
        else if (chatDisplayMode == ChatDisplayMode.SMALL)
        {
            minimizeButtonObject.SetActive(true);
            maximizeButtonObject.SetActive(true);
            resizeBarObject.SetActive(false);
            spacer1ResizeBar.enabled = false;
            spacer2ResizeBar.enabled = false;
            spacer3ResizeBar.enabled = false;
            ridgesObject.SetActive(false);
            tabAreaObject.SetActive(false);
            chatAreaObject.SetActive(true);
            chatInputObject.SetActive(true);
            sendButtonObject.SetActive(true);
            chatIconObject.SetActive(false);
            banInfoObject.SetActive(false);
            chatScrollViewObject.SetActive((ban_active && (chatChannelID == CHANNEL_ID_GENERAL)) == false);

            // Set chat area to be semi-transparent
            chatAreaImage.color = new Color(1, 1, 1, 0.4f);
        }
        else if (chatDisplayMode == ChatDisplayMode.ICON)
        {
            minimizeButtonObject.SetActive(false);
            maximizeButtonObject.SetActive(false);
            resizeBarObject.SetActive(false);
            spacer1ResizeBar.enabled = false;
            spacer2ResizeBar.enabled = false;
            spacer3ResizeBar.enabled = false;
            ridgesObject.SetActive(false);
            tabAreaObject.SetActive(false);
            chatAreaObject.SetActive(false);
            chatInputObject.SetActive(false);
            sendButtonObject.SetActive(false);
            chatIconObject.SetActive(true);
        }

        if (chatDisplayMode != ChatDisplayMode.ICON)
        {
            // If we've switched to a chat mode other than ICON, turn off the chat icon's alert image.
            chatIconObject.transform.GetChild(0).gameObject.SetActive(false);
        }

        // Layout for the new display mode.
        Layout();

        // Have the map view re-determine its screen position and size.
        MapView.instance.DeterminePosition();

        // Re-position the GUI elements above chat
        GameGUI.instance.PositionElementsAboveChat();

        //// TESTING
        //if (chatDisplayMode == ChatDisplayMode.ICON) {
        //    ChatMessageReceived(100, 100, "TestPlayer", "TestNation", 0, GameData.instance.nationID, "", "Well hello, testing chat!", "Well hello, testing chat!", 0);
        //}
    }

    public void Layout()
    {
        float mainUILeftWidth = GameGUI.instance.GetMainUILeftWidth();// mainUILeftObject.activeSelf ? mainUILeftRectTransform.rect.width : 0;
        float mainUIBottomHeight = GameGUI.instance.GetMainUIBottomHeight();// mainUIBottomObject.activeSelf ? mainUIBottomRectTransform.rect.height : 0;

        if (chatDisplayMode == ChatDisplayMode.LARGE)
        {
            float chatSystemWidth = canvasRectTransform.rect.width - mainUILeftWidth;
            float chatSystemHeight = canvasRectTransform.rect.height - mainUIBottomHeight;

            // Position components
            chatSystemRectTransform.SetInsetAndSizeFromParentEdge(RectTransform.Edge.Left, mainUILeftWidth, chatSystemWidth);
            chatSystemRectTransform.SetInsetAndSizeFromParentEdge(RectTransform.Edge.Top, 0, chatSystemHeight);

            minimizeButtonRectTransform.SetInsetAndSizeFromParentEdge(RectTransform.Edge.Top, 4, minimizeButtonRectTransform.rect.height);
            minimizeButtonRectTransform.SetInsetAndSizeFromParentEdge(RectTransform.Edge.Left, chatSystemWidth - minimizeButtonRectTransform.rect.width - 2, minimizeButtonRectTransform.rect.width);
        }
        else if (chatDisplayMode == ChatDisplayMode.MEDIUM)
        {
            float chatSystemWidth = canvasRectTransform.rect.width - mainUILeftWidth;
            float chatSystemHeight = (canvasRectTransform.rect.height - mainUIBottomHeight) * MEDIUM_HEIGHT;

            // Set initial chat_height, based on initial layout.
            if (adjustable_chat_height == -1) {
                adjustable_chat_height = (canvasRectTransform.rect.height - mainUIBottomHeight) * MEDIUM_HEIGHT;
            }

            // Constrain to maximum chat_height, based on current layout.
            if (adjustable_chat_height > (canvasRectTransform.rect.height - mainUIBottomHeight)) {
                adjustable_chat_height = (canvasRectTransform.rect.height - mainUIBottomHeight);
            }

            chat_height = adjustable_chat_height;

            // Let the chat resize bar know about the current chat height limits.
            chatResizeBar.SetHeightLimits(MIN_CHAT_SYSTEM_HEIGHT, (canvasRectTransform.rect.height - mainUIBottomHeight));

            // Position components
            chatSystemRectTransform.SetInsetAndSizeFromParentEdge(RectTransform.Edge.Left, mainUILeftWidth, chatSystemWidth);
            chatSystemRectTransform.SetInsetAndSizeFromParentEdge(RectTransform.Edge.Top, canvasRectTransform.rect.height - mainUIBottomHeight - chat_height, chat_height);
            
            minimizeButtonRectTransform.SetInsetAndSizeFromParentEdge(RectTransform.Edge.Top, RESIZE_BAR_HEIGHT + 4, minimizeButtonRectTransform.rect.height);
            minimizeButtonRectTransform.SetInsetAndSizeFromParentEdge(RectTransform.Edge.Left, chatSystemWidth - minimizeButtonRectTransform.rect.width - maximizeButtonRectTransform.rect.width - 4, minimizeButtonRectTransform.rect.width);
            maximizeButtonRectTransform.SetInsetAndSizeFromParentEdge(RectTransform.Edge.Top, RESIZE_BAR_HEIGHT + 4, maximizeButtonRectTransform.rect.height);
            maximizeButtonRectTransform.SetInsetAndSizeFromParentEdge(RectTransform.Edge.Left, chatSystemWidth - maximizeButtonRectTransform.rect.width - 2, maximizeButtonRectTransform.rect.width);
        }
        else if (chatDisplayMode == ChatDisplayMode.SMALL)
        {
            float chatSystemWidth = canvasRectTransform.rect.width - mainUILeftWidth;
            float chatSystemHeight = (canvasRectTransform.rect.height - mainUIBottomHeight) * SMALL_HEIGHT;

            chat_height = chatSystemHeight;

            // Position components
            chatSystemRectTransform.SetInsetAndSizeFromParentEdge(RectTransform.Edge.Left, mainUILeftWidth, chatSystemWidth);
            chatSystemRectTransform.SetInsetAndSizeFromParentEdge(RectTransform.Edge.Top, canvasRectTransform.rect.height - mainUIBottomHeight - chatSystemHeight, chatSystemHeight);
            
            minimizeButtonRectTransform.SetInsetAndSizeFromParentEdge(RectTransform.Edge.Top, 4, minimizeButtonRectTransform.rect.height);
            minimizeButtonRectTransform.SetInsetAndSizeFromParentEdge(RectTransform.Edge.Left, chatSystemWidth - minimizeButtonRectTransform.rect.width - maximizeButtonRectTransform.rect.width - 4 - SCROLLBAR_WIDTH, minimizeButtonRectTransform.rect.width);
            maximizeButtonRectTransform.SetInsetAndSizeFromParentEdge(RectTransform.Edge.Top, 4, maximizeButtonRectTransform.rect.height);
            maximizeButtonRectTransform.SetInsetAndSizeFromParentEdge(RectTransform.Edge.Left, chatSystemWidth - maximizeButtonRectTransform.rect.width - 2 - SCROLLBAR_WIDTH, maximizeButtonRectTransform.rect.width);
        }
        else if (chatDisplayMode == ChatDisplayMode.ICON)
        {/*
            // Position components
            chatSystemRectTransform.SetInsetAndSizeFromParentEdge(RectTransform.Edge.Left, mainUILeftWidth + 4, chatIconRectTransform.rect.width);
            chatSystemRectTransform.SetInsetAndSizeFromParentEdge(RectTransform.Edge.Top, canvasRectTransform.rect.height - mainUIBottomHeight - chatIconRectTransform.rect.height - 6, chatIconRectTransform.rect.height);
            chatIconRectTransform.SetInsetAndSizeFromParentEdge(RectTransform.Edge.Left, 5, chatIconRectTransform.rect.width);
            chatIconRectTransform.SetInsetAndSizeFromParentEdge(RectTransform.Edge.Top, 0, chatIconRectTransform.rect.height);
            */
        }

        // Update the canvas with the new layout, before setting the chat log position.
        Canvas.ForceUpdateCanvases();

        // Set the position to the bottom of the chat log scroll rect.
        chatScrollRect.normalizedPosition = new Vector2(0, 0);
    }

    public void ChatHeightChanged(float _height)
    {
        chat_height = adjustable_chat_height = _height;

        // Have the map view re-determine its screen position and size.
        MapView.instance.DeterminePosition();

        // Re-position the GUI elements above chat
        GameGUI.instance.PositionElementsAboveChat();
    }

    public float GetChatHeight()
    {
        switch (chatDisplayMode)
        {
            case ChatDisplayMode.LARGE: return Screen.height;
            case ChatDisplayMode.MEDIUM: return chat_height;
            case ChatDisplayMode.SMALL: return chat_height;
            case ChatDisplayMode.ICON: return 0f;
            default: return 0f;
        }
    }

    public float GetOpaqueChatHeight()
    {
        switch (chatDisplayMode)
        {
            case ChatDisplayMode.LARGE: return Screen.height;
            case ChatDisplayMode.MEDIUM: return chat_height;
            case ChatDisplayMode.SMALL: return chatInputRectTransform.rect.height;
            case ChatDisplayMode.ICON: return 0f;
            default: return 0f;
        }
    }

    public ChatDisplayMode GetChatDisplayMode()
    {
        return chatDisplayMode;
    }

    public bool DisplayHeaderForCurrentChannel()
    {
        return ((chatChannelID != CHANNEL_ID_GENERAL) && (chatChannelID != CHANNEL_ID_ALLIES));
    }

    public bool DisplayHeaderForDisplayMode()
    {
        return ((chatDisplayMode == ChatDisplayMode.LARGE) || (chatDisplayMode == ChatDisplayMode.MEDIUM));
    }

    public void SetUpHeader()
    {
        // Clear the ChatName and ChatNameX objects currently in the header.
        while (headerObject.transform.childCount > 1)
        {
            GameObject cur_child = headerObject.transform.GetChild(1).gameObject;
            cur_child.transform.SetParent(null);

            if (cur_child.transform.childCount == 1)
            {
                memManager.ReleaseChatNameObject(cur_child);
            }
            else
            {
                memManager.ReleaseChatNameXObject(cur_child);
            }
        }

        if (chatChannelID < 0)
        {
            // This is a whisper chat channel. Add a ChatName to the header, for the chat partner.
            AddNameToHeader(chatChannelID, channelTable[chatChannelID].title, false);
        }
        else if ((chatChannelID != CHANNEL_ID_GENERAL) && (chatChannelID != CHANNEL_ID_ALLIES))
        {
            if (channelTable.ContainsKey(chatChannelID))
            {
                // Fetch the ChatChannelData for this chat channel.
                ChatChannelData channelData = channelTable[chatChannelID];

                if (chatChannelID == gameData.nationID)
                {
                    // Add the player's nation to the header, without a 'close' button.
                    AddNameToHeader(gameData.nationID, gameData.nationName, false);

                    // Add each nation in the player's nation's chat list to the header, with 'close' buttons.
                    foreach (ChatListEntryData cur_entry in channelData.chatList) {
                        AddNameToHeader(cur_entry.nationID, cur_entry.name, true);
                    }
                }
                else
                {
                    // Add the nation whose chat list this is to the header, without a 'close' button.
                    AddNameToHeader(chatChannelID, channelData.title, false);

                    // Add each nation in the nation's chat list to the header, with a 'close' button only for the player's nation.
                    foreach (ChatListEntryData cur_entry in channelData.chatList) {
                        AddNameToHeader(cur_entry.nationID, cur_entry.name, cur_entry.nationID == gameData.nationID);
                    }
                }
            }
        }
    }

    public void AddNameToHeader(int _id, string _name, bool _close_button)
    {
        GameObject chatNameObject = _close_button ? memManager.GetChatNameXObject() : memManager.GetChatNameObject();
        chatNameObject.transform.GetChild(0).gameObject.GetComponent<Text>().text = _name;

        chatNameObject.transform.SetParent(headerObject.transform);
        chatNameObject.transform.SetAsLastSibling();
        chatNameObject.transform.localScale = new Vector3(1, 1, 1); // Needs to be done each time it's activated, in case it was changed last time used.

        if (_close_button)
        {
            // Set up the header entry's close button's listener.
            Button closeButton = chatNameObject.transform.GetChild(1).gameObject.GetComponent<Button>();
            closeButton.onClick.RemoveAllListeners();
            closeButton.onClick.AddListener(() => ChatNameCloseButtonPressed(_id, _name));
        }
    }

    public bool IsNationInChatList(int _nationID)
    {
        if (channelTable.ContainsKey(gameData.nationID) == false) {
            return false;
        }

        ChatChannelData channelData = channelTable[gameData.nationID];

        for (int i = 0; i < channelData.chatList.Count; i++)
        {
            if (channelData.chatList[i].nationID == _nationID) {
                return true;
            }
        }

        return false;
    }

    public void MuteUser(int _userID, string _username, int _deviceID)
    {
        // GB-Localization
        // "has been muted"
        GameGUI.instance.DisplayMessage(String.Format("{0} {1}", _username, LocalizationManager.GetTranslation("Chat Context/has_been_muted_text")));

        // Add the user and device to the lists of muted users and devices.
        muted_users.Add(_userID);
        muted_devices.Add(_deviceID);

        // Send message to server.
        Network.instance.SendCommand("action=mute|userID=" + _userID + "|deviceID=" + _deviceID);

        // Remove any messages from the muted user from the messages list.
        MessagesPanel.instance.MuteUser(_userID);

        // Have the options panel show the unmute button.
        OptionsPanel.instance.OnMute();
    }

    public void UnmuteUser(int _userID, string _username, int _deviceID)
    {
        // GB-Localization
        // "is no longer muted"
        GameGUI.instance.DisplayMessage(String.Format("{0} {1}", _username, LocalizationManager.GetTranslation("Chat Context/is_no_longer_muted_text")));

        // Remove the user and device from the lists of muted users and devices.
        muted_users.Remove(_userID);
        muted_devices.Remove(_deviceID);
        
        // Send message to server.
        Network.instance.SendCommand("action=unmute|userID=" + _userID + "|deviceID=" + _deviceID);
    }

    public void UnmuteAll()
    {
        // "Other players are no longer muted"
        GameGUI.instance.DisplayMessage(LocalizationManager.GetTranslation("Chat Context/unmuted_all"));

        // Clear the lists of muted users and devices.
        muted_users.Clear();
        muted_devices.Clear();
        
        // Send message to server.
        Network.instance.SendCommand("action=unmute_all");
    }

    public bool IsUserMuted(int _userID, int _deviceID)
    {
        return muted_users.Contains(_userID) || muted_devices.Contains(_deviceID);
    }

    public void InitiateChatBan(int _ban_duration_in_seconds)
    {
        // Record ban information
        ban_active = (_ban_duration_in_seconds > 0);
        ban_end_time = Time.unscaledTime + (float)_ban_duration_in_seconds;

        // Re-set-up chat display, for ban.
        SetDisplayMode(chatDisplayMode);
    }

    public void StepUpDisplayMode()
    {
        switch (chatDisplayMode)
        {
            case ChatDisplayMode.SMALL: SetDisplayMode(ChatDisplayMode.MEDIUM); break;
            case ChatDisplayMode.MEDIUM: SetDisplayMode(ChatDisplayMode.LARGE); break;
        }
    }

    public void StepDownDisplayMode()
    {
        switch (chatDisplayMode)
        {
            case ChatDisplayMode.SMALL: SetDisplayMode(ChatDisplayMode.ICON); break;
            case ChatDisplayMode.MEDIUM: SetDisplayMode(ChatDisplayMode.SMALL); break;
            case ChatDisplayMode.LARGE: SetDisplayMode(ChatDisplayMode.MEDIUM); break;
        }
    }

    public void OnClickChatIcon()
    {
        SetDisplayMode(ChatDisplayMode.MEDIUM);
    }

    public void OnPressChatTab(BaseEventData _data)
    {
        if (Input.GetMouseButtonDown(0)) { // If this was a left click...
           ChatTabPressed(_data.selectedObject);
        }
    }

    public void ChatTabPressed(GameObject _chatTab)
    {
        // If there was a formerly active chat tab, display it as inactive.
        if (selectedChatTab != null) {
            selectedChatTab.GetComponent<Image>().sprite = inactiveChatTabSprite;
        }

        // TESTING -- looking for cause of null reference exception.
        if (_chatTab == null) {
            GameGUI.instance.LogToChat("ChatTabPressed() called for null! Tell Mike, and let him know which chat tab you pressed that caused this. Thanks!");
        }
        else if (_chatTab.GetComponent<Image>() == null) {
            GameGUI.instance.LogToChat("ChatTabPressed() called for tab with no image component! Tell Mike, and let him know which chat tab you pressed that caused this. Thanks!");
        }

        // Record the new active chat tab.
        selectedChatTab = _chatTab;

        // Display the newly activated tab as being active.
        selectedChatTab.GetComponent<Image>().sprite = activeChatTabSprite;

        // Turn off the newly activated tab's alert image.
        selectedChatTab.transform.GetChild(0).gameObject.SetActive(false);

        if (chatChannelID != CHANNEL_ID_UNDEF)
        {
            if (channelTable.ContainsKey(chatChannelID) == false)
            {
                Debug.Log("Chat tab with channel ID " + chatChannelID + " was previously active, which does not exist in channelTable.");
                return;
            }

            ChatChannelData channelData = channelTable[chatChannelID];

            // Remove and free each ChatMessageObject displaying the messages in the current (old) channel.
            foreach (ChatMessageData messageData in channelData.messages)
            {
                messageData.GetMessageObject().transform.SetParent(null);
                memManager.ReleaseChatMessageObject(messageData.GetMessageObject());
                messageData.SetMessageObject(null);
            }
        }

        // Record the new selected chat channel ID.
        chatChannelID = selectedChatTab.GetComponent<ChatTab>().channelID;

        if (channelTable.ContainsKey(chatChannelID) == false)
        {
            Debug.Log("Chat tab with channel ID " + chatChannelID + " pressed, which does not exist in channelTable.");
            return;
        }

        // Fill the header with the appropriate information.
        SetUpHeader();

        // Activate the header, if appropriate.
        headerObject.SetActive(DisplayHeaderForCurrentChannel() && DisplayHeaderForDisplayMode());

        // Show the ban info object if appropriate
        banInfoObject.SetActive(ban_active && (chatChannelID == CHANNEL_ID_GENERAL) && ((chatDisplayMode == ChatDisplayMode.LARGE) || (chatDisplayMode == ChatDisplayMode.MEDIUM)));
        chatScrollViewObject.SetActive((ban_active && (chatChannelID == CHANNEL_ID_GENERAL)) == false);

        if (chatChannelID != CHANNEL_ID_UNDEF)
        {
            ChatChannelData channelData = channelTable[chatChannelID];

            // Add a ChatMessageObject to display each of the messages in the current (new) channel.
            foreach (ChatMessageData messageData in channelData.messages)
            {
                // Get a free ChatMessageObject, add it to the chatContentObject and have it display the current message.
                GameObject chatMessageObject = memManager.GetChatMessageObject();
                chatMessageObject.transform.SetParent(chatContentObject.transform);
                chatMessageObject.transform.SetAsLastSibling();
                chatMessageObject.transform.localScale = new Vector3(1, 1, 1); // Needs to be done each time it's activated, in case it was changed last time used.
                chatMessageObject.GetComponent<ChatMessage>().SetMessage(messageData.sourceUserID, messageData.sourceNationID, messageData.sourceUsername, messageData.sourceNationName, messageData.sourceNationFlags, messageData.channelID, messageData.text, messageData.mod_level);

                // Set up the ChatMessageObject button's listener.
                Button messageButton = chatMessageObject.transform.GetChild(0).gameObject.GetComponent<Button>();
                messageButton.onClick.RemoveAllListeners();
                messageButton.onClick.AddListener(() => MessageButtonPressed(messageData));

                // Record reference to the object representing this message, in the message data.
                messageData.SetMessageObject(chatMessageObject);
            }
        }

        // Update the canvas with the new layout, before setting the chat log position.
        Canvas.ForceUpdateCanvases();

        // Set the position to the bottom of the chat log scroll rect.
        chatScrollRect.normalizedPosition = new Vector2(0, 0);
    }

    public void InitiateWhisper(int _userID, string _username)
    {
        if (channelTable.ContainsKey(-(_userID ^ gameData.userID)))
        {
            // Switch to the existing whisper chat tab.
            ChatChannelData channelData = channelTable[-(_userID ^ gameData.userID)];
            ChatTabPressed(channelData.chatTabObject);
            chatInputField.text = "";
        }
        else
        {
            // Initiate a whisper chat command.
            chatInputField.text = ("/w " + _username + " ");
        }

        // Activate and select the chatInputField to allow the player to just continue typing.
        chatInputField.ActivateInputField();
        chatInputField.Select();

        // Start a coroutine to deselect text and move caret to end. This can't be done now, must be done in the next frame.
        StartCoroutine(MoveTextEnd_NextFrame());
    }

    public void PrefillChatInputField(String _text)
    {
        chatInputField.text = _text;
    }

    IEnumerator MoveTextEnd_NextFrame()
    {
        yield return 0; // Skip the first frame in which this is called.
        chatInputField.MoveTextEnd(false); // Do this during the next frame.
    }

    public void ChatEndEdit()
    {
        if (Input.GetButtonDown("Submit"))
        {
            ChatInputSubmit();
        }
    }

    public void ChatInputSubmit()
    {
        bool clientChatCommand = false;

        // If there is no chat entry text, do nothing.
        if (chatInputField.text.Length == 0) {
            return;
        }

        // Reset the chat command history index to -1.
        commandHistoryIndex = -1;

        // Record previous time when chat input was received.
        prevChatInputTime = Time.time;

        // Chat commands

        // Check for chat command to display guide
        if (chatInputField.text.ToLower().Contains("/doc") || chatInputField.text.ToLower().Equals("/guide"))
        {
#if DISABLESTEAMWORKS
            Application.OpenURL("https://warofconquest.com/guide/");
#else
            SteamFriends.ActivateGameOverlayToWebPage("https://warofconquest.com/guide/");
#endif
            clientChatCommand = true;
        }

        // Check for chat command to display all chat commands
        if (chatInputField.text.ToLower().Equals("/help"))
        {
            GameGUI.instance.LogToChat(LocalizationManager.GetTranslation("chat_commands_list"));
            clientChatCommand = true;
        }

        // Check for chat command to restart tutorial
        if (chatInputField.text.ToLower().Equals("/restart_tutorial"))
        {
            Tutorial.instance.Restart();
            clientChatCommand = true;
        }

        // Check for chat command to display steam info
        if (chatInputField.text.ToLower().Equals("/steam"))
        {
#if !DISABLESTEAMWORKS
            if (SteamManager.Initialized) 
            {
			    string name = SteamFriends.GetPersonaName();
			    GameGUI.instance.LogToChat("Steam persona name: " + name + ", language: " + SteamApps.GetCurrentGameLanguage());
		    }
            else
            {
                GameGUI.instance.LogToChat("Steam not initialized.");
            }
#else
            GameGUI.instance.LogToChat("This is not a Steam build.");
#endif // !DISABLESTEAMWORKS

            clientChatCommand = true;
        }

        // Check for chat command to open admin panel
        if (GameData.instance.userIsAdmin && (chatInputField.text.ToLower().Equals("/admin"))) 
        {
            GameGUI.instance.SetActiveGamePanel(GameGUI.GamePanel.GAME_PANEL_ADMIN);
            clientChatCommand = true;
        }

        // Check for chat command to open moderator panel
        if ((GameData.instance.userIsAdmin || (GameData.instance.modLevel > 0))&& (chatInputField.text.ToLower().Equals("/mod"))) 
        {
            GameGUI.instance.SetActiveGamePanel(GameGUI.GamePanel.GAME_PANEL_MODERATOR);
            ModeratorPanel.instance.ClearComplaint();
            clientChatCommand = true;
        }

        // Check for chat command to clear account data
        if (GameData.instance.userIsAdmin && (chatInputField.text.ToLower().Contains("/clearid"))) 
        {
            Network.instance.ClearAccountData();
            GameGUI.instance.DisplayMessage("Client IDs cleared.");
            clientChatCommand = true;
        }

        // Check for chat command to display stats
        if (GameData.instance.userIsAdmin && (chatInputField.text.ToLower().Contains("/stats"))) 
        {
            GameGUI.instance.DisplayMessage("Screen: " + Screen.width + "," + Screen.height + ", fps: " + (1 / MapView.instance.avgFrameInterval));
            clientChatCommand = true;
        }

        // Check for chat command to display debug info
        if (GameData.instance.userIsAdmin && (chatInputField.text.ToLower().Contains("/debug"))) 
        {
            GameGUI.instance.DisplayMessage("Screen: " + Screen.width + "," + Screen.height + ", camera y: " + MapView.instance.camera.transform.position.y + ", fieldOfView: " + MapView.instance.camera.fieldOfView);
            clientChatCommand = true;
        }

        // Check for chat command to display safe area
        if (chatInputField.text.ToLower().Contains("/safe_area"))
        {
            GameGUI.instance.LogToChat("Safe area: " + Screen.safeArea.xMin + "," + Screen.safeArea.yMin + " to " + Screen.safeArea.xMax + "," + Screen.safeArea.yMax + ", screen size: " + Screen.width + "," + Screen.height + ", Scale: " + MapView.instance.canvas.scaleFactor);
            clientChatCommand = true;
        }

        // Check for chat command to execute test
        if (chatInputField.text.ToLower().Equals("/test"))
        {
            GameGUI.instance.RequestRating();
            //GameGUI.instance.ShowRewardedVideo();
            /*
            GameData.instance.globalTournamentStatus = 1;
            GameData.instance.tournamentEnrollmentClosesTime = ((new System.Random()).Next(1, 2) == 1) ? (Time.unscaledTime + (new System.Random()).Next(10, 600)) : -1;
            GameData.instance.tournamentNextEliminationTime = ((new System.Random()).Next(1, 2) == 1) ? (Time.unscaledTime + (new System.Random()).Next(10, 160000)) : -1;
            GameData.instance.tournamentEndTime = (new System.Random()).Next(10000,400000);
            GameData.instance.globalTournamentStartDay = 1;
            if ((new System.Random()).Next(1, 4) == 1)
            {
                GameData.instance.nationTournamentStartDay = 0;
            }
            else
            {
                GameData.instance.nationTournamentStartDay = 1;
                GameData.instance.nationTournamentActive = ((new System.Random()).Next(1, 2) == 1) ? true : false;
            }

            GameData.instance.tournamentNumActiveContenders = (new System.Random()).Next(1, 120);
            GameData.instance.tournamentRank = (new System.Random()).Next(1, GameData.instance.tournamentNumActiveContenders);
            GameData.instance.tournamentTrophiesAvailable = (new System.Random()).Next(1, 12000);
            GameData.instance.tournamentTrophiesBanked = (new System.Random()).Next(1, 12000);

            TournamentButton.instance.UpdateForGlobalStatus();
            TournamentButton.instance.UpdateForNationStatus();
            TournamentPanel.instance.UpdateForTournamentStatus();
            */

            clientChatCommand = true;
        }

        // Get the chat input string.
        string chatInputString = chatInputField.text;

        // If the input string does not start with '/' (which would make it a command)...
        if (chatInputString[0] != '/')
        {
            // "You are currently banned from general chat"
            // Display message if banned from general chat.
            if (ban_active && (chatChannelID == CHANNEL_ID_GENERAL))
            {
                GameGUI.instance.DisplayMessage(LocalizationManager.GetTranslation("Chat Context/you_are_banned_from_chat"));
                return;
            }

            // Replace any voucher code, so that voucher codes are not accidentally exposed in chat.
            chatInputString = Regex.Replace(chatInputString, @"([A-Z0-9]){3}-([A-Z0-9]){3}-([A-Z0-9]){3}", "XXX-XXX-XXX");
        }
        else
        {
            // This is a chat command. Add it to the command history.
            commandHistory.Insert(0, chatInputString);
        }

		// TEST appending emoji to end of chat message
		//chatInputString = chatInputString + "\uD83D\uDC71";

        if (!clientChatCommand)
        {
            // Send a chat input event to the server.
            Network.instance.SendCommand("action=chat_input|channel=" + chatChannelID + "|text=" + chatInputString);
        }

        // Clear the chat input field's text.
        chatInputField.text = "";

        // Clear the placeholder text if it has not yet been cleared.
        if (!placeholderTextCleared)
        {
            chatInputPlaceholderText.text = "";
            placeholderTextCleared = true;
        }
    }

    public bool IsMouseOverChatLog()
    {
        if ((chatDisplayMode == ChatDisplayMode.LARGE) || (chatDisplayMode == ChatDisplayMode.MEDIUM) || (chatDisplayMode == ChatDisplayMode.SMALL)) {
            return RectTransformUtility.RectangleContainsScreenPoint(chatScrollViewObject.GetComponent<RectTransform>(), new Vector2(Input.mousePosition.x, Input.mousePosition.y));
        }

        return false;
    }

    public void ChatTabCloseButtonPressed(int _channelID)
    {
        if (_channelID < 0)
        {
            RemoveTab(_channelID);
        }
        else
        {
            // TESTING -- looking for bug that causes exception.
            if (channelTable.ContainsKey(_channelID) == false) 
            {
                GameGUI.instance.LogToChat("ChatTabCloseButtonPressed() for _channelID " + _channelID + " that is not in the channelTable! Tell Mike, and let him know what you did right before this appeared. Thanks!");
                return;
            }

            string channelNationName = channelTable[_channelID].title;

            // GB-Localization
            // "Remove {[chat_name]} from {[channel_name]}'s chat list?"
            string confirm = LocalizationManager.GetTranslation("Chat Context/remove_name_from_another_chat_list1")
                .Replace("{[CHAT_NAME]}", gameData.nationName)
                .Replace("{[CHANNEL_NAME]}", channelNationName);

            Requestor.Activate((int)RequestorTask.RemoveThisNationFromGivenChatList, _channelID, this, confirm, 
                LocalizationManager.GetTranslation("Generic Text/yes_word"), LocalizationManager.GetTranslation("Generic Text/no_word"));
        }
    }

    public void ChatNameCloseButtonPressed(int _ID, string _name)
    {
        string channelNationName = channelTable[chatChannelID].title;

        // GB-Localization
        // "Remove {[chat_name]} from {[channel_name]}'s chat list?"
        string confirm = LocalizationManager.GetTranslation("Chat Context/remove_name_from_another_chat_list2")
            .Replace("{[CHAT_NAME]}", _name)
            .Replace("{[CHANNEL_NAME]}", ((chatChannelID == gameData.nationID) ? gameData.nationName : channelNationName));

        Requestor.Activate((int)RequestorTask.RemoveGivenNationFromCurrentChatList, _ID, this, 
            confirm, LocalizationManager.GetTranslation("Generic Text/yes_word"), LocalizationManager.GetTranslation("Generic Text/no_word"));
    }

    public void MessageButtonPressed(ChatMessageData _messageData)
    {
        ChatContextMenu.instance.Activate(Input.mousePosition, ChatContextMenu.Context.Chat, _messageData.sourceUserID, _messageData.sourceNationID, _messageData.sourceDeviceID, _messageData.sourceUsername, _messageData.sourceNationName, _messageData.text, -1);
    }

    public void RequestorResponse(int _task, int _data, Requestor.RequestorButton _result)
    {
        if ((RequestorTask)_task == RequestorTask.RemoveThisNationFromGivenChatList)
        {
            if (_result == Requestor.RequestorButton.LeftButton)
            {
                // Send chat_list_remove event to the server.
                Network.instance.SendCommand("action=chat_list_remove|nationID=" + _data + "|removedNationID=" + gameData.nationID);
            }
        }
        else if ((RequestorTask)_task == RequestorTask.RemoveGivenNationFromCurrentChatList)
        {
            if (_result == Requestor.RequestorButton.LeftButton)
            {
                // Send chat_list_remove event to the server.
                Network.instance.SendCommand("action=chat_list_remove|nationID=" + chatChannelID + "|removedNationID=" + _data);
            }
        }
    }

    public int GetChatChannelID()
    {
        return chatChannelID;
    }
}

public class ChatMessageData 
{
    public int sourceUserID, sourceNationID, sourceDeviceID, sourceNationFlags, channelID, mod_level;
    public string sourceUsername, sourceNationName, text;
    private GameObject messageObject;

    public ChatMessageData(int _sourceUserID, int _sourceNationID, int _sourceDeviceID, string _sourceUsername, string _sourceNationName, int _sourceNationFlags, string _text, int _channelID, int _mod_level)
    {
        sourceUserID = _sourceUserID;
        sourceNationID = _sourceNationID;
        sourceDeviceID = _sourceDeviceID;
        sourceUsername = _sourceUsername;
        sourceNationName = _sourceNationName;
        sourceNationFlags = _sourceNationFlags;
        text = _text;
        channelID = _channelID;
        mod_level = _mod_level;
    }

    public void SetMessageObject(GameObject _messageObject)
    {
        messageObject = _messageObject;
    }

    public GameObject GetMessageObject()
    {
        return messageObject;
    }
}

public class ChatChannelData
{
    public int channelID;
    public Queue<ChatMessageData> messages;
    public GameObject chatTabObject;
    public ChatTab chatTab;
    public string title;
    public List<ChatListEntryData> chatList = new List<ChatListEntryData>();

    public ChatChannelData(int _channelID, string _title, GameObject _chatTabObject, ChatTab _chatTab)
    {
        channelID = _channelID;
        title = _title;
        chatTabObject = _chatTabObject;
        messages = new Queue<ChatMessageData>();
    }
}

public class ChatListEntryData
{
    public int nationID;
    public string name;

    public ChatListEntryData(int _nationID, string _name)
    {
        nationID = _nationID;
        name = _name;
    }
}

public class ChatListEntryDataComparer : IComparer<ChatListEntryData>
{
    public int Compare(ChatListEntryData a, ChatListEntryData b)
    {
        int compareResult = String.Compare(a.name, b.name, StringComparison.OrdinalIgnoreCase);
        if (compareResult < 1) return -1;
        if (compareResult > 1) return 1;
        return 0;
    }
}
