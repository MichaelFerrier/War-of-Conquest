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
import org.apache.commons.codec.digest.*;
import WOCServer.DataManager;
import WOCServer.Output;
import WOCServer.Constants;
import WOCServer.OutputEvents;

public class Login
{
	static int BU_IP_ADDRESS = 0;
	static int BU_EMAIL = 1;
	static int BU_EXPIRE = 2;
  static int BU_RECENT_BAN_COUNT = 3;

	public static boolean allow_multi_client = false;

	static void AttemptConnect(StringBuffer _output_buffer, ClientThread _clientThread, int _client_version, boolean _enter_game)
	{
		// If server is currently only available to admin IP address, and user isn't from that IP, return message.
		if (Constants.admin_login_only && !(_clientThread.GetClientIP().equalsIgnoreCase(Constants.admin_login_ip)))
		{
			// Return error message
			Output.PrintToScreen("Attempt to log in from non-admin IP address " + _clientThread.GetClientIP());
			OutputEvents.GetSuspendEvent(_output_buffer, ClientString.Get("svr_enter_game_updating"), false); // "The War of Conquest server is currently being updated. Please try again in a few minutes."
			return;
		}

		if (_client_version < Constants.client_version)
		{
			OutputEvents.GetSuspendEvent(_output_buffer, ClientString.Get("svr_enter_game_update_client"), false); // "Please update to the latest version of War of Conquest."
			return;
		}

		// Record the client's version.
		_clientThread.SetClientVersion(_client_version);

		// Send connection event to client
		OutputEvents.GetConnectionEvent(_output_buffer, _clientThread);
	}

	static void AttemptEnterGame(StringBuffer _output_buffer, ClientThread _clientThread)
	{
		// Get the device record corresponding to the client ID, if there is one.
		DeviceData deviceData = DeviceData.GetDeviceDataByName(_clientThread.clientID, _clientThread.clientUID);

		if ((deviceData == null) || (deviceData.playerID == -1))
		{
			Output.PrintToScreen(Constants.GetShortTimeString() + " AttemptEnterGame() for client uid " + ((deviceData == null) ? "unknown" : deviceData.uid) + ": no associated device or playerID.");
			OutputEvents.GetNoAssociatedPlayerEvent(_output_buffer);
			return;
		}

		// Get the player account associated with this client's ID.
		PlayerAccountData player_account = AccountDB.ReadPlayerAccount(deviceData.playerID);

		if (player_account == null)
		{
			Output.PrintToScreen(Constants.GetShortTimeString() + " AttemptEnterGame() for client uid " + deviceData.uid + ": no associated player.");
			OutputEvents.GetNoAssociatedPlayerEvent(_output_buffer);
			return;
		}

		// Record the player account with the client thread.
		_clientThread.SetPlayerAccount(player_account);

		// Determine the userID to log in as.
		int userID = DetermineLoginUserID(_clientThread);

		// Have the determined player account enter the game on this server.
		EnterGame(_output_buffer, _clientThread, userID, false);
	}

	static boolean EnterGame(StringBuffer _output_buffer, ClientThread _clientThread, int _userID, boolean _admin_override)
	{
		int nationID;
		UserData userData = null;
		NationData nationData = null;
		ClientThread curClientThread;

		// TESTING
		//Output.PrintToScreen("EnterGame() called for client index " + _clientThread.GetClientIndex() + ", userID " + _userID);
		//Output.PrintStackTrace();

		if (_userID == -1)
		{
			Output.PrintToScreen("EnterGame(): called with userID -1 for client thread with player ID " + _clientThread.player_account.ID);
			return false;
		}

		// Get the user data record, which already exists.
		userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false); // Don't create

		if (userData == null)
		{
			Output.PrintToScreen("EnterGame(): client thread with player ID " + _clientThread.player_account.ID + " has no UserData (ID " + _userID + ")!");
			return false;
		}

		// Make sure the user's mapID is valid.
		if (userData.mapID == -1) {
			userData.mapID = Constants.MAINLAND_MAP_ID;
		}

		// Get the user's nation's ID and data
		nationID = userData.nationID;
		nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

		if (nationData == null)
		{
			Output.PrintToScreen("EnterGame(): client thread with player ID " + _clientThread.player_account.ID +  " and userID " + _userID + " has no NationData (ID " + nationData + ")!");
			return false;
		}

		// Update the nation's stats and remove obsolete technologies.
		Technology.UpdateStats(nationID, nationData);

		// If the user's nation has not yet been placed into the world, or has been removed, place it in the world.
		if (nationData.mainland_footprint.area == 0)
		{
			// Place the new nation in the world
			World.PlaceNation(nationData);

			// Set the user's mainland map view position to be the nation's first area.
			// Do not actually move the user's view to that loction yet, because they aren't logged in. That will be done later by SetUserViewForMap().
			AreaData first_area_data = (AreaData)(nationData.areas.get(0));
			userData.mainland_viewX = first_area_data.nationX;
			userData.mainland_viewY = first_area_data.nationY;
		}

