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
import java.util.*;
import java.lang.*;
import java.text.NumberFormat;
import WOCServer.*;

public class Money
{
	public enum Source
	{
		FREE,
		WON,
		PURCHASED
	}

	static ArrayList<CashOutRecord> cash_out_records = new ArrayList<CashOutRecord>();

	public static void DetermineOrbPaymentRates()
	{
		// Get the mainland LandMap.
		LandMap land_map = DataManager.GetLandMap(Constants.MAINLAND_MAP_ID, false);

		// If there is no mainland map, return.
		if (land_map == null) {
			return;
		}

		// Get the mainland map's object_counts map.
		HashMap<Integer,Integer> object_counts = land_map.GetObjectCountMap();

		// Determine total weight for all orbs: the sum for each orb type of the number of orbs of that type multiplied by the type's payment weight.
		int objectID, objectCount, orbTypeWeight, totalWeight = 0;
		ObjectData objectData;
		for (Map.Entry<Integer,Integer> entry : object_counts.entrySet())
		{
			objectID = entry.getKey();
			objectCount = entry.getValue();

			// Skip any objects that aren't orbs.
			if (objectID < ObjectData.ORB_BASE_ID) {
				continue;
			}

			// Get this orb type's object data.
			objectData = ObjectData.GetObjectData(objectID);

			// Make sure object data was found
      if(objectData == null) {
				Output.PrintToScreen("DetermineOrbPaymentRates(): ObjectData is null for orb type with ID " + objectID + ". Cannot continue!");
				return;
      }

			// If this object is not an orb, skip it.
			if (objectData.type != ObjectData.TYPE_ORB) {
				continue;
			}

			// Sum total weight of all orb payouts.
			totalWeight += (objectData.payout_weight * objectCount);

			// TESTING
		  //Output.PrintToScreen("objectID: " + objectID + ", objectCount: " + objectCount + ", orb type payout_weight: " + objectData.payout_weight + ", totalWeight: " + totalWeight);
		}

		// Determine the actual amount of money that an orb of each type should pay out per hour.
		// It is that orb type's weight's proportion of the prize_dollars_awarded_per_day, divided by 24.
		float orb_payment_per_hour;
		for (Map.Entry<Integer,Integer> entry : object_counts.entrySet())
		{
			objectID = entry.getKey();
			objectCount = entry.getValue();

			// Skip any objects that aren't orbs.
			if (objectID < ObjectData.ORB_BASE_ID) {
				continue;
			}

			// Get this orb type's object data.
			objectData = ObjectData.GetObjectData(objectID);

			// Make sure object data was found
      if(objectData == null) {
				Output.PrintToScreen("DetermineOrbPaymentRates(): ObjectData is null for orb type with ID " + objectID + ". Cannot continue!");
				return;
      }

			// If this object is not an orb, skip it.
			if (objectData.type != ObjectData.TYPE_ORB) {
				continue;
			}

			// Determine and record how much an orb of this type should pay per hour.
			orb_payment_per_hour = (float)objectData.payout_weight / (float)totalWeight * Constants.prize_dollars_awarded_per_day / 24.0f;
			Constants.orb_payments_per_hour.put(objectID, orb_payment_per_hour);

			// TESTING
		  //Output.PrintToScreen("objectID: " + objectID + ", orb_payment_per_hour: " + orb_payment_per_hour);
		}
	}

