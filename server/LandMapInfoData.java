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

public class LandMapInfoData extends BaseData
{
	public static String db_table_name = "LandMapInfo";
	public static int VERSION = 1;

	int sourceMapID = -1;
	int skin = 1;
	ArrayList<Integer> beachheads_x = new ArrayList<Integer>();
	ArrayList<Integer> beachheads_y = new ArrayList<Integer>();

	public LandMapInfoData(int _ID)
	{
		super(Constants.DT_LANDMAPINFO, _ID);
		sourceMapID = _ID; // Default sourceMapID to be the same as the ID.
	}

	public boolean ReadData()
	{
		int file_version;
		boolean result = true;

		String sql = "SELECT " +
		"version, " +
		"sourceMapID, " +
		"skin, " +
		"beachheads_x, " +
		"beachheads_y " +
		"FROM " + db_table_name + " where ID= '" + ID + "'";

		try {
			// Create statement for use with the DB.
			Statement stmt = db.createStatement();

			// Get the result set for the sql query
			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				file_version  = rs.getInt("version");
				sourceMapID  = rs.getInt("sourceMapID");
				skin  = rs.getInt("skin");
				beachheads_x  = JSONToIntArray(rs.getString("beachheads_x"));
				beachheads_y  = JSONToIntArray(rs.getString("beachheads_y"));
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
		"sourceMapID = '" + sourceMapID + "', " +
		"skin = '" + skin + "', " +
		"beachheads_x = '" + IntArrayToJSON(beachheads_x) + "', " +
		"beachheads_y = '" + IntArrayToJSON(beachheads_y) + "' " +
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
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD sourceMapID INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD skin INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD beachheads_x TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD beachheads_y TEXT", true, false);
	}

	public static void DeleteDBTable()
	{
		// Attempt to delete the DB table
		ExecuteUpdate(db, "DROP TABLE " + db_table_name, true, true);
	}
}
