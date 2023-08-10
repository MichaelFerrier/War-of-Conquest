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
import WOCServer.*;

public class RanksData extends BaseData
{
	public static String db_table_name = "Ranks";
	public static int VERSION = 1;

	// Static StringBuffers
	public static int RANKS_HTML_LENGTH = 100000;
	public static StringBuffer ranks_html = new StringBuffer(RANKS_HTML_LENGTH);
	static int [] coord_array_publish = new int[2];

	public static RanksData instance = null;

	ArrayList<Integer> goals_token = new ArrayList<Integer>();
	ArrayList<Integer> goals_value = new ArrayList<Integer>();
	ArrayList<String> goals_name = new ArrayList<String>();
	ArrayList<String> goals_location = new ArrayList<String>();
	ArrayList<String> goals_location_short = new ArrayList<String>();
	ArrayList<Float> goals_total_awarded = new ArrayList<Float>();
	ArrayList<RanksList> goals_ranks = new ArrayList<RanksList>();
	ArrayList<RanksList> goals_ranks_monthly = new ArrayList<RanksList>();
	RanksList ranks_nation_xp = new RanksList();
	RanksList ranks_nation_xp_monthly = new RanksList();
	RanksList ranks_user_xp = new RanksList();
	RanksList ranks_user_xp_monthly = new RanksList();
	RanksList ranks_user_followers = new RanksList();
	RanksList ranks_user_followers_monthly = new RanksList();
	RanksList ranks_nation_winnings = new RanksList();
	RanksList ranks_nation_winnings_monthly = new RanksList();
	RanksList ranks_nation_latest_tournament = new RanksList();
	RanksList ranks_nation_tournament_trophies = new RanksList();
	RanksList ranks_nation_tournament_trophies_monthly = new RanksList();
	RanksList ranks_nation_level = new RanksList();
	RanksList ranks_nation_rebirths = new RanksList();
	RanksList ranks_nation_quests = new RanksList();
	RanksList ranks_nation_quests_monthly = new RanksList();
	RanksList ranks_nation_energy_donated = new RanksList();
	RanksList ranks_nation_energy_donated_monthly = new RanksList();
	RanksList ranks_nation_manpower_donated = new RanksList();
	RanksList ranks_nation_manpower_donated_monthly = new RanksList();
	RanksList ranks_nation_area = new RanksList();
	RanksList ranks_nation_area_monthly = new RanksList();
	RanksList ranks_nation_captures = new RanksList();
	RanksList ranks_nation_captures_monthly = new RanksList();
	RanksList ranks_nation_medals = new RanksList();
	RanksList ranks_nation_medals_monthly = new RanksList();
	RanksList ranks_nation_raid_earnings = new RanksList();
	RanksList ranks_nation_raid_earnings_monthly = new RanksList();
	RanksList ranks_nation_orb_shard_earnings = new RanksList();
	RanksList ranks_nation_orb_shard_earnings_monthly = new RanksList();

	public static void StartUp()
	{
		// Load the RanksData singleton instance.
		instance = (RanksData)DataManager.GetData(Constants.DT_RANKS, Constants.RANKS_DATA_ID, false);
	}

	public RanksData(int _ID)
	{
		super(Constants.DT_RANKS, _ID);
	}

	public static void OutputRanksList(RanksList _ranks_list)
	{
		Output.PrintToScreen("Count: " + _ranks_list.IDs.size());
		for (int i = 0; i < _ranks_list.IDs.size(); i++)
		{
			Output.PrintToScreen((i + 1) + ": name: " +  _ranks_list.names.get(i) + ", amount: " + _ranks_list.amounts.get(i));
		}
	}

	public void RemoveUserFromRanks(int _userID)
	{
		ranks_user_xp.RemoveRank(_userID);
		ranks_user_xp_monthly.RemoveRank(_userID);
		ranks_user_followers.RemoveRank(_userID);
		ranks_user_followers_monthly.RemoveRank(_userID);
	}

	public void RemoveNationFromRanks(int _nationID)
	{
		// Get the nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationID, false);

		// Reset the nation's ranks history data
		nationData.prize_money_history = 0f;
		nationData.prize_money_history_monthly = 0f;
		nationData.level_history = nationData.level;
		nationData.xp_history = nationData.xp;
		nationData.xp_history_monthly = nationData.xp;
		nationData.tournament_trophies_history = 0f;
		nationData.tournament_trophies_history_monthly = 0f;
		nationData.donated_energy_history = 0f;
		nationData.donated_energy_history_monthly = 0f;
		nationData.donated_manpower_history = 0f;
		nationData.donated_manpower_history_monthly = 0f;
		nationData.captures_history = 0;
		nationData.captures_history_monthly = 0;
		nationData.raid_earnings_history = 0;
		nationData.raid_earnings_history_monthly = 0;
		nationData.orb_shard_earnings_history = 0;
		nationData.orb_shard_earnings_history_monthly = 0;
		nationData.medals_history = 0;
		nationData.medals_history_monthly = 0;

		// Mark the nation's data for update
		DataManager.MarkForUpdate(nationData);

		ranks_nation_xp.RemoveRank(_nationID);
		ranks_nation_xp_monthly.RemoveRank(_nationID);
		ranks_nation_winnings.RemoveRank(_nationID);
		ranks_nation_winnings_monthly.RemoveRank(_nationID);
		ranks_nation_tournament_trophies.RemoveRank(_nationID);
		ranks_nation_tournament_trophies_monthly.RemoveRank(_nationID);
		ranks_nation_level.RemoveRank(_nationID);
		ranks_nation_rebirths.RemoveRank(_nationID);
		ranks_nation_quests.RemoveRank(_nationID);
		ranks_nation_quests_monthly.RemoveRank(_nationID);
		ranks_nation_energy_donated.RemoveRank(_nationID);
		ranks_nation_energy_donated_monthly.RemoveRank(_nationID);
		ranks_nation_manpower_donated.RemoveRank(_nationID);
		ranks_nation_manpower_donated_monthly.RemoveRank(_nationID);
		ranks_nation_area.RemoveRank(_nationID);
		ranks_nation_area_monthly.RemoveRank(_nationID);
		ranks_nation_captures.RemoveRank(_nationID);
		ranks_nation_captures_monthly.RemoveRank(_nationID);
		ranks_nation_medals.RemoveRank(_nationID);
		ranks_nation_medals_monthly.RemoveRank(_nationID);
		ranks_nation_raid_earnings.RemoveRank(_nationID);
		ranks_nation_raid_earnings_monthly.RemoveRank(_nationID);
		ranks_nation_orb_shard_earnings.RemoveRank(_nationID);
		ranks_nation_orb_shard_earnings_monthly.RemoveRank(_nationID);
	}

// Example xml file format:
//<?xml version="1.0"?>
//<list>
//  <ID>example_ranks</ID>
//  <total>500</total>
//  <ranks>
//		<rank ID="0" name="example" amount="2000"/>
//		<rank ID="1" name="example" amount="47.2"/>
//  </ranks>
//</list>

