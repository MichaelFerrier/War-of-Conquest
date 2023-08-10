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
import WOCServer.*;

public class Quests
{
	// Hardcoded quest IDs to eliminate the need to iterate through all quests in frequent situations.
	static private int CRITERIA_WINNINGS_QUEST_ID = 28;

	public static void HandleUpgrade(NationData _nationData, int _delay)
	{
		QuestData quest_data;

		// Iterate through all quests...
		for (int quest_id = 0; quest_id <= QuestData.highest_id; quest_id++)
		{
			// Get the data for the current quest
			quest_data = QuestData.quests_array[quest_id];
			if (quest_data == null) {
				continue;
			}

			if (quest_data.criteria == QuestData.CRITERIA_NUM_UPGRADES) {
				IncrementQuestAmount(_nationData, quest_data, 1, _delay);
			}
		}
	}

	public static void HandleBuild(NationData _nationData, BuildData _build_data, int _delay)
	{
		QuestData quest_data;

		// Iterate through all quests...
		for (int quest_id = 0; quest_id <= QuestData.highest_id; quest_id++)
		{
			// Get the data for the current quest
			quest_data = QuestData.quests_array[quest_id];
			if (quest_data == null) {
				continue;
			}

			if ((quest_data.criteria == QuestData.CRITERIA_BUILD) && (quest_data.criteria_subject == _build_data.ID)) {
				IncrementQuestAmount(_nationData, quest_data, 1, _delay);
			}
		}
	}

	public static void HandleDiscovery(NationData _nationData, int _advanceID, int _delay)
	{
		QuestData quest_data;

		// Iterate through all quests...
		for (int quest_id = 0; quest_id <= QuestData.highest_id; quest_id++)
		{
			// Get the data for the current quest
			quest_data = QuestData.quests_array[quest_id];
			if (quest_data == null) {
				continue;
			}

			if ((quest_data.criteria == QuestData.CRITERIA_DISCOVER) && (quest_data.criteria_subject == _advanceID)) {
				IncrementQuestAmount(_nationData, quest_data, 1, _delay);
			}
		}
	}

	public static void HandleDiscoverEnergySupplyLine(NationData _nationData, int _amount_transferred, int _delay)
	{
		QuestData quest_data;

		// Iterate through all quests...
		for (int quest_id = 0; quest_id <= QuestData.highest_id; quest_id++)
		{
			// Get the data for the current quest
			quest_data = QuestData.quests_array[quest_id];
			if (quest_data == null) {
				continue;
			}

			if (quest_data.criteria == QuestData.CRITERIA_ENERGY_SUPPLY_LINE) {
				IncrementQuestAmount(_nationData, quest_data, 1, _delay);
			}
			else if (quest_data.criteria == QuestData.CRITERIA_STOLEN_ENERGY) {
				IncrementQuestAmount(_nationData, quest_data, _amount_transferred, _delay);
			}
		}
	}

	public static void HandleDiscoverManpowerSupplyLine(NationData _nationData, int _amount_transferred, int _delay)
	{
		QuestData quest_data;

		// Iterate through all quests...
		for (int quest_id = 0; quest_id <= QuestData.highest_id; quest_id++)
		{
			// Get the data for the current quest
			quest_data = QuestData.quests_array[quest_id];
			if (quest_data == null) {
				continue;
			}

			if (quest_data.criteria == QuestData.CRITERIA_MANPOWER_SUPPLY_LINE) {
				IncrementQuestAmount(_nationData, quest_data, 1, _delay);
			}
			else if (quest_data.criteria == QuestData.CRITERIA_STOLEN_MANPOWER) {
				IncrementQuestAmount(_nationData, quest_data, _amount_transferred, _delay);
			}
		}
	}

