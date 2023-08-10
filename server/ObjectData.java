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

public class ObjectData
{
	public static final int TYPE_TECH       = 0;
	public static final int TYPE_ORB        = 1;

	public static final int RESOURCE_OBJECT_BASE_ID = 1000;
	public static final int ORB_BASE_ID = 2000;

	String name = "";
	int type = TYPE_TECH;
	int ID = -1, techID = -1, xp = 0;
	float payout_weight = 0, xp_per_hour = 0, frequency = 0, range_min = 0, range_max = 1;
	String description = "";

	public static HashMap<Integer,ObjectData> objects = new HashMap<Integer,ObjectData>();
	public static HashMap<String,Integer> object_name_map = new HashMap<String,Integer>();
	public static String version_string = "";

	public static ObjectData GetObjectData(int _ID)
	{
		return objects.getOrDefault(_ID, null);
	}

	public static int GetNameToIDMap(String _name)
	{
		return object_name_map.getOrDefault(_name, -1);
	}

	public static Boolean LoadObjects()
	{
		int line_num = -1;
		BufferedReader br;
		String filename = "objects.tsv";

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
			ObjectData object_data;
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

				// Create an ObjectData object for this line's object.
				object_data = new ObjectData();

				// Start at the beginning of the line.
				place[0] = 0;

				// Name
				object_data.name = Constants.GetNextTabSeparatedValue(line, place);

				// ID
				String ID_string = Constants.GetNextTabSeparatedValue(line, place);
				object_data.ID = ID_string.isEmpty() ? -1 : Integer.parseInt(ID_string);

				// Type
				String type_string = Constants.GetNextTabSeparatedValue(line, place);
				if (type_string.equalsIgnoreCase("tech")) {
					object_data.type = ObjectData.TYPE_TECH;
				} else if (type_string.equalsIgnoreCase("orb")) {
					object_data.type = ObjectData.TYPE_ORB;
				} else {
					Output.PrintToScreen("Unknown type '" + type_string + "' at line " + line_num + ".");
				}

				// Tech ID
				String tech_ID_string = Constants.GetNextTabSeparatedValue(line, place);
				object_data.techID = tech_ID_string.isEmpty() ? -1 : Integer.parseInt(tech_ID_string);

				// Payout weight
				String payout_weight_string = Constants.GetNextTabSeparatedValue(line, place);
				object_data.payout_weight = payout_weight_string.isEmpty() ? -1 : Float.parseFloat(payout_weight_string);

				// XP per hour
				String xp_per_hour_string = Constants.GetNextTabSeparatedValue(line, place);
				object_data.xp_per_hour = xp_per_hour_string.isEmpty() ? -1 : Float.parseFloat(xp_per_hour_string);

				// Frequency
				String frequency_string = Constants.GetNextTabSeparatedValue(line, place);
				object_data.frequency = frequency_string.isEmpty() ? -1 : Float.parseFloat(frequency_string);

				// Range Min
				String range_min_string = Constants.GetNextTabSeparatedValue(line, place);
				object_data.range_min = range_min_string.isEmpty() ? -1 : Float.parseFloat(range_min_string);

				// Range Max
				String range_max_string = Constants.GetNextTabSeparatedValue(line, place);
				object_data.range_max = range_max_string.isEmpty() ? -1 : Float.parseFloat(range_max_string);

				// xp
				String xp_string = Constants.GetNextTabSeparatedValue(line, place);
				object_data.xp = xp_string.isEmpty() ? -1 : Integer.parseInt(xp_string);

				// Description
				object_data.description = Constants.GetNextTabSeparatedValue(line, place);

				if (object_data.ID < 0)
				{
					Output.PrintToScreen("Object with missing ID at line " + line_num);
					return false;
				}

				// Add this object to the objects vector and object_name_map.
				objects.put(object_data.ID, object_data);
				object_name_map.put(object_data.name, object_data.ID);
			}
		}
		catch (Exception e)
		{
			Output.PrintToScreen("Error loading objects at line " + line_num);
			Output.PrintException(e);
			return false;
		}

		return (objects.size() > 0);
	}

	public float GetPositionInRange(int _x, int _y, LandMap _land_map)
	{
		if (_land_map.ID != Constants.MAINLAND_MAP_ID) {
			return 0;
		}

		if ((range_min < 0) || (range_min >= range_max)) {
			return 0;
		}

		return Math.min(1, Math.max(0, (((float)_x / (float)(_land_map.width - 1)) - range_min) / (range_max - range_min)));
	}
}
