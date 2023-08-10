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
import WOCServer.WOCServer;
import WOCServer.DataManager;
import WOCServer.Output;
import WOCServer.Constants;
import WOCServer.InputEvents;

public class UpdateThread extends Thread
{
	static final long MEM_INCREASE_GC_THRESHOLD = 10000000;
	static boolean create_user_report = false, create_moderator_list = false;
	static int MODS_HTML_LENGTH = 50000;
	static StringBuffer mods_html = new StringBuffer(MODS_HTML_LENGTH);
  static StringBuffer mods_level1_html = new StringBuffer(MODS_HTML_LENGTH);
  static StringBuffer mods_level2_html = new StringBuffer(MODS_HTML_LENGTH);
  static StringBuffer mods_level3_html = new StringBuffer(MODS_HTML_LENGTH);
	static int prev_sleep_time = 0;
	static ArrayList<Integer> status_count_history = new ArrayList<Integer>();
	static ArrayList<Float> status_load_history = new ArrayList<Float>();
	static ArrayList<Float> status_ram_history = new ArrayList<Float>();

	public UpdateThread()
	{
		super("UpdateThread");
	}

	public void QueueUserReport()
	{
		create_user_report = true;
	}

	public void QueueModeratorList()
	{
		create_moderator_list = false;
	}

