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
import java.util.HashMap;
import org.json.simple.*;
import WOCServer.*;

public abstract class BaseData
{
	int type;
	int ID;

	// Transient values
	boolean marked_for_update = false;

	static Connection db = null;

	public BaseData(int _type, int _ID)
	{
		super();

		type = _type;
		ID = _ID;
		marked_for_update = false;
	}

	public abstract boolean ReadData();

	public abstract void WriteData();

	public void CreateRecord()
	{
		String sql = "INSERT INTO " + GetTableName(type) + "(ID) VALUES(" + ID + ")";
		ExecuteUpdate(db, sql, false, true);
	}

	public static void DeleteRecord(int _type, int _ID)
	{
		String sql = "DELETE FROM " + GetTableName(_type) + " WHERE ID='" + _ID + "'";
		ExecuteUpdate(db, sql, false, true);
	}

	public static void DeleteAllRecords(int _type)
	{
		String sql = "DELETE FROM " + GetTableName(_type);
		ExecuteUpdate(db, sql, false, true);
	}

	public static int GetIDByName(int _type, String _name)
	{
		int result = -1;
		String sql = "SELECT * FROM " + GetTableName(_type) + " WHERE LOWER(name) = '" + BaseData.PrepStringForMySQL(_name.toLowerCase()) + "'";

		try {
			// Create statement for use with the DB.
			Statement stmt = db.createStatement();

			// Get the result set for the sql query
			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				result = rs.getInt("ID");
			}

			// Close statement and result set (must be done explicitly to avoid memory leak).
			try { if (rs != null) rs.close(); } catch (Exception e) {};
	    try { if (stmt != null) stmt.close(); } catch (Exception e) {};
		} catch(Exception e) {
      Output.PrintToScreen("GetIDByName() couldn't fetch ID in table " + GetTableName(_type) + ". Message: " + e.getMessage());
		}

