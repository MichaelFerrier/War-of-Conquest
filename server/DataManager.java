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
import WOCServer.Output;
import WOCServer.Constants;
import WOCServer.Semaphore;

public class DataManager
{
	// Database connections
	static public Connection account_db = null, game_db = null;

	// Cache maps
	static public List<ConcurrentHashMap<Integer, BaseData>> data_cache = new ArrayList<ConcurrentHashMap<Integer, BaseData>>();
	static public ConcurrentHashMap<Integer, LandMap> landmap_cache;

	// Update maps
	static public List<ConcurrentHashMap<Integer, BaseData>> data_to_update = new ArrayList<ConcurrentHashMap<Integer, BaseData>>();
	static public ConcurrentHashMap<Integer, LandMap> landmaps_to_update;

	// Highest ID for each data type
	static public int highest_ID[] = new int[Constants.DT_NUM_TYPES];

	static int prev_update_database_time = 0;
	static boolean database_update_in_progress = false;

	public static boolean DatabaseUpdateInProgress()
	{
		return database_update_in_progress;
	}

	public static void Init()
	{
		// Open database connections
		OpenDBConnections();

		// Initialize databases
		InitDBs();

    // Initialize data cache maps
		for (int i = 0; i < Constants.DT_NUM_TYPES; i++) {
			data_cache.add(i, new ConcurrentHashMap<Integer, BaseData>());
		}

		// Initialize landmap cache map
		landmap_cache = new ConcurrentHashMap<Integer, LandMap>();

    // Initialize data update maps
		for (int i = 0; i < Constants.DT_NUM_TYPES; i++) {
			data_to_update.add(i, new ConcurrentHashMap<Integer, BaseData>());
		}

		// Initialize landmap update map
		landmaps_to_update = new ConcurrentHashMap<Integer, LandMap>();

		// Initialize prev_update_database_time
		prev_update_database_time = Constants.GetTime();
	}

	public static void InitData()
	{
		boolean update_database = false;

		// If the global data object doesn't yet exist, create it.
		if (GetHighestDataID(Constants.DT_GLOBAL) == 0)
		{
			// Initialize the global data
			GlobalData globalData = (GlobalData)GetData(Constants.DT_GLOBAL, GetNextDataID(Constants.DT_GLOBAL), true);
			globalData.cur_backup_period = 0;

			// Mark the global data to be updated to the DB.
			MarkForUpdate(globalData);
			update_database = true;

			Output.PrintToScreen("Initialized GlobalData.");
		}

		// If the ranks data object doesn't yet exist, create it.
		if (GetHighestDataID(Constants.DT_RANKS) == 0)
		{
			// Initialize the ranks data
			RanksData ranksData = (RanksData)GetData(Constants.DT_RANKS, GetNextDataID(Constants.DT_RANKS), true);

			// Mark the ranks data to be updated to the DB.
			MarkForUpdate(ranksData);
			update_database = true;

			Output.PrintToScreen("Initialized RanksData.");
		}

		// If the tournament data object doesn't yet exist, create it.
		if (GetHighestDataID(Constants.DT_TOURNAMENT) == 0)
		{
			// Initialize the tournament data
			TournamentData tournamentData = (TournamentData)GetData(Constants.DT_TOURNAMENT, GetNextDataID(Constants.DT_TOURNAMENT), true);

			// Mark the tournament data to be updated to the DB.
			MarkForUpdate(tournamentData);
			update_database = true;

			Output.PrintToScreen("Initialized TournamentData.");
		}

		// Call StartUp on any data classes that require it to instantiate a singleton.
		GlobalData.StartUp();
		RanksData.StartUp();
		TournamentData.StartUp();

		// Update the DB if necessary.
		if (update_database) {
			UpdateDatabase(false);
		}
	}

	public static void OpenDBConnections()
	{
		try {
   		// Register JDBC driver
	    Class.forName("com.mysql.jdbc.Driver");

      // Open connection to account database
      account_db = DriverManager.getConnection(Constants.account_db_url, Constants.account_db_user, Constants.account_db_pass);

      // Open connection to game database
      game_db = DriverManager.getConnection(Constants.game_db_url, Constants.game_db_user, Constants.game_db_pass);
   }
	 catch(Exception e) {
      Output.PrintToScreen("Unable to open database connections.");
			Output.PrintException(e);
			System.exit(1);
   }

	 // Pass the game database connection to other classes that will use it.
	 BaseData.SetDatabase(game_db);
	 LandMap.SetDatabase(game_db);
	 AccountDB.SetDatabase(account_db);
	 DeviceDB.SetDatabase(account_db);
	}