	public void PublishAllRanks()
	{
		String filename = "";
		int index, cur_value;
		RanksList ranks_list;

		for (int goal_index = 0; goal_index < goals_token.size(); goal_index++)
		{
			// Get coordinates of goal object
			Constants.UntokenizeCoordinates(goals_token.get(goal_index), coord_array_publish);

			ranks_html.append(StringConstants.STR_XML_START);

			// This month's top winners for each orb
			ComposeRanksList(goals_ranks_monthly.get(goal_index), ranks_html, StringConstants.STR_ORB_HISTORY_MONTHLY, Constants.RANKS_LIST_LENGTH_SMALL);

			// Overall top winners for each orb
			ComposeRanksList(goals_ranks.get(goal_index), ranks_html, StringConstants.STR_ORB_HISTORY, Constants.RANKS_LIST_LENGTH_SMALL);

			ranks_html.append(StringConstants.STR_XML_END);

			// Write the goal's ranks file
			OutputRanksFile(Constants.ranks_dir + "ranks_" + coord_array_publish[0] + "_" + coord_array_publish[1] + ".xml", ranks_html);
		}

		// Nation XP ranks
		ranks_html.append(StringConstants.STR_XML_START);
		ComposeRanksList(ranks_nation_xp, ranks_html, StringConstants.STR_RANKS_NATION_XP, Constants.RANKS_LIST_LENGTH_LARGE);
		ranks_html.append(StringConstants.STR_XML_END);
		OutputRanksFile(Constants.ranks_dir + StringConstants.STR_RANKS_NATION_XP + StringConstants.STR_XML_EXT, ranks_html);

		// Nation XP monthly ranks
		ranks_html.append(StringConstants.STR_XML_START);
		ComposeRanksList(ranks_nation_xp_monthly, ranks_html, StringConstants.STR_RANKS_NATION_XP_MONTHLY, Constants.RANKS_LIST_LENGTH_LARGE);
		ranks_html.append(StringConstants.STR_XML_END);
		OutputRanksFile(Constants.ranks_dir + StringConstants.STR_RANKS_NATION_XP_MONTHLY + StringConstants.STR_XML_EXT, ranks_html);

		// User XP ranks
		ranks_html.append(StringConstants.STR_XML_START);
		ComposeRanksList(ranks_user_xp, ranks_html, StringConstants.STR_RANKS_USER_XP, Constants.RANKS_LIST_LENGTH_LARGE);
		ranks_html.append(StringConstants.STR_XML_END);
		OutputRanksFile(Constants.ranks_dir + StringConstants.STR_RANKS_USER_XP + StringConstants.STR_XML_EXT, ranks_html);

		// User XP monthly ranks
		ranks_html.append(StringConstants.STR_XML_START);
		ComposeRanksList(ranks_user_xp_monthly, ranks_html, StringConstants.STR_RANKS_USER_XP_MONTHLY, Constants.RANKS_LIST_LENGTH_LARGE);
		ranks_html.append(StringConstants.STR_XML_END);
		OutputRanksFile(Constants.ranks_dir + StringConstants.STR_RANKS_USER_XP_MONTHLY + StringConstants.STR_XML_EXT, ranks_html);

		// User followers ranks
		ranks_html.append(StringConstants.STR_XML_START);
		ComposeRanksList(ranks_user_followers, ranks_html, StringConstants.STR_RANKS_USER_FOLLOWERS, Constants.RANKS_LIST_LENGTH_LARGE);
		ranks_html.append(StringConstants.STR_XML_END);
		OutputRanksFile(Constants.ranks_dir + StringConstants.STR_RANKS_USER_FOLLOWERS + StringConstants.STR_XML_EXT, ranks_html);

		// User followers monthly ranks
		ranks_html.append(StringConstants.STR_XML_START);
		ComposeRanksList(ranks_user_followers_monthly, ranks_html, StringConstants.STR_RANKS_USER_FOLLOWERS_MONTHLY, Constants.RANKS_LIST_LENGTH_LARGE);
		ranks_html.append(StringConstants.STR_XML_END);
		OutputRanksFile(Constants.ranks_dir + StringConstants.STR_RANKS_USER_FOLLOWERS_MONTHLY + StringConstants.STR_XML_EXT, ranks_html);

		// Nation winnings ranks
		ranks_html.append(StringConstants.STR_XML_START);
		ComposeRanksList(ranks_nation_winnings, ranks_html, StringConstants.STR_RANKS_NATION_WINNINGS, Constants.RANKS_LIST_LENGTH_LARGE);
		ranks_html.append(StringConstants.STR_XML_END);
		OutputRanksFile(Constants.ranks_dir + StringConstants.STR_RANKS_NATION_WINNINGS + StringConstants.STR_XML_EXT, ranks_html);

		// Nation winnings monthly ranks
		ranks_html.append(StringConstants.STR_XML_START);
		ComposeRanksList(ranks_nation_winnings_monthly, ranks_html, StringConstants.STR_RANKS_NATION_WINNINGS_MONTHLY, Constants.RANKS_LIST_LENGTH_LARGE);
		ranks_html.append(StringConstants.STR_XML_END);
		OutputRanksFile(Constants.ranks_dir + StringConstants.STR_RANKS_NATION_WINNINGS_MONTHLY + StringConstants.STR_XML_EXT, ranks_html);

		// Nation latest tournament ranks
		ranks_html.append(StringConstants.STR_XML_START);
		ComposeRanksList(ranks_nation_latest_tournament, ranks_html, StringConstants.STR_RANKS_NATION_LATEST_TOURNAMENT, Constants.RANKS_LIST_LENGTH_LARGE);
		ranks_html.append(StringConstants.STR_XML_END);
		OutputRanksFile(Constants.ranks_dir + StringConstants.STR_RANKS_NATION_LATEST_TOURNAMENT + StringConstants.STR_XML_EXT, ranks_html);

		// Nation tournament trophies ranks
		ranks_html.append(StringConstants.STR_XML_START);
		ComposeRanksList(ranks_nation_tournament_trophies, ranks_html, StringConstants.STR_RANKS_NATION_TOURNAMENT_TROPHIES, Constants.RANKS_LIST_LENGTH_LARGE);
		ranks_html.append(StringConstants.STR_XML_END);
		OutputRanksFile(Constants.ranks_dir + StringConstants.STR_RANKS_NATION_TOURNAMENT_TROPHIES + StringConstants.STR_XML_EXT, ranks_html);

		// Nation tournament trophies monthly ranks
		ranks_html.append(StringConstants.STR_XML_START);
		ComposeRanksList(ranks_nation_tournament_trophies_monthly, ranks_html, StringConstants.STR_RANKS_NATION_TOURNAMENT_TROPHIES_MONTHLY, Constants.RANKS_LIST_LENGTH_LARGE);
		ranks_html.append(StringConstants.STR_XML_END);
		OutputRanksFile(Constants.ranks_dir + StringConstants.STR_RANKS_NATION_TOURNAMENT_TROPHIES_MONTHLY + StringConstants.STR_XML_EXT, ranks_html);

		// Nation level ranks
		ranks_html.append(StringConstants.STR_XML_START);
		ComposeRanksList(ranks_nation_level, ranks_html, StringConstants.STR_RANKS_NATION_LEVEL, Constants.RANKS_LIST_LENGTH_LARGE);
		ranks_html.append(StringConstants.STR_XML_END);
		OutputRanksFile(Constants.ranks_dir + StringConstants.STR_RANKS_NATION_LEVEL + StringConstants.STR_XML_EXT, ranks_html);

		// Nation rebirths ranks
		ranks_html.append(StringConstants.STR_XML_START);
		ComposeRanksList(ranks_nation_rebirths, ranks_html, StringConstants.STR_RANKS_NATION_REBIRTHS, Constants.RANKS_LIST_LENGTH_LARGE);
		ranks_html.append(StringConstants.STR_XML_END);
		OutputRanksFile(Constants.ranks_dir + StringConstants.STR_RANKS_NATION_REBIRTHS + StringConstants.STR_XML_EXT, ranks_html);

		// Nation quests ranks
		ranks_html.append(StringConstants.STR_XML_START);
		ComposeRanksList(ranks_nation_quests, ranks_html, StringConstants.STR_RANKS_NATION_QUESTS, Constants.RANKS_LIST_LENGTH_LARGE);
		ranks_html.append(StringConstants.STR_XML_END);
		OutputRanksFile(Constants.ranks_dir + StringConstants.STR_RANKS_NATION_QUESTS + StringConstants.STR_XML_EXT, ranks_html);

		// Nation quests monthly ranks
		ranks_html.append(StringConstants.STR_XML_START);
		ComposeRanksList(ranks_nation_quests_monthly, ranks_html, StringConstants.STR_RANKS_NATION_QUESTS_MONTHLY, Constants.RANKS_LIST_LENGTH_LARGE);
		ranks_html.append(StringConstants.STR_XML_END);
		OutputRanksFile(Constants.ranks_dir + StringConstants.STR_RANKS_NATION_QUESTS_MONTHLY + StringConstants.STR_XML_EXT, ranks_html);

		// Nation energy donated ranks
		ranks_html.append(StringConstants.STR_XML_START);
		ComposeRanksList(ranks_nation_energy_donated, ranks_html, StringConstants.STR_RANKS_NATION_ENERGY_DONATED, Constants.RANKS_LIST_LENGTH_LARGE);
		ranks_html.append(StringConstants.STR_XML_END);
		OutputRanksFile(Constants.ranks_dir + StringConstants.STR_RANKS_NATION_ENERGY_DONATED + StringConstants.STR_XML_EXT, ranks_html);

		// Nation energy donated monthly ranks
		ranks_html.append(StringConstants.STR_XML_START);
		ComposeRanksList(ranks_nation_energy_donated_monthly, ranks_html, StringConstants.STR_RANKS_NATION_ENERGY_DONATED_MONTHLY, Constants.RANKS_LIST_LENGTH_LARGE);
		ranks_html.append(StringConstants.STR_XML_END);
		OutputRanksFile(Constants.ranks_dir + StringConstants.STR_RANKS_NATION_ENERGY_DONATED_MONTHLY + StringConstants.STR_XML_EXT, ranks_html);

		// Nation manpower donated ranks
		ranks_html.append(StringConstants.STR_XML_START);
		ComposeRanksList(ranks_nation_manpower_donated, ranks_html, StringConstants.STR_RANKS_NATION_MANPOWER_DONATED, Constants.RANKS_LIST_LENGTH_LARGE);
		ranks_html.append(StringConstants.STR_XML_END);
		OutputRanksFile(Constants.ranks_dir + StringConstants.STR_RANKS_NATION_MANPOWER_DONATED + StringConstants.STR_XML_EXT, ranks_html);

		// Nation manpower donated monthly ranks
		ranks_html.append(StringConstants.STR_XML_START);
		ComposeRanksList(ranks_nation_manpower_donated_monthly, ranks_html, StringConstants.STR_RANKS_NATION_MANPOWER_DONATED_MONTHLY, Constants.RANKS_LIST_LENGTH_LARGE);
		ranks_html.append(StringConstants.STR_XML_END);
		OutputRanksFile(Constants.ranks_dir + StringConstants.STR_RANKS_NATION_MANPOWER_DONATED_MONTHLY + StringConstants.STR_XML_EXT, ranks_html);

		// Nation area ranks
		ranks_html.append(StringConstants.STR_XML_START);
		ComposeRanksList(ranks_nation_area, ranks_html, StringConstants.STR_RANKS_NATION_AREA, Constants.RANKS_LIST_LENGTH_LARGE);
		ranks_html.append(StringConstants.STR_XML_END);
		OutputRanksFile(Constants.ranks_dir + StringConstants.STR_RANKS_NATION_AREA + StringConstants.STR_XML_EXT, ranks_html);

		// Nation area monthly ranks
		ranks_html.append(StringConstants.STR_XML_START);
		ComposeRanksList(ranks_nation_area_monthly, ranks_html, StringConstants.STR_RANKS_NATION_AREA_MONTHLY, Constants.RANKS_LIST_LENGTH_LARGE);
		ranks_html.append(StringConstants.STR_XML_END);
		OutputRanksFile(Constants.ranks_dir + StringConstants.STR_RANKS_NATION_AREA_MONTHLY + StringConstants.STR_XML_EXT, ranks_html);

		// Nation captures ranks
		ranks_html.append(StringConstants.STR_XML_START);
		ComposeRanksList(ranks_nation_captures, ranks_html, StringConstants.STR_RANKS_NATION_CAPTURES, Constants.RANKS_LIST_LENGTH_LARGE);
		ranks_html.append(StringConstants.STR_XML_END);
		OutputRanksFile(Constants.ranks_dir + StringConstants.STR_RANKS_NATION_CAPTURES + StringConstants.STR_XML_EXT, ranks_html);

		// Nation captures monthly ranks
		ranks_html.append(StringConstants.STR_XML_START);
		ComposeRanksList(ranks_nation_captures_monthly, ranks_html, StringConstants.STR_RANKS_NATION_CAPTURES_MONTHLY, Constants.RANKS_LIST_LENGTH_LARGE);
		ranks_html.append(StringConstants.STR_XML_END);
		OutputRanksFile(Constants.ranks_dir + StringConstants.STR_RANKS_NATION_CAPTURES_MONTHLY + StringConstants.STR_XML_EXT, ranks_html);

		// Nation medals ranks
		ranks_html.append(StringConstants.STR_XML_START);
		ComposeRanksList(ranks_nation_medals, ranks_html, StringConstants.STR_RANKS_NATION_MEDALS, Constants.RANKS_LIST_LENGTH_LARGE);
		ranks_html.append(StringConstants.STR_XML_END);
		OutputRanksFile(Constants.ranks_dir + StringConstants.STR_RANKS_NATION_MEDALS + StringConstants.STR_XML_EXT, ranks_html);

		// Nation medals monthly ranks
		ranks_html.append(StringConstants.STR_XML_START);
		ComposeRanksList(ranks_nation_medals_monthly, ranks_html, StringConstants.STR_RANKS_NATION_MEDALS_MONTHLY, Constants.RANKS_LIST_LENGTH_LARGE);
		ranks_html.append(StringConstants.STR_XML_END);
		OutputRanksFile(Constants.ranks_dir + StringConstants.STR_RANKS_NATION_MEDALS_MONTHLY + StringConstants.STR_XML_EXT, ranks_html);

		// Nation raid earnings ranks
		ranks_html.append(StringConstants.STR_XML_START);
		ComposeRanksList(ranks_nation_raid_earnings, ranks_html, StringConstants.STR_RANKS_NATION_RAID_EARNINGS, Constants.RANKS_LIST_LENGTH_LARGE);
		ranks_html.append(StringConstants.STR_XML_END);
		OutputRanksFile(Constants.ranks_dir + StringConstants.STR_RANKS_NATION_RAID_EARNINGS + StringConstants.STR_XML_EXT, ranks_html);

		// Nation raid earnings monthly ranks
		ranks_html.append(StringConstants.STR_XML_START);
		ComposeRanksList(ranks_nation_raid_earnings_monthly, ranks_html, StringConstants.STR_RANKS_NATION_RAID_EARNINGS_MONTHLY, Constants.RANKS_LIST_LENGTH_LARGE);
		ranks_html.append(StringConstants.STR_XML_END);
		OutputRanksFile(Constants.ranks_dir + StringConstants.STR_RANKS_NATION_RAID_EARNINGS_MONTHLY + StringConstants.STR_XML_EXT, ranks_html);

		// Nation orb shard earnings ranks
		ranks_html.append(StringConstants.STR_XML_START);
		ComposeRanksList(ranks_nation_orb_shard_earnings, ranks_html, StringConstants.STR_RANKS_NATION_ORB_SHARD_EARNINGS, Constants.RANKS_LIST_LENGTH_LARGE);
		ranks_html.append(StringConstants.STR_XML_END);
		OutputRanksFile(Constants.ranks_dir + StringConstants.STR_RANKS_NATION_ORB_SHARD_EARNINGS + StringConstants.STR_XML_EXT, ranks_html);

		// Nation orb shard earnings monthly ranks
		ranks_html.append(StringConstants.STR_XML_START);
		ComposeRanksList(ranks_nation_orb_shard_earnings_monthly, ranks_html, StringConstants.STR_RANKS_NATION_ORB_SHARD_EARNINGS_MONTHLY, Constants.RANKS_LIST_LENGTH_LARGE);
		ranks_html.append(StringConstants.STR_XML_END);
		OutputRanksFile(Constants.ranks_dir + StringConstants.STR_RANKS_NATION_ORB_SHARD_EARNINGS_MONTHLY + StringConstants.STR_XML_EXT, ranks_html);

		// Combined ranks (short lists)
		ranks_html.append(StringConstants.STR_XML_START);
		ComposeRanksList(ranks_nation_xp, ranks_html, StringConstants.STR_RANKS_NATION_XP, Constants.RANKS_LIST_LENGTH_SMALL);
		ComposeRanksList(ranks_nation_xp_monthly, ranks_html, StringConstants.STR_RANKS_NATION_XP_MONTHLY, Constants.RANKS_LIST_LENGTH_SMALL);
		ComposeRanksList(ranks_user_xp, ranks_html, StringConstants.STR_RANKS_USER_XP, Constants.RANKS_LIST_LENGTH_SMALL);
		ComposeRanksList(ranks_user_xp_monthly, ranks_html, StringConstants.STR_RANKS_USER_XP_MONTHLY, Constants.RANKS_LIST_LENGTH_SMALL);
		ComposeRanksList(ranks_nation_winnings, ranks_html, StringConstants.STR_RANKS_NATION_WINNINGS, Constants.RANKS_LIST_LENGTH_SMALL);
		ComposeRanksList(ranks_nation_winnings_monthly, ranks_html, StringConstants.STR_RANKS_NATION_WINNINGS_MONTHLY, Constants.RANKS_LIST_LENGTH_SMALL);
		ComposeRanksList(ranks_nation_medals, ranks_html, StringConstants.STR_RANKS_NATION_MEDALS, Constants.RANKS_LIST_LENGTH_SMALL);
		ComposeRanksList(ranks_nation_medals_monthly, ranks_html, StringConstants.STR_RANKS_NATION_MEDALS_MONTHLY, Constants.RANKS_LIST_LENGTH_SMALL);
		ComposeRanksList(ranks_nation_raid_earnings, ranks_html, StringConstants.STR_RANKS_NATION_RAID_EARNINGS, Constants.RANKS_LIST_LENGTH_SMALL);
		ComposeRanksList(ranks_nation_raid_earnings_monthly, ranks_html, StringConstants.STR_RANKS_NATION_RAID_EARNINGS_MONTHLY, Constants.RANKS_LIST_LENGTH_SMALL);
		ComposeRanksList(ranks_nation_orb_shard_earnings, ranks_html, StringConstants.STR_RANKS_NATION_ORB_SHARD_EARNINGS, Constants.RANKS_LIST_LENGTH_SMALL);
		ComposeRanksList(ranks_nation_orb_shard_earnings_monthly, ranks_html, StringConstants.STR_RANKS_NATION_ORB_SHARD_EARNINGS_MONTHLY, Constants.RANKS_LIST_LENGTH_SMALL);
		ComposeRanksList(ranks_nation_latest_tournament, ranks_html, StringConstants.STR_RANKS_NATION_LATEST_TOURNAMENT, Constants.RANKS_LIST_LENGTH_SMALL);
		ComposeRanksList(ranks_nation_tournament_trophies, ranks_html, StringConstants.STR_RANKS_NATION_TOURNAMENT_TROPHIES, Constants.RANKS_LIST_LENGTH_SMALL);
		ComposeRanksList(ranks_nation_tournament_trophies_monthly, ranks_html, StringConstants.STR_RANKS_NATION_TOURNAMENT_TROPHIES_MONTHLY, Constants.RANKS_LIST_LENGTH_SMALL);
		ComposeRanksList(ranks_nation_level, ranks_html, StringConstants.STR_RANKS_NATION_LEVEL, Constants.RANKS_LIST_LENGTH_SMALL);
		ComposeRanksList(ranks_nation_rebirths, ranks_html, StringConstants.STR_RANKS_NATION_REBIRTHS, Constants.RANKS_LIST_LENGTH_SMALL);
		ComposeRanksList(ranks_nation_quests, ranks_html, StringConstants.STR_RANKS_NATION_QUESTS, Constants.RANKS_LIST_LENGTH_SMALL);
		ComposeRanksList(ranks_nation_quests_monthly, ranks_html, StringConstants.STR_RANKS_NATION_QUESTS_MONTHLY, Constants.RANKS_LIST_LENGTH_SMALL);
		ComposeRanksList(ranks_user_followers, ranks_html, StringConstants.STR_RANKS_USER_FOLLOWERS, Constants.RANKS_LIST_LENGTH_SMALL);
		ComposeRanksList(ranks_user_followers_monthly, ranks_html, StringConstants.STR_RANKS_USER_FOLLOWERS_MONTHLY, Constants.RANKS_LIST_LENGTH_SMALL);
		ComposeRanksList(ranks_nation_energy_donated, ranks_html, StringConstants.STR_RANKS_NATION_ENERGY_DONATED, Constants.RANKS_LIST_LENGTH_SMALL);
		ComposeRanksList(ranks_nation_energy_donated_monthly, ranks_html, StringConstants.STR_RANKS_NATION_ENERGY_DONATED_MONTHLY, Constants.RANKS_LIST_LENGTH_SMALL);
		ComposeRanksList(ranks_nation_manpower_donated, ranks_html, StringConstants.STR_RANKS_NATION_MANPOWER_DONATED, Constants.RANKS_LIST_LENGTH_SMALL);
		ComposeRanksList(ranks_nation_manpower_donated_monthly, ranks_html, StringConstants.STR_RANKS_NATION_MANPOWER_DONATED_MONTHLY, Constants.RANKS_LIST_LENGTH_SMALL);
		ComposeRanksList(ranks_nation_area, ranks_html, StringConstants.STR_RANKS_NATION_AREA, Constants.RANKS_LIST_LENGTH_SMALL);
		ComposeRanksList(ranks_nation_area_monthly, ranks_html, StringConstants.STR_RANKS_NATION_AREA_MONTHLY, Constants.RANKS_LIST_LENGTH_SMALL);
		ComposeRanksList(ranks_nation_captures, ranks_html, StringConstants.STR_RANKS_NATION_CAPTURES, Constants.RANKS_LIST_LENGTH_SMALL);
		ComposeRanksList(ranks_nation_captures_monthly, ranks_html, StringConstants.STR_RANKS_NATION_CAPTURES_MONTHLY, Constants.RANKS_LIST_LENGTH_SMALL);
		ranks_html.append(StringConstants.STR_XML_END);
		OutputRanksFile(Constants.ranks_dir + StringConstants.STR_RANKS_COMBINED + StringConstants.STR_XML_EXT, ranks_html);
	}

