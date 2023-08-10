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
import java.util.ArrayList;

public class Alliance
{
	public static void RequestAlliance(StringBuffer _output_buffer, int _userID, int _targetNationID)
	{
		// Get the user's data and nation ID
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);
		int userNationID = userData.nationID;

		if (userData.rank > Constants.RANK_WARRIOR)
		{
			// Return message
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_alliance_request_rank_too_low")); // You cannot make alliances until you are promoted to Warrior.
			return;
		}

		// If target nation IS the user's nation, return message to that effect.
		if (_targetNationID == userNationID)
		{
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_alliance_self")); // Your nation cannot form an alliance with itself.
			return;
		}

		// Get the user's nation's data
		NationData userNationData = (NationData)DataManager.GetData(Constants.DT_NATION, userNationID, false);

		// Get the target nation's data
		NationData targetNationData = (NationData)DataManager.GetData(Constants.DT_NATION, _targetNationID, false);

		// If the user's nation already has an alliance with the target nation, return message.
		if (userNationData.alliances_active.contains(Integer.valueOf(_targetNationID)))
		{
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_alliance_already", "nation_name", targetNationData.name)); // "Your nation already has an alliance with " + targetNationData.name + "."
			return;
		}

		// If the user's nation has already requested an alliance with the target nation, return message.
		if (userNationData.alliances_requests_outgoing.contains(Integer.valueOf(_targetNationID)))
		{
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_alliance_already_requested", "nation_name", targetNationData.name)); // "Your nation has already requested an alliance with " + targetNationData.name + "."
			return;
		}

    // Do not allow nations with too great a level difference to ally.
		if (Math.abs(userNationData.level - targetNationData.level) > Constants.ALLY_LEVEL_DIFF_LIMIT)
		{
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_alliance_level_difference", "max_level_dif", String.valueOf(Constants.ALLY_LEVEL_DIFF_LIMIT))); // "Nations with a level difference of more than " + Constants.ALLY_LEVEL_DIFF_LIMIT + " cannot be allies."
			return;
		}

		// If the target nation has already requested an alliance with the user's nation, accept that request.
		if (userNationData.alliances_requests_incoming.contains(Integer.valueOf(_targetNationID)))
		{
			AcceptAlliance(_output_buffer, _userID, _targetNationID);
			return;
		}

		int cur_day = Constants.GetAbsoluteDay();

		if (userNationData.prev_alliance_request_day == cur_day)
		{
			if (userNationData.alliance_request_count >= Constants.MAX_ALLIANCE_REQUEST_COUNT_PER_DAY)
			{
				OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_alliance_max_requests", "max_num_requests", String.valueOf(Constants.MAX_ALLIANCE_REQUEST_COUNT_PER_DAY))); // "We've already sent the maximum of " + Constants.MAX_ALLIANCE_REQUEST_COUNT_PER_DAY + " alliance requests today."
				return;
			}
			else
			{
				userNationData.alliance_request_count++;
			}
		}
		else
		{
			userNationData.prev_alliance_request_day = cur_day;
			userNationData.alliance_request_count = 1;
		}

		// Add the targetNationID to the user's nation's list of outgoing alliance requests
		userNationData.alliances_requests_outgoing.add(_targetNationID);

		// Add the userNationID to the target nation's list of incoming alliance requests
		targetNationData.alliances_requests_incoming.add(userNationID);

		// Mark both nations' data to be updated
		DataManager.MarkForUpdate(userNationData);
		DataManager.MarkForUpdate(targetNationData);

		// Broadcast updated alliance lists to both nations.
		OutputEvents.BroadcastAlliancesEvent(userNationID);
		OutputEvents.BroadcastAlliancesEvent(_targetNationID);

    //Constants.WriteToLog("log_MFU.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " "  + userNationData.ID + " evt: RequestAllianceProcess\n");
    //Constants.WriteToLog("log_MFU.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " "  + targetNationData.ID + " evt: RequestAllianceProcess\n");

    // Post report to the requesting nation
		Comm.SendReport(userNationID, ClientString.Get("svr_report_user_requested_alliance", "username", userData.name, "nation_name", targetNationData.name), 0); // userData.name + " has requested an alliance with " + targetNationData.name + "."

