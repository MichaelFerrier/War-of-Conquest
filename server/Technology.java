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

import java.util.*;
import WOCServer.*;

public class Technology
{
	public static void AddTechnology(int _nationID, int _techID, float _position, boolean _updatePendingObjects, boolean _broadcast, int _delay)
	{
		int expire_time = 0;

		// Get current time
		int cur_time = Constants.GetTime();

		// Get the nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationID, false);
		NationTechData nationTechData = (NationTechData)DataManager.GetData(Constants.DT_NATIONTECH, _nationID, false);

		if ((nationData == null) || (nationTechData == null))
		{
			Output.screenOut.println("AddTechnology called for nation with missing data: " + _nationID);
			return;
		}

		// Get the technology's data
		TechData techData = TechData.GetTechData(_techID);

		if (techData == null)
		{
			Output.screenOut.println("AddTechnology called for non-existent tech " + _techID + ", for nation " + _nationID);
			return;
		}

		// Get, increment, and store the new count for this tech
		int count = nationTechData.GetTechCount(_techID) + 1;
		nationTechData.SetTechCount(_techID, count);

		// If the tech being added is temporary, record its expire time.
		if (techData.duration_type == TechData.DURATION_TEMPORARY)
		{
			// Determine this technology's expiration time
			expire_time = Constants.GetTime() + techData.duration_time;

			// Record this tech's expire time.
			nationTechData.tech_temp_expire_time.put(_techID, expire_time);

			if ((nationData.nextTechExpireTime == -1) || (expire_time < nationData.nextTechExpireTime))
			{
				nationData.nextTechExpireTime = expire_time;
				nationData.nextTechExpire = _techID;
			}
		}

		// Apply this tech's bonuses to the nation.
		if (techData.bonus_type_1 != TechData.BONUS_UNDEF) ApplyTechBonus(nationData, techData.bonus_type_1, techData.bonus_val_1, techData.bonus_val_max_1, techData.duration_type, _position, false);
		if (techData.bonus_type_2 != TechData.BONUS_UNDEF) ApplyTechBonus(nationData, techData.bonus_type_2, techData.bonus_val_2, techData.bonus_val_max_2, techData.duration_type, _position, false);
		if (techData.bonus_type_3 != TechData.BONUS_UNDEF) ApplyTechBonus(nationData, techData.bonus_type_3, techData.bonus_val_3, techData.bonus_val_max_3, techData.duration_type, _position, false);

		// Add any newly available structure, and remove any obsolete structure.
		nationTechData.UpdateBuildsForAdvance(_techID);

		// If appropriate, broadcast add technology event to all logged in members of this nation.
		if (_broadcast) {
			OutputEvents.BroadcastAddTechnologyEvent(_nationID, _techID, expire_time, _delay);
		}

		// Broadcast the updated stats to all logged in members of this nation.
		OutputEvents.BroadcastStatsEvent(_nationID, _delay);

		if ((count == 1) && _updatePendingObjects)
		{
			// Update the nation's list of pending objects, adding to the nation the techs of any that are no longer blocked.
			UpdatePendingObjects(nationData, nationTechData);
		}

//		// Post report to nation
//		Comm.SendReport(_nationID, ClientString.Get("svr_report_gained_tech", "tech_name", "{Technologies/tech_" + _techID + "_name}")); // "We have gained " + techData.name + "."

		// Mark the nation's data to be updated
		DataManager.MarkForUpdate(nationData);
		DataManager.MarkForUpdate(nationTechData);
	}

	public static void RemoveTechnology(int _nationID, int _techID, float _position)
	{
		// Get the nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationID, false);
		NationTechData nationTechData = (NationTechData)DataManager.GetData(Constants.DT_NATIONTECH, _nationID, false);

		// Make sure the nation's data was found
		if ((nationData == null) || (nationTechData == null))
		{
			Output.PrintToScreen("** ERROR: RemoveTechnology(_nationID:" + _nationID + ", _techID:" + _techID + ") unable to get nation data or nation tech data.");
			return;
		}

		// Get the technology's data
		TechData techData = TechData.GetTechData(_techID);

		// Make sure the tech's data was found
		if (techData == null)
		{
			Output.PrintToScreen("** ERROR: RemoveTechnology(_nationID:" + _nationID + ", _techID:" + _techID + ") unable to get tech data.");
			return;
		}

		// Get, decrement, and store the new count for this tech
		int count = nationTechData.GetTechCount(_techID) - 1;
		nationTechData.SetTechCount(_techID, count);

		// If the tech being removed is temporary, remove its expire time from tech_temp_expire_time table.
		if (techData.duration_type == TechData.DURATION_TEMPORARY)
		{
			// Remove this tech's expire time.
			nationTechData.tech_temp_expire_time.remove(_techID);

			if (nationData.nextTechExpire == _techID)
			{
				// Determine the nation's nextTechExpireTime and nextTechExpire
				DetermineNextTechExpire(_nationID, nationData, nationTechData);
			}
		}

		// Remove this tech's bonuses from the nation.
		if (techData.bonus_type_1 != TechData.BONUS_UNDEF) ApplyTechBonus(nationData, techData.bonus_type_1, techData.bonus_val_1, techData.bonus_val_max_1, techData.duration_type, _position, true);
		if (techData.bonus_type_2 != TechData.BONUS_UNDEF) ApplyTechBonus(nationData, techData.bonus_type_2, techData.bonus_val_2, techData.bonus_val_max_2, techData.duration_type, _position, true);
		if (techData.bonus_type_3 != TechData.BONUS_UNDEF) ApplyTechBonus(nationData, techData.bonus_type_3, techData.bonus_val_3, techData.bonus_val_max_3, techData.duration_type, _position, true);

		// Broadcast the remove technology event, and the updated stats, to all logged in members of this nation.
		OutputEvents.BroadcastRemoveTechnologyEvent(_nationID, _techID);
		OutputEvents.BroadcastStatsEvent(_nationID, 0);

		//// Post report to nation
		//Comm.SendReport(_nationID, ClientString.Get("svr_report_lost_tech", "tech_name", "{Technologies/tech_" + _techID + "_name}"), 0); // "We have lost " + techData.name + "."

		// Mark the nation's data to be updated
		DataManager.MarkForUpdate(nationData);
		DataManager.MarkForUpdate(nationTechData);
	}

