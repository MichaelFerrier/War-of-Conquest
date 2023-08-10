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

import java.sql.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.*;
import WOCServer.*;

public class TournamentData extends BaseData
{
	// Tournament constants
	public static boolean TOURNAMENT_ENABLED = false; // If disabled, a new tournament will not start
	public static int START_JULIAN_DOW = Calendar.SUNDAY; // Tournament starts on Sunday
	public static int NUM_ENROLLMENT_DAYS = 2;
	public static int FIRST_ELIMINATION_DAY = 2; // First elimination takes place at start of day 2 (starting at 0).
	public static int LAST_ELIMINATION_DAY = 6; // Final elimination takes place at start of day 6 (starting at 0).
	public static int NUM_TOURNAMENT_DAYS = 7;
	public static int NUM_BREAK_DAYS = 0; // No break between tournaments
	public static float ELIMINATION_FRACTION = 0.33f;
	public static int STARTING_POTENTIAL_TROPHIES = 1000;
	public static int UNDO_CAPTURE_PERIOD = 60 * 5; // 5 minutes

	// Status constants
	public static int TOURNAMENT_STATUS_UNDEF = -1;
	public static int TOURNAMENT_STATUS_ACTIVE = 0;
	public static int TOURNAMENT_STATUS_BREAK = 1;

	public static String db_table_name = "Tournament";
	public static int VERSION = 1;

	// Static StringBuffers
	public static int CONTENDERS_HTML_LENGTH = 100000;
	public static StringBuffer contenders_html = new StringBuffer(CONTENDERS_HTML_LENGTH);

	public static TournamentData instance = null;

	// The tournament's list of contenders.
	ArrayList<ContenderData> contenders = new ArrayList<ContenderData>();

	// Information about the current tournament
	int start_day = -1;
	int status = -1;
	int prev_update_day = -1;
	int num_active_contenders = 0;
	int enrollment_closes_time = -1;
	int next_elimination_time = -1;
	int end_time = -1;

	// Winners of previous tournament
	int prev_1st_nationID = -1;
	int prev_2nd_nationID = -1;
	int prev_3rd_nationID = -1;

	public static void StartUp()
	{
		// Load the TournamentData singleton instance.
		instance = (TournamentData)DataManager.GetData(Constants.DT_TOURNAMENT, Constants.TOURNAMENT_DATA_ID, false);
	}

	public TournamentData(int _ID)
	{
		super(Constants.DT_TOURNAMENT, _ID);
	}

	public boolean ReadData()
	{
		int file_version;
		boolean result = true;

		String sql = "SELECT " +
		"version, " +
		"contenders, " +
		"start_day, " +
		"status, " +
		"prev_update_day, " +
		"num_active_contenders, " +
		"enrollment_closes_time, " +
		"next_elimination_time, " +
		"end_time, " +
		"prev_1st_nationID, " +
		"prev_2nd_nationID, " +
		"prev_3rd_nationID " +
		"FROM " + db_table_name + " where ID= '" + ID + "'";

		try {
			// Create statement for use with the DB.
			Statement stmt = db.createStatement();

			// Get the result set for the sql query
			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				file_version  = rs.getInt("version");
				contenders = ContenderData.JSONToContenderDataArray(rs.getString("contenders"));
				start_day  = rs.getInt("start_day");
				status  = rs.getInt("status");
				prev_update_day  = rs.getInt("prev_update_day");
				num_active_contenders  = rs.getInt("num_active_contenders");
				enrollment_closes_time  = rs.getInt("enrollment_closes_time");
				next_elimination_time  = rs.getInt("next_elimination_time");
				end_time  = rs.getInt("end_time");
				prev_1st_nationID  = rs.getInt("prev_1st_nationID");
				prev_2nd_nationID  = rs.getInt("prev_2nd_nationID");
				prev_3rd_nationID  = rs.getInt("prev_3rd_nationID");
			} else {
				result = false;
			}

			// Close statement and result set (must be done explicitly to avoid memory leak).
			try { if (rs != null) rs.close(); } catch (Exception e) {};
	    try { if (stmt != null) stmt.close(); } catch (Exception e) {};
		}
	  catch(Exception e)
		{
      Output.PrintToScreen("Couldn't fetch object with ID " + ID + " from table '" + db_table_name + "'.");
			Output.PrintException(e);
			result = false;
		}

