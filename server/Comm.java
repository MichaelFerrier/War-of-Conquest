//
// War of Conquest Server
// Copyright (c) 2002-2023 Michael Ferrier, IronZog LLC
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
//

package WOCServer;

import java.util.*;
import java.util.HashMap;
import java.io.*;
import WOCServer.*;

// Use ISO-8859-1 encoding

public class Comm
{
  static int COMM_BUFFER_LENGTH = 8192;
  static StringBuffer comm_buffer = new StringBuffer(COMM_BUFFER_LENGTH);
  static int FILTER_BUFFER_LENGTH = 1024;
  static StringBuffer filter_buffer = new StringBuffer(FILTER_BUFFER_LENGTH);
  static StringBuffer filter_modify_buffer = new StringBuffer(FILTER_BUFFER_LENGTH);
	static int prev_general_chat_userID = -1;

	public static int CONTACT_VALUE_SOCIAL_FRIENDS = 10000;
	public static int CONTACT_VALUE_PATRON = 30;
	public static int CONTACT_VALUE_FOLLOWER = 20;
	public static int CONTACT_VALUE_WHISPER_TARGET = 15;
	public static int CONTACT_VALUE_NATION_CHAT_MESSAGE_TARGET = 10;
	public static int CONTACT_VALUE_GENERAL_CHAT_FOLLOW = 1;
	public static int CONTACT_VALUE_NATION_ALLY = 20;
	public static int CONTACT_VALUE_PREV_NEW_USER = 20;
	public static int CONTACT_VALUE_JOIN_NATION = 50;

	public static int MAX_NUM_CONTACTS = 50;
	public static int CONTACT_ACTIVE_THRESHOLD = 7;

	public static int MAX_NUM_PATRON_OFFER = 20;

	public static void ChatInput(StringBuffer _output_buffer, int _userID, int _deviceID, int _channel, String _text)
	{
		//Output.PrintToScreen("ChatInput() from user " + _userID + " on channel " + _channel + " for text '" + _text + "'");

		if (_userID <= 0)
    {
      Output.PrintToScreen("PROBLEM: Attempt to chat by userID " + _userID);
      return;
    }

		if ((_text.length() > 0) && (_text.charAt(0) == '/'))
		{
			//Output.PrintToScreen("Chat command detected.");
			ChatCommand.ProcessChatCommand(_output_buffer, _userID, _deviceID, _text);
			return;
		}

    // Get the user's data
    UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

    // Determine whether the user is a moderator
    boolean user_is_mod = (userData.mod_level != 0);

    // Get the user's nationID, nation data and nation name.
    int nationID = userData.nationID;
    NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);
    String nation_name = nationData.name;

    // Get the username
    String name = userData.name;

		// Determine and record the user's new prev_chat_fine_time and mean_chat_interval
    long prev_chat_fine_time = userData.prev_chat_fine_time;
    long cur_fine_time = Constants.GetFineTime();
    long cur_chat_interval = cur_fine_time - prev_chat_fine_time;
    long mean_chat_interval = userData.mean_chat_interval;
    mean_chat_interval = (long)((mean_chat_interval * Constants.OLD_CHAT_INTERVAL_WEIGHT) + (cur_chat_interval * Constants.NEW_CHAT_INTERVAL_WEIGHT));
    userData.mean_chat_interval = mean_chat_interval;
    userData.prev_chat_fine_time = cur_fine_time;

    // Determine whether player is banned from chat.
    boolean chat_banned = (userData.chat_ban_end_time >= Constants.GetTime());

    // If chat frequency is considered spamming, and the channel is general chat, automatically ignore it.
    if ((mean_chat_interval < Constants.MEAN_CHAT_INTERVAL_SPAMMY) && (_channel == Constants.CHAT_CHANNEL_GENERAL)) {
      return;
    }

		// Remove emojis
		_text = _text.replaceAll("[^\\p{L}\\p{M}\\p{N}\\p{P}\\p{Z}\\p{Cf}\\p{Cs}\\s]","");

    // Constrain length of line to MAX_CHAT_LINE_LENGTH
    if (_text.length() > Constants.MAX_CHAT_LINE_LENGTH) {
      _text = _text.substring(0, Constants.MAX_CHAT_LINE_LENGTH - 1);
    }

		if (_text.length() > 10)
		{
			// Determine number of upper and lower case characters
			int num_upper_case = 0, num_lower_case = 0;
			char cur_char;
			for (int i = 0; i < _text.length(); i++)
			{
				cur_char = _text.charAt(i);

				if ((((int)(cur_char)) >= 65) && (((int)(cur_char)) <= 90)) {
					num_upper_case++;
				} else if ((((int)(cur_char)) >= 97) && (((int)(cur_char)) <= 122))	{
					num_lower_case++;
				}
			}

			// If there are more upper case characters than lower case, make the whole string lower case
			if (num_upper_case > num_lower_case) {
				_text = _text.toLowerCase();
			}
		}

    // Filter the chat text
    boolean altered = FilterText(_text, name);

		// If the filter has altered the text, fetch filtered text.
		String filtered_text = altered ? filter_buffer.toString() : "";

		// If whispering, get information about whisper recipient before creating chat event.
		int whisperRecipientID = -1;
		String whisperRecipientUsername = "";
		UserData whisperRecipientUserData = null;
		if (_channel < 0)
		{
			// Recover the recipient's user ID from the channel ID. The channel ID for a whisper is the negative of the sender user ID XOR the recipient user ID.
			whisperRecipientID = (-_channel) ^ _userID;

			// Get the whisper recipient's username.
			whisperRecipientUserData = (UserData)DataManager.GetData(Constants.DT_USER, whisperRecipientID, false);

			if (whisperRecipientUserData != null)
			{
				// Get the whisper recipient's name.
				whisperRecipientUsername = whisperRecipientUserData.name;

				// Record the user's contact to the whisper recipient.
				Comm.RecordContact(userData, whisperRecipientID, Comm.CONTACT_VALUE_WHISPER_TARGET);
			}
		}

		// If this chat input is to general chat...
		if (_channel == Constants.CHAT_CHANNEL_GENERAL)
		{
			// Record contact between this user, and the previous user who communicated in general chat.
			if (prev_general_chat_userID != -1) {
				Comm.RecordContact(userData, prev_general_chat_userID, Comm.CONTACT_VALUE_GENERAL_CHAT_FOLLOW);
			}

			// Record userID of latest contributor to general chat.
			prev_general_chat_userID = _userID;
		}