	public static void ApplyTechBonus(NationData _nationData, int _bonus_type, int _bonus_val, int _bonus_max_val, int _duration_type, float _position, boolean _remove)
	{
		float sign = _remove ? -1 : 1;

		// Determine bonus value within range based on given position, if appropriate.
		if ((_position > 0) && (_bonus_max_val > 0)) {
			_bonus_val += ((_bonus_max_val - _bonus_val) * _position);
		}

		//Output.PrintToScreen("ApplyTechBonus() applying value of " + (_bonus_val * sign) + " to nation " + _nationData.name + "'s stat " + _bonus_type + ", duration type " + _duration_type);

switch (_bonus_type)
		{
			case TechData.BONUS_TECH:
				switch (_duration_type)
				{
					case TechData.DURATION_PERMANENT: _nationData.tech_perm = ApplyTechBonus_CheckError(_nationData, _bonus_type, _nationData.tech_perm, _bonus_val, _remove); break;
					case TechData.DURATION_TEMPORARY: _nationData.tech_temp = ApplyTechBonus_CheckError(_nationData, _bonus_type, _nationData.tech_temp, _bonus_val, _remove); break;
					case TechData.DURATION_OBJECT: _nationData.tech_object = ApplyTechBonus_CheckError(_nationData, _bonus_type, _nationData.tech_object, _bonus_val, _remove); break;
				}
				break;
			case TechData.BONUS_BIO:
				switch (_duration_type)
				{
					case TechData.DURATION_PERMANENT: _nationData.bio_perm = ApplyTechBonus_CheckError(_nationData, _bonus_type, _nationData.bio_perm, _bonus_val, _remove); break;
					case TechData.DURATION_TEMPORARY: _nationData.bio_temp = ApplyTechBonus_CheckError(_nationData, _bonus_type, _nationData.bio_temp, _bonus_val, _remove); break;
					case TechData.DURATION_OBJECT: _nationData.bio_object = ApplyTechBonus_CheckError(_nationData, _bonus_type, _nationData.bio_object, _bonus_val, _remove); break;
				}
				break;
			case TechData.BONUS_PSI:
				switch (_duration_type)
				{
					case TechData.DURATION_PERMANENT: _nationData.psi_perm = ApplyTechBonus_CheckError(_nationData, _bonus_type, _nationData.psi_perm, _bonus_val, _remove); break;
					case TechData.DURATION_TEMPORARY: _nationData.psi_temp = ApplyTechBonus_CheckError(_nationData, _bonus_type, _nationData.psi_temp, _bonus_val, _remove); break;
					case TechData.DURATION_OBJECT: _nationData.psi_object = ApplyTechBonus_CheckError(_nationData, _bonus_type, _nationData.psi_object, _bonus_val, _remove); break;
				}
				break;
			case TechData.BONUS_MANPOWER_RATE:
				switch (_duration_type)
				{
					case TechData.DURATION_PERMANENT: _nationData.manpower_rate_perm = ApplyTechBonus_CheckError(_nationData, _bonus_type, _nationData.manpower_rate_perm, _bonus_val * Constants.manpower_gen_multiplier, _remove); break;
					case TechData.DURATION_TEMPORARY: _nationData.manpower_rate_temp = ApplyTechBonus_CheckError(_nationData, _bonus_type, _nationData.manpower_rate_temp, _bonus_val * Constants.manpower_gen_multiplier, _remove); break;
					case TechData.DURATION_OBJECT: _nationData.manpower_rate_object = ApplyTechBonus_CheckError(_nationData, _bonus_type, _nationData.manpower_rate_object, _bonus_val * Constants.manpower_gen_multiplier, _remove); break;
				}
				break;
			case TechData.BONUS_ENERGY_RATE:
				switch (_duration_type)
				{
					case TechData.DURATION_PERMANENT: _nationData.energy_rate_perm = ApplyTechBonus_CheckError(_nationData, _bonus_type, _nationData.energy_rate_perm, _bonus_val, _remove); break;
					case TechData.DURATION_TEMPORARY: _nationData.energy_rate_temp = ApplyTechBonus_CheckError(_nationData, _bonus_type, _nationData.energy_rate_temp, _bonus_val, _remove); break;
					case TechData.DURATION_OBJECT: _nationData.energy_rate_object = ApplyTechBonus_CheckError(_nationData, _bonus_type, _nationData.energy_rate_object, _bonus_val, _remove); break;
				}
				OutputEvents.BroadcastUpdateBarsEvent(_nationData.ID, 0, (int)(_bonus_val * sign), 0, 0, 0, 0);
				break;
			case TechData.BONUS_XP_MULTIPLIER:
				switch (_duration_type)
				{
					case TechData.DURATION_PERMANENT: _nationData.xp_multiplier_perm = ApplyTechBonus_CheckError(_nationData, _bonus_type, _nationData.xp_multiplier_perm, _bonus_val / 100.0f, _remove); break;
					case TechData.DURATION_TEMPORARY: _nationData.xp_multiplier_temp = ApplyTechBonus_CheckError(_nationData, _bonus_type, _nationData.xp_multiplier_temp, _bonus_val / 100.0f, _remove); break;
					case TechData.DURATION_OBJECT: _nationData.xp_multiplier_object = ApplyTechBonus_CheckError(_nationData, _bonus_type, _nationData.xp_multiplier_object, _bonus_val / 100.0f, _remove); break;
				}
				break;
			case TechData.BONUS_MANPOWER_MAX: _nationData.manpower_max = ApplyTechBonus_CheckError(_nationData, _bonus_type, _nationData.manpower_max, _bonus_val * Constants.manpower_gen_multiplier, _remove); break;
			case TechData.BONUS_ENERGY_MAX: _nationData.energy_max = ApplyTechBonus_CheckError(_nationData, _bonus_type, _nationData.energy_max, _bonus_val, _remove); break;
			case TechData.BONUS_GEO_EFFICIENCY: _nationData.geo_efficiency_modifier += ((_bonus_val * sign) / 100f); break;
			case TechData.BONUS_HP_PER_SQUARE: _nationData.hit_points_base = ApplyTechBonus_CheckError(_nationData, _bonus_type, _nationData.hit_points_base, _bonus_val, _remove); break;
			case TechData.BONUS_HP_RESTORE: _nationData.hit_points_rate = ApplyTechBonus_CheckError(_nationData, _bonus_type, _nationData.hit_points_rate, _bonus_val, _remove); break;
			case TechData.BONUS_ATTACK_MANPOWER: _nationData.manpower_per_attack = ApplyTechBonus_CheckError(_nationData, _bonus_type, _nationData.manpower_per_attack, _bonus_val, _remove); break;
			case TechData.BONUS_SIMULTANEOUS_ACTIONS: _nationData.max_simultaneous_processes = (int)ApplyTechBonus_CheckError(_nationData, _bonus_type, _nationData.max_simultaneous_processes, _bonus_val, _remove); break;
			case TechData.BONUS_CRIT_CHANCE: _nationData.crit_chance = ApplyTechBonus_CheckError(_nationData, _bonus_type, _nationData.crit_chance, _bonus_val / 100.0f, _remove); break;
			case TechData.BONUS_SALVAGE_VALUE: _nationData.salvage_value = ApplyTechBonus_CheckError(_nationData, _bonus_type, _nationData.salvage_value, _bonus_val / 100.0f, _remove); break;
			case TechData.BONUS_WALL_DISCOUNT: _nationData.wall_discount = ApplyTechBonus_CheckError(_nationData, _bonus_type, _nationData.wall_discount, _bonus_val / 100.0f, _remove); break;
			case TechData.BONUS_STRUCTURE_DISCOUNT: _nationData.structure_discount = ApplyTechBonus_CheckError(_nationData, _bonus_type, _nationData.structure_discount, _bonus_val / 100.0f, _remove); break;
			case TechData.BONUS_SPLASH_DAMAGE: _nationData.splash_damage = ApplyTechBonus_CheckError(_nationData, _bonus_type, _nationData.splash_damage, _bonus_val / 100.0f, _remove); break;
			case TechData.BONUS_MAX_ALLIANCES: _nationData.max_num_alliances = (int)ApplyTechBonus_CheckError(_nationData, _bonus_type, _nationData.max_num_alliances, _bonus_val, _remove); break;
			case TechData.BONUS_INVISIBILITY: _nationData.invisibility = ((_bonus_val > 0) == (!_remove)); break;
			case TechData.BONUS_TECH_MULT: _nationData.tech_mult += ((_bonus_val * sign) / 100f); break;
			case TechData.BONUS_BIO_MULT: _nationData.bio_mult += ((_bonus_val * sign) / 100f); break;
			case TechData.BONUS_PSI_MULT: _nationData.psi_mult += ((_bonus_val * sign) / 100f); break;
			case TechData.BONUS_MANPOWER_RATE_MULT: _nationData.manpower_rate_mult += ((_bonus_val * sign) / 100f); break;
			case TechData.BONUS_ENERGY_RATE_MULT: _nationData.energy_rate_mult += ((_bonus_val * sign) / 100f); break;
			case TechData.BONUS_MANPOWER_MAX_MULT: _nationData.manpower_max_mult += ((_bonus_val * sign) / 100f); break;
			case TechData.BONUS_ENERGY_MAX_MULT: _nationData.energy_max_mult += ((_bonus_val * sign) / 100f); break;
			case TechData.BONUS_HP_PER_SQUARE_MULT: _nationData.hp_per_square_mult += ((_bonus_val * sign) / 100f); break;
			case TechData.BONUS_HP_RESTORE_MULT: _nationData.hp_restore_mult += ((_bonus_val * sign) / 100f); break;
			case TechData.BONUS_ATTACK_MANPOWER_MULT: _nationData.attack_manpower_mult += ((_bonus_val * sign) / 100f); break;
			case TechData.BONUS_CREDITS: Money.AddGameMoney(_nationData, (int)_bonus_val, Money.Source.FREE); OutputEvents.BroadcastUpdateBarsEvent(_nationData.ID, 0, 0, 0, 0, (int)_bonus_val, 0); break;
			case TechData.BONUS_INSURGENCY: _nationData.insurgency = ((_bonus_val > 0) == (!_remove)); break;
			case TechData.BONUS_TOTAL_DEFENSE: _nationData.total_defense = ((_bonus_val > 0) == (!_remove)); break;
		}
		/*
		switch (_bonus_type)
		{
			case TechData.BONUS_TECH:
				switch (_duration_type)
				{
					case TechData.DURATION_PERMANENT: _nationData.tech_perm += (_bonus_val * sign); if (_nationData.tech_perm < 0) ApplyTechBonus_Error(_nationData, _bonus_type, _bonus_val, _remove); break;
					case TechData.DURATION_TEMPORARY: _nationData.tech_temp += (_bonus_val * sign); if (_nationData.tech_temp < 0) ApplyTechBonus_Error(_nationData, _bonus_type, _bonus_val, _remove); break;
					case TechData.DURATION_OBJECT: _nationData.tech_object += (_bonus_val * sign); if (_nationData.tech_object < 0) ApplyTechBonus_Error(_nationData, _bonus_type, _bonus_val, _remove); break;
				}
				break;
			case TechData.BONUS_BIO:
				switch (_duration_type)
				{
					case TechData.DURATION_PERMANENT: _nationData.bio_perm += (_bonus_val * sign); if (_nationData.bio_perm < 0) ApplyTechBonus_Error(_nationData, _bonus_type, _bonus_val, _remove); break;
					case TechData.DURATION_TEMPORARY: _nationData.bio_temp += (_bonus_val * sign); if (_nationData.bio_temp < 0) ApplyTechBonus_Error(_nationData, _bonus_type, _bonus_val, _remove); break;
					case TechData.DURATION_OBJECT: _nationData.bio_object += (_bonus_val * sign); if (_nationData.bio_object < 0) ApplyTechBonus_Error(_nationData, _bonus_type, _bonus_val, _remove); break;
				}
				break;
			case TechData.BONUS_PSI:
				switch (_duration_type)
				{
					case TechData.DURATION_PERMANENT: _nationData.psi_perm += (_bonus_val * sign); if (_nationData.psi_perm < 0) ApplyTechBonus_Error(_nationData, _bonus_type, _bonus_val, _remove); break;
					case TechData.DURATION_TEMPORARY: _nationData.psi_temp += (_bonus_val * sign); if (_nationData.psi_temp < 0) ApplyTechBonus_Error(_nationData, _bonus_type, _bonus_val, _remove); break;
					case TechData.DURATION_OBJECT: _nationData.psi_object += (_bonus_val * sign); if (_nationData.psi_object < 0) ApplyTechBonus_Error(_nationData, _bonus_type, _bonus_val, _remove); break;
				}
				break;
			case TechData.BONUS_MANPOWER_RATE:
				switch (_duration_type)
				{
					case TechData.DURATION_PERMANENT: _nationData.manpower_rate_perm += (_bonus_val * Constants.manpower_gen_multiplier * sign); if (_nationData.manpower_rate_perm < 0) ApplyTechBonus_Error(_nationData, _bonus_type, _bonus_val, _remove); break;
					case TechData.DURATION_TEMPORARY: _nationData.manpower_rate_temp += (_bonus_val * Constants.manpower_gen_multiplier * sign); if (_nationData.manpower_rate_temp < 0) ApplyTechBonus_Error(_nationData, _bonus_type, _bonus_val, _remove); break;
					case TechData.DURATION_OBJECT: _nationData.manpower_rate_object += (_bonus_val * Constants.manpower_gen_multiplier * sign); if (_nationData.manpower_rate_object < 0) ApplyTechBonus_Error(_nationData, _bonus_type, _bonus_val, _remove); break;
				}
				break;
			case TechData.BONUS_ENERGY_RATE:
				switch (_duration_type)
				{
					case TechData.DURATION_PERMANENT: _nationData.energy_rate_perm += (_bonus_val * sign); if (_nationData.energy_rate_perm < 0) ApplyTechBonus_Error(_nationData, _bonus_type, _bonus_val, _remove); break;
					case TechData.DURATION_TEMPORARY: _nationData.energy_rate_temp += (_bonus_val * sign); if (_nationData.energy_rate_temp < 0) ApplyTechBonus_Error(_nationData, _bonus_type, _bonus_val, _remove); break;
					case TechData.DURATION_OBJECT: _nationData.energy_rate_object += (_bonus_val * sign); if (_nationData.energy_rate_object < 0) ApplyTechBonus_Error(_nationData, _bonus_type, _bonus_val, _remove); break;
				}
				OutputEvents.BroadcastUpdateBarsEvent(_nationData.ID, 0, (int)(_bonus_val * sign), 0, 0, 0, 0);
				break;
			case TechData.BONUS_XP_MULTIPLIER:
				switch (_duration_type)
				{
					case TechData.DURATION_PERMANENT: _nationData.xp_multiplier_perm += ((_bonus_val * sign) / 100f); if (_nationData.xp_multiplier_perm < 0) ApplyTechBonus_Error(_nationData, _bonus_type, _bonus_val, _remove); break;
					case TechData.DURATION_TEMPORARY: _nationData.xp_multiplier_temp += ((_bonus_val * sign) / 100f); if (_nationData.xp_multiplier_temp < 0) ApplyTechBonus_Error(_nationData, _bonus_type, _bonus_val, _remove); break;
					case TechData.DURATION_OBJECT: _nationData.xp_multiplier_object += ((_bonus_val * sign) / 100f); if (_nationData.xp_multiplier_object < 0) ApplyTechBonus_Error(_nationData, _bonus_type, _bonus_val, _remove); break;
				}
				break;
			case TechData.BONUS_MANPOWER_MAX: _nationData.manpower_max += (_bonus_val * Constants.manpower_gen_multiplier * sign); if (_nationData.manpower_max < 0) ApplyTechBonus_Error(_nationData, _bonus_type, _bonus_val, _remove); break;
			case TechData.BONUS_ENERGY_MAX: _nationData.energy_max += (_bonus_val * sign); if (_nationData.energy_max < 0) ApplyTechBonus_Error(_nationData, _bonus_type, _bonus_val, _remove); break;
			case TechData.BONUS_GEO_EFFICIENCY: _nationData.geo_efficiency_modifier += ((_bonus_val * sign) / 100f); break;
			case TechData.BONUS_HP_PER_SQUARE: _nationData.hit_points_base += (_bonus_val * sign); if (_nationData.hit_points_base < 0) ApplyTechBonus_Error(_nationData, _bonus_type, _bonus_val, _remove); break;
			case TechData.BONUS_HP_RESTORE: _nationData.hit_points_rate += (_bonus_val * sign); if (_nationData.hit_points_rate < 0) ApplyTechBonus_Error(_nationData, _bonus_type, _bonus_val, _remove); break;
			case TechData.BONUS_ATTACK_MANPOWER: _nationData.manpower_per_attack += (_bonus_val * sign); if (_nationData.manpower_per_attack < 0) ApplyTechBonus_Error(_nationData, _bonus_type, _bonus_val, _remove); break;
			case TechData.BONUS_SIMULTANEOUS_ACTIONS: _nationData.max_simultaneous_processes += (_bonus_val * sign); if (_nationData.max_simultaneous_processes < 0) ApplyTechBonus_Error(_nationData, _bonus_type, _bonus_val, _remove); break;
			case TechData.BONUS_CRIT_CHANCE: _nationData.crit_chance += ((_bonus_val * sign) / 100f); if (_nationData.crit_chance < 0) ApplyTechBonus_Error(_nationData, _bonus_type, _bonus_val, _remove); break;
			case TechData.BONUS_SALVAGE_VALUE: _nationData.salvage_value += ((_bonus_val * sign) / 100f); if (_nationData.salvage_value < 0) ApplyTechBonus_Error(_nationData, _bonus_type, _bonus_val, _remove); break;
			case TechData.BONUS_WALL_DISCOUNT: _nationData.wall_discount += ((_bonus_val * sign) / 100f); if (_nationData.wall_discount < 0) ApplyTechBonus_Error(_nationData, _bonus_type, _bonus_val, _remove); break;
			case TechData.BONUS_STRUCTURE_DISCOUNT: _nationData.structure_discount += ((_bonus_val * sign) / 100f); if (_nationData.structure_discount < 0) ApplyTechBonus_Error(_nationData, _bonus_type, _bonus_val, _remove); break;
			case TechData.BONUS_SPLASH_DAMAGE: _nationData.splash_damage += ((_bonus_val * sign) / 100f); if (Math.abs(_nationData.splash_damage - (float)(Math.round(_nationData.splash_damage))) < 0.001f) _nationData.splash_damage = (float)(Math.round(_nationData.splash_damage)); if (_nationData.splash_damage < 0) ApplyTechBonus_Error(_nationData, _bonus_type, _bonus_val, _remove); break;
			case TechData.BONUS_MAX_ALLIANCES: _nationData.max_num_alliances += (_bonus_val * sign); if (_nationData.max_num_alliances < 0) ApplyTechBonus_Error(_nationData, _bonus_type, _bonus_val, _remove); break;
			case TechData.BONUS_INVISIBILITY: _nationData.invisibility = ((_bonus_val > 0) == (!_remove)); break;
			case TechData.BONUS_TECH_MULT: _nationData.tech_mult += ((_bonus_val * sign) / 100f); break;
			case TechData.BONUS_BIO_MULT: _nationData.bio_mult += ((_bonus_val * sign) / 100f); break;
			case TechData.BONUS_PSI_MULT: _nationData.psi_mult += ((_bonus_val * sign) / 100f); break;
			case TechData.BONUS_MANPOWER_RATE_MULT: _nationData.manpower_rate_mult += ((_bonus_val * sign) / 100f); break;
			case TechData.BONUS_ENERGY_RATE_MULT: _nationData.energy_rate_mult += ((_bonus_val * sign) / 100f); break;
			case TechData.BONUS_MANPOWER_MAX_MULT: _nationData.manpower_max_mult += ((_bonus_val * sign) / 100f); break;
			case TechData.BONUS_ENERGY_MAX_MULT: _nationData.energy_max_mult += ((_bonus_val * sign) / 100f); break;
			case TechData.BONUS_HP_PER_SQUARE_MULT: _nationData.hp_per_square_mult += ((_bonus_val * sign) / 100f); break;
			case TechData.BONUS_HP_RESTORE_MULT: _nationData.hp_restore_mult += ((_bonus_val * sign) / 100f); break;
			case TechData.BONUS_ATTACK_MANPOWER_MULT: _nationData.attack_manpower_mult += ((_bonus_val * sign) / 100f); break;
			case TechData.BONUS_CREDITS: Money.AddGameMoney(_nationData, (int)_bonus_val, Money.Source.FREE); OutputEvents.BroadcastUpdateBarsEvent(_nationData.ID, 0, 0, 0, 0, (int)_bonus_val, 0); if (_nationData.game_money < 0) ApplyTechBonus_Error(_nationData, _bonus_type, _bonus_val, _remove); break;
			case TechData.BONUS_INSURGENCY: _nationData.insurgency = ((_bonus_val > 0) == (!_remove)); break;
			case TechData.BONUS_TOTAL_DEFENSE: _nationData.total_defense = ((_bonus_val > 0) == (!_remove)); break;
		}
		*/

		// If the nation's invisibility stat has changed...
		if (_bonus_type == TechData.BONUS_INVISIBILITY)
		{
			// Update the invisible_time for each of the nation's build objects.
			Gameplay.UpdateInvisibilityOfObjects(_nationData, Constants.MAINLAND_MAP_ID);
			if (_nationData.homeland_mapID > 0) {
				Gameplay.UpdateInvisibilityOfObjects(_nationData, _nationData.homeland_mapID);
			}

			// Make sure the nation is not incognito.
			if (!_nationData.invisibility) {
				_nationData.SetFlags(_nationData.flags & ~Constants.NF_INCOGNITO);
			}
		}

		// If the nation's wall or structure discount to energy burn rate has changed, re-determine the nation's energy burn rate.
		if ((_bonus_type == TechData.BONUS_WALL_DISCOUNT) || (_bonus_type == TechData.BONUS_STRUCTURE_DISCOUNT)) {
			//Output.PrintToScreen("ApplyTechBonus() for nation " + _nationData.name + " (" + _nationData.ID + ") _bonus_type: " + _bonus_type + ", _bonus_val: " + _bonus_val + ", _remove: " + _remove + ", wall discount: " + _nationData.wall_discount + ", structure discount: " + _nationData.structure_discount); // TESTING
			Gameplay.RefreshAreaAndEnergyBurnRate(_nationData.ID);
		}

		// If the max manpower amount is changing...
		if ((_bonus_type == TechData.BONUS_MANPOWER_MAX) || (_bonus_type == TechData.BONUS_MANPOWER_MAX_MULT))
		{
			// If a reset occured recently, restore the pre-reset portion of the additional max.
			if ((_bonus_type == TechData.BONUS_MANPOWER_MAX) && (_bonus_val > 0) && ((Constants.GetTime() - _nationData.prev_reset_time) <= Constants.POST_RESET_REPLACEMENT_WINDOW)) {
				_nationData.mainland_footprint.manpower += _bonus_val * _nationData.prev_reset_manpower_fraction; // Add pre-reset portion of additional max to current manpower level.
			}

			// Make sure manpower amount is still within the new range.
			_nationData.mainland_footprint.manpower = Math.min(_nationData.mainland_footprint.manpower, _nationData.GetMainlandManpowerMax());
		}

		// If the max energy amount is changing...
		if ((_bonus_type == TechData.BONUS_ENERGY_MAX) || (_bonus_type == TechData.BONUS_ENERGY_MAX_MULT))
		{
			// If a reset occured recently, restore the pre-reset portion of the additional max.
			if ((_bonus_type == TechData.BONUS_ENERGY_MAX) && (_bonus_val > 0) && ((Constants.GetTime() - _nationData.prev_reset_time) <= Constants.POST_RESET_REPLACEMENT_WINDOW)) {
				_nationData.energy += _bonus_val * _nationData.prev_reset_energy_fraction; // Add pre-reset portion of additional max to current energy level.
			}

			// Make sure energy amount is still within the new range.
			_nationData.energy = Math.min(_nationData.energy, _nationData.GetFinalEnergyMax());
		}
	}
/*
	public static void ApplyTechBonus_Error(NationData _nationData, int _bonus_type, float _bonus_val, boolean _remove)
	{
		Output.PrintToScreen("ApplyTechBonus() ERROR: Nation " + _nationData.name + " (" + _nationData.ID + ")'s bonus " + _bonus_type + " has gone negative. Given bonus value: " + _bonus_val + ", remove: "+ _remove);
		Output.PrintStackTrace();
		_nationData.Repair();
	}
*/
	public static float ApplyTechBonus_CheckError(NationData _nationData, int _bonus_type, float _original_val, float _delta_val, boolean _remove)
	{
		// Determine the bonus' new value.
		float new_value = _original_val + (_delta_val * (_remove ? -1 : 1));

		// If the new value is very close to an integer, it may be off due to floating point math inaccuracy. Round to nearest integer.
		if (Math.abs((float)Math.round(new_value) - new_value) < 0.001) {
			new_value = (float)Math.round(new_value);
		}

		if (new_value < 0)
		{
			Output.PrintToScreen("ApplyTechBonus() ERROR: Nation " + _nationData.name + " (" + _nationData.ID + ")'s bonus " + _bonus_type + " has gone negative. Original bonus value: " + _original_val + ", delta value: " + _delta_val + ", remove: " + _remove + ", new value: " + new_value);
			Output.PrintStackTrace();
			_nationData.Repair();
		}

		return new_value;
	}

