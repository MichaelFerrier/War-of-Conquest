using UnityEngine;
using System;
using System.Linq;
using System.Collections;
using System.Collections.Generic;
using System.IO;
using System.Globalization;

public class ObjectData : FileData
{
    public enum Type
    {
	    UNDEF                   = -1,
	    TECH                    = 0,
        ORB                     = 1
    }

    public const int RESOURCE_OBJECT_BASE_ID = 1000;
    public const int ORB_BASE_ID = 2000;

    public string original_name = "";
    public string name = "";
    public Type type = Type.UNDEF;
    public int ID = -1, techID = -1, xp = 0;
    public float credits_per_hour = 0, xp_per_hour = 0, frequency = 0, range_min = 0, range_max = 1;
	public string description = "";
    
  	public static Dictionary<int,ObjectData> objects = new Dictionary<int, ObjectData>();
	public static Dictionary<string,int> object_name_map = new Dictionary<string, int>();
	public static string version_string = "";
    public static string fmtLocName = "Objects/object_{0}_name";
    public static string fmtLocDesc = "Objects/object_{0}_description";

    public static ObjectData GetObjectData(int _ID)
	{
        if (objects.ContainsKey(_ID)) {
            return objects[_ID];
        } else {
            return null;
        }
	}

	public static int GetNameToIDMap(string _name)
	{
        if (object_name_map.ContainsKey(_name)) {
            return object_name_map[_name];
        } else {
            return -1;
        }
	}

    public static void UpdateLocalization()
    {
        foreach (var id in objects.Keys)
        {
            objects[id].name = I2.Loc.LocalizationManager.GetTranslation(String.Format(fmtLocName, id));
            objects[id].description = I2.Loc.LocalizationManager.GetTranslation(String.Format(fmtLocDesc, id));
        }
    }

    public static bool LoadObjects()
	{
		int line_num = -1;
		ObjectData object_data;
		int[] place = new int[1];
        String value_string;

        Debug.Log("ObjectData.LoadObjects() location: " + Application.persistentDataPath + "/objects.tsv");

        // Return false if the file doesn't exist on the client.
        if (File.Exists(Application.persistentDataPath + "/objects.tsv") == false) {
            return false;
        }

        // Clear data from any previous load.
        objects.Clear();
        object_name_map.Clear();

        var lines = File.ReadAllLines(Application.persistentDataPath + "/objects.tsv");
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

			// Create a ObjectData object for this line's object.
			object_data = new ObjectData();

			// Start at the beginning of the line.
			place[0] = 0;

            // Name
            object_data.original_name = object_data.name = GetNextTabSeparatedValue(line, place);

            // ID
            value_string = GetNextTabSeparatedValue(line, place);
			object_data.ID = (value_string.Length == 0) ? -1 : Int32.Parse(value_string);

            // Type
            value_string = GetNextTabSeparatedValue(line, place);
			if (String.Equals(value_string, "tech", StringComparison.OrdinalIgnoreCase)) {
				object_data.type = ObjectData.Type.TECH;
			} else if (String.Equals(value_string, "orb", StringComparison.OrdinalIgnoreCase)) {
				object_data.type = ObjectData.Type.ORB;
			} else {
				Debug.Log("Unknown object type '" + value_string + "' at line " + line_num + ".");
			}

            // Tech ID
			value_string = GetNextTabSeparatedValue(line, place);
			object_data.techID = (value_string.Length == 0) ? -1 : Int32.Parse(value_string);

  			// Credits per hour
			value_string = GetNextTabSeparatedValue(line, place);
            object_data.credits_per_hour = (value_string.Length == 0) ? 0 : Single.Parse(value_string, CultureInfo.InvariantCulture); // Use CultureInfo.InvariantCulture so decimals will be expected to use '.' rather than ','.

   			// XP per hour
			value_string = GetNextTabSeparatedValue(line, place);
            object_data.xp_per_hour = (value_string.Length == 0) ? 0 : Single.Parse(value_string, CultureInfo.InvariantCulture);

            // Frequency
			value_string = GetNextTabSeparatedValue(line, place);
			object_data.frequency = (value_string.Length == 0) ? 0 : Single.Parse(value_string, CultureInfo.InvariantCulture);

            // Range Min
			value_string = GetNextTabSeparatedValue(line, place);
			object_data.range_min = (value_string.Length == 0) ? 0 : Single.Parse(value_string, CultureInfo.InvariantCulture);

            // Range Max
			value_string = GetNextTabSeparatedValue(line, place);
			object_data.range_max = (value_string.Length == 0) ? 1 : Single.Parse(value_string, CultureInfo.InvariantCulture);

            // xp
			value_string = GetNextTabSeparatedValue(line, place);
			object_data.xp = (value_string.Length == 0) ? -1 : Int32.Parse(value_string);

            // Description
            object_data.description = GetNextTabSeparatedValue(line, place);

            if (object_data.ID < 0) 
			{
				Debug.Log("Object with missing ID at line " + line_num);
                objects.Clear();
                object_name_map.Clear();
				return false;
			}

			// Add this object to the objects vector and object_name_map.
			objects.Add(object_data.ID, object_data);
			object_name_map.Add(object_data.name, object_data.ID);
		}

