using UnityEngine;
using System;
using System.Linq;
using System.Collections;
using System.Collections.Generic;
using System.IO;

public class TechData : FileData
{
    public enum Duration
    {
        UNDEF           = -1,
	    PERMANENT       = 0,
	    TEMPORARY       = 1,
	    OBJECT          = 2
    }

    public enum Category
    {
        UNDEF     = -1,
	    TECH      = 0,
	    BIO       = 1,
	    PSI       = 2,
        TECH_BUY  = 3,
        BIO_BUY   = 4,
        PSI_BUY   = 5,
        BASE_BUY  = 6
    }

    public enum Bonus
    {
        UNDEF                 = -1,
	    TECH                  = 0,
	    BIO                   = 1,
	    PSI                   = 2,
	    MANPOWER_RATE         = 3,
	    MANPOWER_MAX          = 4,
	    ENERGY_RATE           = 5,
	    ENERGY_MAX            = 6,
        GEO_EFFICIENCY        = 7,
	    XP_MULTIPLIER         = 8,
        HP_PER_SQUARE         = 9,
	    HP_RESTORE            = 10,
	    ATTACK_MANPOWER       = 11,
	    SIMULTANEOUS_ACTIONS  = 12,
	    CRIT_CHANCE           = 13,
        SPLASH_DAMAGE         = 14,
	    SALVAGE_VALUE         = 15,
	    WALL_DISCOUNT         = 16,
	    STRUCTURE_DISCOUNT    = 17,
        MAX_ALLIANCES         = 18,
	    INVISIBILITY          = 19,
	    TECH_MULT             = 20,
	    BIO_MULT              = 21,
	    PSI_MULT              = 22,
	    MANPOWER_RATE_MULT    = 23,
	    MANPOWER_MAX_MULT     = 24,
	    ENERGY_RATE_MULT      = 25,
	    ENERGY_MAX_MULT       = 26,
	    HP_PER_SQUARE_MULT    = 27,
	    HP_RESTORE_MULT       = 28,
	    ATTACK_MANPOWER_MULT  = 29,
	    CREDITS               = 30,
        INSURGENCY            = 31,
        TOTAL_DEFENSE         = 32
    }

    public string original_name = "";
	public string name = "";
	public string icon = "";
    public bool initial = false, random = false;
	public string description = "";
	public string prerequisite_tech_1_name = "", prerequisite_tech_2_name = "", new_build_name = "", obsolete_build_name = "", new_object_name = ""; 
	public int ID = -1, duration_time = 0, default_price = 0, min_price = 0, max_price = 0, prerequisite_level = 0, new_build = -1, obsolete_build = -1, new_object = -1;
    public float order = 0f;
	public int prerequisite_tech_1 = -1, prerequisite_tech_2 = -1;
	public Bonus bonus_type_1 = Bonus.UNDEF, bonus_type_2 = Bonus.UNDEF, bonus_type_3 = Bonus.UNDEF;
	private int bonus_val_1 = 0, bonus_val_2 = 0, bonus_val_3 = 0; // Use accessor function to get these values
    private int bonus_val_max_1 = 0, bonus_val_max_2 = 0, bonus_val_max_3 = 0; // Use accessor function to get these values
	public Duration duration_type = Duration.PERMANENT;
	public Category category = Category.TECH;

    public static Dictionary<int, TechData> techs = new Dictionary<int, TechData>();
    public static Dictionary<string, int> tech_name_map = new Dictionary<string, int>();
	public static string version_string = "";
    public static int num_advances = 0;
    public static string fmtLocName = "Technologies/tech_{0}_name";
    public static string fmtLocDesc = "Technologies/tech_{0}_description";

    public static TechData GetTechData(int _ID)
	{
        if (techs.ContainsKey(_ID)) {
            return techs[_ID];
        } else {
            return null;
        }
	}