	public void ComposeRanksList(RanksList _ranks_list, StringBuffer _buffer, String _title, int _max_count)
	{
		_buffer.append(StringConstants.STR_XML_LIST_START);
		_buffer.append(StringConstants.STR_XML_ID_START);
		_buffer.append(_title);
		_buffer.append(StringConstants.STR_XML_ID_END);
		_buffer.append(StringConstants.STR_XML_RANKS_START);

		for (int index = 0; index < _ranks_list.IDs.size(); index++)
		{
			if (index == _max_count) {
				break;
			}

			_buffer.append(StringConstants.STR_XML_RANK_LINE_1);
			_buffer.append(_ranks_list.IDs.get(index));
			_buffer.append(StringConstants.STR_XML_RANK_LINE_2);
			_buffer.append(Constants.XMLEncode(_ranks_list.names.get(index)));
			_buffer.append(StringConstants.STR_XML_RANK_LINE_3);
			_buffer.append(_ranks_list.amounts.get(index).intValue());
			_buffer.append(StringConstants.STR_XML_RANK_LINE_4);
		}

		_buffer.append(StringConstants.STR_XML_RANKS_END);
		_buffer.append(StringConstants.STR_XML_LIST_END);
	}

	public void OutputRanksFile(String _filename, StringBuffer _buffer)
	{
		try
		{
			// Open the file
			FileWriter fw = new FileWriter(_filename);

			// Write the html string
			fw.write(_buffer.toString());

			// Close the file
			fw.close();

			// Clear the buffer for re-use.
			_buffer.delete(0, _buffer.length());
		}
		catch(Exception e)
		{
			Output.PrintToScreen("Failed to write ranks file '" + _filename + "'");
		}
	}

