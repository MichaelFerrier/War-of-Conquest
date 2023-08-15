using UnityEngine;
using UnityEngine.UI;
using System.Collections;
using I2.Loc;

public class ChatContextMenu : MonoBehaviour, RequestorListener
{
    public static ChatContextMenu instance;

    public GameData gameData;
    public Chat chatSystem;

    public GameObject chatContextMenuBase, chatContextMenu;
    public RectTransform chatContextMenuRectTransform;
    public GameObject chatListButtonObject, whisperButtonObject, replyButtonObject, deleteButtonObject, muteButtonObject, reportButtonObject, allianceButtonObject, patronButtonObject, nationInfoButtonObject;

    bool press_released_after_activation;
    int userID, nationID, deviceID;
    string username, nationName;
    string text;
    bool nationIsInChatList, nationWasSentAllianceInvitation, nationIsAlly, nationIsPlayerNation;
    Vector3 click_pos;
    Context context;
    int time;
    float stateChangeTime = -1;

    private const float POSITION_INSET = 0.1f;

    enum Direction { Up, UpperRight, Right, LowerRight, Down, LowerLeft, Left, UpperLeft };

    public enum Context
    {
        Chat,
        Message
    };

    private enum RequestorTask
    {
        SendAllianceInvitation,
        WithdrawAllianceInvitation,
        BreakAlliance,
        PatronOffer
    };

    public ChatContextMenu()
    {
        // Set instance in constructor, as that is always called when the app starts.
        instance = this;
    }
    
    public void Activate(Vector3 _click_pos, Context _context, int _userID, int _nationID, int _deviceID, string _username, string _nationName, string _text, int _time)
    {
        // Set up the menu for the given block.
        bool valid = Setup(_click_pos, _context, _userID, _nationID, _deviceID, _username, _nationName, _text, _time);

        if (valid)
        {
            // Show the chat context menu.
            chatContextMenuBase.GetComponent<GUITransition>().EndTransition();
            chatContextMenuBase.SetActive(true);
            chatContextMenu.GetComponent<GUITransition>().StartTransition(0, 1, 1, 1, false);
            stateChangeTime = Time.unscaledTime;
        }

        // Keep track of whether the press that opened this menu has been released after activation.
        press_released_after_activation = false;
    }

    public void Deactivate()
    {
        chatContextMenuBase.GetComponent<GUITransition>().StartTransition(1, 0, 1, 1, true);
        chatContextMenu.GetComponent<GUITransition>().StartTransition(1, 0, 1, 1, false);

        stateChangeTime = Time.unscaledTime;
    }

    void Update()
    {	
        if ((Input.touchCount > 0) || Input.GetMouseButton(0))
        {
            // Do not deactivate the context menu if the current press is within the menu itself.
            if (!((Input.GetMouseButton(0) && (RectTransformUtility.RectangleContainsScreenPoint(chatContextMenuRectTransform, new Vector2(Input.mousePosition.x,Input.mousePosition.y)))) ||
                 ((Input.touchCount == 1) && (RectTransformUtility.RectangleContainsScreenPoint(chatContextMenuRectTransform, Input.touches[0].position)))))
            {
                // The current press is not within the menu. If the initial press that activated the menu was released already, and it's been enough time since the menu was activated, deactivate it.
                if (press_released_after_activation && ((Time.unscaledTime - stateChangeTime) > 0.5f)) {
                    Deactivate();
                }
            }
        }
        else
        {
            // Record that the initial press that opened this menu has been released since the menu was activated.
            press_released_after_activation = true;
        }
    }

