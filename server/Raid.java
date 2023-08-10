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
import java.util.concurrent.ThreadLocalRandom;
import WOCServer.*;

public class Raid
{
	public static final int RAID_ID_BASE = 1000000000;
	public static final int RAID_ID_INIT_RANGE = 10000; // Was 10 for testing
	public static final int RAID_HISTORY_DURATION = 3 * Constants.SECONDS_PER_DAY;
	public static final int RAID_MAX_DURATION = 15 * Constants.SECONDS_PER_MINUTE;
	public static final int RAID_CANDIDATE_REVIEW_PERIOD = 5 * Constants.SECONDS_PER_MINUTE;
	public static final float MIN_MANPOWER_FRACTION_TO_START_RAID = 0.9f;
	public static final float MANPOWER_FRACTION_COST_TO_RESTART_RAID = 0.01f;
	public static final float TARGET_AVERAGE_RESULT_STARS = 2f;
	public static final float BASE_MEDALS_PER_STAR = 10;
	public static final float MEDAL_RATING_INTERVAL = 200;
	public static final float MEDALS_ADJUSTMENT_RANGE = 500;
	public static final float MEDALS_RANGE_TOP = 2000;
	public static final int MEDALS_PER_BUCKET = 100;
	public static final int RAID_DEFENDER_SHIELD_DURATION = 6 * Constants.SECONDS_PER_HOUR;
	public static final int MAX_NUM_PREV_RAID_CANDIDATES = 50;
	public static final int MATCHMAKING_MAX_BUCKET_DIF = 4;
	public static final int MEDALS_PER_LEAGUE = 200;

	// Raid replay events
	public static final int RAID_EVENT_SET_NATION_ID = 0;
	public static final int RAID_EVENT_CLEAR_NATION_ID = 1;
	public static final int RAID_EVENT_SET_OBJECT_ID = 2;
	public static final int RAID_EVENT_TOWER_ACTION = 3;
	public static final int RAID_EVENT_END = 4;
	public static final int RAID_EVENT_EXT_DATA = 5;
	public static final int RAID_EVENT_SALVAGE = 6;
	public static final int RAID_EVENT_COMPLETE = 7;
	public static final int RAID_EVENT_BATTLE = 8;
	public static final int RAID_EVENT_TRIGGER_INERT = 9;

	public static ArrayList<ArrayList<Integer>> raid_candidates = new ArrayList<ArrayList<Integer>>();

	public static void OnBuildShard(NationData _nationData)
	{
		// If this nation is not yet considered raid-eligible...
		if (!_nationData.raid_eligible)
		{
			// If the nation has built all three shards...
			if ((_nationData.GetBuildCount(200) == 1) && (_nationData.GetBuildCount(201) == 1) && (_nationData.GetBuildCount(202) == 1))
			{
				// Mark this nation as being raid-eligible.
				_nationData.raid_eligible = true;

				// Add this nation to the list of raid candidates.
				AddRaidCandidate(_nationData.ID, _nationData.raid_defender_medals);
			}
		}
	}

	public static void PlaceShard(NationData _nationData, LandMap _landmap, int _buildID)
	{
		int attempt, x, y;
		BlockData block_data;

		// Attempt to place the shard within a square already belonging to the given nation.
		for (attempt = 0; attempt < 50; attempt++)
		{
			// Pick a random position within the given nation's homeland footprint.
			x = ThreadLocalRandom.current().nextInt(_nationData.homeland_footprint.x0, _nationData.homeland_footprint.x1 + 1);
			y = ThreadLocalRandom.current().nextInt(_nationData.homeland_footprint.y0, _nationData.homeland_footprint.y1 + 1);
			block_data = _landmap.GetBlockData(x, y);

			// Skip this block if it doesn't exist or if it is not occupied by the given nation, or if it has extended data (meaning it already has an object in it).
			if ((block_data == null) || (block_data.nationID != _nationData.ID) || (block_data.BlockHasExtendedData())) {
				continue;
			}

			// Build the shard object at this location.
			Gameplay.Build(_landmap.ID, _landmap.GetBlockExtendedData(x, y, true), _nationData, BuildData.GetBuildData(_buildID), 0f);

			// Update the raid system for the building of this shard.
			Raid.OnBuildShard(_nationData);

			// Mark nation and block data to be updated
			DataManager.MarkBlockForUpdate(_landmap, x, y);
			DataManager.MarkForUpdate(_nationData);

			//Output.PrintToScreen("PlaceShard() placed in occupied square " + x + "," + y + " on attempt " + attempt);

			// The shard has been placed, so no need to continue. Return.
			return;
		}

		// Attempt to place the shard within a square that does not yet belong to the given nation.
		for (attempt = 0; attempt < 50; attempt++)
		{
			// Pick a random position within the homeland map.
			x = ThreadLocalRandom.current().nextInt(0, _landmap.width);
			y = ThreadLocalRandom.current().nextInt(0, _landmap.height);
			block_data = _landmap.GetBlockData(x, y);

			// Skip this block if it doesn't exist, if it's not habitble terrain, or if it is already occupied.
			if ((block_data == null) || ((block_data.terrain != Constants.TERRAIN_FLAT_LAND) && (block_data.terrain != Constants.TERRAIN_BEACH)) || (block_data.nationID != -1)) {
				continue;
			}

			// Have the given nation occupy this block.
			World.SetBlockNationID(_landmap, x, y, _nationData.ID, false, false, -1, 0);

			// Build the shard object at this location.
			Gameplay.Build(_landmap.ID, _landmap.GetBlockExtendedData(x, y, true), _nationData, BuildData.GetBuildData(_buildID), 0f);

			// Update the raid system for the building of this shard.
			Raid.OnBuildShard(_nationData);

			// Mark nation and block data to be updated
			DataManager.MarkBlockForUpdate(_landmap, x, y);
			DataManager.MarkForUpdate(_nationData);

			//Output.PrintToScreen("PlaceShard() placed in unoccupied square " + x + "," + y + " on attempt " + attempt);

			// The shard has been placed, so no need to continue. Return.
			return;
		}
	}