		// Check whether this user is banned from logging in.
		if (userData.game_ban_end_time > Constants.GetTime())
		{
			Output.PrintToScreen("**BANNED User " + userData.name + " attempted to enter game.");
			int ban_time_remaining = userData.game_ban_end_time - Constants.GetTime();
			int ban_hours_remaining = ((int)(ban_time_remaining / 3600));
			int ban_mins_remaining = ((int)((ban_time_remaining % 3600) / 60));
			OutputEvents.GetSuspendEvent(_output_buffer, ClientString.Get("svr_enter_game_account_disabled", "num_hours", String.valueOf(ban_hours_remaining), "hour_quant", (ban_hours_remaining != 1) ? "{Server Strings/hour_quant_plural}" : "{Server Strings/hour_quant_singular}", "num_mins", String.valueOf(ban_mins_remaining), "minute_quant", (ban_mins_remaining != 1) ? "{Server Strings/minute_quant_plural}" : "{Server Strings/minute_quant_singular}"), false); // "This WoC account is disabled, and will be reactivated in " + ((int)(ban_time_remaining / 3600)) + " hours, " + ((int)((ban_time_remaining % 3600) / 60)) + " minutes."
			WOCServer.QueueClientToDelete(_clientThread);
			Constants.WriteToLog("log_ban.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " User " + userData.name + " attempted to login while banned.\n");
			return false;
		}

		if (WOCServer.IsUserLoggedIn(_userID, nationID))
		{
			ClientThread cThread = WOCServer.GetClientThread(_userID, nationID);
			int client_index = cThread.GetClientIndex();
			if (WOCServer.client_table.get(client_index) == null)
			{
				// This user is recorded as being logged in, but the user's client is NOT logged in!!
				Output.PrintToScreen("ERROR: user " + userData.name + "(userID: " + _userID + ", nationID: " + nationID + ") was logged in to nation_table, but its client index " + client_index + " was not in client_table! Logging user out now, on login attempt. Client thread: " + cThread.GetClientIndex() + " ID " + cThread.GetClientID());
				Login.ExitGame(cThread, false);
			}
			else
			{
				// Log this incident
				Output.PrintToScreen("** EnterGame() - Attempt to enter game by user who is already in game, removing the first (client index " + cThread.GetClientIndex()+ ", userID " + cThread.GetUserID() + "). User " + userData.name + " (" + _userID + ") ID: " + _clientThread.GetClientID());

				// Remove the first client associated with this user from the game.
				ForceExitGame(cThread, ClientString.Get("svr_enter_game_other_device_logged_in", "username", userData.name)); // userData.name + " has logged in on another device."
			}
		}

		// If allow_multi_client is false, and the user isn't an admin, and another client is found to be connected from the same device, log out the first client.
		if ((allow_multi_client == false) && (userData.admin == false))
		{
			for (Map.Entry<Integer, ClientThread> entry : WOCServer.client_table.entrySet())
			{
				curClientThread = (ClientThread)(entry.getValue());

				if (curClientThread == null) {
					continue;
				}

				// If clients connected from same client machine (same UID), remove the previously connected one from the game.
				if (curClientThread.UserIsInGame() &&
						(curClientThread.GetClientID().equals(_clientThread.GetClientID())) &&
					  (curClientThread.userIsAdmin == false))
				{
					// Log this incident
					Output.PrintToScreen("** EnterGame() - User entered game with 2nd account from same machine, removing the first (client index " + curClientThread.GetClientIndex()+ ", userID " + curClientThread.GetUserID() + "). User " + userData.name + " (" + _userID + ") ID: " + _clientThread.GetClientID() + ", IP: " + _clientThread.GetClientIP());

					// Remove the first client on this same machine from the game, and exit the loop.
					ForceExitGame(curClientThread, ClientString.Get("svr_enter_game_other_client_logged_in", "username", userData.name)); // userData.name + " has logged in using another client."
					break;
				}

				// If the client entering the game has the same IP address of another, unassociated user's client in the game, log it.
				if (curClientThread.UserIsInGame() &&
						(curClientThread.GetClientIP().equals(_clientThread.GetClientIP())) &&
					  (!userData.associated_users.contains(curClientThread.GetUserID())) &&
					  (curClientThread.userIsAdmin == false))
				{
					Constants.WriteToLog("log_hack.txt", Constants.GetTimestampString() + ": User " + _userID + ((_userID > 0) ? (" (" + userData.name + ")") : "") + ", device " + _clientThread.GetDeviceData().ID + " (" + _clientThread.GetDeviceData().device_type + ") has IP " + _clientThread.GetClientIP() + ", same as in-game unassoc user " + curClientThread.GetUserID() + " (" + curClientThread.player_account.username + "), device " + curClientThread.GetDeviceData().ID + " (" + curClientThread.GetDeviceData().device_type + ").\n");
				}
			}
		}

		if (_clientThread.GetDeviceData().device_type.toLowerCase().contains("virtual"))
		{
			// Log possible use of a virtual machine.
			Constants.WriteToLog("log_hack.txt", Constants.GetTimestampString() + ": User " + _userID + ((_userID > 0) ? (" (" + userData.name + ")") : "") + ", device " + _clientThread.GetDeviceData().ID + " (" + _clientThread.GetDeviceData().device_type + "), IP " + _clientThread.GetClientIP() + ": Possible use of a virtual machine.\n");
		}

		// If another client is already logged in with an associated username, log out that first client.
		if (userData.admin == false)
		{
			for (Map.Entry<Integer, ClientThread> entry : WOCServer.client_table.entrySet())
			{
				curClientThread = (ClientThread)(entry.getValue());

				if (curClientThread == null) {
					continue;
				}

				// If clients connected using associted usernames, log out the first client.
				if (curClientThread.UserIsInGame() &&
					  (userData.associated_users.contains(curClientThread.GetUserID())) &&
					  (curClientThread.userIsAdmin == false))
				{
					// Log this incident
					Output.PrintToScreen("** EnterGame() - User '" + userData.name + "' (" + userData.ID + ") entering game, so booting associated user " + curClientThread.GetUserID() + ".");

					// Remove the first client from the game, and exit the loop.
				  ForceExitGame(curClientThread, ClientString.Get("svr_enter_game_other_device_logged_in", "username", userData.name)); // userData.name + " has logged in on another device."
					break;
				}
			}
		}

/*
		// If a second client is attempting to log in using the same activation code as another logged in client, log out the first client.
		if (userData.admin == false)
		{
			ClientThread curClientThread;
			for (Map.Entry<Integer, ClientThread> entry : WOCServer.client_table.entrySet())
			{
				curClientThread = (ClientThread)(entry.getValue());

				if (curClientThread == null) {
					continue;
				}

				// If clients connected using the same activation code, log out the first client.
				if (curClientThread.UserIsInGame() &&
					  (curClientThread.GetClientActivationCode().compareTo(_clientThread.GetClientActivationCode()) == 0) &&
					  (curClientThread.userIsAdmin == false))
				{
					// Log this incident
					Output.PrintToScreen("** EnterGame() - User entered game with 2nd account using same activation code, '" + _clientThread.GetClientActivationCode() + "', removing the first (client index " + curClientThread.GetClientIndex()+ ", userID " + curClientThread.GetUserID() + "). User " + userData.name + " (" + _userID + ") replaces user " + curClientThread.GetUserID() + ")");

					// Remove the first client using this same activation code from the game, and exit the loop.
					ForceExitGame(curClientThread, ClientString.Get("svr_enter_game_other_client_same_code")); // "Another client has logged in using the same activation code."
					break;
				}
			}
		}
*/
		// Determine whether the user is an admin
		boolean user_is_admin = userData.admin;

		if ((WOCServer.num_clients_in_game >= (Constants.max_num_clients - 1)) && (!user_is_admin))
		{
			// Log this rejection, if appropriate.
			if ((WOCServer.log_flags & Constants.LOG_ENTER) != 0)	{
				Output.PrintToScreen((Constants.GetMonth() + 1) + "/" + Constants.GetDate() + " " + Constants.GetHour() + ":" + Constants.GetMinute() + " Client " + _clientThread.GetClientIndex() + " login rejected because " + WOCServer.num_clients_in_game + " clients are logged in.");
			}

			// Return suspend event
			OutputEvents.GetSuspendEvent(_output_buffer, ClientString.Get("svr_enter_game_max_num_clients"), false); // "The maximum number of players has been reached. Please try again in a few minutes."
			WOCServer.QueueClientToDelete(_clientThread);
			Output.PrintToScreen("Rejecting " + userData.name + " because max " + WOCServer.num_clients_in_game + " player count reached.");
			return false;
		}

		// Get the DeviceData.
		DeviceData device_data = _clientThread.GetDeviceData();

		if (!_admin_override)
		{
			// Increment the UserData's count of entering the game using this device.
			// If this is their first login, then the account was just created on this device, so set the count to the min threshold to associate this user with this device immediately.
			if (userData.devices.containsKey(device_data.ID)) {
				userData.devices.put(device_data.ID, userData.devices.get(device_data.ID) + ((userData.login_count == 0) ? Constants.MIN_COMMON_LOGIN_TO_ASSOCIATE_DEVICES : 1));
			} else {
				userData.devices.put(device_data.ID, ((userData.login_count == 0) ? Constants.MIN_COMMON_LOGIN_TO_ASSOCIATE_DEVICES : 1));
			}

			// If this user has entered the game using this device more times than a minimum threshold...
			if (userData.devices.get(device_data.ID) >= Constants.MIN_COMMON_LOGIN_TO_ASSOCIATE_DEVICES)
			{
				// Make sure this user is in the device's list of users.
				if (device_data.users.contains(_userID) == false) {
					device_data.users.add(_userID);
				}

				// Make sure that each of this device's users is co-associated with this user.
				DeviceData.CoassociateUsers(userData, device_data);

				// Make sure this device is co-associated with every other device that this user has entered the game with, more times than that threshold.
				for (Map.Entry<Integer, Integer> entry : userData.devices.entrySet())
				{
					if ((entry.getValue() >= Constants.MIN_COMMON_LOGIN_TO_ASSOCIATE_DEVICES) && (entry.getKey() != device_data.ID))
					{
						// Get the DeviceData for the user's other device that should be associated with this device.
						DeviceData assoc_device_data = (DeviceData)DataManager.GetData(Constants.DT_DEVICE, entry.getKey(), false);

						if (assoc_device_data == null) {
							continue;
						}

						// Co-associate the two devices.
						DeviceData.CoassociateDevices(device_data, assoc_device_data);

						// Make sure that each of the current associated device's users is co-associated with this user.
						DeviceData.CoassociateUsers(userData, assoc_device_data);
					}
				}
			}
		}

		// Determine which devices already in the game should this device mutually track activity correlations with.
		for (Map.Entry<Integer, ClientThread> entry : WOCServer.client_table.entrySet())
		{
			curClientThread = (ClientThread)(entry.getValue());

			// Skip this client if it's not in the game.
			if ((curClientThread == null) || (curClientThread.UserIsInGame() == false)) {
				continue;
			}

			DeviceData cur_device_data = curClientThread.GetDeviceData();

			// Skip this client if it is using the same device, or if the two devices are already associated.
			if ((cur_device_data == device_data) || (device_data.associated_devices.contains((Integer)cur_device_data.ID))) {
				continue;
			}

			// Get the degrees of online correlation between the two devices.
			float cor0 = device_data.GetOnlineCorrelation(cur_device_data.ID);
			float cor1 = cur_device_data.GetOnlineCorrelation(device_data.ID);

			// If both devices have at least a moderate online correlation with the other, or one device has a strong online correlation with the other...
			if (((cor0 >= 0.25f) && (cor1 >= 0.25f)) || (cor0 >= 0.75f) || (cor1 >= 0.75f))
			{
				// Have the two devices mutually track activity correlation with one another.
				device_data.tracking_correlations.put(cur_device_data.ID, device_data.GetCorrelationRecord(cur_device_data));
				cur_device_data.tracking_correlations.put(device_data.ID, cur_device_data.GetCorrelationRecord(device_data));
				//Output.PrintToScreen("   " + userData.name + " (Dvc " + device_data.ID + ") cor with dvc " + cur_device_data.ID + ": " + cor0 + "," + cor1);
				//Output.PrintToScreen("   Tracking correlation.");
			}
		}

		// Update the user's veteran status, based on its associated users.
		userData.UpdateVeteranStatus();

		// If the nation being entered with has rebirthed, make sure the user's veteran status is set to true.
		if (nationData.rebirth_count > 0) {
			userData.veteran = true;
			Output.PrintToScreen("User '" + userData.name + "' (" + userData.ID + ") made vet due to logging in with vet nation.");
		}

		// Sync this user's veteran status with the device's server-independent veteran status.
		userData.SyncServerIndependentVeteranStatus(device_data);

		// Update this user's list of contacts.
		if (userData.prev_update_contacts_day < Constants.GetAbsoluteDay())
		{
			Map.Entry<Integer, Integer> entry;
			int new_val;
			int delta = userData.prev_update_contacts_day - Constants.GetAbsoluteDay();

			// Decrease the value of each of this user's contacts, for the number of days since they were last updated.
			// Remove any contacts with value that is now <= 0.
			for (Iterator<Map.Entry<Integer, Integer>> it = userData.contacts.entrySet().iterator(); it.hasNext(); )
			{
				entry = it.next();
				new_val = entry.getValue() + delta;

				if(new_val > 0) {
					entry.setValue(new_val); // Record new value
				} else {
					it.remove(); // Remove the contact
				}
			}

			// Record the day when this user's contacts have been updated.
			userData.prev_update_contacts_day = Constants.GetAbsoluteDay();
		}

    // Get the current time
    int cur_time = Constants.GetTime();

		// Record the player account's username and e-mail address in the user data, transiently.
		userData.name = _clientThread.player_account.username;
		userData.email = _clientThread.player_account.email;

		// Set the userID in the client's thread, as logged in.
		_clientThread.SetGameInfo(_userID, user_is_admin, true);

		// Add the user to the server's nation_table
		WOCServer.AddUser(_userID, nationID, _clientThread);

		// Record the ClientThread in the user data
		userData.client_thread = _clientThread;

		// Increment the user's login count
		userData.login_count++;

		// Set the user's prev_login_time
		userData.prev_login_time = cur_time;

		// Set the user data's chat spamming related records
		userData.mean_chat_interval = 60000;
		userData.prev_chat_fine_time = Constants.GetFineTime();

		// Reset the user's count of XP earned during the present login.
		userData.cur_login_XP = 0;

		// Determine whether the user is allowed ad bonuses.
		userData.ad_bonuses_allowed = Constants.ad_bonuses;

		if (nationData != null)
		{
			// Increment this nation's count of members online.
			nationData.num_members_online++;

			// Update this user's nation's prev_use_time to the current time
			nationData.prev_use_time = Constants.GetTime();
		}

		// Log this client entering the game, if appropriate.
		if ((WOCServer.log_flags & Constants.LOG_ENTER) != 0) {
		  Output.PrintToScreen((Constants.GetMonth() + 1) + "/" + Constants.GetDate() + " " + Constants.GetHour() + ":" + Constants.GetMinute() + " EnterGame() client " + _clientThread.GetClientIndex() + ", IP " + _clientThread.GetClientIP() + ", ID: " + _clientThread.clientID + ((_clientThread.GetClientActivationCode() != "") ? (", code: " + _clientThread.GetClientActivationCode()) : "") + ", user " + userData.name + " (" + _userID + ")" + (user_is_admin ? " ADMIN" : "") + " of nation " + ((nationData == null) ? "<NONE>" : nationData.name) + " (" + nationID + ") (total " + WOCServer.num_clients_in_game + ")");
		}

		// Determine the user's fealty limits.
		DetermineFealtyLimit(userData, nationData);

		// Make any necessary repairs to this nation's data
		nationData.Repair();

		// Send the game start up events, to enter the game.
		OutputEvents.GetGameStartUpEvent(_output_buffer, _userID, _clientThread);

	  // Add the message event string
	  OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_login_welcome", "username", userData.name, "nation_name", nationData.name)); // "Welcome, " + userData.name + " of " + nationData.name + "!"

		// Broadcast the updated members list (showing this player being online) to all online players belonging to this nation.
		OutputEvents.BroadcastMembersEvent(nationID);

		// Set the user's view, according to which map it is on.
		Display.SetUserViewForMap(userData, _output_buffer);

		// Update the quests system for this user entering the game.
		Quests.HandleEnterGame(nationData);

		// Mark the user's data to be updated
		DataManager.MarkForUpdate(userData);

		// Mark the nation's data to be updated
		DataManager.MarkForUpdate(nationData);

		return true;
	}

