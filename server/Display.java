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
import java.lang.*;
import java.awt.image.*;
import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.Color;
import java.util.*;
import WOCServer.*;

public class Display
{
	static int MAP_BUFFER_LENGTH = 32768;//8192;
	static StringBuffer map_string = new StringBuffer(MAP_BUFFER_LENGTH);
	static StringBuffer nations_string = new StringBuffer(MAP_BUFFER_LENGTH);
	static StringBuffer nationIDs_string = new StringBuffer(MAP_BUFFER_LENGTH);
	static StringBuffer extended_data_string = new StringBuffer(MAP_BUFFER_LENGTH);
	static Semaphore semaphore_MapEvent = new Semaphore();

	static int CONST_X = 0;
	static int CONST_Y = 1;

	public static void SendFullMapEventToNation(int _nationID)
	{
		// If none of the given nation's member are online, do nothing.
		if (WOCServer.nation_table.containsKey(_nationID) == false) {
			return;
		}

		// Get the record for this nation, if any of its users are online.
		WOCServer.NationRecord nation_record = WOCServer.nation_table.get(_nationID);

		if (nation_record == null) {
			return;
		}

		// Iterate through each of this nation's online users...
		for (Map.Entry<Integer,ClientThread> user_entry : nation_record.users.entrySet())
		{
			// Acquire and clear the output buffer
			WOCServer.temp_output_buffer_semaphore.acquire();
			WOCServer.temp_output_buffer.setLength(0);

			// Create a full map event for this online user.
			GetFullMapEvent(WOCServer.temp_output_buffer, user_entry.getKey(), false);

			// Terminate event string and send to client.
			ClientThread cThread = (ClientThread)(user_entry.getValue());
			cThread.TerminateAndSendNow(WOCServer.temp_output_buffer);

			// Release the output buffer
			WOCServer.temp_output_buffer_semaphore.release();
		}
	}

	public static void GetFullMapEvent(StringBuffer _output_buffer, int _userID, boolean _pan_view)
	{
	  // Get the user's data
	  UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

	  GetFullMapEvent(_output_buffer, userData, _pan_view);
	}

	public static void GetFullMapEvent(StringBuffer _output_buffer, UserData _userData, boolean _pan_view)
	{
		// Get the land map's data
		LandMap land_map = DataManager.GetLandMap(_userData.mapID, false);

		//Output.PrintToScreen("GetFullMapEvent() user " + _userData.ID + ", viewChunkX0: " +  _userData.viewChunkX0 + ", viewChunkY0: " +  _userData.viewChunkY0 + ", _pan_view: " + _pan_view);

		// Determine area in blocks to send to the client for a full map event.
		// A 4x4 area of chunks of width DISPLAY_CHUNK_SIZE are sent. A 1-block perimeter around this area is also sent; it is used to determine height map at edges.
		int x0 = _userData.viewChunkX0 * Constants.DISPLAY_CHUNK_SIZE - 1;
		int x1 = (_userData.viewChunkX0 + Constants.VIEW_AREA_CHUNKS_WIDE) * Constants.DISPLAY_CHUNK_SIZE;
		int y0 = _userData.viewChunkY0 * Constants.DISPLAY_CHUNK_SIZE - 1;
		int y1 = (_userData.viewChunkY0 + Constants.VIEW_AREA_CHUNKS_WIDE) * Constants.DISPLAY_CHUNK_SIZE;

		// Get the map event for the determined area.
		GetMapEvent(_output_buffer, _userData, land_map, x0, y0, x1, y1, _pan_view, true, false);
	}

