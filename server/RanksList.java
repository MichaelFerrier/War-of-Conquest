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
import java.net.URLEncoder;
import java.net.URLDecoder;
import org.json.simple.*;
import WOCServer.*;

public class RanksList implements JSONable
{
	ArrayList<Integer> IDs = new ArrayList<Integer>();
	ArrayList<String> names = new ArrayList<String>();
	ArrayList<Float> amounts = new ArrayList<Float>();

	public String ToJSON()
	{
		JSONObject obj= new JSONObject();
		obj.put("IDs", BaseData.IntArrayToJSON(IDs));
		obj.put("names", BaseData.StringArrayToJSON(names));
		obj.put("amounts", BaseData.FloatArrayToJSON(amounts));

		try {
			return URLEncoder.encode(obj.toJSONString(), "UTF-8");
		}
	  catch(Exception e) {
      Output.PrintToScreen("Couldn't convert RanksList to json.");
			Output.PrintException(e);
		}

		return "";
	}

	public void FromJSON(String _json)
	{
		if (_json == null)
		{
			Output.PrintToScreen("RanksList.FromJSON() passed null _json String.");
			return;
		}

		try {
			JSONObject obj=(JSONObject)(JSONValue.parse(URLDecoder.decode(_json, "UTF-8")));
			IDs = BaseData.JSONToIntArray((String) obj.get("IDs"));
			names = BaseData.JSONToStringArray((String) obj.get("names"));
			amounts = BaseData.JSONToFloatArray((String) obj.get("amounts"));
		}
	  catch(Exception e) {
      Output.PrintToScreen("Couldn't convert json to RanksList.");
			Output.PrintException(e);
		}
	}

	public static String RanksListArrayToJSON(ArrayList<RanksList> _array)
	{
		JSONArray json = new JSONArray();
		int size = _array.size();
		for (int i = 0; i < size; i++)
		{
			json.add(_array.get(i).ToJSON());
		}

		return json.toJSONString();
	}

	public static ArrayList<RanksList> JSONToRanksListArray(String _json)
	{
		RanksList new_obj;
		ArrayList<RanksList> return_array = new ArrayList<RanksList>();
		JSONArray json_array = (JSONArray)(JSONValue.parse(_json));
		for (int i = 0; i < json_array.size(); i++)
		{
			new_obj = new RanksList();
			new_obj.FromJSON((String) json_array.get(i));
			return_array.add(new_obj);
		}

		return return_array;
	}

	public void RemoveRank(int _ID)
	{
		// Determine the index of the given _ID in the ranks list, if it is already in the list.
		int index = IDs.indexOf(_ID);

		if (index != -1)
		{
			// Remove the given index from the IDs, names, and amounts lists.
			IDs.remove(index);
			names.remove(index);
			amounts.remove(index);
		}
	}

	public void UpdateRanks(int _ID, String _name, float _amount, int _max_list_length, boolean _allow_value_decrease)
	{
		// Determine the index of the given _ID in the ranks list, if it is already in the list.
		int index = IDs.indexOf(_ID);

		// Determine position at which given _amount should be inserted in the list.
		int insertion_index = 0;
		if ((amounts.size() > 0) && (amounts.get(amounts.size() - 1) <= _amount))
		{
			// Determine the position in the list where this record should be added.
			for (insertion_index = 0; insertion_index < amounts.size(); insertion_index++)
			{
				if (_amount >= amounts.get(insertion_index)) {
					break;
				}
			}
		}
		else
		{
			insertion_index = amounts.size();
		}

		//Output.PrintToScreen("UpdateRanks() ID: " + _ID + ", _amount: " + _amount + ", index: " + index + ", insertion_index: " + insertion_index + ", prev value: " + ((index == -1) ? -1 : amounts.get(index)));

		// If the given _ID is already at the correct position in the list, and the amount isn't changing integer value, do nothing else.
		if ((index == insertion_index) && (index != -1) && (amounts.get(index).intValue() == (int)_amount)) {
			return;
		}

		// If the given _ID is already in the ranks list...
		if (index != -1)
		{
			// If the new value is lower than the recorded value, and value decreases are not allowed, then don't change anything. Return.
			if ((_allow_value_decrease == false) && (amounts.get(index) > _amount)) {
				return;
			}

			// The amount has changed, so remove this _ID's records from the ranks list, in preparation for re-adding it.
			IDs.remove(index);
			names.remove(index);
			amounts.remove(index);

			// Adjust the insertion_index for the removal of the orginal entry, if necessary.
			if (index < insertion_index) {
				insertion_index--;
			}
		}

		// If the ranks list is full, and the given _amount is less than the amount of the final record in the list, do nothing; just return.
		if ((amounts.size() >= _max_list_length) && (amounts.get(amounts.size() - 1) > _amount)) {
			return;
		}

		// If the last amount in the list is greater than the given _amount, but there is room to add to the end of the list,
		// add the given record at the end of the list, and return.
		if ((amounts.size() < _max_list_length) && ((amounts.size() == 0) || (amounts.get(amounts.size() - 1) > _amount)))
		{
			IDs.add(_ID);
			names.add(_name);
			amounts.add(_amount);
			return;
		}

		// Add this record at the determined position.
		IDs.add(insertion_index, _ID);
		names.add(insertion_index, _name);
		amounts.add(insertion_index, _amount);
	}

	public void Sort()
	{
		boolean change_made;

		do
		{
			change_made = false;
			for (int i = 0; i < (amounts.size() - 1); i++)
			{
				if (amounts.get(i) < amounts.get(i+1))
				{
					amounts.add(i+2, amounts.get(i));
					amounts.remove(i);
					IDs.add(i+2, IDs.get(i));
					IDs.remove(i);
					names.add(i+2, names.get(i));
					names.remove(i);
					change_made = true;
				}
			}
		}
		while (change_made);
	}

	public void Clear()
	{
		IDs.clear();
		names.clear();
		amounts.clear();
	}
}
