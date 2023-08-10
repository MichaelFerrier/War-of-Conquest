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

import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import WOCServer.DataManager;
import WOCServer.Output;
import WOCServer.Constants;
import WOCServer.InputEvents;
import WOCServer.ClientThread;
import WOCServer.EmailThread;
import WOCServer.Semaphore;

public class WOCServer
{
	static class NationRecord
	{
		LinkedHashMap<Integer,ClientThread> users = new LinkedHashMap<Integer,ClientThread>();
	}

	static ServerSocket serverSocket = null;
	static boolean listening = true;
	static InputNode firstInputNode = null;
	static InputNode lastInputNode = null;
	static Semaphore semaphore_InputNodeQueue = new Semaphore();
	static int latest_client_index = 0;
	static boolean quit = false;
	static long sleep_time = 1000000; // High value so it won't think there's high load upon startup.
	static int log_flags = Constants.LOG_LOGIN | Constants.LOG_ENTER | Constants.LOG_CHAT;
	public static int server_start_time = 0;
	static int prev_sleep_time = 0;
	static int num_clients_in_game = 0;

	static HashMap<Integer,ClientThread> client_table = new HashMap<Integer,ClientThread>();
	static HashMap<Integer,NationRecord> nation_table = new HashMap<Integer,NationRecord>();

	static ArrayList<ClientThread> admin_clients = new ArrayList<ClientThread>();
	static ArrayList<ClientThread> clients_to_delete = new ArrayList<ClientThread>();

  static InputThread input_cycle;
	static UpdateThread update_cycle;
	static BackupThread backup_cycle;
	static EmailThread email_cycle;
	static SocketPolicyServer socket_policy_server;

	static boolean update_thread_active = true;
	static boolean backup_thread_active = true;
	static boolean display_thread_active = true;
	static boolean email_thread_active = true;
	static boolean process_events_active = true;
	static boolean garbage_collect_active = true;
	static boolean regular_update_active = true;

	static StringBuffer temp_output_buffer = new StringBuffer(Constants.OUTPUT_BUFFER_LENGTH);
	static Semaphore temp_output_buffer_semaphore = new Semaphore();

	// NEW MESSAGE SENDING SYSTEM
	public static List message_senders = Collections.synchronizedList(new LinkedList<ClientThread>());