	public static void HandleSalvage(NationData _nationData, BuildData _build_data, int _delay)
	{
		QuestData quest_data;

		// Iterate through all quests...
		for (int quest_id = 0; quest_id <= QuestData.highest_id; quest_id++)
		{
			// Get the data for the current quest
			quest_data = QuestData.quests_array[quest_id];
			if (quest_data == null) {
				continue;
			}

			if ((quest_data.criteria == QuestData.CRITERIA_SALVAGE_BUILD) && (_build_data.type != BuildData.TYPE_WALL)) {
				IncrementQuestAmount(_nationData, quest_data, 1, _delay);
			}
			else if ((quest_data.criteria == QuestData.CRITERIA_SALVAGE_WALL) && (_build_data.type == BuildData.TYPE_WALL)) {
				IncrementQuestAmount(_nationData, quest_data, 1, _delay);
			}
		}
	}

	public static void HandleBuildActivate(NationData _nationData, int _buildID, int _delay)
	{
		QuestData quest_data;

		// Iterate through all quests...
		for (int quest_id = 0; quest_id <= QuestData.highest_id; quest_id++)
		{
			// Get the data for the current quest
			quest_data = QuestData.quests_array[quest_id];
			if (quest_data == null) {
				continue;
			}

			if ((quest_data.criteria == QuestData.CRITERIA_BUILD_ACTIVATE) && (quest_data.criteria_subject == _buildID)) {
				IncrementQuestAmount(_nationData, quest_data, 1, _delay);
			}
		}
	}

	public static void HandleEnergyStorageCaptured(NationData _nationData, int _amount, int _delay)
	{
		QuestData quest_data;

		// Iterate through all quests...
		for (int quest_id = 0; quest_id <= QuestData.highest_id; quest_id++)
		{
			// Get the data for the current quest
			quest_data = QuestData.quests_array[quest_id];
			if (quest_data == null) {
				continue;
			}

			if (quest_data.criteria == QuestData.CRITERIA_STOLEN_ENERGY) {
				IncrementQuestAmount(_nationData, quest_data, _amount, _delay);
			}
		}
	}

	public static void HandleManpowerStorageCaptured(NationData _nationData, int _amount, int _delay)
	{
		QuestData quest_data;

		// Iterate through all quests...
		for (int quest_id = 0; quest_id <= QuestData.highest_id; quest_id++)
		{
			// Get the data for the current quest
			quest_data = QuestData.quests_array[quest_id];
			if (quest_data == null) {
				continue;
			}

			if (quest_data.criteria == QuestData.CRITERIA_STOLEN_MANPOWER) {
				IncrementQuestAmount(_nationData, quest_data, _amount, _delay);
			}
		}
	}

	public static void HandleCounterAttackCapture(NationData _nationData, int _delay)
	{
		QuestData quest_data;

		// Iterate through all quests...
		for (int quest_id = 0; quest_id <= QuestData.highest_id; quest_id++)
		{
			// Get the data for the current quest
			quest_data = QuestData.quests_array[quest_id];
			if (quest_data == null) {
				continue;
			}

			if (quest_data.criteria == QuestData.CRITERIA_CAPTURE_LAND_COUNTER_ATTACK) {
				IncrementQuestAmount(_nationData, quest_data, 1, _delay);
			}
		}
	}

