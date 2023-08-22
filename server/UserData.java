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
import java.util.*;
import WOCServer.*;

public class UserData extends BaseData
{
	public static String db_table_name = "Users";

	public static int VERSION = 1;

	public enum ReportVal
	{
		report__defenses_squares_defeated,
		report__defenses_XP,
		report__defenses_lost,
		report__defenses_built,
		report__walls_lost,
		report__walls_built,
		report__attacks_squares_captured,
		report__attacks_XP,
		report__levels_gained,
		report__orb_count_delta,
		report__orb_credits,
		report__orb_XP,
		report__farming_XP,
		report__resource_count_delta,
		report__land_lost,
		report__energy_begin,
		report__energy_spent,
		report__energy_lost_to_raids,
		report__energy_donated,
		report__manpower_begin,
		report__manpower_spent,
		report__manpower_lost_to_resources,
		report__manpower_lost_to_raids,
		report__manpower_donated,
		report__credits_begin,
		report__credits_spent,
		report__patron_XP,
		report__patron_credits,
		report__follower_XP,
		report__follower_credits,
		report__follower_count,
		report__raids_fought,
		report__medals_delta,
		report__rebirth,
		report__home_defense_credits,
		report__home_defense_xp,
		report__home_defense_rebirth
	}

	int playerID;
	int creation_time = 0;
	boolean admin = false;
	boolean veteran = false;
	int mod_level = 0;
	int home_nationID = 0;

	int nationID = 0;
	int rank = 0;
	int flags = 0;

 	String name = "";
	String email = "";
	String patron_code = "";
	String tutorial_state = "";
	String creation_device_type = "";

  int game_ban_end_time = 0;
  int chat_ban_end_time = 0;
 	int prev_report_day = 0;
	int report_count = 0;
  float chat_offense_level = 0;

	ArrayList<Integer> muted_users = new ArrayList<Integer>();
	ArrayList<Integer> muted_devices = new ArrayList<Integer>();

	ArrayList<Integer> associated_users = new ArrayList<Integer>();
	HashMap<Integer,Integer> devices = new HashMap<Integer,Integer>();

	HashMap<Integer,Integer> long_term_reports = new HashMap<Integer,Integer>();
	HashMap<Integer,Integer> short_term_reports = new HashMap<Integer,Integer>();

	HashMap<Integer,Integer> contacts = new HashMap<Integer,Integer>();

	ArrayList<Integer> patron_offers = new ArrayList<Integer>();
	ArrayList<FollowerData> followers = new ArrayList<FollowerData>();
	int patronID;
	float cur_month_patron_bonus_XP;
	float cur_month_patron_bonus_credits;
	float prev_month_patron_bonus_XP;
	float prev_month_patron_bonus_credits;
	float total_patron_xp_received;
	float total_patron_credits_received;
	int max_num_followers;
	int max_num_followers_monthly;

	long mean_chat_interval = 0;
	long prev_chat_fine_time = 0;

	int mapID = -1;
	int mainland_viewX = 0, mainland_viewY = 0;
	int homeland_viewX = 0, homeland_viewY = 0;
	int raidland_viewX = 0, raidland_viewY = 0;

	float xp = 0;
	float xp_monthly = 0;
	int xp_monthly_month = 0;

	int prev_update_contacts_day = 0;
	int prev_check_messages_time = 0;

	int login_count = 0;
	int prev_login_time = 0;
  int prev_logout_time = 0;
	int play_time = 0;

	int ad_bonus_available = 0;

	// Subscription information
	boolean subscribed = false; // Is a subscription currently in effect for this user?
	String subscription_id = "";
	String subscription_gateway = "";
	int subscription_package = -1;
	String subscription_status = "";
	int paid_through_time = 0;
	int bonus_credits_target = -1;
	int bonus_rebirth_target = -1;
	int bonus_xp_target = -1;
	int bonus_manpower_target = -1;

	// Login report data
	int report__defenses_squares_defeated;
	int report__defenses_XP;
	int report__defenses_lost;
	int report__defenses_built;
	int report__walls_lost;
	int report__walls_built;
	int report__attacks_squares_captured;
	int report__attacks_XP;
	int report__levels_gained;
	int report__orb_count_delta;
	float report__orb_credits;
	int report__orb_XP;
	int report__farming_XP;
	int report__resource_count_delta;
	int report__land_lost;
	float report__energy_begin;
	float report__energy_spent;
	float report__energy_donated;
	float report__energy_lost_to_raids;
	float report__manpower_begin;
	float report__manpower_spent;
	float report__manpower_lost_to_resources;
	float report__manpower_donated;
	float report__manpower_lost_to_raids;
	float report__credits_begin;
	float report__credits_spent;
	float report__patron_XP;
	float report__patron_credits;
	float report__follower_XP;
	float report__follower_credits;
	int report__follower_count;
	int report__raids_fought;
	int report__medals_delta;
	int report__rebirth;
	float report__home_defense_credits;
	float report__home_defense_xp;
	float report__home_defense_rebirth;