	public static void SendPrizePayments()
	{
		BufferedReader input = null;
		InputStream stream;
		String buffer;

		int KEY_SALT = 00000001; // This is the "password" for the script that sends out prize money. To send out prize money, you would need to host your own script and have it withdraw money from your own account.

		// Get the prev_prize_payment_count
		int prev_prize_payment_count = GlobalData.instance.prev_prize_payment_count;

		boolean payments_queued = (cash_out_records.size() > 0);

		// For each CashOutRecord in the cash_out_records list...
		while (cash_out_records.size() > 0)
		{
			// Get the first CashOutRecord from the list.
			CashOutRecord cur_cash_out_record = cash_out_records.get(0);

			// Remove this first CashOutRecord from the list.
			cash_out_records.remove(0);

			// Increment the prev_prize_payment_count and use it as the ID for the current prize payment.
			int payment_id = ++prev_prize_payment_count;

			// Create unique payment ID by combining server ID with server-specific payment ID, and generate key by combining with salt.
	    int unique_payment_id = (Constants.server_id * 1000000) + payment_id;
	    int key = unique_payment_id ^ KEY_SALT;

			try
			{
				String url_string = "https://warofconquest.com/woc2/send_prize.php?server_id=" + Constants.server_id +
					"&player_id=" + cur_cash_out_record.playerID +
					"&payment_id=" + payment_id +
					"&nation_name=" + URLEncoder.encode(cur_cash_out_record.nationName, "UTF-8") +
					"&amount=" + cur_cash_out_record.amount +
					"&email=" + cur_cash_out_record.email +
					"&key=" + key;

				//Output.PrintToScreen("SendPrizePayments() URL: " + url_string);

				// Attempt to send the prize payment.
				// Open output of send_prize.php using a BufferedReader
				URL url = new URL(url_string);
				stream = url.openStream();
				input = new BufferedReader(new InputStreamReader(stream));

				// Iterate through each line of the output
				while ((buffer = input.readLine()) != null)
				{
					if (buffer.indexOf("error") != -1)
					{
						Output.PrintToScreen("*** SendPrizePayments(): send_prize.php returned error: " + buffer);
						Admin.Emergency("SendPrizePayments() send_prize.php returned error: '" + buffer + "'");
						break;
					}
				}

				// Close the BufferedReader and InputStream
				input.close();
				stream.close();
			}
			catch (IOException e)
			{
				Output.PrintToScreen("*** SendPrizePayments(): failed to read data from send_prize.php " + Constants.GetFullDate());
				Output.PrintException(e);
			}
		}

		// If payments have been made, mark the GlobalData to be updated due to the updated prev_prize_payment_count.
		if (payments_queued)
		{
			GlobalData.instance.prev_prize_payment_count = prev_prize_payment_count;
			DataManager.MarkForUpdate(GlobalData.instance);
		}
	}

	public static void CheckForPayments()
	{
		int prev_payment_count, cur_payment_count, user_id, pkg;
		float amount;
		BufferedReader input = null;
		InputStream stream;
		String buffer, currency;

		// Get the prev_payment_count
		prev_payment_count = cur_payment_count = GlobalData.instance.prev_payment_count;

		// Attempt to process the most recent payments.
		try
		{
			// Open output of get_payments.php using a BufferedReader
			String url_string = "https://warofconquest.com/payment/get_payments.php?server_id=" + Constants.GetServerID() + "&prev_payment_count=" + prev_payment_count;
			URL url = new URL(url_string);
			stream = url.openStream();
			input = new BufferedReader(new InputStreamReader(stream));

			// Iterate through each line of the output
			while ((buffer = input.readLine()) != null)
			{
				if (buffer.indexOf("error") != -1)
				{
					Output.PrintToScreen("*** CheckForPayments(): get_payments.php returned error (" + buffer + ") " + Constants.GetFullDate());
					break;
				}

				// Initialize user_id to 0 so we'll know if this is a valid line of data
				user_id = 0;

				// Get the current payment's data out of the current line
				cur_payment_count = Constants.FetchParameterInt(buffer, "payment_count");
				user_id = Constants.FetchParameterInt(buffer, "user_id");
				pkg = Constants.FetchParameterInt(buffer, "package");
				amount = Constants.FetchParameterFloat(buffer, "amount");
				currency = Constants.FetchParameter(buffer, "currency", false);

				if (user_id == 0)
				{
					Output.PrintToScreen("*** CheckForPayments(): get_payments.php returned invalid line: '" + buffer + "', " + Constants.GetFullDate());
					break;
				}
				else
				{
					Output.PrintToScreen("Payment received: user_id: " + user_id + ", package: " + pkg + ", amount: " + amount + ", currency: " + currency);

					// Purchase credits
					Money.PurchaseCredits(user_id, pkg, amount, currency);
				}
			}

			// If new payments have been received, store the lastest payment count as the prev_payment_count.
			if (cur_payment_count > prev_payment_count)
			{
				prev_payment_count = cur_payment_count;
				GlobalData.instance.prev_payment_count = prev_payment_count;
				DataManager.MarkForUpdate(GlobalData.instance);
			}

			// Close the BufferedReader and InputStream
			input.close();
			stream.close();
		}
		catch (IOException ioe)
		{
			Output.PrintToScreen("*** CheckForPayments(): failed to read data from get_payments.php " + Constants.GetFullDate());
		}
	}

