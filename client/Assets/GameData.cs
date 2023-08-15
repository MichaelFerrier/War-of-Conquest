using UnityEngine;
using UnityEngine.Profiling;
using System;
using System.Collections;
using System.Collections.Generic;
using I2.Loc;

public class GameData : MonoBehaviour
{
    public enum UserFlags
    {
        CUSTOM_USERNAME         = 1,
        BLOCK_WHISPERS          = 2,
        DISABLE_CHAT_FILTER     = 4,
        FULLSCREEN              = 8,
        SHOW_MAP_LOCATION       = 16,
        SOUND_EFFECTS           = 32,
        HIDE_TUTORIAL           = 64,
        EVERYPLAY        		= 128,
	    SHOW_FACE        		= 256,
    	RECORD_VIDEO     		= 512,
        SHOW_GRID               = 1024,
        MUSIC                   = 2048,
        DISABLE_FLASH_EFFECTS   = 4096,
    };

    public enum NationFlags
    {
        CUSTOM_NATION_NAME              = 1,
        BLOCK_NATION_CHAT_INVITATIONS   = 2,
        TOURNAMENT_FIRST_PLACE		    = 4,
	    TOURNAMENT_SECOND_PLACE			= 8,
	    TOURNAMENT_THIRD_PLACE		    = 16,
	    ORB_OF_FIRE						= 32,
        INCOGNITO                       = 64,
        ONLINE                          = 128,
        TOURNAMENT_CONTENDER            = 256
    };

    public enum RaidFlags
    {
        BEGUN = 1,
	    FINISHED = 2,
	    RED_SHARD = 4,
	    GREEN_SHARD = 8,
	    BLUE_SHARD = 16,
	    PROGRESS_50_PERCENT = 32,
	    PROGRESS_100_PERCENT = 64,
        REPLAY_AVAILABLE = 128
    };

    public enum ProcessFlags
    {
        FIRST_CAPTURE = 1,
        DISCOVERY = 2
    };

    public enum BattleFlags
    {
        INERT = 1,
        CRIT  = 2,
	    TARGET_BLOCK  = 4,
        FLANKED = 8,
        FIRST_CAPTURE = 16,
        DISCOVERY = 32,
        FAST_CRUMBLE = 64,
        TOTAL_DEFENSE  = 128,
        INSURGENCY = 256
    };

    public enum AllianceListType
    {
        CURRENT_ALLIES,
        INCOMING_ALLIANCE_INVITATIONS,
        OUTGOING_ALLIANCE_INVITATIONS,
        INCOMING_UNITE_INVITATIONS,
        OUTGOING_UNITE_INVITATIONS
    };

    public enum Stat
    {
        UNDEF                 = -1,
        LEVEL,
        XP,
        TOTAL_AREA,
        SUPPORTABLE_AREA,
        INTERIOR_AREA,
        BORDER_AREA,
        GEOGRAPHIC_EFFICIENCY,
	    TECH,
	    BIO,
	    PSI,
   	    XP_MULTIPLIER,
	    MANPOWER_MAX,
        MANPOWER_RATE,
        ATTACK_MANPOWER,
	    CRIT_CHANCE,
  	    SPLASH_DAMAGE,
	    SIMULTANEOUS_ACTIONS,
	    ENERGY_MAX,
        ENERGY_RATE,
        ENERGY_BURN_RATE,
        HP_PER_SQUARE,
	    HP_RESTORE,
        SALVAGE_VALUE,
	    WALL_DISCOUNT,
	    STRUCTURE_DISCOUNT,
	    INVISIBILITY,
        MAX_ALLIANCES,
        NUM_STORAGE_STRUCTURES,
        MANPOWER_STORED,
        ENERGY_STORED,
        MANPOWER_AVAILABLE,
        ENERGY_AVAILABLE,
        REBIRTH_LEVEL_BONUS,
        FINAL_ENERGY_BURN_RATE,
        PERIMETER,
        STORAGE_XP_RATE,
        PENDING_XP,
        MANPOWER_RATE_MINUS_BURN,
        ENERGY,
        INSURGENCY,
        TOTAL_DEFENSE
    }

    public enum RanksListType
    {
        UNDEF,
        COMBINED,
        NATION_XP,
        NATION_XP_MONTHLY,
        USER_XP,
        USER_XP_MONTHLY,
        USER_FOLLOWERS,
        USER_FOLLOWERS_MONTHLY,
        NATION_WINNINGS,
        NATION_WINNINGS_MONTHLY,
        NATION_RAID_EARNINGS,
        NATION_RAID_EARNINGS_MONTHLY,
        NATION_MEDALS,
        NATION_MEDALS_MONTHLY,
        NATION_ORB_SHARD_EARNINGS,
        NATION_ORB_SHARD_EARNINGS_MONTHLY,
        NATION_LATEST_TOURNAMENT,
        NATION_TOURNAMENT_TROPHIES,
        NATION_TOURNAMENT_TROPHIES_MONTHLY,
        NATION_LEVEL,
        NATION_REBIRTHS,
        NATION_QUESTS,
        NATION_QUESTS_MONTHLY,
        NATION_ENERGY_DONATED,
        NATION_ENERGY_DONATED_MONTHLY,
        NATION_MANPOWER_DONATED,
        NATION_MANPOWER_DONATED_MONTHLY,
        NATION_AREA,
        NATION_AREA_MONTHLY,
        NATION_CAPTURES,
        NATION_CAPTURES_MONTHLY,
        NATION_TOURNAMENT_CURRENT,
        ORB_WINNINGS
    }

    public enum UserEventType
    {
        ATTACK          = 0,
        OCCUPY          = 1,
        EVACUATE        = 2,
        BUILD           = 3,
        PAN_MAP         = 4,
        SELECT_ADVANCE  = 5,
        COUNT           = 6
    }

    public enum MapMode
    {
        MAINLAND,
        HOMELAND,
        RAID,
        REPLAY
    }

    public enum LimitType
    {
        Undef,
        LimitWestern,
        LimitWesternNextLevel,
        LimitEastern,
        LimitVetArea,
        LimitNewArea,
        LimitExtent
    };

    public static GameData instance = null;

    public const int STAT_TECH = 0;
    public const int STAT_BIO = 1;
    public const int STAT_PSI = 2;
    public const int NUM_STATS = 3;

    // Message types
	public const int MESSAGE_TYPE_GAME   = 0;
	public const int MESSAGE_TYPE_NATION = 1;
	public const int MESSAGE_TYPE_OTHER  = 2;
    
    // User ranks
    public const int RANK_SOVEREIGN    = 0; // Change nation color
    public const int RANK_COSOVEREIGN  = 1; // Delete others' messages, migrate
    public const int RANK_GENERAL      = 3; // Change join password, remove member, distribute winnings
    public const int RANK_CAPTAIN      = 4; // Return winnings for credits, promote/demote, set tech goal
    public const int RANK_COMMANDER    = 6; // purchase tech, choose research, delete flags, modify chat list
    public const int RANK_WARRIOR      = 9; // Attack, evacuate
    public const int RANK_CIVILIAN     = 12;
    public const int RANK_NONE         = 13;

    // Mainland map ID
    public const int MAINLAND_MAP_ID = 1;

    // Raid map ID base
    public const int RAID_ID_BASE = 1000000000;

    public const int XXHASH_SEED = 739683679;

    public int serverID = -1;
    public string player_info = "";
	public int flags = 0;
	public int userID = -1;
	public int nationID = -1;
	public String username = "";
	public String email = "";
    public int userCreationTime = 0;
	public int userFlags = 0;
    public int userRank = 0;
    public int userPrevLogOutGameTime = 0;
    public int homeNationID = 0;
    public String homeNationName = "";
    public int nationFlags = 0;
    public String nationName = "";
    public String nationPassword = "";
    public Color nationColor = new Color();
    public int emblemIndex = -1;
    public NationData.EmblemColor emblemColor = NationData.EmblemColor.BLACK;
    public int level = 0;
    public int xp = 0;
    public int pending_xp = 0;
    public int level_xp_threshold = 0;
    public int next_level_xp_threshold = 0;
    public int rebirth_count = 0;
    public int rebirth_level_bonus = 0;
    public int rebirth_available_level = 0;
    public float rebirth_available_time = 0;
    public int rebirth_countdown = 0;
    public int rebirth_countdown_start = 0;
    public int rebirth_countdown_purchased = 0;
    public int advance_points = 0;
    public int map_position_limit = 0;
    public int map_position_limit_next_level = 0;
    public int map_position_eastern_limit = 0;
    public int credits = 0;
    public int credits_transferable = 0;
    public int credits_allowed_to_buy = 0;
    public float fealtyEndTime = 0f;
    public string fealtyNationName = "";
    public float fealtyTournamentEndTime = 0f;
    public string fealtyTournamentNationName = "";
    public int fealtyNumNationsAtTier = 0;
    public int fealtyNumNationsInTournament = 0;
    public bool fealtyRequestPermission = false;
	public int modLevel = 0;
	public bool userIsAdmin = false;
    public bool userIsVeteran = false;
    public bool nationIsVeteran = false;
	public bool firstLogin = false;
	public bool userIsRegistered = false;
    public bool adBonusesAllowed = false;
	public int adBonusAvailable = 0;
    public bool cashOutPrizesAllowed = false;
    public bool creditPurchasesAllowed = false;
	public int gameTimeAtLogin = 0;
    public float timeAtLogin = 0f;
    public float endOfDayTime = 0f;
    public float nextFreeMigrationTime = 0f;
    public float nextUniteTime = 0f;
    public int resetAdvancesPrice = 0;
    public int targetAdvanceID = -1;
    public int prizeMoney = 0;
    public int prizeMoneyHistory = 0;
    public int prizeMoneyHistoryMonthly = 0;

