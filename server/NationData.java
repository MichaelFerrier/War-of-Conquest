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
import java.util.Set;
import java.util.*;
import WOCServer.*;

public class NationData extends BaseData
{
	public static String db_table_name = "Nations";

	public static int VERSION = 1;

	int creation_time = 0;
	int birth_time = 0;
	int prev_use_time = 0;
	int prev_active_time = 0;
	String name;
	String password;
	int level = 0;
	float xp = 0;
	int pending_xp = 0;
	int advance_points = 0;
	int r = 0;
	int g = 0;
	int b = 0;
	int emblem_index = -1;
	int emblem_color = 0;
	//int area = 0;
	//int border_area = 0;
	//int perimeter = 0;
	//float geo_efficiency_base = 0;
	boolean veteran = false;
	boolean area_visibility_updated = false;
	int nextTechExpireTime = 0;
	int nextTechExpire = 0;
	int targetAdvanceID = -1;
	int prev_free_migration_time = 0;
	int prev_unite_time = 0;
	int prev_go_incognito_time = 0;
	float game_money = 0.0f;
	float game_money_purchased = 0.0f;
	float game_money_won = 0.0f;
	float total_game_money_purchased = 0.0f;
	float prize_money = 0.0f; // In cents
	float prize_money_history = 0.0f;
	float prize_money_history_monthly = 0.0f;
	int prev_prize_money_received_time = 0;
	float money_spent = 0.0f;
	float raid_earnings_history = 0f;
	float raid_earnings_history_monthly = 0f;
	float orb_shard_earnings_history = 0f;
	float orb_shard_earnings_history_monthly = 0f;
	int medals_history = 0;
	int medals_history_monthly = 0;
	int level_history = 0;
	float xp_history = 0;
	float xp_history_monthly = 0;
	float tournament_trophies_history = 0.0f;
	float tournament_trophies_history_monthly = 0.0f;
	float donated_energy_history = 0.0f;
	float donated_energy_history_monthly = 0.0f;
	float donated_manpower_history = 0.0f;
	float donated_manpower_history_monthly = 0.0f;
	int quests_completed = 0;
	int quests_completed_monthly = 0;
	int captures_history = 0;
	int captures_history_monthly = 0;
	int max_area = 0;
	int max_area_monthly = 0;
	float rebirth_countdown = 0;
	float rebirth_countdown_purchased = 0;
	int rebirth_countdown_start = 0;

	int flags = 0;

	int prev_buy_energy_day = 0;
	int buy_energy_day_amount = 0;

	int prev_buy_credits_month = 0;
	int prev_buy_credits_month_amount = 0;
	int prev_receive_credits_month_amount = 0;

	int prev_discovery_day = 0;
	int prev_message_send_day = 0;
	int message_send_count = 0;
	int prev_alliance_request_day = 0;
	int alliance_request_count = 0;
	int rebirth_count = 0;
	int reset_advances_count = 0;
  boolean super_nation = false;

	// Stats
	float energy;
	float energy_max;
	//float energy_rate;
	float manpower;
	float manpower_max;
	//float manpower_rate;
	float manpower_per_attack;
	//float stat_tech;
	//float stat_bio;
	//float stat_psi;
	float geo_efficiency_modifier;
	//float xp_multiplier;
	float hit_points_base;
	float hit_points_rate;
	float crit_chance;
	float salvage_value;
	float wall_discount;
	float structure_discount;
	float splash_damage;
	int max_num_alliances;
	int max_simultaneous_processes;
	boolean invisibility;
	boolean insurgency;
	boolean total_defense;

	// Stat multipliers
	float tech_mult;
	float bio_mult;
	float psi_mult;
	float manpower_rate_mult;
	float energy_rate_mult;
	float manpower_max_mult;
	float energy_max_mult;
	float hp_per_square_mult;
	float hp_restore_mult;
	float attack_manpower_mult;

	// Info for stats affected by caps on resource bonuses.
	float tech_perm;
	float tech_temp;
	float tech_object;
	float bio_perm;
	float bio_temp;
	float bio_object;
	float psi_perm;
	float psi_temp;
	float psi_object;
	float energy_rate_perm;
	float energy_rate_temp;
	float energy_rate_object;
	float manpower_rate_perm;
	float manpower_rate_temp;
	float manpower_rate_object;
	float xp_multiplier_perm;
	float xp_multiplier_temp;
	float xp_multiplier_object;

	// Energy and manpower storage structures, generate XP and share energy and manpower among allies.
	int shared_energy_capacity = 0;
	int shared_manpower_capacity = 0;
	float shared_energy_fill = 0f;
	float shared_manpower_fill = 0f;
	int shared_energy_xp_per_hour = 0;
	int shared_manpower_xp_per_hour = 0;
	int num_share_builds = 0;

	//float energy_burn_rate = 0.0f;
	float manpower_burn_rate = 0.0f;

	int prev_update_stats_time = 0;

	// Tournament information
	int tournament_start_day = -1;
	boolean tournament_active = false;
	int tournament_rank = -1;
	float trophies_available = 0;
	float trophies_banked = 0;
	float trophies_potential = 0;

	// Homeland information
	float shard_red_fill = 0f;
	float shard_green_fill = 0f;
	float shard_blue_fill = 0f;

	// Raid information
	boolean raid_eligible = false;
	int raidID = -1;
	int raid_attacker_medals = 0;
	int raid_defender_medals = 0;
	int raid_shield_end_time = 0;
	ArrayList<Integer> raid_prev_candidates = new ArrayList<Integer>();
	ArrayList<Integer> raid_attack_log = new ArrayList<Integer>();
	ArrayList<Integer> raid_defense_log = new ArrayList<Integer>();

	int homeland_mapID = -1;
	int prev_modify_homeland_time = 0;
	Footprint homeland_footprint = new Footprint();
	Footprint mainland_footprint = new Footprint();

	ArrayList<Integer> users = new ArrayList<Integer>();
	ArrayList<Integer> alliances_active = new ArrayList<Integer>();
	ArrayList<Integer> alliances_requests_outgoing = new ArrayList<Integer>();
	ArrayList<Integer> alliances_requests_incoming = new ArrayList<Integer>();
	ArrayList<Integer> unite_requests_outgoing = new ArrayList<Integer>();
	ArrayList<Integer> unite_requests_incoming = new ArrayList<Integer>();
	ArrayList<Integer> unite_offers_outgoing = new ArrayList<Integer>();
	ArrayList<Integer> unite_offers_incoming = new ArrayList<Integer>();
	ArrayList<Integer> chat_list = new ArrayList<Integer>();
	ArrayList<Integer> reverse_chat_list = new ArrayList<Integer>();

	ArrayList<Integer> goals_token = new ArrayList<Integer>();
	ArrayList<Float> goals_winnings = new ArrayList<Float>();

	ArrayList<Integer> goals_monthly_token = new ArrayList<Integer>();
	ArrayList<Float> goals_monthly_winnings = new ArrayList<Float>();

	ArrayList<Integer> map_flags_token= new ArrayList<Integer>();
	ArrayList<String> map_flags_title = new ArrayList<String>();

	ArrayList<ObjectRecord> objects = new ArrayList<ObjectRecord>();
	HashMap<Integer, Integer> builds = new HashMap<Integer,Integer>();

	ArrayList<AreaData> areas = new ArrayList<AreaData>();

	HashMap<Integer,QuestRecord> quest_records = new HashMap<Integer,QuestRecord>();

  // Subscription information
	ArrayList<Integer> bonus_credits_subscriptions = new ArrayList<Integer>();
	ArrayList<Integer> bonus_rebirth_subscriptions = new ArrayList<Integer>();
	ArrayList<Integer> bonus_xp_subscriptions = new ArrayList<Integer>();
	ArrayList<Integer> bonus_manpower_subscriptions = new ArrayList<Integer>();

  // Transient data
	public int chat_count = -1;
  public long log_suspect_expire_time = 0;
  public int log_suspect_init_nationID = 0;
	public int num_members_online = 0;
	public HashMap<Integer,Integer> hostile_nations = new HashMap<Integer,Integer>();
	public int prev_reset_time = 0;
	public float prev_reset_energy_fraction = 0f, prev_reset_manpower_fraction = 0f;
	public float prev_attack_ratio = 0f;
	public int processID = 0;
	public int processData = 0;

	public NationData(int _ID)
	{
		super(Constants.DT_NATION, _ID);
	}

	public boolean GetFlag(int _flag)
	{
		return ((flags & _flag) != 0);
	}

	public Footprint GetFootprint(int _mapID)
	{
		if (_mapID <= 0) {
			return null;
		} else if (_mapID == Constants.MAINLAND_MAP_ID) {
			return mainland_footprint;
		}	else if (_mapID == homeland_mapID) {
			return homeland_footprint;
		}
		else if (_mapID >= Raid.RAID_ID_BASE)
		{
			// Get the data for the raid with this ID.
			RaidData raidData = (RaidData)DataManager.GetData(Constants.DT_RAID, _mapID, false);

			if (raidData == null)
			{
				Output.PrintToScreen("ERROR: NationData.GettFootprint(): RaidData not found for _mapID " + _mapID);
				Output.PrintStackTrace();
				return null;
			}

			if (raidData.attacker_nationID == ID) {
				return raidData.attacker_footprint;
			}

			if (raidData.defender_nationID == ID) {
				return raidData.defender_footprint;
			}

			Output.PrintToScreen("ERROR: NationData.GetFootprint() for nation " + ID + ": RaidData has no footprint for this nation. Attacker: " + raidData.attacker_nationID + ", defender: " + raidData.defender_nationID);
			Output.PrintStackTrace();
			return null;
		}

		return null;
	}

	public QuestRecord GetQuestRecord(int _quest_id, boolean _create)
	{
		if (quest_records.containsKey(_quest_id))
		{
			return quest_records.get(_quest_id);
		}
		else
		{
			if (_create)
			{
				QuestRecord new_record = new QuestRecord();
				new_record.ID = _quest_id;
				quest_records.put(_quest_id, new_record);
				return new_record;
			}
			else
			{
				return null;
			}
		}
	}

	public void AddObjectRecord(int _landmapID, int _blockX, int _blockY, int _objectID)
	{
		objects.add(new ObjectRecord(_landmapID, _blockX, _blockY, _objectID));
	}