	// Transient data
	ClientThread client_thread;
	int viewX = Integer.MIN_VALUE;
	int viewY = Integer.MIN_VALUE;
	int viewChunkX0 = Integer.MIN_VALUE;
	int viewChunkY0 =Integer.MIN_VALUE;
	float sessionXP = 0;
	int[] processEndTimes = new int[Constants.SIMULTANEOUS_PROCESS_LIMIT];
	float cur_login_XP = 0;
	int fealty_end_time = 0;
	int fealty_nationID = -1;
	int fealty_tournament_end_time = 0;
	int fealty_tournament_nationID = -1;
	int fealty_num_nations_at_tier = 0;
	int fealty_num_nations_in_tournament = 0;
	boolean ad_bonuses_allowed = false;

	public UserData(int _ID)
	{
		super(Constants.DT_USER, _ID);
	}

	public boolean ReadData()
	{
		int file_version;
		boolean result = true;

		String sql = "SELECT " +
		"version," +
		"playerID," +
		"creation_time, " +
		"admin, " +
		"veteran, " +
		"mod_level, " +
		"home_nationID, " +
		"nationID, " +
		"`rank`, " +
		"flags, " +
		"name, " +
		"email, " +
		"patron_code, " +
		"tutorial_state, " +
		"creation_device_type, " +
		"game_ban_end_time, " +
		"chat_ban_end_time, " +
		"prev_report_day, " +
		"report_count, " +
		"chat_offense_level, " +
		"muted_users, " +
		"muted_devices, " +
		"associated_users, " +
		"devices, " +
		"long_term_reports, " +
		"short_term_reports, " +
		"contacts, " +
		"patron_offers, " +
		"followers, " +
		"patronID, " +
		"cur_month_patron_bonus_XP, " +
		"cur_month_patron_bonus_credits, " +
		"prev_month_patron_bonus_XP, " +
		"prev_month_patron_bonus_credits, " +
		"total_patron_xp_received, " +
		"total_patron_credits_received, " +
		"max_num_followers, " +
		"max_num_followers_monthly, " +
		"mean_chat_interval, " +
		"prev_chat_fine_time, " +
		"mapID, " +
		"mainland_viewX, " +
		"mainland_viewY, " +
		"homeland_viewX, " +
		"homeland_viewY, " +
		"raidland_viewX, " +
		"raidland_viewY, " +
		"xp, " +
		"xp_monthly, " +
		"xp_monthly_month, " +
		"prev_update_contacts_day, " +
		"prev_check_messages_time, " +
		"login_count, " +
		"prev_login_time, " +
		"prev_logout_time, " +
		"play_time, " +
		"ad_bonus_available, " +
		"subscribed, " +
		"subscription_id, " +
		"subscription_gateway, " +
		"subscription_package, " +
		"subscription_status, " +
		"paid_through_time, " +
		"bonus_credits_target, " +
		"bonus_rebirth_target, " +
		"bonus_xp_target, " +
		"bonus_manpower_target, " +
		"report__defenses_squares_defeated, " +
		"report__defenses_XP, " +
		"report__defenses_lost, " +
		"report__defenses_built, " +
		"report__walls_lost, " +
		"report__walls_built, " +
		"report__attacks_squares_captured, " +
		"report__attacks_XP, " +
		"report__levels_gained, " +
		"report__orb_count_delta, " +
		"report__orb_credits, " +
		"report__orb_XP, " +
		"report__farming_XP, " +
		"report__resource_count_delta, " +
		"report__land_lost, " +
		"report__energy_begin, " +
		"report__energy_donated, " +
		"report__energy_spent, " +
		"report__energy_lost_to_raids, " +
		"report__manpower_begin, " +
		"report__manpower_donated, " +
		"report__manpower_spent, " +
		"report__manpower_lost_to_resources, " +
		"report__manpower_lost_to_raids, " +
		"report__credits_begin, " +
		"report__credits_spent, " +
		"report__patron_XP, " +
		"report__patron_credits, " +
		"report__follower_XP, " +
		"report__follower_credits, " +
		"report__follower_count, " +
		"report__raids_fought, " +
		"report__medals_delta, " +
		"report__rebirth, " +
		"report__home_defense_credits, " +
		"report__home_defense_xp, " +
		"report__home_defense_rebirth " +
		"FROM " + db_table_name + " where ID= '" + ID + "'";

		try {
			// Create statement for use with the DB.
			Statement stmt = db.createStatement();

			// Get the result set for the sql query
			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				file_version  = rs.getInt("version");
				playerID  = rs.getInt("playerID");
				creation_time = rs.getInt("creation_time");
				admin = rs.getBoolean("admin");
				veteran = rs.getBoolean("veteran");
				mod_level = rs.getInt("mod_level");
				home_nationID = rs.getInt("home_nationID");
				nationID = rs.getInt("nationID");
				rank = rs.getInt("rank");
				flags = rs.getInt("flags");
				name = rs.getString("name");
				email = rs.getString("email");
				patron_code = rs.getString("patron_code");
				tutorial_state = rs.getString("tutorial_state");
				creation_device_type = rs.getString("creation_device_type");
				game_ban_end_time = rs.getInt("game_ban_end_time");
				chat_ban_end_time = rs.getInt("chat_ban_end_time");
				prev_report_day = rs.getInt("prev_report_day");
				report_count = rs.getInt("report_count");
				chat_offense_level = rs.getFloat("chat_offense_level");
				muted_users = JSONToIntArray(rs.getString("muted_users"));
				muted_devices = JSONToIntArray(rs.getString("muted_devices"));
				associated_users = JSONToIntArray(rs.getString("associated_users"));
				devices = JSONToIntIntMap(rs.getString("devices"));
				long_term_reports = JSONToIntIntMap(rs.getString("long_term_reports"));
				short_term_reports = JSONToIntIntMap(rs.getString("short_term_reports"));
				contacts = JSONToIntIntMap(rs.getString("contacts"));
				patron_offers = JSONToIntArray(rs.getString("patron_offers"));
				followers = FollowerData.JSONToFollowerDataArray(rs.getString("followers"));
				patronID = rs.getInt("patronID");
				cur_month_patron_bonus_XP = rs.getFloat("cur_month_patron_bonus_XP");
				cur_month_patron_bonus_credits = rs.getFloat("cur_month_patron_bonus_credits");
				prev_month_patron_bonus_XP = rs.getFloat("prev_month_patron_bonus_XP");
				prev_month_patron_bonus_credits = rs.getFloat("prev_month_patron_bonus_credits");
				total_patron_xp_received = rs.getFloat("total_patron_xp_received");
				total_patron_credits_received = rs.getFloat("total_patron_credits_received");
				max_num_followers = rs.getInt("max_num_followers");
				max_num_followers_monthly = rs.getInt("max_num_followers_monthly");
				mean_chat_interval = rs.getLong("mean_chat_interval");
				prev_chat_fine_time = rs.getLong("prev_chat_fine_time");
				mapID = rs.getInt("mapID");
				mainland_viewX = rs.getInt("mainland_viewX");
				mainland_viewY = rs.getInt("mainland_viewY");
				homeland_viewX = rs.getInt("homeland_viewX");
				homeland_viewY = rs.getInt("homeland_viewY");
				raidland_viewX = rs.getInt("raidland_viewX");
				raidland_viewY = rs.getInt("raidland_viewY");
				xp = rs.getInt("xp");
				xp_monthly = rs.getInt("xp_monthly");
				xp_monthly_month = rs.getInt("xp_monthly_month");
				prev_update_contacts_day = rs.getInt("prev_update_contacts_day");
				prev_check_messages_time = rs.getInt("prev_check_messages_time");
				login_count = rs.getInt("login_count");
				prev_login_time = rs.getInt("prev_login_time");
				prev_logout_time = rs.getInt("prev_logout_time");
				play_time = rs.getInt("play_time");
				ad_bonus_available = rs.getInt("ad_bonus_available");
				subscribed = rs.getBoolean("subscribed");
				subscription_id = rs.getString("subscription_id");
				subscription_gateway = rs.getString("subscription_gateway");
				subscription_package = rs.getInt("subscription_package");
				subscription_status = rs.getString("subscription_status");
				if ((subscription_status == null) || (subscription_status.equals("null"))) subscription_status = "";
				paid_through_time = rs.getInt("paid_through_time");
				bonus_credits_target = rs.getInt("bonus_credits_target");
				bonus_rebirth_target = rs.getInt("bonus_rebirth_target");
				bonus_xp_target = rs.getInt("bonus_xp_target");
				bonus_manpower_target = rs.getInt("bonus_manpower_target");
				report__defenses_squares_defeated = rs.getInt("report__defenses_squares_defeated");
				report__defenses_XP = rs.getInt("report__defenses_XP");
				report__defenses_lost = rs.getInt("report__defenses_lost");
				report__defenses_built = rs.getInt("report__defenses_built");
				report__walls_lost = rs.getInt("report__walls_lost");
				report__walls_built = rs.getInt("report__walls_built");
				report__attacks_squares_captured = rs.getInt("report__attacks_squares_captured");
				report__attacks_XP = rs.getInt("report__attacks_XP");
				report__levels_gained = rs.getInt("report__levels_gained");
				report__orb_count_delta = rs.getInt("report__orb_count_delta");
				report__orb_credits = rs.getFloat("report__orb_credits");
				report__orb_XP = rs.getInt("report__orb_XP");
				report__farming_XP = rs.getInt("report__farming_XP");
				report__resource_count_delta = rs.getInt("report__resource_count_delta");
				report__land_lost = rs.getInt("report__land_lost");
				report__energy_begin = rs.getFloat("report__energy_begin");
				report__energy_spent = rs.getFloat("report__energy_spent");
				report__energy_donated = rs.getFloat("report__energy_donated");
				report__energy_lost_to_raids = rs.getFloat("report__energy_lost_to_raids");
				report__manpower_begin = rs.getFloat("report__manpower_begin");
				report__manpower_spent = rs.getFloat("report__manpower_spent");
				report__manpower_lost_to_resources = rs.getFloat("report__manpower_lost_to_resources");
				report__manpower_donated = rs.getFloat("report__manpower_donated");
				report__manpower_lost_to_raids = rs.getFloat("report__manpower_lost_to_raids");
				report__credits_begin = rs.getFloat("report__credits_begin");
				report__credits_spent = rs.getFloat("report__credits_spent");
				report__patron_XP = rs.getFloat("report__patron_XP");
				report__patron_credits = rs.getFloat("report__patron_credits");
				report__follower_XP = rs.getFloat("report__follower_XP");
				report__follower_credits = rs.getFloat("report__follower_credits");
				report__follower_count = rs.getInt("report__follower_count");
				report__raids_fought = rs.getInt("report__raids_fought");
				report__medals_delta = rs.getInt("report__medals_delta");
				report__rebirth = rs.getInt("report__rebirth");
				report__home_defense_credits = rs.getFloat("report__home_defense_credits");
				report__home_defense_xp = rs.getFloat("report__home_defense_xp");
				report__home_defense_rebirth = rs.getFloat("report__home_defense_rebirth");
			} else {
				result = false;
			}

			// Close statement and result set (must be done explicitly to avoid memory leak).
			try { if (rs != null) rs.close(); } catch (Exception e) {};
	    try { if (stmt != null) stmt.close(); } catch (Exception e) {};
		}
	  catch(Exception e)
		{
      Output.PrintToScreen("Could not fetch object with ID " + ID + " from table '" + db_table_name + "'. Message: " + e.getMessage());
			result = false;
		}