		return result;
	}

	public static int GetHighestID(int _type)
	{
		int result = 0;

		// Create statement to determine highest ID of objects of the given type.
		String sql = "SELECT MAX(ID) AS ID FROM " + GetTableName(_type);

		try {
			// Create statement for use with the DB.
			Statement stmt = db.createStatement();

			// Get the result set for the sql query
			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				result = rs.getInt("ID");
			}

			// Close statement and result set (must be done explicitly to avoid memory leak).
			try { if (rs != null) rs.close(); } catch (Exception e) {};
	    try { if (stmt != null) stmt.close(); } catch (Exception e) {};
		} catch(Exception e) {
      Output.PrintToScreen("Couldn't fetch highest ID in table " + GetTableName(_type) + ". Message: " + e.getMessage());
		}

		return result;
	}

	public static void SetDatabase(Connection _db)
	{
		// Record the _db connection for later use.
		db = _db;
	}

	public static void CreateTable(Connection _db, String _db_table_name)
	{
		try {
			// Query db to determine whether the table called _db_table_name exists yet.
			Statement stmt = _db.createStatement();
			ResultSet resultSet = stmt.executeQuery("SHOW TABLES LIKE '" + _db_table_name + "'");

			// If no table with than name yet exists, attempt to create it.
			if(resultSet.next() == false)
			{
				String sql = "CREATE TABLE " + _db_table_name + " (ID INT not NULL, version INT, PRIMARY KEY (ID)) ENGINE = MyISAM ;";

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

	public static void ExecuteUpdate(Connection _db, String _sql, boolean _report_success, boolean _report_failure)
	{
		try {
			// Attempt to execute the given sql statement.
			Statement stmt = _db.createStatement();
			int result = stmt.executeUpdate(_sql);
			stmt.close();

			// If it successfully executed, the DB was modified. Print notifiction of this fact.
			if (_report_success) System.out.println("Executed statement: '" + _sql + "'.");
		}
		catch(Exception e) {
			if (_report_failure) Output.PrintToScreen("Statement resulted in exception: '" + _sql + "'. Message: " + e.getMessage());
		}
	}

	public static String GetTableName(int _type)
	{
		switch (_type)
		{
			case Constants.DT_GLOBAL: return GlobalData.db_table_name;
			case Constants.DT_BLOCK: return BlockData.db_table_name;
			case Constants.DT_BLOCK_EXT: return BlockExtData.db_table_name;
			case Constants.DT_NATION: return NationData.db_table_name;
			case Constants.DT_NATIONTECH: return NationTechData.db_table_name;
			case Constants.DT_NATION_EXT: return NationExtData.db_table_name;
			case Constants.DT_USER: return UserData.db_table_name;
			case Constants.DT_RANKS: return RanksData.db_table_name;
			case Constants.DT_DEVICE: return DeviceData.db_table_name;
			case Constants.DT_COMPLAINT: return ComplaintData.db_table_name;
			case Constants.DT_VOUCHER: return VoucherData.db_table_name;
			case Constants.DT_EMAIL: return EmailData.db_table_name;
			case Constants.DT_TOURNAMENT: return TournamentData.db_table_name;
			case Constants.DT_RAID: return RaidData.db_table_name;
			case Constants.DT_LANDMAPINFO: return LandMapInfoData.db_table_name;
			default:
				Output.PrintToScreen("GetTableName() given unknown type '" + _type + "'.");
				return "Unknown Type " + _type;
		}
	}

	public static String IntFloatMapToJSON(HashMap<Integer,Float> _map)
	{
		if (_map == null) {Output.PrintToScreen("IntFloatMapToJSON() passed null _map.");}

		JSONObject json = new JSONObject();
		json.putAll(_map);
		return json.toJSONString();
	}

	public static HashMap<Integer,Float> JSONToIntFloatMap(String _json)
	{
		HashMap<Integer,Float> return_map = new HashMap<Integer,Float>();

		if (_json == null) {Output.PrintToScreen("JSONToIntFloatMap() passed null _json String."); return return_map;}

		JSONObject json_object = (JSONObject)(JSONValue.parse(_json));

		// Note: putAll() doesn't work, because keys in JSONObject are seen as Strings, not ints.
		for(Iterator iterator = json_object.entrySet().iterator(); iterator.hasNext();)
		{
			Map.Entry pairs = (Map.Entry)iterator.next();
			Integer key = Integer.parseInt((String)pairs.getKey());
			return_map.put(key, ((Double)pairs.getValue()).floatValue());
		}

		return return_map;
	}

	public static String IntIntMapToJSON(HashMap<Integer,Integer> _map)
	{
		if (_map == null) {Output.PrintToScreen("IntIntMapToJSON() passed null _map.");}

		JSONObject json = new JSONObject();
		json.putAll(_map);
		return json.toJSONString();
	}

	public static HashMap<Integer,Integer> JSONToIntIntMap(String _json)
	{
		HashMap<Integer,Integer> return_map = new HashMap<Integer,Integer>();

		if (_json == null) {Output.PrintToScreen("JSONToIntIntMap() passed null _json String."); return return_map;}

		JSONObject json_object = (JSONObject)(JSONValue.parse(_json));

		// Note: putAll() doesn't work, because keys in JSONObject are seen as Strings, not ints.
		for(Iterator iterator = json_object.entrySet().iterator(); iterator.hasNext();)
		{
			Map.Entry pairs = (Map.Entry)iterator.next();
			Integer key = Integer.parseInt((String)pairs.getKey());
			return_map.put(key, ((Long)pairs.getValue()).intValue());
		}

		return return_map;
	}

	public static String StringIntMapToJSON(HashMap<String,Integer> _map)
	{
		if (_map == null) {Output.PrintToScreen("StringIntMapToJSON() passed null _map.");}

		JSONObject json = new JSONObject();
		json.putAll(_map);
		return json.toJSONString();
	}

	public static HashMap<String,Integer> JSONToStringIntMap(String _json)
	{
		HashMap<String,Integer> return_map = new HashMap<String,Integer>();

		if (_json == null) {Output.PrintToScreen("JSONToStringIntMap() passed null _json String."); return return_map;}

		JSONObject json_object = (JSONObject)(JSONValue.parse(_json));

		// Note: putAll() doesn't work, because keys in JSONObject are seen as Strings, not ints.
		for(Iterator iterator = json_object.entrySet().iterator(); iterator.hasNext();)
		{
			Map.Entry pairs = (Map.Entry)iterator.next();
			String key = (String)pairs.getKey();
			return_map.put(key, ((Long)pairs.getValue()).intValue());
		}

		return return_map;
	}

	public static String IntArrayToJSON(ArrayList<Integer> _array)
	{
		if (_array == null) {Output.PrintToScreen("IntArrayToJSON() passed null _array.");}

		JSONArray json = new JSONArray();
		int size = _array.size();
		for (int i = 0; i < size; i++)
		{
			json.add(_array.get(i));
		}

		return json.toJSONString();
	}

	public static ArrayList<Integer> JSONToIntArray(String _json)
	{
		ArrayList<Integer> return_array = new ArrayList<Integer>();

		if (_json == null) {Output.PrintToScreen("JSONToIntArray() passed null _json String."); return return_array;}

		JSONArray json_array = (JSONArray)(JSONValue.parse(_json));
		for (int i = 0; i < json_array.size(); i++)
		{
			return_array.add(((Number)(json_array.get(i))).intValue());
		}

		return return_array;
	}

	public static String FloatArrayToJSON(ArrayList<Float> _array)
	{
		if (_array == null) {Output.PrintToScreen("FloatArrayToJSON() passed null _array.");}

		JSONArray json = new JSONArray();
		int size = _array.size();
		for (int i = 0; i < size; i++)
		{
			json.add(_array.get(i));
		}

		return json.toJSONString();
	}

	public static ArrayList<Float> JSONToFloatArray(String _json)
	{
		ArrayList<Float> return_array = new ArrayList<Float>();

		if (_json == null) {Output.PrintToScreen("JSONToFloatArray() passed null _json String."); return return_array;}

		JSONArray json_array = (JSONArray)(JSONValue.parse(_json));
		for (int i = 0; i < json_array.size(); i++)
		{
			return_array.add(((Double)(json_array.get(i))).floatValue());
		}

		return return_array;
	}

	public static String StringArrayToJSON(ArrayList<String> _array)
	{
		if (_array == null) {Output.PrintToScreen("StringArrayToJSON() passed null _array.");}

		JSONArray json = new JSONArray();
		int size = _array.size();
		for (int i = 0; i < size; i++)
		{
			json.add(_array.get(i));
		}

		return json.toJSONString();
	}

	public static ArrayList<String> JSONToStringArray(String _json)
	{
		ArrayList<String> return_array = new ArrayList<String>();

		if (_json == null) {Output.PrintToScreen("JSONToStringArray() passed null _json String."); return return_array;}

		JSONArray json_array = (JSONArray)(JSONValue.parse(_json));
		for (int i = 0; i < json_array.size(); i++)
		{
			return_array.add((String)(json_array.get(i)));
		}

		return return_array;
	}

	public static String PrepStringForMySQL(String _string)
	{
		if (_string == null) {return "";}

		String result = _string.replace("'", "''");
		result = result.replace("\\", "\\\\");
		result = result.replace("\"", "\\\"");
		result = result.replace("\n", "\\n");
		result = result.replace("\r", "\\r");

		return result;
	}
}