	static void DetermineFealtyLimit(UserData _userData, NationData _nationData)
	{
		// Init the user's fealty limit.
		_userData.fealty_end_time = Constants.GetTime();
		_userData.fealty_nationID = -1;
		_userData.fealty_tournament_end_time = Constants.GetTime();
		_userData.fealty_tournament_nationID = -1;
		_userData.fealty_num_nations_at_tier = 0;
		_userData.fealty_num_nations_in_tournament = 0;

		// If the user is an admin, do not apply any fealty limit.
		if (_userData.admin) {
			return;
		}

		// Apply any fealty limit from any of the user's own devices.
		ApplyFealtyLimit_FromUser(_userData, _nationData, _userData);

		// Apply any fealty limit from any of the user's associated users' devices.
		for (int i = 0; i < _userData.associated_users.size(); i++)
		{
			UserData assoc_user_data = (UserData)DataManager.GetData(Constants.DT_USER, _userData.associated_users.get(i), false);
			ApplyFealtyLimit_FromUser(_userData, _nationData, assoc_user_data);
		}
	}

	static void ApplyFealtyLimit_FromUser(UserData _userData, NationData _nationData, UserData _limiting_user_data)
	{
		// Apply any fealty limit imposed by any of the user's devices.
		for (Integer deviceID : _limiting_user_data.devices.keySet())
		{
			DeviceData device_data = (DeviceData)DataManager.GetData(Constants.DT_DEVICE, deviceID, false);
			//Output.PrintToScreen("Apply fealty from user " + _limiting_user_data.name + "'s device " + device_data.device_type);
			ApplyFealtyLimit_FromDevice(_userData, _nationData, device_data);
		}

		NationData userNationData = (NationData)DataManager.GetData(Constants.DT_NATION, _limiting_user_data.nationID, false);

		// Keep count of how many of this player's users are in a nation that's in the tournament.
		if ((userNationData.tournament_active) && (userNationData.tournament_start_day == TournamentData.instance.start_day)) {
			_userData.fealty_num_nations_in_tournament++;
		}

		//Output.PrintToScreen("Applying fealty from limiting user " + _limiting_user_data.name + ", nation: " + userNationData.name + ", fealty tier: " + DetermineFealtyTier(userNationData.level) + ". Logging in nation's tier: " + DetermineFealtyTier(_nationData.level));

		// Keep count of how many of this player's users are in the given nation's fealty tier.
		if (DetermineFealtyTier(_nationData.level) == DetermineFealtyTier(userNationData.level)) {
			_userData.fealty_num_nations_at_tier++;
		}
	}

