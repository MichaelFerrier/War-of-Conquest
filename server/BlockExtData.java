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
import WOCServer.*;

public class BlockExtData extends BaseData
{
	public static String db_table_name = "BlockExt";

	public static int VERSION = 1;

	boolean active = true; // Must be true upon creation so that if a DB update happens immediately, before active is set to true, it won't be removed from the extended_data HashMap.
	int objectID = -1;
	int owner_nationID = -1;
	int creation_time = -1;
	int completion_time = -1;
	int invisible_time = -1;
	int capture_time = -1;
	int crumble_time = -1;
	int wipe_flags = 0;
	int wipe_nationID = -1;
	int wipe_end_time = -1;
	int triggerable_time = -1;

	public BlockExtData()
	{
		super(Constants.DT_BLOCK_EXT, -1);
	}

	public void CopyData(BlockExtData _original)
	{
		objectID = _original.objectID;
		owner_nationID = _original.owner_nationID;
		creation_time = _original.creation_time;
		completion_time = _original.completion_time;
		invisible_time = _original.invisible_time;
		capture_time = _original.capture_time;
		crumble_time = _original.crumble_time;
		wipe_flags = _original.wipe_flags;
		wipe_nationID = _original.wipe_nationID;
		wipe_end_time = _original.wipe_end_time;
		triggerable_time = _original.triggerable_time;
	}

	public void ResetData()
	{
		objectID = -1;
		owner_nationID = -1;
		creation_time = -1;
		completion_time = -1;
		invisible_time = -1;
		capture_time = -1;
		crumble_time = -1;
		wipe_flags = 0;
		wipe_nationID = -1;
		wipe_end_time = -1;
		triggerable_time = -1;
	}

	// These are not implemented for BlockExtData -- reading and writing is handled by the LandMap class.
	public boolean ReadData() {return false;}
	public void WriteData() {}

	public static void CreateTable(Connection _db, String _db_table_name)
	{
		try {
			// Query db to determine whether the table called _db_table_name exists yet.
			Statement stmt = _db.createStatement();
			ResultSet resultSet = stmt.executeQuery("SHOW TABLES LIKE '" + _db_table_name + "'");

			// If no table with than name yet exists, attempt to create it.
			if(resultSet.next() == false)
			{
				String sql = "CREATE TABLE " + _db_table_name + " (ID INT not NULL, version INT, x INT, y INT, active BOOl, PRIMARY KEY (ID, x, y, active)) ENGINE = MyISAM ;";

				try {
					// Create the table for this data type
					stmt.executeUpdate(sql);
					System.out.println("Created table '" + _db_table_name + "'.");
				}
				catch(Exception e) {
					Output.PrintToScreen("Could not create table '" + _db_table_name + "'. Message: " + e.getMessage() + ". Exiting.");
					System.exit(1);
				}
			}

			// Close statement and result set (must be done explicitly to avoid memory leak).
			try { if (resultSet != null) resultSet.close(); } catch (Exception e) {};
	    try { if (stmt != null) stmt.close(); } catch (Exception e) {};
		}
		catch(Exception e) {
			Output.PrintToScreen("Could not determine whether table '" + _db_table_name + "' exists. Message: " + e.getMessage() + ". Exiting.");
			System.exit(1);
		}
	}

	public static void InitDBTable()
	{
		// Create the user data table, if it doesn't yet exist.
		CreateTable(db, db_table_name);

		// Attempt to create each column
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD active BOOL DEFAULT FALSE", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD objectID INT DEFAULT -1", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD owner_nationID INT DEFAULT -1", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD creation_time INT DEFAULT -1", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD completion_time INT DEFAULT -1", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD invisible_time INT DEFAULT -1", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD capture_time INT DEFAULT -1", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD crumble_time INT DEFAULT -1", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD wipe_flags INT DEFAULT 0", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD wipe_nationID INT DEFAULT -1", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD wipe_end_time INT DEFAULT -1", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD triggerable_time INT DEFAULT -1", true, false);
	}

	public static void DeleteDBTable()
	{
		// Attempt to delete the DB table
		ExecuteUpdate(db, "DROP TABLE " + db_table_name, true, true);
	}

	public void InitBuildInfo()
	{
		owner_nationID = -1;
		creation_time = -1;
		completion_time = -1;
		invisible_time = -1;
		capture_time = -1;
		crumble_time = -1;
		triggerable_time = -1;
	}
}
