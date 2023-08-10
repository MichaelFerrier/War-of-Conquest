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

public class World
{
	// Radius around single square nation's location where it is displaced if attacked
	static final int DISPLACE_NATION_RADIUS = 50;

	static int NUM_PLACEMENT_ATTEMPTS = 64;
	static int NATION_PLACEMENT_BAND_MAX_WIDTH = 100;
	static int NATION_PLACEMENT_BAND_LEVELS_WIDE = 3;

	// Minimum number of adjacent blocks that must be empty around a nation's placement position.
	static int PLACEMENT_MIN_EMPTY_ADJACENTS = 4;

	// Nation placement criterias.
	static int PLACEMENT_CRITERIA_STRICT = 0;
	static int PLACEMENT_CRITERIA_ALLOW_NO_EMPTY_ADJACENTS = 1;
	static int PLACEMENT_CRITERIA_LOOSE = 2;

	static final int EDGE_LEFT    = 0;
	static final int EDGE_RIGHT   = 1;
	static final int EDGE_TOP     = 2;
	static final int EDGE_BOTTOM  = 3;

	public static int new_player_area_boundary = -1;

	public static void DetermineNewPlayerAreaBoundary()
	{
		// Default to -1, meaning no new player area.
		new_player_area_boundary = -1;

		// Get the mainland LandMap.
		LandMap land_map = DataManager.GetLandMap(Constants.MAINLAND_MAP_ID, false);

		// TESTING
		//Output.PrintToScreen("Level 1 limit X: " + land_map.MaxLevelLimitToPosX(1) + ", level 6 limit X: " + land_map.MaxLevelLimitToPosX(6));

		// If there is no mainland map, or the new player area boundary equals 1 (meaning the bottom of the map), then there is no new player area. Return.
		if ((land_map == null) || (land_map.height == 0) || (Constants.new_player_area_boundary >= 1f)) {
			return;
		}

		// Determine the y position of the upper boundary of the new player area.
		new_player_area_boundary = (int)(Constants.new_player_area_boundary * land_map.height);
	}

	public static void MigrateNation(LandMap _land_map, int _nationID, int _centerX, int _centerY, boolean _wipe_nation, int _userID, boolean _admin)
	{
		// Get the nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationID, false);

		if (_wipe_nation)
		{
			// Remove the nation from each block it occupies in the world
			RemoveNationFromMap(_land_map, nationData);
		}

		int x0 = _centerX - Constants.MIGRATE_PLACEMENT_RADIUS;
		int y0 = _centerY - Constants.MIGRATE_PLACEMENT_RADIUS;
		int x1 = _centerX + Constants.MIGRATE_PLACEMENT_RADIUS;
		int y1 = _centerY + Constants.MIGRATE_PLACEMENT_RADIUS;

		if ((!_admin) && (_land_map.ID == Constants.MAINLAND_MAP_ID))
		{
			// Get the user's data
			UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

			// Determine nation's horizontal boundaries based on its level.
			int boundary_west = _land_map.MaxLevelLimitToPosX(nationData.level);
			int boundary_east = _land_map.MaxLevelLimitToPosX(_land_map.GetEasternLevelLimit(nationData.level)) - 1;

			// Determine nation's vertical boundaries based on whether this nation is new or veteran.
			int boundary_north = 0;
			int boundary_south = _land_map.height - 1;
			if (World.new_player_area_boundary != -1)
			{
				if (nationData.veteran || ((userData != null) && (userData.veteran))) {
					boundary_south = World.new_player_area_boundary - 1; // Restrict to veteran area
				}
			}

			// If the range extends beyond the western boundary, shift the range to the east.
			if (x0 < boundary_west)
			{
				x1 += (boundary_west - x0);
				x0 = boundary_west;
			}

			// If the range extends beyond the eastern boundary, shift the range to the west.
			if (x1 > boundary_east)
			{
				x0 -= (x1 - boundary_east);
				x1 = boundary_east;
			}

			// If the range again extends beyond the western boundary, truncate it at the western boundary.
			x0 = Math.max(x0, boundary_west);

			// If the range extends beyond the northern boundary, shift the range to the south.
			if (y0 < boundary_north)
			{
				y1 += (boundary_north - y0);
				y0 = boundary_north;
			}

			// If the range extends beyond the southern boundary, shift the range to the north.
			if (y1 > boundary_south)
			{
				y0 -= (y1 - boundary_south);
				y1 = boundary_south;
			}

			// If the range again extends beyond the northern boundary, truncate it at the northern boundary.
			y0 = Math.max(y0, boundary_north);

			// If the nation isn't being wiped, enforce the nation's max extent.
			if (!_wipe_nation)
			{
				x0 = Math.max(x0, nationData.mainland_footprint.x1 - Constants.NATION_MAX_EXTENT + 1);
				x1 = Math.min(x1, nationData.mainland_footprint.x0 + Constants.NATION_MAX_EXTENT - 1);
				y0 = Math.max(y0, nationData.mainland_footprint.y1 - Constants.NATION_MAX_EXTENT + 1);
				y1 = Math.min(y1, nationData.mainland_footprint.y0 + Constants.NATION_MAX_EXTENT - 1);
			}
		}

		//Output.PrintToScreen("MigrateNation() for center loc " + _centerX + "," + _centerY + ", range from " + x0 + "," + y0 + " to " + x1 + "," + y1);

		// Place the nation randomly near the given center coordinates
		int[] coords = new int[2];
		PlaceNationWithinArea(_land_map, _nationID, x0, y0, x1, y1, -1, -1, _wipe_nation, coords);

		Output.PrintToScreen("*** MigrateNation " + nationData.name + " (" + _nationID + ") " + " center " + _centerX + "," + _centerY + "; _wipe_nation: " + _wipe_nation + ", boundary_west: " + _land_map.MaxLevelLimitToPosX(nationData.level) + ", final area: " + x0 + "," + y0 + " to " + x1 + "," + y1 + "; placed at " + coords[0] + "," + coords[1] + ".");

		if (_wipe_nation)
		{
			// Center the views of all this nation's players on the nation
			Display.CenterViewsOnNation(_nationID, _land_map.ID);
		}
		else
		{
			// Center the user's view on the nation's migrated position.
			Display.CenterViewOnBlock(_userID, coords[0], coords[1]);
		}

		// Broadcast migration event to each of the nation's online players.
		OutputEvents.BroadcastMigrationEvent(_nationID, nationData);

		// Mark the nation's data to be updated
		DataManager.MarkForUpdate(nationData);

		// Post report to nation
		Comm.SendReport(_nationID, ClientString.Get("svr_report_migrated"), 0); // "Our nation has migrated!"
	}

	public static void RemoveNationFromMap(LandMap _land_map, NationData _nationData)
	{
		int nationID = _nationData.ID;

		// Get the nation's footprint in the given map
		Footprint footprint = _nationData.GetFootprint(_land_map.ID);

		// Remove the nation from each block it occupies in the world
		RemoveNationFromArea(_land_map, _nationData, footprint.x0, footprint.y0, footprint.x1, footprint.y1, false);

		// Check for error.
		if (footprint.area != 0)
		{
			Output.PrintToScreen("ERROR: RemoveNationFromMap() removed " + _nationData.name + " (" + _nationData.ID + ") from its footprint (" + footprint.x0 + "," + footprint.y0 + " to " + footprint.x1 + "," + footprint.y1 + ") but its area is now " + footprint.area + ".");
			if (footprint.area > 0)
			{
				Output.PrintToScreen("Will now remove nation " + _nationData.name + " (" + _nationData.ID + ") from the entire map.");
				RemoveNationFromArea(_land_map, _nationData, 0, 0, _land_map.width - 1, _land_map.height - 1, true);
			}
		}

		// Reset the nation's area related data, in case it was somehow corrupted at some point.
		footprint.area = 0;
		footprint.border_area = 0;
		footprint.perimeter = 0;
		footprint.x0 = footprint.y0 = Constants.MAX_MAP_DIM;
		footprint.x1 = footprint.y1 = -1;
	}

