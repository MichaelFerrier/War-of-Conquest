using UnityEngine;
using UnityEngine.Analytics;
using System;
using System.Collections;
using System.Collections.Generic;
using System.Text;
using I2.Loc;
using SimpleJSON;
using System.Diagnostics;
using Debug = UnityEngine.Debug;

public class HandleInputEvents : MonoBehaviour
{
  public MessageText messageText;
  public Chat chatSystem;

  public int clientVersion = 1;
  public MapView mapView;
  public Network network;
  public int inputBufferPos = 0;

  private const int MAX_STRING_LEN = 262144;
  private char[] subStringBuffer = new char[MAX_STRING_LEN + 1];

  private bool initial_stats_event_received = false;

  private float prev_add_xp_time = 0;

  // User flags
  public const int UF_ADMIN = 1;
  public const int UF_FIRST_LOGIN = 2;
  public const int UF_REGISTERED = 4;
  public const int UF_VETERAN_USER = 8;
  public const int IF_AD_BONUSES_ALLOWED = 16;
  public const int IF_CASH_OUT_PRIZES_ALLOWED = 32;
  public const int IF_CREDIT_PURCHASES_ALLOWED = 64;


  // Block flags
  public const int BF_EXTENDED_DATA = 1;

  void Update()
  {
    String eventID;
    if (network.inputBufferLength > 6) {
      string buffer_end = Encoding.UTF8.GetString(network.inputBuffer, network.inputBufferLength - 6, 5);

      if (buffer_end.Equals("?BVMW")) {
        //Debug.Log ("Input event received: " + Encoding.UTF8.GetString(network.inputBuffer, 0, network.inputBufferLength));

        // Process events
        while ((network.inputBufferLength - inputBufferPos) > 4) {
          eventID = DecodeString();

          //// TESTING
          //Debug.Log("inputBuffer from pos " + inputBufferPos + ": " + Encoding.UTF8.GetString(network.inputBuffer, inputBufferPos, network.inputBufferLength - inputBufferPos) + ", eventID: " + eventID);
          //Debug.Log("inputBuffer at position " + inputBufferPos + " of " + network.inputBufferLength);
          Debug.Log("EVENT: " + eventID + " at " + System.DateTime.Now.Second + ":" + System.DateTime.Now.Millisecond);

          if (eventID.Equals("add_technology")) {
            ProcessEventAddTechnology();
          } else if (eventID.Equals("add_xp")) {
            ProcessEventAddXP();
          } else if (eventID.Equals("admin_log")) {
            ProcessEventAdminLog();
          } else if (eventID.Equals("ally_nation_data")) {
            ProcessEventAllyNationData();
          } else if (eventID.Equals("announcement")) {
            ProcessEventAnnouncement();
          } else if (eventID.Equals("award_available_ad_bonus")) {
            ProcessEventAwardAvailableAdBonus();
          } else if (eventID.Equals("battle_process")) {
            ProcessEventBattleProcess();
          } else if (eventID.Equals("block_ext")) {
            ProcessEventBlockExtendedData();
          } else if (eventID.Equals("block_ext_clear")) {
            ProcessEventClearBlockExtendedData();
          } else if (eventID.Equals("block_update")) {
            ProcessEventBlockUpdate();
          } else if (eventID.Equals("block_process")) {
            ProcessEventBlockProcess();
          } else if (eventID.Equals("buy_resource")) {
            ProcessEventBuyResource();
          } else if (eventID.Equals("capture_storage")) {
            ProcessEventCaptureStorage();
          } else if (eventID.Equals("chat_ban")) {
            ProcessEventChatBan();
          } else if (eventID.Equals("chat_list")) {
            ProcessEventChatList();
          } else if (eventID.Equals("chat_list_add")) {
            ProcessEventChatListAdd();
          } else if (eventID.Equals("chat_list_remove")) {
            ProcessEventChatListRemove();
          } else if (eventID.Equals("clear_gui")) {
            ProcessEventClearGUI();
          } else if (eventID.Equals("collect_ad_bonus")) {
            ProcessEventCollectAdBonus();
          } else if (eventID.Equals("complaint")) {
            ProcessEventComplaint();
          } else if (eventID.Equals("completion")) {
            ProcessEventCompletion();
          } else if (eventID.Equals("connection_info")) {
            ProcessEventConnectionInfo();
          } else if (eventID.Equals("create_password_result")) {
            ProcessEventCreatePasswordResult();
          } else if (eventID.Equals("customize_nation_result")) {
            ProcessEventCustomizeNationResult();
          } else if (eventID.Equals("delete_map_flag")) {
            ProcessEventDeleteMapFlag();
          } else if (eventID.Equals("discovery")) {
            ProcessEventDiscovery();
          } else if (eventID.Equals("display_battle")) {
            ProcessEventDisplayBattle();
          } else if (eventID.Equals("display_process")) {
            ProcessEventDisplayProcess();
          } else if (eventID.Equals("event_account_info")) {
            ProcessEventAccountInfo();
          } else if (eventID.Equals("event_add_follower")) {
            ProcessEventAddFollower();
          } else if (eventID.Equals("event_add_object")) {
            ProcessEventAddObject();
          } else if (eventID.Equals("event_add_patron_offer")) {
            ProcessEventAddPatronOffer();
          } else if (eventID.Equals("event_all_build_counts")) {
            ProcessEventAllBuildCounts();
          } else if (eventID.Equals("event_all_followers")) {
            ProcessEventAllFollowers();
          } else if (eventID.Equals("event_all_messages")) {
            ProcessEventAllMessages();
          } else if (eventID.Equals("event_all_objects")) {
            ProcessEventAllObjects();
          } else if (eventID.Equals("event_all_patron_offers")) {
            ProcessEventAllPatronOffers();
          } else if (eventID.Equals("event_alliances")) {
            ProcessEventAlliances();
          } else if (eventID.Equals("event_build_count")) {
            ProcessEventBuildCount();
          } else if (eventID.Equals("event_chat")) {
            ProcessEventChat();
          } else if (eventID.Equals("event_chat_log")) {
            ProcessEventChatLog();
          } else if (eventID.Equals("event_fealty_info")) {
            ProcessEventFealtyInfo();
          } else if (eventID.Equals("event_info")) {
            ProcessEventInfo();
          } else if (eventID.Equals("event_join_nation_result")) {
            ProcessEventJoinNationResult();
          } else if (eventID.Equals("event_map")) {
            ProcessEventMap();
          } else if (eventID.Equals("event_map_flags")) {
            ProcessEventMapFlags();
          } else if (eventID.Equals("event_message")) {
            ProcessEventMessage();
          } else if (eventID.Equals("event_members")) {
            ProcessEventMembers();
          } else if (eventID.Equals("event_more_messages")) {
            ProcessEventMoreMessages();
          } else if (eventID.Equals("event_nation_info")) {
            ProcessEventNationInfo();
          } else if (eventID.Equals("event_new_message")) {
            ProcessEventNewMessage();
          } else if (eventID.Equals("event_pan_view")) {
            ProcessEventPanView();
          } else if (eventID.Equals("event_purchase_complete")) {
            ProcessEventPurchaseComplete();
          } else if (eventID.Equals("event_remove_follower")) {
            ProcessEventRemoveFollower();
          } else if (eventID.Equals("event_remove_object")) {
            ProcessEventRemoveObject();
          } else if (eventID.Equals("event_remove_patron_offer")) {
            ProcessEventRemovePatronOffer();
          } else if (eventID.Equals("event_report")) {
            ProcessEventReport();
          } else if (eventID.Equals("event_requestor")) {
            ProcessEventRequestor();
          } else if (eventID.Equals("event_requestor_duration")) {
            ProcessEventRequestorDuration();
          } else if (eventID.Equals("event_stats")) {
            ProcessEventStats();
          } else if (eventID.Equals("event_technologies")) {
            ProcessEventTechnologies();
          } else if (eventID.Equals("event_tech_prices")) {
            ProcessEventTechPrices();
          } else if (eventID.Equals("event_update")) {
            ProcessEventUpdate();
          } else if (eventID.Equals("event_update_bars")) {
            ProcessEventUpdateBars();
          } else if (eventID.Equals("hist_extent")) {
            ProcessEventHistoricalExtent();
          } else if (eventID.Equals("log_in_result")) {
            ProcessEventLogInResult();
          } else if (eventID.Equals("migration")) {
            ProcessEventMigration();
          } else if (eventID.Equals("nation_areas")) {
            ProcessEventNationAreas();
          } else if (eventID.Equals("nation_flags")) {
            ProcessEventNationFlags();
          } else if (eventID.Equals("no_associated_player")) {
            ProcessEventNoAssociatedPlayer();
          } else if (eventID.Equals("patron_info")) {
            ProcessEventPatronInfo();
          } else if (eventID.Equals("post_message_result")) {
            ProcessEventPostMessageResult();
          } else if (eventID.Equals("nation_data")) {
            ProcessEventNationData();
          } else if (eventID.Equals("nation_name_available")) {
            ProcessEventNationNameAvailable();
          } else if (eventID.Equals("nation_orbs")) {
            ProcessEventNationOrbs();
          } else if (eventID.Equals("nation_password")) {
            ProcessEventNationPassword();
          } else if (eventID.Equals("new_player_result")) {
            ProcessEventNewPlayerResult();
          } else if (eventID.Equals("orb_winnings")) {
            ProcessEventOrbWinnings();
          } else if (eventID.Equals("quest_status")) {
            ProcessEventQuestStatus();
          } else if (eventID.Equals("raid_log_entry")) {
            ProcessEventRaidLogEntry();
          } else if (eventID.Equals("raid_logs")) {
            ProcessEventRaidLogs();
          } else if (eventID.Equals("raid_replay")) {
            ProcessEventRaidReplay();
          } else if (eventID.Equals("raid_status")) {
            ProcessEventRaidStatus();
          } else if (eventID.Equals("ranks_data")) {
            ProcessEventRanksData();
          } else if (eventID.Equals("remove_technology")) {
            ProcessEventRemoveTechnology();
          } else if (eventID.Equals("request_ping")) {
            ProcessEventRequestPing();
          } else if (eventID.Equals("salvage")) {
            ProcessEventSalvage();
          } else if (eventID.Equals("set_level")) {
            ProcessEventSetLevel();
          } else if (eventID.Equals("set_map_flag")) {
            ProcessEventSetMapFlag();
          } else if (eventID.Equals("set_target")) {
            ProcessEventSetTarget();
          } else if (eventID.Equals("stop_auto_process")) {
            ProcessEventStopAutoProcess();
          } else if (eventID.Equals("subscription")) {
            ProcessEventSubscription();
          } else if (eventID.Equals("suspend")) {
            ProcessEventSuspend();
          } else if (eventID.Equals("tnmt_status_global")) {
            ProcessEventTournamentStatusGlobal();
          } else if (eventID.Equals("tnmt_status_nation")) {
            ProcessEventTournamentStatusNation();
          } else if (eventID.Equals("tower_action")) {
            ProcessEventTowerAction();
          } else if (eventID.Equals("trigger_inert")) {
            ProcessEventTriggerInert();
          } else if (eventID.Equals("username_available")) {
            ProcessEventUsernameAvailable();
          } else if (eventID.Equals("end")) {
            inputBufferPos++;
          } else {
            // Catch any unknown events
            Debug.Log("Unknown eventID:  '" + eventID + "'");
            break;
          }
        }

        // All of the contents of the input buffer have been consumed. Reset the buffer.
        network.ResetInputBuffer();

        // Reset input buffer position.
        inputBufferPos = 0;
      }
    }
  }

  void ProcessEventAddTechnology()
  {
    int techID = DecodeUnsignedNumber(2);
    int seconds_until_expires = DecodeUnsignedNumber(5);
    int delay = DecodeUnsignedNumber(1);

    AddTechnology(techID, seconds_until_expires, delay);
  }

  void ProcessEventAddXP()
  {
    int xp_delta = DecodeNumber(5);
    int xp = DecodeNumber(5);
    int userID = DecodeNumber(4);
    int block_x = DecodeNumber(3);
    int block_y = DecodeNumber(3);
    int delay = DecodeUnsignedNumber(1);

    //Debug.Log("Add XP event, xp delta: " + xp_delta +", xp: " + xp);

    // Record the new XP value
    GameData.instance.xp = xp;

    // Start coroutine to display the change after the given delay.
    StartCoroutine(AddXPAfterDelay(xp_delta, xp, userID, block_x, block_y, delay));
  }

  void ProcessEventAdminLog()
  {
    string log_text = DecodeString();
    AdminPanel.instance.AddLogText(log_text);
  }

  void ProcessEventAllyNationData()
  {
    // Read information about nations.
    int nationID, numNations = DecodeUnsignedNumber(2);
    for (int i = 0; i < numNations; i++) {
      // Read this nation's ID.
      nationID = DecodeNumber(4);

      // Read the nation's data
      ReadNationData(nationID, false);
    }
  }

  void ProcessEventAnnouncement()
  {
    string announcement = DecodeLocalizableString();
    AnnouncementPanel.Activate(announcement);
  }

  void ProcessEventBlockExtendedData()
  {
    ReadBlockExtendedData(false);
  }

  void ProcessEventClearBlockExtendedData()
  {
    int blockX = DecodeUnsignedNumber(4);
    int blockZ = DecodeUnsignedNumber(4);

    if (!MapView.instance.BlockOutsideViewData(blockX, blockZ)) {
      // Clear the block's extended data.
      BlockData blockData = mapView.blocks[blockX - mapView.viewDataBlockX0, blockZ - mapView.viewDataBlockZ0];
      blockData.objectID = -1;
      blockData.wipe_end_time = -1;

      // Tell the MapView that the block's extended data has been modified.
      mapView.ModifiedExtendedData(blockX, blockZ);
    }
  }

  void ProcessEventAllMessages()
  {
    bool unread;
    int userID, nationID, deviceID, time, reported;
    string username, nation_name, text, timestamp;

    int num_messages = DecodeUnsignedNumber(2);

    for (int i = 0; i < num_messages; i++) {
      unread = (DecodeUnsignedNumber(1) != 0);
      userID = DecodeNumber(5);
      nationID = DecodeNumber(5);
      deviceID = DecodeNumber(5);
      username = DecodeString();
      nation_name = DecodeString();
      text = DecodeString();
      timestamp = DecodeString();
      time = DecodeUnsignedNumber(6);
      reported = DecodeUnsignedNumber(1);

      //Debug.Log("Message " + i + " of " + num_messages + " (" + timestamp + "): " + text);

      // If the message text is a JSON encoded localizable string, decode it.
      if (text.Contains("{\n\"ID\": \"")) text = LocalizableStringFromJSON(text);

      // Add this message to the Messages Panel.
      MessagesPanel.instance.AddMessage(unread, userID, nationID, deviceID, username, nation_name, text, timestamp, time, reported, false);
    }
  }

  void ProcessEventAllObjects()
  {
    int blockX, blockZ, objectID;

    // Clear the list of objects.
    GameData.instance.objects.Clear();

    int num_objects = DecodeUnsignedNumber(3);

    for (int i = 0; i < num_objects; i++) {
      blockX = DecodeUnsignedNumber(4);
      blockZ = DecodeUnsignedNumber(4);
      objectID = DecodeUnsignedNumber(4);

      // Add a record for this object.
      GameData.instance.objects.Add(new ObjectRecord(blockX, blockZ, objectID));
      //Debug.Log("Adding object ID " + objectID + " at " + blockX + "," + blockZ);
    }

    // Tell the nation panel that the resources list has been modified.
    NationPanel.instance.ResourcesListModified();
  }

  void ProcessEventAddObject()
  {
    int blockX, blockZ, objectID;

    blockX = DecodeUnsignedNumber(4);
    blockZ = DecodeUnsignedNumber(4);
    objectID = DecodeUnsignedNumber(4);

    // Add a record for this object.
    GameData.instance.objects.Add(new ObjectRecord(blockX, blockZ, objectID));

    // Tell the nation panel that the resources list has been modified.
    NationPanel.instance.ResourcesListModified();
  }

  void ProcessEventRemoveObject()
  {
    int blockX, blockZ;
    ObjectRecord cur_record;

    blockX = DecodeUnsignedNumber(4);
    blockZ = DecodeUnsignedNumber(4);

    // Remove any record for this object.
    for (int i = 0; i < GameData.instance.objects.Count; i++) {
      cur_record = GameData.instance.objects[i];
      if ((cur_record.blockX == blockX) && (cur_record.blockZ == blockZ)) {
        GameData.instance.objects.RemoveAt(i);
        break;
      }
    }

    // Tell the nation panel that the resources list has been modified.
    NationPanel.instance.ResourcesListModified();
  }

  void ProcessEventMoreMessages()
  {
    bool unread;
    int userID, nationID, deviceID, time, reported;
    string username, nation_name, text, timestamp;

    int type = DecodeUnsignedNumber(1);

    int num_messages = DecodeUnsignedNumber(2);

    for (int i = 0; i < num_messages; i++) {
      unread = (DecodeUnsignedNumber(1) != 0);
      userID = DecodeNumber(5);
      nationID = DecodeNumber(5);
      deviceID = DecodeNumber(5);
      username = DecodeString();
      nation_name = DecodeString();
      text = DecodeString();
      timestamp = DecodeString();
      time = DecodeUnsignedNumber(6);
      reported = DecodeUnsignedNumber(1);

      //Debug.Log("Message " + i + " of " + num_messages + ": " + text);

      // If the message text is a JSON encoded localizable string, decode it.
      if (text.Contains("{\n\"ID\": \"")) text = LocalizableStringFromJSON(text);

      // Add this message to the Messages Panel.
      MessagesPanel.instance.AddMessage(unread, userID, nationID, deviceID, username, nation_name, text, timestamp, time, reported, false);
    }

    // Alert the MessagesPanel that more messages have been received.
    MessagesPanel.instance.MoreMessagesReceived(type, num_messages);
  }

  void ProcessEventAlliances()
  {
    // Read current alliances 
    DecodeNationList(GameData.instance.alliesList);

    // Read incoming alliance invitations
    DecodeNationList(GameData.instance.incomingAllyRequestsList);

    // Read outgoing alliance invitations
    DecodeNationList(GameData.instance.outgoingAllyRequestsList);

    // Read incoming unite invitations
    DecodeUniteRequestList(GameData.instance.incomingUniteRequestsList);

    // Read outgoing unite invitations
    DecodeUniteRequestList(GameData.instance.outgoingUniteRequestsList);

    // Sort the given alliance lists.
    GameData.instance.alliesList.Sort(new AllyDataComparer());
    GameData.instance.incomingAllyRequestsList.Sort(new AllyDataComparer());
    GameData.instance.outgoingAllyRequestsList.Sort(new AllyDataComparer());
    GameData.instance.incomingUniteRequestsList.Sort(new AllyDataComparer());
    GameData.instance.outgoingUniteRequestsList.Sort(new AllyDataComparer());

    // Update the nation panel's alliances lists.
    NationPanel.instance.AlliancesReceived();
  }

  void ProcessEventBuildCount()
  {
    int buildID = DecodeUnsignedNumber(3);
    int count = DecodeUnsignedNumber(3);

    // Set record for this object.
    GameData.instance.builds[buildID] = count;
    //Debug.Log("Updated build count for " + buildID + " to " + count);

    // If it is a shard that has been built, update the raid button.
    if ((buildID == 200) || (buildID == 201) || (buildID == 202)) {
      RaidButton.instance.OnBuildShard();
    }

    // Update the build menu.
    BuildMenu.instance.Refresh();
  }