    // Construct chat event
    comm_buffer.setLength(0);
    Constants.EncodeString(comm_buffer, "event_chat");
    Constants.EncodeUnsignedNumber(comm_buffer, _userID, 5);
    Constants.EncodeUnsignedNumber(comm_buffer, nationID, 5);
		Constants.EncodeUnsignedNumber(comm_buffer, _deviceID, 5);
    Constants.EncodeString(comm_buffer, name);
    Constants.EncodeString(comm_buffer, nation_name);
		Constants.EncodeUnsignedNumber(comm_buffer, nationData.flags, 4);
    Constants.EncodeNumber(comm_buffer,_channel,5);
		Constants.EncodeString(comm_buffer, (_channel < 0) ? whisperRecipientUsername : "");
    Constants.EncodeString(comm_buffer, _text);
		Constants.EncodeString(comm_buffer, filtered_text);
    Constants.EncodeNumber(comm_buffer, userData.mod_level, 1);
    Constants.EncodeString(comm_buffer, "end");
    comm_buffer.append('\u0000'); // NULL terminate output string so that it can be sent.
    String chat_event = comm_buffer.toString();

    // Log this chat event if appropriate
    if ((WOCServer.log_flags & Constants.LOG_CHAT) != 0)
    {
      if (_channel < 0)
      {
        // Log this whisper
        Output.PrintToScreen("Whisper " + name + "(" + _userID + ") of " + nation_name + "(" + nationID + ") to " + whisperRecipientUserData.name + ":\"" + _text + "\"");
      }
      else
      {
				// Log this chat message
        Output.PrintToScreen((chat_banned ? "BANNED " : "") + "Chat " + name + "(" + _userID + ") of " + nation_name + "(" + nationID + ") channel " + _channel + ":\"" + _text + "\"");
      }
    }

    if (userData.mod_level > 0)
    {
      // Log this moderator chat event
      Constants.WriteToLog("log_mod.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + ", " + name + "(" + _userID + ") of " + nation_name + ":\"" + _text + "\"\n");
    }

