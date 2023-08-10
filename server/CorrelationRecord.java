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

public class CorrelationRecord
{
	int deviceID = 0;

	int count_interval_10m = 0;
	int count_interval_60s = 0;
	int count_interval_30s = 0;
	int count_interval_2s = 0;
	int count_interval_1s = 0;

	// Transient
	ClientThread clientThread;
	DeviceData deviceData;

	public JSONObject ToJSONObject()
	{
		JSONObject obj= new JSONObject();
		obj.put("deviceID", deviceID);
		obj.put("count_interval_10m", count_interval_10m);
		obj.put("count_interval_60s", count_interval_60s);
		obj.put("count_interval_30s", count_interval_30s);
		obj.put("count_interval_2s", count_interval_2s);
		obj.put("count_interval_1s", count_interval_1s);

		return obj;
	}

	public void FromJSONObject(JSONObject _obj)
	{
		deviceID = (int)(long) _obj.get("deviceID");
		count_interval_10m = (int)(long) _obj.get("count_interval_10m");
		count_interval_60s = (int)(long) _obj.get("count_interval_60s");
		count_interval_30s = (int)(long)_obj.get("count_interval_30s");
		count_interval_2s = (int)(long)_obj.get("count_interval_2s");
		count_interval_1s = (int)(long)_obj.get("count_interval_1s");
	}

	public static String CorrelationRecordMapToJSON(HashMap<Integer,CorrelationRecord> _map)
	{
		JSONArray json = new JSONArray();
		for (CorrelationRecord value : _map.values())
		{
			json.add(value.ToJSONObject());
		}

		return json.toJSONString();
	}

	public static HashMap<Integer,CorrelationRecord> JSONToCorrelationRecordMap(String _json)
	{
		CorrelationRecord new_obj;
		HashMap<Integer,CorrelationRecord> return_map = new HashMap<Integer,CorrelationRecord>();

		if (_json == null) {Output.PrintToScreen("JSONToCorrelationRecordMap() passed null _json String."); return return_map;}

		JSONArray json_array = (JSONArray)(JSONValue.parse(_json));

		for (int i = 0; i < json_array.size(); i++)
		{
			new_obj = new CorrelationRecord();
			new_obj.FromJSONObject((JSONObject) json_array.get(i));
			return_map.put(new_obj.deviceID, new_obj);
		}

		return return_map;
	}
}