  void DecodeFootprint(Footprint _footprint)
  {
    _footprint.area = DecodeUnsignedNumber(4);
    _footprint.border_area = DecodeUnsignedNumber(4);
    _footprint.perimeter = DecodeUnsignedNumber(4);
    _footprint.geo_efficiency_base = ((float)DecodeNumber(4) / 1000f);
    _footprint.energy_burn_rate = DecodeUnsignedNumber(4);
    _footprint.manpower = DecodeUnsignedNumber(4);
    _footprint.buy_manpower_day_amount = DecodeUnsignedNumber(6);
  }

  void DecodeNationList(List<AllyData> _list)
  {
    int curNationID;
    String curNationName;
    AllyData curAllyData;

    int list_size = DecodeUnsignedNumber(2);
    _list.Clear();
    for (int i = 0; i < list_size; i++) {
      curNationID = DecodeUnsignedNumber(4);
      curNationName = DecodeString();

      curAllyData = new AllyData();
      curAllyData.ID = curNationID;
      curAllyData.name = curNationName;

      _list.Add(curAllyData);
    }
  }

  void DecodeUniteRequestList(List<AllyData> _list)
  {
    int curNationID, curPaymentOffer;
    String curNationName;
    AllyData curAllyData;

    int list_size = DecodeUnsignedNumber(2);
    _list.Clear();
    for (int i = 0; i < list_size; i++) {
      curNationID = DecodeUnsignedNumber(4);
      curNationName = DecodeString();
      curPaymentOffer = DecodeUnsignedNumber(6);

      curAllyData = new AllyData();
      curAllyData.ID = curNationID;
      curAllyData.name = curNationName;
      curAllyData.paymentOffer = curPaymentOffer;

      _list.Add(curAllyData);
    }
  }

  void ProcessEventBlockUpdate()
  {
    int x = DecodeUnsignedNumber(2);
    int z = DecodeUnsignedNumber(2);
    int terrain = DecodeUnsignedNumber(1);
    int nationID = DecodeNumber(4);

    if (nationID != -1) {
      // Read the nation's data
      ReadNationData(nationID);
    }

    //Debug.Log ("Received block_update for block " + x + "," + z + ", terrain: " + terrain + ", nationID: " + nationID);
    mapView.UpdateBlock(x, z, nationID, false);
  }

  void ProcessEventAwardAvailableAdBonus()
  {
    int delta_amount = DecodeUnsignedNumber(4);
    GameData.instance.adBonusAvailable = DecodeUnsignedNumber(4);
    int type = DecodeUnsignedNumber(1);
    int x = DecodeNumber(5);
    int z = DecodeNumber(5);
    int delay = DecodeUnsignedNumber(1);

    Debug.Log("ProcessEventAwardAvailableAdBonus delta_amount: " + delta_amount + ", total_amount: " + GameData.instance.adBonusAvailable);

    StartCoroutine(ModifyAdBonusAmountAfterDelay(delta_amount, GameData.instance.adBonusAvailable, /*AdBonusButton.AdBonusType.RESOURCE*/(AdBonusButton.AdBonusType)type, /*337, 1138*/x, z, delay));
  }

  void ProcessEventBattleProcess()
  {
    int x = DecodeUnsignedNumber(2);
    int z = DecodeUnsignedNumber(2);
    int nationID = DecodeNumber(4);
    int hit_points_start = DecodeUnsignedNumber(2);
    int hit_points_end = DecodeUnsignedNumber(2);
    int hit_points_full = DecodeUnsignedNumber(2);
    int hit_points_new_cur = DecodeUnsignedNumber(2);
    int hit_points_new_full = DecodeUnsignedNumber(2);
    float hit_points_rate = (float)(DecodeUnsignedNumber(3)) / 100.0f;
    int delay = DecodeUnsignedNumber(1);
    int battle_duration = DecodeUnsignedNumber(1);
    int initiatorUserID = DecodeNumber(5);
    int battle_flags = DecodeUnsignedNumber(2);

    if (nationID != -1) {
      // Read the nation's data
      ReadNationData(nationID);
    }

    // If viewing a raid map, decode defender's current area.
    if (GameData.instance.mapMode == GameData.MapMode.RAID) {
      GameData.instance.raidDefenderArea = DecodeUnsignedNumber(2);
      RaidScoreHeader.instance.OnDefenderAreaUpdate(delay + battle_duration);
    }

    Debug.Log("Received battle_process for block " + x + "," + z + ", nationID: " + nationID + ", delay: " + delay + ", battle_duration: " + battle_duration + ", hit_points_start: " + hit_points_start + ", hit_points_end: " + hit_points_end + ", hit_points_full: " + hit_points_full + ", hit_points_new_full: " + hit_points_new_full + ", hit_points_rate: " + hit_points_rate + ", initiatorUserID: " + initiatorUserID + ", battle_flags: " + battle_flags);
    mapView.InitBattleProcess(x, z, nationID, delay, battle_duration, hit_points_start, hit_points_end, hit_points_full, hit_points_new_cur, hit_points_new_full, hit_points_rate, initiatorUserID, battle_flags);
  }

  void ProcessEventBlockProcess()
  {
    int x = DecodeUnsignedNumber(2);
    int z = DecodeUnsignedNumber(2);
    int nationID = DecodeNumber(4);
    int hit_points_full = DecodeUnsignedNumber(2);
    int hit_points_start = DecodeUnsignedNumber(2);
    float hit_points_rate = (float)(DecodeUnsignedNumber(3)) / 100.0f;
    int delay = DecodeUnsignedNumber(1);
    int process_type = DecodeUnsignedNumber(1);

    if (nationID != -1) {
      // Read the nation's data
      ReadNationData(nationID);
    }

    //Debug.Log("Received block_process for block " + x + "," + z + ", nationID: " + nationID + ", delay: " + delay);
    mapView.InitBlockProcess(x, z, process_type, nationID, delay, hit_points_start, hit_points_full, hit_points_rate);
  }

  void ProcessEventCreatePasswordResult()
  {
    bool success = (DecodeUnsignedNumber(1) != 0);
    String message = DecodeLocalizableString();

    // Update GUI
    CreatePasswordPanel.instance.CreatePasswordResult(success, message);
  }

  void ProcessEventCustomizeNationResult()
  {
    bool success = (DecodeUnsignedNumber(1) != 0);
    String message = DecodeLocalizableString();

    // TESTING
    if (Network.instance.initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false)) {
      Network.instance.LogEvent(Time.time + ": Client ID " + Network.instance.GetClientID(true) + " ProcessEventCustomizeNationResult() success: " + success + ", message: " + message + ".");
    }

