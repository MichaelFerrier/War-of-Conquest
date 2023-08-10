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

public class BuildData
{
	public static final int TYPE_UNDEF                   = -1;
	public static final int TYPE_WALL                    = 0;
	public static final int TYPE_ENERGY_STORAGE          = 1;
	public static final int TYPE_MANPOWER_STORAGE        = 2;
	public static final int TYPE_DUMMY                   = 3;
	public static final int TYPE_DIRECTED_MULTIPLE       = 4;
	public static final int TYPE_SPLASH                  = 5;
	public static final int TYPE_AREA_EFFECT             = 6;
	public static final int TYPE_AREA_FORTIFICATION      = 7;
	public static final int TYPE_COUNTER_ATTACK          = 8;
	public static final int TYPE_WIPE                    = 9;
	public static final int TYPE_GENERAL_LASTING_WIPE    = 10;
	public static final int TYPE_SPECIFIC_LASTING_WIPE   = 11;
	public static final int TYPE_AIR_DROP                = 12;
	public static final int TYPE_RECAPTURE               = 13;
	public static final int TYPE_TOWER_BUSTER            = 14;
	public static final int TYPE_SHARD									 = 15;
	public static final int NUM_TYPES                    = 16;

	public static final int TRIGGER_ON_UNDEF = -1;
	public static final int TRIGGER_ON_RADIUS_ATTACK = 0;
	public static final int TRIGGER_ON_DIRECT_ATTACK = 1;
	public static final int TRIGGER_ON_RADIUS_TOWER = 2;
	public static final int TRIGGER_ON_RADIUS_ATTACK_EMPTY = 3;

	public static final int VISIBLE_ON_UNDEF = -1;
	public static final int VISIBLE_ON_TRIGGERED = 0;
	public static final int VISIBLE_ON_ATTACKED = 1;
	public static final int VISIBLE_ON_ALWAYS = 2;

	public static final int WIPE_FLAG_GENERAL     = 1;
	public static final int WIPE_FLAG_SPECIFIC    = 2;
	public static final int WIPE_FLAG_CHEMICAL    = 4;
	public static final int WIPE_FLAG_SUPERVIRUS  = 8;
	public static final int WIPE_FLAG_HYPONOTIC   = 16;
	public static final int WIPE_FLAG_TEMPLE      = 32;

	public static final int LAND_FLAG_MAINLAND      = 1;
	public static final int LAND_FLAG_HOMELAND      = 2;
	public static final int LAND_FLAG_RAID          = 4;
	public static final int LAND_FLAG_ALL           = 7;

	public static final int MAX_ATTACK_RADIUS = 5;
	public static final int SECONDS_REMAIN_VISIBLE = 120;
	public static final int COMPLETION_COST_PER_MINUTE = 2;

	String name = "";
	String description = "";
	String upgrades_name = "";
	int ID = -1;
	int type = TYPE_UNDEF;
	int upgrades = -1, land = 0, build_time = 0, hit_points = 0, manpower_cost = 0, energy_burn_rate = 0, max_count = -1;
	int trigger_on = TRIGGER_ON_UNDEF;
	int visible_on = VISIBLE_ON_UNDEF;
	int attack_radius = 0, effect_radius = 0, num_attacks = 0, attack_min_hp = 0, attack_max_hp = 0, wipe_duration = 0, cooldown_time = 0, capacity = 0, xp_per_hour = 0;
	boolean initial = false, flank_nullifies = false;

	public TechData required_tech = null;

	public static HashMap<Integer,BuildData> builds = new HashMap<Integer,BuildData>();
	public static HashMap<String,Integer> build_name_map = new HashMap<String,Integer>();
	public static ArrayList<Integer> initial_builds = new ArrayList<Integer>();
	public static String version_string = "";

	public static BuildData GetBuildData(int _ID)
	{
		return builds.getOrDefault(_ID, null);
	}

	public static int GetNameToIDMap(String _name)
	{
		return build_name_map.getOrDefault(_name, -1);
	}