	public void RemoveObjectRecord(int _landmapID, int _blockX, int _blockY)
	{
		for (int i = 0; i < objects.size(); i++)
		{
			ObjectRecord cur_record = objects.get(i);
			if ((cur_record.blockX == _blockX) && (cur_record.blockY == _blockY) && (cur_record.landmapID == _landmapID))
			{
				objects.remove(i);
				return;
			}
		}
	}

	public int GetBuildCount(int _buildID)
	{
		return builds.containsKey(_buildID) ? builds.get(_buildID) : 0;
	}

	public int ModifyBuildCount(int _buildID, int _delta)
	{
		int count = builds.containsKey(_buildID) ? builds.get(_buildID) : 0;
		count = Math.max(0, count + _delta);
		builds.put(_buildID, count);

		// Broadcast this change in build count.
		OutputEvents.BroadcastBuildCountEvent(ID, _buildID, count);

		return count;
	}

	public void RecordHostileNation(int _nationID)
	{
		if ((num_members_online == 0) && (hostile_nations.size() < 100)) {
			hostile_nations.put(_nationID, 1);
		}
	}

	public boolean IsHostileNation(int _nationID)
	{
		return (num_members_online == 0) && hostile_nations.containsKey(_nationID);
	}

	public void ClearHostileNations()
	{
			hostile_nations.clear();
	}

	public void SetFlags(int _flags)
	{
		// If there is no change, do nothing.
		if (_flags == flags) {
			return;
		}

		// Record previous flags value.
		int prev_flags = flags;

		// Record the nation's new flags value.
		flags = _flags;

		// Broadcast the nation's new flags value to all of the nation's players.
		OutputEvents.BroadcastNationFlagsEvent(ID, flags);

		// If the nation's incognito state has changed...
		if ((prev_flags & Constants.NF_INCOGNITO) != (flags & Constants.NF_INCOGNITO))
		{
			// Record time at which the nation went incognito.
			prev_go_incognito_time = Constants.GetTime();

			// Broadcast message to all players with updated nation appearance data.
			OutputEvents.BroadcastNationDataEvent(this);
		}

		// Mark the nation's data to be updated
		DataManager.MarkForUpdate(this);
	}

	public void DetermineGeographicEfficiency(int _landmapID)
	{
		Footprint fp = GetFootprint(_landmapID);
		int excess_area = Math.max(0, fp.area - GetSupportableArea(_landmapID));
		fp.geo_efficiency_base = (fp.perimeter == 0) ? 0.0f : ((float)(fp.area - fp.border_area - excess_area) / (float)fp.perimeter); // NOTE: Allowed to be negative.
	}

	public void DetermineManpowerBurnRate()
	{
		float final_manpower_max = GetMainlandManpowerMax();

		manpower_burn_rate = 0f;
		manpower_burn_rate += Math.pow(Math.max(0f, (tech_object / tech_perm) - Constants.RESOURCE_BONUS_CAP), Constants.MANPOWER_BURN_EXPONENT) * (Constants.MANPOWER_BURN_FRACTION_OF_MANPOWER_MAX * final_manpower_max);
		manpower_burn_rate += Math.pow(Math.max(0f, (bio_object / bio_perm) - Constants.RESOURCE_BONUS_CAP), Constants.MANPOWER_BURN_EXPONENT) * (Constants.MANPOWER_BURN_FRACTION_OF_MANPOWER_MAX * final_manpower_max);
		manpower_burn_rate += Math.pow(Math.max(0f, (psi_object / psi_perm) - Constants.RESOURCE_BONUS_CAP), Constants.MANPOWER_BURN_EXPONENT) * (Constants.MANPOWER_BURN_FRACTION_OF_MANPOWER_MAX * final_manpower_max);
		manpower_burn_rate += Math.pow(Math.max(0f, (energy_rate_object / energy_rate_perm) - Constants.RESOURCE_BONUS_CAP), Constants.MANPOWER_BURN_EXPONENT) * (Constants.MANPOWER_BURN_FRACTION_OF_MANPOWER_MAX * final_manpower_max);
		manpower_burn_rate += Math.pow(Math.max(0f, (manpower_rate_object / manpower_rate_perm) - Constants.RESOURCE_BONUS_CAP), Constants.MANPOWER_BURN_EXPONENT) * (Constants.MANPOWER_BURN_FRACTION_OF_MANPOWER_MAX * final_manpower_max);
		manpower_burn_rate += Math.pow(Math.max(0f, (xp_multiplier_object / xp_multiplier_perm) - Constants.RESOURCE_BONUS_CAP), Constants.MANPOWER_BURN_EXPONENT) * (Constants.MANPOWER_BURN_FRACTION_OF_MANPOWER_MAX * final_manpower_max);
	}

	public float GetFinalStatTech(int _landmapID)
	{
		return (tech_perm + tech_temp + (GetFinalGeoEfficiency(_landmapID) * tech_object)) * tech_mult;
	}

	public float GetFinalStatBio(int _landmapID)
	{
		return (bio_perm + bio_temp + (GetFinalGeoEfficiency(_landmapID) * bio_object)) * bio_mult;
	}

	public float GetFinalStatPsi(int _landmapID)
	{
		return (psi_perm + psi_temp + (GetFinalGeoEfficiency(_landmapID) * psi_object)) * psi_mult;
	}

	public float GetFinalXPMultiplier(int _landmapID)
	{
		return (xp_multiplier_perm + xp_multiplier_temp + (GetFinalGeoEfficiency(_landmapID) * xp_multiplier_object));
	}

	public int GetSupportableArea(int _landmapID)
	{
		return (int)((Constants.SUPPORTABLE_AREA_BASE + ((level - 1) * Constants.SUPPORTABLE_AREA_PER_LEVEL)) * ((_landmapID == Constants.MAINLAND_MAP_ID) ? 1f : ((_landmapID >= Raid.RAID_ID_BASE) ? Constants.SUPPORTABLE_AREA_RAIDLAND_FRACTION : Constants.SUPPORTABLE_AREA_HOMELAND_FRACTION)));
	}

	public float GetFinalGeoEfficiency(int _landmapID)
	{
		// Get footprint
		Footprint footprint = GetFootprint(_landmapID);
		//if (footprint == null) Output.PrintToScreen("ERROR: No footprint found for nation " + name + " on landmap " + _landmapID);

		return Math.min(Math.max(footprint.geo_efficiency_base + geo_efficiency_modifier, Constants.GEO_EFFICIENCY_MIN), Constants.GEO_EFFICIENCY_MAX);
	}

	public float GetFinalEnergyBurnRate(int _landmapID)
	{
		// Get footprint
		Footprint footprint = GetFootprint(_landmapID);

		float max_energy_rate = GetFinalEnergyRate(_landmapID);

		float energy_burn_rate = footprint.energy_burn_rate;

		// If the nation is incognito, add the incognito energy burn to the nation's mainland energy burn rate.
		if ((_landmapID == Constants.MAINLAND_MAP_ID) && GetFlag(Constants.NF_INCOGNITO)) {
			energy_burn_rate += (GetFinalEnergyRate(_landmapID) * Constants.INCOGNITO_ENERGY_BURN);
		}

		if (energy_burn_rate > max_energy_rate) {
			return (float)Math.pow(energy_burn_rate / max_energy_rate, Constants.OVERBURN_POWER) * max_energy_rate;
		} else {
			return energy_burn_rate;
		}
	}

	public float GetFinalManpowerRate(int _landmapID)
	{
		if (_landmapID == Constants.MAINLAND_MAP_ID) {
			return (manpower_rate_perm + manpower_rate_temp + (GetFinalGeoEfficiency(_landmapID) * manpower_rate_object)) * manpower_rate_mult;
		} else if (_landmapID >= Raid.RAID_ID_BASE) {
			return 0f;
		} else {
			return (manpower_rate_perm + manpower_rate_temp + (GetFinalGeoEfficiency(_landmapID) * manpower_rate_object)) * manpower_rate_mult * (Constants.MANPOWER_RATE_HOMELAND_FRACTION / Constants.manpower_gen_multiplier);
		}
	}

	public float GetFinalManpowerRateMinusBurn(int _landmapID)
	{
		if (_landmapID == Constants.MAINLAND_MAP_ID) {
			return (((manpower_rate_perm + manpower_rate_temp + (GetFinalGeoEfficiency(_landmapID) * manpower_rate_object)) * manpower_rate_mult) - manpower_burn_rate);
		} else if (_landmapID >= Raid.RAID_ID_BASE) {
			return 0f;
		} else {
			return (((manpower_rate_perm + manpower_rate_temp + (GetFinalGeoEfficiency(_landmapID) * manpower_rate_object)) * manpower_rate_mult) - manpower_burn_rate) * (Constants.MANPOWER_RATE_HOMELAND_FRACTION / Constants.manpower_gen_multiplier);
		}
	}
/*
	public float GetFinalManpowerRateMinusBurn(int _landmapID)
	{
		return (((manpower_rate_perm + manpower_rate_temp + (GetFinalGeoEfficiency(_landmapID) * manpower_rate_object)) * manpower_rate_mult) - manpower_burn_rate) * ((_landmapID == Constants.MAINLAND_MAP_ID) ? 1f : ((_landmapID >= Raid.RAID_ID_BASE) ? 0f : Constants.MANPOWER_RATE_HOMELAND_FRACTION));
	}
*/
	public float GetFinalEnergyRate(int _landmapID)
	{
		return (energy_rate_perm + energy_rate_temp + (GetFinalGeoEfficiency(_landmapID) * energy_rate_object)) * energy_rate_mult;
	}

	public float GetMainlandManpowerMax()
	{
		return manpower_max * manpower_max_mult;
	}

	public float GetFinalManpowerMax(int _landmapID)
	{
		if (_landmapID == Constants.MAINLAND_MAP_ID) {
			return manpower_max * manpower_max_mult;
		} else if (_landmapID >= Raid.RAID_ID_BASE) {
			return manpower_max * manpower_max_mult * (Constants.MANPOWER_MAX_HOMELAND_FRACTION / Constants.manpower_gen_multiplier);
		} else {
			return manpower_max * manpower_max_mult * (Constants.MANPOWER_MAX_HOMELAND_FRACTION / Constants.manpower_gen_multiplier);
		}
	}