		// Return success message
		OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_alliance_request", "nation_name", targetNationData.name)); // "Your nation has requested an alliance with " + targetNationData.name + "."
	}

	public static void WithdrawAllianceRequest(StringBuffer _output_buffer, int _userID, int _targetNationID)
	{
		// Get the user's data and nation ID
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);
		int userNationID = userData.nationID;

		if (userData.rank > Constants.RANK_WARRIOR)
		{
			// Return message
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_alliance_withdraw_rank_too_low")); // "You cannot do this until you are promoted to Warrior."
			return;
		}

		// Reciprocally remove the alliance request.
		RemoveAllianceRequest(userNationID, _targetNationID);

		// Get the target nation's data.
		NationData targetNationData = (NationData)DataManager.GetData(Constants.DT_NATION, _targetNationID, false);

		// Broadcast updated alliance lists to both nations.
		OutputEvents.BroadcastAlliancesEvent(userNationID);
		OutputEvents.BroadcastAlliancesEvent(_targetNationID);

		if (targetNationData != null)
		{
			if (_output_buffer != null) {
				OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_alliance_withdrawn", "nation_name", targetNationData.name)); // "The alliance invitation to " + targetNationData.name + " has been withdrawn."
			}
		}
	}

	public static void RemoveAllianceRequest(int _initiatorID, int _recipientID)
	{
		// Get the data for both nations
		NationData initiatorNationData = (NationData)DataManager.GetData(Constants.DT_NATION, _initiatorID, false);
		NationData recipientNationData = (NationData)DataManager.GetData(Constants.DT_NATION, _recipientID, false);

		// Remove the _recipientID from the initiator nation's list of outgoing alliance requests
		if (initiatorNationData != null) initiatorNationData.alliances_requests_outgoing.remove(Integer.valueOf(_recipientID));

		// Remove the _initiatorID from the recipient nation's list of incoming alliance requests
		if (recipientNationData != null) recipientNationData.alliances_requests_incoming.remove(Integer.valueOf(_initiatorID));

		// Mark both nations' data to be updated
		DataManager.MarkForUpdate(initiatorNationData);
		DataManager.MarkForUpdate(recipientNationData);
	}

	public static void AcceptAlliance(StringBuffer _output_buffer, int _userID, int _targetNationID)
	{
		// Get the user's data and nation ID
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);
		int userNationID = userData.nationID;

		if (userData.rank > Constants.RANK_WARRIOR)
		{
			// Return message
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_alliance_accept_rank_too_low")); // "You cannot do this until you are promoted to Warrior."
			return;
		}

		// First remove the request
		RemoveAllianceRequest(_targetNationID, userNationID);

		// Get the data for both nations
		NationData userNationData = (NationData)DataManager.GetData(Constants.DT_NATION, userNationID, false);
		NationData targetNationData = (NationData)DataManager.GetData(Constants.DT_NATION, _targetNationID, false);

		// If the two nations are already allied, do nothing.
		if (userNationData.alliances_active.contains(Integer.valueOf(_targetNationID))) {
			return;
		}

		// Add the userNationID to the target nation's list of active alliances
		targetNationData.alliances_active.add(userNationID);

		// Add the _targetNationID to the user's nation's list of active alliances
		userNationData.alliances_active.add(_targetNationID);

		// Broadcast stats events with updated energy stats, to both nation.
		OutputEvents.BroadcastStatsEvent(userNationID, 0);
		OutputEvents.BroadcastStatsEvent(_targetNationID, 0);

		// Broadcast updated alliance lists to both nations.
		OutputEvents.BroadcastAlliancesEvent(userNationID);
		OutputEvents.BroadcastAlliancesEvent(_targetNationID);

		// Update the quests system for the forming of this alliance.
		Quests.HandleFormAlliance(userNationData);
		Quests.HandleFormAlliance(targetNationData);

		// Record contact between users of both nations.
		Comm.RecordNationContactWithNation(userNationData, targetNationData, Comm.CONTACT_VALUE_NATION_ALLY);
		Comm.RecordNationContactWithNation(targetNationData, userNationData, Comm.CONTACT_VALUE_NATION_ALLY);

		// Mark both nations' data to be updated
		DataManager.MarkForUpdate(userNationData);
		DataManager.MarkForUpdate(targetNationData);

		// Post reports to both nations
		Comm.SendReport(_targetNationID, ClientString.Get("svr_report_other_nation_accepted_alliance", "accepting_nation_name", userNationData.name, "requesting_nation_name", targetNationData.name), 0); // userNationData.name + " has accepted our request for an alliance. There is now an alliance between " + targetNationData.name + " and " + userNationData.name + "."
		Comm.SendReport(userNationID, ClientString.Get("svr_report_our_nation_accepted_alliance", "username", userData.name, "accepting_nation_name", userNationData.name, "requesting_nation_name", targetNationData.name), 0); // userData.name + " has accepted "+ targetNationData.name + "'s request for an alliance. There is now an alliance between " + targetNationData.name + " and " + userNationData.name + "."

		OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_alliance_accepted")); // "The alliance has been accepted."
	}

	public static void DeclineAlliance(StringBuffer _output_buffer, int _userID, int _targetNationID)
	{
		// Get the user's data and nation ID
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);
		int userNationID = userData.nationID;

		if (userData.rank > Constants.RANK_WARRIOR)
		{
			// Return message
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_alliance_decline_rank_too_low")); // "You cannot do this until you are promoted to Warrior."
			return;
		}

		// First remove the request
		RemoveAllianceRequest(_targetNationID, userNationID);

		// Get the data for both nations
		NationData userNationData = (NationData)DataManager.GetData(Constants.DT_NATION, userNationID, false);
		NationData targetNationData = (NationData)DataManager.GetData(Constants.DT_NATION, _targetNationID, false);

		if ((userNationData == null) || (targetNationData == null)) {
			return;
		}

		// Broadcast updated alliance lists to both nations.
		OutputEvents.BroadcastAlliancesEvent(userNationID);
		OutputEvents.BroadcastAlliancesEvent(_targetNationID);

		// Mark both nations' data to be updated
		DataManager.MarkForUpdate(userNationData);
		DataManager.MarkForUpdate(targetNationData);

		if (_output_buffer != null) {
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_alliance_declined")); // "The alliance invitation has been declined."
		}
	}

	public static void AttemptBreakAlliance(StringBuffer _output_buffer, int _userID, int _targetNationID)
	{
		// Get the user's data and nation ID
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);
		int userNationID = userData.nationID;

		if (userData.rank > Constants.RANK_WARRIOR)
		{
			// Return message
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_alliance_break_rank_too_low")); // "You cannot do this until you are promoted to Warrior."
			return;
		}

		// Break the alliance.
		BreakAlliance(userNationID, _targetNationID, userData);

		// Output success message
		OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_alliance_broken")); // "The alliance has been broken."
	}

	public static void BreakAlliance(int _userNationID, int _targetNationID, UserData _userData)
	{
		// Get the data for both nations
		NationData userNationData = (NationData)DataManager.GetData(Constants.DT_NATION, _userNationID, false);
		NationData targetNationData = (NationData)DataManager.GetData(Constants.DT_NATION, _targetNationID, false);

		// Remove the user's nation ID from the target nation's list of active alliances
		if (targetNationData != null) targetNationData.alliances_active.remove(Integer.valueOf(_userNationID));

		// Remove the recipientID from the initiator nation's list of active alliances
		if (userNationData != null) userNationData.alliances_active.remove(Integer.valueOf(_targetNationID));

		// Broadcast stats events with updated energy stats, to both nation.
		OutputEvents.BroadcastStatsEvent(_userNationID, 0);
		OutputEvents.BroadcastStatsEvent(_targetNationID, 0);

		// Broadcast updated alliance lists to both nations.
		OutputEvents.BroadcastAlliancesEvent(_userNationID);
		OutputEvents.BroadcastAlliancesEvent(_targetNationID);

		// Mark both nations' data to be updated
		DataManager.MarkForUpdate(userNationData);
		DataManager.MarkForUpdate(targetNationData);

		if (_userData != null)
		{
			// Post reports to both nations
			Comm.SendReport(_userNationID, ClientString.Get("svr_report_alliance_broken_by", "nation_name", targetNationData.name, "username", _userData.name), 0); // "Our alliance with " + targetNationData.name + " has been broken by " + userData.name + "."
			Comm.SendReport(_targetNationID, ClientString.Get("svr_report_alliance_broken", "nation_name", userNationData.name), 0); // "Our alliance with " + userNationData.name + " has been broken."
		}
	}

	public static void BreakAllAlliances(NationData _nationData)
	{
		// Break active alliances
		while (_nationData.alliances_active.size() > 0) {
			Alliance.BreakAlliance(_nationData.ID, _nationData.alliances_active.get(0), null);
		}

		// Reject incoming alliance invitations
		while (_nationData.alliances_requests_incoming.size() > 0) {
			Alliance.RemoveAllianceRequest(_nationData.alliances_requests_incoming.get(0), _nationData.ID);
		}

		// Withdraw outgoing alliance invitations
		while (_nationData.alliances_requests_outgoing.size() > 0) {
			Alliance.RemoveAllianceRequest(_nationData.ID, _nationData.alliances_requests_outgoing.get(0));
		}
	}

	public static void RequestUnite(StringBuffer _output_buffer, int _userID, int _targetNationID, int _payment_offer)
	{
		// Get the user's data and nation ID
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);
		int userNationID = userData.nationID;

		// Do not allow if the user is not at least a cosovereign.
		if (userData.rank > Constants.RANK_COSOVEREIGN) {
			return;
		}

		// Do not allow if target nation IS the user's nation.
		if (_targetNationID == userNationID) {
			return;
		}

		// Get the user's nation's data
		NationData userNationData = (NationData)DataManager.GetData(Constants.DT_NATION, userNationID, false);

		// Get the target nation's data
		NationData targetNationData = (NationData)DataManager.GetData(Constants.DT_NATION, _targetNationID, false);

		// If the user's nation has already requested a unite with the target nation, do nothing.
		if (userNationData.unite_requests_outgoing.contains(Integer.valueOf(_targetNationID))) {
			return;
		}

		// If the payment offer exceeds the nation's number of transferable credits, do nothing. This should be caught by the client.
		if (_payment_offer > userNationData.GetTransferrableCredits()) {
			return;
		}

		// Add the targetNationID to the user's nation's list of outgoing unite requests, and the _payment_offer to the corresponding list of offers.
		userNationData.unite_requests_outgoing.add(_targetNationID);
		userNationData.unite_offers_outgoing.add(_payment_offer);

		// Add the userNationID to the target nation's list of incoming unite requests, and the _payment_offer to the corresponding list of offers.
		targetNationData.unite_requests_incoming.add(userNationID);
		targetNationData.unite_offers_incoming.add(_payment_offer);

		// Mark both nations' data to be updated
		DataManager.MarkForUpdate(userNationData);
		DataManager.MarkForUpdate(targetNationData);

		// Broadcast updated alliance lists (which includes unite invitations lists) to both nations.
		OutputEvents.BroadcastAlliancesEvent(userNationID);
		OutputEvents.BroadcastAlliancesEvent(_targetNationID);

    //Constants.WriteToLog("log_MFU.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " "  + userNationData.ID + " evt: RequestAllianceProcess\n");
    //Constants.WriteToLog("log_MFU.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " "  + targetNationData.ID + " evt: RequestAllianceProcess\n");

    // Post report to the requesting nation
		Comm.SendReport(userNationID, ClientString.Get("svr_report_user_invited_unite", "username", userData.name, "nation_name", targetNationData.name), 0);

		// Return success message
		OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_unite_invitation_sent", "nation", targetNationData.name));
	}

	public static void WithdrawUniteRequest(StringBuffer _output_buffer, int _userID, int _targetNationID)
	{
		// Get the user's data and nation ID
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);
		int userNationID = userData.nationID;
		NationData userNationData = (NationData)DataManager.GetData(Constants.DT_NATION, userNationID, false);

		// Do not allow if the user is not at least a cosovereign.
		if (userData.rank > Constants.RANK_COSOVEREIGN) {
			return;
		}

		// Reciprocally remove the unite requests.
		RemoveUniteRequest(userNationID, _targetNationID);

		// Get the target nation's data.
		NationData targetNationData = (NationData)DataManager.GetData(Constants.DT_NATION, _targetNationID, false);

		// Broadcast updated alliance lists (which includes unite requests) to both nations.
		OutputEvents.BroadcastAlliancesEvent(userNationID);
		OutputEvents.BroadcastAlliancesEvent(_targetNationID);

		if (targetNationData != null)
		{
			if (_output_buffer != null) {
				OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_unite_invitation_withdrawn", "nation1", userNationData.name, "nation2", targetNationData.name));
			}
		}
	}

	public static void RemoveUniteRequest(int _initiatorID, int _recipientID)
	{
		int index;

		// Get the data for both nations
		NationData initiatorNationData = (NationData)DataManager.GetData(Constants.DT_NATION, _initiatorID, false);
		NationData recipientNationData = (NationData)DataManager.GetData(Constants.DT_NATION, _recipientID, false);

		// Remove the _recipientID from the initiator nation's list of outgoing unite requests
		if (initiatorNationData != null)
		{
			if ((index = initiatorNationData.unite_requests_outgoing.indexOf(Integer.valueOf(_recipientID))) != -1)
			{
				initiatorNationData.unite_requests_outgoing.remove(index);
				initiatorNationData.unite_offers_outgoing.remove(index);
			}
		}

		// Remove the _initiatorID from the recipient nation's list of incoming unite requests
		if (recipientNationData != null)
		{
			if ((index = recipientNationData.unite_requests_incoming.indexOf(Integer.valueOf(_initiatorID))) != -1)
			{
				recipientNationData.unite_requests_incoming.remove(index);
				recipientNationData.unite_offers_incoming.remove(index);
			}
		}

		// Mark both nations' data to be updated
		DataManager.MarkForUpdate(initiatorNationData);
		DataManager.MarkForUpdate(recipientNationData);
	}

	public static void AcceptUnite(StringBuffer _output_buffer, int _userID, int _targetNationID)
	{
		int i, userID, token, x, y;
		int coord_array[] = new int[2];
		String log_text;

		UserData curUserData;
		ArrayList<Integer> online_users = new ArrayList<Integer>();
		ArrayList<ClientThread> online_clients = new ArrayList<ClientThread>();

		// Get the user's data and nation ID
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);
		int userNationID = userData.nationID;

		// Get the data for both nations
		NationData userNationData = (NationData)DataManager.GetData(Constants.DT_NATION, userNationID, false);
		NationData targetNationData = (NationData)DataManager.GetData(Constants.DT_NATION, _targetNationID, false);

		// Do not allow if the user is not at least a cosovereign.
		if (userData.rank > Constants.RANK_COSOVEREIGN) {
			return;
		}

		// Do not allow if the nation is attempting to unite into itself.
		if (userNationID == _targetNationID) {
			return;
		}

		// Determine the index of this request in the user's nation's list of incoming unite requests.
		int request_index = userNationData.unite_requests_incoming.indexOf(Integer.valueOf(_targetNationID));

		// Do nothing if the user's nation has not received a unite request from the target nation.
		if (request_index == -1) {
			return;
		}

		// Determine the payment offer associated with this unite request.
		int payment_offer = userNationData.unite_offers_incoming.get(request_index);

		// If not enough time has passed since the user's nation last united, return message.
		if ((Constants.GetTime() - userNationData.prev_unite_time) < Constants.UNITE_PERIOD)
		{
			// Return requestor message
			OutputEvents.GetRequestorEvent(_output_buffer, ClientString.Get("svr_unite_too_soon", "period", Integer.toString(Constants.UNITE_PERIOD / Constants.SECONDS_PER_DAY), "nation_name", userNationData.name)); // "A nation must wait {period} days between unites. {nation_name} united more recently than that, and so cannot unite again this soon."
			return;
		}

		// If not enough time has passed since the target nation last united, return message.
		if ((Constants.GetTime() - targetNationData.prev_unite_time) < Constants.UNITE_PERIOD)
		{
			// Return requestor message
			OutputEvents.GetRequestorEvent(_output_buffer, ClientString.Get("svr_unite_too_soon", "period", Integer.toString(Constants.UNITE_PERIOD / Constants.SECONDS_PER_DAY), "nation_name", targetNationData.name)); // "A nation must wait {period} days between unites. {nation_name} united more recently than that, and so cannot unite again this soon."
			return;
		}

		// If the target nation does not have enough transferable credits to pay for the unite's payment offer, return message.
		if (targetNationData.GetTransferrableCredits() < payment_offer)
		{
			// Return requestor message
			OutputEvents.GetRequestorEvent(_output_buffer, ClientString.Get("svr_unite_not_enough_transferable_credits", "nation1", userNationData.name, "nation2", targetNationData.name, "cost", Integer.toString(Constants.UNITE_COST))); // "Combined, {nation1} and {nation2} do not have enough credits to pay the cost of {cost}<sprite=2>."
			return;
		}

		// If the combined nation does not have enough credits to pay for the unite, return message.
		if ((userNationData.game_money + targetNationData.game_money) < (Constants.UNITE_COST + payment_offer))
		{
			// Return requestor message
			OutputEvents.GetRequestorEvent(_output_buffer, ClientString.Get("svr_unite_not_enough_credits", "nation1", userNationData.name, "nation2", targetNationData.name, "cost", Integer.toString(Constants.UNITE_COST))); // "Combined, {nation1} and {nation2} do not have enough credits to pay the cost of {cost}<sprite=2>."
			return;
		}

		// Determine what the footprint of the combined nation would be.
		int combined_x0 = Math.min(userNationData.mainland_footprint.x0, targetNationData.mainland_footprint.x0);
		int combined_y0 = Math.min(userNationData.mainland_footprint.y0, targetNationData.mainland_footprint.y0);
		int combined_x1 = Math.max(userNationData.mainland_footprint.x1, targetNationData.mainland_footprint.x1);
		int combined_y1 = Math.max(userNationData.mainland_footprint.y1, targetNationData.mainland_footprint.y1);

		// If the combined nation would extend over a range larger than the maximum allowed, return message.
		if (((combined_x1 - combined_x0 + 1) > Constants.NATION_MAX_EXTENT) || ((combined_y1 - combined_y0 + 1) > Constants.NATION_MAX_EXTENT))
		{
			OutputEvents.GetRequestorEvent(_output_buffer, ClientString.Get("svr_unite_too_large", "max_extent", Integer.toString(Constants.NATION_MAX_EXTENT))); // "Combined, {nation1} and {nation2} do not have enough credits to pay the cost of {cost}<sprite=2>."
			return;
		}

		// Determine the combined rebirth countdown. Combine the difference from starting value for both nations. This will prevent using unite to delay rebirth, but also preserve any countdown that was purchased by either nation.
		float result_countdown = Constants.REBIRTH_COUNTDOWN_START - ((Constants.REBIRTH_COUNTDOWN_START - userNationData.rebirth_countdown) + (Constants.REBIRTH_COUNTDOWN_START - targetNationData.rebirth_countdown));
		int result_countdown_start = (int)(Math.max(Math.max(userNationData.rebirth_countdown_start, targetNationData.rebirth_countdown_start), result_countdown) + 0.5f);
		float result_countdown_purchased = targetNationData.rebirth_countdown_purchased + userNationData.rebirth_countdown_purchased;

		// If the combined nation's rebirth countdown would be <= 0, return message.
		if (result_countdown <= 0)
		{
			// Return requestor message
			OutputEvents.GetRequestorEvent(_output_buffer, ClientString.Get("svr_unite_not_enough_countdown", "nation1", userNationData.name, "nation2", targetNationData.name)); // "Combined, {nation1} and {nation2} do not have high enough countdown to rebirth to unite."
			return;
		}

		// If the combined nation's rebirth_countdown_purchased would be > MAX_REBIRTH_COUNTDOWN_PURCHASED, return message.
		if (result_countdown_purchased > Constants.MAX_REBIRTH_COUNTDOWN_PURCHASED)
		{
			// Return requestor message
			OutputEvents.GetRequestorEvent(_output_buffer, ClientString.Get("svr_unite_purchased_too_much_countdown", "nation1", userNationData.name, "nation2", targetNationData.name)); // "Combined, {nation1} and {nation2} have purchased too much rebirth countdown to be able to unite."
			return;
		}

		// Unite the nations

		// Log that this unite process has begun (so as to be able to tell if it aborts partway through).
		log_text = "User " + userData.name + " (" + _userID + ") initiated unite of " + userNationData.name + " (" + userNationData.ID + ") into " + targetNationData.name + " (" + targetNationData.ID + ").\n";
		Constants.WriteToNationLog(userNationData, userData, log_text);
		Constants.WriteToNationLog(targetNationData, null, log_text);

		// Record all online clients in the user's nation, and log them out.
		for (i = 0; i < userNationData.users.size(); i++)
		{
			// Get the current user's ID and data
			userID = userNationData.users.get(i);
			curUserData = (UserData)DataManager.GetData(Constants.DT_USER, userID, false);


			if ((curUserData != null) && (curUserData.client_thread != null))
			{
				online_users.add(userID);
				online_clients.add(curUserData.client_thread);
				Login.ExitGame(curUserData.client_thread, true);
			}
		}

		// Reciprocally remove the unite request.
		RemoveUniteRequest(_targetNationID, userNationID);

		// Determine the amount of purchased and won credits to transfer.
		int transfer_amount_purchased = (int)Math.min(payment_offer, targetNationData.game_money_purchased);
		int transfer_amount_won = (payment_offer - transfer_amount_purchased);

		// Transfer the credits from the target nation.
		targetNationData.game_money -= payment_offer;
		targetNationData.game_money_purchased -= transfer_amount_purchased;
		targetNationData.game_money_won -= transfer_amount_won;

		// Remove the cost to unite from both nations.
		int remaining_cost = Constants.UNITE_COST;

		// Subtract what we can of the unite's cost from the target nation's credits.
		int cost_taken_from_target_nation = (int)Math.min(remaining_cost, targetNationData.game_money);
		Money.SubtractCost(targetNationData, cost_taken_from_target_nation);
		remaining_cost -= cost_taken_from_target_nation;

		// Subtract the remainder of the unite's cost from the user nation's credits.
		Money.SubtractCost(userNationData, remaining_cost);
		remaining_cost = 0;

		// Transfer the credits to the user nation.
		userNationData.game_money += payment_offer;
		userNationData.game_money_purchased += transfer_amount_purchased;
		userNationData.game_money_won += transfer_amount_won;

		// Break all of the user's nation's alliances, and cancel incoming and outgoing requests.
		BreakAllAlliances(userNationData);

		// Transfer whatever amount of the user's nation's energy that can fit in the target nation.
		// Note: Now that unites are allowed more frequently, limit this to a fraction of the target nation's maximum to avoid free generation loophole.
		float energy_to_transfer = Math.min(Math.min(userNationData.energy, targetNationData.GetFinalEnergyMax() - targetNationData.energy), Constants.UNITE_TRANSFER_RESOURCE_FRACTION * targetNationData.GetFinalEnergyMax());
		userNationData.energy -= energy_to_transfer;
		targetNationData.energy += energy_to_transfer;

		// Transfer whatever amount of the user's nation's manpower that can fit in the target nation.
		// Note: Now that unites are allowed more frequently, limit this to a fraction of the target nation's maximum to avoid free generation loophole.
		float manpower_to_transfer = Math.min(Math.min(userNationData.mainland_footprint.manpower, targetNationData.GetMainlandManpowerMax() - targetNationData.mainland_footprint.manpower), Constants.UNITE_TRANSFER_RESOURCE_FRACTION * targetNationData.GetMainlandManpowerMax());
		userNationData.mainland_footprint.manpower -= manpower_to_transfer;
		targetNationData.mainland_footprint.manpower += manpower_to_transfer;

		// Transfer land

		// Get the mainland LandMap.
		LandMap land_map = DataManager.GetLandMap(Constants.MAINLAND_MAP_ID, false);

		// Get the user's nation's footprint.
		Footprint footprint = userNationData.GetFootprint(Constants.MAINLAND_MAP_ID);

		// Transfer the user's nation's land to the target nation.
		BlockExtData block_ext_data;
		for (y = footprint.y0; y <= footprint.y1; y++)
		{
			for (x = footprint.x0; x <= footprint.x1; x++)
			{
				if (land_map.GetBlockNationID(x, y) == userNationID)
				{
					block_ext_data = land_map.GetBlockExtendedData(x, y, false);

					// If the block being transferred contains an object belonging to the user's nation, change the object's ownership to the
					// target nation. No need to mark the block to be updated, because the below call to SetBlockNationID() will do that.
					if ((block_ext_data != null) && (block_ext_data.objectID != -1) && (block_ext_data.owner_nationID == userNationID)) {
						block_ext_data.owner_nationID = _targetNationID;
					}

					// Transfer ownership of the block of land from the user's nation to the target nation.
					World.SetBlockNationID(land_map, x, y, _targetNationID, false, true, -1, 0); // Broadcast these changes to viewing clients.
				}
			}
		}

		// Reset the user's nation's area related data, in case it was somehow corrupted at some point.
		footprint.area = 0;
		footprint.border_area = 0;
		footprint.perimeter = 0;
		footprint.x0 = footprint.y0 = Constants.MAX_MAP_DIM;
		footprint.x1 = footprint.y1 = -1;

		// If the user's nation has a homeland, remove the nation from its homeland and place a single square.
		if (userNationData.homeland_mapID > 0)
		{
			// Get the nation's homeland map.
			LandMap homeland_map = Homeland.GetHomelandMap(userNationData.ID);

			// Remove the nation from its homeland.
			World.RemoveNationFromMap(homeland_map, userNationData);

			// Place a single square of the nation within its homeland.
			Homeland.PlaceNation(userNationData.ID, homeland_map);
		}

		Output.PrintToScreen("Before unite, user nation " + userNationData.name + " has " + userNationData.xp + " XP. Target nation " + targetNationData.name + " has " + targetNationData.xp + " xp.");

		// Add all of the user's nation's XP and pending XP to the target nation.
		float xp_to_transfer = userNationData.xp;
		int pending_xp = userNationData.pending_xp;

		if (xp_to_transfer > Constants.UNITE_PENDING_XP_PER_HOUR)
		{
			pending_xp += (int)xp_to_transfer - Constants.UNITE_PENDING_XP_PER_HOUR;
			xp_to_transfer = Constants.UNITE_PENDING_XP_PER_HOUR;
		}

		//Output.PrintToScreen("xp_to_transfer: " + xp_to_transfer + ", pending_xp: " + pending_xp + ", after AddXP target nation has " + targetNationData.xp + " xp and " + targetNationData.pending_xp + " pending_xp.");

		// Add to the target nation the XP to be transferred immediately.
		Gameplay.AddXP(targetNationData, xp_to_transfer, -1, -1, -1, false, false, 0, Constants.XP_UNITE_TRANSFER);

		// Log suspect
		if (targetNationData.log_suspect_expire_time > Constants.GetTime())
		{
			// Log the details of this XP gain.
			Constants.WriteToLog("log_suspect.txt", Constants.GetTimestampString() + ": '" + userData.name + "'(" + userData.ID + ") of '" + userNationData.name + "'(ID:" + userNationData.ID + ", Level:" + userNationData.level + ") accepted unite into '" + targetNationData.name + "'(ID:" + targetNationData.ID + ", Level:" + targetNationData.level + "). XP transferred: " + xp_to_transfer + ". XP pending before this unite: " + targetNationData.pending_xp + ". XP pending from this unite: " + pending_xp + ".\n");
		}

		// Log the transfer of XP from this unite, in the target nation's log. Do this early in the unite so we can tell if the rest of the unite didn't go through.
		Constants.WriteToNationLog(targetNationData, null, Constants.GetTimestampString() + ": '" + userData.name + "'(" + userData.ID + ") of '" + userNationData.name + "'(ID:" + userNationData.ID + ", Level:" + userNationData.level + ") accepted unite into '" + targetNationData.name + "'(ID:" + targetNationData.ID + ", Level:" + targetNationData.level + "). XP transferred: " + xp_to_transfer + ". XP pending before this unite: " + targetNationData.pending_xp + ". XP pending from this unite: " + pending_xp + ".\n");

		// Add to the target nation's pending_xp what xp will be pending to add later.
		targetNationData.pending_xp += pending_xp;

		// Set the target nation's rebirth countdown to the determined combination of the two nations'.
		targetNationData.rebirth_countdown = result_countdown;
		targetNationData.rebirth_countdown_start = result_countdown_start;
		targetNationData.rebirth_countdown_purchased = result_countdown_purchased;

		// Reset the user's nations XP, pending XP, rebirth countdown, level, and advance points.
		userNationData.level = 1 + userNationData.GetRebirthLevelBonus();
		userNationData.xp = 0;
		userNationData.pending_xp = 0;
		userNationData.rebirth_countdown = Constants.REBIRTH_COUNTDOWN_START;
		userNationData.rebirth_countdown_start = Constants.REBIRTH_COUNTDOWN_START;
		userNationData.rebirth_countdown_purchased = 0;

		// Reset the user's nation's advances. It's number of Advance Points will be set to its level - 1.
		Gameplay.ResetAdvances(userNationID, true);

		if ((Constants.GetTime() - userNationData.creation_time) >= Constants.VETERAN_NATION_AGE)
		{
			// Mark the user's nation as being veteran (so shedding XP via unite cannot be used to stay in newbie land indefinitely).
			userNationData.veteran = true;
		}

		// Copy all of the user's nation's map flags to the target nation (that are not at duplicate locations).
		for (i = 0; i < userNationData.map_flags_token.size(); i++)
		{
			token = userNationData.map_flags_token.get(i);

			// If there is not already  map flag in the target nation, for this token...
			if (targetNationData.map_flags_token.contains(token) == false)
			{
				// Store this map flag in the target nation's list of map flags.
				targetNationData.map_flags_token.add(token);
				targetNationData.map_flags_title.add(userNationData.map_flags_title.get(i));

				// Broadcast message to the target nation's users, setting the map flag.
				Constants.UntokenizeCoordinates(token, coord_array);
				OutputEvents.BroadcastSetMapFlagEvent(_targetNationID, coord_array[0], coord_array[1], userNationData.map_flags_title.get(i));
			}
		}

		// Remove each player from the joining nation, add them to the target nation, and make appropriate changes. Stop if the target nation's max number of members is reached.
		UserData cur_user_data;
		while ((userNationData.users.size() > 0) && (targetNationData.users.size() < Constants.max_nation_members))
		{
			// Get the current user's data
			cur_user_data = (UserData)DataManager.GetData(Constants.DT_USER, userNationData.users.get(0), false);

			// Remove this user from joining nation
			userNationData.users.remove(0);

			// Clear fealty to the nation they are leaving from all of this user's devices.
			cur_user_data.ClearFealtyToNation(userNationID);

		  // Set the current user's nationID to that of the target nation
		  cur_user_data.nationID = _targetNationID;

			// Limit the joining user's rank to no greater than commander.
			cur_user_data.rank = Math.max(cur_user_data.rank, Constants.RANK_COMMANDER);

			// Mark the current user's data to be updated
			DataManager.MarkForUpdate(cur_user_data);

			// Add the current user to the target nation's list of users
			targetNationData.users.add(cur_user_data.ID);

			// Initialize the current user's login report data, for their new nation.
			Login.InitLoginReportData(cur_user_data, targetNationData);
		}

		// Now that a (potentially lower level) nation has united in, update the target nation as if it
		// has changed level (it may not have) to remove any squares in a lower level area.
		Gameplay.UpdateNationForLevelChange(targetNationData, 0);

		// Send updates to the target nation's logged in clients.
		for (i = 0; i < targetNationData.users.size(); i++)
		{
			// Get the current user's ID and data
			userID = targetNationData.users.get(i);
			curUserData = (UserData)DataManager.GetData(Constants.DT_USER, userID, false);

			if ((curUserData != null) && (curUserData.client_thread != null))
			{
				// Acquire and clear the output buffer
				WOCServer.temp_output_buffer_semaphore.acquire();
				WOCServer.temp_output_buffer.setLength(0);

				try
				{
					// Create necessary update events for this client.
					OutputEvents.GetMembersEvent(WOCServer.temp_output_buffer, _targetNationID);
					OutputEvents.GetMapFlagsEvent(WOCServer.temp_output_buffer, _targetNationID);
					OutputEvents.GetMessageEvent(WOCServer.temp_output_buffer, ClientString.Get("svr_unite", "nation1", userNationData.name, "nation2", targetNationData.name));

					// Terminate event string and send to client.
					curUserData.client_thread.TerminateAndSendNow(WOCServer.temp_output_buffer);
				}
				catch (Exception e)
				{
					// Log error
					Output.PrintToScreen("ERROR: Exception when attempting to send updates about unite to members of target nation " + targetNationData.name + ".");
					Output.PrintException(e);
				}

				// Release the output buffer
				WOCServer.temp_output_buffer_semaphore.release();
			}
		}

		// Log back in the user's nation's online clients that were logged out.
		for (i = 0; i < online_users.size(); i++)
		{
			// Acquire and clear the output buffer
			WOCServer.temp_output_buffer_semaphore.acquire();
			WOCServer.temp_output_buffer.setLength(0);

			try
			{
				// Log in this client, to their new nation.
				Login.EnterGame(WOCServer.temp_output_buffer, online_clients.get(i), online_users.get(i), false);
				OutputEvents.GetMessageEvent(WOCServer.temp_output_buffer, ClientString.Get("svr_unite", "nation1", userNationData.name, "nation2", targetNationData.name));

				// Terminate event string and send to client.
				online_clients.get(i).TerminateAndSendNow(WOCServer.temp_output_buffer);
			}
			catch (Exception e)
			{
				// Log error
				Output.PrintToScreen("ERROR: Exception when attempting to log user " + online_users.get(i) + " back into united nation " + targetNationData.name + ".");
				Output.PrintException(e);
			}

			// Release the output buffer
			WOCServer.temp_output_buffer_semaphore.release();
		}

		// Clear the lists of users and clients to log back in.
		online_users.clear();
		online_clients.clear();

		// Update the prev_unite_time of both nations.
		userNationData.prev_unite_time = Constants.GetTime();
		targetNationData.prev_unite_time = Constants.GetTime();

		// Mark both nations' data to be updated
		DataManager.MarkForUpdate(userNationData);
		DataManager.MarkForUpdate(targetNationData);

		// Post reports to both nations
		Comm.SendReport(_targetNationID, ClientString.Get("svr_report_other_nation_accepted_unite", "accepting_nation_name", userNationData.name), 0);
		Comm.SendReport(userNationID, ClientString.Get("svr_report_our_nation_accepted_unite", "username", userData.name, "requesting_nation_name", targetNationData.name), 0);

		// If the target nation has pending XP, send a report letting them know how long a period they will receive the XP over.
		if (pending_xp > 0) {
			Comm.SendReport(_targetNationID, ClientString.Get("svr_report_unite_pending_xp", "nation_name", userNationData.name, "hours", String.valueOf(pending_xp / Constants.UNITE_PENDING_XP_PER_HOUR + 1)), 0);
		}

		//Output.PrintToScreen("At end of unite, xp_to_transfer: " + xp_to_transfer + ", pending_xp: " + pending_xp + ", after AddXP target nation has " + targetNationData.xp + " xp and " + targetNationData.pending_xp + " pending_xp.");

		// Log this unite
		log_text = "User " + userData.name + " (" + _userID + ") united " + userNationData.name + " (" + userNationData.ID + ") into " + targetNationData.name + " (" + targetNationData.ID + "). XP transferred: " + xp_to_transfer + ", pending_xp: " + pending_xp + ", total pending_xp: " + targetNationData.pending_xp + /*", credits transferred: " + transfer_credits + ", purchased credits transferred: " + purchased_credits_transferring +*/ ", energy transferred: " + energy_to_transfer + ", manpower transferred: " + manpower_to_transfer + "\n";
		Constants.WriteToLog("log_unite.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " " + log_text);
		Constants.WriteToNationLog(userNationData, userData, log_text);
		Constants.WriteToNationLog(targetNationData, null, log_text);
		Output.PrintToScreen(log_text);
	}

	public static void DeclineUnite(StringBuffer _output_buffer, int _userID, int _targetNationID)
	{
		// Get the user's data and nation ID
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);
		int userNationID = userData.nationID;

		// Do not allow if the user is not at least a cosovereign.
		if (userData.rank > Constants.RANK_COSOVEREIGN) {
			return;
		}

		// First remove the request
		RemoveUniteRequest(_targetNationID, userNationID);

		// Get the data for both nations
		NationData userNationData = (NationData)DataManager.GetData(Constants.DT_NATION, userNationID, false);
		NationData targetNationData = (NationData)DataManager.GetData(Constants.DT_NATION, _targetNationID, false);

		if ((userNationData == null) || (targetNationData == null)) {
			return;
		}

		// Broadcast updated alliance lists to both nations.
		OutputEvents.BroadcastAlliancesEvent(userNationID);
		OutputEvents.BroadcastAlliancesEvent(_targetNationID);

		// Mark both nations' data to be updated
		DataManager.MarkForUpdate(userNationData);
		DataManager.MarkForUpdate(targetNationData);

		if (_output_buffer != null) {
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("svr_unite_invitation_declined", "nation1", targetNationData.name, "nation2", userNationData.name));
		}
	}
}
