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

public class TechPriceRecord
{
	int ID = 0;
	int price = 0, prev_price = 0, purchase_count = 0, play_time = 0;
	float prev_revenue_rate = 0f;

	public JSONObject ToJSONObject()
	{
		JSONObject obj= new JSONObject();
		obj.put("ID", ID);
		obj.put("price", price);
		obj.put("prev_price", prev_price);
		obj.put("purchase_count", purchase_count);
		obj.put("play_time", play_time);
		obj.put("prev_revenue_rate", prev_revenue_rate);

		return obj;
	}

	public void FromJSONObject(JSONObject _obj)
	{
		ID = (int)(long) _obj.get("ID");
		price = (int)(long) _obj.get("price");
		prev_price = (int)(long) _obj.get("prev_price");
		purchase_count = (int)(long) _obj.get("purchase_count");
		play_time = (int)(long) _obj.get("play_time");
		prev_revenue_rate = ((Double)_obj.get("prev_revenue_rate")).floatValue();
	}

	public static String TechPriceRecordMapToJSON(HashMap<Integer,TechPriceRecord> _map)
	{
		JSONArray json = new JSONArray();
		for (TechPriceRecord value : _map.values())
		{
			json.add(value.ToJSONObject());
		}

		return json.toJSONString();
	}

	public static HashMap<Integer,TechPriceRecord> JSONToTechPriceRecordMap(String _json)
	{
		TechPriceRecord new_obj;
		HashMap<Integer,TechPriceRecord> return_map = new HashMap<Integer,TechPriceRecord>();

		if (_json == null) {Output.PrintToScreen("JSONToQTechPriceRecordMap() passed null _json String."); return return_map;}

		JSONArray json_array = (JSONArray)(JSONValue.parse(_json));

		for (int i = 0; i < json_array.size(); i++)
		{
			new_obj = new TechPriceRecord();
			new_obj.FromJSONObject((JSONObject) json_array.get(i));
			return_map.put(new_obj.ID, new_obj);
		}

		return return_map;
	}
}
