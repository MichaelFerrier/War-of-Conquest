using UnityEngine;
using System;
using System.Collections;
using System.Collections.Generic;
using System.IO;

public class QuestData : FileData 
{
    public enum Criteria
    {
        UNDEF									= -1,
        NUM_UPGRADES							= 0,
        BUILD									= 1,
        DISCOVER								= 2,
        ENERGY_SUPPLY_LINE						= 3,
        MANPOWER_SUPPLY_LINE					= 4,
        SALVAGE_BUILD							= 5,
        SALVAGE_WALL							= 6,
        BUILD_ACTIVATE							= 7,
        STOLEN_ENERGY							= 8,
        STOLEN_MANPOWER							= 9,
        CAPTURE_LAND_COUNTER_ATTACK	            = 10,
        CAPTURE_LAND							= 11,
        OCCUPY_LAND								= 12,
        SUM_AREA								= 13,
        FORM_ALLIANCE							= 14,
        CAPTURE_WALL							= 15,
        CAPTURE_BUILD_CATEGORY				    = 16,
        CAPTURE_BUILD							= 17,
        CAPTURE_RESOURCE_NUM					= 18,
        CAPTURE_OBJECT							= 19,
        DONATE_ENERGY							= 20,
        CAPTURE_ORB_NUM							= 21,
        WINNINGS								= 22,
        MEMBERS_ONLINE							= 23,
        GEO_EFFICIENCY							= 24,
        CAPTURE_BY_FLANKING					    = 25,
        NUM_RESOURCES							= 26,
        NUM_ORBS                                = 27,
        DONATE_MANPOWER							= 28
    }

    public const int MAX_NUM_STAGES = 3;

    public string original_name = "";
    public string name = "";
	public string description = "";
	public int ID = -1;
    public int num_stages = 0;
    public Criteria criteria = Criteria.UNDEF;
    public int criteria_subject = -1;
    public int[] criteria_amount = new int[MAX_NUM_STAGES];
	public int[] reward_credits = new int[MAX_NUM_STAGES];
	public int[] reward_xp = new int[MAX_NUM_STAGES];
        
  	public static Dictionary<int,QuestData> quests = new Dictionary<int, QuestData>();
	public static Dictionary<string,int> quest_name_map = new Dictionary<string, int>();
	public static string version_string = "";
    public static string fmtLocName = "Quests/quest_{0}_name";
    public static string fmtLocDesc = "Quests/quest_{0}_description";

    public static QuestData GetQuestData(int _ID)
	{
        if (quests.ContainsKey(_ID)) {
            return quests[_ID];
        } else {
            return null;
        }
	}

	public static int GetNameToIDMap(string _name)
	{
        if (quest_name_map.ContainsKey(_name)) {
            return quest_name_map[_name];
        } else {
            return -1;
        }
	}

    public static void UpdateLocalization()
    {
        foreach (var id in quests.Keys)
        {
            quests[id].name = I2.Loc.LocalizationManager.GetTranslation(String.Format(fmtLocName, id));
            quests[id].description = I2.Loc.LocalizationManager.GetTranslation(String.Format(fmtLocDesc, id));
        }
    }