    public Footprint mainland_footprint = new Footprint();
    public Footprint homeland_footprint = new Footprint();
    public Footprint raidland_footprint = new Footprint();
    public Footprint current_footprint;

    public MapMode mapMode = MapMode.MAINLAND;

    public int area = 0;
    public int border_area = 0;
    public int perimeter = 0;
    public float geo_efficiency_base = 0f;

    // Stats
    public int energy = 0;
    public int energyMax = 0;
    //public int energyRate = 0;
    //public int manpower = 0;
    public int manpowerMax = 0;
    //public int manpowerRate = 0;
    public int manpowerPerAttack = 0;
    //public int statTech = 0;
    //public int statBio = 0;
    //public int statPsi = 0;
    public float geo_efficiency_modifier = 0f;
    //public float xpMultiplier = 0f;
    public int hitPointBase = 0;
    public int hitPointRate = 0;
    public float critChance = 0f;
    public float salvageValue = 0f;
    public float wallDiscount = 0f;
    public float structureDiscount = 0f;
    public float splashDamage = 0f;
    public int maxNumAlliances = 0;
    public int maxSimultaneousProcesses = 0;
    public bool invisibility = false;
    public bool insurgency = false;
    public bool total_defense = false;
    public int numStorageStructures = 0;
    public int sharedEnergyXPPerHour = 0;
    public int sharedManpowerXPPerHour = 0;
    public float sharedEnergyFill = 0f;
    public float sharedManpowerFill = 0f;
    public int manpowerStored = 0;
    public int energyStored = 0;
    public int manpowerAvailable = 0;
    public int energyAvailable = 0;

    // Stat amounts from perms
    public int statTechFromPerms = 0;
    public int statBioFromPerms = 0;
    public int statPsiFromPerms = 0;
    public int manpowerRateFromPerms = 0;
    public int energyRateFromPerms = 0;
    public float xpMultiplierFromPerms = 0;

    // Stat amounts from temps
    public int statTechFromTemps = 0;
    public int statBioFromTemps = 0;
    public int statPsiFromTemps = 0;
    public int manpowerRateFromTemps = 0;
    public int energyRateFromTemps = 0;
    public float xpMultiplierFromTemps = 0;

    // Stat amounts from resources
    public int statTechFromResources = 0;
    public int statBioFromResources = 0;
    public int statPsiFromResources = 0;
    public int manpowerRateFromResources = 0;
    public int energyRateFromResources = 0;
    public float xpMultiplierFromResources = 0;

    // Stat multipliers
    public float tech_mult = 1f;
	public float bio_mult = 1f;
	public float psi_mult = 1f;
	public float manpower_rate_mult = 1f;
	public float energy_rate_mult = 1f;
	public float manpower_max_mult = 1f;
	public float energy_max_mult = 1f;
	public float hp_per_square_mult = 1f;
	public float hp_restore_mult = 1f;
	public float attack_manpower_mult = 1f;

    public int energyBurnRate = 0;
    public float manpowerBurnRate = 0;

    // Purchasing energy
    public int buyEnergyDayAmount = 0;

    // Patron info
    public int prev_month_patron_bonus_XP = 0;
    public int prev_month_patron_bonus_credits = 0;
    public String patronCode = "";
    public int patronID = -1;
    public String patronUsername = "";
    public int total_patron_xp_received = 0;
    public int total_patron_credits_received = 0;
    public int patron_prev_month_patron_bonus_XP = 0;
    public int patron_prev_month_patron_bonus_credits = 0;
    public int patron_num_followers = 0;

    // Ranks
    public UserRanksRecord userRanks = new UserRanksRecord();
    public NationRanksRecord nationRanks = new NationRanksRecord();
    public List<UserRanksRecord> contactUserRanks = new List<UserRanksRecord>();
    public List<NationRanksRecord> contactNationRanks = new List<NationRanksRecord>();
    public List<OrbRanksRecord> contactOrbRanks = new List<OrbRanksRecord>();
    public int nation_orb_winnings, nation_orb_winnings_monthly; // For a specific orb
    public int orb_winnings_history, orb_winnings_history_monthly;

    // User login report information
	public int report__defenses_squares_defeated;
	public int report__defenses_XP;
	public int report__defenses_lost;
	public int report__defenses_built;
	public int report__walls_lost;
	public int report__walls_built;
	public int report__attacks_squares_captured;
	public int report__attacks_XP;
	public int report__levels_gained;
	public int report__orb_count_delta;
    public int report__orb_credits;
    public int report__orb_XP;
    public int report__farming_XP;
	public int report__resource_count_delta;
	public int report__land_lost;
	public float report__energy_begin;
	public float report__energy_spent;
	public float report__energy_donated;
	public float report__energy_lost_to_raids;
	public float report__manpower_begin;
	public float report__manpower_spent;
    public float report__manpower_lost_to_resources;
    public float report__manpower_donated;
	public float report__manpower_lost_to_raids;
	public float report__credits_begin;
	public float report__credits_spent;
    public float report__patron_XP;
    public float report__patron_credits;
    public float report__follower_XP;
    public float report__follower_credits;
    public int report__follower_count;
    public int report__raids_fought;
    public int report__medals_delta;
    public int report__rebirth;
    public float report__home_defense_credits;
    public float report__home_defense_xp;
    public float report__home_defense_rebirth;

    // Stat initial values
    public int initEnergy;
    public int initEnergyMax;
    public int initEnergyRate;
    public int initManpower;
    public int initManpowerMax;
    public int initManpowerRate;
    public int initManpowerPerAttack;
    public int initStatTech;
    public int initStatBio;
    public int initStatPsi;
    public int initHitPointBase;
    public int initHitPointsRate;
    public float initSalvageValue;
    public int initMaxNumAlliances;
    public int initMaxSimultaneousProcesses;

    // Global tournament info
    public int globalTournamentStartDay;
    public int globalTournamentStatus;
    public int tournamentNumActiveContenders;
    public float tournamentEnrollmentClosesTime;
    public float tournamentNextEliminationTime;
    public float tournamentEndTime;

    // Nation tournament info
    public int nationTournamentStartDay;
    public bool nationTournamentActive;
    public int tournamentRank;
    public float tournamentTrophiesAvailable;
    public float tournamentTrophiesBanked;

    // Homeland info
    public float shardRedFill;
    public float shardGreenFill;
    public float shardBlueFill;

    // Raid info
    public int raidAttackerNationID;
    public int raidDefenderNationID;
    public string raidAttackerNationName;
    public string raidDefenderNationName;
    public int raidDefenderStartingArea;
    public int raidDefenderArea;
    public int raidAttackerNationNumMedals;
    public int raidDefenderNationNumMedals;
    public int raidFlags;
    public float raidEndTime;
    public float raidReviewEndTime;
    public int raidPercentageDefeated;
    public int raid0StarMedalDelta;
    public int raid5StarMedalDelta;
    public int raidMaxRewardCredits;
    public int raidMaxRewardXP;
    public int raidMaxRewardRebirth;
    public int raidAttackerRewardMedals;
    public int raidDefenderRewardMedals;
    public int raidRewardCredits;
    public int raidRewardXP;
    public int raidRewardRebirth;
    public float replayEndTime;
    public float replayCurTime;
    public int replayEventIndex;
    public int raidNumMedals;
    
    // Constants
    public int timeUntilCrumble;
    public int timeUntilFastCrumble;
    public int defenseRebuildPeriod;
    public int secondsRemainVisible;
    public int completionCostPerMinute;
    public int uniteCost;
    public int pendingXPPerHour;
    public int migrationCost;
    public int customizeCost;
    public int levelBonusPerRebirth;
    public int maxRebirthLevelBonus;
    public int rebirthToBaseLevel;
    public int maxRebirthCountdownPurchased;
    public int allyLevelDiffLimit;
    public int supportableAreaBase;
    public int supportableAreaPerLevel;
    public float geographicEfficiencyMin;
    public float geographicEfficiencyMax;
    public float resourceBonusCap;
    public float manpowerBurnExponent;
    public float manpowerBurnFractionOfManpowerMax;
    public float overburnPower;
    public int maxNumStorageStructures;
    public int storageRefillHours;
    public int maxAccountsPerPeriod;
    public int maxAccountsPeriod;
    public float manpowerMaxHomelandFraction;
    public float manpowerRateHomelandFraction;
    public float energyRateHomelandFraction;
    public float energyRateRaidlandFraction;
    public float supportableAreaHomelandFraction;
    public float supportableAreaRaidlandFraction;
    public float minManpowerFractionToStartRaid;
    public float manpowerFractionCostToRestartRaid;
    public int raidMedalsPerLeague;
    public int newPlayerAreaBoundary;
    public int creditsAllowedBuyPerMonth;
    public float manpowerGenMultiplier;
    public float incognitoEnergyBurn;
    public int minIncognitoPeriod;
    public int minWinningsToCashOut;
	public int creditsPerCentTradedIn;
    