	public void SortLists()
	{
		ranks_nation_xp.Sort();
		ranks_nation_xp_monthly.Sort();
		ranks_user_xp.Sort();
		ranks_user_xp_monthly.Sort();
		ranks_user_followers.Sort();
		ranks_user_followers_monthly.Sort();
		ranks_nation_winnings.Sort();
		ranks_nation_winnings_monthly.Sort();
		ranks_nation_latest_tournament.Sort();
		ranks_nation_tournament_trophies.Sort();
		ranks_nation_tournament_trophies_monthly.Sort();
		ranks_nation_level.Sort();
		ranks_nation_rebirths.Sort();
		ranks_nation_quests.Sort();
		ranks_nation_quests_monthly.Sort();
		ranks_nation_energy_donated.Sort();
		ranks_nation_energy_donated_monthly.Sort();
		ranks_nation_manpower_donated.Sort();
		ranks_nation_manpower_donated_monthly.Sort();
		ranks_nation_area.Sort();
		ranks_nation_area_monthly.Sort();
		ranks_nation_captures.Sort();
		ranks_nation_captures_monthly.Sort();
		ranks_nation_medals.Sort();
		ranks_nation_medals_monthly.Sort();
		ranks_nation_raid_earnings.Sort();
		ranks_nation_raid_earnings_monthly.Sort();
		ranks_nation_orb_shard_earnings.Sort();
		ranks_nation_orb_shard_earnings_monthly.Sort();
	}

