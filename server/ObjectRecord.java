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

public class ObjectRecord
{
	int landmapID, blockX, blockY;
	int objectID;

	public ObjectRecord()
	{
		landmapID = -1;
		blockX = -1;
		blockY = -1;
		objectID = -1;
	}

	public ObjectRecord(int _landmapID, int _blockX, int _blockY, int _objectID)
	{
		landmapID = _landmapID;
		blockX = _blockX;
		blockY = _blockY;
		objectID = _objectID;
	}

	public JSONObject ToJSONObject()
	{
		JSONObject obj= new JSONObject();
		obj.put("landmapID", landmapID);
		obj.put("blockX", blockX);
		obj.put("blockY", blockY);
		obj.put("objectID", objectID);

		return obj;
	}

	public void FromJSONObject(JSONObject _obj)
	{
		landmapID = _obj.containsKey("landmapID") ? (int)(long) _obj.get("landmapID") : Constants.MAINLAND_MAP_ID; // Old ObjectRecords do not include landmapID field.
		blockX = (int)(long) _obj.get("blockX");
		blockY = (int)(long) _obj.get("blockY");
		objectID = (int)(long) _obj.get("objectID");
	}

	public static String ObjectRecordArrayToJSON(ArrayList<ObjectRecord> _array)
	{
		JSONArray json = new JSONArray();
		int size = _array.size();
		for (int i = 0; i < size; i++)
		{
			json.add(_array.get(i).ToJSONObject());
		}

		return json.toJSONString();
	}

	public static ArrayList<ObjectRecord> JSONToObjectRecordArray(String _json)
	{
		ObjectRecord new_obj;
		ArrayList<ObjectRecord> return_array = new ArrayList<ObjectRecord>();

		if (_json == null) {Output.PrintToScreen("JSONToObjectRecordArray() passed null _json String."); return return_array;}

		JSONArray json_array = (JSONArray)(JSONValue.parse(_json));

		for (int i = 0; i < json_array.size(); i++)
		{
			new_obj = new ObjectRecord();
			new_obj.FromJSONObject((JSONObject) json_array.get(i));
			return_array.add(new_obj);
		}

		return return_array;
	}
}