    // Fractions of stats that are used for homeland and raids.
	public static float MANPOWER_MAX_HOMELAND_FRACTION = 0.1f;
	public static float MANPOWER_RATE_HOMELAND_FRACTION = 0.5f;
	public static float ENERGY_RATE_HOMELAND_FRACTION = 0.15f;
	public static float ENERGY_RATE_RAIDLAND_FRACTION = 0.1f;
	public static float SUPPORTABLE_AREA_HOMELAND_FRACTION = 0.1f;
	public static float SUPPORTABLE_AREA_RAIDLAND_FRACTION = 0.1f;

    // Purchasing manpower and energy
	public float buyManpowerBase;
    public float buyManpowerMult;
    public float buyManpowerDailyLimit;
    public float buyManpowerDailyAbsoluteLimit;
    public float buyManpowerLimitBase;
	public float buyEnergyBase;
    public float buyEnergyMult;
    public float buyEnergyDailyLimit;
    public float buyEnergyDailyAbsoluteLimit;
    public float buyEnergyLimitBase;

    // Purchasing credits
    public int numCreditPackages;
	public int[] buyCreditsAmount;
	public float[] buyCreditsCostUSD;

    // Subscriptions
    public int numSubscriptionTiers;
    public float[] subscriptionCostUSD;
    public int[] bonusCreditsPerDay;
    public int[] bonusRebirthPerDay;
    public int[] bonusXPPercentage;
    public int[] bonusManpowerPercentage;

    // Subscription information
    public bool subscribed;
    public int subscriptionTier;
    public String subscriptionGateway;
    public String subscriptionStatus;
    public DateTime subscriptionPaidThrough;
    public String associatedSubscribedUsername;
    public String subscriptionBonusCreditsTarget;
    public String subscriptionBonusRebirthTarget;
    public String subscriptionBonusXPTarget;
    public String subscriptionBonusManpowerTarget;

    // Payout rates (in cents per day) for the various orb types. 
    public Dictionary<int, int> orbPayoutRates = new Dictionary<int, int>();

    // Prices for purchasable advances
    public Dictionary<int, int> prices = new Dictionary<int, int>();

    // User events
    public float[] prevUserEventTime = new float[(int)UserEventType.COUNT];
    public float[] userEventFrequency = new float[(int)UserEventType.COUNT];
    public Dictionary<int, int> blocksTakenByPlayerNation = new Dictionary<int, int>();
    public Dictionary<int, int> blocksTakenFromPlayerNation = new Dictionary<int, int>();

    public Dictionary<int, int> techCount = new Dictionary<int, int>();
    public Dictionary<int, float> tempTechExpireTime = new Dictionary<int, float>();

    public Dictionary<int, bool> availableBuilds = new Dictionary<int, bool>();
    public Dictionary<int, int> availableUpgrades = new Dictionary<int, int>();

	public Dictionary<int, NationData> nationTable = new Dictionary<int, NationData>();

    public List<AllyData> alliesList = new List<AllyData>();
    public List<AllyData> incomingAllyRequestsList = new List<AllyData>();
    public List<AllyData> outgoingAllyRequestsList = new List<AllyData>();
    public List<AllyData> incomingUniteRequestsList = new List<AllyData>();
    public List<AllyData> outgoingUniteRequestsList = new List<AllyData>();

    public Dictionary<int, QuestRecord> questRecords = new Dictionary<int, QuestRecord>();

    public List<RaidLogRecord> raidAttackLog = new List<RaidLogRecord>();
    public List<RaidLogRecord> raidDefenseLog = new List<RaidLogRecord>();

    public List<ReplayEventRecord> replayList = new List<ReplayEventRecord>();

    public List<NationOrbRecord> nationOrbsList = new List<NationOrbRecord>();
    public List<NationOrbRecord> nationOrbsMonthlyList = new List<NationOrbRecord>();

    public List<ObjectRecord> objects = new List<ObjectRecord>();
    public Dictionary<int, int> builds = new Dictionary<int, int>();
    
    public List<NationArea> nationAreas = new List<NationArea>();

    public List<MapFlagRecord> mapFlags = new List<MapFlagRecord>();

    public XXHash xxhash = new XXHash(XXHASH_SEED);

    void Awake() {
		instance = this;
	}

	// Use this for initialization
	void Start () {
	}
	
	// Update is called once per frame
	void Update () {
	}

    public void InfoEventReceived()
    {
        // Clear the table of existing NationData objects.
        nationTable.Clear();

        // Clear the records of user events
        Array.Clear(prevUserEventTime, 0, prevUserEventTime.Length);
        Array.Clear(userEventFrequency, 0, prevUserEventTime.Length);
        blocksTakenByPlayerNation.Clear();
        blocksTakenFromPlayerNation.Clear();

        // Initialize current_footprint to be the mainland_footprint.
        current_footprint = mainland_footprint;

        // Reset raid info
        raidFlags = 0;
    }

    public void OnSwitchedMap(int _mapID, bool _replay)
    {
        if (_replay)
        {
            mapMode = MapMode.REPLAY;
            current_footprint = raidland_footprint;
        }
        else if (_mapID >= RAID_ID_BASE)
        {
            mapMode = MapMode.RAID;
            current_footprint = raidland_footprint;
        }
        else if (_mapID > MAINLAND_MAP_ID) 
        {
            mapMode = MapMode.HOMELAND;
            current_footprint = homeland_footprint;
        }
        else 
        {
            mapMode = MapMode.MAINLAND;
            current_footprint = mainland_footprint;
        }

        GameGUI.instance.OnSwitchedMap(mapMode);
    }

    public NationData GetNationData(int _nationID)
    {
        return (_nationID < 0) ? null : (nationTable.ContainsKey(_nationID) ? nationTable[_nationID] : null);
    }

    public bool GetUserFlag(UserFlags _flag)
    {
        return (userFlags & (int)_flag) != 0;
    }

    public void SetUserFlag(UserFlags _flag, bool _value)
    {
        if (_value) {
            userFlags = userFlags | (int)_flag;
        } else {
            userFlags = userFlags & ~((int)_flag);
        }
    }

    public bool GetNationFlag(NationFlags _flag)
    {
        return (nationFlags & (int)_flag) != 0;
    }

    public void SetNationFlag(NationFlags _flag, bool _value)
    {
        if (_value) {
            nationFlags = nationFlags | (int)_flag;
        } else {
            nationFlags = nationFlags & ~((int)_flag);
        }
    }

    public void SendUserFlags()
    {
        // Send message to server to set user flags.
        Network.instance.SendCommand("action=set_user_flags|flags=" + userFlags);
        Debug.Log("User flags sent");
    }

    public void SendNationFlags()
    {
        // Send message to server to set nation flags.
        Network.instance.SendCommand("action=set_nation_flags|flags=" + nationFlags);
        Debug.Log("Nation flags sent");
    }

    public string GetRankString(int _rank)
    {
        // GB-Localization
        switch (_rank)
        {
            case RANK_SOVEREIGN: return LocalizationManager.GetTranslation("rank_sovereign"); //"Sovereign"
            case RANK_COSOVEREIGN: return LocalizationManager.GetTranslation("rank_cosovereign"); // "Cosovereign"
            case RANK_GENERAL: return LocalizationManager.GetTranslation("rank_general"); // "General"
            case RANK_CAPTAIN: return LocalizationManager.GetTranslation("rank_captain"); // "Captain"
            case RANK_COMMANDER: return LocalizationManager.GetTranslation("rank_commander"); // "Commander"
            case RANK_WARRIOR: return LocalizationManager.GetTranslation("rank_warrior") ; // "Warrior"
            case RANK_CIVILIAN: return LocalizationManager.GetTranslation("rank_civilian"); // "Civilian"
            default: return LocalizationManager.GetTranslation("Generic Text/none_word");
        }
    }

    public void OrbChangedHands(int _x, int _z, Boolean _occupied_by_player)
    {
        // Update the orb's record in the all time list
        for (int i = 0; i < nationOrbsList.Count; i++)
        {
            if ((nationOrbsList[i].x == _x) && (nationOrbsList[i].z == _z)) {
                nationOrbsList[i].currentlyOccupied = _occupied_by_player;
            }
        }

        // Update the orb's record in the monthly list
        for (int i = 0; i < nationOrbsMonthlyList.Count; i++)
        {
            if ((nationOrbsMonthlyList[i].x == _x) && (nationOrbsMonthlyList[i].z == _z)) {
                nationOrbsMonthlyList[i].currentlyOccupied = _occupied_by_player;
            }
        }

        // Refresh the list of nation orbs
        NationPanel.instance.RefreshNationOrbsList();
    }

