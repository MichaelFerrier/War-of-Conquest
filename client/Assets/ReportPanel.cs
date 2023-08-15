using UnityEngine;
using UnityEngine.UI;
using System.Collections;
using System.Collections.Generic;
using I2.Loc;

public class ReportPanel : MonoBehaviour
{
    public const int MAX_NUM_REPORT_LINES = 8;

    public static ReportPanel instance;

    public TMPro.TextMeshProUGUI titleText;
    public RectTransform contentArea;

    public GameObject reportLinePrefab;
    public Sprite reportIconDefenses, reportIconWalls, reportIconAttacks, reportIconLevels, reportIconOrb, reportIconResource, reportIconLand, reportIconEnergy, reportIconManpower, reportIconCredits, reportIconFollowers, reportIconMedal;
    private List<GameObject> reportLines = new List<GameObject>();

    public ReportPanel()
    {
        instance = this;
    }

    public void InfoEventReceived()
    {
        string color_tag_open = "<color=yellow>";
        string color_tag_close = "</color>";
        string locString = "";

        // Clear any previous report
        ClearReport();

        /*
        // TESTING
        AddReportLine(reportIconDefenses, "Testing");
        AddReportLine(reportIconWalls, "Testing");
        AddReportLine(reportIconAttacks, "Testing");
        AddReportLine(reportIconLevels, "Testing");
        AddReportLine(reportIconOrb, "Testing");
        AddReportLine(reportIconResource, "Testing");
        AddReportLine(reportIconLand, "Testing");
        AddReportLine(reportIconEnergy, "Testing");
        AddReportLine(reportIconManpower, "Testing");
        AddReportLine(reportIconCredits, "Testing");
        AddReportLine(reportIconFollowers, "Testing");
        */

        if (GameData.instance.report__rebirth != 0) {
            locString = LocalizationManager.GetTranslation("Report Panel/since_rebirth"); // "Our nation has been reborn!"
            AddReportLine(reportIconLevels, locString);
        }

        if (GameData.instance.report__levels_gained > 0) {
            // We have advanced to level {[LEVEL]}!
            locString = LocalizationManager.GetTranslation("Report Panel/since_advanced_to_level").Replace("{[LEVEL]}", color_tag_open + GameData.instance.level + color_tag_close);
            AddReportLine(reportIconLevels, locString);
        }

        if (GameData.instance.report__orb_count_delta == 1) {
            // "We have captured an orb!" 
            AddReportLine(reportIconOrb, LocalizationManager.GetTranslation("Report Panel/since_captured_orb"));
        }

        if (GameData.instance.report__orb_count_delta > 1) {
            // We have captured {[ORB_DELTA]} orbs!
            locString = LocalizationManager.GetTranslation("Report Panel/since_captured_x_orbs")
                .Replace("{[ORB_DELTA]}", color_tag_open + GameData.instance.report__orb_count_delta + color_tag_close);
            AddReportLine(reportIconOrb, locString);
        }

        if (GameData.instance.report__orb_count_delta == -1) {
            // "We have lost an orb"
            AddReportLine(reportIconOrb, LocalizationManager.GetTranslation("Report Panel/since_lost_an_orb"));
        }

        if (GameData.instance.report__orb_count_delta < -1) {
            // We have lost {[ORB_DELTA]} orbs 
            locString = LocalizationManager.GetTranslation("Report Panel/since_lost_x_orbs")
                .Replace("{[ORB_DELTA]}", color_tag_open + (-GameData.instance.report__orb_count_delta) + color_tag_close);
            AddReportLine(reportIconOrb, locString);
        }

        if (GameData.instance.report__raids_fought == 1)
        {
            if (GameData.instance.report__medals_delta >= 0)
            {
                locString = LocalizationManager.GetTranslation("Report Panel/since_one_raid_earned")
                    .Replace("{[MEDALS_DELTA]}", color_tag_open + string.Format("{0:n0}", GameData.instance.report__medals_delta) + color_tag_close);
                AddReportLine(reportIconMedal, locString);
            }
            else
            {
                locString = LocalizationManager.GetTranslation("Report Panel/since_one_raid_lost")
                    .Replace("{[MEDALS_DELTA]}", color_tag_open + string.Format("{0:n0}", -GameData.instance.report__medals_delta) + color_tag_close);
                AddReportLine(reportIconMedal, locString);
            }
        }

        if (GameData.instance.report__raids_fought > 1)
        {
            if (GameData.instance.report__medals_delta >= 0)
            {
                locString = LocalizationManager.GetTranslation("Report Panel/since_multi_raid_earned")
                    .Replace("{[NUM_RAIDS]}", color_tag_open + string.Format("{0:n0}", GameData.instance.report__raids_fought) + color_tag_close)
                    .Replace("{[MEDALS_DELTA]}", color_tag_open + string.Format("{0:n0}", GameData.instance.report__medals_delta) + color_tag_close);
                AddReportLine(reportIconMedal, locString);
            }
            else
            {
                locString = LocalizationManager.GetTranslation("Report Panel/since_multi_raid_lost")
                    .Replace("{[NUM_RAIDS]}", color_tag_open + string.Format("{0:n0}", GameData.instance.report__raids_fought) + color_tag_close)
                    .Replace("{[MEDALS_DELTA]}", color_tag_open + string.Format("{0:n0}", -GameData.instance.report__medals_delta) + color_tag_close);
                AddReportLine(reportIconMedal, locString);
            }
        }

        if (GameData.instance.report__home_defense_credits >= 1)
        {
            if (GameData.instance.report__home_defense_rebirth >= 1)
            {
                locString = LocalizationManager.GetTranslation("Report Panel/defense_rewards_rebirth")
                    .Replace("{[NUM_CREDITS]}", color_tag_open + string.Format("{0:n0}", (int)GameData.instance.report__home_defense_credits) + color_tag_close)
                    .Replace("{[NUM_XP]}", color_tag_open + string.Format("{0:n0}", (int)GameData.instance.report__home_defense_xp) + color_tag_close)
                    .Replace("{[NUM_REBIRTH]}", color_tag_open + string.Format("{0:n0}", (int)GameData.instance.report__home_defense_rebirth) + color_tag_close);
                AddReportLine(reportIconMedal, locString);
            }
            else
            {
                locString = LocalizationManager.GetTranslation("Report Panel/defense_rewards_no_rebirth")
                    .Replace("{[NUM_CREDITS]}", color_tag_open + string.Format("{0:n0}", (int)GameData.instance.report__home_defense_credits) + color_tag_close)
                    .Replace("{[NUM_XP]}", color_tag_open + string.Format("{0:n0}", (int)GameData.instance.report__home_defense_xp) + color_tag_close);
                AddReportLine(reportIconMedal, locString);
            }
        }
        
        if (GameData.instance.report__orb_credits > 1) {
            // "Orbs we've captured have generated {[credits]} and {[xp]} XP!" 
            AddReportLine(reportIconOrb, LocalizationManager.GetTranslation("Report Panel/orbs_generated")
                .Replace("{[credits]}", color_tag_open + "$" + (GameData.instance.report__orb_credits / 100.0).ToString("N2") /*string.Format("{0:n0}", GameData.instance.report__orb_credits)*/ + color_tag_close)
                .Replace("{[xp]}", color_tag_open + string.Format("{0:n0}", GameData.instance.report__orb_XP) + color_tag_close)
                );
        }

        float credits_delta = GameData.instance.credits - (GameData.instance.report__credits_begin - GameData.instance.report__credits_spent);
        if ((credits_delta > 2) && (credits_delta > (GameData.instance.report__follower_credits + GameData.instance.report__patron_credits))) {
            // We have gained {[CREDIT_DELTA]} credits! 
            locString = LocalizationManager.GetTranslation("Report Panel/since_gained_credits")
                .Replace("{[CREDIT_DELTA]}", color_tag_open + string.Format("{0:n0}", (int)(GameData.instance.credits - (GameData.instance.report__credits_begin - GameData.instance.report__credits_spent))) + color_tag_close);
            AddReportLine(reportIconCredits, locString);
        }

        if (GameData.instance.report__farming_XP > 1) {
            // "We have harvested {[xp]} XP by storing energy or manpower!" 
            AddReportLine(reportIconLevels, LocalizationManager.GetTranslation("Report Panel/farming_generated")
                .Replace("{[xp]}", color_tag_open + string.Format("{0:n0}", GameData.instance.report__farming_XP) + color_tag_close)
                );
        }

        if (GameData.instance.report__follower_count > 1) {
            locString = LocalizationManager.GetTranslation("Report Panel/since_new_followers") // "{[COUNT_DELTA]} new followers have made you their patron!"
                .Replace("{[COUNT_DELTA]}", color_tag_open + string.Format("{0:n0}", GameData.instance.report__follower_count) + color_tag_close);
            AddReportLine(reportIconFollowers, locString);
        }

        if (GameData.instance.report__follower_count == 1) {
            locString = LocalizationManager.GetTranslation("Report Panel/since_new_follower"); // "A new follower has made you their patron!"
            AddReportLine(reportIconFollowers, locString);
        }

        if (GameData.instance.report__follower_count < -1) {
            locString = LocalizationManager.GetTranslation("Report Panel/since_lost_followers") // "You have lost {[COUNT_DELTA]} followers."
                .Replace("{[COUNT_DELTA]}", color_tag_open + string.Format("{0:n0}", -GameData.instance.report__follower_count) + color_tag_close);
            AddReportLine(reportIconFollowers, locString);
        }

        if (GameData.instance.report__follower_count == -1) {
            locString = LocalizationManager.GetTranslation("Report Panel/since_lost_follower"); // "You have lost one follower."
            AddReportLine(reportIconFollowers, locString);
        }

        if (GameData.instance.report__follower_credits > 0) {
            locString = LocalizationManager.GetTranslation("Report Panel/since_follower_credit_bonus") // "You've received a bonus of {[CREDIT_DELTA]} credits from your followers!"
                .Replace("{[CREDIT_DELTA]}", color_tag_open + string.Format("{0:n0}", (int)(GameData.instance.report__follower_credits)) + color_tag_close);
            AddReportLine(reportIconCredits, locString);
        }

        if (GameData.instance.report__follower_XP > 0) {
            locString = LocalizationManager.GetTranslation("Report Panel/since_follower_xp_bonus") // "You've received a bonus of {[XP_DELTA]} XP from your followers!"
                .Replace("{[XP_DELTA]}", color_tag_open + string.Format("{0:n0}", (int)(GameData.instance.report__follower_XP)) + color_tag_close);
            AddReportLine(reportIconLevels, locString);
        }

        if (GameData.instance.report__patron_credits > 0)
        {
            locString = LocalizationManager.GetTranslation("Report Panel/since_patron_credit_bonus") // "You've received a bonus of {[CREDIT_DELTA]} credits from your patron, {[PATRON_USERNAME]}!"
                .Replace("{[CREDIT_DELTA]}", color_tag_open + string.Format("{0:n0}", (int)(GameData.instance.report__patron_credits)) + color_tag_close)
                .Replace("{[PATRON_USERNAME]}", GameData.instance.patronUsername);
            AddReportLine(reportIconCredits, locString);
        }
        
        if (GameData.instance.report__patron_XP > 0) {
            locString = LocalizationManager.GetTranslation("Report Panel/since_patron_xp_bonus") // "You've received a bonus of {[XP_DELTA]} XP from your patron, {[PATRON_USERNAME]}!"
                .Replace("{[XP_DELTA]}", color_tag_open + string.Format("{0:n0}", (int)(GameData.instance.report__patron_XP)) + color_tag_close)
                .Replace("{[PATRON_USERNAME]}", GameData.instance.patronUsername);
            AddReportLine(reportIconLevels, locString);
        }

        if (GameData.instance.report__defenses_squares_defeated == 1) {
            // Our defenses have defeated an enemy land square and earned {[XP_EARNED]} XP! 
            locString = LocalizationManager.GetTranslation("Report Panel/since_defeated_enemy_earned_xp").Replace("{[XP_EARNED]}", color_tag_open + string.Format("{0:n0}", GameData.instance.report__defenses_XP) + color_tag_close);
            AddReportLine(reportIconDefenses, locString);
        }

        if (GameData.instance.report__defenses_squares_defeated > 1) {
            // Our defenses have defeated {[ENEMY_SQUARES]} squares of enemy land and earned {[XP_EARNED]} XP! 
            locString = LocalizationManager.GetTranslation("Report Panel/since_x_enemy_squares_defeated")
                .Replace("{[ENEMY_SQUARES]}", color_tag_open + string.Format("{0:n0}", GameData.instance.report__defenses_squares_defeated) + color_tag_close)
                .Replace("{[XP_EARNED]}", color_tag_open + string.Format("{0:n0}", GameData.instance.report__defenses_XP) + color_tag_close);
            AddReportLine(reportIconDefenses, locString);
        }

        if (GameData.instance.report__attacks_squares_captured == 1) {
            // In battle we have captured an enemy land square and earned {[EARNED_XP]} XP! 
            locString = LocalizationManager.GetTranslation("Report Panel/since_captured_enemy_square")
                .Replace("{[EARNED_XP]}", color_tag_open + string.Format("{0:n0}", GameData.instance.report__attacks_XP) + color_tag_close);
            AddReportLine(reportIconAttacks, locString);
        }

        if (GameData.instance.report__attacks_squares_captured > 1) {
            // In battle we have captured {[ENEMY_SQUARES]} enemy squares and earned {[EARNED_XP]} XP! 
            locString = LocalizationManager.GetTranslation("Report Panel/since_captured_x_enemy_squares")
                .Replace("{[ENEMY_SQUARES]}", color_tag_open + string.Format("{0:n0}", GameData.instance.report__attacks_squares_captured) + color_tag_close)
                .Replace("{[EARNED_XP]}", color_tag_open + string.Format("{0:n0}", GameData.instance.report__attacks_XP) + color_tag_close);
            AddReportLine(reportIconAttacks, locString);
        }

        if (GameData.instance.report__resource_count_delta == 1) {
            // "We have captured a resource!" 
            AddReportLine(reportIconResource, LocalizationManager.GetTranslation("Report Panel/since_captured_resource"));
        }

        if (GameData.instance.report__resource_count_delta > 1) {
            // We have captured {[RESOURCE_DELTA]} resources! 
            locString = LocalizationManager.GetTranslation("Report Panel/since_captured_resources")
                .Replace("{[RESOURCE_DELTA]}", color_tag_open + string.Format("{0:n0}", GameData.instance.report__resource_count_delta) + color_tag_close);
            AddReportLine(reportIconResource, locString);
        }

        if (GameData.instance.report__resource_count_delta == -1) {
            // "We have lost a resource" 
            AddReportLine(reportIconResource, LocalizationManager.GetTranslation("Report Panel/since_lost_a_resource"));
        }

        if (GameData.instance.report__resource_count_delta < -1) {
            // We have lost {[RESOURCE_DELTA]} resources 
            locString = LocalizationManager.GetTranslation("Report Panel/since_lost_x_resources")
                .Replace("{[RESOURCE_DELTA]}", color_tag_open + string.Format("{0:n0}", (-GameData.instance.report__resource_count_delta)) + color_tag_close);
            AddReportLine(reportIconResource, locString);
        }

        if (GameData.instance.report__defenses_lost == 1) {
            // "We have lost a defense to enemy attacks" 
            AddReportLine(reportIconDefenses, LocalizationManager.GetTranslation("Report Panel/since_lost_a_defense"));
        }

        if (GameData.instance.report__defenses_lost > 1) {
            // We have lost {[LOST_DEFENSES]} defenses to enemy attacks 
            locString = LocalizationManager.GetTranslation("Report Panel/since_lost_x_defenses")
                .Replace("{[LOST_DEFENSES]}", color_tag_open + string.Format("{0:n0}", GameData.instance.report__defenses_lost) + color_tag_close);
            AddReportLine(reportIconDefenses, locString);
        }

        if (GameData.instance.report__walls_lost == 1) {
            // "We have lost a wall to enemy attacks" 
            AddReportLine(reportIconDefenses, LocalizationManager.GetTranslation("Report Panel/since_lost_a_wall"));
        }

        if (GameData.instance.report__walls_lost > 1) {
            // We have lost {[LOST_WALLS]} walls to enemy attacks
            locString = LocalizationManager.GetTranslation("Report Panel/since_lost_x_walls")
                .Replace("{[LOST_WALLS]}", color_tag_open + string.Format("{0:n0}", GameData.instance.report__walls_lost) + color_tag_close);
            AddReportLine(reportIconDefenses, locString);
        }

        if (GameData.instance.report__land_lost == 1) {
            // "We have lost one square of land to enemy attacks" 
            AddReportLine(reportIconLand, LocalizationManager.GetTranslation("Report Panel/since_lost_a_land_square"));
        }

        if (GameData.instance.report__land_lost > 1) {
            // We have lost {[LOST_LAND_SQUARE]} squares of land to enemy attacks
            locString = LocalizationManager.GetTranslation("Report Panel/since_lost_x_land_squares")
                .Replace("{[LOST_LAND_SQUARE]}", color_tag_open + string.Format("{0:n0}", GameData.instance.report__land_lost) + color_tag_close);
            AddReportLine(reportIconLand, locString);
        }

        if (GameData.instance.report__credits_spent > 1) {
            // We have spent {[CREDITS_SPENT]} credits 
            locString = LocalizationManager.GetTranslation("Report Panel/since_spent_x_credits")
                .Replace("{[CREDITS_SPENT]}", color_tag_open + string.Format("{0:n0}", (int)GameData.instance.report__credits_spent) + color_tag_close);
            AddReportLine(reportIconCredits, locString);
        }

        if (GameData.instance.report__energy_lost_to_raids > 1) {
            // We have lost {[ENERGY_LOST]} energy to enemy raids 
            locString = LocalizationManager.GetTranslation("Report Panel/since_x_energy_lost_to_raids")
                .Replace("{[ENERGY_LOST]}", color_tag_open + string.Format("{0:n0}", (int)GameData.instance.report__energy_lost_to_raids) + color_tag_close);
            AddReportLine(reportIconEnergy, locString);
        }

        if (GameData.instance.report__manpower_lost_to_raids > 1) {
            // We have lost {[MANPOWER_LOST]} manpower to enemy raids 
            locString = LocalizationManager.GetTranslation("Report Panel/since_x_manpower_lost_to_raids")
                .Replace("{[MANPOWER_LOST]}", color_tag_open + string.Format("{0:n0}", (int)GameData.instance.report__manpower_lost_to_raids) + color_tag_close);
            AddReportLine(reportIconManpower, locString);
        }

        if (GameData.instance.report__defenses_built > 1) {
            // We have built {[NEW_DEFENSES]} defenses 
            locString = LocalizationManager.GetTranslation("Report Panel/since_built_x_defenses")
                .Replace("{[NEW_DEFENSES]}", color_tag_open + string.Format("{0:n0}", GameData.instance.report__defenses_built) + color_tag_close);
            AddReportLine(reportIconDefenses, locString);
        }

        if (GameData.instance.report__walls_built > 1) {
            // We have built {[WALLS_BUILT]} walls 
            locString = LocalizationManager.GetTranslation("Report Panel/since_x_walls_built")
                .Replace("{[WALLS_BUILT]}", color_tag_open + string.Format("{0:n0}", GameData.instance.report__walls_built) + color_tag_close);
            AddReportLine(reportIconWalls, locString);
        }

        if ((GameData.instance.report__energy_spent > 1) && (((int)GameData.instance.report__energy_spent) == ((int)(GameData.instance.energy - (GameData.instance.report__energy_begin - GameData.instance.report__energy_spent)))))
        {
            // We have generated {[ENERGY_SPENT]} energy for our defenses 
            locString = LocalizationManager.GetTranslation("Report Panel/since_generated_x_energy_for_defense")
                .Replace("{[ENERGY_SPENT]}", color_tag_open + string.Format("{0:n0}", (int)GameData.instance.report__energy_spent) + color_tag_close);
            AddReportLine(reportIconEnergy, locString);
        }
        else 
        {
            if ((GameData.instance.report__energy_begin - GameData.instance.report__energy_spent) < (GameData.instance.energy - 1)) {
                // We have generated {[ENERGY_DELTA]} energy 
                locString = LocalizationManager.GetTranslation("Report Panel/since_generated_x_energy")
                    .Replace("{[ENERGY_DELTA]}", color_tag_open + string.Format("{0:n0}", (int)(GameData.instance.energy - (GameData.instance.report__energy_begin - GameData.instance.report__energy_spent))) + color_tag_close);
                AddReportLine(reportIconEnergy, locString);
            }

            if (GameData.instance.report__energy_spent > 1) {
                // We have spent {[ENERGY_SPENT]} energy on our defenses 
                locString = LocalizationManager.GetTranslation("Report Panel/since_spent_x_energy_on_defense")
                    .Replace("{[ENERGY_SPENT]}", color_tag_open + string.Format("{0:n0}", (int)GameData.instance.report__energy_spent) + color_tag_close);
                AddReportLine(reportIconEnergy, locString);
            }
        }

        if (GameData.instance.report__energy_donated > 1) {
            // We have donated {[ENERGY_DONATED]} energy to our allies 
            locString = LocalizationManager.GetTranslation("Report Panel/since_donated_x_energy")
                .Replace("{[ENERGY_DONATED]}", color_tag_open + string.Format("{0:n0}", (int)GameData.instance.report__energy_donated) + color_tag_close);
            AddReportLine(reportIconEnergy, locString);
        }

        if (GameData.instance.report__manpower_spent > 1) {
            // We have spent {[MANPOWER_SPENT]} manpower  
            locString = LocalizationManager.GetTranslation("Report Panel/since_spent_x_manpower")
                .Replace("{[MANPOWER_SPENT]}", color_tag_open + string.Format("{0:n0}", (int)GameData.instance.report__manpower_spent) + color_tag_close);
            AddReportLine(reportIconManpower, locString);
        }

        if (GameData.instance.report__manpower_lost_to_resources > 1) {
            // We have lost {[MANPOWER_LOST]} manpower supporting excess resources
            locString = LocalizationManager.GetTranslation("Report Panel/since_lost_x_manpower_to_resources")
                .Replace("{[MANPOWER_LOST]}", color_tag_open + string.Format("{0:n0}", (int)GameData.instance.report__manpower_lost_to_resources) + color_tag_close);
            AddReportLine(reportIconManpower, locString);
        }

        if (GameData.instance.report__manpower_donated > 1) {
            // We have donated {[MANPOWER_DONATED]} manpower to our allies 
            locString = LocalizationManager.GetTranslation("Report Panel/since_donated_x_manpower")
                .Replace("{[MANPOWER_DONATED]}", color_tag_open + string.Format("{0:n0}", (int)GameData.instance.report__manpower_donated) + color_tag_close);
            AddReportLine(reportIconEnergy, locString);
        }

        if ((GameData.instance.report__manpower_begin - GameData.instance.report__manpower_spent - GameData.instance.report__manpower_lost_to_resources) < (GameData.instance.current_footprint.manpower - 1)) {
            // We have generated {[MANPOWER_DELTA]} manpower 
            locString = LocalizationManager.GetTranslation("Report Panel/since_generated_x_manpower")
                .Replace("{[MANPOWER_DELTA]}", color_tag_open + string.Format("{0:n0}", (int)(GameData.instance.current_footprint.manpower - (GameData.instance.report__manpower_begin - GameData.instance.report__manpower_spent - GameData.instance.report__manpower_lost_to_resources))) + color_tag_close);
            AddReportLine(reportIconManpower, locString);
        }

        // Do not show the report if it contains no lines.
        if (reportLines.Count == 0) {
            return;
        }

        // Set title text
        // Welcome back, {[USER_NAME]}!
        titleText.text = LocalizationManager.GetTranslation("Report Panel/welcome_back_user").Replace("{[USER_NAME]}", GameData.instance.username);

        // Show the report panel.
        GameGUI.instance.OpenReportPanelAfterDelay();
    }

