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
import org.json.simple.*;
import WOCServer.*;

public class ContenderData
{
	int nationID = 0;

	// Transient data
	NationData nation_data = null;

	public ContenderData()
	{
		nationID = 0;
		nation_data = null;
	}

	public ContenderData(int _nationID, NationData _nationData)
	{
		nationID = _nationID;
		nation_data = _nationData;
	}

	public JSONObject ToJSONObject()
	{
		JSONObject obj= new JSONObject();
		obj.put("nationID", nationID);

		return obj;
	}

	public void FromJSONObject(JSONObject _obj)
	{
		nationID = (int)(long) _obj.get("nationID");
	}

	public static String ContenderDataArrayToJSON(ArrayList<ContenderData> _array)
	{
		JSONArray json = new JSONArray();
		int size = _array.size();
		for (int i = 0; i < size; i++)
		{
			json.add(_array.get(i).ToJSONObject());
		}

		return json.toJSONString();
	}

	public static ArrayList<ContenderData> JSONToContenderDataArray(String _json)
	{
		ContenderData new_obj;
		ArrayList<ContenderData> return_array = new ArrayList<ContenderData>();

		if (_json == null) {Output.PrintToScreen("JSONToContenderDataArray() passed null _json String."); return return_array;}

		JSONArray json_array = (JSONArray)(JSONValue.parse(_json));

		for (int i = 0; i < json_array.size(); i++)
		{
			new_obj = new ContenderData();
			new_obj.FromJSONObject((JSONObject) json_array.get(i));
			return_array.add(new_obj);
		}

		return return_array;
	}

	public NationData GetNationData()
	{
		if (nation_data == null)
		{
			// Get the nation data.
			nation_data = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);

			if (nation_data == null) {
				Output.PrintToScreen("ERROR: No NationData found for ContenderData with nationID " + nationID + ".");
			}
		}

		return nation_data;
	}
}
