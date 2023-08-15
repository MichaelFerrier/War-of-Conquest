using UnityEngine;
using UnityEngine.UI;
using UnityEngine.EventSystems;
using System.Collections;
using System;
using I2.Loc;

public class NationInfoPanel : MonoBehaviour, IPointerDownHandler
{
    public static NationInfoPanel instance;

    public TMPro.TextMeshProUGUI title, text_area, allianceButtonText, statsTitleText;
    public TMPro.TextMeshProUGUI nation1NameText, nation1TechText, nation1BioText, nation1PsiText;
    public TMPro.TextMeshProUGUI nation2NameText, nation2TechText, nation2BioText, nation2PsiText;
    public Image nation1TechBackground, nation1BioBackground, nation1PsiBackground;
    public Image nation2TechBackground, nation2BioBackground, nation2PsiBackground;

    public GameObject messageButtonObject, allianceButtonObject, middleSpacerObject, uniteButtonObject;

    private int infoNationID, infoNationLevel;
    private string infoNationName;
    private LinkManager linkManager = new LinkManager();
    private int attackerStat, defenderStat;

    private string line_title_color = "yellow";

    private enum RequestorTask
    {
        SendUniteInvitation
    };

    public NationInfoPanel()
    {
        instance = this;
    }
    
    public void Init(int _ID, string _name, int _level, int _area, int _num_trophies, float _geo_eff, int _stat_tech, int _stat_bio, int _stat_psi, int _num_alliances, int[] _ally_ids, string[] _ally_names, int _num_members, string[] _member_names, bool[] _member_logged_in, int[] _member_rank, int _attacker_stat, int _defender_stat)
    {
        int i;

        // Display stats title
        statsTitleText.text = "<color=\"yellow\">" + I2.Loc.LocalizationManager.GetTranslation("Info Panel/combat_stats") + "</color>";

        // Record ID and name of nation whose info to display, and stats.
        infoNationID = _ID;
        infoNationName = _name;
        infoNationLevel = _level;
        attackerStat = _attacker_stat;
        defenderStat = _defender_stat;

        // Reset list of links.
        linkManager.ResetLinks();

        // Display nation name
        title.text = _name;

        string text = "";

        // Level
        text += "<color=\"" + line_title_color + "\">" + LocalizationManager.GetTranslation("Generic Text/level_word") +":</color> " + _level + "\n"; 

        // Area
        text += "<color=\"" + line_title_color + "\">" + LocalizationManager.GetTranslation("Generic Text/area_word")  + ":</color> " + string.Format("{0:n0}", _area) + "\n"; 

        // Stats
        nation1NameText.text = "<color=\"yellow\">" + GameData.instance.nationName + "</color>";
        nation1TechText.text = string.Format("{0:n0}", GameData.instance.GetStatValueString(GameData.Stat.TECH));
        nation1BioText.text = string.Format("{0:n0}", GameData.instance.GetStatValueString(GameData.Stat.BIO));
        nation1PsiText.text = string.Format("{0:n0}", GameData.instance.GetStatValueString(GameData.Stat.PSI));
        nation2NameText.text = "<color=\"yellow\">" + _name + "</color>";
        nation2TechText.text = string.Format("{0:n0}", _stat_tech);
        nation2BioText.text = string.Format("{0:n0}", _stat_bio);
        nation2PsiText.text = string.Format("{0:n0}", _stat_psi);

        // Stat background hilight images
        nation1TechBackground.enabled = (_attacker_stat == GameData.STAT_TECH);
        nation1BioBackground.enabled = (_attacker_stat == GameData.STAT_BIO);
        nation1PsiBackground.enabled = (_attacker_stat == GameData.STAT_PSI);
        nation2TechBackground.enabled = (_defender_stat == GameData.STAT_TECH);
        nation2BioBackground.enabled = (_defender_stat == GameData.STAT_BIO);
        nation2PsiBackground.enabled = (_defender_stat == GameData.STAT_PSI);

        // Allies
        if (_num_alliances > 0)
        {
            text += "<color=\"" + line_title_color + "\">" + LocalizationManager.GetTranslation("Generic Text/allies_word") + ":</color> ";
            for (i = 0; i < _num_alliances; i++)
            {
                if (i > 0) {
                    text += ", ";
                }

                // Add nation link for this ally
                text += "<link=\"" + linkManager.num_links + "\"><u>" + _ally_names[i] + "</u></link>";
                linkManager.AddLink(LinkManager.LinkType.NATION, _ally_ids[i]);
            }

            text += "\n";
        }

        int sovereign_index = -1, num_online = 0;
        for (i = 0; i < _num_members; i++)
        {
            if (_member_rank[i] == GameData.RANK_SOVEREIGN) {
                sovereign_index = i;
            }

            if (_member_logged_in[i]) {
                num_online++;
            }
        }

        // GB-Localization
        // Name of sovereign
        if (sovereign_index != -1) {
            text += "<color=\"" + line_title_color + "\">" + LocalizationManager.GetTranslation("Generic Text/sovereign_word") + ":</color> " + _member_names[sovereign_index] + "\n"; 
        }

        // Number of members, and number of members online.
        text += "<color=\"" + line_title_color + "\">" + LocalizationManager.GetTranslation("Generic Text/members_word") + ":</color> " + _num_members + ((num_online == 0) ? "" : (" (" + num_online + " " + LocalizationManager.GetTranslation("Generic Text/online_word_lower") + ")")) + "\n"; 

        // Geographic efficiency.
        text += "<color=\"" + line_title_color + "\">" + LocalizationManager.GetTranslation("Stat Description/geographic_efficiency_title") + ":</color> " + (int)(_geo_eff * 100f + 0.5f) + "%\n"; 

        // Number of tournament trophies, if this nation is in the tournament.
        if (_num_trophies != -1) {
            text += "<color=\"" + line_title_color + "\">" + LocalizationManager.GetTranslation("Tournament/tournament_trophies") + ":</color> " + string.Format("{0:n0}", _num_trophies) + "\n"; 
        }

        text_area.text = text;

        UpdateButtons();
    }

