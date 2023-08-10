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
import java.util.Iterator;
import java.util.Map;
import java.util.ArrayList;
import java.util.Vector;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.FileReader;
import WOCServer.*;

public class TechData
{
	public static final int DURATION_PERMANENT       = 0;
	public static final int DURATION_TEMPORARY       = 1;
	public static final int DURATION_OBJECT          = 2;

	public static final int CATEGORY_UNDEF      = -1;
	public static final int CATEGORY_TECH       = 0;
	public static final int CATEGORY_BIO        = 1;
	public static final int CATEGORY_PSI        = 2;
	public static final int CATEGORY_TECH_BUY   = 3;
	public static final int CATEGORY_BIO_BUY    = 4;
	public static final int CATEGORY_PSI_BUY    = 5;
	public static final int CATEGORY_BASE_BUY   = 6;

	public static final int BONUS_UNDEF                 = -1;
	public static final int BONUS_TECH                  = 0;
	public static final int BONUS_BIO                   = 1;
	public static final int BONUS_PSI                   = 2;
	public static final int BONUS_MANPOWER_RATE         = 3;
	public static final int BONUS_MANPOWER_MAX          = 4;
	public static final int BONUS_ENERGY_RATE           = 5;
	public static final int BONUS_ENERGY_MAX            = 6;
	public static final int BONUS_GEO_EFFICIENCY        = 7;
	public static final int BONUS_XP_MULTIPLIER         = 8;
	public static final int BONUS_HP_PER_SQUARE         = 9;
	public static final int BONUS_HP_RESTORE            = 10;
	public static final int BONUS_ATTACK_MANPOWER       = 11;
	public static final int BONUS_SIMULTANEOUS_ACTIONS  = 12;
	public static final int BONUS_CRIT_CHANCE           = 13;
	public static final int BONUS_SPLASH_DAMAGE         = 14;
	public static final int BONUS_SALVAGE_VALUE         = 15;
	public static final int BONUS_WALL_DISCOUNT         = 16;
	public static final int BONUS_STRUCTURE_DISCOUNT    = 17;
	public static final int BONUS_MAX_ALLIANCES         = 18;
	public static final int BONUS_INVISIBILITY          = 19;
	public static final int BONUS_TECH_MULT             = 20;
	public static final int BONUS_BIO_MULT              = 21;
	public static final int BONUS_PSI_MULT              = 22;
	public static final int BONUS_MANPOWER_RATE_MULT    = 23;
	public static final int BONUS_MANPOWER_MAX_MULT     = 24;
	public static final int BONUS_ENERGY_RATE_MULT      = 25;
	public static final int BONUS_ENERGY_MAX_MULT       = 26;
	public static final int BONUS_HP_PER_SQUARE_MULT    = 27;
	public static final int BONUS_HP_RESTORE_MULT       = 28;
	public static final int BONUS_ATTACK_MANPOWER_MULT  = 29;
	public static final int BONUS_CREDITS               = 30;
	public static final int BONUS_INSURGENCY            = 31;
	public static final int BONUS_TOTAL_DEFENSE         = 32;

	String name = "";
	String icon = "";
	boolean initial = false, random = false;
	String description = "";
	String prerequisite_tech_1_name = "", prerequisite_tech_2_name = "";
	int ID = -1, duration_time = 0, default_price = 0, min_price = 0, max_price = 0, prerequisite_level = 0, new_build = -1, obsolete_build = -1;
	int prerequisite_tech_1 = -1, prerequisite_tech_2 = -1;
	int bonus_type_1 = BONUS_UNDEF, bonus_type_2 = BONUS_UNDEF, bonus_type_3 = BONUS_UNDEF;
	int bonus_val_1 = 0, bonus_val_2 = 0, bonus_val_3 = 0;
	int bonus_val_max_1 = 0, bonus_val_max_2 = 0, bonus_val_max_3 = 0;
	int duration_type = DURATION_PERMANENT;
	int category = CATEGORY_TECH;

	public static HashMap<Integer,TechData> techs = new HashMap<Integer,TechData>();
	public static HashMap<String,Integer> tech_name_map = new HashMap<String,Integer>();
	public static ArrayList<Integer> initial_advances = new ArrayList<Integer>();
	public static ArrayList<Integer> random_advances = new ArrayList<Integer>();
	public static String version_string = "";

