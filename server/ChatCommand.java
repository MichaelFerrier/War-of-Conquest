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

import java.io.*;
import java.util.*;
import java.text.NumberFormat;
import WOCServer.*;

public class ChatCommand
{
	public static void ProcessChatCommand(StringBuffer _output_buffer, int _userID, int _deviceID, String _text)
	{
		int command_end_pos = _text.indexOf(" ");

		if (command_end_pos == -1) {
			command_end_pos = _text.length();
		}

		String command =  _text.substring(1, command_end_pos).toLowerCase();

		int param_start_pos = command_end_pos;
		while ((param_start_pos < _text.length()) && (_text.charAt(param_start_pos) == ' ')) {
			param_start_pos++;
		}

		// Get the UserData of the user sending this command.
    UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		if (userData == null) {
			return;
		}

		boolean user_is_admin = userData.admin;
		int user_mod_level = userData.mod_level;

		Output.PrintToScreen("Chat command from " + userData.name + "(" + _userID + "): " + command);

		if (command.compareTo("accounts") == 0)
		{
			// List this user.
			ListRecentAccount(_output_buffer, userData);

			// List each of this user's associated users that have been active recently.
			for (int i = 0; i < userData.associated_users.size(); i++)
			{
				UserData assoc_user_data = (UserData)DataManager.GetData(Constants.DT_USER, userData.associated_users.get(i), false);
				ListRecentAccount(_output_buffer, assoc_user_data);
			}
		}
		else if (command.compareTo("ad_bonus") == 0)
		{
			if ((!user_is_admin) && (user_mod_level == 0))
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_mod_level_limit", "level", "1"));
				return;
			}
/*
			if (_text.length() <= command_end_pos) {
				return;
			}

			// Determine the amount of ad bonus to add.
			int amount_start = _text.indexOf(" ") + 1;
			if ((amount_start == 0) || (_text.length() == amount_start)) return;
			int amount = Integer.parseInt( _text.substring(amount_start).trim());

			if (amount <= 0)
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("Format: /ad_bonus amount"));
				return;
			}
*/
			// Award available ad bonus credits to the user, if appropriate.
			Gameplay.AwardAvailableAdBonus(_output_buffer, userData, 1f, Constants.AD_BONUS_TYPE_LEVEL, -1, -1, 0);
		}
		else if (command.compareTo("chat_ban") == 0)
		{
			if ((!user_is_admin) && (user_mod_level == 0))
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_mod_level_limit", "level", "1"));
				return;
			}

			int user_name_start = _text.indexOf(" ") + 1;
			int user_name_end = _text.indexOf(",");

