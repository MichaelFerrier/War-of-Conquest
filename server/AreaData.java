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

public class AreaData
{
	int gridX, gridY;
	int nationX, nationY;
	int sqrDist;
	boolean visible = false;

	public JSONObject ToJSONObject()
	{
		JSONObject obj= new JSONObject();
		obj.put("gridX", gridX);
		obj.put("gridY", gridY);
		obj.put("nationX", nationX);
		obj.put("nationY", nationY);
		obj.put("sqrDist", sqrDist);
		obj.put("visible", visible ? 1 : 0);

		return obj;
	}

	public void FromJSONObject(JSONObject _obj)
	{
		gridX = (int)(long) _obj.get("gridX");
		gridY = (int)(long) _obj.get("gridY");
		nationX = (int)(long) _obj.get("nationX");
		nationY = (int)(long) _obj.get("nationY");
		sqrDist = (int)(long) _obj.get("sqrDist");
		visible = (((int)(long) _obj.get("visible")) == 1);
	}

	public static String AreaDataArrayToJSON(ArrayList<AreaData> _array)
	{
		JSONArray json = new JSONArray();
		int size = _array.size();
		for (int i = 0; i < size; i++)
		{
			json.add(_array.get(i).ToJSONObject());
		}

		return json.toJSONString();
	}

	public static ArrayList<AreaData> JSONToAreaDataArray(String _json)
	{
		AreaData new_obj;
		ArrayList<AreaData> return_array = new ArrayList<AreaData>();

		if (_json == null) {Output.PrintToScreen("JSONToAreaDataArray() passed null _json String."); return return_array;}

		JSONArray json_array = (JSONArray)(JSONValue.parse(_json));

		for (int i = 0; i < json_array.size(); i++)
		{
			new_obj = new AreaData();
			new_obj.FromJSONObject((JSONObject) json_array.get(i));
			return_array.add(new_obj);
		}

		return return_array;
	}
}
