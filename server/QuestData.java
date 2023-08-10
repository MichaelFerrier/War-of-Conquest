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

public class QuestData
{
	public static final int CRITERIA_UNDEF												= -1;
	public static final int CRITERIA_NUM_UPGRADES									= 0;
	public static final int CRITERIA_BUILD												= 1;
	public static final int CRITERIA_DISCOVER											= 2;
	public static final int CRITERIA_ENERGY_SUPPLY_LINE						= 3;
	public static final int CRITERIA_MANPOWER_SUPPLY_LINE					= 4;
	public static final int CRITERIA_SALVAGE_BUILD								= 5;
	public static final int CRITERIA_SALVAGE_WALL									= 6;
	public static final int CRITERIA_BUILD_ACTIVATE								= 7;
	public static final int CRITERIA_STOLEN_ENERGY								= 8;
	public static final int CRITERIA_STOLEN_MANPOWER							= 9;
	public static final int CRITERIA_CAPTURE_LAND_COUNTER_ATTACK	= 10;
	public static final int CRITERIA_CAPTURE_LAND									= 11;
	public static final int CRITERIA_OCCUPY_LAND									= 12;
	public static final int CRITERIA_SUM_AREA											= 13;
	public static final int CRITERIA_FORM_ALLIANCE								= 14;
	public static final int CRITERIA_CAPTURE_WALL									= 15;
	public static final int CRITERIA_CAPTURE_BUILD_CATEGORY				= 16;
	public static final int CRITERIA_CAPTURE_BUILD								= 17;
	public static final int CRITERIA_CAPTURE_RESOURCE_NUM					= 18;
	public static final int CRITERIA_CAPTURE_OBJECT								= 19;
	public static final int CRITERIA_DONATE_ENERGY								= 20;
	public static final int CRITERIA_CAPTURE_ORB_NUM							= 21;
	public static final int CRITERIA_WINNINGS											= 22;
	public static final int CRITERIA_MEMBERS_ONLINE								= 23;
	public static final int CRITERIA_GEO_EFFICIENCY								= 24;
	public static final int CRITERIA_CAPTURE_BY_FLANKING					= 25;
	public static final int CRITERIA_NUM_RESOURCES								= 26;
	public static final int CRITERIA_NUM_ORBS											= 27;
	public static final int CRITERIA_DONATE_MANPOWER							= 28;

	public static final int MAX_NUM_STAGES = 3;

	String name = "";
	String description = "";
	int ID = -1;
	int num_stages = 0;
	int criteria = CRITERIA_UNDEF, criteria_subject = -1;
	int criteria_amount[] = new int[MAX_NUM_STAGES];
	int reward_credits[] = new int[MAX_NUM_STAGES];
	int reward_xp[] = new int[MAX_NUM_STAGES];

	public static HashMap<Integer,QuestData> quests = new HashMap<Integer,QuestData>();
	public static HashMap<String,Integer> quest_name_map = new HashMap<String,Integer>();
	public static String version_string = "";
	public static int highest_id = -1;
	public static QuestData quests_array[];

	public static QuestData GetQuestData(int _ID)
	{
		return quests.getOrDefault(_ID, null);
	}

	public static int GetNameToIDMap(String _name)
	{
		return quest_name_map.getOrDefault(_name, -1);
	}