    public bool NationIsInAllianceList(List<AllyData> _list, int _nationID)
    {
        foreach (AllyData cur_data in _list)
        {
            if (cur_data.ID == _nationID) {
                return true;
            }
        }

        return false;
    }

    public void ClearQuestRecords()
    {
        questRecords.Clear();
    }

    public QuestRecord GetQuestRecord(int _questID, bool _create)
    {
        if (questRecords.ContainsKey(_questID))
		{
			return questRecords[_questID];
		}
		else
		{
            if (_create)
            {
			    QuestRecord new_record = new QuestRecord();
			    new_record.ID = _questID;
			    questRecords.Add(_questID, new_record);
			    return new_record;
            }
            else
            {
                return null;
            }
		}
    }

    public bool FealtyPreventsAction()
    {
        // Fealty can only prevent actions on the mainland.
        if (GameData.instance.mapMode != MapMode.MAINLAND) {
            return false;
        }

        if (GameData.instance.fealtyEndTime > Time.unscaledTime) 
        {
            String text = LocalizationManager.GetTranslation("fealty_in_effect")
                .Replace("{nation}", GameData.instance.fealtyNationName)
                .Replace("{level_range}", (GameData.instance.level >= 80) ? "80+" : ((GameData.instance.level >= 40) ? "40-79" : "1-39"))
                .Replace("{duration}", GameData.instance.GetDurationText((int)(GameData.instance.fealtyEndTime - Time.unscaledTime)));

            // Display requestor warning that attacking is not allowed due to fealty.
            Requestor.Activate(0, 0, null, text, LocalizationManager.GetTranslation("Generic Text/okay"), "");

            return true;
        }
        else if (GameData.instance.fealtyRequestPermission)
        {
            String text = LocalizationManager.GetTranslation("fealty_tier_request_permission")
                .Replace("{nation}", GameData.instance.nationName)
                .Replace("{level_range}", (GameData.instance.level >= 80) ? "80+" : ((GameData.instance.level >= 40) ? "40-79" : "1-39"))
                .Replace("{duration}", GameData.instance.GetDurationText((GameData.instance.level >= 80) ? (3600 * 24) : ((GameData.instance.level >= 40) ? (3600 * 8) : (60 * 30))));

            // Display requestor warning that taking this action will declare fealty to this nation.
            Requestor.Activate(0, 0, null, text, LocalizationManager.GetTranslation("Generic Text/okay"), "");

            GameData.instance.fealtyRequestPermission = false;

            return true;
        }

        if (GameData.instance.fealtyTournamentEndTime > Time.unscaledTime) 
        {
            String text = LocalizationManager.GetTranslation("fealty_tournament_in_effect")
                .Replace("{nation}", GameData.instance.fealtyNationName);

            // Display requestor warning that attacking is not allowed due to tournament fealty.
            Requestor.Activate(0, 0, null, text, LocalizationManager.GetTranslation("Generic Text/okay"), "");

            return true;
        }
        else if (GameData.instance.fealtyRequestPermission)
        {
            String text = LocalizationManager.GetTranslation("fealty_tournament_request_permission")
                .Replace("{nation}", GameData.instance.nationName);

            // Display requestor warning that taking this action will declare fealty to this nation.
            Requestor.Activate(0, 0, null, text, LocalizationManager.GetTranslation("Generic Text/okay"), "");

            GameData.instance.fealtyRequestPermission = false;

            return true;
        }

        return false;
    }

    public int GetTechCount(int _techID)
    {
        if (techCount.ContainsKey(_techID) == false) {
            return 0;
        }

        return techCount[_techID];
    }

    public void SetTechCount(int _techID, int _count)
    {
        if (_count == 0) {
            techCount.Remove(_techID);
        } else {
            techCount[_techID] = _count;
        }
    }

    public bool IsBuildAvailable(int _buildID)
	{
		return (availableBuilds.ContainsKey(_buildID) && availableBuilds[_buildID]);
	}

	public int GetAvailableUpgrade(int _buildID)
	{
		return (availableUpgrades.ContainsKey(_buildID) == false) ? -1 : availableUpgrades[_buildID];
	}

    public void InitAvailableBuilds()
    {
        // Clear the available builds
        availableBuilds.Clear();

        // Add all of the initially available builds.
        foreach (KeyValuePair<int, BuildData> entry in BuildData.builds)
        {
            if (entry.Value.initial) {
                availableBuilds[entry.Key] = true;
            }
        }
    }

    public void UpdateBuildsForAdvance(int _techID)
    {
        TechData tech_data = TechData.GetTechData(_techID);

        // This only occurs if a nation has a tech that no longer exists.
        if (tech_data == null) {
            return;
        }

		if (tech_data.new_build != -1)
        {
            BuildData build_data = BuildData.GetBuildData(tech_data.new_build);
			if (build_data.upgrades != -1)
			{
           		// Record what build the current technology's new_build is the available upgrade for.
				availableUpgrades[build_data.upgrades] = tech_data.new_build;
			}
            else
            {
		        // Record that this tech's new_build is available (unless it's already been recorded as being obsolete).
                if (availableBuilds.ContainsKey(tech_data.new_build) == false) {
			        availableBuilds[tech_data.new_build] = true;
                }
            }
		}

		// Record that this tech's obsolete_build is not available.
		if (tech_data.obsolete_build != -1) {
			availableBuilds[tech_data.obsolete_build] = false;
		}
    }

    public int GetBuildCount(int _buildID)
	{
		return (builds.ContainsKey(_buildID) == false) ? 0 : builds[_buildID];
	}

    public int GetSecondsRemaining(int _techID)
    {
        if (tempTechExpireTime.ContainsKey(_techID)) {
            return (int)Math.Max(0f, Math.Ceiling(tempTechExpireTime[_techID] - Time.unscaledTime));
        } else {
            return 0;
        }
    }

    public float GetFinalStatTech()
    {
        return (statTechFromPerms + statTechFromTemps + (statTechFromResources * GetFinalGeoEfficiency())) * tech_mult;
    }

    public float GetFinalStatBio()
    {
        return (statBioFromPerms + statBioFromTemps + (statBioFromResources * GetFinalGeoEfficiency())) * bio_mult;
    }

    public float GetFinalStatPsi()
    {
        return (statPsiFromPerms + statPsiFromTemps + (statPsiFromResources * GetFinalGeoEfficiency())) * psi_mult;
    }

    public float GetFinalExperienceMultiplier()
    {
        return (xpMultiplierFromPerms + xpMultiplierFromTemps + (xpMultiplierFromResources * GetFinalGeoEfficiency()));
    }

    public float GetMainlandManpowerRate()
    {
        return (manpowerRateFromPerms + manpowerRateFromTemps + (manpowerRateFromResources * GetFinalGeoEfficiency())) * manpower_rate_mult;
    }

    public float GetHomelandManpowerRate()
    {
        return (manpowerRateFromPerms + manpowerRateFromTemps + (manpowerRateFromResources * GetFinalGeoEfficiency())) * manpower_rate_mult * (manpowerRateHomelandFraction / GameData.instance.manpowerGenMultiplier);
    }

    public float GetFinalManpowerRate()
    {
        switch (mapMode)
        {
            case MapMode.MAINLAND:
                return (manpowerRateFromPerms + manpowerRateFromTemps + (manpowerRateFromResources * GetFinalGeoEfficiency())) * manpower_rate_mult;
            case MapMode.HOMELAND:
                return (manpowerRateFromPerms + manpowerRateFromTemps + (manpowerRateFromResources * GetFinalGeoEfficiency())) * manpower_rate_mult * (manpowerRateHomelandFraction / GameData.instance.manpowerGenMultiplier);
            case MapMode.RAID:
                return 0;
            default:
                return 0;
        }
    }

    public float GetFinalManpowerRateMinusBurn()
    {
        switch (mapMode)
        {
            case MapMode.MAINLAND:
                return (manpowerRateFromPerms + manpowerRateFromTemps + (manpowerRateFromResources * GetFinalGeoEfficiency())) * manpower_rate_mult - manpowerBurnRate;
            case MapMode.HOMELAND:
                //Debug.Log("manpowerRateFromPerms: " + manpowerRateFromPerms + ", manpowerRateFromTemps: " + manpowerRateFromTemps + ", manpowerRateFromResources: " + manpowerRateFromResources + ", GetFinalGeoEfficiency(): " + GetFinalGeoEfficiency() + ", manpowerGenMultiplier: " + GameData.instance.manpowerGenMultiplier + ", manpower_rate_mult: " + manpower_rate_mult + ", manpowerBurnRate: " + manpowerBurnRate + ", manpowerRateHomelandFraction: " + manpowerRateHomelandFraction);
                return ((manpowerRateFromPerms + manpowerRateFromTemps + (manpowerRateFromResources * GetFinalGeoEfficiency())) * manpower_rate_mult - manpowerBurnRate) * (manpowerRateHomelandFraction / GameData.instance.manpowerGenMultiplier);
            case MapMode.RAID:
                return 0;
            default:
                return 0;
        }
    }