    public bool Setup(Vector3 _click_pos, Context _context, int _userID, int _nationID, int _deviceID, string _username, string _nationName, string _text, int _time)
    {
        bool valid = true;

        click_pos = _click_pos;
        context = _context;
        userID = _userID;
        nationID = _nationID;
        deviceID = _deviceID;
        username = _username;
        nationName = _nationName;
        text = _text;
        time = _time;
        
        if (((_context == Context.Chat) && (_nationID == gameData.nationID)) || ((_context == Context.Message) && (_nationID == -1)))
        {
            if (isActiveAndEnabled)
            {
                Deactivate();
            }

            return false;
        }
        
        // Determine whether the block nation is in the player's nation's chat list.
        nationIsInChatList = Chat.instance.IsNationInChatList(nationID);

        // Determine whether an alliance invitation to the block nation is currently pending.
        nationWasSentAllianceInvitation = GameData.instance.NationIsInAllianceList(GameData.instance.outgoingAllyRequestsList, nationID);
        nationIsAlly = GameData.instance.NationIsInAllianceList(GameData.instance.alliesList, nationID);
        nationIsPlayerNation = (nationID == GameData.instance.nationID);

        // Set up buttons 

        // Chat list button

        // GB-Localization
        if (context == Context.Chat)
        {
            ((ContextMenuButton)(chatListButtonObject.GetComponent<ContextMenuButton>())).Init();

            // Set text of chat list button depending on whether the nation is in the chat list.
            if (nationIsInChatList)
            {
                // "Remove {[NATION_NAME]} from chat list"
                chatListButtonObject.transform.GetChild(0).GetComponent<TMPro.TextMeshProUGUI>().text =
                    LocalizationManager.GetTranslation("remove_name_from_chat_list").Replace("{[NATION_NAME]}", nationName);
            }
            else
            {
                // "Add {[NATION_NAME]} to chat list"
                chatListButtonObject.transform.GetChild(0).GetComponent<TMPro.TextMeshProUGUI>().text =
                    LocalizationManager.GetTranslation("add_name_to_chat_list").Replace("{[NATION_NAME]}", nationName);
            }

            chatListButtonObject.SetActive(true);
        }
        else
        {
            chatListButtonObject.SetActive(false);
        }

        // GB-Localization
        // Whisper button

        if (context == Context.Chat)
        {
            // "Whisper to {[USERNAME]}"

            ((ContextMenuButton)(whisperButtonObject.GetComponent<ContextMenuButton>())).Init();
            whisperButtonObject.transform.GetChild(0).GetComponent<TMPro.TextMeshProUGUI>().text =
                LocalizationManager.GetTranslation("whisper_to_username").Replace("{[USERNAME]}", username);

            whisperButtonObject.SetActive(true);
        }
        else
        {
            whisperButtonObject.SetActive(false);
        }

        // Reply Button

        if ((context == Context.Chat) || (_userID != GameData.instance.userID))
        {
            ((ContextMenuButton)(replyButtonObject.GetComponent<ContextMenuButton>())).Init();
            replyButtonObject.SetActive(context == Context.Message);

            replyButtonObject.SetActive(true);
        }
        else
        {
            replyButtonObject.SetActive(false);
        }

        // Delete Button

        ((ContextMenuButton)(deleteButtonObject.GetComponent<ContextMenuButton>())).Init();
        deleteButtonObject.SetActive((context == Context.Message) && ((GameData.instance.userRank == GameData.RANK_SOVEREIGN) || (_userID == GameData.instance.userID))); // Only available for the nation's sovereign (except for deleting your own messages).

        // GB-Localization
        // Mute button

        if ((context == Context.Chat) || (_userID != GameData.instance.userID))
        {
            string mute_word = LocalizationManager.GetTranslation("Generic Text/mute_word") + " "; // "Mute"

            ((ContextMenuButton)(muteButtonObject.GetComponent<ContextMenuButton>())).Init();
            muteButtonObject.transform.GetChild(0).GetComponent<TMPro.TextMeshProUGUI>().text = mute_word + username;
        
            // Set text of mute button depending on whether the user is muted.
            if (Chat.instance.IsUserMuted(userID, deviceID))
            {
                muteButtonObject.transform.GetChild(0).GetComponent<TMPro.TextMeshProUGUI>().text = 
                    LocalizationManager.GetTranslation("Generic Text/unmute_word") + " " + username;
            }
            else
            {
                muteButtonObject.transform.GetChild(0).GetComponent<TMPro.TextMeshProUGUI>().text = mute_word + username;
            }

            muteButtonObject.SetActive(true);
        }
        else
        {
            muteButtonObject.SetActive(false);
        }

        // Report button

        if ((context == Context.Chat) || (_userID != GameData.instance.userID))
        {
            ((ContextMenuButton)(reportButtonObject.GetComponent<ContextMenuButton>())).Init();

            // Only activate the report button if in the general chat channel, or if context is for a message.
            reportButtonObject.SetActive((Chat.instance.GetChatChannelID() == Chat.CHANNEL_ID_GENERAL) || (context == Context.Message));

            reportButtonObject.SetActive(true);
        }
        else
        {
            reportButtonObject.SetActive(false);
        }

        // Alliance button
        if ((context == Context.Chat) || ((context == Context.Message) && (nationID != GameData.instance.nationID)))
        {
            ((ContextMenuButton)(allianceButtonObject.GetComponent<ContextMenuButton>())).Init();

            // Only activate the alliance button if this nation is not the player's nation.
            allianceButtonObject.SetActive(nationIsPlayerNation == false);

            // GB-Localization
            // Set text of alliance button depending on whether the block nation has an alliance invitation pending.
            if (nationIsAlly)
            {
                // "Break Alliance"
                allianceButtonObject.transform.GetChild(0).GetComponent<TMPro.TextMeshProUGUI>().text = 
                    LocalizationManager.GetTranslation("Chat Context/break_alliance");
            }
            else if (nationWasSentAllianceInvitation)
            {
                // "Withdraw Alliance Invitation"
                allianceButtonObject.transform.GetChild(0).GetComponent<TMPro.TextMeshProUGUI>().text =
                    LocalizationManager.GetTranslation("Chat Context/withdraw_ally_invitation");
            }
            else
            {
                // "Send Alliance Invitation"
                allianceButtonObject.transform.GetChild(0).GetComponent<TMPro.TextMeshProUGUI>().text = 
                    LocalizationManager.GetTranslation("Chat Context/send_ally_invitation");
            }

            allianceButtonObject.SetActive(true);
        }
        else
        {
            allianceButtonObject.SetActive(false);
        }

        // Patron button

        if (_userID != GameData.instance.userID)
        {
            ((ContextMenuButton)(patronButtonObject.GetComponent<ContextMenuButton>())).Init();
            patronButtonObject.SetActive(true);
        }
        else
        {
            patronButtonObject.SetActive(false);
        }

        if (_nationID != GameData.instance.nationID)
        {
            nationInfoButtonObject.transform.GetChild(0).GetComponent<TMPro.TextMeshProUGUI>().text = _nationName + " <sprite=3>";
            nationInfoButtonObject.SetActive(true);
            ((ContextMenuButton)(nationInfoButtonObject.GetComponent<ContextMenuButton>())).Init();
        }
        else 
        {
            nationInfoButtonObject.SetActive(false);
        }

        // Determine where the context menu should appear on the screen.

        Direction placement_dir;

        Debug.Log("Click x / screen w: " + click_pos.x + "/" + Screen.width + " Click y / screen h: " + click_pos.y + "/" + Screen.height);

        int screen_width = Screen.width;
        int screen_height = Screen.height;

        int y_space_above = screen_height - (int)(click_pos.y);
        int y_space_below = (int)(click_pos.y);
        int y_space_beside = (int)(Mathf.Min(y_space_above, y_space_below) * 2);

        int x_space_right = screen_width - (int)(click_pos.x);
        int x_space_left = (int)(click_pos.x);
        int x_space_beside = (int)(Mathf.Min(x_space_left, x_space_right) * 2);

        if ((y_space_above >= y_space_beside) && (y_space_above >= y_space_below))
        {
            // Place the context menu above the click position.

            if ((x_space_right >= x_space_beside) && (x_space_right >= x_space_left))
            {
                // Place the context menu to the upper right of the click position.
                placement_dir = Direction.UpperRight;
            }
            else if ((x_space_left >= x_space_beside) && (x_space_left >= x_space_right))
            {
                // Place the context menu to the upper left of the click position.
                placement_dir = Direction.UpperLeft;
            }
            else
            {
                // Place the context menu directly above the click position.
                placement_dir = Direction.Up;
            }
        }
        else if ((y_space_below >= y_space_beside) && (y_space_below >= y_space_above))
        {
            // Place the context menu below the click position.

            if ((x_space_right >= x_space_beside) && (x_space_right >= x_space_left))
            {
                // Place the context menu to the lower right of the click position.
                placement_dir = Direction.LowerRight;
            }
            else if ((x_space_left >= x_space_beside) && (x_space_left >= x_space_right))
            {
                // Place the context menu to the lower left of the click position.
                placement_dir = Direction.LowerLeft;
            }
            else
            {
                // Place the context menu directly below the click position.
                placement_dir = Direction.Down;
            }
        }
        else
        {
            // Place the context menu beside the click position.

            if (x_space_right >= x_space_left)
            {
                // Place the context menu to the right of the click position.
                placement_dir = Direction.Right;
            }
            else
            {
                // Place the context menu to the left of the click position.
                placement_dir = Direction.Left;
            }
        }

        switch (placement_dir)
        {
            case Direction.Up:
                chatContextMenuRectTransform.pivot = new Vector2(0.5f, POSITION_INSET);
                break;
            case Direction.UpperRight:
                chatContextMenuRectTransform.pivot = new Vector2(POSITION_INSET, POSITION_INSET);
                break;
            case Direction.Right:
                chatContextMenuRectTransform.pivot = new Vector2(POSITION_INSET, 0.5f);
                break;
            case Direction.LowerRight:
                chatContextMenuRectTransform.pivot = new Vector2(POSITION_INSET, 1.0f - POSITION_INSET);
                break;
            case Direction.Down:
                chatContextMenuRectTransform.pivot = new Vector2(0.5f, 1.0f - POSITION_INSET);
                break;
            case Direction.LowerLeft:
                chatContextMenuRectTransform.pivot = new Vector2(1.0f - POSITION_INSET, 1.0f - POSITION_INSET);
                break;
            case Direction.Left:
                chatContextMenuRectTransform.pivot = new Vector2(1.0f - POSITION_INSET, 0.5f);
                break;
            case Direction.UpperLeft:
                chatContextMenuRectTransform.pivot = new Vector2(1.0f - POSITION_INSET, POSITION_INSET);
                break;
        }

        chatContextMenuRectTransform.position = click_pos;

        return valid;
    }