	public static void HandleCaptureLand(NationData _nationData, int _formerNationID, BlockData _block_data, BlockExtData _block_ext_data, BuildData _build_data, int _delay)
	{
		QuestData quest_data;

		// Iterate through all quests...
		for (int quest_id = 0; quest_id <= QuestData.highest_id; quest_id++)
		{
			// Get the data for the current quest
			quest_data = QuestData.quests_array[quest_id];
			if (quest_data == null) {
				continue;
			}

			if ((quest_data.criteria == QuestData.CRITERIA_CAPTURE_LAND) && (_formerNationID != -1)) {
				IncrementQuestAmount(_nationData, quest_data, 1, _delay);
			}
			else if ((quest_data.criteria == QuestData.CRITERIA_OCCUPY_LAND) && (_formerNationID == -1)) {
				IncrementQuestAmount(_nationData, quest_data, 1, _delay);
			}
			else if (quest_data.criteria == QuestData.CRITERIA_SUM_AREA) {
				MaxQuestAmount(_nationData, quest_data, _nationData.mainland_footprint.area, _delay);
			}
			else if (quest_data.criteria == QuestData.CRITERIA_GEO_EFFICIENCY)
			{
				MaxQuestAmount(_nationData, quest_data, (int)(_nationData.GetFinalGeoEfficiency(Constants.MAINLAND_MAP_ID) * 100f), _delay);
			}
			else if ((quest_data.criteria == QuestData.CRITERIA_CAPTURE_WALL) && (_build_data != null) && (_build_data.type == BuildData.TYPE_WALL) && (_formerNationID == _block_ext_data.owner_nationID)) {
				IncrementQuestAmount(_nationData, quest_data, 1, _delay);
			}
			else if ((quest_data.criteria == QuestData.CRITERIA_CAPTURE_BUILD) && (_build_data != null) && (_build_data.ID >= quest_data.criteria_subject) && (_build_data.ID < (quest_data.criteria_subject + 3)) && (_formerNationID == _block_ext_data.owner_nationID)) {
				IncrementQuestAmount(_nationData, quest_data, 1, _delay);
			}
			else if ((quest_data.criteria == QuestData.CRITERIA_CAPTURE_BUILD_CATEGORY) && (_build_data != null) && (_build_data.type != BuildData.TYPE_WALL) && (_build_data.required_tech != null) && (_build_data.required_tech.category == quest_data.criteria_subject) && (_formerNationID == _block_ext_data.owner_nationID)) {
				IncrementQuestAmount(_nationData, quest_data, 1, _delay);
			}
		}
	}

	public static void HandleCaptureByFlanking(NationData _nationData, int _delay)
	{
		QuestData quest_data;

		// Iterate through all quests...
		for (int quest_id = 0; quest_id <= QuestData.highest_id; quest_id++)
		{
			// Get the data for the current quest
			quest_data = QuestData.quests_array[quest_id];
			if (quest_data == null) {
				continue;
			}

			if (quest_data.criteria == QuestData.CRITERIA_CAPTURE_BY_FLANKING) {
				IncrementQuestAmount(_nationData, quest_data, 1, _delay);
			}
		}
	}

	public static void HandleAddObject(NationData _nationData, ObjectData _objectData, boolean _first_acquire, boolean _captured, int _delay)
	{
		QuestData quest_data;

		// Iterate through all quests...
		for (int quest_id = 0; quest_id <= QuestData.highest_id; quest_id++)
		{
			// Get the data for the current quest
			quest_data = QuestData.quests_array[quest_id];
			if (quest_data == null) {
				continue;
			}

			if ((quest_data.criteria == QuestData.CRITERIA_CAPTURE_RESOURCE_NUM) && _first_acquire && _captured && (_objectData.ID < ObjectData.ORB_BASE_ID)) {
				IncrementQuestAmount(_nationData, quest_data, 1, _delay);
			}
			else if ((quest_data.criteria == QuestData.CRITERIA_CAPTURE_ORB_NUM) && _first_acquire && _captured && (_objectData.ID >= ObjectData.ORB_BASE_ID)) {
				IncrementQuestAmount(_nationData, quest_data, 1, _delay);
			}
			else if ((quest_data.criteria == QuestData.CRITERIA_CAPTURE_OBJECT) && (quest_data.criteria_subject == _objectData.ID) && _first_acquire) {
				IncrementQuestAmount(_nationData, quest_data, 1, _delay);
			}
			else if ((quest_data.criteria == QuestData.CRITERIA_NUM_RESOURCES) && (_objectData.ID < ObjectData.ORB_BASE_ID)) {
				IncrementQuestAmount(_nationData, quest_data, 1, _delay);
			}
			else if ((quest_data.criteria == QuestData.CRITERIA_NUM_ORBS) && (_objectData.ID >= ObjectData.ORB_BASE_ID)) {
				IncrementQuestAmount(_nationData, quest_data, 1, _delay);
			}
		}
	}