	public static void RemoveNationFromArea(LandMap _land_map, NationData _nationData, int _x0, int _y0, int _x1, int _y1, boolean _log)
	{
		for (int y = _y0; y <= _y1; y++)
		{
			for (int x = _x0; x <= _x1; x++)
			{
				if (_land_map.GetBlockNationID(x, y) == _nationData.ID)
				{
					World.SetBlockNationID(_land_map, x, y, -1, true, true, -1, 0); // Broadcast these changes to viewing clients.

					if (_log)
					{
						int nationID_after_removal = _land_map.GetBlockNationID(x, y);
            Constants.WriteToLog("log_RemoveNationFromArea.txt", "RemoveNationFromArea() removed " + _nationData.name + " (" + _nationData.ID + ") from " + x + "," + y + ". Block nationID after removal: " + nationID_after_removal + ". Nation remaining area: " + _nationData.GetFootprint(_land_map.ID).area);
					}
				}
			}
		}
	}

	public static void RemoveNation(int _nationID)
	{
		// Get the nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationID, false);

		// Remove the nation from each block it occupies in the world
		RemoveNationFromMap(DataManager.GetLandMap(Constants.MAINLAND_MAP_ID, false), nationData);

		// Delete the nation
		DeleteNation(nationData);
	}

	public static void DeleteNation(NationData _nationData)
	{
		Output.PrintToScreen("*** DeleteNation(" + _nationData.ID + ")");

		// Break all of this nation's alliances and potential alliances
		Alliance.BreakAllAlliances(_nationData);

		// Remove the former nation from the database, as it has been vanquished.
		DataManager.DeleteData(Constants.DT_NATION, _nationData.ID);
		DataManager.DeleteData(Constants.DT_NATIONTECH, _nationData.ID);
		DataManager.DeleteData(Constants.DT_NATION_EXT, _nationData.ID);
	}

	// Randomly place a new nation or a nation whose area has been reduced to 0, within the area appropriate to its level.
	public static void PlaceNation(NationData _nationData)
	{
		// Get the mainland LandMap.
		LandMap land_map = DataManager.GetLandMap(Constants.MAINLAND_MAP_ID, false);

		// Determine the horizontal bounds of the area appropriate to this nation's level.
		int x0 = land_map.MaxLevelLimitToPosX(_nationData.level);
		int x1 = Math.min(x0 + NATION_PLACEMENT_BAND_MAX_WIDTH, land_map.MaxLevelLimitToPosX(_nationData.level + NATION_PLACEMENT_BAND_LEVELS_WIDE) - 1);

		// Determine the vertical bounds of the area appropriate to whether this nation is new or veteran.
		int y0 = 0;
		int y1 = land_map.height - 1;
		if (new_player_area_boundary != -1)
		{
			if (_nationData.veteran) {
				y1 = new_player_area_boundary - 1; // Restrict to veteran area
			} else {
				y0 = new_player_area_boundary; // Restrict to new player area
			}
		}
		Output.PrintToScreen("PlaceNation() x0: " + x0 + ", y0: " + y0 + ", x1: " + x1 + ", y1: " + y1);

		// Place the nation within the area appropriate to its level.
		PlaceNationWithinArea(land_map, _nationData.ID, x0, y0, x1, y1, -1, -1, true, null);
	}

	// Place the nation nearby to the given coordinates.
	public static void DisplaceNation(LandMap _land_map, NationData _nationData, int _origX, int _origY)
	{
		// First remove the nation from the map.
		RemoveNationFromMap(_land_map, _nationData);

		// Determine bounds of area in which to displace the nation.
		int x0 = _origX - DISPLACE_NATION_RADIUS;
		int y0 = _origY - DISPLACE_NATION_RADIUS;
		int x1 = _origX + DISPLACE_NATION_RADIUS;
		int y1 = _origY + DISPLACE_NATION_RADIUS;

		if (_land_map.ID == Constants.MAINLAND_MAP_ID)
		{
			// Determine nation's horizontal boundaries based on its level.
			int boundary_west = _land_map.MaxLevelLimitToPosX(_nationData.level);
			int boundary_east = _land_map.MaxLevelLimitToPosX(_land_map.GetEasternLevelLimit(_nationData.level)) - 1;

			// If the range extends beyond the western boundary, shift the range to the east.
			if (x0 < boundary_west)
			{
				x1 += (boundary_west - x0);
				x0 = boundary_west;
			}

			// If the range extends beyond the eastern boundary, shift the range to the west.
			if (x1 > boundary_east)
			{
				x0 -= (x1 - boundary_east);
				x1 = boundary_east;
			}

			// If the range again extends beyond the western boundary, truncate it at the western boundary.
			x0 = Math.max(x0, boundary_west);
		}

		// Place the nation at the nearest available location around given origin
		PlaceNationWithinArea(_land_map, _nationData.ID, x0, y0, x1, y1, _origX, _origY, true, null);

    // Center the views of all this nation's players on the nation
    Display.CenterViewsOnNation(_nationData.ID, _land_map.ID);
	}

