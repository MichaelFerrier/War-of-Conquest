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

import java.io.*;
import WOCServer.*;
import java.util.*;

public class ClientString
{
	static final int FORMAT_ID_LENGTH = 256;
	static final int PARAM_ID_LENGTH = 64;
	static final int PARAM_VAL_LENGTH = 64;
	static final int MAX_NUM_PARAMS = 6;

	public static Vector<ClientString> free_list = new Vector<ClientString>();

	StringBuilder format_id = new StringBuilder(FORMAT_ID_LENGTH);
	StringBuilder[] param_ids = new StringBuilder[MAX_NUM_PARAMS];
	StringBuilder[] param_vals = new StringBuilder[MAX_NUM_PARAMS];
	int num_params = 0;

	public static ClientString Get()
	{
		// Create a new, or get an existing, ClientString.
		ClientString cstring;
		if (free_list.size() > 0) {
			cstring = free_list.remove(free_list.size() - 1);
		} else {
			cstring = new ClientString();
		}

		// Clear the ClientString
		cstring.format_id.setLength(0);
		cstring.num_params = 0;

		return cstring;
	}

	public static ClientString Get(String _format_id)
	{
		ClientString cstring = Get();
		cstring.SetString(_format_id);
		return cstring;
	}

	public static ClientString Get(String _format_id, String _param_id_0, String _param_val_0)
	{
		ClientString cstring = Get();
		cstring.SetString(_format_id, _param_id_0, _param_val_0);
		return cstring;
	}

	public static ClientString Get(String _format_id, String _param_id_0, String _param_val_0, String _param_id_1, String _param_val_1)
	{
		ClientString cstring = Get();
		cstring.SetString(_format_id, _param_id_0, _param_val_0, _param_id_1, _param_val_1);
		return cstring;
	}

	public static ClientString Get(String _format_id, String _param_id_0, String _param_val_0, String _param_id_1, String _param_val_1, String _param_id_2, String _param_val_2)
	{
		ClientString cstring = Get();
		cstring.SetString(_format_id, _param_id_0, _param_val_0, _param_id_1, _param_val_1, _param_id_2, _param_val_2);
		return cstring;
	}

	public static ClientString Get(String _format_id, String _param_id_0, String _param_val_0, String _param_id_1, String _param_val_1, String _param_id_2, String _param_val_2, String _param_id_3, String _param_val_3)
	{
		ClientString cstring = Get();
		cstring.SetString(_format_id, _param_id_0, _param_val_0, _param_id_1, _param_val_1, _param_id_2, _param_val_2, _param_id_3, _param_val_3);
		return cstring;
	}

	public static ClientString Get(String _format_id, String _param_id_0, String _param_val_0, String _param_id_1, String _param_val_1, String _param_id_2, String _param_val_2, String _param_id_3, String _param_val_3, String _param_id_4, String _param_val_4)
	{
		ClientString cstring = Get();
		cstring.SetString(_format_id, _param_id_0, _param_val_0, _param_id_1, _param_val_1, _param_id_2, _param_val_2, _param_id_3, _param_val_3, _param_id_4, _param_val_4);
		return cstring;
	}

	public static ClientString Get(String _format_id, String _param_id_0, String _param_val_0, String _param_id_1, String _param_val_1, String _param_id_2, String _param_val_2, String _param_id_3, String _param_val_3, String _param_id_4, String _param_val_4, String _param_id_5, String _param_val_5)
	{
		ClientString cstring = Get();
		cstring.SetString(_format_id, _param_id_0, _param_val_0, _param_id_1, _param_val_1, _param_id_2, _param_val_2, _param_id_3, _param_val_3, _param_id_4, _param_val_4, _param_id_5, _param_val_5);
		return cstring;
	}

	public static void Release(ClientString _client_string)
	{
		// Add the _client_string to the free_list.
		free_list.add(_client_string);
	}

	public ClientString()
	{
		for (int i = 0; i < MAX_NUM_PARAMS; i++)
		{
			param_ids[i] = new StringBuilder(PARAM_ID_LENGTH);
			param_vals[i] = new StringBuilder(PARAM_VAL_LENGTH);
		}
	}

	public boolean IsEmpty()
	{
		return (format_id.length() == 0);
	}

