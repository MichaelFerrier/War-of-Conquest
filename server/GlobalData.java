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

public class GlobalData extends BaseData
{
	public static String db_table_name = "Global";
	public static int VERSION = 1;

	public static GlobalData instance = null;

	int cur_data_update_count = 0;
	HashMap <String,Integer> data_file_versions = new HashMap <String,Integer>();
	float money_revenue = 0.0f;
	float game_money_awarded = 0.0f;
	HashMap<Integer,TechPriceRecord> tech_price_records = new HashMap<Integer,TechPriceRecord>();
	ArrayList<Integer> complaints = new ArrayList<Integer>();
	HashMap <Integer,Integer> map_modified_times = new HashMap <Integer,Integer>();
	int prev_subscription_modification_time;
	int prev_payment_count;
	int prev_prize_payment_count;
	int heartbeat;
	int cur_backup_period;
	int cur_goal_update_month;
	int cur_goal_update_period;
	int cur_quarter_hourly_update_period;
	int cur_hourly_update_period;
	int cur_daily_update_period;
	int cur_weekly_update_period;
	int cur_monthly_update_period;
	int cur_ranks_publish_period;

	public static void StartUp()
	{
		// Load the GlobalData singleton instance.
		instance = (GlobalData)DataManager.GetData(Constants.DT_GLOBAL, Constants.GLOBAL_DATA_ID, false);
	}

	public GlobalData(int _ID)
	{
		super(Constants.DT_GLOBAL, _ID);
	}

	public boolean ReadData()
	{
		int file_version;
		boolean result = true;

		String sql = "SELECT " +
		"version, " +
		"cur_data_update_count, " +
		"data_file_versions, " +
		"money_revenue, " +
		"game_money_awarded, " +
		"tech_price_records, " +
		"complaints, " +
		"map_modified_times, " +
		"prev_subscription_modification_time, " +
		"prev_payment_count, " +
		"prev_prize_payment_count, " +
		"heartbeat, " +
		"cur_backup_period, " +
		"cur_goal_update_month, " +
		"cur_goal_update_period, " +
		"cur_quarter_hourly_update_period, " +
		"cur_hourly_update_period, " +
		"cur_daily_update_period, " +
		"cur_weekly_update_period, " +
		"cur_monthly_update_period, " +
		"cur_ranks_publish_period " +
		"FROM " + db_table_name + " where ID= '" + ID + "'";

		try {
			// Create statement for use with the DB.
			Statement stmt = db.createStatement();

			// Get the result set for the sql query
			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				file_version  = rs.getInt("version");
				cur_data_update_count = rs.getInt("cur_data_update_count");
				data_file_versions = JSONToStringIntMap(rs.getString("data_file_versions"));
				money_revenue  = rs.getFloat("money_revenue");
				game_money_awarded  = rs.getFloat("game_money_awarded");
				tech_price_records = TechPriceRecord.JSONToTechPriceRecordMap(rs.getString("tech_price_records"));
				complaints = JSONToIntArray(rs.getString("complaints"));
				map_modified_times = JSONToIntIntMap(rs.getString("map_modified_times"));
				prev_subscription_modification_time = rs.getInt("prev_subscription_modification_time");
				prev_payment_count = rs.getInt("prev_payment_count");
				prev_prize_payment_count = rs.getInt("prev_prize_payment_count");
				heartbeat  = rs.getInt("heartbeat");
				cur_backup_period  = rs.getInt("cur_backup_period");
				cur_goal_update_month  = rs.getInt("cur_goal_update_month");
				cur_goal_update_period  = rs.getInt("cur_goal_update_period");
				cur_quarter_hourly_update_period  = rs.getInt("cur_quarter_hourly_update_period");
				cur_hourly_update_period  = rs.getInt("cur_hourly_update_period");
				cur_daily_update_period  = rs.getInt("cur_daily_update_period");
				cur_weekly_update_period  = rs.getInt("cur_weekly_update_period");
				cur_monthly_update_period  = rs.getInt("cur_monthly_update_period");
				cur_ranks_publish_period  = rs.getInt("cur_ranks_publish_period");
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
		"cur_data_update_count = '" + cur_data_update_count + "', " +
		"data_file_versions = '" + StringIntMapToJSON(data_file_versions) + "', " +
		"money_revenue = '" + money_revenue + "', " +
		"game_money_awarded = '" + game_money_awarded + "', " +
		"tech_price_records = '" + PrepStringForMySQL(TechPriceRecord.TechPriceRecordMapToJSON(tech_price_records)) + "', " +
		"complaints = '" + IntArrayToJSON(complaints) + "', " +
		"map_modified_times = '" + IntIntMapToJSON(map_modified_times) + "', " +
		"prev_subscription_modification_time = '" + prev_subscription_modification_time + "', " +
		"prev_payment_count = '" + prev_payment_count + "', " +
		"prev_prize_payment_count = '" + prev_prize_payment_count + "', " +
		"heartbeat = '" + heartbeat + "', " +
		"cur_backup_period = '" + cur_backup_period + "', " +
		"cur_goal_update_month = '" + cur_goal_update_month + "', " +
		"cur_goal_update_period = '" + cur_goal_update_period + "', " +
		"cur_quarter_hourly_update_period = '" + cur_quarter_hourly_update_period + "', " +
		"cur_hourly_update_period = '" + cur_hourly_update_period + "', " +
		"cur_daily_update_period = '" + cur_daily_update_period + "', " +
		"cur_weekly_update_period = '" + cur_weekly_update_period + "', " +
		"cur_monthly_update_period = '" + cur_monthly_update_period + "', " +
		"cur_ranks_publish_period = '" + cur_ranks_publish_period + "' " +
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
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD cur_data_update_count INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD data_file_versions TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD money_revenue INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD game_money_awarded INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD tech_price_records TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD complaints TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD map_modified_times TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD prev_subscription_modification_time INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD prev_payment_count INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD prev_prize_payment_count INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD heartbeat INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD cur_backup_period INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD cur_goal_update_month INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD cur_goal_update_period INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD cur_quarter_hourly_update_period INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD cur_hourly_update_period INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD cur_daily_update_period INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD cur_weekly_update_period INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD cur_monthly_update_period INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD cur_ranks_publish_period INT", true, false);
	}

	public static void DeleteDBTable()
	{
		// Attempt to delete the DB table
		ExecuteUpdate(db, "DROP TABLE " + db_table_name, true, true);
	}

	public TechPriceRecord GetTechPriceRecord(int _advance_id, boolean _create)
	{
		if (tech_price_records.containsKey(_advance_id))
		{
			return tech_price_records.get(_advance_id);
		}
		else
		{
			if (_create)
			{
				TechPriceRecord new_record = new TechPriceRecord();
				new_record.ID = _advance_id;
				tech_price_records.put(_advance_id, new_record);
				return new_record;
			}
			else
			{
				return null;
			}
		}
	}
}