	public static void GetMapEvent(StringBuffer _output_buffer, UserData _userData, LandMap _land_map, int _x0, int _y0, int _x1, int _y1, boolean _pan_view, boolean _initial_map, boolean _replay)
	{
		//Output.PrintToScreen("GetMapEvent() begins, _initial_map: " + _initial_map); // TESTING
		//Output.PrintStackTrace(); // TESTING

		//// TEMPORARY -- for trying to find bug that occassionally sends bad map data to client, but stops doing it if server is restarted.
		//boolean log_debug = ((WOCServer.log_flags & Constants.LOG_DEBUG) != 0);

		// Clip the given area to be within the bounds of the given LandMap.
		_x0 = Math.max(0, Math.min(_land_map.width - 1, _x0));
		_x1 = Math.max(0, Math.min(_land_map.width - 1, _x1));
		_y0 = Math.max(0, Math.min(_land_map.height - 1, _y0));
		_y1 = Math.max(0, Math.min(_land_map.height - 1, _y1));

		// If there is no area to create the map event for, do nothing.
		if (((_x1 - _x0) == 0) || ((_y1 - _y0) == 0)) {
			return; // Only allow return before the semaphore is acquired.
		}

		// Acquire the map event semaphore, to protect the buffers in case multiple threads try to create a map event at one time.
		semaphore_MapEvent.acquire();

		// Initialize variables
		map_string.delete(0, MAP_BUFFER_LENGTH);
		nations_string.delete(0, MAP_BUFFER_LENGTH);
		nationIDs_string.delete(0, MAP_BUFFER_LENGTH);
		extended_data_string.delete(0, MAP_BUFFER_LENGTH);
		int num_nations = 0;
		HashMap<Integer,Boolean> nations = new HashMap<Integer,Boolean>();

		BlockData block_data;
		BlockExtData block_ext_data;
		NationData nationData;
		int x, y, block_type, hit_points, objectID, nation_flags, cur_nationID, running_nationID = -1, running_nationID_count = 0, extended_data_count = 0;
		int cur_time = Constants.GetTime();

		for (y = _y0; y <= _y1; y++)
		{
			for (x = _x0; x <= _x1; x++)
			{
				// Get the block's data
				block_data = DataManager.GetBlockData(_land_map, x, y, false);

				// If an entry doesn't already exist for this nation, add one.
				if ((block_data.nationID != -1) && (nations.get(block_data.nationID) == null))
				{
					// Get the nation's data
					nationData = (NationData)DataManager.GetData(Constants.DT_NATION, block_data.nationID, false);

					if (nationData != null) // SHOULD never be null...
					{
						// Create entry for this nation
						nations.put(block_data.nationID, true);

						// Encode the nation's data
						Constants.EncodeNumber(nations_string, nationData.ID, 4);
						EncodeNationData(nations_string, nationData);

						// Increment the number of nations
						num_nations++;
					}
					else
					{
						// Nation doesn't exist; remove its ID from this block.
						block_data.nationID = -1;
						DataManager.MarkForUpdate(block_data);
					}
				}

				// Compress runs of nationIDs into the nationIDs_string
				cur_nationID = block_data.nationID;
				if (block_data.nationID == running_nationID)
				{
					running_nationID_count++;
				}
				else
				{
					if (running_nationID_count > 0)
					{
						if (running_nationID_count > 1) {
							//if (log_debug) Output.PrintToScreen("block " + x + "," + y + " encoding running_nationID_count " + running_nationID_count); // TEMP
							Constants.EncodeNumber(nationIDs_string, -running_nationID_count, 4);
						}
						//if (log_debug) Output.PrintToScreen("block " + x + "," + y + " encoding running_nationID " + running_nationID); // TEMP
						Constants.EncodeNumber(nationIDs_string, running_nationID, 4);
					}

					running_nationID_count = 1;
					running_nationID = cur_nationID;
				}

				if ((block_data.flags & BlockData.BF_EXTENDED_DATA) != 0)
				{
					block_ext_data = _land_map.GetBlockExtendedData(x, y, false);

					if ((block_ext_data.objectID != -1) && (block_ext_data.owner_nationID != block_data.nationID))
					{
						// If the block contains a build object that has crumbled, remove that build object.
						_land_map.CheckForBuildObjectCrumble(x, y, block_data, block_ext_data);
					}

					// Add this block's extended data to the extended_data_string.
					Constants.EncodeUnsignedNumber(extended_data_string, x, 4);
					Constants.EncodeUnsignedNumber(extended_data_string, y, 4);
					Constants.EncodeNumber(extended_data_string, block_ext_data.objectID, 2);
					Constants.EncodeNumber(extended_data_string, block_ext_data.owner_nationID, 4);
					Constants.EncodeNumber(extended_data_string, (block_ext_data.creation_time == -1) ? Constants.LARGE_NEGATIVE_TIME : (block_ext_data.creation_time - cur_time), 5);
					Constants.EncodeNumber(extended_data_string, (block_ext_data.completion_time == -1) ? -1 : ((block_ext_data.completion_time < cur_time) ? -2 : (block_ext_data.completion_time - cur_time)), 4);
					Constants.EncodeNumber(extended_data_string, (block_ext_data.invisible_time == -1) ? -1 : ((block_ext_data.invisible_time < cur_time) ? -2 : (block_ext_data.invisible_time - cur_time)), 4);
					Constants.EncodeNumber(extended_data_string, (block_ext_data.capture_time == -1) ? Constants.LARGE_NEGATIVE_TIME : (block_ext_data.capture_time - cur_time), 5);
					Constants.EncodeNumber(extended_data_string, (block_ext_data.crumble_time == -1) ? -1 : ((block_ext_data.crumble_time < cur_time) ? -2 : (block_ext_data.crumble_time - cur_time)), 4);
					Constants.EncodeNumber(extended_data_string, block_ext_data.wipe_nationID, 4);
					Constants.EncodeNumber(extended_data_string, (block_ext_data.wipe_end_time == -1) ? -1 : ((block_ext_data.wipe_end_time < cur_time) ? -2 : (block_ext_data.wipe_end_time - cur_time)), 4);
					Constants.EncodeUnsignedNumber(extended_data_string, block_ext_data.wipe_flags, 1);

					// Increment the count of how many blocks' extended data are being sent.
					extended_data_count++;
				}
			}
		}

		// Add final nation ID to nationIDs_string
		if (running_nationID_count > 1) {
			//if (log_debug) Output.PrintToScreen("Final block encoding running_nationID_count " + running_nationID_count); // TEMP
			Constants.EncodeNumber(nationIDs_string, -running_nationID_count, 4);
		}
		//if (log_debug) Output.PrintToScreen("Final block encoding running_nationID " + running_nationID); // TEMP
		Constants.EncodeNumber(nationIDs_string, running_nationID, 4);

		// If this is an initial, full map event, and if the user's nation isn't included in it, add the data about the user's nation.
		// The client should always have the user's nation's data.
		if (_initial_map && (nations.get(_userData.nationID) == null))
		{
			// Get the user's nation's data
			nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _userData.nationID, false);

			// Encode the user's nation's data
			Constants.EncodeNumber(nations_string, nationData.ID, 4);
			EncodeNationData(nations_string, nationData);

			// Increment the number of nations
			num_nations++;
		}