	public void run()
	{
		int next_userID = DataManager.GetHighestDataID(Constants.DT_USER) + 1;
		int highest_userID, prev_payment_count, cur_payment_count, user_id;
		float mc_gross, mc_fee;
		UserData userData;
		BufferedReader input = null;
		InputStream stream;
		String buffer, txn_id;
		long cur_memory_used = 0, memory_used_post_gc = 0;
		int cur_log_status_period = 0;

		// Get the global data
		GlobalData globalData = (GlobalData)DataManager.GetData(Constants.DT_GLOBAL, Constants.GLOBAL_DATA_ID, false);

		// Get the prev_payment_count
		prev_payment_count = cur_payment_count = globalData.prev_payment_count;

		// Update cycle
		for (;;)
		{
 			if ((WOCServer.log_flags & Constants.LOG_UPDATE) != 0) {
				Constants.WriteToLog("log_update.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " UpdateThread about to sleep" + "\n");
			}

			// Sleep for a while, until the next check. (Done first to give server a chance to start up.)
			try{
			  Thread.sleep(Constants.UPDATE_THREAD_SLEEP_MILLISECONDS);
				prev_sleep_time = Constants.GetTime();
			}catch(java.lang.InterruptedException e){Output.PrintTimeToScreen("UpdateThread Insomnia");}

			if ((WOCServer.log_flags & Constants.LOG_UPDATE) != 0) {
				Constants.WriteToLog("log_update.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " UpdateThread woke up" + "\n");
			}

      // Update the global data's heartbeat
      globalData.heartbeat = Constants.GetTime();
      DataManager.MarkForUpdate(globalData);

			// If the update thread is not active, continue to next loop iteration.
			if (!WOCServer.update_thread_active) {
				continue;
			}

			// Send any prize payments
			Money.SendPrizePayments();

			//// Check for payments
			//Money.CheckForPayments();

			// Check for subscription changes
			Money.CheckSubscriptions();

			// Create a user report if necessary //////////////////////////////
			if (create_user_report)
			{
				try
				{
					Output.PrintToScreen("About to create user report");
					CreateUserReport();
					Output.PrintToScreen("Finished creating user report");
				}
				catch(Exception e)
				{
					Output.PrintToScreen("Exception during CreateUserReport() call in UpdateThread:");
					Output.PrintException(e);
				}

				create_user_report = false;
			}

			// Create list of moderators if necessary //////////////////////////////
			if (create_moderator_list)
			{
				try
				{
					CreateModeratorList();
				}
				catch(Exception e)
				{
					Output.PrintToScreen("Exception during CreateModeratorList() call in UpdateThread:");
					Output.PrintException(e);
				}

				create_moderator_list = false;
			}

			// Perform periodic updates if necessary /////////////////////////////////////////////////////

			// Get the current time
			int cur_time = Constants.GetTime();

			if ((cur_time / Constants.LOG_STATUS_PERIOD) != cur_log_status_period)
			{
				// Log the current status of the server.
				LogStatus();

				// Record the new period.
				cur_log_status_period = cur_time / Constants.LOG_STATUS_PERIOD;
			}

			boolean perform_quarter_hourly_update = ((cur_time / (Constants.SECONDS_PER_HOUR / 4)) != globalData.cur_quarter_hourly_update_period);
			boolean perform_hourly_update = ((cur_time / Constants.SECONDS_PER_HOUR) != globalData.cur_hourly_update_period);
			boolean perform_daily_update = (((cur_time / Constants.SECONDS_PER_DAY) != globalData.cur_daily_update_period) && (Constants.GetHour() == 0));
			boolean perform_weekly_update = ((cur_time / (Constants.SECONDS_PER_DAY * 7)) != globalData.cur_weekly_update_period);
			boolean perform_monthly_update = ((Constants.GetMonth() + 1) != globalData.cur_monthly_update_period);

			try
			{
        if ((WOCServer.log_flags & Constants.LOG_UPDATE) != 0) Constants.WriteToLog("log_update.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " UpdateThread cur hour: " + (cur_time / Constants.SECONDS_PER_HOUR) + ", cur_hourly_update_period: " + globalData.cur_hourly_update_period + "\n");

				// Perform quarter hourly update if appropriate
        if (perform_quarter_hourly_update)
        {
          if ((WOCServer.log_flags & Constants.LOG_UPDATE) != 0) Constants.WriteToLog("log_update.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " UpdateThread performing quarter hourly update" + "\n");
          Output.PrintToScreen(Constants.GetFullDate() + ": Performing quarter hourly update");

					// Iterate through all connected clients, updating correlation counts for all devices.
					DeviceData dd0, dd1;
					int count;
					for (ClientThread ct0 : WOCServer.client_table.values())
					{
						dd0 = ct0.GetDeviceData();

						if (dd0 == null)
						{
							Output.PrintToScreen("ERROR: client thread with index " + ct0.client_index + ", user ID: " + ct0.GetUserID() + " has device data: " + dd0);
							continue;
						}

						for (ClientThread ct1 : WOCServer.client_table.values())
						{
							dd1 = ct1.GetDeviceData();

							if (dd1 == null)
							{
								Output.PrintToScreen("ERROR: client thread with index " + ct1.client_index + ", user ID: " + ct1.GetUserID() + " has device data: " + dd0);
								continue;
							}

							if (dd0 != dd1)
							{
								// Increment dd0's correlation count for dd1.
								count = dd0.correlation_counts.containsKey(dd1.ID) ? dd0.correlation_counts.get(dd1.ID) : 0;
								dd0.correlation_counts.put(dd1.ID, count + 1);
							}
						}

						// Increment dd0's num_correlation_checks.
						dd0.num_correlation_checks++;

						// Mark the current DeviceData to be updated.
						DataManager.MarkForUpdate(dd0);
					}

					// Perform hourly update if appropriate
					if (perform_hourly_update)
					{
						if ((WOCServer.log_flags & Constants.LOG_UPDATE) != 0) Constants.WriteToLog("log_update.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " UpdateThread performing hourly update" + "\n");
						Output.PrintToScreen(Constants.GetFullDate() + ": Performing hourly update");

						// Testing
						WOCServer.OutputMemoryStats();

						// Determine highest nation ID
						int highestNationID = DataManager.GetHighestDataID(Constants.DT_NATION);

						// Iterate through each nation
						NationData curNationData;
						int nation_potential_level, nation_creation_time, num_nations_deleted = 0, num_nations_moved = 0;
						int nation_place, farming_xp, curNationID;
						boolean nation_vanquishable;
						String report;
						for (curNationID = 1; curNationID <= highestNationID; curNationID++)
						{
							// Get the data for the nation with the current ID
							curNationData = (NationData)DataManager.GetData(Constants.DT_NATION, curNationID, false);

							if (((WOCServer.log_flags & Constants.LOG_UPDATE) != 0) && ((curNationID % 1000) == 0)) Constants.WriteToLog("log_update.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " UpdateThread updating nation " + curNationID + "\n");

							// If no nation exists with this ID, continue to next.
							if (curNationData == null) {
								continue;
							}

							// Award the nation its farming XP for this hour.
							if ((curNationData.shared_energy_xp_per_hour > 0) || (curNationData.shared_manpower_xp_per_hour > 0))
							{
								// Determine the amount of XP to be granted to the nation for this hour.
								farming_xp = (int)(((float)curNationData.shared_energy_xp_per_hour * (float)curNationData.shared_energy_fill) +
																	 ((float)curNationData.shared_manpower_xp_per_hour * (float)curNationData.shared_manpower_fill) + 0.5f);

								// Add the XP to the nation.
								Gameplay.AddXP(curNationData, farming_xp, -1, -1, -1, true, true, 0, Constants.XP_FARMING);

								// Record user report of this XP award
								curNationData.ModifyUserReportValueInt(UserData.ReportVal.report__farming_XP, farming_xp);
							}

							// Receive pending_xp for this hour.
							if (curNationData.pending_xp > 0)
							{
								// Determine how much pending_xp to receive.
								int xp_to_receive = Math.min(curNationData.pending_xp, Constants.UNITE_PENDING_XP_PER_HOUR);

								// Remove that amount of pending_xp from the nation.
								curNationData.pending_xp -= xp_to_receive;

								// Add that amount of XP to the nation.
								Gameplay.AddXP(curNationData, xp_to_receive, -1, -1, -1, false, false, 0, Constants.XP_UNITE_PENDING);

								// Log suspect
								if ((curNationData.log_suspect_expire_time > cur_time) && (xp_to_receive > 1))
								{
									// Log the details of this xp gain.
									Constants.WriteToLog("log_suspect.txt", Constants.GetTimestampString() + ": '" + curNationData.name + "'(ID:" + curNationData.ID + ", Level:" + curNationData.level + ", XP: " + curNationData.xp + ") received " + xp_to_receive + " XP that were pending. Remaining pending XP: " + curNationData.pending_xp + ".\n");
								}
							}

							// TODO: Redo this. Should nations ever be deleted?
							if (false/*nation_level < Constants.MID_LEVEL_LIMIT*/)
							{
								nation_vanquishable = Gameplay.IsNationVanquishable(curNationData);

								if (nation_vanquishable)
								{
									// This low level nation has been abandoned long enough to be vanquished,
									// and has never purchased any credits. Delete it.
									World.RemoveNation(curNationID);

									// Keep track of number of low level nations deleted
									num_nations_deleted++;

									// Continue on to next nation
									continue;
								}
							}

							// Remove any temporary technologies this nation has that are now obsolete, and update stats.
							Technology.UpdateStats(curNationID, curNationData);

							// If the nation's level justifies it, decrease the nation's rebirth_countdown appropriately for this hour.
							if (curNationData.level > curNationData.GetFinalRebirthAvailableLevel())
							{
								// Only decrease rebirth countdown if the nation is not suspended -- that is, if its area > 1, or if it has won prize money recently.
								if ((curNationData.mainland_footprint.area > 1) || ((Constants.GetTime() - curNationData.prev_prize_money_received_time) < Constants.SUSPEND_TIME_SINCE_LAST_WINNINGS))
								{
									// Determine rate of countdown decrease per day for this nation
									int levels_above_rebirth_available_level = curNationData.level - curNationData.GetFinalRebirthAvailableLevel();
									float decrease_per_day = levels_above_rebirth_available_level;

									// Accelerate the countdown exponentially if the nation's level is high enough
									if (levels_above_rebirth_available_level > Constants.REBIRTH_COUNTDOWN_ACCELERATE_LEVEL) {
										decrease_per_day += (float)Math.pow(levels_above_rebirth_available_level - Constants.REBIRTH_COUNTDOWN_ACCELERATE_LEVEL, Constants.REBIRTH_COUNTDOWN_ACCELERATE_POWER);
									}

									// Subtract (decrease_per_day / 24) from rebirth_countdown.
									Gameplay.ChangeRebirthCountdown(curNationData, -(decrease_per_day / 24.0f));
								}
							}

							// If we'll also be performing a daily update...
							if (perform_daily_update)
							{
								// Remove any obsolete raids from the nation's attack and defense logs.
								Raid.RemoveObsoleteRaidsFromLog(curNationData.raid_attack_log);
								Raid.RemoveObsoleteRaidsFromLog(curNationData.raid_defense_log);
							}

							// Mark this nation's data to be updated
							DataManager.MarkForUpdate(curNationData);

							//Constants.WriteToLog("log_MFU.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " "  + curNationData.ID + " evt: UpdateThread - hourly\n");

							// Sleep briefly between nations, to give the main thread CPU time and avoid lag.
							try{
								Thread.sleep(Constants.UPDATE_PER_NATION_SLEEP_MILLISECONDS);
							}catch(java.lang.InterruptedException e){Output.PrintTimeToScreen("UpdateThread Insomnia");}
						}

						if ((WOCServer.log_flags & Constants.LOG_UPDATE) != 0) Constants.WriteToLog("log_update.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " UpdateThread Done update of nations. " + num_nations_deleted + " nations deleted, " + num_nations_moved + " moved.\n");
						Output.PrintToScreen("Done update of nations. " + num_nations_deleted + " nations deleted, " + num_nations_moved + " moved.");

						if ((WOCServer.log_flags & Constants.LOG_UPDATE) != 0) Constants.WriteToLog("log_update.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " UpdateThread cur day: " + (cur_time / Constants.SECONDS_PER_DAY) + ", cur_daily_update_period: " + globalData.cur_daily_update_period + ", perform_daily_update: " + perform_daily_update + "\n");

						// Perform daily update if appropriate
						//September 2007, attempt to force this to happen near midnight each day
						if (perform_daily_update)
						{
							boolean exists = (new File(Constants.log_dir + "archives/output/output." + (Constants.GetMonth()+1 < 10 ? "0" : "") + (Constants.GetMonth() + 1) + "." + (Constants.GetDate()-1 < 10 ? "0" : "") + (Constants.GetDate() -1) + "." + (Constants.GetYear()+1900)+ ".txt")).exists();
							File renamed;

							if (exists)
							{
								renamed = new File(Constants.log_dir + "archives/output/output." + (Constants.GetMonth()+1 < 10 ? "0" : "") + (Constants.GetMonth() + 1) + "." + (Constants.GetDate()-1 < 10 ? "0" : "") + (Constants.GetDate() -1) + "." + (Constants.GetYear()+1900)+ "." + Constants.GetTime() + ".txt");
							}

							else
							{
								renamed = new File(Constants.log_dir + "archives/output/output." + (Constants.GetMonth()+1 < 10 ? "0" : "") + (Constants.GetMonth() + 1) + "." + (Constants.GetDate()-1 < 10 ? "0" : "") + (Constants.GetDate() -1) + "." + (Constants.GetYear()+1900)+ ".txt");
							}

							//rename the log_output.txt file so we don't have 125 MB log files to slog thru
							File output = new File(Constants.log_dir + "log_output.txt");
							output.renameTo(renamed);

							Output.PrintToScreen(Constants.GetFullDate() + ": Performing daily update");
							if ((WOCServer.log_flags & Constants.LOG_UPDATE) != 0) Constants.WriteToLog("log_update.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " UpdateThread performing daily update." + "\n");

							// Purchasable tech price update /////////

							TechData techData;
							float revenue_rate;
							int price_increment, prev_price, min_price, max_price;
							String log_string = "";

							// Log this tech price update
							if ((WOCServer.log_flags & Constants.LOG_UPDATE) != 0) Constants.WriteToLog("log_update.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " UpdateThread about to update tech prices." + "\n");

							// Write header to log_string
							log_string += "\n\n\n" + Constants.GetShortDateString() + " - Daily Technology Price Update\n";

							for (TechPriceRecord tech_price_record : GlobalData.instance.tech_price_records.values())
							{
								// Get the tech's data
								techData = TechData.GetTechData(tech_price_record.ID);

								// Make sure tech data exists
								if (techData == null) {
									continue;
								}

								// Write to log_string
								log_string += techData.name + " (" + tech_price_record.ID + ")    play time: " + (((float)tech_price_record.play_time) / 3600.0f) + " hours\n";

								// If this tech has been available to nations for the minimum play time, update its price.
								if (tech_price_record.play_time >= Constants.UPDATE_PRICE_MIN_PLAY_TIME)
								{
									// Based on its current price, determine the increment by which the price should change.
									if (tech_price_record.price < 10) price_increment = 1;
									else if (tech_price_record.price < 30) price_increment = 2;
									else if (tech_price_record.price < 80) price_increment = 5;
									else if (tech_price_record.price < 100) price_increment = 10;
									else if (tech_price_record.price < 300) price_increment = 20;
									else if (tech_price_record.price < 800) price_increment = 50;
									else price_increment = 100;

									// Determine the tech's min and max prices.
									min_price = (techData.min_price == 0) ? Constants.MIN_TECH_PRICE : techData.min_price;
									max_price = (techData.max_price == 0) ? Constants.MAX_TECH_PRICE : techData.max_price;

									// Cache the tech's prev price, and store the current price as the prev price.
									prev_price = tech_price_record.prev_price;
									tech_price_record.prev_price = tech_price_record.price;

									// Determine the tech's revenue rate since it was last updated.
									revenue_rate = (float)(tech_price_record.purchase_count * tech_price_record.price) / (float)tech_price_record.play_time;

									if (revenue_rate == 0f)
									{
										// This tech has not been purchased at all. Decrease the price.
										tech_price_record.price -= price_increment;
									}
									else if (prev_price == tech_price_record.price)
									{
										// The price was unchanged during the last update, because it's hit the minimum or maximum. Move it away from that limit by one increment.
										if (tech_price_record.price == max_price) {
											tech_price_record.price -= price_increment;
										} else {
											tech_price_record.price += price_increment;
										}
									}
									else if ((tech_price_record.prev_revenue_rate <= revenue_rate) == (prev_price <= tech_price_record.price))
									{
										// Price and revenue have both either increased or decreased. Increase the price.
										tech_price_record.price += price_increment;
									}
									else
									{
										// Price and revenue have changed in opposite directions. Decrease the price.
										tech_price_record.price -= price_increment;
									}

									// Bound the new price
									if (tech_price_record.price < min_price) tech_price_record.price = min_price;
									if (tech_price_record.price > max_price) tech_price_record.price = max_price;

									// Write to log_string
									log_string += "    prev revenue rate: " + tech_price_record.prev_revenue_rate + " \n";
									log_string += "    prev price: " + prev_price + "\n";
									log_string += "    cur revenue rate: " + revenue_rate + " \n";
									log_string += "    cur price: " + tech_price_record.prev_price + "\n";
									log_string += "    new price: " + tech_price_record.price + "\n";
									log_string += "    revenue: " + (tech_price_record.purchase_count * tech_price_record.prev_price) + "\n";

									// Update records for the price update
									tech_price_record.prev_revenue_rate = revenue_rate;
									tech_price_record.purchase_count = 0;
									tech_price_record.play_time = 0;
								}
							}

							// Broadcast a tech prices event to all clients in the game now.
							OutputEvents.BroadcastTechPricesEvent();

							// Mark the global data to be updated
							DataManager.MarkForUpdate(globalData);

							// Write log_string to log file
							Constants.WriteToLog("log_prices.txt", log_string);

							// Perform daily tournament update
							TournamentData.instance.UpdateForDay();

							// Log amount of each object on map
							Admin.LogObjects();

							if ((WOCServer.log_flags & Constants.LOG_UPDATE) != 0) Constants.WriteToLog("log_update.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " UpdateThread about to perform daily user update." + "\n");

							// Perform daily update on all user data
							DailyUpdate_Users(perform_monthly_update);

							if ((WOCServer.log_flags & Constants.LOG_UPDATE) != 0) Constants.WriteToLog("log_update.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " UpdateThread about to create moderator list." + "\n");

							// Create html page listing all moderators
							//this seems to hang as of Oct 2006
							//CreateModeratorList();

							if ((WOCServer.log_flags & Constants.LOG_UPDATE) != 0) Constants.WriteToLog("log_update.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " UpdateThread about to update public logs." + "\n");

							// Update the public log files
							UpdatePublicLogs();

							// Perform weekly update if a new week has begun
							if (perform_weekly_update)
							{
								if ((WOCServer.log_flags & Constants.LOG_UPDATE) != 0) Constants.WriteToLog("log_update.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " UpdateThread about to perform weekly update." + "\n");
								Output.PrintToScreen(Constants.GetFullDate() + ": Performing weekly update");

								// Store the new cur_weekly_update_period
								globalData.cur_weekly_update_period = (cur_time / (Constants.SECONDS_PER_DAY * 7));
							}

							// Perform monthly update if a new month has begun
							if (perform_monthly_update)
							{
								//once a month, rename (archive) a bunch of log files
								//this keeps their size under control

								//File nations = new File(Constants.log_dir + "log_nations.txt");
								//File renamed_n = new File(Constants.log_dir + "archives/nations/nations_" + (Constants.GetMonth() + 1) + "_" + Constants.GetDate() + "_" + (Constants.GetYear()+1900)+ ".txt");
								//nations.renameTo(renamed_n);

								//File users = new File(Constants.log_dir + "log_users.txt");
								//File renamed_u = new File(Constants.log_dir + "archives/users/users_" + (Constants.GetMonth() + 1) + "_" + Constants.GetDate() + "_" + (Constants.GetYear()+1900)+ ".txt");
								//users.renameTo(renamed_u);

								File bans = new File(Constants.log_dir + "log_ban.txt");
								File renamed_b = new File(Constants.log_dir + "archives/bans/ban_" + (Constants.GetMonth() + 1) + "_" + Constants.GetDate() + "_" + (Constants.GetYear()+1900)+ ".txt");
								bans.renameTo(renamed_b);

								File migrate = new File(Constants.log_dir + "log_migrate.txt");
								File renamed_m = new File(Constants.log_dir + "archives/migrate/migrate_" + (Constants.GetMonth() + 1) + "_" + Constants.GetDate() + "_" + (Constants.GetYear()+1900)+ ".txt");
								migrate.renameTo(renamed_m);

								File purchase = new File(Constants.log_dir + "log_purchase.txt");
								File renamed_p = new File(Constants.log_dir + "archives/purchases/purchase_" + (Constants.GetMonth() + 1) + "_" + Constants.GetDate() + "_" + (Constants.GetYear()+1900)+ ".txt");
								purchase.renameTo(renamed_p);

								File bug = new File(Constants.log_dir + "log_bug.txt");
								File renamed_bug = new File(Constants.log_dir + "archives/bugs/bug_" + (Constants.GetMonth() + 1) + "_" + Constants.GetDate() + "_" + (Constants.GetYear()+1900)+ ".txt");
								bug.renameTo(renamed_bug);

								File ud_bug = new File(Constants.log_dir + "log_ud_bug.txt");
								File renamed_udb = new File(Constants.log_dir + "archives/bugs/ud_bug_" + (Constants.GetMonth() + 1) + "_" + Constants.GetDate() + "_" + (Constants.GetYear()+1900)+ ".txt");
								ud_bug.renameTo(renamed_udb);

								//File player = new File(Constants.log_dir + "log_players.txt");
								//File renamed_pl = new File(Constants.log_dir + "archives/players/players_" + (Constants.GetMonth() + 1) + "_" + Constants.GetDate() + "_" + (Constants.GetYear()+1900)+ ".txt");
								//player.renameTo(renamed_pl);

								if ((WOCServer.log_flags & Constants.LOG_UPDATE) != 0) Constants.WriteToLog("log_update.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " UpdateThread about to perform monthly update." + "\n");
								Output.PrintToScreen(Constants.GetFullDate() + ": Performing monthly update");

								// Store the new cur_monthly_update_period
								globalData.cur_monthly_update_period = (Constants.GetMonth() + 1);

								// Clear the ranksData's ranks_monthly arrays
								RanksData.instance.ranks_nation_winnings_monthly.Clear();
								RanksData.instance.ranks_nation_xp_monthly.Clear();
								RanksData.instance.ranks_user_xp_monthly.Clear();
								RanksData.instance.ranks_user_followers_monthly.Clear();
								RanksData.instance.ranks_nation_tournament_trophies_monthly.Clear();
								RanksData.instance.ranks_nation_quests_monthly.Clear();
								RanksData.instance.ranks_nation_energy_donated_monthly.Clear();
								RanksData.instance.ranks_nation_area_monthly.Clear();
								RanksData.instance.ranks_nation_captures_monthly.Clear();
								RanksData.instance.ranks_nation_medals.Clear();
								RanksData.instance.ranks_nation_medals_monthly.Clear();
								RanksData.instance.ranks_nation_raid_earnings.Clear();
								RanksData.instance.ranks_nation_raid_earnings_monthly.Clear();
								RanksData.instance.ranks_nation_orb_shard_earnings.Clear();
								RanksData.instance.ranks_nation_orb_shard_earnings_monthly.Clear();

								// Mark the RanksData to be updated.
								DataManager.MarkForUpdate(RanksData.instance);

								// Perform monthly update for each nation. (This must be done here, after hourly and daily updates are complete, for tournament ranks.)
								for (curNationID = 1; curNationID <= highestNationID; curNationID++)
								{
									// Get the data for the nation with the current ID
									curNationData = (NationData)DataManager.GetData(Constants.DT_NATION, curNationID, false);

									if (((WOCServer.log_flags & Constants.LOG_UPDATE) != 0) && ((curNationID % 1000) == 0)) Constants.WriteToLog("log_update.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " UpdateThread monthly update for nation " + curNationID + "\n");

									// If no nation exists with this ID, continue to next.
									if (curNationData == null) {
										continue;
									}

									// Reset the nation's monthly ranks
									curNationData.goals_monthly_token.clear();
									curNationData.goals_monthly_winnings.clear();
									curNationData.prize_money_history_monthly = 0.0f;
									curNationData.raid_earnings_history_monthly = 0.0f;
									curNationData.orb_shard_earnings_history_monthly = 0.0f;
									curNationData.medals_history_monthly = curNationData.raid_attacker_medals + curNationData.raid_defender_medals;
									curNationData.xp_history_monthly = 0;
									curNationData.tournament_trophies_history_monthly = 0.0f;
									curNationData.donated_energy_history_monthly = 0.0f;
									curNationData.donated_manpower_history_monthly = 0.0f;
									curNationData.quests_completed_monthly = 0;
									curNationData.captures_history_monthly = 0;
									curNationData.max_area_monthly = curNationData.mainland_footprint.area;

									// Mark the nation's data to be updated.
									DataManager.MarkForUpdate(curNationData);

									// Sleep briefly between nations, to give the main thread CPU time and avoid lag.
									try{
										Thread.sleep(Constants.UPDATE_PER_NATION_SLEEP_MILLISECONDS);
									}catch(java.lang.InterruptedException e){Output.PrintTimeToScreen("UpdateThread Insomnia");}
								}

								// Perform monthly update for each device.
								for (int deviceID = 1; deviceID <= DataManager.GetHighestDataID(Constants.DT_DEVICE); deviceID++)
								{
									// Get the current device's data
									DeviceData deviceData = (DeviceData)DataManager.GetData(Constants.DT_DEVICE, deviceID, false);

									// Reset the device's correlation_counts and num_correlation_checks, and its tables of CorrelationRecords.
									deviceData.correlation_counts.clear();
									deviceData.num_correlation_checks = 0;
									deviceData.correlation_records.clear();
									deviceData.tracking_correlations.clear();

									// Mark the device's data to be updated.
									DataManager.MarkForUpdate(deviceData);
								}
							}

							// Store the new cur_daily_update_period
							globalData.cur_daily_update_period = (cur_time / Constants.SECONDS_PER_DAY);
						}

						// Store the new cur_hourly_update_period
						globalData.cur_hourly_update_period = (cur_time / Constants.SECONDS_PER_HOUR);
					}

					// Store the new cur_quarter_hourly_update_period
					globalData.cur_quarter_hourly_update_period = (cur_time / (Constants.SECONDS_PER_HOUR / 4));
				}
			}
			catch(Exception e)
			{
				Output.PrintToScreen("Exception during periodic update in UpdateThread:");
				Output.PrintException(e);
				e.printStackTrace();
			}

			// Update the ranks files if necessary ////////////////////////////////
			try
			{
				// Publish all of the ranks data if a new period has begun.
				if (((int)(Constants.GetTime() / Constants.RANKS_PUBLISH_PERIOD)) > globalData.cur_ranks_publish_period)
				{
          if ((WOCServer.log_flags & Constants.LOG_UPDATE) != 0) Constants.WriteToLog("log_update.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " UpdateThread about to update ranks files." + "\n");

					// Publish all of the ranks
					Output.PrintToScreen(Constants.GetShortTimeString() + " About to update ranks files");
					RanksData.instance.PublishAllRanks();
					TournamentData.instance.PublishTournamentRanksList();
					Output.PrintToScreen(Constants.GetShortTimeString() + " Done updating ranks files");

					// Store the number of the current cur_ranks_publish_period
					globalData.cur_ranks_publish_period = ((int)(Constants.GetTime() / Constants.RANKS_PUBLISH_PERIOD));
				}
			}
			catch(Exception e)
			{
				Output.PrintToScreen("Exception during PublishAllGoals() call in UpdateThread:");
				Output.PrintException(e);
			}

			// Update the database ////////////////////////////////

      if ((WOCServer.log_flags & Constants.LOG_UPDATE) != 0) Constants.WriteToLog("log_update.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " UpdateThread about to update database." + "\n");

			try
			{
				DataManager.UpdateDatabase(false);
			}
			catch(Exception e)
			{
				Output.PrintToScreen("Exception during UpdateDatabase() call in UpdateThread:");
				Output.PrintException(e);
			}

			// Garbage collect if appropriate ////////////////////////////////

			// Determine current amount of memory in use
			cur_memory_used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

			if (WOCServer.garbage_collect_active)
			{
				// If amount of memory in use has increased by more than MEM_INCREASE_GC_THRESHOLD, garbage collect.
				if ((cur_memory_used - memory_used_post_gc) > MEM_INCREASE_GC_THRESHOLD)
				{
          if ((WOCServer.log_flags & Constants.LOG_UPDATE) != 0) Constants.WriteToLog("log_update.txt", Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " UpdateThread about to garbage collect." + "\n");

					long prev_fine_time = Constants.GetFreshFineTime();

					// Garbage collect.
					System.gc();

					// Record amount of memory in use post gargbage collection.
					memory_used_post_gc = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

					Output.PrintToScreen("Garbage collect complete. (" + (Constants.GetFreshFineTime() - prev_fine_time) + " ms, " + cur_memory_used + " -> " + memory_used_post_gc + " in use)");
				}
			}
		}
	}

	public static void DailyUpdate_Users(boolean _perform_monthly_update)
	{
		Output.PrintToScreen(Constants.GetFullDate() + ": Performing daily user data update");

		// Determine highest user ID
		int highestUserID = DataManager.GetHighestDataID(Constants.DT_USER);

		// Determine the current time
		int cur_time = Constants.GetTime();

		// Iterate through each user
		UserData curUserData;
		int count, prev_login_time, user_nationID, num_accounts_deleted = 0;
		for (int curUserID = 1; curUserID <= highestUserID; curUserID++)
		{
			// Get the data for the user with the current ID
			curUserData = (UserData)DataManager.GetData(Constants.DT_USER, curUserID, false);

			// If no user exists with this ID, continue to next.
			if (curUserData == null) {
				continue;
			}

      // Update this user's chat_offense_level
      curUserData.chat_offense_level *= Constants.OFFENSE_LEVEL_MULTIPLIER_PER_DAY;
      if (curUserData.chat_offense_level < Constants.OFFENSE_LEVEL_MIN_THRESHOLD) {
        curUserData.chat_offense_level = 0.0f;
      }

			// Get time of user's previous login
			prev_login_time = curUserData.prev_login_time;

			// If the user account has expired...
			if ((cur_time - prev_login_time) > Constants.USER_ACCOUNT_EXPIRE_TIME)
			{
				// Determine the user's nation ID
				user_nationID = curUserData.nationID;

				// If the user has no nation, and less than 5000000 points, we may delete this expired user account.
				if (((user_nationID == -1) || (DataManager.GetData(Constants.DT_NATION, user_nationID, false) == null)) && (curUserData.xp < 5000000))
				{
          // Only delete if the username's account is not banned
          if (curUserData.game_ban_end_time < cur_time)
          {
            // Remove the expired user from the database
            DataManager.DeleteData(Constants.DT_USER, curUserID);

            // Increment num_accounts_deleted
            num_accounts_deleted++;
          }
				}
			}

			if (_perform_monthly_update)
			{
				// Rest the user's record of the max number of followers they've had this month, to their current number of followers.
				curUserData.max_num_followers_monthly = curUserData.followers.size();
				RanksData.instance.ranks_user_followers_monthly.UpdateRanks(curUserData.ID, curUserData.name, curUserData.max_num_followers_monthly, Constants.NUM_FOLLOWERS_RANKS, true);
			}

      // Mark this user to be updated
      DataManager.MarkForUpdate(curUserData);

			// Sleep briefly between users, to give the main thread CPU time and avoid lag.
			try{
				Thread.sleep(Constants.DAILY_UPDATE_PER_USER_SLEEP_MILLISECONDS);
			}catch(java.lang.InterruptedException e){Output.PrintTimeToScreen("UpdateThread Insomnia");}
		}

		Output.PrintToScreen("Done daily user data update. " + num_accounts_deleted + " users deleted.");
	}

	public void LogStatus()
	{
		int i;
		int cur_time = Constants.GetTime();

		// Determine load status
		long total_period = Constants.LOG_STATUS_PERIOD * 1000;
		float sleep_fraction = (float)(WOCServer.sleep_time) / (float)total_period;
		float status_load = (float)Math.max(0, 1.0 - sleep_fraction);
		//Output.PrintToScreen("total_period: " + total_period + ", sleep_fraction: " + sleep_fraction + ", status_load: " + status_load); // TESTING

		// Reset WOCServer's record of time spent sleeping.
		WOCServer.sleep_time = 0;

		// Determine login count status
		int status_count = WOCServer.num_clients_in_game;

		// Determine memory usage status
		float status_ram = 1f - ((float)Runtime.getRuntime().freeMemory() / (float)Runtime.getRuntime().totalMemory());

		// Update the status history.
		status_count_history.add(status_count);
		status_load_history.add(status_load);
		status_ram_history.add(status_ram);

		// Truncate status history.
		while (status_count_history.size() > Constants.LOG_STATUS_HISTORY_LEN) status_count_history.remove((int)0);
		while (status_load_history.size() > Constants.LOG_STATUS_HISTORY_LEN) status_load_history.remove((int)0);
		while (status_ram_history.size() > Constants.LOG_STATUS_HISTORY_LEN) status_ram_history.remove((int)0);

		// Log load percentage, if appropriate.
		if (((WOCServer.log_flags & Constants.LOG_LOAD) != 0) || (status_load >= 0.5f)) {
			Output.PrintToScreen("Load: " + (status_load * 100) + "%");
		}

		// Determine status string.
		String status = "Okay";
		if ((cur_time - WOCServer.prev_sleep_time) >= Constants.LOG_STATUS_ALERT_DELAY) status = "Server thread halted for " + (cur_time - WOCServer.prev_sleep_time) + "s!";
		if ((cur_time - UpdateThread.prev_sleep_time) >= Constants.LOG_STATUS_ALERT_DELAY) status = "Update thread halted for " + (cur_time - UpdateThread.prev_sleep_time) + "s!";
		if ((cur_time - BackupThread.prev_sleep_time) >= Constants.LOG_BACKUP_THREAD_STATUS_ALERT_DELAY) status = "Backup thread halted for " + (cur_time - BackupThread.prev_sleep_time) + "s!";
		if ((cur_time - EmailThread.prev_sleep_time) >= Constants.LOG_STATUS_ALERT_DELAY) status = "E-mail thread halted for " + (cur_time - EmailThread.prev_sleep_time) + "s!";
		if (((cur_time - InputThread.prev_sleep_time) >= Constants.LOG_STATUS_ALERT_DELAY) && (InputThread.prev_sleep_time != -1)) status = "Input thread halted for " + (cur_time - InputThread.prev_sleep_time) + "s!";
		if ((cur_time / Constants.SECONDS_PER_HOUR) > (GlobalData.instance.cur_hourly_update_period + 1)) status = "Late for hourly update!";
		if ((cur_time - DataManager.prev_update_database_time) >= Constants.LOG_STATUS_ALERT_DELAY) status = "Database not updated for " + (cur_time - DataManager.prev_update_database_time) + "s!";

		// If status is not okay, and the server started at least 10 minutes ago, send emergency message.
		if ((!status.equals("Okay")) && ((Constants.GetTime() - WOCServer.server_start_time) > 600)) {
			Admin.Emergency(status);
		}

		// Compose status log json string
		String status_log = "{\n\"time\":" + cur_time + ",\n";
		status_log += "\"status\":\"" + status + "\",\n";
		status_log += "\"count_history\":[ ";

		for (i = 0; i < status_count_history.size(); i++)
		{
			if (i > 0) status_log += ", ";
			status_log += status_count_history.get(i);
		}

		status_log += " ],\n";
		status_log += "\"load_history\":[ ";

		for (i = 0; i < status_load_history.size(); i++)
		{
			if (i > 0) status_log += ", ";
			status_log += status_load_history.get(i);
		}

		status_log += " ],\n";
		status_log += "\"ram_history\":[ ";

		for (i = 0; i < status_ram_history.size(); i++)
		{
			if (i > 0) status_log += ", ";
			status_log += status_ram_history.get(i);
		}

		status_log += " ]\n";
		status_log += "}";

		// Output status log
		Constants.WriteNewPublicLog("log_status.txt", status_log);
	}

	public void CreateUserReport()
	{
		// Determine highest user ID
		int highestUserID = DataManager.GetHighestDataID(Constants.DT_USER);

		// Create arrays
		int start_dates[] = new int[highestUserID + 1];
		int end_dates[] = new int[highestUserID + 1];

		// Determine the present date
		int present_date = Constants.GetAbsoluteDay();

		// Iterate through each user
		UserData curUserData;
		for (int curUserID = 1; curUserID <= highestUserID; curUserID++)
		{
			// Get the data for the user with the current ID
			curUserData = (UserData)DataManager.GetData(Constants.DT_USER, curUserID, false);

			// If no user exists with this ID, continue to next.
			if (curUserData == null) {
				continue;
			}

			// Store this user's start_date and end_date
			start_dates[curUserID] = curUserData.creation_time / Constants.SECONDS_PER_DAY;
			end_dates[curUserID] = curUserData.prev_login_time / Constants.SECONDS_PER_DAY;
		}

		String log_string = "";

		// Write to log_string
		log_string += "\n\n\n" + Constants.GetDateString() + " - User Report \n";

		// Determine the present fine time
		long present_fine_time = Constants.GetFineTime();

		// Iterate through the last 30 days
		int cur_date, i;
		Date whole_date;
		String cur_date_string;
		int num_started, num_ended, num_played_another_day, num_played_over_an_hour, num_never_logged_in;
		for (int days_ago = 30; days_ago > 0; days_ago--)
		{
			// Determine current date
			cur_date = present_date - days_ago;

			// Determine current date string
			whole_date = new Date(present_fine_time - ((long)days_ago * (long)(Constants.SECONDS_PER_DAY) * (long)1000));
			cur_date_string = (whole_date.getMonth() + 1) + "/" + whole_date.getDate() + "/" + (whole_date.getYear() + 1900);

			log_string += cur_date_string + "\n";

			// Determine data about users that started on current day
			num_started = 0;
			num_played_another_day = 0;
			num_played_over_an_hour = 0;
			num_never_logged_in = 0;
			for (i = 0; i <= highestUserID; i++)
			{
				if (start_dates[i] == cur_date)
				{
					num_started++;

					// Get the data for the user with the current ID
					curUserData = (UserData)DataManager.GetData(Constants.DT_USER, i, false);

					if (curUserData.prev_login_time == 0) num_never_logged_in++;
					if (((int)(curUserData.prev_login_time / Constants.SECONDS_PER_DAY)) > cur_date) num_played_another_day++;
					if (curUserData.play_time > Constants.SECONDS_PER_HOUR) num_played_over_an_hour++;
				}
			}

			// Determine data about users that ended on current day
			num_ended = 0;
			for (i = 0; i <= highestUserID; i++)
			{
				if (end_dates[i] == cur_date)
				{
					num_ended++;
				}
			}

			// Log data for current day
			log_string += "    Number of players started: " + num_started + "\n";
			log_string += "    Number of players ended (or never logged in): " + (num_ended + num_never_logged_in)+ "\n";
			if (num_started > 0)
			{
				log_string += "      Of players started, " + num_never_logged_in + " (" + Math.floor((float)num_never_logged_in / (float)num_started * 100.0f) + "%) never logged in.\n";
				log_string += "      Of players started, " + num_played_another_day + " (" + Math.floor((float)num_played_another_day / (float)num_started * 100.0f) + "%) played again another day.\n";
				log_string += "      Of players started, " + num_played_over_an_hour + " (" + Math.floor((float)num_played_over_an_hour / (float)num_started * 100.0f) + "%) played in total over an hour.\n";
				log_string += "      Player loss rate: " + Math.floor((float)(num_ended + num_never_logged_in) / (float)num_started * 100.0f) + "%\n";
			}
		}

		// Write log_string to log file
		Constants.WriteToLog("log_report.txt", log_string);
	}

	public void CreateModeratorList()
	{
		UserData curUserData;
		int curUserID;

		Output.PrintToScreen(Constants.GetFullDate() + ": Creating moderators.htm");

    mods_level1_html.delete(0, MODS_HTML_LENGTH);
    mods_level2_html.delete(0, MODS_HTML_LENGTH);
    mods_level3_html.delete(0, MODS_HTML_LENGTH);

		// Determine highest user ID
		int highestUserID = DataManager.GetHighestDataID(Constants.DT_USER);

    // Iterate through each user
		for (curUserID = 1; curUserID <= highestUserID; curUserID++)
		{
			// Get the data for the user with the current ID
			curUserData = (UserData)DataManager.GetData(Constants.DT_USER, curUserID, false);

			// If no user exists with this ID, continue to next.
			if (curUserData == null) {
				continue;
			}

			if (curUserData.mod_level == 3)
			{
				mods_level3_html.append(curUserData.name);
				mods_level3_html.append("<br>");
			}
      else if (curUserData.mod_level == 2)
			{
				mods_level2_html.append(curUserData.name);
				mods_level3_html.append("<br>");
			}
      else if (curUserData.mod_level == 1)
			{
				mods_level1_html.append(curUserData.name);
				mods_level3_html.append("<br>");
			}
		}

    mods_html.append(StringConstants.STR_PAGE_START);
    mods_html.append(StringConstants.STR_HEADING_ROW_BEGIN);
		mods_html.append("Level 3");
    mods_html.append(StringConstants.STR_HEADING_ROW_END);
    mods_html.append(StringConstants.STR_ROW_BEGIN);
    mods_html.append(mods_level3_html.toString());
    mods_html.append(StringConstants.STR_ROW_END);
    mods_html.append(StringConstants.STR_SPACER_ROW);

    mods_html.append(StringConstants.STR_PAGE_START);
    mods_html.append(StringConstants.STR_HEADING_ROW_BEGIN);
		mods_html.append("Level 2");
    mods_html.append(StringConstants.STR_HEADING_ROW_END);
    mods_html.append(StringConstants.STR_ROW_BEGIN);
    mods_html.append(mods_level2_html.toString());
    mods_html.append(StringConstants.STR_ROW_END);
    mods_html.append(StringConstants.STR_SPACER_ROW);

    mods_html.append(StringConstants.STR_PAGE_START);
    mods_html.append(StringConstants.STR_HEADING_ROW_BEGIN);
		mods_html.append("Level 1");
    mods_html.append(StringConstants.STR_HEADING_ROW_END);
    mods_html.append(StringConstants.STR_ROW_BEGIN);
    mods_html.append(mods_level1_html.toString());
    mods_html.append(StringConstants.STR_ROW_END);
    mods_html.append(StringConstants.STR_SPACER_ROW);

		mods_html.append(StringConstants.STR_PAGE_END);

		// Write the mods list file
		try{
			// Open the file
			String filename = Constants.publiclog_dir + "moderators.htm";

			FileWriter fw = new FileWriter(filename);

			// Write the html string
			fw.write(mods_html.toString());

			// Close the file
			fw.close();
		}
		catch(Exception e)  {Output.PrintToScreen("Failed to write moderators.htm");}

		Output.PrintToScreen(Constants.GetFullDate() + ": Finished creating moderators.htm");
	}

	void UpdatePublicLogs()
	{
		File file, dest_file;

		Output.PrintToScreen(Constants.GetFullDate() + ": Updating public log files");

		try
		{
			// Delete the expired oldest public log file, if it exists.
			file = new File(Constants.publiclog_dir + "publiclog_6.txt");
			if (file.exists()) {
				file.delete();
			}

			// Rename each public log file
			for (int i = 5; i >= 0; i--)
			{
				file = new File(Constants.publiclog_dir + "publiclog_" + i + ".txt");

				if (file.exists())
				{
					dest_file = new File(Constants.publiclog_dir + "publiclog_" + (i + 1) + ".txt");
					file.renameTo(dest_file);
				}
			}
		}
		catch(Exception e)  {Output.PrintException(e);}
	}
}
