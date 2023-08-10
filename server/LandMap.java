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
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

import WOCServer.*;

public class LandMap
{
	final int XXHASH_SEED = 739683679;

	int ID = -1;
	int width = -1;
	int height = -1;
	int width_in_chunks = -1;
	int height_in_chunks = -1;
	BlockData [][] blocks = null;
	HashMap<Integer,BlockExtData> extended_data;
	HashMap<Integer,Integer> object_count;

	LandMapInfoData info = null;
	RaidData raidData = null;

	LinkedList<ClientThread> [][] viewers;
	ConcurrentHashMap<Integer,Boolean> blocks_to_update;
	boolean marked_for_update = false;
	XXHash xxhash = null;

	static Connection db = null;

	public void Clear()
	{
		if (blocks_to_update != null)
		{
			blocks_to_update.clear();
			blocks_to_update = null;
		}

		if (blocks != null) {
			blocks = null;
		}

		if (extended_data != null)
		{
			extended_data.clear();
			extended_data = null;
		}

		if (object_count != null)
		{
			object_count.clear();
			object_count = null;
		}
	}

	public LandMap(int _ID)
	{
		ID = _ID;
		extended_data = new HashMap<Integer,BlockExtData>();
		object_count = new HashMap<Integer,Integer>();
		blocks_to_update = new ConcurrentHashMap<Integer,Boolean>();
		xxhash = new XXHash(XXHASH_SEED);
	}

	public boolean LoadFromDB()
	{
		String sql;
		BlockExtData block_ext_data;

		//Output.PrintToScreen("LoadFromDB() start for landmap " + ID);
		//Output.PrintStackTrace();

		// Create query statement to determine dimensions of this landmap
		sql = "SELECT MAX(x) AS 'x', MAX(y) AS 'y', COUNT(*) as count FROM Block WHERE ID= '" + ID + "'";

		try {
			// Create statement for use with the DB.
			Statement stmt = db.createStatement();

			// Get the result set for the sql query
			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next())
			{
				if (rs.getInt("count") > 0)
				{
					width = rs.getInt("x") + 1;
					height = rs.getInt("y") + 1;
				}
			}

			// Close statement and result set (must be done explicitly to avoid memory leak).
			try { if (rs != null) rs.close(); } catch (Exception e) {};
	    try { if (stmt != null) stmt.close(); } catch (Exception e) {};
		} catch(Exception e) {
      Output.PrintToScreen("Couldn't fetch dimensions of landmap with ID " + ID + ". Message: " + e.getMessage());
		}

		if ((width <= 0) || (height <= 0)) {
			return false;
		}

		// Initialize the land map for this size.
		SetSize(width, height, false);

		// Create sql statement to fetch all blocks belonging to this landmap
		sql = "SELECT * FROM Block WHERE ID= '" + ID + "'";

		try {
			// Create statement for use with the DB.
			Statement stmt = db.createStatement();

			// Get the result set for the sql query
			ResultSet rs = stmt.executeQuery(sql);

			int cur_x, cur_y;
			int num_blocks_fetched = 0;

			while (rs.next()) {
				cur_x = rs.getInt("x");
				cur_y = rs.getInt("y");
				blocks[cur_x][cur_y].terrain = rs.getInt("terrain");
				blocks[cur_x][cur_y].nationID = rs.getInt("nationID");
				blocks[cur_x][cur_y].flags = rs.getInt("flags");
				blocks[cur_x][cur_y].marked_for_update = false;
				num_blocks_fetched++;
			}

			if (num_blocks_fetched != (width * height))
			{
	      Output.PrintToScreen("Fetched incorrect number of blocks for landmap with ID " + ID + ". Number fetched: " + num_blocks_fetched + ", correct number: " + (width * height) + ".");
				blocks = null;
				return false;
			}

			// Close statement and result set (must be done explicitly to avoid memory leak).
			try { if (rs != null) rs.close(); } catch (Exception e) {};
	    try { if (stmt != null) stmt.close(); } catch (Exception e) {};
		}
	  catch(Exception e)
		{
      Output.PrintToScreen("Couldn't fetch blocks for landmap with ID " + ID + ". Message: " + e.getMessage());
			blocks = null;
			return false;
		}

		//// TEMPORARY
		//CreateBlockExtData();

		// Create sql statement to fetch all *active* block exts belonging to this landmap
		sql = "SELECT * FROM BlockExt WHERE ID= '" + ID + "' AND active= TRUE";

		try {
			// Create statement for use with the DB.
			Statement stmt = db.createStatement();

			// Get the result set for the sql query
			ResultSet rs = stmt.executeQuery(sql);

			int cur_x, cur_y;

			while (rs.next()) {
				block_ext_data = new BlockExtData();
				cur_x = rs.getInt("x");
				cur_y = rs.getInt("y");
				block_ext_data.objectID = rs.getInt("objectID");
				block_ext_data.owner_nationID = rs.getInt("owner_nationID");
				block_ext_data.creation_time = rs.getInt("creation_time");
				block_ext_data.completion_time = rs.getInt("completion_time");
				block_ext_data.invisible_time = rs.getInt("invisible_time");
				block_ext_data.capture_time = rs.getInt("capture_time");
				block_ext_data.crumble_time = rs.getInt("crumble_time");
				block_ext_data.wipe_flags = rs.getInt("wipe_flags");
				block_ext_data.wipe_nationID = rs.getInt("wipe_nationID");
				block_ext_data.wipe_end_time = rs.getInt("wipe_end_time");
				block_ext_data.triggerable_time = rs.getInt("triggerable_time");

				// Record this BlockExtData in the extended_data hash table.
				extended_data.put(GetBlockHash(cur_x, cur_y), block_ext_data);

				// Keep count of each resource and orb type.
				if (block_ext_data.objectID >= ObjectData.RESOURCE_OBJECT_BASE_ID) {
					object_count.put(block_ext_data.objectID, object_count.getOrDefault(block_ext_data.objectID, 0) + 1);
				}

				//Output.PrintToScreen("Loaded block ext data for " + cur_x +"," + cur_y + " with objectID " + block_ext_data.objectID);
			}

			// Close statement and result set (must be done explicitly to avoid memory leak).
			try { if (rs != null) rs.close(); } catch (Exception e) {};
	    try { if (stmt != null) stmt.close(); } catch (Exception e) {};
		}
	  catch(Exception e)
		{
      Output.PrintToScreen("Couldn't fetch block exts for landmap with ID " + ID + ". Message: " + e.getMessage());
			return false;
		}

