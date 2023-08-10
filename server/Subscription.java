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

public class Subscription
{
	public static final int SUBSCRIPTION_TIER_COMMANDER = 0;
	public static final int SUBSCRIPTION_TIER_SOVEREIGN = 1;
	public static final int SUBSCRIPTION_TIER_COUNT     = 2;

	public static final int SUBSCRIPTION_BONUS_CREDITS  = 0;
	public static final int SUBSCRIPTION_BONUS_REBIRTH  = 1;
	public static final int SUBSCRIPTION_BONUS_XP       = 2;
	public static final int SUBSCRIPTION_BONUS_MANPOWER = 3;

	public static final float[] subscription_cost_usd = new float [] {4.99f, 9.99f};
	public static final int[] bonus_credits_per_day = new int [] {20, 50};
	public static final int[] bonus_rebirth_per_day = new int [] {2, 5};
	public static final int[] bonus_xp_percentage = new int [] {5, 10};
	public static final int[] bonus_manpower_percentage = new int [] {10, 20};

	public static void ActivateSubscription(UserData _userData, int _tier)
	{
		_userData.subscribed = true;

		// Get the user's nation's data
		NationData userNationData = (NationData)DataManager.GetData(Constants.DT_NATION, _userData.nationID, false);

		// The user's own nation will be the initial target for their subscription bonuses.
		_userData.bonus_credits_target = _userData.nationID;
		_userData.bonus_rebirth_target = _userData.nationID;
		_userData.bonus_xp_target = _userData.nationID;
		_userData.bonus_manpower_target = _userData.nationID;

		// Add the user's subscription to their nation's lists of subscription bonuses.
		userNationData.bonus_credits_subscriptions.add(_userData.ID);
		userNationData.bonus_rebirth_subscriptions.add(_userData.ID);
		userNationData.bonus_xp_subscriptions.add(_userData.ID);
		userNationData.bonus_manpower_subscriptions.add(_userData.ID);

		// HERE TODO: Re-determine the target nation's subscripton bonuses.

		// Send a subscription event to the user.
		OutputEvents.SendSubscriptionEvent(_userData);

		// Mark both the user and their nation to be updated.
		DataManager.MarkForUpdate(_userData);
		DataManager.MarkForUpdate(userNationData);
	}

	public static void DeactivateSubscription(UserData _userData)
	{
		_userData.subscribed = false;

		NationData targetNationData;

		// Remove the user's subscripton from their bonus_credits_target's bonus_credits_subscriptions list.
		targetNationData = (NationData)DataManager.GetData(Constants.DT_NATION, _userData.bonus_credits_target, false);
		targetNationData.bonus_credits_subscriptions.remove(new Integer(_userData.ID));
		DataManager.MarkForUpdate(targetNationData);

		// Remove the user's subscripton from their bonus_rebirth_target's bonus_rebirth_subscriptions list.
		targetNationData = (NationData)DataManager.GetData(Constants.DT_NATION, _userData.bonus_rebirth_target, false);
		targetNationData.bonus_rebirth_subscriptions.remove(new Integer(_userData.ID));
		DataManager.MarkForUpdate(targetNationData);

		// Remove the user's subscripton from their bonus_xp_target's bonus_xp_subscriptions list.
		targetNationData = (NationData)DataManager.GetData(Constants.DT_NATION, _userData.bonus_xp_target, false);
		targetNationData.bonus_xp_subscriptions.remove(new Integer(_userData.ID));
		DataManager.MarkForUpdate(targetNationData);

		// Remove the user's subscripton from their bonus_manpower_target's bonus_manpower_subscriptions list.
		targetNationData = (NationData)DataManager.GetData(Constants.DT_NATION, _userData.bonus_manpower_target, false);
		targetNationData.bonus_manpower_subscriptions.remove(new Integer(_userData.ID));
		DataManager.MarkForUpdate(targetNationData);

		// Clear the user's lists of bonus targets.
		_userData.bonus_credits_target = -1;
		_userData.bonus_rebirth_target = -1;
		_userData.bonus_xp_target = -1;
		_userData.bonus_manpower_target = -1;

		// HERE TODO: Re-determine each target nation's subscripton bonuses.

		// Send a subscription event to the user.
		OutputEvents.SendSubscriptionEvent(_userData);

		// Mark both the user data to be updated.
		DataManager.MarkForUpdate(_userData);
	}

