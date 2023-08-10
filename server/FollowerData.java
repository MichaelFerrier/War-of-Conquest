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

public class FollowerData
{
	int userID, initDay;
	float bonusXP, bonusCredits;

	public FollowerData()
	{
	}

	public FollowerData(int _userID, int _initDay, float _bonusXP, float _bonusCredits)
	{
		userID = _userID;
		initDay = _initDay;
		bonusXP = _bonusXP;
		bonusCredits = _bonusCredits;
	}

	public JSONObject ToJSONObject()
	{
		JSONObject obj= new JSONObject();
		obj.put("userID", userID);
		obj.put("initDay", initDay);
		obj.put("bonusXP", bonusXP);
		obj.put("bonusCredits", bonusCredits);

		return obj;
	}

	public void FromJSONObject(JSONObject _obj)
	{
		userID = (int)(long) _obj.get("userID");
		initDay = (int)(long) _obj.get("initDay");
		bonusXP = (float)(double) _obj.get("bonusXP");
		bonusCredits = (float)(double) _obj.get("bonusCredits");
	}

	public static String FollowerDataArrayToJSON(ArrayList<FollowerData> _array)
	{
		JSONArray json = new JSONArray();
		int size = _array.size();
		for (int i = 0; i < size; i++)
		{
			json.add(_array.get(i).ToJSONObject());
		}

		return json.toJSONString();
	}

	public static ArrayList<FollowerData> JSONToFollowerDataArray(String _json)
	{
		FollowerData new_obj;
		ArrayList<FollowerData> return_array = new ArrayList<FollowerData>();

		if (_json == null) {Output.PrintToScreen("JSONToFollowerDataArray() passed null _json String."); return return_array;}

		JSONArray json_array = (JSONArray)(JSONValue.parse(_json));

		for (int i = 0; i < json_array.size(); i++)
		{
			new_obj = new FollowerData();
			new_obj.FromJSONObject((JSONObject) json_array.get(i));
			return_array.add(new_obj);
		}

		return return_array;
	}
}
