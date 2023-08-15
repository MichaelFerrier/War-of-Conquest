using UnityEngine;
using UnityEngine.UI;
using System;
using System.Collections;
using System.Collections.Generic;
using I2.Loc;

public class StatDetailsPanel : MonoBehaviour
{
    public static StatDetailsPanel instance;

    public TMPro.TextMeshProUGUI title_text;
    public Text description_text;
    public Text r1c1_text, r1c2_text, r1c3_text, r1c4_text, r1c5_text, r1c6_text;
    public Text r2c1_text, r2c2_text, r2c3_text, r2c4_text, r2c5_text, r2c6_text;
    public GameObject r1c1_object, r1c2_object, r1c3_object, r1c4_object, r1c5_object, r1c6_object;
    public GameObject r2c1_object, r2c2_object, r2c3_object, r2c4_object, r2c5_object, r2c6_object;

	public StatDetailsPanel()
    {
        instance = this;	
	}

    public void ActivateForBonus(TechData.Bonus _bonusID)
    {
        switch (_bonusID)
        {
            case TechData.Bonus.ATTACK_MANPOWER_MULT:
            case TechData.Bonus.ATTACK_MANPOWER: StatDetailsPanel.instance.Activate(GameData.Stat.ATTACK_MANPOWER); break;
            case TechData.Bonus.BIO_MULT:
            case TechData.Bonus.BIO: StatDetailsPanel.instance.Activate(GameData.Stat.BIO); break;
            case TechData.Bonus.CRIT_CHANCE: StatDetailsPanel.instance.Activate(GameData.Stat.CRIT_CHANCE); break;
            case TechData.Bonus.ENERGY_MAX_MULT:
            case TechData.Bonus.ENERGY_MAX: StatDetailsPanel.instance.Activate(GameData.Stat.ENERGY_MAX); break;
            case TechData.Bonus.ENERGY_RATE_MULT:
            case TechData.Bonus.ENERGY_RATE: StatDetailsPanel.instance.Activate(GameData.Stat.ENERGY_RATE); break;
            case TechData.Bonus.GEO_EFFICIENCY: StatDetailsPanel.instance.Activate(GameData.Stat.GEOGRAPHIC_EFFICIENCY); break;
            case TechData.Bonus.HP_PER_SQUARE_MULT:
            case TechData.Bonus.HP_PER_SQUARE: StatDetailsPanel.instance.Activate(GameData.Stat.HP_PER_SQUARE); break;
            case TechData.Bonus.HP_RESTORE_MULT:
            case TechData.Bonus.HP_RESTORE: StatDetailsPanel.instance.Activate(GameData.Stat.HP_RESTORE); break;
            case TechData.Bonus.INSURGENCY: StatDetailsPanel.instance.Activate(GameData.Stat.INSURGENCY); break;
            case TechData.Bonus.INVISIBILITY: StatDetailsPanel.instance.Activate(GameData.Stat.INVISIBILITY); break;
            case TechData.Bonus.MANPOWER_MAX_MULT:
            case TechData.Bonus.MANPOWER_MAX: StatDetailsPanel.instance.Activate(GameData.Stat.MANPOWER_MAX); break;
            case TechData.Bonus.MANPOWER_RATE_MULT:
            case TechData.Bonus.MANPOWER_RATE: StatDetailsPanel.instance.Activate(GameData.Stat.MANPOWER_RATE); break;
            case TechData.Bonus.MAX_ALLIANCES: StatDetailsPanel.instance.Activate(GameData.Stat.MAX_ALLIANCES); break;
            case TechData.Bonus.PSI_MULT:
            case TechData.Bonus.PSI: StatDetailsPanel.instance.Activate(GameData.Stat.PSI); break;
            case TechData.Bonus.SALVAGE_VALUE: StatDetailsPanel.instance.Activate(GameData.Stat.SALVAGE_VALUE); break;
            case TechData.Bonus.SIMULTANEOUS_ACTIONS: StatDetailsPanel.instance.Activate(GameData.Stat.SIMULTANEOUS_ACTIONS); break;
            case TechData.Bonus.SPLASH_DAMAGE: StatDetailsPanel.instance.Activate(GameData.Stat.SPLASH_DAMAGE); break;
            case TechData.Bonus.STRUCTURE_DISCOUNT: StatDetailsPanel.instance.Activate(GameData.Stat.STRUCTURE_DISCOUNT); break;
            case TechData.Bonus.TECH_MULT:
            case TechData.Bonus.TECH: StatDetailsPanel.instance.Activate(GameData.Stat.TECH); break;
            case TechData.Bonus.TOTAL_DEFENSE: StatDetailsPanel.instance.Activate(GameData.Stat.TOTAL_DEFENSE); break;
            case TechData.Bonus.WALL_DISCOUNT: StatDetailsPanel.instance.Activate(GameData.Stat.WALL_DISCOUNT); break;
            case TechData.Bonus.XP_MULTIPLIER: StatDetailsPanel.instance.Activate(GameData.Stat.XP_MULTIPLIER); break;
            default: break;
        }
    }
    