	public static void OnRaidCommand(StringBuffer _output_buffer, int _userID, int _targetNationID)
	{
		// If there is no raid going yet, or previous raid has begun and ended, and user isn't in raid, and there is enough manpower to start a new raid, start a new raid and have the user join it.
		// If there is a raid going and the user isn't in it, join it.
		// If there is a raid going and the user is in it:
		//   If the raid has not yet begun, clear the raid and start a new one, sending all users in the raid to that new one.
		//   If the raid has begun (land has been taken) but has not yet ended, end the raid (but do not clear it or remove users).
		//   If the raid has ended already, do nothing (use switch_map to return users to their homeland map when they choose to).

		RaidData raidData = null;
		NationData defenderNationData = null;
		int defenderNationID = -1;

		// Get the user's data
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		// Get the user's nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, userData.nationID, false);

		// If this nation is not raid-eligible, do nothing.
		if (!nationData.raid_eligible) {
			return;
		}

		// Get the nation's current raid ID.
		int raidID = nationData.raidID;

		if (raidID != -1)
		{
			// Get the data for the nation's raid already in progress.
			raidData = (RaidData)DataManager.GetData(Constants.DT_RAID, raidID, false);
		}

		// Determine whether the naton has enough manpower to start a raid.
		boolean enough_manpower_to_start_raid = (nationData.homeland_footprint.manpower >= ((float)nationData.GetFinalManpowerMax(nationData.homeland_mapID) * MIN_MANPOWER_FRACTION_TO_START_RAID));

		// TESTING
		if (raidData != null) {
			Output.PrintToScreen("User mapID: " + userData.mapID + ", cur raid: " + raidData.ID + ", begun: " + ((raidData.flags & RaidData.RAID_FLAG_BEGUN) != 0) + ", finished: " + ((raidData.flags & RaidData.RAID_FLAG_FINISHED) != 0) + ", attacker_nationID: " + raidData.attacker_nationID + ", defender_nationID: " + raidData.defender_nationID);
		}

		// Determine whether to start a new raid. Do so if:
		//   - There is no current raid, and there is enough manpower to start a raid; or
		//   - The user is not viewing the current raid, and the current raid has both begun and ended, and there is enough manpower to start a raid; or
		//   - The user is viewing the current raid, and the current raid has not yet begun, and there is enough manpower to start a new raid.
		//   - The RaidData is corrupt.
		boolean start_new_raid =  ((raidData == null) && enough_manpower_to_start_raid) ||
			                        ((raidData != null) && (userData.mapID != raidID) && ((raidData.flags & RaidData.RAID_FLAG_BEGUN) != 0) && (((raidData.flags & RaidData.RAID_FLAG_FINISHED) != 0) || ((Constants.GetTime() - raidData.begin_time) >= RAID_MAX_DURATION)) && enough_manpower_to_start_raid) ||
															((raidData != null) && (userData.mapID == raidID) && ((raidData.flags & RaidData.RAID_FLAG_BEGUN) == 0) && enough_manpower_to_start_raid) ||
															((raidData != null) && ((raidData.attacker_nationID <= 0) || (raidData.defender_nationID <= 0)));

		// Determine whether this is a restart (and so will have a cost in manpower).
		boolean restarting_raid = (raidData != null) && (userData.mapID == raidID) && ((raidData.flags & RaidData.RAID_FLAG_BEGUN) == 0) && enough_manpower_to_start_raid;

		Output.PrintToScreen("enough_manpower_to_start_raid: " + enough_manpower_to_start_raid + ", start_new_raid: " + start_new_raid);

		// Start a new raid if appropriate.
		if (start_new_raid)
		{
			// Determine ID of the defender nation whose homeland to raid.
			defenderNationID = (_targetNationID == -1) ? FindRaidMatch(nationData) : _targetNationID;

			if (defenderNationID == -1)
			{
				// Return message to client stating that no raid is available right now.
				OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_message_no_raid_available"));
				return;
			}

			// If the determined defenderNationID is already in the attacker nation's raid_prev_candidates list, remove it.
			nationData.raid_prev_candidates.remove((Integer)defenderNationID);

			// Record the determined defenderNationID in the attacker nation's raid_prev_candidates list.
			nationData.raid_prev_candidates.add(defenderNationID);

			// If the attacker nation's raid_prev_candidates list is too long, remove the oldest raid candidates from it.
			while (nationData.raid_prev_candidates.size() > MAX_NUM_PREV_RAID_CANDIDATES) {
				nationData.raid_prev_candidates.remove(0);
			}

			// Determine ID for this new raid.
			int prevRaidID = raidID;
			for (raidID = RAID_ID_BASE + ThreadLocalRandom.current().nextInt(0, RAID_ID_INIT_RANGE);; raidID++)
			{
				// Do not re-use the same raid ID, because that would result in the map ID remaining the same, and the clients wouldn't realize they're switching to a new map.
				if (raidID == prevRaidID) {
					continue;
				}

				// Get (or create) the data for the raid with this ID.
				raidData = (RaidData)DataManager.GetData(Constants.DT_RAID, raidID, true);

				// If this raid has not yet started (it's new) or else has started long enough ago that it no longer needs to be kept in storage, or else has started a while ago but battle has not begun, use this raid ID; exit loop.
				if ((raidData.start_time == 0) || ((raidData.start_time > 0) && ((Constants.GetTime() - raidData.start_time) > RAID_HISTORY_DURATION)) || ((raidData.begin_time == 0) && ((Constants.GetTime() - raidData.start_time) > RAID_MAX_DURATION))) {
					break;
				}
			}

			Output.PrintToScreen("Starting raidID " + raidID + ", defenderNationID: " + defenderNationID);

			// Before re-using this raid, remove it from its previous attacker and defender nations' raid logs.
			RemoveRaidFromLogs(raidData.attacker_nationID, raidID);
			RemoveRaidFromLogs(raidData.defender_nationID, raidID);

			if (restarting_raid)
			{
				// Subtract the cost for restarting the raid from the nation's homeland manpower.
				nationData.homeland_footprint.manpower -= ((float)nationData.GetFinalManpowerMax(nationData.homeland_mapID) * MANPOWER_FRACTION_COST_TO_RESTART_RAID);
			}

			// Get the data for the nation whose homeland will be raided.
			defenderNationData = (NationData)DataManager.GetData(Constants.DT_NATION, defenderNationID, false);

			// Get the raid source homeland landmap.
			LandMap source_landmap = Homeland.GetHomelandMap(defenderNationID);

			// If the defender's homeland is missing any of the shards, place them.
			if (defenderNationData.GetBuildCount(200) == 0) PlaceShard(defenderNationData, source_landmap, 200);
			if (defenderNationData.GetBuildCount(201) == 0) PlaceShard(defenderNationData, source_landmap, 201);
			if (defenderNationData.GetBuildCount(202) == 0) PlaceShard(defenderNationData, source_landmap, 202);

			// Record info about this raid
			raidData.landmapID = raidID;
			raidData.attacker_nationID = nationData.ID;
			raidData.defender_nationID = defenderNationID;
			raidData.attacker_nationName = nationData.name;
			raidData.defender_nationName = defenderNationData.name;
			raidData.homeland_mapID = defenderNationData.homeland_mapID;
			raidData.defender_starting_area = defenderNationData.homeland_footprint.area;
			raidData.attacker_start_medals = nationData.raid_attacker_medals + nationData.raid_defender_medals;
			raidData.defender_start_medals = defenderNationData.raid_attacker_medals + defenderNationData.raid_defender_medals;
			raidData.start_time = 0;
			raidData.begin_time = 0;
			raidData.end_time = 0;
			raidData.flags = 0;

			// Determine difference between defender's defense rating and attacker's attack rating.
			float challenge_difference = defenderNationData.raid_defender_medals - nationData.raid_attacker_medals;

			// Determine the attacker's medals delta for 0 stars.
			float min_medal_delta = -TARGET_AVERAGE_RESULT_STARS * BASE_MEDALS_PER_STAR;
			min_medal_delta = Logistic(-challenge_difference / MEDAL_RATING_INTERVAL) * (2 * min_medal_delta);

			// Determine the attacker's medals delta for 5 stars.
			float max_medal_delta = (5f - TARGET_AVERAGE_RESULT_STARS) * BASE_MEDALS_PER_STAR;
			max_medal_delta = Logistic(challenge_difference / MEDAL_RATING_INTERVAL) * (2 * max_medal_delta);

			// Determine min and max medal delta for attacker.
			raidData.attacker_0_star_medal_delta = DetermineAdjustedMedalDelta(nationData.raid_attacker_medals, min_medal_delta);
			raidData.attacker_5_star_medal_delta = DetermineAdjustedMedalDelta(nationData.raid_attacker_medals, max_medal_delta);

			// Determine min and max medal delta for defender.
			raidData.defender_0_star_medal_delta = DetermineAdjustedMedalDelta(defenderNationData.raid_defender_medals, -min_medal_delta);
			raidData.defender_5_star_medal_delta = DetermineAdjustedMedalDelta(defenderNationData.raid_defender_medals, -max_medal_delta);

			//Output.PrintToScreen("Attacker has " + nationData.raid_attacker_medals + " attacker medals. 0 stars: " + raidData.attacker_0_star_medal_delta + ", 5 stars: " + raidData.attacker_5_star_medal_delta + ".");
			//Output.PrintToScreen("Defender has " + defenderNationData.raid_defender_medals + " defender medals. 0 stars: " + raidData.defender_0_star_medal_delta + ", 5 stars: " + raidData.defender_5_star_medal_delta + ".");

			// Get the attacker's league data.
			LeagueData attacker_league = GetNationLeague(nationData);

			// Determine any penalty for the defender being much weaker than the attacker. The defender's trophy count ranging from 0 to half of the attacker's trophy count, maps to a penalty factor of 0 to 1, with minimum factor being 0.05.
			float weak_defender_penalty = (nationData.raid_attacker_medals == 0) ? 1f : Math.max(0.05f, Math.min(1f, ((float)defenderNationData.raid_defender_medals / (float)nationData.raid_attacker_medals)));

			// Record the attacker's max reward values (for if they get 5 stars). Factor in any penalty for the defender being much weaker than the attacker.
			raidData.max_reward_credits = (int)((float)attacker_league.raid_reward_credits * weak_defender_penalty + 0.5f);
			raidData.max_reward_xp = (int)((float)attacker_league.raid_reward_xp * weak_defender_penalty + 0.5f);
			raidData.max_reward_rebirth = (int)((float)attacker_league.raid_reward_rebirth * weak_defender_penalty + 0.5f);

			// Initialize the raid's replay.
			raidData.replay.setLength(0);
			Constants.EncodeString(raidData.replay, "raid_replay");
			Constants.EncodeNumber(raidData.replay, raidData.attacker_nationID, 5);
			Constants.EncodeNumber(raidData.replay, raidData.defender_nationID, 5);
			Constants.EncodeUnsignedNumber(raidData.replay, raidData.defender_starting_area, 2);
			Display.EncodeNationData(raidData.replay, nationData);
			Display.EncodeNationData(raidData.replay, defenderNationData);

			// Record the nation's raid ID.
			nationData.raidID = raidID;

			// Get or create the raid landmap.
			LandMap raid_landmap = DataManager.GetLandMap(raidID, true);

			// Fill in the new raid map's LandMapInfo.
			raid_landmap.info.sourceMapID = source_landmap.info.sourceMapID;
			raid_landmap.info.skin = source_landmap.info.skin;
			DataManager.MarkForUpdate(raid_landmap.info);

			// Set the new size of the land map, and insert its blocks into the database.
			raid_landmap.SetSize(source_landmap.width, source_landmap.height, true);

			// Copy the block data from the source landmap to the new landmap.
			raid_landmap.Copy(source_landmap);

			// Copy the defending nation's raid footprint from their homeland footprint.
			raidData.defender_footprint.Copy(defenderNationData.homeland_footprint);

			// Reset the attacking nation's raid footprint, and copy in appropriate values from their homeland.
			raidData.attacker_footprint.Reset();
			raidData.attacker_footprint.prev_buy_manpower_day = 0;
			raidData.attacker_footprint.buy_manpower_day_amount = 0;
			raidData.attacker_footprint.energy_burn_rate = 0;
			raidData.attacker_footprint.manpower = nationData.homeland_footprint.manpower;

			// Determine the attacker nation's initial geographic efficiency on the raid landmap.
			nationData.DetermineGeographicEfficiency(raidID);

			// Record the manpower cost of this raid, if it is begun.
			raidData.manpower_cost = (int)nationData.homeland_footprint.manpower;

			// Place a square of the raiding nation on the coast.
			int[] coords = new int[2];
			World.PlaceNationOnBeach(raid_landmap, nationData.ID, coords);

			// Record initial placement of attacker nation in the replay history.
			RecordEvent_SetNationID(raidID, coords[0], coords[1], nationData.ID, 0);

			// Record that the raid has started (do this after placing the attacker's square, so that won't count as the first attack).
			raidData.start_time = Constants.GetTime();

			// Send to the new raid map any of the attacking nation's users who are currently viewing a raid map.
			for (int user_index = 0; user_index < nationData.users.size(); user_index++)
			{
				// Get the current member user's data
				UserData curUserData = (UserData)DataManager.GetData(Constants.DT_USER, nationData.users.get(user_index), false);

				// Center the user's raidland view on the nation in the raid map. Center their current view if they're logged in and looking at the raid map, otherwise center their stored view.
				if ((userData.mapID >= Raid.RAID_ID_BASE) && (userData.client_thread != null) && userData.client_thread.UserIsInGame())
				{
					// Remove the user from its current map.
					Display.ResetUserView(curUserData);

					// Set the user's mapID to the new raid.
					curUserData.mapID = raidID;

					// Set the user's view positon on the new raid map.
					Display.CenterViewOnBlock(curUserData.ID, coords[0], coords[1]);
				}
				else
				{
					// Set only the user's stored view position on the raid map.
					Display.SetUserStoredView(curUserData, raidID, coords[0], coords[1]);
				}

				// Mark the current user's data to be updated
				DataManager.MarkForUpdate(curUserData);
			}

			// Broadcast an update event to the attacking nation.
			OutputEvents.BroadcastUpdateEvent(raidData.attacker_nationID);

			// Mark the RaidData to be updated.
			DataManager.MarkForUpdate(raidData);

			// Mark the user's nation's data to be updated.
			DataManager.MarkForUpdate(nationData);

			Output.PrintToScreen("New raid created.");
		}

		if (userData.mapID == raidID)
		{
			if (((raidData.flags & RaidData.RAID_FLAG_BEGUN) != 0) && (((raidData.flags & RaidData.RAID_FLAG_FINISHED) == 0) && ((Constants.GetTime() - raidData.begin_time) < RAID_MAX_DURATION)))
			{
				// End the raid.
				FinishRaid(raidData);
				Output.PrintToScreen("Finished raid.");
			}
		}
		else if (raidID != -1)
		{
			// Remove the user from its current map.
			Display.ResetUserView(userData);

			// Switch the user's view to the raid map.
			userData.mapID = raidID;
			Display.SetUserViewForMap(userData, _output_buffer);

			Output.PrintToScreen("Switched user to raid.");
		}

		// Broadcast raid event to the attacking nation.
		OutputEvents.BroadcastRaidStatusEvent(nationData, 0);
	}