    public static void UpdateLocalization()
    {
        foreach(var id in techs.Keys)
        {
            techs[id].name = I2.Loc.LocalizationManager.GetTranslation(String.Format(fmtLocName, id));
            techs[id].description = I2.Loc.LocalizationManager.GetTranslation(String.Format(fmtLocDesc, id));
        }
    }

    public static int GetNameToIDMap(string _name)
	{
        if (tech_name_map.ContainsKey(_name)) {
            return tech_name_map[_name];
        } else {
            return -1;
        }
	}

	public static bool LoadTechnologies()
	{
		int line_num = -1;
		TechData tech_data, prereq_tech_data;
		int[] place = new int[1];

        // Return false if the file doesn't exist on the client.
        if (File.Exists(Application.persistentDataPath + "/technologies.tsv") == false) {
            return false;
        }

        // Clear the tech data, in case there was a previous unsuccessful attempt to load it.
        techs.Clear();
        tech_name_map.Clear();

        var lines = File.ReadAllLines(Application.persistentDataPath + "/technologies.tsv");
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

			// Create a TechData object for this line's technology.
			tech_data = new TechData();

			// Start at the beginning of the line.
			place[0] = 0;

            // Name
            tech_data.original_name = tech_data.name = GetNextTabSeparatedValue(line, place);

            // ID
            string ID_string = GetNextTabSeparatedValue(line, place);
			tech_data.ID = (ID_string.Length == 0) ? -1 : Int32.Parse(ID_string);

            // Category
            String category_string = GetNextTabSeparatedValue(line, place);
			if (String.Equals(category_string, "tech", StringComparison.OrdinalIgnoreCase)) {
				tech_data.category = TechData.Category.TECH;
			} else if (String.Equals(category_string, "bio", StringComparison.OrdinalIgnoreCase)) {
				tech_data.category = TechData.Category.BIO;
			} else if (String.Equals(category_string, "psi", StringComparison.OrdinalIgnoreCase)) {
				tech_data.category = TechData.Category.PSI;
            } else if (String.Equals(category_string, "tech_buy", StringComparison.OrdinalIgnoreCase)) {
				tech_data.category = TechData.Category.TECH_BUY;
            } else if (String.Equals(category_string, "bio_buy", StringComparison.OrdinalIgnoreCase)) {
				tech_data.category = TechData.Category.BIO_BUY;
            } else if (String.Equals(category_string, "psi_buy", StringComparison.OrdinalIgnoreCase)) {
				tech_data.category = TechData.Category.PSI_BUY;
            } else if (String.Equals(category_string, "base_buy", StringComparison.OrdinalIgnoreCase)) {
				tech_data.category = TechData.Category.BASE_BUY;
			} else if (String.Equals(category_string, "none", StringComparison.OrdinalIgnoreCase)) {
				tech_data.category = TechData.Category.UNDEF;
			} else {
                tech_data.category = TechData.Category.UNDEF;
				Debug.Log("Unknown category '" + category_string + "' at line " + line_num + ".");
			}

			// Order
			String order_string = GetNextTabSeparatedValue(line, place);
			tech_data.order = (order_string.Length == 0) ? -1 : Single.Parse(order_string);

			// Icon
			tech_data.icon = GetNextTabSeparatedValue(line, place);

   			// Initial
			String initial_string = GetNextTabSeparatedValue(line, place);
            tech_data.initial = (String.Equals(initial_string, "TRUE", StringComparison.OrdinalIgnoreCase));

            // Random
			String random_string = GetNextTabSeparatedValue(line, place);
            tech_data.random = (String.Equals(random_string, "TRUE", StringComparison.OrdinalIgnoreCase));

			// Duration type
			String duration_type_string = GetNextTabSeparatedValue(line, place);
			if (String.Equals(duration_type_string, "permanent", StringComparison.OrdinalIgnoreCase)) {
				tech_data.duration_type = TechData.Duration.PERMANENT;
			} else if (String.Equals(duration_type_string, "temporary", StringComparison.OrdinalIgnoreCase)) {
				tech_data.duration_type = TechData.Duration.TEMPORARY;
			} else if (String.Equals(duration_type_string, "object", StringComparison.OrdinalIgnoreCase)) {
				tech_data.duration_type = TechData.Duration.OBJECT;
			} else {
				Debug.Log("Unknown duration type '" + duration_type_string + "' at line " + line_num + ".");
			}

			// Duration time
			String duration_time_string = GetNextTabSeparatedValue(line, place);
			tech_data.duration_time = (duration_time_string.Length == 0) ? 0 : Int32.Parse(duration_time_string);

			// Default price
			String default_price_string = GetNextTabSeparatedValue(line, place);
			tech_data.default_price = (default_price_string.Length == 0) ? 0 : Int32.Parse(default_price_string);

   			// Min price
			String min_price_string = GetNextTabSeparatedValue(line, place);
			tech_data.min_price = (min_price_string.Length == 0) ? 0 : Int32.Parse(min_price_string);

   			// Max price
			String max_price_string = GetNextTabSeparatedValue(line, place);
			tech_data.max_price = (max_price_string.Length == 0) ? 0 : Int32.Parse(max_price_string);

			// Prerequisite tech 1
			tech_data.prerequisite_tech_1_name = GetNextTabSeparatedValue(line, place);

			// Prerequisite tech 2
			tech_data.prerequisite_tech_2_name = GetNextTabSeparatedValue(line, place);

			// Prerequisite level
			String prerequisite_level_string = GetNextTabSeparatedValue(line, place);
			tech_data.prerequisite_level = (prerequisite_level_string.Length == 0) ? 0 : Int32.Parse(prerequisite_level_string);

			// Bonus type 1
			String bonus_type_1_string = GetNextTabSeparatedValue(line, place);
			tech_data.bonus_type_1 = ParseBonusType(bonus_type_1_string, line_num);

			// Bonus value 1
			String bonus_value_1_string = GetNextTabSeparatedValue(line, place);
			tech_data.bonus_val_1 = (bonus_value_1_string.Length == 0) ? 0 : Int32.Parse(bonus_value_1_string);

            // Bonus value max 1
			String bonus_value_max_1_string = GetNextTabSeparatedValue(line, place);
			tech_data.bonus_val_max_1 = (bonus_value_max_1_string.Length == 0) ? 0 : Int32.Parse(bonus_value_max_1_string);

			// Bonus type 2
			String bonus_type_2_string = GetNextTabSeparatedValue(line, place);
			tech_data.bonus_type_2 = ParseBonusType(bonus_type_2_string, line_num);

			// Bonus value 2
			String bonus_value_2_string = GetNextTabSeparatedValue(line, place);
			tech_data.bonus_val_2 = (bonus_value_2_string.Length == 0) ? 0 : Int32.Parse(bonus_value_2_string);

            // Bonus value max 2
			String bonus_value_max_2_string = GetNextTabSeparatedValue(line, place);
			tech_data.bonus_val_max_2 = (bonus_value_max_2_string.Length == 0) ? 0 : Int32.Parse(bonus_value_max_2_string);

   			// Bonus type 3
			String bonus_type_3_string = GetNextTabSeparatedValue(line, place);
			tech_data.bonus_type_3 = ParseBonusType(bonus_type_3_string, line_num);

			// Bonus value 3
			String bonus_value_3_string = GetNextTabSeparatedValue(line, place);
			tech_data.bonus_val_3 = (bonus_value_3_string.Length == 0) ? 0 : Int32.Parse(bonus_value_3_string);

            // Bonus value max 3
			String bonus_value_max_3_string = GetNextTabSeparatedValue(line, place);
			tech_data.bonus_val_max_3 = (bonus_value_max_3_string.Length == 0) ? 0 : Int32.Parse(bonus_value_max_3_string);

            // New build
			tech_data.new_build_name = GetNextTabSeparatedValue(line, place);
			tech_data.new_build = BuildData.GetNameToIDMap(tech_data.new_build_name);

			if ((tech_data.new_build_name.Length > 0) && (tech_data.new_build == -1)) 
			{
				Debug.Log("Technology '" + tech_data.name + "' has invalid new build '" + tech_data.new_build_name + "' at line " + line_num);
                techs.Clear();
                tech_name_map.Clear();
				return false;
			}

            if (tech_data.new_build != -1)
            {
                BuildData build_data = BuildData.GetBuildData(tech_data.new_build);
                build_data.required_advance = tech_data.ID;
            }

            // Obsolete build
			tech_data.obsolete_build_name = GetNextTabSeparatedValue(line, place);
			tech_data.obsolete_build = BuildData.GetNameToIDMap(tech_data.obsolete_build_name);

			if ((tech_data.obsolete_build_name.Length > 0) && (tech_data.obsolete_build == -1)) 
			{
				Debug.Log("Technology '" + tech_data.name + "' has invalid obsolete build '" + tech_data.obsolete_build_name + "' at line " + line_num);
                techs.Clear();
                tech_name_map.Clear();
				return false;
			}

            // Obsolete tech 1
			GetNextTabSeparatedValue(line, place);

            // Description
            tech_data.description = GetNextTabSeparatedValue(line, place);

            if (tech_data.ID < 0) 
			{
				Debug.Log("Technology with missing ID at line " + line_num);
                techs.Clear();
                tech_name_map.Clear();
                return false;
			}

            if (tech_name_map.ContainsKey(tech_data.name)) 
            {
                Debug.Log("Technology '" + tech_data.name + "' is duplicated at line " + line_num);
                techs.Clear();
                tech_name_map.Clear();
                return false;
            }

            // Keep track of the number of attainable advances (that is, do not include initial techs or landscape object techs).
            if ((tech_data.initial == false) && (tech_data.duration_type != Duration.OBJECT)) {
                num_advances++;
            }

			// Add this technology to the techs vector and tech_name_map.
			techs.Add(tech_data.ID, tech_data);
			tech_name_map.Add(tech_data.name, tech_data.ID);
		}