	public static void DetermineNextTechExpire(int _nationID, NationData _nationData, NationTechData _nationTechData)
	{
		_nationData.nextTechExpire = -1;
		_nationData.nextTechExpireTime = -1;

		// Determine the nation's nextTechExpireTime and nextTechExpire
		for (Map.Entry<Integer, Integer> entry : _nationTechData.tech_temp_expire_time.entrySet())
		{
			if ((_nationData.nextTechExpireTime == -1) || (_nationData.nextTechExpireTime > entry.getValue()))
			{
				_nationData.nextTechExpire = entry.getKey();
				_nationData.nextTechExpireTime = entry.getValue();
			}
		}

		// Mark the nation's data to be updated
		DataManager.MarkForUpdate(_nationData);
	}

	public static boolean RequirementsMet(int _techID, NationData _nationData, NationTechData _nationTechData)
	{
		// Get the technology's data
		TechData techData = TechData.GetTechData(_techID);

		// Return false if this is not a valid technology.
		if (techData == null) {
			return false;
		}

		// Check whether the nation meets the required level.
		if (_nationData.level < techData.prerequisite_level) {
			return false;
		}

		// Check whether the nation has prerequisite tech 1.
		if ((techData.prerequisite_tech_1 != -1) && (_nationTechData.GetTechCount(techData.prerequisite_tech_1) == 0)) {
			return false;
		}

		// Check whether the nation has prerequisite tech 2.
		if ((techData.prerequisite_tech_2 != -1) && (_nationTechData.GetTechCount(techData.prerequisite_tech_2) == 0)) {
			return false;
		}

		// All requirements are met.
		return true;
	}

