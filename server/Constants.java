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
import java.net.*;
import java.util.*;
import java.nio.charset.*;
import java.text.SimpleDateFormat;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import WOCServer.Output;
import WOCServer.Hashids;

public class Constants
{
	// Debug info
	public static boolean debug_flag = false;
	public static long debug_start_time;
	public static StringBuffer debug_stringbuffer = new StringBuffer(5000);

	//public static final int USER_SETTINGS_PAYMENT_PAYPAL = 0;
	//public static final int USER_SETTINGS_PAYMENT_MONEYBOOKERS = 32;
  public static final int USER_SETTINGS_BLOCK_WHISPERS = 16;

	// Data types
	public static final int DT_GLOBAL      = 0;
	public static final int DT_BLOCK       = 1;
	public static final int DT_BLOCK_EXT   = 2;
	public static final int DT_NATION      = 3;
	public static final int DT_NATIONTECH  = 4;
	public static final int DT_NATION_EXT  = 5;
	public static final int DT_USER        = 6;
	public static final int DT_RANKS       = 7;
	public static final int DT_DEVICE      = 8;
	public static final int DT_COMPLAINT   = 9;
	public static final int DT_VOUCHER     = 10;
	public static final int DT_EMAIL       = 11;
	public static final int DT_TOURNAMENT  = 12;
	public static final int DT_RAID        = 13;
	public static final int DT_LANDMAPINFO = 14;
	public static final int DT_NUM_TYPES   = 15;

	// IDs of unique data objects
	public static final int GLOBAL_DATA_ID = 1;
	public static final int RANKS_DATA_ID = 1;
	public static final int TOURNAMENT_DATA_ID = 1;

	// ID of mainland map
	public static final int MAINLAND_MAP_ID = 1;

	// Max dimension of a map, used to reset bounding boxes.
	public static final int MAX_MAP_DIM = 1000000;

	// Current data update count
	static int DATA_UPDATE_COUNT = 11;

  // Number of seconds in various time intervals
  static int SECONDS_PER_MINUTE = 60;
	static int SECONDS_PER_HOUR = 3600;
	static int SECONDS_PER_DAY = 86400;

	// Used to avoid encoding numbers of too great a magnitude.
	static int LARGE_NEGATIVE_TIME = -16777215;

	// Set delays for removing inactive clients
	public static int SUSPEND_INACTIVE_PLAYER_FINE_DELAY = 15 * 60 * 1000; // 15 minutes in milliseconds
	public static int REMOVE_INACTIVE_CLIENT_FINE_DELAY = 30 * 60 * 1000; // 30 minutes in milliseconds

	// Terrain types
	public static final int TERRAIN_DEEP_WATER 				= 0;
	public static final int TERRAIN_MEDIUM_WATER 			= 1;
	public static final int TERRAIN_SHALLOW_WATER			= 2;
	public static final int TERRAIN_BEACH 						= 3;
	public static final int TERRAIN_FLAT_LAND 				= 4;
	public static final int TERRAIN_HILLS 						= 5;
	public static final int TERRAIN_MEDIUM_MOUNTAINS 	= 6;
	public static final int TERRAIN_TALL_MOUNTAINS 		= 7;
	public static final int TERRAIN_POND			 		    = 8; // Only used on the client
	public static final int TERRAIN_MOUND      		    = 9; // Only used on the client
	public static final int NUM_TERRAIN_TYPES					= 10;

	// Mainland map level limits
	public static final double LEVEL_LIMIT_POWER = 1;
	//public static final int    MIN_LEVEL_LIMIT = -1;
	//public static final float  MAX_LEVEL_LIMIT = 300f;
	public static final int LEVEL_MARGIN_TO_LIMIT_EXPANSION = 1;
	public static final int EASTERN_LEVEL_LIMIT_MULTIPLIER = 3;
	public static final int EASTERN_LEVEL_LIMIT_ADDEND = 40;

	// Display area info
	public static int DISPLAY_CHUNK_SIZE = 16;
	public static int VIEW_AREA_CHUNKS_WIDE = 7;

	// Nation area grid spacing
	public static int AREA_GRID_SPACING = 32;
	public static int HALF_AREA_GRID_SPACING = AREA_GRID_SPACING / 2;
	public static int AREA_GRID_SPACING_SQUARE_DISTANCE = AREA_GRID_SPACING * AREA_GRID_SPACING;

	// How far beyond a nation's historical extents that its players may center their map view.
	public static int EXTRA_VIEW_RANGE = 20;

	// Starting index of restricted emblems
	public static int RESTRICTED_EMBLEM_START_INDEX = 120;

	// User and nation name constraints
  public static int MIN_USERNAME_LEN = 1;
  public static int MIN_PASSWORD_LEN = 8;
	public static int MIN_NATION_NAME_LEN = 1;
  public static int MIN_ANSWER_LEN = 1;
  public static int MAX_USERNAME_LEN = 20;
  public static int MAX_PASSWORD_LEN = 20;
	public static int MAX_NATION_NAME_LEN = 20;
  public static int MAX_ANSWER_LEN = 50;

	// Info flags
	public static int IF_ADMIN								    = 1;
	public static int IF_FIRST_LOGIN					    = 2;
	public static int IF_REGISTERED						    = 4;
	public static int IF_VETERAN_USER					    = 8;
	public static int IF_AD_BONUSES_ALLOWED		    = 16;
	public static int IF_CASH_OUT_PRIZES_ALLOWED  = 32;
	public static int IF_CREDIT_PURCHASES_ALLOWED = 64;

	// User flags
	public static int UF_CUSTOM_USERNAME			= 1;
	public static int UF_BLOCK_WHISPERS       = 2;
	public static int UF_DISABLE_CHAT_FILTER  = 4;
	public static int UF_FULLSCREEN						= 8;
	public static int UF_SHOW_MAP_LOCATION		= 16;
	public static int UF_SOUND_EFFECTS     		= 32;
	public static int UF_HIDE_TUTORIAL     		= 64;
	public static int UF_EVERYPLAY        		= 128;
	public static int UF_SHOW_FACE        		= 256;
	public static int UF_RECORD_VIDEO     		= 512;
	public static int UF_SHOW_GRID        		= 1024;
	public static int UF_MUSIC				     		= 2048;

	// Default user flags
	public static int UF_DEFAULT = UF_FULLSCREEN | UF_SOUND_EFFECTS | UF_MUSIC | UF_EVERYPLAY;

	// Nation flags
	public static int NF_CUSTOM_NATION_NAME							= 1;
	public static int NF_BLOCK_NATION_CHAT_INVITATIONS	= 2;
	public static int NF_TOURNAMENT_FIRST_PLACE					= 4;
	public static int NF_TOURNAMENT_SECOND_PLACE				= 8;
	public static int NF_TOURNAMENT_THIRD_PLACE					= 16;
	public static int NF_ORB_OF_FIRE										= 32;
	public static int NF_INCOGNITO											= 64;
	public static int NF_ONLINE													= 128;
	public static int NF_TOURNAMENT_CONTENDER						= 256;


	// Stat types
	public static int STAT_TECH = 0;
	public static int STAT_BIO  = 1;
	public static int STAT_PSI  = 2;
	public static int NUM_STATS = 3;

	// Fraction of a nation's permanent stat value that can be added by resource bonuses, before incurring a manpower burn penalty.
	public static float RESOURCE_BONUS_CAP = 2f;

	// Used to determine manpower burn rate for stats increased by resources in excess of cap.
	public static float MANPOWER_BURN_EXPONENT = 2f;
	public static float MANPOWER_BURN_FRACTION_OF_MANPOWER_MAX = 0.015f;

	// Fraction of energy generation that is burned by being incognito.
	public static float INCOGNITO_ENERGY_BURN = 0.4f;

	// The minimum period that a nation must stay incognito once it's gone incognito.
	public static int MIN_INCOGNITO_PERIOD = SECONDS_PER_HOUR * 2;

	// The amount of time it takes for a nation's homeland orb shards to completely fill.
	public static int SHARD_FILL_PERIOD = 24 * SECONDS_PER_HOUR;

	// The maximum number of storage/sharing builds that a nation may have at one time.
	public static int MAX_NUM_SHARE_BUILDS = 8;

	// The amount of time it takes for a nation's storage structures to completely fill.
	public static int STORAGE_FILL_PERIOD = 48 * SECONDS_PER_HOUR;

	// The fraction of manpower or energy stored in a captured storage structure, that the caturing nation receives. May be decreased to prevent abuses.
	public static float STORAGE_FRACTION_TRANSFERRED = 1f;

	// If energy burn rate rises above full energy generation rate, it is incresed so that the ratio (energy burn rate / full energy generation rate) is increased by this power.
	public static float OVERBURN_POWER = 3f;

	// Stat combination multipliers
	public static float STAT_COMBO_WEAK = 0.5f;
	public static float STAT_COMBO_STRONG = 2.0f;

	// Vacant block data values
	public static int VACANT_BLOCK_HIT_POINTS = 4;
	public static int VACANT_BLOCK_HIT_POINTS_RATE = 16;

	// The portion of a defense's hit points that are in effect if the defense is not yet complete.
	public static float INCOMPLETE_DEFENSE_HIT_POINTS_PORTION = 0.5f;

	// The portion of a defense's hit points that are in effect immediately upon being recaptured by its owner.
	public static float RECAPTURED_DEFENSE_HIT_POINTS_PORTION = 0.25f;

	// The hard limit on the number of simultaneous processes one user may have going simultaneously.
	public static int SIMULTANEOUS_PROCESS_LIMIT = 10;

  // Fractions of stats that are used for homeland and raids.
	public static float MANPOWER_MAX_HOMELAND_FRACTION = 0.1f;
	public static float MANPOWER_RATE_HOMELAND_FRACTION = 2.0f;
	public static float ENERGY_RATE_HOMELAND_FRACTION = 0.2f;
	public static float ENERGY_RATE_RAIDLAND_FRACTION = 0.1f;
	public static float SUPPORTABLE_AREA_HOMELAND_FRACTION = 0.2f;
	public static float SUPPORTABLE_AREA_RAIDLAND_FRACTION = 0.2f;