	static void ApplyFealtyLimit_FromDevice(UserData _userData, NationData _nationData, DeviceData _device_data)
	{
		if (_device_data == null) {
			return;
		}

		if (_nationData.level >= Constants.FEALTY_2_MIN_LEVEL)
		{
			if ((_device_data.fealty2_nationID > 0) && (_device_data.fealty2_nationID != _nationData.ID))
			{
				int fealty_end_time = _device_data.fealty2_prev_attack_time + Constants.FEALTY_2_PERIOD;
				if (fealty_end_time > _userData.fealty_end_time)
				{
					_userData.fealty_end_time = fealty_end_time;
					_userData.fealty_nationID = _device_data.fealty2_nationID;
				}
			}
		}
		else if (_nationData.level >= Constants.FEALTY_1_MIN_LEVEL)
		{
			if ((_device_data.fealty1_nationID > 0) && (_device_data.fealty1_nationID != _nationData.ID))
			{
				int fealty_end_time = _device_data.fealty1_prev_attack_time + Constants.FEALTY_1_PERIOD;
				if (fealty_end_time > _userData.fealty_end_time)
				{
					_userData.fealty_end_time = fealty_end_time;
					_userData.fealty_nationID = _device_data.fealty1_nationID;
				}
			}
		}
		else if (_nationData.level >= Constants.FEALTY_0_MIN_LEVEL)
		{
			if ((_device_data.fealty0_nationID > 0) && (_device_data.fealty0_nationID != _nationData.ID))
			{
				int fealty_end_time = _device_data.fealty0_prev_attack_time + Constants.FEALTY_0_PERIOD;
				if (fealty_end_time > _userData.fealty_end_time)
				{
					_userData.fealty_end_time = fealty_end_time;
					_userData.fealty_nationID = _device_data.fealty0_nationID;
				}
			}
		}

		// If the nation is active in the current tournament, apply tournament fealty.
		if ((_nationData.tournament_active) && (_nationData.tournament_start_day == TournamentData.instance.start_day))
		{
			if ((_device_data.fealty_tournament_nationID > 0) && (_device_data.fealty_tournament_nationID != _nationData.ID) && (_device_data.fealty_tournament_start_day == TournamentData.instance.start_day))
			{
				int fealty_end_time = TournamentData.instance.end_time;
				if (fealty_end_time > _userData.fealty_tournament_end_time)
				{
					_userData.fealty_tournament_end_time = fealty_end_time;
					_userData.fealty_tournament_nationID = _device_data.fealty_tournament_nationID;
				}
			}
		}
	}