	public static void main(String[] args) throws IOException
	{
		CommandReader tempCommand;
		int currentNode;
		InputNode curInputNode;
		int prev_update_time = 0;
		long loop_start_time = 0, new_loop_start_time = 0, pre_db_update_time = 0;

		// NEW MESSAGE SENDING SYSTEM
		ListIterator senders_iter;
		ClientThread cur_sender;

		// Initialize the Constants class
		Constants.Init();

		// Initialize the DataManager class
		DataManager.Init();

		//Output.screenOut.println("home_dir: " + Constants.home_dir);

		Output.PrintToScreen("--------------------------------------------");
		Output.PrintToScreen("| War of Conquest 2 Server v1.0            |");
		Output.PrintToScreen("|  Copyright 2002,2023 - IronZog           |");
		Output.PrintToScreen("|  Type HELP to see list of commands.      |");
		Output.PrintToScreen("--------------------------------------------");

		// Initialize data
		DataManager.InitData();

		// Update the data for latest changes, if necessary.
		Constants.UpdateData();

		// Record time when server started.
		server_start_time = Constants.GetTime();

		// Download the info xml file.
		Document info_xml = URLToXML("https://warofconquest.com/woc2/info.xml");

		boolean load_data_success = true;
		load_data_success = load_data_success && DownloadAndLoadDataFile(FileData.DataFileType.Build, GetDataFileVersionFromInfo(info_xml, "builds"), "builds", "builds.tsv");
		load_data_success = load_data_success && DownloadAndLoadDataFile(FileData.DataFileType.Tech, GetDataFileVersionFromInfo(info_xml, "technologies"), "technologies", "technologies.tsv");
		load_data_success = load_data_success && DownloadAndLoadDataFile(FileData.DataFileType.Object, GetDataFileVersionFromInfo(info_xml, "objects"), "objects", "objects.tsv");
		load_data_success = load_data_success && DownloadAndLoadDataFile(FileData.DataFileType.Quest, GetDataFileVersionFromInfo(info_xml, "quests"), "quests", "quests.tsv");
		load_data_success = load_data_success && DownloadAndLoadDataFile(FileData.DataFileType.League, GetDataFileVersionFromInfo(info_xml, "leagues"), "leagues", "leagues.tsv");

		if (load_data_success == false)
		{
			Output.PrintToScreen("Failed to load data files. Exiting.");
			System.exit(1);
		}

		Output.PrintToScreen("Loading mainland map...");
		if (DataManager.GetLandMap(Constants.MAINLAND_MAP_ID, false) == null) {
			Output.PrintToScreen("Mainland map data does not exist.");
		}

		// Determine payment rates for each orb type.
		Money.DetermineOrbPaymentRates();

		// Determine the boundary of the new player area.
		World.DetermineNewPlayerAreaBoundary();

		// Initialize the list of raid candidate nations.
		Raid.InitRaidCandidates();

		// Initialize the list of homeland maps.
		Homeland.InitMapList();

/*
		// TESTING
		LandMap land_map = DataManager.GetLandMap(Constants.MAINLAND_MAP_ID, false);
		for (int i = 1; i <= 200; i++)
		{
			Output.PrintToScreen("Level " + i + " limit pos: " + land_map.MaxLevelLimitToPosX(i) + ", level limit of pos: " + land_map.PosXToMaxLevelLimit(land_map.MaxLevelLimitToPosX(i)));
		}
		for (int i = 1; i <= 200; i++)
		{
			Output.PrintToScreen("X pos: " + i + ", max level limit: " + land_map.PosXToMaxLevelLimit(i));
		}
*/

    // Update temp techs for server downtime if necessary
		if (Constants.update_for_downtime) {
	    Technology.UpdateTempTechsForDowntime();
		}

		// Start command reader thread
		tempCommand = new CommandReader();
		tempCommand.start();

		// Create and start the UpdateThread
		update_cycle = new UpdateThread();
		Thread update_cycle_thread = new Thread(update_cycle);
		update_cycle_thread.start();

		// Create and start the BackupThread
		backup_cycle = new BackupThread();
		Thread backup_cycle_thread = new Thread(backup_cycle);
		backup_cycle_thread.start();

		// Create and start the EmailThread
		email_cycle = new EmailThread();
		Thread email_cycle_thread = new Thread(email_cycle);
		email_cycle_thread.start();

		// Start socket policy server thread
		socket_policy_server = new SocketPolicyServer();
		socket_policy_server.start();

		// Start input thread
		input_cycle = new InputThread();
		input_cycle.start();

		Output.PrintToScreen("Server started and listening on port " + Constants.port + "\n");
		Output.screenOut.print(">");
		Output.screenOut.flush();

		while (listening)
		{
			// TEMPORARY FIX for dealing with system clock problem!
			UserData userData;
			new_loop_start_time = Constants.GetFreshFineTime();
			if (Constants.GetFreshFineTime() < loop_start_time)
			{
				long time_adjustment = (Constants.GetFreshFineTime() - loop_start_time);

				Output.PrintToScreen("ABOUT TO ADJUST FOR TIME CHANGE OF OVER " + (time_adjustment / 60000) + " MINS");

				// Adjust stored times for each logged in client
				for (Integer key : client_table.keySet())
				{
					ClientThread cThread = (ClientThread)(client_table.get(key));

					cThread.fine_time += time_adjustment;
					cThread.prev_use_fine_time += time_adjustment;

					if (cThread.GetUserID() <= 0) {
						continue;
					}

					userData = (UserData)DataManager.GetData(Constants.DT_USER, cThread.GetUserID(), false);

					if (userData == null) {
						continue;
					}

					userData.prev_chat_fine_time += time_adjustment;
				}

				Output.PrintToScreen("DONE ADJUSTING FOR TIME CHANGE");
			}
			loop_start_time = new_loop_start_time;
			// END TEMPORARY FIX for dealing with system clock problem!

			// Update the time for this loop
			Constants.UpdateTime();

			if ((firstInputNode != null) && process_events_active)
			{
				//Constants.WriteToLog("log_lag.txt", "P1"); // 1 Char to represent common processing of events.

				// Record time for logging of event, if appropriate
				if ((WOCServer.log_flags & Constants.LOG_EVENTS) != 0)
				{
				  Constants.debug_start_time = Constants.GetFineTime();
				}

				semaphore_InputNodeQueue.acquire();

				// Get current input node from queue
				curInputNode = firstInputNode;

				if (firstInputNode.next == null) {
					lastInputNode = null;
				}

				// This is done last so that, should another InputNode have just been queued, a loop won't have formed.
				firstInputNode = firstInputNode.next;

				semaphore_InputNodeQueue.release();

				// Process this input event
				//Constants.WriteToLog("log_lag.txt", "P2(" + curInputNode.hashCode() + ")");
//				Constants.WriteToLog("log_inputnode.txt", Constants.GetHour() + ":" + Constants.GetMinute() + ", P: " + curInputNode.hashCode() + "\n");
				InputEvents.ProcessEvent(curInputNode);
				//Constants.WriteToLog("log_lag.txt", "P3");

				// Record time for logging of event, if appropriate
				if ((WOCServer.log_flags & Constants.LOG_EVENTS) != 0)
				{
					Constants.UpdateTime();
					pre_db_update_time = (Constants.GetFineTime() - Constants.debug_start_time);
				}

				// Update the database
				//DataManager.UpdateDatabase(); // Rather than after every event, database will be updated by Update() call.

				// Log event, if appropriate
				if ((WOCServer.log_flags & Constants.LOG_EVENTS) != 0)
				{
				  Constants.UpdateTime();
				  Constants.debug_stringbuffer.append(Constants.GetHour() + ":" + Constants.GetMinute() + " c#" + curInputNode.clientThread.GetClientIndex() + "(uID#" + curInputNode.clientThread.GetUserID() + "): preDB:" + pre_db_update_time + "ms, postDB:" + (Constants.GetFineTime() - Constants.debug_start_time) + "ms (" + curInputNode.input + ")\n");
					Output.PrintToScreen(Constants.GetHour() + ":" + Constants.GetMinute() + " c#" + curInputNode.clientThread.GetClientIndex() + "(uID#" + curInputNode.clientThread.GetUserID() + "): preDB:" + pre_db_update_time + "ms, postDB:" + (Constants.GetFineTime() - Constants.debug_start_time) + "ms (" + curInputNode.input + ")");
					Constants.WriteToLog("log_lag.txt", Constants.debug_stringbuffer.toString());
					Constants.debug_stringbuffer.setLength(0);
				}

				// Free the processed InputNode
				InputNode.Free(curInputNode);
			}
			else
			{
				if ((firstInputNode == null) && (lastInputNode != null))
				{
					// If (firstInputNode == null) and (lastInputNode != null), this is a bug. Reset the InputNode queue.
					firstInputNode = null;
					lastInputNode = null;

					Output.PrintToScreen("** ERROR: (firstInputNode == null) and (lastInputNode != null), this is a bug! InputNode queue has been reset.");
				}
				else
				{
					// Nothing to do, sleep a while.
					try{
						Thread.sleep(Constants.SERVER_SLEEP_MILLISECONDS);
						prev_sleep_time = Constants.GetTime();
						sleep_time += Constants.SERVER_SLEEP_MILLISECONDS;
					}catch(java.lang.InterruptedException e){Output.PrintTimeToScreen("Insomnia");}
				}
			}

			// NEW MESSAGE SENDING SYSTEM
			// Send queued messages to clients.
			SendQueuedMessages();

			// Update server if necessary
			if (regular_update_active)
			{
				if ((Constants.GetTime() - prev_update_time) > Constants.SERVER_UPDATE_PERIOD)
				{
					try
					{
						//Constants.WriteToLog("log_lag.txt", "U1");
						Update.Update();
						//Constants.WriteToLog("log_lag.txt", "U9");
					}
					catch(Exception e)
					{
						Output.PrintToScreen("Exception during periodic call to Update():");
						Output.PrintException(e);
					}

					prev_update_time = Constants.GetTime();
				}
			}

			// Exit the program loop if told to quit
			if (quit) {
				Output.PrintToScreen("Exiting server...");
				break;
			}

//			Output.PrintToScreen("time: " + Constants.GetTime() + ", DataManager.prev_update_database_time: " + DataManager.prev_update_database_time + ", Constants.UPDATE_DATABASE_BROKEN_DELAY_SECONDS: " + Constants.UPDATE_DATABASE_BROKEN_DELAY_SECONDS);

			if ((Constants.GetTime() - DataManager.prev_update_database_time) > Constants.UPDATE_DATABASE_BROKEN_DELAY_SECONDS)
			{
				Output.PrintToScreen("CRITICAL FAILURE: UpdateDatabase() has not completed in over " + (Constants.UPDATE_DATABASE_BROKEN_DELAY_SECONDS / 60) + " minutes. About to force update and exit server. prev_update_database_time: " + DataManager.prev_update_database_time + ", cur time: " + Constants.GetTime());
				Admin.Emergency("UpdateDatabase() has not completed, exiting.");
				DataManager.UpdateDatabase(true);
				break;
			}

			// TESTING
			//if (Constants.GetFreshFineTime() - loop_start_time > 2000)
			//{
			//	Constants.WriteToLog("log_lag.txt", "\n WOCServer main loop TOOK " + (Constants.GetFreshFineTime() - loop_start_time) + " MS!\n");
			//}
		}

		// Force all clients to exit the game.
		WOCServer.ForceAllExitGame();

    // Update the global data's heartbeat
		GlobalData.instance.heartbeat = Constants.GetTime();
    DataManager.MarkForUpdate(GlobalData.instance);

		// Update the database
		DataManager.UpdateDatabase(false);

		// Send any final messages to the clients
		SendAllQueuedMessages();

		// Pause a moment before exiting, to give messages a chance to be sent.
		try {Thread.sleep(1000);} catch (Exception e) {}

		// Close the server socket
//		serverSocket.close();

		// Exit
		System.exit(0);
	}

