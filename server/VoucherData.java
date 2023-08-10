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

public class VoucherData extends BaseData
{
	public static String db_table_name = "Vouchers";

	public static int VERSION = 1;

	public static int MAX_VOUCHER_CODE_LEN = 64;

	String code = "";
	int credits_remaining = 0;
	int credits_redeemed = 0;
	ArrayList<String> history = new ArrayList<String>();

	public VoucherData(int _ID)
	{
		super(Constants.DT_VOUCHER, _ID);
	}

	public boolean ReadData()
	{
		int file_version;
		boolean result = true;

		String sql = "SELECT " +
		"version," +
		"code, " +
		"credits_remaining, " +
		"credits_redeemed, " +
		"history " +
		"FROM " + db_table_name + " where ID= '" + ID + "'";

		try {
			// Create statement for use with the DB.
			Statement stmt = db.createStatement();

			// Get the result set for the sql query
			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				file_version  = rs.getInt("version");

				code = rs.getString("code");
				credits_remaining = rs.getInt("credits_remaining");
				credits_redeemed = rs.getInt("credits_redeemed");
				history = JSONToStringArray(rs.getString("history"));
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
		"code = '" + PrepStringForMySQL(code) + "', " +
		"credits_remaining = '" + credits_remaining + "', " +
		"credits_redeemed = '" + credits_redeemed + "', " +
		"history = '" + PrepStringForMySQL(StringArrayToJSON(history)) + "' " +
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
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD code VARCHAR(" + MAX_VOUCHER_CODE_LEN + ")", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD credits_remaining INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD credits_redeemed INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD history TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD INDEX code (code)", true, false);
	}

	public static void DeleteDBTable()
	{
		// Attempt to delete the DB table
		ExecuteUpdate(db, "DROP TABLE " + db_table_name, true, true);
	}

	public static VoucherData GetVoucherDataByCode(String _voucher_code, boolean _create)
	{
		int voucherID = GetVoucherIDByCode(_voucher_code);

		if (voucherID != -1) {
			return (VoucherData)DataManager.GetData(Constants.DT_VOUCHER, voucherID, false);
		}

		VoucherData voucherData = null;

		if (_create)
		{
			// Determine ID for new voucher
			voucherID = DataManager.GetNextDataID(Constants.DT_VOUCHER);

			// Create a new VoucherData for the new voucher
			voucherData = (VoucherData)DataManager.GetData(Constants.DT_VOUCHER, voucherID, true); // Create

			// Initialize the new VoucherData
			voucherData.code = _voucher_code;
			voucherData.credits_remaining = 0;
			voucherData.credits_redeemed = 0;

			// Mark the new VoucherData to be updated
			DataManager.MarkForUpdate(voucherData);

			return voucherData;
		}

		return voucherData;
	}

	public static int GetVoucherIDByCode(String _voucher_code)
	{
		String sql = "SELECT ID FROM " + db_table_name + " where code= '" + BaseData.PrepStringForMySQL(_voucher_code) + "'";

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

	public static int AddValueToVoucher(String _voucher_code, int _amount, boolean _replace_value)
	{
		// Get or create the voucher with the given code.
		VoucherData voucherData = GetVoucherDataByCode(_voucher_code, true);

		if (_replace_value) {
			voucherData.credits_remaining = 0;
		}

		// Add the given value to this voucher.
		voucherData.credits_remaining += _amount;

		// Log this addition of credits in the voucher's history.
		voucherData.history.add(_amount + " credits added.");

		// Mark the VoucherData to be updated
		DataManager.MarkForUpdate(voucherData);

		// Log the adding of value to this voucher.
		Constants.WriteToLog("log_vouchers.txt", Constants.GetTimestampString() + ": " + _amount + " credits added.\n");

		// Return the new total credits_remaining.
		return voucherData.credits_remaining;
	}

	public static boolean RemoveValueFromVoucher(VoucherData _voucherData, int _amount, boolean _redeem)
	{
		// If there is no voucher with the given code, return false.
		if (_voucherData == null) {
			return false;
		}

		// If the given amount is more than remains on the voucher, and if redeeming, return false.
		if (_redeem && (_amount > _voucherData.credits_remaining)) {
			return false;
		}

		// Remove the given amount of credits from the voucher, or the total amount remaining, whichever is less.
		int amount_removed = Math.min(_amount, _voucherData.credits_remaining);
		_voucherData.credits_remaining -= amount_removed;

		// If redeeming, add the amount removed to the credits_redeemed.
		if (_redeem) {
			_voucherData.credits_redeemed += amount_removed;
		}

		// Mark the VoucherData to be updated
		DataManager.MarkForUpdate(_voucherData);

		return true;
	}
}