	public float GetFinalEnergyMax()
	{
		return energy_max * energy_max_mult;
	}

	public int GetFinalHitPointsPerSquare()
	{
		// Determine how many hit points each of this nation's squares has.
		int result = (int)((float)hit_points_base * hp_per_square_mult + 0.5f);

		// If the nation's manpower has reched 0, and they're burning manpower faster than they're generating it, then decrease the number of hit points per square.
		// It will be decreased down to the minimum (1 hp) if the nation is burning manpower twice as fast as it's being generated.
		if (mainland_footprint.manpower == 0)
		{
			// Determine the nation's manpower rate minus its manpower burn rate.
			float mp_rate_minus_burn = GetFinalManpowerRateMinusBurn(Constants.MAINLAND_MAP_ID);

			// If the nation is burning manpower faster than it's generating it...
			if (mp_rate_minus_burn < 0)
			{
				// Determine what fraction of the square's hit points to remove. Reaches minimum of 1 hp if burning manpower twice as fast as generating it.
				float hit_point_penalty_fraction = Math.min(1f, -mp_rate_minus_burn / GetFinalManpowerRate(Constants.MAINLAND_MAP_ID));

				// Determine final number of hit points after applying manpower burn penalty, and do not allow value lower than 1.
				result = (int)Math.max(1, (1f - hit_point_penalty_fraction) * result);
			}
		}

		return result;
	}

	public float GetFinalHitPointsRate()
	{
		return hit_points_rate * hp_restore_mult;
	}

	public float GetFinalManpowerPerAttack()
	{
		return manpower_per_attack * attack_manpower_mult;
	}

	public int GetFinalRebirthAvailableLevel()
	{
		return Constants.REBIRTH_AVAILABLE_LEVEL + GetRebirthLevelBonus();
	}

	public int GetRebirthLevelBonus()
	{
		return Math.min(rebirth_count * Constants.REBIRTH_LEVEL_BONUS, Constants.MAX_REBIRTH_LEVEL_BONUS);
	}

	public int GetNumCreditsPurchasedThisMonth()
	{
		if (prev_buy_credits_month == Constants.GetFullMonth()) {
			return prev_buy_credits_month_amount;
		} else {
			return 0;
		}
	}

	public int GetNumCreditsReceivedThisMonth()
	{
		if (prev_buy_credits_month == Constants.GetFullMonth()) {
			return prev_receive_credits_month_amount;
		} else {
			return 0;
		}
	}

	public int GetNumCreditsAllowedToBuyThisMonth()
	{
		if (Constants.max_buy_credits_per_month == -1) {
			return -1; // Unlimited
		}

		// Determine how many credits the nation can still buy this month.
		int num_credits_received_rounded_down = ((int)(GetNumCreditsReceivedThisMonth() / Constants.BUY_CREDITS_AMOUNT[0]) * Constants.BUY_CREDITS_AMOUNT[0]); // Round down to multiple of lowest credit package amount, so that gifting small numbers of credits won't prevent a nation from buying a package.
		return Math.max(0, Constants.max_buy_credits_per_month - (GetNumCreditsPurchasedThisMonth() + num_credits_received_rounded_down));
	}

	public int GetTransferrableCredits()
	{
		return (int)(game_money_purchased + game_money_won);
	}

	public boolean RollForCrit()
	{
		return Constants.random.nextFloat() <= crit_chance;
	}

	public float DetermineDiscountedEnergyBurn(BuildData _build_data)
	{
		// Determine the given structure's energy burn rate for this nation, factoring in any discount.
		return Math.max(0, 1.0f - ((_build_data.type == BuildData.TYPE_WALL) ? wall_discount : structure_discount)) * (float)_build_data.energy_burn_rate;
	}