	public static void UpdatePendingObjects(NationData _nationData, NationTechData _nationTechData)
	{
		int coords_token, objectID;
		ObjectData object_data;
		int [] coord_array = new int[2];

		// Get the mainland map
		LandMap land_map = DataManager.GetLandMap(Constants.MAINLAND_MAP_ID, false);

		// Iterate through each of the nation's pending objects...
		for (int i = 0; i < _nationTechData.pending_object_coords.size();)
		{
			// Determine the coords of the current pending object's block.
			coords_token = _nationTechData.pending_object_coords.get(i);
			Constants.UntokenizeCoordinates(coords_token, coord_array);

			// Get the current block's object ID.
			objectID = land_map.GetBlockObjectID(coord_array[0], coord_array[1]);

			// If the current block has a resource object...
			if ((objectID >= ObjectData.RESOURCE_OBJECT_BASE_ID) && (objectID < ObjectData.ORB_BASE_ID))
			{
				// Get the current block's resource object's data.
				object_data = ObjectData.GetObjectData(objectID);

				// If the current block's resource technology's requirements are met by this nation...
				if ((object_data != null) && (object_data.techID != -1) && (RequirementsMet(object_data.techID, _nationData, _nationTechData)))
				{
					// Add this resource object's tech to the nation.
					Technology.AddTechnology(_nationTechData.ID, object_data.techID, object_data.GetPositionInRange(coord_array[0], coord_array[1], land_map), false, true, 0);

					// Remove this object's coordinates from the pending_object_coords array.
					_nationTechData.pending_object_coords.remove(i);

					// Continue so that the array index will not be incremented, due to the current element having been removed.
					continue;
				}
			}

			// Increment the array index to the next pending object.
			i++;
		}
	}