    public void OnClick_ChatListButton()
    {
        if (nationIsInChatList == false)
        {
            // Send chat_list_add event to the server.
            Network.instance.SendCommand("action=chat_list_add|nationID=" + gameData.nationID + "|addedNationID=" + nationID);
        }
        else
        {
            // Send chat_list_remove event to the server.
            Network.instance.SendCommand("action=chat_list_remove|nationID=" + gameData.nationID + "|removedNationID=" + nationID);
        }

        // Hide the context menu
        Deactivate();
    }

    public void OnClick_WhisperButton()
    {
        Chat.instance.InitiateWhisper(userID, username);

        // Hide the context menu
        Deactivate();
    }

    public void OnClick_ReplyButton()
    {
        // Initiate a reply to this message
        MessagesPanel.instance.OnClick_ReplyButton(nationID, nationName);

        // Hide the context menu
        Deactivate();
    }

    public void OnClick_DeleteButton()
    {
        // Delete this message
        MessagesPanel.instance.DeleteMessage(time);

        // Hide the context menu
        Deactivate();
    }

    public void OnClick_MuteButton()
    {
        if (Chat.instance.IsUserMuted(userID, deviceID))
        {
            // Unmute the user
            Chat.instance.UnmuteUser(userID, username, deviceID);
        }
        else
        {
            // Mute the user
            Chat.instance.MuteUser(userID, username, deviceID);
        }

        // Hide the context menu
        Deactivate();
    }

