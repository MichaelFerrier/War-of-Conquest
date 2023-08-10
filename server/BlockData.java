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
import WOCServer.*;

public class BlockData extends BaseData
{
	public static String db_table_name = "Block";

	public static int VERSION = 1;

	// Flags
	public static final int BF_EXTENDED_DATA  = 1;
	public static final int BF_ISLAND         = 2;

	int terrain = 0;
	int nationID = -1;
	int flags = 0;

	// Transient data
	int hit_points_restored_time = -1;
	int lock_until_time = -1;
	int attack_complete_time = -1;
	int transition_complete_time = -1;
	public BlockTournamentRecord tournament_record = null;

	public BlockData()
	{
		super(Constants.DT_BLOCK, -1);
	}

	// Copy constructor
	public BlockData(BlockData _original)
	{
		super(Constants.DT_BLOCK, -1);

		terrain = _original.terrain;
		nationID = _original.nationID;
		flags = _original.flags;
		hit_points_restored_time = _original.hit_points_restored_time;
		lock_until_time = -1;
		attack_complete_time = -1;
		transition_complete_time = -1;
		marked_for_update = false;
	}

	public void CopyData(BlockData _original)
	{
		terrain = _original.terrain;
		nationID = _original.nationID;
		flags = _original.flags;
	}

	public void ResetTransientData()
	{
		hit_points_restored_time = -1;
		lock_until_time = -1;
		attack_complete_time = -1;
		transition_complete_time = -1;
	}

	// These are not implemented for BlockData -- reading and writing is handled by the LandMap class.
	public boolean ReadData() {return false;}
	public void WriteData() {}