	public static void SendAllQueuedMessages()
	{
		long start_fine_time = Constants.GetFreshFineTime();

		// While there are any clients queued to have messages sent to them, send them.
		while (message_senders.size() > 0)
		{
			SendQueuedMessages();

			// Give up after 10 seconds.
			if ((Constants.GetFreshFineTime() - start_fine_time) > 10f) {
				break;
			}
		}
	}

	public static void SendQueuedMessages()
	{
		ListIterator senders_iter;
		ClientThread cur_sender;

		// If there are any clients queued to have messages sent to them...
		if (message_senders.size() > 0)
		{
			// Iterate through the list of clients queued to have messages sent to them...
			senders_iter = message_senders.listIterator();
			while(senders_iter.hasNext())
			{
				// Get the next in the list of clients queued to send.
				cur_sender = (ClientThread)(senders_iter.next());

				// Send to the current client.
				if (cur_sender.SendMessage())
				{
					// There is no more to be sent to this client. Remove it from the message_senders list and record that it is no longer queued to send.
					cur_sender.queued_to_send = false;
					senders_iter.remove();
				}
			}
		}
	}

	static Document URLToXML(String _url)
	{
		Document doc = null;

		try
		{
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			doc = db.parse(new URL(_url).openStream()); // NOTE: If this call hangs, warofconquest.com may be inaccessible.
		}
		catch (Exception e)
		{
			Output.PrintToScreen("Exception while attempting to load XML file at URL " + _url + ": " + e);
			Output.PrintException(e);
		}

		if (doc != null) {
			doc.getDocumentElement().normalize();
		}

		return doc;
	}