	public static TechData GetTechData(int _ID)
	{
		return techs.getOrDefault(_ID, null);
	}

	public static int GetNameToIDMap(String _name)
	{
		return tech_name_map.getOrDefault(_name, -1);
	}

	public static Boolean LoadTechnologies()
	{
		int line_num = -1;
		BufferedReader br;
		String filename = "technologies.tsv";
		boolean tech_price_record_updated = false;

		try
		{
			br = new BufferedReader(new FileReader(filename));
		}
		catch (Exception e)
		{
			Output.PrintToScreen("File not found: " + filename);
			return false;
		}

		try
		{
			TechData tech_data;
			String line;
			int[] place = new int[1];

			while ((line = br.readLine()) != null)
			{
				// Increment line number.
				line_num++;

				// Read the version line
				if (line_num == 0)
				{
					version_string = line;
					continue;
				}

				// Process the line.

				// Skip comments
				if (line.charAt(0) == '#') {
					continue;
				}

				// Create a TechData object for this line's technology.
				tech_data = new TechData();

				// Start at the beginning of the line.
				place[0] = 0;

				// Name
				tech_data.name = Constants.GetNextTabSeparatedValue(line, place);

				// ID
				String ID_string = Constants.GetNextTabSeparatedValue(line, place);
				tech_data.ID = ID_string.isEmpty() ? -1 : Integer.parseInt(ID_string);

				// Category
				String category_string = Constants.GetNextTabSeparatedValue(line, place);
				if (category_string.equalsIgnoreCase("tech")) {
					tech_data.category = TechData.CATEGORY_TECH;
				} else if (category_string.equalsIgnoreCase("bio")) {
					tech_data.category = TechData.CATEGORY_BIO;
				} else if (category_string.equalsIgnoreCase("psi")) {
					tech_data.category = TechData.CATEGORY_PSI;
				} else if (category_string.equalsIgnoreCase("tech_buy")) {
					tech_data.category = TechData.CATEGORY_TECH_BUY;
				} else if (category_string.equalsIgnoreCase("bio_buy")) {
					tech_data.category = TechData.CATEGORY_BIO_BUY;
				} else if (category_string.equalsIgnoreCase("psi_buy")) {
					tech_data.category = TechData.CATEGORY_PSI_BUY;
				} else if (category_string.equalsIgnoreCase("base_buy")) {
					tech_data.category = TechData.CATEGORY_BASE_BUY;
				} else if (category_string.equalsIgnoreCase("none")) {
					tech_data.category = TechData.CATEGORY_UNDEF;
				} else {
					tech_data.category = TechData.CATEGORY_UNDEF;
					Output.PrintToScreen("Unknown category '" + category_string + "' at line " + line_num + ".");
				}

				// Order
				String order_string = Constants.GetNextTabSeparatedValue(line, place);

				// Icon
				tech_data.icon = Constants.GetNextTabSeparatedValue(line, place);

				// Initial
				String initial_string = Constants.GetNextTabSeparatedValue(line, place);
				tech_data.initial = (initial_string.equalsIgnoreCase("TRUE"));

				// Random
				String random_string = Constants.GetNextTabSeparatedValue(line, place);
				tech_data.random = (random_string.equalsIgnoreCase("TRUE"));

				// Duration type
				String duration_type_string = Constants.GetNextTabSeparatedValue(line, place);
				if (duration_type_string.equalsIgnoreCase("permanent")) {
					tech_data.duration_type = TechData.DURATION_PERMANENT;
				} else if (duration_type_string.equalsIgnoreCase("temporary")) {
					tech_data.duration_type = TechData.DURATION_TEMPORARY;
				} else if (duration_type_string.equalsIgnoreCase("object")) {
					tech_data.duration_type = TechData.DURATION_OBJECT;
				} else {
					Output.PrintToScreen("Unknown duration type '" + duration_type_string + "' at line " + line_num + ".");
				}

				// Duration time
				String duration_time_string = Constants.GetNextTabSeparatedValue(line, place);
				tech_data.duration_time = duration_time_string.isEmpty() ? 0 : Integer.parseInt(duration_time_string);

				// Default price
				String default_price_string = Constants.GetNextTabSeparatedValue(line, place);
				tech_data.default_price = default_price_string.isEmpty() ? 0 : Integer.parseInt(default_price_string);

				// Min price
				String min_price_string = Constants.GetNextTabSeparatedValue(line, place);
				tech_data.min_price = min_price_string.isEmpty() ? 0 : Integer.parseInt(min_price_string);

				// Max price
				String max_price_string = Constants.GetNextTabSeparatedValue(line, place);
				tech_data.max_price = max_price_string.isEmpty() ? 0 : Integer.parseInt(max_price_string);

				// Prerequisite tech 1
				tech_data.prerequisite_tech_1_name = Constants.GetNextTabSeparatedValue(line, place);

				// Prerequisite tech 2
				tech_data.prerequisite_tech_2_name = Constants.GetNextTabSeparatedValue(line, place);

				// Prerequisite level
				String prerequisite_level_string = Constants.GetNextTabSeparatedValue(line, place);
				tech_data.prerequisite_level = prerequisite_level_string.isEmpty() ? 0 : Integer.parseInt(prerequisite_level_string);

				// Bonus type 1
				String bonus_type_1_string = Constants.GetNextTabSeparatedValue(line, place);
				tech_data.bonus_type_1 = ParseBonusType(bonus_type_1_string, line_num);

				// Bonus value 1
				String bonus_value_1_string = Constants.GetNextTabSeparatedValue(line, place);
				tech_data.bonus_val_1 = bonus_value_1_string.isEmpty() ? 0 : Integer.parseInt(bonus_value_1_string);

				// Bonus value max 1 (used to set range for landscape object bonus)
				String bonus_value_max_1_string = Constants.GetNextTabSeparatedValue(line, place);
				tech_data.bonus_val_max_1 = bonus_value_max_1_string.isEmpty() ? 0 : Integer.parseInt(bonus_value_max_1_string);

				// Bonus type 2
				String bonus_type_2_string = Constants.GetNextTabSeparatedValue(line, place);
				tech_data.bonus_type_2 = ParseBonusType(bonus_type_2_string, line_num);

				// Bonus value 2
				String bonus_value_2_string = Constants.GetNextTabSeparatedValue(line, place);
				tech_data.bonus_val_2 = bonus_value_2_string.isEmpty() ? 0 : Integer.parseInt(bonus_value_2_string);

				// Bonus value max 2 (used to set range for landscape object bonus)
				String bonus_value_max_2_string = Constants.GetNextTabSeparatedValue(line, place);
				tech_data.bonus_val_max_2 = bonus_value_max_2_string.isEmpty() ? 0 : Integer.parseInt(bonus_value_max_2_string);

				// Bonus type 3
				String bonus_type_3_string = Constants.GetNextTabSeparatedValue(line, place);
				tech_data.bonus_type_3 = ParseBonusType(bonus_type_3_string, line_num);

				// Bonus value 3
				String bonus_value_3_string = Constants.GetNextTabSeparatedValue(line, place);
				tech_data.bonus_val_3 = bonus_value_3_string.isEmpty() ? 0 : Integer.parseInt(bonus_value_3_string);

				// Bonus value max 3 (used to set range for landscape object bonus)
				String bonus_value_max_3_string = Constants.GetNextTabSeparatedValue(line, place);
				tech_data.bonus_val_max_3 = bonus_value_max_3_string.isEmpty() ? 0 : Integer.parseInt(bonus_value_max_3_string);

				// New build
				String new_build_string = Constants.GetNextTabSeparatedValue(line, place);
				tech_data.new_build = BuildData.GetNameToIDMap(new_build_string);

				if ((new_build_string.isEmpty() == false) && (tech_data.new_build == -1))
				{
					Output.PrintToScreen("Technology '" + tech_data.name + "' has invalid new build '" + new_build_string + "' at line " + line_num);
					return false;
				}

				// Connect this technology to the build that it enables.
				if (tech_data.new_build != -1)
				{
					BuildData build_data = BuildData.GetBuildData(tech_data.new_build);
					build_data.required_tech  = tech_data;
				}

				// Obsolete build
				String obsolete_build_string = Constants.GetNextTabSeparatedValue(line, place);
				tech_data.obsolete_build = BuildData.GetNameToIDMap(obsolete_build_string);

				if ((obsolete_build_string.isEmpty() == false) && (tech_data.obsolete_build == -1))
				{
					Output.PrintToScreen("Technology '" + tech_data.name + "' has invalid obsolete build '" + obsolete_build_string + "' at line " + line_num);
					return false;
				}

				// Obsolete tech
				Constants.GetNextTabSeparatedValue(line, place);

				// Description
				tech_data.description = Constants.GetNextTabSeparatedValue(line, place);

				if (tech_data.ID < 0)
				{
					Output.PrintToScreen("Technology with missing ID at line " + line_num);
					return false;
				}

				// Add this technology to the techs vector and tech_name_map.
				techs.put(tech_data.ID, tech_data);
				tech_name_map.put(tech_data.name, tech_data.ID);

				// If this is an initial advance, add it to the initial_advances list.
				if (tech_data.initial) {
					initial_advances.add(tech_data.ID);
				}

				// If this is a random advance, add it to the random_advances list.
				if (tech_data.random) {
					random_advances.add(tech_data.ID);
				}

				// If this is a purchasable advance...
				if (tech_data.default_price != 0)
				{
					// Get (or create) the TechPriceRecord for this advance.
					TechPriceRecord tech_price_record = GlobalData.instance.GetTechPriceRecord(tech_data.ID, true);

					// If the TechPriceRecord does not reflect that this is a purchasable tech, initialize it for the tech's default price.
					if (tech_price_record.price == 0)
					{
						tech_price_record.price = tech_data.default_price;
						tech_price_record.prev_price = tech_data.default_price;
						tech_price_record.purchase_count = 0;
						tech_price_record.play_time = 0;
						tech_price_record.prev_revenue_rate = 0f;
						tech_price_record_updated = true;
					}
				}
			}
		}
		catch (Exception e)
		{
			Output.PrintToScreen("Error loading technologies at line " + line_num);
			Output.PrintException(e);
			return false;
		}

		TechData tech_data;
		Iterator it = techs.entrySet().iterator();
    while (it.hasNext())
		{
			Map.Entry pair = (Map.Entry)it.next();
			tech_data = (TechData)(pair.getValue());

			//Output.PrintToScreen("Tech " + tech_data.name + ", tech_data.prerequisite_tech_1_name: " + tech_data.prerequisite_tech_1_name + ", tech_data.prerequisite_tech_2_name: " + tech_data.prerequisite_tech_2_name);

			if (tech_data.prerequisite_tech_1_name.equals(""))
			{
				tech_data.prerequisite_tech_1 = -1;
			}
			else
			{
				tech_data.prerequisite_tech_1 = GetNameToIDMap(tech_data.prerequisite_tech_1_name);

				if (tech_data.prerequisite_tech_1 < 0)
				{
					Output.PrintToScreen("\'" + tech_data.name + "\' prerequisite 1 name \'" + tech_data.prerequisite_tech_1_name + "\' not a valid technology.");
					return false;
				}
			}

			if (tech_data.prerequisite_tech_2_name.equals(""))
			{
				tech_data.prerequisite_tech_2 = -1;
			}
			else
			{
				tech_data.prerequisite_tech_2 = GetNameToIDMap(tech_data.prerequisite_tech_2_name);

				if (tech_data.prerequisite_tech_2 < 0)
				{
					Output.PrintToScreen("\'" + tech_data.name + "\' prerequisite 2 name \'" + tech_data.prerequisite_tech_2_name + "\' not a valid technology.");
					return false;
				}
			}
    }

		// If any TechPriceRecords have been added, mark the GlobalData to be updated.
		if (tech_price_record_updated)
		{
			Output.PrintToScreen("TechPriceRecords have been added.");
			DataManager.MarkForUpdate(GlobalData.instance);
		}

		return (techs.size() > 0);
	}

