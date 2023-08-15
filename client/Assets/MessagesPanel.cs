using UnityEngine;
using UnityEngine.UI;
using System.Collections;

public class MessagesPanel : MonoBehaviour
{
    public static MessagesPanel instance;

    public Sprite inactivePanelTabSprite, activePanelTabSprite;
    public GameObject nationPanelTab, othersPanelTab, gamePanelTab;
    public GameObject nationContentArea, othersContentArea, gameContentArea;

    public GameObject nationMessageListContentObject, othersMessageListContentObject, gameMessageListContentObject;
    public ScrollRect nationMessageScrollRect, othersMessageScrollRect, gameMessageScrollRect;
    public Scrollbar nationMessageScrollbar, othersMessageScrollbar, gameMessageScrollbar;
    public Sprite background_same_nation, background_other_nation, background_admin;

    public LinkManager linkManager = new LinkManager();

    Color tabTextColor_Inctive = new Color(0.65f, 0.65f, 0.65f);
    Color tabTextColor_Active = new Color(1.0f, 1.0f, 1.0f);
    Color tabTextColor_Alert = new Color(0, 0, 0);

    GameObject selectedPanelTab = null;

    float prev_play_message_sound_time = 0;

    bool nationMessageFullListReceived, othersMessageFullListReceived, gameMessageFullListReceived, awaitingMoreMessages;

    public MessagesPanel()
    {
        instance = this;
    }

    public void Start()
    {
        // Set up scroll rect listeners for each of the message lists.
        nationMessageScrollRect.onValueChanged.AddListener(NationMessagesList_OnValueChanged);
        othersMessageScrollRect.onValueChanged.AddListener(OthersMessagesList_OnValueChanged);
        gameMessageScrollRect.onValueChanged.AddListener(GameMessagesList_OnValueChanged);
    }

    public void AddMessage(bool _unread, int _userID, int _nationID, int _deviceID, string _username, string _nation_name, string _text, string _timestamp, int _time, int _reported, bool _addAtTop)
    {
        //Debug.Log("AddMessage() called: " + _text);

        // If the source user is muted on this client, ignore this message.
        if (Chat.instance.IsUserMuted(_userID, _deviceID)) {
            return;
        }

        // Get a new message entry
        GameObject entryObject = MemManager.instance.GetMessageEntryObject();

        // Add the new entry to the list.
        entryObject.transform.SetParent(GetMessageListTransform(_nationID));
        entryObject.transform.localScale = new Vector3(1, 1, 1); // Needs to be done each time it's activated, in case it was changed last time used.

        // Position the message entry at the top or bottom of the list as appropriate.
        if (_addAtTop) {
            entryObject.transform.SetAsFirstSibling();
        } else {
            entryObject.transform.SetAsLastSibling();
        }

        // Get pointer to MessageEntry component.
        MessageEntry curEntry = entryObject.GetComponent<MessageEntry>();

        // Set up the ChatMessageObject button's listener.
        Button messageButton = entryObject.GetComponent<Button>();
        //Debug.Log("entryObject: " + entryObject + ", curEntry: " + curEntry + ", messageButton: " + messageButton);
        messageButton.onClick.RemoveAllListeners();
        messageButton.onClick.AddListener(() => MessageButtonPressed(curEntry));

        // Initialize the new entry
        curEntry.Init(_unread, _userID, _nationID, _deviceID, _username, _nation_name, _text, _timestamp, _time, _reported);

        // If an unread message has been received, turn on the messages panel's alert state, and the appropriate tab's alert state.
        if (_unread) 
        {
            GameGUI.instance.SetPanelAlertState(GameGUI.GamePanel.GAME_PANEL_MESSAGES, true);
            ActivateTabAlert((_nationID == GameData.instance.nationID) ? nationPanelTab : ((_nationID == -1) ? gamePanelTab : othersPanelTab));
        }

        // Play message received sound if appropriate (if it's an unread message, not a game message, and the sound hasn't just been played).
        if (_unread && (_nationID != -1) && (Time.unscaledTime > (prev_play_message_sound_time + 1)))
        {
            Sound.instance.Play2D(Sound.instance.message_received);
            prev_play_message_sound_time = Time.unscaledTime;
        } 
    }