	static int GetDataFileVersionFromInfo(Document _info_xml, String _file)
	{
		NodeList nList = _info_xml.getElementsByTagName(_file);

		if (nList.getLength() == 0) {
			return -1;
		}

		Element element = (Element)nList.item(0);

		return Integer.valueOf(element.getAttribute("version"));
	}

	static boolean DownloadAndLoadDataFile(FileData.DataFileType _type, int _required_version, String _version_key, String _data_filename)
	{
		boolean download = false;

		if ((GlobalData.instance.data_file_versions.containsKey(_version_key) == false) || (GlobalData.instance.data_file_versions.get(_version_key) != _required_version)) {
				download = true;
		}

		// Have the appropriate data class load the data file. If it fails, re-download the data file.
		if (download == false) {
				if (FileData.LoadDataFile(_type) == false) {
						download = true;
				}
		}

		if (download)
		{
			Output.PrintToScreen(_version_key + " required version: " + _required_version + " download '" + _data_filename + "': " + download);

			String url = "https://warofconquest.com/woc2/" + _data_filename;

			try
			{
				URL webfile = new URL(url);
				ReadableByteChannel rbc = Channels.newChannel(webfile.openStream());
				FileOutputStream fos = new FileOutputStream(_data_filename);
				fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			}
			catch (Exception e)
			{
				Output.PrintToScreen("Exception while attempting to download file at URL " + url + ": " + e);
				Output.PrintException(e);
				return false;
			}

			// Have the appropriate data class load the downloaded data file. If it fails, try again later.
			if (FileData.LoadDataFile(_type) == false) {
					Output.PrintToScreen("ERROR: could not load file " + _data_filename);
					return false;
			}

			// Record the newly stored version of the data file.
			GlobalData.instance.data_file_versions.put(_version_key, _required_version);
			DataManager.MarkForUpdate(GlobalData.instance);
		}

		return true;
	}

	static void QueueInput(ClientThread _clientThread, int _userID, StringBuffer _input, long _fine_time)
	{
		InputNode new_node = InputNode.Get(_clientThread,	_userID, _input, _fine_time);
//		Constants.WriteToLog("log_inputnode.txt", Constants.GetHour() + ":" + Constants.GetMinute() + ", Q: " + new_node.hashCode() + "\n");
//Output.PrintToScreen("queueing input");

		semaphore_InputNodeQueue.acquire();

		// Add the new input node to the end of the queue.
		if (lastInputNode == null) {
			firstInputNode = new_node;
		}	else {
			lastInputNode.next = new_node;
		}
		lastInputNode = new_node;

		semaphore_InputNodeQueue.release();
	}

	static void listClients()
	{
		ClientThread cThread;
		String output_string;
		Output.PrintToScreen(" Client count: " + client_table.size());
		for (Integer key : client_table.keySet())
		{
			cThread = (ClientThread)(client_table.get(key));
			output_string = " Client " + cThread.GetClientIndex() + ": " + cThread.clientIP + ", " + cThread.hashCode() + ", " + cThread.clientID + ((cThread.GetClientActivationCode().equals("")) ? "" : (", " + cThread.GetClientActivationCode())) + "\n    Dvc " + cThread.GetDeviceData().ID + ": " + cThread.GetDeviceData().GetDeviceType();

			if (cThread.GetUserID() != -1)
			{
				UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, cThread.GetUserID(), false);
				if (userData != null)
				{
					output_string += ("\n    " + userData.name);
					NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, userData.nationID, false);
					if (nationData != null) {
						output_string += (" of " + nationData.name);
					}
				}
			}