    public void Activate(GameData.Stat _statID)
    {
        string locDescText, totalText;

        float geo_efficiency = GameData.instance.GetFinalGeoEfficiency();
        string geo_efficiency_text = ((geo_efficiency < 1f) ? "<color=\"yellow\">" : "") + GameData.instance.GetStatValueString(GameData.Stat.GEOGRAPHIC_EFFICIENCY).ToString() + ((geo_efficiency < 1f) ? "</color>" : "");

        GameGUI.instance.OpenStatDetailsDialog();

        // GB-Localization
        string i2_base = LocalizationManager.GetTranslation("Stat Description/base_title");
        string i2_advances = LocalizationManager.GetTranslation("Stat Description/advances_title");
        string i2_total = LocalizationManager.GetTranslation("Stat Description/total_title");
        string i2_current = LocalizationManager.GetTranslation("Stat Description/current_title");
        string i2_temporary = LocalizationManager.GetTranslation("Stat Description/temporary_title"); // Temporary
        string i2_resources = LocalizationManager.GetTranslation("Stat Description/resources_title"); // Resources
        string i2_resources_short = LocalizationManager.GetTranslation("Stat Description/resources_short_title"); // Res
        string i2_excess_resources_short = LocalizationManager.GetTranslation("Stat Description/excess_resources_short_title"); // Excess Res
        string i2_plus_allies = LocalizationManager.GetTranslation("Stat Description/plus_allies_title"); // Plus Allies
        string i2_defense = LocalizationManager.GetTranslation("Stat Description/defense_title"); // Defense

        switch (_statID)
        {
            case GameData.Stat.LEVEL:
                // "The measure of our nation's advancement. For each level we gain, we receive an Advance Point which we can use to research a new advance. To increase from one level to the next we must earn a certain number of experience points."
                DisplayDetails("Level", LocalizationManager.GetTranslation("Stat Description/level"), "Level", GameData.instance.GetStatValueString(GameData.Stat.LEVEL), "", "", "", "", "", "", "", "", "", "");
                break;
            case GameData.Stat.XP:
                locDescText = LocalizationManager.GetTranslation("Stat Description/experience_points");
                // "A measure of our nation's learning and understanding, experience points are earned through successful attacks and defense, by taking control of resources and orbs, and by completing quests. To earn each advancing level requires an increasing number of experience points."
                // "Experience Points", "Experience Points (XP)"
                if (GameData.instance.pending_xp == 0)
                {
                    DisplayDetails(LocalizationManager.GetTranslation("Stat Description/experience_points_title"), locDescText, LocalizationManager.GetTranslation("Stat Description/experience_points_title") + " (XP)", GameData.instance.GetStatValueString(GameData.Stat.XP), "", "", "", "", "", "", "", "", "", "");
                }
                else
                {
                    locDescText += " " + LocalizationManager.GetTranslation("Stat Description/experience_points_pending")
                        .Replace("{[pending_xp_rate]}", string.Format("{0:n0}", GameData.instance.pendingXPPerHour));
                    DisplayDetails(LocalizationManager.GetTranslation("Stat Description/experience_points_title"), locDescText, LocalizationManager.GetTranslation("Stat Description/experience_points_title") + " (XP)", GameData.instance.GetStatValueString(GameData.Stat.XP), LocalizationManager.GetTranslation("Stat Description/experience_points_pending_title"), GameData.instance.GetStatValueString(GameData.Stat.PENDING_XP), "", "", "", "", "", "", "", "");
                }
                break;
            case GameData.Stat.TOTAL_AREA:
                // "The total number of land squares occupied by our nation."
                // "Total Area"
                DisplayDetails(LocalizationManager.GetTranslation("Stat Description/total_area_title"), LocalizationManager.GetTranslation("Stat Description/total_area"), LocalizationManager.GetTranslation("Stat Description/total_area_title"), GameData.instance.GetStatValueString(GameData.Stat.TOTAL_AREA), "", "", "", "", "", "", "", "", "", "");
                break;
            case GameData.Stat.SUPPORTABLE_AREA:
                // "The maximum number of land squares that our nation can support at full strength. If we occupy land in excess of our supportable area then our geographic efficiency will suffer, decreasing our energy production rate and making us more vulnerable to attacks. A nation's supportable area starts at {[supportable_area_base]} and increases by {[supportable_area_per_level]} for each level gained."
                // "Total Area"
                locDescText = LocalizationManager.GetTranslation("Stat Description/supportable_area")
                    .Replace("{[supportable_area_base]}", "" + GameData.instance.supportableAreaBase)
                    .Replace("{[supportable_area_per_level]}", "" + GameData.instance.supportableAreaPerLevel);

                if (GameData.instance.mapMode == GameData.MapMode.HOMELAND)
                {
                    locDescText += " " + LocalizationManager.GetTranslation("Stat Description/supportable_area_homeland")
                        .Replace("{[SUPPORTABLE_AREA_HOMELAND_FRACTION]}", "" + (int)(GameData.instance.supportableAreaHomelandFraction * 100));
                }
                else if (GameData.instance.mapMode == GameData.MapMode.RAID)
                {
                    locDescText += " " + LocalizationManager.GetTranslation("Stat Description/supportable_area_raid")
                        .Replace("{[SUPPORTABLE_AREA_RAIDLAND_FRACTION]}", "" + (int)(GameData.instance.supportableAreaRaidlandFraction * 100));
                }
                
                DisplayDetails(LocalizationManager.GetTranslation("Stat Description/supportable_area_title"), locDescText, LocalizationManager.GetTranslation("Stat Description/total_area_title"), GameData.instance.GetStatValueString(GameData.Stat.TOTAL_AREA), LocalizationManager.GetTranslation("Stat Description/supportable_area_title"), GameData.instance.GetStatValueString(GameData.Stat.SUPPORTABLE_AREA), LocalizationManager.GetTranslation("Stat Description/excess_area_title"), string.Format("{0:n0}", Mathf.Max(0, GameData.instance.current_footprint.area - GameData.instance.GetSupportableArea())), "", "", "", "", "", "");
                break;
            case GameData.Stat.INTERIOR_AREA:
                // "The number of interior land squares occupied by our nation, that is, land surrounded only by other squares belonging to our nation. Greater interior area is better because it leads to increased geographic efficiency, which improves our energy generation rate."
                // "Interior Area"
                DisplayDetails(LocalizationManager.GetTranslation("Stat Description/interior_area_title"), LocalizationManager.GetTranslation("Stat Description/interior_area"), LocalizationManager.GetTranslation("Stat Description/interior_area_title"), GameData.instance.GetStatValueString(GameData.Stat.INTERIOR_AREA), "", "", "", "", "", "", "", "", "", "");
                break;
            case GameData.Stat.PERIMETER:
                // "The length of our nation's border. An isolated square has a border length of 4, while a 5x5 block has a border length of 20. We should clean up stray squares and keep our holdings to a few large contiguous areas as much as possible, to minimize border length and maximize interior area. Doing this will increase our geographic efficiency."
                // "Border Length"
                DisplayDetails(LocalizationManager.GetTranslation("Stat Description/border_length_title"), LocalizationManager.GetTranslation("Stat Description/border_length"), LocalizationManager.GetTranslation("Stat Description/border_length_title"), GameData.instance.GetStatValueString(GameData.Stat.PERIMETER), "", "", "", "", "", "", "", "", "", "");
                break;
            case GameData.Stat.GEOGRAPHIC_EFFICIENCY:
                // "A measure of how efficiently energy and materials can be transported throughout our nation. It is the ratio of our nation's interior area to its border area, and will not drop below 50% or exceed 200%. Geographic efficiency modifies our energy generation rate, and so can have a great impact on how much energy we produce."
                // "Geographic Efficiency"
                locDescText = LocalizationManager.GetTranslation("Stat Description/geographic_efficiency")
                    .Replace("{[GEOGRAPHIC_EFFICIENCY_MIN]}", "" + (int)(GameData.instance.geographicEfficiencyMin * 100))
                    .Replace("{[GEOGRAPHIC_EFFICIENCY_MAX]}", "" + (int)(GameData.instance.geographicEfficiencyMax * 100));

                DisplayDetails(LocalizationManager.GetTranslation("Stat Description/geographic_efficiency_title"), locDescText, i2_base, "" + String.Format("{0:0.##}", Math.Min(1f, GameData.instance.current_footprint.geo_efficiency_base) * 100f) + "%", i2_advances, SumStatFromSource(TechData.Bonus.GEO_EFFICIENCY, TechData.Duration.PERMANENT), i2_temporary, SumStatFromSource(TechData.Bonus.GEO_EFFICIENCY, TechData.Duration.TEMPORARY), i2_total, GameData.instance.GetStatValueString(GameData.Stat.GEOGRAPHIC_EFFICIENCY), "", "", "", "");
                break;
	        case GameData.Stat.TECH:
                // "This is how advanced our nation is at building machines and technologies using scientific principles. In battle, Tech is especially strong against Bio, but vulnerable to Psi."
                // "Tech"
                locDescText = LocalizationManager.GetTranslation("Stat Description/tech")
                    .Replace("{[RESOURCE_BONUS_CAP]}", ((int)(GameData.instance.resourceBonusCap * 100)) + "%")
                    .Replace("{[RESOURCE_BONUS_CAP_AMOUNT]}", string.Format("{0:n0}", ((int)(GameData.instance.resourceBonusCap * GameData.instance.statTechFromPerms + 0.5f))))
                    .Replace("{[MANPOWER_LOSS]}", GetManpowerLossText(GameData.instance.statTechFromPerms, GameData.instance.statTechFromResources, LocalizationManager.GetTranslation("Stat Description/tech_title")))
                    .Replace("{[GEOGRAPHIC_EFFICIENCY]}", geo_efficiency_text);
                DisplayDetails(LocalizationManager.GetTranslation("Stat Description/tech_title"), locDescText, i2_base, "" + GameData.instance.initStatTech, i2_advances, SumStatFromSource(TechData.Bonus.TECH, TechData.Duration.PERMANENT), (geo_efficiency == 1f) ? i2_resources : (i2_resources_short + " x " + geo_efficiency_text), SumStatFromSource(TechData.Bonus.TECH, TechData.Duration.OBJECT), i2_temporary, SumStatFromSource(TechData.Bonus.TECH_MULT, TechData.Duration.UNDEF), i2_total, GameData.instance.GetStatValueString(GameData.Stat.TECH), "", "");
                break;
	        case GameData.Stat.BIO:
                // "The measure of our nation's ability to manipulate all kinds of living systems. In battle, Bio is especially strong against Psi, but vulnerable to Tech."
                // Bio
                locDescText = LocalizationManager.GetTranslation("Stat Description/bio")
                    .Replace("{[RESOURCE_BONUS_CAP]}", ((int)(GameData.instance.resourceBonusCap * 100)) + "%")
                    .Replace("{[RESOURCE_BONUS_CAP_AMOUNT]}", string.Format("{0:n0}", ((int)(GameData.instance.resourceBonusCap * GameData.instance.statBioFromPerms + 0.5f))))
                    .Replace("{[MANPOWER_LOSS]}", GetManpowerLossText(GameData.instance.statBioFromPerms, GameData.instance.statBioFromResources, LocalizationManager.GetTranslation("Stat Description/bio_title")))
                    .Replace("{[GEOGRAPHIC_EFFICIENCY]}", geo_efficiency_text);
                DisplayDetails(LocalizationManager.GetTranslation("Stat Description/bio_title"), locDescText, i2_base, "" + GameData.instance.initStatBio, i2_advances, SumStatFromSource(TechData.Bonus.BIO, TechData.Duration.PERMANENT), (geo_efficiency == 1f) ? i2_resources : (i2_resources_short + " x " + geo_efficiency_text), SumStatFromSource(TechData.Bonus.BIO, TechData.Duration.OBJECT), i2_temporary, SumStatFromSource(TechData.Bonus.BIO_MULT, TechData.Duration.UNDEF), i2_total, GameData.instance.GetStatValueString(GameData.Stat.BIO), "", "");
                break;
	        case GameData.Stat.PSI:
                // "Our nation's degree of mastery of the esoteric arts, including psychic phenomena, unseen dimensions of reality, and manifestations of magic. In battle, Psi is especially strong against Tech, but vulnerable to Bio."
                // Psi
                locDescText = LocalizationManager.GetTranslation("Stat Description/psi")
                    .Replace("{[RESOURCE_BONUS_CAP]}", ((int)(GameData.instance.resourceBonusCap * 100)) + "%")
                    .Replace("{[RESOURCE_BONUS_CAP_AMOUNT]}", string.Format("{0:n0}", ((int)(GameData.instance.resourceBonusCap * GameData.instance.statPsiFromPerms + 0.5f))))
                    .Replace("{[MANPOWER_LOSS]}", GetManpowerLossText(GameData.instance.statPsiFromPerms, GameData.instance.statPsiFromResources, LocalizationManager.GetTranslation("Stat Description/psi_title")))
                    .Replace("{[GEOGRAPHIC_EFFICIENCY]}", geo_efficiency_text);
                DisplayDetails(LocalizationManager.GetTranslation("Stat Description/psi_title"), locDescText, i2_base, "" + GameData.instance.initStatPsi, i2_advances, SumStatFromSource(TechData.Bonus.PSI, TechData.Duration.PERMANENT), (geo_efficiency == 1f) ? i2_resources : (i2_resources_short + " x " + geo_efficiency_text), SumStatFromSource(TechData.Bonus.PSI, TechData.Duration.OBJECT), i2_temporary, SumStatFromSource(TechData.Bonus.PSI_MULT, TechData.Duration.UNDEF), i2_total, GameData.instance.GetStatValueString(GameData.Stat.PSI), "", "");
                break;
   	        case GameData.Stat.XP_MULTIPLIER:
                // "A measure of how quickly we gain knowledge from our experiences. By developing advances that help us to learn more effectively, we can gain experience points more rapidly."
                // "Experience Multiplier"
                locDescText = LocalizationManager.GetTranslation("Stat Description/experience_multiplier")
                    .Replace("{[RESOURCE_BONUS_CAP]}", ((int)(GameData.instance.resourceBonusCap * 100)) + "%")
                    .Replace("{[RESOURCE_BONUS_CAP_AMOUNT]}", string.Format("{0:n0}", ((int)(GameData.instance.resourceBonusCap * 100 * GameData.instance.xpMultiplierFromPerms + 0.5f))))
                    .Replace("{[MANPOWER_LOSS]}", GetManpowerLossText(GameData.instance.xpMultiplierFromPerms, GameData.instance.xpMultiplierFromResources, LocalizationManager.GetTranslation("Stat Description/experience_multiplier_title")))
                    .Replace("{[GEOGRAPHIC_EFFICIENCY]}", geo_efficiency_text);
                DisplayDetails(LocalizationManager.GetTranslation("Stat Description/experience_multiplier_title"), locDescText, i2_base, "100%", i2_advances, SumStatFromSource(TechData.Bonus.XP_MULTIPLIER, TechData.Duration.PERMANENT), (geo_efficiency == 1f) ? i2_resources : (i2_resources_short + " x " + geo_efficiency_text), SumStatFromSource(TechData.Bonus.XP_MULTIPLIER, TechData.Duration.OBJECT), i2_temporary, SumStatFromSource(TechData.Bonus.XP_MULTIPLIER, TechData.Duration.TEMPORARY), i2_total, GameData.instance.GetStatValueString(GameData.Stat.XP_MULTIPLIER), "", "");
                break;
	        case GameData.Stat.MANPOWER_MAX:
                // "The maximum amount of manpower we can have stored. Manpower is the stock of trained fighters that we have ready to send into combat. Each attack we make costs some manpower, so the more we have the more we can attack."
                // "Manpower Max"
                locDescText = LocalizationManager.GetTranslation("Stat Description/manpower_max");
                totalText = i2_total;

                if (GameData.instance.mapMode == GameData.MapMode.HOMELAND)
                {
                    locDescText += " " + LocalizationManager.GetTranslation("Stat Description/manpower_max_homeland")
                        .Replace("{[MANPOWER_MAX_HOMELAND_FRACTION]}", "" + (int)(GameData.instance.manpowerMaxHomelandFraction / GameData.instance.manpowerGenMultiplier * 100f + 0.5f));
                    totalText = i2_total + " x " + ((int)(GameData.instance.manpowerMaxHomelandFraction / GameData.instance.manpowerGenMultiplier * 100f + 0.5f)) + "%";
                }
                else if (GameData.instance.mapMode == GameData.MapMode.RAID)
                {
                    locDescText += " " + LocalizationManager.GetTranslation("Stat Description/manpower_max_raid")
                        .Replace("{[MANPOWER_MAX_HOMELAND_FRACTION]}", "" + (int)(GameData.instance.manpowerMaxHomelandFraction / GameData.instance.manpowerGenMultiplier * 100f + 0.5f));
                    totalText = i2_total + " x " + ((int)(GameData.instance.manpowerMaxHomelandFraction / GameData.instance.manpowerGenMultiplier * 100f + 0.5f)) + "%";
                }

                DisplayDetails(LocalizationManager.GetTranslation("Stat Description/manpower_max_title"), locDescText, i2_base, string.Format("{0:n0}", GameData.instance.initManpowerMax * GameData.instance.manpowerGenMultiplier), i2_advances, SumStatFromSource(TechData.Bonus.MANPOWER_MAX, TechData.Duration.PERMANENT), i2_temporary, SumStatFromSource(TechData.Bonus.MANPOWER_MAX_MULT, TechData.Duration.UNDEF), totalText, GameData.instance.GetStatValueString(GameData.Stat.MANPOWER_MAX), "", "", "", "");
                break;
            case GameData.Stat.MANPOWER_RATE:
                // "The rate our manpower increases every hour. Manpower is the stock of trained fighters that we have ready to send into combat. Each attack we make costs some manpower, so the faster our manpower builds up the more we can attack."
                // "Manpower Generation Rate"
                locDescText = LocalizationManager.GetTranslation("Stat Description/manpower_rate")
                    .Replace("{[RESOURCE_BONUS_CAP]}", ((int)(GameData.instance.resourceBonusCap * 100)) + "%")
                    .Replace("{[RESOURCE_BONUS_CAP_AMOUNT]}", string.Format("{0:n0}", ((int)(GameData.instance.resourceBonusCap * GameData.instance.manpowerRateFromPerms + 0.5f))))
                    .Replace("{[MANPOWER_LOSS]}", (GameData.instance.DetermineStatManpowerPenalty(GameData.instance.manpowerRateFromPerms, GameData.instance.manpowerRateFromResources) > 0) ? GetManpowerLossText(GameData.instance.manpowerRateFromPerms, GameData.instance.manpowerRateFromResources, LocalizationManager.GetTranslation("Stat Description/manpower_rate_title")) : ((GameData.instance.manpowerBurnRate > 0) ? ("<color=#ffff00ff>" + LocalizationManager.GetTranslation("Stat Description/manpower_loss_general").Replace("{[MANPOWER_LOSS_AMOUNT]}", string.Format("{0:n0}", GameData.instance.manpowerBurnRate)) + "</color>") : ""))
                    .Replace("{[GEOGRAPHIC_EFFICIENCY]}", geo_efficiency_text);
                totalText = i2_total;

                if (GameData.instance.mapMode == GameData.MapMode.HOMELAND)
                {
                    locDescText += " " + LocalizationManager.GetTranslation("Stat Description/manpower_rate_homeland")
                        .Replace("{[MANPOWER_RATE_HOMELAND_FRACTION]}", "" + (int)(GameData.instance.manpowerRateHomelandFraction / GameData.instance.manpowerGenMultiplier * 100f + 0.5f));
                    totalText = i2_total + " x " + ((int)(GameData.instance.manpowerRateHomelandFraction / GameData.instance.manpowerGenMultiplier * 100f + 0.5f)) + "%";
                }
                else if (GameData.instance.mapMode == GameData.MapMode.RAID)
                {
                    locDescText += " " + LocalizationManager.GetTranslation("Stat Description/manpower_rate_raid")
                        .Replace("{[MANPOWER_RATE_RAIDLAND_FRACTION]}", "" + 0);
                    totalText = i2_total + " x " + 0 + "%";
                }

                if (GameData.instance.manpowerBurnRate > 0) {
                    DisplayDetails(LocalizationManager.GetTranslation("Stat Description/manpower_rate_title"), locDescText, i2_base, string.Format("{0:n0}", GameData.instance.initManpowerRate * GameData.instance.manpowerGenMultiplier), i2_advances, SumStatFromSource(TechData.Bonus.MANPOWER_RATE, TechData.Duration.PERMANENT), (geo_efficiency == 1f) ? i2_resources : (i2_resources_short + " x " + geo_efficiency_text), SumStatFromSource(TechData.Bonus.MANPOWER_RATE, TechData.Duration.OBJECT), i2_temporary, SumStatFromSource(TechData.Bonus.MANPOWER_RATE_MULT, TechData.Duration.UNDEF), "<color=#ffff00ff>" + i2_excess_resources_short + "</color>", "-" + string.Format("{0:n0}", GameData.instance.manpowerBurnRate), totalText, GameData.instance.GetStatValueString(GameData.Stat.MANPOWER_RATE_MINUS_BURN));
                } else {
                    DisplayDetails(LocalizationManager.GetTranslation("Stat Description/manpower_rate_title"), locDescText, i2_base, string.Format("{0:n0}", GameData.instance.initManpowerRate * GameData.instance.manpowerGenMultiplier), i2_advances, SumStatFromSource(TechData.Bonus.MANPOWER_RATE, TechData.Duration.PERMANENT), (geo_efficiency == 1f) ? i2_resources : (i2_resources_short + " x " + geo_efficiency_text), SumStatFromSource(TechData.Bonus.MANPOWER_RATE, TechData.Duration.OBJECT), i2_temporary, SumStatFromSource(TechData.Bonus.MANPOWER_RATE_MULT, TechData.Duration.UNDEF), totalText, GameData.instance.GetStatValueString(GameData.Stat.MANPOWER_RATE_MINUS_BURN), "", "");
                }
                break;
            case GameData.Stat.ATTACK_MANPOWER:
                // "The amount of manpower that we spend on each attack. The more manpower we use, the more powerful the attack, but the faster we deplete our manpower. If we win an attack, then we don't use up all the manpower we put into it."
                // "Manpower per Attack"
                DisplayDetails(LocalizationManager.GetTranslation("Stat Description/attack_manpower_title"), LocalizationManager.GetTranslation("Stat Description/attack_manpower"), i2_base, "" + GameData.instance.initManpowerPerAttack, i2_advances, SumStatFromSource(TechData.Bonus.ATTACK_MANPOWER, TechData.Duration.PERMANENT), i2_temporary, SumStatFromSource(TechData.Bonus.ATTACK_MANPOWER_MULT, TechData.Duration.UNDEF), i2_total, GameData.instance.GetStatValueString(GameData.Stat.ATTACK_MANPOWER), "", "", "", "");
                break;
	        case GameData.Stat.CRIT_CHANCE:
                // "The chance for our attack to successfully hit a critical enemy target, resulting in much more damage than a normal attack would."
                // "Critical Hit Chance"
                DisplayDetails(LocalizationManager.GetTranslation("Stat Description/crit_chance_title"), LocalizationManager.GetTranslation("Stat Description/crit_chance"), i2_advances, SumStatFromSource(TechData.Bonus.CRIT_CHANCE, TechData.Duration.PERMANENT), i2_temporary, SumStatFromSource(TechData.Bonus.CRIT_CHANCE, TechData.Duration.TEMPORARY), i2_total, GameData.instance.GetStatValueString(GameData.Stat.CRIT_CHANCE), "", "", "", "", "", "");
                break;
	        case GameData.Stat.SIMULTANEOUS_ACTIONS:
                // "The maximum number of squares that each member of our nation can be attacking or evacuating at one time."
                // "Max Simultaneous Attacks"
                DisplayDetails(LocalizationManager.GetTranslation("Stat Description/simultaneous_actions_title"), LocalizationManager.GetTranslation("Stat Description/simultaneous_actions"), i2_base, "" + GameData.instance.initMaxSimultaneousProcesses, i2_advances, SumStatFromSource(TechData.Bonus.SIMULTANEOUS_ACTIONS, TechData.Duration.PERMANENT), i2_temporary, SumStatFromSource(TechData.Bonus.SIMULTANEOUS_ACTIONS, TechData.Duration.TEMPORARY), i2_total, GameData.instance.GetStatValueString(GameData.Stat.SIMULTANEOUS_ACTIONS), "", "", "", "");
                break;
	        case GameData.Stat.ENERGY_MAX:
                // "The total amount of energy we can have stored. Energy is used to build and maintain walls and towers. If we run out of energy these structures will start falling into disrepair, becoming useless. Energy is shared among allies."
                // "Energy Max"
                DisplayDetails(LocalizationManager.GetTranslation("Stat Description/energy_max_title"), LocalizationManager.GetTranslation("Stat Description/energy_max"), i2_base, string.Format("{0:n0}", GameData.instance.initEnergyMax), i2_advances, SumStatFromSource(TechData.Bonus.ENERGY_MAX, TechData.Duration.PERMANENT), i2_temporary, SumStatFromSource(TechData.Bonus.ENERGY_MAX_MULT, TechData.Duration.UNDEF), i2_total, GameData.instance.GetStatValueString(GameData.Stat.ENERGY_MAX), i2_current, GameData.instance.GetStatValueString(GameData.Stat.ENERGY), "", "");
                break;
            case GameData.Stat.ENERGY_RATE:
                // "The rate our energy increases every hour. Energy is used to build and maintain walls and towers, so we need to keep its generation rate higher than its burn rate, or we will run out of energy and our walls and towers will begin to fail. Our geographic efficiency (currently {[GEOGRAPHIC_EFFICIENCY]}) factors into our final energy rate, reflecting our ability to transport energy and materials throughout our nation's land area. Energy is shared among allies."
                locDescText = LocalizationManager.GetTranslation("Stat Description/energy_rate")
                    .Replace("{[ENERGY_BURN_RATE]}", string.Format("{0:n0}", (GameData.instance.GetFinalEnergyBurnRate() + 0.5f)))
                    .Replace("{[RESOURCE_BONUS_CAP]}", ((int)(GameData.instance.resourceBonusCap * 100)) + "%")
                    .Replace("{[RESOURCE_BONUS_CAP_AMOUNT]}", string.Format("{0:n0}", ((int)(GameData.instance.resourceBonusCap * GameData.instance.energyRateFromPerms + 0.5f))))
                    .Replace("{[MANPOWER_LOSS]}", GetManpowerLossText(GameData.instance.energyRateFromPerms, GameData.instance.energyRateFromResources, LocalizationManager.GetTranslation("Stat Description/energy_rate_title")))
                    .Replace("{[GEOGRAPHIC_EFFICIENCY]}", geo_efficiency_text);
                totalText = i2_total;

                if (GameData.instance.mapMode == GameData.MapMode.HOMELAND)
                {
                    locDescText += " " + LocalizationManager.GetTranslation("Stat Description/energy_rate_homeland")
                        .Replace("{[ENERGY_RATE_HOMELAND_FRACTION]}", "" + (int)(GameData.instance.energyRateHomelandFraction * 100f + 0.5f));
                    totalText = i2_total + " x " + ((int)(GameData.instance.energyRateHomelandFraction * 100f + 0.5f)) + "%";
                }
                else if (GameData.instance.mapMode == GameData.MapMode.RAID)
                {
                    locDescText += " " + LocalizationManager.GetTranslation("Stat Description/energy_rate_raid")
                        .Replace("{[ENERGY_RATE_RAIDLAND_FRACTION]}", "" + (int)(GameData.instance.energyRateRaidlandFraction * 100f + 0.5f));
                    totalText = i2_total + " x " + ((int)(GameData.instance.energyRateRaidlandFraction * 100f + 0.5f)) + "%";
                }

                // "Energy Generation Rate"
                DisplayDetails(LocalizationManager.GetTranslation("Stat Description/energy_rate_title"), locDescText, i2_base, string.Format("{0:n0}", GameData.instance.initEnergyRate), i2_advances, SumStatFromSource(TechData.Bonus.ENERGY_RATE, TechData.Duration.PERMANENT), (geo_efficiency == 1f) ? i2_resources : (i2_resources_short + " x " + geo_efficiency_text), SumStatFromSource(TechData.Bonus.ENERGY_RATE, TechData.Duration.OBJECT), i2_temporary, SumStatFromSource(TechData.Bonus.ENERGY_RATE_MULT, TechData.Duration.UNDEF), totalText, GameData.instance.GetStatValueString(GameData.Stat.ENERGY_RATE), "", "");
                break;
            case GameData.Stat.ENERGY_BURN_RATE:
                // "The rate we spend our energy every hour, to maintain our defenses. The energy burn rate should be kept below the energy generation rate, or we'll be in danger of running out of energy. If that happens our defenses will start to fail. If our energy burn rate exceeds our maximum energy generation rate ({[FULL_ENERGY_RATE]}) we'll go into overburn and use up our stored energy much more quickly. Energy is shared among allies."
                locDescText = LocalizationManager.GetTranslation("Stat Description/energy_burn_rate")
                    .Replace("{[FULL_ENERGY_RATE]}", string.Format("{0:n0}", GameData.instance.GetFinalEnergyRate()));

                float incognito_penalty = GameData.instance.GetIncognitoEnergyPenalty();
                int overburn_penalty = (int)(GameData.instance.GetFinalEnergyBurnRate() - GameData.instance.current_footprint.energy_burn_rate - incognito_penalty);

                // "Burn Rate to Maintain Walls and Towers"
                // "Energy Burn Rate"
                if (incognito_penalty > 0f) {
                    DisplayDetails(LocalizationManager.GetTranslation("Stat Description/energy_burn_rate_title"), locDescText, LocalizationManager.GetTranslation("Stat Description/energy_burn_rate_for_walls_towers"), GameData.instance.GetStatValueString(GameData.Stat.ENERGY_BURN_RATE), LocalizationManager.GetTranslation("incognito_nation"), string.Format("{0:n0}", (int)(GameData.instance.GetFinalEnergyRate() * GameData.instance.incognitoEnergyBurn)), LocalizationManager.GetTranslation("Stat Description/overburn"), string.Format("{0:n0}", overburn_penalty), i2_total, GameData.instance.GetStatValueString(GameData.Stat.FINAL_ENERGY_BURN_RATE), "", "", "", "");
                } else {
                    DisplayDetails(LocalizationManager.GetTranslation("Stat Description/energy_burn_rate_title"), locDescText, LocalizationManager.GetTranslation("Stat Description/energy_burn_rate_for_walls_towers"), GameData.instance.GetStatValueString(GameData.Stat.ENERGY_BURN_RATE), LocalizationManager.GetTranslation("Stat Description/overburn"), string.Format("{0:n0}", overburn_penalty), i2_total, GameData.instance.GetStatValueString(GameData.Stat.FINAL_ENERGY_BURN_RATE), "", "", "", "", "", "");
                }
                break;
            case GameData.Stat.HP_PER_SQUARE:
                // "The number of defensive hit points that each empty square of our land has. Squares with walls and towers will have additional hit points. The more hit points a square has, the more manpower our enemies will need to expend to win an attack."
                // "Hit Points per Square"
                DisplayDetails(LocalizationManager.GetTranslation("Stat Description/hp_per_square_title"), LocalizationManager.GetTranslation("Stat Description/hp_per_square"), i2_base, "" + GameData.instance.initHitPointBase, i2_advances, SumStatFromSource(TechData.Bonus.HP_PER_SQUARE, TechData.Duration.PERMANENT), i2_temporary, SumStatFromSource(TechData.Bonus.HP_PER_SQUARE_MULT, TechData.Duration.UNDEF), i2_total, GameData.instance.GetStatValueString(GameData.Stat.HP_PER_SQUARE), "", "", "", "");
                break;
	        case GameData.Stat.HP_RESTORE:
                // "The number of defensive hit points that our land squares can regenerate per minute, after they've been attacked. The faster our hit points are regenerated, the more resilient we are to attack."
                // "Hit Point Restore Rate"
                DisplayDetails(LocalizationManager.GetTranslation("Stat Description/hp_restore_title"), LocalizationManager.GetTranslation("Stat Description/hp_restore"), i2_base, "" + GameData.instance.initHitPointsRate, i2_advances, SumStatFromSource(TechData.Bonus.HP_RESTORE, TechData.Duration.PERMANENT), i2_temporary, SumStatFromSource(TechData.Bonus.HP_RESTORE_MULT, TechData.Duration.UNDEF), i2_total, GameData.instance.GetStatValueString(GameData.Stat.HP_RESTORE), "", "", "", "");
                break;
   	        case GameData.Stat.SPLASH_DAMAGE:
                // "If we have splash damage, then when we attack an enemy square, surrounding squares belonging to that same enemy will also receive a fraction of the damage that we inflict."
                // "Splash Damage"
                DisplayDetails(LocalizationManager.GetTranslation("Stat Description/splash_damage_title"), LocalizationManager.GetTranslation("Stat Description/splash_damage"), "", "", i2_advances, SumStatFromSource(TechData.Bonus.SPLASH_DAMAGE, TechData.Duration.PERMANENT), i2_temporary, SumStatFromSource(TechData.Bonus.SPLASH_DAMAGE, TechData.Duration.TEMPORARY), i2_total, GameData.instance.GetStatValueString(GameData.Stat.SPLASH_DAMAGE), "", "", "", "");
                break;
            case GameData.Stat.SALVAGE_VALUE:
                // "When we tear down a wall or tower, we gain back some fraction of its initial cost in energy. The salvage value is the amount of the initial energy cost that we recoup."
                // "Salvage Value"
                DisplayDetails(LocalizationManager.GetTranslation("Stat Description/salvage_value_title"), LocalizationManager.GetTranslation("Stat Description/salvage_value"), i2_base, "" + (int)(GameData.instance.initSalvageValue * 100f) + "%", i2_advances, SumStatFromSource(TechData.Bonus.SALVAGE_VALUE, TechData.Duration.PERMANENT), i2_temporary, SumStatFromSource(TechData.Bonus.SALVAGE_VALUE, TechData.Duration.TEMPORARY), i2_total, GameData.instance.GetStatValueString(GameData.Stat.SALVAGE_VALUE), "", "", "", "");
                break;
	        case GameData.Stat.WALL_DISCOUNT:
                // "By developing certain advances, we are able to decrease the cost in energy of building and maintaining walls. The greater the discount, the less energy we spend per wall, and so the more we can support."
                // "Wall Discount"
                DisplayDetails(LocalizationManager.GetTranslation("Stat Description/wall_discount_title"), LocalizationManager.GetTranslation("Stat Description/wall_discount"), "", "", i2_advances, SumStatFromSource(TechData.Bonus.WALL_DISCOUNT, TechData.Duration.PERMANENT), i2_temporary, SumStatFromSource(TechData.Bonus.WALL_DISCOUNT, TechData.Duration.TEMPORARY), i2_total, GameData.instance.GetStatValueString(GameData.Stat.WALL_DISCOUNT), "", "", "", "");
                break;
	        case GameData.Stat.STRUCTURE_DISCOUNT:
                // "By developing certain advances, we are able to decrease the cost in energy of building and maintaining defensive towers. The greater the discount, the less energy we spend per tower, and so the more we can support."
                // "Tower Discount"
                DisplayDetails(LocalizationManager.GetTranslation("Stat Description/structure_discount_title"), LocalizationManager.GetTranslation("Stat Description/structure_discount"), "", "", i2_advances, SumStatFromSource(TechData.Bonus.STRUCTURE_DISCOUNT, TechData.Duration.PERMANENT), i2_temporary, SumStatFromSource(TechData.Bonus.STRUCTURE_DISCOUNT, TechData.Duration.TEMPORARY), i2_total, GameData.instance.GetStatValueString(GameData.Stat.STRUCTURE_DISCOUNT), "", "", "", "");
                break;
            case GameData.Stat.INSURGENCY:
                // "Doubles base hit points for interior areas."
                // "Insurgency"
                DisplayDetails(LocalizationManager.GetTranslation("Stat Description/insurgency_title"), LocalizationManager.GetTranslation("Stat Description/insurgency"), LocalizationManager.GetTranslation("Stat Description/insurgency_title"), GameData.instance.GetStatValueString(GameData.Stat.INSURGENCY), "", "", "", "", "", "", "", "", "", "");
                break;
	        case GameData.Stat.INVISIBILITY:
                // "The ability to render our walls and towers invisible to our enemies, so that they attack us unaware of our defensive capabilities."
                // "Invisibility"
                DisplayDetails(LocalizationManager.GetTranslation("Stat Description/invisibility_title"), LocalizationManager.GetTranslation("Stat Description/invisibility"), LocalizationManager.GetTranslation("Stat Description/invisibility_title"), GameData.instance.GetStatValueString(GameData.Stat.INVISIBILITY), "", "", "", "", "", "", "", "", "", "");
                break;
            case GameData.Stat.MAX_ALLIANCES:
                // "How many nations we can be allied with at one time. Energy and manpower are shared among allied nations."
                // "Max Number of Alliances"
                DisplayDetails(LocalizationManager.GetTranslation("Stat Description/max_alliances_title"), LocalizationManager.GetTranslation("Stat Description/max_alliances"), LocalizationManager.GetTranslation("Stat Description/max_alliances_title"), GameData.instance.GetStatValueString(GameData.Stat.MAX_ALLIANCES), "", "", "", "", "", "", "", "", "", "");
                break;
            case GameData.Stat.NUM_STORAGE_STRUCTURES:
                // "Once we have developed the necessary advances we can build structures to store extra manpower and energy, to be donated to our allies when they run out. We can have a maximum of {[max_num_storage_structures]} storage structures at one time."
                // "Storage Structure Built"
                locDescText = LocalizationManager.GetTranslation("Stat Description/num_storage_structures")
                    .Replace("{[max_num_storage_structures]}", "" + GameData.instance.maxNumStorageStructures);
                DisplayDetails(LocalizationManager.GetTranslation("Stat Description/num_storage_structures_title"), locDescText, LocalizationManager.GetTranslation("Stat Description/num_storage_structures_title"), GameData.instance.GetStatValueString(GameData.Stat.NUM_STORAGE_STRUCTURES), LocalizationManager.GetTranslation("Generic Text/maximum_word"), "" + GameData.instance.maxNumStorageStructures, "", "", "", "", "", "", "", "");
                break;
            case GameData.Stat.MANPOWER_STORED:
                // "The amount of extra manpower that we've built up in storage structures, to donate to our allies when they run out."
                // "Manpower Stored for Allies"
                DisplayDetails(LocalizationManager.GetTranslation("Stat Description/manpower_stored_title"), LocalizationManager.GetTranslation("Stat Description/manpower_stored"), LocalizationManager.GetTranslation("Stat Description/manpower_stored_title"), GameData.instance.GetStatValueString(GameData.Stat.MANPOWER_STORED), "", "", "", "", "", "", "", "", "", "");
                break;
            case GameData.Stat.ENERGY_STORED:
                // "The amount of extra energy that we've built up in storage structures, to donate to our allies when they run out."
                // "Energy Stored for Allies"
                DisplayDetails(LocalizationManager.GetTranslation("Stat Description/energy_stored_title"), LocalizationManager.GetTranslation("Stat Description/energy_stored"), LocalizationManager.GetTranslation("Stat Description/energy_stored_title"), GameData.instance.GetStatValueString(GameData.Stat.ENERGY_STORED), "", "", "", "", "", "", "", "", "", "");
                break;
            case GameData.Stat.MANPOWER_AVAILABLE:
                // "The amount of extra manpower that our allies have built up in their storage structures, that will be available to us if we run out."
                // "Manpower Available from Allies"
                DisplayDetails(LocalizationManager.GetTranslation("Stat Description/manpower_available_title"), LocalizationManager.GetTranslation("Stat Description/manpower_available"), LocalizationManager.GetTranslation("Stat Description/manpower_available_title"), GameData.instance.GetStatValueString(GameData.Stat.MANPOWER_AVAILABLE), "", "", "", "", "", "", "", "", "", "");
                break;
            case GameData.Stat.ENERGY_AVAILABLE:
                // "The amount of extra energy that our allies have built up in their storage structures, that will be available to us if we run out."
                // "Energy Available from Allies"
                DisplayDetails(LocalizationManager.GetTranslation("Stat Description/energy_available_title"), LocalizationManager.GetTranslation("Stat Description/energy_available"), LocalizationManager.GetTranslation("Stat Description/energy_available_title"), GameData.instance.GetStatValueString(GameData.Stat.ENERGY_AVAILABLE), "", "", "", "", "", "", "", "", "", "");
                break;
            case GameData.Stat.REBIRTH_LEVEL_BONUS:
                // "The number of free bonus levels that our nation accumulates, growing by +{[level_bonus_per_rebirth]} each time we rebirth. The rebirth level bonus allows us to reach higher levels and grow to be a more powerful nation with each rebirth. Maximum is +{[max_rebirth_level_bonus]}."
                // "Rebirth Level Bonus"
                locDescText = LocalizationManager.GetTranslation("Stat Description/rebirth_level_bonus")
                    .Replace("{[level_bonus_per_rebirth]}", "" + GameData.instance.levelBonusPerRebirth)
                    .Replace("{[max_rebirth_level_bonus]}", "" + GameData.instance.maxRebirthLevelBonus);
                DisplayDetails(LocalizationManager.GetTranslation("Stat Description/rebirth_level_bonus_title"), locDescText, LocalizationManager.GetTranslation("Stat Description/rebirth_level_bonus_title"), GameData.instance.GetStatValueString(GameData.Stat.REBIRTH_LEVEL_BONUS), "", "", "", "", "", "", "", "", "", "");
                break;
            case GameData.Stat.STORAGE_XP_RATE:
                // "The XP generated each hour by our manpower or energy storage structures. A storage structure will generate XP at its maximum rate when it is full. Otherwise it will generate XP at a lower rate proportional to how close it is to full. A storage structure is emptied if captured by another nation, or its contents may be used by our allies. It refills over the course of {[refill_hours]} hours."
                // "XP Generated by Storage Structures"
                locDescText = LocalizationManager.GetTranslation("Stat Description/storage_xp_rate")
                    .Replace("{[refill_hours]}", string.Format("{0}", GameData.instance.storageRefillHours));
                DisplayDetails(LocalizationManager.GetTranslation("Stat Description/storage_xp_rate_title"), locDescText, LocalizationManager.GetTranslation("Stat Description/maximum_rate"), string.Format("{0:n0}", GameData.instance.sharedEnergyXPPerHour + GameData.instance.sharedManpowerXPPerHour), LocalizationManager.GetTranslation("Stat Description/current_rate"), GameData.instance.GetStatValueString(GameData.Stat.STORAGE_XP_RATE), "", "", "", "", "", "", "", "");
                break;
            case GameData.Stat.TOTAL_DEFENSE:
                // "Increased base hit points while offline for over two hours. Expires at level 110."
                // "Total Defense"
                DisplayDetails(LocalizationManager.GetTranslation("Stat Description/total_defense_title"), LocalizationManager.GetTranslation("Stat Description/total_defense"), LocalizationManager.GetTranslation("Stat Description/total_defense_title"), GameData.instance.GetStatValueString(GameData.Stat.TOTAL_DEFENSE), "", "", "", "", "", "", "", "", "", "");
                break;
        }
    }

