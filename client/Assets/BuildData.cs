using UnityEngine;
using System;
using System.Collections;
using System.Collections.Generic;
using System.IO;

public class BuildData : FileData
{
    public enum Type
    {
	    UNDEF                   = -1,
	    WALL                    = 0,
	    ENERGY_STORAGE          = 1,
	    MANPOWER_STORAGE        = 2,
        DUMMY                   = 3,
	    DIRECTED_MULTIPLE       = 4,
	    SPLASH                  = 5,
	    AREA_EFFECT             = 6,
	    AREA_FORTIFICATION      = 7,
	    COUNTER_ATTACK          = 8,
	    WIPE                    = 9,
	    GENERAL_LASTING_WIPE    = 10,
	    SPECIFIC_LASTING_WIPE   = 11,
	    AIR_DROP                = 12,
	    RECAPTURE               = 13,
	    TOWER_BUSTER            = 14,
        SHARD                   = 15
    }

    public enum TriggerOn
    {
    	UNDEF = -1,
	    RADIUS_ATTACK = 0,
	    DIRECT_ATTACK = 1,
	    RADIUS_TOWER = 2,
        RADIUS_ATTACK_EMPTY = 3
    }

    public enum VisibleOn
    {
        UNDEF = -1,
        TRIGGERED = 0,
	    ATTACKED = 1,
	    ALWAYS = 2
    }

   	public const int WIPE_FLAG_GENERAL     = 1;
	public const int WIPE_FLAG_SPECIFIC    = 2;
	public const int WIPE_FLAG_CHEMICAL    = 4;
	public const int WIPE_FLAG_SUPERVIRUS  = 8;
	public const int WIPE_FLAG_HYPONOTIC   = 16;
	public const int WIPE_FLAG_TEMPLE      = 32;

  	public const int LAND_FLAG_MAINLAND      = 1;
	public const int LAND_FLAG_HOMELAND      = 2;
	public const int LAND_FLAG_RAID          = 4;
    public const int LAND_FLAG_ALL           = 7;

    public const int MAX_ATTACK_RADIUS = 5;

    public string original_name = "";
  	public string name = "";
	public string description = "";
	public string upgrades_name = ""; 
	public int ID = -1, upgrades = -1, upgrades_to = -1, land = 0, build_time = 0, hit_points = 0, manpower_cost = 0, energy_burn_rate = 0, max_count = -1, required_advance = -1;
   	public int attack_radius = 0, effect_radius = 0, num_attacks = 0, attack_min_hp = 0, attack_max_hp = 0, wipe_duration = 0, cooldown_time = 0, capacity = 0, xp_per_hour = 0;
	public bool initial = false, flank_nullifies = false;
    public TriggerOn trigger_on;
    public VisibleOn visible_on;
	public Type type = Type.UNDEF;
    public Sprite build_icon_sprite;
    
  	public static Dictionary<int,BuildData> builds = new Dictionary<int, BuildData>();
	public static Dictionary<string,int> build_name_map = new Dictionary<string, int>();
	public static string version_string = "";
    public static string fmtLocName = "Builds/build_{0}_name";
    public static string fmtLocDesc = "Builds/build_{0}_description";

    public static BuildData GetBuildData(int _ID)
	{
        if (builds.ContainsKey(_ID)) {
            return builds[_ID];
        } else {
            return null;
        }
	}

	public static int GetNameToIDMap(string _name)
	{
        if (build_name_map.ContainsKey(_name)) {
            return build_name_map[_name];
        } else {
            return -1;
        }
	}

    public static void UpdateLocalization()
    {
        foreach (var id in builds.Keys)
        {
            builds[id].name = I2.Loc.LocalizationManager.GetTranslation(String.Format(fmtLocName, id));

            builds[id].description = I2.Loc.LocalizationManager.GetTranslation(String.Format(fmtLocDesc, id))
                .Replace("{effect_radius}", string.Format("{0}", builds[id].effect_radius))
                .Replace("{attack_radius}", string.Format("{0}", builds[id].attack_radius))
                .Replace("{refill_hours}", string.Format("{0}", GameData.instance.storageRefillHours));
        }
    }