    public float GetFinalEnergyRate()
    {
        switch (mapMode)
        {
            case MapMode.MAINLAND:
                return (energyRateFromPerms + energyRateFromTemps + (energyRateFromResources * GetFinalGeoEfficiency())) * energy_rate_mult;
            case MapMode.HOMELAND:
                return (energyRateFromPerms + energyRateFromTemps + (energyRateFromResources * GetFinalGeoEfficiency())) * energy_rate_mult * energyRateHomelandFraction;
            case MapMode.RAID:
                return (energyRateFromPerms + energyRateFromTemps + (energyRateFromResources * GetFinalGeoEfficiency())) * energy_rate_mult * energyRateRaidlandFraction;
            default:
                return 0;
        }
    }

    public int GetFinalManpowerMax()
    {
        switch (mapMode)
        {
            case MapMode.MAINLAND:
                return Mathf.RoundToInt(GameData.instance.manpowerMax * GameData.instance.manpower_max_mult);
            case MapMode.HOMELAND:
            case MapMode.RAID:
                return Mathf.RoundToInt(GameData.instance.manpowerMax * GameData.instance.manpower_max_mult * (manpowerMaxHomelandFraction / GameData.instance.manpowerGenMultiplier));
            default:
                return 0;
        }
    }

    public int GetHomelandManpowerMax()
    {
        return Mathf.RoundToInt(GameData.instance.manpowerMax * GameData.instance.manpower_max_mult * (manpowerMaxHomelandFraction / GameData.instance.manpowerGenMultiplier));
    }

    public int GetFinalEnergyMax()
    {
        return Mathf.RoundToInt(GameData.instance.energyMax * GameData.instance.energy_max_mult);
    }

    public void DetermineManpowerBurnRate()
    {
   		float final_manpower_max = GameData.instance.manpowerMax * GameData.instance.manpower_max_mult;

		manpowerBurnRate = 0f;
		manpowerBurnRate += Mathf.Pow(Mathf.Max(0f, ((float)statTechFromResources / (float)statTechFromPerms) - resourceBonusCap), manpowerBurnExponent) * (manpowerBurnFractionOfManpowerMax * final_manpower_max);
		manpowerBurnRate += Mathf.Pow(Mathf.Max(0f, ((float)statBioFromResources / (float)statBioFromPerms) - resourceBonusCap), manpowerBurnExponent) * (manpowerBurnFractionOfManpowerMax * final_manpower_max);
		manpowerBurnRate += Mathf.Pow(Mathf.Max(0f, ((float)statPsiFromResources / (float)statPsiFromPerms) - resourceBonusCap), manpowerBurnExponent) * (manpowerBurnFractionOfManpowerMax * final_manpower_max);
		manpowerBurnRate += Mathf.Pow(Mathf.Max(0f, ((float)energyRateFromResources / (float)energyRateFromPerms) - resourceBonusCap), manpowerBurnExponent) * (manpowerBurnFractionOfManpowerMax * final_manpower_max);
		manpowerBurnRate += Mathf.Pow(Mathf.Max(0f, ((float)manpowerRateFromResources / (float)manpowerRateFromPerms) - resourceBonusCap), manpowerBurnExponent) * (manpowerBurnFractionOfManpowerMax * final_manpower_max);
		manpowerBurnRate += Mathf.Pow(Mathf.Max(0f, ((float)xpMultiplierFromResources / (float)xpMultiplierFromPerms) - resourceBonusCap), manpowerBurnExponent) * (manpowerBurnFractionOfManpowerMax * final_manpower_max);
    }

    public float DetermineStatManpowerPenalty(float _valueFromPerms, float _valueFromResources)
    {
        if ((_valueFromResources / _valueFromPerms) > GameData.instance.resourceBonusCap)
        {
       		float final_manpower_max = GameData.instance.manpowerMax * GameData.instance.manpower_max_mult;
            return Mathf.Pow(Mathf.Max(0f, (_valueFromResources / _valueFromPerms) - GameData.instance.resourceBonusCap), GameData.instance.manpowerBurnExponent) * (GameData.instance.manpowerBurnFractionOfManpowerMax * final_manpower_max);
        }
        else
        {
            return 0f;
        }
    }

    public float GetFinalEnergyBurnRate()
    {
        if (GameData.instance.current_footprint == null)
        {
            Debug.Log("ERROR: GetFinalEnergyBurnRate() current_footprint is NULL!");
            return 0f;
        }

        float final_energy_rate = GameData.instance.GetFinalEnergyRate();
        float energy_burn_rate = GameData.instance.current_footprint.energy_burn_rate;

        // Add the incognito energy burn to the nation's mainland energy burn rate, if appropriate.
        energy_burn_rate += GetIncognitoEnergyPenalty();

		if (energy_burn_rate > final_energy_rate) {
			return Mathf.Pow(energy_burn_rate / final_energy_rate, GameData.instance.overburnPower) * final_energy_rate;
		} else {
			return energy_burn_rate;
		}
    }

    public float GetIncognitoEnergyPenalty()
    {
        if ((GameData.instance.mapMode == MapMode.MAINLAND) && GameData.instance.GetNationFlag(GameData.NationFlags.INCOGNITO)) {
			return (GameData.instance.GetFinalEnergyRate() * GameData.instance.incognitoEnergyBurn);
		} else {
            return 0;
        }
    }

    public int GetSupportableArea()
    {
        return (int)((supportableAreaBase + ((level - 1) * supportableAreaPerLevel)) * ((mapMode == MapMode.HOMELAND) ? supportableAreaHomelandFraction : ((mapMode == MapMode.RAID) ? supportableAreaRaidlandFraction : 1f)));
    }

    public float GetFinalGeoEfficiency()
    {
        if (GameData.instance.current_footprint == null)
        {
            Debug.Log("ERROR: GetFinalGeoEfficiency() current_footprint is NULL!");
            return 0f;
        }

        return Mathf.Min(Mathf.Max(GameData.instance.current_footprint.geo_efficiency_base + GameData.instance.geo_efficiency_modifier, GameData.instance.geographicEfficiencyMin), GameData.instance.geographicEfficiencyMax);
    }

    public bool BenefitsFromGeoEfficiency()
    {
        bool bonuses_from_resources = ((GameData.instance.xpMultiplierFromResources + GameData.instance.statTechFromResources + GameData.instance.statBioFromResources + GameData.instance.statPsiFromResources + GameData.instance.manpowerRateFromResources + GameData.instance.energyRateFromResources) > 0f);
        bool supporting_defenses = (GameData.instance.GetFinalEnergyBurnRate() > 0f);

        // Return true if this nation gets bonuses from resources, or is supporting defenses. Either way, the nation benefits from having good geo efficiency.
        return bonuses_from_resources || supporting_defenses;
    }

    public void UpdateEndOfDayTime()
    {
        if (endOfDayTime < Time.unscaledTime)
        {
            // Advance to end of new day.
            endOfDayTime += (3600 * 24);
            buyEnergyDayAmount = 0;
            mainland_footprint.buy_manpower_day_amount = 0;
            homeland_footprint.buy_manpower_day_amount = 0;
            raidland_footprint.buy_manpower_day_amount = 0;
        }
    }

    public string ConstructTimeRemainingString(int _techID)
    {
        string return_string = "";

        int seconds_remaining = GameData.instance.GetSecondsRemaining(_techID);

        if (seconds_remaining >= 86400) {
            return_string = (seconds_remaining / 86400) + " " + (((seconds_remaining / 86400) > 1) ? LocalizationManager.GetTranslation("time_days") : LocalizationManager.GetTranslation("time_day"));
        } else if (seconds_remaining >= 3600) {
            return_string = (seconds_remaining / 3600) + " " + (((seconds_remaining / 3600) > 1) ? LocalizationManager.GetTranslation("time_hours") : LocalizationManager.GetTranslation("time_hour"));
        } else if (seconds_remaining >= 60) {
            return_string = (seconds_remaining / 60) + " " + (((seconds_remaining / 60) > 1) ? LocalizationManager.GetTranslation("time_minutes") : LocalizationManager.GetTranslation("time_minute"));
        } else if (seconds_remaining >= 1) {
            return_string = seconds_remaining + " " + ((seconds_remaining > 1) ? LocalizationManager.GetTranslation("time_seconds") : LocalizationManager.GetTranslation("time_second"));
        }

        return return_string;
    }