	public boolean ReadData()
	{
		int file_version;
		boolean result = true;

		String sql = "SELECT " +
		"version," +
		"creation_time, " +
		"birth_time, " +
		"prev_use_time, " +
		"prev_active_time, " +
		"name, " +
		"password, " +
		"level, " +
		"xp, " +
		"pending_xp, " +
		"advance_points, " +
		"r, " +
		"g, " +
		"b, " +
		"emblem_index, " +
		"emblem_color, " +
		"veteran, " +
		"area_visibility_updated, " +
		"nextTechExpireTime, " +
		"nextTechExpire, " +
		"targetAdvanceID, " +
		"prev_free_migration_time, " +
		"prev_unite_time, " +
		"prev_go_incognito_time, " +
		"game_money, " +
		"game_money_purchased, " +
		"game_money_won, " +
		"total_game_money_purchased, " +
		"prize_money, " +
		"prize_money_history, " +
		"prize_money_history_monthly, " +
		"prev_prize_money_received_time, " +
		"money_spent, " +
		"raid_earnings_history, " +
		"raid_earnings_history_monthly, " +
		"orb_shard_earnings_history, " +
		"orb_shard_earnings_history_monthly, " +
		"medals_history, " +
		"medals_history_monthly, " +
		"level_history, " +
		"xp_history, " +
		"xp_history_monthly, " +
		"tournament_trophies_history, " +
		"tournament_trophies_history_monthly, " +
		"donated_energy_history, " +
		"donated_energy_history_monthly, " +
		"donated_manpower_history, " +
		"donated_manpower_history_monthly, " +
		"quests_completed, " +
		"quests_completed_monthly, " +
		"captures_history, " +
		"captures_history_monthly, " +
		"max_area, " +
		"max_area_monthly, " +
		"rebirth_countdown, " +
		"rebirth_countdown_purchased, " +
		"rebirth_countdown_start, " +
		"flags, " +
		"prev_buy_energy_day, " +
		"buy_energy_day_amount, " +
		"prev_buy_credits_month, " +
		"prev_buy_credits_month_amount, " +
		"prev_receive_credits_month_amount, " +
		"prev_discovery_day, " +
		"prev_message_send_day, " +
		"message_send_count, " +
		"prev_alliance_request_day, " +
		"alliance_request_count, " +
		"rebirth_count, " +
		"reset_advances_count, " +
		"super_nation, " +
		"energy, " +
		"energy_max, " +
		"manpower, " +
		"manpower_max, " +
		"manpower_per_attack, " +
		"geo_efficiency_modifier, " +
		"hit_points_base, " +
		"hit_points_rate, " +
		"crit_chance, " +
		"salvage_value, " +
		"wall_discount, " +
		"structure_discount, " +
		"splash_damage, " +
		"max_num_alliances, " +
		"max_simultaneous_processes, " +
		"invisibility, " +
		"insurgency, " +
		"total_defense, " +
		"tech_mult, " +
		"bio_mult, " +
		"psi_mult, " +
		"manpower_rate_mult, " +
		"energy_rate_mult, " +
		"manpower_max_mult, " +
		"energy_max_mult, " +
		"hp_per_square_mult, " +
		"hp_restore_mult, " +
		"attack_manpower_mult, " +
		"tech_perm, " +
		"tech_temp, " +
		"tech_object, " +
		"bio_perm, " +
		"bio_temp, " +
		"bio_object, " +
		"psi_perm, " +
		"psi_temp, " +
		"psi_object, " +
		"energy_rate_perm, " +
		"energy_rate_temp, " +
		"energy_rate_object, " +
		"manpower_rate_perm, " +
		"manpower_rate_temp, " +
		"manpower_rate_object, " +
		"xp_multiplier_perm, " +
		"xp_multiplier_temp, " +
		"xp_multiplier_object, " +
		"shared_energy_capacity, " +
		"shared_manpower_capacity, " +
		"shared_energy_fill, " +
		"shared_manpower_fill, " +
		"shared_energy_xp_per_hour, " +
		"shared_manpower_xp_per_hour, " +
		"num_share_builds, " +
		"manpower_burn_rate, " +
		"prev_update_stats_time, " +
		"tournament_start_day, " +
		"tournament_active, " +
		"tournament_rank, " +
		"trophies_available, " +
		"trophies_banked, " +
		"trophies_potential, " +
		"shard_red_fill, " +
		"shard_green_fill, " +
		"shard_blue_fill, " +
		"raid_eligible, " +
		"raidID, " +
		"raid_attacker_medals, " +
		"raid_defender_medals, " +
		"raid_shield_end_time, " +
		"raid_prev_candidates, " +
		"raid_attack_log, " +
		"raid_defense_log, " +
		"homeland_mapID, " +
		"prev_modify_homeland_time, " +
		"homeland_x0, " +
		"homeland_x1, " +
		"homeland_y0, " +
		"homeland_y1, " +
    "homeland_min_x0, " +
		"homeland_min_y0, " +
		"homeland_max_x1, " +
		"homeland_max_y1, " +
    "homeland_max_x0, " +
		"homeland_area, " +
		"homeland_border_area, " +
		"homeland_perimeter, " +
		"homeland_geo_efficiency_base, " +
		"homeland_energy_burn_rate, " +
		"homeland_manpower, " +
		"homeland_prev_buy_manpower_day, " +
		"homeland_buy_manpower_day_amount, " +
		"mainland_x0, " +
		"mainland_x1, " +
		"mainland_y0, " +
		"mainland_y1, " +
    "mainland_min_x0, " +
		"mainland_min_y0, " +
		"mainland_max_x1, " +
		"mainland_max_y1, " +
    "mainland_max_x0, " +
		"mainland_area, " +
		"mainland_border_area, " +
		"mainland_perimeter, " +
		"mainland_geo_efficiency_base, " +
		"mainland_energy_burn_rate, " +
		"mainland_manpower, " +
		"mainland_prev_buy_manpower_day, " +
		"mainland_buy_manpower_day_amount, " +
		"users, " +
		"alliances_active, " +
		"alliances_requests_outgoing, " +
		"alliances_requests_incoming, " +
		"unite_requests_outgoing, " +
		"unite_requests_incoming, " +
		"unite_offers_outgoing, " +
		"unite_offers_incoming, " +
		"chat_list, " +
		"reverse_chat_list, " +
		"goals_token, " +
		"goals_winnings, " +
		"goals_monthly_token, " +
		"goals_monthly_winnings, " +
		"map_flags_token, " +
		"map_flags_title, " +
		"objects, " +
		"builds, " +
		"areas, " +
		"quest_records, " +
		"bonus_credits_subscriptions, " +
		"bonus_rebirth_subscriptions, " +
		"bonus_xp_subscriptions, " +
		"bonus_manpower_subscriptions " +
		"FROM " + db_table_name + " where ID= '" + ID + "'";

		try {
			// Create statement for use with the DB.
			Statement stmt = db.createStatement();

			// Get the result set for the sql query
			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				file_version  = rs.getInt("version");
				if (rs.wasNull()) return false; // Version was not set, so data has not been stored correctly. Return false.
				creation_time  = rs.getInt("creation_time");
				birth_time  = rs.getInt("birth_time");
				prev_use_time  = rs.getInt("prev_use_time");
				prev_active_time  = rs.getInt("prev_active_time");
				name = rs.getString("name");
				password = rs.getString("password");
				level = rs.getInt("level");
				xp = rs.getInt("xp");
				pending_xp = rs.getInt("pending_xp");
				advance_points = rs.getInt("advance_points");
				r = rs.getInt("r");
				g = rs.getInt("g");
				b = rs.getInt("b");
				emblem_index = rs.getInt("emblem_index");
				emblem_color = rs.getInt("emblem_color");
				veteran = rs.getBoolean("veteran");
				area_visibility_updated = rs.getBoolean("area_visibility_updated");
				nextTechExpireTime = rs.getInt("nextTechExpireTime");
				nextTechExpire = rs.getInt("nextTechExpire");
				targetAdvanceID = rs.getInt("targetAdvanceID");
				prev_free_migration_time = rs.getInt("prev_free_migration_time");
				prev_unite_time = rs.getInt("prev_unite_time");
				prev_go_incognito_time = rs.getInt("prev_go_incognito_time");
				game_money = rs.getInt("game_money");
				game_money_purchased = rs.getInt("game_money_purchased");
				game_money_won = rs.getInt("game_money_won");
				total_game_money_purchased = rs.getInt("total_game_money_purchased");
				prize_money = rs.getFloat("prize_money");
				prize_money_history = rs.getFloat("prize_money_history");
				prize_money_history_monthly = rs.getFloat("prize_money_history_monthly");
				prev_prize_money_received_time = rs.getInt("prev_prize_money_received_time");
				money_spent = rs.getFloat("money_spent");
				raid_earnings_history = rs.getFloat("raid_earnings_history");
				raid_earnings_history_monthly = rs.getFloat("raid_earnings_history_monthly");
				orb_shard_earnings_history = rs.getFloat("orb_shard_earnings_history");
				orb_shard_earnings_history_monthly = rs.getFloat("orb_shard_earnings_history_monthly");
				medals_history = rs.getInt("medals_history");
				medals_history_monthly = rs.getInt("medals_history_monthly");
				level_history = rs.getInt("level_history");
				xp_history = rs.getInt("xp_history");
				xp_history_monthly = rs.getInt("xp_history_monthly");
				tournament_trophies_history = rs.getFloat("tournament_trophies_history");
				tournament_trophies_history_monthly = rs.getFloat("tournament_trophies_history_monthly");
				donated_energy_history = rs.getFloat("donated_energy_history");
				donated_energy_history_monthly = rs.getFloat("donated_energy_history_monthly");
				donated_manpower_history = rs.getFloat("donated_manpower_history");
				donated_manpower_history_monthly = rs.getFloat("donated_manpower_history_monthly");
				quests_completed = rs.getInt("quests_completed");
				quests_completed_monthly = rs.getInt("quests_completed_monthly");
				captures_history = rs.getInt("captures_history");
				captures_history_monthly = rs.getInt("captures_history_monthly");
				max_area = rs.getInt("max_area");
				max_area_monthly = rs.getInt("max_area_monthly");
				rebirth_countdown = rs.getFloat("rebirth_countdown");
				rebirth_countdown_purchased = rs.getFloat("rebirth_countdown_purchased");
				rebirth_countdown_start = rs.getInt("rebirth_countdown_start");
				flags = rs.getInt("flags");
				prev_buy_energy_day = rs.getInt("prev_buy_energy_day");
				buy_energy_day_amount = rs.getInt("buy_energy_day_amount");
				prev_buy_credits_month = rs.getInt("prev_buy_credits_month");
				prev_buy_credits_month_amount = rs.getInt("prev_buy_credits_month_amount");
				prev_receive_credits_month_amount = rs.getInt("prev_receive_credits_month_amount");
				prev_discovery_day = rs.getInt("prev_discovery_day");
				prev_message_send_day = rs.getInt("prev_message_send_day");
				message_send_count = rs.getInt("message_send_count");
				prev_alliance_request_day = rs.getInt("prev_alliance_request_day");
				alliance_request_count = rs.getInt("alliance_request_count");
				rebirth_count = rs.getInt("rebirth_count");
				reset_advances_count = rs.getInt("reset_advances_count");
				super_nation = rs.getBoolean("super_nation");
				energy = rs.getFloat("energy");
				energy_max = rs.getFloat("energy_max");
				manpower = rs.getFloat("manpower");
				manpower_max = rs.getFloat("manpower_max");
				manpower_per_attack = rs.getFloat("manpower_per_attack");
				geo_efficiency_modifier = rs.getFloat("geo_efficiency_modifier");
				hit_points_base = rs.getFloat("hit_points_base");
				hit_points_rate = rs.getFloat("hit_points_rate");
				crit_chance = rs.getFloat("crit_chance");
				salvage_value = rs.getFloat("salvage_value");
				wall_discount = rs.getFloat("wall_discount");
				structure_discount = rs.getFloat("structure_discount");
				splash_damage = rs.getFloat("splash_damage");
				max_num_alliances = rs.getInt("max_num_alliances");
				max_simultaneous_processes = rs.getInt("max_simultaneous_processes");
				invisibility = rs.getBoolean("invisibility");
				insurgency = rs.getBoolean("insurgency");
				total_defense = rs.getBoolean("total_defense");
				tech_mult = rs.getFloat("tech_mult");
				bio_mult = rs.getFloat("bio_mult");
				psi_mult = rs.getFloat("psi_mult");
				manpower_rate_mult = rs.getFloat("manpower_rate_mult");
				energy_rate_mult = rs.getFloat("energy_rate_mult");
				manpower_max_mult = rs.getFloat("manpower_max_mult");
				energy_max_mult = rs.getFloat("energy_max_mult");
				hp_per_square_mult = rs.getFloat("hp_per_square_mult");
				hp_restore_mult = rs.getFloat("hp_restore_mult");
				attack_manpower_mult = rs.getFloat("attack_manpower_mult");
				tech_perm = rs.getFloat("tech_perm");
				tech_temp = rs.getFloat("tech_temp");
				tech_object = rs.getFloat("tech_object");
				bio_perm = rs.getFloat("bio_perm");
				bio_temp = rs.getFloat("bio_temp");
				bio_object = rs.getFloat("bio_object");
				psi_perm = rs.getFloat("psi_perm");
				psi_temp = rs.getFloat("psi_temp");
				psi_object = rs.getFloat("psi_object");
				energy_rate_perm = rs.getFloat("energy_rate_perm");
				energy_rate_temp = rs.getFloat("energy_rate_temp");
				energy_rate_object = rs.getFloat("energy_rate_object");
				manpower_rate_perm = rs.getFloat("manpower_rate_perm");
				manpower_rate_temp = rs.getFloat("manpower_rate_temp");
				manpower_rate_object = rs.getFloat("manpower_rate_object");
				xp_multiplier_perm = rs.getFloat("xp_multiplier_perm");
				xp_multiplier_temp = rs.getFloat("xp_multiplier_temp");
				xp_multiplier_object = rs.getFloat("xp_multiplier_object");
				shared_energy_capacity = rs.getInt("shared_energy_capacity");
				shared_manpower_capacity = rs.getInt("shared_manpower_capacity");
				shared_energy_fill = rs.getFloat("shared_energy_fill");
				shared_manpower_fill = rs.getFloat("shared_manpower_fill");
				shared_energy_xp_per_hour = rs.getInt("shared_energy_xp_per_hour");
				shared_manpower_xp_per_hour = rs.getInt("shared_manpower_xp_per_hour");
				num_share_builds = rs.getInt("num_share_builds");
				manpower_burn_rate = rs.getFloat("manpower_burn_rate");
				prev_update_stats_time = rs.getInt("prev_update_stats_time");
				tournament_start_day = rs.getInt("tournament_start_day");
				tournament_active = (rs.getInt("tournament_active") == 0) ? false : true;
				tournament_rank = rs.getInt("tournament_rank");
				trophies_available = rs.getFloat("trophies_available");
				trophies_banked = rs.getFloat("trophies_banked");
				trophies_potential = rs.getFloat("trophies_potential");
				shard_red_fill = rs.getFloat("shard_red_fill");
				shard_green_fill = rs.getFloat("shard_green_fill");
				shard_blue_fill = rs.getFloat("shard_blue_fill");
				raid_eligible = (rs.getInt("raid_eligible") == 0) ? false : true;
				raidID = rs.getInt("raidID");
				raid_attacker_medals = rs.getInt("raid_attacker_medals");
				raid_defender_medals = rs.getInt("raid_defender_medals");
				raid_shield_end_time = rs.getInt("raid_shield_end_time");
				raid_prev_candidates = JSONToIntArray(rs.getString("raid_prev_candidates"));
				raid_attack_log = JSONToIntArray(rs.getString("raid_attack_log"));
				raid_defense_log = JSONToIntArray(rs.getString("raid_defense_log"));
				homeland_mapID = rs.getInt("homeland_mapID");
				prev_modify_homeland_time = rs.getInt("prev_modify_homeland_time");
				homeland_footprint.x0 = rs.getInt("homeland_x0");
				homeland_footprint.y0 = rs.getInt("homeland_y0");
				homeland_footprint.x1 = rs.getInt("homeland_x1");
				homeland_footprint.y1 = rs.getInt("homeland_y1");
				homeland_footprint.min_x0 = rs.getInt("homeland_min_x0");
				homeland_footprint.min_y0 = rs.getInt("homeland_min_y0");
				homeland_footprint.max_x1 = rs.getInt("homeland_max_x1");
				homeland_footprint.max_y1 = rs.getInt("homeland_max_y1");
				homeland_footprint.max_x0 = rs.getInt("homeland_max_x0");
				homeland_footprint.area = rs.getInt("homeland_area");
				homeland_footprint.border_area = rs.getInt("homeland_border_area");
				homeland_footprint.perimeter = rs.getInt("homeland_perimeter");
				homeland_footprint.geo_efficiency_base = rs.getFloat("homeland_geo_efficiency_base");
				homeland_footprint.energy_burn_rate = rs.getFloat("homeland_energy_burn_rate");
				homeland_footprint.manpower = rs.getInt("homeland_manpower");
				homeland_footprint.prev_buy_manpower_day = rs.getInt("homeland_prev_buy_manpower_day");
				homeland_footprint.buy_manpower_day_amount = rs.getInt("homeland_buy_manpower_day_amount");
				mainland_footprint.x0 = rs.getInt("mainland_x0");
				mainland_footprint.y0 = rs.getInt("mainland_y0");
				mainland_footprint.x1 = rs.getInt("mainland_x1");
				mainland_footprint.y1 = rs.getInt("mainland_y1");
				mainland_footprint.min_x0 = rs.getInt("mainland_min_x0");
				mainland_footprint.min_y0 = rs.getInt("mainland_min_y0");
				mainland_footprint.max_x1 = rs.getInt("mainland_max_x1");
				mainland_footprint.max_y1 = rs.getInt("mainland_max_y1");
				mainland_footprint.max_x0 = rs.getInt("mainland_max_x0");
				mainland_footprint.area = rs.getInt("mainland_area");
				mainland_footprint.border_area = rs.getInt("mainland_border_area");
				mainland_footprint.perimeter = rs.getInt("mainland_perimeter");
				mainland_footprint.geo_efficiency_base = rs.getFloat("mainland_geo_efficiency_base");
				mainland_footprint.energy_burn_rate = rs.getFloat("mainland_energy_burn_rate");
				mainland_footprint.manpower = rs.getInt("mainland_manpower");
				mainland_footprint.prev_buy_manpower_day = rs.getInt("mainland_prev_buy_manpower_day");
				mainland_footprint.buy_manpower_day_amount = rs.getInt("mainland_buy_manpower_day_amount");
				users = JSONToIntArray(rs.getString("users"));
				alliances_active = JSONToIntArray(rs.getString("alliances_active"));
				alliances_requests_outgoing = JSONToIntArray(rs.getString("alliances_requests_outgoing"));
				alliances_requests_incoming = JSONToIntArray(rs.getString("alliances_requests_incoming"));
				unite_requests_outgoing = JSONToIntArray(rs.getString("unite_requests_outgoing"));
				unite_requests_incoming = JSONToIntArray(rs.getString("unite_requests_incoming"));
				unite_offers_outgoing = JSONToIntArray(rs.getString("unite_offers_outgoing"));
				unite_offers_incoming = JSONToIntArray(rs.getString("unite_offers_incoming"));
				chat_list = JSONToIntArray(rs.getString("chat_list"));
				reverse_chat_list = JSONToIntArray(rs.getString("reverse_chat_list"));
				goals_token = JSONToIntArray(rs.getString("goals_token"));
				goals_winnings = JSONToFloatArray(rs.getString("goals_winnings"));
				goals_monthly_token = JSONToIntArray(rs.getString("goals_monthly_token"));
				goals_monthly_winnings = JSONToFloatArray(rs.getString("goals_monthly_winnings"));
				map_flags_token = JSONToIntArray(rs.getString("map_flags_token"));
				map_flags_title = JSONToStringArray(rs.getString("map_flags_title"));
				objects = ObjectRecord.JSONToObjectRecordArray(rs.getString("objects"));
				builds = JSONToIntIntMap(rs.getString("builds"));
				areas = AreaData.JSONToAreaDataArray(rs.getString("areas"));
				quest_records = QuestRecord.JSONToQuestRecordMap(rs.getString("quest_records"));
				bonus_credits_subscriptions = JSONToIntArray(rs.getString("bonus_credits_subscriptions"));
				bonus_rebirth_subscriptions = JSONToIntArray(rs.getString("bonus_rebirth_subscriptions"));
				bonus_xp_subscriptions = JSONToIntArray(rs.getString("bonus_xp_subscriptions"));
				bonus_manpower_subscriptions = JSONToIntArray(rs.getString("bonus_manpower_subscriptions"));
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

		// TEMPORARY
		if (mainland_footprint.max_x1 < mainland_footprint.x1)
		{
			mainland_footprint.min_x0 = mainland_footprint.x0;
			mainland_footprint.min_y0 = mainland_footprint.y0;
			mainland_footprint.max_x1 = mainland_footprint.x1;
			mainland_footprint.max_y1 = mainland_footprint.y1;
			mainland_footprint.max_x0 = mainland_footprint.x0;
		}

		// TEMPORARY
		if (tech_mult == 0)
		{
			tech_mult = 1f;
			bio_mult = 1f;
			psi_mult = 1f;
			manpower_rate_mult = 1f;
			energy_rate_mult = 1f;
			manpower_max_mult = 1f;
			energy_max_mult = 1f;
			hp_per_square_mult = 1f;
			hp_restore_mult = 1f;
			attack_manpower_mult = 1f;
		}

		//Output.PrintToScreen("Nation " + name + " footprint: " + mainland_footprint.x0 + "," + mainland_footprint.y0 + " to " + mainland_footprint.x1 + "," + mainland_footprint.y1 + " hist: " + mainland_footprint.min_x0 + "," + mainland_footprint.min_y0 + " to " + mainland_footprint.max_x1 + "," + mainland_footprint.max_y1 + ". max_x0: " + mainland_footprint.max_x0);

		return result;
	}

	public void WriteData()
	{
		String sql = "UPDATE " + db_table_name + " SET " +
		"version = '" + VERSION + "', " +
		"creation_time = '" + creation_time + "', " +
		"birth_time = '" + birth_time + "', " +
		"prev_use_time = '" + prev_use_time + "', " +
		"prev_active_time = '" + prev_active_time + "', " +
		"name = '" + PrepStringForMySQL(name) + "', " +
		"password = '" + PrepStringForMySQL(password) + "', " +
		"level = '" + level + "', " +
		"xp = '" + xp + "', " +
		"pending_xp = '" + pending_xp + "', " +
		"advance_points = '" + advance_points + "', " +
		"r = '" + r + "', " +
		"g = '" + g + "', " +
		"b = '" + b + "', " +
		"emblem_index = '" + emblem_index + "', " +
		"emblem_color = '" + emblem_color + "', " +
		//"area = '" + area + "', " +
		//"border_area = '" + border_area + "', " +
		//"perimeter = '" + perimeter + "', " +
		//"geo_efficiency_base = '" + geo_efficiency_base + "', " +
		"veteran = " + (veteran ? "TRUE" : "FALSE") + ", " +
		"area_visibility_updated = " + (area_visibility_updated ? "TRUE" : "FALSE") + ", " +
		"nextTechExpireTime = '" + nextTechExpireTime + "', " +
		"nextTechExpire = '" + nextTechExpire + "', " +
		"targetAdvanceID = '" + targetAdvanceID + "', " +
		"prev_free_migration_time = '" + prev_free_migration_time + "', " +
		"prev_unite_time = '" + prev_unite_time + "', " +
		"prev_go_incognito_time = '" + prev_go_incognito_time + "', " +
		"game_money = '" + (int)game_money + "', " +
		"game_money_purchased = '" + (int)game_money_purchased + "', " +
		"game_money_won = '" + (int)game_money_won + "', " +
		"total_game_money_purchased = '" + (int)total_game_money_purchased + "', " +
		"prize_money = '" + prize_money + "', " +
		"prize_money_history = '" + prize_money_history + "', " +
		"prize_money_history_monthly = '" + prize_money_history_monthly + "', " +
		"prev_prize_money_received_time = '" + prev_prize_money_received_time + "', " +
		"money_spent = '" + money_spent + "', " +
		"raid_earnings_history = '" + raid_earnings_history + "', " +
		"raid_earnings_history_monthly = '" + raid_earnings_history_monthly + "', " +
		"orb_shard_earnings_history = '" + orb_shard_earnings_history + "', " +
		"orb_shard_earnings_history_monthly = '" + orb_shard_earnings_history_monthly + "', " +
		"medals_history = '" + medals_history + "', " +
		"medals_history_monthly = '" + medals_history_monthly + "', " +
		"level_history = '" + level_history + "', " +
		"xp_history = '" + xp_history + "', " +
		"xp_history_monthly = '" + xp_history_monthly + "', " +
		"tournament_trophies_history = '" + tournament_trophies_history + "', " +
		"tournament_trophies_history_monthly = '" + tournament_trophies_history_monthly + "', " +
		"donated_energy_history = '" + donated_energy_history + "', " +
		"donated_energy_history_monthly = '" + donated_energy_history_monthly + "', " +
		"donated_manpower_history = '" + donated_manpower_history + "', " +
		"donated_manpower_history_monthly = '" + donated_manpower_history_monthly + "', " +
		"quests_completed = '" + quests_completed + "', " +
		"quests_completed_monthly = '" + quests_completed_monthly + "', " +
		"captures_history = '" + captures_history + "', " +
		"captures_history_monthly = '" + captures_history_monthly + "', " +
		"max_area = '" + max_area + "', " +
		"max_area_monthly = '" + max_area_monthly + "', " +
		"rebirth_countdown = '" + rebirth_countdown + "', " +
		"rebirth_countdown_purchased = '" + rebirth_countdown_purchased + "', " +
		"rebirth_countdown_start = '" + rebirth_countdown_start + "', " +
		"flags = '" + flags + "', " +
		"prev_buy_energy_day = '" + prev_buy_energy_day + "', " +
		"buy_energy_day_amount = '" + buy_energy_day_amount + "', " +
		"prev_buy_credits_month = '" + prev_buy_credits_month + "', " +
		"prev_buy_credits_month_amount = '" + prev_buy_credits_month_amount + "', " +
		"prev_receive_credits_month_amount = '" + prev_receive_credits_month_amount + "', " +
		"prev_discovery_day = '" + prev_discovery_day + "', " +
		"prev_message_send_day = '" + prev_message_send_day + "', " +
		"message_send_count = '" + message_send_count + "', " +
		"prev_alliance_request_day = '" + prev_alliance_request_day + "', " +
		"alliance_request_count = '" + alliance_request_count + "', " +
		"rebirth_count = '" + rebirth_count + "', " +
		"reset_advances_count = '" + reset_advances_count + "', " +
		"super_nation = " + (super_nation ? "TRUE" : "FALSE") + ", " +
		"energy = '" + energy + "', " +
		"energy_max = '" + energy_max + "', " +
		//"energy_rate = '" + energy_rate + "', " +
		"manpower = '" + manpower + "', " +
		"manpower_max = '" + manpower_max + "', " +
		//"manpower_rate = '" + manpower_rate + "', " +
		"manpower_per_attack = '" + manpower_per_attack + "', " +
		//"stat_tech = '" + stat_tech + "', " +
		//"stat_bio = '" + stat_bio + "', " +
		//"stat_psi = '" + stat_psi + "', " +
		"geo_efficiency_modifier = '" + geo_efficiency_modifier + "', " +
		//"xp_multiplier = '" + xp_multiplier + "', " +
		"hit_points_base = '" + hit_points_base + "', " +
		"hit_points_rate = '" + hit_points_rate + "', " +
		"crit_chance = '" + crit_chance + "', " +
		"salvage_value = '" + salvage_value + "', " +
		"wall_discount = '" + wall_discount + "', " +
		"structure_discount = '" + structure_discount + "', " +
		"splash_damage = '" + splash_damage + "', " +
		"max_num_alliances = '" + max_num_alliances + "', " +
		"max_simultaneous_processes = '" + max_simultaneous_processes + "', " +
		"invisibility = " + (invisibility ? "TRUE" : "FALSE") + ", " +
		"insurgency = " + (insurgency ? "TRUE" : "FALSE") + ", " +
		"total_defense = " + (total_defense ? "TRUE" : "FALSE") + ", " +
		"tech_mult = '" + tech_mult + "', " +
		"bio_mult = '" + bio_mult + "', " +
		"psi_mult = '" + psi_mult + "', " +
		"manpower_rate_mult = '" + manpower_rate_mult + "', " +
		"energy_rate_mult = '" + energy_rate_mult + "', " +
		"manpower_max_mult = '" + manpower_max_mult + "', " +
		"energy_max_mult = '" + energy_max_mult + "', " +
		"hp_per_square_mult = '" + hp_per_square_mult + "', " +
		"hp_restore_mult = '" + hp_restore_mult + "', " +
		"attack_manpower_mult = '" + attack_manpower_mult + "', " +
		"tech_perm = '" + tech_perm + "', " +
		"tech_temp = '" + tech_temp + "', " +
		"tech_object = '" + tech_object + "', " +
		"bio_perm = '" + bio_perm + "', " +
		"bio_temp = '" + bio_temp + "', " +
		"bio_object = '" + bio_object + "', " +
		"psi_perm = '" + psi_perm + "', " +
		"psi_temp = '" + psi_temp + "', " +
		"psi_object = '" + psi_object + "', " +
		"energy_rate_perm = '" + energy_rate_perm + "', " +
		"energy_rate_temp = '" + energy_rate_temp + "', " +
		"energy_rate_object = '" + energy_rate_object + "', " +
		"manpower_rate_perm = '" + manpower_rate_perm + "', " +
		"manpower_rate_temp = '" + manpower_rate_temp + "', " +
		"manpower_rate_object = '" + manpower_rate_object + "', " +
		"xp_multiplier_perm = '" + xp_multiplier_perm + "', " +
		"xp_multiplier_temp = '" + xp_multiplier_temp + "', " +
		"xp_multiplier_object = '" + xp_multiplier_object + "', " +
		"shared_energy_capacity = '" + shared_energy_capacity + "', " +
		"shared_manpower_capacity = '" + shared_manpower_capacity + "', " +
		"shared_energy_fill = '" + shared_energy_fill + "', " +
		"shared_manpower_fill = '" + shared_manpower_fill + "', " +
		"shared_energy_xp_per_hour = '" + shared_energy_xp_per_hour + "', " +
		"shared_manpower_xp_per_hour = '" + shared_manpower_xp_per_hour + "', " +
		"num_share_builds = '" + num_share_builds + "', " +
		"manpower_burn_rate = '" + manpower_burn_rate + "', " +
		"prev_update_stats_time = '" + prev_update_stats_time + "', " +
		"tournament_start_day = '" + tournament_start_day + "', " +
		"tournament_active = '" + (tournament_active ? 1 : 0) + "', " +
		"tournament_rank = '" + tournament_rank + "', " +
		"trophies_available = '" + trophies_available + "', " +
		"trophies_banked = '" + trophies_banked + "', " +
		"trophies_potential = '" + trophies_potential + "', " +
		"shard_red_fill = '" + shard_red_fill + "', " +
		"shard_green_fill = '" + shard_green_fill + "', " +
		"shard_blue_fill = '" + shard_blue_fill + "', " +
		"raid_eligible = '" + (raid_eligible ? 1 : 0) + "', " +
		"raidID = '" + raidID + "', " +
		"raid_attacker_medals = '" + raid_attacker_medals + "', " +
		"raid_defender_medals = '" + raid_defender_medals + "', " +
		"raid_shield_end_time = '" + raid_shield_end_time + "', " +
		"raid_prev_candidates = '" + IntArrayToJSON(raid_prev_candidates) + "', " +
		"raid_attack_log = '" + IntArrayToJSON(raid_attack_log) + "', " +
		"raid_defense_log = '" + IntArrayToJSON(raid_defense_log) + "', " +
		"homeland_mapID = '" + homeland_mapID + "', " +
		"prev_modify_homeland_time = '" + prev_modify_homeland_time + "', " +
		"homeland_x0 = '" + homeland_footprint.x0 + "', " +
		"homeland_y0 = '" + homeland_footprint.y0 + "', " +
		"homeland_x1 = '" + homeland_footprint.x1 + "', " +
		"homeland_y1 = '" + homeland_footprint.y1 + "', " +
		"homeland_min_x0 = '" + homeland_footprint.min_x0 + "', " +
		"homeland_min_y0 = '" + homeland_footprint.min_y0 + "', " +
		"homeland_max_x1 = '" + homeland_footprint.max_x1 + "', " +
		"homeland_max_y1 = '" + homeland_footprint.max_y1 + "', " +
		"homeland_max_x0 = '" + homeland_footprint.max_x0 + "', " +
		"homeland_area = '" + homeland_footprint.area + "', " +
		"homeland_border_area = '" + homeland_footprint.border_area + "', " +
		"homeland_perimeter = '" + homeland_footprint.perimeter + "', " +
		"homeland_geo_efficiency_base = '" + homeland_footprint.geo_efficiency_base + "', " +
		"homeland_energy_burn_rate = '" + homeland_footprint.energy_burn_rate + "', " +
		"homeland_manpower = '" + homeland_footprint.manpower + "', " +
		"homeland_prev_buy_manpower_day = '" + homeland_footprint.prev_buy_manpower_day + "', " +
		"homeland_buy_manpower_day_amount = '" + homeland_footprint.buy_manpower_day_amount + "', " +
		"mainland_x0 = '" + mainland_footprint.x0 + "', " +
		"mainland_y0 = '" + mainland_footprint.y0 + "', " +
		"mainland_x1 = '" + mainland_footprint.x1 + "', " +
		"mainland_y1 = '" + mainland_footprint.y1 + "', " +
		"mainland_min_x0 = '" + mainland_footprint.min_x0 + "', " +
		"mainland_min_y0 = '" + mainland_footprint.min_y0 + "', " +
		"mainland_max_x1 = '" + mainland_footprint.max_x1 + "', " +
		"mainland_max_y1 = '" + mainland_footprint.max_y1 + "', " +
		"mainland_max_x0 = '" + mainland_footprint.max_x0 + "', " +
		"mainland_area = '" + mainland_footprint.area + "', " +
		"mainland_border_area = '" + mainland_footprint.border_area + "', " +
		"mainland_perimeter = '" + mainland_footprint.perimeter + "', " +
		"mainland_geo_efficiency_base = '" + mainland_footprint.geo_efficiency_base + "', " +
		"mainland_energy_burn_rate = '" + mainland_footprint.energy_burn_rate + "', " +
		"mainland_manpower = '" + mainland_footprint.manpower + "', " +
		"mainland_prev_buy_manpower_day = '" + mainland_footprint.prev_buy_manpower_day + "', " +
		"mainland_buy_manpower_day_amount = '" + mainland_footprint.buy_manpower_day_amount + "', " +
		"users = '" + IntArrayToJSON(users) + "', " +
		"alliances_active = '" + IntArrayToJSON(alliances_active) + "', " +
		"alliances_requests_outgoing = '" + IntArrayToJSON(alliances_requests_outgoing) + "', " +
		"alliances_requests_incoming = '" + IntArrayToJSON(alliances_requests_incoming) + "', " +
		"unite_requests_outgoing = '" + IntArrayToJSON(unite_requests_outgoing) + "', " +
		"unite_requests_incoming = '" + IntArrayToJSON(unite_requests_incoming) + "', " +
		"unite_offers_outgoing = '" + IntArrayToJSON(unite_offers_outgoing) + "', " +
		"unite_offers_incoming = '" + IntArrayToJSON(unite_offers_incoming) + "', " +
		"chat_list = '" + IntArrayToJSON(chat_list) + "', " +
		"reverse_chat_list = '" + IntArrayToJSON(reverse_chat_list) + "', " +
		"goals_token = '" + IntArrayToJSON(goals_token) + "', " +
		"goals_winnings = '" + FloatArrayToJSON(goals_winnings) + "', " +
		"goals_monthly_token = '" + IntArrayToJSON(goals_monthly_token) + "', " +
		"goals_monthly_winnings = '" + FloatArrayToJSON(goals_monthly_winnings) + "', " +
		"map_flags_token = '" + IntArrayToJSON(map_flags_token) + "', " +
		"map_flags_title = '" + PrepStringForMySQL(StringArrayToJSON(map_flags_title)) + "', " +
		"objects = '" + PrepStringForMySQL(ObjectRecord.ObjectRecordArrayToJSON(objects)) + "', " +
		"builds = '" + PrepStringForMySQL(IntIntMapToJSON(builds)) + "', " +
		"areas = '" + PrepStringForMySQL(AreaData.AreaDataArrayToJSON(areas)) + "', " +
		"quest_records = '" + PrepStringForMySQL(QuestRecord.QuestRecordMapToJSON(quest_records)) + "', " +
		"bonus_credits_subscriptions = '" + IntArrayToJSON(bonus_credits_subscriptions) + "', " +
		"bonus_rebirth_subscriptions = '" + IntArrayToJSON(bonus_rebirth_subscriptions) + "', " +
		"bonus_xp_subscriptions = '" + IntArrayToJSON(bonus_xp_subscriptions) + "', " +
		"bonus_manpower_subscriptions = '" + IntArrayToJSON(bonus_manpower_subscriptions) + "' " +
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

		//// TEMP -- modify existing column
		//ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY shared_energy_capacity INT", true, false);
		//ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY shared_manpower_capacity INT", true, false);
		//ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY rebirth_countdown FLOAT", true, false);
		//ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY homeland_geo_efficiency_base FLOAT", true, false);
		//ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY xp FLOAT", true, false);
		//ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY xp_history FLOAT", true, false);
		//ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY homeland_energy_burn_rate FLOAT", true, false);
		//ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY mainland_energy_burn_rate FLOAT", true, false);
		//ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " MODIFY objects MEDIUMTEXT", true, false);

/*
		// TESTING
		//String sql = "select column_name from information_schema.columns where table_name = '" + db_table_name + "' and ordinal_position = 9";
		//String sql = "SHOW FULL COLUMNS FROM " + db_table_name;
		String sql = "select id,name from " + db_table_name + " where xp > 2000000000";
		//String sql = "update " + db_table_name + " set xp=2000000000 where xp > 2000000000";

		try {
			// Create statement for use with the DB.
			Statement stmt = db.createStatement();

			//stmt.executeUpdate(sql);

			// Get the result set for the sql query
			ResultSet rs = stmt.executeQuery(sql);

			//String result = "";
			//if (rs.next()) {
			//	result = rs.getString(1);
			//}
			//Output.PrintToScreen("Name of column 9: " + result);

			ResultSetMetaData rsmd = rs.getMetaData();
			int columnsNumber = rsmd.getColumnCount();
			while (rs.next()) {
			    for (int i = 1; i <= columnsNumber; i++) {
			        if (i > 1) System.out.print(",  ");
			        String columnValue = rs.getString(i);
			        System.out.print(columnValue + " " + rsmd.getColumnName(i));
			    }
			    System.out.println("");
			}

			// Close statement and result set (must be done explicitly to avoid memory leak).
			try { if (rs != null) rs.close(); } catch (Exception e) {};
			try { if (stmt != null) stmt.close(); } catch (Exception e) {};
		}
		catch(Exception e)
		{
			Output.PrintToScreen("Failed to get name of column 9.");
			Output.PrintException(e);
			e.printStackTrace();
		}
*/

		// Attempt to create each column
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD creation_time INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD birth_time INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD prev_use_time INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD prev_active_time INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD name TINYTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD password TINYTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD level INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD xp FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD pending_xp INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD advance_points INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD r INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD g INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD b INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD emblem_index INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD emblem_color INT", true, false);
		//ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD area INT", true, false);
		//ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD border_area INT", true, false);
		//ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD perimeter INT", true, false);
		//ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD geo_efficiency_base FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD veteran BOOL", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD area_visibility_updated BOOL", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD nextTechExpireTime INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD nextTechExpire INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD targetAdvanceID INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD prev_free_migration_time INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD prev_unite_time INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD prev_go_incognito_time INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD game_money INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD game_money_purchased INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD game_money_won INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD total_game_money_purchased INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD prize_money FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD prize_money_history FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD prize_money_history_monthly FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD prev_prize_money_received_time INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD money_spent FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD raid_earnings_history FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD raid_earnings_history_monthly FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD orb_shard_earnings_history FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD orb_shard_earnings_history_monthly FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD medals_history INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD medals_history_monthly INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD level_history INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD xp_history FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD xp_history_monthly FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD tournament_trophies_history FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD tournament_trophies_history_monthly FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD donated_energy_history FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD donated_energy_history_monthly FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD donated_manpower_history FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD donated_manpower_history_monthly FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD quests_completed INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD quests_completed_monthly INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD captures_history INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD captures_history_monthly INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD max_area INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD max_area_monthly INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD rebirth_countdown FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD rebirth_countdown_purchased FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD rebirth_countdown_start INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD flags INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD prev_buy_energy_day INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD buy_energy_day_amount INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD prev_buy_credits_month INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD prev_buy_credits_month_amount INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD prev_receive_credits_month_amount INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD prev_discovery_day INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD prev_message_send_day INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD message_send_count INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD prev_alliance_request_day INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD alliance_request_count INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD rebirth_count INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD reset_advances_count INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD super_nation BOOL", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD energy FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD energy_max FLOAT", true, false);
		//ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD energy_rate FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD manpower FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD manpower_max FLOAT", true, false);
		//ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD manpower_rate FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD manpower_per_attack FLOAT", true, false);
		//ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD stat_tech FLOAT", true, false);
		//ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD stat_bio FLOAT", true, false);
		//ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD stat_psi FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD geo_efficiency_modifier FLOAT", true, false);
		//ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD xp_multiplier FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD hit_points_base FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD hit_points_rate FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD crit_chance FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD salvage_value FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD wall_discount FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD structure_discount FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD splash_damage FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD max_num_alliances INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD max_simultaneous_processes INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD invisibility BOOL", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD insurgency BOOL", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD total_defense BOOL", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD tech_mult FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD bio_mult FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD psi_mult FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD manpower_rate_mult FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD energy_rate_mult FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD manpower_max_mult FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD energy_max_mult FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD hp_per_square_mult FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD hp_restore_mult FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD attack_manpower_mult FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD tech_perm FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD tech_temp FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD tech_object FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD bio_perm FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD bio_temp FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD bio_object FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD psi_perm FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD psi_temp FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD psi_object FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD energy_rate_perm FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD energy_rate_temp FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD energy_rate_object FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD manpower_rate_perm FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD manpower_rate_temp FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD manpower_rate_object FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD xp_multiplier_perm FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD xp_multiplier_temp FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD xp_multiplier_object FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD shared_energy_capacity INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD shared_manpower_capacity INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD shared_energy_fill FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD shared_manpower_fill FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD shared_energy_xp_per_hour INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD shared_manpower_xp_per_hour INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD num_share_builds INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD manpower_burn_rate FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD prev_update_stats_time INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD tournament_start_day INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD tournament_active INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD tournament_rank INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD trophies_available FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD trophies_banked FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD trophies_potential FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD shard_red_fill FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD shard_green_fill FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD shard_blue_fill FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD raid_eligible INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD raidID INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD raid_attacker_medals INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD raid_defender_medals INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD raid_shield_end_time INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD raid_prev_candidates TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD raid_attack_log TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD raid_defense_log TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD homeland_mapID INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD prev_modify_homeland_time INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD homeland_x0 INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD homeland_y0 INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD homeland_x1 INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD homeland_y1 INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD homeland_min_x0 INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD homeland_min_y0 INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD homeland_max_x1 INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD homeland_max_y1 INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD homeland_max_x0 INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD homeland_area INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD homeland_border_area INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD homeland_perimeter INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD homeland_geo_efficiency_base FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD homeland_energy_burn_rate FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD homeland_manpower INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD homeland_prev_buy_manpower_day INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD homeland_buy_manpower_day_amount INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD mainland_x0 INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD mainland_y0 INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD mainland_x1 INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD mainland_y1 INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD mainland_min_x0 INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD mainland_min_y0 INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD mainland_max_x1 INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD mainland_max_y1 INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD mainland_max_x0 INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD mainland_area INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD mainland_border_area INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD mainland_perimeter INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD mainland_geo_efficiency_base FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD mainland_energy_burn_rate FLOAT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD mainland_manpower INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD mainland_prev_buy_manpower_day INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD mainland_buy_manpower_day_amount INT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD users TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD alliances_active TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD alliances_requests_outgoing TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD alliances_requests_incoming TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD unite_requests_outgoing TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD unite_requests_incoming TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD unite_offers_outgoing TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD unite_offers_incoming TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD chat_list TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD reverse_chat_list TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD goals_token TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD goals_winnings TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD goals_monthly_token TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD goals_monthly_winnings TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD map_flags_token TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD map_flags_title TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD objects MEDIUMTEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD builds TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD areas TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD quest_records TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD bonus_credits_subscriptions TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD bonus_rebirth_subscriptions TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD bonus_xp_subscriptions TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD bonus_manpower_subscriptions TEXT", true, false);
		ExecuteUpdate(db, "ALTER TABLE " + db_table_name + " ADD INDEX name (name)", true, false);
	}

	public static void DeleteDBTable()
	{
		// Attempt to delete the DB table
		ExecuteUpdate(db, "DROP TABLE " + db_table_name, true, true);
	}

	public static String GetNationName(int _nationID)
	{
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationID, false);

		if (nationData == null) {
			return "";
		} else {
			return nationData.name;
		}
	}

