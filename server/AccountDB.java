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

class PlayerAccountData
{
	int ID = -1;
	String username = "";
	String passhash = "";
	String email = "";
	String security_question = "";
	String security_answer = "";
	int woc2_serverID = -1;
	String info = "";
	int game_ban_end_time = 0;
	int num_complaints_by = 0;
	int num_complaints_against = 0;
	int num_warnings_sent = 0;
	int num_chat_bans = 0;
	int num_game_bans = 0;
}

class AccountDB
{
	static String db_table_name = "Accounts";
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
				String sql = "CREATE TABLE " + db_table_name + " (ID INT not NULL auto_increment, PRIMARY KEY (ID)) ENGINE = MyISAM ;";

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
		BaseData.ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD username VARCHAR(20)", true, false);
		BaseData.ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD passhash CHAR(64)", true, false);
		BaseData.ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD email VARCHAR(50)", true, false);
		BaseData.ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD security_question TINYTEXT", true, false);
		BaseData.ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD security_answer TINYTEXT", true, false);
		BaseData.ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD woc2_serverID INT", true, false);
		BaseData.ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD info TEXT", true, false);
		BaseData.ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD INDEX username (username)", true, false);
		BaseData.ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD game_ban_end_time INT", true, false);
		BaseData.ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD num_complaints_by INT", true, false);
		BaseData.ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD num_complaints_against INT", true, false);
		BaseData.ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD num_warnings_sent INT", true, false);
		BaseData.ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD num_chat_bans INT", true, false);
		BaseData.ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD num_game_bans INT", true, false);
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

