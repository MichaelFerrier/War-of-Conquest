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
import WOCServer.*;

public class InputEvents
{
	private static StringBuffer output_buffer = new StringBuffer(Constants.OUTPUT_BUFFER_LENGTH);

	public static void ProcessEvent(InputNode _inputNode)
	{
		// Get info from given InputNode
		ClientThread clientThread = _inputNode.clientThread;
		int userID = _inputNode.userID;
		StringBuffer input = _inputNode.input;

		// If clientThread == NULL, this InputNode shouldn't be processed and is here due to a bug.
		if (clientThread == null) {
			Output.PrintToScreen("ERROR: ProcessEvent() given InputNode with (clientThread == null)!");
			return;
		}

		// Fetch action value
		String action = Constants.FetchParameterFromBuffer(input, "action", false);
//Constants.WriteToLog("log_lag.txt", "[" + action + "]");

		// Do nothing if action isn't specified
		if (action.equals("")) {
			return;
		}

		// Clear the output StringBuffer
		output_buffer.setLength(0);

		// Log input event if appropriate
		if ((WOCServer.log_flags & Constants.LOG_INPUT) != 0) {
			Output.PrintToScreen("Input: '" + input + "'");
		}

		// Log input event from client we are tracking, if appropriate.
		if ((Admin.track_clientID.length() > 0) && (clientThread.clientID.indexOf(Admin.track_clientID) != -1)) {
			Output.PrintToScreen("TRACKING: " + Constants.GetShortTimeString() + ": ProcessEvent() for client ID " + clientThread.clientID + ": " + input);
		}

		// FOR TESTING ONLY:
		long event_start_time = Constants.GetFreshFineTime();
		long event_start_free_mem = Runtime.getRuntime().freeMemory();

		try {
			// Respond according to content of given input message
			switch (action.charAt(0))
			{
				case 'a':
					if (action.equals("ad_watched"))
					{
						// Reward the user for watching the ad.
						Gameplay.AdWatched(output_buffer, clientThread.GetUserID());
					}
					else if (action.equals("admin_command"))
					{
						String command = Constants.FetchParameterFromBuffer(input, "command", true);

						if (clientThread.userIsAdmin) {
							Admin.ProcessAdminCommand(command, clientThread);
						} else {
							Output.PrintToScreen("WARNING: Attempt to submit admin command from non-admin account " + clientThread.player_account.username + "(" + clientThread.GetUserID() + ", IP: " + clientThread.clientIP + ")");
						}
					}
					else if (action.equals("admin_mute_user"))
					{
						String muted_user = Constants.FetchParameterFromBuffer(input, "username", false);
						int num_hours = Constants.FetchParameterIntFromBuffer(input, "mute_hours");
						Admin.ChatBanUser(output_buffer, muted_user, num_hours, false, false);

						// Get the admin's user data
						UserData adminData = (UserData)DataManager.GetData(Constants.DT_USER, clientThread.GetUserID(), false);
						Constants.WriteToLog("log_ban.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + adminData.name + " muted " + muted_user + " for " + num_hours + " hours.\n");
					}
					else if (action.equals("admin_ban_user"))
					{
						String banned_user = Constants.FetchParameterFromBuffer(input, "username", false);
						int num_hours = Constants.FetchParameterIntFromBuffer(input, "ban_hours");
						Admin.GameBanUser(output_buffer, banned_user, num_hours, false, false);

						// Get the admin's user data
						UserData adminData = (UserData)DataManager.GetData(Constants.DT_USER, clientThread.GetUserID(), false);
						Constants.WriteToLog("log_ban.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + adminData.name + " banned " + banned_user + " for " + num_hours + " hours.\n");
					}
					else if (action.equals("admin_nation_info"))
					{
						Admin.NationInfo(output_buffer, Constants.FetchParameterFromBuffer(input, "nation", false),false);
					}
					else if (action.equals("admin_add_xp"))
					{
						Admin.AddNationXP(output_buffer,Constants.FetchParameterFromBuffer(input, "nation", false),Constants.FetchParameterIntFromBuffer(input, "amount"),false);
					}
					else if (action.equals("admin_add_credits"))
					{
						Admin.AddCredits(output_buffer,Constants.FetchParameterFromBuffer(input, "nation", false),Constants.FetchParameterIntFromBuffer(input, "amount"),(Constants.FetchParameterIntFromBuffer(input, "purchased")!=0),false);
					}
					else if (action.equals("admin_reload_nation"))
					{
						Admin.ReloadNation(output_buffer,Constants.FetchParameterFromBuffer(input, "nation", false),false);
					}
					else if (action.equals("admin_reload_nation_techs"))
					{
						Admin.ReloadNationTechs(output_buffer,Constants.FetchParameterFromBuffer(input, "nation", false),false);
					}
					else if (action.equals("auto_enter_game"))
					{
						// Attempt to have this client enter the game.
						Login.AttemptEnterGame(output_buffer, clientThread);
					}
					break;

				case 'b':
					if (action.equals("build"))
					{
						int buildID = Constants.FetchParameterIntFromBuffer(input, "buildID");
						int x = Constants.FetchParameterIntFromBuffer(input, "x");
						int y = Constants.FetchParameterIntFromBuffer(input, "y");

						Gameplay.Build(output_buffer, clientThread.GetUserID(), buildID, x, y);

						clientThread.GetDeviceData().UpdateCorrelationsForActivity();
					}
					else if (action.equals("buy_energy"))
					{
						int pkg = Constants.FetchParameterIntFromBuffer(input, "package");
						Gameplay.BuyEnergy(output_buffer, clientThread.GetUserID(), pkg);
					}
					else if (action.equals("buy_manpower"))
					{
						int pkg = Constants.FetchParameterIntFromBuffer(input, "package");
						Gameplay.BuyManpower(output_buffer, clientThread.GetUserID(), pkg);
					}
				case 'c':
					if (action.equals("captcha"))
					{
						clientThread.CaptchaFailed(Constants.FetchParameterFromBuffer(input, "event", false));
					}
					else if (action.equals("cash_out"))
					{
						int targetUserID = Constants.FetchParameterIntFromBuffer(input, "target_user_id");
						int amount = Constants.FetchParameterIntFromBuffer(input, "amount");
						Money.CashOut(output_buffer, clientThread.GetPlayerID(), clientThread.GetUserID(), targetUserID, amount);
					}
					else if (action.equals("change_rank"))
					{
						int memberID = Constants.FetchParameterIntFromBuffer(input, "memberID");
						int new_rank = Constants.FetchParameterIntFromBuffer(input, "rank");

						// Change the nation's password
						Application.ChangeRank(output_buffer, clientThread.GetUserID(), memberID, new_rank);
					}
					else if (action.equals("chat_input"))
					{
						int channel = Constants.FetchParameterIntFromBuffer(input, "channel");
						String text = Constants.FetchParameterFromBuffer(input, "text", true); // Take the entire remainder of the param string

						// Process chat input event.
						Comm.ChatInput(output_buffer, clientThread.GetUserID(), clientThread.GetDeviceData().ID, channel, text);

						clientThread.GetDeviceData().UpdateCorrelationsForActivity();
					}
					else if (action.equals("chat_list_add"))
					{
						// The nation's ID and added nation's ID
						int nationID = Constants.FetchParameterIntFromBuffer(input, "nationID");
						int addedNationID = Constants.FetchParameterIntFromBuffer(input, "addedNationID");

						// Get the WOCServer's nation table's record for the given nation
						WOCServer.NationRecord chat_nation_record = WOCServer.nation_table.get(addedNationID);

						// If no member of the nation is online, do nothing.
						if (chat_nation_record == null)
						{
							OutputEvents.GetMessageEvent(output_buffer, ClientString.Get("svr_chat_list_add_nation_offline")); // "You cannot add an offline nation to your chat list."
							break;
						}

						// Get the user data
						UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, clientThread.GetUserID(), false);

						// Do not allow addition to the chat list of a nation that the user doesn't belong to.
						if (userData.nationID != nationID) {
							break;
						}

						// Require rank of commander or higher in order to change chat list.
						if (userData.rank > Constants.RANK_COMMANDER)
						{
							OutputEvents.GetMessageEvent(output_buffer, ClientString.Get("svr_chat_list_add_rank_too_low")); // "You must rank as Commander or higher to add to the chat list."
							break;
						}

						// Get the nation data
						NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, nationID, false);
						NationData addedNationData = (NationData)DataManager.GetData(Constants.DT_NATION, addedNationID, false);

						if (nationData.chat_list.contains(addedNationID))
						{
							OutputEvents.GetMessageEvent(output_buffer, ClientString.Get("svr_chat_list_add_already_in_list")); // "This nation is already in your chat list."
							break;
						}

						if (nationData.chat_list.size() >= Constants.MAX_NUM_CHAT_LIST)
						{
							OutputEvents.GetMessageEvent(output_buffer, ClientString.Get("svr_chat_list_add_full")); // "You can't add any more nations to your chat list."
							break;
						}

						if ((addedNationData.flags & Constants.NF_BLOCK_NATION_CHAT_INVITATIONS) != 0)
						{
							OutputEvents.GetMessageEvent(output_buffer, ClientString.Get("svr_chat_list_add_not_accepted")); // "This nation is not accepting chat invitations."
							break;
						}

						// Add the nation to the chat list.
						Comm.ChatListAdd(nationID, addedNationID);
					}
					else if (action.equals("chat_list_remove"))
					{
						// The nation's ID and removed nation's ID
						int nationID = Constants.FetchParameterIntFromBuffer(input, "nationID");
						int removedNationID = Constants.FetchParameterIntFromBuffer(input, "removedNationID");

						// Get the user data
						UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, clientThread.GetUserID(), false);

						// Do not allow removal from the chat list by a third party nation.
						if ((userData.nationID != nationID) && (userData.nationID != removedNationID)) {
							return;
						}

						// Require rank of commander or higher in order to change chat list.
						if (userData.rank > Constants.RANK_COMMANDER)
						{
							OutputEvents.GetMessageEvent(output_buffer, ClientString.Get("svr_chat_list_remove_rank_too_low")); // "You must rank as Commander or higher to remove a nation from this list."
							return;
						}

						// Remove the nation from the chat list.
						Comm.ChatListRemove(nationID, removedNationID);
					}
					else if (action.equals("complete"))
					{
						int x = Constants.FetchParameterIntFromBuffer(input, "x");
						int y = Constants.FetchParameterIntFromBuffer(input, "y");

						Gameplay.Complete(output_buffer, clientThread.GetUserID(), x, y);

						clientThread.GetDeviceData().UpdateCorrelationsForActivity();
					}
					else if (action.equals("create_password"))
					{
						String email = Constants.FetchParameterFromBuffer(input, "email", false);
						String question = Constants.FetchParameterFromBuffer(input, "question", false);
						String answer = Constants.FetchParameterFromBuffer(input, "answer", false);

						// Call password creation method
						Application.CreatePassword(output_buffer, clientThread, email, question, answer);
					}
					else if (action.equals("customize_appearance"))
					{
						int color_r = Constants.FetchParameterIntFromBuffer(input, "color_r");
						int color_g = Constants.FetchParameterIntFromBuffer(input, "color_g");
						int color_b = Constants.FetchParameterIntFromBuffer(input, "color_b");
						int emblem_index = Constants.FetchParameterIntFromBuffer(input, "emblem_index");
						int emblem_color = Constants.FetchParameterIntFromBuffer(input, "emblem_color");

						// Call customize nation method
						Application.CustomizeAppearance(output_buffer, clientThread, color_r, color_g, color_b, emblem_index, emblem_color);
					}
					else if (action.equals("customize_nation"))
					{
						String nation_name = Constants.FetchParameterFromBuffer(input, "nation_name", false);
						int color_r = Constants.FetchParameterIntFromBuffer(input, "color_r");
						int color_g = Constants.FetchParameterIntFromBuffer(input, "color_g");
						int color_b = Constants.FetchParameterIntFromBuffer(input, "color_b");

						// Call customize nation method
						Application.CustomizeNation(output_buffer, clientThread, nation_name, color_r, color_g, color_b);
					}
					break;

				case 'd':
					if (action.equals("deposit_money"))
					{
						float deposit_amount = Constants.FetchParameterFloatFromBuffer(input, "deposit_amount");

						// Deposit money into game
						Money.AttemptDeposit(output_buffer, clientThread.GetUserID(), deposit_amount);
					}
					else if (action.equals("delete_map_flag"))
					{
						int x = Constants.FetchParameterIntFromBuffer(input, "x");
						int y = Constants.FetchParameterIntFromBuffer(input, "y");

						Gameplay.DeleteMapFlag(output_buffer, clientThread.GetUserID(), x, y);
					}
					else if (action.equals("delete_message"))
					{
						int message_time = Constants.FetchParameterIntFromBuffer(input, "message_time");

						Comm.DeleteMessage(output_buffer, clientThread.GetUserID(), message_time);
					}
					break;

				case 'e':
					if (action.equals("end_replay"))
					{
						// Switch the user to their homeland map.
						Raid.EndReplay(output_buffer, clientThread.GetUserID());
					}
					else if (action.equals("evacuate"))
					{
						int x = Constants.FetchParameterIntFromBuffer(input, "x");
						int y = Constants.FetchParameterIntFromBuffer(input, "y");
						boolean auto = (Constants.FetchParameterIntFromBuffer(input, "auto") != 0);

						Gameplay.Evacuate(output_buffer, clientThread.GetUserID(), x, y);

						if (!auto) clientThread.GetDeviceData().UpdateCorrelationsForActivity();
					}
					else if (action.equals("event_connect"))
					{
						// Fetch paramaters
						String uid = Constants.FetchParameterFromBuffer(input, "uid", false);
						String basic_uid = Constants.FetchParameterFromBuffer(input, "basic_uid", false);
						int client_version = Constants.FetchParameterIntFromBuffer(input, "client_version");
						String activation_code = Constants.FetchParameterFromBuffer(input, "activation_code", false);
						String device_type = Constants.FetchParameterFromBuffer(input, "device_type", false);
						boolean enter_game = (Constants.FetchParameterIntFromBuffer(input, "enter_game") != 0);

						// Shorten the device uid and basic uid to the max allowed length.
						uid = uid.substring(0, Math.min(uid.length(), DeviceData.MAX_DEVICE_NAME_LEN));
						basic_uid = basic_uid.substring(0, Math.min(basic_uid.length(), DeviceData.MAX_DEVICE_NAME_LEN));

						// Shorten the device type to the max allowed length.
						device_type = device_type.substring(0, Math.min(device_type.length(), DeviceData.MAX_DEVICE_TYPE_LEN));

						if (uid.length() == 0)
						{
							Output.PrintToScreen("Error: event_connect: Blank uid '" + uid + "' given in message '" + input + "'.");
							break;
						}

						// Get (or create) a DeviceData corresponding to the client device uid.
						DeviceData device_data = DeviceData.GetDeviceDataByName(uid, basic_uid);

						// Make sure the device data exists.
						if (device_data == null)
						{
							Output.PrintToScreen("Error: event_connect: no device data exists for device uid '" + uid + "'.");
							break;
						}

						// Record the device's model and IP address in the DeviceData.
						device_data.device_type = device_type;
						device_data.prev_IP = clientThread.GetClientIP();

						// TESTING
						Output.PrintToScreen("Event connect uid: '" + uid + "', basic_uid: '" + basic_uid + "', basic_device_ID: " + DeviceData.GetDeviceIDByName(basic_uid) + ", device_data.ID: " + device_data.ID + ".");

						// Coassociate this device with any other devices that share the same basic UID (that is, other Steam accounts, or a non-Steam account).
						device_data.CoassociateDevicesByUID();
/*
						// If the device's uid is not the same as its basic uid (which is the case for a Steam account), co-associate the two.
						if ((!uid.equals(basic_uid)) && (!basic_uid.equals("")))
						{
							int basic_device_ID = DeviceData.GetDeviceIDByName(basic_uid);
							if ((basic_device_ID != -1) && (basic_device_ID != device_data.ID))
							{
								Output.PrintToScreen("Associating device " + device_data.ID + " with basic device " + basic_device_ID);
								DeviceData.CoassociateDevices(device_data, DeviceData.GetDeviceDataByName(basic_uid));
							}
						}
*/
						// Set the client thread's unique client ID. This is only provided with event_connect.
						// Truncate it to the maximum length that will be stored. Also record DeviceData.
						clientThread.SetDeviceData(uid, basic_uid, device_data);

						// Record the activation code being used by this client
						clientThread.SetClientActivationCode(activation_code);

						// Attempt to have this client connect to the game server.
						Login.AttemptConnect(output_buffer, clientThread, client_version, enter_game);
					}
					else if (action.equals("event_patron_offer"))
					{
						int targetUserID = Constants.FetchParameterIntFromBuffer(input, "targetUserID");

						// Send a patron offer to the given user.
						Comm.SendPatronOffer(output_buffer, clientThread.GetUserID(), targetUserID);
					}
					else if (action.equals("event_withdraw_alliance"))
					{
						int targetNationID = Constants.FetchParameterIntFromBuffer(input, "targetNationID");

						// Withdraw the alliance request
						Alliance.WithdrawAllianceRequest(output_buffer, clientThread.GetUserID(), targetNationID);
					}
					else if (action.equals("event_accept_alliance"))
					{
						int targetNationID = Constants.FetchParameterIntFromBuffer(input, "targetNationID");

						Alliance.AcceptAlliance(output_buffer, clientThread.GetUserID(), targetNationID);
					}
					else if (action.equals("event_decline_alliance"))
					{
						int targetNationID = Constants.FetchParameterIntFromBuffer(input, "targetNationID");

						Alliance.DeclineAlliance(output_buffer, clientThread.GetUserID(), targetNationID);
					}
					else if (action.equals("event_break_alliance"))
					{
						int targetNationID = Constants.FetchParameterIntFromBuffer(input, "targetNationID");

						Alliance.AttemptBreakAlliance(output_buffer, clientThread.GetUserID(), targetNationID);
					}
					else if (action.equals("event_request_alliance"))
					{
						int otherNationID = Constants.FetchParameterIntFromBuffer(input, "targetNationID");

						Alliance.RequestAlliance(output_buffer, clientThread.GetUserID(), otherNationID);
					}
					else if (action.equals("event_withdraw_unite"))
					{
						int targetNationID = Constants.FetchParameterIntFromBuffer(input, "targetNationID");

						// Withdraw the unite request
						Alliance.WithdrawUniteRequest(output_buffer, clientThread.GetUserID(), targetNationID);
					}
					else if (action.equals("event_accept_unite"))
					{
						int targetNationID = Constants.FetchParameterIntFromBuffer(input, "targetNationID");

						Alliance.AcceptUnite(output_buffer, clientThread.GetUserID(), targetNationID);
					}
					else if (action.equals("event_decline_unite"))
					{
						int targetNationID = Constants.FetchParameterIntFromBuffer(input, "targetNationID");

						Alliance.DeclineUnite(output_buffer, clientThread.GetUserID(), targetNationID);
					}
					else if (action.equals("event_request_unite"))
					{
						int otherNationID = Constants.FetchParameterIntFromBuffer(input, "targetNationID");
						int payment_offer = Constants.FetchParameterIntFromBuffer(input, "payment_offer");

						Alliance.RequestUnite(output_buffer, clientThread.GetUserID(), otherNationID, payment_offer);
					}
					else if (action.equals("event_center_on_nation"))
					{
						String center_nation_name = Constants.FetchParameterFromBuffer(input, "center_nation_name", false);

						// Attempt to center view on given nation
						Display.CenterOnNation(output_buffer, clientThread.GetUserID(), center_nation_name);

						int nationID = DataManager.GetNameToIDMap(Constants.DT_NATION, center_nation_name);
						if (nationID > 0) {
							OutputEvents.GetNationInfoEvent(output_buffer,clientThread.GetUserID(), nationID);
						}
					}
					else if (action.equals("event_center_on_block"))
					{
						UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, clientThread.GetUserID(), false);

						int blockX = Constants.FetchParameterIntFromBuffer(input, "blockX");
						int blockY = Constants.FetchParameterIntFromBuffer(input, "blockY");

						// Center the user's view on the given coordinates
						Display.CenterViewOnBlock(clientThread, output_buffer, blockX, blockY);
					}
					break;

				case 'f':
					if (action.equals("file_report"))
					{
						int report_userID = Constants.FetchParameterIntFromBuffer(input, "userID");
						String report_username = Constants.FetchParameterFromBuffer(input, "username", false);
						String report_issue = Constants.FetchParameterFromBuffer(input, "issue", false);
						String report_text = Constants.FetchParameterFromBuffer(input, "text", false);

						Comm.FileReport(output_buffer, clientThread.GetUserID(), report_userID, report_username, report_issue, report_text);
					}
					else if (action.equals("forgot_password"))
					{
						String username = Constants.FetchParameterFromBuffer(input, "username", false);

						// Call ForgotPassword()
						Login.ForgotPassword(output_buffer, username, clientThread);
					}
					break;

				case 'g':
					if (action.equals("get_nation_orbs"))
					{
						// Get nation orbs event
						UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, clientThread.GetUserID(), false);
						OutputEvents.GetNationOrbsEvent(output_buffer,userData.nationID);
					}
					else if (action.equals("get_orb_winnings"))
					{
						int orbX = Constants.FetchParameterIntFromBuffer(input, "x");
						int orbY = Constants.FetchParameterIntFromBuffer(input, "y");

						// Get orb ranks data event
						OutputEvents.GetOrbWinningsEvent(output_buffer, clientThread.GetUserID(), orbX, orbY);
					}
					else if (action.equals("get_payment"))
					{
						//// The user is purchasing credits -- check for the payment.
						//Money.CheckForPayments();

						// The user has subscribed -- check subscriptions.
						Money.CheckSubscriptions();
					}
					else if (action.equals("get_ranks_data"))
					{
						// Get ranks data event
						OutputEvents.GetRanksDataEvent(output_buffer, clientThread.GetUserID());
					}
					break;

