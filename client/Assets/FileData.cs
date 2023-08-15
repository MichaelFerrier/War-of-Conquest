using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class FileData
{
    
	public enum DataFileType
    {
        Tech,
        Build,
        Object,
        Quest,
        League,
        Emblem
    }

    
	
	public static bool LoadDataFile(DataFileType _type)
    {
        switch (_type)
        {
            case DataFileType.Tech: return TechData.LoadTechnologies();
            case DataFileType.Build: return BuildData.LoadBuilds();
            case DataFileType.Object: return ObjectData.LoadObjects();
            case DataFileType.Quest: return QuestData.LoadQuests();
            case DataFileType.League: return LeagueData.LoadLeagues();
            case DataFileType.Emblem: return MapView.instance.LoadEmblems();
        }
        return false;
   
	}

    
			
	public static string GetNextTabSeparatedValue(string _line, int [] _place)
	{
		string return_val = "";
		int next_tab_index = _line.IndexOf('\t', _place[0]);

		if (next_tab_index == -1)
		{
			if (_place[0] < _line.Length)
			{
				return_val = _line.Substring(_place[0]);
				_place[0] = _line.Length;
			}
		}
		else
		{
			if (next_tab_index > _place[0])
			{
				return_val = _line.Substring(_place[0], next_tab_index - _place[0]);
			}

			_place[0] = next_tab_index + 1;
		}

		return return_val;
	}
}