    public void InfoEventReceived()
    {
        // Turn off the alert image of each panel tab
        nationPanelTab.transform.GetChild(0).gameObject.SetActive(false);
        othersPanelTab.transform.GetChild(0).gameObject.SetActive(false);
        gamePanelTab.transform.GetChild(0).gameObject.SetActive(false);

        // Set text of each tab initially to inactive color.
        nationPanelTab.transform.GetChild(1).GetComponent<Text>().color = tabTextColor_Inctive;
        othersPanelTab.transform.GetChild(1).GetComponent<Text>().color = tabTextColor_Inctive;
        gamePanelTab.transform.GetChild(1).GetComponent<Text>().color = tabTextColor_Inctive;

        // Begin with the game tab active.
        TabPressed(gamePanelTab);

        // Reset records dealing with fetching additional messages.
        nationMessageFullListReceived = false;
        othersMessageFullListReceived = false;
        gameMessageFullListReceived = false;
        awaitingMoreMessages = false;

        // Remove all messages from each of the messages lists.
        RemoveAllMessages(nationMessageListContentObject.transform);
        RemoveAllMessages(othersMessageListContentObject.transform);
        RemoveAllMessages(gameMessageListContentObject.transform);

        // Clear the links
        linkManager.ResetLinks();
    }

    public void MoreMessagesReceived(int _type, int _num_messages)
    {
        // Record that we are no longer awaiting more messages.
        awaitingMoreMessages = false;

        // If there were no more messages to be sent of the given type, record that the full list of that type has been received.
        if (_num_messages == 0)
        {
            switch (_type)
            {
                case GameData.MESSAGE_TYPE_GAME: gameMessageFullListReceived = true; break;
                case GameData.MESSAGE_TYPE_NATION: nationMessageFullListReceived = true; break;
                case GameData.MESSAGE_TYPE_OTHER: othersMessageFullListReceived = true; break;
            }
        }
    }

    public void RemoveAllMessages(Transform _messageList)
    {
        // Remove any messages from the given messages list.
        GameObject cur_entry_object;
        while (_messageList.childCount > 0)
        {
            cur_entry_object = _messageList.GetChild(0).gameObject;
            cur_entry_object.transform.SetParent(null);
            MemManager.instance.ReleaseMessageEntryObject(cur_entry_object);
        }
    }

    public void ClosingPanel()
    {
        // Turn off unread toggles on all messages.
        TurnOffUnreadToggles(nationMessageListContentObject.transform);
        TurnOffUnreadToggles(othersMessageListContentObject.transform);
        TurnOffUnreadToggles(gameMessageListContentObject.transform);

        // Tell the server that this user has checked their messages.
        Network.instance.SendCommand("action=messages_checked");
    }

    public void TurnOffUnreadToggles(Transform _messageList)
    {
        // Turn off the unread toggle for each message entry.
        GameObject cur_entry_object;
        for (int i = 0; i < _messageList.childCount; i++)
        {
            cur_entry_object = _messageList.GetChild(i).gameObject;
            cur_entry_object.GetComponent<MessageEntry>().unread_toggle.isOn = false;
        }
    }

    public void MuteUser(int _userID)
    {
        // Remove all messages from the given user, from all message lists.
        RemoveMessagesFromUser(nationMessageListContentObject.transform, _userID);
        RemoveMessagesFromUser(othersMessageListContentObject.transform, _userID);
        RemoveMessagesFromUser(gameMessageListContentObject.transform, _userID);
    }

    public void RemoveMessagesFromUser(Transform _messageList, int _userID)
    {
        // Remove any messages from the given user from the list.
        GameObject cur_entry_object;
        for (int i = 0; i < _messageList.childCount; i++)
        {
            cur_entry_object = _messageList.GetChild(i).gameObject;
            if (cur_entry_object.GetComponent<MessageEntry>().userID == _userID)
            {
                cur_entry_object.transform.SetParent(null);
                MemManager.instance.ReleaseMessageEntryObject(cur_entry_object);
                i--;
            }
        }
    }

    public void DeleteMessage(int _message_time)
    {
        // Remove any message with the given time, from any of the message lists.
        DeleteMessageFromList(nationMessageListContentObject.transform, _message_time);
        DeleteMessageFromList(othersMessageListContentObject.transform, _message_time);
        DeleteMessageFromList(gameMessageListContentObject.transform, _message_time);

        // Have the server delete this message.
        Network.instance.SendCommand("action=delete_message|message_time=" + _message_time);
    }

    public void DeleteMessageFromList(Transform _messageList, int _message_time)
    {
        // Remove the message with the given time from the list.
        GameObject cur_entry_object;
        for (int i = 0; i < _messageList.childCount; i++)
        {
            cur_entry_object = _messageList.GetChild(i).gameObject;
            if (cur_entry_object.GetComponent<MessageEntry>().time == _message_time)
            {
                cur_entry_object.transform.SetParent(null);
                MemManager.instance.ReleaseMessageEntryObject(cur_entry_object);
                i--;
            }
        }
    }

