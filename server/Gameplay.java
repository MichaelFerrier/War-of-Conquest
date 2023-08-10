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

public class Gameplay
{
	public static class CombatStats
	{
		int attacker_stat, defender_stat;
		float attacker_best_ratio;
	}

	static final int ADJ_X = 0;
	static final int ADJ_Y = 1;
	static int[] adj = new int[2], adj_empty = new int[2];
	static float[][] stat_ratios = new float[Constants.NUM_STATS][Constants.NUM_STATS];
	static CombatStats combat_stats = new CombatStats();

	public static void Evacuate(StringBuffer _output_buffer, int _userID, int _x, int _y)
	{
		// Get the current time
		int cur_time = Constants.GetTime();

		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);
		LandMap land_map = DataManager.GetLandMap(userData.mapID, false);

		if (land_map == null)
		{
			Output.PrintToScreen("ERROR: Evacuate() user " + userData.name + " (" + userData.ID + ") has mapID " + userData.mapID + "; land_map == null.");
			return;
		}

		BlockData blockData = land_map.GetBlockData(_x, _y);
		BlockExtData block_ext_data = ((blockData.flags & BlockData.BF_EXTENDED_DATA) == 0) ? null : land_map.GetBlockExtendedData(_x, _y, false);

		// If the block contains a build object that has crumbled, remove that build object.
		land_map.CheckForBuildObjectCrumble(_x, _y, blockData, block_ext_data);

		// Determine whether this evacuation will require salvaging a structure build in this block.
		boolean salvage_required = (block_ext_data != null) && (block_ext_data.objectID != -1) && (block_ext_data.owner_nationID == userData.nationID);

		// If the given block does not belong to the user's nation, do nothing.
		if (blockData.nationID != userData.nationID) {
			return;
		}

		// If the block is currently locked, do nothing.
		if (blockData.lock_until_time > cur_time) {
			return;
		}

		// If the user is in a raid that has finished, do nothing.
		if (Raid.IsInFinishedRaid(userData)) {
			return;
		}

		if (userData.rank > Constants.RANK_WARRIOR)
		{
			// Return message
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_evacuate_rank_too_low")); // "You cannot evacuate land until you've been promoted to Warrior."
			return;
		}

		// Get the user's nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, userData.nationID, false);

		if (nationData.GetFootprint(land_map.ID).area == 1)
		{
			// Return message
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_evacuate_only_area")); // "We can't evacuate our nation's only area."
			return;
		}

		// Reserve one of this user's available processes. If none are available, do nothing.
		if (ReserveUserProcess(userData, nationData, Constants.PROCESS_DURATION) == false) {
			return;
		}

		if (salvage_required)
		{
			// If attempt to salvage fails, do not evacuate the block.
			if (Salvage(_output_buffer, _userID, _x, _y) == false) {
				return;
			}
		}

		// Update this user's nation's prev_use_time to the current time
		nationData.prev_use_time = cur_time;

		// Update the nation's prev_active_time
		nationData.prev_active_time = Constants.GetTime();

		// Determine how many hit points the target block currently has.
		int full_hit_points = land_map.DetermineBlockFullHitPoints(_x, _y, false, null);
		int block_hit_points = blockData.GetBlockCurrentHitPoints(full_hit_points, GetNationHitPointsRate(nationData), cur_time);

		// Record when the block's transition will be completed, so that it will not count in adjacency checks until that time.
		blockData.transition_complete_time = cur_time + Constants.PROCESS_DURATION;

		// Evacuate the block.
		World.SetBlockNationID(land_map, _x, _y, -1, true, false, _userID, Constants.PROCESS_DURATION);

		// If this event took place on a raid map, record it in the raid's replay history.
		if (land_map.ID >= Raid.RAID_ID_BASE) {
			Raid.RecordEvent_ClearNationID(land_map.ID, _x, _y, Constants.PROCESS_DURATION);
		}

		// Encode the process display event
		Constants.EncodeString(_output_buffer, "display_process"); // Event ID
		Constants.EncodeUnsignedNumber(_output_buffer, Constants.PROCESS_DURATION, 1); // Process duration in seconds
		Constants.EncodeNumber(_output_buffer, _x, 3); // Map x coord
		Constants.EncodeNumber(_output_buffer, _y, 3); // Map y coord
		Constants.EncodeUnsignedNumber(_output_buffer, full_hit_points, 2); // Max hit points
		Constants.EncodeUnsignedNumber(_output_buffer, block_hit_points, 2); // Starting hit points
		Constants.EncodeUnsignedNumber(_output_buffer, 0, 2); // Ending hit points
		Constants.EncodeNumber(_output_buffer, (int)(nationData.GetFootprint(land_map.ID).geo_efficiency_base * 1000), 4); // Ending geo efficiency
		Constants.EncodeUnsignedNumber(_output_buffer, 0, 1); // Process flags