		// Encode event
		Constants.EncodeString(_output_buffer, "event_map");
		Constants.EncodeUnsignedNumber(_output_buffer, _land_map.ID, 6);
		Constants.EncodeUnsignedNumber(_output_buffer, _land_map.info.sourceMapID, 2);
		Constants.EncodeUnsignedNumber(_output_buffer, _land_map.info.skin, 2);
		Constants.EncodeUnsignedNumber(_output_buffer, _land_map.width, 3);
		Constants.EncodeUnsignedNumber(_output_buffer, _land_map.height, 3);
		Constants.EncodeUnsignedNumber(_output_buffer, _replay ? 1 : 0, 1);
		Constants.EncodeUnsignedNumber(_output_buffer, _pan_view ? 1 : 0, 1);
		Constants.EncodeNumber(_output_buffer, _userData.viewX, 3);
		Constants.EncodeNumber(_output_buffer, _userData.viewY, 3);
		Constants.EncodeNumber(_output_buffer, _x0, 3);
		Constants.EncodeNumber(_output_buffer, _y0, 3);
		Constants.EncodeNumber(_output_buffer, _x1, 3);
		Constants.EncodeNumber(_output_buffer, _y1, 3);
		Constants.EncodeUnsignedNumber(_output_buffer, num_nations, 2);
		_output_buffer.append(nations_string);
		_output_buffer.append(map_string);
		_output_buffer.append(nationIDs_string);
		Constants.EncodeUnsignedNumber(_output_buffer, extended_data_count, 2);
		_output_buffer.append(extended_data_string);

		//Output.PrintToScreen("Map event for area " + _x0 + "," + _y0 + " to " + _x1 + "," + _y1 + " (" + ((_x1-_x0+1) * (_y1-_y0+1)) + " blocks). viewX: " + _userData.viewX + ", viewY: " + _userData.viewY + ", Output buffer length: " + _output_buffer.length() + ", capacity: " + _output_buffer.capacity());

		// Clear the nations HashMap
		nations.clear();