	public static int GetPlayerIDByUsername(String _username)
	{
		int playerID = -1;
		String sql = "SELECT ID FROM " + db_table_name + " where username= '" + BaseData.PrepStringForMySQL(_username) + "'";

		try {
			// Create statement for use with the DB.
			Statement stmt = db.createStatement();

			// Get the result set for the sql query
			ResultSet rs = stmt.executeQuery(sql);

			int result;
			if (rs.next()) {
				result = rs.getInt("ID");
			} else {
				result = -1;
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

	public static PlayerAccountData CreateNewPlayerAccount(String _username)
	{
		// Create data for new player account.
		PlayerAccountData player_account = new PlayerAccountData();
		player_account.username = _username;
		player_account.woc2_serverID = Constants.GetServerID(); // Use this server's ID

    int generatedkey = -1;

		try {
			PreparedStatement pstmt = db.prepareStatement("INSERT INTO " + db_table_name + " (username, woc2_serverID) values('" + BaseData.PrepStringForMySQL(player_account.username) + "','" + player_account.woc2_serverID + "')", java.sql.Statement.RETURN_GENERATED_KEYS);
			pstmt.execute();
			ResultSet rs = pstmt.getGeneratedKeys();
			if (rs.next()) {
				 generatedkey=rs.getInt(1);
			}

			// Close statement and result set (must be done explicitly to avoid memory leak).
			try { if (rs != null) rs.close(); } catch (Exception e) {};
	    try { if (pstmt != null) pstmt.close(); } catch (Exception e) {};
		}
		catch(Exception e) {
			Output.PrintToScreen("Could not add new player to account table '" + db_table_name + "'. Message: " + e.getMessage() + ". Exiting.");
			System.exit(1);
		}

		// Record the new player's ID in the PlayerAccountData.
		player_account.ID = generatedkey;

		return player_account;
	}

	public static void WritePlayerAccount(PlayerAccountData _player_account)
	{
		String sql = "UPDATE " + db_table_name + " SET " +
		"username = '" + BaseData.PrepStringForMySQL(_player_account.username) + "', " +
		"passhash = '" + _player_account.passhash + "', " +
		"email = '" + BaseData.PrepStringForMySQL(_player_account.email) + "', " +
		"security_question = '" + BaseData.PrepStringForMySQL(_player_account.security_question) + "', " +
		"security_answer = '" + BaseData.PrepStringForMySQL(_player_account.security_answer) + "', " +
		"info = '" + _player_account.info + "', " +
		"woc2_serverID = '" + _player_account.woc2_serverID + "', " +
		"game_ban_end_time = '" + _player_account.game_ban_end_time + "', " +
		"num_complaints_by = '" + _player_account.num_complaints_by + "', " +
		"num_complaints_against = '" + _player_account.num_complaints_against + "', " +
		"num_warnings_sent = '" + _player_account.num_warnings_sent + "', " +
		"num_chat_bans = '" + _player_account.num_chat_bans + "', " +
		"num_game_bans = '" + _player_account.num_game_bans + "' " +
		"WHERE ID= '" + _player_account.ID + "'";

		//Output.PrintToScreen("Writing player account " + _player_account.ID + " woc2_serverID: " + _player_account.woc2_serverID);

		try {
			// Create statement for use with the DB.
			Statement stmt = db.createStatement();

			// Execute the sql query
			stmt.executeUpdate(sql);
			stmt.close();
		}
	  catch(Exception e) {
      Output.PrintToScreen("Could not store object with ID " + _player_account.ID + " in table '" + db_table_name + "'. Message: " + e.getMessage());
		}
	}

	public static PlayerAccountData ReadPlayerAccount(int _playerID)
	{
		String sql = "SELECT " +
		"ID," +
		"username," +
		"passhash," +
		"email, " +
		"security_question, " +
		"security_answer, " +
		"info, " +
		"woc2_serverID, " +
		"game_ban_end_time, " +
		"num_complaints_by, " +
		"num_complaints_against, " +
		"num_warnings_sent, " +
		"num_chat_bans, " +
		"num_game_bans " +
		"FROM " + db_table_name + " where ID= '" + _playerID + "'";

		return ReadPlayerAccountBySQL(sql);
	}

	public static PlayerAccountData ReadPlayerAccountByUsername(String _username)
	{
		String sql = "SELECT " +
		"ID," +
		"username," +
		"passhash," +
		"email, " +
		"security_question, " +
		"security_answer, " +
		"info, " +
		"woc2_serverID, " +
		"game_ban_end_time, " +
		"num_complaints_by, " +
		"num_complaints_against, " +
		"num_warnings_sent, " +
		"num_chat_bans, " +
		"num_game_bans " +
		"FROM " + db_table_name + " where username= '" + BaseData.PrepStringForMySQL(_username) + "'";

		return ReadPlayerAccountBySQL(sql);
	}

	public static PlayerAccountData ReadPlayerAccountBySQL(String _sql)
	{
		PlayerAccountData account = new PlayerAccountData();

		try {
			// Create statement for use with the DB.
			Statement stmt = db.createStatement();

			// Get the result set for the sql query
			ResultSet rs = stmt.executeQuery(_sql);

			if (rs.next()) {
				account.ID  = rs.getInt("ID");
				account.username  = rs.getString("username");
				account.passhash  = rs.getString("passhash");
				account.email  = rs.getString("email");
				account.security_question  = rs.getString("security_question");
				account.security_answer  = rs.getString("security_answer");
				account.info  = rs.getString("info");
				account.woc2_serverID  = rs.getInt("woc2_serverID");
				account.game_ban_end_time  = rs.getInt("game_ban_end_time");
				account.num_complaints_by  = rs.getInt("num_complaints_by");
				account.num_complaints_against  = rs.getInt("num_complaints_against");
				account.num_warnings_sent  = rs.getInt("num_warnings_sent");
				account.num_chat_bans  = rs.getInt("num_chat_bans");
				account.num_game_bans  = rs.getInt("num_game_bans");
				if (account.username == null) account.username = "";
				if (account.passhash == null) account.passhash = "";
				if (account.email == null) account.email = "";
				if ((account.info == null) || (account.info.equals("null"))) account.info = "";
			} else {
				account = null;
			}

			// Close statement and result set (must be done explicitly to avoid memory leak).
			try { if (rs != null) rs.close(); } catch (Exception e) {};
	    try { if (stmt != null) stmt.close(); } catch (Exception e) {};
		}
	  catch(Exception e)
		{
			Output.PrintException(e);
      Output.PrintToScreen("Could not fetch object from table '" + db_table_name + "' with SQL '" + _sql + "'. Message: " + e.getMessage());
			account = null;
		}

		return account;
	}

	public static String DeterminePasswordHash(String _password)
	{
		// Determine the hash for the given password.
		return org.apache.commons.codec.digest.DigestUtils.sha256Hex("" + _password);
	}
}
