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
import java.sql.*;

class DeviceAccountData
{
	String name = "";
	String device_type = "";
	boolean veteran = false;
}

class DeviceDB
{
	static String db_table_name = "Devices";
	static Connection db = null;

	public static void SetDatabase(Connection _db)
	{
		// Record the _db connection for later use.
		db = _db;
	}

	public static void InitDBTable()
	{
		try {
			// Query db to determine whether the table called _db_table_name exists yet.
			Statement stmt = db.createStatement();
			ResultSet resultSet = stmt.executeQuery("SHOW TABLES LIKE '" + db_table_name + "'");

			// If no table with that name yet exists, attempt to create it.
			if(resultSet.next() == false)
			{
				String sql = "CREATE TABLE " + db_table_name + " (name VARCHAR(" + DeviceData.MAX_DEVICE_NAME_LEN + ") NOT NULL, PRIMARY KEY (name)) ENGINE = MyISAM ;";

				try {
					// Create the table for this data type
					stmt.executeUpdate(sql);
					stmt.close();
					System.out.println("Created table '" + db_table_name + "'.");
				}
				catch(Exception e) {
					Output.PrintToScreen("Could not create table '" + db_table_name + "'. Message: " + e.getMessage() + ". Exiting.");
					System.exit(1);
				}
			}

			// Close statement and result set (must be done explicitly to avoid memory leak).
			try { if (resultSet != null) resultSet.close(); } catch (Exception e) {};
	    try { if (stmt != null) stmt.close(); } catch (Exception e) {};
		}
		catch(Exception e) {
			Output.PrintToScreen("Could not determine whether table '" + db_table_name + "' exists. Message: " + e.getMessage() + ". Exiting.");
			System.exit(1);
		}

		// Add fields
		BaseData.ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD device_type VARCHAR(100)", true, false);
		BaseData.ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD veteran INT", true, false);
	}

	public static void DeleteDBTable()
	{
		// Attempt to delete the DB table
		BaseData.ExecuteUpdate(db, "DROP TABLE " + db_table_name, true, true);
	}

	public static void DeleteAllRecords()
	{
		String sql = "DELETE FROM " + db_table_name;
		BaseData.ExecuteUpdate(db, sql, false, true);
	}

	public static void DeleteDeviceAccount(String _deviceUID)
	{
		String sql = "DELETE FROM " + db_table_name + " where name= '" + BaseData.PrepStringForMySQL(_deviceUID) + "'";
		BaseData.ExecuteUpdate(db, sql, false, true);
	}

	public static DeviceAccountData CreateNewDeviceAccount(String _deviceUID, String _device_type)
	{
		// Create data for new device account.
		DeviceAccountData device_account = new DeviceAccountData();
		device_account.name = _deviceUID;
		device_account.device_type = _device_type;

		try {
			PreparedStatement pstmt = db.prepareStatement("INSERT INTO " + db_table_name + " (name, device_type) values('" + BaseData.PrepStringForMySQL(device_account.name) + "','" + BaseData.PrepStringForMySQL(device_account.device_type) + "')", java.sql.Statement.RETURN_GENERATED_KEYS);
			pstmt.execute();

			// Close statement (must be done explicitly to avoid memory leak).
	    try { if (pstmt != null) pstmt.close(); } catch (Exception e) {};
		}
		catch(Exception e) {
			Output.PrintToScreen("Could not add new device to account table '" + db_table_name + "'. Message: " + e.getMessage() + ". Exiting.");
			System.exit(1);
		}

		return device_account;
	}

	public static void WriteDeviceAccount(DeviceAccountData _device_account)
	{
		String sql = "UPDATE " + db_table_name + " SET " +
		"name = '" + BaseData.PrepStringForMySQL(_device_account.name) + "', " +
		"device_type = '" + BaseData.PrepStringForMySQL(_device_account.device_type) + "', " +
		"veteran = '" + (_device_account.veteran ? 1 : 0) + "' " +
		"WHERE name= '" + BaseData.PrepStringForMySQL(_device_account.name) + "'";

		//Output.PrintToScreen("Writing device account " + _device_account.name);

		try {
			// Create statement for use with the DB.
			Statement stmt = db.createStatement();

			// Execute the sql query
			stmt.executeUpdate(sql);
			stmt.close();
		}
	  catch(Exception e) {
      Output.PrintToScreen("Could not store object with name " + _device_account.name + " in table '" + db_table_name + "'. Message: " + e.getMessage());
		}
	}

	public static DeviceAccountData ReadDeviceAccount(String _deviceUID)
	{
		String sql = "SELECT " +
		"name," +
		"device_type," +
		"veteran " +
		"FROM " + db_table_name + " where name= '" + BaseData.PrepStringForMySQL(_deviceUID) + "'";

		return ReadDeviceAccountBySQL(sql);
	}

	public static DeviceAccountData ReadDeviceAccountBySQL(String _sql)
	{
		DeviceAccountData device = new DeviceAccountData();

		try {
			// Create statement for use with the DB.
			Statement stmt = db.createStatement();

			// Get the result set for the sql query
			ResultSet rs = stmt.executeQuery(_sql);

			if (rs.next()) {
				device.name  = rs.getString("name");
				device.device_type  = rs.getString("device_type");
				device.veteran  = (rs.getInt("veteran") != 0);
				if ((device.name == null) || (device.name.equals("null"))) device.name = "";
				if ((device.device_type == null) || (device.device_type.equals("null"))) device.device_type = "";
			} else {
				device = null;
			}

			// Close statement and result set (must be done explicitly to avoid memory leak).
			try { if (rs != null) rs.close(); } catch (Exception e) {};
	    try { if (stmt != null) stmt.close(); } catch (Exception e) {};
		}
	  catch(Exception e)
		{
			Output.PrintException(e);
      Output.PrintToScreen("Could not fetch object from table '" + db_table_name + "' with SQL '" + _sql + "'. Message: " + e.getMessage());
			device = null;
		}

		return device;
	}
}