    public string GetStatValueString(Stat _stat)
    {
        // Make sure we don't try to access the current footprint if there isn't yet one.
        if ((GameData.instance == null) || (GameData.instance.current_footprint == null)) 
        {
            Debug.Log("GetStatValueString(" + _stat + ") called when there is no current footprint. GameData.instance: " + GameData.instance);
            return "";
        }

        switch (_stat)
        {
            case Stat.LEVEL: return "" + GameData.instance.level;
            case Stat.XP: return String.Format("{0:n0}", GameData.instance.xp);
            case Stat.TOTAL_AREA: return string.Format("{0:n0}", GameData.instance.current_footprint.area);
            case Stat.SUPPORTABLE_AREA: return string.Format("{0:n0}", GameData.instance.GetSupportableArea());
            case Stat.INTERIOR_AREA: return string.Format("{0:n0}", (GameData.instance.current_footprint.area - GameData.instance.current_footprint.border_area));
            case Stat.BORDER_AREA: return string.Format("{0:n0}", GameData.instance.current_footprint.border_area);
            case Stat.PERIMETER: return string.Format("{0:n0}", GameData.instance.current_footprint.perimeter);
            case Stat.GEOGRAPHIC_EFFICIENCY: return "" + String.Format("{0:#,#.##}", GetFinalGeoEfficiency() * 100f) + "%";
	        case Stat.TECH: return string.Format("{0:n0}", Mathf.RoundToInt(GameData.instance.GetFinalStatTech()));
	        case Stat.BIO: return string.Format("{0:n0}", Mathf.RoundToInt(GameData.instance.GetFinalStatBio()));
	        case Stat.PSI: return string.Format("{0:n0}", Mathf.RoundToInt(GameData.instance.GetFinalStatPsi()));
   	        case Stat.XP_MULTIPLIER: return "" + Mathf.RoundToInt(GameData.instance.GetFinalExperienceMultiplier() * 100f) + "%";
	        case Stat.MANPOWER_MAX: return string.Format("{0:n0}", GetFinalManpowerMax());
            case Stat.MANPOWER_RATE: return string.Format("{0:n0}", GetFinalManpowerRate());
            case Stat.MANPOWER_RATE_MINUS_BURN: return string.Format("{0:n0}", GetFinalManpowerRateMinusBurn());
            case Stat.ATTACK_MANPOWER: return "" + Mathf.RoundToInt(GameData.instance.manpowerPerAttack * GameData.instance.attack_manpower_mult);
	        case Stat.CRIT_CHANCE: return "" + Mathf.RoundToInt(GameData.instance.critChance * 100f) + "%";
	        case Stat.SIMULTANEOUS_ACTIONS: return "" + GameData.instance.maxSimultaneousProcesses;
	        case Stat.ENERGY_MAX: return string.Format("{0:n0}", GetFinalEnergyMax());
            case Stat.ENERGY_RATE: return string.Format("{0:n0}", GetFinalEnergyRate());
            case Stat.ENERGY_BURN_RATE: return string.Format("{0:n0}", GameData.instance.current_footprint.energy_burn_rate);
            case Stat.HP_PER_SQUARE: return "" + Mathf.RoundToInt(GameData.instance.hitPointBase * GameData.instance.hp_per_square_mult);
	        case Stat.HP_RESTORE: return "" + Mathf.RoundToInt(GameData.instance.hitPointRate * GameData.instance.hp_restore_mult);
   	        case Stat.SPLASH_DAMAGE: return "" + Mathf.RoundToInt(GameData.instance.splashDamage * 100f) + "%";
            case Stat.SALVAGE_VALUE: return "" + Mathf.RoundToInt(GameData.instance.salvageValue * 100f) + "%";
	        case Stat.WALL_DISCOUNT: return "" + Mathf.RoundToInt(GameData.instance.wallDiscount * 100f) + "%";
	        case Stat.STRUCTURE_DISCOUNT: return "" + Mathf.RoundToInt(GameData.instance.structureDiscount * 100f) + "%";
	        case Stat.INVISIBILITY: return GameData.instance.invisibility ? LocalizationManager.GetTranslation("Generic Text/yes_word") : LocalizationManager.GetTranslation("Generic Text/no_word");
            case Stat.INSURGENCY: return GameData.instance.insurgency ? LocalizationManager.GetTranslation("Generic Text/yes_word") : LocalizationManager.GetTranslation("Generic Text/no_word");
            case Stat.TOTAL_DEFENSE: return GameData.instance.total_defense ? LocalizationManager.GetTranslation("Generic Text/yes_word") : LocalizationManager.GetTranslation("Generic Text/no_word");
            case Stat.MAX_ALLIANCES: return "" + GameData.instance.maxNumAlliances;
            case Stat.REBIRTH_LEVEL_BONUS: return "" + GameData.instance.rebirth_level_bonus;
            case Stat.NUM_STORAGE_STRUCTURES: return string.Format("{0:n0}", numStorageStructures);
            case Stat.MANPOWER_STORED: return string.Format("{0:n0}", manpowerStored);
            case Stat.ENERGY_STORED: return string.Format("{0:n0}", energyStored);
            case Stat.MANPOWER_AVAILABLE: return string.Format("{0:n0}", manpowerAvailable);
            case Stat.ENERGY_AVAILABLE: return string.Format("{0:n0}", energyAvailable);
            case Stat.FINAL_ENERGY_BURN_RATE: return string.Format("{0:n0}", GetFinalEnergyBurnRate());
            case Stat.STORAGE_XP_RATE: return string.Format("{0:n0}", (int)((sharedEnergyXPPerHour * sharedEnergyFill) + (sharedManpowerXPPerHour * sharedManpowerFill) + 0.5f));
            case Stat.PENDING_XP: return String.Format("{0:n0}", GameData.instance.pending_xp);
            case Stat.ENERGY: return string.Format("{0:n0}", energy);
        }

        return "GetStatValueString() error for " + _stat + ".";
    }

    public string GetDurationText(int _seconds)
    {
        // GB:Localization

        if (_seconds <= 60) // 1 minute
        {
            return _seconds + " " + ((_seconds != 1) ? LocalizationManager.GetTranslation("time_seconds") : LocalizationManager.GetTranslation("time_second"));
        }
        else if (_seconds <= 3600) // 1 hour
        {
            return (_seconds / 60) + " " + (((_seconds / 60) > 1) ? LocalizationManager.GetTranslation("time_minutes") : LocalizationManager.GetTranslation("time_minute"));
        }
        else if (_seconds <= 86400) // 1 day
        {
            return (_seconds / 3600) + " " + (((_seconds / 3600) > 1) ? LocalizationManager.GetTranslation("time_hours") : LocalizationManager.GetTranslation("time_hour")) + (((_seconds % 3600) < 60) ? "" : (" " + ((_seconds % 3600) / 60) + " " + ((((_seconds % 3600) / 60) > 1) ? LocalizationManager.GetTranslation("time_minutes") : LocalizationManager.GetTranslation("time_minute"))));
        }
        else
        {
            return (_seconds / 86400) + " " + (((_seconds / 86400) > 1) ? LocalizationManager.GetTranslation("time_days") : LocalizationManager.GetTranslation("time_day")) + (((_seconds % 86400) < 3600) ? "" : (" " + ((_seconds % 86400) / 3600) + " " + ((((_seconds % 86400) / 3600) > 1) ? LocalizationManager.GetTranslation("time_hours") : LocalizationManager.GetTranslation("time_hour"))));
        }
    }

    public string GetDurationClockText(int _seconds)
    {
        string result = "";

        // Hours
        int hours = (_seconds / 3600);
        if (hours > 0) {
            result += hours + ":";
        }

        // Minutes
        int mins = (_seconds % 3600) / 60;
        if (hours > 0) {
            if (mins < 10) result += "0";
        }
        result += mins + ":";

        // Seconds
        int secs = (_seconds % 60);
        if (secs < 10) result += "0";
        result += secs;

        return result;
    }

    public string GetDurationShortText(int _seconds)
    {
        if (_seconds <= 60) // 1 minute
        {
            return _seconds + "s";
        }
        else if (_seconds <= 3600) // 1 hour
        {
            return (_seconds / 60) + "m " + (_seconds % 60) + "s";
        }
        else if (_seconds <= 86400) // 1 day
        {
            return (_seconds / 3600) + "h " + ((_seconds % 3600) / 60) + "m";
        }
        else
        {
            return (_seconds / 86400) + "d " + ((_seconds % 86400) / 3600) + "h";
        }
    }

    public void UserEventOccurred(UserEventType _event_type)
    {
        userEventFrequency[(int)_event_type] = GetUserEventFrequency(_event_type) + 1;
        prevUserEventTime[(int)_event_type] = Time.unscaledTime;
        //Debug.Log("UserEventOccurred(" + _event_type + "). Frequency: " + GetUserEventFrequency(_event_type));
    }

    public float GetUserEventFrequency(UserEventType _event_type)
    {
        float minsSincePrev = (Time.unscaledTime - prevUserEventTime[(int)_event_type]) / 60f;
        return userEventFrequency[(int)_event_type] / Mathf.Pow(2, minsSincePrev); // Discount prev values over time, by 1/2 per minute.
    }

    public int GetNumBlocksTakenFromPlayerNation(int _nationID)
    {
        return blocksTakenFromPlayerNation.ContainsKey(_nationID) ? blocksTakenFromPlayerNation[_nationID] : 0;
    }