	public static void OnSetBlockNationID(LandMap _land_map, int _x, int _y, BlockData _block_data, BlockExtData _block_ext_data, NationData _formerNationData, NationData _nationData, int _delay)
	{
		// Get the LandMap's corresponding RaidData.
		RaidData raidData = _land_map.GetRaidData();

		// Record the raid's flags at the start.
		int flags_at_start = raidData.flags;

		// Get the IDs of the new and former nations in this block.
		int nationID = (_nationData == null) ? -1 : _nationData.ID;
		int formerNationID = (_formerNationData == null) ? -1 : _formerNationData.ID;

		if (raidData == null)
		{
			Output.PrintToScreen("ERROR: Raid.OnSetBlockNationID(): raidData is null for LandMap with ID " + _land_map.ID);
			return;
		}

		// Get the attacker's nation's data
		NationData attackerNationData = (NationData)DataManager.GetData(Constants.DT_NATION, raidData.attacker_nationID, false);

		if ((_block_ext_data != null) && ((_block_ext_data.objectID == 200) || (_block_ext_data.objectID == 201) || (_block_ext_data.objectID == 202)))
		{
			if (_block_ext_data.objectID == 200) // Red shard
			{
				if (nationID == raidData.attacker_nationID) {
					raidData.flags = raidData.flags | RaidData.RAID_FLAG_RED_SHARD; // Add flag
				} else if (formerNationID == raidData.attacker_nationID) {
					raidData.flags = raidData.flags & (~RaidData.RAID_FLAG_RED_SHARD); // Remove flag
				}
			}
			else if (_block_ext_data.objectID == 201) // Green shard
			{
				if (nationID == raidData.attacker_nationID) {
					raidData.flags = raidData.flags | RaidData.RAID_FLAG_GREEN_SHARD; // Add flag
				} else if (formerNationID == raidData.attacker_nationID) {
					raidData.flags = raidData.flags & (~RaidData.RAID_FLAG_GREEN_SHARD); // Remove flag
				}
			}
			else if (_block_ext_data.objectID == 202) // Blue shard
			{
				if (nationID == raidData.attacker_nationID) {
					raidData.flags = raidData.flags | RaidData.RAID_FLAG_BLUE_SHARD; // Add flag
				} else if (formerNationID == raidData.attacker_nationID) {
					raidData.flags = raidData.flags & (~RaidData.RAID_FLAG_BLUE_SHARD); // Remove flag
				}
			}
		}

		// If the defending nation has gained or lost land...
		if ((nationID == raidData.defender_nationID) || (formerNationID == raidData.defender_nationID))
		{
			// Check whether the defender has lost 100% of their land.
			if (raidData.defender_footprint.area == 0)
			{
				raidData.flags = raidData.flags | RaidData.RAID_FLAG_100_PERCENT; // Add flag
				//FinishRaid(raidData);
			} else {
				raidData.flags = raidData.flags & (~RaidData.RAID_FLAG_100_PERCENT); // Remove flag
			}

			// Check whether the defender has lost 50% of their land.
			if (raidData.defender_footprint.area <= (raidData.defender_starting_area / 2)) {
				raidData.flags = raidData.flags | RaidData.RAID_FLAG_50_PERCENT; // Add flag
			} else {
				raidData.flags = raidData.flags & (~RaidData.RAID_FLAG_50_PERCENT); // Remove flag
			}
		}

		//Output.PrintToScreen("Raid.OnSetBlockNationID() flags at end: " + raidData.flags + ", defender area: " + raidData.defender_footprint.area + ", starting area: " + raidData.defender_starting_area);

		// If any of the raid's flags have changed, broadcast raid event to the attacking nation.
		if (raidData.flags != flags_at_start) {
			OutputEvents.BroadcastRaidStatusEvent(attackerNationData, _delay);
		}

		// Mark the RaidData to be updated (to store changes to footprints, and any other changes.)
		DataManager.MarkForUpdate(raidData);
	}

	public static void OnMapClick(LandMap _land_map, NationData _nationData)
	{
		// Get the LandMap's corresponding RaidData.
		RaidData raidData = _land_map.GetRaidData();

		// The given nation must be the raid's attacker nation.
		if (_nationData.ID != raidData.attacker_nationID) {
			return;
		}

		// If the raid has not yet begun, record that it has begun.
		if ((raidData.flags & RaidData.RAID_FLAG_BEGUN) == 0)
		{
			// Begin the raid.
			BeginRaid(raidData);

			// Broadcast raid status immediately.
			OutputEvents.BroadcastRaidStatusEvent(_nationData, 0);
		}

		// Finish the raid if the attacker has run out of manpower.
		if (raidData.attacker_footprint.manpower == 0)
		{
			// Finish the raid.
			FinishRaid(raidData);

			// Broadcast raid status after delay.
			OutputEvents.BroadcastRaidStatusEvent(_nationData, Constants.BATTLE_DURATION);
		}

		// Finish the raid if it has timed out.
		else if (Constants.GetTime() >= raidData.end_time)
		{
			// Finish the raid.
			FinishRaid(raidData);

			// Broadcast raid status after delay.
			OutputEvents.BroadcastRaidStatusEvent(_nationData, 0);
		}

		// Finish the raid if the defender's last square has been taken.
		// Do this here rather than in OnSetBlockNationID() so that the final event will have a chance to be added to the replay.
		else if (raidData.defender_footprint.area == 0)
		{
			// Finish the raid.
			FinishRaid(raidData);

			// Broadcast raid status after delay.
			OutputEvents.BroadcastRaidStatusEvent(_nationData, Constants.BATTLE_DURATION);
		}

		// Finish the raid if the attacker's last square has been taken.
		// Do this here rather than in OnSetBlockNationID() so that the final event will have a chance to be added to the replay.
		// Also, do this here rather than in response to a tower action, because a tower action is always caused by a click, and after the tower action takes place the attacker may still occupy the attcked square.
		else if (raidData.attacker_footprint.area == 0)
		{
			// Finish the raid.
			FinishRaid(raidData);

			// Broadcast raid status after delay.
			OutputEvents.BroadcastRaidStatusEvent(_nationData, Constants.BATTLE_DURATION);
		}
	}

