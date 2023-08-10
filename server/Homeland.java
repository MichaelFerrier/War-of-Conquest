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

import java.lang.*;
import java.util.*;
import WOCServer.*;

public class Homeland
{
	public static ArrayList<Integer> homeland_maps = new ArrayList<Integer>();

	public static LandMap GetHomelandMap(int _nationID)
	{
		// Get the nation's data.
	  NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationID, false);

		if (nationData == null) {
			return null;
		}

		// If this nation doesn't yet have a homeland, create and return it.
		if (nationData.homeland_mapID <= 0) {
			return Homeland.CreateHomelandMap(nationData);
		}

		// Get the nation's homeland landmap.
		LandMap landmap = DataManager.GetLandMap(nationData.homeland_mapID, false);

		// If the nation has no area on their homeland map, place the nation there.
		if (nationData.homeland_footprint.area == 0)
		{
			// Place the nation near the center of their homeland.
			PlaceNation(nationData.ID, landmap);
		}

		return landmap;
	}

	public static LandMap CreateHomelandMap(NationData _nationData)
	{
		// Determine the nation's homeland map ID, and homeland source map ID.
		_nationData.homeland_mapID = _nationData.ID + 1000; // IMPORTANT: Adding 1000 so that mainland map, and source maps, are not overwritten by home island maps!

		if (homeland_maps.size() == 0) {
			Output.PrintToScreen("ERROR: No homeland maps found!");
		}

		// Determine the ID of the map to use as source.
		int sourceMapID = homeland_maps.get(_nationData.homeland_mapID % homeland_maps.size());

		// Mark the nation's data to be updated.
		DataManager.MarkForUpdate(_nationData);

		// Get the source landmap.
		LandMap source_landmap = DataManager.GetLandMap(sourceMapID, false);

		if (source_landmap == null)
		{
			Output.PrintToScreen("ERROR in CreateHomelandMap(): source LandMap with ID " + sourceMapID + " not found.");
			return null;
		}

		// Create the nation's homeland landmap.
		LandMap landmap = DataManager.GetLandMap(_nationData.homeland_mapID, true);
		Output.PrintToScreen("Home island " + _nationData.homeland_mapID + " with source " + sourceMapID + " created for nation " + _nationData.name + " (" + _nationData.ID + ")");

		// Fill in the new homeland map's LandMapInfo.
		landmap.info.sourceMapID = sourceMapID;
		landmap.info.skin = 1;
		DataManager.MarkForUpdate(landmap.info);

		// Set the new size of the land map, and insert its blocks into the database.
		landmap.SetSize(source_landmap.width, source_landmap.height, true);

		// Copy the block data from the source landmap to the new landmap.
		landmap.Copy(source_landmap);

		// Place the nation near the center of their homeland.
		PlaceNation(_nationData.ID, landmap);

		return landmap;
	}

	public static void PlaceNation(int _nationID, LandMap _homeland_map)
	{
		// Place the nation near the center of their homeland.
		World.PlaceNationWithinArea(_homeland_map, _nationID, (int)(_homeland_map.width * 0.25f), (int)(_homeland_map.height * 0.25f), (int)(_homeland_map.width * 0.75f), (int)(_homeland_map.height * 0.75f), -1, -1, true, null);

		// Get the nation's data.
	  NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, _nationID, false);

		if (nationData == null) {
			return;
		}

		// Center each of th nation's users' homeland view on the new placement position of the nation.
		for (int user_index = 0; user_index < nationData.users.size(); user_index++)
		{
			// Get the current member user's data
			UserData curUserData = (UserData)DataManager.GetData(Constants.DT_USER, nationData.users.get(user_index), false);

			// Set only the user's stored view position on the homeland map.
			Display.SetUserStoredView(curUserData, nationData.homeland_mapID, nationData.homeland_footprint.x0, nationData.homeland_footprint.y0);

			// Mark the user's data to be updated
			DataManager.MarkForUpdate(curUserData);
		}
	}

	public static void InitMapList()
	{
		for (int mapID = Constants.MAINLAND_MAP_ID + 1;; mapID++)
		{
			// Attempt to load the landmap with the current map ID.
			LandMap landmap = DataManager.GetLandMap(mapID, false);

			// If no landmap with this ID exists, exit the loop, the list is complete.
			if (landmap == null) {
				break;
			}

			// Add the current landmap's ID to the list of homeland maps.
			homeland_maps.add(mapID);
		}

		if (homeland_maps.size() == 0) {
			Output.PrintToScreen("ERROR: No homeland maps found!");
		}
	}
}