	public static void Research(StringBuffer _output_buffer, int _userID, int _techID)
	{
		// Get the user's data
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		if (userData == null)
		{
			Output.PrintToScreen("Research() called for invalid _userID: " + _userID);
			return;
		}

		// If the user's rank disallows this action, return.
		if (userData.rank > Constants.RANK_CAPTAIN) {
			return;
		}

		// Get the user's nation ID
		int nationID = userData.nationID;

		// Get the nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);
		NationTechData nationTechData = (NationTechData)DataManager.GetData(Constants.DT_NATIONTECH, nationID, false);

		// If the requirements are not met for the chosen tech, return message.
		if (RequirementsMet(_techID, nationData, nationTechData) == false)
		{
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_advance_not_available")); // "This advance is not available."
			return;
		}

		// Get the data of the tech to research
		TechData techData = TechData.GetTechData(_techID);

		// Do nothing if the given techID is not valid.
		if (techData == null) {
			return;
		}

		// Do not allow the technology to be added if the nation already has it.
		if (nationTechData.GetTechCount(_techID) > 0)
		{
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_advance_already_have", "advance_name", "{Technologies/tech_" + _techID + "_name}")); // "We already have " + techData.name + "."
			return;
		}

		// Do nothing if this advance must be purchased.
		if (techData.default_price > 0) {
			return;
		}

		// Return message if the nation doesn't have an advance point to spend on this advance.
		if (nationData.advance_points < 1)
		{
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_advance_not_enough_points")); // "We don't have enough advance points to research this."
			return;
		}

		// Determine and store the nation's new number of advance_points.
		nationData.advance_points -= 1;

		// Add the given tech to the nation
		AddTechnology(nationID, _techID, 0, true, true, 0);

		// Post report to nation
		Comm.SendReport(nationID, ClientString.Get("svr_report_tech_researched", "tech_name", "{Technologies/tech_" + _techID + "_name}", "username", userData.name), 0); // "{tech_name} has been researched by {username}."

		// Return message
		OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_advance_gained", "advance_name", "{Technologies/tech_" + _techID + "_name}")); // "We have gained " + techData.name + "!"
	}