	// Place the nation within the given area of the given landmap.
	public static void PlaceNationWithinArea(LandMap _land_map, int _nationID, int _x0, int _y0, int _x1, int _y1, int _exclude_x, int _exclude_y, boolean _only_square, int[] _coords)
	{
		int x, y, empty_adjacents, block_flags;
		boolean placed = false;
		BlockExtData block_ext_data;

		// Constrain the placement area to within the land map.
		if (_x0 < 0) _x0 = 0;
		if (_y0 < 0) _y0 = 0;
		if (_x1 >= _land_map.width) _x1 = _land_map.width - 1;
		if (_y1 >= _land_map.height) _y1 = _land_map.height - 1;

		// The given bounding box must be valid.
		if ((_x0 > _x1) || (_y0 > _y1)) {
			return;
		}

		int width = _x1 - _x0 + 1;
		int height = _y1 - _y0 + 1;

		// Get the nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationID, false);

		// Get the nation's footprint on the given map
		Footprint footprint = nationData.GetFootprint(_land_map.ID);

		for (int criteria_index = PLACEMENT_CRITERIA_STRICT; criteria_index <= PLACEMENT_CRITERIA_LOOSE; criteria_index++)
		{
			for (int i = 0; i < NUM_PLACEMENT_ATTEMPTS; i++)
			{
				x = _x0 + Constants.random.nextInt(width);
				y = _y0 + Constants.random.nextInt(height);

				// Do not allow the nation to be placed precisely at the given position to exclude.
				if ((x == _exclude_x) && (y == _exclude_y)) {
					continue;
				}

				// If the coordinates are not flat land terrain, try again. Do not allow beach, because it's harder to see, and a nation isn't allowed on their homeland's beach (so that raiders can make their way around).
				int terrain = _land_map.GetBlockTerrain(x, y);
				if (terrain != Constants.TERRAIN_FLAT_LAND) {
					continue;
				}

				if (criteria_index < PLACEMENT_CRITERIA_LOOSE)
				{
					// If the prospective nation placement block is not empty habitable terrain, try again.
					if (!BlockIsEmpty(_land_map, x, y)) {
						continue;
					}
				}

				block_flags = _land_map.GetBlockFlags(x, y);

				// If the block is on an isolated island, try again.
				if ((block_flags & BlockData.BF_ISLAND) != 0) {
					continue;
				}

				// If the block has a resource object on it, or has a wipe in effect, try again.
				if ((block_flags & BlockData.BF_EXTENDED_DATA) != 0)
				{
					block_ext_data = _land_map.GetBlockExtendedData(x, y, false);
					if ((block_ext_data.objectID >= ObjectData.RESOURCE_OBJECT_BASE_ID) || (block_ext_data.wipe_flags != 0)) {
						continue;
					}
				}

				if (criteria_index < PLACEMENT_CRITERIA_ALLOW_NO_EMPTY_ADJACENTS)
				{
					// Determine how many adjacent blocks are empty
					empty_adjacents = 0;
					if (empty_adjacents < PLACEMENT_MIN_EMPTY_ADJACENTS) empty_adjacents += (BlockIsEmpty(_land_map, x - 1, y - 1) ? 1 : 0);
					if (empty_adjacents < PLACEMENT_MIN_EMPTY_ADJACENTS) empty_adjacents += (BlockIsEmpty(_land_map, x, y - 1) ? 1 : 0);
					if (empty_adjacents < PLACEMENT_MIN_EMPTY_ADJACENTS) empty_adjacents += (BlockIsEmpty(_land_map, x + 1, y - 1) ? 1 : 0);
					if (empty_adjacents < PLACEMENT_MIN_EMPTY_ADJACENTS) empty_adjacents += (BlockIsEmpty(_land_map, x - 1, y) ? 1 : 0);
					if (empty_adjacents < PLACEMENT_MIN_EMPTY_ADJACENTS) empty_adjacents += (BlockIsEmpty(_land_map, x + 1, y) ? 1 : 0);
					if (empty_adjacents < PLACEMENT_MIN_EMPTY_ADJACENTS) empty_adjacents += (BlockIsEmpty(_land_map, x - 1, y + 1) ? 1 : 0);
					if (empty_adjacents < PLACEMENT_MIN_EMPTY_ADJACENTS) empty_adjacents += (BlockIsEmpty(_land_map, x, y + 1) ? 1 : 0);
					if (empty_adjacents < PLACEMENT_MIN_EMPTY_ADJACENTS) empty_adjacents += (BlockIsEmpty(_land_map, x + 1, y + 1) ? 1 : 0);

					// If the minimum number of empty adjacent blocks has not been met, try again.
					if (empty_adjacents < PLACEMENT_MIN_EMPTY_ADJACENTS) {
						continue;
					}
				}

				// Place the first block of the new nation at this position
				SetBlockNationID(_land_map, x, y, _nationID, true, true, -1, 0);

				if (_coords != null)
				{
					// Record placement coords
					_coords[0] = x;
					_coords[1] = y;
				}

				// If this is the nation's first and only square being placed...
				if (_only_square)
				{
					if ((footprint.area != 1) || (footprint.border_area != 1) || (footprint.perimeter != 4)) {
						Output.PrintToScreen("ERROR: PlaceNationWithinArea() after placing nation " + nationData.name + " (" + nationData.ID + "): footprint.area: " + footprint.area + ", footprint.border_area: " + footprint.border_area + ", footprint.perimeter: " + footprint.perimeter);
					}

					// Store the nation's position and area. Do this even though SetBlockNationID() should, to reset to be correct in case an error accrued.
					footprint.x0 = x;
					footprint.x1 = x;
					footprint.y0 = y;
					footprint.y1 = y;
					footprint.area = 1;
					footprint.border_area = 1;
					footprint.perimeter = 4;
				}

				// TEMP
				Output.PrintToScreen("Placed nation " + nationData.name + " (" + _nationID + ") on map " + _land_map.ID + " at location " + x + "," + y + ".");
				//Output.PrintStackTrace();

				DataManager.MarkForUpdate(nationData);

				// Record that the nation has been placed
				placed = true;

				// Break out of the for loop
				break;
			}

			if (placed) {
				break;
			}
		}

		if (!placed) {
			Output.PrintToScreen("ERROR: PlaceNationWithinArea() failed to place nation " + nationData + " (" + nationData.ID + ") within area " + _x0 + "," + _y0 + " to " + _x1 + "," +_y1 + ".");
		}
	}

	// Place the nation on a random beach square in the given map.
	public static void PlaceNationOnBeach(LandMap _land_map, int _nationID, int[] _coords)
	{
		// Get the given landmap's source landmap.
		LandMap source_land_map = _land_map;
		if (_land_map.info.sourceMapID != _land_map.ID) {
			source_land_map = DataManager.GetLandMap(_land_map.info.sourceMapID, false);
		}

		if (source_land_map.info.beachheads_x.size() == 0)
		{
			// There are no beach locations stored for this map; place the nation near the center of the map.
			PlaceNationWithinArea(_land_map, _nationID, (int)(_land_map.width * 0.25f), (int)(_land_map.height * 0.25f), (int)(_land_map.width * 0.75f), (int)(_land_map.height * 0.75f), -1, -1, true, _coords);
			return;
		}

		// Choose randomly among the map's beach positions.
		int place_x = -1, place_y = -1, i;
		float num_loc_candidates = 0f;
		for (i = 0; i < source_land_map.info.beachheads_x.size(); i++)
		{
			if (_land_map.GetBlockNationID(source_land_map.info.beachheads_x.get(i), source_land_map.info.beachheads_y.get(i)) == -1)
			{
				// Increment the count of the number of location candidates that have been found.
				num_loc_candidates += 1f;

				// Randomly choose this locaton if appropriate.
				if (Math.random() < (1f / num_loc_candidates))
				{
					place_x = source_land_map.info.beachheads_x.get(i);
					place_y = source_land_map.info.beachheads_y.get(i);
				}
			}
		}

		// If none of the beach positions are unoccupied, randomly choose one (that is occupied).
		if (place_x == -1)
		{
			i = ThreadLocalRandom.current().nextInt(0, source_land_map.info.beachheads_x.size());
			place_x = source_land_map.info.beachheads_x.get(i);
			place_y = source_land_map.info.beachheads_y.get(i);
		}

		// Place the block of the given nation at the determined position.
		SetBlockNationID(_land_map, place_x, place_y, _nationID, false, false, -1, 0);

		if (_coords != null)
		{
			// Record placement coords
			_coords[0] = place_x;
			_coords[1] = place_y;
		}
	}

	public static boolean BlockIsEmpty(LandMap _land_map, int _x, int _y)
	{
		BlockData block_data = _land_map.GetBlockData(_x, _y);

		if (block_data == null) {
			return false;
		}

		return (((block_data.terrain == Constants.TERRAIN_FLAT_LAND) || (block_data.terrain == Constants.TERRAIN_BEACH)) && (block_data.nationID == -1));
	}

