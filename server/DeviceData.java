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
import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import WOCServer.*;

public class DeviceData extends BaseData
{
	public static String db_table_name = "Devices";

	public static int VERSION = 1;

	public static int MAX_DEVICE_NAME_LEN = 30;
	public static int MAX_DEVICE_TYPE_LEN = 80;
	public static int MAX_IP_ADDRESS_LEN = 80;

	String name = "";
	String uid = "";
	String device_type = "";
	String prev_IP = "";
	int playerID = -1;
  int game_ban_end_time = -1;
  int chat_ban_end_time = -1;

	int creation_time = 0;

	int fealty0_nationID = -1;
	int fealty1_nationID = -1;
	int fealty2_nationID = -1;
	int fealty0_prev_attack_time = -1;
	int fealty1_prev_attack_time = -1;
	int fealty2_prev_attack_time = -1;
	int fealty_tournament_nationID = -1;
	int fealty_tournament_start_day = -1;

	int num_correlation_checks = 0;
	HashMap<Integer,Integer> correlation_counts = new HashMap<Integer,Integer>();
	HashMap<Integer,CorrelationRecord> correlation_records = new HashMap<Integer,CorrelationRecord>();

	ArrayList<Integer> associated_devices = new ArrayList<Integer>();
	ArrayList<Integer> users = new ArrayList<Integer>();

	// Transient
	HashMap<Integer,CorrelationRecord> tracking_correlations = new HashMap<Integer,CorrelationRecord>();
	long prev_active_fine_time = 0;

	public DeviceData(int _ID)
	{
		super(Constants.DT_DEVICE, _ID);
	}