		// Broadcast a block process event for this evacuation to all clients in the view area.
		OutputEvents.BroadcastBlockProcessEvent(land_map, _x, _y, blockData, full_hit_points, block_hit_points, GetNationHitPointsRate(null), Constants.PROCESS_DURATION, Constants.PROCESS_EVACUATE);
	}

	public static void Build(StringBuffer _output_buffer, int _userID, int _buildID, int _x, int _y)
	{
		// Get the user's data
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		if (userData.rank > Constants.RANK_GENERAL)
		{
			// Return message
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("You cannot build until you are promoted to General."));
			return;
		}

		// Do not allow the user to build if they are restricted by fealty. This should be caught first by the client.
		if ((userData.mapID == Constants.MAINLAND_MAP_ID) && (userData.fealty_end_time > Constants.GetTime()))
		{
			Output.PrintToScreen("Attempt to build when restricted by fealty, by user " + userData.name + " (" + userData.ID + ").");
			return;
		}

		// If the user is in a raid that has finished, do nothing.
		if (Raid.IsInFinishedRaid(userData)) {
			return;
		}

		// Determine ID of the user's nation
		int nationID = userData.nationID;

		// Get the user's nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

		// Get the land map
		LandMap land_map = DataManager.GetLandMap(userData.mapID, false);

		if (land_map == null)
		{
			Output.PrintToScreen("ERROR: Build() user " + userData.name + " (" + userData.ID + ") has mapID " + userData.mapID + "; land_map == null.");
			return;
		}

		// If the nation's level exceeds the target block's level limit then do not allow the nation to build in this block.
		int block_level_limit = land_map.PosXToMaxLevelLimit(_x);
		if (nationData.level > block_level_limit)
		{
			// Return message
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_build_too_far_west")); // "We are too far advanced to build in these primitive lands to the west."
			return;
		}

		// Get the build data
		BuildData build_data = BuildData.GetBuildData(_buildID);

		// Get the nation's tech data
		NationTechData nationTechData = (NationTechData)DataManager.GetData(Constants.DT_NATIONTECH, nationID, false);

		// Check whether this structure can be built by this nation.
		if ((nationTechData.available_builds.containsKey(_buildID) == false) || (nationTechData.available_builds.get(_buildID) == false) || (build_data == null))
		{
			// Return message
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_build_cannot")); // "We cannot build that."
			return;
		}

		// Check whether this structure can be built on this map.
		if (build_data.land != BuildData.LAND_FLAG_ALL)
		{
			if ((((build_data.land & BuildData.LAND_FLAG_MAINLAND) == 0) && (userData.mapID == Constants.MAINLAND_MAP_ID)) ||
				  (((build_data.land & BuildData.LAND_FLAG_HOMELAND) == 0) && (userData.mapID > Constants.MAINLAND_MAP_ID)))
			{
				// Return message
				OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_build_cannot")); // "We cannot build that."
				return;
			}
		}

		// Check whether the nation already has the maximum count of this build.
		if ((build_data.max_count != -1) && (nationData.GetBuildCount(build_data.ID) >= build_data.max_count))
		{
			// Return message
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_build_cannot")); // "We cannot build that."
			return;
		}

		if (((build_data.type == BuildData.TYPE_MANPOWER_STORAGE) || (build_data.type == BuildData.TYPE_ENERGY_STORAGE)) && (nationData.num_share_builds >= Constants.MAX_NUM_SHARE_BUILDS))
		{
			// Return message
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_build_max_storage", "max", String.valueOf(Constants.MAX_NUM_SHARE_BUILDS))); // "We've reached the maximum of {max} storage structures."
			return;
		}

		// Get the nation's footprint.
		Footprint footprint = nationData.GetFootprint(land_map.ID);

		if (land_map.ID == Constants.MAINLAND_MAP_ID)
		{
			// If we don't have enough manpower, attempt to take manpower from allies.
			if (footprint.manpower < build_data.manpower_cost) {
				TakeManpowerFromAllies(nationData);
			}
		}

		// Check whether the nation has enough manpower to build this.
		if (footprint.manpower < build_data.manpower_cost)
		{
			// Return message
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_build_not_enough_manpower", "missing_amount", String.valueOf((int)(build_data.manpower_cost - footprint.manpower)))); // "We don't have the manpower to build this. We need " + (build_data.manpower_cost - nationData.manpower) + " more."
			return;
		}

		// Determine the structure's energy burn rate for this nation.
		float energy_burn_rate = nationData.DetermineDiscountedEnergyBurn(build_data);
		//Output.PrintToScreen("Build(): energy_burn_rate: " + energy_burn_rate + ", build's rate: " + build_data.energy_burn_rate + ", discounts: " + nationData.structure_discount + "," + nationData.wall_discount);

		if (land_map.ID == Constants.MAINLAND_MAP_ID)
		{
			// If we don't have enough energy, attempt to take energy from allies.
			if (nationData.energy < energy_burn_rate) {
				TakeEnergyFromAllies(nationData);
			}

			// Check whether the nation has enough energy to build this (one hour's worth of its energy burn rate).
			if (nationData.energy < energy_burn_rate)
			{
				// Return message
				OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_build_not_enough_energy", "missing_amount", String.valueOf((int)(energy_burn_rate - nationData.energy)))); // "We don't have the energy to build this. We need " + (energy_burn_rate - nationData.available_energy) + " more."
				return;
			}
		}
		else
		{
			// On homeland or raid, require that there is excess energy generation in order to build.
			if ((energy_burn_rate > 0) && (nationData.GetFinalEnergyRate(land_map.ID) <= nationData.GetFinalEnergyBurnRate(land_map.ID)))
			{
				// Return message
				OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_build_not_enough_energy_gen")); // "We are not generating enough energy to support any more defenses."
				return;
			}
		}

		// Get the block's data and extended data.
		BlockData block_data = land_map.GetBlockData(_x, _y);
		BlockExtData block_ext_data = land_map.GetBlockExtendedData(_x, _y, true);

		if ((block_data == null) || (block_ext_data == null)) {
			return;
		}

		// If the block contains a build object that has crumbled, remove that build object.
		land_map.CheckForBuildObjectCrumble(_x, _y, block_data, block_ext_data);

		// Make sure this nation owns the block to be built on.
		if (block_data.nationID != nationID)
		{
			// Return message
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_build_bad_location")); // "We cannot build there."
			return;
		}

		if (block_ext_data.objectID != -1)
		{
			// Return message
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_build_already_occupied")); // "This area is already occupied."
			Output.PrintToScreen("ERROR: Attempt to build in square " + _x + "," + _y + " where there is already an object with ID " + block_ext_data.objectID + ", time until crumbles: " + (block_ext_data.crumble_time - Constants.GetTime()) + ". User: '" + userData.name + "' (" + userData.ID + ") of nation '" + nationData.name + "' (" + userData.nationID + ").");
			return;
		}

		if (userData.mapID == Constants.MAINLAND_MAP_ID)
		{
			// Record this latest action in the user's DeviceData, for fealty.
			userData.client_thread.GetDeviceData().RecordFealty(nationData);
		}

		// Build the structure
		Build(userData.mapID, block_ext_data, nationData, build_data, energy_burn_rate);

		//Output.PrintToScreen("Building object with creation_time: " + block_ext_data.creation_time + ", block_extended_data.completion_time: " + block_ext_data.completion_time + ", build_time: " + build_data.build_time);

		// Update the nation's prev_active_time
		nationData.prev_active_time = Constants.GetTime();

		// Update quests system for this build.
		Quests.HandleBuild(nationData, build_data, 0);

		// If it is a shard that has been built, update the raid system.
		if (build_data.type == BuildData.TYPE_SHARD) {
			Raid.OnBuildShard(nationData);
		}

		// If this build took place on a raid map, update the raid system for this build.
		if (land_map.ID >= Raid.RAID_ID_BASE) {
			Raid.OnBuild(land_map, nationData);
		}

		// Update the nation's users' reports.
		if (build_data.type == BuildData.TYPE_WALL) {
			nationData.ModifyUserReportValueInt(UserData.ReportVal.report__walls_built, 1);
		} else if ((build_data.type != BuildData.TYPE_ENERGY_STORAGE) && (build_data.type != BuildData.TYPE_MANPOWER_STORAGE)) {
			nationData.ModifyUserReportValueInt(UserData.ReportVal.report__defenses_built, 1);
		}

		// Mark nation and block data to be updated
		DataManager.MarkBlockForUpdate(land_map, _x, _y);
		DataManager.MarkForUpdate(nationData);

		// Broadcast a stats event to this nation, in case of change to energy burn rate or change due to object capacity. So, no need to send update bars event.
		OutputEvents.BroadcastStatsEvent(nationData.ID, 0);

		// Broadcast the change to this block to all local clients.
		OutputEvents.BroadcastBlockExtendedDataEvent(land_map, _x, _y);

		// If this event took place on a raid map, record it in the raid's replay.
		if (land_map.ID >= Raid.RAID_ID_BASE) {
			Raid.RecordEvent_ExtendedData(land_map.ID, land_map, _x, _y);
		}
	}

	public static void Upgrade(StringBuffer _output_buffer, int _userID, int _buildID, int _x, int _y)
	{
		// Get the user's data
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		if (userData.rank > Constants.RANK_GENERAL)
		{
			// Return message
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_upgrade_rank_too_low")); // "You cannot upgrade until you are promoted to General."
			return;
		}

		// Do not allow the user to upgrade if they are restricted by fealty. This should be caught first by the client.
		if ((userData.mapID == Constants.MAINLAND_MAP_ID) && (userData.fealty_end_time > Constants.GetTime()))
		{
			Output.PrintToScreen("Attempt to upgrade when restricted by fealty, by user " + userData.name + " (" + userData.ID + ").");
			return;
		}

		// If the user is in a raid that has finished, do nothing.
		if (Raid.IsInFinishedRaid(userData)) {
			return;
		}

		// Determine ID of the user's nation
		int nationID = userData.nationID;

		// Get the user's nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);
		NationTechData nationTechData = (NationTechData)DataManager.GetData(Constants.DT_NATIONTECH, nationID, false);

		// Get the build data
		BuildData build_data = BuildData.GetBuildData(_buildID);

		if ((build_data == null) || (nationData == null) || (nationTechData == null)) {
			return;
		}

		// Get the nation's footprint.
		Footprint footprint = nationData.GetFootprint(userData.mapID);

		if (userData.mapID == Constants.MAINLAND_MAP_ID)
		{
			// If we don't have enough manpower, attempt to take manpower from allies.
			if (footprint.manpower < build_data.manpower_cost) {
				TakeManpowerFromAllies(nationData);
			}
		}

		// Check whether the nation has enough manpower to build this.
		if (footprint.manpower < build_data.manpower_cost)
		{
			// Return message
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_build_not_enough_manpower", "missing_amount", String.valueOf((int)(build_data.manpower_cost - footprint.manpower)))); // "We don't have the manpower to build this. We need " + (build_data.manpower_cost - nationData.manpower) + " more."
			return;
		}

		// Determine the structure's energy burn rate for this nation.
		float energy_burn_rate = nationData.DetermineDiscountedEnergyBurn(build_data);

		if (userData.mapID == Constants.MAINLAND_MAP_ID)
		{
			// If we don't have enough energy, attempt to take energy from allies.
			if (nationData.energy < energy_burn_rate) {
				TakeEnergyFromAllies(nationData);
			}
		}

		/*
		// Check whether the nation has enough energy to build this (one hour's worth of its energy burn rate).
		if (nationData.energy < energy_burn_rate)
		{
			// Return message
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_build_not_enough_energy", "missing_amount", String.valueOf((int)(energy_burn_rate - nationData.energy)))); // "We don't have the energy to build this. We need " + (energy_burn_rate - nationData.energy) + " more."
			return;
		}
		*/

		// Get the land map
		LandMap land_map = DataManager.GetLandMap(userData.mapID, false);

		if (land_map == null)
		{
			Output.PrintToScreen("ERROR: Upgrade() user " + userData.name + " (" + userData.ID + ") has mapID " + userData.mapID + "; land_map == null.");
			return;
		}

		// Get the block's data and extended data.
		BlockData block_data = land_map.GetBlockData(_x, _y);
		BlockExtData block_ext_data = land_map.GetBlockExtendedData(_x, _y, true);

		if ((block_data == null) || (block_ext_data == null)) {
			return;
		}

		// If the block contains a build object that has crumbled, remove that build object.
		land_map.CheckForBuildObjectCrumble(_x, _y, block_data, block_ext_data);

		// Make sure this nation owns the block to be built on.
		if (block_data.nationID != nationID)
		{
			// Return message
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_build_bad_location")); // "We cannot build there."
			return;
		}

		if ((block_ext_data.objectID == -1) || (block_ext_data.owner_nationID != userData.nationID) || (nationTechData.available_upgrades.containsKey(block_ext_data.objectID) == false) || (nationTechData.available_upgrades.get(block_ext_data.objectID) != _buildID))
		{
			// Return message
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_upgrade_unavailable")); // "We cannot build that upgrade here."
			return;
		}

		// Get the original object's build data
		BuildData original_build_data = BuildData.GetBuildData(block_ext_data.objectID);

		// Determine the original structure's energy burn rate for this nation.
		float original_energy_burn_rate = nationData.DetermineDiscountedEnergyBurn(original_build_data);

		// Remove from the nation any storage capacity the original object may have.
		ModifyStatsForObjectCapacity(nationData, original_build_data, true, false);

		// If the original build has a max count, keep track of this nation's count of this build.
		if (original_build_data.max_count != -1) {
			nationData.ModifyBuildCount(original_build_data.ID, -1);
		}

		if (userData.mapID == Constants.MAINLAND_MAP_ID)
		{
			// Record this latest action in the user's DeviceData, for fealty.
			userData.client_thread.GetDeviceData().RecordFealty(nationData);
		}

		// Build the upgrade structure
		Build(userData.mapID, block_ext_data, nationData, build_data, energy_burn_rate - original_energy_burn_rate);

		//Output.PrintToScreen("Building upgrade object with creation_time: " + block_ext_data.creation_time + ", block_extended_data.completion_time: " + block_ext_data.completion_time + ", build_time: " + build_data.build_time);

		// Update the nation's prev_active_time
		nationData.prev_active_time = Constants.GetTime();

		// Update quests system for this upgrade.
		Quests.HandleUpgrade(nationData, 0);

		// Mark nation and block data to be updated
		DataManager.MarkBlockForUpdate(land_map, _x, _y);
		DataManager.MarkForUpdate(nationData);

		// Broadcast a stats event to this nation, in case of change to energy burn rate or change due to object capacity. So, no need to send update bars event.
		OutputEvents.BroadcastStatsEvent(nationID, 0);

		// Broadcast the change to this block to all local clients.
		OutputEvents.BroadcastBlockExtendedDataEvent(land_map, _x, _y);

		// If this event took place on a raid map, record it in the raid's replay.
		if (land_map.ID >= Raid.RAID_ID_BASE) {
			Raid.RecordEvent_ExtendedData(land_map.ID, land_map, _x, _y);
		}
	}

	public static void Build(int _landmapID, BlockExtData _block_ext_data, NationData _nationData, BuildData _buildData, float _energy_burn_rate_delta)
	{
		if ((_block_ext_data == null) || (_nationData == null) || (_buildData == null)) {
			return;
		}

		// Build the structure
		_block_ext_data.objectID = _buildData.ID;
		_block_ext_data.owner_nationID = _nationData.ID;
		_block_ext_data.creation_time = Constants.GetTime();
		_block_ext_data.completion_time = Constants.GetTime() + _buildData.build_time;
		_block_ext_data.invisible_time = DetermineBuildInvisibileTime(_buildData, _nationData);
		_block_ext_data.capture_time = -1;
		_block_ext_data.crumble_time = -1;

		// Add to the nation any storage capacity this new object may have.
		ModifyStatsForObjectCapacity(_nationData, _buildData, false, false);

		// Take cost from nation
		Footprint footprint = _nationData.GetFootprint(_landmapID);
		footprint.manpower = Math.max(0, footprint.manpower - _buildData.manpower_cost);
		ModifyEnergyBurnRate(_nationData, _landmapID, _energy_burn_rate_delta);

		// If this build has a max count, keep track of this nation's count of this build.
		if (_buildData.max_count != -1) {
			_nationData.ModifyBuildCount(_buildData.ID, 1);
		}

		// If this nation's homeland is being modified, record when it was last modified.
		if (_landmapID == _nationData.homeland_mapID) {
			_nationData.prev_modify_homeland_time = Constants.GetTime();
		}

		if (_landmapID == Constants.MAINLAND_MAP_ID)
		{
			// Update the nation's users' reports.
			_nationData.ModifyUserReportValueFloat(UserData.ReportVal.report__manpower_spent, _buildData.manpower_cost);
		}

		// If this build may affect which of this nation's build objects are inert, broadcast message to update clients.
		if (_nationData.GetFinalEnergyBurnRate(_landmapID) > _nationData.GetFinalEnergyRate(_landmapID)) {
			OutputEvents.BroadcastUpdateEvent(_nationData.ID);
		}
	}

	public static void Complete(StringBuffer _output_buffer, int _userID, int _x, int _y)
	{
		// Get the user's data
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		if (userData.rank > Constants.RANK_GENERAL)
		{
			// Return message
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_complete_rank_too_low")); // "You cannot complete builds until you are promoted to General."
			return;
		}

		// Determine ID of the user's nation
		int nationID = userData.nationID;

		// Get the user's nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

		if (nationData == null) {
			return;
		}

		// Get the land map
		LandMap land_map = DataManager.GetLandMap(userData.mapID, false);

		if (land_map == null)
		{
			Output.PrintToScreen("ERROR: Complete() user " + userData.name + " (" + userData.ID + ") has mapID " + userData.mapID + "; land_map == null.");
			return;
		}

		// Get the block's data and extended data.
		BlockData block_data = land_map.GetBlockData(_x, _y);
		BlockExtData block_ext_data = land_map.GetBlockExtendedData(_x, _y, true);

		if ((block_data == null) || (block_ext_data == null)) {
			return;
		}

		// Make sure this nation owns the block to be built on.
		if (block_data.nationID != nationID)
		{
			// Return message
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_build_bad_location")); // "We cannot build there."
			return;
		}

		// Get the current time.
		int cur_time = Constants.GetTime();

		if ((block_ext_data.objectID == -1) || (block_ext_data.owner_nationID != userData.nationID) || (block_ext_data.completion_time == -1) || (block_ext_data.completion_time <= cur_time))
		{
			// Do nothing.
			return;
		}

		// Determine the cost in credits to complete this build.
		int completionCost = ((int)(block_ext_data.completion_time - cur_time) * BuildData.COMPLETION_COST_PER_MINUTE / 60) + 1;

		// Check whether the nation has enough credits to build this.
		if (nationData.game_money < completionCost)
		{
			// Return message
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_build_not_enough_credits", "missing_amount", String.valueOf((int)(completionCost - nationData.game_money)))); // "We don't have enough credits to build this. We need " + (completionCost - nationData.game_money) + " more."
			return;
		}

		// Complete the object.
		block_ext_data.completion_time = cur_time;

		// Take cost from nation
		Money.SubtractCost(nationData, completionCost);

		// Update the nation's users' reports.
		nationData.ModifyUserReportValueFloat(UserData.ReportVal.report__credits_spent, completionCost);

		// Mark nation and block data to be updated
		DataManager.MarkBlockForUpdate(land_map, _x, _y);
		DataManager.MarkForUpdate(nationData);

		// Broadcast the completion event to all local clients.
		OutputEvents.BroadcastBuildCompletionEvent(land_map, _x, _y);

		// If this event took place on a raid map, record it in the raid's replay.
		if (land_map.ID >= Raid.RAID_ID_BASE) {
			Raid.RecordEvent_Complete(land_map.ID, _x, _y);
		}
	}

	public static boolean Salvage(StringBuffer _output_buffer, int _userID, int _x, int _y)
	{
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);
		LandMap land_map = DataManager.GetLandMap(userData.mapID, false);

		if (land_map == null)
		{
			Output.PrintToScreen("ERROR: Salvage() user " + userData.name + " (" + userData.ID + ") has mapID " + userData.mapID + "; land_map == null.");
			return false;
		}

		BlockData block_data = land_map.GetBlockData(_x, _y);
		BlockExtData block_ext_data = ((block_data.flags & BlockData.BF_EXTENDED_DATA) == 0) ? null : land_map.GetBlockExtendedData(_x, _y, false);

		// If the block contains a build object that has crumbled, remove that build object.
		land_map.CheckForBuildObjectCrumble(_x, _y, block_data, block_ext_data);

		// If the given block does not belong to the user's nation, do nothing.
		if (block_data.nationID != userData.nationID) {
			return false;
		}

		// If the block is currently locked, do nothing.
		if (block_data.lock_until_time > Constants.GetTime()) {
			return false;
		}

		if ((block_ext_data == null) || (block_ext_data.objectID == -1))
		{
			// Return message
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_salvage_nothing")); // "There is nothing here to salvage."
			return false;
		}

		if (block_ext_data.owner_nationID != userData.nationID)
		{
			// Return message
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_salvage_not_owned")); // "We did not build this."
			return false;
		}

		if (userData.rank > Constants.RANK_CAPTAIN)
		{
			// Return message
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_salvage_rank_too_low")); // "You cannot salvage structures until you've been promoted to Captain."
			return false;
		}

		// Salvage the structure. Don't send update bars event because Salvage() broadcasts stats event.
		int salvaged_manpower = Salvage(land_map, _x, _y, block_ext_data, userData.nationID);

		// Success
		return true;
	}

	public static int Salvage(LandMap _land_map, int _x, int _y, BlockExtData _block_ext_data, int _nationID)
	{
		// Get the user's nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationID, false);

		// Get the nation's footprint.
		Footprint footprint = nationData.GetFootprint(_land_map.ID);

		// Update this user's nation's prev_use_time to the current time
		nationData.prev_use_time = Constants.GetTime();

		// Update the nation's prev_active_time
		nationData.prev_active_time = Constants.GetTime();

		// Get the object's build data.
		BuildData build_data = BuildData.GetBuildData(_block_ext_data.objectID);

		// Remove from the nation any storage capacity this object may have.
		ModifyStatsForObjectCapacity(nationData, build_data, true, false);

		// Determine the structure's energy burn rate for this nation.
		float energy_burn_rate = nationData.DetermineDiscountedEnergyBurn(build_data);

		// Return the salvaged manpower to the nation.
		int salvaged_manpower = (int)((float)(build_data.manpower_cost) * Math.min(1f, nationData.salvage_value) + 0.5f);
		footprint.manpower += salvaged_manpower;

		// Remove the structure's energy burn rate from the nation's energy burn rate.
		ModifyEnergyBurnRate(nationData, _land_map.ID, -energy_burn_rate);

		// Broadcast a stats event to this nation, in case of change to energy burn rate or change due to object capacity.
		OutputEvents.BroadcastStatsEvent(_nationID, 0);

		// If this salvage may affect which of this nation's build objects are inert, broadcast message to update clients.
		if ((nationData.GetFinalEnergyBurnRate(_land_map.ID) + energy_burn_rate) > nationData.GetFinalEnergyRate(_land_map.ID)) {
			OutputEvents.BroadcastUpdateEvent(_nationID);
		}

		// If this build has a max count, keep track of this nation's count of this build.
		if (build_data.max_count != -1) {
			nationData.ModifyBuildCount(build_data.ID, -1);
		}

		// Remove the object from the block.
		_block_ext_data.objectID = -1;
		_block_ext_data.owner_nationID = -1;

		// If this nation's homeland is being modified, record when it was last modified.
		if (_land_map.ID == nationData.homeland_mapID) {
			nationData.prev_modify_homeland_time = Constants.GetTime();
		}

		// Broadcast the salvage event to all local viewing clients.
		OutputEvents.BroadcastSalvageEvent(_land_map, _x, _y);

		// If this event took place on a raid map, record it in the raid's replay.
		if (_land_map.ID >= Raid.RAID_ID_BASE) {
			Raid.RecordEvent_Salvage(_land_map.ID, _x, _y);
		}

		// Update quests system for this salvage
		Quests.HandleSalvage(nationData, build_data, 0);

		// Update the DB records of the nation and the block.
		DataManager.MarkForUpdate(nationData);
		DataManager.MarkBlockForUpdate(_land_map, _x, _y);

		return salvaged_manpower;
	}

	public static void MapClick(StringBuffer _output_buffer, int _userID, int _x, int _y, boolean _splash, boolean _auto)
	{
		try {
			// Get the current time
			int cur_time = Constants.GetTime();

			UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

			if (userData == null)
			{
				Output.PrintToScreen("ERROR: MapClick(): No UserData found for given userID " + _userID + "!");
				return;
			}

			// If the user is in a raid that has finished, do nothing.
			if (Raid.IsInFinishedRaid(userData)) {
				return;
			}

			LandMap land_map = DataManager.GetLandMap(userData.mapID, false);

			if (land_map == null)
			{
				Output.PrintToScreen("ERROR: MapClick() user " + userData.name + " (" + userData.ID + ") has mapID " + userData.mapID + "; land_map == null.");
				return;
			}

			BlockData blockData = land_map.GetBlockData(_x, _y);

			if (blockData == null)
			{
				Output.PrintToScreen("ERROR: MapClick(): No BlockData found for given block " + _x + "," + _y + " in map " + userData.mapID + "! User '" + userData.name + "' (" + _userID + ").");
				return;
			}

			// Determine whether this is a homeland map.
			boolean homeland_map = (userData.mapID != Constants.MAINLAND_MAP_ID) && (userData.mapID < Raid.RAID_ID_BASE);

			//// Testing
			//Output.PrintToScreen("Click on " + _x + "," + _y + ", nationID: " + blockData.nationID + ", terrain: " + blockData.terrain + ", flags: " + blockData.flags);
			//BlockExtData block_ext_data = land_map.GetBlockExtendedData(_x, _y, false);
			//if (block_ext_data != null) Output.PrintToScreen("  objectID: " + block_ext_data.objectID + ", owner_nationID: " + block_ext_data.owner_nationID + ", creation_time: " + block_ext_data.creation_time + ", completion_time: " + block_ext_data.completion_time + ", invisible_time: " + block_ext_data.invisible_time + ", capture_time: " + block_ext_data.capture_time + ", crumble_time: " + block_ext_data.crumble_time);

			// If the click is on the player's own nation, do nothing.
			if (blockData.nationID == userData.nationID) {
				return;
			}

			// Player has clicked on empty land, or land occupied by another nation.

			if ((blockData.terrain == Constants.TERRAIN_FLAT_LAND) || ((homeland_map == false) && (blockData.terrain == Constants.TERRAIN_BEACH)))
			{
				// The terrain is habitable. (Occupation of beach area is not allowed on homeland map.)

				boolean first_capture = false;
				boolean discovery_made = false;
				boolean fast_crumble = false;

				// Get the user's nation's data and footprint
				NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, userData.nationID, false);
				Footprint footprint = nationData.GetFootprint(land_map.ID);

				if ((nationData.super_nation == false) && ((IsBlockAdjacentToNation(land_map, _x, _y, userData.nationID) == false) || (blockData.lock_until_time > cur_time))) {
					return;
				}

				if ((userData.rank > Constants.RANK_WARRIOR) && (userData.admin == false))
				{
					// Return message
					OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get((blockData.nationID == -1) ? "svr_occupy_rank_too_low" : "svr_attack_rank_too_low")); // "You cannot occupy land until you've been promoted to Warrior." "You cannot attack until you've been promoted to Warrior."
					return;
				}

				if (userData.mapID == Constants.MAINLAND_MAP_ID)
				{
					// If the nation's level exceeds the target block's level limit then do not allow the nation to expand into this block.
					int block_level_limit = land_map.PosXToMaxLevelLimit(_x);
					if ((nationData.level > block_level_limit) && !userData.admin)
					{
						// Return message
						OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_attack_too_far_west")); // "We are too far advanced to move west into these primitive lands."
						return;
					}

					// Enforce eastern limit on expansion of the nation, based on its level.
					int map_position_eastern_limit = land_map.MaxLevelLimitToPosX(land_map.GetEasternLevelLimit(nationData.level));
					if ((_x >= map_position_eastern_limit)  && !userData.admin)
					{
						// Return message
						OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_attack_too_far_east")); // "We have not yet reached a high enough level to advance that far to the east."
						return;
					}

					// Enforce the nation's maximum extent
					if (BlockIsWithinNationMaxExtent(nationData, land_map.ID, _x, _y) == false)
					{
						OutputEvents.GetRequestorEvent(_output_buffer, ClientString.Get("svr_beyond_max_extent")); // "We will need to evacuate land on the other side of our nation before we can go there, because a nation cannot span an area larger than 500 by 500."
						OutputEvents.GetStopAutoProcessEvent(_output_buffer);
						return;
					}

					// Note set new_player_area_boundary to 1 to eliminate new player area.
					if ((World.new_player_area_boundary != -1) && (_y >= World.new_player_area_boundary) && (nationData.veteran || userData.veteran))
					{
						// Return message
						OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_vet_in_new_land")); // "Veteran players and nations can't occupy land in the new player area."
						return;
					}

/*
					if ((World.new_player_area_boundary != -1) && (_y < World.new_player_area_boundary) && !nationData.veteran && !userData.veteran)
					{
						// Return message
						OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("New players can't occupy land in the veteran player area.")); // "New players can't occupy land in the veteran player area."
						return;
					}
*/
				}

				if (footprint.area >= (nationData.GetSupportableArea(userData.mapID) * 2))
				{
					// Return message
					OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_area_too_large")); // "Our forces are spread over too vast an area."
					return;
				}

				// Update this user's nation's prev_use_time to the current time
				nationData.prev_use_time = cur_time;

				// Update the nation's prev_active_time
				nationData.prev_active_time = Constants.GetTime();

				// Get the nation's hit points rate
				float nation_hit_points_rate = GetNationHitPointsRate(nationData);

				if (blockData.nationID == -1)
				{
					// Player has clicked on empty land.

					if (blockData.BlockHasExtendedData() && land_map.CheckForLastingWipePreventingAttack(_x, _y, userData.nationID))
					{
						// Return message
						OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_occupy_cant_enter_area")); // "We can't enter this area yet."
						return;
					}

					//if (userData.mapID == Constants.MAINLAND_MAP_ID)
					//{
					//  // Record this latest action in the user's DeviceData, for fealty.
					//  userData.client_thread.GetDeviceData().RecordFealty(nationData);
					//}

					// Reserve one of this user's available processes. If none are available, do nothing.
					if (ReserveUserProcess(userData, nationData, Constants.PROCESS_DURATION) == false) {
						return;
					}

					// Determine how many hit points the target block currently has.
					int full_hit_points = land_map.DetermineBlockFullHitPoints(_x, _y, false, null);
					int block_hit_points_at_attack_start = blockData.GetBlockCurrentHitPoints(full_hit_points, GetNationHitPointsRate(null), cur_time);
					float block_hit_points_restored_during_attack = blockData.GetBlockHitPointsRestored(Constants.VACANT_BLOCK_HIT_POINTS_RATE, Constants.PROCESS_DURATION);

					//Output.PrintToScreen("Block hit points: " + block_hit_points + "/" + full_hit_points + ", cur time: " + cur_time + ", hit_points_restored_time: " + blockData.hit_points_restored_time);

					int remaining_hit_points = (nationData.GetFinalManpowerPerAttack() >= (block_hit_points_at_attack_start + block_hit_points_restored_during_attack)) ? 0 : Math.min(full_hit_points, (int)((block_hit_points_at_attack_start + block_hit_points_restored_during_attack - (int)(nationData.GetFinalManpowerPerAttack()) + 0.5f)));

					// Record when the block's transition will be completed, so that it will not count in adjacency checks until that time.
					blockData.transition_complete_time = cur_time + Constants.PROCESS_DURATION;

					// Encode the process display event
					Constants.EncodeString(_output_buffer, "display_process"); // Event ID
					Constants.EncodeUnsignedNumber(_output_buffer, Constants.PROCESS_DURATION, 1); // Process duration in seconds
					Constants.EncodeNumber(_output_buffer, _x, 3); // Map x coord
					Constants.EncodeNumber(_output_buffer, _y, 3); // Map y coord
					Constants.EncodeUnsignedNumber(_output_buffer, full_hit_points, 2); // Max hit points
					Constants.EncodeUnsignedNumber(_output_buffer, block_hit_points_at_attack_start, 2); // Starting hit points
					Constants.EncodeUnsignedNumber(_output_buffer, remaining_hit_points, 2); // Ending hit points

					// The block now has only its remaining hit points.
					int cur_hit_points = remaining_hit_points;

					if (remaining_hit_points == 0)
					{
						// Convert the given square to the user's nation. Don't broadcast this change to viewing clients.
						World.SetBlockNationID(land_map, _x, _y, userData.nationID, true, false, _userID, Constants.PROCESS_DURATION);

						// The block has now been restored to full hit points for the nation that has taken it over (or not yet full if a defense has been recaptured).
						full_hit_points = land_map.DetermineBlockFullHitPoints(_x, _y, false, null);
						cur_hit_points = land_map.DetermineBlockFullHitPoints(_x, _y, true, null);
						if (cur_hit_points < full_hit_points) {
							blockData.SetHitPointsRestoredTime(cur_time + Constants.PROCESS_DURATION + (int)((float)(full_hit_points - cur_hit_points) / (nation_hit_points_rate / 60.0) + 0.5f)/*, land_map, _x, _y*/);
						}

						// Get the nation's tech data.
						NationTechData nationTechData = (NationTechData)DataManager.GetData(Constants.DT_NATIONTECH, userData.nationID, false);

						// Determine whether this captured block contains a resource or orb presently being captured by this nation for the first time.
						first_capture = ((blockData.BlockHasExtendedData()) && (land_map.GetBlockObjectID(_x, _y) >= ObjectData.RESOURCE_OBJECT_BASE_ID) && (nationTechData.object_capture_history.get(Constants.TokenizeCoordinates(_x, _y)) == Constants.GetTime()));

						// If this event took place on a raid map, record it in the raid's replay history.
						if (land_map.ID >= Raid.RAID_ID_BASE) {
							Raid.RecordEvent_SetNationID(land_map.ID, _x, _y, userData.nationID, Constants.PROCESS_DURATION);
						}
					}
					else
					{
						// The target block has lost hit points. Record the time at which they will all have been restored.
						blockData.SetHitPointsRestoredTime(cur_time + Constants.PROCESS_DURATION + (int)((float)(full_hit_points - remaining_hit_points) / (GetNationHitPointsRate(null) / 60.0) + 0.5f)/*, land_map, _x, _y*/);
					}

					// Record time when this attack on the block will be complete.
					blockData.SetAttackCompleteTime(cur_time + Constants.PROCESS_DURATION);

					// Attempt to make a random discovery in this newly occupied square.
					discovery_made = AttemptDiscovery(nationData, null, _userID, land_map, _x, _y, Constants.DISCOVERY_PROBABILITY_OCCUPY, Constants.PROCESS_DURATION);
					//discovery_made = true; // TESTING

					// Finish encoding the display process event.
					Constants.EncodeNumber(_output_buffer, (int)(nationData.GetFootprint(land_map.ID).geo_efficiency_base * 1000), 4); // Ending geo efficiency
					Constants.EncodeUnsignedNumber(_output_buffer, (first_capture ? Constants.PROCESS_FLAG_FIRST_CAPTURE : 0) | (discovery_made ? Constants.PROCESS_FLAG_DISCOVERY : 0), 1); // Process flags

					// Broadcast a block process event for this occupation to all clients in the view area.
					OutputEvents.BroadcastBlockProcessEvent(land_map, _x, _y, blockData, full_hit_points, cur_hit_points, GetNationHitPointsRate(nationData), Constants.PROCESS_DURATION, Constants.PROCESS_OCCUPY);
				}
				else
				{
					// Player has clicked on land occupied by another nation.

					int xp_to_add = 0;

					// Do not allow an attack on one of the nation's allies
					if (nationData.alliances_active.indexOf(Integer.valueOf(blockData.nationID)) != -1)
					{
						// Return message
						OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_attack_have_alliance")); // "We have an alliance with this nation."
						return;
					}

					if (land_map.ID == Constants.MAINLAND_MAP_ID)
					{
						// If we don't have enough manpower, attempt to take manpower from allies.
						if (footprint.manpower < nationData.GetFinalManpowerPerAttack()) {
							TakeManpowerFromAllies(nationData);
						}

						if (footprint.manpower < nationData.GetFinalManpowerPerAttack())
						{
							// Return message
							OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_attack_not_enough_manpower")); // "We need to wait until we have enough manpower."
							return;
						}
					}

					if (blockData.BlockHasExtendedData() && land_map.CheckForLastingWipePreventingAttack(_x, _y, userData.nationID))
					{
						// Return message
						OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_occupy_cant_enter_area")); // "We can't enter this area yet."
						return;
					}

					// Get the target nation's data
					NationData targetNationData = (NationData)DataManager.GetData(Constants.DT_NATION, blockData.nationID, false);
					Footprint target_nation_footprint = targetNationData.GetFootprint(land_map.ID);
					float target_nation_hit_points_rate = GetNationHitPointsRate(targetNationData);

					// Determine how many hit points the target block currently has.
					int[] hp_flags = {0};
					int full_hit_points = land_map.DetermineBlockFullHitPoints(_x, _y, false, hp_flags);
					int old_full_hit_points = full_hit_points;
					int block_hit_points_at_attack_start = blockData.GetBlockCurrentHitPoints(full_hit_points, target_nation_hit_points_rate, cur_time);
					float block_hit_points_restored_during_attack = blockData.GetBlockHitPointsRestored(target_nation_hit_points_rate, Constants.BATTLE_DURATION);
					//Output.PrintToScreen("block_hit_points_at_attack_start: " + block_hit_points_at_attack_start + ", block_hit_points_restored_during_attack: " + block_hit_points_restored_during_attack + ", full_hit_points: " + full_hit_points + ", cur time: " + cur_time + ", hit_points_restored_time: " + blockData.hit_points_restored_time);

					if (block_hit_points_at_attack_start < 0)
					{
						Output.PrintToScreen("MapClick() Error: block_hit_points_at_attack_start < 0 (" + block_hit_points_at_attack_start + "). hit_points_restored_time: " + blockData.hit_points_restored_time + ", cur_time: " + cur_time + ", target_nation_hit_points_rate:" + target_nation_hit_points_rate);
						block_hit_points_at_attack_start = 0;
					}

					if (targetNationData == null)
					{
						Output.PrintToScreen("Attempt to attack nation " + blockData.nationID + " which is missing data.");

						// Convert the target square to the user's nation Don't broadcast this change to viewing clients.
						World.SetBlockNationID(land_map, _x, _y, userData.nationID, true, true, _userID, 0);
						return;
					}

					// Do not allow the user to attack if they are restricted by fealty. This should be caught first by the client.
					if ((userData.mapID == Constants.MAINLAND_MAP_ID) && (userData.fealty_end_time > cur_time))
					{
						Output.PrintToScreen("Attempt to attack when restricted by fealty, by user " + userData.name + " (" + userData.ID + ").");
						return;
					}

					if (userData.mapID == Constants.MAINLAND_MAP_ID)
					{
						// Record this latest attack in the user's DeviceData, for fealty.
						userData.client_thread.GetDeviceData().RecordFealty(nationData);
					}

					// Reserve one of this user's available processes. If none are available, do nothing.
					if (ReserveUserProcess(userData, nationData, Constants.BATTLE_DURATION) == false) {
						return;
					}

					// Remove any obsolete technologies from the target nation
					Technology.UpdateStats(blockData.nationID, targetNationData);

					// If the block being attacked has a tower that is triggered by a direct attack, trigger it (whether or not it's about to be destroyed).
					boolean flanked = false;
					if (land_map.CheckForBuildObjectWithTriggerType(_x, _y, BuildData.TRIGGER_ON_DIRECT_ATTACK, userData.nationID, blockData.nationID, _x, _y)) {
						flanked = TriggerObject(land_map, _x, _y, userData.nationID, _x, _y);
					}

					// Trigger any nearby tower that would be triggered by this attack occuring within its attack radius.
					TriggerObjectsInRange(land_map, _x, _y, userData.nationID, blockData.nationID, BuildData.TRIGGER_ON_RADIUS_ATTACK);

					// If the block being attacked has no object in it...
					if ((blockData.BlockHasExtendedData() == false) || (land_map.GetBlockObjectID(_x, _y) == -1))
					{
						// Trigger any nearby tower that would be triggered by this attack upon an empty square occuring within its attack radius.
						TriggerObjectsInRange(land_map, _x, _y, userData.nationID, blockData.nationID, BuildData.TRIGGER_ON_RADIUS_ATTACK_EMPTY);
					}

					// Determine battle flags
					int battle_flags = (land_map.BlockContainsInertObject(_x, _y) ? Constants.BATTLE_FLAG_INERT : 0) |
														 (flanked ? Constants.BATTLE_FLAG_FLANKED : 0) | hp_flags[0];

					// Determine whether this is a critial hit.
					float crit_multiplier = 1f;
					if (nationData.RollForCrit())
					{
						crit_multiplier = 2f;
						battle_flags = battle_flags | Constants.BATTLE_FLAG_CRIT;
					}

					// Determine what stats the attacker and defender will use in combat
					DetermineCombatStats(userData.mapID, crit_multiplier, nationData, targetNationData, combat_stats);
					float attacker_best_ratio = combat_stats.attacker_best_ratio;

					// Record the attacker nation's latest attack ratio. Record original ration rathr than after adding randomness to it, so that XP and trophy rewards don't vary randomly.
					nationData.prev_attack_ratio = combat_stats.attacker_best_ratio;

					// Add random noise to the battle's ratio.
					attacker_best_ratio = attacker_best_ratio * (0.5f + Constants.random.nextFloat());

					// If attacker or defender is a super nation, adjust result as appropriate.
					if (nationData.super_nation) {
						attacker_best_ratio = 1000000.0f;
					} else if (targetNationData.super_nation) {
						attacker_best_ratio = 0.0f;
					}

					// Determine how much manpower will remain for the attacker, and how many hit points will remain for the target, after this attack.
					int remaining_manpower, remaining_hit_points;
					if ((nationData.GetFinalManpowerPerAttack() * attacker_best_ratio) >= (block_hit_points_at_attack_start + block_hit_points_restored_during_attack))
					{
						remaining_manpower = (int)(nationData.GetFinalManpowerPerAttack() - ((block_hit_points_at_attack_start + block_hit_points_restored_during_attack) / attacker_best_ratio));
						int max_remaining_manpower = (int)((nationData.GetFinalManpowerPerAttack() * (1f - Constants.ATTACK_MIN_MANPOWER_COST)) + 0.5f);
						remaining_manpower = Math.min(remaining_manpower, max_remaining_manpower);
						remaining_hit_points = 0;
					}
					else
					{
						remaining_manpower = 0;
						remaining_hit_points = Math.min(full_hit_points, (int)((block_hit_points_at_attack_start + block_hit_points_restored_during_attack) - (nationData.GetFinalManpowerPerAttack() * attacker_best_ratio) + 0.5f));
					}

					if (remaining_hit_points < 0)
					{
						Output.PrintToScreen("ERROR: remaining_hit_points (" + remaining_hit_points + ") < 0!");
						Output.PrintStackTrace();
					}

					// Determine the appropriate amount of manpower to take from the attacking nation.
					float manpower_cost = nationData.GetFinalManpowerPerAttack() - remaining_manpower;

					// Lock the block for the duration of the battle, so no player clicks will cause changes to it during that time.
					blockData.lock_until_time = cur_time + Constants.BATTLE_DURATION;
					//Output.PrintToScreen("Block locked until " + blockData.lock_until_time);

					if (remaining_hit_points == 0)
					{
						// Determine the number of xp to add to the attacking nation for this attack.
						// XP is only added if the target square has been defeated. It is based on the square's original number of hit points.
						// Geo efficiency is factored in so that attacking weakened nations provides a proportionally smaller amount of XP.
						xp_to_add += DetermineXPToAdd(userData.mapID, nationData, targetNationData, combat_stats.attacker_best_ratio, (int)Math.max(Constants.INIT_HIT_POINT_BASE, full_hit_points * targetNationData.GetFinalGeoEfficiency(userData.mapID)));

						// If this land capture was from an opponent worthy enough that the nation gained xp...
						if (xp_to_add > 0)
						{
							// Increment the nation's counts of captured land sqaures.
							nationData.captures_history++;
							nationData.captures_history_monthly++;

							// Update the ranks for captures history.
							RanksData.instance.ranks_nation_captures.UpdateRanks(nationData.ID, nationData.name, nationData.captures_history, Constants.NUM_CAPTURES_RANKS, false);
							RanksData.instance.ranks_nation_captures_monthly.UpdateRanks(nationData.ID, nationData.name, nationData.captures_history_monthly, Constants.NUM_CAPTURES_RANKS, false);
						}
					}

					// Update the block's object invisibility for this attack.
					UpdateObjectInvisibilityForAttack(land_map, _x, _y, blockData);

					// Get the user's nation's tech data.
					NationTechData nationTechData = (NationTechData)DataManager.GetData(Constants.DT_NATIONTECH, userData.nationID, false);

					// Determine whether the attacking nation has a temp tech that should produce a visual effect.
					int effect_techID = -1;
					for (Integer techID : nationTechData.tech_temp_expire_time.keySet())
					{
						if (((techID == 306) && _splash) || // Radioactive Fallout
								((techID == 360) && _splash) || // Contagion
								((techID == 409) && _splash) || // Psychic Shockwave
								((techID == 465) && _splash) || // Guerrilla Warfare
								((techID == 515) && _splash))   // Battle Plague
						{
							effect_techID = techID;
							break;
						}
					}

					if (nationData.log_suspect_expire_time > cur_time)
					{
						// Log the details of this attack.
						Constants.WriteToLog("log_suspect.txt", Constants.GetTimestampString() + ": '" + userData.name + "'(" + userData.ID + ") of '" + nationData.name + "'(ID:" + nationData.ID + ", Level:" + nationData.level + ") attacked '" + targetNationData.name + "'(ID:" + targetNationData.ID + ", Level:" + targetNationData.level + ", Geo:" + targetNationData.GetFinalGeoEfficiency(userData.mapID) + ") at block " + _x + "," + _y + " containing object " + land_map.GetBlockObjectID(_x, _y) + ". Full HP: " + full_hit_points + ", HP pre-attack: " + block_hit_points_at_attack_start + ", Remaining HP: " + remaining_hit_points + ", manpower_cost: " + manpower_cost + ", attack ratio: " + combat_stats.attacker_best_ratio + ", XP: " + xp_to_add + ".\n");
					}

					// Handle splash damage to surrounding blocks, if appropriate.
					if (_splash && (nationData.splash_damage > 0))
					{
						BlockData splashBlockData;
						int splash_block_full_hit_points, splash_block_hit_points_at_attack_start, splash_block_remaining_hit_points, splash_block_old_full_hit_points, splash_block_remaining_manpower;
						float splash_block_hit_points_restored_during_attack;
						boolean splash_block_fast_crumble = false;

						int splash_min_manpower_cost = (int)((nationData.GetFinalManpowerPerAttack() * nationData.splash_damage * Constants.ATTACK_MIN_MANPOWER_COST) + 0.5f);

						// Iterate through all blocks that immediately surround the attacked block, and apply splash damage to any that belong to the target nation.
						for (int y = _y - 1; y <= _y + 1; y++)
						{
							for (int x = _x - 1; x <= _x + 1; x++)
							{
								// Get the data for the current splash block.
								splashBlockData = land_map.GetBlockData(x, y);

								// If the current splash block doesn't belong to the target nation, skip it.
								if (splashBlockData.nationID != targetNationData.ID) {
									continue;
								}

								// Skip the central block, the target of the attack, as damage to it has already been handled above.
								if ((x == _x) && (y == _y)) {
									continue;
								}

								// If the nation doesn't have enough manpower for a full attack, splash cannot occur.
								if (footprint.manpower < nationData.GetFinalManpowerPerAttack()) {
									break;
								}

								// Determine how many hit points the target block currently has, and will be left with.
								splash_block_full_hit_points = land_map.DetermineBlockFullHitPoints(x, y, false, null);
								splash_block_old_full_hit_points = splash_block_full_hit_points;
								splash_block_hit_points_at_attack_start = splashBlockData.GetBlockCurrentHitPoints(splash_block_full_hit_points, target_nation_hit_points_rate, cur_time);
								splash_block_hit_points_restored_during_attack = splashBlockData.GetBlockHitPointsRestored(target_nation_hit_points_rate, Constants.BATTLE_DURATION);

								if (splash_block_hit_points_at_attack_start < 0)
								{
									Output.PrintToScreen("MapClick() Error: splash_block_hit_points_at_attack_start < 0 (" + splash_block_hit_points_at_attack_start + "). hit_points_restored_time: " + splashBlockData.hit_points_restored_time + ", cur_time: " + cur_time + ", target_nation_hit_points_rate:" + target_nation_hit_points_rate);
									splash_block_hit_points_at_attack_start = 0;
								}

								// Determine how much manpower will remain for the attacker, and how many hit points will remain for the target, after this attack.
								if ((nationData.GetFinalManpowerPerAttack() * attacker_best_ratio * nationData.splash_damage) >= (splash_block_hit_points_at_attack_start + splash_block_hit_points_restored_during_attack))
								{
									splash_block_remaining_manpower = (int)(nationData.GetFinalManpowerPerAttack() - ((splash_block_hit_points_at_attack_start + splash_block_hit_points_restored_during_attack) / attacker_best_ratio));
									splash_block_remaining_hit_points = 0;
								}
								else
								{
									splash_block_remaining_manpower = 0;
									splash_block_remaining_hit_points = Math.min(splash_block_full_hit_points, (int)((splash_block_hit_points_at_attack_start + splash_block_hit_points_restored_during_attack) - (nationData.GetFinalManpowerPerAttack() * attacker_best_ratio * nationData.splash_damage) + 0.5f));
								}

								if (splash_block_remaining_hit_points < 0)
								{
									Output.PrintToScreen("ERROR: splash_block_remaining_hit_points (" + splash_block_remaining_hit_points + ") < 0!");
									Output.PrintStackTrace();
								}

								// Determine the appropriate amount of manpower to take from the attacking nation.
								manpower_cost += Math.max(splash_min_manpower_cost, (nationData.GetFinalManpowerPerAttack() * nationData.splash_damage) - splash_block_remaining_manpower);

								//// TESTING
								//Output.PrintToScreen("Splash on block " + x + "," + y + "; splash_block_full_hit_points: " + splash_block_full_hit_points + ", splash_block_hit_points_at_attack_start: " + splash_block_hit_points_at_attack_start + ", splash_block_hit_points_restored_during_attack: " + splash_block_hit_points_restored_during_attack + ", splash_block_remaining_hit_points: " + splash_block_remaining_hit_points + ", splash damage: " + (nationData.GetFinalManpowerPerAttack() * attacker_best_ratio * nationData.splash_damage));

								// If this block is outside of the attacking nation's max extent, they cannot occupy it, so make sure at least 1 hit point remains.
								if ((splash_block_remaining_hit_points == 0) && (BlockIsWithinNationMaxExtent(nationData, land_map.ID, x, y) == false)) {
									splash_block_remaining_hit_points = 1;
								}

								// The splash block now has only its remaining hit points.
								int splash_block_cur_hit_points = splash_block_remaining_hit_points;

								// If the splash damage has defeated this block...
								if (splash_block_remaining_hit_points == 0)
								{
									// Have the attacking nation occupy the target block. Don't broadcast this change to viewing clients.
									World.SetBlockNationID(land_map, x, y, userData.nationID, true, false, _userID, Constants.BATTLE_DURATION);

									// If the target nation's final block on the mainland has been defeated, and the nation is online, displace the nation to a different location nearby.
									// Do this after the call to SetBlockNationID() above, so that the tournament system can respond to the attacker capturing the square from the defender.
									if ((land_map.ID == Constants.MAINLAND_MAP_ID) && (target_nation_footprint.area == 0) && (targetNationData.num_members_online > 0)) {
										World.DisplaceNation(land_map, targetNationData, x, y);
									}

									// Determine the number of xp to add to the attacking nation for this splash attack.
									// XP is only added if the splash target square has been defeated. It is based on the square's original number of hit points.
									// Geo efficiency is factored in so that attacking weakened nations provides a proportionally smaller amount of XP.
									xp_to_add += DetermineXPToAdd(userData.mapID, nationData, targetNationData, combat_stats.attacker_best_ratio, (int)Math.max(Constants.INIT_HIT_POINT_BASE, splash_block_full_hit_points * targetNationData.GetFinalGeoEfficiency(userData.mapID)));

									// The block has now been restored to full hit points for the nation that has taken it over (or not yet full if a defense has been recaptured).
									splash_block_full_hit_points = land_map.DetermineBlockFullHitPoints(x, y, false, null);
									splash_block_cur_hit_points = land_map.DetermineBlockFullHitPoints(x, y, true, null);
									if (splash_block_cur_hit_points < splash_block_full_hit_points) {
										splashBlockData.SetHitPointsRestoredTime(cur_time + Constants.BATTLE_DURATION + (int)((float)(splash_block_full_hit_points - splash_block_cur_hit_points) / (nation_hit_points_rate / 60.0) + 0.5f)/*, land_map, x, y*/);
									}

									// Record when the block's transition will be completed, so that it will not count in adjacency checks until that time.
									splashBlockData.transition_complete_time = cur_time + Constants.BATTLE_DURATION;

									// Rapidly crumble the captured block's defense, if appropriate.
									splash_block_fast_crumble = AttemptFastCrumbleUponCapture(land_map, x, y, splashBlockData, targetNationData.ID, nationData, Constants.BATTLE_DURATION);

									// If this event took place on a raid map, record it in the raid's replay history.
									if (land_map.ID >= Raid.RAID_ID_BASE) {
										Raid.RecordEvent_Battle(land_map.ID, x, y, userData.nationID, splash_block_fast_crumble ? Constants.BATTLE_FLAG_FAST_CRUMBLE : 0, Constants.BATTLE_DURATION);
									}

									// Update the nations' users' reports.
									nationData.ModifyUserReportValueInt(UserData.ReportVal.report__attacks_squares_captured, 1);
									targetNationData.ModifyUserReportValueInt(UserData.ReportVal.report__land_lost, 1);
								}
								else if (splash_block_remaining_hit_points < splash_block_full_hit_points)
								{
									// The splash block has lost hit points. Record the time at which they will all have been restored.
									splashBlockData.SetHitPointsRestoredTime(cur_time + Constants.BATTLE_DURATION + (int)((float)(splash_block_full_hit_points - splash_block_remaining_hit_points) / (target_nation_hit_points_rate / 60.0) + 0.5f)/*, land_map, x, y*/);
								}

								// Record time when this attack on the block will be complete.
								splashBlockData.SetAttackCompleteTime(cur_time + Constants.BATTLE_DURATION);

								// Broadcast a battle process event for this battle splash to all clients in the view area.
								//Output.PrintToScreen("sx: " + x + ", sy: " + y + ", full_hit_points: " + full_hit_points + ", remaining_hit_points: " + remaining_hit_points + ", splash_block_full_hit_points: " + splash_block_full_hit_points + ", splash_block_remaining_hit_points: " + splash_block_remaining_hit_points);
								OutputEvents.BroadcastBattleProcessEvent(land_map, x, y, splashBlockData, splash_block_hit_points_at_attack_start, splash_block_remaining_hit_points, splash_block_old_full_hit_points, splash_block_cur_hit_points, splash_block_full_hit_points, GetNationHitPointsRate((splashBlockData.nationID == userData.nationID) ? nationData : targetNationData), 0, Constants.BATTLE_DURATION, -1, splash_block_fast_crumble ? Constants.BATTLE_FLAG_FAST_CRUMBLE : 0);
								//OutputEvents.BroadcastBlockProcessEvent(land_map, x, y, splashBlockData, splash_block_full_hit_points, splash_block_remaining_hit_points, GetNationHitPointsRate((splashBlockData.nationID == userData.nationID) ? nationData : targetNationData), Constants.BATTLE_DURATION, Constants.PROCESS_BATTLE);

								if (nationData.log_suspect_expire_time > cur_time)
								{
									// Log the details of this attack.
									Constants.WriteToLog("log_suspect.txt", "    SPLASH attack: '" + userData.name + "'(" + userData.ID + ") of '" + nationData.name + "'(ID:" + nationData.ID + ", Level:" + nationData.level + ") attacked '" + targetNationData.name + "'(ID:" + targetNationData.ID + ", Level:" + targetNationData.level + ", Geo:" + targetNationData.GetFinalGeoEfficiency(userData.mapID) + ") at block " + x + "," + y + " containing object " + land_map.GetBlockObjectID(x, y) + ". Full HP: " + splash_block_full_hit_points + ", HP pre-attack: " + splash_block_hit_points_at_attack_start + ", Remaining HP: " + splash_block_remaining_hit_points + ", manpower_cost: " + manpower_cost + ", XP after adding this splash: " + xp_to_add + ".\n");
								}
							}
						}
					}

					// Remove the total manpower cost from the attacking nation.
					footprint.manpower = Math.max(0, footprint.manpower - manpower_cost);

					if (userData.mapID == Constants.MAINLAND_MAP_ID)
					{
						// Update the nation's users' reports.
						nationData.ModifyUserReportValueFloat(UserData.ReportVal.report__manpower_spent, manpower_cost);
					}

					// Encode the battle display event
					Constants.EncodeString(_output_buffer, "display_battle"); // Event ID
					Constants.EncodeUnsignedNumber(_output_buffer, Constants.BATTLE_DURATION, 1); // Battle duration in seconds
					Constants.EncodeNumber(_output_buffer, _x, 3); // Map x coord
					Constants.EncodeNumber(_output_buffer, _y, 3); // Map y coord
					Constants.EncodeUnsignedNumber(_output_buffer, (int)(manpower_cost + 0.5f), 2); // Manpower cost
					Constants.EncodeUnsignedNumber(_output_buffer, (int)(nationData.GetFinalManpowerPerAttack()), 2); // Starting manpower
					Constants.EncodeUnsignedNumber(_output_buffer, remaining_manpower, 2); // Ending manpower
					Constants.EncodeUnsignedNumber(_output_buffer, full_hit_points, 2); // Max hit points
					Constants.EncodeUnsignedNumber(_output_buffer, block_hit_points_at_attack_start, 2); // Starting hit points
					Constants.EncodeUnsignedNumber(_output_buffer, remaining_hit_points, 2); // Ending hit points

					// TESTING
					//Output.PrintToScreen(nationData.name + "(" + nationData.ID + ") attacks " + targetNationData.name + "(" + targetNationData.ID + ") at " + _x + "," + _y + ": attacker_stat: " + combat_stats.attacker_stat + ", defender_stat: " + combat_stats.defender_stat + ", stat_ratio: " + stat_ratios[combat_stats.attacker_stat][combat_stats.defender_stat] + ", attacker_best_ratio: " + attacker_best_ratio + ", final manpower per attack: " + nationData.GetFinalManpowerPerAttack() + ", damage done (mpa x ratio): " + (nationData.manpower_per_attack * attacker_best_ratio) + ", block_hit_points_at_attack_start (without attack): " + block_hit_points_at_attack_start + ", block_hit_points_restored_during_attack: " + block_hit_points_restored_during_attack + ", remaining_hit_points: " + remaining_hit_points);

					// Log this attack if necessary
					if ((WOCServer.log_flags & Constants.LOG_ATTACK) != 0) Constants.WriteToLog("log_attack.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " " + nationData.name + "(" + nationData.ID + ") attacks " + targetNationData.name + "(" + targetNationData.ID + ") at " + _x + "," + _y + ": attacker_stat: " + combat_stats.attacker_stat + ", defender_stat: " + combat_stats.defender_stat + ", stat_ratio: " + stat_ratios[combat_stats.attacker_stat][combat_stats.defender_stat] + ", attacker_best_ratio: " + attacker_best_ratio + ", final manpower per attack: " + nationData.GetFinalManpowerPerAttack() + ", damage done (mpa x ratio): " + (nationData.manpower_per_attack * attacker_best_ratio) + ", block_hit_points_at_attack_start (without attack): " + block_hit_points_at_attack_start + ", block_hit_points_restored_during_attack: " + block_hit_points_restored_during_attack + ", remaining_hit_points: " + remaining_hit_points + "\n");

					// The block now has only its remaining hit points.
					int cur_hit_points = remaining_hit_points;

					// If the attack has been won...
					if (remaining_hit_points == 0)
					{
						// Have the attacking nation occupy the target block. Don't broadcast this change to viewing clients.
						World.SetBlockNationID(land_map, _x, _y, userData.nationID, true, false, _userID, Constants.BATTLE_DURATION);

						// If the target nation's final block on the mainland has been defeated, and the nation is online, displace the nation to a different location nearby.
						// Do this after the call to SetBlockNationID() above, so that the tournament system can respond to the attacker capturing the square from the defender.
						if ((land_map.ID == Constants.MAINLAND_MAP_ID) && (target_nation_footprint.area == 0) && (targetNationData.num_members_online > 0)) {
							World.DisplaceNation(land_map, targetNationData, _x, _y);
						}

						// The block has now been restored to full hit points for the nation that has taken it over (or not yet full if a defense has been recaptured).
						full_hit_points = land_map.DetermineBlockFullHitPoints(_x, _y, false, null);
						cur_hit_points = land_map.DetermineBlockFullHitPoints(_x, _y, true, null);
						if (cur_hit_points < full_hit_points) {
							blockData.SetHitPointsRestoredTime(cur_time + Constants.BATTLE_DURATION + (int)((float)(full_hit_points - cur_hit_points) / (nation_hit_points_rate / 60.0) + 0.5f)/*, land_map, _x, _y*/);
						}

						// Record when the block's transition will be completed, so that it will not count in adjacency checks until that time.
						blockData.transition_complete_time = blockData.lock_until_time;

						// Determine whether this captured block contains a resource or orb presently being captured by this nation for the first time.
						first_capture = ((blockData.BlockHasExtendedData()) && (land_map.GetBlockObjectID(_x, _y) >= ObjectData.RESOURCE_OBJECT_BASE_ID) && (nationTechData.object_capture_history.get(Constants.TokenizeCoordinates(_x, _y)) == Constants.GetTime()));

						// Rapidly crumble the captured block's defense, if appropriate.
						fast_crumble = AttemptFastCrumbleUponCapture(land_map, _x, _y, blockData, targetNationData.ID, nationData, Constants.BATTLE_DURATION);

						// If a fast crumble will take place, add it to the battle flags.
						if (fast_crumble) {
							battle_flags = battle_flags | Constants.BATTLE_FLAG_FAST_CRUMBLE;
						}

						// If this event took place on a raid map, record it in the raid's replay history.
						if (land_map.ID >= Raid.RAID_ID_BASE) {
							Raid.RecordEvent_Battle(land_map.ID, _x, _y, userData.nationID, battle_flags, Constants.BATTLE_DURATION);
						}

						// Attempt to make a random discovery in this newly conquered square.
						discovery_made = AttemptDiscovery(nationData, targetNationData, _userID, land_map, _x, _y, Constants.DISCOVERY_PROBABILITY_CONQUER, Constants.BATTLE_DURATION);
						//discovery_made = true; // TESTING

						// Update the quests system for this block that has been taken by flanking.
						if (flanked) {
							Quests.HandleCaptureByFlanking(nationData, Constants.BATTLE_DURATION);
						}

						// Update the nations' users' reports.
						nationData.ModifyUserReportValueInt(UserData.ReportVal.report__attacks_squares_captured, 1);
						targetNationData.ModifyUserReportValueInt(UserData.ReportVal.report__land_lost, 1);
					}
					else if (remaining_hit_points < full_hit_points)
					{
						// The target block has lost hit points. Record the time at which they will all have been restored.
						//Output.PrintToScreen("Time " + cur_time + ", remaining_hit_points: " + remaining_hit_points + ", full hp: " + full_hit_points + ", rate per min: " + (target_nation_hit_points_rate / 60.0) + ", battle dur: " + Constants.BATTLE_DURATION + ", hp restore time: " + (cur_time + Constants.BATTLE_DURATION + (int)((float)(full_hit_points - remaining_hit_points) / (GetNationHitPointsRate(targetNationData) / 60.0) + 0.5f)));
						blockData.SetHitPointsRestoredTime(cur_time + Constants.BATTLE_DURATION + (int)((float)(full_hit_points - remaining_hit_points) / (target_nation_hit_points_rate / 60.0) + 0.5f)/*, land_map, _x, _y*/);

						// Mark the nation's data to be updated
						DataManager.MarkForUpdate(nationData);
					}

					// Record time when this attack on the block will be complete.
					blockData.SetAttackCompleteTime(cur_time + Constants.BATTLE_DURATION);

					// If this is the first time this nation has captured the object in this block, add the first capture battle flag.
					if (first_capture) {
						battle_flags = battle_flags | Constants.BATTLE_FLAG_FIRST_CAPTURE;
					}

					// If a discovery has been made, add the discovery battle flag.
					if (discovery_made) {
						battle_flags = battle_flags | Constants.BATTLE_FLAG_DISCOVERY;
					}

					// Finish encoding the display_battle event. (Parts of it needed to be encoded above.)
					Constants.EncodeNumber(_output_buffer, (int)(nationData.GetFootprint(land_map.ID).geo_efficiency_base * 1000), 4); // Ending geo efficiency base
					Constants.EncodeUnsignedNumber(_output_buffer, combat_stats.attacker_stat, 1); // Attacker stat
					Constants.EncodeUnsignedNumber(_output_buffer, combat_stats.defender_stat, 1); // Defender stat
					Constants.EncodeNumber(_output_buffer, nationData.ID, 4); // Attacking nation ID
					Constants.EncodeNumber(_output_buffer, targetNationData.ID, 4); // Defending nation ID
					Constants.EncodeNumber(_output_buffer, effect_techID, 3); // Effect techID
					Constants.EncodeUnsignedNumber(_output_buffer, battle_flags, 2); // Battle flags

					// Broadcast a battle process event for this battle to all clients in the view area.
					OutputEvents.BroadcastBattleProcessEvent(land_map, _x, _y, blockData, block_hit_points_at_attack_start, remaining_hit_points, old_full_hit_points, cur_hit_points, full_hit_points, GetNationHitPointsRate((blockData.nationID == userData.nationID) ? nationData : targetNationData), 0, Constants.BATTLE_DURATION, _userID, battle_flags);
					//OutputEvents.BroadcastBlockProcessEvent(land_map, _x, _y, blockData, full_hit_points, remaining_hit_points, GetNationHitPointsRate((blockData.nationID == userData.nationID) ? nationData : targetNationData), Constants.BATTLE_DURATION, Constants.PROCESS_BATTLE);

					// If the attack has been won, and XP has been gained for it...
					if ((remaining_hit_points == 0) && (xp_to_add > 0))
					{
						if (Constants.random.nextInt(Constants.AD_BONUS_BLOCKS_PROBABILITY) == 0)
						{
							// Award the available ad bonus to the attacking user.
							AwardAvailableAdBonus(_output_buffer, userData, 1f, Constants.AD_BONUS_TYPE_BLOCKS, _x, _y, Constants.BATTLE_DURATION);
						}
					}

					if (xp_to_add > 0)
					{
						// Add determined amount of XP to the attacking nation.
						AddXP(nationData, xp_to_add, _userID, _x, _y, true, true, Constants.BATTLE_DURATION, Constants.XP_ATTACK);

						// Update the nation's users' reports.
						nationData.ModifyUserReportValueInt(UserData.ReportVal.report__attacks_XP, xp_to_add);
					}

					// Record that the attacking nation has been hostile to the target nation.
					targetNationData.RecordHostileNation(nationData.ID);
				}

				// If this took place on a raid map, update the raid system for this map click.
				if (land_map.ID >= Raid.RAID_ID_BASE) {
					Raid.OnMapClick(land_map, nationData);
				}
			}
		}
		catch (Exception e) {
      Output.PrintToScreen("Exception in MapClick()");
			//e.printStackTrace();
			Output.PrintException(e);
    }
	}

	public static void DetermineCombatStats(int _landmapID, float _crit_multiplier, NationData _attackerNationData, NationData _defenderNationData, CombatStats _result)
	{
		// Determine stat ratios for each stat combination, factoring in rock/paper/scissors relationship for tech/bio/psi, as well as the defender's geographic efficiency.
		stat_ratios[Constants.STAT_TECH][Constants.STAT_TECH] = _attackerNationData.GetFinalStatTech(_landmapID) * _crit_multiplier / _defenderNationData.GetFinalStatTech(_landmapID);
		stat_ratios[Constants.STAT_TECH][Constants.STAT_BIO] = _attackerNationData.GetFinalStatTech(_landmapID) * _crit_multiplier / (_defenderNationData.GetFinalStatBio(_landmapID) * Constants.STAT_COMBO_WEAK);
		stat_ratios[Constants.STAT_TECH][Constants.STAT_PSI] = _attackerNationData.GetFinalStatTech(_landmapID) * _crit_multiplier / (_defenderNationData.GetFinalStatPsi(_landmapID) * Constants.STAT_COMBO_STRONG);
		stat_ratios[Constants.STAT_BIO][Constants.STAT_TECH] = _attackerNationData.GetFinalStatBio(_landmapID) * _crit_multiplier / (_defenderNationData.GetFinalStatTech(_landmapID) * Constants.STAT_COMBO_STRONG);
		stat_ratios[Constants.STAT_BIO][Constants.STAT_BIO] = _attackerNationData.GetFinalStatBio(_landmapID) * _crit_multiplier / _defenderNationData.GetFinalStatBio(_landmapID);
		stat_ratios[Constants.STAT_BIO][Constants.STAT_PSI] = _attackerNationData.GetFinalStatBio(_landmapID) * _crit_multiplier / (_defenderNationData.GetFinalStatPsi(_landmapID) * Constants.STAT_COMBO_WEAK);
		stat_ratios[Constants.STAT_PSI][Constants.STAT_TECH] = _attackerNationData.GetFinalStatPsi(_landmapID) * _crit_multiplier / (_defenderNationData.GetFinalStatTech(_landmapID) * Constants.STAT_COMBO_WEAK);
		stat_ratios[Constants.STAT_PSI][Constants.STAT_BIO] = _attackerNationData.GetFinalStatPsi(_landmapID) * _crit_multiplier / (_defenderNationData.GetFinalStatBio(_landmapID) * Constants.STAT_COMBO_STRONG);
		stat_ratios[Constants.STAT_PSI][Constants.STAT_PSI] = _attackerNationData.GetFinalStatPsi(_landmapID) * _crit_multiplier / _defenderNationData.GetFinalStatPsi(_landmapID);

		int attack_stat = Constants.STAT_TECH;
		int defense_stat = Constants.STAT_TECH;
		float cur_ratio, cur_attack_stat__best_defense_ratio, attacker_best_ratio;
		int cur_attack_stat__best_defense_stat, defender_best_stat, attacker_best_stat;

		// Initialize best stats and ratio
		attacker_best_stat = Constants.STAT_TECH;
		defender_best_stat = Constants.STAT_TECH;
		attacker_best_ratio = 0.0f;

		// Iterate through each possible stat the attacker could use for the attack, to find the one most advantageous to the attacker.
		for (int cur_attack_stat = Constants.STAT_TECH; cur_attack_stat < Constants.NUM_STATS; cur_attack_stat++)
		{
			// For the current attacker stat, initialize defender stat and ratio
			cur_attack_stat__best_defense_stat = Constants.STAT_TECH;
			cur_attack_stat__best_defense_ratio = 1000000.0f;

			// For the current attacker stat, iterate through each possible stat the defender could use for defense, to find the best vs this attack stat.
			for (int cur_defense_stat = Constants.STAT_TECH; cur_defense_stat < Constants.NUM_STATS; cur_defense_stat++)
			{
				cur_ratio = stat_ratios[cur_attack_stat][cur_defense_stat];
				if (cur_ratio < cur_attack_stat__best_defense_ratio)
				{
					cur_attack_stat__best_defense_stat = cur_defense_stat;
					cur_attack_stat__best_defense_ratio = cur_ratio;
				}
			}

			if (cur_attack_stat__best_defense_ratio > attacker_best_ratio)
			{
				attacker_best_stat = cur_attack_stat;
				attacker_best_ratio = cur_attack_stat__best_defense_ratio;
				defender_best_stat = cur_attack_stat__best_defense_stat;
			}
		}

		// Record results
		_result.attacker_stat = attacker_best_stat;
		_result.defender_stat = defender_best_stat;
		_result.attacker_best_ratio = attacker_best_ratio;
	}

	public static boolean AttemptFastCrumbleUponCapture(LandMap _land_map, int _x, int _y, BlockData _blockData, int _nationCapturedFromID, NationData _nationCapturedBy, int _delay)
	{
		// If the block has no extended data, there is no build to crumble.
		if (_blockData.BlockHasExtendedData() == false) {
			return false;
		}

		BlockExtData block_ext_data = _land_map.GetBlockExtendedData(_x, _y, false);

		// If the block has no extended data, return false.
		if (block_ext_data == null) {
			return false;
		}

		// If the block contains a build object that is owned by the nation it has just been captured from...
		if ((block_ext_data.objectID != -1) && (block_ext_data.objectID < ObjectData.RESOURCE_OBJECT_BASE_ID) && (block_ext_data.owner_nationID == _nationCapturedFromID))
		{
			// Get the build data for the object in this block.
			BuildData build_data = BuildData.GetBuildData(block_ext_data.objectID);

			// If the build is not a shard (shards shouldn't crumble)...
			if (build_data.type != BuildData.TYPE_SHARD)
			{
				// Determine randomly whether this object should crumble upon being captured.
				if (Constants.random.nextFloat() < Constants.CRUMBLE_UPON_CAPTURE_PROBABILITY)
				{
					// This object should crumble (almost) immediately.
					block_ext_data.crumble_time = Constants.GetTime() + _delay + Constants.TIME_UNTIL_FAST_CRUMBLE;

					return true;
				}
			}
		}

		return false;
	}

	public static boolean AttemptDiscovery(NationData _nationData, NationData _targetNationData, int _userID, LandMap _land_map, int _x, int _y, float _discovery_prob, int _delay)
	{
		int cur_day = Constants.GetAbsoluteDay();

		// If the given nation has already made a discovery during the current day, it cannot make another.
		if (_nationData.prev_discovery_day == cur_day) {
			return false;
		}

		// If the given block contains a landscape object (resource or orb), do not allow a discovery to take place here.
		if (_land_map.GetBlockObjectID(_x, _y) >= ObjectData.RESOURCE_OBJECT_BASE_ID) {
			return false;
		}

		// If generated random number does not fall within the given _discovery_prob, do nothing.
    if (Constants.random.nextFloat() > _discovery_prob) {
			return false;
		}

		// Record that the nation has made a discovery on the current day.
		_nationData.prev_discovery_day = cur_day;

		int energy_transferred = 0, manpower_transferred = 0, advanceID = -1, advance_duration = 0, xp = 0;

		// Get the nation's footprint.
		Footprint footprint = _nationData.GetFootprint(_land_map.ID);

		// Determine the maximum amount of energy or manpower that can be transferred.
		int max_energy_to_transfer = (int)(_nationData.GetFinalEnergyMax() - _nationData.energy);
		int max_manpower_to_transfer = (int)(_nationData.GetFinalManpowerMax(_land_map.ID) - footprint.manpower);

		if ((_land_map.ID == Constants.MAINLAND_MAP_ID) && (_targetNationData != null) && ((max_energy_to_transfer >= Constants.MIN_SUPPLY_LINE_TRANSFER_AMOUNT) || (max_manpower_to_transfer >= Constants.MIN_SUPPLY_LINE_TRANSFER_AMOUNT)) && (Constants.random.nextFloat() <= Constants.SUPPLY_LINE_PROBABILITY))
		{
			// Supply line

			// Transfer out the appropriate amount of energy or manpower from the target nation, to the given nation.
			if (max_energy_to_transfer > max_manpower_to_transfer)
			{
				energy_transferred = (int)Math.min(max_energy_to_transfer, (_targetNationData.energy * Constants.SUPPLY_LINE_AMOUNT_STOLEN + 0.5f));
				_targetNationData.energy -= energy_transferred;
				_nationData.energy += energy_transferred;

				// Broadcast message to the target nation.
				OutputEvents.BroadcastMessageEvent(_targetNationData.ID, ClientString.Get("svr_supply_line_energy_stolen", "nation_name", _nationData.name, "amount", String.valueOf(energy_transferred))); // _nationData.name + " has captured a supply line and stolen " + energy_transferred + " energy!"

				// Update quests system for this discovery
				Quests.HandleDiscoverEnergySupplyLine(_nationData, energy_transferred, _delay);

				// Update the nation's users' reports.
				_targetNationData.ModifyUserReportValueFloat(UserData.ReportVal.report__energy_lost_to_raids, energy_transferred);
			}
			else
			{
				// Get the target nation's footprint.
				Footprint target_footprint = _targetNationData.GetFootprint(_land_map.ID);

				manpower_transferred = (int)Math.min(max_manpower_to_transfer, (target_footprint.manpower * Constants.SUPPLY_LINE_AMOUNT_STOLEN + 0.5f));
				target_footprint.manpower -= manpower_transferred;
				footprint.manpower += manpower_transferred;

				// Broadcast message to the target nation.
				OutputEvents.BroadcastMessageEvent(_targetNationData.ID, ClientString.Get("svr_supply_line_manpower_stolen", "nation_name", _nationData.name, "amount", String.valueOf(manpower_transferred))); // _nationData.name + " has captured a supply line and stolen " + manpower_transferred + " manpower!"

				// Update quests system for this discovery
				Quests.HandleDiscoverManpowerSupplyLine(_nationData, manpower_transferred, _delay);

				// Update the nation's users' reports.
				_targetNationData.ModifyUserReportValueFloat(UserData.ReportVal.report__manpower_lost_to_raids, manpower_transferred);
			}

			// Broadcast loss of manpower or energy to the target nation.
			OutputEvents.BroadcastUpdateBarsEvent(_targetNationData.ID, -energy_transferred, 0, 0, -manpower_transferred, 0, _delay);

			// Record the XP bonus for this discovery.
			xp = Constants.DISCOVER_SUPPLY_LINE_XP;

			// Mark the target nation's data to be updated
			DataManager.MarkForUpdate(_targetNationData);
		}
		else
		{
			// Random advance

			// Determine ID of the advance to be discovered, by choosing randomly from the list of random_advances.
			advanceID = TechData.random_advances.get(Constants.random.nextInt(TechData.random_advances.size()));

			// Record the XP bonus for this discovery.
			xp = Constants.DISCOVER_ADVANCE_XP;

			// Determine how long until the discovered advance expires.
			TechData techData = TechData.GetTechData(advanceID);
			advance_duration = techData.duration_time;

			// Add the discovered advance to the given nation, but do not broadcast its addition.
			Technology.AddTechnology(_nationData.ID, advanceID, 0, false, false, _delay);

			// Update quests system for this discovery
			Quests.HandleDiscovery(_nationData, advanceID, _delay);
		}

		// Add the determined XP to the given nation.
		AddXP(_nationData, xp, _userID, -1, -1, true, true, 0, Constants.XP_DISCOVERY);

		// Log suspect
		if (_nationData.log_suspect_expire_time > Constants.GetTime())
		{
			// Log the details of this XP gain.
			Constants.WriteToLog("log_suspect.txt", Constants.GetTimestampString() + ": '" + _nationData.name + "'(ID:" + _nationData.ID + ", Level:" + _nationData.level + ") made discovery at block " + _x + "," + _y + ", earning " + xp + " XP.\n");
		}

		// Broadcast the discovery event to each member of the nation who is logged in.
		OutputEvents.BroadcastDiscoveryEvent(_nationData.ID, _delay, manpower_transferred, energy_transferred, _targetNationData, advanceID, advance_duration, xp);

		// Mark the given nation's data to be updated
		DataManager.MarkForUpdate(_nationData);

		return true;
	}

	public static boolean ReserveUserProcess(UserData _userData, NationData _nationData, int _duration)
	{
		int cur_time = Constants.GetTime();

		for (int i = 0; i < _nationData.max_simultaneous_processes; i++)
		{
			if (_userData.processEndTimes[i] <= cur_time)
			{
				_userData.processEndTimes[i] = cur_time + _duration;
				return true;
			}
		}

		//Output.PrintToScreen("ALREADY USING " + _nationData.max_simultaneous_processes + " PROCESSES");

		return false;
	}

	public static float GetNationHitPointsRate(int _nationID)
	{
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationID, false);
		return GetNationHitPointsRate(nationData);
	}

	public static float GetNationHitPointsRate(NationData _nationData)
	{
		return (_nationData == null) ? Constants.VACANT_BLOCK_HIT_POINTS_RATE : _nationData.GetFinalHitPointsRate();
	}

	public static int DetermineXPToAdd(int _landmapID, NationData _nationData, NationData _targetNationData, float _attacker_stat_ratio, int _base_xp_amount)
	{
		float xp_to_add = (float)_base_xp_amount * _nationData.GetFinalXPMultiplier(_landmapID);

		// If the attacker is much more powerful than the defender, reduce the XP amount.
		if (_attacker_stat_ratio > 2f) {
			xp_to_add = xp_to_add * (2f / _attacker_stat_ratio);
		}

		int half_xp_threshold = Math.max(5, _nationData.level / 6);
		int no_xp_threshold = Math.max(10, _nationData.level / 3);

		// If the target nation is >= half_xp_threshold levels lower than the attacker, then the XP is halved.
		// If the target nation >= no_xp_threshold levels lower, then no XP is awarded.
		// Subtract unspent advance points for purposes of determining level, so nations can't create weak adversaries to level off of, by not using advance points.
		if (((_nationData.level - _nationData.advance_points) - (_targetNationData.level - _targetNationData.advance_points)) >= no_xp_threshold) {
			xp_to_add = 0;
		} else if (((_nationData.level - _nationData.advance_points) - (_targetNationData.level - _targetNationData.advance_points)) >= half_xp_threshold) {
			xp_to_add = xp_to_add / 2f;
		}

		//Output.PrintToScreen("DetermineXPToAdd() for _attacker_stat_ratio: " + _attacker_stat_ratio + ", _base_xp_amount: " + _base_xp_amount + " returning " + (int)(xp_to_add + 0.5f));

		return (int)(xp_to_add + 0.5f);
	}

	public static void ModifyEnergyBurnRate(NationData _nationData, int _landmapID, float _energy_burn_rate_delta)
	{
		// Get the nation's footprint for this landmap.
		Footprint footprint = _nationData.GetFootprint(_landmapID);

		// Apply the delta to the nation's energy_burn_rate, clipping at 0.
		footprint.energy_burn_rate = Math.max(0f, footprint.energy_burn_rate + _energy_burn_rate_delta);

		// Mark the nation's data to be updated
		DataManager.MarkForUpdate(_nationData);
	}

	public static void TriggerObjectsInRange(LandMap _land_map, int _x, int _y, int _triggerNationID, int _targetNationID, int _trigger_on)
	{
		for (int y = _y - BuildData.MAX_ATTACK_RADIUS; y <= _y + BuildData.MAX_ATTACK_RADIUS; y++)
		{
			for (int x = _x - BuildData.MAX_ATTACK_RADIUS; x <= _x + BuildData.MAX_ATTACK_RADIUS; x++)
			{
				//Output.PrintToScreen("For " + x + "," + y + ", CheckForBuildObjectWithTriggerType() returns " + _land_map.CheckForBuildObjectWithTriggerType(x, y, _trigger_on, _triggerNationID, _targetNationID, _x, _y));
				if (_land_map.CheckForBuildObjectWithTriggerType(x, y, _trigger_on, _triggerNationID, _targetNationID, _x, _y))
				{
					//Output.PrintToScreen("CheckForBuildObjectWithTriggerType() returned true for " + x + "," + y);
					TriggerObject(_land_map, x, y, _triggerNationID, _x, _y);
				}
			}
		}
	}

	public static boolean TriggerObject(LandMap _land_map, int _x, int _y, int _triggerNationID, int _trigger_x, int _trigger_y)
	{
		int x, y, battle_flags;
		int block_full_hp, block_start_hp, damage, block_end_hp;
		boolean crit;
		boolean flanked = false;
		float radius_squared, x_dist, y_dist;
		BlockData cur_block_data;
		BlockExtData cur_block_ext_data;

		try {
			// Get the block's data.
			BlockData block_data = _land_map.GetBlockData(_x, _y);
			BlockExtData block_ext_data = _land_map.GetBlockExtendedData(_x, _y, false);

			if ((block_data == null) || (block_ext_data == null) || (block_ext_data.objectID == -1)) {
				return false;
			}

			// Get the build data for the object in this block.
			BuildData build_data = BuildData.GetBuildData(block_ext_data.objectID);

			if (build_data == null) {
				return false;
			}

			// Get current time.
			int cur_time = Constants.GetTime();

			// Get the block's nation's data
			NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, block_data.nationID, false);

			// Get the trigger nation's data
			NationData triggerNationData = (NationData)DataManager.GetData(Constants.DT_NATION, _triggerNationID, false);

			if ((nationData == null) || (triggerNationData == null)) {
				Output.PrintToScreen("ERROR: TriggerObject(): nationData or triggerNationData is null.");
				return false;
			}

			// If the build object requires energy and is presently inert, it cannot be triggered. Broadcast a trigger inert event to show that an inert build attempted to trigger.
			if ((build_data.energy_burn_rate > 0) && _land_map.BlockIsInert(nationData, _x, _y))
			{
				// Broadcast an event to all clients viewing this area, stating that an inert build attempted to trigger.
				OutputEvents.BroadcastTriggerInertEvent(_land_map, _x, _y);

				// If this event is taking place on a raid map...
				if (_land_map.ID >= Raid.RAID_ID_BASE)
				{
					// Record the trigger inert event in the raid's replay.
					Raid.RecordEvent_TriggerInert(_land_map.ID, _x, _y);
				}

				return false;
			}

			// Get the trigger nation's hit points rate.
			float triggerNationHitPointsRate = GetNationHitPointsRate(triggerNationData);

			// Create a list to hold info on target blocks.
			ArrayList<TargetRecord> targets = new ArrayList<TargetRecord>();

			if (build_data.flank_nullifies && (IsBlockFlanked(_land_map, _x, _y, block_data, triggerNationData)))
			{
				flanked = true;
			}
			else
			{
				if (build_data.type == BuildData.TYPE_DIRECTED_MULTIPLE)
				{
					radius_squared = (build_data.attack_radius + 0.5f) * (build_data.attack_radius + 0.5f);
					ArrayList<TargetCandidateRecord> target_candidates = new ArrayList<TargetCandidateRecord>();

					// Iterate all blocks within this block's attack radius, to generate a list of candidate targets.
					for (y = _y - build_data.attack_radius; y <= _y + build_data.attack_radius; y++)
					{
						for (x = _x - build_data.attack_radius; x <= _x + build_data.attack_radius; x++)
						{
							// Skip this block if it is outside of the tower's attack radius, as a circle.
							y_dist = y - _y;
							x_dist = x - _x;
							if (((y_dist * y_dist) + (x_dist * x_dist)) > radius_squared) {
								continue;
							}

							// Get the data for the current block.
							cur_block_data = _land_map.GetBlockData(x, y);

							if (cur_block_data == null) {
								continue;
							}

							// If this block is not owned by the nation that triggered this tower, skip it.
							if (cur_block_data.nationID != _triggerNationID) {
								continue;
							}

							// If the block is currently locked, skip it.
							if (cur_block_data.lock_until_time > cur_time) {
								continue;
							}

							// If this block has a lasting wipe that prevents attacks from the tower's nation, skip it.
							if (cur_block_data.BlockHasExtendedData() && _land_map.CheckForLastingWipePreventingAttack(x, y, block_data.nationID)) {
								continue;
							}

							// If this block is outside of the tower's nation's max extent, skip it.
							if (BlockIsWithinNationMaxExtent(nationData, _land_map.ID, x, y) == false) {
								continue;
							}

							// Add this block as a target candidate. Its score is the squared distance from the trigger block.
							target_candidates.add(new TargetCandidateRecord(x, y, cur_block_data, ((x - _trigger_x) * (x - _trigger_x)) + ((y - _trigger_y) * (y - _trigger_y))));
						}
					}

					// Sort the list of candidate targets, from closest to furthest from the triggering square.
					Collections.sort(target_candidates);

					int target_candidate_index = 0;
					TargetCandidateRecord cur_candidate;
					boolean repeating_target = false;
					for (int target_index = 0; target_index < build_data.num_attacks; target_index++)
					{
						// Exit loop if there are no more target candidates.
						if (target_candidates.size() <= target_candidate_index) {
							break;
						}

						// Get the current target candidate.
						cur_candidate = target_candidates.get(target_candidate_index);

						// Determine the amount of damage to be done to this target block.
						damage = (build_data.attack_min_hp == build_data.attack_max_hp) ? build_data.attack_max_hp : (build_data.attack_min_hp + Constants.random.nextInt(build_data.attack_max_hp - build_data.attack_min_hp + 1));
						crit = nationData.RollForCrit();
						if (crit) damage *= 2;

						// Determine this target block's full HP value.
						block_full_hp = _land_map.DetermineBlockFullHitPoints(cur_candidate.x, cur_candidate.y, false, null);

						// Determine the starting hp of this block. If this same block is being attacked repeatedly, use the previous attack's ending hp value,
						// so as to get an exact value and not be thrown off by the tower action duration that is added to the restore time.
						if (repeating_target) {
							block_start_hp = targets.get(target_index - 1).end_hit_points;
						} else {
							block_start_hp = cur_candidate.block_data.GetBlockCurrentHitPoints(block_full_hp, triggerNationHitPointsRate, cur_time);
						}

						// Handle the attack of this tower on the current target block, and add the target block's info to the targets array.
						block_end_hp = TowerAttacksBlock(_land_map, cur_candidate.x, cur_candidate.y, block_full_hp, block_start_hp, nationData, _triggerNationID, damage, crit, true, true, -1, -1, 0, targets);

						// Only advance to the next target candidate if this target block has been defeated. Otherwise the next attack (if there is one) will repeat the same target.
						if (block_end_hp == 0) {
							target_candidate_index++;
							repeating_target = false;
						} else {
							repeating_target = true;
						}
					}

					// Clear the list of target candidates.
					target_candidates.clear();
				}
				else if (build_data.type == BuildData.TYPE_SPLASH)
				{
					radius_squared = (build_data.attack_radius + 0.5f) * (build_data.attack_radius + 0.5f);
					ArrayList<TargetCandidateRecord> target_candidates = new ArrayList<TargetCandidateRecord>();

					// Iterate all blocks within this block's attack radius, to generate a list of candidate targets.
					for (y = _y - build_data.attack_radius; y <= _y + build_data.attack_radius; y++)
					{
						for (x = _x - build_data.attack_radius; x <= _x + build_data.attack_radius; x++)
						{
							// Skip this block if it is outside of the tower's attack radius, as a circle.
							y_dist = y - _y;
							x_dist = x - _x;
							if (((y_dist * y_dist) + (x_dist * x_dist)) > radius_squared) {
								continue;
							}

							// Get the data for the current block.
							cur_block_data = _land_map.GetBlockData(x, y);

							if (cur_block_data == null) {
								continue;
							}

							// If this block is not owned by the nation that triggered this tower or a recently hostile nation, skip it.
							if ((cur_block_data.nationID != _triggerNationID) && (nationData.IsHostileNation(cur_block_data.nationID) == false)) {
								continue;
							}

							// If the block is currently locked, skip it.
							if (cur_block_data.lock_until_time > cur_time) {
								continue;
							}

							// If this block has a lasting wipe that prevents attacks from the tower's nation, skip it.
							if (cur_block_data.BlockHasExtendedData() && _land_map.CheckForLastingWipePreventingAttack(x, y, block_data.nationID)) {
								continue;
							}

							// If this block is outside of the tower's nation's max extent, skip it.
							if (BlockIsWithinNationMaxExtent(nationData, _land_map.ID, x, y) == false) {
								continue;
							}

							// Add this block as a target candidate. Its score is the squared distance from the trigger block.
							target_candidates.add(new TargetCandidateRecord(x, y, cur_block_data, ((x - _trigger_x) * (x - _trigger_x)) + ((y - _trigger_y) * (y - _trigger_y))));
						}
					}

					if (target_candidates.size() > 0)
					{
						// Sort the list of candidate targets, from closest to furthest from the triggering square.
						Collections.sort(target_candidates);

						// Get the first target candidate.
						TargetCandidateRecord cur_candidate = target_candidates.get(0);

						// Determine the amount of damage to be done to this target block and the surrounding blocks in the effect radius.
						damage = (build_data.attack_min_hp == build_data.attack_max_hp) ? build_data.attack_max_hp : (build_data.attack_min_hp + Constants.random.nextInt(build_data.attack_max_hp - build_data.attack_min_hp + 1));
						crit = nationData.RollForCrit();
						if (crit) damage *= 2;

						radius_squared = (build_data.effect_radius + 0.5f) * (build_data.effect_radius + 0.5f);

						// Iterate all blocks within the target block's effect radius, to generate the list of targets.
						for (y = cur_candidate.y - build_data.effect_radius; y <= cur_candidate.y + build_data.effect_radius; y++)
						{
							for (x = cur_candidate.x - build_data.effect_radius; x <= cur_candidate.x + build_data.effect_radius; x++)
							{
								// Skip this block if it is outside of the tower's effect radius, as a circle.
								y_dist = y - cur_candidate.y;
								x_dist = x - cur_candidate.x;
								if (((y_dist * y_dist) + (x_dist * x_dist)) > radius_squared) {
									continue;
								}

								// Get the data for the current block.
								cur_block_data = _land_map.GetBlockData(x, y);

								if (cur_block_data == null) {
									continue;
								}

								// If this block is owned by the tower's nation or by an ally of the tower's nation, skip it.
								if ((cur_block_data.nationID == -1) || (cur_block_data.nationID == block_data.nationID) || (nationData.alliances_active.contains(cur_block_data.nationID))) {
									continue;
								}

								// If the block is currently locked, skip it.
								if (cur_block_data.lock_until_time > cur_time) {
									continue;
								}

								// If this block has a lasting wipe that prevents attacks from the tower's nation, skip it.
								if (cur_block_data.BlockHasExtendedData() && _land_map.CheckForLastingWipePreventingAttack(x, y, block_data.nationID)) {
									continue;
								}

								// Get the block's nation's hit points rate.
								NationData blockNationData = (NationData)DataManager.GetData(Constants.DT_NATION, cur_block_data.nationID, false);
								float blockNationHitPointsRate = GetNationHitPointsRate(blockNationData);

								// Determine this target block's full HP value.
								block_full_hp = _land_map.DetermineBlockFullHitPoints(x, y, false, null);

								// Determine the starting hp of this block.
								block_start_hp = cur_block_data.GetBlockCurrentHitPoints(block_full_hp, blockNationHitPointsRate, cur_time);

								// Handle the attack of this tower on the current target block, and add the target block's info to the targets array.
								block_end_hp = TowerAttacksBlock(_land_map, x, y, block_full_hp, block_start_hp, nationData, cur_block_data.nationID, damage, crit, true, (x == cur_candidate.x) && (y == cur_candidate.y), -1, -1, 0, targets);
							}
						}
					}

					// Clear the list of target candidates.
					target_candidates.clear();
				}
				else if (build_data.type == BuildData.TYPE_AREA_EFFECT)
				{
					radius_squared = (build_data.attack_radius + 0.5f) * (build_data.attack_radius + 0.5f);

					// Iterate all blocks within this block's attack radius, to generate the list of targets.
					for (y = _y - build_data.attack_radius; y <= _y + build_data.attack_radius; y++)
					{
						for (x = _x - build_data.attack_radius; x <= _x + build_data.attack_radius; x++)
						{
							// Skip this block if it is outside of the tower's attack radius, as a circle.
							y_dist = y - _y;
							x_dist = x - _x;
							if (((y_dist * y_dist) + (x_dist * x_dist)) > radius_squared) {
								continue;
							}

							// Get the data for the current block.
							cur_block_data = _land_map.GetBlockData(x, y);

							if (cur_block_data == null) {
								continue;
							}

							// If this block is not owned by the nation that triggered this tower or a recently hostile nation, skip it.
							if ((cur_block_data.nationID != _triggerNationID) && (nationData.IsHostileNation(cur_block_data.nationID) == false)) {
								continue;
							}

							// If the block is currently locked, skip it.
							if (cur_block_data.lock_until_time > cur_time) {
								continue;
							}

							// If this block has a lasting wipe that prevents attacks from the tower's nation, skip it.
							if (cur_block_data.BlockHasExtendedData() && _land_map.CheckForLastingWipePreventingAttack(x, y, block_data.nationID)) {
								continue;
							}

							// If this block is outside of the tower's nation's max extent, skip it.
							if (BlockIsWithinNationMaxExtent(nationData, _land_map.ID, x, y) == false) {
								continue;
							}

							// Determine the amount of damage to be done to this target block.
							damage = (build_data.attack_min_hp == build_data.attack_max_hp) ? build_data.attack_max_hp : (build_data.attack_min_hp + Constants.random.nextInt(build_data.attack_max_hp - build_data.attack_min_hp + 1));
							crit = nationData.RollForCrit();
							if (crit) damage *= 2;

							// Determine this target block's full HP value.
							block_full_hp = _land_map.DetermineBlockFullHitPoints(x, y, false, null);

							// Determine the starting hp of this block.
							block_start_hp = cur_block_data.GetBlockCurrentHitPoints(block_full_hp, GetNationHitPointsRate(cur_block_data.nationID), cur_time);

							// Handle the attack of this tower on the current target block, and add the target block's info to the targets array.
							block_end_hp = TowerAttacksBlock(_land_map, x, y, block_full_hp, block_start_hp, nationData, cur_block_data.nationID, damage, crit, true, false, -1, -1, 0, targets);
						}
					}
				}
				else if (build_data.type == BuildData.TYPE_COUNTER_ATTACK)
				{
					int target_x = -1, target_y = -1;
					BlockData target_block_data = null;

					for (y = _trigger_y - 1; y <= _trigger_y + 1; y++)
					{
						for (x = _trigger_x - 1; x <= _trigger_x + 1; x++)
						{
							// Get the data for the current block.
							cur_block_data = _land_map.GetBlockData(x, y);

							if (cur_block_data == null) {
								continue;
							}

							// If this block is not owned by the nation that triggered this tower, skip it.
							if (cur_block_data.nationID != _triggerNationID) {
								continue;
							}

							// If the block is currently locked, skip it.
							if (cur_block_data.lock_until_time > cur_time) {
								continue;
							}

							// If this block has a lasting wipe that prevents attacks from the tower's nation, skip it.
							if (cur_block_data.BlockHasExtendedData() && _land_map.CheckForLastingWipePreventingAttack(x, y, block_data.nationID)) {
								continue;
							}

							// If this block is outside of the tower's nation's max extent, skip it.
							if (BlockIsWithinNationMaxExtent(nationData, _land_map.ID, x, y) == false) {
								continue;
							}

							target_x = x;
							target_y = y;
							target_block_data = cur_block_data;
						}
					}

					if (target_block_data != null)
					{
						// Determine the amount of damage to be done to the target block.
						damage = (build_data.attack_min_hp == build_data.attack_max_hp) ? build_data.attack_max_hp : (build_data.attack_min_hp + Constants.random.nextInt(build_data.attack_max_hp - build_data.attack_min_hp + 1));
						crit = nationData.RollForCrit();
						if (crit) damage *= 2;

						// Determine this target block's full HP value.
						block_full_hp = _land_map.DetermineBlockFullHitPoints(target_x, target_y, false, null);

						// Determine the starting hp of this block.
						block_start_hp = target_block_data.GetBlockCurrentHitPoints(block_full_hp, triggerNationHitPointsRate, cur_time);

						// Handle the attack of this tower on the current target block, and add the target block's info to the targets array.
						block_end_hp = TowerAttacksBlock(_land_map, target_x, target_y, block_full_hp, block_start_hp, nationData, _triggerNationID, damage, crit, true, false, -1, -1, 0, targets);
					}
				}
				else if ((build_data.type == BuildData.TYPE_WIPE) || (build_data.type == BuildData.TYPE_GENERAL_LASTING_WIPE) || (build_data.type == BuildData.TYPE_SPECIFIC_LASTING_WIPE))
				{
					radius_squared = (build_data.effect_radius + 0.5f) * (build_data.effect_radius + 0.5f);

					// Iterate all blocks within this block's effect radius, to generate the list of targets.
					for (y = _y - build_data.effect_radius; y <= _y + build_data.effect_radius; y++)
					{
						for (x = _x - build_data.effect_radius; x <= _x + build_data.effect_radius; x++)
						{
							// Skip this block if it is outside of the tower's effect radius, as a circle.
							y_dist = y - _y;
							x_dist = x - _x;
							if (((y_dist * y_dist) + (x_dist * x_dist)) > radius_squared) {
								continue;
							}

							// Get the data for the current block.
							cur_block_data = _land_map.GetBlockData(x, y);

							if (cur_block_data == null) {
								continue;
							}

							// If this block is not owned by any nation, skip it.
							if (cur_block_data.nationID == -1) {
								continue;
							}

							if (build_data.type == BuildData.TYPE_SPECIFIC_LASTING_WIPE)
							{
								// If this block is not owned by the nation that triggered this tower or another recently hostile nation, skip it.
								if ((cur_block_data.nationID != _triggerNationID) && (nationData.IsHostileNation(cur_block_data.nationID) == false)) {
									continue;
								}
							}

							// If the block is currently locked, skip it.
							if (cur_block_data.lock_until_time > cur_time) {
								continue;
							}

							// If this block is outside of the tower's nation's max extent, skip it.
							if (BlockIsWithinNationMaxExtent(nationData, _land_map.ID, x, y) == false) {
								continue;
							}

							// Determine this target block's full HP value.
							block_full_hp = _land_map.DetermineBlockFullHitPoints(x, y, false, null);

							// Determine the starting hp of this block.
							block_start_hp = cur_block_data.GetBlockCurrentHitPoints(block_full_hp, GetNationHitPointsRate(cur_block_data.nationID), cur_time);

							// Determine the amount of damage to be done to the block.
							crit = false;
							if ((cur_block_data.nationID == _triggerNationID) || nationData.IsHostileNation(cur_block_data.nationID)) {
								damage = block_start_hp; // Because this is a wipe, all of the block's hp will be removed from blocks belonging to the triggering nation or another recently hostile nation.
							} else {
								damage = (int)Math.ceil((float)block_start_hp * Constants.WIPE_COLLATERAL_DAMAGE_AMOUNT); // Blocks belonging to other nations (including the defender) will lose only a fraction of their XP.
							}

							// Determine lasting wipe information, if applicable.
							int wipe_nationID = -1;
							int wipe_flags = 0;
							int wipe_end_time = -1;
							if (build_data.type == BuildData.TYPE_GENERAL_LASTING_WIPE)
							{
								// Only place temporary block in those squares that were wiped (due to belonging to the trggering naton or a hostile nation).
								if ((cur_block_data.nationID == _triggerNationID) || nationData.IsHostileNation(cur_block_data.nationID))
								{
									wipe_nationID = block_data.nationID;
									wipe_end_time = cur_time + build_data.wipe_duration;
									wipe_flags = BuildData.WIPE_FLAG_GENERAL;
									if (build_data.name.contains("Toxic Chemical Dump")) wipe_flags = wipe_flags | BuildData.WIPE_FLAG_CHEMICAL;
									if (build_data.name.contains("Supervirus Contagion")) wipe_flags = wipe_flags | BuildData.WIPE_FLAG_SUPERVIRUS;
								}
							}
							else if (build_data.type == BuildData.TYPE_SPECIFIC_LASTING_WIPE)
							{
								wipe_nationID = _triggerNationID;
								wipe_end_time = cur_time + build_data.wipe_duration;
								wipe_flags = BuildData.WIPE_FLAG_SPECIFIC;
								if (build_data.name.contains("Hypnotic Inducer")) wipe_flags = wipe_flags | BuildData.WIPE_FLAG_HYPONOTIC;
								if (build_data.name.contains("Temple of Zoth-Ommog")) wipe_flags = wipe_flags | BuildData.WIPE_FLAG_TEMPLE;
							}

							// Handle the attack of this tower on the current target block, and add the target block's info to the targets array.
							block_end_hp = TowerAttacksBlock(_land_map, x, y, block_full_hp, block_start_hp, nationData, cur_block_data.nationID, damage, crit, false, false, wipe_end_time, wipe_nationID, wipe_flags, targets);
						}
					}
				}
				else if (build_data.type == BuildData.TYPE_RECAPTURE)
				{
					radius_squared = (build_data.attack_radius + 0.5f) * (build_data.attack_radius + 0.5f);
					ArrayList<TargetCandidateRecord> target_candidates = new ArrayList<TargetCandidateRecord>();

					// Iterate all blocks within this block's attack radius, to generate a list of candidate targets.
					for (y = _y - build_data.attack_radius; y <= _y + build_data.attack_radius; y++)
					{
						for (x = _x - build_data.attack_radius; x <= _x + build_data.attack_radius; x++)
						{
							// Skip this block if it is outside of the tower's attack radius, as a circle.
							y_dist = y - _y;
							x_dist = x - _x;
							if (((y_dist * y_dist) + (x_dist * x_dist)) > radius_squared) {
								continue;
							}

							// Get the data for the current block.
							cur_block_data = _land_map.GetBlockData(x, y);

							if (cur_block_data == null) {
								continue;
							}

							// If this block has no extended data, skip it -- recapture towers are only interested in targeting blocks with objects on them.
							if ((block_data.flags & BlockData.BF_EXTENDED_DATA) == 0) {
								continue;
							}

							// If this block is occupied by the tower's nation already, skip it.
							if (cur_block_data.nationID == block_data.nationID) {
								continue;
							}

							// If the block is currently locked, skip it.
							if (cur_block_data.lock_until_time > cur_time) {
								continue;
							}

							// If this block has a lasting wipe that prevents attacks from the tower's nation, skip it.
							if (_land_map.CheckForLastingWipePreventingAttack(x, y, block_data.nationID)) {
								continue;
							}

							// If this block is outside of the tower's nation's max extent, skip it.
							if (BlockIsWithinNationMaxExtent(nationData, _land_map.ID, x, y) == false) {
								continue;
							}

							// Get the extended data for the current block.
							cur_block_ext_data = _land_map.GetBlockExtendedData(x, y, true);

							// If the current block has no object in it, or has an object that is owned by a nation other than the tower's nation, skip it.
							if ((cur_block_ext_data == null) || (cur_block_ext_data.objectID == -1) || ((cur_block_ext_data.owner_nationID != -1) && (cur_block_ext_data.owner_nationID != block_data.nationID))) {
								continue;
							}

							// Target score is the squared distance from the trigger block, plus a penalty for having an object with an owner, thus prioritizing orbs and resources.
							float score = ((x - _trigger_x) * (x - _trigger_x)) + ((y - _trigger_y) * (y - _trigger_y)) + ((cur_block_ext_data.owner_nationID != -1) ? 1000 : 0);

							// Add this block as a target candidate.
							target_candidates.add(new TargetCandidateRecord(x, y, cur_block_data, score));
						}
					}

					// Sort the list of candidate targets, from lowest to highest score.
					Collections.sort(target_candidates);

					//// TESTING -- TRYING TO FIND BUG where Djinn Portal appears to attack even when its square seems to be evaced.
					//if ((build_data.ID == 117) || (build_data.ID == 118) || (build_data.ID == 119)) {
					//	Output.PrintToScreen("DJINN PORTAL at " + _x + "," + _y + " nationID: " + block_data.nationID + ", ownerNationID: " + block_ext_data.owner_nationID + ", capture_time: " + block_ext_data.capture_time + ", cur_time: " + cur_time + " trigged. Triggered by " + _trigger_x + "," + _trigger_y);
					//}

					int target_candidate_index = 0;
					TargetCandidateRecord cur_candidate;
					boolean repeating_target = false;
					for (int target_index = 0; target_index < build_data.num_attacks; target_index++)
					{
						// Exit loop if there are no more target candidates.
						if (target_candidates.size() <= target_candidate_index) {
							break;
						}

						// Get the current target candidate.
						cur_candidate = target_candidates.get(target_candidate_index);

						// Determine the amount of damage to be done to this target block.
						damage = (build_data.attack_min_hp == build_data.attack_max_hp) ? build_data.attack_max_hp : (build_data.attack_min_hp + Constants.random.nextInt(build_data.attack_max_hp - build_data.attack_min_hp + 1));
						crit = nationData.RollForCrit();
						if (crit) damage *= 2;

						// Determine this target block's full HP value.
						block_full_hp = _land_map.DetermineBlockFullHitPoints(cur_candidate.x, cur_candidate.y, false, null);

						// Determine the starting hp of this block. If this same block is being attacked repeatedly, use the previous attack's ending hp value,
						// so as to get an exact value and not be thrown off by the tower action duration that is added to the restore time.
						if (repeating_target) {
							block_start_hp = targets.get(target_index - 1).end_hit_points;
						} else {
							block_start_hp = cur_candidate.block_data.GetBlockCurrentHitPoints(block_full_hp, GetNationHitPointsRate(cur_candidate.block_data.nationID), cur_time);
						}

						//// TESTING -- TRYING TO FIND BUG where Djinn Portal appears to attack even when its square seems to be evaced.
						//if ((build_data.ID == 117) || (build_data.ID == 118) || (build_data.ID == 119)) {
						//	Output.PrintToScreen("    " + _x + "," + _y + " attacking block " + cur_candidate.x + "," + cur_candidate.y + " nationID: " + cur_candidate.block_data.nationID);
						//}

						// Handle the attack of this tower on the current target block, and add the target block's info to the targets array.
						block_end_hp = TowerAttacksBlock(_land_map, cur_candidate.x, cur_candidate.y, block_full_hp, block_start_hp, nationData, cur_candidate.block_data.nationID, damage, crit, true, true, -1, -1, 0, targets);

						//// TESTING -- TRYING TO FIND BUG where Djinn Portal appears to attack even when its square seems to be evaced.
						//if ((build_data.ID == 117) || (build_data.ID == 118) || (build_data.ID == 119)) {
						//	Output.PrintToScreen("    after attack, block " + cur_candidate.x + "," + cur_candidate.y + " nationID: " + cur_candidate.block_data.nationID);
						//}

						// Only advance to the next target candidate if this target block has been defeated. Otherwise the next attack (if there is one) will repeat the same target.
						if (block_end_hp == 0) {
							target_candidate_index++;
							repeating_target = false;
						} else {
							repeating_target = true;
						}
					}

					//// TESTING -- TRYING TO FIND BUG where Djinn Portal appears to attack even when its square seems to be evaced.
					//if ((build_data.ID == 117) || (build_data.ID == 118) || (build_data.ID == 119)) {
					//	Output.PrintToScreen("    Djinn portal done attacking blocks.");
					//}

					// Clear the list of target candidates.
					target_candidates.clear();
				}
				else if (build_data.type == BuildData.TYPE_TOWER_BUSTER)
				{
					// Get the data for the triggering block.
					BlockData target_block_data = _land_map.GetBlockData(_trigger_x, _trigger_y);

					// If the block exists, hasn't already been defeated, and is within the tower nation's max extent...
					if ((target_block_data != null) && (target_block_data.nationID != -1) && (target_block_data.nationID != nationData.ID) && (BlockIsWithinNationMaxExtent(nationData, _land_map.ID, _trigger_x, _trigger_y)))
					{
						// Determine the amount of damage to be done to the target block.
						damage = (build_data.attack_min_hp == build_data.attack_max_hp) ? build_data.attack_max_hp : (build_data.attack_min_hp + Constants.random.nextInt(build_data.attack_max_hp - build_data.attack_min_hp + 1));
						crit = nationData.RollForCrit();
						if (crit) damage *= 2;

						// Determine this target block's full HP value.
						block_full_hp = _land_map.DetermineBlockFullHitPoints(_trigger_x, _trigger_y, false, null);

						// Determine the starting hp of this block.
						block_start_hp = target_block_data.GetBlockCurrentHitPoints(block_full_hp, triggerNationHitPointsRate, cur_time);

						// Handle the attack of this tower on the current target block, and add the target block's info to the targets array.
						block_end_hp = TowerAttacksBlock(_land_map, _trigger_x, _trigger_y, block_full_hp, block_start_hp, nationData, _triggerNationID, damage, crit, true, false, -1, -1, 0, targets);
					}
				}
				else if (build_data.type == BuildData.TYPE_AREA_FORTIFICATION)
				{
					radius_squared = (build_data.effect_radius + 0.5f) * (build_data.effect_radius + 0.5f);

					// Get the nation's tech data.
					NationTechData nationTechData = (NationTechData)DataManager.GetData(Constants.DT_NATIONTECH, block_data.nationID, false);
					if (nationTechData == null) {
						return false;
					}

					// Determine what type of fortification to build.
					int cur_buildID, fort_buildID = -1;
					if (nationTechData.available_upgrades.containsKey(cur_buildID = BuildData.GetNameToIDMap("Root Barricade III"))) fort_buildID = cur_buildID;
					else if (nationTechData.available_upgrades.containsKey(cur_buildID = BuildData.GetNameToIDMap("Root Barricade II"))) fort_buildID = cur_buildID;
					else if (nationTechData.available_builds.containsKey(cur_buildID = BuildData.GetNameToIDMap("Root Barricade"))) fort_buildID = cur_buildID;
					else if (nationTechData.available_upgrades.containsKey(cur_buildID = BuildData.GetNameToIDMap("Strangling Vines III"))) fort_buildID = cur_buildID;
					else if (nationTechData.available_upgrades.containsKey(cur_buildID = BuildData.GetNameToIDMap("Strangling Vines II"))) fort_buildID = cur_buildID;
					else if (nationTechData.available_builds.containsKey(cur_buildID = BuildData.GetNameToIDMap("Strangling Vines"))) fort_buildID = cur_buildID;
					else if (nationTechData.available_upgrades.containsKey(cur_buildID = BuildData.GetNameToIDMap("Hedge III"))) fort_buildID = cur_buildID;
					else if (nationTechData.available_upgrades.containsKey(cur_buildID = BuildData.GetNameToIDMap("Hedge II"))) fort_buildID = cur_buildID;
					else if (nationTechData.available_builds.containsKey(cur_buildID = BuildData.GetNameToIDMap("Hedge"))) fort_buildID = cur_buildID;

					//Output.PrintToScreen("here3 " + fort_buildID + ", sv: " + BuildData.GetNameToIDMap("Strangling Vines II") + ", nation has: " + nationTechData.available_upgrades.containsKey(46));

					// If the nation cannot build any of the applicable fortifications, do nothing.
					if (fort_buildID == -1) {
						return false;
					}

					// Get the build data for the fortification to be built.
					BuildData fort_build_data = BuildData.GetBuildData(fort_buildID);

					// Determine the energy burn for each fort build.
					float fort_energy_burn = nationData.DetermineDiscountedEnergyBurn(fort_build_data);

					// Iterate all blocks within this block's effect radius, to generate the list of targets.
					for (y = _y - build_data.effect_radius; y <= _y + build_data.effect_radius; y++)
					{
						for (x = _x - build_data.effect_radius; x <= _x + build_data.effect_radius; x++)
						{
							// Skip this block if it is outside of the tower's effect radius, as a circle.
							y_dist = y - _y;
							x_dist = x - _x;
							if (((y_dist * y_dist) + (x_dist * x_dist)) > radius_squared) {
								continue;
							}

							// Get the data for the current block.
							cur_block_data = _land_map.GetBlockData(x, y);

							// If this block is not owned by the nation that owns this tower, skip it.
							if (cur_block_data.nationID != block_data.nationID) {
								continue;
							}

							// If the block is currently locked, skip it.
							if (cur_block_data.lock_until_time > cur_time) {
								continue;
							}

							// Get the extended data for the current block.
							cur_block_ext_data = _land_map.GetBlockExtendedData(x, y, true);

							// If the block already has an object on it, skip it.
							if (cur_block_ext_data.objectID != -1) {
								continue;
							}

							// Build the structure
							cur_block_ext_data.objectID = fort_buildID;
							cur_block_ext_data.owner_nationID = block_data.nationID;
							cur_block_ext_data.creation_time = Constants.GetTime();
							cur_block_ext_data.completion_time = Constants.GetTime();
							cur_block_ext_data.invisible_time = DetermineBuildInvisibileTime(fort_build_data, nationData);
							cur_block_ext_data.capture_time = -1;
							cur_block_ext_data.crumble_time = -1;

							// Take cost from nation (energy cost, but not manpower cost).
							ModifyEnergyBurnRate(nationData, _land_map.ID, fort_energy_burn);

							// Mark nation and block data to be updated
							DataManager.MarkBlockForUpdate(_land_map, x, y);
							DataManager.MarkForUpdate(nationData);

							// Broadcast the change to this block to all local clients.
							OutputEvents.BroadcastBlockExtendedDataEvent(_land_map, x, y);

							// If this event took place on a raid map, record it in the raid's replay.
							if (_land_map.ID >= Raid.RAID_ID_BASE) {
								Raid.RecordEvent_ExtendedData(_land_map.ID, _land_map, x, y);
							}

							// Determine this target block's full HP value.
							block_full_hp = _land_map.DetermineBlockFullHitPoints(x, y, false, null);

							// Determine the starting hp of this block.
							block_start_hp = cur_block_data.GetBlockCurrentHitPoints(block_full_hp, triggerNationHitPointsRate, cur_time);

							// Add the target block's info to the targets array.
							block_end_hp = TowerAttacksBlock(_land_map, x, y, block_full_hp, block_start_hp, nationData, _triggerNationID, 0, false, false, false, -1, -1, 0, targets);
						}
					}

					// If this build may affect which of this nation's build objects are inert, broadcast message to update clients.
					if (nationData.GetFinalEnergyBurnRate(_land_map.ID) > nationData.GetFinalEnergyRate(_land_map.ID)) {
						OutputEvents.BroadcastUpdateEvent(block_data.nationID);
					}
				}
				else if (build_data.type == BuildData.TYPE_AIR_DROP)
				{
					radius_squared = (build_data.attack_radius + 0.5f) * (build_data.attack_radius + 0.5f);
					ArrayList<TargetCandidateRecord> target_candidates = new ArrayList<TargetCandidateRecord>();

					// Iterate all blocks within this block's attack radius, to generate a list of candidate targets.
					for (y = _y - build_data.attack_radius; y <= _y + build_data.attack_radius; y++)
					{
						for (x = _x - build_data.attack_radius; x <= _x + build_data.attack_radius; x++)
						{
							// Skip this block if it is outside of the tower's attack radius, as a circle.
							y_dist = y - _y;
							x_dist = x - _x;
							if (((y_dist * y_dist) + (x_dist * x_dist)) > radius_squared) {
								continue;
							}

							// Get the data for the current block.
							cur_block_data = _land_map.GetBlockData(x, y);

							if (cur_block_data == null) {
								continue;
							}

							// If the block is currently locked, skip it.
							if (cur_block_data.lock_until_time > cur_time) {
								continue;
							}

							// If this block is not owned by the nation that triggered this tower or a recently hostile nation, skip it.
							if ((cur_block_data.nationID != _triggerNationID) && (nationData.IsHostileNation(cur_block_data.nationID) == false)) {
								continue;
							}

							if (cur_block_data.BlockHasExtendedData())
							{
								// If this block has a lasting wipe that prevents attacks from the tower's nation, skip it.
								if (_land_map.CheckForLastingWipePreventingAttack(x, y, block_data.nationID)) {
									continue;
								}

								// Get the extended data for the current block.
								cur_block_ext_data = _land_map.GetBlockExtendedData(x, y, true);

								// If this block contains an object, skip it.
								if (cur_block_ext_data.objectID != -1) {
									continue;
								}
							}

							// If this block is outside of the tower's nation's max extent, skip it.
							if (BlockIsWithinNationMaxExtent(nationData, _land_map.ID, x, y) == false) {
								continue;
							}

							// Add this block as a target candidate. No score is needed, as one target will be randomly chosen.
							target_candidates.add(new TargetCandidateRecord(x, y, cur_block_data, 0));
						}
					}

					// If no target candidates were found, and this is not a shard, do nothing. Just return.
					if ((target_candidates.size() == 0) && (build_data.type != BuildData.TYPE_SHARD)) {
						return false;
					}

					// Chose a random target candidate to be the target.
					int target_candidate_index = Constants.random.nextInt(target_candidates.size());
					TargetCandidateRecord cur_candidate = target_candidates.get(target_candidate_index);

					// Determine this target block's full HP value.
					block_full_hp = _land_map.DetermineBlockFullHitPoints(cur_candidate.x, cur_candidate.y, false, null);

					// Determine the starting hp of this block.
					block_start_hp = cur_candidate.block_data.GetBlockCurrentHitPoints(block_full_hp, GetNationHitPointsRate(cur_candidate.block_data.nationID), cur_time);

					// Determine the amount of damage to be done to this block, sufficient to take it over.
					damage = block_start_hp;
					crit = false;

					// Handle the attack of this tower on the current target block, and add the target block's info to the targets array.
					block_end_hp = TowerAttacksBlock(_land_map, cur_candidate.x, cur_candidate.y, block_full_hp, block_start_hp, nationData, cur_candidate.block_data.nationID, damage, crit, true, true, -1, -1, 0, targets);

					// Clear the list of target candidates.
					target_candidates.clear();
				}
			}

			// If there are no eligible targets, and the tower is not a shard, the tower does not activate. Return.
			if ((targets.size() == 0) && (build_data.type != BuildData.TYPE_SHARD))
			{
				targets.clear();
				return flanked;
			}

			// If this object is made visible when triggered, make it visible now.
			if ((build_data.visible_on == BuildData.VISIBLE_ON_TRIGGERED) && (block_ext_data.invisible_time != -1)) {
				block_ext_data.invisible_time = cur_time + BuildData.SECONDS_REMAIN_VISIBLE;
			}

			// Prevent this tower from being triggered again, until this activation and then its cooldown period is complete.
			block_ext_data.triggerable_time = cur_time + Constants.TOWER_ACTION_DURATION + build_data.cooldown_time;

			// Broadcast an event to all clients viewing this area, describing this tower's action.
			OutputEvents.BroadcastTowerActionEvent(_land_map, _x, _y, block_ext_data.objectID, build_data.type, block_ext_data.invisible_time, _trigger_x, _trigger_y, targets, _triggerNationID);

			// If this event is taking place on a raid map...
			if (_land_map.ID >= Raid.RAID_ID_BASE)
			{
				// Record the tower action in the raid's replay.
				Raid.RecordEvent_TowerAction(_land_map.ID, _x, _y, block_ext_data.objectID, build_data.type, block_ext_data.invisible_time, _trigger_x, _trigger_y, targets, _triggerNationID);
			}

			// Trigger any nearby tower that would be triggered by the activation of this block's tower within its attack radius.
			TriggerObjectsInRange(_land_map, _x, _y, block_data.nationID, _triggerNationID, BuildData.TRIGGER_ON_RADIUS_TOWER);

			// Clear the list of targets.
			targets.clear();

			// Update quests system for the activation of this build.
			Quests.HandleBuildActivate(nationData, build_data.ID, 0);
		}
		catch (Exception e) {
      Output.PrintToScreen("Exception in TriggerObject()");
			Output.PrintException(e);
    }

		return flanked;
	}

	public static int TowerAttacksBlock(LandMap _land_map, int _target_blockX, int _target_blockY, int _target_block_full_hp, int _target_block_start_hp, NationData _towerNationData, int _targetNationID, float _damage, boolean _crit, boolean _occupy, boolean _target_block, int _wipe_end_time, int _wipe_nationID, int _wipe_flags, ArrayList<TargetRecord> _targets)
	{
		try {
			// Get the current time.
			int cur_time = Constants.GetTime();

			// Get the target block's data
			BlockData target_block_data = _land_map.GetBlockData(_target_blockX, _target_blockY);

			// Get the target nation's data.
			// Note that this may be NULL, because the tower may be attacking a block that is not occupied by any nation (eg. a recapture tower taking an unoccupied resource).
			NationData targetNationData = (_targetNationID == -1) ? null : (NationData)DataManager.GetData(Constants.DT_NATION, _targetNationID, false);

			if (_land_map == null) {
				Output.PrintToScreen("ERROR: TowerAttacksBlock(): _land_map: " + _land_map);
				return 0;
			}

			if ((_towerNationData == null) || (target_block_data == null)) {
				Output.PrintToScreen("ERROR: TowerAttacksBlock(): _towerNationData: " + _towerNationData + ", target_block_data: " + target_block_data);
				return 0;
			}

			//if (target_block_data.nationID != _targetNationID) { // Note this check is in error -- for area fortification towers, the target nation will not be the block's nation.
			//	Output.PrintToScreen("ERROR: TowerAttacksBlock(): The target block's nationID " + target_block_data.nationID + " != _targetNationID. The block may have already been defeated.");
			//	return 0;
			//}

			// Update the block's object invisibility for this attack.
			UpdateObjectInvisibilityForAttack(_land_map, _target_blockX, _target_blockY, target_block_data);

			// Determine battle flags
			int battle_flags = (_land_map.BlockContainsInertObject(_target_blockX, _target_blockY) ? Constants.BATTLE_FLAG_INERT : 0);
			if (_crit) battle_flags = battle_flags | Constants.BATTLE_FLAG_CRIT;
			if (_target_block) battle_flags = battle_flags | Constants.BATTLE_FLAG_TARGET_BLOCK;

			//Output.PrintToScreen("attack_min_hp: " + build_data.attack_min_hp + ", attack_max_hp: " + build_data.attack_max_hp + ", damage: " + damage);

			int block_new_cur_hp, block_new_full_hp, newNationID;
			float hit_points_rate;
			boolean fast_crumble;
			int block_end_hp = (int)Math.max(0, _target_block_start_hp - _damage);

			// Lock the block for the duration of the tower action, so no player clicks will cause changes to it during that time.
			target_block_data.lock_until_time = cur_time + Constants.TOWER_ACTION_DURATION;

			// If this target block has been defeated...
			if (block_end_hp == 0)
			{
				hit_points_rate = _towerNationData.GetFinalHitPointsRate();
				newNationID = _occupy ? _towerNationData.ID : -1;

				// Record when the block's transition will be completed, so that it will not count in adjacency checks until that time.
				target_block_data.transition_complete_time = cur_time + Constants.TOWER_ACTION_DURATION;

				// For purposes of tournament trophy reward, consider the tower nation's attack ratio to be 1.
				_towerNationData.prev_attack_ratio = 1f;

				if (targetNationData != null)
				{
					if (_occupy)
					{
						// Determine the number of xp to add to the tower's nation for this attack.
						// XP is only added if the target square has been defeated and occupied. It is based on the square's original number of hit points.
						int xp_to_add = DetermineXPToAdd(_land_map.ID, _towerNationData, targetNationData, 1f, _target_block_start_hp);
						if (xp_to_add > 0)
						{
							// Reduce xp_to_add by multiplying by DEFENSE_XP_MULTIPLIER.
							xp_to_add = (int)Math.ceil((float)xp_to_add * Constants.DEFENSE_XP_MULTIPLIER);

							// Add the XP
							AddXP(_towerNationData, xp_to_add, -1, -1, -1, true, true, Constants.TOWER_ACTION_DURATION, Constants.XP_TOWER);

							// Log suspect
							if (_towerNationData.log_suspect_expire_time > cur_time)
							{
								// Log the details of this XP gain.
								Constants.WriteToLog("log_suspect.txt", Constants.GetTimestampString() + ": '" + _towerNationData.name + "'(ID:" + _towerNationData.ID + ", Level:" + _towerNationData.level + ")'s tower attacked and captured block " + _target_blockX + "," + _target_blockY + ", earning " + xp_to_add + " XP.\n");
							}

							// Increment the nation's counts of captured land sqaures.
							_towerNationData.captures_history++;
							_towerNationData.captures_history_monthly++;

							// Update the ranks for captures history.
							RanksData.instance.ranks_nation_captures.UpdateRanks(_towerNationData.ID, _towerNationData.name, _towerNationData.captures_history, Constants.NUM_CAPTURES_RANKS, false);
							RanksData.instance.ranks_nation_captures_monthly.UpdateRanks(_towerNationData.ID, _towerNationData.name, _towerNationData.captures_history_monthly, Constants.NUM_CAPTURES_RANKS, false);

							// Update the nation's users' reports.
							_towerNationData.ModifyUserReportValueInt(UserData.ReportVal.report__defenses_XP, xp_to_add);
						}

						// Update the quests system for this capture by counter attack
						Quests.HandleCounterAttackCapture(_towerNationData, Constants.TOWER_ACTION_DURATION);
					}

					// Update the nations' users' reports.
					_towerNationData.ModifyUserReportValueInt(UserData.ReportVal.report__defenses_squares_defeated, 1);
					targetNationData.ModifyUserReportValueInt(UserData.ReportVal.report__land_lost, 1);
				}

				// Have the tower's nation occupy the target block (or clear the block, if _occupy is false). Don't broadcast this change to viewing clients.
				World.SetBlockNationID(_land_map, _target_blockX, _target_blockY, newNationID, true, false, -1/*TODO: use builder?*/, Constants.TOWER_ACTION_DURATION);

				if (targetNationData != null)
				{
					Footprint target_nation_footprint = targetNationData.GetFootprint(_land_map.ID);

					if (target_nation_footprint == null) {
						Output.PrintToScreen("ERROR: TowerAttacksBlock(): target_nation_footprint: " + target_nation_footprint + ", _targetNationID: " + _targetNationID + ", _land_map.ID: " + _land_map.ID);
						return 0;
					}

					// If the target nation's final block on the mainland has been defeated, and the nation is online, and this is not a raid map, displace the nation to a different location nearby.
					// Do this after the call to SetBlockNationID() above, so that the tournament system can respond to the attacker capturing the square from the defender.
					if ((_land_map.ID == Constants.MAINLAND_MAP_ID) && (target_nation_footprint.area == 0) && (targetNationData.num_members_online > 0) && (_land_map.ID < Raid.RAID_ID_BASE)) {
						World.DisplaceNation(_land_map, targetNationData, _target_blockX, _target_blockY);
					}
				}

				if (_occupy)
				{
					// The block has now been restored to full hit points for the nation that has taken it over (or not yet full if a defense has been recaptured).
					block_new_full_hp = _land_map.DetermineBlockFullHitPoints(_target_blockX, _target_blockY, false, null);
					block_new_cur_hp = _land_map.DetermineBlockFullHitPoints(_target_blockX, _target_blockY, true, null);
					if (block_new_cur_hp < block_new_full_hp) {
						target_block_data.SetHitPointsRestoredTime(cur_time + Constants.TOWER_ACTION_DURATION + (int)((float)(block_new_full_hp - block_new_cur_hp) / (_towerNationData.GetFinalHitPointsRate() / 60.0) + 0.5f)/*, _land_map, _target_blockX, _target_blockY*/);
					}
				}
				else
				{
					block_new_full_hp = block_new_cur_hp = Constants.VACANT_BLOCK_HIT_POINTS;
				}

				// Rapidly crumble the defeated block's defense, if appropriate.
				fast_crumble = AttemptFastCrumbleUponCapture(_land_map, _target_blockX, _target_blockY, target_block_data, _targetNationID, _towerNationData, Constants.TOWER_ACTION_DURATION);

				// If a fast crumble will take place, add it to the battle flags.
				if (fast_crumble) {
					battle_flags = battle_flags | Constants.BATTLE_FLAG_FAST_CRUMBLE;
				}
			}
			else
			{
				block_new_cur_hp = block_end_hp;
				block_new_full_hp = _target_block_full_hp;
				hit_points_rate = GetNationHitPointsRate(targetNationData);
				newNationID = _targetNationID;

				if (block_end_hp < _target_block_full_hp)
				{
					// The target block has lost hit points. Record the time at which they will all have been restored.
					target_block_data.SetHitPointsRestoredTime(cur_time + Constants.TOWER_ACTION_DURATION + (int)((float)(_target_block_full_hp - block_end_hp) / (hit_points_rate / 60.0))/*, _land_map, _target_blockX, _target_blockY*/);
					//Output.PrintToScreen("SetBlockHitPointsRestoredTime() set to " + (cur_time + Constants.TOWER_ACTION_DURATION + (int)((float)(block_full_hp - block_end_hp) / (hit_points_rate / 60.0))) + ", cur_time: " + cur_time + ", block_full_hp: " + block_full_hp + ", block_end_hp: " + block_end_hp + ", hit_points_rate: " + hit_points_rate);

					// Mark the nation's data to be updated
					DataManager.MarkForUpdate(_towerNationData);
				}
			}

			// Record time when this attack on the block will be complete.
			target_block_data.SetAttackCompleteTime(cur_time + Constants.TOWER_ACTION_DURATION);

			if (_wipe_end_time != -1)
			{
				// Get the target block's extended data.
				BlockExtData block_ext_data = _land_map.GetBlockExtendedData(_target_blockX, _target_blockY, true);

				if (block_ext_data.wipe_end_time > _wipe_end_time)
				{
					// The block's existing lasting wipe ends later, so keep that existing wipe.
					_wipe_end_time = block_ext_data.wipe_end_time;
					_wipe_nationID = block_ext_data.wipe_nationID;
					_wipe_flags = block_ext_data.wipe_flags;
				}
				else
				{
					// The block's new lasting wipe ends later, so use the new wipe.
					block_ext_data.wipe_end_time = _wipe_end_time;
					block_ext_data.wipe_nationID = _wipe_nationID;
					block_ext_data.wipe_flags = _wipe_flags;

					// Mark the block to be updated.
					DataManager.MarkBlockForUpdate(_land_map, _target_blockX, _target_blockY);
				}
			}

			//// LOG FOR TESTING
			//if ((_towerNationData.ID == 588) && (targetNationData != null)) // ShadowRealm
			//{
			//	Constants.WriteToLog("log_attack.txt", "Nation " + _towerNationData.ID + "'s tower attacks nation " + targetNationData.name + " at block " + _target_blockX + "," + _target_blockY + ". _target_block_start_hp: " + _target_block_start_hp + ", _damage: " + _damage + ", block_end_hp: " + block_end_hp + ", newNationID: " + newNationID + ", target block's nation: " + target_block_data.nationID);
			//}

			// Create a TargetRecord with information about this current target, and add it to the targets ArrayList.
			_targets.add(new TargetRecord(_target_blockX, _target_blockY, newNationID, _target_block_full_hp, _target_block_start_hp, block_end_hp, block_new_cur_hp, block_new_full_hp, hit_points_rate, battle_flags, _wipe_end_time, _wipe_nationID, _wipe_flags));

			return block_end_hp;
		}
		catch (Exception e) {
      Output.PrintToScreen("Exception in TowerAttacksBlock()");
			Output.PrintException(e);
			return 0;
    }
	}

	public static boolean IsBlockFlanked(LandMap _land_map, int _blockX, int _blockY, BlockData _block_data, NationData _attackerNationData)
	{
		BlockData adj_block_data;
		BlockExtData adj_block_ext_data;

		for (int y = _blockY - 1; y <= _blockY + 1; y++)
		{
			for (int x = _blockX - 1; x <= _blockX + 1; x++)
			{
				// Skip the center square.
				if ((y == _blockY) && (x == _blockX)) {
					continue;
				}

				// Get this adjacent block's data.
				adj_block_data = _land_map.GetBlockData(x, y);

				// If this adjacent block is uninhabitable terrain, it does not prevent flanking.
				if ((adj_block_data.terrain != Constants.TERRAIN_FLAT_LAND) && (adj_block_data.terrain != Constants.TERRAIN_BEACH)) {
					continue;
				}

				// If this adjacent block is habitable but is empty, then the given block isn't flanked.
				if (adj_block_data.nationID == -1) {
					return false;
				}

				// If this adjacent block is inhabited by the attacking nation or an ally of the attacking nation, then this adjacent block doesn't prevent flanking.
				if ((adj_block_data.nationID == _attackerNationData.ID) || (_attackerNationData.alliances_active.indexOf(Integer.valueOf(adj_block_data.nationID)) != -1)) {
					continue;
				}

				// If this adjacent block was just recently captured by the defending nation, and the transition is not yet complete (battle is in progress), do not yet allow it to prevent flanking.
				if ((adj_block_data.nationID == _block_data.nationID) && (Constants.GetTime() <= adj_block_data.transition_complete_time)) {
					continue;
				}

				// If this adjacent block belongs to the defensing nation and has no object in it, then it prevents flanking.
				if ((adj_block_data.nationID == _block_data.nationID) && ((adj_block_data.flags & BlockData.BF_EXTENDED_DATA) == 0)) {
					return false;
				}

				if ((adj_block_data.flags & BlockData.BF_EXTENDED_DATA) != 0)
				{
					// Get this adjacent block's extended data.
					adj_block_ext_data = _land_map.GetBlockExtendedData(x, y, false);

					// If this adjacent block contains a resource or orb, it does not prevent flanking.
					if (adj_block_ext_data.objectID >= ObjectData.RESOURCE_OBJECT_BASE_ID) {
						continue;
					}

					// If this adjacent block belongs to the given block's nation (the defending nation)...
					if (adj_block_data.nationID == _block_data.nationID)
					{
						// An adjacent block belonging to the defending nation only does NOT prevent flanking if it contains an object
						// that is not a wall, that also belong to the defending nation.
						if ((adj_block_ext_data != null) && (adj_block_ext_data.objectID != -1) && (adj_block_ext_data.owner_nationID == adj_block_data.nationID))
						{
							// Get the object's BuildData.
							BuildData build_data = BuildData.GetBuildData(adj_block_ext_data.objectID);

							if (build_data.type != BuildData.TYPE_WALL) {
								continue;
							}
						}
					}
				}

				// If none of the above conditions were met, then this adjacent block prevents the given block from being flanked.
				return false;
			}
		}

		// None of the adjacent blocks prevent the given block from being flanked. Return true.
		return true;
	}

	public static int DetermineBuildInvisibileTime(BuildData _build_data, NationData _nation_data)
	{
		// If the nation's stat does not support invisibility, and the object is not a shard, return -1.
		if ((_nation_data.invisibility == false) && (_build_data.type != BuildData.TYPE_SHARD)) {
			return -1;
		}

		// If the given build object never becomes invisible, return -1.
		if (_build_data.visible_on == BuildData.VISIBLE_ON_ALWAYS) {
			return -1;
		}

		// The object should be made invisible now.
		return Constants.GetTime();
	}

	public static void UpdateObjectInvisibilityForAttack(LandMap _land_map, int _x, int _y, BlockData _block_data)
	{
		if ((_block_data.flags & BlockData.BF_EXTENDED_DATA) != 0)
		{
			// If the block contains an object that can be made invisible, make it visible for duration SECONDS_REMAIN_VISIBLE.
			BlockExtData block_ext_data = _land_map.GetBlockExtendedData(_x, _y, true);
			if ((block_ext_data != null) && (block_ext_data.objectID != -1) && (block_ext_data.invisible_time != -1))
			{
				block_ext_data.invisible_time = Constants.GetTime() + BuildData.SECONDS_REMAIN_VISIBLE;
			}
		}
	}

	// Return true if any block adjacent to the block with the given coords has the given nationID.
	public static boolean IsBlockAdjacentToNation(LandMap _land_map, int _x, int _y, int _nationID)
	{
		int cur_time = Constants.GetTime();

		if (_land_map.BlockHasTransitionedToNationID(_x - 1, _y - 1, _nationID, cur_time)) return true;
		if (_land_map.BlockHasTransitionedToNationID(_x - 1, _y, _nationID, cur_time)) return true;
		if (_land_map.BlockHasTransitionedToNationID(_x - 1, _y + 1, _nationID, cur_time)) return true;
		if (_land_map.BlockHasTransitionedToNationID(_x, _y - 1, _nationID, cur_time)) return true;
		if (_land_map.BlockHasTransitionedToNationID(_x, _y + 1, _nationID, cur_time)) return true;
		if (_land_map.BlockHasTransitionedToNationID(_x + 1, _y - 1, _nationID, cur_time)) return true;
		if (_land_map.BlockHasTransitionedToNationID(_x + 1, _y, _nationID, cur_time)) return true;
		if (_land_map.BlockHasTransitionedToNationID(_x + 1, _y + 1, _nationID, cur_time)) return true;

		return false;
	}

	// Return true if any block adjacent to the block with the given coords is an empty land area.
	public static boolean IsBlockAdjacentToEmpty(int _mapID, int _x, int _y, int [] _adj)
	{
		LandMap land_map = DataManager.GetLandMap(_mapID, false);

		if ((land_map.GetBlockNationID((_adj[ADJ_X] = _x - 1), (_adj[ADJ_Y] = _y - 1)) == -1) && (land_map.GetBlockObjectID(_x - 1, _y - 1) >= -1)) return true;
		if ((land_map.GetBlockNationID((_adj[ADJ_X] = _x - 1), (_adj[ADJ_Y] = _y)) == -1) && (land_map.GetBlockObjectID(_x - 1, _y) >= -1)) return true;
		if ((land_map.GetBlockNationID((_adj[ADJ_X] = _x - 1), (_adj[ADJ_Y] = _y + 1)) == -1) && (land_map.GetBlockObjectID(_x - 1, _y + 1) >= -1)) return true;
		if ((land_map.GetBlockNationID((_adj[ADJ_X] = _x), (_adj[ADJ_Y] = _y - 1)) == -1) && (land_map.GetBlockObjectID(_x, _y - 1) >= -1)) return true;
		if ((land_map.GetBlockNationID((_adj[ADJ_X] = _x), (_adj[ADJ_Y] = _y + 1)) == -1) && (land_map.GetBlockObjectID(_x, _y + 1) >= -1)) return true;
		if ((land_map.GetBlockNationID((_adj[ADJ_X] = _x + 1), (_adj[ADJ_Y] = _y - 1)) == -1) && (land_map.GetBlockObjectID(_x + 1, _y - 1) >= -1)) return true;
		if ((land_map.GetBlockNationID((_adj[ADJ_X] = _x + 1), (_adj[ADJ_Y] = _y)) == -1) && (land_map.GetBlockObjectID(_x + 1, _y) >= -1)) return true;
		if ((land_map.GetBlockNationID((_adj[ADJ_X] = _x + 1), (_adj[ADJ_Y] = _y + 1)) == -1) && (land_map.GetBlockObjectID(_x + 1, _y + 1) >= -1)) return true;

		return false;
	}

	public static void BuyManpower(StringBuffer _output_buffer, int _userID, int _pkg)
	{
		// Get the user's data
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		if (userData.rank > Constants.RANK_CAPTAIN)
		{
			// Return message
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_buy_resource_rank_too_low")); // "You cannot buy resources until you are promoted to Captain."
			return;
		}

		// Determine ID of the user's nation
		int nationID = userData.nationID;

		// Get the user's nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

		if (nationData == null) {
			return;
		}

		// Get the nation's footprint.
		Footprint footprint = nationData.GetFootprint(userData.mapID);

		// If previous manpower purchase was on an earlier day, reset the buy_manpower_day_amount.
		if (footprint.prev_buy_manpower_day < Constants.GetAbsoluteDay())
		{
			footprint.prev_buy_manpower_day = Constants.GetAbsoluteDay();
			footprint.buy_manpower_day_amount = 0;
		}

		// Determine the factor to be applied to manpower prices, based on how much manpower this nation has purchased during the current day.
		float purchased_ratio = (float)footprint.buy_manpower_day_amount / (float)nationData.GetFinalManpowerMax(userData.mapID);
		//float power = (float)Math.floor(purchased_ratio / Constants.BUY_MANPOWER_DAILY_LIMIT);
		//float limit_factor = (float)Math.pow(Constants.BUY_MANPOWER_LIMIT_BASE, power);
		float fraction_allowed_to_buy = Math.max(0f, Constants.BUY_MANPOWER_DAILY_ABSOLUTE_LIMIT - purchased_ratio);

		int amount = 0, price = 0;

		if (_pkg == Constants.BUY_PACKAGE_FILL)
		{
			// Fill to capacity
			amount = (int)(nationData.GetFinalManpowerMax(userData.mapID) - footprint.manpower);
		}
		else if (_pkg == Constants.BUY_PACKAGE_50)
		{
	    // Buy 50% of capacity
			amount = (int)Math.min((int)(nationData.GetFinalManpowerMax(userData.mapID) * 0.5f), nationData.GetFinalManpowerMax(userData.mapID) - footprint.manpower);
		}
		else if (_pkg == Constants.BUY_PACKAGE_10)
		{
	    // Buy 10% of capacity (or less, if 10% is no longer allowed today)
			amount = (int)Math.min((int)(nationData.GetFinalManpowerMax(userData.mapID) * Math.min(0.1f, fraction_allowed_to_buy)), nationData.GetFinalManpowerMax(userData.mapID) - footprint.manpower);
		}

		// Determine price in credits, based on amount.
		price = (int)(AmountToPrice(amount, Constants.BUY_MANPOWER_BASE, Constants.BUY_MANPOWER_MULT)/* * limit_factor*/);

		if (price > nationData.game_money)
		{
			// Return message
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_message_not_enough_credits")); // "We don't have enough credits."
			return;
		}

		// Buy the manpower
		Money.SubtractCost(nationData, price);
		footprint.manpower += amount;

		// Update the nation's users' reports.
		nationData.ModifyUserReportValueFloat(UserData.ReportVal.report__credits_spent, price);

		// Keep track of amount of manpower purchased on the current day.
		footprint.buy_manpower_day_amount += amount;

		// Log this manpower purchase
		Constants.WriteToNationLog(nationData, userData, "Purchased " + amount + " manpower for " + price + " credits on map " + userData.mapID + ".");

		// Mark the nation's data to be updated
		DataManager.MarkForUpdate(nationData);

		// Broadcast buy resource event with new manpower amount, and day's buy amounts, to the nation.
		OutputEvents.BroadcastBuyResourceEvent(nationData);
	}

	public static void BuyEnergy(StringBuffer _output_buffer, int _userID, int _pkg)
	{
		// Get the user's data
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		if (userData.rank > Constants.RANK_CAPTAIN)
		{
			// Return message
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_buy_resource_rank_too_low")); // "You cannot buy resources until you are promoted to Captain."
			return;
		}

		// Determine ID of the user's nation
		int nationID = userData.nationID;

		// Get the user's nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

		if (nationData == null) {
			return;
		}

		// If previous energy purchase was on an earlier day, reset the buy_energy_day_amount.
		if (nationData.prev_buy_energy_day < Constants.GetAbsoluteDay())
		{
			nationData.prev_buy_energy_day = Constants.GetAbsoluteDay();
			nationData.buy_energy_day_amount = 0;
		}

		// Determine the factor to be applied to energy prices, based on how much energy this nation has purchased during the current day.
		float purchased_ratio = nationData.buy_energy_day_amount / nationData.GetFinalEnergyMax();
		//float power = (float)Math.floor(purchased_ratio / Constants.BUY_ENERGY_DAILY_LIMIT);
		//float limit_factor = (float)Math.pow(Constants.BUY_ENERGY_LIMIT_BASE, power);
		float fraction_allowed_to_buy = Math.max(0f, Constants.BUY_ENERGY_DAILY_ABSOLUTE_LIMIT - purchased_ratio);

		int amount = 0, price = 0;

		if (_pkg == Constants.BUY_PACKAGE_FILL)
		{
			// Fill to capacity
			amount = (int)(nationData.GetFinalEnergyMax() - nationData.energy);
		}
		else if (_pkg == Constants.BUY_PACKAGE_50)
		{
	    // Buy 50% of capacity
			amount = (int)Math.min((int)(nationData.GetFinalEnergyMax() * 0.5f), nationData.GetFinalEnergyMax() - nationData.energy);
		}
		else if (_pkg == Constants.BUY_PACKAGE_10)
		{
	    // Buy 10% of capacity (or less, if 10% is no longer allowed today)
			amount = (int)Math.min((int)(nationData.GetFinalEnergyMax() * Math.min(0.1f, fraction_allowed_to_buy)), nationData.GetFinalEnergyMax() - nationData.energy);
		}

		// Determine price in credits, based on amount.
		price = (int)(AmountToPrice(amount, Constants.BUY_ENERGY_BASE, Constants.BUY_ENERGY_MULT)/* * limit_factor*/);

		if (price > nationData.game_money)
		{
			// Return message
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("We don't have enough credits.")); // "We don't have enough credits."
			return;
		}

		// Buy the energy
		Money.SubtractCost(nationData, price);
		nationData.energy += amount;

		// Update the nation's users' reports.
		nationData.ModifyUserReportValueFloat(UserData.ReportVal.report__credits_spent, price);