	public static Boolean LoadQuests()
	{
		int line_num = -1, i;
		String value_string;
		BufferedReader br;
		String [] string_array;
		String filename = "quests.tsv";

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
			QuestData quest_data;
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

				// Create a QuestData object for this line's build.
				quest_data = new QuestData();

				// Start at the beginning of the line.
				place[0] = 0;

				// Name
				quest_data.name = Constants.GetNextTabSeparatedValue(line, place);

				// ID
				value_string = Constants.GetNextTabSeparatedValue(line, place);
				quest_data.ID = value_string.isEmpty() ? -1 : Integer.parseInt(value_string);

				// Criteria
				String criteria_string = Constants.GetNextTabSeparatedValue(line, place);
				if (criteria_string.equalsIgnoreCase("num_upgrades")) {
					quest_data.criteria = QuestData.CRITERIA_NUM_UPGRADES;
				} else if (criteria_string.equalsIgnoreCase("build")) {
					quest_data.criteria = QuestData.CRITERIA_BUILD;
				} else if (criteria_string.equalsIgnoreCase("discover")) {
					quest_data.criteria = QuestData.CRITERIA_DISCOVER;
				} else if (criteria_string.equalsIgnoreCase("energy_supply_line")) {
					quest_data.criteria = QuestData.CRITERIA_ENERGY_SUPPLY_LINE;
				} else if (criteria_string.equalsIgnoreCase("manpower_supply_line")) {
					quest_data.criteria = QuestData.CRITERIA_MANPOWER_SUPPLY_LINE;
				} else if (criteria_string.equalsIgnoreCase("salvage_build")) {
					quest_data.criteria = QuestData.CRITERIA_SALVAGE_BUILD;
				} else if (criteria_string.equalsIgnoreCase("salvage_wall")) {
					quest_data.criteria = QuestData.CRITERIA_SALVAGE_WALL;
				} else if (criteria_string.equalsIgnoreCase("build_activate")) {
					quest_data.criteria = QuestData.CRITERIA_BUILD_ACTIVATE;
				} else if (criteria_string.equalsIgnoreCase("stolen_energy")) {
					quest_data.criteria = QuestData.CRITERIA_STOLEN_ENERGY;
				} else if (criteria_string.equalsIgnoreCase("stolen_manpower")) {
					quest_data.criteria = QuestData.CRITERIA_STOLEN_MANPOWER;
				} else if (criteria_string.equalsIgnoreCase("capture_land_counter_attack")) {
					quest_data.criteria = QuestData.CRITERIA_CAPTURE_LAND_COUNTER_ATTACK;
				} else if (criteria_string.equalsIgnoreCase("capture_land")) {
					quest_data.criteria = QuestData.CRITERIA_CAPTURE_LAND;
				} else if (criteria_string.equalsIgnoreCase("occupy_land")) {
					quest_data.criteria = QuestData.CRITERIA_OCCUPY_LAND;
				} else if (criteria_string.equalsIgnoreCase("sum_area")) {
					quest_data.criteria = QuestData.CRITERIA_SUM_AREA;
				} else if (criteria_string.equalsIgnoreCase("form_alliance")) {
					quest_data.criteria = QuestData.CRITERIA_FORM_ALLIANCE;
				} else if (criteria_string.equalsIgnoreCase("capture_wall")) {
					quest_data.criteria = QuestData.CRITERIA_CAPTURE_WALL;
				} else if (criteria_string.equalsIgnoreCase("capture_build_category")) {
					quest_data.criteria = QuestData.CRITERIA_CAPTURE_BUILD_CATEGORY;
				} else if (criteria_string.equalsIgnoreCase("capture_build")) {
					quest_data.criteria = QuestData.CRITERIA_CAPTURE_BUILD;
				} else if (criteria_string.equalsIgnoreCase("capture_resource_num")) {
					quest_data.criteria = QuestData.CRITERIA_CAPTURE_RESOURCE_NUM;
				} else if (criteria_string.equalsIgnoreCase("capture_object")) {
					quest_data.criteria = QuestData.CRITERIA_CAPTURE_OBJECT;
				} else if (criteria_string.equalsIgnoreCase("donate_energy")) {
					quest_data.criteria = QuestData.CRITERIA_DONATE_ENERGY;
				} else if (criteria_string.equalsIgnoreCase("capture_orb_num")) {
					quest_data.criteria = QuestData.CRITERIA_CAPTURE_ORB_NUM;
				} else if (criteria_string.equalsIgnoreCase("winnings")) {
					quest_data.criteria = QuestData.CRITERIA_WINNINGS;
				} else if (criteria_string.equalsIgnoreCase("members_online")) {
					quest_data.criteria = QuestData.CRITERIA_MEMBERS_ONLINE;
				} else if (criteria_string.equalsIgnoreCase("geo_efficiency")) {
					quest_data.criteria = QuestData.CRITERIA_GEO_EFFICIENCY;
				} else if (criteria_string.equalsIgnoreCase("capture_by_flanking")) {
					quest_data.criteria = QuestData.CRITERIA_CAPTURE_BY_FLANKING;
				} else if (criteria_string.equalsIgnoreCase("num_resources")) {
					quest_data.criteria = QuestData.CRITERIA_NUM_RESOURCES;
				} else if (criteria_string.equalsIgnoreCase("num_orbs")) {
					quest_data.criteria = QuestData.CRITERIA_NUM_ORBS;
				} else if (criteria_string.equalsIgnoreCase("donate_manpower")) {
					quest_data.criteria = QuestData.CRITERIA_DONATE_MANPOWER;
				} else {
					Output.PrintToScreen("Unknown quest criteria '" + criteria_string + "' at line " + line_num + ".");
				}

				// Criteria subject
				String criteria_subject_string = Constants.GetNextTabSeparatedValue(line, place);
				switch (quest_data.criteria)
				{
					case CRITERIA_DISCOVER:
						quest_data.criteria_subject = TechData.GetNameToIDMap(criteria_subject_string);
						if (quest_data.criteria_subject == -1) Output.PrintToScreen("Unknown quest subject '" + criteria_subject_string + "' (should be a technology) at line " + line_num + ".");
						break;
					case CRITERIA_BUILD_ACTIVATE:
					case CRITERIA_BUILD:
					case CRITERIA_CAPTURE_BUILD:
						quest_data.criteria_subject = BuildData.GetNameToIDMap(criteria_subject_string);
						if (quest_data.criteria_subject == -1) Output.PrintToScreen("Unknown quest subject '" + criteria_subject_string + "' (should be a build) at line " + line_num + ".");
						break;
					case CRITERIA_CAPTURE_BUILD_CATEGORY:
						if (criteria_subject_string.equalsIgnoreCase("tech")) {
							quest_data.criteria_subject = Constants.STAT_TECH;
						} else if (criteria_subject_string.equalsIgnoreCase("bio")) {
							quest_data.criteria_subject = Constants.STAT_BIO;
						} else if (criteria_subject_string.equalsIgnoreCase("psi")) {
							quest_data.criteria_subject = Constants.STAT_PSI;
						} else {
							Output.PrintToScreen("Unknown tech stat '" + criteria_subject_string + "' at line " + line_num + ". Should be tech, bio or psi.");
						}
						break;
					case CRITERIA_CAPTURE_OBJECT:
						quest_data.criteria_subject = ObjectData.GetNameToIDMap(criteria_subject_string);
						if (quest_data.criteria_subject == -1) Output.PrintToScreen("Unknown quest subject '" + criteria_subject_string + "' (should be object) at line " + line_num + ".");
						break;
				}

				// Criteria amount
				value_string = Constants.GetNextTabSeparatedValue(line, place);
				string_array = value_string.split("\\s*,\\s*");
				for (i = 0; i < Math.min(MAX_NUM_STAGES, string_array.length); i++) {
					quest_data.criteria_amount[i] = Integer.parseInt(string_array[i]);
				}

				// Record the quest's number of stages.
				quest_data.num_stages = Math.min(MAX_NUM_STAGES, string_array.length);

				// Reward credits
				value_string = Constants.GetNextTabSeparatedValue(line, place);
				string_array = value_string.split("\\s*,\\s*");
				for (i = 0; i < Math.min(MAX_NUM_STAGES, string_array.length); i++) {
					quest_data.reward_credits[i] = Integer.parseInt(string_array[i]);
				}

				// Reward XP
				value_string = Constants.GetNextTabSeparatedValue(line, place);
				string_array = value_string.split("\\s*,\\s*");
				for (i = 0; i < Math.min(MAX_NUM_STAGES, string_array.length); i++) {
					quest_data.reward_xp[i] = Integer.parseInt(string_array[i]);
				}

				// Description
				quest_data.description = Constants.GetNextTabSeparatedValue(line, place);

				if (quest_data.ID < 0)
				{
					Output.PrintToScreen("Quest with missing ID at line " + line_num);
					return false;
				}

				// Add this quest to the quests vector and quest_name_map.
				quests.put(quest_data.ID, quest_data);
				quest_name_map.put(quest_data.name, quest_data.ID);

				// Keep track of highest ID
				highest_id = Math.max(highest_id, quest_data.ID);
			}

			// Fill the quests_array for fast iteration of all quests.
			quests_array = new QuestData[highest_id + 1];
			for (QuestData value : quests.values()) {
				quests_array[value.ID] = value;
			}
		}
		catch (Exception e)
		{
			Output.PrintToScreen("Error loading quests at line " + line_num);
			Output.PrintException(e);
			return false;
		}

		return (quests.size() > 0);
	}
}