	static int DetermineFealtyTier(int _level)
	{
		if (_level >= Constants.FEALTY_2_MIN_LEVEL) {
			return 2;
		} else if (_level >= Constants.FEALTY_1_MIN_LEVEL) {
			return 1;
		}	else if (_level >= Constants.FEALTY_0_MIN_LEVEL) {
			return 0;
		}

		return -1;
	}

	static void ExitGame(ClientThread _clientThread, boolean _voluntary)
	{
		// If the given client's user is not in the game, do nothing.
		if (_clientThread.UserIsInGame() == false) {
			return;
		}

		if (_clientThread.GetUserID() < 1)
		{
			Output.PrintToScreen("** ERROR: ExitGame() called for client " + _clientThread.GetClientIndex() + ", userID: " + _clientThread.GetUserID());
			return;
		}

		// Get the data for user
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _clientThread.GetUserID(), false);

		// Determine whether the user is viewing a raid.
		boolean user_viewing_raid = (userData.mapID >= Raid.RAID_ID_BASE);

		// Contribute a share of the XP this user has earned while logged in, to this user's patron and/or followers.
		if (userData.cur_login_XP > 0)
		{
			// If the user has a patron...
			if (userData.patronID != -1)
			{
				// Get the data for the user's patron
				UserData patronUserData = (UserData)DataManager.GetData(Constants.DT_USER, userData.patronID, false);

				if (patronUserData != null)
				{
					int patron_xp_contribution = (int)(userData.cur_login_XP * Constants.PATRON_XP_SHARE + 0.5f);

					// Get the patron's nation data
					NationData patronNationData = (NationData)DataManager.GetData(Constants.DT_NATION, patronUserData.nationID, false);

					// Add the determined patron_xp_contribution of XP to the patron's nation and user.
					Gameplay.AddXP(patronNationData, patron_xp_contribution, userData.patronID, -1, -1, true, true, 0, Constants.XP_PATRON);

					// Log suspect
					if (patronNationData.log_suspect_expire_time > Constants.GetTime())
					{
						// Log the details of this xp gain.
						Constants.WriteToLog("log_suspect.txt", Constants.GetTimestampString() + ": '" + patronNationData.name + "'(ID:" + patronNationData.ID + ", Level:" + patronNationData.level + ") received " + patron_xp_contribution + " XP for being patron of user " + userData.name + " (" + userData.ID + ").\n");
					}

					// Add this XP contribution to the patron's login report.
					patronUserData.ModifyReportValueFloat(UserData.ReportVal.report__follower_XP, patron_xp_contribution);

					// Add the record of this XP contribution to the patron's record for this follower.
					for (FollowerData follower : patronUserData.followers)
					{
						if (follower.userID == userData.ID)
						{
							follower.bonusXP += patron_xp_contribution;
							break;
						}
					}

					// Send message to the receiving user, if they're online.
					OutputEvents.SendMessageEvent(userData.patronID, ClientString.Get("svr_patron_xp_to_patron", "xp", String.format("%,d", patron_xp_contribution), "username", userData.name));

					// Update the patron's user and nation data.
					DataManager.MarkForUpdate(patronUserData);
					DataManager.MarkForUpdate(patronNationData);
				}
			}

			int follower_xp_contribution = (int)(userData.cur_login_XP * Constants.FOLLOWER_XP_SHARE + 0.5f);
			int single_follower_xp_contribution = (userData.followers.size() == 0) ? 0 : (int)((float)follower_xp_contribution / userData.followers.size() + 0.5f);

			// Add the follower_xp_contribution to this user's record of patron contributions for the current month.
			userData.cur_month_patron_bonus_XP += follower_xp_contribution;

			for (FollowerData follower : userData.followers)
			{
				// Get the user data for the user's current follower.
				UserData followerUserData = (UserData)DataManager.GetData(Constants.DT_USER, follower.userID, false);

				if (followerUserData == null)
				{
					Output.PrintToScreen("ERROR: user " + userData.name + " (" + userData.ID + ") has follower (ID " + follower.userID + ") with missing user data.");
					continue;
				}

				// Get the follower's nation's data.
				NationData followerNationData = (NationData)DataManager.GetData(Constants.DT_NATION, followerUserData.nationID, false);

				// Add the determined follower_xp_contribution of XP to the follower's nation and user.
				Gameplay.AddXP(followerNationData, single_follower_xp_contribution, followerUserData.ID, -1, -1, true, true, 0, Constants.XP_FOLLOWER);

				// Log suspect
				if (followerNationData.log_suspect_expire_time > Constants.GetTime())
				{
					// Log the details of this xp gain.
					Constants.WriteToLog("log_suspect.txt", Constants.GetTimestampString() + ": '" + followerNationData.name + "'(ID:" + followerNationData.ID + ", Level:" + followerNationData.level + ") received " + single_follower_xp_contribution + " XP for being follower of user " + userData.name + " (" + userData.ID + ").\n");
				}

				// Add the follower_xp_contribution to the record of the amount this follower has received from their current patron.
				followerUserData.total_patron_xp_received += single_follower_xp_contribution;

				// Add this XP contribution to the follower's login report.
				followerUserData.ModifyReportValueFloat(UserData.ReportVal.report__patron_XP, single_follower_xp_contribution);

				// Send message to the receiving user, if they're online.
				OutputEvents.SendMessageEvent(follower.userID, ClientString.Get("svr_patron_xp_to_follower", "xp", String.format("%,d", single_follower_xp_contribution), "username", userData.name));

				// Update the follower's user and nation data.
				DataManager.MarkForUpdate(followerUserData);
				DataManager.MarkForUpdate(followerNationData);
			}

			// Reset the user's record of XP earned during the current login.
			userData.cur_login_XP = 0;
		}