		// Release the map event semaphore
		semaphore_MapEvent.release();
	}

	public static void EncodeNationData(StringBuffer _buffer, NationData _nationData)
	{
		// Determine nation flags
		int nation_flags = _nationData.flags;
		if (_nationData.num_members_online > 0)	{
			nation_flags |= Constants.NF_ONLINE;
		}
		if (_nationData.tournament_active && (_nationData.tournament_start_day == TournamentData.instance.start_day)) {
			nation_flags |= Constants.NF_TOURNAMENT_CONTENDER;
		}

		// Encode the nation's data
		Constants.EncodeUnsignedNumber(_buffer, nation_flags, 2);
		Constants.EncodeString(_buffer, _nationData.name);
		//Constants.EncodeNumber(_buffer, _nationData.area, 3);
		Constants.EncodeUnsignedNumber(_buffer, _nationData.r, 2);
		Constants.EncodeUnsignedNumber(_buffer, _nationData.g, 2);
		Constants.EncodeUnsignedNumber(_buffer, _nationData.b, 2);
		Constants.EncodeNumber(_buffer, _nationData.emblem_index, 2);
		Constants.EncodeUnsignedNumber(_buffer, _nationData.emblem_color, 1);
		Constants.EncodeUnsignedNumber(_buffer, (int)(_nationData.shared_energy_fill * 100), 2);
		Constants.EncodeUnsignedNumber(_buffer, (int)(_nationData.shared_manpower_fill * 100), 2);
	}

	public static void GetPanViewEvent(StringBuffer _output_buffer, int _viewX, int _viewY)
	{
		Constants.EncodeString(_output_buffer, "event_pan_view");
		Constants.EncodeNumber(_output_buffer, _viewX, 3);
		Constants.EncodeNumber(_output_buffer, _viewY, 3);
	}

	// Center the given user's view on the nation with the given name.
	public static void CenterOnNation(StringBuffer _output_buffer, int _userID, String _center_nation_name)
	{
		// Get the nation's ID based on it's name
		int nationID = DataManager.GetNameToIDMap(Constants.DT_NATION, _center_nation_name);

		if (nationID == -1)
		{
			// Return message
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_msg_no_such_nation", "nation_name", _center_nation_name)); // "There is no nation named " + _center_nation_name
			return;
		}

		// Center the user's view on the given nation
		CenterViewOnNation(_userID, nationID);
	}

	// Center the view of each of the nation's users on the nation's first area, regardless of whether the user is online.
  public static void CenterViewsOnNation(int _nationID, int _landmapID)
  {
		// Get the nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationID, false);

 		UserData userData;
		for (int user_index = 0; user_index < nationData.users.size(); user_index++)
		{
			// Get the current member user's data
			userData = (UserData)DataManager.GetData(Constants.DT_USER, nationData.users.get(user_index), false);

			if (userData.mapID == _landmapID)
			{
				// Center the user's view on the nation. Center their current view if they're logged in, otherwise center their stored view.
				if (WOCServer.IsUserLoggedIn(userData.ID, _nationID)) {
					Display.CenterViewOnNation(userData.ID, _nationID);
				} else {
					Display.CenterStoredViewOnNation(userData.ID, _nationID);
				}

				// Mark the user's data to be updated
				DataManager.MarkForUpdate(userData);
			}
		}
  }

	// Center the given user's view on their nation's first area, regardless of whether they are online.
	public static void CenterViewOnNation(int _userID, int _nationID)
	{
	  NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationID, false);

		// If nation's data not found, return.
		if (nationData == null) {
			return;
		}

		if (nationData.areas.size() == 0)
		{
			Output.PrintToScreen("CenterViewOnNation() called for nation with ID " + _nationID + " that has no areas.");
			return;
		}

		// Get the nation's first area's record.
		AreaData first_area_data = (AreaData)(nationData.areas.get(0));

		// Set the user's view to the representative block of the nation's first area.
		CenterViewOnBlock(_userID, first_area_data.nationX, first_area_data.nationY);
	}

	// Center the given user's stored view on their nation's first area, regardless of whether they are online. Do not update their current view.
	public static void CenterStoredViewOnNation(int _userID, int _nationID)
	{
	  NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationID, false);

		// If nation's data not found, return.
		if (nationData == null) {
			return;
		}

		if (nationData.areas.size() == 0)
		{
			Output.PrintToScreen("CenterStoredViewOnNation() called for nation with ID " + _nationID + " that has no areas.");
			return;
		}

		// Get the nation's first area's record.
		AreaData first_area_data = (AreaData)(nationData.areas.get(0));

		// Get the user's data
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		// Set the user's stored view to the representative block of the nation's first area.
		SetUserStoredView(userData, Constants.MAINLAND_MAP_ID, first_area_data.nationX, first_area_data.nationY);
	}

	// Set the user's view position in their current map, regardless of whether they're online.
	public static void CenterViewOnBlock(int _userID, int _centerX, int _centerY)
	{
		// Get the user's data
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		// Get this user's client thread, if they're logged in.
		ClientThread client_thread = WOCServer.GetClientThread(_userID, userData.nationID);

		if (client_thread != null)
		{
			// Acquire and clear the output buffer
			WOCServer.temp_output_buffer_semaphore.acquire();
			WOCServer.temp_output_buffer.setLength(0);

			// Change the user's view, and output the change to the user's client.
			Display.SetUserView(userData, _centerX, _centerY, true, WOCServer.temp_output_buffer);

			// Terminate event string and send to client.
			client_thread.TerminateAndSendNow(WOCServer.temp_output_buffer);

			// Release the output buffer
			WOCServer.temp_output_buffer_semaphore.release();
		}
		else
		{
			// Change the user's view, and do not output the change because the user isn't logged in.
			Display.SetUserView(userData, _centerX, _centerY, true, null);
		}

		//Output.PrintToScreen("CenterViewOnBlock() set user " + userData.ID + "'s viewX: " + userData.viewX + ", viewY: " + userData.viewY + ", viewChunkX0: " + userData.viewChunkX0 + ", viewChunkY0: " + userData.viewChunkY0);

		// Mark the user data to be updated
		DataManager.MarkForUpdate(userData);
	}

	// Set the logged-in user's view position in their current map, and output to the given buffer.
	public static void CenterViewOnBlock(ClientThread _clientThread, StringBuffer _output_buffer, int _centerX, int _centerY)
	{
		// Get the user's data
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _clientThread.GetUserID(), false);

		// Change the user's view, and output the change to the user's client.
		Display.SetUserView(userData, _centerX, _centerY, true, _output_buffer);

		// Mark the user data to be updated
		DataManager.MarkForUpdate(userData);
	}

	public static void SetUserViewForMap(UserData _userData, StringBuffer _output_buffer)
	{
		int viewX, viewY;

		// Get the user's current land map.
		LandMap map = DataManager.GetLandMap(_userData.mapID, false);

		//Output.PrintToScreen("SetUserViewForMap() user " + _userData.name + " mapID: " + _userData.mapID + ": " + map);

		if (map == null)
		{
			Output.PrintToScreen("ERROR: SetUserViewForMap() for user " + _userData.name + " (" + _userData.ID + "), LandMap with ID " + _userData.mapID + " does not exist.");
			return;
		}

		if (_userData.mapID == Constants.MAINLAND_MAP_ID)
		{
			// The user's current map is the mainland map. Set the user's view to its mainland view position.
			viewX = _userData.mainland_viewX;
			viewY = _userData.mainland_viewY;
		}
		else if (_userData.mapID >= Raid.RAID_ID_BASE)
		{
			// The user's current map is a raid map. Set the user's view to its raidland view position.
			viewX = _userData.raidland_viewX;
			viewY = _userData.raidland_viewY;
		}
		else
		{
			NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _userData.nationID, false);

			if (_userData.mapID == nationData.homeland_mapID)
			{
				// The user's current map is its nation's homeland map. Set the user's view to its homeland view position.
				viewX = _userData.homeland_viewX;
				viewY = _userData.homeland_viewY;
			}
			else
			{
				// Set the user's view to the default view position for the current map.
				viewX = map.width / 2;  // TEMP placing user in middle of map
				viewY = map.height / 2;  // TEMP placing user in middle of map
			}
		}

		// Constrain the user's view to be within the map. This is especially important in the case that the map has been resized to a smaller size.
		viewX = Math.max(0, Math.min(viewX, map.width - 1));
		viewY = Math.max(0, Math.min(viewY, map.height - 1));

		// Set the user's view to the determined position.
		SetUserView(_userData, viewX, viewY, true, _output_buffer);
	}

	// Set the user's view position in their current map. If given an _output_buffer for an online user, will generate message to update client's view.
	public static void SetUserView(UserData _userData, int _viewX, int _viewY, boolean _pan_view, StringBuffer _output_buffer)
	{
		int x, y;

		// Get the user's current map.
		LandMap land_map = DataManager.GetLandMap(_userData.mapID, false);

		// Get the user's nation's data.
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _userData.nationID, false);

		// Get the nation's footprint in this landmap.
		Footprint fp = nationData.GetFootprint(_userData.mapID);

		// Constrain the view position to be within the map.
		_viewX = Math.max(_viewX, 0);
		_viewY = Math.max(_viewY, 0);
		_viewX = Math.min(_viewX, land_map.width - 1);
		_viewY = Math.min(_viewY, land_map.height - 1);

		// If the user is not an admin, constrain their view to a limited range beyond the nation's historical extent (for mainland).
		if ((_userData.admin == false) && (_userData.mapID == Constants.MAINLAND_MAP_ID))
		{
			_viewX = Math.max(_viewX, fp.min_x0 - Constants.EXTRA_VIEW_RANGE);
			_viewX = Math.min(_viewX, fp.max_x1 + Constants.EXTRA_VIEW_RANGE);
			_viewY = Math.max(_viewY, fp.min_y0 - Constants.EXTRA_VIEW_RANGE);
			_viewY = Math.min(_viewY, fp.max_y1 + Constants.EXTRA_VIEW_RANGE);
		}

		// Record the user's view position for their current map.
		SetUserStoredView(_userData, _userData.mapID, _viewX, _viewY);

		int viewChunkX = (_viewX / Constants.DISPLAY_CHUNK_SIZE) - ((_viewX < 0) ? 1 : 0);
		int viewChunkY = (_viewY / Constants.DISPLAY_CHUNK_SIZE) - ((_viewY < 0) ? 1 : 0);

		// Determine whether this user is just starting to view this map.
		boolean user_is_new_to_map = (_userData.viewChunkX0 == Integer.MIN_VALUE);

		// Determine the new view area origin.
		// The view center is kept within chunks 2,3 through 3,4 relative to the view origin. This is off-center because a greater distance is visible on the far sides of both axes.
		int newViewChunkX0, newViewChunkY0;
		if (user_is_new_to_map) {
			// The view is not yet positioned on this map.
			// Reset view area origin so that view is centered in chunk 2,3.
			newViewChunkX0 = viewChunkX - 2;
			newViewChunkY0 = viewChunkY - 3;
		} else {
			// If necessary, adjust view area origin so that view is centered within chunks 2,3 through 3,4.
			newViewChunkX0 = ((viewChunkX - _userData.viewChunkX0) < 2) ? (viewChunkX - 2) : (((viewChunkX - _userData.viewChunkX0) > 3) ? (viewChunkX - 3) : _userData.viewChunkX0);
			newViewChunkY0 = ((viewChunkY - _userData.viewChunkY0) < 3) ? (viewChunkY - 3) : (((viewChunkY - _userData.viewChunkY0) > 4) ? (viewChunkY - 4) : _userData.viewChunkY0);
		}

		// Determine whether the new view is non-overlapping with the previous view.
		boolean new_view_nonoverlaps = (!user_is_new_to_map) && ((newViewChunkX0 >= (_userData.viewChunkX0 + Constants.VIEW_AREA_CHUNKS_WIDE)) || (newViewChunkY0 >= (_userData.viewChunkY0 + Constants.VIEW_AREA_CHUNKS_WIDE)) || (_userData.viewChunkX0 >= (newViewChunkX0 + Constants.VIEW_AREA_CHUNKS_WIDE)) || (_userData.viewChunkY0 >= (newViewChunkY0 + Constants.VIEW_AREA_CHUNKS_WIDE)));

		//Output.PrintToScreen("For viewChunkX: " + viewChunkX + ", viewChunkY: " + viewChunkY + ", newViewChunkX0: " + newViewChunkX0 + ", newViewChunkY0: " + newViewChunkY0);

		//// Determine the new upper right corner of the area of chunks that the user's view will encompass.
		//int newViewChunkX0 = (_viewX - (viewCenterChunkX * Constants.DISPLAY_CHUNK_SIZE)) < (Constants.DISPLAY_CHUNK_SIZE / 2) ? (viewCenterChunkX - 2) : (viewCenterChunkX - 1);
		//int newViewChunkY0 = (_viewY - (viewCenterChunkY * Constants.DISPLAY_CHUNK_SIZE)) < (Constants.DISPLAY_CHUNK_SIZE / 2) ? (viewCenterChunkY - 2) : (viewCenterChunkY - 1);

		// Record the user's new view position.
		_userData.viewX = _viewX;
		_userData.viewY = _viewY;

		// If the user is logged in, update the viewer lists of the blocks that the user's client has started and stopped viewing.
		if (_userData.client_thread != null)
		{
			// Update the view lists of this land map's chunks, for the change in position of this user's view.
			if (user_is_new_to_map || new_view_nonoverlaps)
			{
				// Either the user has no previous view on this map, or else its previous view has no overlap with its new view.

				if ((!user_is_new_to_map) && new_view_nonoverlaps)
				{
					// The user's view position is moving to a non-overlapping position in the same land map.
					// Remove the user's ClientThreads from each of the LandMap's chunks in the old view area.
					for (y = Math.max(0, _userData.viewChunkY0); y < Math.min(land_map.height_in_chunks, _userData.viewChunkY0 + Constants.VIEW_AREA_CHUNKS_WIDE); y++) {
						for (x = Math.max(0, _userData.viewChunkX0); x < Math.min(land_map.width_in_chunks, _userData.viewChunkX0 + Constants.VIEW_AREA_CHUNKS_WIDE); x++) {
							land_map.viewers[x][y].remove(_userData.client_thread);
						}
					}
				}

				// Add the user's ClientThread to each of the LandMap's chunks in the new view area.
				for (y = Math.max(0, newViewChunkY0); y < Math.min(land_map.height_in_chunks, newViewChunkY0 + Constants.VIEW_AREA_CHUNKS_WIDE); y++) {
					for (x = Math.max(0, newViewChunkX0); x < Math.min(land_map.width_in_chunks, newViewChunkX0 + Constants.VIEW_AREA_CHUNKS_WIDE); x++) {
						land_map.viewers[x][y].add(_userData.client_thread);
					}
				}
			}
			else
			{
				// Remove the user's ClientThread from viewing parts of the old view that are outside of the new view.
				for (y = Math.max(0, _userData.viewChunkY0); y < Math.min(land_map.height_in_chunks, _userData.viewChunkY0 + Constants.VIEW_AREA_CHUNKS_WIDE); y++) {
					for (x = Math.max(0, _userData.viewChunkX0); x < Math.min(land_map.width_in_chunks, _userData.viewChunkX0 + Constants.VIEW_AREA_CHUNKS_WIDE); x++) {
						if ((y < newViewChunkY0) || (y >= (newViewChunkY0 + Constants.VIEW_AREA_CHUNKS_WIDE)) || (x < newViewChunkX0) || (x >= (newViewChunkX0 + Constants.VIEW_AREA_CHUNKS_WIDE))) {
							land_map.viewers[x][y].remove(_userData.client_thread);
						}
					}
				}

				// Add the user's ClientThread for viewing parts of the new view that are outside of the old view.
				for (y = Math.max(0, newViewChunkY0); y < Math.min(land_map.height_in_chunks, newViewChunkY0 + Constants.VIEW_AREA_CHUNKS_WIDE); y++) {
					for (x = Math.max(0, newViewChunkX0); x < Math.min(land_map.width_in_chunks, newViewChunkX0 + Constants.VIEW_AREA_CHUNKS_WIDE); x++) {
						if ((y < _userData.viewChunkY0) || (y >= (_userData.viewChunkY0 + Constants.VIEW_AREA_CHUNKS_WIDE)) || (x < _userData.viewChunkX0) || (x >= (_userData.viewChunkX0 + Constants.VIEW_AREA_CHUNKS_WIDE))) {
							land_map.viewers[x][y].add(_userData.client_thread);
						}
					}
				}
			}
		}

		// If an output buffer is given, so that map events may be sent to the user...
		if (_output_buffer != null)
		{
			if (user_is_new_to_map || new_view_nonoverlaps)
			{
				// Either the user has no previous view on this map, or else its previous view has no overlap with its new view.
				// Send a full map event, encompassing the entire view area as well as a 1-block border surrounding it.
				Display.GetMapEvent(_output_buffer, _userData, land_map, newViewChunkX0 * Constants.DISPLAY_CHUNK_SIZE - 1, newViewChunkY0 * Constants.DISPLAY_CHUNK_SIZE - 1, (newViewChunkX0 + Constants.VIEW_AREA_CHUNKS_WIDE) * Constants.DISPLAY_CHUNK_SIZE, (newViewChunkY0 + Constants.VIEW_AREA_CHUNKS_WIDE) * Constants.DISPLAY_CHUNK_SIZE, true, true, false);
			}
			else
			{
				// The user has a previous view on this map that overlaps with its new view. Send a map event including only the new area(s).

				if ((newViewChunkX0 == _userData.viewChunkX0) && (newViewChunkY0 == _userData.viewChunkY0))
				{
					// There is no new map data to send, so send a simple event alerting the client of the new view position.
					Display.GetPanViewEvent(_output_buffer, _viewX, _viewY);
				}
				else
				{
					// Generate map event for part of new view area that is to the left or right of old view area, with full height of new view area.
					if (newViewChunkX0 > _userData.viewChunkX0) {
						Display.GetMapEvent(_output_buffer, _userData, land_map, (_userData.viewChunkX0 + Constants.VIEW_AREA_CHUNKS_WIDE) * Constants.DISPLAY_CHUNK_SIZE + 1, newViewChunkY0 * Constants.DISPLAY_CHUNK_SIZE - 1, (newViewChunkX0 + Constants.VIEW_AREA_CHUNKS_WIDE) * Constants.DISPLAY_CHUNK_SIZE, (newViewChunkY0 + Constants.VIEW_AREA_CHUNKS_WIDE) * Constants.DISPLAY_CHUNK_SIZE, _pan_view, false, false);
					} else if (newViewChunkX0 < _userData.viewChunkX0) {
						Display.GetMapEvent(_output_buffer, _userData, land_map, newViewChunkX0 * Constants.DISPLAY_CHUNK_SIZE - 1, newViewChunkY0 * Constants.DISPLAY_CHUNK_SIZE - 1, _userData.viewChunkX0 * Constants.DISPLAY_CHUNK_SIZE - 2, (newViewChunkY0 + Constants.VIEW_AREA_CHUNKS_WIDE) * Constants.DISPLAY_CHUNK_SIZE, _pan_view, false, false);
					}

					// Generate map event for part of new view that is above or below old view area, and entirely contained within horizontal extent of old view area, so as not to overlap the first map event, above.
					if (newViewChunkY0 > _userData.viewChunkY0) {
						Display.GetMapEvent(_output_buffer, _userData, land_map, Math.max(_userData.viewChunkX0, newViewChunkX0) * Constants.DISPLAY_CHUNK_SIZE - 1, (_userData.viewChunkY0 + Constants.VIEW_AREA_CHUNKS_WIDE) * Constants.DISPLAY_CHUNK_SIZE + 1, (Math.min(_userData.viewChunkX0, newViewChunkX0) + Constants.VIEW_AREA_CHUNKS_WIDE) * Constants.DISPLAY_CHUNK_SIZE, (newViewChunkY0 + Constants.VIEW_AREA_CHUNKS_WIDE) * Constants.DISPLAY_CHUNK_SIZE, _pan_view, false, false);
					} else if (newViewChunkY0 < _userData.viewChunkY0) {
						Display.GetMapEvent(_output_buffer, _userData, land_map, Math.max(_userData.viewChunkX0, newViewChunkX0) * Constants.DISPLAY_CHUNK_SIZE - 1, newViewChunkY0 * Constants.DISPLAY_CHUNK_SIZE - 1, (Math.min(_userData.viewChunkX0, newViewChunkX0) + Constants.VIEW_AREA_CHUNKS_WIDE) * Constants.DISPLAY_CHUNK_SIZE, _userData.viewChunkY0 * Constants.DISPLAY_CHUNK_SIZE - 2, _pan_view, false, false);
					}
				}
			}
		}

		// Record the user's new view chunk area position.
		_userData.viewChunkX0 = newViewChunkX0;
		_userData.viewChunkY0 = newViewChunkY0;
	}

	public static void SetUserStoredView(UserData _userData, int _landmapID, int _viewX, int _viewY)
	{
		// Record the user's view position for their current map.
		if (_landmapID == Constants.MAINLAND_MAP_ID) {
			_userData.mainland_viewX = _viewX;
			_userData.mainland_viewY = _viewY;
		} else if (_landmapID >= Raid.RAID_ID_BASE) {
			_userData.raidland_viewX = _viewX;
			_userData.raidland_viewY = _viewY;
		} else {
			_userData.homeland_viewX = _viewX;
			_userData.homeland_viewY = _viewY;
		}
	}

	public static void ResetUserView(UserData _userData)
	{
		// Get the user's current map.
		LandMap land_map = DataManager.GetLandMap(_userData.mapID, false);

		if (land_map == null)
		{
			Output.PrintToScreen("ERROR: ResetUserView() for user " + _userData.name + " (" + _userData.ID + "), land_map with ID " + _userData.mapID + " does not exist.");
			return;
		}

		// Record the user's view position for their current map.
		SetUserStoredView(_userData, _userData.mapID, _userData.viewX, _userData.viewY);

		// Remove the user's ClientThread from each of the LandMap's chunks in the old view area.
		for (int y = Math.max(0, _userData.viewChunkY0); y < Math.min(land_map.height_in_chunks, _userData.viewChunkY0 + Constants.VIEW_AREA_CHUNKS_WIDE); y++) {
			for (int x = Math.max(0, _userData.viewChunkX0); x < Math.min(land_map.width_in_chunks, _userData.viewChunkX0 + Constants.VIEW_AREA_CHUNKS_WIDE); x++) {
				land_map.viewers[x][y].remove(_userData.client_thread);
			}
		}

		// Record the user's new view position and view chunk area position.
		_userData.viewX = Integer.MIN_VALUE;
		_userData.viewY = Integer.MIN_VALUE;
		_userData.viewChunkX0 = Integer.MIN_VALUE;
		_userData.viewChunkY0 = Integer.MIN_VALUE;

		// Mark the user's data to be updated.
		DataManager.MarkForUpdate(_userData);
	}

	public static void SwitchMap(StringBuffer _output_buffer, int _userID)
	{
	  // Get the user's data
	  UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		boolean user_viewing_raid = (userData.mapID >= Raid.RAID_ID_BASE);

		// Remove the user from its current map.
		ResetUserView(userData);

		if ((userData.mapID == Constants.MAINLAND_MAP_ID) || user_viewing_raid)
		{
			// Get the user's nation's homeland map (creating it if it doesn't yet exist).
			LandMap homeland_map = Homeland.GetHomelandMap(userData.nationID);

			// Switch the user's view to thier nation's homeland map.
			userData.mapID = homeland_map.ID;
		}
		else
		{
			// Switch the user's view to the mainland map.
			userData.mapID = Constants.MAINLAND_MAP_ID;
		}

		// Set the user's view, according to which map it is on.
		SetUserViewForMap(userData, _output_buffer);

		// If appropriate, alert the raid system that a user has stopped viewing their raid.
		if (user_viewing_raid) {
			Raid.OnUserStoppedViewingRaid(userData);
		}
	}
}
