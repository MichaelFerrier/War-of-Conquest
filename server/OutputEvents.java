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
import WOCServer.*;
import java.util.*;

public class OutputEvents
{
	static final int EVENT_BUFFER_LENGTH = 24576;//10000
	static StringBuffer event_buffer = new StringBuffer(EVENT_BUFFER_LENGTH);
	static StringBuffer event_buffer_2 = new StringBuffer(EVENT_BUFFER_LENGTH);
	static final int ENTRY_BUFFER_LENGTH = 200;
	static StringBuffer entry_buffer = new StringBuffer(ENTRY_BUFFER_LENGTH);

	static StringBuffer broadcast_buffer = new StringBuffer(Constants.OUTPUT_BUFFER_LENGTH);
	static Semaphore broadcast_buffer_semaphore = new Semaphore();

	static HashMap<Integer,Integer> nation_ranks_map = new HashMap<Integer,Integer>();

  public static void GetAdminNationInfoEvent(StringBuffer _output_buffer, NationData nationData)
	{
		// Encode event ID
	  Constants.EncodeString(_output_buffer, "event_admin_nation_info");

		// Encode the nation's info
		Constants.EncodeString(_output_buffer, nationData.name);
		Constants.EncodeUnsignedNumber(_output_buffer, nationData.level, 2);
    Constants.EncodeString(_output_buffer, nationData.password);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.game_money), 4);
    Constants.EncodeUnsignedNumber(_output_buffer, nationData.creation_time, 6);
    Constants.EncodeUnsignedNumber(_output_buffer, nationData.prev_use_time, 6);
	}
