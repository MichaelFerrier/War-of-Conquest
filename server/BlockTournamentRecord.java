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

class BlockTournamentRecord
{
	int winning_nationID = -1;
	int losing_nationID = -1;
	float trophies_transferred = 0;
	float trophies_actualized = 0;
	int expire_time = 0;

	public BlockTournamentRecord(int _winning_nationID, int _losing_nationID, float _trophies_transferred, float _trophies_actualized, int _expire_time)
	{
		winning_nationID = _winning_nationID;
		losing_nationID = _losing_nationID;
		trophies_transferred = _trophies_transferred;
		trophies_actualized = _trophies_actualized;
		expire_time = _expire_time;
	}
}
