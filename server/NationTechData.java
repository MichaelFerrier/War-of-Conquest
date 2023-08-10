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
import java.util.ArrayList;
import java.util.HashMap;
import WOCServer.*;

public class NationTechData extends BaseData
{
	public static String db_table_name = "NationTechs";

	public static int VERSION = 1;

	// Map of tech IDs to counts, for acquired techs.
	HashMap<Integer,Integer> tech_count = new HashMap<Integer,Integer>();

	// Map of tech IDs to expire times, for pending techs.
	HashMap<Integer,Integer> tech_temp_expire_time = new HashMap<Integer,Integer>();

	// The time of first capture of each object, keyed by the object's coordinates.
	HashMap<Integer,Integer> object_capture_history = new HashMap<Integer,Integer>();

	// Array of object coordinate tokens, for pending objects.
	ArrayList<Integer> pending_object_coords = new ArrayList<Integer>();

	// Transient:

	// Map of build IDs to bool representing whether that build is available.
	HashMap<Integer, Boolean> available_builds = new HashMap<Integer, Boolean>();

	// Map of build IDs to available upgrade build ID.
	HashMap<Integer, Integer> available_upgrades = new HashMap<Integer, Integer>();

	public NationTechData(int _ID)
	{
		super(Constants.DT_NATIONTECH, _ID);
	}

	public void Clear()
	{
		// Clear all appropriate data arrays and maps (don't clear pending techs).
		tech_count.clear();
		tech_temp_expire_time.clear();
	}

	public int GetTechCount(int _techID)
	{
		return (tech_count.containsKey(_techID) ? tech_count.get(_techID) : 0);
	}

	public void SetTechCount(int _techID, int _count)
	{
		if (_count > 0)
		{
			tech_count.put(_techID, Integer.valueOf(_count));
		}
		else
		{
			if (_count == 0) {
				tech_count.remove(_techID);
			}	else {
				tech_count.put(_techID, Integer.valueOf(0));
				throw new RuntimeException("ERROR: NationTechData.SetTechCount() called with _count < 0: " + _count);
			}
		}
	}
/*
	public int GetTechPendingCount(int _techID)
	{
		return (tech_pending_list.containsKey(_techID) ? tech_pending_list.get(_techID) : 0);
	}

	public void SetTechPendingCount(int _techID, int _count)
	{
		if (_count > 0)
		{
			tech_pending_list.put(_techID, Integer.valueOf(_count));
		}
		else
		{
			if (_count == 0) {
				tech_pending_list.remove(_techID);
			}	else {
				Output.PrintToScreen("ERROR: NationTechData.SetTechPendingCount() called with _count < 0: " + _count);
				tech_pending_list.put(_techID, Integer.valueOf(0));
			}
		}
	}
*/

	public void AddPendingObject(int _x, int _y)
	{
		pending_object_coords.add(Constants.TokenizeCoordinates(_x, _y));
	}

	public void RemovePendingObject(int _x, int _y)
	{
		pending_object_coords.remove(Integer.valueOf(Constants.TokenizeCoordinates(_x, _y)));
	}

	public boolean IsBuildAvailable(int _buildID)
	{
		return (available_builds.containsKey(_buildID) && available_builds.get(_buildID));
	}

	public int GetAvailableUpgrade(int _buildID)
	{
		return (available_upgrades.containsKey(_buildID) == false) ? -1 : available_upgrades.get(_buildID);
	}

	public boolean ReadData()
	{
		int file_version;
		boolean result = true;

		String sql = "SELECT " +
		"version," +
		"tech_count, " +
		"tech_temp_expire_time, " +
		"object_capture_history, " +
		"pending_object_coords " +
		"FROM " + db_table_name + " where ID= '" + ID + "'";

		try {
			// Create statement for use with the DB.
			Statement stmt = db.createStatement();

			// Get the result set for the sql query
			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				file_version  = rs.getInt("version");
				tech_count = JSONToIntIntMap(rs.getString("tech_count"));
				tech_temp_expire_time = JSONToIntIntMap(rs.getString("tech_temp_expire_time"));
				object_capture_history = JSONToIntIntMap(rs.getString("object_capture_history"));
				pending_object_coords = JSONToIntArray(rs.getString("pending_object_coords"));
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

		// Add the initial builds to the nation.
		AddInitialAvailableBuilds();

		// Determine available builds and upgrades
		for (Integer techID : tech_count.keySet()) {
			UpdateBuildsForAdvance(techID);
		}

		return result;
	}

	public void UpdateBuildsForAdvance(int _techID)
  {
		TechData tech_data = TechData.GetTechData(_techID);

		if (tech_data == null) {
			return;
		}

		if (tech_data.new_build != -1)
    {
      BuildData build_data = BuildData.GetBuildData(tech_data.new_build);
			if (build_data.upgrades != -1)
			{
        // Record what build the current technology's new_build is the available upgrade for.
				available_upgrades.put(build_data.upgrades, tech_data.new_build);
			}
			else
			{
				// Record that this tech's new_build is available (unless it's already been recorded as being obsolete).
				if (available_builds.containsKey(tech_data.new_build) == false) {
					available_builds.put(tech_data.new_build, true);
				}
			}
		}

		// Record that this tech's obsolete_build is not available.
		if (tech_data.obsolete_build != -1) {
			available_builds.put(tech_data.obsolete_build, false);
		}
	}

	public void AddInitialAvailableBuilds()
	{
		// Add the initial builds to the nation.
		for (Integer initial_buildID : BuildData.initial_builds) {
			available_builds.put(initial_buildID, true);
		}
	}

	public void WriteData()
	{
		String sql = "UPDATE " + db_table_name + " SET " +
		"version = '" + VERSION + "', " +
		"tech_count = '" + IntIntMapToJSON(tech_count) + "', " +
		"tech_temp_expire_time = '" + IntIntMapToJSON(tech_temp_expire_time) + "', " +
		"object_capture_history = '" + IntIntMapToJSON(object_capture_history) + "', " +
		"pending_object_coords = '" + IntArrayToJSON(pending_object_coords) + "' " +
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
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD tech_count TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD tech_temp_expire_time TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD object_capture_history MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD pending_object_coords TEXT", true, false);
	}

	public static void DeleteDBTable()
	{
		// Attempt to delete the DB table
		ExecuteUpdate(db, "DROP TABLE " + db_table_name, true, true);
	}
}