    public string GetManpowerLossText(float _valueFromPerms, float _valueFromResources, string _statName)
    {
        int manpower_burn = (int)(GameData.instance.DetermineStatManpowerPenalty(_valueFromPerms, _valueFromResources) + 0.5f);
        if (manpower_burn > 0)
        {
            return "<color=#ffff00ff>" + LocalizationManager.GetTranslation("Stat Description/manpower_loss")
                .Replace("{[MANPOWER_LOSS_AMOUNT]}", string.Format("{0:n0}", manpower_burn))
                .Replace("{[STAT]}", _statName)
                + "</color>";
        }
        else
        {
            return "";
        }
    }
  
    public void DisplayDetails(string _title, string _description, string _col1_title, string _col1_value, string _col2_title, string _col2_value, string _col3_title, string _col3_value, string _col4_title, string _col4_value, string _col5_title, string _col5_value, string _col6_title, string _col6_value)
    {
        title_text.text = _title;
        description_text.text = _description;

        // Set row/col activation.
        r1c1_object.SetActive(_col1_title.Equals("") == false);
        r2c1_object.SetActive(_col1_title.Equals("") == false);
        r1c2_object.SetActive(_col2_title.Equals("") == false);
        r2c2_object.SetActive(_col2_title.Equals("") == false);
        r1c3_object.SetActive(_col3_title.Equals("") == false);
        r2c3_object.SetActive(_col3_title.Equals("") == false);
        r1c4_object.SetActive(_col4_title.Equals("") == false);
        r2c4_object.SetActive(_col4_title.Equals("") == false);
        r1c5_object.SetActive(_col5_title.Equals("") == false);
        r2c5_object.SetActive(_col5_title.Equals("") == false);
        r1c6_object.SetActive(_col6_title.Equals("") == false);
        r2c6_object.SetActive(_col6_title.Equals("") == false);

        // Set row/col text
        r1c1_text.text = _col1_title;
        r2c1_text.text = _col1_value;
        r1c2_text.text = _col2_title;
        r2c2_text.text = _col2_value;
        r1c3_text.text = _col3_title;
        r2c3_text.text = _col3_value;
        r1c4_text.text = _col4_title;
        r2c4_text.text = _col4_value;
        r1c5_text.text = _col5_title;
        r2c5_text.text = _col5_value;
        r1c6_text.text = _col6_title;
        r2c6_text.text = _col6_value;
    }