	// Nation data initial values
	public static float INIT_ENERGY = 3720;
	public static float INIT_ENERGY_MAX = 3720;
	public static float INIT_ENERGY_RATE = 310;
	public static float INIT_MANPOWER = 20000;
	public static float INIT_MANPOWER_MAX = 20000;
	public static float INIT_MANPOWER_RATE = 800;
	public static float INIT_MANPOWER_PER_ATTACK = 10;
	public static float INIT_STAT_TECH = 5;
	public static float INIT_STAT_BIO = 5;
	public static float INIT_STAT_PSI = 5;
	public static float INIT_GEO_EFFICIENCY_MODIFIER = 0.0f;
	public static float INIT_XP_MULTIPLIER = 1.0f;
	public static float INIT_HIT_POINT_BASE = 6;
	public static float INIT_HIT_POINTS_RATE = 12;
	public static float INIT_CRIT_CHANCE = 0.0f;
	public static float INIT_SALVAGE_VALUE = 0.5f;
	public static float INIT_WALL_DISCOUNT = 0.0f;
	public static float INIT_STRUCTURE_DISCOUNT = 0.0f;
	public static float INIT_SPLASH_DAMAGE = 0.0f;
	public static int INIT_MAX_NUM_ALLIANCES = 2;
	public static int INIT_MAX_SIMULTANEOUS_PROCESSES = 2;

	// Geographic efficiency limits
	public static float GEO_EFFICIENCY_MIN = 0.05f;
	public static float GEO_EFFICIENCY_MAX = 1.0f;

	// Manpower and energy purchase packages
	public static int BUY_PACKAGE_FILL  = 0;
	public static int BUY_PACKAGE_50    = 1;
	public static int BUY_PACKAGE_10    = 2;

	// Purchasing manpower and energy
	public static float BUY_MANPOWER_BASE = 5f;
	public static float BUY_MANPOWER_MULT = 0.12f;
	public static float BUY_MANPOWER_DAILY_LIMIT = 0.5f;
	public static float BUY_MANPOWER_DAILY_ABSOLUTE_LIMIT = 1.0f;
	public static float BUY_MANPOWER_LIMIT_BASE = 2f;
	public static float BUY_ENERGY_BASE = 5f;
	public static float BUY_ENERGY_MULT = 0.25f;
	public static float BUY_ENERGY_DAILY_LIMIT = 0.5f;
	public static float BUY_ENERGY_DAILY_ABSOLUTE_LIMIT = 1.0f;
	public static float BUY_ENERGY_LIMIT_BASE = 2f;

	// Purchasing credits
	public static int NUM_CREDIT_PACKAGES = 4;
	public static int BUY_CREDITS_AMOUNT[] = new int[NUM_CREDIT_PACKAGES];
	public static float BUY_CREDITS_COST_USD[] = new float[NUM_CREDIT_PACKAGES];
	public static int CREDITS_EARNED_PER_DOLLAR = 100;

	// Battle flags
	public static final int BATTLE_FLAG_INERT          = 1;
	public static final int BATTLE_FLAG_CRIT           = 2;
	public static final int BATTLE_FLAG_TARGET_BLOCK   = 4;
	public static final int BATTLE_FLAG_FLANKED        = 8;
	public static final int BATTLE_FLAG_FIRST_CAPTURE  = 16;
	public static final int BATTLE_FLAG_DISCOVERY      = 32;
	public static final int BATTLE_FLAG_FAST_CRUMBLE   = 64;
	public static final int BATTLE_FLAG_TOTAL_DEFENSE  = 128;
	public static final int BATTLE_FLAG_INSURGENCY     = 256;

	// Process flags
	public static final int PROCESS_FLAG_FIRST_CAPTURE = 1;
	public static final int PROCESS_FLAG_DISCOVERY     = 2;

	// Amount of collateral damage a wipe defense does (to nations other than the triggering or hostile nations).
	public static final float WIPE_COLLATERAL_DAMAGE_AMOUNT = 0.3f;

	// Fraction of normal XP for capturing a square that a nation receives if their defensive structure accomplishes it.
	public static final float DEFENSE_XP_MULTIPLIER = 0.1f;

	// How long after a nation is last used may its last block be taken
	public static int TIME_SINCE_LAST_USE_ALLOW_NATION_VANQUISH = SECONDS_PER_HOUR * 24 * 14; // 14 days
	public static int TIME_SINCE_LAST_USE_ALLOW_FAVORED_NATION_VANQUISH = SECONDS_PER_HOUR * 24 * 500; // 500 days

	// How long after a user last logs in are they considered absentee
	public static int TIME_SINCE_LAST_LOGIN_ABSENTEE = 1209600; // 14 days

	// How long after a nation is last used does it no longer receive reward from Orbs.
	public static int TIME_SINCE_LAST_USE_DISABLE_GOALS = SECONDS_PER_HOUR * 48;

	// How long after a nation is last used does it enter total defense mode
	public static int TIME_SINCE_LAST_ACTIVE_TOTAL_DEFENSE = 7200; // 2 hours

	// Amount by which base hit points are multiplied if defender is in total defense mode.
	public static float TOTAL_DEFENSE_MULTIPLIER = 1.5f;

  // Level at which a nation no longer benefits from Total Defense when offline
  public static int TOTAL_DEFENSE_OBSOLETE_LEVEL = 110;

	// Radius of area that is checked to determine if a square is eligible for insurgency.
	public static int INSURGENCY_CHECK_RADIUS = 4;

	// Area that is checked to determine if a square is eligible for insurgency.
	public static float INSURGENCY_CHECK_AREA = (INSURGENCY_CHECK_RADIUS * 2 + 1) * (INSURGENCY_CHECK_RADIUS * 2 + 1);

  // Fraction of local squares that must belong to attacked nation for insurgency to apply.
  public static float INSURGENCY_LOCAL_AREA_FRACTION = 0.56f;

	// Amount by which base hit points are multiplied if defending square is subject to insurgency.
	public static float INSURGENCY_DEFENSE_MULTIPLIER = 1.5f;

	// Minimum number of logins of a username on two different devices, to associate those devices.
	public static int MIN_COMMON_LOGIN_TO_ASSOCIATE_DEVICES = 3;

	// Fealty tier level threshold
	public static int FEALTY_0_MIN_LEVEL = 1;
	public static int FEALTY_1_MIN_LEVEL = 40;
	public static int FEALTY_2_MIN_LEVEL = 80;

	// Fealty periods
	public static int FEALTY_0_PERIOD = SECONDS_PER_HOUR * 6;
	public static int FEALTY_1_PERIOD = SECONDS_PER_HOUR * 12;
	public static int FEALTY_2_PERIOD = SECONDS_PER_HOUR * 24;

	// Ad bonus types
	public static final int AD_BONUS_TYPE_RESOURCE  = 0;
	public static final int AD_BONUS_TYPE_ORB       = 1;
	public static final int AD_BONUS_TYPE_LEVEL     = 2;
	public static final int AD_BONUS_TYPE_QUEST     = 3;
	public static final int AD_BONUS_TYPE_RAID      = 4;
	public static final int AD_BONUS_TYPE_BLOCKS    = 5;

	// Ad bonus amount
	public static final int AD_BONUS_AMOUNT_RESOURCE = 6;
	public static final int AD_BONUS_AMOUNT_ORB = 30;
	public static final int AD_BONUS_AMOUNT_LEVEL = 15;
	public static final int AD_BONUS_AMOUNT_QUEST = 20;
	public static final int AD_BONUS_AMOUNT_RAID = 10;
	public static final int AD_BONUS_AMOUNT_BLOCKS = 2;

	// Probability (1/N) of adding to ad bonus if user has captured a block (and received XP for it).
	public static final int AD_BONUS_BLOCKS_PROBABILITY = 60;

	// Additions to ad bonus are halved if ad bonus already meets this threshold.
	public static int AD_BONUS_AMOUNT_HALVE_THRESHOLD = 25;
	public static int AD_BONUS_AMOUNT_SINGLE_THRESHOLD = 50;
	public static int AD_BONUS_AMOUNT_ZERO_THRESHOLD = 100;

	// XP
	public static int XP_ADVANCEMENT_BASE = 0;
	public static int XP_ADVANCEMENT_ADDER = 1000;
	public static float XP_ADVANCEMENT_MULTIPLIER = 1.0335f;

	// Array of points to reach each level
	public static final int NUM_LEVELS = 1000;
	public static int XP_PER_LEVEL[] = new int[NUM_LEVELS + 1];

	public static final int MAX_XP = 2100000000;

	// XP Types
	public static final int XP_ADMIN           = 0;
	public static final int XP_DEMO            = 1;
	public static final int XP_UNITE_TRANSFER  = 2;
	public static final int XP_UNITE_PENDING   = 3;
	public static final int XP_ATTACK          = 4;
	public static final int XP_DISCOVERY       = 5;
	public static final int XP_TOWER           = 6;
	public static final int XP_PATRON          = 7;
	public static final int XP_FOLLOWER        = 8;
	public static final int XP_FIRST_CAPTURE   = 9;
	public static final int XP_ORB             = 10;
	public static final int XP_QUEST           = 11;
	public static final int XP_RAID            = 12;
	public static final int XP_RAID_DEFENSE    = 13;
	public static final int XP_TOURNAMENT      = 14;
	public static final int XP_FARMING         = 15;

	// Number of credits given as a reward for each level gained.
	public static final int LEVEL_UP_REWARD_CREDITS = 10;

	// Minimum fraction of a nation's manpower per attack that an attack may cost.
	// This is necessary so that even attacking (purposefully) much weaker nations will have a significant manpower cost, and so will not provide "free" XP.
	// UPDATE: Instead of a min manpower cost per attack, I will factor geo efficiency into how much XP is earned from defeating a square. That should eliminate XP exploits even better, and in a way that makes more sense and is less artificial.
	public static float ATTACK_MIN_MANPOWER_COST = 0f;// 0.25f; // NOTE: If this remains 0, it can be removed from the game.

  // Degree to which attack results are exagerated based on imbalance of odds.
	public static float ATTACK_RESULT_RADICALIZATION_FACTOR = 1.0f;

	// How frequently to update goals.
	// Make this fairly infrequent, so that small amounts of XP, which are added as integers, add up correctly.
	public static int GOAL_UPDATE_PERIOD = 180;