	public boolean ReadData()
	{
		int file_version;
		boolean result = true;

		String sql = "SELECT " +
		"version, " +
		"goals_token, " +
		"goals_total_awarded, " +
		"goals_ranks, " +
		"goals_ranks_monthly, " +
		"ranks_nation_xp, " +
		"ranks_nation_xp_monthly, " +
		"ranks_user_xp, " +
		"ranks_user_xp_monthly, " +
		"ranks_user_followers, " +
		"ranks_user_followers_monthly, " +
		"ranks_nation_winnings, " +
		"ranks_nation_winnings_monthly, " +
		"ranks_nation_latest_tournament, " +
		"ranks_nation_tournament_trophies, " +
		"ranks_nation_tournament_trophies_monthly, " +
		"ranks_nation_level, " +
		"ranks_nation_rebirths, " +
		"ranks_nation_quests, " +
		"ranks_nation_quests_monthly, " +
		"ranks_nation_energy_donated, " +
		"ranks_nation_energy_donated_monthly, " +
		"ranks_nation_manpower_donated, " +
		"ranks_nation_manpower_donated_monthly, " +
		"ranks_nation_area, " +
		"ranks_nation_area_monthly, " +
		"ranks_nation_captures, " +
		"ranks_nation_captures_monthly, " +
		"ranks_nation_medals, " +
		"ranks_nation_medals_monthly, " +
		"ranks_nation_raid_earnings, " +
		"ranks_nation_raid_earnings_monthly, " +
		"ranks_nation_orb_shard_earnings, " +
		"ranks_nation_orb_shard_earnings_monthly " +
		"FROM " + db_table_name + " where ID= '" + ID + "'";

		try {
			// Create statement for use with the DB.
			Statement stmt = db.createStatement();

			// Get the result set for the sql query
			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				file_version  = rs.getInt("version");
				goals_token  = JSONToIntArray(rs.getString("goals_token"));
				goals_total_awarded  = JSONToFloatArray(rs.getString("goals_total_awarded"));
				goals_ranks  = RanksList.JSONToRanksListArray(rs.getString("goals_ranks"));
				goals_ranks_monthly  = RanksList.JSONToRanksListArray(rs.getString("goals_ranks_monthly"));
				ranks_nation_xp.FromJSON(rs.getString("ranks_nation_xp"));
				ranks_nation_xp_monthly.FromJSON(rs.getString("ranks_nation_xp_monthly"));
				ranks_user_xp.FromJSON(rs.getString("ranks_user_xp"));
				ranks_user_xp_monthly.FromJSON(rs.getString("ranks_user_xp_monthly"));
				ranks_user_followers.FromJSON(rs.getString("ranks_user_followers"));
				ranks_user_followers_monthly.FromJSON(rs.getString("ranks_user_followers_monthly"));
				ranks_nation_winnings.FromJSON(rs.getString("ranks_nation_winnings"));
				ranks_nation_winnings_monthly.FromJSON(rs.getString("ranks_nation_winnings_monthly"));
				ranks_nation_latest_tournament.FromJSON(rs.getString("ranks_nation_latest_tournament"));
				ranks_nation_tournament_trophies.FromJSON(rs.getString("ranks_nation_tournament_trophies"));
				ranks_nation_tournament_trophies_monthly.FromJSON(rs.getString("ranks_nation_tournament_trophies_monthly"));
				ranks_nation_level.FromJSON(rs.getString("ranks_nation_level"));
				ranks_nation_rebirths.FromJSON(rs.getString("ranks_nation_rebirths"));
				ranks_nation_quests.FromJSON(rs.getString("ranks_nation_quests"));
				ranks_nation_quests_monthly.FromJSON(rs.getString("ranks_nation_quests_monthly"));
				ranks_nation_energy_donated.FromJSON(rs.getString("ranks_nation_energy_donated"));
				ranks_nation_energy_donated_monthly.FromJSON(rs.getString("ranks_nation_energy_donated_monthly"));
				ranks_nation_manpower_donated.FromJSON(rs.getString("ranks_nation_manpower_donated"));
				ranks_nation_manpower_donated_monthly.FromJSON(rs.getString("ranks_nation_manpower_donated_monthly"));
				ranks_nation_area.FromJSON(rs.getString("ranks_nation_area"));
				ranks_nation_area_monthly.FromJSON(rs.getString("ranks_nation_area_monthly"));
				ranks_nation_captures.FromJSON(rs.getString("ranks_nation_captures"));
				ranks_nation_captures_monthly.FromJSON(rs.getString("ranks_nation_captures_monthly"));
				ranks_nation_medals.FromJSON(rs.getString("ranks_nation_medals"));
				ranks_nation_medals_monthly.FromJSON(rs.getString("ranks_nation_medals_monthly"));
				ranks_nation_raid_earnings.FromJSON(rs.getString("ranks_nation_raid_earnings"));
				ranks_nation_raid_earnings_monthly.FromJSON(rs.getString("ranks_nation_raid_earnings_monthly"));
				ranks_nation_orb_shard_earnings.FromJSON(rs.getString("ranks_nation_orb_shard_earnings"));
				ranks_nation_orb_shard_earnings_monthly.FromJSON(rs.getString("ranks_nation_orb_shard_earnings_monthly"));
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
		"goals_token = '" + IntArrayToJSON(goals_token) + "', " +
		"goals_total_awarded = '" + FloatArrayToJSON(goals_total_awarded) + "', " +
		"goals_ranks = '" + PrepStringForMySQL(RanksList.RanksListArrayToJSON(goals_ranks)) + "', " +
		"goals_ranks_monthly = '" + PrepStringForMySQL(RanksList.RanksListArrayToJSON(goals_ranks_monthly)) + "', " +
		"ranks_nation_xp = '" + PrepStringForMySQL(ranks_nation_xp.ToJSON()) + "', " +
		"ranks_nation_xp_monthly = '" + PrepStringForMySQL(ranks_nation_xp_monthly.ToJSON()) + "', " +
		"ranks_user_xp = '" + PrepStringForMySQL(ranks_user_xp.ToJSON()) + "', " +
		"ranks_user_xp_monthly = '" + PrepStringForMySQL(ranks_user_xp_monthly.ToJSON()) + "', " +
		"ranks_user_followers = '" + PrepStringForMySQL(ranks_user_followers.ToJSON()) + "', " +
		"ranks_user_followers_monthly = '" + PrepStringForMySQL(ranks_user_followers_monthly.ToJSON()) + "', " +
		"ranks_nation_winnings = '" + PrepStringForMySQL(ranks_nation_winnings.ToJSON()) + "', " +
		"ranks_nation_winnings_monthly = '" + PrepStringForMySQL(ranks_nation_winnings_monthly.ToJSON()) + "', " +
		"ranks_nation_latest_tournament = '" + PrepStringForMySQL(ranks_nation_latest_tournament.ToJSON()) + "', " +
		"ranks_nation_tournament_trophies = '" + PrepStringForMySQL(ranks_nation_tournament_trophies.ToJSON()) + "', " +
		"ranks_nation_tournament_trophies_monthly = '" + PrepStringForMySQL(ranks_nation_tournament_trophies_monthly.ToJSON()) + "', " +
		"ranks_nation_level = '" + PrepStringForMySQL(ranks_nation_level.ToJSON()) + "', " +
		"ranks_nation_rebirths = '" + PrepStringForMySQL(ranks_nation_rebirths.ToJSON()) + "', " +
		"ranks_nation_quests = '" + PrepStringForMySQL(ranks_nation_quests.ToJSON()) + "', " +
		"ranks_nation_quests_monthly = '" + PrepStringForMySQL(ranks_nation_quests_monthly.ToJSON()) + "', " +
		"ranks_nation_energy_donated = '" + PrepStringForMySQL(ranks_nation_energy_donated.ToJSON()) + "', " +
		"ranks_nation_energy_donated_monthly = '" + PrepStringForMySQL(ranks_nation_energy_donated_monthly.ToJSON()) + "', " +
		"ranks_nation_manpower_donated = '" + PrepStringForMySQL(ranks_nation_manpower_donated.ToJSON()) + "', " +
		"ranks_nation_manpower_donated_monthly = '" + PrepStringForMySQL(ranks_nation_manpower_donated_monthly.ToJSON()) + "', " +
		"ranks_nation_area = '" + PrepStringForMySQL(ranks_nation_area.ToJSON()) + "', " +
		"ranks_nation_area_monthly = '" + PrepStringForMySQL(ranks_nation_area_monthly.ToJSON()) + "', " +
		"ranks_nation_captures = '" + PrepStringForMySQL(ranks_nation_captures.ToJSON()) + "', " +
		"ranks_nation_captures_monthly = '" + PrepStringForMySQL(ranks_nation_captures_monthly.ToJSON()) + "', " +
		"ranks_nation_medals = '" + PrepStringForMySQL(ranks_nation_medals.ToJSON()) + "', " +
		"ranks_nation_medals_monthly = '" + PrepStringForMySQL(ranks_nation_medals_monthly.ToJSON()) + "', " +
		"ranks_nation_raid_earnings = '" + PrepStringForMySQL(ranks_nation_raid_earnings.ToJSON()) + "', " +
		"ranks_nation_raid_earnings_monthly = '" + PrepStringForMySQL(ranks_nation_raid_earnings_monthly.ToJSON()) + "', " +
		"ranks_nation_orb_shard_earnings = '" + PrepStringForMySQL(ranks_nation_orb_shard_earnings.ToJSON()) + "', " +
		"ranks_nation_orb_shard_earnings_monthly = '" + PrepStringForMySQL(ranks_nation_orb_shard_earnings_monthly.ToJSON()) + "' " +
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

		/*
		//// TEMP -- modify existing column
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY ranks_nation_xp MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY ranks_nation_xp_monthly MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY ranks_user_xp MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY ranks_user_xp_monthly MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY ranks_user_followers MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY ranks_user_followers_monthly MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY ranks_nation_winnings MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY ranks_nation_winnings_monthly MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY ranks_nation_latest_tournament MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY ranks_nation_tournament_trophies MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY ranks_nation_tournament_trophies_monthly MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY ranks_nation_level MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY ranks_nation_rebirths MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY ranks_nation_quests MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY ranks_nation_quests_monthly MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY ranks_nation_energy_donated MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY ranks_nation_energy_donated_monthly MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY ranks_nation_manpower_donated MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY ranks_nation_manpower_donated_monthly MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY ranks_nation_area MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY ranks_nation_area_monthly MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY ranks_nation_captures MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY ranks_nation_captures_monthly MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY ranks_nation_medals MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY ranks_nation_medals_monthly MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY ranks_nation_raid_earnings MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY ranks_nation_raid_earnings_monthly MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY ranks_nation_orb_shard_earnings MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY ranks_nation_orb_shard_earnings_monthly MEDIUMTEXT", true, false);
		*/

		// Attempt to create each column
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD goals_token TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD goals_total_awarded TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD goals_ranks MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD goals_ranks_monthly MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD ranks_nation_xp MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD ranks_nation_xp_monthly MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD ranks_user_xp MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD ranks_user_xp_monthly MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD ranks_user_followers MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD ranks_user_followers_monthly MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD ranks_nation_winnings MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD ranks_nation_winnings_monthly MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD ranks_nation_latest_tournament MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD ranks_nation_tournament_trophies MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD ranks_nation_tournament_trophies_monthly MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD ranks_nation_level MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD ranks_nation_rebirths MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD ranks_nation_quests MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD ranks_nation_quests_monthly MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD ranks_nation_energy_donated MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD ranks_nation_energy_donated_monthly MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD ranks_nation_manpower_donated MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD ranks_nation_manpower_donated_monthly MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD ranks_nation_area MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD ranks_nation_area_monthly MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD ranks_nation_captures MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD ranks_nation_captures_monthly MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD ranks_nation_medals MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD ranks_nation_medals_monthly MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD ranks_nation_raid_earnings MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD ranks_nation_raid_earnings_monthly MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD ranks_nation_orb_shard_earnings MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD ranks_nation_orb_shard_earnings_monthly MEDIUMTEXT", true, false);
	}

	public static void DeleteDBTable()
	{
		// Attempt to delete the DB table
		ExecuteUpdate(db, "DROP TABLE " + db_table_name, true, true);
	}
}