    public int GetNumBlocksTakenByPlayerNation(int _nationID)
    {
        return blocksTakenByPlayerNation.ContainsKey(_nationID) ? blocksTakenByPlayerNation[_nationID] : 0;
    }

    public void BlockCaptured(int _x, int _z, int _newNationID, int _oldNationID, BlockData _block_data, float _delay)
    {
        if ((_newNationID == nationID) && (_oldNationID != nationID) && (_oldNationID != -1)) 
        {
            // Keep count of blocks of _oldNationID nation that have been taken by the player's nation.
            if (blocksTakenByPlayerNation.ContainsKey(_oldNationID)) {
                blocksTakenByPlayerNation[_oldNationID] = blocksTakenByPlayerNation[_oldNationID] + 1;
            } else {
                blocksTakenByPlayerNation[_oldNationID] = 1;
            }
        }

        if ((_newNationID != nationID) && (_oldNationID == nationID) && (_newNationID != -1)) 
        {
            // Keep track of how many blocks of the player's natio have been taken by _newNationID nation.
            if (blocksTakenFromPlayerNation.ContainsKey(_newNationID)) {
                blocksTakenFromPlayerNation[_newNationID] = blocksTakenFromPlayerNation[_newNationID] + 1;
            } else {
                blocksTakenFromPlayerNation[_newNationID] = 1;
            }
        }

        StartCoroutine(BlockCaptured_Coroutine(_x, _z, _newNationID, _oldNationID, _block_data, _delay));
    }

    public IEnumerator BlockCaptured_Coroutine(int _x, int _z, int _newNationID, int _oldNationID, BlockData _block_data, float _delay)
    {
        // Wait through the delay.
        yield return new WaitForSeconds(_delay);

        // Tell the sound system that a block has been captured, in case it may cue some music.
        Sound.instance.BlockCaptured(_x, _z, _newNationID, _oldNationID, _block_data);
    }

    public void DefenseTriggered(int _triggerNationID, int _defenseNationID)
    {
        // Tell the sound system that a defense has been triggered, in case it may cue some music.
        Sound.instance.DefenseTriggered(_triggerNationID, _defenseNationID);
    }

    public int GetPrice(int _advanceID)
    {
        if (prices.ContainsKey(_advanceID)) {
            return prices[_advanceID];
        } else {
            return 0;
        }
    }

    public int DetermineFealtyTier(int _level)
    {
        if (_level >= 80) {
            return 3;
        } else if (_level >= 40) {
            return 2;
        } else {
            return 1;
        }
    }

    public int DetermineNumShardsPlaced()
    {
        int num_shards = 0;
        if (GetBuildCount(200) > 0) num_shards++;
        if (GetBuildCount(201) > 0) num_shards++;
        if (GetBuildCount(202) > 0) num_shards++;

        return num_shards;
    }

    public int DetermineNumRaidStars(int _raidFlags)
    {
        int count = 0;
        if ((_raidFlags & (int)RaidFlags.PROGRESS_50_PERCENT) != 0) count++;
        if ((_raidFlags & (int)RaidFlags.PROGRESS_100_PERCENT) != 0) count++;
        if ((_raidFlags & (int)RaidFlags.RED_SHARD) != 0) count++;
        if ((_raidFlags & (int)RaidFlags.GREEN_SHARD) != 0) count++;
        if ((_raidFlags & (int)RaidFlags.BLUE_SHARD) != 0) count++;

        return count;
    }

    public MapFlagRecord SetMapFlag(int _x, int _z, string _text)
    {
        // Remove any existing map flag at the given coords.
        for (var i = 0; i < mapFlags.Count; i++) 
        {
            if ((mapFlags[i].x == _x) && (mapFlags[i].z == _z))
            {
                mapFlags.RemoveAt(i);
                break;
            }
        }

        // Add a new map flag at the given coords.
        MapFlagRecord mapFlagRecord = new MapFlagRecord(_x, _z, _text);
        GameData.instance.mapFlags.Add(mapFlagRecord);

        return mapFlagRecord;
    }

    public void LogMemoryStats()
    {
        long type_use;

        // Mesh
        type_use = 0;
        UnityEngine.Object[] meshes = Resources.FindObjectsOfTypeAll(typeof(Mesh));
        foreach (Mesh t in meshes) {
            type_use += Profiler.GetRuntimeMemorySizeLong((Mesh)t);
        }
        Debug.Log("Memory used by meshes: " + type_use + " bytes.");

        // Textures
        type_use = 0;
        UnityEngine.Object[] textures = Resources.FindObjectsOfTypeAll(typeof(Texture));
        foreach (Texture t in textures) {
            type_use += Profiler.GetRuntimeMemorySizeLong((Texture)t);
        }
        Debug.Log("Memory used by textures: " + type_use + " bytes.");

        // Materials
        type_use = 0;
        UnityEngine.Object[] materials = Resources.FindObjectsOfTypeAll(typeof(Material));
        foreach (Material t in materials) {
            type_use += Profiler.GetRuntimeMemorySizeLong((Material)t);
        }
        Debug.Log("Memory used by materials: " + type_use + " bytes.");

        // Animations
        type_use = 0;
        UnityEngine.Object[] animations = Resources.FindObjectsOfTypeAll(typeof(Animation));
        foreach (Animation t in animations) {
            type_use += Profiler.GetRuntimeMemorySizeLong((Animation)t);
        }
        Debug.Log("Memory used by animations: " + type_use + " bytes.");
    }
}

public class NationData
{
    public enum EmblemColor
    {
        WHITE   = 0,
        BLACK   = 1,
        RED     = 2,
        YELLOW  = 3,
        GREEN   = 4,
        BLUE    = 5,
        PURPLE  = 6
    }

	private String name;
	public int flags;
	public float r, g, b;
    public int emblem_index = -1, emblem_u = -1, emblem_v = -1;
    public EmblemColor emblem_color = EmblemColor.WHITE;
    public float sharedEnergyFill = 0f, sharedManpowerFill = 0f;

    public bool label_required = false;
    public int label_block_x = -1, label_block_z = -1, label_score = -100;
    public GameObject label = null;

    public void DetermineEmblemUV()
    {
        if ((emblem_index >= 0) && (MapView.instance.emblemsPerRow > 0))
        {
            emblem_u = (emblem_index % MapView.instance.emblemsPerRow) * MapView.EMBLEM_DIM;
            emblem_v = (emblem_index / MapView.instance.emblemsPerRow) * MapView.EMBLEM_DIM;
        }
        else
        {
            emblem_u = -1;
            emblem_v = -1;
        }
    }

    public bool GetFlag(GameData.NationFlags _flag)
    {
        return ((flags & (int)_flag) != 0);
    }

    public String GetName(bool _process_incognito)
    {
        if (_process_incognito && GetFlag(GameData.NationFlags.INCOGNITO)) {
            return LocalizationManager.GetTranslation("incognito_nation");
        } else {
            return name;
        }
    }

    public void SetName(String _name)
    {
        name = _name;
    }
}

public class AllyData
{
    public String name;
    public int ID, paymentOffer = 0;
}

public class AllyDataComparer : IComparer<AllyData>
{
    public int Compare(AllyData a, AllyData b)
    {
        // Sort by username alphabetical order.
        int compareResult = String.Compare(a.name, b.name, StringComparison.OrdinalIgnoreCase);
        if (compareResult < 1) return -1;
        if (compareResult > 1) return 1;
        return 0;
    }
}

public class MemberEntryData
{
    public int userID;
    public string username;
    public int points, rank;
    public bool logged_in, absentee;

    public MemberEntryData(int _userID, string _username, int _points, int _rank, bool _logged_in, bool _absentee)
    {
        userID = _userID;
        username = _username;
        points = _points;
        rank = _rank;
        logged_in = _logged_in;
        absentee = _absentee;
    }
}

public class MemberEntryDataComparer : IComparer<MemberEntryData>
{
    public int Compare(MemberEntryData a, MemberEntryData b)
    {
        // Sort first by whether the user is logged in, then by username alphabetical order.

        if (a.logged_in && !b.logged_in) {
            return -1;
        }

        if (!a.logged_in && b.logged_in) {
            return 1;
        }

        int compareResult = String.Compare(a.username, b.username, StringComparison.OrdinalIgnoreCase);
        if (compareResult < 1) return -1;
        if (compareResult > 1) return 1;
        return 0;
    }
}

public class MemberEntryDataRankComparer : IComparer<MemberEntryData>
{
    public int Compare(MemberEntryData a, MemberEntryData b)
    {
        // Sort first by rank, then by username alphabetical order.

        if (a.rank < b.rank) {
            return -1;
        }

        if (a.rank > b.rank) {
            return 1;
        }

        int compareResult = String.Compare(a.username, b.username, StringComparison.OrdinalIgnoreCase);
        if (compareResult < 1) return -1;
        if (compareResult > 1) return 1;
        return 0;
    }
}