	public boolean ReadData()
	{
		int file_version;
		boolean result = true;

		String sql = "SELECT " +
		"version," +
		"name, " +
		"uid, " +
		"device_type, " +
		"prev_IP, " +
		"playerID, " +
		"game_ban_end_time, " +
		"chat_ban_end_time, " +
		"creation_time, " +
		"fealty0_nationID, " +
		"fealty1_nationID, " +
		"fealty2_nationID, " +
		"fealty0_prev_attack_time, " +
		"fealty1_prev_attack_time, " +
		"fealty2_prev_attack_time, " +
		"fealty_tournament_nationID, " +
		"fealty_tournament_start_day, " +
		"num_correlation_checks, " +
		"correlation_counts, " +
		"correlation_records, " +
		"associated_devices, " +
		"users " +
		"FROM " + db_table_name + " where ID= '" + ID + "'";

		try {
			// Create statement for use with the DB.
			Statement stmt = db.createStatement();

			// Get the result set for the sql query
			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				file_version  = rs.getInt("version");

				name = rs.getString("name");
				uid = rs.getString("uid");
				device_type = rs.getString("device_type");
				prev_IP = rs.getString("prev_IP");
				playerID = rs.getInt("playerID");
				game_ban_end_time = rs.getInt("game_ban_end_time");
				chat_ban_end_time = rs.getInt("chat_ban_end_time");
				creation_time = rs.getInt("creation_time");
				fealty0_nationID = rs.getInt("fealty0_nationID");
				fealty1_nationID = rs.getInt("fealty1_nationID");
				fealty2_nationID = rs.getInt("fealty2_nationID");
				fealty0_prev_attack_time = rs.getInt("fealty0_prev_attack_time");
				fealty1_prev_attack_time = rs.getInt("fealty1_prev_attack_time");
				fealty2_prev_attack_time = rs.getInt("fealty2_prev_attack_time");
				fealty_tournament_nationID = rs.getInt("fealty_tournament_nationID");
				fealty_tournament_start_day = rs.getInt("fealty_tournament_start_day");
				num_correlation_checks = rs.getInt("num_correlation_checks");
				correlation_counts = JSONToIntIntMap(rs.getString("correlation_counts"));
				correlation_records = CorrelationRecord.JSONToCorrelationRecordMap(rs.getString("correlation_records"));
				associated_devices = JSONToIntArray(rs.getString("associated_devices"));
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
		"name = '" + PrepStringForMySQL(name) + "', " +
		"uid = '" + PrepStringForMySQL(uid) + "', " +
		"device_type = '" + PrepStringForMySQL(device_type) + "', " +
		"prev_IP = '" + PrepStringForMySQL(prev_IP) + "', " +
		"playerID = '" + playerID + "', " +
		"chat_ban_end_time = '" + chat_ban_end_time + "', " +
		"creation_time = '" + creation_time + "', " +
		"fealty0_nationID = '" + fealty0_nationID + "', " +
		"fealty1_nationID = '" + fealty1_nationID + "', " +
		"fealty2_nationID = '" + fealty2_nationID + "', " +
		"fealty0_prev_attack_time = '" + fealty0_prev_attack_time + "', " +
		"fealty1_prev_attack_time = '" + fealty1_prev_attack_time + "', " +
		"fealty2_prev_attack_time = '" + fealty2_prev_attack_time + "', " +
		"fealty_tournament_nationID = '" + fealty_tournament_nationID + "', " +
		"fealty_tournament_start_day = '" + fealty_tournament_start_day + "', " +
		"num_correlation_checks = '" + num_correlation_checks + "', " +
		"correlation_counts = '" + IntIntMapToJSON(correlation_counts) + "', " +
		"correlation_records = '" + PrepStringForMySQL(CorrelationRecord.CorrelationRecordMapToJSON(correlation_records)) + "', " +
		"associated_devices = '" + IntArrayToJSON(associated_devices) + "', " +
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

	public String GetDeviceType()
	{
		return device_type;
	}

	public float GetOnlineCorrelation(int _deviceID)
	{
		if (num_correlation_checks < 10) {
			return 0f;
		}

		float correlation_count = correlation_counts.containsKey(_deviceID) ? correlation_counts.get(_deviceID) : 0f;

		return correlation_count / num_correlation_checks;
	}

	public CorrelationRecord GetCorrelationRecord(DeviceData _device_data)
	{
		CorrelationRecord record = null;

		if (correlation_records.containsKey(_device_data.ID) == false)
		{
			// Create a new CorrelationRecord for the given _deviceID.
			record = new CorrelationRecord();
			record.deviceID = _device_data.ID;
			correlation_records.put(_device_data.ID, record);

			// Mark this DeviceData to be updated.
			DataManager.MarkForUpdate(this);
		}
		else
		{
			record = correlation_records.get(_device_data.ID);
		}

		record.deviceData = _device_data;
		return record;
	}

	public void UpdateCorrelationsForActivity()
	{
		long cur_time_delta;

		prev_active_fine_time = Constants.GetFineTime();

		for (CorrelationRecord cur_record : tracking_correlations.values())
		{
			cur_time_delta = prev_active_fine_time- cur_record.deviceData.prev_active_fine_time;

			if (cur_time_delta <= 600000)
			{
				cur_record.count_interval_10m++;

				if (cur_time_delta <= 60000)
				{
					cur_record.count_interval_60s++;

					if (cur_time_delta <= 30000)
					{
						cur_record.count_interval_30s++;

						if (cur_time_delta <= 2000)
						{
							cur_record.count_interval_2s++;

							if (cur_time_delta <= 1000)
							{
								cur_record.count_interval_1s++;
							}
						}
					}
				}
			}
		}
	}

	public void RecordFealty(NationData _nationData)
	{
		// Record this latest attack in the user's DeviceData, for fealty.
		if (_nationData.level >= Constants.FEALTY_2_MIN_LEVEL)
		{
			fealty2_nationID = _nationData.ID;
			fealty2_prev_attack_time = Constants.GetTime();
		}
		else if (_nationData.level >= Constants.FEALTY_1_MIN_LEVEL)
		{
			fealty1_nationID = _nationData.ID;
			fealty1_prev_attack_time = Constants.GetTime();
		}
		else if (_nationData.level >= Constants.FEALTY_0_MIN_LEVEL)
		{
			fealty0_nationID = _nationData.ID;
			fealty0_prev_attack_time = Constants.GetTime();
		}

		//Output.PrintToScreen("Recorded for device " + ID + " fealty 0 nationID: " + fealty0_nationID + ", prev attack time: " + fealty0_prev_attack_time + ", cur time: " + Constants.GetTime());

		// If the nation is active in the current tournament, apply tournament fealty.
		if ((_nationData.tournament_active) && (_nationData.tournament_start_day == TournamentData.instance.start_day))
		{
			fealty_tournament_nationID = _nationData.ID;
			fealty_tournament_start_day = _nationData.tournament_start_day;
		}
	}

	public void CoassociateDevicesByUID()
	{
		String sql = "SELECT ID FROM " + db_table_name + " where uid= '" + BaseData.PrepStringForMySQL(uid) + "'";

		try {
			// Create statement for use with the DB.
			Statement stmt = db.createStatement();

			// Get the result set for the sql query
			ResultSet rs = stmt.executeQuery(sql);

			while (rs.next())
			{
				int curID = rs.getInt("ID");

				// Skip this device itself.
				if (curID == ID) {
					continue;
				}

				// Get the data for the device to associate.
				DeviceData assoc_device_data = (DeviceData)DataManager.GetData(Constants.DT_DEVICE, curID, false);

				if (assoc_device_data == null) {
					continue;
				}

				// Coassociate this device with the current device that shares the same UID.
				CoassociateDevices(this, assoc_device_data);
			}

			// Close statement and result set (must be done explicitly to avoid memory leak).
			try { if (rs != null) rs.close(); } catch (Exception e) {};
	    try { if (stmt != null) stmt.close(); } catch (Exception e) {};
		}
	  catch(Exception e)
		{
			Output.PrintToScreen("ERROR: Exception in CoassociateDevicesByUID().");
		}
	}

	// Associate any devices that have been found to be very closely correlated.
	public static void AssociateCorrelatedDevices()
	{
		int highestDeviceID = DataManager.GetHighestDataID(Constants.DT_DEVICE);

		for (int deviceID = 1; deviceID <= highestDeviceID; deviceID++)
		{
			// Get the current device's data
			DeviceData device_data = (DeviceData)DataManager.GetData(Constants.DT_DEVICE, deviceID, false);

			if (device_data == null) {
				continue;
			}

			if ((device_data.num_correlation_checks > 40) && (device_data.correlation_counts.size() > 0))
			{
				for (Map.Entry<Integer, Integer> entry : device_data.correlation_counts.entrySet())
				{
					float correlation = (float)(entry.getValue()) / (float)device_data.num_correlation_checks;

					if (correlation > 0.25f)
					{
						DeviceData correl_device_data = (DeviceData)DataManager.GetData(Constants.DT_DEVICE, entry.getKey(), false);

						// Skip this correlated device if the two devices are already associated.
						if (device_data.associated_devices.contains((Integer)correl_device_data.ID)) {
							continue;
						}

						if (device_data.tracking_correlations.containsKey(correl_device_data.ID))
						{
							// Get the device_data's CorrelationRecord for the correl_device_data.
							CorrelationRecord cor_record = device_data.tracking_correlations.get(correl_device_data.ID);

							// These two devices are candidates for association if their short term activity correlation is significantly less than would be expected given their long term activity correlation.
							if ((cor_record.count_interval_10m >= 2000) && ((cor_record.count_interval_30s <= (cor_record.count_interval_10m / 40)) || (cor_record.count_interval_2s <= (cor_record.count_interval_60s / 60)) || (cor_record.count_interval_1s <= (cor_record.count_interval_30s / 60))))
							{
								Output.PrintToScreen("  Assoc candidate: Dvc " + device_data.ID + " to dvc " + correl_device_data.ID + " (" + entry.getValue() + "/" + device_data.num_correlation_checks + ", " + correlation + "): Activity cor: 10m: " + cor_record.count_interval_10m + ", 60s: " + cor_record.count_interval_60s + ", 30s: " + cor_record.count_interval_30s + ", 2s: " + cor_record.count_interval_2s + ", 1s: " + cor_record.count_interval_1s);
							}
						}
					}
				}
			}
		}

		Output.PrintToScreen("AssociateCorrelatedDevices() complete.");
	}

	public static void InitDBTable()
	{
		// Create the user data table, if it doesn't yet exist.
		CreateTable(db, db_table_name);

		// Attempt to create each column
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD name VARCHAR(" + MAX_DEVICE_NAME_LEN + ")", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD uid VARCHAR(" + MAX_DEVICE_NAME_LEN + ")", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD device_type VARCHAR(" + MAX_DEVICE_TYPE_LEN + ")", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD prev_IP VARCHAR(" + MAX_IP_ADDRESS_LEN + ")", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD playerID INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD game_ban_end_time INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD chat_ban_end_time INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD creation_time INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD fealty0_nationID INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD fealty1_nationID INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD fealty2_nationID INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD fealty0_prev_attack_time INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD fealty1_prev_attack_time INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD fealty2_prev_attack_time INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD fealty_tournament_nationID INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD fealty_tournament_start_day INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD num_correlation_checks INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD correlation_counts TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD correlation_records TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD associated_devices TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD users TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD INDEX playerID (playerID)", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD INDEX playerID (playerID)", true, false);
	}

	public static void DeleteDBTable()
	{
		// Attempt to delete the DB table
		ExecuteUpdate(db, "DROP TABLE " + db_table_name, true, true);
	}

	public static DeviceData GetDeviceDataByName(String _device_name, String _device_UID)
	{
		boolean new_device = false;

		if (_device_name.equals(""))
		{
			Output.PrintToScreen("GetDeviceDataByName() given empty _device_name!");
			return null;
		}

		// Determine whether there exists a device record corresponding to the given device name.
		int deviceID = DeviceData.GetDeviceIDByName(_device_name);

		// If not, get the next available device data ID for the new device record.
		if (deviceID == -1)
		{
			deviceID = DataManager.GetNextDataID(Constants.DT_DEVICE);
			new_device = true;
		}

		// Get or create the device record.
		DeviceData deviceData = (DeviceData)DataManager.GetData(Constants.DT_DEVICE, deviceID, true); // Create, if it doesn't exist.

		// Record the device's name and uid (whether or not it's a new record, in case UID was not yet recorded).
		deviceData.name = _device_name;
		deviceData.uid = _device_UID;

		if (new_device)
		{
			// Record the device's creation time.
			deviceData.creation_time = Constants.GetTime();

			// Log this device.
			Constants.WriteToLog("log_devices.txt", Constants.GetTimestampString() + ": Device ID: " + deviceData.ID + ", name: " + _device_name + "\n");

			// Update the device data. Update immediately, so that searches for the device by its name will immediately be successful.
			DataManager.UpdateImmediately(deviceData);
		}

		return deviceData;
	}

	public static int GetDeviceIDByName(String _device_name)
	{
		String sql = "SELECT ID FROM " + db_table_name + " where name= '" + BaseData.PrepStringForMySQL(_device_name) + "'";

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

			//Output.PrintToScreen("GetDeviceIDByName(" + _device_name + ") returning " + result + ".");
			//Output.PrintStackTrace();
			return result;
		}
	  catch(Exception e)
		{
			//Output.PrintToScreen("GetDeviceIDByName(" + _device_name + ") returning -1.");
			//Output.PrintStackTrace();
			return -1;
		}
	}