	public static void OnBuild(LandMap _land_map, NationData _nationData)
	{
		// Get the LandMap's corresponding RaidData.
		RaidData raidData = _land_map.GetRaidData();

		// The given nation must be the raid's attacker nation.
		if (_nationData.ID != raidData.attacker_nationID) {
			return;
		}

		// If the raid has not yet begun, record that it has begun.
		if ((raidData.flags & RaidData.RAID_FLAG_BEGUN) == 0)
		{
			// Begin the raid.
			BeginRaid(raidData);

			// Broadcast raid status immediately.
			OutputEvents.BroadcastRaidStatusEvent(_nationData, 0);
		}
	}

	public static void OnRaidTimeout(StringBuffer _output_buffer, int _userID)
	{
		// Get the user's data
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		// Get the user's nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, userData.nationID, false);

		// Get the nation's current raid ID.
		int raidID = nationData.raidID;

		if (nationData.raidID == -1) {
			return;
		}

		// Get the data for the nation's raid already in progress.
		RaidData raidData = (RaidData)DataManager.GetData(Constants.DT_RAID, raidID, false);

		if (raidData == null) {
			return;
		}

		// If the raid has not yet been marked as finished...
		if ((raidData.flags & RaidData.RAID_FLAG_FINISHED) == 0)
		{
			// Finish the raid.
			FinishRaid(raidData);

			// Broadcast raid status after delay.
			OutputEvents.BroadcastRaidStatusEvent(nationData, 0);
		}
	}

	public static void OnUserStoppedViewingRaid(UserData _userData)
	{
		// Get the user's nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _userData.nationID, false);

		if ((nationData == null) || (nationData.raidID <= 0)) {
			return;
		}

		// Get the data for this nation's current raid.
		RaidData raidData = (RaidData)DataManager.GetData(Constants.DT_RAID, nationData.raidID, false);

		// If this nation's raid has not yet begun, or has finished...
		if (((raidData.flags & RaidData.RAID_FLAG_BEGUN) == 0) ||  ((raidData.flags & RaidData.RAID_FLAG_FINISHED) != 0))
		{
			// Determine whether any of this nation's logged-in users are viewing the raid map.
			boolean raid_empty = true;
			for (int user_index = 0; user_index < nationData.users.size(); user_index++)
			{
				// Get the current member user's data
				UserData curUserData = (UserData)DataManager.GetData(Constants.DT_USER, nationData.users.get(user_index), false);

				// If this user is currently viewing the raid map, and is in the game, then the raid is not yet empty.
				if ((_userData.mapID == nationData.raidID) && (_userData.client_thread != null) && _userData.client_thread.UserIsInGame())
				{
					raid_empty = false;
					break;
				}
			}

			//Output.PrintToScreen("raid_empty: " + raid_empty);

			if (raid_empty)
			{
				// Clear the nation's raid.
				ClearRaid(raidData, nationData);

				// Broadcats raid status event to the attacking nation.
				OutputEvents.BroadcastRaidStatusEvent(nationData, 0);
			}
		}
	}

	public static void BeginRaid(RaidData _raidData)
	{
		if ((_raidData.start_time > 0) && ((_raidData.flags & RaidData.RAID_FLAG_BEGUN) == 0))
		{
			// Record the flag
			_raidData.flags = _raidData.flags | RaidData.RAID_FLAG_BEGUN;

			// Record the time when the raid begins.
			_raidData.begin_time = Constants.GetTime();

			// Record the raid's end time (when it times out).
			_raidData.end_time = Constants.GetTime() + RAID_MAX_DURATION;

			// Get the attacking nation's data
			NationData attackerNationData = (NationData)DataManager.GetData(Constants.DT_NATION, _raidData.attacker_nationID, false);

			// Remove the manpower cost from the attacking nation's homeland.
			attackerNationData.homeland_footprint.manpower = Math.max(0, attackerNationData.homeland_footprint.manpower - _raidData.manpower_cost);

			// Get the defending nation's data
			NationData defenderNationData = (NationData)DataManager.GetData(Constants.DT_NATION, _raidData.defender_nationID, false);

			// Add a temporary shield to the defending nation, for the max duration of this raid.
			defenderNationData.raid_shield_end_time = Math.max(defenderNationData.raid_shield_end_time, Constants.GetTime() + RAID_MAX_DURATION);

			// Broadcast an update event to the attacking nation.
			OutputEvents.BroadcastUpdateEvent(_raidData.attacker_nationID);

			// Mark both nations' data to be updated.
			DataManager.MarkForUpdate(attackerNationData);
			DataManager.MarkForUpdate(defenderNationData);

			// Mark the RaidData to be updated.
			DataManager.MarkForUpdate(_raidData);

			Output.PrintToScreen("Raid " + _raidData.ID + " begun.");
		}
	}