    public string SumStatFromSource(TechData.Bonus _bonusID, TechData.Duration _duration_type)
    {
        if (_duration_type == TechData.Duration.OBJECT)
        {
            float resource_val = 0f;
            bool percentage = false;

            switch (_bonusID)
            {
                case TechData.Bonus.TECH: resource_val = GameData.instance.statTechFromResources; break;
                case TechData.Bonus.BIO: resource_val = GameData.instance.statBioFromResources; break;
	            case TechData.Bonus.PSI: resource_val = GameData.instance.statPsiFromResources; break;
	            case TechData.Bonus.MANPOWER_RATE: resource_val = GameData.instance.manpowerRateFromResources; break;
                case TechData.Bonus.ENERGY_RATE: resource_val = GameData.instance.energyRateFromResources; break;
                case TechData.Bonus.XP_MULTIPLIER: resource_val = GameData.instance.xpMultiplierFromResources * 100f; percentage = true;  break;
            }

            return "+" + (int)(resource_val * GameData.instance.GetFinalGeoEfficiency() + 0.5f) + (percentage ? "%" : "");

            /*
            float perm_val = 0f, resource_val = 0f, max_resource_bonus = 0f;
            bool percentage = false;

            switch (_bonusID)
            {
                case TechData.Bonus.TECH:
                    perm_val = GameData.instance.initStatTech + DetermineStatFromSource(_bonusID, TechData.Duration.PERMANENT);
                    resource_val = GameData.instance.statTechFromResources;
                    break;
                case TechData.Bonus.BIO:
                    perm_val = GameData.instance.initStatBio + DetermineStatFromSource(_bonusID, TechData.Duration.PERMANENT);
                    resource_val = GameData.instance.statBioFromResources;
                    break;
	            case TechData.Bonus.PSI:
                    perm_val = GameData.instance.initStatPsi + DetermineStatFromSource(_bonusID, TechData.Duration.PERMANENT);
                    resource_val = GameData.instance.statPsiFromResources;
                    break;
	            case TechData.Bonus.MANPOWER_RATE:
                    perm_val = (GameData.instance.initManpowerRate * GameData.instance.manpowerGenMultiplier) + DetermineStatFromSource(_bonusID, TechData.Duration.PERMANENT);
                    resource_val = GameData.instance.manpowerRateFromResources;
                    break;
                case TechData.Bonus.ENERGY_RATE:
                    perm_val = GameData.instance.initEnergyRate + DetermineStatFromSource(_bonusID, TechData.Duration.PERMANENT);
                    resource_val = GameData.instance.energyRateFromResources;
                    break;
                case TechData.Bonus.XP_MULTIPLIER:
                    perm_val = 100 + DetermineStatFromSource(_bonusID, TechData.Duration.PERMANENT);
                    resource_val = GameData.instance.xpMultiplierFromResources * 100f;
                    percentage = true;
                    break;
            }

            // Detrmine the max sum resource bonus allowed for this stat.
            max_resource_bonus = perm_val * GameData.instance.resourceBonusCap;

            return ((resource_val >= 0) ? "+" : "") + (int)Mathf.Min(resource_val, max_resource_bonus) + (percentage ? "%" : "") + ((resource_val > max_resource_bonus) ? (" (" + LocalizationManager.GetTranslation("Generic Text/of_word") + " " + (int)resource_val + ")") : "");
            */
        }
        else
        {
            float val = DetermineStatFromSource(_bonusID, _duration_type);

            // GB-Localization
            switch (_bonusID)
            {
                case TechData.Bonus.TECH:
                case TechData.Bonus.BIO:
	            case TechData.Bonus.PSI:
	            case TechData.Bonus.MANPOWER_RATE:
	            case TechData.Bonus.MANPOWER_MAX:
	            case TechData.Bonus.ENERGY_RATE:
	            case TechData.Bonus.ENERGY_MAX:
                case TechData.Bonus.HP_PER_SQUARE:
	            case TechData.Bonus.HP_RESTORE:
	            case TechData.Bonus.ATTACK_MANPOWER:
	            case TechData.Bonus.SIMULTANEOUS_ACTIONS:
                case TechData.Bonus.MAX_ALLIANCES:
                    return ((val >= 0) ? "+" : "") + string.Format("{0:n0}", (int)val);
                case TechData.Bonus.GEO_EFFICIENCY:
	            case TechData.Bonus.XP_MULTIPLIER:
	            case TechData.Bonus.CRIT_CHANCE:
	            case TechData.Bonus.SALVAGE_VALUE:
	            case TechData.Bonus.WALL_DISCOUNT:
	            case TechData.Bonus.STRUCTURE_DISCOUNT:
	            case TechData.Bonus.SPLASH_DAMAGE:
                case TechData.Bonus.TECH_MULT:
                case TechData.Bonus.BIO_MULT:
                case TechData.Bonus.PSI_MULT:
                case TechData.Bonus.MANPOWER_RATE_MULT:
                case TechData.Bonus.MANPOWER_MAX_MULT:
                case TechData.Bonus.ENERGY_RATE_MULT:
                case TechData.Bonus.ENERGY_MAX_MULT:
                case TechData.Bonus.HP_PER_SQUARE_MULT:
                case TechData.Bonus.HP_RESTORE_MULT:
                case TechData.Bonus.ATTACK_MANPOWER_MULT:
                    return ((val >= 0) ? "+" : "") + string.Format("{0:n0}", ((int)val)) + "%";
	            case TechData.Bonus.INVISIBILITY:
                case TechData.Bonus.INSURGENCY:
                case TechData.Bonus.TOTAL_DEFENSE:
                    return (val > 0) ? I2.Loc.LocalizationManager.GetTranslation("Generic Text/yes_word") : I2.Loc.LocalizationManager.GetTranslation("Generic Text/no_word");
            }
        }

        return "SumStatFromSource() error for " + _bonusID + ", " + _duration_type + ".";
    }