		// Initialize transient data
		for (int i = 0; i < Constants.SIMULTANEOUS_PROCESS_LIMIT; i++) {
			processEndTimes[i] = -1;
		}

		return result;
	}

	public void WriteData()
	{
		String sql = "UPDATE " + db_table_name + " SET " +
		"version = '" + VERSION + "', " +
		"playerID = '" + playerID + "', " +
		"creation_time = '" + creation_time + "', " +
		"admin = " + (admin ? "TRUE" : "FALSE") + ", " +
		"veteran = " + (veteran ? "TRUE" : "FALSE") + ", " +
		"mod_level = '" + mod_level + "', " +
		"home_nationID = '" + home_nationID + "', " +
		"nationID = '" + nationID + "', " +
		"`rank` = '" + rank + "', " +
		"flags = '" + flags + "', " +
		"name = '" + PrepStringForMySQL(name) + "', " +
		"email = '" + PrepStringForMySQL(email) + "', " +
		"patron_code = '" + PrepStringForMySQL(patron_code) + "', " +
		"tutorial_state = '" + PrepStringForMySQL(tutorial_state) + "', " +
		"creation_device_type = '" + PrepStringForMySQL(creation_device_type) + "', " +
		"game_ban_end_time = '" + game_ban_end_time + "', " +
		"chat_ban_end_time = '" + chat_ban_end_time + "', " +
		"prev_report_day = '" + prev_report_day + "', " +
		"report_count = '" + report_count + "', " +
		"chat_offense_level = '" + chat_offense_level + "', " +
		"muted_users = '" + IntArrayToJSON(muted_users) + "', " +
		"muted_devices = '" + IntArrayToJSON(muted_devices) + "', " +
		"associated_users = '" + IntArrayToJSON(associated_users) + "', " +
		"devices = '" + IntIntMapToJSON(devices) + "', " +
		"long_term_reports = '" + IntIntMapToJSON(long_term_reports) + "', " +
		"short_term_reports = '" + IntIntMapToJSON(short_term_reports) + "', " +
		"contacts = '" + IntIntMapToJSON(contacts) + "', " +
		"patron_offers = '" + IntArrayToJSON(patron_offers) + "', " +
		"followers = '" + PrepStringForMySQL(FollowerData.FollowerDataArrayToJSON(followers)) + "', " +
		"patronID = '" + patronID + "', " +
		"cur_month_patron_bonus_XP = '" + cur_month_patron_bonus_XP + "', " +
		"cur_month_patron_bonus_credits = '" + cur_month_patron_bonus_credits + "', " +
		"prev_month_patron_bonus_XP = '" + prev_month_patron_bonus_XP + "', " +
		"prev_month_patron_bonus_credits = '" + prev_month_patron_bonus_credits + "', " +
		"total_patron_xp_received = '" + total_patron_xp_received + "', " +
		"total_patron_credits_received = '" + total_patron_credits_received + "', " +
		"max_num_followers = '" + max_num_followers + "', " +
		"max_num_followers_monthly = '" + max_num_followers_monthly + "', " +
		"mean_chat_interval = '" + mean_chat_interval + "', " +
		"prev_chat_fine_time = '" + prev_chat_fine_time + "', " +
		"mapID = '" + mapID + "', " +
		"mainland_viewX = '" + mainland_viewX + "', " +
		"mainland_viewY = '" + mainland_viewY + "', " +
		"homeland_viewX = '" + homeland_viewX + "', " +
		"homeland_viewY = '" + homeland_viewY + "', " +
		"raidland_viewX = '" + raidland_viewX + "', " +
		"raidland_viewY = '" + raidland_viewY + "', " +
		"xp = '" + xp + "', " +
		"xp_monthly = '" + xp_monthly + "', " +
		"xp_monthly_month = '" + xp_monthly_month + "', " +
		"prev_update_contacts_day = '" + prev_update_contacts_day + "', " +
		"prev_check_messages_time = '" + prev_check_messages_time + "', " +
		"login_count = '" + login_count + "', " +
		"prev_login_time = '" + prev_login_time + "', " +
		"prev_logout_time = '" + prev_logout_time + "', " +
		"play_time = '" + play_time + "', " +
		"ad_bonus_available = '" + ad_bonus_available + "', " +
		"subscribed = " + (subscribed ? "TRUE" : "FALSE") + ", " +
		"subscription_id = '" + subscription_id + "', " +
		"subscription_gateway = '" + subscription_gateway + "', " +
		"subscription_package = '" + subscription_package + "', " +
		"subscription_status = '" + subscription_status + "', " +
		"paid_through_time = '" + paid_through_time + "', " +
		"bonus_credits_target = '" + bonus_credits_target + "', " +
		"bonus_rebirth_target = '" + bonus_rebirth_target + "', " +
		"bonus_xp_target = '" + bonus_xp_target + "', " +
		"bonus_manpower_target = '" + bonus_manpower_target + "', " +
		"report__defenses_squares_defeated = '" + report__defenses_squares_defeated + "', " +
		"report__defenses_XP = '" + report__defenses_XP + "', " +
		"report__defenses_lost = '" + report__defenses_lost + "', " +
		"report__defenses_built = '" + report__defenses_built + "', " +
		"report__walls_lost = '" + report__walls_lost + "', " +
		"report__walls_built = '" + report__walls_built + "', " +
		"report__attacks_squares_captured = '" + report__attacks_squares_captured + "', " +
		"report__attacks_XP = '" + report__attacks_XP + "', " +
		"report__levels_gained = '" + report__levels_gained + "', " +
		"report__orb_count_delta = '" + report__orb_count_delta + "', " +
		"report__orb_credits = '" + report__orb_credits + "', " +
		"report__orb_XP = '" + report__orb_XP + "', " +
		"report__farming_XP = '" + report__farming_XP + "', " +
		"report__resource_count_delta = '" + report__resource_count_delta + "', " +
		"report__land_lost = '" + report__land_lost + "', " +
		"report__energy_begin = '" + report__energy_begin + "', " +
		"report__energy_spent = '" + report__energy_spent + "', " +
		"report__energy_donated = '" + report__energy_donated + "', " +
		"report__energy_lost_to_raids = '" + report__energy_lost_to_raids + "', " +
		"report__manpower_begin = '" + report__manpower_begin + "', " +
		"report__manpower_spent = '" + report__manpower_spent + "', " +
		"report__manpower_lost_to_resources = '" + report__manpower_lost_to_resources + "', " +
		"report__manpower_donated = '" + report__manpower_donated + "', " +
		"report__manpower_lost_to_raids = '" + report__manpower_lost_to_raids + "', " +
		"report__credits_begin = '" + report__credits_begin + "', " +
		"report__credits_spent = '" + report__credits_spent + "', " +
		"report__patron_XP = '" + report__patron_XP + "', " +
		"report__patron_credits = '" + report__patron_credits + "', " +
		"report__follower_XP = '" + report__follower_XP + "', " +
		"report__follower_credits = '" + report__follower_credits + "', " +
		"report__follower_count = '" + report__follower_count + "', " +
		"report__raids_fought = '" + report__raids_fought + "', " +
		"report__medals_delta = '" + report__medals_delta + "', " +
		"report__rebirth = '" + report__rebirth + "', " +
		"report__home_defense_credits = '" + report__home_defense_credits + "', " +
		"report__home_defense_xp = '" + report__home_defense_xp + "', " +
		"report__home_defense_rebirth = '" + report__home_defense_rebirth + "' " +
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

		// TEMP -- modify existing column
		//ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY xp FLOAT", true, false);
		//ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY xp_monthly FLOAT", true, false);

		// Attempt to create each column
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD playerID INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD creation_time INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD admin BOOL", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD veteran BOOL", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD mod_level INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD home_nationID INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD nationID INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD `rank` INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD flags INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD name VARCHAR(20)", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD email VARCHAR(50)", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD patron_code VARCHAR(20)", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD tutorial_state TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD creation_device_type TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD game_ban_end_time INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD game_ban_end_time INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD chat_ban_end_time INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD prev_report_day INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD report_count INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD chat_offense_level FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD muted_users TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD muted_devices TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD associated_users TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD devices TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD long_term_reports TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD short_term_reports TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD contacts TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD patron_offers TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD followers TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD patronID INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD cur_month_patron_bonus_XP FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD cur_month_patron_bonus_credits FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD prev_month_patron_bonus_XP FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD prev_month_patron_bonus_credits FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD total_patron_xp_received FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD total_patron_credits_received FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD max_num_followers INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD max_num_followers_monthly INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD mean_chat_interval BIGINT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD prev_chat_fine_time BIGINT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD mapID INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD mainland_viewX INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD mainland_viewY INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD homeland_viewX INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD homeland_viewY INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD raidland_viewX INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD raidland_viewY INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD xp FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD xp_monthly FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD xp_monthly_month INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD prev_update_contacts_day INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD prev_check_messages_time INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD login_count INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD prev_login_time INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD prev_logout_time INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD play_time INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD ad_bonus_available INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD subscribed BOOL", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD subscription_id VARCHAR(24) DEFAULT ''", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD subscription_gateway VARCHAR(24) DEFAULT ''", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD subscription_package INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD subscription_status VARCHAR(24) DEFAULT ''", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD paid_through_time INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD bonus_credits_target INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD bonus_rebirth_target INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD bonus_xp_target INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD bonus_manpower_target INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD report__defenses_squares_defeated INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD report__defenses_XP INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD report__defenses_lost INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD report__defenses_built INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD report__walls_lost INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD report__walls_built INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD report__attacks_squares_captured INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD report__attacks_XP INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD report__levels_gained INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD report__orb_count_delta INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD report__orb_credits FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD report__orb_XP INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD report__farming_XP INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD report__resource_count_delta INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD report__land_lost INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD report__energy_begin FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD report__energy_spent FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD report__energy_donated FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD report__energy_lost_to_raids FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD report__manpower_begin FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD report__manpower_spent FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD report__manpower_lost_to_resources FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD report__manpower_donated FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD report__manpower_lost_to_raids FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD report__credits_begin FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD report__credits_spent FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD report__patron_XP FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD report__patron_credits FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD report__follower_XP FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD report__follower_credits FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD report__follower_count INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD report__raids_fought INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD report__medals_delta INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD report__rebirth INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD report__home_defense_credits FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD report__home_defense_xp FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD report__home_defense_rebirth FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD INDEX playerID (playerID)", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD INDEX name (name)", true, false);
	}

	public static void DeleteDBTable()
	{
		// Attempt to delete the DB table
		ExecuteUpdate(db, "DROP TABLE " + db_table_name, true, true);
	}

	public static int GetUserIDByPlayerID(int _playerID)
	{
		int userID = -1;
		String sql = "SELECT ID FROM " + db_table_name + " where playerID= '" + _playerID + "'";

		try {
			// Create statement for use with the DB.
			Statement stmt = db.createStatement();

			// Get the result set for the sql query
			ResultSet rs = stmt.executeQuery(sql);

			int result = -1;
			if (rs.next()) {
				result = rs.getInt("ID");
			}

			// Close statement and result set (must be done explicitly to avoid memory leak).
			try { if (rs != null) rs.close(); } catch (Exception e) {};
	    try { if (stmt != null) stmt.close(); } catch (Exception e) {};

			return result;
		}
	  catch(Exception e)
		{
			return -1;
		}
	}

	public static int GetUserIDByUsername(String _username)
	{
		int userID = -1;
		String sql = "SELECT ID FROM " + db_table_name + " where name= '" + PrepStringForMySQL(_username) + "'";

		try {
			// Create statement for use with the DB.
			Statement stmt = db.createStatement();

			// Get the result set for the sql query
			ResultSet rs = stmt.executeQuery(sql);

			int result = -1;
			if (rs.next()) {
				result = rs.getInt("ID");
			}

			// Close statement and result set (must be done explicitly to avoid memory leak).
			try { if (rs != null) rs.close(); } catch (Exception e) {};
	    try { if (stmt != null) stmt.close(); } catch (Exception e) {};

			return result;
		}
	  catch(Exception e)
		{
			return -1;
		}
	}

	public void ModifyReportValueInt(ReportVal _reportVal, int _amount)
	{
		switch (_reportVal)
		{
		case report__defenses_squares_defeated: report__defenses_squares_defeated += _amount; break;
		case report__defenses_XP: report__defenses_XP += _amount; break;
		case report__defenses_lost: report__defenses_lost += _amount; break;
		case report__defenses_built: report__defenses_built += _amount; break;
		case report__walls_lost: report__walls_lost += _amount; break;
		case report__walls_built: report__walls_built += _amount; break;
		case report__attacks_squares_captured: report__attacks_squares_captured += _amount; break;
		case report__attacks_XP: report__attacks_XP += _amount; break;
		case report__levels_gained: report__levels_gained += _amount; break;
		case report__orb_count_delta: report__orb_count_delta += _amount; break;
		case report__orb_XP: report__orb_XP += _amount; break;
		case report__farming_XP: report__farming_XP += _amount; break;
		case report__resource_count_delta: report__resource_count_delta += _amount; break;
		case report__land_lost: report__land_lost += _amount; break;
		case report__follower_count: report__follower_count += _amount; break;
		case report__raids_fought: report__raids_fought += _amount; break;
		case report__medals_delta: report__medals_delta += _amount; break;
		case report__rebirth: report__rebirth += _amount; break;
		default: Output.PrintToScreen("ModifyReportValueInt() called for ReportVal '" + _reportVal + "' of wrong type.");
		}

		// Mark this user data to be updated
		DataManager.MarkForUpdate(this);
	}

	public void ModifyReportValueFloat(ReportVal _reportVal, float _amount)
	{
		// TESTING
		//Output.PrintToScreen("ModifyReportValueFloat() user: " + name + ", _reportVal '" + _reportVal + "', _amount: " + _amount);

		switch (_reportVal)
		{
		case report__energy_begin: report__energy_begin += _amount; break;
		case report__energy_spent: report__energy_spent += _amount; break;
		case report__energy_donated: report__energy_donated += _amount; break;
		case report__energy_lost_to_raids: report__energy_lost_to_raids += _amount; break;
		case report__manpower_begin: report__manpower_begin += _amount; break;
		case report__manpower_spent: report__manpower_spent += _amount; break;
		case report__manpower_lost_to_resources: report__manpower_lost_to_resources += _amount; break;
		case report__manpower_donated: report__manpower_donated += _amount; break;
		case report__manpower_lost_to_raids: report__manpower_lost_to_raids += _amount; break;
		case report__orb_credits: report__orb_credits += _amount; break;
		case report__credits_begin: report__credits_begin += _amount; break;
		case report__credits_spent: report__credits_spent += _amount; break;
		case report__patron_XP: report__patron_XP += _amount; break;
		case report__patron_credits: report__patron_credits += _amount; break;
		case report__follower_XP: report__follower_XP += _amount; break;
		case report__follower_credits: report__follower_credits += _amount; break;
		case report__home_defense_credits: report__home_defense_credits += _amount; break;
		case report__home_defense_xp: report__home_defense_xp += _amount; break;
		case report__home_defense_rebirth: report__home_defense_rebirth += _amount; break;
		default: Output.PrintToScreen("ModifyReportValueFloat() called for ReportVal '" + _reportVal + "' of wrong type.");
		}

		// Mark this user data to be updated
		DataManager.MarkForUpdate(this);
	}

	public void ClearFealtyToNation(int _nationID)
	{
		for (Map.Entry<Integer, Integer> entry : devices.entrySet())
		{
			// Get the user's current deviceID.
			int deviceID = entry.getKey();

			// Get the device record corresponding to the deviceID.
			DeviceData deviceData = (DeviceData)DataManager.GetData(Constants.DT_DEVICE, deviceID, false);

			if (deviceData == null) {
				continue;
			}

			// If the device has fealty to the given _nationID, clear that fealty.
			if (deviceData.fealty0_nationID == _nationID) deviceData.fealty0_nationID = -1;
			if (deviceData.fealty1_nationID == _nationID) deviceData.fealty1_nationID = -1;
			if (deviceData.fealty2_nationID == _nationID) deviceData.fealty2_nationID = -1;
			if (deviceData.fealty_tournament_nationID == _nationID) deviceData.fealty_tournament_nationID = -1;

			// Mark the device data to be updated.
			DataManager.MarkForUpdate(deviceData);
		}
	}

	public void ClearFealty(boolean _clear_fealty_0, boolean _clear_fealty_1, boolean _clear_fealty_2, boolean _clear_tournament_fealty)
	{
		for (Map.Entry<Integer, Integer> entry : devices.entrySet())
		{
			// Get the user's current deviceID.
			int deviceID = entry.getKey();

			// Get the device record corresponding to the deviceID.
			DeviceData deviceData = (DeviceData)DataManager.GetData(Constants.DT_DEVICE, deviceID, false);

			if (deviceData == null) {
				continue;
			}

			// Clear the appropriate fealty levels.
			if (_clear_fealty_0) deviceData.fealty0_nationID = -1;
			if (_clear_fealty_1) deviceData.fealty1_nationID = -1;
			if (_clear_fealty_2) deviceData.fealty2_nationID = -1;
			if (_clear_tournament_fealty) deviceData.fealty_tournament_nationID = -1;

			// Mark the device data to be updated.
			DataManager.MarkForUpdate(deviceData);
		}
	}

	public void CopyBansToAssociatedUsersAndDevices()
	{
		// Copy this user's bans to its devices.
		CopyBansToDevices();

		for (int i = 0; i < associated_users.size(); i++)
		{
			UserData assoc_user_data = (UserData)DataManager.GetData(Constants.DT_USER, associated_users.get(i), false);

			if (assoc_user_data != null)
			{
				// Copy this user's bans to its associated user.
				assoc_user_data.game_ban_end_time = game_ban_end_time;
				assoc_user_data.chat_ban_end_time = chat_ban_end_time;

				// Have the associated user copy its bans to its devices.
				assoc_user_data.CopyBansToDevices();

				// Mark the associated user's data to be updated.
				DataManager.MarkForUpdate(assoc_user_data);
			}
		}
	}

	public void CopyBansToDevices()
	{
		for (Map.Entry<Integer, Integer> entry : devices.entrySet())
		{
			// Get the user's current deviceID.
			int deviceID = entry.getKey();

			// Get the device record corresponding to the deviceID.
			DeviceData deviceData = (DeviceData)DataManager.GetData(Constants.DT_DEVICE, deviceID, false);

			if (deviceData == null) {
				continue;
			}

			// Copy this user's bans to this device.
			deviceData.game_ban_end_time = game_ban_end_time;
			deviceData.chat_ban_end_time = chat_ban_end_time;

			// Mark the device data to be updated.
			DataManager.MarkForUpdate(deviceData);
		}
	}

	public void UpdateComplaintAndBanCounts(int _complaints_by_delta, int _complaints_against_delta, int _warnings_sent_delta, int _chat_bans_delta, int _game_bans_delta)
	{
		PlayerAccountData account = AccountDB.ReadPlayerAccount(playerID);
		if (account != null)
		{
			account.num_complaints_by += _complaints_by_delta;
			account.num_complaints_against += _complaints_against_delta;
			account.num_warnings_sent += _warnings_sent_delta;
			account.num_chat_bans += _chat_bans_delta;
			account.num_game_bans += _game_bans_delta;
			AccountDB.WritePlayerAccount(account);
		}
	}

	public void UpdateVeteranStatus()
	{
		// If this user is already considered veteran, do nothing.
		if (veteran) {
			return;
		}

		// Iterate associated users. If any are found to be veteran, mark this user as also being veteran.
		for (int i = 0; i < associated_users.size(); i++)
		{
			UserData assoc_user_data = (UserData)DataManager.GetData(Constants.DT_USER, associated_users.get(i), false);

			if ((assoc_user_data != null) && assoc_user_data.veteran)
			{
				// Mark this user as being veteran.
				veteran = true;
				Output.PrintToScreen("User '" + name + "' (" + ID + ") made vet due to being associated with vet user '" + assoc_user_data.name + "'.");
				return;
			}
		}
	}

	public void SyncServerIndependentVeteranStatus(DeviceData _device_data)
	{
		// Get (or create) the game-server-independent record of this user's device.
		DeviceAccountData device_account = DeviceDB.ReadDeviceAccount(_device_data.name);
		if (device_account == null) {
			device_account = DeviceDB.CreateNewDeviceAccount(_device_data.name, _device_data.device_type);
		}

		// If the device account is not marked as being veteran, determine whether it should be.
		if (!device_account.veteran)
		{
			// If this user is a veteran, mark the device's account as being veteran.
			if (veteran)
			{
				device_account.veteran = true;
				Output.PrintToScreen("Device account for device ID '" + _device_data.ID + "' made vet due to being associated with vet user '" + name + "' (" + ID + ").");
			}
			else
			{
				// If any of this device's associated devices are veteran, mark this device's account as being veteran.
				for (int i = 0; i < _device_data.associated_devices.size(); i++)
				{
					DeviceData assoc_device_data = (DeviceData)DataManager.GetData(Constants.DT_DEVICE, _device_data.associated_devices.get(i), false);
					if (assoc_device_data != null)
					{
						DeviceAccountData assoc_device_account = DeviceDB.ReadDeviceAccount(assoc_device_data.name);
						if ((assoc_device_account != null) && assoc_device_account.veteran)
						{
							device_account.veteran = true;
							Output.PrintToScreen("Device account for device ID " + _device_data.ID + " made VET due to associated device ID " + assoc_device_data.ID + ".");
							break;
						}
					}
				}
			}

			// If the device account has been marked as being veteran, update it to the DB.
			if (device_account.veteran) {
				DeviceDB.WriteDeviceAccount(device_account);
			}
		}

		// If this device is marked as being veteran, make sure this user is marked as being veteran.
		if (device_account.veteran) {
			veteran = true;
			Output.PrintToScreen("User '" + name + "' (" + ID + ") made vet due to logging in with vet device account for device ID '" + _device_data.ID + "'.");
		}
	}

	public int DetermineOldestAgeOfAssociatedUsers()
	{
		int oldest_creation_time = creation_time;

		// Iterate associated users, and record oldest creation time among them.
		for (int i = 0; i < associated_users.size(); i++)
		{
			UserData assoc_user_data = (UserData)DataManager.GetData(Constants.DT_USER, associated_users.get(i), false);
			oldest_creation_time = Math.min(oldest_creation_time, assoc_user_data.creation_time);
		}

		return Constants.GetTime() - oldest_creation_time;
	}

	// Make any necessary repairs to this user's data
	public void Repair()
	{
		boolean modified = false;
		int cur_index;

		// Make sure data exists for all followers
		FollowerData cur_follower;
		UserData followerUserData;
		for (cur_index = 0; cur_index < followers.size();)
		{
			// Get the current follower's data
			cur_follower = (FollowerData)followers.get(cur_index);

			// Get the current follower's user data
			followerUserData = (UserData)DataManager.GetData(Constants.DT_USER, cur_follower.userID, false);

			if (followerUserData == null)
			{
				Output.PrintToScreen("  FIXED ERROR: user " + name + " (" + ID + ") has follower (ID " + cur_follower.userID + ") with missing data.");
				followers.remove(cur_index);
				modified = true;
			}
			else
			{
				 cur_index++;
			}
		}

		// Make sure data exists for all patron offers
		UserData offerUserData;
		int offerUserID;
		for (cur_index = 0; cur_index < patron_offers.size();)
		{
			// Get the current offering user's ID
			offerUserID = patron_offers.get(cur_index);

			// Get the current offer's user data
			offerUserData = (UserData)DataManager.GetData(Constants.DT_USER, offerUserID, false);

			if (offerUserData == null)
			{
				Output.PrintToScreen("  FIXED ERROR: user " + name + " (" + ID + ") has patron offer from user (ID " + offerUserID + ") with missing data.");
				patron_offers.remove(cur_index);
				modified = true;
			}
			else
			{
				 cur_index++;
			}
		}

		// Make sure data exists for patron user
		if (patronID != -1)
		{
			UserData patronUserData = (UserData)DataManager.GetData(Constants.DT_USER, patronID, false);

			if (patronUserData == null)
			{
				Output.PrintToScreen("  FIXED ERROR: user " + name + " (" + ID + ") has patron user (ID " + patronID + ") with missing data.");
				patronID = -1;
				modified = true;
			}
		}

		// Mark the user to be updated to the DB if appropriate.
		if (modified) {
			DataManager.MarkForUpdate(this);
		}
	}
}