  // How frequently to publish ranks data
	public static int RANKS_PUBLISH_PERIOD = 300;

	// Initial amount of game money given to a new nation
	public static int INIT_GAME_MONEY = 250;

	// Bonus game money given to a new nation that signs up with a bonus code
	public static int PATRON_CODE_BONUS_GAME_MONEY = 50;

	// Amount of game money given to a new nation upon first purchase of credits
	public static int FIRST_PURCHASE_BONUS_GAME_MONEY = 250;

	// Number of ranks to keep for the various ranks lists.
	public static int NUM_GLOBAL_PRIZE_RANKS = 125;
	public static int NUM_PRIZE_RANKS = 10;
	public static int NUM_XP_RANKS = 125;
	public static int NUM_FOLLOWERS_RANKS = 125;
	public static int NUM_ENERGY_DONATED_RANKS = 125;
	public static int NUM_MANPOWER_DONATED_RANKS = 125;
	public static int NUM_QUESTS_COMPLETED_RANKS = 125;
	public static int NUM_CAPTURES_RANKS = 125;
	public static int NUM_LEVEL_RANKS = 125;
	public static int NUM_REBIRTHS_RANKS = 125;
	public static int NUM_AREA_RANKS = 125;
	public static int NUM_MEDALS_RANKS = 125;

	// Number of ranks to list
	public static int RANKS_LIST_LENGTH_SMALL = 10;
	public static int RANKS_LIST_LENGTH_LARGE = 100;

	// How long after a reset will advances that increase manpower and energy storage fill up to the pre-reset level
	public static int POST_RESET_REPLACEMENT_WINDOW = 3600; // 1 hour

  // Each time a nation's advances are reset, it costs this much more to reset them again the next time.
	public static int RESET_ADVANCES_BASE_PRICE = 100;

	// Minimum amount of time that a nation's cycle must last before rebirth is allowed.
	public static int MIN_NATION_CYCLE_TIME = SECONDS_PER_DAY * 14; // 2 weeks

	// Amount at which rebirth countdown begins
	public static int REBIRTH_COUNTDOWN_START = 500;

	// Level at which rebirth becomes available (before adding rebirth level bonus)
	public static int REBIRTH_AVAILABLE_LEVEL = 100;

	// Level above which countdown to rebirth accelerates
	public static int REBIRTH_COUNTDOWN_ACCELERATE_LEVEL = 20;

	// Power to which excess level is raised, to accelerate rebirth countdown.
	public static double REBIRTH_COUNTDOWN_ACCELERATE_POWER = 1.2;

	// The maximum amount of rebirth countdown that a nation may purchase per cycle.
	public static int MAX_REBIRTH_COUNTDOWN_PURCHASED = 1000;

	// The bonus number of levels that a nation receives for each rebirth it has gone through.
	public static int REBIRTH_LEVEL_BONUS = 1;

	// The maximum sum rebirth level bonus a nation can accumulate.
	public static int MAX_REBIRTH_LEVEL_BONUS = 30;

	// Amount of game money a nation is given at rebirth
	public static int REBIRTH_GAME_MONEY = 1000;

	// The number of credits purchased that results in 1 being added to the nation's rebirth countdown.
	public static int REBIRTH_COUNTDOWN_INCREMENT_PURCHASE_AMOUNT = 25;

	// The base level (before adding level bonus) that a nation rebirths to.
	public static int REBIRTH_TO_BASE_LEVEL = 30;

	// A nation will go into suspension only after it has not won prize money in a full day.
	public static int SUSPEND_TIME_SINCE_LAST_WINNINGS = SECONDS_PER_DAY;

	public static int MIN_WINNINGS_TO_CASH_OUT = 1000;

	public static int CREDITS_PER_CENT_TRADED_IN = 1;

	// Battle duration in seconds
	public static int BATTLE_DURATION = 6;

	// The duration of a tower's attack action in seconds.
	public static int TOWER_ACTION_DURATION = 6;

	// The period of time after a defensive tower is recaptured by its owner, before it can be activated again.
	public static int TOWER_REBUILD_PERIOD = 60;

	// Types of process
	public static int PROCESS_EVACUATE = 0;
	public static int PROCESS_OCCUPY   = 1;
	public static int PROCESS_BATTLE   = 2;

  // Process duration in seconds
	public static int PROCESS_DURATION = 2;

	// Probabilities that a random discovery will occur, when a block is occupied, or conquered.
	public static float DISCOVERY_PROBABILITY_OCCUPY = 0.02f;
	public static float DISCOVERY_PROBABILITY_CONQUER = 0.1f;

	// Discoveries and supply lines
	public static float SUPPLY_LINE_PROBABILITY = 0.20f;
	public static int MIN_SUPPLY_LINE_TRANSFER_AMOUNT = 500;
	public static float SUPPLY_LINE_AMOUNT_STOLEN = 0.25f;
	public static int DISCOVER_SUPPLY_LINE_XP = 100;
	public static int DISCOVER_ADVANCE_XP = 50;

	// Time after a block has been captured before the object in it crumbles.
	public static int TIME_UNTIL_CRUMBLE = SECONDS_PER_HOUR * 10; // 10 hours

	// Time after being captured before an object crumbles, if it is to crumble upon capture.
	public static int TIME_UNTIL_FAST_CRUMBLE = 45;

	// Probablity that a defense will crumble immediately after being captured from its owner.
	public static float CRUMBLE_UPON_CAPTURE_PROBABILITY = 0.2f;

  // If mean use interval drops below this number, messages are ignored, or client is booted.
	public static float MEAN_USE_INTERVAL_IGNORE_THRESHOLD = 200; // 0.2 seconds
	public static float MEAN_USE_INTERVAL_BOOT_THRESHOLD = 120; // 0.12 seconds

	// Weights to give old and new use intervals to determine new mean.
	static float NEW_USE_INTERVAL_WEIGHT = 0.15f;
	static float OLD_USE_INTERVAL_WEIGHT = 0.85f;

	// Weights to give old and new chat intervals to determine new mean.
	static float NEW_CHAT_INTERVAL_WEIGHT = 0.2f;
	static float OLD_CHAT_INTERVAL_WEIGHT = 0.8f;

	// If mean chat interval drops below this number, chat is ignored from general channel.
	public static float MEAN_CHAT_INTERVAL_SPAMMY = 3000; // 3 seconds

	// Max length of a line of chat
	static int MAX_CHAT_LINE_LENGTH = 200;

	// Max length of a flag description
	static int MAX_FLAG_DESC_LENGTH = 50;

	// Max number of map flags a nation may have
	static int MAX_FLAG_COUNT = 1000;

	// Max length of a message
	static int MAX_MESSAGE_LENGTH = 500;

	// How long a message lasts before expiring
	static int MESSAGE_DURATION = SECONDS_PER_DAY * 7;

	// Maximum number of messages a nation may have in its message list.
	static int MAX_NUM_MESSAGES = 1000;

	// Maximum number of messages that a nation may post in one day
	static final int MAX_MESSAGE_SEND_COUNT_PER_DAY = 10;

	// Message types
	static final int MESSAGE_TYPE_GAME   = 0;
	static final int MESSAGE_TYPE_NATION = 1;
	static final int MESSAGE_TYPE_OTHER  = 2;

	// How many messages of one type sent in each "paging" batch.
	static final int MESSAGE_BATCH_SIZE = 20;

	// Maximum number of alliance requests that a nation may send in one day
	static final int MAX_ALLIANCE_REQUEST_COUNT_PER_DAY = 10;

	// Price of resetting a nation voluntarily
	static int RESET_NATION_PRICE = 150;

	// How long a user account must be inactive before it's deleted (if it has no nation)
	static int USER_ACCOUNT_EXPIRE_TIME = 3888000; // 45 days

	// How long a player record must have no users before it's deleted
	static int PLAYER_ACCOUNT_EXPIRE_TIME = 3888000; // 45 days

	// Amount of time between UpdateDatabase completions before it's considered broken (in seconds)
	static long UPDATE_DATABASE_BROKEN_DELAY_SECONDS = 5400; // 90 minutes

	// How long UpdateDatabase should sleep between attempts to execute
	static int UPDATE_DATABASE_WAIT_SLEEP_MILLISECONDS = 1000; // 1 second

	// How long UpdateDatabase should sleep at intervals between object updates
	static int UPDATE_DATABASE_SLEEP_MILLISECONDS = 50; // .05 seconds

	// How long the UpdateThread should sleep between updates
	static int UPDATE_THREAD_SLEEP_MILLISECONDS = 90000; // 90 seconds

	// How long UpdateThread should sleep between nations when doing an hourly update
	static int UPDATE_PER_NATION_SLEEP_MILLISECONDS = 10; // 10 milliseconds

	// How long UpdateThread should sleep between users when doing a daily update
	static int DAILY_UPDATE_PER_USER_SLEEP_MILLISECONDS = 1; // 1 milliseconds

	// How long the BackupThread should sleep between checks
	static int BACKUP_THREAD_SLEEP_MILLISECONDS = 60000; // 1 minutes

	// How long the EmailThread should sleep between checks
	static int EMAIL_THREAD_WAIT_SLEEP_MILLISECONDS = 15000; // 15 seconds

	// How long a ClientThread should sleep between checks for new client socket connection
	static int CLIENT_THREAD_SLEEP_MILLISECONDS = 100; // 1/10 second

	// How long to wait after a purchase before sending report to nation
	static int PURCHASE_REPORT_DELAY = 15;

	// Minimum period between free migrations.
	static int FREE_MIGRATION_PERIOD = SECONDS_PER_DAY;

	// Cost of a paid migration.
	static int MIGRATION_COST = 60;

	// Cost to unite
	static int UNITE_COST = 500;

	// Minimum period between unites.
	static int UNITE_PERIOD = SECONDS_PER_DAY * 2;

	// Max fraction of the united nation's energy and manpower that is allowed to be transferred over via unite.
	static float UNITE_TRANSFER_RESOURCE_FRACTION = 0.25f;

	// Maximum amount of pending XP from a unite that a nation may receive per hour.
	static int UNITE_PENDING_XP_PER_HOUR = 100000;