    void OnEnable()
    {
        // Do nothing if the client is not connected to the server yet.
        if ((GameData.instance == null) || (GameData.instance.userID == -1)) {
            return;
        }
    }

    public void AddReportLine(Sprite _iconImage, string _text)
    {
        // Do not add another line if the maximum number has already been reached.
        if (reportLines.Count >= MAX_NUM_REPORT_LINES) {
            return;
        }

        // Instantiate a new ReportLine
        GameObject reportLineObject = Instantiate(reportLinePrefab, new Vector3(0f, 0f, 0f), Quaternion.identity) as GameObject;
        reportLineObject.transform.SetParent(contentArea);
        reportLineObject.transform.SetAsLastSibling();
        reportLineObject.transform.localScale = new Vector3(1, 1, 1);

        // Initialize the ReportLine
        ReportLine reportLine = reportLineObject.GetComponent<ReportLine>();
        reportLine.image.sprite = _iconImage;
        reportLine.text.text = _text;
        //reportLineObject.GetComponent<Image>().enabled = ((reportLines.Count % 2) == 0); // Show background image on every other line.

        // Add this new ReportLine to the list.
        reportLines.Add(reportLineObject);
    }

    public void ClearReport()
    {
        // Remove and delete all ReportLines.
        GameObject cur_report_line;
        while (reportLines.Count > 0)
        {
            cur_report_line = reportLines[0];
            reportLines.RemoveAt(0);
            cur_report_line.transform.SetParent(null);
            GameObject.Destroy(cur_report_line);
        }
    }

    public bool LoginReportAvailable()
    {
        return (reportLines.Count > 0);
    }
}