			if (user_name_start >= user_name_end)
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("Format: /chat_ban username,minutes"));
				return;
			}

			// Determine the name of the target user
			String target_user_name = _text.substring(user_name_start, user_name_end);

			// Determine the target user's ID
			int targetUserID = UserData.GetUserIDByUsername(target_user_name);

			if (targetUserID == -1)
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_msg_unknown_user", "username", target_user_name));
				return;
			}

			// Fetch the number of minutes from the command string
			int num_minutes = Integer.parseInt(_text.substring(user_name_end + 1).replaceAll(" ", ""));

			// Check that given number of minutes is within acceptable range
			if ((num_minutes < 0) || (num_minutes > 1000000))
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("Format: @chat_ban player_name,minutes"));
				return;
			}

			// Check that given number of minutes is allowed
			if ((num_minutes > 60) && (user_mod_level == 1))
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_mod_ban_time_limit", "minutes", "60"));
				return;
			}

			// Check that given number of minutes is allowed
			if ((num_minutes > 1440) && (user_mod_level == 2))
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_mod_ban_time_limit", "minutes", "1440"));
				return;
			}

			// Get the target user's data
			UserData targetUserData = (UserData)DataManager.GetData(Constants.DT_USER, targetUserID, false);

			// Record the ban
			Comm.RecordChatBan(targetUserData, num_minutes * 60, 0f);

			// Log this banning
			Output.PrintToScreen("**User " + target_user_name + " banned from public chat for " + num_minutes + " minutes by " + userData.name);
			Constants.WriteToLog("log_mod.txt", "COMMAND: " + target_user_name + " banned from public chat for " + num_minutes + " minutes by " + userData.name + "\n");
			Constants.WriteToPublicLog(Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + ", **BAN** " + target_user_name + " banned from public chat for " + num_minutes + " minutes by " + userData.name + "</br>\n");

			// Send success message back to the user
			OutputEvents.GetChatLogEvent(_output_buffer,  ClientString.Get("svr_mod_ban", "username", target_user_name, "minutes", "" + num_minutes, "moderator", userData.name));

			// Send report of this banning to the banned player's nation
			Comm.SendReport(targetUserData.nationID, ClientString.Get("svr_mod_ban", "username", target_user_name, "minutes", "" + num_minutes, "moderator", userData.name), 0);
		}
		else if (command.compareTo("countdown") == 0)
		{
			// Get the nation's data
			NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, userData.nationID, false);

			if (nationData == null) {
				return;
			}

			// Display the nation's rebirth countdown.
			OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("Countdown to rebirth: " + nationData.rebirth_countdown));
		}
		else if (command.compareTo("credits") == 0)
		{
			// Get the nation's data
			NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, userData.nationID, false);

			if (nationData == null) {
				return;
			}

			// Display the nation's credit information.
			OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_num_credits", "num_credits", NumberFormat.getIntegerInstance().format(nationData.game_money), "num_transferable", NumberFormat.getIntegerInstance().format(nationData.game_money_purchased)));
		}
		else if ((command.compareTo("g") == 0) || (command.compareTo("go") == 0))
		{
			char curChar, firstDir = '\0', secondDir = '\0';
			boolean firstNumStarted = false, secondNumStarted = false, firstNumEnded = false, secondNumEnded = false;
			String firstNumString = "", secondNumString = "";
			int firstNum = -1;
			int secondNum = -1;

			// Parse string for first and second direction and number. Eg., "450 N, 200 E" or "200e, 450n".
			for (int i = command_end_pos + 1; i < _text.length(); i++)
			{
				curChar = _text.charAt(i);
				if ((Character.isDigit(curChar)) || (curChar == '.'))
				{
					if (firstNumEnded == false)
					{
						firstNumStarted = true;
						firstNumString = firstNumString + curChar;
					}
					else if (secondNumEnded == false)
					{
						secondNumStarted = true;
						secondNumString = secondNumString + curChar;
					}
				}
				else
				{
					if (secondNumStarted) {
						secondNumEnded = true;
					} else if (firstNumStarted) {
						firstNumEnded = true;
					}

					if ((curChar == 'n') || (curChar == 'N') || (curChar == 's') || (curChar == 'S') || (curChar == 'e') || (curChar == 'E') || (curChar == 'w') || (curChar == 'W'))
					{
						if (firstDir == '\0') {
							firstDir = curChar;
						} else if (secondDir == '\0') {
							secondDir = curChar;
						}
					}
				}
			}

			LandMap landMap = DataManager.GetLandMap(userData.mapID, false);

			if (landMap == null) {
				return;
			}

			int newViewX = -1, newViewY = -1;

			if ((firstDir != '\0') && firstNumStarted)
			{
				try {
					firstNum = (int)(Double.parseDouble(firstNumString) + 0.5);
				} catch (Exception e) {
				}

				if ((firstDir == 'n') || (firstDir == 'N')) newViewY = (landMap.height / 2) - firstNum;
				else if ((firstDir == 's') || (firstDir == 'S')) newViewY = (landMap.height / 2) + firstNum;
				else if ((firstDir == 'w') || (firstDir == 'W')) newViewX = (landMap.width / 2) - firstNum;
				else if ((firstDir == 'e') || (firstDir == 'E')) newViewX = (landMap.width / 2) + firstNum;
			}

			if ((secondDir != '\0') && secondNumStarted)
			{
				try {
					secondNum = (int)(Double.parseDouble(secondNumString) + 0.5);
				} catch (Exception e) {
				}

				if ((secondDir == 'n') || (secondDir == 'N')) newViewY = (landMap.height / 2) - secondNum;
				else if ((secondDir == 's') || (secondDir == 'S')) newViewY = (landMap.height / 2) + secondNum;
				else if ((secondDir == 'w') || (secondDir == 'W')) newViewX = (landMap.width / 2) - secondNum;
				else if ((secondDir == 'e') || (secondDir == 'E')) newViewX = (landMap.width / 2) + secondNum;
			}

			if ((newViewX == -1) && (newViewY == -1))
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_chat_command_go_format"));
				return;
			}

			// For any axis with no coord provided, default to the user's current view coord on that axis.
			if (newViewX == -1) newViewX = userData.viewX;
			if (newViewY == -1) newViewY = userData.viewY;

			//Output.PrintToScreen("firstDir: " + firstDir + ", firstNumString: " + firstNumString + ", firstNum: " + firstNum + ", secondDir: " + secondDir + ", secondNumString: " + secondNumString + ", secondNum: " + secondNum + ", newViewX: " + newViewX + ", newViewY: " + newViewY);

			// Attempt to set the user's view to the given location.
			Display.SetUserView(userData, newViewX, newViewY, true, _output_buffer);
		}
		else if (command.compareTo("info") == 0)
		{
			int nation_name_start = command_end_pos + 1;

			if (_text.length() <= command_end_pos) {
				return;
			}

			// Determine the name of the nation
			String nation_name = _text.substring(nation_name_start);

			// Determine the nation's ID
			int targetNationID = DataManager.GetNameToIDMap(Constants.DT_NATION, nation_name);

			if (targetNationID == -1)
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_msg_no_such_nation", "nation_name", nation_name));
				return;
			}

			// Get the nation info event string
			OutputEvents.GetNationInfoEvent(_output_buffer, _userID, targetNationID);
		}
		else if (command.compareTo("login_as") == 0)
		{
			// This command can only be executed by an admin.
			if (userData.admin == false) {
				return;
			}

			int username_end_pos = _text.length();
			//int username_end_pos = _text.indexOf(' ', param_start_pos + 1);
			//if ((username_end_pos == -1) && (param_start_pos < _text.length())) {
			//	username_end_pos = _text.length();
			//}

			if (username_end_pos != -1)
			{
				String login_username = _text.substring(param_start_pos, username_end_pos);
				int login_userID = UserData.GetUserIDByUsername(login_username);

				if (login_userID == -1) {
					OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_msg_unknown_user", "username", login_username));
				}

				// Get the admin user's client thread.
				ClientThread clientThread = WOCServer.GetClientThread(_userID);

				// Log this admin user out of their current account, and into the account with the given username.
				//Login.Logout(clientThread, true);
				Login.AttemptLogin(_output_buffer, login_username, "", true, clientThread);
			}
		}
		else if ((command.compareTo("m") == 0) || (command.compareTo("message") == 0))
		{
			int name_end_pos = -1;
			String recipient_name = "";
			int recipientID = -1;

			// Attempt to isolate name based on comma separator.
			name_end_pos = _text.indexOf(',', param_start_pos + 1);
			if ((name_end_pos == -1) && (param_start_pos < _text.length())) {
				name_end_pos = _text.length();
			}

			if (name_end_pos != -1)
			{
				// Determine the target name
				recipient_name = _text.substring(param_start_pos, name_end_pos);

				// Determine the target's ID
				recipientID = NationData.GetNationIDByNationName(recipient_name);
			}

			// If no valid name was found...
			if (recipientID == -1)
			{
				// Attempt to isolate name based on space separator.
				name_end_pos = _text.indexOf(' ', param_start_pos + 1);
				if ((name_end_pos == -1) && (param_start_pos < _text.length())) {
					name_end_pos = _text.length();
				}

				if (name_end_pos != -1)
				{
					// Determine the target name
					recipient_name = _text.substring(param_start_pos, name_end_pos);

					// Determine the target's ID
					recipientID = NationData.GetNationIDByNationName(recipient_name);
				}
			}

			if ((name_end_pos != -1) && (recipientID != -1))
			{
				String recipient_nation_name = _text.substring(param_start_pos, name_end_pos);

				String message_text = "";
				if ((name_end_pos + 1) < _text.length()) {
					message_text = _text.substring(name_end_pos + 1);
				}

				// Send the message to the nation
				ClientString error_message = Comm.PostMessage(_userID, _deviceID, recipient_nation_name, message_text);

				if (error_message.IsEmpty())
				{
					// Return success message
					OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_message_sent", "nation_name", recipient_nation_name)); // "Message sent to " + recipient_nation_name + "."
				}
				else
				{
					// Return error message
					OutputEvents.GetChatLogEvent(_output_buffer, error_message);
				}

				ClientString.Release(error_message);
			}
			else
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_message_format")); // "Format: /message nationname message"
			}
		}
		else if (command.compareTo("online") == 0)
		{
			// Display list of this user's contacts that are online.
			UserData contactUserData;
			NationData contactNationData;
			Iterator it = userData.contacts.entrySet().iterator();
			while (it.hasNext())
			{
				Map.Entry pair = (Map.Entry)it.next();
				contactUserData = (UserData)DataManager.GetData(Constants.DT_USER, (Integer)(pair.getKey()), false);

				if ((contactUserData != null) && WOCServer.IsUserLoggedIn(contactUserData.ID))
				{
					contactNationData = (NationData)DataManager.GetData(Constants.DT_NATION, contactUserData.nationID, false);

					// Do not list members of incognito nations, even though they're contacts.
					if (contactNationData.GetFlag(Constants.NF_INCOGNITO)) {
						continue;
					}

					OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_online_contact", "username", contactUserData.name, "nation_name", contactNationData.name));
				}
			}

			// Display total number of players online.
			OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_online_num", "num_online", String.valueOf(WOCServer.client_table.size())));
		}
		else if (command.compareTo("raid") == 0)
		{
			if ((!user_is_admin) && (user_mod_level < 3))
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_mod_level_limit", "level", "3"));
				return;
			}

			int nation_name_start = command_end_pos + 1;

			if (_text.length() <= command_end_pos) {
				return;
			}

			// Determine the name of the nation
			String nation_name = _text.substring(nation_name_start);

			// Determine the nation's ID
			int targetNationID = DataManager.GetNameToIDMap(Constants.DT_NATION, nation_name);

			if (targetNationID == -1)
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_msg_no_such_nation", "nation_name", nation_name));
				return;
			}

			// Get the user's nation's data.
			NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, userData.nationID, false);

			if (nationData == null) {
				return;
			}

			// Fill the user's nation's homeland manpower.
			nationData.homeland_footprint.manpower = nationData.GetFinalManpowerMax(nationData.homeland_mapID);

			// Simulate a raid command from this user, passing in the target nation ID.
			Raid.OnRaidCommand(_output_buffer, _userID, targetNationID);
		}
		else if (command.compareTo("redeem_voucher") == 0)
		{
			int code_start = _text.indexOf(" ") + 1;
			int code_end = _text.indexOf(",");

			if (code_start >= code_end)
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("Format: /redeem_voucher xxx-xxx-xxx, amount"));
				return;
			}

			// Determine the voucher code
			String voucher_code = _text.substring(code_start, code_end).trim();

			// Fetch the amount from the command string
			int amount = 0;
			try {
				amount = Integer.parseInt(_text.substring(code_end + 1).trim());
			} catch(NumberFormatException e) {}

			if (amount <= 0)
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("Format: /redeem_voucher xxx-xxx-xxx, amount"));
				return;
			}

			VoucherData voucherData = VoucherData.GetVoucherDataByCode(voucher_code, false);

			if (voucherData == null)
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("There is no voucher with the code " + voucher_code + "."));
				return;
			}

			if (amount > voucherData.credits_remaining)
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("Cannot redeem " + amount + " credits. Only " + voucherData.credits_remaining + " credits remain on this voucher."));
				return;
			}

			// Get the nation's data
			NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, userData.nationID, false);

			if (nationData == null) {
				return;
			}

			// Redeem the credits from the voucher and add this redemption to its history.
			voucherData.RemoveValueFromVoucher(voucherData, amount, true);
			voucherData.history.add(amount + " credits redeemed by " + userData.name + " for nation " + nationData.name + " on " + Constants.GetDateString() + ". " + voucherData.credits_remaining + " left.");
			DataManager.MarkForUpdate(voucherData);

			// Add the credits (as having been purchased) to the user's nation.
			Money.AddGameMoney(nationData, amount, Money.Source.PURCHASED);

			// Log the redeeming of this voucher.
			Constants.WriteToLog("log_vouchers.txt", Constants.GetTimestampString() + ": " + amount + " credits redeemed by " + userData.name + " for nation " + nationData.name + " on " + Constants.GetDateString() + ". " + voucherData.credits_remaining + " left.\n");

			// Broadcast an update bars event to the nation, letting all players know about the change in credits.
			OutputEvents.BroadcastUpdateBarsEvent(nationData.ID, 0, 0, 0, 0, amount, 0);

			// Send report of this credit redemption to nation
			Comm.SendReport(nationData.ID, ClientString.Get("svr_report_redeemed_credits", "username", userData.name, "num_credits", String.valueOf(amount)), 0); // userData.name + " has purchased " + num_credits_bought + " credits."

			// Send success message back to the user
			OutputEvents.GetChatLogEvent(_output_buffer,  ClientString.Get("svr_msg_redeemed_credits", "num_credits", String.valueOf(amount), "remaining_credits", String.valueOf(voucherData.credits_remaining)));
		}
		else if (command.compareTo("rename_nation") == 0)
		{
			if ((!user_is_admin) && (user_mod_level < 3))
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_mod_level_limit", "level", "3"));
				return;
			}

			int name_start = _text.indexOf(" ") + 1;
			int name_end = _text.indexOf(",");

			if ((name_start == -1) || (name_end == -1))
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("Format: /rename_nation old_name,new_name"));
				return;
			}

			String old_name = _text.substring(name_start, name_end).trim();
			String new_name = _text.substring(name_end + 1).trim();

			if ((old_name.length() == 0) || (new_name.length() == 0))
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("Format: /rename_nation old_name,new_name"));
				return;
			}

			// Check that a nation name of a sufficient length is given
			if ((new_name.length() < Constants.MIN_NATION_NAME_LEN) || (new_name.length() > Constants.MAX_NATION_NAME_LEN))
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_mod_rename_nation_length_limit", "min_length", "" + Constants.MIN_NATION_NAME_LEN, "max_length", "" + Constants.MAX_NATION_NAME_LEN));
				return;
			}

			int nationID = NationData.GetNationIDByNationName(old_name);
			NationData nation_data = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);
			int newNationID = NationData.GetNationIDByNationName(new_name);

			if ((nationID == -1) || (nation_data == null))
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_msg_unknown_nation", "nation_name", old_name)); // "Unknown nation: " + old_name
			}
			else if (newNationID != -1)
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_mod_rename_nation_duplicate", "naton_name", new_name));
			}
			else
			{
				// Change the name in the data
				nation_data.name = new_name;

				// Mark the nation's data to be updated
				DataManager.MarkForUpdate(nation_data);
        //Constants.WriteToLog("log_MFU.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " "  + nation_data.ID + " evt: @rename_nation\n");

				// Log this name change
				Output.PrintToScreen("**Nation " + old_name + " name changed to " + new_name + " by " + userData.name);
				Constants.WriteToLog("log_mod.txt", "COMMAND: " + old_name + " name changed to " + new_name + " by " + userData.name + "\n");
				Constants.WriteToPublicLog(Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + ", **NATION NAME CHANGE** " + old_name + " changed to " + new_name + " by " + userData.name + "</br>\n");

				// Output message
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_mod_rename_nation", "old_name", old_name, "new_name", new_name));
			}
		}
		else if (command.compareTo("rename_user") == 0)
		{
			if ((!user_is_admin) && (user_mod_level < 3))
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_mod_level_limit", "level", "3"));
				return;
			}

			int name_start = _text.indexOf(" ") + 1;
			int name_end = _text.indexOf(",");

			if ((name_start == -1) || (name_end == -1))
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("Format: /rename_user old_name,new_name"));
				return;
			}

			String old_name = _text.substring(name_start, name_end).trim();
			String new_name = _text.substring(name_end + 1).trim();

			if ((old_name.length() == 0) || (new_name.length() == 0))
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("Format: /rename_user old_name,new_name"));
				return;
			}

			// Check that a name of a sufficient length is given
			if ((new_name.length() < Constants.MIN_USERNAME_LEN) || (new_name.length() > Constants.MAX_USERNAME_LEN))
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_mod_rename_user_length_limit", "min_length", "" + Constants.MIN_USERNAME_LEN, "max_length", "" + Constants.MAX_USERNAME_LEN));
				return;
			}

			int newTargetPlayerID = AccountDB.GetPlayerIDByUsername(new_name);

			if (newTargetPlayerID != -1)
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_mod_rename_user_duplicate", "username", new_name));
				return;
			}

			int targetPlayerID = AccountDB.GetPlayerIDByUsername(old_name);
			int targetUserID = UserData.GetUserIDByPlayerID(targetPlayerID);
			UserData target_user_data = (UserData)DataManager.GetData(Constants.DT_USER, targetUserID, false);

			if ((targetPlayerID == -1) || (targetUserID == -1) || (target_user_data == null))
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_msg_unknown_user", "username", old_name));
				return;
			}
			else
			{
				// Change the username in the player account data.
				PlayerAccountData playerData = AccountDB.ReadPlayerAccount(targetPlayerID);
				playerData.username = new_name;
				AccountDB.WritePlayerAccount(playerData);

				// Change the name in the user data
				target_user_data.name = new_name;

				// Mark the user's data to be updated
				DataManager.MarkForUpdate(target_user_data);

				// Log this name change
				Output.PrintToScreen("**User " + old_name + " name changed to " + new_name + " by " + userData.name);
				Constants.WriteToLog("log_mod.txt", "COMMAND: " + old_name + " name changed to " + new_name + " by " + userData.name + "\n");
				Constants.WriteToPublicLog(Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + ", **PLAYER NAME CHANGE** " + old_name + " changed to " + new_name + " by " + userData.name + "</br>\n");

				// Output message
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_mod_rename_user", "old_name", old_name, "new_name", new_name));
			}
		}
		else if (command.compareTo("set_mod") == 0)
		{
			if ((!user_is_admin) && (user_mod_level < 3))
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_mod_level_limit", "level", "3"));
				return;
			}

			int user_name_start = _text.indexOf(" ") + 1;
			int user_name_end = _text.indexOf(",");

			if (user_name_start >= user_name_end)
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("Format: /set_mod player_name,number"));
				return;
			}

			// Determine the name of the target user
			String target_user_name = _text.substring(user_name_start, user_name_end);

			// Determine the target user's ID
			int targetUserID = UserData.GetUserIDByUsername(target_user_name);

			if (targetUserID == -1)
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_msg_unknown_user", "username", target_user_name)); // "'" + target_user_name + "' is not a known user."
				return;
			}

			// Get the target user's data
			UserData targetUserData = (UserData)DataManager.GetData(Constants.DT_USER, targetUserID, false);
			int target_user_mod_level = targetUserData.mod_level;

			// Fetch the new mod level from the command string
			int target_user_new_mod_level = Integer.parseInt(_text.substring(user_name_end + 1).replaceAll(" ", ""));

			// Check that given level is within acceptable range
			if ((target_user_new_mod_level < 0) || (target_user_new_mod_level > 10))
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("Format: @set_mod player_name,number"));
				return;
			}

			if (user_is_admin || ((user_mod_level > target_user_mod_level) && (user_mod_level > target_user_new_mod_level)))
			{
				// Change the target user's mod_level
				targetUserData.mod_level = target_user_new_mod_level;
				DataManager.MarkForUpdate(targetUserData);

				// Send success message back to the user
				OutputEvents.GetChatLogEvent(_output_buffer,  ClientString.Get("svr_mod_set_mod_level", "username", target_user_name, "level", "" + target_user_new_mod_level));

				// Send report to the target user's nation
				int targetNationID = targetUserData.nationID;
				Comm.SendReport(targetNationID, ClientString.Get("svr_mod_set_mod_level", "username", target_user_name, "level", "" + target_user_new_mod_level), 0);

				// Log this change
				Constants.WriteToLog("log_mod.txt", "COMMAND: " + target_user_name + "'s moderator level set to " + target_user_new_mod_level + " by " + userData.name + "\n");
			}
			else
			{
				// Send failure message back to user
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_mod_set_mod_level_fail", "username", target_user_name, "level", "" + target_user_new_mod_level));
			}
		}
		else if (command.compareTo("suspect") == 0)
		{
			int nation_name_start = _text.indexOf(" ") + 1;

			if (nation_name_start == 0)
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_chat_command_suspect_format"));
				return;
			}

			// Determine the name of the nation
			String nation_name = _text.substring(nation_name_start);

			// Determine the nation's ID
			int targetNationID = DataManager.GetNameToIDMap(Constants.DT_NATION, nation_name);

			if (targetNationID == -1)
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_msg_unknown_nation", "nation_name", nation_name)); // "Unknown nation: " + nation_name
				return;
			}

			WOCServer.NationRecord nation_record = WOCServer.nation_table.get(targetNationID);

			// Write this command to log
			Constants.WriteToLog("log_suspect.txt", "@SUSPECT Logging nation " + nation_name + ", requested by " + userData.name + " (" + _userID + ")\n");
 			Constants.WriteToLog("log_attack.txt", "@SUSPECT Logging nation " + nation_name + ", requested by " + userData.name + " (" + _userID + ")\n");

 			// Get the suspect nation's data
			NationData suspect_nationData = (NationData)DataManager.GetData(Constants.DT_NATION, targetNationID, false);

      if (suspect_nationData != null)
      {
        // Mark the suspect nation to be logged
        suspect_nationData.log_suspect_expire_time = Constants.GetFineTime() + Constants.LOG_SUSPECT_FINE_DURATION;
        suspect_nationData.log_suspect_init_nationID = userData.nationID;
      }

      if (nation_record != null)
      {
        // Mark each of the given nation's online users, to be logged
				for (Map.Entry<Integer,ClientThread> user_entry : nation_record.users.entrySet())
        {
          // Set the current ClientThread's log_suspect_expire_time
          ((ClientThread)(user_entry.getValue())).log_suspect_expire_time = Constants.GetFineTime() + Constants.LOG_SUSPECT_FINE_DURATION;
        }
      }

			// Return confirmation message
			OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_chat_command_suspect", "nation_name", nation_name));
		}
		else if (command.compareTo("transfer_credits") == 0)
		{
			if (userData.rank > Constants.RANK_COSOVEREIGN)
			{
				// Return message
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_transfer_credits_rank_too_low")); // "You cannot transfer credits until you've been promoted to Cosovereign."
				return;
			}

			// Get the user's nation's data
			NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, userData.nationID, false);

			if (nationData == null) {
				return;
			}

			int name_start = _text.indexOf(" ") + 1;
			int name_end = _text.indexOf(",");

			if ((name_start == -1) || (name_end == -1))
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("Format: /transfer_credits nation_name, amount"));
				return;
			}

			// Determine the name of the target nation
			String target_nation_name = _text.substring(name_start, name_end).trim();

			// Determine the target nation's ID
			int targetNationID = NationData.GetNationIDByNationName(target_nation_name);

			// Get the target nation's data
			NationData targetNationData = (NationData)DataManager.GetData(Constants.DT_NATION, targetNationID, false);

			if ((targetNationID == -1) || (targetNationData == null))
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_msg_unknown_nation", "nation_name", target_nation_name));
				return;
			}

			// Fetch the number of credits to transfer from the command string
			int num_credits = Integer.parseInt(_text.substring(name_end + 1).trim());

			// Check that given number of credits is valid.
			if (num_credits < 0)
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("Format: /transfer_credits nation_name, amount"));
				return;
			}

			// Check that the user's nation has at least that many transferable credits.
			if ((nationData.game_money_purchased + nationData.game_money_won) < num_credits)
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_transfer_credits_not_enough", "num_transferable", NumberFormat.getIntegerInstance().format(nationData.game_money_purchased)));
				return;
			}

			if (Constants.max_buy_credits_per_month != -1)
			{
				// Determine how many credits the target nation can still receive this month.
				int num_credits_allowed_to_receive = Math.max(0, Constants.max_buy_credits_per_month - (targetNationData.GetNumCreditsPurchasedThisMonth() + targetNationData.GetNumCreditsReceivedThisMonth()));

				if (num_credits > num_credits_allowed_to_receive)
				{
					OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_transfer_credits_cannot_receive", "num_allowed", NumberFormat.getIntegerInstance().format(num_credits_allowed_to_receive)));
					return;
				}
			}

			// Determine the amount of purchased and won credits to transfer.
			int transfer_amount_purchased = (int)Math.min(num_credits, nationData.game_money_purchased);
			int transfer_amount_won = (num_credits - transfer_amount_purchased);

			// Transfer the credits from the source nation.
			nationData.game_money -= num_credits;
			nationData.game_money_purchased -= transfer_amount_purchased;
			nationData.game_money_won -= transfer_amount_won;

			// Transfer the credits to the target nation.
			targetNationData.game_money += num_credits;
			targetNationData.game_money_purchased += transfer_amount_purchased;
			targetNationData.game_money_won += transfer_amount_won;

			// Reset the target nation's information about credits bought and received this month, if necessary.
			if (targetNationData.prev_buy_credits_month != Constants.GetFullMonth())
			{
				targetNationData.prev_buy_credits_month = Constants.GetFullMonth();
				targetNationData.prev_buy_credits_month_amount = 0;
				targetNationData.prev_receive_credits_month_amount = 0;
			}

			// Record that the target nation has received this amount of credits during the current month.
			targetNationData.prev_receive_credits_month_amount += num_credits;

  		// Mark both nations' data to be updated
  		DataManager.MarkForUpdate(nationData);
			DataManager.MarkForUpdate(targetNationData);

			// Broadcast an update event to both nations, letting all players know about the change in credits.
			OutputEvents.BroadcastUpdateEvent(userData.nationID);
			OutputEvents.BroadcastUpdateEvent(targetNationID);

			// Return success message.
			OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_transfer_credits_success", "num_transfered", NumberFormat.getIntegerInstance().format(num_credits), "nation_name", target_nation_name));
		}
		else if (command.compareTo("unban") == 0)
		{
			if ((!user_is_admin) && (user_mod_level == 0))
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_mod_level_limit", "level", "1"));
				return;
			}

			int user_name_start = _text.indexOf(" ") + 1;

			// Determine the name of the target user
			String target_user_name = _text.substring(user_name_start).trim();

			// Determine the target user's ID
			int targetUserID = UserData.GetUserIDByUsername(target_user_name);

			// Get the target user's data
			UserData targetUserData = (UserData)DataManager.GetData(Constants.DT_USER, targetUserID, false);

			if ((targetUserID == -1) || (targetUserData == null))
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_msg_unknown_user", "username", target_user_name));
				return;
			}

		  // Unban the user
			Comm.RecordChatBan(targetUserData, 0, 0f);

			// Log this unbanning
			Constants.WriteToLog("log_ban.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " " + userData.name + " unbanned " + target_user_name + " (" + targetUserID + ")\n");

			// Return message
			OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_mod_ban_unban", "username", target_user_name));
		}
		else if (command.compareTo("voucher_info") == 0)
		{
			String voucher_code = "";

			if (param_start_pos < _text.length()) {
				voucher_code = _text.substring(param_start_pos, _text.length()).trim();
			}

			if (voucher_code.length() > 0)
			{
				VoucherData voucherData = VoucherData.GetVoucherDataByCode(voucher_code, false);

				if (voucherData == null)
				{
					OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("There is no voucher with the code " + voucher_code + "."));
				}
				else
				{
					OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("Voucher code " + voucher_code + ":"));

					// Output history of the voucher
					for (int i = 0; i < voucherData.history.size(); i++) {
						OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get(voucherData.history.get(i)));
					}

					// Output stats of the voucher.
					OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get(voucherData.credits_redeemed + " credits redeemed, " + voucherData.credits_remaining + " remaining."));
				}
			}
			else
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("Format: /voucher_info xxx-xxx-xxx")); // "Format: /voucher_info xxx-xxx-xxx"
			}
		}
		else if ((command.compareTo("w") == 0) || (command.compareTo("whisper") == 0))
		{
			int name_end_pos = -1;
			String recipient_name = "";
			int recipientID = -1;

			// Attempt to isolate name based on comma separator.
			name_end_pos = _text.indexOf(',', param_start_pos + 1);
			if ((name_end_pos == -1) && (param_start_pos < _text.length())) {
				name_end_pos = _text.length();
			}

			if (name_end_pos != -1)
			{
				// Determine the target name
				recipient_name = _text.substring(param_start_pos, name_end_pos);

				// Determine the target's ID
				recipientID = UserData.GetUserIDByUsername(recipient_name);
			}

			// If no valid name was found...
			if (recipientID == -1)
			{
				// Attempt to isolate name based on space separator.
				name_end_pos = _text.indexOf(' ', param_start_pos + 1);
				if ((name_end_pos == -1) && (param_start_pos < _text.length())) {
					name_end_pos = _text.length();
				}

				if (name_end_pos != -1)
				{
					// Determine the target name
					recipient_name = _text.substring(param_start_pos, name_end_pos);

					// Determine the target's ID
					recipientID = UserData.GetUserIDByUsername(recipient_name);
				}
			}

			if (name_end_pos == -1)
			{
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_whisper_format")); // "Format: /whisper username, message"
				return;
			}
			else if (recipientID == -1)
			{
				// Do not allow whisper to non-existent player.
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_whisper_unknown_username", "username", recipient_name)); // "There is no player named '" + recipient_username + "'."
				Output.PrintToScreen("Attempt to whisper to player '" + recipient_name + "' who doesn't exist.");
				return;
			}
			else if (recipientID == _userID)
			{
				// Do not allow whisper to the sending player.
				OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_whisper_self")); // "Whispering to ourselves again, are we?"
				return;
			}
			else
			{
				String message_text = "";
				if ((name_end_pos + 1) < _text.length()) {
					message_text = _text.substring(name_end_pos + 1);
				}

				Output.PrintToScreen("Whisper to '" + recipient_name + "' of '" + message_text + "'.");

				// Treat the given message as chat input. Use the negative of the sender user ID XOR the recipient user's ID as the chat channel.
				Comm.ChatInput(_output_buffer, _userID, _deviceID, -(_userID ^ recipientID), message_text);
			}
		}
	}

	public static void ListRecentAccount(StringBuffer _output_buffer, UserData _userData)
	{
		// Don't list this account if it was not used recently.
		if ((Constants.GetTime() - _userData.prev_logout_time) > Constants.max_accounts_period) {
			return;
		}

		if ((Constants.GetTime() - _userData.prev_logout_time) <= Constants.SECONDS_PER_DAY) {
			OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_list_recent_account_past_day", "username", _userData.name));
		} else {
			OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("svr_list_recent_account", "username", _userData.name, "num_days", NumberFormat.getIntegerInstance().format((Constants.GetTime() - _userData.prev_logout_time) / Constants.SECONDS_PER_DAY + 1)));
		}
	}
};
