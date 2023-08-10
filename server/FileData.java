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

import WOCServer.*;

public class FileData
{
	public enum DataFileType
	{
		Tech,
		Build,
		Object,
		Quest,
		League
	}

	public static boolean LoadDataFile(DataFileType _type)
	{
			switch (_type)
			{
					case Tech: return TechData.LoadTechnologies();
					case Build: return BuildData.LoadBuilds();
					case Object: return ObjectData.LoadObjects();
					case Quest: return QuestData.LoadQuests();
					case League: return LeagueData.LoadLeagues();
			}
			return false;
	}
}