    public float DetermineStatFromSource(TechData.Bonus _bonusID, TechData.Duration _duration_type)
    {
        TechData techData;
        float sum = 0f;

        foreach(KeyValuePair<int, int> entry in GameData.instance.techCount)
        {
            techData = TechData.GetTechData(entry.Key);

            // Skip techs that are not of the given duration type.
            if ((_duration_type != TechData.Duration.UNDEF) && (techData.duration_type != _duration_type)) {
                continue;
            }

            // Look for advances that add to the bonus itself

            if (techData.bonus_type_1 == _bonusID) {
                sum += (techData.GetBonusVal(1) * entry.Value);
                //Debug.Log("Tech " + techData.name + " adds to splash " + techData.bonus_val_1 + " x count " + entry.Value);
            }

            if (techData.bonus_type_2 == _bonusID) {
                sum += (techData.GetBonusVal(2) * entry.Value);
                //Debug.Log("Tech " + techData.name + " adds to splash " + techData.bonus_val_2 + " x count " + entry.Value);
            }

            if (techData.bonus_type_3 == _bonusID) {
                sum += (techData.GetBonusVal(3) * entry.Value);
                //Debug.Log("Tech " + techData.name + " adds to splash " + techData.bonus_val_3 + " x count " + entry.Value);
            }
        }

        return sum;
    }
	
    public void OnClick_Close()
    {
        GameGUI.instance.CloseAllPanels();
    }
}