			Output.PrintToScreen(output_string);
		}
	}

	static void listNations()
	{
		ClientThread cThread;
		NationData nationData;
		UserData userData;
		int num_nations = 0, num_players = 0;

		Output.PrintToScreen(" Client count: " + client_table.size());
		for (Map.Entry<Integer,NationRecord> nation_entry : nation_table.entrySet())
		{
			num_nations++;
			nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nation_entry.getKey(), false);

			Output.PrintToScreen(" Nation " + nationData.name + " (" + nation_entry.getKey() + ") Lvl " + nationData.level +  (nationData.veteran ? " vet" : " new") + (nationData.GetFlag(Constants.NF_INCOGNITO) ? " incognito" : ""));

			for (Map.Entry<Integer,ClientThread> user_entry : nation_entry.getValue().users.entrySet())
			{
				num_players++;
				cThread = (ClientThread)(user_entry.getValue());
				userData = (UserData)DataManager.GetData(Constants.DT_USER, user_entry.getKey(), false);
				Output.PrintToScreen("   User " + userData.name + " (" + user_entry.getKey() + ")" +  (userData.veteran ? " vet" : " new") + " IP: " + cThread.clientIP + ", index: " + cThread.GetClientIndex() + " hash: " + cThread.hashCode() + " Dvc " + cThread.GetDeviceData().ID);
			}
		}

		Output.PrintToScreen(" Logged in: " + num_players + " players (" + num_nations + " nations)");
	}

	static int GetNationNumUsersOnline(int _nationID)
	{
		NationRecord nation_record = nation_table.get(_nationID);
		return (nation_record == null) ? 0 : nation_record.users.size();
	}

	static void ForceAllExitGame()
	{
		ClientThread cThread;

		// Preload keys into array rather than iterating map, because map will be modified as clients are made to exit the game.
		Set<Integer> client_keys = client_table.keySet();
		Integer[] client_keys_array = client_keys.toArray(new Integer[client_keys.size()]);

		for (int i = 0; i < client_keys_array.length; i++)
		{
			// Force the current client to log out
			Login.ForceExitGame(client_table.get(client_keys_array[i]), ClientString.Get("svr_go_offline")); // "The War of Conquest game server is temporarily offline.\nPlease try again in a few minutes."
		}

/*
		NationRecord cur_nation_record;
		ClientThread cur_client_thread;

		// Preload keys into array rather than iterating map, because map will be modified as clients are made to exit the game.
		Set<Integer> nation_keys = nation_table.keySet();
		Integer[] nation_keys_array = nation_keys.toArray(new Integer[nation_keys.size()]);

		for (int i = 0; i < nation_keys_array.length; i++)
		{
			cur_nation_record = nation_table.get(nation_keys_array[i]);

			// Preload keys into array rather than iterating map, because map will be modified as clients are made to exit the game.
			Set<Integer> client_keys = cur_nation_record.users.keySet();
			Integer[] client_keys_array = client_keys.toArray(new Integer[client_keys.size()]);

			for (int j = 0; j < client_keys_array.length; j++)
			{
				// Force the current client to log out
				Login.ForceExitGame(cur_nation_record.users.get(client_keys_array[j]), ClientString.Get("svr_go_offline")); // "The War of Conquest game server is temporarily offline.\nPlease try again in a few minutes."
			}

			// Remove any remaining client records the nation record's users table.
			cur_nation_record.users.clear();
		}

		// Remove any remaining nation records from the nation_table.
		nation_table.clear();
		*/
	}

	// Queue a client to be deleted.
	static void QueueClientToDelete(ClientThread _clientThread)
	{
		if (_clientThread.IsFree())
		{
			Output.PrintToScreen("ERROR: QueueClientToDelete() called for client " + _clientThread + " that is already marked as being free!");
			Output.PrintStackTrace();
			return;
		}

		if (clients_to_delete.contains(_clientThread) == false) {
			clients_to_delete.add(_clientThread);
		}
	}

	// Delete all clients queued to be deleted.
	static void DeleteQueuedClients()
	{
		ClientThread cur_client;

		for (int i = 0; i < clients_to_delete.size();)
		{
			cur_client = clients_to_delete.get(i);

			if (cur_client.IsQueuedToSend() == false) {
				DeleteClient(clients_to_delete.remove(i)); // This client is not queued to send messages, so delete it.
			} else {
				i++; // This client is still queued to send messages. Skip it.
			}
		}

		// TESTING
		if (clients_to_delete.size() > 10) Output.PrintToScreen("DeleteQueuedClients() finished with " + clients_to_delete.size() + " clients still queued. Has of first: " + ((ClientThread)(clients_to_delete.get(0))).hashCode());
	}

	// This method should never be called directly, instead QueueClientToDelete() should be used. This way final messages can be sent to the client before it is deleted, and it avoid deleting the same client more than once.
	static void DeleteClient(ClientThread _clientThread)
	{
		//Output.PrintToScreen("DeleteClient() Removing and freeing client thread index " + _clientThread.GetClientIndex() + " ID: " + _clientThread.GetClientID() + " hash: " + _clientThread.hashCode());

		// Remove the ClientThread from the client_table
		int index = _clientThread.GetClientIndex();
		client_table.remove(index);

		// Reset the ClientThread
		_clientThread.Reset();

		// Free the ClientThread
		ClientThread.Free(_clientThread);
	}

	public static void AddUser(int _userID, int _nationID, ClientThread _clientThread)
	{
		// Add this user and nation to the nation_table
		//Output.PrintToScreen("AddUser(_userID: " + _userID + ", _nationID: " + _nationID + ", client index: " + _clientThread.GetClientIndex() + ", ID: " + _clientThread.clientID + ")"); // DEBUG ONLY

		NationRecord nation_record = nation_table.get(_nationID);

		if (nation_record == null)
		{
			nation_record = new NationRecord();
			nation_table.put(_nationID, nation_record);
		}
		else
		{
			if (nation_record.users.get(_userID) != null) {
				Output.PrintToScreen("ERROR: AddUser(): _userID " + _userID + " is already logged in to nation with _nationID: " + _nationID);
			}
		}

		nation_record.users.put(_userID, _clientThread);

		// If the newly added client is an admin user, record it in the list of admin users.
		if (_clientThread.UserIsAdmin()) {
				admin_clients.add(_clientThread);
		}

		// Increment the count of the number of clients in game.
		num_clients_in_game++;

		// Record that the client is in the game.
		_clientThread.in_game = true;
	}

	public static void RemoveUser(int _userID, int _nationID, ClientThread _clientThread)
	{
		//Output.PrintToScreen("RemoveUser(_userID: " + _userID + ", _nationID: " + _nationID + ", client index: " + _clientThread.GetClientIndex() + ", ID: " + _clientThread.clientID + ")"); // DEBUG ONLY

		// Remove this user (and nation, if user is last) from the nation_table
		NationRecord nation_record = nation_table.get(_nationID);

		if (nation_record == null) {
			Output.PrintToScreen("ERROR: RemoveUser(): nation_record not found! (_nationID: " + _nationID + ", _userID: " + _userID + ")");
			return;
		}

		ClientThread client_thread = nation_record.users.get(_userID);

		if (client_thread == null) {
			Output.PrintToScreen("ERROR: RemoveUser(): user not found! (_nationID: " + _nationID + ", _userID: " + _userID + ")");
			return;
		}

		// Remove this user from the nation's record
		nation_record.users.remove(Integer.valueOf(_userID));

		// If the nation's user list is empty, remove the nation's record.
		if (nation_record.users.size() == 0) {
			nation_table.remove(_nationID);
		}

		// If the newly removed client is an admin user, remove it from the list of admin users.
		if (_clientThread.UserIsAdmin()) {
				admin_clients.remove(_clientThread);
		}

		// Decrement the count of the number of clients in game.
		num_clients_in_game--;

		// Record that the client is not in the game.
		_clientThread.in_game = false;
	}

	public static boolean IsUserLoggedIn(int _userID)
	{
		// Get the user's data.
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		if (userData == null) {
			return false;
		}

		return (GetClientThread(_userID, userData.nationID) != null);
	}

	public static boolean IsUserLoggedIn(int _userID, int _nationID)
	{
		return (GetClientThread(_userID, _nationID) != null);
	}

	public static ClientThread GetClientThread(int _userID, int _nationID)
	{
		NationRecord nation_record = nation_table.get(_nationID);

		if (nation_record == null) {
			return null;
		}

		ClientThread client_thread = nation_record.users.get(_userID);

		return client_thread;
	}

  public static ClientThread GetClientThread(int _userID)
	{
		ClientThread client_thread;

		for (Map.Entry<Integer,NationRecord> entry : nation_table.entrySet())
		{
      client_thread = (ClientThread)(entry.getValue().users.get(_userID));

			if (client_thread != null) {
				return client_thread;
			}
		}

    return null;
	}

	public static boolean ToggleLogFlag(int _log_flag)
	{
		if ((log_flags & _log_flag) != 0) {
			log_flags = log_flags & ~_log_flag;
		} else {
			log_flags |= _log_flag;
		}

		return ((log_flags & _log_flag) != 0);
	}

	public static boolean GetLogFlag(int _log_flag)
	{
		return ((log_flags & _log_flag) != 0);
	}

	public static void OutputMemoryStats()
	{
		Output.PrintToScreen("Total Java VM memory: " + Runtime.getRuntime().totalMemory());
		Output.PrintToScreen("Free Java VM memory: " + Runtime.getRuntime().freeMemory());
		Output.PrintToScreen("Used Java VM memory: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
	}

	public static void Quit()
	{
		// Mark the global data and ranks data to be updated.
		DataManager.MarkForUpdate(GlobalData.instance);
		DataManager.MarkForUpdate(RanksData.instance);

		quit = true;
	}
}