	public static void HandleRemoveObject(NationData _nationData, ObjectData _objectData, int _delay)
	{
		QuestData quest_data;

		// Iterate through all quests...
		for (int quest_id = 0; quest_id <= QuestData.highest_id; quest_id++)
		{
			// Get the data for the current quest
			quest_data = QuestData.quests_array[quest_id];
			if (quest_data == null) {
				continue;
			}

			if ((quest_data.criteria == QuestData.CRITERIA_NUM_RESOURCES) && (_objectData.ID < ObjectData.ORB_BASE_ID)) {
				IncrementQuestAmount(_nationData, quest_data, -1, _delay);
			}
			else if ((quest_data.criteria == QuestData.CRITERIA_NUM_ORBS) && (_objectData.ID >= ObjectData.ORB_BASE_ID)) {
				IncrementQuestAmount(_nationData, quest_data, -1, _delay);
			}
		}
	}

	public static void HandleEnterGame(NationData _nationData)
	{
		QuestData quest_data;

		// Iterate through all quests...
		for (int quest_id = 0; quest_id <= QuestData.highest_id; quest_id++)
		{
			// Get the data for the current quest
			quest_data = QuestData.quests_array[quest_id];
			if (quest_data == null) {
				continue;
			}

			if (quest_data.criteria == QuestData.CRITERIA_MEMBERS_ONLINE) {
				MaxQuestAmount(_nationData, quest_data, WOCServer.GetNationNumUsersOnline(_nationData.ID), 0);
			}
		}
	}

	public static void HandleAwardPrize(NationData _nationData)
	{
		QuestData quest_data;

		// Get the data for the current quest
		quest_data = QuestData.quests_array[CRITERIA_WINNINGS_QUEST_ID];
		if (quest_data == null) {
			return;
		}

		if (quest_data.criteria == QuestData.CRITERIA_WINNINGS) {
			MaxQuestAmount(_nationData, quest_data, (int)_nationData.prize_money_history, 0);
		}
	}

	public static void HandleFormAlliance(NationData _nationData)
	{
		QuestData quest_data;

		// Iterate through all quests...
		for (int quest_id = 0; quest_id <= QuestData.highest_id; quest_id++)
		{
			// Get the data for the current quest
			quest_data = QuestData.quests_array[quest_id];
			if (quest_data == null) {
				continue;
			}

			if (quest_data.criteria == QuestData.CRITERIA_FORM_ALLIANCE) {
				MaxQuestAmount(_nationData, quest_data, _nationData.alliances_active.size(), 0);
			}
		}
	}

	public static void HandleDonateEnergyToAlly(NationData _nationData, float _donated_energy)
	{
		QuestData quest_data;

		float prev_donated_energy_history = _nationData.donated_energy_history;

		// Add the given _donated_energy to the nation's donated_energy_history and donated_energy_history_monthly.
		_nationData.donated_energy_history += _donated_energy;
		_nationData.donated_energy_history_monthly += _donated_energy;

		// Only update the quest record and ranks if the amount has changed by at least 100.
		if (((int)(prev_donated_energy_history / 100)) != ((int)(_nationData.donated_energy_history / 100)))
		{
			// Iterate through all quests...
			for (int quest_id = 0; quest_id <= QuestData.highest_id; quest_id++)
			{
				// Get the data for the current quest
				quest_data = QuestData.quests_array[quest_id];
				if (quest_data == null) {
					continue;
				}

				if (quest_data.criteria == QuestData.CRITERIA_DONATE_ENERGY) {
					MaxQuestAmount(_nationData, quest_data, (int)_nationData.donated_energy_history, 0);
				}
			}

			// Update the ranks
			RanksData.instance.ranks_nation_energy_donated.UpdateRanks(_nationData.ID, _nationData.name, _nationData.donated_energy_history, Constants.NUM_ENERGY_DONATED_RANKS, false);
			RanksData.instance.ranks_nation_energy_donated_monthly.UpdateRanks(_nationData.ID, _nationData.name, _nationData.donated_energy_history_monthly, Constants.NUM_ENERGY_DONATED_RANKS, false);
		}
	}