	public static int ParseBonusType(String _string, int _line_num)
	{
		if (_string.equalsIgnoreCase("")) {
			return BONUS_UNDEF;
		} else if (_string.equalsIgnoreCase("tech")) {
			return BONUS_TECH;
		} else if (_string.equalsIgnoreCase("bio")) {
			return BONUS_BIO;
		} else if (_string.equalsIgnoreCase("psi")) {
			return BONUS_PSI;
		} else if (_string.equalsIgnoreCase("manpower_rate")) {
			return BONUS_MANPOWER_RATE;
		} else if (_string.equalsIgnoreCase("manpower_max")) {
			return BONUS_MANPOWER_MAX;
		} else if (_string.equalsIgnoreCase("energy_rate")) {
			return BONUS_ENERGY_RATE;
		} else if (_string.equalsIgnoreCase("energy_max")) {
			return BONUS_ENERGY_MAX;
		} else if (_string.equalsIgnoreCase("geo_efficiency")) {
			return BONUS_GEO_EFFICIENCY;
		}else if (_string.equalsIgnoreCase("xp_multiplier")) {
			return BONUS_XP_MULTIPLIER;
		} else if (_string.equalsIgnoreCase("hp_per_square")) {
			return BONUS_HP_PER_SQUARE;
		} else if (_string.equalsIgnoreCase("hp_restore")) {
			return BONUS_HP_RESTORE;
		} else if (_string.equalsIgnoreCase("attack_manpower")) {
			return BONUS_ATTACK_MANPOWER;
		} else if (_string.equalsIgnoreCase("simultaneous_actions")) {
			return BONUS_SIMULTANEOUS_ACTIONS;
		} else if (_string.equalsIgnoreCase("crit_chance")) {
			return BONUS_CRIT_CHANCE;
		} else if (_string.equalsIgnoreCase("salvage_value")) {
			return BONUS_SALVAGE_VALUE;
		} else if (_string.equalsIgnoreCase("wall_discount")) {
			return BONUS_WALL_DISCOUNT;
		} else if (_string.equalsIgnoreCase("structure_discount")) {
			return BONUS_STRUCTURE_DISCOUNT;
		} else if (_string.equalsIgnoreCase("splash_damage")) {
			return BONUS_SPLASH_DAMAGE;
		} else if (_string.equalsIgnoreCase("max_alliances")) {
			return BONUS_MAX_ALLIANCES;
		} else if (_string.equalsIgnoreCase("invisibility")) {
			return BONUS_INVISIBILITY;
		} else if (_string.equalsIgnoreCase("tech_mult")) {
			return BONUS_TECH_MULT;
		} else if (_string.equalsIgnoreCase("bio_mult")) {
			return BONUS_BIO_MULT;
		} else if (_string.equalsIgnoreCase("psi_mult")) {
			return BONUS_PSI_MULT;
		} else if (_string.equalsIgnoreCase("manpower_rate_mult")) {
			return BONUS_MANPOWER_RATE_MULT;
		} else if (_string.equalsIgnoreCase("manpower_max_mult")) {
			return BONUS_MANPOWER_MAX_MULT;
		} else if (_string.equalsIgnoreCase("energy_rate_mult")) {
			return BONUS_ENERGY_RATE_MULT;
		} else if (_string.equalsIgnoreCase("energy_max_mult")) {
			return BONUS_ENERGY_MAX_MULT;
		} else if (_string.equalsIgnoreCase("hp_per_square_mult")) {
			return BONUS_HP_PER_SQUARE_MULT;
		} else if (_string.equalsIgnoreCase("hp_restore_mult")) {
			return BONUS_HP_RESTORE_MULT;
		} else if (_string.equalsIgnoreCase("attack_manpower_mult")) {
			return BONUS_ATTACK_MANPOWER_MULT;
		} else if (_string.equalsIgnoreCase("credits")) {
			return BONUS_CREDITS;
		} else if (_string.equalsIgnoreCase("insurgency")) {
			return BONUS_INSURGENCY;
		} else if (_string.equalsIgnoreCase("total_defense")) {
			return BONUS_TOTAL_DEFENSE;
		} else {
			Output.PrintToScreen("Unknown bonus type '" + _string + "' at line " + _line_num);
			return BONUS_UNDEF;
		}
	}
}