/*
	public static void GetMessageEvent(StringBuffer _output_buffer, String _message)
	{
		// Do nothing if there is no message.
		if (_message.equals("")) return;

		GetMessageEvent(_output_buffer, ClientString.Get(_message));
	}
*/
	public static void GetMessageEvent(StringBuffer _output_buffer, ClientString _message)
	{
		// Encode event ID
		Constants.EncodeString(_output_buffer, "event_message");

		// Encode message string
		_message.Encode(_output_buffer);

		// Release the given _message ClientString
		ClientString.Release(_message);
	}

	public static void GetChatLogEvent(StringBuffer _output_buffer, ClientString _text)
	{
		// Encode event ID
		Constants.EncodeString(_output_buffer, "event_chat_log");

		// Encode message string
		_text.Encode(_output_buffer);

		// Release the given _text ClientString
		ClientString.Release(_text);
	}

	public static void GetRequestorEvent(StringBuffer _output_buffer, ClientString _message)
	{
		// Encode event ID
		Constants.EncodeString(_output_buffer, "event_requestor");

		// Encode message string
		_message.Encode(_output_buffer);

		// Release the given _message ClientString
		ClientString.Release(_message);
	}

	public static void GetRequestorDurationEvent(StringBuffer _output_buffer, ClientString _message, int _duration)
	{
		// Encode event ID
		Constants.EncodeString(_output_buffer, "event_requestor_duration");

		// Encode message string
		_message.Encode(_output_buffer);

		// Encode duration
		Constants.EncodeUnsignedNumber(_output_buffer, _duration, 6);

		// Release the given _message ClientString
		ClientString.Release(_message);
	}

	public static void GetAnnouncementEvent(StringBuffer _output_buffer, ClientString _announcement)
	{
		// Encode event ID
		Constants.EncodeString(_output_buffer, "announcement");

		// Encode announcement string
		_announcement.Encode(_output_buffer);
		ClientString.Release(_announcement);
	}

	public static void GetSuspendEvent(StringBuffer _output_buffer, ClientString _message, boolean _due_to_inactivity)
	{
		// Encode event ID
		Constants.EncodeString(_output_buffer, "suspend");

		// Encode success value
		Constants.EncodeUnsignedNumber(_output_buffer, _due_to_inactivity ? 1 : 0, 1);

		// Encode message string
		_message.Encode(_output_buffer);
		ClientString.Release(_message);
	}

	public static void GetNoAssociatedPlayerEvent(StringBuffer _output_buffer)
	{
		// Encode event ID
		Constants.EncodeString(_output_buffer, "no_associated_player");
	}

	public static void GetStopAutoProcessEvent(StringBuffer _output_buffer)
	{
		// Encode event ID
		Constants.EncodeString(_output_buffer, "stop_auto_process");
	}

	public static void GetJoinNationResponseEvent(StringBuffer _output_buffer, boolean _success, ClientString _message)
	{
		// Encode event ID
		Constants.EncodeString(_output_buffer, "event_join_nation_result");

		// Encode success value
		Constants.EncodeUnsignedNumber(_output_buffer, _success ? 1 : 0, 1);

		// Encode message string
		_message.Encode(_output_buffer);
		ClientString.Release(_message);
	}

  public static void GetSecurityQuestionEvent(StringBuffer _output_buffer, int _userID)
	{
		/*
		// Get the user's data
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		// Encode event ID
		Constants.EncodeString(_output_buffer, "event_security_question");

    // Encode event data
    Constants.EncodeString(_output_buffer, userData.name);
    Constants.EncodeNumber(_output_buffer, userData.security_question, 1);
		*/
	}

	public static void GetConnectionEvent(StringBuffer _output_buffer, ClientThread _clientThread)
	{
	  // Encode event ID
	  Constants.EncodeString(_output_buffer, "connection_info");

	  // Encode serverID
	  Constants.EncodeNumber(_output_buffer, Constants.GetServerID(), 1);

	  // Encode map modified times
		Constants.EncodeUnsignedNumber(_output_buffer, GlobalData.instance.map_modified_times.size(), 2);
		for (Map.Entry<Integer, Integer> entry : GlobalData.instance.map_modified_times.entrySet())
		{
			Constants.EncodeUnsignedNumber(_output_buffer, entry.getKey(), 2);
			Constants.EncodeUnsignedNumber(_output_buffer, entry.getValue(), 6);
		}
	}

	public static void GetGameStartUpEvent(StringBuffer _output_buffer, int _userID, ClientThread _clientThread)
	{
		// Get the user's data
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		// Get the user's nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, userData.nationID, false);

	  // Get the info event string
	  GetInfoEvent(_output_buffer, _userID, _clientThread);

		// Get events for all of the chat lists that this nation is on, including its own.
		GetAllChatListsEvent(_output_buffer, userData.nationID);

		// Get list of the nation's members (must be sent here so it will be received after info event).
		GetMembersEvent(_output_buffer, userData.nationID);

		// Add the alliances event string
		GetAlliancesEvent(_output_buffer, userData.nationID);

		// Get the stats event
		GetStatsEvent(_output_buffer, userData.nationID, 0);

		// Get the technologies event
		GetTechnologiesEvent(_output_buffer, userData.nationID);

		// Get the tech prices event
		GetTechPricesEvent(_output_buffer);

		// Get the messages event string
		GetAllMessagesEvent(_output_buffer, _userID);

		// Get the followers event string
		GetAllFollowersEvent(_output_buffer, _userID);

		// Get the patron offers event string
		GetAllPatronOffersEvent(_output_buffer, _userID);

		// Get the patron info event
		GetPatronInfoEvent(_output_buffer, _userID);

		// Get nation orb winnings event string
		GetNationOrbsEvent(_output_buffer, userData.nationID);

		// Get the map flags event string
		GetMapFlagsEvent(_output_buffer, userData.nationID);

		// Get the list of all objects occupied by this nation.
		GetAllObjectsEvent(_output_buffer, nationData);

		// Get the list of this nation's build counts.
		GetAllBuildCountsEvent(_output_buffer, nationData);

		// Get the tournament related events strings
		GetNationTournamentStatusEvent(_output_buffer, nationData, 0);
		GetGlobalTournamentStatusEvent(_output_buffer);

		// Get the raid logs event
		GetRaidLogsEvent(_output_buffer, nationData);

		// Get the raid status event.
		GetRaidStatusEvent(_output_buffer, nationData, 0);

		if (_clientThread.GetClientVersion() >= 50) { // TEMP
			// Get the user's subscription information
			GetSubscriptionEvent(_output_buffer, userData);
		}

		// Get the update event string
		GetUpdateEvent(_output_buffer, userData.nationID);

		// If the user is currently banned from general chat, send event to that effect.
		if (userData.chat_ban_end_time > Constants.GetTime()) {
			GetChatBanEvent(_output_buffer, _userID);
		}
	}

	public static void GetInfoEvent(StringBuffer _output_buffer, int _userID, ClientThread _clientThread)
	{
		int i;

	  // Get the user's data
	  UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

	  // Get the user's nation's data
	  int nationID = userData.nationID;
	  NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

	  // Get the user's home nation's data
	  NationData homeNationData = (NationData)DataManager.GetData(Constants.DT_NATION, userData.home_nationID, false);

		// TESTING
		if (homeNationData == null) {
			Output.PrintToScreen("ERROR: GetInfoEvent() user " + userData.name + " (" + _userID + ") has homeNationID: " + userData.home_nationID + ", NOT FOUND.");
		}

		// Determine the nation's map position limit, based on its level.
		LandMap land_map = DataManager.GetLandMap(Constants.MAINLAND_MAP_ID, false);
		int map_position_limit = land_map.MaxLevelLimitToPosX(nationData.level) - 1;
		int map_position_limit_next_level = land_map.MaxLevelLimitToPosX(nationData.level + 1) - 1;
		int map_position_eastern_limit = land_map.MaxLevelLimitToPosX(land_map.GetEasternLevelLimit(nationData.level));

	  // Encode event ID
	  Constants.EncodeString(_output_buffer, "event_info");

		// Encode player account info
	  Constants.EncodeString(_output_buffer, _clientThread.player_account.info);

	  // Encode info flags
	  Constants.EncodeUnsignedNumber(_output_buffer, CompileInfoFlags(userData, _clientThread), 2);

	  // Encode userID
	  Constants.EncodeNumber(_output_buffer, _userID, 4);

		// Encode username
	  Constants.EncodeString(_output_buffer, userData.name);

		// Encode email address
	  Constants.EncodeString(_output_buffer, userData.email);

	  // Encode user account creation time
	  Constants.EncodeUnsignedNumber(_output_buffer, userData.creation_time, 6);

	  // Encode nationID
	  Constants.EncodeNumber(_output_buffer, nationID, 4);

		// Encode user flags
	  Constants.EncodeUnsignedNumber(_output_buffer, userData.flags, 4);

		// Encode user rank
		Constants.EncodeUnsignedNumber(_output_buffer, userData.rank, 1);

		// Encode user's previous logout time
		Constants.EncodeUnsignedNumber(_output_buffer, userData.prev_logout_time, 6);

		// Encode the user's patron's name
		if (userData.patronID == -1) {
			Constants.EncodeString(_output_buffer, "");
		} else {
			UserData patronUserData = (UserData)DataManager.GetData(Constants.DT_USER, userData.patronID, false);
			Constants.EncodeString(_output_buffer, patronUserData.name);
		}

		// Encode the user's available ad bonus amount.
		Constants.EncodeUnsignedNumber(_output_buffer, userData.ad_bonus_available, 4);

		// Encode tutorial state
		Constants.EncodeString(_output_buffer, userData.tutorial_state);

	  // Encode user's home nationID
	  Constants.EncodeNumber(_output_buffer, userData.home_nationID, 4);

		// Encode user's home nation's name
	  Constants.EncodeString(_output_buffer, (homeNationData == null) ? "" : homeNationData.name);

		// Encode nation flags
	  Constants.EncodeUnsignedNumber(_output_buffer, nationData.flags, 4);

	  // Encode nation name
	  Constants.EncodeString(_output_buffer, nationData.name);

	  // Encode nation join password
	  Constants.EncodeString(_output_buffer, nationData.password);

		// Encode information about the nation's appearance
		Constants.EncodeUnsignedNumber(_output_buffer, nationData.r, 2);
		Constants.EncodeUnsignedNumber(_output_buffer, nationData.g, 2);
		Constants.EncodeUnsignedNumber(_output_buffer, nationData.b, 2);
		Constants.EncodeNumber(_output_buffer, nationData.emblem_index, 2);
		Constants.EncodeUnsignedNumber(_output_buffer, nationData.emblem_color, 1);

		// Encode information about the nation
		Constants.EncodeUnsignedNumber(_output_buffer, nationData.level, 2);
		Constants.EncodeUnsignedNumber(_output_buffer, Math.min(Constants.MaxEncodableUnsignedNumber(5), (int)nationData.xp), 5);
		Constants.EncodeUnsignedNumber(_output_buffer, Math.min(Constants.MaxEncodableUnsignedNumber(5), Constants.XP_PER_LEVEL[nationData.level - nationData.GetRebirthLevelBonus()]), 5);
		Constants.EncodeUnsignedNumber(_output_buffer, Math.min(Constants.MaxEncodableUnsignedNumber(5), Constants.XP_PER_LEVEL[nationData.level + 1 - nationData.GetRebirthLevelBonus()]), 5);
		Constants.EncodeUnsignedNumber(_output_buffer, nationData.pending_xp, 5);
		Constants.EncodeUnsignedNumber(_output_buffer, nationData.rebirth_count, 2);
		Constants.EncodeUnsignedNumber(_output_buffer, nationData.GetRebirthLevelBonus(), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, Constants.REBIRTH_AVAILABLE_LEVEL + nationData.GetRebirthLevelBonus(), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, Math.max(0, nationData.birth_time + Constants.MIN_NATION_CYCLE_TIME - Constants.GetTime()), 5); // Seconds until rebirth is allowed.
		Constants.EncodeUnsignedNumber(_output_buffer, (int)nationData.rebirth_countdown, 3);
		Constants.EncodeUnsignedNumber(_output_buffer, nationData.rebirth_countdown_start, 3);
		Constants.EncodeUnsignedNumber(_output_buffer, nationData.advance_points, 2);
		Constants.EncodeNumber(_output_buffer, nationData.targetAdvanceID, 3);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.prize_money), 5);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.prize_money_history), 5);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.prize_money_history_monthly), 5);
		Constants.EncodeNumber(_output_buffer, map_position_limit, 3);
		Constants.EncodeNumber(_output_buffer, map_position_limit_next_level, 3);
		Constants.EncodeNumber(_output_buffer, map_position_eastern_limit, 3);

		// Encode list of muted user IDs
		Constants.EncodeUnsignedNumber(_output_buffer, userData.muted_users.size(), 3);
		for (i = 0; i < userData.muted_users.size(); i++) {
			Constants.EncodeUnsignedNumber(_output_buffer, userData.muted_users.get(i), 5);
		}

		// Encode list of muted device IDs
		Constants.EncodeUnsignedNumber(_output_buffer, userData.muted_devices.size(), 3);
		for (i = 0; i < userData.muted_devices.size(); i++) {
			int cur_muted_device_id = userData.muted_devices.get(i);

			// Sometimes a muted device ID can be negative! Why?
			if (cur_muted_device_id < 0) {
				Output.PrintToScreen("WARNING: GetInfoEvent() muted device ID is negative: " + cur_muted_device_id + " for user " + userData.name + " (" + _userID + ").");
				cur_muted_device_id = 0;
			}

			Constants.EncodeUnsignedNumber(_output_buffer, cur_muted_device_id, 5);
		}

		// Encode the nation's quest records
		Constants.EncodeUnsignedNumber(_output_buffer, nationData.quest_records.size(), 2);
		for (QuestRecord cur_record : nationData.quest_records.values())
		{
			Constants.EncodeUnsignedNumber(_output_buffer, cur_record.ID, 2);
			Constants.EncodeUnsignedNumber(_output_buffer, cur_record.cur_amount, 5);
			Constants.EncodeUnsignedNumber(_output_buffer, cur_record.completed, 1);
			Constants.EncodeUnsignedNumber(_output_buffer, cur_record.collected, 1);
		}

    // Encode current time
		Calendar woc_time = Calendar.getInstance();
    int time_in_secs = (int)(woc_time.getTimeInMillis() / 1000);
    Constants.EncodeUnsignedNumber(_output_buffer, time_in_secs, 6);

		// Encode number of second left in the current day
		Constants.EncodeUnsignedNumber(_output_buffer, Constants.GetSecondsLeftInAbsoluteDay(), 6);

		// Encode time until next free migration
		Constants.EncodeUnsignedNumber(_output_buffer, Math.max(0, nationData.prev_free_migration_time + Constants.FREE_MIGRATION_PERIOD - Constants.GetTime()), 6);

		// Encode time until next unite is allowed
		Constants.EncodeUnsignedNumber(_output_buffer, Math.max(0, nationData.prev_unite_time + Constants.UNITE_PERIOD - Constants.GetTime()), 6);

    //Encode the user's moderator level
    Constants.EncodeNumber(_output_buffer, userData.mod_level, 1);

		// Encode the user's fealty information
		EncodeFealtyInfo(_output_buffer, userData);

		// Encode the nation's historical extent on the mainland map.
		Footprint fp = nationData.GetFootprint(Constants.MAINLAND_MAP_ID);
		Constants.EncodeNumber(_output_buffer, fp.min_x0, 3);
		Constants.EncodeNumber(_output_buffer, fp.min_y0, 3);
		Constants.EncodeNumber(_output_buffer, fp.max_x1, 3);
		Constants.EncodeNumber(_output_buffer, fp.max_y1, 3);

		// Info about recent energy purchases.
		Constants.EncodeUnsignedNumber(_output_buffer, (Constants.GetAbsoluteDay() == nationData.prev_buy_energy_day) ? nationData.buy_energy_day_amount : 0, 6);

		// Nation quantities
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.energy), 5);
		//Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.manpower), 5);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.game_money), 4);
		Constants.EncodeUnsignedNumber(_output_buffer, nationData.GetTransferrableCredits(), 4);

		// User login report information
		Constants.EncodeNumber(_output_buffer, userData.report__defenses_squares_defeated, 5);
		Constants.EncodeNumber(_output_buffer, userData.report__defenses_XP, 5);
		Constants.EncodeNumber(_output_buffer, userData.report__defenses_lost, 5);
		Constants.EncodeNumber(_output_buffer, userData.report__defenses_built, 5);
		Constants.EncodeNumber(_output_buffer, userData.report__walls_lost, 5);
		Constants.EncodeNumber(_output_buffer, userData.report__walls_built, 5);
		Constants.EncodeNumber(_output_buffer, userData.report__attacks_squares_captured, 5);
		Constants.EncodeNumber(_output_buffer, userData.report__attacks_XP, 5);
		Constants.EncodeNumber(_output_buffer, userData.report__levels_gained, 5);
		Constants.EncodeNumber(_output_buffer, userData.report__orb_count_delta, 5);
		Constants.EncodeNumber(_output_buffer, Constants.LimitNumberForEncoding((int)userData.report__orb_credits, 5), 5);
		Constants.EncodeNumber(_output_buffer, userData.report__orb_XP, 5);
		Constants.EncodeNumber(_output_buffer, userData.report__farming_XP, 5);
		Constants.EncodeNumber(_output_buffer, userData.report__resource_count_delta, 5);
		Constants.EncodeNumber(_output_buffer, userData.report__land_lost, 5);
		Constants.EncodeNumber(_output_buffer, Constants.LimitNumberForEncoding((int)(userData.report__energy_begin * 100), 6), 6);
		Constants.EncodeNumber(_output_buffer, Constants.LimitNumberForEncoding((int)(userData.report__energy_spent * 100), 6), 6);
		Constants.EncodeNumber(_output_buffer, Constants.LimitNumberForEncoding((int)(userData.report__energy_donated * 100), 6), 6);
		Constants.EncodeNumber(_output_buffer, Constants.LimitNumberForEncoding((int)(userData.report__energy_lost_to_raids * 100), 6), 6);
		Constants.EncodeNumber(_output_buffer, Constants.LimitNumberForEncoding((int)(userData.report__manpower_begin * 100), 6), 6);
		Constants.EncodeNumber(_output_buffer, Constants.LimitNumberForEncoding((int)(userData.report__manpower_spent * 100), 6), 6);
		Constants.EncodeNumber(_output_buffer, Constants.LimitNumberForEncoding((int)(userData.report__manpower_lost_to_resources * 100), 6), 6);
		Constants.EncodeNumber(_output_buffer, Constants.LimitNumberForEncoding((int)(userData.report__manpower_donated * 100), 6), 6);
		Constants.EncodeNumber(_output_buffer, Constants.LimitNumberForEncoding((int)(userData.report__manpower_lost_to_raids * 100), 6), 6);
		Constants.EncodeNumber(_output_buffer, Constants.LimitNumberForEncoding((int)(userData.report__credits_begin * 100), 6), 6);
		Constants.EncodeNumber(_output_buffer, Constants.LimitNumberForEncoding((int)(userData.report__credits_spent * 100), 6), 6);
		Constants.EncodeNumber(_output_buffer, Constants.LimitNumberForEncoding((int)(userData.report__patron_XP * 100), 6), 6);
		Constants.EncodeNumber(_output_buffer, Constants.LimitNumberForEncoding((int)(userData.report__patron_credits * 100), 6), 6);
		Constants.EncodeNumber(_output_buffer, Constants.LimitNumberForEncoding((int)(userData.report__follower_XP * 100), 6), 6);
		Constants.EncodeNumber(_output_buffer, Constants.LimitNumberForEncoding((int)(userData.report__follower_credits * 100), 6), 6);
		Constants.EncodeNumber(_output_buffer, userData.report__follower_count, 3);
		Constants.EncodeNumber(_output_buffer, userData.report__raids_fought, 3);
		Constants.EncodeNumber(_output_buffer, userData.report__medals_delta, 3);
		Constants.EncodeNumber(_output_buffer, userData.report__rebirth, 2);
		Constants.EncodeNumber(_output_buffer, Constants.LimitNumberForUnsignedEncoding((int)(userData.report__home_defense_credits * 100), 6), 6);
		Constants.EncodeNumber(_output_buffer, Constants.LimitNumberForUnsignedEncoding((int)(userData.report__home_defense_xp * 100), 6), 6);
		Constants.EncodeNumber(_output_buffer, Constants.LimitNumberForUnsignedEncoding((int)(userData.report__home_defense_rebirth * 100), 6), 6);

		// Encode stat initial values
		Constants.EncodeUnsignedNumber(_output_buffer, (int)Constants.INIT_ENERGY, 4);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)Constants.INIT_ENERGY_MAX, 4);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)Constants.INIT_ENERGY_RATE, 4);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)Constants.INIT_MANPOWER, 4);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)Constants.INIT_MANPOWER_MAX, 4);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)Constants.INIT_MANPOWER_RATE, 4);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)Constants.INIT_MANPOWER_PER_ATTACK, 4);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)Constants.INIT_STAT_TECH, 4);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)Constants.INIT_STAT_BIO, 4);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)Constants.INIT_STAT_PSI, 4);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)Constants.INIT_HIT_POINT_BASE, 4);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)Constants.INIT_HIT_POINTS_RATE, 4);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(Constants.INIT_SALVAGE_VALUE * 100), 4);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)Constants.INIT_MAX_NUM_ALLIANCES, 4);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)Constants.INIT_MAX_SIMULTANEOUS_PROCESSES, 4);

		// Encode constants
		Constants.EncodeUnsignedNumber(_output_buffer, (int)Constants.TIME_UNTIL_CRUMBLE, 4);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)Constants.TIME_UNTIL_FAST_CRUMBLE, 4);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)Constants.TOWER_REBUILD_PERIOD, 4);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)BuildData.SECONDS_REMAIN_VISIBLE, 4);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)BuildData.COMPLETION_COST_PER_MINUTE, 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)Constants.EXTRA_VIEW_RANGE, 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)Constants.NATION_MAX_EXTENT, 3);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)Constants.UNITE_COST, 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)Constants.UNITE_PENDING_XP_PER_HOUR, 5);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)Constants.MIGRATION_COST, 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)Constants.CUSTOMIZE_COST, 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)Constants.REBIRTH_LEVEL_BONUS, 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)Constants.MAX_REBIRTH_LEVEL_BONUS, 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)Constants.REBIRTH_TO_BASE_LEVEL, 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)Constants.MAX_REBIRTH_COUNTDOWN_PURCHASED, 3);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)Constants.ALLY_LEVEL_DIFF_LIMIT, 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)Constants.SUPPORTABLE_AREA_BASE, 4);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)Constants.SUPPORTABLE_AREA_PER_LEVEL, 4);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(Constants.GEO_EFFICIENCY_MIN * 100), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(Constants.GEO_EFFICIENCY_MAX * 100), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(Constants.RESOURCE_BONUS_CAP * 100), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(Constants.MANPOWER_BURN_EXPONENT * 100), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(Constants.MANPOWER_BURN_FRACTION_OF_MANPOWER_MAX * 100), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(Constants.OVERBURN_POWER * 100), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)Constants.MAX_NUM_SHARE_BUILDS, 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(Constants.STORAGE_FILL_PERIOD / Constants.SECONDS_PER_HOUR), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)Constants.max_accounts_per_period, 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)Constants.max_accounts_period, 5);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(Constants.MANPOWER_MAX_HOMELAND_FRACTION * 100), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(Constants.MANPOWER_RATE_HOMELAND_FRACTION * 100), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(Constants.ENERGY_RATE_HOMELAND_FRACTION * 100), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(Constants.ENERGY_RATE_RAIDLAND_FRACTION * 100), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(Constants.SUPPORTABLE_AREA_HOMELAND_FRACTION * 100), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(Constants.SUPPORTABLE_AREA_RAIDLAND_FRACTION * 100), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(Raid.MIN_MANPOWER_FRACTION_TO_START_RAID * 100), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(Raid.MANPOWER_FRACTION_COST_TO_RESTART_RAID * 100), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)Raid.MEDALS_PER_LEAGUE, 2);
		Constants.EncodeNumber(_output_buffer, (int)World.new_player_area_boundary, 3);
		Constants.EncodeNumber(_output_buffer, Constants.max_buy_credits_per_month, 4);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(Constants.manpower_gen_multiplier * 100), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(Constants.INCOGNITO_ENERGY_BURN * 100), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, Constants.MIN_INCOGNITO_PERIOD, 3);
		Constants.EncodeUnsignedNumber(_output_buffer, Constants.MIN_WINNINGS_TO_CASH_OUT, 4);
		Constants.EncodeUnsignedNumber(_output_buffer, Constants.CREDITS_PER_CENT_TRADED_IN, 2);

		// Purchasing manpower and energy
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(Constants.BUY_MANPOWER_BASE * 100), 4);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(Constants.BUY_MANPOWER_MULT * 100), 4);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(Constants.BUY_MANPOWER_DAILY_LIMIT * 100), 4);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(Constants.BUY_MANPOWER_DAILY_ABSOLUTE_LIMIT * 100), 4);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(Constants.BUY_MANPOWER_LIMIT_BASE * 100), 4);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(Constants.BUY_ENERGY_BASE * 100), 4);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(Constants.BUY_ENERGY_MULT * 100), 4);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(Constants.BUY_ENERGY_DAILY_LIMIT * 100), 4);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(Constants.BUY_ENERGY_DAILY_ABSOLUTE_LIMIT * 100), 4);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(Constants.BUY_ENERGY_LIMIT_BASE * 100), 4);

		// Purchasing credits
		Constants.EncodeUnsignedNumber(_output_buffer, Constants.NUM_CREDIT_PACKAGES, 2);
		for (i = 0; i < Constants.NUM_CREDIT_PACKAGES; i++)
		{
			Constants.EncodeUnsignedNumber(_output_buffer, Constants.BUY_CREDITS_AMOUNT[i], 4);
			Constants.EncodeUnsignedNumber(_output_buffer, (int)(Constants.BUY_CREDITS_COST_USD[i] * 100f + 0.5f), 4);
		}

		if (_clientThread.GetClientVersion() >= 50) // TEMP
		{
			// Subscription tiers
			Constants.EncodeUnsignedNumber(_output_buffer, Subscription.SUBSCRIPTION_TIER_COUNT, 2);
			for (i = 0; i < Subscription.SUBSCRIPTION_TIER_COUNT; i++)
			{
				Constants.EncodeUnsignedNumber(_output_buffer, (int)(Subscription.subscription_cost_usd[i] * 100f + 0.5f), 4);
				Constants.EncodeUnsignedNumber(_output_buffer, Subscription.bonus_credits_per_day[i], 4);
				Constants.EncodeUnsignedNumber(_output_buffer, Subscription.bonus_rebirth_per_day[i], 4);
				Constants.EncodeUnsignedNumber(_output_buffer, Subscription.bonus_xp_percentage[i], 4);
				Constants.EncodeUnsignedNumber(_output_buffer, Subscription.bonus_manpower_percentage[i], 4);
			}
		}

		// Orb payout rates
		Constants.EncodeUnsignedNumber(_output_buffer, Constants.orb_payments_per_hour.size(), 3);
		for (Map.Entry<Integer,Float> entry : Constants.orb_payments_per_hour.entrySet())
		{
			Constants.EncodeUnsignedNumber(_output_buffer, entry.getKey(), 3); // Orb type ID
			Constants.EncodeUnsignedNumber(_output_buffer, (int)Math.round(entry.getValue() * 100 * 24), 3); // Payout in cents per day
		}
	}

	public static void GetAccountInfoEvent(StringBuffer _output_buffer, int _userID, ClientThread _clientThread)
	{
	  // Get the user's data
	  UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		if (userData == null) {
			Output.PrintToScreen("ERROR: GetAccountInfoEvent() called for userID " + _userID + ", which is missing data.");
			Output.PrintStackTrace();
			return;
		}

	  // Get the user's nation's data
	  int nationID = userData.nationID;
	  NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

		if (nationData == null) {
			Output.PrintToScreen("ERROR: GetAccountInfoEvent() called for userID " + _userID + " with nationID " + nationID + ", which is missing data.");
			Output.PrintStackTrace();
			return;
		}

		if (userData.name == null) {
			Output.PrintToScreen("ERROR: GetAccountInfoEvent() called for userID " + _userID + ", which has userData with name set to NULL.");
			Output.PrintStackTrace();
			return;
		}

	  // Encode event ID
	  Constants.EncodeString(_output_buffer, "event_account_info");

	  // Encode info flags
	  Constants.EncodeUnsignedNumber(_output_buffer, CompileInfoFlags(userData, _clientThread), 2);

		// Encide user flags
	  Constants.EncodeUnsignedNumber(_output_buffer, userData.flags, 4);

		// Encode username
	  Constants.EncodeString(_output_buffer, userData.name);

		// Encode email address
	  Constants.EncodeString(_output_buffer, userData.email);

	  // Encode nation name
	  Constants.EncodeString(_output_buffer, nationData.name);

    // Encode the user's moderator level
    Constants.EncodeNumber(_output_buffer, userData.mod_level, 1);
	}

	public static int CompileInfoFlags(UserData _userData, ClientThread _clientThread)
	{
		int info_flags = 0;
		if (_userData.admin == true) {info_flags |= Constants.IF_ADMIN;}
		if (_userData.login_count == 1) {info_flags |= Constants.IF_FIRST_LOGIN;}
		if (_userData.veteran == true) {info_flags |= Constants.IF_VETERAN_USER;}
		if (_clientThread.player_account.passhash.equals("") == false) {info_flags |= Constants.IF_REGISTERED;}
		if (_userData.ad_bonuses_allowed == true) {info_flags |= Constants.IF_AD_BONUSES_ALLOWED;}
		if (Constants.cash_out_prizes == true) {info_flags |= Constants.IF_CASH_OUT_PRIZES_ALLOWED;}
		if (Constants.allow_credit_purchases == true) {info_flags |= Constants.IF_CREDIT_PURCHASES_ALLOWED;}

		return info_flags;
	}

	public static void GetChatBanEvent(StringBuffer _output_buffer, int _userID)
	{
		// Get the user's data
	  UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		// Encode event ID
		Constants.EncodeString(_output_buffer, "chat_ban");

		// Encode the remaining duration of the chat ban.
		Constants.EncodeUnsignedNumber(_output_buffer, Math.max(0, userData.chat_ban_end_time - Constants.GetTime()), 5);
	}

	public static void GetAlliancesEvent(StringBuffer _output_buffer, int _nationID)
	{
		int cur_index, curID;
		NationData cur_nation_data;

		// Get the nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationID, false);

		// Encode event ID
		Constants.EncodeString(_output_buffer, "event_alliances");

		// Encode list of active alliances
		EncodeListOfNations(_output_buffer, nationData.alliances_active);

		// Encode list of incoming alliance requests
		EncodeListOfNations(_output_buffer, nationData.alliances_requests_incoming);

		// Encode list of outgoing alliance requests
		EncodeListOfNations(_output_buffer, nationData.alliances_requests_outgoing);

		// Encode list of incoming unite requests
		EncodeListOfUniteRequests(_output_buffer, nationData.unite_requests_incoming, nationData.unite_offers_incoming);

		// Encode list of outgoing unite requests
		EncodeListOfUniteRequests(_output_buffer, nationData.unite_requests_outgoing, nationData.unite_offers_outgoing);
	}

	public static void EncodeListOfNations(StringBuffer _output_buffer, ArrayList<Integer> _list)
	{
		int curID, cur_index;
		NationData cur_nation_data;
		int size = _list.size();

		// Encode number of nations in list
		Constants.EncodeUnsignedNumber(_output_buffer, size, 2);

		// Encode list of nations IDs and names.
		for (cur_index = 0; cur_index < size; cur_index++)
		{
			// Encode ID and name of the current nation in the list.
			curID = _list.get(cur_index);
			cur_nation_data = (NationData)DataManager.GetData(Constants.DT_NATION, curID, false);
			Constants.EncodeUnsignedNumber(_output_buffer, curID, 4);
			Constants.EncodeString(_output_buffer, cur_nation_data == null ? "" : cur_nation_data.name);
		}
	}

	public static void EncodeListOfUniteRequests(StringBuffer _output_buffer, ArrayList<Integer> _nationIDs, ArrayList<Integer> _payment_offers)
	{
		int curID, cur_index;
		NationData cur_nation_data;
		int size = _nationIDs.size();

		// Encode number of nations in list
		Constants.EncodeUnsignedNumber(_output_buffer, size, 2);

		// Encode list of nations IDs, names, and payment offers.
		for (cur_index = 0; cur_index < size; cur_index++)
		{
			// Encode ID and name of the current nation in the list.
			curID = _nationIDs.get(cur_index);
			cur_nation_data = (NationData)DataManager.GetData(Constants.DT_NATION, curID, false);
			Constants.EncodeUnsignedNumber(_output_buffer, curID, 4);
			Constants.EncodeString(_output_buffer, cur_nation_data == null ? "" : cur_nation_data.name);

			// Encode the corresponding payment offer.
			Constants.EncodeUnsignedNumber(_output_buffer, (_payment_offers.size() > cur_index) ? _payment_offers.get(cur_index) : 0, 6);
		}
	}

	public static void GetMembersEvent(StringBuffer _output_buffer, int _nationID)
	{
	  // Get the user's nation's data
	  NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationID, false);

    if (nationData == null) {
      Output.PrintToScreen("ERROR: GetMembersEvent(), nationData is NULL for ID " + _nationID);
      return;
    }

	  // Encode event ID
	  Constants.EncodeString(_output_buffer, "event_members");

		// Encode number of members
		Constants.EncodeUnsignedNumber(_output_buffer, nationData.users.size(), 2);

		// Encode list of members
		UserData cur_member_data;
		boolean logged_in, absentee;
		for (int cur_user_index = 0; cur_user_index < nationData.users.size(); cur_user_index++)
		{
			// Get the current member's data
			cur_member_data = (UserData)DataManager.GetData(Constants.DT_USER, nationData.users.get(cur_user_index), false);

			// If member data not found, remove member from nation and exit this loop.
			// This event will be broken, but the nation's data will be fixed for next time.
			if (cur_member_data == null)
			{
				// Remove the member's ID from the nation's list of users
				nationData.users.remove(cur_user_index);

				// Mark the nation's data to be updated
				DataManager.MarkForUpdate(nationData);

        //Constants.WriteToLog("log_MFU.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " "  + nationData.ID + " evt: GetMembersEvent\n");

				// Exit this loop
				break;
			}

			// Determine whether the user is logged in
			logged_in = WOCServer.IsUserLoggedIn(cur_member_data.ID, _nationID);

			// Determine whether the member is absentee
			absentee = ((Constants.GetTime() - cur_member_data.prev_login_time) > Constants.TIME_SINCE_LAST_LOGIN_ABSENTEE);

			//Output.PrintToScreen("Member index: " + cur_user_index + ", ID: " + nationData.users.get(cur_user_index) + ", data: " + cur_member_data + ", name: " + cur_member_data.name);

			// Encode the current member's information
			Constants.EncodeUnsignedNumber(_output_buffer, cur_member_data.ID, 4);
			Constants.EncodeString(_output_buffer, cur_member_data.name);
			Constants.EncodeUnsignedNumber(_output_buffer, Math.min(Constants.MaxEncodableUnsignedNumber(5), (int)cur_member_data.xp), 5);
			Constants.EncodeUnsignedNumber(_output_buffer, cur_member_data.rank, 1);
			Constants.EncodeUnsignedNumber(_output_buffer, (absentee == true) ? 1 : 0, 1);
			Constants.EncodeUnsignedNumber(_output_buffer, (logged_in == true) ? 1 : 0, 1);
		}
	}

	public static void GetFealtyInfoEvent(StringBuffer _output_buffer, int _userID)
	{
		// Get the user's data
	  UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		// Do not send output if ID is not valid
		if (userData == null) {
			return;
		}

	  // Encode event ID
	  Constants.EncodeString(_output_buffer, "event_fealty_info");

		// Encode the user's fealty information
		EncodeFealtyInfo(_output_buffer, userData);
	}

	public static void EncodeFealtyInfo(StringBuffer _output_buffer, UserData _userData)
	{
		// Encode the user's fealty information
		if (_userData.fealty_nationID != -1)
		{
			NationData fealty_nation_data = (NationData)DataManager.GetData(Constants.DT_NATION, _userData.fealty_nationID, false);
			Constants.EncodeUnsignedNumber(_output_buffer, Math.max(0, _userData.fealty_end_time - Constants.GetTime()), 6);
			Constants.EncodeString(_output_buffer, (fealty_nation_data == null) ? "" : fealty_nation_data.name);
		}
		else
		{
			Constants.EncodeUnsignedNumber(_output_buffer, 0, 6);
			Constants.EncodeString(_output_buffer, "");
		}

		// Encode the user's tournament fealty information
		if (_userData.fealty_tournament_nationID != -1)
		{
			NationData fealty_nation_data = (NationData)DataManager.GetData(Constants.DT_NATION, _userData.fealty_tournament_nationID, false);
			Constants.EncodeUnsignedNumber(_output_buffer, Math.max(0, _userData.fealty_tournament_end_time - Constants.GetTime()), 6);
			Constants.EncodeString(_output_buffer, (fealty_nation_data == null) ? "" : fealty_nation_data.name);
		}
		else
		{
			Constants.EncodeUnsignedNumber(_output_buffer, 0, 6);
			Constants.EncodeString(_output_buffer, "");
		}

		Constants.EncodeUnsignedNumber(_output_buffer, _userData.fealty_num_nations_at_tier, 2);
		Constants.EncodeUnsignedNumber(_output_buffer, _userData.fealty_num_nations_in_tournament, 2);
	}

	public static void GetStatsEvent(StringBuffer _output_buffer, int _nationID, int _delay)
	{
	  // Get the nation's data
	  NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationID, false);

    if (nationData == null) {
      Output.PrintToScreen("ERROR: GetStatsEvent(), nationData is NULL for ID " + _nationID);
      return;
    }

		// Sanity check
		if (nationData.mainland_footprint.manpower < 0) {
			Output.PrintToScreen("ERROR: GetStatsEvent(), Manpower of nation " + nationData.name + " (" + nationData.ID + " is negative: " + nationData.mainland_footprint.manpower);
			return;
		}

		// Determine whether a raid is in progress.
		boolean raid_in_progress = (nationData.raidID > 0);

	  // Encode event ID
	  Constants.EncodeString(_output_buffer, "event_stats");

		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.pending_xp), 5);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.energy), 5);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.energy_max), 5);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.manpower_max), 5);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.manpower_per_attack), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.geo_efficiency_modifier * 100), 3);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.hit_points_base), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.hit_points_rate), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.crit_chance * 100), 3);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.salvage_value * 100), 3);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.wall_discount * 100), 3);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.structure_discount * 100), 3);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.splash_damage * 100), 3);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.max_num_alliances), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.max_simultaneous_processes), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.shard_red_fill * 100), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.shard_green_fill * 100), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.shard_blue_fill * 100), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, nationData.num_share_builds, 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.shared_energy_fill * 100), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.shared_manpower_fill * 100), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, nationData.shared_energy_xp_per_hour, 3);
		Constants.EncodeUnsignedNumber(_output_buffer, nationData.shared_manpower_xp_per_hour, 3);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.shared_energy_capacity * nationData.shared_energy_fill), 5);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.shared_manpower_capacity * nationData.shared_manpower_fill), 5);
		Constants.EncodeUnsignedNumber(_output_buffer, Gameplay.DetermineEnergyAvailableFromAllies(nationData), 5);
		Constants.EncodeUnsignedNumber(_output_buffer, Gameplay.DetermineManpowerAvailableFromAllies(nationData), 5);
		Constants.EncodeUnsignedNumber(_output_buffer, nationData.invisibility ? 1 : 0, 1);
		Constants.EncodeUnsignedNumber(_output_buffer, nationData.insurgency ? 1 : 0, 1);
		Constants.EncodeUnsignedNumber(_output_buffer, nationData.total_defense ? 1 : 0, 1);
		Constants.EncodeUnsignedNumber(_output_buffer, nationData.veteran ? 1 : 0, 1);

		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.tech_perm), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.bio_perm), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.psi_perm), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.manpower_rate_perm), 3);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.energy_rate_perm), 3);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.xp_multiplier_perm * 100), 2);

		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.tech_temp), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.bio_temp), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.psi_temp), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.manpower_rate_temp), 3);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.energy_rate_temp), 3);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.xp_multiplier_temp * 100), 2);

		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.tech_object), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.bio_object), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.psi_object), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.manpower_rate_object), 3);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.energy_rate_object), 3);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.xp_multiplier_object * 100), 2);

		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.tech_mult * 100), 3);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.bio_mult * 100), 3);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.psi_mult * 100), 3);
		Constants.EncodeNumber(_output_buffer, (int)(nationData.manpower_rate_mult * 100), 3);
		Constants.EncodeNumber(_output_buffer, (int)(nationData.energy_rate_mult * 100), 3);
		Constants.EncodeNumber(_output_buffer, (int)(nationData.manpower_max_mult * 100), 3);
		Constants.EncodeNumber(_output_buffer, (int)(nationData.energy_max_mult * 100), 3);
		Constants.EncodeNumber(_output_buffer, (int)(nationData.hp_per_square_mult * 100), 3);
		Constants.EncodeNumber(_output_buffer, (int)(nationData.hp_restore_mult * 100), 3);
		Constants.EncodeNumber(_output_buffer, (int)(nationData.attack_manpower_mult * 100), 3);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.advance_points), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.reset_advances_count * Constants.RESET_ADVANCES_BASE_PRICE), 3);
		Constants.EncodeUnsignedNumber(_output_buffer, _delay, 1);

		Constants.EncodeUnsignedNumber(_output_buffer, nationData.raid_attacker_medals + nationData.raid_defender_medals, 3);
		Constants.EncodeUnsignedNumber(_output_buffer, raid_in_progress ? 1 : 0, 1);

		EncodeFootprint(_output_buffer, nationData.mainland_footprint);
		EncodeFootprint(_output_buffer, nationData.homeland_footprint);

		if (raid_in_progress)
		{
			// Get the raid's data.
			RaidData raidData = (RaidData)DataManager.GetData(Constants.DT_RAID, nationData.raidID, false);
			Output.PrintToScreen("RaidID: " + nationData.raidID + ", raidData: " + raidData);

			// Encode the nation's raidland footprint.
			EncodeFootprint(_output_buffer, raidData.attacker_footprint);
		}
	}

	public static void GetTechnologiesEvent(StringBuffer _output_buffer, int _nationID)
	{
		// Get the nation's tech data
	  NationTechData nationTechData = (NationTechData)DataManager.GetData(Constants.DT_NATIONTECH, _nationID, false);

    if (nationTechData == null) {
      Output.PrintToScreen("ERROR: GetTechnologiesEvent(), nationTechData is NULL for ID " + _nationID);
      return;
    }

	  // Encode event ID
	  Constants.EncodeString(_output_buffer, "event_technologies");

		// Encode tech_counts map.
		Constants.EncodeUnsignedNumber(_output_buffer, nationTechData.tech_count.size(), 2);
		for (Map.Entry<Integer, Integer> entry : nationTechData.tech_count.entrySet())
		{
			Constants.EncodeUnsignedNumber(_output_buffer, entry.getKey(), 2);
			Constants.EncodeUnsignedNumber(_output_buffer, entry.getValue(), 2);
		}

		// Encode tech_temp_expire_time map.
		int cur_time = Constants.GetTime();
		Constants.EncodeUnsignedNumber(_output_buffer, nationTechData.tech_temp_expire_time.size(), 2);
		for (Map.Entry<Integer, Integer> entry : nationTechData.tech_temp_expire_time.entrySet())
		{
			Constants.EncodeUnsignedNumber(_output_buffer, entry.getKey(), 2);
			Constants.EncodeUnsignedNumber(_output_buffer, Math.max(0, entry.getValue() - cur_time), 5);
		}
	}

	public static void GetAddTechnologyEvent(StringBuffer _output_buffer, int _techID, int _expire_time, int _delay)
	{
		// Encode event ID
	  Constants.EncodeString(_output_buffer, "add_technology");

		Constants.EncodeUnsignedNumber(_output_buffer, _techID, 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (_expire_time <= 0) ? 0 : (_expire_time - Constants.GetTime()), 5);
		Constants.EncodeUnsignedNumber(_output_buffer, _delay, 1);
	}

	public static void GetRemoveTechnologyEvent(StringBuffer _output_buffer, int _techID)
	{
		// Encode event ID
	  Constants.EncodeString(_output_buffer, "remove_technology");

		Constants.EncodeUnsignedNumber(_output_buffer, _techID, 2);
	}

	public static void GetTechPricesEvent(StringBuffer _output_buffer)
	{
		// Encode event ID
		Constants.EncodeString(_output_buffer, "event_tech_prices");

		// Encode the number of prices that will be sent
		Constants.EncodeUnsignedNumber(_output_buffer, GlobalData.instance.tech_price_records.size(), 2);

		for (TechPriceRecord tech_price_record : GlobalData.instance.tech_price_records.values())
		{
			// Encode the ID and current price of this tech.
			Constants.EncodeUnsignedNumber(_output_buffer, tech_price_record.ID, 2);
			Constants.EncodeUnsignedNumber(_output_buffer, tech_price_record.price, 4);
		}
	}

	public static void GetMapFlagsEvent(StringBuffer _output_buffer, int _nationID)
	{
	  // Get the user's nation's data
	  NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationID, false);

		// Encode event ID
		Constants.EncodeString(_output_buffer, "event_map_flags");

		// Encode the number of map flags
		Constants.EncodeUnsignedNumber(_output_buffer, nationData.map_flags_token.size(), 2);

		// Encode list of map flags
		int token;
		int [] coord_array = new int[2];
		for (int cur_index = 0; cur_index < nationData.map_flags_token.size(); cur_index++)
		{
			// Encode information about the current map flag
			token = nationData.map_flags_token.get(cur_index);
			Constants.UntokenizeCoordinates(token, coord_array);
			Constants.EncodeUnsignedNumber(_output_buffer, coord_array[0], 4);
			Constants.EncodeUnsignedNumber(_output_buffer, coord_array[1], 4);
			Constants.EncodeString(_output_buffer, nationData.map_flags_title.get(cur_index));
		}
	}

	public static void GetAllObjectsEvent(StringBuffer _output_buffer, NationData _nationData)
	{
		// Encode event ID
		Constants.EncodeString(_output_buffer, "event_all_objects");

		// Encode the number of object records
		Constants.EncodeUnsignedNumber(_output_buffer, _nationData.objects.size(), 3);

		// Encode list of objects
		ObjectRecord cur_record;
		for (int cur_index = 0; cur_index < _nationData.objects.size(); cur_index++)
		{
			// Encode information about the current object
			cur_record = _nationData.objects.get(cur_index);
			Constants.EncodeUnsignedNumber(_output_buffer, cur_record.blockX, 4);
			Constants.EncodeUnsignedNumber(_output_buffer, cur_record.blockY, 4);
			Constants.EncodeUnsignedNumber(_output_buffer, cur_record.objectID, 4);
		}
	}

	public static void GetAddObjectEvent(StringBuffer _output_buffer, int _blockX, int _blockY, int _objectID)
	{
		// Encode event ID
		Constants.EncodeString(_output_buffer, "event_add_object");

		// Encode info about this object.
		Constants.EncodeUnsignedNumber(_output_buffer, _blockX, 4);
		Constants.EncodeUnsignedNumber(_output_buffer, _blockY, 4);
		Constants.EncodeUnsignedNumber(_output_buffer, _objectID, 4);
	}

	public static void GetRemoveObjectEvent(StringBuffer _output_buffer, int _blockX, int _blockY)
	{
		// Encode event ID
		Constants.EncodeString(_output_buffer, "event_remove_object");

		// Encode info about this object.
		Constants.EncodeUnsignedNumber(_output_buffer, _blockX, 4);
		Constants.EncodeUnsignedNumber(_output_buffer, _blockY, 4);
	}

	public static void GetAllBuildCountsEvent(StringBuffer _output_buffer, NationData _nationData)
	{
		// Encode event ID
		Constants.EncodeString(_output_buffer, "event_all_build_counts");

		// Encode the number of build counts
		Constants.EncodeUnsignedNumber(_output_buffer, _nationData.builds.size(), 3);

		// Encode list of build counts
		for (Map.Entry<Integer, Integer> entry : _nationData.builds.entrySet())
		{
			Constants.EncodeUnsignedNumber(_output_buffer, entry.getKey(), 3);
			Constants.EncodeUnsignedNumber(_output_buffer, entry.getValue(), 3);
		}
	}

	public static void GetBuildCountEvent(StringBuffer _output_buffer, int _buildID, int _count)
	{
		// Encode event ID
		Constants.EncodeString(_output_buffer, "event_build_count");

		Constants.EncodeUnsignedNumber(_output_buffer, _buildID, 3);
		Constants.EncodeUnsignedNumber(_output_buffer, _count, 3);
	}

	public static void GetClearGUIEvent(StringBuffer _output_buffer)
	{
		// Encode event ID
		Constants.EncodeString(_output_buffer, "clear_gui");
	}

	public static void GetAwardAvailableAdBonusEvent(StringBuffer _output_buffer, int _amount, int _ad_bonus_available, int _type, int _block_x, int _block_y, int _delay)
	{
		// Encode event ID
		Constants.EncodeString(_output_buffer, "award_available_ad_bonus");

		Constants.EncodeUnsignedNumber(_output_buffer, _amount, 4);
		Constants.EncodeUnsignedNumber(_output_buffer, _ad_bonus_available, 4);
		Constants.EncodeUnsignedNumber(_output_buffer, _type, 1);
		Constants.EncodeNumber(_output_buffer, _block_x, 5);
		Constants.EncodeNumber(_output_buffer, _block_y, 5);
		Constants.EncodeUnsignedNumber(_output_buffer, _delay, 1);
	}

	public static void GetUpdateBarsEvent(StringBuffer _output_buffer, int _energy_delta, int _energy_rate_delta, int _energy_burn_rate_delta, int _manpower_delta, int _credits_delta, int _delay)
	{
		// Encode event ID
		Constants.EncodeString(_output_buffer, "event_update_bars");

		Constants.EncodeNumber(_output_buffer, _energy_delta, 5);
		Constants.EncodeNumber(_output_buffer, _energy_rate_delta, 5);
		Constants.EncodeNumber(_output_buffer, _energy_burn_rate_delta, 5);
		Constants.EncodeNumber(_output_buffer, _manpower_delta, 5);
		Constants.EncodeNumber(_output_buffer, _credits_delta, 4);
		Constants.EncodeUnsignedNumber(_output_buffer, _delay, 1);
	}

	public static void GetUpdateEvent(StringBuffer _output_buffer, int _nationID)
	{
	  // Get the nation's data
	  NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationID, false);

		if (nationData == null) {
			Output.PrintToScreen("ERROR: Nation data not found in GetUpdateEvent(), nationID: " + _nationID);
			return;
		}

		// Determine whether a raid is in progress.
		boolean raid_in_progress = (nationData.raidID > 0);

		// TESTING
		if (WOCServer.GetLogFlag(Constants.LOG_DEBUG)) Output.PrintToScreen("GetUpdateEvent() for nation " + _nationID + " name: " + nationData.name + ", credits: " + nationData.game_money + ", energy: " + nationData.energy);

		// TESTING
		if ((_nationID == 232) && (nationData.game_money == 250)) {
			Output.PrintToScreen("ERROR: GetUpdateEvent(): Nation ID 232 (AlphaWolf) has only 250 credits. _nationID: " + _nationID + ", nationData.ID: " + nationData.ID + ", nationData.name: " + nationData.name);
		}

		// Encode event ID
		Constants.EncodeString(_output_buffer, "event_update");

		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.game_money), 4);
		Constants.EncodeUnsignedNumber(_output_buffer, nationData.GetTransferrableCredits(), 4);
		Constants.EncodeNumber(_output_buffer, nationData.GetNumCreditsAllowedToBuyThisMonth(), 4);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.energy), 5);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.rebirth_countdown), 3);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.rebirth_countdown_purchased), 3);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.prize_money), 5);
		Constants.EncodeUnsignedNumber(_output_buffer, raid_in_progress ? 1 : 0, 1);

		// Encode the nation's mainland map extent
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.mainland_footprint.x0), 3);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.mainland_footprint.y0), 3);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.mainland_footprint.x1), 3);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.mainland_footprint.y1), 3);

		EncodeFootprint(_output_buffer, nationData.mainland_footprint);
		EncodeFootprint(_output_buffer, nationData.homeland_footprint);

		if (raid_in_progress)
		{
			// Get the raid's data.
			RaidData raidData = (RaidData)DataManager.GetData(Constants.DT_RAID, nationData.raidID, false);

			// Encode the nation's raidland footprint.
			EncodeFootprint(_output_buffer, raidData.attacker_footprint);
		}
	}

	public static void GetBuyResourceEvent(StringBuffer _output_buffer, NationData _nationData)
	{
		// Determine whether a raid is in progress.
		boolean raid_in_progress = (_nationData.raidID > 0);

		// Encode event ID
		Constants.EncodeString(_output_buffer, "buy_resource");

		Constants.EncodeUnsignedNumber(_output_buffer, (int)(_nationData.game_money), 4);
		Constants.EncodeUnsignedNumber(_output_buffer, _nationData.GetTransferrableCredits(), 4);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(_nationData.energy), 5);
		Constants.EncodeUnsignedNumber(_output_buffer, raid_in_progress ? 1 : 0, 1);

		EncodeFootprint(_output_buffer, _nationData.mainland_footprint);
		EncodeFootprint(_output_buffer, _nationData.homeland_footprint);

		if (raid_in_progress)
		{
			// Get the raid's data.
			RaidData raidData = (RaidData)DataManager.GetData(Constants.DT_RAID, _nationData.raidID, false);

			// Encode the nation's raidland footprint.
			EncodeFootprint(_output_buffer, raidData.attacker_footprint);
		}
	}

	public static void GetNationTournamentStatusEvent(StringBuffer _output_buffer, NationData _nationData, int _delay)
	{
		// Encode event ID
		Constants.EncodeString(_output_buffer, "tnmt_status_nation");

		Constants.EncodeNumber(_output_buffer, (int)(_nationData.tournament_start_day), 6);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(_nationData.tournament_active ? 1 : 0), 1);
		Constants.EncodeNumber(_output_buffer, (int)(_nationData.tournament_rank), 3);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(_nationData.trophies_available * 100), 5);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(_nationData.trophies_banked * 100), 5);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(_delay), 1);
	}

	public static void GetGlobalTournamentStatusEvent(StringBuffer _output_buffer)
	{
		// Encode event ID
		Constants.EncodeString(_output_buffer, "tnmt_status_global");

		Constants.EncodeNumber(_output_buffer, (int)(TournamentData.instance.start_day), 6);
		Constants.EncodeNumber(_output_buffer, (int)(TournamentData.instance.status), 1);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(TournamentData.instance.num_active_contenders), 3);
		Constants.EncodeUnsignedNumber(_output_buffer, Math.max(0, TournamentData.instance.enrollment_closes_time - Constants.GetTime()), 5); // Seconds until enrollment closes.
		Constants.EncodeUnsignedNumber(_output_buffer, Math.max(0, TournamentData.instance.next_elimination_time - Constants.GetTime()), 5); // Seconds until next elimination.
		Constants.EncodeUnsignedNumber(_output_buffer, Math.max(0, TournamentData.instance.end_time - Constants.GetTime()), 5); // Seconds until tournament ends.
	}

	public static void GetRaidStatusEvent(StringBuffer _output_buffer, NationData _nationData, int _delay)
	{
		// Encode event ID
		Constants.EncodeString(_output_buffer, "raid_status");

		// Encode delay
		Constants.EncodeUnsignedNumber(_output_buffer, _delay, 2);

		if (_nationData.raidID <= 0)
		{
			Constants.EncodeNumber(_output_buffer, -1, 5); // Defender nationID of -1 means no raid is active.
		}
		else
		{
			// Get the data for this nation's current raid.
			RaidData raidData = (RaidData)DataManager.GetData(Constants.DT_RAID, _nationData.raidID, false);

			// Encode info about the current raid.
			Constants.EncodeNumber(_output_buffer, raidData.defender_nationID, 5);
			Constants.EncodeString(_output_buffer, raidData.defender_nationName);
			Constants.EncodeUnsignedNumber(_output_buffer, raidData.defender_starting_area, 2);
			Constants.EncodeUnsignedNumber(_output_buffer, raidData.defender_footprint.area, 2);
			Constants.EncodeUnsignedNumber(_output_buffer, raidData.attacker_start_medals, 3);
			Constants.EncodeUnsignedNumber(_output_buffer, raidData.defender_start_medals, 3);
			Constants.EncodeUnsignedNumber(_output_buffer, raidData.flags, 2);
			Constants.EncodeUnsignedNumber(_output_buffer, Math.max(0, raidData.end_time - Constants.GetTime()), 6);

			if ((raidData.flags & RaidData.RAID_FLAG_BEGUN) == 0)
			{
				Constants.EncodeNumber(_output_buffer, raidData.attacker_0_star_medal_delta, 2);
				Constants.EncodeNumber(_output_buffer, raidData.attacker_5_star_medal_delta, 2);
				Constants.EncodeUnsignedNumber(_output_buffer, raidData.max_reward_credits, 2);
				Constants.EncodeUnsignedNumber(_output_buffer, raidData.max_reward_xp, 4);
				Constants.EncodeUnsignedNumber(_output_buffer, raidData.max_reward_rebirth, 2);
				Constants.EncodeUnsignedNumber(_output_buffer, Math.max(0, raidData.start_time + Raid.RAID_CANDIDATE_REVIEW_PERIOD - Constants.GetTime()), 2);
			}

			if ((raidData.flags & RaidData.RAID_FLAG_FINISHED) != 0)
			{
				Constants.EncodeUnsignedNumber(_output_buffer, raidData.percentage_defeated, 2);
				Constants.EncodeNumber(_output_buffer, raidData.attacker_reward_medals, 2);
				Constants.EncodeNumber(_output_buffer, raidData.defender_reward_medals, 2);
				Constants.EncodeUnsignedNumber(_output_buffer, raidData.reward_credits, 2);
				Constants.EncodeUnsignedNumber(_output_buffer, raidData.reward_xp, 4);
				Constants.EncodeUnsignedNumber(_output_buffer, raidData.reward_rebirth, 2);
			}
		}
	}

	public static void GetRaidLogsEvent(StringBuffer _output_buffer, NationData _nationData)
	{
		int raidID;

		// Encode event ID
		Constants.EncodeString(_output_buffer, "raid_logs");

		// Encode the length of the nation's raid_defense_log.
		Constants.EncodeUnsignedNumber(_output_buffer, _nationData.raid_defense_log.size(), 2);

		// Encode the raid defense log.
		for (int i = 0; i < _nationData.raid_defense_log.size(); i++)
		{
			raidID = _nationData.raid_defense_log.get(i);
			EncodeRaidLogEntry(_output_buffer, (RaidData)DataManager.GetData(Constants.DT_RAID, raidID, false), false);
		}

		// Encode the length of the nation's raid_attack_log.
		Constants.EncodeUnsignedNumber(_output_buffer, _nationData.raid_attack_log.size(), 2);

		// Encode the raid attack log.
		for (int i = 0; i < _nationData.raid_attack_log.size(); i++)
		{
			raidID = _nationData.raid_attack_log.get(i);
			EncodeRaidLogEntry(_output_buffer, (RaidData)DataManager.GetData(Constants.DT_RAID, raidID, false), true);
		}
	}

	public static void GetRaidLogEntryEvent(StringBuffer _output_buffer, RaidData _raidData, boolean _attack)
	{
		int raidID;

		// Encode event ID
		Constants.EncodeString(_output_buffer, "raid_log_entry");

		// Encode whether this is an attack or defense entry.
		Constants.EncodeUnsignedNumber(_output_buffer, _attack ? 1 : 0, 1);

		// Encode the info about this raid.
		EncodeRaidLogEntry(_output_buffer, _raidData, _attack);
	}

	public static void EncodeRaidLogEntry(StringBuffer _output_buffer, RaidData _raidData, boolean _attack)
	{
		int start_time_ago = Math.max(0, Constants.GetTime() - _raidData.start_time);

		//Output.PrintToScreen("EncodeRaidLogEntry() raidID: " + _raidData.ID + ", cur time: " + Constants.GetTime() + " raid start time: " + _raidData.start_time + ", start_time_ago: " + start_time_ago + ", Raid.RAID_HISTORY_DURATION: " + Raid.RAID_HISTORY_DURATION);

		if (start_time_ago >= Raid.RAID_HISTORY_DURATION)
		{
			// This raid is expired (do not display in history).
			Constants.EncodeUnsignedNumber(_output_buffer, 0, 1);
		}
		else
		{
			// This raid is valid.
			Constants.EncodeUnsignedNumber(_output_buffer, 1, 1);

			// Encode info about this raid, from either the attacker or defender perspective as appropriate.
			Constants.EncodeUnsignedNumber(_output_buffer, _raidData.ID - Raid.RAID_ID_BASE, 5);
			Constants.EncodeUnsignedNumber(_output_buffer, _attack ? _raidData.defender_nationID : _raidData.attacker_nationID, 5);
			Constants.EncodeString(_output_buffer, _attack ? _raidData.defender_nationName : _raidData.attacker_nationName);
			Constants.EncodeUnsignedNumber(_output_buffer, _attack ? _raidData.defender_start_medals : _raidData.attacker_start_medals, 3);
			Constants.EncodeUnsignedNumber(_output_buffer, _raidData.flags, 2);
			Constants.EncodeUnsignedNumber(_output_buffer, start_time_ago, 6);
			Constants.EncodeUnsignedNumber(_output_buffer, Math.max(0, _raidData.percentage_defeated), 2);
			Constants.EncodeNumber(_output_buffer, _attack ? _raidData.attacker_reward_medals : _raidData.defender_reward_medals, 2);
			Constants.EncodeUnsignedNumber(_output_buffer, _raidData.reward_credits, 2);
			Constants.EncodeUnsignedNumber(_output_buffer, _raidData.reward_xp, 4);
			Constants.EncodeUnsignedNumber(_output_buffer, _raidData.reward_rebirth, 2);
		}
	}

	public static void GetNationAreasEvent(StringBuffer _output_buffer, int _userID)
	{
		// Get the user's data
	  UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

	  // Get the user's nation's data
	  int nationID = userData.nationID;
	  NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

		// Do not send output if ID is not valid
		if ((nationID <= 0) || (_userID <= 0)) {
			return;
		}

		// Update the visibility of each of this nation's areas, if it is not up to date.
		if (nationData.area_visibility_updated == false) {
			World.UpdateAreaVisibility(nationData);
		}

		// Encode event ID
		Constants.EncodeString(_output_buffer, "nation_areas");

		// Encode the number of nation areas.
		Constants.EncodeUnsignedNumber(_output_buffer, nationData.areas.size(), 2);

		// Iterate through each of this nation's areas...
		AreaData cur_area_data;
		for (int i = 0; i < nationData.areas.size(); i++)
		{
			cur_area_data = (AreaData)(nationData.areas.get(i));

			// Encode this nation area.
			Constants.EncodeUnsignedNumber(_output_buffer, cur_area_data.nationX, 3);
			Constants.EncodeUnsignedNumber(_output_buffer, cur_area_data.nationY, 3);
			Constants.EncodeUnsignedNumber(_output_buffer, cur_area_data.visible ? 1 : 0, 1);
		}
	}

	public static void GetNationInfoEvent(StringBuffer _output_buffer, int _userID, int _map_block_x, int _map_block_y)
	{
		// Determine nationID of target block
		BlockData block_data = DataManager.GetBlockDataForUser(_userID, _map_block_x, _map_block_y, false);
		int targetNationID = block_data.nationID;

		if (targetNationID <= 0)
		{
			// Return message event string
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_nation_info_area_empty")); // "No nation controls this area."
			return;
		}

		GetNationInfoEvent(_output_buffer, _userID, targetNationID);
	}

	public static void GetNationInfoEvent(StringBuffer _output_buffer, int _userID, int _targetNationID)
	{
		int cur_index;

		// Get the user's data
	  UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

	  // Get the user's nation's data
	  int nationID = userData.nationID;
	  NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

		// Do not send output if data is not valid
		if ((userData == null) || (nationData == null)) {
			return;
		}

		// Get the target nation's data
		NationData targetNationData = (NationData)DataManager.GetData(Constants.DT_NATION, _targetNationID, false);

		if (targetNationData == null)
		{
			Output.PrintToScreen("Error: GetNationInfoEvent() nation data not found for target nation " + _targetNationID);
			return;
		}

		// Determine the map to base stats on. If the requesting user's map is not the mainland, or the target nation's homeland or raid, use the mainland stats.
		int mapID = userData.mapID;
		if ((mapID != Constants.MAINLAND_MAP_ID) && (mapID != targetNationData.homeland_mapID) && ((targetNationData.raidID <= 0) || (mapID != targetNationData.raidID))) {
			mapID = Constants.MAINLAND_MAP_ID;
		}

		// Get the combat stats information
		Gameplay.CombatStats combat_stats = new Gameplay.CombatStats();
		Gameplay.DetermineCombatStats(mapID, 1, nationData, targetNationData, combat_stats);

		// Encode event ID
	  Constants.EncodeString(_output_buffer, "event_nation_info");

		// Encode the nation's information
		Constants.EncodeUnsignedNumber(_output_buffer, _targetNationID, 5);
		Constants.EncodeString(_output_buffer, targetNationData.name);
		Constants.EncodeUnsignedNumber(_output_buffer, targetNationData.level, 2);
		Constants.EncodeUnsignedNumber(_output_buffer, Math.max(0, targetNationData.GetFootprint(mapID).area), 4);
		Constants.EncodeNumber(_output_buffer, (targetNationData.tournament_active && (targetNationData.tournament_start_day == TournamentData.instance.start_day)) ? (int)(targetNationData.trophies_available + targetNationData.trophies_banked + 0.5f) : -1, 5);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(targetNationData.GetFinalGeoEfficiency(mapID) * 1000.0f + 0.5f), 3);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(targetNationData.GetFinalStatTech(mapID) + 0.5f), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(targetNationData.GetFinalStatBio(mapID) + 0.5f), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(targetNationData.GetFinalStatPsi(mapID) + 0.5f), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, combat_stats.attacker_stat, 1);
		Constants.EncodeUnsignedNumber(_output_buffer, combat_stats.defender_stat, 1);

		// Encode number of alliances
		Constants.EncodeUnsignedNumber(_output_buffer, targetNationData.alliances_active.size(), 1);

		// Encode list of active alliances
		NationData allyNationData;
		for (cur_index = 0; cur_index < targetNationData.alliances_active.size(); cur_index++)
		{
			allyNationData = (NationData)DataManager.GetData(Constants.DT_NATION, targetNationData.alliances_active.get(cur_index), false);
			Constants.EncodeUnsignedNumber(_output_buffer, allyNationData.ID, 5);
			Constants.EncodeString(_output_buffer, allyNationData.name);
		}

		// Encode number of members
		Constants.EncodeUnsignedNumber(_output_buffer, targetNationData.users.size(), 2);

		// Encode list of members
		UserData cur_member_data;
		boolean logged_in;
		int curMemberID;
		for (cur_index = 0; cur_index < targetNationData.users.size(); cur_index++)
		{
			// Get the current member's data
			curMemberID = targetNationData.users.get(cur_index);
			cur_member_data = (UserData)DataManager.GetData(Constants.DT_USER, curMemberID, false);

      if (cur_member_data == null) {
        Output.PrintToScreen("GetNationInfoEvent() ERROR: Data not found for user ID " + curMemberID);
        break;
      }

			// Determine whether the user is logged in
			logged_in = WOCServer.IsUserLoggedIn(curMemberID, _targetNationID);

			// Encode the current member's information
			Constants.EncodeString(_output_buffer, cur_member_data.name);
			Constants.EncodeUnsignedNumber(_output_buffer, (logged_in == true) ? 1 : 0, 1);
			Constants.EncodeUnsignedNumber(_output_buffer, cur_member_data.rank, 2);
		}
	}

	public static void GetNationOrbsEvent(StringBuffer _output_buffer, int _nationID)
	{
		int i, token, winnings, blockObjectID, blockNationID;
		int coord_array[] = new int[2];

		// Get the mainland map
		LandMap land_map = DataManager.GetLandMap(Constants.MAINLAND_MAP_ID, false);

	  // Get the nation's data
	  NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationID, false);

		// Encode event ID
		Constants.EncodeString(_output_buffer, "nation_orbs");

		// Encode orb winnings history, all-time and monthly.
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.prize_money_history), 5);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.prize_money_history_monthly), 5);

		// Encode monthly orbs
		Constants.EncodeUnsignedNumber(_output_buffer, nationData.goals_monthly_token.size(), 2);

		for (i = 0; i < nationData.goals_monthly_token.size(); i++)
		{
			token = nationData.goals_monthly_token.get(i);
			winnings = (int)(float)(nationData.goals_monthly_winnings.get(i));
			Constants.UntokenizeCoordinates(token, coord_array);
			blockObjectID = land_map.GetBlockObjectID(coord_array[0], coord_array[1]);
			blockNationID = land_map.GetBlockNationID(coord_array[0], coord_array[1]);

			// Encode monthly winnings info about this orb
			Constants.EncodeUnsignedNumber(_output_buffer, coord_array[0], 4);
			Constants.EncodeUnsignedNumber(_output_buffer, coord_array[1], 4);
			Constants.EncodeNumber(_output_buffer, blockObjectID, 3);
			Constants.EncodeUnsignedNumber(_output_buffer, winnings, 5);
			Constants.EncodeUnsignedNumber(_output_buffer, (blockNationID == _nationID) ? 1 : 0, 1);
		}

		// Encode all-time orbs
		Constants.EncodeUnsignedNumber(_output_buffer, nationData.goals_token.size(), 2);

		for (i = 0; i < nationData.goals_token.size(); i++)
		{
			token = nationData.goals_token.get(i);
			winnings = (int)(float)(nationData.goals_winnings.get(i));
			Constants.UntokenizeCoordinates(token, coord_array);
			blockObjectID = land_map.GetBlockObjectID(coord_array[0], coord_array[1]);
			blockNationID = land_map.GetBlockNationID(coord_array[0], coord_array[1]);

			// Encode all-time winnings info about this orb
			Constants.EncodeUnsignedNumber(_output_buffer, coord_array[0], 4);
			Constants.EncodeUnsignedNumber(_output_buffer, coord_array[1], 4);
			Constants.EncodeNumber(_output_buffer, blockObjectID, 3);
			Constants.EncodeUnsignedNumber(_output_buffer, winnings, 5);
			Constants.EncodeUnsignedNumber(_output_buffer, (blockNationID == _nationID) ? 1 : 0, 1);
		}
	}

	public static void GetOrbWinningsEvent(StringBuffer _output_buffer, int _userID, int _orbX, int _orbY)
	{
		UserData contactUserData;
		NationData contactNationData;
		int index;

		// Get the user's data
	  UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

	  // Get the user's nation's data
	  NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, userData.nationID, false);

		// Get the orb's coordinates token
		int token = Constants.TokenizeCoordinates(_orbX, _orbY);

		// Determine the nation's amount of winnings from the orb at the given location.
		index = nationData.goals_token.indexOf(Integer.valueOf(token));
		int winnings = (index != -1) ? (nationData.goals_winnings.get(index)).intValue() : 0;
		index = nationData.goals_monthly_token.indexOf(Integer.valueOf(token));
		int winnings_monthly = (index != -1) ? (nationData.goals_monthly_winnings.get(index)).intValue() : 0;

		// Encode event
		Constants.EncodeString(_output_buffer, "orb_winnings");
		Constants.EncodeUnsignedNumber(_output_buffer, winnings, 5);
		Constants.EncodeUnsignedNumber(_output_buffer, winnings_monthly, 5);

		// Encode all contacts' nations' winnings for this orb.
		Map.Entry<Integer, Integer> entry;
		for (Iterator<Map.Entry<Integer, Integer>> it = userData.contacts.entrySet().iterator(); it.hasNext(); )
		{
			entry = it.next();

			// If this contact is active...
			if (entry.getValue() >= Comm.CONTACT_ACTIVE_THRESHOLD)
			{
				// Get the contact user's data
			  contactUserData = (UserData)DataManager.GetData(Constants.DT_USER, entry.getKey(), false);

				if ((contactUserData.nationID != userData.nationID) && (nation_ranks_map.containsKey(contactUserData.nationID) == false))
				{
					// Get the contact nation's data
					contactNationData = (NationData)DataManager.GetData(Constants.DT_NATION, contactUserData.nationID, false);

					index = contactNationData.goals_token.indexOf(Integer.valueOf(token));

					if (index != -1) {
						EncodeOrbWinnings(_output_buffer, contactNationData, token);
					}

					// Record this contact nation's ID in the nation_ranks_map so its data will not be added again, if there is another contact belonging to that nation.
					nation_ranks_map.put(contactUserData.nationID, 1);
				}
			}
		}

		// Encode terminator to let the client know that th list is finished.
	  Constants.EncodeNumber(_output_buffer, -1, 5);

		// Clear the nation_ranks_map for re-use.
		nation_ranks_map.clear();
	}

	public static void GetRanksDataEvent(StringBuffer _output_buffer, int _userID)
	{
		// Get the user's data
	  UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

	  // Get the user's nation's data
	  NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, userData.nationID, false);

		// Encode event
		Constants.EncodeString(_output_buffer, "ranks_data");

		// Encode the player's user and nation ranks.
		EncodeUserRanks(_output_buffer, userData);
		EncodeNationRanks(_output_buffer, nationData);

		int num_active_contacts = 0;
		UserData contactUserData;
		NationData contactNationData;
		Map.Entry<Integer, Integer> entry;

		// Count the number of active contacts (contacts with value exceeding threshold).
		for (Integer value : userData.contacts.values()) {
			if (value >= Comm.CONTACT_ACTIVE_THRESHOLD) {
				num_active_contacts++;
			}
		}

		// Encode count of active contacts.
		Constants.EncodeUnsignedNumber(_output_buffer, num_active_contacts, 2);

		for (Iterator<Map.Entry<Integer, Integer>> it = userData.contacts.entrySet().iterator(); it.hasNext(); )
		{
			entry = it.next();

			// If this contact is active...
			if (entry.getValue() >= Comm.CONTACT_ACTIVE_THRESHOLD)
			{
				// Get the contact user's data
			  contactUserData = (UserData)DataManager.GetData(Constants.DT_USER, entry.getKey(), false);

				// Encode this user's ranks data.
				EncodeUserRanks(_output_buffer, contactUserData);

				// Record that this user's nation must have its ranks data sent (if it is not also the given user's nation).
				if (contactUserData.nationID != userData.nationID) {
					nation_ranks_map.put(contactUserData.nationID, 1);
				}
			}
		}

		// Encode count of active contacts' nations.
		Constants.EncodeUnsignedNumber(_output_buffer, nation_ranks_map.size(), 2);

		for (Integer contactNationID : nation_ranks_map.keySet())
		{
			// Get the contact nation's data
		  contactNationData = (NationData)DataManager.GetData(Constants.DT_NATION, contactNationID, false);

			// Encode this nation's ranks data.
			EncodeNationRanks(_output_buffer, contactNationData);
		}

		// Clear the nation_ranks_map for re-use.
		nation_ranks_map.clear();
	}

	public static void EncodeFootprint(StringBuffer _output_buffer, Footprint _footprint)
	{
		// TESTING
		if (_footprint.energy_burn_rate < 0) {
			Output.PrintToScreen("ERROR: Footprint energy_burn_rate is negative: " + _footprint.energy_burn_rate + ". Area: " + _footprint.area);
		}

		Constants.EncodeUnsignedNumber(_output_buffer, (int)(Math.max(0, _footprint.area)), 4);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(Math.max(0, _footprint.border_area)), 4);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(Math.max(0, _footprint.perimeter)), 4);
		Constants.EncodeNumber(_output_buffer, (int)(_footprint.geo_efficiency_base * 1000), 4);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)Math.max(0, _footprint.energy_burn_rate), 4);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)(_footprint.manpower), 4);
		Constants.EncodeUnsignedNumber(_output_buffer, (Constants.GetAbsoluteDay() == _footprint.prev_buy_manpower_day) ? _footprint.buy_manpower_day_amount : 0, 6);
	}

	public static void EncodeUserRanks(StringBuffer _output_buffer, UserData _userData)
	{
	  Constants.EncodeNumber(_output_buffer, _userData.ID, 5);
	  Constants.EncodeString(_output_buffer, _userData.name);
		Constants.EncodeUnsignedNumber(_output_buffer, Math.max(0, Math.min(Constants.MaxEncodableUnsignedNumber(5), (int)_userData.xp)), 5);
		Constants.EncodeUnsignedNumber(_output_buffer, Math.max(0, Math.min(Constants.MaxEncodableUnsignedNumber(5), (int)_userData.xp_monthly)), 5);
		Constants.EncodeUnsignedNumber(_output_buffer, Math.max(0, _userData.max_num_followers), 3);
		Constants.EncodeUnsignedNumber(_output_buffer, Math.max(0, _userData.max_num_followers_monthly), 3);
	}

	public static void EncodeNationRanks(StringBuffer _output_buffer, NationData _nationData)
	{
	  Constants.EncodeNumber(_output_buffer, _nationData.ID, 5);
	  Constants.EncodeString(_output_buffer, _nationData.name);
		Constants.EncodeUnsignedNumber(_output_buffer, Math.max(0, _nationData.level_history), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, Math.max(0, _nationData.rebirth_count), 2);
		Constants.EncodeUnsignedNumber(_output_buffer, Math.max(0, Math.min(Constants.MaxEncodableUnsignedNumber(5), (int)_nationData.xp_history)), 5);
		Constants.EncodeUnsignedNumber(_output_buffer, Math.max(0, Math.min(Constants.MaxEncodableUnsignedNumber(5), (int)_nationData.xp_history_monthly)), 5);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)_nationData.prize_money_history, 5);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)_nationData.prize_money_history_monthly, 5);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)_nationData.raid_earnings_history, 5);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)_nationData.raid_earnings_history_monthly, 5);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)_nationData.orb_shard_earnings_history, 5);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)_nationData.orb_shard_earnings_history_monthly, 5);
		Constants.EncodeUnsignedNumber(_output_buffer, Math.max(0, _nationData.medals_history), 3);
		Constants.EncodeUnsignedNumber(_output_buffer, Math.max(0, _nationData.medals_history_monthly), 3);
		Constants.EncodeUnsignedNumber(_output_buffer, _nationData.quests_completed, 2);
		Constants.EncodeUnsignedNumber(_output_buffer, _nationData.quests_completed_monthly, 2);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)_nationData.tournament_trophies_history, 5);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)_nationData.tournament_trophies_history_monthly, 5);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)_nationData.donated_energy_history, 5);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)_nationData.donated_energy_history_monthly, 5);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)_nationData.donated_manpower_history, 5);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)_nationData.donated_manpower_history_monthly, 5);
		Constants.EncodeUnsignedNumber(_output_buffer, _nationData.captures_history, 5);
		Constants.EncodeUnsignedNumber(_output_buffer, _nationData.captures_history_monthly, 5);
		Constants.EncodeUnsignedNumber(_output_buffer, _nationData.max_area, 4);
		Constants.EncodeUnsignedNumber(_output_buffer, _nationData.max_area_monthly, 4);
		Constants.EncodeUnsignedNumber(_output_buffer, (_nationData.tournament_start_day == TournamentData.instance.start_day) ? (int)(_nationData.trophies_available + _nationData.trophies_banked + 0.5f) : 0, 4);
	}

	public static void EncodeOrbWinnings(StringBuffer _output_buffer, NationData _nationData, int _orb_token)
	{
		// Determine the nation's amount of winnings from the orb at the given location.
		int index = _nationData.goals_token.indexOf(Integer.valueOf(_orb_token));
		int winnings = (index != -1) ? (_nationData.goals_winnings.get(index)).intValue() : 0;
		index = _nationData.goals_monthly_token.indexOf(Integer.valueOf(_orb_token));
		int winnings_monthly = (index != -1) ? (_nationData.goals_monthly_winnings.get(index)).intValue() : 0;

	  Constants.EncodeNumber(_output_buffer, _nationData.ID, 5);
	  Constants.EncodeString(_output_buffer, _nationData.name);
		Constants.EncodeUnsignedNumber(_output_buffer, winnings, 5);
		Constants.EncodeUnsignedNumber(_output_buffer, winnings_monthly, 5);
	}

	public static void GetAllChatListsEvent(StringBuffer _output_buffer, int _nationID)
	{
		// Get the event for the nation's own chat list.
		GetChatListEvent(_output_buffer, _nationID);

		// Get chat list events for each nation that the given nation belongs to the chat list of.
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationID, false);
		for (int cur_nation_index = 0; cur_nation_index < nationData.reverse_chat_list.size(); cur_nation_index++) {
			GetChatListEvent(_output_buffer, nationData.reverse_chat_list.get(cur_nation_index));
		}
	}

	public static void GetChatListEvent(StringBuffer _output_buffer, int _nationID)
	{
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationID, false);

		if (nationData.chat_list.size() > 0)
		{
			Constants.EncodeString(_output_buffer, "chat_list");
			Constants.EncodeUnsignedNumber(_output_buffer, _nationID, 4);
			Constants.EncodeString(_output_buffer, nationData.name);
			Constants.EncodeUnsignedNumber(_output_buffer, nationData.chat_list.size(), 2);

			// Encode all nationIDs in the chat list.
			for (int cur_recipient_index = 0; cur_recipient_index < nationData.chat_list.size(); cur_recipient_index++)
			{
				NationData curNationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationData.chat_list.get(cur_recipient_index), false);
				Constants.EncodeUnsignedNumber(_output_buffer, nationData.chat_list.get(cur_recipient_index), 4);
				Constants.EncodeString(_output_buffer, curNationData.name);
			}
		}
	}

	public static void GetAllMessagesEvent(StringBuffer _output_buffer, int _userID)
	{
		// Get the user's data
	  UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

	  // Get the user's nation's extended data
	  int nationID = userData.nationID;
	  NationExtData nationExtData = (NationExtData)DataManager.GetData(Constants.DT_NATION_EXT, nationID, false);

		// Encode event ID
		Constants.EncodeString(_output_buffer, "event_all_messages");

		// Encode a dummy value which will later be replaced by the number of messages.
		int num_messages_pos = _output_buffer.length();
		Constants.EncodeUnsignedNumber(_output_buffer, 0, 2);

		// Determine when this user last checked messages
		int prev_check_messages_time = userData.prev_check_messages_time;

		// Encode list of messages, in order from newest to oldest.
		MessageData cur_message;
		int message_type, num_messages = 0, num_game_messages = 0, num_nation_messages = 0, num_other_messages = 0;
		String filtered_text;
		for (int cur_index = nationExtData.messages.size() - 1; cur_index >= 0; cur_index--)
		{
			// Get the current message's data.
			cur_message = (MessageData)nationExtData.messages.get(cur_index);

			// Determine this message's type.
			message_type = (cur_message.nationID == -1) ? Constants.MESSAGE_TYPE_GAME : ((cur_message.nationID == nationID) ? Constants.MESSAGE_TYPE_NATION : Constants.MESSAGE_TYPE_OTHER);
			//Output.PrintToScreen("Message nationID: " + cur_message.nationID + ", type: " + message_type);

			// If we've already reached the full batch size for this type of message, skip this message. Otherwise, increment the count for the message type.
			switch (message_type)
			{
				case Constants.MESSAGE_TYPE_GAME:
					if (num_game_messages == Constants.MESSAGE_BATCH_SIZE) {
						continue;
					} else {
						num_game_messages++;
					}
					break;
				case Constants.MESSAGE_TYPE_NATION:
					if (num_nation_messages == Constants.MESSAGE_BATCH_SIZE) {
						continue;
					} else {
						num_nation_messages++;
					}
					break;
				case Constants.MESSAGE_TYPE_OTHER:
					if (num_other_messages == Constants.MESSAGE_BATCH_SIZE) {
						continue;
					} else {
						num_other_messages++;
					}
					break;
			}

			// Remove emojis
			filtered_text = cur_message.text.replaceAll("[^\\p{L}\\p{M}\\p{N}\\p{P}\\p{Z}\\p{Cf}\\p{Cs}\\s]","");

			// Encode information about the current message
			Constants.EncodeUnsignedNumber(_output_buffer, (cur_message.time > prev_check_messages_time) ? 1 : 0, 1);
      Constants.EncodeNumber(_output_buffer, cur_message.userID, 5);
			Constants.EncodeNumber(_output_buffer, cur_message.nationID, 5);
			Constants.EncodeNumber(_output_buffer, cur_message.deviceID, 5);
			Constants.EncodeString(_output_buffer, cur_message.username);
			Constants.EncodeString(_output_buffer, cur_message.nation_name);
			Constants.EncodeString(_output_buffer, filtered_text);
			Constants.EncodeString(_output_buffer, cur_message.timestamp);
			Constants.EncodeUnsignedNumber(_output_buffer, cur_message.time, 6);
      Constants.EncodeUnsignedNumber(_output_buffer, cur_message.reported, 1);

			// Increment count of number of messages.
			num_messages++;
		}

		//Output.PrintToScreen("Encoded messages. num_messages: " + num_messages + ", num_game_messages: " + num_game_messages + ", num_nation_messages: " + num_nation_messages + ", num_other_messages: " + num_other_messages);

		// Insert the determined number of messages at the appropriate position the output buffer.
		entry_buffer.setLength(0);
		Constants.EncodeUnsignedNumber(entry_buffer, num_messages, 2);
		_output_buffer.replace(num_messages_pos, num_messages_pos + 2, entry_buffer.toString());
	}

	public static void GetMoreMessagesEvent(StringBuffer _output_buffer, int _userID, int _type, int _start)
	{
		// Get the user's data
	  UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

	  // Get the user's nation's extended data
	  int nationID = userData.nationID;
	  NationExtData nationExtData = (NationExtData)DataManager.GetData(Constants.DT_NATION_EXT, nationID, false);

		// Encode event ID
		Constants.EncodeString(_output_buffer, "event_more_messages");

		// Encode the type of messages being sent
		Constants.EncodeUnsignedNumber(_output_buffer, _type, 1);

		// Encode a dummy value which will later be replaced by the number of messages.
		int num_messages_pos = _output_buffer.length();
		Constants.EncodeUnsignedNumber(_output_buffer, 0, 2);

		// Determine when this user last checked messages
		int prev_check_messages_time = userData.prev_check_messages_time;

		// Encode list of messages of the given _type, in order from newest to oldest, starting at the _start index.
		MessageData cur_message;
		int message_type, num_messages = 0, num_messages_before_start = 0;
		String filtered_text;
		for (int cur_index = nationExtData.messages.size() - 1; cur_index >= 0; cur_index--)
		{
			// Get the current message's data.
			cur_message = (MessageData)nationExtData.messages.get(cur_index);

			// Determine this message's type.
			message_type = (cur_message.nationID == -1) ? Constants.MESSAGE_TYPE_GAME : ((cur_message.nationID == nationID) ? Constants.MESSAGE_TYPE_NATION : Constants.MESSAGE_TYPE_OTHER);
			//Output.PrintToScreen("Message nationID: " + cur_message.nationID + ", type: " + message_type);

			// If this message is not of the given _type, skip it.
			if (message_type != _type) {
				continue;
			}

			// If this message of the given _type comes before the _start index, skip it.
			if (num_messages_before_start < _start)
			{
				num_messages_before_start++;
				continue;
			}

			// Remove emojis
			filtered_text = cur_message.text.replaceAll("[^\\p{L}\\p{M}\\p{N}\\p{P}\\p{Z}\\p{Cf}\\p{Cs}\\s]","");

			// Encode information about the current message
			Constants.EncodeUnsignedNumber(_output_buffer, (cur_message.time > prev_check_messages_time) ? 1 : 0, 1);
      Constants.EncodeNumber(_output_buffer, cur_message.userID, 5);
			Constants.EncodeNumber(_output_buffer, cur_message.nationID, 5);
			Constants.EncodeNumber(_output_buffer, cur_message.deviceID, 5);
			Constants.EncodeString(_output_buffer, cur_message.username);
			Constants.EncodeString(_output_buffer, cur_message.nation_name);
			Constants.EncodeString(_output_buffer, filtered_text);
			Constants.EncodeString(_output_buffer, cur_message.timestamp);
			Constants.EncodeUnsignedNumber(_output_buffer, cur_message.time, 6);
      Constants.EncodeUnsignedNumber(_output_buffer, cur_message.reported, 1);

			// Increment count of number of messages.
			num_messages++;

			// If we've reached the size of a full batch of messages, send no more.
			if (num_messages == Constants.MESSAGE_BATCH_SIZE) {
				break;
			}
		}

		//Output.PrintToScreen("Encoded more messages. num_messages: " + num_messages + ", num_messages_before_start: " + num_messages_before_start);

		// Insert the determined number of messages at the appropriate position the output buffer.
		entry_buffer.setLength(0);
		Constants.EncodeUnsignedNumber(entry_buffer, num_messages, 2);
		_output_buffer.replace(num_messages_pos, num_messages_pos + 2, entry_buffer.toString());
	}

	public static void GetNewMessageEvent(StringBuffer _output_buffer, int _delay, MessageData _message_data)
	{
		// Encode event ID
		Constants.EncodeString(_output_buffer, "event_new_message");

		// Encode delay
		Constants.EncodeUnsignedNumber(_output_buffer, _delay, 1);

		// Encode information about the given message
		Constants.EncodeUnsignedNumber(_output_buffer, 1, 1); // Message is unread
		Constants.EncodeNumber(_output_buffer, _message_data.userID, 5);
		Constants.EncodeNumber(_output_buffer, _message_data.nationID, 5);
		Constants.EncodeNumber(_output_buffer, _message_data.deviceID, 5);
		Constants.EncodeString(_output_buffer, _message_data.username);
		Constants.EncodeString(_output_buffer, _message_data.nation_name);
		Constants.EncodeString(_output_buffer, _message_data.text);
		Constants.EncodeString(_output_buffer, _message_data.timestamp);
		Constants.EncodeUnsignedNumber(_output_buffer, _message_data.time, 6);
		Constants.EncodeUnsignedNumber(_output_buffer, _message_data.reported, 1);
	}

	public static void GetPostMessageResultEvent(StringBuffer _output_buffer, ClientString _error_message)
	{
		Constants.EncodeString(_output_buffer, "post_message_result");
		Constants.EncodeUnsignedNumber(_output_buffer, _error_message.IsEmpty() ? 1 : 0, 1);
		_error_message.Encode(_output_buffer);
		ClientString.Release(_error_message);
	}

	public static void GetAllFollowersEvent(StringBuffer _output_buffer, int _userID)
	{
		// Get the user's data
	  UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		// Encode event ID
		Constants.EncodeString(_output_buffer, "event_all_followers");

		// Encode the number of followers
		Constants.EncodeUnsignedNumber(_output_buffer, userData.followers.size(), 3);

		// Encode list of followers
		FollowerData cur_follower;
		UserData followerUserData;
		for (int cur_index = 0; cur_index < userData.followers.size(); cur_index++)
		{
			// Get the current follower's data
			cur_follower = (FollowerData)userData.followers.get(cur_index);

			// Get the current follower's user data
			followerUserData = (UserData)DataManager.GetData(Constants.DT_USER, cur_follower.userID, false);

			if (followerUserData == null) {
				Output.PrintToScreen("ERROR: user " + userData.name + " (" + userData.ID + ") has follower (ID " + cur_follower.userID + ") with missing user data.");
			}

			// Encode information about the current follower
			Constants.EncodeNumber(_output_buffer, cur_follower.userID, 5);
			Constants.EncodeString(_output_buffer, (followerUserData == null) ? "" : followerUserData.name);
      Constants.EncodeUnsignedNumber(_output_buffer, Constants.GetAbsoluteDay() - cur_follower.initDay, 3);
			Constants.EncodeUnsignedNumber(_output_buffer, (int)(cur_follower.bonusXP), 5);
			Constants.EncodeUnsignedNumber(_output_buffer, (int)(cur_follower.bonusCredits), 5);
		}
	}

	public static void GetAllPatronOffersEvent(StringBuffer _output_buffer, int _userID)
	{
		// Get the user's data
	  UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		// Encode event ID
		Constants.EncodeString(_output_buffer, "event_all_patron_offers");

		// Encode the number of patron offers
		Constants.EncodeUnsignedNumber(_output_buffer, userData.patron_offers.size(), 2);

		// Encode list of patron offers
		int offerUserID;
		UserData offerUserData;
		for (int cur_index = 0; cur_index < userData.patron_offers.size(); cur_index++)
		{
			// Get the current patron offer's userID
			offerUserID = userData.patron_offers.get(cur_index);

			// Get the current patron offer's user data
			offerUserData = (UserData)DataManager.GetData(Constants.DT_USER, offerUserID, false);

			// Encode information about the current patron offer
			Constants.EncodeNumber(_output_buffer, offerUserID, 5);
			Constants.EncodeString(_output_buffer, offerUserData.name);
      Constants.EncodeUnsignedNumber(_output_buffer, (int)offerUserData.prev_month_patron_bonus_XP, 5);
			Constants.EncodeUnsignedNumber(_output_buffer, (int)offerUserData.prev_month_patron_bonus_credits, 5);
			Constants.EncodeUnsignedNumber(_output_buffer, (int)offerUserData.followers.size(), 3);
		}
	}

	public static void GetEventAddFollower(StringBuffer _output_buffer, int _followerID)
	{
		// Get the current follower's user data
		UserData followerUserData = (UserData)DataManager.GetData(Constants.DT_USER, _followerID, false);

		if (followerUserData == null)
		{
			Output.PrintToScreen("ERROR: GetEventAddFollower() given _followerID " + _followerID + ", for which no user data is found.");
			return;
		}

		// Encode event ID
		Constants.EncodeString(_output_buffer, "event_add_follower");

		// Encode information about the current follower
		Constants.EncodeNumber(_output_buffer, _followerID, 5);
		Constants.EncodeString(_output_buffer, followerUserData.name);
	}

	public static void GetEventRemoveFollower(StringBuffer _output_buffer, int _followerID)
	{
		// Encode event ID
		Constants.EncodeString(_output_buffer, "event_remove_follower");

		// Encode follower user ID
		Constants.EncodeNumber(_output_buffer, _followerID, 5);
	}

	public static void GetEventAddPatronOffer(StringBuffer _output_buffer, int _patronOfferID)
	{
		// Encode event ID
		Constants.EncodeString(_output_buffer, "event_add_patron_offer");

		// Get the offering patron's user data
		UserData patronOfferUserData = (UserData)DataManager.GetData(Constants.DT_USER, _patronOfferID, false);

		// Encode information about the current patron offer
		Constants.EncodeNumber(_output_buffer, _patronOfferID, 5);
		Constants.EncodeString(_output_buffer, patronOfferUserData.name);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)patronOfferUserData.prev_month_patron_bonus_XP, 5);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)patronOfferUserData.prev_month_patron_bonus_credits, 5);
		Constants.EncodeUnsignedNumber(_output_buffer, patronOfferUserData.followers.size(), 3);
	}

	public static void GetEventRemovePatronOffer(StringBuffer _output_buffer, int _patronOfferID)
	{
		// Encode event ID
		Constants.EncodeString(_output_buffer, "event_remove_patron_offer");

		// Encode patron offer user ID
		Constants.EncodeNumber(_output_buffer, _patronOfferID, 5);
	}

	public static void GetPatronInfoEvent(StringBuffer _output_buffer, int _userID)
	{
		// Get the user's data
	  UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		// Encode event ID
		Constants.EncodeString(_output_buffer, "patron_info");

		// Encode the user's patron code.
		Constants.EncodeString(_output_buffer, userData.patron_code);

		// Encode the patron bonuses I awarded to followers last month.
		Constants.EncodeUnsignedNumber(_output_buffer, (int)userData.prev_month_patron_bonus_XP, 5);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)userData.prev_month_patron_bonus_credits, 5);

		// Encode the user's patron's ID.
		Constants.EncodeNumber(_output_buffer, userData.patronID, 5);

		// If the user has a patron...
		if (userData.patronID != -1)
		{
			// Get the patron's user data
			UserData patronUserData = (UserData)DataManager.GetData(Constants.DT_USER, userData.patronID, false);

			// Encode information about this user's relationship with their patron.
			Constants.EncodeString(_output_buffer, patronUserData.name);
			Constants.EncodeUnsignedNumber(_output_buffer, (int)patronUserData.prev_month_patron_bonus_XP, 5);
			Constants.EncodeUnsignedNumber(_output_buffer, (int)patronUserData.prev_month_patron_bonus_credits, 5);
			Constants.EncodeUnsignedNumber(_output_buffer, patronUserData.followers.size(), 3);
			Constants.EncodeUnsignedNumber(_output_buffer, (int)userData.total_patron_xp_received, 5);
			Constants.EncodeUnsignedNumber(_output_buffer, (int)userData.total_patron_credits_received, 5);
		}
	}

	public static void GetNationDataEvent(StringBuffer _output_buffer, NationData _nationData)
	{
		Constants.EncodeString(_output_buffer, "nation_data");
		Constants.EncodeUnsignedNumber(_output_buffer, _nationData.ID, 4);
		Display.EncodeNationData(_output_buffer, _nationData);
	}

	public static void GetAdminLogEvent(StringBuffer _output_buffer, String _output)
	{
		Constants.EncodeString(_output_buffer, "admin_log");
		Constants.EncodeString(_output_buffer, _output);
	}

	public static void GetNextComplaintEvent(StringBuffer _output_buffer, int _userID, int _complaint_index)
	{
		// Get the user's data
	  UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		// If the user doesn't exist, or is not an admin or mod, do nothing.
		if ((userData == null) || ((userData.admin == false) && (userData.mod_level == 0))) {
			return;
		}

		// If the _complaint_index is beyond the end of the list, loop back to start of list. If the list is empty, index shall be -1.
		if (_complaint_index >= GlobalData.instance.complaints.size()) {
			_complaint_index = Math.min(0, GlobalData.instance.complaints.size() - 1);
		}

		// If there is no complaint to be sent, do nothing.
		if (_complaint_index == -1) {
			return;
		}

		// Get the data for the complaint with the determined index.
		int complaintID = GlobalData.instance.complaints.get(_complaint_index);
	  ComplaintData complaintData = (ComplaintData)DataManager.GetData(Constants.DT_COMPLAINT, complaintID, false);

		if (complaintData == null)
		{
			Output.PrintToScreen("Error: no complaint exists for index " + _complaint_index + ", ID " + GlobalData.instance.complaints.get(_complaint_index) + ".");
			Admin.DeleteComplaint(complaintID); // The complaint's data is invalid; delete it.
			OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("Deleted complaint " + complaintID + "; data does not exist."));
			return;
		}

		// Get the user data of the user who reported this complaint, and the user they reported against.
		UserData reporter_userData = (UserData)DataManager.GetData(Constants.DT_USER, complaintData.userID, false);
		UserData reported_userData = (UserData)DataManager.GetData(Constants.DT_USER, complaintData.reported_userID, false);

		if ((reporter_userData == null) || (reported_userData == null))
		{
			Output.PrintToScreen("Error: Complaint " + complaintID + " has invalid reporter or reported user ID. complaintData.userID: " + complaintData.userID + ", reporter_userData: " + reporter_userData + ", complaintData.reported_userID: " + complaintData.reported_userID + ", reported_userData: " + reported_userData + ".");
			Admin.DeleteComplaint(complaintID); // The complaint's data is invalid; delete it.
			OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("Deleted complaint " + complaintID + "; user data does not exist."));
			return;
		}

		//// TESTING
		//Output.PrintToScreen("complaintID: " + complaintID + ", complaintData.ID: " + complaintData.ID + ", text: " + complaintData.text);
		//Output.PrintToScreen("complaintData.userID: " + complaintData.userID + ", reporter_userData: " + reporter_userData + ", complaintData.reported_userID: " + complaintData.reported_userID + ", reported_userData: " + reported_userData);

		// Get the player data of the user who reported this complaint, and the user they reported against.
		PlayerAccountData reporter_playerData = AccountDB.ReadPlayerAccount(reporter_userData.playerID);
		PlayerAccountData reported_playerData = AccountDB.ReadPlayerAccount(reported_userData.playerID);

		if ((reporter_playerData == null) || (reported_playerData == null))
		{
			Output.PrintToScreen("Error: Complaint " + complaintID + " has invalid reporter or reported player ID. reporter_userData.playerID: " + reporter_userData.playerID + ", reporter_playerData: " + reporter_playerData + ", reported_userData.playerID: " + reported_userData.playerID + ", reported_playerData: " + reported_playerData + ".");
			Admin.DeleteComplaint(complaintID); // The complaint's data is invalid; delete it.
			OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("Deleted complaint " + complaintID + "; player data does not exist."));
			return;
		}

		// Get the nation data for the user who reported this complaint, and the user they reported against.
		NationData reporter_nationData = (NationData)DataManager.GetData(Constants.DT_NATION, reporter_userData.nationID, false);
		NationData reported_nationData = (NationData)DataManager.GetData(Constants.DT_NATION, reported_userData.nationID, false);

		if ((reporter_nationData == null) || (reported_nationData == null))
		{
			Output.PrintToScreen("Error: Complaint " + complaintID + " has invalid reporter or reported nation ID. reporter_userData.nationID: " + reporter_userData.nationID + ", reporter_nationData: " + reporter_nationData + ", reported_userData.nationID: " + reported_userData.nationID + ", reported_nationData: " + reported_nationData + ".");
			Admin.DeleteComplaint(complaintID); // The complaint's data is invalid; delete it.
			OutputEvents.GetChatLogEvent(_output_buffer, ClientString.Get("Deleted complaint " + complaintID + "; nation data does not exist."));
			return;
		}

		// Encode event ID
		Constants.EncodeString(_output_buffer, "complaint");

		// Encode the index of the complaint to be sent, and the total number of compliants.
		Constants.EncodeNumber(_output_buffer, _complaint_index, 3);
		Constants.EncodeNumber(_output_buffer, GlobalData.instance.complaints.size(), 3);

		// Encode the complaint's ID
		Constants.EncodeUnsignedNumber(_output_buffer, complaintID, 6);

		// Encode information about the reporter user.
		Constants.EncodeUnsignedNumber(_output_buffer, reporter_userData.ID, 5);
		Constants.EncodeUnsignedNumber(_output_buffer, reporter_nationData.ID, 5);
		Constants.EncodeString(_output_buffer, reporter_userData.name);
		Constants.EncodeString(_output_buffer, reporter_playerData.email);
		Constants.EncodeString(_output_buffer, reporter_nationData.name);
		Constants.EncodeUnsignedNumber(_output_buffer, reporter_playerData.num_complaints_by, 3);
		Constants.EncodeUnsignedNumber(_output_buffer, reporter_playerData.num_complaints_against, 3);
		Constants.EncodeUnsignedNumber(_output_buffer, reporter_playerData.num_warnings_sent, 3);
		Constants.EncodeUnsignedNumber(_output_buffer, reporter_playerData.num_chat_bans, 3);
		Constants.EncodeUnsignedNumber(_output_buffer, reporter_playerData.num_game_bans, 3);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)Math.max(0, ((float)(reporter_userData.game_ban_end_time - Constants.GetTime()) / (float)(Constants.SECONDS_PER_DAY) + 0.5f)), 3);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)Math.max(0, ((float)(reporter_userData.chat_ban_end_time - Constants.GetTime()) / (float)(Constants.SECONDS_PER_DAY) + 0.5f)), 3);

		// Encode information about the reported user.
		Constants.EncodeUnsignedNumber(_output_buffer, reported_userData.ID, 5);
		Constants.EncodeUnsignedNumber(_output_buffer, reported_nationData.ID, 5);
		Constants.EncodeString(_output_buffer, reported_userData.name);
		Constants.EncodeString(_output_buffer, reported_playerData.email);
		Constants.EncodeString(_output_buffer, reported_nationData.name);
		Constants.EncodeUnsignedNumber(_output_buffer, reported_playerData.num_complaints_by, 3);
		Constants.EncodeUnsignedNumber(_output_buffer, reported_playerData.num_complaints_against, 3);
		Constants.EncodeUnsignedNumber(_output_buffer, reported_playerData.num_warnings_sent, 3);
		Constants.EncodeUnsignedNumber(_output_buffer, reported_playerData.num_chat_bans, 3);
		Constants.EncodeUnsignedNumber(_output_buffer, reported_playerData.num_game_bans, 3);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)Math.max(0, ((float)(reported_userData.game_ban_end_time - Constants.GetTime()) / (float)(Constants.SECONDS_PER_DAY) + 0.5f)), 3);
		Constants.EncodeUnsignedNumber(_output_buffer, (int)Math.max(0, ((float)(reported_userData.chat_ban_end_time - Constants.GetTime()) / (float)(Constants.SECONDS_PER_DAY) + 0.5f)), 3);

		// Encode information about the complaint.
		Constants.EncodeUnsignedNumber(_output_buffer, complaintData.timestamp, 6);
		Constants.EncodeString(_output_buffer, complaintData.issue);
		Constants.EncodeString(_output_buffer, complaintData.text);
	}

	public static void SendMessageEvent(int _userID, ClientString _message)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// Encode event
		broadcast_buffer.setLength(0);
		GetMessageEvent(broadcast_buffer, _message);

		// Send this event to the given user.
		SendToUser(broadcast_buffer.toString(), _userID);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void SendPurchaseCompleteEvent(int _userID, int _package, float _amount, String _currency)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// Encode event
		broadcast_buffer.setLength(0);
		Constants.EncodeString(broadcast_buffer, "event_purchase_complete");
		Constants.EncodeNumber(broadcast_buffer, _package, 1);
		Constants.EncodeUnsignedNumber(broadcast_buffer, (int)(_amount * 100f), 4);
		Constants.EncodeString(broadcast_buffer, _currency);

		// Send this event to the given user.
		SendToUser(broadcast_buffer.toString(), _userID);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void SendSubscriptionEvent(UserData _userData)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// Encode event
		GetSubscriptionEvent(broadcast_buffer, _userData);

		// Send this event to the given user.
		SendToUser(broadcast_buffer.toString(), _userData.ID);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void GetSubscriptionEvent(StringBuffer _output_buffer, UserData _userData)
	{
		Output.PrintToScreen("Sending subscription event for subscription_id " + _userData.subscription_id);

		// Encode subscription information.
		Constants.EncodeString(_output_buffer, "subscription");
		Constants.EncodeUnsignedNumber(_output_buffer, _userData.subscribed ? 1 : 0, 1);
		Constants.EncodeString(_output_buffer, _userData.subscription_gateway);
		Constants.EncodeString(_output_buffer, _userData.subscription_status);
		Constants.EncodeUnsignedNumber(_output_buffer, Math.max(0, _userData.paid_through_time - Constants.GetTime()), 6);
		Constants.EncodeString(_output_buffer, Subscription.GetAssociatedSubscribedUsername(_userData));

		// Encode names of bonus target nations.
		Constants.EncodeString(_output_buffer, (_userData.bonus_credits_target <= 0) ? "" : NationData.GetNationName(_userData.bonus_credits_target));
		Constants.EncodeString(_output_buffer, (_userData.bonus_rebirth_target <= 0) ? "" : NationData.GetNationName(_userData.bonus_rebirth_target));
		Constants.EncodeString(_output_buffer, (_userData.bonus_xp_target <= 0) ? "" : NationData.GetNationName(_userData.bonus_xp_target));
		Constants.EncodeString(_output_buffer, (_userData.bonus_manpower_target <= 0) ? "" : NationData.GetNationName(_userData.bonus_manpower_target));
	}

	public static void SendAwardAvailableAdBonusEvent(int _userID, int _amount, int _ad_bonus_available, int _type, int _block_x, int _block_y, int _delay)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// Encode the event
		GetAwardAvailableAdBonusEvent(broadcast_buffer, _amount, _ad_bonus_available, _type, _block_x, _block_y, _delay);

		// Send this event to the given user.
		SendToUser(broadcast_buffer.toString(), _userID);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void SendCollectAdBonusEvent(int _userID, int _delay)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// Encode event
		broadcast_buffer.setLength(0);
		Constants.EncodeString(broadcast_buffer, "collect_ad_bonus");
		Constants.EncodeUnsignedNumber(broadcast_buffer, _delay, 1);

		// Send this event to the given user.
		SendToUser(broadcast_buffer.toString(), _userID);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastBlockUpdateEvent(LandMap _land_map, int _blockX, int _blockY, BlockData _block_data)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// Encode the block update event
		broadcast_buffer.setLength(0);
		Constants.EncodeString(broadcast_buffer, "block_update");
		Constants.EncodeUnsignedNumber(broadcast_buffer, _blockX, 2);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _blockY, 2);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _block_data.terrain, 1);
		Constants.EncodeNumber(broadcast_buffer, _block_data.nationID, 4);

		if (_block_data.nationID != -1)
		{
			// Get the nation's data
			NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _block_data.nationID, false);

			if (nationData == null) {
				Output.PrintToScreen("ERROR: BroadcastBlockUpdateEvent() called for block " + _blockX + "," + _blockY + " with nationID " + _block_data.nationID + " that has no data!");
			}

			// Encode the nation's data
			Display.EncodeNationData(broadcast_buffer, nationData);
		}

		// Broadcast the event to all users with the given block's view area.
		BroadcastEventToViewArea(_land_map, _blockX, _blockY);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastBlockProcessEvent(LandMap _land_map, int _blockX, int _blockY, BlockData _block_data, int _full_hit_points, int _cur_hit_points, float _hit_points_rate, int _delay, int _process_type)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		int cur_time = Constants.GetTime();

		// Encode the block process event
		broadcast_buffer.setLength(0);
		Constants.EncodeString(broadcast_buffer, "block_process");
		Constants.EncodeUnsignedNumber(broadcast_buffer, _blockX, 2);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _blockY, 2);
		Constants.EncodeNumber(broadcast_buffer, _block_data.nationID, 4);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _full_hit_points, 2);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _cur_hit_points, 2);
		Constants.EncodeUnsignedNumber(broadcast_buffer, (int)(_hit_points_rate * 100.0f), 3);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _delay, 1);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _process_type, 1);

		if (_block_data.nationID != -1)
		{
			// Get the nation's data
			NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _block_data.nationID, false);

			// Encode the nation's data
			Display.EncodeNationData(broadcast_buffer, nationData);
		}

		// Broadcast the event to all users with the given block's view area.
		BroadcastEventToViewArea(_land_map, _blockX, _blockY);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastBattleProcessEvent(LandMap _land_map, int _blockX, int _blockY, BlockData _block_data, int _start_hit_points, int _end_hit_points, int _full_hit_points, int _new_cur_hit_points, int _new_full_hit_points, float _hit_points_rate, int _delay, int _battle_duration, int _initiatorUserID, int _battle_flags)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		int cur_time = Constants.GetTime();

		// Encode the block process event
		broadcast_buffer.setLength(0);
		Constants.EncodeString(broadcast_buffer, "battle_process");
		Constants.EncodeUnsignedNumber(broadcast_buffer, _blockX, 2);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _blockY, 2);
		Constants.EncodeNumber(broadcast_buffer, _block_data.nationID, 4);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _start_hit_points, 2);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _end_hit_points, 2);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _full_hit_points, 2);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _new_cur_hit_points, 2);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _new_full_hit_points, 2);
		Constants.EncodeUnsignedNumber(broadcast_buffer, (int)(_hit_points_rate * 100.0f), 3);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _delay, 1);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _battle_duration, 1);
		Constants.EncodeNumber(broadcast_buffer, _initiatorUserID, 5);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _battle_flags, 2);

		if (_block_data.nationID != -1)
		{
			// Get the nation's data
			NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _block_data.nationID, false);

			// Encode the nation's data
			Display.EncodeNationData(broadcast_buffer, nationData);
		}

		if (_land_map.ID >= Raid.RAID_ID_BASE)
		{
			// This block process is taking place on a raid map. Send the defender's remaining area.
			Constants.EncodeUnsignedNumber(broadcast_buffer, _land_map.GetRaidData().defender_footprint.area, 2);
		}

		// Broadcast the event to all users with the given block's view area.
		BroadcastEventToViewArea(_land_map, _blockX, _blockY);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastBlockExtendedDataEvent(LandMap _land_map, int _blockX, int _blockY)
	{
		// Get the block's extended data.
		BlockExtData block_ext_data = _land_map.GetBlockExtendedData(_blockX, _blockY, false);

		// Generate and broadcast the event.
		BroadcastBlockExtendedDataEvent(_land_map, _blockX, _blockY, block_ext_data);
	}

	public static void BroadcastBlockExtendedDataEvent(LandMap _land_map, int _blockX, int _blockY, BlockExtData _block_ext_data)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		int cur_time = Constants.GetTime();

		// Encode the block extended data event
		broadcast_buffer.setLength(0);
		Constants.EncodeString(broadcast_buffer, "block_ext");
		Constants.EncodeUnsignedNumber(broadcast_buffer, _blockX, 4);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _blockY, 4);
		Constants.EncodeNumber(broadcast_buffer, _block_ext_data.objectID, 2);
		Constants.EncodeNumber(broadcast_buffer, _block_ext_data.owner_nationID, 4);
		Constants.EncodeNumber(broadcast_buffer, (_block_ext_data.creation_time == -1) ? Constants.LARGE_NEGATIVE_TIME : (_block_ext_data.creation_time - cur_time), 5);
		Constants.EncodeNumber(broadcast_buffer, (_block_ext_data.completion_time == -1) ? -1 : ((_block_ext_data.completion_time < cur_time) ? -2 : (_block_ext_data.completion_time - cur_time)), 4);
		Constants.EncodeNumber(broadcast_buffer, (_block_ext_data.invisible_time == -1) ? -1 : ((_block_ext_data.invisible_time < cur_time) ? -2 : (_block_ext_data.invisible_time - cur_time)), 4);
		Constants.EncodeNumber(broadcast_buffer, (_block_ext_data.capture_time == -1) ? Constants.LARGE_NEGATIVE_TIME : (_block_ext_data.capture_time - cur_time), 5);
		Constants.EncodeNumber(broadcast_buffer, (_block_ext_data.crumble_time == -1) ? -1 : ((_block_ext_data.crumble_time < cur_time) ? -2 : (_block_ext_data.crumble_time - cur_time)), 4);
		Constants.EncodeNumber(broadcast_buffer, _block_ext_data.wipe_nationID, 4);
		Constants.EncodeNumber(broadcast_buffer, (_block_ext_data.wipe_end_time == -1) ? -1 : ((_block_ext_data.wipe_end_time < cur_time) ? -2 : (_block_ext_data.wipe_end_time - cur_time)), 4);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _block_ext_data.wipe_flags, 1);

		// Broadcast the event to all users with the given block's view area.
		BroadcastEventToViewArea(_land_map, _blockX, _blockY);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastClearBlockExtendedDataEvent(LandMap _land_map, int _blockX, int _blockY)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// Encode the clear block extended data event
		broadcast_buffer.setLength(0);
		Constants.EncodeString(broadcast_buffer, "block_ext_clear");
		Constants.EncodeUnsignedNumber(broadcast_buffer, _blockX, 4);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _blockY, 4);

		// Broadcast the event to all users with the given block's view area.
		BroadcastEventToViewArea(_land_map, _blockX, _blockY);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastSalvageEvent(LandMap _land_map, int _blockX, int _blockY)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// Encode the salvage event
		broadcast_buffer.setLength(0);
		Constants.EncodeString(broadcast_buffer, "salvage");
		Constants.EncodeUnsignedNumber(broadcast_buffer, _blockX, 4);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _blockY, 4);

		// Broadcast the event to all users within the given block's view area.
		BroadcastEventToViewArea(_land_map, _blockX, _blockY);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastBuildCompletionEvent(LandMap _land_map, int _blockX, int _blockY)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// Encode the salvage event
		broadcast_buffer.setLength(0);
		Constants.EncodeString(broadcast_buffer, "completion");
		Constants.EncodeUnsignedNumber(broadcast_buffer, _blockX, 4);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _blockY, 4);

		// Broadcast the event to all users within the given block's view area.
		BroadcastEventToViewArea(_land_map, _blockX, _blockY);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastTowerActionEvent(LandMap _land_map, int _blockX, int _blockY, int _build_ID, int _build_type, int _invisible_time, int _trigger_x, int _trigger_y, ArrayList<TargetRecord> _targets, int _triggerNationID)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		int cur_time = Constants.GetTime();

		// Encode the tower action event
		broadcast_buffer.setLength(0);
		Constants.EncodeString(broadcast_buffer, "tower_action");
		Constants.EncodeUnsignedNumber(broadcast_buffer, _blockX, 4);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _blockY, 4);
		Constants.EncodeNumber(broadcast_buffer, _build_ID, 2);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _build_type, 1);
		Constants.EncodeNumber(broadcast_buffer, (_invisible_time == -1) ? -1 : ((_invisible_time < cur_time) ? -2 : (_invisible_time - cur_time)), 4);
		Constants.EncodeUnsignedNumber(broadcast_buffer, Constants.TOWER_ACTION_DURATION, 1);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _trigger_x, 4);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _trigger_y, 4);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _triggerNationID, 4);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _targets.size(), 2);

		//Output.PrintToScreen("BroadcastTowerActionEvent() block " + _blockX + "," + _blockY + " _build_ID: " + _build_ID + ", _build_type: " + _build_type + ", Targets:");

		// Encode each TargetRecord
		Iterator<TargetRecord> targetsIterator = _targets.iterator();
		TargetRecord cur_target;
		while (targetsIterator.hasNext())
		{
			cur_target = targetsIterator.next();
			Constants.EncodeUnsignedNumber(broadcast_buffer, cur_target.x, 4);
			Constants.EncodeUnsignedNumber(broadcast_buffer, cur_target.y, 4);
			Constants.EncodeNumber(broadcast_buffer, cur_target.newNationID, 4);
			Constants.EncodeUnsignedNumber(broadcast_buffer, cur_target.full_hit_points, 2);
			Constants.EncodeUnsignedNumber(broadcast_buffer, cur_target.start_hit_points, 2);
			Constants.EncodeUnsignedNumber(broadcast_buffer, cur_target.end_hit_points, 2);
			Constants.EncodeUnsignedNumber(broadcast_buffer, cur_target.new_cur_hit_points, 2);
			Constants.EncodeUnsignedNumber(broadcast_buffer, cur_target.new_full_hit_points, 2);
			Constants.EncodeUnsignedNumber(broadcast_buffer, (int)(cur_target.hit_points_rate * 100.0f), 3);
			Constants.EncodeUnsignedNumber(broadcast_buffer, cur_target.battle_flags, 2);
			Constants.EncodeNumber(broadcast_buffer, (cur_target.wipe_end_time == -1) ? -1 : ((cur_target.wipe_end_time < cur_time) ? -2 : (cur_target.wipe_end_time - cur_time)), 4);
			Constants.EncodeNumber(broadcast_buffer, cur_target.wipe_nationID, 4);
			Constants.EncodeUnsignedNumber(broadcast_buffer, cur_target.wipe_flags, 1);
			//Output.PrintToScreen("    Target " + cur_target.x + "," + cur_target.y + ", newNationID: " + cur_target.newNationID + ", full_hit_points: " + cur_target.full_hit_points + ", start_hit_points: " + cur_target.start_hit_points + ", end_hit_points: " + cur_target.end_hit_points + ", new_full_hit_points: " + cur_target.new_full_hit_points + ", hit_points_rate: " + cur_target.hit_points_rate);
		}

		if (_land_map.ID >= Raid.RAID_ID_BASE)
		{
			// This block process is taking place on a raid map. Send the defender's remaining area.
			Constants.EncodeUnsignedNumber(broadcast_buffer, _land_map.GetRaidData().defender_footprint.area, 2);
		}

		// Broadcast the event to all users within the given block's view area.
		BroadcastEventToViewArea(_land_map, _blockX, _blockY);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastTriggerInertEvent(LandMap _land_map, int _blockX, int _blockY)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		int cur_time = Constants.GetTime();

		// Encode the tower action event
		broadcast_buffer.setLength(0);
		Constants.EncodeString(broadcast_buffer, "trigger_inert");
		Constants.EncodeUnsignedNumber(broadcast_buffer, _blockX, 4);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _blockY, 4);

		// Broadcast the event to all users within the given block's view area.
		BroadcastEventToViewArea(_land_map, _blockX, _blockY);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastSetTargetEvent(NationData _nationData)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// Encode the set target event
		broadcast_buffer.setLength(0);
		Constants.EncodeString(broadcast_buffer, "set_target");
		Constants.EncodeNumber(broadcast_buffer, _nationData.targetAdvanceID, 3);

		// Broadcast this event to all players logged in to this nation.
		BroadcastEventToNation(_nationData.ID);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastUpdateBarsEvent(int _nationID, int _energy_delta, int _energy_rate_delta, int _energy_burn_rate_delta, int _manpower_delta, int _credits_delta, int _delay)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// Encode the update bars event
		broadcast_buffer.setLength(0);
		GetUpdateBarsEvent(broadcast_buffer, _energy_delta, _energy_rate_delta, _energy_burn_rate_delta, _manpower_delta, _credits_delta, _delay);

		// Broadcast this event to all players logged in to this nation.
		BroadcastEventToNation(_nationID);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastUpdateEvent(int _nationID)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// TESTING
		if (WOCServer.GetLogFlag(Constants.LOG_DEBUG)) Output.PrintToScreen("BroadcastUpdateEvent() for nation " + _nationID);

		// Encode the update event
		broadcast_buffer.setLength(0);
		GetUpdateEvent(broadcast_buffer, _nationID);

		// Broadcast this event to all players logged in to this nation.
		BroadcastEventToNation(_nationID);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastBuyResourceEvent(NationData _nationData)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		broadcast_buffer.setLength(0);

		// Get buy_resources event.
		GetBuyResourceEvent(broadcast_buffer, _nationData);

		// Broadcast this event to all players logged in to this nation.
		BroadcastEventToNation(_nationData.ID);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastMigrationEvent(int _nationID, NationData _nationData)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// Encode the add points
		broadcast_buffer.setLength(0);

		// Encode event ID
		Constants.EncodeString(broadcast_buffer, "migration");

		// Encode time until next free migration
		Constants.EncodeUnsignedNumber(broadcast_buffer, Math.max(0, _nationData.prev_free_migration_time + Constants.FREE_MIGRATION_PERIOD - Constants.GetTime()), 6);

		// Broadcast this event to all players logged in to this nation.
		BroadcastEventToNation(_nationID);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastQuestStatusEvent(int _nationID, QuestRecord _quest_record, int _delay)
	{
		// If none of the given nation's players are online, do nothing.
		if (WOCServer.GetNationNumUsersOnline(_nationID) == 0) {
			return;
		}

		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// Encode the event
		broadcast_buffer.setLength(0);
		Constants.EncodeString(broadcast_buffer, "quest_status");
		Constants.EncodeUnsignedNumber(broadcast_buffer, _quest_record.ID, 2);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _quest_record.cur_amount, 5);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _quest_record.completed, 1);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _quest_record.collected, 1);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _delay, 1);

		// Broadcast this event to all players logged in to this nation.
		BroadcastEventToNation(_nationID);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastSetMapFlagEvent(int _nationID, int _blockX, int _blockY, String _text)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// Encode the set map flag event
		broadcast_buffer.setLength(0);
		Constants.EncodeString(broadcast_buffer, "set_map_flag");
		Constants.EncodeUnsignedNumber(broadcast_buffer, _blockX, 4);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _blockY, 4);
		Constants.EncodeString(broadcast_buffer, _text);

		// Broadcast this event to all players logged in to this nation.
		BroadcastEventToNation(_nationID);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastDeleteMapFlagEvent(int _nationID, int _blockX, int _blockY)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// Encode the delete map flag event
		broadcast_buffer.setLength(0);
		Constants.EncodeString(broadcast_buffer, "delete_map_flag");
		Constants.EncodeUnsignedNumber(broadcast_buffer, _blockX, 4);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _blockY, 4);

		// Broadcast this event to all players logged in to this nation.
		BroadcastEventToNation(_nationID);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastAddObjectEvent(int _nationID, int _blockX, int _blockY, int _objectID)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// Encode the add object event
		broadcast_buffer.setLength(0);
		GetAddObjectEvent(broadcast_buffer, _blockX, _blockY, _objectID);

		// Broadcast this event to all players logged in to this nation.
		BroadcastEventToNation(_nationID);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastRemoveObjectEvent(int _nationID, int _blockX, int _blockY)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// Encode the remove object event
		broadcast_buffer.setLength(0);
		GetRemoveObjectEvent(broadcast_buffer, _blockX, _blockY);

		// Broadcast this event to all players logged in to this nation.
		BroadcastEventToNation(_nationID);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastBuildCountEvent(int _nationID, int _buildID, int _count)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// Encode the build count event
		broadcast_buffer.setLength(0);
		GetBuildCountEvent(broadcast_buffer, _buildID, _count);

		// Broadcast this event to all players logged in to this nation.
		BroadcastEventToNation(_nationID);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastAddXPEvent(int _nationID, int _xp_delta, int _xp, int _userID, int _block_x, int _block_y, int _delay)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// Encode the add points
		broadcast_buffer.setLength(0);
		Constants.EncodeString(broadcast_buffer, "add_xp");
		Constants.EncodeNumber(broadcast_buffer, _xp_delta, 5);
		Constants.EncodeNumber(broadcast_buffer, _xp, 5);
		Constants.EncodeNumber(broadcast_buffer, _userID, 4);
		Constants.EncodeNumber(broadcast_buffer, _block_x, 3);
		Constants.EncodeNumber(broadcast_buffer, _block_y, 3);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _delay, 1);

		// Broadcast this event to all players logged in to this nation.
		BroadcastEventToNation(_nationID);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastSetLevelEvent(NationData _nationData, int _delay)
	{
		BroadcastSetLevelEvent(_nationData.ID, _nationData.level,
			Constants.XP_PER_LEVEL[_nationData.level - _nationData.GetRebirthLevelBonus()],
			Constants.XP_PER_LEVEL[_nationData.level + 1 - _nationData.GetRebirthLevelBonus()],
			(int)_nationData.xp,
			_nationData.rebirth_count,
			_nationData.GetRebirthLevelBonus(),
			Constants.REBIRTH_AVAILABLE_LEVEL + _nationData.GetRebirthLevelBonus(),
			_nationData.advance_points, (int)(_nationData.game_money), _delay);
	}

	public static void BroadcastSetLevelEvent(int _nationID, int _level, int _level_xp_threshold, int _next_level_xp_threshold, int _xp, int _rebirth_count, int _rebirth_level_bonus, int _rebirth_available_level, int _advance_points, int _game_money, int _delay)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// Determine the nation's map position limit, based on its level.
		LandMap land_map = DataManager.GetLandMap(Constants.MAINLAND_MAP_ID, false);
		int map_position_limit = land_map.MaxLevelLimitToPosX(_level) - 1;
		int map_position_limit_next_level = land_map.MaxLevelLimitToPosX(_level + 1) - 1;
		int map_position_eastern_limit = land_map.MaxLevelLimitToPosX(land_map.GetEasternLevelLimit(_level));

		// Encode the event
		broadcast_buffer.setLength(0);
		Constants.EncodeString(broadcast_buffer, "set_level");
		Constants.EncodeUnsignedNumber(broadcast_buffer, _level, 2);
		Constants.EncodeUnsignedNumber(broadcast_buffer, Math.min(Constants.MaxEncodableUnsignedNumber(5), _level_xp_threshold), 5);
		Constants.EncodeUnsignedNumber(broadcast_buffer, Math.min(Constants.MaxEncodableUnsignedNumber(5), _next_level_xp_threshold), 5);
		Constants.EncodeUnsignedNumber(broadcast_buffer, Math.min(Constants.MaxEncodableUnsignedNumber(5), _xp), 5);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _rebirth_count, 2);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _rebirth_level_bonus, 2);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _rebirth_available_level, 2);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _advance_points, 2);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _game_money, 4);
		Constants.EncodeNumber(broadcast_buffer, map_position_limit, 3);
		Constants.EncodeNumber(broadcast_buffer, map_position_limit_next_level, 3);
		Constants.EncodeNumber(broadcast_buffer, map_position_eastern_limit, 3);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _delay, 1);

		// Broadcast this event to all players logged in to this nation.
		BroadcastEventToNation(_nationID);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastTechnologiesEvent(int _nationID)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// Encode the event
		broadcast_buffer.setLength(0);
		GetTechnologiesEvent(broadcast_buffer, _nationID);

		// Broadcast this event to all players logged in to this nation.
		BroadcastEventToNation(_nationID);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastNationFlagsEvent(int _nationID, int _flags)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// Encode the add points
		broadcast_buffer.setLength(0);
		Constants.EncodeString(broadcast_buffer, "nation_flags");
		Constants.EncodeUnsignedNumber(broadcast_buffer, _flags, 4);

		// Broadcast this event to all players logged in to this nation.
		BroadcastEventToNation(_nationID);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastNationPasswordEvent(int _nationID, String _password)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// Encode the add points
		broadcast_buffer.setLength(0);
		Constants.EncodeString(broadcast_buffer, "nation_password");
		Constants.EncodeString(broadcast_buffer, _password);

		// Broadcast this event to all players logged in to this nation.
		BroadcastEventToNation(_nationID);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastMembersEvent(int _nationID)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// Encode event
		broadcast_buffer.setLength(0);
		GetMembersEvent(broadcast_buffer, _nationID);

		// Broadcast this event to all players logged in to this nation.
		BroadcastEventToNation(_nationID);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastAlliancesEvent(int _nationID)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// Encode event
		broadcast_buffer.setLength(0);
		GetAlliancesEvent(broadcast_buffer, _nationID);

		// Broadcast this event to all players logged in to this nation.
		BroadcastEventToNation(_nationID);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastNewMessageEvent(int _nationID, int _delay, MessageData _message_data)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// Encode event
		broadcast_buffer.setLength(0);
		GetNewMessageEvent(broadcast_buffer, _delay, _message_data);

		// Broadcast this event to all players logged in to this nation.
		BroadcastEventToNation(_nationID);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastMessageEvent(int _nationID, ClientString _message)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// Encode event
		broadcast_buffer.setLength(0);
		GetMessageEvent(broadcast_buffer, _message);

		// Broadcast this event to all players logged in to this nation.
		BroadcastEventToNation(_nationID);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastStatsEvent(int _nationID, int _delay)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// Encode event
		broadcast_buffer.setLength(0);
		GetStatsEvent(broadcast_buffer, _nationID, _delay);

		// Broadcast this event to all players logged in to this nation.
		BroadcastEventToNation(_nationID);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastAddTechnologyEvent(int _nationID, int _techID, int _expire_time, int _delay)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// Encode event
		broadcast_buffer.setLength(0);
		GetAddTechnologyEvent(broadcast_buffer, _techID, _expire_time, _delay);

		// Broadcast this event to all players logged in to this nation.
		BroadcastEventToNation(_nationID);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastRemoveTechnologyEvent(int _nationID, int _techID)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// Encode event
		broadcast_buffer.setLength(0);
		GetRemoveTechnologyEvent(broadcast_buffer, _techID);

		// Broadcast this event to all players logged in to this nation.
		BroadcastEventToNation(_nationID);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastDiscoveryEvent(int _nationID, int _delay, int _manpower_added, int _energy_added, NationData _targetNationData, int _advanceID, int _duration, int _xp)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// Encode event
		broadcast_buffer.setLength(0);
		Constants.EncodeString(broadcast_buffer, "discovery");
		Constants.EncodeUnsignedNumber(broadcast_buffer, _delay, 2);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _manpower_added, 5);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _energy_added, 5);
		Constants.EncodeString(broadcast_buffer, (_targetNationData == null) ? "" : _targetNationData.name);
		Constants.EncodeNumber(broadcast_buffer, _advanceID, 2);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _duration, 5);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _xp, 3);

		// Broadcast this event to all players logged in to this nation.
		BroadcastEventToNation(_nationID);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastAllyNationDataEvent(NationData _nationData)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		int curID, cur_index;
		NationData cur_nation_data;
		int size = _nationData.alliances_active.size();

		// Encode event
		broadcast_buffer.setLength(0);
		Constants.EncodeString(broadcast_buffer, "ally_nation_data");
		Constants.EncodeUnsignedNumber(broadcast_buffer, size, 2);

		// Encode the nation data of each of the given nation's allies.
		for (cur_index = 0; cur_index < size; cur_index++)
		{
			curID = _nationData.alliances_active.get(cur_index);
			cur_nation_data = (NationData)DataManager.GetData(Constants.DT_NATION, curID, false);

			// Encode ID and nation data.
			Constants.EncodeUnsignedNumber(broadcast_buffer, curID, 4);
			Display.EncodeNationData(broadcast_buffer, cur_nation_data);
		}

		// Broadcast this event to all players logged in to this nation.
		BroadcastEventToNation(_nationData.ID);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastCaptureStorageEvent(int _nationID, int _delay, int _resource_added, NationData _targetNationData, int _buildID)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// Encode event
		broadcast_buffer.setLength(0);
		Constants.EncodeString(broadcast_buffer, "capture_storage");
		Constants.EncodeUnsignedNumber(broadcast_buffer, _delay, 2);
		Constants.EncodeUnsignedNumber(broadcast_buffer, _resource_added, 5);
		Constants.EncodeString(broadcast_buffer, (_targetNationData == null) ? "" : _targetNationData.name);
		Constants.EncodeNumber(broadcast_buffer, _buildID, 2);

		// Broadcast this event to all players logged in to this nation.
		BroadcastEventToNation(_nationID);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastHistoricalExtentEvent(int _nationID, Footprint _footprint)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// Encode the event
		broadcast_buffer.setLength(0);
		Constants.EncodeString(broadcast_buffer, "hist_extent");
		Constants.EncodeNumber(broadcast_buffer, _footprint.min_x0, 3);
		Constants.EncodeNumber(broadcast_buffer, _footprint.min_y0, 3);
		Constants.EncodeNumber(broadcast_buffer, _footprint.max_x1, 3);
		Constants.EncodeNumber(broadcast_buffer, _footprint.max_y1, 3);

		// Broadcast this event to all players logged in to this nation.
		BroadcastEventToNation(_nationID);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastNationTournamentStatusEvent(NationData _nationData, int _delay)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// Encode event
		broadcast_buffer.setLength(0);
		GetNationTournamentStatusEvent(broadcast_buffer, _nationData, _delay);

		// Broadcast this event to all players logged in to this nation.
		BroadcastEventToNation(_nationData.ID);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastGlobalTournamentStatusEvent()
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// Encode the event
		broadcast_buffer.setLength(0);
		GetGlobalTournamentStatusEvent(broadcast_buffer);

		// Broadcast this event to all clients in the game.
		BroadcastEventToAllInGame();

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastRaidStatusEvent(NationData _nationData, int _delay)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// Encode event
		broadcast_buffer.setLength(0);
		GetRaidStatusEvent(broadcast_buffer, _nationData, _delay);

		// Broadcast this event to all players logged in to this nation.
		BroadcastEventToNation(_nationData.ID);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastRaidLogEntryEvent(NationData _nationData, RaidData _raidData, boolean _attack)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// Encode event
		broadcast_buffer.setLength(0);
		GetRaidLogEntryEvent(broadcast_buffer, _raidData, _attack);

		// Broadcast this event to all players logged in to this nation.
		BroadcastEventToNation(_nationData.ID);

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastTechPricesEvent()
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// Encode the event
		broadcast_buffer.setLength(0);
		GetTechPricesEvent(broadcast_buffer);

		// Broadcast this event to all clients in the game.
		BroadcastEventToAllInGame();

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastNationDataEvent(NationData _nationData)
	{
		// Acquire the broadcast buffer
		broadcast_buffer_semaphore.acquire();

		// Encode event
		broadcast_buffer.setLength(0);
		GetNationDataEvent(broadcast_buffer, _nationData);

		// Broadcast this event to all clients in the game.
		BroadcastEventToAllInGame();

		// Release the broadcast buffer
		broadcast_buffer_semaphore.release();
	}

	public static void BroadcastEventToViewArea(LandMap _land_map, int _blockX, int _blockY)
	{
		// Add end marker to output string
		Constants.EncodeString(broadcast_buffer, "end");

		// NULL terminate output string so that it can be sent.
		broadcast_buffer.append('\u0000');

		// Get the event as a string
		String event_string = broadcast_buffer.toString();

		// Send the event to each of the given block's viewers.
		BroadcastToViewArea(event_string, _land_map, _blockX, _blockY);
	}

	public static void BroadcastEventToNation(int _nationID)
	{
		// Add end marker to output string
		Constants.EncodeString(broadcast_buffer, "end");

		// NULL terminate output string so that it can be sent.
		broadcast_buffer.append('\u0000');

		// Get the event as a string
		String event_string = broadcast_buffer.toString();

		// Broadcast this event to every player logged in to the given nation.
		BroadcastToNation(event_string, _nationID);
	}

	public static void BroadcastEventToAllInGame()
	{
		// Add end marker to output string
		Constants.EncodeString(broadcast_buffer, "end");

		// NULL terminate output string so that it can be sent.
		broadcast_buffer.append('\u0000');

		// Get the event as a string
		String event_string = broadcast_buffer.toString();

		// Broadcast this event to all clients in the game.
		BroadcastToAllInGame(event_string);
	}

	public static void BroadcastToViewArea(String _event, LandMap _land_map, int _blockX, int _blockY)
	{
		// Get the viewer list for the given block in the given land map
		LinkedList<ClientThread> viewer_list = _land_map.viewers[_blockX / Constants.DISPLAY_CHUNK_SIZE][_blockY / Constants.DISPLAY_CHUNK_SIZE];

		// Send the event to each of the given block's viewers.
		for (ClientThread cur_client_thread : viewer_list) {
			if (cur_client_thread == null) Output.PrintToScreen("NULL client thread in  _land_map.viewers[] for block " + _blockX + "," + _blockY); // TESTING
			cur_client_thread.SendNow(_event);
		}
	}

	public static void BroadcastToNation(String _event, int _nationID)
	{
		WOCServer.NationRecord nation_record = WOCServer.nation_table.get(_nationID);

		if (nation_record != null)
		{
			ClientThread client_thread;
			for (Map.Entry<Integer,ClientThread> user_entry : nation_record.users.entrySet())
			{
				client_thread = (ClientThread)(user_entry.getValue());

				if (client_thread.UserIsLoggedIn() == false) {
					continue;
				}

				// Send the event to each logged in player belonging to the nation with the given ID.
				client_thread.SendNow(_event);
			}
		}
	}

	public static void BroadcastToAllOnChatList(String _event, int _nationID)
	{
		// Broadcast to the nation whose chat list it is.
		OutputEvents.BroadcastToNation(_event, _nationID);

		// Broadcast to each nation on that chat list.
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationID, false);
		if (nationData != null)
		{
			for (int cur_recipient_index = 0; cur_recipient_index < nationData.chat_list.size(); cur_recipient_index++)
			{
				// Send the chat text to the current nation on the chat list.
				OutputEvents.BroadcastToNation(_event, nationData.chat_list.get(cur_recipient_index));
			}
		}
	}

	public static void BroadcastToAllInGame(String _event)
  {
    // Send the given event to each online user.
		for (Map.Entry<Integer, ClientThread> entry : WOCServer.client_table.entrySet())
    {
			ClientThread client_thread = (ClientThread)(entry.getValue());

      if (client_thread == null) {
        continue;
      }

      if (client_thread.UserIsInGame() == false) {
        continue;
      }

      client_thread.SendNow(_event);
    }
  }

	public static void BroadcastToAllAdmins(String _event)
	{
		for (ClientThread cur_admin_thread : WOCServer.admin_clients)
		{
			if (cur_admin_thread.UserIsInGame() == false) {
        continue;
      }

      cur_admin_thread.SendNow(_event);
		}
	}

	public static void SendToUser(String _event, int _userID)
	{
		// Get the user's client thread
		ClientThread targetClientThread = WOCServer.GetClientThread(_userID);

		if (targetClientThread == null) {
        return;
    }

		targetClientThread.SendNow(_event);
	}

	public static void SendToClient(String _event, ClientThread _client_thread)
	{
		if (_client_thread == null) {
        return;
    }

		_client_thread.SendNow(_event);
	}
}