	public static void Purchase(StringBuffer _output_buffer, int _userID, int _techID)
	{
		// Get the user's data
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		// If the user's rank disallows this action, return.
		if (userData.rank > Constants.RANK_COMMANDER) {
			return;
		}

		// Get the user's nation ID
		int nationID = userData.nationID;

		// Get the nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);
		NationTechData nationTechData = (NationTechData)DataManager.GetData(Constants.DT_NATIONTECH, nationID, false);

		// If the requirements are not met for the chosen tech, return message.
		if (RequirementsMet(_techID, nationData, nationTechData) == false)
		{
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_advance_not_available")); // "This advance is not available."
			return;
		}

		// Get the data of the tech to purchase
		TechData techData = TechData.GetTechData(_techID);

		// Do nothing if the given techID is not valid.
		if (techData == null) {
			return;
		}

		// Do not allow the technology to be added if the nation already has it.
		if (nationTechData.GetTechCount(_techID) > 0)
		{
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_advance_already_have", "advance_name", "{Technologies/tech_" + _techID + "_name}")); // "We already have " + techData.name + "."
			return;
		}

		// Get the tech's price record
		TechPriceRecord tech_price_record = GlobalData.instance.GetTechPriceRecord(_techID, false);

		if (tech_price_record == null)
		{
			Output.PrintToScreen("ERROR: No TechPriceRecord found for techID " + _techID + " being purchased by " + userData.name + " (" + _userID + ").");
			return;
		}

		if (nationData.game_money < tech_price_record.price)
		{
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_advance_purchase_not_enough_credits")); // "We don't have enough credits to buy this advance."
			return;
		}

		// Determine and store the nation's new amount of game_money
		Money.SubtractCost(nationData, tech_price_record.price);

		// Update the nation's users' reports.
		nationData.ModifyUserReportValueFloat(UserData.ReportVal.report__credits_spent, (float)tech_price_record.price);

		// Add the given tech to the nation
		AddTechnology(nationID, _techID, 0, true, true, 0);

		// Increment this tech's global purchase count
		tech_price_record.purchase_count++;
		DataManager.MarkForUpdate(GlobalData.instance);

		// Broadcast an update event to the nation, letting all players know about the decrease in credits.
		OutputEvents.BroadcastUpdateEvent(nationID);

		// Log this purchase
		Constants.WriteToNationLog(nationData, userData, "Purchased '" + techData.name + "'");

		// Post report to nation
		Comm.SendReport(nationID, ClientString.Get("svr_report_tech_purchased", "tech_name", "{Technologies/tech_" + _techID + "_name}", "username", userData.name), 0); // "{tech_name} has been purchased by {username}."

		// Add change in credits amount to history if appropriate
		if (((int)(nationData.game_money / 250)) != ((int)((nationData.game_money + tech_price_record.price) / 250))) {
			Comm.SendReport(nationID, ClientString.Get("svr_report_after_purchase_num_credits", "username", userData.name, "tech_name", "{Technologies/tech_" + _techID + "_name}", "num_credits", String.valueOf(((int)((nationData.game_money + tech_price_record.price) / 250)) * 250)), 0); // "After " + userData.name + "'s purchase of " + techData.name + ", we now have fewer than " + (((int)((nationData.game_money + game_price) / 250)) * 250) + " credits."
		}

		// Return message
		OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_advance_gained", "advance_name", "{Technologies/tech_" + _techID + "_name}")); // "We have gained " + techData.name + "!"
	}

	public static void SetTargetAdvance(StringBuffer _output_buffer, int _userID, int _advanceID)
	{
		// Get the user's data
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		// Get the user's nation ID
		int nationID = userData.nationID;

		// Get the nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

		// Set the nation's target advance.
		nationData.targetAdvanceID = _advanceID;

		// Mark the nation's data to be updated.
		DataManager.MarkForUpdate(nationData);

		// Broadcast set target advance event to the nation.
		OutputEvents.BroadcastSetTargetEvent(nationData);
	}

	public static void UpdateStats(int _nationID, NationData _nationData)
	{
		// If this nation's stats have already been updated at the current time, do nothing.
		if (_nationData.prev_update_stats_time == Constants.GetTime()) {
			return;
		}

		// Record the time when the nation's stats were last updated. Do this early to avoid a recursive loop with Technology.UpdateStats().
		int prev_update_stats_time = _nationData.prev_update_stats_time;
		_nationData.prev_update_stats_time = Constants.GetTime();

		// Get the nation's tech data.
		NationTechData nationTechData = (NationTechData)DataManager.GetData(Constants.DT_NATIONTECH, _nationData.ID, false);

		// Check whether any temporary technologies must be removed from the given nation.
		//Output.PrintToScreen("UpdateStats() nationName: " + _nationData.name") +  " time: " + Constants.GetTime() + ", exp time: " + _nationData.nextTechExpireTime"));

		while ((_nationData.nextTechExpireTime != -1) && (_nationData.nextTechExpireTime <= Constants.GetTime()))
		{
			// Only remove the technology if the nation actually has it.
			if (nationTechData.GetTechCount(_nationData.nextTechExpire) > 0)
			{
				// Update the nation's stats, for the period of time up until this tech expired.
				float period = _nationData.nextTechExpireTime - prev_update_stats_time;
				UpdateStatsForPeriod(_nationData, period);

				// Record the time up through which the nation's stats were last updated.
				prev_update_stats_time = _nationData.nextTechExpireTime;

				// Record the ID of the tech that is expiring, in case it needs to be removed multiple times below.
				int expiring_techID = _nationData.nextTechExpire;

				// Remove the expired temp technology from the nation.
				// Remove every instance of the expired tech, if there is more than one, because there is only one expire time mapped per techID.
				// So, the count for that techID must be returned to 0 when the expire time is reached, or else the remaining instances will never expire.
				// This is not an important enough case to justify keeping a separate expire time for each instance of a temp tech, because very rarely
				// would a nation have multiple instances of the same temp tech. Only in the case of duplicate long lasting discoveries.
				do
				{
					Technology.RemoveTechnology(_nationID, expiring_techID, 0);
				}
				while (nationTechData.GetTechCount(expiring_techID) > 0);
			}
			else
			{
				Output.PrintToScreen("ERROR: UpdateStats() attempted to remove nextTechExpire " + _nationData.nextTechExpire + " from nation " + _nationData.ID + " which does not have that tech.");

				// Make sure the nextTechExpire is no longer in the nation's tech_temp_expire_time list.
				nationTechData.tech_temp_expire_time.remove(_nationData.nextTechExpire);

				// Determine the nation's nextTechExpireTime and nextTechExpire
				DetermineNextTechExpire(_nationID, _nationData, nationTechData);
			}
		}

		// Update the nation's stats for the period of time from the last update up until the current time.
		float period = Constants.GetTime() - prev_update_stats_time;
		UpdateStatsForPeriod(_nationData, period);

		// Mark the nation's data to be updated
		DataManager.MarkForUpdate(_nationData);
	}