	public static void InitDBs()
	{
		Statement account_db_stmt = null, game_db_stmt = null;

		// ACCOUNTS DB

		try {
			// Create statement for use with accounts DB.
			account_db_stmt = account_db.createStatement();
		}
	  catch(Exception e) {
      Output.PrintToScreen("Could not create statement for connection to accounts database.");
			Output.PrintException(e);
			System.exit(1);
		}

		try {
			// Attempt to create the ACCOUNTS database. This fails if it already exists.
      account_db_stmt.executeUpdate("CREATE DATABASE ACCOUNTS");
      System.out.println("ACCOUNTS DB created.");
		}
	  catch(Exception e) {
      Output.PrintToScreen("ACCOUNTS DB not created: " + e.getMessage());
    }

		try {
			// Attempt to select the ACCOUNTS database.
      account_db_stmt.executeUpdate("USE ACCOUNTS");
		}
	  catch(Exception e) {
      Output.PrintToScreen("Could not select the ACCOUNTS DB.");
    }

		// Game server DB

		try {
			// Create statement for use with game server DB.
			game_db_stmt = game_db.createStatement();
		}
	  catch(Exception e) {
      Output.PrintToScreen("Could not create statement for connection to game database.");
			Output.PrintException(e);
			System.exit(1);
		}

		try {
			// Attempt to create the game server database. This fails if it already exists.
      game_db_stmt.executeUpdate("CREATE DATABASE " + Constants.server_db_name);
      System.out.println(Constants.server_db_name + " DB created.");
		}
	  catch(Exception e) {
      Output.PrintToScreen(Constants.server_db_name + " DB not created: " + e.getMessage());
    }

		try {
			// Attempt to select the game server database.
      game_db_stmt.executeUpdate("USE " + Constants.server_db_name);
		}
	  catch(Exception e) {
      Output.PrintToScreen("Could not select the '" + Constants.server_db_name + "' DB.");
    }

		// Initialize account DB table for each data type
		AccountDB.InitDBTable();
		DeviceDB.InitDBTable();

		// Initialize game server DB table for each data type
		GlobalData.InitDBTable();
		UserData.InitDBTable();
		NationData.InitDBTable();
		NationTechData.InitDBTable();
		NationExtData.InitDBTable();
		RanksData.InitDBTable();
		DeviceData.InitDBTable();
		ComplaintData.InitDBTable();
		VoucherData.InitDBTable();
		EmailData.InitDBTable();
		TournamentData.InitDBTable();
		RaidData.InitDBTable();
		LandMapInfoData.InitDBTable();
		BlockData.InitDBTable();
		BlockExtData.InitDBTable();

		// Determine the highest ID in the DB of each data type.
		for (int type = 0; type < Constants.DT_NUM_TYPES; type++) {
			highest_ID[type] = BaseData.GetHighestID(type);
		}

		// Close statements
		try {account_db_stmt.close();} catch (Exception e) {};
		try {game_db_stmt.close();} catch (Exception e) {};
	}

	public static void DeleteAccountDB()
	{
		AccountDB.DeleteDBTable();
		DeviceDB.DeleteDBTable();
	}

	public static void DeleteGameDB()
	{
		// Delete game server DB table for each data type
		GlobalData.DeleteDBTable();
		UserData.DeleteDBTable();
		NationData.DeleteDBTable();
		NationTechData.DeleteDBTable();
		NationExtData.DeleteDBTable();
		RanksData.DeleteDBTable();
		DeviceData.DeleteDBTable();
		ComplaintData.DeleteDBTable();
		VoucherData.DeleteDBTable();
		EmailData.DeleteDBTable();
		TournamentData.DeleteDBTable();
		RaidData.DeleteDBTable();
		LandMapInfoData.DeleteDBTable();
		BlockData.DeleteDBTable();
		BlockExtData.DeleteDBTable();
	}