		// Get the DeviceData
		DeviceData device_data = _clientThread.GetDeviceData();

		// Remove this device from the tracking_correlations list of any devices that this device has been mutually tracking correlations with.
		for (CorrelationRecord cor_record : device_data.tracking_correlations.values())
		{
			DeviceData cur_device_data = (DeviceData)DataManager.GetData(Constants.DT_DEVICE, cor_record.deviceID, false);

			if (cur_device_data != null) {
				cur_device_data.tracking_correlations.remove((Integer)device_data.ID);
			}
		}

		// Clear this device's list of tracking_correlations.
		device_data.tracking_correlations.clear();

		// The user is no longer viewing a map.
		Display.ResetUserView(userData);

		// Get the user's nationID
		int nationID = userData.nationID;

		// Remove the user from the server's nation_table
		WOCServer.RemoveUser(_clientThread.GetUserID(), nationID, _clientThread);

		// Broadcast the updated members list (showing this player being offline) to all online players belonging to this nation.
		OutputEvents.BroadcastMembersEvent(nationID);

		// Determine the time the user has played during this session
		int play_time = Constants.GetTime() - userData.prev_login_time;

		// Add this session's play time to the user's total
		userData.play_time += play_time;

		// Record the time this player logged out, this is part of an exploit fix where players would log into the
		// server faster than 8 seconds it takes to complete an attack. Allowing them to make attacks much more quickly.
		userData.prev_logout_time = Constants.GetTime();

		// Remove the ClientThread from the user data
		userData.client_thread = null;

		// Mark the user's data to be updated
		DataManager.MarkForUpdate(userData);

		// Get the user's nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

		if (nationData != null)
		{
			// Decrement this nation's count of members online.
			nationData.num_members_online--;

			// If the nation's only online member has exited the game, clear the nation's list of hostile nations.
			if (nationData.num_members_online == 0) {
				nationData.ClearHostileNations();
			}

			// Update this user's nation's prev_use_time to the current time
			nationData.prev_use_time = Constants.GetTime();

			// Mark the nation to be updated
			DataManager.MarkForUpdate(nationData);

			// Update the global record of technology play times for today, for each tech available to this user's nation.
			Technology.UpdateTechPlayTimes(userData, nationData, play_time);
		}

		// If appropriate, alert the raid system that a user has stopped viewing their raid.
		if (user_viewing_raid) {
			Raid.OnUserStoppedViewingRaid(userData);
		}

		// Mark the user's DeviceData to be updated, to record any changes made for fealty or max_level.
		DataManager.MarkForUpdate(device_data);

		// Initialize the user's login report data, to begin collecting data for their next login.
		InitLoginReportData(userData, nationData);