    // Update GUI
    NewNationPanel.instance.CustomizeNationResult(success, message);
  }

  void ProcessEventDeleteMapFlag()
  {
    int x = DecodeUnsignedNumber(4);
    int z = DecodeUnsignedNumber(4);

    for (int i = 0; i < GameData.instance.mapFlags.Count; i++) {
      if ((GameData.instance.mapFlags[i].x == x) && (GameData.instance.mapFlags[i].z == z)) {
        // Remove the map flag from the list.
        GameData.instance.mapFlags.RemoveAt(i);

        // Update the Map Panel
        MapPanel.instance.DeleteMapFlag(x, z);

        break;
      }
    }
  }

  void ProcessEventDiscovery()
  {
    int delay = DecodeUnsignedNumber(2);
    int manpower_added = DecodeUnsignedNumber(5);
    int energy_added = DecodeUnsignedNumber(5);
    String target_nation_name = DecodeString();
    int advanceID = DecodeNumber(2);
    int duration = DecodeUnsignedNumber(5);
    int xp = DecodeUnsignedNumber(3);

    StartCoroutine(AnnounceDiscovery(delay, manpower_added, energy_added, target_nation_name, advanceID, duration, xp));
  }

  void ProcessEventCaptureStorage()
  {
    int delay = DecodeUnsignedNumber(2);
    int resource_added = DecodeUnsignedNumber(5);
    String target_nation_name = DecodeString();
    int buildID = DecodeNumber(2);

    StartCoroutine(AnnounceCaptureStorage(delay, resource_added, target_nation_name, buildID));
  }

  void ProcessEventDisplayBattle()
  {
    int duration = DecodeUnsignedNumber(1);
    int x = DecodeNumber(3);
    int z = DecodeNumber(3);
    int manpower_cost = DecodeUnsignedNumber(2);
    int manpower_start = DecodeUnsignedNumber(2);
    int manpower_end = DecodeUnsignedNumber(2);
    int hit_points_max = DecodeUnsignedNumber(2);
    int hit_points_start = DecodeUnsignedNumber(2);
    int hit_points_end = DecodeUnsignedNumber(2);
    float geo_efficiency_base_end = ((float)DecodeNumber(4) / 1000f);
    int attacker_stat = DecodeUnsignedNumber(1);
    int defender_stat = DecodeUnsignedNumber(1);
    int attacker_nation_ID = DecodeNumber(4);
    int defender_nation_ID = DecodeNumber(4);
    int effect_techID = DecodeNumber(3);
    int battle_flags = DecodeUnsignedNumber(2);

    Debug.Log("Received battle event for block " + x + "," + z + ", manpower_cost: " + manpower_cost + ", manpower_start: " + manpower_start + ", manpower_end: " + manpower_end + ", hit_points_max: " + hit_points_max + ", hit_points_start: " + hit_points_start + ", hit_points_end: " + hit_points_end + ", attacker_stat: " + attacker_stat + ", defender_stat: " + defender_stat + ", attacker_nation_ID: " + attacker_nation_ID + ", defender_nation_ID: " + defender_nation_ID + ", battle_flags: " + battle_flags);

    // Record that this user event has occurred
    GameData.instance.UserEventOccurred(GameData.UserEventType.ATTACK);

    // Display the battle.
    mapView.DisplayNewAttack(duration, x, z, manpower_start, manpower_start, manpower_end, hit_points_max, hit_points_start, hit_points_end, attacker_stat, defender_stat, attacker_nation_ID, defender_nation_ID, effect_techID, battle_flags);

    // Play sound for attack, if appropriate.
    Sound.instance.PlaySoundForAttack(x, z);

    // After end of battle, display removal of manpower cost (which may include cost of splash damage in addition to that of main attack).
    StartCoroutine(ChangeManpowerAfterDelay(-manpower_cost, duration));

    // After end of battle, display new geo efficiency value.
    StartCoroutine(UpdateGeoEfficiencyAfterDelay(geo_efficiency_base_end, duration));

    //if (energy_cost > 0)
    //{
    //    // After end of battle, display removal of energy cost.
    //    StartCoroutine(ChangeEnergyAfterDelay(-energy_cost, duration));
    //}
  }

  void ProcessEventDisplayProcess()
  {
    int duration = DecodeUnsignedNumber(1);
    int x = DecodeNumber(3);
    int z = DecodeNumber(3);
    //int energy_delta = DecodeNumber(1);
    int hit_points_max = DecodeUnsignedNumber(2);
    int hit_points_start = DecodeUnsignedNumber(2);
    int hit_points_end = DecodeUnsignedNumber(2);
    float geo_efficiency_base_end = ((float)DecodeNumber(4) / 1000f);
    int process_flags = DecodeUnsignedNumber(1);

    mapView.DisplayNewProcess(duration, x, z, hit_points_max, hit_points_start, hit_points_end, process_flags);

    // Display alert with details about the block's object, if it has been captured.
    StartCoroutine(AlertForBlockAfterDelay(duration, x, z, ((process_flags & (int)(GameData.ProcessFlags.FIRST_CAPTURE)) != 0)));

    // After end of battle, display new geo efficiency value.
    StartCoroutine(UpdateGeoEfficiencyAfterDelay(geo_efficiency_base_end, duration));

    //if (energy_delta != 0)
    //{
    //    // After end of process, display energy delta.
    //    StartCoroutine(ChangeEnergyAfterDelay(energy_delta, duration));
    //}
  }

  void ProcessEventChat()
  {
    int sourceUserID = DecodeUnsignedNumber(5);
    int sourceNationID = DecodeUnsignedNumber(5);
    int sourceDeviceID = DecodeUnsignedNumber(5);
    String sourceUsername = DecodeString();
    String sourceNationName = DecodeString();
    int sourceNationFlags = DecodeUnsignedNumber(4);
    int channelID = DecodeNumber(5);
    String recipientUsername = DecodeString();
    String text = DecodeString();
    String filteredText = DecodeString();
    int mod_level = DecodeNumber(1);

    // Send the chat message info to the chat system.
    chatSystem.ChatMessageReceived(sourceUserID, sourceNationID, sourceDeviceID, sourceUsername, sourceNationName, sourceNationFlags, channelID, recipientUsername, text, filteredText, mod_level);
  }

  void ProcessEventConnectionInfo()
  {
    Dictionary<int, int> map_modified_times = new Dictionary<int, int>();

    GameData.instance.serverID = DecodeNumber(1);

    // Decode the list of map modified times.
    int numMapModifiedTimes = DecodeUnsignedNumber(2);
    for (int i = 0; i < numMapModifiedTimes; i++) {
      int mapID = DecodeUnsignedNumber(2);
      int mapModifiedTime = DecodeUnsignedNumber(6);
      map_modified_times.Add(mapID, mapModifiedTime);
    }

    // Tell the Network that the connection is established; load map images (download them if necessary), and enter game if appropriate.
    Network.instance.ConnectionEstablished(GameData.instance.serverID, map_modified_times);
  }

  void ProcessEventBuyResource()
  {
    int prev_credits = GameData.instance.credits;
    int prev_manpower = GameData.instance.current_footprint.manpower;
    int prev_energy = GameData.instance.energy;

    GameData.instance.credits = DecodeUnsignedNumber(4);
    GameData.instance.credits_transferable = DecodeUnsignedNumber(4);
    GameData.instance.energy = DecodeUnsignedNumber(5);

    bool raidInProgress = (DecodeUnsignedNumber(1) != 0);

    DecodeFootprint(GameData.instance.mainland_footprint);
    DecodeFootprint(GameData.instance.homeland_footprint);

    if (raidInProgress) {
      DecodeFootprint(GameData.instance.raidland_footprint);
    }

    // Update status bars, with animated text.
    GameGUI.instance.UpdateForUpdateBarsEvent(GameData.instance.energy - prev_energy, 0, 0, GameData.instance.current_footprint.manpower - prev_manpower, GameData.instance.credits - prev_credits);

    // Update the raid button as if an update event had been received.
    RaidButton.instance.OnUpdateEvent();
  }

  void ProcessEventChatBan()
  {
    int ban_duration = DecodeUnsignedNumber(5);
    Chat.instance.InitiateChatBan(ban_duration);
  }

  void ProcessEventChatList()
  {
    int nationID = DecodeUnsignedNumber(4);
    string nationName = DecodeString();
    int list_size = DecodeUnsignedNumber(2);

    // Create new chat list
    List<ChatListEntryData> chatList = new List<ChatListEntryData>();

    // Read all entries in the chat list.
    for (int i = 0; i < list_size; i++) {
      int curNationID = DecodeUnsignedNumber(4);
      string curName = DecodeString();

      ChatListEntryData newEntry = new ChatListEntryData(curNationID, curName);
      chatList.Add(newEntry);
    }

    chatSystem.ChatListReceived(nationID, nationName, chatList);
  }

  void ProcessEventChatListAdd()
  {
    int nationID = DecodeUnsignedNumber(4);
    int addedNationID = DecodeUnsignedNumber(4);
    string addedNationName = DecodeString();

    chatSystem.ChatListAddReceived(nationID, addedNationID, addedNationName);
  }

  void ProcessEventChatListRemove()
  {
    int nationID = DecodeUnsignedNumber(4);
    int removedNationID = DecodeUnsignedNumber(4);

    chatSystem.ChatListRemoveReceived(nationID, removedNationID);
  }

  void ProcessEventClearGUI()
  {
    // Close any active game panel.
    GameGUI.instance.CloseActiveGamePanel();
  }

  void ProcessEventCollectAdBonus()
  {
    int delay = DecodeUnsignedNumber(1);
    StartCoroutine(ModifyAdBonusAmountAfterDelay(0, 0, AdBonusButton.AdBonusType.NONE, -1, -1, delay));
  }

  void ProcessEventComplaint()
  {
    int complaintIndex = DecodeNumber(3);
    int complaintCount = DecodeNumber(3);

    if (complaintIndex == -1) {
      return;
    }

    ComplaintData complaintData = new ComplaintData();

    // Decode the complaint's ID
    complaintData.ID = DecodeUnsignedNumber(6);

    // Decode information about the reporter user.
    complaintData.reporter_userID = DecodeUnsignedNumber(5);
    complaintData.reporter_nationID = DecodeUnsignedNumber(5);
    complaintData.reporter_userName = DecodeString();
    complaintData.reporter_email = DecodeString();
    complaintData.reporter_nationName = DecodeString();
    complaintData.reporter_num_complaints_by = DecodeUnsignedNumber(3);
    complaintData.reporter_num_complaints_against = DecodeUnsignedNumber(3);
    complaintData.reporter_num_warnings_sent = DecodeUnsignedNumber(3);
    complaintData.reporter_num_chat_bans = DecodeUnsignedNumber(3);
    complaintData.reporter_num_game_bans = DecodeUnsignedNumber(3);
    complaintData.reporter_game_ban_days = DecodeUnsignedNumber(3);
    complaintData.reporter_chat_ban_days = DecodeUnsignedNumber(3);

    // Decode information about the reported user.
    complaintData.reported_userID = DecodeUnsignedNumber(5);
    complaintData.reported_nationID = DecodeUnsignedNumber(5);
    complaintData.reported_userName = DecodeString();
    complaintData.reported_email = DecodeString();
    complaintData.reported_nationName = DecodeString();
    complaintData.reported_num_complaints_by = DecodeUnsignedNumber(3);
    complaintData.reported_num_complaints_against = DecodeUnsignedNumber(3);
    complaintData.reported_num_warnings_sent = DecodeUnsignedNumber(3);
    complaintData.reported_num_chat_bans = DecodeUnsignedNumber(3);
    complaintData.reported_num_game_bans = DecodeUnsignedNumber(3);
    complaintData.reported_game_ban_days = DecodeUnsignedNumber(3);
    complaintData.reported_chat_ban_days = DecodeUnsignedNumber(3);

    // Decode informaton about the complaint
    complaintData.timestamp = DecodeUnsignedNumber(6);
    complaintData.issue = DecodeString();
    complaintData.text = DecodeString();

    // Have the moderator panel display this complaint.
    ModeratorPanel.instance.DisplayComplaint(complaintIndex, complaintCount, complaintData);
  }

  void ProcessEventCompletion()
  {
    int blockX = DecodeUnsignedNumber(4);
    int blockZ = DecodeUnsignedNumber(4);

    // Tell the map view that this block's object is being completed.
    mapView.CompleteBuildObject(blockX, blockZ);
  }

  void ProcessEventInfo()
  {
    // TESTING
    if (Network.instance.initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false)) {
      Network.instance.LogEvent(Time.time + ": Client ID " + Network.instance.GetClientID(true) + " event_info received; entering game.");
    }

    GameData.instance.player_info = DecodeString();
    int info_flags = DecodeUnsignedNumber(2);
    GameData.instance.userID = DecodeNumber(4);
    GameData.instance.username = DecodeString();
    GameData.instance.email = DecodeString();
    GameData.instance.userCreationTime = DecodeUnsignedNumber(6);
    GameData.instance.nationID = DecodeNumber(4);
    GameData.instance.userFlags = DecodeUnsignedNumber(4);
    GameData.instance.userRank = DecodeUnsignedNumber(1);
    GameData.instance.userPrevLogOutGameTime = DecodeUnsignedNumber(6);
    GameData.instance.patronUsername = DecodeString();
    GameData.instance.adBonusAvailable = DecodeUnsignedNumber(4);
    String tutorial_state = DecodeString();
    GameData.instance.homeNationID = DecodeNumber(4);
    GameData.instance.homeNationName = DecodeString();
    GameData.instance.nationFlags = DecodeUnsignedNumber(4);
    GameData.instance.nationName = DecodeString();
    GameData.instance.nationPassword = DecodeString();
    Debug.Log("ProcessEventInfo() nationPassword: " + GameData.instance.nationPassword);

    float r = ((float)DecodeUnsignedNumber(2)) / 255.0f;
    float g = ((float)DecodeUnsignedNumber(2)) / 255.0f;
    float b = ((float)DecodeUnsignedNumber(2)) / 255.0f;
    GameData.instance.nationColor = new Color(r, g, b);

    GameData.instance.emblemIndex = DecodeNumber(2);
    GameData.instance.emblemColor = (NationData.EmblemColor)(DecodeUnsignedNumber(1));

    GameData.instance.level = DecodeUnsignedNumber(2);
    GameData.instance.xp = DecodeUnsignedNumber(5);
    GameData.instance.level_xp_threshold = DecodeUnsignedNumber(5);
    GameData.instance.next_level_xp_threshold = DecodeUnsignedNumber(5);
    GameData.instance.pending_xp = DecodeUnsignedNumber(5);
    GameData.instance.rebirth_count = DecodeUnsignedNumber(2);
    GameData.instance.rebirth_level_bonus = DecodeUnsignedNumber(2);
    GameData.instance.rebirth_available_level = DecodeUnsignedNumber(2);
    GameData.instance.rebirth_available_time = Time.unscaledTime + DecodeUnsignedNumber(5);
    GameData.instance.rebirth_countdown = DecodeUnsignedNumber(3);
    GameData.instance.rebirth_countdown_start = DecodeUnsignedNumber(3);
    GameData.instance.advance_points = DecodeUnsignedNumber(2);
    GameData.instance.targetAdvanceID = DecodeNumber(3);
    GameData.instance.prizeMoney = DecodeUnsignedNumber(5);
    GameData.instance.prizeMoneyHistory = DecodeUnsignedNumber(5);
    GameData.instance.prizeMoneyHistoryMonthly = DecodeUnsignedNumber(5);
    GameData.instance.map_position_limit = DecodeNumber(3);
    GameData.instance.map_position_limit_next_level = DecodeNumber(3);
    GameData.instance.map_position_eastern_limit = DecodeNumber(3);
    Debug.Log("ProcessEventInfo() level: " + GameData.instance.level + ", xp: " + GameData.instance.xp + ", prizeMoneyHistory: " + GameData.instance.prizeMoneyHistory + ", map_position_limit: " + GameData.instance.map_position_limit + ", map_position_limit_next_level: " + GameData.instance.map_position_limit_next_level);
    Debug.Log("ProcessEventInfo() map_position_eastern_limit: " + GameData.instance.map_position_eastern_limit);

    // Muted user IDs
    Chat.instance.muted_users.Clear();
    int num_muted_users = DecodeUnsignedNumber(3);
    Debug.Log("ProcessEventInfo() num muted users: " + num_muted_users);
    for (int i = 0; i < num_muted_users; i++) {
      Chat.instance.muted_users.Add(DecodeUnsignedNumber(5));
      Debug.Log("Muted userID: " + Chat.instance.muted_users[Chat.instance.muted_users.Count - 1]);
    }

    // Muted device IDs
    Chat.instance.muted_devices.Clear();
    int num_muted_devices = DecodeUnsignedNumber(3);
    Debug.Log("ProcessEventInfo() num_muted_devices: " + num_muted_devices);
    for (int i = 0; i < num_muted_devices; i++) {
      Chat.instance.muted_devices.Add(DecodeUnsignedNumber(5));
      Debug.Log("Muted deviceID: " + Chat.instance.muted_devices[Chat.instance.muted_devices.Count - 1]);
    }

    // Quest records
    GameData.instance.ClearQuestRecords();
    int num_quest_records = DecodeUnsignedNumber(2);
    Debug.Log("ProcessEventInfo() num_quest_records: " + num_quest_records);
    for (int i = 0; i < num_quest_records; i++) {
      int questID = DecodeUnsignedNumber(2);
      QuestRecord quest_record = GameData.instance.GetQuestRecord(questID, true);

      quest_record.ID = questID;
      quest_record.cur_amount = DecodeUnsignedNumber(5);
      quest_record.completed = DecodeUnsignedNumber(1);
      quest_record.collected = DecodeUnsignedNumber(1);
    }

    GameData.instance.gameTimeAtLogin = DecodeUnsignedNumber(6);
    GameData.instance.endOfDayTime = Time.unscaledTime + (float)(DecodeUnsignedNumber(6));
    GameData.instance.nextFreeMigrationTime = Time.unscaledTime + (float)(DecodeUnsignedNumber(6));
    GameData.instance.nextUniteTime = Time.unscaledTime + (float)(DecodeUnsignedNumber(6));
    GameData.instance.modLevel = DecodeNumber(1);
    Debug.Log("ProcessEventInfo() gameTimeAtLogin: " + GameData.instance.gameTimeAtLogin + ", nextFreeMigrationTime: " + GameData.instance.nextFreeMigrationTime + ", modLevel: " + GameData.instance.modLevel);

    DecodeFealtyInfo();

    // Historical extent of nation
    mapView.minX0 = DecodeNumber(3);
    mapView.minZ0 = DecodeNumber(3);
    mapView.maxX1 = DecodeNumber(3);
    mapView.maxZ1 = DecodeNumber(3);

    Debug.Log("ProcessEventInfo() Nation historical extent: " + mapView.minX0 + "," + mapView.minZ0 +  " to " + mapView.maxX1 + "," + mapView.maxZ1);

    // Info about recent energy purchases.
    GameData.instance.buyEnergyDayAmount = DecodeUnsignedNumber(6);

    // Nation quantities
    GameData.instance.energy = DecodeUnsignedNumber(5);
    //GameData.instance.manpower = DecodeUnsignedNumber(5);
    GameData.instance.credits = DecodeUnsignedNumber(4);
    GameData.instance.credits_transferable = DecodeUnsignedNumber(4);

    Debug.Log("ProcessEventInfo() energy: " + GameData.instance.energy + ", credits: " + GameData.instance.credits + ", credits_transferable: " + GameData.instance.credits_transferable);

    // User login report information
    GameData.instance.report__defenses_squares_defeated = DecodeNumber(5);
    GameData.instance.report__defenses_XP = DecodeNumber(5);
    GameData.instance.report__defenses_lost = DecodeNumber(5);
    GameData.instance.report__defenses_built = DecodeNumber(5);
    GameData.instance.report__walls_lost = DecodeNumber(5);
    GameData.instance.report__walls_built = DecodeNumber(5);
    GameData.instance.report__attacks_squares_captured = DecodeNumber(5);
    GameData.instance.report__attacks_XP = DecodeNumber(5);
    GameData.instance.report__levels_gained = DecodeNumber(5);
    GameData.instance.report__orb_count_delta = DecodeNumber(5);
    GameData.instance.report__orb_credits = DecodeNumber(5);
    GameData.instance.report__orb_XP = DecodeNumber(5);
    GameData.instance.report__farming_XP = DecodeNumber(5);
    GameData.instance.report__resource_count_delta = DecodeNumber(5);
    GameData.instance.report__land_lost = DecodeNumber(5);
    GameData.instance.report__energy_begin = DecodeNumber(6) / 100f;
    GameData.instance.report__energy_spent = DecodeNumber(6) / 100f;
    GameData.instance.report__energy_donated = DecodeNumber(6) / 100f;
    GameData.instance.report__energy_lost_to_raids = DecodeNumber(6) / 100f;
    GameData.instance.report__manpower_begin = DecodeNumber(6) / 100f;
    GameData.instance.report__manpower_spent = DecodeNumber(6) / 100f;
    GameData.instance.report__manpower_lost_to_resources = DecodeNumber(6) / 100f;
    GameData.instance.report__manpower_donated = DecodeNumber(6) / 100f;
    GameData.instance.report__manpower_lost_to_raids = DecodeNumber(6) / 100f;
    GameData.instance.report__credits_begin = DecodeNumber(6) / 100f;
    GameData.instance.report__credits_spent = DecodeNumber(6) / 100f;
    GameData.instance.report__patron_XP = DecodeNumber(6) / 100f;
    GameData.instance.report__patron_credits = DecodeNumber(6) / 100f;
    GameData.instance.report__follower_XP = DecodeNumber(6) / 100f;
    GameData.instance.report__follower_credits = DecodeNumber(6) / 100f;
    GameData.instance.report__follower_count = DecodeNumber(3);
    GameData.instance.report__raids_fought = DecodeNumber(3);
    GameData.instance.report__medals_delta = DecodeNumber(3);
    GameData.instance.report__rebirth = DecodeNumber(2);
    GameData.instance.report__home_defense_credits = DecodeNumber(6) / 100f;
    GameData.instance.report__home_defense_xp = DecodeNumber(6) / 100f;
    GameData.instance.report__home_defense_rebirth = DecodeNumber(6) / 100f;

    // Stat initial values
    GameData.instance.initEnergy = DecodeUnsignedNumber(4);
    GameData.instance.initEnergyMax = DecodeUnsignedNumber(4);
    GameData.instance.initEnergyRate = DecodeUnsignedNumber(4);
    GameData.instance.initManpower = DecodeUnsignedNumber(4);
    GameData.instance.initManpowerMax = DecodeUnsignedNumber(4);
    GameData.instance.initManpowerRate = DecodeUnsignedNumber(4);
    GameData.instance.initManpowerPerAttack = DecodeUnsignedNumber(4);
    GameData.instance.initStatTech = DecodeUnsignedNumber(4);
    GameData.instance.initStatBio = DecodeUnsignedNumber(4);
    GameData.instance.initStatPsi = DecodeUnsignedNumber(4);
    GameData.instance.initHitPointBase = DecodeUnsignedNumber(4);
    GameData.instance.initHitPointsRate = DecodeUnsignedNumber(4);
    GameData.instance.initSalvageValue = (float)DecodeUnsignedNumber(4) / 100f;
    GameData.instance.initMaxNumAlliances = DecodeUnsignedNumber(4);
    GameData.instance.initMaxSimultaneousProcesses = DecodeUnsignedNumber(4);

    // Constants
    GameData.instance.timeUntilCrumble = DecodeUnsignedNumber(4);
    GameData.instance.timeUntilFastCrumble = DecodeUnsignedNumber(4);
    GameData.instance.defenseRebuildPeriod = DecodeUnsignedNumber(4);
    GameData.instance.secondsRemainVisible = DecodeUnsignedNumber(4);
    GameData.instance.completionCostPerMinute = DecodeUnsignedNumber(2);
    mapView.extraViewRange = DecodeUnsignedNumber(2);
    mapView.nationMaxExtent = DecodeUnsignedNumber(3);
    GameData.instance.uniteCost = DecodeUnsignedNumber(2);
    GameData.instance.pendingXPPerHour = DecodeUnsignedNumber(5);
    GameData.instance.migrationCost = DecodeUnsignedNumber(2);
    GameData.instance.customizeCost = DecodeUnsignedNumber(2);
    GameData.instance.levelBonusPerRebirth = DecodeUnsignedNumber(2);
    GameData.instance.maxRebirthLevelBonus = DecodeUnsignedNumber(2);
    GameData.instance.rebirthToBaseLevel = DecodeUnsignedNumber(2);
    GameData.instance.maxRebirthCountdownPurchased = DecodeUnsignedNumber(3);
    GameData.instance.allyLevelDiffLimit = DecodeUnsignedNumber(2);
    GameData.instance.supportableAreaBase = DecodeUnsignedNumber(4);
    GameData.instance.supportableAreaPerLevel = DecodeUnsignedNumber(4);
    GameData.instance.geographicEfficiencyMin = DecodeUnsignedNumber(2) / 100f;
    GameData.instance.geographicEfficiencyMax = DecodeUnsignedNumber(2) / 100f;
    GameData.instance.resourceBonusCap = DecodeUnsignedNumber(2) / 100f;
    GameData.instance.manpowerBurnExponent = DecodeUnsignedNumber(2) / 100f;
    GameData.instance.manpowerBurnFractionOfManpowerMax = DecodeUnsignedNumber(2) / 100f;
    GameData.instance.overburnPower = DecodeUnsignedNumber(2) / 100f;
    GameData.instance.maxNumStorageStructures = DecodeUnsignedNumber(2);
    GameData.instance.storageRefillHours = DecodeUnsignedNumber(2);
    GameData.instance.maxAccountsPerPeriod = DecodeUnsignedNumber(2);
    GameData.instance.maxAccountsPeriod = DecodeUnsignedNumber(5);
    GameData.instance.manpowerMaxHomelandFraction = DecodeUnsignedNumber(2) / 100f;
    GameData.instance.manpowerRateHomelandFraction = DecodeUnsignedNumber(2) / 100f;
    GameData.instance.energyRateHomelandFraction = DecodeUnsignedNumber(2) / 100f;
    GameData.instance.energyRateRaidlandFraction = DecodeUnsignedNumber(2) / 100f;
    GameData.instance.supportableAreaHomelandFraction = DecodeUnsignedNumber(2) / 100f;
    GameData.instance.supportableAreaRaidlandFraction = DecodeUnsignedNumber(2) / 100f;
    GameData.instance.minManpowerFractionToStartRaid = DecodeUnsignedNumber(2) / 100f;
    GameData.instance.manpowerFractionCostToRestartRaid = DecodeUnsignedNumber(2) / 100f;
    GameData.instance.raidMedalsPerLeague = DecodeUnsignedNumber(2);
    GameData.instance.newPlayerAreaBoundary = DecodeNumber(3);
    GameData.instance.creditsAllowedBuyPerMonth = DecodeNumber(4);
    GameData.instance.manpowerGenMultiplier = DecodeUnsignedNumber(2) / 100f;
    GameData.instance.incognitoEnergyBurn = DecodeUnsignedNumber(2) / 100f;
    GameData.instance.minIncognitoPeriod = DecodeUnsignedNumber(3);
    GameData.instance.minWinningsToCashOut = DecodeUnsignedNumber(4);
    GameData.instance.creditsPerCentTradedIn = DecodeUnsignedNumber(2);

    // Purchasing manpower and energy
    GameData.instance.buyManpowerBase = (float)DecodeUnsignedNumber(4) / 100f;
    GameData.instance.buyManpowerMult = (float)DecodeUnsignedNumber(4) / 100f;
    GameData.instance.buyManpowerDailyLimit = (float)DecodeUnsignedNumber(4) / 100f;
    GameData.instance.buyManpowerDailyAbsoluteLimit = (float)DecodeUnsignedNumber(4) / 100f;
    GameData.instance.buyManpowerLimitBase = (float)DecodeUnsignedNumber(4) / 100f;
    GameData.instance.buyEnergyBase = (float)DecodeUnsignedNumber(4) / 100f;
    GameData.instance.buyEnergyMult = (float)DecodeUnsignedNumber(4) / 100f;
    GameData.instance.buyEnergyDailyLimit = (float)DecodeUnsignedNumber(4) / 100f;
    GameData.instance.buyEnergyDailyAbsoluteLimit = (float)DecodeUnsignedNumber(4) / 100f;
    GameData.instance.buyEnergyLimitBase = (float)DecodeUnsignedNumber(4) / 100f;

    // Purchasing credits
    GameData.instance.numCreditPackages = DecodeUnsignedNumber(2);
    GameData.instance.buyCreditsAmount = new int[GameData.instance.numCreditPackages];
    GameData.instance.buyCreditsCostUSD = new float[GameData.instance.numCreditPackages];
    for (int i = 0; i < GameData.instance.numCreditPackages; i++) {
      GameData.instance.buyCreditsAmount[i] = DecodeUnsignedNumber(4);
      GameData.instance.buyCreditsCostUSD[i] = (float)DecodeUnsignedNumber(4) / 100f;
    }

    Debug.Log("numCreditPackages: " + GameData.instance.numCreditPackages);

    // Subscription tiers
    GameData.instance.numSubscriptionTiers = DecodeUnsignedNumber(2);
    GameData.instance.subscriptionCostUSD = new float[GameData.instance.numSubscriptionTiers];
    GameData.instance.bonusCreditsPerDay = new int[GameData.instance.numSubscriptionTiers];
    GameData.instance.bonusRebirthPerDay = new int[GameData.instance.numSubscriptionTiers];
    GameData.instance.bonusXPPercentage = new int[GameData.instance.numSubscriptionTiers];
    GameData.instance.bonusManpowerPercentage = new int[GameData.instance.numSubscriptionTiers];
    for (int i = 0; i < GameData.instance.numSubscriptionTiers; i++) {
      GameData.instance.subscriptionCostUSD[i] = (float)DecodeUnsignedNumber(4) / 100f;
      GameData.instance.bonusCreditsPerDay[i] = DecodeUnsignedNumber(4);
      GameData.instance.bonusRebirthPerDay[i] = DecodeUnsignedNumber(4);
      GameData.instance.bonusXPPercentage[i] = DecodeUnsignedNumber(4);
      GameData.instance.bonusManpowerPercentage[i] = DecodeUnsignedNumber(4);
    }

    Debug.Log("numSubscriptionTiers: " + GameData.instance.numSubscriptionTiers);

    Debug.Log("inputBufferPos: " + inputBufferPos + "/" + network.inputBufferLength);

    // Orb payout rates (in cents per day).
    int numOrbTypes = DecodeUnsignedNumber(3);
    Debug.Log("numOrbTypes: " + numOrbTypes);
    for (int i = 0; i < numOrbTypes; i++) {
      Debug.Log("Orb type " + i + " inputBufferPos: " + inputBufferPos + "/" + network.inputBufferLength);
      int orbObjectID = DecodeUnsignedNumber(3);
      int orbPayoutRate = DecodeUnsignedNumber(3);
      GameData.instance.orbPayoutRates[orbObjectID] = orbPayoutRate;
      //Debug.Log("Orb type " + orbObjectID + " pays out " + orbPayoutRate + " cents per day.");
    }

    // Derive data from flags
    GameData.instance.userIsAdmin = ((info_flags & UF_ADMIN) != 0);
    GameData.instance.userIsVeteran = ((info_flags & UF_VETERAN_USER) != 0);
    GameData.instance.firstLogin = ((info_flags & UF_FIRST_LOGIN) != 0);
    GameData.instance.userIsRegistered = ((info_flags & UF_REGISTERED) != 0);
    GameData.instance.adBonusesAllowed = ((info_flags & IF_AD_BONUSES_ALLOWED) != 0);
    GameData.instance.cashOutPrizesAllowed = ((info_flags & IF_CASH_OUT_PRIZES_ALLOWED) != 0);
    GameData.instance.creditPurchasesAllowed = ((info_flags & IF_CREDIT_PURCHASES_ALLOWED) != 0);

    // Record client time upon login.
    GameData.instance.timeAtLogin = Time.unscaledTime;

    // Associate this client with the currently connected game server
    network.AssociateWithConnectedServer();

    Debug.Log("userID: " + GameData.instance.userID + ", username: " + GameData.instance.username + ", nationID: " + GameData.instance.nationID + ", nationName: " + GameData.instance.nationName + ", clientID: " + Network.instance.GetClientID() + ", userIsRegistered: " + GameData.instance.userIsRegistered + ", userFlags: " + GameData.instance.userFlags + ", secondsRemainVisible: " + GameData.instance.secondsRemainVisible);

    // Record that the initial stats event for this login has not yet been received.
    initial_stats_event_received = false;

    // Set the tutorial system's state
    //Debug.Log("Tutorial state: " + tutorial_state);
    Tutorial.instance.LoadState(tutorial_state);

    // Update GameData
    GameData.instance.InfoEventReceived();

    // Update GUI
    GameGUI.instance.InfoEventReceived();

    // Update the UI for the new target advance.
    AdvancesPanel.instance.UpdateForSetTarget();
    AdvanceDetailsPanel.instance.UpdateForSetTarget();

    // TESTING
    if (Network.instance.initialRun && (PlayerPrefs.HasKey("loggedEnterGame") == false)) {
      Network.instance.LogEvent(Time.time + ": Client ID " + Network.instance.GetClientID(true) + " event_info processing complete. We're in.");
    }

    // Log this installation entering the game, if this is its first time.
    Network.instance.LogInstallation(true);
  }

  void ProcessEventAccountInfo()
  {
    int info_flags = DecodeUnsignedNumber(2);
    GameData.instance.userFlags = DecodeUnsignedNumber(4);
    GameData.instance.username = DecodeString();
    GameData.instance.email = DecodeString();
    GameData.instance.nationName = DecodeString();
    GameData.instance.modLevel = DecodeNumber(1);

    // Derive data from flags
    GameData.instance.userIsAdmin = ((info_flags & UF_ADMIN) != 0);
    GameData.instance.firstLogin = ((info_flags & UF_FIRST_LOGIN) != 0);
    GameData.instance.userIsRegistered = ((info_flags & UF_REGISTERED) != 0);
    GameData.instance.adBonusesAllowed = ((info_flags & IF_AD_BONUSES_ALLOWED) != 0);
    GameData.instance.cashOutPrizesAllowed = ((info_flags & IF_CASH_OUT_PRIZES_ALLOWED) != 0);
    GameData.instance.creditPurchasesAllowed = ((info_flags & IF_CREDIT_PURCHASES_ALLOWED) != 0);

    Debug.Log("Account info: username: " + GameData.instance.username + ", nationName: " + GameData.instance.nationName + ", userFlags: " + GameData.instance.userFlags);

    // Update GUI
    GameGUI.instance.AccountInfoEventReceived();
  }

  void ProcessEventAddFollower()
  {
    int userID = DecodeNumber(5);
    String username = DecodeString();

    // Add the follower to the connect panel.
    ConnectPanel.instance.AddFollower(userID, username, 0, 0);
  }

  void ProcessEventAddPatronOffer()
  {
    int userID = DecodeNumber(5);
    String username = DecodeString();
    int bonusXP = DecodeUnsignedNumber(5);
    int bonusCredits = DecodeUnsignedNumber(5);
    int numFollowers = DecodeUnsignedNumber(3);

    // Add the patron offer to the connect panel.
    ConnectPanel.instance.AddPatronOffer(userID, username, bonusXP, bonusCredits, numFollowers);
  }

  void ProcessEventAllBuildCounts()
  {
    // Clear the dictionary of build counts.
    GameData.instance.builds.Clear();

    int num_build_counts = DecodeUnsignedNumber(3);

    for (int i = 0; i < num_build_counts; i++) {
      int buildID = DecodeUnsignedNumber(3);
      int count = DecodeUnsignedNumber(3);

      // Add a record for this object.
      GameData.instance.builds[buildID] = count;
      //Debug.Log("Adding build ID " + buildID + " with count " + count);
    }
  }

  void ProcessEventAllFollowers()
  {
    int userID, duration, bonusXP, bonusCredits;
    String username;
    int num_followers = DecodeUnsignedNumber(3);

    // Clear the list of followers
    ConnectPanel.instance.ClearFollowerListDisplay();

    for (int i = 0; i < num_followers; i++) {
      userID = DecodeNumber(5);
      username = DecodeString();
      duration = DecodeUnsignedNumber(3);
      bonusXP = DecodeUnsignedNumber(5);
      bonusCredits = DecodeUnsignedNumber(5);

      // Add the follower to the connect panel.
      ConnectPanel.instance.AddFollower(userID, username, bonusXP, bonusCredits);
    }
  }

  void ProcessEventAllPatronOffers()
  {
    int userID, numFollowers, bonusXP, bonusCredits;
    String username;
    int num_patron_offers = DecodeUnsignedNumber(2);

    // Clear the connect panel's list of patron offers.
    ConnectPanel.instance.ClearPatronOfferListDisplay();

    for (int i = 0; i < num_patron_offers; i++) {
      userID = DecodeNumber(5);
      username = DecodeString();
      bonusXP = DecodeUnsignedNumber(5);
      bonusCredits = DecodeUnsignedNumber(5);
      numFollowers = DecodeUnsignedNumber(3);

      // Add the patron offer to the connect panel.
      ConnectPanel.instance.AddPatronOffer(userID, username, bonusXP, bonusCredits, numFollowers);
    }
  }

  void ProcessEventChatLog()
  {
    //Debug.Log("ProcessEventChatLog()");
    String text = DecodeLocalizableString();

    // Display message
    GameGUI.instance.LogToChat(text);
  }

  void ProcessEventFealtyInfo()
  {
    DecodeFealtyInfo();
  }

  void DecodeFealtyInfo()
  {
    GameData.instance.fealtyEndTime = Time.unscaledTime + (float)(DecodeUnsignedNumber(6));
    GameData.instance.fealtyNationName = DecodeString();
    GameData.instance.fealtyTournamentEndTime = Time.unscaledTime + (float)(DecodeUnsignedNumber(6));
    GameData.instance.fealtyTournamentNationName = DecodeString();
    GameData.instance.fealtyNumNationsAtTier = DecodeUnsignedNumber(2);
    GameData.instance.fealtyNumNationsInTournament = DecodeUnsignedNumber(2);
    GameData.instance.fealtyRequestPermission = (GameData.instance.fealtyEndTime <= Time.unscaledTime) && ((GameData.instance.fealtyNumNationsAtTier > 1) || (GameData.instance.fealtyNumNationsInTournament > 1));
    Debug.Log("DecodeFealtyInfo() fealtyEndTime: " + GameData.instance.fealtyEndTime + ", fealtyNationName: " + GameData.instance.fealtyNationName + ", fealtyTournamentNationName: " + GameData.instance.fealtyTournamentNationName + ", fealtyNumNationsInTournament: " + GameData.instance.fealtyNumNationsInTournament);
  }

  void ProcessEventJoinNationResult()
  {
    bool success = (DecodeUnsignedNumber(1) != 0);
    String message = DecodeLocalizableString();

    JoinNationPanel.instance.JoinNationResult(success, message);
  }

  void ProcessEventLogInResult()
  {
    bool success = (DecodeUnsignedNumber(1) != 0);
    String message = DecodeLocalizableString();

    // Update GUI
    LogInPanel.instance.LogInResult(success, message);
  }

  void ProcessEventMap()
  {
    int nationID;
    int runningNationID = -1, runningNationIDCount = 0, extendedDataCount = 0, temp;

    BlockData blockData;

    int mapID = DecodeUnsignedNumber(6);
    int sourceMapID = DecodeUnsignedNumber(2);
    int skin = DecodeUnsignedNumber(2);
    int mapDimX = DecodeUnsignedNumber(3);
    int mapDimZ = DecodeUnsignedNumber(3);
    bool replay = (DecodeUnsignedNumber(1) == 1);
    bool pan_view = (DecodeUnsignedNumber(1) == 1);
    int viewX = DecodeNumber(3);
    int viewZ = DecodeNumber(3);
    int x0 = DecodeNumber(3);
    int z0 = DecodeNumber(3);
    int x1 = DecodeNumber(3);
    int z1 = DecodeNumber(3);

    Debug.Log("Map event received for x0 " + x0 + " z0 " + z0 + " x1 " + x1 + " z1 " + z1 + ", viewX: " + viewX + ", viewZ: " + viewZ); // TESTING

    // Prepare the MapView for this update.
    mapView.SetView(mapID, sourceMapID, skin, mapDimX, mapDimZ, viewX, viewZ, pan_view, replay);

    // Read information about nations.
    int numNations = DecodeUnsignedNumber(2);
    for (int i = 0; i < numNations; i++) {
      // Read this nation's ID.
      nationID = DecodeNumber(4);

      // Read the nation's data
      ReadNationData(nationID);
    }

    for (int z = z0; z <= z1; z++) {
      for (int x = x0; x <= x1; x++) {
        // If the current block is within the area that the MapView currently represents data for...
        if (!MapView.instance.BlockOutsideViewData(x, z)) {
          // Update the data for the current block.
          blockData = mapView.blocks[x - mapView.viewDataBlockX0, z - mapView.viewDataBlockZ0];
          blockData.locked_until = 0.0f; // Set this block to be unlocked.

          // Reset other BlockData members.
          blockData.label_nationID = -1;
          blockData.objectID = -1;
          blockData.wipe_end_time = -1;
        }
      }
    }

    // Decompress and record nationIDs.
    for (int z = z0; z <= z1; z++) {
      for (int x = x0; x <= x1; x++) {
        //if ((z >= 126) && (z <= 128)) Debug.Log("At start of x loop for block " + x + "," + z + ": runningNationIDCount: " + runningNationIDCount);

        if (runningNationIDCount == 0) {
          //if ((z >= 126) && (z <= 128)) Debug.Log("here1");
          temp = DecodeNumber(4);
          //Debug.Log("Block " + x + "," + z + ": Decoded temp: " + temp);
          ///*if (z == 128)*/ Debug.Log ("Block " + x + "," + z + ": Nation IDs temp value: " + temp);
          if (temp < -1) {
            runningNationIDCount = -temp;
            runningNationID = DecodeNumber(4);
            //Debug.Log("Decoded runningNationID: " + runningNationID);
            ///*if (z == 128)*/ Debug.Log ("Block " + x + "," + z + ": runningNationIDCount set to " + runningNationIDCount + ", runningNationID set to: " + runningNationID);
          } else {
            runningNationIDCount = 1;
            runningNationID = temp;
            ///*if (z == 128)*/ Debug.Log ("Block " + x + "," + z + ": runningNationIDCount set to " + runningNationIDCount + ", runningNationID set to: " + runningNationID);
          }
        }

        if (!MapView.instance.BlockOutsideViewData(x, z)) {
          mapView.blocks[x - mapView.viewDataBlockX0, z - mapView.viewDataBlockZ0].nationID = runningNationID;
          //if (runningNationID == GameData.instance.nationID) Debug.Log("Block " + x + "," + z + " set to user nation " + runningNationID); // TESTING
        }

        // If this is a replay map, record every block in the blockReplay data.
        if (replay) {
          mapView.blocksReplayOriginal[x, z].nationID = runningNationID;
        }

        //if ((z >= 126) && (z <= 128)) Debug.Log("Before decrementing for block " + x + "," + z + ": runningNationIDCount: " + runningNationIDCount);
        runningNationIDCount--;
        //if ((z >= 126) && (z <= 128)) Debug.Log("Block " + x + "," + z + ": runningNationIDCount: " + runningNationIDCount);
      }
    }

    // Read count of blocks' extended data to be received.
    extendedDataCount = DecodeUnsignedNumber(2);

    // Read blocks' extended data
    for (int i = 0; i < extendedDataCount; i++) {
      ReadBlockExtendedData(replay);
    }

    // If this is a replay map, initialize the replay.
    if (replay) {
      mapView.InitReplay();
    }

    // Alert the MapView of the area of blocks that has been updated.
    mapView.AreaDataUpdateComplete(x0, z0, x1, z1);
  }

  void ReadBlockExtendedData(bool _replay)
  {
    BlockData blockData;
    int blockX, blockZ;
    int objectID, owner_nationID, creation_time, completion_time, invisible_time, capture_time, crumble_time, wipe_nationID, wipe_end_time, wipe_flags;
    float cur_time = Time.time;

    blockX = DecodeUnsignedNumber(4);
    blockZ = DecodeUnsignedNumber(4);

    objectID = DecodeNumber(2);
    owner_nationID = DecodeNumber(4);
    creation_time = DecodeNumber(5);
    completion_time = DecodeNumber(4);
    invisible_time = DecodeNumber(4);
    capture_time = DecodeNumber(5);
    crumble_time = DecodeNumber(4);
    wipe_nationID = DecodeNumber(4);
    wipe_end_time = DecodeNumber(4);
    wipe_flags = DecodeUnsignedNumber(1);

    if (!MapView.instance.BlockOutsideViewData(blockX, blockZ)) {
      // Record block's extended data.
      blockData = mapView.blocks[blockX - mapView.viewDataBlockX0, blockZ - mapView.viewDataBlockZ0];
      blockData.objectID = objectID;
      blockData.owner_nationID = owner_nationID;
      blockData.creation_time = creation_time + cur_time;
      blockData.completion_time = (completion_time == -1f) ? -1f : (completion_time + cur_time);
      blockData.invisible_time = (invisible_time == -1f) ? -1f : (invisible_time + cur_time);
      blockData.capture_time = capture_time + cur_time;
      blockData.crumble_time = (crumble_time == -1f) ? -1f : (crumble_time + cur_time);
      blockData.wipe_nationID = wipe_nationID;
      blockData.wipe_end_time = (wipe_end_time == -1f) ? -1f : (wipe_end_time + cur_time);
      blockData.wipe_flags = wipe_flags;

      // Tell the MapView that the block's extended data has been modified.
      mapView.ModifiedExtendedData(blockX, blockZ);
    }

    // If this is a replay map, record every block in the blockReplay data.
    if (_replay) {
      // Record block's extended data.
      blockData = mapView.blocksReplayOriginal[blockX, blockZ];
      blockData.objectID = objectID;
      blockData.owner_nationID = owner_nationID;
      blockData.creation_time = creation_time + cur_time;
      blockData.completion_time = (completion_time == -1f) ? -1f : (completion_time + cur_time);
      blockData.invisible_time = (invisible_time == -1f) ? -1f : (invisible_time + cur_time);
      blockData.capture_time = capture_time + cur_time;
      blockData.crumble_time = (crumble_time == -1f) ? -1f : (crumble_time + cur_time);
      blockData.wipe_nationID = wipe_nationID;
      blockData.wipe_end_time = (wipe_end_time == -1f) ? -1f : (wipe_end_time + cur_time);
      blockData.wipe_flags = wipe_flags;
    }
  }

  void ProcessEventMapFlags()
  {
    int num_flags = DecodeUnsignedNumber(2);

    // Clear all MapFlagRecords
    GameData.instance.mapFlags.Clear();

    for (int i = 0; i < num_flags; i++) {
      int x = DecodeUnsignedNumber(4);
      int z = DecodeUnsignedNumber(4);
      string text = DecodeString();

      // Add this map flag to the list.
      GameData.instance.mapFlags.Add(new MapFlagRecord(x, z, text));
      //Debug.Log("Added flag " + (i + 1) + "/" + num_flags + ": " + text);
    }

    // Add all flags to the map panel
    MapPanel.instance.MapFlagsReceived();
  }

  void ProcessEventMessage()
  {
    //Debug.Log("ProcessEventMessage()");
    String text = DecodeLocalizableString();

    // Display message
    GameGUI.instance.DisplayMessage(text);
  }

  void ProcessEventMembers()
  {
    int list_size = DecodeUnsignedNumber(2);

    // Create new chat list
    List<MemberEntryData> memberList = new List<MemberEntryData>();

    // Read all entries in the member list.
    for (int i = 0; i < list_size; i++) {
      int curUserID = DecodeUnsignedNumber(4);
      string curUsername = DecodeString();
      int curPoints = DecodeUnsignedNumber(5);
      int curRank = DecodeUnsignedNumber(1);
      bool curAbsentee = (DecodeUnsignedNumber(1) != 0);
      bool curLoggedIn = (DecodeUnsignedNumber(1) != 0);

      MemberEntryData newEntry = new MemberEntryData(curUserID, curUsername, curPoints, curRank, curLoggedIn, curAbsentee);
      memberList.Add(newEntry);

      // If this user's rank has changed, record new rank and update UI.
      if ((curUserID == GameData.instance.userID) && (curRank != GameData.instance.userRank)) {
        GameData.instance.userRank = curRank;
        NationPanel.instance.RankReceived();
      }
    }

    NationPanel.instance.MemberListReceived(memberList);
    CashOutPanel.instance.MemberListReceived(memberList);
  }

  void ProcessEventMigration()
  {
    // Record the new value for nextFreeMigrationTime.
    GameData.instance.nextFreeMigrationTime = Time.unscaledTime + (float)(DecodeUnsignedNumber(6));

    // Have the GUI close all panels, so migration can be viewed.
    GameGUI.instance.CloseActiveGamePanel();
  }

  void ProcessEventNationData()
  {
    // Read the nation's ID.
    int nationID = DecodeNumber(4);

    // Read the nation's data
    ReadNationData(nationID);

    // Update the GUI for nation data received.
    NationPanel.instance.NationDataReceived(nationID);

    // Queue for update any patches that contain this nation.
    MapView.instance.NationDataReceived(nationID);
  }

  void ProcessEventNationAreas()
  {
    int num_areas = DecodeUnsignedNumber(2);

    // Clear the list of nation areas.
    GameData.instance.nationAreas.Clear();

    // Iterate through each of this nation's areas...
    NationArea cur_area;
    bool visible;
    for (int i = 0; i < num_areas; i++) {
      cur_area = new NationArea();

      cur_area.x = DecodeUnsignedNumber(3);
      cur_area.y = DecodeUnsignedNumber(3);
      visible = (DecodeUnsignedNumber(1) == 1);

      Debug.Log("ProcessEventNationAreas() nation area at: " + cur_area.x + "," + cur_area.y + " " + (visible ? "visible" : ""));

      if (visible) {
        GameData.instance.nationAreas.Add(cur_area);
      }
    }

    // Update the map panel.
    MapPanel.instance.NationAreasReceived();
  }

  void ProcessEventNationFlags()
  {
    int flags = DecodeUnsignedNumber(4);
    GameData.instance.nationFlags = flags;
  }

  void ProcessEventNationInfo()
  {
    int i;

    int infoNationID = DecodeUnsignedNumber(5);
    string name = DecodeString();
    int level = DecodeUnsignedNumber(2);
    int area = DecodeUnsignedNumber(4);
    int trophies = DecodeNumber(5);
    float geo_eff = DecodeUnsignedNumber(3) / 1000f;
    int stat_tech = DecodeUnsignedNumber(2);
    int stat_bio = DecodeUnsignedNumber(2);
    int stat_psi = DecodeUnsignedNumber(2);
    int attacker_stat = DecodeUnsignedNumber(1);
    int defender_stat = DecodeUnsignedNumber(1);

    int num_alliances = DecodeUnsignedNumber(1);

    int[] ally_ids = new int[num_alliances];
    string[] ally_names = new string[num_alliances];

    for (i = 0; i < num_alliances; i++) {
      ally_ids[i] = DecodeUnsignedNumber(5);
      ally_names[i] = DecodeString();
    }

    int num_members = DecodeUnsignedNumber(2);

    string[] member_names = new string[num_members];
    bool[] member_logged_in = new bool[num_members];
    int[] member_rank = new int[num_members];

    for (i = 0; i < num_members; i++) {
      member_names[i] = DecodeString();
      member_logged_in[i] = (DecodeUnsignedNumber(1) != 0);
      member_rank[i] = DecodeUnsignedNumber(2);
    }

    GameGUI.instance.OpenNationInfoDialog(infoNationID, name, level, area, trophies, geo_eff, stat_tech, stat_bio, stat_psi, num_alliances, ally_ids, ally_names, num_members, member_names, member_logged_in, member_rank, attacker_stat, defender_stat);
  }

  void ProcessEventNewMessage()
  {
    int delay = DecodeUnsignedNumber(1);
    bool unread = (DecodeUnsignedNumber(1) != 0);
    int userID = DecodeNumber(5);
    int nationID = DecodeNumber(5);
    int deviceID = DecodeNumber(5);
    string username = DecodeString();
    string nation_name = DecodeString();
    string text = DecodeString();
    string timestamp = DecodeString();
    int time = DecodeUnsignedNumber(6);
    int reported = DecodeUnsignedNumber(1);

    //Debug.Log("message text pre localize:" + text);

    // If the message text is a JSON encoded localizable string, decode it.
    if (text.Contains("{\n\"ID\": \"")) text = LocalizableStringFromJSON(text);

    Debug.Log("message delay: " + delay + ", text post localize:" + text);

    // Add the new message to the Messages Panel.
    StartCoroutine(AddMessageAfterDelay(delay, unread, userID, nationID, deviceID, username, nation_name, text, timestamp, time, reported, true));
  }

  void ProcessEventNationOrbs()
  {
    int i, count;
    NationOrbRecord record;

    // Orb winnings history, all-time and monthly.
    GameData.instance.orb_winnings_history = DecodeUnsignedNumber(5);
    GameData.instance.orb_winnings_history_monthly = DecodeUnsignedNumber(5);

    // Clear the lists of nation orb records
    GameData.instance.nationOrbsList.Clear();
    GameData.instance.nationOrbsMonthlyList.Clear();

    // Read the number of monthly orb records
    count = DecodeUnsignedNumber(2);

    // Read monthly orb records.
    for (i = 0; i < count; i++) {
      record = new NationOrbRecord();
      record.x = DecodeUnsignedNumber(4);
      record.z = DecodeUnsignedNumber(4);
      record.objectID = DecodeNumber(3);
      record.winnings = DecodeUnsignedNumber(5);
      record.currentlyOccupied = (DecodeUnsignedNumber(1) != 0);
      GameData.instance.nationOrbsMonthlyList.Add(record);
    }

    // Sort the list of monthly orb records by the amount of winnings.
    GameData.instance.nationOrbsMonthlyList.Sort(delegate (NationOrbRecord _record1, NationOrbRecord _record2)
    {
      return (_record1.winnings > _record2.winnings) ? -1 : ((_record1.winnings < _record2.winnings) ? 1 : 0);
    });


    // Read the number of all-time orb records
    count = DecodeUnsignedNumber(2);

    // Read all-time orb records.
    for (i = 0; i < count; i++) {
      record = new NationOrbRecord();
      record.x = DecodeUnsignedNumber(4);
      record.z = DecodeUnsignedNumber(4);
      record.objectID = DecodeNumber(3);
      record.winnings = DecodeUnsignedNumber(5);
      record.currentlyOccupied = (DecodeUnsignedNumber(1) != 0);
      GameData.instance.nationOrbsList.Add(record);
    }

    // Sort the list of all-time orb records by the amount of winnings.
    GameData.instance.nationOrbsList.Sort(delegate (NationOrbRecord _record1, NationOrbRecord _record2)
    {
      return (_record1.winnings > _record2.winnings) ? -1 : ((_record1.winnings < _record2.winnings) ? 1 : 0);
    });

    // Have the Connect Panel display the new list of nation orbs.
    NationPanel.instance.NationOrbsReceived();
  }

  void ProcessEventNewPlayerResult()
  {
    bool success = (DecodeUnsignedNumber(1) != 0);
    String message = DecodeLocalizableString();

    // Update GUI
    NewPlayerPanel.instance.NewPlayerResult(success, message);
  }

  void ProcessEventNoAssociatedPlayer()
  {
    // This client has no associated player account on the associated game server. Display the welcome panel.
    GameGUI.instance.OpenWelcomePanel();
  }

  void ProcessEventPanView()
  {
    int viewX = DecodeNumber(3);
    int viewZ = DecodeNumber(3);

    mapView.LocalPanView(viewX, viewZ);
  }

  void ProcessEventPurchaseComplete()
  {
    int package = DecodeNumber(1);
    float amount = (float)DecodeUnsignedNumber(4) / 100f;
    String currency = DecodeString();

    // Record this purchase with Unity Analytics.
    Analytics.Transaction((package < 0) ? "Reward package" : ((GameGUI.instance.buyCreditsPackageName.Length > package) ? GameGUI.instance.buyCreditsPackageName[package] : ("Unknown package " + package)), (decimal)amount, currency, null, null);
  }

  void ProcessEventRemoveFollower()
  {
    int userID = DecodeNumber(5);

    // Remove the folower from the Connect panel
    ConnectPanel.instance.RemoveFollower(userID);
  }

  void ProcessEventRemovePatronOffer()
  {
    int userID = DecodeNumber(5);

    // Remove the patron offer from the Connect panel
    ConnectPanel.instance.RemovePatronOffer(userID);
  }

  void ProcessEventPatronInfo()
  {
    GameData.instance.patronCode = DecodeString();
    GameData.instance.prev_month_patron_bonus_XP = DecodeUnsignedNumber(5);
    GameData.instance.prev_month_patron_bonus_credits = DecodeUnsignedNumber(5);
    GameData.instance.patronID = DecodeNumber(5);

    if (GameData.instance.patronID == -1) {
      GameData.instance.patronUsername = "";
      GameData.instance.total_patron_xp_received = 0;
      GameData.instance.total_patron_credits_received = 0;
    } else {
      GameData.instance.patronUsername = DecodeString();
      GameData.instance.patron_prev_month_patron_bonus_XP = DecodeUnsignedNumber(5);
      GameData.instance.patron_prev_month_patron_bonus_credits = DecodeUnsignedNumber(5);
      GameData.instance.patron_num_followers = DecodeUnsignedNumber(3);
      GameData.instance.total_patron_xp_received = DecodeUnsignedNumber(5);
      GameData.instance.total_patron_credits_received = DecodeUnsignedNumber(5);
    }

    // Update the Connect panel
    ConnectPanel.instance.PatronInfoReceived();
  }

  void ProcessEventPostMessageResult()
  {
    bool success = (DecodeUnsignedNumber(1) != 0);
    string error_message = DecodeLocalizableString();
    PostMessagePanel.instance.PostMessageResult(success, error_message);
  }

  void ProcessEventNationNameAvailable()
  {
    bool isAvailable = (DecodeUnsignedNumber(1) != 0);
    bool isSet = (DecodeUnsignedNumber(1) != 0);
    //CustomizeNationPanel.instance.NationNameAvailable(isAvailable, isSet);
  }

  void ProcessEventNationPassword()
  {
    GameData.instance.nationPassword = DecodeString();
    NationPanel.instance.InfoEventReceived();
  }

  void ProcessEventOrbWinnings()
  {
    int contactNationID;
    OrbRanksRecord orb_ranks_record;

    GameData.instance.nation_orb_winnings = DecodeUnsignedNumber(5);
    GameData.instance.nation_orb_winnings_monthly = DecodeUnsignedNumber(5);

    // Clear the list of contacts' orb ranks.
    GameData.instance.contactOrbRanks.Clear();

    for (; ; )
    {
      contactNationID = DecodeNumber(5);

      // Exit loop if we've received the terminator.
      if (contactNationID == -1) {
        break;
      }

      // Create a record for this contact nation's rank for this orb, and add it to the contactOrbRanks list.
      orb_ranks_record = new OrbRanksRecord();
      orb_ranks_record.nationID = contactNationID;
      orb_ranks_record.nation_name = DecodeString();
      orb_ranks_record.orb_winnings = DecodeUnsignedNumber(5);
      orb_ranks_record.orb_winnings_monthly = DecodeUnsignedNumber(5);
      GameData.instance.contactOrbRanks.Add(orb_ranks_record);
    }

    ConnectPanel.instance.OrbWinningsEventReceived();
  }

  void ProcessEventQuestStatus()
  {
    int questID = DecodeUnsignedNumber(2);
    int cur_amount = DecodeUnsignedNumber(5);
    int completed = DecodeUnsignedNumber(1);
    int collected = DecodeUnsignedNumber(1);
    int delay = DecodeUnsignedNumber(1);

    // Get and update the QuestRecord with the given ID.
    QuestRecord questRecord = GameData.instance.GetQuestRecord(questID, true);
    questRecord.SetStatus(cur_amount, completed, collected);

    // Update the quest status after the given delay.
    StartCoroutine(UpdateQuestStatusAfterDelay(questRecord, delay));
  }

  void ProcessEventRaidLogEntry()
  {
    // Determine whether this is an attack or defense log entry.
    bool attack = (DecodeUnsignedNumber(1) != 0);

    // Decode this raid log entry, and add it to the appropriate log list.
    RaidLogRecord record = DecodeRaidLogEntry(attack ? GameData.instance.raidAttackLog : GameData.instance.raidDefenseLog);

    // Update the raid panel
    if (record != null) {
      RaidPanel.instance.AddToRaidEntryList(record, attack);
    }
  }

  void ProcessEventRaidLogs()
  {
    // Clear the lists of raid log entries.
    GameData.instance.raidDefenseLog.Clear();
    GameData.instance.raidAttackLog.Clear();

    // Decode raid defense log entries.
    int num_defense_entries = DecodeUnsignedNumber(2);
    for (int i = 0; i < num_defense_entries; i++) {
      DecodeRaidLogEntry(GameData.instance.raidDefenseLog);
    }

    // Decode raid attack log entries.
    int num_attack_entries = DecodeUnsignedNumber(2);
    for (int i = 0; i < num_attack_entries; i++) {
      DecodeRaidLogEntry(GameData.instance.raidAttackLog);
    }

    // Update the raid panel.
    RaidPanel.instance.OnEventRaidLogs();
  }

  public RaidLogRecord DecodeRaidLogEntry(List<RaidLogRecord> _log)
  {
    // Determine whether this entry is valid (not obsolete).
    bool valid = (DecodeUnsignedNumber(1) != 0);

    if (!valid) {
      return null;
    }

    RaidLogRecord record = new RaidLogRecord();

    record.raidID = DecodeUnsignedNumber(5);
    record.enemyNationID = DecodeUnsignedNumber(5);
    record.enemyNationName = DecodeString();
    record.enemyNationMedals = DecodeUnsignedNumber(3);
    record.flags = DecodeUnsignedNumber(2);
    record.startTime = Time.unscaledTime - DecodeUnsignedNumber(6);
    record.percentageDefeated = DecodeUnsignedNumber(2);
    record.rewardMedals = DecodeNumber(2);
    record.rewardCredits = DecodeUnsignedNumber(2);
    record.rewardXP = DecodeUnsignedNumber(4);
    record.rewardRebirth = DecodeUnsignedNumber(2);

    Debug.Log("Raid log entry startTime: " + record.startTime);

    // Add the record to the given list.
    _log.Add(record);

    return record;
  }

  void ProcessEventRaidReplay()
  {
    // Decode raid info
    GameData.instance.raidAttackerNationID = DecodeNumber(5);
    GameData.instance.raidDefenderNationID = DecodeNumber(5);
    GameData.instance.raidDefenderStartingArea = DecodeUnsignedNumber(2);
    GameData.instance.raidDefenderArea = GameData.instance.raidDefenderStartingArea;

    // Read the attacker and defender nation data (but do not overwrite any existing data for these two nations). 
    NationData attackerNationData = ReadNationData(GameData.instance.raidAttackerNationID, false, false);
    NationData defenderNationData = ReadNationData(GameData.instance.raidDefenderNationID, false, false);

    // Record both the attacker and defender nation names.
    GameData.instance.raidAttackerNationName = attackerNationData.GetName(false);
    GameData.instance.raidDefenderNationName = defenderNationData.GetName(false);

    // Initialize replay.
    GameData.instance.replayCurTime = -3f;
    GameData.instance.replayEndTime = 0f;
    GameData.instance.replayEventIndex = 0;
    GameData.instance.raidFlags = 0;

    // Clear the replay list.
    foreach (ReplayEventRecord rec in GameData.instance.replayList) rec.targets.Clear();
    GameData.instance.replayList.Clear();

    ReplayEventRecord.Event eventID;
    ReplayEventRecord record;

    for (; ; )
    {
      eventID = (ReplayEventRecord.Event)DecodeUnsignedNumber(1);

      // If end event received (or we've reached the end of the input buffer, due to error), exit loop.
      if ((eventID == ReplayEventRecord.Event.END) || (inputBufferPos >= Network.instance.inputBufferLength)) {
        break;
      }

      // Create new replay event record, read eventID and timestamp, and add it to the replayList.
      record = new ReplayEventRecord();
      record.eventID = eventID;
      record.timestamp = ((float)DecodeUnsignedNumber(5) / 1000f);
      record.x = DecodeUnsignedNumber(2);
      record.z = DecodeUnsignedNumber(2);
      GameData.instance.replayList.Add(record);

      // Update the replay's end time to last event.
      GameData.instance.replayEndTime = Math.Max(GameData.instance.replayEndTime, record.timestamp);

      if (eventID == ReplayEventRecord.Event.SET_NATION_ID) {
        record.subjectID = DecodeUnsignedNumber(5);
      } else if (eventID == ReplayEventRecord.Event.BATTLE) {
        record.subjectID = DecodeUnsignedNumber(5);
        record.battle_flags = DecodeUnsignedNumber(2);
      } else if (eventID == ReplayEventRecord.Event.SET_OBJECT_ID) {
        record.subjectID = DecodeUnsignedNumber(5);
      } else if (eventID == ReplayEventRecord.Event.TOWER_ACTION) {
        TargetRecord cur_target;
        float cur_time = Time.time;

        record.subjectID = DecodeNumber(2);
        record.build_type = DecodeUnsignedNumber(1);
        record.invisible_time = DecodeNumber(4);
        record.duration = DecodeUnsignedNumber(1);
        record.trigger_x = DecodeUnsignedNumber(2);
        record.trigger_z = DecodeUnsignedNumber(2);
        record.triggerNationID = DecodeUnsignedNumber(5);

        int num_targets = DecodeUnsignedNumber(2);
        for (int i = 0; i < num_targets; i++) {
          cur_target = new TargetRecord();
          cur_target.x = DecodeUnsignedNumber(2);
          cur_target.y = DecodeUnsignedNumber(2);
          cur_target.newNationID = DecodeNumber(5);
          cur_target.full_hit_points = 1;
          cur_target.start_hit_points = 1;
          cur_target.end_hit_points = DecodeUnsignedNumber(1);
          cur_target.new_cur_hit_points = 1;
          cur_target.new_full_hit_points = 1;
          cur_target.hit_points_rate = 1;
          cur_target.battle_flags = DecodeUnsignedNumber(2);
          cur_target.wipe_end_time = DecodeNumber(4);
          cur_target.wipe_flags = DecodeUnsignedNumber(1);
          record.targets.Add(cur_target);
        }
      } else if (eventID == ReplayEventRecord.Event.EXT_DATA) {
        record.subjectID = DecodeNumber(2);
        record.owner_nationID = DecodeNumber(5);
        record.creation_time = DecodeNumber(5);
        record.completion_time = DecodeNumber(4);
        record.invisible_time = DecodeNumber(4);
        record.capture_time = DecodeNumber(5);
        record.crumble_time = DecodeNumber(4);
        record.wipe_nationID = DecodeNumber(5);
        record.wipe_end_time = DecodeNumber(4);
        record.wipe_flags = DecodeNumber(1);
      }

      //Debug.Log("Replay event: " + record.eventID + ", x: " + record.x + ", z: " + record.z + ", subjectID: " + record.subjectID);
    }

    Debug.Log("Raid replay event end time: " + GameData.instance.replayEndTime + ", num events: " + GameData.instance.replayList.Count + ", buffer len: " + network.inputBufferLength);

    // Update the RaidScoreHeader for the start of the replay.
    RaidScoreHeader.instance.OnReplayBegin();

    // Update the replay controls for the start of the replay.
    ReplayControls.instance.OnReplayBegin();

    // Close the active game panel.
    GameGUI.instance.CloseActiveGamePanel();
  }

  void ProcessEventRaidStatus()
  {
    // Record previous state of raid flags.
    int prevRaidFlags = GameData.instance.raidFlags;

    int delay = DecodeUnsignedNumber(2);

    GameData.instance.raidDefenderNationID = DecodeNumber(5);

    if (GameData.instance.raidDefenderNationID != -1) {
      // Decode raid info.
      GameData.instance.raidDefenderNationName = DecodeString();
      GameData.instance.raidDefenderStartingArea = DecodeUnsignedNumber(2);
      GameData.instance.raidDefenderArea = DecodeUnsignedNumber(2);
      GameData.instance.raidAttackerNationNumMedals = DecodeUnsignedNumber(3);
      GameData.instance.raidDefenderNationNumMedals = DecodeUnsignedNumber(3);
      GameData.instance.raidFlags = DecodeUnsignedNumber(2);
      GameData.instance.raidEndTime = Time.unscaledTime + DecodeUnsignedNumber(6);

      if ((GameData.instance.raidFlags & (int)GameData.RaidFlags.BEGUN) == 0) {
        GameData.instance.raid0StarMedalDelta = DecodeNumber(2);
        GameData.instance.raid5StarMedalDelta = DecodeNumber(2);
        GameData.instance.raidMaxRewardCredits = DecodeUnsignedNumber(2);
        GameData.instance.raidMaxRewardXP = DecodeUnsignedNumber(4);
        GameData.instance.raidMaxRewardRebirth = DecodeUnsignedNumber(2);
        GameData.instance.raidReviewEndTime = Time.unscaledTime + DecodeUnsignedNumber(2);
      }

      if ((GameData.instance.raidFlags & (int)GameData.RaidFlags.FINISHED) != 0) {
        GameData.instance.raidPercentageDefeated = DecodeUnsignedNumber(2);
        GameData.instance.raidAttackerRewardMedals = DecodeNumber(2);
        GameData.instance.raidDefenderRewardMedals = DecodeNumber(2);
        GameData.instance.raidRewardCredits = DecodeUnsignedNumber(2);
        GameData.instance.raidRewardXP = DecodeUnsignedNumber(4);
        GameData.instance.raidRewardRebirth = DecodeUnsignedNumber(2);
      }
    } else {
      // Clear raid info.
      GameData.instance.raidDefenderNationName = "";
      GameData.instance.raidDefenderStartingArea = 0;
      GameData.instance.raidDefenderNationNumMedals = 0;
      GameData.instance.raidFlags = 0;
      GameData.instance.raidEndTime = 0f;
    }

    // Update the raid button.
    RaidButton.instance.OnRaidStatusEvent(delay);

    // Update the raid intro header.
    RaidIntroHeader.instance.OnRaidStatusEvent(delay);

    // Update the raid score header.
    RaidScoreHeader.instance.OnRaidStatusEvent(delay);

    if (((prevRaidFlags & (int)GameData.RaidFlags.FINISHED) == 0) &&
        ((GameData.instance.raidFlags & (int)GameData.RaidFlags.FINISHED) != 0)) {
      StartCoroutine(GameGUI.instance.OnRaidFinished(delay));
    }
  }

  void ProcessEventRanksData()
  {
    DecodeUserRanks(GameData.instance.userRanks);
    DecodeNationRanks(GameData.instance.nationRanks);

    // Clear the lists of contact user and nation ranks data.
    GameData.instance.contactUserRanks.Clear();
    GameData.instance.contactNationRanks.Clear();

    // Decode the number of active contacts
    int num_active_contacts = DecodeUnsignedNumber(2);

    // Read all contacts' ranks.
    for (int i = 0; i < num_active_contacts; i++) {
      UserRanksRecord user_ranks_record = new UserRanksRecord();
      DecodeUserRanks(user_ranks_record);
      GameData.instance.contactUserRanks.Add(user_ranks_record);
    }

    // Decode the number of active contacts' nations
    int num_active_contacts_nations = DecodeUnsignedNumber(2);

    // Read all contacts nations' ranks.
    for (int i = 0; i < num_active_contacts_nations; i++) {
      NationRanksRecord nation_ranks_record = new NationRanksRecord();
      DecodeNationRanks(nation_ranks_record);
      GameData.instance.contactNationRanks.Add(nation_ranks_record);
    }

    // Alert the ConnectPanel and TournamentPanel that the ranks data has been received.
    ConnectPanel.instance.RanksDataEventReceived();
    TournamentPanel.instance.RanksDataEventReceived();
  }

  void DecodeUserRanks(UserRanksRecord _userRanks)
  {
    _userRanks.userID = DecodeNumber(5);
    _userRanks.username = DecodeString();
    _userRanks.user_xp = DecodeUnsignedNumber(5);
    _userRanks.user_xp_monthly = DecodeUnsignedNumber(5);
    _userRanks.user_followers = DecodeUnsignedNumber(3);
    _userRanks.user_followers_monthly = DecodeUnsignedNumber(3);
  }

  void DecodeNationRanks(NationRanksRecord _nationRanks)
  {
    _nationRanks.nationID = DecodeNumber(5);
    _nationRanks.nation_name = DecodeString();
    _nationRanks.level_history = DecodeUnsignedNumber(2);
    _nationRanks.rebirth_count = DecodeUnsignedNumber(2);
    _nationRanks.nation_xp_history = DecodeUnsignedNumber(5);
    _nationRanks.nation_xp_monthly = DecodeUnsignedNumber(5);
    _nationRanks.prize_money_history = DecodeUnsignedNumber(5);
    _nationRanks.prize_money_history_monthly = DecodeUnsignedNumber(5);
    _nationRanks.raid_earnings_history = DecodeUnsignedNumber(5);
    _nationRanks.raid_earnings_history_monthly = DecodeUnsignedNumber(5);
    _nationRanks.orb_shard_earnings_history = DecodeUnsignedNumber(5);
    _nationRanks.orb_shard_earnings_history_monthly = DecodeUnsignedNumber(5);
    _nationRanks.medals_history = DecodeUnsignedNumber(3);
    _nationRanks.medals_history_monthly = DecodeUnsignedNumber(3);
    _nationRanks.quests_completed = DecodeUnsignedNumber(2);
    _nationRanks.quests_completed_monthly = DecodeUnsignedNumber(2);
    _nationRanks.tournament_trophies_history = DecodeUnsignedNumber(5);
    _nationRanks.tournament_trophies_history_monthly = DecodeUnsignedNumber(5);
    _nationRanks.donated_energy_history = DecodeUnsignedNumber(5);
    _nationRanks.donated_energy_history_monthly = DecodeUnsignedNumber(5);
    _nationRanks.donated_manpower_history = DecodeUnsignedNumber(5);
    _nationRanks.donated_manpower_history_monthly = DecodeUnsignedNumber(5);
    _nationRanks.captures_history = DecodeUnsignedNumber(5);
    _nationRanks.captures_history_monthly = DecodeUnsignedNumber(5);
    _nationRanks.max_area = DecodeUnsignedNumber(4);
    _nationRanks.max_area_monthly = DecodeUnsignedNumber(4);
    _nationRanks.tournament_current = DecodeUnsignedNumber(4);
  }

  void ProcessEventRemoveTechnology()
  {
    int techID = DecodeUnsignedNumber(2);

    // Decrement count associated with this techID.
    GameData.instance.SetTechCount(techID, GameData.instance.GetTechCount(techID) - 1);

    // If temp advance is expiring, play sound, display message and remove from Temps List.
    if (GameData.instance.tempTechExpireTime.ContainsKey(techID)) {
      // Play sound.
      Sound.instance.Play2D(Sound.instance.advance_temp_expire);

      // Display time-out message.
      TechData techData = TechData.GetTechData(techID);
      GameGUI.instance.DisplayMessage(techData.name + " has timed out.");

      // Remove this temp tech from the Temps List
      GameGUI.instance.tempsList.RemoveTechnology(techID);
    }

    // Remove this tech from the tempTechExpireTime table, if it's in there.
    GameData.instance.tempTechExpireTime.Remove(techID);

    // Update the AdvancesPanel for the modified list of technologies.
    AdvancesPanel.instance.UpdateForTechnologies();

    // Update the BuildMenu for the modified list of technologies.
    BuildMenu.instance.UpdateForTechnologies();
  }

  void ProcessEventRequestPing()
  {
    // Return a ping to the server
    network.SendCommand("action=ping");
  }

  void ProcessEventRequestor()
  {
    String text = DecodeLocalizableString();

    // Display requestor with given text.
    Requestor.Activate(0, 0, null, text, LocalizationManager.GetTranslation("Generic Text/okay"), "");
  }

  void ProcessEventRequestorDuration()
  {
    String text = DecodeLocalizableString();

    int duration = DecodeUnsignedNumber(6);

    // Replace {duration} with the text representing the given duration.
    text = text.Replace("{duration}", GameData.instance.GetDurationText(duration));

    // Display requestor with given text.
    Requestor.Activate(0, 0, null, text, LocalizationManager.GetTranslation("Generic Text/okay"), "");
  }

  void ProcessEventReport()
  {
    String text = DecodeString();
    // TODO: Display report
  }

  void ProcessEventSalvage()
  {
    int blockX = DecodeUnsignedNumber(4);
    int blockZ = DecodeUnsignedNumber(4);

    // Tell the map view that this block's object is being salvaged.
    mapView.SalvageBuildObject(blockX, blockZ);
  }

  void ProcessEventSetLevel()
  {
    int old_fealty_tier = GameData.instance.DetermineFealtyTier(GameData.instance.level);

    GameData.instance.level = DecodeUnsignedNumber(2);
    GameData.instance.level_xp_threshold = DecodeUnsignedNumber(5);
    GameData.instance.next_level_xp_threshold = DecodeUnsignedNumber(5);
    GameData.instance.xp = DecodeUnsignedNumber(5);
    GameData.instance.rebirth_count = DecodeUnsignedNumber(2);
    GameData.instance.rebirth_level_bonus = DecodeUnsignedNumber(2);
    GameData.instance.rebirth_available_level = DecodeUnsignedNumber(2);
    GameData.instance.advance_points = DecodeUnsignedNumber(2);
    GameData.instance.credits = DecodeUnsignedNumber(4);
    GameData.instance.map_position_limit = DecodeNumber(3);
    GameData.instance.map_position_limit_next_level = DecodeNumber(3);
    GameData.instance.map_position_eastern_limit = DecodeNumber(3);
    int delay = DecodeUnsignedNumber(1);

    Debug.Log("Set Level event, level: " + GameData.instance.level + ", advance_points: " + GameData.instance.advance_points + ", credits: " + GameData.instance.credits);

    // Start coroutine to display the change after the given delay.
    StartCoroutine(ChangeLevelAfterDelay(delay));

    // If the nation's fealty tier has changed, request updated fealty info.
    int new_fealty_tier = GameData.instance.DetermineFealtyTier(GameData.instance.level);
    if (old_fealty_tier != new_fealty_tier) {
      network.SendCommand("action=request_fealty_info");
    }
  }

  void ProcessEventSetMapFlag()
  {
    int x = DecodeUnsignedNumber(4);
    int z = DecodeUnsignedNumber(4);
    string text = DecodeString();

    // Add this map flag to the list.
    MapFlagRecord mapFlagRecord = GameData.instance.SetMapFlag(x, z, text);

    // Sort the list of map flags.
    GameData.instance.mapFlags.Sort(new MapFlagRecordComparer());

    // Return the index of the newly added map flag.
    int index = GameData.instance.mapFlags.IndexOf(mapFlagRecord);

    // Update Map Panel
    MapPanel.instance.MapFlagAdded(mapFlagRecord, index);
  }

  void ProcessEventSetTarget()
  {
    GameData.instance.targetAdvanceID = DecodeNumber(3);

    // Update the UI for the new target advance.
    AdvancesPanel.instance.UpdateForSetTarget();
    AdvanceDetailsPanel.instance.UpdateForSetTarget();
  }

  void ProcessEventStopAutoProcess()
  {
    // Stop any auto process that's in progress.
    mapView.StopAutoProcess();
  }

  void ProcessEventStats()
  {
    GameData.instance.pending_xp = DecodeUnsignedNumber(5);
    GameData.instance.energy = DecodeUnsignedNumber(5);
    GameData.instance.energyMax = DecodeUnsignedNumber(5);
    GameData.instance.manpowerMax = DecodeUnsignedNumber(5);
    GameData.instance.manpowerPerAttack = DecodeUnsignedNumber(2);
    GameData.instance.geo_efficiency_modifier = (DecodeUnsignedNumber(3) / 100f);
    GameData.instance.hitPointBase = DecodeUnsignedNumber(2);
    GameData.instance.hitPointRate = DecodeUnsignedNumber(2);
    GameData.instance.critChance = (DecodeUnsignedNumber(3) / 100f);
    GameData.instance.salvageValue = (DecodeUnsignedNumber(3) / 100f);
    GameData.instance.wallDiscount = (DecodeUnsignedNumber(3) / 100f);
    GameData.instance.structureDiscount = (DecodeUnsignedNumber(3) / 100f);
    GameData.instance.splashDamage = (DecodeUnsignedNumber(3) / 100f);
    GameData.instance.maxNumAlliances = DecodeUnsignedNumber(2);
    GameData.instance.maxSimultaneousProcesses = DecodeUnsignedNumber(2);
    float shardRedFill = ((float)DecodeUnsignedNumber(2)) / 100f;
    float shardGreenFill = ((float)DecodeUnsignedNumber(2)) / 100f;
    float shardBlueFill = ((float)DecodeUnsignedNumber(2)) / 100f;
    GameData.instance.numStorageStructures = DecodeUnsignedNumber(2);
    float sharedEnergyFill = GameData.instance.sharedEnergyFill = ((float)DecodeUnsignedNumber(2)) / 100f;
    float sharedManpowerFill = GameData.instance.sharedManpowerFill = ((float)DecodeUnsignedNumber(2)) / 100f;
    GameData.instance.sharedEnergyXPPerHour = DecodeUnsignedNumber(3);
    GameData.instance.sharedManpowerXPPerHour = DecodeUnsignedNumber(3);
    GameData.instance.energyStored = DecodeUnsignedNumber(5);
    GameData.instance.manpowerStored = DecodeUnsignedNumber(5);
    GameData.instance.energyAvailable = DecodeUnsignedNumber(5);
    GameData.instance.manpowerAvailable = DecodeUnsignedNumber(5);

    GameData.instance.invisibility = (DecodeUnsignedNumber(1) == 1);
    GameData.instance.insurgency = (DecodeUnsignedNumber(1) == 1);
    GameData.instance.total_defense = (DecodeUnsignedNumber(1) == 1);
    GameData.instance.nationIsVeteran = (DecodeUnsignedNumber(1) == 1);

    GameData.instance.statTechFromPerms = DecodeUnsignedNumber(2);
    GameData.instance.statBioFromPerms = DecodeUnsignedNumber(2);
    GameData.instance.statPsiFromPerms = DecodeUnsignedNumber(2);
    GameData.instance.manpowerRateFromPerms = DecodeUnsignedNumber(3);
    GameData.instance.energyRateFromPerms = DecodeUnsignedNumber(3);
    GameData.instance.xpMultiplierFromPerms = (DecodeUnsignedNumber(2) / 100f);

    GameData.instance.statTechFromTemps = DecodeUnsignedNumber(2);
    GameData.instance.statBioFromTemps = DecodeUnsignedNumber(2);
    GameData.instance.statPsiFromTemps = DecodeUnsignedNumber(2);
    GameData.instance.manpowerRateFromTemps = DecodeUnsignedNumber(3);
    GameData.instance.energyRateFromTemps = DecodeUnsignedNumber(3);
    GameData.instance.xpMultiplierFromTemps = (DecodeUnsignedNumber(2) / 100f);

    GameData.instance.statTechFromResources = DecodeUnsignedNumber(2);
    GameData.instance.statBioFromResources = DecodeUnsignedNumber(2);
    GameData.instance.statPsiFromResources = DecodeUnsignedNumber(2);
    GameData.instance.manpowerRateFromResources = DecodeUnsignedNumber(3);
    GameData.instance.energyRateFromResources = DecodeUnsignedNumber(3);
    GameData.instance.xpMultiplierFromResources = (DecodeUnsignedNumber(2) / 100f);

    GameData.instance.tech_mult = (DecodeUnsignedNumber(3) / 100f);
    GameData.instance.bio_mult = (DecodeUnsignedNumber(3) / 100f);
    GameData.instance.psi_mult = (DecodeUnsignedNumber(3) / 100f);
    GameData.instance.manpower_rate_mult = (DecodeNumber(3) / 100f);
    GameData.instance.energy_rate_mult = (DecodeNumber(3) / 100f);
    GameData.instance.manpower_max_mult = (DecodeNumber(3) / 100f);
    GameData.instance.energy_max_mult = (DecodeNumber(3) / 100f);
    GameData.instance.hp_per_square_mult = (DecodeNumber(3) / 100f);
    GameData.instance.hp_restore_mult = (DecodeNumber(3) / 100f);
    GameData.instance.attack_manpower_mult = (DecodeNumber(3) / 100f);
    GameData.instance.advance_points = DecodeUnsignedNumber(2);
    GameData.instance.resetAdvancesPrice = DecodeUnsignedNumber(3);
    int delay = DecodeUnsignedNumber(1);

    GameData.instance.raidNumMedals = DecodeUnsignedNumber(3);
    bool raidInProgress = (DecodeUnsignedNumber(1) != 0);

    DecodeFootprint(GameData.instance.mainland_footprint);
    DecodeFootprint(GameData.instance.homeland_footprint);

    if (raidInProgress) {
      DecodeFootprint(GameData.instance.raidland_footprint);
    }

    //Debug.Log("Stats event. mainland mp: " + GameData.instance.mainland_footprint.manpower + ", homeland mp: " + GameData.instance.homeland_footprint.manpower + ", raidland mp: " + GameData.instance.raidland_footprint.manpower);

    // Update the nation's manpower burn rate, for the new stat value.
    GameData.instance.DetermineManpowerBurnRate();

    bool update_nation_meters = false;

    // Update the player's nation's storage meters if either of the fill values have changed.
    NationData nationData = GameData.instance.GetNationData(GameData.instance.nationID);
    if ((nationData != null) && ((sharedEnergyFill != nationData.sharedEnergyFill) || (sharedManpowerFill != nationData.sharedManpowerFill))) {
      nationData.sharedEnergyFill = sharedEnergyFill;
      nationData.sharedManpowerFill = sharedManpowerFill;
      update_nation_meters = true;
    }

    // Update the player's nation's storage meters if any of the shard fill levels have changed.
    if ((shardRedFill != GameData.instance.shardRedFill) || (shardGreenFill != GameData.instance.shardGreenFill) || (shardBlueFill != GameData.instance.shardBlueFill)) {
      GameData.instance.shardRedFill = shardRedFill;
      GameData.instance.shardGreenFill = shardGreenFill;
      GameData.instance.shardBlueFill = shardBlueFill;
      update_nation_meters = true;
    }

    // Update the player's nation's storage meters if appropriate.
    if (update_nation_meters) {
      StorageMeter.UpdateNationMeters(GameData.instance.nationID);
    }

    // Update the GUI
    if (delay == 0) {
      GameGUI.instance.UpdateForStatsEvent(!initial_stats_event_received);
    } else {
      StartCoroutine(UpdateForStatsEventAfterDelay(!initial_stats_event_received, delay));
    }

    // Record that the initial stats event has been received.
    initial_stats_event_received = true;
  }

  void ProcessEventSubscription()
  {
    GameData.instance.subscribed = (DecodeUnsignedNumber(1) == 1);
    GameData.instance.subscriptionGateway = DecodeString();
    GameData.instance.subscriptionStatus = DecodeString();
    GameData.instance.subscriptionPaidThrough = DateTime.Now + TimeSpan.FromSeconds(DecodeUnsignedNumber(6));
    GameData.instance.associatedSubscribedUsername = DecodeString();
    Debug.Log("subscribed: " + GameData.instance.subscribed + ", subscriptionStatus: " + GameData.instance.subscriptionStatus + ", subscriptionPaidThrough: " + GameData.instance.subscriptionPaidThrough + ", associatedSubscribedUsername: " + GameData.instance.associatedSubscribedUsername);

    GameData.instance.subscriptionBonusCreditsTarget = DecodeString();
    GameData.instance.subscriptionBonusRebirthTarget = DecodeString();
    GameData.instance.subscriptionBonusXPTarget = DecodeString();
    GameData.instance.subscriptionBonusManpowerTarget = DecodeString();

    // Update the subscribe panel
    SubscribePanel.instance.UpdateSubscriptionState();
  }

  void ProcessEventSuspend()
  {
    bool due_to_inactivity = (DecodeUnsignedNumber(1) != 0);
    string message = DecodeLocalizableString();
    GameGUI.instance.SuspendEventReceived(due_to_inactivity, message);
  }

  void ProcessEventTournamentStatusGlobal()
  {
    GameData.instance.globalTournamentStartDay = DecodeNumber(6);
    GameData.instance.globalTournamentStatus = DecodeNumber(1);
    GameData.instance.tournamentNumActiveContenders = DecodeUnsignedNumber(3);
    GameData.instance.tournamentEnrollmentClosesTime = Time.unscaledTime + (float)DecodeUnsignedNumber(5);
    GameData.instance.tournamentNextEliminationTime = Time.unscaledTime + (float)DecodeUnsignedNumber(5);
    GameData.instance.tournamentEndTime = Time.unscaledTime + (float)DecodeUnsignedNumber(5);

    TournamentButton.instance.UpdateForGlobalStatus();
    TournamentPanel.instance.UpdateForTournamentStatus();
  }

  void ProcessEventTournamentStatusNation()
  {
    GameData.instance.nationTournamentStartDay = DecodeNumber(6);
    GameData.instance.nationTournamentActive = (DecodeUnsignedNumber(1) == 0) ? false : true;
    GameData.instance.tournamentRank = DecodeNumber(3);
    GameData.instance.tournamentTrophiesAvailable = DecodeUnsignedNumber(5) / 100f;
    GameData.instance.tournamentTrophiesBanked = DecodeUnsignedNumber(5) / 100f;

    int delay = DecodeUnsignedNumber(1);

    //Debug.Log("ProcessEventTournamentStatusNation() called. nationTournamentStartDay: " + GameData.instance.nationTournamentStartDay + ", nationTournamentActive: " + GameData.instance.nationTournamentActive + ", tournamentRank: " + GameData.instance.tournamentRank + ", tournamentTrophiesAvailable: " + GameData.instance.tournamentTrophiesAvailable + ", tournamentTrophiesBanked: " + GameData.instance.tournamentTrophiesBanked);

    StartCoroutine(ProcessEventTournamentStatusNation_Coroutine(delay));
  }

  public IEnumerator ProcessEventTournamentStatusNation_Coroutine(int _delay)
  {
    if (_delay > 0) {
      yield return new WaitForSeconds(_delay);
    }

    TournamentButton.instance.UpdateForNationStatus();
    TournamentPanel.instance.UpdateForTournamentStatus();
  }

  void ProcessEventTechnologies()
  {
    int i, ID, count, seconds_until_expires, num_techs;

    // Clear the technology lists.
    GameData.instance.techCount.Clear();
    GameData.instance.tempTechExpireTime.Clear();

    // Clear the available builds and add the initial builds.
    GameData.instance.InitAvailableBuilds();

    // Read the techCount table.
    num_techs = DecodeUnsignedNumber(2);
    for (i = 0; i < num_techs; i++) {
      ID = DecodeUnsignedNumber(2);
      count = DecodeUnsignedNumber(2);

      // Insert this entry in the techCount table.
      GameData.instance.techCount[ID] = count;
    }

    // Clear the UI's Temps List
    GameGUI.instance.tempsList.Clear();

    // Read the tempTechExpireTime table.
    num_techs = DecodeUnsignedNumber(2);
    for (i = 0; i < num_techs; i++) {
      ID = DecodeUnsignedNumber(2);
      seconds_until_expires = DecodeUnsignedNumber(5);

      // Insert this entry in the techCount table.
      GameData.instance.tempTechExpireTime[ID] = Time.unscaledTime + seconds_until_expires;

      // Add this temp tech to the Temps List
      GameGUI.instance.tempsList.AddTechnology(ID, Time.unscaledTime + seconds_until_expires);
    }

    // Determine available builds and upgrades.
    foreach (var techID in GameData.instance.techCount.Keys) {
      GameData.instance.UpdateBuildsForAdvance(techID);
    }

    // Update the AdvancesPanel for the modified list of technologies.
    AdvancesPanel.instance.UpdateForTechnologies();

    // Update the BuildMenu for the modified list of technologies.
    BuildMenu.instance.UpdateForTechnologies();
  }

  void ProcessEventTechPrices()
  {
    TechData techData;
    int techID, price;
    int count = DecodeUnsignedNumber(2);

    for (int i = 0; i < count; i++) {
      techID = DecodeUnsignedNumber(2);
      price = DecodeUnsignedNumber(4);

      // Record this tech's price.
      techData = TechData.GetTechData(techID);
      if (techData != null) {
        GameData.instance.prices[techID] = price;
      }
    }

    // Update the Advances Panel for the new tech prices.
    AdvancesPanel.instance.UpdateForTechPrices();
  }

  void ProcessEventTowerAction()
  {
    TargetRecord cur_target;
    int wipe_end_time;
    float cur_time = Time.time;

    int x = DecodeUnsignedNumber(4);
    int z = DecodeUnsignedNumber(4);
    int build_ID = DecodeNumber(2);
    int build_type = DecodeUnsignedNumber(1);
    int invisible_time = DecodeNumber(4);
    int duration = DecodeUnsignedNumber(1);
    int trigger_x = DecodeUnsignedNumber(4);
    int trigger_z = DecodeUnsignedNumber(4);
    int triggerNationID = DecodeUnsignedNumber(4);

    int num_targets = DecodeUnsignedNumber(2);
    List<TargetRecord> targets = new List<TargetRecord>();
    for (int i = 0; i < num_targets; i++) {
      cur_target = new TargetRecord();
      cur_target.x = DecodeUnsignedNumber(4);
      cur_target.y = DecodeUnsignedNumber(4);
      cur_target.newNationID = DecodeNumber(4);
      cur_target.full_hit_points = DecodeUnsignedNumber(2);
      cur_target.start_hit_points = DecodeUnsignedNumber(2);
      cur_target.end_hit_points = DecodeUnsignedNumber(2);
      cur_target.new_cur_hit_points = DecodeUnsignedNumber(2);
      cur_target.new_full_hit_points = DecodeUnsignedNumber(2);
      cur_target.hit_points_rate = (float)(DecodeUnsignedNumber(3)) / 100f;
      cur_target.battle_flags = DecodeUnsignedNumber(2);
      cur_target.wipe_end_time = DecodeNumber(4);
      cur_target.wipe_nationID = DecodeNumber(4);
      cur_target.wipe_flags = DecodeUnsignedNumber(1);
      targets.Add(cur_target);
    }

    // If viewing a raid map, decode defender's current area.
    if (GameData.instance.mapMode == GameData.MapMode.RAID) {
      GameData.instance.raidDefenderArea = DecodeUnsignedNumber(2);
      RaidScoreHeader.instance.OnDefenderAreaUpdate(4);
    }

    Debug.Log("Received tower_action for block " + x + "," + z + ", build_ID: " + build_ID + ", build_type: " + build_type);
    mapView.InitTowerAction(x, z, build_ID, (BuildData.Type)build_type, invisible_time, duration, trigger_x, trigger_z, triggerNationID, targets);
  }

  void ProcessEventTriggerInert()
  {
    int x = DecodeUnsignedNumber(4);
    int z = DecodeUnsignedNumber(4);

    mapView.InitTriggerInert(x, z);
  }

  void ProcessEventUpdate()
  {
    GameData.instance.credits = DecodeUnsignedNumber(4);
    GameData.instance.credits_transferable = DecodeUnsignedNumber(4);
    GameData.instance.credits_allowed_to_buy = DecodeNumber(4);
    GameData.instance.energy = DecodeUnsignedNumber(5);
    GameData.instance.rebirth_countdown = DecodeUnsignedNumber(3);
    GameData.instance.rebirth_countdown_purchased = DecodeUnsignedNumber(3);
    GameData.instance.prizeMoney = DecodeUnsignedNumber(5);

    //Debug.Log("ProcessEventUpdate() credits: " + GameData.instance.credits + ", credits_transferable: " + GameData.instance.credits_transferable + ", energy: " + GameData.instance.energy + ", rebirth_countdown: " + GameData.instance.rebirth_countdown);

    bool raidInProgress = (DecodeUnsignedNumber(1) != 0);

    MapView.instance.mainlandExtentX0 = DecodeUnsignedNumber(3);
    MapView.instance.mainlandExtentZ0 = DecodeUnsignedNumber(3);
    MapView.instance.mainlandExtentX1 = DecodeUnsignedNumber(3);
    MapView.instance.mainlandExtentZ1 = DecodeUnsignedNumber(3);

    MapView.instance.UpdateNationMaxExtent();

    DecodeFootprint(GameData.instance.mainland_footprint);
    DecodeFootprint(GameData.instance.homeland_footprint);

    if (raidInProgress) {
      DecodeFootprint(GameData.instance.raidland_footprint);
    }

    //Debug.Log("Update event. mainland mp: " + GameData.instance.mainland_footprint.manpower + ", homeland mp: " + GameData.instance.homeland_footprint.manpower + ", raidland mp: " + GameData.instance.raidland_footprint.manpower);

    NationPanel.instance.UpdateForArea();
    GameGUI.instance.UpdateForUpdateEvent();
  }

  void ProcessEventUpdateBars()
  {
    int energy_delta = DecodeNumber(5);
    int energy_rate_delta = DecodeNumber(5);
    int energy_burn_rate_delta = DecodeNumber(5);
    int manpower_delta = DecodeNumber(5);
    int credits_delta = DecodeNumber(4);
    int delay = DecodeUnsignedNumber(1);

    if (GameData.instance.current_footprint == null) {
      Debug.Log("ERROR: GetFinalGeoEfficiency() current_footprint is NULL!");
    } else {
      GameData.instance.current_footprint.manpower += manpower_delta;
    }

    GameData.instance.energy += energy_delta;
    GameData.instance.credits += credits_delta;

    if (delay > 0) {
      StartCoroutine(UpdateBarsAfterDelay(energy_delta, energy_rate_delta, energy_burn_rate_delta, manpower_delta, credits_delta, delay));
    } else {
      GameGUI.instance.UpdateForUpdateBarsEvent(energy_delta, energy_rate_delta, energy_burn_rate_delta, manpower_delta, credits_delta);
    }
  }

  void ProcessEventUsernameAvailable()
  {
    bool isAvailable = (DecodeUnsignedNumber(1) != 0);
    bool isSet = (DecodeUnsignedNumber(1) != 0);
    GameGUI.instance.UsernameAvailable(isAvailable, isSet);
  }

  void ProcessEventHistoricalExtent()
  {
    // Record previous easter max.
    int prevMaxX1 = mapView.maxX1;

    // Historical extent of nation
    mapView.minX0 = DecodeNumber(3);
    mapView.minZ0 = DecodeNumber(3);
    mapView.maxX1 = DecodeNumber(3);
    mapView.maxZ1 = DecodeNumber(3);

    // Have the map view redetermine its view limits.
    mapView.DetermineViewLimits();

    // If the eastern max has pushed further east, let the tutorial system know.
    if (mapView.maxX1 > prevMaxX1) {
      Tutorial.instance.PushedFurtherEast();
    }
  }

  void AddTechnology(int _techID, int _seconds_until_expires, int _delay)
  {
    // Increment count associated with this techID.
    GameData.instance.SetTechCount(_techID, GameData.instance.GetTechCount(_techID) + 1);

    // If this tech is temporary, add its expire time to the tempTechExpireTime table.
    if (_seconds_until_expires > 0) {
      GameData.instance.tempTechExpireTime[_techID] = Time.unscaledTime + (float)_seconds_until_expires;
    }

    // If this tech is temporary, add it to the Temps List
    if (_seconds_until_expires > 0) {
      GameGUI.instance.tempsList.AddTechnology(_techID, Time.unscaledTime + _seconds_until_expires);
    }

    // Update the lists of available builds and upgrades for the addition of this tech.
    GameData.instance.UpdateBuildsForAdvance(_techID);

    // Play sound
    if (_seconds_until_expires > 0) {
      // Temporary advance
      Sound.instance.Play2DAfterDelay(_delay, Sound.instance.advance_temp);
    } else {
      // Permanent advance
      Sound.instance.Play2DAfterDelay(_delay, Sound.instance.advance_permanent);
    }

    // Update the AdvancesPanel for the modified list of technologies.
    AdvancesPanel.instance.UpdateForTechnologies();

    // Update the AdvanceDetailsPanel for the modified list of technologies.
    AdvanceDetailsPanel.instance.UpdateForTechnologies();

    // Update the BuildMenu for the modified list of technologies.
    BuildMenu.instance.UpdateForTechnologies();
  }

  IEnumerator AnnounceDiscovery(int _delay, int _manpower_added, int _energy_added, string _target_nation_name, int _advanceID, int _duration, int _xp)
  {
    // Wait until it's time to announce the discovery.
    yield return new WaitForSeconds(_delay);

    string alert_text = "";

    if (_manpower_added > 0) {
      // GB:Localization
      string announce_manpower_increase_discovery_phrase1 = LocalizationManager.GetTranslation("announce_manpower_increase_discovery_phrase1"); // {3} "We have discovered a"
      string announce_manpower_increase_discovery_phrase2 = LocalizationManager.GetTranslation("announce_manpower_increase_discovery_phrase2"); // {4} "Manpower Supply Line"
      string announce_manpower_increase_discovery_phrase3 = LocalizationManager.GetTranslation("announce_manpower_increase_discovery_phrase3"); // {5} "manpower has been captured from"
      string announce_manpower_increase_discovery_phrase4 = LocalizationManager.GetTranslation("announce_manpower_increase_discovery_phrase4"); // {6} "for discovery"

      alert_text = String.Format("{3}<size=8>\n\n</size><font=\"trajanpro-bold SDF\"><color=\"yellow\"><b>{4}</b></color></font><size=8>\n\n</size><color=#00ff00ff>{0}</color> {5} {1}!\n\n<color=#00ff00ff>+{2}</color> XP {6}",
          _manpower_added, _target_nation_name, _xp,
          announce_manpower_increase_discovery_phrase1,
          announce_manpower_increase_discovery_phrase2,
          announce_manpower_increase_discovery_phrase3,
          announce_manpower_increase_discovery_phrase4);
    } else if (_energy_added > 0) {
      // GB:Localization
      string announce_energy_increase_discovery_phrase1 = LocalizationManager.GetTranslation("announce_energy_increase_discovery_phrase1"); // {0} "We have discovered an"
      string announce_energy_increase_discovery_phrase2 = LocalizationManager.GetTranslation("announce_energy_increase_discovery_phrase2"); // {1} "Energy Supply Line"
      string announce_energy_increase_discovery_phrase3 = LocalizationManager.GetTranslation("announce_energy_increase_discovery_phrase3"); // {2} "energy has been captured from"
      string announce_energy_increase_discovery_phrase4 = LocalizationManager.GetTranslation("announce_energy_increase_discovery_phrase4"); // {3} "for discovery"

      alert_text = String.Format("{0}<size=8>\n\n</size><font=\"trajanpro-bold SDF\"><color=\"yellow\"><b>{1}</b></color></font><size=8>\n\n</size><color=#00ff00ff>{4}</color> {2} {5}!\n\n<color=#00ff00ff>+{6}</color> XP {3}",
          announce_energy_increase_discovery_phrase1,
          announce_energy_increase_discovery_phrase2,
          announce_energy_increase_discovery_phrase3,
          announce_energy_increase_discovery_phrase4,
          _energy_added, _target_nation_name, _xp);
    } else if (_advanceID != -1) {
      TechData tech_data = TechData.GetTechData(_advanceID);

      // GB:Localization
      string announce_advance_discovery_phrase1 = LocalizationManager.GetTranslation("announce_advance_discovery_phrase1"); // "We have discovered"

      alert_text = String.Format("{0}<size=8>\n\n</size>", announce_advance_discovery_phrase1);
      alert_text += "<font=\"trajanpro-bold SDF\"><color=\"yellow\">" + tech_data.name + "</color></font><size=8>\n\n</size>";
      alert_text += tech_data.description + "\n\n";

      // Add bonuses
      bool bonus_added = false;
      if ((tech_data.bonus_type_1 != TechData.Bonus.UNDEF) || (tech_data.bonus_type_2 != TechData.Bonus.UNDEF) || (tech_data.bonus_type_3 != TechData.Bonus.UNDEF)) {
        if (tech_data.bonus_type_1 != TechData.Bonus.UNDEF) {
          alert_text += GameGUI.instance.GetBonusText(tech_data.bonus_type_1, tech_data.GetBonusVal(1), tech_data.GetBonusValMax(1), 0, false, GameGUI.instance.linkManager);
          bonus_added = true;
        }

        if (tech_data.bonus_type_2 != TechData.Bonus.UNDEF) {
          alert_text += (bonus_added ? ", " : "") + GameGUI.instance.GetBonusText(tech_data.bonus_type_2, tech_data.GetBonusVal(2), tech_data.GetBonusValMax(2), 0, false, GameGUI.instance.linkManager);
          bonus_added = true;
        }

        if (tech_data.bonus_type_3 != TechData.Bonus.UNDEF) {
          alert_text += (bonus_added ? ", " : "") + GameGUI.instance.GetBonusText(tech_data.bonus_type_3, tech_data.GetBonusVal(3), tech_data.GetBonusValMax(3), 0, false, GameGUI.instance.linkManager);
          bonus_added = true;
        }

        alert_text += "\n";
      }

      if (tech_data.duration_type == TechData.Duration.TEMPORARY) {
        // GB:Localization
        string announce_advance_discovery_phrase2 = LocalizationManager.GetTranslation("announce_advance_discovery_phrase2"); // "Lasts"

        alert_text += String.Format("{0} {1}\n",
            announce_advance_discovery_phrase2,
            GameData.instance.GetDurationText(_duration));
      }

      // GB:Localization
      string announce_advance_discovery_phrase3 = LocalizationManager.GetTranslation("announce_advance_discovery_phrase3"); // "for discovery"

      alert_text += String.Format("<color=#00ff00ff>+{0}</color> XP {1}\n", _xp, announce_advance_discovery_phrase3);

      // Add the technology on the client, since an AddTechnology event wasn't sent, to allow for the delay.
      AddTechnology(_advanceID, _duration, 0);
    }

    // Update the bars for energy or manpower added.
    GameGUI.instance.UpdateForUpdateBarsEvent(_energy_added, 0, 0, _manpower_added, 0);

    Debug.Log("AnnounceDiscovery() _energy_added: " + _energy_added + ", _manpower_added: " + _manpower_added + " _advanceID: " + _advanceID + ", alert_text: " + alert_text);

    // Show the requestor.
    Alert.Activate(alert_text);
  }

  IEnumerator AnnounceCaptureStorage(int _delay, int _resource_added, string _target_nation_name, int _buildID)
  {
    // Wait until it's time to announce the discovery.
    yield return new WaitForSeconds(_delay);

    BuildData buildData = BuildData.GetBuildData(_buildID);

    if (buildData == null) {
      yield break;
    }

    string alert_text = "";

    // GB:Localization
    string announce_capture_storage_phrase1 = LocalizationManager.GetTranslation("announce_capture_storage_phrase1"); // {2} "We have captured"
    string announce_resource_increase_discovery_phrase3 = (buildData.type == BuildData.Type.MANPOWER_STORAGE) ? LocalizationManager.GetTranslation("announce_manpower_increase_discovery_phrase3") : LocalizationManager.GetTranslation("announce_energy_increase_discovery_phrase3"); // {4} "manpower has been captured from"

    alert_text = String.Format("{2}<size=8>\n\n</size><font=\"trajanpro-bold SDF\"><color=\"yellow\"><b>{3}</b></color></font><size=8>\n\n</size><color=#00ff00ff>{0}</color> {4} {1}!\n",
        _resource_added, _target_nation_name,
        announce_capture_storage_phrase1,
        buildData.name,
        announce_resource_increase_discovery_phrase3);

    // Update the bars for energy or manpower added.
    GameGUI.instance.UpdateForUpdateBarsEvent((buildData.type == BuildData.Type.ENERGY_STORAGE) ? _resource_added : 0, 0, 0, (buildData.type == BuildData.Type.MANPOWER_STORAGE) ? _resource_added : 0, 0);

    Debug.Log("AnnounceCaptureStorage() _resource_added: " + _resource_added + ", _buildID: " + _buildID + ", alert_text: " + alert_text);

    // Show the requestor.
    Alert.Activate(alert_text);
  }

  IEnumerator ModifyAdBonusAmountAfterDelay(int _delta_amount, int _total_amount, AdBonusButton.AdBonusType _type, int _x, int _z, int _delay)
  {
    // Wait until it's time to update the bars.
    yield return new WaitForSeconds(_delay);

    GameGUI.instance.adBonusButton.ModifyAmount(_delta_amount, _total_amount, _type, _x, _z);
  }

  IEnumerator UpdateBarsAfterDelay(int _energy_delta, int _energy_rate_delta, int _energy_burn_rate_delta, int _manpower_delta, int _credits_delta, int _delay)
  {
    // Wait until it's time to update the bars.
    yield return new WaitForSeconds(_delay);

    GameGUI.instance.UpdateForUpdateBarsEvent(_energy_delta, _energy_rate_delta, _energy_burn_rate_delta, _manpower_delta, _credits_delta);
  }

  IEnumerator UpdateForStatsEventAfterDelay(bool _initial_stats_event, int _delay)
  {
    // Wait until it's time to update the GUI for the stats event.
    yield return new WaitForSeconds(_delay);

    GameGUI.instance.UpdateForStatsEvent(_initial_stats_event);
  }

  public IEnumerator AddXPAfterDelay(int _xp_delta, int _xp, int _userID, int _block_x, int _block_y, int _delay)
  {
    // Determine when to display the XP addition. Either after _delay time, or shortly after the previously queued XP addition -- whichever is later.
    prev_add_xp_time = Math.Max(prev_add_xp_time + 2f, Time.unscaledTime + _delay);

    // Wait until it's time to execute the addition of points.
    yield return new WaitForSeconds(prev_add_xp_time - Time.unscaledTime);

    // Have the GUI display the change in XP.
    GameGUI.instance.AddedXP(_xp_delta, _xp, _userID, _block_x, _block_y);
  }

  public IEnumerator ChangeLevelAfterDelay(int _delay)
  {
    // Wait until it's time to execute the change of level.
    yield return new WaitForSeconds(_delay);

    // Have the GUI display the change in level.
    GameGUI.instance.ChangedLevel();

    // Have the map view update its boundary lines for the new level.
    mapView.UpdateBoundaryLines();
  }

  public IEnumerator ChangeManpowerAfterDelay(int _delta, int _delay)
  {
    float eventReceivedTime = Time.unscaledTime;

    // Wait until it's time to execute the change of manpower.
    yield return new WaitForSeconds(_delay);

    // Have the GUI display the change in manpower.
    GameGUI.instance.ChangedManpower(_delta, eventReceivedTime);
  }

  public IEnumerator ChangeEnergyAfterDelay(int _delta, int _delay)
  {
    // Wait until it's time to execute the change of energy.
    yield return new WaitForSeconds(_delay);

    // Have the GUI display the change in energy.
    GameGUI.instance.ChangedEnergy(_delta);
  }

  public IEnumerator UpdateGeoEfficiencyAfterDelay(float _geo_efficiency_base, int _delay)
  {
    // Wait until it's time to execute the change of geo efficiency.
    yield return new WaitForSeconds(_delay);

    // Have the GUI display the change in geo efficiency.
    GameGUI.instance.ChangedGeoEfficiency(_geo_efficiency_base);
  }

  public IEnumerator UpdateQuestStatusAfterDelay(QuestRecord _questRecord, int _delay)
  {
    // Wait until it's time to execute the change of level.
    yield return new WaitForSeconds(_delay);

    // Update the quest's status.
    QuestsPanel.instance.UpdateQuestStatus(_questRecord);
  }

  public IEnumerator AddMessageAfterDelay(int _delay, bool _unread, int _userID, int _nationID, int _deviceID, string _username, string _nation_name, string _text, string _timestamp, int _time, int _reported, bool _addAtTop)
  {
    // Wait out the delay
    yield return new WaitForSeconds(_delay);

    // Add the message
    MessagesPanel.instance.AddMessage(_unread, _userID, _nationID, _deviceID, _username, _nation_name, _text, _timestamp, _time, _reported, _addAtTop);
  }

  public IEnumerator AlertForBlockAfterDelay(float _delay, int _x, int _z, bool _first_capture)
  {
    // Wait through the delay.
    yield return new WaitForSeconds(_delay);

    AlertForBlock(_x, _z, _first_capture);
  }

  public void AlertForBlock(int _x, int _z, bool _first_capture)
  {
    BlockData block_data = MapView.instance.GetBlockData(_x, _z);

    //Debug.Log("x: " + _x + ", z: " + _z + ", block_data: " + block_data);
    //Debug.Log("block_data.nationID: " + block_data.nationID);
    //Debug.Log("block_data.objectID: " + block_data.objectID + ", landscape_object: " + block_data.landscape_object);

    // If the given block is not occupied by this nation, or does not contain a resource or orb, do not show an alert.
    if ((block_data == null) || (block_data.nationID != GameData.instance.nationID) || (block_data.objectID < ObjectData.RESOURCE_OBJECT_BASE_ID)) {
      return;
    }

    ObjectData object_data = ObjectData.GetObjectData(block_data.objectID);

    // GB:Localization
    string alert_block_captured_phrase1 = LocalizationManager.GetTranslation("alert_block_captured_phrase1"); // "We have captured"

    string alert_text = String.Format("{0}<size=8>\n\n</size>", alert_block_captured_phrase1);
    alert_text += "<font=\"trajanpro-bold SDF\"><color=\"yellow\"><b>" + object_data.name + "</b></color></font><size=8>\n\n</size>";

    if (_first_capture) {
      alert_text += "<color=#00ff00ff>+" + object_data.xp + "</color> " + LocalizationManager.GetTranslation("xp_for_discovery") + "\n"; // " XP for discovery\n";
    }

    if (block_data.objectID < ObjectData.ORB_BASE_ID) {
      TechData tech_data = TechData.GetTechData(object_data.techID);

      float position = object_data.GetPosition(_x);

      // Add bonuses
      bool bonus_added = false;
      if ((tech_data.bonus_type_1 != TechData.Bonus.UNDEF) || (tech_data.bonus_type_2 != TechData.Bonus.UNDEF) || (tech_data.bonus_type_3 != TechData.Bonus.UNDEF) || (tech_data.new_build_name != "")) {
        if (tech_data.bonus_type_1 != TechData.Bonus.UNDEF) {
          alert_text += GameGUI.instance.GetBonusText(tech_data.bonus_type_1, tech_data.GetBonusVal(1), tech_data.GetBonusValMax(1), position, false, GameGUI.instance.linkManager);
          bonus_added = true;
        }

        if (tech_data.bonus_type_2 != TechData.Bonus.UNDEF) {
          alert_text += (bonus_added ? ", " : "") + GameGUI.instance.GetBonusText(tech_data.bonus_type_2, tech_data.GetBonusVal(2), tech_data.GetBonusValMax(2), position, false, GameGUI.instance.linkManager);
          bonus_added = true;
        }

        if (tech_data.bonus_type_3 != TechData.Bonus.UNDEF) {
          alert_text += (bonus_added ? ", " : "") + GameGUI.instance.GetBonusText(tech_data.bonus_type_3, tech_data.GetBonusVal(3), tech_data.GetBonusValMax(3), position, false, GameGUI.instance.linkManager);
          bonus_added = true;
        }
      }
    } else {
      alert_text += LocalizationManager.GetTranslation((object_data.credits_per_hour == 1) ? "orb_converts_singular" : "orb_converts")
          .Replace("{credits}", "$" + (GameData.instance.orbPayoutRates[block_data.objectID] / 100.0).ToString("N2")/*string.Format("{0:G}", object_data.credits_per_hour)*/)
          .Replace("{xp}", string.Format("{0:G}", (object_data.xp_per_hour * 24))); //  "This orb generates <color=#00ff00ff>{credits}</color> credits and <color=#00ff00ff>{xp}</color> XP every hour!";
    }

    // Show the requestor.
    Alert.Activate(alert_text);

    // Play sound
    Sound.instance.Play2D(Sound.instance.capture_resource);
  }

  NationData ReadNationData(int _nationID, bool _process_incognito = true, bool _overwrite = true)
  {
    bool nationRecordExists;
    NationData nationData;

    //Debug.Log("ReadNationData() reading data for nation " + _nationID);

    // Determine whether a record for this nation is already in the nation table.
    nationRecordExists = GameData.instance.nationTable.ContainsKey(_nationID);

    // If we already have data for this nation, and we're not to overwrite it, read in but do nothing with the new data, and return.
    if (nationRecordExists && !_overwrite) {
      // Read in the data, but do nothing with it.
      DecodeUnsignedNumber(2);
      DecodeString();
      DecodeUnsignedNumber(2);
      DecodeUnsignedNumber(2);
      DecodeUnsignedNumber(2);
      DecodeNumber(2);
      DecodeUnsignedNumber(1);
      DecodeUnsignedNumber(2);
      DecodeUnsignedNumber(2);
      return GameData.instance.nationTable[_nationID];
    }

    if (nationRecordExists) {
      // Fetch the data for this nation from the table.
      nationData = GameData.instance.nationTable[_nationID];
    } else {
      // Create a new NationData object for this nation.
      nationData = new NationData();
    }

    float prevSharedEnergyFill = nationData.sharedEnergyFill;
    float prevSharedManpowerFill = nationData.sharedManpowerFill;

    // Read this nation's data.
    nationData.flags = DecodeUnsignedNumber(2);
    nationData.SetName(DecodeString());
    //nationData.area = DecodeNumber(3);
    nationData.r = ((float)DecodeUnsignedNumber(2)) / 255.0f;
    nationData.g = ((float)DecodeUnsignedNumber(2)) / 255.0f;
    nationData.b = ((float)DecodeUnsignedNumber(2)) / 255.0f;
    nationData.emblem_index = DecodeNumber(2);
    nationData.emblem_color = (NationData.EmblemColor)(DecodeUnsignedNumber(1));
    nationData.sharedEnergyFill = ((float)DecodeUnsignedNumber(2)) / 100f;
    nationData.sharedManpowerFill = ((float)DecodeUnsignedNumber(2)) / 100f;

    // If the nation is incognito, modify its appearance and name.
    if (_process_incognito && ((nationData.flags & (int)GameData.NationFlags.INCOGNITO) != 0)) {
      nationData.r = 0.1f;
      nationData.g = 0.0f;
      nationData.b = 0.0f;
      nationData.emblem_index = 125;
      nationData.emblem_color = NationData.EmblemColor.WHITE;
    }

    // Have the nation data determine the emblem UV coords.
    nationData.DetermineEmblemUV();

    // Determine whether a map label is required for this nation.
    // A label is required if the nation is the player's nation, or an ally, or in the player's nation's chat list.
    nationData.label_required = ((_nationID == GameData.instance.nationID) || (GameData.instance.NationIsInAllianceList(GameData.instance.alliesList, _nationID)) || (chatSystem.IsNationInChatList(_nationID)));

    if (!nationRecordExists) {
      // Add this nation's data to the nation table.
      GameData.instance.nationTable.Add(_nationID, nationData);
    }

    // If this nation's shared energy or manpower has changed, update its storage meters.
    if (nationRecordExists && ((nationData.sharedEnergyFill != prevSharedEnergyFill) || (nationData.sharedManpowerFill != prevSharedManpowerFill))) {
      StorageMeter.UpdateNationMeters(_nationID);
    }

    // If this data is for the player's nation, record its updated flags.
    if (_nationID == GameData.instance.nationID) {
      GameData.instance.nationFlags = nationData.flags;
    }

    return nationData;
  }

  String DecodeLocalizableString()
  {
    String format_id = DecodeString();
    //Debug.Log("DecodeLocalizableString() format_id: " + format_id); 

    // If no string ID is given, return empty string. No parameters will be given.
    if (format_id.Length == 0) {
      return "";
    }

    String format_val = I2.Loc.LocalizationManager.GetTranslation("Server Strings/" + format_id);

    // If there is no localization value with the given format_id as its key, use the format_id as the format_val.
    if ((format_val == null) || (format_val.Length == 0)) {
      Debug.Log("No localization found for key: " + ("Server Strings/" + format_id));
      format_val = format_id;
    }

    //Debug.Log("DecodeLocalizableString() format_val: " + format_val);

    // Read the number of params
    int num_params = DecodeUnsignedNumber(1);

    for (int i = 0; i < num_params; i++) {
      String param_id = DecodeString();
      String param_val = DecodeString();

      if (param_id.Length == 0) {
        continue;
      }

      // If the parameter value is enclosed within braces, attempt to localize it.
      if (param_val.StartsWith("{") && param_val.EndsWith("}")) {
        String param_val_loc = I2.Loc.LocalizationManager.GetTranslation(param_val.Substring(1, param_val.Length - 2));
        if ((param_val_loc != null) && (param_val_loc.Length > 0)) {
          param_val = param_val_loc;
        } else {
          Debug.Log("No value found for localization key: " + param_val.Substring(1, param_val.Length - 2));
        }
      }

      //Debug.Log("DecodeLocalizableString() param_id: " + param_id + ", param_val: " + param_val);

      // Replace any occurrances of the current param_id with the corresponding param_val.
      format_val = format_val.Replace("{" + param_id + "}", param_val);
    }

    //Debug.Log("DecodeLocalizableString(): " + format_val);

    return format_val;
  }

  String LocalizableStringFromJSON(String _json_text)
  {
    JSONNode json = JSON.Parse(_json_text);

    if ((json == null) || (json["ID"] == null)) {
      return "INVALID JSON: " + _json_text;
    }

    String format_id = json["ID"];
    String format_val = I2.Loc.LocalizationManager.GetTranslation("Server Strings/" + format_id);

    // If there is no localization value with the given format_id as its key, use the format_id as the format_val.
    if ((format_val == null) || (format_val.Length == 0)) {
      Debug.Log("No localization found for key: " + ("Server Strings/" + format_id));
      format_val = format_id;
    }

    //Debug.Log("LocalizableStringFromJSON() format_val: " + format_val);

    // Read params
    for (int i = 0; ; i++) {
      if (json["param_id_" + i] == null) {
        break;
      }

      String param_id = json["param_id_" + i];
      String param_val = json["param_val_" + i];

      if (param_id.Length == 0) {
        continue;
      }

      // If the parameter value is enclosed within braces, attempt to localize it.
      if (param_val.StartsWith("{") && param_val.EndsWith("}")) {
        String param_val_loc = I2.Loc.LocalizationManager.GetTranslation(param_val.Substring(1, param_val.Length - 2));
        //Debug.Log("key: " + param_val.Substring(1, param_val.Length - 2) + ", val: " + I2.Loc.LocalizationManager.GetTranslation(param_val.Substring(1, param_val.Length - 2)));
        if ((param_val_loc != null) && (param_val_loc.Length > 0)) {
          param_val = param_val_loc;
        } else {
          Debug.Log("No value found for localization key: " + param_val.Substring(1, param_val.Length - 2));
        }
      }

      //Debug.Log("LocalizableStringFromJSON() param_id: " + param_id + ", param_val: " + param_val);

      // Replace any occurrances of the current param_id with the corresponding param_val.
      format_val = format_val.Replace("{" + param_id + "}", param_val);
    }

    //Debug.Log("LocalizableStringFromJSON(): " + format_val);

    return format_val;
  }

  int DecodeNumber(int _num_digits)
  {
    int increment, val = 0, digit, i;
    bool negative = false;

    for (i = _num_digits; i > 0; i--) {
      if (i == 1) {
        increment = 1;
      } else if (i == 2) {
        increment = 64;
      } else if (i == 3) {
        increment = 4096;
      } else if (i == 4) {
        increment = 262144;
      } else if (i == 5) {
        increment = 16777216;
      } else { // i == 6
        increment = 1073741824;
      }

      // Get the next character in the event data
      digit = FetchNextCharCode() - 63;

      if ((i == _num_digits) && (digit > 31)) {
        digit -= 32;
        negative = true;
      }

      val += (digit * increment);
    }

    if (negative) {
      return -val;
    }

    return val;
  }

  int DecodeUnsignedNumber(int _num_digits)
  {
    int increment, val = 0, digit, i;

    for (i = _num_digits; i > 0; i--) {
      if (i == 1) {
        increment = 1;
      } else if (i == 2) {
        increment = 64;
      } else if (i == 3) {
        increment = 4096;
      } else if (i == 4) {
        increment = 262144;
      } else if (i == 5) {
        increment = 16777216;
      } else { // i == 6
        increment = 1073741824;
      }

      // Get the next character in the event data
      digit = FetchNextCharCode() - 63;

      val += (digit * increment);
    }

    return val;
  }

  String DecodeString()
  {
    int i = 0, subStringLength = 0;

    // Decode length of string
    int str_length = DecodeUnsignedNumber(2);

    //// TESTING
    //bool test = (str_length == 57);

    /*
		// NEW
		//// TEMP
		//for (int j = 0; j < str_length; j++) {
		//	network.inputBuffer[inputBufferPos + j] = (byte)(((int)187) - ((int)(network.inputBuffer[inputBufferPos + j])));
		//}

		String str = Encoding.UTF8.GetString(network.inputBuffer, inputBufferPos, str_length);

		// Advance the input buffer position pointer
		inputBufferPos += str_length;

		Debug.Log ("Decoded string: " + str);

		return str;
		*/
    /*
// OLD WAY
// Decrypt the string
int ch;
for (i = inputBufferPos; i < Math.Min(Network.instance.inputBufferCapacity, inputBufferPos + str_length); i++)
{
  ch = (int)(network.inputBuffer[i]);
  if ((ch >= 65) && (ch <= 122)) ch = (57 - (ch - 65)) + 65;
  subStringBuffer[subStringLength++] = Convert.ToChar(ch);
}

// Advance the input buffer position pointer
inputBufferPos += str_length;
    */

    // UTF-8 WAY
    // Decrypt the string
    int ch, temp;
    while ((subStringLength < str_length) && (inputBufferPos < Network.instance.inputBufferCapacity)) {
      ch = (int)(network.inputBuffer[inputBufferPos]);
      inputBufferPos++;

      //if (test) Debug.Log("Test 1st ch: " + ch);

      if ((ch & 0x80) == 0) // if ((ch & 10000000) == 0)
      {
        // "Obscuring"
        if ((ch >= 65) && (ch <= 122)) ch = (57 - (ch - 65)) + 65;
      } else if ((ch & 0xE0) == 0xC0) // if ((ch & 11100000) == 11000000)
        {
        ch = (ch & 0x1F) << 6;
        temp = (int)(network.inputBuffer[inputBufferPos]);
        inputBufferPos++;
        ch = ch | (temp & 0x3F); // ch = ch | (temp & 00111111);
                                 //if (test) Debug.Log("Test temp1: " + temp + ", new ch: " + ch);
      } else if ((ch & 0xF0) == 0xE0) // if ((ch & 11110000) == 11100000)
        {
        ch = (ch & 0x0F) << 6;
        temp = (int)(network.inputBuffer[inputBufferPos]);
        inputBufferPos++;
        ch = ch | (temp & 0x3F); // ch = ch | (temp & 00111111);
                                 //if (test) Debug.Log("Test temp1: " + temp + ", new ch: " + ch);

        ch = ch << 6;
        temp = (int)(network.inputBuffer[inputBufferPos]);
        inputBufferPos++;
        ch = ch | (temp & 0x3F); // ch = ch | (temp & 00111111);
                                 //if (test) Debug.Log("Test temp2: " + temp + ", new ch: " + ch);
      } else if ((ch & 0xF8) == 0xF0) // if ((ch & 11111000) == 11110000)
        {
        ch = (ch & 0x07) << 6;
        temp = (int)(network.inputBuffer[inputBufferPos]);
        inputBufferPos++;
        ch = ch | (temp & 0x3F); // ch = ch | (temp & 00111111);
                                 //if (test) Debug.Log("Test temp1: " + temp + ", new ch: " + ch);

        ch = ch << 6;
        temp = (int)(network.inputBuffer[inputBufferPos]);
        inputBufferPos++;
        ch = ch | (temp & 0x3F); // ch = ch | (temp & 00111111);
                                 //if (test) Debug.Log("Test temp2: " + temp + ", new ch: " + ch);

        ch = ch << 6;
        temp = (int)(network.inputBuffer[inputBufferPos]);
        inputBufferPos++;
        ch = ch | (temp & 0x3F); // ch = ch | (temp & 00111111);
                                 //if (test) Debug.Log("Test temp3: " + temp + ", new ch: " + ch);
      }

      //if (test) Debug.Log("Test ch: " + ch);

      if (ch >= 0x1F000) {
        Debug.Log("Ignoring emoji character");

        // An emoji counts as two characters in the string length.
        subStringBuffer[subStringLength++] = Convert.ToChar(9647); // box
        subStringBuffer[subStringLength++] = Convert.ToChar(9647); // box
                                                                   //if (test) Debug.Log("After emoji, subStringLength increased to: " + subStringLength);
      } else {
        try {
          subStringBuffer[subStringLength++] = Convert.ToChar(ch);
          //if (test) Debug.Log("Test string (subStringLength " + subStringLength + "/" + str_length + "): '" + new String(subStringBuffer, 0, subStringLength) + "'");
        } catch (OverflowException e) {
          Debug.Log("DecodeString() OverflowException for char value " + ch + ". inputBufferPos: " + inputBufferPos + ", inputBufferLength: " + network.inputBufferLength + ", subString: '" + new String(subStringBuffer, 0, subStringLength) + "'");
          subStringBuffer[subStringLength++] = Convert.ToChar(9647); // box
                                                                     //if (test) Debug.Log("After exception, subStringLength increased to: " + subStringLength);
        }
      }
    }

    return new String(subStringBuffer, 0, subStringLength);
  }

  int FetchNextCharCode()
  {
    if (inputBufferPos >= network.inputBufferLength) {
      throw new Exception("Exceeded end of input buffer!");
    }

    // Get the next character in the event data
    int val = (int)(network.inputBuffer[inputBufferPos]);

    // Advance the input buffer position index
    inputBufferPos++;

    return val;
  }
}
