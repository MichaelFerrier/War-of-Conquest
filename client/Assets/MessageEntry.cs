using UnityEngine;
using UnityEngine.UI;
using UnityEngine.EventSystems;
using System;
using I2.Loc;

public class MessageEntry : MonoBehaviour, IPointerDownHandler
{
    public Toggle unread_toggle;
    public Text sender_text, timestamp_text;
    public TMPro.TextMeshProUGUI message_text;
    public Button reply_button;
    public int userID, nationID, deviceID, reported, time, link_base_index;
    public string username, nation_name, text;
        
    public void Init(bool _unread, int _userID, int _nationID, int _deviceID, string _username, string _nation_name, string _text, string _timestamp, int _time, int _reported)
    {
        // Record base index for this message's links.
        link_base_index = MessagesPanel.instance.linkManager.GetNumLinks();

        // Record given data
        userID = _userID;
        nationID = _nationID;
        deviceID = _deviceID;
        username = _username;
        nation_name = _nation_name;
        text = Format(_text);
        reported = _reported;
        time = _time;

        // Fill in values
        unread_toggle.isOn = _unread;
        sender_text.text = (_nationID == -1) ? LocalizationManager.GetTranslation("Generic Text/war_of_conquest") : ((_nationID == GameData.instance.nationID) ? _username : (_username + " of " + _nation_name));
        timestamp_text.text = _timestamp;
        message_text.text = text;

        // Set up the buttons' listeners.
        reply_button.onClick.RemoveAllListeners();
        reply_button.onClick.AddListener(() => ReplyButtonPressed());

        // Determine which background image to used, and whether reply button will be active, based on origin of this message.
        if (nationID == GameData.instance.nationID)
        {
            gameObject.GetComponent<Image>().sprite = MessagesPanel.instance.background_same_nation;
            reply_button.gameObject.SetActive(true);
        }
        else if (nationID == -1)
        {
            gameObject.GetComponent<Image>().sprite = MessagesPanel.instance.background_admin;
            reply_button.gameObject.SetActive(false);
        }
        else
        {
            gameObject.GetComponent<Image>().sprite = MessagesPanel.instance.background_other_nation;
            reply_button.gameObject.SetActive(true);
        }
    }

    public void ReplyButtonPressed()
    {
        MessagesPanel.instance.OnClick_ReplyButton(nationID, nation_name);
    }

    string Format(string _text)
    {
        int start_index = -1, start_val_index, end_val_index, seperator_index, x = 0, z = 0, nationID = -1;
        string nationName = "";

        try
        {
            // Parse location tags
            while ((start_index = _text.IndexOf("<location=", start_index + 1)) != -1)
            {
                start_val_index = start_index + 10;
                seperator_index = _text.IndexOf(",", start_val_index);
                x = 0;
                if (seperator_index != -1) 
                {
                    end_val_index = _text.IndexOf(">", seperator_index);
                    if (end_val_index != -1)
                    {
                        // Parse x and z positions.
                        x = Int32.Parse(_text.Substring(start_val_index, seperator_index - start_val_index));
                        z = Int32.Parse(_text.Substring(seperator_index + 1, end_val_index - seperator_index - 1));

                        // Replace the locaton tag with the location link.
                        _text = _text.Substring(0, start_index) + "<link=\"" + MessagesPanel.instance.linkManager.GetNumLinks() + "\"><color=\"yellow\"><u>" + MapView.instance.GetMapLocationText(x,z,true) + "</u></color></link>" + _text.Substring(end_val_index + 1);

                        // Record this link.
                        MessagesPanel.instance.linkManager.AddLink(LinkManager.LinkType.LOCATION, x | (z << 16));
                    }
                }
            }

            // Parse nation tags
            while ((start_index = _text.IndexOf("<nation=", start_index + 1)) != -1)
            {
                start_val_index = start_index + 8;
                seperator_index = _text.IndexOf("|", start_val_index);
                x = 0;
                if (seperator_index != -1) 
                {
                    end_val_index = _text.IndexOf("|", seperator_index + 1);
                    if (end_val_index != -1)
                    {
                        // Parse nation ID and name.
                        nationID = Int32.Parse(_text.Substring(start_val_index, seperator_index - start_val_index));
                        nationName = _text.Substring(seperator_index + 1, end_val_index - seperator_index - 1);

                        // Replace the nation tag with the nation link (unless the ID is -1, meaning incognito).
                        if (nationID == -1) {
                            _text = _text.Substring(0, start_index) + LocalizationManager.GetTranslation("an_incognito_nation") + _text.Substring(end_val_index + 2);
                        } else {
                            _text = _text.Substring(0, start_index) + "<link=\"" + MessagesPanel.instance.linkManager.GetNumLinks() + "\"><color=\"yellow\"><u>" + nationName + "</u></color></link>" + _text.Substring(end_val_index + 2);
                        }

                        // Record this link.
                        MessagesPanel.instance.linkManager.AddLink(LinkManager.LinkType.NATION, nationID);
                    }
                }
            }
        }
        catch (Exception e)
        {
            Debug.Log("Exception in MessageEntry.Format() for text '" + _text + "': " + e);
        }

        return _text;
    }

    public void OnPointerDown(PointerEventData _eventData)
    {
        // Determine whether link text has been clicked.
        int link_index = TMPro.TMP_TextUtilities.FindIntersectingLink(message_text, Input.mousePosition, null);

        if (link_index != -1)
        {
            LinkManager.LinkType type = MessagesPanel.instance.linkManager.link_types[link_base_index + link_index];
            int id = MessagesPanel.instance.linkManager.link_ids[link_base_index + link_index];

            //Debug.Log("Link " + (link_base_index + link_index) + ": type: " + type + ", id: " + id);

            if (type == LinkManager.LinkType.LOCATION)
            {
                int x = (int)((uint)id & 0x0000FFFF);
                int z = (int)(((uint)id & 0xFFFF0000) >> 16);

                if (GameData.instance.mapMode == GameData.MapMode.MAINLAND)
                {
                    // Send message to server, to set view to the map location of this link.
                    Network.instance.SendCommand("action=event_center_on_block|blockX=" + x + "|blockY=" + z, true);
                }
            }
            else if (type == LinkManager.LinkType.NATION)
            {
                // Send request_nation_info event to the server.
                Network.instance.SendCommand("action=request_nation_info|targetNationID=" + id);
            }
        }
    }
}