	// Cost to customize nation appearance.
	static int CUSTOMIZE_COST = 300;

	// Constants used to determine a nation's supportable area
	static int SUPPORTABLE_AREA_BASE = 300;
	static int SUPPORTABLE_AREA_PER_LEVEL = 5;

	// Max extent of a nation
	static int NATION_MAX_EXTENT = 10000; // Was 500. This limitation is now disabled by setting it to a very high number.

	// Starting radius around user view coords at which migrating nation will be placed.
	static int MIGRATE_PLACEMENT_RADIUS = 40;

	// The maximum allowed number of bad passwords allowed, and the period in which they're allowed.
	static int BAD_PASSWORD_MAX_COUNT = 3;
	static int BAD_PASSWORD_PERIOD = 60 * 5; // 5 minutes

	// The amount of time that an event remains in a nation's history
	static int HISTORY_EVENT_DURATION = SECONDS_PER_DAY * 7;

	// Chat reports
	public static int MAX_REPORTS_PER_DAY = 40;

	// Report duration
	public static int LONG_TERM_REPORT_DURATION = 3 * SECONDS_PER_DAY; // 3 days
	public static int SHORT_TERM_REPORT_DURATION = 30 * 60; // 30 minutes

	// Duration of log_suspect logging
	public static long LOG_SUSPECT_FINE_DURATION = 2 * 60 * 1000; // 2 minutes

	// For daily tech prices update
	static int UPDATE_PRICE_MIN_PLAY_TIME = SECONDS_PER_HOUR * 25; // Min play availability time to update tech's price INCREASE WHEN THERE ARE MORE PLAYERS
	static int MIN_TECH_PRICE = 2; // Minimum technology price
	static int MAX_TECH_PRICE = 5000; // Maximum technology price

	// Age of a user after which any new nations created by associated users will be considered veteran.
	public static int VETERAN_USER_AGE = 90 * SECONDS_PER_DAY; // 90 days

	// Age of a nation after which, if it rebirths or unites into another nation, it will be considered veteran.
	public static int VETERAN_NATION_AGE = 90 * SECONDS_PER_DAY; // 90 days

  //Chat Channels
  public static int CHAT_CHANNEL_GENERAL = 10000000;
  public static int CHAT_CHANNEL_ALLIES  = 10000001;

  // Max number of nations in chat list and blocked list
  public static int MAX_NUM_CHAT_LIST              = 50;

	// The duration of a chat ban
	public static int CHAT_BAN_DURATION = 24 * Constants.SECONDS_PER_HOUR; // 24 hours

	public static float MEDIUM_OFFENSE_LEVEL_THRESHOLD = 3.0f;
	public static float HIGH_OFFENSE_LEVEL_THRESHOLD = 10.0f;

	public static int CHAT_BAN_REPORTS_COUNT__LOW_OFFENSE     = 6;
	public static int CHAT_BAN_REPORTS_COUNT__MEDIUM_OFFENSE  = 4;
	public static int CHAT_BAN_REPORTS_COUNT__HIGH_OFFENSE    = 2;

  // Player penalties
  public static final int PENALTY_PENALIZE_COMPLAINER    = 0;
  public static final int PENALTY_WARNING                = 1;
  public static final int PENALTY_CHAT_BAN_3_HOUR        = 2;
  public static final int PENALTY_CHAT_BAN_6_HOUR        = 3;
  public static final int PENALTY_CHAT_BAN_12_HOUR       = 4;
  public static final int PENALTY_CHAT_BAN_1_DAY         = 5;
  public static final int PENALTY_CHAT_BAN_3_DAY         = 6;
  public static final int PENALTY_CHAT_BAN_7_DAY         = 7;
  public static final int PENALTY_GAME_BAN_1_DAY         = 8;
  public static final int PENALTY_GAME_BAN_3_DAY         = 9;

	// Actions that can be taken in response to a complaint
	public static final int COMPLAINT_ACTION_NO_ACTION = 0;
  public static final int COMPLAINT_ACTION_WARN = 1;
  public static final int COMPLAINT_ACTION_CHAT_BAN = 2;
  public static final int COMPLAINT_ACTION_GAME_BAN = 3;
  public static final int COMPLAINT_ACTION_WARN_FILER = 4;
  public static final int COMPLAINT_ACTION_CHAT_BAN_FILER = 5;

	// Update flags
	public static int UPDATE_FLAG_ALLOW_PAYMENTS = 1;
	public static int UPDATE_FLAG_SPENT_MONEY    = 4;

	// Available hours (read from server_info.txt)
	public static int HOUR_WEEKDAY_FIRST;
	public static int HOUR_WEEKDAY_LAST;
  public static int HOUR_WEEKEND_FIRST;
	public static int HOUR_WEEKEND_LAST;
	public static String UNAVAILABLE_MESSAGE;

	// The share of earned XP and purchased credits that are sent to a user's patron and followers.
	public static float PATRON_XP_SHARE = 0.05f;
	public static float PATRON_CREDITS_SHARE = 0.10f;
	public static float FOLLOWER_XP_SHARE = 0.05f;
	public static float FOLLOWER_CREDITS_SHARE = 0.10f;

	// Server log flags
	public static int LOG_AWARDS      = 1;
	public static int LOG_LOAD        = 2;
	public static int LOG_EVENTS      = 4;
	public static int LOG_ATTACK      = 8;
	public static int LOG_LOGIN       = 16;
	public static int LOG_ENTER       = 32;
	public static int LOG_CHAT        = 64;
	public static int LOG_SEND        = 128;
  public static int LOG_UPDATE      = 256;
	public static int LOG_INPUT       = 512;
	public static int LOG_DEBUG       = 1024;

	// User ranks
	public static final int RANK_SOVEREIGN    = 0; // Change nation color
	public static final int RANK_COSOVEREIGN  = 1; // Delete others' messages, migrate, customize appearance, unite, transfer credits
	public static final int RANK_GENERAL      = 3; // Change join password, remove member, build, upgrade, complete builds, collect on quests, join tournament
	public static final int RANK_CAPTAIN      = 4; // Buy manpower or energy, promote/demote, research advances, salvage
	public static final int RANK_COMMANDER    = 6; // Purchase tech, delete flags, modify chat list
	public static final int RANK_WARRIOR      = 9; // Attack, evacuate
	public static final int RANK_CIVILIAN     = 12;
	public static final int RANK_NONE         = 13;

	// Period between data backups, in seconds.
	public static int BACKUP_PERIOD = SECONDS_PER_HOUR * 24 * 7; // 7 days

	// Offset from backup period, in seconds.
	public static int BACKUP_PERIOD_OFFSET = SECONDS_PER_HOUR * 3; // 3 hours (Back up at 12)

	// Status logging
	public static int LOG_STATUS_PERIOD = 300; // 5 Minutes
	public static int LOG_STATUS_HISTORY_LEN = 288; // 24 hours worth
	public static int LOG_STATUS_ALERT_DELAY = 300; // 5 Minutes
	public static int LOG_BACKUP_THREAD_STATUS_ALERT_DELAY = 1800; // 30 Minutes

	// Length of output string buffer
	public static final int OUTPUT_BUFFER_LENGTH = 65536;//8192;

  // Chat offense level updating
  public static final float OFFENSE_LEVEL_MULTIPLIER_PER_DAY = 0.9f;
  public static final float OFFENSE_LEVEL_MIN_THRESHOLD = 0.1f;

	// Two PI
	public static double TWO_PI = 2 * Math.PI;

	public static String home_dir="";
	public static String public_gen_dir="";
	public static String private_gen_dir="";
	public static String php_exe="";
	public static int port=0;
	public static int client_version=0;
	public static int max_num_clients=0;
	public static int max_num_clients_per_nation=0;
	public static String ftp_host = "";
	public static String ftp_username = "";
	public static String ftp_password = "";
	public static String account_db_url = "";
	public static String account_db_user = "";
	public static String account_db_pass = "";
	public static String game_db_url = "";
	public static String game_db_user = "";
	public static String game_db_pass = "";
	public static boolean admin_login_only;
	public static String admin_login_ip;
	public static int server_id=0;
	public static String server_db_name = "";
	public static boolean update_for_downtime = false;
	public static int min_level_limit = 0;
	public static int max_level_limit = 0;
	public static int mid_level_limit = 0;
	public static float mid_level_limit_pos = 0;
	public static float new_player_area_boundary = 0;
	public static int max_accounts_per_period = 0;
	public static int max_accounts_period = 0;
	public static int max_buy_credits_per_month = 0;
	public static float manpower_gen_multiplier = 0;
	public static int max_nation_members = 0;
	public static boolean allow_credit_purchases = false;
	public static boolean ad_bonuses = false;
	public static boolean cash_out_prizes = false;
	public static float prize_dollars_awarded_per_day = 0;

	public static String client_maps_dir="";
	public static String backup_dir="";
	public static String log_dir="";
	public static String nation_log_dir="";
	public static String ranks_dir="";
	public static String publiclog_dir="";

	public static Random random = new Random();

	public static Hashids hashids = new Hashids("War of Conquest Hashids salt", 4, "abcdefghjkmnprstuvwxyz23456789");

	public static HashMap<Integer,Float> orb_payments_per_hour = new HashMap<Integer,Float>();

	static int hour, minute, second, month, day, date, year, absolute_day;
	static int time;
	static long fine_time;

	// UTF charset for encoding and decoding unicode characters.
	static Charset charset = Charset.forName("UTF-8");

  // Maximum sum of payments allowed from one player in one day
  //public static float MAX_PAYMENT_SUM_PER_DAY = 100.0f;
  public static float MAX_PAYMENT_SUM_PER_DAY = 0;

	static String wait_filename = "";
	static int wait_start_time;
	static int SERVER_UPDATE_PERIOD = 5; // 5 seconds
	static int COMPLETE_UPDATE_FREQUENCY = 6;
	static long SERVER_SLEEP_MILLISECONDS = 20;
	static int MAX_WAIT_TIME = 10000; // 10 seconds
	static int NUM_CLIENT_BUCKETS = 250;
	static int NUM_NATION_BUCKETS = 200;

	// Test flag
	public static boolean test_flag = false;

  public static final int ALLY_LEVEL_DIFF_LIMIT = 30;

