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

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.json.simple.*;
import org.json.simple.parser.*;
import ij.*;
import ij.io.*;
import ij.process.*;
import ij.plugin.filter.*;
import ij.plugin.frame.*;
import ij.gui.*;
import java.awt.*;
import java.awt.color.*;
import WOCServer.*;

public class Admin
{
	static int prev_emergency_time = 0;
	static int EMERGENCY_PERIOD = 7200; // 2 hours

	static String track_clientID = "";

	static byte COLOR_INDEX_LAND = 0;
	static byte COLOR_INDEX_WATER = 1;
	static byte COLOR_INDEX_MOUNTAIN = 2;
	static byte COLOR_INDEX_ORB_BASE = 8;
	static byte COLOR_INDEX_RESOURCE_BASE = 16;

	static final int ISLAND_CHECK_RANGE = 20;

  public static void AddCredits(StringBuffer output_buffer, String nation_name, int num_credits, boolean _purchased, boolean console)
  {
  	int nationID = DataManager.GetNameToIDMap(Constants.DT_NATION, nation_name);
  	NationData nation_data = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

  	if ((nation_data == null) || (nationID <= 0))
  	{
      if(console)
      {
  		  Output.PrintToScreen("Unknown nation: " + nation_name);
      }
      else
      {
		    Output.PrintToScreen("Admin Client Add Credits Error - Unknown nation: " + nation_name);
  	    OutputEvents.GetMessageEvent(output_buffer, ClientString.Get("svr_msg_unknown_nation", "nation_name", nation_name)); // "Unknown nation: " + nation_name
      }
      return;
  	}
  	else
  	{
			// Add the given number of credits to this nation.
			Money.AddGameMoney(nation_data, num_credits, _purchased ? Money.Source.PURCHASED : Money.Source.FREE);

			// Broadcast an update event to the nation, letting all players know about the change in credits.
			OutputEvents.BroadcastUpdateEvent(nationID);

  		// Mark the nation's data to be updated
  		DataManager.MarkForUpdate(nation_data);
      //Constants.WriteToLog("log_MFU.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " "  + nation_data.ID + " evt: AWARD_CREDITS\n");

      if(console)
      {
  		  Output.PrintToScreen(num_credits + " credits added to " + nation_name);
      }
      else
      {
		    Output.PrintToScreen("Admin Client Add Credits Success: " + num_credits + " credits awarded to " + nation_name);
  	    OutputEvents.GetMessageEvent(output_buffer, ClientString.Get("svr_msg_credits_added", "num_credits", String.valueOf(num_credits), "nation_name", nation_name)); // num_credits + " credits added to " + nation_name
      }
      return;
  	}
  }

  public static void AddCreditsAll(int _num_credits, boolean _purchased)
  {
		NationData nation_data;
		int highest_nation_ID = DataManager.GetHighestDataID(Constants.DT_NATION);
		for (int nationID = 1; nationID <= highest_nation_ID; nationID++)
		{
			// Get the current nation's data
			nation_data = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

			// If there is no nation with the current nationID, continue on to the next.
			if (nation_data == null) {
				continue;
			}

			// Add the given number of credits to this nation.
			Money.AddGameMoney(nation_data, _num_credits, _purchased ? Money.Source.PURCHASED : Money.Source.FREE);

			// Broadcast an update event to the nation, letting all players know about the change in credits.
			OutputEvents.BroadcastUpdateEvent(nationID);

      // Mark the nation's data to be updated
      DataManager.MarkForUpdate(nation_data);
		  //Output.PrintToScreen(_num_credits + " credits added to " + nation_data.name);
		}

		Output.PrintToScreen(_num_credits + " credits added to all nations.");
  }

  public static void AwardLevels(String nation_name, int num_levels)
  {
	  int nationID = DataManager.GetNameToIDMap(Constants.DT_NATION, nation_name);
		NationData nation_data = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

		if (nation_data == null)
		{
			Output.PrintToScreen("Unknown nation: '" + nation_name + "'");
		}
		else
		{
			nation_data.level += num_levels;
			Output.PrintToScreen(num_levels + " levels awarded to '" + nation_name + "'");

			// Mark the nation's data to be updated
			DataManager.MarkForUpdate(nation_data);
    }
  }

	public static void AddAdvance(String _nation_name, int _advanceID)
	{
		int nationID = DataManager.GetNameToIDMap(Constants.DT_NATION, _nation_name);
  	NationData nation_data = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);
		TechData tech_data = TechData.GetTechData(_advanceID);

  	if (nation_data == null)
  	{
 		  Output.PrintToScreen("Unknown nation: " + _nation_name);
      return;
  	}
		else if (tech_data == null)
  	{
 		  Output.PrintToScreen("Unknown advance ID: " + _advanceID);
      return;
  	}
  	else
  	{
			// Add the given advance to the given nation.
			Technology.AddTechnology(nationID, _advanceID, 0f, true, true, 0);
			Output.PrintToScreen("Added advance '" + tech_data.name + "' (" + _advanceID + ") to nation " + nation_data.name);
		}
	}

	public static void RemoveAdvance(String _nation_name, int _advanceID)
	{
		int nationID = DataManager.GetNameToIDMap(Constants.DT_NATION, _nation_name);
  	NationData nation_data = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);
		TechData tech_data = TechData.GetTechData(_advanceID);

  	if (nation_data == null)
  	{
 		  Output.PrintToScreen("Unknown nation: " + _nation_name);
      return;
  	}
		else if (tech_data == null)
  	{
 		  Output.PrintToScreen("Unknown advance ID: " + _advanceID);
      return;
  	}
  	else
  	{
			// Remove the given advance from the given nation.
			Technology.RemoveTechnology(nationID, _advanceID, -1);
			Output.PrintToScreen("Removed advance '" + tech_data.name + "' (" + _advanceID + ") from nation " + nation_data.name);
		}
	}

	public static void AddAdvancePoints(String _nation_name, int _num)
	{
		int nationID = DataManager.GetNameToIDMap(Constants.DT_NATION, _nation_name);
  	NationData nation_data = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

  	if (nation_data == null)
  	{
 		  Output.PrintToScreen("Unknown nation: " + _nation_name);
      return;
  	}
  	else
  	{
			// Add the given number of advance points to the given nation.
			nation_data.advance_points = Math.max(0, nation_data.advance_points + _num);
			DataManager.MarkForUpdate(nation_data);
			Output.PrintToScreen("Added " + _num + " advance points to nation " + nation_data.name + " (" + nation_data.ID + "). New total: " + nation_data.advance_points);
		}
	}

	public static void AddCountdown(String _nation_name, int _amount, boolean _purchased)
	{
		int nationID = DataManager.GetNameToIDMap(Constants.DT_NATION, _nation_name);
		NationData nation_data = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

		if (nation_data == null)
		{
			Output.PrintToScreen("Unknown nation: '" + _nation_name + "'");
		}
		else
		{
			Gameplay.ChangeRebirthCountdown(nation_data, (float)_amount);

			if (_purchased)
			{
				// Keep track of the amount of rebirth countdown this nation has purchased.
				nation_data.rebirth_countdown_purchased += _amount;
			}

			Output.PrintToScreen(_amount + " countdown awarded to '" + _nation_name + "'");

			// Mark the nation's data to be updated
			DataManager.MarkForUpdate(nation_data);
		}
	}

	public static void AddCountdownAll(int _amount, boolean _purchased)
	{
		NationData nationData;
		int highest_nation_ID = DataManager.GetHighestDataID(Constants.DT_NATION);
		for (int nationID = 1; nationID <= highest_nation_ID; nationID++)
		{
			// Get the current nation's data
			nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

			// If there is no nation with the current nationID, continue on to the next.
			if (nationData == null) {
				continue;
			}

			// Add the given amount to the nation's countdown.
			Gameplay.ChangeRebirthCountdown(nationData, _amount);

			if (_purchased)
			{
				// Keep track of the amount of rebirth countdown this nation has purchased.
				nationData.rebirth_countdown_purchased += _amount;
			}

			// Mark the nation's data to be updated
			DataManager.MarkForUpdate(nationData);
		}

		Output.PrintToScreen("Countdowns awarded");
	}

	public static void AddDayCountdownAll()
	{
		int cd_per_day;
		NationData nationData;
		int highest_nation_ID = DataManager.GetHighestDataID(Constants.DT_NATION);
		for (int nationID = 1; nationID <= highest_nation_ID; nationID++)
		{
			// Get the current nation's data
			nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

			// If there is no nation with the current nationID, continue on to the next.
			if (nationData == null) {
				continue;
			}

			// Determine one day's worth of rebirth countdown for this nation.
			cd_per_day = nationData.level - nationData.GetFinalRebirthAvailableLevel();

			if (cd_per_day > 0)
			{
				// Add the determined number to the nation's countdown.
				Gameplay.ChangeRebirthCountdown(nationData, (float)cd_per_day);

				// Mark the nation's data to be updated
				DataManager.MarkForUpdate(nationData);
			}
		}

		Output.PrintToScreen("Countdowns awarded");
	}

  public static void AddEnergy(StringBuffer output_buffer, String nation_name, int _amount, boolean console)
  {
  	int nationID = DataManager.GetNameToIDMap(Constants.DT_NATION, nation_name);
  	NationData nation_data = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

  	if (nation_data == null)
  	{
 		  Output.PrintToScreen("Unknown nation: " + nation_name);
      return;
  	}
  	else
  	{
			// Add the energy
			nation_data.energy = Math.max(0, Math.min(nation_data.GetFinalEnergyMax(), nation_data.energy + _amount));

			// Broadcast update event, with new energy amount, to the nation.
			OutputEvents.BroadcastUpdateEvent(nationID);

			// Mark the nation's data to be updated
  		DataManager.MarkForUpdate(nation_data);

 		  Output.PrintToScreen(_amount + " energy added to " + nation_name);
      return;
		}
	}

	public static void AddManpower(StringBuffer output_buffer, String nation_name, int _amount, boolean console)
  {
  	int nationID = DataManager.GetNameToIDMap(Constants.DT_NATION, nation_name);
  	NationData nation_data = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

  	if (nation_data == null)
  	{
 		  Output.PrintToScreen("Unknown nation: " + nation_name);
      return;
  	}
  	else
  	{
			// Add the manpower
			nation_data.mainland_footprint.manpower = Math.max(0, Math.min(nation_data.GetMainlandManpowerMax(), nation_data.mainland_footprint.manpower + _amount));

			// Broadcast update event, with new manpower amount, to the nation.
			OutputEvents.BroadcastUpdateEvent(nationID);

			// Mark the nation's data to be updated
  		DataManager.MarkForUpdate(nation_data);

 		  Output.PrintToScreen(_amount + " manpower added to " + nation_name);
      return;
		}
	}

	public static void AddManpowerAll(int _amount)
	{
		NationData nationData;
		int highest_nation_ID = DataManager.GetHighestDataID(Constants.DT_NATION);
		for (int nationID = 1; nationID <= highest_nation_ID; nationID++)
		{
			// Get the current nation's data
			nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

			// If there is no nation with the current nationID, continue on to the next.
			if (nationData == null) {
				continue;
			}

			// Add the manpower
			nationData.mainland_footprint.manpower = Math.max(0, Math.min(nationData.GetMainlandManpowerMax(), nationData.mainland_footprint.manpower + _amount));

			// Broadcast update event, with new manpower amount, to the nation.
			OutputEvents.BroadcastUpdateEvent(nationID);

			// Mark the nation's data to be updated
  		DataManager.MarkForUpdate(nationData);
		}

		Output.PrintToScreen("Manpower awarded");
	}

	public static void AddPrizeMoneyHistory(StringBuffer output_buffer, String nation_name, int _amount, boolean console)
	{
		int nationID = DataManager.GetNameToIDMap(Constants.DT_NATION, nation_name);
  	NationData nation_data = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

  	if (nation_data == null)
  	{
 		  Output.PrintToScreen("Unknown nation: " + nation_name);
      return;
  	}
  	else
  	{
			// Add to the nation's prize_money_history
			nation_data.prize_money_history = Math.max(0, nation_data.prize_money_history + _amount);

			// Mark the nation's data to be updated
  		DataManager.MarkForUpdate(nation_data);

 		  Output.PrintToScreen(_amount + " added to " + nation_name + "'s prize_money_history. Total is: " + nation_data.prize_money_history);
      return;
		}
	}

  public static void AddNationXP(StringBuffer output_buffer, String nation_name, int _num_xp, boolean console)
  {
  	int nationID = DataManager.GetNameToIDMap(Constants.DT_NATION, nation_name);
  	NationData nation_data = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

  	if (nation_data == null)
  	{
      if(console)
      {
  		  Output.PrintToScreen("Unknown nation: " + nation_name);
      }
      else
      {
		    Output.PrintToScreen("Admin Client Award Nation Points Error: Unknown nation: " + nation_name);
  	    OutputEvents.GetMessageEvent(output_buffer, ClientString.Get("svr_msg_unknown_nation", "nation_name", nation_name)); // "Unknown nation: " + nation_name
      }
      return;
  	}
  	else
  	{
      Gameplay.AddXP(nation_data, _num_xp, -1, -1, -1, true, false, 0, Constants.XP_ADMIN);

			// Mark the nation's data to be updated
  		DataManager.MarkForUpdate(nation_data);
       //Constants.WriteToLog("log_MFU.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " "  + nation_data.ID + " evt: AWARD_XP\n");

      if(console)
      {
  		  Output.PrintToScreen(_num_xp + " points awarded to " + nation_name);
      }
      else
      {
  		  Output.PrintToScreen("Admin Client Award Nation Points Success: " + _num_xp + " points awarded to " + nation_name);
    	  OutputEvents.GetMessageEvent(output_buffer, ClientString.Get(_num_xp + " points added to " + nation_name));
      }
      return;
		}
  }

	public static void AddUserXP(StringBuffer output_buffer, String user_name, int _num_xp, boolean console)
  {
  	int userID = UserData.GetUserIDByUsername(user_name);
  	UserData user_data = (UserData)DataManager.GetData(Constants.DT_USER, userID, false);

  	if (user_data == null)
  	{
      if(console)
      {
  		  Output.PrintToScreen("Unknown user: " + user_name);
      }
      else
      {
		    Output.PrintToScreen("Admin Client Award User XP Error: Unknown user: " + user_name);
  	    OutputEvents.GetMessageEvent(output_buffer, ClientString.Get("svr_msg_unknown_user", "user_name", user_name)); // "Unknown user: " + user_name
      }
      return;
  	}
  	else
  	{
			// Add the xp to the user's account.
			Gameplay.AddXPToUser(userID, _num_xp);

			if(console)
      {
  		  Output.PrintToScreen(_num_xp + " points awarded to " + user_name);
      }
      else
      {
  		  Output.PrintToScreen("Admin Client Award User XP Success: " + _num_xp + " points awarded to " + user_name);
    	  OutputEvents.GetMessageEvent(output_buffer, ClientString.Get(_num_xp + " points added to " + user_name));
      }
      return;
		}
  }

  public static void AddVoucher(String _voucher_code, int _amount)
  {
		int remaining = VoucherData.AddValueToVoucher(_voucher_code, _amount, false);

		Output.PrintToScreen(_amount + " credits added to voucher code '" + _voucher_code + "'. Voucher remaining credits: " + remaining + ".");
  }

	public static void AddVouchersFromFile(String _filename)
  {
		int line_num = -1;
		String value_string, code;
		int num_credits, vouchers_count = 0, total_num_credits = 0;
		BufferedReader br;

		try
		{
			br = new BufferedReader(new FileReader(_filename));
		}
		catch (Exception e)
		{
			Output.PrintToScreen("File not found: " + _filename + ", message: " + e.getMessage());
			return;
		}

		try
		{
			String line;
			int[] place = new int[1];

			while ((line = br.readLine()) != null)
			{
				// Increment line number.
				line_num++;

				// Process the line.

				// Skip comments
				if (line.charAt(0) == '#') {
					continue;
				}

				// Start at the beginning of the line.
				place[0] = 0;

				// Code
				code = Constants.GetNextTabSeparatedValue(line, place);

				// Number of Credits
				value_string = Constants.GetNextTabSeparatedValue(line, place);
				//Output.PrintToScreen("code: " + code + ", value_string: " + value_string);
				num_credits = value_string.isEmpty() ? -1 : Integer.parseInt(value_string);

				// Add the current voucher
				VoucherData.AddValueToVoucher(code, num_credits, true);

				// Keep tally of totals
				vouchers_count++;
				total_num_credits += num_credits;
			}

			Output.PrintToScreen(vouchers_count + " vouchers added, totaling " + total_num_credits + " credits.");
		}
		catch (Exception e)
		{
			Output.PrintToScreen("Error adding vouchers from " + _filename + " at line " + line_num);
			Output.PrintException(e);
			return;
		}
	}

	public static void AllowEmblem(String _username, int _emblem_index)
	{
		int playerID = AccountDB.GetPlayerIDByUsername(_username);

		if (playerID == -1)
		{
			Output.PrintToScreen("There is no player account with username '" + _username + "'.");
			return;
		}

		PlayerAccountData account = AccountDB.ReadPlayerAccount(playerID);

		if (account == null)
		{
			Output.PrintToScreen("Couldn't fetch player account data for ID " + playerID + ".");
			return;
		}

		account.info += "<emblem index=\"" + _emblem_index + "\">";

		// Store the modified player account data.
		AccountDB.WritePlayerAccount(account);

		Output.PrintToScreen("Emblem index " + _emblem_index + " added to " + _username + "'s player info.");
	}

	public static void RemoveEmblem(String _username, int _emblem_index)
	{
		int playerID = AccountDB.GetPlayerIDByUsername(_username);

		if (playerID == -1)
		{
			Output.PrintToScreen("There is no player account with username '" + _username + "'.");
			return;
		}

		PlayerAccountData account = AccountDB.ReadPlayerAccount(playerID);

		if (account == null)
		{
			Output.PrintToScreen("Couldn't fetch player account data for ID " + playerID + ".");
			return;
		}

		account.info = account.info.replaceAll("<emblem index=\"" + _emblem_index + "\">", "");

		// Store the modified player account data.
		AccountDB.WritePlayerAccount(account);

		Output.PrintToScreen("Emblem index " + _emblem_index + " removed from " + _username + "'s player info.");
	}

  public static void GameBanUser(StringBuffer output_buffer, String user_name, int num_hours, boolean console, boolean complaint)
  {
		int userID = UserData.GetUserIDByUsername(user_name);
		UserData user_data = (UserData)DataManager.GetData(Constants.DT_USER, userID, false);

		if (user_data == null)
		{
      if(console)
      {
		    Output.PrintToScreen("Unknown user: " + user_name);
      }
      else
      {
		    Output.PrintToScreen("Admin Client Ban User Error: Unknown user: " + user_name);
  	    OutputEvents.GetMessageEvent(output_buffer, ClientString.Get("svr_msg_unknown_user", "username", user_name)); // "Unknown user: " + user_name
      }
      return;
		}

		// Ban the user
		GameBanUser(user_data, num_hours);

    if(console)
    {
      Constants.WriteToLog("log_ban.txt",Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " Server command banned " + user_data.name + " (u:" + userID + ") for " + num_hours + " hours.\n");
	    Output.PrintToScreen("User account " + user_name + " banned for " + num_hours + " hours.");
      return;
    }
    else if(complaint)
    {
	    OutputEvents.GetMessageEvent(output_buffer, ClientString.Get("User account " + user_name + " banned for " + num_hours  + " hours."));
      Constants.WriteToLog("log_ban.txt",Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " Complaint Process banned " + user_data.name + " (u:" + userID + ") for " + num_hours + " hours.\n");
      return;
    }
    else
    {
	    Output.PrintToScreen("Admin Client Ban User Success: User account " + user_name + " banned for " + num_hours  + " hours.");
	    OutputEvents.GetMessageEvent(output_buffer, ClientString.Get("User account " + user_name + " banned for " + num_hours  + " hours."));
      Constants.WriteToLog("log_ban.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " Admin Client banned " + user_data.name + " (u:" + userID + ") for " + num_hours + " hours.\n");
    }
	}

	public static void GameBanUser(UserData _user_data, int num_hours)
  {
    String expire_time = "";

		// Record new ban end time
    if (num_hours == 0) {
			_user_data.game_ban_end_time = -1;
    }	else {
			_user_data.game_ban_end_time = Math.max(Constants.GetTime(), _user_data.game_ban_end_time) + (num_hours * Constants.SECONDS_PER_HOUR);
		}

		// Record the ban in the player's account.
		_user_data.UpdateComplaintAndBanCounts(0, 0, 0, 0, 1);

		// Copy the user's bans to ist asociated users and devices.
		_user_data.CopyBansToAssociatedUsersAndDevices();

		// Propagate the new game_ban_end_time to the user's player data.
		PlayerAccountData account = AccountDB.ReadPlayerAccount(_user_data.playerID);
		if (account != null)
		{
			account.game_ban_end_time = _user_data.game_ban_end_time;
			AccountDB.WritePlayerAccount(account);
		}

		// Mark the user data to be updated
		DataManager.MarkForUpdate(_user_data);

		if (_user_data.game_ban_end_time > Constants.GetTime())
		{
			// Log out the banned user if they are logged in.

			// Get the current user's clientThread (if they're connected)
			ClientThread clientThread = WOCServer.GetClientThread(_user_data.ID);

			Output.PrintToScreen("    Applying to user: " + _user_data.ID + ", logged in: " + ((clientThread == null) ? "No" : "Yes"));

			if (clientThread != null)
			{
				// Force the banned user's client to exit the game.
				Login.ForceExitGame(clientThread, ClientString.Get("svr_login_account_disabled", "num_hours", String.valueOf(num_hours), "hour_quant", ((num_hours != 1) ? "{Server Strings/hour_quant_plural}" : "{Server Strings/hour_quant_singular}"))); // "This account is disabled for " + num_hours + " hour" + ((num_hours != 1) ? "s" : "") + ".")
			}
		}
  }

	public static void BlockInfo(int _x, int _y)
	{
		// Get the land map
		LandMap land_map = DataManager.GetLandMap(Constants.MAINLAND_MAP_ID, false);

		// Get the block's data and extended data.
		BlockData block_data = land_map.GetBlockData(_x, _y);
		BlockExtData block_ext_data = land_map.GetBlockExtendedData(_x, _y, false);

		if (block_data == null)
		{
			Output.PrintToScreen("There is no block as coords " + _x + "," + _y);
			return;
		}

		NationData nationData = null;
		if (block_data.nationID != -1)
		{
			// Get the block's nation's data
			nationData = (NationData)DataManager.GetData(Constants.DT_NATION, block_data.nationID, false);
		}

		Output.PrintToScreen("============================");
		Output.PrintToScreen(" Block " + _x + "," + _y + " terrain: " + block_data.terrain + ", nationID: " + block_data.nationID + ((nationData == null) ? "(None)" : "(" + nationData.name + ")") + ", flags: " + block_data.flags);
		Output.PrintToScreen(" Cur time: " + Constants.GetTime() + ", hit_points_restored_time: " + block_data.hit_points_restored_time + ", lock_until_time: " + block_data.lock_until_time);
		Output.PrintToScreen(" attack_complete_time: " + block_data.attack_complete_time + ", transition_complete_time: " + block_data.transition_complete_time);

		if (block_ext_data != null)
		{
			ObjectData objectData = ObjectData.GetObjectData(block_ext_data.objectID);
			BuildData buildData = BuildData.GetBuildData(block_ext_data.objectID);
			NationData ownerNationData = (NationData)DataManager.GetData(Constants.DT_NATION, block_ext_data.owner_nationID, false);
			NationData wipeNationData = (NationData)DataManager.GetData(Constants.DT_NATION, block_ext_data.wipe_nationID, false);

			Output.PrintToScreen(" objectID: " + block_ext_data.objectID + ((objectData == null) ? ((buildData == null) ? "(None)" : "(" + buildData.name + ")") : "(" + objectData.name + ")") + ", owner_NationID: " + block_ext_data.owner_nationID + ((ownerNationData == null) ? "(None)" : "(" + ownerNationData.name + ")") );
			Output.PrintToScreen(" creation_time: " + block_ext_data.creation_time + ", completion_time: " + block_ext_data.completion_time);
			Output.PrintToScreen(" invisible_time: " + block_ext_data.invisible_time + ", capture_time: " + block_ext_data.capture_time);
			Output.PrintToScreen(" crumble_time: " + block_ext_data.crumble_time + ", triggerable_time: " + block_ext_data.triggerable_time);
			Output.PrintToScreen(" wipe_nationID: " + block_ext_data.wipe_nationID + ((wipeNationData == null) ? "(None)" : "(" + wipeNationData.name + ")") + ", wipe_flags: " + block_ext_data.wipe_flags + ", wipe_end_time: " + block_ext_data.wipe_end_time);
		}

		Output.PrintToScreen("============================");
	}

	public static void Build(int _buildID, int _x, int _y, int _landmapID)
	{
		// Get the build data
		BuildData build_data = BuildData.GetBuildData(_buildID);

		if (build_data == null) {
			Output.PrintToScreen("No build object with ID " + _buildID);
			return;
		}

		// If no landmap ID is given, default to mainland map.
		if (_landmapID <= 0) {
			_landmapID = Constants.MAINLAND_MAP_ID;
		}

		// Get the land map
		LandMap land_map = DataManager.GetLandMap(_landmapID, false);

		if (land_map == null) {
			Output.PrintToScreen("LandMap with ID " + _landmapID + " doesn't exist.");
			return;
		}

		// Get the block's data and extended data.
		BlockData block_data = land_map.GetBlockData(_x, _y);
		BlockExtData block_ext_data = land_map.GetBlockExtendedData(_x, _y, true);

		if ((block_data == null) || (block_ext_data == null)) {
			return;
		}

		if (build_data == null) {
			Output.PrintToScreen("Block " + _x + "," + _y + " doesn't exist.");
			return;
		}

		if (block_data.nationID == -1) {
			Output.PrintToScreen("Block " + _x + "," + _y + " doesn't belong to any nation.");
			return;
		}

		// Get the block's nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, block_data.nationID, false);

		// If the block contains a build object that has crumbled, remove that build object.
		land_map.CheckForBuildObjectCrumble(_x, _y, block_data, block_ext_data);

		// If this block already contains an object owned by the nation occupying the block, remove that old structure's energy burn rate and capacity from the nation.
		if ((block_ext_data.objectID != -1) && (block_ext_data.owner_nationID == block_data.nationID))
		{
			BuildData removed_build_data = BuildData.GetBuildData(block_ext_data.objectID);

			// Determine the removed structure's energy burn rate for this nation.
			float removed_energy_burn_rate = nationData.DetermineDiscountedEnergyBurn(removed_build_data);

			Gameplay.ModifyEnergyBurnRate(nationData, _landmapID, -removed_energy_burn_rate);

			// Remove from the nation any storage capacity this object may have.
			Gameplay.ModifyStatsForObjectCapacity(nationData, removed_build_data, true, false);
		}

		// Build the structure
		block_ext_data.objectID = _buildID;
		block_ext_data.owner_nationID = block_data.nationID;
		block_ext_data.creation_time = Constants.GetTime();
		block_ext_data.completion_time = Constants.GetTime(); // Have it be completed immediately.
		block_ext_data.invisible_time = Gameplay.DetermineBuildInvisibileTime(build_data, nationData);
		block_ext_data.capture_time = -1;
		block_ext_data.crumble_time = -1;

		// Add to the nation any storage capacity this object may have.
		Gameplay.ModifyStatsForObjectCapacity(nationData, build_data, false, false);

		Output.PrintToScreen("Built object \"" + build_data.name + "\" belonging to " + nationData.name + " at " + _x + "," + _y + ".");

		// Determine the new structure's energy burn rate for this nation.
		float energy_burn_rate = nationData.DetermineDiscountedEnergyBurn(build_data);

		// Take cost from nation
		Gameplay.ModifyEnergyBurnRate(nationData, _landmapID, energy_burn_rate);

		// Broadcast a stats event to this nation, in case of change to energy burn rate or change due to object capacity.
		OutputEvents.BroadcastStatsEvent(nationData.ID, 0);

		// If this build may affect which of this nation's build objects are inert, broadcast message to update clients.
		if (nationData.GetFinalEnergyBurnRate(_landmapID) > nationData.GetFinalEnergyRate(_landmapID)) {
			OutputEvents.BroadcastUpdateEvent(block_data.nationID);
		}

		// Update quests system for this build.
		Quests.HandleBuild(nationData, build_data, 0);

		// Mark nation and block data to be updated
		DataManager.MarkBlockForUpdate(land_map, _x, _y);
		DataManager.MarkForUpdate(nationData);

		// Broadcast the change to this block to all local clients.
		OutputEvents.BroadcastBlockExtendedDataEvent(land_map, _x, _y);

		// If this event took place on a raid map, record it in the raid's replay.
		if (land_map.ID >= Raid.RAID_ID_BASE) {
			Raid.RecordEvent_ExtendedData(land_map.ID, land_map, _x, _y);
		}
	}

  public static void ClearChatLists()
  {
		NationData nation_data;
		int highest_nation_ID = DataManager.GetHighestDataID(Constants.DT_NATION);
		for (int nationID = 1; nationID <= highest_nation_ID; nationID++)
		{
			// Get the current nation's data
			nation_data = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

			// If there is no nation with the current nationID, continue on to the next.
			if (nation_data == null) {
				continue;
			}

			if ((nation_data.chat_list.size() > 0) || (nation_data.reverse_chat_list.size() > 0))
			{
				// Clear the nation's chat list and reverse chat list.
				nation_data.chat_list.clear();
				nation_data.reverse_chat_list.clear();

  			// Mark the nation's data to be updated
  			DataManager.MarkForUpdate(nation_data);
  		  Output.PrintToScreen("Chat List Cleared for Nation " + nation_data.name + ".");
			}
		}
  }

	public static void ResolveComplaint(StringBuffer _output_buffer, int _userID, int _complaintID, int _action, int _ban_days, String _message, String _log)
	{
		UserData user_data = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		// if the user doesn't exist or is not a mod or admin, do nothing.
		if ((user_data == null) || ((user_data.mod_level == 0) &&(!user_data.admin))) {
			return;
		}

		ComplaintData complaintData = (ComplaintData)DataManager.GetData(Constants.DT_COMPLAINT, _complaintID, false);

		// If the complaint with the given ID doesn't exist, just fetch first complaint in list.
		if (complaintData == null)
		{
			OutputEvents.GetNextComplaintEvent(_output_buffer, _userID, 0);
			return;
		}

		// Get data about both the reporting and reported users.
		UserData reporter_userData = (UserData)DataManager.GetData(Constants.DT_USER, complaintData.userID, false);
		UserData reported_userData = (UserData)DataManager.GetData(Constants.DT_USER, complaintData.reported_userID, false);

		if ((reporter_userData != null) && (reported_userData != null))
		{
			if (_action == Constants.COMPLAINT_ACTION_NO_ACTION)
			{
				Constants.WriteToLog("log_resolved_complaints.txt", Constants.GetTimestampString() + " Mod " + user_data.name + " (" + _userID + ") took no action for complaint " + _complaintID + ", submitted by " + reporter_userData.name + " (" + complaintData.userID + ") against " + reported_userData.name + " (" + complaintData.reported_userID + ") for issue: '" + complaintData.issue + "', text: '" + complaintData.text + "', message: '" + _message + "', log: '" + _log + "'.\n");
			}
			else if (_action == Constants.COMPLAINT_ACTION_WARN)
			{
				// Send an email to the reported player
				String body_string = "Your War of Conquest account '" + reported_userData.name + "' has violated the game's chat rules.\n\n" +
					"Chat message: '" + complaintData.text + "'\n" +
					"Issue: " + complaintData.issue + "\n\n" +
					((_message.equals("")) ? "" : (_message + "\n\n")) +
					"This is only a warning, no action will be taken now.\n" +
					"Further violations may result in your accounts being banned from chat or from the game.\n\n" +
					"You can find a list of War of Conquest's chat and forum rules here:\n" +
					"https://warofconquest.com/forum/viewtopic.php?f=3&t=2\n\n" +
					"Thank you,\n" +
					"The War of Conquest team\n" +
					"\n";
				Constants.SendEmail("noreply@warofconquest.com", "War of Conquest", reported_userData.email, "Violation of War of Conquest's chat rules", body_string);

				// Record the warning in the player's account.
				reported_userData.UpdateComplaintAndBanCounts(0, 0, 1, 0, 0);

				Constants.WriteToLog("log_resolved_complaints.txt", Constants.GetTimestampString() + " Mod " + user_data.name + " (" + _userID + ") warned the reported player for complaint " + _complaintID + ", submitted by " + reporter_userData.name + " (" + complaintData.userID + ") against " + reported_userData.name + " (" + complaintData.reported_userID + ") for issue: '" + complaintData.issue + "', text: '" + complaintData.text + "', message: '" + _message + "', log: '" + _log + "'.\n");
			}
			else if (_action == Constants.COMPLAINT_ACTION_CHAT_BAN)
			{
				// Send an email to the reported player
				String body_string = "Your War of Conquest account '" + reported_userData.name + "' has violated the game's chat rules.\n\n" +
					"Chat message: '" + complaintData.text + "'\n" +
					"Issue: " + complaintData.issue + "\n\n" +
					(_message.equals("") ? "" : (_message + "\n\n")) +
					"Your account has been banned from chat for " + _ban_days + " days.\n\n" +
					"Further violations may result in additional bans, from chat or from the game.\n\n" +
					"You can find a list of War of Conquest's chat and forum rules here:\n" +
					"https://warofconquest.com/forum/viewtopic.php?f=3&t=2\n\n" +
					"Thank you,\n" +
					"The War of Conquest team\n" +
					"\n";
				Constants.SendEmail("noreply@warofconquest.com", "War of Conquest", reported_userData.email, "Violation of War of Conquest's chat rules", body_string);

				// Ban the reported player from chat
				Comm.RecordChatBan(reported_userData, _ban_days * Constants.SECONDS_PER_DAY, 1.0f);

				Constants.WriteToLog("log_resolved_complaints.txt", Constants.GetTimestampString() + " Mod " + user_data.name + " (" + _userID + ") banned the reported player from chat for " + _ban_days + " days for complaint " + _complaintID + ", submitted by " + reporter_userData.name + " (" + complaintData.userID + ") against " + reported_userData.name + " (" + complaintData.reported_userID + ") for issue: '" + complaintData.issue + "', text: '" + complaintData.text + "', message: '" + _message + "', log: '" + _log + "'.\n");
			}
			else if (_action == Constants.COMPLAINT_ACTION_GAME_BAN)
			{
				// Send an email to the reported player
				String body_string = "Your War of Conquest account '" + reported_userData.name + "' has violated the game's chat rules.\n\n" +
					"Chat message: '" + complaintData.text + "'\n" +
					"Issue: " + complaintData.issue + "\n\n" +
					(_message.equals("") ? "" : (_message + "\n\n")) +
					"Your account has been banned from the game for " + _ban_days + " days.\n\n" +
					"Further violations may result in additional bans, from chat or from the game.\n\n" +
					"You can find a list of War of Conquest's chat and forum rules here:\n" +
					"https://warofconquest.com/forum/viewtopic.php?f=3&t=2\n\n" +
					"Thank you,\n" +
					"The War of Conquest team\n" +
					"\n";
				Constants.SendEmail("noreply@warofconquest.com", "War of Conquest", reported_userData.email, "Violation of War of Conquest's chat rules", body_string);

				// Ban the reported user from the game.
				GameBanUser(reported_userData, _ban_days * 24);

				Constants.WriteToLog("log_resolved_complaints.txt", Constants.GetTimestampString() + " Mod " + user_data.name + " (" + _userID + ") banned the reported player from the game for " + _ban_days + " days for complaint " + _complaintID + ", submitted by " + reporter_userData.name + " (" + complaintData.userID + ") against " + reported_userData.name + " (" + complaintData.reported_userID + ") for issue: '" + complaintData.issue + "', text: '" + complaintData.text + "', message: '" + _message + "', log: '" + _log + "'.\n");
			}
			else if (_action == Constants.COMPLAINT_ACTION_WARN_FILER)
			{
				// Send an email to the reported player
				String body_string = "Your War of Conquest account '" + reported_userData.name + "' has violated the game's chat rules.\n\n" +
					"Your account submitted a report about the following:\n" +
					"Chat message: '" + complaintData.text + "'\n" +
					"Issue: " + complaintData.issue + "\n\n" +
					"This report was deemed to be inappropriate or spammy.\n" +
					"This is only a warning, no action will be taken now.\n" +
					"Further submissions of inappropriate or spammy chat reports may result in your accounts being banned from chat.\n\n" +
					(_message.equals("") ? "" : (_message + "\n\n")) +
					"You can find a list of War of Conquest's chat and forum rules here:\n" +
					"https://warofconquest.com/forum/viewtopic.php?f=3&t=2\n\n" +
					"Thank you,\n" +
					"The War of Conquest team\n" +
					"\n";
				Constants.SendEmail("noreply@warofconquest.com", "War of Conquest", reported_userData.email, "Violation of War of Conquest's chat rules", body_string);

				// Record the warning in the player's account.
				reporter_userData.UpdateComplaintAndBanCounts(0, 0, 1, 0, 0);

				Constants.WriteToLog("log_resolved_complaints.txt", Constants.GetTimestampString() + " Mod " + user_data.name + " (" + _userID + ") warned the reporting player for complaint " + _complaintID + ", submitted by " + reporter_userData.name + " (" + complaintData.userID + ") against " + reported_userData.name + " (" + complaintData.reported_userID + ") for issue: '" + complaintData.issue + "', text: '" + complaintData.text + "', message: '" + _message + "', log: '" + _log + "'.\n");
			}
			else if (_action == Constants.COMPLAINT_ACTION_CHAT_BAN_FILER)
			{
				// Send an email to the reported player
				String body_string = "Your War of Conquest account '" + reported_userData.name + "' has violated the game's chat rules.\n\n" +
					"Your account submitted a report about the following:\n" +
					"Chat message: '" + complaintData.text + "'\n" +
					"Issue: " + complaintData.issue + "\n\n" +
					"This report was deemed to be inappropriate or spammy.\n" +
					"Your account has been banned from chat for " + _ban_days + " days.\n" +
					"Further submissions of inappropriate or spammy chat reports may result in additional bans from chat.\n\n" +
					(_message.equals("") ? "" : (_message + "\n\n")) +
					"You can find a list of War of Conquest's chat and forum rules here:\n" +
					"https://warofconquest.com/forum/viewtopic.php?f=3&t=2\n\n" +
					"Thank you,\n" +
					"The War of Conquest team\n" +
					"\n";
				Constants.SendEmail("noreply@warofconquest.com", "War of Conquest", reported_userData.email, "Violation of War of Conquest's chat rules", body_string);

				// Ban the reporter player from chat
				Comm.RecordChatBan(reporter_userData, _ban_days * Constants.SECONDS_PER_DAY, 1.0f);

				Constants.WriteToLog("log_resolved_complaints.txt", Constants.GetTimestampString() + " Mod " + user_data.name + " (" + _userID + ") banned the reporting player from chat for " + _ban_days + " days for complaint " + _complaintID + ", submitted by " + reporter_userData.name + " (" + complaintData.userID + ") against " + reported_userData.name + " (" + complaintData.reported_userID + ") for issue: '" + complaintData.issue + "', text: '" + complaintData.text + "', message: '" + _message + "', log: '" + _log + "'.\n");
			}
		}

		// Delete the resolved complaint
		int complaint_index = DeleteComplaint(_complaintID);

		// Fetch the next complaint.
		OutputEvents.GetNextComplaintEvent(_output_buffer, _userID, complaint_index);
	}

	public static int DeleteComplaint(int _complaintID)
	{
		// Delete the complaint with the given ID
		DataManager.DeleteData(Constants.DT_COMPLAINT, _complaintID);

		// Remove the resolved complaint from the list of complaints.
		int complaint_index = GlobalData.instance.complaints.indexOf(_complaintID);
		if (complaint_index != -1) {
			GlobalData.instance.complaints.remove((int)complaint_index);
		}

		// Mark the GlobalData to be updated.
		DataManager.MarkForUpdate(GlobalData.instance);

		return complaint_index;
	}

	public static void ListComplaints()
	{
		int complaintID;
		ComplaintData complaintData;

		for (int i = 0; i < GlobalData.instance.complaints.size(); i++)
		{
			complaintID = GlobalData.instance.complaints.get(i);
			complaintData = (ComplaintData)DataManager.GetData(Constants.DT_COMPLAINT, complaintID, false);

			if (complaintData != null)
			{
				Output.PrintToScreen("Complaint " + i + ", ID " + complaintData.ID + ", userID: " + complaintData.userID + ", reported_userID: " + complaintData.reported_userID + ", issue: " + complaintData.issue);

				// If this complaint is corrupt, remove it.
				if (complaintData.userID <= 0)
				{
					// Delete this complaint and remove it from the list.
					DataManager.DeleteData(Constants.DT_COMPLAINT, complaintID);
					GlobalData.instance.complaints.remove((int)i);
					DataManager.MarkForUpdate(GlobalData.instance);
					Output.PrintToScreen("Deleted corrupt complaint with ID " + complaintID);

					// Decrement the index.
					i--;
				}
			}
		}
	}

	public static void ClearComplaints()
	{
		ComplaintData complaintData;

		for (int i = 0; i < GlobalData.instance.complaints.size(); i++)
		{
			complaintData = (ComplaintData)DataManager.GetData(Constants.DT_COMPLAINT, GlobalData.instance.complaints.get(i), false);

			if (complaintData != null)
			{
				Output.PrintToScreen("Complaint " + i + ", ID " + complaintData.ID + ", userID: " + complaintData.userID + ", reported_userID: " + complaintData.reported_userID + ", issue: " + complaintData.issue);
				DataManager.DeleteData(Constants.DT_COMPLAINT, complaintData.ID);
			}
		}

		// Clear the global list of complaints.
		GlobalData.instance.complaints.clear();
		DataManager.MarkForUpdate(GlobalData.instance);
	}

	public static String ClearDatabase()
	{
		// Clear the database.
		DataManager.ClearDatabase();

		// Return message
		return "Database cleared.";
	}

	public static String ClearPlayerDatabase()
	{
		// Clear the player database.
		AccountDB.DeleteAllRecords();

		// Return message
		return "Player database cleared.";
	}

	public static void ClearDeviceFealty(int _deviceID)
	{
    DeviceData device_data = (DeviceData)DataManager.GetData(Constants.DT_DEVICE, _deviceID, false);

    if (device_data == null)
    {
      Output.PrintToScreen("There is no device data for ID " + _deviceID + ".");
      return;
    }

		// Clear the device's fealty records.
		device_data.fealty0_nationID = -1;
		device_data.fealty1_nationID = -1;
		device_data.fealty2_nationID = -1;
		device_data.fealty0_prev_attack_time = -1;
		device_data.fealty1_prev_attack_time = -1;
		device_data.fealty2_prev_attack_time = -1;
		device_data.fealty_tournament_nationID = -1;
		device_data.fealty_tournament_start_day = -1;

		// Mark the device's data to be updated.
		DataManager.MarkForUpdate(device_data);
	}

	public static void ClearTemps(String _nation_name)
  {
		int expire_time;
		boolean expire_time_exists;
		TechData tech_data;

		// Get the nation's data
    int nationID = DataManager.GetNameToIDMap(Constants.DT_NATION, _nation_name);
  	NationData nation_data = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);
    if (nation_data == null)
		{
		  Output.PrintToScreen("Unknown nation: '" + _nation_name + "'");
			return;
		}

		// Get the nation's tech data
		NationTechData nation_tech_data = (NationTechData)DataManager.GetData(Constants.DT_NATIONTECH, nationID, false);

		HashMap<Integer, Integer> techs_to_remove = new HashMap<Integer, Integer>();

		// Compile a list of temp techs to remove
		for (Map.Entry<Integer, Integer> entry : nation_tech_data.tech_count.entrySet())
		{
			tech_data = TechData.GetTechData(entry.getKey());

			if (tech_data.duration_type == TechData.DURATION_TEMPORARY) {
				techs_to_remove.put(entry.getKey(), entry.getValue());
			}
		}

		// Remove the temp techs
		int removed_count = 0;
		for (Map.Entry<Integer, Integer> entry : techs_to_remove.entrySet())
		{
			for (int i = 0; i < entry.getValue(); i++)
			{
				Technology.RemoveTechnology(nationID, entry.getKey(), 0);
				removed_count++;
			}
		}

		// Clear the lost of expire times
		nation_tech_data.tech_temp_expire_time.clear();

		// Clear the nation's next tech expire time info.
		Technology.DetermineNextTechExpire(nationID, nation_data, nation_tech_data);

		// Mark the nation's data to be updated
		DataManager.MarkForUpdate(nation_data);
		DataManager.MarkForUpdate(nation_tech_data);

		// Output results
		Output.PrintToScreen(removed_count + " temp techs removed.");
}

	public static void DeleteNation(String nation_name)
  {
    int nationID = DataManager.GetNameToIDMap(Constants.DT_NATION, nation_name);
  	NationData nation_data = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);
    if (nation_data == null)
		{
		  Output.PrintToScreen("Unknown nation: '" + nation_name + "'");
		}
		else
		{
      // Write event to log
      Output.PrintToScreen("*** DELETE_NATION from server console. NATION: " + nation_name + " (ID: " + nationID + ")\n");
      Constants.WriteToLog("log_admin_deleted_nations.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " DELETE_NATION from server console. NATION: " + nation_name + " (ID: " + nationID + ")\n");

      // Remove the nation from the world and delete it.
      World.RemoveNation(nationID);
    }
  }

	public static void ClearDevice(int _deviceID)
  {
		int i;
    DeviceData device_data = (DeviceData)DataManager.GetData(Constants.DT_DEVICE, _deviceID, false);

    if (device_data == null)
    {
      Output.PrintToScreen("There is no device data for ID " + _deviceID + ".");
    }
		else
		{
			// Set the device independent record for this device to not consider it veteran.
			DeviceAccountData device_account_data = DeviceDB.ReadDeviceAccount(device_data.uid);
			if (device_account_data != null)
			{
				device_account_data.veteran = false;
				DeviceDB.WriteDeviceAccount(device_account_data);
			}

			// Clear the device's data. Do not actually delete the record, as that could cause problems when iterating lists of devices that include it.
			device_data.name = "";
			device_data.device_type = "";
			device_data.prev_IP = "";
			device_data.playerID = -1;
			device_data.game_ban_end_time = -1;
			device_data.chat_ban_end_time = -1;
			device_data.creation_time = 0;
			device_data.num_correlation_checks = 0;
			device_data.correlation_counts.clear();
			device_data.correlation_records.clear();

			device_data.fealty0_nationID = -1;
			device_data.fealty1_nationID = -1;
			device_data.fealty2_nationID = -1;
			device_data.fealty0_prev_attack_time = -1;
			device_data.fealty1_prev_attack_time = -1;
			device_data.fealty2_prev_attack_time = -1;
			device_data.fealty_tournament_nationID = -1;
			device_data.fealty_tournament_start_day = -1;

			for (i = 0; i < device_data.associated_devices.size(); i++)
			{
				DeviceData assoc_device_data = (DeviceData)DataManager.GetData(Constants.DT_DEVICE, device_data.associated_devices.get(i), false);

				if (assoc_device_data != null)
				{
					assoc_device_data.associated_devices.remove((Integer)device_data.ID);
					DataManager.MarkForUpdate(assoc_device_data);
				}
			}

			for (i = 0; i < device_data.users.size(); i++)
			{
				UserData user_data = (UserData)DataManager.GetData(Constants.DT_USER, device_data.users.get(i), false);

				if (user_data != null)
				{
					user_data.devices.remove((Integer)device_data.ID);
					DataManager.MarkForUpdate(user_data);
				}
			}

			device_data.associated_devices.clear();
			device_data.users.clear();

			// Update the device's record immediately.
			DataManager.UpdateImmediately(device_data);

			Output.PrintToScreen("Device record with ID " + _deviceID + " has been cleared.");
		}
	}

  public static void DeleteUser(String user_name)
  {
    int userID = UserData.GetUserIDByUsername(user_name);
		if (userID <= 0)
		{
			Output.PrintToScreen("Unknown user: '" + user_name + "', userID not found");
		}
		else
		{
			UserData user_data = (UserData)DataManager.GetData(Constants.DT_USER, userID, false);
			if (user_data == null)
			{
				Output.PrintToScreen("Unknown user: '" + user_name + "', data not found");
			}
			else if (user_data.nationID != -1)
			{
				Output.PrintToScreen("User '" + user_name + "' is a member of a nation and will not be deleted.");
			}
			else
			{
				// Delete the user's data
				DataManager.DeleteData(Constants.DT_USER, userID);

				Output.PrintToScreen("User '" + user_name + "' has been deleted.");
			}
		}
  }

	public static void DisassocDevice(int _deviceID)
  {
		DeviceData.DisassociateDeviceFromPlayer(_deviceID);
		Output.PrintToScreen("Device ID '" + _deviceID + "' playerID cleared.");
  }

	public static void Emergency(String _text)
	{
		Emergency(_text, true);
	}

	public static void Emergency(String _text, boolean _log)
	{
		if (Constants.GetTime() < (prev_emergency_time + EMERGENCY_PERIOD)) {
			return;
		}

		// Record time of this emergency
		prev_emergency_time = Constants.GetTime();

		// Send emergency notification e-mail
		Constants.SendEmail("contact@warofconquest.com", "War of Conquest Server " + Constants.server_id, "emergency@warofconquest.com", "War of Conquest Server " + Constants.server_id + " Emergency!", Constants.GetFullDate() + " War of Conquest Server " + Constants.server_id + " Emergency! " + _text);
		//Constants.SendEmail("contact@warofconquest.com", "War of Conquest Server " + Constants.server_id, "4013682917@txt.att.net", "War of Conquest Server " + Constants.server_id + " Emergency!", Constants.GetFullDate() + " War of Conquest Server " + Constants.server_id + " Emergency! " + _text);
		//Constants.SendEmail("contact@warofconquest.com", "War of Conquest Server " + Constants.server_id, "5084155210@txt.att.net", "War of Conquest Server " + Constants.server_id + " Emergency!", Constants.GetFullDate() + " War of Conquest Server " + Constants.server_id + " Emergency! " + _text);

		if (_log) {
			Output.PrintToScreen("EMERGENCY NOTIFICATION SENT");
		}
	}

	public static void GetStatusEvent(StringBuffer _output_buffer, int _userID)
	{
		String statusText =
		"Number of clients attached: " + WOCServer.client_table.size() + "\n" +
		"Number of nations logged in: " + WOCServer.nation_table.size() + "\n" +
		"Total revenue to date: " + GlobalData.instance.money_revenue + "\n" +
		"Total awarded as prizes: " + GlobalData.instance.game_money_awarded;

		// Encode event ID
		Constants.EncodeString(_output_buffer, "event_admin_status");

		// Add status string
		Constants.EncodeString(_output_buffer, statusText);
	}

	public static void GenerateLandscape(int _width, int _height, int _seed, int _max_border_width)
	{
		int i, x, y, cur_obj_count;
		double val, dif, prob;
		SimplexNoise simplexNoise;
		ObjectData cur_obj_data;
		Integer cur_objectID;
		float rand, probability;
		Iterator<Integer> keySetIterator;
		int num_rand_objects_placed = 0;

		double MOUNTAIN_PROB_CENTER = 0.515;
		double MOUNTAIN_PROB_RANGE = 0.035;
		double MOUNTAIN_PEAK_PROB = 0.5;
		double MAJOR_MOUNTAIN_PROB_CENTER = 1;
		double MAJOR_MOUNTAIN_PROB_RANGE = 0.1; // 0.2;
		double MAJOR_MOUNTAIN_PEAK_PROB = 1;
		double RESOURCE_DENSITY = 0.007;// Was 0.003, then 0.0044, before increasing frequency of some resources.

		double[] depth_data_1 = new double[_width * _height];
		double[] depth_data_2 = new double[_width * _height];
		double[] depth_data_3 = new double[_width * _height];

		XXHash mountain_xxhash = new XXHash(_seed);
		XXHash resource_placement_xxhash = new XXHash(_seed + 5771); // old 8769
		XXHash resource_type_xxhash = new XXHash(_seed + 7548);

		// If no _max_border_width given, use default value.
		if (_max_border_width <= 0) {
			_max_border_width = 200;
		}

		// Change smallestFeature and largestFeature to affect character of landscape.
		// Increase persistence to make landscape more irregular.

		// Landmass noise
		simplexNoise = new SimplexNoise(16, 512/*256*/,0.7,_seed);
		FillArrayWithSimplexNoise(_width, _height, depth_data_1, simplexNoise);

		// Simplified landmass noise
		simplexNoise = new SimplexNoise(64, 512/*256*/,0.7,_seed);
		FillArrayWithSimplexNoise(_width, _height, depth_data_2, simplexNoise);

		// Noise determining transition between landscape noise and simplified landscape noise
		simplexNoise = new SimplexNoise(64, 512/*256*/,0.7,_seed+1);
		FillArrayWithSimplexNoise(_width, _height, depth_data_3, simplexNoise);

		// Mix together the landscape data with the simplified landscape data, the mix determined by the transition noise.
		for (y = 0; y < _height; y++)
		{
			for (x = 0; x < _width; x++)
			{
				val = depth_data_3[y * _width + x] * 0.6;
				depth_data_1[y * _width + x] = (depth_data_1[y * _width + x] * (1 - val)) + (depth_data_2[y * _width + x] * val);
			}
		}

		// Subtract depression around borders
		SubtractBorderDepression(_width, _height, _max_border_width, depth_data_1);

		int[] palette = new int[128];

		// Grayscale palette for testing
		for (i = 0; i < 128; i++) {
			palette[i] = (i << 17) | (i << 9) | (i << 1);
		}

		// Hardcoded colors
		palette[COLOR_INDEX_LAND]     = 0x507900;
		palette[COLOR_INDEX_WATER]    = 0x9ba8eb;
		palette[COLOR_INDEX_MOUNTAIN] = 0xb49257;

		palette[COLOR_INDEX_ORB_BASE + 0] = 0xff99cc;
		palette[COLOR_INDEX_ORB_BASE + 1] = 0xff66cc;
		palette[COLOR_INDEX_ORB_BASE + 2] = 0xff33cc;
		palette[COLOR_INDEX_ORB_BASE + 3] = 0xcc0099;
		palette[COLOR_INDEX_ORB_BASE + 4] = 0x993399;

		palette[COLOR_INDEX_RESOURCE_BASE + 0] = 0xD98880;
		palette[COLOR_INDEX_RESOURCE_BASE + 1] = 0x7FB3D5;
		palette[COLOR_INDEX_RESOURCE_BASE + 2] = 0x85C1E9;
		palette[COLOR_INDEX_RESOURCE_BASE + 3] = 0xF7DC6F;
		palette[COLOR_INDEX_RESOURCE_BASE + 4] = 0xF8C471;
		palette[COLOR_INDEX_RESOURCE_BASE + 5] = 0xF0B27A;
		palette[COLOR_INDEX_RESOURCE_BASE + 6] = 0xE59866;
		palette[COLOR_INDEX_RESOURCE_BASE + 7] = 0x922B21;
		palette[COLOR_INDEX_RESOURCE_BASE + 8] = 0x1F618D;
		palette[COLOR_INDEX_RESOURCE_BASE + 9] = 0x2874A6;
		palette[COLOR_INDEX_RESOURCE_BASE + 10] = 0x148F77;
		palette[COLOR_INDEX_RESOURCE_BASE + 11] = 0x117A65;
		palette[COLOR_INDEX_RESOURCE_BASE + 12] = 0x1E8449;
		palette[COLOR_INDEX_RESOURCE_BASE + 13] = 0xECF0F1;
		palette[COLOR_INDEX_RESOURCE_BASE + 14] = 0xBDC3C7;
		palette[COLOR_INDEX_RESOURCE_BASE + 15] = 0x95A5A6;
		palette[COLOR_INDEX_RESOURCE_BASE + 16] = 0xff5050;
		palette[COLOR_INDEX_RESOURCE_BASE + 17] = 0xff6d50;
		palette[COLOR_INDEX_RESOURCE_BASE + 18] = 0xff8a50;
		palette[COLOR_INDEX_RESOURCE_BASE + 19] = 0xffa850;
		palette[COLOR_INDEX_RESOURCE_BASE + 20] = 0xffc550;

		//palette[8] = 0x7b006b;
		//palette[9] = 0xc100a8;
		//palette[10] = 0xff5fea;
		//palette[11] = 0xffa3f3;

		ArrayList<Integer> obj_prob_IDs = new ArrayList<Integer>();
		ArrayList<Float> obj_prob_cutoffs = new ArrayList<Float>();
		ArrayList<Float> obj_prob_probabilities = new ArrayList<Float>();

		// Create an array to record how many of each resource object was placed
		HashMap<Integer, Integer> obj_counts = new HashMap<Integer, Integer>();

		IndexColorModel colorModel = new IndexColorModel(8, palette.length, palette, 0, false, -1, DataBuffer.TYPE_BYTE);

		BufferedImage ls_image = new BufferedImage(_width, _height, BufferedImage.TYPE_BYTE_INDEXED, colorModel);

		byte[] image_data = new byte[_width * _height];

		// Determine what is land and what is water.
		for (y = 0; y < _height; y++)
		{
			for (x = 0; x < _width; x++)
			{
				image_data[y * _width + x] = (depth_data_1[y * _width + x] > 0.2) ? COLOR_INDEX_LAND : COLOR_INDEX_WATER;
				//if ((val > 0.6) && (val < 0.65)) image_data[y * _width + x] = COLOR_INDEX_MOUNTAIN;
				//if ((val > 0.48) && (val < 0.55)) image_data[y * _width + x] = COLOR_INDEX_MOUNTAIN;
			}
		}

		// Mountain range noise
		// Persistence determines the thickness of the bands of mountains.
		// largestFeature seems to make no difference.
		// smallestFeature determines the size of the open spaces between bands of mountains and the amount of complex nuanced detail to the bands of mountains.
		simplexNoise = new SimplexNoise(16, 128/*64*/,0.4,_seed);
		FillArrayWithSimplexNoise(_width, _height, depth_data_2, simplexNoise);

		// Add in small scale mountain ranges.
		for (y = 0; y < _height; y++)
		{
			for (x = 0; x < _width; x++)
			{
				if (image_data[y * _width + x] != COLOR_INDEX_LAND) {
					continue;
				}

				val = depth_data_2[y * _width + x];
				dif = Math.abs(val - MOUNTAIN_PROB_CENTER);

				if (dif <=  MOUNTAIN_PROB_RANGE)
				{
					prob = (1 - (dif / MOUNTAIN_PROB_RANGE)) * MOUNTAIN_PEAK_PROB;
					if (mountain_xxhash.GetHashFloat(x, y) <= prob) {
						image_data[y * _width + x] = COLOR_INDEX_MOUNTAIN;
					}
				}

				//if ((val > 0.48) && (val < 0.55)) image_data[y * _width + x] = COLOR_INDEX_MOUNTAIN;
			}
		}

		// Major mountain range noise
		// Persistence determines the thickness of the bands of mountains.
		// largestFeature determines the size of the open spaces between bands of mountains.
		// smallestFeature determines the amount of complex nuanced detail to the bands of mountains.
		simplexNoise = new SimplexNoise(32, 256/*128*/,0.75,_seed);
		FillArrayWithSimplexNoise(_width, _height, depth_data_2, simplexNoise);

		// Subtract depression around borders from major mountain ranges
		SubtractBorderDepression(_width, _height, _max_border_width, depth_data_2);

		// Add in large scale mountain ranges.
		for (y = 0; y < _height; y++)
		{
			for (x = 0; x < _width; x++)
			{
				if (image_data[y * _width + x] != COLOR_INDEX_LAND) {
					continue;
				}

				val = depth_data_2[y * _width + x];
				dif = Math.abs(val - MAJOR_MOUNTAIN_PROB_CENTER);

				if (dif <=  MAJOR_MOUNTAIN_PROB_RANGE)
				{
					prob = (1 - (dif / MAJOR_MOUNTAIN_PROB_RANGE)) * MAJOR_MOUNTAIN_PEAK_PROB;
					if (mountain_xxhash.GetHashFloat(x, y) <= prob) {
						image_data[y * _width + x] = COLOR_INDEX_MOUNTAIN;
					}
				}

//				if ((val > 0.8) && (val < 1)) image_data[y * _width + x] = COLOR_INDEX_MOUNTAIN;
			}
		}

		// Place resource objects
		for (x = 0; x < _width; x++)
		{
			// Determine the current horizontal position in the map, in range 0->1.
			float cur_horiz_position = (float)x / (float)(_width - 1);

			// Determine the sum of the frequencies of all resource landscape objects at this horizontal position in the map.
			float frequency_sum_value = 0.0f;
			keySetIterator = ObjectData.objects.keySet().iterator();
			while (keySetIterator.hasNext())
			{
				cur_objectID = keySetIterator.next();

				// Only look at resource objects; skip orbs.
				if (cur_objectID >= ObjectData.ORB_BASE_ID) {
					continue;
				}

				// Get the current object's data
				cur_obj_data = ObjectData.GetObjectData(cur_objectID);

				if (cur_obj_data == null) {
					continue;
				}

				// Skip resource objects whose range does not include the current horizontal position in the map.
				if ((cur_horiz_position < cur_obj_data.range_min) || (cur_horiz_position > cur_obj_data.range_max)) {
					continue;
				}

				frequency_sum_value += cur_obj_data.frequency;
			}

			// Fill arrays of probabilities for resource object placement, for the current horizontal position in the map.
			obj_prob_IDs.clear();
			obj_prob_cutoffs.clear();
			obj_prob_probabilities.clear();
			float cutoff = 0, cur_cutoff;
			keySetIterator = ObjectData.objects.keySet().iterator();
			while (keySetIterator.hasNext())
			{
				cur_objectID = keySetIterator.next();

				// Only look at resource objects; skip orbs.
				if (cur_objectID >= ObjectData.ORB_BASE_ID) {
					continue;
				}

				cur_obj_data = ObjectData.GetObjectData(cur_objectID);
				if (cur_obj_data == null) {
					continue;
				}

				// Skip resource objects whose range does not include the current horizontal position in the map.
				if ((cur_horiz_position < cur_obj_data.range_min) || (cur_horiz_position > cur_obj_data.range_max)) {
					continue;
				}

				// Determine the current resource object's probability at this horizontal position.
				probability = (((float)(cur_obj_data.frequency)) / frequency_sum_value);

				if (probability > 0.0f)
				{
					cutoff += probability;
					obj_prob_IDs.add(cur_objectID);
					obj_prob_probabilities.add(probability);
					obj_prob_cutoffs.add(cutoff);
				}
			}

			for (y = 0; y < _height; y++)
			{
				// Determine whether a resource object should be placed here...
				if ((image_data[y * _width + x] == COLOR_INDEX_LAND) && (resource_placement_xxhash.GetHashFloat(x, y) <= RESOURCE_DENSITY))
				{
					// Determine which resource object should be placed here.

					// Generate position-based hash pseudo-random number to determine which object to place.
					rand = resource_type_xxhash.GetHashFloat(x, y);

					// Determine what object to place based on probabilities
					for (i = 0; i < obj_prob_IDs.size(); i++)
					{
						// Determine the current random object's cutoff
						cur_cutoff = obj_prob_cutoffs.get(i);

						if (cur_cutoff >= rand)
						{
							// Get the current object's ID
							cur_objectID = obj_prob_IDs.get(i);

							//// TESTING
							//if (cur_objectID == 1016) Output.PrintToScreen("Alchemist's Lair at " + x + "," + y);

							// Set the current map position's pixel to this resource object's color index.
							image_data[y * _width + x] = (byte)(COLOR_INDEX_RESOURCE_BASE + (cur_objectID - ObjectData.RESOURCE_OBJECT_BASE_ID));

							// Record placement of this object in the obj_counts map
							obj_counts.put(cur_objectID, ((obj_counts.get(cur_objectID) == null) ? 0 : obj_counts.get(cur_objectID)) + 1);
							num_rand_objects_placed++;

							break;
						}
					}
				}
			}
		}

		// Output list of resource object counts
		if (num_rand_objects_placed > 0)
		{
			for (i = 0; i < obj_prob_IDs.size(); i++)
			{
				// Output line for this object
				cur_objectID = obj_prob_IDs.get(i);
				cur_obj_data = ObjectData.GetObjectData(cur_objectID);
				cur_obj_count = obj_counts.containsKey(cur_objectID) ? obj_counts.get(cur_objectID) : 0;
				probability = obj_prob_probabilities.get(i);
				Output.PrintToScreen(cur_obj_data.name + " (prob: " +	(probability * 100) + "%, actual: " + ((float)cur_obj_count * 100.0f / (float)num_rand_objects_placed) +	"%, count: " + cur_obj_count + ")");
			}
		}

		ls_image.getRaster().setDataElements(0, 0, _width, _height, image_data);

		// Output the landscape image.
		try{
			ImageIO.write(ls_image, "PNG", new File("landgen_" + _width + "_" + _height + "_" + _seed + ".png"));
		} catch (IOException ie) {
      Output.PrintException(ie);
    }
	}

	public static String GenerateMap(String _filename)
	{
		// http://albert.rierol.net/imagej_programming_tutorials.html#ImageJ
		// https://imagej.nih.gov/ij/download.html
		// https://imagej.nih.gov/ij/developer/api/index.html
		// https://imagej.nih.gov/ij/docs/guide/146.html

		Output.PrintToScreen("Generating UI map...");

		int MAP_WIDTH = 4096;
		int MAP_MARGIN = 160;
		Roi roi, land_roi, water_roi;
		Opener opener = new Opener();
		int x, y, x1, y1;
		float angle, pos;
		ImageUtils.Vector2 origin;
		ImageUtils.Vector2 v0, v1, v2;

		// Make sure the given file exists.
		if (Constants.FileExists(_filename) == false) {
			return "File '" + _filename + "' does not exist.";
		}

		ImagePlus imp = IJ.openImage(_filename);
		imp.show();

		ImageProcessor ip = imp.getProcessor();

		// Make an RGB copy of the image
		ImagePlus imp_rgb = IJ.createImage("Map", "RGB white", imp.getWidth(), imp.getHeight(), 1);
		ImageProcessor ip_rgb = imp_rgb.getProcessor();
		ip_rgb.insert(ip, 0, 0);

		imp_rgb.show();

		// Make a copy of the original image, for determining where to place mountains
		ImagePlus imp_rgb_mtns = IJ.createImage("Mountains", "RGB white", imp_rgb.getWidth(), imp_rgb.getHeight(), 1);
		ImageProcessor ip_rgb_mtns = imp_rgb_mtns.getProcessor();
		ip_rgb_mtns.insert(ip_rgb, 0, 0);

		// Select the mountains
		ImageUtils.ThresholdSelection(imp_rgb_mtns, true, 180, 180, 146, 146, 87, 87);

		// Fill the mountains with white
		roi = imp_rgb_mtns.getRoi();
		ip_rgb_mtns.setColor(new Color(255,255,255));
		ip_rgb_mtns.fill(roi.getMask());

		// Select everything but the mountains
		ImageUtils.ThresholdSelection(imp_rgb_mtns, true, 0, 254, 0, 254, 0, 254);

		// Fill the non-mountains with black
		roi = imp_rgb_mtns.getRoi();
		ip_rgb_mtns.setColor(new Color(0,0,0));
		ip_rgb_mtns.fill(roi.getMask());

		// Deselect
		imp_rgb_mtns.deleteRoi();

		// Blur the mountains
		IJ.run(imp_rgb_mtns, "Gaussian Blur...", "sigma=2");

		//imp_rgb_mtns.show();

		// Scale the map image
		int new_width = 4096;
		int new_height = (int)(new_width * ip_rgb.getHeight() / ip_rgb.getWidth());
		ImageUtils.ScaleImage(imp_rgb, new_width, new_height);
		ip_rgb = imp_rgb.getProcessor(); // Get the image's new ImageProcessor

		int max_dim = Math.max(imp_rgb.getWidth(), imp_rgb.getHeight());
		float scale = (float)max_dim;
		float xscale = imp_rgb.getWidth();
		float yscale = imp_rgb.getHeight();

		// Create land and water selections
		ImageUtils.ThresholdSelection(imp_rgb, true, 149, 160, 164, 172, 211, 244);
		water_roi = imp_rgb.getRoi();
		ImageUtils.ThresholdSelection(imp_rgb, false, 149, 160, 164, 172, 211, 244);
		land_roi = imp_rgb.getRoi();

		// Fill the whole image with white
		imp_rgb.deleteRoi();
		ip_rgb.setColor(new Color(255,255,255));
		ip_rgb.fill();

		// Insert mountains

		// Load mountain images
		ImagePlus imp_mtn[] = new ImagePlus[4];
		imp_mtn[0] = opener.openImage("./images/mtn1.png");
		imp_mtn[1] = opener.openImage("./images/mtn2.png");
		imp_mtn[2] = opener.openImage("./images/mtn3.png");
		imp_mtn[3] = opener.openImage("./images/mtn4.png");

		IJ.setPasteMode("Transparent-white");

		int cur_val, fx, fy;
		ImagePlus cur_imp_mtn;
		Random rand = new Random(123456);
		for (y = 0; y < imp_rgb.getHeight(); y += 14)
		{
			for (x = 0; x < imp_rgb.getWidth(); x += 30)
			{
				fx = x + rand.nextInt(13) - 6;
				fy = y + rand.nextInt(9) - 4;
				x1 = fx * imp_rgb_mtns.getWidth() / imp_rgb.getWidth();
				y1 = fy * imp_rgb_mtns.getHeight() / imp_rgb.getHeight();
				cur_val = ip_rgb_mtns.getPixel(x1, y1);

				if ((cur_val & 0x000000ff) > 24)
				{
					// Paste mountain image here.
					cur_imp_mtn = imp_mtn[rand.nextInt(4)]; // Choose random mountain image
					IJ.run(cur_imp_mtn, "Select All", "");
					IJ.run(cur_imp_mtn, "Copy", "");
					imp_rgb.setRoi(fx - (cur_imp_mtn.getWidth() / 2), fy - (cur_imp_mtn.getHeight() / 2), cur_imp_mtn.getWidth(), cur_imp_mtn.getHeight());
					IJ.run(imp_rgb, "Paste", "");
				}
			}
		}

		// Fill the water areas with white, to erase any mountains that have been drawn there.
		imp_rgb.setRoi(water_roi);
		ip_rgb.setColor(new Color(255,255,255));
		ip_rgb.fill(imp_rgb.getMask());

		// Select the land
		imp_rgb.setRoi(land_roi);

		// Draw border of land
		IJ.setForegroundColor(91, 50, 16);
		IJ.run(imp, "Line Width...", "line=6");
		IJ.run(imp_rgb, "Draw", "slice");

		// Draw depth lines in water
		IJ.run(imp, "Line Width...", "line=2");
		IJ.run(imp_rgb, "Enlarge...", "enlarge=10");
		IJ.run(imp_rgb, "Draw", "slice");
		IJ.run(imp_rgb, "Enlarge...", "enlarge=12");
		IJ.run(imp_rgb, "Draw", "slice");
		IJ.run(imp_rgb, "Enlarge...", "enlarge=14");
		IJ.run(imp_rgb, "Draw", "slice");

		// Draw radial marker around Orb of Fire

		ImageUtils.Vector2 orb_fire_loc = ImageUtils.FindColorIndex(imp, ObjectData.GetNameToIDMap("Orb of Fire") - ObjectData.ORB_BASE_ID + COLOR_INDEX_ORB_BASE, 0);

		if (orb_fire_loc != null)
		{
			orb_fire_loc = ImageUtils.Vector2.Multiply(orb_fire_loc, (float)imp_rgb.getWidth() / (float)imp.getWidth());
			Output.PrintToScreen("Fire orb loc: " + orb_fire_loc.x + "," + orb_fire_loc.y);

			float circle_radius = 0.02f * scale;

			origin = orb_fire_loc;

			ImageUtils.SelectCircle(imp_rgb, origin, circle_radius);

			// Clear inside of circle
			ip_rgb.setColor(new Color(255,255,255));
			ip_rgb.fill(imp_rgb.getMask());

			IJ.setForegroundColor(91, 50, 16);
			IJ.run(imp, "Line Width...", "line=5");

			IJ.run(imp_rgb, "Draw", "slice");

			ImageUtils.SelectCircle(imp_rgb, origin, circle_radius * 0.9f);
			IJ.run(imp, "Line Width...", "line=3");
			IJ.run(imp_rgb, "Draw", "slice");

			ImageUtils.SelectCircle(imp_rgb, origin, circle_radius * 0.7f);
			IJ.run(imp_rgb, "Draw", "slice");

			for (angle = 0; angle < 360; angle += 6)
			{
				v0 = ImageUtils.Vector2.AngleToVector(angle, circle_radius * 0.7f);
				v1 = ImageUtils.Vector2.AngleToVector(angle, circle_radius * 0.9f);
				//Output.PrintToScreen("line " + v0.x + "," + v0.y + " to " + v1.x + "," + v1.y);
				ImageUtils.SelectLine(imp_rgb, ImageUtils.Vector2.Add(origin, v0), ImageUtils.Vector2.Add(origin, v1));
				IJ.run(imp_rgb, "Draw", "slice");
			}

			IJ.run(imp, "Line Width...", "line=5");
			for (angle = 0; angle < 360; angle += 22.5)
			{
				v0 = ImageUtils.Vector2.AngleToVector(angle - 5, circle_radius);
				v1 = ImageUtils.Vector2.AngleToVector(angle, circle_radius * (((((int)angle) % 90) == 0) ? 2.5f : ((((int)angle) % 45) == 0) ? 2.0f : 1.5f));
				v2 = ImageUtils.Vector2.AngleToVector(angle + 5, circle_radius);
				ImageUtils.SelectTriangle(imp_rgb, ImageUtils.Vector2.Add(origin, v0), ImageUtils.Vector2.Add(origin, v1), ImageUtils.Vector2.Add(origin, v2));

				// Fill the area of the triangle with white, clearing it.
				ip_rgb.setColor(new Color(255,255,255));
				ip_rgb.fill(imp_rgb.getMask());

				// Draw the triangle
				IJ.run(imp_rgb, "Draw", "slice");
			}
		}

		// Draw center marker

		origin = new ImageUtils.Vector2(xscale / 2, yscale / 2);
		float star_radius = 0.04f * scale;

		IJ.run(imp, "Line Width...", "line=4");

		// Orthogonal rays
		for (angle = 0; angle < 360; angle += 90)
		{
			// Outer angle
			v0 = ImageUtils.Vector2.AngleToVector(angle - 45, star_radius * 0.1f);
			v1 = ImageUtils.Vector2.AngleToVector(angle, star_radius * (((angle % 180) == 0) ? 0.5f : 1f));
			v2 = ImageUtils.Vector2.AngleToVector(angle + 45, star_radius * 0.1f);
			// Clear triangle
			ImageUtils.SelectTriangle(imp_rgb, ImageUtils.Vector2.Add(origin, v0), ImageUtils.Vector2.Add(origin, v1), ImageUtils.Vector2.Add(origin, v2));
			ip_rgb.setColor(new Color(255,255,255));
			ip_rgb.fill(imp_rgb.getMask());
			// Draw outer angle
			IJ.setForegroundColor(91, 50, 16);
			ImageUtils.SelectLine(imp_rgb, ImageUtils.Vector2.Add(origin, v0), ImageUtils.Vector2.Add(origin, v1));
			IJ.run(imp_rgb, "Draw", "slice");
			ImageUtils.SelectLine(imp_rgb, ImageUtils.Vector2.Add(origin, v1), ImageUtils.Vector2.Add(origin, v2));
			IJ.run(imp_rgb, "Draw", "slice");
			// Inner angle
			v0 = ImageUtils.Vector2.AngleToVector(angle - 45, star_radius * 0.1f);
			v1 = ImageUtils.Vector2.AngleToVector(angle, star_radius * 0.2f);
			v2 = ImageUtils.Vector2.AngleToVector(angle + 45, star_radius * 0.1f);
			ImageUtils.SelectLine(imp_rgb, ImageUtils.Vector2.Add(origin, v0), ImageUtils.Vector2.Add(origin, v1));
			IJ.run(imp_rgb, "Draw", "slice");
			ImageUtils.SelectLine(imp_rgb, ImageUtils.Vector2.Add(origin, v1), ImageUtils.Vector2.Add(origin, v2));
			IJ.run(imp_rgb, "Draw", "slice");
			// Connecting line
			v0 = ImageUtils.Vector2.AngleToVector(angle, star_radius * 0.2f);
			v1 = ImageUtils.Vector2.AngleToVector(angle, star_radius * (((angle % 180) == 0) ? 0.5f : 1f));
			ImageUtils.SelectLine(imp_rgb, ImageUtils.Vector2.Add(origin, v0), ImageUtils.Vector2.Add(origin, v1));
			IJ.run(imp_rgb, "Draw", "slice");
		}

		// Diagonal rays
		for (angle = 45; angle < 360; angle += 90)
		{
			v0 = ImageUtils.Vector2.AngleToVector(angle - 20, star_radius * 0.12f);
			v1 = ImageUtils.Vector2.AngleToVector(angle, star_radius * 0.35f);
			v2 = ImageUtils.Vector2.AngleToVector(angle + 20, star_radius * 0.12f);
			// Clear triangle
			ImageUtils.SelectTriangle(imp_rgb, ImageUtils.Vector2.Add(origin, v0), ImageUtils.Vector2.Add(origin, v1), ImageUtils.Vector2.Add(origin, v2));
			ip_rgb.setColor(new Color(255,255,255));
			ip_rgb.fill(imp_rgb.getMask());
			// Draw angle
			IJ.setForegroundColor(91, 50, 16);
			ImageUtils.SelectLine(imp_rgb, ImageUtils.Vector2.Add(origin, v0), ImageUtils.Vector2.Add(origin, v1));
			IJ.run(imp_rgb, "Draw", "slice");
			ImageUtils.SelectLine(imp_rgb, ImageUtils.Vector2.Add(origin, v1), ImageUtils.Vector2.Add(origin, v2));
			IJ.run(imp_rgb, "Draw", "slice");
		}

		// Center circle
		ImageUtils.SelectCircle(imp_rgb, origin, star_radius * 0.09f);
		ip_rgb.setColor(new Color(255,255,255));
		ip_rgb.fill(imp_rgb.getMask());
		IJ.run(imp_rgb, "Draw", "slice");

		// Draw markers for several instances of various objects
		DrawSeveralMapMarkers(imp, imp_rgb, ObjectData.GetNameToIDMap("Cryptid Colony") - ObjectData.RESOURCE_OBJECT_BASE_ID + COLOR_INDEX_RESOURCE_BASE, 6, "map_icon_cryptid.png");
		DrawSeveralMapMarkers(imp, imp_rgb, ObjectData.GetNameToIDMap("Ancient Starship Wreckage") - ObjectData.RESOURCE_OBJECT_BASE_ID + COLOR_INDEX_RESOURCE_BASE, 4, "map_icon_starship.png");
		DrawSeveralMapMarkers(imp, imp_rgb, ObjectData.GetNameToIDMap("Grave of an Ancient God") - ObjectData.RESOURCE_OBJECT_BASE_ID + COLOR_INDEX_RESOURCE_BASE, 4, "map_icon_grave.png");
		DrawSeveralMapMarkers(imp, imp_rgb, ObjectData.GetNameToIDMap("Henge") - ObjectData.RESOURCE_OBJECT_BASE_ID + COLOR_INDEX_RESOURCE_BASE, 8, "map_icon_henge.png");
		DrawSeveralMapMarkers(imp, imp_rgb, ObjectData.GetNameToIDMap("Quartz Deposit") - ObjectData.RESOURCE_OBJECT_BASE_ID + COLOR_INDEX_RESOURCE_BASE, 10, "map_icon_quartz.png");
		DrawSeveralMapMarkers(imp, imp_rgb, ObjectData.GetNameToIDMap("Oracle") - ObjectData.RESOURCE_OBJECT_BASE_ID + COLOR_INDEX_RESOURCE_BASE, 6, "map_icon_oracle.png");
		DrawSeveralMapMarkers(imp, imp_rgb, ObjectData.GetNameToIDMap("Orb of Noontide") - ObjectData.ORB_BASE_ID + COLOR_INDEX_ORB_BASE, 10, "map_icon_noontide.png");

		// Clear the selection.
		imp_rgb.deleteRoi();

		// Draw graphics that are just to be overlaid on the water, not land.

		// Make a new image for the graphics that are to appear only over the water areas.
		ImagePlus imp_rgb_water = IJ.createImage("Water overlay", "RGB white", imp_rgb.getWidth(), imp_rgb.getHeight(), 1);
		ImageProcessor ip_rgb_water = imp_rgb_water.getProcessor();

		// Fill the water image with white
		ip_rgb_water.setColor(new Color(255,255,255));
		ip_rgb_water.fill();

		// Draw blurred outline of land
		imp_rgb_water.setRoi(land_roi);
		IJ.setForegroundColor(136, 75, 16);
		IJ.run(imp_rgb_water, "Line Width...", "line=15");
		IJ.run(imp_rgb_water, "Draw", "slice");
		imp_rgb_water.deleteRoi();
		IJ.run(imp_rgb_water, "Gaussian Blur...", "sigma=40");

		if (orb_fire_loc != null)
		{
			// Draw lines radiating across water from Orb of Fire
			//IJ.setForegroundColor(240, 220, 200);
			IJ.setForegroundColor(200, 180, 160);
			IJ.run(imp_rgb_water, "Line Width...", "line=4");
			for (angle = 0; angle < 360; angle += 6)
			{
				v0 = ImageUtils.Vector2.AngleToVector(angle, 1);
				v1 = ImageUtils.Vector2.AngleToVector(angle, imp_rgb_water.getWidth() * 2);
				//Output.PrintToScreen("line " + v0.x + "," + v0.y + " to " + v1.x + "," + v1.y);
				ImageUtils.SelectLine(imp_rgb_water, ImageUtils.Vector2.Add(orb_fire_loc, v0), ImageUtils.Vector2.Add(orb_fire_loc, v1));
				IJ.run(imp_rgb_water, "Draw", "slice");
			}
		}

		// Draw border
		//IJ.setForegroundColor(91, 50, 16);
		IJ.setForegroundColor(141, 100, 66);
		IJ.run(imp, "Line Width...", "line=5");

		float border_thickness = 0.004f * xscale;

		// Top borders
		ImageUtils.SelectLine(imp_rgb_water, new ImageUtils.Vector2(0f * xscale, 0f * yscale), new ImageUtils.Vector2(1f * xscale, 0f * yscale));
		IJ.run(imp_rgb_water, "Draw", "slice");
		ImageUtils.SelectLine(imp_rgb_water, new ImageUtils.Vector2(border_thickness, border_thickness), new ImageUtils.Vector2(1f * xscale - border_thickness, border_thickness));
		IJ.run(imp_rgb_water, "Draw", "slice");

		// Bottom borders
		ImageUtils.SelectLine(imp_rgb_water, new ImageUtils.Vector2(0f * xscale, 1f * yscale), new ImageUtils.Vector2(1f * xscale, 1f * yscale));
		IJ.run(imp_rgb_water, "Draw", "slice");
		ImageUtils.SelectLine(imp_rgb_water, new ImageUtils.Vector2(border_thickness, 1f * yscale - border_thickness), new ImageUtils.Vector2(1f * xscale - border_thickness, 1f * yscale - border_thickness));
		IJ.run(imp_rgb_water, "Draw", "slice");

		// Left borders
		ImageUtils.SelectLine(imp_rgb_water, new ImageUtils.Vector2(0f * xscale, 0f * yscale), new ImageUtils.Vector2(0f * xscale, 1f * yscale));
		IJ.run(imp_rgb_water, "Draw", "slice");
		ImageUtils.SelectLine(imp_rgb_water, new ImageUtils.Vector2(border_thickness, border_thickness), new ImageUtils.Vector2(border_thickness, 1f * yscale - border_thickness));
		IJ.run(imp_rgb_water, "Draw", "slice");

		// Right borders
		ImageUtils.SelectLine(imp_rgb_water, new ImageUtils.Vector2(1f * xscale, 0f * yscale), new ImageUtils.Vector2(1f * xscale, 1f * yscale));
		IJ.run(imp_rgb_water, "Draw", "slice");
		ImageUtils.SelectLine(imp_rgb_water, new ImageUtils.Vector2(1f * xscale - border_thickness, border_thickness), new ImageUtils.Vector2(1f * xscale - border_thickness, 1f * yscale - border_thickness));
		IJ.run(imp_rgb_water, "Draw", "slice");

		// Horizontal border decoration
		float horiz_inc = 0.02f;
		for (pos = 0f; pos < 1f; pos += horiz_inc)
		{
			// Top
			ImageUtils.SelectRectangle(imp_rgb_water, pos * xscale, 0, (horiz_inc / 2) * xscale, border_thickness);
			ip_rgb_water.fill();

			// Bottom
			ImageUtils.SelectRectangle(imp_rgb_water, pos * xscale, 1f * yscale - border_thickness, (horiz_inc / 2) * xscale, border_thickness);
			ip_rgb_water.fill();
		}

		// Vertical border decoration
		float vert_inc = horiz_inc * xscale / yscale;
		for (pos = 0f; pos < 1f; pos += vert_inc)
		{
			// Left
			ImageUtils.SelectRectangle(imp_rgb_water, 0, pos * yscale, border_thickness, (vert_inc / 2) * yscale);
			ip_rgb_water.fill();

			// Right
			ImageUtils.SelectRectangle(imp_rgb_water, 1f * xscale - border_thickness, pos * yscale, border_thickness, (vert_inc / 2) * yscale);
			ip_rgb_water.fill();
		}

		// Fill in the land area itself with white.
		imp_rgb_water.setRoi(land_roi);
		ip_rgb_water.setColor(new Color(255,255,255));
		ip_rgb_water.fill(imp_rgb_water.getMask());

		imp_rgb_water.show();

		// Clear the water ovrlay's selection.
		imp_rgb_water.deleteRoi();

		// Composite the final image.

		// Determine the final image size
		new_width = MAP_WIDTH - (2 * MAP_MARGIN);
		new_height = (int)(new_width * ip_rgb.getHeight() / ip_rgb.getWidth());

		// Scale the map image to its final size
		ImageUtils.ScaleImage(imp_rgb, new_width, new_height);
		ip_rgb = imp_rgb.getProcessor(); // Get the image's new ImageProcessor

		// Scale the water overlay image to its final size
		ImageUtils.ScaleImage(imp_rgb_water, new_width, new_height);
		ip_rgb_water = imp_rgb_water.getProcessor(); // Get the image's new ImageProcessor

		// Load the paper mask image
		ImagePlus imp_paper_mask = opener.openImage("./images/map_paper_mask.png");
		ImageProcessor ip_paper_mask = imp_paper_mask.getProcessor();

		// Scale the paper mask image
		ImageUtils.ScaleImage(imp_paper_mask, ip_rgb.getWidth() + (2 * MAP_MARGIN), ip_rgb.getHeight() + (2 * MAP_MARGIN));
		ip_paper_mask = imp_paper_mask.getProcessor(); // Get the image's new ImageProcessor

		// Composite the paper mask onto the water overlay image
		IJ.run(imp_paper_mask, "Copy", "");
		IJ.setPasteMode("Transparent-white");
		IJ.run(imp_rgb_water, "Paste", "");

		// Load the paper image
		ImagePlus imp_paper = opener.openImage("./images/map_paper.png");
		ImageProcessor ip_paper = imp_paper.getProcessor();

		// Scale the paper image
		ImageUtils.ScaleImage(imp_paper, ip_rgb.getWidth() + (2 * MAP_MARGIN), ip_rgb.getHeight() + (2 * MAP_MARGIN));
		ip_paper = imp_paper.getProcessor(); // Get the image's new ImageProcessor

		// Composite the map onto the paper image
		IJ.run(imp_rgb, "Copy", "");
		IJ.setPasteMode("Min");
		IJ.run(imp_paper, "Paste", "");

		ImageUtils.MultiplyComposite(imp_rgb_water, imp_paper, MAP_MARGIN, MAP_MARGIN);

		// Clear the selection.
		imp_paper.deleteRoi();

		imp_paper.show();

		// update screen view of the map image
		imp_rgb.updateAndDraw();

		// Output the map image.
		FileSaver fs = new FileSaver(imp_paper);
		fs.setJpegQuality(80);
		fs.saveAsJpeg(Constants.client_maps_dir + "ui_map.jpg");

		return "Finished generating map image.";
	}

	public static void DrawSeveralMapMarkers(ImagePlus _original_imp, ImagePlus _dest_imp, int _color_index, int _count, String _image_filename)
	{
		int x = 0;

		do
		{
			x = DrawMapMarker(_original_imp, _dest_imp, _color_index, x, _image_filename);
			x += (_original_imp.getWidth() / 10);
			_count--;
		}
		while ((x != -1) && (_count > 0));
	}

	public static int DrawMapMarker(ImagePlus _original_imp, ImagePlus _dest_imp, int _color_index, int _start_x, String _image_filename)
	{
		// Determine location of object
		ImageUtils.Vector2 original_object_loc = ImageUtils.FindColorIndex(_original_imp, _color_index, _start_x);

		//Output.PrintToScreen("DrawMapMarker() " + _image_filename + " _start_x: " + _start_x + ", loc: " + ((original_object_loc == null) ? ("none") : (original_object_loc.x + "," + original_object_loc.y)));

		if (original_object_loc == null) {
			return -1;
		}

		// Scale the object locaton for the destination image.
		ImageUtils.Vector2 object_loc = ImageUtils.Vector2.Multiply(original_object_loc, (float)_dest_imp.getWidth() / (float)_original_imp.getWidth());

		// Load the marker image.
		ImagePlus imp_marker = (new Opener()).openImage("./images/" + _image_filename);
		ImageProcessor ip_marker = imp_marker.getProcessor();

		// Paste the marker image into the _dest_imp at the appropriate position.
		IJ.run(imp_marker, "Copy", "");
		IJ.setPasteMode("Transparent-white");
		_dest_imp.setRoi((int)(object_loc.x - (imp_marker.getWidth() / 2)), (int)(object_loc.y - (imp_marker.getHeight() / 2)), imp_marker.getWidth(), imp_marker.getHeight());
		IJ.run(_dest_imp, "Paste", "");

		return (int)original_object_loc.x;
	}

	public static void SubtractBorderDepression(int _width, int _height, int _max_border_width, double [] _array)
	{
		//double MAX_BORDER_WIDTH = 200/*70*/;
		double MAX_BORDER_FRACTION = 0.4/*70*/;
		double BORDER_POWER = 3.0;
		double BORDER_LEVEL = 1.0;

		double border_width = Math.min(_max_border_width, Math.min(MAX_BORDER_FRACTION * _width, MAX_BORDER_FRACTION * _height));

		double x_dist, y_dist, val;
		for (int y = 0; y < _height; y++)
		{
			for (int x = 0; x < _width; x++)
			{
				x_dist = Math.min(1, Math.min(x, _width - 1 - x) / border_width);
				y_dist = Math.min(1, Math.min(y, _height - 1 - y) / border_width);

				if ((x_dist + y_dist) < 2)
				{
					val = Math.max(0, (1- x_dist) + (1 - y_dist));
					val = Math.pow(val, BORDER_POWER);
					//if (y == 0) Output.PrintToScreen(_array[y * _width + x] + " - " + (val * BORDER_LEVEL));
					_array[y * _width + x] = Math.max(0, (_array[y * _width + x] - (val * BORDER_LEVEL)));
				}
			}
		}
	}

	public static void FillArrayWithSimplexNoise(int _width, int _height, double [] _array, SimplexNoise _simplexNoise)
	{
		double temp;
		for (int y = 0; y < _height; y++)
		{
			for (int x = 0; x < _width; x++)
			{
				_array[y * _width + x] = (_simplexNoise.getNoise(x,y) + 1) / 2;
			}
		}
	}

	public static void ListContacts(String user_name)
  {
    int userID = UserData.GetUserIDByUsername(user_name);
		if (userID <= 0)
		{
			Output.PrintToScreen("Unknown user: '" + user_name + "', userID not found");
		}
		else
		{
			UserData user_data = (UserData)DataManager.GetData(Constants.DT_USER, userID, false);
			if (user_data == null)
			{
				Output.PrintToScreen("Unknown user: '" + user_name + "', data not found");
			}
			else
			{
				UserData contactUserData;
				Output.PrintToScreen("User: '" + user_name + "' (" + userID + ") has " + user_data.contacts.size() + " contacts:");
				Iterator it = user_data.contacts.entrySet().iterator();
				while (it.hasNext())
				{
						Map.Entry pair = (Map.Entry)it.next();
						contactUserData = (UserData)DataManager.GetData(Constants.DT_USER, (Integer)(pair.getKey()), false);
						Output.PrintToScreen("    '" + ((contactUserData == null) ? "not found" : contactUserData.name) + "' (" + (Integer)(pair.getKey()) + ") weight: " + (Integer)(pair.getValue()));
				}
			}
		}
  }

	public static void ListFiles()
	{
		File f = null;
    File[] paths;

    try {
			f = new File(".");

			// returns pathnames for files and directory
			paths = f.listFiles();

			// for each pathname in pathname array
			for(File path:paths) {

				// prints file and directory paths
				Output.PrintToScreen(path.getAbsolutePath());
			}
		} catch(Exception e) {

			 // if any error occurs
			 e.printStackTrace();
		}
	}

	public static void ListFollowers(String user_name)
  {
    int userID = UserData.GetUserIDByUsername(user_name);
		if (userID <= 0)
		{
			Output.PrintToScreen("Unknown user: '" + user_name + "', userID not found");
		}
		else
		{
			UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, userID, false);
			if (userData == null)
			{
				Output.PrintToScreen("Unknown user: '" + user_name + "' (" + userID + "), data not found");
			}
			else
			{
				Output.PrintToScreen("User '" + user_name + "' (" + userID + ") has " + userData.followers.size() + " followers.");
				FollowerData cur_follower;
				UserData followerUserData;
				for (int cur_index = 0; cur_index < userData.followers.size(); cur_index++)
				{
					// Get the current follower's data
					cur_follower = (FollowerData)userData.followers.get(cur_index);

					// Get the current follower's user data
					followerUserData = (UserData)DataManager.GetData(Constants.DT_USER, cur_follower.userID, false);

					if (followerUserData == null) {
						Output.PrintToScreen("    Error: follower with user ID " + cur_follower.userID + " has no data!");
					}	else {
						Output.PrintToScreen("    Follower " + followerUserData.name + " (" + cur_follower.userID + ") bonus XP: " + (int)(cur_follower.bonusXP) + ", credits: " + (int)(cur_follower.bonusCredits) + ", duration: " + (Constants.GetAbsoluteDay() - cur_follower.initDay) + " days.");
					}
				}
			}
		}
  }

	public static void ListIncognitoNations()
	{
		int incognito_count = 0;
		for (int nationID = 1; nationID <= DataManager.GetHighestDataID(Constants.DT_NATION); nationID++)
		{
			// Get the current nation's data
			NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

			// If there is no nation with the current nationID, continue on to the next.
			if (nationData == null) {
				continue;
			}

			if (nationData.GetFlag(Constants.NF_INCOGNITO))
			{
				Output.PrintToScreen(nationData.name + " incognito for " + ((Constants.GetTime() - nationData.prev_go_incognito_time) / 3600) + " hours.");
				incognito_count++;
			}
		}

		Output.PrintToScreen(incognito_count + " incognito nations.");
	}

	public static void ListTemps(String _nation_name)
  {
		int expire_time;
		boolean expire_time_exists;
		TechData tech_data;

		// Get the nation's data
    int nationID = DataManager.GetNameToIDMap(Constants.DT_NATION, _nation_name);
  	NationData nation_data = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);
    if (nation_data == null)
		{
		  Output.PrintToScreen("Unknown nation: '" + _nation_name + "'");
			return;
		}

		// Get the nation's tech data
		NationTechData nation_tech_data = (NationTechData)DataManager.GetData(Constants.DT_NATIONTECH, nationID, false);

		Output.PrintToScreen("Nation " + nation_data.name + " (" + nationID + ") temporary techs:");

		for (Map.Entry<Integer, Integer> entry : nation_tech_data.tech_count.entrySet())
		{
			tech_data = TechData.GetTechData(entry.getKey());

			if (tech_data.duration_type != TechData.DURATION_TEMPORARY) {
				continue;
			}

			expire_time_exists = nation_tech_data.tech_temp_expire_time.containsKey(entry.getKey());

			expire_time = -1;
			if (expire_time_exists) {
				expire_time = nation_tech_data.tech_temp_expire_time.get(entry.getKey());
			}

			Output.PrintToScreen(tech_data.name + " (" + entry.getKey() + "): Count: " + entry.getValue() + ", " + (expire_time_exists ? ("Expires in " + (expire_time - Constants.GetTime()) + "s") : "NO EXPIRE TIME"));
		}
	}

	public static String LoadLandscape(int _landmapID, String _filename)
	{
		int x, y, color_index, i, coord_token, goal_value;
		int terrain, nationID, objectID, old_nationID, old_objectID;
		float rand, coord_ratio;
		ObjectData objectData;
		BlockData block_data;
		BlockExtData block_ext_data;
		String location_string, location_short_string;

		// Return error if no map ID is given.
		if (_landmapID <= 0) {
			return "Invalid map ID: " + _landmapID;
		}

		// Attempt to load the specified image file.
		BufferedImage image = null;
		try {
			image = ImageIO.read(new File(_filename));
		} catch (IOException e) {
			return "Could not open file '" + _filename + "': " + e.getMessage();
		}

		// Get the image's dimensions;
		int width = image.getWidth();
		int height = image.getHeight();

		// Get the land map data with the given _ID (create it if it doesn't yet exist).
		LandMap land_map = DataManager.GetLandMap(_landmapID, true);

		// Set the landmap's info appropriately.
		land_map.info.sourceMapID = _landmapID;
		land_map.info.skin = (_landmapID == Constants.MAINLAND_MAP_ID) ? 0 : 1;

		Output.PrintToScreen("Width: " + width + ", height: " + height + ". Inserting blocks into DB...");

		// Set the new size of the land map, and insert its blocks into the database.
		land_map.SetSize(width, height, true);

		ColorModel colorModel = image.getColorModel();
		IndexColorModel indexColorModel = null;
		if (colorModel instanceof IndexColorModel) {
			indexColorModel = (IndexColorModel)colorModel;
		}	else {
			return "Image " + _filename + " does not have indexed color.";
		}

		DataBuffer dataBuffer = image.getRaster().getDataBuffer();
		DataBufferByte dataBufferByte = null;
		if (dataBuffer instanceof DataBufferByte) {
			dataBufferByte = (DataBufferByte)dataBuffer;
		}	else {
			return "No DataBufferByte";
		}

		// Prepare the output client map image
		BufferedImage client_map_image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		// Get the image data buffer
		byte image_data[] = dataBufferByte.getData();

		Output.PrintToScreen("Generating map...");

		for (y = 0; y < height; y++)
		{
			if ((y % 100) == 0) Output.PrintToScreen("Y: " + y + " of " + height);

			for (x = 0; x < width; x++)
			{
				// Determine this pixel's color index
				color_index = image_data[x + y * width];

				//Output.PrintToScreen("x: " + x + ", y: " + y + ", color_index: " + color_index);

				// Initialize block attributes
				terrain = -1;
				nationID = -1;
				objectID = -1;

				if (color_index == COLOR_INDEX_LAND)
				{
					terrain = Constants.TERRAIN_FLAT_LAND;
				}
				else if (color_index == COLOR_INDEX_WATER)
				{
					terrain = Constants.TERRAIN_SHALLOW_WATER;
				}
				else if (color_index == COLOR_INDEX_MOUNTAIN)
				{
					terrain = Constants.TERRAIN_HILLS;
				}
				else if ((color_index >= COLOR_INDEX_ORB_BASE) && (color_index < COLOR_INDEX_RESOURCE_BASE))
				{
					// Orbs are positioned on flat land
					terrain = Constants.TERRAIN_FLAT_LAND;

					objectID = (color_index - COLOR_INDEX_ORB_BASE) + ObjectData.ORB_BASE_ID;
					objectData = ObjectData.GetObjectData(objectID);

					if (objectData == null)
					{
						// Return error message
						return "Meaningless orb color index " + color_index + " (object ID " + objectID + ") at x: " + x + ", y: " + y;
					}

					if (_landmapID == Constants.MAINLAND_MAP_ID)
					{
						// Determine this goal object's coordinates token
						coord_token = Constants.TokenizeCoordinates(x, y);

						// Store this orb object's information in the ranks data's arrays.
						int ranks_index = RanksData.instance.goals_token.indexOf(coord_token);
						if (ranks_index == -1)
						{
							// The ranks data records for this orb don't yet exist, so create them.
							RanksData.instance.goals_token.add(coord_token);
							RanksData.instance.goals_total_awarded.add(0.0f);
							RanksData.instance.goals_ranks.add(new RanksList());
							RanksData.instance.goals_ranks_monthly.add(new RanksList());
						}

						//Output.PrintToScreen("Placing goal object: " + objectData.name + " at (" + x + ", " + y + "), coord_token: " + coord_token);
					}
				}
				else if (color_index >= COLOR_INDEX_RESOURCE_BASE)
				{
					// Resources are positioned on flat land
					terrain = Constants.TERRAIN_FLAT_LAND;

					objectID = (color_index - COLOR_INDEX_RESOURCE_BASE) + ObjectData.RESOURCE_OBJECT_BASE_ID;

					if (ObjectData.GetObjectData(objectID) == null)
					{
						// Return error message
						return "Meaningless resource object color index " + color_index + " (object ID " + objectID + ") at x: " + x + ", y: " + y;
					}
				}
				else
				{
					// Return error message
					return "Meaningless color index " + color_index + " at x: " + x + ", y: " + y;
				}

				// Get the current block's data and extended data, and mark the block to be updated.
				block_data = DataManager.GetBlockData(_landmapID, x, y, true);
				block_ext_data = land_map.GetBlockExtendedData(x, y, false);

				//Output.PrintToScreen("objectID: " + objectID + ", nationID: " + nationID);

				// Determine the block's former attributes
				old_nationID = block_data.nationID;
				old_objectID = (block_ext_data == null) ? -1 : block_ext_data.objectID;

				// Preserve the block's existing nationID, if possible.
				if ((terrain == Constants.TERRAIN_FLAT_LAND) || (terrain == Constants.TERRAIN_BEACH)) {
					nationID = old_nationID;
				}

				// Preserve the block's existing objectID, if it is a build object, and the new map does not have a landscape object here.
				if ((objectID == -1) && (old_objectID != -1) && (old_objectID < ObjectData.RESOURCE_OBJECT_BASE_ID)) {
					objectID = old_objectID;
				}

				if ((old_nationID > 0) && (old_objectID >= ObjectData.RESOURCE_OBJECT_BASE_ID) && ((nationID != old_nationID) || (objectID != old_objectID)))
				{
					// Remove the former object from the former nation at this block
					Objects.RemoveObject(old_nationID, old_objectID, land_map, x, y, -1, 0);
				}

				if ((nationID > 0) && (objectID >= ObjectData.RESOURCE_OBJECT_BASE_ID) && ((nationID != old_nationID) || (objectID != old_objectID)))
				{
					// Add the new object to the new nation at this block
					Objects.AddObject(nationID, objectID, land_map, x, y, -1, (old_nationID != -1), 0);
				}

				// Set the block's terrain
				block_data.terrain = terrain;

				// Set nationID for this block if it doesn't exist yet or nationID has changed.
				if (nationID != old_nationID) {
					World.SetBlockNationID(land_map, x, y, nationID, false, false, -1, 0); // Don't update objects, or broadcast.
				}

				// Update the block's object ID and info.
				if (objectID == -1)
				{
					// Clear the block's object ID.
					if (block_ext_data != null)
					{
						block_ext_data.InitBuildInfo();
						block_ext_data.objectID = -1;
					}
				}
				else
				{
					// Create the block's extended data, if it doesn't have it yet.
					if (block_ext_data == null) {
						block_ext_data = land_map.GetBlockExtendedData(x, y, true);
					}

					// Set the block's object ID.
					block_ext_data.objectID = objectID;

					if (objectID >= ObjectData.RESOURCE_OBJECT_BASE_ID) {
						block_ext_data.InitBuildInfo(); // Clear info about any former build object at this location.
					}
				}
			}
		}

		Output.PrintToScreen("Finalizing terain...");

		// Examine each block, replacing some terrain types with others, depending on their neighbors.
		for (y = 0; y < height; y++)
		{
			for (x = 0; x < width; x++)
			{
				// Get the current block's data, and mark it to be updated.
				block_data = DataManager.GetBlockData(_landmapID, x, y, true);

				if ((block_data.terrain == Constants.TERRAIN_FLAT_LAND) || (block_data.terrain == Constants.TERRAIN_HILLS) || (block_data.terrain == Constants.TERRAIN_SHALLOW_WATER))
				{
					BlockData block_ul = ((x > 0) && (y > 0)) ? DataManager.GetBlockData(_landmapID, x-1, y-1, false) : null;
					BlockData block_u = (y > 0) ? DataManager.GetBlockData(_landmapID, x, y-1, false) : null;
					BlockData block_ur = ((x < (width - 1)) && (y > 0)) ? DataManager.GetBlockData(_landmapID, x+1, y-1, false) : null;
					BlockData block_l = (x > 0) ? DataManager.GetBlockData(_landmapID, x-1, y, false) : null;
					BlockData block_r = (x < (width - 1)) ? DataManager.GetBlockData(_landmapID, x+1, y, false) : null;
					BlockData block_dl = ((x > 0) && (y < (height - 1))) ? DataManager.GetBlockData(_landmapID, x-1, y+1, false) : null;
					BlockData block_d = (y < (height - 1)) ? DataManager.GetBlockData(_landmapID, x, y+1, false) : null;
					BlockData block_dr = ((x < (width - 1)) && (y < (height - 1))) ? DataManager.GetBlockData(_landmapID, x+1, y+1, false) : null;

					// Any flat land that borders water in any direction is replaced with beach.
					if ((block_data.terrain == Constants.TERRAIN_FLAT_LAND) &&
						  (((block_ul != null) && (block_ul.terrain <= Constants.TERRAIN_SHALLOW_WATER)) || ((block_u != null) && (block_u.terrain <= Constants.TERRAIN_SHALLOW_WATER)) || ((block_ur != null) && (block_ur.terrain <= Constants.TERRAIN_SHALLOW_WATER)) ||
						   ((block_l != null) && (block_l.terrain <= Constants.TERRAIN_SHALLOW_WATER)) || ((block_r != null) && (block_r.terrain <= Constants.TERRAIN_SHALLOW_WATER)) ||
						   ((block_dl != null) && (block_dl.terrain <= Constants.TERRAIN_SHALLOW_WATER)) || ((block_d != null) && (block_d.terrain <= Constants.TERRAIN_SHALLOW_WATER)) || ((block_dr != null) && (block_dr.terrain <= Constants.TERRAIN_SHALLOW_WATER))))
					{
						block_data.terrain = Constants.TERRAIN_BEACH;
					}

					// Any shallow water that is completely surrounded by water is replaced with medium water.
					if ((block_data.terrain == Constants.TERRAIN_SHALLOW_WATER) &&
						  (((block_ul == null) || (block_ul.terrain <= Constants.TERRAIN_SHALLOW_WATER)) && ((block_u == null) || (block_u.terrain <= Constants.TERRAIN_SHALLOW_WATER)) && ((block_ur == null) || (block_ur.terrain <= Constants.TERRAIN_SHALLOW_WATER)) &&
						   ((block_l == null) || (block_l.terrain <= Constants.TERRAIN_SHALLOW_WATER)) && ((block_r == null) || (block_r.terrain <= Constants.TERRAIN_SHALLOW_WATER)) &&
						   ((block_dl == null) || (block_dl.terrain <= Constants.TERRAIN_SHALLOW_WATER)) && ((block_d == null) || (block_d.terrain <= Constants.TERRAIN_SHALLOW_WATER)) && ((block_dr == null) || (block_dr.terrain <= Constants.TERRAIN_SHALLOW_WATER))))
					{
						block_data.terrain = Constants.TERRAIN_MEDIUM_WATER;
					}

					// Any hills that are completely surrounded by hills are replaced with medium mountains.
					if ((block_data.terrain == Constants.TERRAIN_HILLS) &&
						  (((block_ul == null) || (block_ul.terrain >= Constants.TERRAIN_HILLS)) && ((block_u == null) || (block_u.terrain >= Constants.TERRAIN_HILLS)) && ((block_ur == null) || (block_ur.terrain >= Constants.TERRAIN_HILLS)) &&
						   ((block_l == null) || (block_l.terrain >= Constants.TERRAIN_HILLS)) && ((block_r == null) || (block_r.terrain >= Constants.TERRAIN_HILLS)) &&
						   ((block_dl == null) || (block_dl.terrain >= Constants.TERRAIN_HILLS)) && ((block_d == null) || (block_d.terrain >= Constants.TERRAIN_HILLS)) && ((block_dr == null) || (block_dr.terrain >= Constants.TERRAIN_HILLS))))
					{
						block_data.terrain = Constants.TERRAIN_MEDIUM_MOUNTAINS;
					}
				}
			}
		}

		// Examine each block again, replacing some terrain types with others, depending on their neighbors.
		for (y = 0; y < height; y++)
		{
			for (x = 0; x < width; x++)
			{
				// Get the current block's data, and mark it to be updated.
				block_data = DataManager.GetBlockData(_landmapID, x, y, true);

				if ((block_data.terrain == Constants.TERRAIN_MEDIUM_MOUNTAINS) || (block_data.terrain == Constants.TERRAIN_MEDIUM_WATER))
				{
					BlockData block_ul = ((x > 0) && (y > 0)) ? DataManager.GetBlockData(_landmapID, x-1, y-1, false) : null;
					BlockData block_u = (y > 0) ? DataManager.GetBlockData(_landmapID, x, y-1, false) : null;
					BlockData block_ur = ((x < (width - 1)) && (y > 0)) ? DataManager.GetBlockData(_landmapID, x+1, y-1, false) : null;
					BlockData block_l = (x > 0) ? DataManager.GetBlockData(_landmapID, x-1, y, false) : null;
					BlockData block_r = (x < (width - 1)) ? DataManager.GetBlockData(_landmapID, x+1, y, false) : null;
					BlockData block_dl = ((x > 0) && (y < (height - 1))) ? DataManager.GetBlockData(_landmapID, x-1, y+1, false) : null;
					BlockData block_d = (y < (height - 1)) ? DataManager.GetBlockData(_landmapID, x, y+1, false) : null;
					BlockData block_dr = ((x < (width - 1)) && (y < (height - 1))) ? DataManager.GetBlockData(_landmapID, x+1, y+1, false) : null;

					// Any medium water that is completely surrounded by medium water is replaced with deep water.
					if ((block_data.terrain == Constants.TERRAIN_MEDIUM_WATER) &&
						  (((block_ul == null) || (block_ul.terrain <= Constants.TERRAIN_MEDIUM_WATER)) && ((block_u == null) || (block_u.terrain <= Constants.TERRAIN_MEDIUM_WATER)) && ((block_ur == null) || (block_ur.terrain <= Constants.TERRAIN_MEDIUM_WATER)) &&
						   ((block_l == null) || (block_l.terrain <= Constants.TERRAIN_MEDIUM_WATER)) && ((block_r == null) || (block_r.terrain <= Constants.TERRAIN_MEDIUM_WATER)) &&
						   ((block_dl == null) || (block_dl.terrain <= Constants.TERRAIN_MEDIUM_WATER)) && ((block_d == null) || (block_d.terrain <= Constants.TERRAIN_MEDIUM_WATER)) && ((block_dr == null) || (block_dr.terrain <= Constants.TERRAIN_MEDIUM_WATER))))
					{
						block_data.terrain = Constants.TERRAIN_DEEP_WATER;
					}

					// Any medium mountains that are completely surrounded by medium mountains are replaced with tall mountains.
					if ((block_data.terrain == Constants.TERRAIN_MEDIUM_MOUNTAINS) &&
						  (((block_ul == null) || (block_ul.terrain >= Constants.TERRAIN_MEDIUM_MOUNTAINS)) && ((block_u == null) || (block_u.terrain >= Constants.TERRAIN_MEDIUM_MOUNTAINS)) && ((block_ur == null) || (block_ur.terrain >= Constants.TERRAIN_MEDIUM_MOUNTAINS)) &&
						   ((block_l == null) || (block_l.terrain >= Constants.TERRAIN_MEDIUM_MOUNTAINS)) && ((block_r == null) || (block_r.terrain >= Constants.TERRAIN_MEDIUM_MOUNTAINS)) &&
						   ((block_dl == null) || (block_dl.terrain >= Constants.TERRAIN_MEDIUM_MOUNTAINS)) && ((block_d == null) || (block_d.terrain >= Constants.TERRAIN_MEDIUM_MOUNTAINS)) && ((block_dr == null) || (block_dr.terrain >= Constants.TERRAIN_MEDIUM_MOUNTAINS))))
					{
						block_data.terrain = Constants.TERRAIN_TALL_MOUNTAINS;
					}
				}
			}
		}

		// Flag each block that is part of an isolated island, and so should not be inhabited.
		boolean is_island, water_found;
		int nearby_terrain;
		int x1, y1;
		for (y = 0; y < height; y++)
		{
			for (x = 0; x < width; x++)
			{
				// Get the current block's data.
				block_data = land_map.GetBlockData(x, y);

				// If this block is habitable, check to see whether it is part of an island.
				if ((block_data.terrain == Constants.TERRAIN_BEACH) || (block_data.terrain == Constants.TERRAIN_FLAT_LAND))
				{
					is_island = true;

					// Check north of the block
					if (is_island)
					{
						water_found = false;
						for (y1 = y - 1; y1 > (y - ISLAND_CHECK_RANGE); y1--)
						{
							nearby_terrain = land_map.GetBlockTerrain(x, y1);
							if (nearby_terrain <= Constants.TERRAIN_SHALLOW_WATER) {
								water_found = true;
								break;
							}
						}

						if (water_found == false) {
							is_island = false;
						}
					}

					// Check south of the block
					if (is_island)
					{
						water_found = false;
						for (y1 = y + 1; y1 < (y + ISLAND_CHECK_RANGE); y1++)
						{
							nearby_terrain = land_map.GetBlockTerrain(x, y1);
							if (nearby_terrain <= Constants.TERRAIN_SHALLOW_WATER) {
								water_found = true;
								break;
							}
						}

						if (water_found == false) {
							is_island = false;
						}
					}

					// Check west of the block
					if (is_island)
					{
						water_found = false;
						for (x1 = x - 1; x1 > (x - ISLAND_CHECK_RANGE); x1--)
						{
							nearby_terrain = land_map.GetBlockTerrain(x1, y);
							if (nearby_terrain <= Constants.TERRAIN_SHALLOW_WATER) {
								water_found = true;
								break;
							}
						}

						if (water_found == false) {
							is_island = false;
						}
					}

					// Check east of the block
					if (is_island)
					{
						water_found = false;
						for (x1 = x + 1; x1 < (x + ISLAND_CHECK_RANGE); x1++)
						{
							nearby_terrain = land_map.GetBlockTerrain(x1, y);
							if (nearby_terrain <= Constants.TERRAIN_SHALLOW_WATER) {
								water_found = true;
								break;
							}
						}

						if (water_found == false) {
							is_island = false;
						}
					}

					if (is_island)
					{
						// Record flag indicating that this block is part of an island, and update the block's data.
						block_data.flags |= BlockData.BF_ISLAND;
						DataManager.MarkBlockForUpdate(land_map, x, y);
						//Output.PrintToScreen("Island: " + x + "," + y);
					}
				}
			}
		}

		Output.PrintToScreen("Generating client map...");

		// Generate the client map image to output.
		int cur_terrain;
		for (y = 0; y < height; y++)
		{
			for (x = 0; x < width; x++)
			{
				// Get the current block's data, and mark it to be updated.
				block_data = DataManager.GetBlockData(_landmapID, x, y, true);
				block_ext_data = land_map.GetBlockExtendedData(x, y, false);

				// Determine the current block's terrain. If the block has one of several resource objects that require a small mound or body of water,
				// use the TERRAIN_POND or TERRAIN_MOUND values that are only used on the client for display.
				cur_terrain = block_data.terrain;
				if (block_ext_data != null)
				{
					// Geothermal Vent and Fresh water
					if ((block_ext_data.objectID == 1009) || (block_ext_data.objectID == 1013)) cur_terrain = Constants.TERRAIN_POND;

					// Ancient Starship Wreckage and Grave of an Ancient God
					if ((block_ext_data.objectID == 1001) || (block_ext_data.objectID == 1012)) cur_terrain = Constants.TERRAIN_MOUND;
				}

				// Set the blue color component of the corresponding pixel to this block's terrain value. Set alpha to 255.
				client_map_image.setRGB(x, y, (255 << 24) | cur_terrain);
			}
		}

		// Output the client map image.
		try{
			ImageIO.write(client_map_image, "PNG", new File(Constants.client_maps_dir + "map_" + _landmapID + ".png"));
		} catch (IOException ie) {
      Output.PrintException(ie);
    }

		// Record time when this map was previously modified.
		GlobalData.instance.map_modified_times.put(_landmapID, Constants.GetTime());
		DataManager.MarkForUpdate(GlobalData.instance);

		if (_landmapID == Constants.MAINLAND_MAP_ID)
		{
			// Generate the UI map.
			GenerateMap(_filename);

			// Determine the boundary of the new player area.
			World.DetermineNewPlayerAreaBoundary();
		}
		else
		{
			// For non-mainland maps, compile a list of coordinates of all beach squares, as possible raid starting positions.
			for (y = 0; y < height; y++)
			{
				for (x = 0; x < width; x++)
				{
					// Get the current block's data.
					block_data = DataManager.GetBlockData(_landmapID, x, y, false);

					if (block_data.terrain == Constants.TERRAIN_BEACH)
					{
						// Record the coordinates of this beach square.
						land_map.info.beachheads_x.add(x);
						land_map.info.beachheads_y.add(y);
					}
				}
			}
		}

		// Mark the LandMapInfoData to be updated.
		DataManager.MarkForUpdate(land_map.info);

		// Mark the ranks data to be updated
		DataManager.MarkForUpdate(RanksData.instance);

		// Update the database
		DataManager.UpdateDatabase(false);

		// Output results message
		return "Landscape loaded.";
	}

	public static void LogObjects()
	{
		String log_string = "\n\nObjects " + Constants.GetTimestampString() + "\n";
		int array_len = 3000;
		int[] counts = new int[array_len];

		// Get the mainland land map data
		LandMap land_map = DataManager.GetLandMap(Constants.MAINLAND_MAP_ID, false);

		// Count all objects on the map.
		for (int y = 0; y < land_map.height; y++)
		{
			for (int x = 0; x < land_map.width; x++)
			{
				int objectID = land_map.GetBlockObjectID(x, y);

				if ((objectID > -1) && (objectID < array_len)) {
					counts[objectID]++;
				}
			}
		}

		// List counts of each object
		for (int i = 0; i < array_len; i++)
		{
			if (counts[i] > 0)
			{
				String name = "";

				if (i < ObjectData.RESOURCE_OBJECT_BASE_ID)
				{
					BuildData build_data = BuildData.GetBuildData(i);
					name = build_data.name;
				}
				else
				{
					ObjectData object_data = ObjectData.GetObjectData(i);
					name = object_data.name;
				}

				log_string += counts[i] + "\t" + name + " (" + i + ")\n";
			}
		}

		// Write log_string to log file
		Constants.WriteToLog("log_objects.txt", log_string);
	}

	public static void LogPurchasedCredits()
	{
		NationData nationData;

		String log_string = "\nPurchased Credits " + Constants.GetTimestampString() + "\n";

		for (int nationID = 1; nationID <= DataManager.GetHighestDataID(Constants.DT_NATION); nationID++)
		{
			if ((nationID / 1000.0f) == Math.floor(nationID / 1000.0f)) {
				Output.PrintToScreen("NationID: " + nationID);
			}

			// Get the current nation's data
			nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

			// If there is no nation with the current nationID, continue on to the next.
			if (nationData == null) {
				continue;
			}

			// If this nation has no purchased credits, skip it.
			if (nationData.game_money_purchased == 0) {
				continue;
			}

			log_string += nationData.name + " (" + nationID + ")\t" + nationData.game_money_purchased + "\n";
		}

		// Write log_string to log file
		Constants.WriteToLog("log_purchased_credits.txt", log_string);

		Output.PrintToScreen("Done logging purchased credits.");
	}

	public static void LogWinnings()
	{
		NationData nationData;
		float total = 0;

		String log_string = "\nCurrent Prize Winnings " + Constants.GetTimestampString() + "\n";

		for (int nationID = 1; nationID <= DataManager.GetHighestDataID(Constants.DT_NATION); nationID++)
		{
			if ((nationID / 1000.0f) == Math.floor(nationID / 1000.0f)) {
				Output.PrintToScreen("NationID: " + nationID);
			}

			// Get the current nation's data
			nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

			// If there is no nation with the current nationID, continue on to the next.
			if (nationData == null) {
				continue;
			}

			// If this nation has no current prize winnings, skip it.
			if (nationData.prize_money == 0) {
				continue;
			}

			log_string += nationData.name + " (" + nationID + ")\t$" + (Math.round(nationData.prize_money) / 100f) + "\n";

			total += nationData.prize_money;
		}

		log_string += "\nTotal: \t$" + (Math.round(total) / 100f) + "\n";

		// Write log_string to log file
		Constants.WriteToLog("log_winnings.txt", log_string);

		Output.PrintToScreen("Done logging prize winnings.");
	}

	public static String RefreshAllNations()
	{
		int nationID;
		NationData nationData;

		Output.PrintToScreen("Refreshing all nations...");

		for (nationID = 1; nationID <= DataManager.GetHighestDataID(Constants.DT_NATION); nationID++)
		{
			if ((nationID / 1000.0f) == Math.floor(nationID / 1000.0f)) {
				Output.PrintToScreen("NationID: " + nationID);
			}

			// Get the current nation's data
			nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

			// If there is no nation with the current nationID, continue on to the next.
			if (nationData == null) {
				continue;
			}

			// Refresh the current nation, clearing and re-adding its advances and resource objects.
			RefreshNation(nationID);
		}

		Output.PrintToScreen("Done refreshing all nations.");

		// Return a message with the command's output
		return "Success";
	}

	public static void RenewAll()
	{
		int nationID, userID;
		NationData nationData;
		NationTechData nationTechData;
		NationExtData nationExtData;
		UserData userData;

		// Remove all nations from the map.
		LandMap land_map = DataManager.GetLandMap(Constants.MAINLAND_MAP_ID, false);
		for (int y = 0; y <= land_map.height; y++)
		{
			for (int x = 0; x <= land_map.width; x++)
			{
				if (land_map.GetBlockNationID(x, y) != -1)
				{
					World.SetBlockNationID(land_map, x, y, -1, true, false, -1, 0); // Salvage build; don't broadcast.
				}
			}
		}

		// Iterate through all nations...
		for (nationID = 1; nationID <= DataManager.GetHighestDataID(Constants.DT_NATION); nationID++)
		{
			// Get the current nation's data
			nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);
			nationTechData = (NationTechData)DataManager.GetData(Constants.DT_NATIONTECH, nationID, false);
			nationExtData = (NationExtData)DataManager.GetData(Constants.DT_NATION_EXT, nationID, false);

			// If there is no nation with the current nationID, continue on to the next.
			if (nationData == null) {
				continue;
			}

			// Reset the nation's area properties (do this to correct any errors before reseting advances).
			nationData.mainland_footprint.area = 0;
			nationData.mainland_footprint.border_area = 0;
			nationData.mainland_footprint.perimeter = 0;
			nationData.mainland_footprint.x0 = Constants.MAX_MAP_DIM;
			nationData.mainland_footprint.x1 = 0;
			nationData.mainland_footprint.y0 = Constants.MAX_MAP_DIM;
			nationData.mainland_footprint.y1 = 0;

			// Reset the nation's advances and stats
			Gameplay.ResetAdvances(nationID, false);

			// Reset the nation's properties that need to be renewed and were not reset above.
			nationTechData.object_capture_history.clear();
			nationExtData.messages.clear();
			nationData.birth_time = Constants.GetTime();
			nationData.prev_use_time = Constants.GetTime();
			nationData.level = 1;
			nationData.xp = 0;
			nationData.pending_xp = 0;
			nationData.advance_points = 0;
			nationData.nextTechExpireTime = -1;
			nationData.nextTechExpire = -1;
			nationData.targetAdvanceID = -1;
			nationData.prev_free_migration_time = 0;
			nationData.prev_unite_time = 0;
			nationData.game_money = Constants.INIT_GAME_MONEY;
			nationData.game_money_purchased = 0;
			nationData.game_money_won = 0;
			nationData.total_game_money_purchased = 0;
			nationData.prize_money = 0;
			nationData.prize_money_history = 0;
			nationData.prize_money_history_monthly = 0;
			nationData.money_spent = 0;
			nationData.raid_earnings_history = 0;
			nationData.raid_earnings_history_monthly = 0;
			nationData.orb_shard_earnings_history = 0;
			nationData.orb_shard_earnings_history_monthly = 0;
			nationData.medals_history = 0;
			nationData.medals_history_monthly = 0;
			nationData.xp_history = 0;
			nationData.xp_history_monthly = 0;
			nationData.tournament_trophies_history = 0.0f;
			nationData.tournament_trophies_history_monthly = 0.0f;
			nationData.donated_energy_history = 0;
			nationData.donated_energy_history_monthly = 0.0f;
			nationData.donated_manpower_history = 0;
			nationData.donated_manpower_history_monthly = 0.0f;
			nationData.quests_completed = 0;
			nationData.quests_completed_monthly = 0;
			nationData.captures_history = 0;
			nationData.captures_history_monthly = 0;
			nationData.max_area = 0;
			nationData.max_area_monthly = 0;
			nationData.rebirth_countdown = Constants.REBIRTH_COUNTDOWN_START;
			nationData.rebirth_countdown_start = Constants.REBIRTH_COUNTDOWN_START;
			nationData.prev_message_send_day = 0;
			nationData.message_send_count = 0;
			nationData.prev_alliance_request_day = 0;
			nationData.alliance_request_count = 0;
			nationData.rebirth_count = 0;
			nationData.reset_advances_count = 0;
			nationData.prev_update_stats_time = Constants.GetTime();
			nationData.alliances_active.clear();
			nationData.alliances_requests_outgoing.clear();
			nationData.alliances_requests_incoming.clear();
			nationData.unite_requests_outgoing.clear();
			nationData.unite_requests_incoming.clear();
			nationData.chat_list.clear();
			nationData.reverse_chat_list.clear();
			nationData.goals_token.clear();
			nationData.goals_winnings.clear();
			nationData.goals_monthly_token.clear();
			nationData.goals_monthly_winnings.clear();
			nationData.map_flags_token.clear();
			nationData.map_flags_title.clear();
			nationData.areas.clear();
			nationData.quest_records.clear();

			// Mark the nation's data to be updated.
			DataManager.MarkForUpdate(nationData);
		}

		// Iterate through all users...
		for (userID = 1; userID <= DataManager.GetHighestDataID(Constants.DT_USER); userID++)
		{
			// Get the current user's data
			userData = (UserData)DataManager.GetData(Constants.DT_USER, userID, false);

			// If there is no user with the current userID, continue on to the next.
			if (userData == null) {
				continue;
			}

			// Get the user's nation's data
			nationData = (NationData)DataManager.GetData(Constants.DT_NATION, userData.nationID, false);

			// If the user's nation doesn't exist, continue on to the next.
			if (nationData == null) {
				continue;
			}

			// Initialize the user's login report data.
			Login.InitLoginReportData(userData, nationData);

			// Reset the nation's properties that need to be renewed.
			userData.game_ban_end_time = 0;
			userData.chat_ban_end_time = 0;
			userData.prev_report_day = 0;
			userData.report_count = 0;
			userData.chat_offense_level = 0;
			userData.long_term_reports.clear();
			userData.short_term_reports.clear();
			userData.cur_month_patron_bonus_XP = 0;
			userData.cur_month_patron_bonus_credits = 0;
			userData.prev_month_patron_bonus_XP = 0;
			userData.prev_month_patron_bonus_credits = 0;
			userData.total_patron_xp_received = 0;
			userData.total_patron_credits_received = 0;
			userData.max_num_followers = 0;
			userData.max_num_followers_monthly = 0;
			userData.mean_chat_interval = 0;
			userData.prev_chat_fine_time = 0;
			userData.xp = 0;
			userData.xp_monthly = 0;
			userData.xp_monthly_month = 0;
			userData.prev_check_messages_time = 0;
			userData.login_count = 0;
			userData.prev_login_time = 0;
			userData.prev_logout_time = 0;
			userData.play_time = 0;

			// Mark the user's data to be updated.
			DataManager.MarkForUpdate(userData);
		}

		// Renew global data
		GlobalData.instance.money_revenue = 0.0f;
		GlobalData.instance.game_money_awarded = 0.0f;
		GlobalData.instance.cur_ranks_publish_period = 0;

		// Mark the global data to be updated.
		DataManager.MarkForUpdate(GlobalData.instance);

		// Renew ranks data
		RanksData.instance.goals_token.clear();
		RanksData.instance.goals_total_awarded.clear();
		RanksData.instance.goals_ranks.clear();
		RanksData.instance.goals_ranks_monthly.clear();
		RanksData.instance.ranks_nation_xp.Clear();
		RanksData.instance.ranks_nation_xp_monthly.Clear();
		RanksData.instance.ranks_user_xp.Clear();
		RanksData.instance.ranks_user_xp_monthly.Clear();
		RanksData.instance.ranks_user_followers.Clear();
		RanksData.instance.ranks_user_followers_monthly.Clear();
		RanksData.instance.ranks_nation_winnings.Clear();
		RanksData.instance.ranks_nation_winnings_monthly.Clear();
		RanksData.instance.ranks_nation_latest_tournament.Clear();
		RanksData.instance.ranks_nation_tournament_trophies.Clear();
		RanksData.instance.ranks_nation_tournament_trophies_monthly.Clear();
		RanksData.instance.ranks_nation_level.Clear();
		RanksData.instance.ranks_nation_rebirths.Clear();
		RanksData.instance.ranks_nation_quests.Clear();
		RanksData.instance.ranks_nation_quests_monthly.Clear();
		RanksData.instance.ranks_nation_energy_donated.Clear();
		RanksData.instance.ranks_nation_energy_donated_monthly.Clear();
		RanksData.instance.ranks_nation_manpower_donated.Clear();
		RanksData.instance.ranks_nation_manpower_donated_monthly.Clear();
		RanksData.instance.ranks_nation_area.Clear();
		RanksData.instance.ranks_nation_area_monthly.Clear();
		RanksData.instance.ranks_nation_captures.Clear();
		RanksData.instance.ranks_nation_captures_monthly.Clear();
		RanksData.instance.ranks_nation_medals.Clear();
		RanksData.instance.ranks_nation_medals_monthly.Clear();
		RanksData.instance.ranks_nation_raid_earnings.Clear();
		RanksData.instance.ranks_nation_raid_earnings_monthly.Clear();
		RanksData.instance.ranks_nation_orb_shard_earnings.Clear();
		RanksData.instance.ranks_nation_orb_shard_earnings_monthly.Clear();

		// Mark the ranks data to be updated.
		DataManager.MarkForUpdate(RanksData.instance);

		// Update the database.
		DataManager.UpdateDatabase(false);

		Output.PrintToScreen("RenewAll() complete.");
	}

	public static String RepairAllNations()
	{
		int nationID;
		NationData nationData;

		Output.PrintToScreen("Repairing all nations...");

		for (nationID = 1; nationID <= DataManager.GetHighestDataID(Constants.DT_NATION); nationID++)
		{
			if ((nationID / 1000.0f) == Math.floor(nationID / 1000.0f)) {
				Output.PrintToScreen("NationID: " + nationID);
			}

			// Get the current nation's data
			nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

			// If there is no nation with the current nationID, continue on to the next.
			if (nationData == null) {
				continue;
			}

			// Repair the current nation.
			nationData.Repair();
		}

		Output.PrintToScreen("Done repairing all nations.");

		// Return a message with the command's output
		return "Success";
	}

	public static String RepairAllUsers()
	{
		int userID;
		UserData userData;

		Output.PrintToScreen("Repairing all users...");

		for (userID = 1; userID <= DataManager.GetHighestDataID(Constants.DT_USER); userID++)
		{
			if ((userID / 1000.0f) == Math.floor(userID / 1000.0f)) {
				Output.PrintToScreen("UserID: " + userID);
			}

			// Get the current user's data
			userData = (UserData)DataManager.GetData(Constants.DT_USER, userID, false);

			// If there is no user with the current userID, continue on to the next.
			if (userData == null) {
				continue;
			}

			// Repair the current user.
			userData.Repair();
		}

		Output.PrintToScreen("Done repairing all users.");

		// Return a message with the command's output
		return "Success";
	}

  public static void MakeAbsentee(String user_name)
  {
    int userID = UserData.GetUserIDByUsername(user_name);
    UserData user_data = (UserData)DataManager.GetData(Constants.DT_USER, userID, false);

    if (user_data == null)
    {
    	Output.PrintToScreen("Unknown user: '" + user_name + "'");
    }
    else
    {
    	// Set the user's prev_login_time to an absentee time
    	user_data.prev_login_time = Constants.GetTime() - Constants.TIME_SINCE_LAST_LOGIN_ABSENTEE;
    	Output.PrintToScreen(user_name + " now considered absentee");

    	// Mark the user's data to be updated
    	DataManager.MarkForUpdate(user_data);
    }
  }

  public static void MigrateNation(String nation_name, int _x, int _y)
  {
    int nationID = DataManager.GetNameToIDMap(Constants.DT_NATION, nation_name);
    NationData nation_data = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

    if (nation_data == null)
    {
    	Output.PrintToScreen("Unknown nation: '" + nation_name + "'");
    }
    else
    {
			LandMap mainland_map = DataManager.GetLandMap(Constants.MAINLAND_MAP_ID, false);

			// Create random level-appropriate center position, if no position was given.
			Random rand = new Random();
			if (_x == 0) _x = mainland_map.MaxLevelLimitToPosX(nation_data.level);
      if (_y == 0) _y = (int)(mainland_map.height * 0.1) + rand.nextInt((int)(mainland_map.height * 0.8));

      // Write event to log
      Constants.WriteToLog("log_migrate.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " @migrate_nation used from server console on nation " + nation_name + "\n");

      // Migrate the nation
      World.MigrateNation(mainland_map, nationID, _x, _y, true, -1, true);
    }
  }

  public static void ChatBanUser(StringBuffer output_buffer, String user_name, int num_hours, boolean console, boolean complaint)
  {
    int userID = UserData.GetUserIDByUsername(user_name);
		UserData user_data = (UserData)DataManager.GetData(Constants.DT_USER, userID, false);

		if (user_data == null)
		{
      if(console)
      {
		    Output.PrintToScreen("Unknown user: " + user_name);
      }
      else
      {
		    Output.PrintToScreen("Admin Client Chat Ban User Error: Unknown user: " + user_name);
  	    OutputEvents.GetMessageEvent(output_buffer, ClientString.Get("svr_msg_unknown_user", "username", user_name)); // "Unknown user: " + user_name
      }
      return;
		}

		//If num_hours is 0 unban user
		if(num_hours == 0) {
			user_data.chat_ban_end_time = 0;
		}	else {
			// Record the ban
			user_data.chat_ban_end_time = Math.max(Constants.GetTime(), user_data.chat_ban_end_time) + (num_hours * Constants.SECONDS_PER_HOUR);
		}

		// Record the ban in the player's account.
		user_data.UpdateComplaintAndBanCounts(0, 0, 0, 1, 0);

		// Copy the user's bans to ist asociated users and devices.
		user_data.CopyBansToAssociatedUsersAndDevices();

		DataManager.MarkForUpdate(user_data);

		if(console)
		{
			Output.PrintToScreen("User account " + user_name + " banned for " + num_hours + " hours.");
			Constants.WriteToLog("log_ban.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " Server command banned " + user_data.name + " (u:" + userID + ", " + user_data.email + ") from chat for " + num_hours + " hours.\n");
		}
		else
		{
			Output.PrintToScreen("Admin Client Chat Ban User Success: User account " + user_name + " banned for " + num_hours + " hours.");
			OutputEvents.GetMessageEvent(output_buffer, ClientString.Get("User account " + user_name + " banned from chat for " + num_hours + " hours."));
			Constants.WriteToLog("log_ban.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " Admin account banned " + user_data.name + " (u:" + userID + ", " + user_data.email + ") from chat for " + num_hours + " hours.\n");
		}
	}

	public static void DeviceInfo(int _deviceID)
  {
    DeviceData device_data = (DeviceData)DataManager.GetData(Constants.DT_DEVICE, _deviceID, false);

    if (device_data == null)
    {
      Output.PrintToScreen("There is no device data for ID " + _deviceID + ".");
      return;
    }

    Output.PrintToScreen("============================");
    Output.PrintToScreen("Device " + _deviceID + ": " + device_data.device_type);
		Output.PrintToScreen("Name: " + device_data.name);
		Output.PrintToScreen("UID: " + device_data.uid);
		Output.PrintToScreen("Type: " + device_data.device_type);
		Output.PrintToScreen("prev_IP: " + device_data.prev_IP);
		Output.PrintToScreen("creation_time: " + ((Constants.GetTime() - device_data.creation_time) / Constants.SECONDS_PER_DAY) + " days ago");
		if (device_data.fealty0_nationID > 0) Output.PrintToScreen("Fealty 0 nation ID: " + device_data.fealty0_nationID + ", prev attack time: " + ((Constants.GetTime() - device_data.fealty0_prev_attack_time) / 60) + " mins ago.");
		if (device_data.fealty1_nationID > 0) Output.PrintToScreen("Fealty 1 nation ID: " + device_data.fealty1_nationID + ", prev attack time: " + ((Constants.GetTime() - device_data.fealty1_prev_attack_time) / 60) + " mins ago.");
		if (device_data.fealty2_nationID > 0) Output.PrintToScreen("Fealty 2 nation ID: " + device_data.fealty2_nationID + ", prev attack time: " + ((Constants.GetTime() - device_data.fealty2_prev_attack_time) / 60) + " mins ago.");
		if (device_data.fealty_tournament_nationID > 0) Output.PrintToScreen("Fealty Tournament nation ID: " + device_data.fealty_tournament_nationID + ", start day: " + device_data.fealty_tournament_start_day + ".");

		DeviceAccountData device_account = DeviceDB.ReadDeviceAccount(device_data.name);
		if (device_account != null) {
			Output.PrintToScreen("Server Independent: " + (device_account.veteran ? "VET" : "NEW"));
		}

		if (device_data.associated_devices.size() > 0)
		{
			Output.PrintToScreen("Associated devices:");

			for (int i = 0; i < device_data.associated_devices.size(); i++)
			{
				DeviceData assoc_device_data = (DeviceData)DataManager.GetData(Constants.DT_DEVICE, device_data.associated_devices.get(i), false);
				if (assoc_device_data != null) Output.PrintToScreen("  Device " + device_data.associated_devices.get(i) + ": " + assoc_device_data.GetDeviceType());
			}
		}

		if (device_data.users.size() > 0)
		{
			Output.PrintToScreen("Users:");

			// Iterate through each of the device's users
			UserData cur_user_data;
			for (int cur_user_index = 0; cur_user_index < device_data.users.size(); cur_user_index++)
			{
				// Get the current user's data
				 cur_user_data = (UserData)DataManager.GetData(Constants.DT_USER, device_data.users.get(cur_user_index), false);

				// Output info about user
				Output.PrintToScreen("  User " + cur_user_data.name + " (" + cur_user_data.ID + ") " + (WOCServer.IsUserLoggedIn(cur_user_data.ID) ? "ONLINE " : "") + "email: " + cur_user_data.email);
			}
		}

		if ((device_data.num_correlation_checks > 0) && (device_data.correlation_counts.size() > 0))
		{
			Output.PrintToScreen("Correlated devices:");

			for (Map.Entry<Integer, Integer> entry : device_data.correlation_counts.entrySet())
			{
				float correlation = (float)(entry.getValue()) / (float)device_data.num_correlation_checks;

				if (correlation > 0.25f)
				{
					DeviceData correl_device_data = (DeviceData)DataManager.GetData(Constants.DT_DEVICE, entry.getKey(), false);
					Output.PrintToScreen("  Device " + entry.getKey() + " (" + entry.getValue() + "/" + device_data.num_correlation_checks + ", " + correlation + "): " + correl_device_data.GetDeviceType());

					if (device_data.tracking_correlations.containsKey(correl_device_data.ID))
					{
						CorrelationRecord cor_record = device_data.tracking_correlations.get(correl_device_data.ID);
						Output.PrintToScreen("    Activity cor: 10m: " + cor_record.count_interval_10m + ", 60s: " + cor_record.count_interval_60s + ", 30s: " + cor_record.count_interval_30s + ", 2s: " + cor_record.count_interval_2s + ", 1s: " + cor_record.count_interval_1s);
					}
				}
			}
		}

    Output.PrintToScreen("============================");
  }

	public static void EmailInfo(String _email)
  {
		int emailID = EmailData.GetEmailIDByEmail(_email);

    if (emailID == -1)
    {
      Output.PrintToScreen("There is no email ID associated with email address '" + _email + "'.");
      return;
    }

    EmailData email_data = (EmailData)DataManager.GetData(Constants.DT_EMAIL, emailID, false);

    if (email_data == null)
    {
      Output.PrintToScreen("There is no email data for email ID " + emailID + ".");
      return;
    }

    Output.PrintToScreen("============================");
    Output.PrintToScreen("Email: " + _email);

		if (email_data.users.size() > 0)
		{
			Output.PrintToScreen("Users:");

			// Iterate through each of the email's users
			UserData cur_user_data;
			for (int cur_user_index = 0; cur_user_index < email_data.users.size(); cur_user_index++)
			{
				// Get the current user's data
				 cur_user_data = (UserData)DataManager.GetData(Constants.DT_USER, email_data.users.get(cur_user_index), false);

				// Output info about user
				Output.PrintToScreen("  User " + cur_user_data.name + " (" + cur_user_data.ID + ") " + (WOCServer.IsUserLoggedIn(cur_user_data.ID) ? "ONLINE " : "") + "email: " + cur_user_data.email);
			}
		}

    Output.PrintToScreen("============================");
  }

  public static void NationInfo(StringBuffer output_buffer, String nation_name, boolean console)
  {
    int nationID = DataManager.GetNameToIDMap(Constants.DT_NATION, nation_name);
    NationData nation_data = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

    if (nation_data == null)
    {
      if(console)
      {
    	  Output.PrintToScreen("Unknown nation: '" + nation_name + "'");
      }
      else
      {
		    Output.PrintToScreen("Admin Client Nation Info Error: Unknown nation: " + nation_name);
  	    OutputEvents.GetMessageEvent(output_buffer, ClientString.Get("svr_msg_unknown_nation", "nation_name", nation_name)); // "Unknown nation: " + nation_name
      }
      return;
    }
    else
    {
      if(console)
      {
				NationTechData nationTechData = (NationTechData)DataManager.GetData(Constants.DT_NATIONTECH, nationID, false);

				if (nationTechData == null) {
					Output.PrintToScreen("Tech data missing for nation " + nationID);
					return;
				}

				int cur_count, count_perm = 0, count_temp = 0, count_level = 0, count_pay = 0, count_object = 0;
				Iterator it = nationTechData.tech_count.entrySet().iterator();
				TechData tech_data;
				while (it.hasNext())
				{
					Map.Entry pairs = (Map.Entry)it.next();

					// Get the ID and data for the current advance
					tech_data = TechData.GetTechData((Integer)(pairs.getKey()));
					cur_count = (Integer)(pairs.getValue());

					if (tech_data != null)
					{
						if ((tech_data.duration_type == TechData.DURATION_PERMANENT) && (tech_data.default_price == 0) && (!tech_data.initial)) count_level += cur_count;
						if (tech_data.duration_type == TechData.DURATION_PERMANENT) count_perm += cur_count;
						if (tech_data.duration_type == TechData.DURATION_TEMPORARY) count_temp += cur_count;
						if (tech_data.default_price != 0) count_pay += cur_count;
						if (tech_data.duration_type == TechData.DURATION_OBJECT) count_object += cur_count;
					}
				}

        Output.PrintToScreen("============================");
        Output.PrintToScreen(" Nation " + nation_data.name + " (" + nationID + ")" + (nation_data.veteran ? " vet" : " new"));
        Output.PrintToScreen(" -- level: " + nation_data.level + ", XP: " + nation_data.xp + ((nation_data.pending_xp > 0) ? (" (pending: " + nation_data.pending_xp + ")") : "") + ", password: " + nation_data.password + ", flags: " + nation_data.flags);
        Output.PrintToScreen(" -- Mainland area: " + nation_data.mainland_footprint.area + ", border area: " + nation_data.mainland_footprint.border_area + ", perimeter: " + nation_data.mainland_footprint.perimeter + ", en burn rate: " + nation_data.GetFinalEnergyBurnRate(Constants.MAINLAND_MAP_ID) + ", geo eff: " + nation_data.GetFinalGeoEfficiency(Constants.MAINLAND_MAP_ID));
				if (nation_data.homeland_mapID > 0) Output.PrintToScreen(" -- Homeland area: " + nation_data.homeland_footprint.area + ", border area: " + nation_data.homeland_footprint.border_area + ", perimeter: " + nation_data.homeland_footprint.perimeter + ", en burn rate: " + nation_data.GetFinalEnergyBurnRate(nation_data.homeland_mapID) + ", geo eff: " + nation_data.GetFinalGeoEfficiency(nation_data.homeland_mapID)); else Output.PrintToScreen(" -- No homeland");
				Output.PrintToScreen(" -- Credits: " + nation_data.game_money + " (" + nation_data.game_money_purchased + " purchased," + nation_data.game_money_won + " won), money_spent: " + nation_data.money_spent);
				if (nation_data.prize_money_history > 0) Output.PrintToScreen(" -- Prize Money: " + nation_data.prize_money + " (" + nation_data.prize_money_history + " all-time, " + nation_data.prize_money_history_monthly + " this month)");
				Output.PrintToScreen(" -- energy: " + nation_data.energy + "/" + nation_data.energy_max + ", manpower: " + nation_data.mainland_footprint.manpower + "/" + nation_data.GetMainlandManpowerMax() + ", advance points: " + nation_data.advance_points);
				Output.PrintToScreen(" -- stats: Tech: " + nation_data.GetFinalStatTech(Constants.MAINLAND_MAP_ID) + " (perm " + nation_data.tech_perm + ", obj " + nation_data.tech_object + ", tmp " + nation_data.tech_temp + ", mult " + nation_data.tech_mult + ")");
				Output.PrintToScreen("           Bio: " + nation_data.GetFinalStatBio(Constants.MAINLAND_MAP_ID) + " (perm " + nation_data.bio_perm + ", obj " + nation_data.bio_object + ", tmp " + nation_data.bio_temp + ", mult " + nation_data.bio_mult + ")");
				Output.PrintToScreen("           Psi: " + nation_data.GetFinalStatPsi(Constants.MAINLAND_MAP_ID) + " (perm " + nation_data.psi_perm + ", obj " + nation_data.psi_object + ", tmp " + nation_data.psi_temp + ", mult " + nation_data.psi_mult + ")");
				Output.PrintToScreen("           XP Mult: " + nation_data.GetFinalXPMultiplier(Constants.MAINLAND_MAP_ID) + " (perm " + nation_data.xp_multiplier_perm + ", obj " + nation_data.xp_multiplier_object + ", tmp " + nation_data.xp_multiplier_temp + ")");
				Output.PrintToScreen("           hp base: " + nation_data.hit_points_base + ", mp/atk: " + nation_data.manpower_per_attack);
				Output.PrintToScreen("           mp rate: " + nation_data.GetFinalManpowerRate(Constants.MAINLAND_MAP_ID) + ", en rate: " + nation_data.GetFinalEnergyRate(Constants.MAINLAND_MAP_ID));
				Output.PrintToScreen("           mp burn rate: " + nation_data.manpower_burn_rate + ", crit: " + nation_data.crit_chance + ", splash: " + nation_data.splash_damage + ", en burn rate: " + nation_data.GetFinalEnergyBurnRate(Constants.MAINLAND_MAP_ID));
				Output.PrintToScreen("           stored en: " + (int)(nation_data.shared_energy_capacity * nation_data.shared_energy_fill) + "/" + nation_data.shared_energy_capacity + " (" + nation_data.shared_energy_xp_per_hour + " XP/hr) , mp: " + (int)(nation_data.shared_manpower_capacity * nation_data.shared_manpower_fill) + "/" + nation_data.shared_manpower_capacity + " (" + nation_data.shared_manpower_xp_per_hour + " XP/hr)");
				Output.PrintToScreen(" -- advances: lvl: " + count_level + ", obj: " + count_object + ", pay: " + count_pay + ", temp: " + count_temp + ", perm: " + count_perm + ", ttl: " + nationTechData.tech_count.size());
        Output.PrintToScreen(" -- rebirth countdown: " + nation_data.rebirth_countdown + ", purchased: " + nation_data.rebirth_countdown_purchased + ", rebirths: " + nation_data.rebirth_count);
				Output.PrintToScreen(" -- suspect time remaining: " + ((nation_data.log_suspect_expire_time <= Constants.GetFineTime()) ? "None" : (((nation_data.log_suspect_expire_time - Constants.GetFineTime()) / 60000) + " mins")));
				Output.PrintToScreen(" -- footprint: " + nation_data.mainland_footprint.x0 + "," + nation_data.mainland_footprint.y0 + " to " + nation_data.mainland_footprint.x1 + "," + nation_data.mainland_footprint.y1);
				Output.PrintToScreen(" -- Raid medals: " + (nation_data.raid_attacker_medals + nation_data.raid_defender_medals) + " (" + nation_data.raid_attacker_medals + " att, " + nation_data.raid_defender_medals + " def)");
        Output.PrintToScreen(" -- Users:");

        // Iterate through each of the nation's users
        UserData cur_user_data;
        for (int cur_user_index = 0; cur_user_index < nation_data.users.size(); cur_user_index++)
        {
          // Get the current user's data
           cur_user_data = (UserData)DataManager.GetData(Constants.DT_USER, nation_data.users.get(cur_user_index), false);

          // Output info about user
          Output.PrintToScreen(" ---- User " + cur_user_data.name + " (" + cur_user_data.ID + ")" + (WOCServer.IsUserLoggedIn(cur_user_data.ID) ? " ONLINE " : "") + (cur_user_data.veteran ? " vet" : " new") + " email: " + cur_user_data.email + ", rank: " + cur_user_data.rank);
        }

        Output.PrintToScreen("============================");
      }
      else
      {
        OutputEvents.GetAdminNationInfoEvent(output_buffer, nation_data);
      }
      return;
    }
  }

  public static void UserInfo(String user_name)
  {
    int userID = UserData.GetUserIDByUsername(user_name);

    if (userID <= 0)
    {
    	Output.PrintToScreen("Unknown user: '" + user_name + "', userID not found");
      return;
    }

    UserData user_data = (UserData)DataManager.GetData(Constants.DT_USER, userID, false);

    if (user_data == null)
    {
      Output.PrintToScreen("Unknown user: '" + user_name + "', data not found");
      return;
    }

    // Construct creation time string
    Date whole_date = new Date(((long)(user_data.creation_time)) * (long)1000);
    String creation_date_string = (whole_date.getMonth() + 1) + "/" + whole_date.getDate() + "/" + (whole_date.getYear() + 1900) + " " + whole_date.getHours() + ":" + whole_date.getMinutes();

    //// TESTING Current time string
    //whole_date = new Date(((long)(Constants.GetTime())) * (long)1000);
    //String current_date_string = (whole_date.getMonth() + 1) + "/" + whole_date.getDate() + "/" + (whole_date.getYear() + 1900) + " " + whole_date.getHours() + ":" + whole_date.getMinutes();

    int cur_time = Constants.GetTime();
    NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, user_data.nationID, false);
		ClientThread clientThread = WOCServer.GetClientThread(userID);

		int prev_use_days_ago = (Constants.GetTime() - user_data.prev_logout_time) / Constants.SECONDS_PER_DAY;

    Output.PrintToScreen("============================");
    Output.PrintToScreen("User: " + user_name + " (" + userID + ")" + (WOCServer.IsUserLoggedIn(userID) ? " ONLINE" : " offline") + (user_data.veteran ? " vet" : " new"));
		Output.PrintToScreen("Nation: " + ((nationData == null) ? "none" : nationData.name) + " (" + user_data.nationID + ")");
    Output.PrintToScreen("e-mail address: " + user_data.email);
		Output.PrintToScreen("XP: " + user_data.xp + ", login count: " + user_data.login_count + ", play time: " + (int)(user_data.play_time / 3600) + " hrs" + ", prev use: " + ((prev_use_days_ago == 0) ? "today" : (prev_use_days_ago + " days ago")));
    Output.PrintToScreen("patron code: " + user_data.patron_code + ", num followers: " + user_data.followers.size());
		Output.PrintToScreen("Creation time: " + creation_date_string + " (" + user_data.creation_time + ")");
		Output.PrintToScreen("Moderator level: " + user_data.mod_level + ", Ad bonus available: " + user_data.ad_bonus_available);
    Output.PrintToScreen("Game ban time remaining: " + ((user_data.game_ban_end_time <= cur_time) ? "None" : (((user_data.game_ban_end_time - cur_time) / Constants.SECONDS_PER_DAY) + "d " + ((user_data.game_ban_end_time - cur_time) % Constants.SECONDS_PER_DAY / Constants.SECONDS_PER_HOUR) + "h " + ((user_data.game_ban_end_time - cur_time) % Constants.SECONDS_PER_HOUR / 60) + "m")));
		Output.PrintToScreen("Chat ban time remaining: " + ((user_data.chat_ban_end_time <= cur_time) ? "None" : (((user_data.chat_ban_end_time - cur_time) / Constants.SECONDS_PER_DAY) + "d " + ((user_data.chat_ban_end_time - cur_time) % Constants.SECONDS_PER_DAY / Constants.SECONDS_PER_HOUR) + "h " + ((user_data.chat_ban_end_time - cur_time) % Constants.SECONDS_PER_HOUR / 60) + "m")));
    Output.PrintToScreen("Chat offense level: " + user_data.chat_offense_level);
		Output.PrintToScreen("View: " + user_data.viewX + "," + user_data.viewY + ", origin chunk: " + user_data.viewChunkX0 + "," + user_data.viewChunkY0);

		if (clientThread != null)
		{
			Output.PrintToScreen("Suspect time remaining: " + ((clientThread.log_suspect_expire_time <= Constants.GetFineTime()) ? "None" : (((clientThread.log_suspect_expire_time - Constants.GetFineTime()) / 60000) + " mins")));

			if (user_data.fealty_nationID != -1)
			{
				NationData fealtyNationData = (NationData)DataManager.GetData(Constants.DT_NATION, user_data.fealty_nationID, false);
				Output.PrintToScreen("Fealty time remaining: " + ((user_data.fealty_end_time <= Constants.GetTime()) ? "None" : (((user_data.fealty_end_time - Constants.GetTime()) / 60) + " mins")) + " due to nation " + fealtyNationData.name + " (" + user_data.fealty_nationID + ")");
			}
		}

		// Log info about each device associated with this user.
		if (user_data.devices.size() > 0)
		{
			Output.PrintToScreen("Devices:");

			for (Map.Entry<Integer, Integer> entry : user_data.devices.entrySet())
			{
				DeviceData device_data = (DeviceData)DataManager.GetData(Constants.DT_DEVICE, entry.getKey(), false);
				if (device_data != null) Output.PrintToScreen("  Device " + entry.getKey() + " (" + entry.getValue() + " uses): " + device_data.GetDeviceType());
			}
		}

		// Log list of associated users.
		if (user_data.associated_users.size() > 0)
		{
			Output.PrintToScreen("Associated users:");

			// Iterate through each of the user's associated users
			UserData cur_user_data;
			for (int cur_user_index = 0; cur_user_index < user_data.associated_users.size(); cur_user_index++)
			{
				// Get the current associated user's data
				 cur_user_data = (UserData)DataManager.GetData(Constants.DT_USER, user_data.associated_users.get(cur_user_index), false);

				// Output info about user
				prev_use_days_ago = (Constants.GetTime() - cur_user_data.prev_logout_time) / Constants.SECONDS_PER_DAY;
				Output.PrintToScreen("  User " + cur_user_data.name + " (" + cur_user_data.ID + ") " + (WOCServer.IsUserLoggedIn(cur_user_data.ID) ? "ONLINE " : "") + "email: " + cur_user_data.email + ", prev use: " + ((prev_use_days_ago == 0) ? "today" : (prev_use_days_ago + " days ago")));
			}
		}

		// Log list of muted users.
		if (user_data.muted_users.size() > 0)
		{
			Output.PrintToScreen("Muted users (" + user_data.muted_users.size() + "):");

			// Iterate through each of the user's muted users
			UserData cur_user_data;
			for (int cur_user_index = 0; cur_user_index < user_data.muted_users.size(); cur_user_index++)
			{
				// Get the current muted user's data
				 cur_user_data = (UserData)DataManager.GetData(Constants.DT_USER, user_data.muted_users.get(cur_user_index), false);

				// Output info about user
				prev_use_days_ago = (Constants.GetTime() - cur_user_data.prev_logout_time) / Constants.SECONDS_PER_DAY;
				Output.PrintToScreen("  User " + cur_user_data.name + " (" + cur_user_data.ID + ") " + (WOCServer.IsUserLoggedIn(cur_user_data.ID) ? "ONLINE " : "") + "email: " + cur_user_data.email + ", prev use: " + ((prev_use_days_ago == 0) ? "today" : (prev_use_days_ago + " days ago")));
			}
		}

		// Log list of muted devices.
		if (user_data.muted_devices.size() > 0)
		{
			Output.PrintToScreen("Muted devices (" + user_data.muted_devices.size() + "):");

			// Iterate through each of the user's muted devices
			for (int cur_device_index = 0; cur_device_index < user_data.muted_devices.size(); cur_device_index++)
			{
				// Output the muted device's ID
				Output.PrintToScreen("  Device " + user_data.muted_devices.get(cur_device_index));
			}
		}

    Output.PrintToScreen("============================");
  }

	public static void PlayerInfo(String user_name)
  {
    int playerID = AccountDB.GetPlayerIDByUsername(user_name);

    if (playerID <= 0)
    {
    	Output.PrintToScreen("Unknown player: '" + user_name + "', playerID not found");
      return;
    }

		// Fetch player account data.
		PlayerAccountData accountData = AccountDB.ReadPlayerAccount(playerID);

		if (accountData == null)
    {
      Output.PrintToScreen("Unknown player: '" + user_name + "', data not found");
      return;
    }

    Output.PrintToScreen("============================");
    Output.PrintToScreen("Username: " + accountData.username);
		Output.PrintToScreen("Player ID: " + playerID);
    Output.PrintToScreen("e-mail address: " + accountData.email);
		Output.PrintToScreen("passhash: " + accountData.passhash);
		Output.PrintToScreen("security question: " + accountData.security_question);
		Output.PrintToScreen("security answer: " + accountData.security_answer);
		Output.PrintToScreen("info: " + accountData.info);
    Output.PrintToScreen("woc2_serverID: " + accountData.woc2_serverID);
    Output.PrintToScreen("============================");
  }

  public static void ProcessPurchase(String _user_name, int _package)
  {
		if ((_package < 0) || (_package >= Constants.NUM_CREDIT_PACKAGES))
		{
			Output.PrintToScreen("Package: '" + _package + "' does not exist.");
			return;
    }

    int userID = UserData.GetUserIDByUsername(_user_name);

    if (userID <= 0)
    {
    	Output.PrintToScreen("Unknown user: '" + _user_name + "', userID not found");
			return;
    }

		UserData user_data = (UserData)DataManager.GetData(Constants.DT_USER, userID, false);

		if (user_data == null)
		{
			Output.PrintToScreen("Unknown user: '" + _user_name + "', data not found");
		}

		// Process the purchase
		Money.PurchaseCredits(userID, _package, Constants.BUY_CREDITS_COST_USD[_package], "USD");
		Output.PrintToScreen("The purchase has been processed.");
  }

	public static int ReadInt(FileInputStream _stream)
	{
		String str = "";
		char ch;

		try {
			while((ch = (char)(_stream.read())) != ',')
			{
				str += ch;
			}
		}	catch(Exception e) {Output.PrintToScreen("ReadInt() unable to read from file");}

		return Integer.parseInt(str);
	}

  public static void RebirthNation(String nation_name)
  {
    int nationID = DataManager.GetNameToIDMap(Constants.DT_NATION, nation_name);
    NationData nation_data = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

    if (nation_data == null)
    {
    	Output.PrintToScreen("Unknown nation: '" + nation_name + "'");
    }
    else
    {
    	// Rebirth the nation
    	Gameplay.RebirthNation(nation_data);
    	Output.PrintToScreen(nation_name + " has been rebirthed");
    }
  }

	public static void RefreshNation(int _nationID)
	{
		NationData nationData;
		NationTechData nationTechData;
		int i, j, techID, techCount;

		if (_nationID <= 0) {
			Output.PrintToScreen("Invalid nation ID: " + _nationID);
			return;
		}

		// Get the given nation's data
		nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationID, false);
		nationTechData = (NationTechData)DataManager.GetData(Constants.DT_NATIONTECH, _nationID, false);

		if ((nationData == null) || (nationTechData == null)) {
			Output.PrintToScreen("Data missing for nation " + _nationID);
			return;
		}

		// Record the nation's flags, to restore later.
		int flags = nationData.flags;

		try
		{
			// Get copies of the nation's tech_count array
			HashMap<Integer,Integer> nation_tech_count = (HashMap<Integer,Integer>)(nationTechData.tech_count.clone());

			// Record how many unused advance points the nation started with.
			int starting_advance_points = nationData.advance_points;

			// Reset the nation's advances and stats
			Gameplay.ResetAdvances(_nationID, true);

			TechData tech_data;
			Iterator it = nation_tech_count.entrySet().iterator();
			while (it.hasNext())
			{
				Map.Entry pairs = (Map.Entry)it.next();

				// Get the ID and count for the current tech to add
				techID = (Integer)(pairs.getKey());
				techCount = (Integer)(pairs.getValue());
				tech_data = TechData.GetTechData(techID);

				// Do not re-add landscape object techs; they've been re-added already by ResetAdvances().
				if ((tech_data == null) || (tech_data.duration_type == TechData.DURATION_OBJECT)) {
					continue;
				}

				// Do not re-add initial advances; they've been re-added already by ResetAdvances().
				if (tech_data.initial) {
					continue;
				}

				// Add the current tech to the nation, techCount times.
				for (j = 0; j < techCount; j++) {
					Technology.AddTechnology(_nationID, techID, 0, true, true, 0); // Update pending techs
				}
			}

			// Return all of the nation's advance points to the nation.
			nationData.advance_points = starting_advance_points;

			// Restore the nation's flags.
			nationData.SetFlags(flags);

			// Mark this nation's data to be updated
			DataManager.MarkForUpdate(nationData);
			DataManager.MarkForUpdate(nationTechData);

			Output.PrintToScreen("Refreshed nation " + nationData.name + " (" + _nationID + ")");

      //Constants.WriteToLog("log_MFU.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " "  + nationData.ID + " evt: RefreshNation\n");
		}
		catch (Exception e)
		{
			Output.PrintToScreen("Exception while adding techs to nation " + _nationID + ": " + e);
			Output.PrintException(e);
		}
	}

	public static void SyncLevelsToAdvances()
	{
		int nationID, levels, techID;
		NationData nationData;
		NationTechData nationTechData;
		TechData tech_data;

		Output.PrintToScreen("Syncing all nations' levels with their advances...");

		for (nationID = 1; nationID <= DataManager.GetHighestDataID(Constants.DT_NATION); nationID++)
		{
			if ((nationID / 1000.0f) == Math.floor(nationID / 1000.0f)) {
				Output.PrintToScreen("NationID: " + nationID);
			}

			// Get the given nation's data
			nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);
			nationTechData = (NationTechData)DataManager.GetData(Constants.DT_NATIONTECH, nationID, false);

			if ((nationData == null) || (nationTechData == null)) {
				continue;
			}

			levels = 1;
			Iterator it = nationTechData.tech_count.entrySet().iterator();
			while (it.hasNext())
			{
				Map.Entry pairs = (Map.Entry)it.next();

				// Get the ID and data for the current advance
				techID = (Integer)(pairs.getKey());
				tech_data = TechData.GetTechData(techID);

				// Keep count of how many of this nation's advances would have been acquired with advance points (leveling up).
				if ((tech_data != null) && (tech_data.duration_type == TechData.DURATION_PERMANENT) && (tech_data.default_price == 0) && (!tech_data.initial)) {
					levels++;
				}
			}

			// Add the nation's unspent advance_points to its levels count.
			levels += nationData.advance_points;

			if ((levels < 1) || (levels >= (Constants.NUM_LEVELS - 1))) {
				Output.PrintToScreen("Error: Nation " + nationData.name + " (" + nationID + ")'s correct level determined to be " + levels + ". They have " + nationData.advance_points + " advance points.");
			}

			// If level is incorrect...
			if (nationData.level != levels)
			{
				Output.PrintToScreen("Nation " + nationData.name + " (" + nationID + ") incorrectly has level " + nationData.level + " and " + nationData.xp + " XP.");

				// Set the nation's level and XP to the determined correct values.
				nationData.level = levels;
				nationData.xp = Math.min(Constants.MAX_XP, (Constants.XP_PER_LEVEL[levels - nationData.GetRebirthLevelBonus()] + Constants.XP_PER_LEVEL[levels + 1 - nationData.GetRebirthLevelBonus()]) / 2);

				Output.PrintToScreen("        Reset to level " + nationData.level + " and " + nationData.xp + " XP.");

				// Mark this nation's data to be updated
				DataManager.MarkForUpdate(nationData);
			}

			// If XP is incorrect for level...
			if ((nationData.xp < Constants.XP_PER_LEVEL[levels - nationData.GetRebirthLevelBonus()]) || (nationData.xp > Constants.XP_PER_LEVEL[levels + 1 - nationData.GetRebirthLevelBonus()]))
			{
				Output.PrintToScreen("Nation " + nationData.name + " (" + nationID + ") incorrectly has level " + nationData.level + " and " + nationData.xp + " XP.");

				// Set the nation's XP to the determined correct values.
				nationData.xp = Math.min(Constants.MAX_XP, (Constants.XP_PER_LEVEL[levels - nationData.GetRebirthLevelBonus()] + Constants.XP_PER_LEVEL[levels + 1 - nationData.GetRebirthLevelBonus()]) / 2);

				Output.PrintToScreen("        Reset to level " + nationData.level + " and " + nationData.xp + " XP.");

				// Mark this nation's data to be updated
				DataManager.MarkForUpdate(nationData);
			}
		}

		Output.PrintToScreen("Done syncing levels with advances.");
	}

  public static void ReloadNation(StringBuffer output_buffer, String nation_name, boolean console)
  {
	  int nationID = DataManager.GetNameToIDMap(Constants.DT_NATION, nation_name);
		NationData nation_data = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

		if (nation_data == null)
		{
      if(console)
      {
			  Output.PrintToScreen("Unknown nation: " + nation_name);
      }
      else
      {
		    Output.PrintToScreen("Admin Client Reload Nation Error - Unknown nation: " + nation_name);
  	    OutputEvents.GetMessageEvent(output_buffer, ClientString.Get("svr_msg_unknown_nation", "nation_name", nation_name)); // "Unknown nation: " + nation_name
      }
      return;
		}
		else
		{
			// Remove the nation's data from the cache
			DataManager.RemoveFromCache(Constants.DT_NATION, nationID);

			// Reload the nation's data
			nation_data = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

      if(console)
      {
			  Output.PrintToScreen("RELOAD_NATION " + ((nation_data == null) ? "failed." : "succeeded."));
      }
      else
      {
		    Output.PrintToScreen("Admin Client Reload Nation " + ((nation_data == null) ? "failed." : "succeeded."));
  	    OutputEvents.GetMessageEvent(output_buffer, ClientString.Get("Reload Nation"  + ((nation_data == null) ? "failed." : "succeeded.")));
      }
      return;
		}
  }

  public static void ReloadNationTechs(StringBuffer output_buffer, String nation_name, boolean console)
  {
		int nationID = DataManager.GetNameToIDMap(Constants.DT_NATION, nation_name);
		NationTechData nation_tech_data = (NationTechData)DataManager.GetData(Constants.DT_NATIONTECH, nationID, false);

		if (nation_tech_data == null)
		{
      if(console)
      {
			  Output.PrintToScreen("Unknown nationtech: '" + nation_name + "'");
      }
      else
      {
		    Output.PrintToScreen("Admin Client Reload Nation Tech Error - Unknown nationtech: " + nation_name);
  	    OutputEvents.GetMessageEvent(output_buffer, ClientString.Get("Unknown nationtech: " + nation_name));
      }
      return;
		}
		else
		{
			// Remove the nation's tech data from the cache
			DataManager.RemoveFromCache(Constants.DT_NATIONTECH, nationID);

			// Reload the nation's tech data
			nation_tech_data = (NationTechData)DataManager.GetData(Constants.DT_NATIONTECH, nationID, false);

      if(console)
      {
		  	Output.PrintToScreen("RELOAD_NATIONTECH " + ((nation_tech_data == null) ? "failed." : "succeeded."));
      }
      else
      {
		  	Output.PrintToScreen("Admin Client Reload Nation Techs " + ((nation_tech_data == null) ? "failed." : "succeeded."));
  	    OutputEvents.GetMessageEvent(output_buffer, ClientString.Get("Reload Nation Techs " + ((nation_tech_data == null) ? "failed." : "succeeded.")));
      }
      return;
		}
  }

  public static void ReloadUser(String user_name)
  {
    int userID = UserData.GetUserIDByUsername(user_name);

    if (userID <= 0)
    {
    	Output.PrintToScreen("Unknown user: '" + user_name + "', userID not found");
    }
    else
    {
    	UserData user_data = (UserData)DataManager.GetData(Constants.DT_USER, userID, false);

    	if (user_data == null)
    	{
    		Output.PrintToScreen("Unknown user: '" + user_name + "', data not found");
    	}
    	else
    	{
    		// Remove the user's data from the cache
    		DataManager.RemoveFromCache(Constants.DT_USER, userID);

    		// Reload the user's data
    		user_data = (UserData)DataManager.GetData(Constants.DT_USER, userID, false);

    		Output.PrintToScreen("RELOAD_USER " + ((user_data == null) ? "failed." : "succeeded."));
    	}
    }
  }

  public static void RenameNation(int _nationID, String new_name)
  {
		// If no new name is given, create default.
		if (new_name.equals("")) {
			new_name = "renamed" + _nationID;
		}

    int newNameID = DataManager.GetNameToIDMap(Constants.DT_NATION, new_name);

    if (newNameID > 0)
    {
    	Output.PrintToScreen("There's already a nation called '" + new_name + "'");
      return;
    }

    //int nationID = DataManager.GetNameToIDMap(Constants.DT_NATION, old_name);
    NationData nation_data = (NationData)DataManager.GetData(Constants.DT_NATION, _nationID, false);

    if ((_nationID == -1) || (nation_data == null))
    {
    	Output.PrintToScreen("Unknown nation with ID " + _nationID);
    }
    else
    {
    	// Change the name in the data
			String old_name = nation_data.name;
    	nation_data.name = new_name;

    	// Update the nation's data immediately, so new name can be looked up in DB.
    	DataManager.UpdateImmediately(nation_data);

    	// Output message
    	Output.PrintToScreen("Renamed nation " + old_name + " to " + new_name);
    }
  }

  public static void RenameUser(int _userID, String new_name)
  {
		// If no new name is given, create default.
		if (new_name.equals("")) {
			new_name = "renamed" + _userID;
		}

    int newNameID = UserData.GetUserIDByUsername(new_name);

    if (newNameID > 0)
    {
    	Output.PrintToScreen("There's already a user called '" + new_name + "'");
      return;
    }

    //int userID = UserData.GetUserIDByUsername(old_name);
    UserData user_data = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

    if ((_userID == -1) || (user_data == null))
    {
    	Output.PrintToScreen("Unknown user with ID " + _userID);
    }
    else
    {
			// Send an email to the renamed user's e-mail address.
			String body_string = "Your War of Conquest account named '" + user_data.name + "' has been renamed to '" + new_name + "'.\n\n" +
				"If you did not request this change, it may have been done because the name was deemed to violate War of Conquest's rules of conduct. In that case you can request a new name by sending an e-mail to contact@ironzog.com.\n\n" +
				"Thank you,\n" +
				"The War of Conquest team\n" +
				"\n";
			Constants.SendEmail("noreply@warofconquest.com", "War of Conquest", user_data.email, "Your War of Conquest account name has been changed", body_string);

			// Change the name in the user data immediately, so new name can be looked up in DB.
    	user_data.name = new_name;
    	DataManager.UpdateImmediately(user_data);

			// Change the name in the player account data
			PlayerAccountData player_account = AccountDB.ReadPlayerAccount(user_data.playerID);
			String old_name = player_account.username;
			player_account.username = new_name;
			AccountDB.WritePlayerAccount(player_account);

    	// Output message
    	Output.PrintToScreen("Renamed user " + old_name + " to " + new_name);
    }
  }

	public static void ResetPassword(String _username, String _new_password)
	{
		int playerID = AccountDB.GetPlayerIDByUsername(_username);

		if (playerID == -1)
		{
			Output.PrintToScreen("There is no player account with username '" + _username + "'.");
			return;
		}

		PlayerAccountData account = AccountDB.ReadPlayerAccount(playerID);

		if (account == null)
		{
			Output.PrintToScreen("Couldn't fetch player account data for ID " + playerID + ".");
			return;
		}

		// If no new password was given, generate one.
		if (_new_password.equals("")) {
			_new_password = Application.GeneratePassword();
		}

		// Record the new password's hash in the player account.
		account.passhash = AccountDB.DeterminePasswordHash(_new_password);

		// Store the modified player account data.
		AccountDB.WritePlayerAccount(account);

		// E-mail the account holder about the new password.
		Application.SendAccountInfoEmail(_username, _new_password, account.email);

		Output.PrintToScreen(_username + "'s password changed to '" + _new_password + "'.");
	}

	public static void ResetPrevLogout(String _username)
	{
		// Get the user data
		int userID = UserData.GetUserIDByUsername(_username);
    UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, userID, false);

		if (userData == null)
		{
			Output.PrintToScreen("No user data found for username '" + _username + "'.");
			return;
		}

		// Reset the user's prev_logout_time.
		userData.prev_logout_time = 0;

		// Mark the user's data to be updated.
		DataManager.MarkForUpdate(userData);
	}

	public static void RestoreDefaultPrices()
	{
		for (TechPriceRecord tech_price_record : GlobalData.instance.tech_price_records.values())
		{
			// Get the tech's data
			TechData techData = TechData.GetTechData(tech_price_record.ID);

			// Make sure tech data exists
			if (techData == null) {
				continue;
			}

			// Set the tech's price to its default.
			tech_price_record.price = techData.default_price;
		}

		// Mark the global data to be updated
		DataManager.MarkForUpdate(GlobalData.instance);

		// Broadcast a tech prices event to all clients in the game now.
		OutputEvents.BroadcastTechPricesEvent();

		Output.PrintToScreen("All prices set to defaults.");
	}

	public static void ReturnToHomeNations()
	{
		for (int userID = 1; userID <= DataManager.GetHighestDataID(Constants.DT_USER); userID++)
		{
			// Get the current user's data
			UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, userID, false);

			// If there is no user with the current userID, continue on to the next.
			if (userData == null) {
				continue;
			}

			if (userData.nationID != userData.home_nationID)
			{
				// Have the user join their home nation.
				Application.JoinNation(userData.ID, userData.home_nationID);
				Output.PrintToScreen("Returned '" + userData.name + "' to their home nation.");
			}
		}
	}

	public static boolean RunScript(String _filename)
	{
		int line_num = -1;
		BufferedReader br;

		try
		{
			br = new BufferedReader(new FileReader(_filename));
		}
		catch (Exception e)
		{
			Output.PrintToScreen("File not found: " + _filename + ", message: " + e.getMessage());
			return false;
		}

		try
		{
			String line;
			int[] place = new int[1];
			boolean success;

			while ((line = br.readLine()) != null)
			{
				// Increment line number.
				line_num++;

				// Skip comments
				if (line.charAt(0) == '#') {
					continue;
				}

				// Process the line.
				success = ProcessAdminCommand(line, null);

				if (!success)
				{
					Output.PrintToScreen("Command failed. Aborting script.");
					return false;
				}
			}

			Output.PrintToScreen("Finished running " + _filename + ".");
		}
		catch (Exception e)
		{
			Output.PrintToScreen("Error running " + _filename + " at line " + line_num);
			Output.PrintException(e);
			return false;
		}

		return true;
	}

	public static void AssociateUsers(String _username1, String _username2)
	{
		// Get the data for user 1
		int userID_1 = UserData.GetUserIDByUsername(_username1);
    UserData user_data_1 = (UserData)DataManager.GetData(Constants.DT_USER, userID_1, false);

		if (user_data_1 == null)
		{
			Output.PrintToScreen("No user data found for username '" + _username1 + "'.");
			return;
		}

		// Get the data for user 2
		int userID_2 = UserData.GetUserIDByUsername(_username2);
    UserData user_data_2 = (UserData)DataManager.GetData(Constants.DT_USER, userID_2, false);

		if (user_data_2 == null)
		{
			Output.PrintToScreen("No user data found for username '" + _username2 + "'.");
			return;
		}

		UserData cur_user_data;
		int cur_user_index;

		// Create list of user 1 and all of its associated users.
		ArrayList<UserData> users_1 = new ArrayList<UserData>();
		users_1.add(user_data_1);

		// Iterate through each of the user's associated users
		for (cur_user_index = 0; cur_user_index < user_data_1.associated_users.size(); cur_user_index++)
		{
			// Get the current associated user's data and add it to the users_1 list.
			cur_user_data = (UserData)DataManager.GetData(Constants.DT_USER, user_data_1.associated_users.get(cur_user_index), false);
			users_1.add(cur_user_data);
		}

		// Create list of user 2 and all of its associated users.
		ArrayList<UserData> users_2 = new ArrayList<UserData>();
		users_2.add(user_data_2);

		// Iterate through each of the user's associated users
		for (cur_user_index = 0; cur_user_index < user_data_2.associated_users.size(); cur_user_index++)
		{
			// Get the current associated user's data and add it to the users_2 list.
			cur_user_data = (UserData)DataManager.GetData(Constants.DT_USER, user_data_2.associated_users.get(cur_user_index), false);
			users_2.add(cur_user_data);
		}

		DeviceData cur_device_data;
		int cur_device_index;

		// Create list of all of user 1's associated devices.
		ArrayList<DeviceData> devices_1 = new ArrayList<DeviceData>();

		for(Iterator<Map.Entry<Integer, Integer>> it = user_data_1.devices.entrySet().iterator(); it.hasNext();)
		{
      Map.Entry<Integer, Integer> entry = it.next();
			cur_device_data = (DeviceData)DataManager.GetData(Constants.DT_DEVICE, entry.getKey(), false);

			if (cur_device_data != null) {
				devices_1.add(cur_device_data);
			}
		}

		// Create list of all of user 2's associated devices.
		ArrayList<DeviceData> devices_2 = new ArrayList<DeviceData>();

		for(Iterator<Map.Entry<Integer, Integer>> it = user_data_2.devices.entrySet().iterator(); it.hasNext();)
		{
      Map.Entry<Integer, Integer> entry = it.next();
			cur_device_data = (DeviceData)DataManager.GetData(Constants.DT_DEVICE, entry.getKey(), false);

			if (cur_device_data != null) {
				devices_2.add(cur_device_data);
			}
		}

		// Coassociate all users in users_1 and users_2 lists.
		for(UserData user_1_data : users_1)
		{
			for(UserData user_2_data : users_2)
			{
				if (user_1_data != user_2_data)
				{
					Output.PrintToScreen("Associating user '" + user_1_data.name + "' with '" + user_2_data.name + "'.");

					// Make sure that the user from list 1 is associated with the user from list 2.
					if (user_1_data.associated_users.contains(user_2_data.ID) == false) {
						user_1_data.associated_users.add(user_2_data.ID);
						DataManager.MarkForUpdate(user_1_data);
					}

					// Make sure that the user from list 2 is associated with the user from list 1.
					if (user_2_data.associated_users.contains(user_1_data.ID) == false) {
						user_2_data.associated_users.add(user_1_data.ID);
						DataManager.MarkForUpdate(user_2_data);
					}
				}
			}
		}

		// Coassociate all devices in devices_1 and devices_2 lists.
		for(DeviceData device_1_data : devices_1)
		{
			for(DeviceData device_2_data : devices_2)
			{
				if (device_1_data != device_2_data)
				{
					Output.PrintToScreen("Associating device '" + device_1_data.ID + "' with '" + device_2_data.ID + "'.");
					DeviceData.CoassociateDevices(device_1_data, device_2_data);
				}
			}
		}
	}

	public static void SeparateUsers(String _username1, String _username2)
	{
		// Get the data for user 1
		int userID_1 = UserData.GetUserIDByUsername(_username1);
    UserData user_data_1 = (UserData)DataManager.GetData(Constants.DT_USER, userID_1, false);

		if (user_data_1 == null)
		{
			Output.PrintToScreen("No user data found for username '" + _username1 + "'.");
			return;
		}

		// Get the data for user 2
		int userID_2 = UserData.GetUserIDByUsername(_username2);
    UserData user_data_2 = (UserData)DataManager.GetData(Constants.DT_USER, userID_2, false);

		if (user_data_2 == null)
		{
			Output.PrintToScreen("No user data found for username '" + _username2 + "'.");
			return;
		}

		if (user_data_1.email.equals(user_data_2.email))
		{
			Output.PrintToScreen("Cannot separate '" + _username1 + "' from '" + _username2 + "'; they share the same e-mail address.");
			return;
		}

		// For one user and all of its associated user, remove associated users that have the other user's e-mail address.
		DisassociateUsersWithEmail(user_data_1, user_data_2.email);
		DisassociateUsersWithEmail(user_data_2, user_data_1.email);

		// For each device associated with both usernames, disassociate it from the username with the fewest logins on that device.
		int deviceID, login_count_1, login_count_2;
		for(Iterator<Map.Entry<Integer, Integer>> it = user_data_1.devices.entrySet().iterator(); it.hasNext(); )
		{
      Map.Entry<Integer, Integer> entry = it.next();

			deviceID = entry.getKey();
			login_count_1 = entry.getValue();

			if (user_data_2.devices.containsKey(Integer.valueOf(deviceID)))
			{
				login_count_2 = user_data_2.devices.get(Integer.valueOf(deviceID));

				if (login_count_1 >= login_count_2)
				{
					// Disassociate this device from user 2.
					RemoveUserFromDevice(user_data_2, deviceID);

					// Disassociate this device from each of user 2's associated users.
					RemoveAssociatedUsersFromDevice(user_data_2, deviceID);
				}
				else
				{
					// Disassociate this device from user 1.
					RemoveUserFromDevice(user_data_1, deviceID);

					// Disassociate this device from each of user 1's associated users.
					RemoveAssociatedUsersFromDevice(user_data_1, deviceID);
				}
			}
    }
	}

	public static void DisassociateUsersWithEmail(UserData _userData, String _email)
	{
		// Remove from the given user, any associated users who have the given e-mail address.
		RemoveAssociatedUsersWithEmail(_userData, _email);

		// Iterate through the given user's associated users, removing from each of them any associated users with the given e-mail.
		for (int i = 0; i < _userData.associated_users.size(); i++)
		{
			UserData cur_assoc_user = (UserData)DataManager.GetData(Constants.DT_USER, _userData.associated_users.get(i), false);
			if (cur_assoc_user != null) {
				RemoveAssociatedUsersWithEmail(cur_assoc_user, _email);
			}
		}
	}

	public static void RemoveAssociatedUsersWithEmail(UserData _userData, String _email)
	{
		// Iterate through the given user's associated users...
		for (int i = 0; i < _userData.associated_users.size(); i++)
		{
			UserData cur_assoc_user = (UserData)DataManager.GetData(Constants.DT_USER, _userData.associated_users.get(i), false);

			// If the current associated user has the given email address...
			if ((cur_assoc_user != null) && (cur_assoc_user.email.equals(_email)))
			{
				// Mutually disassociate this user from its current associated user.
				cur_assoc_user.associated_users.remove(Integer.valueOf(_userData.ID));
				_userData.associated_users.remove(Integer.valueOf(cur_assoc_user.ID));

				// Mark both users data to be updated.
				DataManager.MarkForUpdate(cur_assoc_user);
				DataManager.MarkForUpdate(_userData);

				Output.PrintToScreen("Disassociated users " + _userData.name + " and " + cur_assoc_user.name + ".");

				// Decrement counter, so as to continue to next after removing the current.
				i--;
			}
		}
	}

	public static void RemoveUserFromDevice(UserData _userData, int _deviceID)
	{
		// Disassociate this device from the current associated user.
		DeviceData.RemoveUser(_deviceID, _userData.ID);
		_userData.devices.remove(Integer.valueOf(_deviceID));
		DataManager.MarkForUpdate(_userData);
		Output.PrintToScreen("Disassociated associated user " + _userData.name + " from device ID " + _deviceID);
	}

	public static void RemoveAssociatedUsersFromDevice(UserData _userData, int _deviceID)
	{
		for (int i = 0; i < _userData.associated_users.size(); i++)
		{
			UserData cur_assoc_user = (UserData)DataManager.GetData(Constants.DT_USER, _userData.associated_users.get(i), false);
			if (cur_assoc_user != null)
			{
				// Disassociate this device from the current associated user.
				RemoveUserFromDevice(cur_assoc_user, _deviceID);
			}
		}
	}

	public static void SeparateUserFromDevice(String _username, int _deviceID)
	{
		// Get the data for the user
		int userID = UserData.GetUserIDByUsername(_username);
    UserData user_data = (UserData)DataManager.GetData(Constants.DT_USER, userID, false);

		if (user_data == null)
		{
			Output.PrintToScreen("No user data found for username '" + _username + "'.");
			return;
		}

    DeviceData device_data = (DeviceData)DataManager.GetData(Constants.DT_DEVICE, _deviceID, false);

    if (device_data == null)
    {
      Output.PrintToScreen("There is no device data for ID " + _deviceID + ".");
      return;
    }

		// Disassociate this device from this user.
		RemoveUserFromDevice(user_data, _deviceID);

		// Disassociate this device from each of this user's associated users.
		RemoveAssociatedUsersFromDevice(user_data, _deviceID);
	}

	public static void SetEmail(String _username, String _new_email)
	{
		int playerID = AccountDB.GetPlayerIDByUsername(_username);

		if (playerID == -1)
		{
			Output.PrintToScreen("There is no player account with username '" + _username + "'.");
			return;
		}

		PlayerAccountData account = AccountDB.ReadPlayerAccount(playerID);

		if (account == null)
		{
			Output.PrintToScreen("Couldn't fetch player account data for ID " + playerID + ".");
			return;
		}

		// Record the new e-mail address in the player account.
		account.email = _new_email;

		// Store the modified player account data.
		AccountDB.WritePlayerAccount(account);

		// Propagate the change to the player's user data.
		int userID = UserData.GetUserIDByUsername(_username);
    UserData user_data = (UserData)DataManager.GetData(Constants.DT_USER, userID, false);
		if (user_data != null)
		{
			user_data.email = _new_email;
			DataManager.MarkForUpdate(user_data);
		}

		Output.PrintToScreen(_username + "'s e-mail address changed to '" + _new_email + "'.");
	}

	public static void SetShareFills(String _nation_name, int _amount)
  {
  	int nationID = DataManager.GetNameToIDMap(Constants.DT_NATION, _nation_name);
  	NationData nation_data = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

  	if (nation_data == null)
  	{
 		  Output.PrintToScreen("Unknown nation: " + _nation_name);
      return;
  	}
  	else
  	{
			float normalized_amount = (float)_amount / 100f;

			// Add the nation's share fill amount.
			nation_data.shared_energy_fill = Math.max(0, Math.min(1f, normalized_amount));
			nation_data.shared_manpower_fill = Math.max(0, Math.min(1f, normalized_amount));

			// Broadcast stats event to the nation.
			OutputEvents.BroadcastStatsEvent(nationID, 0);

			// Mark the nation's data to be updated
  		DataManager.MarkForUpdate(nation_data);

 		  Output.PrintToScreen(_nation_name + "'s share fills set to " + normalized_amount + ".");
      return;
		}
	}

	public static void SetPrice(int _techID, int _price)
	{
		// Get the given tech's TechPriceRecord
		TechPriceRecord tech_price_record = GlobalData.instance.GetTechPriceRecord(_techID, false);

		if (tech_price_record == null)
		{
			Output.PrintToScreen("No TechPriceRecord found for ID " + _techID);
			return;
		}

		// Set the tech's price
		tech_price_record.price = Math.max(0, _price);

		// Mark the global data to be updated
		DataManager.MarkForUpdate(GlobalData.instance);

		// Broadcast a tech prices event to all clients in the game now.
		OutputEvents.BroadcastTechPricesEvent();

		Output.PrintToScreen("Price set to " + tech_price_record.price);
	}

  public static void SetUserRank(String _username, int _rank)
  {
		if ((_rank < Constants.RANK_SOVEREIGN) || (_rank > Constants.RANK_CIVILIAN))
		{
			Output.PrintToScreen("Invalid rank: " + _rank);
			return;
		}

    int userID = UserData.GetUserIDByUsername(_username);

		UserData user_data = (UserData)DataManager.GetData(Constants.DT_USER, userID, false);

		if (user_data == null)
		{
			Output.PrintToScreen("Unknown user: '" + _username + "'");
			return;
		}

		// Set the user's rank and mark the data to be updated.
		user_data.rank = _rank;
		DataManager.MarkForUpdate(user_data);

		Output.PrintToScreen("Set " + _username + "'s rank to " + _rank + ".");
  }

  public static void SuspectNation(String _nation_name, int _minutes)
  {
		// Determine the nation's ID
		int targetNationID = DataManager.GetNameToIDMap(Constants.DT_NATION, _nation_name);

		if (targetNationID == -1)
		{
			Output.PrintToScreen("Unknown nation '" + _nation_name + "'");
			return;
		}

		WOCServer.NationRecord nation_record = WOCServer.nation_table.get(targetNationID);

		// Write this command to log
		Constants.WriteToLog("log_suspect.txt", "@SUSPECT Logging nation " + _nation_name + ", requested by admin for " + _minutes + " mins.\n");
		Constants.WriteToLog("log_attack.txt", "@SUSPECT Logging nation " + _nation_name + ", requested by admin for " + _minutes + " mins.\n");

		// Get the suspect nation's data
		NationData suspect_nationData = (NationData)DataManager.GetData(Constants.DT_NATION, targetNationID, false);

		if (suspect_nationData != null)
		{
			// Mark the suspect nation to be logged
			suspect_nationData.log_suspect_expire_time = Constants.GetFineTime() + (_minutes * 60000);
			suspect_nationData.log_suspect_init_nationID = -1;
		}

		if (nation_record != null)
		{
			// Mark each of the given nation's online users, to be logged
			for (Map.Entry<Integer,ClientThread> user_entry : nation_record.users.entrySet())
			{
				// Set the current ClientThread's log_suspect_expire_time
				((ClientThread)(user_entry.getValue())).log_suspect_expire_time = Constants.GetFineTime() + (_minutes * 60000);
			}
		}

		// Return confirmation message
		Output.PrintToScreen("Logging activity of nation '" + _nation_name + "' for " + _minutes + " minutes.");
  }

	public static void SetBlockType(LandMap _land_map, int _map_block_x, int _map_block_y, int _block_type, int _radius)
	{
		int x, y, nationID;
		int time = Constants.GetTime();
		ObjectData object_data;
		BlockData block_data;

		for (y = _map_block_y - _radius + 1; y < (_map_block_y + _radius); y++)
		{
			for (x = _map_block_x - _radius + 1; x < (_map_block_x + _radius); x++)
			{
				// Get the data for the current block, and mark it to be updated.
				block_data = DataManager.GetBlockData(_land_map, x, y, true);

				// Convert the given square to the given block type
				if ((_block_type >= 0) &&	(_block_type < Constants.NUM_TERRAIN_TYPES))
				{
					block_data.terrain = _block_type;
					World.SetBlockNationID(_land_map, x, y, _block_type, true, true, -1, 0);
				}
				else
				{
					// Get the block's nation ID
					nationID = block_data.nationID;

					// If not clearing the object, and object has no nationID yet
					if ((_block_type != 0) && (nationID == 0))
					{
						// First set the block's nationID to -1
						World.SetBlockNationID(_land_map, x, y, -1, true, true, -1, 0);
					}
				}
			}
		}
	}

	public static void ToggleAdmin(String _username)
	{
		// Get the user's data.
		int userID = UserData.GetUserIDByUsername(_username);
    UserData user_data = (UserData)DataManager.GetData(Constants.DT_USER, userID, false);

		if (user_data == null)
		{
			Output.PrintToScreen("There is no user named '" + _username + "'");
			return;
		}

		// Toggle the user's admin flag.
		user_data.admin = !user_data.admin;

		Output.PrintToScreen("User " + user_data.name + " (" + userID + ") " + (user_data.admin ? "is now an admin." : "is no longer an admin."));

		// Mark the user's data to be updated.
		DataManager.MarkForUpdate(user_data);
	}

	public static void ToggleAdminLoginOnly()
	{
		// Flip state of admin_login_only
		Constants.admin_login_only = !Constants.admin_login_only;

		// Return message
		if (Constants.admin_login_only) {
			Output.PrintToScreen("Log in now only allowed from admin IP address " + Constants.admin_login_ip + ".");
		} else {
			Output.PrintToScreen("Log in is now allowed from any IP address.");
		}
	}

	public static void ToggleVeteranStatus(String _username, String _nation_name)
	{
		// Get the user's data.
		int userID = UserData.GetUserIDByUsername(_username);
    UserData user_data = (UserData)DataManager.GetData(Constants.DT_USER, userID, false);

		if (user_data != null)
		{
			// Toggle the user's veteran status.
			user_data.veteran = !user_data.veteran;

			// Mark the user's data to be updated.
			DataManager.MarkForUpdate(user_data);

			// Print message.
			Output.PrintToScreen("User " + user_data.name + " (" + userID + ") " + (user_data.veteran ? "is now a veteran." : "is no longer a veteran."));

			return;
		}

		// Get the nation's data.
		int nationID = DataManager.GetNameToIDMap(Constants.DT_NATION, _nation_name);
    NationData nation_data = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

		if (nation_data != null)
		{
			// Toggle the nation's veteran status.
			nation_data.veteran = !nation_data.veteran;

			// Mark the nations's data to be updated.
			DataManager.MarkForUpdate(nation_data);

			// Print message.
			Output.PrintToScreen("Nation " + nation_data.name + " (" + nationID + ") " + (nation_data.veteran ? "is now a veteran." : "is no longer a veteran."));

			return;
		}

		Output.PrintToScreen("There is no user named '" + _username + "' or nation named '" + _nation_name + "'.");
	}

	public static void TrackClient(String _ID)
	{
		track_clientID = _ID.toLowerCase();

		if (track_clientID.length() == 0) {
			Output.PrintToScreen("Client tracking turned off.");
		} else {
			Output.PrintToScreen("Tracking clients with ID that includes '" + track_clientID + "'.");
		}
	}

  public static void TransferPlayerXP(String from_name, String to_name)
  {
    int fromID = UserData.GetUserIDByUsername(from_name);
    UserData from_data = (UserData)DataManager.GetData(Constants.DT_USER, fromID, false);


    int toID = UserData.GetUserIDByUsername(to_name);
    UserData to_data = (UserData)DataManager.GetData(Constants.DT_USER, toID, false);

    if (from_data == null)
    {
    	Output.PrintToScreen("Unknown user: '" + from_name + "'");
    }
    else if (to_data == null)
    {
    	Output.PrintToScreen("Unknown user: '" + to_name + "'");
    }
    else
    {
    	Output.PrintToScreen("Transfering " + from_data.xp + " XP from " + from_name + " to " + to_name);

    	// Transfer player points from the 'from' player to the 'to' player
    	to_data.xp += from_data.xp;
    	from_data.xp = 0;

    	// Mark both user's data to be updated
    	DataManager.MarkForUpdate(from_data);
    	DataManager.MarkForUpdate(to_data);
    }
  }

	public static void UnmuteAll(String _username)
	{
		// Get the user's data.
		int userID = UserData.GetUserIDByUsername(_username);
    UserData user_data = (UserData)DataManager.GetData(Constants.DT_USER, userID, false);

		if (user_data == null)
		{
			Output.PrintToScreen("There is no user named '" + _username + "'");
			return;
		}

		Output.PrintToScreen("For user " + _username + " unmuting " + user_data.muted_users.size() + " users and " + user_data.muted_devices.size() + " devices.");

		// Unmute all users and devices.
		Comm.UnmuteAll(user_data.ID);
	}

  public static void UpdateArea(String nation_name)
  {
    int nationID = DataManager.GetNameToIDMap(Constants.DT_NATION, nation_name);
    NationData nation_data = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

    if (nation_data == null)
    {
    	Output.PrintToScreen("Unknown nation: '" + nation_name + "'");
    }
    else
    {
    	Output.PrintToScreen("Pre-update (" + nation_name + "): mainland area: " + nation_data.mainland_footprint.area + ", ml border_area: " + nation_data.mainland_footprint.border_area + ", ml perimeter: " + nation_data.mainland_footprint.perimeter + ", mainland footprint: " + nation_data.mainland_footprint.x0 + "," + nation_data.mainland_footprint.y0 + " to " + nation_data.mainland_footprint.x1 + "," + nation_data.mainland_footprint.y1);

    	// Update the nation's area and perimeter in the mainland.
    	World.DetermineArea(DataManager.GetLandMap(Constants.MAINLAND_MAP_ID, false), nation_data);

			// Determine the nation's new base geographic efficiency.
			nation_data.DetermineGeographicEfficiency(Constants.MAINLAND_MAP_ID);

    	// Update the nation's area, perimeter and geographic efficiency in its homeland.
			if (nation_data.homeland_mapID != -1)
			{
				World.DetermineArea(DataManager.GetLandMap(nation_data.homeland_mapID, false), nation_data);
				nation_data.DetermineGeographicEfficiency(nation_data.homeland_mapID);
			}

    	Output.PrintToScreen("Post-update (" + nation_name + "): mainland area: " + nation_data.mainland_footprint.area + ", ml border_area: " + nation_data.mainland_footprint.border_area + ", ml perimeter: " + nation_data.mainland_footprint.perimeter + ", mainland footprint: " + nation_data.mainland_footprint.x0 + "," + nation_data.mainland_footprint.y0 + " to " + nation_data.mainland_footprint.x1 + "," + nation_data.mainland_footprint.y1);

    	// Mark the nation's data to be updated
    	DataManager.MarkForUpdate(nation_data);
      //Constants.WriteToLog("log_MFU.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " "  + nation_data.ID + " evt: UPDATE_AREA\n");
    }
  }

	public static void UpdateAllAreas()
	{
		BlockData block_data;
		int blockNationID, x, y, block_perimeter;
		NationData nationData;
		HashMap<Integer,Integer> originalAreas = new HashMap<Integer,Integer>();
		HashMap<Integer,Float> originalEnergyBurnRates = new HashMap<Integer,Float>();

		Output.PrintToScreen("UpdateAllAreas() begin");

		// Create ID for this process
		int processID = Constants.GetTime();

		// Get the land map
		LandMap land_map = DataManager.GetLandMap(Constants.MAINLAND_MAP_ID, false);

		for (y = 0; y < land_map.height; y++)
		{
			for (x = 0; x < land_map.width; x++)
			{
				block_data = land_map.GetBlockData(x, y);

				if (block_data.nationID != -1)
				{
					// Get the nation's data
					nationData = (NationData)DataManager.GetData(Constants.DT_NATION, block_data.nationID, false);

					if (nationData == null)
					{
						// The nation with this ID is missing its data. Clear the block.
						Output.PrintToScreen("Clearing block " + x + "," + y + " of nation " + block_data.nationID + " (nation data missing).");
						World.SetBlockNationID(land_map, x, y, -1, true, false, -1, 0);
					}
					else
					{
						if (nationData.processID != processID)
						{
							// This is the first time this nation is seen on the map.

							// Record this nation's original area.
							originalAreas.put(block_data.nationID, nationData.mainland_footprint.area);
							originalEnergyBurnRates.put(block_data.nationID, nationData.mainland_footprint.energy_burn_rate);

							// Reset the nation's energy burn rate.
							nationData.mainland_footprint.energy_burn_rate = 0;

							// Reset the nation's storage structure related data.
							nationData.shared_energy_capacity = 0;
							nationData.shared_manpower_capacity = 0;
							nationData.shared_energy_xp_per_hour = 0;
							nationData.shared_manpower_xp_per_hour = 0;
							nationData.num_share_builds = 0;

							// Reset the nation's footprint information.
							nationData.mainland_footprint.Reset();

							// Clear the nation's list of objects.
							nationData.objects.clear();

							// Clear the nation's list of build counts.
							nationData.builds.clear();

							// Record the process ID in this nation.
							nationData.processID = processID;
						}

						// Update the nation's area and other stats for the contents of this block that it owns.
						Gameplay.UpdateNationAreaForBlock(nationData, land_map, nationData.mainland_footprint, block_data, x, y);
					}
				}
			}
		}

		// Iterate though hash of all nations on the map...
		Iterator it = originalAreas.entrySet().iterator();
    while (it.hasNext())
		{
			Map.Entry<Integer, Integer> pair = (Map.Entry<Integer, Integer>)it.next();

			// Get the nation's data
			nationData = (NationData)DataManager.GetData(Constants.DT_NATION, pair.getKey(), false);

			// Refresh this nation's homeland area, if it has a homeland.
			if (nationData.homeland_mapID > 0) {
				Gameplay.RefreshAreaAndEnergyBurnRate(nationData, nationData.homeland_mapID);
			}

			// If the nation has no shared manpower or energy capacity, set the corresponding fill amount to 0.
			if (nationData.shared_manpower_capacity == 0) nationData.shared_manpower_fill = 0f;
			if (nationData.shared_energy_capacity == 0) nationData.shared_energy_fill = 0f;

			// Determine the nation's new base geographic efficiency.
			nationData.DetermineGeographicEfficiency(Constants.MAINLAND_MAP_ID);

			// Log which nations have had their area changed.
			if (nationData.mainland_footprint.area != pair.getValue()) {
				Output.PrintToScreen("Nation " + nationData.name + " (" + nationData.ID + ") area changed from " + pair.getValue() + " to " + nationData.mainland_footprint.area);
			}

			// Log which nations have had their energy burn rate changed.
			if (Math.abs(nationData.mainland_footprint.energy_burn_rate - originalEnergyBurnRates.get(nationData.ID)) > 0.01f) {
				Output.PrintToScreen("Nation " + nationData.name + " (" + nationData.ID + ") energy burn rate changed from " + originalEnergyBurnRates.get(nationData.ID) + " to " + nationData.mainland_footprint.energy_burn_rate);
			}

			// Mark this nation to be updated to the DB
			DataManager.MarkForUpdate(nationData);
    }

		// Determine highest nation ID
		int highestNationID = DataManager.GetHighestDataID(Constants.DT_NATION);

		// Iterate through each nation
		for (int curNationID = 1; curNationID <= highestNationID; curNationID++)
		{
			// Get the data for the nation with the current ID
			nationData = (NationData)DataManager.GetData(Constants.DT_NATION, curNationID, false);

			// If no nation exists with this ID, continue to next.
			if (nationData == null) {
				continue;
			}

			if ((nationData.processID != processID) && (nationData.mainland_footprint.area != 0))
			{
				Output.PrintToScreen("Nation " + nationData.name + " (" + nationData.ID + ") not on map but its mainland area is " + nationData.mainland_footprint.area + ". Correcting to 0 area.");

				// This nation is not on the map, but its area is not 0. Reset its area to 0 and log the error.
				nationData.mainland_footprint.area = 0;
				nationData.mainland_footprint.border_area = 0;
				nationData.mainland_footprint.perimeter = 0;
				nationData.mainland_footprint.x0 = Constants.MAX_MAP_DIM;
				nationData.mainland_footprint.x1 = 0;
				nationData.mainland_footprint.y0 = Constants.MAX_MAP_DIM;
				nationData.mainland_footprint.y1 = 0;

				// Mark this nation to be updated to the DB
				DataManager.MarkForUpdate(nationData);
			}
		}

		Output.PrintToScreen("UpdateAllAreas() end");
	}

	public static void ConfineNations()
	{
		int blockNationID, x, y;
		NationData nationData;

		Output.PrintToScreen("ConfineNations() begin");

		// Get the land map
		LandMap land_map = DataManager.GetLandMap(Constants.MAINLAND_MAP_ID, false);

		for (y = 0; y <= land_map.height; y++)
		{
			for (x = 0; x <= land_map.width; x++)
			{
				blockNationID = land_map.GetBlockNationID(x, y);

				if (blockNationID != -1)
				{
					// Get the nation's data
					nationData = (NationData)DataManager.GetData(Constants.DT_NATION, blockNationID, false);

					if (nationData == null)
					{
						// The nation with this ID is missing its data. Clear the block.
						Output.PrintToScreen("Clearing block " + x + "," + y + " of nation " + blockNationID + " (nation data missing).");
						World.SetBlockNationID(land_map, x, y, -1, true, false, -1, 0);
					}
					else
					{
						int western_limit = land_map.MaxLevelLimitToPosX(nationData.level);

						// If this block is beyond the nation's limit to the west, remove the nation from this block.
						if (x < western_limit)
						{
							Output.PrintToScreen("Clearing block " + x + "," + y + " of nation " + blockNationID + " (Western limit is " + western_limit + ").");
							World.SetBlockNationID(land_map, x, y, -1, true, false, -1, 0);
						}
					}
				}
			}
		}

		Output.PrintToScreen("ConfineNations() end");
	}

  public static void UpdateNationRanks(String nation_name)
  {
    int nationID = DataManager.GetNameToIDMap(Constants.DT_NATION, nation_name);
    NationData nation_data = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

    if (nation_data == null)
    {
    	Output.PrintToScreen("Unknown nation: '" + nation_name + "'");
    }
    else
    {
      // Update nation's various ranks position.
			RanksData.instance.ranks_nation_level.UpdateRanks(nationID, nation_data.name, nation_data.level, Constants.NUM_LEVEL_RANKS, false);
			RanksData.instance.ranks_nation_rebirths.UpdateRanks(nationID, nation_data.name, nation_data.rebirth_count, Constants.NUM_REBIRTHS_RANKS, false);
			RanksData.instance.ranks_nation_xp.UpdateRanks(nationID, nation_data.name, nation_data.xp_history, Constants.NUM_XP_RANKS, false);
			RanksData.instance.ranks_nation_xp_monthly.UpdateRanks(nationID, nation_data.name, nation_data.xp_history_monthly, Constants.NUM_XP_RANKS, false);
			RanksData.instance.ranks_nation_winnings.UpdateRanks(nationID, nation_data.name, nation_data.prize_money_history, Constants.NUM_GLOBAL_PRIZE_RANKS, true);
			RanksData.instance.ranks_nation_winnings_monthly.UpdateRanks(nationID, nation_data.name, nation_data.prize_money_history_monthly, Constants.NUM_GLOBAL_PRIZE_RANKS, true);
			RanksData.instance.ranks_nation_tournament_trophies.UpdateRanks(nationID, nation_data.name, nation_data.tournament_trophies_history, Constants.NUM_GLOBAL_PRIZE_RANKS, false);
			RanksData.instance.ranks_nation_tournament_trophies_monthly.UpdateRanks(nationID, nation_data.name, nation_data.tournament_trophies_history_monthly, Constants.NUM_GLOBAL_PRIZE_RANKS, false);
			RanksData.instance.ranks_nation_energy_donated.UpdateRanks(nationID, nation_data.name, nation_data.donated_energy_history, Constants.NUM_ENERGY_DONATED_RANKS, false);
			RanksData.instance.ranks_nation_energy_donated_monthly.UpdateRanks(nationID, nation_data.name, nation_data.donated_energy_history_monthly, Constants.NUM_ENERGY_DONATED_RANKS, false);
			RanksData.instance.ranks_nation_manpower_donated.UpdateRanks(nationID, nation_data.name, nation_data.donated_manpower_history, Constants.NUM_MANPOWER_DONATED_RANKS, false);
			RanksData.instance.ranks_nation_manpower_donated_monthly.UpdateRanks(nationID, nation_data.name, nation_data.donated_manpower_history_monthly, Constants.NUM_MANPOWER_DONATED_RANKS, false);
			RanksData.instance.ranks_nation_quests.UpdateRanks(nationID, nation_data.name, nation_data.quests_completed, Constants.NUM_QUESTS_COMPLETED_RANKS, false);
			RanksData.instance.ranks_nation_quests_monthly.UpdateRanks(nationID, nation_data.name, nation_data.quests_completed_monthly, Constants.NUM_QUESTS_COMPLETED_RANKS, false);
			RanksData.instance.ranks_nation_captures.UpdateRanks(nationID, nation_data.name, nation_data.captures_history, Constants.NUM_CAPTURES_RANKS, false);
			RanksData.instance.ranks_nation_captures_monthly.UpdateRanks(nationID, nation_data.name, nation_data.captures_history_monthly, Constants.NUM_CAPTURES_RANKS, false);
			RanksData.instance.ranks_nation_area.UpdateRanks(nationID, nation_data.name, nation_data.mainland_footprint.area, Constants.NUM_AREA_RANKS, false);
			RanksData.instance.ranks_nation_area_monthly.UpdateRanks(nationID, nation_data.name, nation_data.mainland_footprint.area, Constants.NUM_AREA_RANKS, false);
			RanksData.instance.ranks_nation_medals.UpdateRanks(nationID, nation_data.name, nation_data.medals_history, Constants.NUM_MEDALS_RANKS, false);
			RanksData.instance.ranks_nation_medals_monthly.UpdateRanks(nationID, nation_data.name, nation_data.medals_history_monthly, Constants.NUM_MEDALS_RANKS, false);
			RanksData.instance.ranks_nation_raid_earnings.UpdateRanks(nationID, nation_data.name, nation_data.raid_earnings_history, Constants.NUM_GLOBAL_PRIZE_RANKS, false);
			RanksData.instance.ranks_nation_raid_earnings_monthly.UpdateRanks(nationID, nation_data.name, nation_data.raid_earnings_history_monthly, Constants.NUM_GLOBAL_PRIZE_RANKS, false);
			RanksData.instance.ranks_nation_orb_shard_earnings.UpdateRanks(nationID, nation_data.name, nation_data.orb_shard_earnings_history, Constants.NUM_GLOBAL_PRIZE_RANKS, false);
			RanksData.instance.ranks_nation_orb_shard_earnings_monthly.UpdateRanks(nationID, nation_data.name, nation_data.orb_shard_earnings_history_monthly, Constants.NUM_GLOBAL_PRIZE_RANKS, false);

    	// Mark the ranks data to be updated
    	DataManager.MarkForUpdate(RanksData.instance);

      Output.PrintToScreen("Ranks updated for nation '" + nation_data.name + "'");
    }
  }

	public static void DemoPopulate()
	{
		//String[] demo_names = {"32 in 1","3-D Tic-Tac-Toe","Acid Drop","Action Pak","Actionauts","Decathlon","Adventure","Adventures of Tron","Air Raid","Air Raiders","Airlock","Air-Sea Battle","Alien","Alien's Return","Alpha Beam with Ernie","Amidar","Armor Ambush","Artillery Duel","Assault","Asterix","Asteroids","Astroblast","Atari Video Cube","Atlantis","Atlantis II","Bachelor Party","Bachelorette Party","Back To School Pak","Backgammon","Bank Heist","Barnstorming","Fun With Numbers","Basic Programming","Basketball","Battlezone","Beamrider","Beany Bopper","Beat' Em & Eat 'Em","Berenstain Bears","Bermuda Triangle","Berzerk","Egg Catch","Birthday Mania","Blackjack","Blue Print","BMX Airmaster","Bobby Is Going Home","Boing!","Bowling","Boxing","Brain Games","Breakout","Bridge","Bugs","Bump 'n' Jump","Bumper Bash","BurgerTime","Burning Desire","Busy Police","Cakewalk","California Games","Canyon Bomber","Carnival","Casino","Cathouse Blues","Centipede","Challenge","Challenge of Nexar","Chase the Chuck Wagon","Checkers","China Syndrome","Chopper Command","Chuck Norris Superkicks","Circus Atari","Coconuts","Codebreaker","Combat","Commando","Commando Raid","Communist Mutants from Space","Concentration","Condor Attack","Confrontation","Congo Bongo","Cookie Monster Munch","Cosmic Ark","Cosmic Commuter","Cosmic Corridor","Cosmic Creeps","Cosmic Free Fire","Cosmic Swarm","Crab Control","Crackpots","Crash Dive","Crazy Climber","Cross Force","Crossbow","Cruise Missile","Crypts of Chaos","Crystal Castles","Cubicolor","Custer's Revenge","Dancing Plate","Dark Cavern","Dark Chambers","Deadly Discs","Deadly Duck","Death Star Battle","Death Trap","Defender","Defender II","Demolition Herby","Demon Attack","Demons to Diamonds","Desert Falcon","Diagnostic Cartridge","Dice Puzzle","Dig Dug","Dishaster","Dodge 'Em","Dolphin","Donkey Kong","Donkey Kong Junior","Double Dragon","Double Dunk","Dragon Treasure","Dragonfire","Dragonstomper","Dragster","Dumbo's Flying Circus","E.T. the Extra-Terrestrial","Earth Attack","Earth Dies Screaming","Earthworld","Eddy Langfinger","Eggomania","Eli's Ladder","Encounter at L-5","Enduro","Entombed","Escape From The Mindmaster","Espial","Exocet","Exocet Missile","Extra Terrestrials","Fantastic Voyage","Farmer Dan","Fast Eddie","Fast Food","Fatal Run","Fathom","Fighter Pilot","Final Approach","Fire Fighter","Fire Fly","Fireball","Fireworld","Fisher Price","Fishing Derby","Flag Capture","Flash Gordon","Football","Frankenstein's Monster","Freeway","Frog Pond","Frogger","Frogs and Flies","Front Line","Frostbite","G.I. Joe: Cobra Strike","Galaxian","Gamma-Attack","Gangster Alley","Gas Hog","Gauntlet","Ghost Manor","Ghostbusters","Ghostbusters II","Gigolo","Glacier Patrol","Glib","Golf","Gopher","Gorf","Grand Prix","Gravitar","Great Escape","Gremlins","Guardian","Gyruss","H.E.R.O.","Halloween","Hangman","Harbor Escape","Haunted House","Home Run","Human Cannonball","Hunt & Score","I Want My Mommy","Ice Hockey","Ikari Warriors","Inca Gold","Indy 500","Infiltrate","International Soccer","James Bond 007","Jawbreaker","Jedi Arena","Journey Escape","Joust","Jr. Pac-Man","Jungle Fever","Jungle Hunt","Kaboom!","Kangaroo","Karate","Keystone Kapers","Killer Satellites","King Kong","Klax","Knight on the Town","Kool-Aid Man","Krull","Kung-Fu Master","Lady in Wading","Laser Blast","Laser Gates","Laser Volley","Lochjaw","Lock 'n' Chase","London Blitz","Lost Luggage","M*A*S*H","M.A.D.","MagiCard","Malagai","Mangia","Marauder","Marine Wars","Mario Bros.","Master Builder","Masters of the Universe","Math Gran Prix","Maze Craze","Mega Force","MegaBoy","Megamania","Midnight Magic","Millipede","Miner 2049er","Miner 2049er II","Mines of Minos","Miniature Golf","Missile Command","Missile Control","Mission 3000 A.D.","Mission Survive","Mogul Maniac","Montezuma's Revenge","Moon Patrol","Moonsweeper","Motocross","Motocross Racer","MotoRodeo","Mountain King","Mouse Trap","Mr. Do!","Mr. Do's Castle","Mr. Postman","Ms. Pac-Man","Music Machine","My Golf","Name This Game","Night Driver","Night Stalker","Nightmare","No Escape!","Nuts","Obelix","Ocean City Defender","Off the Wall","Off Your Rocker","Official Frogger","Oink!","Omega Race","Open Sesame","Oscar's Trash Race","Othello","Out of Control","Outlaw","Pac-Kong","Pac-Man","Panda Chase","Parachute","Party Mix","Pelé's Soccer","Pengo","Pepsi Invaders","Pete Rose Baseball","Phantom Tank","Phantom-Panzer","Phaser Patrol","Philly Flasher","Phoenix","Pick 'n Pile","Pick Up","Picnic","Piece o' Cake","Pigs in Space","Pinball","Piraten-Schiff","Pitfall II: Lost Caverns","Pitfall!","Planet of Zoom","Planet Patrol","Planeten Patrouile","Plaque Attack","Polaris","Pole Position","Polo","Pooyan","Popeye","Porky's","Pressure Cooker","Private Eye","Pyramid War","Q*bert","Q*bert's Qubes","Quadrun","Quest for Quintana Roo","Quick Step","Rabbit Transit","Racing Pak","Racquetball","Radar","Radar Lock","Raft Rider","Raiders of the Lost Ark","Ram It","Rampage","Reactor","RealSports Baseball","RealSports Boxing","RealSports Football","RealSports Soccer","RealSports Tennis","RealSports Volleyball","Red Sea Crossing","Rescue in Gargamel's Castle","Rescue Terra 1","Revenge of the Beefsteak Tomatoes","Riddle of the Sphinx","River Patrol","River Raid","River Raid II","Road Runner","Robin Hood","Robot Commando Raid","Robot Tank","Roc 'N Rope","Room of Doom","Rubik's Cube","Save Our Ship","Save the Whales","Scraper Caper","Scuba Diver","Sea Hawk","Seamonster","Seaquest","Secret Quest","Sentinel","Shark Attack","Shootin' Gallery","Shuttle Orbiter","Signal Tracing Cartridge","Sir Lancelot","Skate Boardin'","Skeet Shoot","Skiing","Sky Diver","Sky Jinks","Sky Skipper","Slot Machine","Slot Racers","Smurfs Save the Day","Snail Against Squirrel","Sneak 'N Peek","Snoopy and the Red Baron","Solar Fox","Solar Storm","Solaris","Sorcerer","Sorcerer's Apprentice","Space Adventure","Space Attack","Space Canyon","Space Cavern","Space Grid","Space Invaders","Space Jockey","Space Shuttle","Space War","Spacechase","Spacemaster X-7","Spider Fighter","Spider Maze","Spiderdroid","Spider-Man","Spike's Peak","Spitfire Attack","Springer","Sprintmaster","Spy Hunter","Squeeze Box","Sssnake","Stampede","Star Fox","Star Raiders","Star Ship","Star Strike","Star Voyager","Star Wars: The Arcade Game","Stargate","Stargunner","Starmaster","Steeplechase","Stellar Track","Strategic Operations Simulator","Strategy X","Strawberry Shortcake","Street Racer","Stronghold","Stunt Man","Submarine Commander","Sub-Scan","Subterranea","Suicide Mission","Summer Games","Super Baseball","Super Baumeister","Super Breakout","Super Challenge Baseball","Super Challenge Football","Super Cobra","Super Football","Superman","Surfer's Paradise","Surround","Survival Island","Survival Run","Sword of Saros","Tac-Scan","Tank Brigade","Tank City","Tanks But No Tanks","Tapeworm","Tapper","Task Force","Tax Avoiders","Taz","Tennis","Texas Chainsaw Massacre","The Empire Strikes Back","Threeedeep!","Threshold","Thunderground","Time Pilot","Time Warp","Title Match","Tomarc The Barbarian","Tomcat","Tooth Protectors","Towering Inferno","Track & Field","Treasure Below","Trick Shot","Tron: Deadly Discs","Tunnel Runner","Turmoil","Tutankham","Universal Chaos","Up'n Down","Vanguard","Venture","Video Checkers","Video Chess","Video Jogger","Video Life","Video Olympics","Video Pinball","Video Reflex","Vulture Attack","Wabbit","Wall Ball","Wall Defender","War Zone","Warlords","Warplock","Waterworld","Weltraumtunnel","Wing War","Winter Games","Wizard of Wor","Word Zapper","Worm War I","Xenophobe","X-man","Yars' Revenge","Zaxxon","Zoo Fun","Z-Tack"};
		NationData nationData;
		NationTechData nationTechData;
		UserData userData;
		BlockData blockData, blockNData, blockSData, blockEData, blockWData;
		BlockExtData blockExtData;
		BuildData buildData, defense0, defense1, defense2, defense3;
		BuildData[] bestBuild = new BuildData[BuildData.NUM_TYPES];
		int i, userID, nationID, x, y, xp, yp, blockTerrain, blockNationID, selfSquares;
		int[] bestBuildHP = new int[BuildData.NUM_TYPES];
		LandMap mainland_map = DataManager.GetLandMap(Constants.MAINLAND_MAP_ID, false);

		String[] demo_names = new String[100];
		for (i = 0; i < 100; i++) {
			demo_names[i] = "Feeder-" + i;
		}

/*
		// TEMP -- remove structures in blocks not occupied by the structure's owner.
		for (y = 0; y < mainland_map.height; y++)
		{
			for (x = 0; x < mainland_map.width; x++)
			{
				blockExtData = mainland_map.GetBlockExtendedData(x, y, false);
				if ((blockExtData != null) && (blockExtData.objectID != -1) && (blockExtData.objectID < ObjectData.RESOURCE_OBJECT_BASE_ID) && (blockExtData.owner_nationID != mainland_map.GetBlockNationID(x, y)))
				{
					blockExtData.objectID = -1;
					DataManager.MarkBlockForUpdate(mainland_map, x, y);
				}
			}
		}
*/
		// Create a random generator with a seed, so its results can be reproduced.
		Random rand = new Random(865875);

		// Create the demo nations.
		CreateDemoNations(demo_names, rand, false);

		// Place each demo nation on the map.
		for (String cur_name : demo_names)
		{
			// Get the data for the nation with the current demo name.
			nationID = NationData.GetNationIDByNationName(cur_name);
			nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);
			nationTechData = (NationTechData)DataManager.GetData(Constants.DT_NATIONTECH, nationID, false);

			if (nationData != null)
			{
				// Determine the BuildData of this nation's build of each type, with the highest hit points.
				int max_hit_points = 0;
				for (i = 0; i < BuildData.NUM_TYPES; i++)
				{
					bestBuild[i] = null;
					bestBuildHP[i] = 0;

					for (int j = 0; j <= 150; j++)
					{
						buildData = BuildData.GetBuildData(j);

						if ((buildData != null) && (buildData.type == i) && (buildData.hit_points >= bestBuildHP[i]) && nationTechData.available_builds.containsKey(j) && (nationTechData.available_builds.get(j) != false))
						{
							bestBuild[i] = buildData;
							bestBuildHP[i] = buildData.hit_points;
						}
					}

					//Output.PrintToScreen(cur_name + " bestBuild[" + i + "]: " + ((bestBuild[i] == null) ? "none" : bestBuild[i].name));
				}

				defense0 = (bestBuild[BuildData.TYPE_AREA_EFFECT] != null) ? bestBuild[BuildData.TYPE_AREA_EFFECT] : ((bestBuild[BuildData.TYPE_SPLASH] != null) ? bestBuild[BuildData.TYPE_SPLASH] : bestBuild[BuildData.TYPE_DIRECTED_MULTIPLE]);
				defense1 = (bestBuild[BuildData.TYPE_COUNTER_ATTACK] != null) ? bestBuild[BuildData.TYPE_COUNTER_ATTACK] : ((bestBuild[BuildData.TYPE_AREA_FORTIFICATION] != null) ? bestBuild[BuildData.TYPE_AREA_FORTIFICATION] : ((bestBuild[BuildData.TYPE_SPECIFIC_LASTING_WIPE] != null) ? bestBuild[BuildData.TYPE_SPECIFIC_LASTING_WIPE] : defense0));
				defense2 = (bestBuild[BuildData.TYPE_WIPE] != null) ? bestBuild[BuildData.TYPE_WIPE] : ((bestBuild[BuildData.TYPE_TOWER_BUSTER] != null) ? bestBuild[BuildData.TYPE_TOWER_BUSTER] : ((bestBuild[BuildData.TYPE_GENERAL_LASTING_WIPE] != null) ? bestBuild[BuildData.TYPE_GENERAL_LASTING_WIPE] : defense1));
				defense3 = (bestBuild[BuildData.TYPE_AIR_DROP] != null) ? bestBuild[BuildData.TYPE_AIR_DROP] : ((bestBuild[BuildData.TYPE_RECAPTURE] != null) ? bestBuild[BuildData.TYPE_RECAPTURE] : defense2);

				//Output.PrintToScreen(cur_name + " defense0: " + ((defense0 == null) ? "none" : defense0.name));
				//Output.PrintToScreen(cur_name + " defense1: " + ((defense1 == null) ? "none" : defense1.name));
				//Output.PrintToScreen(cur_name + " defense2: " + ((defense2 == null) ? "none" : defense2.name));
				//Output.PrintToScreen(cur_name + " defense3: " + ((defense3 == null) ? "none" : defense3.name));

				// Determine starting location of this nation.
				int centerX = mainland_map.MaxLevelLimitToPosX(nationData.level) - 10;
				int centerY = (int)(mainland_map.height * 0.3) + rand.nextInt((int)(mainland_map.height * 0.6)); // Lower 2/3 of map, so some is in vet area as well.
				//Output.PrintToScreen("mainland_map.height: " + mainland_map.height + ", centerY: " + centerY); // TESTING

				// Migrate the nation to its determined starting location.
				World.MigrateNation(mainland_map, nationID, centerX, centerY, true, -1, true); // Set admin flag so that nation position will not be restricted by its veteran status.

				// If the nation could not be placed, skip it.
				if (nationData.mainland_footprint.area == 0) {
					continue;
				}

				//Output.PrintToScreen("Before expanding " + cur_name + ", area: " + nationData.mainland_footprint.area + ", x0: " + nationData.mainland_footprint.x0 + ", x1: " + nationData.mainland_footprint.x1 + ", y0: " + nationData.mainland_footprint.y0 + ", y1: " + nationData.mainland_footprint.y1);

				// Loop to expand the nation
				for (i = 0; i < 50; i++)
				{
					// Determine point around which we will try to expand the nation.
					int expand_x = 0, expand_y = 0;
					switch (rand.nextInt(4))
					{
						case 0:
							// Left edge
							expand_x = nationData.mainland_footprint.x0;
							expand_y = nationData.mainland_footprint.y0 + rand.nextInt(nationData.mainland_footprint.y1 - nationData.mainland_footprint.y0 + 1);
							break;
						case 1:
							// Right edge
							expand_x = nationData.mainland_footprint.x1;
							expand_y = nationData.mainland_footprint.y0 + rand.nextInt(nationData.mainland_footprint.y1 - nationData.mainland_footprint.y0 + 1);
							break;
						case 2:
							// Top edge
							expand_y = nationData.mainland_footprint.y0;
							expand_x = nationData.mainland_footprint.x0 + rand.nextInt(nationData.mainland_footprint.x1 - nationData.mainland_footprint.x0 + 1);
							break;
						case 3:
							// Bottom edge
							expand_y = nationData.mainland_footprint.y1;
							expand_x = nationData.mainland_footprint.x0 + rand.nextInt(nationData.mainland_footprint.x1 - nationData.mainland_footprint.x0 + 1);
							break;
					}

					int eligibleSquares = 0;
					selfSquares = 0;
					for (y = expand_y - 2; y <= expand_y + 2; y++)
					{
						for (x = expand_x - 2; x <= expand_x + 2; x++)
						{
							// Get the block's terrain type
							blockTerrain = mainland_map.GetBlockTerrain(x, y);

							// Skip the block if it is not habitable terrain
							if ((blockTerrain != Constants.TERRAIN_FLAT_LAND) && (blockTerrain != Constants.TERRAIN_BEACH)) {
								continue;
							}

							// Get the block's nation ID
							blockNationID = mainland_map.GetBlockNationID(x, y);

							// Skip the block if it is occupied by another nation.
							if ((blockNationID != nationID) && (blockNationID != -1)) {
								continue;
							}

							if (blockNationID == nationID) {
								selfSquares++;
							}

							// Increment the count of the number of squares in the potential expansion area that are eligible to be expanded into.
							eligibleSquares++;
						}
					}

					//Output.PrintToScreen("Nation " + cur_name + ": expand_x: " + expand_x + ", expand_y: " + expand_y + ", eligibleSquares: " + eligibleSquares + ", selfSquares: " + selfSquares);

					// If the count of eligible squares is high enough...
					if ((eligibleSquares >= 23) && (selfSquares >= 1))
					{
						// Occupy all empty habitable squares in the expansion area.
						for (y = expand_y - 2; y <= expand_y + 2; y++)
						{
							for (x = expand_x - 2; x <= expand_x + 2; x++)
							{
								// Get the block's terrain type
								blockTerrain = mainland_map.GetBlockTerrain(x, y);

								// Skip the block if it is not habitable terrain
								if ((blockTerrain != Constants.TERRAIN_FLAT_LAND) && (blockTerrain != Constants.TERRAIN_BEACH)) {
									continue;
								}

								// Get the block's nation ID
								blockNationID = mainland_map.GetBlockNationID(x, y);

								// Skip the block if it is already occupied by a nation.
								if (blockNationID != -1) {
									continue;
								}

								// Occupy the current square.
								World.SetBlockNationID(mainland_map, x, y, nationID, true, false, -1, 0);
							}
						}
					}
				}

				// For any resources or orbs within the nation's bounding box, that are occupied by this nation or by no nation, occupy it and its surrounding squares.
				for (y = nationData.mainland_footprint.y0 - 4; y <= nationData.mainland_footprint.y1 + 4; y++)
				{
					for (x = nationData.mainland_footprint.x0 - 4; x <= nationData.mainland_footprint.x1 + 4; x++)
					{
						blockData = mainland_map.GetBlockData(x, y);
						blockExtData = mainland_map.GetBlockExtendedData(x, y, false);

						if ((blockData != null) && ((blockData.nationID == -1) || (blockData.nationID == nationID)) && (blockExtData != null) && (blockExtData.objectID >= ObjectData.RESOURCE_OBJECT_BASE_ID))
						{
							if ((mainland_map.GetBlockNationID(x-1, y-1) == -1) && mainland_map.BlockIsHabitable(x-1, y-1)) World.SetBlockNationID(mainland_map, x-1, y-1, nationID, true, false, -1, 0);
							if ((mainland_map.GetBlockNationID(x-1, y) == -1) && mainland_map.BlockIsHabitable(x-1, y)) World.SetBlockNationID(mainland_map, x-1, y, nationID, true, false, -1, 0);
							if ((mainland_map.GetBlockNationID(x-1, y+1) == -1) && mainland_map.BlockIsHabitable(x-1, y+1)) World.SetBlockNationID(mainland_map, x-1, y+1, nationID, true, false, -1, 0);
							if ((mainland_map.GetBlockNationID(x, y-1) == -1) && mainland_map.BlockIsHabitable(x, y-1)) World.SetBlockNationID(mainland_map, x, y-1, nationID, true, false, -1, 0);
							if ((mainland_map.GetBlockNationID(x, y) == -1) && mainland_map.BlockIsHabitable(x, y)) World.SetBlockNationID(mainland_map, x, y, nationID, true, false, -1, 0);
							if ((mainland_map.GetBlockNationID(x, y+1) == -1) && mainland_map.BlockIsHabitable(x, y+1)) World.SetBlockNationID(mainland_map, x, y+1, nationID, true, false, -1, 0);
							if ((mainland_map.GetBlockNationID(x+1, y-1) == -1) && mainland_map.BlockIsHabitable(x+1, y-1)) World.SetBlockNationID(mainland_map, x+1, y-1, nationID, true, false, -1, 0);
							if ((mainland_map.GetBlockNationID(x+1, y) == -1) && mainland_map.BlockIsHabitable(x+1, y)) World.SetBlockNationID(mainland_map, x+1, y, nationID, true, false, -1, 0);
							if ((mainland_map.GetBlockNationID(x+1, y+1) == -1) && mainland_map.BlockIsHabitable(x+1, y+1)) World.SetBlockNationID(mainland_map, x+1, y+1, nationID, true, false, -1, 0);
						}
					}
				}

				// Loop to fill in gaps
				for (i = 0; i < 2; i++)
				{
					for (y = nationData.mainland_footprint.y0; y <= nationData.mainland_footprint.y1; y++)
					{
						for (x = nationData.mainland_footprint.x0; x <= nationData.mainland_footprint.x1; x++)
						{
							blockData = mainland_map.GetBlockData(x, y);
							blockNData = mainland_map.GetBlockData(x, y-1);
							blockSData = mainland_map.GetBlockData(x, y+1);
							blockEData = mainland_map.GetBlockData(x+1, y);
							blockWData = mainland_map.GetBlockData(x-1, y);

							// If this block is empty and habitable, but at least 2 of its neighbors belong to the current nation, have the current nation occupy this block.
							if ((blockData != null) && (blockData.nationID == -1) && ((blockData.terrain == Constants.TERRAIN_FLAT_LAND) || (blockData.terrain == Constants.TERRAIN_BEACH)))
							{
								int neighborSelf = 0;
								if ((blockNData != null) && (blockNData.nationID == nationID)) neighborSelf++;
								if ((blockSData != null) && (blockSData.nationID == nationID)) neighborSelf++;
								if ((blockEData != null) && (blockEData.nationID == nationID)) neighborSelf++;
								if ((blockWData != null) && (blockWData.nationID == nationID)) neighborSelf++;

								if (neighborSelf >= 2) {
									World.SetBlockNationID(mainland_map, x, y, nationID, true, false, -1, 0);
								}
							}
						}
					}
				}

				// For any resources or orbs within the nation's bounding box, surround them with defenses.
				for (y = nationData.mainland_footprint.y0; y <= nationData.mainland_footprint.y1; y++)
				{
					for (x = nationData.mainland_footprint.x0; x <= nationData.mainland_footprint.x1; x++)
					{
						blockData = mainland_map.GetBlockData(x, y);
						blockExtData = mainland_map.GetBlockExtendedData(x, y, false);

						if ((blockData != null) && (blockData.nationID == nationID) && (blockExtData != null) && (blockExtData.objectID >= ObjectData.RESOURCE_OBJECT_BASE_ID))
						{
							// Test for 7x7 area belonging to this nation, around the object.
							selfSquares = 0;
							for (yp = y - 3; yp <= y + 3; yp++)
							{
								for (xp = x - 3; xp <= x + 3; xp++)
								{
									if (mainland_map.GetBlockNationID(xp, yp) == nationID) {
										selfSquares++;
									}
								}
							}

							if (selfSquares == 49)
							{
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x-2, y-2, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x-2, y-1, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x-2, y, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x-2, y+1, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x-2, y+2, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x-1, y-2, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x-1, y+2, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x, y-2, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x, y+2, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x+1, y-2, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x+1, y+2, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x+2, y-2, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x+2, y-1, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x+2, y, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x+2, y+1, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x+2, y+2, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);

								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x-3, y-2, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x-3, y, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x-3, y+2, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);

								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x+3, y-2, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x+3, y, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x+3, y+2, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);

								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x-2, y-3, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x, y-3, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x+2, y-3, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);

								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x-2, y+3, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x, y+3, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x+2, y+3, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);

								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x-1, y-1, true), nationData, defense0, 0);
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x-1, y, true), nationData, defense2, 0);
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x-1, y+1, true), nationData, defense0, 0);
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x, y-1, true), nationData, defense2, 0);
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x, y+1, true), nationData, defense2, 0);
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x+1, y-1, true), nationData, defense0, 0);
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x+1, y, true), nationData, defense2, 0);
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x+1, y+1, true), nationData, defense0, 0);

								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x-3, y-1, true), nationData, defense3, 0);
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x-3, y+1, true), nationData, defense1, 0);
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x+3, y-1, true), nationData, defense3, 0);
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x+3, y+1, true), nationData, defense1, 0);

								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x-1, y-3, true), nationData, defense3, 0);
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x+1, y-3, true), nationData, defense1, 0);
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x-1, y+3, true), nationData, defense3, 0);
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x+1, y+3, true), nationData, defense1, 0);
							}
							else
							{
								// Test for 5x5 area belonging to this nation, around the object.
								selfSquares = 0;
								for (yp = y - 2; yp <= y + 2; yp++)
								{
									for (xp = x - 2; xp <= x + 2; xp++)
									{
										if (mainland_map.GetBlockNationID(xp, yp) == nationID) {
											selfSquares++;
										}
									}
								}

								if (selfSquares == 25)
								{
										Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x-2, y-2, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
										Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x-2, y-1, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
										Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x-2, y, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
										Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x-2, y+1, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
										Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x-2, y+2, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
										Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x-1, y-2, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
										Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x-1, y+2, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
										Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x, y-2, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
										Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x, y+2, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
										Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x+1, y-2, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
										Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x+1, y+2, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
										Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x+2, y-2, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
										Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x+2, y-1, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
										Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x+2, y, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
										Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x+2, y+1, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
										Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x+2, y+2, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);

										Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x-1, y-1, true), nationData, defense0, 0);
										Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x-1, y, true), nationData, defense1, 0);
										Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x-1, y+1, true), nationData, defense0, 0);
										Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x, y-1, true), nationData, defense1, 0);
										Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x, y+1, true), nationData, defense1, 0);
										Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x+1, y-1, true), nationData, defense0, 0);
										Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x+1, y, true), nationData, defense1, 0);
										Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x+1, y+1, true), nationData, defense0, 0);

								}
								else
								{
									// Test for 3x3 area belonging to this nation, around the object.
									selfSquares = 0;
									for (yp = y - 1; yp <= y + 1; yp++)
									{
										for (xp = x - 1; xp <= x + 1; xp++)
										{
											if (mainland_map.GetBlockNationID(xp, yp) == nationID) {
												selfSquares++;
											}
										}
									}

									if (selfSquares == 9)
									{
										Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x-1, y-1, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
										Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x-1, y, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
										Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x-1, y+1, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
										Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x, y-1, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
										Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x, y+1, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
										Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x+1, y-1, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
										Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x+1, y, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
										Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x+1, y+1, true), nationData, bestBuild[BuildData.TYPE_WALL], 0);
									}
								}
							}
						}
					}
				}

				// Randomly place some defenses and storage structures, and defenses, in the nation's area.
				for (y = nationData.mainland_footprint.y0; y <= nationData.mainland_footprint.y1; y++)
				{
					for (x = nationData.mainland_footprint.x0; x <= nationData.mainland_footprint.x1; x++)
					{
						blockData = mainland_map.GetBlockData(x, y);
						blockExtData = mainland_map.GetBlockExtendedData(x, y, false);

						if ((blockData != null) && (blockData.nationID == nationID) && ((blockExtData == null) || (blockExtData.objectID == -1)))
						{
							// Build energy storage
							if (rand.nextInt(200) == 0) {
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x, y, true), nationData, bestBuild[BuildData.TYPE_ENERGY_STORAGE], 0);
							}

							// Build manpower storage
							if (rand.nextInt(300) == 0) {
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x, y, true), nationData, bestBuild[BuildData.TYPE_MANPOWER_STORAGE], 0);
							}

							// Build defense0
							if (rand.nextInt(400) == 0) {
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x, y, true), nationData, defense0, 0);
							}

							// Build defense1
							if (rand.nextInt(600) == 0) {
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x, y, true), nationData, defense1, 0);
							}

							// Build defense2
							if (rand.nextInt(800) == 0) {
								Gameplay.Build(Constants.MAINLAND_MAP_ID, mainland_map.GetBlockExtendedData(x, y, true), nationData, defense2, 0);
							}
						}
					}
				}
			}

			// Progress update
			Output.PrintToScreen("Placed nation '" + cur_name + "'.");
		}
	}

	public static void DemoHomelands()
	{
		//String[] demo_names = {"32 in 1","3-D Tic-Tac-Toe","Acid Drop","Action Pak","Actionauts","Decathlon","Adventure","Adventures of Tron","Air Raid","Air Raiders","Airlock","Air-Sea Battle","Alien","Alien's Return","Alpha Beam with Ernie","Amidar","Armor Ambush","Artillery Duel","Assault","Asterix","Asteroids","Astroblast","Atari Video Cube","Atlantis","Atlantis II","Bachelor Party","Bachelorette Party","Back To School Pak","Backgammon","Bank Heist","Barnstorming","Fun With Numbers","Basic Programming","Basketball","Battlezone","Beamrider","Beany Bopper","Beat' Em & Eat 'Em","Berenstain Bears","Bermuda Triangle","Berzerk","Egg Catch","Birthday Mania","Blackjack","Blue Print","BMX Airmaster","Bobby Is Going Home","Boing!","Bowling","Boxing","Brain Games","Breakout","Bridge","Bugs","Bump 'n' Jump","Bumper Bash","BurgerTime","Burning Desire","Busy Police","Cakewalk","California Games","Canyon Bomber","Carnival","Casino","Cathouse Blues","Centipede","Challenge","Challenge of Nexar","Chase the Chuck Wagon","Checkers","China Syndrome","Chopper Command","Chuck Norris Superkicks","Circus Atari","Coconuts","Codebreaker","Combat","Commando","Commando Raid","Communist Mutants from Space","Concentration","Condor Attack","Confrontation","Congo Bongo","Cookie Monster Munch","Cosmic Ark","Cosmic Commuter","Cosmic Corridor","Cosmic Creeps","Cosmic Free Fire","Cosmic Swarm","Crab Control","Crackpots","Crash Dive","Crazy Climber","Cross Force","Crossbow","Cruise Missile","Crypts of Chaos","Crystal Castles","Cubicolor","Custer's Revenge","Dancing Plate","Dark Cavern","Dark Chambers","Deadly Discs","Deadly Duck","Death Star Battle","Death Trap","Defender","Defender II","Demolition Herby","Demon Attack","Demons to Diamonds","Desert Falcon","Diagnostic Cartridge","Dice Puzzle","Dig Dug","Dishaster","Dodge 'Em","Dolphin","Donkey Kong","Donkey Kong Junior","Double Dragon","Double Dunk","Dragon Treasure","Dragonfire","Dragonstomper","Dragster","Dumbo's Flying Circus","E.T. the Extra-Terrestrial","Earth Attack","Earth Dies Screaming","Earthworld","Eddy Langfinger","Eggomania","Eli's Ladder","Encounter at L-5","Enduro","Entombed","Escape From The Mindmaster","Espial","Exocet","Exocet Missile","Extra Terrestrials","Fantastic Voyage","Farmer Dan","Fast Eddie","Fast Food","Fatal Run","Fathom","Fighter Pilot","Final Approach","Fire Fighter","Fire Fly","Fireball","Fireworld","Fisher Price","Fishing Derby","Flag Capture","Flash Gordon","Football","Frankenstein's Monster","Freeway","Frog Pond","Frogger","Frogs and Flies","Front Line","Frostbite","G.I. Joe: Cobra Strike","Galaxian","Gamma-Attack","Gangster Alley","Gas Hog","Gauntlet","Ghost Manor","Ghostbusters","Ghostbusters II","Gigolo","Glacier Patrol","Glib","Golf","Gopher","Gorf","Grand Prix","Gravitar","Great Escape","Gremlins","Guardian","Gyruss","H.E.R.O.","Halloween","Hangman","Harbor Escape","Haunted House","Home Run","Human Cannonball","Hunt & Score","I Want My Mommy","Ice Hockey","Ikari Warriors","Inca Gold","Indy 500","Infiltrate","International Soccer","James Bond 007","Jawbreaker","Jedi Arena","Journey Escape","Joust","Jr. Pac-Man","Jungle Fever","Jungle Hunt","Kaboom!","Kangaroo","Karate","Keystone Kapers","Killer Satellites","King Kong","Klax","Knight on the Town","Kool-Aid Man","Krull","Kung-Fu Master","Lady in Wading","Laser Blast","Laser Gates","Laser Volley","Lochjaw","Lock 'n' Chase","London Blitz","Lost Luggage","M*A*S*H","M.A.D.","MagiCard","Malagai","Mangia","Marauder","Marine Wars","Mario Bros.","Master Builder","Masters of the Universe","Math Gran Prix","Maze Craze","Mega Force","MegaBoy","Megamania","Midnight Magic","Millipede","Miner 2049er","Miner 2049er II","Mines of Minos","Miniature Golf","Missile Command","Missile Control","Mission 3000 A.D.","Mission Survive","Mogul Maniac","Montezuma's Revenge","Moon Patrol","Moonsweeper","Motocross","Motocross Racer","MotoRodeo","Mountain King","Mouse Trap","Mr. Do!","Mr. Do's Castle","Mr. Postman","Ms. Pac-Man","Music Machine","My Golf","Name This Game","Night Driver","Night Stalker","Nightmare","No Escape!","Nuts","Obelix","Ocean City Defender","Off the Wall","Off Your Rocker","Official Frogger","Oink!","Omega Race","Open Sesame","Oscar's Trash Race","Othello","Out of Control","Outlaw","Pac-Kong","Pac-Man","Panda Chase","Parachute","Party Mix","Pelé's Soccer","Pengo","Pepsi Invaders","Pete Rose Baseball","Phantom Tank","Phantom-Panzer","Phaser Patrol","Philly Flasher","Phoenix","Pick 'n Pile","Pick Up","Picnic","Piece o' Cake","Pigs in Space","Pinball","Piraten-Schiff","Pitfall II: Lost Caverns","Pitfall!","Planet of Zoom","Planet Patrol","Planeten Patrouile","Plaque Attack","Polaris","Pole Position","Polo","Pooyan","Popeye","Porky's","Pressure Cooker","Private Eye","Pyramid War","Q*bert","Q*bert's Qubes","Quadrun","Quest for Quintana Roo","Quick Step","Rabbit Transit","Racing Pak","Racquetball","Radar","Radar Lock","Raft Rider","Raiders of the Lost Ark","Ram It","Rampage","Reactor","RealSports Baseball","RealSports Boxing","RealSports Football","RealSports Soccer","RealSports Tennis","RealSports Volleyball","Red Sea Crossing","Rescue in Gargamel's Castle","Rescue Terra 1","Revenge of the Beefsteak Tomatoes","Riddle of the Sphinx","River Patrol","River Raid","River Raid II","Road Runner","Robin Hood","Robot Commando Raid","Robot Tank","Roc 'N Rope","Room of Doom","Rubik's Cube","Save Our Ship","Save the Whales","Scraper Caper","Scuba Diver","Sea Hawk","Seamonster","Seaquest","Secret Quest","Sentinel","Shark Attack","Shootin' Gallery","Shuttle Orbiter","Signal Tracing Cartridge","Sir Lancelot","Skate Boardin'","Skeet Shoot","Skiing","Sky Diver","Sky Jinks","Sky Skipper","Slot Machine","Slot Racers","Smurfs Save the Day","Snail Against Squirrel","Sneak 'N Peek","Snoopy and the Red Baron","Solar Fox","Solar Storm","Solaris","Sorcerer","Sorcerer's Apprentice","Space Adventure","Space Attack","Space Canyon","Space Cavern","Space Grid","Space Invaders","Space Jockey","Space Shuttle","Space War","Spacechase","Spacemaster X-7","Spider Fighter","Spider Maze","Spiderdroid","Spider-Man","Spike's Peak","Spitfire Attack","Springer","Sprintmaster","Spy Hunter","Squeeze Box","Sssnake","Stampede","Star Fox","Star Raiders","Star Ship","Star Strike","Star Voyager","Star Wars: The Arcade Game","Stargate","Stargunner","Starmaster","Steeplechase","Stellar Track","Strategic Operations Simulator","Strategy X","Strawberry Shortcake","Street Racer","Stronghold","Stunt Man","Submarine Commander","Sub-Scan","Subterranea","Suicide Mission","Summer Games","Super Baseball","Super Baumeister","Super Breakout","Super Challenge Baseball","Super Challenge Football","Super Cobra","Super Football","Superman","Surfer's Paradise","Surround","Survival Island","Survival Run","Sword of Saros","Tac-Scan","Tank Brigade","Tank City","Tanks But No Tanks","Tapeworm","Tapper","Task Force","Tax Avoiders","Taz","Tennis","Texas Chainsaw Massacre","The Empire Strikes Back","Threeedeep!","Threshold","Thunderground","Time Pilot","Time Warp","Title Match","Tomarc The Barbarian","Tomcat","Tooth Protectors","Towering Inferno","Track & Field","Treasure Below","Trick Shot","Tron: Deadly Discs","Tunnel Runner","Turmoil","Tutankham","Universal Chaos","Up'n Down","Vanguard","Venture","Video Checkers","Video Chess","Video Jogger","Video Life","Video Olympics","Video Pinball","Video Reflex","Vulture Attack","Wabbit","Wall Ball","Wall Defender","War Zone","Warlords","Warplock","Waterworld","Weltraumtunnel","Wing War","Winter Games","Wizard of Wor","Word Zapper","Worm War I","Xenophobe","X-man","Yars' Revenge","Zaxxon","Zoo Fun","Z-Tack"};
		NationData nationData;
		NationTechData nationTechData;
		UserData userData;
		BlockData blockData, blockNData, blockSData, blockEData, blockWData;
		BlockExtData blockExtData;
		BuildData buildData, defense0, defense1, defense2, defense3;
		BuildData[] bestBuild = new BuildData[BuildData.NUM_TYPES];
		int i, userID, nationID, x, y, xp, yp, blockTerrain, blockNationID, selfSquares;
		int[] bestBuildHP = new int[BuildData.NUM_TYPES];
		LandMap mainland_map = DataManager.GetLandMap(Constants.MAINLAND_MAP_ID, false);

		String[] demo_names = new String[100];
		for (i = 0; i < 100; i++) {
			demo_names[i] = "Nemesis-" + i;
		}

		// Create a random generator with a seed, so its results can be reproduced.
		Random rand = new Random(865875);

		// Create the demo nations.
		CreateDemoNations(demo_names, rand, true);

		// Place each demo nation on its homeland map.
		for (String cur_name : demo_names)
		{
			// Get the data for the nation with the current demo name.
			nationID = NationData.GetNationIDByNationName(cur_name);
			nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);
			nationTechData = (NationTechData)DataManager.GetData(Constants.DT_NATIONTECH, nationID, false);

			if (nationData != null)
			{
				// Determine the BuildData of this nation's build of each type, with the highest hit points.
				int max_hit_points = 0;
				for (i = 0; i < BuildData.NUM_TYPES; i++)
				{
					bestBuild[i] = null;
					bestBuildHP[i] = 0;

					for (int j = 0; j <= 150; j++)
					{
						buildData = BuildData.GetBuildData(j);

						if ((buildData != null) && (buildData.type == i) && (buildData.hit_points >= bestBuildHP[i]) && nationTechData.available_builds.containsKey(j) && (nationTechData.available_builds.get(j) != false))
						{
							bestBuild[i] = buildData;
							bestBuildHP[i] = buildData.hit_points;
						}
					}

					//Output.PrintToScreen(cur_name + " bestBuild[" + i + "]: " + ((bestBuild[i] == null) ? "none" : bestBuild[i].name));
				}

				defense0 = (bestBuild[BuildData.TYPE_AREA_EFFECT] != null) ? bestBuild[BuildData.TYPE_AREA_EFFECT] : ((bestBuild[BuildData.TYPE_SPLASH] != null) ? bestBuild[BuildData.TYPE_SPLASH] : bestBuild[BuildData.TYPE_DIRECTED_MULTIPLE]);
				defense1 = (bestBuild[BuildData.TYPE_COUNTER_ATTACK] != null) ? bestBuild[BuildData.TYPE_COUNTER_ATTACK] : ((bestBuild[BuildData.TYPE_AREA_FORTIFICATION] != null) ? bestBuild[BuildData.TYPE_AREA_FORTIFICATION] : ((bestBuild[BuildData.TYPE_SPECIFIC_LASTING_WIPE] != null) ? bestBuild[BuildData.TYPE_SPECIFIC_LASTING_WIPE] : defense0));
				defense2 = (bestBuild[BuildData.TYPE_WIPE] != null) ? bestBuild[BuildData.TYPE_WIPE] : ((bestBuild[BuildData.TYPE_TOWER_BUSTER] != null) ? bestBuild[BuildData.TYPE_TOWER_BUSTER] : ((bestBuild[BuildData.TYPE_GENERAL_LASTING_WIPE] != null) ? bestBuild[BuildData.TYPE_GENERAL_LASTING_WIPE] : defense1));
				defense3 = (bestBuild[BuildData.TYPE_AIR_DROP] != null) ? bestBuild[BuildData.TYPE_AIR_DROP] : ((bestBuild[BuildData.TYPE_RECAPTURE] != null) ? bestBuild[BuildData.TYPE_RECAPTURE] : defense2);

				//Output.PrintToScreen(cur_name + " defense0: " + ((defense0 == null) ? "none" : defense0.name));
				//Output.PrintToScreen(cur_name + " defense1: " + ((defense1 == null) ? "none" : defense1.name));
				//Output.PrintToScreen(cur_name + " defense2: " + ((defense2 == null) ? "none" : defense2.name));
				//Output.PrintToScreen(cur_name + " defense3: " + ((defense3 == null) ? "none" : defense3.name));

				// Create this nation's homeland map.
				LandMap homeland_map = Homeland.GetHomelandMap(nationData.ID);

				// Remove the nation from its homeland.
				World.RemoveNationFromMap(homeland_map, nationData);

				// Place the nation within its homeland map.
				int[] coords = new int[2];
				World.PlaceNationWithinArea(homeland_map, nationData.ID, 10, 10, homeland_map.width - 10, homeland_map.height - 10, -1, -1, true, coords);
				int centerX = coords[0];
				int centerY = coords[1];

				// If the nation could not be placed, skip it.
				if (nationData.homeland_footprint.area == 0) {
					continue;
				}

				//Output.PrintToScreen("Before expanding " + cur_name + ", area: " + nationData.homeland_footprint.area + ", x0: " + nationData.homeland_footprint.x0 + ", x1: " + nationData.homeland_footprint.x1 + ", y0: " + nationData.homeland_footprint.y0 + ", y1: " + nationData.homeland_footprint.y1);

				// Loop to expand the nation
				boolean supportable_area_reached = false;
				for (i = 1; i < 50; i++)
				{
					for (y = centerY - i; y <= centerY + i; y++)
					{
						for (x = centerX - i; x <= centerX + i; x++)
						{
							// Get the block's terrain type
							blockTerrain = homeland_map.GetBlockTerrain(x, y);

							// Skip the block if it is not habitable terrain
							if (blockTerrain != Constants.TERRAIN_FLAT_LAND) {
								continue;
							}

							// Occupy the current square.
							World.SetBlockNationID(homeland_map, x, y, nationID, true, false, -1, 0);

							// If the nation's supportable area has been reached, stop expanding.
							if (nationData.homeland_footprint.area >= nationData.GetSupportableArea(homeland_map.ID))
							{
								supportable_area_reached = true;
								break;
							}
						}

						if (supportable_area_reached) {
							break;
						}
					}

					if (supportable_area_reached) {
						break;
					}
				}

				// Place the orb shards in the nation's homeland.
				Raid.PlaceShard(nationData, homeland_map, 200);
				Raid.PlaceShard(nationData, homeland_map, 201);
				Raid.PlaceShard(nationData, homeland_map, 202);

				// Surround the red and green orb shards with defenses.
				for (y = nationData.homeland_footprint.y0; y <= nationData.homeland_footprint.y1; y++)
				{
					for (x = nationData.homeland_footprint.x0; x <= nationData.homeland_footprint.x1; x++)
					{
						blockData = homeland_map.GetBlockData(x, y);
						blockExtData = homeland_map.GetBlockExtendedData(x, y, false);

						if ((blockData != null) && (blockData.nationID == nationID) && (blockExtData != null) && ((blockExtData.objectID == 200) || (blockExtData.objectID == 201)))
						{
							if (rand.nextInt(3) == 0)
							{
								DemoHomelands_Build(homeland_map, x-2, y-2, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x-2, y-1, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x-2, y, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x-2, y+1, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x-2, y+2, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x-1, y-2, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x-1, y+2, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x, y-2, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x, y+2, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x+1, y-2, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x+1, y+2, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x+2, y-2, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x+2, y-1, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x+2, y, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x+2, y+1, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x+2, y+2, nationData, bestBuild[BuildData.TYPE_WALL]);

								DemoHomelands_Build(homeland_map, x-3, y-2, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x-3, y, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x-3, y+2, nationData, bestBuild[BuildData.TYPE_WALL]);

								DemoHomelands_Build(homeland_map, x+3, y-2, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x+3, y, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x+3, y+2, nationData, bestBuild[BuildData.TYPE_WALL]);

								DemoHomelands_Build(homeland_map, x-2, y-3, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x, y-3, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x+2, y-3, nationData, bestBuild[BuildData.TYPE_WALL]);

								DemoHomelands_Build(homeland_map, x-2, y+3, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x, y+3, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x+2, y+3, nationData, bestBuild[BuildData.TYPE_WALL]);

								DemoHomelands_Build(homeland_map, x-1, y-1, nationData, defense0);
								DemoHomelands_Build(homeland_map, x-1, y, nationData, defense2);
								DemoHomelands_Build(homeland_map, x-1, y+1, nationData, defense0);
								DemoHomelands_Build(homeland_map, x, y-1, nationData, defense2);
								DemoHomelands_Build(homeland_map, x, y+1, nationData, defense2);
								DemoHomelands_Build(homeland_map, x+1, y-1, nationData, defense0);
								DemoHomelands_Build(homeland_map, x+1, y, nationData, defense2);
								DemoHomelands_Build(homeland_map, x+1, y+1, nationData, defense0);

								DemoHomelands_Build(homeland_map, x-3, y-1, nationData, defense3);
								DemoHomelands_Build(homeland_map, x-3, y+1, nationData, defense1);
								DemoHomelands_Build(homeland_map, x+3, y-1, nationData, defense3);
								DemoHomelands_Build(homeland_map, x+3, y+1, nationData, defense1);

								DemoHomelands_Build(homeland_map, x-1, y-3, nationData, defense3);
								DemoHomelands_Build(homeland_map, x+1, y-3, nationData, defense1);
								DemoHomelands_Build(homeland_map, x-1, y+3, nationData, defense3);
								DemoHomelands_Build(homeland_map, x+1, y+3, nationData, defense1);
							}
							else if (rand.nextInt(2) == 0)
							{
								DemoHomelands_Build(homeland_map, x-2, y-2, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x-2, y-1, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x-2, y, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x-2, y+1, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x-2, y+2, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x-1, y-2, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x-1, y+2, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x, y-2, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x, y+2, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x+1, y-2, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x+1, y+2, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x+2, y-2, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x+2, y-1, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x+2, y, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x+2, y+1, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x+2, y+2, nationData, bestBuild[BuildData.TYPE_WALL]);

								DemoHomelands_Build(homeland_map, x-1, y-1, nationData, defense0);
								DemoHomelands_Build(homeland_map, x-1, y, nationData, defense1);
								DemoHomelands_Build(homeland_map, x-1, y+1, nationData, defense0);
								DemoHomelands_Build(homeland_map, x, y-1, nationData, defense1);
								DemoHomelands_Build(homeland_map, x, y+1, nationData, defense1);
								DemoHomelands_Build(homeland_map, x+1, y-1, nationData, defense0);
								DemoHomelands_Build(homeland_map, x+1, y, nationData, defense1);
								DemoHomelands_Build(homeland_map, x+1, y+1, nationData, defense0);
							}
							else
							{
								DemoHomelands_Build(homeland_map, x-1, y-1, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x-1, y, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x-1, y+1, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x, y-1, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x, y+1, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x+1, y-1, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x+1, y, nationData, bestBuild[BuildData.TYPE_WALL]);
								DemoHomelands_Build(homeland_map, x+1, y+1, nationData, bestBuild[BuildData.TYPE_WALL]);
							}
						}
					}
				}

				// Randomly place some defenses and storage structures, and defenses, in the nation's area.
				for (y = nationData.homeland_footprint.y0; y <= nationData.homeland_footprint.y1; y++)
				{
					for (x = nationData.homeland_footprint.x0; x <= nationData.homeland_footprint.x1; x++)
					{
						blockData = mainland_map.GetBlockData(x, y);
						blockExtData = mainland_map.GetBlockExtendedData(x, y, false);

						if ((blockData != null) && (blockData.nationID == nationID) && ((blockExtData == null) || (blockExtData.objectID == -1)))
						{
							// Build defense0
							if (rand.nextInt(40) == 0) {
								DemoHomelands_Build(homeland_map, x, y, nationData, defense0);
							}

							// Build defense1
							if (rand.nextInt(60) == 0) {
								DemoHomelands_Build(homeland_map, x, y, nationData, defense1);
							}

							// Build defense2
							if (rand.nextInt(80) == 0) {
								DemoHomelands_Build(homeland_map, x, y, nationData, defense2);
							}
						}
					}
				}
			}

			// Progress update
			Output.PrintToScreen("Created homeland for nation '" + cur_name + "'.");
		}
	}

	public static void DemoHomelands_Build(LandMap _landmap, int _x, int _y, NationData _nationData, BuildData _buildData)
	{
		BlockData blockData = _landmap.GetBlockData(_x, _y);

		// If the given block doesn't exist or is not occupied by the given nation, do nothing.
		if ((blockData == null) || (blockData.nationID != _nationData.ID)) {
			return;
		}

		// If the block already contains any object (other defense, or orb shard), do nothing.
		if (_landmap.GetBlockObjectID(_x, _y) != -1) {
			return;
		}

		// If this build's energy burn rate is greater than the nation's remaining energy rate, do nothing.
		if (_buildData.energy_burn_rate >  (_nationData.GetFinalEnergyRate(_landmap.ID) - _nationData.GetFinalEnergyBurnRate(_landmap.ID))) {
			return;
		}

		// Build the structure.
		Gameplay.Build(_landmap.ID, _landmap.GetBlockExtendedData(_x, _y, true), _nationData, _buildData, 0);
	}

	public static void CreateDemoNations(String[] _demo_names, Random _rand, boolean _allow_invisibility)
	{
		int userID, nationID;
		UserData userData;
		NationData nationData;
		NationTechData nationTechData;
		LandMap mainland_map = DataManager.GetLandMap(Constants.MAINLAND_MAP_ID, false);

		for (String cur_name : _demo_names)
		{
			// Get or create the player account data corresponding to this name.
			PlayerAccountData player_account = null;
			int playerID = AccountDB.GetPlayerIDByUsername(cur_name);
			if (playerID == -1)
			{
				// Create player account
				player_account = AccountDB.CreateNewPlayerAccount(cur_name);
				//player_account = Application.CreateNewPlayer(cur_name, null);

				// Set the player account's password to standard default
				player_account.passhash = AccountDB.DeterminePasswordHash("nem-aaaa");
				AccountDB.WritePlayerAccount(player_account);
			}
			else
			{
				player_account = AccountDB.ReadPlayerAccount(playerID);
			}

			// If the user and nation corresponding to this name don't yet exist on this server, create them.
			if ((UserData.GetUserIDByUsername(cur_name) == -1) && (NationData.GetNationIDByNationName(cur_name) == -1))
			{
				// Create user and nation
				userID = Application.CreateUserAndNation(null, player_account, null);

				// Get the user data
				userData = (UserData)DataManager.GetData(Constants.DT_USER, userID, false);

				// Get the nation data
				nationData = (NationData)DataManager.GetData(Constants.DT_NATION, userData.nationID, false);

				// Set the nation's name and password
				nationData.name = cur_name;
				nationData.password = "nem-aaaa";

				// Mark the nation's data to be updated.
				DataManager.MarkForUpdate(nationData);
			}
		}

		// Update the database, to store any new nation names created above.
		DataManager.UpdateDatabase(false);

		// Remove each demo nation from the map, and reset its level and advances.
		for (String cur_name : _demo_names)
		{
			// Get the data for the nation with the current demo name.
			nationID = NationData.GetNationIDByNationName(cur_name);
			nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);
			nationTechData = (NationTechData)DataManager.GetData(Constants.DT_NATIONTECH, nationID, false);

			if (nationData != null)
			{
				// Randomly select the nation's color from the options available in the client.
				switch (_rand.nextInt(30))
				{
					case 0: nationData.r = 255; nationData.g = 0; nationData.b = 0; break;
					case 1: nationData.r = 255; nationData.g = 153; nationData.b = 0; break;
					case 2: nationData.r = 255; nationData.g = 248; nationData.b = 0; break;
					case 3: nationData.r = 0; nationData.g = 255; nationData.b = 7; break;
					case 4: nationData.r = 0; nationData.g = 192; nationData.b = 255; break;
					case 5: nationData.r = 41; nationData.g = 65; nationData.b = 223; break;
					case 6: nationData.r = 172; nationData.g = 26; nationData.b = 223; break;
					case 7: nationData.r = 255; nationData.g = 0; nationData.b = 195; break;
					case 8: nationData.r = 180; nationData.g = 0; nationData.b = 0; break;
					case 9: nationData.r = 163; nationData.g = 71; nationData.b = 0; break;
					case 10: nationData.r = 11; nationData.g = 139; nationData.b = 0; break;
					case 11: nationData.r = 255; nationData.g = 165; nationData.b = 165; break;
					case 12: nationData.r = 255; nationData.g = 255; nationData.b = 255; break;
					case 13: nationData.r = 128; nationData.g = 128; nationData.b = 128; break;
					case 14: nationData.r = 0; nationData.g = 0; nationData.b = 0; break;
					case 15: nationData.r = 204; nationData.g = 5; nationData.b = 87; break;
					case 16: nationData.r = 255; nationData.g = 196; nationData.b = 107; break;
					case 17: nationData.r = 131; nationData.g = 122; nationData.b = 0; break;
					case 18: nationData.r = 0; nationData.g = 255; nationData.b = 155; break;
					case 19: nationData.r = 0; nationData.g = 125; nationData.b = 167; break;
					case 20: nationData.r = 173; nationData.g = 187; nationData.b = 255; break;
					case 21: nationData.r = 109; nationData.g = 0; nationData.b = 167; break;
					case 22: nationData.r = 255; nationData.g = 161; nationData.b = 233; break;
					case 23: nationData.r = 99; nationData.g = 42; nationData.b = 42; break;
					case 24: nationData.r = 165; nationData.g = 125; nationData.b = 93; break;
					case 25: nationData.r = 16; nationData.g = 88; nationData.b = 10; break;
					case 26: nationData.r = 212; nationData.g = 175; nationData.b = 55; break;
					case 27: nationData.r = 253; nationData.g = 94; nationData.b = 83; break;
					case 28: nationData.r = 195; nationData.g = 195; nationData.b = 195; break;
					case 29: nationData.r = 64; nationData.g = 64; nationData.b = 64; break;
				}

				if (_rand.nextInt(5) == 0)
				{
					// Give the nation an emblem
					nationData.emblem_index = _rand.nextInt(14) + 69;
					nationData.emblem_color = _rand.nextInt(7);
				}

				// Reset the nation's advances
				Gameplay.ResetAdvances(nationID, false);

				// Remove the nation's XP, levels and advance points.
				nationData.xp = 0;
				nationData.level = 1;
				nationData.advance_points = 0;

				// Randomly determine this nation's new level.
				int level = _rand.nextInt(120) + 2;

				// Set the nation's number of raid medals.
				int prev_defender_medals = nationData.raid_defender_medals;
				nationData.raid_attacker_medals = level * 10;
				nationData.raid_defender_medals = level * 10;
				Raid.DefenderMedalsCountChanged(nationData.ID, prev_defender_medals, nationData.raid_defender_medals);

				Output.PrintToScreen("About to add XP to nation: " + nationData.name + " (should be " + cur_name + ")");

				// Add sufficient XP to the nation to get it to the desired level.
				Gameplay.AddXP(nationData, Constants.XP_PER_LEVEL[level], -1, -1, -1, false, false, 0, Constants.XP_DEMO);

				if (nationData.advance_points > 0)
				{
					int id_chemistry			= TechData.GetNameToIDMap("Chemistry");
					int id_biology				= TechData.GetNameToIDMap("Biology");
					int id_seed_illusion	= TechData.GetNameToIDMap("Seed Illusion");

					// Add the root advance of one of the three branches, chosen randomly.
					switch (_rand.nextInt(3))
					{
						case 0: Technology.AddTechnology(nationID, id_chemistry, 0, false, false, 0); break;
						case 1: Technology.AddTechnology(nationID, id_biology, 0, false, false, 0); break;
						case 2: Technology.AddTechnology(nationID, id_seed_illusion, 0, false, false, 0); break;
					}

					// Remove advance point for the added root advance.
					nationData.advance_points -= 1;

					while (nationData.advance_points > 0)
					{
						int selectedTechID = -1;
						int numAvailableTechs = 0;
						for (int techID = 0; techID < 300; techID++)
						{
							// Skip root advances.
							if ((techID == id_chemistry) || (techID == id_biology) || (techID == id_seed_illusion)) {
								continue;
							}

							// If _allow_invisibility is false, skip Mass Illusion so structures won't become invisible.
							if ((!_allow_invisibility) && (techID == 215)) {
								continue;
							}

							// Determine whether this advance will be selected to be researched.
							if (Technology.RequirementsMet(techID, nationData, nationTechData))
							{
								numAvailableTechs++;
								if ((numAvailableTechs == 1) || (_rand.nextInt(numAvailableTechs) == 0)) {
									selectedTechID = techID;
								}
							}
						}

						// Research the selected advance.
						if (selectedTechID != -1) {
							Technology.AddTechnology(nationID, selectedTechID, 0, false, false, 0);
						}

						// Remove advance point for the added advance.
						nationData.advance_points -= 1;
					}
				}

				// Remove the nation from each block it occupies in the world
				World.RemoveNationFromMap(mainland_map, nationData);
			}
		}
	}

	public static boolean ProcessAdminCommand(String _command, ClientThread _admin_thread)
	{
		boolean success = true;

		try
		{
			boolean valid = false;

			// Print the command that's been received.
			Output.PrintToScreen("COMMAND from " + ((_admin_thread == null) ? "console: " : (_admin_thread.player_account.username + " (" + _admin_thread.clientIP + "): ")) + _command);

			String original_command = _command.trim();
			String command = original_command.toUpperCase();

			if ((command.indexOf("ADD_ADVANCE")!=-1) && (command.indexOf("ADD_ADVANCE_POINTS")==-1))
			{
				valid = true;
				Admin.AddAdvance(Constants.FetchParameter(command, "NATION", false),Constants.FetchParameterInt(command, "ADVANCE"));
			}
			if (command.indexOf("ADD_ADVANCE_POINTS")!=-1)
			{
				int num_points = Constants.FetchParameterInt(command, "AMOUNT");

				Admin.AddAdvancePoints(Constants.FetchParameter(command, "NATION", false), Constants.FetchParameterInt(command, "AMOUNT"));
				valid = true;
			}
			if (command.indexOf("ADD_COUNTDOWN_ALL")!=-1)
			{
				// Award the given countdown amount to every nation
				int num_countdown = Constants.FetchParameterInt(command, "AMOUNT");
				boolean purchased = (Constants.FetchParameterInt(command, "PURCHASED") != 0);
				Admin.AddCountdownAll(num_countdown, purchased);
				valid = true;
			}
			else if (command.indexOf("ADD_COUNTDOWN")!=-1)
			{
				valid = true;
				String nation_name = Constants.FetchParameter(command, "NATION", false);
				int num_countdown = Constants.FetchParameterInt(command, "AMOUNT");
				boolean purchased = (Constants.FetchParameterInt(command, "PURCHASED") != 0);
				Admin.AddCountdown(nation_name, num_countdown, purchased);
			}
			if (command.indexOf("ADD_CREDITS")!=-1)
			{
				int num_credits = Constants.FetchParameterInt(command, "AMOUNT");
				boolean purchased = (Constants.FetchParameterInt(command, "PURCHASED") != 0);

				if (command.indexOf("ADD_CREDITS_ALL")!=-1)
				{
					// Add credit amount to every nation
					Admin.AddCreditsAll(num_credits, purchased);
					valid = true;
				}
				else
				{
					Admin.AddCredits(null, Constants.FetchParameter(command, "NATION", false), num_credits, purchased, true);
					valid = true;
				}
			}
			if (command.indexOf("ADD_DAY_COUNTDOWN_ALL")!=-1)
			{
				// Award level-based countdown amount for one day to every nation
				Admin.AddDayCountdownAll();
				valid = true;
			}
			if (command.indexOf("ADD_ENERGY")!=-1)
			{
				valid = true;
				Admin.AddEnergy(null,Constants.FetchParameter(command, "NATION", false),Constants.FetchParameterInt(command, "AMOUNT"),true);
			}
			if (command.indexOf("ADD_MANPOWER_ALL") != -1)
			{
				valid = true;
				Admin.AddManpowerAll(Constants.FetchParameterInt(command, "AMOUNT"));
			}
			else if (command.indexOf("ADD_MANPOWER") != -1)
			{
				valid = true;
				Admin.AddManpower(null,Constants.FetchParameter(command, "NATION", false),Constants.FetchParameterInt(command, "AMOUNT"),true);
			}
			else if (command.indexOf("ADD_PRIZE_MONEY_HISTORY") != -1)
			{
				valid = true;
				Admin.AddPrizeMoneyHistory(null,Constants.FetchParameter(command, "NATION", false),Constants.FetchParameterInt(command, "AMOUNT"),true);
			}
			if (command.indexOf("ADD_VOUCHERS_FROM_FILE")!=-1)
			{
				valid = true;
				Admin.AddVouchersFromFile(Constants.FetchParameter(original_command, "FILE", false));
			}
			else if (command.indexOf("ADD_VOUCHER")!=-1)
			{
				valid = true;
				Admin.AddVoucher(Constants.FetchParameter(original_command, "CODE", false),Constants.FetchParameterInt(command, "AMOUNT"));
			}
			if ((command.indexOf("ADD_XP")!=-1) && (command.indexOf("ADD_XP_USER")==-1))
			{
				valid = true;
				Admin.AddNationXP(null,Constants.FetchParameter(command, "NATION", false),Constants.FetchParameterInt(command, "AMOUNT"),true);
			}
			if (command.indexOf("ADD_XP_USER")!=-1)
			{
				valid = true;
				Admin.AddUserXP(null,Constants.FetchParameter(command, "USER", false),Constants.FetchParameterInt(command, "AMOUNT"),true);
			}
			if (command.indexOf("ALLOW_EMBLEM")!=-1)
			{
				valid = true;
				String user_name = Constants.FetchParameter(original_command, "USER", false);
				int emblem_index = Constants.FetchParameterInt(original_command, "EMBLEM");

				Admin.AllowEmblem(user_name, emblem_index);
			}
			if (command.indexOf("AWARD_PRIZE")!=-1)
			{
				int nationID = DataManager.GetNameToIDMap(Constants.DT_NATION, Constants.FetchParameter(command, "NATION", false));
				int prize_cents = Constants.FetchParameterInt(command, "AMOUNT");
				Objects.AwardPrize(nationID, prize_cents, -1, RanksData.instance, -1);
				valid = true;
			}
			if (command.indexOf("REMOVE_EMBLEM")!=-1)
			{
				valid = true;
				String user_name = Constants.FetchParameter(original_command, "USER", false);
				int emblem_index = Constants.FetchParameterInt(original_command, "EMBLEM");

				Admin.RemoveEmblem(user_name, emblem_index);
			}
			if (command.indexOf("ANNOUNCE")==0)
			{
				valid = true;
				String announcement_text = original_command.substring(8).trim();
				Comm.SendAdminAnnouncement(announcement_text);
			}
			if (command.indexOf("ASSOCIATE_USERS")!=-1)
			{
				valid = true;
				Admin.AssociateUsers(Constants.FetchParameter(original_command, "user1", false), Constants.FetchParameter(original_command, "user2", false));
			}
			if (command.indexOf("AWARD_LEVELS")!=-1)
			{
				valid = true;
				Admin.AwardLevels(Constants.FetchParameter(command, "NATION", false),Constants.FetchParameterInt(command, "AMOUNT"));
			}
			if (command.compareTo("BACKUP")==0)
			{
				valid = true;
				Output.PrintToScreen("Data backup imminent...");
				WOCServer.backup_cycle.ForceBackup();
			}
			if (command.indexOf("BLOCK_INFO")!=-1)
			{
				valid = true;
				Admin.BlockInfo(Constants.FetchParameterInt(command, "X"), Constants.FetchParameterInt(command, "Y"));
			}
			if (command.indexOf("BUILD")!=-1)
			{
				valid = true;
				Admin.Build(Constants.FetchParameterInt(command, "ID"), Constants.FetchParameterInt(command, "X"), Constants.FetchParameterInt(command, "Y"), Constants.FetchParameterInt(command, "MAP"));
			}
			if (command.indexOf("CHAT_BAN_USER")!=-1)
			{
				valid = true;
				Admin.ChatBanUser(null, Constants.FetchParameter(command, "USER", false),Constants.FetchParameterInt(command, "HOURS"),true,false);
			}
			if (command.indexOf("CLEAR_CHAT_LISTS")!=-1)
			{
				valid = true;
				Output.PrintToScreen("About to clear chat lists for all nations.");
				Admin.ClearChatLists();
			}
			if (command.compareTo("CLEAR_COMPLAINTS")==0)
			{
				valid = true;
				Output.PrintToScreen("About to clear complaints");
				Admin.ClearComplaints();
			}
			if (command.compareTo("CLEAR_DATABASE")==0)
			{
				valid = true;
				//Output.PrintToScreen("This commmand compiled out.");
				Output.PrintToScreen("About to clear the game database");
				String output_string = Admin.ClearDatabase();
				Output.PrintToScreen(output_string);
			}
			if ((command.indexOf("CLEAR_DEVICE")!=-1) && (command.indexOf("CLEAR_DEVICE_FEALTY")!=0))
			{
				valid = true;
				Admin.ClearDevice(Constants.FetchParameterInt(command, "ID"));
			}
			if (command.indexOf("CLEAR_DEVICE_FEALTY")!=-1)
			{
				valid = true;
				Admin.ClearDeviceFealty(Constants.FetchParameterInt(command, "ID"));
			}
			if (command.compareTo("CLEAR_PLAYER_DATABASE")==0)
			{
				valid = true;
				Output.PrintToScreen("This commmand compiled out.");
				//Output.PrintToScreen("About to clear the player database");
				//String output_string = Admin.ClearPlayerDatabase();
				//Output.PrintToScreen(output_string);
			}
			if (command.indexOf("CLEAR_TEMPS")!=-1)
			{
				valid = true;
				Admin.ClearTemps(Constants.FetchParameter(command, "NATION", false));
			}
			if (command.compareTo("CLIENTS")==0)
			{
				valid = true;
				Output.PrintToScreen("");
				WOCServer.listClients();
				Output.PrintToScreen("");
			}
			if (command.compareTo("CONFINE_NATIONS")==0)
			{
				valid = true;
				Admin.ConfineNations();
			}
			if (command.compareTo("CREATE_USER_REPORT")==0)
			{
				valid = true;
				Output.PrintToScreen("User report creation imminent...");
				WOCServer.update_cycle.QueueUserReport();
			}
			if (command.indexOf("DELETE_ACCOUNT_DATABASE")!=-1)
			{
				valid = true;
				Output.PrintToScreen("Deleting account DB...");
				DataManager.DeleteAccountDB();
				Output.PrintToScreen("Done. Exiting.");
				System.exit(0);
			}
			if (command.indexOf("DELETE_GAME_DATABASE")!=-1)
			{
				valid = true;
				Output.PrintToScreen("Deleting game DB...");
				DataManager.DeleteGameDB();
				Output.PrintToScreen("Done. Exiting.");
				System.exit(0);
			}
			if (command.indexOf("DELETE_NATION")!=-1)
			{
				valid = true;
				Admin.DeleteNation(Constants.FetchParameter(command, "nation", false));
			}
			if (command.indexOf("DELETE_USER")!=-1)
			{
				valid = true;
				Admin.DeleteUser(Constants.FetchParameter(command, "USER", false));
			}
			if (command.indexOf("DISASSOC_DEVICE")!=-1)
			{
				valid = true;
				Admin.DisassocDevice(Constants.FetchParameterInt(command, "ID"));
			}
			if (command.indexOf("DEMO_HOMELANDS")!=-1)
			{
				valid = true;
				Admin.DemoHomelands();
			}
			if (command.indexOf("DEMO_POPULATE")!=-1)
			{
				valid = true;
				Admin.DemoPopulate();
			}
			if (command.indexOf("DEVICE_INFO")!=-1)
			{
				valid = true;
				Admin.DeviceInfo(Constants.FetchParameterInt(command, "ID"));
			}
			if (command.indexOf("EMAIL_INFO")!=-1)
			{
				valid = true;
				Admin.EmailInfo(Constants.FetchParameter(command, "EMAIL", false));
			}
			if (command.compareTo("EMERGENCY")==0)
			{
				valid = true;
				Admin.Emergency("Admin command");
			}
			if (command.compareTo("FORCE_UPDATE_DATABASE")==0)
			{
				valid = true;
				Output.PrintToScreen("About to force database update...");
				DataManager.UpdateDatabase(true);
			}
			if (command.indexOf("GAME_BAN_USER")!=-1)
			{
				valid = true;
				Admin.GameBanUser(null, Constants.FetchParameter(command, "USER", false),Constants.FetchParameterInt(command, "HOURS"),true,false);
			}
			if ((command.indexOf("GENERATE_LANDSCAPE")==0) && (command.indexOf("GENERATE_LANDSCAPE_SET")!=0))
			{
				valid = true;
				Output.PrintToScreen("About to generate landscape image...");
				Admin.GenerateLandscape(Constants.FetchParameterInt(command, "w"), Constants.FetchParameterInt(command, "h"), Constants.FetchParameterInt(command, "seed"), Constants.FetchParameterInt(command, "border"));
				Output.PrintToScreen("Landscape image generated.");
			}
			if (command.indexOf("GENERATE_LANDSCAPE_SET")==0)
			{
				valid = true;
				int width = Constants.FetchParameterInt(command, "w");
				int height = Constants.FetchParameterInt(command, "h");
				int seed = Constants.FetchParameterInt(command, "seed");
				int border = Constants.FetchParameterInt(command, "border");
				Output.PrintToScreen("About to generate set of landscape image...");
				for (int i = 0; i < 10; i++) {
					Admin.GenerateLandscape(width, height, seed+i, border);
				}
				Output.PrintToScreen("Landscape image generated.");
			}
			if (command.indexOf("GENERATE_MAP")==0)
			{
				valid = true;
				Output.PrintToScreen("About to generate map");
				String output_string = Admin.GenerateMap(Constants.FetchParameter(original_command, "file", false));
				Output.PrintToScreen(output_string);
			}
			if (command.compareTo("GARBAGE_COLLECT")==0)
			{
				valid = true;
				long cur_memory_used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
				Output.PrintToScreen("About to run garbage collector...");
				System.gc();
				long memory_used_post_gc = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
				Output.PrintToScreen("Garbage collect complete. (" + cur_memory_used + " -> " + memory_used_post_gc + " in use)");
			}
			if (command.compareTo("HELP")==0)
			{
				valid = true;
				Output.PrintToScreen(" ADD_ADVANCE nation=<name>|advance=<number> - Add advance to a nation");
				Output.PrintToScreen(" ADD_ADVANCE_POINTS nation=<name>|amount=<number> - Add advance points to a nation");
				Output.PrintToScreen(" ADD_COUNTDOWN nation=<name>|amount=<number> - Add the amount to the nation's rebirth countdown");
				Output.PrintToScreen(" ADD_COUNTDOWN_ALL amount=<number> - Add the amount to all nations' rebirth countdowns");
				Output.PrintToScreen(" ADD_CREDITS nation=<name>|amount=<number>|purchased=<0 or 1> - Add credits to a nation");
				Output.PrintToScreen(" ADD_CREDITS_ALL amount=<number>|purchased=<0 or 1> - Add credits to all nations");
				Output.PrintToScreen(" ADD_DAY_COUNTDOWN_ALL - Add one day's rebirth countdown to all nations");
				Output.PrintToScreen(" ADD_ENERGY nation=<name>|amount=<number> - Add energy amount to a nation");
				Output.PrintToScreen(" ADD_MANPOWER nation=<name>|amount=<number> - Add manpower amount to a nation");
				Output.PrintToScreen(" ADD_MANPOWER_ALL amount=<number> - Add manpower amount to all nations");
				Output.PrintToScreen(" ADD_PRIZE_MONEY_HISTORY nation=<name>|amount=<number> - Add the given number of cents to the nation's prize_money_history");
				Output.PrintToScreen(" ADD_VOUCHER code=<code>|amount=<number> - Add credits to the voucher with the given code");
				Output.PrintToScreen(" ADD_VOUCHERS_FROM_FILE file=<filename.tsv> - Add credits to vouchers from the given file");
				Output.PrintToScreen(" ADD_XP nation=<name>|amount=<number> - Add XP to a nation");
				Output.PrintToScreen(" ADD_XP_USER user=<name>|amount=<number> - Add XP to a user (not their nation)");
				Output.PrintToScreen(" ALLOW_EMBLEM user=<name>|emblem=<index> - Allow the player to use the specified restricted emblem");
				Output.PrintToScreen(" ANNOUNCE <text of announcement> - Send announcement to all logged in client");
				Output.PrintToScreen(" ASSOCIATE_USERS user1=<name>|user2=<name> - Coassociate the two users and their associated users and devices with one another");
				Output.PrintToScreen(" AWARD_LEVELS nation=<name>|amount=<number> - Award levels to a nation");
				Output.PrintToScreen(" AWARD_PRIZE nation=<name>|amount=<cents> - Add prize money to a nation");
				Output.PrintToScreen(" BACKUP - backup game data");
				Output.PrintToScreen(" BLOCK_INFO x=<x>|y=<y> - Info about the block at the given coords");
				Output.PrintToScreen(" BUILD ID=<buildID>|x=<x>|y=<y> - Build the object with the given ID at the given position");
				Output.PrintToScreen(" CHAT_BAN_USER user=<name>|hours=<hours> - Ban user from chat for given time");
				Output.PrintToScreen(" CLEAR_COMPLAINTS - Clear all active complaints");
				Output.PrintToScreen(" CLEAR_DATABASE - Clear the entire game database");
				Output.PrintToScreen(" CLEAR_DEVICE id=<device_id> - Reset the record for the device with the given ID");
				Output.PrintToScreen(" CLEAR_DEVICE_FEALTY ID=<deviceID> - Clear any fealty associated with this device");
				Output.PrintToScreen(" CLEAR_PLAYER_DATABASE - Clear the entire player database");
				Output.PrintToScreen(" CLEAR_TEMPS nation=<name> - Removes all of the nation's temporary techs");
				Output.PrintToScreen(" CLIENTS - lists all currently connected clients");
				Output.PrintToScreen(" CONFINE_NATIONS - Remove nations from any blocks that have a lower level limit than the nation's level");
				Output.PrintToScreen(" CREATE_USER_REPORT - Create a report of user statistics, add to log_report.txt");
				Output.PrintToScreen(" DELETE_ACCOUNT_DATABASE - Delete all tables on the accounts database");
				Output.PrintToScreen(" DELETE_GAME_DATABASE - Delete all tables on the game server database");
				Output.PrintToScreen(" DELETE_NATION nation=<name> - Delete the nation");
				Output.PrintToScreen(" DELETE_USER user=<name> - Delete the user's account if not in a nation");
				Output.PrintToScreen(" DEVICE_INFO id=<device_id> - Show info about the device with the given ID");
				Output.PrintToScreen(" DISASSOC_DEVICE id=<device_id> - Disassociate the device with the given ID from their playerID");
				Output.PrintToScreen(" DEMO_HOMELANDS - Create raid nemesis stock nations");
				Output.PrintToScreen(" DEMO_POPULATE - Populate the lower part of the map with demo nations");
				Output.PrintToScreen(" EMAIL_INFO email=<email> - Show info about the given e-mail address");
				Output.PrintToScreen(" EMERGENCY - Send out emergency notification e-mails");
				Output.PrintToScreen(" FORCE_UPDATE_DATABASE - Force a database update immediately");
				Output.PrintToScreen(" GAME_BAN_USER user=<name>|hours=<hours> - Ban user from login for given time (0 to unban)");
				Output.PrintToScreen(" GENERATE_LANDSCAPE w=<width>|h=<height>|seed=<seed>[|border=<max_border_width>] - Generate a landscape image with the given dimensions");
				Output.PrintToScreen(" GENERATE_LANDSCAPE_SET w=<width>|h=<height>|seed=<seed> - Generate 10 landscape images with the given dimensions and 1st seed");
				Output.PrintToScreen(" GARBAGE_COLLECT - Tell the Java virtual machine to run the garbage collector");
				Output.PrintToScreen(" HELP - Displays this list of functions");
				Output.PrintToScreen(" LIST_COMPLAINTS - lists all active complaints");
				Output.PrintToScreen(" LIST_CONTACTS user=<username> - lists each of the user's contacts and their weights");
				Output.PrintToScreen(" LIST_CONTENDERS - lists all current tournament contenders");
				Output.PrintToScreen(" LIST_FILES - lists all files in current directory");
				Output.PrintToScreen(" LIST_FOLLOWERS user=<username> - lists each of the user's folowers");
				Output.PrintToScreen(" LIST_INCOGNITO_NATIONS - lists all incognito nations");
				Output.PrintToScreen(" LIST_TEMPS nation=<name> - lists all of the nation's temporary techs");
				Output.PrintToScreen(" LOAD_LANDSCAPE [ID=<ID>|]file=<.png image filename> - Load landscape data from image file.");
				Output.PrintToScreen(" LOG_ATTACK - toggle logging of attacks");
				Output.PrintToScreen(" LOG_AWARDS - toggle logging of awards");
				Output.PrintToScreen(" LOG_CHAT - toggle logging of chat");
				Output.PrintToScreen(" LOG_DEBUG - toggle logging of debug information");
				Output.PrintToScreen(" LOG_ENTER - toggle logging of enter/exit game events");
				Output.PrintToScreen(" LOG_EVENTS - toggle logging of input events");
				Output.PrintToScreen(" LOG_INPUT - toggle logging of each input event");
				Output.PrintToScreen(" LOG_LOAD - toggle logging of load statistics");
				Output.PrintToScreen(" LOG_LOGIN - toggle logging of login/logout events");
				Output.PrintToScreen(" LOG_SEND - toggle logging of message sends to log_send.txt");
				Output.PrintToScreen(" LOG_UPDATE - toggle logging of update events to log_update.txt");
				Output.PrintToScreen(" LOG_OBJECTS - Log the count of each object to log_objects.txt");
				Output.PrintToScreen(" LOG_PURCHAED_CREDITS - Log the count of each nation's purchased credits to log_purchased_credits.txt");
				Output.PrintToScreen(" LOG_WINNINGS - Log all nations' current winnings to log_winnings.txt");
				Output.PrintToScreen(" MAKE_ABSENTEE user=<name> - Cause user to be considered absentee");
				Output.PrintToScreen(" MEMORY_STATS - Return stats on current Java Virtual Machine memory usage");
				Output.PrintToScreen(" MAP_MODIFIED ID=<ID> - Record that the map with this ID has been modified so that clients will download the new map image");
				Output.PrintToScreen(" MIGRATE_NATION nation=<name>|x=<x>|y=<y> - Migrate the given nation, around the given location");
				Output.PrintToScreen(" NATIONS - lists all connected nations and clients");
				Output.PrintToScreen(" NATION_INFO nation=<name> - Lists info about the nation");
				Output.PrintToScreen(" PLAYER_INFO user=<name> - Lists info about the user's player data");
				Output.PrintToScreen(" PROCESS_PURCHASE user=<name>|package=<number> - Purchase given package.");
				Output.PrintToScreen(" PLAYER_INFO user=<name> - Display the player's info.");
				Output.PrintToScreen(" PUBLISH_RANKS - update ranks files.");
				Output.PrintToScreen(" QUIT - disconnects all clients and exits");
				Output.PrintToScreen(" REBIRTH_NATION nation=<name> - Rebirth the given nation");
				Output.PrintToScreen(" REFRESH_ALL_NATIONS - Remove and re-add each nation's technologies and resources");
				Output.PrintToScreen(" REFRESH_NATION nation=<name> - Remove and re-add the nation's technologies and resources");
				Output.PrintToScreen(" RELOAD_NATION nation=<name> - Reloads the nation's data");
				Output.PrintToScreen(" RELOAD_NATIONTECH nation=<name> - Reloads the nation's data and tech data");
				Output.PrintToScreen(" RELOAD_USER user=<name> - Reloads the user's data");
				Output.PrintToScreen(" REMOVE_ADVANCE nation=<name>|advance=<number> - Remove advance from a nation");
				Output.PrintToScreen(" REMOVE_EMBLEM user=<name>|emblem=<index> - Disallow the player from using the specified restricted emblem");
				Output.PrintToScreen(" REMOVE_NATION_RANKS [nation=<name>] - Remove the nation from the ranks lists");
				Output.PrintToScreen(" REMOVE_USER_RANKS [user=<name>] - Remove the user from the ranks lists");
				Output.PrintToScreen(" RENAME_NATION [nationID=<ID>][old=<name>][|new=<name>] - Rename the nation");
				Output.PrintToScreen(" RENAME_USER [userID=<ID>]|[old=<name>][|new=<name>] - Rename the user");
				Output.PrintToScreen(" RENEW_ALL - Renew all nations, user accounts, ranks, and the map.");
				Output.PrintToScreen(" REPAIR_ALL_NATIONS - Sanity check data for each nation and repair if necessary");
				Output.PrintToScreen(" REPAIR_ALL_USERS - Sanity check data for each user and repair if necessary");
				Output.PrintToScreen(" REPAIR_NATION nation=<name> - Sanity check data for this nation and repair if necessary");
				Output.PrintToScreen(" RESET_ADVANCES nation=<name> - Nation's advances are removed and advance points returned");
				Output.PrintToScreen(" RESET_PASSWORD user=<name>[|password=<password>] - Change a player account's password");
				Output.PrintToScreen(" RESET_PREV_LOGOUT user=<name> - Reset a user's prev_logout_time, so that username doesn't count toward the player's recently used");
				Output.PrintToScreen(" RESTORE_DEFAULT_PRICES - Restore the prices of all advances to their defaults");
				Output.PrintToScreen(" RETURN_TO_HOME_NATIONS - Return all users to their home nations");
				Output.PrintToScreen(" RUN file=<script filename> - Runs the given script file containing admin commands");
				Output.PrintToScreen(" SEPARATE_USERS user1=<name>|user2=<name> - Disassociate the two users and their associated users from one another");
				Output.PrintToScreen(" SEPARATE_USER_FROM_DEVICE user=<name>|device=<deviceID> - Disassociate the user (and assoc users) from the device");
				Output.PrintToScreen(" SET_EMAIL user=<name>|email=<email> - Change a player account's e-mail address");
				Output.PrintToScreen(" SET_SHARE_FILLS nation=<name>|amount=<number> - Set the nation's manpower and energy shares to the given fill percent amount 0->100");
				Output.PrintToScreen(" SET_PRICE ID=<ID>|price=<price> - Set the price of an advance");
				Output.PrintToScreen(" SET_USER_RANK user=<name>|rank=<number> - Set the user's rank");
				Output.PrintToScreen(" SUSPECT nation=<name>|minutes=<number> - Log a suspect nation's actions temporarily");
				Output.PrintToScreen(" SYNC_LEVELS_TO_ADVANCES - Redetermine each nation's level and XP so that it matches their number of advances");
				Output.PrintToScreen(" TEST_COMMAND - Used for whatever is currently being tested");
				Output.PrintToScreen(" THREAD_STATUS - Display time since last pause for each thread");
				Output.PrintToScreen(" TOGGLE_ADMIN user=<name> - Toggle admin status on/off for this user");
				Output.PrintToScreen(" TOGGLE_ADMIN_LOGIN_ONLY - Toggles whether login is only allowed from admin IP address");
				Output.PrintToScreen(" TOGGLE_BACKUP_THREAD - turns BackupThread on/off");
				Output.PrintToScreen(" TOGGLE_EMAIL_THREAD - turns EmailThread on/off");
				Output.PrintToScreen(" TOGGLE_GARBAGE_COLLECT - turns periodic garbage collection on/off");
				Output.PrintToScreen(" TOGGLE_MULTI_CLIENT - toggles allowing multi clients from one machine");
				Output.PrintToScreen(" TOGGLE_PROCESS_EVENTS - turns event processing on/off");
				Output.PrintToScreen(" TOGGLE_REGULAR_UPDATE - turns regular updates on/off");
				Output.PrintToScreen(" TOGGLE_SUPER nation=<name> - Toggle omnipotence on/off for this nation");
				Output.PrintToScreen(" TOGGLE_UPDATE_THREAD - turns UpdateThread on/off");
				Output.PrintToScreen(" TOGGLE_VET [user=<name>][nation=<name>] - Toggle veteran status on/off for the user or nation");
				Output.PrintToScreen(" TRACK_CLIENT ID=<fragment of client ID> - Logs messages to/from any client matching the given ID");
				Output.PrintToScreen(" TRANSFER_PLAYER_XP from=<name>|to=<name> - Transfer XP to 'to' user");
				Output.PrintToScreen(" UNMUTE_ALL user=<name> - Unmute all users and devices for this user");
				Output.PrintToScreen(" UPDATE_ALL_AREAS - Update the area of every nation on the map");
				Output.PrintToScreen(" UPDATE_AREA nation=<name> - Update the nation's area");
				Output.PrintToScreen(" UPDATE_NATION_RANKS nation=<name> - Updates the nation's ranks list records");
				Output.PrintToScreen(" UPDATE_TOURNAMENT - Updates the tournament for the current day");
				Output.PrintToScreen(" USER_INFO user=<name> - Lists info about the user's user data");
				Output.PrintToScreen(" WARNING_BRIEF - Give warning immediately before brief server downtime");
				Output.PrintToScreen(" WARNING_0 - Give warning immediately before server downtime");
				Output.PrintToScreen(" WARNING_1 - Give warning 1 minute before server downtime");
				Output.PrintToScreen(" WARNING_5 - Give warning 5 minutes before server downtime");
			}
			if (command.compareTo("LIST_COMPLAINTS")==0)
			{
				valid = true;
				Output.PrintToScreen("Complaints:");
				Admin.ListComplaints();
			}
			if (command.indexOf("LIST_CONTACTS")!=-1)
			{
				valid = true;
				Admin.ListContacts(Constants.FetchParameter(command, "USER", false));
			}
			if (command.indexOf("LIST_CONTENDERS")!=-1)
			{
				valid = true;
				TournamentData.instance.ListContenders();
			}
			if (command.indexOf("LIST_FILES")!=-1)
			{
				valid = true;
				Admin.ListFiles();
			}
			if (command.indexOf("LIST_FOLLOWERS")!=-1)
			{
				valid = true;
				Admin.ListFollowers(Constants.FetchParameter(command, "USER", false));
			}
			if (command.indexOf("LIST_INCOGNITO_NATIONS")!=-1)
			{
				valid = true;
				Admin.ListIncognitoNations();
			}
			if (command.indexOf("LIST_TEMPS")!=-1)
			{
				valid = true;
				Admin.ListTemps(Constants.FetchParameter(command, "NATION", false));
			}
			if (command.indexOf("LOAD_LANDSCAPE")==0)
			{
				valid = true;

				int mapID = Constants.FetchParameterInt(command, "ID");
				String filename = Constants.FetchParameter(original_command, "file", false);
				int really = Constants.FetchParameterInt(command, "really");

				if ((mapID == Constants.MAINLAND_MAP_ID) && (really != 1))
				{
					Output.PrintToScreen("To replace mainland map (ID " + Constants.MAINLAND_MAP_ID + "), include parameter really=1.");
					return false;
				}

				Output.PrintToScreen("About to load landscape for map ID " + mapID);
				String output_string = Admin.LoadLandscape(mapID, filename);
				Output.PrintToScreen(output_string);

				success = output_string.equals("Landscape loaded.");
			}
			if (command.compareTo("LOG_ATTACK")==0)
			{
				valid = true;
				boolean val = WOCServer.ToggleLogFlag(Constants.LOG_ATTACK);
				Output.PrintToScreen("LOG_ATTACK turned " + (val ? "on" : "off"));
			}
			if (command.compareTo("LOG_AWARDS")==0)
			{
				valid = true;
				boolean val = WOCServer.ToggleLogFlag(Constants.LOG_AWARDS);
				Output.PrintToScreen("LOG_AWARDS turned " + (val ? "on" : "off"));
			}
			if (command.compareTo("LOG_CHAT")==0)
			{
				valid = true;
				boolean val = WOCServer.ToggleLogFlag(Constants.LOG_CHAT);
				Output.PrintToScreen("LOG_CHAT turned " + (val ? "on" : "off"));
			}
			if (command.compareTo("LOG_DEBUG")==0)
			{
				valid = true;
				boolean val = WOCServer.ToggleLogFlag(Constants.LOG_DEBUG);
				Output.PrintToScreen("LOG_DEBUG turned " + (val ? "on" : "off"));
			}
			if (command.compareTo("LOG_ENTER")==0)
			{
				valid = true;
				boolean val = WOCServer.ToggleLogFlag(Constants.LOG_ENTER);
				Output.PrintToScreen("LOG_ENTER turned " + (val ? "on" : "off"));
			}
			if (command.compareTo("LOG_EVENTS")==0)
			{
				valid = true;
				boolean val = WOCServer.ToggleLogFlag(Constants.LOG_EVENTS);
				Output.PrintToScreen("LOG_EVENTS turned " + (val ? "on" : "off"));
			}
			if (command.compareTo("LOG_INPUT")==0)
			{
				valid = true;
				boolean val = WOCServer.ToggleLogFlag(Constants.LOG_INPUT);
				Output.PrintToScreen("LOG_INPUT turned " + (val ? "on" : "off"));
			}
			if (command.compareTo("LOG_LOAD")==0)
			{
				valid = true;
				boolean val = WOCServer.ToggleLogFlag(Constants.LOG_LOAD);
				Output.PrintToScreen("LOG_LOAD turned " + (val ? "on" : "off"));
			}
			if (command.compareTo("LOG_LOGIN")==0)
			{
				valid = true;
				boolean val = WOCServer.ToggleLogFlag(Constants.LOG_LOGIN);
				Output.PrintToScreen("LOG_LOGIN turned " + (val ? "on" : "off"));
			}
			if (command.compareTo("LOG_SEND")==0)
			{
				valid = true;
				boolean val = WOCServer.ToggleLogFlag(Constants.LOG_SEND);
				Output.PrintToScreen("LOG_SEND turned " + (val ? "on" : "off"));
			}
			if (command.compareTo("LOG_UPDATE")==0)
			{
				valid = true;
				boolean val = WOCServer.ToggleLogFlag(Constants.LOG_UPDATE);
				Output.PrintToScreen("LOG_UPDATE turned " + (val ? "on" : "off"));
			}
			if (command.compareTo("LOG_OBJECTS")==0)
			{
				valid = true;
				LogObjects();
				Output.PrintToScreen("Object counts logged.");
			}
			if (command.compareTo("LOG_PURCHASED_CREDITS")==0)
			{
				valid = true;
				LogPurchasedCredits();
				Output.PrintToScreen("Purchased credits logged.");
			}
			if (command.indexOf("LOG_WINNINGS")!=-1)
			{
				valid = true;
				LogWinnings();
				Output.PrintToScreen("Winnings logged.");
			}
			if (command.indexOf("MAKE_ABSENTEE")!=-1)
			{
				valid = true;
				//String user_name = Constants.FetchParameter(command, "USER", false);

				Admin.MakeAbsentee(Constants.FetchParameter(command, "USER", false));
			}

			if (command.indexOf("MAP_MODIFIED")!=-1)
			{
				valid = true;

				int mapID = Constants.FetchParameterInt(command, "ID");

				// Record time when the map with the given ID was previously modified.
				GlobalData.instance.map_modified_times.put(mapID, Constants.GetTime());
				DataManager.MarkForUpdate(GlobalData.instance);

				Output.PrintToScreen("Recorded that map with ID " + mapID + " has been modified.");
			}
			if (command.compareTo("MEMORY_STATS")==0)
			{
				valid = true;
				WOCServer.OutputMemoryStats();
			}
			if (command.indexOf("MIGRATE_NATION")!=-1)
			{
				valid = true;
				Admin.MigrateNation(Constants.FetchParameter(command, "NATION", false), Constants.FetchParameterInt(command, "X"), Constants.FetchParameterInt(command, "Y"));
			}
			if (command.compareTo("NATIONS")==0)
			{
				valid = true;
				Output.PrintToScreen("");
				WOCServer.listNations();
				Output.PrintToScreen("");
			}
			if (command.indexOf("NATION_INFO")!=-1)
			{
				valid = true;
				Admin.NationInfo(null, Constants.FetchParameter(command, "NATION", false), true);
			}
			if (command.indexOf("PLAYER_INFO")!=-1)
			{
				valid = true;
				Admin.PlayerInfo(Constants.FetchParameter(command, "USER", false));
			}
			if (command.indexOf("PROCESS_PURCHASE")!=-1)
			{
				valid = true;
				Admin.ProcessPurchase(Constants.FetchParameter(command, "USER", false),Constants.FetchParameterInt(command, "PACKAGE"));
			}
			if (command.indexOf("PUBLISH_RANKS")!=-1)
			{
				valid = true;
				RanksData.instance.PublishAllRanks();
				Output.PrintToScreen(Constants.GetShortTimeString() + " Done updating ranks files");
			}
			if (command.compareTo("QUIT")==0)
			{
				valid = true;
				WOCServer.Quit();
			}
			if (command.indexOf("REBIRTH_NATION")!=-1)
			{
				valid = true;
				Admin.RebirthNation(Constants.FetchParameter(command, "NATION", false));
			}
			if (command.compareTo("REFRESH_ALL_NATIONS")==0)
			{
				valid = true;
				String output_string = Admin.RefreshAllNations();
				Output.PrintToScreen(output_string);
			}
			if (command.indexOf("REFRESH_NATION")!=-1)
			{
				valid = true;
				String nation_name = Constants.FetchParameter(command, "NATION", false);

				int nationID = DataManager.GetNameToIDMap(Constants.DT_NATION, nation_name);
				NationData nation_data = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

				if (nation_data == null)
				{
					Output.PrintToScreen("Unknown nation: '" + nation_name + "'");
				}
				else
				{
					// Refresh the nation
					Admin.RefreshNation(nationID);

					// Output message
					Output.PrintToScreen(nation_name + " has been refreshed");
				}
			}
			if (command.indexOf("RELOAD_NATION")!=-1)
			{
				valid = true;
				//String nation_name = Constants.FetchParameter(command, "NATION", false);

				Admin.ReloadNation(null, Constants.FetchParameter(command, "NATION", false), true);
			}
			if (command.indexOf("RELOAD_NATIONTECH")!=-1)
			{
				valid = true;
				//String nation_name = Constants.FetchParameter(command, "NATION", false);

				Admin.ReloadNationTechs(null, Constants.FetchParameter(command, "NATION", false),true);
			}
			if (command.indexOf("RELOAD_USER")!=-1)
			{
				valid = true;
				//String user_name = Constants.FetchParameter(command, "USER", false);
				Admin.ReloadUser(Constants.FetchParameter(command, "USER", false));
			}
			if (command.indexOf("REMOVE_ADVANCE")!=-1)
			{
				valid = true;
				Admin.RemoveAdvance(Constants.FetchParameter(command, "NATION", false),Constants.FetchParameterInt(command, "ADVANCE"));
			}
			if (command.indexOf("REMOVE_NATION_RANKS")!=-1)
			{
				valid = true;
				String nation_name = Constants.FetchParameter(command, "NATION", false);

				int nationID = DataManager.GetNameToIDMap(Constants.DT_NATION, nation_name);
				NationData nation_data = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

				if (nation_data == null)
				{
					Output.PrintToScreen("Unknown nation: '" + nation_name + "'");
				}
				else
				{
					// Remove the nation from the ranks lists.
					RanksData.instance.RemoveNationFromRanks(nationID);

					// Output message
					Output.PrintToScreen("Nation " + nation_name + " has been removed from the ranks lists.");
				}
			}
			if (command.indexOf("REMOVE_USER_RANKS")!=-1)
			{
				valid = true;
				String user_name = Constants.FetchParameter(command, "USER", false);

				int userID = DataManager.GetNameToIDMap(Constants.DT_USER, user_name);
				UserData user_data = (UserData)DataManager.GetData(Constants.DT_USER, userID, false);

				if (user_data == null)
				{
					Output.PrintToScreen("Unknown user: '" + user_name + "'");
				}
				else
				{
					// Remove the user from the ranks lists.
					RanksData.instance.RemoveUserFromRanks(userID);

					// Output message
					Output.PrintToScreen("User " + user_name + " has been removed from the ranks lists.");
				}
			}
			if (command.indexOf("RENAME_NATION")!=-1)
			{
				valid = true;
				int nationID = Constants.FetchParameterInt(command, "nationid");

				if (nationID == 0) {
					nationID = DataManager.GetNameToIDMap(Constants.DT_NATION, Constants.FetchParameter(original_command, "old", false));
				}

				Admin.RenameNation(nationID,Constants.FetchParameter(original_command, "new", false));
			}
			if (command.indexOf("RENAME_USER")!=-1)
			{
				valid = true;
				int userID = Constants.FetchParameterInt(command, "userid");

				if (userID == 0) {
					userID = DataManager.GetNameToIDMap(Constants.DT_USER, Constants.FetchParameter(original_command, "old", false));
				}

				Admin.RenameUser(userID,Constants.FetchParameter(original_command, "new", false));
			}
			if (command.indexOf("RENEW_ALL")!=-1)
			{
				valid = true;
				Output.PrintToScreen("This commmand compiled out.");
				//Admin.RenewAll();
			}
			if (command.compareTo("REPAIR_ALL_NATIONS")==0)
			{
				valid = true;
				String output_string = Admin.RepairAllNations();
				Output.PrintToScreen(output_string);
			}
			if (command.compareTo("REPAIR_ALL_USERS")==0)
			{
				valid = true;
				String output_string = Admin.RepairAllUsers();
				Output.PrintToScreen(output_string);
			}
			if (command.indexOf("REPAIR_NATION")!=-1)
			{
				valid = true;
				String nation_name = Constants.FetchParameter(command, "NATION", false);

				int nationID = DataManager.GetNameToIDMap(Constants.DT_NATION, nation_name);
				NationData nation_data = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

				if (nation_data == null)
				{
					Output.PrintToScreen("Unknown nation: '" + nation_name + "'");
				}
				else
				{
					// Repair the nation
					nation_data.Repair();

					// Output message
					Output.PrintToScreen(nation_name + " has been repaired");
				}
			}
			if (command.indexOf("RESET_ADVANCES")!=-1)
			{
				valid = true;
				String nation_name = Constants.FetchParameter(command, "NATION", false);

				int nationID = DataManager.GetNameToIDMap(Constants.DT_NATION, nation_name);
				NationData nation_data = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

				if (nation_data == null)
				{
					Output.PrintToScreen("Unknown nation: '" + nation_name + "'");
				}
				else
				{
					// Reset the nation's advances
					Gameplay.ResetAdvances(nationID, true);

					// Output message
					Output.PrintToScreen(nation_name + "'s advances have been reset, leaving it with " + nation_data.advance_points + " advance points.");
				}
			}
			if (command.indexOf("RESET_PASSWORD")!=-1)
			{
				valid = true;
				String user_name = Constants.FetchParameter(original_command, "USER", false);
				String new_password = Constants.FetchParameter(original_command, "PASSWORD", false);

				Admin.ResetPassword(user_name, new_password);
			}
			if (command.indexOf("RESET_PREV_LOGOUT")!=-1)
			{
				valid = true;
				String user_name = Constants.FetchParameter(original_command, "USER", false);

				Admin.ResetPrevLogout(user_name);
			}
			if (command.indexOf("RESTORE_DEFAULT_PRICES")!=-1)
			{
				valid = true;
				Admin.RestoreDefaultPrices();
			}
			if (command.indexOf("RETURN_TO_HOME_NATIONS")!=-1)
			{
				valid = true;
				Admin.ReturnToHomeNations();
			}
			if (command.indexOf("RUN ") != -1)
			{
				valid = true;
				success = Admin.RunScript(Constants.FetchParameter(original_command, "FILE", false));
			}
			if (command.indexOf("SEPARATE_USERS")!=-1)
			{
				valid = true;
				Admin.SeparateUsers(Constants.FetchParameter(original_command, "user1", false), Constants.FetchParameter(original_command, "user2", false));
			}
			if (command.indexOf("SEPARATE_USER_FROM_DEVICE")!=-1)
			{
				valid = true;
				Admin.SeparateUserFromDevice(Constants.FetchParameter(original_command, "user", false), Constants.FetchParameterInt(original_command, "device"));
			}
			if (command.indexOf("SET_EMAIL")!=-1)
			{
				valid = true;
				String user_name = Constants.FetchParameter(original_command, "USER", false);
				String new_email = Constants.FetchParameter(original_command, "EMAIL", false);

				Admin.SetEmail(user_name, new_email);
			}
			if (command.indexOf("SET_SHARE_FILLS")!=-1)
			{
				valid = true;
				Admin.SetShareFills(Constants.FetchParameter(command, "NATION", false), Constants.FetchParameterInt(command, "AMOUNT"));
			}
			if (command.indexOf("SET_PRICE")!=-1)
			{
				valid = true;
				int techID = Constants.FetchParameterInt(command, "ID");
				int price = Constants.FetchParameterInt(command, "PRICE");

				Admin.SetPrice(techID, price);
			}
			if (command.indexOf("SET_TOURNAMENT_DAY")!=-1)
			{
				valid = true;
				int day = Constants.FetchParameterInt(command, "day");

				TournamentData.instance.SetTournamentDay(day);
			}
			if (command.indexOf("SET_USER_RANK")!=-1)
			{
				valid = true;
				//String user_name = Constants.FetchParameter(command, "USER", false);
				Admin.SetUserRank(Constants.FetchParameter(command, "USER", false), Constants.FetchParameterInt(command, "RANK"));
			}
			if (command.indexOf("SUSPECT")!=-1)
			{
				valid = true;
				//String user_name = Constants.FetchParameter(command, "USER", false);
				Admin.SuspectNation(Constants.FetchParameter(command, "NATION", false), Constants.FetchParameterInt(command, "MINUTES"));
			}
			if (command.indexOf("SYNC_LEVELS_TO_ADVANCES")!=-1)
			{
				valid = true;
				Admin.SyncLevelsToAdvances();
			}
			if (command.compareTo("TEST_COMMAND")==0)
			{
				// Reserved for whatever I'm testing
				valid = true;

				// Reset every nation's winnings amount for each orb.
				int i;
				for (int nationID = 1; nationID <= DataManager.GetHighestDataID(Constants.DT_NATION); nationID++)
				{
					// Get the current nation's data
					NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

					// If there is no nation with the current nationID, continue on to the next.
					if (nationData == null) {
						continue;
					}

					// Reset the record of this nation's winnings on this orb to 0.
					for (i = 0; i < nationData.goals_winnings.size(); i++) {
						nationData.goals_winnings.set(i, 0.0f);
					}
				}

				// Reset all the goals_total_awarded records to 0.
				for (i = 0; i < RanksData.instance.goals_total_awarded.size(); i++) {
					RanksData.instance.goals_total_awarded.set(i, 0.0f);
				}

				// Clear all the goals_ranks lists.
				for (i = 0; i < RanksData.instance.goals_ranks.size(); i++) {
					RanksData.instance.goals_ranks.get(i).Clear();
				}

				// Clear all the goals_ranks_monthly lists.
				for (i = 0; i < RanksData.instance.goals_ranks_monthly.size(); i++) {
					RanksData.instance.goals_ranks_monthly.get(i).Clear();
				}

				/*
				for (int nationID = 1; nationID <= DataManager.GetHighestDataID(Constants.DT_NATION); nationID++)
				{
					// Get the current nation's data
					NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

					// If there is no nation with the current nationID, continue on to the next.
					if (nationData == null) {
						continue;
					}

					// Remove the nation's purchased credits.
					if (nationData.game_money_purchased > 0)
					{
						Output.PrintToScreen("Nation " + nationData.name + " (" + nationID + ") removing " + nationData.game_money_purchased + " purchased credits.");
						nationData.game_money = Math.max(0f, nationData.game_money - nationData.game_money_purchased);
						nationData.game_money_purchased = 0f;
						Output.PrintToScreen("Nation " + nationData.name + " (" + nationID + ") has " + nationData.game_money + " remaining credits.");
					}

					// Remove the nation's won credits.
					if (nationData.game_money_won > 0)
					{
						Output.PrintToScreen("Nation " + nationData.name + " (" + nationID + ") removing " + nationData.game_money_won + " won credits.");
						nationData.game_money = Math.max(0f, nationData.game_money - nationData.game_money_won);
						nationData.game_money_won = 0f;
						Output.PrintToScreen("Nation " + nationData.name + " (" + nationID + ") has " + nationData.game_money + " remaining credits.");
					}

					// Reduce the nation's rebirth_count to no greater than 10.
					if (nationData.rebirth_count > 10)
					{
						Output.PrintToScreen("Reducing nation " + nationData.name + " (" + nationID + ")'s rebirth-count from " + nationData.rebirth_count + " to 10.");
						nationData.rebirth_count = 10;
					}

					// Remove the nation's current prize winnings.
					if (nationData.prize_money > 0)
					{
						Output.PrintToScreen("Removing nation " + nationData.name + " (" + nationID + ")'s prize winnings of $" + nationData.prize_money + ".");
						nationData.prize_money = 0f;
					}

					// Reset all of the nation's ranks.
					RanksData.instance.RemoveNationFromRanks(nationID);

					// Mark the nation's data to be updated.
					DataManager.MarkForUpdate(nationData);
				}

				Output.PrintToScreen("About to reset user ranks.");

				for (int userID = 1; userID <= DataManager.GetHighestDataID(Constants.DT_USER); userID++)
				{
					// Get the current user's data
					UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, userID, false);

					// If there is no user with the current userID, continue on to the next.
					if (userData == null) {
						continue;
					}

					if (userData.xp > 0) {
						Output.PrintToScreen("Removing all " + userData.xp + " xp from user " + userData.name + " (" + userID + ").");
					}

					// Reset the user's XP.
					userData.xp = 0;
					userData.xp_monthly = 0;

					// Reset all of the user's ranks.
					RanksData.instance.RemoveUserFromRanks(userID);

					// Mark the user's data to be updated.
					DataManager.MarkForUpdate(userData);
				}

				// Publish ranks lists.
				Output.PrintToScreen("About to publish ranks.");
				RanksData.instance.PublishAllRanks();

				Output.PrintToScreen("Done.");
				*/

				/*
				int time_cutoff = Constants.GetTime() - (Constants.SECONDS_PER_DAY * 14);
				for (int nationID = 1; nationID <= DataManager.GetHighestDataID(Constants.DT_NATION); nationID++)
				{
					// Get the current nation's data
					NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

					// If there is no nation with the current nationID, continue on to the next.
					if (nationData == null) {
						continue;
					}

					if ((nationData.pending_xp > 0) && (nationData.prev_unite_time < time_cutoff))
					{
						Output.PrintToScreen("Nation " + nationData.name + " has " + nationData.pending_xp + " pending XP from unite " + ((Constants.GetTime() - nationData.prev_unite_time) / Constants.SECONDS_PER_DAY) + " days ago.");

						// Remove the nation's pending XP.
						nationData.pending_xp = 0;

						// Mark the nation's data to be updated.
						DataManager.MarkForUpdate(nationData);
					}
				}
        */
				//// TEST call to AssociateCorrelatedDevices().
				//DeviceData.AssociateCorrelatedDevices();
				/*
				for (int deviceID = 1; deviceID <= DataManager.GetHighestDataID(Constants.DT_DEVICE); deviceID++)
				{
					// Get the current device's data
					DeviceData deviceData = (DeviceData)DataManager.GetData(Constants.DT_DEVICE, deviceID, false);

					// If there is no device with the current userID, continue on to the next.
					if (deviceData == null) {
						continue;
					}

					// Skip any device that has blank UID.
					if (deviceData.name.equals("")) {
						continue;
					}

					// Iterate through each of the device's users
					boolean has_vet_user = false;
					UserData cur_user_data;
					for (int cur_user_index = 0; cur_user_index < deviceData.users.size(); cur_user_index++)
					{
						// Get the current user's data
						cur_user_data = (UserData)DataManager.GetData(Constants.DT_USER, deviceData.users.get(cur_user_index), false);

						if ((cur_user_data != null) && cur_user_data.veteran)
						{
							has_vet_user = true;
							break;
						}
					}

					// If this has at least one veteran user associated with it...
					if (has_vet_user)
					{
						// Get (or create) the game-server-independent record of this user's device.
						DeviceAccountData device_account = DeviceDB.ReadDeviceAccount(deviceData.name);
						if (device_account == null) {
							device_account = DeviceDB.CreateNewDeviceAccount(deviceData.name, deviceData.device_type);
						}

						// If this device account is not yet marked as being veteran, mark it and save it.
						if (!device_account.veteran)
						{
							device_account.veteran = true;
							DeviceDB.WriteDeviceAccount(device_account);
							Output.PrintToScreen("Marked as vet device ID: " + deviceData.ID + ", UID: " + deviceData.name + ", type: " + deviceData.device_type);
						}
					}

					//// Log this device.
					//Constants.WriteToLog("log_devices.txt", Constants.GetTimestampString() + ": Device ID: " + deviceID + ", type: " + deviceData.device_type + "\n");
				}
				*/
				/*
				for (int userID = 1; userID <= DataManager.GetHighestDataID(Constants.DT_USER); userID++)
				{
					// Get the current user's data
					UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, userID, false);

					// If there is no user with the current userID, continue on to the next.
					if (userData == null) {
						continue;
					}

					// Set the user to be a veteran.
					userData.veteran = true;

					// Mark the user's data to be updated.
					DataManager.MarkForUpdate(userData);
				}
				*/
				/*
				// Create ID for this process
				int processID = Constants.GetTime();

				for (int i = 0; i < TournamentData.instance.contenders.size();)
				{
					int nationID = TournamentData.instance.contenders.get(i).nationID;

					// Get the current nation's data
					NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

					if ((nationData.tournament_start_day != TournamentData.instance.start_day) || (nationData.processID == processID))
					{
						// This contender belongs only to the previous tournament, or is a duplicate in this tournament; remove them.
						TournamentData.instance.contenders.remove(i);
						Output.PrintToScreen("Removed contender " + nationData.name);
					}
					else
					{
						// This contender is in the current tournament; skip them.
						nationData.tournament_rank = i + 1;
						nationData.processID = processID;
						nationData.tournament_active = true;
						DataManager.MarkForUpdate(nationData);
						i++;
					}

					DataManager.MarkForUpdate(TournamentData.instance);
				}
				*/

				/*
				for (int nationID = 1; nationID <= DataManager.GetHighestDataID(Constants.DT_NATION); nationID++)
				{
					// Get the current nation's data
					NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

					// If there is no nation with the current nationID, continue on to the next.
					if (nationData == null) {
						continue;
					}

					// Set the nation to be a veteran.
					nationData.veteran = true;

					// Mark the nation's data to be updated.
					DataManager.MarkForUpdate(nationData);
				}
				*/

				/*
				// Get the mainland land map data
				LandMap land_map = DataManager.GetLandMap(Constants.MAINLAND_MAP_ID, false);

				for (int y = 0; y < land_map.height; y++)
				{
					for (int x = 0; x < land_map.width; x++)
					{
						int nationID = land_map.GetBlockNationID(x, y);

						if (nationID < 1) {
							continue;
						}

						// Get the current nation's data
						NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

						if (nationData == null) {
							continue;
						}

						if ((x < nationData.mainland_footprint.x0) || (x > nationData.mainland_footprint.x1) || (y < nationData.mainland_footprint.y0) || (y > nationData.mainland_footprint.y1)) {
							Output.PrintToScreen("Nation " + nationData.name + " (" + nationID + ") block " + x + "," + y + " is out of its footprint " + nationData.mainland_footprint.x0 + "," + nationData.mainland_footprint.y0 + " to " + nationData.mainland_footprint.x1 + "," + nationData.mainland_footprint.y1);
						}
					}
				}
				*/
			}
			if (command.compareTo("THREAD_STATUS")==0)
			{
				valid = true;
				int cur_time = Constants.GetTime();

				Output.PrintToScreen("\nTimes since last sleep:");
				Output.PrintToScreen("WOCServer: " + (cur_time - WOCServer.prev_sleep_time) + "s");
				Output.PrintToScreen("UpdateThread: " + (cur_time - UpdateThread.prev_sleep_time) + "s");
				Output.PrintToScreen("BackupThread: " + (cur_time - BackupThread.prev_sleep_time) + "s");
				Output.PrintToScreen("EmailThread: " + (cur_time - EmailThread.prev_sleep_time) + "s");
				Output.PrintToScreen("InputThread: " + (cur_time - InputThread.prev_sleep_time) + "s");
				Output.PrintToScreen("\n");
			}
			if ((command.indexOf("TOGGLE_ADMIN")!=-1) && (command.indexOf("TOGGLE_ADMIN_LOGIN_ONLY")==-1))
			{
				valid = true;
				Admin.ToggleAdmin(Constants.FetchParameter(command, "USER", false));
			}
			if ((command.indexOf("TOGGLE_ADMIN_LOGIN_ONLY")!=-1) || (command.equals("TALO")))
			{
				valid = true;
				Admin.ToggleAdminLoginOnly();
			}
			if (command.compareTo("TOGGLE_BACKUP_THREAD")==0)
			{
				valid = true;
				WOCServer.backup_thread_active = !WOCServer.backup_thread_active;
				Output.PrintToScreen("BackupThread is " + (WOCServer.backup_thread_active ? "active" : "inactive"));
			}
			if (command.compareTo("TOGGLE_EMAIL_THREAD")==0)
			{
				valid = true;
				WOCServer.email_thread_active = !WOCServer.email_thread_active;
				Output.PrintToScreen("EmailThread is " + (WOCServer.email_thread_active ? "active" : "inactive"));
			}
			if (command.compareTo("TOGGLE_GARBAGE_COLLECT")==0)
			{
				valid = true;
				WOCServer.garbage_collect_active = !WOCServer.garbage_collect_active;
				Output.PrintToScreen("Periodic garbage collection is " + (WOCServer.garbage_collect_active ? "active" : "inactive"));
			}
			if (command.compareTo("TOGGLE_MULTI_CLIENT")==0)
			{
				valid = true;
				Login.allow_multi_client = !Login.allow_multi_client;
				Output.PrintToScreen("Multi clients from same machine are now " + (Login.allow_multi_client ? "allowed" : "disallowed"));
			}
			if (command.compareTo("TOGGLE_PROCESS_EVENTS")==0)
			{
				valid = true;
				WOCServer.process_events_active = !WOCServer.process_events_active;
				Output.PrintToScreen("Event processing is " + (WOCServer.process_events_active ? "active" : "inactive"));
			}
			if (command.compareTo("TOGGLE_REGULAR_UPDATE")==0)
			{
				valid = true;
				WOCServer.regular_update_active = !WOCServer.regular_update_active;
				Output.PrintToScreen("Regular update is " + (WOCServer.regular_update_active ? "active" : "inactive"));
			}
			if (command.indexOf("TOGGLE_SUPER")!=-1)
			{
				valid = true;
				String nation_name = Constants.FetchParameter(command, "NATION", false);

				int nationID = DataManager.GetNameToIDMap(Constants.DT_NATION, nation_name);
				NationData nation_data = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

				if (nation_data == null)
				{
					Output.PrintToScreen("Unknown nation: '" + nation_name + "'");
				}
				else
				{
					// Toggle the nation's super_nation value
					nation_data.super_nation = !nation_data.super_nation;

					Output.PrintToScreen("The nation " + nation_name + (nation_data.super_nation ? " is now possessed of godlike omnipotence." : " is once again pathetic and weak."));

					// Mark the nation's data to be updated
					DataManager.MarkForUpdate(nation_data);
					//Constants.WriteToLog("log_MFU.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " "  + nation_data.ID + " evt: TOGGLE_SUPER\n");
				}
			}
			if (command.compareTo("TOGGLE_UPDATE_THREAD")==0)
			{
				valid = true;
				WOCServer.update_thread_active = !WOCServer.update_thread_active;
				Output.PrintToScreen("UpdateThread is " + (WOCServer.update_thread_active ? "active" : "inactive"));
			}
			if (command.indexOf("TOGGLE_VET")!=-1)
			{
				valid = true;
				Admin.ToggleVeteranStatus(Constants.FetchParameter(command, "USER", false), Constants.FetchParameter(command, "NATION", false));
			}
			if (command.indexOf("TRACK_CLIENT")!=-1)
			{
				valid = true;
				Admin.TrackClient(Constants.FetchParameter(command, "ID", false));
			}
			if (command.indexOf("TRANSFER_PLAYER_XP")!=-1)
			{
				valid = true;
				Admin.TransferPlayerXP(Constants.FetchParameter(command, "FROM", false),Constants.FetchParameter(command, "TO", false));
			}
			if (command.indexOf("UNMUTE_ALL")!=-1)
			{
				valid = true;
				Admin.UnmuteAll(Constants.FetchParameter(command, "USER", false));
			}
			if (command.indexOf("UPDATE_ALL_AREAS")!=-1)
			{
				valid = true;
				Admin.UpdateAllAreas();
			}
			if (command.indexOf("UPDATE_AREA")!=-1)
			{
				valid = true;
				Admin.UpdateArea(Constants.FetchParameter(command, "NATION", false));
			}
			if (command.indexOf("UPDATE_NATION_RANKS")!=-1)
			{
				valid = true;
				String nation_name = Constants.FetchParameter(command, "NATION", false);

				Admin.UpdateNationRanks(Constants.FetchParameter(command, "NATION", false));
			}
			if (command.indexOf("UPDATE_TOURNAMENT")!=-1)
			{
				valid = true;
				TournamentData.instance.UpdateForDay();
			}
			if (command.indexOf("USER_INFO")!=-1)
			{
				valid = true;
				Admin.UserInfo(Constants.FetchParameter(command, "USER", false));
			}
			if (command.compareTo("WARNING_BRIEF")==0)
			{
				valid = true;
				Comm.SendAdminAnnouncement(ClientString.Get("svr_announce_update_brief")); // "The server is about to go down for a brief update. You should be able to log back in after a few seconds."
			}
			if (command.compareTo("WARNING_0")==0)
			{
				valid = true;
				Comm.SendAdminAnnouncement(ClientString.Get("svr_announce_update_now"));
			}
			if (command.compareTo("WARNING_1")==0)
			{
				valid = true;
				Comm.SendAdminAnnouncement(ClientString.Get("svr_announce_update_1_min"));
			}
			if (command.compareTo("WARNING_5")==0)
			{
				valid = true;
				Comm.SendAdminAnnouncement(ClientString.Get("svr_announce_update_5_min"));
			}
			if (!valid){
				Output.PrintToScreen("\nError: '" + command + "' not a supported command. Type HELP for list of commands.\n");
			}
			Output.screenOut.print(">");
			Output.screenOut.flush();
		}
		catch (Exception e)
		{
			Output.PrintToScreen("Exception while executing server command:");
			Output.PrintException(e);
			success = false;
		}

		return success;
	}
};
