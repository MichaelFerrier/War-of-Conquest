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

class Footprint
{
	int x0, x1, y0, y1;
	int min_x0, min_y0, max_x1, max_y1, max_x0;
	int area, border_area, perimeter;
	float geo_efficiency_base;
	float manpower, energy_burn_rate;
	int prev_buy_manpower_day;
	int buy_manpower_day_amount;

	public Footprint() {x0 = -1; x1 = -1; y0 = -1; y1 = -1; min_x0 = -1; min_y0 = -1; max_x1 = -1; max_y1 = -1; area = 0; border_area = 0; perimeter = 0; geo_efficiency_base = 0f; manpower = 0f; energy_burn_rate = 0f; prev_buy_manpower_day = 0; buy_manpower_day_amount = 0;}

	public void Reset()
	{
		area = 0;
		border_area = 0;
		perimeter = 0;
		x0 = Constants.MAX_MAP_DIM;
		x1 = 0;
		y0 = Constants.MAX_MAP_DIM;
		y1 = 0;
	}

	public void Copy(Footprint _original)
	{
		area = _original.area;
		border_area = _original.border_area;
		perimeter = _original.perimeter;
		geo_efficiency_base = _original.geo_efficiency_base;
		manpower = _original.manpower;
		energy_burn_rate = _original.energy_burn_rate;
		prev_buy_manpower_day = _original.prev_buy_manpower_day;
		buy_manpower_day_amount = _original.buy_manpower_day_amount;
		x0 = _original.x0;
		x1 = _original.x1;
		y0 = _original.y0;
		y1 = _original.y1;
	}
}
