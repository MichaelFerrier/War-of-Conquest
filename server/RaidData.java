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

public class RaidData extends BaseData
{
	public static String db_table_name = "Raid";
	public static int VERSION = 1;

	public static int REPLAY_BUFFER_LENGTH = 40000;

	public static final int RAID_FLAG_BEGUN = 1;
	public static final int RAID_FLAG_FINISHED = 2;
	public static final int RAID_FLAG_RED_SHARD = 4;
	public static final int RAID_FLAG_GREEN_SHARD = 8;
	public static final int RAID_FLAG_BLUE_SHARD = 16;
	public static final int RAID_FLAG_50_PERCENT = 32;
	public static final int RAID_FLAG_100_PERCENT = 64;
	public static final int RAID_REPLAY_AVAILABLE = 128;

	int landmapID = 0;
	int attacker_nationID = 0;
	int defender_nationID = 0;
	String attacker_nationName = "";
	String defender_nationName = "";
	int homeland_mapID = 0;
	int defender_starting_area = 0;
	int manpower_cost = 0;
	int flags = 0;
	int attacker_start_medals = 0;
	int defender_start_medals = 0;
	int percentage_defeated = 0;
	int attacker_0_star_medal_delta = 0;
	int attacker_5_star_medal_delta = 0;
	int defender_0_star_medal_delta = 0;
	int defender_5_star_medal_delta = 0;
	int max_reward_credits = 0;
	int max_reward_xp = 0;
	int max_reward_rebirth = 0;
	int attacker_reward_medals = 0;
	int defender_reward_medals = 0;
	int reward_credits = 0;
	int reward_xp = 0;
	int reward_rebirth = 0;
	int start_time = 0;
	int begin_time = 0;
	int end_time = 0;

	StringBuffer replay = new StringBuffer(REPLAY_BUFFER_LENGTH);

	Footprint attacker_footprint = new Footprint();
	Footprint defender_footprint = new Footprint();

	public RaidData(int _ID)
	{
		super(Constants.DT_RAID, _ID);
	}