	public static void FinishRaid(RaidData _raidData)
	{
		// If the raid is already finished, do nothing.
		if ((_raidData.flags & RaidData.RAID_FLAG_FINISHED) != 0) {
			return;
		}

		// Get both nations' data
		NationData attackerNationData = (NationData)DataManager.GetData(Constants.DT_NATION, _raidData.attacker_nationID, false);
		NationData defenderNationData = (NationData)DataManager.GetData(Constants.DT_NATION, _raidData.defender_nationID, false);

		// Record the flag
		_raidData.flags = _raidData.flags | RaidData.RAID_FLAG_FINISHED;

		// Record the actual end time
		_raidData.end_time = Constants.GetTime();

		// Determine percentage defeated
		_raidData.percentage_defeated = Math.max(0, 100 - (int)(_raidData.defender_footprint.area * 100f / Math.max(1, _raidData.defender_starting_area)));

		// Set the defender's shard fill level to 0 for any shards that have been captured in this raid.
		if ((_raidData.flags & RaidData.RAID_FLAG_RED_SHARD) != 0) defenderNationData.shard_red_fill = 0f;
		if ((_raidData.flags & RaidData.RAID_FLAG_GREEN_SHARD) != 0) defenderNationData.shard_green_fill = 0f;
		if ((_raidData.flags & RaidData.RAID_FLAG_BLUE_SHARD) != 0) defenderNationData.shard_blue_fill = 0f;

		// Determine the number of stars
		int num_stars = 0;
		if ((_raidData.flags & RaidData.RAID_FLAG_50_PERCENT) != 0) num_stars++;
		if ((_raidData.flags & RaidData.RAID_FLAG_100_PERCENT) != 0) num_stars++;
		if ((_raidData.flags & RaidData.RAID_FLAG_RED_SHARD) != 0) num_stars++;
		if ((_raidData.flags & RaidData.RAID_FLAG_GREEN_SHARD) != 0) num_stars++;
		if ((_raidData.flags & RaidData.RAID_FLAG_BLUE_SHARD) != 0) num_stars++;

		// Determine the medal deltas for both the attacker and defender.
		_raidData.attacker_reward_medals = (int)(_raidData.attacker_0_star_medal_delta + ((_raidData.attacker_5_star_medal_delta - _raidData.attacker_0_star_medal_delta) * (float)num_stars / 5f + 0.5f));
		_raidData.defender_reward_medals = (int)(_raidData.defender_0_star_medal_delta + ((_raidData.defender_5_star_medal_delta - _raidData.defender_0_star_medal_delta) * (float)num_stars / 5f + 0.5f));

		// Record the defender nation's prev defender medals count.
		int prev_defender_medals = defenderNationData.raid_defender_medals;

		// Add the medal deltas to both nations' appropriate medal counts.
		attackerNationData.raid_attacker_medals = Math.max(0, attackerNationData.raid_attacker_medals + _raidData.attacker_reward_medals);
		defenderNationData.raid_defender_medals = Math.max(0, defenderNationData.raid_defender_medals + _raidData.defender_reward_medals);

		// Change which raid candidate bucket the defender is in, if necessary.
		DefenderMedalsCountChanged(defenderNationData.ID, prev_defender_medals, defenderNationData.raid_defender_medals);

		// Determine attacker rewards
		_raidData.reward_credits = _raidData.max_reward_credits * num_stars / 5;
		_raidData.reward_xp = _raidData.max_reward_xp * num_stars / 5;
		_raidData.reward_rebirth = _raidData.max_reward_rebirth * num_stars / 5;

		// Award attacker rewards
		Money.AddGameMoney(attackerNationData, _raidData.reward_credits, Money.Source.FREE);
		Gameplay.AddXP(attackerNationData, _raidData.reward_xp, -1, -1, -1, true, true, 0, Constants.XP_RAID);
		Gameplay.ChangeRebirthCountdown(attackerNationData, _raidData.reward_rebirth);

		// Log suspect
		if (attackerNationData.log_suspect_expire_time > Constants.GetTime())
		{
			// Log the details of this xp gain.
			Constants.WriteToLog("log_suspect.txt", Constants.GetTimestampString() + ": '" + attackerNationData.name + "'(ID:" + attackerNationData.ID + ", Level:" + attackerNationData.level + ") received " + _raidData.reward_xp + " XP for raid against nation " + defenderNationData.name + "'(ID:" + defenderNationData.ID + ", Level:" + defenderNationData.level + ").\n");
		}

		// Update the attacker nation's records of raid earnings and medals.
		attackerNationData.raid_earnings_history += _raidData.reward_credits;
		attackerNationData.raid_earnings_history_monthly += _raidData.reward_credits;
		attackerNationData.medals_history = Math.max(attackerNationData.medals_history, attackerNationData.raid_attacker_medals + attackerNationData.raid_defender_medals);
		attackerNationData.medals_history_monthly = Math.max(attackerNationData.medals_history_monthly, attackerNationData.raid_attacker_medals + attackerNationData.raid_defender_medals);

		// Update the attacker nation's raid earnings and medals ranks.
		RanksData.instance.ranks_nation_raid_earnings.UpdateRanks(attackerNationData.ID, attackerNationData.name, attackerNationData.raid_earnings_history, Constants.NUM_GLOBAL_PRIZE_RANKS, false);
		RanksData.instance.ranks_nation_raid_earnings_monthly.UpdateRanks(attackerNationData.ID, attackerNationData.name, attackerNationData.raid_earnings_history_monthly, Constants.NUM_GLOBAL_PRIZE_RANKS, false);
		RanksData.instance.ranks_nation_medals.UpdateRanks(attackerNationData.ID, attackerNationData.name, attackerNationData.medals_history, Constants.NUM_MEDALS_RANKS, false);
		RanksData.instance.ranks_nation_medals_monthly.UpdateRanks(attackerNationData.ID, attackerNationData.name, attackerNationData.medals_history_monthly, Constants.NUM_MEDALS_RANKS, false);

		// Update the defender nation's records of medals.
		defenderNationData.medals_history = Math.max(defenderNationData.medals_history, defenderNationData.raid_attacker_medals + defenderNationData.raid_defender_medals);
		defenderNationData.medals_history_monthly = Math.max(defenderNationData.medals_history_monthly, defenderNationData.raid_attacker_medals + defenderNationData.raid_defender_medals);

		// Update the defender nation's medals ranks.
		RanksData.instance.ranks_nation_medals.UpdateRanks(defenderNationData.ID, defenderNationData.name, defenderNationData.medals_history, Constants.NUM_MEDALS_RANKS, false);
		RanksData.instance.ranks_nation_medals_monthly.UpdateRanks(defenderNationData.ID, defenderNationData.name, defenderNationData.medals_history_monthly, Constants.NUM_MEDALS_RANKS, false);

		Output.PrintToScreen("Raid " + attackerNationData.name + " vs " + defenderNationData.name + " result: " + num_stars + " stars. Attacker receives " + _raidData.reward_credits + " credits, " + _raidData.reward_xp + " XP, " + _raidData.reward_rebirth + " rebirth, and " + _raidData.attacker_reward_medals + " medals (total: " + attackerNationData.raid_attacker_medals + "). Defender receives " + _raidData.defender_reward_medals + " medals (total: " + defenderNationData.raid_defender_medals + "). Replay buffer len: " + _raidData.replay.length() + ".");

		// Log result of this raid.
		Constants.WriteToLog("log_raid.txt", Constants.GetTimestampString() + ": " + attackerNationData.name + " (" + attackerNationData.ID + ") raided " + defenderNationData.name + " (" + defenderNationData.ID + "). " + _raidData.percentage_defeated + "% defeated, " + num_stars + " stars, lasted " + ((_raidData.end_time - _raidData.begin_time) / 60) + " mins. Attacker receives " + _raidData.reward_credits + " credits, " + _raidData.reward_xp + " XP, " + _raidData.reward_rebirth + " rebirth, and " + _raidData.attacker_reward_medals + " medals (total: " + attackerNationData.raid_attacker_medals + "). Defender receives " + _raidData.defender_reward_medals + " medals (total: " + defenderNationData.raid_defender_medals + ").\n");
		Constants.WriteToNationLog(attackerNationData, null, attackerNationData.name + " (" + attackerNationData.ID + ") raided " + defenderNationData.name + " (" + defenderNationData.ID + "). " + _raidData.percentage_defeated + "% defeated, " + num_stars + " stars, lasted " + ((_raidData.end_time - _raidData.begin_time) / 60) + " mins. Attacker receives " + _raidData.reward_credits + " credits, " + _raidData.reward_xp + " XP, " + _raidData.reward_rebirth + " rebirth, and " + _raidData.attacker_reward_medals + " medals (total: " + attackerNationData.raid_attacker_medals + "). Defender receives " + _raidData.defender_reward_medals + " medals (total: " + defenderNationData.raid_defender_medals + ").");
		Constants.WriteToNationLog(defenderNationData, null, attackerNationData.name + " (" + attackerNationData.ID + ") raided " + defenderNationData.name + " (" + defenderNationData.ID + "). " + _raidData.percentage_defeated + "% defeated, " + num_stars + " stars, lasted " + ((_raidData.end_time - _raidData.begin_time) / 60) + " mins. Attacker receives " + _raidData.reward_credits + " credits, " + _raidData.reward_xp + " XP, " + _raidData.reward_rebirth + " rebirth, and " + _raidData.attacker_reward_medals + " medals (total: " + attackerNationData.raid_attacker_medals + "). Defender receives " + _raidData.defender_reward_medals + " medals (total: " + defenderNationData.raid_defender_medals + ").");

		// If this was at least a partial victory...
		if (num_stars >= 1)
		{
			// Remove any shield from the attacking nation (unless there's so little remaining that it may be because they're currently being raided).
			if ((attackerNationData.raid_shield_end_time - Constants.GetTime()) > RAID_MAX_DURATION) {
				attackerNationData.raid_shield_end_time = Constants.GetTime();
			}

			// Add a temporary shield to the defending nation.
			defenderNationData.raid_shield_end_time = Math.max(defenderNationData.raid_shield_end_time, Constants.GetTime() + RAID_DEFENDER_SHIELD_DURATION);

			// Add to the available ad bonus for this nation's logged in users, as appropriate.
			Gameplay.AwardAvailableAdBonusToNation(attackerNationData, num_stars / 5f, Constants.AD_BONUS_TYPE_RAID, -1, -1, 1);
		}

		// Add 1 to the raids fought report for both nations' users.
		attackerNationData.ModifyUserReportValueInt(UserData.ReportVal.report__raids_fought, 1);
		defenderNationData.ModifyUserReportValueInt(UserData.ReportVal.report__raids_fought, 1);

		// Add the appropriate amount to the medals delta report for both nations' users.
		attackerNationData.ModifyUserReportValueInt(UserData.ReportVal.report__medals_delta, _raidData.attacker_reward_medals);
		defenderNationData.ModifyUserReportValueInt(UserData.ReportVal.report__medals_delta, _raidData.defender_reward_medals);

		// If the attacking nation has any manpower left, add it back in to their homeland manpower.
		if (_raidData.attacker_footprint.manpower > 0)
		{
			// Add the remaining manpower back to the attacking nation's homeland.
			attackerNationData.homeland_footprint.manpower = Math.min(attackerNationData.GetFinalManpowerMax(attackerNationData.homeland_mapID), attackerNationData.homeland_footprint.manpower + _raidData.attacker_footprint.manpower);

			// Broadcast an update event to the attacking nation.
			OutputEvents.BroadcastUpdateEvent(_raidData.attacker_nationID);
		}

		// If the defender's homeland map hasn't been modified since this raid began, this raid is eligible for replay.
		if (defenderNationData.prev_modify_homeland_time < _raidData.start_time)
		{
			// Record the flag indicating that the raid will have a replay available.
			_raidData.flags = _raidData.flags | RaidData.RAID_REPLAY_AVAILABLE;

			// Get the raid source homeland landmap.
			LandMap source_landmap = Homeland.GetHomelandMap(_raidData.defender_nationID);

			// Get the raid landmap.
			LandMap raid_landmap = DataManager.GetLandMap(_raidData.ID, true);

			// Copy the block data from the defender's homeland landmap to the raid's landmap.
			raid_landmap.Copy(source_landmap);

			//// Copy the defending nation's raid footprint from their homeland footprint.
			//_raidData.defender_footprint.Copy(defenderNationData.homeland_footprint);
		}

		// Add end event to the raid's replay history
		Constants.EncodeUnsignedNumber(_raidData.replay, RAID_EVENT_END, 1);

		// Add this raid to the appropriate raid logs of both nations.
		attackerNationData.raid_attack_log.add(_raidData.ID);
		defenderNationData.raid_defense_log.add(_raidData.ID);

		// Broadcast rewards to attacker and defender nations.
		OutputEvents.BroadcastUpdateEvent(attackerNationData.ID); // For credits and rebirth
		OutputEvents.BroadcastStatsEvent(attackerNationData.ID, 0); // For medals
		OutputEvents.BroadcastStatsEvent(defenderNationData.ID, 0); // For medals

		// Broadcast the new raid log entry to all members of both nations.
		OutputEvents.BroadcastRaidLogEntryEvent(attackerNationData, _raidData, true);
		OutputEvents.BroadcastRaidLogEntryEvent(defenderNationData, _raidData, false);

		// Mark both nations' data to be updated.
		DataManager.MarkForUpdate(attackerNationData);
		DataManager.MarkForUpdate(defenderNationData);

		// Mark the RaidData to be updated.
		DataManager.MarkForUpdate(_raidData);

		Output.PrintToScreen("Raid " + _raidData.ID + " finished.");
	}