		if (_channel < 0) // Whisper to an individual player
    {
			ClientThread targetClientThread = WOCServer.GetClientThread(whisperRecipientID);

			if ((targetClientThread == null) || (targetClientThread.UserIsLoggedIn() == false)) {
					OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_whisper_not_logged_in")); // "That player is not logged in."
					return;
			}

			// Send the chat event to the sender's own client.
			_output_buffer.append(chat_event);

			// Send the chat event to the recipient player's client.
			OutputEvents.SendToClient(chat_event, targetClientThread);
    }
    else if (_channel == Constants.CHAT_CHANNEL_ALLIES)
    {
      // Send the chat event to the sender's nation
			OutputEvents.BroadcastToNation(chat_event, nationID);

      // Send the chat event to each of the nation's ally nations.
      for (int cur_ally_index = 0; cur_ally_index < nationData.alliances_active.size(); cur_ally_index++) {
				OutputEvents.BroadcastToNation(chat_event, nationData.alliances_active.get(cur_ally_index));
      }
    }
    else if (_channel == Constants.CHAT_CHANNEL_GENERAL)
    {
      if (chat_banned)
      {
				// Send message alerting player that they are banned from general chat, and return.
				OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_chat_general_banned")); // "You are currently banned from general chat."
        Output.PrintToScreen("Chat text from '" + name + "' BANNED from general chat.");
        return;
      }
      else
      {
        // Send the chat event to all users currently in game.
				OutputEvents.BroadcastToAllInGame(chat_event);
      }
    }
		else if (_channel >= 0) // Chat on an individual nation's private channel.
    {
      //if ((chat_banned) && (_channel != nationID))
      //{
      //  // Send report to the user's nation
      //  OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_chat_nation_banned")); // "You are currently banned from chatting on other nations' channels."
      //  Output.PrintToScreen("Chat text from '" + name + "' BANNED from list chat.");
      //  return;
      //}
      //else
      //{
        // Send the chat text to the channel's nation, and all nations on its chat list.
				OutputEvents.BroadcastToAllOnChatList(chat_event, _channel);

				// Record contact to this nation's players.
				Comm.RecordContactWithNation(userData, _channel, Comm.CONTACT_VALUE_NATION_CHAT_MESSAGE_TARGET, false);
      //}
    }
	}

	public static void Mute(int _userID, int _userID_to_mute, int _deviceID_to_mute)
	{
	  // Get the user's data
    UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

	  // Get the user to mute's data
    UserData userToMuteData = (UserData)DataManager.GetData(Constants.DT_USER, _userID_to_mute, false);

		// Add the _userID_to_mute to the user's muted_users list.
		if (!userData.muted_users.contains(_userID_to_mute)) userData.muted_users.add(_userID_to_mute);

		// Add the _deviceID_to_mute to the user's muted_devices list.
		if (!userData.muted_devices.contains(_deviceID_to_mute)) userData.muted_devices.add(_deviceID_to_mute);

		// Add each of the muted user's devices to the muted devices list.
		for (int i = 0; i < userToMuteData.devices.size(); i++)
		{
			int deviceID = userToMuteData.devices.get((int)i);

			// Add the current deviceID to the user's muted_devices list.
			if (!userData.muted_devices.contains(deviceID)) userData.muted_devices.add(deviceID);
		}

		// Mark the user data to be updated.
		DataManager.MarkForUpdate(userData);
	}

	public static void Unmute(int _userID, int _userID_to_unmute, int _deviceID_to_unmute)
	{
	  // Get the user's data
    UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

	  // Get the user to unmute's data
    UserData userToUnmuteData = (UserData)DataManager.GetData(Constants.DT_USER, _userID_to_unmute, false);

		// Remove the _userID_to_unmute from the user's muted_users list.
		userData.muted_users.remove((Integer)_userID_to_unmute);

		// Remove the _deviceID_to_unmute from the user's muted_devices list.
		userData.muted_devices.remove((Integer)_deviceID_to_unmute);

		// Remove each of the muted user's devices from the muted devices list.
		for (int i = 0; i < userToUnmuteData.devices.size(); i++)
		{
			int deviceID = userToUnmuteData.devices.get((int)i);

			// Remove the current deviceID from the user's muted_devices list.
			userData.muted_devices.remove((Integer)deviceID);
		}

		// Mark the user data to be updated.
		DataManager.MarkForUpdate(userData);
	}

	public static void UnmuteAll(int _userID)
	{
	  // Get the user's data
    UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		// Clear the user's lists of muted users and devices.
		userData.muted_users.clear();
		userData.muted_devices.clear();

		// Mark the user data to be updated.
		DataManager.MarkForUpdate(userData);
	}

	public static void SendAdminAnnouncement(String _text)
	{
		SendAdminAnnouncement(ClientString.Get(_text));
	}

	// Note: Because this method is not necessarily event driven, it should use its own event buffer.
	public static void SendAdminAnnouncement(ClientString _text)
	{
    // Construct chat event
    comm_buffer.setLength(0);
		OutputEvents.GetAnnouncementEvent(comm_buffer, _text);
    Constants.EncodeString(comm_buffer, "end");
    comm_buffer.append('\u0000'); // NULL terminate output string so that it can be sent.
    String event_string = comm_buffer.toString();

		// Send the announcement event to all users currently in game.
		OutputEvents.BroadcastToAllInGame(event_string);
	}

  public static boolean FilterText(String _text, String _username)
  {
    filter_buffer.setLength(0);
    filter_buffer.append(_text);

    filter_modify_buffer.setLength(0);
    filter_modify_buffer.append(_text);

    // Perform character substitution
    char cur_char;
    int final_pos = 0;
    for (int i = 0; i < filter_modify_buffer.length(); i++)
    {
      cur_char = filter_modify_buffer.charAt(i);

      switch (cur_char)
      {
        case '1': cur_char = 'i'; break;
        case '0': cur_char = 'o'; break;
        case '3': cur_char = 'e'; break;
        case '5': cur_char = 's'; break;
        case '$': cur_char = 's'; break;
        case '@': cur_char = 'a'; break;
        case '|': cur_char = 'l'; break;
        case '¦': cur_char = 'l'; break;
        case 'Ç': cur_char = 'c'; break;
        case 'ü': cur_char = 'u'; break;
        case 'é': cur_char = 'e'; break;
        case 'â': cur_char = 'a'; break;
        case 'ä': cur_char = 'a'; break;
        case 'à': cur_char = 'a'; break;
        case 'å': cur_char = 'a'; break;
        case 'ç': cur_char = 'c'; break;
        case 'ê': cur_char = 'e'; break;
        case 'ë': cur_char = 'e'; break;
        case 'è': cur_char = 'e'; break;
        case 'ï': cur_char = 'i'; break;
        case 'î': cur_char = 'i'; break;
        case 'ì': cur_char = 'i'; break;
        case 'Ä': cur_char = 'a'; break;
        case 'Å': cur_char = 'a'; break;
        case 'É': cur_char = 'e'; break;
        case 'ô': cur_char = 'o'; break;
        case 'ö': cur_char = 'o'; break;
        case 'ò': cur_char = 'o'; break;
        case 'û': cur_char = 'u'; break;
        case 'ù': cur_char = 'u'; break;
        case 'ÿ': cur_char = 'y'; break;
        case 'Ö': cur_char = 'o'; break;
        case 'Ü': cur_char = 'u'; break;
        case '¢': cur_char = 'c'; break;
        case '£': cur_char = 'f'; break;
        case '¥': cur_char = 'y'; break;
        case 'P': cur_char = 'p'; break;
        case 'ƒ': cur_char = 'f'; break;
        case 'á': cur_char = 'a'; break;
        case 'í': cur_char = 'i'; break;
        case 'ó': cur_char = 'o'; break;
        case 'ú': cur_char = 'u'; break;
        case 'ñ': cur_char = 'n'; break;
        case 'Ñ': cur_char = 'n'; break;
      }

      // Replace upper case characters with lower case characters
      if ((((int)(cur_char)) >= 65) && (((int)(cur_char)) <= 90)) {
        cur_char = (char)(((int)(cur_char)) + 32);
      }

      // Eliminate non alphabetic characters
      if ((((int)(cur_char)) >= 97) && (((int)(cur_char)) <= 122))
      {
        filter_modify_buffer.setCharAt(final_pos, cur_char);
        final_pos++;
      }
      else
      {
        // Mark this char as having been eliminated
        filter_buffer.setCharAt(i, (char)127);
      }
    }

    // Constrain length of modified string buffer
    filter_modify_buffer.setLength(final_pos);

    boolean cur_altered = false, altered = false;

    do {
      cur_altered = false;

			cur_altered = cur_altered || FilterWord("anal", true);
			cur_altered = cur_altered || FilterWord("anus", true);
			cur_altered = cur_altered || FilterWord("arse", true);
			cur_altered = cur_altered || FilterWord("asshole", false);
			cur_altered = cur_altered || FilterWord("ass", true);
			cur_altered = cur_altered || FilterWord("azz", true);
			cur_altered = cur_altered || FilterWord("ballsack", false);
			cur_altered = cur_altered || FilterWord("balls", false);
			cur_altered = cur_altered || FilterWord("bastard", false);
			cur_altered = cur_altered || FilterWord("biatch", false);
			cur_altered = cur_altered || FilterWord("bitch", false);
			cur_altered = cur_altered || FilterWord("bloody", false);
			cur_altered = cur_altered || FilterWord("blow job", false);
			cur_altered = cur_altered || FilterWord("blowjob", false);
			cur_altered = cur_altered || FilterWord("bollock", false);
			cur_altered = cur_altered || FilterWord("bollok", false);
			cur_altered = cur_altered || FilterWord("boner", false);
			cur_altered = cur_altered || FilterWord("boob", false);
			cur_altered = cur_altered || FilterWord("bugger", false);
			cur_altered = cur_altered || FilterWord("clit", false);
			cur_altered = cur_altered || FilterWord("cock", false);
			cur_altered = cur_altered || FilterWord("coon", false);
			cur_altered = cur_altered || FilterWord("cunt", false);
			cur_altered = cur_altered || FilterWord("cracker", false);
			cur_altered = cur_altered || FilterWord("craker", false);
			cur_altered = cur_altered || FilterWord("damn", false);
			cur_altered = cur_altered || FilterWord("dick", false);
			cur_altered = cur_altered || FilterWord("dildo", false);
			cur_altered = cur_altered || FilterWord("dyke", false);
			cur_altered = cur_altered || FilterWord("faggot", false);
			cur_altered = cur_altered || FilterWord("fag", true);
			cur_altered = cur_altered || FilterWord("feck", false);
			cur_altered = cur_altered || FilterWord("felat", false);
			cur_altered = cur_altered || FilterWord("felch", false);
			cur_altered = cur_altered || FilterWord("foad", false);
			cur_altered = cur_altered || FilterWord("fucker", false);
			cur_altered = cur_altered || FilterWord("fucking", false);
			cur_altered = cur_altered || FilterWord("fuck", false);
			cur_altered = cur_altered || FilterWord("fuc", false);
			cur_altered = cur_altered || FilterWord("fuk", false);
			cur_altered = cur_altered || FilterWord("fvc", false);
			cur_altered = cur_altered || FilterWord("fvk", false);
			cur_altered = cur_altered || FilterWord("gay", true);
			cur_altered = cur_altered || FilterWord("gtfo", false);
			cur_altered = cur_altered || FilterWord("homo", true);
			cur_altered = cur_altered || FilterWord("hooker", false);
			cur_altered = cur_altered || FilterWord("jizz", false);
			cur_altered = cur_altered || FilterWord("kike", false);
			cur_altered = cur_altered || FilterWord("kyk", false);
			cur_altered = cur_altered || FilterWord("lmfao", false);
			cur_altered = cur_altered || FilterWord("mf", true);
			cur_altered = cur_altered || FilterWord("milf", false);
			cur_altered = cur_altered || FilterWord("mofo", false);
			cur_altered = cur_altered || FilterWord("nigga", false);
			cur_altered = cur_altered || FilterWord("nigger", false);
			cur_altered = cur_altered || FilterWord("nogaf", false);
			cur_altered = cur_altered || FilterWord("omfg", false);
			cur_altered = cur_altered || FilterWord("penis", false);
			cur_altered = cur_altered || FilterWord("piss", false);
			cur_altered = cur_altered || FilterWord("prick", false);
			cur_altered = cur_altered || FilterWord("prik", false);
			cur_altered = cur_altered || FilterWord("pussies", false);
			cur_altered = cur_altered || FilterWord("pussy", false);
			cur_altered = cur_altered || FilterWord("queer", false);
			cur_altered = cur_altered || FilterWord("scrotum", true);
			cur_altered = cur_altered || FilterWord("shit", true);
			cur_altered = cur_altered || FilterWord("slut", false);
			cur_altered = cur_altered || FilterWord("smegma", false);
			cur_altered = cur_altered || FilterWord("spic", false);
			cur_altered = cur_altered || FilterWord("spik", false);
			cur_altered = cur_altered || FilterWord("stfu", false);
			cur_altered = cur_altered || FilterWord("tit", true);
			cur_altered = cur_altered || FilterWord("twat", true);
			cur_altered = cur_altered || FilterWord("vagina", false);
			cur_altered = cur_altered || FilterWord("wank", false);
			cur_altered = cur_altered || FilterWord("whitey", false);
			cur_altered = cur_altered || FilterWord("whity", false);
			cur_altered = cur_altered || FilterWord("whore", false);
			cur_altered = cur_altered || FilterWord("wtf", false);

      altered = altered || cur_altered;
    } while (cur_altered == true);

    if (altered)
    {
      // Log this filtering
      Constants.WriteToLog("log_filter.txt", Constants.GetFullDate() + ": user " + _username + ": \"" + _text + "\"\n");

      // Transplant the *s back into the original string.
      int source_place = -1, mod_place;
      for (mod_place = 0; mod_place <= filter_modify_buffer.length(); mod_place++)
      {
        for(;;)
        {
          source_place++;

          if (source_place >= filter_buffer.length()) {
            source_place = filter_buffer.length() - 1;
            break;
          }

          if (filter_buffer.charAt(source_place) == ((char)127))
          {
            filter_buffer.setCharAt(source_place, _text.charAt(source_place));
            continue;
          }
          else
          {
            break;
          }
        }

        // Allow to go up to here past end of modified text, to finish rest of source text
        if (mod_place == filter_modify_buffer.length()) {
          break;
        }

        if (filter_modify_buffer.charAt(mod_place) == '*')
        {
          filter_buffer.setCharAt(source_place, '*');
        }
      }
    }

    return altered;
  }

  public static boolean FilterWord(String _search_string, boolean _require_word_boundary)
  {
    int index = 0, string_length;
    boolean altered = false;

    while ((index = filter_modify_buffer.indexOf(_search_string, index)) != -1)
    {
      string_length = _search_string.length();

      if ((!_require_word_boundary) || IsWordStart(index))
      {
        // Replace the search string with *s
        for (int i = index; i < index + string_length; i++)
        {
          filter_modify_buffer.setCharAt(i, '*');
        }

        altered = true;
      }

      index += string_length;
    }

    return altered;
  }

  // Returns true if the given position in the filter_modify_buffer is, at the
  // equivalent position in the filter_buffer, preceeded by a non alphabetic character.
  public static boolean IsWordStart(int _index)
  {
    int mod_place = -1;
    char cur_char;

    for (int place = 0; place < filter_buffer.length(); place++)
    {
      cur_char = filter_buffer.charAt(place);
      if (((int)(cur_char)) != 127)
      {
        mod_place++;
      }

      if (mod_place == _index)
      {
        if (place == 0) {
          return true;
        }
        else
        {
          cur_char = filter_buffer.charAt(place - 1);
          return (((int)(cur_char)) == 127);
        }
      }
    }

    return false;
  }

  public static void ChatListAdd(int _nationID, int _addedNationID)
  {
    // Get the nation data
    NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationID, false);
    NationData addedNationData = (NationData)DataManager.GetData(Constants.DT_NATION, _addedNationID, false);

		// Do nothing if the nation's chat list already contains the added nation.
		if (nationData.chat_list.contains(_addedNationID)) {
			return;
		}

		// Broadcast an event about this addition, to each current member of the nation's chat list.
    comm_buffer.setLength(0);
    Constants.EncodeString(comm_buffer, "chat_list_add");
    Constants.EncodeUnsignedNumber(comm_buffer, _nationID, 4);
		Constants.EncodeUnsignedNumber(comm_buffer, _addedNationID, 4);
		Constants.EncodeString(comm_buffer, addedNationData.name);
    Constants.EncodeString(comm_buffer, "end");
    comm_buffer.append('\u0000'); // NULL terminate output string so that it can be sent.
    String chat_list_add_event = comm_buffer.toString();

		// Send the chat list add event to all users already on the nation's chat list.
		OutputEvents.BroadcastToAllOnChatList(chat_list_add_event, _nationID);

		// Add the addedNation to the nation's chat list, and add the nation to the addedNation's reverse chat list.
		nationData.chat_list.add(_addedNationID);
		addedNationData.reverse_chat_list.add(_nationID);

		// Mark both nations to be updated.
		DataManager.MarkForUpdate(nationData);
		DataManager.MarkForUpdate(addedNationData);

		// Send an event for the full chat list to the addedNation.
		comm_buffer.setLength(0);
		OutputEvents.GetChatListEvent(comm_buffer, _nationID);
    Constants.EncodeString(comm_buffer, "end");
    comm_buffer.append('\u0000'); // NULL terminate output string so that it can be sent.
    String chat_list_event = comm_buffer.toString();

		// Send the chat list event to all users of the added nation.
		OutputEvents.BroadcastToNation(chat_list_event, _addedNationID);

		// Generate a message event to alert everyone on the chat list about the addition.
		comm_buffer.setLength(0);
		OutputEvents.GetMessageEvent(comm_buffer, ClientString.Get("svr_chat_list_add", "added_nation_name", addedNationData.name, "nation_name", nationData.name)); // addedNationData.name + " has been added to " + nationData.name + "'s chat list"
		Constants.EncodeString(comm_buffer, "end");
    comm_buffer.append('\u0000'); // NULL terminate output string so that it can be sent.
    String message_event = comm_buffer.toString();

		// Send the message event to all users in the nation's chat list.
		OutputEvents.BroadcastToAllOnChatList(message_event, _nationID);
  }

  // Remove a nation from the chat list.
  public static void ChatListRemove(int _nationID, int _removedNationID)
  {
    // Get the nation data
    NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationID, false);
    NationData removedNationData = (NationData)DataManager.GetData(Constants.DT_NATION, _removedNationID, false);

		// Do nothing if the nation's chat list doesn't contain the removed nation.
		if (nationData.chat_list.contains(_removedNationID) == false) {
			return;
		}

		// Broadcast an event about this removal, as well as a message, to each current member of the nation's chat list.
    comm_buffer.setLength(0);
    Constants.EncodeString(comm_buffer, "chat_list_remove");
    Constants.EncodeUnsignedNumber(comm_buffer, _nationID, 4);
		Constants.EncodeUnsignedNumber(comm_buffer, _removedNationID, 4);
		OutputEvents.GetMessageEvent(comm_buffer, ClientString.Get("svr_chat_list_remove", "removed_nation_name", removedNationData.name, "nation_name", nationData.name)); // removedNationData.name + " has been removed from " + nationData.name + "'s chat list"
		Constants.EncodeString(comm_buffer, "end");
    comm_buffer.append('\u0000'); // NULL terminate output string so that it can be sent.
    String chat_list_remove_event = comm_buffer.toString();

		// Send the chat list remove event to all users on the nation's chat list.
		OutputEvents.BroadcastToAllOnChatList(chat_list_remove_event, _nationID);

		// Remove the removedNation from the nation's chat list, and remove the nation to the removedNation's reverse chat list.
		nationData.chat_list.remove(Integer.valueOf(_removedNationID));
		removedNationData.reverse_chat_list.remove(Integer.valueOf(_nationID));

		// Mark both nations to be updated.
		DataManager.MarkForUpdate(nationData);
		DataManager.MarkForUpdate(removedNationData);
  }

  public static void FileReport(StringBuffer _output_buffer, int _userID, int _report_userID, String _report_username, String _report_issue, String _report_text)
  {
    // Get the reporter's user data
    UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

    // Determine current day
    int cur_day = Constants.GetAbsoluteDay();

    // Enforce the limit on the number of reports one player may enter in a day.
    if (cur_day == userData.prev_report_day)
    {
      if (userData.report_count >= Constants.MAX_REPORTS_PER_DAY)
      {
				Output.PrintToScreen("Max for day");
        OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_report_submitted_max", "max_per_day", String.valueOf(Constants.MAX_REPORTS_PER_DAY))); // "You've already submitted the maximum of " + Constants.MAX_REPORTS_PER_DAY + " reports today"
        return;
      }
      else
      {
        userData.report_count++;
      }
    }
    else
    {
      userData.prev_report_day = cur_day;
      userData.report_count = 1;
    }

		// Return a message stating that the report has been submitted.
    OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("Your report has been submitted"));

    // Mark the complainer's user data for update
    DataManager.MarkForUpdate(userData);

    // Get the violator's user data
    UserData report_userData = (UserData)DataManager.GetData(Constants.DT_USER, _report_userID, false);

    if (report_userData == null) {
      return;
    }

		NationData report_nationData = (NationData)DataManager.GetData(Constants.DT_NATION, report_userData.nationID, false);

    if (report_nationData == null) {
      return;
    }

		// Determine the number of reports required to ban this user from chat, which depends on their chat_offense_level.
		int num_reports_for_ban = Constants.CHAT_BAN_REPORTS_COUNT__LOW_OFFENSE;

		if (report_userData.chat_offense_level > Constants.HIGH_OFFENSE_LEVEL_THRESHOLD) {
			num_reports_for_ban = Constants.CHAT_BAN_REPORTS_COUNT__HIGH_OFFENSE;
		}
		else if (report_userData.chat_offense_level > Constants.MEDIUM_OFFENSE_LEVEL_THRESHOLD) {
			num_reports_for_ban = Constants.CHAT_BAN_REPORTS_COUNT__MEDIUM_OFFENSE;
		}

		int cur_time = Constants.GetTime();

		// Remove obsolete reports from both the short and long term report hashmaps.
		RemoveObsoleteReports(report_userData.long_term_reports, cur_time - Constants.LONG_TERM_REPORT_DURATION);
		RemoveObsoleteReports(report_userData.short_term_reports, cur_time - Constants.SHORT_TERM_REPORT_DURATION);

		// Record the complaint in both player accounts.
		userData.UpdateComplaintAndBanCounts(1, 0, 0, 0, 0);
		report_userData.UpdateComplaintAndBanCounts(0, 1, 0, 0, 0);

		// Record a complaint representing this report.
		int complaintID = DataManager.GetNextDataID(Constants.DT_COMPLAINT);
		ComplaintData complaintData = (ComplaintData)DataManager.GetData(Constants.DT_COMPLAINT, complaintID, true); // Create
		complaintData.timestamp = Constants.GetTime();
		complaintData.userID = _userID;
		complaintData.reported_userID = _report_userID;
		complaintData.issue = _report_issue;
		complaintData.text = _report_text;
		DataManager.MarkForUpdate(complaintData);
		GlobalData.instance.complaints.add(complaintID);
		DataManager.MarkForUpdate(GlobalData.instance);

    // Log the report
    Constants.WriteToLog("log_chat_report.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " Complaint " + complaintID + " submitted by " + userData.name + "(" + _userID + ") against " + report_userData.name + "(" + _report_userID + ") of nation " + report_nationData.name + "(" + report_userData.nationID + ") for issue \"" + _report_issue + "\". Reported text: \"" + _report_text + "\". Previously " + report_userData.short_term_reports.size() + "/" + num_reports_for_ban + " reports.\n");

		if (report_userData.long_term_reports.containsKey(_userID))
		{
			// The user has filed a report on the reported user within the LONG_TERM_REPORT_DURATION period. Update the long term record, and otherwise ignore this report.
			report_userData.long_term_reports.put(_userID, cur_time);
			return;
		}

		// This report will count; add it to the reported user's short_term_reports table.
		report_userData.short_term_reports.put(_userID, cur_time);

		// If the threshold for banning the user has not been met, do nothing further.
		if (report_userData.short_term_reports.size() < num_reports_for_ban) {
			return;
		}

		// Record the ban
		RecordChatBan(report_userData, Constants.CHAT_BAN_DURATION, 1.0f);

    // Mark the reported user's data for update
    DataManager.MarkForUpdate(report_userData);

    // Log the banning
    Constants.WriteToLog("log_ban.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " User " + report_userData.name + "(" + _report_userID + ") of nation " + report_nationData.name + "(" + report_userData.nationID + ") (offense level " + report_userData.chat_offense_level + ")banned from chat due to report " + report_userData.short_term_reports.size() + " submitted by " + userData.name + "(" + _userID + ") for issue \"" + _report_issue + "\". Reported text: \"" + _report_text + "\"\n");
  }

	public static void RecordChatBan(UserData _userData, int _ban_duration, float _offense_level_delta)
	{
		// Determine when ban will end.
		int ban_end_time = Constants.GetTime() + _ban_duration;

		// Record ban and change to the the user's chat_offense_level.
		_userData.chat_ban_end_time = ban_end_time;
		_userData.chat_offense_level += _offense_level_delta;

		// Record the ban in the player's account.
		_userData.UpdateComplaintAndBanCounts(0, 0, 0, 1, 0);

		// Copy the user's bans to ist asociated users and devices.
		_userData.CopyBansToAssociatedUsersAndDevices();

		// If the user is currently logged in...
		if (_userData.client_thread != null)
		{
			// Send a notification of this chat ban, and a message, to the banned user.
			comm_buffer.setLength(0);
			OutputEvents.GetChatBanEvent(comm_buffer, _userData.ID);
			if (_ban_duration > 0) {
				OutputEvents.GetMessageEvent(comm_buffer, ClientString.Get("svr_chat_temp_ban")); // "You have been temporarily banned from general chat"
			}
			Constants.EncodeString(comm_buffer, "end");
			comm_buffer.append('\u0000'); // NULL terminate output string so that it can be sent.
			OutputEvents.SendToClient(comm_buffer.toString(), _userData.client_thread);

			Output.PrintToScreen("ban_end_time of " + ban_end_time + " associated with client ID " + _userData.client_thread.clientID);
		}
	}

	public static void RemoveObsoleteReports(HashMap<Integer,Integer> _reports_map, int _cutoff_time)
	{
		Iterator<Map.Entry<Integer,Integer>> iter = _reports_map.entrySet().iterator();
		while (iter.hasNext()) {
				Map.Entry<Integer,Integer> entry = iter.next();
				if(entry.getValue() <= _cutoff_time){
						iter.remove();
				}
		}
	}

	public static ClientString PostMessage(int _userID, int _deviceID, String _recipient_nation_name, String _text)
	{
		// Get the user's data
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		// If user is banned from chat, do not allow sending of messages
		if (userData.chat_ban_end_time > Constants.GetTime()) {
			return ClientString.Get("svr_post_message_muted"); // "Players banned from chat cannot send messages."
		}

		// Get the user's nationID
		int userNationID = userData.nationID;

		// Get the user's nation's data
		NationData userNationData = (NationData)DataManager.GetData(Constants.DT_NATION, userNationID, false);

		// Determine the recipient nation's ID
		int recipientNationID = DataManager.GetNameToIDMap(Constants.DT_NATION, _recipient_nation_name);

		if (recipientNationID == -1) {
			return ClientString.Get("svr_post_message_no_such_nation", "nation_name", _recipient_nation_name); // "'" + _recipient_nation_name + "' is not a known nation."
		}

		// Get the recipient nation's extended data
		NationExtData recipientNationExtData = (NationExtData)DataManager.GetData(Constants.DT_NATION_EXT, recipientNationID, false);

		// Remove any obsolete messages
		while ((recipientNationExtData.messages.size() > 0) && ((Constants.GetTime() - recipientNationExtData.messages.get(0).time) > Constants.MESSAGE_DURATION)) {
			recipientNationExtData.messages.remove(0);
		}

		// If the nation's list of messages is full, delete the oldest messages until there is room.
		while (recipientNationExtData.messages.size() >= Constants.MAX_NUM_MESSAGES) {
			recipientNationExtData.messages.remove(0);
		}

		//// If the nation's list of messages is full, return error message.
		//if (recipientNationExtData.messages.size() >= Constants.MAX_NUM_MESSAGES) {
		//	return ClientString.Get("svr_post_message_list_full"); // "That nation's message list is full."
		//}

		// Remove emojis
		_text = _text.replaceAll("[^\\p{L}\\p{M}\\p{N}\\p{P}\\p{Z}\\p{Cf}\\p{Cs}\\s]","");

		// Limit the length of the message text string
		if (_text.length() > Constants.MAX_MESSAGE_LENGTH) {
			_text = _text.substring(0, Constants.MAX_MESSAGE_LENGTH - 1);
		}

		int cur_day = Constants.GetAbsoluteDay();

		if (userNationData.prev_message_send_day == cur_day)
		{
			if (userNationData.message_send_count >= Constants.MAX_MESSAGE_SEND_COUNT_PER_DAY)
			{
				return ClientString.Get("svr_post_message_sent_max_num", "max_per_day", String.valueOf(Constants.MAX_MESSAGE_SEND_COUNT_PER_DAY)); // "We've already sent the maximum of " + Constants.MAX_MESSAGE_SEND_COUNT_PER_DAY + " messages today."
			}
			else
			{
				userNationData.message_send_count++;
			}
		}
		else
		{
			userNationData.prev_message_send_day = cur_day;
			userNationData.message_send_count = 1;
		}

    // Filter the chat text
    boolean altered = FilterText(_text, userData.name);
		if (altered) {
			_text = filter_buffer.toString();
		}

		// Record contact between user and recipient nation.
		Comm.RecordContactWithNation(userData, recipientNationID, Comm.CONTACT_VALUE_NATION_CHAT_MESSAGE_TARGET, false);

		// Mark the sending user's nation's data to be updated
		DataManager.MarkForUpdate(userNationData);

		// Deliver the message to the recipient nation.
		DeliverMessage(_userID, userData.name, userNationID, _deviceID, userNationData.name, _text, recipientNationExtData, 0);

		return ClientString.Get(""); // No error
	}

	public static void DeliverMessage(int _sender_userID, String _sender_username, int _sender_nationID, int _sender_deviceID, String _sender_nation_name, String _message_text, NationExtData _recipient_nation_ext_data, int _delay)
	{
		// Create a MessageData object to represent this message
		MessageData message_data_object = new MessageData();
		message_data_object.time = Constants.GetTime();
		message_data_object.userID = _sender_userID;
		message_data_object.nationID = _sender_nationID;
		message_data_object.deviceID = _sender_deviceID;
		message_data_object.username = _sender_username;
		message_data_object.nation_name = _sender_nation_name;
		message_data_object.timestamp = Constants.GetTimestampString();
		message_data_object.text = _message_text;
    message_data_object.reported = 0;

		// Add this message to the recipient nation's list of messages
		_recipient_nation_ext_data.messages.add(message_data_object);

		// While there are too many messages in the recipient's list, remove the first (earliest) message.
		while (_recipient_nation_ext_data.messages.size() > Constants.MAX_NUM_MESSAGES) {
			_recipient_nation_ext_data.messages.remove(0);
		}

		// Broadcast the new message to all online players of the recipient nation.
		OutputEvents.BroadcastNewMessageEvent(_recipient_nation_ext_data.ID, _delay, message_data_object);

		// Mark the recipient nations' extended data to be updated
		DataManager.MarkForUpdate(_recipient_nation_ext_data);
	}

	public static void DeleteMessage(StringBuffer _output_buffer, int _userID, int _message_time)
	{
		// Get the user's data
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		// Get the user's nation's extended data
		int nationID = userData.nationID;
		NationExtData nationExtData = (NationExtData)DataManager.GetData(Constants.DT_NATION_EXT, nationID, false);

		// Remove the message with the given time from the nation's list of messages.
		Iterator<MessageData> it = nationExtData.messages.iterator();
		MessageData cur_message_data;
		while (it.hasNext())
		{
			cur_message_data = it.next();
			if (cur_message_data.time == _message_time) {
					it.remove();
			}
		}

		// Mark this nation's extended data to be updated
		DataManager.MarkForUpdate(nationExtData);
	}

	public static void MessagesChecked(StringBuffer _output_buffer, int _userID)
	{
		// Get the user's data
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		// Record time when messages checked
		userData.prev_check_messages_time = Constants.GetTime();

		// Mark this user's data to be updated
		DataManager.MarkForUpdate(userData);
	}

	public static void FetchMoreMessages(StringBuffer _output_buffer, int _userID, int _type, int _start)
	{
		OutputEvents.GetMoreMessagesEvent(_output_buffer, _userID, _type, _start);
	}

	public static void SendReport(int _nationID, ClientString _text, int _delay)
  {
		// Get the recipient nation's extended data.
		NationExtData nationExtData = (NationExtData)DataManager.GetData(Constants.DT_NATION_EXT, _nationID, false);

		// Send the report as a message to the given nation.
		Comm.DeliverMessage(-1, "", -1, -1, "", _text.GetJSON(), nationExtData, _delay);
  }

	public static void RecordContact(int _userID, int _contactUserID, int _amount)
	{
		// Get the user's data
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		if (userData != null)
		{
			// Record the contact
			RecordContact(userData, _contactUserID, _amount);
		}
	}

	public static void RecordContact(UserData _userData, int _contactUserID, int _amount)
	{
		if (_userData.ID == _contactUserID) {
			return;
		}

		// If the user already has a record of _contactUserID being a contact, add its existing value to the given _amount.
		if (_userData.contacts.containsKey(_contactUserID)) {
			_amount += _userData.contacts.get(_contactUserID);
		} else {
			if (_userData.contacts.size() >= MAX_NUM_CONTACTS) return;
		}

		// Record the new amount for this contact.
		_userData.contacts.put(_contactUserID, _amount);

		// Mark the user's data to be updated
		DataManager.MarkForUpdate(_userData);
	}

	public static void RecordContactWithNation(UserData _userData, int _contactNationID, int _amount, boolean _mutual)
	{
		// Get the nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _contactNationID, false);

		if (nationData != null)
		{
			// Record the contact
			RecordContactWithNation(_userData, nationData, _amount, _mutual);
		}
	}

	public static void RecordContactWithNation(UserData _userData, NationData _contactNationData, int _amount, boolean _mutual)
	{
		// Record contacts to each of the given contact nation's users.
		for (int cur_user_index = 0; cur_user_index < _contactNationData.users.size(); cur_user_index++)
		{
			// Get the current user's ID
			int userID = _contactNationData.users.get(cur_user_index);

			if (userID != _userData.ID)
			{
				RecordContact(_userData, userID, _amount);

				if (_mutual) {
					RecordContact(userID, _userData.ID, _amount);
				}
			}
		}
	}

	public static void RecordNationContactWithNation(NationData _nationData, NationData _contactNationData, int _amount)
	{
		// Record contacts to each of the given contact nation's users.
		for (int cur_user_index = 0; cur_user_index < _nationData.users.size(); cur_user_index++)
		{
			// Get the current user's data
			UserData curUserData = (UserData)DataManager.GetData(Constants.DT_USER, _nationData.users.get(cur_user_index), false);

			if (curUserData != null) {
				RecordContactWithNation(curUserData, _contactNationData, _amount, false);
			}
		}
	}

	public static void SendPatronOffer(StringBuffer _output_buffer, int _userID, int _targetUserID)
	{
		// Get the target user's data
		UserData targetUserData = (UserData)DataManager.GetData(Constants.DT_USER, _targetUserID, false);

		if ((_userID == _targetUserID) || (targetUserData == null)) {
			return;
		}

		// Check whether the target user already has the maximum number of patron offers.
		if (targetUserData.patron_offers.size() >= MAX_NUM_PATRON_OFFER)
		{
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_patron_too_many_offers", "username", targetUserData.name));
			return;
		}

		// Check whether this user has already sent a patron offer to the target user.
		if (targetUserData.patron_offers.contains(_userID))
		{
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_patron_already_offered", "username", targetUserData.name));
			return;
		}

		// Add this user's ID to the target user's patron_offers list.
		targetUserData.patron_offers.add(_userID);

		// Send add_patron_offer message to the target user, if they are online.
		ClientThread targetClientThread = WOCServer.GetClientThread(_targetUserID);
		if ((targetClientThread != null) && targetClientThread.UserIsLoggedIn())
		{
	    // Construct and send add_patron_offer event
			comm_buffer.setLength(0);
			OutputEvents.GetEventAddPatronOffer(comm_buffer, _userID);
			targetClientThread.TerminateAndSendNow(comm_buffer);
		}

		// Send message back to user.
		OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_patron_offer_sent", "username", targetUserData.name));
	}

	public static void PatronOfferAccept(StringBuffer _output_buffer, int _userID, int _targetUserID)
	{
		// Get the user's data
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		// Get the target user's data
		UserData targetUserData = (UserData)DataManager.GetData(Constants.DT_USER, _targetUserID, false);

		if ((_userID == _targetUserID) || (targetUserData == null) || (userData == null)) {
			return;
		}

		// Make sure that the _targetUserID has actually sent a patron offer to the user.
		if (userData.patron_offers.contains(_targetUserID) == false) {
			return;
		}

		if (userData.patronID != -1)
		{
			// Remove the user from their previous patron's list of followers.
			RemoveFollower(userData.patronID, _userID);
		}

		// Remove the _targetUserID from the user's list of patron offers.
		userData.patron_offers.remove(Integer.valueOf(_targetUserID));

		// Change the user's patron information.
		userData.patronID = _targetUserID;
		userData.total_patron_xp_received = 0;
		userData.total_patron_credits_received = 0;

		// Send the remove patron offer event and the patron info event to the user.
		OutputEvents.GetEventRemovePatronOffer(_output_buffer, _targetUserID);
		OutputEvents.GetPatronInfoEvent(_output_buffer, _userID);

		// Add the user as a follower to their new patron.
		AddFollower(targetUserData, userData);

		// Send message back to user.
		OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_patron_offer_accept", "username", targetUserData.name));

		// Update data for the user.
		DataManager.MarkForUpdate(userData);
	}

	public static void AttemptRemoveFollower(StringBuffer _output_buffer, int _userID, int _targetUserID)
	{
		// Get the user's data
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		// Get the target user's data
		UserData targetUserData = (UserData)DataManager.GetData(Constants.DT_USER, _targetUserID, false);

		// Remove the follower
		RemoveFollower(_userID, _targetUserID);

		// Change the target user's patron information.
		targetUserData.patronID = -1;
		targetUserData.total_patron_xp_received = 0;
		targetUserData.total_patron_credits_received = 0;

		// Update data for the removed follower.
		DataManager.MarkForUpdate(targetUserData);

		// Send the user their updated list of followers.
		OutputEvents.GetAllFollowersEvent(_output_buffer, _userID);
	}

	public static void AddFollower(UserData _patronUserData, UserData _followerUserData)
	{
		// Add the user to their new patron's list of followers.
		_patronUserData.followers.add(new FollowerData(_followerUserData.ID, Constants.GetAbsoluteDay(), 0, 0));

		// Modify the new patron's follower count report value.
		_patronUserData.ModifyReportValueInt(UserData.ReportVal.report__follower_count, 1);

		// Update the patron's max number of followers, all-time and monthly.
		_patronUserData.max_num_followers = Math.max(_patronUserData.max_num_followers, _patronUserData.followers.size());
		_patronUserData.max_num_followers_monthly = Math.max(_patronUserData.max_num_followers_monthly, _patronUserData.followers.size());

		// Update the global user followers ranks
		RanksData.instance.ranks_user_followers.UpdateRanks(_patronUserData.ID, _patronUserData.name, _patronUserData.max_num_followers, Constants.NUM_FOLLOWERS_RANKS, false);

		// Update the global user followers monthly ranks
		RanksData.instance.ranks_user_followers_monthly.UpdateRanks(_patronUserData.ID, _patronUserData.name, _patronUserData.max_num_followers_monthly, Constants.NUM_FOLLOWERS_RANKS, false);

		// Send the add follower event to the new patron, if they are online.
		ClientThread targetClientThread = WOCServer.GetClientThread(_patronUserData.ID);
		if ((targetClientThread != null) && targetClientThread.UserIsLoggedIn())
		{
	    // Construct and send add_follower and message events
			comm_buffer.setLength(0);
			OutputEvents.GetEventAddFollower(comm_buffer, _followerUserData.ID);
			OutputEvents.GetMessageEvent(comm_buffer, ClientString.Get("svr_patron_new_follower", "username", _followerUserData.name));
			targetClientThread.TerminateAndSendNow(comm_buffer);
		}

		// Mark the patron's user data to be updated.
		DataManager.MarkForUpdate(_patronUserData);
	}

	public static void RemoveFollower(int _patronUserID, int _followerUserID)
	{
		UserData prevPatronUserData = (UserData)DataManager.GetData(Constants.DT_USER, _patronUserID, false);

		if (prevPatronUserData != null)
		{
			// Find the index of, and remove, the patron's FollowerData entry for the follower being used.
			for (int index = 0; index < prevPatronUserData.followers.size(); index++)
			{
				if (prevPatronUserData.followers.get(index).userID == _followerUserID)
				{
					prevPatronUserData.followers.remove(index);
					break;
				}
			}

			// Update the patron's user data.
			DataManager.MarkForUpdate(prevPatronUserData);

			// Modify the former patron's follower count report value.
			prevPatronUserData.ModifyReportValueInt(UserData.ReportVal.report__follower_count, -1);
		}
	}

	public static void PatronOfferDecline(StringBuffer _output_buffer, int _userID, int _targetUserID)
	{
		// Get the user's data
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		// Get the target user's data
		UserData targetUserData = (UserData)DataManager.GetData(Constants.DT_USER, _targetUserID, false);

		if ((_userID == _targetUserID) || (targetUserData == null) || (userData == null)) {
			return;
		}

		// Make sure that the _targetUserID has actually sent a patron offer to the user.
		if (userData.patron_offers.contains(_targetUserID) == false) {
			return;
		}

		// Remove the _targetUserID from the user's list of patron offers.
		userData.patron_offers.remove(Integer.valueOf(_targetUserID));

		// Send the remove patron offer event to the user.
		OutputEvents.GetEventRemovePatronOffer(_output_buffer, _targetUserID);

		// Send message back to user.
		OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_patron_offer_decline", "username", targetUserData.name));
	}
}