    public static bool LoadBuilds()
	{
		int line_num = -1;
		BuildData build_data;
		int[] place = new int[1];
        String value_string;

        Debug.Log("BuildData.LoadBuilds() location: " + Application.persistentDataPath + "/builds.tsv");

        // Return false if the file doesn't exist on the client.
        if (File.Exists(Application.persistentDataPath + "/builds.tsv") == false) {
            return false;
        }

        // Clear data from any previous load.
        builds.Clear();
        build_name_map.Clear();

        var lines = File.ReadAllLines(Application.persistentDataPath + "/builds.tsv");
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

			// Create a BuildData object for this line's build.
			build_data = new BuildData();

			// Start at the beginning of the line.
			place[0] = 0;

            // Name
            build_data.original_name = build_data.name = GetNextTabSeparatedValue(line, place);

            // ID
            string ID_string = GetNextTabSeparatedValue(line, place);
			build_data.ID = (ID_string.Length == 0) ? -1 : Int32.Parse(ID_string);

            // Upgrades
            build_data.upgrades_name = GetNextTabSeparatedValue(line, place);

   			// Land
			String land_string = GetNextTabSeparatedValue(line, place).ToLower();
			if (land_string.Length == 0) 
			{
				build_data.land = LAND_FLAG_ALL;
			}
			else 
			{
				if (land_string.Contains("mainland")) build_data.land = build_data.land | LAND_FLAG_MAINLAND;
				if (land_string.Contains("homeland")) build_data.land = build_data.land | LAND_FLAG_HOMELAND;
				if (land_string.Contains("raid")) build_data.land = build_data.land | LAND_FLAG_RAID;
			}
            
   			// Initial
			String initial_string = GetNextTabSeparatedValue(line, place);
            build_data.initial = (String.Equals(initial_string, "TRUE", StringComparison.OrdinalIgnoreCase));

   			// Manpower cost
			String manpower_cost_string = GetNextTabSeparatedValue(line, place);
			build_data.manpower_cost = (manpower_cost_string.Length == 0) ? 0 : Int32.Parse(manpower_cost_string);

            // Energy burn rate
			String energy_burn_rate_string = GetNextTabSeparatedValue(line, place);
			build_data.energy_burn_rate = (energy_burn_rate_string.Length == 0) ? 0 : Int32.Parse(energy_burn_rate_string);

            // Hit points
			String hit_points_string = GetNextTabSeparatedValue(line, place);
			build_data.hit_points = (hit_points_string.Length == 0) ? 0 : Int32.Parse(hit_points_string);

            // Build time
			String build_time_string = GetNextTabSeparatedValue(line, place);
			build_data.build_time = (build_time_string.Length == 0) ? 0 : Int32.Parse(build_time_string);

   			// Type
            String type_string = GetNextTabSeparatedValue(line, place);
			if (String.Equals(type_string, "wall", StringComparison.OrdinalIgnoreCase)) {
				build_data.type = BuildData.Type.WALL;
			} else if (String.Equals(type_string, "energy_storage", StringComparison.OrdinalIgnoreCase)) {
				build_data.type = BuildData.Type.ENERGY_STORAGE;
			} else if (String.Equals(type_string, "manpower_storage", StringComparison.OrdinalIgnoreCase)) {
				build_data.type = BuildData.Type.MANPOWER_STORAGE;
			} else if (String.Equals(type_string, "directed_multiple", StringComparison.OrdinalIgnoreCase)) {
				build_data.type = BuildData.Type.DIRECTED_MULTIPLE;
			} else if (String.Equals(type_string, "splash", StringComparison.OrdinalIgnoreCase)) {
				build_data.type = BuildData.Type.SPLASH;
			} else if (String.Equals(type_string, "area_effect", StringComparison.OrdinalIgnoreCase)) {
				build_data.type = BuildData.Type.AREA_EFFECT;
			} else if (String.Equals(type_string, "area_fortification", StringComparison.OrdinalIgnoreCase)) {
				build_data.type = BuildData.Type.AREA_FORTIFICATION;
			} else if (String.Equals(type_string, "counter_attack", StringComparison.OrdinalIgnoreCase)) {
				build_data.type = BuildData.Type.COUNTER_ATTACK;
			} else if (String.Equals(type_string, "wipe", StringComparison.OrdinalIgnoreCase)) {
				build_data.type = BuildData.Type.WIPE;
			} else if (String.Equals(type_string, "general_lasting_wipe", StringComparison.OrdinalIgnoreCase)) {
				build_data.type = BuildData.Type.GENERAL_LASTING_WIPE;
			} else if (String.Equals(type_string, "specific_lasting_wipe", StringComparison.OrdinalIgnoreCase)) {
				build_data.type = BuildData.Type.SPECIFIC_LASTING_WIPE;
			} else if (String.Equals(type_string, "air_drop", StringComparison.OrdinalIgnoreCase)) {
				build_data.type = BuildData.Type.AIR_DROP;
			} else if (String.Equals(type_string, "recapture", StringComparison.OrdinalIgnoreCase)) {
				build_data.type = BuildData.Type.RECAPTURE;
			} else if (String.Equals(type_string, "dummy", StringComparison.OrdinalIgnoreCase)) {
				build_data.type = BuildData.Type.DUMMY;
			} else if (String.Equals(type_string, "tower_buster", StringComparison.OrdinalIgnoreCase)) {
				build_data.type = BuildData.Type.TOWER_BUSTER;
			} else if (String.Equals(type_string, "shard", StringComparison.OrdinalIgnoreCase)) {
				build_data.type = BuildData.Type.SHARD;
			} else {
				Debug.Log("Unknown build type '" + type_string + "' at line " + line_num + ".");
			}

            // Trigger on
			String trigger_on_string = GetNextTabSeparatedValue(line, place);
			if (String.Equals(trigger_on_string, "radius_attack", StringComparison.OrdinalIgnoreCase)) {
				build_data.trigger_on = BuildData.TriggerOn.RADIUS_ATTACK;
			} else if (String.Equals(trigger_on_string, "direct_attack", StringComparison.OrdinalIgnoreCase)) {
				build_data.trigger_on = BuildData.TriggerOn.DIRECT_ATTACK;
			} else if (String.Equals(trigger_on_string, "radius_tower", StringComparison.OrdinalIgnoreCase)) {
				build_data.trigger_on = BuildData.TriggerOn.RADIUS_TOWER;
   			} else if (String.Equals(trigger_on_string, "radius_attack_empty", StringComparison.OrdinalIgnoreCase)) {
				build_data.trigger_on = BuildData.TriggerOn.RADIUS_ATTACK_EMPTY;
			} else if (String.Equals(trigger_on_string, "none", StringComparison.OrdinalIgnoreCase)) {
				build_data.trigger_on = BuildData.TriggerOn.UNDEF;
			} else {
				Debug.Log("Unknown trigger on value '" + trigger_on_string + "' at line " + line_num + ".");
			}

			// Visible on
			String visible_on_string = GetNextTabSeparatedValue(line, place);
			if (String.Equals(visible_on_string, "triggered", StringComparison.OrdinalIgnoreCase)) {
				build_data.visible_on = BuildData.VisibleOn.TRIGGERED;
			} else if (String.Equals(visible_on_string, "attacked", StringComparison.OrdinalIgnoreCase)) {
				build_data.visible_on = BuildData.VisibleOn.ATTACKED;
			} else if (String.Equals(visible_on_string, "always", StringComparison.OrdinalIgnoreCase)) {
				build_data.visible_on = BuildData.VisibleOn.ALWAYS;
			} else {
				Debug.Log("Unknown visible on value '" + visible_on_string + "' at line " + line_num + ".");
			}

            //String hit_points_string = GetNextTabSeparatedValue(line, place);
			//build_data.hit_points = (hit_points_string.Length == 0) ? 0 : Int32.Parse(hit_points_string);


			// Attack radius
			value_string = GetNextTabSeparatedValue(line, place);
			build_data.attack_radius = (value_string.Length == 0) ? 0 : Int32.Parse(value_string);

			if (build_data.attack_radius > MAX_ATTACK_RADIUS) {
				Debug.Log("Attack radius of " + build_data.attack_radius + " > max of " + MAX_ATTACK_RADIUS + " at line " + line_num + ".");
			}

			// Effect radius
			value_string = GetNextTabSeparatedValue(line, place);
			build_data.effect_radius = (value_string.Length == 0) ? 0 : Int32.Parse(value_string);

			// Num attacks
			value_string = GetNextTabSeparatedValue(line, place);
			build_data.num_attacks = (value_string.Length == 0) ? 0 : Int32.Parse(value_string);

			// Attack min HP 
			value_string = GetNextTabSeparatedValue(line, place);
			build_data.attack_min_hp = (value_string.Length == 0) ? 0 : Int32.Parse(value_string);

			// Attack max HP
			value_string = GetNextTabSeparatedValue(line, place);
			build_data.attack_max_hp = (value_string.Length == 0) ? 0 : Int32.Parse(value_string);

			// Wipe duration
			value_string = GetNextTabSeparatedValue(line, place);
			build_data.wipe_duration = (value_string.Length == 0) ? 0 : Int32.Parse(value_string);
				
			// Cooldown time
			value_string = GetNextTabSeparatedValue(line, place);
			build_data.cooldown_time = (value_string.Length == 0) ? 0 : Int32.Parse(value_string);

			// Capacity
			value_string = GetNextTabSeparatedValue(line, place);
			build_data.capacity = (value_string.Length == 0) ? 0 : Int32.Parse(value_string);

            // XP per hour
			value_string = GetNextTabSeparatedValue(line, place);
			build_data.xp_per_hour = (value_string.Length == 0) ? 0 : Int32.Parse(value_string);

			// Flank nullifies
			String flank_nullifies_string = GetNextTabSeparatedValue(line, place);
			if (String.Equals(flank_nullifies_string, "true", StringComparison.OrdinalIgnoreCase)) {
				build_data.flank_nullifies = true;
			} else {
				build_data.flank_nullifies = false;
			}

   			// Capacity
			value_string = GetNextTabSeparatedValue(line, place);
			build_data.max_count = (value_string.Length == 0) ? -1 : Int32.Parse(value_string);

            // Description
			build_data.description = GetNextTabSeparatedValue(line, place);

            if (build_data.ID < 0) 
			{
				Debug.Log("Build with missing ID at line " + line_num);
				return false;
			}

            // Add this build to the builds vector and build_name_map.
            //Debug.Log("Adding BuildData " + build_data.name + " with ID " + build_data.ID);
			builds.Add(build_data.ID, build_data);
			build_name_map.Add(build_data.name, build_data.ID);
		}