	// Set the given block's nation ID, update the border flags of this block and all surrounding blocks,
	// and update the area of this block's (former and new) nation.
	public static void SetBlockNationID(LandMap _land_map, int _x, int _y, int _nationID, boolean _salvage_builds, boolean _broadcast, int _userID, int _delay)
	{
		float resource_transferred = 0;
		int formerNationID = -1, new_count = 0;
		int former_nation_prev_local_borders = 0, new_nation_prev_local_borders = 0, former_nation_cur_local_borders = 0, new_nation_cur_local_borders = 0;
		int former_nation_prev_local_perimeter = 0, new_nation_prev_local_perimeter = 0, former_nation_cur_local_perimeter = 0, new_nation_cur_local_perimeter = 0;
		NationData formerNationData = null, nationData = null;

		// Do nothing if block is outside of world.
		if ((_x < 0) || (_x >= _land_map.width) || (_y < 0) || (_y >= _land_map.height)) {
			Output.PrintToScreen("Attempt to set Block ID outside map boundaries (" + _land_map.width + "," + _land_map.height + ") at (" + _x + ", " + _y + ") to " + _nationID + ".");
			return;
		}

		// Get the data for the block being changed
		BlockData block_data = DataManager.GetBlockData(_land_map, _x, _y, true); // Mark for update.

		formerNationID = block_data.nationID;

		// If the block's nation ID is already that given, do nothing; return.
		if (formerNationID == _nationID) {
			return;
		}

		// Get current time
		int cur_time = Constants.GetTime();

		// Count the former nation's local blocks that are border, before the change.
		if (formerNationID != -1) {
			former_nation_prev_local_borders = CountLocalBorderBlocks(_land_map, _x, _y, formerNationID);
		}

		// Count the former nation's local perimeter, before the change.
		if (formerNationID != -1) {
			former_nation_prev_local_perimeter = CountLocalPerimeter(_land_map, _x, _y, formerNationID);
		}

		// Count the new nation's local blocks that are border, before the change.
		if (_nationID != -1) {
			new_nation_prev_local_borders = CountLocalBorderBlocks(_land_map, _x, _y, _nationID);
		}

		// Count the new nation's local perimeter, before the change.
		if (_nationID != -1) {
			new_nation_prev_local_perimeter = CountLocalPerimeter(_land_map, _x, _y, _nationID);
		}

		// Record the block's new nation ID.
		block_data.nationID = _nationID;

		// Give the block its full amount of hit points.
		block_data.hit_points_restored_time = -1;

		// Remove the record of the previous attack on the block.
		block_data.attack_complete_time = -1;

		// Get the block's extended data, if it exists.
		BlockExtData block_ext_data = ((block_data.flags & BlockData.BF_EXTENDED_DATA) == 0) ? null : _land_map.GetBlockExtendedData(_x, _y, false);
		BuildData build_data = null;

		// If the block has a build object in it...
		if ((block_ext_data != null) && (block_ext_data.objectID != -1))
		{
			// If the block contains a build object that has crumbled, remove that build object.
			_land_map.CheckForBuildObjectCrumble(_x, _y, block_data, block_ext_data);

			// If the block still has an object in it...
			if (block_ext_data.objectID != -1)
			{
				// Get the object's build data
				build_data = BuildData.GetBuildData(block_ext_data.objectID);

				// Record the time when the block most recently changed nation.
				block_ext_data.capture_time = cur_time;

				if (_salvage_builds && (_nationID == -1) && (block_ext_data.owner_nationID == formerNationID))
				{
					// Before this block is evacuated by its object's owner, salvage the object.
					Gameplay.Salvage(_land_map, _x, _y, block_ext_data, formerNationID);
				}
				else
				{
					// Record the object's capture_time and crumble_time as appropriate.
					if ((block_ext_data.owner_nationID == formerNationID) && (formerNationID != -1) && (build_data.type != BuildData.TYPE_SHARD))
					{
						block_ext_data.crumble_time = cur_time + Constants.TIME_UNTIL_CRUMBLE;
					}
					else if (block_ext_data.owner_nationID == _nationID)
					{
						block_ext_data.crumble_time = -1;
					}
				}
			}
		}

		if (formerNationID > 0)
		{
			// Get the former nation's data
			formerNationData = (NationData)DataManager.GetData(Constants.DT_NATION, formerNationID, false);

			if (formerNationData == null)
			{
				Output.PrintToScreen("ERROR: SetBlockNationID() Former nation data not found. x: " + _x + ", y: " + _y + ", formerNationID: " + formerNationID);
			}
			else
			{
				// Get the former nation's footprint on this map
				Footprint footprint = formerNationData.GetFootprint(_land_map.ID);

				if ((block_ext_data != null) && (block_ext_data.objectID >= ObjectData.RESOURCE_OBJECT_BASE_ID))
				{
					// Remove the block's object from the nation that formerly possessed it
					Objects.RemoveObject(formerNationID, block_ext_data.objectID, _land_map, _x, _y, _nationID, _delay);
				}

				if (footprint != null)
				{
					footprint.area--;

					// Count the former nation's local blocks that are now border, after the change.
					former_nation_cur_local_borders = CountLocalBorderBlocks(_land_map, _x, _y, formerNationID);

					// Count the former nation's local perimeter, after the change.
					former_nation_cur_local_perimeter = CountLocalPerimeter(_land_map, _x, _y, formerNationID);

					// Update the former nation's count of border area.
					footprint.border_area += (former_nation_cur_local_borders - former_nation_prev_local_borders);

					// Update the former nation's count of perimeter.
					footprint.perimeter += (former_nation_cur_local_perimeter - former_nation_prev_local_perimeter);

					// Determine the former nation's new base geographic efficiency.
					formerNationData.DetermineGeographicEfficiency(_land_map.ID);

					if (footprint.area > 0)
					{
						if (_x == footprint.x0) {
							RecomputeNationEdge(formerNationID, _land_map, footprint, footprint.x0, footprint.x1 + 1, 1, footprint.y0, footprint.y1, EDGE_LEFT);
						}

						if (_x == footprint.x1) {
							RecomputeNationEdge(formerNationID, _land_map, footprint, footprint.x1, footprint.x0 - 1, -1, footprint.y0, footprint.y1, EDGE_RIGHT);
						}

						if (_y == footprint.y0) {
							RecomputeNationEdge(formerNationID, _land_map, footprint, footprint.y0, footprint.y1 + 1, 1, footprint.x0, footprint.x1, EDGE_TOP);
						}

						if (_y == footprint.y1) {
							RecomputeNationEdge(formerNationID, _land_map, footprint, footprint.y1, footprint.y0 - 1, -1, footprint.x0, footprint.x1, EDGE_BOTTOM);
						}

						// Update the former nation's footprint's historical extent.
						UpdateHistoricalExtent(formerNationID, footprint, _land_map.ID == Constants.MAINLAND_MAP_ID);
					}

					// Mark the former nation to be updated. Do so here, as well as below, just in case an exception occurs that prevents it being marked below -- so its area will be updated anyway.
					DataManager.MarkForUpdate(formerNationData);
				}

				if (_land_map.ID == Constants.MAINLAND_MAP_ID)
				{
					// Remove this block from the former nation's areas.
					RemoveFromNationArea(_land_map, formerNationData, _x, _y);
				}

				// If the block has a build object that belongs to the block's former owner, remove the object's energy burn rate, capacity, and resource amount from its owner.
				if ((block_ext_data != null) && (block_ext_data.objectID != -1) && (build_data != null) && (formerNationID == block_ext_data.owner_nationID))
				{
					// Determine the structure's energy burn rate for the former nation.
					float energy_burn_rate = formerNationData.DetermineDiscountedEnergyBurn(build_data);

					Gameplay.ModifyEnergyBurnRate(formerNationData, _land_map.ID, -energy_burn_rate);
					resource_transferred = Gameplay.ModifyStatsForObjectCapacity(formerNationData, build_data, true, true);
					OutputEvents.BroadcastStatsEvent(formerNationID, _delay);
					//Output.PrintToScreen("resource_transferred: " + resource_transferred);

					// Update the former owner nation's users' reports for losing this build object.
					if (build_data.type == BuildData.TYPE_WALL) {
						formerNationData.ModifyUserReportValueInt(UserData.ReportVal.report__walls_lost, 1);
					} else {
						formerNationData.ModifyUserReportValueInt(UserData.ReportVal.report__defenses_lost, 1);
					}

					// If this build has a max count, keep track of this nation's count of this build (so long as this is not a raid map).
					if ((_land_map.ID < Raid.RAID_ID_BASE) && (build_data.max_count != -1)) {
						formerNationData.ModifyBuildCount(build_data.ID, -1);
					}
				}

				// If this nation's homeland is being modified, record when it was last modified.
				if (_land_map.ID == formerNationData.homeland_mapID) {
					formerNationData.prev_modify_homeland_time = cur_time;
				}

				// Mark the former nation to be updated
				DataManager.MarkForUpdate(formerNationData);
			}
		}

		if (_nationID > 0)
		{
			// Get the new nation's data
			nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationID, false);

			// Get the nation's footprint on this map
			Footprint footprint = nationData.GetFootprint(_land_map.ID);

			if (footprint != null)
			{
				if (footprint.area == 0)
				{
					// This is the nation's first block; store all of the nation's bounding coordinates and edge counts.
					footprint.x0 = _x;
					footprint.x1 = _x;
					footprint.y0 = _y;
					footprint.y1 = _y;
				}
				else
				{
					// Update the nation's x0 bounding coordinate.
					if (_x < footprint.x0) {
						footprint.x0 = _x;
					}

					// Update the nation's x1 bounding coordinate.
					if (_x > footprint.x1) {
						footprint.x1 = _x;
					}

					// Update the nation's y0 bounding coordinate.
					if (_y < footprint.y0) {
						footprint.y0 = _y;
					}

					// Update the nation's y1 bounding coordinate.
					if (_y > footprint.y1) {
						footprint.y1 = _y;
					}
				}

				// Increase the new nation's area
				footprint.area++;

				// Update the nation's footprint's historical extent.
				UpdateHistoricalExtent(_nationID, footprint, _land_map.ID == Constants.MAINLAND_MAP_ID);

				// Count the new nation's local blocks that are now borders, after the change.
				new_nation_cur_local_borders = CountLocalBorderBlocks(_land_map, _x, _y, _nationID);

				// Count the new nation's local perimeter, after the change.
				new_nation_cur_local_perimeter = CountLocalPerimeter(_land_map, _x, _y, _nationID);

				// Update the new nation's count of border area.
				footprint.border_area += (new_nation_cur_local_borders - new_nation_prev_local_borders);

				// Update the new nation's count of perimeter.
				footprint.perimeter += (new_nation_cur_local_perimeter - new_nation_prev_local_perimeter);

				// Determine the new nation's new base geographic efficiency.
				nationData.DetermineGeographicEfficiency(_land_map.ID);

				// Mark the new nation to be updated. Do so here, as well as below, just in case an exception occurs that prevents it being marked below -- so its area will be updated anyway.
				DataManager.MarkForUpdate(nationData);

				if (_land_map.ID == Constants.MAINLAND_MAP_ID)
				{
					// Increase the nation's record of its max area
					if (footprint.area > nationData.max_area) nationData.max_area = footprint.area;
					if (footprint.area > nationData.max_area_monthly) nationData.max_area_monthly = footprint.area;

					// Update the area ranks
					RanksData.instance.ranks_nation_area.UpdateRanks(_nationID, nationData.name, footprint.area, Constants.NUM_AREA_RANKS, false);
					RanksData.instance.ranks_nation_area_monthly.UpdateRanks(_nationID, nationData.name, footprint.area, Constants.NUM_AREA_RANKS, false);
				}
			}

			if ((block_ext_data != null) && (block_ext_data.objectID >= ObjectData.RESOURCE_OBJECT_BASE_ID))
			{
				// Add the block's object to the nation that has come to possess it
				Objects.AddObject(_nationID, block_ext_data.objectID, _land_map, _x, _y, _userID, formerNationID != -1, _delay);

				// Update the new owner nation's users' reports for gaining this object.
				if (block_ext_data.objectID >= ObjectData.ORB_BASE_ID) {
					nationData.ModifyUserReportValueInt(UserData.ReportVal.report__orb_count_delta, 1);
				} else {
					nationData.ModifyUserReportValueInt(UserData.ReportVal.report__resource_count_delta, 1);
				}
			}

			if (_land_map.ID == Constants.MAINLAND_MAP_ID)
			{
				// Add this block to the new owner nation's areas.
				AddToNationArea(_land_map, nationData, _x, _y);
			}

			// If the block has a build object that belongs to the block's new owner, add the object's energy burn rate, capacity, and resource amount to its owner.
			if ((block_ext_data != null) && (block_ext_data.objectID != -1) && (build_data != null) && (_nationID == block_ext_data.owner_nationID))
			{
				if (((build_data.type == BuildData.TYPE_MANPOWER_STORAGE) || (build_data.type == BuildData.TYPE_ENERGY_STORAGE)) && (nationData.num_share_builds >= Constants.MAX_NUM_SHARE_BUILDS))
				{
					// The recaptured block contains a storage structure, and the nation already has the max number allowed. Salvage the structure immediately.

					// Remove the object from the block.
					block_ext_data.objectID = -1;
					block_ext_data.owner_nationID = -1;

					// Broadcast the salvage event to all local viewing clients.
					OutputEvents.BroadcastSalvageEvent(_land_map, _x, _y);

					// Broadcast message to the nation.
					OutputEvents.BroadcastMessageEvent(_nationID, ClientString.Get("svr_build_max_storage", "max", String.valueOf(Constants.MAX_NUM_SHARE_BUILDS))); // "We've reached the maximum of {max} storage structures."
				}
				else
				{
					// Determine the structure's energy burn rate for the nation.
					float energy_burn_rate = nationData.DetermineDiscountedEnergyBurn(build_data);

					Gameplay.ModifyEnergyBurnRate(nationData, _land_map.ID, energy_burn_rate);
					Gameplay.ModifyStatsForObjectCapacity(nationData, build_data, false, false);

					OutputEvents.BroadcastStatsEvent(_nationID, _delay);

					// Update the new owner nation's users' reports for regaining this build object.
					if (build_data.type == BuildData.TYPE_WALL) {
						nationData.ModifyUserReportValueInt(UserData.ReportVal.report__walls_lost, -1);
					} else {
						nationData.ModifyUserReportValueInt(UserData.ReportVal.report__defenses_lost, -1);
					}

					// If this build has a max count, keep track of this nation's count of this build (so long as this is not a raid map).
					if ((_land_map.ID < Raid.RAID_ID_BASE) && (build_data.max_count != -1)) {
						nationData.ModifyBuildCount(build_data.ID, 1);
					}
				}
			}

			// Update the quests system for the capturing of this block of land
			Quests.HandleCaptureLand(nationData, formerNationID, block_data, block_ext_data, build_data, _delay);

			// If the nation captured a storage structure, add the amount of resource transferred to this nation.
			if ((resource_transferred > 0) && (build_data != null))
			{
				//Output.PrintToScreen("Transferring:"  + resource_transferred);
				if (build_data.type == BuildData.TYPE_MANPOWER_STORAGE)
				{
					resource_transferred = Math.min(resource_transferred * Constants.STORAGE_FRACTION_TRANSFERRED, nationData.GetMainlandManpowerMax() - nationData.GetFootprint(_land_map.ID).manpower);
					nationData.GetFootprint(_land_map.ID).manpower += resource_transferred;

					// Update quests system for the capure of this manpower storage structure
					Quests.HandleManpowerStorageCaptured(nationData, (int)resource_transferred, _delay);

					// Update the former nation's users' reports.
					formerNationData.ModifyUserReportValueFloat(UserData.ReportVal.report__manpower_lost_to_raids, resource_transferred);
				}
				else if (build_data.type == BuildData.TYPE_ENERGY_STORAGE)
				{
					resource_transferred = Math.min(resource_transferred * Constants.STORAGE_FRACTION_TRANSFERRED, nationData.GetFinalEnergyMax() - nationData.energy);
					nationData.energy += resource_transferred;

					// Update quests system for the capure of this energy storage structure
					Quests.HandleEnergyStorageCaptured(nationData, (int)resource_transferred, _delay);

					// Update the former nation's users' reports.
					formerNationData.ModifyUserReportValueFloat(UserData.ReportVal.report__energy_lost_to_raids, resource_transferred);
				}

				// Broadcast the capture storage event to each member of the nation who is logged in.
				OutputEvents.BroadcastCaptureStorageEvent(nationData.ID, _delay, (int)resource_transferred, formerNationData, build_data.ID);

				// Broadcast stats to the capturing nation.
				OutputEvents.BroadcastStatsEvent(_nationID, (int)_delay);
			}

			// If this nation's homeland is being modified, record when it was last modified.
			if (_land_map.ID == nationData.homeland_mapID) {
				nationData.prev_modify_homeland_time = cur_time;
			}

			// Mark the new nation to be updated
			DataManager.MarkForUpdate(nationData);
		}