	public static void CheckSubscriptions()
	{
		int prev_subscription_modification_time, latest_subscription_modification_time, cur_modification_time, user_id, pkg, paid_through_time;
		BufferedReader input = null;
		InputStream stream;
		String buffer, status, subscription_id, gateway;

		// Get the prev_subscription_modification_time
		prev_subscription_modification_time = latest_subscription_modification_time = GlobalData.instance.prev_subscription_modification_time;

		// Attempt to access the most recent subscription changes.
		try
		{
			// Open output of get_subscriptions.php using a BufferedReader
			String url_string = "https://warofconquest.com/payment/get_subscriptions.php?server_id=" + Constants.GetServerID() + "&min_modification_time=" + (prev_subscription_modification_time + 1);
			URL url = new URL(url_string);
			stream = url.openStream();
			input = new BufferedReader(new InputStreamReader(stream));

			// Iterate through each line of the output
			while ((buffer = input.readLine()) != null)
			{
				if (buffer.indexOf("error") != -1)
				{
					Output.PrintToScreen("*** CheckForPayments(): get_subscriptions.php returned error (" + buffer + ") " + Constants.GetFullDate());
					break;
				}

				// Initialize user_id to 0 so we'll know if this is a valid line of data
				user_id = 0;

				// Example: ?modification_time=1590008639|user_id=1|package=1|paid_through_date=2020-06-18 00:00:00|status=Active

				// Get the current payment's data out of the current line
				cur_modification_time = Constants.FetchParameterInt(buffer, "modification_time");
				user_id = Constants.FetchParameterInt(buffer, "user_id");
				gateway = Constants.FetchParameter(buffer, "gateway", false);
				subscription_id = Constants.FetchParameter(buffer, "subscription_id", false);
				pkg = Constants.FetchParameterInt(buffer, "package");
				paid_through_time = Constants.FetchParameterInt(buffer, "paid_through_time");
				status = Constants.FetchParameter(buffer, "status", false);

				// Record most recent subscription modification time being processed.
				latest_subscription_modification_time = Math.max(latest_subscription_modification_time, cur_modification_time);

				if (user_id == 0)
				{
					Output.PrintToScreen("*** CheckSubscriptions(): get_subscriptions.php returned invalid line: '" + buffer + "', " + Constants.GetFullDate());
					break;
				}
				else
				{
					// Get the user's data
					UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, user_id, false);

					Output.PrintToScreen("Subscription info received: user_id: " + user_id + ", subscription_id: " + subscription_id + ", gateway: " + gateway + ", package: " + pkg + ", paid_through_time: " + paid_through_time + ", status: " + status + ", modification_time: " + cur_modification_time + ", prev_subscription_modification_time: " + prev_subscription_modification_time + ", user's current subscription_id: " + userData.subscription_id);

					if (userData != null)
					{
						// Only update the user's subscription if the user doesn't already have a subscription, or if the update is to the user's existing
						// subscription, or if the user's existing subscription is canceled and the update is to a new actove subscription.
						if ((userData.subscription_id.equals("")) || (userData.subscription_id.equals(subscription_id)) || ((userData.subscription_status.equals("Canceled")) && (status.equals("Active"))))
						{
							Output.PrintToScreen("Subscription update being applied.");

							// Update the user's subscription information
							userData.subscription_id = subscription_id;
							userData.subscription_gateway = gateway;
							userData.subscription_status = status;
							userData.paid_through_time = paid_through_time;

							if ((userData.subscribed == false) && (userData.paid_through_time > Constants.GetTime()))
							{
								// Activate subscription.
								Subscription.ActivateSubscription(userData, pkg);
							}
							else if ((userData.subscribed == true) && (userData.paid_through_time <= Constants.GetTime()))
							{
								// Deactivate subscription.
								Subscription.DeactivateSubscription(userData);
							}
							else if ((userData.subscribed == true) && (userData.paid_through_time > Constants.GetTime()) && (userData.subscription_package != pkg))
							{
								// Subscription package has changed; deactivate old subscription and activate new subscription.
								Subscription.DeactivateSubscription(userData);
								Subscription.ActivateSubscription(userData, pkg);
							}
							else
							{
								// Subscription has changed in a way that doesn't cause it to activate or deactivate (which would update the user.)
								// Send a subscription event to the user.
								OutputEvents.SendSubscriptionEvent(userData);
							}
						}
					}
				}
			}

			// If subscription modifications have been received, store the lastest modification time as the prev_subscription_modification_time.
			if (latest_subscription_modification_time > prev_subscription_modification_time)
			{
				GlobalData.instance.prev_subscription_modification_time = latest_subscription_modification_time;
				DataManager.MarkForUpdate(GlobalData.instance);
			}

			// Close the BufferedReader and InputStream
			input.close();
			stream.close();
		}
		catch (IOException ioe)
		{
			Output.PrintToScreen("*** CheckSubscriptions(): failed to read data from get_subscriptions.php " + Constants.GetFullDate());
		}
	}

	public static void AttemptDeposit(StringBuffer _output_buffer, int _userID, float _deposit_amount)
	{
		// This free method of purchasing credits is no longer available
		/*
		// Get the user's nation ID
		int userNationID = DataManager.GetIntRecord(Constants.DT_USER, _userID, "nationID");

		// Get the user's nation's data
		NationData userNationData = (NationData)DataManager.GetData(Constants.DT_NATION, userNationID, false);

		if (_deposit_amount < 1.0)
		{
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("Minimum purchase amount is $1"));
			return ;
		}

		if (_deposit_amount > 50.0)
		{
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("Maximum of $50 worth of credits can be purchased at one time."));
			return;
		}

		// FOR TESTING PHASE ONLY:
		// Limit of amount of money that can be deposited per day.
		float MAX_AMOUNT = 5.0f;
		int cur_day = (int)(Constants.GetTime() / 86400);
		if (cur_day != userNationData.cur_deposit_money_day) {
			userNationData.cur_deposit_money_day = cur_day;
			userNationData.cur_deposit_money_amount = 0;
		}
		if (userNationData.cur_deposit_money_amount + _deposit_amount > MAX_AMOUNT) {
			float amount_remaining = ((int)((MAX_AMOUNT - userNationData.cur_deposit_money_amount) * 100)) / 100f;
			OutputEvents.GetMessageEvent(_output_buffer, ClientString.Get("During beta, you may purchase for free up to $" + Constants.ConvertToMoneyString(MAX_AMOUNT) + " of credits per day. You may still purchase $" + Constants.ConvertToMoneyString(amount_remaining) + " today."));
			return;
		}
		userNationData.SetStringKeyToFloatAdd("cur_deposit_money_amount", _deposit_amount);

		// Deposit the "money", purchasing credits with it.
		Deposit(_userID, _deposit_amount, 0);
		*/
	}

	public static void PurchaseCredits(int _userID, int _pkg, float _amount, String _currency)
	{
		if ((_pkg < -1) || (_pkg >= Constants.NUM_CREDIT_PACKAGES)) {
			Output.PrintToScreen("ERROR: Attempt to purchase invalid credit package " + _pkg + " for $" + _amount + " by user " + _userID);
			return;
		}

		if ((Constants.allow_credit_purchases == false) && (_pkg != -1)) {
			Output.PrintToScreen("ERROR: Attempt to purchase credit package " + _pkg + " for $" + _amount + " by user " + _userID + ", through credit purchasing is not allowed.");
			return;
		}

		// Get the user's data
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

    // Make sure user's data was found
		if (userData == null) {
			Output.PrintToScreen("ERROR: Attempt to purchase credits (package " + _pkg + " for $" + _amount + ") by user " + _userID + ", data not found!");
			return;
		}

		// Get the user's nation's data
		int userNationID = userData.nationID;
		NationData userNationData = (NationData)DataManager.GetData(Constants.DT_NATION, userNationID, false);

		// Make sure nation's data was found
		if (userNationData == null) {
			Output.PrintToScreen("ERROR: Attempt to purchase credits (package " + _pkg + " for $" + _amount + ") for nation " + userNationID + ", data not found!");
			return;
		}

		// Determine number of credits to buy
		int num_credits_bought = (_pkg == -1) ? (int)(_amount * Constants.CREDITS_EARNED_PER_DOLLAR) : Constants.BUY_CREDITS_AMOUNT[_pkg];

		if (Constants.max_buy_credits_per_month != -1)
		{
			if (num_credits_bought > userNationData.GetNumCreditsAllowedToBuyThisMonth())
			{
				Output.PrintToScreen("ERROR: Attempt to purchase credits (package " + _pkg + " for $" + _amount + ") for nation " + userNationID + ", only allowed to buy " + userNationData.GetNumCreditsAllowedToBuyThisMonth() + " more this month!");
				return;
			}
		}

		// Add the determined number of purchased credits to the nation's game_money
		Money.AddGameMoney(userNationData, num_credits_bought, Money.Source.PURCHASED);

		// Reset the nation's information about credits bought and received this month, if necessary.
		if (userNationData.prev_buy_credits_month != Constants.GetFullMonth())
		{
			userNationData.prev_buy_credits_month = Constants.GetFullMonth();
			userNationData.prev_buy_credits_month_amount = 0;
			userNationData.prev_receive_credits_month_amount = 0;
		}

		// Record that the target nation has bought this amount of credits during the current month.
		userNationData.prev_buy_credits_month_amount += num_credits_bought;


		// Add the _amount to the nation's money_spent
		userNationData.money_spent += _amount;

		// Determine how much rebirth countdown is being purchased.
		float countdown_bonus = num_credits_bought / Constants.REBIRTH_COUNTDOWN_INCREMENT_PURCHASE_AMOUNT;

		// Limit the amount of rebirth countdown being purchased to MAX_REBIRTH_COUNTDOWN_PURCHASED per cycle.
		countdown_bonus = Math.min(countdown_bonus, Math.max(0, Constants.MAX_REBIRTH_COUNTDOWN_PURCHASED - userNationData.rebirth_countdown_purchased));

		// Add to the nation's rebirth countdown for the amount of credits purchased.
		Gameplay.ChangeRebirthCountdown(userNationData, countdown_bonus);

		// Keep track of the amount of rebirth countdown this nation has purchased.
		userNationData.rebirth_countdown_purchased += countdown_bonus;

		// Add the deposit amount to the GlobalData's money_revenue
		GlobalData.instance.money_revenue += _amount;

		// Log this purchase of game credits
		Constants.WriteToLog("log_purchase.txt", Constants.GetFullDate() + " Nation " + userNationData.name + " (" + userNationID + ") user " + userData.name + " (" + _userID + ") purchased " + num_credits_bought + " credits for $" + _amount + ". Countdown bonus: " + countdown_bonus + " (cycle total: " + userNationData.rebirth_countdown_purchased + ")\n");

    /* // DISABLED
		// Contribute a share of the credits this user has purchased, to this user's patron and/or followers.

		// If the user has a patron...
		if (userData.patronID != -1)
		{
			// Get the data for the user's patron
			UserData patronUserData = (UserData)DataManager.GetData(Constants.DT_USER, userData.patronID, false);

			if (patronUserData != null)
			{
				float patron_credits_contribution = num_credits_bought * Constants.PATRON_CREDITS_SHARE;

				// Get the patron's nation data
				NationData patronNationData = (NationData)DataManager.GetData(Constants.DT_NATION, patronUserData.nationID, false);

				// Add the determined fraction of purchased credits to the patron's nation's game_money
				Money.AddGameMoney(patronNationData, patron_credits_contribution, Money.Source.FREE);

				// Add this credit contribution to the patron's login report.
				patronUserData.ModifyReportValueFloat(UserData.ReportVal.report__follower_credits, (int)(patron_credits_contribution + 0.5f));

				// Broadcast an update bars event to the patron's nation, letting all players know about the change in credits.
				OutputEvents.BroadcastUpdateBarsEvent(patronUserData.nationID, 0, 0, 0, 0, (int)(patron_credits_contribution + 0.5f), 0);

				// Add the record of this credit contribution to the patron's record for this follower.
				for (FollowerData follower : patronUserData.followers)
				{
					if (follower.userID == userData.ID)
					{
						follower.bonusCredits += patron_credits_contribution;
						break;
					}
				}

				// Send message to the receiving user, if they're online.
				OutputEvents.SendMessageEvent(userData.patronID, ClientString.Get("svr_patron_credits_to_patron", "nation", patronNationData.name, "credits", String.format("%,d", (int)(patron_credits_contribution + 0.5f)), "username", userData.name));

				// Update the patron's user and nation data.
				DataManager.MarkForUpdate(patronUserData);
				DataManager.MarkForUpdate(patronNationData);
			}
		}

		// Determine the share of credits that will be awarded to followers.
		float follower_credits_contribution = num_credits_bought * Constants.FOLLOWER_CREDITS_SHARE;

		// Add the follower_credits_contribution to this user's record of patron contributions for the current month.
		userData.cur_month_patron_bonus_credits += (int)follower_credits_contribution;

		// Determine the share of credits that will be awarded to each follower. This is the follower share of the purchased amount, divided by the number of followers.
		float per_follower_credits_contribution = follower_credits_contribution / (float)(userData.followers.size());

		for (FollowerData follower : userData.followers)
		{
			// Get the user data for the user's current follower.
			UserData followerUserData = (UserData)DataManager.GetData(Constants.DT_USER, follower.userID, false);

			// Get the follower's nation's data.
			NationData followerNationData = (NationData)DataManager.GetData(Constants.DT_NATION, followerUserData.nationID, false);

			// Add the determined fraction of purchased credits to the follower's nation's game_money
			Money.AddGameMoney(followerNationData, per_follower_credits_contribution, Money.Source.FREE);

			// Add this credit contribution to the follower's login report.
			followerUserData.ModifyReportValueFloat(UserData.ReportVal.report__patron_credits, (int)(per_follower_credits_contribution + 0.5f));

			// Broadcast an update bars event to the follower's nation, letting all players know about the change in credits.
			OutputEvents.BroadcastUpdateBarsEvent(followerUserData.nationID, 0, 0, 0, 0, (int)(per_follower_credits_contribution + 0.5f), 0);

			// Add the per_follower_credits_contribution to the record of the amount this follower has received from their current patron.
			followerUserData.total_patron_credits_received += per_follower_credits_contribution;

			// Send message to the receiving user, if they're online.
			OutputEvents.SendMessageEvent(follower.userID, ClientString.Get("svr_patron_credits_to_follower", "nation", followerNationData.name, "credits", String.format("%,d", (int)(per_follower_credits_contribution + 0.5f)), "username", userData.name));

			// Update the follower's user and nation data.
			DataManager.MarkForUpdate(followerUserData);
			DataManager.MarkForUpdate(followerNationData);
		}
		*/

		// Mark the nation and global data to be updated
		DataManager.MarkForUpdate(userNationData);
		DataManager.MarkForUpdate(GlobalData.instance);

		// Send a purchase complete event to the user who made this purchase.
		OutputEvents.SendPurchaseCompleteEvent(_userID, _pkg, _amount, _currency);

		// Broadcast an update bars event to the nation, letting all players know about the change in credits.
		OutputEvents.BroadcastUpdateBarsEvent(userNationID, 0, 0, 0, 0, num_credits_bought, 0);

		// Send report of this purchase to nation
		Comm.SendReport(userNationID, ClientString.Get("svr_report_purchased_credits", "username", userData.name, "num_credits", String.valueOf(num_credits_bought)), 0); // userData.name + " has earned " + num_credits_bought + " credits."

		// If this user has an available ad bonus, grant it (as if they had watched an ad).
		if (userData.ad_bonuses_allowed && (userData.ad_bonus_available > 0))
		{
			// Add the user's ad_bonus_available number of credits to the nation, as having been purchased.
			Money.AddGameMoney(userNationData, userData.ad_bonus_available, Money.Source.PURCHASED);

			// Broadcast the change in this nation's number of credits.
			OutputEvents.BroadcastUpdateBarsEvent(userNationData.ID, 0, 0, 0, 0, userData.ad_bonus_available, 1); // 1 second delay

			// Send message to user that ad bonus has been collected.
			OutputEvents.SendCollectAdBonusEvent(_userID, 1); // 1 second delay

			// Reset the user's ad_bonus_available to 0.
			userData.ad_bonus_available = 0;
			DataManager.MarkForUpdate(userData);
		}
	}

	public static void TradeInWinnings(int _userID)
	{
		// Get the user data
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		if (userData == null) {
			return;
		}

		// Require rank of cosovereign or higher in order to trade in winnings. No need to return message, since this is first checked by client.
		if (userData.rank > Constants.RANK_COSOVEREIGN) {
			return;
		}

		// Get the user's nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, userData.nationID, false);

		if (nationData == null) {
			return;
		}

		// Make sure that the nation has winnings to trade in.
		if (nationData.prize_money == 0) {
			return;
		}

		int creditsToAdd = (int)Math.floor(nationData.prize_money) * Constants.CREDITS_PER_CENT_TRADED_IN;

		NumberFormat formatter = NumberFormat.getCurrencyInstance();
		String amountString = formatter.format(nationData.prize_money / 100f);

		Output.PrintToScreen("TradeInWinnings() Nation " + nationData.name + " user " + userData.name + " trading in $" + amountString + " winnings for " + creditsToAdd + " credits.");
		Constants.WriteToNationLog(nationData, userData, Constants.GetTimestampString() + " User " + userData.name + " trading in $" + amountString + " winnings for " + creditsToAdd + " credits. Prize money history: " + nationData.prize_money_history + " cents. Current prize_money: " + nationData.prize_money + " cents.\n");

		// Record report of this trade in.
		Comm.SendReport(nationData.ID, ClientString.Get("svr_report_trade_in", "USERNAME", userData.name, "AMOUNT", amountString, "NUM_CREDITS", "" + creditsToAdd), 0); // "{USERNAME} traded in {AMOUNT} of winnings for {NUM_CREDITS}<sprite=2>."

		// Add the nation's prize_money to the nation's game_money and game_money_won.
		Money.AddGameMoney(nationData, creditsToAdd, Money.Source.WON);

		// Reset the nation's prize_money to 0.
		nationData.prize_money = 0;

		// Mark the nation's data to be updated.
		DataManager.MarkForUpdate(nationData);

		// Broadcast an update bars event to the nation, letting all players know about the change in credits.
		OutputEvents.BroadcastUpdateBarsEvent(userData.nationID, 0, 0, 0, 0, creditsToAdd, 0);

		// Send update event, because prizeMoney has been reset to 0.
		OutputEvents.BroadcastUpdateEvent(userData.nationID);
	}

	public static void CashOut(StringBuffer _output_buffer, int _playerID, int _userID, int _targetUserID, float _amount)
	{
		// Get the user data
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		if (userData == null) {
			return;
		}

		// Require rank of cosovereign or higher in order to trade in winnings.
		// No need to return message, since this is first checked by client.
		if (userData.rank > Constants.RANK_COSOVEREIGN) {
			return;
		}

		// Get the user's nation's data
		NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, userData.nationID, false);

		if (nationData == null) {
			return;
		}

		// Make sure that the nation will be cashing out at least MIN_WINNINGS_TO_CASH_OUT.
		// No need to return message, since this is first checked by client.
		if (_amount < Constants.MIN_WINNINGS_TO_CASH_OUT) {
			return;
		}

		// Make sure that the nation has at least as much winnings as it wants to cash out.
		// No need to return message, since this is first checked by client.
		if (nationData.prize_money < _amount) {
			return;
		}

		// Get the target user data
		UserData targetUserData = (UserData)DataManager.GetData(Constants.DT_USER, _targetUserID, false);

		if (targetUserData == null) {
			return;
		}

		if (targetUserData.nationID != userData.nationID) {
			return;
		}

		// Subtract the given _amount from the nation's prize_money.
		nationData.prize_money -= _amount;

		// Store CashOutRecord for this cash out order.
		cash_out_records.add(new CashOutRecord(_playerID, (int)_amount, nationData.name, targetUserData.email));

		NumberFormat formatter = NumberFormat.getCurrencyInstance();
		String amountString = formatter.format(_amount / 100f);
		String remainString = formatter.format(nationData.prize_money / 100f);

		// Log this cash out.
		Output.PrintToScreen("CashOut() Nation " + nationData.name + "'s " + userData.name + " (" + userData.ID + ") cashing out " + amountString + " winnings to user " + targetUserData.name + " (" + targetUserData.ID + "). " + remainString + " winnings remain.");
		Constants.WriteToNationLog(nationData, userData, Constants.GetTimestampString() + " User " + userData.name + " cashing out " + amountString + " winnings to user " + targetUserData.name + " (" + targetUserData.ID + "). " + remainString + " winnings remain. Prize money history: " + nationData.prize_money_history + " cents. Current prize_money: " + nationData.prize_money + " cents.\n");

		// Record report of this cash out.
		Comm.SendReport(nationData.ID, ClientString.Get("svr_report_cash_out", "USERNAME", userData.name, "AMOUNT", amountString, "TARGET_USERNAME", targetUserData.name, "REMAIN", remainString), 0); // "{USERNAME} cashed out {AMOUNT} of winnings to {TARGET_USERNAME}. {REMAIN} winnings remain."

		// Mark the nation's data to be updated.
		DataManager.MarkForUpdate(nationData);

		// Return requestor message
		OutputEvents.GetRequestorEvent(_output_buffer, ClientString.Get("svr_cashed_out", "amount", amountString, "remain", remainString, "target_username", targetUserData.name)); // "We've sent {amount} of winnings to {target_username}. {remain} of winnings remain."

		// Send update event, because prizeMoney has been modified.
		OutputEvents.BroadcastUpdateEvent(userData.nationID);
	}

	public static void AddGameMoney(NationData _nation_data, float _amount, Source _source)
	{
		// Add to the nation's total.
		_nation_data.game_money = Math.max(0, _nation_data.game_money + _amount);

		// Add to the nation's game money from the given source.
		if (_source == Source.PURCHASED)
		{
			_nation_data.game_money_purchased += _amount;
			_nation_data.total_game_money_purchased += _amount;
		}
		else if (_source == Source.WON)
		{
			_nation_data.game_money_won += _amount;
		}

		// Make sure records of game money purchased and won are less than total game money.
		_nation_data.game_money_purchased = Math.min(_nation_data.game_money_purchased, _nation_data.game_money);
		_nation_data.game_money_won = Math.min(_nation_data.game_money_won, _nation_data.game_money - _nation_data.game_money_purchased);

		// Mark the nation's data to be updated.
		DataManager.MarkForUpdate(_nation_data);
	}

	public static void SubtractCost(NationData _nationData, int _cost)
	{
		if (_cost > _nationData.game_money)
		{
			Output.PrintToScreen("ERROR: Attempting to subtract " + _cost + " credits from nation " + _nationData.name + " that only has " + _nationData.game_money + " credits!");
			return;
		}

		// Determine the number of purchased credits to remove (this is proportional to purchased credist / all credits).
		int purchased_credits_cost = (int)(_nationData.game_money_purchased * _cost / _nationData.game_money);

		// Determine the number of won credits to remove (this is proportional to won credist / all credits).
		int won_credits_cost = (int)(_nationData.game_money_won * _cost / _nationData.game_money);

		// Subtract the cost from the nation.
		_nationData.game_money -= (float)_cost;
		_nationData.game_money_purchased -= (float)purchased_credits_cost;
		_nationData.game_money_won -= (float)won_credits_cost;

		// Mark the nation's data to be updated.
		DataManager.MarkForUpdate(_nationData);
	}
}

class CashOutRecord
{
	int playerID, amount;
	String nationName, email;

	public CashOutRecord(int _playerID, int _amount, String _nationName, String _email)
	{
		playerID = _playerID;
		amount = _amount;
		nationName = _nationName;
		email = _email;
	}
}