/*
		// Increase the nation's available energy.
		nationData.available_energy = Math.min(nationData.available_energy_max, nationData.available_energy + amount);

		// Sanity check available_energy
		if (nationData.available_energy < 0)
		{
			Output.PrintToScreen("ERROR: Nation " + nationData.ID + "'s available_energy is " + nationData.available_energy + ". Setting to 0.");
			Output.PrintStackTrace();
			nationData.available_energy = 0;
		}
*/
		// Keep track of amount of energy purchased on the current day.
		nationData.buy_energy_day_amount += amount;

		// Log this energy purchase
		Constants.WriteToNationLog(nationData, userData, "Purchased " + amount + " energy for " + price + " credits.");

		// Mark the nation's data to be updated
		DataManager.MarkForUpdate(nationData);

		// Broadcast buy resource event with new energy amount, and day's buy amounts, to the nation.
		OutputEvents.BroadcastBuyResourceEvent(nationData);
	}

	public static float AmountToPrice(int _amount, float _base, float _mult)
	{
		return (float)Math.ceil(Math.pow(_base, Math.log10(_amount)) * _mult);
	}

	public static void AddXP(NationData _nationData, float _xp_to_add, int _userID, int _block_x, int _block_y, boolean _update_monthly_ranks, boolean _reward_for_level, int _delay, int _type)
	{
		// Add the points to the nation's data
		float prev_nation_xp = _nationData.xp;
		float nation_xp = Math.min(Constants.MAX_XP, Math.max(0f, _nationData.xp + _xp_to_add));
		_nationData.xp = nation_xp;

		// If xp has increased by at least 1...
		if (((int)nation_xp) != ((int)prev_nation_xp))
		{
			// Broadcast addition of points to this nation's logged in players.
			OutputEvents.BroadcastAddXPEvent(_nationData.ID, (int)_xp_to_add, (int)nation_xp, _userID, _block_x, _block_y, _delay);
		}

		// This check is added to keep nations from getting themselves onto the top nations lists via the reset feature, when they re-receive all their xp.
    if (_update_monthly_ranks)
    {
  		// Add the points to the nation's xp_history and xp_history_monthly
			_nationData.xp_history = Math.max(0, _nationData.xp_history + _xp_to_add);
  		_nationData.xp_history_monthly = Math.max(0, _nationData.xp_history_monthly + _xp_to_add);
    }

		// Determine the nation's new level, with the addition of these XP.
		int new_level = _nationData.level;

		// TESTING
		if (((new_level - _nationData.GetRebirthLevelBonus()) < 0) || ((new_level - _nationData.GetRebirthLevelBonus()) >= (Constants.NUM_LEVELS - 1)))
		{
			Output.PrintToScreen("Nation " + _nationData.name + " (" + _nationData.ID + ") level: " + _nationData.level + ", rb count: " + _nationData.rebirth_count + ", rb level bonus: " + _nationData.GetRebirthLevelBonus());
		}

		while (Constants.XP_PER_LEVEL[new_level + 1 - _nationData.GetRebirthLevelBonus()] <= nation_xp) {
			new_level++;
		}
		while (Constants.XP_PER_LEVEL[new_level - _nationData.GetRebirthLevelBonus()] > nation_xp) {
			new_level--;
		}

		// If the nation's level is being changed...
		if (new_level != _nationData.level)
		{
			// Log any unusually large XP additions.
			if ((new_level - _nationData.level) > 1)
			{
				String message = "Nation " + _nationData.name + " (" + _nationData.ID + ") adding " + _xp_to_add + " XP of type " + Constants.XPTypeToString(_type) + " in one shot, raising level from " + _nationData.level + " to " + new_level + ". User ID: " + _userID + ".";
				Constants.WriteToNationLog(_nationData, null, message);
				Output.PrintToScreen(message);
			}

			// Update the nation's users' reports for gaining level(s).
			_nationData.ModifyUserReportValueInt(UserData.ReportVal.report__levels_gained, new_level - _nationData.level);

			// Grant the nation its reward for gaining a level, if appropriate.
			if ((new_level > _nationData.level) && _reward_for_level)
			{
				// Add the given number of credits to this nation.
				Money.AddGameMoney(_nationData, (new_level - _nationData.level) * Constants.LEVEL_UP_REWARD_CREDITS, Money.Source.FREE);
			}

			// Record new level and add advance point(s).
			_nationData.advance_points = Math.max(0, _nationData.advance_points + (new_level - _nationData.level));
			_nationData.level = new_level;

			// Broadcast addition of level to this nation's logged in players.
			OutputEvents.BroadcastSetLevelEvent(_nationData, _delay);

			// Add event to history if appropriate
			if ((_nationData.level / 10.0) == Math.floor(_nationData.level / 10.0)) {
				Comm.SendReport(_nationData.ID, ClientString.Get("svr_report_advanced_level", "level", String.valueOf(_nationData.level)), _delay); // "Advanced to level " + _nationData.level
			}

			// Update the nation's alliances, geographical limits, position in the game world, etc. for the change in level.
			UpdateNationForLevelChange(_nationData, _delay);

			// Add to the available ad bonus for this nation's logged in users, as appropriate.
			Gameplay.AwardAvailableAdBonusToNation(_nationData, 1f, Constants.AD_BONUS_TYPE_LEVEL, -1, -1, 0);

			// Log this level up
			Constants.WriteToNationLog(_nationData, null, "Level " + new_level);
		}

		// Update the global nation xp ranks
		RanksData.instance.ranks_nation_xp.UpdateRanks(_nationData.ID, _nationData.name, (int)_nationData.xp_history, Constants.NUM_XP_RANKS, false);

		// Update the global nation xp monthly ranks
		RanksData.instance.ranks_nation_xp_monthly.UpdateRanks(_nationData.ID, _nationData.name, (int)_nationData.xp_history_monthly, Constants.NUM_XP_RANKS, false);

		// Add the XP to the user's account.
		if (_userID > 0) {
			AddXPToUser(_userID, _xp_to_add);
		}

		// Mark the nation's data to be updated
		DataManager.MarkForUpdate(_nationData);
	}

	public static void AddXPToUser(int _userID, float _xp_to_add)
	{
		// Get the user's data
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		if (userData == null) {
			return;
		}

		// Reset the user's xp_monthly if a new month has started
		if (Constants.GetMonth() != userData.xp_monthly_month)
		{
			userData.xp_monthly_month = Constants.GetMonth();
			userData.xp_monthly = 0;
		}

		// Add the xp to the users record of xp earned during their current login.
		userData.cur_login_XP += _xp_to_add;

		// Add the xp to the user's xp and xp_monthly
		userData.xp = Math.min(Constants.MAX_XP, Math.max(0, userData.xp + _xp_to_add));
		userData.xp_monthly = Math.min(Constants.MAX_XP, Math.max(0, userData.xp_monthly + _xp_to_add));

		// Update the global user xp ranks
		RanksData.instance.ranks_user_xp.UpdateRanks(_userID, userData.name, (int)userData.xp, Constants.NUM_XP_RANKS, true);

		// Update the global user xp monthly ranks
		RanksData.instance.ranks_user_xp_monthly.UpdateRanks(_userID, userData.name, (int)userData.xp_monthly, Constants.NUM_XP_RANKS, true);

		// Mark the user's data to be updated
		DataManager.MarkForUpdate(userData);
	}

	// Update the nation's alliances, geographical limits, position in the game world, etc. for the change in level.
	public static void UpdateNationForLevelChange(NationData _nationData, int _delay)
	{
		// Check the nation's active alliances, and make sure that because of this level change
		// there are not more than the maximum allowed levels difference between this nation and any of its allies.
		NationData allyNationData;
		if (_nationData.alliances_active.size() > 0)
		{
			for (int i = 0; i < _nationData.alliances_active.size(); i++)
			{
				allyNationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationData.alliances_active.get(i), false);
				if(allyNationData !=null)
				{
					if(Math.abs(_nationData.level - allyNationData.level) > Constants.ALLY_LEVEL_DIFF_LIMIT)
					{
						if (_nationData.level > allyNationData.level) {
							Comm.SendReport(_nationData.ID, ClientString.Get("svr_report_advanced_beyond_ally", "nation_name", allyNationData.name), _delay); // "We have advanced far beyond our allies " + allyNationData.name + ". Our alliance with this much weaker nation is broken."
						}

						Alliance.BreakAlliance(_nationData.ID, allyNationData.ID, null);
					}
				}
			}
		}

		// Determine the nation's eastern and western bounds, given its new level.
		LandMap land_map = DataManager.GetLandMap(Constants.MAINLAND_MAP_ID, false);
		int western_limit = land_map.MaxLevelLimitToPosX(_nationData.level) - 1;
		int eastern_limit = land_map.MaxLevelLimitToPosX(land_map.GetEasternLevelLimit(_nationData.level));

		// TESTING
		Constants.WriteToNationLog(_nationData, null, "Leveling to " + _nationData.level + " footprint pre: " + _nationData.mainland_footprint.x0 + "," + _nationData.mainland_footprint.y0 + " to " + _nationData.mainland_footprint.x1 + "," + _nationData.mainland_footprint.y1 + " area: " + _nationData.mainland_footprint.area + ". Western limit: " + western_limit + ", eastern limit: " + eastern_limit);

		// If the nation extends beyond its limit to the west, remove it from the area to the west of its western limit.
		if (_nationData.mainland_footprint.x0 <= western_limit) {
			World.RemoveNationFromArea(land_map, _nationData, _nationData.mainland_footprint.x0, _nationData.mainland_footprint.y0, western_limit, _nationData.mainland_footprint.y1, true); // TEMP LOGGING FOR TESTING -- LOOKING FOR BUG!
		}

		// If the nation extends beyond its limit to the east, remove it from the area to the east of its eastern limit.
		if (_nationData.mainland_footprint.x1 >= eastern_limit) {
			World.RemoveNationFromArea(land_map, _nationData, eastern_limit, _nationData.mainland_footprint.y0, _nationData.mainland_footprint.x1, _nationData.mainland_footprint.y1, false);
		}

		// If this is a vet nation and it extends into the new player area in the south, remove it from the new player area.
		if (_nationData.veteran && (World.new_player_area_boundary != -1) && (_nationData.mainland_footprint.y1 >= World.new_player_area_boundary)) {
			World.RemoveNationFromArea(land_map, _nationData, _nationData.mainland_footprint.x0, World.new_player_area_boundary, _nationData.mainland_footprint.x1, _nationData.mainland_footprint.y1, false);
		}

		// TESTING
		Constants.WriteToNationLog(_nationData, null, "Leveling to " + _nationData.level + " footprint post: " + _nationData.mainland_footprint.x0 + "," + _nationData.mainland_footprint.y0 + " to " + _nationData.mainland_footprint.x1 + "," + _nationData.mainland_footprint.y1 + " area: " + _nationData.mainland_footprint.area + ". Western limit: " + western_limit + ", eastern limit: " + eastern_limit);

		// If the nation has been fully removed from the map, above, then place it from scratch in the level appropriate area.
		if (_nationData.mainland_footprint.area <= 0)
		{
			// Place the nation in a level-appropriate area.
			World.PlaceNation(_nationData);

			// Center the views of all this nation's players on the nation.
			Display.CenterViewsOnNation(_nationData.ID, Constants.MAINLAND_MAP_ID);
		}

		// Redetermine the nation's base geographic efficiency. The nation's supportable area has changed with the level change.
		_nationData.DetermineGeographicEfficiency(Constants.MAINLAND_MAP_ID);
		if (_nationData.homeland_mapID > 0) {
			_nationData.DetermineGeographicEfficiency(_nationData.homeland_mapID);
		}

		// Update the nation's historical record of the highest level it has reached.
		_nationData.level_history = Math.max(_nationData.level_history, _nationData.level);

		// Update the nation level ranks.
		RanksData.instance.ranks_nation_level.UpdateRanks(_nationData.ID, _nationData.name, _nationData.level, Constants.NUM_LEVEL_RANKS, false);

		// Re-determine each of this nation's online users' fealty limits.
		UserData cur_user_data;
		for (int cur_user_index = 0; cur_user_index < _nationData.users.size(); cur_user_index++)
		{
			// Get the current user's data
			cur_user_data = (UserData)DataManager.GetData(Constants.DT_USER, _nationData.users.get(cur_user_index), false);

			// Skip this user if their data doesn't exist or if they're not logged in.
			if ((cur_user_data == null) || (!WOCServer.IsUserLoggedIn(cur_user_data.ID))) {
				continue;
			}

			// Clear fealty to the nation that just rebirthed.
			cur_user_data.ClearFealtyToNation(_nationData.ID);

			// Re-determine this user's fealty limits.
			Login.DetermineFealtyLimit(cur_user_data, _nationData);
		}
	}

	public static float ModifyStatsForObjectCapacity(NationData _nationData, BuildData _buildData, boolean _subtract, boolean _transfer_out)
	{
		float amount_transferred_out = 0;

		if (_buildData.type == BuildData.TYPE_MANPOWER_STORAGE)
		{
			// Determine the current shared amount.
			float shared_amount = _nationData.shared_manpower_fill * _nationData.shared_manpower_capacity;

			if (_subtract)
			{
				// Determine the amount transferred out.
				amount_transferred_out = _transfer_out ? (_nationData.shared_manpower_fill * _buildData.capacity) : 0f;

				// Reduce the nation's XP per hour
				_nationData.shared_manpower_xp_per_hour -= _buildData.xp_per_hour;

				// Sanity check xp per hour.
				if (_nationData.shared_manpower_xp_per_hour < 0)
				{
					Output.PrintToScreen("ERROR: shared_manpower_xp_per_hour reduced to " + _nationData.shared_manpower_xp_per_hour);
					_nationData.shared_manpower_xp_per_hour = 0;
				}

				// Reduce the nation's shared capacity.
				_nationData.shared_manpower_capacity -= _buildData.capacity;

				// Sanity check shared capacity.
				if (_nationData.shared_manpower_capacity < 0)
				{
					Output.PrintToScreen("ERROR: shared_manpower_capacity reduced to " + _nationData.shared_manpower_capacity);
					_nationData.shared_manpower_capacity = 0;
				}

				// If the nation is down to 0 capacity, reset fill to 0.
				if (_nationData.shared_manpower_capacity == 0) {
					_nationData.shared_manpower_fill = 0f;
				}

				// Determine the new fill amount, if the capacity of this object was not transferred out.
				if ((!_transfer_out) && (_nationData.shared_manpower_capacity > 0f)) {
					_nationData.shared_manpower_fill = Math.min(shared_amount, _nationData.shared_manpower_capacity) / _nationData.shared_manpower_capacity;
				}

				// Keep track of the nation's number of share builds.
				_nationData.num_share_builds = Math.max(0, _nationData.num_share_builds - 1);
			}
			else
			{
				// Increase the nation's xp per hour.
				_nationData.shared_manpower_xp_per_hour += _buildData.xp_per_hour;

				// Increase the nation's shared capacity.
				_nationData.shared_manpower_capacity += _buildData.capacity;

				// Determine the new shared fill, keeping the shared amount the same for the new capacity.
				_nationData.shared_manpower_fill = shared_amount / _nationData.shared_manpower_capacity;

				// Keep track of the nation's number of share builds.
				_nationData.num_share_builds++;
			}
		}
		else if (_buildData.type == BuildData.TYPE_ENERGY_STORAGE)
		{
			// Determine the current shared amount.
			float shared_amount = _nationData.shared_energy_fill * _nationData.shared_energy_capacity;

			if (_subtract)
			{
				// Determine the amount transferred out.
				amount_transferred_out = _transfer_out ? (_nationData.shared_energy_fill * _buildData.capacity) : 0f;

				// Reduce the nation's XP per hour
				_nationData.shared_energy_xp_per_hour -= _buildData.xp_per_hour;

				// Sanity check xp per hour.
				if (_nationData.shared_energy_xp_per_hour < 0)
				{
					Output.PrintToScreen("ERROR: shared_energy_xp_per_hour reduced to " + _nationData.shared_energy_xp_per_hour);
					_nationData.shared_energy_xp_per_hour = 0;
				}

				// Reduce the nation's shared capacity.
				_nationData.shared_energy_capacity -= _buildData.capacity;

				// Sanity check shared capacity.
				if (_nationData.shared_energy_capacity < 0)
				{
					Output.PrintToScreen("ERROR: shared_energy_capacity reduced to " + _nationData.shared_energy_capacity);
					_nationData.shared_energy_capacity = 0;
				}

				// If the nation is down to 0 capacity, reset fill to 0.
				if (_nationData.shared_energy_capacity == 0) {
					_nationData.shared_energy_fill = 0f;
				}

				// Determine the new fill amount, if the capacity of this object was not transferred out.
				if ((!_transfer_out) && (_nationData.shared_energy_capacity > 0f)) {
					_nationData.shared_energy_fill = Math.min(shared_amount, _nationData.shared_energy_capacity) / _nationData.shared_energy_capacity;
				}

				// Keep track of the nation's number of share builds.
				_nationData.num_share_builds = Math.max(0, _nationData.num_share_builds - 1);
			}
			else
			{
				// Increase the nation's xp per hour.
				_nationData.shared_energy_xp_per_hour += _buildData.xp_per_hour;

				// Increase the nation's shared capacity.
				_nationData.shared_energy_capacity += _buildData.capacity;

				// Determine the new shared fill, keeping the shared amount the same for the new capacity.
				_nationData.shared_energy_fill = shared_amount / _nationData.shared_energy_capacity;

				// Keep track of the nation's number of share builds.
				_nationData.num_share_builds++;
			}
		}

		return amount_transferred_out;
	}

	public static void TakeEnergyFromAllies(NationData _nationData)
	{
		int available = DetermineEnergyAvailableFromAllies(_nationData);
		int max = (int)_nationData.GetFinalEnergyMax();

		if (available >= (max / 20))
		{
			// Determine amount to take -- up to 25% of the given nation's max capacity.
			int amount_to_take = (int)Math.min(Math.min(max / 4, available), max - _nationData.energy);

			// Take appropriate amount from each ally.
			NationData allyNationData;
			int ally_share_amount, ally_donate_amount;
			for (int cur_alliance_index = 0; cur_alliance_index < _nationData.alliances_active.size(); cur_alliance_index++)
			{
				// Get the current ally nation's data.
				allyNationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationData.alliances_active.get(cur_alliance_index), false);

				// Determine the amount that this ally has available to share.
				ally_share_amount = (int)(allyNationData.shared_energy_capacity * allyNationData.shared_energy_fill);

				// Determine this ally's proportional share of the amount to take from all allies.
				ally_donate_amount = ally_share_amount * amount_to_take / available;

				if (ally_donate_amount > 0)
				{
					// Adjust this ally's fill in order to take the determined amount from them.
					allyNationData.shared_energy_fill = ((float)(ally_share_amount - ally_donate_amount)) / (float)(allyNationData.shared_energy_capacity);

					// Send report to ally.
					Comm.SendReport(allyNationData.ID, ClientString.Get("svr_report_donated_energy", "amount", String.format("%,d", ally_donate_amount), "recipient", _nationData.name), 0);

					// Send stats event to the ally's players.
					OutputEvents.BroadcastStatsEvent(allyNationData.ID, 0);

					// Update the quests system for the donation of this amount of energy to an ally.
					Quests.HandleDonateEnergyToAlly(allyNationData, ally_donate_amount);

					// Update the ally nation's users' reports.
					allyNationData.ModifyUserReportValueFloat(UserData.ReportVal.report__energy_donated, ally_donate_amount);

					// Log this energy donation
					Constants.WriteToNationLog(allyNationData, null, "Donated " + ally_donate_amount + " energy to ally " + _nationData.name + " (" + _nationData.ID + ")");
					Constants.WriteToNationLog(_nationData, null, "Received " + ally_donate_amount + " energy from ally " + allyNationData.name + " (" + allyNationData.ID + ")");

					// Mark this ally nation's data to be updated.
					DataManager.MarkForUpdate(allyNationData);
				}
			}

			// Give the energy to the given nation.
			_nationData.energy += amount_to_take;

			// Broadcast an update event to the given nation's players.
			OutputEvents.BroadcastUpdateEvent(_nationData.ID);

			// Send the updated nation data of each of the receiving nation's allies to the receiving nation's players, to display the change in their storage fill.
			OutputEvents.BroadcastAllyNationDataEvent(_nationData);

			// Send report and message to nation about receiving energy from allies.
			Comm.SendReport(_nationData.ID, ClientString.Get("svr_report_received_energy", "amount", String.format("%,d", amount_to_take)), 0);
			OutputEvents.BroadcastMessageEvent(_nationData.ID, ClientString.Get("svr_msg_received_energy", "amount", String.format("%,d", amount_to_take)));
		}
	}

	public static void TakeManpowerFromAllies(NationData _nationData)
	{
		int available = DetermineManpowerAvailableFromAllies(_nationData);
		int max = (int)_nationData.GetMainlandManpowerMax();

		// Get the nation's mainland footprint.
		Footprint footprint = _nationData.GetFootprint(Constants.MAINLAND_MAP_ID);

		if (available >= (max / 20))
		{
			// Determine amount to take -- up to 25% of the given nation's max capacity.
			int amount_to_take = (int)Math.min(Math.min(max / 4, available), max - footprint.manpower);

			// Take appropriate amount from each ally.
			NationData allyNationData;
			int ally_share_amount, ally_donate_amount;
			for (int cur_alliance_index = 0; cur_alliance_index < _nationData.alliances_active.size(); cur_alliance_index++)
			{
				// Get the current ally nation's data.
				allyNationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationData.alliances_active.get(cur_alliance_index), false);

				// Determine the amount that this ally has available to share.
				ally_share_amount = (int)(allyNationData.shared_manpower_capacity * allyNationData.shared_manpower_fill);

				// Determine this ally's proportional share of the amount to take from all allies.
				ally_donate_amount = ally_share_amount * amount_to_take / available;

				if (ally_donate_amount > 0)
				{
					// Adjust this ally's fill in order to take the determined amount from them.
					allyNationData.shared_manpower_fill = ((float)(ally_share_amount - ally_donate_amount)) / (float)(allyNationData.shared_manpower_capacity);

					// Send report to ally.
					Comm.SendReport(allyNationData.ID, ClientString.Get("svr_report_donated_manpower", "amount", String.format("%,d", ally_donate_amount), "recipient", _nationData.name), 0);

					// Send stats event to the ally's players.
					OutputEvents.BroadcastStatsEvent(allyNationData.ID, 0);

					// Update the quests system for the donation of this amount of manpower to an ally.
					Quests.HandleDonateManpowerToAlly(allyNationData, ally_donate_amount);

					// Update the ally nation's users' reports.
					allyNationData.ModifyUserReportValueFloat(UserData.ReportVal.report__manpower_donated, ally_donate_amount);

					// Log this manpower donation
					Constants.WriteToNationLog(allyNationData, null, "Donated " + ally_donate_amount + " manpower to ally " + _nationData.name + " (" + _nationData.ID + ")");
					Constants.WriteToNationLog(_nationData, null, "Received " + ally_donate_amount + " manpower from ally " + allyNationData.name + " (" + allyNationData.ID + ")");

					// Mark this ally nation's data to be updated.
					DataManager.MarkForUpdate(allyNationData);
				}
			}

			// Give the manpower to the given nation.
			footprint.manpower += amount_to_take;

			// Broadcast an update event to the given nation's players.
			OutputEvents.BroadcastUpdateEvent(_nationData.ID);

			// Send the updated nation data of each of the receiving nation's allies to the receiving nation's players, to display the change in their storage fill.
			OutputEvents.BroadcastAllyNationDataEvent(_nationData);

			// Send report and message to nation about receiving manpower from allies.
			Comm.SendReport(_nationData.ID, ClientString.Get("svr_report_received_manpower", "amount", String.format("%,d", amount_to_take)), 0);
			OutputEvents.BroadcastMessageEvent(_nationData.ID, ClientString.Get("svr_msg_received_manpower", "amount", String.format("%,d", amount_to_take)));
		}
	}

	public static int DetermineEnergyAvailableFromAllies(NationData _nationData)
	{
		int sum = 0;

		// Iterate through all of the given nation's active alliances, summing their stored energy available to allies.
		NationData allyNationData;
		for (int cur_alliance_index = 0; cur_alliance_index < _nationData.alliances_active.size(); cur_alliance_index++)
		{
			// Get the current ally nation's data.
			allyNationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationData.alliances_active.get(cur_alliance_index), false);

			// Add this ally's shared energy to the sum.
			sum += (int)(allyNationData.shared_energy_capacity * allyNationData.shared_energy_fill);
		}

		return sum;
	}

	public static int DetermineManpowerAvailableFromAllies(NationData _nationData)
	{
		int sum = 0;

		// Iterate through all of the given nation's active alliances, summing their stored manpower available to allies.
		NationData allyNationData;
		for (int cur_alliance_index = 0; cur_alliance_index < _nationData.alliances_active.size(); cur_alliance_index++)
		{
			// Get the current ally nation's data.
			allyNationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationData.alliances_active.get(cur_alliance_index), false);

			// Add this ally's shared manpower to the sum.
			sum += (int)(allyNationData.shared_manpower_capacity * allyNationData.shared_manpower_fill);
		}

		return sum;
	}