	public static void ClearRaid(RaidData _raidData, NationData _nationData)
	{
		// Clear the nation's raid information.
		_nationData.raidID = -1;
		DataManager.MarkForUpdate(_nationData);

		// If any of this nation's users (who are logged off) are still viewing the raid, set their view to the homeland instead.
		for (int user_index = 0; user_index < _nationData.users.size(); user_index++)
		{
			// Get the current member user's data
			UserData curUserData = (UserData)DataManager.GetData(Constants.DT_USER, _nationData.users.get(user_index), false);

			if (curUserData.mapID >= RAID_ID_BASE)
			{
				curUserData.mapID = _nationData.homeland_mapID;
				DataManager.MarkForUpdate(curUserData);
			}
		}
	}

	public static boolean IsInFinishedRaid(UserData _userData)
	{
		if (_userData.mapID < RAID_ID_BASE) {
			return false;
		}

		// Get the data for this user's current raid.
		RaidData raidData = (RaidData)DataManager.GetData(Constants.DT_RAID, _userData.mapID, false);

		if (raidData == null)
		{
			Output.PrintToScreen("ERROR: User " + _userData.name + "'s current map is " + _userData.mapID + ", but there is no raid with that ID.");
			return false;
		}

		return ((raidData.flags & RaidData.RAID_FLAG_FINISHED) != 0);
	}

	public static void RemoveObsoleteRaidsFromLog(ArrayList<Integer> _log_list)
	{
		Integer raidID;
		RaidData raidData;

		for (Iterator<Integer> iterator = _log_list.iterator(); iterator.hasNext();)
		{
			raidID = iterator.next();

			// Get the data for the current raid.
			raidData = (RaidData)DataManager.GetData(Constants.DT_RAID, raidID, false);

			if (raidData == null)
			{
				Output.PrintToScreen("ERROR: RemoveObsoleteRaidsFromLog() data for raidID " + raidID + " not found.");
				iterator.remove();
				continue;
			}

			// If more time has passed since this raid started than the RAID_HISTORY_DURATION, remove this raid from the log list.
			if ((raidData.start_time > 0) && ((Constants.GetTime() - raidData.start_time) >= RAID_HISTORY_DURATION)) {
				iterator.remove();
			}
		}
	}

	public static void RemoveRaidFromLogs(int _nationID, int _raidID)
	{
		//Output.PrintToScreen("RemoveRaidFromLogs() called to remove raid " + _raidID + " from nation " + _nationID);

		// Get the given nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationID, false);

		if (nationData == null) {
			return;
		}

		// Remove the given _raidID from both of this nation's raid logs.
		nationData.raid_attack_log.remove((Integer)_raidID);
		nationData.raid_defense_log.remove((Integer)_raidID);

		// Mark the nation's data to be updated.
		DataManager.MarkForUpdate(nationData);
	}

	public static void Replay(StringBuffer _output_buffer, int _userID, int _raidID)
	{
		// Add the raid ID base back into the given ID.
		_raidID += RAID_ID_BASE;

		// Get the data for the raid with the given ID.
		RaidData raidData = (RaidData)DataManager.GetData(Constants.DT_RAID, _raidID, false);

		// If the raid doesn't exist or isn't finished, do nothing.
		if ((raidData == null) || ((raidData.flags & RaidData.RAID_FLAG_FINISHED) == 0)) {
			return;
		}

		// Get the raid landmap.
		LandMap raid_landmap = DataManager.GetLandMap(_raidID, false);

		// If the raid landmap doesn't exist, do nothing.
		if (raid_landmap == null) {
			return;
		}

		// Get the user's data
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		// Get a map event for the entire raid landmap.
		Display.GetMapEvent(_output_buffer, userData, raid_landmap, 0, 0, raid_landmap.width -1, raid_landmap.height -1, true, false, true);

		// Append the raid's replay event to the output buffer.
		_output_buffer.append(raidData.replay);
	}

	public static void EndReplay(StringBuffer _output_buffer, int _userID)
	{
	  // Get the user's data
	  UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		// Get the user's nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, userData.nationID, false);

		// Get the user's nation's homeland map (creating it if it doesn't yet exist).
		LandMap homeland_map = Homeland.GetHomelandMap(userData.nationID);

		// Remove the user from its current map.
		Display.ResetUserView(userData);

		// Switch the user's view to thier nation's homeland map.
		userData.mapID = homeland_map.ID;

		// Set the user's view, according to which map it is on.
		Display.SetUserViewForMap(userData, _output_buffer);

		// Refresh the user's raid status (which will have been modified by the replay).
		OutputEvents.GetRaidStatusEvent(_output_buffer, nationData, 0);
	}

	public static void RecordEvent_SetNationID(int _raidID, int _x, int _y, int _nationID, int _delay)
	{
		// Get the data for the raid with the given ID.
		RaidData raidData = (RaidData)DataManager.GetData(Constants.DT_RAID, _raidID, false);

		// Do nothing if the raid data wasn't found.
		if (raidData == null) {
			return;
		}

		//Output.PrintToScreen("RecordEvent_SetNationID() for " + _x + "," + _y + " nation ID " + _nationID);

		// Encode event
		Constants.EncodeUnsignedNumber(raidData.replay, RAID_EVENT_SET_NATION_ID, 1);
		Constants.EncodeUnsignedNumber(raidData.replay, (raidData.begin_time == 0) ? 0 : Math.max(0, (int)(Constants.GetFineTime() + (_delay * 1000) - (raidData.begin_time * 1000))), 5); // Timestamp
		Constants.EncodeUnsignedNumber(raidData.replay, _x, 2);
		Constants.EncodeUnsignedNumber(raidData.replay, _y, 2);
		Constants.EncodeUnsignedNumber(raidData.replay, _nationID, 5);
	}

	public static void RecordEvent_ClearNationID(int _raidID, int _x, int _y, int _delay)
	{
		// Get the data for the raid with the given ID.
		RaidData raidData = (RaidData)DataManager.GetData(Constants.DT_RAID, _raidID, false);

		// Do nothing if the raid data wasn't found.
		if (raidData == null) {
			return;
		}

		//Output.PrintToScreen("RecordEvent_ClearNationID() for " + _x + "," + _y);

		// Encode event
		Constants.EncodeUnsignedNumber(raidData.replay, RAID_EVENT_CLEAR_NATION_ID, 1);
		Constants.EncodeUnsignedNumber(raidData.replay, (int)(Constants.GetFineTime() + (_delay * 1000) - (raidData.begin_time * 1000)), 5); // Timestamp
		Constants.EncodeUnsignedNumber(raidData.replay, _x, 2);
		Constants.EncodeUnsignedNumber(raidData.replay, _y, 2);
	}

	public static void RecordEvent_Battle(int _raidID, int _x, int _y, int _nationID, int _battle_flags, int _delay)
	{
		// Get the data for the raid with the given ID.
		RaidData raidData = (RaidData)DataManager.GetData(Constants.DT_RAID, _raidID, false);

		// Do nothing if the raid data wasn't found.
		if (raidData == null) {
			return;
		}

		//Output.PrintToScreen("RecordEvent_SetNationID() for " + _x + "," + _y + " nation ID " + _nationID);

		// Encode event
		Constants.EncodeUnsignedNumber(raidData.replay, RAID_EVENT_BATTLE, 1);
		Constants.EncodeUnsignedNumber(raidData.replay, (raidData.begin_time == 0) ? 0 : Math.max(0, (int)(Constants.GetFineTime() + (_delay * 1000) - (raidData.begin_time * 1000))), 5); // Timestamp
		Constants.EncodeUnsignedNumber(raidData.replay, _x, 2);
		Constants.EncodeUnsignedNumber(raidData.replay, _y, 2);
		Constants.EncodeUnsignedNumber(raidData.replay, _nationID, 5);
		Constants.EncodeUnsignedNumber(raidData.replay, _battle_flags, 2);
	}

