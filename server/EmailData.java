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

public class EmailData extends BaseData
{
	public static String db_table_name = "Emails";

	public static int VERSION = 1;

	public static int MAX_EMAIL_LEN = 64;

	String email = "";
	ArrayList<Integer> users = new ArrayList<Integer>();

	public EmailData(int _ID)
	{
		super(Constants.DT_EMAIL, _ID);
	}

	public boolean ReadData()
	{
		int file_version;
		boolean result = true;

		String sql = "SELECT " +
		"version," +
		"email, " +
		"users " +
		"FROM " + db_table_name + " where ID= '" + ID + "'";

		try {
			// Create statement for use with the DB.
			Statement stmt = db.createStatement();

			// Get the result set for the sql query
			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				file_version  = rs.getInt("version");

				email = rs.getString("email");
				users = JSONToIntArray(rs.getString("users"));
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
		"email = '" + PrepStringForMySQL(email) + "', " +
		"users = '" + IntArrayToJSON(users) + "' " +
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

	public String GetEmail()
	{
		return email;
	}

	public static void InitDBTable()
	{
		// Create the user data table, if it doesn't yet exist.
		CreateTable(db, db_table_name);

		// Attempt to create each column
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD email VARCHAR(" + MAX_EMAIL_LEN + ")", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD users TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD INDEX email (email)", true, false);
	}

	public static void DeleteDBTable()
	{
		// Attempt to delete the DB table
		ExecuteUpdate(db, "DROP TABLE " + db_table_name, true, true);
	}

	public static EmailData GetEmailDataByEmail(String _email)
	{
		// Trim email to max length if necessary.
		if (_email.length() > MAX_EMAIL_LEN) {
			_email = _email.substring(0, MAX_EMAIL_LEN);
		}

		boolean new_email = false;

		if (_email.equals(""))
		{
			Output.PrintToScreen("GetEmailDataByEmail() given empty _email!");
			return null;
		}

		// Determine whether there exists an email record corresponding to the given email address.
		int emailID = EmailData.GetEmailIDByEmail(_email);

		// If not, get the next available email data ID for the new email address.
		if (emailID == -1)
		{
			emailID = DataManager.GetNextDataID(Constants.DT_EMAIL);
			new_email = true;
		}

		// Get or create the email record.
		EmailData emailData = (EmailData)DataManager.GetData(Constants.DT_EMAIL, emailID, true); // Create, if it doesn't exist.

		if (new_email)
		{
			// Record the email's email  address.
			emailData.email = _email;

			// Update the email data.
			DataManager.MarkForUpdate(emailData);
		}

		return emailData;
	}

	public static int GetEmailIDByEmail(String _email)
	{
		// Trim email to max length if necessary.
		if (_email.length() > MAX_EMAIL_LEN) {
			_email = _email.substring(0, MAX_EMAIL_LEN);
		}

		String sql = "SELECT ID FROM " + db_table_name + " where email= '" + BaseData.PrepStringForMySQL(_email) + "'";

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

	public static void AssociateEmailWithUser(String _email, int _userID)
	{
		// Get the data for the user.
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		// If the user data doesn't exist, do nothing.
		if (userData == null) {
			return;
		}

		// If the given e-mail is blank, do nothing.
		if (_email.equals("")) {
			return;
		}

		// Get or create the EmailData representing this email address.
		EmailData email_data = GetEmailDataByEmail(_email);

		//Output.PrintToScreen("Associating email " + _email + " (ID " + email_data.ID + ":" + email_data.email + ") with user " + userData.name);

		// Make sure this user is in the email's list of users.
		if (email_data.users.contains(_userID) == false) {
			email_data.users.add(_userID);
		}

		// Make sure that each of this email's users is co-associated with this user.
		EmailData.CoassociateUsers(userData, email_data);

		// Mark the email_data and userData to be updated.
		DataManager.MarkForUpdate(email_data);
		DataManager.MarkForUpdate(userData);
	}

	// Coassociate the given user with each of the given email's users.
	public static void CoassociateUsers(UserData _userData, EmailData _emailData)
	{
		for (int i = 0; i < _emailData.users.size(); i++)
		{
			int assoc_userID = _emailData.users.get(i);

			if ((assoc_userID == -1) || (assoc_userID == _userData.ID)) {
				continue;
			}

			// Get the UserData for the other email's current user that should be associated with this user.
			UserData assoc_user_data = (UserData)DataManager.GetData(Constants.DT_USER, assoc_userID, false);

			if (assoc_user_data == null) {
				continue;
			}

			// Make sure that the other user is associated with this user.
			if (assoc_user_data.associated_users.contains(_userData.ID) == false) {
				assoc_user_data.associated_users.add(_userData.ID);
			}

			// Make sure that this user is associated with the other user.
			if (_userData.associated_users.contains(assoc_userID) == false) {
				_userData.associated_users.add(assoc_userID);
			}

			// Mark the associated UserData to be updated.
			DataManager.MarkForUpdate(assoc_user_data);
		}
	}
}