	public static void ClearDatabase()
	{
		// Clear the cached LandMaps
		LandMap land_map;
		for (Integer i : landmap_cache.keySet())
		{
			land_map = landmap_cache.get(i);
			land_map.Clear();
    }

		landmap_cache.clear();

		// Delete all data from the database.
		for (int type = 0; type < Constants.DT_NUM_TYPES; type++)
		{
			DataManager.ClearCache(type);
			BaseData.DeleteAllRecords(type);
			DataManager.highest_ID[type] = 0;
		}

		// Have the DataManager reset its update queue, so no objects will be updated.
		DataManager.ResetUpdateQueue();

		// Initialize data
		DataManager.InitData();
	}

	public static BaseData GetData(int _type, int _ID, boolean _create)
	{
		// Fetch the data object with the given ID from the cache map for the given data type.
		BaseData data = data_cache.get(_type).get(_ID);

		// If the data object was in the cache, return it.
		if (data != null) {
			return data;
		}

		// Create a new object of the given type.
		switch (_type)
		{
			case Constants.DT_GLOBAL:
				data = new GlobalData(_ID);
				break;
			case Constants.DT_NATION:
				data = new NationData(_ID);
				break;
			case Constants.DT_NATIONTECH:
				data = new NationTechData(_ID);
				break;
			case Constants.DT_NATION_EXT:
				data = new NationExtData(_ID);
				break;
			case Constants.DT_USER:
				data = new UserData(_ID);
				break;
			case Constants.DT_RANKS:
				data = new RanksData(_ID);
				break;
			case Constants.DT_DEVICE:
				data = new DeviceData(_ID);
				break;
			case Constants.DT_COMPLAINT:
				data = new ComplaintData(_ID);
				break;
			case Constants.DT_VOUCHER:
				data = new VoucherData(_ID);
				break;
			case Constants.DT_EMAIL:
				data = new EmailData(_ID);
				break;
			case Constants.DT_TOURNAMENT:
				data = new TournamentData(_ID);
				break;
			case Constants.DT_RAID:
				data = new RaidData(_ID);
				break;
			case Constants.DT_LANDMAPINFO:
				data = new LandMapInfoData(_ID);
				break;
			default:
				Output.PrintToScreen("ERROR: DataManager.GetData() BaseData unknown type: " + _type);
				return null;
		}

		// Read in the data
		boolean read_success = data.ReadData();

		// If a record for this object does not exist in the DB, and if _create is true, then create the record.
		if ((read_success == false) && _create)
		{
			highest_ID[_type] = Math.max(highest_ID[_type], _ID);
			data.CreateRecord();
		}

		// Add the base_data to the cache and return it, if appropriate
		if (read_success || _create)
		{
			data_cache.get(_type).put(_ID, data);
			return data;
		}

		// The object did not exist in the cache or the DB, and _create is false. Return null.
		return null;
	}

	public static LandMap GetLandMap(int _landmapID, boolean _create)
	{
		// Fetch the land map with the given ID from the cache.
		LandMap land_map = landmap_cache.get(_landmapID);

		// If the land map object was in the cache, return it.
		if (land_map != null) {
			return land_map;
		}

		// Create a new land map object.
		land_map = new LandMap(_landmapID);

		// Read in the data
		boolean read_success = land_map.LoadFromDB();

		// Add the landmap to the cache and return it, if appropriate
		if (read_success || _create)
		{
			// Fetch this landmap's corresponding LandMapInfo.
			land_map.info = (LandMapInfoData)DataManager.GetData(Constants.DT_LANDMAPINFO, _landmapID, true);
			//Output.PrintToScreen("Map " + _landmapID + " sourceMapID: " + land_map.info.sourceMapID);
/*
			// TEMP -- FOR FIXING OLD HOMELAND MAPS
			if ((land_map.info.sourceMapID <= 0) || (land_map.info.skin <= 0))
			{
				land_map.info.sourceMapID = (_landmapID == Constants.MAINLAND_MAP_ID) ? 1 : ((_landmapID % 15) + 2);
				land_map.info.skin = (_landmapID == Constants.MAINLAND_MAP_ID) ? 0 : 1;
				DataManager.MarkForUpdate(land_map.info);
			}
*/
			// Add this landmap to the cache.
			landmap_cache.put(_landmapID, land_map);

			return land_map;
		}

		// The land_map object did not exist in the cache or the DB, and _create is false. Return null.
		return null;
	}

