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

import java.util.ArrayList;
import java.text.DecimalFormat;
import WOCServer.*;

public class Objects
{
	static int [] coord_array = new int[2];

	public static void AddObject(int _nationID, int _objectID, LandMap _land_map, int _x, int _y, int _userID, boolean _captured, int _delay)
	{
		// Get the nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationID, false);
		NationTechData nationTechData = (NationTechData)DataManager.GetData(Constants.DT_NATIONTECH, _nationID, false);

		if ((nationData == null) || (nationTechData == null)) {
			Output.PrintToScreen("AddObject called for nation " + _nationID + " which is missing part of its data");
			return;
		}

		// Get the coordinates token
		int token = Constants.TokenizeCoordinates(_x, _y);

		// Get the object's data
		ObjectData objectData = ObjectData.GetObjectData(_objectID);

		if (objectData.type == ObjectData.TYPE_TECH)
		{
			// Get the tech's ID
			int techID = objectData.techID;

			// Get the technology's data
			TechData techData = TechData.GetTechData(techID);

			if (techData == null) {
				Output.PrintToScreen("AddObject called for object " + objectData.name + " with non-existent technology " + techID);
				return;
			}

			if (Technology.RequirementsMet(techID, nationData, nationTechData) == false)
			{
				// Add the object tech to the nation's pending_object_coords array.
				nationTechData.AddPendingObject(_x, _y);

				// Post report to nation
				//Comm.SendReport(_nationID, ClientString.Get("We are not yet able to make use of our discovery."));
			}
			else
			{
				// Add the object's technology to the nation
				Technology.AddTechnology(_nationID, techID, objectData.GetPositionInRange(_x, _y, _land_map), false, true, _delay);
			}
		}
		else if (objectData.type == ObjectData.TYPE_ORB)
		{
			// Post report to nation
			Comm.SendReport(_nationID, ClientString.Get("svr_report_capture_object", "object_name", "{Objects/object_" + _objectID + "_name}", "object_location", _x + "," + _y), _delay); // "We have captured " + objectData.name + "!"

			// Make sure the nation has a winnings record for this goal (for goals list update), by adding 0 to it.
			int orb_index = nationData.goals_token.indexOf(Integer.valueOf(token));
			if (orb_index == -1)
			{
				nationData.goals_token.add(token);
				nationData.goals_winnings.add(0.0f);
			}

			if (_objectID == 2004) // Orb of Fire
			{
				// Add to the nation the flag indicating that it possesses the Orb of Fire.
				nationData.SetFlags(nationData.flags | Constants.NF_ORB_OF_FIRE);
			}
		}

		boolean first_acquire = (nationTechData.object_capture_history.containsKey(token) == false);

		// If this nation has not previously acquired this object...
		if (first_acquire)
		{
			// Record that this nation has first captured the object in this block, at this time.
			nationTechData.object_capture_history.put(token, Constants.GetTime());

			// Add the appropriate amount of XP to this nation for first capturing the object in this block.
			Gameplay.AddXP(nationData, objectData.xp, _userID, _x, _y, true, true, _delay, Constants.XP_FIRST_CAPTURE);

			// Log suspect
			if (nationData.log_suspect_expire_time > Constants.GetTime())
			{
				// Log the details of this xp gain.
				Constants.WriteToLog("log_suspect.txt", Constants.GetTimestampString() + ": '" + nationData.name + "'(ID:" + nationData.ID + ", Level:" + nationData.level + ") received " + objectData.xp + " XP for first acquiring object at " + _x + "," + _y + ".\n");
			}

			if (_userID != -1)
			{
				// Award available ad bonus credits to the user, if appropriate.
				UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);
				Gameplay.AwardAvailableAdBonus(null, userData, 1f, (objectData.type == ObjectData.TYPE_ORB) ? Constants.AD_BONUS_TYPE_ORB : Constants.AD_BONUS_TYPE_RESOURCE, _x, _y, _delay);
			}
		}

		// Add a record of this object to the nation.
		nationData.AddObjectRecord(_land_map.ID, _x, _y, _objectID);

		// Broadcast to the nation that this object is being added.
		OutputEvents.BroadcastAddObjectEvent(_nationID, _x, _y, _objectID);

