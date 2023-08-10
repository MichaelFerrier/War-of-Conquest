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

public class Update
{
	static final int UPDATE_BUFFER_LENGTH = 8192;
	static StringBuffer update_buffer = new StringBuffer(UPDATE_BUFFER_LENGTH);
	static int update_count = 0;

	public static void Update()
	{
		NationData nationData;
		UserData userData;
		ClientThread cur_client;
		long cur_fine_time, fine_time_since_last_use;
		int update_interval, cur_time, userID, nationID;

		// Get the current time and fine time
		cur_time = Constants.GetTime();
		cur_fine_time = Constants.GetFineTime();

		// If this is a complete update...
		if (update_count == 0)
		{
			//Constants.WriteToLog("log_lag.txt", "\n" + Constants.GetShortTimeString() + ",uF");

			// Update stats and remove obsolete technologies of all online nations, and broadcast regular update events to them.
			for (Integer nationID_Integer :  WOCServer.nation_table.keySet())
			{
				// Get the nationData
				nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID_Integer, false);

				// If nation's data not found, continue.
				if (nationData == null) {
					Output.PrintToScreen("ERROR: Update(): Nation " + nationID_Integer + ", data is null!");
					continue;
				}

				// TESTING
				if (nationID_Integer != nationData.ID) {
					Output.PrintToScreen("ERROR: Update() for nation ID " + nationID_Integer + ", nationData's ID is different: " + nationData.ID);
				}

				// Update stats and remove any obsolete technologies from the nation.
				Technology.UpdateStats(nationID_Integer, nationData);

				// Broadcast update event to all of this nation's logged in players.
				OutputEvents.BroadcastUpdateEvent(nationID_Integer);
			}
		}

		// Get the client table
		HashMap<Integer,ClientThread> client_table = WOCServer.client_table;

		// Iterate through all connected clients

		// Preload keys into array rather than iterating map, because map will be modified if clients are made to exit the game.
		Set<Integer> client_keys = client_table.keySet();
		Integer[] client_keys_array = client_keys.toArray(new Integer[client_keys.size()]);

		for (int i = 0; i < client_keys_array.length; i++)
		{
			// Get the current ClientThread
			cur_client = client_table.get(client_keys_array[i]);

			if (cur_client == null)
			{
				Output.PrintToScreen("ERROR: null ClientThread pointer in list, in Update(). Key: " + client_keys_array[i]);
				continue;
			}

			// Get the client's userID
			userID = cur_client.GetUserID();

			// Determine fine time since this client was last used
			fine_time_since_last_use = cur_fine_time - cur_client.prev_use_fine_time;

			// Suspend a player who is in the game, if their client has been inactive too long.
			if ((fine_time_since_last_use >= Constants.SUSPEND_INACTIVE_PLAYER_FINE_DELAY) && (userID != -1) && (cur_client.UserIsInGame()))
			{
				// Force the client to exit the game.
				Login.ExitGame(cur_client, false);

				// Send suspend event
				update_buffer.setLength(0);
				OutputEvents.GetSuspendEvent(update_buffer, ClientString.Get("svr_client_paused"), true); // "Game Paused"
				cur_client.TerminateAndSendNow(update_buffer);

				// Remove this client thread from the server. (Must be done queued, rather than directly, so messages can be sent to it first.)
				WOCServer.QueueClientToDelete(cur_client);

				// Continue on to next client.
				continue;
			}

			// Remove a client that is not in the game, if it has been inactive too long.
			if ((fine_time_since_last_use >= Constants.REMOVE_INACTIVE_CLIENT_FINE_DELAY) && (!cur_client.UserIsInGame()))
			{
				// Remove this client thread from the server. (Must be done queued, rather than directly, so messages can be sent to it first.)
				WOCServer.QueueClientToDelete(cur_client);

				// Continue on to next client.
				continue;
			}
		}

		//Constants.WriteToLog("log_lag.txt", "u2");

		// Update all of the goals if a new period has begun.
		int new_goal_update_period = (int)(cur_time / Constants.GOAL_UPDATE_PERIOD);
		if (new_goal_update_period > GlobalData.instance.cur_goal_update_period)
		{
			// Update all of the goals
			Objects.UpdateAllGoals();

			// Store the number of the current cur_goal_update_period
			GlobalData.instance.cur_goal_update_period = ((int)(cur_time / Constants.GOAL_UPDATE_PERIOD));
		}
		else if (new_goal_update_period < GlobalData.instance.cur_goal_update_period)
		{
			// This means that the GOAL_UPDATE_PERIOD has increased, so reset the cur_goal_update_period.
			GlobalData.instance.cur_goal_update_period = 0;
			DataManager.MarkForUpdate(GlobalData.instance);
		}

		//Constants.WriteToLog("log_lag.txt", "u3");

		// Cycle update_count.
		update_count++;
		if (update_count == Constants.COMPLETE_UPDATE_FREQUENCY) {
			update_count = 0;
		}
	}
}