	public static void HandleDonateManpowerToAlly(NationData _nationData, float _donated_manpower)
	{
		QuestData quest_data;

		float prev_donated_manpower_history = _nationData.donated_manpower_history;

		// Add the given _donated_manpower to the nation's donated_manpower_history and donated_manpower_history_monthly.
		_nationData.donated_manpower_history += _donated_manpower;
		_nationData.donated_manpower_history_monthly += _donated_manpower;

		// Only update the quest record and ranks if the amount has changed by at least 100.
		if (((int)(prev_donated_manpower_history / 100)) != ((int)(_nationData.donated_manpower_history / 100)))
		{
			// Iterate through all quests...
			for (int quest_id = 0; quest_id <= QuestData.highest_id; quest_id++)
			{
				// Get the data for the current quest
				quest_data = QuestData.quests_array[quest_id];
				if (quest_data == null) {
					continue;
				}

				if (quest_data.criteria == QuestData.CRITERIA_DONATE_MANPOWER) {
					MaxQuestAmount(_nationData, quest_data, (int)_nationData.donated_manpower_history, 0);
				}
			}

			// Update the ranks
			RanksData.instance.ranks_nation_manpower_donated.UpdateRanks(_nationData.ID, _nationData.name, _nationData.donated_manpower_history, Constants.NUM_MANPOWER_DONATED_RANKS, false);
			RanksData.instance.ranks_nation_manpower_donated_monthly.UpdateRanks(_nationData.ID, _nationData.name, _nationData.donated_manpower_history_monthly, Constants.NUM_MANPOWER_DONATED_RANKS, false);
		}
	}

	public static void IncrementQuestAmount(NationData _nationData, QuestData _quest_data, int _amount, int _delay)
	{
		// Get the given nation's record for the given quest's ID, creating the record if it doesn't yet exist.
		QuestRecord quest_record = _nationData.GetQuestRecord(_quest_data.ID, true);

		// If the nation has not yet completed all stages of this quest, increment its amount counter.
		if (quest_record.completed < _quest_data.num_stages)
		{
			// Increment cur_amount
			quest_record.cur_amount += _amount;

			//if (quest_record.cur_amount > _quest_data.criteria_amount[quest_record.completed]) {
			//	quest_record.cur_amount = _quest_data.criteria_amount;
			//}

			// Sanity check cur_amount
			if (quest_record.cur_amount < 0)
			{
				Output.PrintToScreen("IncrementQuestAmount(): Nation " + _nationData.ID + "'s quest " + _quest_data.ID + "'s record's cur_amount is negative! cur_amount: " + quest_record.cur_amount + ", _amount: " + _amount + ". Setting to 0.");
				quest_record.cur_amount = 0;
			}

			// If all stages of this quest have not yet been completed...
			if (quest_record.completed < _quest_data.num_stages)
			{
				// If the cur_amount has reached or exceeded the criteria amount, record that this stage of the quest has been completed.
				if (quest_record.cur_amount >= _quest_data.criteria_amount[quest_record.completed])
				{
					quest_record.completed++;
				}

				// Broadcast the change in the quest's status to the nation's players.
				OutputEvents.BroadcastQuestStatusEvent(_nationData.ID, quest_record, _delay);
			}
		}
	}