        foreach(KeyValuePair<int, TechData> entry in techs)
		{
            tech_data = entry.Value;
			//Debug.Log("Tech " + tech_data.name + ", tech_data.prerequisite_tech_1_name: " + tech_data.prerequisite_tech_1_name + ", tech_data.prerequisite_tech_2_name: " + tech_data.prerequisite_tech_2_name);

			if (tech_data.prerequisite_tech_1_name.Length == 0)
			{
				tech_data.prerequisite_tech_1 = -1;
			}
			else 
			{
				tech_data.prerequisite_tech_1 = GetNameToIDMap(tech_data.prerequisite_tech_1_name);

				if (tech_data.prerequisite_tech_1 < 0) 
				{
					Debug.Log("\'" + tech_data.name + "\' prerequisite 1 name \'" + tech_data.prerequisite_tech_1_name + "\' not a valid technology.");
					return false;
				}
			}

			if (tech_data.prerequisite_tech_2_name.Length == 0)
			{
				tech_data.prerequisite_tech_2 = -1;
			}
			else 
			{
				tech_data.prerequisite_tech_2 = GetNameToIDMap(tech_data.prerequisite_tech_2_name);

				if (tech_data.prerequisite_tech_2 < 0) 
				{
					Debug.Log("\'" + tech_data.name + "\' prerequisite 2 name \'" + tech_data.prerequisite_tech_2_name + "\' not a valid technology.");
					return false;
				}
			}

            // If this is a landscape object technology with a prerequisite, record in the prerequisite tech that it enables this object. 
            if ((tech_data.duration_type == Duration.OBJECT) && (tech_data.prerequisite_tech_1 != -1))
            {
                prereq_tech_data = TechData.GetTechData(tech_data.prerequisite_tech_1);
                prereq_tech_data.new_object = tech_data.ID;
                prereq_tech_data.new_object_name = tech_data.name;
            }
        }