	public void SetString(String _format_id)
	{
		num_params = 0;
		format_id.setLength(0);
		format_id.append(_format_id);
	}

	public void SetString(String _format_id, String _param_id_0, String _param_val_0)
	{
		num_params = 0;
		format_id.setLength(0);
		format_id.append(_format_id);
		AddParam(_param_id_0, _param_val_0);
	}

	public void SetString(String _format_id, String _param_id_0, String _param_val_0, String _param_id_1, String _param_val_1)
	{
		num_params = 0;
		format_id.setLength(0);
		format_id.append(_format_id);
		AddParam(_param_id_0, _param_val_0);
		AddParam(_param_id_1, _param_val_1);
	}

	public void SetString(String _format_id, String _param_id_0, String _param_val_0, String _param_id_1, String _param_val_1, String _param_id_2, String _param_val_2)
	{
		num_params = 0;
		format_id.setLength(0);
		format_id.append(_format_id);
		AddParam(_param_id_0, _param_val_0);
		AddParam(_param_id_1, _param_val_1);
		AddParam(_param_id_2, _param_val_2);
	}

	public void SetString(String _format_id, String _param_id_0, String _param_val_0, String _param_id_1, String _param_val_1, String _param_id_2, String _param_val_2, String _param_id_3, String _param_val_3)
	{
		num_params = 0;
		format_id.setLength(0);
		format_id.append(_format_id);
		AddParam(_param_id_0, _param_val_0);
		AddParam(_param_id_1, _param_val_1);
		AddParam(_param_id_2, _param_val_2);
		AddParam(_param_id_3, _param_val_3);
	}

	public void SetString(String _format_id, String _param_id_0, String _param_val_0, String _param_id_1, String _param_val_1, String _param_id_2, String _param_val_2, String _param_id_3, String _param_val_3, String _param_id_4, String _param_val_4)
	{
		num_params = 0;
		format_id.setLength(0);
		format_id.append(_format_id);
		AddParam(_param_id_0, _param_val_0);
		AddParam(_param_id_1, _param_val_1);
		AddParam(_param_id_2, _param_val_2);
		AddParam(_param_id_3, _param_val_3);
		AddParam(_param_id_4, _param_val_4);
	}

	public void SetString(String _format_id, String _param_id_0, String _param_val_0, String _param_id_1, String _param_val_1, String _param_id_2, String _param_val_2, String _param_id_3, String _param_val_3, String _param_id_4, String _param_val_4, String _param_id_5, String _param_val_5)
	{
		num_params = 0;
		format_id.setLength(0);
		format_id.append(_format_id);
		AddParam(_param_id_0, _param_val_0);
		AddParam(_param_id_1, _param_val_1);
		AddParam(_param_id_2, _param_val_2);
		AddParam(_param_id_3, _param_val_3);
		AddParam(_param_id_4, _param_val_4);
		AddParam(_param_id_5, _param_val_5);
	}

	public void AddParam(String _param_id, String _param_val)
	{
		if (num_params == MAX_NUM_PARAMS) {
			throw new RuntimeException("ClientString AddParam(): attempt to add param (" + _param_id + "," + _param_val + ") when there are already MAX_NUM_PARAMS.");
		}

		param_ids[num_params].setLength(0);
		param_ids[num_params].append(_param_id);

		param_vals[num_params].setLength(0);
		param_vals[num_params].append(_param_val);

		num_params++;
	}

	public String GetFormatID()
	{
		return format_id.toString();
	}

	public void Encode(StringBuffer _output_buffer)
	{
		Constants.EncodeString(_output_buffer, format_id.toString());

		if (format_id.length() > 0)
		{
			Constants.EncodeUnsignedNumber(_output_buffer, num_params, 1);
			for (int i = 0; i < num_params; i++)
			{
				Constants.EncodeString(_output_buffer, param_ids[i].toString());
				Constants.EncodeString(_output_buffer, param_vals[i].toString());
			}
		}
	}

	public String GetJSON()
	{
		String result = "{\n\"ID\": \"" + format_id.toString() + "\"";
		for (int i = 0; i < num_params; i++) {
			result += ",\n\"param_id_" + i + "\": \"" + param_ids[i].toString() + "\"";
			result += ",\n\"param_val_" + i + "\": \"" + param_vals[i].toString() + "\"";
		}
		result += "\n}";
		return result;
	}
}