	public static void MaxQuestAmount(NationData _nationData, QuestData _quest_data, int _amount, int _delay)
	{
		// Get the given nation's record for the given quest's ID, creating the record if it doesn't yet exist.
		QuestRecord quest_record = _nationData.GetQuestRecord(_quest_data.ID, true);

		// If the nation has not yet completed all stages of this quest, max its amount counter with the given value.
		if (quest_record.completed < _quest_data.num_stages)
		{
			if (_amount > quest_record.cur_amount)
			{
				// Set the cur_amount to the given greater _amount.
				quest_record.cur_amount = _amount;

				//if (quest_record.cur_amount > _quest_data.criteria_amount[quest_record.completed]) {
				//	quest_record.cur_amount = _quest_data.criteria_amount;
				//}

				// Sanity check cur_amount
				if (quest_record.cur_amount < 0)
				{
					Output.PrintToScreen("MaxQuestAmount(): Nation " + _nationData.ID + "'s quest " + _quest_data.ID + "'s record's cur_amount is negative! cur_amount: " + quest_record.cur_amount + ", _amount: " + _amount + ". Setting to 0.");
					quest_record.cur_amount = 0;
				}

				// If all stages of this quest have not yet been completed...
				if (quest_record.completed < _quest_data.num_stages)
				{
					// If the cur_amount has reached or exceeded the criteria amount, record that this stage of the quest has been completed.
					if (quest_record.cur_amount >= _quest_data.criteria_amount[quest_record.completed])
					{
						quest_record.completed++;
					}

					// Broadcast the change in the quest's status to the nation's players.
					OutputEvents.BroadcastQuestStatusEvent(_nationData.ID, quest_record, _delay);
				}
			}
		}
	}

	public static void Collect(StringBuffer _output_buffer, int _userID, int _questID)
	{
		// Get the user's data
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		if (userData.rank > Constants.RANK_GENERAL)
		{
			// Return message
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_quests_collect_rank_too_low")); // "You cannot collect for completed quests until you are promoted to General."
			return;
		}

		// Determine ID of the user's nation
		int nationID = userData.nationID;

		// Get the user's nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

		if (nationData == null) {
			return;
		}

		QuestRecord quest_record = nationData.GetQuestRecord(_questID, false);

		if ((quest_record == null) || (quest_record.completed == 0) || (quest_record.collected >= quest_record.completed)) {
			return;
		}

		// Get the data for this quest
		QuestData quest_data = QuestData.quests_array[_questID];

		if (quest_data == null) {
			return;
		}

		if (quest_data.reward_credits[quest_record.collected] > 0)
		{
			// Add the reward credits to the nation.
			Money.AddGameMoney(nationData, quest_data.reward_credits[quest_record.collected], Money.Source.FREE);

			// Broadcast an update bars event to the nation, letting all players know about the change in credits.
			OutputEvents.BroadcastUpdateBarsEvent(nationID, 0, 0, 0, 0, quest_data.reward_credits[quest_record.collected], 0);
		}

		if (quest_data.reward_xp[quest_record.collected] > 0)
		{
			// Add the reward XP to the nation.
			Gameplay.AddXP(nationData, quest_data.reward_xp[quest_record.collected], _userID, -1, -1, true, true, 0, Constants.XP_QUEST);

			// Log suspect
			if (nationData.log_suspect_expire_time > Constants.GetTime())
			{
				// Log the details of this xp gain.
				Constants.WriteToLog("log_suspect.txt", Constants.GetTimestampString() + ": '" + nationData.name + "'(ID:" + nationData.ID + ", Level:" + nationData.level + ") received " + quest_data.reward_xp[quest_record.collected] + " XP for completing quest " + _questID + ".\n");
			}
		}

		// Mark this quest's record as the current stage having been collected.
		quest_record.collected++;

		// Increment the nation's counts of quests_completed and quests_completed_monthly
		nationData.quests_completed++;
		nationData.quests_completed_monthly++;

		// Update the ranks for quests completed
		RanksData.instance.ranks_nation_quests.UpdateRanks(nationData.ID, nationData.name, nationData.quests_completed, Constants.NUM_QUESTS_COMPLETED_RANKS, false);
		RanksData.instance.ranks_nation_quests_monthly.UpdateRanks(nationData.ID, nationData.name, nationData.quests_completed_monthly, Constants.NUM_QUESTS_COMPLETED_RANKS, false);

		// Update the nation's data
		DataManager.MarkForUpdate(nationData);

		// Broadcast quest status update to the user's nation.
		OutputEvents.BroadcastQuestStatusEvent(nationID, quest_record, 0);

		// Award available ad bonus credits to the user, if appropriate.
		Gameplay.AwardAvailableAdBonus(_output_buffer, userData, 1f, Constants.AD_BONUS_TYPE_QUEST, -1, -1, 0);
	}
}