	public static BlockData GetBlockDataForUser(int _userID, int _blockX, int _blockY, boolean _mark_for_update)
	{
		UserData userdata = (UserData)GetData(Constants.DT_USER, _userID, false);
		return GetBlockData(userdata.mapID, _blockX, _blockY, _mark_for_update);
	}

	public static BlockData GetBlockData(int _landmapID, int _blockX, int _blockY, boolean _mark_for_update)
	{
		LandMap land_map = GetLandMap(_landmapID, false);
		return GetBlockData(land_map, _blockX, _blockY, _mark_for_update);
	}

	public static BlockData GetBlockData(LandMap _land_map, int _blockX, int _blockY, boolean _mark_for_update)
	{
		if (_land_map != null) {
			if (_mark_for_update) {
				MarkBlockForUpdate(_land_map, _blockX, _blockY);
			}
			return _land_map.blocks[_blockX][_blockY];
		} else 	{
			return null;
		}
	}

	public static void UpdateImmediately(BaseData _data)
	{
		// Write the data object to the database.
		_data.WriteData();
	}

	public static void MarkForUpdate(BaseData _data)
	{
		// If this object is already marked to be updated, return.
		if (_data.marked_for_update){
			return;
		}

		// Mark this object to be updated
		_data.marked_for_update = true;

		// Add the given object to the map of data objects to update.
		data_to_update.get(_data.type).put(_data.ID, _data);
	}

	public static void MarkBlockForUpdate(int _mapID, int _blockX, int _blockY)
	{
		// Get the landmap with the given ID from the cache.
		LandMap land_map = landmap_cache.get(_mapID);

		// If the landmap is in the cache, mark its given block to be updated.
		if (land_map != null) {
			MarkBlockForUpdate(land_map, _blockX, _blockY);
		}
	}

	public static void MarkBlockForUpdate(LandMap _land_map, int _blockX, int _blockY)
	{
		if (_land_map.marked_for_update == false)
		{
			// Mark the _land_map to be updated.
			_land_map.marked_for_update = true;

			// Add the given _land_map to the map of land map objects to update.
			landmaps_to_update.put(_land_map.ID, _land_map);
		}

		// Mark the _land_map's given block to update.
		_land_map.MarkBlockForUpdate(_blockX, _blockY);
	}

	public static void ResetUpdateQueue()
	{
    // Clear data update maps
		for (int i = 0; i < Constants.DT_NUM_TYPES; i++) {
			data_to_update.get(i).clear();
		}

		// Clear landmap update map
		landmaps_to_update.clear();
	}

