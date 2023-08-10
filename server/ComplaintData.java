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

public class ComplaintData extends BaseData
{
	public static String db_table_name = "Complaints";

	public static int VERSION = 1;

	int timestamp;
	int userID;
	int reported_userID;
	String issue;
	String text;

	public ComplaintData(int _ID)
	{
		super(Constants.DT_COMPLAINT, _ID);
	}

	public boolean ReadData()
	{
		int file_version;
		boolean result = true;

		String sql = "SELECT " +
		"version," +
		"timestamp, " +
		"userID, " +
		"reported_userID, " +
		"issue, " +
		"text " +
		"FROM " + db_table_name + " where ID= '" + ID + "'";

		try {
			// Create statement for use with the DB.
			Statement stmt = db.createStatement();

			// Get the result set for the sql query
			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				file_version  = rs.getInt("version");

				timestamp = rs.getInt("timestamp");
				userID = rs.getInt("userID");
				reported_userID = rs.getInt("reported_userID");
				issue = rs.getString("issue");
				text = rs.getString("text");
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
		"timestamp = '" + timestamp + "', " +
		"userID = '" + userID + "', " +
		"reported_userID = '" + reported_userID + "', " +
		"issue = '" + PrepStringForMySQL(issue) + "', " +
		"text = '" + PrepStringForMySQL(text) + "' " +
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
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD timestamp INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD userID INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD reported_userID INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD issue TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD text TEXT", true, false);
	}

	public static void DeleteDBTable()
	{
		// Attempt to delete the DB table
		ExecuteUpdate(db, "DROP TABLE " + db_table_name, true, true);
	}
}