	public static void UpdateStatsForPeriod(NationData _nationData, float _period)
	{
		//// TESTING
		//Output.PrintToScreen("UpdateStatsForPeriod() pre shared_manpower_capacity: " + _nationData.shared_manpower_capacity + ", shared_manpower_fill: " + _nationData.shared_manpower_fill + ", shared_energy_capacity: " + _nationData.shared_energy_capacity + ", shared_energy_fill: " + _nationData.shared_energy_fill + ", period: " + _period + ", addition: " + (_period / (float)Constants.STORAGE_FILL_PERIOD));

		// Fill the nation's homeland shards, for the given period of time.
		_nationData.shard_red_fill = Math.min(1f, _nationData.shard_red_fill + (_period / (float)Constants.SHARD_FILL_PERIOD));
		_nationData.shard_green_fill = Math.min(1f, _nationData.shard_green_fill + (_period / (float)Constants.SHARD_FILL_PERIOD));
		_nationData.shard_blue_fill = Math.min(1f, _nationData.shard_blue_fill + (_period / (float)Constants.SHARD_FILL_PERIOD));

		// If this nation is eligible to participate in raids, and has been active recently enough, award orb shard defense rewards for the given period of time.
		if (_nationData.raid_eligible && ((Constants.GetTime() - _nationData.prev_use_time) < Constants.TIME_SINCE_LAST_USE_DISABLE_GOALS))
		{
			float shard_fill = (_nationData.shard_red_fill + _nationData.shard_green_fill + _nationData.shard_blue_fill) / 3f;
			LeagueData league_data = Raid.GetNationLeague(_nationData);

			// Determine the defense rewards.
			float fraction_of_day = _period / (float)Constants.SECONDS_PER_DAY;
			float credits_delta = fraction_of_day * (float)league_data.defense_daily_credits;
			float xp_delta = fraction_of_day * (float)league_data.defense_daily_xp;
			float rebirth_delta = fraction_of_day * (float)league_data.defense_daily_rebirth;

			//Output.PrintToScreen("Awarding nation " + _nationData.name + " defense reward for " + _period + " seconds, of " + credits_delta + " credits, " + xp_delta + " xp, " + rebirth_delta + " rebirth.");

			// Award the defense rewards.
			Money.AddGameMoney(_nationData, credits_delta, Money.Source.FREE);
			Gameplay.AddXP(_nationData, xp_delta, -1, -1, -1, false, true, 0, Constants.XP_RAID_DEFENSE);
			Gameplay.ChangeRebirthCountdown(_nationData, rebirth_delta);

			// Log suspect
			if ((_nationData.log_suspect_expire_time > Constants.GetTime()) && (xp_delta > 1))
			{
				// Log the details of this xp gain.
				Constants.WriteToLog("log_suspect.txt", Constants.GetTimestampString() + ": '" + _nationData.name + "'(ID:" + _nationData.ID + ", Level:" + _nationData.level + ") received " + xp_delta + " XP for home island defense.\n");
			}

			// Update the nation's records of orb shard earnings.
			_nationData.orb_shard_earnings_history += credits_delta;
			_nationData.orb_shard_earnings_history_monthly += credits_delta;

			// Update the nation's orb shard earnings ranks.
			RanksData.instance.ranks_nation_orb_shard_earnings.UpdateRanks(_nationData.ID, _nationData.name, _nationData.orb_shard_earnings_history, Constants.NUM_GLOBAL_PRIZE_RANKS, false);
			RanksData.instance.ranks_nation_orb_shard_earnings_monthly.UpdateRanks(_nationData.ID, _nationData.name, _nationData.orb_shard_earnings_history_monthly, Constants.NUM_GLOBAL_PRIZE_RANKS, false);

			// Record reports of defense rewards.
			_nationData.ModifyUserReportValueFloat(UserData.ReportVal.report__home_defense_credits, credits_delta);
			_nationData.ModifyUserReportValueFloat(UserData.ReportVal.report__home_defense_xp, xp_delta);
			_nationData.ModifyUserReportValueFloat(UserData.ReportVal.report__home_defense_rebirth, rebirth_delta);
		}

		// Fill the nation's manpower storage structures, for the given period of time.
		if (_nationData.shared_manpower_capacity > 0) {
			_nationData.shared_manpower_fill = Math.min(1f, _nationData.shared_manpower_fill + (_period / (float)Constants.STORAGE_FILL_PERIOD));
		}

		// Fill the nation's energy storage structures, for the given period of time.
		if (_nationData.shared_energy_capacity > 0) {
			_nationData.shared_energy_fill = Math.min(1f, _nationData.shared_energy_fill + (_period / (float)Constants.STORAGE_FILL_PERIOD));
		}

		//// TESTING
		//Output.PrintToScreen("UpdateStatsForPeriod() post shared_manpower_capacity: " + _nationData.shared_manpower_capacity + ", shared_manpower_fill: " + _nationData.shared_manpower_fill + ", shared_energy_capacity: " + _nationData.shared_energy_capacity + ", shared_energy_fill: " + _nationData.shared_energy_fill + ", period: " + _period + ", addition: " + (_period / (float)Constants.STORAGE_FILL_PERIOD));

		// Update the nation's manpower, for the given period of time.

		// Determine the nation's manpower burn rate.
		_nationData.DetermineManpowerBurnRate();

		// Modify the nation's mainland manpower by the appropriate amount for this time period.
		float manpower_delta = (_nationData.GetFinalManpowerRateMinusBurn(Constants.MAINLAND_MAP_ID) * (_period / 3600.0f));
		_nationData.mainland_footprint.manpower += manpower_delta;

		// If we don't have enough mainland manpower given present burn rate, attempt to take manpower from allies.
		if (_nationData.mainland_footprint.manpower < 0) {
			Gameplay.TakeManpowerFromAllies(_nationData);
		}

		// Update the nation's users' report of how much manpower has been lost to excess resources.
		_nationData.ModifyUserReportValueFloat(UserData.ReportVal.report__manpower_lost_to_resources, Math.min(_nationData.mainland_footprint.manpower, _nationData.manpower_burn_rate * (_period / 3600.0f)));

		// Constrain this nation's mainland manpower to the appropriate bounds.
		_nationData.mainland_footprint.manpower = Math.max(Math.min(_nationData.mainland_footprint.manpower, _nationData.GetMainlandManpowerMax()), 0);

		if (_nationData.homeland_mapID > 0)
		{
			// Modify the nation's homeland manpower by the appropriate amount for this time period.
			manpower_delta = (_nationData.GetFinalManpowerRateMinusBurn(_nationData.homeland_mapID) * (_period / 3600.0f));
			_nationData.homeland_footprint.manpower += manpower_delta;

			// Constrain this nation's homeland manpower to the appropriate bounds.
			_nationData.homeland_footprint.manpower = Math.max(Math.min(_nationData.homeland_footprint.manpower, _nationData.GetFinalManpowerMax(_nationData.homeland_mapID)), 0);
		}

		// Update the nation's energy, for the given period of time.

		// Increase the nation's energy by the appropriate amount for this time period.
		float energy_increase = (_nationData.GetFinalEnergyRate(Constants.MAINLAND_MAP_ID) * (_period / 3600.0f));
		_nationData.energy += energy_increase;

		// Determine how much energy the nation burns during this time period.
		float energy_burn = (_nationData.GetFinalEnergyBurnRate(Constants.MAINLAND_MAP_ID) * (_period / 3600.0f));

		// If we don't have enough energy given present burn rate, attempt to take energy from allies.
		if (energy_burn >= _nationData.energy) {
			Gameplay.TakeEnergyFromAllies(_nationData);
		}

		// Update the nation's users' report of how much energy has been spent.
		_nationData.ModifyUserReportValueFloat(UserData.ReportVal.report__energy_spent, Math.min(_nationData.energy, energy_burn));

		// Remove this nation's energy_burn from this nation's energy.
		_nationData.energy -= energy_burn;

		// TESTING
		//if (energy_burn > energy_increase)
		//{
		//	Output.PrintToScreen("Nation " + _nationData.name + " energy_increase: " + energy_increase + ", energy_burn: " + energy_burn + ", energy: " + _nationData.energy);
		//}

/*
		// Remove a portion of the energy_burn from each of this nation's allies' energy, proportional to that ally's energy's share in all of this nation's available energy.
		NationData allyNationData;
		float energy_burn_ally_share;
		for (int cur_alliance_index = 0; cur_alliance_index < _nationData.alliances_active.size(); cur_alliance_index++)
		{
			// Get the current ally nation's data.
			allyNationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationData.alliances_active.get(cur_alliance_index), false);

			// Remove this ally nation's share of the energy_burn from its energy.
			energy_burn_ally_share = (_nationData.available_energy == 0) ? 0 : (energy_burn * (allyNationData.energy / _nationData.available_energy));
			allyNationData.energy -= energy_burn_ally_share;

			// Remove the full energy_burn from this ally nation's available_energy.
			allyNationData.available_energy -= energy_burn;

			// Constrain the ally's energy and available_energy to the appropriate bounds.
			allyNationData.energy = Math.max(allyNationData.energy, 0);
			allyNationData.available_energy = Math.max(Math.min(allyNationData.available_energy, allyNationData.available_energy_max), 0);

			// Sanity check available_energy
			if (allyNationData.available_energy < 0)
			{
				Output.PrintToScreen("ERROR: Nation " + allyNationData.ID + "'s available_energy is " + allyNationData.available_energy + ". Setting to 0.");
				Output.PrintStackTrace();
				allyNationData.available_energy = 0;
			}

			// Update the quests system for the donation of this amount of energy to an ally.
			Quests.HandleDonateEnergyToAlly(allyNationData, energy_burn_ally_share);

			// Update the ally nation's users' reports.
			allyNationData.ModifyUserReportValueFloat(UserData.ReportVal.report__energy_donated, energy_burn_ally_share);
		}
*/

		// Constrain this nation's energy to the appropriate bounds.
		_nationData.energy = Math.max(Math.min(_nationData.energy, _nationData.GetFinalEnergyMax()), 0);
	}