    public static bool LoadQuests()
	{
		int line_num = -1, i;
		QuestData quest_data;
		int[] place = new int[1];
        String value_string;
        String[] string_array;

        Debug.Log("QuestData.LoadQuests() location: " + Application.persistentDataPath + "/quests.tsv");

        // Return false if the file doesn't exist on the client.
        if (File.Exists(Application.persistentDataPath + "/quests.tsv") == false) {
            return false;
        }

        // Clear data from any previous load.
        quests.Clear();
        quest_name_map.Clear();

        var lines = File.ReadAllLines(Application.persistentDataPath + "/quests.tsv");
        foreach (var line in lines)
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
			if (line[0] == '#') {
				continue;
			}

			// Create a QuestData object for this line's quest.
			quest_data = new QuestData();

			// Start at the beginning of the line.
			place[0] = 0;

            // Name
            quest_data.original_name = quest_data.name = GetNextTabSeparatedValue(line, place);

            // ID
            value_string = GetNextTabSeparatedValue(line, place);
			quest_data.ID = (value_string.Length == 0) ? -1 : Int32.Parse(value_string);

            // Criteria
            String criteria_string = GetNextTabSeparatedValue(line, place);
            if (String.Equals(criteria_string, "num_upgrades", StringComparison.OrdinalIgnoreCase)) {
				quest_data.criteria = QuestData.Criteria.NUM_UPGRADES;
            } else if (String.Equals(criteria_string, "build", StringComparison.OrdinalIgnoreCase)) {
				quest_data.criteria = QuestData.Criteria.BUILD;
            } else if (String.Equals(criteria_string, "discover", StringComparison.OrdinalIgnoreCase)) {
				quest_data.criteria = QuestData.Criteria.DISCOVER;
            } else if (String.Equals(criteria_string, "energy_supply_line", StringComparison.OrdinalIgnoreCase)) {
				quest_data.criteria = QuestData.Criteria.ENERGY_SUPPLY_LINE;
            } else if (String.Equals(criteria_string, "manpower_supply_line", StringComparison.OrdinalIgnoreCase)) {
				quest_data.criteria = QuestData.Criteria.MANPOWER_SUPPLY_LINE;
            } else if (String.Equals(criteria_string, "salvage_build", StringComparison.OrdinalIgnoreCase)) {
				quest_data.criteria = QuestData.Criteria.SALVAGE_BUILD;
            } else if (String.Equals(criteria_string, "salvage_wall", StringComparison.OrdinalIgnoreCase)) {
				quest_data.criteria = QuestData.Criteria.SALVAGE_WALL;
            } else if (String.Equals(criteria_string, "build_activate", StringComparison.OrdinalIgnoreCase)) {
				quest_data.criteria = QuestData.Criteria.BUILD_ACTIVATE;
            } else if (String.Equals(criteria_string, "stolen_energy", StringComparison.OrdinalIgnoreCase)) {
				quest_data.criteria = QuestData.Criteria.STOLEN_ENERGY;
            } else if (String.Equals(criteria_string, "stolen_manpower", StringComparison.OrdinalIgnoreCase)) {
				quest_data.criteria = QuestData.Criteria.STOLEN_MANPOWER;
            } else if (String.Equals(criteria_string, "capture_land_counter_attack", StringComparison.OrdinalIgnoreCase)) {
				quest_data.criteria = QuestData.Criteria.CAPTURE_LAND_COUNTER_ATTACK;
            } else if (String.Equals(criteria_string, "capture_land", StringComparison.OrdinalIgnoreCase)) {
				quest_data.criteria = QuestData.Criteria.CAPTURE_LAND;
            } else if (String.Equals(criteria_string, "occupy_land", StringComparison.OrdinalIgnoreCase)) {
				quest_data.criteria = QuestData.Criteria.OCCUPY_LAND;
            } else if (String.Equals(criteria_string, "sum_area", StringComparison.OrdinalIgnoreCase)) {
				quest_data.criteria = QuestData.Criteria.SUM_AREA;
            } else if (String.Equals(criteria_string, "form_alliance", StringComparison.OrdinalIgnoreCase)) {
				quest_data.criteria = QuestData.Criteria.FORM_ALLIANCE;
            } else if (String.Equals(criteria_string, "capture_wall", StringComparison.OrdinalIgnoreCase)) {
				quest_data.criteria = QuestData.Criteria.CAPTURE_WALL;
            } else if (String.Equals(criteria_string, "capture_build_category", StringComparison.OrdinalIgnoreCase)) {
				quest_data.criteria = QuestData.Criteria.CAPTURE_BUILD_CATEGORY;
            } else if (String.Equals(criteria_string, "capture_build", StringComparison.OrdinalIgnoreCase)) {
				quest_data.criteria = QuestData.Criteria.CAPTURE_BUILD;
            } else if (String.Equals(criteria_string, "capture_resource_num", StringComparison.OrdinalIgnoreCase)) {
				quest_data.criteria = QuestData.Criteria.CAPTURE_RESOURCE_NUM;
            } else if (String.Equals(criteria_string, "capture_object", StringComparison.OrdinalIgnoreCase)) {
				quest_data.criteria = QuestData.Criteria.CAPTURE_OBJECT;
            } else if (String.Equals(criteria_string, "donate_energy", StringComparison.OrdinalIgnoreCase)) {
				quest_data.criteria = QuestData.Criteria.DONATE_ENERGY;
            } else if (String.Equals(criteria_string, "capture_orb_num", StringComparison.OrdinalIgnoreCase)) {
				quest_data.criteria = QuestData.Criteria.CAPTURE_ORB_NUM;
            } else if (String.Equals(criteria_string, "winnings", StringComparison.OrdinalIgnoreCase)) {
				quest_data.criteria = QuestData.Criteria.WINNINGS;
            } else if (String.Equals(criteria_string, "members_online", StringComparison.OrdinalIgnoreCase)) {
				quest_data.criteria = QuestData.Criteria.MEMBERS_ONLINE;
            } else if (String.Equals(criteria_string, "geo_efficiency", StringComparison.OrdinalIgnoreCase)) {
				quest_data.criteria = QuestData.Criteria.GEO_EFFICIENCY;
            } else if (String.Equals(criteria_string, "capture_by_flanking", StringComparison.OrdinalIgnoreCase)) {
				quest_data.criteria = QuestData.Criteria.CAPTURE_BY_FLANKING;
            } else if (String.Equals(criteria_string, "num_resources", StringComparison.OrdinalIgnoreCase)) {
				quest_data.criteria = QuestData.Criteria.NUM_RESOURCES;
            } else if (String.Equals(criteria_string, "num_orbs", StringComparison.OrdinalIgnoreCase)) {
				quest_data.criteria = QuestData.Criteria.NUM_ORBS;
            } else if (String.Equals(criteria_string, "donate_manpower", StringComparison.OrdinalIgnoreCase)) {
				quest_data.criteria = QuestData.Criteria.DONATE_MANPOWER;
			} else {
                Debug.Log("Unknown quest criteria '" + criteria_string + "' at line " + line_num + ".");
			}

            // Criteria subject
  			String criteria_subject_string = GetNextTabSeparatedValue(line, place);
			switch (quest_data.criteria)
			{
				case QuestData.Criteria.DISCOVER:
					quest_data.criteria_subject = TechData.GetNameToIDMap(criteria_subject_string);
					if (quest_data.criteria_subject == -1) Debug.Log("Unknown quest subject '" + criteria_subject_string + "' (should be a technology) at line " + line_num + ".");
					break;
				case QuestData.Criteria.BUILD_ACTIVATE:
				case QuestData.Criteria.BUILD:
				case QuestData.Criteria.CAPTURE_BUILD:
					quest_data.criteria_subject = BuildData.GetNameToIDMap(criteria_subject_string);
					if (quest_data.criteria_subject == -1) Debug.Log("Unknown quest subject '" + criteria_subject_string + "' (should be a build) at line " + line_num + ".");
					break;
				case QuestData.Criteria.CAPTURE_BUILD_CATEGORY:
                    if (String.Equals(criteria_subject_string, "tech", StringComparison.OrdinalIgnoreCase)) {
						quest_data.criteria_subject = (int)TechData.Category.TECH;
					} else if (String.Equals(criteria_subject_string, "bio", StringComparison.OrdinalIgnoreCase)) {
						quest_data.criteria_subject = (int)TechData.Category.BIO;
					} else if (String.Equals(criteria_subject_string, "psi", StringComparison.OrdinalIgnoreCase)) {
						quest_data.criteria_subject = (int)TechData.Category.PSI;
					} else {
						Debug.Log("Unknown tech stat '" + criteria_subject_string + "' at line " + line_num + ". Should be tech, bio or psi.");
					}
					break;
				case QuestData.Criteria.CAPTURE_OBJECT:
					quest_data.criteria_subject = ObjectData.GetNameToIDMap(criteria_subject_string);
					if (quest_data.criteria_subject == -1) Debug.Log("Unknown quest subject '" + criteria_subject_string + "' (should be object) at line " + line_num + ".");
					break;
			}

            // Criteria amount
			value_string = GetNextTabSeparatedValue(line, place);
            string_array = value_string.Split(',');
			for (i = 0; i < Math.Min(MAX_NUM_STAGES, string_array.Length); i++) {
				quest_data.criteria_amount[i] = Int32.Parse(string_array[i]);
				//Debug.Log("Criteria amount " + i + ": " + quest_data.criteria_amount[i]);
			}

			// Record the quest's number of stages.
			quest_data.num_stages = Math.Min(MAX_NUM_STAGES, string_array.Length);
				
            // Reward credits
			value_string = GetNextTabSeparatedValue(line, place);
            string_array = value_string.Split(',');
			for (i = 0; i < Math.Min(MAX_NUM_STAGES, string_array.Length); i++) {
				quest_data.reward_credits[i] = Int32.Parse(string_array[i]);
				//Debug.Log("Reward credits " + i + ": " + quest_data.reward_credits[i]);
			}

            // Reward XP
			value_string = GetNextTabSeparatedValue(line, place);
            string_array = value_string.Split(',');
			for (i = 0; i < Math.Min(MAX_NUM_STAGES, string_array.Length); i++) {
				quest_data.reward_xp[i] = Int32.Parse(string_array[i]);
				//Debug.Log("Reward XP " + i + ": " + quest_data.reward_xp[i]);
			}

            // Description
            quest_data.description = GetNextTabSeparatedValue(line, place);

            if (quest_data.ID < 0) 
			{
				Debug.Log("Quest with missing ID at line " + line_num);
				return false;
			}

			// Add this quest to the quests vector and quest_name_map.
			quests.Add(quest_data.ID, quest_data);
			quest_name_map.Add(quest_data.name, quest_data.ID);
		}

        UpdateLocalization();

        return (quests.Count > 0);		
	}
}