	public static void Init()
	{
		// Read the server configuration file
		ReadServerConfig();

		// Construct directory paths
		backup_dir = private_gen_dir + "backup";
		log_dir = private_gen_dir + "log/";
		nation_log_dir = private_gen_dir + "nationlog/";
		client_maps_dir = public_gen_dir + "clientmaps/";
		ranks_dir = public_gen_dir + "ranks/";
		publiclog_dir = public_gen_dir + "publiclog/";

		// Make sure that all the necessary directories exist.
		Constants.EnsureDirExists(Constants.backup_dir);
		Constants.EnsureDirExists(Constants.log_dir);
		Constants.EnsureDirExists(Constants.nation_log_dir);
		Constants.EnsureDirExists(Constants.client_maps_dir);
		Constants.EnsureDirExists(Constants.ranks_dir);
		Constants.EnsureDirExists(Constants.publiclog_dir);
		Constants.EnsureDirExists(Constants.backup_dir + "/daily");
		Constants.EnsureDirExists(Constants.backup_dir + "/weekly");
		Constants.EnsureDirExists(Constants.backup_dir + "/monthly");


		XP_PER_LEVEL[0] = XP_PER_LEVEL[1] = 0;

		// Fill in the array of xp per level
		int total = XP_ADVANCEMENT_BASE;
		float multiplier = 1f;
		for (int i = 2; i <= NUM_LEVELS; i++)
		{
			total += XP_ADVANCEMENT_ADDER;
			XP_PER_LEVEL[i] = (int)((float)total * multiplier);
			multiplier *= XP_ADVANCEMENT_MULTIPLIER;
			//Output.PrintToScreen("Level " + i + " XP: " + (XP_PER_LEVEL[i] - XP_PER_LEVEL[i - 1]) + ", total: " + XP_PER_LEVEL[i]);
		}

		if (NUM_CREDIT_PACKAGES > 3)
		{
			// Record credit package prices
			BUY_CREDITS_AMOUNT[0]    = 500;
			BUY_CREDITS_COST_USD[0]  = 4.99f;
			BUY_CREDITS_AMOUNT[1]    = 1200;
			BUY_CREDITS_COST_USD[1]  = 9.99f;
			BUY_CREDITS_AMOUNT[2]    = 2500;
			BUY_CREDITS_COST_USD[2]  = 19.99f;
			BUY_CREDITS_AMOUNT[3]    = 7000;
			BUY_CREDITS_COST_USD[3]  = 49.99f;
		}

		// Initial time update
		UpdateTime();
	}

	public static int GetServerID()
	{
		return server_id;
	}

	public static String EncodePatronCode(int _serverID, int _userID)
	{
		String code = hashids.encode(_serverID, _userID);

		// Depending on length, insert a first '-' if necessary.
		if (code.length() >= 5) {
			code = code.substring(0, 3) + "-" + code.substring(3);
		}

		// Depending on length, insert a second '-' if necessary.
		if ((code.length() == 8) || (code.length() == 9)) {
			code = code.substring(0, 6) + "-" + code.substring(6);
		} else if (code.length() > 9) {
			code = code.substring(0, 7) + "-" + code.substring(7);
		}

		return code;
	}

	public static void DecodePatronCode(String _patron_code, long[] _results)
	{
		long[] results = hashids.decode(_patron_code.replace("-", "").toLowerCase());
		//Output.PrintToScreen("DecodePatronCode() _patron_code: " + _patron_code + ", result length: " + results.length + ", output length: " + _results.length);

		if (results.length < 2)
		{
			_results[0] = -1;
			_results[1] = -1;
		}
		else
		{
			_results[0] = results[0];
			_results[1] = results[1];
		}
	}

	public static void UpdateTime()
	{
		Date whole_date = new Date();
		fine_time = whole_date.getTime();
		time = (int)(fine_time / 1000);
    absolute_day = time / Constants.SECONDS_PER_DAY;
		hour = whole_date.getHours();
		minute = whole_date.getMinutes();
		second = whole_date.getSeconds();
		month = whole_date.getMonth();
		day = whole_date.getDay();
		date = whole_date.getDate();
		year = whole_date.getYear();
	}

	public static int GetHour()
	{
		return hour;
	}

	public static int GetMinute()
	{
		return minute;
	}

	public static int GetSecond()
	{
		return second;
	}

	public static int GetMonth()
	{
		return month;
	}

	public static int GetDay()
	{
		return day;
	}

	public static int GetDate()
	{
		return date;
	}

	public static int GetYear()
	{
		return year;
	}

  public static int GetAbsoluteDay()
	{
		return absolute_day;
	}

	public static int GetFullMonth()
	{
		return (year * 12) + month;
	}

	public static int GetSecondsLeftInAbsoluteDay()
	{
		return SECONDS_PER_DAY - (time % Constants.SECONDS_PER_DAY);
	}

	public static int GetTime()
	{
		return time;
	}

	public static long GetFineTime()
	{
		return fine_time;
	}

	public static long GetFreshFineTime()
	{
		Date date = new Date();
		return date.getTime();
	}

	public static String GetFullDate()
	{
		Date date = new Date();
		return date.toString();
	}

	public static String GetTimestampString()
	{
		SimpleDateFormat sdfDate = new SimpleDateFormat("EEE, MMM d yyyy, HH:mm");
    Date now = new Date();
    String strDate = sdfDate.format(now);
    return strDate;
	}

	public static String GetDateString()
	{
		return (Constants.GetMonth() + 1) + "/" + Constants.GetDate() + "/" + (Constants.GetYear() + 1900);
	}

	public static String GetShortDateString()
	{
		return (Constants.GetMonth() + 1) + "/" + Constants.GetDate();
	}

	public static String GetShortTimeString()
	{
		return Constants.GetHour() + ":" + (Constants.GetMinute() < 10 ? "0" : "") + Constants.GetMinute() + ":" + (Constants.GetSecond() < 10 ? "0" : "") + Constants.GetSecond();
	}