	public static int GetNationIDByNationName(String _nation_name)
	{
		int playerID = -1;
		String sql = "SELECT ID FROM " + db_table_name + " where name= '" + BaseData.PrepStringForMySQL(_nation_name) + "'";

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

	public void ModifyUserReportValueInt(UserData.ReportVal _reportVal, int _amount)
	{
    // Iterate through each of the nation's users
		UserData cur_user_data;
		for(int cur_user_id : users)
		{
			// Get the current user's data
			cur_user_data = (UserData)DataManager.GetData(Constants.DT_USER, cur_user_id, false);

			// If the current user isn't currently in the game...
			if (cur_user_data.client_thread == null)
			{
				// Modify the current user's report value.
				cur_user_data.ModifyReportValueInt(_reportVal, _amount);
			}
		}
	}

	public void ModifyUserReportValueFloat(UserData.ReportVal _reportVal, float _amount)
	{
    // Iterate through each of the nation's users
		UserData cur_user_data;
		for(int cur_user_id : users)
		{
			// Get the current user's data
			cur_user_data = (UserData)DataManager.GetData(Constants.DT_USER, cur_user_id, false);

			// If the current user isn't currently in the game...
			if (cur_user_data.client_thread == null)
			{
				// Modify the current user's report value.
				cur_user_data.ModifyReportValueFloat(_reportVal, _amount);
			}
		}
	}


	// Make any necessary repairs to the given nation's data
	public void Repair()
	{
		boolean refresh = false;
		boolean modified = false;
		boolean determine_area = false;

		if ((energy < 0) || (Float.isNaN(energy)))
		{
			Output.PrintToScreen("NationData.Repair() found error: Nation " + name + " (" + ID + ") energy: " + energy);
			energy = 0;
			modified = true;
		}

		if ((mainland_footprint.manpower < 0) || (Float.isNaN(mainland_footprint.manpower)))
		{
			Output.PrintToScreen("NationData.Repair() found error: Nation " + name + " (" + ID + ") manpower: " + mainland_footprint.manpower);
			mainland_footprint.manpower = 0;
			modified = true;
		}

		if ((rebirth_count < 0) || (Float.isNaN(rebirth_count)))
		{
			Output.PrintToScreen("NationData.Repair() found error: Nation " + name + " (" + ID + ") rebirth_count: " + rebirth_count);
			rebirth_count = 0;
			modified = true;
		}

		// Mainland footprint (note that geo_efficiency_base is allowed to be negative)
		refresh = refresh || (Repair_TestForNegative("mainland area", mainland_footprint.area));
		refresh = refresh || (Repair_TestForNegative("mainland border_area", mainland_footprint.border_area));
		refresh = refresh || (Repair_TestForNegative("mainland perimeter", mainland_footprint.perimeter));
		refresh = refresh || (Repair_TestForNegative("mainland manpower", mainland_footprint.manpower));
		refresh = refresh || (Repair_TestForNegative("mainland energy_burn_rate", mainland_footprint.energy_burn_rate));

		// Homeland footprint (note that geo_efficiency_base is allowed to be negative)
		refresh = refresh || (Repair_TestForNegative("homeland area", homeland_footprint.area));
		refresh = refresh || (Repair_TestForNegative("homeland border_area", homeland_footprint.border_area));
		refresh = refresh || (Repair_TestForNegative("homeland perimeter", homeland_footprint.perimeter));
		refresh = refresh || (Repair_TestForNegative("homeland manpower", homeland_footprint.manpower));
		refresh = refresh || (Repair_TestForNegative("homeland energy_burn_rate", homeland_footprint.energy_burn_rate));

		refresh = refresh || (Repair_TestForNegative("energy_max", energy_max));
		refresh = refresh || (Repair_TestForNegative("manpower_max", manpower_max));
		refresh = refresh || (Repair_TestForNegative("manpower_per_attack", manpower_per_attack));
		refresh = refresh || (Repair_TestForNegative("geo_efficiency_modifier", geo_efficiency_modifier));
		refresh = refresh || (Repair_TestForNegative("hit_points_base", hit_points_base));
		refresh = refresh || (Repair_TestForNegative("hit_points_rate", hit_points_rate));
		refresh = refresh || (Repair_TestForNegative("crit_chance", crit_chance));
		refresh = refresh || (Repair_TestForNegative("salvage_value", salvage_value));
		refresh = refresh || (Repair_TestForNegative("wall_discount", wall_discount));
		refresh = refresh || (Repair_TestForNegative("structure_discount", structure_discount));
		refresh = refresh || (Repair_TestForNegative("splash_damage", splash_damage));
		refresh = refresh || (Repair_TestForNegative("tech_mult", tech_mult));
		refresh = refresh || (Repair_TestForNegative("bio_mult", bio_mult));
		refresh = refresh || (Repair_TestForNegative("psi_mult", psi_mult));
		refresh = refresh || (Repair_TestForNegative("manpower_rate_mult", manpower_rate_mult));
		refresh = refresh || (Repair_TestForNegative("energy_rate_mult", energy_rate_mult));
		refresh = refresh || (Repair_TestForNegative("manpower_max_mult", manpower_max_mult));
		refresh = refresh || (Repair_TestForNegative("energy_max_mult", energy_max_mult));
		refresh = refresh || (Repair_TestForNegative("hp_per_square_mult", hp_per_square_mult));
		refresh = refresh || (Repair_TestForNegative("hp_restore_mult", hp_restore_mult));
		refresh = refresh || (Repair_TestForNegative("attack_manpower_mult", attack_manpower_mult));
		refresh = refresh || (Repair_TestForNegative("tech_perm", tech_perm));
		refresh = refresh || (Repair_TestForNegative("tech_temp", tech_temp));
		refresh = refresh || (Repair_TestForNegative("tech_object", tech_object));
		refresh = refresh || (Repair_TestForNegative("bio_perm", bio_perm));
		refresh = refresh || (Repair_TestForNegative("bio_temp", bio_temp));
		refresh = refresh || (Repair_TestForNegative("bio_object", bio_object));
		refresh = refresh || (Repair_TestForNegative("psi_perm", psi_perm));
		refresh = refresh || (Repair_TestForNegative("psi_temp", psi_temp));
		refresh = refresh || (Repair_TestForNegative("psi_object", psi_object));
		refresh = refresh || (Repair_TestForNegative("energy_rate_perm", energy_rate_perm));
		refresh = refresh || (Repair_TestForNegative("energy_rate_temp", energy_rate_temp));
		refresh = refresh || (Repair_TestForNegative("energy_rate_object", energy_rate_object));
		refresh = refresh || (Repair_TestForNegative("manpower_rate_perm", manpower_rate_perm));
		refresh = refresh || (Repair_TestForNegative("manpower_rate_temp", manpower_rate_temp));
		refresh = refresh || (Repair_TestForNegative("manpower_rate_object", manpower_rate_object));
		refresh = refresh || (Repair_TestForNegative("xp_multiplier_perm", xp_multiplier_perm));
		refresh = refresh || (Repair_TestForNegative("xp_multiplier_temp", xp_multiplier_temp));
		refresh = refresh || (Repair_TestForNegative("xp_multiplier_object", xp_multiplier_object));
		refresh = refresh || (Repair_TestForNegative("shared_energy_capacity", shared_energy_capacity));
		refresh = refresh || (Repair_TestForNegative("shared_manpower_capacity", shared_manpower_capacity));
		refresh = refresh || (Repair_TestForNegative("shared_energy_fill", shared_energy_fill));
		refresh = refresh || (Repair_TestForNegative("shared_manpower_fill", shared_manpower_fill));
		refresh = refresh || (Repair_TestForNegative("shared_energy_xp_per_hour", shared_energy_xp_per_hour));
		refresh = refresh || (Repair_TestForNegative("shared_manpower_xp_per_hour", shared_manpower_xp_per_hour));

		determine_area = (mainland_footprint.area < 1) || (mainland_footprint.border_area < 1)  || (mainland_footprint.perimeter < 1) || (mainland_footprint.area < mainland_footprint.border_area) || (homeland_footprint.area < 1) || (homeland_footprint.border_area < 1)  || (homeland_footprint.perimeter < 1) || (homeland_footprint.area < homeland_footprint.border_area) || (num_share_builds < 0) || (num_share_builds > Constants.MAX_NUM_SHARE_BUILDS);

		// Sanity check user list
		for (Iterator<Integer> iterator = users.iterator(); iterator.hasNext();)
		{
				Integer cur_userID = iterator.next();

				// Get the data for this user.
				UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, cur_userID, false);

				if ((userData == null) || (userData.nationID != ID))
				{
					// This user either doesn't exist, or doesn't belong to this nation. Remove their ID from the users list.
					iterator.remove();
					modified = true;
				}
		}

		// Sanity check quest records
		QuestRecord quest_record;
		for (Integer key : quest_records.keySet())
		{
			quest_record = quest_records.get(key);
			if (quest_record.cur_amount < 0)
			{
				Output.PrintToScreen("NationData.Repair() found error: Nation " + name + " (" + ID + ")'s quest " + quest_record.ID + "'s cur_amount: " + quest_record.cur_amount);
				quest_record.cur_amount = 0;
				modified = true;
			}
		}

		// Santy check tournament info
		if ((trophies_available < 0) || Float.isNaN(trophies_available)) {trophies_available = 0; modified = true;}
		if ((trophies_banked < 0) || Float.isNaN(trophies_banked)) {trophies_banked = 0; modified = true;}
		if ((trophies_potential < 0) || Float.isNaN(trophies_potential)) {trophies_potential = 0; modified = true;}

		// Make sure level is high enough, given rebirth_count.
		if (level < (1 + GetRebirthLevelBonus()))
		{
			Output.PrintToScreen("Nation " + name + " (" + ID + ")'s level of " + level + " is too low given its rebirth_count of " + rebirth_count + ". Fixing.");
			level = 1 + GetRebirthLevelBonus();
			modified = true;
		}

		// Refresh all of the nation's advances and stats if appropriate
		if (refresh) {
			Admin.RefreshNation(ID);
		}

		// Otherwise, re-determine the nation's area if appropriate
		else if (determine_area)
		{
			Gameplay.RefreshAreaAndEnergyBurnRate(ID);
			modified = true;
		}

		// Mark the nation to be updated to the DB if appropriate.
		if (modified) {
			DataManager.MarkForUpdate(this);
		}
	}

	public boolean Repair_TestForNegative(String _field, float _value)
	{
		if (_value < 0)
		{
			Output.PrintToScreen("NationData.Repair() found error: Nation " + name + " (" + ID + ") " + _field + ": " + _value);
			return true;
		}

		return false;
	}
}