public class NationArea
{
    public int x, y;
}

public class Footprint
{
  	public int area, border_area, perimeter;
  	public float geo_efficiency_base;
    public float energy_burn_rate;
    public int manpower;
    public int buy_manpower_day_amount;
}

public class TargetRecord
{
    public int x, y, newNationID, battle_flags;
	public int full_hit_points, start_hit_points, end_hit_points, new_cur_hit_points, new_full_hit_points;
	public float hit_points_rate;
    public int wipe_nationID, wipe_flags;
    public float wipe_end_time;

    public float GetWipeEndTime(float _startActionTime) { return (wipe_end_time == -1f) ? -1f : (wipe_end_time + _startActionTime); }
}

public class QuestRecord
{
    public int ID = 0, cur_amount = 0;
    public int completed = 0, collected = 0;

    public void SetStatus(int _cur_amount, int _completed, int _collected) { cur_amount = _cur_amount; completed = _completed; collected = _collected; }
}

public class UserRanksRecord
{
    public int userID;
    public String username;
    public int user_xp, user_xp_monthly;
    public int user_followers, user_followers_monthly;

    public int GetValue(GameData.RanksListType _type)
    {
        switch (_type)
        {
            case GameData.RanksListType.USER_XP: return user_xp;
            case GameData.RanksListType.USER_XP_MONTHLY: return user_xp_monthly;
            case GameData.RanksListType.USER_FOLLOWERS: return user_followers;
            case GameData.RanksListType.USER_FOLLOWERS_MONTHLY: return user_followers_monthly;
            default: return 0;
        }
    }
}

public class NationRanksRecord
{
    public int nationID;
    public String nation_name;
    public int level_history, rebirth_count;
    public int nation_xp_history, nation_xp_monthly;
    public int prize_money_history, prize_money_history_monthly;
    public int raid_earnings_history, raid_earnings_history_monthly;
    public int orb_shard_earnings_history, orb_shard_earnings_history_monthly;
    public int medals_history, medals_history_monthly;
    public int quests_completed, quests_completed_monthly;
    public int tournament_trophies_history, tournament_trophies_history_monthly;
    public int donated_energy_history, donated_energy_history_monthly;
    public int donated_manpower_history, donated_manpower_history_monthly;
    public int captures_history, captures_history_monthly;
    public int max_area, max_area_monthly;
    public int tournament_current;

    public int GetValue(GameData.RanksListType _type)
    {
        switch (_type)
        {
            case GameData.RanksListType.NATION_XP: return nation_xp_history;
            case GameData.RanksListType.NATION_XP_MONTHLY: return nation_xp_monthly;
            case GameData.RanksListType.NATION_WINNINGS: return prize_money_history;
            case GameData.RanksListType.NATION_WINNINGS_MONTHLY: return prize_money_history_monthly;
            case GameData.RanksListType.NATION_RAID_EARNINGS: return raid_earnings_history;
            case GameData.RanksListType.NATION_RAID_EARNINGS_MONTHLY: return raid_earnings_history_monthly;
            case GameData.RanksListType.NATION_ORB_SHARD_EARNINGS: return orb_shard_earnings_history;
            case GameData.RanksListType.NATION_ORB_SHARD_EARNINGS_MONTHLY: return orb_shard_earnings_history_monthly;
            case GameData.RanksListType.NATION_MEDALS: return medals_history;
            case GameData.RanksListType.NATION_MEDALS_MONTHLY: return medals_history_monthly;
            case GameData.RanksListType.NATION_TOURNAMENT_TROPHIES: return tournament_trophies_history;
            case GameData.RanksListType.NATION_TOURNAMENT_TROPHIES_MONTHLY: return tournament_trophies_history_monthly;
            case GameData.RanksListType.NATION_LEVEL: return level_history;
            case GameData.RanksListType.NATION_REBIRTHS: return rebirth_count;
            case GameData.RanksListType.NATION_QUESTS: return quests_completed;
            case GameData.RanksListType.NATION_QUESTS_MONTHLY: return quests_completed_monthly;
            case GameData.RanksListType.NATION_ENERGY_DONATED: return donated_energy_history;
            case GameData.RanksListType.NATION_ENERGY_DONATED_MONTHLY: return donated_energy_history_monthly;
            case GameData.RanksListType.NATION_MANPOWER_DONATED: return donated_manpower_history;
            case GameData.RanksListType.NATION_MANPOWER_DONATED_MONTHLY: return donated_manpower_history_monthly;
            case GameData.RanksListType.NATION_AREA: return max_area;
            case GameData.RanksListType.NATION_AREA_MONTHLY: return max_area_monthly;
            case GameData.RanksListType.NATION_CAPTURES: return captures_history;
            case GameData.RanksListType.NATION_CAPTURES_MONTHLY: return captures_history_monthly;
            case GameData.RanksListType.NATION_TOURNAMENT_CURRENT: return tournament_current;
            default: return 0;
        }
    }
}

public class OrbRanksRecord
{
    public int nationID;
    public String nation_name;
    public int orb_winnings, orb_winnings_monthly;
}

public class NationOrbRecord
{
    public int x, z, objectID, winnings;
    public Boolean currentlyOccupied;
}

public class RaidLogRecord
{
    public int raidID;
    public int enemyNationID;
    public string enemyNationName;
    public int enemyNationMedals;
    public int flags;
    public float startTime;
    public int percentageDefeated;
    public int rewardMedals;
    public int rewardCredits;
    public int rewardXP;
    public int rewardRebirth;
}

public class ReplayEventRecord
{
    public enum Event
    {
        SET_NATION_ID = 0,
	    CLEAR_NATION_ID = 1,
	    SET_OBJECT_ID = 2,
	    TOWER_ACTION = 3,
	    END = 4,
        EXT_DATA = 5,
	    SALVAGE = 6,
	    COMPLETE = 7,
        BATTLE = 8,
        TRIGGER_INERT = 9
    }

    public Event eventID;
    public float timestamp;
    public int x, z, subjectID, battle_flags;

    // Tower action
    public int build_type, invisible_time, duration, trigger_x, trigger_z, triggerNationID;
    public List<TargetRecord> targets = new List<TargetRecord>();

    // Extended data
    public int owner_nationID, wipe_nationID, wipe_flags;
    public int creation_time, completion_time, capture_time, crumble_time, wipe_end_time;
}

public class MapFlagRecord
{
    public enum SortMode
    {
        Text, 
        Location
    }

    public static SortMode sortMode = SortMode.Text;

    public int x, z;
    public string text;

    public MapFlagRecord(int _x, int _z, string _text)
    {
        x = _x;
        z = _z;
        text = _text;
    }
}

public class MapFlagRecordComparer : IComparer<MapFlagRecord>
{
    public int Compare(MapFlagRecord a, MapFlagRecord b)
    {
        if (MapFlagRecord.sortMode == MapFlagRecord.SortMode.Text) 
        {
            int result = String.Compare(a.text, b.text, StringComparison.OrdinalIgnoreCase);
            return (result == 0) ? ((a.x == b.x) ? ((a.z == b.z) ? 0 : ((a.z < b.z) ? -1 : 1)) : ((a.x < b.x) ? -1 : 1)) : result;
        } else {
            return (a.x == b.x) ? ((a.z == b.z) ? 0 : ((a.z < b.z) ? -1 : 1)) : ((a.x < b.x) ? -1 : 1);
        }
    }
}

public class ObjectRecord
{
    public int blockX, blockZ;
    public int objectID;
    public ObjectData objectData;

    public ObjectRecord(int _blockX, int _blockZ, int _objectID)
    {
        blockX = _blockX;
        blockZ = _blockZ;
        objectID = _objectID;

        // Fetch the object's data.
        objectData = ObjectData.GetObjectData(_objectID);
    }
}

public class ObjectRecordComparer : IComparer<ObjectRecord>
{
    public int Compare(ObjectRecord a, ObjectRecord b)
    {
        if (a.objectID == b.objectID)
        {
            return (a.blockX == b.blockX) ? ((a.blockZ == b.blockZ) ? 0 : ((a.blockZ < b.blockZ) ? -1 : 1)) : ((a.blockX < b.blockX) ? -1 : 1);
        }
        else
        {
            return String.Compare(a.objectData.name, b.objectData.name, StringComparison.OrdinalIgnoreCase);
        }
    }
}

public class ComplaintData
{
    public int ID, timestamp;
    public bool valid;
    public String issue, text;

    public int reporter_userID, reporter_nationID;
    public String reporter_userName, reporter_email, reporter_nationName;
    public int reporter_num_complaints_by, reporter_num_complaints_against, reporter_num_warnings_sent, reporter_num_chat_bans, reporter_num_game_bans, reporter_chat_ban_days, reporter_game_ban_days;

    public int reported_userID, reported_nationID;
    public String reported_userName, reported_email, reported_nationName;
    public int reported_num_complaints_by, reported_num_complaints_against, reported_num_warnings_sent, reported_num_chat_bans, reported_num_game_bans, reported_chat_ban_days, reported_game_ban_days;
 }