	public static void CoassociateDevices(DeviceData _deviceData0, DeviceData _deviceData1)
	{
		// Make sure that _deviceData0 is associated with this _deviceData1.
		if (_deviceData0.associated_devices.contains(_deviceData1.ID) == false)
		{
			_deviceData0.associated_devices.add(_deviceData1.ID);
			DataManager.MarkForUpdate(_deviceData0);
		}

		// Make sure that _deviceData1 is associated with this _deviceData0.
		if (_deviceData1.associated_devices.contains(_deviceData0.ID) == false)
		{
			_deviceData1.associated_devices.add(_deviceData0.ID);
			DataManager.MarkForUpdate(_deviceData1);
		}
	}

	// Coassociate the given user with each of the given device's users.
	public static void CoassociateUsers(UserData _userData, DeviceData _deviceData)
	{
		for (int i = 0; i < _deviceData.users.size(); i++)
		{
			int assoc_userID = _deviceData.users.get(i);

			if ((assoc_userID == -1) || (assoc_userID == _userData.ID)) {
				continue;
			}

			// Get the UserData for the other device's current user that should be associated with this user.
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

	public static void RemoveUser(int _deviceID, int _userID)
	{
		// Get the device data.
		DeviceData device_data = (DeviceData)DataManager.GetData(Constants.DT_DEVICE, _deviceID, false);

		if (device_data == null) {
			return;
		}

		// Remove the given _userID from the device's list of users.
		if (device_data.users.contains(Integer.valueOf(_userID))) {
			device_data.users.remove(Integer.valueOf(_userID));
		}

		// Mark the dveice data to be updated.
		DataManager.MarkForUpdate(device_data);
	}

	public static void AssociateDeviceWithPlayer(String _device_name, String _device_uid, int _playerID)
	{
		// Get (or create) the DeviceData associated with the given _device_name.
		DeviceData deviceData = GetDeviceDataByName(_device_name, _device_uid);

		// If this device is already associated with the given _playerID, no need to change it.
		if (deviceData.playerID == _playerID) {
			return;
		}

		//Output.PrintToScreen("AssociateDeviceWithPlayer() associating device " + deviceData.ID + " (" + _device_name + ") with playerID " + _playerID);
		//Output.PrintStackTrace();

		// Set the device's playerID and userID.
		deviceData.playerID = _playerID;

		// Update the device data. Immediately write it to the DB so that a search by client ID string will work right away.
		DataManager.UpdateImmediately(deviceData);
	}

	public static void DisassociateDeviceFromPlayer(String _device_name)
	{
		if (_device_name.equals(""))
		{
			Output.PrintToScreen("AssociateDeviceWithPlayer() given empty _device_name!");
			return;
		}

		// Determine whether there exists a device record corresponding to the given device name.
		int deviceID = DeviceData.GetDeviceIDByName(_device_name);

		// If not, get the next available device data ID for the new device record.
		if (deviceID == -1)
		{
			Output.PrintToScreen("No record of device with name " + _device_name);
			return;
		}

		// Disassociate the device with the given ID from its playerID.
		DisassociateDeviceFromPlayer(deviceID);
	}

	public static void DisassociateDeviceFromPlayer(int _deviceID)
	{
		// Get the device record (do not create a new one).
		DeviceData deviceData = (DeviceData)DataManager.GetData(Constants.DT_DEVICE, _deviceID, false);

		if (deviceData != null)
		{
			// Clear the device's playerID.
			deviceData.playerID = -1;
		}

		// Update the device data.
		DataManager.MarkForUpdate(deviceData);
	}

	public static void RemovePlayerDevicesFromCache(int _playerID)
	{
		// Remove any DeviceData records associated with the given player ID from the cache so the modified version will be loaded next time they're needed.
		String sql = "SELECT " +
		"ID " +
		"FROM " + db_table_name + " where playerID= '" + _playerID + "'";

		try {
			// Create statement for use with the DB.
			Statement stmt = db.createStatement();

			// Get the result set for the sql query
			ResultSet rs = stmt.executeQuery(sql);

			while (rs.next()) {
				int curID = rs.getInt("ID");
				DataManager.RemoveFromCache(Constants.DT_DEVICE, curID);
			}

			// Close statement and result set (must be done explicitly to avoid memory leak).
			try { if (rs != null) rs.close(); } catch (Exception e) {};
	    try { if (stmt != null) stmt.close(); } catch (Exception e) {};
		}
		catch(Exception e)
		{
      Output.PrintToScreen("Couldn't fetch object with playerID " + _playerID + " from table '" + db_table_name + "'.");
			Output.PrintException(e);
		}
	}
}