		// Tell the tournament system about this change in block nationID.
		TournamentData.instance.OnSetBlockNationID(_x, _y, block_data, formerNationData, nationData, _delay);

		// If this took place on a raid map, update the raid system for this change in block nationID.
		if (_land_map.ID >= Raid.RAID_ID_BASE) {
			Raid.OnSetBlockNationID(_land_map, _x, _y, block_data, block_ext_data, formerNationData, nationData, _delay);
		}

		if (_broadcast) {
			// Broadcast this block's change to all users that have this block in their view area.
			OutputEvents.BroadcastBlockUpdateEvent(_land_map, _x, _y, block_data);
		}
	}

	public static int CountLocalBorderBlocks(LandMap _land_map, int _x, int _y, int _nationID)
	{
		return (_land_map.BlockIsNationBorder(_x-1, _y-1, _nationID) ? 1 : 0) +
			     (_land_map.BlockIsNationBorder(_x-1, _y, _nationID) ? 1 : 0) +
			     (_land_map.BlockIsNationBorder(_x-1, _y+1, _nationID) ? 1 : 0) +
			     (_land_map.BlockIsNationBorder(_x, _y-1, _nationID) ? 1 : 0) +
			     (_land_map.BlockIsNationBorder(_x, _y, _nationID) ? 1 : 0) +
			     (_land_map.BlockIsNationBorder(_x, _y+1, _nationID) ? 1 : 0) +
			     (_land_map.BlockIsNationBorder(_x+1, _y-1, _nationID) ? 1 : 0) +
			     (_land_map.BlockIsNationBorder(_x+1, _y, _nationID) ? 1 : 0) +
			     (_land_map.BlockIsNationBorder(_x+1, _y+1, _nationID) ? 1 : 0);
	}

	public static int CountLocalPerimeter(LandMap _land_map, int _x, int _y, int _nationID)
	{
		int local_perimeter = 0;

		boolean center_block_owned = (_land_map.GetBlockNationID(_x, _y) == _nationID);

		if ((_land_map.GetBlockNationID(_x-1, _y) == _nationID) != center_block_owned) local_perimeter++;
		if ((_land_map.GetBlockNationID(_x+1, _y) == _nationID) != center_block_owned) local_perimeter++;
		if ((_land_map.GetBlockNationID(_x, _y-1) == _nationID) != center_block_owned) local_perimeter++;
		if ((_land_map.GetBlockNationID(_x, _y+1) == _nationID) != center_block_owned) local_perimeter++;

		return local_perimeter;
	}


	public static void DetermineArea(LandMap _land_map, NationData _nationData)
	{
		int new_area = 0, new_border_area = 0, new_perimeter = 0;
    int x, y;

		// Get the nation's footprint in the given map
		Footprint footprint = _nationData.GetFootprint(_land_map.ID);

		// If there is no footprint for the nation in this map, do nothing.
		if (footprint == null) {
			return;
		}

		// Get the given nation's ID
		int nationID = _nationData.ID;

		footprint.x0 = footprint.y0 = Constants.MAX_MAP_DIM;
		footprint.x1 = footprint.y1 = -1;
		for (y = 0; y <= _land_map.height; y++)
		{
			for (x = 0; x <= _land_map.width; x++)
			{
				if (_land_map.GetBlockNationID(x, y) == nationID)
				{
					// Increment new_area
					new_area++;

					// Increment border_area if this block is on the nation's border.
					if (_land_map.BlockIsNationBorder(x, y, nationID)) new_border_area++;

					// Increase perimeter for any perimeter around this block.
					new_perimeter += _land_map.DetermineBlockPerimeter(x, y, nationID);

          // Update nation's bounding box

          if (x < footprint.x0) {
            footprint.x0 = x;
          }

          if (x > footprint.x1) {
            footprint.x1 = x;
          }

          if (y < footprint.y0) {
            footprint.y0 = y;
          }

          if (y > footprint.y1) {
            footprint.y1 = y;
          }
				}
			}
		}

		// Store the nation's new area and border_area.
		footprint.area = new_area;
		footprint.border_area = new_border_area;
		footprint.perimeter = new_perimeter;
	}

	public static void RecomputeNationEdge(int _nationID, LandMap _land_map, Footprint _footprint, int _edgeStartValue, int _edgeEndValue, int _edgeIncrement, int _bound0, int _bound1, int _edge_dir)
	{
		int blockNationID;
		boolean edgeFound = false;

		for (int edge = _edgeStartValue; edge != _edgeEndValue; edge += _edgeIncrement)
		{
			// Run along the current edge, counting the number of blocks belonging to this nation.
			for (int pos = _bound0; pos <= _bound1; pos++)
			{
				blockNationID = ((_edge_dir == EDGE_LEFT) || (_edge_dir == EDGE_RIGHT)) ? _land_map.GetBlockNationID(edge, pos) : _land_map.GetBlockNationID(pos, edge);

				if (blockNationID == _nationID)
				{
					edgeFound = true;
					break;
				}
			}

			if (edgeFound)
			{
				// Store the new edge position and count
				switch (_edge_dir)
				{
					case EDGE_LEFT:
						_footprint.x0 = edge;
						break;
					case EDGE_RIGHT:
						_footprint.x1 = edge;
						break;
					case EDGE_TOP:
						_footprint.y0 = edge;
						break;
					case EDGE_BOTTOM:
						_footprint.y1 = edge;
						break;
				}

				return;
			}
		}
	}

	public static void UpdateHistoricalExtent(int _nationID, Footprint _footprint, boolean _broadcast)
	{
		boolean broadcast_change = false;

		if (_footprint.min_x0 == -1)
		{
			// Set initial historical extent.
			_footprint.min_x0 = _footprint.x0;
			_footprint.min_y0 = _footprint.y0;
			_footprint.max_x1 = _footprint.x1;
			_footprint.max_y1 = _footprint.y1;
			_footprint.max_x0 = _footprint.x0;
			broadcast_change = true;
		}
		else
		{
			if (_footprint.x0 < _footprint.min_x0)
			{
				_footprint.min_x0 = _footprint.x0;
				broadcast_change = true;
			}

			if (_footprint.y0 < _footprint.min_y0)
			{
				_footprint.min_y0 = _footprint.y0;
				broadcast_change = true;
			}

			if (_footprint.x1 > _footprint.max_x1)
			{
				_footprint.max_x1 = _footprint.x1;
				broadcast_change = true;
			}

			if (_footprint.y1 > _footprint.max_y1)
			{
				_footprint.max_y1 = _footprint.y1;
				broadcast_change = true;
			}

			if (_footprint.x0 > _footprint.max_x0)
			{
				_footprint.max_x0 = _footprint.x0;
			}
		}

		if (_broadcast && broadcast_change) {
			OutputEvents.BroadcastHistoricalExtentEvent(_nationID, _footprint);
		}
	}

	public static void AddToNationArea(LandMap _land_map, NationData _nationData, int _block_x, int _block_y)
	{
		int i, cur_sqr_dist;
		AreaData cur_area_data;

		// Determine the area grid space of the given block.
		int grid_x = _block_x / Constants.AREA_GRID_SPACING;
		int grid_y = _block_y / Constants.AREA_GRID_SPACING;

		// Determine the given block's squared distance from the center of its area grid space.
		int sqr_dist = GetAreaGridSqrDist(_block_x, _block_y, grid_x, grid_y);

		// Search the nation's areas list for an AreaData representing this grid space, or else where in the list that AreaData should be placed.
		boolean match_found = false;
		for (i = 0; i < _nationData.areas.size(); i++)
		{
			cur_area_data = (AreaData)(_nationData.areas.get(i));

			// If this is the position where an AreaData representing this area grid space should go...
			if ((cur_area_data.gridX > grid_x) || ((cur_area_data.gridX == grid_x) && (cur_area_data.gridY >= grid_y)))
			{
				// If there is already an AreaData here that represents this area grid space...
				if ((cur_area_data.gridX == grid_x) && (cur_area_data.gridY == grid_y))
				{
					// If the given block is closer to the center of this area grid space than its AreaData's current nation block,
					// record this as the new nation block for this AreaData.
					if (sqr_dist < cur_area_data.sqrDist)
					{
						cur_area_data.nationX = _block_x;
						cur_area_data.nationY = _block_y;
						cur_area_data.sqrDist = sqr_dist;
						_nationData.area_visibility_updated = false;
						//Output.PrintToScreen("AddToNationArea(" + _block_x + "," + _block_y + "), grid " + grid_x + "," + grid_y + " replacing area nation block.");
					}

					// A match has been found in the areas list for this area grid space, so no need to add a new AreaData.
					match_found = true;
				}

				// Exit the loop.
				break;
			}
		}

		if (!match_found)
		{
			// Create a new area data to represent the nation's presence in this area grid space.
			cur_area_data = new AreaData();
			cur_area_data.gridX = grid_x;
			cur_area_data.gridY = grid_y;
			cur_area_data.nationX = _block_x;
			cur_area_data.nationY = _block_y;
			cur_area_data.sqrDist = sqr_dist;

			// Insert the new AreaData at the determined position in the nation's areas list.
			_nationData.areas.add(i, cur_area_data);

			// The nation's areas' visibility is no longer up to date and needs to be re-determined.
			_nationData.area_visibility_updated = false;

			//Output.PrintToScreen("AddToNationArea(" + _block_x + "," + _block_y + "), grid " + grid_x + "," + grid_y + " adding new nation area.");
		}
	}

	public static void RemoveFromNationArea(LandMap _land_map, NationData _nationData, int _block_x, int _block_y)
	{
		int i, cur_sqr_dist;
		AreaData cur_area_data = null;

		// Determine the area grid space of the given block.
		int grid_x = _block_x / Constants.AREA_GRID_SPACING;
		int grid_y = _block_y / Constants.AREA_GRID_SPACING;

		// Search the nation's areas list for an AreaData representing this grid space.
		boolean match_found = false;
		for (i = 0; i < _nationData.areas.size(); i++)
		{
			cur_area_data = (AreaData)(_nationData.areas.get(i));

			// If there is already an AreaData here that represents this area grid space...
			if ((cur_area_data.gridX == grid_x) && (cur_area_data.gridY == grid_y))
			{
				// A match has been found in the areas list for this area grid space. Exit loop.
				match_found = true;
				break;
			}
		}

		// If no match was found in the areas list, there's nothing more to do, so return. This should never happen.
		if (match_found == false) {
			return;
		}

		//Output.PrintToScreen("RemoveFromNationArea(" + _block_x + "," + _block_y + "), grid " + grid_x + "," + grid_y + " existing representative block is " + cur_area_data.nationX + "," + cur_area_data.nationY);

		// If it is not the area's representative block for this nation that is being removed, no need to find a replacement. Return.
		if ((cur_area_data.nationX != _block_x) || (cur_area_data.nationY != _block_y)) {
			return;
		}

		boolean replacement_found = false;
		int replacement_block_x = -1, replacement_block_y = -1;
		int replacement_sqr_dist = 100000;

		int block_x0 = grid_x * Constants.AREA_GRID_SPACING;
		int block_y0 = grid_y * Constants.AREA_GRID_SPACING;
		int block_x1 = block_x0 + Constants.AREA_GRID_SPACING - 1;
		int block_y1 = block_y0 + Constants.AREA_GRID_SPACING - 1;

		// If any adjacent block(s), within the same grid square, are owned by the same nation, choose the one closest
		// to the center of the grid square to replace the block that has been removed from the nation's area.

		// _block_x - 1, _block_y
		if ((_block_x > block_x0) && (_land_map.GetBlockNationID(_block_x - 1, _block_y) == _nationData.ID) &&
			  ((cur_sqr_dist = GetAreaGridSqrDist(_block_x - 1, _block_y, grid_x, grid_y)) < replacement_sqr_dist))
		{
			replacement_block_x = _block_x - 1;
			replacement_block_y = _block_y;
			replacement_sqr_dist = cur_sqr_dist;
			replacement_found = true;
		}

		// _block_x - 1, _block_y - 1
		if ((_block_x > block_x0) && (_block_y > block_y0) && (_land_map.GetBlockNationID(_block_x - 1, _block_y - 1) == _nationData.ID) &&
			  ((cur_sqr_dist = GetAreaGridSqrDist(_block_x - 1, _block_y - 1, grid_x, grid_y)) < replacement_sqr_dist))
		{
			replacement_block_x = _block_x - 1;
			replacement_block_y = _block_y - 1;
			replacement_sqr_dist = cur_sqr_dist;
			replacement_found = true;
		}

		// _block_x, _block_y - 1
		if ((_block_y > block_y0) && (_land_map.GetBlockNationID(_block_x, _block_y - 1) == _nationData.ID) &&
			  ((cur_sqr_dist = GetAreaGridSqrDist(_block_x, _block_y - 1, grid_x, grid_y)) < replacement_sqr_dist))
		{
			replacement_block_x = _block_x;
			replacement_block_y = _block_y - 1;
			replacement_sqr_dist = cur_sqr_dist;
			replacement_found = true;
		}

		// _block_x + 1, _block_y - 1
		if ((_block_x < block_x1) && (_block_y > block_y0) && (_land_map.GetBlockNationID(_block_x + 1, _block_y - 1) == _nationData.ID) &&
			  ((cur_sqr_dist = GetAreaGridSqrDist(_block_x + 1, _block_y - 1, grid_x, grid_y)) < replacement_sqr_dist))
		{
			replacement_block_x = _block_x + 1;
			replacement_block_y = _block_y - 1;
			replacement_sqr_dist = cur_sqr_dist;
			replacement_found = true;
		}

		// _block_x + 1, _block_y
		if ((_block_x < block_x1) && (_land_map.GetBlockNationID(_block_x + 1, _block_y) == _nationData.ID) &&
			  ((cur_sqr_dist = GetAreaGridSqrDist(_block_x + 1, _block_y, grid_x, grid_y)) < replacement_sqr_dist))
		{
			replacement_block_x = _block_x + 1;
			replacement_block_y = _block_y;
			replacement_sqr_dist = cur_sqr_dist;
			replacement_found = true;
		}

		// _block_x + 1, _block_y + 1
		if ((_block_x < block_x1) && (_block_y < block_y1) && (_land_map.GetBlockNationID(_block_x + 1, _block_y + 1) == _nationData.ID) &&
			  ((cur_sqr_dist = GetAreaGridSqrDist(_block_x + 1, _block_y + 1, grid_x, grid_y)) < replacement_sqr_dist))
		{
			replacement_block_x = _block_x + 1;
			replacement_block_y = _block_y + 1;
			replacement_sqr_dist = cur_sqr_dist;
			replacement_found = true;
		}

		// _block_x, _block_y + 1
		if ((_block_y < block_y1) && (_land_map.GetBlockNationID(_block_x, _block_y + 1) == _nationData.ID) &&
			  ((cur_sqr_dist = GetAreaGridSqrDist(_block_x, _block_y + 1, grid_x, grid_y)) < replacement_sqr_dist))
		{
			replacement_block_x = _block_x;
			replacement_block_y = _block_y + 1;
			replacement_sqr_dist = cur_sqr_dist;
			replacement_found = true;
		}

		// _block_x - 1, _block_y + 1
		if ((_block_x > block_x0) && (_block_y < block_y1) && (_land_map.GetBlockNationID(_block_x - 1, _block_y + 1) == _nationData.ID) &&
			  ((cur_sqr_dist = GetAreaGridSqrDist(_block_x - 1, _block_y + 1, grid_x, grid_y)) < replacement_sqr_dist))
		{
			replacement_block_x = _block_x - 1;
			replacement_block_y = _block_y + 1;
			replacement_sqr_dist = cur_sqr_dist;
			replacement_found = true;
		}

		//if (replacement_found) Output.PrintToScreen("RemoveFromNationArea(" + _block_x + "," + _block_y + "), grid " + grid_x + "," + grid_y + " found adjacent replacement block " + replacement_block_x + "," + replacement_block_y);

		if (!replacement_found)
		{
			// Starting from the center of the grid square and working out, look for any block that belongs to this nation.
			// The first such block found is the one closest to the center, and will be used as the replacement block for this area.
			int dist, x, y, x0, y0, x1, y1;
			for (dist = 0; dist < Constants.HALF_AREA_GRID_SPACING; dist++)
			{
				x0 = (grid_x * Constants.AREA_GRID_SPACING) + Constants.HALF_AREA_GRID_SPACING - dist - 1;
				x1 = (grid_x * Constants.AREA_GRID_SPACING) + Constants.HALF_AREA_GRID_SPACING + dist;

				// Check top of square
				y = (grid_y * Constants.AREA_GRID_SPACING) + Constants.HALF_AREA_GRID_SPACING - 1 - dist;
				for (x = x0; x <= x1; x++)
				{
					if (_land_map.GetBlockNationID(x, y) == _nationData.ID)
					{
						replacement_block_x = x;
						replacement_block_y = y;
						replacement_sqr_dist = GetAreaGridSqrDist(x, y, grid_x, grid_y);
						replacement_found = true;
						break;
					}
				}

				if (replacement_found) break;

				// Check bottom of square
				y = (grid_y * Constants.AREA_GRID_SPACING) + Constants.HALF_AREA_GRID_SPACING + dist;
				for (x = x0; x <= x1; x++)
				{
					if (_land_map.GetBlockNationID(x, y) == _nationData.ID)
					{
						replacement_block_x = x;
						replacement_block_y = y;
						replacement_sqr_dist = GetAreaGridSqrDist(x, y, grid_x, grid_y);
						replacement_found = true;
						break;
					}
				}

				if (replacement_found) break;

				y0 = (grid_y * Constants.AREA_GRID_SPACING) + Constants.HALF_AREA_GRID_SPACING - dist;
				y1 = (grid_y * Constants.AREA_GRID_SPACING) + Constants.HALF_AREA_GRID_SPACING + dist - 1;

				// Check left side of square
				x = (grid_x * Constants.AREA_GRID_SPACING) + Constants.HALF_AREA_GRID_SPACING - 1 - dist;
				for (y = y0; y <= y1; y++)
				{
					if (_land_map.GetBlockNationID(x, y) == _nationData.ID)
					{
						replacement_block_x = x;
						replacement_block_y = y;
						replacement_sqr_dist = GetAreaGridSqrDist(x, y, grid_x, grid_y);
						replacement_found = true;
						break;
					}
				}

				if (replacement_found) break;

				// Check right side of square
				x = (grid_x * Constants.AREA_GRID_SPACING) + Constants.HALF_AREA_GRID_SPACING + dist;
				for (y = y0; y <= y1; y++)
				{
					if (_land_map.GetBlockNationID(x, y) == _nationData.ID)
					{
						replacement_block_x = x;
						replacement_block_y = y;
						replacement_sqr_dist = GetAreaGridSqrDist(x, y, grid_x, grid_y);
						replacement_found = true;
						break;
					}
				}

				if (replacement_found) break;
			}
		}

		if (replacement_found)
		{
			// A replacement block belonging to this nation has been found in this area grid square. Record its location for this area.
			cur_area_data.nationX = replacement_block_x;
			cur_area_data.nationY = replacement_block_y;
			cur_area_data.sqrDist = replacement_sqr_dist;
			//Output.PrintToScreen("RemoveFromNationArea(" + _block_x + "," + _block_y + "), grid " + grid_x + "," + grid_y + " found replacement block " + replacement_block_x + "," + replacement_block_y);
		}
		else
		{
			// The nation has no more presence within this area's grid square. Remove the corresponding AreaData from the nation's areas list.
			_nationData.areas.remove(i);
			//Output.PrintToScreen("RemoveFromNationArea(" + _block_x + "," + _block_y + "), grid " + grid_x + "," + grid_y + " found no replacement block, removing AreaData.");
		}

		// The nation's areas' visibility is no longer up to date and needs to be re-determined.
		_nationData.area_visibility_updated = false;
	}

	// Update whether each of the nation's areas is far enough from previous areas in the list,
	// so that it will be independently visible when iterating through the nation's areas.
	public static void UpdateAreaVisibility(NationData _nationData)
	{
		AreaData cur_area_data, prev_area_data;

		// Iterate through each of this nation's areas...
		for (int i = 0; i < _nationData.areas.size(); i++)
		{
			cur_area_data = (AreaData)(_nationData.areas.get(i));

			// Reset the current area to being visibile, unless it will be found that it shouldn't be visible, below.
			cur_area_data.visible = true;

			// Iterate through each area prior in the list to the current area...
			for (int j = i - 1; j >= 0; j--)
			{
				prev_area_data = (AreaData)(_nationData.areas.get(j));

				// Only check previous areas that are in the same, or the previous, grid x positon as is the current area.
				if (prev_area_data.gridX < (cur_area_data.gridX - 1)) {
					break;
				}

				// Skip previous areas that are further than 1 grid space away from the current area; they can't be close enough to cause the current area to not be visible.
				if ((prev_area_data.gridY < (cur_area_data.gridY - 1)) || (prev_area_data.gridY > (cur_area_data.gridY + 1))) {
					continue;
				}

				// If the current area's block is less than one full grid spacing distance from the previous area's block, the current area should not be independently visible.
				if (GetBlocksSqrDist(cur_area_data.nationX, cur_area_data.nationY, prev_area_data.nationX, prev_area_data.nationY) < Constants.AREA_GRID_SPACING_SQUARE_DISTANCE)
				{
					cur_area_data.visible = false;
					break;
				}
			}
		}

		// Record that the given nation's area visibility has ben updated.
		_nationData.area_visibility_updated = true;
	}

	public static void ListNationAreas(NationData _nationData)
	{
		AreaData cur_area_data;

		Output.PrintToScreen("Nation " + _nationData.name + " (" + _nationData.ID + ") Areas:");

		for (int i = 0; i < _nationData.areas.size(); i++)
		{
			cur_area_data = (AreaData)(_nationData.areas.get(i));
			Output.PrintToScreen("   " + i + ") " + cur_area_data.nationX + "," + cur_area_data.nationY);
		}
	}

	public static void SetUserViewToNextArea(UserData _userData, StringBuffer _output_buffer)
	{
		AreaData cur_area_data, first_visible_area_data = null, selected_area_data = null;

		// Begin by selecting the first visible area.
		boolean select_next_visible_area = true;

		// Get the user's nation's data.
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _userData.nationID, false);

		// Update the visibility of each of this nation's areas, if it is not up to date.
		if (nationData.area_visibility_updated == false) {
			//Output.PrintToScreen("About to update area visibility.");
			UpdateAreaVisibility(nationData);
		}

		// Iterate through each of this nation's areas...
		for (int i = 0; i < nationData.areas.size(); i++)
		{
			cur_area_data = (AreaData)(nationData.areas.get(i));

			// Skip the current area, if it is so close to an earlier area that it is marked not to be visible.
			if (cur_area_data.visible == false) {
				//Output.PrintToScreen("Area " + i + " not visible, skipping.");
				continue;
			}

			// Keep track of the first visible area, in case we need to loop back to the beginning.
			if (first_visible_area_data == null) {
				first_visible_area_data = cur_area_data;
			}

			// If the next visibile area is to be selected, select this current visible area.
			if (select_next_visible_area)
			{
				//Output.PrintToScreen("Area " + i + " being selected.");
				selected_area_data = cur_area_data;
				select_next_visible_area = false;
			}

			// If the player is currently viewing near to the current area, then select the next area after this one.
			if (GetBlocksSqrDist(cur_area_data.nationX, cur_area_data.nationY, _userData.viewX, _userData.viewY) < Constants.AREA_GRID_SPACING_SQUARE_DISTANCE) {
				//Output.PrintToScreen("Area " + i + " close to current view, so will select next area.");
				select_next_visible_area = true;
			}
		}

		// If we've ended the loop with select_next_visible_area set, we need to loop back to select the first visible area.
		if (select_next_visible_area) {
			selected_area_data = first_visible_area_data;
		}

		// If an area has been selected, pan the player's view over to that area.
		if (selected_area_data != null) {
			//Output.PrintToScreen("Setting view to next area.");
			Display.SetUserView(_userData, selected_area_data.nationX, selected_area_data.nationY, true, _output_buffer);
		}
	}

	// Get the square distance between the given block and the center of the given area grid square.
	public static int GetAreaGridSqrDist(int _block_x, int _block_y, int _grid_x, int _grid_y)
	{
		int x_dist = _block_x - (_grid_x * Constants.AREA_GRID_SPACING + Constants.HALF_AREA_GRID_SPACING);
		int y_dist = _block_y - (_grid_y * Constants.AREA_GRID_SPACING + Constants.HALF_AREA_GRID_SPACING);
		return (x_dist * x_dist) + (y_dist * y_dist);
	}

	public static int GetBlocksSqrDist(int _block_1_x, int _block_1_y, int _block_2_x, int _block_2_y)
	{
		int x_dist = _block_1_x - _block_2_x;
		int y_dist = _block_1_y - _block_2_y;
		return (x_dist * x_dist) + (y_dist * y_dist);
	}
}
