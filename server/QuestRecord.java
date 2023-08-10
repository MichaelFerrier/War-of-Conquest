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

public class QuestRecord
{
	int ID = 0;
	int cur_amount = 0;
	int completed = 0, collected = 0;

	public JSONObject ToJSONObject()
	{
		JSONObject obj= new JSONObject();
		obj.put("ID", ID);
		obj.put("cur_amount", cur_amount);
		obj.put("completed", completed);
		obj.put("collected", collected);

		return obj;
	}

	public void FromJSONObject(JSONObject _obj)
	{
		ID = (int)(long) _obj.get("ID");
		cur_amount = (int)(long) _obj.get("cur_amount");
		completed = (int)(long)_obj.get("completed");
		collected = (int)(long)_obj.get("collected");
	}

	public static String QuestRecordMapToJSON(HashMap<Integer,QuestRecord> _map)
	{
		JSONArray json = new JSONArray();
		for (QuestRecord value : _map.values())
		{
			json.add(value.ToJSONObject());
		}

		return json.toJSONString();
	}

	public static HashMap<Integer,QuestRecord> JSONToQuestRecordMap(String _json)
	{
		QuestRecord new_obj;
		HashMap<Integer,QuestRecord> return_map = new HashMap<Integer,QuestRecord>();

		if (_json == null) {Output.PrintToScreen("JSONToQuestRecordMap() passed null _json String."); return return_map;}

		JSONArray json_array = (JSONArray)(JSONValue.parse(_json));

		for (int i = 0; i < json_array.size(); i++)
		{
			new_obj = new QuestRecord();
			new_obj.FromJSONObject((JSONObject) json_array.get(i));
			return_map.put(new_obj.ID, new_obj);
		}

		return return_map;
	}
}