        foreach(KeyValuePair<int, BuildData> entry in builds)
		{
            build_data = entry.Value;
			//Debug.Log("Build " + build_data.name);

			if (build_data.upgrades_name.Length == 0)
			{
				build_data.upgrades = -1;
			}
			else 
			{
				build_data.upgrades = GetNameToIDMap(build_data.upgrades_name);

				if (build_data.upgrades < 0) 
				{
					Debug.Log("\'" + build_data.name + "\' upgrades name \'" + build_data.upgrades_name + "\' not a valid build.");
                    builds.Clear();
                    build_name_map.Clear();
					return false;
				}

                // Record the ID of the build that the upgraded from build upgrades to.
                BuildData upgraded_build = BuildData.GetBuildData(build_data.upgrades);
                if (upgraded_build != null)
                {
                    upgraded_build.upgrades_to = build_data.ID;
                }
			}
        }

        UpdateLocalization();

		return (builds.Count > 0);		
	}

    public GameObject GetPrefab()
    {
        switch (type)
        {
            case Type.DIRECTED_MULTIPLE:
                if (original_name.Equals("Cannon"))
                {
                    return BuildPrefabs.instance.cannon1;
                }
                else if (original_name.Equals("Cannon II"))
                {
                    return BuildPrefabs.instance.cannon2;
                }
                else if (original_name.Equals("Cannon III"))
                {
                    return BuildPrefabs.instance.cannon3;
                }
                if (original_name.Equals("Artillery Battery"))
                {
                    return BuildPrefabs.instance.artillery1;
                }
                else if (original_name.Equals("Artillery Battery II"))
                {
                    return BuildPrefabs.instance.artillery2;
                }
                else if (original_name.Equals("Artillery Battery III"))
                {
                    return BuildPrefabs.instance.artillery3;
                }
                else if (original_name.Equals("Rocket Launcher"))
                {
                    return BuildPrefabs.instance.rocketLauncher1;
                }
                else if (original_name.Equals("Rocket Launcher II"))
                {
                    return BuildPrefabs.instance.rocketLauncher2;
                }
                else if (original_name.Equals("Rocket Launcher III"))
                {
                    return BuildPrefabs.instance.rocketLauncher3;
                }
                break;
            case Type.SPLASH:
                if (original_name.Equals("Pestilence Launcher"))
                {
                    return BuildPrefabs.instance.pestilenceLauncher1;
                }
                else if (original_name.Equals("Pestilence Launcher II"))
                {
                    return BuildPrefabs.instance.pestilenceLauncher2;
                }
                else if (original_name.Equals("Pestilence Launcher III"))
                {
                    return BuildPrefabs.instance.pestilenceLauncher3;
                }
                else if (original_name.Equals("Toxic Mist Launcher"))
                {
                    return BuildPrefabs.instance.toxicMistLauncher1;
                }
                else if (original_name.Equals("Toxic Mist Launcher II"))
                {
                    return BuildPrefabs.instance.toxicMistLauncher2;
                }
                else if (original_name.Equals("Toxic Mist Launcher III"))
                {
                    return BuildPrefabs.instance.toxicMistLauncher3;
                }
                else if (original_name.Equals("Pathogen Spore Launcher"))
                {
                    return BuildPrefabs.instance.pathogenSporeLauncher1;
                }
                else if (original_name.Equals("Pathogen Spore Launcher II"))
                {
                    return BuildPrefabs.instance.pathogenSporeLauncher2;
                }
                else if (original_name.Equals("Pathogen Spore Launcher III"))
                {
                    return BuildPrefabs.instance.pathogenSporeLauncher3;
                }
                else if (original_name.Equals("Nanobot Swarm Base"))
                {
                    return BuildPrefabs.instance.nanobotSwarmBase1;
                }
                else if (original_name.Equals("Nanobot Swarm Base II"))
                {
                    return BuildPrefabs.instance.nanobotSwarmBase2;
                }
                else if (original_name.Equals("Nanobot Swarm Base III"))
                {
                    return BuildPrefabs.instance.nanobotSwarmBase3;
                }
                break;
            case Type.AREA_EFFECT:
                if (original_name.Equals("Telekinetic Projector"))
                {
                    return BuildPrefabs.instance.telekineticProjector1;
                }
                else if (original_name.Equals("Telekinetic Projector II"))
                {
                    return BuildPrefabs.instance.telekineticProjector2;
                }
                else if (original_name.Equals("Telekinetic Projector III"))
                {
                    return BuildPrefabs.instance.telekineticProjector3;
                }
                else if (original_name.Equals("Pyroclasm"))
                {
                    return BuildPrefabs.instance.pyroclasm1;
                }
                else if (original_name.Equals("Pyroclasm II"))
                {
                    return BuildPrefabs.instance.pyroclasm2;
                }
                else if (original_name.Equals("Pyroclasm III"))
                {
                    return BuildPrefabs.instance.pyroclasm3;
                }
                else if (original_name.Equals("Keraunocon"))
                {
                    return BuildPrefabs.instance.keraunocon1;
                }
                else if (original_name.Equals("Keraunocon II"))
                {
                    return BuildPrefabs.instance.keraunocon2;
                }
                else if (original_name.Equals("Keraunocon III"))
                {
                    return BuildPrefabs.instance.keraunocon3;
                }
                break;
            case Type.COUNTER_ATTACK:
                if (original_name.Equals("Radio Tower"))
                {
                    return BuildPrefabs.instance.radioTower1;
                }
                else if (original_name.Equals("Radio Tower II"))
                {
                    return BuildPrefabs.instance.radioTower2;
                }
                else if (original_name.Equals("Radio Tower III"))
                {
                    return BuildPrefabs.instance.radioTower3;
                }
                else if (original_name.Equals("SatCom Command"))
                {
                    return BuildPrefabs.instance.satComCommand1;
                }
                else if (original_name.Equals("SatCom Command II"))
                {
                    return BuildPrefabs.instance.satComCommand2;
                }
                else if (original_name.Equals("SatCom Command III"))
                {
                    return BuildPrefabs.instance.satComCommand3;
                }
                else if (original_name.Equals("Autonomous War Base"))
                {
                    return BuildPrefabs.instance.autonomousWarBase1;
                }
                else if (original_name.Equals("Autonomous War Base II"))
                {
                    return BuildPrefabs.instance.autonomousWarBase2;
                }
                else if (original_name.Equals("Autonomous War Base III"))
                {
                    return BuildPrefabs.instance.autonomousWarBase3;
                }
                break;
            case Type.WIPE:
                if (original_name.Equals("Dead Hand"))
                {
                    return BuildPrefabs.instance.deadHand1;
                }
                else if (original_name.Equals("Dead Hand II"))
                {
                    return BuildPrefabs.instance.deadHand2;
                }
                else if (original_name.Equals("Dead Hand III"))
                {
                    return BuildPrefabs.instance.deadHand3;
                }
                else if (original_name.Equals("Geographic Wipe"))
                {
                    return BuildPrefabs.instance.geographicWipe1;
                }
                else if (original_name.Equals("Geographic Wipe II"))
                {
                    return BuildPrefabs.instance.geographicWipe2;
                }
                else if (original_name.Equals("Geographic Wipe III"))
                {
                    return BuildPrefabs.instance.geographicWipe3;
                }
                break;
            case Type.GENERAL_LASTING_WIPE:
                if (original_name.Equals("Toxic Chemical Dump"))
                {
                    return BuildPrefabs.instance.toxicChemicalDump1;
                }
                else if (original_name.Equals("Toxic Chemical Dump II"))
                {
                    return BuildPrefabs.instance.toxicChemicalDump2;
                }
                else if (original_name.Equals("Toxic Chemical Dump III"))
                {
                    return BuildPrefabs.instance.toxicChemicalDump3;
                }
                else if (original_name.Equals("Supervirus Contagion"))
                {
                    return BuildPrefabs.instance.supervirusContagion1;
                }
                else if (original_name.Equals("Supervirus Contagion II"))
                {
                    return BuildPrefabs.instance.supervirusContagion2;
                }
                else if (original_name.Equals("Supervirus Contagion III"))
                {
                    return BuildPrefabs.instance.supervirusContagion3;
                }
                break;
            case Type.SPECIFIC_LASTING_WIPE:
                if (original_name.Equals("Hypnotic Inducer"))
                {
                    return BuildPrefabs.instance.hypnoticInducer1;
                }
                else if (original_name.Equals("Hypnotic Inducer II"))
                {
                    return BuildPrefabs.instance.hypnoticInducer2;
                }
                else if (original_name.Equals("Hypnotic Inducer III"))
                {
                    return BuildPrefabs.instance.hypnoticInducer3;
                }
                else if (original_name.Equals("Temple of Zoth-Ommog"))
                {
                    return BuildPrefabs.instance.templeOfZothOmmog1;
                }
                else if (original_name.Equals("Temple of Zoth-Ommog II"))
                {
                    return BuildPrefabs.instance.templeOfZothOmmog2;
                }
                else if (original_name.Equals("Temple of Zoth-Ommog III"))
                {
                    return BuildPrefabs.instance.templeOfZothOmmog3;
                }
                break;
            case Type.RECAPTURE:
                if (original_name.Equals("Ecto Ray"))
                {
                    return BuildPrefabs.instance.ectoRay1;
                }
                else if (original_name.Equals("Ecto Ray II"))
                {
                    return BuildPrefabs.instance.ectoRay2;
                }
                else if (original_name.Equals("Ecto Ray III"))
                {
                    return BuildPrefabs.instance.ectoRay3;
                }
                else if (original_name.Equals("Djinn Portal"))
                {
                    return BuildPrefabs.instance.djinnPortal1;
                }
                else if (original_name.Equals("Djinn Portal II"))
                {
                    return BuildPrefabs.instance.djinnPortal2;
                }
                else if (original_name.Equals("Djinn Portal III"))
                {
                    return BuildPrefabs.instance.djinnPortal3;
                }
                break;
            case Type.TOWER_BUSTER:
                if (original_name.Equals("Guided Missile Station"))
                {
                    return BuildPrefabs.instance.guidedMissileStation1;
                }
                else if (original_name.Equals("Guided Missile Station II"))
                {
                    return BuildPrefabs.instance.guidedMissileStation2;
                
                }
                else if (original_name.Equals("Guided Missile Station III"))
                {
                    return BuildPrefabs.instance.guidedMissileStation3;
                }
                break;
            case Type.AREA_FORTIFICATION:
                if (original_name.Equals("Tree Summoner"))
                {
                    return BuildPrefabs.instance.treeSummoner1;
                }
                else if (original_name.Equals("Tree Summoner II"))
                {
                    return BuildPrefabs.instance.treeSummoner2;
                }
                else if (original_name.Equals("Tree Summoner III"))
                {
                    return BuildPrefabs.instance.treeSummoner3;
                }
                else if (original_name.Equals("Roots of Despair"))
                {
                    return BuildPrefabs.instance.rootsOfDespair1;
                }
                else if (original_name.Equals("Roots of Despair II"))
                {
                    return BuildPrefabs.instance.rootsOfDespair2;
                }
                else if (original_name.Equals("Roots of Despair III"))
                {
                    return BuildPrefabs.instance.rootsOfDespair3;
                }
                break;
            case Type.AIR_DROP:
                if (original_name.Equals("Brainsweeper"))
                {
                    return BuildPrefabs.instance.brainSweeper1;
                }
                else if (original_name.Equals("Brainsweeper II"))
                {
                    return BuildPrefabs.instance.brainSweeper2;
                }
                else if (original_name.Equals("Brainsweeper III"))
                {
                    return BuildPrefabs.instance.brainSweeper3;
                }
                break;
            case Type.DUMMY:
                if (original_name.Equals("Phantasmic Threat"))
                {
                    return BuildPrefabs.instance.phantasmicThreat1;
                }
                else if (original_name.Equals("Phantasmic Threat II"))
                {
                    return BuildPrefabs.instance.phantasmicThreat2;
                }
                else if (original_name.Equals("Phantasmic Threat III"))
                {
                    return BuildPrefabs.instance.phantasmicThreat3;
                }
                break;
            case Type.ENERGY_STORAGE:
                if (original_name.Equals("Fuel Tank"))
                {
                    return BuildPrefabs.instance.fuelTank1;
                }
                else if (original_name.Equals("Fuel Tank II"))
                {
                    return BuildPrefabs.instance.fuelTank2;
                }
                else if (original_name.Equals("Fuel Tank III"))
                {
                    return BuildPrefabs.instance.fuelTank3;
                }
                else if (original_name.Equals("Energy Vortex"))
                {
                    return BuildPrefabs.instance.energyVortex1;
                }
                else if (original_name.Equals("Energy Vortex II"))
                {
                    return BuildPrefabs.instance.energyVortex2;
                }
                else if (original_name.Equals("Energy Vortex III"))
                {
                    return BuildPrefabs.instance.energyVortex3;
                }
                else if (original_name.Equals("Earth Chakra"))
                {
                    return BuildPrefabs.instance.earthChakra1;
                }
                else if (original_name.Equals("Earth Chakra II"))
                {
                    return BuildPrefabs.instance.earthChakra2;
                }
                else if (original_name.Equals("Earth Chakra III"))
                {
                    return BuildPrefabs.instance.earthChakra3;
                }
                break;
            case Type.MANPOWER_STORAGE:
                if (original_name.Equals("Agro Colony"))
                {
                    return BuildPrefabs.instance.agroColony1;
                }
                else if (original_name.Equals("Agro Colony II"))
                {
                    return BuildPrefabs.instance.agroColony2;
                }
                else if (original_name.Equals("Agro Colony III"))
                {
                    return BuildPrefabs.instance.agroColony3;
                }
                else if (original_name.Equals("Hydroponic Garden"))
                {
                    return BuildPrefabs.instance.hydroponicGarden1;
                }
                else if (original_name.Equals("Hydroponic Garden II"))
                {
                    return BuildPrefabs.instance.hydroponicGarden2;
                }
                else if (original_name.Equals("Hydroponic Garden III"))
                {
                    return BuildPrefabs.instance.hydroponicGarden3;
                }
                break;
            case Type.WALL:
                if (original_name.Equals("Wall"))
                {
                    return BuildPrefabs.instance.wall1Post;
                }
                else if (original_name.Equals("Wall II"))
                {
                    return BuildPrefabs.instance.wall2Post;
                }
                else if (original_name.Equals("Wall III"))
                {
                    return BuildPrefabs.instance.wall3Post;
                }
                else if (original_name.Equals("Plasma Screen"))
                {
                    return BuildPrefabs.instance.plasmaScreen1Post;
                }
                else if (original_name.Equals("Plasma Screen II"))
                {
                    return BuildPrefabs.instance.plasmaScreen2Post;
                }
                else if (original_name.Equals("Plasma Screen III"))
                {
                    return BuildPrefabs.instance.plasmaScreen3Post;
                }
                else if (original_name.Equals("Void Dam"))
                {
                    return BuildPrefabs.instance.voidDam1Post;
                }
                else if (original_name.Equals("Void Dam II"))
                {
                    return BuildPrefabs.instance.voidDam2Post;
                }
                else if (original_name.Equals("Void Dam III"))
                {
                    return BuildPrefabs.instance.voidDam3Post;
                }
                else if (original_name.Equals("Hedge"))
                {
                    return BuildPrefabs.instance.hedge1Post;
                }
                else if (original_name.Equals("Hedge II"))
                {
                    return BuildPrefabs.instance.hedge2Post;
                }
                else if (original_name.Equals("Hedge III"))
                {
                    return BuildPrefabs.instance.hedge3Post;
                }
                else if (original_name.Equals("Strangling Vines"))
                {
                    return BuildPrefabs.instance.stranglingVines1Post;
                }
                else if (original_name.Equals("Strangling Vines II"))
                {
                    return BuildPrefabs.instance.stranglingVines2Post;
                }
                else if (original_name.Equals("Strangling Vines III"))
                {
                    return BuildPrefabs.instance.stranglingVines3Post;
                }
                else if (original_name.Equals("Root Barricade"))
                {
                    return BuildPrefabs.instance.rootBarricade1Post;
                }
                else if (original_name.Equals("Root Barricade II"))
                {
                    return BuildPrefabs.instance.rootBarricade2Post;
                }
                else if (original_name.Equals("Root Barricade III"))
                {
                    return BuildPrefabs.instance.rootBarricade3Post;
                }
                else if (original_name.Equals("Telekinetic Block"))
                {
                    return BuildPrefabs.instance.telekineticBlock1Post;
                }
                else if (original_name.Equals("Telekinetic Block II"))
                {
                    return BuildPrefabs.instance.telekineticBlock2Post;
                }
                else if (original_name.Equals("Telekinetic Block III"))
                {
                    return BuildPrefabs.instance.telekineticBlock3Post;
                }
                else if (original_name.Equals("Pyralisade"))
                {
                    return BuildPrefabs.instance.pyralisade1Post;
                }
                else if (original_name.Equals("Pyralisade II"))
                {
                    return BuildPrefabs.instance.pyralisade2Post;
                }
                else if (original_name.Equals("Pyralisade III"))
                {
                    return BuildPrefabs.instance.pyralisade3Post;
                }
                else if (original_name.Equals("Ectochasm"))
                {
                    return BuildPrefabs.instance.ectochasm1Post;
                }
                else if (original_name.Equals("Ectochasm II"))
                {
                    return BuildPrefabs.instance.ectochasm2Post;
                }
                else if (original_name.Equals("Ectochasm III"))
                {
                    return BuildPrefabs.instance.ectochasm3Post;
                }
                else
                {
                    return BuildPrefabs.instance.wall1Post;
                }
                break;
            case Type.SHARD:
                if (original_name.Equals("Red Shard"))
                {
                    return BuildPrefabs.instance.shardRed;
                }
                else if (original_name.Equals("Green Shard"))
                {
                    return BuildPrefabs.instance.shardGreen;
                }
                else if (original_name.Equals("Blue Shard"))
                {
                    return BuildPrefabs.instance.shardBlue;
                }
                break;
        }

        return null;
    }

    public GameObject GetWallLengthPrefab()
    {
        switch (type)
        {
            case Type.WALL:
                if (original_name.Equals("Wall"))
                {
                    return BuildPrefabs.instance.wall1Length;
                }
                else if (original_name.Equals("Wall II"))
                {
                    return BuildPrefabs.instance.wall2Length;
                }
                else if (original_name.Equals("Wall III"))
                {
                    return BuildPrefabs.instance.wall3Length;
                }
                else if (original_name.Equals("Plasma Screen"))
                {
                    return BuildPrefabs.instance.plasmaScreen1Length;
                }
                else if (original_name.Equals("Plasma Screen II"))
                {
                    return BuildPrefabs.instance.plasmaScreen2Length;
                }
                else if (original_name.Equals("Plasma Screen III"))
                {
                    return BuildPrefabs.instance.plasmaScreen3Length;
                }
                else if (original_name.Equals("Void Dam"))
                {
                    return BuildPrefabs.instance.voidDam1Length;
                }
                else if (original_name.Equals("Void Dam II"))
                {
                    return BuildPrefabs.instance.voidDam2Length;
                }
                else if (original_name.Equals("Void Dam III"))
                {
                    return BuildPrefabs.instance.voidDam3Length;
                }
                else if (original_name.Equals("Hedge"))
                {
                    return BuildPrefabs.instance.hedge1Length;
                }
                else if (original_name.Equals("Hedge II"))
                {
                    return BuildPrefabs.instance.hedge2Length;
                }
                else if (original_name.Equals("Hedge III"))
                {
                    return BuildPrefabs.instance.hedge3Length;
                }
                else if (original_name.Equals("Strangling Vines"))
                {
                    return BuildPrefabs.instance.stranglingVines1Length;
                }
                else if (original_name.Equals("Strangling Vines II"))
                {
                    return BuildPrefabs.instance.stranglingVines2Length;
                }
                else if (original_name.Equals("Strangling Vines III"))
                {
                    return BuildPrefabs.instance.stranglingVines3Length;
                }
                else if (original_name.Equals("Root Barricade"))
                {
                    return BuildPrefabs.instance.rootBarricade1Length;
                }
                else if (original_name.Equals("Root Barricade II"))
                {
                    return BuildPrefabs.instance.rootBarricade2Length;
                }
                else if (original_name.Equals("Root Barricade III"))
                {
                    return BuildPrefabs.instance.rootBarricade3Length;
                }
                else if (original_name.Equals("Telekinetic Block"))
                {
                    return BuildPrefabs.instance.telekineticBlock1Length;
                }
                else if (original_name.Equals("Telekinetic Block II"))
                {
                    return BuildPrefabs.instance.telekineticBlock2Length;
                }
                else if (original_name.Equals("Telekinetic Block III"))
                {
                    return BuildPrefabs.instance.telekineticBlock3Length;
                }
                else if (original_name.Equals("Pyralisade"))
                {
                    return BuildPrefabs.instance.pyralisade1Length;
                }
                else if (original_name.Equals("Pyralisade II"))
                {
                    return BuildPrefabs.instance.pyralisade2Length;
                }
                else if (original_name.Equals("Pyralisade III"))
                {
                    return BuildPrefabs.instance.pyralisade3Length;
                }
                else if (original_name.Equals("Ectochasm"))
                {
                    return BuildPrefabs.instance.ectochasm1Length;
                }
                else if (original_name.Equals("Ectochasm II"))
                {
                    return BuildPrefabs.instance.ectochasm2Length;
                }
                else if (original_name.Equals("Ectochasm III"))
                {
                    return BuildPrefabs.instance.ectochasm3Length;
                }
                else
                {
                    return BuildPrefabs.instance.wall1Length;
                }
                break;
        }

        return null;
    }

    public bool GetWallShowAllPosts()
    {
        switch (type)
        {
            case Type.WALL:
                if ((original_name.Equals("Wall II")) ||
                    (original_name.Equals("Wall III")) ||
                    (original_name.Contains("Strangling Vines")) ||
                    (original_name.Contains("Root Barricade")) ||
                    (original_name.Contains("Pyralisade")) ||
                    (original_name.Contains("Ectochasm")))
                {
                    return false;
                }
                else
                {
                    return true;
                }
            default:
                return false;
        }
    }

    public GameObject GetRaidPrefab()
    {
        switch (type)
        {
            case Type.COUNTER_ATTACK:
                if (original_name.Equals("Radio Tower"))
                {
                    return BuildPrefabs.instance.radTow1Raid;
                }
                else if (original_name.Equals("Radio Tower II"))
                {
                    return BuildPrefabs.instance.radTow2Raid;
                }
                else if (original_name.Equals("Radio Tower III"))
                {
                    return BuildPrefabs.instance.radTow3Raid;
                }
                else if (original_name.Equals("SatCom Command"))
                {
                    return BuildPrefabs.instance.satCom1Raid;
                }
                else if (original_name.Equals("SatCom Command II"))
                {
                    return BuildPrefabs.instance.satCom2Raid;
                }
                else if (original_name.Equals("SatCom Command III"))
                {
                    return BuildPrefabs.instance.satCom3Raid;
                }
                else if (original_name.Equals("Autonomous War Base"))
                {
                    return BuildPrefabs.instance.warBase1Raid;
                }
                else if (original_name.Equals("Autonomous War Base II"))
                {
                    return BuildPrefabs.instance.warBase2Raid;
                }
                else if (original_name.Equals("Autonomous War Base III"))
                {
                    return BuildPrefabs.instance.warBase3Raid;
                }
                break;
        }

        return null;
    }

    public AudioClip GetRaidAudioClip()
    {
        switch (type)
        {
            case Type.COUNTER_ATTACK:
                if (original_name.Equals("Radio Tower"))
                {
                    return Sound.instance.counter_attack_tanks;
                }
                else if (original_name.Equals("Radio Tower II"))
                {
                    return Sound.instance.counter_attack_tanks;
                }
                else if (original_name.Equals("Radio Tower III"))
                {
                    return Sound.instance.counter_attack_tanks;
                }
                else if (original_name.Equals("SatCom Command"))
                {
                    return Sound.instance.counter_attack_planes;
                }
                else if (original_name.Equals("SatCom Command II"))
                {
                    return Sound.instance.counter_attack_planes;
                }
                else if (original_name.Equals("SatCom Command III"))
                {
                    return Sound.instance.counter_attack_planes;
                }
                else if (original_name.Equals("Autonomous War Base"))
                {
                    return Sound.instance.counter_attack_mechs;
                }
                else if (original_name.Equals("Autonomous War Base II"))
                {
                    return Sound.instance.counter_attack_mechs;
                }
                else if (original_name.Equals("Autonomous War Base III"))
                {
                    return Sound.instance.counter_attack_mechs;
                }
                break;
        }

        return null;
    }

    public bool MayBecomeInert()
    {
        return ((type != BuildData.Type.ENERGY_STORAGE) && (type != BuildData.Type.MANPOWER_STORAGE) && (type != BuildData.Type.SHARD));
    }

    public void PrepObject(GameObject _gameObject)
    {
        if (type == Type.COUNTER_ATTACK)
        {
            // Start by turning off FX emission.
            SetEmission(_gameObject, false);
        }

        SetMaterialFlag(_gameObject);
    }

    // Set the flag on each of the object's materials, needed to have the objects draw correctly with "Fade" render mode.
    // See this for more details: http://forum.unity3d.com/threads/unity-5-fade-render-mode-sort-order-issue.318298/
    private void SetMaterialFlag(GameObject _gameObject)
    {
        // Turn on the flag for each of this object's materials.
        Renderer rend = _gameObject.GetComponent<Renderer>();
        if (rend != null)
        {
            foreach (Material material in rend.materials) {
                material.SetInt("_ZWrite", 1);
                //Debug.Log("Set flag in material " + material.name);
            }
        }
  
        // Call this method recursively for all child objects.
        foreach (Transform child in _gameObject.transform) {
            SetMaterialFlag(child.gameObject);
        }
    }

    public void SetEmission(GameObject _gameObject, bool _enable)
    {
        //Debug.Log("Test: " + object0.name);
        //Debug.Log("Test: " + object0.transform.GetChild(0).GetChild(0).GetChild(0).gameObject.name);
        try
        {
            ParticleSystem.EmissionModule em = _gameObject.transform.GetChild(0).GetChild(0).GetChild(0).gameObject.GetComponent<ParticleSystem>().emission;
            em.enabled = _enable;
            em = _gameObject.transform.GetChild(0).GetChild(0).GetChild(0).GetChild(0).gameObject.GetComponent<ParticleSystem>().emission;
            em.enabled = _enable;
        }
        catch (Exception e)
        {
            Debug.Log("BuildData.SetEmission() called for object " + _gameObject.name + " that doesn't have an emission object: " + e.Message);
        }
    }
}