    public void OnClick_ReportButton()
    {
        // Display the report dialog.
        ReportDialog.instance.Activate(userID, username, text);

        // Hide the context menu
        Deactivate();
    }

    public void OnClick_AllianceInvitationButton()
    {
        string message = "";
        string yes = LocalizationManager.GetTranslation("Generic Text/yes_word");
        string no = LocalizationManager.GetTranslation("Generic Text/no_word");

        // GB-Localization
        if (nationIsAlly)
        {
            // "Break " + GameData.instance.nationName + "'s alliance with " + nationName + "?"
            // "Break {[NATION_NAME]}'s alliance with {[NATION_NAME2]}?"
            message = LocalizationManager.GetTranslation("Chat Context/confirm_break_alliance")
                .Replace("{[NATION_NAME]}", GameData.instance.nationName)
                .Replace("{[NATION_NAME2]}", nationName);

            Requestor.Activate((int)RequestorTask.BreakAlliance, nationID, this, message, yes, no);
        }
        else if (nationWasSentAllianceInvitation)
        {
            // "Withdraw the alliance invitation that was sent to " + nationName + "?"
            // "Withdraw the alliance invitation that was sent to {[NATION_NAME]}?"
            message = LocalizationManager.GetTranslation("Chat Context/confirm_withdraw_ally_invitation")
                .Replace("{[NATION_NAME]}", nationName);

            Requestor.Activate((int)RequestorTask.WithdrawAllianceInvitation, nationID, this, message, yes, no);
        }
        else
        {
            // "Send an invitation to " + nationName + " to form an alliance with " + GameData.instance.nationName + "?"
            // "Send an invitation to {[NATION_NAME]} to form an alliance with {[NATION_NAME2]}?"
            message = LocalizationManager.GetTranslation("Chat Context/confirm_alliance_suggestion_with_other_nation")
                .Replace("{[NATION_NAME]}", nationName)
                .Replace("{[NATION_NAME2]}", GameData.instance.nationName);

            Requestor.Activate((int)RequestorTask.SendAllianceInvitation, nationID, this, message, yes, no);
        }

        // Hide the context menu
        Deactivate();
    }