	public static void UpdateDatabase(boolean _force_immediate)
	{
		long start_time = Constants.GetFreshFineTime();
		int num_records_updated = 0;
		int ID;
		BaseData data;
		LandMap land_map;
		Iterator it;

		Output.PrintToScreen("UpdateDatabase() start");

		if (!_force_immediate)
		{
			// Wait until a database update is no longer in progress
			while (database_update_in_progress)
			{
				// Sleep for a while
				try{
					Thread.sleep(Constants.UPDATE_DATABASE_WAIT_SLEEP_MILLISECONDS);
				}catch(java.lang.InterruptedException e){Output.PrintTimeToScreen("UpdateDatabase Insomnia");}
			}
		}

		// Record that a database update is in progress
		database_update_in_progress = true;

		try {
			// For each data type...
			for (int type = 0; type < Constants.DT_NUM_TYPES; type++)
			{
				// For each object of this data type that needs to be updated...
				it = data_to_update.get(type).entrySet().iterator();
				while (it.hasNext())
				{
					Map.Entry<Integer, BaseData> entry = (Map.Entry<Integer, BaseData>)it.next();

					// Get the data object to be updated.
					data = (BaseData)entry.getValue();

					// Write the data object to the database, and mark it as no longer needing to be updated.
					data.WriteData();
					data.marked_for_update = false;

					// Remove the current entry from the list of objects to update.
					it.remove();

					// Increment number of records updated
					num_records_updated++;

					if ((num_records_updated % 50) == 0)
					{
						// Sleep for a while
						try{
							Thread.sleep(Constants.UPDATE_DATABASE_SLEEP_MILLISECONDS);
						}catch(java.lang.InterruptedException e){Output.PrintTimeToScreen("UpdateDatabase Insomnia");}
					}

					if ((num_records_updated % 1000) == 0) {
						Output.PrintTimeToScreen("UpdateDatabase updated " + num_records_updated + " records.");
					}
				}

				//if (data_to_update.get(type).size() > 0) {
				//	Output.PrintToScreen("NOTE: UpdateDatabase() writing objects of type " + type + ", after writing there are " + data_to_update.get(type).size() + " objects in the list. This should be okay, as they will be updated next time.");
				//}

				// NOTE: Do NOT clear the list of objects to be updated. Doing so, if any were added to the list during the update process, could result in those objects being marked for update but not being in the list, and so never updated again!
			}

			// For each landmap that needs to be updated...
			it = landmaps_to_update.entrySet().iterator();
			while (it.hasNext())
			{
				Map.Entry<Integer, LandMap> entry = (Map.Entry<Integer, LandMap>)it.next();

				// Get the land map to be updated.
				land_map = (LandMap)entry.getValue();

				// Write the marked blocks of the LandMap object to the database, and mark it as no longer needing to be updated.
				land_map.UpdateMarkedBlocks();
				land_map.marked_for_update = false;

				// Remove the current entry from the list of landmaps to update.
				it.remove();

				// Increment number of records updated
				num_records_updated++;

				if ((num_records_updated % 50) == 0)
				{
					// Sleep for a while
					try{
						Thread.sleep(Constants.UPDATE_DATABASE_SLEEP_MILLISECONDS);
					}catch(java.lang.InterruptedException e){Output.PrintTimeToScreen("UpdateDatabase Insomnia");}
				}

				if ((num_records_updated % 1000) == 0) {
					Output.PrintTimeToScreen("UpdateDatabase updated " + num_records_updated + " records.");
				}
			}

			// NOTE: Do NOT clear the list of landmaps to be updated. Doing so, if any were added to the list during the update process, could result in those objects being marked for update but not being in the list, and so never updated again!
		}
		catch (Exception e) {
			Output.PrintToScreen("Exception during UpdateDatabase(), message: " + e.getMessage() + ", stack trace: ");
			e.printStackTrace(System.out);
		}

		// Record that a database update is no longer in progress
		database_update_in_progress = false;

		// Record time when latest database update completed.
		prev_update_database_time = Constants.GetTime();

		Output.PrintToScreen("UpdateDatabase() end at " + prev_update_database_time + " (" + num_records_updated + " records updated)");
	}

	public static void DeleteData(int _type, int _ID)
	{
		// Remove the data object from the cache.
		RemoveFromCache(_type, _ID);

		// Delete the data object from the DB.
		BaseData.DeleteRecord(_type, _ID);
	}

	public static void DeleteLandMap(int _ID)
	{
		// Remove the land map from the cache.
		RemoveLandMapFromCache(_ID);

		// Delete the land map's block records from the DB.
		LandMap.DeleteBlocks(_ID);
	}

	public static void RemoveFromCache(int _type, int _ID)
	{
		if ((_type < 0) || (_type >= Constants.DT_NUM_TYPES)) {
			Output.PrintToScreen("RemoveFromCache(): Unknown data type " + _type);
		}

		data_cache.get(_type).remove(_ID);
	}

	public static void RemoveLandMapFromCache(int _ID)
	{
		landmap_cache.remove(_ID);
	}

	public static void DeleteAll(int _type)
	{
		ClearCache(_type);
		BaseData.DeleteAllRecords(_type);
		highest_ID[_type] = 0;
	}

	public static void ClearCache(int _type)
	{
		data_cache.get(_type).clear();
		data_to_update.get(_type).clear();
	}

	public static int GetHighestDataID(int _data_type)
	{
		return highest_ID[_data_type];
	}

	public static int GetNextDataID(int _data_type)
	{
		// Increment highest ID for the given data type.
		highest_ID[_data_type]++;

		// Return new highest ID.
		return highest_ID[_data_type];
	}

	public static int GetNameToIDMap(int _data_type, String _name)
	{
		return BaseData.GetIDByName(_data_type, _name);
	}
};