	public static String GetTimeAndDateString(long _fine_time)
	{
		// Derive date and time string from given _fine_time
		Date given_date = new Date(_fine_time);
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(given_date);
		return (calendar.get(Calendar.MONTH) + 1) + "/" + calendar.get(Calendar.DAY_OF_MONTH) + "/" + calendar.get(Calendar.YEAR) + ", " + calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE) + " EST";
	}

	public static String GetRankString(int _rank)
	{
		switch (_rank)
		{
			case RANK_SOVEREIGN: return "sovereign";
			case RANK_COSOVEREIGN: return "cosovereign";
			case RANK_GENERAL: return "general";
			case RANK_CAPTAIN: return "captain";
			case RANK_COMMANDER: return "commander";
			case RANK_WARRIOR: return "warrior";
			case RANK_CIVILIAN: return "civilian";
			default: return "INVALID RANK";
		}
	}

	public static String Execute(String _command, boolean _wait)
	{
		Process process;
		InputStream input_stream;

		// Execute the given command
		try{
			process = Runtime.getRuntime().exec(_command);
		}catch(IOException e){
			Output.PrintTimeToScreen("Couldn't execute command " + _command + "; " + e.getMessage()); return "Error";
		}

		// If we're not to wait for process to finish, return.
		if (!_wait) {
			return "";
		}

		// Get the process's input stream
		input_stream = process.getInputStream();

		// Wait for the process to terminate before continuing
		try{
			process.waitFor();
		}catch(InterruptedException e){
			Output.PrintTimeToScreen("Wait for command to complete was interrupted."); return "Error";
		}

		// Read first bytes of output from the input stream
		byte[] bytes = new byte[1000];
		try{
			input_stream.read(bytes);
			input_stream.close();
		}catch(IOException e){
			Output.PrintTimeToScreen("Failure reading command output from stream."); return "Error";
		}

		String output_string = new String(bytes);
		String message = Constants.FetchParameter(output_string, "message", false);

		// Return output as string
		return message;
	}

	public static void ExecuteWaitSilent(String _command)
	{
		Process process;

		// Execute the given command
		try{
			process = Runtime.getRuntime().exec(_command);
		}catch(IOException e){
			Output.PrintTimeToScreen("Couldn't execute command " + _command); return;
		}

		try
		{
			InputStream stderr = process.getErrorStream();
			InputStream outstr = process.getInputStream();
			InputStreamReader err_isr = new InputStreamReader(stderr);
			InputStreamReader out_isr = new InputStreamReader(outstr);
			BufferedReader err_br = new BufferedReader(err_isr);
			BufferedReader out_br = new BufferedReader(out_isr);
			String line = null;

			while ( (line = out_br.readLine()) != null){}
//				System.out.println(line);

			while ( (line = err_br.readLine()) != null){}
//		    System.out.println(line);
		}
		catch (Exception ex)
    {
      Output.PrintException(ex);
    }

		// Wait for the process to terminate before continuing
		try{
			process.waitFor();
		}catch(InterruptedException e){
			Output.PrintTimeToScreen("Wait for command to complete was interrupted."); return;
		}
	}

  public static void CopyFile(String _source_path, String _dest_path)
  {
    try
    {
      FileInputStream fis  = new FileInputStream(new File(_source_path));
      FileOutputStream fos = new FileOutputStream(new File(_dest_path));
      byte[] buf = new byte[1024];
      int i = 0;
      while((i=fis.read(buf))!=-1) {
        fos.write(buf, 0, i);
      }
      fis.close();
      fos.close();
    }catch(Exception e){
			Output.PrintTimeToScreen("Exception in CopyFile():");
      Output.PrintException(e);
		}
  }

	public static void WaitForFile(String _filename)
	{
		File file;

		for(;;)
		{
			file = new File(_filename);
			if (file.exists()) {
				break;
			}

			try {
//Output.PrintTimeToScreen("About to sleep.");
				Thread.sleep(5);
			}catch(Exception e){
				Output.PrintTimeToScreen("Sleep interupted.");
			}
		}
	}

	public static void SetWaitFilename(String _wait_filename)
	{
		wait_filename = _wait_filename;
		wait_start_time = GetTime();
	}

	public static void AwaitWaitFile()
	{
		// If the wait_filename is not set, return.
		if (wait_filename.equals("")) {
			return;
		}

		if ((Constants.GetTime() - wait_start_time) > MAX_WAIT_TIME) {
			wait_filename = "";
			return;
		}

		// Wait for the file with the wait_filename to exist
		WaitForFile(wait_filename);

		// Clear the wait_filename
		wait_filename = "";
	}

	public static void EnsureDirExists(String _dir)
	{
		File f = new File(_dir);

		if (f.exists())
		{
			if (!f.isDirectory())
			{
				Output.PrintToScreen("ERROR: '" + _dir + "' exists but is not a directory!");
			}
		}
		else
		{
			try
			{
        f.mkdir();
        Output.PrintToScreen("Created directory '" + _dir + "'.");
			}
			catch(SecurityException se)
			{
				Output.PrintToScreen("ERROR: Failed to create directory '" + _dir + "'.");
			}
		}
	}

	public static void SendEmail(String _from, String _from_name, String _address, String _subject, String _body)
	{
		// Queue the email to be sent
		EmailThread.QueueEmail(_from, _from_name, _address, _subject, _body);
	}

	public static boolean StringContainsSwear(String _string)
	{
		// Make the string lower case
		_string = _string.toLowerCase();

		if (_string.indexOf("fuck") != -1) return true;
		if (_string.indexOf("fuk") != -1) return true;
    if (_string.indexOf("fvck") != -1) return true;
		if (_string.indexOf("fvk") != -1) return true;
		if (_string.indexOf("cock") != -1) return true;
		if (_string.indexOf("dick") != -1) return true;
		if (_string.indexOf("penis") != -1) return true;
		if (_string.indexOf("vagina") != -1) return true;
		if (_string.indexOf("cunt") != -1) return true;
		if (_string.indexOf("pussy") != -1) return true;
		if (_string.indexOf("pussies") != -1) return true;
		if (_string.indexOf("twat") != -1) return true;
		if (_string.indexOf("fag") != -1) return true;
		if (_string.indexOf("homo") != -1) return true;
		if (_string.indexOf("queer") != -1) return true;
		if (_string.indexOf("nigger") != -1) return true;
		if (_string.indexOf("shit") != -1) return true;
		if (_string.indexOf("asshole") != -1) return true;
		if (_string.indexOf("fuck") != -1) return true;
		if (_string.indexOf("bitch") != -1) return true;
		if (_string.indexOf("bastard") != -1) return true;
		if (_string.indexOf("nazi") != -1) return true;
		if (_string.indexOf("hitler") != -1) return true;
		if (_string.indexOf("jesus") != -1) return true;
		if (_string.indexOf("christ") != -1) return true;
		if (_string.indexOf("yahweh") != -1) return true;
		if (_string.indexOf("allah") != -1) return true;
		if (_string.indexOf("mohammed") != -1) return true;
		if (_string.indexOf("admin") != -1) return true;
		if (_string.indexOf("moderator") != -1) return true;
		if (_string.indexOf("mike") != -1) return true;

		return false;
	}

	public static boolean StringContainsIllegalWhitespace(String _string)
	{
		if (_string.length() == 0) {
			return false;
		}

		// Check whether string starts with whitespace
		if (Character.isWhitespace(_string.charAt(0))) {
			return true;
		}

		// Check whether string ends with whitespace
		if (Character.isWhitespace(_string.charAt(_string.length() - 1))) {
			return true;
		}

		// Check for 2 whitespace characters in a row.
		boolean prev_is_whitespace = false;
		for (int i = 0; i < _string.length(); i++)
		{
			if (Character.isWhitespace(_string.charAt(i)))
			{
				if (prev_is_whitespace) {
					return true;
				}

				prev_is_whitespace = true;
			}
			else
			{
				prev_is_whitespace = false;
			}
		}

		return false;
	}

	public static String RemoveControlCharacters(String _string)
	{
		return _string.replaceAll("\\p{Cntrl}", "");
	}

	public static boolean FileExists(String _filename)
	{
		File varTmpDir = new File(_filename);
		return varTmpDir.exists();
	}

	public static String GetNextTabSeparatedValue(String _line, int [] _place)
	{
		String return_val = "";
		int next_tab_index = _line.indexOf('\t', _place[0]);

		if (next_tab_index == -1)
		{
			if (_place[0] < _line.length())
			{
				return_val = _line.substring(_place[0]);
				_place[0] = _line.length();
			}
		}
		else
		{
			if (next_tab_index > _place[0])
			{
				return_val = _line.substring(_place[0], next_tab_index);
			}

			_place[0] = next_tab_index + 1;
		}

		return return_val;
	}

	public static String FetchParameter(String _param_list, String _param_name, boolean _to_end)
	{
		// Determine place of parameter name within parameter list
		int place = _param_list.toLowerCase().indexOf(_param_name.toLowerCase() + "=");
		//Output.PrintToScreen("FetchParameter(" + _param_list + ", " + _param_name + ", " + _to_end + ") place: " + place);

    // If parameter not found, return empty string
		if (place == -1) {
			return "";
		}

    // Determine place of beginning of parameter value string
		place = _param_list.indexOf('=', place);

		// If equal sign not found, return empty string
		if (place == -1) {
			return "";
		}

    // Determine place of end of this parameter's value
		int end_place = _param_list.indexOf('|', place);

    // If this is the last parameter, or if _to_end is given as true, end place is end of parameter list
		if ((end_place == -1) || _to_end)
		{
			// End place is end of string
			end_place = _param_list.length();
		}

    // Isolate the value string
		String value_string = _param_list.substring(place + 1, end_place);

		return value_string;
	}

	public static int FetchParameterInt(String _param_list, String _param_name)
	{
		String val_string = FetchParameter(_param_list, _param_name, false);

		try {
			return (val_string.equals("")) ? 0 : Integer.parseInt(val_string);
		}
    catch (Exception e) {
			return 0;
		}
	}

	public static float FetchParameterFloat(String _param_list, String _param_name)
	{
		String val_string = FetchParameter(_param_list, _param_name, false);

		try {
			return (val_string.equals("")) ? 0 : Float.parseFloat(val_string);
		}
    catch (Exception e) {
			return 0f;
		}
	}

	public static String FetchParameterFromBuffer(StringBuffer _param_list, String _param_name, boolean _to_end)
	{
		// Determine place of parameter name within parameter list
		int place = _param_list.indexOf(_param_name + "=");
		//Output.PrintToScreen("FetchParameter(" + _param_list + ", " + _param_name + ", " + _to_end + ") place: " + place);

    // If parameter not found, return empty string
		if (place == -1) {
			return "";
		}

    // Determine place of beginning of parameter value string
		place = _param_list.indexOf("=", place);

		// If equal sign not found, return empty string
		if (place == -1) {
			return "";
		}

    // Determine place of end of this parameter's value
		int end_place = _param_list.indexOf("|", place);

    // If this is the last parameter, or if _to_end is given as true, end place is end of parameter list
		if ((end_place == -1) || _to_end)
		{
			// End place is end of string
			end_place = _param_list.length();
		}

    // Isolate the value string
		String value_string = _param_list.substring(place + 1, end_place);

		return value_string;
	}

	public static int FetchParameterIntFromBuffer(StringBuffer _param_list, String _param_name)
	{
		String val_string = FetchParameterFromBuffer(_param_list, _param_name, false);

		try {
			return (val_string.equals("")) ? 0 : Integer.parseInt(val_string);
		}
    catch (Exception e) {
			return 0;
		}
	}

	public static float FetchParameterFloatFromBuffer(StringBuffer _param_list, String _param_name)
	{
		String val_string = FetchParameterFromBuffer(_param_list, _param_name, false);

		try {
			return (val_string.equals("")) ? 0 : Float.parseFloat(val_string);
		}
    catch (Exception e) {
			return 0f;
		}
	}

	public static int GetNextInt(StringBuffer _string_buffer, int[] _place)
	{
		int end_place = _string_buffer.indexOf(",", _place[0]);

		// Check for negative string index errors
		if ((_place[0] < 0) || (end_place < 0))	{
			Output.PrintToScreen("Constants.GetNextInt() Error, negative string index. place: " + _place[0] + ", end_place: " + end_place);
		}

		try
		{
			String sub_str = _string_buffer.substring(_place[0], end_place);

			//Output.PrintToScreen("int string: '" + sub_str + "', start place: " + (_place[0]) + ", end_place: " + end_place);
			_place[0] = end_place + 1;
			return Integer.parseInt(sub_str);
		}
		catch (Exception e)
		{
			Output.PrintToScreen("Exception in Constants.GetNextInt(). place: " + _place[0] + ", end_place: " + end_place + ", _string_buffer.length(): " + _string_buffer.length());
			return 0;
		}

	}

	public static int TokenizeCoordinates(int _x, int _y)
	{
		return ((_x + 10000) * 20000) + _y + 10000;
	}

	public static void UntokenizeCoordinates(int _token, int [] _coord_array)
	{
		_coord_array[0] = ((int)Math.floor(_token / 20000)) - 10000;
		_coord_array[1] = (_token % 20000) - 10000;
	}

	public static String XPTypeToString(int _xp_type)
	{
		switch (_xp_type)
		{
			case XP_ADMIN: return "ADMIN";
			case XP_DEMO: return "DEMO";
			case XP_UNITE_TRANSFER: return "UNITE_TRANSFER";
			case XP_UNITE_PENDING: return "UNITE_PENDING";
			case XP_ATTACK: return "ATTACK";
			case XP_DISCOVERY: return "DISCOVERY";
			case XP_TOWER: return "TOWER";
			case XP_PATRON: return "PATRON";
			case XP_FOLLOWER: return "FOLLOWER";
			case XP_FIRST_CAPTURE: return "FIRST_CAPTURE";
			case XP_ORB: return "ORB";
			case XP_QUEST: return "QUEST";
			case XP_RAID: return "RAID";
			case XP_RAID_DEFENSE: return "RAID_DEFENSE";
			case XP_TOURNAMENT: return "TOURNAMENT";
			case XP_FARMING: return "FARMING";
		}

		return "<UNKNOWN>";
	}

	public static int ConvertXPToLevel(int _xp)
	{
		for (int i = 2; i <= NUM_LEVELS; i++)
		{
			if (XP_PER_LEVEL[i] > _xp) {
				return i - 1;
			}
		}

		Output.PrintToScreen("ConvertXPToLevel(" + _xp + ") Error!");
		return 0; // ERROR
	}

	public static String ConvertToMoneyString(float _value)
	{
		String str = Float.toString(((int)(_value * 100)) / 100.0f);

		int dec_place = str.indexOf('.');
		if ((dec_place != -1) && (dec_place == (str.length() - 2))) {
			str += "0";
		}

		return str;
	}

	public static String ConvertToIntegerString(int _value)
	{
		String str = Integer.toString(_value);

		if (str.length() > 3) str = (str.substring(0, str.length() - 3) + "," + str.substring(str.length() - 3));
		if (str.length() > 7) str = (str.substring(0, str.length() - 7) + "," + str.substring(str.length() - 7));
		if (str.length() > 11) str = (str.substring(0, str.length() - 11) + "," + str.substring(str.length() - 11));

		return str;
	}

	public static String XMLEncode(String _string)
	{
		return _string.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\"", "&quot;").replaceAll("'", "&apos;");
	}

	static void EncodeString(StringBuffer _buffer, String _string)
	{
		// Encode the length of the string, and the string itself.
		EncodeUnsignedNumber(_buffer, _string.length(), 2);

		int len = _string.length();
		char ch;

		for (int i = 0; i < len; i++)
		{
			ch = _string.charAt(i);

			// "Encrypt" to make less human-readable.
      if (((int)ch >= 65) && ((int)ch <= 122)) {
        ch = (char)((57 - ((int)ch - 65)) + 65);
      }

      _buffer.append(ch);
    }
	}

	static int LimitNumberForEncoding(int _val, int _num_digits)
	{
		int limit;

		switch (_num_digits)
		{
			case 1: limit = 64 - 1; break;
			case 2: limit = 4096 - 1; break;
			case 3: limit = 262144 - 1; break;
			case 4: limit = 16777216 - 1; break;
			case 5: limit = 1073741824 - 1; break;
			default: limit = Integer.MAX_VALUE; break;
		}

		if ((_val > limit) || (_val < -limit))
		{
			Output.PrintToScreen("LimitNumberForEncoding(): Limited number (" + _val + ") to fit within too few digits (" + _num_digits + "). Limit is " + limit + ".");
			Output.PrintStackTrace();
		}

		return Math.max(Math.min(_val, limit), -limit);
	}

	static int LimitNumberForUnsignedEncoding(int _val, int _num_digits)
	{
		int limit;

		switch (_num_digits)
		{
			case 1: limit = 64 - 1; break;
			case 2: limit = 4096 - 1; break;
			case 3: limit = 262144 - 1; break;
			case 4: limit = 16777216 - 1; break;
			case 5: limit = 1073741824 - 1; break;
			default: limit = Integer.MAX_VALUE; break;
		}

		if (_val > limit)
		{
			Output.PrintToScreen("LimitNumberForUnsignedEncoding(): Limited number (" + _val + ") to fit within too few digits (" + _num_digits + "). Limit is " + limit + ".");
			Output.PrintStackTrace();
		}

		return Math.min(_val, limit);
	}

	static void EncodeNumber(StringBuffer _buffer, int _val, int _num_digits)
	{
		boolean negative = false;
		int increment, digit, limit;

		if (_val < 0)
		{
			_val = -_val;
		  negative = true;
		}

		// Set maximum magnitude depending on number of digits used, to avoid overflow.
		switch (_num_digits)
		{
			case 1: limit = 64 - 1; break;
			case 2: limit = 4096 - 1; break;
			case 3: limit = 262144 - 1; break;
			case 4: limit = 16777216 - 1; break;
			case 5: limit = 1073741824 - 1; break;
			default: limit = Integer.MAX_VALUE; break;
		}

		if (_val > limit)
		{
			//_val = limit;
			Output.PrintToScreen("Attempt to encode too large a number (" + _val + ") in too few digits (" + _num_digits + "). Limit is " + limit + ".");
			Output.PrintStackTrace();
			return;
		}

		for (int i = _num_digits; i > 0; i--)
		{
			switch (i)
			{
			case 1:
				increment = 1;
				break;
			case 2:
				increment = 64;
				break;
			case 3:
				increment = 4096;
				break;
			case 4:
				increment = 262144;
				break;
			case 5:
				increment = 16777216;
				break;
			default: // case 6
				increment = 1073741824;
				break;
			}

			digit = (int)(_val / increment);
			_val -= (digit * increment);

			if ((i == _num_digits) && negative)
			{
				digit |= 32; // Turn digit's high bit on
			}

			// Append the current digit's character to the StringBuffer
			_buffer.append((char)(63 + digit));
			//if (debug_flag) Output.PrintToScreen("Encoded digit " + i + " of _val " + _val + " as " + digit);
		}
	}

	static void EncodeNumberDebug(StringBuffer _buffer, int _val, int _num_digits)
	{
		boolean negative = false;
		int increment, digit, limit;

		Output.PrintToScreen("    1) _val: " + _val + ", _num_digits: " + _num_digits);

		if (_val < 0)
		{
			_val = -_val;
		  negative = true;
		}

		// Set maximum magnitude depending on number of digits used, to avoid overflow.
		switch (_num_digits)
		{
			case 1: limit = 64 - 1; break;
			case 2: limit = 4096 - 1; break;
			case 3: limit = 262144 - 1; break;
			case 4: limit = 16777216 - 1; break;
			case 5: limit = 1073741824 - 1; break;
			default: limit = Integer.MAX_VALUE >> 1; break;
		}

		if (_val > limit)
		{
			//_val = limit;
			Output.PrintToScreen("Attempt to encode too large a number (" + _val + ") in too few digits (" + _num_digits + "). Limit is " + limit + ".");
			Output.PrintStackTrace();
			return;
		}

		Output.PrintToScreen("    2) _val: " + _val + ", negative: " + negative);

		for (int i = _num_digits; i > 0; i--)
		{
			switch (i)
			{
			case 1:
				increment = 1;
				break;
			case 2:
				increment = 64;
				break;
			case 3:
				increment = 4096;
				break;
			case 4:
				increment = 262144;
				break;
			case 5:
				increment = 16777216;
				break;
			default: // case 6
				increment = 1073741824;
				break;
			}

			digit = (int)(_val / increment);
			_val -= (digit * increment);

			Output.PrintToScreen("    3) index (i): " + i + ", _val: " + _val + ", digit: " + digit);

			if ((i == _num_digits) && negative)
			{
				digit |= 32; // Turn digit's high bit on
				Output.PrintToScreen("    4) digit: " + digit);
			}

			// Append the current digit's character to the StringBuffer
			Output.PrintToScreen("    5) num to encode: " + (63 + digit) + ", char: " + (char)(63 + digit));
			_buffer.append((char)(63 + digit));
		}
	}

	static int MaxEncodableUnsignedNumber(int _num_digits)
	{
		switch (_num_digits)
		{
			case 1: return 64 - 1;
			case 2: return 4096 - 1;
			case 3: return 262144 - 1;
			case 4: return 16777216 - 1;
			case 5: return 1073741824 - 1;
			default: return Integer.MAX_VALUE;
		}
	}

	static void EncodeUnsignedNumber(StringBuffer _buffer, int _val, int _num_digits)
	{
		int increment, digit, limit;

    // Make sure value is not negative
		if (_val < 0) {
			_val = 0;
			Output.PrintToScreen("Attempt to encode a negative number as unsigned!");
			Output.PrintStackTrace();
			return;
		}

		// Set maximum magnitude depending on number of digits used, to avoid overflow.
		switch (_num_digits)
		{
			case 1: limit = 64 - 1; break;
			case 2: limit = 4096 - 1; break;
			case 3: limit = 262144 - 1; break;
			case 4: limit = 16777216 - 1; break;
			case 5: limit = 1073741824 - 1; break;
			default: limit = Integer.MAX_VALUE; break;
		}

		if (_val > limit)
		{
			//_val = limit;
			Output.PrintToScreen("Attempt to encode too large a number (" + _val + ") in too few digits (" + _num_digits + "). Limit is " + limit + ".");
			Output.PrintStackTrace();
			return;
		}

		for (int i = _num_digits; i > 0; i--)
		{
			switch (i)
			{
			case 1:
				increment = 1;
				break;
			case 2:
				increment = 64;
				break;
			case 3:
				increment = 4096;
				break;
			case 4:
				increment = 262144;
				break;
			case 5:
				increment = 16777216;
				break;
			default: // case 6
				increment = 1073741824;
				break;
			}

			digit = (int)(_val / increment);
			_val -= (digit * increment);

//Output.PrintToScreen("Encoding i:" + i + ", digit:" + digit + ", val remain:" + _val + ", char: " + ((char)(63 + digit)));

			// Append the current digit's character to the StringBuffer
			_buffer.append((char)(63 + digit));
		}
	}

	public static void WriteToLog(String _log_filename, String _string)
	{
		// Prepend directory name to _log_filename
		_log_filename = log_dir + _log_filename;

		try{
		// Open the file
		FileWriter fw = new FileWriter(_log_filename, true); // append

		// Write the serialized data to the file
		fw.write(_string);

		// Close the file
		fw.close();
		}catch(IOException e){
			Output.screenOut.println("Couldn't write to log '" + _log_filename + "': " + e.getMessage()); // To avoid infinite loop, don't call Output.PrintToScreen().
			Admin.Emergency("Couldn't write to log '" + _log_filename + "': " + e.getMessage(), false);
		}
	}

	public static void WriteNewLog(String _log_filename, String _string)
	{
		// Prepend directory name to _log_filename
		_log_filename = log_dir + _log_filename;

		try{
		// Open the file
		FileWriter fw = new FileWriter(_log_filename, false); // Do not append

		// Write the serialized data to the file
		fw.write(_string);

		// Close the file
		fw.close();
		}catch(IOException e){
			Output.screenOut.println("Couldn't write to log '" + _log_filename + "': " + e.getMessage()); // To avoid infinite loop, don't call Output.PrintToScreen().
			Admin.Emergency("Couldn't write to log '" + _log_filename + "': " + e.getMessage(), false);
		}
	}

	public static void WriteToPublicLog(String _string)
	{
		// Construct the log_filename
		String log_filename = publiclog_dir + "publiclog_0.txt";

		try{
		// Open the file
		FileWriter fw = new FileWriter(log_filename, true); // append

		// Write the serialized data to the file
		fw.write(_string);

		// Close the file
		fw.close();
		}catch(IOException e){
			Output.screenOut.println("Couldn't write to log '" + log_filename + "': " + e.getMessage()); // To avoid infinite loop, don't call Output.PrintToScreen().
			Admin.Emergency("Couldn't write to log '" + log_filename + "': " + e.getMessage(), false);
		}
	}

  public static void WriteNewPublicLog(String _log_filename, String _string)
	{
		// Prepend directory name to _log_filename
		_log_filename = publiclog_dir + _log_filename;

		try{
		// Open the file
		FileWriter fw = new FileWriter(_log_filename, false); // Do not append

		// Write the serialized data to the file
		fw.write(_string);

		// Close the file
		fw.close();
		}catch(IOException e){
			Output.screenOut.println("Couldn't write to log '" + _log_filename + "': " + e.getMessage()); // To avoid infinite loop, don't call Output.PrintToScreen().
			Admin.Emergency("Couldn't write to log '" + _log_filename + "': " + e.getMessage(), false);
		}
	}

	public static void WriteToNationLog(NationData _nationData, UserData _userData, String _string)
	{
		// Construct log string
		_string = GetTimestampString() + ((_userData == null) ? " " : " " + _userData.name + " (" + _userData.ID + ") ") + _string + "\n";

		// Prepend directory name to _log_filename
		String log_filename = nation_log_dir + _nationData.name + "_" + _nationData.ID + ".txt";

		try{
		// Open the file
		FileWriter fw = new FileWriter(log_filename, true); // append

		// Write the serialized data to the file
		fw.write(_string);

		// Close the file
		fw.close();
		}catch(IOException e){
			Output.screenOut.println("Couldn't write to log '" + log_filename + "': " + e.getMessage()); // To avoid infinite loop, don't call Output.PrintToScreen().
			Admin.Emergency("Couldn't write to log '" + log_filename + "': " + e.getMessage(), false);
		}
	}

	static void ReadServerConfig()
	{
		try {
			// Read the config.txt json file
			FileReader reader = new FileReader("config.json");

			JSONParser jsonParser = new JSONParser();
			JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);

			// Fetch the home_dir value
			home_dir = (String) jsonObject.get("home_dir");

			// Fetch the public_gen_dir value
			public_gen_dir = (String) jsonObject.get("public_gen_dir");

			// Fetch the private_gen_dir value
			private_gen_dir = (String) jsonObject.get("private_gen_dir");

			// Fetch the php_exe value
			php_exe = (String) jsonObject.get("php_exe");

			// Fetch the port value
			port =  Integer.parseInt((String) jsonObject.get("port"));

			// Fetch the client_version value
			client_version = Integer.parseInt((String) jsonObject.get("client_version"));

			// Fetch the max_num_clients value
			max_num_clients = Integer.parseInt((String) jsonObject.get("max_num_clients"));

			// Fetch the max_num_clients_per_nation value
			max_num_clients_per_nation = Integer.parseInt((String) jsonObject.get("max_num_clients_per_nation"));

			// Fetch the admin_login_only value
			admin_login_only = Boolean.parseBoolean((String) jsonObject.get("admin_login_only"));

			// Fetch the admin_login_ip value
			admin_login_ip = (String) jsonObject.get("admin_login_ip");

			// Fetch the server_id value
			server_id = Integer.parseInt((String) jsonObject.get("server_id"));

			// Fetch the ftp_username value
			ftp_username = (String) jsonObject.get("ftp_username");

			// Fetch the ftp_password value
			ftp_password = (String) jsonObject.get("ftp_password");

			// Fetch the ftp_host value
			ftp_host = (String) jsonObject.get("ftp_host");

			// Fetch the account DB url value
			account_db_url = (String) jsonObject.get("account_db_url");

			// Fetch the account DB user value
			account_db_user = (String) jsonObject.get("account_db_user");

			// Fetch the account DB pass value
			account_db_pass = (String) jsonObject.get("account_db_pass");

			// Fetch the game DB url value
			game_db_url = (String) jsonObject.get("game_db_url");

			// Fetch the game DB user value
			game_db_user = (String) jsonObject.get("game_db_user");

			// Fetch the game DB pass value
			game_db_pass = (String) jsonObject.get("game_db_pass");

			// Fetch the update_for_downtime value
			update_for_downtime = Boolean.parseBoolean((String) jsonObject.get("update_for_downtime"));

			// Fetch the disable_update_thread value
			boolean disable_update_thread = Boolean.parseBoolean((String) jsonObject.get("disable_update_thread"));
			if (disable_update_thread) WOCServer.update_thread_active = false;

			// Fetch the min_level_limit value
			min_level_limit = Integer.parseInt((String) jsonObject.get("min_level_limit"));

			// Fetch the max_level_limit value
			max_level_limit = Integer.parseInt((String) jsonObject.get("max_level_limit"));

			// Fetch the mid_level_limit value
			mid_level_limit = Integer.parseInt((String) jsonObject.get("mid_level_limit"));

			// Fetch the mid_level_limit_pos value
			mid_level_limit_pos = Float.parseFloat((String) jsonObject.get("mid_level_limit_pos"));

			// Fetch the new_player_area_boundary value
			new_player_area_boundary = Float.parseFloat((String) jsonObject.get("new_player_area_boundary"));

			// Fetch the max_accounts_per_period value
			max_accounts_per_period = Integer.parseInt((String) jsonObject.get("max_accounts_per_period"));

			// Fetch the max_accounts_period value
			max_accounts_period = Integer.parseInt((String) jsonObject.get("max_accounts_period")) * SECONDS_PER_DAY;

			// Fetch the max_buy_credits_per_month value
			max_buy_credits_per_month = Integer.parseInt((String) jsonObject.get("max_buy_credits_per_month"));

			// Fetch the manpower_gen_multiplier value
			manpower_gen_multiplier = Float.parseFloat((String) jsonObject.get("manpower_gen_multiplier"));

			// Fetch the max_nation_members value
			max_nation_members = Integer.parseInt((String) jsonObject.get("max_nation_members"));

			// Fetch the ad_bonuses value
			ad_bonuses = Boolean.parseBoolean((String) jsonObject.get("ad_bonuses"));

			// Fetch the cash_out_prizes value
			cash_out_prizes = Boolean.parseBoolean((String) jsonObject.get("cash_out_prizes"));

			// Fetch the allow_credit_purchases value
			allow_credit_purchases = Boolean.parseBoolean((String) jsonObject.get("allow_credit_purchases"));

			// Fetch the prize_dollars_awarded_per_day value
			prize_dollars_awarded_per_day = Float.parseFloat((String) jsonObject.get("prize_dollars_awarded_per_day"));

			// Determine the server_db_name
			server_db_name = "WOC" + server_id;
    }
    catch (Exception e) {
      Output.PrintToScreen("Unable to load config.txt");
			Output.PrintException(e);
			System.exit(1);
    }
	}

	static void UpdateData()
	{
		// If the data has already been updated to the current count, return.
		if (GlobalData.instance.cur_data_update_count >= Constants.DATA_UPDATE_COUNT) {
			return;
		}

		// DON'T WANT THIS CHANGE BEING RE-MADE UNDER ANY CIRCUMSTANCES!!!

/*
		Output.PrintToScreen("About to update data.");

		// Determine current time
		int cur_time = GetTime();

    // Determine highest user ID
		int highestUserID = DataManager.GetHighestDataID(Constants.DT_USER);

		// Iterate through each user
		UserData curUserData;
		for (int curUserID = 1; curUserID <= highestUserID; curUserID++)
		{
      if ((curUserID % 1000) == 0) {
        Output.PrintToScreen("Updating user " + curUserID);
      }

			// Get the data for the user with the current ID
			curUserData = (UserData)DataManager.GetData(Constants.DT_USER, curUserID, false);

			// If no user exists with this ID, continue to next.
			if (curUserData == null) {
				continue;
			}

			// NOTE: HERE is where changes to the data would be made.

			// Fetch player account data.
			PlayerAccountData accountData = AccountDB.ReadPlayerAccount(curUserData.playerID);

			Output.PrintToScreen("User " + curUserData.name + " (" + curUserData.ID + ")'s playerID: " + curUserData.playerID);

			if ((accountData != null) && (accountData.email.equals("") == false))
			{
				Output.PrintToScreen("Player " + accountData.username + " (" + accountData.ID + ")'s email: " + accountData.email);
				EmailData.AssociateEmailWithUser(accountData.email, curUserID);
			}


			// Mark the user data to be updated.
      DataManager.MarkForUpdate(curUserData);
    }
*/
/*
		// Determine highest nation ID
		int highestNationID = DataManager.GetHighestDataID(Constants.DT_NATION);

		// Iterate through each nation
		NationData curNationData;
		for (int curNationID = 1; curNationID <= highestNationID; curNationID++)
		{
      if ((curNationID % 1000) == 0) {
        Output.PrintToScreen("Updating nation " + curNationID);
      }

			// Get the data for the nation with the current ID
			curNationData = (NationData)DataManager.GetData(Constants.DT_NATION, curNationID, false);

			// If no nation exists with this ID, continue to next.
			if (curNationData == null) {
				continue;
			}

			// NOTE: HERE is where changes to the data would be made.

			// Mark the nation data to be updated.
      DataManager.MarkForUpdate(curNationData);
    }
*/
		Output.PrintToScreen("Data update complete. About to update database.");

		// Update the cur_data_update_count
		GlobalData.instance.cur_data_update_count = Constants.DATA_UPDATE_COUNT;
		DataManager.MarkForUpdate(GlobalData.instance);

		// Update the database ////////////////////////////////
		try
		{
			DataManager.UpdateDatabase(false);
		}
		catch(Exception e)
		{
			Output.PrintToScreen("Exception during UpdateDatabase() call in Constants.UpdateData():");
			Output.PrintException(e);
		}

		Output.PrintToScreen("Finished updating database.");
	}
};