	public static Boolean LoadBuilds()
	{
		int line_num = -1;
		String value_string;
		BufferedReader br;
		String filename = "builds.tsv";

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
			BuildData build_data;
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

				// Create a BuildData object for this line's build.
				build_data = new BuildData();

				// Start at the beginning of the line.
				place[0] = 0;

				// Name
				build_data.name = Constants.GetNextTabSeparatedValue(line, place);

				// ID
				value_string = Constants.GetNextTabSeparatedValue(line, place);
				build_data.ID = value_string.isEmpty() ? -1 : Integer.parseInt(value_string);

				// Upgrades
				build_data.upgrades_name = Constants.GetNextTabSeparatedValue(line, place);

				// Land
				value_string = Constants.GetNextTabSeparatedValue(line, place).toLowerCase();
				if (value_string.length() == 0)
				{
					build_data.land = LAND_FLAG_ALL;
				}
				else
				{
					if (value_string.contains("mainland")) build_data.land = build_data.land | LAND_FLAG_MAINLAND;
					if (value_string.contains("homeland")) build_data.land = build_data.land | LAND_FLAG_HOMELAND;
					if (value_string.contains("raid")) build_data.land = build_data.land | LAND_FLAG_RAID;
				}

				// Initial
				String initial_string = Constants.GetNextTabSeparatedValue(line, place);
				build_data.initial = (initial_string.equalsIgnoreCase("TRUE"));

				// Manpower cost
				value_string = Constants.GetNextTabSeparatedValue(line, place);
				build_data.manpower_cost = value_string.isEmpty() ? 0 : Integer.parseInt(value_string);

				// Energy burn rate
				value_string = Constants.GetNextTabSeparatedValue(line, place);
				build_data.energy_burn_rate = value_string.isEmpty() ? 0 : Integer.parseInt(value_string);

				// Hit points
				value_string = Constants.GetNextTabSeparatedValue(line, place);
				build_data.hit_points = value_string.isEmpty() ? 0 : Integer.parseInt(value_string);

				// Build time
				value_string = Constants.GetNextTabSeparatedValue(line, place);
				build_data.build_time = value_string.isEmpty() ? 0 : Integer.parseInt(value_string);

				// Type
				String type_string = Constants.GetNextTabSeparatedValue(line, place);
				if (type_string.equalsIgnoreCase("wall")) {
					build_data.type = BuildData.TYPE_WALL;
				} else if (type_string.equalsIgnoreCase("energy_storage")) {
					build_data.type = BuildData.TYPE_ENERGY_STORAGE;
				} else if (type_string.equalsIgnoreCase("manpower_storage")) {
					build_data.type = BuildData.TYPE_MANPOWER_STORAGE;
				} else if (type_string.equalsIgnoreCase("dummy")) {
					build_data.type = BuildData.TYPE_DUMMY;
				} else if (type_string.equalsIgnoreCase("directed_multiple")) {
					build_data.type = BuildData.TYPE_DIRECTED_MULTIPLE;
				} else if (type_string.equalsIgnoreCase("splash")) {
					build_data.type = BuildData.TYPE_SPLASH;
				} else if (type_string.equalsIgnoreCase("area_effect")) {
					build_data.type = BuildData.TYPE_AREA_EFFECT;
				} else if (type_string.equalsIgnoreCase("area_fortification")) {
					build_data.type = BuildData.TYPE_AREA_FORTIFICATION;
				} else if (type_string.equalsIgnoreCase("counter_attack")) {
					build_data.type = BuildData.TYPE_COUNTER_ATTACK;
				} else if (type_string.equalsIgnoreCase("wipe")) {
					build_data.type = BuildData.TYPE_WIPE;
				} else if (type_string.equalsIgnoreCase("general_lasting_wipe")) {
					build_data.type = BuildData.TYPE_GENERAL_LASTING_WIPE;
				} else if (type_string.equalsIgnoreCase("specific_lasting_wipe")) {
					build_data.type = BuildData.TYPE_SPECIFIC_LASTING_WIPE;
				} else if (type_string.equalsIgnoreCase("air_drop")) {
					build_data.type = BuildData.TYPE_AIR_DROP;
				} else if (type_string.equalsIgnoreCase("recapture")) {
					build_data.type = BuildData.TYPE_RECAPTURE;
				} else if (type_string.equalsIgnoreCase("tower_buster")) {
					build_data.type = BuildData.TYPE_TOWER_BUSTER;
				} else if (type_string.equalsIgnoreCase("shard")) {
					build_data.type = BuildData.TYPE_SHARD;
				} else {
					Output.PrintToScreen("Unknown build type '" + type_string + "' at line " + line_num + ".");
				}

				// Trigger on
				String trigger_on_string = Constants.GetNextTabSeparatedValue(line, place);
				if (trigger_on_string.equalsIgnoreCase("radius_attack")) {
					build_data.trigger_on = BuildData.TRIGGER_ON_RADIUS_ATTACK;
				} else if (trigger_on_string.equalsIgnoreCase("direct_attack")) {
					build_data.trigger_on = BuildData.TRIGGER_ON_DIRECT_ATTACK;
				} else if (trigger_on_string.equalsIgnoreCase("radius_tower")) {
					build_data.trigger_on = BuildData.TRIGGER_ON_RADIUS_TOWER;
				} else if (trigger_on_string.equalsIgnoreCase("radius_attack_empty")) {
					build_data.trigger_on = BuildData.TRIGGER_ON_RADIUS_ATTACK_EMPTY;
				} else if (trigger_on_string.equalsIgnoreCase("none")) {
					build_data.trigger_on = BuildData.TRIGGER_ON_UNDEF;
				} else {
					Output.PrintToScreen("Unknown trigger on value '" + trigger_on_string + "' at line " + line_num + ".");
				}

				// Visible on
				String visible_on_string = Constants.GetNextTabSeparatedValue(line, place);
				if (visible_on_string.equalsIgnoreCase("triggered")) {
					build_data.visible_on = BuildData.VISIBLE_ON_TRIGGERED;
				} else if (visible_on_string.equalsIgnoreCase("attacked")) {
					build_data.visible_on = BuildData.VISIBLE_ON_ATTACKED;
				} else if (visible_on_string.equalsIgnoreCase("always")) {
					build_data.visible_on = BuildData.VISIBLE_ON_ALWAYS;
				} else {
					Output.PrintToScreen("Unknown visible on value '" + visible_on_string + "' at line " + line_num + ".");
				}

				// Attack radius
				value_string = Constants.GetNextTabSeparatedValue(line, place);
				build_data.attack_radius = value_string.isEmpty() ? 0 : Integer.parseInt(value_string);

				if (build_data.attack_radius > MAX_ATTACK_RADIUS) {
					Output.PrintToScreen("Attack radius of " + build_data.attack_radius + " > max of " + MAX_ATTACK_RADIUS + " at line " + line_num + ".");
				}

				// Effect radius
				value_string = Constants.GetNextTabSeparatedValue(line, place);
				build_data.effect_radius = value_string.isEmpty() ? 0 : Integer.parseInt(value_string);

				// Num attacks
				value_string = Constants.GetNextTabSeparatedValue(line, place);
				build_data.num_attacks = value_string.isEmpty() ? 0 : Integer.parseInt(value_string);

				// Attack min HP
				value_string = Constants.GetNextTabSeparatedValue(line, place);
				build_data.attack_min_hp = value_string.isEmpty() ? 0 : Integer.parseInt(value_string);

				// Attack max HP
				value_string = Constants.GetNextTabSeparatedValue(line, place);
				build_data.attack_max_hp = value_string.isEmpty() ? 0 : Integer.parseInt(value_string);

				// Wipe duration
				value_string = Constants.GetNextTabSeparatedValue(line, place);
				build_data.wipe_duration = value_string.isEmpty() ? 0 : Integer.parseInt(value_string);

				// Cooldown time
				value_string = Constants.GetNextTabSeparatedValue(line, place);
				build_data.cooldown_time = value_string.isEmpty() ? 0 : Integer.parseInt(value_string);

				// Capacity
				value_string = Constants.GetNextTabSeparatedValue(line, place);
				build_data.capacity = value_string.isEmpty() ? 0 : Integer.parseInt(value_string);

				// XP per hour
				value_string = Constants.GetNextTabSeparatedValue(line, place);
				build_data.xp_per_hour = value_string.isEmpty() ? 0 : Integer.parseInt(value_string);

				// Flank nullifies
				String flank_nullifies_string = Constants.GetNextTabSeparatedValue(line, place);
				if (flank_nullifies_string.equalsIgnoreCase("true")) {
					build_data.flank_nullifies = true;
				} else {
					build_data.flank_nullifies = false;
				}

				// Max Count
				value_string = Constants.GetNextTabSeparatedValue(line, place);
				build_data.max_count = value_string.isEmpty() ? -1 : Integer.parseInt(value_string);

				// Description
				build_data.description = Constants.GetNextTabSeparatedValue(line, place);

				if (build_data.ID < 0)
				{
					Output.PrintToScreen("Build with missing ID at line " + line_num);
					return false;
				}

				// Add this build to the builds vector and build_name_map.
				builds.put(build_data.ID, build_data);
				build_name_map.put(build_data.name, build_data.ID);

				// If this is an initial build, add it to the initial_builds list.
				if (build_data.initial) {
					initial_builds.add(build_data.ID);
				}
			}
		}
		catch (Exception e)
		{
			Output.PrintToScreen("Error loading builds at line " + line_num);
			Output.PrintException(e);
			return false;
		}

		BuildData build_data;
		Iterator it = builds.entrySet().iterator();
    while (it.hasNext())
		{
			Map.Entry pair = (Map.Entry)it.next();
			build_data = (BuildData)(pair.getValue());

			//Output.PrintToScreen("Build " + build_data.name);

			if (build_data.upgrades_name.equals(""))
			{
				build_data.upgrades = -1;
			}
			else
			{
				build_data.upgrades = GetNameToIDMap(build_data.upgrades_name);

				if (build_data.upgrades < 0)
				{
					Output.PrintToScreen("\'" + build_data.name + "\' upgrades name \'" + build_data.upgrades_name + "\' not a valid build.");
					return false;
				}
			}
    }

		return (builds.size() > 0);
	}
}