		//Output.PrintToScreen("LoadFromDB() end for landmap " + ID);

		// TEMP
		//Output.PrintToScreen("Updating to DB");
		//UpdateToDB();

		return true;
	}

	public void SetSize(int _width, int _height, boolean _insert_to_db)
	{
		String sql, block_sql, block_ext_sql;
		int x, y;

		//Output.PrintToScreen("Cur size: " + width + "," + height + ", SetSize(" + _width + "," + _height + ") _insert_to_db: " + _insert_to_db);

		if ((_width <= 0) || (_height <= 0))
		{
	    Output.PrintToScreen("LandMap.SetSize() cannot set size to " + _width + "," + _height + ".");
			return;
		}

		if (blocks != null)
		{
			// If the map's width is decreasing, remove all nations from the width of the map that is being eliminated.
			if (_width < width)
			{
				for (y = 0; y < height; y++)
				{
					for (x = _width; x < width; x++)
					{
						World.SetBlockNationID(this, x, y, -1, true, false, -1, 0);
					}
				}
			}

			// If the map's height is decreasing, remove all nations from the height of the map that is being eliminated.
			if (_height < height)
			{
				for (y = _height; y < height; y++)
				{
					for (x = 0; x < width; x++)
					{
						World.SetBlockNationID(this, x, y, -1, true, false, -1, 0);
					}
				}
			}
		}

		BlockData new_blocks[][] = new BlockData[_width][_height];

		// Initialize all blocks in the new block array.
		for (y = 0; y < _height; y++)
		{
			for (x = 0; x < _width; x++)
			{
				new_blocks[x][y] = new BlockData();
			}
		}

		// Copy over blocks from the old block array, if appropriate.
		if (blocks != null)
		{
			for (y = 0; y < Math.min(height, _height); y++)
			{
				for (x = 0; x < Math.min(width, _width); x++)
				{
					new_blocks[x][y] = blocks[x][y];
				}
			}
		}

		// Create a viewer list for each chunk in this land map.
		width_in_chunks = (_width + Constants.DISPLAY_CHUNK_SIZE - 1) / Constants.DISPLAY_CHUNK_SIZE;
		height_in_chunks = (_height + Constants.DISPLAY_CHUNK_SIZE - 1) / Constants.DISPLAY_CHUNK_SIZE;
		viewers = new LinkedList[width_in_chunks][height_in_chunks];
		for (y = 0; y < height_in_chunks; y++)
		{
			for (x = 0; x < width_in_chunks; x++)
			{
				viewers[x][y] = new LinkedList<ClientThread>(); // Create viewer list for this chunk, with initial capacity.
			}
		}

		if (_insert_to_db)
		{
			// Insert block data ////////////////////
			try {
				block_sql = "INSERT INTO Block (ID,x,y) VALUES(?, ?, ?)";
				PreparedStatement block_stmt = db.prepareStatement(block_sql);

				block_ext_sql = "INSERT INTO BlockExt (ID,x,y,active) VALUES(?, ?, ?, ?)";
				PreparedStatement block_ext_stmt = db.prepareStatement(block_ext_sql);

				//Output.PrintToScreen("here1 y range " + Math.max(0,height) + " to " + _height + ", x range " + Math.max(0,width) + " to " + _width);

				// Insert each new block (expanding from old size to new size) into the DB.

				// Expand existing rows, if width has increased.
				if ((width > 0) && (height > 0) && (_width > width))
				{
					for (y = 0; y < height; y++)
					{
						for (x = width; x < _width; x++)
						{
							// Populate the prepared statement for this block.
							block_stmt.setInt(1, ID);
							block_stmt.setInt(2, x);
							block_stmt.setInt(3, y);

							//Output.PrintToScreen("Adding ID " + ID + " block " + x + "," + y);

							// Add the sql query to this batch
							block_stmt.addBatch();

							// Populate the prepared statement for this block ext.
							block_ext_stmt.setInt(1, ID);
							block_ext_stmt.setInt(2, x);
							block_ext_stmt.setInt(3, y);
							block_ext_stmt.setBoolean(4, false);

							// Add the sql query to this batch
							block_ext_stmt.addBatch();
						}

						// Execute the batch
						block_stmt.executeBatch();

						// Execute the batch
						block_ext_stmt.executeBatch();
					}
				}

				// Add new rows, if y has increased.
				if (_height > height)
				{
					for (y = Math.max(0,height); y < _height; y++)
					{
						for (x = 0; x < _width; x++)
						{
							// Populate the prepared statement for this block.
							block_stmt.setInt(1, ID);
							block_stmt.setInt(2, x);
							block_stmt.setInt(3, y);

							//Output.PrintToScreen("Adding ID " + ID + " block " + x + "," + y);

							// Add the sql query to this batch
							block_stmt.addBatch();

							// Populate the prepared statement for this block ext.
							block_ext_stmt.setInt(1, ID);
							block_ext_stmt.setInt(2, x);
							block_ext_stmt.setInt(3, y);
							block_ext_stmt.setBoolean(4, false);

							// Add the sql query to this batch
							block_ext_stmt.addBatch();
						}

						// Execute the batch
						block_stmt.executeBatch();

						// Execute the batch
						block_ext_stmt.executeBatch();
					}
				}

				// Close statements
				block_stmt.close();
				block_ext_stmt.close();
			}
			catch(Exception e) {
				Output.PrintToScreen("Could not store landmap with ID " + ID + ". Error when inserting blocks.");
				Output.PrintException(e);
			}

			// In case map width is decreasing, delete any blocks with this map ID that have an x value beyond the new width.
			sql = "DELETE FROM Block WHERE ID='" + ID + "' AND x>=" + _width;
			BaseData.ExecuteUpdate(db, sql, false, true);
			sql = "DELETE FROM BlockExt WHERE ID='" + ID + "' AND x>=" + _width;
			BaseData.ExecuteUpdate(db, sql, false, true);

			// In case map height is decreasing, delete any blocks with this map ID that have a y value beyond the new height.
			sql = "DELETE FROM Block WHERE ID='" + ID + "' AND y>=" + _height;
			BaseData.ExecuteUpdate(db, sql, false, true);
			sql = "DELETE FROM BlockExt WHERE ID='" + ID + "' AND y>=" + _height;
			BaseData.ExecuteUpdate(db, sql, false, true);
		}

		// Switch to using the new blocks array and the new dimensions.
		blocks = new_blocks;
		width = _width;
		height = _height;
	}

	public void CreateBlockExtData() // Temporary -- going forward, this data will be created when the landscape is created.
	{
		Output.PrintToScreen("About to create block ext data for landscape...");

		try {
			String sql = "INSERT INTO BlockExt (ID,x,y,active) VALUES(?, ?, ?, ?)";
			PreparedStatement stmt = db.prepareStatement(sql);

			// Insert each new block ext (expanding from old size to new size) into the DB.
			for (int y = 0; y < height; y++)
			{
				for (int x = 0; x < width; x++)
				{
					// Populate the prepared statement for this block ext.
					stmt.setInt(1, ID);
					stmt.setInt(2, x);
					stmt.setInt(3, y);
					stmt.setBoolean(4, false);

					// Add the sql query to this batch
					stmt.addBatch();
				}

				// Execute the batch
				stmt.executeBatch();
			}

			// Close statement
			stmt.close();
		}
		catch(Exception e) {
			Output.PrintToScreen("Could not insert BlockExt objects into DB for landmap with ID " + ID + ".");
			Output.PrintException(e);
		}
	}

	public void UpdateToDB()
	{
		// Save each block to the DB.
		for (int y = 0; y < height; y++)
		{
			for (int x = 0; x < width; x++)
			{
				UpdateSingleBlockToDB(x, y);
			}
		}
	}

	public void Copy(LandMap _original)
	{
		BlockData block_data, source_block_data;
		BlockExtData block_ext_data, source_block_ext_data;
		int block_hash;

		if ((blocks == null) || (width != _original.width) || (height != _original.height))
		{
			Output.PrintToScreen("ERROR: LandMap.Copy() called on LandMap that has not had its size set, or that differs in size from the source map.");
			return;
		}

		// Copy all blocks from the original landmap.
		for (int y = 0; y < height; y++)
		{
			for (int x = 0; x < width; x++)
			{
				// Determine the hash for this block's coords
				block_hash = GetBlockHash(x, y);

				// Get the block's data and the source block's data.
				block_data = blocks[x][y];
				source_block_data = _original.blocks[x][y];

				// Get the block's extended data, if it exists.
				block_ext_data = extended_data.get(block_hash);

				// Copy the block's data, and reset its transient data.
				block_data.CopyData(source_block_data);
				block_data.ResetTransientData();

				// If the source block has extended data (and so the copy should as well)...
				if ((source_block_data.flags & BlockData.BF_EXTENDED_DATA) != 0)
				{
					// Get BlockExtData for this block from the source landmap.
					source_block_ext_data = _original.extended_data.get(block_hash);

					if (source_block_ext_data == null)
					{
						Output.PrintToScreen("ERROR: LandMap.Create() Source map " + _original.ID + " coords " + x + "," + y + " has BF_EXTENDED_DATA flag set, but no BlockExtData.");
						return;
					}

					// If the block doesn't already have extended data...
					if (block_ext_data == null)
					{
						// Create the equivalent BlockExtData for this landmap.
						block_ext_data = new BlockExtData();
						extended_data.put(block_hash, block_ext_data);
					}

					// Copy the block's extended data.
					block_ext_data.CopyData(source_block_ext_data);
				}
				else if (block_ext_data != null)
				{
					// The block already has extended data, but now doesn't need to. Reset the extended data's values so that it will be removed.
					block_data.flags = block_data.flags | BlockData.BF_EXTENDED_DATA; // Add flag indicating that the block has extended data.
					block_ext_data.ResetData();	// Reset the extended data's values so it will be removed when updated to the DB.
				}

				// Mark this block to be updated.
				DataManager.MarkBlockForUpdate(this, x, y);
			}
		}
	}

	// NOTE: Don't call this directly, call DataManager.MarkBlockForUpdate() so that it will record that this land map needs to be updated.
	public void MarkBlockForUpdate(int _x, int _y)
	{
		if ((_x < 0) || (_x >= width) || (_y < 0) || (_y >= height))
		{
			Output.PrintToScreen("MarkBlockForUpdate() for landmap " + ID + " given coords " + _x + "," + _y + " that are out of range. Width: " + width + ", height: " + height + ".");
			return;
		}

		if (blocks[_x][_y].marked_for_update == false)
		{
			blocks[_x][_y].marked_for_update = true;
			blocks_to_update.put(Constants.TokenizeCoordinates(_x, _y), true);

			// TESTING
			//if (DataManager.DatabaseUpdateInProgress()) {
			//	Output.PrintToScreen("NOTE: Block " + _x + "," + _y + " (nationID " + blocks[_x][_y].nationID + ") marked for update while database update is in progress.");
			//}
		}
	}

	public void UpdateMarkedBlocks()
	{
		Integer key;
		int [] coord_array = new int[2];

		// Update the DB records of each of the blocks in the blocks_to_update map.
		Iterator it = blocks_to_update.keySet().iterator();
		while (it.hasNext())
		{
      key = (Integer) it.next();
//		for (Integer key : blocks_to_update.keySet())
//		{

			// Determine the coords of the current block to update.
			Constants.UntokenizeCoordinates(key, coord_array);

			// Update the DB entry of the current block.
			UpdateSingleBlockToDB(coord_array[0], coord_array[1]);

			// Unmark the block; it no longer needs to be updated in the DB.
			blocks[coord_array[0]][coord_array[1]].marked_for_update = false;

			// Remove the current key's entry from the map of blocks to update.
			it.remove();
		}

		// NOTE: Do NOT clear the list of blocks to be updated. Doing so, if any were added to the list during the update process, could result in those objects being marked for update but not being in the list, and so never updated again!
	}

	public void UpdateSingleBlockToDB(int _x, int _y)
	{
		// Get pointer to the bock's data
		BlockData block_data = blocks[_x][_y];

		// If the block has the BF_EXTENDED_DATA flag set...
		if ((block_data.flags & BlockData.BF_EXTENDED_DATA) != 0)
		{
			// Determine the hash for this block's coords
			int block_hash = GetBlockHash(_x, _y);

			// Get the block's extended data object from the extended_data HashMap.
			BlockExtData block_ext_data = extended_data.get(block_hash);

			if (block_ext_data == null)
			{
				// The BF_EXTENDED_DATA flag was set, but the extended_data HashMap has no BlockExtData object for this block. This should never happen.
				Output.PrintToScreen("ERROR: Block extended data not found for block " + _x + "," + _y + ", though BF_EXTENDED_DATA is set!");

				// Remove the BF_EXTENDED_DATA flag from the block.
				block_data.flags = block_data.flags & ~BlockData.BF_EXTENDED_DATA;
			}
			else
			{
				// Update the block's extended data in the database //////////////

				// Determine whether the extended data should remain active.
				boolean active = (((block_ext_data.objectID != -1) && ((block_ext_data.crumble_time == -1) || (block_ext_data.crumble_time > Constants.GetTime()))) || (block_ext_data.wipe_end_time > Constants.GetTime()));

				String sql = "UPDATE BlockExt SET " +
				"version = '" + BlockExtData.VERSION + "', " +
				"active = " + (active ? "TRUE" : "FALSE") + ", " +
				"objectID = '" + block_ext_data.objectID + "', " +
				"owner_nationID = '" + block_ext_data.owner_nationID + "', " +
				"creation_time = '" + block_ext_data.creation_time + "', " +
				"completion_time = '" + block_ext_data.completion_time + "', " +
				"invisible_time = '" + block_ext_data.invisible_time + "', " +
				"capture_time = '" + block_ext_data.capture_time + "', " +
				"crumble_time = '" + block_ext_data.crumble_time + "', " +
				"wipe_flags = '" + block_ext_data.wipe_flags + "', " +
				"wipe_nationID = '" + block_ext_data.wipe_nationID + "', " +
				"wipe_end_time = '" + block_ext_data.wipe_end_time + "', " +
				"triggerable_time = '" + block_ext_data.triggerable_time + "' " +
				"WHERE ID= '" + ID + "' AND x= '" + _x + "' AND y= '" + _y + "'";

				try {
					// Create statement for use with the DB.
					Statement stmt = db.createStatement();

					// Execute the sql query
					stmt.executeUpdate(sql);
					stmt.close();
				}
				catch(Exception e) {
					Output.PrintToScreen("Could not store landmap with ID " + ID + ". Error when updating extended data for block " + _x + "," + _y + ". Message: " + e.getMessage() + "\n sql string: '" + sql + "'");
				}

				// If the block's extended data is no longer active, remove the BF_EXTENDED_DATA flag from the block and remove the BlockExtData from the extended_data HashMap.
				if (active == false)
				{
					// Remove the BF_EXTENDED_DATA flag from the block.
					block_data.flags = block_data.flags & ~BlockData.BF_EXTENDED_DATA;

					// Remove this block's BlockExtData from the extended_data HashMap.
					extended_data.remove(block_hash);
				}
			}
		}

		// Update the block's data in the database //////////////

		String sql = "UPDATE Block SET " +
		"version = '" + BlockData.VERSION + "', " +
		"terrain = '" + block_data.terrain + "', " +
		"nationID = '" + block_data.nationID + "', " +
		"flags = '" + block_data.flags + "' " +
		"WHERE ID= '" + ID + "' AND x= '" + _x + "' AND y= '" + _y + "'";

		try {
			// Create statement for use with the DB.
			Statement stmt = db.createStatement();

			// Execute the sql query
			stmt.executeUpdate(sql);
			stmt.close();
		}
		catch(Exception e) {
			Output.PrintToScreen("Could not store landmap with ID " + ID + ". Error when updating block " + _x + "," + _y + ". Message: " + e.getMessage() + "\n sql string: '" + sql + "'");
		}
	}

	public RaidData GetRaidData()
	{
		if (raidData == null) {
			raidData = (RaidData)DataManager.GetData(Constants.DT_RAID, ID, false);
		}

		return raidData;
	}

	public void CheckForBuildObjectCrumble(int _blockX, int _blockY, BlockData _block_data, BlockExtData _block_ext_data)
	{
		// If the block data is not given, or if the block has no extended data (and so no build obect), do nothing.
		if ((_block_ext_data == null) || (_block_data == null)) {
			return;
		}

		// If the given block contains a build object that has crumbled by now, remove the build object from the block.
		if ((_block_ext_data.owner_nationID != _block_data.nationID) && (_block_ext_data.crumble_time != -1) && (_block_ext_data.crumble_time < Constants.GetTime()))
		{
			_block_ext_data.objectID = -1;
			DataManager.MarkBlockForUpdate(this, _blockX, _blockY);
		}
	}

	public boolean CheckForBuildObjectWithTriggerType(int _blockX, int _blockY, int _trigger_on, int _triggerNationID, int _targetNationID, int _event_x, int _event_y)
	{
		BlockData block_data = GetBlockData(_blockX, _blockY);

		if ((block_data == null) || ((block_data.flags & BlockData.BF_EXTENDED_DATA) == 0)) {
			return false;
		}

		// If it's a nation other than this block's nation that has been attacked nearby, return false.
		if (((_trigger_on == BuildData.TRIGGER_ON_RADIUS_ATTACK) || (_trigger_on == BuildData.TRIGGER_ON_RADIUS_ATTACK_EMPTY)) && (_targetNationID != block_data.nationID)) {
			return false;
		}

		BlockExtData block_ext_data = GetBlockExtendedData(_blockX, _blockY, false);

		// Get the current time
		int cur_time = Constants.GetTime();

		// If the block has no extended data, or does not have a build object, or its build object is incomplete,
		// or owned by a nation other than the one occupying the block, then an object in this block can't be triggered.
		if ((block_ext_data == null) || (block_ext_data.objectID == -1) || (block_ext_data.objectID >= ObjectData.RESOURCE_OBJECT_BASE_ID) || (block_ext_data.completion_time > cur_time) || (block_ext_data.owner_nationID == -1) || (block_ext_data.owner_nationID != block_data.nationID)) {
			return false;
		}

		// If the build object is still in its cooldown period then it's too early to trigger it again.
		if (cur_time < block_ext_data.triggerable_time) {
			return false;
		}

		// If the build object was re-captured by its owner, and less than the TOWER_REBUILD_PERIOD has passed since then, it's too early to trigger it.
		if (cur_time < (block_ext_data.capture_time + Constants.TOWER_REBUILD_PERIOD)) {
			return false;
		}

		// Get the build data for the object in this block.
		BuildData build_data = BuildData.GetBuildData(block_ext_data.objectID);

		// Make sure the build data exists.
		if (build_data == null)
		{
			Output.PrintToScreen("ERROR: Block " + _blockX + "," + _blockY + " has objectID " + block_ext_data.objectID + "; there is no build data with that ID!");
			return false;
		}

		// If the build object in this block doesn't have a trigger type matching that given, return false.
		if (build_data.trigger_on != _trigger_on) {
			return false;
		}

		// If the triggering event is beyond this object's attack radius (as a circle), return false.
		if ((_trigger_on == BuildData.TRIGGER_ON_RADIUS_ATTACK) || (_trigger_on == BuildData.TRIGGER_ON_RADIUS_TOWER))
		{
			float radius_squared = (build_data.attack_radius + 0.5f) * (build_data.attack_radius + 0.5f);
			float y_dist = _blockY - _event_y;
			float x_dist = _blockX - _event_x;
			if (((y_dist * y_dist) + (x_dist * x_dist)) > radius_squared) {
				return false;
			}
		}

		// Get the block's nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, block_data.nationID, false);

		if (_trigger_on == BuildData.TRIGGER_ON_RADIUS_TOWER)
		{
			// If the triggering tower belongs to this block's nation, or an ally of this block's nation, this block's tower will not be triggered.
			if ((block_data.nationID == _triggerNationID) || (nationData.alliances_active.indexOf(Integer.valueOf(_triggerNationID)) != -1)) {
				return false;
			}
		}

		// There is a build object in this block that can be triggered on the given _trigger_on type.
		return true;
	}

	public boolean CheckForLastingWipePreventingAttack(int _x, int _y, int _attack_nationID)
	{
		// Get the target block's extended data.
		BlockExtData blockExtData = GetBlockExtendedData(_x, _y, false);

		//Output.PrintToScreen("CheckForLastingWipePreventingAttack() called for block " + _x + "," + _y + ", attack nation ID: " + _attack_nationID + ". blockExtData: " + blockExtData + ", blockExtData.wipe_end_time: " + blockExtData.wipe_end_time + ", cur time: " + Constants.GetTime());

		// If the block currently has a lasting wipe in effect...
		if ((blockExtData != null) && (blockExtData.wipe_end_time > Constants.GetTime()))
		{
			// If this is a general wipe, and if the attacking nation isn't the block's wipe nation (which is immune to the wipe)...
			if (((blockExtData.wipe_flags & BuildData.WIPE_FLAG_GENERAL) != 0) && (blockExtData.wipe_nationID != _attack_nationID))
			{
				// Get the attacking nation's data
				NationData attackNationData = (NationData)DataManager.GetData(Constants.DT_NATION, _attack_nationID, false);

				// If the attacking nation is also not an ally of the block's wipe nation (which would also be immune), return true.
				if (attackNationData.alliances_active.indexOf(Integer.valueOf(blockExtData.wipe_nationID)) == -1) {
					return true;
				}
			}
			else if (((blockExtData.wipe_flags & BuildData.WIPE_FLAG_SPECIFIC) != 0) && (blockExtData.wipe_nationID == _attack_nationID))
			{
				// This is a specific lasting wipe, and the atacking nation is the one specific nation that is banned by this wipe. Return true.
				return true;
			}
		}

		return false;
	}

	public int PosXToMaxLevelLimit(int _x)
	{
		float pos = (float)_x / (float)(width - 1);

		if (pos < Constants.mid_level_limit_pos) {
			// Early levels
			pos = pos / Constants.mid_level_limit_pos;
			return (int)(Math.pow(pos, Constants.LEVEL_LIMIT_POWER) * (Constants.mid_level_limit - Constants.min_level_limit)) + Constants.min_level_limit;
		} else {
			// Later levels
			pos = (pos - Constants.mid_level_limit_pos) / (1.0f - Constants.mid_level_limit_pos);
			return (int)(Math.pow(pos, Constants.LEVEL_LIMIT_POWER) * (Constants.max_level_limit - Constants.mid_level_limit)) + Constants.mid_level_limit;
		}

		//return (int)(Math.pow(((float)_x / (float)(width - 1)), Constants.LEVEL_LIMIT_POWER) * (Constants.max_level_limit - Constants.min_level_limit)) + Constants.min_level_limit;
	}

	public int MaxLevelLimitToPosX(int _max_level_limit)
	{
		float early_level_width = Constants.mid_level_limit_pos * (float)(width - 1);
		float late_level_width = (1.0f - Constants.mid_level_limit_pos) * (float)(width - 1);

		if (_max_level_limit < Constants.mid_level_limit) {
			return (int)(Math.pow(((float)(_max_level_limit - Constants.min_level_limit) / (Constants.mid_level_limit - Constants.min_level_limit)), 1 / Constants.LEVEL_LIMIT_POWER) * early_level_width + 1.0f);
		} else {
			return (int)(Math.pow(((float)(_max_level_limit - Constants.mid_level_limit) / (Constants.max_level_limit - Constants.mid_level_limit)), 1 / Constants.LEVEL_LIMIT_POWER) * late_level_width + early_level_width + 1.0f);
		}

		//return (int)(Math.pow(((float)(_max_level_limit - Constants.min_level_limit) / (Constants.max_level_limit - Constants.min_level_limit)), 1 / Constants.LEVEL_LIMIT_POWER) * (float)(width - 1) + 1.0f);
	}

	public int GetEasternLevelLimit(int _nation_level)
	{
		return Math.max(_nation_level * Constants.EASTERN_LEVEL_LIMIT_MULTIPLIER, _nation_level + Constants.EASTERN_LEVEL_LIMIT_ADDEND);
	}

	public int GetBlockHash(int _x, int _y)
	{
		return (_x << 16) | _y;
	}

	public BlockData GetBlockData(int _x, int _y)
	{
		if ((_x < 0) || (_x >= width) || (_y < 0) || (_y >= height)) {
			return null;
		}
		return blocks[_x][_y];
	}

	public BlockExtData GetBlockExtendedData(int _x, int _y, boolean _create)
	{
		// Get the block's data.
		BlockData block_data = GetBlockData(_x, _y);

		if (block_data == null) {
			return null;
		}

		BlockExtData block_ext_data = null;

		// Determine the hash for this block's coords
		int block_hash = GetBlockHash(_x, _y);

		if ((block_data.flags & BlockData.BF_EXTENDED_DATA) != 0)
		{
			block_ext_data = extended_data.get(block_hash);

			if (block_ext_data == null)
			{
				// The BF_EXTENDED_DATA flag was set, but the extended_data HashMap has no BlockExtData object for this block. This should never happen.
				Output.PrintToScreen("Block extended data not found for block " + _x + "," + _y + ", though BF_EXTENDED_DATA is set!");
			}
		}

		if ((block_ext_data == null) && _create)
		{
			// Create a new BlockExtData from this block, and add it to the extended_data HashMap.
			block_ext_data = new BlockExtData();
			extended_data.put(block_hash, block_ext_data);

			// Set the block's BF_EXTENDED_DATA flag.
			block_data.flags |= BlockData.BF_EXTENDED_DATA;
		}

		return block_ext_data;
	}

	public boolean BlockIsHabitable(int _x, int _y)
	{
		int terrain = GetBlockTerrain(_x, _y);
		return ((terrain == Constants.TERRAIN_FLAT_LAND) || (terrain == Constants.TERRAIN_BEACH));
	}

	public int GetBlockTerrain(int _x, int _y)
	{
		if ((_x < 0) || (_x >= width) || (_y < 0) || (_y >= height)) {
			return -1;
		}
		return blocks[_x][_y].terrain;
	}

	public int GetBlockNationID(int _x, int _y)
	{
		if ((_x < 0) || (_x >= width) || (_y < 0) || (_y >= height)) {
			return -1;
		}
		return blocks[_x][_y].nationID;
	}

	public int GetBlockObjectID(int _x, int _y)
	{
		if ((_x < 0) || (_x >= width) || (_y < 0) || (_y >= height)) {
			return -1;
		}

		BlockExtData block_ext_data = GetBlockExtendedData(_x, _y, false);

		return (block_ext_data == null) ? -1 : block_ext_data.objectID;
	}

	public int GetBlockFlags(int _x, int _y)
	{
		if ((_x < 0) || (_x >= width) || (_y < 0) || (_y >= height)) {
			return -1;
		}
		return blocks[_x][_y].flags;
	}

	public int DetermineBlockFullHitPoints(int _x, int _y, boolean _upon_capture, int[] _flags)
	{
		// Get the block's data.
		BlockData block_data = GetBlockData(_x, _y);

		// If the block has no data or is not occupied by a nation, return the number of hit points of a vacant block.
		if ((block_data == null) || (block_data.nationID == -1)) {
			return Constants.VACANT_BLOCK_HIT_POINTS;
		}

		// Get the block's extended data
		BlockExtData block_ext_data = ((block_data.flags & BlockData.BF_EXTENDED_DATA) == 0) ? null : GetBlockExtendedData(_x, _y, false);

		// Get the block's nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, block_data.nationID, false);

		if (nationData == null) {
			return Constants.VACANT_BLOCK_HIT_POINTS;
		}

		// Start with the block's nation's number of hit points per square.
		float result = nationData.GetFinalHitPointsPerSquare();

		// Determine whether total defense is in effect due to the nation being inactive (no one logged in for a while). Does not apply on raid maps.
		boolean total_defense = (nationData.total_defense && (ID < Raid.RAID_ID_BASE) && (nationData.level < Constants.TOTAL_DEFENSE_OBSOLETE_LEVEL) && (nationData.num_members_online == 0) && ((Constants.GetTime() - nationData.prev_use_time) >= Constants.TIME_SINCE_LAST_ACTIVE_TOTAL_DEFENSE));

		if (total_defense)
		{
			// Multiply the base hp amount by the total defense multiplier.
			result *= Constants.TOTAL_DEFENSE_MULTIPLIER;

			// Add the total defense flag to the given flags value.
			if (_flags != null) {
				_flags[0] |= Constants.BATTLE_FLAG_TOTAL_DEFENSE;
			}

			//Output.PrintToScreen("Total defense active!");
		}

		// Determine whether insurgency is in effect for this square.
		boolean insurgency = (nationData.insurgency && IsBlockInsurgencyCandidate(_x, _y, nationData.ID));

		if (insurgency)
		{
			// Multiply the base hp amount by the insurgency defense multiplier.
			result *= Constants.INSURGENCY_DEFENSE_MULTIPLIER;

			// Add the insurgency flag to the given flags value.
			if (_flags != null) {
				_flags[0] |= Constants.BATTLE_FLAG_INSURGENCY;
			}

			//Output.PrintToScreen("Insurgency active!");
		}

		// If the block has a build object that is owned by the nation occupying the block...
		if ((block_ext_data != null) && (block_ext_data.objectID != -1) && (block_ext_data.owner_nationID == block_data.nationID))
		{
			// Get the build data
			BuildData build_data = BuildData.GetBuildData(block_ext_data.objectID);

			// If the object in the block is not currently inert (or is a storage type build, which cannot go inert)...
			if ((build_data.type == BuildData.TYPE_ENERGY_STORAGE) || (build_data.type == BuildData.TYPE_MANPOWER_STORAGE) || (BlockIsInert(nationData, _x, _y) == false))
			{
				// If the build object is not currently rebuilding after having been re-captured by its owner...
				if (Constants.GetTime() >= (block_ext_data.capture_time + Constants.TOWER_REBUILD_PERIOD))
				{
					// If the structure is complete...
					if ((block_ext_data.completion_time == -1) || (block_ext_data.completion_time < Constants.GetTime()))
					{
						// Add the build object's number of hit points to the result.
						result += build_data.hit_points * (_upon_capture ? Constants.RECAPTURED_DEFENSE_HIT_POINTS_PORTION : 1f);
					}
					else
					{
						// Add only a portion of the build object's number of hit points to the result, since the object is not yet complete.
						result += (build_data.hit_points * Constants.INCOMPLETE_DEFENSE_HIT_POINTS_PORTION * (_upon_capture ? Constants.RECAPTURED_DEFENSE_HIT_POINTS_PORTION : 1f));
					}
				}
			}
		}

		// TESTING
		if (result < 0)
		{
			Output.PrintToScreen("ERROR: DetermineBlockFullHitPoints() returning negative value " + result + " for nation " + nationData.name + "!");
			Output.PrintStackTrace();
		}

		return (int)(result + 0.5f);
	}

	// Return true if this block is surrounded by at least six blocks of the given nationID.
	public boolean IsBlockInsurgencyCandidate(int _x, int _y, int _nationID)
	{
    int surround_count = 0;

		if (GetBlockNationID(_x - 1, _y - 1) == _nationID)  surround_count++;
		if (GetBlockNationID(_x - 1, _y) == _nationID)      surround_count++;
		if (GetBlockNationID(_x - 1, _y + 1) == _nationID)  surround_count++;
		if (GetBlockNationID(_x, _y - 1) == _nationID)      surround_count++;
		if (GetBlockNationID(_x, _y + 1) == _nationID)      surround_count++;
		if (GetBlockNationID(_x + 1, _y - 1) == _nationID)  surround_count++;
		if (GetBlockNationID(_x + 1, _y) == _nationID)      surround_count++;
		if (GetBlockNationID(_x + 1, _y + 1) == _nationID)  surround_count++;

    if (surround_count < 5) {
      return false;
    }

    int match_count = 0;
    for (int cur_y = (_y - Constants.INSURGENCY_CHECK_RADIUS); cur_y <= (_y + Constants.INSURGENCY_CHECK_RADIUS); cur_y++)
    {
      for (int cur_x = (_x - Constants.INSURGENCY_CHECK_RADIUS); cur_x <= (_x + Constants.INSURGENCY_CHECK_RADIUS); cur_x++)
      {
        if (GetBlockNationID(cur_x, cur_y) == _nationID) {
          match_count++;
        }
      }
    }

    // Determine fraction of surrounding squares that are occupied by the given nation
    float fraction = (float)match_count / Constants.INSURGENCY_CHECK_AREA;

		return (fraction >= Constants.INSURGENCY_LOCAL_AREA_FRACTION);
	}

	public boolean BlockContainsInertObject(int _x, int _y)
	{
		// Get the block's data.
		BlockData block_data = GetBlockData(_x, _y);

		// If the block has no data or is not occupied by a nation, it does not contain an inert object.
		if ((block_data == null) || (block_data.nationID == -1)) {
			return false;
		}

		// Get the block's extended data
		BlockExtData block_ext_data = ((block_data.flags & BlockData.BF_EXTENDED_DATA) == 0) ? null : GetBlockExtendedData(_x, _y, false);

		// If the block doesn't have extended data, it does not contain an inert object.
		if (block_ext_data == null) {
			return false;
		}

		// Get the block's nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, block_data.nationID, false);

		if (nationData == null) {
			return false;
		}

		// If the block has a build object that is owned by the nation occupying the block, and that is complete...
		if ((block_ext_data != null) && (block_ext_data.objectID != -1) && (block_ext_data.owner_nationID == block_data.nationID) &&
			  ((block_ext_data.completion_time == -1) || (block_ext_data.completion_time < Constants.GetTime())))
		{
			// Get the build data for the object in this block.
			BuildData build_data = BuildData.GetBuildData(block_ext_data.objectID);

			// Storage and shard type builds cannot go inert.
			if ((build_data.type == BuildData.TYPE_ENERGY_STORAGE) || (build_data.type == BuildData.TYPE_MANPOWER_STORAGE) || (build_data.type == BuildData.TYPE_SHARD)) {
				return false;
			}

			return BlockIsInert(nationData, _x, _y);
		}

		return false;
	}

	public boolean BlockIsInert(NationData _nationData, int _x, int _y)
	{
		float inert_threshold = _nationData.GetFinalGeoEfficiency(ID);

		if ((inert_threshold == 1f) && ((ID == Constants.MAINLAND_MAP_ID) && (_nationData.energy > 0))) {
			return false;
		}

		float final_energy_rate = _nationData.GetFinalEnergyRate(ID);

		if ((inert_threshold == 1f) && (final_energy_rate >= _nationData.GetFinalEnergyBurnRate(ID))) {
			return false;
		}

		inert_threshold = Math.min(inert_threshold, final_energy_rate / _nationData.GetFinalEnergyBurnRate(ID));

    // Determine whether an object in this block should be inert.
	  return (xxhash.GetHashFloat(_x, _y) > inert_threshold);
	}

	public boolean BlockHasTransitionedToNationID(int _x, int _y, int _nationID, int _cur_time)
	{
		if ((_x < 0) || (_x >= width) || (_y < 0) || (_y >= height)) {
			return false;
		}

		BlockData block_data = blocks[_x][_y];

		return ((block_data.nationID == _nationID) && (block_data.transition_complete_time <= _cur_time));
	}

	public boolean BlockIsNationBorder(int _x, int _y, int _nationID)
	{
		// If the given block does not belong to the given nation, return false.
		if (GetBlockNationID(_x, _y) != _nationID) return false;

		// If any of the the block's surrounding blocks don't belong to the given nation, it is a border block. Return true.
		if (GetBlockNationID(_x-1, _y) != _nationID) return true;
		if (GetBlockNationID(_x+1, _y) != _nationID) return true;
		if (GetBlockNationID(_x, _y-1) != _nationID) return true;
		if (GetBlockNationID(_x, _y+1) != _nationID) return true;
		if (GetBlockNationID(_x-1, _y-1) != _nationID) return true;
		if (GetBlockNationID(_x-1, _y+1) != _nationID) return true;
		if (GetBlockNationID(_x+1, _y-1) != _nationID) return true;
		if (GetBlockNationID(_x+1, _y+1) != _nationID) return true;

		// The given block and all surrounding blocks belong to the given nation. It is an interior block. Return false.
		return false;
	}

	public int DetermineBlockPerimeter(int _x, int _y, int _nationID)
	{
		// If the given block does not belong to the given nation, return 0.
		if (GetBlockNationID(_x, _y) != _nationID) return 0;

		// For each of the four bordering blocks that don't belong to the same nation, increment perimeter.
		int perimeter = 0;
		if (GetBlockNationID(_x-1, _y) != _nationID) perimeter++;
		if (GetBlockNationID(_x+1, _y) != _nationID) perimeter++;
		if (GetBlockNationID(_x, _y-1) != _nationID) perimeter++;
		if (GetBlockNationID(_x, _y+1) != _nationID) perimeter++;

		return perimeter;
	}

	public float GetHashFloat(int _x, int _y)
	{
		return xxhash.GetHashFloat(_x, _y);
	}

	public int GetObjectCount(int _objectID)
	{
		return object_count.getOrDefault(_objectID, 0);
	}

	public HashMap<Integer,Integer> GetObjectCountMap()
	{
		return object_count;
	}

	public static void SetDatabase(Connection _db)
	{
		// Record the _db connection for later use.
		db = _db;
	}

	public static void DeleteBlocks(int _mapID)
	{
		String sql;

		// Delete block data
		sql = "DELETE FROM Block WHERE ID='" + _mapID + "'";
		BaseData.ExecuteUpdate(db, sql, false, true);

		// Delete block extended data
		sql = "DELETE FROM BlockExt WHERE ID='" + _mapID + "'";
		BaseData.ExecuteUpdate(db, sql, false, true);
	}


	// Put in system for recording when a landmap was last used, and to unload it from memory if it hasn't been used in a while.
	// Also put in a system to explicitly unload a landmap from memory, eg. when a copy is done being used.
	// Also may need to be a system for adding a copy to the DB, then removing it after it reaches a certain age, so as to allow replay of battles.
}