        UpdateLocalization();

		return (objects.Count > 0);		
	}

    public GameObject GetPrefab()
    {
        if (original_name.Equals("Alchemist's Lair"))
        {
            return BuildPrefabs.instance.alchemistsLair;
        }
        else if (original_name.Equals("Ancient Starship Wreckage"))
        {
            return BuildPrefabs.instance.ancientStarshipWreckage;
        }
        else if (original_name.Equals("Cryptid Colony"))
        {
            return BuildPrefabs.instance.cryptidColony;
        }
        else if (original_name.Equals("Fertile Soil"))
        {
            return BuildPrefabs.instance.fertileSoil;
        }
        else if (original_name.Equals("Fresh Water"))
        {
            return BuildPrefabs.instance.freshWater;
        }
        else if (original_name.Equals("Geothermal Vent"))
        {
            return BuildPrefabs.instance.geothermalVent;
        }
        else if (original_name.Equals("Grave of an Ancient God"))
        {
            return BuildPrefabs.instance.graveOfAnAncientGod;
        }
        else if (original_name.Equals("Henge"))
        {
            return BuildPrefabs.instance.henge;
        }
        else if (original_name.Equals("Iron Deposit"))
        {
            return BuildPrefabs.instance.ironDeposit;
        }
        else if (original_name.Equals("Ley Lines"))
        {
            return BuildPrefabs.instance.leyLines;
        }
        else if (original_name.Equals("Nickel Deposit"))
        {
            return BuildPrefabs.instance.nickelDeposit;
        }
        else if (original_name.Equals("Oil Deposit"))
        {
            return BuildPrefabs.instance.oilDeposit;
        }
        else if (original_name.Equals("Oracle"))
        {
            return BuildPrefabs.instance.oracle;
        }
        else if (original_name.Equals("Orb of Destiny"))
        {
            return BuildPrefabs.instance.orbOfDestiny;
        }
        else if (original_name.Equals("Orb of Fire"))
        {
            return BuildPrefabs.instance.orbOfFire;
        }
        else if (original_name.Equals("Orb of Noontide"))
        {
            return BuildPrefabs.instance.orbOfNoontide;
        }
        else if (original_name.Equals("Orb of Shadow"))
        {
            return BuildPrefabs.instance.orbOfShadow;
        }
        else if (original_name.Equals("Orb of the Void"))
        {
            return BuildPrefabs.instance.orbOfTheVoid;
        }
        else if (original_name.Equals("Research Facility"))
        {
            return BuildPrefabs.instance.researchFacility;
        }
        else if (original_name.Equals("Quartz Deposit"))
        {
            return BuildPrefabs.instance.quartzDeposit;
        }
        else if (original_name.Equals("Unique Ecosystem"))
        {
            return BuildPrefabs.instance.uniqueEcosystem;
        }
        else if (original_name.Equals("Uranium Deposit"))
        {
            return BuildPrefabs.instance.uraniumDeposit;
        }
        else
        {
            return BuildPrefabs.instance.ironDeposit;
        }
        
        return null;
    }

    public void PrepObject(GameObject _gameObject)
    {
        /*
        if (type == Type.COUNTER_ATTACK)
        {
            // Start by turning off FX emission.
            SetEmission(_gameObject, false);
        }
        */
        SetMaterialFlag(_gameObject);
    }

    public float GetPosition(int _block_x)
    {
        //Debug.Log("_block_x" + _block_x + ", range_min: " + range_min + ", range_max: " + range_max + ", MapView.instance.mapDimX: " + MapView.instance.mapDimX);
        if (range_min == range_max) {
            return 0;
        }

        return (((float)_block_x / (float)(MapView.instance.mapDimX - 1)) - range_min) / (range_max - range_min);
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

    // Materials are recursively cloned by SetMaterialFlag(), and those clones must be explicitly destroyed, to avoid a memory leak.
    public static void DestroyAllMaterials(GameObject _gameObject)
    {
        //// Regularly calling UnloadUnusedAssets() instead. The below code may have caused some crashes, not sure.
        
        // Destroy each of this object's materials.
        Renderer rend = _gameObject.GetComponent<Renderer>();
        if (rend != null)
        {
            foreach (Material material in rend.materials) {
                UnityEngine.Object.Destroy(material);
            }
        }
  
        // Call this method recursively for all child objects.
        foreach (Transform child in _gameObject.transform) {
            DestroyAllMaterials(child.gameObject);
        }
    }

    /*
    public void SetEmission(GameObject _gameObject, bool _enable)
    {
        //Debug.Log("Test: " + object0.name);
        //Debug.Log("Test: " + object0.transform.GetChild(0).GetChild(0).GetChild(0).gameObject.name);
        ParticleSystem.EmissionModule em = _gameObject.transform.GetChild(0).GetChild(0).GetChild(0).gameObject.GetComponent<ParticleSystem>().emission;
        em.enabled = _enable;
        em = _gameObject.transform.GetChild(0).GetChild(0).GetChild(0).GetChild(0).gameObject.GetComponent<ParticleSystem>().emission;
        em.enabled = _enable;
    }
    */
}