/*
	public static void DetermineAvailableEnergyStats(NationData _nationData)
	{
		// Initialize this nation's available energy stats to the energy stats of this nation.
		_nationData.available_energy = _nationData.energy;
		_nationData.available_energy_max = _nationData.GetFinalEnergyMax();
		_nationData.available_energy_rate = _nationData.GetFinalEnergyRate();
		_nationData.available_energy_burn_rate = _nationData.GetFinalEnergyBurnRate();

		// Sanity check available_energy
		if (_nationData.available_energy < 0)
		{
			Output.PrintToScreen("ERROR: Nation " + _nationData.ID + "'s available_energy is " + _nationData.available_energy + ". Setting to 0.");
			Output.PrintStackTrace();
			_nationData.available_energy = 0;
		}

		// Iterate through all of the given nation's active alliances, adding their energy stats to this nation's available energy stats.
		NationData allyNationData;
		for (int cur_alliance_index = 0; cur_alliance_index < _nationData.alliances_active.size(); cur_alliance_index++)
		{
			// Get the current ally nation's data.
			allyNationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationData.alliances_active.get(cur_alliance_index), false);

			// Update the ally nation's stats and remove any obsolete technologies from the nation.
			Technology.UpdateStats(allyNationData.ID, allyNationData);

			// Add the ally nation's stats to this nation's stats.
			_nationData.available_energy += allyNationData.energy;
			_nationData.available_energy_max += allyNationData.GetFinalEnergyMax();
			_nationData.available_energy_rate += allyNationData.GetFinalEnergyRate();
			_nationData.available_energy_burn_rate += allyNationData.GetFinalEnergyBurnRate();
		}

		//Output.PrintToScreen("DetermineAvailableEnergyStats() for nation " + _nationData.name + " (" + _nationData.ID + "): energy: " + _nationData.energy + ", available_energy: " + _nationData.available_energy + ", num allies: " + _nationData.alliances_active.size());
	}
*/
	public static void ChangePassword(StringBuffer _output_buffer, int _userID, String _new_password)
	{
		// Get the user's data
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		// If the user's rank disallows this action, return.
		if (userData.rank > Constants.RANK_GENERAL) {
			return;
		}

		// Get the user's nationID
		int nationID = userData.nationID;

		// Get the user's nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

		// Set the user's nation's password record
		nationData.password = _new_password;

		// Mark the nation's data to be updated
		DataManager.MarkForUpdate(nationData);

		// Broadcast the new password to all online players in this nation.
		OutputEvents.BroadcastNationPasswordEvent(nationID, _new_password);

    //Constants.WriteToLog("log_MFU.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " "  + nationData.ID + " evt:  ChangePassword\n");

		// Record event in history of nation
		Comm.SendReport(nationData.ID, ClientString.Get("svr_report_password_changed", "username", userData.name), 0); // "Password to join has been changed by " + userData.name + "."

		// Add the message event string
		OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_msg_password_changed", "new_password", _new_password)); // "Password changed to `" + _new_password + "`"
	}

	public static void Recruit(StringBuffer _output_buffer, int _userID, String _name, String [] _emails)
	{
		int i, num_sent = 0;
		String address = "";
		String subject = "";
		String body = "";

		for (i = 0; i < 4; i++)
		{
			if (_emails[i].equals("")) {
				continue;
			}

			if (_emails[i].matches("^[a-zA-Z0-9_.-]+@[a-zA-Z0-9_-]+\\.[a-zA-Z0-9._-]+") == false) {
				continue;
			}

			if (num_sent > 0) {
				address += ", ";
			}

			address += _emails[i];

			num_sent++;
		}

		// Get the user's data
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		// Get the user's email address
		String email = userData.email;

		// Get the user's nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, userData.nationID, false);

		subject = _name + " invites you to join in battle... and share the rewards!";
		body = "\n" +
		_name + " invites you to join the cause of the nation \n" +
		nationData.name + " as it battles for dominance in a hostile world\n" +
		"where the peril is exceeded only by the opportunity of great fortune.\n" +
		"It is 'War of Conquest', the massively multiplayer online strategy\n" +
		"game where cash prizes are at stake!\n" +
		"\n" +
	  "Join " + nationData.name + " or create your own nation, recruit your\n" +
		"friends to join forces with you, forge alliances or wage war with\n" +
		"your friends or your enemies. Grow in strength and learning, develop\n" +
		"your nation's technological sophistication from stone tools through\n" +
		"nanotechnology and beyond, all while competing for the planet's\n" +
		"precious resources.\n" +
		"\n" +
	  "Your ultimate goal is to search out the ancient and mysterious orbs\n" +
		"that are scattered throughout the landscape, conquer those who would\n" +
		"have them, and make them your own. Every minute you hold on to a\n" +
		"captured orb your cash prize grows. The rewards can be great!\n" +
	  "\n" +
	  "Playing is free.  Just follow the link below:\n" +
	  "\n" +
	  "https://warofconquest.com/aff.php?email=" + email + "\n" +
	  "\n" +
	  "Sign up and join this nation:\n" +
	  "Nation name: " + nationData.name + "\n" +
	  "Password to join: " + nationData.password + "\n" +
	  "\n" +
	  "Begin the adventure... and reap the rewards!\n" +
	  "\n" +
	  "\n";

		// Send the email
		Constants.SendEmail(email, "War of Conquest", address, subject, body);

		// Log e-mail addresses that recruit message was sent to
		Constants.WriteToLog("log_recruit.txt", address + ", ");

		OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get(num_sent + " invitation e-mails sent"));
	}

	public static void AttemptRebirthNation(StringBuffer _output_buffer, int _userID)
	{
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		// If the user's rank disallows this action, return.
		if (userData.rank > Constants.RANK_COSOVEREIGN) {
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("only_sov_or_co_can"));
			return;
		}

		// Get the user's nation ID
		int userNationID = userData.nationID;

		// Get the user's nation's data
		NationData userNationData = (NationData)DataManager.GetData(Constants.DT_NATION, userNationID, false);

		if (userNationData.level < (Constants.REBIRTH_AVAILABLE_LEVEL + userNationData.GetRebirthLevelBonus()))
		{
			// Level is too low to allow rebirth. Do nothing.
			Output.PrintToScreen("Attempt to rebirth nation " + userNationData.name + " (" + userNationID + ") by user " + userData.name + " (" + _userID + "). Level is too low.");
			return;
		}

		if ((userNationData.birth_time + Constants.MIN_NATION_CYCLE_TIME) > Constants.GetTime())
		{
			// Too early to allow rebirth. Do nothing.
			Output.PrintToScreen("Attempt to rebirth nation " + userNationData.name + " (" + userNationID + ") by user " + userData.name + " (" + _userID + "). Too early.");
			return;
		}

		// Log this rebirth
		Constants.WriteToNationLog(userNationData, userData, "Manual rebirth at countdown " + userNationData.rebirth_countdown);

		// Rebirth the nation
		RebirthNation(userNationData);

		// Have the user's client clear the GUI.
		OutputEvents.GetClearGUIEvent(_output_buffer);
	}

	public static void RebirthNation(NationData _nationData)
	{
		Output.PrintToScreen("*** RebirthNation() " + _nationData.name + " (" + _nationData.ID + ")");

		// Make the nation no longer raid eligible and reset its raid medals counts and shard fill levels.
		Raid.RemoveRaidCandidate(_nationData.ID, _nationData.raid_defender_medals);
		_nationData.raid_eligible = false;
		_nationData.raid_attacker_medals = 0;
		_nationData.raid_defender_medals = 0;
		_nationData.shard_red_fill = 0f;
		_nationData.shard_green_fill = 0f;
		_nationData.shard_blue_fill = 0f;

		// Get the nation's homeland map.
		LandMap homeland_map = Homeland.GetHomelandMap(_nationData.ID);

		// Remove the nation from its homeland.
		World.RemoveNationFromMap(homeland_map, _nationData);

		// Place a single square of the nation within its homeland.
		Homeland.PlaceNation(_nationData.ID, homeland_map);

		// Remove the nation from each block it occupies in the mainland game world
		// This must be done before resetting the tech_pending_list, so as not to mess it up.
		// The call to UpdateNationForLevelChange() will place the nation in its new position in the world.
		World.RemoveNationFromMap(DataManager.GetLandMap(Constants.MAINLAND_MAP_ID, false), _nationData);

		// Reset the nation's birth_time to now
		_nationData.birth_time = Constants.GetTime();

		// Increment the nation's rebirth count.
		_nationData.rebirth_count++;

		// Reset the nation's rebirth countdown.
		_nationData.rebirth_countdown = Constants.REBIRTH_COUNTDOWN_START;
		_nationData.rebirth_countdown_start = Constants.REBIRTH_COUNTDOWN_START;
		_nationData.rebirth_countdown_purchased = 0;

		// Reset the nation's level, based on its rebirth count.
		_nationData.level = _nationData.GetRebirthLevelBonus() + Constants.REBIRTH_TO_BASE_LEVEL;

		// Reset the nation's XP to the starting XP for the level it has rebirthed to (not counting the rebirth level bonus).
		_nationData.xp = Math.min(Constants.MAX_XP, Constants.XP_PER_LEVEL[Constants.REBIRTH_TO_BASE_LEVEL]);

		// Mark the nation as being veteran, if appropriate.
		// Do this before UpdateNationForLevelChange() places the nation in the world, so it will be placed in the appropriate area.
		if ((Constants.GetTime() - _nationData.creation_time) >= Constants.VETERAN_NATION_AGE)
		{
			// Mark the nation as being veteran.
			_nationData.veteran = true;
		}

		// Make sure each of the nation's users are considered veteran, and reset their fealty for rebirth.
		UserData cur_user_data;
		for (int cur_user_index = 0; cur_user_index < _nationData.users.size(); cur_user_index++)
		{
			// Get the current user's data
			cur_user_data = (UserData)DataManager.GetData(Constants.DT_USER, _nationData.users.get(cur_user_index), false);

			// If this is a veteran nation after the rebirth, mark the current user as veteran.
			if (_nationData.veteran) {
				cur_user_data.veteran = true;
				Output.PrintToScreen("User '" + cur_user_data.name + "' (" + cur_user_data.ID + ") made vet due to being in nation that rebirths.");
			}

			// Reset this user's fealty for rebirth.
			cur_user_data.ClearFealty(true, true, false, true);

			// Update the user's data.
			DataManager.MarkForUpdate(cur_user_data);
		}

		// Update the nation's alliances, geographical limits, position in the game world, etc. for the change in level.
		// This places the nation in its new position in the world.
		UpdateNationForLevelChange(_nationData, 0);

		// Reset the nation's advances. The number of Advance Points returned to the nation will be equal to its level - 1.
		ResetAdvances(_nationData.ID, true);

		// Add REBIRTH_GAME_MONEY to the nation's game_money
		Money.AddGameMoney(_nationData, Constants.REBIRTH_GAME_MONEY, Money.Source.FREE);

		// Update the nation rebirths ranks.
		RanksData.instance.ranks_nation_rebirths.UpdateRanks(_nationData.ID, _nationData.name, _nationData.rebirth_count, Constants.NUM_REBIRTHS_RANKS, false);

		// Mark the nation's data to be updated
		DataManager.MarkForUpdate(_nationData);

		// Post report to nation
		Comm.SendReport(_nationData.ID, ClientString.Get("svr_report_rebirth"), 0); // "Our nation has been reborn!"

		// Log this rebirth
		Constants.WriteToNationLog(_nationData, null, "Rebirth at countdown " + _nationData.rebirth_countdown);

		// Record user report for the nation's user's.
		_nationData.ModifyUserReportValueInt(UserData.ReportVal.report__rebirth, 1);

		// Broadcast level-related stats to this nation's logged in players.
		OutputEvents.BroadcastSetLevelEvent(_nationData, 0);

		// Broadcast stats event to this nation's logged-in players.
		OutputEvents.BroadcastStatsEvent(_nationData.ID, 0);

		// Broadcast technologies event to this nation's players.
		OutputEvents.BroadcastTechnologiesEvent(_nationData.ID);

		// Broadcast alliances event to this nation's players.
		OutputEvents.BroadcastAlliancesEvent(_nationData.ID);
	}

	public static void ChangeRebirthCountdown(NationData _nationData, float _amount)
	{
		// Use previous countdown value to determine whether rebirth takes place now, to allow it to remain at 0 briefly.
		float prev_countdown = _nationData.rebirth_countdown;

		// Change the nation's rebirth_countdown by the given _amount
		_nationData.rebirth_countdown = Math.max(0f, _nationData.rebirth_countdown + _amount);

		// Make sure that the nation's rebirth_countdown_start is no less than its current rebirth_countdown.
		_nationData.rebirth_countdown_start = (int)Math.max(_nationData.rebirth_countdown_start, _nationData.rebirth_countdown);

		// Rebirth the nation if appropriate
		if ((prev_countdown <= 0f) && (_nationData.rebirth_countdown <= 0f)) {
			RebirthNation(_nationData);
		}
	}

	public static void AttemptResetAdvances(StringBuffer _output_buffer, int _userID)
	{
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		// If the user's rank disallows this action, return.
		if (userData.rank > Constants.RANK_COSOVEREIGN) {
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("only_sov_or_co_can"));
			return;
		}

		// Get the user's nation ID
		int userNationID = userData.nationID;

		// Get the user's nation's data
		NationData userNationData = (NationData)DataManager.GetData(Constants.DT_NATION, userNationID, false);

		if (userNationData.game_money < (Constants.RESET_ADVANCES_BASE_PRICE * userNationData.reset_advances_count))
		{
			// Not enough credits. This should have been caught by the client.
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_message_not_enough_credits_to_do")); // "We don't have enough credits to do that."
			Output.PrintToScreen("Attempt to reset advances for nation " + userNationData.name + " (" + userNationID + ") by user " + userData.name + " (" + _userID + "). Not enough credits.");
			return;
		}

		// Take cost from nation
		Money.SubtractCost(userNationData, Constants.RESET_ADVANCES_BASE_PRICE * userNationData.reset_advances_count);

		// Update the nation's users' reports.
		userNationData.ModifyUserReportValueFloat(UserData.ReportVal.report__credits_spent, (Constants.RESET_ADVANCES_BASE_PRICE * userNationData.reset_advances_count));

		// Broadcast an update event to the nation, letting all players know about the change in credits.
		OutputEvents.BroadcastUpdateEvent(userNationID);

		// Reset the nation's advances.
		ResetAdvances(userNationID, true);

		// Increment the nation's count of how many times its advances have been reset.
		userNationData.reset_advances_count++;

		// Output message
		Output.PrintToScreen("*** " + userNationData.name + " has been reset by its sovereign " + userData.name);

		// Send stats event to the nation's players.
		OutputEvents.BroadcastStatsEvent(userNationID, 0);

		// Broadcast technologies event to this nation's players.
		OutputEvents.BroadcastTechnologiesEvent(userNationID);

		// Post report to nation
		Comm.SendReport(userNationData.ID, ClientString.Get("svr_report_reset", "username", userData.name), 0); // "Our nation's advances have been reset!"

		// Have the user's client clear the GUI.
		OutputEvents.GetClearGUIEvent(_output_buffer);

		// Send success message back to the user
		OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_reset_nation", "username", userData.name));

		// Mark the nation's data to be updated
		DataManager.MarkForUpdate(userNationData);
	}

	public static void ResetAdvances(int _nationID, boolean _broadcast)
	{
		// Get the nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationID, false);
		NationTechData nationTechData = (NationTechData)DataManager.GetData(Constants.DT_NATIONTECH, _nationID, false);

		// Get the nation's mainland footprint.
		Footprint footprint = nationData.GetFootprint(Constants.MAINLAND_MAP_ID);

		// Record the time of the nation's reset, as well as the fraction of its energy and manpower storage that is filled.
		nationData.prev_reset_time = Constants.GetTime();
		nationData.prev_reset_energy_fraction = nationData.energy / nationData.GetFinalEnergyMax();
		nationData.prev_reset_manpower_fraction = footprint.manpower / nationData.GetMainlandManpowerMax();

		// Remove all of the nation's advances (including those from objects).
		nationTechData.tech_count.clear();
		nationTechData.tech_temp_expire_time.clear();
		nationTechData.pending_object_coords.clear();
		nationTechData.available_builds.clear();
		nationTechData.available_upgrades.clear();
		nationData.nextTechExpireTime = -1;
		nationData.nextTechExpire = -1;

		// Reset the nation's stats to the initial values.
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
		nationData.energy_max = Constants.INIT_ENERGY_MAX;
		nationData.energy = nationData.GetFinalEnergyMax() * nationData.prev_reset_energy_fraction;
		nationData.manpower_max = Constants.INIT_MANPOWER_MAX * Constants.manpower_gen_multiplier;
		footprint.manpower = nationData.GetMainlandManpowerMax() * nationData.prev_reset_manpower_fraction;
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

		// Return all of the nation's advance points to the nation.
		nationData.advance_points = nationData.level - 1;

		// Re-add the initial builds to the nation.
		nationTechData.AddInitialAvailableBuilds();

		// Re-add the initial advances to the nation.
		for (Integer initial_advanceID : TechData.initial_advances) {
			Technology.AddTechnology(_nationID, initial_advanceID, 0, false, _broadcast, 0);
		}

		// Add technologies from resource objects.
		AddResourceTechnologies(_nationID, Constants.MAINLAND_MAP_ID, _broadcast);
		if (nationData.homeland_mapID > 0) {
			AddResourceTechnologies(_nationID, nationData.homeland_mapID, _broadcast);
		}

		// Iterate through the nation's area to re-determine its area and energy burn rate.
		RefreshAreaAndEnergyBurnRate(_nationID);

		// Make sure the nation is not incognito, since it no longer has invisibility.
		nationData.SetFlags(nationData.flags & ~Constants.NF_INCOGNITO);

		// Mark the nation's data to be updated
		DataManager.MarkForUpdate(nationData);
		DataManager.MarkForUpdate(nationTechData);
	}

	public static void AddResourceTechnologies(int _nationID, int _landmapID, boolean _broadcast)
	{
		// Get the nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationID, false);
		NationTechData nationTechData = (NationTechData)DataManager.GetData(Constants.DT_NATIONTECH, _nationID, false);

		// Get the landmap's data
		LandMap land_map = DataManager.GetLandMap(_landmapID, false);

		// Get the nation's footprint in this landmap
		Footprint footprint = nationData.GetFootprint(_landmapID);

		// If the nation has no area, do nothing.
		if ((footprint.area == 0) || (footprint.x0 > footprint.x1) || (footprint.y0 > footprint.y1)) {
			return;
		}

		// Iterate through each block owned by this nation...
		BlockData block_data;
		BlockExtData block_ext_data;
		for (int y = footprint.y0; y <= footprint.y1; y++)
		{
			for (int x = footprint.x0; x <= footprint.x1; x++)
			{
				block_data = land_map.GetBlockData(x, y);
				if (block_data.nationID == _nationID)
				{
					if ((block_data.flags & BlockData.BF_EXTENDED_DATA) != 0)
					{
						block_ext_data = land_map.GetBlockExtendedData(x, y, false);

						// If there is a resource object in this block, add its technology to the nation.
						if ((block_ext_data != null) && (block_ext_data.objectID >= ObjectData.RESOURCE_OBJECT_BASE_ID) && (block_ext_data.objectID < ObjectData.ORB_BASE_ID))
						{
							// Get the object's data.
							ObjectData object_data = ObjectData.GetObjectData(block_ext_data.objectID);

							if (object_data == null) {
								continue;
							}

							if (Technology.RequirementsMet(object_data.techID, nationData, nationTechData) == false)
							{
								// Add the object tech to the nation's pending_object_coords array.
								nationTechData.AddPendingObject(x, y);
							}
							else
							{
								// Add the object's technology to the nation
								Technology.AddTechnology(_nationID, object_data.techID, object_data.GetPositionInRange(x, y, land_map), false, _broadcast, 0);
							}
						}
					}
				}
			}
		}
	}

	public static void RefreshAreaAndEnergyBurnRate(int _nationID)
	{
		// Get the nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationID, false);

		// Reset the nation's storage structure related data.
		nationData.shared_energy_capacity = 0;
		nationData.shared_manpower_capacity = 0;
		nationData.shared_energy_xp_per_hour = 0;
		nationData.shared_manpower_xp_per_hour = 0;
		nationData.num_share_builds = 0;

		// Clear the nation's list of objects.
		nationData.objects.clear();

		// Clear the nation's list of build counts.
		nationData.builds.clear();

		// Refresh area and energy burn rate in the mainland.
		RefreshAreaAndEnergyBurnRate(nationData, Constants.MAINLAND_MAP_ID);

		// Refresh area and energy burn rate in the nation's homeland.
		if (nationData.homeland_mapID > 0) {
			RefreshAreaAndEnergyBurnRate(nationData, nationData.homeland_mapID);
		}

		// If the nation has no shared manpower or energy capacity, set the corresponding fill amount to 0.
		if (nationData.shared_manpower_capacity == 0) nationData.shared_manpower_fill = 0f;
		if (nationData.shared_energy_capacity == 0) nationData.shared_energy_fill = 0f;
	}

	public static void RefreshAreaAndEnergyBurnRate(NationData _nationData, int _landmapID)
	{
		// Get the landmap's data
		LandMap land_map = DataManager.GetLandMap(_landmapID, false);

		// Get the nation's footprint in the landmap
		Footprint footprint = _nationData.GetFootprint(_landmapID);

		//// TESTING
		//Output.PrintToScreen("RefreshAreaAndEnergyBurnRate() called for nation " + _nationData.name + " (" + _nationID + "). Pre: area: " + footprint.area + ", border_area: " + footprint.border_area + ", perimeter: " + footprint.perimeter + ", energy_burn_rate: " + footprint.energy_burn_rate);

		// Reset the nation's energy burn rate.
		footprint.energy_burn_rate = 0;

		if (footprint.area != 0)
		{
			// Record original footprint as bounds of box to go through.
			int x0 = footprint.x0;
			int x1 = footprint.x1;
			int y0 = footprint.y0;
			int y1 = footprint.y1;

			// Reset the nation's footprint information.
			footprint.Reset();

			// Reset the nation's area related data.
			footprint.area = 0;
			footprint.border_area = 0;
			footprint.perimeter = 0;

			// Iterate through each block owned by this nation...
			BlockData block_data;
			BlockExtData block_ext_data;
			int block_perimeter;
			for (int y = y0; y <= y1; y++)
			{
				for (int x = x0; x <= x1; x++)
				{
					block_data = land_map.GetBlockData(x, y);
					if (block_data.nationID == _nationData.ID)
					{
						// Update the nation's area and other stats for the contents of this block that it owns.
						UpdateNationAreaForBlock(_nationData, land_map, footprint, block_data, x, y);
					}
				}
			}
		}

		// Redetermine the nation's base geographic efficiency.
		_nationData.DetermineGeographicEfficiency(_landmapID);

		//// TESTING
		//Output.PrintToScreen("RefreshAreaAndEnergyBurnRate() called for nation " + _nationData.name + " (" + _nationID + "). Post: area: " + footprint.area + ", border_area: " + footprint.border_area + ", perimeter: " + footprint.perimeter + ", energy_burn_rate: " + footprint.energy_burn_rate);
	}

	public static void UpdateNationAreaForBlock(NationData _nationData, LandMap _land_map, Footprint _footprint, BlockData _block_data, int _x, int _y)
	{
		// Update the footprint's area.
		_footprint.area++;

		// Modify border_area for this block
		if (_land_map.BlockIsNationBorder(_x, _y, _nationData.ID)) {
			_footprint.border_area++;
		}

		// Modify perimeter for this block
		int block_perimeter = _land_map.DetermineBlockPerimeter(_x, _y, _nationData.ID);
		_footprint.perimeter += block_perimeter;

		// Modify footprint boundary for this block
		_footprint.x0 = Math.min(_footprint.x0, _x);
		_footprint.x1 = Math.max(_footprint.x1, _x);
		_footprint.y0 = Math.min(_footprint.y0, _y);
		_footprint.y1 = Math.max(_footprint.y1, _y);

		if ((_block_data.flags & BlockData.BF_EXTENDED_DATA) != 0)
		{
			BlockExtData block_ext_data = _land_map.GetBlockExtendedData(_x, _y, false);
			if ((block_ext_data != null) && (block_ext_data.objectID != -1))
			{
				if (block_ext_data.objectID < ObjectData.RESOURCE_OBJECT_BASE_ID)
				{
					// If there is an object in this block that is owned by this nation, add its energy_burn_rate to the nation's energy_burn_rate, and re-determine its invisibility.
					if (block_ext_data.owner_nationID == _block_data.nationID)
					{
						// Get the object's BuildData.
						BuildData build_data = BuildData.GetBuildData(block_ext_data.objectID);

						// If this build has a max count, keep track of this nation's count of this build.
						if (build_data.max_count != -1) {
							_nationData.ModifyBuildCount(build_data.ID, 1);
						}

						// Determine the structure's energy burn rate for this nation.
						float energy_burn_rate = _nationData.DetermineDiscountedEnergyBurn(build_data);

						// Add the object's energy burn rate to the nation's energy_burn_rate.
						_footprint.energy_burn_rate += energy_burn_rate;

						// Re-determine the object's invisibility.
						block_ext_data.invisible_time = DetermineBuildInvisibileTime(build_data, _nationData);

						// Update the block to the DB.
						DataManager.MarkBlockForUpdate(_land_map, _x, _y);

						if ((build_data.type == BuildData.TYPE_MANPOWER_STORAGE) || (build_data.type == BuildData.TYPE_ENERGY_STORAGE))
						{
							// Keep track of the nation's number of storage structures for sharing with allies.
							_nationData.num_share_builds++;

							// Keep track of the nation's shared manpower and energy capacities, and XP per hour generated by these structures.
							if (build_data.type == BuildData.TYPE_MANPOWER_STORAGE)
							{
								_nationData.shared_manpower_capacity += build_data.capacity;
								_nationData.shared_manpower_xp_per_hour += build_data.xp_per_hour;
							}

							if (build_data.type == BuildData.TYPE_ENERGY_STORAGE)
							{
								_nationData.shared_energy_capacity += build_data.capacity;
								_nationData.shared_energy_xp_per_hour += build_data.xp_per_hour;
							}
						}
					}
				}
				else
				{
					// Add a record of this object to the nation.
					_nationData.AddObjectRecord(_land_map.ID, _x, _y, block_ext_data.objectID);
				}
			}
		}
	}

	public static void UpdateInvisibilityOfObjects(NationData _nationData, int _landmapID)
	{
		// Get the mainland map's data
		LandMap land_map = DataManager.GetLandMap(_landmapID, false);

		// Get the nation's footprint in the mainland map
		Footprint footprint = _nationData.GetFootprint(_landmapID);

		if (footprint.area > 0)
		{
			// Iterate through each block owned by this nation...
			BlockData block_data;
			BlockExtData block_ext_data;
			for (int y = footprint.y0; y <= footprint.y1; y++)
			{
				for (int x = footprint.x0; x <= footprint.x1; x++)
				{
					block_data = land_map.GetBlockData(x, y);
					if (block_data.nationID == _nationData.ID)
					{
						if ((block_data.flags & BlockData.BF_EXTENDED_DATA) != 0)
						{
							block_ext_data = land_map.GetBlockExtendedData(x, y, false);
							if ((block_ext_data != null) && (block_ext_data.objectID != -1))
							{
								// If there is an object in this block that is owned by this nation, re-determine its invisibility.
								if (block_ext_data.owner_nationID == block_data.nationID)
								{
									// Get the object's BuildData.
									BuildData build_data = BuildData.GetBuildData(block_ext_data.objectID);

									// Re-determine the object's invisibility.
									block_ext_data.invisible_time = DetermineBuildInvisibileTime(build_data, _nationData);

									// Update the block to the DB.
									DataManager.MarkBlockForUpdate(land_map, x, y);
								}
							}
						}
					}
				}
			}
		}
	}

	public static boolean IsNationVanquishable(NationData _nationData)
	{
		// For now, do not allow nations to be vanquished. This may be changed in the future.
		return false;

		/*
		// Determine time since nation last used
		int time_since_last_use = Constants.GetTime() - _nationData.prev_use_time;

		int max_time_allowed = Constants.TIME_SINCE_LAST_USE_ALLOW_NATION_VANQUISH;

		if ((_nationData.prize_money_history >= 1.0f) || (_nationData.money_spent >= 1.0f))
		{
			// Allow longer disuse period for nations that have won or spent money
			max_time_allowed = Constants.TIME_SINCE_LAST_USE_ALLOW_FAVORED_NATION_VANQUISH;
		}

		// Nation is vanquishable if its time_since_last_use is greater than its determined max_time_allowed
		return (time_since_last_use > max_time_allowed);
		*/
	}

	public static void Migrate(StringBuffer _output_buffer, int _userID, boolean _wipe_nation)
	{
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		// If the user's rank disallows this action, return.
		if (userData.rank > Constants.RANK_COSOVEREIGN) {
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_migrate_rank_too_low")); // "Only the Sovereign or Co-Sovereign choose to migrate."
			return;
		}

		// Get the user's nation ID
		int userNationID = userData.nationID;

		// Get the user's nation's data
		NationData userNationData = (NationData)DataManager.GetData(Constants.DT_NATION, userNationID, false);

		// Enforce nation's maximum extent.
		if (_wipe_nation == false)
		{
			if (BlockIsWithinNationMaxExtent(userNationData, userData.mapID, userData.viewX, userData.viewY) == false)
			{
				OutputEvents.GetRequestorEvent(_output_buffer, ClientString.Get("svr_beyond_max_extent")); // "We will need to evacuate land on the other side of our nation before we can go there, because a nation cannot span an area larger than 500 by 500."
				return;
			}
		}

		// Get the user's land map data
		LandMap land_map = DataManager.GetLandMap(userData.mapID, false);

		if (land_map == null)
		{
			Output.PrintToScreen("ERROR: Migrate() user " + userData.name + " (" + userData.ID + ") has mapID " + userData.mapID + "; land_map == null.");
			return;
		}

		if ((Constants.GetTime() - userNationData.prev_free_migration_time) < (Constants.FREE_MIGRATION_PERIOD - 10)) // 10 second buffer
		{
			// This is a paid migration.
			if (userNationData.game_money < Constants.MIGRATION_COST)
			{
				OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_migrate_not_enough_credits")); // "We do not have enough credits to migrate."
				return;
			}
			else
			{
				// Take cost from nation
				Money.SubtractCost(userNationData, Constants.MIGRATION_COST);

				// Update the nation's users' reports.
				userNationData.ModifyUserReportValueFloat(UserData.ReportVal.report__credits_spent, Constants.MIGRATION_COST);

				// Broadcast an update event to the nation, letting all players know about the change in credits.
				OutputEvents.BroadcastUpdateEvent(userNationID);
			}
		}
		else
		{
			// This is a free migration. Update time at which prev free migration took place.
			userNationData.prev_free_migration_time = Constants.GetTime();
		}

		// Migrate the nation
		World.MigrateNation(land_map, userNationID, userData.viewX, userData.viewY, _wipe_nation, _userID, userData.admin);

		// Mark the nation's data to be updated
		DataManager.MarkForUpdate(userNationData);

		if (_wipe_nation)
		{
			// Record event in history
			Comm.SendReport(userNationData.ID, ClientString.Get("svr_report_migrated_by", "username", userData.name), 0); // "Migrated by " + userData.name + "."

			// Write event to log
			Constants.WriteToLog("log_migrate.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " " + userNationData.name + " migrated by " + userData.name + "\n");
		}

		// Log this migration
		Constants.WriteToNationLog(userNationData, userData, "Migrated " + (_wipe_nation ? "nation" : "colony"));
	}

	public static boolean BlockIsWithinNationMaxExtent(NationData _nationData, int _mapID, int _x, int _y)
	{
		// There is no max extent limitation on maps other than the mainland.
		if (_mapID != Constants.MAINLAND_MAP_ID) {
			return true;
		}

		// If the given position is already within the nation's footprint, allow it.
		if ((_x >= _nationData.mainland_footprint.x0) && (_x <= _nationData.mainland_footprint.x1) && (_y >= _nationData.mainland_footprint.y0) && (_y <= _nationData.mainland_footprint.y1)) {
			return true;
		}

		if (((_x - _nationData.mainland_footprint.x0) >= Constants.NATION_MAX_EXTENT) ||
			  ((_nationData.mainland_footprint.x1 - _x) >= Constants.NATION_MAX_EXTENT) ||
				((_y - _nationData.mainland_footprint.y0) >= Constants.NATION_MAX_EXTENT) ||
			  ((_nationData.mainland_footprint.y1 - _y) >= Constants.NATION_MAX_EXTENT))
		{
			return false;
		}

		return true;
	}

	public static void SetMapFlag(StringBuffer _output_buffer, int _userID, int _x, int _y, String _desc)
	{
		// Get the user's data
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		// Get the user's nation's data
		int nationID = userData.nationID;
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

		if (userData.rank > Constants.RANK_WARRIOR)
		{
			// Return message
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_map_flags_rank_too_low")); // "You cannot create map flags until you are promoted to Warrior."
			return;
		}

    if(nationData.map_flags_token.size() >= Constants.MAX_FLAG_COUNT)
    {
			// Return message
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_map_flags_max_num", "max", String.valueOf(Constants.MAX_FLAG_COUNT), "num", String.valueOf(nationData.map_flags_token.size()))); // "You cannot have more than {max} flags. You currently have {num} flags."
			return;
    }

		// Remove emojis
		_desc = _desc.replaceAll("[^\\p{L}\\p{M}\\p{N}\\p{P}\\p{Z}\\p{Cf}\\p{Cs}\\s]","");

		// Limit the length of the description string
		if (_desc.length() > Constants.MAX_FLAG_DESC_LENGTH) {
			_desc = _desc.substring(0, Constants.MAX_FLAG_DESC_LENGTH - 1);
		}

		// Determine this map flag's location token
		int token = Constants.TokenizeCoordinates(_x, _y);

		// Remove any map flags with the determined token from the nation's list of map flags
		int index;
		while ((index = nationData.map_flags_token.indexOf(Integer.valueOf(token))) != -1)
		{
			nationData.map_flags_token.remove(index);
			nationData.map_flags_title.remove(index);
		}

		// Add this flag to the nation's list of map flags
		nationData.map_flags_token.add(token);
		nationData.map_flags_title.add(_desc);

		// Broadcast message to the nation's users, setting the map flag.
		OutputEvents.BroadcastSetMapFlagEvent(nationID, _x, _y, _desc);

		// Mark this nation's data to be updated
		DataManager.MarkForUpdate(nationData);
	}

	public static void DeleteMapFlag(StringBuffer _output_buffer, int _userID, int _x, int _y)
	{
		// Get the user's data
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		// Get the user's nation's data
		int nationID = userData.nationID;
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

		// If the user's rank disallows this action, return.
		if (userData.rank > Constants.RANK_COMMANDER) {
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_map_flags_delete_rank_too_low")); // "Your rank is too low to delete map flags."
			return;
		}

		// Determine this map flag's location token
		int token = Constants.TokenizeCoordinates(_x, _y);

		// Remove any map flags with the given _token from the nation's list of map flags
		int index;
		while ((index = nationData.map_flags_token.indexOf(Integer.valueOf(token))) != -1)
		{
			nationData.map_flags_token.remove(index);
			nationData.map_flags_title.remove(index);
		}

		// Broadcast message to the nation's users, deleting the map flag.
		OutputEvents.BroadcastDeleteMapFlagEvent(nationID, _x, _y);

		// Mark this nation's data to be updated
		DataManager.MarkForUpdate(nationData);
	}

	public static void SetNationFlags(StringBuffer _output_buffer, int _userID, int _flags)
	{
		// Get the user's nation's data
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, userData.nationID, false);

		// If the user is trying to turn off incognito mode, and it hasn't been on long enough to allow it to turn off yet, don't make the change.
		if (nationData.GetFlag(Constants.NF_INCOGNITO) && ((_flags & Constants.NF_INCOGNITO) == 0) && ((Constants.GetTime() - nationData.prev_go_incognito_time) < Constants.MIN_INCOGNITO_PERIOD))
		{
			OutputEvents.GetRequestorEvent(_output_buffer, ClientString.Get("svr_too_soon_exit_incognito", "minutes_min", String.valueOf(Constants.MIN_INCOGNITO_PERIOD / 60), "minutes_remaining", String.valueOf((Constants.MIN_INCOGNITO_PERIOD - (Constants.GetTime() - nationData.prev_go_incognito_time)) / 60))); // "Once we've gone incognito, we must stay incognito for at least {minutes_min} minutes. We can turn off incognito in {minutes_remaining} minutes."
			OutputEvents.GetNationDataEvent(_output_buffer, nationData);
			return;
		}

		// Set the user's nation's flags
		nationData.SetFlags(_flags);

	}

	public static void SaveTutorialState(int _userID, String _state)
	{
    // Get the user's data
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		if (userData == null) { // In rare cases, this event may be received after user has logged out.
			return;
		}

		// Record the new tutorial state
		userData.tutorial_state = _state;

		// Mark the user's data to be updated.
		DataManager.MarkForUpdate(userData);
	}

	public static void AwardAvailableAdBonusToNation(NationData _nationData, float _fraction, int _type, int _x, int _y, int _delay)
	{
		// Add to the ad bonus credits for all of this nation's users who are logged in (if appropriate).
		int curUserID;
		UserData curUserData;
		for (int i = 0; i < _nationData.users.size(); i++)
		{
			// Get the current user's ID and data
			curUserID = _nationData.users.get(i);
			curUserData = (UserData)DataManager.GetData(Constants.DT_USER, curUserID, false);

			// If this user is logged in, add to their available ad bonus (if appropriate).
			if ((curUserData != null) && (curUserData.client_thread != null)) {
				AwardAvailableAdBonus(null, curUserData, _fraction, _type, _x, _y, _delay);
			}
		}
	}

	public static void AwardAvailableAdBonus(StringBuffer _output_buffer, UserData _userData, float _fraction, int _type, int _x, int _y, int _delay)
	{
		// If this user's client device, or this server, does not allow ad bonuses, do nothing.
		if ((_userData == null) || (!_userData.ad_bonuses_allowed)) {
			return;
		}

		// Determine the amount for this ad bonus.
		int amount = 0;
		switch(_type)
		{
			case Constants.AD_BONUS_TYPE_RESOURCE: amount = Constants.AD_BONUS_AMOUNT_RESOURCE; break;
			case Constants.AD_BONUS_TYPE_ORB: amount = Constants.AD_BONUS_AMOUNT_ORB; break;
			case Constants.AD_BONUS_TYPE_LEVEL: amount = Constants.AD_BONUS_AMOUNT_LEVEL; break;
			case Constants.AD_BONUS_TYPE_QUEST: amount = Constants.AD_BONUS_AMOUNT_QUEST; break;
			case Constants.AD_BONUS_TYPE_RAID: amount = Constants.AD_BONUS_AMOUNT_RAID; break;
			case Constants.AD_BONUS_TYPE_BLOCKS: amount = Constants.AD_BONUS_AMOUNT_BLOCKS; break;
		}

		// If the ad_bonus_available >= a threshold, reduce the amount appropriately.
		if (_userData.ad_bonus_available >= Constants.AD_BONUS_AMOUNT_ZERO_THRESHOLD) {
			amount = 0;
		}
		else if (_userData.ad_bonus_available >= Constants.AD_BONUS_AMOUNT_SINGLE_THRESHOLD) {
			amount = Math.min(amount, 1);
		}
		else if (_userData.ad_bonus_available >= Constants.AD_BONUS_AMOUNT_HALVE_THRESHOLD) {
			amount = amount / 2;
		}

		// Multiply the amount by the given _fraction.
		amount = (int)(amount * _fraction + 0.5);

		if (amount > 0)
		{
			// Add the determined amount to this user's ad_bonus_available.
			_userData.ad_bonus_available += amount;

			// Mark the user's data to be updated.
			DataManager.MarkForUpdate(_userData);

			// Send event to the user.
			if (_output_buffer != null) {
				OutputEvents.GetAwardAvailableAdBonusEvent(_output_buffer, amount, _userData.ad_bonus_available, _type, _x, _y, _delay);
			} else {
				OutputEvents.SendAwardAvailableAdBonusEvent(_userData.ID, amount, _userData.ad_bonus_available, _type, _x, _y, _delay);
			}
		}
	}

	public static void AdWatched(StringBuffer _output_buffer, int _userID)
	{
		// Get the user's data.
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		// If this user cannot receive ad bonuses, or if their available bonus is 0, do nothing.
		if ((userData.ad_bonuses_allowed == false) || (userData.ad_bonus_available == 0)) {
			return;
		}

		// Get the user's nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, userData.nationID, false);

		// Add the user's ad_bonus_available number of credits to the nation, as having been purchased.
		Money.AddGameMoney(nationData, userData.ad_bonus_available, Money.Source.PURCHASED);

		// Broadcast the change in this nation's number of credits.
		OutputEvents.BroadcastUpdateBarsEvent(nationData.ID, 0, 0, 0, 0, userData.ad_bonus_available, 0);

		// Reset the user's ad_bonus_available to 0.
		userData.ad_bonus_available = 0;
		DataManager.MarkForUpdate(userData);
	}

	private static class TargetCandidateRecord implements Comparable<TargetCandidateRecord>
	{
		int x, y;
		BlockData block_data;
		float score;

		public TargetCandidateRecord(int _x, int _y, BlockData _block_data, float _score)
		{
			x = _x;
			y = _y;
			block_data = _block_data;
			score = _score;
		}

		public int compareTo(TargetCandidateRecord _compare_record) {
			return (score > _compare_record.score) ? 1 : ((score == _compare_record.score) ? 0 : -1);
		}
	}
}