    public void TabPressed(GameObject _panelTab)
    {
        // If there was a formerly active chat tab, display it as inactive.
        if (selectedPanelTab != null)
        {
            selectedPanelTab.GetComponent<Image>().sprite = inactivePanelTabSprite;
            selectedPanelTab.transform.GetChild(1).GetComponent<Text>().color = tabTextColor_Inctive;
        }

        // Record the new active chat tab.
        selectedPanelTab = _panelTab;

        // Display the newly activated tab as being active.
        selectedPanelTab.GetComponent<Image>().sprite = activePanelTabSprite;

        // Set selected tab's text color.
        selectedPanelTab.transform.GetChild(1).GetComponent<Text>().color = tabTextColor_Active;

        // Turn off the newly activated tab's alert image.
        selectedPanelTab.transform.GetChild(0).gameObject.SetActive(false);

        // Activate the appropriate content area.
        nationContentArea.SetActive(selectedPanelTab == nationPanelTab);
        othersContentArea.SetActive(selectedPanelTab == othersPanelTab);
        gameContentArea.SetActive(selectedPanelTab == gamePanelTab);
    }

    public void ActivateTabAlert(GameObject _panelTab)
    {
        //Debug.Log("Messages panel ActivateTabAlert() called for tab " + _panelTab);

        // Do nothing if the given tab is already selected.
        if (_panelTab == selectedPanelTab) {
            return;
        }

        // Turn on the alert image.
        _panelTab.transform.GetChild(0).gameObject.SetActive(true);

        // Change text color.
        //_panelTab.transform.GetChild(1).GetComponent<Text>().color = tabTextColor_Alert;
    }

    public Transform GetMessageListTransform(int _nationID)
    {
        return (_nationID == GameData.instance.nationID) ? nationMessageListContentObject.transform : ((_nationID == -1) ? gameMessageListContentObject.transform : othersMessageListContentObject.transform);
    }

    public void MessageButtonPressed(MessageEntry _entry)
    {
        ChatContextMenu.instance.Activate(Input.mousePosition, ChatContextMenu.Context.Message, _entry.userID, _entry.nationID, _entry.deviceID, _entry.username, _entry.nation_name, _entry.text, _entry.time);
    }

    public void OnClickPostMessage()
    {
        // If the post message dialog doesn't contain a draft message, set its recipient to this nation. 
        if (PostMessagePanel.instance.IsClear()) {
            PostMessagePanel.instance.SetRecipient(GameData.instance.nationName);
        }

        // Open the post message dialog.
        GameGUI.instance.OpenPostMessageDialog();
    }

    public void OnClick_ReplyButton(int nationID, string _nationName)
    {
        PostMessagePanel.instance.Reset();
        PostMessagePanel.instance.SetRecipient(_nationName);

        // Open the post message dialog.
        GameGUI.instance.OpenPostMessageDialog();
    }

    public void OnClick_NationTab()
    {
        TabPressed(nationPanelTab);
    }

    public void OnClick_OthersTab()
    {
        TabPressed(othersPanelTab);
    }

    public void OnClick_GameTab()
    {
        TabPressed(gamePanelTab);
    }

    public void NationMessagesList_OnValueChanged(Vector2 value)
    {
        if ((value.y < 0.001f) && (nationMessageScrollbar.gameObject.activeSelf) && !awaitingMoreMessages && !nationMessageFullListReceived)
        {
            //Debug.Log("nation messages list reached bottom.");
            Network.instance.SendCommand("action=request_more_messages|type=" + GameData.MESSAGE_TYPE_NATION + "|start=" + nationMessageListContentObject.transform.childCount);
            awaitingMoreMessages = true;
        }
    }

    public void OthersMessagesList_OnValueChanged(Vector2 value)
    {
        if ((value.y < 0.001f) && (othersMessageScrollbar.gameObject.activeSelf) && !awaitingMoreMessages && !othersMessageFullListReceived)
        {
            //Debug.Log("other messages list reached bottom.");
            Network.instance.SendCommand("action=request_more_messages|type=" + GameData.MESSAGE_TYPE_OTHER + "|start=" + othersMessageListContentObject.transform.childCount);
            awaitingMoreMessages = true;
        }
    }

    public void GameMessagesList_OnValueChanged(Vector2 value)
    {
        if ((value.y < 0.001f) && (gameMessageScrollbar.gameObject.activeSelf) && !awaitingMoreMessages && !gameMessageFullListReceived)
        {
            //Debug.Log("game messages list reached bottom.");
            Network.instance.SendCommand("action=request_more_messages|type=" + GameData.MESSAGE_TYPE_GAME + "|start=" + gameMessageListContentObject.transform.childCount);
            awaitingMoreMessages = true;
        }
    }
}