	public static void RecordEvent_SetObjectID(int _raidID, int _x, int _y, int _objectID, int _delay)
	{
		// Get the data for the raid with the given ID.
		RaidData raidData = (RaidData)DataManager.GetData(Constants.DT_RAID, _raidID, false);

		// Do nothing if the raid data wasn't found.
		if (raidData == null) {
			return;
		}

		// Encode event
		Constants.EncodeUnsignedNumber(raidData.replay, RAID_EVENT_SET_OBJECT_ID, 1);
		Constants.EncodeUnsignedNumber(raidData.replay, (int)(Constants.GetFineTime() + (_delay * 1000) - (raidData.begin_time * 1000)), 5); // Timestamp
		Constants.EncodeUnsignedNumber(raidData.replay, _x, 2);
		Constants.EncodeUnsignedNumber(raidData.replay, _y, 2);
		Constants.EncodeUnsignedNumber(raidData.replay, _objectID, 5);
	}

	public static void RecordEvent_TowerAction(int _raidID, int _blockX, int _blockY, int _build_ID, int _build_type, int _invisible_time, int _trigger_x, int _trigger_y, ArrayList<TargetRecord> _targets, int _triggerNationID)
	{
		// Get the data for the raid with the given ID.
		RaidData raidData = (RaidData)DataManager.GetData(Constants.DT_RAID, _raidID, false);

		// Do nothing if the raid data wasn't found.
		if (raidData == null) {
			return;
		}

		int timestamp = (int)(Constants.GetFineTime() - (raidData.begin_time * 1000));

		// Test for problem where sometimes timestamp is way too large.
		if (timestamp > (RAID_MAX_DURATION * 1000))
		{
			Output.PrintToScreen("ERROR: RecordEvent_TowerAction() called for raid " + _raidID + " when timestamp (" + timestamp + ") would be too large. Raid begin_time: " + raidData.begin_time + ", cur time: " + Constants.GetTime() + ", cur fine time: " + Constants.GetFineTime() + ". Raid flags: " + raidData.flags);
			Output.PrintStackTrace();
			return;
		}

		int cur_time = Constants.GetTime();

		// Encode the tower action event
		Constants.EncodeUnsignedNumber(raidData.replay, RAID_EVENT_TOWER_ACTION, 1);
		Constants.EncodeUnsignedNumber(raidData.replay, timestamp, 5); // Timestamp
		Constants.EncodeUnsignedNumber(raidData.replay, _blockX, 2);
		Constants.EncodeUnsignedNumber(raidData.replay, _blockY, 2);
		Constants.EncodeNumber(raidData.replay, _build_ID, 2);
		Constants.EncodeUnsignedNumber(raidData.replay, _build_type, 1);
		Constants.EncodeNumber(raidData.replay, (_invisible_time == -1) ? -1 : ((_invisible_time < cur_time) ? -2 : (_invisible_time - cur_time)), 4);
		Constants.EncodeUnsignedNumber(raidData.replay, Constants.TOWER_ACTION_DURATION, 1);
		Constants.EncodeUnsignedNumber(raidData.replay, _trigger_x, 2);
		Constants.EncodeUnsignedNumber(raidData.replay, _trigger_y, 2);
		Constants.EncodeUnsignedNumber(raidData.replay, _triggerNationID, 5);
		Constants.EncodeUnsignedNumber(raidData.replay, _targets.size(), 2);

		// Encode each TargetRecord
		Iterator<TargetRecord> targetsIterator = _targets.iterator();
		TargetRecord cur_target;
		while (targetsIterator.hasNext())
		{
			cur_target = targetsIterator.next();
			Constants.EncodeUnsignedNumber(raidData.replay, cur_target.x, 2);
			Constants.EncodeUnsignedNumber(raidData.replay, cur_target.y, 2);
			Constants.EncodeNumber(raidData.replay, cur_target.newNationID, 5);
			Constants.EncodeUnsignedNumber(raidData.replay, (cur_target.end_hit_points > 0) ? 1 : 0, 1);
			Constants.EncodeUnsignedNumber(raidData.replay, cur_target.battle_flags, 2);
			Constants.EncodeNumber(raidData.replay, (cur_target.wipe_end_time == -1) ? -1 : ((cur_target.wipe_end_time < cur_time) ? -2 : (cur_target.wipe_end_time - cur_time)), 4);
			Constants.EncodeUnsignedNumber(raidData.replay, cur_target.wipe_flags, 1);
		}
	}

	public static void RecordEvent_TriggerInert(int _raidID, int _blockX, int _blockY)
	{
		// Get the data for the raid with the given ID.
		RaidData raidData = (RaidData)DataManager.GetData(Constants.DT_RAID, _raidID, false);

		// Do nothing if the raid data wasn't found.
		if (raidData == null) {
			return;
		}

		// Encode the trigger inert event
		Constants.EncodeUnsignedNumber(raidData.replay, RAID_EVENT_TRIGGER_INERT, 1);
		Constants.EncodeUnsignedNumber(raidData.replay, (int)(Constants.GetFineTime() - (raidData.begin_time * 1000)), 5); // Timestamp
		Constants.EncodeUnsignedNumber(raidData.replay, _blockX, 2);
		Constants.EncodeUnsignedNumber(raidData.replay, _blockY, 2);
	}

	public static void RecordEvent_ExtendedData(int _raidID, LandMap _land_map, int _x, int _y)
	{
		// Get the data for the raid with the given ID.
		RaidData raidData = (RaidData)DataManager.GetData(Constants.DT_RAID, _raidID, false);

		// Do nothing if the raid data wasn't found.
		if (raidData == null) {
			return;
		}

		// Get the block's extended data.
		BlockExtData block_ext_data = _land_map.GetBlockExtendedData(_x, _y, false);

		// Get the current time.
		int cur_time = Constants.GetTime();

		// Encode event
		Constants.EncodeUnsignedNumber(raidData.replay, RAID_EVENT_EXT_DATA, 1);
		Constants.EncodeUnsignedNumber(raidData.replay, (int)(Constants.GetFineTime() - (raidData.begin_time * 1000)), 5); // Timestamp
		Constants.EncodeUnsignedNumber(raidData.replay, _x, 2);
		Constants.EncodeUnsignedNumber(raidData.replay, _y, 2);
		Constants.EncodeNumber(raidData.replay, block_ext_data.objectID, 2);
		Constants.EncodeNumber(raidData.replay, block_ext_data.owner_nationID, 5);
		Constants.EncodeNumber(raidData.replay, (block_ext_data.creation_time == -1) ? Constants.LARGE_NEGATIVE_TIME : (block_ext_data.creation_time - cur_time), 5);
		Constants.EncodeNumber(raidData.replay, (block_ext_data.completion_time == -1) ? -1 : ((block_ext_data.completion_time < cur_time) ? -2 : (block_ext_data.completion_time - cur_time)), 4);
		Constants.EncodeNumber(raidData.replay, (block_ext_data.invisible_time == -1) ? -1 : ((block_ext_data.invisible_time < cur_time) ? -2 : (block_ext_data.invisible_time - cur_time)), 4);
		Constants.EncodeNumber(raidData.replay, (block_ext_data.capture_time == -1) ? Constants.LARGE_NEGATIVE_TIME : (block_ext_data.capture_time - cur_time), 5);
		Constants.EncodeNumber(raidData.replay, (block_ext_data.crumble_time == -1) ? -1 : ((block_ext_data.crumble_time < cur_time) ? -2 : (block_ext_data.crumble_time - cur_time)), 4);
		Constants.EncodeNumber(raidData.replay, block_ext_data.wipe_nationID, 5);
		Constants.EncodeNumber(raidData.replay, (block_ext_data.wipe_end_time == -1) ? -1 : ((block_ext_data.wipe_end_time < cur_time) ? -2 : (block_ext_data.wipe_end_time - cur_time)), 4);
		Constants.EncodeUnsignedNumber(raidData.replay, block_ext_data.wipe_flags, 1);
	}

	public static void RecordEvent_Salvage(int _raidID, int _x, int _y)
	{
		// Get the data for the raid with the given ID.
		RaidData raidData = (RaidData)DataManager.GetData(Constants.DT_RAID, _raidID, false);

		// Do nothing if the raid data wasn't found.
		if (raidData == null) {
			return;
		}

		// Encode event
		Constants.EncodeUnsignedNumber(raidData.replay, RAID_EVENT_SALVAGE, 1);
		Constants.EncodeUnsignedNumber(raidData.replay, (int)(Constants.GetFineTime() - (raidData.begin_time * 1000)), 5); // Timestamp
		Constants.EncodeUnsignedNumber(raidData.replay, _x, 2);
		Constants.EncodeUnsignedNumber(raidData.replay, _y, 2);
	}