    public void OnClick_PatronButton()
    {
        string message = "";
        string yes = LocalizationManager.GetTranslation("Generic Text/yes_word");
        string no = LocalizationManager.GetTranslation("Generic Text/no_word");

        message = LocalizationManager.GetTranslation("Chat Context/patron_offer_query").Replace("{username}", username); // "Offer to be " + username + "'s patron?";

        Requestor.Activate((int)RequestorTask.PatronOffer, userID, this, message, yes, no);

        // Hide the context menu
        Deactivate();
    }

    public void OnClick_NationInfoButton()
    {
        // Send request_nation_info event to the server.
        Network.instance.SendCommand("action=request_nation_info|targetNationID=" + nationID);

        // Hide the context menu
        Deactivate();
    }

    public void RequestorResponse(int _task, int _data, Requestor.RequestorButton _result)
    {
        if (_result == Requestor.RequestorButton.LeftButton)
        {
            if ((RequestorTask)_task == RequestorTask.BreakAlliance)
            {
                // Send event_break_alliance event to the server.
                Network.instance.SendCommand("action=event_break_alliance|targetNationID=" + _data);
            }
            else if ((RequestorTask)_task == RequestorTask.SendAllianceInvitation)
            {
                // Send event_request_alliance event to the server.
                Network.instance.SendCommand("action=event_request_alliance|targetNationID=" + _data);
            }
            else if ((RequestorTask)_task == RequestorTask.WithdrawAllianceInvitation)
            {
                // Send event_withdraw_alliance event to the server.
                Network.instance.SendCommand("action=event_withdraw_alliance|targetNationID=" + _data);
            }
            else if ((RequestorTask)_task == RequestorTask.PatronOffer)
            {
                // Send event_withdraw_alliance event to the server.
                Network.instance.SendCommand("action=event_patron_offer|targetUserID=" + _data);
            }
        }
    }
}
