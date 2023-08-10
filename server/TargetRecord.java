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
import java.util.*;
import WOCServer.*;

public class TargetRecord
{
	int x, y, newNationID, battle_flags;
	int full_hit_points, start_hit_points, end_hit_points, new_cur_hit_points, new_full_hit_points;
	int wipe_end_time, wipe_nationID, wipe_flags;
	float hit_points_rate;

	public TargetRecord(int _x, int _y, int _newNationID, int _full_hit_points, int _start_hit_points, int _end_hit_points, int _new_cur_hit_points, int _new_full_hit_points, float _hit_points_rate, int _battle_flags, int _wipe_end_time, int _wipe_nationID, int _wipe_flags)
	{
		x = _x;
		y = _y;
		newNationID = _newNationID;
		full_hit_points = _full_hit_points;
		start_hit_points = _start_hit_points;
		end_hit_points = _end_hit_points;
		new_cur_hit_points = _new_cur_hit_points;
		new_full_hit_points = _new_full_hit_points;
		hit_points_rate = _hit_points_rate;
		battle_flags = _battle_flags;
		wipe_end_time = _wipe_end_time;
		wipe_nationID = _wipe_nationID;
		wipe_flags = _wipe_flags;
	}
}