				case 'j':
					if (action.equals("join_nation"))
					{
						String nation_name = Constants.FetchParameterFromBuffer(input, "nation", false);
						String password = Constants.FetchParameterFromBuffer(input, "password", false);

						Application.JoinNationRequest(output_buffer, clientThread.GetUserID(), nation_name, password, clientThread);
					}
					break;

				case 'l':
					if (action.equals("log_in"))
					{
						String username = Constants.FetchParameterFromBuffer(input, "username", false);
						String password = Constants.FetchParameterFromBuffer(input, "password", false);

						// Attempt login
						Login.AttemptLogin(output_buffer, username, password, false, clientThread);
					}
					else if (action.equals("log_out"))
					{
						// Logout
						Login.Logout(clientThread, true);
					}
					break;

				case 'm':
					if (action.equals("mapclick"))
					{
						int x = Constants.FetchParameterIntFromBuffer(input, "x");
						int y = Constants.FetchParameterIntFromBuffer(input, "y");
						boolean splash = (Constants.FetchParameterIntFromBuffer(input, "splash") != 0);
						boolean auto = (Constants.FetchParameterIntFromBuffer(input, "auto") != 0);

						Gameplay.MapClick(output_buffer, clientThread.GetUserID(), x, y, splash, auto);

						if (!auto) clientThread.GetDeviceData().UpdateCorrelationsForActivity();
					}
					else if (action.equals("messages_checked"))
					{
						// Record that this user has checked messages
						Comm.MessagesChecked(output_buffer, clientThread.GetUserID());
					}
					else if (action.equals("migrate"))
					{
						// Detrmine whether to migrate just a colony, or the entire nation.
						boolean colony = (Constants.FetchParameterIntFromBuffer(input, "colony") != 0);

						// For detecting possible hack
						Output.PrintToScreen("Migrate command given by userID: " + clientThread.GetUserID() + ", IP: " + clientThread.clientIP + ", client index: " + clientThread.GetClientIndex() + ", client ID: " + clientThread.GetClientID() + ", colony: " + colony);

						// Migrate the nation
						Gameplay.Migrate(output_buffer, clientThread.GetUserID(), !colony);
					}
					else if (action.equals("mod_fetch_complaint"))
					{
						int skip = Constants.FetchParameterIntFromBuffer(input, "skip");
						int index = GlobalData.instance.complaints.indexOf(skip) + 1;
						OutputEvents.GetNextComplaintEvent(output_buffer, clientThread.GetUserID(), index);
					}
					else if (action.equals("mod_resolve_complaint"))
					{
						int complaintID = Constants.FetchParameterIntFromBuffer(input, "ID");
						int act = Constants.FetchParameterIntFromBuffer(input, "act");
						int ban_days = Constants.FetchParameterIntFromBuffer(input, "ban_days");
						String message = Constants.FetchParameterFromBuffer(input, "message", false);
						String log = Constants.FetchParameterFromBuffer(input, "log", false);

						// Resolve the complaint
						Admin.ResolveComplaint(output_buffer, clientThread.GetUserID(), complaintID, act, ban_days, message, log);
					}
					else if (action.equals("mute"))
					{
						// Mute the given user and device for this user.
						Comm.Mute(clientThread.GetUserID(), Constants.FetchParameterIntFromBuffer(input, "userID"), Constants.FetchParameterIntFromBuffer(input, "deviceID"));
					}
					break;