	public static void CreateTable(Connection _db, String _db_table_name)
	{
		try {
			// Query db to determine whether the table called _db_table_name exists yet.
			Statement stmt = _db.createStatement();
			ResultSet resultSet = stmt.executeQuery("SHOW TABLES LIKE '" + _db_table_name + "'");

			// If no table with than name yet exists, attempt to create it.
			if(resultSet.next() == false)
			{
				String sql = "CREATE TABLE " + _db_table_name + " (ID INT not NULL, version INT, x INT, y INT, PRIMARY KEY (ID, x, y)) ENGINE = MyISAM ;";

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

	public static void InitDBTable()
	{
		// Create the user data table, if it doesn't yet exist.
		CreateTable(db, db_table_name);

		// Attempt to create each column
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD terrain INT DEFAULT 0", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD nationID INT DEFAULT -1", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD flags INT DEFAULT 0", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD hit_points INT DEFAULT 0", true, false);
	}

	public static void DeleteDBTable()
	{
		// Attempt to delete the DB table
		ExecuteUpdate(db, "DROP TABLE " + db_table_name, true, true);
	}

	public boolean BlockHasExtendedData()
	{
		return (flags & BF_EXTENDED_DATA) != 0;
	}

	public int GetBlockCurrentHitPoints(int _full_hit_points, float _hit_points_rate, int _cur_time)
	{
		int cur_hit_points;
		//Output.PrintToScreen("GetBlockCurrentHitPoints() hit_points_restored_time: " + hit_points_restored_time + ", _cur_time: " + _cur_time + ", _full_hit_points: " + _full_hit_points + ", _hit_points_rate: " + _hit_points_rate);

		if (hit_points_restored_time <= _cur_time) {
			cur_hit_points = _full_hit_points;
		} else {
			cur_hit_points = (int)((float)_full_hit_points - ((float)(hit_points_restored_time - Math.max(_cur_time, attack_complete_time)) * (_hit_points_rate / 60.0)) + 0.5f);
		}

		if (cur_hit_points < 0)
		{
			// TESTING
			NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);
			Output.PrintToScreen("ERROR: block " + hashCode() + " GetBlockCurrentHitPoints() has neg result " + cur_hit_points + ". _full_hit_points: " + _full_hit_points + ", _hit_points_rate: " + _hit_points_rate + ", _cur_time: " + _cur_time + ", hit_points_restored_time: " + hit_points_restored_time);
			Output.PrintToScreen("                                  nationID: " + nationID + ", nation hit points rate: " + Gameplay.GetNationHitPointsRate(nationData) + ", lock_until_time: " + lock_until_time + ", attack_complete_time: " + attack_complete_time + ", transition_complete_time: " + transition_complete_time);
			//Output.PrintStackTrace();
			//Constants.WriteToLog("log_block.txt", "ERROR: block " + hashCode() + " GetBlockCurrentHitPoints() has neg result " + cur_hit_points + ". _full_hit_points: " + _full_hit_points + ", _hit_points_rate: " + _hit_points_rate + ", _cur_time: " + _cur_time + ", hit_points_restored_time: " + hit_points_restored_time + "\n");
			//Constants.WriteToLog("log_block.txt", "                                  nationID: " + nationID + ", nation hit points rate: " + Gameplay.GetNationHitPointsRate(nationData) + ", lock_until_time: " + lock_until_time + ", attack_complete_time: " + attack_complete_time + ", transition_complete_time: " + transition_complete_time + "\n");
			cur_hit_points = 0;
		}

		return cur_hit_points;
	}

	public float GetBlockHitPointsRestored(float _hit_points_restore_rate, float _seconds)
	{
		// Determine whay fraction of the block's restore to use. If no previous attack on this block is in progress, then the full restore is used. If a previous attack on this
		// block IS still in progress, determine for what fraction of this attack's duration this will be the first (currently only) attack on this block.
		// The restore will only apply to this attack during that time period. Without this, the restore would be counted for multiple simultaneous attacks.
		float restore_fraction = (attack_complete_time <= Constants.GetTime()) ? 1f : (1f - ((float)Math.min(Constants.BATTLE_DURATION, (attack_complete_time - Constants.GetTime())) / (float)Constants.BATTLE_DURATION));

		//// TESTING
		//Output.PrintToScreen("GetBlockHitPointsRestored() for rate " + _hit_points_restore_rate + ", seconds: " + _seconds + ", cur time: " + Constants.GetTime() + ", attack_complete_time: " + attack_complete_time + ", restore_fraction: " + restore_fraction + ", total: " + (restore_fraction * _seconds * _hit_points_restore_rate / 60.0f));

		return restore_fraction * _seconds * _hit_points_restore_rate / 60.0f;
	}

	public void SetHitPointsRestoredTime(int _time/*, LandMap _land_map, int _x, int _y*/)
	{
		// Record the block's new hit_points_restored_time.
		hit_points_restored_time = _time;
/*
		// TESTING -- NOTE THAT THE _land_map, _x, and _y params are only needed for the below test.
		// ONCE NO LONGER SEEING the error below or in GetBlockCurrentHitPoints(), the below code and those 3 params can be removed.
		// UPDATE: This problem still happens rarely. Maybe because the nationID somehow changes after SetHitPointsRestoredTime() is called? Too rare to bother with unless it causes a problem.
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);
		float hit_points_rate = Gameplay.GetNationHitPointsRate(nationData);
		float full_hit_points = _land_map.DetermineBlockFullHitPoints(_x, _y, false, null);
		int after_attack_time = Constants.GetTime() + Constants.BATTLE_DURATION;
		Constants.WriteToLog("log_block.txt", "SetHitPointsRestoredTime() for block " + hashCode() + " map " + _land_map.ID + ", pos " + _x + "," + _y + " setting restored time to " + hit_points_restored_time + ", that's " + (hit_points_restored_time - Constants.GetTime()) + " secs after now, " +  (hit_points_restored_time - after_attack_time) + " secs after attack. hit_points_rate: " + hit_points_rate + " full hit points: " + full_hit_points + ". hit_points_restored_time: " + hit_points_restored_time + ", nationID: " + nationID + ", current time: " + Constants.GetTime() + ", after_attack_time: " + after_attack_time + "\n");
		if ((hit_points_restored_time > Constants.GetTime()) && (((((float)(hit_points_restored_time - after_attack_time)) / 60f) * hit_points_rate) > (full_hit_points + 5)))
		{
			Output.PrintToScreen("ERROR: SetHitPointsRestoredTime() for block " + hashCode() + " map " + _land_map.ID + ", pos " + _x + "," + _y + " setting restored time to " + (hit_points_restored_time - after_attack_time) + " secs after attack. At hit_points_rate of " + hit_points_rate + ", that is time to generate more than the full hit points: " + full_hit_points + ". hit_points_restored_time: " + hit_points_restored_time + ", nationID: " + nationID + ", current time: " + Constants.GetTime() + ", after_attack_time: " + after_attack_time);
			Output.PrintStackTrace();
		}
		*/
	}

	public void SetAttackCompleteTime(int _attack_complete_time)
	{
		// Record the time when the latest attack on this block will be complete.
		attack_complete_time = _attack_complete_time;
	}
}