class InputNode
{
	static final int FREE_STACK_LENGTH = 5000;
	static InputNode free_stack[] = new InputNode[FREE_STACK_LENGTH];
	static int free_stack_top = 0;
	static Semaphore semaphore_InputNode = new Semaphore();
	private boolean free = true;

	private int MAX_MESSAGE_LENGTH = 1024;
	public StringBuffer input = new StringBuffer(MAX_MESSAGE_LENGTH);

	ClientThread clientThread;
	int userID;
	long fine_time;
	InputNode next = null;

	public static InputNode Get(ClientThread _clientThread,	int _userID, StringBuffer _input, long _fine_time)
	{
		InputNode object;

		semaphore_InputNode.acquire();

		if (free_stack_top > 0)
		{
			free_stack_top--;
			free_stack[free_stack_top].Initialize(_clientThread, _userID, _input, _fine_time);
			object = free_stack[free_stack_top];
		}
		else
		{
			object = new InputNode(_clientThread, _userID,	_input, _fine_time);
		}

		// Record that this object is not free.
		object.free = false;

		// No longer busy
		semaphore_InputNode.release();

		return object;
	}

	public static void Free(InputNode _object)
	{
		if (_object.free)
		{
			Output.PrintToScreen("ERROR: InputNode.Free() called for InputNode " + _object.hashCode() + " that is already marked as being free!");
			Output.PrintStackTrace();
			return;
		}

		// Make sure the object being freed is non-null.
		if (_object == null) {
			Output.PrintToScreen("Attempt to free null InputNode!");
			throw new RuntimeException("Attempt to free null InputNode!");
		}

		// Initialize InputNode's members upon freeing it so that it can't be re-processed, due to an occasional bug.
		_object.clientThread = null;
		_object.userID = 0;
		_object.fine_time = 0;
		_object.next = null;

		semaphore_InputNode.acquire();

		// Add the object to the free stack if there's room
		if (free_stack_top < FREE_STACK_LENGTH)
		{
			free_stack[free_stack_top] = _object;
			free_stack_top++;
		}

		// Record that this object is free.
		_object.free = true;

		// No longer busy
		semaphore_InputNode.release();
	}