	public static void SwitchSubscriptionBonus(StringBuffer _output_buffer, int _userID, int _bonus_type)
	{
		Output.PrintToScreen("SwitchSubscriptionBonus() SwitchSubscriptionBonus: " + _bonus_type);

		// Get the user's data
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		if (userData == null) {
			return;
		}

		// Get the user's nation's data
		NationData userNationData = (NationData)DataManager.GetData(Constants.DT_NATION, userData.nationID, false);

		// Determine the old target ID for the given subscription bonus.
		int oldTargetNationID = -1;
		switch (_bonus_type)
		{
				case SUBSCRIPTION_BONUS_CREDITS: oldTargetNationID = userData.bonus_credits_target; break;
				case SUBSCRIPTION_BONUS_REBIRTH: oldTargetNationID = userData.bonus_rebirth_target; break;
				case SUBSCRIPTION_BONUS_XP: oldTargetNationID = userData.bonus_xp_target; break;
				case SUBSCRIPTION_BONUS_MANPOWER: default: oldTargetNationID = userData.bonus_manpower_target; break;
		}

		// Get the user's old bonus target nation's data
		NationData oldTargetNationData = (NationData)DataManager.GetData(Constants.DT_NATION, oldTargetNationID, false);

		if ((userNationData == null) || (oldTargetNationData == null)) {
			return;
		}

		// Switch over the bonus from the old target to the user's current nation.
		switch (_bonus_type)
		{
			case SUBSCRIPTION_BONUS_CREDITS:
				oldTargetNationData.bonus_credits_subscriptions.remove(new Integer(_userID));
				userNationData.bonus_credits_subscriptions.add(new Integer(_userID));
				userData.bonus_credits_target = userData.nationID;
				break;
			case SUBSCRIPTION_BONUS_REBIRTH:
				oldTargetNationData.bonus_rebirth_subscriptions.remove(new Integer(_userID));
				userNationData.bonus_rebirth_subscriptions.add(new Integer(_userID));
				userData.bonus_rebirth_target = userData.nationID;
				break;
			case SUBSCRIPTION_BONUS_XP:
				oldTargetNationData.bonus_xp_subscriptions.remove(new Integer(_userID));
				userNationData.bonus_xp_subscriptions.add(new Integer(_userID));
				userData.bonus_xp_target = userData.nationID;
				break;
			case SUBSCRIPTION_BONUS_MANPOWER:
				oldTargetNationData.bonus_manpower_subscriptions.remove(new Integer(_userID));
				userNationData.bonus_manpower_subscriptions.add(new Integer(_userID));
				userData.bonus_manpower_target = userData.nationID;
				break;
		}

		// HERE TODO: Re-determine each target nation's subscripton bonuses.

		// Send a subscription event the user.
		OutputEvents.SendSubscriptionEvent(userData);

		// Return requestor message
		OutputEvents.GetRequestorEvent(_output_buffer, ClientString.Get("svr_bonus_switched", "old_nation", oldTargetNationData.name, "new_nation", userNationData.name)); // "Your subscription's bonus has been switched from {old_nation} to {new_nation}."

		// Update the user's data and both nations' data.
		DataManager.MarkForUpdate(userData);
		DataManager.MarkForUpdate(userNationData);
		DataManager.MarkForUpdate(oldTargetNationData);
	}

	public static void Unsubscribe(StringBuffer _output_buffer, int _userID)
	{
		boolean success = false;

		// Get the user's data
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, _userID, false);

		if (userData == null) {
			return;
		}

		if (userData.subscription_gateway.equals("PayPal"))
		{
			try
			{
				BufferedReader input = null;
				InputStream stream;
				String buffer;

				// Open output of bt_cancel_subscription.php using a BufferedReader
				String url_string = "https://warofconquest.com/payment/bt_cancel_subscription.php?subscription_id=" + userData.subscription_id;
				URL url = new URL(url_string);
				stream = url.openStream();
				input = new BufferedReader(new InputStreamReader(stream));

				// Iterate through each line of the output
				while ((buffer = input.readLine()) != null)
				{
					if (buffer.indexOf("success") != -1)
					{
						success = true;
						break;
					}
				}

				// Close the BufferedReader and InputStream
				input.close();
				stream.close();
			}
			catch (IOException ioe)
			{
				Output.PrintToScreen("*** Unsubscribe(): failed to read data from bt_cancel_subscription.php " + Constants.GetFullDate());
			}
		}

		if (success)
		{
			// Return requestor message
			OutputEvents.GetRequestorEvent(_output_buffer, ClientString.Get("svr_subscription_canceled")); // "The cancellation is being processed. Your subscription will stop once the period that has already been paid for is finished."
		}
	}

	public static String GetAssociatedSubscribedUsername(UserData _userData)
	{
		for (int i = 0; i < _userData.associated_users.size(); i++)
		{
			UserData assoc_user_data = (UserData)DataManager.GetData(Constants.DT_USER, _userData.associated_users.get(i), false);

			if ((assoc_user_data != null) && assoc_user_data.subscribed) {
				return assoc_user_data.name;
			}
		}

		return "";
	}
}