	public static void RecordEvent_Complete(int _raidID, int _x, int _y)
	{
		// Get the data for the raid with the given ID.
		RaidData raidData = (RaidData)DataManager.GetData(Constants.DT_RAID, _raidID, false);

		// Do nothing if the raid data wasn't found.
		if (raidData == null) {
			return;
		}

		// Encode event
		Constants.EncodeUnsignedNumber(raidData.replay, RAID_EVENT_COMPLETE, 1);
		Constants.EncodeUnsignedNumber(raidData.replay, (int)(Constants.GetFineTime() - (raidData.begin_time * 1000)), 5); // Timestamp
		Constants.EncodeUnsignedNumber(raidData.replay, _x, 2);
		Constants.EncodeUnsignedNumber(raidData.replay, _y, 2);
	}

	public static float Logistic(float _x)
	{
		float e_to_x = (float)Math.pow(Math.E, _x);
		return e_to_x / (e_to_x + 1);
	}

	public static int DetermineAdjustedMedalDelta(float _cur_medals, float _medal_delta)
	{
		if (_medal_delta < 0)
		{
			if (_cur_medals < MEDALS_ADJUSTMENT_RANGE) {
				_medal_delta *= (_cur_medals / MEDALS_ADJUSTMENT_RANGE);
			}

			// If the nation currently has 0 medals, don't allow it to lose any more.
			if (_cur_medals == 0) {
				_medal_delta = 0;
			}
		}
		else if (_medal_delta > 0)
		{
			if (_cur_medals > (MEDALS_RANGE_TOP - MEDALS_ADJUSTMENT_RANGE)) {
				_medal_delta *= ((MEDALS_RANGE_TOP - _cur_medals) / MEDALS_ADJUSTMENT_RANGE);
			}

			// Apply ceiling funtion so that no positive medal delta will be less than 1. Rounding down already does the same for negative medal deltas.
			_medal_delta = (float)Math.ceil(_medal_delta);
		}

		return (int)_medal_delta;
	}

	public static int GetRaidCandidateBucketIndex(int _defender_medals)
	{
		int bucket_index = _defender_medals / MEDALS_PER_BUCKET;

		// Make sure the appropriate bucket exists in the raid_candidates list.
		while (raid_candidates.size() <= bucket_index) {
			raid_candidates.add(new ArrayList<Integer>());
		}

		return bucket_index;
	}

	public static void AddRaidCandidate(int _nationID, int _defender_medals)
	{
		// Determine the bucket index (also creating the bucket if necessary).
		int bucket_index = GetRaidCandidateBucketIndex(_defender_medals);

		// Add the given _nationID to the appropriate bucket.
		raid_candidates.get(bucket_index).add(_nationID);
	}

	public static void RemoveRaidCandidate(int _nationID, int _defender_medals)
	{
		// Determine the bucket index (also creating the bucket if necessary).
		int bucket_index = GetRaidCandidateBucketIndex(_defender_medals);

		// Remove the given _nationID from the appropriate bucket.
		raid_candidates.get(bucket_index).remove((Integer)_nationID);
	}

	public static void DefenderMedalsCountChanged(int _nationID, int _prev_defender_medals, int _cur_defender_medals)
	{
		// Determine the nation's prev and current bucket indices.
		int prev_bucket_index = GetRaidCandidateBucketIndex(_prev_defender_medals);
		int cur_bucket_index = GetRaidCandidateBucketIndex(_cur_defender_medals);

		if (prev_bucket_index != cur_bucket_index)
		{
			// Change which bucket this candidate nation is stored in.
			raid_candidates.get(prev_bucket_index).remove((Integer)_nationID);
			raid_candidates.get(cur_bucket_index).add(_nationID);
		}
	}

	public static void InitRaidCandidates()
	{
		// Determine highest nation ID
		int highestNationID = DataManager.GetHighestDataID(Constants.DT_NATION);

		Output.PrintToScreen("Initializing raid candidates...");

		// Iterate through each nation
		NationData curNationData;
		for (int curNationID = 1; curNationID <= highestNationID; curNationID++)
		{
			// Get the data for the nation with the current ID
			curNationData = (NationData)DataManager.GetData(Constants.DT_NATION, curNationID, false);

			// If no nation exists with this ID, continue to next.
			if (curNationData == null) {
				continue;
			}

			// If this nation is raid eligible, add it to the list of raid candidates.
			if (curNationData.raid_eligible) {
				AddRaidCandidate(curNationID, curNationData.raid_defender_medals);
			}
		}
	}

	public static int FindRaidMatch(NationData _attackerNationData)
	{
		int i;
		NationData curNationData;
		int defenderNationID = -1;
		ArrayList<Integer> bucket;

		// Determine a semi-unique process ID based on the current fine time.
		int processID = (int)(Constants.GetFineTime() & 0x7FFFFFFF);

		// Mark all of the given attacker nation's recent raid candidates with the process ID.
		for (i = 0; i < _attackerNationData.raid_prev_candidates.size(); i++)
		{
			// Get the data for the current prev candidate nation.
			curNationData = (NationData)DataManager.GetData(Constants.DT_NATION, _attackerNationData.raid_prev_candidates.get(i), false);

			// Mark this nation with the current processID.
			curNationData.processID = processID;
			curNationData.processData = 0;
		}

		// Determine the bucket index for the attacker nation's medal count.
		int attacker_bucket_index = GetRaidCandidateBucketIndex(_attackerNationData.raid_attacker_medals);

		// Cache the current time.
		int cur_time = Constants.GetTime();

		// Determine the max level difference allowed between the attacker and defender nations.
		int max_level_dif = (int)Math.max(10, _attackerNationData.level * 0.4);

		for (int bucket_dif = 0; bucket_dif <= MATCHMAKING_MAX_BUCKET_DIF; bucket_dif++)
		{
			// Set cur_bucket_index first to (attacker_bucket_index - bucket_dif), then to (attacker_bucket_index + bucket_dif).
			for (int cur_bucket_index = attacker_bucket_index - bucket_dif; cur_bucket_index == (attacker_bucket_index - bucket_dif); cur_bucket_index = attacker_bucket_index + bucket_dif)
			{
				// If the cur_bucket_index refers to a valid bucket...
				if ((cur_bucket_index >= 0) && (cur_bucket_index < raid_candidates.size()))
				{
					// Get the current bucket.
					bucket = raid_candidates.get(cur_bucket_index);

					// Iterate through each raid candidate in the current bucket...
					for (i = 0; i < bucket.size(); i++)
					{
						// Get the data for the current raid candidate nation.
						curNationData = (NationData)DataManager.GetData(Constants.DT_NATION, bucket.get(i), false);

						//Output.PrintToScreen("bucket_dif: " + bucket_dif + ", cur_bucket_index: " + cur_bucket_index + ", index: " + i + ", nation " + curNationData.name + ", shield end time: " + curNationData.raid_shield_end_time + ", level: " + curNationData.level + ", cur proces ID: " + (curNationData.processID == processID));

						// If the current nation is the attacker nation itself, skip it.
						if (curNationData.ID == _attackerNationData.ID) {
							continue;
						}

						// If the current nation is an ally of the attacker nation, skip it. (The attacker won't be able to attack it!)
						if (_attackerNationData.alliances_active.contains(curNationData.ID)) {
							continue;
						}

						// If the current nation has a shield now, skip it.
						if (curNationData.raid_shield_end_time >= cur_time) {
							continue;
						}

						// If the level difference with this nation is too great, skip it.
						if (Math.abs(curNationData.level - _attackerNationData.level) > max_level_dif) {
							continue;
						}

						// If this nation is marked as being one of the attacker's recent prev raid candidates, mark it as a potential candidate but skip it for now.
						if (curNationData.processID == processID)
						{
							curNationData.processData = 1;
							continue;
						}

						// All criteria met; this is a valid defender candidate.
						defenderNationID = curNationData.ID;
						break;
					}
				}

				// Exit loop if a result has been found.
				if (defenderNationID != -1) {
					break;
				}

				// Only iterate this loop once if bucket_dif is 0.
				if (bucket_dif == 0) {
					break;
				}
			}

			// Exit loop if a result has been found.
			if (defenderNationID != -1) {
				break;
			}
		}

		// If no defender candidate has yet been found...
		if (defenderNationID == -1)
		{
			Output.PrintToScreen("FindRaidMatch() resorting to previous candidates.");

			// Iterate again through each of the attacker nation's recent prev candidates, from oldest to most recent...
			for (i = 0; i < _attackerNationData.raid_prev_candidates.size(); i++)
			{
				// Get the data for the current prev candidate nation.
				curNationData = (NationData)DataManager.GetData(Constants.DT_NATION, _attackerNationData.raid_prev_candidates.get(i), false);

				// If this recent prev candidate is otherwise a valid defender candidate, select it as the defender candidate, even though it was also a candidate recently.
				if ((curNationData.processID == processID) && (curNationData.processData != 0))
				{
					defenderNationID = curNationData.ID;
					break;
				}
			}
		}

		// Return result.
		return defenderNationID;
	}

	public static LeagueData GetNationLeague(NationData _nationData)
	{
		return LeagueData.GetLeagueData((_nationData.raid_attacker_medals + _nationData.raid_defender_medals) / MEDALS_PER_LEAGUE);
	}
}