	public InputNode(ClientThread _clientThread,	int _userID,	StringBuffer _input, long _fine_time)
	{
		Initialize(_clientThread,	_userID, _input, _fine_time);
	}

	public void Initialize(ClientThread _clientThread,	int _userID,	StringBuffer _input, long _fine_time)
	{
		clientThread = _clientThread;
		userID = _userID;
		fine_time = _fine_time;
		next = null;

		// Copy the _input message into the input StringBuffer
		input.delete(0, MAX_MESSAGE_LENGTH);
		input.append(_input);
	}
};

class CommandReader extends Thread
{
	BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
	String command;

	public CommandReader()
	{
		super("CommandReader");
	}

	public void run()
	{
		while (true)
		{
			try
			{
				while ((command = in.readLine()) != null)
				{
					// Process this admin command submitted through the console.
					Admin.ProcessAdminCommand(command, null);
				}
			}
			catch (IOException e)
			{
				Output.PrintException(e);
			}
		}
	}
};

class InputThread extends Thread
{
	static StringBuffer output_buffer = new StringBuffer(Constants.OUTPUT_BUFFER_LENGTH);

	CharsetDecoder decoder = Constants.charset.newDecoder();

	int READ_BUFFER_SIZE = 2048;
	ByteBuffer read_buffer = ByteBuffer.allocate(READ_BUFFER_SIZE);
	CharBuffer char_buffer = CharBuffer.allocate(READ_BUFFER_SIZE);

	static int prev_sleep_time = -1;