		return result;
	}

	public void WriteData()
	{
		String sql = "UPDATE " + db_table_name + " SET " +
		"version = '" + VERSION + "', " +
		"contenders = '" + PrepStringForMySQL(ContenderData.ContenderDataArrayToJSON(contenders)) + "', " +
		"start_day = '" + start_day + "', " +
		"status = '" + status + "', " +
		"prev_update_day = '" + prev_update_day + "', " +
		"num_active_contenders = '" + num_active_contenders + "', " +
		"enrollment_closes_time = '" + enrollment_closes_time + "', " +
		"next_elimination_time = '" + next_elimination_time + "', " +
		"end_time = '" + end_time + "', " +
		"prev_1st_nationID = '" + prev_1st_nationID + "', " +
		"prev_2nd_nationID = '" + prev_2nd_nationID + "', " +
		"prev_3rd_nationID = '" + prev_3rd_nationID + "' " +
		"WHERE ID= '" + ID + "'";

		try {
			// Create statement for use with the DB.
			Statement stmt = db.createStatement();

			// Execute the sql query
			stmt.executeUpdate(sql);
			stmt.close();
		}
	  catch(Exception e) {
      Output.PrintToScreen("Could not store object with ID " + ID + " in table '" + db_table_name + "'. Message: " + e.getMessage());
		}
	}

	public static void InitDBTable()
	{
		// Create the user data table, if it doesn't yet exist.
		CreateTable(db, db_table_name);

		// Attempt to create each column
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD contenders MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD start_day INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD status INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD prev_update_day INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD num_active_contenders INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD enrollment_closes_time INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD next_elimination_time INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD end_time INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD prev_1st_nationID INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD prev_2nd_nationID INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD prev_3rd_nationID INT", true, false);
	}

	public static void DeleteDBTable()
	{
		// Attempt to delete the DB table
		ExecuteUpdate(db, "DROP TABLE " + db_table_name, true, true);
	}

	public void SetTournamentDay(int _tournament_day)
	{
		// Determine the current day.
		int current_day = Constants.GetTime() / Constants.SECONDS_PER_DAY;

		// Determine start day.
		start_day = current_day - _tournament_day;

		// Determine status.
		if ((_tournament_day < 0) || (_tournament_day >= (NUM_TOURNAMENT_DAYS + NUM_BREAK_DAYS)))
		{
			status = TOURNAMENT_STATUS_UNDEF;
			end_time = -1;
		}
		else
		{
			status = TOURNAMENT_STATUS_ACTIVE;
			end_time = Constants.GetTime() + ((NUM_TOURNAMENT_DAYS - _tournament_day) * Constants.SECONDS_PER_DAY);
			// Allow tournament update to put the tournament into break status if necessary, that way it will award prizes.
		}

		// Determine enrollment_closes_time and next_elimination_time.
		DetermineTimes(_tournament_day);

		// Broadcast global tournament status to all clients.
		OutputEvents.BroadcastGlobalTournamentStatusEvent();

		// Mark the tournament data to be updated.
		DataManager.MarkForUpdate(this);

		// Output info about tournament status.
		Output.PrintToScreen("Tournament start_day: " + start_day + ", current_day: " + current_day + ", status: " + status + ", num_active_contenders: " + num_active_contenders + ", enrollment_closes_time: " + ((enrollment_closes_time == -1) ? "none" : (((enrollment_closes_time - Constants.GetTime()) / Constants.SECONDS_PER_HOUR) + "hours")) + ", next_elimination_time: " + ((next_elimination_time == -1) ? "none" : (((next_elimination_time - Constants.GetTime()) / Constants.SECONDS_PER_HOUR) + "hours")) + ".");
	}

	public void UpdateForDay()
	{
		int i;

		// Determine the current day of the week.
		int current_day = Constants.GetTime() / Constants.SECONDS_PER_DAY;
		Calendar calendar = Calendar.getInstance();
		int julian_dow = calendar.get(Calendar.DAY_OF_WEEK);

		// Determine the total length of a tournament cycle, including break.
		int total_cycle_days = NUM_TOURNAMENT_DAYS + NUM_BREAK_DAYS;

		if (start_day != -1)
		{
			int tournament_day_index = current_day - start_day;

			if (status != TOURNAMENT_STATUS_BREAK)
			{
				if (tournament_day_index >= NUM_TOURNAMENT_DAYS)
				{
					Output.PrintToScreen("Tournament is over. Awarding prizes.");

					// Clear the previous latest_tournament_ranks list.
					RanksData.instance.ranks_nation_latest_tournament.Clear();

					// Remove the tournament winner flags from the previous tournament's winning nations.
					ClearTournamentWinnerFlags(prev_1st_nationID);
					ClearTournamentWinnerFlags(prev_2nd_nationID);
					ClearTournamentWinnerFlags(prev_3rd_nationID);

					// Clear the records of the previous winning nations.
					prev_1st_nationID = prev_2nd_nationID = prev_3rd_nationID = -1;

					// Iterate through all contenders....
					for (i = 0; i < contenders.size(); i++)
					{
						// Get the current contender nation's data.
						NationData curNationData = contenders.get(i).GetNationData();

						if (curNationData == null) {
							continue;
						}

						// Add the nation's number of trophies in this tournament to their tournament trophies history.
						curNationData.tournament_trophies_history += (curNationData.trophies_available + curNationData.trophies_banked);
						curNationData.tournament_trophies_history_monthly += (curNationData.trophies_available + curNationData.trophies_banked);

						// Set the nation's appropriate value in the ranks_nation_latest_tournament, ranks_nation_tournament_trophies, and ranks_nation_tournament_trophies_monthly lists.
						RanksData.instance.ranks_nation_latest_tournament.UpdateRanks(curNationData.ID, curNationData.name, (int)(curNationData.trophies_available + curNationData.trophies_banked + 0.5f), Constants.NUM_GLOBAL_PRIZE_RANKS, false);
						RanksData.instance.ranks_nation_tournament_trophies.UpdateRanks(curNationData.ID, curNationData.name, curNationData.tournament_trophies_history, Constants.NUM_GLOBAL_PRIZE_RANKS, false);
						RanksData.instance.ranks_nation_tournament_trophies_monthly.UpdateRanks(curNationData.ID, curNationData.name, curNationData.tournament_trophies_history_monthly, Constants.NUM_GLOBAL_PRIZE_RANKS, false);

						// Award prizes
						if (i == 0)
						{
							// First place winner.
							Money.AddGameMoney(curNationData, 2000, Money.Source.FREE);
							Gameplay.AddXP(curNationData, 1000000, -1, -1, -1, true, true, 0, Constants.XP_TOURNAMENT);
							Gameplay.ChangeRebirthCountdown(curNationData, 100);
							Comm.SendReport(curNationData.ID, ClientString.Get("svr_report_tournament_top_prize", "nation_name", curNationData.name, "rank", String.valueOf(i + 1), "num_contenders", String.valueOf(contenders.size()), "num_credits", String.valueOf(2000), "num_xp", String.valueOf(1000000), "num_countdown", String.valueOf(100)), 0); // "The tournament has ended, and {nation_name} ranked #{rank} out of {num_contenders}! You've received the prize of {num_credits} credits, {num_xp} XP, and {num_countdown} added to your countdown to rebirth!"
							curNationData.SetFlags(curNationData.flags | Constants.NF_TOURNAMENT_FIRST_PLACE);
							prev_1st_nationID = curNationData.ID;
							Output.PrintToScreen("First place prize awarded to nation " + curNationData.name);
						}
						else if (i == 1)
						{
							// Second place winner.
							Money.AddGameMoney(curNationData, 1000, Money.Source.FREE);
							Gameplay.AddXP(curNationData, 400000, -1, -1, -1, true, true, 0, Constants.XP_TOURNAMENT);
							Gameplay.ChangeRebirthCountdown(curNationData, 40);
							Comm.SendReport(curNationData.ID, ClientString.Get("svr_report_tournament_top_prize", "nation_name", curNationData.name, "rank", String.valueOf(i + 1), "num_contenders", String.valueOf(contenders.size()), "num_credits", String.valueOf(1000), "num_xp", String.valueOf(400000), "num_countdown", String.valueOf(40)), 0); // "The tournament has ended, and {nation_name} ranked #{rank} out of {num_contenders}! You've received the prize of {num_credits} credits, {num_xp} XP, and {num_countdown} added to your countdown to rebirth!"
							curNationData.SetFlags(curNationData.flags | Constants.NF_TOURNAMENT_SECOND_PLACE);
							prev_2nd_nationID = curNationData.ID;
							Output.PrintToScreen("Second place prize awarded to nation " + curNationData.name);
						}
						else if (i == 2)
						{
							// Third place winner.
							Money.AddGameMoney(curNationData, 500, Money.Source.FREE);
							Gameplay.AddXP(curNationData, 200000, -1, -1, -1, true, true, 0, Constants.XP_TOURNAMENT);
							Gameplay.ChangeRebirthCountdown(curNationData, 20);
							Comm.SendReport(curNationData.ID, ClientString.Get("svr_report_tournament_top_prize", "nation_name", curNationData.name, "rank", String.valueOf(i + 1), "num_contenders", String.valueOf(contenders.size()), "num_credits", String.valueOf(500), "num_xp", String.valueOf(200000), "num_countdown", String.valueOf(20)), 0); // "The tournament has ended, and {nation_name} ranked #{rank} out of {num_contenders}! You've received the prize of {num_credits} credits, {num_xp} XP, and {num_countdown} added to your countdown to rebirth!"
							curNationData.SetFlags(curNationData.flags | Constants.NF_TOURNAMENT_THIRD_PLACE);
							prev_3rd_nationID = curNationData.ID;
							Output.PrintToScreen("Third place prize awarded to nation " + curNationData.name);
						}
						else if (curNationData.tournament_active)
						{
							// Prize for surviving all elimination rounds.
							Money.AddGameMoney(curNationData, 250, Money.Source.FREE);
							Gameplay.AddXP(curNationData, 100000, -1, -1, -1, true, true, 0, Constants.XP_TOURNAMENT);
							Gameplay.ChangeRebirthCountdown(curNationData, 10);
							Comm.SendReport(curNationData.ID, ClientString.Get("svr_report_tournament_survival_prize", "nation_name", curNationData.name, "rank", String.valueOf(i + 1), "num_contenders", String.valueOf(contenders.size()), "num_credits", String.valueOf(250), "num_xp", String.valueOf(100000), "num_countdown", String.valueOf(10)), 0); // "The tournament has ended, and {nation_name} ranked #{rank} out of {num_contenders}. Because you survived all of the elimination rounds, you've been awarded {num_credits} credits, {num_xp} XP, and {num_countdown} added to your countdown to rebirth!"
							Output.PrintToScreen("Third place prize awarded to nation " + curNationData.name);
						}
						else
						{
							// Send message reporting that tournament is over, and final rank.
							Comm.SendReport(curNationData.ID, ClientString.Get("svr_report_tournament_ended", "nation_name", curNationData.name, "rank", String.valueOf(i + 1), "num_contenders", String.valueOf(contenders.size())), 0); // "The tournament has ended, and {nation_name} ranked #{rank} out of {num_contenders}."
						}

						// Mark the nation to be updated.
						DataManager.MarkForUpdate(curNationData);
					}

					// The tournament is over, it's now on break until the next tournament begins.
					status = TOURNAMENT_STATUS_BREAK;
				}
				else
				{
					if ((tournament_day_index >= FIRST_ELIMINATION_DAY) && (tournament_day_index <= LAST_ELIMINATION_DAY) && (current_day != prev_update_day))
					{
						// Eliminate lowest scoring contenders.

						// Determine how many contenders will be allowed to remain active after this elimination.
						int new_active_contenders = (int)((float)num_active_contenders * (1f - ELIMINATION_FRACTION) + 0.5f);

						Output.PrintToScreen("Eliminating " + (num_active_contenders - new_active_contenders) + " of " + num_active_contenders + " active tournament contenders.");

						// Eliminate the appropriate number of lower ranking contenders.
						num_active_contenders = 0;
						for (i = 0; i < contenders.size(); i++)
						{
							if (contenders.get(i).GetNationData().tournament_active)
							{
								if (num_active_contenders < new_active_contenders)
								{
									num_active_contenders++;
								}
								else
								{
									// Get the current contender's NationData.
									NationData nationData = contenders.get(i).GetNationData();

									// Record that this nation is no longer active in the tournament.
									nationData.tournament_active = false;
									Output.PrintToScreen("Eliminated nation '" + nationData.name + "' from tournament. Rank #" + (i+1) + ".");

									// Broadcast to this nation its new tournament status (eliminated).
									OutputEvents.BroadcastNationTournamentStatusEvent(nationData, 0);
								}
							}
						}
					}
				}
			}

			if (status == TOURNAMENT_STATUS_BREAK)
			{
				if (tournament_day_index >= (NUM_TOURNAMENT_DAYS + NUM_BREAK_DAYS))
				{
					// This tournament cycle is finished.
					start_day = -1;
					status = TOURNAMENT_STATUS_UNDEF;
					Output.PrintToScreen("Tournament cycle is finished.");
				}
			}
		}

		// If no tournament is in progress, and today is the day of week to start a new tournament, start a new tournament.
		if (TOURNAMENT_ENABLED && (start_day == -1) && (julian_dow == START_JULIAN_DOW))
		{
			// Start a new tournament.
			start_day = current_day;
			status = TOURNAMENT_STATUS_ACTIVE;
			end_time = Constants.GetTime() + (NUM_TOURNAMENT_DAYS * Constants.SECONDS_PER_DAY);
			contenders.clear();
			Output.PrintToScreen("New tournament has started.");

			// Get the mainland land map data
			LandMap land_map = DataManager.GetLandMap(Constants.MAINLAND_MAP_ID, false);

			// Clear any tournament records (that are from the previous tournament) from all of the map's blocks.
			for (int y = 0; y < land_map.height; y++)
			{
				for (int x = 0; x < land_map.width; x++)
				{
					land_map.GetBlockData(x, y).tournament_record = null;
				}
			}
		}

		// Determine enrollment_closes_time and next_elimination_time.
		int tournament_day_index = current_day - start_day;
		DetermineTimes(tournament_day_index);

		// Record previous day that tournament status was updated.
		prev_update_day = current_day;

		// Write tournament ranks list.
		PublishTournamentRanksList();

		// Broadcast global tournament status to all clients.
		OutputEvents.BroadcastGlobalTournamentStatusEvent();

		// Mark the tournament data to be updated.
		DataManager.MarkForUpdate(this);

		// Output info about tournament status.
		Output.PrintToScreen("Tournament start_day: " + start_day + ", current_day: " + current_day + ", status: " + status + ", num_active_contenders: " + num_active_contenders + ", enrollment_closes_time: " + ((enrollment_closes_time == -1) ? "none" : (((enrollment_closes_time - Constants.GetTime()) / Constants.SECONDS_PER_HOUR) + "hours")) + ", next_elimination_time: " + ((next_elimination_time == -1) ? "none" : (((next_elimination_time - Constants.GetTime()) / Constants.SECONDS_PER_HOUR) + "hours")) + ".");
	}

	public void DetermineTimes(int _tournament_day_index)
	{
		if (start_day == -1)
		{
			enrollment_closes_time = -1;
			next_elimination_time = -1;
		}
		else
		{
			// Record how long until enrollment closes.
			enrollment_closes_time = Constants.GetTime() + Math.max(0, (NUM_ENROLLMENT_DAYS - _tournament_day_index) * Constants.SECONDS_PER_DAY);

			// Record how long until next elimination takes place.
			if (_tournament_day_index < FIRST_ELIMINATION_DAY) {
				next_elimination_time = Constants.GetTime() + ((FIRST_ELIMINATION_DAY - _tournament_day_index) * Constants.SECONDS_PER_DAY);
			} else if (_tournament_day_index < LAST_ELIMINATION_DAY) {
				next_elimination_time = Constants.GetTime() + Constants.SECONDS_PER_DAY;
			} else {
				next_elimination_time = -1;
			}
		}
	}

	public void ClearTournamentWinnerFlags(int _nationID)
	{
		// Get the nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationID, false);

		if (nationData == null) {
			return;
		}

		// Remove any tournament winner flags from the nation.
		nationData.SetFlags(nationData.flags & ~(Constants.NF_TOURNAMENT_FIRST_PLACE | Constants.NF_TOURNAMENT_SECOND_PLACE | Constants.NF_TOURNAMENT_THIRD_PLACE));

		// Mark the nation's data to be updated.
		DataManager.MarkForUpdate(nationData);
	}

	public void AttemptJoinTournament(StringBuffer _output_buffer, int _userID)
	{
		// Get the user's data.
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		if (userData == null) {
			return;
		}

		// Get the user's nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, userData.nationID, false);

		// If the nation is already in the current tournament, or if there is no current tournament, do nothing.
		if ((start_day == -1) || (nationData.tournament_start_day == start_day)) {
			return;
		}

		// If the user's rank is not high enough, return message.
		if (userData.rank > Constants.RANK_GENERAL)
		{
			// Return message
			OutputEvents.GetRequestorEvent(_output_buffer, ClientString.Get("svr_join_tournament_rank_too_low")); // "You cannot enroll your nation in the tournament until you've been promoted to General."
			return;
		}

		// Add the nation as a tournament contender.
		nationData.tournament_start_day = start_day;
		nationData.tournament_active = true;
		nationData.tournament_rank = contenders.size() + 1;
		nationData.trophies_available = 0;
		nationData.trophies_banked = 0;
		nationData.trophies_potential = STARTING_POTENTIAL_TROPHIES;
		num_active_contenders++;
		contenders.add(new ContenderData(nationData.ID, nationData));

		// Write tournament ranks list.
		PublishTournamentRanksList();

		// Broadcast global tournament status to all clients.
		OutputEvents.BroadcastGlobalTournamentStatusEvent();

		// Broadcast to this nation its new tournament status.
		OutputEvents.BroadcastNationTournamentStatusEvent(nationData, 0);

		// Send a full map update event to each of the nation's online users, so they will see which other nations are enrolled in the tournament.
		Display.SendFullMapEventToNation(nationData.ID);

		// Mark the TournamentData to be updated.
		DataManager.MarkForUpdate(this);

		// Return message
		OutputEvents.GetRequestorEvent(_output_buffer, ClientString.Get("svr_join_tournament_confirm", "nation_name", nationData.name)); // "{nation_name} has joined the tournament!"
	}

	public void PublishTournamentRanksList()
	{
		ContenderData cur_contender;
		NationData cur_contender_nation;

		contenders_html.append(StringConstants.STR_XML_START);
		contenders_html.append(StringConstants.STR_XML_LIST_START);
		contenders_html.append(StringConstants.STR_XML_ID_START);
		contenders_html.append(StringConstants.STR_RANKS_TOURNAMENT_CURRENT);
		contenders_html.append(StringConstants.STR_XML_ID_END);
		contenders_html.append(StringConstants.STR_XML_RANKS_START);

		for (int index = 0; index < contenders.size(); index++)
		{
			cur_contender = contenders.get(index);
			cur_contender_nation = cur_contender.GetNationData();

			contenders_html.append(StringConstants.STR_XML_RANK_LINE_1);
			contenders_html.append(cur_contender_nation.ID);
			contenders_html.append(StringConstants.STR_XML_RANK_LINE_2);
			contenders_html.append(Constants.XMLEncode(cur_contender_nation.name));
			contenders_html.append(StringConstants.STR_XML_RANK_LINE_3);
			contenders_html.append((int)(cur_contender_nation.trophies_available + cur_contender_nation.trophies_banked + 0.5f));
			contenders_html.append(StringConstants.STR_XML_RANK_LINE_3A);
			contenders_html.append(cur_contender_nation.tournament_active ? "true" : "false");
			contenders_html.append(StringConstants.STR_XML_RANK_LINE_4);
		}

		contenders_html.append(StringConstants.STR_XML_RANKS_END);
		contenders_html.append(StringConstants.STR_XML_LIST_END);
		contenders_html.append(StringConstants.STR_XML_END);

		RanksData.instance.OutputRanksFile(Constants.ranks_dir + StringConstants.STR_RANKS_TOURNAMENT_CURRENT + StringConstants.STR_XML_EXT, contenders_html);
	}

	public void OnSetBlockNationID(int _x, int _y, BlockData _block_data, NationData _formerNationData, NationData _nationData, int _delay)
	{
		// If there exists a tournament record for this block...
		if (_block_data.tournament_record != null)
		{
			// If the record has not yet expired...
			if (_block_data.tournament_record.expire_time > Constants.GetTime())
			{
				// Undo the transfer represented by the record, returning the transferred trophies to the original nation and de-actualizing any potential trophies that were actualized.
				AddTrophiesToNation(_block_data.tournament_record.winning_nationID, -_block_data.tournament_record.trophies_transferred / 2f, -_block_data.tournament_record.trophies_transferred / 2f, -_block_data.tournament_record.trophies_actualized, _delay);
				AddTrophiesToNation(_block_data.tournament_record.losing_nationID, _block_data.tournament_record.trophies_transferred, 0, 0, _delay);
			}

			// Remove the tournament record from the block.
			_block_data.tournament_record = null;
		}

		// If one tournament nation has captured the block from another tournament nation...
		if ((_formerNationData != null) && _formerNationData.tournament_active && (_formerNationData.tournament_start_day == start_day) && (_nationData != null) && _nationData.tournament_active && (_nationData.tournament_start_day == start_day))
		{
			// Determine the reward factor, based on the relative strength and stats of the two nations.
			float reward_factor = DetermineTrophyRewardFactor(_nationData, _formerNationData);

			// Determine how many trophies should be transferred.
			// Reward factor is not factored into the number of trophies to transfer, or else it would be impossible to capture all of a nation's available trophies, as the nation's area decreases and so does its geo eff. Also, would encourage players to have low geo so they lose fewer trophies when attacked.
			float trophies_to_transfer = _formerNationData.trophies_available / Math.max(1, (_formerNationData.mainland_footprint.area + 1));
			//Output.PrintToScreen("trophies_to_transfer: " + trophies_to_transfer + ", trophies_available: " + _formerNationData.trophies_available + ", area: " + _formerNationData.mainland_footprint.area);

			// Determine how many trophies should be actualized. Rate at which trophies are actualized gradually slows.
			// Multiply the reward factor into how many trophies are ctualized, to prevent playrs from creating low geo or low stat opponents to rapidly actualize trophies.
			float trophies_to_actualize = Math.min(_nationData.trophies_potential, (_nationData.trophies_potential + 200f) / 1000f);
			trophies_to_actualize = trophies_to_actualize * reward_factor;

			// Transfer the appropriate amount of trophies from the block's former nation to its new nation, and actualize the appropriate number of trophies for the block's new nation.
			AddTrophiesToNation(_nationData, trophies_to_transfer / 2f, trophies_to_transfer / 2f, trophies_to_actualize, _delay);
			AddTrophiesToNation(_formerNationData, -trophies_to_transfer, 0, 0, _delay);

			// Create a BlockTournamentRecord for this block so that the above will be undone if the block's new nation does not hold the block for at least UNDO_CAPTURE_PERIOD.
			_block_data.tournament_record = new BlockTournamentRecord(_nationData.ID, _formerNationData.ID, trophies_to_transfer, trophies_to_actualize, Constants.GetTime() + UNDO_CAPTURE_PERIOD);

			// If the nation that lost the block now has no area (it's been wiped), make half of its banked trophies available.
			if (_formerNationData.mainland_footprint.area == 0)
			{
				float half_banked_trophies = (_formerNationData.trophies_banked / 2f);
				_formerNationData.trophies_available += half_banked_trophies;
				_formerNationData.trophies_banked = half_banked_trophies;

				// Send a nation tournament status event to the nation that lost the block, telling it the new counts of available and banked trophies.
				OutputEvents.BroadcastNationTournamentStatusEvent(_formerNationData, _delay);
			}
		}
	}

	public void AddTrophiesToNation(int _nationID, float _trophies_to_transfer_available, float _trophies_to_transfer_banked, float _trophies_to_actualize, int _delay)
	{
		// Get the nation's NationData.
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationID, false);

		if (nationData == null) {
			return;
		}

		AddTrophiesToNation(nationData, _trophies_to_transfer_available, _trophies_to_transfer_banked, _trophies_to_actualize, _delay);
	}

	public void AddTrophiesToNation(NationData _nationData, float _trophies_to_transfer_available, float _trophies_to_transfer_banked, float _trophies_to_actualize, int _delay)
	{
		//Output.PrintToScreen("AddTrophiesToNation() nation: " + _nationData.name + ", transfer avail: " + _trophies_to_transfer_available + ", transfer bnkd: " + _trophies_to_transfer_banked + ", actualize: " + _trophies_to_actualize);
		//Output.PrintStackTrace();

		if (Float.isNaN(_trophies_to_transfer_available) || Float.isNaN(_trophies_to_transfer_banked) || Float.isNaN(_trophies_to_actualize))
		{
			Output.PrintToScreen("ERROR: NaN value passed to AddTrophiesToNation(). _trophies_to_transfer_available: " + _trophies_to_transfer_available + ", _trophies_to_transfer_banked: " + _trophies_to_transfer_banked + ", _trophies_to_actualize: " + _trophies_to_actualize);
			Output.PrintStackTrace();
			return;
		}

		// Determine the nation's current total integral number of trophies.
		int starting_trophy_count = (int)(_nationData.trophies_available + _nationData.trophies_banked + 0.5f);

		// Limit the _trophies_to_actualize to no more than the nation's number of pending trophies.
		_trophies_to_actualize = Math.min(_trophies_to_actualize, _nationData.trophies_potential);

		// Add the appropriate transfer deltas to, and split the trophies to actualize delta between, the nation's available and banked trophies.
		_nationData.trophies_available = Math.max(0, _nationData.trophies_available + _trophies_to_transfer_available + (_trophies_to_actualize / 2f));
		_nationData.trophies_banked = Math.max(0, _nationData.trophies_banked + _trophies_to_transfer_banked + (_trophies_to_actualize / 2f));

		// Subtract the _trophies_to_actualize from the nation's potential trophies.
		_nationData.trophies_potential = Math.max(0, _nationData.trophies_potential - _trophies_to_actualize);

		// Make sure the nation's tournament rank is correct.
		if ((_nationData.tournament_rank > contenders.size()) || (contenders.get(_nationData.tournament_rank - 1).nationID != _nationData.ID))
		{
			Output.PrintToScreen("ERROR: Nation " + _nationData.name + " has incorrect tournament rank " + _nationData.tournament_rank + ".");

			// Determine the correct tournament rank.
			_nationData.tournament_rank = -1;
			for (int i = 0; i < contenders.size(); i++)
			{
				if (contenders.get(i).nationID == _nationData.ID)
				{
					_nationData.tournament_rank = i + 1;
					break;
				}
			}

			Output.PrintToScreen("       Nation " + _nationData.name + " tournament rank corrected to " + _nationData.tournament_rank + ".");
		}

		// Make sure the nation is in the list of contenders.
		if (_nationData.tournament_rank == -1)
		{
			Output.PrintToScreen("ERROR: Contender nation " + _nationData.name + " is not in the list of contenders.");
			return;
		}

		// While this nation has more trophies than the nation before it in the contenders list, move this nation up the list and adjust ranks appropriately.
		while ((_nationData.tournament_rank > 1) && ((_nationData.trophies_available + _nationData.trophies_banked) > (contenders.get(_nationData.tournament_rank - 2).GetNationData().trophies_available + contenders.get(_nationData.tournament_rank - 2).GetNationData().trophies_banked)))
		{
			// Swap the ranks of this nation and the previous nation in the list of contenders.
			contenders.get(_nationData.tournament_rank - 2).GetNationData().tournament_rank = _nationData.tournament_rank;
			_nationData.tournament_rank--;

			// Swap the list positions of the two nations.
			Collections.swap(contenders, _nationData.tournament_rank - 1, _nationData.tournament_rank);

			// Mark the TournamentData to be updated.
			DataManager.MarkForUpdate(this);
		}

		// While this nation has fewer trophies than the nation after it in the contenders list, move this nation down the list and adjust ranks appropriately.
		while ((_nationData.tournament_rank > 1) && ((_nationData.trophies_available + _nationData.trophies_banked) > (contenders.get(_nationData.tournament_rank - 2).GetNationData().trophies_available + contenders.get(_nationData.tournament_rank - 2).GetNationData().trophies_banked)))
		{
			// Swap the ranks of this nation and the next nation in the list of contenders.
			contenders.get(_nationData.tournament_rank).GetNationData().tournament_rank = _nationData.tournament_rank;
			_nationData.tournament_rank++;

			// Swap the list positions of the two nations.
			Collections.swap(contenders, _nationData.tournament_rank - 2, _nationData.tournament_rank - 1);

			// Mark the TournamentData to be updated.
			DataManager.MarkForUpdate(this);
		}

		// Determine the nation's new total integral number of trophies.
		int ending_trophy_count = (int)(_nationData.trophies_available + _nationData.trophies_banked + 0.5f);

		// If the nation's sum trophy amount has changed by a whole number value, broadcast a tournament statis event to the nation.
		if (starting_trophy_count != ending_trophy_count) {
			OutputEvents.BroadcastNationTournamentStatusEvent(_nationData, _delay);
		}

		// Mark the nation's data to be updated.
		DataManager.MarkForUpdate(_nationData);
	}

	public float DetermineTrophyRewardFactor(NationData _nationData, NationData _targetNationData)
	{
		float reward_factor = 1f;

		// If the attacker is much more powerful than the defender, reduce the XP amount.
		if (_nationData.prev_attack_ratio > 2f) {
			reward_factor = reward_factor * (2f / _nationData.prev_attack_ratio);
		}

		int half_xp_threshold = Math.max(5, _nationData.level / 6);
		int no_xp_threshold = Math.max(10, _nationData.level / 3);

		// If the target nation is >= half_xp_threshold levels lower than the attacker, then the XP is halved.
		// If the target nation >= no_xp_threshold levels lower, then no XP is awarded.
		// Subtract unspent advance points for purposes of determining level, so nations can't create weak adversaries to level off of, by not using advance points.
		if (((_nationData.level - _nationData.advance_points) - (_targetNationData.level - _targetNationData.advance_points)) >= no_xp_threshold) {
			reward_factor = 0f;
		} else if ((_nationData.level - _targetNationData.level) >= half_xp_threshold) {
			reward_factor = reward_factor / 2f;
		}

		reward_factor *= _targetNationData.GetFinalGeoEfficiency(Constants.MAINLAND_MAP_ID);

		return reward_factor;
	}

	public void ListContenders()
	{
		Output.PrintToScreen("Tournament contenders:");
		for (int i = 0; i < contenders.size(); i++)
		{
			NationData cur_contender = contenders.get(i).GetNationData();
			Output.PrintToScreen((cur_contender.tournament_active ? "   " : " X ") + cur_contender.tournament_rank + ") " + cur_contender.name + " (" + cur_contender.ID + ") " + (cur_contender.trophies_available + cur_contender.trophies_banked) + " trophies (" + cur_contender.trophies_banked + " banked), " + cur_contender.trophies_potential + " potential.");
		}
	}
}