		// Log this ExitGame(), if appropriate.
		if ((WOCServer.log_flags & Constants.LOG_ENTER) != 0) {
			Output.PrintToScreen((Constants.GetMonth() + 1) + "/" + Constants.GetDate() + " " + Constants.GetHour() + ":" + Constants.GetMinute() + " ExitGame() for client " + _clientThread.GetClientIndex() + ", ID: " + _clientThread.clientID + ", user " + userData.name + "(" + _clientThread.GetUserID() + ") (total " + WOCServer.num_clients_in_game + ")");
		}
	}

	static void AttemptLogin(StringBuffer _output_buffer, String _username, String _password, boolean _admin_override, ClientThread _clientThread)
	{
		boolean success = true;
		ClientString message = ClientString.Get();

		// If too many incorrect passwords have been entered recently, don't allow this attempt.
		int time_until_allowed = _clientThread.GetTimeBeforePasswordAllowed();
		if (time_until_allowed > 0)
		{
			// Return error message
			Output.PrintToScreen("AttemptLogin() for username " + _username + " failed due to too many incorrect passwords. Client uid " + _clientThread.GetDeviceData().uid + ".");
			int mins_until_allowed = (time_until_allowed + 59) / 60;
			message.SetString("svr_log_in_too_many_incorrect_passwords", "num_mins", String.valueOf(mins_until_allowed), "minute_quant", ((mins_until_allowed != 1) ? "{Server Strings/minute_quant_plural}" : "{Server Strings/minute_quant_singular}")); // "Please try again in " + mins_until_allowed + " minute" + ((mins_until_allowed > 1) ? "s" : "") + "."
			success = false;
		}

		if (success)
		{
			// Fetch player account data.
			PlayerAccountData accountData = AccountDB.ReadPlayerAccountByUsername(_username);

			if (accountData == null)
			{
				Output.PrintToScreen("AttemptLogin() for username " + _username + " failed due to unknown username. Client uid " + _clientThread.GetDeviceData().uid + ".");
				message.SetString("svr_log_in_unknown_account_name"); // "No account exists with that name."
				success = false;
			}

			// Check whether password is correct.
			//Output.PrintToScreen("Account passhash: " + accountData.passhash + ", given passhash: " + org.apache.commons.codec.digest.DigestUtils.sha256Hex("" + _password)); // TESTING
			if (success && (_admin_override == false) && (accountData.passhash.equals(org.apache.commons.codec.digest.DigestUtils.sha256Hex("" + _password)) == false))
			{
				// Log incorrect password login attempt
				Output.PrintToScreen(Constants.GetShortTimeString() + " wrong password entered for username " + _username + " by client " + _clientThread.GetClientIndex() + ", IP " + _clientThread.GetClientIP() + ", ID: " + _clientThread.clientID);

				// Record that a bad password has been entered from this client thread.
				_clientThread.BadPasswordEntered();

				message.SetString("svr_log_in_incorrct_password"); // "Incorrect password."
				success = false;
			}

			if (success)
			{
				// If the client thread is considered logged in, log out this client thread before logging it back in.
				if (_clientThread.logged_in == true)
				{
					// Log out the client's current player account.
					Login.Logout(_clientThread, false);
				}

				// Record the new player account with the client thread.
				_clientThread.SetPlayerAccount(accountData);

				// Determine the userID to log in as.
				int userID = DetermineLoginUserID(_clientThread);

				// Check whether this client is not allowed to log in to this user account due to having the maximum number of recently active accounts.

				int account_login_delay = Application.DetermineRecentAccountDelay(_clientThread.device_data, userID);

				if ((account_login_delay > 0) && (_admin_override == false))
				{
					Output.PrintToScreen("AttemptLogin() for username " + _username + " failed due to max accounts login delay. Client uid " + _clientThread.GetDeviceData().uid + ".");
					OutputEvents.GetRequestorDurationEvent(_output_buffer, ClientString.Get("svr_max_accounts_log_in_delay", "max_accounts", "" + Constants.max_accounts_per_period, "max_account_days", "" + (Constants.max_accounts_period / Constants.SECONDS_PER_DAY)), account_login_delay);
					return;
				}

				// If this player account is not yet associated with a userID on this server, then create a user and nation for this player.
				if (userID == -1) {
					userID = Application.CreateUserAndNation(_clientThread, accountData, null);
				}

				if (_admin_override) Output.PrintToScreen("Admin about to enter game as '" + _username + "' (" + userID + ").");

				// Have the determined player account enter the game on this server.
				success = EnterGame(_output_buffer, _clientThread, userID, _admin_override);

				if (!_admin_override)
				{
					// Associate the client's device with the player account that is being logged in.
					DeviceData.AssociateDeviceWithPlayer(_clientThread.clientID, _clientThread.clientUID, accountData.ID);

					// Now that we know the user has succeeded in logging in, associate the user's account with its e-mail address.
					EmailData.AssociateEmailWithUser(accountData.email, userID);
				}

				//// Log this logging in, if appropriate.
				//if ((WOCServer.log_flags & Constants.LOG_LOGIN) != 0) {
					//Output.PrintToScreen((Constants.GetMonth() + 1) + "/" + Constants.GetDate() + " " + Constants.GetHour() + ":" + Constants.GetMinute() + " AttemptLogin() for client " + _clientThread.GetClientIndex() + ", player " + accountData.username + "(" + accountData.ID + ")");
				//}
			}
		}

		if (_admin_override) Output.PrintToScreen("Admin attempt to enter game as '" + _username + "': " + (success ? "succeeded." : ("failed. " + message.GetJSON())));

		// Encode event letting client know whether this login succeeded.
		Constants.EncodeString(_output_buffer, "log_in_result");
		Constants.EncodeUnsignedNumber(_output_buffer, success ? 1 : 0, 1);
		message.Encode(_output_buffer);
		ClientString.Release(message);
	}

	public static void Logout(ClientThread _clientThread, boolean _voluntary)
	{
		// Log this logging out, if appropriate.
		if ((WOCServer.log_flags & Constants.LOG_LOGIN) != 0)
		{
			UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _clientThread.GetUserID(), false);
			Output.PrintToScreen((Constants.GetMonth() + 1) + "/" + Constants.GetDate() + " " + Constants.GetHour() + ":" + Constants.GetMinute() + " Logout() for client " + _clientThread.GetClientIndex() + ", user " + userData.name + "(" + _clientThread.GetUserID() + ") (total " + WOCServer.num_clients_in_game + ")");
		}

		if (_clientThread.UserIsInGame())
		{
			// Have the client exit the game.
			ExitGame(_clientThread, _voluntary);
		}

		// Disassociate the client's device from the player account that is being logged out.
		DeviceData.AssociateDeviceWithPlayer(_clientThread.clientID, _clientThread.clientUID, -1);

		// Clear the player account and user info in the client's thread.
		_clientThread.SetPlayerAccount(null);
		_clientThread.SetGameInfo(-1, false, false);
	}

	public static void QuitClient(ClientThread _clientThread)
	{
		//// Attempt to find bug that allows logged out user to remain in nation_table.
		//Output.PrintToScreen("QuitClient() called for user " + _clientThread.GetUserID() + ", client index: " + _clientThread.GetClientIndex() + ", ID: " + _clientThread.GetClientID() + ", logged in: " + _clientThread.UserIsLoggedIn());

		if ((_clientThread.GetUserID() != -1) && _clientThread.UserIsLoggedIn()) {
			ExitGame(_clientThread, true);
		}

		WOCServer.QueueClientToDelete(_clientThread);
	}

	public static int DetermineLoginUserID(ClientThread _clientThread)
	{
		// Get the userID already associated with the client thread, if there is one (as there would be if an account was just created.
		// Better to get it from the client thread than searching the UserData DB (below), which may not have been updated since the user was created.)
		int userID = _clientThread.GetUserID();

		if (userID == -1)
		{
			// Get this game server's userID that is associated with the client's player account.
			userID = UserData.GetUserIDByPlayerID(_clientThread.player_account.ID);
		}

		return userID;
	}

	public static void InitLoginReportData(UserData _userData, NationData _nationData)
	{
		_userData.report__defenses_squares_defeated = 0;
		_userData.report__defenses_XP = 0;
		_userData.report__defenses_lost = 0;
		_userData.report__defenses_built = 0;
		_userData.report__walls_lost = 0;
		_userData.report__walls_built = 0;
		_userData.report__attacks_squares_captured = 0;
		_userData.report__attacks_XP = 0;
		_userData.report__levels_gained = 0;
		_userData.report__orb_count_delta = 0;
		_userData.report__orb_credits = 0;
		_userData.report__orb_XP = 0;
		_userData.report__farming_XP = 0;
		_userData.report__resource_count_delta = 0;
		_userData.report__land_lost = 0;
		_userData.report__energy_begin = (int)_nationData.energy;
		_userData.report__energy_spent = 0;
		_userData.report__energy_donated = 0;
		_userData.report__energy_lost_to_raids = 0;
		_userData.report__manpower_begin = (int)_nationData.GetFootprint(Constants.MAINLAND_MAP_ID).manpower;
		_userData.report__manpower_spent = 0;
		_userData.report__manpower_lost_to_resources = 0;
		_userData.report__manpower_donated = 0;
		_userData.report__manpower_lost_to_raids = 0;
		_userData.report__credits_begin = (int)_nationData.game_money;
		_userData.report__credits_spent = 0;
		_userData.report__patron_XP = 0;
		_userData.report__patron_credits = 0;
		_userData.report__follower_XP = 0;
		_userData.report__follower_credits = 0;
		_userData.report__follower_count = 0;
		_userData.report__raids_fought = 0;
		_userData.report__medals_delta = 0;
		_userData.report__rebirth = 0;
		_userData.report__home_defense_credits = 0;
		_userData.report__home_defense_xp = 0;
		_userData.report__home_defense_rebirth = 0;
	}

  public static void ForgotPassword(StringBuffer _output_buffer, String _username, ClientThread _clientThread)
  {
		/*
    // Get ID of user with given username
		int userID = UserData.GetUserIDByUsername(_username);

		// If the username maps to no userID, report error.
		if (userID == -1)
		{
			// Return error message
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("There is no known user named '" + _username + "'."));
			return;
		}

		// Attempt to get the data for user
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, userID, false);

		// If user with given name not found, report error.
		if (userData == null)
		{
			// Return error message
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("There is no user named '" + _username + "'."));
			return;
		}

// We used to email lost passwords but now all accounts require security, if players haven't implemented it,
// that is really too bad, as we gave them 14 months to add a security question and answer to their accounts

    if (userData.security_question == 0)
    {
/*
      // Send an email to the user specifying the account's password
		  String body_string = "Welcome to War of Conquest! Here is the user name and password for your player account.\n\n" +
			  "user name: " + userData.name + "\n" +
			  "password: " + userData.password + "\n\n" +
			  "Please save this information for future reference. Have fun, and good luck in your conquests!\n" +
			  "\n";
		  Constants.SendEmail("automated-message@warofconquest.com", "War of Conquest", userData.email, "Your 'War of Conquest' account information", body_string);
* /
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("There is no security question for this account. Password is not retrievable."));
			return;
    }

    // Get the security question event
    OutputEvents.GetSecurityQuestionEvent(_output_buffer, userID);
		*/
  }

	public static void SendPingRequest(ClientThread _clientThread)
	{
		// Acquire the output buffer
		WOCServer.temp_output_buffer_semaphore.acquire();

		// Send ping request to client
		WOCServer.temp_output_buffer.setLength(0);
		Constants.EncodeString(WOCServer.temp_output_buffer, "request_ping");
		_clientThread.TerminateAndSendNow(WOCServer.temp_output_buffer);

		// Release the output buffer
		WOCServer.temp_output_buffer_semaphore.release();
	}

	public static void ForceExitGame(ClientThread _clientThread, ClientString _message)
	{
		try
		{
			if (_clientThread.UserIsInGame())
			{
				// Force the client to exit the game.
				Login.ExitGame(_clientThread, false);
			}

			// Acquire the output buffer
			WOCServer.temp_output_buffer_semaphore.acquire();

			// Send the suspend message
			WOCServer.temp_output_buffer.setLength(0);
			OutputEvents.GetSuspendEvent(WOCServer.temp_output_buffer, _message, false);
			_clientThread.TerminateAndSendNow(WOCServer.temp_output_buffer);

			// Release the output buffer
			WOCServer.temp_output_buffer_semaphore.release();

			// Remove the client thread from the server.
			WOCServer.QueueClientToDelete(_clientThread);
		}
		catch (Exception e)
		{
			Output.PrintException(e);
		}
	}
}