	public static void UpdateTechPlayTimes(UserData _userData, NationData _nationData, int _play_time)
	{
	  // Get the user's nation's data
		NationTechData nationTechData = (NationTechData)DataManager.GetData(Constants.DT_NATIONTECH, _nationData.ID, false);

		// If player has no nation, return.
		if (nationTechData == null) {
			return;
		}

		// Add the given _play_time to the play time record of each purchasable tech that is available to this nation.
		for (TechPriceRecord tech_price_record : GlobalData.instance.tech_price_records.values())
		{
			if ((tech_price_record.price > 0) && (RequirementsMet(tech_price_record.ID, _nationData, nationTechData))) {
				tech_price_record.play_time += _play_time;
			}
		}

		// Update the global data
		DataManager.MarkForUpdate(GlobalData.instance);
	}

  public static void UpdateTempTechsForDowntime()
  {
    NationData curNationData;
    NationTechData curNationTechData;
    int num_techs, cur_tech_index;

    // Determine the difference between the current time and the previous heartbeat's time
    int time_difference = Constants.GetTime() - GlobalData.instance.heartbeat;

    // Do nothing if the time difference is too great or too small
    if ((time_difference > (3600 * 24 * 7)) || (time_difference < 300)) {
      return;
    }

    Output.PrintToScreen("Updating temp techs for server downtime of " + (time_difference / 60) + " minutes");

    // Determine highest nation ID
    int highestNationID = DataManager.GetHighestDataID(Constants.DT_NATION);

    // Iterate through each nation
    for (int curNationID = 1; curNationID <= highestNationID; curNationID++)
    {
      if ((curNationID % 10000) == 0) {
        Output.PrintToScreen("Updating nation " + curNationID);
      }

      // Get the data for the nation with the current ID
      curNationData = (NationData)DataManager.GetData(Constants.DT_NATION, curNationID, false);

      // If no nation exists with this ID, continue to next.
      if (curNationData == null) {
        continue;
      }

      if (curNationData.nextTechExpire > 0)
      {
        // Get the tech data for the nation with the current ID
        curNationTechData = (NationTechData)DataManager.GetData(Constants.DT_NATIONTECH, curNationID, false);

        if (curNationTechData == null) {
          continue;
        }

        // Update the nation's nextTechExpireTime
        curNationData.nextTechExpireTime += time_difference;

        // Update expiration time of each of this nation's temp techs
				for (Map.Entry<Integer, Integer> entry : curNationTechData.tech_temp_expire_time.entrySet())
				{
					curNationTechData.tech_temp_expire_time.put(entry.getKey(), entry.getValue() + time_difference);
				}

 				// Mark the nation's data to be updated
				DataManager.MarkForUpdate(curNationData);
        DataManager.MarkForUpdate(curNationTechData);
      }
    }
  }
};
