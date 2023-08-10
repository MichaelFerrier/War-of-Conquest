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

public class MessageData
{
	int time = 0;
	int userID = 0, nationID = 0, deviceID = 0;
	String username, nation_name, timestamp, text;
  int reported = 0;

	public JSONObject ToJSONObject()
	{
		JSONObject obj= new JSONObject();
		obj.put("time", time);
		obj.put("userID", userID);
		obj.put("nationID", nationID);
		obj.put("deviceID", deviceID);
		obj.put("username", username.replace("\"", "\\\""));
		obj.put("nation_name", nation_name.replace("\"", "\\\""));
		obj.put("timestamp", timestamp);
		obj.put("text", text.replace("\"", "\\\""));
		obj.put("reported", reported);

		return obj;
	}

	public void FromJSONObject(JSONObject _obj)
	{
		time = (int)(long) _obj.get("time");
		userID = (int)(long) _obj.get("userID");
		nationID = (int)(long) _obj.get("nationID");
		deviceID = _obj.containsKey("deviceID") ? (int)(long) _obj.get("deviceID") : 0; // Old messages do not include deviceID field.
		username = ((String) _obj.get("username")).replace("\\\"", "\"");
		nation_name = ((String) _obj.get("nation_name")).replace("\\\"", "\"");
		timestamp = (String) _obj.get("timestamp");
		text = ((String) _obj.get("text")).replace("\\\"", "\"");
		reported = (int)(long) _obj.get("reported");
	}

	public static String MessageDataArrayToJSON(ArrayList<MessageData> _array)
	{
		JSONArray json = new JSONArray();
		int size = _array.size();
		for (int i = 0; i < size; i++)
		{
			json.add(_array.get(i).ToJSONObject());
		}

		return json.toJSONString();
	}

	public static ArrayList<MessageData> JSONToMessageDataArray(String _json)
	{
		MessageData new_obj;
		ArrayList<MessageData> return_array = new ArrayList<MessageData>();

		if (_json == null) {Output.PrintToScreen("JSONToMessageDataArray() passed null _json String."); return return_array;}

		JSONArray json_array = (JSONArray)(JSONValue.parse(_json));

		for (int i = 0; i < json_array.size(); i++)
		{
			new_obj = new MessageData();
			new_obj.FromJSONObject((JSONObject) json_array.get(i));
			return_array.add(new_obj);
		}

		return return_array;
	}
}
