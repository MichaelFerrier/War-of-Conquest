using UnityEngine;
using System;
using System.Linq;
using System.Collections;
using System.Collections.Generic;
using System.IO;

public class LeagueData : FileData
{
    public int raid_reward_credits, raid_reward_xp, raid_reward_rebirth;
	public int defense_daily_credits, defense_daily_xp, defense_daily_rebirth;

	public static List<LeagueData> leagues = new List<LeagueData>();
    
    public static LeagueData GetLeagueData(int _index)
	{
		if (_index >= leagues.Count) {
			_index = leagues.Count - 1;
		}

		if (_index < 0) 
		{
			Debug.Log("ERROR: GetLeagueData() called for league index " + _index);
			return null;
		}

		return leagues[_index];
	}

	public static Boolean LoadLeagues()
	{
        int line_num = -1;
		LeagueData league_data;
		int[] place = new int[1];
        String version_string, value_string;

        Debug.Log("LeagueData.LoadLeagues() location: " + Application.persistentDataPath + "/leagues.tsv");

        // Return false if the file doesn't exist on the client.
        if (File.Exists(Application.persistentDataPath + "/leagues.tsv") == false) {
            return false;
        }

        // Clear data from any previous load.
        leagues.Clear();

        var lines = File.ReadAllLines(Application.persistentDataPath + "/leagues.tsv");
        foreach (var line in lines)
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
			if (line[0] == '#') {
				continue;
			}

			// Create a LeagueData object for this line's object.
			league_data = new LeagueData();

			// Start at the beginning of the line.
			place[0] = 0;

            // Skip index.
            GetNextTabSeparatedValue(line, place);

            // raid_reward_credits
   			value_string = GetNextTabSeparatedValue(line, place);
			league_data.raid_reward_credits = (value_string.Length == 0) ? 0 : Int32.Parse(value_string);

            // raid_reward_xp
   			value_string = GetNextTabSeparatedValue(line, place);
			league_data.raid_reward_xp = (value_string.Length == 0) ? 0 : Int32.Parse(value_string);

            // raid_reward_rebirth
   			value_string = GetNextTabSeparatedValue(line, place);
			league_data.raid_reward_rebirth = (value_string.Length == 0) ? 0 : Int32.Parse(value_string);

            // defense_daily_credits
   			value_string = GetNextTabSeparatedValue(line, place);
			league_data.defense_daily_credits = (value_string.Length == 0) ? 0 : Int32.Parse(value_string);

            // defense_daily_xp
   			value_string = GetNextTabSeparatedValue(line, place);
			league_data.defense_daily_xp = (value_string.Length == 0) ? 0 : Int32.Parse(value_string);

            // defense_daily_rebirth
   			value_string = GetNextTabSeparatedValue(line, place);
			league_data.defense_daily_rebirth = (value_string.Length == 0) ? 0 : Int32.Parse(value_string);

            if (league_data.raid_reward_xp <= 0) 
			{
				Debug.Log("League with missing data at line " + line_num);
                leagues.Clear();
				return false;
			}

			// Add this league to the leagues list.
			leagues.Add(league_data);
		}

		return (leagues.Count > 0);		
	}
}