		// Update the quest system for the adding of this landscape object.
		Quests.HandleAddObject(nationData, objectData, first_acquire, _captured, _delay);
	}

	public static void RemoveObject(int _nationID, int _objectID, LandMap _land_map, int _x, int _y, int _capturedByNationID, int _delay)
	{
		// Get the nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationID, false);
		NationTechData nationTechData = (NationTechData)DataManager.GetData(Constants.DT_NATIONTECH, _nationID, false);

		// Get the object's data
		ObjectData objectData = ObjectData.GetObjectData(_objectID);

		if (objectData.type == ObjectData.TYPE_TECH)
		{
			// Get the tech's ID
			int techID = objectData.techID;

			// Get the technology's data
			TechData techData = TechData.GetTechData(techID);

			if (techData == null) {
				Output.PrintToScreen("RemoveObject called for object " + objectData.name + " with non-existent technology " + techID);
				return;
			}

			// If the nation is to possess the technology only so long as it possesses the object...
			if (techData.duration_type == TechData.DURATION_OBJECT)
			{
				if (Technology.RequirementsMet(techID, nationData, nationTechData) == false)
				{
					// Remove the object from the list of pending objects
					nationTechData.RemovePendingObject(_x, _y);
				}
				else if (nationTechData.GetTechCount(techID) > 0)
				{
					// Remove the object's technology from the nation
					Technology.RemoveTechnology(_nationID, techID, objectData.GetPositionInRange(_x, _y, _land_map));
				}
			}

			// Update the nation's users' reports for losing this resource.
			nationData.ModifyUserReportValueInt(UserData.ReportVal.report__resource_count_delta, -1);
		}
		else if (objectData.type == ObjectData.TYPE_ORB)
		{
		  int token = Constants.TokenizeCoordinates(_x, _y);
		  int cur_orb = nationData.goals_token.indexOf(Integer.valueOf(token));

			// Update the nation's users' reports for losing this orb.
			nationData.ModifyUserReportValueInt(UserData.ReportVal.report__orb_count_delta, -1);

			if (_objectID == 2004) // Orb of Fire
			{
				// Remove from the nation the flag indicating that it possesses the Orb of Fire.
				nationData.SetFlags(nationData.flags & ~Constants.NF_ORB_OF_FIRE);
			}
		}

		// If the object has been captured from this nation by another nation...
		if (_capturedByNationID != -1)
		{
			NationData capturedByNationData = (NationData)DataManager.GetData(Constants.DT_NATION, _capturedByNationID, false);
			if (capturedByNationData != null)
			{
				// Post report to nation
				Comm.SendReport(_nationID, ClientString.Get("svr_report_lost_object", "object_name", "{Objects/object_" + _objectID + "_name}", "object_location", _x + "," + _y, "nation_name", capturedByNationData.GetFlag(Constants.NF_INCOGNITO) ? "{an_incognito_nation}" : capturedByNationData.name, "nation_id", capturedByNationData.GetFlag(Constants.NF_INCOGNITO) ? "-1" : ("" + _capturedByNationID)), _delay); // "We have lost " + objectData.name + "."
			}
		}

		// Remove the record of this object from the nation.
		nationData.RemoveObjectRecord(_land_map.ID, _x, _y);

		// Broadcast to the nation that this object is being removed.
		OutputEvents.BroadcastRemoveObjectEvent(_nationID, _x, _y);

		// Update the quest system for the removing of this landscape object.
		Quests.HandleRemoveObject(nationData, objectData, 0);
	}

	public static void UpdateAllGoals()
	{
		// Get the mainland map's data
		LandMap mainland_map = DataManager.GetLandMap(Constants.MAINLAND_MAP_ID, false);

		// Make sure the mainland map has been loaded.
		if (mainland_map == null) {
			Output.PrintToScreen("Mainland landscape map not loaded.");
			return;
		}

		boolean new_month = (Constants.GetMonth() != GlobalData.instance.cur_goal_update_month);

		if (new_month)
		{
			// Record the new cur_goal_update_month
			GlobalData.instance.cur_goal_update_month = Constants.GetMonth();
		}

		int nationID, objectID, xp_award;
		float nation_award, total_awarded = 0.0f;
		NationData nationData;
		ObjectData objectData;

		// Iterate through the goals array...
		for (int goal_index = 0; goal_index < RanksData.instance.goals_token.size(); goal_index++)
		{
			if (new_month)
			{
				// Clear the goal's ranks_monthly array
				RanksData.instance.goals_ranks_monthly.get(goal_index).Clear();
			}

			// Determine the current goal's coordinates
			Constants.UntokenizeCoordinates(RanksData.instance.goals_token.get(goal_index), coord_array);

			// Get this block's objectID
			objectID = mainland_map.GetBlockObjectID(coord_array[0], coord_array[1]);

			// If this block no longer has an object, the goal's been removed. Continue on to next.
			if (objectID == -1) {
				continue;
			}

			// Get this block's object data
			objectData = ObjectData.GetObjectData(objectID);

			// Make sure object data was found
      if(objectData == null) {
				Output.PrintToScreen("UpdateAllGoals(): ObjectData is null for orb at " + coord_array[0] + "," + coord_array[1] + " with object ID " + objectID + ". Cannot continue!");
				return;
      }

			// If this block's object is no longer an orb object, the orb's been removed. Continue on to next.
			if (objectData.type != ObjectData.TYPE_ORB) {
				continue;
			}

			// Get the nationID of this orb's block
			nationID = mainland_map.GetBlockNationID(coord_array[0], coord_array[1]);

			if (nationID > 0)
			{
				// Get the nation's data
				nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

				// If the nation hasn't been used recently, do not allow its captured goal(s) to generate credits.
				if ((Constants.GetTime() - nationData.prev_use_time) > Constants.TIME_SINCE_LAST_USE_DISABLE_GOALS) {
					continue;
				}

				// Determine the amount of winnings, converted to cents, to award to this nation for this update period.
				//nation_award = (float)objectData.credits_per_hour * (float)Constants.GOAL_UPDATE_PERIOD / (float)Constants.SECONDS_PER_HOUR;
				nation_award = Constants.orb_payments_per_hour.get(objectID) * 100.0f * (float)Constants.GOAL_UPDATE_PERIOD / (float)Constants.SECONDS_PER_HOUR;

				//// Constrain the award to be no more than the nation's amount of energy.
				//nation_award = Math.min(nation_award, nationData.energy);

				//// Remove the reward amount from the nation's own energy.
				//nationData.energy -= nation_award;

				// Log awarding of prize
				if ((WOCServer.log_flags & Constants.LOG_AWARDS) != 0) {
	        Output.PrintToScreen("Awarding " + nation_award + " cents to " + nationData.name + " (" + nationID + ") for " + objectData.name + " at pos " + coord_array[0] + "," + coord_array[1] + ". Winnings $/hr: " + Constants.orb_payments_per_hour.get(objectID) + ".");
				}

				// Keep track of total awarded in this update so far
				total_awarded += nation_award;

				// Award the prize to the nation
				AwardPrize(nationID, nation_award, RanksData.instance.goals_token.get(goal_index), RanksData.instance, goal_index);

				//// Update the nation's users' reports.
				//nationData.ModifyUserReportValueFloat(UserData.ReportVal.report__energy_spent, nation_award);

				// Determine the amount of XP to award to this nation for this update period.
				xp_award = (int)((float)objectData.xp_per_hour * (float)Constants.GOAL_UPDATE_PERIOD / (float)Constants.SECONDS_PER_HOUR + 0.5f);

				// Add the determined amount of XP to the nation.
				Gameplay.AddXP(nationData, xp_award, -1, -1, -1, true, true, 0, Constants.XP_ORB);

				// Log suspect
				if ((nationData.log_suspect_expire_time > Constants.GetTime()) && (xp_award > 1))
				{
					// Log the details of this xp gain.
					Constants.WriteToLog("log_suspect.txt", Constants.GetTimestampString() + ": '" + nationData.name + "'(ID:" + nationData.ID + ", Level:" + nationData.level + ") received " + objectData.xp + " XP for holding Orb.\n");
				}

				// Record user report of this XP award
				nationData.ModifyUserReportValueInt(UserData.ReportVal.report__orb_XP, xp_award);
			}
		}

		// Add the amount awarded in this update to the total amount awarded
		GlobalData.instance.game_money_awarded += total_awarded;

		// Mark the global data to be updated
		DataManager.MarkForUpdate(GlobalData.instance);

		// Mark the ranks data to be updated
		DataManager.MarkForUpdate(RanksData.instance);
	}

	public static void AwardPrize(int _nationID, float _prize, int _goal_token, RanksData _ranksData, int _goal_index)
	{
		boolean log_award = false;

		// Get the nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationID, false);

		if (nationData == null) {
			return;
		}

		if (_prize < 0)
		{
			Output.PrintToScreen("ERROR: AwardPrize() for nation " + nationData.name + " given negative _prize amount " + _prize + ".");
			Constants.WriteToNationLog(nationData, null, "ERROR: AwardPrize() for nation " + nationData.name + " given negative _prize amount " + _prize + ".");
			Output.PrintStackTrace();
		}

		// Record the nation's former prize_money_history value.
		float prev_prize_money_history = nationData.prize_money_history;

		// Add the given _prize to the nation's prize_money
		nationData.prize_money += _prize;

		// Add the given _prize to the nation's prize_money_history and prize_money_history_monthly
		nationData.prize_money_history += _prize;
		nationData.prize_money_history_monthly += _prize;

		// Record time when this nation last received winnings.
		nationData.prev_prize_money_received_time = Constants.GetTime();

		//Output.PrintToScreen("AwardPrize() awarding to nation " + nationData.name + " amount: " + _prize + ", history: " + nationData.prize_money_history + ", monthly history: " + nationData.prize_money_history_monthly);

		// NOTE: AwardPrize() no longer automatically awards credits, it now awards prize_money that can be cashed out or traded in for credits.
		//// Add the given _prize to the nation's game_money and game_money_won.
		//Money.AddGameMoney(nationData, _prize, Money.Source.WON);

		int index;
		float winnings = 0.0f, winnings_monthly = 0.0f;

		if (_goal_token != -1)
		{
			// Update this nation's record of how much money won via this goal
			index = nationData.goals_token.indexOf(Integer.valueOf(_goal_token));
			if (index == -1)
			{
				nationData.goals_token.add(_goal_token);
				nationData.goals_winnings.add(_prize);
				winnings = _prize;
			}
			else
			{
				winnings = nationData.goals_winnings.get(index) + _prize;
				nationData.goals_winnings.set(index, winnings);
			}

			// Update this nation's record of how much money won via this goal this month
			index = nationData.goals_monthly_token.indexOf(Integer.valueOf(_goal_token));
			if (index == -1)
			{
				nationData.goals_monthly_token.add(_goal_token);
				nationData.goals_monthly_winnings.add(_prize);
				winnings_monthly = _prize;
			}
			else
			{
				winnings_monthly = nationData.goals_monthly_winnings.get(index) + _prize;
				nationData.goals_monthly_winnings.set(index, winnings_monthly);
			}
		}

		// Get the nation's name
		String nation_name = nationData.name;

		if (_goal_index != -1)
		{
			// Update this goal's record of the total amount of prize money that it has awarded
			_ranksData.goals_total_awarded.set(_goal_index, _ranksData.goals_total_awarded.get(_goal_index) + _prize);

			// Update the goal's overall ranks
			_ranksData.goals_ranks.get(_goal_index).UpdateRanks(_nationID, nation_name, winnings, Constants.NUM_PRIZE_RANKS, true);

			// Update the goal's monthly ranks
			_ranksData.goals_ranks_monthly.get(_goal_index).UpdateRanks(_nationID, nation_name, winnings_monthly, Constants.NUM_PRIZE_RANKS, true);
		}

    // TESTING
    //Constants.WriteToLog("log_bug.txt", Constants.GetShortTimeString() + " AwardPrize() Nation: " + nation_name + ", prize_money_history: " + prize_money_history + ", _ranksData: " + _ranksData + ", hash: " + _ranksData.hashCode() + "\n");

		// Update the global overall ranks
		_ranksData.ranks_nation_winnings.UpdateRanks(_nationID, nation_name, nationData.prize_money_history, Constants.NUM_GLOBAL_PRIZE_RANKS, true);

		// Update the global monthly ranks
		_ranksData.ranks_nation_winnings_monthly.UpdateRanks(_nationID, nation_name, nationData.prize_money_history_monthly, Constants.NUM_GLOBAL_PRIZE_RANKS, true);

		// Update the quests system for the awarding of this prize amount
		Quests.HandleAwardPrize(nationData);

		// Record user report of this award
		nationData.ModifyUserReportValueFloat(UserData.ReportVal.report__orb_credits, _prize);

		// Mark the nation data to be updated
		DataManager.MarkForUpdate(nationData);

    ////Constants.WriteToLog("log_MFU.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " "  + nationData.ID + " evt: AwardPrize\n");

		// Post a report to the nation, if appropriate.
		if ((int)(prev_prize_money_history / 500) < (int)(nationData.prize_money_history / 500))
		{
	    // Post report to nation
			DecimalFormat df = new DecimalFormat("0.00");
			Comm.SendReport(_nationID, ClientString.Get("svr_report_winnings_exceeded", "winnings", "$" + df.format((((int)(nationData.prize_money_history / 500)) * 500) / 100f)/*String.valueOf(((int)(nationData.prize_money_history / 500)) * 500)*/), 0); // "Our winnings have exceeded {winnings}!"
			log_award = true;
		}

		if (log_award)
		{
			// Log the awarding of this prize
			Constants.WriteToLog("log_prize.txt", Constants.GetTimestampString() + ": Nation " + nationData.name + "(" + _nationID + ") prize money history has reached " + nationData.prize_money_history + " cents. Current prize_money: " + nationData.prize_money + " cents.\n");
			Constants.WriteToNationLog(nationData, null, Constants.GetTimestampString() + ": Nation " + nationData.name + "(" + _nationID + ") prize money history has reached " + nationData.prize_money_history + " cents. Current prize_money: " + nationData.prize_money + " cents.\n");
		}
	}
}