        UpdateLocalization();

        return (techs.Count > 0);		
	}
        
	public static Bonus ParseBonusType(String _string, int _line_num)
	{
		if (String.Equals(_string, "", StringComparison.OrdinalIgnoreCase)) {
			return Bonus.UNDEF;
		} else if (String.Equals(_string, "tech", StringComparison.OrdinalIgnoreCase)) {
			return Bonus.TECH;
		} else if (String.Equals(_string, "bio", StringComparison.OrdinalIgnoreCase)) {
			return Bonus.BIO;
		} else if (String.Equals(_string, "psi", StringComparison.OrdinalIgnoreCase)) {
			return Bonus.PSI;
		} else if (String.Equals(_string, "manpower_rate", StringComparison.OrdinalIgnoreCase)) {
			return Bonus.MANPOWER_RATE;
		} else if (String.Equals(_string, "manpower_max", StringComparison.OrdinalIgnoreCase)) {
			return Bonus.MANPOWER_MAX;
		} else if (String.Equals(_string, "energy_rate", StringComparison.OrdinalIgnoreCase)) {
			return Bonus.ENERGY_RATE;
		} else if (String.Equals(_string, "energy_max", StringComparison.OrdinalIgnoreCase)) {
			return Bonus.ENERGY_MAX;
		} else if (String.Equals(_string, "geo_efficiency", StringComparison.OrdinalIgnoreCase)) {
			return Bonus.GEO_EFFICIENCY;
		} else if (String.Equals(_string, "xp_multiplier", StringComparison.OrdinalIgnoreCase)) {
			return Bonus.XP_MULTIPLIER;
		} else if (String.Equals(_string, "hp_per_square", StringComparison.OrdinalIgnoreCase)) {
			return Bonus.HP_PER_SQUARE;
		} else if (String.Equals(_string, "hp_restore", StringComparison.OrdinalIgnoreCase)) {
			return Bonus.HP_RESTORE;
		} else if (String.Equals(_string, "attack_manpower", StringComparison.OrdinalIgnoreCase)) {
			return Bonus.ATTACK_MANPOWER;
		} else if (String.Equals(_string, "simultaneous_actions", StringComparison.OrdinalIgnoreCase)) {
			return Bonus.SIMULTANEOUS_ACTIONS;
		} else if (String.Equals(_string, "crit_chance", StringComparison.OrdinalIgnoreCase)) {
			return Bonus.CRIT_CHANCE;
		} else if (String.Equals(_string, "salvage_value", StringComparison.OrdinalIgnoreCase)) {
			return Bonus.SALVAGE_VALUE;
		} else if (String.Equals(_string, "wall_discount", StringComparison.OrdinalIgnoreCase)) {
			return Bonus.WALL_DISCOUNT;
		} else if (String.Equals(_string, "structure_discount", StringComparison.OrdinalIgnoreCase)) {
			return Bonus.STRUCTURE_DISCOUNT;
		} else if (String.Equals(_string, "max_alliances", StringComparison.OrdinalIgnoreCase)) {
			return Bonus.MAX_ALLIANCES;
		} else if (String.Equals(_string, "splash_damage", StringComparison.OrdinalIgnoreCase)) {
			return Bonus.SPLASH_DAMAGE;
		} else if (String.Equals(_string, "max_alliances", StringComparison.OrdinalIgnoreCase)) {
			return Bonus.MAX_ALLIANCES;
		} else if (String.Equals(_string, "invisibility", StringComparison.OrdinalIgnoreCase)) {
			return Bonus.INVISIBILITY;
        } else if (String.Equals(_string, "tech_mult", StringComparison.OrdinalIgnoreCase)) {
			return Bonus.TECH_MULT;
        } else if (String.Equals(_string, "bio_mult", StringComparison.OrdinalIgnoreCase)) {
			return Bonus.BIO_MULT;
        } else if (String.Equals(_string, "psi_mult", StringComparison.OrdinalIgnoreCase)) {
			return Bonus.PSI_MULT;
        } else if (String.Equals(_string, "manpower_rate_mult", StringComparison.OrdinalIgnoreCase)) {
			return Bonus.MANPOWER_RATE_MULT;
        } else if (String.Equals(_string, "manpower_max_mult", StringComparison.OrdinalIgnoreCase)) {
			return Bonus.MANPOWER_MAX_MULT;
        } else if (String.Equals(_string, "energy_rate_mult", StringComparison.OrdinalIgnoreCase)) {
			return Bonus.ENERGY_RATE_MULT;
        } else if (String.Equals(_string, "energy_max_mult", StringComparison.OrdinalIgnoreCase)) {
			return Bonus.ENERGY_MAX_MULT;
        } else if (String.Equals(_string, "hp_per_square_mult", StringComparison.OrdinalIgnoreCase)) {
			return Bonus.HP_PER_SQUARE_MULT;
        } else if (String.Equals(_string, "hp_restore_mult", StringComparison.OrdinalIgnoreCase)) {
			return Bonus.HP_RESTORE_MULT;
        } else if (String.Equals(_string, "attack_manpower_mult", StringComparison.OrdinalIgnoreCase)) {
			return Bonus.ATTACK_MANPOWER_MULT;
        } else if (String.Equals(_string, "credits", StringComparison.OrdinalIgnoreCase)) {
			return Bonus.CREDITS;
        } else if (String.Equals(_string, "insurgency", StringComparison.OrdinalIgnoreCase)) {
			return Bonus.INSURGENCY;
        } else if (String.Equals(_string, "total_defense", StringComparison.OrdinalIgnoreCase)) {
			return Bonus.TOTAL_DEFENSE;
		} else {
			Debug.Log("Unknown bonus type at line " + _line_num);
			return Bonus.UNDEF;
		}
	}

    static public int GetNumAdvances()
    {
        return num_advances;
    }

    public int GetBonusVal(int _index)
    {
        switch (_index)
        {
            case 1:
                if ((bonus_type_1 == Bonus.MANPOWER_RATE) || (bonus_type_1 == Bonus.MANPOWER_MAX)) {
                    return (int)(bonus_val_1 * GameData.instance.manpowerGenMultiplier);
                } else {
                    return bonus_val_1;
                }
            case 2:
                if ((bonus_type_2 == Bonus.MANPOWER_RATE) || (bonus_type_2 == Bonus.MANPOWER_MAX)) {
                    return (int)(bonus_val_2 * GameData.instance.manpowerGenMultiplier);
                } else {
                    return bonus_val_2;
                }
            case 3:
                if ((bonus_type_3 == Bonus.MANPOWER_RATE) || (bonus_type_3 == Bonus.MANPOWER_MAX)) {
                    return (int)(bonus_val_3 * GameData.instance.manpowerGenMultiplier);
                } else {
                    return bonus_val_3;
                }
            default:
                return 0;
        }
    }

    public int GetBonusValMax(int _index)
    {
        switch (_index)
        {
            case 1:
                if ((bonus_type_1 == Bonus.MANPOWER_RATE) || (bonus_type_1 == Bonus.MANPOWER_MAX)) {
                    return (int)(bonus_val_max_1 * GameData.instance.manpowerGenMultiplier);
                } else {
                    return bonus_val_max_1;
                }
            case 2:
                if ((bonus_type_2 == Bonus.MANPOWER_RATE) || (bonus_type_2 == Bonus.MANPOWER_MAX)) {
                    return (int)(bonus_val_max_2 * GameData.instance.manpowerGenMultiplier);
                } else {
                    return bonus_val_max_2;
                }
            case 3:
                if ((bonus_type_3 == Bonus.MANPOWER_RATE) || (bonus_type_3 == Bonus.MANPOWER_MAX)) {
                    return (int)(bonus_val_max_3 * GameData.instance.manpowerGenMultiplier);
                } else {
                    return bonus_val_max_3;
                }
            default:
                return 0;
        }
    }
}
