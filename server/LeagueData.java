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

import java.sql.*;
import java.util.Iterator;
import java.util.Map;
import java.util.ArrayList;
import java.util.Vector;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.FileReader;
import WOCServer.*;

public class LeagueData
{
	int raid_reward_credits, raid_reward_xp, raid_reward_rebirth;
	int defense_daily_credits, defense_daily_xp, defense_daily_rebirth;

	public static ArrayList<LeagueData> leagues = new ArrayList<LeagueData>();

	public static LeagueData GetLeagueData(int _index)
	{
		if (_index >= leagues.size()) {
			_index = leagues.size() - 1;
		}

		if (_index < 0)
		{
			Output.PrintToScreen("ERROR: GetLeagueData() called for league index " + _index);
			return null;
		}

		return leagues.get(_index);
	}

	public static Boolean LoadLeagues()
	{
		int line_num = -1;
		BufferedReader br;
		String filename = "leagues.tsv";

		try
		{
			br = new BufferedReader(new FileReader(filename));
		}
		catch (Exception e)
		{
			Output.PrintToScreen("File not found: " + filename);
			return false;
		}

		try
		{
			LeagueData league_data;
			String line, value_string, version_string;
			int[] place = new int[1];

			while ((line = br.readLine()) != null)
			{
				// Increment line number.
				line_num++;

				// Read the version line
				if (line_num == 0)
				{
					version_string = line;
					continue;
				}

				// Process the line.

				// Skip comments
				if (line.charAt(0) == '#') {
					continue;
				}

				// Create a LeagueData object for this line's object.
				league_data = new LeagueData();

				// Start at the beginning of the line.
				place[0] = 0;

				// Skip index
				Constants.GetNextTabSeparatedValue(line, place);

				// raid_reward_credits
				value_string = Constants.GetNextTabSeparatedValue(line, place);
				league_data.raid_reward_credits = value_string.isEmpty() ? 0 : Integer.parseInt(value_string);

				// raid_reward_xp
				value_string = Constants.GetNextTabSeparatedValue(line, place);
				league_data.raid_reward_xp = value_string.isEmpty() ? 0 : Integer.parseInt(value_string);

				// raid_reward_rebirth
				value_string = Constants.GetNextTabSeparatedValue(line, place);
				league_data.raid_reward_rebirth = value_string.isEmpty() ? 0 : Integer.parseInt(value_string);

				// defense_daily_credits
				value_string = Constants.GetNextTabSeparatedValue(line, place);
				league_data.defense_daily_credits = value_string.isEmpty() ? 0 : Integer.parseInt(value_string);

				// defense_daily_xp
				value_string = Constants.GetNextTabSeparatedValue(line, place);
				league_data.defense_daily_xp = value_string.isEmpty() ? 0 : Integer.parseInt(value_string);

				// defense_daily_rebirth
				value_string = Constants.GetNextTabSeparatedValue(line, place);
				league_data.defense_daily_rebirth = value_string.isEmpty() ? 0 : Integer.parseInt(value_string);

				if (league_data.raid_reward_xp <= 0)
				{
					Output.PrintToScreen("League with missing data at line " + line_num);
					return false;
				}

				// Add this league to the leagues ArrayList.
				leagues.add(league_data);
			}
		}
		catch (Exception e)
		{
			Output.PrintToScreen("Error loading leagues at line " + line_num);
			Output.PrintException(e);
			return false;
		}

		return (leagues.size() > 0);
	}
}