    public void UpdateButtons()
    {
        int num_buttons = 0;

        messageButtonObject.SetActive(false);
        allianceButtonObject.SetActive(false);
        uniteButtonObject.SetActive(false);

        if (infoNationID != GameData.instance.nationID)
        {
            messageButtonObject.SetActive(true);
            num_buttons++;
        }

        if (infoNationID != GameData.instance.nationID)
        {
            if ((Math.Abs(infoNationLevel - GameData.instance.level) <= GameData.instance.allyLevelDiffLimit) && (GameData.instance.NationIsInAllianceList(GameData.instance.alliesList, infoNationID) == false) && (GameData.instance.NationIsInAllianceList(GameData.instance.outgoingAllyRequestsList, infoNationID) == false))
            {
                allianceButtonText.text = (GameData.instance.NationIsInAllianceList(GameData.instance.incomingAllyRequestsList, infoNationID) ? LocalizationManager.GetTranslation("Nation Panel/accept_alliance") : LocalizationManager.GetTranslation("Nation Panel/invite_to_ally"));
                allianceButtonObject.SetActive(true);
                num_buttons++;
            }
            else if (GameData.instance.NationIsInAllianceList(GameData.instance.outgoingUniteRequestsList, infoNationID) == false)
            {
                uniteButtonObject.SetActive(true);
                num_buttons++;
            }
        }

        middleSpacerObject.SetActive(num_buttons != 1);
    }

    public void OnPointerDown(PointerEventData _eventData)
    {
        // Determine whether link text has been clicked.
        int link_index = TMPro.TMP_TextUtilities.FindIntersectingLink(text_area, Input.mousePosition, null);

        if (link_index != -1)
        {
            if (linkManager.link_types[link_index] == LinkManager.LinkType.NATION)
            {
                // Close this panel
                GameGUI.instance.CloseAllPanels();

                // Send event_nation_info event to the server.
                Network.instance.SendCommand("action=request_nation_info|targetNationID=" + linkManager.link_ids[link_index]);
            }
        }
    }

    public void OnClick_Close()
    {
        GameGUI.instance.CloseAllPanels();
    }

    public void OnClick_StatsInfo()
    {
        string info_text = I2.Loc.LocalizationManager.GetTranslation("nation_info_stats_help_1");

        if (attackerStat != defenderStat)
        {
            if (((attackerStat == GameData.STAT_TECH) && (defenderStat == GameData.STAT_BIO)) ||
                ((attackerStat == GameData.STAT_BIO) && (defenderStat == GameData.STAT_PSI)) ||
                ((attackerStat == GameData.STAT_PSI) && (defenderStat == GameData.STAT_TECH)))
            {
                info_text += I2.Loc.LocalizationManager.GetTranslation("nation_info_stats_help_2");
            }

            if (((attackerStat == GameData.STAT_BIO) && (defenderStat == GameData.STAT_TECH)) ||
                ((attackerStat == GameData.STAT_PSI) && (defenderStat == GameData.STAT_BIO)) ||
                ((attackerStat == GameData.STAT_TECH) && (defenderStat == GameData.STAT_PSI)))
            {
                info_text += I2.Loc.LocalizationManager.GetTranslation("nation_info_stats_help_3");
            }
        }

        info_text = info_text.Replace("{info_nation_name}", infoNationName);
        info_text = info_text.Replace("{attacker_stat}", GameGUI.instance.GetCombatStatName(attackerStat));
        info_text = info_text.Replace("{defender_stat}", GameGUI.instance.GetCombatStatName(defenderStat));

        Requestor.Activate(0, 0, null, info_text, LocalizationManager.GetTranslation("Generic Text/okay"), "");
    }

    public void OnClick_MessageButton()
    {
        // Close this panel.
        GameGUI.instance.CloseAllPanels();

        // Open the post message dialog.
        PostMessagePanel.instance.SetRecipient(infoNationName);
        GameGUI.instance.OpenPostMessageDialog();
    }

    public void OnClick_AllianceButton()
    {
        if (GameData.instance.NationIsInAllianceList(GameData.instance.incomingAllyRequestsList, infoNationID))
        {
            // Send event_accept_alliance event to the server.
            Network.instance.SendCommand("action=event_accept_alliance|targetNationID=" + infoNationID);
        }
        else
        {
            // Send event_request_alliance event to the server.
            Network.instance.SendCommand("action=event_request_alliance|targetNationID=" + infoNationID);
        }

        // Refresh nation info
        Network.instance.SendCommand("action=request_nation_info|targetNationID=" + infoNationID);
    }

    public void OnClick_UniteButton()
    {
        // Set up the invite unite panel.
        InviteUnitePanel.instance.Init(infoNationID, infoNationName);

        // Show the invite unite panel.
        GameGUI.instance.OpenInviteUniteDialog();
    }
}