				case 'n':
					if (action.equals("next_area"))
					{
						UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, clientThread.GetUserID(), false);
						World.SetUserViewToNextArea(userData, output_buffer);
						clientThread.GetDeviceData().UpdateCorrelationsForActivity();
					}
					else if (action.equals("new_player"))
					{
						String username = Constants.FetchParameterFromBuffer(input, "username", false);
						String patron_code = Constants.FetchParameterFromBuffer(input, "patron_code", false);

						// Call new player creation method
						Application.AttemptCreateNewPlayer(output_buffer, clientThread, username, patron_code);
					}
					break;

				case 'p':
					if (action.equals("pan_view"))
					{
						UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, clientThread.GetUserID(), false);
						if (userData == null) {Output.PrintToScreen("ERROR: Event pan_view, no data for userID " + clientThread.GetUserID() + ", in_game: " + clientThread.in_game + ", logged_in: " + clientThread.logged_in); break;}
						Display.SetUserView(userData, Constants.FetchParameterIntFromBuffer(input, "x"), Constants.FetchParameterIntFromBuffer(input, "y"), false, output_buffer);
						clientThread.GetDeviceData().UpdateCorrelationsForActivity();
					}
					else if (action.equals("patron_offer_accept"))
					{
						int targetUserID = Constants.FetchParameterIntFromBuffer(input, "targetUserID");
						Comm.PatronOfferAccept(output_buffer, clientThread.GetUserID(), targetUserID);
					}
					else if (action.equals("patron_offer_decline"))
					{
						int targetUserID = Constants.FetchParameterIntFromBuffer(input, "targetUserID");
						Comm.PatronOfferDecline(output_buffer, clientThread.GetUserID(), targetUserID);
					}
					else if (action.equals("ping"))
					{
						// Record that this client is still active, so it will not be disconnected.
						clientThread.prev_use_fine_time = Constants.GetFineTime();
					}
					else if (action.equals("purchase_advance"))
					{
						int techID = Constants.FetchParameterIntFromBuffer(input, "techID");
						Technology.Purchase(output_buffer, clientThread.GetUserID(), techID);
						clientThread.GetDeviceData().UpdateCorrelationsForActivity();
					}
					else if (action.equals("post_message"))
					{
						String recipient = Constants.FetchParameterFromBuffer(input, "recipient", false);
						String text = Constants.FetchParameterFromBuffer(input, "text", true);

						ClientString error_message = Comm.PostMessage(clientThread.GetUserID(), clientThread.GetDeviceData().ID, recipient, text);

						// Get post message result event
						OutputEvents.GetPostMessageResultEvent(output_buffer, error_message);

						clientThread.GetDeviceData().UpdateCorrelationsForActivity();
					}
					break;

				case 'q':
					if (action.equals("quest_collect"))
					{
						int questID = Constants.FetchParameterIntFromBuffer(input, "questID");
						Quests.Collect(output_buffer, clientThread.GetUserID(), questID);
						clientThread.GetDeviceData().UpdateCorrelationsForActivity();
					}
					else if (action.equals("quit_client"))
					{
						// Exit the client
						//Output.PrintToScreen("About to call QuitClient() on clientThread " + GetClientIndex() + " ID " + GetClientID());
						Login.QuitClient(clientThread);
					}
					break;

				case 'r':
					if (action.equals("raid"))
					{
						// Begin, join, or exit a raid.
						Raid.OnRaidCommand(output_buffer, clientThread.GetUserID(), -1);
					}
					else if (action.equals("raid_timeout"))
					{
						// The user's raid has timed out.
						Raid.OnRaidTimeout(output_buffer, clientThread.GetUserID());
					}
					else if (action.equals("rebirth"))
					{
						// Rebirth the player's nation
						Gameplay.AttemptRebirthNation(output_buffer, clientThread.GetUserID());
					}
					else if (action.equals("recruit"))
					{
						String emails[] = new String[4];

						String name = Constants.FetchParameterFromBuffer(input, "name", false);
						emails[0] = Constants.FetchParameterFromBuffer(input, "email1", false);
						emails[1] = Constants.FetchParameterFromBuffer(input, "email2", false);
						emails[2] = Constants.FetchParameterFromBuffer(input, "email3", false);
						emails[3] = Constants.FetchParameterFromBuffer(input, "email4", false);

						// Send e-mails
						Gameplay.Recruit(output_buffer, clientThread.GetUserID(), name, emails);
					}
					else if (action.equals("remove_follower"))
					{
						int targetUserID = Constants.FetchParameterIntFromBuffer(input, "targetUserID");
						Comm.AttemptRemoveFollower(output_buffer, clientThread.GetUserID(), targetUserID);
					}
					else if (action.equals("replay_raid"))
					{
						int raidID = Constants.FetchParameterIntFromBuffer(input, "raidID");
						Raid.Replay(output_buffer, clientThread.GetUserID(), raidID);
					}
					else if (action.equals("research_advance"))
					{
						int techID = Constants.FetchParameterIntFromBuffer(input, "techID");
						Technology.Research(output_buffer, clientThread.GetUserID(), techID);
						clientThread.GetDeviceData().UpdateCorrelationsForActivity();
					}
					else if (action.equals("request_fealty_info"))
					{
						OutputEvents.GetFealtyInfoEvent(output_buffer, clientThread.GetUserID());
					}
					else if (action.equals("request_join_tournament"))
					{
						TournamentData.instance.AttemptJoinTournament(output_buffer, clientThread.GetUserID());
					}
					else if (action.equals("request_more_messages"))
					{
						int type = Constants.FetchParameterIntFromBuffer(input, "type");
						int start = Constants.FetchParameterIntFromBuffer(input, "start");
						Comm.FetchMoreMessages(output_buffer, clientThread.GetUserID(), type, start);
					}
					else if (action.equals("request_nation_areas"))
					{
						OutputEvents.GetNationAreasEvent(output_buffer, clientThread.GetUserID());
					}
					else if (action.equals("request_nation_info"))
					{
						int targetNationID = Constants.FetchParameterIntFromBuffer(input, "targetNationID");

						// Add the nation info event string
						OutputEvents.GetNationInfoEvent(output_buffer, clientThread.GetUserID(), targetNationID);
					}
					else if (action.equals("request_stats"))
					{
						// Add the stats event string
						UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, clientThread.GetUserID(), false);
						OutputEvents.GetStatsEvent(output_buffer, (userData == null) ? -1 : userData.nationID, 0);
					}
					else if (action.equals("reset_advances"))
					{
						// Attempt to reset the nation's advances.
						Gameplay.AttemptResetAdvances(output_buffer, clientThread.GetUserID());
					}
					break;

				case 's':
					if (action.equals("salvage"))
					{
						int x = Constants.FetchParameterIntFromBuffer(input, "x");
						int y = Constants.FetchParameterIntFromBuffer(input, "y");

						Gameplay.Salvage(output_buffer, clientThread.GetUserID(), x, y);

						clientThread.GetDeviceData().UpdateCorrelationsForActivity();
					}
					else if (action.equals("send_to_home_nation"))
					{
						int memberID = Constants.FetchParameterIntFromBuffer(input, "memberID");
						Application.SendUserToHomeNation(output_buffer, clientThread.GetUserID(), memberID);
					}
					else if (action.equals("set_map_flag"))
					{
						int x = Constants.FetchParameterIntFromBuffer(input, "x");
						int y = Constants.FetchParameterIntFromBuffer(input, "y");
						String desc = Constants.FetchParameterFromBuffer(input, "text", false);

						Gameplay.SetMapFlag(output_buffer, clientThread.GetUserID(), x, y, desc);
					}
					else if (action.equals("set_nation_flags"))
					{
						int flags = Constants.FetchParameterIntFromBuffer(input, "flags");
						Gameplay.SetNationFlags(output_buffer, clientThread.GetUserID(), flags);
					}
					else if (action.equals("set_nation_password"))
					{
						String new_password = Constants.FetchParameterFromBuffer(input, "password", false);

						// Change the nation's password
						Gameplay.ChangePassword(output_buffer, clientThread.GetUserID(), new_password);
					}
					else if (action.equals("set_target"))
					{
						int advanceID = Constants.FetchParameterIntFromBuffer(input, "ID");

						// Change the nation's target advance
						Technology.SetTargetAdvance(output_buffer, clientThread.GetUserID(), advanceID);
					}
					else if (action.equals("set_user_flags"))
					{
						// Get the user data
						UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, clientThread.GetUserID(), false);

						// Set the user's flags
						int flags = Constants.FetchParameterIntFromBuffer(input, "flags");
						userData.flags = flags;

						// Mark the user's data to be updated
						DataManager.MarkForUpdate(userData);
					}
					else if (action.equals("set_username"))
					{
						String username = Constants.FetchParameterFromBuffer(input, "name", false);
						boolean check = (Constants.FetchParameterIntFromBuffer(input, "check") != 0);

						// Set the player's username, if it's available.
						Application.SetUsername(output_buffer, clientThread, username, check);
					}
					else if (action.equals("set_nation_color"))
					{
						int r = Constants.FetchParameterIntFromBuffer(input, "r");
						int g = Constants.FetchParameterIntFromBuffer(input, "g");
						int b = Constants.FetchParameterIntFromBuffer(input, "b");
						Application.SetNationColor(output_buffer, clientThread, r, g, b);
					}
					else if (action.equals("set_nation_name"))
					{
						String nation_name = Constants.FetchParameterFromBuffer(input, "name", false);
						boolean check = (Constants.FetchParameterIntFromBuffer(input, "check") != 0);

						// Set the player's nation's name, if it's available.
						Application.SetNationName(output_buffer, clientThread, nation_name, check);
					}
					else if (action.equals("switch_map"))
					{
						// Switch the user from mainland to homeland, or vice-versa.
						Display.SwitchMap(output_buffer, clientThread.GetUserID());
					}
					else if (action.equals("switch_subscription_bonus"))
					{
						// Switch the user's subscription bonus of the given type to their current nation.
						Subscription.SwitchSubscriptionBonus(output_buffer, clientThread.GetUserID(), Constants.FetchParameterIntFromBuffer(input, "bonus"));
					}
					break;

				case 't':
					if (action.equals("tutorial_state"))
					{
						String val = Constants.FetchParameterFromBuffer(input, "val", true);
						Gameplay.SaveTutorialState(clientThread.GetUserID(), val);
					}
					else if (action.equals("trade_in"))
					{
						Money.TradeInWinnings(clientThread.GetUserID());
					}
					break;

				case 'u':
					if (action.equals("unmute"))
					{
						// Unmute the given user and device for this user.
						Comm.Unmute(clientThread.GetUserID(), Constants.FetchParameterIntFromBuffer(input, "userID"), Constants.FetchParameterIntFromBuffer(input, "deviceID"));
					}
					else if (action.equals("unmute_all"))
					{
						// Unmute all users and devices for this user.
						Comm.UnmuteAll(clientThread.GetUserID());
					}
					else if (action.equals("unsubscribe"))
					{
						// Cancel this user's subscription.
						Subscription.Unsubscribe(output_buffer, clientThread.GetUserID());
					}
					else if (action.equals("upgrade"))
					{
						int buildID = Constants.FetchParameterIntFromBuffer(input, "buildID");
						int x = Constants.FetchParameterIntFromBuffer(input, "x");
						int y = Constants.FetchParameterIntFromBuffer(input, "y");

						Gameplay.Upgrade(output_buffer, clientThread.GetUserID(), buildID, x, y);
					}
					break;
			}
		} catch (Exception e) {
			Output.PrintTimeToScreen("Exception while processing event data `" + input
			+ "`\nFrom userID " + clientThread.GetUserID()
			+ " at IP " + clientThread.clientIP
			+ ", client index: " + clientThread.GetClientIndex()
			/* + ".\nStack Trace:"*/);
			Output.PrintException(e);
		}

		// If an output string has been created, send it to the client.
		if (output_buffer.length() > 0)
		{
	//		Output.PrintToScreen("Output event: '" + output_buffer.toString() + "'");

			// Send the output string to the client
			clientThread.TerminateAndSendNow(output_buffer);
		}

		// TESTING
		//Output.PrintToScreen("EVENT " + action + " TOOK " + (Constants.GetFreshFineTime() - event_start_time) + " MS");

		if (Constants.GetFreshFineTime() - event_start_time > 2000)
		{
			Constants.WriteToLog("log_lag.txt", "\n EVENT " + action + " TOOK " + (Constants.GetFreshFineTime() - event_start_time) + " MS!\n");
		}

		//if (event_start_free_mem - Runtime.getRuntime().freeMemory() > 500)
		//{
		//	Constants.WriteToLog("log_memory.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " EVENT " + action + " TOOK " + (event_start_free_mem - Runtime.getRuntime().freeMemory()) + " Bytes\n");
		//}

		// Delete any clients that were queued to be deleted by the processing of this event. The client thread that triggered the event could
		// not be deleted until the event has finished being processing and any return messages are sent.
		WOCServer.DeleteQueuedClients();
	}
};