	public void run()
	{
		ServerSocketChannel server;
		Selector selector;
		boolean client_disconnected;
		int bytes_read;

		try
		{
			// Create the server socket channel
			server = ServerSocketChannel.open();

			// Set to use nonblocking I/O
			server.configureBlocking(false);

			// Set IP address and port
			server.socket().bind(new java.net.InetSocketAddress(Constants.port));

			// Create the selector
			selector = Selector.open();

			// Register selector with the ServerSocketChannel (type OP_ACCEPT)
			server.register(selector,SelectionKey.OP_ACCEPT);
		}
		catch (Exception e)
		{
			Output.PrintToScreen("Failed to create ServerSocketChannel or Selector");
			Output.PrintException(e);
			return;
		}

		// Infinite server loop
		for(;;)
		{
			// Sleep for a while, until the next check.
			try{
			  Thread.sleep(Constants.CLIENT_THREAD_SLEEP_MILLISECONDS);
				//prev_sleep_time = Constants.GetTime();
			}catch(java.lang.InterruptedException e){Output.PrintTimeToScreen("ClientThread Insomnia");}

			try
			{
				// Waiting for events
				selector.select();
			}
			catch(Exception e)
			{
				Output.PrintToScreen("Exception during call to selector.select()");
				Output.PrintException(e);
				return;
			}

  		// Set prev_sleep_time here, since selector.select() is blocking, and so would count as as sometimes a very long lag.
		  prev_sleep_time = Constants.GetTime();

			// Get keys
			Set keys = selector.selectedKeys();
			Iterator i = keys.iterator();

			// For each key...
			while(i.hasNext())
			{
				SelectionKey key = (SelectionKey) i.next();

  			// Remove the current key
				i.remove();

				try
				{
					// If isAcceptable = true
					// then a client is requesting a connection
					if (key.isAcceptable())
					{
						// Get client socket channel
						SocketChannel client_channel = server.accept();

						// Non Blocking I/O
						client_channel.configureBlocking(false);

						// Register with the selector (for reading)
						SelectionKey readKey = client_channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

						// Create a new ClientThread for this client
						ClientThread clientThread = ClientThread.Get();
						clientThread.Init(client_channel, ++(WOCServer.latest_client_index));

						// Attach the new clientThread object to this key
						readKey.attach(clientThread);

						// Add the new ClientThread to the WOCServer's table of clients
						WOCServer.client_table.put(WOCServer.latest_client_index, clientThread);

						//Output.PrintToScreen("Client thread " + clientThread.GetClientIndex() + " ID " + clientThread.GetClientID() + " hash " + clientThread.hashCode() + " ACCEPTED.");

						continue;
					}

					// If isReadable = true
					// then the server is ready to read
					if (key.isReadable())
					{
						client_disconnected = false;
						SocketChannel socket_channel = (SocketChannel) key.channel();

						try
						{
							// Read bytes coming from the client
							read_buffer.clear();
							bytes_read = socket_channel.read(read_buffer);

							if (bytes_read == -1)
							{
								// This should mean that the client cleanly closed th socket connection.
								Output.PrintToScreen("read() on readable channel returned -1; client disconnected. uid: " + ((ClientThread)(key.attachment())).GetDeviceData().uid);
								client_disconnected = true;
							}
						}
						catch (Exception e)
						{
							// This can mean that the socket connection was interrupted.
							Output.PrintToScreen("read() on readable channel produced exception (" + e.getMessage() + "); client disconnected.");
							client_disconnected = true;
						}

						if (client_disconnected)
						{
							// Get the clientThread object
							ClientThread clientThread = (ClientThread)(key.attachment());

							//// Log this logoff
							//if (clientThread.UserIsLoggedIn()) {
							//  Output.PrintToScreen("clientThread index " + clientThread.GetClientIndex() + ", client disconnected; queueing exit event to log off.");
							//}

							// Have the clientThread queue a quit client event for processing, to logout this client
							// and remove it from client list. This is done via an event rather than here,
							// to avoid thread concurrency problems.
							clientThread.QueueQuitClientEvent();

							// Have this client's SelectionKey remove itself from the Selector
							key.cancel();

							continue;
						}

						// Convert bytes into characters
						read_buffer.flip();
						CharBuffer char_buffer = decoder.decode(read_buffer);  // Would like to use the version of decode() that puts it into an existing CharBuffer instead to minimize allocations, but that version doesn't set the 'limit' (length) of the CharBuffer, resulting in strings that are too long.

						try
						{
						  // Give the received characters to the ClientThread
						  ((ClientThread)key.attachment()).InputReceived(char_buffer);
            }
            catch(Exception e)
            {
              Output.PrintToScreen("Exception when calling InputReceived() for message: \"" + char_buffer.toString() + "\"");
              Output.PrintException(e);
            }

						continue;
					}
				}
				catch(Exception e)
				{
					Output.PrintToScreen("Exception when processing key");
					Output.PrintException(e);
					//return;
				}
			}

			if ((Constants.GetTime() - prev_sleep_time) > 3)
			{
				Output.PrintToScreen(">> InputThread.run() took " + (Constants.GetTime() - prev_sleep_time) + "s between sleeps");
				Constants.WriteToLog("log_lag.txt", Constants.GetTimestampString() + " InputThread.run() took " + (Constants.GetTime() - prev_sleep_time) + "s between sleeps\n");
			}

			// Reset prev_sleep_time to -1 so that time spent in selector.select() will not be counted as lag.
			prev_sleep_time = -1;

			// If the server is exiting, return.
			if (WOCServer.quit == true) {
				return;
			}
		}
	}
};