	public boolean ReadData()
	{
		int file_version;
		boolean result = true;

		String sql = "SELECT " +
		"version, " +
		"landmapID, " +
		"attacker_nationID, " +
		"defender_nationID, " +
		"attacker_nationName, " +
		"defender_nationName, " +
		"homeland_mapID, " +
		"defender_starting_area, " +
		"manpower_cost, " +
		"flags, " +
		"attacker_start_medals, " +
		"defender_start_medals, " +
		"percentage_defeated, " +
		"attacker_0_star_medal_delta, " +
		"attacker_5_star_medal_delta, " +
		"defender_0_star_medal_delta, " +
		"defender_5_star_medal_delta, " +
		"max_reward_credits, " +
		"max_reward_xp, " +
		"max_reward_rebirth, " +
		"attacker_reward_medals, " +
		"defender_reward_medals, " +
		"reward_credits, " +
		"reward_xp, " +
		"reward_rebirth, " +
		"start_time, " +
		"begin_time, " +
		"end_time, " +
		"replay, " +
		"attacker_x0, " +
		"attacker_x1, " +
		"attacker_y0, " +
		"attacker_y1, " +
    "attacker_min_x0, " +
		"attacker_min_y0, " +
		"attacker_max_x1, " +
		"attacker_max_y1, " +
    "attacker_max_x0, " +
		"attacker_area, " +
		"attacker_border_area, " +
		"attacker_perimeter, " +
		"attacker_geo_efficiency_base, " +
		"attacker_energy_burn_rate, " +
		"attacker_manpower, " +
		"attacker_prev_buy_manpower_day, " +
		"attacker_buy_manpower_day_amount, " +
		"defender_x0, " +
		"defender_x1, " +
		"defender_y0, " +
		"defender_y1, " +
    "defender_min_x0, " +
		"defender_min_y0, " +
		"defender_max_x1, " +
		"defender_max_y1, " +
    "defender_max_x0, " +
		"defender_area, " +
		"defender_border_area, " +
		"defender_perimeter, " +
		"defender_geo_efficiency_base, " +
		"defender_energy_burn_rate, " +
		"defender_manpower, " +
		"defender_prev_buy_manpower_day, " +
		"defender_buy_manpower_day_amount " +
		"FROM " + db_table_name + " where ID= '" + ID + "'";

		try {
			// Create statement for use with the DB.
			Statement stmt = db.createStatement();

			// Get the result set for the sql query
			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				file_version  = rs.getInt("version");
				landmapID  = rs.getInt("landmapID");
				attacker_nationID  = rs.getInt("attacker_nationID");
				defender_nationID  = rs.getInt("defender_nationID");
				attacker_nationName = rs.getString("attacker_nationName");
				defender_nationName = rs.getString("defender_nationName");
				homeland_mapID  = rs.getInt("homeland_mapID");
				defender_starting_area  = rs.getInt("defender_starting_area");
				manpower_cost  = rs.getInt("manpower_cost");
				flags  = rs.getInt("flags");
				attacker_start_medals  = rs.getInt("attacker_start_medals");
				defender_start_medals  = rs.getInt("defender_start_medals");
				percentage_defeated  = rs.getInt("percentage_defeated");
				attacker_0_star_medal_delta  = rs.getInt("attacker_0_star_medal_delta");
				attacker_5_star_medal_delta  = rs.getInt("attacker_5_star_medal_delta");
				defender_0_star_medal_delta  = rs.getInt("defender_0_star_medal_delta");
				defender_5_star_medal_delta  = rs.getInt("defender_5_star_medal_delta");
				max_reward_credits  = rs.getInt("max_reward_credits");
				max_reward_xp  = rs.getInt("max_reward_xp");
				max_reward_rebirth  = rs.getInt("max_reward_rebirth");
				attacker_reward_medals  = rs.getInt("attacker_reward_medals");
				defender_reward_medals  = rs.getInt("defender_reward_medals");
				reward_credits  = rs.getInt("reward_credits");
				reward_xp  = rs.getInt("reward_xp");
				reward_rebirth  = rs.getInt("reward_rebirth");
				start_time  = rs.getInt("start_time");
				begin_time  = rs.getInt("begin_time");
				end_time  = rs.getInt("end_time");
				replay.setLength(0);
				replay.append(rs.getString("replay"));
				attacker_footprint.x0 = rs.getInt("attacker_x0");
				attacker_footprint.y0 = rs.getInt("attacker_y0");
				attacker_footprint.x1 = rs.getInt("attacker_x1");
				attacker_footprint.y1 = rs.getInt("attacker_y1");
				attacker_footprint.min_x0 = rs.getInt("attacker_min_x0");
				attacker_footprint.min_y0 = rs.getInt("attacker_min_y0");
				attacker_footprint.max_x1 = rs.getInt("attacker_max_x1");
				attacker_footprint.max_y1 = rs.getInt("attacker_max_y1");
				attacker_footprint.max_x0 = rs.getInt("attacker_max_x0");
				attacker_footprint.area = rs.getInt("attacker_area");
				attacker_footprint.border_area = rs.getInt("attacker_border_area");
				attacker_footprint.perimeter = rs.getInt("attacker_perimeter");
				attacker_footprint.geo_efficiency_base = rs.getFloat("attacker_geo_efficiency_base");
				attacker_footprint.energy_burn_rate = rs.getFloat("attacker_energy_burn_rate");
				attacker_footprint.manpower = rs.getInt("attacker_manpower");
				attacker_footprint.prev_buy_manpower_day = rs.getInt("attacker_prev_buy_manpower_day");
				attacker_footprint.buy_manpower_day_amount = rs.getInt("attacker_buy_manpower_day_amount");
				defender_footprint.x0 = rs.getInt("defender_x0");
				defender_footprint.y0 = rs.getInt("defender_y0");
				defender_footprint.x1 = rs.getInt("defender_x1");
				defender_footprint.y1 = rs.getInt("defender_y1");
				defender_footprint.min_x0 = rs.getInt("defender_min_x0");
				defender_footprint.min_y0 = rs.getInt("defender_min_y0");
				defender_footprint.max_x1 = rs.getInt("defender_max_x1");
				defender_footprint.max_y1 = rs.getInt("defender_max_y1");
				defender_footprint.max_x0 = rs.getInt("defender_max_x0");
				defender_footprint.area = rs.getInt("defender_area");
				defender_footprint.border_area = rs.getInt("defender_border_area");
				defender_footprint.perimeter = rs.getInt("defender_perimeter");
				defender_footprint.geo_efficiency_base = rs.getFloat("defender_geo_efficiency_base");
				defender_footprint.energy_burn_rate = rs.getFloat("defender_energy_burn_rate");
				defender_footprint.manpower = rs.getInt("defender_manpower");
				defender_footprint.prev_buy_manpower_day = rs.getInt("defender_prev_buy_manpower_day");
				defender_footprint.buy_manpower_day_amount = rs.getInt("defender_buy_manpower_day_amount");
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
		"landmapID = '" + landmapID + "', " +
		"attacker_nationID = '" + attacker_nationID + "', " +
		"defender_nationID = '" + defender_nationID + "', " +
		"attacker_nationName = '" + PrepStringForMySQL(attacker_nationName) + "', " +
		"defender_nationName = '" + PrepStringForMySQL(defender_nationName) + "', " +
		"homeland_mapID = '" + homeland_mapID + "', " +
		"defender_starting_area = '" + defender_starting_area + "', " +
		"manpower_cost = '" + manpower_cost + "', " +
		"flags = '" + flags + "', " +
		"attacker_start_medals = '" + attacker_start_medals + "', " +
		"defender_start_medals = '" + defender_start_medals + "', " +
		"percentage_defeated = '" + percentage_defeated + "', " +
		"attacker_0_star_medal_delta = '" + attacker_0_star_medal_delta + "', " +
		"attacker_5_star_medal_delta = '" + attacker_5_star_medal_delta + "', " +
		"defender_0_star_medal_delta = '" + defender_0_star_medal_delta + "', " +
		"defender_5_star_medal_delta = '" + defender_5_star_medal_delta + "', " +
		"max_reward_credits = '" + max_reward_credits + "', " +
		"max_reward_xp = '" + max_reward_xp + "', " +
		"max_reward_rebirth = '" + max_reward_rebirth + "', " +
		"attacker_reward_medals = '" + attacker_reward_medals + "', " +
		"defender_reward_medals = '" + defender_reward_medals + "', " +
		"reward_credits = '" + reward_credits + "', " +
		"reward_xp = '" + reward_xp + "', " +
		"reward_rebirth = '" + reward_rebirth + "', " +
		"start_time = '" + start_time + "', " +
		"begin_time = '" + begin_time + "', " +
		"end_time = '" + end_time + "', " +
		"replay = '" + PrepStringForMySQL(replay.toString()) + "', " +
		"attacker_x0 = '" + attacker_footprint.x0 + "', " +
		"attacker_y0 = '" + attacker_footprint.y0 + "', " +
		"attacker_x1 = '" + attacker_footprint.x1 + "', " +
		"attacker_y1 = '" + attacker_footprint.y1 + "', " +
		"attacker_min_x0 = '" + attacker_footprint.min_x0 + "', " +
		"attacker_min_y0 = '" + attacker_footprint.min_y0 + "', " +
		"attacker_max_x1 = '" + attacker_footprint.max_x1 + "', " +
		"attacker_max_y1 = '" + attacker_footprint.max_y1 + "', " +
		"attacker_max_x0 = '" + attacker_footprint.max_x0 + "', " +
		"attacker_area = '" + attacker_footprint.area + "', " +
		"attacker_border_area = '" + attacker_footprint.border_area + "', " +
		"attacker_perimeter = '" + attacker_footprint.perimeter + "', " +
		"attacker_geo_efficiency_base = '" + attacker_footprint.geo_efficiency_base + "', " +
		"attacker_energy_burn_rate = '" + attacker_footprint.energy_burn_rate + "', " +
		"attacker_manpower = '" + attacker_footprint.manpower + "', " +
		"attacker_prev_buy_manpower_day = '" + attacker_footprint.prev_buy_manpower_day + "', " +
		"attacker_buy_manpower_day_amount = '" + attacker_footprint.buy_manpower_day_amount + "', " +
		"defender_x0 = '" + defender_footprint.x0 + "', " +
		"defender_y0 = '" + defender_footprint.y0 + "', " +
		"defender_x1 = '" + defender_footprint.x1 + "', " +
		"defender_y1 = '" + defender_footprint.y1 + "', " +
		"defender_min_x0 = '" + defender_footprint.min_x0 + "', " +
		"defender_min_y0 = '" + defender_footprint.min_y0 + "', " +
		"defender_max_x1 = '" + defender_footprint.max_x1 + "', " +
		"defender_max_y1 = '" + defender_footprint.max_y1 + "', " +
		"defender_max_x0 = '" + defender_footprint.max_x0 + "', " +
		"defender_area = '" + defender_footprint.area + "', " +
		"defender_border_area = '" + defender_footprint.border_area + "', " +
		"defender_perimeter = '" + defender_footprint.perimeter + "', " +
		"defender_geo_efficiency_base = '" + defender_footprint.geo_efficiency_base + "', " +
		"defender_energy_burn_rate = '" + defender_footprint.energy_burn_rate + "', " +
		"defender_manpower = '" + defender_footprint.manpower + "', " +
		"defender_prev_buy_manpower_day = '" + defender_footprint.prev_buy_manpower_day + "', " +
		"defender_buy_manpower_day_amount = '" + defender_footprint.buy_manpower_day_amount + "' " +
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

		// TEMP -- MODIFY EXISTING COLUMNS
		//ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY attacker_geo_efficiency_base FLOAT", true, false);
		//ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY defender_geo_efficiency_base FLOAT", true, false);
		//ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY attacker_energy_burn_rate FLOAT", true, false);
		//ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY defender_energy_burn_rate FLOAT", true, false);

		// Attempt to create each column
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD landmapID INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD attacker_nationID INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD defender_nationID INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD attacker_nationName TINYTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD defender_nationName TINYTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD homeland_mapID INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD defender_starting_area INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD manpower_cost INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD flags INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD attacker_start_medals INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD defender_start_medals INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD percentage_defeated INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD attacker_0_star_medal_delta INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD attacker_5_star_medal_delta INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD defender_0_star_medal_delta INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD defender_5_star_medal_delta INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD max_reward_credits INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD max_reward_xp INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD max_reward_rebirth INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD attacker_reward_medals INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD defender_reward_medals INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD reward_credits INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD reward_xp INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD reward_rebirth INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD start_time INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD begin_time INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD end_time INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD replay TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD attacker_x0 INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD attacker_y0 INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD attacker_x1 INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD attacker_y1 INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD attacker_min_x0 INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD attacker_min_y0 INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD attacker_max_x1 INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD attacker_max_y1 INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD attacker_max_x0 INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD attacker_area INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD attacker_border_area INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD attacker_perimeter INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD attacker_geo_efficiency_base FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD attacker_energy_burn_rate FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD attacker_manpower INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD attacker_prev_buy_manpower_day INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD attacker_buy_manpower_day_amount INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD defender_x0 INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD defender_y0 INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD defender_x1 INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD defender_y1 INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD defender_min_x0 INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD defender_min_y0 INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD defender_max_x1 INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD defender_max_y1 INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD defender_max_x0 INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD defender_area INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD defender_border_area INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD defender_perimeter INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD defender_geo_efficiency_base FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD defender_energy_burn_rate FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD defender_manpower INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD defender_prev_buy_manpower_day INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD defender_buy_manpower_day_amount INT", true, false);
	}

	public static void DeleteDBTable()
	{
		// Attempt to delete the DB table
		ExecuteUpdate(db, "DROP TABLE " + db_table_name, true, true);
	}
}
