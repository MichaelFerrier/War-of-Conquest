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
import WOCServer.*;
import java.awt.image.*;
import java.util.ArrayList;

public class Application
{
	static char[] vowels = {'a','e','i','o','u'};
	static char[] consonants = {'b','c','d','g','h','j','k','l','m','n','p','q','r','s','t','v','w','x','y','z'}; // "f"-less

	static final int EVENT_BUFFER_LENGTH = 24576;
	static StringBuffer event_buffer = new StringBuffer(EVENT_BUFFER_LENGTH);

	// Create a Buffered image for its ColorModel, with which to derive nation pixel color values.
	static BufferedImage buffered_image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);

	static int PREV_NEW_USER_LIST_LENGTH = 3;
	public static ArrayList<Integer> prev_new_user_list = new ArrayList<Integer>();

  public static void AttemptCreateNewPlayer(StringBuffer _output_buffer, ClientThread _clientThread, String _username, String _patron_code)
	{
		ClientString message = ClientString.Get();
		boolean success = true;
		int playerID;

		// Make sure the given _clientThread has an associated device.
		if (_clientThread.device_data == null)
		{
			Output.PrintToScreen("ERROR: AttemptCreateNewPlayer(" + _username + ") _clientThread has no associated DeviceData!");
			message.SetString("Error: device data is missing.");
			success = false;
		}

		// Check whether this client is not allowed to create a new account due to having the maximum number of recently active accounts.

		int account_creation_delay = DetermineRecentAccountDelay(_clientThread.device_data, -1);

		if (account_creation_delay > 0)
		{
			Output.PrintToScreen("AttemptCreateNewPlayer(): Not allowed to create new account due to max accounts limit. Client uid: " + _clientThread.GetDeviceData().uid);
			OutputEvents.GetRequestorDurationEvent(_output_buffer, ClientString.Get("svr_max_accounts_creation_delay", "max_accounts", "" + Constants.max_accounts_per_period, "max_account_days", "" + (Constants.max_accounts_period / Constants.SECONDS_PER_DAY)), account_creation_delay);
			return;
		}

		// Username

		// Remove emojis
		_username = _username.replaceAll("[^\\p{L}\\p{M}\\p{N}\\p{P}\\p{Z}\\p{Cf}\\p{Cs}\\s]","");

		// Remove any control characters from the string
		_username = Constants.RemoveControlCharacters(_username);

		// Check that a username of a sufficient length is given
		if ((_username.length() < Constants.MIN_USERNAME_LEN) || (_username.length() > Constants.MAX_USERNAME_LEN))
		{
			Output.PrintToScreen("AttemptCreateNewPlayer(): Not allowed to create new account due to username length. Client uid: " + _clientThread.GetDeviceData().uid);
			message.SetString("svr_create_account_username_length", "min_len", String.valueOf(Constants.MIN_USERNAME_LEN), "max_len", String.valueOf(Constants.MAX_USERNAME_LEN)); //  "User name must be between " + Constants.MIN_USERNAME_LEN + " and " + Constants.MAX_USERNAME_LEN + " characters long."
			success = false;
		}

		// Find out if there is an existing player account ID mapped to the given name.
		playerID = AccountDB.GetPlayerIDByUsername(_username);

		// If user with given name already exists, return message.
		if (playerID != -1)
		{
			// Create message
			Output.PrintToScreen("AttemptCreateNewPlayer(): Not allowed to create new account due to name being taken. Client uid: " + _clientThread.GetDeviceData().uid);
			message.SetString("svr_create_account_username_taken", "username", _username); // "Username '" + _username + "' is already taken. Please choose another."
			success = false;
		}

		// Check that the name contains no swears
		if (Constants.StringContainsSwear(_username))
		{
			Output.PrintToScreen("AttemptCreateNewPlayer(): Not allowed to create new account due to offensive username. Client uid: " + _clientThread.GetDeviceData().uid);
			message.SetString("svr_create_account_username_offensive"); // Usernames may not contain offensive language.
			success = false;
		}

		if (Constants.StringContainsIllegalWhitespace(_username))
		{
			Output.PrintToScreen("AttemptCreateNewPlayer(): Not allowed to create new account due to illegal whitespace. Client uid: " + _clientThread.GetDeviceData().uid);
			message.SetString("svr_create_account_illegal_whitespace"); // Names may not start or end with spaces, or have two spaces in a row.
			success = false;
		}

		// Patron code

		int patronUserID = -1;
		UserData patronUserData = null;

		if (_patron_code.length() > 0)
		{
			long[] results = new long[2];
			Constants.DecodePatronCode(_patron_code, results);

			// Get the patron userID
			patronUserID = (int)(results[1]);

			// Get the user data of the patron specified by the patron code.
			patronUserData = (UserData)DataManager.GetData(Constants.DT_USER, patronUserID, false);

			if ((patronUserID == -1) || (patronUserData == null))
			{
				message.SetString("svr_create_account_patron_code_invalid"); // The patron code is not valid.
				success = false;
			}
		}

		if (success)
		{
			Output.PrintToScreen("AttemptCreateNewPlayer(): Success. Username: " + _username + ", playerID: " + playerID + ", deviceData's playerID: " + _clientThread.GetDeviceData().playerID + ", client uid: " + _clientThread.GetDeviceData().uid);

			// Create player account
			PlayerAccountData player_account = AccountDB.CreateNewPlayerAccount(_username);

			// Record the player account with the client thread.
			_clientThread.SetPlayerAccount(player_account);

			// Create new user and nation to correspond with this new player account.
			CreateUserAndNation(_clientThread, player_account, patronUserData);
		}

		// Encode event letting client know whether this player creation succeeded.
		Constants.EncodeString(_output_buffer, "new_player_result");
		Constants.EncodeUnsignedNumber(_output_buffer, success ? 1 : 0, 1);
		message.Encode(_output_buffer);
		ClientString.Release(message);
	}

	public static int CreateUserAndNation(ClientThread _clientThread, PlayerAccountData _player_account, UserData _patronUserData)
	{
		int userID, nationID;
		PlayerAccountData player_account = null;
		UserData userData = null;
		NationData nationData = null;

		// Create new user record

		// Determine ID for new user. Skip any IDs that are already in use by a nation or landmap, so that this user's nation and landmap can all use the same ID.
		for (userID = DataManager.GetNextDataID(Constants.DT_USER);; userID++)
		{
			if (DataManager.GetData(Constants.DT_NATION, userID, false) != null) {
				continue;
			}

			if (DataManager.GetLandMap(userID, false) != null) {
				continue;
			}

			break;
		}

		// Create a new record for the new user
		userData = (UserData)DataManager.GetData(Constants.DT_USER, userID, true); // Create

		Output.PrintToScreen("CreateUserAndNation() Setting new user " + userID + "'s playerID to player account's ID: " + _player_account.ID);

		// Set the new user's records
		userData.playerID = _player_account.ID;
		userData.creation_time = Constants.GetTime();
		userData.name = _player_account.username;
		userData.email = _player_account.email;
		userData.patron_code = Constants.EncodePatronCode(Constants.GetServerID(), userID);
		userData.tutorial_state = "";
		userData.creation_device_type = (_clientThread == null) ? "" : _clientThread.device_data.device_type;
		userData.game_ban_end_time = (_clientThread == null) ? -1 : _clientThread.device_data.game_ban_end_time;
		userData.chat_ban_end_time = (_clientThread == null) ? -1 : _clientThread.device_data.chat_ban_end_time;
		userData.mean_chat_interval = 0;
		userData.prev_chat_fine_time = 0;
		userData.login_count = 0;
		userData.prev_login_time = 0;
		userData.play_time = 0;
		userData.admin = false;
		userData.veteran = false;
		userData.mod_level = 0;
		userData.xp = 0;
		userData.nationID = -1;
		userData.flags = Constants.UF_DEFAULT;
		userData.mapID = 1; // TEMP -- default starting map
		userData.mainland_viewX = 300; // TEMP
		userData.mainland_viewY = 300; // TEMP
		userData.homeland_viewX = 0;
		userData.homeland_viewY = 0;
		userData.prev_update_contacts_day = Constants.GetAbsoluteDay();
		userData.prev_check_messages_time = 0;
		userData.patronID = (_patronUserData == null) ? -1 : _patronUserData.ID;
		userData.cur_month_patron_bonus_XP = 0;
		userData.cur_month_patron_bonus_credits = 0;
		userData.prev_month_patron_bonus_XP = 0;
		userData.prev_month_patron_bonus_credits = 0;
		userData.total_patron_xp_received = 0;
		userData.total_patron_credits_received = 0;
		userData.max_num_followers = 0;
		userData.max_num_followers_monthly = 0;

		if (_clientThread != null)
		{
			// Set the client thread's userID
			_clientThread.SetGameInfo(userID, false, false);

			// Create a DeviceData for the client device (if it doesn't yet exist) and associate it with the new player and user accounts.
			DeviceData.AssociateDeviceWithPlayer(_clientThread.clientID, _clientThread.clientUID, _clientThread.player_account.ID);

			// Make sure this user is in the device's list of users.
			if (_clientThread.device_data.users.contains(userID) == false) {
				_clientThread.device_data.users.add(userID);
			}

			// Make sure that each of this device's users is co-associated with this user.
			DeviceData.CoassociateUsers(userData, _clientThread.device_data);
		}

		// Update the new user's veteran status, based on its associated users.
		userData.UpdateVeteranStatus();

		// Sync this user's veteran status with the device's server-independent veteran status.
		userData.SyncServerIndependentVeteranStatus(_clientThread.device_data);

		// Determine if this user's associated users' oldest age should cause this new nation to be considered a veteran nation.
		boolean veteran_by_age = (userData.DetermineOldestAgeOfAssociatedUsers() >= Constants.VETERAN_USER_AGE);

		// If the new user has a patron, add the new user as a follower of the patron.
		if (_patronUserData != null) {
			Comm.AddFollower(_patronUserData, userData);
		}

		// Create new nation record

		// Determine ID for new nation (use the userID, which above was selected such that there is not yet a nation with this ID).
		nationID = userID;

		// Create a new NationData for the new nation
		nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, true); // Create

		// Create a new NationTechData for the new nation
		NationTechData nationTechData = (NationTechData)DataManager.GetData(Constants.DT_NATIONTECH, nationID, true); // Create

		// Create a new NationExtData for the new nation
		NationExtData nationExtData = (NationExtData)DataManager.GetData(Constants.DT_NATION_EXT, nationID, true); // Create

		// Set the new nation's records
		nationData.creation_time = Constants.GetTime();
		nationData.birth_time = Constants.GetTime();
		nationData.prev_use_time = Constants.GetTime();
		nationData.name = "Village" + nationID; // Temporary name until the player customizes it.
		nationData.level = 1;
		nationData.xp = 0;
		nationData.pending_xp = 0;
		nationData.advance_points = 0;
		nationData.password = org.apache.commons.codec.digest.DigestUtils.sha256Hex("" + (Math.random() * 1000000000)).substring(0, 8); // Generate random password
		nationData.r = (int)(Math.random() * 255); // Temporary color until player customizes it.
		nationData.g = (int)(Math.random() * 255); // Temporary color until player customizes it.
		nationData.b = (int)(Math.random() * 255); // Temporary color until player customizes it.
		nationData.emblem_index = -1;
		nationData.emblem_color = 0;
		nationData.veteran = userData.veteran || veteran_by_age;
		nationData.nextTechExpireTime = -1;
		nationData.nextTechExpire = -1;
		nationData.targetAdvanceID = -1;
		nationData.prev_free_migration_time = 0;
		nationData.prev_unite_time = 0;
		nationData.prev_go_incognito_time = 0;
		nationData.game_money = Constants.INIT_GAME_MONEY + ((_patronUserData != null) ? Constants.PATRON_CODE_BONUS_GAME_MONEY : 0);
		nationData.game_money_purchased = 0;
		nationData.game_money_won = 0;
		nationData.total_game_money_purchased = 0;
		nationData.prize_money = 0;
		nationData.prize_money_history = 0;
		nationData.prize_money_history_monthly = 0;
		nationData.money_spent = 0;
		nationData.raid_earnings_history = 0;
		nationData.raid_earnings_history_monthly = 0;
		nationData.orb_shard_earnings_history = 0;
		nationData.orb_shard_earnings_history_monthly = 0;
		nationData.medals_history = 0;
		nationData.medals_history_monthly = 0;
		nationData.xp_history = 0;
		nationData.xp_history_monthly = 0;
		nationData.tournament_trophies_history = 0;
		nationData.tournament_trophies_history_monthly = 0;
		nationData.donated_energy_history = 0;
		nationData.donated_energy_history_monthly = 0.0f;
		nationData.donated_manpower_history = 0;
		nationData.donated_manpower_history_monthly = 0.0f;
		nationData.quests_completed = 0;
		nationData.quests_completed_monthly = 0;
		nationData.captures_history = 0;
		nationData.captures_history_monthly = 0;
		nationData.max_area = 0;
		nationData.max_area_monthly = 0;
		nationData.rebirth_countdown = Constants.REBIRTH_COUNTDOWN_START;
		nationData.rebirth_countdown_start = Constants.REBIRTH_COUNTDOWN_START;
		nationData.flags = 0;
		nationData.prev_message_send_day = 0;
		nationData.message_send_count = 0;
		nationData.prev_alliance_request_day = 0;
		nationData.alliance_request_count = 0;
		nationData.rebirth_count = 0;
		nationData.reset_advances_count = 0;
		nationData.energy = Constants.INIT_ENERGY;
		nationData.energy_max = Constants.INIT_ENERGY_MAX;
		nationData.manpower_max = Constants.INIT_MANPOWER_MAX * Constants.manpower_gen_multiplier;
		nationData.manpower_per_attack = Constants.INIT_MANPOWER_PER_ATTACK;
		nationData.geo_efficiency_modifier = Constants.INIT_GEO_EFFICIENCY_MODIFIER;
		nationData.hit_points_base = Constants.INIT_HIT_POINT_BASE;
		nationData.hit_points_rate = Constants.INIT_HIT_POINTS_RATE;
		nationData.crit_chance = Constants.INIT_CRIT_CHANCE;
		nationData.salvage_value = Constants.INIT_SALVAGE_VALUE;
		nationData.wall_discount = Constants.INIT_WALL_DISCOUNT;
		nationData.structure_discount = Constants.INIT_STRUCTURE_DISCOUNT;
		nationData.splash_damage = Constants.INIT_SPLASH_DAMAGE;
		nationData.max_num_alliances = Constants.INIT_MAX_NUM_ALLIANCES;
		nationData.max_simultaneous_processes = Constants.INIT_MAX_SIMULTANEOUS_PROCESSES;
		nationData.invisibility = false;
		nationData.insurgency = false;
		nationData.total_defense = false;
		nationData.tech_perm = Constants.INIT_STAT_TECH;
		nationData.tech_temp = 0;
		nationData.tech_object = 0;
		nationData.bio_perm = Constants.INIT_STAT_BIO;
		nationData.bio_temp = 0;
		nationData.bio_object = 0;
		nationData.psi_perm = Constants.INIT_STAT_PSI;
		nationData.psi_temp = 0;
		nationData.psi_object = 0;
		nationData.energy_rate_perm = Constants.INIT_ENERGY_RATE;
		nationData.energy_rate_temp = 0;
		nationData.energy_rate_object = 0;
		nationData.manpower_rate_perm = Constants.INIT_MANPOWER_RATE * Constants.manpower_gen_multiplier;
		nationData.manpower_rate_temp = 0;
		nationData.manpower_rate_object = 0;
		nationData.xp_multiplier_perm = Constants.INIT_XP_MULTIPLIER;
		nationData.xp_multiplier_temp = 0;
		nationData.xp_multiplier_object = 0;
		nationData.tech_mult = 1f;
		nationData.bio_mult = 1f;
		nationData.psi_mult = 1f;
		nationData.manpower_rate_mult = 1f;
		nationData.energy_rate_mult = 1f;
		nationData.manpower_max_mult = 1f;
		nationData.energy_max_mult = 1f;
		nationData.hp_per_square_mult = 1f;
		nationData.hp_restore_mult = 1f;
		nationData.attack_manpower_mult = 1f;
		nationData.prev_update_stats_time = Constants.GetTime();
		nationData.tournament_start_day = -1;
		nationData.tournament_active = false;
		nationData.tournament_rank = -1;
		nationData.trophies_available = 0;
		nationData.trophies_banked = 0;
		nationData.trophies_potential = 0;

		nationData.mainland_footprint.manpower = Constants.INIT_MANPOWER * Constants.manpower_gen_multiplier;
		nationData.homeland_footprint.manpower = Constants.INIT_MANPOWER * Constants.MANPOWER_MAX_HOMELAND_FRACTION;

		// Add the initial builds to the nation.
		nationTechData.AddInitialAvailableBuilds();

		// Add the initial technologies to the new nation.
		for (Integer initial_advanceID : TechData.initial_advances) {
			Technology.AddTechnology(nationID, initial_advanceID, 0, false, true, 0);
		}

		// Set the given user's home_nationID and nationID to that of the new nation
		userData.home_nationID = nationID;
		userData.nationID = nationID;

		// Set the user's rank to sovereign
		userData.rank = Constants.RANK_SOVEREIGN;

		// Initialize the new nation's list of users to the user's ID
		nationData.users.add(userID);

		// Initialize the new user's login report data.
		Login.InitLoginReportData(userData, nationData);

		// Establish the most recently registered new players as this new user's first contacts.
		for (int i = 0; i < Application.prev_new_user_list.size(); i++) {
			Comm.RecordContact(userData, Application.prev_new_user_list.get(i), Comm.CONTACT_VALUE_PREV_NEW_USER);
		}

		// Log the creation of this nation account
		Constants.WriteToLog("log_nations.txt", Constants.GetFullDate() + " user " + _player_account.username + " (" + userID + ") of new nation " + nationData.name + " (" + nationID + ")\n");
		Constants.WriteToNationLog(nationData, userData, "Created");

		// Maintain a list of the last few users who've created accounts.

		// Add this user to the list of prev new users.
		prev_new_user_list.add(userID);

		// If the list has exceeded its max size, remove the earliest user from the list.
		if (prev_new_user_list.size() > PREV_NEW_USER_LIST_LENGTH) {
			prev_new_user_list.remove(0);
		}

		// Update the UserData and NationData immediately, so searches for them by name will immediately be successful.
		DataManager.UpdateImmediately(userData);
		DataManager.UpdateImmediately(nationData);

		// Mark the nation's tech data and extended data to be updated.
		DataManager.MarkForUpdate(nationTechData);
		DataManager.MarkForUpdate(nationExtData);

		return userID;
	}

	public static void CustomizeAppearance(StringBuffer _output_buffer, ClientThread _clientThread, int _color_r, int _color_g, int _color_b, int _emblem_index, int _emblem_color)
	{
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _clientThread.GetUserID(), false);

		// If the user's rank disallows this action, return.
		if (userData.rank > Constants.RANK_COSOVEREIGN) {
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_customize_rank_too_low")); // "Only the Sovereign or Co-Sovereign can customize."
			return;
		}

		// Make sure the player isn't trying to use an emblem they're not allowed to use.
		if ((_emblem_index >= Constants.RESTRICTED_EMBLEM_START_INDEX) && (_clientThread.player_account.info.contains("<emblem index=\"" + _emblem_index + "\">") == false) && (userData.admin == false))
		{
			Output.PrintToScreen("Attempt by user " + userData.name + " (" + userData.ID + ") to use restricted emblem " + _emblem_index);
			return;
		}

		// Get the user's nation ID
		int userNationID = userData.nationID;

		// Get the user's nation's data
		NationData userNationData = (NationData)DataManager.GetData(Constants.DT_NATION, userNationID, false);

		if (userNationData.game_money < Constants.CUSTOMIZE_COST)
		{
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_customize_not_enough_credits")); // "We do not have enough credits to change the appearance of our nation."
			return;
		}
		else
		{
			// Take cost from nation
			Money.SubtractCost(userNationData, Constants.CUSTOMIZE_COST);

			// Update the nation's users' reports.
			userNationData.ModifyUserReportValueFloat(UserData.ReportVal.report__credits_spent, Constants.CUSTOMIZE_COST);

			// Broadcast an update event to the nation, letting all players know about the change in credits.
			OutputEvents.BroadcastUpdateEvent(userNationID);
		}

		// Record the nation's new appearance values.
		userNationData.r = _color_r;
		userNationData.g = _color_g;
		userNationData.b = _color_b;
		userNationData.emblem_index = _emblem_index;
		userNationData.emblem_color = _emblem_color;

		// Mark the nation's data to be updated
		DataManager.MarkForUpdate(userNationData);

		// Broadcast message to all players with updated nation appearance data.
		OutputEvents.BroadcastNationDataEvent(userNationData);

		// Return message
		OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_customize_complete"));

		// Record event in history
		Comm.SendReport(userNationData.ID, ClientString.Get("svr_report_customized_by", "username", userData.name), 0); // "Nation's appearance changed by " + userData.name + "."
	}

	public static void CustomizeNation(StringBuffer _output_buffer, ClientThread _clientThread, String _nation_name, int _color_r, int _color_g, int _color_b)
	{
		ClientString message = ClientString.Get();
		boolean success = true;

		Output.PrintToScreen(Constants.GetShortTimeString() + " CustomizeNation(): _nation_name: " + _nation_name + ", client UID: " + _clientThread.GetDeviceData().uid);

		// Get the user data associated with the given client thread
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _clientThread.GetUserID(), false);

		if (userData == null)
		{
			Output.PrintToScreen("CustomizeNation(): client thread userID " + _clientThread.GetUserID() + " has no user data!");
			return;
		}

		// Nation name

		// Remove emojis
		_nation_name = _nation_name.replaceAll("[^\\p{L}\\p{M}\\p{N}\\p{P}\\p{Z}\\p{Cf}\\p{Cs}\\s]","");

		// Remove any control characters from the string
		_nation_name = Constants.RemoveControlCharacters(_nation_name);

		// Check that a nation name of a sufficient length is given
		if ((_nation_name.length() < Constants.MIN_NATION_NAME_LEN) || (_nation_name.length() > Constants.MAX_NATION_NAME_LEN))
		{
			message.SetString("svr_create_account_nation_name_length", "min_len", String.valueOf(Constants.MIN_NATION_NAME_LEN), "max_len", String.valueOf(Constants.MAX_NATION_NAME_LEN)); // Nation name must be between {min_len} and {max_len} characters long.
			success = false;
		}

		// Find out if there is an existing nation ID mapped to the given name.
		int nationID = NationData.GetNationIDByNationName(_nation_name);

		// If a different nation with the given name already exists, return message.
		if ((nationID != -1) && (nationID != userData.nationID))
		{
			// Create message
			message.SetString("svr_create_account_nation_name_taken", "nation_name", _nation_name); // There is already a nation named '{nation_name}'. Please choose another.
			success = false;
		}

		// Check that the nation name contains no swears
		if (Constants.StringContainsSwear(_nation_name))
		{
			message.SetString("svr_create_account_nation_name_offensive"); // Nation names may not contain offensive language.
			success = false;
		}

		// Check that the nation name doesn't use whitespace illegally.
		if (Constants.StringContainsIllegalWhitespace(_nation_name))
		{
			message.SetString("svr_create_account_illegal_whitespace"); // Names may not start or end with spaces, or have two spaces in a row.
			success = false;
		}

		if (success)
		{
			// Get the nation data associated with the client's user
			NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, userData.nationID, false);

			// Record the nation's new name
			nationData.name = _nation_name;

			// Record the nation's new color
			nationData.r = Math.max(0, Math.min(255, _color_r));
			nationData.g = Math.max(0, Math.min(255, _color_g));
			nationData.b = Math.max(0, Math.min(255, _color_b));

			// Record a flag noting that the nation's name has been chosen by a player, so it won't ask again.
			nationData.flags = nationData.flags | Constants.NF_CUSTOM_NATION_NAME;

			// Mark the nation's data to be updated.
			DataManager.MarkForUpdate(nationData);

			if (_clientThread.UserIsInGame())
			{
				// Send account info event to client with updated information.
				OutputEvents.GetAccountInfoEvent(_output_buffer, _clientThread.GetUserID(), _clientThread);

				// Broadcast the nation's changed flags to each of the nation's players.
				OutputEvents.BroadcastNationFlagsEvent(userData.nationID, nationData.flags);

				// Send full map event to client with updated information.
				Display.GetFullMapEvent(_output_buffer, userData, false);

				Output.PrintToScreen("CustomizeNation(): success, user is already in game.");
			}

			// If the client has not yet entered the game, attempt to enter the game.
			if (_clientThread.UserIsInGame() == false) {
				Output.PrintToScreen("CustomizeNation(): success, calling AttemptEnterGame().");
				Login.AttemptEnterGame(_output_buffer, _clientThread);
			}
		}

		// Encode event letting client know whether this operation succeeded.
		Output.PrintToScreen("CustomizeNation() result: success: " + success + ", message: " + message.GetFormatID());
		Constants.EncodeString(_output_buffer, "customize_nation_result");
		Constants.EncodeUnsignedNumber(_output_buffer, success ? 1 : 0, 1);
		message.Encode(_output_buffer);
		ClientString.Release(message);
	}

	public static void SetUsername(StringBuffer _output_buffer, ClientThread _clientThread, String _username, boolean _check)
	{
		boolean success = true;

		// Remove emojis
		_username = _username.replaceAll("[^\\p{L}\\p{M}\\p{N}\\p{P}\\p{Z}\\p{Cf}\\p{Cs}\\s]","");

		// Remove any control characters from the string
		_username = Constants.RemoveControlCharacters(_username);

		// Check that a username of a sufficient length is given
		if ((_username.length() < Constants.MIN_USERNAME_LEN) || (_username.length() > Constants.MAX_USERNAME_LEN))
		{
			success = false;
		}
		else
		{
			int ID = AccountDB.GetPlayerIDByUsername(_username);

			if ((ID != -1) && (ID != _clientThread.player_account.ID))
			{
				success = false;
			}
		}

		if (Constants.StringContainsSwear(_username))
		{
			success = false;
		}

		if (Constants.StringContainsIllegalWhitespace(_username))
		{
			success = false;
		}

		if (_username.indexOf(' ') != -1)
		{
			success = false;
		}

		// Change the username, if appropriate.
		if (success && !_check)
		{
			// Fill in username field in player account data.
			_clientThread.player_account.username = _username;

			// Store the player account data.
			AccountDB.WritePlayerAccount(_clientThread.player_account);

			// Record the new username in the user data.
			UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _clientThread.GetUserID(), false);
			userData.name = _username;

			// Record a flag noting that the player's name has been chosen by the player, so it won't ask again.
			userData.flags = userData.flags | Constants.UF_CUSTOM_USERNAME;

			// Send account info event to client with updated information.
			OutputEvents.GetAccountInfoEvent(_output_buffer, _clientThread.GetUserID(), _clientThread);

			// Broadcast list of members to each of the user's nation's members, with the user's new account name.
			OutputEvents.BroadcastMembersEvent(userData.nationID);
		}

		// Encode event letting client know whether this username is available.
		Constants.EncodeString(_output_buffer, "username_available");
		Constants.EncodeUnsignedNumber(_output_buffer, success ? 1 : 0, 1);
		Constants.EncodeUnsignedNumber(_output_buffer, (success && !_check) ? 1 : 0, 1);
	}

	public static void SetNationName(StringBuffer _output_buffer, ClientThread _clientThread, String _nation_name, boolean _check)
	{
		boolean success = true;

		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _clientThread.GetUserID(), false);
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, userData.nationID, false);

		// Remove emojis
		_nation_name = _nation_name.replaceAll("[^\\p{L}\\p{M}\\p{N}\\p{P}\\p{Z}\\p{Cf}\\p{Cs}\\s]","");

		// Remove any control characters from the string
		_nation_name = Constants.RemoveControlCharacters(_nation_name);

		// Check that a nation name of a sufficient length is given
		if ((_nation_name.length() < Constants.MIN_NATION_NAME_LEN) || (_nation_name.length() > Constants.MAX_NATION_NAME_LEN))
		{
			success = false;
		}
		else
		{
			int ID = NationData.GetNationIDByNationName(_nation_name);

			if ((ID != -1) && (ID != userData.nationID))
			{
				success = false;
			}
		}

		if (Constants.StringContainsSwear(_nation_name))
		{
			success = false;
		}

		if (Constants.StringContainsIllegalWhitespace(_nation_name))
		{
			success = false;
		}

		if (_nation_name.indexOf(' ') != -1)
		{
			success = false;
		}

		// Change the nation's name, if appropriate.
		if (success && !_check)
		{
			// Set the nation's name.
			nationData.name = _nation_name;

			// Record a flag noting that the nation's name has been chosen by a player, so it won't ask again.
			nationData.flags = nationData.flags | Constants.NF_CUSTOM_NATION_NAME;

			// Mark the nation's data to be updated.
			DataManager.MarkForUpdate(nationData);

			// Send account info event to client with updated information.
			OutputEvents.GetAccountInfoEvent(_output_buffer, _clientThread.GetUserID(), _clientThread);

			// Broadcast the nation's changed flags to each of the nation's players.
			OutputEvents.BroadcastNationFlagsEvent(userData.nationID, nationData.flags);
		}

		// Encode event letting client know whether this nation name is available.
		Constants.EncodeString(_output_buffer, "nation_name_available");
		Constants.EncodeUnsignedNumber(_output_buffer, success ? 1 : 0, 1);
		Constants.EncodeUnsignedNumber(_output_buffer, (success && !_check) ? 1 : 0, 1);
	}

	public static void SetNationColor(StringBuffer _output_buffer, ClientThread _clientThread, int _r, int _g, int _b)
	{
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _clientThread.GetUserID(), false);
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, userData.nationID, false);

		// Set the nation's color.
		nationData.r = _r;
		nationData.g = _g;
		nationData.b = _b;

		// Mark the nation's data to be updated.
		DataManager.MarkForUpdate(nationData);

		// Send full map event to client with updated information.
		Display.GetFullMapEvent(_output_buffer, userData, false);
	}

	public static void CreatePassword(StringBuffer _output_buffer, ClientThread _clientThread, String _email, String _question, String _answer)
	{
		ClientString message = ClientString.Get();
		boolean success = true;

    // Lower case the security answer
    _answer = _answer.toLowerCase();

    // Check that a security answer of a sufficient length is given
		if ((_answer.length() < Constants.MIN_ANSWER_LEN) || (_answer.length() > Constants.MAX_ANSWER_LEN))
		{
			message.SetString("svr_create_account_answer_length", "min_len", String.valueOf(Constants.MIN_ANSWER_LEN), "max_len", String.valueOf(Constants.MAX_ANSWER_LEN)); //  "Answer to security question must be between " + Constants.MIN_ANSWER_LEN + " and " + Constants.MAX_ANSWER_LEN + " characters long."
			success = false;
		}

		// Check that the e-mail address is of the correct format
		if (_email.matches("^[a-zA-Z0-9_.-]+@[a-zA-Z0-9_-]+\\.[a-zA-Z0-9._-]+") == false)
		{
			message.SetString("svr_create_account_email_invalid"); // The given e-mail address is not valid, please re-enter it.
			success = false;
		}

    // Check that a security question has been chosen
		if (_question.length() == 0)
		{
			message.SetString("svr_create_account_choose_question"); // Please choose a security question.
			success = false;
		}

		if (success)
		{
			// Get the client's existing PlayerAccountData.
			PlayerAccountData accountData = _clientThread.player_account;

			// Generate a random password
			String password = GeneratePassword();//org.apache.commons.codec.digest.DigestUtils.sha256Hex("" + (Math.random() * 1000000000)).substring(0, 8); // Generate random password

			// Fill in fields in player account data.
			accountData.passhash = AccountDB.DeterminePasswordHash(password);
			accountData.email = _email;
			accountData.security_question = _question;
			accountData.security_answer = _answer;

			// Store the player account data.
			AccountDB.WritePlayerAccount(accountData);

			// Get the user data
			UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _clientThread.GetUserID(), false);

			// Copy the player account's username and email into the user data
			userData.name = accountData.username;
			userData.email = accountData.email;

			// Mark the user's data to be updated
			DataManager.MarkForUpdate(userData);

			// Send account info e-mail to player
			SendAccountInfoEmail(userData.name, password, _email);

			// Log the creation of this player account
			Constants.WriteToLog("log_players.txt", Constants.GetFullDate() + " playerID: " + accountData.ID + ", userID: " + _clientThread.GetUserID() + ", IP: " + _clientThread.GetClientIP() + ", username: " + userData.name + ", e-mail: " + _email + ", question: " + _question + ", answer: " + _answer + "\n");

			// Log the new user's e-mail address
			Constants.WriteToLog("log_email.txt", _email + ", ");

			// Send account info event to client with updated information.
			OutputEvents.GetAccountInfoEvent(_output_buffer, _clientThread.GetUserID(), _clientThread);
		}

		// Encode event letting client know whether this player creation succeeded.
		Constants.EncodeString(_output_buffer, "create_password_result");
		Constants.EncodeUnsignedNumber(_output_buffer, success ? 1 : 0, 1);
		message.Encode(_output_buffer);
		ClientString.Release(message);
	}

	public static void SendAccountInfoEmail(String _username, String _password, String _email)
	{
		// Send an email to the user specifying the new account's password
		String body_string = "Welcome to War of Conquest! Here is the username and password for your player account.\n\n" +
			"username: " + _username + "\n" +
			"password: " + _password + "\n\n" +
			"If you'd like, you can change your password here:\n" +
			"https://warofconquest.com/change-password/\n\n" +
			"If you forget your password, you can reset it here:\n" +
			"https://warofconquest.com/reset-password/\n\n" +
			"You can also change the e-mail address associated with your account, here:\n" +
			"https://warofconquest.com/change-email-address/\n\n" +
			"Please save this information for future reference. Have fun, and good luck in your conquests!\n" +
			"\n";
		Constants.SendEmail("automated-message@warofconquest.com", "War of Conquest", _email, "Your 'War of Conquest' account information", body_string);
	}

	public static void SendUserToHomeNation(StringBuffer _output_buffer, int _userID, int _targetUserID)
	{
		// Get the data for the requesting user and the target user.
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);
		UserData targetUserData = (UserData)DataManager.GetData(Constants.DT_USER, _targetUserID, false);

		// If either user's data doesn't exist, do nothing.
		if ((userData == null) || (targetUserData == null)) {
			return;
		}

		// If the given user and target user are not in the same nation, do nothing.
		if (userData.nationID != targetUserData.nationID) {
			return;
		}

		// If the given user does not have a superior rank to the target user, and is not the same as the target user, do nothing.
		if ((_userID != _targetUserID) && (userData.rank >= targetUserData.rank)) {
			return;
		}

		// If the target user is already in their home nation, do nothing.
		if (targetUserData.nationID == targetUserData.home_nationID) {
			return;
		}

		// Send report to the nation
		if (_userID == _targetUserID) {
	    Comm.SendReport(userData.nationID, ClientString.Get("svr_report_user_left", "username", userData.name), 0); // userData.name + " has left our nation."
		} else {
			Comm.SendReport(userData.nationID, ClientString.Get("svr_report_user_removed", "username", userData.name, "removed_username", targetUserData.name), 0); // userData.name + " has removed " + targetUserData.name + " from our nation."
		}

		// Have the target user join their home nation.
		JoinNation(_targetUserID, targetUserData.home_nationID);
	}

	public static void JoinNationRequest(StringBuffer _output_buffer, int _userID, String _nation_name, String _password, ClientThread _clientThread)
	{
		// If too many incorrect passwords have been entered recently, don't allow this attempt.
		int time_until_allowed = _clientThread.GetTimeBeforePasswordAllowed();
		if (time_until_allowed > 0)
		{
			// Return error message
			int mins_until_allowed = (time_until_allowed + 59) / 60;
			OutputEvents.GetJoinNationResponseEvent(_output_buffer, false, ClientString.Get("svr_join_nation_try_later", "num_mins", String.valueOf(mins_until_allowed), "minute_quant", ((mins_until_allowed != 1) ? "Server Strings/minute_quant_plural" : "Server Strings/minute_quant_singular"))); // "Please try again in " + mins_until_allowed + " minute" + ((mins_until_allowed > 1) ? "s" : "") + "."
			return;
		}

		// Check that a nation name is given
		if (_nation_name.equals(""))
		{
			// Create message
			OutputEvents.GetJoinNationResponseEvent(_output_buffer, false, ClientString.Get("svr_join_nation_enter_name"));
			return;
		}

		// Find out if there is an existing nation ID mapped to the given name.
		int nationID = DataManager.GetNameToIDMap(Constants.DT_NATION, _nation_name);

		// If nation with given name doesn't exist, return message.
		if (nationID == -1)
		{
			// Create message
			OutputEvents.GetJoinNationResponseEvent(_output_buffer, false, ClientString.Get("svr_join_nation_no_such_nation", "nation_name", _nation_name)); // "No nation named " + _nation_name + " exists."
			return;
		}

		// Get the nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

		// If data for nation with given name doesn't exist, return message.
		if (nationData == null)
		{
			// Create message
			OutputEvents.GetJoinNationResponseEvent(_output_buffer, false, ClientString.Get("svr_join_nation_not_found", "nation_name", _nation_name)); // "No nation was found named " + _nation_name + "."
			return;
		}

		// If the nation data has a password different from that given, report error.
		if ((!nationData.password.equals("")) && (_password.compareTo(nationData.password) != 0))
		{
			// Record that a bad password has been entered from this IP address
			_clientThread.BadPasswordEntered();

			if (_clientThread.GetTimeBeforePasswordAllowed() > 0)
			{
				// Return error message
				OutputEvents.GetJoinNationResponseEvent(_output_buffer, false, ClientString.Get("svr_join_nation_too_many_incorrect_passwords")); // "Too many incorrect passwords, try again in 5 minutes."
			}
			else
			{
				// Return error message
				OutputEvents.GetJoinNationResponseEvent(_output_buffer, false, ClientString.Get("svr_join_nation_incorrect_password")); // "Password is incorrect, try again."
			}

			return;
		}

		if (nationData.users.size() >= Constants.max_nation_members)
		{
			// Return error message
			OutputEvents.GetJoinNationResponseEvent(_output_buffer, false, ClientString.Get("svr_join_nation_max_members", "nation_name", nationData.name, "max_nation_members", String.valueOf(Constants.max_nation_members))); // "{nation_name} already has the limit of {max_nation_members} member(s)."
			return;
		}

		// Get the user data
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		// If the user is already a member of this nation, return message.
		if (nationID == userData.nationID)
		{
			// Create message
			OutputEvents.GetJoinNationResponseEvent(_output_buffer, false, ClientString.Get("svr_join_nation_already_member")); // "You are already a member of this nation."
			return;
		}

		// Have the given user join the given nation.
		JoinNation(_userID, nationID);
	}

	public static void JoinNation(int _userID, int _nationID)
	{
		Output.PrintToScreen("User " + _userID + " about to join nation " + _nationID);

		// Get the given user's data
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		if (userData == null) {
			return;
		}

		// If the given user is already a member of the nation to be joined, do nothing.
		if (userData.nationID == _nationID) {
			return;
		}

		// Get the nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationID, false);

		if (nationData == null) {
			return;
		}

		// Record mutual contacts between the user and the existing users of the nation they are joining.
		Comm.RecordContactWithNation(userData, nationData, Comm.CONTACT_VALUE_JOIN_NATION, true);

		// Get the user's old nation's data
		NationData oldNationData = (NationData)DataManager.GetData(Constants.DT_NATION, userData.nationID, false);

		// Remove the user's ID from their old nation's list of users
		for (int i = 0; i < 5; i++) oldNationData.users.remove(Integer.valueOf(_userID));

		// Get the user's client thread, if they are logged in.
		ClientThread clientThread = WOCServer.GetClientThread(_userID, userData.nationID);

		// If the user's client is logged in to the game, have it exit the game.
		if (clientThread  != null)
		{
			Login.ExitGame(clientThread, true);
		}
		else
		{
			// Broadcast a members event to the old nation.
			OutputEvents.BroadcastMembersEvent(userData.nationID);
		}

    // Set the user's nationID
		userData.nationID = _nationID;

		// Add the new user's ID to the nation's list of users
		nationData.users.add(_userID);

		// Set the user's rank to sovereign if this is their home nation, or general if the nation is empty, otherwise civilian.
		if (userData.home_nationID == _nationID) {
			userData.rank = Constants.RANK_SOVEREIGN; // This is the username's home nation; set rank to sovereign.
		//} else if (nationData.users.size() == 1) {
		//	userData.rank = Constants.RANK_GENERAL; // They are now the only member of this previously empty nation; set rank to general. NOTE: This would make it easy to skirt around the 5 account per month rule by swapping each account between many nations.
		}	else {
			userData.rank = Constants.RANK_CIVILIAN; // There are other members in this nation. Set rank to civilian.
		}

		// If the nation being joined is a veteran nation that has rebirthed, make sure the user's veteran status is set to true.
		if (nationData.veteran && (nationData.rebirth_count > 0)) {
			userData.veteran = true;
			Output.PrintToScreen("User '" + userData.name + "' (" + userData.ID + ") made vet due to joining vet nation.");
		}

		// Center the user's stored view on the nation. (Don't update their current view; it will be set from the stored view when they enter the game.)
		Display.CenterStoredViewOnNation(_userID, _nationID);

		// Initialize the user's login report data, for their new nation.
		Login.InitLoginReportData(userData, nationData);

		// Mark the user's data to be updated
		DataManager.MarkForUpdate(userData);

		// Mark the old and new nations' data to be updated
		DataManager.MarkForUpdate(nationData);
		DataManager.MarkForUpdate(oldNationData);

		// Log this joining
		Constants.WriteToNationLog(oldNationData, userData, "Left");
		Constants.WriteToNationLog(nationData, userData, "Joined");
    //Constants.WriteToLog("log_MFU.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " "  + nationData.ID + " evt: ApplyJoinNation_Submit\n");

    // Send report to the nation
    Comm.SendReport(_nationID, ClientString.Get("svr_join_nation", "username", userData.name, "nation_name", nationData.name), 0);

		// If the user's client was logged in to the game, have it re-enter the game, as a member of the newly joined nation.
		if (clientThread != null)
		{
			// Clear the event_buffer in preparation for compiling an event string.
			event_buffer.setLength(0);

			// Have the client belonging to the user who has changed nation, enter the game in the new nation.
			Login.EnterGame(event_buffer, clientThread, _userID, false);

			// Terminate event string and send to client belonging to the user who has changed nation.
			clientThread.TerminateAndSendNow(event_buffer);
		}
		else
		{
			// Broadcast a members event to the newly joined nation.
			OutputEvents.BroadcastMembersEvent(_nationID);
		}

		// Broadcast message to the nation joined.
		OutputEvents.BroadcastMessageEvent(_nationID, ClientString.Get("svr_join_nation", "username", userData.name, "nation_name", nationData.name)); // userData.name + " has joined " + nationData.name + "!"
	}


	public static String GeneratePassword()
	{
		String password = "";
		password += consonants[Constants.random.nextInt(19)];
		password += vowels[Constants.random.nextInt(4)];
		password += consonants[Constants.random.nextInt(19)];
		password += vowels[Constants.random.nextInt(4)];
		password += consonants[Constants.random.nextInt(19)];
		password += "-"; // To avoid confusion between letters and numbers
		password += Constants.random.nextInt(9);
		password += Constants.random.nextInt(9);

		return password;
	}

	public static void ChangeRank(StringBuffer _output_buffer, int _userID, int _memberID, int _new_rank)
	{
		// Get the user's data
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		// Get the member's data
		UserData memberData = (UserData)DataManager.GetData(Constants.DT_USER, _memberID, false);

		// Determine ID of user's nation
		int nationID = userData.nationID;

		// Get the user and member ranks
		int userRank = userData.rank;
		int memberRank = memberData.rank;

		if (nationID != memberData.nationID)
		{
			// Return message
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("Trying to change the rank of another nation's player? Hmm."));
			return;
		}

		if ((userRank >= memberRank) || (userRank > Constants.RANK_CAPTAIN))
		{
			// Return message
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_change_rank_rank_too_low")); // "Your rank is not high enough to make this change."
			return;
		}

		// Change the member's rank to that given
		memberData.rank = _new_rank;

		// Broadcast the updated members list to all online players belonging to this nation.
		OutputEvents.BroadcastMembersEvent(nationID);

		// Record report
		Comm.SendReport(userData.nationID, ClientString.Get("svr_report_rank_changed", "member_name", memberData.name, "username", userData.name, "new_rank", "{rank_" + Constants.GetRankString(_new_rank) + "}"), 0); // memberData.name + "'s rank changed to " + Constants.GetRankString(_new_rank) + " by " + userData.name + "."

		// Mark the member's data to be updated
		DataManager.MarkForUpdate(memberData);
	}

	public static int DetermineRecentAccountDelay(DeviceData _device_data, int _logInUserID)
	{
		int userID = -1;
		UserData userData = null;

		if (_device_data == null) {
			return 0;
		}

		// Get the data for the user attempting to log in.
		userData = (UserData)DataManager.GetData(Constants.DT_USER, _logInUserID, false);

		// If attempting to log in as an admin account, do not impose delay.
		if ((userData != null) && (userData.admin)) {
			return 0;
		}

		// Determine the user associated with this device that has logged off most recently.
		int most_recent_logout_time = 0;
		userData = null;
		for (int i = 0; i < _device_data.users.size(); i++)
		{
			int cur_userID = _device_data.users.get(i);
			UserData cur_user_data = (UserData)DataManager.GetData(Constants.DT_USER, cur_userID, false);

			if ((cur_user_data != null) && (cur_user_data.prev_logout_time > most_recent_logout_time))
			{
				userID = cur_userID;
				userData = cur_user_data;
				most_recent_logout_time = cur_user_data.prev_logout_time;
			}
		}

		if (userData == null) {
			return 0; // No user has logged in with this device; allow any to log in.
		}

		// If the user attempting to log in IS this device's most recently logged off user, allow login immediately.
		if (userData.ID == _logInUserID) {
			return 0;
		}

		//Output.PrintToScreen("_logInUserID: " + _logInUserID);

		ArrayList<UserLogoutRecord> logout_records = new ArrayList<UserLogoutRecord>();

		// Determine if the (most recently logged off) user account has been recently active within the max_accounts_period.
		if ((Constants.GetTime() - userData.prev_logout_time) < Constants.max_accounts_period)
		{
			// Keep a list of records of users that have recently logged out during the limit period.
			logout_records.add(new UserLogoutRecord(userID, userData.prev_logout_time));
			//Output.PrintToScreen("Recently active account " + userData.ID + ": " + userData.name);
		}
		else
		{
			// The most recently logged off user has not been active within the max_accounts_period. So, allow log in with any account.
			return 0;
		}

		// Iterate through each of the most recently logged out account's associated accounts...
		for (int i = 0; i < userData.associated_users.size(); i++)
		{
			// Get the data for the current associated account.
			int assoc_userID = userData.associated_users.get(i);
			UserData assoc_user_data = (UserData)DataManager.GetData(Constants.DT_USER, assoc_userID, false);

			if (assoc_user_data == null) {
				continue;
			}

			// Determine if the current associated account has been recently active.
			if ((Constants.GetTime() - assoc_user_data.prev_logout_time) < Constants.max_accounts_period)
			{
				// Keep a list of records of users that have recently logged out during the limit period.
				logout_records.add(new UserLogoutRecord(assoc_userID, assoc_user_data.prev_logout_time));
				//Output.PrintToScreen("Recently active account " + assoc_user_data.ID + ": " + assoc_user_data.name);
			}
		}

		if (logout_records.size() <= Constants.max_accounts_per_period) {
			return 0; // Fewer than, or exactly, the max number of accounts were used in the limit period; allow login with any account immediately.
		}

		// Sort the list of UserLogoutRecords, from earliest to most recent.
		Collections.sort(logout_records);

		// At least one more than the limit number of accounts have been used within the limit period.
		// If the user attempting to log in was one of the max_accounts_per_period latest accounts used, then allow it to log in again immediately.
		for (int i = logout_records.size() - Constants.max_accounts_per_period; i < logout_records.size(); i++)
		{
			if (logout_records.get(i).userID == _logInUserID) {
				return 0;
			}
		}

		// This user cannot log in immediately. Return time until the last logged in account before the limit period started will no longer be recently active.
		return logout_records.get(logout_records.size() - Constants.max_accounts_per_period - 1).prev_logout_time + Constants.max_accounts_period - Constants.GetTime();
	}

	private static class UserLogoutRecord implements Comparable<UserLogoutRecord>
	{
		int userID, prev_logout_time;

		public UserLogoutRecord(int _userID, int _prev_logout_time)
		{
			userID = _userID;
			prev_logout_time = _prev_logout_time;
		}

		public int compareTo(UserLogoutRecord _compare_record) {
			return (prev_logout_time > _compare_record.prev_logout_time) ? 1 : ((prev_logout_time == _compare_record.prev_logout_time) ? 0 : -1);
		}
	}
}
